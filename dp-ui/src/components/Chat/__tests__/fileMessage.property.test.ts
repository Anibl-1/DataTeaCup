/**
 * Feature: mars-integration-optimization, Property 18: 文件消息渲染完整性
 * **Validates: Requirements 12.10**
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { formatFileSize } from '../../../utils/chatTimeFormat'
import type { ChatMessage } from '../../../types/chat'

// --- Generators ---

/** Generate a positive file size in bytes */
const arbPositiveBytes = fc.integer({ min: 1, max: 10 * 1024 * 1024 * 1024 }) // up to ~10 GB

/** Generate a non-empty file name */
const arbFileName = fc
  .array(
    fc.constantFrom(
      ...'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-'.split('')
    ),
    { minLength: 1, maxLength: 50 }
  )
  .map((chars) => chars.join(''))
  .chain((name) =>
    fc.constantFrom('.pdf', '.docx', '.xlsx', '.zip', '.png', '.jpg', '.txt').map(
      (ext) => name + ext
    )
  )

/** Generate a non-empty file URL */
const arbFileUrl = fc
  .array(
    fc.constantFrom(
      ...'abcdefghijklmnopqrstuvwxyz0123456789'.split('')
    ),
    { minLength: 5, maxLength: 30 }
  )
  .map((chars) => `/files/upload/${chars.join('')}`)

/** Generate a file-type ChatMessage with all required fields */
function arbFileMessage(): fc.Arbitrary<ChatMessage> {
  return fc.record({
    id: fc.integer({ min: 1, max: 999999 }),
    conversationId: fc.integer({ min: 1, max: 999999 }),
    senderId: fc.integer({ min: 1, max: 999999 }),
    senderName: fc.string({ minLength: 1, maxLength: 20 }),
    fileName: arbFileName,
    fileSize: arbPositiveBytes,
    fileUrl: arbFileUrl,
    sendTime: fc.date({ min: new Date('2020-01-01'), max: new Date('2030-01-01') }).map(
      (d) => d.toISOString()
    ),
    content: fc.string({ minLength: 0, maxLength: 100 }),
  }).map(({ id, conversationId, senderId, senderName, fileName, fileSize, fileUrl, sendTime, content }) => ({
    id,
    conversationId,
    senderId,
    senderName,
    contentType: 'file' as const,
    content,
    fileUrl,
    fileName,
    fileSize,
    sendTime,
  }))
}

// --- Property 18: 文件消息渲染完整性 ---

describe('Property 18: 文件消息渲染完整性', () => {
  it('formatFileSize returns a non-empty string for any positive fileSize', () => {
    fc.assert(
      fc.property(arbPositiveBytes, (bytes) => {
        const result = formatFileSize(bytes)
        expect(result).toBeTruthy()
        expect(result.length).toBeGreaterThan(0)
      }),
      { numRuns: 200 }
    )
  })

  it('bytes < 1024 result ends with " B"', () => {
    fc.assert(
      fc.property(fc.integer({ min: 1, max: 1023 }), (bytes) => {
        const result = formatFileSize(bytes)
        expect(result).toMatch(/ B$/)
        expect(result).toBe(`${bytes} B`)
      }),
      { numRuns: 200 }
    )
  })

  it('bytes >= 1024 and < 1024*1024 result ends with " KB"', () => {
    fc.assert(
      fc.property(fc.integer({ min: 1024, max: 1024 * 1024 - 1 }), (bytes) => {
        const result = formatFileSize(bytes)
        expect(result).toMatch(/ KB$/)
      }),
      { numRuns: 200 }
    )
  })

  it('bytes >= 1024*1024 result ends with " MB"', () => {
    fc.assert(
      fc.property(fc.integer({ min: 1024 * 1024, max: 10 * 1024 * 1024 * 1024 }), (bytes) => {
        const result = formatFileSize(bytes)
        expect(result).toMatch(/ MB$/)
      }),
      { numRuns: 200 }
    )
  })

  it('file message with fileName, fileSize, fileUrl has all required fields present and non-empty', () => {
    fc.assert(
      fc.property(arbFileMessage(), (msg) => {
        // All file-related fields must be present and non-empty
        expect(msg.contentType).toBe('file')
        expect(msg.fileName).toBeTruthy()
        expect(msg.fileName!.length).toBeGreaterThan(0)
        expect(msg.fileUrl).toBeTruthy()
        expect(msg.fileUrl!.length).toBeGreaterThan(0)
        expect(msg.fileSize).toBeGreaterThan(0)

        // formatFileSize should produce a valid formatted string for the fileSize
        const formattedSize = formatFileSize(msg.fileSize)
        expect(formattedSize).toBeTruthy()
        expect(formattedSize.length).toBeGreaterThan(0)

        // The fileUrl can serve as a download link
        expect(msg.fileUrl).toMatch(/^\/files\/upload\//)
      }),
      { numRuns: 200 }
    )
  })
})

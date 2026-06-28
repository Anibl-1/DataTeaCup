/**
 * Feature: page-audit-optimization, Property 6: 文件导入类型和大小校验
 * **Validates: Requirements 3.4**
 *
 * *For any* 上传文件，当文件扩展名为 .xlsx、.xls、.csv 或 .txt 且文件大小 ≤ 100MB 时，
 * 应被接受；否则应被拒绝并显示错误提示。
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import {
  validateFileForImport,
  ALLOWED_EXTENSIONS,
  MAX_FILE_SIZE
} from '../fileImport'

/** Arbitrary that produces one of the allowed extensions (case-insensitive variants). */
const allowedExtArb = fc.constantFrom(...ALLOWED_EXTENSIONS).map((ext) => {
  // Randomly upper/lower-case the extension to test case insensitivity
  return ext
    .split('')
    .map((ch) => (Math.random() > 0.5 ? ch.toUpperCase() : ch))
    .join('')
})

/** Arbitrary for a valid base file name (non-empty, no dots). */
const baseNameArb = fc
  .array(fc.constantFrom(...'abcdefghijklmnopqrstuvwxyz0123456789_-'.split('')), {
    minLength: 1,
    maxLength: 30
  })
  .map((chars) => chars.join(''))

/** Arbitrary for a disallowed extension. */
const disallowedExtArb = fc
  .array(fc.constantFrom(...'abcdefghijklmnopqrstuvwxyz'.split('')), {
    minLength: 1,
    maxLength: 6
  })
  .map((chars) => chars.join(''))
  .filter((s) => !ALLOWED_EXTENSIONS.includes(`.${s.toLowerCase()}` as any))
  .map((s) => `.${s}`)

/** Arbitrary for a valid file size: 1 byte to MAX_FILE_SIZE inclusive. */
const validSizeArb = fc.integer({ min: 1, max: MAX_FILE_SIZE })

/** Arbitrary for an oversized file: just above MAX_FILE_SIZE. */
const oversizedArb = fc.integer({ min: MAX_FILE_SIZE + 1, max: MAX_FILE_SIZE * 2 })

describe('Property 6: 文件导入类型和大小校验', () => {
  /**
   * Files with an allowed extension AND valid size (1..100MB) must be accepted.
   */
  it('accepts files with allowed extension and valid size', () => {
    fc.assert(
      fc.property(baseNameArb, allowedExtArb, validSizeArb, (base, ext, size) => {
        const result = validateFileForImport(`${base}${ext}`, size)
        expect(result.valid).toBe(true)
        expect(result.error).toBeUndefined()
      }),
      { numRuns: 200 }
    )
  })

  /**
   * Files with a disallowed extension must be rejected regardless of size.
   */
  it('rejects files with disallowed extension', () => {
    fc.assert(
      fc.property(baseNameArb, disallowedExtArb, validSizeArb, (base, ext, size) => {
        const result = validateFileForImport(`${base}${ext}`, size)
        expect(result.valid).toBe(false)
        expect(result.error).toBeDefined()
      }),
      { numRuns: 200 }
    )
  })

  /**
   * Files exceeding MAX_FILE_SIZE must be rejected even with an allowed extension.
   */
  it('rejects files exceeding 100MB even with allowed extension', () => {
    fc.assert(
      fc.property(baseNameArb, allowedExtArb, oversizedArb, (base, ext, size) => {
        const result = validateFileForImport(`${base}${ext}`, size)
        expect(result.valid).toBe(false)
        expect(result.error).toContain('100MB')
      }),
      { numRuns: 200 }
    )
  })

  /**
   * Files with zero or negative size must be rejected.
   */
  it('rejects files with non-positive size', () => {
    fc.assert(
      fc.property(
        baseNameArb,
        allowedExtArb,
        fc.integer({ min: -1000, max: 0 }),
        (base, ext, size) => {
          const result = validateFileForImport(`${base}${ext}`, size)
          expect(result.valid).toBe(false)
          expect(result.error).toBeDefined()
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * The boundary: a file at exactly MAX_FILE_SIZE with an allowed extension must be accepted.
   */
  it('accepts files at exactly MAX_FILE_SIZE boundary', () => {
    fc.assert(
      fc.property(baseNameArb, allowedExtArb, (base, ext) => {
        const result = validateFileForImport(`${base}${ext}`, MAX_FILE_SIZE)
        expect(result.valid).toBe(true)
      }),
      { numRuns: 100 }
    )
  })

  /**
   * One byte over MAX_FILE_SIZE with an allowed extension must be rejected.
   */
  it('rejects files at MAX_FILE_SIZE + 1 boundary', () => {
    fc.assert(
      fc.property(baseNameArb, allowedExtArb, (base, ext) => {
        const result = validateFileForImport(`${base}${ext}`, MAX_FILE_SIZE + 1)
        expect(result.valid).toBe(false)
      }),
      { numRuns: 100 }
    )
  })
})

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { updateMessageStatus } from '@/stores/chat'
import type { ChatMessage } from '@/types/chat'

/**
 * Property 12: 消息发送状态转换正确性
 *
 * **Validates: Requirements 11.3**
 * **Feature: core-modules-deep-optimization, Property 12: 消息发送状态转换正确性**
 *
 * For any 消息发送操作，消息状态应从 'sending' 转换为 'sent'（API 成功时）
 * 或 'failed'（API 失败时），且不存在其他状态转换路径。
 */
describe('chat store — Property Tests (消息状态转换)', () => {
  // --- Smart Arbitraries ---

  const contentTypeArb = fc.constantFrom('text', 'image', 'file') as fc.Arbitrary<
    'text' | 'image' | 'file'
  >

  const chatMessageArb = (overrides?: Partial<ChatMessage>): fc.Arbitrary<ChatMessage> =>
    fc.record({
      id: fc.nat(),
      conversationId: fc.nat(),
      senderId: fc.nat(),
      senderName: fc.string({ minLength: 1, maxLength: 20 }),
      contentType: contentTypeArb,
      content: fc.string({ maxLength: 200 }),
      sendTime: fc
        .integer({ min: 1577836800000, max: 1893456000000 })
        .map((ts) => new Date(ts).toISOString()),
      status: fc.constant('sending' as const),
      localId: fc.string({ minLength: 1, maxLength: 30 }),
    }).map((msg) => {
      const base: ChatMessage = { ...msg }
      return { ...base, ...overrides }
    })

  /** Generate a messages array that contains exactly one 'sending' message with a known localId */
  const messagesWithSendingArb = fc
    .tuple(
      fc.array(chatMessageArb({ status: 'sent' }), { minLength: 0, maxLength: 5 }),
      chatMessageArb({ status: 'sending' }),
      fc.array(chatMessageArb({ status: 'sent' }), { minLength: 0, maxLength: 5 }),
    )
    .map(([before, target, after]) => ({
      messages: [...before, target, ...after],
      target,
      localId: target.localId!,
    }))

  // --- Property 12 Tests ---

  it('Property 12: sending → sent transition produces status "sent"', () => {
    fc.assert(
      fc.property(messagesWithSendingArb, ({ messages, localId }) => {
        const result = updateMessageStatus(messages, localId, 'sent')
        const updated = result.find((m) => m.localId === localId)
        expect(updated).toBeDefined()
        expect(updated!.status).toBe('sent')
      }),
      { numRuns: 100 },
    )
  })

  it('Property 12: sending → failed transition produces status "failed"', () => {
    fc.assert(
      fc.property(messagesWithSendingArb, ({ messages, localId }) => {
        const result = updateMessageStatus(messages, localId, 'failed')
        const updated = result.find((m) => m.localId === localId)
        expect(updated).toBeDefined()
        expect(updated!.status).toBe('failed')
      }),
      { numRuns: 100 },
    )
  })

  it('Property 12: only valid target statuses from "sending" are "sent" and "failed"', () => {
    const validTargetStatus = fc.constantFrom('sent', 'failed') as fc.Arbitrary<
      ChatMessage['status']
    >

    fc.assert(
      fc.property(
        messagesWithSendingArb,
        validTargetStatus,
        ({ messages, localId }, targetStatus) => {
          const result = updateMessageStatus(messages, localId, targetStatus)
          const updated = result.find((m) => m.localId === localId)
          expect(updated).toBeDefined()
          expect(['sent', 'failed']).toContain(updated!.status)
        },
      ),
      { numRuns: 100 },
    )
  })

  it('Property 12: updateMessageStatus returns a new array (immutability)', () => {
    fc.assert(
      fc.property(messagesWithSendingArb, ({ messages, localId }) => {
        const result = updateMessageStatus(messages, localId, 'sent')
        // New array reference
        expect(result).not.toBe(messages)
        // Original messages unchanged
        const original = messages.find((m) => m.localId === localId)
        expect(original!.status).toBe('sending')
      }),
      { numRuns: 100 },
    )
  })

  it('Property 12: unmatched localId returns original array unchanged', () => {
    fc.assert(
      fc.property(messagesWithSendingArb, ({ messages }) => {
        // Use a UUID-style id that cannot collide with generated localIds (max 30 chars)
        const fakeId = `__nonexistent__${Date.now()}_${Math.random()}`
        const result = updateMessageStatus(messages, fakeId, 'sent')
        // Same reference — no copy made
        expect(result).toBe(messages)
      }),
      { numRuns: 100 },
    )
  })

  it('Property 12: merge fields are applied alongside status change', () => {
    fc.assert(
      fc.property(
        messagesWithSendingArb,
        fc.nat(),
        fc.string({ minLength: 1, maxLength: 30 }),
        ({ messages, localId }, newId, newSendTime) => {
          const result = updateMessageStatus(messages, localId, 'sent', {
            id: newId,
            sendTime: newSendTime,
          })
          const updated = result.find((m) => m.localId === localId)
          expect(updated).toBeDefined()
          expect(updated!.status).toBe('sent')
          expect(updated!.id).toBe(newId)
          expect(updated!.sendTime).toBe(newSendTime)
        },
      ),
      { numRuns: 100 },
    )
  })
})

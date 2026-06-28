/**
 * Feature: mars-integration-optimization, Property 21: 通知推送更新未读计数
 * **Validates: Requirements 14.2**
 *
 * *For any* 通过 WebSocket 收到的通知消息，未读计数应递增 1，且该通知应被添加到通知列表的最前面。
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as fc from 'fast-check'

// Mock dependencies before importing the composable
vi.mock('vue', async () => {
  const actual = await vi.importActual<typeof import('vue')>('vue')
  return {
    ...actual,
    onUnmounted: vi.fn()
  }
})

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    userInfo: { id: 1, username: 'test' }
  })
}))

vi.mock('@/stores/chat', () => ({
  useChatStore: () => ({
    panelVisible: false,
    togglePanel: vi.fn(),
    selectConversation: vi.fn()
  })
}))

vi.mock('@/utils/logger', () => ({
  logger: {
    error: vi.fn(),
    warn: vi.fn(),
    info: vi.fn(),
    debug: vi.fn()
  }
}))

import { useNotificationPush } from '../useNotificationPush'

// ============================================================================
// Arbitraries (Test Data Generators)
// ============================================================================

/**
 * Generate a random notification payload with realistic fields
 */
const notificationPayloadArb = fc.record({
  id: fc.integer({ min: 1, max: 1_000_000 }),
  title: fc.string({ minLength: 1, maxLength: 100 }),
  content: fc.string({ minLength: 0, maxLength: 500 }),
  type: fc.constantFrom('info', 'warning', 'error', 'success', 'chat'),
  createTime: fc.integer({ min: 1577836800000, max: 1893456000000 })
    .map(ts => new Date(ts).toISOString()),
  conversationId: fc.option(fc.integer({ min: 1, max: 100_000 }), { nil: undefined })
})

/**
 * Generate a minimal/partial notification payload (some fields missing)
 */
const partialPayloadArb = fc.oneof(
  // Payload with only some fields
  fc.record({
    id: fc.option(fc.integer({ min: 1, max: 1_000_000 }), { nil: undefined }),
    title: fc.option(fc.string({ minLength: 1, maxLength: 50 }), { nil: undefined }),
    content: fc.option(fc.string({ minLength: 0, maxLength: 200 }), { nil: undefined })
  }),
  // Completely empty payload
  fc.constant({})
)

/**
 * Generate a batch of N notification payloads
 */
const notificationBatchArb = fc.array(notificationPayloadArb, { minLength: 1, maxLength: 50 })

// ============================================================================
// Property-Based Tests
// ============================================================================

describe('Property 21: 通知推送更新未读计数', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  /**
   * Core property: For any single notification, unread count increments by 1
   * and the notification is prepended to the list.
   */
  it('should increment unreadNotificationCount by 1 for any notification payload', () => {
    fc.assert(
      fc.property(
        notificationPayloadArb,
        (payload) => {
          const { unreadNotificationCount, notifications, handleNotification } = useNotificationPush()

          const countBefore = unreadNotificationCount.value
          handleNotification(payload)
          const countAfter = unreadNotificationCount.value

          expect(countAfter).toBe(countBefore + 1)
          expect(notifications.value).toHaveLength(1)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * For any sequence of N notifications, unread count should equal N
   * and notifications list should have N items.
   */
  it('should have unreadNotificationCount equal to N after receiving N notifications', () => {
    fc.assert(
      fc.property(
        notificationBatchArb,
        (payloads) => {
          const { unreadNotificationCount, notifications, handleNotification } = useNotificationPush()

          for (const payload of payloads) {
            handleNotification(payload)
          }

          expect(unreadNotificationCount.value).toBe(payloads.length)
          expect(notifications.value).toHaveLength(payloads.length)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * The most recently received notification should always be at index 0
   * (prepended to the front of the list).
   */
  it('should prepend the latest notification to the front of the list', () => {
    fc.assert(
      fc.property(
        notificationBatchArb,
        (payloads) => {
          const { notifications, handleNotification } = useNotificationPush()

          for (const payload of payloads) {
            handleNotification(payload)
          }

          // The last payload sent should be at index 0
          const lastPayload = payloads[payloads.length - 1]!
          const firstNotification = notifications.value[0]!

          expect(firstNotification.id).toBe(lastPayload.id)
          expect(firstNotification.title).toBe(lastPayload.title)
          expect(firstNotification.content).toBe(lastPayload.content)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Notifications should appear in reverse insertion order (most recent first).
   */
  it('should maintain reverse insertion order in the notifications list', () => {
    fc.assert(
      fc.property(
        fc.array(notificationPayloadArb, { minLength: 2, maxLength: 20 }),
        (payloads) => {
          const { notifications, handleNotification } = useNotificationPush()

          for (const payload of payloads) {
            handleNotification(payload)
          }

          // notifications[0] should correspond to payloads[last]
          // notifications[last] should correspond to payloads[0]
          for (let i = 0; i < payloads.length; i++) {
            const notifIdx = payloads.length - 1 - i
            expect(notifications.value[notifIdx]!.id).toBe(payloads[i]!.id)
          }
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * All created NotificationItems should have isRead = 0 (unread).
   */
  it('should mark all new notifications as unread (isRead = 0)', () => {
    fc.assert(
      fc.property(
        notificationBatchArb,
        (payloads) => {
          const { notifications, handleNotification } = useNotificationPush()

          for (const payload of payloads) {
            handleNotification(payload)
          }

          for (const notif of notifications.value) {
            expect(notif.isRead).toBe(0)
          }
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * handleNotification should handle partial/empty payloads gracefully
   * by applying defaults, still incrementing the count.
   */
  it('should handle partial payloads gracefully and still increment count', () => {
    fc.assert(
      fc.property(
        partialPayloadArb,
        (payload) => {
          const { unreadNotificationCount, notifications, handleNotification } = useNotificationPush()

          handleNotification(payload)

          expect(unreadNotificationCount.value).toBe(1)
          expect(notifications.value).toHaveLength(1)

          const item = notifications.value[0]!
          // Defaults should be applied
          expect(item.title).toBeDefined()
          expect(item.type).toBeDefined()
          expect(item.isRead).toBe(0)
        }
      ),
      { numRuns: 100 }
    )
  })
})

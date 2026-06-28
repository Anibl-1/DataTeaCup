import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import * as fc from 'fast-check'
import { useChatStore } from '../chat'
import { useUserStore } from '../user'

// Mock the chat API — sendMessageApi never resolves during the synchronous check
vi.mock('@/api/chat', () => ({
  getConversationList: vi.fn(),
  getMessages: vi.fn(),
  sendMessage: vi.fn(() => new Promise(() => {})), // pending forever
  getOnlineUsers: vi.fn(),
}))

/**
 * Property 1: 消息发送后立即出现在列表中且状态为sending
 *
 * **Validates: Requirements 1.1**
 *
 * For any valid message content and conversation ID, after calling sendMsg,
 * the messages array immediately contains a message with status='sending'
 * and matching content.
 */
describe('Chat Store sendMsg — Property Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('Property 1: message appears immediately with status="sending" and matching content', () => {
    fc.assert(
      fc.property(
        // Generate a positive conversation ID
        fc.integer({ min: 1, max: 100_000 }),
        // Generate non-empty message content (trimmed, printable)
        fc.string({ minLength: 1, maxLength: 500 }).filter(s => s.trim().length > 0),
        (conversationId, content) => {
          // Fresh pinia per iteration
          setActivePinia(createPinia())

          // Set up user store with minimal user info so sendMsg can read senderId/senderName
          const userStore = useUserStore()
          userStore.setUserInfo({
            id: 1,
            username: 'testuser',
            nickname: 'Test User',
            email: 'test@example.com',
            roles: [],
          })

          const chatStore = useChatStore()

          // Capture messages length before
          const lengthBefore = chatStore.messages.length

          // Fire sendMsg — we don't await; we check the synchronous effect
          chatStore.sendMsg(conversationId, 'text', content)

          // --- Property assertions ---

          // 1. Exactly one new message was added
          expect(chatStore.messages.length).toBe(lengthBefore + 1)

          // 2. The last message has status 'sending'
          const lastMsg = chatStore.messages[chatStore.messages.length - 1]
          expect(lastMsg).toBeDefined()
          expect(lastMsg!.status).toBe('sending')

          // 3. Content matches
          expect(lastMsg!.content).toBe(content)

          // 4. Conversation ID matches
          expect(lastMsg!.conversationId).toBe(conversationId)

          // 5. localId is a non-empty string
          expect(typeof lastMsg!.localId).toBe('string')
          expect(lastMsg!.localId!.length).toBeGreaterThan(0)
        },
      ),
      { numRuns: 100 },
    )
  })
})

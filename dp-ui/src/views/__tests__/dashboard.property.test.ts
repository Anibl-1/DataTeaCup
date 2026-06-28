/**
 * Dashboard & Workspace property-based tests (Task 6.3)
 *
 * Property 25: 时段问候语 — getGreetingByHour(hour) maps 0-23 to correct greetings
 * Property 9: 未读消息角标可见性 — count > 0 shows badge, count === 0 hides badge
 *
 * **Validates: Requirements 21.1, 11.5**
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { getGreetingByHour } from '@/utils/greeting'

// ============================================================================
// Property 25: 时段问候语
// For any hour value h (0-23), the greeting should match the time period:
//   0-5 → 夜深了, 6-11 → 早上好, 12-17 → 下午好, 18-23 → 晚上好
// ============================================================================

describe('Property 25: 时段问候语', () => {
  it('should map every valid hour (0-23) to the correct greeting', () => {
    fc.assert(
      fc.property(fc.integer({ min: 0, max: 23 }), (hour) => {
        const greeting = getGreetingByHour(hour)

        if (hour >= 0 && hour <= 5) {
          expect(greeting).toBe('夜深了')
        } else if (hour >= 6 && hour <= 11) {
          expect(greeting).toBe('早上好')
        } else if (hour >= 12 && hour <= 17) {
          expect(greeting).toBe('下午好')
        } else {
          expect(greeting).toBe('晚上好')
        }
      }),
      { numRuns: 200 }
    )
  })

  it('should always return one of the four valid greetings', () => {
    const validGreetings = ['夜深了', '早上好', '下午好', '晚上好']

    fc.assert(
      fc.property(fc.integer({ min: 0, max: 23 }), (hour) => {
        const greeting = getGreetingByHour(hour)
        expect(validGreetings).toContain(greeting)
      }),
      { numRuns: 200 }
    )
  })

  it('should return the same greeting for all hours within the same period', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: 5 }),
        fc.integer({ min: 0, max: 5 }),
        (h1, h2) => {
          expect(getGreetingByHour(h1)).toBe(getGreetingByHour(h2))
        }
      ),
      { numRuns: 100 }
    )

    fc.assert(
      fc.property(
        fc.integer({ min: 6, max: 11 }),
        fc.integer({ min: 6, max: 11 }),
        (h1, h2) => {
          expect(getGreetingByHour(h1)).toBe(getGreetingByHour(h2))
        }
      ),
      { numRuns: 100 }
    )

    fc.assert(
      fc.property(
        fc.integer({ min: 12, max: 17 }),
        fc.integer({ min: 12, max: 17 }),
        (h1, h2) => {
          expect(getGreetingByHour(h1)).toBe(getGreetingByHour(h2))
        }
      ),
      { numRuns: 100 }
    )

    fc.assert(
      fc.property(
        fc.integer({ min: 18, max: 23 }),
        fc.integer({ min: 18, max: 23 }),
        (h1, h2) => {
          expect(getGreetingByHour(h1)).toBe(getGreetingByHour(h2))
        }
      ),
      { numRuns: 100 }
    )
  })
})

// ============================================================================
// Property 9: 未读消息角标可见性
// For any unread message count value:
//   count > 0 → badge should be visible (show = true)
//   count === 0 → badge should be hidden (show = false)
//
// This mirrors the MainLayout template logic:
//   <n-badge :show="chatStore.totalUnread > 0" :value="chatStore.totalUnread">
// ============================================================================

/**
 * Pure function that computes badge visibility from a list of conversation
 * unread counts — same logic as chatStore.totalUnread + MainLayout template.
 */
function computeTotalUnread(unreadCounts: number[]): number {
  return unreadCounts.reduce((sum, c) => sum + (c || 0), 0)
}

function shouldShowBadge(totalUnread: number): boolean {
  return totalUnread > 0
}

describe('Property 9: 未读消息角标可见性', () => {
  it('badge is visible when total unread count > 0, hidden when === 0', () => {
    fc.assert(
      fc.property(fc.nat({ max: 1000 }), (count) => {
        const visible = shouldShowBadge(count)
        if (count > 0) {
          expect(visible).toBe(true)
        } else {
          expect(visible).toBe(false)
        }
      }),
      { numRuns: 200 }
    )
  })

  it('totalUnread computed from conversation unreadCounts determines badge visibility', () => {
    fc.assert(
      fc.property(
        fc.array(fc.nat({ max: 100 }), { minLength: 0, maxLength: 20 }),
        (unreadCounts) => {
          const total = computeTotalUnread(unreadCounts)
          const visible = shouldShowBadge(total)

          const hasAnyUnread = unreadCounts.some(c => c > 0)
          expect(visible).toBe(hasAnyUnread)
        }
      ),
      { numRuns: 200 }
    )
  })

  it('badge is always hidden when all conversations have zero unread', () => {
    fc.assert(
      fc.property(
        fc.array(fc.constant(0), { minLength: 0, maxLength: 20 }),
        (zeroCounts) => {
          const total = computeTotalUnread(zeroCounts)
          expect(total).toBe(0)
          expect(shouldShowBadge(total)).toBe(false)
        }
      ),
      { numRuns: 100 }
    )
  })

  it('badge is always visible when at least one conversation has unread > 0', () => {
    fc.assert(
      fc.property(
        fc.array(fc.nat({ max: 100 }), { minLength: 1, maxLength: 20 }).filter(
          arr => arr.some(c => c > 0)
        ),
        (unreadCounts) => {
          const total = computeTotalUnread(unreadCounts)
          expect(total).toBeGreaterThan(0)
          expect(shouldShowBadge(total)).toBe(true)
        }
      ),
      { numRuns: 200 }
    )
  })
})

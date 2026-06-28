/**
 * Feature: mars-integration-optimization, Property 20: 在线状态指示器颜色映射
 * **Validates: Requirements 13.3**
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { getStatusColor } from '../../../utils/chatTimeFormat'

// --- Generators ---

/** Generate a random status value from the valid set */
const arbStatus = fc.constantFrom<'online' | 'offline'>('online', 'offline')

// --- Property 20: 在线状态指示器颜色映射 ---

describe('Property 20: 在线状态指示器颜色映射', () => {
  it('online status always maps to #10B981', () => {
    fc.assert(
      fc.property(fc.constant('online' as const), (status) => {
        expect(getStatusColor(status)).toBe('#10B981')
      }),
      { numRuns: 100 }
    )
  })

  it('offline status always maps to #9CA3AF', () => {
    fc.assert(
      fc.property(fc.constant('offline' as const), (status) => {
        expect(getStatusColor(status)).toBe('#9CA3AF')
      }),
      { numRuns: 100 }
    )
  })

  it('for any status in {online, offline}, the result is always one of the two valid colors', () => {
    fc.assert(
      fc.property(arbStatus, (status) => {
        const color = getStatusColor(status)
        expect(['#10B981', '#9CA3AF']).toContain(color)
      }),
      { numRuns: 200 }
    )
  })
})

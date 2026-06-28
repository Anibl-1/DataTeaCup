/**
 * Feature: mars-integration-optimization, Property 17: 聊天时间格式化
 * **Validates: Requirements 12.4**
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { formatChatTime } from '../chatTimeFormat'

// --- Generators ---

/**
 * Generate a date on the same day as the given reference date.
 */
function arbSameDayDate(now: Date): fc.Arbitrary<Date> {
  return fc.record({
    hour: fc.integer({ min: 0, max: 23 }),
    minute: fc.integer({ min: 0, max: 59 }),
    second: fc.integer({ min: 0, max: 59 }),
  }).map(({ hour, minute, second }) => {
    const d = new Date(now)
    d.setHours(hour, minute, second, 0)
    return d
  })
}

/**
 * Generate a date that is NOT on the same day as the given reference date.
 * Offsets by at least 1 day (positive or negative).
 */
function arbDifferentDayDate(now: Date): fc.Arbitrary<Date> {
  return fc.integer({ min: 1, max: 365 }).chain((dayOffset) =>
    fc.record({
      direction: fc.constantFrom(-1, 1),
      hour: fc.integer({ min: 0, max: 23 }),
      minute: fc.integer({ min: 0, max: 59 }),
    }).map(({ direction, hour, minute }) => {
      const d = new Date(now)
      d.setDate(d.getDate() + dayOffset * direction)
      d.setHours(hour, minute, 0, 0)
      return d
    })
  )
}

const HH_MM_PATTERN = /^\d{2}:\d{2}$/
const MM_DD_HH_MM_PATTERN = /^\d{2}-\d{2} \d{2}:\d{2}$/

// Fixed reference time for deterministic tests
const REFERENCE_NOW = new Date('2025-06-15T12:00:00')

// --- Property 17: 聊天时间格式化 ---

describe('Property 17: 聊天时间格式化', () => {
  it('same-day dates produce HH:mm format', () => {
    fc.assert(
      fc.property(arbSameDayDate(REFERENCE_NOW), (date) => {
        const result = formatChatTime(date.toISOString(), REFERENCE_NOW)
        expect(result).toMatch(HH_MM_PATTERN)
      }),
      { numRuns: 200 }
    )
  })

  it('same-day dates produce correct hour and minute values', () => {
    fc.assert(
      fc.property(arbSameDayDate(REFERENCE_NOW), (date) => {
        const result = formatChatTime(date.toISOString(), REFERENCE_NOW)
        const expectedHH = String(date.getHours()).padStart(2, '0')
        const expectedMM = String(date.getMinutes()).padStart(2, '0')
        expect(result).toBe(`${expectedHH}:${expectedMM}`)
      }),
      { numRuns: 200 }
    )
  })

  it('different-day dates produce MM-DD HH:mm format', () => {
    fc.assert(
      fc.property(arbDifferentDayDate(REFERENCE_NOW), (date) => {
        const result = formatChatTime(date.toISOString(), REFERENCE_NOW)
        expect(result).toMatch(MM_DD_HH_MM_PATTERN)
      }),
      { numRuns: 200 }
    )
  })

  it('different-day dates produce correct month, day, hour, and minute values', () => {
    fc.assert(
      fc.property(arbDifferentDayDate(REFERENCE_NOW), (date) => {
        const result = formatChatTime(date.toISOString(), REFERENCE_NOW)
        const expectedMonth = String(date.getMonth() + 1).padStart(2, '0')
        const expectedDay = String(date.getDate()).padStart(2, '0')
        const expectedHH = String(date.getHours()).padStart(2, '0')
        const expectedMM = String(date.getMinutes()).padStart(2, '0')
        expect(result).toBe(`${expectedMonth}-${expectedDay} ${expectedHH}:${expectedMM}`)
      }),
      { numRuns: 200 }
    )
  })

  it('invalid date strings return empty string', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 1 }).filter((s) => isNaN(new Date(s).getTime())),
        (invalidStr) => {
          const result = formatChatTime(invalidStr, REFERENCE_NOW)
          expect(result).toBe('')
        }
      ),
      { numRuns: 200 }
    )
  })
})

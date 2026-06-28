/**
 * 慢查询日期范围筛选 属性测试
 * Feature: ui-deep-polish, Property 5: 慢查询日期范围筛选
 *
 * **Validates: Requirements 7.4**
 *
 * For any date range [startDate, endDate] and slow query dataset,
 * the filtered results should only contain records whose query time
 * falls within that date range.
 *
 * We test the pure date range filtering logic as an extracted function.
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'

// ============================================================================
// Types & Pure functions extracted for testability
// ============================================================================

interface SlowQueryRecord {
  queryTime: string // ISO date string
  sqlText: string
  executionTime: number
}

/**
 * Filter slow query records by date range [startDate, endDate].
 * Returns only records whose queryTime falls within the inclusive range.
 */
function filterByDateRange(
  records: SlowQueryRecord[],
  startDate: Date,
  endDate: Date,
): SlowQueryRecord[] {
  return records.filter((r) => {
    const t = new Date(r.queryTime).getTime()
    return t >= startDate.getTime() && t <= endDate.getTime()
  })
}

// ============================================================================
// Arbitraries (测试数据生成器)
// ============================================================================

/** Generate a Date within a reasonable range (2020-01-01 to 2025-12-31) */
const dateArb = fc
  .integer({
    min: new Date('2020-01-01T00:00:00Z').getTime(),
    max: new Date('2025-12-31T23:59:59Z').getTime(),
  })
  .map((ts) => new Date(ts))

/** Generate an ordered date range [startDate, endDate] where start <= end */
const dateRangeArb = fc
  .tuple(dateArb, dateArb)
  .map(([a, b]) => (a.getTime() <= b.getTime() ? [a, b] : [b, a]) as [Date, Date])

/** Generate a simple SQL text string */
const sqlTextArb = fc
  .array(
    fc.constantFrom(
      ...'SELECT FROM WHERE AND OR JOIN ON INSERT UPDATE DELETE table column'.split(' '),
    ),
    { minLength: 1, maxLength: 5 },
  )
  .map((words) => words.join(' '))

/** Generate a single SlowQueryRecord with a random queryTime */
const slowQueryRecordArb = fc
  .tuple(dateArb, sqlTextArb, fc.integer({ min: 100, max: 60000 }))
  .map(([date, sqlText, executionTime]) => ({
    queryTime: date.toISOString(),
    sqlText,
    executionTime,
  }))

/** Generate an array of SlowQueryRecords */
const slowQueryListArb = fc.array(slowQueryRecordArb, { minLength: 0, maxLength: 30 })

// ============================================================================
// Property-Based Tests
// ============================================================================

describe('SlowQueryAnalysis — Property Tests', () => {
  /**
   * Property 5: 慢查询日期范围筛选
   *
   * **Validates: Requirements 7.4**
   * **Feature: ui-deep-polish, Property 5: 慢查询日期范围筛选**
   */
  describe('Property 5: Date range filtering returns only records within [startDate, endDate]', () => {
    it('all filtered records have queryTime within the date range', () => {
      fc.assert(
        fc.property(slowQueryListArb, dateRangeArb, (records, [startDate, endDate]) => {
          const filtered = filterByDateRange(records, startDate, endDate)

          for (const record of filtered) {
            const t = new Date(record.queryTime).getTime()
            expect(t).toBeGreaterThanOrEqual(startDate.getTime())
            expect(t).toBeLessThanOrEqual(endDate.getTime())
          }
        }),
        { numRuns: 100 },
      )
    })

    it('no records outside the date range are included in the result', () => {
      fc.assert(
        fc.property(slowQueryListArb, dateRangeArb, (records, [startDate, endDate]) => {
          const filtered = filterByDateRange(records, startDate, endDate)
          const excluded = records.filter((r) => {
            const t = new Date(r.queryTime).getTime()
            return t < startDate.getTime() || t > endDate.getTime()
          })

          // None of the excluded records should appear in filtered
          for (const excl of excluded) {
            expect(filtered).not.toContain(excl)
          }
        }),
        { numRuns: 100 },
      )
    })

    it('filtered count equals count of records within range', () => {
      fc.assert(
        fc.property(slowQueryListArb, dateRangeArb, (records, [startDate, endDate]) => {
          const filtered = filterByDateRange(records, startDate, endDate)
          const expectedCount = records.filter((r) => {
            const t = new Date(r.queryTime).getTime()
            return t >= startDate.getTime() && t <= endDate.getTime()
          }).length

          expect(filtered.length).toBe(expectedCount)
        }),
        { numRuns: 100 },
      )
    })

    it('filtering is a subset: every filtered record exists in the original list', () => {
      fc.assert(
        fc.property(slowQueryListArb, dateRangeArb, (records, [startDate, endDate]) => {
          const filtered = filterByDateRange(records, startDate, endDate)

          for (const record of filtered) {
            expect(records).toContain(record)
          }
        }),
        { numRuns: 100 },
      )
    })

    it('filtering with range covering all records returns the full list', () => {
      fc.assert(
        fc.property(slowQueryListArb, (records) => {
          if (records.length === 0) return // skip empty

          const allStart = new Date('2019-01-01T00:00:00Z')
          const allEnd = new Date('2026-12-31T23:59:59Z')
          const filtered = filterByDateRange(records, allStart, allEnd)

          expect(filtered.length).toBe(records.length)
        }),
        { numRuns: 100 },
      )
    })
  })
})

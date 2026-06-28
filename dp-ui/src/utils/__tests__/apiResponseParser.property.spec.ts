/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import {
  parseListResponse,
  serializeListResponse,
  deserializeListResponse,
  type StandardListResponse,
} from '../apiResponseParser'

/**
 * Property 7: parseListResponse 格式归一化
 *
 * **Validates: Requirements 4.3, 4.4**
 * **Feature: core-modules-deep-optimization, Property 7: parseListResponse 格式归一化**
 *
 * For any API 响应对象（无论其列表数据位于 data.records、data.list、data.rows、data 或顶层 list 字段），
 * parseListResponse 的返回值应始终包含 list（数组类型）和 total（数字类型）字段。
 */
describe('apiResponseParser — Property Tests', () => {
  // Generator for an array of simple JSON-serializable items
  const itemListArb = fc.array(fc.jsonValue(), { minLength: 0, maxLength: 20 })

  // Generator for a valid total number
  const totalArb = fc.nat({ max: 100000 })

  // Generator: { data: { records: [...], total } }
  const dataRecordsArb = fc.tuple(itemListArb, totalArb).map(([list, total]) => ({
    data: { records: list, total },
  }))

  // Generator: { data: { list: [...], total } }
  const dataListArb = fc.tuple(itemListArb, totalArb).map(([list, total]) => ({
    data: { list, total },
  }))

  // Generator: { data: { rows: [...], total } }
  const dataRowsArb = fc.tuple(itemListArb, totalArb).map(([list, total]) => ({
    data: { rows: list, total },
  }))

  // Generator: { data: [...] } (data is array)
  const dataArrayArb = itemListArb.map((list) => ({
    data: list,
  }))

  // Generator: { list: [...], total } (top-level list)
  const topLevelListArb = fc.tuple(itemListArb, totalArb).map(([list, total]) => ({
    list,
    total,
  }))

  // Generator: direct array
  const directArrayArb = itemListArb

  // Generator: null / undefined
  const nullishArb = fc.constantFrom(null, undefined)

  // Combined generator for all supported API response formats
  const apiResponseArb = fc.oneof(
    dataRecordsArb,
    dataListArb,
    dataRowsArb,
    dataArrayArb,
    topLevelListArb,
    directArrayArb,
    nullishArb,
  )

  it('Property 7: parseListResponse always returns { list: Array, total: number } regardless of input format', () => {
    fc.assert(
      fc.property(apiResponseArb, (response) => {
        const result = parseListResponse(response)

        // list must be an array
        expect(Array.isArray(result.list)).toBe(true)

        // total must be a number and not NaN
        expect(typeof result.total).toBe('number')
        expect(Number.isNaN(result.total)).toBe(false)

        // total must be non-negative
        expect(result.total).toBeGreaterThanOrEqual(0)
      }),
      { numRuns: 200 },
    )
  })

  it('Property 7 (supplemental): parseListResponse preserves list content for known formats', () => {
    // For formats that carry an explicit list, the parsed list should match the original
    const knownFormatArb = fc.oneof(
      dataRecordsArb,
      dataListArb,
      dataRowsArb,
      topLevelListArb,
    )

    fc.assert(
      fc.property(knownFormatArb, (response: any) => {
        const result = parseListResponse(response)

        // Extract the original list from whichever field it was in
        const inner = response.data ?? response
        const originalList = inner.records ?? inner.list ?? inner.rows
        expect(result.list).toEqual(originalList)
      }),
      { numRuns: 100 },
    )
  })

  /**
   * Property 7: apiResponseParser 列表解析一致性
   *
   * **Validates: Requirements 10.5**
   * **Feature: frontend-comprehensive-optimization, Property 7: apiResponseParser 列表解析一致性**
   *
   * 对于任意合法分页响应，解析后 list 长度与原始 list 长度一致且 total 为非负整数。
   */
  it('Property 7: apiResponseParser 列表解析一致性 — list length consistent and total is non-negative integer', () => {
    // Generator for paginated responses with explicit list and total
    const paginatedResponseArb = fc.oneof(
      // { data: { records: [...], total } }
      fc.tuple(itemListArb, totalArb).map(([list, total]) => ({
        response: { data: { records: list, total } },
        originalLength: list.length,
      })),
      // { data: { list: [...], total } }
      fc.tuple(itemListArb, totalArb).map(([list, total]) => ({
        response: { data: { list, total } },
        originalLength: list.length,
      })),
      // { data: { rows: [...], total } }
      fc.tuple(itemListArb, totalArb).map(([list, total]) => ({
        response: { data: { rows: list, total } },
        originalLength: list.length,
      })),
      // { data: [...] } (data is array)
      itemListArb.map((list) => ({
        response: { data: list },
        originalLength: list.length,
      })),
      // { list: [...], total } (top-level)
      fc.tuple(itemListArb, totalArb).map(([list, total]) => ({
        response: { list, total },
        originalLength: list.length,
      })),
      // { records: [...], total } (top-level records)
      fc.tuple(itemListArb, totalArb).map(([list, total]) => ({
        response: { records: list, total },
        originalLength: list.length,
      })),
      // { rows: [...], total } (top-level rows)
      fc.tuple(itemListArb, totalArb).map(([list, total]) => ({
        response: { rows: list, total },
        originalLength: list.length,
      })),
      // Direct array
      itemListArb.map((list) => ({
        response: list,
        originalLength: list.length,
      })),
    )

    fc.assert(
      fc.property(paginatedResponseArb, ({ response, originalLength }) => {
        const result = parseListResponse(response)

        // Parsed list length must match original list length
        expect(result.list.length).toBe(originalLength)

        // total must be a non-negative integer
        expect(Number.isInteger(result.total)).toBe(true)
        expect(result.total).toBeGreaterThanOrEqual(0)
      }),
      { numRuns: 100 },
    )
  })

  /**
   * Property 8: API 列表响应序列化往返一致性
   *
   * **Validates: Requirements 4.5**
   * **Feature: core-modules-deep-optimization, Property 8: API 列表响应序列化往返一致性**
   *
   * For any 有效的 StandardListResponse<T> 对象，执行 serializeListResponse 然后
   * deserializeListResponse 应产生与原始对象深度相等的结果。
   */

  // Generator for JSON-safe values (excludes -0 which JSON.stringify converts to 0)
  const jsonSafeValue = fc.jsonValue().map((v) => JSON.parse(JSON.stringify(v)))

  // Generator for StandardListResponse with JSON-safe items
  const standardListResponseArb: fc.Arbitrary<StandardListResponse<unknown>> = fc.record({
    list: fc.array(jsonSafeValue, { minLength: 0, maxLength: 30 }),
    total: fc.nat({ max: 100000 }),
  })

  it('Property 8: serialize then deserialize yields deep-equal result', () => {
    fc.assert(
      fc.property(standardListResponseArb, (original) => {
        const serialized = serializeListResponse(original)
        const deserialized = deserializeListResponse(serialized)
        expect(deserialized).toEqual(original)
      }),
      { numRuns: 200 },
    )
  })
})

import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as fc from 'fast-check'

// Mock logger to avoid side effects
vi.mock('@/utils/logger', () => ({
  logger: {
    error: vi.fn(),
    warn: vi.fn(),
    info: vi.fn(),
    debug: vi.fn(),
  },
}))

// Mock onMounted since we use immediate: false
vi.mock('vue', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue')>()
  return {
    ...actual,
    onMounted: vi.fn(), // no-op so it never fires
  }
})

import { useDataTable } from '../useDataTable'

/**
 * Property 1: useDataTable 重置恢复初始状态
 *
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4**
 * **Feature: core-modules-deep-optimization, Property 1: useDataTable 重置恢复初始状态**
 *
 * For any useDataTable 实例，在经过任意序列的状态变更（分页、排序、筛选、选中）后，
 * 调用 reset() 应将所有状态恢复为初始值：page=1、sortField=''、sortOrder=''、
 * filters={}、checkedRowKeys=[]。
 */

/**
 * Property 2: useDataTable 数据加载正确性
 *
 * **Validates: Requirements 1.5**
 * **Feature: core-modules-deep-optimization, Property 2: useDataTable 数据加载正确性**
 *
 * For any fetchData 函数返回的 { list, total } 结果，调用 load() 后
 * useDataTable 的 data 应等于 list，total 应等于返回的 total 值。
 */

describe('useDataTable — Property Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // --- Generators ---

  /** Arbitrary page number (reasonable range) */
  const pageArb = fc.integer({ min: 1, max: 500 })

  /** Arbitrary sort field name */
  const sortFieldArb = fc.string({ minLength: 1, maxLength: 10, unit: 'grapheme' })

  /** Arbitrary sort order for Naive UI sorter format */
  const sortOrderArb = fc.constantFrom('ascend', 'descend')

  /** Arbitrary filter key-value pairs */
  const filtersArb = fc.dictionary(
    fc.string({ minLength: 1, maxLength: 6, unit: 'grapheme' }),
    fc.oneof(fc.string({ maxLength: 10 }), fc.integer(), fc.boolean()),
    { minKeys: 0, maxKeys: 5 },
  )

  /** Arbitrary checked row keys */
  const checkedKeysArb = fc.array(
    fc.oneof(fc.integer({ min: 1, max: 1000 }), fc.string({ minLength: 1, maxLength: 8 })),
    { minLength: 0, maxLength: 10 },
  )

  /** Arbitrary state mutation action */
  const mutationArb = fc.oneof(
    // Change page
    pageArb.map((page) => ({ type: 'page' as const, page })),
    // Change sort
    fc.tuple(sortFieldArb, sortOrderArb).map(([field, order]) => ({
      type: 'sort' as const,
      field,
      order,
    })),
    // Change filters
    filtersArb.map((filters) => ({ type: 'filter' as const, filters })),
    // Change checked keys
    checkedKeysArb.map((keys) => ({ type: 'check' as const, keys })),
  )

  /** Sequence of mutations */
  const mutationSeqArb = fc.array(mutationArb, { minLength: 1, maxLength: 15 })

  it('Property 1: reset() restores initial state after arbitrary state mutations', async () => {
    await fc.assert(
      fc.asyncProperty(mutationSeqArb, async (mutations) => {
        // Create a mock apiFn that always resolves with empty data
        const apiFn = vi.fn().mockResolvedValue({ data: { list: [], total: 0 } })

        const table = useDataTable<Record<string, unknown>>({
          apiFn,
          immediate: false,
          defaultFilters: {},
        })

        // Apply all mutations
        for (const m of mutations) {
          switch (m.type) {
            case 'page':
              table.handlePageChange(m.page)
              break
            case 'sort':
              table.handleSorterChange({ columnKey: m.field, order: m.order })
              break
            case 'filter':
              table.searchParams.value = { ...m.filters }
              break
            case 'check':
              table.handleCheck(m.keys)
              break
          }
        }

        // Wait for any pending async operations from handlePageChange/handleSorterChange
        await vi.dynamicImportSettled?.() // safe no-op if not available
        // Give promises a tick to settle
        await new Promise((r) => setTimeout(r, 0))

        // Now reset
        await table.reset()

        // Verify all state is back to initial values
        expect(table.pagination.value.page).toBe(1)
        expect(table.searchParams.value).toEqual({})
        expect(table.checkedRowKeys.value).toEqual([])

        // Verify the last call to apiFn (from reset -> load) has no sort params
        const lastCall = apiFn.mock.calls[apiFn.mock.calls.length - 1][0]
        expect(lastCall.page).toBe(1)
        expect(lastCall.sortField).toBeUndefined()
        expect(lastCall.sortOrder).toBeUndefined()
      }),
      { numRuns: 100 },
    )
  })

  it('Property 1 (with defaultFilters): reset() restores searchParams to defaultFilters', async () => {
    await fc.assert(
      fc.asyncProperty(mutationSeqArb, filtersArb, async (mutations, defaultFilters) => {
        const apiFn = vi.fn().mockResolvedValue({ data: { list: [], total: 0 } })

        const table = useDataTable<Record<string, unknown>>({
          apiFn,
          immediate: false,
          defaultFilters,
        })

        // Apply mutations
        for (const m of mutations) {
          if (m.type === 'filter') {
            table.searchParams.value = { ...m.filters }
          } else if (m.type === 'check') {
            table.handleCheck(m.keys)
          }
        }

        await table.reset()

        // searchParams should be restored to defaultFilters (deep copy)
        expect(table.searchParams.value).toEqual(defaultFilters)
        expect(table.checkedRowKeys.value).toEqual([])
        expect(table.pagination.value.page).toBe(1)
      }),
      { numRuns: 100 },
    )
  })

  /** Generator for simple row objects */
  const rowArb = fc.record({
    id: fc.integer({ min: 1, max: 100000 }),
    name: fc.string({ minLength: 0, maxLength: 20 }),
    value: fc.oneof(fc.integer(), fc.double({ noNaN: true }), fc.string({ maxLength: 10 })),
  })

  const listArb = fc.array(rowArb, { minLength: 0, maxLength: 50 })
  const totalArb = fc.nat({ max: 100000 })

  it('Property 2: load() correctly populates data and total from apiFn response', async () => {
    await fc.assert(
      fc.asyncProperty(listArb, totalArb, async (list, total) => {
        const apiFn = vi.fn().mockResolvedValue({
          data: { list, total },
        })

        const table = useDataTable<Record<string, unknown>>({
          apiFn,
          immediate: false,
        })

        await table.load()

        expect(table.data.value).toEqual(list)
        expect(table.total.value).toBe(total)
        expect(table.loading.value).toBe(false)
      }),
      { numRuns: 100 },
    )
  })

  it('Property 2 (records format): load() works with data.records format', async () => {
    await fc.assert(
      fc.asyncProperty(listArb, totalArb, async (list, total) => {
        const apiFn = vi.fn().mockResolvedValue({
          data: { records: list, total },
        })

        const table = useDataTable<Record<string, unknown>>({
          apiFn,
          immediate: false,
        })

        await table.load()

        expect(table.data.value).toEqual(list)
        expect(table.total.value).toBe(total)
      }),
      { numRuns: 100 },
    )
  })

  it('Property 2 (error handling): load() sets data=[] and total=0 on failure', async () => {
    const apiFn = vi.fn().mockRejectedValue(new Error('network error'))

    const table = useDataTable<Record<string, unknown>>({
      apiFn,
      immediate: false,
    })

    await table.load()

    expect(table.data.value).toEqual([])
    expect(table.total.value).toBe(0)
    expect(table.loading.value).toBe(false)
  })
})


/**
 * Property 4: useTable 状态管理一致性
 *
 * **Validates: Requirements 10.1, 10.2**
 * **Feature: frontend-comprehensive-optimization, Property 4: useTable 状态管理一致性**
 *
 * 对于任意有效页码值 n（正整数），调用 handlePageChange(n) 后 pagination.page 应等于 n。
 * 对于任意有效排序参数（field, order），调用 handleSorterChange 后 sortState 应反映传入的 field 和 order。
 */
describe('useDataTable — Property 4: useTable 状态管理一致性', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  /** Arbitrary valid page number (positive integer) */
  const validPageArb = fc.integer({ min: 1, max: 10000 })

  /** Arbitrary valid sort field */
  const validSortFieldArb = fc.string({ minLength: 1, maxLength: 20, unit: 'grapheme' })

  /** Arbitrary valid Naive UI sort order */
  const validSortOrderArb = fc.constantFrom('ascend' as const, 'descend' as const)

  it('Property 4a: handlePageChange(n) sets pagination.page to n for any positive integer n', async () => {
    await fc.assert(
      fc.asyncProperty(validPageArb, async (page) => {
        const apiFn = vi.fn().mockResolvedValue({ data: { list: [], total: 0 } })

        const table = useDataTable<Record<string, unknown>>({
          apiFn,
          immediate: false,
        })

        table.handlePageChange(page)

        // Allow async load() triggered by handlePageChange to settle
        await new Promise((r) => setTimeout(r, 0))

        // pagination.page should equal the passed value
        expect(table.pagination.value.page).toBe(page)

        // The apiFn should have been called with the correct page
        const lastCall = apiFn.mock.calls[apiFn.mock.calls.length - 1][0]
        expect(lastCall.page).toBe(page)
      }),
      { numRuns: 100 },
    )
  })

  it('Property 4b: handleSorterChange reflects passed field and order in API params', async () => {
    await fc.assert(
      fc.asyncProperty(validSortFieldArb, validSortOrderArb, async (field, order) => {
        const apiFn = vi.fn().mockResolvedValue({ data: { list: [], total: 0 } })

        const table = useDataTable<Record<string, unknown>>({
          apiFn,
          immediate: false,
        })

        table.handleSorterChange({ columnKey: field, order })

        // Allow async load() triggered by handleSorterChange to settle
        await new Promise((r) => setTimeout(r, 0))

        // The expected mapped order: 'ascend' -> 'asc', 'descend' -> 'desc'
        const expectedOrder = order === 'ascend' ? 'asc' : 'desc'

        // Verify the apiFn was called with correct sort params
        const lastCall = apiFn.mock.calls[apiFn.mock.calls.length - 1][0]
        expect(lastCall.sortField).toBe(field)
        expect(lastCall.sortOrder).toBe(expectedOrder)

        // Page should reset to 1 after sort change
        expect(lastCall.page).toBe(1)
        expect(table.pagination.value.page).toBe(1)
      }),
      { numRuns: 100 },
    )
  })

  it('Property 4c: handleSorterChange with null/no order clears sort state', async () => {
    await fc.assert(
      fc.asyncProperty(validSortFieldArb, validSortOrderArb, async (field, order) => {
        const apiFn = vi.fn().mockResolvedValue({ data: { list: [], total: 0 } })

        const table = useDataTable<Record<string, unknown>>({
          apiFn,
          immediate: false,
        })

        // First set a sort
        table.handleSorterChange({ columnKey: field, order })
        await new Promise((r) => setTimeout(r, 0))

        // Then clear the sort by passing no order
        table.handleSorterChange({ columnKey: field, order: false })
        await new Promise((r) => setTimeout(r, 0))

        // Sort params should be cleared
        const lastCall = apiFn.mock.calls[apiFn.mock.calls.length - 1][0]
        expect(lastCall.sortField).toBeUndefined()
        expect(lastCall.sortOrder).toBeUndefined()
      }),
      { numRuns: 100 },
    )
  })

  it('Property 4d: sequential page changes always reflect the latest value', async () => {
    await fc.assert(
      fc.asyncProperty(
        fc.array(validPageArb, { minLength: 2, maxLength: 10 }),
        async (pages) => {
          const apiFn = vi.fn().mockResolvedValue({ data: { list: [], total: 0 } })

          const table = useDataTable<Record<string, unknown>>({
            apiFn,
            immediate: false,
          })

          // Apply all page changes sequentially
          for (const page of pages) {
            table.handlePageChange(page)
          }

          await new Promise((r) => setTimeout(r, 0))

          // The final pagination.page should be the last page value
          const lastPage = pages[pages.length - 1]
          expect(table.pagination.value.page).toBe(lastPage)
        },
      ),
      { numRuns: 100 },
    )
  })
})

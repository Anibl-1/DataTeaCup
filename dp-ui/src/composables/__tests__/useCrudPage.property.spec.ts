import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as fc from 'fast-check'

/**
 * Property 1: useCrudPage 自定义处理器覆盖
 *
 * **Validates: Requirements 1.4**
 * **Feature: frontend-comprehensive-optimization, Property 1: useCrudPage 自定义处理器覆盖**
 *
 * 对于任意自定义删除处理函数，调用 handleDelete 时应执行自定义函数而非默认 deleteApi。
 */

// Mock logger to avoid side effects
vi.mock('@/utils/logger', () => ({
  logger: {
    error: vi.fn(),
    warn: vi.fn(),
    info: vi.fn(),
    debug: vi.fn(),
  },
}))

// Mock message/dialog to avoid Naive UI dependency
vi.mock('@/utils/message', () => ({
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
  },
  dialog: {
    warning: vi.fn(),
    success: vi.fn(),
    error: vi.fn(),
  },
}))

// Mock apiResponseParser
vi.mock('@/utils/apiResponseParser', () => ({
  parseListResponse: vi.fn(() => ({ list: [], total: 0 })),
}))

import { useCrudPage } from '../useCrudPage'

describe('useCrudPage — Property Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // --- Generators ---

  /** Arbitrary id: either a positive integer or a non-empty string */
  const idArb = fc.oneof(
    fc.integer({ min: 1, max: 100000 }),
    fc.string({ minLength: 1, maxLength: 20, unit: 'grapheme' }),
  )

  // --- Property 1: Custom onDelete handler overrides default deleteApi ---

  it('Property 1: handleDelete executes custom onDelete and does NOT call deleteApi', async () => {
    await fc.assert(
      fc.asyncProperty(idArb, async (id) => {
        const deleteApi = vi.fn().mockResolvedValue(undefined)
        const onDelete = vi.fn().mockResolvedValue(undefined)

        const crud = useCrudPage({
          listApi: vi.fn().mockResolvedValue({ data: { list: [], total: 0 } }),
          createApi: vi.fn().mockResolvedValue(undefined),
          updateApi: vi.fn().mockResolvedValue(undefined),
          deleteApi,
          defaultFormData: () => ({} as Record<string, unknown>),
          onDelete,
        })

        await crud.handleDelete(id)

        // Custom onDelete should be called exactly once with the given id
        expect(onDelete).toHaveBeenCalledTimes(1)
        expect(onDelete).toHaveBeenCalledWith(id)

        // Default deleteApi should NOT be called
        expect(deleteApi).not.toHaveBeenCalled()
      }),
      { numRuns: 100 },
    )
  })

  it('Property 1: handleDelete passes the exact id to the custom onDelete handler', async () => {
    await fc.assert(
      fc.asyncProperty(idArb, async (id) => {
        const receivedIds: (number | string)[] = []
        const onDelete = vi.fn(async (receivedId: number | string) => {
          receivedIds.push(receivedId)
        })

        const crud = useCrudPage({
          listApi: vi.fn().mockResolvedValue({ data: { list: [], total: 0 } }),
          createApi: vi.fn().mockResolvedValue(undefined),
          updateApi: vi.fn().mockResolvedValue(undefined),
          deleteApi: vi.fn().mockResolvedValue(undefined),
          defaultFormData: () => ({} as Record<string, unknown>),
          onDelete,
        })

        await crud.handleDelete(id)

        // The id received by onDelete should be exactly the id passed to handleDelete
        expect(receivedIds).toHaveLength(1)
        expect(receivedIds[0]).toBe(id)
      }),
      { numRuns: 100 },
    )
  })
})

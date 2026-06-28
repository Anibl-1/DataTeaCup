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

import { useFormModal } from '../useFormModal'

/**
 * Property 3: useFormModal 打开/关闭往返一致性
 *
 * **Validates: Requirements 2.2, 2.3, 2.4**
 * **Feature: core-modules-deep-optimization, Property 3: useFormModal 打开/关闭往返一致性**
 *
 * For any 默认表单数据和任意记录数据，无论执行 openCreate() 还是 openEdit(record)，
 * 调用 close() 后 formData 应恢复为默认值，visible 应为 false。
 */

/**
 * Property 4: useFormModal 提交路由正确性
 *
 * **Validates: Requirements 2.5**
 * **Feature: core-modules-deep-optimization, Property 4: useFormModal 提交路由正确性**
 *
 * For any 表单数据，当 mode 为 'create' 时 submit() 应调用 createFn，
 * 当 mode 为 'edit' 时 submit() 应调用 updateFn，且传入的参数应为当前 formData。
 */

describe('useFormModal — Property Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // --- Generators ---

  /** Arbitrary simple form data object (JSON-serializable for deep clone) */
  const formFieldValueArb = fc.oneof(
    fc.string({ maxLength: 20 }),
    fc.integer(),
    fc.boolean(),
    fc.constant(null),
  )

  const formDataArb = fc.dictionary(
    fc.string({ minLength: 1, maxLength: 8, unit: 'grapheme' }),
    formFieldValueArb,
    { minKeys: 1, maxKeys: 6 },
  ) as fc.Arbitrary<Record<string, unknown>>

  /** Arbitrary partial record data for openEdit */
  const recordDataArb = fc.dictionary(
    fc.string({ minLength: 1, maxLength: 8, unit: 'grapheme' }),
    formFieldValueArb,
    { minKeys: 1, maxKeys: 6 },
  ) as fc.Arbitrary<Record<string, unknown>>

  /** Arbitrary open action: either openCreate or openEdit */
  const openActionArb = fc.oneof(
    fc.constant({ type: 'create' as const }),
    recordDataArb.map((record) => ({ type: 'edit' as const, record })),
  )

  // --- Property 3 Tests ---

  it('Property 3: close() after openCreate() restores formData to defaults and sets visible=false', async () => {
    await fc.assert(
      fc.asyncProperty(formDataArb, async (defaults) => {
        const modal = useFormModal({
          defaultFormData: defaults,
        })

        modal.openCreate()

        // Verify openCreate set visible=true and mode='create'
        expect(modal.visible.value).toBe(true)
        expect(modal.mode.value).toBe('create')

        modal.close()

        // After close: formData should equal defaults, visible should be false
        expect(modal.visible.value).toBe(false)
        expect(modal.formData.value).toEqual(defaults)
      }),
      { numRuns: 100 },
    )
  })

  it('Property 3: close() after openEdit(record) restores formData to defaults and sets visible=false', async () => {
    await fc.assert(
      fc.asyncProperty(formDataArb, recordDataArb, async (defaults, record) => {
        const modal = useFormModal({
          defaultFormData: defaults,
        })

        modal.openEdit(record)

        // Verify openEdit set visible=true and mode='edit'
        expect(modal.visible.value).toBe(true)
        expect(modal.mode.value).toBe('edit')
        // formData should be merged: defaults + record
        expect(modal.formData.value).toEqual({ ...defaults, ...record })

        modal.close()

        // After close: formData should equal defaults, visible should be false
        expect(modal.visible.value).toBe(false)
        expect(modal.formData.value).toEqual(defaults)
      }),
      { numRuns: 100 },
    )
  })

  it('Property 3: close() is idempotent — multiple close() calls maintain defaults', async () => {
    await fc.assert(
      fc.asyncProperty(formDataArb, openActionArb, async (defaults, action) => {
        const modal = useFormModal({
          defaultFormData: defaults,
        })

        // Open
        if (action.type === 'create') {
          modal.openCreate()
        } else {
          modal.openEdit(action.record)
        }

        // Close twice
        modal.close()
        modal.close()

        expect(modal.visible.value).toBe(false)
        expect(modal.formData.value).toEqual(defaults)
      }),
      { numRuns: 100 },
    )
  })

  // --- Property 4 Tests ---

  it('Property 4: submit() calls createFn with formData when mode is create', async () => {
    await fc.assert(
      fc.asyncProperty(formDataArb, async (defaults) => {
        const createFn = vi.fn().mockResolvedValue(undefined)
        const updateFn = vi.fn().mockResolvedValue(undefined)

        const modal = useFormModal({
          defaultFormData: defaults,
          createFn,
          updateFn,
        })

        modal.openCreate()

        // Capture formData before submit (submit will close and reset)
        const dataBeforeSubmit = { ...modal.formData.value }

        await modal.submit()

        // createFn should be called exactly once with the form data
        expect(createFn).toHaveBeenCalledTimes(1)
        expect(createFn).toHaveBeenCalledWith(dataBeforeSubmit)
        // updateFn should NOT be called
        expect(updateFn).not.toHaveBeenCalled()
      }),
      { numRuns: 100 },
    )
  })

  it('Property 4: submit() calls updateFn with formData when mode is edit', async () => {
    await fc.assert(
      fc.asyncProperty(formDataArb, recordDataArb, async (defaults, record) => {
        const createFn = vi.fn().mockResolvedValue(undefined)
        const updateFn = vi.fn().mockResolvedValue(undefined)

        const modal = useFormModal({
          defaultFormData: defaults,
          createFn,
          updateFn,
        })

        modal.openEdit(record)

        // Capture formData before submit
        const dataBeforeSubmit = { ...modal.formData.value }

        await modal.submit()

        // updateFn should be called exactly once with the merged form data
        expect(updateFn).toHaveBeenCalledTimes(1)
        expect(updateFn).toHaveBeenCalledWith(dataBeforeSubmit)
        // createFn should NOT be called
        expect(createFn).not.toHaveBeenCalled()
      }),
      { numRuns: 100 },
    )
  })

  it('Property 4: submit() keeps modal open on error', async () => {
    await fc.assert(
      fc.asyncProperty(formDataArb, openActionArb, async (defaults, action) => {
        const error = new Error('submit failed')
        const createFn = vi.fn().mockRejectedValue(error)
        const updateFn = vi.fn().mockRejectedValue(error)
        const onError = vi.fn()

        const modal = useFormModal({
          defaultFormData: defaults,
          createFn,
          updateFn,
          onError,
        })

        // Open in the appropriate mode
        if (action.type === 'create') {
          modal.openCreate()
        } else {
          modal.openEdit(action.record)
        }

        const dataBeforeSubmit = { ...modal.formData.value }

        await modal.submit()

        // Modal should remain open on error (Req 2.6)
        expect(modal.visible.value).toBe(true)
        // formData should be unchanged
        expect(modal.formData.value).toEqual(dataBeforeSubmit)
        // submitting should be false after completion
        expect(modal.submitting.value).toBe(false)
        // onError should have been called
        expect(onError).toHaveBeenCalledTimes(1)
      }),
      { numRuns: 100 },
    )
  })
})

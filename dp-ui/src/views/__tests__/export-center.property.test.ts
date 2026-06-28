/**
 * 导出中心属性测试
 * Feature: page-audit-optimization
 * Property 26: 导出任务添加到导出中心
 *
 * **Validates: Requirements 22.3**
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as fc from 'fast-check'

// ============================================================================
// Mock setup — mock the API and naive-ui before importing useExport
// ============================================================================

const mockCreateExportTask = vi.fn()
vi.mock('@/api/exportTask', () => ({
  createExportTask: (...args: unknown[]) => mockCreateExportTask(...args)
}))

const mockMessageSuccess = vi.fn()
const mockMessageError = vi.fn()
vi.mock('naive-ui', () => ({
  useMessage: () => ({
    success: mockMessageSuccess,
    error: mockMessageError,
    info: vi.fn(),
    warning: vi.fn()
  })
}))

vi.mock('@/utils/export', () => ({
  exportToExcel: vi.fn()
}))

vi.mock('@/utils/logger', () => ({
  logger: { error: vi.fn(), warn: vi.fn(), info: vi.fn() }
}))

import { useExport, type ExportCenterParams } from '@/composables/useExport'

// ============================================================================
// Arbitraries (Test Data Generators)
// ============================================================================

/** Generate a valid task name */
const taskNameArb = fc
  .stringMatching(/^[a-zA-Z\u4e00-\u9fa5][a-zA-Z0-9\u4e00-\u9fa5_\- ]{0,29}$/)
  .filter((s) => s.length >= 1)

/** Generate a valid task type */
const taskTypeArb = fc.constantFrom(
  'report', 'user', 'datasource', 'chart', 'pipeline',
  'ticket', 'log', 'config', 'alert', 'data-transfer'
)

/** Generate valid ExportCenterParams */
const exportCenterParamsArb: fc.Arbitrary<ExportCenterParams> = fc
  .record({
    taskName: taskNameArb,
    taskType: fc.option(taskTypeArb, { nil: undefined }),
    refId: fc.option(fc.nat({ max: 999999 }), { nil: undefined }),
    refCode: fc.option(
      fc.stringMatching(/^[a-z][a-z0-9_]{1,15}$/),
      { nil: undefined }
    ),
    filters: fc.option(
      fc.constant('{"status":"active"}'),
      { nil: undefined }
    ),
    params: fc.option(
      fc.constant('{"format":"xlsx"}'),
      { nil: undefined }
    )
  })
  .map((r) => {
    const p: ExportCenterParams = { taskName: r.taskName }
    if (r.taskType !== undefined) p.taskType = r.taskType
    if (r.refId !== undefined) p.refId = r.refId
    if (r.refCode !== undefined) p.refCode = r.refCode
    if (r.filters !== undefined) p.filters = r.filters
    if (r.params !== undefined) p.params = r.params
    return p
  })

// ============================================================================
// Property 26: 导出任务添加到导出中心
// ============================================================================

describe('Property 26: 导出任务添加到导出中心', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  /**
   * For any valid ExportCenterParams, calling addToExportCenter should
   * invoke createExportTask API exactly once with the correct parameters.
   *
   * **Validates: Requirements 22.3**
   */
  it('addToExportCenter calls createExportTask API with correct params', async () => {
    await fc.assert(
      fc.asyncProperty(exportCenterParamsArb, async (params) => {
        vi.clearAllMocks()
        mockCreateExportTask.mockResolvedValue({ code: 200, data: { id: 1 } })

        const { addToExportCenter } = useExport()
        await addToExportCenter(params)

        expect(mockCreateExportTask).toHaveBeenCalledTimes(1)
        const callArgs = mockCreateExportTask.mock.calls[0]![0]
        expect(callArgs.taskName).toBe(params.taskName)
        if (params.taskType !== undefined) {
          expect(callArgs.taskType).toBe(params.taskType)
        }
        if (params.refId !== undefined) {
          expect(callArgs.refId).toBe(params.refId)
        }
        if (params.refCode !== undefined) {
          expect(callArgs.refCode).toBe(params.refCode)
        }
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * For any valid ExportCenterParams, when createExportTask returns
   * success (code 200), addToExportCenter should return true.
   *
   * **Validates: Requirements 22.3**
   */
  it('addToExportCenter returns true on successful task creation', async () => {
    await fc.assert(
      fc.asyncProperty(exportCenterParamsArb, async (params) => {
        vi.clearAllMocks()
        mockCreateExportTask.mockResolvedValue({ code: 200, data: { id: 1 } })

        const { addToExportCenter } = useExport()
        const result = await addToExportCenter(params)

        expect(result).toBe(true)
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * For any valid ExportCenterParams, when createExportTask returns
   * a non-200 code, addToExportCenter should return false.
   *
   * **Validates: Requirements 22.3**
   */
  it('addToExportCenter returns false on API failure', async () => {
    await fc.assert(
      fc.asyncProperty(exportCenterParamsArb, async (params) => {
        vi.clearAllMocks()
        mockCreateExportTask.mockResolvedValue({ code: 500, msg: '服务器错误' })

        const { addToExportCenter } = useExport()
        const result = await addToExportCenter(params)

        expect(result).toBe(false)
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * For any valid ExportCenterParams, when createExportTask throws,
   * addToExportCenter should return false and not throw.
   *
   * **Validates: Requirements 22.3**
   */
  it('addToExportCenter returns false on exception without throwing', async () => {
    await fc.assert(
      fc.asyncProperty(exportCenterParamsArb, async (params) => {
        vi.clearAllMocks()
        mockCreateExportTask.mockRejectedValue(new Error('Network error'))

        const { addToExportCenter } = useExport()
        const result = await addToExportCenter(params)

        expect(result).toBe(false)
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * For any valid ExportCenterParams, successful task creation should
   * trigger a success message notification.
   *
   * **Validates: Requirements 22.3**
   */
  it('shows success message after task is created', async () => {
    await fc.assert(
      fc.asyncProperty(exportCenterParamsArb, async (params) => {
        vi.clearAllMocks()
        mockCreateExportTask.mockResolvedValue({ code: 200, data: { id: 1 } })

        const { addToExportCenter } = useExport()
        await addToExportCenter(params)

        expect(mockMessageSuccess).toHaveBeenCalledTimes(1)
        return true
      }),
      { numRuns: 100 }
    )
  })
})

import { describe, it, expect, vi, beforeEach } from 'vitest'

/**
 * Unit tests for useCrudPage composable enhancements
 *
 * Validates: Requirements 18.1, 18.2, 18.3, 18.4, 18.5
 */

// Mock logger
vi.mock('@/utils/logger', () => ({
  logger: {
    error: vi.fn(),
    warn: vi.fn(),
    info: vi.fn(),
    debug: vi.fn(),
  },
}))

// Mock message/dialog — factory must not reference top-level variables
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
import { message, dialog } from '@/utils/message'

function createDefaultOptions(overrides: Record<string, any> = {}) {
  return {
    listApi: vi.fn().mockResolvedValue({ data: { list: [], total: 0 } }),
    createApi: vi.fn().mockResolvedValue(undefined),
    updateApi: vi.fn().mockResolvedValue(undefined),
    deleteApi: vi.fn().mockResolvedValue(undefined),
    defaultFormData: () => ({ name: '', value: '' } as Record<string, unknown>),
    ...overrides,
  }
}

/** Helper to get the last dialog.warning call options */
function getLastDialogOptions(): any {
  const calls = vi.mocked(dialog.warning).mock.calls
  return calls[calls.length - 1]?.[0]
}

describe('useCrudPage — Unit Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // --- Req 18.2: 表单提交成功后显示成功消息并自动关闭弹窗 ---

  describe('Req 18.2: Form submit success shows message and closes modal', () => {
    it('should show success message after create submit', async () => {
      const crud = useCrudPage(createDefaultOptions())

      crud.modal.openCreate()
      expect(crud.modal.visible.value).toBe(true)

      await crud.modal.submit()

      expect(message.success).toHaveBeenCalledWith('创建成功')
      expect(crud.modal.visible.value).toBe(false)
    })

    it('should show success message after edit submit', async () => {
      const crud = useCrudPage(createDefaultOptions())

      crud.modal.openEdit({ id: 1, name: 'test' } as Record<string, unknown>)
      expect(crud.modal.visible.value).toBe(true)

      await crud.modal.submit()

      expect(message.success).toHaveBeenCalledWith('更新成功')
      expect(crud.modal.visible.value).toBe(false)
    })

    it('should use custom success messages', async () => {
      const crud = useCrudPage(createDefaultOptions({
        createSuccessMsg: '添加完成',
        updateSuccessMsg: '修改完成',
      }))

      crud.modal.openCreate()
      await crud.modal.submit()
      expect(message.success).toHaveBeenCalledWith('添加完成')
    })
  })

  // --- Req 18.3: 表单提交失败时显示具体错误信息 ---

  describe('Req 18.3: Form submit failure shows error message', () => {
    it('should show error message when create fails', async () => {
      const crud = useCrudPage(createDefaultOptions({
        createApi: vi.fn().mockRejectedValue(new Error('名称已存在')),
      }))

      crud.modal.openCreate()
      await crud.modal.submit()

      expect(message.error).toHaveBeenCalledWith('名称已存在')
      // Modal should stay open on failure
      expect(crud.modal.visible.value).toBe(true)
    })

    it('should show error message when update fails', async () => {
      const crud = useCrudPage(createDefaultOptions({
        updateApi: vi.fn().mockRejectedValue(new Error('更新权限不足')),
      }))

      crud.modal.openEdit({ id: 1 } as Record<string, unknown>)
      await crud.modal.submit()

      expect(message.error).toHaveBeenCalledWith('更新权限不足')
      expect(crud.modal.visible.value).toBe(true)
    })

    it('should extract error message from API response', async () => {
      const apiError = new Error('Request failed') as any
      apiError.response = { data: { msg: '数据源连接失败' } }

      const crud = useCrudPage(createDefaultOptions({
        createApi: vi.fn().mockRejectedValue(apiError),
      }))

      crud.modal.openCreate()
      await crud.modal.submit()

      expect(message.error).toHaveBeenCalledWith('数据源连接失败')
    })

    it('should show fallback error message for non-Error rejections', async () => {
      const crud = useCrudPage(createDefaultOptions({
        createApi: vi.fn().mockRejectedValue('unknown error'),
      }))

      crud.modal.openCreate()
      await crud.modal.submit()

      expect(message.error).toHaveBeenCalledWith('unknown error')
    })
  })

  // --- Req 18.4: 删除操作弹出确认对话框 ---

  describe('Req 18.4: Delete shows confirmation dialog', () => {
    it('should show confirmation dialog on delete', async () => {
      const deleteApi = vi.fn().mockResolvedValue(undefined)
      const crud = useCrudPage(createDefaultOptions({ deleteApi }))

      const deletePromise = crud.handleDelete(1)

      expect(dialog.warning).toHaveBeenCalledTimes(1)
      const opts = getLastDialogOptions()
      expect(opts.title).toBe('确认删除')
      expect(opts.positiveText).toBe('确认删除')
      expect(opts.negativeText).toBe('取消')

      // Simulate user confirming
      await opts.onPositiveClick()
      await deletePromise

      expect(deleteApi).toHaveBeenCalledWith(1)
      expect(message.success).toHaveBeenCalledWith('删除成功')
    })

    it('should show error message when delete fails', async () => {
      const crud = useCrudPage(createDefaultOptions({
        deleteApi: vi.fn().mockRejectedValue(new Error('删除失败：存在关联数据')),
      }))

      const deletePromise = crud.handleDelete(1)
      const opts = getLastDialogOptions()
      await opts.onPositiveClick()
      await deletePromise

      expect(message.error).toHaveBeenCalledWith('删除失败：存在关联数据')
    })
  })

  // --- Req 18.5: 批量操作弹出确认对话框 ---

  describe('Req 18.5: Batch operations show confirmation dialog', () => {
    it('should show warning when no items selected for batch delete', async () => {
      const crud = useCrudPage(createDefaultOptions())

      await crud.handleBatchDelete()

      expect(message.warning).toHaveBeenCalledWith('请先选择要删除的记录')
      expect(dialog.warning).not.toHaveBeenCalled()
    })

    it('should show confirmation dialog for batch delete with explicit ids', async () => {
      const deleteApi = vi.fn().mockResolvedValue(undefined)
      const crud = useCrudPage(createDefaultOptions({ deleteApi }))

      const batchPromise = crud.handleBatchDelete([1, 2, 3])

      expect(dialog.warning).toHaveBeenCalledTimes(1)
      const opts = getLastDialogOptions()
      expect(opts.title).toBe('确认批量删除')
      expect(opts.content).toContain('3')

      await opts.onPositiveClick()
      await batchPromise

      expect(deleteApi).toHaveBeenCalledTimes(3)
      expect(message.success).toHaveBeenCalledWith('批量删除成功')
    })

    it('should use batchDeleteApi when provided', async () => {
      const batchDeleteApi = vi.fn().mockResolvedValue(undefined)
      const deleteApi = vi.fn().mockResolvedValue(undefined)
      const crud = useCrudPage(createDefaultOptions({ deleteApi, batchDeleteApi }))

      const batchPromise = crud.handleBatchDelete([1, 2])

      const opts = getLastDialogOptions()
      await opts.onPositiveClick()
      await batchPromise

      expect(batchDeleteApi).toHaveBeenCalledWith([1, 2])
      expect(deleteApi).not.toHaveBeenCalled()
    })

    it('should show confirmation dialog for generic batch action', async () => {
      const actionFn = vi.fn().mockResolvedValue(undefined)
      const crud = useCrudPage(createDefaultOptions())

      const batchPromise = crud.handleBatchAction({
        title: '确认批量启用',
        content: '确定要启用选中的 5 条记录吗？',
        action: actionFn,
        successMsg: '批量启用成功',
      })

      const opts = getLastDialogOptions()
      expect(opts.title).toBe('确认批量启用')
      expect(opts.content).toBe('确定要启用选中的 5 条记录吗？')

      await opts.onPositiveClick()
      await batchPromise

      expect(actionFn).toHaveBeenCalledTimes(1)
      expect(message.success).toHaveBeenCalledWith('批量启用成功')
    })

    it('should show error message when batch action fails', async () => {
      const crud = useCrudPage(createDefaultOptions())

      const batchPromise = crud.handleBatchAction({
        title: '确认操作',
        content: '确定吗？',
        action: vi.fn().mockRejectedValue(new Error('批量操作部分失败')),
        successMsg: '操作成功',
      })

      const opts = getLastDialogOptions()
      await opts.onPositiveClick()
      await batchPromise

      expect(message.error).toHaveBeenCalledWith('批量操作部分失败')
      expect(message.success).not.toHaveBeenCalled()
    })
  })

  // --- Req 18.1: 数据列表查询、分页、排序 ---

  describe('Req 18.1: Data list query, pagination, sorting', () => {
    it('should expose pagination and sorting handlers', () => {
      const crud = useCrudPage(createDefaultOptions())

      expect(crud.handlePageChange).toBeDefined()
      expect(crud.handlePageSizeChange).toBeDefined()
      expect(crud.handleSorterChange).toBeDefined()
      expect(crud.pagination).toBeDefined()
      expect(crud.data).toBeDefined()
      expect(crud.total).toBeDefined()
      expect(crud.loading).toBeDefined()
    })

    it('should expose filter apply handler', () => {
      const crud = useCrudPage(createDefaultOptions())
      expect(crud.handleFilterApply).toBeDefined()
    })

    it('should expose batch delete and batch action handlers', () => {
      const crud = useCrudPage(createDefaultOptions())
      expect(crud.handleBatchDelete).toBeDefined()
      expect(crud.handleBatchAction).toBeDefined()
    })
  })
})

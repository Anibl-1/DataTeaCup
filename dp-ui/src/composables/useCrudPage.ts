/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * CRUD 页面组合式函数
 * 内部组合 useDataTable 和 useFormModal，提供统一的 CRUD 操作接口
 *
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.6, 18.1, 18.2, 18.3, 18.4, 18.5
 */
import { type Ref, type ComputedRef } from 'vue'
import type { PaginationProps } from 'naive-ui'
import type { FilterCondition } from '@/types/api'
import { useDataTable, type DataTableQueryParams } from './useDataTable'
import { useFormModal } from './useFormModal'
import { message } from '@/utils/message'
import { dialog } from '@/utils/message'
import { logger } from '@/utils/logger'

/** useCrudPage 配置选项 */
export interface CrudPageOptions<
  TRow extends Record<string, unknown>,
  TForm extends Record<string, unknown>,
  TQuery extends Record<string, unknown> = Record<string, unknown>
> {
  /** 列表查询 API */
  listApi: (params: DataTableQueryParams & TQuery) => Promise<any>
  /** 创建 API */
  createApi: (data: TForm) => Promise<any>
  /** 更新 API */
  updateApi: (data: TForm) => Promise<any>
  /** 删除 API */
  deleteApi: (id: number | string) => Promise<any>
  /** 批量删除 API（可选） */
  batchDeleteApi?: (ids: (number | string)[]) => Promise<any>
  /** 默认表单数据工厂 */
  defaultFormData: () => TForm
  /** 行唯一键 */
  rowKey?: keyof TRow | ((row: TRow) => string | number)
  /** 默认分页大小 */
  defaultPageSize?: number
  /** 默认筛选条件 */
  defaultFilters?: TQuery
  /** 创建成功提示 */
  createSuccessMsg?: string
  /** 更新成功提示 */
  updateSuccessMsg?: string
  /** 删除成功提示 */
  deleteSuccessMsg?: string
  /** 批量删除成功提示 */
  batchDeleteSuccessMsg?: string
  /** 数据转换函数 */
  transform?: (list: TRow[]) => TRow[]
  /** 自定义删除处理（覆盖默认 deleteApi 调用） */
  onDelete?: (id: number | string) => Promise<void>
  /** 自定义提交处理（覆盖默认 createApi/updateApi 调用） */
  onSubmit?: (mode: 'create' | 'edit', data: TForm) => Promise<void>
  /** 提交成功回调 */
  onSuccess?: (mode: 'create' | 'edit') => void
  /** 提交失败回调 */
  onError?: (error: Error) => void
}

/** useCrudPage 返回值 */
export interface CrudPageReturn<
  TRow extends Record<string, unknown>,
  TForm extends Record<string, unknown>
> {
  // 来自 useDataTable
  data: Ref<TRow[]>
  loading: Ref<boolean>
  total: Ref<number>
  pagination: ComputedRef<PaginationProps>
  searchParams: Ref<Record<string, unknown>>
  checkedRowKeys: Ref<(string | number)[]>
  checkedRows: ComputedRef<TRow[]>

  // 来自 useFormModal
  modal: {
    visible: Ref<boolean>
    mode: Ref<'create' | 'edit'>
    formData: Ref<TForm>
    submitting: Ref<boolean>
    openCreate: () => void
    openEdit: (row: TRow) => void
    close: () => void
    submit: () => Promise<void>
  }

  // 操作方法
  load: () => Promise<void>
  refresh: () => Promise<void>
  reset: () => Promise<void>
  /** 删除单条记录（含确认对话框）Req 18.4 */
  handleDelete: (id: number | string) => Promise<void>
  /** 批量删除（含确认对话框）Req 18.5 */
  handleBatchDelete: (ids?: (number | string)[]) => Promise<void>
  /** 批量操作（含确认对话框）Req 18.5 */
  handleBatchAction: (options: BatchActionOptions) => Promise<void>
  handlePageChange: (page: number) => void
  handlePageSizeChange: (pageSize: number) => void
  handleSorterChange: (sorter: any) => void
  handleFilterApply: (filters: FilterCondition[]) => void
}

/** 批量操作配置 */
export interface BatchActionOptions {
  /** 操作标题（用于确认对话框） */
  title: string
  /** 操作描述（用于确认对话框） */
  content: string
  /** 执行函数 */
  action: () => Promise<void>
  /** 成功提示 */
  successMsg?: string
  /** 确认按钮文字 */
  positiveText?: string
}

/**
 * 从错误对象中提取用户可读的错误消息
 * Req 18.3: 提交失败时显示具体错误信息
 */
function extractErrorMessage(err: unknown): string {
  if (err instanceof Error) {
    // 尝试从 API 响应中提取 msg 字段
    const anyErr = err as any
    if (anyErr.response?.data?.msg) return anyErr.response.data.msg
    if (anyErr.response?.data?.message) return anyErr.response.data.message
    return err.message || '操作失败'
  }
  if (typeof err === 'string') return err
  return '操作失败'
}

/**
 * CRUD 页面组合式函数
 *
 * 内部组合 useDataTable 和 useFormModal，对外提供统一的 CRUD 操作接口。
 * 页面组件只需传入配置即可获得完整功能。
 *
 * 功能特性：
 * - 数据列表查询、分页、排序 (Req 18.1)
 * - 表单弹窗提交成功后显示成功消息并自动关闭 (Req 18.2)
 * - 表单提交失败时显示具体错误信息 (Req 18.3)
 * - 删除操作弹出确认对话框 (Req 18.4)
 * - 批量操作弹出确认对话框 (Req 18.5)
 *
 * 支持自定义扩展点：
 * - onDelete: 覆盖默认删除逻辑
 * - onSubmit: 覆盖默认提交逻辑
 * - onSuccess / onError: 提交成功/失败回调
 */
export function useCrudPage<
  TRow extends Record<string, unknown>,
  TForm extends Record<string, unknown>,
  TQuery extends Record<string, unknown> = Record<string, unknown>
>(
  options: CrudPageOptions<TRow, TForm, TQuery>
): CrudPageReturn<TRow, TForm> {
  const {
    listApi,
    createApi,
    updateApi,
    deleteApi,
    batchDeleteApi,
    defaultFormData,
    rowKey = 'id' as keyof TRow,
    defaultPageSize = 10,
    defaultFilters,
    createSuccessMsg = '创建成功',
    updateSuccessMsg = '更新成功',
    deleteSuccessMsg = '删除成功',
    batchDeleteSuccessMsg = '批量删除成功',
    transform,
    onDelete,
    onSubmit,
    onSuccess,
    onError
  } = options

  // --- 组合 useDataTable (Req 18.1: 数据列表查询、分页、排序) ---
  const tableOptions: Parameters<typeof useDataTable<TRow>>[0] = {
    apiFn: (params) => listApi(params as DataTableQueryParams & TQuery),
    defaultPageSize,
    immediate: true,
    rowKey,
  }
  if (transform !== undefined) tableOptions.transform = transform
  if (defaultFilters !== undefined) tableOptions.defaultFilters = defaultFilters as Record<string, unknown>
  const table = useDataTable<TRow>(tableOptions)

  // --- 组合 useFormModal (Req 18.2, 18.3) ---
  const formModal = useFormModal<TForm>({
    defaultFormData,
    createFn: onSubmit
      ? (data) => onSubmit('create', data)
      : async (data) => {
          await createApi(data)
          // Req 18.2: 提交成功后显示成功消息
          message.success(createSuccessMsg)
        },
    updateFn: onSubmit
      ? (data) => onSubmit('edit', data)
      : async (data) => {
          await updateApi(data)
          // Req 18.2: 提交成功后显示成功消息
          message.success(updateSuccessMsg)
        },
    onSuccess: (mode) => {
      // Req 18.2: 成功后刷新列表（弹窗由 useFormModal 自动关闭）
      table.load()
      onSuccess?.(mode)
    },
    onError: (error) => {
      // Req 18.3: 提交失败时显示具体错误信息
      const errorMsg = extractErrorMessage(error)
      message.error(errorMsg)
      onError?.(error)
    }
  })

  // --- handleDelete（含确认对话框）Req 18.4 ---
  const handleDelete = async (id: number | string): Promise<void> => {
    if (onDelete) {
      await onDelete(id)
      return
    }

    return new Promise<void>((resolve) => {
      dialog.warning({
        title: '确认删除',
        content: '确定要删除吗？此操作不可恢复。',
        positiveText: '确认删除',
        negativeText: '取消',
        onPositiveClick: async () => {
          try {
            await deleteApi(id)
            message.success(deleteSuccessMsg)
            await table.load()
          } catch (err) {
            // Req 18.3: 删除失败时显示具体错误信息
            const errorMsg = extractErrorMessage(err)
            logger.error('useCrudPage 删除失败', err)
            message.error(errorMsg)
          } finally {
            resolve()
          }
        },
        onNegativeClick: () => {
          resolve()
        }
      })
    })
  }

  // --- handleBatchDelete（含确认对话框）Req 18.5 ---
  const handleBatchDelete = async (ids?: (number | string)[]): Promise<void> => {
    const targetIds = ids ?? table.checkedRowKeys.value
    if (targetIds.length === 0) {
      message.warning('请先选择要删除的记录')
      return
    }

    return new Promise<void>((resolve) => {
      dialog.warning({
        title: '确认批量删除',
        content: `确定要删除选中的 ${targetIds.length} 条记录吗？此操作不可恢复。`,
        positiveText: '确认删除',
        negativeText: '取消',
        onPositiveClick: async () => {
          try {
            if (batchDeleteApi) {
              await batchDeleteApi(targetIds)
            } else {
              // 逐条删除
              await Promise.all(targetIds.map((id) => deleteApi(id)))
            }
            message.success(batchDeleteSuccessMsg)
            table.checkedRowKeys.value = []
            await table.load()
          } catch (err) {
            const errorMsg = extractErrorMessage(err)
            logger.error('useCrudPage 批量删除失败', err)
            message.error(errorMsg)
          } finally {
            resolve()
          }
        },
        onNegativeClick: () => {
          resolve()
        }
      })
    })
  }

  // --- handleBatchAction（通用批量操作，含确认对话框）Req 18.5 ---
  const handleBatchAction = async (actionOptions: BatchActionOptions): Promise<void> => {
    return new Promise<void>((resolve) => {
      dialog.warning({
        title: actionOptions.title,
        content: actionOptions.content,
        positiveText: actionOptions.positiveText ?? '确认',
        negativeText: '取消',
        onPositiveClick: async () => {
          try {
            await actionOptions.action()
            if (actionOptions.successMsg) {
              message.success(actionOptions.successMsg)
            }
            await table.load()
          } catch (err) {
            const errorMsg = extractErrorMessage(err)
            logger.error('useCrudPage 批量操作失败', err)
            message.error(errorMsg)
          } finally {
            resolve()
          }
        },
        onNegativeClick: () => {
          resolve()
        }
      })
    })
  }

  // --- handleFilterApply ---
  const handleFilterApply = (filters: FilterCondition[]): void => {
    table.searchParams.value = {
      ...table.searchParams.value,
      filters
    }
    table.load()
  }

  return {
    // useDataTable 返回值
    data: table.data,
    loading: table.loading,
    total: table.total,
    pagination: table.pagination,
    searchParams: table.searchParams,
    checkedRowKeys: table.checkedRowKeys,
    checkedRows: table.checkedRows,

    // useFormModal 返回值（包装为 modal 对象）
    modal: {
      visible: formModal.visible,
      mode: formModal.mode,
      formData: formModal.formData,
      submitting: formModal.submitting,
      openCreate: formModal.openCreate,
      openEdit: (row: TRow) => formModal.openEdit(row as unknown as Partial<TForm>),
      close: formModal.close,
      submit: formModal.submit
    },

    // 操作方法
    load: table.load,
    refresh: table.refresh,
    reset: table.reset,
    handleDelete,
    handleBatchDelete,
    handleBatchAction,
    handlePageChange: table.handlePageChange,
    handlePageSizeChange: table.handlePageSizeChange,
    handleSorterChange: table.handleSorterChange,
    handleFilterApply
  }
}

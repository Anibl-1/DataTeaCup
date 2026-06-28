/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 增强版数据表格组合式函数
 * 基于 useTable 增强，集成 API 响应自动解析、自定义搜索参数等功能
 *
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6
 */
import { ref, computed, onMounted, type Ref, type ComputedRef } from 'vue'
import type { PaginationProps } from 'naive-ui'
import { parseListResponse } from '@/utils/apiResponseParser'
import { logger } from '@/utils/logger'

/** 数据表格查询参数 */
export interface DataTableQueryParams {
  page: number
  pageSize: number
  sortField?: string
  sortOrder?: 'asc' | 'desc'
  [key: string]: unknown
}

/** useDataTable 配置选项 */
export interface UseDataTableOptions<T> {
  /** API 请求函数，返回原始 API 响应 */
  apiFn: (params: DataTableQueryParams) => Promise<any>
  /** 默认每页条数 */
  defaultPageSize?: number
  /** 是否立即加载 */
  immediate?: boolean
  /** 行唯一键 */
  rowKey?: keyof T | ((row: T) => string | number)
  /** 数据转换函数（可选） */
  transform?: (list: T[]) => T[]
  /** 默认筛选条件 */
  defaultFilters?: Record<string, unknown>
}

/** useDataTable 返回值 */
export interface UseDataTableReturn<T> {
  data: Ref<T[]>
  loading: Ref<boolean>
  total: Ref<number>
  pagination: ComputedRef<PaginationProps>
  checkedRowKeys: Ref<(string | number)[]>
  checkedRows: ComputedRef<T[]>
  searchParams: Ref<Record<string, unknown>>
  load: () => Promise<void>
  refresh: () => Promise<void>
  reset: () => Promise<void>
  handlePageChange: (page: number) => void
  handlePageSizeChange: (pageSize: number) => void
  handleSorterChange: (sorter: any) => void
  handleCheck: (keys: (string | number)[]) => void
  clearChecked: () => void
}

/**
 * 增强版数据表格组合式函数
 *
 * 相比 useTable 的增强点：
 * - 集成 parseListResponse 自动解析各种 API 响应格式
 * - 支持 searchParams 自定义搜索参数
 * - 支持 transform 数据转换
 * - 支持 defaultFilters 默认筛选条件
 */
export function useDataTable<T extends object>(
  options: UseDataTableOptions<T>
): UseDataTableReturn<T> {
  const {
    apiFn,
    defaultPageSize = 10,
    immediate = true,
    rowKey = 'id' as keyof T,
    transform,
    defaultFilters = {}
  } = options

  // --- 状态 ---
  const data: Ref<T[]> = ref([])
  const loading = ref(false)
  const total = ref(0)

  // 分页状态
  const currentPage = ref(1)
  const pageSize = ref(defaultPageSize)

  // 排序状态 (Req 1.2)
  const sortField = ref<string>('')
  const sortOrder = ref<'asc' | 'desc' | ''>('')

  // 搜索参数 (Req 1.3)
  const searchParams = ref<Record<string, unknown>>({ ...defaultFilters })

  // 选中状态 (Req 1.4)
  const checkedRowKeys = ref<(string | number)[]>([])

  // --- 计算属性 ---

  // Naive UI 分页配置 (Req 1.1)
  const pagination = computed<PaginationProps>(() => ({
    page: currentPage.value,
    pageSize: pageSize.value,
    itemCount: total.value,
    showSizePicker: true,
    pageSizes: [10, 20, 50, 100],
    prefix: ({ itemCount }: { itemCount: number | undefined }) => `共 ${itemCount ?? 0} 条`,
    onUpdatePage: handlePageChange,
    onUpdatePageSize: handlePageSizeChange
  }))

  // 选中行数据
  const checkedRows = computed<T[]>(() => {
    return data.value.filter((row: T) => {
      const key = typeof rowKey === 'function' ? rowKey(row) : row[rowKey]
      return checkedRowKeys.value.includes(key as string | number)
    })
  })

  // --- 方法 ---

  /** 加载数据 (Req 1.5, 1.6) */
  const load = async (): Promise<void> => {
    loading.value = true
    try {
      const params: DataTableQueryParams = {
        page: currentPage.value,
        pageSize: pageSize.value,
        ...searchParams.value
      }
      if (sortField.value) {
        params.sortField = sortField.value
      }
      if (sortOrder.value) {
        params.sortOrder = sortOrder.value as 'asc' | 'desc'
      }

      const response = await apiFn(params)
      const parsed = parseListResponse<T>(response)

      data.value = transform ? transform(parsed.list) : parsed.list
      total.value = parsed.total
    } catch (error) {
      // Req 1.6: 加载失败时 data=[], total=0
      logger.error('useDataTable 数据加载失败', error)
      data.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  /** 刷新（保持当前页） */
  const refresh = async (): Promise<void> => {
    await load()
  }

  /** 重置（回到第一页，清空排序、筛选、选中） (Req 1.1, 1.2, 1.3, 1.4) */
  const reset = async (): Promise<void> => {
    currentPage.value = 1
    sortField.value = ''
    sortOrder.value = ''
    searchParams.value = { ...defaultFilters }
    checkedRowKeys.value = []
    await load()
  }

  /** 分页变化 (Req 1.1) */
  const handlePageChange = (page: number): void => {
    currentPage.value = page
    load()
  }

  /** 每页条数变化 (Req 1.1) */
  const handlePageSizeChange = (size: number): void => {
    pageSize.value = size
    currentPage.value = 1
    load()
  }

  /** 排序变化 (Req 1.2) */
  const handleSorterChange = (sorter: any): void => {
    if (sorter && sorter.order) {
      sortField.value = sorter.columnKey || sorter.field || ''
      sortOrder.value = sorter.order === 'ascend' ? 'asc'
        : sorter.order === 'descend' ? 'desc'
        : sorter.order
    } else {
      sortField.value = ''
      sortOrder.value = ''
    }
    currentPage.value = 1
    load()
  }

  /** 选中变化 (Req 1.4) */
  const handleCheck = (keys: (string | number)[]): void => {
    checkedRowKeys.value = keys
  }

  /** 清空选中 (Req 1.4) */
  const clearChecked = (): void => {
    checkedRowKeys.value = []
  }

  // 立即加载
  if (immediate) {
    onMounted(() => {
      load()
    })
  }

  return {
    data,
    loading,
    total,
    pagination,
    checkedRowKeys,
    checkedRows,
    searchParams,
    load,
    refresh,
    reset,
    handlePageChange,
    handlePageSizeChange,
    handleSorterChange,
    handleCheck,
    clearChecked
  }
}

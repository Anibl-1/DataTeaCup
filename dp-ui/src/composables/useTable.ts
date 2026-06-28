/**
 * 表格 Hook
 * 封装表格常用逻辑
 */
import { ref, reactive, computed, type Ref } from 'vue'
import type { DataTableColumns } from 'naive-ui'
import { logger } from '@/utils/logger'

interface UseTableOptions<T> {
  /** 获取数据的函数 */
  fetchData: (params: TableParams) => Promise<{ list: T[]; total: number }>
  /** 默认分页大小 */
  defaultPageSize?: number
  /** 是否立即加载 */
  immediate?: boolean
  /** 行key字段 */
  rowKey?: keyof T | ((row: T) => string | number)
}

interface TableParams {
  page: number
  pageSize: number
  sortField?: string
  sortOrder?: 'asc' | 'desc'
  filters?: Record<string, unknown>
}

/**
 * 表格 Hook
 */
export function useTable<T extends Record<string, unknown>>(options: UseTableOptions<T>) {
  const {
    fetchData,
    defaultPageSize = 10,
    immediate = true,
    rowKey = 'id'
  } = options

  // 数据
  const data: Ref<T[]> = ref([])
  const loading = ref(false)
  const total = ref(0)

  // 分页
  const pagination = reactive({
    page: 1,
    pageSize: defaultPageSize,
    showSizePicker: true,
    pageSizes: [10, 20, 50, 100],
    prefix: ({ itemCount }: { itemCount: number }) => `共 ${itemCount} 条`
  })

  // 排序
  const sortState = reactive({
    field: '' as string,
    order: '' as 'asc' | 'desc' | ''
  })

  // 筛选
  const filters = ref<Record<string, unknown>>({})

  // 选中行
  const checkedRowKeys = ref<(string | number)[]>([])
  const checkedRows = computed(() => {
    return data.value.filter((row: T) => {
      const key = typeof rowKey === 'function' ? rowKey(row) : row[rowKey]
      return checkedRowKeys.value.includes(key as string | number)
    })
  })

  // 加载数据
  const load = async () => {
    loading.value = true
    try {
      logger.time('table-load')
      const params: TableParams = {
        page: pagination.page,
        pageSize: pagination.pageSize,
        filters: filters.value
      }
      if (sortState.field) params.sortField = sortState.field
      if (sortState.order) params.sortOrder = sortState.order
      const result = await fetchData(params)
      data.value = result.list
      total.value = result.total
      logger.timeEnd('table-load')
    } catch (error) {
      logger.error('表格数据加载失败', error)
      data.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  // 刷新（保持当前页）
  const refresh = () => load()

  // 重置（回到第一页）
  const reset = () => {
    pagination.page = 1
    sortState.field = ''
    sortState.order = ''
    filters.value = {}
    checkedRowKeys.value = []
    return load()
  }

  // 分页变化
  const handlePageChange = (page: number) => {
    pagination.page = page
    load()
  }

  // 分页大小变化
  const handlePageSizeChange = (pageSize: number) => {
    pagination.pageSize = pageSize
    pagination.page = 1
    load()
  }

  // 排序变化
  const handleSorterChange = (sorter: { columnKey: string; order: 'ascend' | 'descend' | false }) => {
    if (sorter.order) {
      sortState.field = sorter.columnKey
      sortState.order = sorter.order === 'ascend' ? 'asc' : 'desc'
    } else {
      sortState.field = ''
      sortState.order = ''
    }
    pagination.page = 1
    load()
  }

  // 筛选变化
  const handleFilterChange = (newFilters: Record<string, unknown>) => {
    filters.value = newFilters
    pagination.page = 1
    load()
  }

  // 选中变化
  const handleCheckedRowKeysChange = (keys: (string | number)[]) => {
    checkedRowKeys.value = keys
  }

  // 清空选中
  const clearChecked = () => {
    checkedRowKeys.value = []
  }

  // 立即加载
  if (immediate) {
    load()
  }

  return {
    // 数据
    data,
    loading,
    total,
    
    // 分页
    pagination: computed(() => ({
      ...pagination,
      itemCount: total.value,
      onUpdatePage: handlePageChange,
      onUpdatePageSize: handlePageSizeChange
    })),
    
    // 排序
    sortState,
    
    // 筛选
    filters,
    
    // 选中
    checkedRowKeys,
    checkedRows,
    
    // 方法
    load,
    refresh,
    reset,
    handlePageChange,
    handlePageSizeChange,
    handleSorterChange,
    handleFilterChange,
    handleCheckedRowKeysChange,
    clearChecked
  }
}

/**
 * 创建表格列配置的辅助函数
 */
export function createColumns<T>(columns: DataTableColumns<T>): DataTableColumns<T> {
  return columns
}

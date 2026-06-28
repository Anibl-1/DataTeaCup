/**
 * 仪表盘筛选器状态管理 Composable
 * 用于管理全局筛选器状态和筛选器与图表的联动
 */
import { ref, reactive, computed } from 'vue'
import type { FilterWidgetConfig, DashboardWidget } from '@/types/dashboard'

/**
 * 筛选器值类型
 */
export type FilterValue = string | number | boolean | null | string[] | [string, string]

/**
 * 筛选器变更事件
 */
export interface FilterChangeEvent {
  /** 筛选器ID */
  filterId: string
  /** 字段名 */
  field: string
  /** 筛选值 */
  value: FilterValue
  /** 关联的图表ID列表 */
  linkedCharts: string[]
}

/**
 * 级联筛选器配置
 */
export interface CascadeFilterConfig {
  /** 父筛选器ID */
  parentFilterId: string
  /** 子筛选器ID */
  childFilterId: string
  /** 父字段名 */
  parentField: string
  /** 子字段名 */
  childField: string
}

/**
 * 筛选器状态
 */
export interface FilterState {
  /** 筛选器ID */
  id: string
  /** 字段名 */
  field: string
  /** 当前值 */
  value: FilterValue
  /** 关联的图表ID列表 */
  linkedCharts: string[]
}

export interface UseDashboardFiltersOptions {
  /** 筛选器变更回调 */
  onFilterChange?: (event: FilterChangeEvent) => void
  /** 级联筛选器配置 */
  cascadeConfigs?: CascadeFilterConfig[]
}

export function useDashboardFilters(options: UseDashboardFiltersOptions = {}) {
  const { onFilterChange, cascadeConfigs = [] } = options

  // 筛选器值存储 - 使用 reactive 确保响应式
  const filterValues = reactive<Record<string, FilterValue>>({})
  
  // 筛选器配置存储
  const filterConfigs = ref<Record<string, FilterState>>({})
  
  // 级联筛选器配置
  const cascadeConfigsRef = ref<CascadeFilterConfig[]>(cascadeConfigs)

  /**
   * 设置筛选器值
   * @param filterId 筛选器ID
   * @param value 筛选值
   * @param triggerChange 是否触发变更事件，默认 true
   */
  const setFilterValue = (
    filterId: string,
    value: FilterValue,
    triggerChange: boolean = true
  ) => {
    const oldValue = filterValues[filterId]
    filterValues[filterId] = value

    // 如果值没有变化，不触发事件
    if (JSON.stringify(oldValue) === JSON.stringify(value)) {
      return
    }

    const config = filterConfigs.value[filterId]
    
    if (triggerChange && config) {
      // 触发变更回调
      onFilterChange?.({
        filterId,
        field: config.field,
        value,
        linkedCharts: config.linkedCharts
      })

      // 处理级联筛选器
      handleCascadeFilters(filterId, value)
    }
  }

  /**
   * 获取筛选器值
   * @param filterId 筛选器ID
   * @returns 筛选值
   */
  const getFilterValue = (filterId: string): FilterValue => {
    return filterValues[filterId] ?? null
  }

  /**
   * 清除所有筛选器值
   */
  const clearFilters = () => {
    Object.keys(filterValues).forEach(key => {
      filterValues[key] = null
    })
    
    // 触发所有筛选器的变更事件
    Object.values(filterConfigs.value).forEach(config => {
      onFilterChange?.({
        filterId: config.id,
        field: config.field,
        value: null,
        linkedCharts: config.linkedCharts
      })
    })
  }

  /**
   * 清除单个筛选器值
   * @param filterId 筛选器ID
   */
  const clearFilter = (filterId: string) => {
    setFilterValue(filterId, null)
  }

  /**
   * 获取指定图表的筛选条件
   * @param chartId 图表ID
   * @returns 筛选条件对象 { field: value }
   */
  const getFiltersForChart = (chartId: string): Record<string, FilterValue> => {
    const result: Record<string, FilterValue> = {}
    
    Object.entries(filterConfigs.value).forEach(([filterId, config]) => {
      // 检查该筛选器是否关联到此图表
      if (config.linkedCharts.includes(chartId)) {
        const value = filterValues[filterId]
        if (value !== null && value !== undefined && value !== '') {
          result[config.field] = value
        }
      }
    })
    
    return result
  }

  /**
   * 注册筛选器
   * @param filterId 筛选器ID
   * @param config 筛选器配置
   */
  const registerFilter = (filterId: string, config: Partial<FilterState>) => {
    filterConfigs.value[filterId] = {
      id: filterId,
      field: config.field || '',
      value: config.value ?? null,
      linkedCharts: config.linkedCharts || []
    }
    
    // 初始化筛选器值
    if (filterValues[filterId] === undefined) {
      filterValues[filterId] = config.value ?? null
    }
  }

  /**
   * 从 Widget 配置注册筛选器
   * @param widget 仪表盘组件
   */
  const registerFilterFromWidget = (widget: DashboardWidget) => {
    if (widget.type !== 'filter') return
    
    const config = widget.config as FilterWidgetConfig
    registerFilter(widget.i, {
      field: config.field,
      linkedCharts: config.linkedCharts || [],
      value: config.defaultValue ?? null
    })
  }

  /**
   * 注销筛选器
   * @param filterId 筛选器ID
   */
  const unregisterFilter = (filterId: string) => {
    delete filterConfigs.value[filterId]
    delete filterValues[filterId]
  }

  /**
   * 更新筛选器关联的图表
   * @param filterId 筛选器ID
   * @param linkedCharts 关联的图表ID列表
   */
  const updateLinkedCharts = (filterId: string, linkedCharts: string[]) => {
    if (filterConfigs.value[filterId]) {
      filterConfigs.value[filterId].linkedCharts = linkedCharts
    }
  }

  /**
   * 处理级联筛选器
   * @param parentFilterId 父筛选器ID
   * @param parentValue 父筛选器值
   */
  const handleCascadeFilters = (parentFilterId: string, _parentValue: FilterValue) => {
    // 找到所有以此筛选器为父级的级联配置
    const childConfigs = cascadeConfigsRef.value.filter(
      c => c.parentFilterId === parentFilterId
    )
    
    // 清除子筛选器的值
    childConfigs.forEach(config => {
      // 当父筛选器值变化时，清除子筛选器的值
      setFilterValue(config.childFilterId, null, true)
    })
  }

  /**
   * 添加级联筛选器配置
   * @param config 级联配置
   */
  const addCascadeConfig = (config: CascadeFilterConfig) => {
    cascadeConfigsRef.value.push(config)
  }

  /**
   * 移除级联筛选器配置
   * @param parentFilterId 父筛选器ID
   * @param childFilterId 子筛选器ID
   */
  const removeCascadeConfig = (parentFilterId: string, childFilterId: string) => {
    cascadeConfigsRef.value = cascadeConfigsRef.value.filter(
      c => !(c.parentFilterId === parentFilterId && c.childFilterId === childFilterId)
    )
  }

  /**
   * 获取所有筛选器值
   * @returns 所有筛选器值的副本
   */
  const getAllFilterValues = (): Record<string, FilterValue> => {
    return { ...filterValues }
  }

  /**
   * 批量设置筛选器值
   * @param values 筛选器值对象
   * @param triggerChange 是否触发变更事件
   */
  const setFilterValues = (
    values: Record<string, FilterValue>,
    triggerChange: boolean = true
  ) => {
    Object.entries(values).forEach(([filterId, value]) => {
      setFilterValue(filterId, value, triggerChange)
    })
  }

  /**
   * 检查是否有任何筛选器有值
   */
  const hasActiveFilters = computed(() => {
    return Object.values(filterValues).some(
      value => value !== null && value !== undefined && value !== ''
    )
  })

  /**
   * 获取活跃筛选器数量
   */
  const activeFilterCount = computed(() => {
    return Object.values(filterValues).filter(
      value => value !== null && value !== undefined && value !== ''
    ).length
  })

  return {
    // 状态
    filterValues,
    filterConfigs,
    hasActiveFilters,
    activeFilterCount,
    
    // 方法
    setFilterValue,
    getFilterValue,
    clearFilters,
    clearFilter,
    getFiltersForChart,
    registerFilter,
    registerFilterFromWidget,
    unregisterFilter,
    updateLinkedCharts,
    getAllFilterValues,
    setFilterValues,
    
    // 级联筛选器
    addCascadeConfig,
    removeCascadeConfig
  }
}

/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 图表实时预览 Composable
 * 实现所见即所得的配置体验
 * 验证需求: 3.1, 3.4
 */
import { ref, computed, type Ref, type ComputedRef } from 'vue'

export interface ChartPreviewConfig {
  /** 防抖延迟时间（毫秒），默认 200ms 以满足 300ms 内更新的需求 */
  debounceDelay?: number
  /** 节流延迟时间（毫秒），用于高频更新场景 */
  throttleDelay?: number
  /** 是否启用实时预览 */
  enableRealtime?: boolean
  /** 预览更新回调 */
  onPreviewUpdate?: () => void
  /** 预览错误回调 */
  onPreviewError?: (error: Error) => void
}

export interface ChartPreviewState {
  /** 是否正在更新预览 */
  isUpdating: Ref<boolean>
  /** 上次更新时间 */
  lastUpdateTime: Ref<number>
  /** 更新计数 */
  updateCount: Ref<number>
  /** 预览是否就绪 */
  isReady: ComputedRef<boolean>
  /** 更新延迟（毫秒） */
  updateLatency: Ref<number>
}

export interface UseChartPreviewReturn {
  /** 预览状态 */
  state: ChartPreviewState
  /** 触发预览更新（防抖） */
  triggerUpdate: () => void
  /** 立即更新预览 */
  updateNow: () => void
  /** 触发预览更新（节流） */
  triggerThrottledUpdate: () => void
  /** 重置预览状态 */
  reset: () => void
  /** 暂停实时预览 */
  pause: () => void
  /** 恢复实时预览 */
  resume: () => void
  /** 是否暂停 */
  isPaused: Ref<boolean>
}

/**
 * 简单的防抖函数实现
 */
function debounce<T extends (...args: any[]) => any>(
  fn: T,
  delay: number
): (...args: Parameters<T>) => void {
  let timeoutId: ReturnType<typeof setTimeout> | null = null
  
  return (...args: Parameters<T>) => {
    if (timeoutId) {
      clearTimeout(timeoutId)
    }
    timeoutId = setTimeout(() => {
      fn(...args)
      timeoutId = null
    }, delay)
  }
}

/**
 * 简单的节流函数实现
 */
function throttle<T extends (...args: any[]) => any>(
  fn: T,
  delay: number
): (...args: Parameters<T>) => void {
  let lastCall = 0
  let timeoutId: ReturnType<typeof setTimeout> | null = null
  
  return (...args: Parameters<T>) => {
    const now = Date.now()
    const remaining = delay - (now - lastCall)
    
    if (remaining <= 0) {
      if (timeoutId) {
        clearTimeout(timeoutId)
        timeoutId = null
      }
      lastCall = now
      fn(...args)
    } else if (!timeoutId) {
      timeoutId = setTimeout(() => {
        lastCall = Date.now()
        timeoutId = null
        fn(...args)
      }, remaining)
    }
  }
}

/**
 * 图表实时预览 Hook
 * 提供防抖和节流的预览更新机制，确保配置变更在 300ms 内反映到预览
 */
export function useChartPreview(
  updateFn: () => void | Promise<void>,
  config: ChartPreviewConfig = {}
): UseChartPreviewReturn {
  const {
    debounceDelay = 200, // 默认 200ms，确保在 300ms 内完成更新
    throttleDelay = 100,
    enableRealtime = true,
    onPreviewUpdate,
    onPreviewError
  } = config

  // 状态
  const isUpdating = ref(false)
  const lastUpdateTime = ref(0)
  const updateCount = ref(0)
  const updateLatency = ref(0)
  const isPaused = ref(false)
  const isRealtimeEnabled = ref(enableRealtime)

  // 计算属性
  const isReady = computed(() => !isUpdating.value && lastUpdateTime.value > 0)

  // 执行更新的核心函数
  const executeUpdate = async () => {
    if (isPaused.value || !isRealtimeEnabled.value) return

    const startTime = performance.now()
    isUpdating.value = true

    try {
      await updateFn()
      updateCount.value++
      lastUpdateTime.value = Date.now()
      updateLatency.value = Math.round(performance.now() - startTime)
      onPreviewUpdate?.()
    } catch (error) {
      console.error('[ChartPreview] Update failed:', error)
      onPreviewError?.(error as Error)
    } finally {
      isUpdating.value = false
    }
  }

  // 防抖更新（用于配置变更）
  const triggerUpdate = debounce(executeUpdate, debounceDelay)

  // 节流更新（用于高频操作如拖拽）
  const triggerThrottledUpdate = throttle(executeUpdate, throttleDelay)

  // 立即更新
  const updateNow = () => {
    executeUpdate()
  }

  // 重置状态
  const reset = () => {
    isUpdating.value = false
    lastUpdateTime.value = 0
    updateCount.value = 0
    updateLatency.value = 0
    isPaused.value = false
  }

  // 暂停/恢复
  const pause = () => {
    isPaused.value = true
  }

  const resume = () => {
    isPaused.value = false
  }

  return {
    state: {
      isUpdating,
      lastUpdateTime,
      updateCount,
      isReady,
      updateLatency
    },
    triggerUpdate,
    updateNow,
    triggerThrottledUpdate,
    reset,
    pause,
    resume,
    isPaused
  }
}

/**
 * 配置项提示信息
 * 验证需求: 3.6 - 悬停显示配置项使用提示
 */
export const CONFIG_TOOLTIPS = {
  // 基础配置
  chartTitle: '设置图表的标题，将显示在图表顶部',
  titlePosition: '标题的水平位置：居中、左侧或右侧',
  chartDescription: '图表的描述信息，用于说明图表的用途',
  
  // 颜色配置
  colorScheme: '选择预设的颜色方案，影响图表中所有数据系列的颜色',
  customColor: '自定义主题颜色，将作为第一个系列的颜色',
  backgroundColor: '图表的背景颜色，浅色适合打印，深色适合演示',
  colorPresets: '推荐的颜色搭配方案，包含背景色和数据颜色的最佳组合',
  
  // 图表配置
  showLegend: '是否显示图例，图例用于标识不同的数据系列',
  legendPosition: '图例的显示位置：顶部、底部、左侧或右侧',
  showGrid: '是否显示网格线，帮助读取数据值',
  showLabel: '是否在数据点上显示数值标签',
  
  // 折线图配置
  smooth: '是否使用平滑曲线连接数据点',
  showSymbol: '是否在数据点位置显示标记符号',
  symbolSize: '数据点标记的大小（像素）',
  
  // 柱状图配置
  barWidth: '柱子的宽度，可以是像素值或 "auto"',
  barMaxWidth: '柱子的最大宽度限制（像素）',
  
  // 表格配置
  striped: '是否显示斑马纹（交替行背景色）',
  showIndex: '是否显示行序号列',
  tableSize: '表格的整体尺寸：小、中、大',
  showPagination: '是否显示分页控件',
  pageSize: '每页显示的数据行数',
  headerBgColor: '表头的背景颜色',
  headerTextColor: '表头的文字颜色',
  headerFontWeight: '表头文字的粗细',
  headerAlign: '表头文字的对齐方式',
  enableExport: '是否允许导出表格数据为 Excel 文件',
  exportFileName: '导出文件的名称，留空则使用图表名称',
  
  // 条件格式化
  enableConditionalFormat: '启用条件格式化，根据数值自动设置单元格样式',
  conditionalFormatField: '选择要应用条件格式化的数值字段',
  conditionalFormatType: '格式化类型：颜色渐变或数据条',
  conditionalFormatColors: '条件格式化的颜色梯度，从低值到高值',
  
  // 汇总表配置
  showSummary: '是否在表格底部显示汇总行',
  summaryColumns: '选择需要计算汇总的数值列',
  summaryLabel: '汇总行的标签文字',
  
  // 透视表配置
  pivotRowField: '透视表的行字段，用于分组显示',
  pivotColField: '透视表的列字段，将展开为多列',
  pivotValueField: '透视表的值字段，用于计算汇总值',
  pivotAggType: '透视表的聚合方式：求和、平均、最大、最小或计数',
  pivotShowRowTotal: '是否显示每行的合计值',
  pivotShowColTotal: '是否显示每列的合计值',
  
  // 交互配置
  enableZoom: '是否启用图表缩放功能',
  enableDataZoom: '是否显示数据缩放滑块',
  enableLegendSelect: '是否允许点击图例筛选数据系列',
  enableTooltip: '是否显示悬停提示框',
  enableToolbox: '是否显示工具箱（保存图片、数据视图等）',
  enableClick: '是否启用数据点点击事件',
  enableHover: '是否启用悬停高亮效果',
  
  // 尺寸配置
  chartWidth: '图表的宽度（像素），留空则自适应容器',
  chartHeight: '图表的高度（像素），默认 400 像素',
  animation: '是否启用图表动画效果',
  animationDuration: '动画的持续时间（毫秒）',
  
  // 数据配置
  dataLimit: '限制查询返回的数据行数，避免数据量过大导致卡顿',
  xAxis: '选择作为 X 轴（维度）的字段，通常是分类或时间字段',
  yAxis: '选择作为 Y 轴（指标）的字段，通常是数值字段',
  groupField: '选择分组字段，用于创建多系列图表',
  aggregateFunction: '选择聚合函数：不聚合、求和、平均、计数、最大或最小',
  fieldAlias: '设置字段的显示别名，用于图例和标签',
  sortField: '选择排序依据：按 X 轴字段或按 Y 轴数值',
  sortOrder: '排序方向：升序或降序'
}

/**
 * 获取配置项的提示信息
 */
export function getConfigTooltip(key: keyof typeof CONFIG_TOOLTIPS): string {
  return CONFIG_TOOLTIPS[key] || ''
}

/**
 * 图表主题切换动画配置
 * 验证需求: 3.3 - 主题切换在 200ms 内完成
 */
export const THEME_TRANSITION_CONFIG = {
  duration: 200, // 200ms 内完成主题切换
  easing: 'ease-out'
}

/**
 * 预览更新配置
 * 验证需求: 3.4 - 配置修改在 300ms 内更新预览
 */
export const PREVIEW_UPDATE_CONFIG = {
  debounceDelay: 200, // 防抖延迟
  maxWait: 300, // 最大等待时间
  throttleDelay: 100 // 节流延迟
}

/**
 * 图表主题切换 Composable
 * 实现主题一键切换功能，确保切换在 200ms 内完成
 * 验证需求: 3.2, 3.3
 */
import { ref, computed, type Ref, type ComputedRef } from 'vue'
import echarts from '@/utils/echarts'
import {
  chartThemes,
  getTheme,
  switchTheme,
  registerThemes,
  applyThemeToOption,
  getThemePreviewColors,
  isDarkTheme,
  getAllThemeNames,
  type ChartThemeConfig,
  type ThemeSwitchOptions
} from '@/utils/chartThemes'

/**
 * 主题切换状态
 */
export interface ThemeSwitchState {
  /** 当前主题名称 */
  currentTheme: Ref<string>
  /** 当前主题配置 */
  currentThemeConfig: ComputedRef<ChartThemeConfig>
  /** 是否正在切换主题 */
  isSwitching: Ref<boolean>
  /** 上次切换耗时（毫秒） */
  lastSwitchDuration: Ref<number>
  /** 是否为深色主题 */
  isDark: ComputedRef<boolean>
  /** 可用主题列表 */
  availableThemes: ComputedRef<ChartThemeConfig[]>
}

/**
 * 主题切换配置
 */
export interface UseChartThemeConfig {
  /** 初始主题名称 */
  initialTheme?: string
  /** 是否使用动画 */
  useAnimation?: boolean
  /** 主题切换回调 */
  onThemeChange?: (themeName: string, config: ChartThemeConfig) => void
  /** 切换完成回调 */
  onSwitchComplete?: (duration: number) => void
}

/**
 * useChartTheme 返回类型
 */
export interface UseChartThemeReturn {
  /** 主题状态 */
  state: ThemeSwitchState
  /** 切换主题 */
  switchTo: (themeName: string, chartInstance?: echarts.ECharts | null) => Promise<void>
  /** 切换到下一个主题 */
  switchToNext: (chartInstance?: echarts.ECharts | null) => Promise<void>
  /** 切换到上一个主题 */
  switchToPrev: (chartInstance?: echarts.ECharts | null) => Promise<void>
  /** 应用主题到图表选项 */
  applyToOption: (option: echarts.EChartsCoreOption) => echarts.EChartsCoreOption
  /** 获取主题预览颜色 */
  getPreviewColors: (themeName?: string) => string[]
  /** 重置为默认主题 */
  resetToDefault: (chartInstance?: echarts.ECharts | null) => Promise<void>
}

/**
 * 图表主题切换 Hook
 * 
 * 功能：
 * 1. 管理当前主题状态
 * 2. 提供主题切换方法，确保在 200ms 内完成（需求 3.3）
 * 3. 支持 6 套预设主题（需求 3.2）
 * 4. 提供主题预览和应用功能
 * 
 * @param config 配置选项
 * @returns 主题管理方法和状态
 */
export function useChartTheme(config: UseChartThemeConfig = {}): UseChartThemeReturn {
  const {
    initialTheme = 'default',
    useAnimation = false,
    onThemeChange,
    onSwitchComplete
  } = config

  // 确保主题已注册
  registerThemes()

  // 状态
  const currentTheme = ref(initialTheme)
  const isSwitching = ref(false)
  const lastSwitchDuration = ref(0)

  // 计算属性
  const currentThemeConfig = computed(() => getTheme(currentTheme.value))
  const isDark = computed(() => isDarkTheme(currentTheme.value))
  const availableThemes = computed(() => Object.values(chartThemes))
  const themeNames = getAllThemeNames()

  /**
   * 切换到指定主题
   * 确保切换在 200ms 内完成（需求 3.3）
   */
  const switchTo = async (
    themeName: string,
    chartInstance?: echarts.ECharts | null
  ): Promise<void> => {
    if (isSwitching.value) return
    if (!chartThemes[themeName]) {
      console.warn(`[useChartTheme] Theme "${themeName}" not found, using default`)
      themeName = 'default'
    }

    const startTime = performance.now()
    isSwitching.value = true

    try {
      // 更新当前主题
      currentTheme.value = themeName
      const themeConfig = getTheme(themeName)

      // 如果提供了图表实例，应用主题
      if (chartInstance) {
        const options: ThemeSwitchOptions = {
          animation: useAnimation,
          // 使用 150ms 动画时间，确保总时间在 200ms 内
          animationDuration: useAnimation ? 150 : 0
        }
        await switchTheme(chartInstance, themeName, options)
      }

      // 计算切换耗时
      const duration = Math.round(performance.now() - startTime)
      lastSwitchDuration.value = duration

      // 触发回调
      onThemeChange?.(themeName, themeConfig)
      onSwitchComplete?.(duration)

      // 验证是否在 200ms 内完成
      if (duration > 200) {
        console.warn(`[useChartTheme] Theme switch took ${duration}ms, exceeding 200ms target`)
      }
    } finally {
      isSwitching.value = false
    }
  }

  /**
   * 切换到下一个主题
   */
  const switchToNext = async (chartInstance?: echarts.ECharts | null): Promise<void> => {
    const currentIndex = themeNames.indexOf(currentTheme.value)
    const nextIndex = (currentIndex + 1) % themeNames.length
    await switchTo(themeNames[nextIndex]!, chartInstance)
  }

  /**
   * 切换到上一个主题
   */
  const switchToPrev = async (chartInstance?: echarts.ECharts | null): Promise<void> => {
    const currentIndex = themeNames.indexOf(currentTheme.value)
    const prevIndex = (currentIndex - 1 + themeNames.length) % themeNames.length
    await switchTo(themeNames[prevIndex]!, chartInstance)
  }

  /**
   * 应用当前主题到图表选项
   */
  const applyToOption = (option: echarts.EChartsCoreOption): echarts.EChartsCoreOption => {
    return applyThemeToOption(option, currentTheme.value)
  }

  /**
   * 获取主题预览颜色
   */
  const getPreviewColors = (themeName?: string): string[] => {
    return getThemePreviewColors(themeName || currentTheme.value)
  }

  /**
   * 重置为默认主题
   */
  const resetToDefault = async (chartInstance?: echarts.ECharts | null): Promise<void> => {
    await switchTo('default', chartInstance)
  }

  return {
    state: {
      currentTheme,
      currentThemeConfig,
      isSwitching,
      lastSwitchDuration,
      isDark,
      availableThemes
    },
    switchTo,
    switchToNext,
    switchToPrev,
    applyToOption,
    getPreviewColors,
    resetToDefault
  }
}

/**
 * 主题切换性能监控
 * 用于验证需求 3.3 - 主题切换在 200ms 内完成
 */
export async function measureThemeSwitchPerformance(
  chartInstance: echarts.ECharts,
  themeName: string
): Promise<{ success: boolean; duration: number }> {
  const startTime = performance.now()
  
  try {
    await switchTheme(chartInstance, themeName, { animation: false })
    const duration = Math.round(performance.now() - startTime)
    
    return {
      success: duration <= 200,
      duration
    }
  } catch (error) {
    console.error('[measureThemeSwitchPerformance] Error:', error)
    return {
      success: false,
      duration: -1
    }
  }
}

export default useChartTheme

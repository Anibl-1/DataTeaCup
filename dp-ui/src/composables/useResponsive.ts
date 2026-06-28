/**
 * 响应式布局服务
 * Responsive Layout Service
 * 
 * 实现断点检测，支持 1280/1366/1440/1920 四种屏幕尺寸
 * Implements breakpoint detection for 1280/1366/1440/1920 screen sizes
 * 
 * Validates: Requirements 1.2
 */

import { ref, computed, onMounted, onUnmounted, readonly, type Ref, type ComputedRef } from 'vue'
import { themeConfig } from '@/config/theme'

// ============================================================================
// Types
// ============================================================================

/**
 * 断点类型
 * Breakpoint type based on theme configuration
 */
export type Breakpoint = 'xs' | 'sm' | 'md' | 'lg'

/**
 * 响应式状态接口
 * Responsive state interface as defined in design document
 */
export interface ResponsiveState {
  /** 当前断点 */
  breakpoint: Breakpoint
  /** 当前屏幕宽度 */
  width: number
  /** 是否为移动设备 (< 1280px) */
  isMobile: boolean
  /** 是否为平板设备 (1280px - 1440px) */
  isTablet: boolean
  /** 是否为桌面设备 (>= 1440px) */
  isDesktop: boolean
}

/**
 * useResponsive 返回类型
 */
export interface UseResponsiveReturn {
  /** 当前断点 (响应式) */
  breakpoint: ComputedRef<Breakpoint>
  /** 当前屏幕宽度 (响应式) */
  width: Readonly<Ref<number>>
  /** 是否为移动设备 */
  isMobile: ComputedRef<boolean>
  /** 是否为平板设备 */
  isTablet: ComputedRef<boolean>
  /** 是否为桌面设备 */
  isDesktop: ComputedRef<boolean>
  /** 是否为大屏幕 (>= 1920px) */
  isLargeScreen: ComputedRef<boolean>
  /** 是否为小屏幕 (< 1366px) */
  isSmallScreen: ComputedRef<boolean>
  /** 当前断点配置 */
  breakpoints: typeof BREAKPOINTS
  /** 检查是否大于等于指定断点 */
  isBreakpointUp: (bp: Breakpoint) => boolean
  /** 检查是否小于指定断点 */
  isBreakpointDown: (bp: Breakpoint) => boolean
  /** 检查是否在指定断点范围内 */
  isBreakpointBetween: (min: Breakpoint, max: Breakpoint) => boolean
}

// ============================================================================
// Constants
// ============================================================================

/**
 * 断点配置 - 从主题配置获取
 * Breakpoint configuration from theme config
 * 
 * xs: 1280px (小屏幕)
 * sm: 1366px (中小屏幕)
 * md: 1440px (中等屏幕)
 * lg: 1920px (大屏幕)
 */
export const BREAKPOINTS = {
  xs: themeConfig.breakpoints.xs,  // 1280
  sm: themeConfig.breakpoints.sm,  // 1366
  md: themeConfig.breakpoints.md,  // 1440
  lg: themeConfig.breakpoints.lg   // 1920
} as const

/**
 * 断点顺序 (从小到大)
 */
const BREAKPOINT_ORDER: Breakpoint[] = ['xs', 'sm', 'md', 'lg']

/**
 * 默认防抖延迟 (毫秒)
 */
const DEFAULT_DEBOUNCE_DELAY = 100

// ============================================================================
// Utility Functions
// ============================================================================

/**
 * 根据屏幕宽度获取当前断点
 * Get current breakpoint based on screen width
 * 
 * @param width - 屏幕宽度
 * @returns 当前断点
 */
export function getBreakpointFromWidth(width: number): Breakpoint {
  if (width >= BREAKPOINTS.lg) {
    return 'lg'
  }
  if (width >= BREAKPOINTS.md) {
    return 'md'
  }
  if (width >= BREAKPOINTS.sm) {
    return 'sm'
  }
  return 'xs'
}

/**
 * 获取断点的数值索引
 */
function getBreakpointIndex(bp: Breakpoint): number {
  return BREAKPOINT_ORDER.indexOf(bp)
}

// ============================================================================
// Composable
// ============================================================================

/**
 * 响应式布局 Composable
 * Responsive layout composable for detecting screen breakpoints
 * 
 * @param options - 配置选项
 * @returns 响应式状态和工具函数
 * 
 * @example
 * ```ts
 * const { breakpoint, isMobile, isDesktop, width } = useResponsive()
 * 
 * // 在模板中使用
 * <div v-if="isMobile">Mobile View</div>
 * <div v-else-if="isDesktop">Desktop View</div>
 * 
 * // 检查断点
 * if (isBreakpointUp('md')) {
 *   // 中等屏幕及以上
 * }
 * ```
 */
export function useResponsive(options?: {
  /** 防抖延迟 (毫秒) */
  debounceDelay?: number
}): UseResponsiveReturn {
  const { debounceDelay = DEFAULT_DEBOUNCE_DELAY } = options || {}

  // 响应式状态
  const width = ref(getInitialWidth())
  
  // 防抖定时器
  let resizeTimer: ReturnType<typeof setTimeout> | null = null

  /**
   * 获取初始宽度
   */
  function getInitialWidth(): number {
    if (typeof window === 'undefined') {
      return BREAKPOINTS.md // SSR 默认返回中等屏幕
    }
    return window.innerWidth
  }

  /**
   * 更新屏幕宽度 (带防抖)
   */
  function updateWidth(): void {
    if (resizeTimer) {
      clearTimeout(resizeTimer)
    }
    
    resizeTimer = setTimeout(() => {
      if (typeof window !== 'undefined') {
        width.value = window.innerWidth
      }
    }, debounceDelay)
  }

  /**
   * 立即更新屏幕宽度 (无防抖)
   */
  function updateWidthImmediate(): void {
    if (typeof window !== 'undefined') {
      width.value = window.innerWidth
    }
  }

  // 计算属性
  const breakpoint = computed<Breakpoint>(() => getBreakpointFromWidth(width.value))
  
  // 设备类型判断
  // isMobile: 小于 xs 断点 (< 1280px)
  const isMobile = computed(() => width.value < BREAKPOINTS.xs)
  
  // isTablet: xs 到 md 之间 (1280px - 1440px)
  const isTablet = computed(() => 
    width.value >= BREAKPOINTS.xs && width.value < BREAKPOINTS.md
  )
  
  // isDesktop: md 及以上 (>= 1440px)
  const isDesktop = computed(() => width.value >= BREAKPOINTS.md)
  
  // 额外的便捷属性
  const isLargeScreen = computed(() => width.value >= BREAKPOINTS.lg)
  const isSmallScreen = computed(() => width.value < BREAKPOINTS.sm)

  /**
   * 检查是否大于等于指定断点
   */
  function isBreakpointUp(bp: Breakpoint): boolean {
    return width.value >= BREAKPOINTS[bp]
  }

  /**
   * 检查是否小于指定断点
   */
  function isBreakpointDown(bp: Breakpoint): boolean {
    return width.value < BREAKPOINTS[bp]
  }

  /**
   * 检查是否在指定断点范围内
   */
  function isBreakpointBetween(min: Breakpoint, max: Breakpoint): boolean {
    const minIndex = getBreakpointIndex(min)
    const maxIndex = getBreakpointIndex(max)
    
    if (minIndex > maxIndex) {
      console.warn(`[useResponsive] Invalid breakpoint range: ${min} > ${max}`)
      return false
    }
    
    return width.value >= BREAKPOINTS[min] && width.value < BREAKPOINTS[max]
  }

  // 生命周期
  onMounted(() => {
    if (typeof window === 'undefined') return

    // 初始化时立即更新
    updateWidthImmediate()

    // 监听 resize 事件
    window.addEventListener('resize', updateWidth, { passive: true })
    
    // 监听屏幕方向变化
    window.addEventListener('orientationchange', updateWidth, { passive: true })
  })

  onUnmounted(() => {
    if (typeof window === 'undefined') return

    // 清理事件监听
    window.removeEventListener('resize', updateWidth)
    window.removeEventListener('orientationchange', updateWidth)
    
    // 清理定时器
    if (resizeTimer) {
      clearTimeout(resizeTimer)
      resizeTimer = null
    }
  })

  return {
    breakpoint,
    width: readonly(width),
    isMobile,
    isTablet,
    isDesktop,
    isLargeScreen,
    isSmallScreen,
    breakpoints: BREAKPOINTS,
    isBreakpointUp,
    isBreakpointDown,
    isBreakpointBetween
  }
}

// ============================================================================
// Default Export
// ============================================================================

export default useResponsive

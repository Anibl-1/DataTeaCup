import { ref, computed, onMounted, onUnmounted } from 'vue'

/**
 * 响应式断点 Hook
 * 用于检测当前屏幕尺寸并返回相应的断点信息
 */
export function useBreakpoint() {
  const width = ref(typeof window !== 'undefined' ? window.innerWidth : 1200)
  const height = ref(typeof window !== 'undefined' ? window.innerHeight : 800)

  // 防抖更新
  let resizeTimer: ReturnType<typeof setTimeout> | null = null
  const updateDimensions = () => {
    if (resizeTimer) clearTimeout(resizeTimer)
    resizeTimer = setTimeout(() => {
      width.value = window.innerWidth
      height.value = window.innerHeight
    }, 100)
  }

  onMounted(() => {
    window.addEventListener('resize', updateDimensions)
    // 监听屏幕方向变化
    window.addEventListener('orientationchange', updateDimensions)
    updateDimensions()
  })

  onUnmounted(() => {
    window.removeEventListener('resize', updateDimensions)
    window.removeEventListener('orientationchange', updateDimensions)
    if (resizeTimer) clearTimeout(resizeTimer)
  })

  // 断点判断
  const isMobile = computed(() => width.value < 768)
  const isTablet = computed(() => width.value >= 768 && width.value < 992)
  const isDesktop = computed(() => width.value >= 992)
  const isLargeDesktop = computed(() => width.value >= 1200)
  const isSmallMobile = computed(() => width.value < 576)

  // 当前断点名称
  const breakpoint = computed(() => {
    if (width.value < 576) return 'xs'
    if (width.value < 768) return 'sm'
    if (width.value < 992) return 'md'
    if (width.value < 1200) return 'lg'
    if (width.value < 1400) return 'xl'
    return 'xxl'
  })

  // 是否为触摸设备
  const isTouchDevice = computed(() => {
    if (typeof window === 'undefined') return false
    return 'ontouchstart' in window || navigator.maxTouchPoints > 0
  })

  // 是否为横屏
  const isLandscape = computed(() => width.value > height.value)

  // 响应式列数（用于网格布局）
  const gridColumns = computed(() => {
    if (width.value < 576) return 1
    if (width.value < 768) return 2
    if (width.value < 992) return 3
    if (width.value < 1200) return 4
    return 5
  })

  // 响应式尺寸（用于组件 size 属性）
  const componentSize = computed<'small' | 'medium' | 'large'>(() => {
    if (width.value < 768) return 'small'
    if (width.value < 1200) return 'medium'
    return 'large'
  })

  // 安全区域（适配刘海屏）
  const safeAreaInsets = computed(() => {
    if (typeof window === 'undefined') return { top: 0, bottom: 0, left: 0, right: 0 }
    const style = getComputedStyle(document.documentElement)
    const parseInset = (varName: string): number => {
      const val = style.getPropertyValue(varName)
      const parsed = parseInt(val, 10)
      return Number.isNaN(parsed) ? 0 : parsed
    }
    return {
      top: parseInset('--sat'),
      bottom: parseInset('--sab'),
      left: parseInset('--sal'),
      right: parseInset('--sar')
    }
  })

  return {
    width,
    height,
    isMobile,
    isTablet,
    isDesktop,
    isLargeDesktop,
    isSmallMobile,
    breakpoint,
    isTouchDevice,
    isLandscape,
    gridColumns,
    componentSize,
    safeAreaInsets
  }
}

/**
 * 媒体查询 Hook
 * @param query CSS媒体查询字符串
 */
export function useMediaQuery(query: string) {
  const matches = ref(false)

  onMounted(() => {
    const mediaQuery = window.matchMedia(query)
    matches.value = mediaQuery.matches

    const handler = (e: MediaQueryListEvent) => {
      matches.value = e.matches
    }

    mediaQuery.addEventListener('change', handler)
    
    onUnmounted(() => {
      mediaQuery.removeEventListener('change', handler)
    })
  })

  return matches
}

/**
 * 预设断点常量
 */
export const BREAKPOINTS = {
  xs: 0,
  sm: 576,
  md: 768,
  lg: 992,
  xl: 1200,
  xxl: 1400
} as const

export type BreakpointName = keyof typeof BREAKPOINTS

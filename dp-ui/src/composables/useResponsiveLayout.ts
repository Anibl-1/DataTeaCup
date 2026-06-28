/**
 * 响应式布局 Hook - 移动端适配
 * Responsive Layout Hook for Mobile Adaptation
 *
 * 基于 useBreakpoint 扩展，提供移动端专属布局能力
 * Validates: Requirements 26.1
 */

import { ref, computed, onMounted, onUnmounted, watch, type Ref, type ComputedRef } from 'vue'

// ============================================================================
// Types
// ============================================================================

export type DeviceType = 'mobile' | 'tablet' | 'desktop'

export interface ResponsiveLayoutState {
  deviceType: DeviceType
  width: number
  height: number
  isMobile: boolean
  isTablet: boolean
  isDesktop: boolean
  isLandscape: boolean
  isTouchDevice: boolean
  columns: number
  sidebarCollapsed: boolean
}

export interface UseResponsiveLayoutReturn {
  deviceType: ComputedRef<DeviceType>
  width: Readonly<Ref<number>>
  height: Readonly<Ref<number>>
  isMobile: ComputedRef<boolean>
  isTablet: ComputedRef<boolean>
  isDesktop: ComputedRef<boolean>
  isLandscape: ComputedRef<boolean>
  isTouchDevice: ComputedRef<boolean>
  columns: ComputedRef<number>
  sidebarCollapsed: Ref<boolean>
  containerPadding: ComputedRef<string>
  fontSize: ComputedRef<string>
  toggleSidebar: () => void
}

// ============================================================================
// Constants
// ============================================================================

/** 移动端断点 */
export const MOBILE_BREAKPOINT = 768
/** 平板断点 */
export const TABLET_BREAKPOINT = 1024

// ============================================================================
// Pure Functions (可测试)
// ============================================================================

/**
 * 根据宽度判断设备类型
 */
export function getDeviceType(width: number): DeviceType {
  if (width < MOBILE_BREAKPOINT) return 'mobile'
  if (width < TABLET_BREAKPOINT) return 'tablet'
  return 'desktop'
}

/**
 * 根据设备类型和方向计算列数
 */
export function getColumns(deviceType: DeviceType, isLandscape: boolean): number {
  if (deviceType === 'mobile') return isLandscape ? 2 : 1
  if (deviceType === 'tablet') return isLandscape ? 3 : 2
  return 4
}

/**
 * 根据设备类型获取容器内边距
 */
export function getContainerPadding(deviceType: DeviceType): string {
  if (deviceType === 'mobile') return '8px'
  if (deviceType === 'tablet') return '16px'
  return '24px'
}

/**
 * 根据设备类型获取基础字号
 */
export function getFontSize(deviceType: DeviceType): string {
  if (deviceType === 'mobile') return '14px'
  if (deviceType === 'tablet') return '14px'
  return '16px'
}

// ============================================================================
// Composable
// ============================================================================

export function useResponsiveLayout(): UseResponsiveLayoutReturn {
  const width = ref(typeof window !== 'undefined' ? window.innerWidth : 1200)
  const height = ref(typeof window !== 'undefined' ? window.innerHeight : 800)
  const sidebarCollapsed = ref(false)

  let resizeTimer: ReturnType<typeof setTimeout> | null = null

  function updateDimensions(): void {
    if (resizeTimer) clearTimeout(resizeTimer)
    resizeTimer = setTimeout(() => {
      if (typeof window !== 'undefined') {
        width.value = window.innerWidth
        height.value = window.innerHeight
      }
    }, 100)
  }

  const isLandscape = computed(() => width.value > height.value)

  const isTouchDevice = computed(() => {
    if (typeof window === 'undefined') return false
    return 'ontouchstart' in window || navigator.maxTouchPoints > 0
  })

  const deviceType = computed(() => getDeviceType(width.value))
  const isMobile = computed(() => deviceType.value === 'mobile')
  const isTablet = computed(() => deviceType.value === 'tablet')
  const isDesktop = computed(() => deviceType.value === 'desktop')
  const columns = computed(() => getColumns(deviceType.value, isLandscape.value))
  const containerPadding = computed(() => getContainerPadding(deviceType.value))
  const fontSize = computed(() => getFontSize(deviceType.value))

  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  // 移动端自动折叠侧边栏
  watch(deviceType, (newType) => {
    if (newType === 'mobile') {
      sidebarCollapsed.value = true
    }
  })

  onMounted(() => {
    if (typeof window === 'undefined') return
    window.addEventListener('resize', updateDimensions, { passive: true })
    window.addEventListener('orientationchange', updateDimensions, { passive: true })
    // 初始化
    width.value = window.innerWidth
    height.value = window.innerHeight
    if (width.value < MOBILE_BREAKPOINT) {
      sidebarCollapsed.value = true
    }
  })

  onUnmounted(() => {
    if (typeof window === 'undefined') return
    window.removeEventListener('resize', updateDimensions)
    window.removeEventListener('orientationchange', updateDimensions)
    if (resizeTimer) clearTimeout(resizeTimer)
  })

  return {
    deviceType,
    width,
    height,
    isMobile,
    isTablet,
    isDesktop,
    isLandscape,
    isTouchDevice,
    columns,
    sidebarCollapsed,
    containerPadding,
    fontSize,
    toggleSidebar
  }
}

export default useResponsiveLayout

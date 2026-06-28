/**
 * 应用全局状态 Store
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { throttle } from '@/utils/debounce'

export const useAppStore = defineStore('app', () => {
  // 事件处理器引用（用于清理）
  let resizeHandler: (() => void) | null = null
  let onlineHandler: (() => void) | null = null
  let offlineHandler: (() => void) | null = null
  // 全局加载状态
  const loadingCount = ref(0)
  const loadingText = ref('')
  
  // 是否正在加载
  const isLoading = computed(() => loadingCount.value > 0)
  
  // 侧边栏折叠状态
  const sidebarCollapsed = ref(false)
  
  // 设备类型
  const device = ref<'desktop' | 'tablet' | 'mobile'>('desktop')
  
  // 移动端模式: auto=自动检测, mobile=强制移动端, desktop=强制桌面端
  const mobileMode = ref<'auto' | 'mobile' | 'desktop'>(
    (typeof localStorage !== 'undefined' ? localStorage.getItem('dp-mobile-mode') : null) as 'auto' | 'mobile' | 'desktop' || 'auto'
  )
  
  // 综合判断当前是否为移动端视图
  const isMobileView = computed(() => {
    if (mobileMode.value === 'mobile') return true
    if (mobileMode.value === 'desktop') return false
    return device.value === 'mobile'
  })
  
  // 网络状态
  const isOnline = ref(navigator.onLine)
  
  /**
   * 开始加载
   */
  function startLoading(text = '加载中...') {
    loadingCount.value++
    loadingText.value = text
  }
  
  /**
   * 结束加载
   */
  function stopLoading() {
    if (loadingCount.value > 0) {
      loadingCount.value--
    }
    if (loadingCount.value === 0) {
      loadingText.value = ''
    }
  }
  
  /**
   * 重置加载状态
   */
  function resetLoading() {
    loadingCount.value = 0
    loadingText.value = ''
  }
  
  /**
   * 切换侧边栏
   */
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }
  
  /**
   * 设置设备类型
   */
  function setDevice(type: 'desktop' | 'tablet' | 'mobile') {
    device.value = type
  }
  
  /**
   * 设置移动端模式
   */
  function setMobileMode(mode: 'auto' | 'mobile' | 'desktop') {
    mobileMode.value = mode
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('dp-mobile-mode', mode)
    }
  }
  
  /**
   * 检测设备类型
   */
  function detectDevice() {
    const width = window.innerWidth
    if (width < 768) {
      device.value = 'mobile'
    } else if (width < 1024) {
      device.value = 'tablet'
    } else {
      device.value = 'desktop'
    }
  }
  
  /**
   * 初始化
   */
  function init() {
    // 先清理旧的监听器（防止重复绑定）
    cleanup()
    
    // 检测设备（节流 200ms 降低 resize 频率）
    detectDevice()
    resizeHandler = throttle(detectDevice, 200)
    window.addEventListener('resize', resizeHandler)
    
    // 监听网络状态
    onlineHandler = () => { isOnline.value = true }
    offlineHandler = () => { isOnline.value = false }
    window.addEventListener('online', onlineHandler)
    window.addEventListener('offline', offlineHandler)
  }
  
  /**
   * 清理事件监听器
   */
  function cleanup() {
    if (resizeHandler) {
      window.removeEventListener('resize', resizeHandler)
      resizeHandler = null
    }
    if (onlineHandler) {
      window.removeEventListener('online', onlineHandler)
      onlineHandler = null
    }
    if (offlineHandler) {
      window.removeEventListener('offline', offlineHandler)
      offlineHandler = null
    }
  }
  
  return {
    loadingCount,
    loadingText,
    isLoading,
    sidebarCollapsed,
    device,
    mobileMode,
    isMobileView,
    isOnline,
    startLoading,
    stopLoading,
    resetLoading,
    toggleSidebar,
    setDevice,
    setMobileMode,
    detectDevice,
    init,
    cleanup
  }
})

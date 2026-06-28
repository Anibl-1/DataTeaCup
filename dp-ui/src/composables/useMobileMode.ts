import { computed } from 'vue'
import { useAppStore } from '@/stores/app'

/**
 * 移动端模式管理 Hook
 * 支持 auto/mobile/desktop 三种模式切换
 * 参考 DataEase 的桌面端/移动端切换功能
 */
export function useMobileMode() {
  const appStore = useAppStore()

  const isMobileView = computed(() => appStore.isMobileView)
  const mobileMode = computed(() => appStore.mobileMode)
  const isAutoMode = computed(() => appStore.mobileMode === 'auto')
  const isForced = computed(() => appStore.mobileMode !== 'auto')

  function setMode(mode: 'auto' | 'mobile' | 'desktop') {
    appStore.setMobileMode(mode)
  }

  function toggleMode() {
    if (appStore.isMobileView) {
      appStore.setMobileMode('desktop')
    } else {
      appStore.setMobileMode('mobile')
    }
  }

  function resetToAuto() {
    appStore.setMobileMode('auto')
  }

  return {
    isMobileView,
    mobileMode,
    isAutoMode,
    isForced,
    setMode,
    toggleMode,
    resetToAuto
  }
}

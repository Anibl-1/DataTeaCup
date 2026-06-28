/**
 * 加载状态 Hook
 */
import { ref, computed } from 'vue'

export interface UseLoadingOptions {
  /** 初始加载状态 */
  initialLoading?: boolean
  /** 最小加载时间（毫秒），防止闪烁 */
  minLoadingTime?: number
}

export function useLoading(options: UseLoadingOptions = {}) {
  const { initialLoading = false, minLoadingTime = 0 } = options
  
  const loadingCount = ref(initialLoading ? 1 : 0)
  const loadingStartTime = ref<number | null>(null)
  
  const isLoading = computed(() => loadingCount.value > 0)
  
  /**
   * 开始加载
   */
  const startLoading = () => {
    if (loadingCount.value === 0) {
      loadingStartTime.value = Date.now()
    }
    loadingCount.value++
  }
  
  /**
   * 结束加载
   */
  const stopLoading = async () => {
    if (loadingCount.value <= 0) return
    
    // 确保最小加载时间
    if (minLoadingTime > 0 && loadingStartTime.value) {
      const elapsed = Date.now() - loadingStartTime.value
      if (elapsed < minLoadingTime) {
        await new Promise(resolve => setTimeout(resolve, minLoadingTime - elapsed))
      }
    }
    
    loadingCount.value--
    if (loadingCount.value === 0) {
      loadingStartTime.value = null
    }
  }
  
  /**
   * 重置加载状态
   */
  const resetLoading = () => {
    loadingCount.value = 0
    loadingStartTime.value = null
  }
  
  /**
   * 包装异步函数，自动管理加载状态
   */
  const withLoading = async <T>(fn: () => Promise<T>): Promise<T> => {
    startLoading()
    try {
      return await fn()
    } finally {
      await stopLoading()
    }
  }
  
  return {
    isLoading,
    loadingCount,
    startLoading,
    stopLoading,
    resetLoading,
    withLoading
  }
}

/**
 * 多个加载状态管理
 */
export function useMultiLoading<T extends string>(keys: T[]) {
  const loadingMap = ref<Record<string, boolean>>(
    Object.fromEntries(keys.map(key => [key, false]))
  )
  
  const isLoading = (key: T) => loadingMap.value[key] ?? false
  
  const isAnyLoading = computed(() => 
    Object.values(loadingMap.value).some(v => v)
  )
  
  const startLoading = (key: T) => {
    loadingMap.value[key] = true
  }
  
  const stopLoading = (key: T) => {
    loadingMap.value[key] = false
  }
  
  const withLoading = async <R>(key: T, fn: () => Promise<R>): Promise<R> => {
    startLoading(key)
    try {
      return await fn()
    } finally {
      stopLoading(key)
    }
  }
  
  return {
    loadingMap,
    isLoading,
    isAnyLoading,
    startLoading,
    stopLoading,
    withLoading
  }
}

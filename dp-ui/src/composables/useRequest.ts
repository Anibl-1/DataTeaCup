/**
 * 请求 Hook
 * 封装常用的请求逻辑
 */
import { ref, Ref } from 'vue'
import { useAppStore } from '@/stores/app'
import { logger } from '@/utils/logger'

interface UseRequestOptions<T> {
  /** 是否立即执行 */
  immediate?: boolean
  /** 是否显示全局加载 */
  showLoading?: boolean
  /** 加载文本 */
  loadingText?: string
  /** 成功回调 */
  onSuccess?: (data: T) => void
  /** 失败回调 */
  onError?: (error: Error) => void
  /** 默认值 */
  defaultValue?: T
}

interface UseRequestReturn<T, P extends unknown[]> {
  data: Ref<T | undefined>
  loading: Ref<boolean>
  error: Ref<Error | null>
  execute: (...args: P) => Promise<T | undefined>
  refresh: () => Promise<T | undefined>
}

/**
 * 通用请求 Hook
 */
export function useRequest<T, P extends unknown[] = []>(
  requestFn: (...args: P) => Promise<T>,
  options: UseRequestOptions<T> = {}
): UseRequestReturn<T, P> {
  const {
    immediate = false,
    showLoading = false,
    loadingText = '加载中...',
    onSuccess,
    onError,
    defaultValue
  } = options

  const appStore = useAppStore()
  const data = ref<T | undefined>(defaultValue) as Ref<T | undefined>
  const loading = ref(false)
  const error = ref<Error | null>(null)
  let lastArgs: P | null = null

  const execute = async (...args: P): Promise<T | undefined> => {
    lastArgs = args
    loading.value = true
    error.value = null

    if (showLoading) {
      appStore.startLoading(loadingText)
    }

    try {
      logger.time('request')
      const result = await requestFn(...args)
      logger.timeEnd('request')
      
      data.value = result
      onSuccess?.(result)
      return result
    } catch (e) {
      const err = e instanceof Error ? e : new Error(String(e))
      error.value = err
      logger.error('请求失败', err)
      onError?.(err)
      return undefined
    } finally {
      loading.value = false
      if (showLoading) {
        appStore.stopLoading()
      }
    }
  }

  const refresh = async (): Promise<T | undefined> => {
    if (lastArgs) {
      return execute(...lastArgs)
    }
    return execute(...([] as unknown as P))
  }

  // 立即执行
  if (immediate) {
    execute(...([] as unknown as P))
  }

  return {
    data,
    loading,
    error,
    execute,
    refresh
  }
}

/**
 * 分页请求 Hook
 */
export function usePagination<T, P extends unknown[] = []>(
  requestFn: (page: number, pageSize: number, ...args: P) => Promise<{ list: T[]; total: number }>,
  options: UseRequestOptions<T[]> & { defaultPageSize?: number } = {}
) {
  const { defaultPageSize = 10, ...restOptions } = options

  const list = ref<T[]>([]) as Ref<T[]>
  const total = ref(0)
  const page = ref(1)
  const pageSize = ref(defaultPageSize)
  const loading = ref(false)

  const appStore = useAppStore()

  const fetch = async (...args: P) => {
    loading.value = true
    if (restOptions.showLoading) {
      appStore.startLoading(restOptions.loadingText)
    }

    try {
      const result = await requestFn(page.value, pageSize.value, ...args)
      list.value = result.list
      total.value = result.total
      restOptions.onSuccess?.(result.list)
    } catch (e) {
      const err = e instanceof Error ? e : new Error(String(e))
      logger.error('分页请求失败', err)
      restOptions.onError?.(err)
    } finally {
      loading.value = false
      if (restOptions.showLoading) {
        appStore.stopLoading()
      }
    }
  }

  const changePage = (newPage: number) => {
    page.value = newPage
  }

  const changePageSize = (newSize: number) => {
    pageSize.value = newSize
    page.value = 1
  }

  const refresh = () => fetch(...([] as unknown as P))

  return {
    list,
    total,
    page,
    pageSize,
    loading,
    fetch,
    changePage,
    changePageSize,
    refresh
  }
}

/**
 * 防抖请求 Hook
 */
export function useDebouncedRequest<T, P extends unknown[] = []>(
  requestFn: (...args: P) => Promise<T>,
  delay = 300,
  options: UseRequestOptions<T> = {}
) {
  const request = useRequest(requestFn, options)
  let timer: ReturnType<typeof setTimeout> | null = null

  const debouncedExecute = (...args: P) => {
    if (timer) {
      clearTimeout(timer)
    }
    
    return new Promise<T | undefined>((resolve) => {
      timer = setTimeout(async () => {
        const result = await request.execute(...args)
        resolve(result)
      }, delay)
    })
  }

  return {
    ...request,
    execute: debouncedExecute
  }
}

/**
 * 异步组件加载工具
 * 提供统一的异步组件加载、错误处理和加载状态管理
 * 
 * 需求: 1.9, 1.10 - 路由懒加载和组件按需加载
 */
import { defineAsyncComponent, defineComponent, h, type Component, type AsyncComponentLoader, type PropType } from 'vue'
import { SkeletonLoader } from '@/components/animations'

/**
 * 异步组件配置选项
 */
export interface AsyncComponentOptions {
  /** 加载超时时间（毫秒），默认 10000ms */
  timeout?: number
  /** 延迟显示加载组件的时间（毫秒），默认 200ms */
  delay?: number
  /** 骨架屏类型 */
  loadingType?: 'table' | 'card' | 'chart' | 'form'
  /** 自定义加载组件 */
  loadingComponent?: Component
  /** 自定义错误组件 */
  errorComponent?: Component
  /** 是否在加载失败时重试 */
  retryOnError?: boolean
  /** 最大重试次数 */
  maxRetries?: number
}

/**
 * 默认错误组件
 */
const DefaultErrorComponent = defineComponent({
  name: 'AsyncComponentError',
  props: {
    error: {
      type: Object as PropType<Error | null>,
      default: null
    },
    retry: {
      type: Function as unknown as PropType<(() => void) | null>,
      default: null
    }
  },
  setup(props) {
    return () => h('div', {
      style: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '40px',
        color: '#666',
        backgroundColor: '#fafafa',
        borderRadius: '8px',
        minHeight: '200px'
      }
    }, [
      h('div', {
        style: {
          fontSize: '48px',
          marginBottom: '16px'
        }
      }, '⚠️'),
      h('div', {
        style: {
          fontSize: '16px',
          marginBottom: '8px',
          fontWeight: '500'
        }
      }, '组件加载失败'),
      h('div', {
        style: {
          fontSize: '14px',
          color: '#999',
          marginBottom: '16px'
        }
      }, props.error?.message || '请检查网络连接后重试'),
      props.retry ? h('button', {
        onClick: props.retry,
        style: {
          padding: '8px 24px',
          fontSize: '14px',
          color: '#fff',
          backgroundColor: '#1890ff',
          border: 'none',
          borderRadius: '4px',
          cursor: 'pointer'
        }
      }, '重新加载') : null
    ])
  }
})

/**
 * 创建带有加载状态的异步组件
 * 
 * @param loader - 组件加载器函数
 * @param options - 配置选项
 * @returns 异步组件
 * 
 * @example
 * ```ts
 * // 基本用法
 * const MyComponent = createAsyncComponent(() => import('./MyComponent.vue'))
 * 
 * // 带配置
 * const MyComponent = createAsyncComponent(
 *   () => import('./MyComponent.vue'),
 *   { loadingType: 'table', timeout: 5000 }
 * )
 * ```
 */
export function createAsyncComponent(
  loader: AsyncComponentLoader,
  options: AsyncComponentOptions = {}
): Component {
  const {
    timeout = 10000,
    delay = 200,
    loadingType = 'card',
    loadingComponent,
    errorComponent,
    retryOnError = true,
    maxRetries = 3
  } = options

  let retryCount = 0

  // 创建加载组件
  const LoadingComponent = loadingComponent || {
    name: 'AsyncComponentLoading',
    setup() {
      return () => h(SkeletonLoader, {
        type: loadingType,
        rows: loadingType === 'table' ? 5 : 3,
        animated: true
      })
    }
  }

  // 创建错误组件
  const ErrorComponent = errorComponent || DefaultErrorComponent

  // 包装加载器以支持重试
  const wrappedLoader: AsyncComponentLoader = () => {
    return loader().catch((error: Error) => {
      if (retryOnError && retryCount < maxRetries) {
        retryCount++
        console.warn(`组件加载失败，正在重试 (${retryCount}/${maxRetries})...`)
        return new Promise((resolve) => {
          setTimeout(() => {
            resolve(wrappedLoader())
          }, 1000 * retryCount) // 递增延迟
        })
      }
      throw error
    })
  }

  return defineAsyncComponent({
    loader: wrappedLoader,
    loadingComponent: LoadingComponent,
    errorComponent: ErrorComponent,
    delay,
    timeout,
    onError(error, retry, fail, attempts) {
      if (retryOnError && attempts <= maxRetries) {
        console.warn(`组件加载错误，尝试重试 (${attempts}/${maxRetries})`)
        retry()
      } else {
        console.error('组件加载失败:', error)
        fail()
      }
    }
  })
}

/**
 * 创建路由级别的异步组件
 * 针对路由组件优化，使用更长的超时时间和表格类型的骨架屏
 * 
 * @param loader - 组件加载器函数
 * @returns 异步组件
 */
export function createRouteComponent(loader: AsyncComponentLoader): Component {
  return createAsyncComponent(loader, {
    timeout: 15000,
    delay: 100,
    loadingType: 'card',
    retryOnError: true,
    maxRetries: 2
  })
}

/**
 * 预加载组件
 * 用于在空闲时间预加载可能需要的组件
 * 
 * @param loader - 组件加载器函数
 */
export function preloadComponent(loader: AsyncComponentLoader): void {
  if (typeof requestIdleCallback !== 'undefined') {
    requestIdleCallback(() => {
      loader().catch(() => {
        // 预加载失败不需要处理
      })
    })
  } else {
    setTimeout(() => {
      loader().catch(() => {
        // 预加载失败不需要处理
      })
    }, 0)
  }
}

/**
 * 批量预加载组件
 * 
 * @param loaders - 组件加载器函数数组
 */
export function preloadComponents(loaders: AsyncComponentLoader[]): void {
  loaders.forEach(preloadComponent)
}

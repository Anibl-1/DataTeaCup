/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 图片懒加载指令
 * 使用 IntersectionObserver 实现
 * 
 * 用法：
 * <img v-lazy="imageUrl" />
 * <img v-lazy="{ src: imageUrl, loading: loadingUrl, error: errorUrl }" />
 */
import type { Directive, DirectiveBinding } from 'vue'

interface LazyOptions {
  src: string
  loading?: string
  error?: string
}

// 默认占位图
const DEFAULT_LOADING = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjBmMGYwIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxMiIgZmlsbD0iIzk5OSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPuWKoOi9veS4rS4uLjwvdGV4dD48L3N2Zz4='
export const DEFAULT_ERROR = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjVmNWY1Ii8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxMiIgZmlsbD0iI2NjYyIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPuWKoOi9veWksei0pTwvdGV4dD48L3N2Zz4='

// 观察器实例
let observer: IntersectionObserver | null = null

// 获取观察器
function getObserver(): IntersectionObserver {
  if (observer) return observer
  
  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          const el = entry.target as HTMLImageElement
          const options = (el as any)._lazyOptions as LazyOptions
          
          if (options?.src) {
            loadImage(el, options)
          }
          
          observer?.unobserve(el)
        }
      })
    },
    {
      rootMargin: '50px 0px', // 提前 50px 开始加载
      threshold: 0.01
    }
  )
  
  return observer
}

// 加载图片
function loadImage(el: HTMLImageElement, options: LazyOptions) {
  const img = new Image()
  
  img.onload = () => {
    el.src = options.src
    el.classList.remove('lazy-loading')
    el.classList.add('lazy-loaded')
  }
  
  img.onerror = () => {
    el.src = options.error || DEFAULT_ERROR
    el.classList.remove('lazy-loading')
    el.classList.add('lazy-error')
  }
  
  img.src = options.src
}

// 解析选项
function parseOptions(binding: DirectiveBinding): LazyOptions {
  const value = binding.value
  
  if (typeof value === 'string') {
    return { src: value }
  }
  
  return {
    src: value.src,
    loading: value.loading,
    error: value.error
  }
}

export const vLazy: Directive<HTMLImageElement> = {
  mounted(el, binding) {
    const options = parseOptions(binding)
    ;(el as any)._lazyOptions = options
    
    // 设置占位图
    el.src = options.loading || DEFAULT_LOADING
    el.classList.add('lazy-loading')
    
    // 添加 onerror 回调，确保图片加载失败时显示统一占位图
    el.onerror = () => {
      const errorSrc = (el as any)._lazyOptions?.error || DEFAULT_ERROR
      // 避免占位图自身加载失败导致无限循环
      if (el.src !== errorSrc) {
        el.src = errorSrc
        el.classList.remove('lazy-loading')
        el.classList.add('lazy-error')
      }
    }
    
    // 开始观察
    getObserver().observe(el)
  },
  
  updated(el, binding) {
    const options = parseOptions(binding)
    const oldOptions = (el as any)._lazyOptions as LazyOptions
    
    // 如果 src 变化，重新加载
    if (options.src !== oldOptions?.src) {
      ;(el as any)._lazyOptions = options
      el.src = options.loading || DEFAULT_LOADING
      el.classList.remove('lazy-loaded', 'lazy-error')
      el.classList.add('lazy-loading')
      
      // 更新 onerror 回调以使用最新的 error 占位图
      el.onerror = () => {
        const errorSrc = (el as any)._lazyOptions?.error || DEFAULT_ERROR
        if (el.src !== errorSrc) {
          el.src = errorSrc
          el.classList.remove('lazy-loading')
          el.classList.add('lazy-error')
        }
      }
      
      getObserver().observe(el)
    }
  },
  
  unmounted(el) {
    observer?.unobserve(el)
    delete (el as any)._lazyOptions
  }
}

// 注册全局指令
export function setupLazyLoad(app: any) {
  app.directive('lazy', vLazy)
}

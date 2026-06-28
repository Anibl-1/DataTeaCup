/**
 * 可视区域组件懒加载指令
 * 使用 IntersectionObserver 实现仪表盘中仅加载可视区域内的组件
 *
 * 用法：
 * <div v-lazy-component="{ loaded: isLoaded, onVisible: () => { isLoaded = true } }">
 *   <template v-if="isLoaded"><HeavyComponent /></template>
 *   <SkeletonLoader v-else type="card" />
 * </div>
 *
 * 或简写（自动管理加载状态）：
 * <div v-lazy-component="onVisibleCallback">
 *   ...
 * </div>
 *
 * 需求: 15.2 - THE DataTeaCup SHALL 仅加载仪表盘中可视区域内的组件
 */
import type { Directive, DirectiveBinding } from 'vue'

export interface LazyComponentOptions {
  /** 组件进入可视区域时的回调 */
  onVisible: () => void
  /** 预加载距离（像素），提前加载即将进入可视区域的组件 */
  rootMargin?: string
  /** 可见比例阈值，0-1 之间 */
  threshold?: number
  /** 是否只触发一次（默认 true，组件加载后不再监听） */
  once?: boolean
}

type BindingValue = LazyComponentOptions | (() => void)

/** 存储每个元素对应的 observer，用于清理 */
const observerMap = new WeakMap<HTMLElement, IntersectionObserver>()

/**
 * 解析指令绑定值
 */
function parseBinding(binding: DirectiveBinding<BindingValue>): LazyComponentOptions {
  const value = binding.value

  if (typeof value === 'function') {
    return {
      onVisible: value,
      rootMargin: '200px 0px',
      threshold: 0,
      once: true,
    }
  }

  return {
    onVisible: value.onVisible,
    rootMargin: value.rootMargin ?? '200px 0px',
    threshold: value.threshold ?? 0,
    once: value.once ?? true,
  }
}

/**
 * 为元素创建并启动 IntersectionObserver
 */
function observe(el: HTMLElement, options: LazyComponentOptions): void {
  // 清理旧的 observer
  cleanup(el)

  const observer = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          options.onVisible()

          if (options.once) {
            observer.unobserve(el)
            observerMap.delete(el)
          }
        }
      }
    },
    {
      rootMargin: options.rootMargin,
      threshold: options.threshold,
    }
  )

  observer.observe(el)
  observerMap.set(el, observer)
}

/**
 * 清理元素的 observer
 */
function cleanup(el: HTMLElement): void {
  const existing = observerMap.get(el)
  if (existing) {
    existing.unobserve(el)
    existing.disconnect()
    observerMap.delete(el)
  }
}

/**
 * v-lazy-component 指令
 */
export const vLazyComponent: Directive<HTMLElement, BindingValue> = {
  mounted(el, binding) {
    const options = parseBinding(binding)
    observe(el, options)
  },

  updated(el, binding) {
    // 仅在绑定值变化时重新观察
    if (binding.value !== binding.oldValue) {
      const options = parseBinding(binding)
      observe(el, options)
    }
  },

  unmounted(el) {
    cleanup(el)
  },
}

/**
 * 注册 v-lazy-component 指令
 */
export function setupLazyComponent(app: { directive: (name: string, dir: Directive) => void }): void {
  app.directive('lazy-component', vLazyComponent)
}

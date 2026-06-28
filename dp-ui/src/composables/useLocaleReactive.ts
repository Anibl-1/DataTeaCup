/**
 * 响应式语言切换 Composable
 * 提供便捷的方式让组件响应语言变化
 * 
 * **Validates: Requirements 24.4**
 * THE I18n_Manager SHALL 支持动态切换语言而无需刷新页面
 * - 组件可以使用此 composable 来响应语言变化
 * - 支持在语言变化时执行回调
 * - 支持自动清理事件监听器
 */
import { ref, onMounted, onUnmounted, watch, type Ref } from 'vue'
import { 
  currentLocale, 
  onLocaleChange, 
  type Locale,
} from '@/i18n'

/**
 * 语言变化回调函数类型
 */
export type LocaleChangeCallback = (locale: Locale, previousLocale: Locale) => void

/**
 * useLocaleReactive 返回值类型
 */
export interface UseLocaleReactiveReturn {
  /** 当前语言（响应式） */
  locale: Ref<Locale>
  /** 是否为中文 */
  isZhCN: Ref<boolean>
  /** 是否为英文 */
  isEnUS: Ref<boolean>
  /** 语言变化次数（用于触发重新渲染） */
  localeChangeCount: Ref<number>
}

/**
 * 响应式语言切换 Composable
 * 
 * @param onChangeCallback - 可选的语言变化回调函数
 * @returns 响应式语言状态
 * 
 * @example
 * ```ts
 * import { useLocaleReactive } from '@/composables/useLocaleReactive'
 * 
 * // 基本用法
 * const { locale, isZhCN, isEnUS } = useLocaleReactive()
 * 
 * // 带回调
 * const { locale } = useLocaleReactive((newLocale, oldLocale) => {
 *   console.log(`Language changed from ${oldLocale} to ${newLocale}`)
 *   // 重新加载数据等操作
 * })
 * 
 * // 在模板中使用
 * // <div v-if="isZhCN">中文内容</div>
 * // <div v-else>English content</div>
 * ```
 */
export function useLocaleReactive(onChangeCallback?: LocaleChangeCallback): UseLocaleReactiveReturn {
  // 语言变化计数器，用于触发依赖此值的组件重新渲染
  const localeChangeCount = ref(0)
  
  // 派生的响应式状态
  const isZhCN = ref(currentLocale.value === 'zh-CN')
  const isEnUS = ref(currentLocale.value === 'en-US')
  
  // 清理函数
  let cleanup: (() => void) | null = null
  
  onMounted(() => {
    // 监听语言变化事件
    cleanup = onLocaleChange((newLocale, previousLocale) => {
      // 更新派生状态
      isZhCN.value = newLocale === 'zh-CN'
      isEnUS.value = newLocale === 'en-US'
      
      // 增加变化计数
      localeChangeCount.value++
      
      // 执行回调
      if (onChangeCallback) {
        onChangeCallback(newLocale, previousLocale)
      }
    })
  })
  
  onUnmounted(() => {
    // 清理事件监听器
    if (cleanup) {
      cleanup()
      cleanup = null
    }
  })
  
  // 同时监听 currentLocale 的变化（用于初始化和直接修改的情况）
  watch(currentLocale, (newLocale) => {
    isZhCN.value = newLocale === 'zh-CN'
    isEnUS.value = newLocale === 'en-US'
  }, { immediate: true })
  
  return {
    locale: currentLocale,
    isZhCN,
    isEnUS,
    localeChangeCount
  }
}

/**
 * 创建一个在语言变化时自动更新的计算值
 * 
 * @param getter - 获取值的函数，接收当前语言作为参数
 * @returns 响应式的计算值
 * 
 * @example
 * ```ts
 * import { useLocaleComputed } from '@/composables/useLocaleReactive'
 * 
 * // 根据语言返回不同的值
 * const greeting = useLocaleComputed((locale) => {
 *   return locale === 'zh-CN' ? '你好' : 'Hello'
 * })
 * ```
 */
export function useLocaleComputed<T>(getter: (locale: Locale) => T): Ref<T> {
  const result = ref<T>(getter(currentLocale.value)) as Ref<T>
  
  let cleanup: (() => void) | null = null
  
  onMounted(() => {
    cleanup = onLocaleChange((newLocale) => {
      result.value = getter(newLocale)
    })
  })
  
  onUnmounted(() => {
    if (cleanup) {
      cleanup()
      cleanup = null
    }
  })
  
  // 监听 currentLocale 变化
  watch(currentLocale, (newLocale) => {
    result.value = getter(newLocale)
  }, { immediate: true })
  
  return result
}

export default useLocaleReactive

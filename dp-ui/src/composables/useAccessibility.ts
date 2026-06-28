/**
 * 无障碍支持 Composable
 * Accessibility Support Composable
 * 
 * 提供完整的无障碍支持功能：
 * - 焦点管理 (Focus Trap, Focus Restore)
 * - 键盘导航 (Arrow Keys, Tab, Enter, Escape)
 * - ARIA 属性辅助
 * - 屏幕阅读器公告
 * - 减少动画检测
 * 
 * Validates: Requirements 1.6, 1.7
 * - 1.6: 支持完整的键盘导航，包括 Tab 键切换焦点和 Enter 键激活操作
 * - 1.7: 为所有交互元素提供符合 WCAG 2.1 标准的 ARIA 标签
 */

import { ref, computed, onMounted, onUnmounted, type Ref, type ComputedRef } from 'vue'
import { themeConfig } from '@/config/theme'

// ============================================================================
// Types
// ============================================================================

/**
 * 焦点陷阱选项
 */
export interface FocusTrapOptions {
  /** 是否自动聚焦第一个可聚焦元素 */
  autoFocus?: boolean
  /** 是否在 Escape 键时释放焦点陷阱 */
  escapeDeactivates?: boolean
  /** 是否允许点击外部区域释放焦点陷阱 */
  clickOutsideDeactivates?: boolean
  /** 初始聚焦元素选择器 */
  initialFocus?: string
  /** 返回焦点元素选择器 */
  returnFocusOnDeactivate?: boolean
}

/**
 * 焦点陷阱返回类型
 */
export interface UseFocusTrapReturn {
  /** 容器引用 */
  containerRef: Ref<HTMLElement | null>
  /** 是否激活 */
  isActive: Ref<boolean>
  /** 激活焦点陷阱 */
  activate: () => void
  /** 停用焦点陷阱 */
  deactivate: () => void
  /** 暂停焦点陷阱 */
  pause: () => void
  /** 恢复焦点陷阱 */
  unpause: () => void
}


/**
 * 键盘导航选项
 */
export interface KeyboardNavigationOptions {
  /** 导航方向 */
  orientation?: 'horizontal' | 'vertical' | 'both'
  /** 是否循环导航 */
  loop?: boolean
  /** 可聚焦元素选择器 */
  itemSelector?: string
  /** 是否阻止默认行为 */
  preventDefault?: boolean
  /** 自定义按键处理 */
  onKeyDown?: (event: KeyboardEvent, currentIndex: number) => void
  /** Enter 键处理 */
  onEnter?: (event: KeyboardEvent, currentIndex: number) => void
  /** Escape 键处理 */
  onEscape?: (event: KeyboardEvent) => void
  /** Home 键处理 */
  onHome?: (event: KeyboardEvent) => void
  /** End 键处理 */
  onEnd?: (event: KeyboardEvent) => void
}

/**
 * 键盘导航返回类型
 */
export interface UseKeyboardNavigationReturn {
  /** 容器引用 */
  containerRef: Ref<HTMLElement | null>
  /** 当前聚焦索引 */
  currentIndex: Ref<number>
  /** 聚焦指定索引 */
  focusIndex: (index: number) => void
  /** 聚焦下一个 */
  focusNext: () => void
  /** 聚焦上一个 */
  focusPrevious: () => void
  /** 聚焦第一个 */
  focusFirst: () => void
  /** 聚焦最后一个 */
  focusLast: () => void
  /** 键盘事件处理器 */
  handleKeyDown: (event: KeyboardEvent) => void
}

/**
 * 屏幕阅读器公告选项
 */
export interface AnnounceOptions {
  /** 公告优先级 */
  priority?: 'polite' | 'assertive'
  /** 公告延迟（毫秒） */
  delay?: number
  /** 清除之前的公告 */
  clearPrevious?: boolean
}

/**
 * 屏幕阅读器公告返回类型
 */
export interface UseAnnounceReturn {
  /** 发送公告 */
  announce: (message: string, options?: AnnounceOptions) => void
  /** 清除公告 */
  clear: () => void
  /** 当前公告消息 */
  currentMessage: Ref<string>
}

/**
 * 减少动画返回类型
 */
export interface UseReducedMotionReturn {
  /** 是否偏好减少动画 */
  prefersReducedMotion: ComputedRef<boolean>
  /** 是否应该使用动画 */
  shouldAnimate: ComputedRef<boolean>
}

// ============================================================================
// Constants
// ============================================================================

/** 可聚焦元素选择器 */
const FOCUSABLE_SELECTOR = [
  'a[href]',
  'area[href]',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  'button:not([disabled])',
  'iframe',
  'object',
  'embed',
  '[contenteditable]',
  '[tabindex]:not([tabindex="-1"])'
].join(',')

/** ARIA ID 计数器 */
let ariaIdCounter = 0

// ============================================================================
// Utility Functions
// ============================================================================

/**
 * 生成唯一的 ARIA ID
 * @param prefix - ID 前缀
 * @returns 唯一 ID
 */
export function generateAriaId(prefix: string = 'aria'): string {
  ariaIdCounter += 1
  return `${prefix}-${ariaIdCounter}-${Date.now().toString(36)}`
}

/**
 * 重置 ARIA ID 计数器（仅用于测试）
 */
export function resetAriaIdCounter(): void {
  ariaIdCounter = 0
}

/**
 * 获取容器内所有可聚焦元素
 * @param container - 容器元素
 * @param selector - 自定义选择器
 * @returns 可聚焦元素数组
 */
export function getFocusableElements(
  container: HTMLElement,
  selector: string = FOCUSABLE_SELECTOR
): HTMLElement[] {
  const elements = Array.from(container.querySelectorAll<HTMLElement>(selector))
  return elements.filter(el => {
    // 过滤不可见元素
    if (el.offsetParent === null && el.style.position !== 'fixed') return false
    // 过滤 tabindex="-1" 的元素（除非明确指定）
    const tabindex = el.getAttribute('tabindex')
    if (tabindex === '-1' && !selector.includes('[tabindex="-1"]')) return false
    return true
  })
}

/**
 * 检查元素是否可聚焦
 * @param element - 要检查的元素
 * @returns 是否可聚焦
 */
export function isFocusable(element: HTMLElement): boolean {
  return element.matches(FOCUSABLE_SELECTOR) && element.offsetParent !== null
}


// ============================================================================
// useFocusTrap Composable
// ============================================================================

/**
 * 焦点陷阱 Composable
 * 将焦点限制在指定容器内，用于模态框、下拉菜单等场景
 * 
 * @param options - 焦点陷阱选项
 * @returns 焦点陷阱控制对象
 * 
 * @example
 * ```ts
 * const { containerRef, activate, deactivate } = useFocusTrap({
 *   autoFocus: true,
 *   escapeDeactivates: true
 * })
 * 
 * // 在模板中
 * <div ref="containerRef">
 *   <button>Button 1</button>
 *   <button>Button 2</button>
 * </div>
 * ```
 */
export function useFocusTrap(options: FocusTrapOptions = {}): UseFocusTrapReturn {
  const {
    autoFocus = true,
    escapeDeactivates = true,
    clickOutsideDeactivates = false,
    initialFocus,
    returnFocusOnDeactivate = true
  } = options

  const containerRef = ref<HTMLElement | null>(null)
  const isActive = ref(false)
  const isPaused = ref(false)
  const previousActiveElement = ref<HTMLElement | null>(null)

  /**
   * 处理 Tab 键导航
   */
  function handleTabKey(event: KeyboardEvent): void {
    if (!containerRef.value || !isActive.value || isPaused.value) return

    const focusableElements = getFocusableElements(containerRef.value)
    if (focusableElements.length === 0) return

    const firstElement = focusableElements[0]
    const lastElement = focusableElements[focusableElements.length - 1]
    const activeElement = document.activeElement as HTMLElement

    if (event.shiftKey) {
      // Shift + Tab: 向后导航
      if (activeElement === firstElement || !containerRef.value.contains(activeElement)) {
        event.preventDefault()
        lastElement?.focus()
      }
    } else {
      // Tab: 向前导航
      if (activeElement === lastElement || !containerRef.value.contains(activeElement)) {
        event.preventDefault()
        firstElement?.focus()
      }
    }
  }

  /**
   * 处理 Escape 键
   */
  function handleEscapeKey(event: KeyboardEvent): void {
    if (!isActive.value || isPaused.value || !escapeDeactivates) return
    event.preventDefault()
    deactivate()
  }

  /**
   * 键盘事件处理器
   */
  function handleKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Tab') {
      handleTabKey(event)
    } else if (event.key === 'Escape') {
      handleEscapeKey(event)
    }
  }

  /**
   * 点击外部处理器
   */
  function handleClickOutside(event: MouseEvent): void {
    if (!isActive.value || isPaused.value || !clickOutsideDeactivates) return
    if (!containerRef.value) return
    
    const target = event.target as Node
    if (!containerRef.value.contains(target)) {
      deactivate()
    }
  }

  /**
   * 激活焦点陷阱
   */
  function activate(): void {
    if (isActive.value || !containerRef.value) return

    // 保存当前焦点元素
    previousActiveElement.value = document.activeElement as HTMLElement
    isActive.value = true

    // 添加事件监听
    document.addEventListener('keydown', handleKeyDown)
    if (clickOutsideDeactivates) {
      document.addEventListener('click', handleClickOutside)
    }

    // 自动聚焦
    if (autoFocus) {
      const focusTarget = initialFocus
        ? containerRef.value.querySelector<HTMLElement>(initialFocus)
        : getFocusableElements(containerRef.value)[0]
      
      if (focusTarget) {
        // 使用 requestAnimationFrame 确保 DOM 已更新
        requestAnimationFrame(() => {
          focusTarget.focus()
        })
      }
    }
  }

  /**
   * 停用焦点陷阱
   */
  function deactivate(): void {
    if (!isActive.value) return

    isActive.value = false
    isPaused.value = false

    // 移除事件监听
    document.removeEventListener('keydown', handleKeyDown)
    document.removeEventListener('click', handleClickOutside)

    // 恢复焦点
    if (returnFocusOnDeactivate && previousActiveElement.value) {
      previousActiveElement.value.focus()
      previousActiveElement.value = null
    }
  }

  /**
   * 暂停焦点陷阱
   */
  function pause(): void {
    isPaused.value = true
  }

  /**
   * 恢复焦点陷阱
   */
  function unpause(): void {
    isPaused.value = false
  }

  // 清理
  onUnmounted(() => {
    if (isActive.value) {
      deactivate()
    }
  })

  return {
    containerRef,
    isActive,
    activate,
    deactivate,
    pause,
    unpause
  }
}


// ============================================================================
// useKeyboardNavigation Composable
// ============================================================================

/**
 * 键盘导航 Composable
 * 实现方向键导航、Tab 键切换焦点、Enter 键激活操作
 * 
 * @param options - 键盘导航选项
 * @returns 键盘导航控制对象
 * 
 * @example
 * ```ts
 * const { containerRef, handleKeyDown, currentIndex } = useKeyboardNavigation({
 *   orientation: 'vertical',
 *   loop: true,
 *   onEnter: (event, index) => {
 *     console.log('Activated item:', index)
 *   }
 * })
 * 
 * // 在模板中
 * <ul ref="containerRef" @keydown="handleKeyDown" role="listbox">
 *   <li role="option" tabindex="0">Item 1</li>
 *   <li role="option" tabindex="-1">Item 2</li>
 * </ul>
 * ```
 */
export function useKeyboardNavigation(
  options: KeyboardNavigationOptions = {}
): UseKeyboardNavigationReturn {
  const {
    orientation = 'vertical',
    loop = true,
    itemSelector = '[role="option"], [role="menuitem"], [role="tab"], [tabindex]',
    preventDefault = true,
    onKeyDown,
    onEnter,
    onEscape,
    onHome,
    onEnd
  } = options

  const containerRef = ref<HTMLElement | null>(null)
  const currentIndex = ref(0)

  /**
   * 获取导航项
   */
  function getItems(): HTMLElement[] {
    if (!containerRef.value) return []
    return Array.from(containerRef.value.querySelectorAll<HTMLElement>(itemSelector))
      .filter(el => !el.hasAttribute('disabled') && el.getAttribute('aria-disabled') !== 'true')
  }

  /**
   * 聚焦指定索引的元素
   */
  function focusIndex(index: number): void {
    const items = getItems()
    if (items.length === 0) return

    // 边界处理
    let targetIndex = index
    if (loop) {
      targetIndex = ((index % items.length) + items.length) % items.length
    } else {
      targetIndex = Math.max(0, Math.min(index, items.length - 1))
    }

    currentIndex.value = targetIndex
    
    // 更新 tabindex
    items.forEach((item, i) => {
      item.setAttribute('tabindex', i === targetIndex ? '0' : '-1')
    })
    
    items[targetIndex]?.focus()
  }

  /**
   * 聚焦下一个元素
   */
  function focusNext(): void {
    focusIndex(currentIndex.value + 1)
  }

  /**
   * 聚焦上一个元素
   */
  function focusPrevious(): void {
    focusIndex(currentIndex.value - 1)
  }

  /**
   * 聚焦第一个元素
   */
  function focusFirst(): void {
    focusIndex(0)
  }

  /**
   * 聚焦最后一个元素
   */
  function focusLast(): void {
    const items = getItems()
    focusIndex(items.length - 1)
  }

  /**
   * 键盘事件处理器
   */
  function handleKeyDown(event: KeyboardEvent): void {
    const { key } = event
    
    // 自定义处理
    if (onKeyDown) {
      onKeyDown(event, currentIndex.value)
    }

    // 方向键导航
    const isVertical = orientation === 'vertical' || orientation === 'both'
    const isHorizontal = orientation === 'horizontal' || orientation === 'both'

    switch (key) {
      case 'ArrowDown':
        if (isVertical) {
          if (preventDefault) event.preventDefault()
          focusNext()
        }
        break
      case 'ArrowUp':
        if (isVertical) {
          if (preventDefault) event.preventDefault()
          focusPrevious()
        }
        break
      case 'ArrowRight':
        if (isHorizontal) {
          if (preventDefault) event.preventDefault()
          focusNext()
        }
        break
      case 'ArrowLeft':
        if (isHorizontal) {
          if (preventDefault) event.preventDefault()
          focusPrevious()
        }
        break
      case 'Home':
        if (preventDefault) event.preventDefault()
        if (onHome) {
          onHome(event)
        } else {
          focusFirst()
        }
        break
      case 'End':
        if (preventDefault) event.preventDefault()
        if (onEnd) {
          onEnd(event)
        } else {
          focusLast()
        }
        break
      case 'Enter':
      case ' ':
        if (key === ' ') {
          if (preventDefault) event.preventDefault()
        }
        if (onEnter) {
          onEnter(event, currentIndex.value)
        }
        break
      case 'Escape':
        if (onEscape) {
          if (preventDefault) event.preventDefault()
          onEscape(event)
        }
        break
    }
  }

  return {
    containerRef,
    currentIndex,
    focusIndex,
    focusNext,
    focusPrevious,
    focusFirst,
    focusLast,
    handleKeyDown
  }
}


// ============================================================================
// useAnnounce Composable
// ============================================================================

/**
 * 创建或获取公告区域
 */
function getAnnounceRegion(priority: 'polite' | 'assertive'): HTMLElement {
  const regionId = `aria-live-region-${priority}`
  let region = document.getElementById(regionId)
  
  if (!region) {
    region = document.createElement('div')
    region.id = regionId
    region.setAttribute('role', 'status')
    region.setAttribute('aria-live', priority)
    region.setAttribute('aria-atomic', 'true')
    // 视觉隐藏但对屏幕阅读器可见
    Object.assign(region.style, {
      position: 'absolute',
      width: '1px',
      height: '1px',
      padding: '0',
      margin: '-1px',
      overflow: 'hidden',
      clip: 'rect(0, 0, 0, 0)',
      whiteSpace: 'nowrap',
      border: '0'
    })
    document.body.appendChild(region)
  }
  
  return region
}

/**
 * 屏幕阅读器公告 Composable
 * 向屏幕阅读器发送公告消息
 * 
 * @returns 公告控制对象
 * 
 * @example
 * ```ts
 * const { announce, clear } = useAnnounce()
 * 
 * // 发送礼貌公告
 * announce('数据已加载完成')
 * 
 * // 发送紧急公告
 * announce('发生错误，请重试', { priority: 'assertive' })
 * ```
 */
export function useAnnounce(): UseAnnounceReturn {
  const currentMessage = ref('')
  let timeoutId: ReturnType<typeof setTimeout> | null = null

  /**
   * 发送公告
   */
  function announce(message: string, options: AnnounceOptions = {}): void {
    const {
      priority = 'polite',
      delay = 0,
      clearPrevious = true
    } = options

    // 清除之前的定时器
    if (timeoutId) {
      clearTimeout(timeoutId)
      timeoutId = null
    }

    // 清除之前的公告
    if (clearPrevious) {
      clear()
    }

    const doAnnounce = () => {
      const region = getAnnounceRegion(priority)
      currentMessage.value = message
      
      // 清空后重新设置，确保屏幕阅读器能检测到变化
      region.textContent = ''
      requestAnimationFrame(() => {
        region.textContent = message
      })
    }

    if (delay > 0) {
      timeoutId = setTimeout(doAnnounce, delay)
    } else {
      doAnnounce()
    }
  }

  /**
   * 清除公告
   */
  function clear(): void {
    currentMessage.value = ''
    const politeRegion = document.getElementById('aria-live-region-polite')
    const assertiveRegion = document.getElementById('aria-live-region-assertive')
    if (politeRegion) politeRegion.textContent = ''
    if (assertiveRegion) assertiveRegion.textContent = ''
  }

  // 清理
  onUnmounted(() => {
    if (timeoutId) {
      clearTimeout(timeoutId)
    }
  })

  return {
    announce,
    clear,
    currentMessage
  }
}


// ============================================================================
// useReducedMotion Composable
// ============================================================================

/**
 * 减少动画检测 Composable
 * 检测用户是否偏好减少动画（prefers-reduced-motion）
 * 
 * @returns 减少动画状态
 * 
 * @example
 * ```ts
 * const { prefersReducedMotion, shouldAnimate } = useReducedMotion()
 * 
 * // 在模板中
 * <transition :name="shouldAnimate ? 'fade' : ''">
 *   <div v-if="show">Content</div>
 * </transition>
 * ```
 */
export function useReducedMotion(): UseReducedMotionReturn {
  const mediaQuery = ref<MediaQueryList | null>(null)
  const systemPrefersReducedMotion = ref(false)

  /**
   * 媒体查询变化处理器
   */
  function handleChange(event: MediaQueryListEvent): void {
    systemPrefersReducedMotion.value = event.matches
  }

  onMounted(() => {
    if (typeof window === 'undefined') return

    mediaQuery.value = window.matchMedia('(prefers-reduced-motion: reduce)')
    systemPrefersReducedMotion.value = mediaQuery.value.matches

    // 监听变化
    if (mediaQuery.value.addEventListener) {
      mediaQuery.value.addEventListener('change', handleChange)
    } else {
      // 兼容旧版浏览器
      mediaQuery.value.addListener(handleChange)
    }
  })

  onUnmounted(() => {
    if (!mediaQuery.value) return

    if (mediaQuery.value.removeEventListener) {
      mediaQuery.value.removeEventListener('change', handleChange)
    } else {
      mediaQuery.value.removeListener(handleChange)
    }
  })

  // 结合系统偏好和主题配置
  const prefersReducedMotion = computed(() => 
    systemPrefersReducedMotion.value || themeConfig.accessibility.reducedMotion
  )

  // 是否应该使用动画
  const shouldAnimate = computed(() => !prefersReducedMotion.value)

  return {
    prefersReducedMotion,
    shouldAnimate
  }
}

// ============================================================================
// ARIA Helpers
// ============================================================================

/**
 * ARIA 属性辅助接口
 */
export interface AriaAttributes {
  'aria-label'?: string
  'aria-labelledby'?: string
  'aria-describedby'?: string
  'aria-expanded'?: boolean
  'aria-selected'?: boolean
  'aria-checked'?: boolean | 'mixed'
  'aria-disabled'?: boolean
  'aria-hidden'?: boolean
  'aria-haspopup'?: boolean | 'menu' | 'listbox' | 'tree' | 'grid' | 'dialog'
  'aria-controls'?: string
  'aria-owns'?: string
  'aria-activedescendant'?: string
  'aria-valuemin'?: number
  'aria-valuemax'?: number
  'aria-valuenow'?: number
  'aria-valuetext'?: string
  'aria-busy'?: boolean
  'aria-live'?: 'off' | 'polite' | 'assertive'
  'aria-atomic'?: boolean
  'aria-relevant'?: 'additions' | 'removals' | 'text' | 'all'
  'aria-current'?: boolean | 'page' | 'step' | 'location' | 'date' | 'time'
  'aria-invalid'?: boolean | 'grammar' | 'spelling'
  'aria-errormessage'?: string
  'aria-required'?: boolean
  'aria-readonly'?: boolean
  'aria-multiselectable'?: boolean
  'aria-orientation'?: 'horizontal' | 'vertical'
  'aria-sort'?: 'none' | 'ascending' | 'descending' | 'other'
  'aria-pressed'?: boolean | 'mixed'
  'aria-level'?: number
  'aria-setsize'?: number
  'aria-posinset'?: number
  'aria-modal'?: boolean
  role?: string
  tabindex?: number
}

/**
 * 创建按钮的 ARIA 属性
 */
export function createButtonAria(options: {
  label: string
  pressed?: boolean
  expanded?: boolean
  controls?: string
  disabled?: boolean
  haspopup?: AriaAttributes['aria-haspopup']
}): AriaAttributes {
  const attrs: AriaAttributes = {
    'aria-label': options.label,
    role: 'button',
    tabindex: options.disabled ? -1 : 0
  }

  if (options.pressed !== undefined) {
    attrs['aria-pressed'] = options.pressed
  }
  if (options.expanded !== undefined) {
    attrs['aria-expanded'] = options.expanded
  }
  if (options.controls) {
    attrs['aria-controls'] = options.controls
  }
  if (options.disabled) {
    attrs['aria-disabled'] = true
  }
  if (options.haspopup) {
    attrs['aria-haspopup'] = options.haspopup
  }

  return attrs
}

/**
 * 创建菜单项的 ARIA 属性
 */
export function createMenuItemAria(options: {
  label: string
  selected?: boolean
  disabled?: boolean
  hasSubmenu?: boolean
  index?: number
  total?: number
}): AriaAttributes {
  const attrs: AriaAttributes = {
    'aria-label': options.label,
    role: 'menuitem',
    tabindex: -1
  }

  if (options.selected !== undefined) {
    attrs['aria-selected'] = options.selected
  }
  if (options.disabled) {
    attrs['aria-disabled'] = true
  }
  if (options.hasSubmenu) {
    attrs['aria-haspopup'] = 'menu'
    attrs['aria-expanded'] = false
  }
  if (options.index !== undefined && options.total !== undefined) {
    attrs['aria-posinset'] = options.index + 1
    attrs['aria-setsize'] = options.total
  }

  return attrs
}

/**
 * 创建选项卡的 ARIA 属性
 */
export function createTabAria(options: {
  label: string
  selected: boolean
  controls: string
  index: number
  total: number
}): AriaAttributes {
  return {
    'aria-label': options.label,
    'aria-selected': options.selected,
    'aria-controls': options.controls,
    'aria-posinset': options.index + 1,
    'aria-setsize': options.total,
    role: 'tab',
    tabindex: options.selected ? 0 : -1
  }
}

/**
 * 创建表单字段的 ARIA 属性
 */
export function createFieldAria(options: {
  label?: string
  labelledby?: string
  describedby?: string
  required?: boolean
  invalid?: boolean
  errormessage?: string
  readonly?: boolean
  disabled?: boolean
}): AriaAttributes {
  const attrs: AriaAttributes = {}

  if (options.label) {
    attrs['aria-label'] = options.label
  }
  if (options.labelledby) {
    attrs['aria-labelledby'] = options.labelledby
  }
  if (options.describedby) {
    attrs['aria-describedby'] = options.describedby
  }
  if (options.required) {
    attrs['aria-required'] = true
  }
  if (options.invalid) {
    attrs['aria-invalid'] = true
  }
  if (options.errormessage) {
    attrs['aria-errormessage'] = options.errormessage
  }
  if (options.readonly) {
    attrs['aria-readonly'] = true
  }
  if (options.disabled) {
    attrs['aria-disabled'] = true
  }

  return attrs
}

/**
 * 创建进度条的 ARIA 属性
 */
export function createProgressAria(options: {
  label: string
  value: number
  min?: number
  max?: number
  valuetext?: string
}): AriaAttributes {
  const result: AriaAttributes = {
    'aria-label': options.label,
    'aria-valuenow': options.value,
    'aria-valuemin': options.min ?? 0,
    'aria-valuemax': options.max ?? 100,
    role: 'progressbar'
  }
  if (options.valuetext !== undefined) {
    result['aria-valuetext'] = options.valuetext
  }
  return result
}

/**
 * 创建对话框的 ARIA 属性
 */
export function createDialogAria(options: {
  label?: string
  labelledby?: string
  describedby?: string
  modal?: boolean
}): AriaAttributes {
  const attrs: AriaAttributes = {
    role: options.modal ? 'alertdialog' : 'dialog'
  }

  if (options.label) {
    attrs['aria-label'] = options.label
  }
  if (options.labelledby) {
    attrs['aria-labelledby'] = options.labelledby
  }
  if (options.describedby) {
    attrs['aria-describedby'] = options.describedby
  }
  if (options.modal) {
    attrs['aria-modal'] = true
  }

  return attrs
}


// ============================================================================
// Combined Accessibility Composable
// ============================================================================

/**
 * 综合无障碍 Composable 返回类型
 */
export interface UseAccessibilityReturn {
  /** 焦点陷阱 */
  focusTrap: UseFocusTrapReturn
  /** 键盘导航 */
  keyboardNav: UseKeyboardNavigationReturn
  /** 屏幕阅读器公告 */
  announcer: UseAnnounceReturn
  /** 减少动画检测 */
  reducedMotion: UseReducedMotionReturn
  /** 生成 ARIA ID */
  generateId: typeof generateAriaId
  /** 获取可聚焦元素 */
  getFocusable: typeof getFocusableElements
  /** 检查是否可聚焦 */
  checkFocusable: typeof isFocusable
}

/**
 * 综合无障碍 Composable
 * 提供所有无障碍功能的统一入口
 * 
 * @param options - 配置选项
 * @returns 无障碍功能集合
 * 
 * @example
 * ```ts
 * const {
 *   focusTrap,
 *   keyboardNav,
 *   announcer,
 *   reducedMotion,
 *   generateId
 * } = useAccessibility({
 *   focusTrap: { autoFocus: true },
 *   keyboardNav: { orientation: 'vertical' }
 * })
 * ```
 */
export function useAccessibility(options?: {
  focusTrap?: FocusTrapOptions
  keyboardNav?: KeyboardNavigationOptions
}): UseAccessibilityReturn {
  const focusTrap = useFocusTrap(options?.focusTrap)
  const keyboardNav = useKeyboardNavigation(options?.keyboardNav)
  const announcer = useAnnounce()
  const reducedMotion = useReducedMotion()

  return {
    focusTrap,
    keyboardNav,
    announcer,
    reducedMotion,
    generateId: generateAriaId,
    getFocusable: getFocusableElements,
    checkFocusable: isFocusable
  }
}

// ============================================================================
// Default Export
// ============================================================================

export default useAccessibility

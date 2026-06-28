/**
 * 快捷键提示 Composable
 * Shortcut Hints Composable
 * 
 * 提供响应式的快捷键提示服务访问
 * 
 * 需求 19.4: THE DataTeaCup SHALL 显示当前页面可用的快捷键列表
 * 
 * @module useShortcutHints
 */

import { ref, readonly, computed, type ComputedRef, type Ref } from 'vue'
import {
  ShortcutHintsService,
  getShortcutHintsService,
  createShortcutHintsService,
  SHORTCUT_CATEGORIES
} from './ShortcutHintsService'
import type {
  ShortcutItem,
  ShortcutCategory,
  ShortcutCategoryInfo,
  ShortcutHintsState
} from './shortcutHintsTypes'

/**
 * useShortcutHints 返回值接口
 */
export interface UseShortcutHintsReturn {
  // 状态
  state: Readonly<Ref<ShortcutHintsState>>
  isOpen: ComputedRef<boolean>
  searchQuery: ComputedRef<string>
  selectedCategory: ComputedRef<ShortcutCategory | null>
  currentScope: ComputedRef<string | null>
  categories: ComputedRef<ShortcutCategoryInfo[]>
  
  // 方法
  open: () => void
  close: () => void
  toggle: () => void
  selectCategory: (category: ShortcutCategory | null) => void
  setSearchQuery: (query: string) => void
  clearSearch: () => void
  setCurrentScope: (scope: string | null) => void
  
  // 数据获取方法
  getAllShortcuts: () => ShortcutItem[]
  getShortcutsByCategory: (category: ShortcutCategory) => ShortcutItem[]
  getShortcutsByScope: (scope: string) => ShortcutItem[]
  search: (query: string) => ShortcutItem[]
  formatShortcut: (key: string) => string
  getCategoryInfo: (category: ShortcutCategory) => ShortcutCategoryInfo | undefined
  
  // 注册方法
  registerShortcut: (shortcut: ShortcutItem) => void
  registerShortcuts: (shortcuts: ShortcutItem[]) => void
  unregisterShortcut: (id: string) => void
}

/**
 * 快捷键提示 Composable
 * 
 * 提供响应式的快捷键提示服务访问
 * 
 * @example
 * ```typescript
 * const { 
 *   isOpen, 
 *   categories,
 *   open, 
 *   close,
 *   toggle,
 *   getAllShortcuts,
 *   registerShortcut
 * } = useShortcutHints()
 * 
 * // 打开快捷键提示
 * open()
 * 
 * // 注册自定义快捷键
 * registerShortcut({
 *   id: 'custom-action',
 *   key: 'Ctrl+Shift+A',
 *   description: '自定义操作',
 *   category: 'custom',
 *   scope: 'my-component'
 * })
 * ```
 */
export function useShortcutHints(): UseShortcutHintsReturn {
  const service = getShortcutHintsService()
  
  // 响应式状态
  const state = ref<ShortcutHintsState>(service.getState())
  
  // 监听状态变更
  service.setOnStateChange((newState) => {
    state.value = newState
  })
  
  // 计算属性
  const isOpen = computed(() => state.value.isOpen)
  const searchQuery = computed(() => state.value.searchQuery)
  const selectedCategory = computed(() => state.value.selectedCategory)
  const currentScope = computed(() => state.value.currentScope)
  const categories = computed(() => service.getCategories())
  
  return {
    // 状态
    state: readonly(state),
    isOpen,
    searchQuery,
    selectedCategory,
    currentScope,
    categories,
    
    // 方法
    open: () => service.open(),
    close: () => service.close(),
    toggle: () => service.toggle(),
    selectCategory: (category: ShortcutCategory | null) => service.selectCategory(category),
    setSearchQuery: (query: string) => service.setSearchQuery(query),
    clearSearch: () => service.clearSearch(),
    setCurrentScope: (scope: string | null) => service.setCurrentScope(scope),
    
    // 数据获取方法
    getAllShortcuts: () => service.getAllShortcuts(),
    getShortcutsByCategory: (category: ShortcutCategory) => service.getShortcutsByCategory(category),
    getShortcutsByScope: (scope: string) => service.getShortcutsByScope(scope),
    search: (query: string) => service.search(query),
    formatShortcut: (key: string) => service.formatShortcut(key),
    getCategoryInfo: (category: ShortcutCategory) => service.getCategoryInfo(category),
    
    // 注册方法
    registerShortcut: (shortcut: ShortcutItem) => service.registerShortcut(shortcut),
    registerShortcuts: (shortcuts: ShortcutItem[]) => service.registerShortcuts(shortcuts),
    unregisterShortcut: (id: string) => service.unregisterShortcut(id)
  }
}

// 导出服务相关
export {
  ShortcutHintsService,
  getShortcutHintsService,
  createShortcutHintsService,
  SHORTCUT_CATEGORIES
}

export default useShortcutHints


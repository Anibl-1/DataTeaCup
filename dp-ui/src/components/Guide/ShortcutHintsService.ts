/**
 * 快捷键提示服务
 * Shortcut Hints Service
 * 
 * 管理快捷键提示的显示和注册，支持按分类和范围筛选。
 * 
 * 需求 19.4: THE DataTeaCup SHALL 显示当前页面可用的快捷键列表
 * 
 * @module ShortcutHintsService
 */

// Vue imports removed - not needed in service class
import type {
  ShortcutItem,
  ShortcutCategory,
  ShortcutCategoryInfo,
  ShortcutHintsState,
  IShortcutHintsService
} from './shortcutHintsTypes'
import { formatShortcutForDisplay } from '@/composables/useShortcuts'

// ==================== 常量定义 ====================

/** 快捷键分类信息 */
export const SHORTCUT_CATEGORIES: ShortcutCategoryInfo[] = [
  {
    id: 'navigation',
    name: '导航',
    description: '页面和元素导航快捷键',
    icon: 'compass',
    order: 1
  },
  {
    id: 'editing',
    name: '编辑',
    description: '编辑操作快捷键',
    icon: 'edit',
    order: 2
  },
  {
    id: 'file',
    name: '文件',
    description: '文件操作快捷键',
    icon: 'file',
    order: 3
  },
  {
    id: 'view',
    name: '视图',
    description: '视图控制快捷键',
    icon: 'eye',
    order: 4
  },
  {
    id: 'help',
    name: '帮助',
    description: '帮助和提示快捷键',
    icon: 'help-circle',
    order: 5
  },
  {
    id: 'custom',
    name: '自定义',
    description: '自定义快捷键',
    icon: 'settings',
    order: 6
  }
]

/** 默认状态 */
const DEFAULT_STATE: ShortcutHintsState = {
  isOpen: false,
  selectedCategory: null,
  searchQuery: '',
  currentScope: null
}

/** 预设的全局快捷键 */
export const PRESET_SHORTCUTS: ShortcutItem[] = [
  // 文件操作
  {
    id: 'save',
    key: 'Ctrl+S',
    description: '保存',
    category: 'file',
    scope: 'global',
    order: 1
  },
  // 编辑操作
  {
    id: 'undo',
    key: 'Ctrl+Z',
    description: '撤销',
    category: 'editing',
    scope: 'global',
    order: 1
  },
  {
    id: 'redo',
    key: 'Ctrl+Y',
    description: '重做',
    category: 'editing',
    scope: 'global',
    order: 2
  },
  {
    id: 'redo-alt',
    key: 'Ctrl+Shift+Z',
    description: '重做（备选）',
    category: 'editing',
    scope: 'global',
    order: 3
  },
  {
    id: 'copy',
    key: 'Ctrl+C',
    description: '复制',
    category: 'editing',
    scope: 'global',
    order: 4
  },
  {
    id: 'paste',
    key: 'Ctrl+V',
    description: '粘贴',
    category: 'editing',
    scope: 'global',
    order: 5
  },
  {
    id: 'cut',
    key: 'Ctrl+X',
    description: '剪切',
    category: 'editing',
    scope: 'global',
    order: 6
  },
  {
    id: 'select-all',
    key: 'Ctrl+A',
    description: '全选',
    category: 'editing',
    scope: 'global',
    order: 7
  },
  {
    id: 'delete',
    key: 'Delete',
    description: '删除',
    category: 'editing',
    scope: 'global',
    order: 8
  },
  // 视图操作
  {
    id: 'preview',
    key: 'Ctrl+P',
    description: '预览',
    category: 'view',
    scope: 'global',
    order: 1
  },
  {
    id: 'export',
    key: 'Ctrl+E',
    description: '导出',
    category: 'file',
    scope: 'global',
    order: 2
  },
  // 帮助
  {
    id: 'help',
    key: 'F1',
    description: '打开帮助',
    category: 'help',
    scope: 'global',
    order: 1
  },
  {
    id: 'shortcut-hints',
    key: 'Ctrl+/',
    description: '显示快捷键提示',
    category: 'help',
    scope: 'global',
    order: 2
  },
  {
    id: 'shortcut-hints-alt',
    key: '?',
    description: '显示快捷键提示（备选）',
    category: 'help',
    scope: 'global',
    order: 3
  },
  // 导航
  {
    id: 'escape',
    key: 'Escape',
    description: '关闭/取消',
    category: 'navigation',
    scope: 'global',
    order: 1
  }
]

// ==================== 快捷键提示服务类 ====================

/**
 * 快捷键提示服务类
 * 
 * 提供快捷键提示的完整管理功能，包括：
 * - 快捷键注册和存储
 * - 分类管理
 * - 范围筛选
 * - 搜索功能
 */
export class ShortcutHintsService implements IShortcutHintsService {
  /** 快捷键存储 */
  private shortcuts: Map<string, ShortcutItem> = new Map()
  
  /** 分类索引 */
  private categoryIndex: Map<ShortcutCategory, Set<string>> = new Map()
  
  /** 范围索引 */
  private scopeIndex: Map<string, Set<string>> = new Map()
  
  /** 状态变更回调 */
  private onStateChange: ((state: ShortcutHintsState) => void) | null = null
  
  /** 当前状态 */
  private state: ShortcutHintsState = { ...DEFAULT_STATE }
  
  constructor() {
    // 初始化分类索引
    SHORTCUT_CATEGORIES.forEach(cat => {
      this.categoryIndex.set(cat.id, new Set())
    })
    
    // 注册预设快捷键
    this.registerShortcuts(PRESET_SHORTCUTS)
  }

  /**
   * 设置状态变更回调
   */
  setOnStateChange(callback: (state: ShortcutHintsState) => void): void {
    this.onStateChange = callback
  }
  
  /**
   * 触发状态变更
   */
  private emitStateChange(): void {
    if (this.onStateChange) {
      this.onStateChange({ ...this.state })
    }
  }
  
  /**
   * 获取当前状态
   */
  getState(): ShortcutHintsState {
    return { ...this.state }
  }
  
  /**
   * 获取所有分类
   */
  getCategories(): ShortcutCategoryInfo[] {
    return [...SHORTCUT_CATEGORIES].sort((a, b) => a.order - b.order)
  }
  
  /**
   * 获取分类信息
   */
  getCategoryInfo(category: ShortcutCategory): ShortcutCategoryInfo | undefined {
    return SHORTCUT_CATEGORIES.find(c => c.id === category)
  }
  
  /**
   * 注册快捷键
   */
  registerShortcut(shortcut: ShortcutItem): void {
    if (!shortcut.id || !shortcut.key || !shortcut.description) {
      console.warn('[ShortcutHintsService] 无效的快捷键配置:', shortcut)
      return
    }
    
    // 存储快捷键
    this.shortcuts.set(shortcut.id, {
      ...shortcut,
      enabled: shortcut.enabled ?? true
    })
    
    // 更新分类索引
    const categorySet = this.categoryIndex.get(shortcut.category)
    if (categorySet) {
      categorySet.add(shortcut.id)
    }
    
    // 更新范围索引
    const scope = shortcut.scope || 'global'
    if (!this.scopeIndex.has(scope)) {
      this.scopeIndex.set(scope, new Set())
    }
    this.scopeIndex.get(scope)!.add(shortcut.id)
  }
  
  /**
   * 批量注册快捷键
   */
  registerShortcuts(shortcuts: ShortcutItem[]): void {
    shortcuts.forEach(shortcut => this.registerShortcut(shortcut))
  }
  
  /**
   * 注销快捷键
   */
  unregisterShortcut(id: string): void {
    const shortcut = this.shortcuts.get(id)
    if (!shortcut) return
    
    // 从存储中移除
    this.shortcuts.delete(id)
    
    // 从分类索引中移除
    const categorySet = this.categoryIndex.get(shortcut.category)
    if (categorySet) {
      categorySet.delete(id)
    }
    
    // 从范围索引中移除
    const scope = shortcut.scope || 'global'
    const scopeSet = this.scopeIndex.get(scope)
    if (scopeSet) {
      scopeSet.delete(id)
    }
  }
  
  /**
   * 获取所有快捷键
   */
  getAllShortcuts(): ShortcutItem[] {
    return Array.from(this.shortcuts.values())
      .filter(s => s.enabled !== false)
      .sort((a, b) => (a.order ?? 999) - (b.order ?? 999))
  }
  
  /**
   * 获取分类下的快捷键
   */
  getShortcutsByCategory(category: ShortcutCategory): ShortcutItem[] {
    const shortcutIds = this.categoryIndex.get(category)
    if (!shortcutIds) return []
    
    const shortcuts: ShortcutItem[] = []
    shortcutIds.forEach(id => {
      const shortcut = this.shortcuts.get(id)
      if (shortcut && shortcut.enabled !== false) {
        shortcuts.push(shortcut)
      }
    })
    
    return shortcuts.sort((a, b) => (a.order ?? 999) - (b.order ?? 999))
  }
  
  /**
   * 获取当前范围的快捷键
   */
  getShortcutsByScope(scope: string): ShortcutItem[] {
    const result: ShortcutItem[] = []
    
    // 获取全局快捷键
    const globalIds = this.scopeIndex.get('global')
    if (globalIds) {
      globalIds.forEach(id => {
        const shortcut = this.shortcuts.get(id)
        if (shortcut && shortcut.enabled !== false) {
          result.push(shortcut)
        }
      })
    }
    
    // 获取指定范围的快捷键
    if (scope !== 'global') {
      const scopeIds = this.scopeIndex.get(scope)
      if (scopeIds) {
        scopeIds.forEach(id => {
          const shortcut = this.shortcuts.get(id)
          if (shortcut && shortcut.enabled !== false) {
            result.push(shortcut)
          }
        })
      }
    }
    
    return result.sort((a, b) => (a.order ?? 999) - (b.order ?? 999))
  }
  
  /**
   * 获取当前范围的快捷键（按分类分组）
   */
  getShortcutsByScopeGrouped(scope: string | null): Map<ShortcutCategory, ShortcutItem[]> {
    const shortcuts = scope ? this.getShortcutsByScope(scope) : this.getAllShortcuts()
    const grouped = new Map<ShortcutCategory, ShortcutItem[]>()
    
    // 初始化所有分类
    SHORTCUT_CATEGORIES.forEach(cat => {
      grouped.set(cat.id, [])
    })
    
    // 分组
    shortcuts.forEach(shortcut => {
      const list = grouped.get(shortcut.category)
      if (list) {
        list.push(shortcut)
      }
    })
    
    // 移除空分类
    grouped.forEach((list, category) => {
      if (list.length === 0) {
        grouped.delete(category)
      }
    })
    
    return grouped
  }
  
  /**
   * 搜索快捷键
   */
  search(query: string): ShortcutItem[] {
    if (!query.trim()) return this.getAllShortcuts()
    
    const queryLower = query.toLowerCase()
    const results: ShortcutItem[] = []
    
    this.shortcuts.forEach(shortcut => {
      if (shortcut.enabled === false) return
      
      // 匹配描述
      if (shortcut.description.toLowerCase().includes(queryLower)) {
        results.push(shortcut)
        return
      }
      
      // 匹配快捷键
      if (shortcut.key.toLowerCase().includes(queryLower)) {
        results.push(shortcut)
        return
      }
    })
    
    return results.sort((a, b) => (a.order ?? 999) - (b.order ?? 999))
  }
  
  /**
   * 设置当前范围
   */
  setCurrentScope(scope: string | null): void {
    this.state.currentScope = scope
    this.emitStateChange()
  }
  
  // ==================== 状态管理方法 ====================
  
  /**
   * 打开快捷键提示面板
   */
  open(): void {
    this.state.isOpen = true
    this.emitStateChange()
  }
  
  /**
   * 关闭快捷键提示面板
   */
  close(): void {
    this.state.isOpen = false
    this.state.searchQuery = ''
    this.state.selectedCategory = null
    this.emitStateChange()
  }
  
  /**
   * 切换显示状态
   */
  toggle(): void {
    if (this.state.isOpen) {
      this.close()
    } else {
      this.open()
    }
  }
  
  /**
   * 选择分类
   */
  selectCategory(category: ShortcutCategory | null): void {
    this.state.selectedCategory = category
    this.emitStateChange()
  }
  
  /**
   * 设置搜索关键词
   */
  setSearchQuery(query: string): void {
    this.state.searchQuery = query
    this.emitStateChange()
  }
  
  /**
   * 清除搜索
   */
  clearSearch(): void {
    this.state.searchQuery = ''
    this.emitStateChange()
  }
  
  /**
   * 格式化快捷键用于显示
   */
  formatShortcut(key: string): string {
    return formatShortcutForDisplay(key)
  }
}

// ==================== 单例实例 ====================

/** 全局快捷键提示服务实例 */
let shortcutHintsServiceInstance: ShortcutHintsService | null = null

/**
 * 获取快捷键提示服务实例（单例）
 */
export function getShortcutHintsService(): ShortcutHintsService {
  if (!shortcutHintsServiceInstance) {
    shortcutHintsServiceInstance = new ShortcutHintsService()
  }
  return shortcutHintsServiceInstance
}

/**
 * 创建新的快捷键提示服务实例
 */
export function createShortcutHintsService(): ShortcutHintsService {
  return new ShortcutHintsService()
}


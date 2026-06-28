/**
 * 快捷键提示类型定义
 * Shortcut Hints Type Definitions
 * 
 * 需求 19.4: THE DataTeaCup SHALL 显示当前页面可用的快捷键列表
 */

/**
 * 快捷键分类
 */
export type ShortcutCategory = 
  | 'navigation'    // 导航
  | 'editing'       // 编辑
  | 'file'          // 文件操作
  | 'view'          // 视图
  | 'help'          // 帮助
  | 'custom'        // 自定义

/**
 * 快捷键分类信息
 */
export interface ShortcutCategoryInfo {
  /** 分类ID */
  id: ShortcutCategory
  /** 分类名称 */
  name: string
  /** 分类描述 */
  description?: string
  /** 分类图标 */
  icon?: string
  /** 排序顺序 */
  order: number
}

/**
 * 快捷键项
 */
export interface ShortcutItem {
  /** 快捷键唯一标识 */
  id: string
  /** 快捷键组合（如 'Ctrl+S'） */
  key: string
  /** 快捷键描述 */
  description: string
  /** 所属分类 */
  category: ShortcutCategory
  /** 所属页面/功能 */
  scope?: string
  /** 是否启用 */
  enabled?: boolean
  /** 排序顺序 */
  order?: number
}

/**
 * 快捷键提示状态
 */
export interface ShortcutHintsState {
  /** 是否显示快捷键提示面板 */
  isOpen: boolean
  /** 当前选中的分类 */
  selectedCategory: ShortcutCategory | null
  /** 搜索关键词 */
  searchQuery: string
  /** 当前页面/功能范围 */
  currentScope: string | null
}

/**
 * 快捷键提示服务接口
 */
export interface IShortcutHintsService {
  /** 打开快捷键提示面板 */
  open(): void
  /** 关闭快捷键提示面板 */
  close(): void
  /** 切换显示状态 */
  toggle(): void
  /** 注册快捷键 */
  registerShortcut(shortcut: ShortcutItem): void
  /** 批量注册快捷键 */
  registerShortcuts(shortcuts: ShortcutItem[]): void
  /** 注销快捷键 */
  unregisterShortcut(id: string): void
  /** 获取所有快捷键 */
  getAllShortcuts(): ShortcutItem[]
  /** 获取分类下的快捷键 */
  getShortcutsByCategory(category: ShortcutCategory): ShortcutItem[]
  /** 获取当前范围的快捷键 */
  getShortcutsByScope(scope: string): ShortcutItem[]
  /** 搜索快捷键 */
  search(query: string): ShortcutItem[]
  /** 设置当前范围 */
  setCurrentScope(scope: string | null): void
  /** 获取当前状态 */
  getState(): ShortcutHintsState
}


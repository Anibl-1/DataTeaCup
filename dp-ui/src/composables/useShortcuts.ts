import { ref, onMounted, onUnmounted, type Ref } from 'vue'

/**
 * 快捷键修饰符
 */
export interface ShortcutModifiers {
  ctrl?: boolean
  shift?: boolean
  alt?: boolean
  meta?: boolean
}

/**
 * 快捷键定义
 */
export interface ShortcutDefinition {
  /** 快捷键组合，如 'Ctrl+S', 'Ctrl+Shift+Z' */
  key: string
  /** 快捷键处理函数 */
  handler: (event: KeyboardEvent) => void
  /** 快捷键描述 */
  description?: string
  /** 作用域，用于区分不同组件的快捷键 */
  scope?: string
  /** 是否阻止默认行为，默认 true */
  preventDefault?: boolean
  /** 是否阻止事件冒泡，默认 false */
  stopPropagation?: boolean
  /** 是否启用，默认 true */
  enabled?: boolean
}

/**
 * 解析后的快捷键
 */
export interface ParsedShortcut {
  /** 主键（不含修饰符） */
  key: string
  /** 修饰符 */
  modifiers: ShortcutModifiers
  /** 原始快捷键字符串 */
  original: string
}

/**
 * 已注册的快捷键
 */
export interface RegisteredShortcut extends ShortcutDefinition {
  /** 解析后的快捷键信息 */
  parsed: ParsedShortcut
  /** 注册ID */
  id: string
}

/**
 * 快捷键冲突信息
 */
export interface ShortcutConflict {
  /** 冲突的快捷键 */
  key: string
  /** 已存在的快捷键 */
  existing: RegisteredShortcut
  /** 新注册的快捷键 */
  incoming: ShortcutDefinition
}

/**
 * useShortcuts 配置选项
 */
export interface UseShortcutsOptions {
  /** 作用域，用于区分不同组件的快捷键 */
  scope?: string
  /** 是否在挂载时自动启用，默认 true */
  autoEnable?: boolean
  /** 是否检测冲突，默认 true */
  detectConflicts?: boolean
  /** 冲突回调 */
  onConflict?: (conflict: ShortcutConflict) => void
}

/**
 * useShortcuts 返回值接口
 */
export interface UseShortcutsReturn {
  /** 已注册的快捷键列表 */
  shortcuts: Ref<RegisteredShortcut[]>
  /** 是否启用 */
  enabled: Ref<boolean>
  /** 注册快捷键 */
  register: (shortcut: ShortcutDefinition) => string
  /** 批量注册快捷键 */
  registerMany: (shortcuts: ShortcutDefinition[]) => string[]
  /** 注销快捷键 */
  unregister: (id: string) => boolean
  /** 注销所有快捷键 */
  unregisterAll: () => void
  /** 启用快捷键监听 */
  enable: () => void
  /** 禁用快捷键监听 */
  disable: () => void
  /** 检查快捷键是否已注册 */
  isRegistered: (key: string) => boolean
  /** 获取快捷键列表（用于显示帮助） */
  getShortcutList: () => Array<{ key: string; description: string }>
  /** 解析快捷键字符串 */
  parseShortcut: (key: string) => ParsedShortcut
}

// 全局快捷键注册表（用于跨组件冲突检测）
const globalRegistry = new Map<string, RegisteredShortcut>()

// 生成唯一ID
let shortcutIdCounter = 0
const generateId = (): string => `shortcut_${++shortcutIdCounter}`

/**
 * 重置快捷键ID计数器（用于测试）
 */
export const resetShortcutIdCounter = (): void => {
  shortcutIdCounter = 0
}

/**
 * 解析快捷键字符串
 * 支持格式：'Ctrl+S', 'Ctrl+Shift+Z', 'Alt+Enter', 'Meta+C', 'Escape', 'Delete'
 */
export const parseShortcutKey = (key: string): ParsedShortcut => {
  const parts = key.split('+').map(p => p.trim().toLowerCase())
  const modifiers: ShortcutModifiers = {
    ctrl: false,
    shift: false,
    alt: false,
    meta: false
  }
  
  let mainKey = ''
  
  for (const part of parts) {
    switch (part) {
      case 'ctrl':
      case 'control':
        modifiers.ctrl = true
        break
      case 'shift':
        modifiers.shift = true
        break
      case 'alt':
        modifiers.alt = true
        break
      case 'meta':
      case 'cmd':
      case 'command':
        modifiers.meta = true
        break
      default:
        mainKey = part
    }
  }
  
  return {
    key: mainKey,
    modifiers,
    original: key
  }
}

/**
 * 将解析后的快捷键转换为标准化字符串（用于比较）
 */
export const normalizeShortcutKey = (parsed: ParsedShortcut): string => {
  const parts: string[] = []
  if (parsed.modifiers.ctrl) parts.push('ctrl')
  if (parsed.modifiers.alt) parts.push('alt')
  if (parsed.modifiers.shift) parts.push('shift')
  if (parsed.modifiers.meta) parts.push('meta')
  parts.push(parsed.key)
  return parts.join('+')
}

/**
 * 检查键盘事件是否匹配快捷键
 */
export const matchShortcut = (event: KeyboardEvent, parsed: ParsedShortcut): boolean => {
  // 检查修饰符
  if (event.ctrlKey !== (parsed.modifiers.ctrl ?? false)) return false
  if (event.shiftKey !== (parsed.modifiers.shift ?? false)) return false
  if (event.altKey !== (parsed.modifiers.alt ?? false)) return false
  if (event.metaKey !== (parsed.modifiers.meta ?? false)) return false
  
  // 检查主键
  const eventKey = event.key.toLowerCase()
  const targetKey = parsed.key.toLowerCase()
  
  // 处理特殊键名映射
  const keyMap: Record<string, string[]> = {
    'escape': ['escape', 'esc'],
    'delete': ['delete', 'del'],
    'backspace': ['backspace'],
    'enter': ['enter', 'return'],
    'tab': ['tab'],
    'space': [' ', 'space'],
    'arrowup': ['arrowup', 'up'],
    'arrowdown': ['arrowdown', 'down'],
    'arrowleft': ['arrowleft', 'left'],
    'arrowright': ['arrowright', 'right']
  }
  
  // 检查是否匹配
  for (const [, aliases] of Object.entries(keyMap)) {
    if (aliases.includes(targetKey) && aliases.includes(eventKey)) {
      return true
    }
  }
  
  // 直接比较
  return eventKey === targetKey
}

/**
 * 格式化快捷键用于显示
 */
export const formatShortcutForDisplay = (key: string): string => {
  const isMac = typeof navigator !== 'undefined' && /Mac|iPod|iPhone|iPad/.test(navigator.platform)
  
  return key
    .replace(/ctrl/gi, isMac ? '⌃' : 'Ctrl')
    .replace(/shift/gi, isMac ? '⇧' : 'Shift')
    .replace(/alt/gi, isMac ? '⌥' : 'Alt')
    .replace(/meta|cmd|command/gi, isMac ? '⌘' : 'Win')
    .replace(/\+/g, isMac ? '' : '+')
}


/**
 * 快捷键管理 Composable
 * 
 * 提供快捷键注册、注销、冲突检测等功能，支持：
 * - 快捷键组合解析（Ctrl+S, Ctrl+Shift+Z 等）
 * - 作用域管理（全局 vs 组件级）
 * - 冲突检测
 * - 自动清理
 * 
 * @param options 配置选项
 * @returns 快捷键管理方法和状态
 * 
 * @example
 * ```typescript
 * const { register, unregister, enable, disable } = useShortcuts({ scope: 'chart-designer' })
 * 
 * // 注册快捷键
 * const saveId = register({
 *   key: 'Ctrl+S',
 *   handler: () => handleSave(),
 *   description: '保存'
 * })
 * 
 * // 批量注册
 * registerMany([
 *   { key: 'Ctrl+Z', handler: handleUndo, description: '撤销' },
 *   { key: 'Ctrl+Y', handler: handleRedo, description: '重做' }
 * ])
 * 
 * // 注销
 * unregister(saveId)
 * ```
 */
export function useShortcuts(options: UseShortcutsOptions = {}): UseShortcutsReturn {
  const {
    scope = 'global',
    autoEnable = true,
    detectConflicts = true,
    onConflict
  } = options
  
  // 已注册的快捷键
  const shortcuts = ref<RegisteredShortcut[]>([])
  
  // 是否启用
  const enabled = ref(false)
  
  // 当前作用域的快捷键ID列表（用于清理）
  const scopeShortcutIds: string[] = []
  
  /**
   * 解析快捷键
   */
  const parseShortcut = (key: string): ParsedShortcut => {
    return parseShortcutKey(key)
  }
  
  /**
   * 检查快捷键是否已注册
   */
  const isRegistered = (key: string): boolean => {
    const parsed = parseShortcut(key)
    const normalized = normalizeShortcutKey(parsed)
    return globalRegistry.has(normalized)
  }
  
  /**
   * 注册快捷键
   */
  const register = (shortcut: ShortcutDefinition): string => {
    const id = generateId()
    const parsed = parseShortcut(shortcut.key)
    const normalized = normalizeShortcutKey(parsed)
    
    // 检测冲突
    if (detectConflicts && globalRegistry.has(normalized)) {
      const existing = globalRegistry.get(normalized)!
      const conflict: ShortcutConflict = {
        key: shortcut.key,
        existing,
        incoming: shortcut
      }
      
      if (onConflict) {
        onConflict(conflict)
      } else {
        console.warn(
          `[useShortcuts] 快捷键冲突: ${shortcut.key} 已被 ${existing.scope || 'global'} 作用域注册`
        )
      }
    }
    
    const registered: RegisteredShortcut = {
      ...shortcut,
      parsed,
      id,
      scope: shortcut.scope || scope,
      preventDefault: shortcut.preventDefault ?? true,
      stopPropagation: shortcut.stopPropagation ?? false,
      enabled: shortcut.enabled ?? true
    }
    
    // 添加到本地列表
    shortcuts.value.push(registered)
    
    // 添加到全局注册表
    globalRegistry.set(normalized, registered)
    
    // 记录ID用于清理
    scopeShortcutIds.push(id)
    
    return id
  }
  
  /**
   * 批量注册快捷键
   */
  const registerMany = (shortcutList: ShortcutDefinition[]): string[] => {
    return shortcutList.map(s => register(s))
  }
  
  /**
   * 注销快捷键
   */
  const unregister = (id: string): boolean => {
    const index = shortcuts.value.findIndex(s => s.id === id)
    if (index === -1) return false
    
    const shortcut = shortcuts.value[index]!
    const normalized = normalizeShortcutKey(shortcut.parsed)
    
    // 从本地列表移除
    shortcuts.value.splice(index, 1)
    
    // 从全局注册表移除
    globalRegistry.delete(normalized)
    
    // 从作用域ID列表移除
    const scopeIndex = scopeShortcutIds.indexOf(id)
    if (scopeIndex !== -1) {
      scopeShortcutIds.splice(scopeIndex, 1)
    }
    
    return true
  }
  
  /**
   * 注销所有快捷键
   */
  const unregisterAll = (): void => {
    // 复制ID列表，因为 unregister 会修改原数组
    const ids = [...scopeShortcutIds]
    ids.forEach(id => unregister(id))
  }
  
  /**
   * 键盘事件处理器
   */
  const handleKeyDown = (event: KeyboardEvent): void => {
    if (!enabled.value) return
    
    // 忽略输入框中的快捷键（除非是特定的全局快捷键）
    const target = event.target as HTMLElement
    const isInputElement = target.tagName === 'INPUT' || 
                          target.tagName === 'TEXTAREA' || 
                          target.isContentEditable
    
    for (const shortcut of shortcuts.value) {
      // 检查是否启用
      if (shortcut.enabled === false) continue
      
      // 检查是否匹配
      if (!matchShortcut(event, shortcut.parsed)) continue
      
      // 如果在输入框中，只处理带修饰符的快捷键
      if (isInputElement) {
        const hasModifier = shortcut.parsed.modifiers.ctrl || 
                           shortcut.parsed.modifiers.alt || 
                           shortcut.parsed.modifiers.meta
        if (!hasModifier) continue
      }
      
      // 阻止默认行为
      if (shortcut.preventDefault) {
        event.preventDefault()
      }
      
      // 阻止事件冒泡
      if (shortcut.stopPropagation) {
        event.stopPropagation()
      }
      
      // 执行处理函数
      shortcut.handler(event)
      
      // 只执行第一个匹配的快捷键
      break
    }
  }
  
  /**
   * 启用快捷键监听
   */
  const enable = (): void => {
    if (enabled.value) return
    enabled.value = true
    window.addEventListener('keydown', handleKeyDown)
  }
  
  /**
   * 禁用快捷键监听
   */
  const disable = (): void => {
    if (!enabled.value) return
    enabled.value = false
    window.removeEventListener('keydown', handleKeyDown)
  }
  
  /**
   * 获取快捷键列表（用于显示帮助）
   */
  const getShortcutList = (): Array<{ key: string; description: string }> => {
    return shortcuts.value
      .filter(s => s.enabled !== false && s.description)
      .map(s => ({
        key: formatShortcutForDisplay(s.key),
        description: s.description || ''
      }))
  }
  
  // 生命周期
  onMounted(() => {
    if (autoEnable) {
      enable()
    }
  })
  
  onUnmounted(() => {
    disable()
    unregisterAll()
  })
  
  return {
    shortcuts,
    enabled,
    register,
    registerMany,
    unregister,
    unregisterAll,
    enable,
    disable,
    isRegistered,
    getShortcutList,
    parseShortcut
  }
}

/**
 * 预定义的常用快捷键
 */
export const COMMON_SHORTCUTS = {
  SAVE: 'Ctrl+S',
  UNDO: 'Ctrl+Z',
  REDO: 'Ctrl+Y',
  REDO_ALT: 'Ctrl+Shift+Z',
  COPY: 'Ctrl+C',
  PASTE: 'Ctrl+V',
  CUT: 'Ctrl+X',
  DELETE: 'Delete',
  ESCAPE: 'Escape',
  ENTER: 'Enter',
  SELECT_ALL: 'Ctrl+A',
  PREVIEW: 'Ctrl+P',
  EXPORT: 'Ctrl+E',
  HELP: 'F1'
} as const

export default useShortcuts

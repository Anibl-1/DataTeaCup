/**
 * 快捷键提示服务单元测试
 * Shortcut Hints Service Unit Tests
 * 
 * 需求 19.4: THE DataTeaCup SHALL 显示当前页面可用的快捷键列表
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import {
  ShortcutHintsService,
  createShortcutHintsService,
  SHORTCUT_CATEGORIES
} from '../ShortcutHintsService'
import type { ShortcutItem, ShortcutCategory } from '../shortcutHintsTypes'

describe('ShortcutHintsService', () => {
  let service: ShortcutHintsService

  beforeEach(() => {
    // 创建新的服务实例以隔离测试
    service = createShortcutHintsService()
  })

  describe('初始化', () => {
    it('应该正确初始化分类', () => {
      const categories = service.getCategories()
      expect(categories.length).toBe(SHORTCUT_CATEGORIES.length)
      expect(categories.map(c => c.id)).toContain('navigation')
      expect(categories.map(c => c.id)).toContain('editing')
      expect(categories.map(c => c.id)).toContain('file')
      expect(categories.map(c => c.id)).toContain('view')
      expect(categories.map(c => c.id)).toContain('help')
      expect(categories.map(c => c.id)).toContain('custom')
    })

    it('应该预注册全局快捷键', () => {
      const shortcuts = service.getAllShortcuts()
      expect(shortcuts.length).toBeGreaterThan(0)
      
      // 检查一些预设快捷键
      const saveShortcut = shortcuts.find(s => s.id === 'save')
      expect(saveShortcut).toBeDefined()
      expect(saveShortcut?.key).toBe('Ctrl+S')
      
      const undoShortcut = shortcuts.find(s => s.id === 'undo')
      expect(undoShortcut).toBeDefined()
      expect(undoShortcut?.key).toBe('Ctrl+Z')
    })

    it('应该初始化为关闭状态', () => {
      const state = service.getState()
      expect(state.isOpen).toBe(false)
      expect(state.selectedCategory).toBeNull()
      expect(state.searchQuery).toBe('')
      expect(state.currentScope).toBeNull()
    })
  })

  describe('快捷键注册', () => {
    it('应该能注册新的快捷键', () => {
      const customShortcut: ShortcutItem = {
        id: 'custom-test',
        key: 'Ctrl+Shift+T',
        description: '测试快捷键',
        category: 'custom',
        scope: 'test-scope'
      }

      service.registerShortcut(customShortcut)
      
      const shortcuts = service.getAllShortcuts()
      const registered = shortcuts.find(s => s.id === 'custom-test')
      expect(registered).toBeDefined()
      expect(registered?.key).toBe('Ctrl+Shift+T')
      expect(registered?.description).toBe('测试快捷键')
    })

    it('应该能批量注册快捷键', () => {
      const customShortcuts: ShortcutItem[] = [
        {
          id: 'batch-1',
          key: 'Ctrl+1',
          description: '批量1',
          category: 'custom'
        },
        {
          id: 'batch-2',
          key: 'Ctrl+2',
          description: '批量2',
          category: 'custom'
        }
      ]

      service.registerShortcuts(customShortcuts)
      
      const shortcuts = service.getAllShortcuts()
      expect(shortcuts.find(s => s.id === 'batch-1')).toBeDefined()
      expect(shortcuts.find(s => s.id === 'batch-2')).toBeDefined()
    })

    it('应该拒绝无效的快捷键配置', () => {
      const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})
      
      // 缺少 id
      service.registerShortcut({
        id: '',
        key: 'Ctrl+X',
        description: '无效',
        category: 'custom'
      })
      
      expect(consoleSpy).toHaveBeenCalled()
      consoleSpy.mockRestore()
    })

    it('应该能注销快捷键', () => {
      const customShortcut: ShortcutItem = {
        id: 'to-remove',
        key: 'Ctrl+R',
        description: '待删除',
        category: 'custom'
      }

      service.registerShortcut(customShortcut)
      expect(service.getAllShortcuts().find(s => s.id === 'to-remove')).toBeDefined()
      
      service.unregisterShortcut('to-remove')
      expect(service.getAllShortcuts().find(s => s.id === 'to-remove')).toBeUndefined()
    })
  })

  describe('分类筛选', () => {
    it('应该能按分类获取快捷键', () => {
      const editingShortcuts = service.getShortcutsByCategory('editing')
      expect(editingShortcuts.length).toBeGreaterThan(0)
      editingShortcuts.forEach(s => {
        expect(s.category).toBe('editing')
      })
    })

    it('应该返回空数组对于无效分类', () => {
      const shortcuts = service.getShortcutsByCategory('invalid' as ShortcutCategory)
      expect(shortcuts).toEqual([])
    })

    it('应该能获取分类信息', () => {
      const editingInfo = service.getCategoryInfo('editing')
      expect(editingInfo).toBeDefined()
      expect(editingInfo?.name).toBe('编辑')
    })
  })

  describe('范围筛选', () => {
    it('应该能按范围获取快捷键', () => {
      // 注册一个特定范围的快捷键
      service.registerShortcut({
        id: 'scoped-test',
        key: 'Ctrl+T',
        description: '范围测试',
        category: 'custom',
        scope: 'test-page'
      })

      const scopedShortcuts = service.getShortcutsByScope('test-page')
      
      // 应该包含全局快捷键和特定范围的快捷键
      expect(scopedShortcuts.find(s => s.id === 'scoped-test')).toBeDefined()
      expect(scopedShortcuts.find(s => s.id === 'save')).toBeDefined() // 全局快捷键
    })

    it('应该能设置当前范围', () => {
      service.setCurrentScope('my-page')
      expect(service.getState().currentScope).toBe('my-page')
      
      service.setCurrentScope(null)
      expect(service.getState().currentScope).toBeNull()
    })
  })

  describe('搜索功能', () => {
    it('应该能搜索快捷键描述', () => {
      const results = service.search('保存')
      expect(results.find(s => s.id === 'save')).toBeDefined()
    })

    it('应该能搜索快捷键组合', () => {
      const results = service.search('Ctrl+S')
      expect(results.find(s => s.id === 'save')).toBeDefined()
    })

    it('应该返回所有快捷键当搜索为空', () => {
      const results = service.search('')
      expect(results.length).toBe(service.getAllShortcuts().length)
    })

    it('应该返回空数组当没有匹配', () => {
      const results = service.search('不存在的快捷键xyz123')
      expect(results.length).toBe(0)
    })
  })

  describe('状态管理', () => {
    it('应该能打开和关闭面板', () => {
      expect(service.getState().isOpen).toBe(false)
      
      service.open()
      expect(service.getState().isOpen).toBe(true)
      
      service.close()
      expect(service.getState().isOpen).toBe(false)
    })

    it('应该能切换面板状态', () => {
      expect(service.getState().isOpen).toBe(false)
      
      service.toggle()
      expect(service.getState().isOpen).toBe(true)
      
      service.toggle()
      expect(service.getState().isOpen).toBe(false)
    })

    it('关闭时应该重置搜索和分类', () => {
      service.open()
      service.setSearchQuery('test')
      service.selectCategory('editing')
      
      expect(service.getState().searchQuery).toBe('test')
      expect(service.getState().selectedCategory).toBe('editing')
      
      service.close()
      
      expect(service.getState().searchQuery).toBe('')
      expect(service.getState().selectedCategory).toBeNull()
    })

    it('应该能选择分类', () => {
      service.selectCategory('editing')
      expect(service.getState().selectedCategory).toBe('editing')
      
      service.selectCategory(null)
      expect(service.getState().selectedCategory).toBeNull()
    })

    it('应该能设置和清除搜索', () => {
      service.setSearchQuery('test')
      expect(service.getState().searchQuery).toBe('test')
      
      service.clearSearch()
      expect(service.getState().searchQuery).toBe('')
    })
  })

  describe('状态变更回调', () => {
    it('应该在状态变更时触发回调', () => {
      const callback = vi.fn()
      service.setOnStateChange(callback)
      
      service.open()
      expect(callback).toHaveBeenCalledWith(expect.objectContaining({ isOpen: true }))
      
      service.close()
      expect(callback).toHaveBeenCalledWith(expect.objectContaining({ isOpen: false }))
    })
  })

  describe('快捷键格式化', () => {
    it('应该正确格式化快捷键', () => {
      // 基本格式化测试
      const formatted = service.formatShortcut('Ctrl+S')
      expect(formatted).toBeTruthy()
      // 格式化结果取决于平台，这里只验证返回非空字符串
    })
  })

  describe('禁用的快捷键', () => {
    it('应该过滤掉禁用的快捷键', () => {
      service.registerShortcut({
        id: 'disabled-test',
        key: 'Ctrl+D',
        description: '禁用测试',
        category: 'custom',
        enabled: false
      })

      const shortcuts = service.getAllShortcuts()
      expect(shortcuts.find(s => s.id === 'disabled-test')).toBeUndefined()
    })
  })

  describe('排序', () => {
    it('应该按 order 排序快捷键', () => {
      service.registerShortcut({
        id: 'order-3',
        key: 'Ctrl+3',
        description: '顺序3',
        category: 'custom',
        order: 3
      })
      
      service.registerShortcut({
        id: 'order-1',
        key: 'Ctrl+1',
        description: '顺序1',
        category: 'custom',
        order: 1
      })
      
      service.registerShortcut({
        id: 'order-2',
        key: 'Ctrl+2',
        description: '顺序2',
        category: 'custom',
        order: 2
      })

      const customShortcuts = service.getShortcutsByCategory('custom')
      const orderIds = customShortcuts
        .filter(s => s.id.startsWith('order-'))
        .map(s => s.id)
      
      expect(orderIds).toEqual(['order-1', 'order-2', 'order-3'])
    })
  })
})


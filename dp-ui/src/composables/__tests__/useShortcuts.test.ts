import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import {
  useShortcuts,
  parseShortcutKey,
  normalizeShortcutKey,
  matchShortcut,
  formatShortcutForDisplay,
  resetShortcutIdCounter,
  COMMON_SHORTCUTS
} from '../useShortcuts'

// 测试组件
const createTestComponent = (options = {}) => {
  return defineComponent({
    setup() {
      const shortcuts = useShortcuts(options)
      return { shortcuts }
    },
    template: '<div>Test</div>'
  })
}

describe('useShortcuts', () => {
  beforeEach(() => {
    resetShortcutIdCounter()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('parseShortcutKey', () => {
    it('应该正确解析简单快捷键', () => {
      const result = parseShortcutKey('Escape')
      expect(result.key).toBe('escape')
      expect(result.modifiers.ctrl).toBe(false)
      expect(result.modifiers.shift).toBe(false)
      expect(result.modifiers.alt).toBe(false)
      expect(result.modifiers.meta).toBe(false)
    })

    it('应该正确解析 Ctrl+S', () => {
      const result = parseShortcutKey('Ctrl+S')
      expect(result.key).toBe('s')
      expect(result.modifiers.ctrl).toBe(true)
      expect(result.modifiers.shift).toBe(false)
    })

    it('应该正确解析 Ctrl+Shift+Z', () => {
      const result = parseShortcutKey('Ctrl+Shift+Z')
      expect(result.key).toBe('z')
      expect(result.modifiers.ctrl).toBe(true)
      expect(result.modifiers.shift).toBe(true)
    })

    it('应该正确解析 Alt+Enter', () => {
      const result = parseShortcutKey('Alt+Enter')
      expect(result.key).toBe('enter')
      expect(result.modifiers.alt).toBe(true)
    })

    it('应该正确解析 Meta/Cmd 修饰符', () => {
      const result1 = parseShortcutKey('Meta+C')
      expect(result1.modifiers.meta).toBe(true)

      const result2 = parseShortcutKey('Cmd+C')
      expect(result2.modifiers.meta).toBe(true)

      const result3 = parseShortcutKey('Command+C')
      expect(result3.modifiers.meta).toBe(true)
    })
  })

  describe('normalizeShortcutKey', () => {
    it('应该生成标准化的快捷键字符串', () => {
      const parsed = parseShortcutKey('Ctrl+Shift+S')
      const normalized = normalizeShortcutKey(parsed)
      expect(normalized).toBe('ctrl+shift+s')
    })

    it('应该按固定顺序排列修饰符', () => {
      const parsed1 = parseShortcutKey('Shift+Ctrl+A')
      const parsed2 = parseShortcutKey('Ctrl+Shift+A')
      expect(normalizeShortcutKey(parsed1)).toBe(normalizeShortcutKey(parsed2))
    })
  })

  describe('matchShortcut', () => {
    it('应该正确匹配 Ctrl+S', () => {
      const parsed = parseShortcutKey('Ctrl+S')
      const event = new KeyboardEvent('keydown', {
        key: 's',
        ctrlKey: true,
        shiftKey: false,
        altKey: false,
        metaKey: false
      })
      expect(matchShortcut(event, parsed)).toBe(true)
    })

    it('应该正确匹配 Escape', () => {
      const parsed = parseShortcutKey('Escape')
      const event = new KeyboardEvent('keydown', {
        key: 'Escape',
        ctrlKey: false,
        shiftKey: false,
        altKey: false,
        metaKey: false
      })
      expect(matchShortcut(event, parsed)).toBe(true)
    })

    it('应该正确匹配 Delete', () => {
      const parsed = parseShortcutKey('Delete')
      const event = new KeyboardEvent('keydown', {
        key: 'Delete',
        ctrlKey: false,
        shiftKey: false,
        altKey: false,
        metaKey: false
      })
      expect(matchShortcut(event, parsed)).toBe(true)
    })

    it('不应该匹配错误的修饰符', () => {
      const parsed = parseShortcutKey('Ctrl+S')
      const event = new KeyboardEvent('keydown', {
        key: 's',
        ctrlKey: false, // 缺少 Ctrl
        shiftKey: false,
        altKey: false,
        metaKey: false
      })
      expect(matchShortcut(event, parsed)).toBe(false)
    })

    it('不应该匹配额外的修饰符', () => {
      const parsed = parseShortcutKey('Ctrl+S')
      const event = new KeyboardEvent('keydown', {
        key: 's',
        ctrlKey: true,
        shiftKey: true, // 额外的 Shift
        altKey: false,
        metaKey: false
      })
      expect(matchShortcut(event, parsed)).toBe(false)
    })
  })

  describe('formatShortcutForDisplay', () => {
    it('应该格式化快捷键用于显示', () => {
      // 在非 Mac 环境下
      const formatted = formatShortcutForDisplay('Ctrl+S')
      expect(formatted).toContain('Ctrl')
      expect(formatted).toContain('S')
    })
  })

  describe('COMMON_SHORTCUTS', () => {
    it('应该包含常用快捷键定义', () => {
      expect(COMMON_SHORTCUTS.SAVE).toBe('Ctrl+S')
      expect(COMMON_SHORTCUTS.UNDO).toBe('Ctrl+Z')
      expect(COMMON_SHORTCUTS.REDO).toBe('Ctrl+Y')
      expect(COMMON_SHORTCUTS.REDO_ALT).toBe('Ctrl+Shift+Z')
      expect(COMMON_SHORTCUTS.COPY).toBe('Ctrl+C')
      expect(COMMON_SHORTCUTS.PASTE).toBe('Ctrl+V')
      expect(COMMON_SHORTCUTS.DELETE).toBe('Delete')
      expect(COMMON_SHORTCUTS.ESCAPE).toBe('Escape')
    })
  })

  describe('useShortcuts composable', () => {
    it('应该注册和注销快捷键', async () => {
      const handler = vi.fn()
      const wrapper = mount(createTestComponent())
      const { shortcuts } = wrapper.vm

      const id = shortcuts.register({
        key: 'Ctrl+S',
        handler,
        description: '保存'
      })

      expect(id).toBeTruthy()
      expect(shortcuts.shortcuts.value.length).toBe(1)
      expect(shortcuts.isRegistered('Ctrl+S')).toBe(true)

      const result = shortcuts.unregister(id)
      expect(result).toBe(true)
      expect(shortcuts.shortcuts.value.length).toBe(0)
      expect(shortcuts.isRegistered('Ctrl+S')).toBe(false)

      wrapper.unmount()
    })

    it('应该批量注册快捷键', async () => {
      const wrapper = mount(createTestComponent())
      const { shortcuts } = wrapper.vm

      const ids = shortcuts.registerMany([
        { key: 'Ctrl+S', handler: vi.fn(), description: '保存' },
        { key: 'Ctrl+Z', handler: vi.fn(), description: '撤销' },
        { key: 'Ctrl+Y', handler: vi.fn(), description: '重做' }
      ])

      expect(ids.length).toBe(3)
      expect(shortcuts.shortcuts.value.length).toBe(3)

      wrapper.unmount()
    })

    it('应该获取快捷键列表', async () => {
      const wrapper = mount(createTestComponent())
      const { shortcuts } = wrapper.vm

      shortcuts.registerMany([
        { key: 'Ctrl+S', handler: vi.fn(), description: '保存' },
        { key: 'Ctrl+Z', handler: vi.fn(), description: '撤销' }
      ])

      const list = shortcuts.getShortcutList()
      expect(list.length).toBe(2)
      expect(list[0]!.description).toBe('保存')
      expect(list[1]!.description).toBe('撤销')

      wrapper.unmount()
    })

    it('应该在组件卸载时清理快捷键', async () => {
      const wrapper = mount(createTestComponent())
      const { shortcuts } = wrapper.vm

      shortcuts.register({
        key: 'Ctrl+S',
        handler: vi.fn(),
        description: '保存'
      })

      expect(shortcuts.shortcuts.value.length).toBe(1)

      wrapper.unmount()

      // 卸载后快捷键应该被清理
      expect(shortcuts.shortcuts.value.length).toBe(0)
    })

    it('应该支持启用和禁用', async () => {
      const wrapper = mount(createTestComponent({ autoEnable: false }))
      const { shortcuts } = wrapper.vm

      expect(shortcuts.enabled.value).toBe(false)

      shortcuts.enable()
      expect(shortcuts.enabled.value).toBe(true)

      shortcuts.disable()
      expect(shortcuts.enabled.value).toBe(false)

      wrapper.unmount()
    })

    it('应该检测快捷键冲突', async () => {
      const onConflict = vi.fn()
      const wrapper = mount(createTestComponent({ detectConflicts: true, onConflict }))
      const { shortcuts } = wrapper.vm

      shortcuts.register({
        key: 'Ctrl+S',
        handler: vi.fn(),
        description: '保存1'
      })

      shortcuts.register({
        key: 'Ctrl+S',
        handler: vi.fn(),
        description: '保存2'
      })

      expect(onConflict).toHaveBeenCalled()
      expect(onConflict.mock.calls[0][0].key).toBe('Ctrl+S')

      wrapper.unmount()
    })

    it('应该注销所有快捷键', async () => {
      const wrapper = mount(createTestComponent())
      const { shortcuts } = wrapper.vm

      shortcuts.registerMany([
        { key: 'Ctrl+S', handler: vi.fn() },
        { key: 'Ctrl+Z', handler: vi.fn() },
        { key: 'Ctrl+Y', handler: vi.fn() }
      ])

      expect(shortcuts.shortcuts.value.length).toBe(3)

      shortcuts.unregisterAll()
      expect(shortcuts.shortcuts.value.length).toBe(0)

      wrapper.unmount()
    })
  })
})

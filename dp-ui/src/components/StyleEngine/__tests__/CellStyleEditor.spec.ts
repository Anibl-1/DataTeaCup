/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * CellStyleEditor 单元测试
 * Unit tests for CellStyleEditor component
 * 
 * 验证需求: 14.2.6, 14.2.7, 14.2.8, 14.2.9, 14.2.10
 */

import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import CellStyleEditor from '../CellStyleEditor.vue'
import type { CellStyle } from '../types'

// Mock naive-ui components
vi.mock('naive-ui', () => ({
  NIcon: { template: '<span><slot /></span>' },
  NTabs: { template: '<div><slot /></div>', props: ['value'] },
  NTabPane: { template: '<div><slot /></div>', props: ['name', 'tab'] },
  NForm: { template: '<form><slot /></form>', props: ['labelPlacement', 'labelWidth', 'size'] },
  NFormItem: { template: '<div><slot /></div>', props: ['label'] },
  NSelect: { 
    template: '<select @change="$emit(\'update:value\', $event.target.value)"><slot /></select>',
    props: ['value', 'options', 'placeholder', 'size'],
    emits: ['update:value']
  },
  NInputNumber: {
    template: '<input type="number" :value="value" @input="$emit(\'update:value\', Number($event.target.value))" />',
    props: ['value', 'min', 'max', 'placeholder', 'size', 'step'],
    emits: ['update:value']
  },
  NColorPicker: {
    template: '<input type="color" :value="value" @input="$emit(\'update:value\', $event.target.value)" />',
    props: ['value', 'showAlpha', 'modes', 'size'],
    emits: ['update:value']
  },
  NRadioGroup: {
    template: '<div @click="handleClick"><slot /></div>',
    props: ['value'],
    emits: ['update:value'],
    methods: {
      handleClick(e: Event) {
        const target = e.target as HTMLElement
        if (target.dataset.value) {
          this.$emit('update:value', target.dataset.value)
        }
      }
    }
  },
  NRadioButton: { template: '<button :data-value="value"><slot /></button>', props: ['value'] },
  NSwitch: {
    template: '<input type="checkbox" :checked="value" @change="$emit(\'update:value\', $event.target.checked)" />',
    props: ['value'],
    emits: ['update:value']
  },
  NSlider: {
    template: '<input type="range" :value="value" :min="min" :max="max" @input="$emit(\'update:value\', Number($event.target.value))" />',
    props: ['value', 'min', 'max', 'step', 'tooltip'],
    emits: ['update:value']
  },
  NButton: { template: '<button @click="$emit(\'click\')"><slot /></button>', props: ['size', 'type', 'dashed', 'text'], emits: ['click'] },
  NTooltip: { template: '<div><slot /><slot name="trigger" /></div>', props: ['trigger'] },
  NDivider: { template: '<hr />' },
  NCheckbox: {
    template: '<input type="checkbox" :checked="checked" @change="$emit(\'update:checked\', $event.target.checked)" />',
    props: ['checked'],
    emits: ['update:checked']
  },
}))

// Mock icons
vi.mock('@vicons/ionicons5', () => ({
  ColorPaletteOutline: { template: '<span>icon</span>' },
  HelpCircleOutline: { template: '<span>icon</span>' },
  EyeOutline: { template: '<span>icon</span>' },
  AddOutline: { template: '<span>icon</span>' },
  CloseOutline: { template: '<span>icon</span>' },
  ReorderTwoOutline: { template: '<span>icon</span>' },
  RemoveOutline: { template: '<span>icon</span>' },
  ChevronUpOutline: { template: '<span>icon</span>' },
  ChevronDownOutline: { template: '<span>icon</span>' },
  ChevronBackOutline: { template: '<span>icon</span>' },
  ChevronForwardOutline: { template: '<span>icon</span>' },
}))

describe('CellStyleEditor', () => {
  /**
   * 测试组件渲染
   * Validates: 14.2.6, 14.2.7, 14.2.8, 14.2.9, 14.2.10
   */
  describe('Component Rendering', () => {
    it('should render the editor with all sections', () => {
      const wrapper = mount(CellStyleEditor)
      
      expect(wrapper.find('.cell-style-editor').exists()).toBe(true)
      expect(wrapper.find('.editor-header').exists()).toBe(true)
      expect(wrapper.find('.preview-section').exists()).toBe(true)
      expect(wrapper.find('.editor-actions').exists()).toBe(true)
    })

    it('should display preview text', () => {
      const previewText = '测试文本'
      const wrapper = mount(CellStyleEditor, {
        props: { previewText }
      })
      
      expect(wrapper.find('.preview-cell').text()).toBe(previewText)
    })

    it('should use default preview text when not provided', () => {
      const wrapper = mount(CellStyleEditor)
      
      expect(wrapper.find('.preview-cell').text()).toBe('示例文本 Sample Text 12345')
    })
  })

  /**
   * 测试字体配置
   * Validates: 14.2.6 - 支持配置单元格字体（字体族、大小、粗细、斜体、下划线、删除线）
   */
  describe('Font Configuration (14.2.6)', () => {
    it('should initialize with default font settings', () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      expect(vm.localStyle.font.family).toBe('Microsoft YaHei')
      expect(vm.localStyle.font.size).toBe(14)
      expect(vm.localStyle.font.weight).toBe('normal')
      expect(vm.localStyle.font.style).toBe('normal')
      expect(vm.localStyle.font.decoration).toBe('none')
      expect(vm.localStyle.font.color).toBe('#333333')
    })

    it('should accept external font style via modelValue', async () => {
      const fontStyle: CellStyle = {
        font: {
          family: 'Arial',
          size: 16,
          weight: 'bold',
          style: 'italic',
          decoration: 'underline',
          color: '#ff0000'
        }
      }
      
      const wrapper = mount(CellStyleEditor, {
        props: { modelValue: fontStyle }
      })
      
      await nextTick()
      const vm = wrapper.vm as any
      
      expect(vm.localStyle.font.family).toBe('Arial')
      expect(vm.localStyle.font.size).toBe(16)
      expect(vm.localStyle.font.weight).toBe('bold')
      expect(vm.localStyle.font.style).toBe('italic')
      expect(vm.localStyle.font.decoration).toBe('underline')
      expect(vm.localStyle.font.color).toBe('#ff0000')
    })

    it('should emit update when font settings change', async () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      vm.localStyle.font.size = 20
      vm.emitUpdate()
      
      await nextTick()
      
      const emitted = wrapper.emitted('update:modelValue')
      expect(emitted).toBeTruthy()
      expect(emitted![0][0]).toMatchObject({
        font: expect.objectContaining({ size: 20 })
      })
    })
  })

  /**
   * 测试对齐配置
   * Validates: 14.2.7 - 支持配置单元格对齐方式（水平对齐、垂直对齐、文字方向、缩进）
   */
  describe('Alignment Configuration (14.2.7)', () => {
    it('should initialize with default alignment settings', () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      expect(vm.localStyle.alignment.horizontal).toBe('left')
      expect(vm.localStyle.alignment.vertical).toBe('middle')
      expect(vm.localStyle.alignment.textDirection).toBe('ltr')
      expect(vm.localStyle.alignment.indent).toBe(0)
      expect(vm.localStyle.alignment.wrapText).toBe(false)
      expect(vm.localStyle.alignment.shrinkToFit).toBe(false)
      expect(vm.localStyle.alignment.rotation).toBe(0)
    })

    it('should accept external alignment style via modelValue', async () => {
      const alignmentStyle: CellStyle = {
        alignment: {
          horizontal: 'center',
          vertical: 'top',
          textDirection: 'rtl',
          indent: 2,
          wrapText: true,
          shrinkToFit: true,
          rotation: 45
        }
      }
      
      const wrapper = mount(CellStyleEditor, {
        props: { modelValue: alignmentStyle }
      })
      
      await nextTick()
      const vm = wrapper.vm as any
      
      expect(vm.localStyle.alignment.horizontal).toBe('center')
      expect(vm.localStyle.alignment.vertical).toBe('top')
      expect(vm.localStyle.alignment.textDirection).toBe('rtl')
      expect(vm.localStyle.alignment.indent).toBe(2)
      expect(vm.localStyle.alignment.wrapText).toBe(true)
      expect(vm.localStyle.alignment.shrinkToFit).toBe(true)
      expect(vm.localStyle.alignment.rotation).toBe(45)
    })

    it('should emit update when alignment settings change', async () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      vm.localStyle.alignment.horizontal = 'right'
      vm.localStyle.alignment.vertical = 'bottom'
      vm.emitUpdate()
      
      await nextTick()
      
      const emitted = wrapper.emitted('update:modelValue')
      expect(emitted).toBeTruthy()
      expect(emitted![0][0]).toMatchObject({
        alignment: expect.objectContaining({
          horizontal: 'right',
          vertical: 'bottom'
        })
      })
    })
  })

  /**
   * 测试边框配置
   * Validates: 14.2.8 - 支持配置单元格边框（样式、颜色、宽度，支持单边设置）
   */
  describe('Border Configuration (14.2.8)', () => {
    it('should initialize with default border settings (no border)', () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      for (const side of ['top', 'right', 'bottom', 'left']) {
        expect(vm.localStyle.border[side].style).toBe('none')
        expect(vm.localStyle.border[side].width).toBe(1)
        expect(vm.localStyle.border[side].color).toBe('#000000')
      }
    })

    it('should accept external border style via modelValue', async () => {
      const borderStyle: CellStyle = {
        border: {
          top: { style: 'solid', width: 2, color: '#ff0000' },
          right: { style: 'dashed', width: 1, color: '#00ff00' },
          bottom: { style: 'dotted', width: 3, color: '#0000ff' },
          left: { style: 'double', width: 2, color: '#ffff00' }
        }
      }
      
      const wrapper = mount(CellStyleEditor, {
        props: { modelValue: borderStyle }
      })
      
      await nextTick()
      const vm = wrapper.vm as any
      
      expect(vm.localStyle.border.top.style).toBe('solid')
      expect(vm.localStyle.border.top.width).toBe(2)
      expect(vm.localStyle.border.top.color).toBe('#ff0000')
      expect(vm.localStyle.border.right.style).toBe('dashed')
      expect(vm.localStyle.border.bottom.style).toBe('dotted')
      expect(vm.localStyle.border.left.style).toBe('double')
    })

    it('should apply border to all sides using "all" shortcut', async () => {
      const borderStyle: CellStyle = {
        border: {
          all: { style: 'solid', width: 1, color: '#333333' }
        }
      }
      
      const wrapper = mount(CellStyleEditor, {
        props: { modelValue: borderStyle }
      })
      
      await nextTick()
      const vm = wrapper.vm as any
      
      for (const side of ['top', 'right', 'bottom', 'left']) {
        expect(vm.localStyle.border[side].style).toBe('solid')
        expect(vm.localStyle.border[side].width).toBe(1)
        expect(vm.localStyle.border[side].color).toBe('#333333')
      }
    })

    it('should set all borders when setBorderAll is called', async () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      vm.setBorderAll()
      await nextTick()
      
      for (const side of ['top', 'right', 'bottom', 'left']) {
        expect(vm.localStyle.border[side].style).toBe('solid')
        expect(vm.localStyle.border[side].width).toBe(1)
        expect(vm.localStyle.border[side].color).toBe('#000000')
      }
    })

    it('should clear all borders when clearBorder is called', async () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      // First set borders
      vm.setBorderAll()
      await nextTick()
      
      // Then clear
      vm.clearBorder()
      await nextTick()
      
      for (const side of ['top', 'right', 'bottom', 'left']) {
        expect(vm.localStyle.border[side].style).toBe('none')
      }
    })
  })

  /**
   * 测试背景配置
   * Validates: 14.2.9 - 支持配置单元格背景（纯色、渐变色、图案填充）
   */
  describe('Background Configuration (14.2.9)', () => {
    it('should initialize with default solid background', () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      expect(vm.localStyle.background.type).toBe('solid')
      expect(vm.localStyle.background.color).toBe('#ffffff')
    })

    it('should accept solid background via modelValue', async () => {
      const bgStyle: CellStyle = {
        background: {
          type: 'solid',
          color: '#f0f0f0'
        }
      }
      
      const wrapper = mount(CellStyleEditor, {
        props: { modelValue: bgStyle }
      })
      
      await nextTick()
      const vm = wrapper.vm as any
      
      expect(vm.localStyle.background.type).toBe('solid')
      expect(vm.localStyle.background.color).toBe('#f0f0f0')
    })

    it('should accept gradient background via modelValue', async () => {
      const bgStyle: CellStyle = {
        background: {
          type: 'gradient',
          gradient: {
            type: 'linear',
            angle: 45,
            colors: [
              { offset: 0, color: '#ff0000' },
              { offset: 0.5, color: '#00ff00' },
              { offset: 1, color: '#0000ff' }
            ]
          }
        }
      }
      
      const wrapper = mount(CellStyleEditor, {
        props: { modelValue: bgStyle }
      })
      
      await nextTick()
      const vm = wrapper.vm as any
      
      expect(vm.localStyle.background.type).toBe('gradient')
      expect(vm.localStyle.background.gradient.type).toBe('linear')
      expect(vm.localStyle.background.gradient.angle).toBe(45)
      expect(vm.localStyle.background.gradient.colors).toHaveLength(3)
    })

    it('should accept pattern background via modelValue', async () => {
      const bgStyle: CellStyle = {
        background: {
          type: 'pattern',
          pattern: {
            type: 'dots',
            foreground: '#cccccc',
            background: '#ffffff'
          }
        }
      }
      
      const wrapper = mount(CellStyleEditor, {
        props: { modelValue: bgStyle }
      })
      
      await nextTick()
      const vm = wrapper.vm as any
      
      expect(vm.localStyle.background.type).toBe('pattern')
      expect(vm.localStyle.background.pattern.type).toBe('dots')
      expect(vm.localStyle.background.pattern.foreground).toBe('#cccccc')
      expect(vm.localStyle.background.pattern.background).toBe('#ffffff')
    })

    it('should add gradient stop when addGradientStop is called', async () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      const initialCount = vm.localStyle.background.gradient.colors.length
      vm.addGradientStop()
      await nextTick()
      
      expect(vm.localStyle.background.gradient.colors.length).toBe(initialCount + 1)
    })

    it('should remove gradient stop when removeGradientStop is called', async () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      // Add a stop first to have more than 2
      vm.addGradientStop()
      await nextTick()
      
      const countAfterAdd = vm.localStyle.background.gradient.colors.length
      vm.removeGradientStop(1)
      await nextTick()
      
      expect(vm.localStyle.background.gradient.colors.length).toBe(countAfterAdd - 1)
    })

    it('should not remove gradient stop if only 2 remain', async () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      // Ensure we have exactly 2 stops
      vm.localStyle.background.gradient.colors = [
        { offset: 0, color: '#ffffff' },
        { offset: 1, color: '#000000' }
      ]
      
      vm.removeGradientStop(0)
      await nextTick()
      
      expect(vm.localStyle.background.gradient.colors.length).toBe(2)
    })
  })

  /**
   * 测试内边距配置
   * Validates: 14.2.10 - 支持配置单元格内边距（上下左右独立设置）
   */
  describe('Padding Configuration (14.2.10)', () => {
    it('should initialize with default padding settings', () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      expect(vm.localStyle.padding.top).toBe(4)
      expect(vm.localStyle.padding.right).toBe(8)
      expect(vm.localStyle.padding.bottom).toBe(4)
      expect(vm.localStyle.padding.left).toBe(8)
    })

    it('should accept external padding style via modelValue', async () => {
      const paddingStyle: CellStyle = {
        padding: {
          top: 10,
          right: 20,
          bottom: 15,
          left: 25
        }
      }
      
      const wrapper = mount(CellStyleEditor, {
        props: { modelValue: paddingStyle }
      })
      
      await nextTick()
      const vm = wrapper.vm as any
      
      expect(vm.localStyle.padding.top).toBe(10)
      expect(vm.localStyle.padding.right).toBe(20)
      expect(vm.localStyle.padding.bottom).toBe(15)
      expect(vm.localStyle.padding.left).toBe(25)
    })

    it('should link all padding values when linkPadding is enabled', async () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      vm.localStyle.padding.top = 16
      vm.linkPadding = true
      vm.onLinkPaddingChange(true)
      await nextTick()
      
      expect(vm.localStyle.padding.top).toBe(16)
      expect(vm.localStyle.padding.right).toBe(16)
      expect(vm.localStyle.padding.bottom).toBe(16)
      expect(vm.localStyle.padding.left).toBe(16)
    })

    it('should update all padding values when linked and one changes', async () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      vm.linkPadding = true
      vm.onPaddingChange('top', 20)
      await nextTick()
      
      expect(vm.localStyle.padding.top).toBe(20)
      expect(vm.localStyle.padding.right).toBe(20)
      expect(vm.localStyle.padding.bottom).toBe(20)
      expect(vm.localStyle.padding.left).toBe(20)
    })

    it('should allow independent padding values when not linked', async () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      vm.linkPadding = false
      vm.localStyle.padding.top = 10
      vm.localStyle.padding.right = 20
      vm.localStyle.padding.bottom = 30
      vm.localStyle.padding.left = 40
      vm.emitUpdate()
      await nextTick()
      
      const emitted = wrapper.emitted('update:modelValue')
      expect(emitted).toBeTruthy()
      expect(emitted![0][0]).toMatchObject({
        padding: {
          top: 10,
          right: 20,
          bottom: 30,
          left: 40
        }
      })
    })
  })

  /**
   * 测试实时预览
   * Validates: 样式实时预览功能
   */
  describe('Real-time Preview', () => {
    it('should compute preview style based on font settings', () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      vm.localStyle.font.family = 'Arial'
      vm.localStyle.font.size = 20
      vm.localStyle.font.weight = 'bold'
      vm.localStyle.font.color = '#ff0000'
      
      const previewStyle = vm.previewStyle
      
      expect(previewStyle.fontFamily).toBe('Arial')
      expect(previewStyle.fontSize).toBe('20px')
      expect(previewStyle.fontWeight).toBe('bold')
      expect(previewStyle.color).toBe('#ff0000')
    })

    it('should compute preview style based on alignment settings', () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      vm.localStyle.alignment.horizontal = 'center'
      vm.localStyle.alignment.vertical = 'top'
      vm.localStyle.alignment.rotation = 45
      
      const previewStyle = vm.previewStyle
      
      expect(previewStyle.textAlign).toBe('center')
      expect(previewStyle.alignItems).toBe('flex-start')
      expect(previewStyle.transform).toBe('rotate(45deg)')
    })

    it('should compute preview style based on border settings', () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      vm.localStyle.border.top = { style: 'solid', width: 2, color: '#ff0000' }
      
      const previewStyle = vm.previewStyle
      
      expect(previewStyle.borderTop).toBe('2px solid #ff0000')
    })

    it('should compute preview style for solid background', () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      vm.localStyle.background.type = 'solid'
      vm.localStyle.background.color = '#f0f0f0'
      
      const previewStyle = vm.previewStyle
      
      expect(previewStyle.backgroundColor).toBe('#f0f0f0')
    })

    it('should compute preview style for gradient background', () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      vm.localStyle.background.type = 'gradient'
      vm.localStyle.background.gradient = {
        type: 'linear',
        angle: 90,
        colors: [
          { offset: 0, color: '#ffffff' },
          { offset: 1, color: '#000000' }
        ]
      }
      
      const previewStyle = vm.previewStyle
      
      expect(previewStyle.background).toContain('linear-gradient')
      expect(previewStyle.background).toContain('90deg')
    })

    it('should compute preview style for pattern background', () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      vm.localStyle.background.type = 'pattern'
      vm.localStyle.background.pattern = {
        type: 'stripe',
        foreground: '#cccccc',
        background: '#ffffff'
      }
      
      const previewStyle = vm.previewStyle
      
      expect(previewStyle.backgroundColor).toBe('#ffffff')
      expect(previewStyle.backgroundImage).toContain('repeating-linear-gradient')
    })

    it('should compute preview style based on padding settings', () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      vm.localStyle.padding = { top: 10, right: 20, bottom: 30, left: 40 }
      
      const previewStyle = vm.previewStyle
      
      expect(previewStyle.padding).toBe('10px 20px 30px 40px')
    })
  })

  /**
   * 测试重置和应用功能
   */
  describe('Reset and Apply', () => {
    it('should reset all styles to default when resetStyle is called', async () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      // Modify styles
      vm.localStyle.font.size = 30
      vm.localStyle.alignment.horizontal = 'right'
      vm.localStyle.background.color = '#ff0000'
      vm.localStyle.padding.top = 50
      
      // Reset
      vm.resetStyle()
      await nextTick()
      
      // Verify defaults
      expect(vm.localStyle.font.size).toBe(14)
      expect(vm.localStyle.font.family).toBe('Microsoft YaHei')
      expect(vm.localStyle.alignment.horizontal).toBe('left')
      expect(vm.localStyle.background.type).toBe('solid')
      expect(vm.localStyle.background.color).toBe('#ffffff')
      expect(vm.localStyle.padding.top).toBe(4)
    })

    it('should emit apply event when applyStyle is called', async () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      vm.localStyle.font.size = 20
      vm.applyStyle()
      await nextTick()
      
      const emitted = wrapper.emitted('apply')
      expect(emitted).toBeTruthy()
      expect(emitted![0][0]).toMatchObject({
        font: expect.objectContaining({ size: 20 })
      })
    })
  })

  /**
   * 测试 buildCellStyle 函数
   */
  describe('buildCellStyle', () => {
    it('should build complete CellStyle object', () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      const style = vm.buildCellStyle()
      
      expect(style).toHaveProperty('font')
      expect(style).toHaveProperty('alignment')
      expect(style).toHaveProperty('border')
      expect(style).toHaveProperty('background')
      expect(style).toHaveProperty('padding')
    })

    it('should include gradient only when background type is gradient', () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      vm.localStyle.background.type = 'solid'
      let style = vm.buildCellStyle()
      expect(style.background.gradient).toBeUndefined()
      
      vm.localStyle.background.type = 'gradient'
      style = vm.buildCellStyle()
      expect(style.background.gradient).toBeDefined()
    })

    it('should include pattern only when background type is pattern', () => {
      const wrapper = mount(CellStyleEditor)
      const vm = wrapper.vm as any
      
      vm.localStyle.background.type = 'solid'
      let style = vm.buildCellStyle()
      expect(style.background.pattern).toBeUndefined()
      
      vm.localStyle.background.type = 'pattern'
      style = vm.buildCellStyle()
      expect(style.background.pattern).toBeDefined()
    })
  })
})

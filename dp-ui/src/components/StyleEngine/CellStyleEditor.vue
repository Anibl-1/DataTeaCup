<template>
  <div class="cell-style-editor">
    <!-- 编辑器头部 -->
    <div class="editor-header">
      <div class="editor-title">
        <n-icon size="18" color="var(--color-primary)"><ColorPaletteOutline /></n-icon>
        <span>单元格样式配置</span>
      </div>
      <n-tooltip trigger="hover">
        <template #trigger>
          <n-icon size="14" color="#94a3b8" style="cursor: help;"><HelpCircleOutline /></n-icon>
        </template>
        <div style="max-width: 280px; font-size: 12px;">
          <p style="margin: 0 0 8px 0;"><b>样式配置说明：</b></p>
          <p style="margin: 0 0 4px 0;">• <b>字体</b>：设置字体族、大小、粗细、样式等</p>
          <p style="margin: 0 0 4px 0;">• <b>对齐</b>：设置水平、垂直对齐和文字方向</p>
          <p style="margin: 0 0 4px 0;">• <b>边框</b>：设置边框样式、颜色、宽度</p>
          <p style="margin: 0 0 4px 0;">• <b>背景</b>：设置纯色、渐变或图案背景</p>
          <p style="margin: 0;">• <b>内边距</b>：设置上下左右内边距</p>
        </div>
      </n-tooltip>
    </div>

    <!-- 样式预览区域 -->
    <div class="preview-section">
      <div class="section-title">
        <n-icon size="14"><EyeOutline /></n-icon>
        <span>实时预览</span>
      </div>
      <div class="preview-container">
        <div class="preview-cell" :style="previewStyle">
          {{ previewText }}
        </div>
      </div>
    </div>

    <!-- 配置标签页 -->
    <n-tabs v-model:value="activeTab" type="line" size="small">
      <!-- 字体配置 -->
      <n-tab-pane name="font" tab="字体">
        <div class="config-section">
          <n-form label-placement="left" label-width="70" size="small">
            <n-form-item label="字体族">
              <n-select
                v-model:value="localStyle.font.family"
                :options="fontFamilyOptions"
                placeholder="选择字体"
                @update:value="emitUpdate"
              />
            </n-form-item>
            <n-form-item label="字号">
              <n-input-number
                v-model:value="localStyle.font.size"
                :min="8"
                :max="72"
                placeholder="字号"
                @update:value="emitUpdate"
              />
            </n-form-item>
            <n-form-item label="粗细">
              <n-select
                v-model:value="localStyle.font.weight"
                :options="fontWeightOptions"
                placeholder="选择粗细"
                @update:value="emitUpdate"
              />
            </n-form-item>
            <n-form-item label="样式">
              <n-select
                v-model:value="localStyle.font.style"
                :options="fontStyleOptions"
                placeholder="选择样式"
                @update:value="emitUpdate"
              />
            </n-form-item>
            <n-form-item label="装饰">
              <n-select
                v-model:value="localStyle.font.decoration"
                :options="fontDecorationOptions"
                placeholder="选择装饰"
                @update:value="emitUpdate"
              />
            </n-form-item>
            <n-form-item label="颜色">
              <n-color-picker
                v-model:value="localStyle.font.color"
                :show-alpha="false"
                :modes="['hex']"
                @update:value="emitUpdate"
              />
            </n-form-item>
          </n-form>
        </div>
      </n-tab-pane>

      <!-- 对齐配置 -->
      <n-tab-pane name="alignment" tab="对齐">
        <div class="config-section">
          <n-form label-placement="left" label-width="70" size="small">
            <n-form-item label="水平对齐">
              <n-radio-group v-model:value="localStyle.alignment.horizontal" @update:value="emitUpdate">
                <n-radio-button value="left">
                  <n-icon><AlignLeftOutline /></n-icon>
                </n-radio-button>
                <n-radio-button value="center">
                  <n-icon><AlignCenterOutline /></n-icon>
                </n-radio-button>
                <n-radio-button value="right">
                  <n-icon><AlignRightOutline /></n-icon>
                </n-radio-button>
                <n-radio-button value="justify">
                  <n-icon><AlignJustifyOutline /></n-icon>
                </n-radio-button>
              </n-radio-group>
            </n-form-item>
            <n-form-item label="垂直对齐">
              <n-radio-group v-model:value="localStyle.alignment.vertical" @update:value="emitUpdate">
                <n-radio-button value="top">顶部</n-radio-button>
                <n-radio-button value="middle">居中</n-radio-button>
                <n-radio-button value="bottom">底部</n-radio-button>
              </n-radio-group>
            </n-form-item>
            <n-form-item label="文字方向">
              <n-radio-group v-model:value="localStyle.alignment.textDirection" @update:value="emitUpdate">
                <n-radio-button value="ltr">从左到右</n-radio-button>
                <n-radio-button value="rtl">从右到左</n-radio-button>
              </n-radio-group>
            </n-form-item>
            <n-form-item label="缩进">
              <n-input-number
                v-model:value="localStyle.alignment.indent"
                :min="0"
                :max="20"
                placeholder="缩进级别"
                @update:value="emitUpdate"
              />
            </n-form-item>
            <n-form-item label="自动换行">
              <n-switch v-model:value="localStyle.alignment.wrapText" @update:value="emitUpdate" />
            </n-form-item>
            <n-form-item label="缩小填充">
              <n-switch v-model:value="localStyle.alignment.shrinkToFit" @update:value="emitUpdate" />
            </n-form-item>
            <n-form-item label="旋转角度">
              <n-slider
                v-model:value="localStyle.alignment.rotation"
                :min="-90"
                :max="90"
                :step="1"
                :tooltip="true"
                @update:value="emitUpdate"
              />
            </n-form-item>
          </n-form>
        </div>
      </n-tab-pane>

      <!-- 边框配置 -->
      <n-tab-pane name="border" tab="边框">
        <div class="config-section">
          <div class="border-quick-actions">
            <n-button size="small" @click="setBorderAll">全部边框</n-button>
            <n-button size="small" @click="setBorderOuter">外边框</n-button>
            <n-button size="small" @click="clearBorder">清除边框</n-button>
          </div>
          
          <n-divider style="margin: 12px 0;">单边设置</n-divider>
          
          <div class="border-sides">
            <div v-for="side in borderSides" :key="side.key" class="border-side-config">
              <div class="side-label">
                <n-icon :component="side.icon" size="14" />
                <span>{{ side.label }}</span>
              </div>
              <div class="side-controls">
                <n-select
                  v-model:value="localStyle.border[side.key].style"
                  :options="borderStyleOptions"
                  size="small"
                  style="width: 90px;"
                  placeholder="样式"
                  @update:value="emitUpdate"
                />
                <n-input-number
                  v-model:value="localStyle.border[side.key].width"
                  :min="0"
                  :max="10"
                  size="small"
                  style="width: 70px;"
                  placeholder="宽度"
                  @update:value="emitUpdate"
                />
                <n-color-picker
                  v-model:value="localStyle.border[side.key].color"
                  :show-alpha="false"
                  :modes="['hex']"
                  size="small"
                  @update:value="emitUpdate"
                />
              </div>
            </div>
          </div>
        </div>
      </n-tab-pane>

      <!-- 背景配置 -->
      <n-tab-pane name="background" tab="背景">
        <div class="config-section">
          <n-form label-placement="left" label-width="70" size="small">
            <n-form-item label="类型">
              <n-radio-group v-model:value="localStyle.background.type" @update:value="onBackgroundTypeChange">
                <n-radio-button value="solid">纯色</n-radio-button>
                <n-radio-button value="gradient">渐变</n-radio-button>
                <n-radio-button value="pattern">图案</n-radio-button>
              </n-radio-group>
            </n-form-item>
            
            <!-- 纯色背景 -->
            <template v-if="localStyle.background.type === 'solid'">
              <n-form-item label="背景色">
                <n-color-picker
                  v-model:value="localStyle.background.color"
                  :show-alpha="true"
                  :modes="['hex', 'rgb']"
                  @update:value="emitUpdate"
                />
              </n-form-item>
            </template>
            
            <!-- 渐变背景 -->
            <template v-if="localStyle.background.type === 'gradient'">
              <n-form-item label="渐变类型">
                <n-radio-group v-model:value="localStyle.background.gradient.type" @update:value="emitUpdate">
                  <n-radio-button value="linear">线性</n-radio-button>
                  <n-radio-button value="radial">径向</n-radio-button>
                </n-radio-group>
              </n-form-item>
              <n-form-item v-if="localStyle.background.gradient.type === 'linear'" label="角度">
                <n-slider
                  v-model:value="localStyle.background.gradient.angle"
                  :min="0"
                  :max="360"
                  :step="1"
                  @update:value="emitUpdate"
                />
              </n-form-item>
              <n-form-item label="颜色节点">
                <div class="gradient-colors">
                  <div v-for="(stop, index) in localStyle.background.gradient.colors" :key="index" class="gradient-stop">
                    <n-input-number
                      v-model:value="stop.offset"
                      :min="0"
                      :max="1"
                      :step="0.1"
                      size="small"
                      style="width: 70px;"
                      @update:value="emitUpdate"
                    />
                    <n-color-picker
                      v-model:value="stop.color"
                      :show-alpha="false"
                      :modes="['hex']"
                      size="small"
                      @update:value="emitUpdate"
                    />
                    <n-button v-if="localStyle.background.gradient.colors.length > 2" text type="error" size="small" @click="removeGradientStop(index)">
                      <n-icon><CloseOutline /></n-icon>
                    </n-button>
                  </div>
                  <n-button size="small" dashed @click="addGradientStop">
                    <template #icon><n-icon><AddOutline /></n-icon></template>
                    添加节点
                  </n-button>
                </div>
              </n-form-item>
            </template>
            
            <!-- 图案背景 -->
            <template v-if="localStyle.background.type === 'pattern'">
              <n-form-item label="图案类型">
                <n-select
                  v-model:value="localStyle.background.pattern.type"
                  :options="patternTypeOptions"
                  placeholder="选择图案"
                  @update:value="emitUpdate"
                />
              </n-form-item>
              <n-form-item label="前景色">
                <n-color-picker
                  v-model:value="localStyle.background.pattern.foreground"
                  :show-alpha="false"
                  :modes="['hex']"
                  @update:value="emitUpdate"
                />
              </n-form-item>
              <n-form-item label="背景色">
                <n-color-picker
                  v-model:value="localStyle.background.pattern.background"
                  :show-alpha="false"
                  :modes="['hex']"
                  @update:value="emitUpdate"
                />
              </n-form-item>
            </template>
          </n-form>
        </div>
      </n-tab-pane>

      <!-- 内边距配置 -->
      <n-tab-pane name="padding" tab="内边距">
        <div class="config-section">
          <div class="padding-link-toggle">
            <n-checkbox v-model:checked="linkPadding" @update:checked="onLinkPaddingChange">
              统一设置
            </n-checkbox>
          </div>
          
          <div class="padding-config">
            <div class="padding-visual">
              <div class="padding-box">
                <div class="padding-input top">
                  <n-input-number
                    v-model:value="localStyle.padding.top"
                    :min="0"
                    :max="100"
                    size="small"
                    placeholder="上"
                    @update:value="(v) => onPaddingChange('top', v)"
                  />
                </div>
                <div class="padding-middle">
                  <div class="padding-input left">
                    <n-input-number
                      v-model:value="localStyle.padding.left"
                      :min="0"
                      :max="100"
                      size="small"
                      placeholder="左"
                      @update:value="(v) => onPaddingChange('left', v)"
                    />
                  </div>
                  <div class="padding-center">
                    <span>内容</span>
                  </div>
                  <div class="padding-input right">
                    <n-input-number
                      v-model:value="localStyle.padding.right"
                      :min="0"
                      :max="100"
                      size="small"
                      placeholder="右"
                      @update:value="(v) => onPaddingChange('right', v)"
                    />
                  </div>
                </div>
                <div class="padding-input bottom">
                  <n-input-number
                    v-model:value="localStyle.padding.bottom"
                    :min="0"
                    :max="100"
                    size="small"
                    placeholder="下"
                    @update:value="(v) => onPaddingChange('bottom', v)"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </n-tab-pane>
    </n-tabs>

    <!-- 操作按钮 -->
    <div class="editor-actions">
      <n-button size="small" @click="resetStyle">重置</n-button>
      <n-button size="small" type="primary" @click="applyStyle">应用</n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 单元格样式配置器
 * Cell Style Editor Component
 * 
 * 支持功能：
 * - 字体配置（字体族、大小、粗细、斜体、下划线、删除线）
 * - 对齐配置（水平对齐、垂直对齐、文字方向、缩进）
 * - 边框配置（样式、颜色、宽度，支持单边设置）
 * - 背景配置（纯色、渐变色、图案填充）
 * - 内边距配置（上下左右独立设置）
 * - 样式实时预览
 * 
 * 需求: 14.2.6, 14.2.7, 14.2.8, 14.2.9, 14.2.10
 */

import { ref, computed, watch, reactive, h } from 'vue'
import {
  NIcon,
  NTabs,
  NTabPane,
  NForm,
  NFormItem,
  NSelect,
  NInputNumber,
  NColorPicker,
  NRadioGroup,
  NRadioButton,
  NSwitch,
  NSlider,
  NButton,
  NTooltip,
  NDivider,
  NCheckbox,
} from 'naive-ui'
import {
  ColorPaletteOutline,
  HelpCircleOutline,
  EyeOutline,
  AddOutline,
  CloseOutline,
  ReorderTwoOutline,
  RemoveOutline,
  ChevronUpOutline,
  ChevronDownOutline,
  ChevronBackOutline,
  ChevronForwardOutline,
} from '@vicons/ionicons5'

// Custom alignment icons using simple SVG components
const AlignLeftOutline = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'currentColor' }, [
      h('path', { d: 'M3 3h18v2H3V3zm0 4h12v2H3V7zm0 4h18v2H3v-2zm0 4h12v2H3v-2zm0 4h18v2H3v-2z' })
    ])
  }
}

const AlignCenterOutline = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'currentColor' }, [
      h('path', { d: 'M3 3h18v2H3V3zm3 4h12v2H6V7zm-3 4h18v2H3v-2zm3 4h12v2H6v-2zm-3 4h18v2H3v-2z' })
    ])
  }
}

const AlignRightOutline = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'currentColor' }, [
      h('path', { d: 'M3 3h18v2H3V3zm6 4h12v2H9V7zm-6 4h18v2H3v-2zm6 4h12v2H9v-2zm-6 4h18v2H3v-2z' })
    ])
  }
}

const AlignJustifyOutline = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'currentColor' }, [
      h('path', { d: 'M3 3h18v2H3V3zm0 4h18v2H3V7zm0 4h18v2H3v-2zm0 4h18v2H3v-2zm0 4h18v2H3v-2z' })
    ])
  }
}

// Border icons
const BorderTop = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'currentColor' }, [
      h('path', { d: 'M3 3h18v2H3V3z' })
    ])
  }
}

const BorderRight = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'currentColor' }, [
      h('path', { d: 'M19 3h2v18h-2V3z' })
    ])
  }
}

const BorderBottom = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'currentColor' }, [
      h('path', { d: 'M3 19h18v2H3v-2z' })
    ])
  }
}

const BorderLeft = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'currentColor' }, [
      h('path', { d: 'M3 3h2v18H3V3z' })
    ])
  }
}
import type {
  CellStyle,
  FontStyle,
  AlignmentStyle,
  BorderStyle,
  BorderSide,
  BackgroundStyle,
  PaddingStyle,
  GradientConfig,
  PatternConfig,
} from './types'

// Props
const props = withDefaults(defineProps<{
  modelValue?: CellStyle
  previewText?: string
}>(), {
  previewText: '示例文本 Sample Text 12345'
})

// Emits
const emit = defineEmits<{
  (e: 'update:modelValue', value: CellStyle): void
  (e: 'apply', value: CellStyle): void
}>()

// 当前激活的标签页
const activeTab = ref('font')

// 是否联动内边距
const linkPadding = ref(false)

// 默认边框侧配置
const defaultBorderSide = (): BorderSide => ({
  style: 'none',
  width: 1,
  color: '#000000'
})

// 默认渐变配置
const defaultGradient = (): GradientConfig => ({
  type: 'linear',
  angle: 90,
  colors: [
    { offset: 0, color: '#ffffff' },
    { offset: 1, color: '#f0f0f0' }
  ]
})

// 默认图案配置
const defaultPattern = (): PatternConfig => ({
  type: 'stripe',
  foreground: '#e0e0e0',
  background: '#ffffff'
})

// 本地样式状态
const localStyle = reactive({
  font: {
    family: 'Microsoft YaHei',
    size: 14,
    weight: 'normal' as FontStyle['weight'],
    style: 'normal' as FontStyle['style'],
    decoration: 'none' as FontStyle['decoration'],
    color: '#333333'
  },
  alignment: {
    horizontal: 'left' as AlignmentStyle['horizontal'],
    vertical: 'middle' as AlignmentStyle['vertical'],
    textDirection: 'ltr' as AlignmentStyle['textDirection'],
    indent: 0,
    wrapText: false,
    shrinkToFit: false,
    rotation: 0
  },
  border: {
    top: defaultBorderSide(),
    right: defaultBorderSide(),
    bottom: defaultBorderSide(),
    left: defaultBorderSide()
  } as Record<string, BorderSide>,
  background: {
    type: 'solid' as BackgroundStyle['type'],
    color: '#ffffff',
    gradient: defaultGradient(),
    pattern: defaultPattern()
  },
  padding: {
    top: 4,
    right: 8,
    bottom: 4,
    left: 8
  }
})

// 字体族选项
const fontFamilyOptions = [
  { label: '微软雅黑', value: 'Microsoft YaHei' },
  { label: '宋体', value: 'SimSun' },
  { label: '黑体', value: 'SimHei' },
  { label: '楷体', value: 'KaiTi' },
  { label: 'Arial', value: 'Arial' },
  { label: 'Times New Roman', value: 'Times New Roman' },
  { label: 'Verdana', value: 'Verdana' },
  { label: 'Courier New', value: 'Courier New' },
  { label: 'Georgia', value: 'Georgia' },
  { label: 'Tahoma', value: 'Tahoma' }
]

// 字体粗细选项
const fontWeightOptions = [
  { label: '正常', value: 'normal' },
  { label: '粗体', value: 'bold' },
  { label: '100 - 极细', value: 100 },
  { label: '200 - 特细', value: 200 },
  { label: '300 - 细', value: 300 },
  { label: '400 - 正常', value: 400 },
  { label: '500 - 中等', value: 500 },
  { label: '600 - 半粗', value: 600 },
  { label: '700 - 粗', value: 700 },
  { label: '800 - 特粗', value: 800 },
  { label: '900 - 极粗', value: 900 }
]

// 字体样式选项
const fontStyleOptions = [
  { label: '正常', value: 'normal' },
  { label: '斜体', value: 'italic' },
  { label: '倾斜', value: 'oblique' }
]

// 字体装饰选项
const fontDecorationOptions = [
  { label: '无', value: 'none' },
  { label: '下划线', value: 'underline' },
  { label: '删除线', value: 'line-through' },
  { label: '下划线+删除线', value: 'underline line-through' }
]

// 边框样式选项
const borderStyleOptions = [
  { label: '无', value: 'none' },
  { label: '实线', value: 'solid' },
  { label: '虚线', value: 'dashed' },
  { label: '点线', value: 'dotted' },
  { label: '双线', value: 'double' }
]

// 边框侧配置
const borderSides = [
  { key: 'top', label: '上边框', icon: BorderTop },
  { key: 'right', label: '右边框', icon: BorderRight },
  { key: 'bottom', label: '下边框', icon: BorderBottom },
  { key: 'left', label: '左边框', icon: BorderLeft }
]

// 图案类型选项
const patternTypeOptions = [
  { label: '条纹', value: 'stripe' },
  { label: '点阵', value: 'dots' },
  { label: '网格', value: 'grid' },
  { label: '斜线', value: 'diagonal' }
]

// 计算预览样式
const previewStyle = computed(() => {
  const style: Record<string, string> = {}
  
  // 字体样式
  if (localStyle.font.family) {
    style.fontFamily = localStyle.font.family
  }
  if (localStyle.font.size) {
    style.fontSize = `${localStyle.font.size}px`
  }
  if (localStyle.font.weight) {
    style.fontWeight = String(localStyle.font.weight)
  }
  if (localStyle.font.style) {
    style.fontStyle = localStyle.font.style
  }
  if (localStyle.font.decoration) {
    style.textDecoration = localStyle.font.decoration
  }
  if (localStyle.font.color) {
    style.color = localStyle.font.color
  }
  
  // 对齐样式
  if (localStyle.alignment.horizontal) {
    style.textAlign = localStyle.alignment.horizontal
  }
  if (localStyle.alignment.vertical) {
    const verticalMap: Record<string, string> = {
      top: 'flex-start',
      middle: 'center',
      bottom: 'flex-end'
    }
    style.alignItems = verticalMap[localStyle.alignment.vertical] || 'center'
  }
  if (localStyle.alignment.textDirection) {
    style.direction = localStyle.alignment.textDirection
  }
  if (localStyle.alignment.indent && localStyle.alignment.indent > 0) {
    style.paddingLeft = `${localStyle.alignment.indent * 8}px`
  }
  if (localStyle.alignment.wrapText) {
    style.whiteSpace = 'pre-wrap'
    style.wordBreak = 'break-word'
  } else {
    style.whiteSpace = 'nowrap'
    style.overflow = 'hidden'
    style.textOverflow = 'ellipsis'
  }
  if (localStyle.alignment.rotation && localStyle.alignment.rotation !== 0) {
    style.transform = `rotate(${localStyle.alignment.rotation}deg)`
  }
  
  // 边框样式
  const borderStyles: string[] = []
  for (const side of ['top', 'right', 'bottom', 'left']) {
    const b = localStyle.border[side]
    if (b && b.style !== 'none') {
      borderStyles.push(`border-${side}: ${b.width || 1}px ${b.style} ${b.color || '#000'}`)
    }
  }
  if (borderStyles.length > 0) {
    borderStyles.forEach(bs => {
      const [prop, val] = bs.split(': ')
      const camelProp = prop.replace(/-([a-z])/g, (_, c) => c.toUpperCase())
      style[camelProp] = val
    })
  }
  
  // 背景样式
  if (localStyle.background.type === 'solid' && localStyle.background.color) {
    style.backgroundColor = localStyle.background.color
  } else if (localStyle.background.type === 'gradient' && localStyle.background.gradient) {
    const g = localStyle.background.gradient
    const colorStops = g.colors.map(c => `${c.color} ${c.offset * 100}%`).join(', ')
    if (g.type === 'linear') {
      style.background = `linear-gradient(${g.angle || 90}deg, ${colorStops})`
    } else {
      style.background = `radial-gradient(circle, ${colorStops})`
    }
  } else if (localStyle.background.type === 'pattern' && localStyle.background.pattern) {
    const p = localStyle.background.pattern
    style.backgroundColor = p.background
    // 使用 CSS 图案
    switch (p.type) {
      case 'stripe':
        style.backgroundImage = `repeating-linear-gradient(45deg, ${p.foreground}, ${p.foreground} 2px, transparent 2px, transparent 8px)`
        break
      case 'dots':
        style.backgroundImage = `radial-gradient(${p.foreground} 1px, transparent 1px)`
        style.backgroundSize = '8px 8px'
        break
      case 'grid':
        style.backgroundImage = `linear-gradient(${p.foreground} 1px, transparent 1px), linear-gradient(90deg, ${p.foreground} 1px, transparent 1px)`
        style.backgroundSize = '10px 10px'
        break
      case 'diagonal':
        style.backgroundImage = `repeating-linear-gradient(-45deg, ${p.foreground}, ${p.foreground} 1px, transparent 1px, transparent 6px)`
        break
    }
  }
  
  // 内边距样式
  style.padding = `${localStyle.padding.top || 0}px ${localStyle.padding.right || 0}px ${localStyle.padding.bottom || 0}px ${localStyle.padding.left || 0}px`
  
  return style
})

// 监听外部值变化
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    // 合并字体样式
    if (newVal.font) {
      Object.assign(localStyle.font, newVal.font)
    }
    // 合并对齐样式
    if (newVal.alignment) {
      Object.assign(localStyle.alignment, newVal.alignment)
    }
    // 合并边框样式
    if (newVal.border) {
      for (const side of ['top', 'right', 'bottom', 'left']) {
        if (newVal.border[side as keyof BorderStyle]) {
          Object.assign(localStyle.border[side], newVal.border[side as keyof BorderStyle])
        }
      }
      // 处理 all 快捷设置
      if (newVal.border.all) {
        for (const side of ['top', 'right', 'bottom', 'left']) {
          Object.assign(localStyle.border[side], newVal.border.all)
        }
      }
    }
    // 合并背景样式
    if (newVal.background) {
      localStyle.background.type = newVal.background.type || 'solid'
      if (newVal.background.color) {
        localStyle.background.color = newVal.background.color
      }
      if (newVal.background.gradient) {
        Object.assign(localStyle.background.gradient, newVal.background.gradient)
      }
      if (newVal.background.pattern) {
        Object.assign(localStyle.background.pattern, newVal.background.pattern)
      }
    }
    // 合并内边距样式
    if (newVal.padding) {
      Object.assign(localStyle.padding, newVal.padding)
    }
  }
}, { immediate: true, deep: true })

// 发送更新
function emitUpdate() {
  const style = buildCellStyle()
  emit('update:modelValue', style)
}

// 构建 CellStyle 对象
function buildCellStyle(): CellStyle {
  const style: CellStyle = {
    font: { ...localStyle.font },
    alignment: { ...localStyle.alignment },
    border: {
      top: { ...localStyle.border.top },
      right: { ...localStyle.border.right },
      bottom: { ...localStyle.border.bottom },
      left: { ...localStyle.border.left }
    },
    background: {
      type: localStyle.background.type,
      color: localStyle.background.type === 'solid' ? localStyle.background.color : undefined,
      gradient: localStyle.background.type === 'gradient' ? { ...localStyle.background.gradient } : undefined,
      pattern: localStyle.background.type === 'pattern' ? { ...localStyle.background.pattern } : undefined
    },
    padding: { ...localStyle.padding }
  }
  return style
}

// 边框快捷操作
function setBorderAll() {
  const defaultBorder: BorderSide = { style: 'solid', width: 1, color: '#000000' }
  for (const side of ['top', 'right', 'bottom', 'left']) {
    Object.assign(localStyle.border[side], defaultBorder)
  }
  emitUpdate()
}

function setBorderOuter() {
  const defaultBorder: BorderSide = { style: 'solid', width: 1, color: '#000000' }
  for (const side of ['top', 'right', 'bottom', 'left']) {
    Object.assign(localStyle.border[side], defaultBorder)
  }
  emitUpdate()
}

function clearBorder() {
  for (const side of ['top', 'right', 'bottom', 'left']) {
    localStyle.border[side] = defaultBorderSide()
  }
  emitUpdate()
}

// 背景类型变化
function onBackgroundTypeChange() {
  emitUpdate()
}

// 渐变颜色节点操作
function addGradientStop() {
  const colors = localStyle.background.gradient.colors
  const lastOffset = colors[colors.length - 1]?.offset || 0
  const newOffset = Math.min(lastOffset + 0.2, 1)
  colors.push({ offset: newOffset, color: '#cccccc' })
  emitUpdate()
}

function removeGradientStop(index: number) {
  if (localStyle.background.gradient.colors.length > 2) {
    localStyle.background.gradient.colors.splice(index, 1)
    emitUpdate()
  }
}

// 内边距联动
function onLinkPaddingChange(linked: boolean) {
  if (linked) {
    const value = localStyle.padding.top
    localStyle.padding.right = value
    localStyle.padding.bottom = value
    localStyle.padding.left = value
    emitUpdate()
  }
}

function onPaddingChange(side: string, value: number | null) {
  if (linkPadding.value && value !== null) {
    localStyle.padding.top = value
    localStyle.padding.right = value
    localStyle.padding.bottom = value
    localStyle.padding.left = value
  }
  emitUpdate()
}

// 重置样式
function resetStyle() {
  localStyle.font = {
    family: 'Microsoft YaHei',
    size: 14,
    weight: 'normal',
    style: 'normal',
    decoration: 'none',
    color: '#333333'
  }
  localStyle.alignment = {
    horizontal: 'left',
    vertical: 'middle',
    textDirection: 'ltr',
    indent: 0,
    wrapText: false,
    shrinkToFit: false,
    rotation: 0
  }
  localStyle.border = {
    top: defaultBorderSide(),
    right: defaultBorderSide(),
    bottom: defaultBorderSide(),
    left: defaultBorderSide()
  }
  localStyle.background = {
    type: 'solid',
    color: '#ffffff',
    gradient: defaultGradient(),
    pattern: defaultPattern()
  }
  localStyle.padding = {
    top: 4,
    right: 8,
    bottom: 4,
    left: 8
  }
  emitUpdate()
}

// 应用样式
function applyStyle() {
  const style = buildCellStyle()
  emit('apply', style)
}
</script>

<style scoped>
.cell-style-editor {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  background: #ffffff;
  border-radius: 8px;
}

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.editor-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

/* 预览区域 */
.preview-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
}

.preview-container {
  padding: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  min-height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 200px;
  min-height: 48px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
  transition: all 0.2s ease;
}

/* 配置区域 */
.config-section {
  padding: 12px 0;
}

/* 边框配置 */
.border-quick-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.border-sides {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.border-side-config {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 6px;
}

.side-label {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 80px;
  font-size: 12px;
  color: #475569;
}

.side-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

/* 渐变颜色配置 */
.gradient-colors {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.gradient-stop {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 内边距配置 */
.padding-link-toggle {
  margin-bottom: 12px;
}

.padding-config {
  display: flex;
  justify-content: center;
}

.padding-visual {
  width: 240px;
}

.padding-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: #f8fafc;
  border: 2px dashed #cbd5e1;
  border-radius: 8px;
}

.padding-input {
  display: flex;
  justify-content: center;
}

.padding-input :deep(.n-input-number) {
  width: 70px;
}

.padding-middle {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.padding-center {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 48px;
  background: #e2e8f0;
  border-radius: 4px;
  font-size: 12px;
  color: #64748b;
}

/* 操作按钮 */
.editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
}
</style>

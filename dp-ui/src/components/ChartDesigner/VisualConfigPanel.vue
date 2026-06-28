<template>
  <div class="visual-config-panel">
    <!-- 快速配置区域 -->
    <div class="quick-config-section">
      <div class="section-header">
        <n-icon size="14"><FlashOutline /></n-icon>
        <span>快速配置</span>
      </div>
      
      <!-- 图表类型快速切换 -->
      <div class="chart-type-grid">
        <div
          v-for="type in chartTypes"
          :key="type.value"
          class="chart-type-item"
          :class="{ 'chart-type-item--active': modelValue.chartType === type.value }"
          @click="updateConfig('chartType', type.value)"
        >
          <div class="chart-type-icon" :style="{ background: type.gradient }">
            <n-icon :size="20" color="#fff">
              <component :is="type.icon" />
            </n-icon>
          </div>
          <span class="chart-type-label">{{ type.label }}</span>
        </div>
      </div>
    </div>

    <!-- 颜色主题快速选择 -->
    <div class="theme-section">
      <div class="section-header">
        <n-icon size="14"><ColorPaletteOutline /></n-icon>
        <span>配色主题</span>
        <ConfigTooltip :content="tooltips.colorScheme" />
      </div>
      
      <div class="theme-grid">
        <div
          v-for="theme in colorThemes"
          :key="theme.name"
          class="theme-item"
          :class="{ 'theme-item--active': isThemeActive(theme) }"
          @click="applyTheme(theme)"
        >
          <div class="theme-preview" :style="{ background: theme.background }">
            <div class="theme-colors">
              <span
                v-for="(color, idx) in theme.colors.slice(0, 4)"
                :key="idx"
                class="theme-color-dot"
                :style="{ background: color }"
              />
            </div>
          </div>
          <span class="theme-label">{{ theme.label }}</span>
        </div>
      </div>
    </div>

    <!-- 实时预览开关 -->
    <div class="realtime-toggle">
      <n-switch
        :value="realtimePreview"
        size="small"
        @update:value="$emit('update:realtimePreview', $event)"
      >
        <template #checked>实时预览</template>
        <template #unchecked>手动预览</template>
      </n-switch>
      <ConfigTooltip content="开启后，配置变更将自动更新预览；关闭后需手动点击预览按钮" />
    </div>

    <!-- 常用配置快捷开关 -->
    <div class="quick-toggles">
      <div class="toggle-item">
        <span class="toggle-label">
          显示图例
          <ConfigTooltip :content="tooltips.showLegend" />
        </span>
        <n-switch
          :value="modelValue.showLegend"
          size="small"
          @update:value="updateConfig('showLegend', $event)"
        />
      </div>
      
      <div class="toggle-item">
        <span class="toggle-label">
          显示网格
          <ConfigTooltip :content="tooltips.showGrid" />
        </span>
        <n-switch
          :value="modelValue.showGrid"
          size="small"
          @update:value="updateConfig('showGrid', $event)"
        />
      </div>
      
      <div class="toggle-item">
        <span class="toggle-label">
          显示标签
          <ConfigTooltip :content="tooltips.showLabel" />
        </span>
        <n-switch
          :value="modelValue.showLabel"
          size="small"
          @update:value="updateConfig('showLabel', $event)"
        />
      </div>
      
      <div class="toggle-item">
        <span class="toggle-label">
          启用动画
          <ConfigTooltip :content="tooltips.animation" />
        </span>
        <n-switch
          :value="modelValue.animation"
          size="small"
          @update:value="updateConfig('animation', $event)"
        />
      </div>
    </div>

    <!-- 尺寸快速调整 -->
    <div class="size-section">
      <div class="section-header">
        <n-icon size="14"><ResizeOutline /></n-icon>
        <span>图表尺寸</span>
      </div>
      
      <div class="size-presets">
        <n-button
          v-for="preset in sizePresets"
          :key="preset.label"
          size="tiny"
          :type="isSizeActive(preset) ? 'primary' : 'default'"
          @click="applySize(preset)"
        >
          {{ preset.label }}
        </n-button>
      </div>
      
      <div class="size-inputs">
        <div class="size-input-item">
          <label>宽度</label>
          <n-input-number
            :value="modelValue.width"
            size="small"
            :min="200"
            :max="4000"
            :step="10"
            placeholder="自适应"
            clearable
            @update:value="updateConfig('width', $event)"
          />
        </div>
        <div class="size-input-item">
          <label>高度</label>
          <n-input-number
            :value="modelValue.height"
            size="small"
            :min="200"
            :max="3000"
            :step="10"
            placeholder="400"
            clearable
            @update:value="updateConfig('height', $event)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 可视化配置面板组件
 * 提供所见即所得的快速配置体验
 * 验证需求: 3.1, 3.2, 3.3
 */
import { computed } from 'vue'
import { NSwitch, NButton, NIcon, NInputNumber } from 'naive-ui'
import {
  FlashOutline,
  ColorPaletteOutline,
  ResizeOutline,
  BarChartOutline,
  TrendingUpOutline,
  PieChartOutline,
  StatsChartOutline,
  GridOutline
} from '@vicons/ionicons5'
import ConfigTooltip from './ConfigTooltip.vue'
import { CONFIG_TOOLTIPS } from '@/composables/useChartPreview'
import { chartThemes, getThemePreviewColors, type ChartThemeConfig } from '@/utils/chartThemes'

interface StyleConfig {
  chartType?: string
  colorScheme?: string
  backgroundColor?: string
  showLegend?: boolean
  showGrid?: boolean
  showLabel?: boolean
  animation?: boolean
  width?: number | null
  height?: number | null
}

interface Props {
  modelValue: StyleConfig
  realtimePreview?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: StyleConfig): void
  (e: 'update:realtimePreview', value: boolean): void
  (e: 'configChange', key: string, value: any): void
}

const props = withDefaults(defineProps<Props>(), {
  realtimePreview: true
})

const emit = defineEmits<Emits>()

// 图表类型选项
const chartTypes = [
  { value: 'line', label: '折线图', icon: TrendingUpOutline, gradient: 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)' },
  { value: 'bar', label: '柱状图', icon: BarChartOutline, gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' },
  { value: 'pie', label: '饼图', icon: PieChartOutline, gradient: 'linear-gradient(135deg, #ee0979 0%, #ff6a00 100%)' },
  { value: 'scatter', label: '散点图', icon: StatsChartOutline, gradient: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' },
  { value: 'table', label: '数据表', icon: GridOutline, gradient: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)' }
]

/**
 * 颜色主题选项 - 使用 chartThemes 模块的 6 套配色主题
 * 验证需求: 3.2 - 图表设计器应提供至少 6 套图表配色主题
 */
const colorThemes = computed(() => 
  Object.values(chartThemes).map((theme: ChartThemeConfig) => ({
    name: theme.name,
    label: theme.label,
    scheme: theme.name,
    background: theme.backgroundColor,
    colors: getThemePreviewColors(theme.name)
  }))
)

// 尺寸预设
const sizePresets = [
  { label: '小', width: 400, height: 300 },
  { label: '中', width: 600, height: 400 },
  { label: '大', width: 800, height: 500 },
  { label: '宽屏', width: 1000, height: 400 },
  { label: '自适应', width: null, height: 400 }
]

// 提示文本
const tooltips = CONFIG_TOOLTIPS

// 更新配置
const updateConfig = (key: string, value: any) => {
  emit('update:modelValue', { ...props.modelValue, [key]: value })
  emit('configChange', key, value)
}

// 应用主题
const applyTheme = (theme: typeof colorThemes[0]) => {
  emit('update:modelValue', {
    ...props.modelValue,
    colorScheme: theme.scheme,
    backgroundColor: theme.background
  })
  emit('configChange', 'theme', theme)
}

// 检查主题是否激活
const isThemeActive = (theme: typeof colorThemes[0]) => {
  return props.modelValue.colorScheme === theme.scheme &&
         props.modelValue.backgroundColor === theme.background
}

// 应用尺寸预设
const applySize = (preset: typeof sizePresets[0]) => {
  emit('update:modelValue', {
    ...props.modelValue,
    width: preset.width,
    height: preset.height
  })
  emit('configChange', 'size', preset)
}

// 检查尺寸是否激活
const isSizeActive = (preset: typeof sizePresets[0]) => {
  return props.modelValue.width === preset.width &&
         props.modelValue.height === preset.height
}
</script>

<style scoped>
.visual-config-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 12px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 10px;
}

/* 图表类型网格 */
.chart-type-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.chart-type-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 4px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.chart-type-item:hover {
  background: #f1f5f9;
}

.chart-type-item--active {
  background: #eff6ff;
  border-color: #3b82f6;
}

.chart-type-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-type-label {
  font-size: 10px;
  color: #64748b;
}

.chart-type-item--active .chart-type-label {
  color: #3b82f6;
  font-weight: 600;
}

/* 主题网格 */
.theme-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.theme-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 6px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.theme-item:hover {
  background: #f1f5f9;
}

.theme-item--active {
  border-color: #3b82f6;
}

.theme-preview {
  width: 100%;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e2e8f0;
}

.theme-colors {
  display: flex;
  gap: 3px;
}

.theme-color-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.theme-label {
  font-size: 10px;
  color: #64748b;
}

.theme-item--active .theme-label {
  color: #3b82f6;
  font-weight: 600;
}

/* 实时预览开关 */
.realtime-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 8px;
}

/* 快捷开关 */
.quick-toggles {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.toggle-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  background: #f8fafc;
  border-radius: 6px;
}

.toggle-label {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #475569;
}

/* 尺寸配置 */
.size-presets {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
}

.size-inputs {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.size-input-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.size-input-item label {
  font-size: 11px;
  color: #64748b;
}
</style>

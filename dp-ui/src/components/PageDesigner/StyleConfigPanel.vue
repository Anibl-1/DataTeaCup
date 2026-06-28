<template>
  <div class="style-config-panel">
    <n-tabs v-model:value="styleTab" type="segment" size="small">
      <!-- 基础 -->
      <n-tab-pane name="basic" tab="基础">
        <div class="tab-content">
          <div class="config-section">
            <div class="section-label">图表类型</div>
            <n-select
              :value="config.chartType"
              :options="chartTypeOptions"
              size="small"
              @update:value="updateField('chartType', $event)"
            />
          </div>
          <div class="config-section">
            <div class="section-label">图表名称</div>
            <n-input
              :value="config.chartName"
              placeholder="图表名称"
              size="small"
              @update:value="updateField('chartName', $event)"
            />
          </div>
          <div class="config-section">
            <div class="section-label">描述</div>
            <n-input
              :value="config.description"
              type="textarea"
              :rows="2"
              placeholder="图表描述（可选）"
              size="small"
              @update:value="updateField('description', $event)"
            />
          </div>
        </div>
      </n-tab-pane>

      <!-- 颜色 -->
      <n-tab-pane name="color" tab="颜色">
        <div class="tab-content">
          <div class="config-section">
            <div class="section-label">配色方案</div>
            <n-select
              :value="config.colorScheme || 'default'"
              :options="colorSchemeOptions"
              size="small"
              @update:value="handleColorSchemeChange"
            >
              <template #option="{ option }">
                <div class="color-option">
                  <div class="color-preview">
                    <span
                      v-for="(c, idx) in (option.colors || []).slice(0, 6)"
                      :key="idx"
                      class="color-dot"
                      :style="{ background: c }"
                    />
                  </div>
                  <span>{{ option.label }}</span>
                </div>
              </template>
            </n-select>
            <!-- 颜色预览条 -->
            <div v-if="currentSchemeColors.length > 0" class="color-scheme-bar">
              <span
                v-for="(c, idx) in currentSchemeColors"
                :key="idx"
                class="color-bar-block"
                :style="{ backgroundColor: c }"
              />
            </div>
          </div>

          <div class="config-section">
            <div class="section-label">背景颜色</div>
            <n-color-picker
              :value="styleState.backgroundColor"
              :modes="['hex']"
              size="small"
              :swatches="['#ffffff','#f5f5f5','#fafafa','#ecf0f1','#2c3e50','#1a1a1a','#0f0f23','#000000']"
              @update:value="handleBackgroundChange"
            />
          </div>
        </div>
      </n-tab-pane>

      <!-- 图表 -->
      <n-tab-pane name="chart" tab="图表">
        <div class="tab-content">
          <n-form label-placement="left" label-width="auto" size="small">
            <n-form-item label="显示图例">
              <n-switch v-model:value="styleState.showLegend" @update:value="applyStyleToConfig" />
            </n-form-item>
            <n-form-item v-if="styleState.showLegend" label="图例位置">
              <n-select
                v-model:value="styleState.legendPosition"
                :options="legendPositionOptions"
                size="small"
                @update:value="applyStyleToConfig"
              />
            </n-form-item>
            <n-form-item label="显示网格">
              <n-switch v-model:value="styleState.showGrid" @update:value="applyStyleToConfig" />
            </n-form-item>
            <n-form-item label="显示标签">
              <n-switch v-model:value="styleState.showLabel" @update:value="applyStyleToConfig" />
            </n-form-item>

            <!-- 折线图 -->
            <template v-if="config.chartType === 'line'">
              <n-divider style="margin: 8px 0" />
              <n-text depth="3" style="font-size: 12px">折线图配置</n-text>
              <n-form-item label="平滑曲线">
                <n-switch v-model:value="styleState.smooth" @update:value="applyStyleToConfig" />
              </n-form-item>
              <n-form-item label="数据点">
                <n-switch v-model:value="styleState.showSymbol" @update:value="applyStyleToConfig" />
              </n-form-item>
              <n-form-item v-if="styleState.showSymbol" label="点大小">
                <n-input-number v-model:value="styleState.symbolSize" :min="2" :max="20" style="width:100%" @update:value="applyStyleToConfig" />
              </n-form-item>
              <n-form-item label="面积填充">
                <n-switch v-model:value="styleState.areaStyle" @update:value="applyStyleToConfig" />
              </n-form-item>
            </template>

            <!-- 柱状图 -->
            <template v-if="config.chartType === 'bar'">
              <n-divider style="margin: 8px 0" />
              <n-text depth="3" style="font-size: 12px">柱状图配置</n-text>
              <n-form-item label="最大柱宽">
                <n-input-number v-model:value="styleState.barMaxWidth" :min="5" :max="100" placeholder="40" style="width:100%" @update:value="applyStyleToConfig" />
              </n-form-item>
              <n-form-item label="柱圆角">
                <n-input-number v-model:value="styleState.barBorderRadius" :min="0" :max="20" placeholder="3" style="width:100%" @update:value="applyStyleToConfig" />
              </n-form-item>
            </template>

            <!-- 饼图 -->
            <template v-if="config.chartType === 'pie'">
              <n-divider style="margin: 8px 0" />
              <n-text depth="3" style="font-size: 12px">饼图配置</n-text>
              <n-form-item label="内半径%">
                <n-input-number v-model:value="styleState.pieInnerRadius" :min="0" :max="80" style="width:100%" @update:value="applyStyleToConfig" />
              </n-form-item>
              <n-form-item label="玫瑰图">
                <n-switch v-model:value="styleState.pieRoseType" @update:value="applyStyleToConfig" />
              </n-form-item>
            </template>

            <!-- 通用动画 -->
            <n-divider style="margin: 8px 0" />
            <n-form-item label="动画效果">
              <n-switch v-model:value="styleState.animation" @update:value="applyStyleToConfig" />
            </n-form-item>
          </n-form>
        </div>
      </n-tab-pane>

      <!-- 高级 -->
      <n-tab-pane name="advanced" tab="高级">
        <div class="tab-content">
          <div class="config-section">
            <div class="section-label-row">
              <span class="section-label">ECharts 配置</span>
              <n-button size="tiny" text type="primary" @click="formatJson">格式化</n-button>
            </div>
            <n-input
              v-model:value="jsonInput"
              type="textarea"
              :rows="10"
              placeholder='{"tooltip":{"trigger":"axis"},...}'
              size="small"
              :status="jsonError ? 'error' : undefined"
              style="font-family: monospace; font-size: 11px;"
              @blur="handleJsonChange"
            />
            <div v-if="jsonError" class="json-error">{{ jsonError }}</div>
          </div>

        </div>
      </n-tab-pane>
    </n-tabs>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, computed, watch } from 'vue'
import {
  NTabs, NTabPane, NSelect, NInput, NButton, NSwitch,
  NForm, NFormItem, NInputNumber, NDivider, NColorPicker, NText,
  useMessage
} from 'naive-ui'
import { CHART_TYPES } from '@/types/chart'
import { getColorSchemeOptions, getColorScheme } from '@/utils/chartColorSchemes'
import type { InlineChartConfig } from '@/types/page'

const props = defineProps<{
  config: InlineChartConfig
}>()

const emit = defineEmits<{
  (e: 'update:config', config: InlineChartConfig): void
}>()

const message = useMessage()
const jsonInput = ref('')
const jsonError = ref('')
const styleTab = ref('basic')

const chartTypeOptions = CHART_TYPES.map(t => ({ label: t.label, value: t.value }))

const colorSchemeOptions = getColorSchemeOptions().map(s => ({
  label: s.label,
  value: s.value,
  colors: s.colors
}))

// 当前配色预览
const currentSchemeColors = computed(() => {
  const scheme = props.config.colorScheme || 'default'
  try { return getColorScheme(scheme).colors } catch { return [] }
})

// 图例位置选项
const legendPositionOptions = [
  { label: '顶部', value: 'top' },
  { label: '底部', value: 'bottom' },
  { label: '左侧', value: 'left' },
  { label: '右侧', value: 'right' }
]

// ---- 样式状态（从 chartConfig JSON 中提取 / 反写） ----
const styleState = reactive({
  showLegend: true,
  legendPosition: 'top',
  showGrid: true,
  showLabel: false,
  smooth: true,
  showSymbol: true,
  symbolSize: 5,
  areaStyle: false,
  barMaxWidth: 40,
  barBorderRadius: 3,
  pieInnerRadius: 35,
  pieRoseType: false,
  animation: true,
  backgroundColor: '#ffffff'
})

// 从 chartConfig JSON 中解析当前样式
const parseStyleFromConfig = () => {
  if (!props.config.chartConfig) return
  try {
    const opt = JSON.parse(props.config.chartConfig)
    if (opt.legend) {
      styleState.showLegend = opt.legend.show !== false
      if (opt.legend.bottom !== undefined) styleState.legendPosition = 'bottom'
      else if (opt.legend.left === 'left' || (opt.legend.orient === 'vertical' && opt.legend.left !== undefined && opt.legend.right === undefined)) styleState.legendPosition = 'left'
      else if (opt.legend.right !== undefined && opt.legend.left === undefined) styleState.legendPosition = 'right'
      else styleState.legendPosition = 'top'
    }
    if (opt.grid) styleState.showGrid = true
    if (opt.backgroundColor) styleState.backgroundColor = opt.backgroundColor
    if (opt.animation !== undefined) styleState.animation = opt.animation
    // series-level（兼容 series 未指定 type 的情况）
    const s0 = Array.isArray(opt.series) ? opt.series[0] : null
    const s0Type = s0?.type || props.config.chartType
    if (s0) {
      if (s0Type === 'line' || s0.smooth !== undefined) styleState.smooth = !!s0.smooth
      if (s0.showSymbol !== undefined) styleState.showSymbol = s0.showSymbol
      if (s0.symbolSize) styleState.symbolSize = s0.symbolSize
      styleState.areaStyle = !!s0.areaStyle
      if (s0.barMaxWidth) styleState.barMaxWidth = s0.barMaxWidth
      if (s0.itemStyle?.borderRadius) {
        const br = s0.itemStyle.borderRadius
        styleState.barBorderRadius = Array.isArray(br) ? br[0] : br
      }
      if (s0.radius) {
        const inner = Array.isArray(s0.radius) ? s0.radius[0] : '0%'
        styleState.pieInnerRadius = parseInt(String(inner)) || 0
      }
      styleState.pieRoseType = !!s0.roseType
      if (s0.label?.show !== undefined) styleState.showLabel = s0.label.show
    }
  } catch { /* ignore */ }
}

// 将样式状态写回 chartConfig JSON
const applyStyleToConfig = () => {
  if (!props.config.chartConfig) return
  try {
    const opt = JSON.parse(props.config.chartConfig)

    // legend
    if (styleState.showLegend) {
      if (!opt.legend) opt.legend = {}
      opt.legend.show = true
      delete opt.legend.top; delete opt.legend.bottom; delete opt.legend.left; delete opt.legend.right; delete opt.legend.orient
      switch (styleState.legendPosition) {
        case 'bottom': opt.legend.bottom = 10; break
        case 'left': opt.legend.left = 10; opt.legend.orient = 'vertical'; break
        case 'right': opt.legend.right = 10; opt.legend.orient = 'vertical'; break
        default: opt.legend.top = 5; break
      }
    } else if (opt.legend) {
      opt.legend.show = false
    }

    // background
    if (styleState.backgroundColor && styleState.backgroundColor !== '#ffffff') {
      opt.backgroundColor = styleState.backgroundColor
    } else {
      delete opt.backgroundColor
    }
    opt.animation = styleState.animation

    // series（使用 chartType 作为 fallback，兼容 series 未指定 type 的情况）
    if (Array.isArray(opt.series)) {
      opt.series.forEach((s: any) => {
        const sType = s.type || props.config.chartType
        if (!s.label) s.label = {}
        s.label.show = styleState.showLabel

        if (sType === 'line') {
          s.smooth = styleState.smooth
          s.showSymbol = styleState.showSymbol
          s.symbolSize = styleState.symbolSize
          if (styleState.areaStyle) { s.areaStyle = {} } else { delete s.areaStyle }
        }
        if (sType === 'bar') {
          s.barMaxWidth = styleState.barMaxWidth
          if (!s.itemStyle) s.itemStyle = {}
          s.itemStyle.borderRadius = [styleState.barBorderRadius, styleState.barBorderRadius, 0, 0]
        }
        if (sType === 'pie') {
          s.radius = [`${styleState.pieInnerRadius}%`, '65%']
          if (styleState.pieRoseType) { s.roseType = 'area' } else { delete s.roseType }
        }
      })
    }

    const newJson = JSON.stringify(opt)
    emit('update:config', { ...props.config, chartConfig: newJson })
  } catch { /* ignore */ }
}

const handleBackgroundChange = (val: string) => {
  styleState.backgroundColor = val
  applyStyleToConfig()
}

const handleColorSchemeChange = (val: string) => {
  // 如果有 chartConfig，同步更新 color 数组
  if (props.config.chartConfig) {
    try {
      const opt = JSON.parse(props.config.chartConfig)
      if (val && val !== 'default') {
        opt.color = getColorScheme(val).colors
      } else {
        delete opt.color
      }
      emit('update:config', { ...props.config, colorScheme: val, chartConfig: JSON.stringify(opt) })
      return
    } catch { /* ignore */ }
  }
  updateField('colorScheme', val)
}

// 初始化 + watch
watch(() => props.config.chartConfig, () => parseStyleFromConfig(), { immediate: true })

// 同步 JSON 输入
watch(() => props.config.chartConfig, (val) => {
  if (val) {
    try {
      jsonInput.value = JSON.stringify(JSON.parse(val), null, 2)
      jsonError.value = ''
    } catch {
      jsonInput.value = val
    }
  } else {
    jsonInput.value = ''
  }
}, { immediate: true })

const updateField = (field: string, value: any) => {
  emit('update:config', { ...props.config, [field]: value })
}

const formatJson = () => {
  try {
    const parsed = JSON.parse(jsonInput.value)
    jsonInput.value = JSON.stringify(parsed, null, 2)
    jsonError.value = ''
  } catch (e: any) {
    jsonError.value = e.message
  }
}

const handleJsonChange = () => {
  if (!jsonInput.value.trim()) {
    updateField('chartConfig', undefined)
    jsonError.value = ''
    return
  }
  try {
    JSON.parse(jsonInput.value)
    jsonError.value = ''
    updateField('chartConfig', jsonInput.value)
  } catch (e: any) {
    jsonError.value = `JSON 格式错误: ${e.message}`
  }
}

</script>

<style scoped>
.style-config-panel {
  padding: 0;
}
.tab-content {
  padding: 8px 0;
}
.config-section {
  margin-bottom: 14px;
}
.section-label {
  font-size: 12px;
  font-weight: 600;
  color: #666;
  margin-bottom: 6px;
}
.section-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.color-option {
  display: flex;
  align-items: center;
  gap: 8px;
}
.color-preview {
  display: flex;
  gap: 2px;
}
.color-dot {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}
.color-scheme-bar {
  display: flex;
  margin-top: 6px;
  border-radius: 4px;
  overflow: hidden;
  height: 16px;
}
.color-bar-block {
  flex: 1;
  min-width: 0;
}
.json-error {
  margin-top: 4px;
  font-size: 11px;
  color: #d03050;
}
</style>

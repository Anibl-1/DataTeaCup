<template>
  <div class="inline-chart-editor">
    <!-- 头部卡片 -->
    <div class="editor-header">
      <div class="header-top">
        <div class="header-badge" :class="item.mode === 'inline' ? 'badge-inline' : 'badge-ref'">
          <n-icon :size="12"><BarChartOutline /></n-icon>
          {{ item.mode === 'inline' ? '内联图表' : '引用图表' }}
        </div>
        <div class="header-actions">
          <n-tag v-if="item.mode === 'inline' && inlineConfig.chartType" size="tiny" :bordered="false" type="success">
            {{ chartTypeLabel }}
          </n-tag>
        </div>
      </div>
      <n-input
        v-if="item.mode === 'inline'"
        :value="inlineConfig.chartName"
        placeholder="输入图表名称…"
        size="small"
        class="name-input"
        @update:value="handleNameChange"
      >
        <template #prefix><n-icon :size="14" color="#999"><CreateOutline /></n-icon></template>
      </n-input>
      <span v-else class="chart-name">{{ chartDisplayName }}</span>
    </div>

    <!-- 内联图表主体: 4 Tab -->
    <template v-if="item.mode === 'inline'">
      <div class="tab-bar">
        <div
          v-for="tab in mainTabs" :key="tab.key"
          class="tab-item" :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          <n-icon :size="14"><component :is="tab.icon" /></n-icon>
          <span>{{ tab.label }}</span>
        </div>
      </div>

      <n-scrollbar class="tab-body">
        <!-- 数据 -->
        <div v-show="activeTab === 'data'" class="tab-panel">
          <DataConfigPanel
            :config="inlineConfig"
            :data-sources="dataSources"
            @update:config="handleConfigUpdate"
            @data-ready="handleDataReady"
            @params-detected="handleParamsDetected"
          />
        </div>
        <!-- 样式 -->
        <div v-show="activeTab === 'style'" class="tab-panel">
          <StyleConfigPanel
            :config="inlineConfig"
            @update:config="handleConfigUpdate"
          />
        </div>
        <!-- AI 智能 -->
        <div v-show="activeTab === 'ai'" class="tab-panel">
          <div class="ai-hero">
            <div class="ai-hero-icon">
              <n-icon :size="28" color="#8b5cf6"><SparklesOutline /></n-icon>
            </div>
            <div class="ai-hero-text">
              <div class="ai-hero-title">AI 智能助手</div>
              <div class="ai-hero-desc">自动优化图表配色、布局和交互效果</div>
            </div>
          </div>

          <!-- AI 优化样式 -->
          <div class="ai-card">
            <div class="ai-card-header">
              <n-icon :size="14" color="#f59e0b"><ColorPaletteOutline /></n-icon>
              <span>样式优化</span>
            </div>
            <div class="ai-card-desc">基于当前数据和图表类型，AI 自动调优配色、图例、动画等视觉效果</div>
            <n-button
              block type="primary" size="small" ghost
              :loading="optimizing"
              :disabled="!inlineConfig.chartConfig && !inlineConfig.sqlContent"
              @click="handleAiOptimize"
            >
              <template #icon><n-icon><SparklesOutline /></n-icon></template>
              一键优化样式
            </n-button>
          </div>

          <!-- AI 自定义需求 -->
          <div class="ai-card">
            <div class="ai-card-header">
              <n-icon :size="14" color="#3b82f6"><ChatboxEllipsesOutline /></n-icon>
              <span>自定义需求</span>
            </div>
            <n-input
              v-model:value="aiRequirement"
              type="textarea"
              :rows="2"
              size="small"
              placeholder="描述你想要的效果，如：使用暗色主题，圆角柱状图…"
            />
            <n-button
              block size="small" secondary
              :loading="aiGenerating"
              :disabled="!aiRequirement.trim() || (!inlineConfig.chartConfig && !inlineConfig.sqlContent)"
              style="margin-top: 6px"
              @click="handleAiCustom"
            >
              <template #icon><n-icon><SendOutline /></n-icon></template>
              发送给 AI
            </n-button>
          </div>

          <div v-if="aiLastResult" class="ai-result">
            <n-icon :size="12" color="#10b981"><CheckmarkCircleOutline /></n-icon>
            <span>{{ aiLastResult }}</span>
          </div>
        </div>
        <!-- 布局 -->
        <div v-show="activeTab === 'layout'" class="tab-panel">
          <div class="layout-section">
            <div class="section-label">位置</div>
            <div class="layout-grid">
              <div class="layout-field">
                <span class="field-label">X</span>
                <n-input-number
                  :value="item.left" :min="0" :precision="0" :step="5" size="small"
                  :show-button="false" style="flex:1"
                  @update:value="$emit('update:position', { left: $event, top: item.top })"
                />
              </div>
              <div class="layout-field">
                <span class="field-label">Y</span>
                <n-input-number
                  :value="item.top" :min="0" :precision="0" :step="5" size="small"
                  :show-button="false" style="flex:1"
                  @update:value="$emit('update:position', { left: item.left, top: $event })"
                />
              </div>
            </div>
          </div>
          <div class="layout-section">
            <div class="section-label">尺寸</div>
            <div class="layout-grid">
              <div class="layout-field">
                <span class="field-label">W</span>
                <n-input-number
                  :value="item.width" :min="150" :precision="0" :step="10" size="small"
                  :show-button="false" style="flex:1"
                  @update:value="$emit('update:size', { width: $event, height: item.height })"
                />
              </div>
              <div class="layout-field">
                <span class="field-label">H</span>
                <n-input-number
                  :value="item.height" :min="120" :precision="0" :step="10" size="small"
                  :show-button="false" style="flex:1"
                  @update:value="$emit('update:size', { width: item.width, height: $event })"
                />
              </div>
            </div>
          </div>
          <div class="layout-section">
            <div class="section-label">快速尺寸</div>
            <n-select
              v-model:value="quickSize"
              :options="quickSizeOptions"
              placeholder="选择预设尺寸"
              size="small"
              @update:value="handleQuickSize"
            />
          </div>
          <div class="layout-section">
            <div class="section-label">图层顺序</div>
            <div class="layer-buttons">
              <n-button size="small" secondary @click="$emit('layer', 'top')">
                <template #icon><n-icon :size="13"><ArrowUpOutline /></n-icon></template>
                置顶
              </n-button>
              <n-button size="small" secondary @click="$emit('layer', 'up')">
                上移一层
              </n-button>
              <n-button size="small" secondary @click="$emit('layer', 'down')">
                下移一层
              </n-button>
              <n-button size="small" secondary @click="$emit('layer', 'bottom')">
                <template #icon><n-icon :size="13"><ArrowDownOutline /></n-icon></template>
                置底
              </n-button>
            </div>
          </div>
        </div>
      </n-scrollbar>
    </template>

    <!-- 引用图表 -->
    <div v-else class="referenced-info">
      <n-icon :size="32" color="#ccc"><LinkOutline /></n-icon>
      <n-text depth="3" style="font-size: 12px; margin-top: 8px">引用图表的数据和样式在图表管理中配置</n-text>
    </div>

    <!-- 底部操作栏 -->
    <div class="editor-footer">
      <n-button
        v-if="item.mode === 'inline'"
        size="small" quaternary class="footer-btn btn-convert"
        @click="$emit('convert-to-public')"
      >
        <template #icon><n-icon :size="14"><CloudUploadOutline /></n-icon></template>
        转为公共图表
      </n-button>
      <n-popconfirm
        :positive-text="'确认删除'"
        :negative-text="'取消'"
        @positive-click="$emit('remove')"
      >
        <template #trigger>
          <n-button size="small" quaternary class="footer-btn btn-delete">
            <template #icon><n-icon :size="14"><TrashOutline /></n-icon></template>
            删除
          </n-button>
        </template>
        确定要删除此图表吗？
      </n-popconfirm>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, watch, type Component } from 'vue'
import {
  NTabs, NTab, NInputNumber, NInput,
  NSelect, NButton, NButtonGroup,
  NTag, NScrollbar, NText, NIcon,
  NPopconfirm,
  useMessage
} from 'naive-ui'
import {
  BarChartOutline, CreateOutline, TrashOutline,
  SparklesOutline, ColorPaletteOutline, ChatboxEllipsesOutline,
  SendOutline, CheckmarkCircleOutline, LinkOutline,
  CloudUploadOutline, ArrowUpOutline, ArrowDownOutline,
  ServerOutline, BrushOutline, FlashOutline, ResizeOutline
} from '@vicons/ionicons5'
import DataConfigPanel from './DataConfigPanel.vue'
import StyleConfigPanel from './StyleConfigPanel.vue'
import { aiGenerateInlineChart } from '@/api/pageDesigner'
import type { InlineChartConfig } from '@/types/page'
import { createDefaultInlineConfig, getChartTypeLabel } from '@/types/page'

const props = defineProps<{
  item: {
    i: string
    chartId?: number
    mode?: 'inline' | 'referenced'
    inlineConfig?: InlineChartConfig
    left: number
    top: number
    width: number
    height: number
    chartName?: string
  }
  dataSources: Array<{ id: number; name: string }>
}>()

const emit = defineEmits<{
  (e: 'update:config', config: InlineChartConfig): void
  (e: 'update:position', pos: { left: number; top: number }): void
  (e: 'update:size', size: { width: number; height: number }): void
  (e: 'layer', action: 'up' | 'down' | 'top' | 'bottom'): void
  (e: 'remove'): void
  (e: 'convert-to-public'): void
  (e: 'preview-data', itemId: string, data: any[]): void
  (e: 'params-detected', itemId: string, params: string[]): void
}>()

const message = useMessage()
const activeTab = ref('data')
const quickSize = ref<string | null>(null)
const optimizing = ref(false)
const aiGenerating = ref(false)
const aiRequirement = ref('')
const aiLastResult = ref('')

// Tab 定义
const mainTabs: Array<{ key: string; label: string; icon: Component }> = [
  { key: 'data', label: '数据', icon: ServerOutline },
  { key: 'style', label: '样式', icon: BrushOutline },
  { key: 'ai', label: 'AI', icon: FlashOutline },
  { key: 'layout', label: '布局', icon: ResizeOutline }
]

watch(() => props.item.i, () => {
  activeTab.value = 'data'
})

const inlineConfig = computed(() =>
  props.item.inlineConfig || createDefaultInlineConfig(props.item.inlineConfig?.chartType || 'bar')
)

const chartDisplayName = computed(() => {
  if (props.item.mode === 'inline') {
    return props.item.inlineConfig?.chartName || '未命名图表'
  }
  return props.item.chartName || '引用图表'
})

const chartTypeLabel = computed(() =>
  getChartTypeLabel(inlineConfig.value.chartType)
)

const quickSizeOptions = [
  { label: '小 (300×200)', value: '300x200' },
  { label: '中 (400×300)', value: '400x300' },
  { label: '大 (600×400)', value: '600x400' },
  { label: '超大 (800×600)', value: '800x600' }
]

const handleNameChange = (name: string) => {
  emit('update:config', { ...inlineConfig.value, chartName: name })
}
const handleConfigUpdate = (config: InlineChartConfig) => {
  emit('update:config', config)
}
const handleDataReady = (data: any[]) => {
  emit('preview-data', props.item.i, data)
}
const handleParamsDetected = (params: string[]) => {
  emit('params-detected', props.item.i, params)
}
const handleQuickSize = (val: string | null) => {
  if (!val) return
  const [w, h] = val.split('x').map(Number)
  emit('update:size', { width: w, height: h })
  quickSize.value = null
}

// ---- AI 功能 ----
const doAiOptimize = async (requirement: string) => {
  const config = inlineConfig.value
  const currentConfig = config.chartConfig || '{}'
  const res = await aiGenerateInlineChart({
    requirement,
    dataSourceId: config.dataSourceId,
    context: { preferredChartType: config.chartType }
  })
  if (res?.data?.chartConfig?.chartConfig) {
    const optimized = res.data.chartConfig.chartConfig
    const newJson = typeof optimized === 'string' ? optimized : JSON.stringify(optimized)
    emit('update:config', { ...config, chartConfig: newJson })
    return true
  } else if (res?.data?.content) {
    const match = res.data.content.match(/```json\s*([\s\S]*?)```/)
    if (match?.[1]) {
      try {
        const parsed = JSON.parse(match[1].trim())
        emit('update:config', { ...config, chartConfig: JSON.stringify(parsed) })
        return true
      } catch { /* fall through */ }
    }
  }
  return false
}

const handleAiOptimize = async () => {
  optimizing.value = true
  aiLastResult.value = ''
  try {
    const config = inlineConfig.value
    const req = `请优化以下${config.chartType}图表的ECharts样式配置，使其更美观专业。`
      + `\n当前配置: ${(config.chartConfig || '{}').substring(0, 500)}`
      + `\n要求: 1.保持数据不变 2.优化颜色和视觉效果 3.优化图例、提示框、标签样式 4.返回完整的ECharts option JSON`
    const ok = await doAiOptimize(req)
    if (ok) {
      aiLastResult.value = 'AI 样式优化已应用'
      message.success('AI 样式优化完成')
    } else {
      message.warning('AI 未返回有效的优化结果')
    }
  } catch (e: any) {
    message.error(e?.message || 'AI 优化失败')
  } finally {
    optimizing.value = false
  }
}

const handleAiCustom = async () => {
  aiGenerating.value = true
  aiLastResult.value = ''
  try {
    const config = inlineConfig.value
    const req = aiRequirement.value
      + `\n当前图表类型: ${config.chartType}`
      + `\n当前配置: ${(config.chartConfig || '{}').substring(0, 500)}`
      + `\n要求: 返回完整的ECharts option JSON`
    const ok = await doAiOptimize(req)
    if (ok) {
      aiLastResult.value = '自定义优化已应用'
      message.success('AI 优化完成')
      aiRequirement.value = ''
    } else {
      message.warning('AI 未返回有效结果')
    }
  } catch (e: any) {
    message.error(e?.message || 'AI 优化失败')
  } finally {
    aiGenerating.value = false
  }
}
</script>

<style scoped>
.inline-chart-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* ---- 头部 ---- */
.editor-header {
  padding: 10px 0 12px;
  border-bottom: 1px solid #eee;
}
.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.header-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  letter-spacing: 0.3px;
}
.badge-inline {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  color: #92400e;
}
.badge-ref {
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
  color: #1e40af;
}
.header-actions {
  display: flex;
  gap: 2px;
}
.name-input :deep(.n-input__border) {
  border-color: #e5e7eb;
}
.name-input :deep(.n-input__state-border) {
  border-color: transparent;
}
.chart-name {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---- 自定义 Tab 栏 ---- */
.tab-bar {
  display: flex;
  gap: 0;
  border-bottom: 1px solid #eee;
  background: #fafbfc;
  margin: 0 -2px;
  padding: 0 2px;
}
.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 0;
  font-size: 12px;
  font-weight: 500;
  color: #888;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
  user-select: none;
}
.tab-item:hover {
  color: #555;
  background: #f0f2f5;
}
.tab-item.active {
  color: #18a058;
  border-bottom-color: #18a058;
  font-weight: 600;
}

/* ---- Tab 内容 ---- */
.tab-body {
  flex: 1;
  min-height: 0;
}
.tab-panel {
  padding: 6px 0;
}

/* ---- AI 面板 ---- */
.ai-hero {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
  border-radius: 8px;
  margin-bottom: 10px;
}
.ai-hero-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(139, 92, 246, 0.12);
  flex-shrink: 0;
}
.ai-hero-title {
  font-size: 13px;
  font-weight: 700;
  color: #5b21b6;
}
.ai-hero-desc {
  font-size: 11px;
  color: #7c3aed;
  margin-top: 2px;
}
.ai-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 8px;
  background: #fff;
  transition: border-color 0.2s;
}
.ai-card:hover {
  border-color: #d4d4d8;
}
.ai-card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;
}
.ai-card-desc {
  font-size: 11px;
  color: #9ca3af;
  margin-bottom: 8px;
  line-height: 1.5;
}
.ai-result {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #10b981;
  padding: 6px 10px;
  background: #ecfdf5;
  border-radius: 6px;
}

/* ---- 布局面板 ---- */
.layout-section {
  margin-bottom: 14px;
}
.section-label {
  font-size: 11px;
  font-weight: 600;
  color: #9ca3af;
  margin-bottom: 6px;
}
.layout-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}
.layout-field {
  display: flex;
  align-items: center;
  gap: 4px;
}
.field-label {
  font-size: 11px;
  font-weight: 700;
  color: #a1a1aa;
  width: 18px;
  text-align: center;
  flex-shrink: 0;
}
.layer-buttons {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
}
.layer-buttons :deep(.n-button) {
  font-size: 12px;
  border-radius: 6px;
}

/* ---- 引用图表 ---- */
.referenced-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 0;
  flex: 1;
}

/* ---- 底部操作栏 ---- */
.editor-footer {
  display: flex;
  gap: 6px;
  padding: 8px 0 2px;
  border-top: 1px solid #eee;
  margin-top: auto;
}
.footer-btn {
  flex: 1;
  font-size: 12px;
  border-radius: 6px;
}
.btn-convert {
  color: #18a058;
}
.btn-convert:hover {
  background: #f0faf4;
}
.btn-delete {
  color: #d03050;
  flex: 0 0 auto;
}
.btn-delete:hover {
  background: #fef2f2;
}
</style>

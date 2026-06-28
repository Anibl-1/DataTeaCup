<template>
  <div class="right-panel-content">
    <!-- 图表预览卡片 -->
    <n-card class="preview-card" :segmented="{ content: true }">
      <template #header>
        <div class="preview-header">
          <span>📊 图表预览</span>
          <n-space>
            <n-button v-if="generatedConfig" size="small" :loading="previewLoading" @click="handleRefreshPreview">
              <template #icon><n-icon><RefreshOutline /></n-icon></template>
              刷新
            </n-button>
            <n-button v-if="generatedConfig" size="small" @click="$emit('fullscreen')">
              <template #icon><n-icon><ExpandOutline /></n-icon></template>
              全屏
            </n-button>
          </n-space>
        </div>
      </template>
      <div class="chart-preview-area">
        <div v-if="generating || previewLoading" class="loading-overlay">
          <n-spin size="large">
            <template #description>
              <div style="text-align: center;">
                <div>{{ generating ? generateStatus : '正在加载数据...' }}</div>
                <n-text v-if="generating" depth="3" style="font-size: 12px; margin-top: 4px;">
                  通常需要 10-30 秒，请耐心等待
                </n-text>
              </div>
            </template>
          </n-spin>
        </div>
        <div v-if="!generatedConfig && !generating && !previewLoading" class="empty-preview">
          <n-empty description="配置需求后点击「AI 生成图表」">
            <template #icon><n-icon size="80" color="#d0d0d0"><BarChartOutline /></n-icon></template>
            <template #extra><n-text depth="3">AI 将根据您的需求自动设计图表</n-text></template>
          </n-empty>
        </div>
        <div v-show="generatedConfig" ref="chartContainerRef" class="chart-container"></div>
      </div>
    </n-card>

    <!-- 配置编辑区 -->
    <n-card v-if="generatedConfig" class="config-card" size="small">
      <n-tabs type="line" animated>
        <n-tab-pane name="basic" tab="基本信息">
          <n-grid :cols="2" :x-gap="16" :y-gap="12">
            <n-gi>
              <n-form-item label="图表名称" label-placement="left">
                <n-input v-model:value="configChartName" placeholder="输入图表名称" />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="图表类型" label-placement="left">
                <n-select v-model:value="configChartType" :options="chartTypeOptions" />
              </n-form-item>
            </n-gi>
            <n-gi :span="2">
              <n-form-item label="图表描述" label-placement="left">
                <n-input v-model:value="configDescription" placeholder="图表描述" />
              </n-form-item>
            </n-gi>
          </n-grid>
        </n-tab-pane>
        <n-tab-pane name="filters" tab="查询条件">
          <div class="filters-section">
            <n-alert type="info" style="margin-bottom: 12px;">
              <template #header>使用说明</template>
              1. 在下方定义参数（如 startDate、endDate）<br/>
              2. 在 SQL 中使用 <code>${参数名}</code> 引用<br/>
              3. 填写参数值后点击「应用参数并刷新」
            </n-alert>
            <n-card title="参数定义" size="small" style="margin-bottom: 12px;">
              <n-dynamic-input :value="queryParams" :on-create="() => ({ name: '', label: '', type: 'text', defaultValue: '' })" @update:value="$emit('update:queryParams', $event)">
                <template #default="{ value }">
                  <n-space style="width: 100%;">
                    <n-input v-model:value="value.name" placeholder="参数名(英文)" style="width: 120px;" />
                    <n-input v-model:value="value.label" placeholder="显示名称" style="width: 120px;" />
                    <n-select v-model:value="value.type" :options="paramTypeOptions" placeholder="类型" style="width: 100px;" />
                    <n-input v-model:value="value.defaultValue" placeholder="默认值" style="width: 120px;" />
                  </n-space>
                </template>
              </n-dynamic-input>
            </n-card>
            <n-card v-if="queryParams.length > 0" title="参数值" size="small">
              <n-grid :cols="2" :x-gap="12" :y-gap="8">
                <n-gi v-for="param in queryParams" :key="param.name">
                  <n-form-item :label="param.label || param.name" label-placement="left">
                    <n-date-picker v-if="param.type === 'date'" :value="paramValues[param.name]" type="date" style="width: 100%;" @update:value="(v: any) => emit('updateParamValue', param.name, v)" />
                    <n-input-number v-else-if="param.type === 'number'" :value="paramValues[param.name]" style="width: 100%;" @update:value="(v: any) => emit('updateParamValue', param.name, v)" />
                    <n-input v-else :value="paramValues[param.name]" :placeholder="param.defaultValue || '请输入'" @update:value="(v: string) => emit('updateParamValue', param.name, v)" />
                  </n-form-item>
                </n-gi>
              </n-grid>
              <n-button type="primary" style="margin-top: 12px;" :loading="testingSql" :disabled="!generatedConfig" @click="$emit('testSql')">
                应用参数并刷新图表
              </n-button>
            </n-card>
            <n-empty v-else description="暂无查询参数，点击上方「添加」按钮创建" style="margin-top: 20px;" />
          </div>
        </n-tab-pane>
        <n-tab-pane name="sql" tab="SQL 查询">
          <div class="sql-editor-wrapper">
            <n-input v-model:value="configSql" type="textarea" :rows="6" placeholder="SQL 查询语句，支持 ${参数名} 引用查询条件" style="font-family: 'Fira Code', monospace;" />
            <n-space style="margin-top: 8px;">
              <n-button size="small" type="primary" :loading="testingSql" @click="$emit('testSql')">测试 SQL</n-button>
              <n-text v-if="chartData.length > 0" depth="3">查询到 {{ chartData.length }} 条数据</n-text>
            </n-space>
          </div>
        </n-tab-pane>
        <n-tab-pane name="echarts" tab="图表配置">
          <n-input v-model:value="localEchartsConfigStr" type="textarea" :rows="10" placeholder="ECharts 配置 JSON" style="font-family: 'Fira Code', monospace;" />
          <n-button size="small" type="primary" style="margin-top: 8px;" @click="handleApplyConfig">应用配置</n-button>
        </n-tab-pane>
        <n-tab-pane name="ai" tab="AI 分析">
          <div class="ai-response-content" v-html="formattedAiResponse"></div>
        </n-tab-pane>
      </n-tabs>
    </n-card>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { logger } from '@/utils/logger'
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useMessage } from 'naive-ui'
import { RefreshOutline, ExpandOutline, BarChartOutline } from '@vicons/ionicons5'
import echarts from '@/utils/echarts'
import { marked } from 'marked'
import { buildDefaultChartOption, resolveFieldMapping, fillChartData } from './chartOptionBuilder'

export interface GeneratedChartConfig {
  chartName: string
  chartCode: string
  chartType: string
  description: string
  sql: string
  chartConfig: Record<string, any>
  dataMapping: Record<string, any>
}

const props = defineProps<{
  generatedConfig: GeneratedChartConfig | null
  chartTypeOptions: { label: string; value: string }[]
  generating: boolean
  generateStatus: string
  testingSql: boolean
  chartData: any[]
  aiResponse: string
  echartsConfigStr: string
  queryParams: { name: string; label: string; type: string; defaultValue: string }[]
  paramValues: Record<string, any>
}>()

const emit = defineEmits<{
  (e: 'fullscreen'): void
  (e: 'testSql'): void
  (e: 'update:echartsConfigStr', value: string): void
  (e: 'update:queryParams', value: { name: string; label: string; type: string; defaultValue: string }[]): void
  (e: 'applyConfig', config: Record<string, any>): void
  (e: 'chartInstanceReady', instance: echarts.ECharts | null): void
  (e: 'updateConfig', field: string, value: any): void
  (e: 'updateParamValue', key: string, value: any): void
}>()

// Writable proxies to avoid direct prop mutation
const configChartName = computed({
  get: () => props.generatedConfig?.chartName ?? '',
  set: (v: string) => emit('updateConfig', 'chartName', v)
})
const configChartType = computed({
  get: () => props.generatedConfig?.chartType ?? '',
  set: (v: string) => emit('updateConfig', 'chartType', v)
})
const configDescription = computed({
  get: () => props.generatedConfig?.description ?? '',
  set: (v: string) => emit('updateConfig', 'description', v)
})
const configSql = computed({
  get: () => props.generatedConfig?.sql ?? '',
  set: (v: string) => emit('updateConfig', 'sql', v)
})

const message = useMessage()
const chartContainerRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null
const previewLoading = ref(false)

const paramTypeOptions = [
  { label: '文本', value: 'text' },
  { label: '数字', value: 'number' },
  { label: '日期', value: 'date' }
]

const localEchartsConfigStr = computed({
  get: () => props.echartsConfigStr,
  set: (val: string) => emit('update:echartsConfigStr', val)
})

const formattedAiResponse = computed(() => {
  if (!props.aiResponse) return '<p style="color: var(--text-tertiary);">暂无 AI 分析内容</p>'
  try { return marked(props.aiResponse) } catch { return props.aiResponse.replace(/\n/g, '<br>') }
})

let renderRetryCount = 0
const MAX_RENDER_RETRIES = 20

const renderChart = (data: any[]) => {
  if (!props.generatedConfig || !data?.length) { renderRetryCount = 0; return }
  if (!chartContainerRef.value || chartContainerRef.value.getBoundingClientRect().width === 0) {
    if (++renderRetryCount > MAX_RENDER_RETRIES) { renderRetryCount = 0; return }
    setTimeout(() => renderChart(data), 100); return
  }
  renderRetryCount = 0
  if (chartInstance) { chartInstance.dispose(); chartInstance = null }

  try {
    if (!data[0] || typeof data[0] !== 'object') { message.error('图表数据格式无效'); return }
    chartInstance = echarts.init(chartContainerRef.value)
    emit('chartInstanceReady', chartInstance)

    const config = props.generatedConfig!
    const chartType = config.chartType || 'bar'
    const { xField, yField } = resolveFieldMapping(config.dataMapping || {}, Object.keys(data[0]))

    let option: any
    if (config.chartConfig && Object.keys(config.chartConfig).length > 0) {
      option = JSON.parse(JSON.stringify(config.chartConfig))
      fillChartData(option, chartType, data, xField, yField)
    } else {
      option = buildDefaultChartOption(chartType, data, xField, yField, config.chartName)
    }
    chartInstance.setOption(option)
    setTimeout(() => chartInstance?.resize(), 100)
  } catch (error) {
    logger.error('图表渲染错误:', error)
    message.error('图表渲染失败，请检查配置')
  }
}

const handleRefreshPreview = async () => {
  if (props.chartData.length > 0) {
    previewLoading.value = true
    await nextTick(); await nextTick()
    setTimeout(() => { renderChart(props.chartData); previewLoading.value = false }, 50)
  } else { emit('testSql') }
}

const handleApplyConfig = () => {
  if (!props.generatedConfig) return
  try {
    emit('applyConfig', JSON.parse(localEchartsConfigStr.value))
    message.success('配置已应用')
    handleRefreshPreview()
  } catch { message.error('JSON 格式错误，请检查配置') }
}

watch(() => props.chartData, (newData) => {
  if (newData?.length && props.generatedConfig) {
    nextTick(() => nextTick(() => setTimeout(() => renderChart(newData), 50)))
  }
})

const disposeChart = () => { if (chartInstance) { chartInstance.dispose(); chartInstance = null } }
const getChartInstance = () => chartInstance

const handleResize = () => chartInstance?.resize()
onMounted(() => window.addEventListener('resize', handleResize))
onUnmounted(() => { window.removeEventListener('resize', handleResize); disposeChart() })

defineExpose({ renderChart, getChartInstance, disposeChart })
</script>

<style scoped>
.right-panel-content { display: flex; flex-direction: column; gap: var(--dp-spacing-md, 16px); height: 100%; }
.preview-card { flex: 0 0 auto; display: flex; flex-direction: column; height: 450px; min-height: 400px; }
.preview-card :deep(.n-card__content) { flex: 1; display: flex; flex-direction: column; padding: 0 !important; }
.preview-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.chart-preview-area { flex: 1; min-height: 400px; height: 400px; display: flex; align-items: center; justify-content: center; padding: var(--dp-spacing-md, 16px); }
.loading-overlay { display: flex; align-items: center; justify-content: center; width: 100%; height: 100%; min-height: 400px; }
.empty-preview { display: flex; flex-direction: column; align-items: center; justify-content: center; width: 100%; height: 100%; min-height: 400px; }
.chart-container { width: 100%; height: 400px; min-height: 400px; }
.config-card { flex-shrink: 0; min-height: 200px; }
.config-card :deep(.n-tabs-pane-wrapper) { min-height: 150px; }
.config-card :deep(.n-tab-pane) { padding: 12px 0; }
.sql-editor-wrapper { display: flex; flex-direction: column; }
.filters-section { padding: var(--dp-spacing-sm, 8px) 0; }
.ai-response-content { max-height: 200px; overflow-y: auto; font-size: var(--dp-font-md, 14px); line-height: 1.6; color: #666; }
.ai-response-content :deep(pre) { background: #f5f5f5; padding: 12px; border-radius: var(--dp-radius-sm, 4px); overflow-x: auto; }
.ai-response-content :deep(code) { background: #f0f0f0; padding: 2px 6px; border-radius: 3px; }
.ai-response-content::-webkit-scrollbar { width: 6px; }
.ai-response-content::-webkit-scrollbar-thumb { background: #d0d0d0; border-radius: 3px; }
.ai-response-content::-webkit-scrollbar-thumb:hover { background: #b0b0b0; }

</style>

<style>
/* AiChartPreview 深色模式（非 scoped） */
html.dark .preview-container { background: #0f172a !important; }
html.dark .preview-header { color: #e2e8f0 !important; }
html.dark .preview-desc { color: #94a3b8 !important; }
</style>

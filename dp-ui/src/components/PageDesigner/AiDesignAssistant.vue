<template>
  <div class="ai-assistant">
    <!-- 数据源选择 -->
    <div class="ai-section">
      <div class="section-label">数据源</div>
      <n-select
        v-model:value="selectedDataSourceId"
        :options="dataSourceOptions"
        placeholder="选择数据源（可选）"
        size="small"
        clearable
        @update:value="handleDataSourceChange"
      />
    </div>

    <!-- 表选择 -->
    <div v-if="selectedDataSourceId" class="ai-section">
      <div class="section-label">数据表</div>
      <n-select
        v-model:value="selectedTable"
        :options="tableOptions"
        placeholder="选择数据表（可选）"
        size="small"
        clearable
        filterable
        :loading="loadingTables"
      />
    </div>

    <!-- 自然语言输入 -->
    <div class="ai-section">
      <div class="section-label">描述需求</div>
      <n-input
        v-model:value="requirement"
        type="textarea"
        placeholder="例如：用柱状图展示各部门月度销售额对比"
        :rows="3"
        size="small"
      />
    </div>

    <!-- 偏好设置 -->
    <div class="ai-section preference-row">
      <n-select
        v-model:value="preferredType"
        :options="chartTypeOptions"
        placeholder="图表类型(可选)"
        size="small"
        clearable
        style="flex: 1"
      />
      <n-select
        v-model:value="colorTheme"
        :options="colorOptions"
        placeholder="配色"
        size="small"
        clearable
        style="flex: 1"
      />
    </div>

    <!-- 生成按钮和停止按钮 -->
    <div class="button-row">
      <n-button
        v-if="!isStreaming"
        type="primary"
        block
        :loading="generating"
        :disabled="!requirement.trim()"
        @click="handleGenerate"
      >
        <template #icon><n-icon><SparklesOutline /></n-icon></template>
        AI 生成图表
      </n-button>
      <n-button
        v-else
        type="error"
        block
        @click="stopStreaming"
      >
        <template #icon><n-icon><StopCircleOutline /></n-icon></template>
        停止生成
      </n-button>
    </div>

    <!-- 流式输出预览 -->
    <div v-if="isStreaming && streamingContent" class="streaming-preview">
      <div class="streaming-header">
        <n-icon size="14" color="#6366f1"><PulseOutline /></n-icon>
        <span>正在生成...</span>
      </div>
      <div class="streaming-content">{{ streamingContent }}</div>
    </div>

    <!-- 生成结果 -->
    <div v-if="result" class="ai-result">
      <n-alert :type="result.success ? 'success' : 'error'" :title="result.success ? '生成成功' : '生成失败'" style="margin-bottom: 12px">
        <template v-if="result.success && result.chartConfig">
          <div class="result-info">
            <span>{{ result.chartConfig.chartName }}</span>
            <n-tag size="tiny">{{ result.chartConfig.chartType }}</n-tag>
          </div>
        </template>
        <template v-else>{{ result.error || '未知错误' }}</template>
      </n-alert>
      <n-button
        v-if="result.success && result.chartConfig"
        type="primary"
        block
        size="small"
        @click="handleAddToCanvas"
      >
        添加到画布
      </n-button>
    </div>

    <!-- 连接状态 -->
    <div v-if="connectionState === 'error'" class="connection-error">
      <n-alert type="error" title="连接失败">
        <template #default>
          连接中断，请检查网络后重试
          <n-button size="tiny" style="margin-left: 8px;" @click="handleGenerate">重试</n-button>
        </template>
      </n-alert>
    </div>

    <!-- 快捷建议 -->
    <div class="ai-section" style="margin-top: 16px;">
      <div class="section-label">快捷建议</div>
      <div class="suggestions">
        <div class="suggestion-chip" @click="applySuggestion('展示各类别销售额占比的饼图')">
          📊 销售占比饼图
        </div>
        <div class="suggestion-chip" @click="applySuggestion('按月份展示订单趋势的折线图')">
          📈 月度趋势
        </div>
        <div class="suggestion-chip" @click="applySuggestion('各部门业绩排名柱状图')">
          📋 业绩排名
        </div>
        <div class="suggestion-chip" @click="applySuggestion('展示地域分布数据的热力图')">
          🗺️ 地域分布
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, onUnmounted } from 'vue'
import { NInput, NSelect, NButton, NIcon, NAlert, NTag, useMessage } from 'naive-ui'
import { SparklesOutline, StopCircleOutline, PulseOutline } from '@vicons/ionicons5'
import { CHART_TYPES } from '@/types/chart'
import { getColorSchemeOptions } from '@/utils/chartColorSchemes'
import { aiGenerateInlineChart } from '@/api/pageDesigner'
import { getDataSourceTables } from '@/api/dataSource'
import { streamChart, type SseConnection, type SseConnectionState } from '@/utils/sseClient'
import type { InlineChartConfig } from '@/types/page'

const props = defineProps<{
  dataSources: Array<{ id: number; name: string }>
}>()

const emit = defineEmits<{
  (e: 'add-inline-chart', config: InlineChartConfig): void
}>()

const message = useMessage()

const requirement = ref('')
const selectedDataSourceId = ref<number | null>(null)
const selectedTable = ref<string | null>(null)
const tableOptions = ref<Array<{ label: string; value: string }>>([])
const loadingTables = ref(false)
const preferredType = ref<string | null>(null)
const colorTheme = ref<string | null>(null)
const generating = ref(false)
const result = ref<any>(null)

// 流式输出相关状态
const streamingEnabled = ref(true)
const isStreaming = ref(false)
const streamingContent = ref('')
const currentConnection = ref<SseConnection | null>(null)
const connectionState = ref<SseConnectionState>('disconnected')

const dataSourceOptions = computed(() =>
  props.dataSources.map(ds => ({ label: ds.name, value: ds.id }))
)

const chartTypeOptions = CHART_TYPES.map(t => ({ label: t.label, value: t.value }))
const colorOptions = getColorSchemeOptions().map(s => ({ label: s.label, value: s.value }))

const handleDataSourceChange = async (dsId: number | null) => {
  selectedTable.value = null
  tableOptions.value = []
  if (!dsId) return
  loadingTables.value = true
  try {
    const res = await getDataSourceTables(dsId)
    tableOptions.value = (res.data || []).map((t: any) => ({
      label: t.tableName,
      value: t.tableName
    }))
  } catch {
    message.warning('加载表列表失败')
  } finally {
    loadingTables.value = false
  }
}

// 停止流式生成
const stopStreaming = () => {
  if (currentConnection.value) {
    currentConnection.value.abort()
    currentConnection.value = null
  }
  isStreaming.value = false
  generating.value = false
  // 保留已接收的内容
  if (streamingContent.value) {
    message.info('已停止生成，保留已接收内容')
  }
}

// 流式生成图表
const handleGenerateStreaming = () => {
  if (!requirement.value.trim() || !selectedDataSourceId.value) return
  
  generating.value = true
  isStreaming.value = true
  streamingContent.value = ''
  result.value = null
  
  let fullRequirement = requirement.value
  if (selectedTable.value) {
    fullRequirement = `基于数据表 ${selectedTable.value}，${fullRequirement}`
  }
  
  currentConnection.value = streamChart(
    {
      requirement: fullRequirement,
      dataSourceId: selectedDataSourceId.value
    },
    {
      onStart: () => {
        connectionState.value = 'connected'
      },
      onToken: (token) => {
        streamingContent.value += token
      },
      onComplete: (fullContent) => {
        isStreaming.value = false
        generating.value = false
        currentConnection.value = null
        connectionState.value = 'disconnected'
        
        // 尝试解析 JSON 配置
        try {
          const jsonMatch = fullContent.match(/```json\s*([\s\S]*?)```/i)
          if (jsonMatch && jsonMatch[1]) {
            const chartConfig = JSON.parse(jsonMatch[1])
            result.value = {
              success: true,
              chartConfig: {
                chartName: chartConfig.chartName || '新建图表',
                chartType: chartConfig.chartType || 'bar',
                sql: chartConfig.sql || '',
                chartConfig: chartConfig.chartConfig || {},
                dataMapping: chartConfig.dataMapping || {},
                description: chartConfig.description
              }
            }
          } else {
            result.value = { success: false, error: '无法解析图表配置' }
          }
        } catch (e) {
          result.value = { success: false, error: '解析配置失败' }
        }
        
        streamingContent.value = ''
      },
      onError: (error) => {
        isStreaming.value = false
        generating.value = false
        currentConnection.value = null
        connectionState.value = 'error'
        result.value = { success: false, error: error.message }
        streamingContent.value = ''
      },
      onStateChange: (state) => {
        connectionState.value = state
      },
      onAbort: () => {
        isStreaming.value = false
        generating.value = false
        connectionState.value = 'aborted'
      }
    }
  )
}

const handleGenerate = async () => {
  if (!requirement.value.trim()) return
  
  // 流式模式
  if (streamingEnabled.value && selectedDataSourceId.value) {
    handleGenerateStreaming()
    return
  }
  
  // 非流式模式（原有逻辑）
  generating.value = true
  result.value = null
  try {
    let fullRequirement = requirement.value
    if (selectedTable.value) {
      fullRequirement = `基于数据表 ${selectedTable.value}，${fullRequirement}`
    }
    const res = await aiGenerateInlineChart({
      requirement: fullRequirement,
      ...(selectedDataSourceId.value ? { dataSourceId: selectedDataSourceId.value } : {}),
      context: {
        preferredChartType: preferredType.value || undefined,
        colorTheme: colorTheme.value || undefined,
        tableName: selectedTable.value || undefined
      } as any
    })
    if (res?.data) {
      result.value = res.data
    } else {
      result.value = { success: false, error: '无响应数据' }
    }
  } catch (err: any) {
    result.value = { success: false, error: err?.message || '请求失败' }
  } finally {
    generating.value = false
  }
}

const handleAddToCanvas = () => {
  if (!result.value?.chartConfig) return
  const cfg = result.value.chartConfig
  const inlineConfig: InlineChartConfig = {
    chartName: cfg.chartName || '新建图表',
    chartType: cfg.chartType || 'bar',
    ...(selectedDataSourceId.value ? { dataSourceId: selectedDataSourceId.value } : {}),
    sqlContent: cfg.sql || '',
    chartConfig: JSON.stringify(cfg.chartConfig || {}),
    ...(cfg.dataMapping ? {
      fieldMapping: {
        xAxis: cfg.dataMapping.xField,
        yAxis: cfg.dataMapping.yField,
        nameField: cfg.dataMapping.nameField,
        valueField: cfg.dataMapping.valueField
      }
    } : {}),
    colorScheme: colorTheme.value || 'default',
    description: cfg.description
  }
  emit('add-inline-chart', inlineConfig)
  message.success('已添加到画布')
}

const applySuggestion = (text: string) => {
  requirement.value = text
}

// 组件卸载时清理连接
onUnmounted(() => {
  if (currentConnection.value) {
    currentConnection.value.close()
    currentConnection.value = null
  }
})
</script>

<style scoped>
.ai-assistant {
  padding: 4px 0;
}
.ai-section {
  margin-bottom: 12px;
}
.section-label {
  font-size: 12px;
  font-weight: 600;
  color: #666;
  margin-bottom: 6px;
}
.preference-row {
  display: flex;
  gap: 8px;
}
.ai-result {
  margin-top: 12px;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
}
.result-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.suggestion-chip {
  padding: 6px 12px;
  background: #f0f4ff;
  border: 1px solid #e0e8ff;
  border-radius: 16px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}
.suggestion-chip:hover {
  background: #e0e8ff;
  border-color: #5470c6;
  transform: translateY(-1px);
}

/* 流式输出相关样式 */
.streaming-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
}
.toggle-label {
  font-size: 12px;
  color: #666;
}
.button-row {
  margin-bottom: 12px;
}
.streaming-preview {
  margin-top: 12px;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}
.streaming-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #6366f1;
  margin-bottom: 8px;
}
.streaming-content {
  font-size: 13px;
  color: #374151;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 200px;
  overflow-y: auto;
}
.connection-error {
  margin-top: 12px;
}
</style>

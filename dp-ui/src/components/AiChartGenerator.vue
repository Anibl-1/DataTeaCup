<template>
  <n-modal
    v-model:show="showModal"
    preset="card"
    title="AI 智能生成图表"
    style="width: 900px; max-width: 95vw;"
    :segmented="{ content: 'soft', footer: 'soft' }"
    :mask-closable="false"
  >
    <template #header-extra>
      <n-space align="center">
        <!-- 流式模式开关 -->
        <n-switch v-model:value="streamingEnabled" size="small" :disabled="isStreaming">
          <template #checked>流式</template>
          <template #unchecked>普通</template>
        </n-switch>
        <n-tag type="info" size="small">
          <template #icon>
            <n-icon><SparklesOutline /></n-icon>
          </template>
          AI 助手
        </n-tag>
        <!-- 连接状态 -->
        <n-tag v-if="isStreaming" type="success" size="small">
          <template #icon>
            <n-icon size="12"><PulseOutline /></n-icon>
          </template>
          流式输出中
        </n-tag>
      </n-space>
    </template>

    <div class="ai-chart-generator">
      <!-- 步骤指示器 -->
      <n-steps :current="currentStep" size="small" style="margin-bottom: 24px;">
        <n-step title="描述需求" description="告诉AI你想要什么图表" />
        <n-step title="预览配置" description="查看并调整AI生成的配置" />
        <n-step title="创建图表" description="确认并创建图表" />
      </n-steps>

      <!-- 第一步：描述需求 -->
      <div v-if="currentStep === 1" class="step-content">
        <n-form ref="formRef" :model="formData" label-placement="top">
          <n-form-item label="选择数据源" required>
            <n-select
              v-model:value="formData.dataSourceId"
              :options="dataSourceOptions"
              placeholder="请选择数据源"
              filterable
              :loading="loadingDataSources"
            />
          </n-form-item>

          <n-form-item label="图表需求描述" required>
            <n-input
              v-model:value="formData.requirement"
              type="textarea"
              placeholder="请描述您想要的图表，例如：&#10;- 按月份统计销售额的折线图&#10;- 各部门人数占比的饼图&#10;- 产品销量TOP10的柱状图"
              :rows="5"
              :maxlength="1000"
              show-count
            />
          </n-form-item>

          <n-grid :cols="2" :x-gap="16">
            <n-gi>
              <n-form-item label="偏好图表类型">
                <n-select
                  v-model:value="formData.preferredChartType"
                  :options="chartTypeOptions"
                  placeholder="可选，AI会根据数据自动推荐"
                  clearable
                />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="配色主题">
                <n-select
                  v-model:value="formData.colorTheme"
                  :options="colorThemeOptions"
                  placeholder="默认使用专业配色"
                  clearable
                />
              </n-form-item>
            </n-gi>
          </n-grid>

          <!-- 快捷模板 -->
          <n-form-item label="快捷模板">
            <n-space>
              <n-tag
                v-for="template in quickTemplates"
                :key="template.label"
                :bordered="false"
                checkable
                :checked="formData.requirement === template.value"
                style="cursor: pointer;"
                @click="formData.requirement = template.value"
              >
                {{ template.label }}
              </n-tag>
            </n-space>
          </n-form-item>
        </n-form>
      </div>

      <!-- 第二步：预览配置 -->
      <div v-if="currentStep === 2" class="step-content">
        <n-spin :show="generating && !isStreaming">
          <template #description>AI 正在分析数据并生成图表配置...</template>
          
          <!-- 流式输出预览 -->
          <div v-if="isStreaming && streamingContent" class="streaming-preview">
            <div class="streaming-header">
              <n-icon size="14" color="#6366f1"><PulseOutline /></n-icon>
              <span>正在生成...</span>
              <n-button size="tiny" type="error" style="margin-left: auto;" @click="stopStreaming">
                <template #icon><n-icon size="12"><StopCircleOutline /></n-icon></template>
                停止
              </n-button>
            </div>
            <div class="streaming-content">{{ streamingContent }}</div>
          </div>
          
          <div v-if="generatedConfig" class="generated-config">
            <!-- AI 回复内容 -->
            <n-card size="small" title="AI 分析结果" style="margin-bottom: 16px;">
              <div class="ai-response" v-html="formatAiResponse(aiResponse)"></div>
            </n-card>

            <!-- 生成的配置 -->
            <n-grid :cols="2" :x-gap="16">
              <n-gi>
                <n-form-item label="图表名称">
                  <n-input v-model:value="generatedConfig.chartName" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="图表类型">
                  <n-select
                    v-model:value="generatedConfig.chartType"
                    :options="chartTypeOptions"
                  />
                </n-form-item>
              </n-gi>
            </n-grid>

            <n-form-item label="图表描述">
              <n-input v-model:value="generatedConfig.description" type="textarea" :rows="2" />
            </n-form-item>

            <n-form-item label="SQL 查询语句">
              <n-input
                v-model:value="generatedConfig.sql"
                type="textarea"
                :rows="4"
                style="font-family: monospace;"
              />
            </n-form-item>

            <!-- 图表预览 -->
            <n-form-item label="图表预览">
              <div class="chart-preview-container">
                <div ref="chartPreviewRef" class="chart-preview"></div>
                <n-button 
                  v-if="generatedConfig.sql" 
                  size="small" 
                  type="primary"
                  :loading="previewLoading"
                  style="position: absolute; top: 8px; right: 8px;"
                  @click="handlePreviewChart"
                >
                  <template #icon><n-icon><RefreshOutline /></n-icon></template>
                  刷新预览
                </n-button>
              </div>
            </n-form-item>
          </div>

          <n-empty v-else-if="!generating" description="请先生成图表配置" />
        </n-spin>
      </div>

      <!-- 第三步：创建图表 -->
      <div v-if="currentStep === 3" class="step-content">
        <n-result
          v-if="createResult"
          :status="createResult.success ? 'success' : 'error'"
          :title="createResult.success ? '图表创建成功！' : '创建失败'"
          :description="createResult.message"
        >
          <template #footer>
            <n-space>
              <n-button v-if="createResult.success" type="primary" @click="handleViewChart">
                查看图表
              </n-button>
              <n-button @click="handleCreateAnother">
                继续创建
              </n-button>
              <n-button @click="handleClose">
                关闭
              </n-button>
            </n-space>
          </template>
        </n-result>

        <n-spin v-else :show="creating">
          <template #description>正在创建图表...</template>
          <div style="height: 200px; display: flex; align-items: center; justify-content: center;">
            <n-empty description="正在创建图表..." />
          </div>
        </n-spin>
      </div>
    </div>

    <template #footer>
      <n-space justify="space-between" style="width: 100%;">
        <div>
          <n-button v-if="currentStep > 1 && currentStep < 3" @click="currentStep--">
            上一步
          </n-button>
        </div>
        <n-space>
          <n-button @click="handleClose">取消</n-button>
          <n-button
            v-if="currentStep === 1"
            type="primary"
            :loading="generating"
            :disabled="!canGenerate"
            @click="handleGenerate"
          >
            <template #icon><n-icon><SparklesOutline /></n-icon></template>
            AI 生成配置
          </n-button>
          <n-button
            v-if="currentStep === 2"
            type="primary"
            :loading="creating"
            :disabled="!generatedConfig"
            @click="handleCreate"
          >
            <template #icon><n-icon><CheckmarkOutline /></n-icon></template>
            创建图表
          </n-button>
        </n-space>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { SparklesOutline, RefreshOutline, CheckmarkOutline, StopCircleOutline, PulseOutline } from '@vicons/ionicons5'
import { getDataSourceList } from '@/api/dataSource'
import { aiGenerateChart, aiCreateChart, testChartSql } from '@/api/chart'
import { streamChart, type SseConnection, type SseConnectionState } from '@/utils/sseClient'
import echarts from '@/utils/echarts'
import { marked } from 'marked'

const props = defineProps<{
  show: boolean
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'created', chartId: number): void
}>()

const router = useRouter()
const message = useMessage()

const showModal = computed({
  get: () => props.show,
  set: (val) => emit('update:show', val)
})

const currentStep = ref(1)
const generating = ref(false)
const creating = ref(false)
const previewLoading = ref(false)
const loadingDataSources = ref(false)
const chartPreviewRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

// 流式输出相关状态
const streamingEnabled = ref(true)
const isStreaming = ref(false)
const streamingContent = ref('')
const currentConnection = ref<SseConnection | null>(null)
const connectionState = ref<SseConnectionState>('disconnected')

const formData = reactive({
  dataSourceId: null as number | null,
  requirement: '',
  preferredChartType: null as string | null,
  colorTheme: null as string | null
})

const generatedConfig = ref<{
  chartName: string
  chartCode: string
  chartType: string
  description: string
  sql: string
  chartConfig: Record<string, any>
  dataMapping: Record<string, any>
  queryParams?: Array<{ name: string; label: string; type: string; defaultValue?: string }>
} | null>(null)

const aiResponse = ref('')
const createResult = ref<{ success: boolean; message: string; chartId?: number } | null>(null)
const dataSourceOptions = ref<{ label: string; value: number }[]>([])

const chartTypeOptions = [
  { label: '折线图', value: 'line' },
  { label: '柱状图', value: 'bar' },
  { label: '饼图', value: 'pie' },
  { label: '散点图', value: 'scatter' },
  { label: '雷达图', value: 'radar' },
  { label: '仪表盘', value: 'gauge' },
  { label: '漏斗图', value: 'funnel' },
  { label: '热力图', value: 'heatmap' }
]

const colorThemeOptions = [
  { label: '商务蓝', value: 'professional' },
  { label: '活力多彩', value: 'vibrant' },
  { label: '深色主题', value: 'dark' },
  { label: '柔和色调', value: 'pastel' },
  { label: '大地色系', value: 'earth' }
]

const quickTemplates = [
  { label: '📊 销售趋势图', value: '按月份统计销售额的趋势折线图，显示最近12个月的数据' },
  { label: '🥧 占比分析', value: '各分类数据的占比饼图，显示每个分类的百分比' },
  { label: '📈 排名TOP10', value: '数据排名TOP10的柱状图，按数值从高到低排列' },
  { label: '📉 对比分析', value: '多个维度的对比柱状图，支持多组数据对比' },
  { label: '🎯 KPI仪表盘', value: '关键指标的仪表盘图，显示完成率和目标值' }
]

const canGenerate = computed(() => {
  return formData.dataSourceId && formData.requirement.trim().length > 0
})

const loadDataSources = async () => {
  loadingDataSources.value = true
  try {
    const res = await getDataSourceList({ page: 1, pageSize: 100 })
    const data = res.data?.data || res.data
    if (Array.isArray(data)) {
      dataSourceOptions.value = data.map((ds: any) => ({
        label: `${ds.name} (${ds.dbType})`,
        value: ds.id
      }))
    } else if (data?.list) {
      dataSourceOptions.value = data.list.map((ds: any) => ({
        label: `${ds.name} (${ds.dbType})`,
        value: ds.id
      }))
    }
  } catch (error) {
    console.error('加载数据源失败:', error)
  } finally {
    loadingDataSources.value = false
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
  if (streamingContent.value) {
    message.info('已停止生成，保留已接收内容')
    aiResponse.value = streamingContent.value
  }
}

// 解析流式内容中的图表配置
const parseChartConfigFromContent = (content: string) => {
  try {
    const jsonMatch = content.match(/```json\s*([\s\S]*?)```/i)
    if (jsonMatch) {
      const cfg = JSON.parse(jsonMatch[1])
      return {
        chartName: cfg.chartName || '新图表',
        chartCode: cfg.chartCode || `ai_chart_${Date.now()}`,
        chartType: cfg.chartType || 'bar',
        description: cfg.description || '',
        sql: cfg.sql || '',
        chartConfig: cfg.chartConfig || {},
        dataMapping: cfg.dataMapping || {},
        queryParams: cfg.queryParams || []
      }
    }
  } catch (e) {
    console.error('解析图表配置失败:', e)
  }
  return null
}

// 流式生成图表
const handleGenerateStreaming = () => {
  if (!canGenerate.value) return
  
  generating.value = true
  isStreaming.value = true
  streamingContent.value = ''
  currentStep.value = 2
  
  currentConnection.value = streamChart(
    {
      requirement: formData.requirement,
      dataSourceId: formData.dataSourceId!
    },
    {
      onStart: () => {
        connectionState.value = 'connected'
      },
      onToken: (token) => {
        streamingContent.value += token
      },
      onComplete: async (fullContent) => {
        isStreaming.value = false
        generating.value = false
        currentConnection.value = null
        connectionState.value = 'disconnected'
        
        aiResponse.value = fullContent
        const config = parseChartConfigFromContent(fullContent)
        if (config) {
          generatedConfig.value = config
          await nextTick()
          handlePreviewChart()
        } else {
          message.warning('AI生成的配置无法解析，请查看AI回复并手动调整')
        }
        
        streamingContent.value = ''
      },
      onError: (error) => {
        isStreaming.value = false
        generating.value = false
        currentConnection.value = null
        connectionState.value = 'error'
        
        if (streamingContent.value) {
          aiResponse.value = streamingContent.value
          const config = parseChartConfigFromContent(streamingContent.value)
          if (config) {
            generatedConfig.value = config
          }
        } else {
          message.error(error.message || '生成失败')
          currentStep.value = 1
        }
        
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
  if (!canGenerate.value) return

  // 流式模式
  if (streamingEnabled.value) {
    handleGenerateStreaming()
    return
  }

  // 非流式模式（原有逻辑）
  generating.value = true
  currentStep.value = 2

  try {
    const res = await aiGenerateChart({
      requirement: formData.requirement,
      dataSourceId: formData.dataSourceId!,
      context: {
        preferredChartType: formData.preferredChartType || undefined,
        colorTheme: formData.colorTheme || undefined
      }
    })

    const data = res.data?.data || res.data
    if (data?.success) {
      aiResponse.value = data.content || ''
      if (data.chartConfig) {
        const cfg = data.chartConfig as any
        generatedConfig.value = {
          chartName: cfg.chartName || '新图表',
          chartCode: cfg.chartCode || `ai_chart_${Date.now()}`,
          chartType: cfg.chartType || 'bar',
          description: cfg.description || '',
          sql: cfg.sql || '',
          chartConfig: cfg.chartConfig || {},
          dataMapping: cfg.dataMapping || {},
          queryParams: cfg.queryParams || []
        }
        await nextTick()
        handlePreviewChart()
      } else {
        message.warning('AI生成的配置无法解析，请查看AI回复并手动调整')
      }
    } else {
      message.error(data?.error || '生成失败')
      currentStep.value = 1
    }
  } catch (error: any) {
    console.error('AI生成图表失败:', error)
    message.error(error.message || 'AI生成图表失败')
    currentStep.value = 1
  } finally {
    generating.value = false
  }
}

const handlePreviewChart = async () => {
  if (!generatedConfig.value?.sql || !formData.dataSourceId) return

  previewLoading.value = true
  try {
    // 执行SQL获取数据
    const res = await testChartSql({
      dataSourceId: formData.dataSourceId,
      sqlContent: generatedConfig.value.sql,
      limit: 100
    })

    const data = res.data?.data || res.data
    if (Array.isArray(data) && data.length > 0) {
      // 渲染图表
      await nextTick()
      renderChart(data)
    } else {
      message.warning('SQL查询无数据，请检查SQL语句')
    }
  } catch (error: any) {
    console.error('预览失败:', error)
    message.error('预览失败: ' + (error.message || '请检查SQL语句'))
  } finally {
    previewLoading.value = false
  }
}

const renderChart = (data: any[]) => {
  if (!chartPreviewRef.value || !generatedConfig.value || data.length === 0) return

  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }

  chartInstance = echarts.init(chartPreviewRef.value)

  const config = generatedConfig.value
  const mapping = config.dataMapping || {}
  const chartType = config.chartType || 'bar'
  
  // 获取实际数据字段
  const keys = Object.keys(data[0])
  
  // 智能匹配字段
  let xField = mapping.xField || mapping.nameField
  let yField = mapping.yField || mapping.valueField
  
  if (!xField || !keys.includes(xField)) xField = keys[0]
  if (!yField || !keys.includes(yField)) yField = keys.length > 1 ? keys[1] : keys[0]

  // 构建ECharts配置
  let option: echarts.EChartsOption = config.chartConfig ? JSON.parse(JSON.stringify(config.chartConfig)) : {}

  // 如果配置为空，根据数据自动生成
  if (!option.series || (Array.isArray(option.series) && option.series.length === 0)) {
    if (chartType === 'pie') {
      option = {
        title: { text: config.chartName, left: 'center' },
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 10, left: 'center' },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
          label: { show: true, formatter: '{b}: {d}%' },
          data: data.map(row => ({ name: String(row[xField] ?? ''), value: Number(row[yField]) || 0 }))
        }],
        color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']
      }
    } else {
      option = {
        title: { text: config.chartName, left: 'center' },
        tooltip: { trigger: 'axis' },
        legend: { bottom: 10 },
        grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
        xAxis: { type: 'category', data: data.map(row => String(row[xField] ?? '')) },
        yAxis: { type: 'value' },
        series: [{
          name: yField,
          type: chartType as any,
          data: data.map(row => Number(row[yField]) || 0),
          smooth: chartType === 'line',
          areaStyle: chartType === 'line' ? { opacity: 0.3 } : undefined
        }],
        color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de']
      }
    }
  } else {
    // 使用AI生成的配置，填充数据
    if (chartType === 'pie') {
      if (option.series && Array.isArray(option.series) && option.series[0]) {
        (option.series[0] as any).data = data.map(row => ({
          name: String(row[xField] ?? ''),
          value: Number(row[yField]) || 0
        }))
      }
    } else {
      if (option.xAxis && !Array.isArray(option.xAxis)) {
        (option.xAxis as any).data = data.map(row => String(row[xField] ?? ''))
      }
      if (option.series && Array.isArray(option.series) && option.series[0]) {
        (option.series[0] as any).data = data.map(row => Number(row[yField]) || 0)
      }
    }
  }

  chartInstance.setOption(option)
  
  setTimeout(() => {
    chartInstance?.resize()
  }, 100)
}

const handleCreate = async () => {
  if (!generatedConfig.value || !formData.dataSourceId) return

  creating.value = true
  currentStep.value = 3

  try {
    const res = await aiCreateChart({
      chartName: generatedConfig.value.chartName,
      chartCode: generatedConfig.value.chartCode,
      chartType: generatedConfig.value.chartType,
      description: generatedConfig.value.description,
      dataSourceId: formData.dataSourceId,
      sql: generatedConfig.value.sql,
      chartConfig: JSON.stringify(generatedConfig.value.chartConfig),
      // 🆕 传递查询参数，使其与ChartDesigner兼容
      queryParams: generatedConfig.value.queryParams && generatedConfig.value.queryParams.length > 0 
        ? generatedConfig.value.queryParams 
        : undefined
    })

    const data = res.data?.data || res.data
    if (data?.success) {
      createResult.value = {
        success: true,
        message: data.message || '图表创建成功！',
        chartId: data.chartId
      }
      emit('created', data.chartId)
    } else {
      createResult.value = {
        success: false,
        message: data?.error || '创建失败'
      }
    }
  } catch (error: any) {
    console.error('创建图表失败:', error)
    createResult.value = {
      success: false,
      message: error.message || '创建图表失败'
    }
  } finally {
    creating.value = false
  }
}

const handleViewChart = () => {
  if (createResult.value?.chartId) {
    router.push(`/chart-center/${createResult.value.chartId}`)
    handleClose()
  }
}

const handleCreateAnother = () => {
  currentStep.value = 1
  generatedConfig.value = null
  aiResponse.value = ''
  createResult.value = null
  formData.requirement = ''
}

const handleClose = () => {
  // 清理流式连接
  if (currentConnection.value) {
    currentConnection.value.close()
    currentConnection.value = null
  }
  isStreaming.value = false
  streamingContent.value = ''
  
  showModal.value = false
  // 重置状态
  setTimeout(() => {
    currentStep.value = 1
    generatedConfig.value = null
    aiResponse.value = ''
    createResult.value = null
    formData.requirement = ''
    formData.preferredChartType = null
    formData.colorTheme = null
  }, 300)
}

const formatAiResponse = (content: string) => {
  if (!content) return ''
  try {
    return marked(content)
  } catch {
    return content.replace(/\n/g, '<br>')
  }
}

watch(showModal, (val) => {
  if (val) {
    loadDataSources()
  }
})

onMounted(() => {
  if (props.show) {
    loadDataSources()
  }
})

// 组件卸载时清理连接
onUnmounted(() => {
  if (currentConnection.value) {
    currentConnection.value.close()
    currentConnection.value = null
  }
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.ai-chart-generator {
  min-height: 400px;
}

.step-content {
  padding: 16px 0;
}

.generated-config {
  padding: 8px 0;
}

.ai-response {
  max-height: 200px;
  overflow-y: auto;
  font-size: 14px;
  line-height: 1.6;
  color: #666;
}

.ai-response :deep(pre) {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
}

.ai-response :deep(code) {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: monospace;
}

.chart-preview-container {
  position: relative;
  width: 100%;
  height: 350px;
  background: #fafafa;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}

.chart-preview {
  width: 100%;
  height: 100%;
}

/* 流式输出相关样式 */
.streaming-preview {
  margin-bottom: 16px;
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
  font-family: monospace;
  background: #f1f5f9;
  padding: 8px;
  border-radius: 4px;
}
</style>

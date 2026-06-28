<template>
  <div class="ai-chart-design-page">
    <!-- 顶部工具栏 -->
    <div class="page-header">
      <div class="header-left">
        <n-button quaternary circle @click="handleBack">
          <template #icon>
            <n-icon><ArrowBackOutline /></n-icon>
          </template>
        </n-button>
        <h2 class="page-title">
          <n-icon color="#18a058" size="24"><SparklesOutline /></n-icon>
          AI 智能图表设计
        </h2>
        <n-tag type="info" size="small">AI 助手</n-tag>
      </div>
      <div class="header-right">
        <n-button 
          type="primary" 
          :loading="saving" 
          :disabled="!canSave"
          @click="handleSaveChart"
        >
          <template #icon>
            <n-icon><SaveOutline /></n-icon>
          </template>
          保存图表
        </n-button>
      </div>
    </div>

    <!-- 主体内容区 -->
    <div class="main-content">
      <!-- 左侧配置面板 -->
      <div class="left-panel">
        <AiChatPanel
          :form-data="formData"
          :data-source-options="dataSourceOptions"
          :table-options="tableOptions"
          :chart-type-options="chartTypeOptions"
          :quick-templates="quickTemplates"
          :loading-data-sources="loadingDataSources"
          :loading-tables="loadingTables"
          :generating="generating"
          :has-generated-config="!!generatedConfig"
          @generate="handleGenerate"
          @new-conversation="handleNewConversation"
          @data-source-change="handleDataSourceChange"
          @update-field="(field: string, value: unknown) => { (formData as any)[field] = value }"
        />
      </div>

      <!-- 右侧预览和编辑区 -->
      <div class="right-panel">
        <AiChartPreview
          ref="chartPreviewRef"
          :generated-config="generatedConfig"
          :chart-type-options="chartTypeOptions"
          :generating="generating"
          :generate-status="generateStatus"
          :testing-sql="testingSql"
          :chart-data="chartData"
          :ai-response="aiResponse"
          :echarts-config-str="echartsConfigStr"
          :query-params="queryParams"
          :param-values="paramValues"
          @fullscreen="handleFullscreen"
          @test-sql="handleTestSql"
          @update:echarts-config-str="echartsConfigStr = $event"
          @update-config="handleUpdateConfig"
          @update-param-value="handleUpdateParamValue"
          @apply-config="handleApplyChartConfig"
          @chart-instance-ready="handleChartInstanceReady"
        />
      </div>
    </div>

    <!-- 全屏预览模态框 -->
    <n-modal v-model:show="showFullscreen" preset="card" style="width: 90vw; height: 90vh;">
      <template #header>
        <span>{{ generatedConfig?.chartName || '图表预览' }}</span>
      </template>
      <div ref="fullscreenChartRef" style="width: 100%; height: calc(90vh - 100px);"></div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { logger } from '@/utils/logger'
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { ArrowBackOutline, SparklesOutline, SaveOutline } from '@vicons/ionicons5'
import { getDataSourceList } from '@/api/dataSource'
import { aiGenerateChart, aiCreateChart, testChartSql } from '@/api/chart'
import { getTables } from '@/api/tableData'
import echarts from '@/utils/echarts'
import AiChatPanel from './AiChatPanel.vue'
import AiChartPreview from './AiChartPreview.vue'
import type { GeneratedChartConfig } from './AiChartPreview.vue'

const router = useRouter()
const message = useMessage()

// Child component ref
const chartPreviewRef = ref<InstanceType<typeof AiChartPreview> | null>(null)

// Fullscreen refs
const fullscreenChartRef = ref<HTMLElement | null>(null)
let currentChartInstance: echarts.ECharts | null = null
let fullscreenChartInstance: echarts.ECharts | null = null

// 状态
const generating = ref(false)
const generateStatus = ref('AI 正在分析数据结构...')
const saving = ref(false)
const testingSql = ref(false)
const loadingDataSources = ref(false)
const loadingTables = ref(false)
const showFullscreen = ref(false)

// 表单数据
const formData = reactive({
  dataSourceId: null as number | null,
  selectedTables: [] as string[],
  requirement: '',
  preferredChartType: null as string | null,
  colorTheme: 'professional'
})

// 生成的配置
const generatedConfig = ref<GeneratedChartConfig | null>(null)

const aiResponse = ref('')
const echartsConfigStr = ref('')
const chartData = ref<any[]>([])

// 查询参数
const queryParams = ref<{ name: string; label: string; type: string; defaultValue: string }[]>([])
const paramValues = reactive<Record<string, any>>({})

// 对话历史
const conversationHistory = ref<Array<{
  role: 'user' | 'assistant'
  content: string
  chartConfig?: any
}>>([])

// 选项数据
const dataSourceOptions = ref<{ label: string; value: number }[]>([])
const tableOptions = ref<{ label: string; value: string }[]>([])

const chartTypeOptions = [
  { label: '📈 折线图', value: 'line' },
  { label: '📊 柱状图', value: 'bar' },
  { label: '🥧 饼图', value: 'pie' },
  { label: '⭕ 散点图', value: 'scatter' },
  { label: '🎯 雷达图', value: 'radar' },
  { label: '⏱️ 仪表盘', value: 'gauge' },
  { label: '🔻 漏斗图', value: 'funnel' },
  { label: '🔥 热力图', value: 'heatmap' }
]

const quickTemplates = [
  { type: 'trend', icon: '📈', label: '趋势分析', keyword: '趋势', template: '按时间维度展示数据变化趋势的折线图' },
  { type: 'compare', icon: '📊', label: '对比分析', keyword: '对比', template: '不同分类之间的数据对比柱状图' },
  { type: 'ratio', icon: '🥧', label: '占比分析', keyword: '占比', template: '各分类数据的占比饼图，显示百分比' },
  { type: 'ranking', icon: '🏆', label: 'TOP排名', keyword: 'TOP', template: '数据排名TOP10的柱状图，从高到低排列' },
  { type: 'kpi', icon: '🎯', label: 'KPI指标', keyword: 'KPI', template: '关键指标的仪表盘，显示完成率' }
]

// 计算属性
const canSave = computed(() => {
  return generatedConfig.value && 
         generatedConfig.value.chartName && 
         generatedConfig.value.sql &&
         formData.dataSourceId
})

// Handle chart instance from child
const handleChartInstanceReady = (instance: echarts.ECharts | null) => {
  currentChartInstance = instance
}

// 加载数据源
const loadDataSources = async () => {
  loadingDataSources.value = true
  try {
    const res: any = await getDataSourceList({ page: 1, pageSize: 100 })
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
    logger.error('加载数据源失败:', error)
    message.error('加载数据源失败')
  } finally {
    loadingDataSources.value = false
  }
}

// 数据源变化时加载表
const handleDataSourceChange = async (value: number) => {
  if (!value) {
    tableOptions.value = []
    return
  }
  
  loadingTables.value = true
  try {
    const res = await getTables(value)
    const data = res.data?.data || res.data
    if (Array.isArray(data)) {
      tableOptions.value = data.map((t: any) => ({
        label: t.tableName + (t.remarks ? ` (${t.remarks})` : ''),
        value: t.tableName
      }))
    }
  } catch (error) {
    logger.error('加载数据表失败:', error)
  } finally {
    loadingTables.value = false
  }
}

// 新建对话
const handleNewConversation = () => {
  conversationHistory.value = []
  generatedConfig.value = null
  aiResponse.value = ''
  echartsConfigStr.value = ''
  chartData.value = []
  queryParams.value = []
  Object.keys(paramValues).forEach(key => delete paramValues[key])
  
  chartPreviewRef.value?.disposeChart()
  
  formData.requirement = ''
  message.success('已开始新对话，请输入新的图表需求')
}

// AI 生成图表
const handleGenerate = async () => {
  const canGenerate = formData.dataSourceId && formData.requirement.trim().length > 5
  if (!canGenerate) {
    message.warning('请选择数据源并输入图表需求（至少6个字符）')
    return
  }

  const isModifyMode = generatedConfig.value !== null
  const previousConfig = isModifyMode ? { ...generatedConfig.value } : null

  generating.value = true
  generateStatus.value = isModifyMode ? 'AI 正在修改图表...' : 'AI 正在分析数据结构...'
  generatedConfig.value = null
  aiResponse.value = ''
  echartsConfigStr.value = ''
  chartData.value = []
  queryParams.value = []
  Object.keys(paramValues).forEach(key => delete paramValues[key])
  
  chartPreviewRef.value?.disposeChart()

  const statusTimer: ReturnType<typeof setInterval> | null = setInterval(() => {
    const statuses = isModifyMode 
      ? ['AI 正在修改图表...', 'AI 正在调整配置...', 'AI 正在更新样式...']
      : ['AI 正在分析数据结构...', 'AI 正在设计图表类型...', 'AI 正在生成SQL查询...', 'AI 正在配置图表样式...']
    const idx = statuses.indexOf(generateStatus.value)
    if (idx < statuses.length - 1) {
      generateStatus.value = statuses[idx + 1] ?? statuses[statuses.length - 1] ?? generateStatus.value
    }
  }, 5000)

  try {
    const context: Record<string, any> = {}
    if (formData.preferredChartType) context['preferredChartType'] = formData.preferredChartType
    if (formData.colorTheme) context['colorTheme'] = formData.colorTheme
    if (formData.selectedTables.length > 0) context['tables'] = formData.selectedTables
    
    if (isModifyMode && previousConfig) {
      context['isModify'] = true
      context['previousConfig'] = previousConfig
      context['conversationHistory'] = conversationHistory.value.slice(-6)
    }

    generateStatus.value = isModifyMode ? 'AI 正在修改图表配置...' : 'AI 正在生成图表配置...'
    const res: any = await aiGenerateChart({
      requirement: formData.requirement,
      dataSourceId: formData.dataSourceId!,
      context
    })
    const data = res.data?.data || res.data
    
    if (data?.success) {
      aiResponse.value = data.content || ''
      
      if (data.chartConfig) {
        generatedConfig.value = {
          chartName: data.chartConfig.chartName || '新图表',
          chartCode: data.chartConfig.chartCode || `ai_chart_${Date.now()}`,
          chartType: data.chartConfig.chartType || 'bar',
          description: data.chartConfig.description || '',
          sql: data.chartConfig.sql || '',
          chartConfig: data.chartConfig.chartConfig || {},
          dataMapping: data.chartConfig.dataMapping || {}
        }
        
        const aiQueryParams = (data.chartConfig as any).queryParams
        if (aiQueryParams && Array.isArray(aiQueryParams)) {
          queryParams.value = aiQueryParams.map((p: any) => ({
            name: p.name || '',
            label: p.label || p.name || '',
            type: p.type || 'text',
            defaultValue: p.defaultValue ?? ''
          }))
          for (const param of queryParams.value) {
            if (!(param.name in paramValues)) {
              paramValues[param.name] = param.defaultValue
            }
          }
        }
        
        echartsConfigStr.value = JSON.stringify(generatedConfig.value.chartConfig, null, 2)
        
        conversationHistory.value.push({ role: 'user', content: formData.requirement })
        conversationHistory.value.push({
          role: 'assistant',
          content: data.content || '',
          chartConfig: generatedConfig.value
        })
        if (conversationHistory.value.length > 10) {
          conversationHistory.value = conversationHistory.value.slice(-10)
        }
        
        const retryCount: number = (data as { retryCount?: number }).retryCount || 0
        if (retryCount > 0) {
          message.success(`AI 图表配置生成成功！(经过${retryCount}次重试)`)
        } else {
          message.success('AI 图表配置生成成功！')
        }
        const warningMsg: string | undefined = (data as { warning?: string }).warning
        if (warningMsg) message.warning(warningMsg)
        if (queryParams.value.length > 0) {
          message.info(`已生成 ${queryParams.value.length} 个查询参数，请在"查询条件"中设置`)
        }
        await nextTick()
        await handleTestSql()
      } else {
        message.warning('AI 返回的配置无法解析，请查看 AI 分析标签页了解详情')
      }
    } else {
      message.error(data?.error || 'AI 生成失败，请重试')
    }
  } catch (error: any) {
    logger.error('AI 生成图表失败:', error)
    const errMsg = error.response?.data?.message || error.message || 'AI 生成失败'
    if (errMsg.includes('timeout') || errMsg.includes('超时')) {
      message.error('AI 服务响应超时，请稍后重试或简化需求描述')
    } else {
      message.error(errMsg)
    }
  } finally {
    if (statusTimer) clearInterval(statusTimer)
    generating.value = false
  }
}

// 替换SQL中的参数占位符
const substituteSqlParams = (sql: string): string => {
  if (!sql) return sql
  
  let result = sql
  
  for (const param of queryParams.value) {
    const placeholder = '${' + param.name + '}'
    const rawValue = paramValues[param.name] ?? param.defaultValue
    
    const hasValue = rawValue !== undefined && rawValue !== null && String(rawValue).trim() !== ''
    
    if (!hasValue) {
      const conditionPatterns = [
        new RegExp(`\\s+AND\\s*\\(\\s*\\$\\{${param.name}\\}\\s+IS\\s+NULL\\s+OR\\s+[\\w.\`]+\\s*=\\s*\\$\\{${param.name}\\}\\s*\\)`, 'gi'),
        new RegExp(`\\s+AND\\s*\\(\\s*\\$\\{${param.name}\\}\\s+IS\\s+NULL\\s+OR\\s+[\\w.\`]+\\s*=\\s*'\\$\\{${param.name}\\}'\\s*\\)`, 'gi'),
        new RegExp(`\\s+AND\\s+[^\\s]+\\s*[=<>!]+\\s*'?\\$\\{${param.name}\\}'?`, 'gi'),
        new RegExp(`\\s+AND\\s+[^\\s]+\\s+LIKE\\s+'%?\\$\\{${param.name}\\}%?'`, 'gi'),
        new RegExp(`\\s+AND\\s+[^\\s]+\\s+IN\\s*\\([^)]*\\$\\{${param.name}\\}[^)]*\\)`, 'gi'),
        new RegExp(`\\s+OR\\s+[^\\s]+\\s*[=<>!]+\\s*'?\\$\\{${param.name}\\}'?`, 'gi'),
      ]
      
      for (const pattern of conditionPatterns) {
        result = result.replace(pattern, '')
      }
      continue
    }
    
    let formattedValue = String(rawValue)
    if (param.type === 'date' && rawValue) {
      try {
        formattedValue = new Date(rawValue).toISOString().split('T')[0] ?? ''
      } catch {
        formattedValue = String(rawValue)
      }
    }
    
    result = result.split(placeholder).join(formattedValue)
  }
  
  result = result.replace(/WHERE\s+AND\s+/gi, 'WHERE ')
  result = result.replace(/WHERE\s+OR\s+/gi, 'WHERE ')
  result = result.replace(/WHERE\s+$/gi, '')
  
  result = result.replace(/\s+AND\s*\(\s*\$\{[^}]+\}\s+IS\s+NULL\s+OR\s+[\w.`]+\s*=\s*'?\$\{[^}]+\}'?\s*\)/gi, '')
  result = result.replace(/\s+AND\s+[\w.`]+\s*[=<>!]+\s*'?\$\{[^}]+\}'?/gi, '')
  result = result.replace(/\s+OR\s+[\w.`]+\s*[=<>!]+\s*'?\$\{[^}]+\}'?/gi, '')
  
  result = result.replace(/WHERE\s+AND\s+/gi, 'WHERE ')
  result = result.replace(/WHERE\s+OR\s+/gi, 'WHERE ')
  result = result.replace(/WHERE\s+1\s*=\s*1\s*$/gi, '')
  result = result.replace(/WHERE\s+$/gi, '')
  
  return result
}

// 测试SQL并获取数据
const handleTestSql = async () => {
  if (!generatedConfig.value?.sql || !formData.dataSourceId) {
    message.warning('请先生成图表配置')
    return
  }

  testingSql.value = true

  try {
    const finalSql = substituteSqlParams(generatedConfig.value.sql)
    
    if (finalSql.includes('${')) {
      logger.warn('SQL中存在未替换的参数:', finalSql)
    }
    
    const res: any = await testChartSql({
      dataSourceId: formData.dataSourceId!,
      sqlContent: finalSql,
      limit: 200
    })

    const data = res.data?.data || res.data
    
    if (Array.isArray(data) && data.length > 0) {
      chartData.value = data
      message.success(`查询成功，获取到 ${data.length} 条数据`)
    } else {
      message.warning('SQL 查询无数据返回，请检查 SQL 语句')
      chartData.value = []
    }
  } catch (error: any) {
    logger.error('SQL 测试失败:', error)
    message.error('SQL 执行失败: ' + (error.response?.data?.message || error.message))
  } finally {
    testingSql.value = false
  }
}

// Handle config field updates from child
const handleUpdateConfig = (field: string, value: any) => {
  if (generatedConfig.value) {
    ;(generatedConfig.value as any)[field] = value
  }
}

// Handle param value updates from child
const handleUpdateParamValue = (key: string, value: any) => {
  paramValues[key] = value
}

// Apply chart config from child
const handleApplyChartConfig = (config: Record<string, any>) => {
  if (generatedConfig.value) {
    generatedConfig.value.chartConfig = config
  }
}

// 全屏预览
const handleFullscreen = async () => {
  showFullscreen.value = true
  await nextTick()
  
  if (fullscreenChartRef.value && chartData.value.length > 0) {
    if (fullscreenChartInstance) {
      fullscreenChartInstance.dispose()
    }
    fullscreenChartInstance = echarts.init(fullscreenChartRef.value)
    
    if (currentChartInstance) {
      fullscreenChartInstance.setOption(currentChartInstance.getOption())
    }
  }
}

// 保存图表
const handleSaveChart = async () => {
  if (!canSave.value || !generatedConfig.value) return

  saving.value = true
  try {
    const res: any = await aiCreateChart({
      chartName: generatedConfig.value.chartName,
      chartCode: generatedConfig.value.chartCode,
      chartType: generatedConfig.value.chartType,
      description: generatedConfig.value.description,
      dataSourceId: formData.dataSourceId!,
      sql: generatedConfig.value.sql,
      chartConfig: JSON.stringify(generatedConfig.value.chartConfig),
      queryParams: queryParams.value.length > 0 ? queryParams.value : []
    })

    const data = res.data?.data || res.data
    
    if (data?.success) {
      message.success('图表保存成功！')
      router.push('/chart-manage')
    } else {
      message.error(data?.error || '保存失败')
    }
  } catch (error: any) {
    logger.error('保存失败:', error)
    message.error(error.response?.data?.message || error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 返回
const handleBack = () => {
  router.back()
}

// 监听窗口大小变化
const handleResize = () => {
  fullscreenChartInstance?.resize()
}

onMounted(() => {
  loadDataSources()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  fullscreenChartInstance?.dispose()
})
</script>

<style scoped>
.ai-chart-design-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--dp-spacing-md, 12px) 20px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: var(--dp-shadow-sm, 0 1px 4px rgba(0, 0, 0, 0.05));
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--dp-spacing-md, 12px);
}

.page-title {
  margin: 0;
  font-size: var(--dp-font-xl, 18px);
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: var(--dp-spacing-sm, 8px);
}

.main-content {
  flex: 1;
  display: flex;
  gap: var(--dp-spacing-md, 16px);
  padding: var(--dp-spacing-md, 16px);
  overflow: hidden;
}

.left-panel {
  width: 360px;
  flex-shrink: 0;
  overflow-y: auto;
}

.right-panel {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 滚动条样式 */
.left-panel::-webkit-scrollbar {
  width: 6px;
}

.left-panel::-webkit-scrollbar-thumb {
  background: #d0d0d0;
  border-radius: 3px;
}

.left-panel::-webkit-scrollbar-thumb:hover {
  background: #b0b0b0;
}

</style>

<style>
/* AiChartDesign 深色模式（非 scoped） */
html.dark .design-container { background: #0f172a !important; }
html.dark .design-header { color: #e2e8f0 !important; }
</style>

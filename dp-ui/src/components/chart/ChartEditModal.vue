<template>
  <n-modal
    v-model:show="visible"
    preset="card"
    :title="isEdit ? '编辑图表' : '新建图表'"
    style="width: 90vw; max-width: 1400px;"
    :mask-closable="false"
    :segmented="{ content: true, footer: true }"
  >
    <div class="chart-edit-container">
      <!-- 左侧配置 -->
      <div class="edit-left">
        <n-tabs type="line" animated>
          <!-- 基本信息 -->
          <n-tab-pane name="basic" tab="基本信息">
            <n-form
              ref="formRef"
              :model="formData"
              :rules="formRules"
              label-placement="left"
              label-width="80px"
            >
              <n-form-item label="图表名称" path="chartName">
                <n-input v-model:value="formData.chartName" placeholder="请输入图表名称" />
              </n-form-item>
              <n-form-item label="图表编码" path="chartCode">
                <n-input-group>
                  <n-input v-model:value="formData.chartCode" placeholder="唯一标识" :disabled="isEdit" />
                  <n-button v-if="!isEdit" @click="generateCode">生成</n-button>
                </n-input-group>
              </n-form-item>
              <n-form-item label="图表类型" path="chartType">
                <n-select
                  v-model:value="formData.chartType"
                  :options="chartTypeOptions"
                  placeholder="选择图表类型"
                />
              </n-form-item>
              <n-form-item label="数据源" path="dataSourceId">
                <n-select
                  v-model:value="formData.dataSourceId"
                  :options="dataSourceOptions"
                  placeholder="选择数据源"
                  filterable
                  :loading="loadingDataSources"
                  @update:value="handleDataSourceChange"
                />
              </n-form-item>
              <n-form-item label="描述">
                <n-input
                  v-model:value="formData.description"
                  type="textarea"
                  placeholder="图表描述（可选）"
                  :rows="2"
                />
              </n-form-item>
            </n-form>
          </n-tab-pane>

          <!-- SQL配置 -->
          <n-tab-pane name="sql" tab="SQL配置">
            <div class="sql-section">
              <!-- 表选择 -->
              <div class="table-select">
                <n-select
                  v-model:value="selectedTable"
                  :options="tableOptions"
                  placeholder="选择数据表（可选，辅助编写SQL）"
                  filterable
                  clearable
                  :loading="loadingTables"
                  @update:value="handleTableChange"
                />
              </div>
              
              <!-- SQL编辑器 -->
              <div class="sql-editor">
                <n-input
                  v-model:value="formData.sqlContent"
                  type="textarea"
                  placeholder="输入SQL查询语句，支持 ${参数名} 占位符"
                  :rows="10"
                  font-family="monospace"
                />
              </div>
              
              <!-- 测试按钮 -->
              <div class="sql-actions">
                <n-button type="primary" :loading="testingSql" @click="handleTestSql">
                  <template #icon><n-icon><PlayOutline /></n-icon></template>
                  测试SQL
                </n-button>
                <span v-if="chartData.length > 0" class="data-info">
                  获取到 {{ chartData.length }} 条数据
                </span>
              </div>
            </div>
          </n-tab-pane>

          <!-- 查询参数 -->
          <n-tab-pane name="params" tab="查询参数">
            <div class="params-section">
              <div class="params-header">
                <span>定义查询参数，在SQL中使用 ${参数名} 引用</span>
                <n-button size="small" type="primary" @click="addQueryParam">
                  <template #icon><n-icon><AddOutline /></n-icon></template>
                  添加参数
                </n-button>
              </div>
              
              <n-empty v-if="queryParams.length === 0" description="暂无查询参数" />
              
              <div v-else class="params-list">
                <div v-for="(param, idx) in queryParams" :key="idx" class="param-item">
                  <n-input v-model:value="param.name" placeholder="参数名" style="width: 120px" />
                  <n-input v-model:value="param.label" placeholder="显示名称" style="width: 120px" />
                  <n-select
                    v-model:value="param.type"
                    :options="paramTypeOptions"
                    style="width: 100px"
                  />
                  <n-input v-model:value="param.defaultValue" placeholder="默认值" style="width: 120px" />
                  <n-button quaternary type="error" @click="removeQueryParam(idx)">
                    <template #icon><n-icon><TrashOutline /></n-icon></template>
                  </n-button>
                </div>
              </div>
              
              <!-- 参数测试值 -->
              <div v-if="queryParams.length > 0" class="params-test">
                <n-divider>测试参数值</n-divider>
                <n-space vertical>
                  <n-form-item v-for="param in queryParams" :key="param.name" :label="param.label || param.name">
                    <n-date-picker
                      v-if="param.type === 'date'"
                      v-model:formatted-value="paramValues[param.name]"
                      type="date"
                      value-format="yyyy-MM-dd"
                      clearable
                    />
                    <n-input-number
                      v-else-if="param.type === 'number'"
                      v-model:value="paramValues[param.name]"
                      clearable
                    />
                    <n-input v-else v-model:value="paramValues[param.name]" clearable />
                  </n-form-item>
                </n-space>
              </div>
            </div>
          </n-tab-pane>

          <!-- 图表样式 -->
          <n-tab-pane name="style" tab="图表样式">
            <div class="style-section">
              <n-form-item label="数据映射">
                <n-space vertical style="width: 100%">
                  <n-input-group>
                    <n-input-group-label>X轴字段</n-input-group-label>
                    <n-select
                      v-model:value="dataMapping.xField"
                      :options="fieldOptions"
                      placeholder="选择X轴字段"
                      filterable
                    />
                  </n-input-group>
                  <n-input-group>
                    <n-input-group-label>Y轴字段</n-input-group-label>
                    <n-select
                      v-model:value="dataMapping.yField"
                      :options="fieldOptions"
                      placeholder="选择Y轴字段"
                      filterable
                    />
                  </n-input-group>
                </n-space>
              </n-form-item>
              
              <n-form-item label="配色方案">
                <n-select
                  v-model:value="colorTheme"
                  :options="colorThemeOptions"
                  placeholder="选择配色方案"
                />
              </n-form-item>
              
              <n-form-item label="ECharts配置">
                <n-input
                  v-model:value="echartsConfigStr"
                  type="textarea"
                  placeholder="高级ECharts配置（JSON格式）"
                  :rows="6"
                  font-family="monospace"
                />
              </n-form-item>
            </div>
          </n-tab-pane>
        </n-tabs>
      </div>

      <!-- 右侧预览 -->
      <div class="edit-right">
        <div class="preview-header">
          <span>图表预览</span>
          <n-button size="small" :loading="testingSql" @click="handleRefreshPreview">
            <template #icon><n-icon><RefreshOutline /></n-icon></template>
            刷新
          </n-button>
        </div>
        <div class="preview-content">
          <div v-if="chartData.length === 0" class="preview-empty">
            <n-empty description="请先测试SQL获取数据" />
          </div>
          <div v-else ref="chartContainerRef" class="chart-container"></div>
        </div>
      </div>
    </div>

    <template #footer>
      <n-space justify="end">
        <n-button @click="handleCancel">取消</n-button>
        <n-button type="primary" :loading="saving" @click="handleSave">
          {{ isEdit ? '保存' : '创建' }}
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, computed, watch, nextTick, onUnmounted } from 'vue'
import { useMessage } from 'naive-ui'
import echarts from '@/utils/echarts'
import {
  PlayOutline, AddOutline, TrashOutline, RefreshOutline
} from '@vicons/ionicons5'
import {
  getChartDefinitionById,
  createChartDefinition,
  updateChartDefinition,
  testChartSql
} from '@/api/chart'
import { getDataSources, getTables, getTableStructure } from '@/api/tableData'

// Props
const props = defineProps<{
  chartId?: number | null
}>()

// Emits
const emit = defineEmits<{
  (e: 'success'): void
  (e: 'close'): void
}>()

const message = useMessage()

// 状态
const visible = ref(false)
const saving = ref(false)
const testingSql = ref(false)
const loadingDataSources = ref(false)
const loadingTables = ref(false)
const chartContainerRef = ref<HTMLElement | null>(null)
let chartInstance: echarts.ECharts | null = null

// 表单数据
const formData = reactive({
  id: null as number | null,
  chartName: '',
  chartCode: '',
  chartType: 'bar',
  description: '',
  dataSourceId: null as number | null,
  sqlContent: '',
  status: 1
})

// 数据映射
const dataMapping = reactive({
  xField: 'name',
  yField: 'value'
})

// 查询参数
const queryParams = ref<Array<{name: string, label: string, type: string, defaultValue: string}>>([])
const paramValues = reactive<Record<string, any>>({})

// 选项数据
const dataSourceOptions = ref<any[]>([])
const tableOptions = ref<any[]>([])
const fieldOptions = ref<any[]>([])
const selectedTable = ref<string | null>(null)
const chartData = ref<any[]>([])
const echartsConfigStr = ref('')
const colorTheme = ref('default')

// 计算属性
const isEdit = computed(() => !!formData.id)

// 图表类型选项
const chartTypeOptions = [
  { value: 'bar', label: '柱状图' },
  { value: 'line', label: '折线图' },
  { value: 'pie', label: '饼图' },
  { value: 'scatter', label: '散点图' },
  { value: 'gauge', label: '仪表盘' },
  { value: 'area', label: '面积图' }
]

// 参数类型选项
const paramTypeOptions = [
  { value: 'text', label: '文本' },
  { value: 'number', label: '数字' },
  { value: 'date', label: '日期' }
]

// 配色方案选项
const colorThemeOptions = [
  { value: 'default', label: '默认' },
  { value: 'blue', label: '蓝色系' },
  { value: 'warm', label: '暖色系' },
  { value: 'cool', label: '冷色系' },
  { value: 'business', label: '商务' }
]

// 表单验证规则
const formRules = {
  chartName: { required: true, message: '请输入图表名称', trigger: 'blur' },
  chartCode: { required: true, message: '请输入图表编码', trigger: 'blur' },
  chartType: { required: true, message: '请选择图表类型', trigger: 'change' },
  dataSourceId: { required: true, message: '请选择数据源', trigger: 'change' }
}

// 配色方案
const COLOR_THEMES: Record<string, string[]> = {
  default: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4'],
  blue: ['#1890ff', '#36cfc9', '#40a9ff', '#73d13d', '#9254de'],
  warm: ['#ff7a45', '#ffa940', '#ffc53d', '#ff4d4f', '#f759ab'],
  cool: ['#36cfc9', '#13c2c2', '#1890ff', '#2f54eb', '#722ed1'],
  business: ['#3366cc', '#dc3912', '#ff9900', '#109618', '#990099']
}

// ==================== 方法 ====================

// 打开弹窗
const open = async (chartId?: number | null) => {
  resetForm()
  visible.value = true
  await loadDataSources()
  
  if (chartId) {
    await loadChartData(chartId)
  }
}

// 重置表单
const resetForm = () => {
  Object.assign(formData, {
    id: null,
    chartName: '',
    chartCode: '',
    chartType: 'bar',
    description: '',
    dataSourceId: null,
    sqlContent: '',
    status: 1
  })
  Object.assign(dataMapping, { xField: 'name', yField: 'value' })
  queryParams.value = []
  Object.keys(paramValues).forEach(key => delete paramValues[key])
  chartData.value = []
  echartsConfigStr.value = ''
  selectedTable.value = null
  disposeChart()
}

// 加载图表数据（编辑模式）
const loadChartData = async (chartId: number) => {
  try {
    const res = await getChartDefinitionById(chartId)
    const chart = res.data?.data || res.data
    if (chart) {
      Object.assign(formData, {
        id: chart.id,
        chartName: chart.chartName || '',
        chartCode: chart.chartCode || '',
        chartType: chart.chartType || 'bar',
        description: chart.description || '',
        dataSourceId: chart.dataSourceId,
        sqlContent: chart.sqlContent || '',
        status: chart.status ?? 1
      })
      
      // 解析配置
      if (chart.chartConfig) {
        try {
          const config = JSON.parse(chart.chartConfig)
          if (config.dataMapping) {
            Object.assign(dataMapping, config.dataMapping)
          }
          if (config.queryParams) {
            queryParams.value = config.queryParams
            for (const p of queryParams.value) {
              paramValues[p.name] = p.defaultValue
            }
          }
          echartsConfigStr.value = JSON.stringify(config, null, 2)
        } catch (_) { /* chartConfig parse failed, skip config load */ }
      }
      
      // 加载表列表
      if (formData.dataSourceId) {
        await loadTables(formData.dataSourceId)
      }
    }
  } catch (error) {
    message.error('加载图表数据失败')
  }
}

// 加载数据源
const loadDataSources = async () => {
  loadingDataSources.value = true
  try {
    const res = await getDataSources()
    const data = res.data?.data?.list || res.data?.data || res.data?.list || []
    dataSourceOptions.value = (Array.isArray(data) ? data : []).map((ds: any) => ({
      label: `${ds.name} (${ds.dbType})`,
      value: ds.id
    }))
  } catch (error) {
    message.error('加载数据源失败')
  } finally {
    loadingDataSources.value = false
  }
}

// 加载表列表
const loadTables = async (dataSourceId: number) => {
  loadingTables.value = true
  try {
    const res = await getTables(dataSourceId)
    const data = res.data?.data || res.data || []
    tableOptions.value = (Array.isArray(data) ? data : []).map((t: any) => ({
      label: t.tableName + (t.remarks ? ` (${t.remarks})` : ''),
      value: t.tableName
    }))
  } catch (error) {
    console.error('加载表失败:', error)
  } finally {
    loadingTables.value = false
  }
}

// 数据源变化
const handleDataSourceChange = async (value: number) => {
  tableOptions.value = []
  selectedTable.value = null
  fieldOptions.value = []
  if (value) {
    await loadTables(value)
  }
}

// 表变化
const handleTableChange = async (tableName: string) => {
  if (!formData.dataSourceId || !tableName) {
    fieldOptions.value = []
    return
  }
  try {
    const res = await getTableStructure(formData.dataSourceId, tableName)
    const columns = res.data?.data || res.data || []
    fieldOptions.value = columns.map((col: any) => ({
      label: col.columnName,
      value: col.columnName
    }))
  } catch (error) {
    console.error('加载字段失败:', error)
  }
}

// 生成编码
const generateCode = () => {
  formData.chartCode = `chart_${Date.now()}`
}

// 添加查询参数
const addQueryParam = () => {
  queryParams.value.push({
    name: `param${queryParams.value.length + 1}`,
    label: '',
    type: 'text',
    defaultValue: ''
  })
}

// 移除查询参数
const removeQueryParam = (idx: number) => {
  const param = queryParams.value[idx]
  if (param) {
    delete paramValues[param.name]
  }
  queryParams.value.splice(idx, 1)
}

// 替换SQL参数
const substituteSqlParams = (sql: string): string => {
  let result = sql
  for (const param of queryParams.value) {
    const placeholder = '${' + param.name + '}'
    const value = paramValues[param.name] ?? param.defaultValue ?? ''
    let formatted = String(value)
    
    if (param.type === 'date' && value) {
      formatted = `'${value}'`
    } else if (param.type === 'text' && value) {
      formatted = `'${value}'`
    }
    
    result = result.split(placeholder).join(formatted)
  }
  return result
}

// 测试SQL
const handleTestSql = async () => {
  if (!formData.dataSourceId || !formData.sqlContent) {
    message.warning('请选择数据源并输入SQL')
    return
  }
  
  testingSql.value = true
  try {
    const finalSql = substituteSqlParams(formData.sqlContent)
    const res = await testChartSql({
      dataSourceId: formData.dataSourceId,
      sqlContent: finalSql,
      limit: 200
    })
    
    const data = res.data?.data || res.data
    if (Array.isArray(data) && data.length > 0) {
      chartData.value = data
      // 更新字段选项
      fieldOptions.value = Object.keys(data[0]).map(key => ({
        label: key,
        value: key
      }))
      // 自动推断映射
      if (!dataMapping.xField || !fieldOptions.value.find(f => f.value === dataMapping.xField)) {
        dataMapping.xField = fieldOptions.value[0]?.value || 'name'
      }
      if (!dataMapping.yField || !fieldOptions.value.find(f => f.value === dataMapping.yField)) {
        dataMapping.yField = fieldOptions.value[1]?.value || fieldOptions.value[0]?.value || 'value'
      }
      message.success(`查询成功，获取到 ${data.length} 条数据`)
      await nextTick()
      renderChart()
    } else {
      message.warning('SQL查询无数据返回')
      chartData.value = []
    }
  } catch (error: any) {
    message.error('SQL执行失败: ' + (error.response?.data?.message || error.message))
  } finally {
    testingSql.value = false
  }
}

// 刷新预览
const handleRefreshPreview = async () => {
  await handleTestSql()
}

// 渲染图表
const renderChart = () => {
  if (!chartContainerRef.value || chartData.value.length === 0) return
  
  disposeChart()
  chartInstance = echarts.init(chartContainerRef.value)
  
  const colors = COLOR_THEMES[colorTheme.value] || COLOR_THEMES.default
  const option = buildChartOption(formData.chartType, chartData.value, dataMapping, colors)
  chartInstance.setOption(option)
  
  setTimeout(() => chartInstance?.resize(), 100)
}

// 销毁图表
const disposeChart = () => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
}

// 构建图表配置
const buildChartOption = (chartType: string, data: any[], mapping: any, colors: string[]): echarts.EChartsOption => {
  const xField = mapping.xField || 'name'
  const yField = mapping.yField || 'value'
  
  const baseOption: echarts.EChartsOption = {
    color: colors,
    tooltip: { trigger: chartType === 'pie' ? 'item' : 'axis' },
    grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true }
  }
  
  if (chartType === 'pie') {
    return {
      ...baseOption,
      legend: { bottom: '5%', left: 'center' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: data.map(row => ({
          name: String(row[xField] ?? ''),
          value: Number(row[yField]) || 0
        }))
      }]
    }
  }
  
  return {
    ...baseOption,
    xAxis: { type: 'category', data: data.map(row => String(row[xField] ?? '')) },
    yAxis: { type: 'value' },
    series: [{
      type: chartType === 'area' ? 'line' : chartType as any,
      smooth: chartType === 'line' || chartType === 'area',
      areaStyle: chartType === 'area' ? {} : undefined,
      data: data.map(row => Number(row[yField]) || 0)
    }]
  }
}

// 构建完整配置
const buildChartConfig = () => {
  let config: any = {}
  
  // 尝试解析用户自定义配置
  if (echartsConfigStr.value) {
    try {
      config = JSON.parse(echartsConfigStr.value)
    } catch (_) { /* user config parse failed, use empty config */ }
  }
  
  // 合并数据映射和查询参数
  config.dataMapping = { ...dataMapping }
  if (queryParams.value.length > 0) {
    config.queryParams = queryParams.value
  }
  config.colorTheme = colorTheme.value
  
  return JSON.stringify(config)
}

// 保存
const handleSave = async () => {
  if (!formData.chartName || !formData.chartCode || !formData.dataSourceId || !formData.sqlContent) {
    message.warning('请填写必要信息')
    return
  }
  
  saving.value = true
  try {
    const chartConfig = buildChartConfig()
    const saveData = {
      ...formData,
      chartConfig
    }
    
    if (isEdit.value) {
      await updateChartDefinition(formData.id!, saveData as any)
      message.success('图表更新成功')
    } else {
      await createChartDefinition(saveData as any)
      message.success('图表创建成功')
    }
    
    emit('success')
    visible.value = false
  } catch (error: any) {
    message.error(error.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 取消
const handleCancel = () => {
  visible.value = false
  emit('close')
}

// 监听配色变化
watch(colorTheme, () => {
  if (chartData.value.length > 0) {
    renderChart()
  }
})

// 监听数据映射变化
watch([() => dataMapping.xField, () => dataMapping.yField], () => {
  if (chartData.value.length > 0) {
    renderChart()
  }
})

// 清理
onUnmounted(() => {
  disposeChart()
})

// 暴露方法
defineExpose({
  open
})
</script>

<style scoped>
.chart-edit-container {
  display: flex;
  gap: 20px;
  height: 70vh;
  min-height: 500px;
}

.edit-left {
  flex: 1;
  min-width: 400px;
  overflow-y: auto;
}

.edit-right {
  width: 500px;
  display: flex;
  flex-direction: column;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  background: #fafafa;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e8e8e8;
  background: #fff;
  border-radius: 8px 8px 0 0;
  font-weight: 500;
}

.preview-content {
  flex: 1;
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-empty {
  color: #999;
}

.chart-container {
  width: 100%;
  height: 100%;
  min-height: 300px;
}

.sql-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sql-editor :deep(textarea) {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace !important;
  font-size: 13px;
}

.sql-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.data-info {
  color: #18a058;
  font-size: 13px;
}

.params-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.params-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.params-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.param-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.params-test {
  margin-top: 16px;
}

.style-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>

/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 图表编辑器统一逻辑 - Composable
 * 
 * 供 AiChartDesign 和 ChartDesigner 共同使用
 * 统一了图表创建、编辑、SQL执行、配置验证等核心逻辑
 */

import { ref, reactive, computed } from 'vue'
import { useMessage } from 'naive-ui'
import echarts from '@/utils/echarts'
import { 
  getChartDefinitionById, 
  createChartDefinition, 
  updateChartDefinition,
  testChartSql 
} from '@/api/chart'
import { getDataSources, getTables, getTableStructure } from '@/api/tableData'

// 支持的图表类型
export const CHART_TYPES = [
  { value: 'bar', label: '柱状图', icon: 'BarChartOutline' },
  { value: 'line', label: '折线图', icon: 'TrendingUpOutline' },
  { value: 'pie', label: '饼图', icon: 'PieChartOutline' },
  { value: 'scatter', label: '散点图', icon: 'EllipseOutline' },
  { value: 'gauge', label: '仪表盘', icon: 'SpeedometerOutline' },
  { value: 'radar', label: '雷达图', icon: 'RadioOutline' },
  { value: 'area', label: '面积图', icon: 'LayersOutline' },
  { value: 'funnel', label: '漏斗图', icon: 'FunnelOutline' }
]

// 默认配色方案
export const DEFAULT_COLORS = [
  '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
  '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#67C23A'
]

// 主题配色
export const THEME_COLORS: Record<string, string[]> = {
  default: DEFAULT_COLORS,
  blue: ['#1890ff', '#36cfc9', '#40a9ff', '#73d13d', '#9254de'],
  warm: ['#ff7a45', '#ffa940', '#ffc53d', '#ff4d4f', '#f759ab'],
  cool: ['#36cfc9', '#13c2c2', '#1890ff', '#2f54eb', '#722ed1'],
  business: ['#3366cc', '#dc3912', '#ff9900', '#109618', '#990099']
}

// 图表表单数据类型
export interface ChartFormData {
  id?: number | null
  chartName: string
  chartCode: string
  chartType: string
  description: string
  dataSourceId: number | null
  sqlContent: string
  chartConfig: string
  status: number
}

// 数据映射类型
export interface DataMapping {
  xField?: string
  yField?: string
  nameField?: string
  valueField?: string
}

// 查询参数类型
export interface QueryParam {
  name: string
  label: string
  type: 'text' | 'number' | 'date'
  defaultValue: string
}

/**
 * 图表编辑器 Composable
 */
export function useChartEditor() {
  const message = useMessage()
  
  // 状态
  const loading = ref(false)
  const saving = ref(false)
  const testingSql = ref(false)
  const chartInstance = ref<echarts.ECharts | null>(null)
  
  // 数据源相关
  const dataSources = ref<any[]>([])
  const tables = ref<any[]>([])
  const columns = ref<any[]>([])
  const loadingDataSources = ref(false)
  const loadingTables = ref(false)
  const loadingColumns = ref(false)
  
  // 表单数据
  const formData = reactive<ChartFormData>({
    id: null,
    chartName: '',
    chartCode: '',
    chartType: 'bar',
    description: '',
    dataSourceId: null,
    sqlContent: '',
    chartConfig: '',
    status: 1
  })
  
  // 数据映射
  const dataMapping = reactive<DataMapping>({
    xField: 'name',
    yField: 'value'
  })
  
  // 查询参数
  const queryParams = ref<QueryParam[]>([])
  const paramValues = reactive<Record<string, any>>({})
  
  // 图表数据
  const chartData = ref<any[]>([])
  
  // 计算属性
  const isEditing = computed(() => !!formData.id)
  const canSave = computed(() => 
    formData.chartName && 
    formData.chartCode && 
    formData.dataSourceId && 
    formData.sqlContent
  )
  
  // ==================== 数据源操作 ====================
  
  /**
   * 加载数据源列表
   */
  async function loadDataSources() {
    loadingDataSources.value = true
    try {
      const res = await getDataSources()
      dataSources.value = res.data?.data?.list || res.data?.data || res.data?.list || []
    } catch (error: any) {
      console.error('加载数据源失败:', error)
      message.error('加载数据源失败')
    } finally {
      loadingDataSources.value = false
    }
  }
  
  /**
   * 加载表列表
   */
  async function loadTables(dataSourceId: number) {
    if (!dataSourceId) {
      tables.value = []
      return
    }
    loadingTables.value = true
    try {
      const res = await getTables(dataSourceId)
      tables.value = res.data?.data || res.data || []
    } catch (error: any) {
      console.error('加载表列表失败:', error)
      message.error('加载表列表失败')
    } finally {
      loadingTables.value = false
    }
  }
  
  /**
   * 加载表字段
   */
  async function loadColumns(dataSourceId: number, tableName: string) {
    if (!dataSourceId || !tableName) {
      columns.value = []
      return
    }
    loadingColumns.value = true
    try {
      const res = await getTableStructure(dataSourceId, tableName)
      columns.value = res.data?.data || res.data || []
    } catch (error: any) {
      console.error('加载字段失败:', error)
      message.error('加载字段失败')
    } finally {
      loadingColumns.value = false
    }
  }
  
  // ==================== 图表操作 ====================
  
  /**
   * 加载图表详情（编辑模式）
   */
  async function loadChart(chartId: number) {
    loading.value = true
    try {
      const res = await getChartDefinitionById(chartId)
      const chart = (res as any).data?.data || (res as any).data || res
      if (chart) {
        Object.assign(formData, {
          id: chart.id,
          chartName: chart.chartName || '',
          chartCode: chart.chartCode || '',
          chartType: chart.chartType || 'bar',
          description: chart.description || '',
          dataSourceId: chart.dataSourceId,
          sqlContent: chart.sqlContent || '',
          chartConfig: chart.chartConfig || '',
          status: chart.status ?? 1
        })
        
        // 解析配置中的数据映射
        if (chart.chartConfig) {
          try {
            const config = JSON.parse(chart.chartConfig)
            if (config.dataMapping) {
              Object.assign(dataMapping, config.dataMapping)
            }
          } catch (_) { /* chartConfig parse failed, skip dataMapping load */ }
        }
        
        // 加载相关数据
        if (formData.dataSourceId) {
          await loadTables(formData.dataSourceId)
        }
      }
    } catch (error: any) {
      console.error('加载图表失败:', error)
      message.error('加载图表失败')
    } finally {
      loading.value = false
    }
  }
  
  /**
   * 保存图表
   */
  async function saveChart(): Promise<boolean> {
    if (!canSave.value) {
      message.warning('请填写必要信息')
      return false
    }
    
    saving.value = true
    try {
      const chartData = {
        ...formData,
        chartConfig: buildChartConfig()
      }
      
      if (isEditing.value) {
        await updateChartDefinition(formData.id!, chartData as any)
        message.success('图表更新成功')
      } else {
        await createChartDefinition(chartData as any)
        message.success('图表创建成功')
      }
      return true
    } catch (error: any) {
      console.error('保存图表失败:', error)
      message.error(error.response?.data?.message || '保存失败')
      return false
    } finally {
      saving.value = false
    }
  }
  
  /**
   * 构建图表配置JSON
   */
  function buildChartConfig(): string {
    const config = getDefaultChartConfig(formData.chartType)
    config.dataMapping = { ...dataMapping }
    if (queryParams.value.length > 0) {
      config.queryParams = queryParams.value
    }
    return JSON.stringify(config)
  }
  
  // ==================== SQL操作 ====================
  
  /**
   * 替换SQL中的参数占位符
   */
  function substituteSqlParams(sql: string): string {
    let result = sql
    for (const param of queryParams.value) {
      const placeholder = '${' + param.name + '}'
      const value = paramValues[param.name] ?? param.defaultValue ?? ''
      let formattedValue = String(value)
      
      // 日期类型格式化
      if (param.type === 'date' && value) {
        try {
          formattedValue = new Date(value).toISOString().split('T')[0] ?? ''
        } catch {
          formattedValue = String(value)
        }
      }
      
      // 日期和文本参数加引号
      if ((param.type === 'date' || param.type === 'text') && formattedValue) {
        formattedValue = `'${formattedValue}'`
      }
      
      result = result.split(placeholder).join(formattedValue)
    }
    return result
  }
  
  /**
   * 测试SQL并获取数据
   */
  async function testSql(): Promise<any[]> {
    if (!formData.dataSourceId || !formData.sqlContent) {
      message.warning('请选择数据源并输入SQL')
      return []
    }
    
    testingSql.value = true
    try {
      const finalSql = substituteSqlParams(formData.sqlContent)
      const res = await testChartSql({
        dataSourceId: formData.dataSourceId,
        sqlContent: finalSql,
        limit: 200
      })
      
      const data = (res as any).data?.data || (res as any).data || res
      if (Array.isArray(data) && data.length > 0) {
        chartData.value = data
        message.success(`查询成功，获取到 ${data.length} 条数据`)
        return data
      } else {
        message.warning('SQL查询无数据返回')
        chartData.value = []
        return []
      }
    } catch (error: any) {
      console.error('SQL测试失败:', error)
      message.error('SQL执行失败: ' + (error.response?.data?.message || error.message))
      return []
    } finally {
      testingSql.value = false
    }
  }
  
  // ==================== 图表渲染 ====================
  
  /**
   * 初始化图表实例
   */
  function initChart(container: HTMLElement) {
    if (chartInstance.value) {
      chartInstance.value.dispose()
    }
    chartInstance.value = echarts.init(container)
    return chartInstance.value
  }
  
  /**
   * 销毁图表实例
   */
  function disposeChart() {
    if (chartInstance.value) {
      chartInstance.value.dispose()
      chartInstance.value = null
    }
  }
  
  /**
   * 渲染图表
   */
  function renderChart(container: HTMLElement, data: any[]) {
    if (!data || data.length === 0) {
      console.warn('无数据，无法渲染图表')
      return
    }
    
    const instance = initChart(container)
    const option = buildChartOption(formData.chartType, data, dataMapping)
    instance.setOption(option)
    
    // 延迟调整大小
    setTimeout(() => {
      instance.resize()
    }, 100)
  }
  
  /**
   * 构建ECharts配置
   */
  function buildChartOption(chartType: string, data: any[], mapping: DataMapping): echarts.EChartsCoreOption {
    const keys = data.length > 0 ? Object.keys(data[0]) : []
    const xField = mapping.xField || mapping.nameField || keys[0] || 'name'
    const yField = mapping.yField || mapping.valueField || keys[1] || keys[0] || 'value'
    
    const baseOption: echarts.EChartsCoreOption = {
      color: DEFAULT_COLORS,
      tooltip: { trigger: chartType === 'pie' ? 'item' : 'axis' },
      grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true }
    }
    
    switch (chartType) {
      case 'pie':
        return {
          ...baseOption,
          legend: { bottom: '5%', left: 'center' },
          series: [{
            type: 'pie',
            radius: ['40%', '70%'],
            avoidLabelOverlap: false,
            itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
            label: { show: true, formatter: '{b}: {d}%' },
            data: data.map(row => ({
              name: String(row[xField] ?? ''),
              value: Number(row[yField]) || 0
            }))
          }]
        }
        
      case 'gauge': {
        const gaugeValue = data[0]?.[yField] || 0
        return {
          ...baseOption,
          series: [{
            type: 'gauge',
            progress: { show: true, width: 18 },
            axisLine: { lineStyle: { width: 18 } },
            axisTick: { show: false },
            splitLine: { length: 15, lineStyle: { width: 2, color: '#999' } },
            axisLabel: { distance: 25, color: '#999', fontSize: 14 },
            detail: { valueAnimation: true, fontSize: 36, offsetCenter: [0, '70%'] },
            data: [{ value: gaugeValue }]
          }]
        }
      }
      case 'line':
      case 'area':
        return {
          ...baseOption,
          xAxis: { type: 'category', data: data.map(row => String(row[xField] ?? '')) },
          yAxis: { type: 'value' },
          series: [{
            type: 'line',
            smooth: true,
            areaStyle: chartType === 'area' ? {} : undefined,
            data: data.map(row => Number(row[yField]) || 0)
          }]
        }
        
      case 'scatter':
        return {
          ...baseOption,
          xAxis: { type: 'value' },
          yAxis: { type: 'value' },
          series: [{
            type: 'scatter',
            symbolSize: 10,
            data: data.map(row => [Number(row[xField]) || 0, Number(row[yField]) || 0])
          }]
        }
        
      case 'bar':
      default:
        return {
          ...baseOption,
          xAxis: { type: 'category', data: data.map(row => String(row[xField] ?? '')) },
          yAxis: { type: 'value' },
          series: [{
            type: 'bar',
            itemStyle: { borderRadius: [4, 4, 0, 0] },
            data: data.map(row => Number(row[yField]) || 0)
          }]
        }
    }
  }
  
  /**
   * 获取默认图表配置
   */
  function getDefaultChartConfig(chartType: string): any {
    const baseConfig = {
      title: { text: '', left: 'center' },
      tooltip: { trigger: chartType === 'pie' ? 'item' : 'axis' },
      color: DEFAULT_COLORS
    }
    
    switch (chartType) {
      case 'pie':
        return {
          ...baseConfig,
          legend: { bottom: '5%', left: 'center' },
          series: [{ type: 'pie', radius: ['40%', '70%'] }]
        }
      case 'gauge':
        return {
          ...baseConfig,
          series: [{ type: 'gauge', progress: { show: true } }]
        }
      default:
        return {
          ...baseConfig,
          grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
          xAxis: { type: 'category' },
          yAxis: { type: 'value' },
          series: [{ type: chartType }]
        }
    }
  }
  
  // ==================== 工具方法 ====================
  
  /**
   * 重置表单
   */
  function resetForm() {
    Object.assign(formData, {
      id: null,
      chartName: '',
      chartCode: '',
      chartType: 'bar',
      description: '',
      dataSourceId: null,
      sqlContent: '',
      chartConfig: '',
      status: 1
    })
    Object.assign(dataMapping, { xField: 'name', yField: 'value' })
    queryParams.value = []
    Object.keys(paramValues).forEach(key => delete paramValues[key])
    chartData.value = []
    disposeChart()
  }
  
  /**
   * 生成唯一图表编码
   */
  function generateChartCode(prefix = 'chart'): string {
    return `${prefix}_${Date.now()}`
  }
  
  /**
   * 从数据推断字段映射
   */
  function inferDataMapping(data: any[]): DataMapping {
    if (!data || data.length === 0) return { xField: 'name', yField: 'value' }
    
    const keys = Object.keys(data[0])
    const stringFields: string[] = []
    const numericFields: string[] = []
    
    for (const key of keys) {
      const value = data[0][key]
      if (typeof value === 'number') {
        numericFields.push(key)
      } else {
        stringFields.push(key)
      }
    }
    
    const result: DataMapping = {}
    const xVal = stringFields[0] || keys[0]
    const yVal = numericFields[0] || keys[1] || keys[0]
    if (xVal !== undefined) result.xField = xVal
    if (yVal !== undefined) result.yField = yVal
    return result
  }
  
  /**
   * 添加查询参数
   */
  function addQueryParam(param: QueryParam) {
    if (!queryParams.value.find(p => p.name === param.name)) {
      queryParams.value.push(param)
      paramValues[param.name] = param.defaultValue
    }
  }
  
  /**
   * 移除查询参数
   */
  function removeQueryParam(name: string) {
    const idx = queryParams.value.findIndex(p => p.name === name)
    if (idx >= 0) {
      queryParams.value.splice(idx, 1)
      delete paramValues[name]
    }
  }
  
  return {
    // 状态
    loading,
    saving,
    testingSql,
    chartInstance,
    
    // 数据源
    dataSources,
    tables,
    columns,
    loadingDataSources,
    loadingTables,
    loadingColumns,
    
    // 表单
    formData,
    dataMapping,
    queryParams,
    paramValues,
    chartData,
    
    // 计算属性
    isEditing,
    canSave,
    
    // 常量
    CHART_TYPES,
    DEFAULT_COLORS,
    THEME_COLORS,
    
    // 方法
    loadDataSources,
    loadTables,
    loadColumns,
    loadChart,
    saveChart,
    testSql,
    substituteSqlParams,
    initChart,
    disposeChart,
    renderChart,
    buildChartOption,
    resetForm,
    generateChartCode,
    inferDataMapping,
    addQueryParam,
    removeQueryParam
  }
}

export default useChartEditor

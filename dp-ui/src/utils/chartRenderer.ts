/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 图表渲染工具函数
 * 用于统一处理图表配置解析和数据填充
 */

import type { ChartDefinition } from '@/types/chart'
import { getColorScheme as _getColorScheme } from '@/utils/chartColorSchemes'
import { registerExtendedCharts } from '@/utils/echarts'

// 需要扩展图表的类型
const EXTENDED_CHART_TYPES = new Set([
  'heatmap', 'treemap', 'graph', 'radar', 'funnel', 'sankey',
  'candlestick', 'boxplot', 'map', 'custom', 'effectScatter',
  'waterfall', 'wordCloud', 'combo'
])

/**
 * 判断是否为深色背景
 */
function isDarkColor(color: string): boolean {
  if (!color || color === 'transparent') return false
  const hex = color.replace('#', '')
  if (hex.length !== 6) return false
  const r = parseInt(hex.substring(0, 2), 16)
  const g = parseInt(hex.substring(2, 4), 16)
  const b = parseInt(hex.substring(4, 6), 16)
  const brightness = (r * 299 + g * 587 + b * 114) / 1000
  return brightness < 128
}

/**
 * 从图表配置和数据生成 ECharts 配置
 */
export function buildChartOption(
  chart: ChartDefinition,
  data: any[]
): any {
  // 表格类型不需要 ECharts 配置
  if (chart.chartType === 'table' || chart.chartType === 'summaryTable' || chart.chartType === 'pivotTable') {
    return { _isTable: true, _tableType: chart.chartType, data }
  }

  // 按需注册扩展图表类型（异步但不阻塞，注册后下次渲染生效）
  if (EXTENDED_CHART_TYPES.has(chart.chartType)) {
    registerExtendedCharts()
  }

  let option: any = {}
  let metadata: any = null

  // 解析保存的配置（新格式：包含echarts和metadata）
  let styleConfig: any = null
  if (chart.chartConfig) {
    try {
      const config = JSON.parse(chart.chartConfig)
      // parsed config
      
      // 支持新格式：{ echarts: {...}, metadata: {...}, styleConfig: {...} }
      if (config.echarts && config.metadata) {
        option = config.echarts
        metadata = config.metadata
        // 优先从顶层获取styleConfig，其次从metadata中获取
        styleConfig = config.styleConfig || metadata.styleConfig
        // styleConfig from new format
      } else {
        // 兼容旧格式：直接是ECharts配置，但也尝试提取styleConfig
        option = config
        styleConfig = config.styleConfig
        // styleConfig from old format
      }
    } catch (e) {
      console.error('解析图表配置失败', e)
      option = {}
    }
  }

  // 🆕 应用styleConfig中的配色方案和背景色
  if (styleConfig) {
    if (styleConfig.colorScheme && styleConfig.colorScheme !== 'default') {
      try {
        option.color = _getColorScheme(styleConfig.colorScheme).colors
      } catch { /* ignore */ }
    }
    if (styleConfig.backgroundColor) {
      option.backgroundColor = styleConfig.backgroundColor
    }
  }

  // 🔧 修复 legend 和 title 重叠问题
  option = fixLegendTitleOverlap(option)

  // 🔧 修复标题颜色：如果标题没有设置颜色，根据背景色自动计算
  if (option.title && option.title.text) {
    if (!option.title.textStyle?.color) {
      const bgColor = option.backgroundColor || '#ffffff'
      const isDark = isDarkColor(bgColor)
      const titleColor = isDark ? '#ffffff' : '#333333'
      option.title.textStyle = {
        ...option.title.textStyle,
        color: titleColor
      }
    }
  }

  // 如果没有数据，返回空状态配置
  if (data.length === 0) {
    return {
      ...option,
      graphic: [{
        type: 'text',
        left: 'center',
        top: 'middle',
        style: {
          text: '暂无数据',
          fontSize: 16,
          fill: '#999'
        }
      }]
    }
  }

  // 获取数据字段
  const keys = Object.keys(data[0] || {})
  if (keys.length === 0) {
    return option
  }

  // 🔧 如果有metadata，使用metadata中的字段映射
  if (metadata && metadata.fieldMapping) {
    return fillDataWithMetadata(option, data, metadata, chart.chartType)
  }

  // 如果没有配置或配置不完整，使用默认配置
  if (!option.series || (Array.isArray(option.series) && option.series.length === 0)) {
    return buildDefaultOption(chart.chartType, data, keys)
  }

  // 有配置但没有metadata，使用旧的填充方式
  return fillDataToOption(option, data, keys)
}

/**
 * 🆕 修复 legend、title、toolbox 重叠问题
 */
function fixLegendTitleOverlap(option: any): any {
  // 如果有 title 和 legend，确保它们不重叠
  if (option.title && option.legend) {
    const legendTop = option.legend.top
    const titleExists = option.title.text || option.title.show !== false
    
    // 如果 legend 在顶部且有标题，调整 legend 位置
    if (titleExists && (legendTop === 'top' || legendTop === undefined || legendTop === 0 || legendTop === '0')) {
      // 将 legend 移到标题下方
      option.legend.top = 35
    }
  }
  
  // 🔧 修复工具箱和图例重叠问题
  if (option.toolbox && option.toolbox.show !== false) {
    // 将工具箱移到右上角
    option.toolbox.right = 10
    option.toolbox.top = 5
    
    // 如果图例在顶部右侧，需要给工具箱留出空间
    if (option.legend) {
      const legendLeft = option.legend.left
      const legendRight = option.legend.right
      
      // 如果图例在右侧或居中，调整图例位置避免与工具箱重叠
      if (legendRight === 'right' || legendLeft === 'right') {
        option.legend.right = 100 // 给工具箱留出空间
      } else if (legendLeft === 'center' || legendLeft === undefined) {
        // 图例居中时，限制图例宽度或调整位置
        option.legend.right = 100
      }
    }
  }
  
  // 如果只有 legend 且在顶部，确保有足够的 grid top 空间
  if (option.legend && option.grid) {
    const legendTop = option.legend.top
    if (legendTop === 'top' || legendTop === undefined || (typeof legendTop === 'number' && legendTop < 50)) {
      // 确保 grid 有足够的顶部空间
      if (typeof option.grid.top === 'number' && option.grid.top < 60) {
        option.grid.top = 60
      } else if (option.grid.top === undefined || option.grid.top === 'auto') {
        option.grid.top = 60
      }
    }
  }
  
  return option
}

/**
 * 构建默认图表配置
 */
function buildDefaultOption(
  chartType: string,
  data: any[],
  keys: string[]
): any {
  const xAxisKey = keys[0] ?? ''
  const yAxisKey = keys[1] ?? xAxisKey

  if (chartType === 'pie') {
    return {
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: '60%',
        data: data.map((item: any) => ({
          value: Number(item[yAxisKey]) || 0,
          name: String(item[xAxisKey] || '')
        }))
      }]
    }
  }

  if (chartType === 'bar' || chartType === 'line') {
    return {
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: data.map((item: any) => String(item[xAxisKey] || ''))
      },
      yAxis: { type: 'value' },
      series: [{
        type: chartType,
        data: data.map((item: any) => Number(item[yAxisKey]) || 0)
      }]
    }
  }

  if (chartType === 'scatter') {
    return {
      tooltip: { trigger: 'item' },
      xAxis: { type: 'value' },
      yAxis: { type: 'value' },
      series: [{
        type: 'scatter',
        data: data.map((item: any) => [
          Number(item[xAxisKey]) || 0,
          Number(item[yAxisKey]) || 0
        ])
      }]
    }
  }

  if (chartType === 'radar') {
    const indicators = keys.slice(1).map(key => ({
      name: key,
      max: Math.max(...data.map(item => Number(item[key]) || 0), 1)
    }))
    return {
      tooltip: { trigger: 'item' },
      radar: {
        indicator: indicators
      },
      series: [{
        type: 'radar',
        data: [{
          value: keys.slice(1).map(key =>
            data.reduce((sum, item) => sum + (Number(item[key]) || 0), 0) / data.length
          ),
          name: '平均值'
        }]
      }]
    }
  }

  return {}
}

/**
 * 🆕 使用metadata中的字段映射填充数据
 */
function fillDataWithMetadata(
  option: any,
  data: any[],
  metadata: any,
  chartType: string
): any {
  const fieldMapping = metadata.fieldMapping || {}
  let xAxisField: string | undefined = fieldMapping.xAxis
  let yAxisFields: string[] = Array.isArray(fieldMapping.yAxis) ? fieldMapping.yAxis : 
                    (fieldMapping.yAxis ? [fieldMapping.yAxis] : [])
  const yAxisLabels: Record<string, string> = metadata.yAxisLabels || {} // 🆕 获取Y轴别名配置

  // fieldMapping resolved

  // 🔧 如果字段映射不完整，从数据中自动推断
  if (data.length > 0) {
    const keys = Object.keys(data[0])
    if (!xAxisField && keys.length > 0) {
      xAxisField = keys[0] ?? undefined
    }
    if (yAxisFields.length === 0 && keys.length > 1) {
      yAxisFields = keys.slice(1)
    }
  }

  // 饼图
  if (chartType === 'pie') {
    if (xAxisField && yAxisFields.length > 0) {
      const xF = xAxisField // 捕获窄化后的字符串类型
      const yField = yAxisFields[0] ?? ''
      // 确保series存在
      if (!option.series || !Array.isArray(option.series) || option.series.length === 0) {
        option.series = [{ type: 'pie', radius: '60%' }]
      }
      option.series[0].data = data.map((item: any, idx: number) => ({
        value: Number(item[yField]) || 0,
        name: String(item[xF] || `项${idx + 1}`)
      }))
      // 确保tooltip存在
      if (!option.tooltip) {
        option.tooltip = { trigger: 'item' }
      }
    }
  }
  // 柱状图和折线图
  else if (chartType === 'bar' || chartType === 'line') {
    if (xAxisField) {
      const xF = xAxisField // 捕获窄化后的字符串类型
      // 确保tooltip存在
      if (!option.tooltip) {
        option.tooltip = { trigger: 'axis' }
      }
      // 确保xAxis存在
      if (!option.xAxis) {
        option.xAxis = { type: 'category' }
      }
      // 确保yAxis存在
      if (!option.yAxis) {
        option.yAxis = { type: 'value' }
      }
      // 填充X轴数据
      option.xAxis.data = data.map((item: any) => String(item[xF] || ''))
      
      // 填充Y轴系列数据
      if (yAxisFields.length > 0) {
        // 确保series存在
        if (!option.series || !Array.isArray(option.series)) {
          option.series = []
        }
        // 保存原有系列的样式配置作为 fallback
        const originalSeriesConfig = option.series[0] || {}
        
        // 重新创建系列，使用 spread 保留所有已有样式属性
        option.series = yAxisFields.map((yField: string, index: number) => {
          const existingSeries = option.series?.[index] || {}
          const seriesName = yAxisLabels[yField] || yField
          
          // 先用第一个系列的样式兜底，再用当前系列覆盖，最后强制设置数据字段
          return {
            ...originalSeriesConfig,
            ...existingSeries,
            type: chartType,
            name: seriesName,
            data: data.map((item: any) => Number(item[yField]) || 0),
          }
        })
      }
    }
  }
  // 散点图
  else if (chartType === 'scatter') {
    if (xAxisField && yAxisFields.length > 0) {
      const xF = xAxisField
      const yField = yAxisFields[0] ?? ''
      if (option.series && option.series[0]) {
        option.series[0].data = data.map((item: any) => [
          Number(item[xF]) || 0,
          Number(item[yField]) || 0
        ])
      }
    }
  }
  // 雷达图
  else if (chartType === 'radar') {
    if (yAxisFields.length > 0) {
      // 设置指标 - 🆕 使用别名
      if (option.radar) {
        option.radar.indicator = yAxisFields.map((field: string) => ({
          name: yAxisLabels[field] || field,
          max: Math.max(...data.map(item => Number(item[field]) || 0), 1)
        }))
      }
      
      // 填充数据（可以按xAxisField分组）
      if (option.series && option.series[0]) {
        if (xAxisField) {
          const xF = xAxisField
          // 按xAxisField分组
          const uniqueKeys = [...new Set(data.map(item => item[xF]))]
          option.series[0].data = uniqueKeys.map((key: any) => {
            const items = data.filter(item => item[xF] === key)
            return {
              value: yAxisFields.map((field: string) =>
                items.reduce((sum, item) => sum + (Number(item[field]) || 0), 0) / items.length
              ),
              name: String(key)
            }
          })
        } else {
          // 单条数据：所有记录的平均值
          option.series[0].data = [{
            value: yAxisFields.map((field: string) =>
              data.reduce((sum, item) => sum + (Number(item[field]) || 0), 0) / data.length
            ),
            name: '平均值'
          }]
        }
      }
    }
  }

  return option
}

/**
 * 将数据填充到已有配置中
 */
function fillDataToOption(
  option: any,
  data: any[],
  keys: string[]
): any {
  const key0 = keys[0] ?? ''

  // 填充 X 轴数据（修复：检查空数组）
  if (option.xAxis && option.xAxis.type === 'category' && (!option.xAxis.data || option.xAxis.data.length === 0)) {
    option.xAxis.data = data.map((item: any) => String(item[key0] || ''))
  }

  // 填充系列数据
  if (option.series && Array.isArray(option.series)) {
    option.series.forEach((series: any, index: number) => {
      if (!series.data || series.data.length === 0) {
        const dataKey = keys[index + 1] ?? keys[1] ?? key0
        
        if (series.type === 'pie') {
          series.data = data.map((item: any, idx: number) => ({
            value: Number(item[dataKey]) || 0,
            name: String(item[key0] || `项${idx + 1}`)
          }))
        } else if (series.type === 'scatter') {
          series.data = data.map((item: any) => [
            Number(item[key0]) || 0,
            Number(item[dataKey]) || 0
          ])
        } else {
          series.data = data.map((item: any) => Number(item[dataKey]) || 0)
        }
      }
    })
  }

  return option
}

/**
 * 🆕 为内联图表构建 ECharts 配置
 * 根据 chartType + fieldMapping + data[] + colorScheme 自动生成
 */
export function buildInlineChartOption(
  chartType: string,
  data: any[],
  fieldMapping?: {
    xAxis?: string
    yAxis?: string | string[]
    group?: string
    nameField?: string
    valueField?: string
  },
  colorScheme?: string,
  existingConfig?: string
): any {
  // 如果有自定义 JSON 配置且有 series，以它为主
  if (existingConfig) {
    try {
      const parsed = JSON.parse(existingConfig)
      if (parsed.series || parsed._isTable) {
        // 用 fillDataWithMetadata 填充数据
        if (fieldMapping && data.length > 0) {
          const metadata = { fieldMapping: {
            xAxis: fieldMapping.xAxis,
            yAxis: Array.isArray(fieldMapping.yAxis) ? fieldMapping.yAxis
                   : fieldMapping.yAxis ? [fieldMapping.yAxis] : []
          }}
          const filled = fillDataWithMetadata(JSON.parse(JSON.stringify(parsed)), data, metadata, chartType)
          if (colorScheme && colorScheme !== 'default') {
            filled.color = _getColorScheme(colorScheme).colors
          }
          return filled
        }
        return parsed
      }
    } catch { /* ignore */ }
  }

  const keys = data.length > 0 ? Object.keys(data[0]) : []
  if (keys.length === 0 || data.length === 0) {
    return _buildEmptyInlineOption(chartType)
  }

  // 推断字段映射
  const xField: string = fieldMapping?.xAxis ?? keys[0] ?? ''
  const rawY = fieldMapping?.yAxis
  const yFields: string[] = rawY
    ? (Array.isArray(rawY) ? rawY : [rawY])
    : keys.slice(1).filter(k => data.some(row => !isNaN(Number(row[k]))))
  const nameField: string = fieldMapping?.nameField ?? xField
  const valueField: string = fieldMapping?.valueField ?? yFields[0] ?? keys[1] ?? keys[0] ?? ''

  // 获取颜色
  let colors: string[] | undefined
  if (colorScheme && colorScheme !== 'default') {
    try {
      colors = _getColorScheme(colorScheme).colors
    } catch { /* ignore */ }
  }

  let option: any = {}

  switch (chartType) {
    case 'pie': {
      option = {
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 10, type: 'scroll' },
        series: [{
          type: 'pie',
          radius: ['35%', '65%'],
          center: ['50%', '45%'],
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { show: true, formatter: '{b}: {d}%' },
          emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.3)' } },
          data: data.map(row => ({
            value: Number(row[valueField]) || 0,
            name: String(row[nameField] || '')
          }))
        }]
      }
      break
    }
    case 'scatter': {
      const yF = yFields[0] ?? keys[1] ?? keys[0] ?? ''
      option = {
        tooltip: { trigger: 'item' },
        xAxis: { type: 'value', name: xField },
        yAxis: { type: 'value', name: yF },
        series: [{
          type: 'scatter',
          symbolSize: 8,
          data: data.map(row => [Number(row[xField]) || 0, Number(row[yF]) || 0])
        }]
      }
      break
    }
    case 'radar': {
      if (yFields.length === 0) break
      const indicators = yFields.map(f => ({
        name: f,
        max: Math.max(...data.map(r => Number(r[f]) || 0), 1)
      }))
      option = {
        tooltip: { trigger: 'item' },
        radar: { indicator: indicators },
        series: [{
          type: 'radar',
          data: xField && data.length > 0
            ? [...new Set(data.map(r => r[xField]))].slice(0, 5).map(key => ({
                value: yFields.map(f => {
                  const items = data.filter(r => r[xField] === key)
                  return items.reduce((s, r) => s + (Number(r[f]) || 0), 0) / (items.length || 1)
                }),
                name: String(key)
              }))
            : [{ value: yFields.map(f => data.reduce((s, r) => s + (Number(r[f]) || 0), 0) / data.length), name: '平均值' }]
        }]
      }
      break
    }
    case 'gauge': {
      const val = data.length > 0 ? Number(data[0][valueField]) || 0 : 0
      option = {
        tooltip: { formatter: '{a} <br/>{b}: {c}' },
        series: [{
          type: 'gauge',
          detail: { valueAnimation: true, formatter: '{value}' },
          data: [{ value: val, name: nameField }]
        }]
      }
      break
    }
    case 'funnel': {
      option = {
        tooltip: { trigger: 'item', formatter: '{b}: {c}' },
        series: [{
          type: 'funnel',
          left: '10%', width: '80%',
          label: { show: true, position: 'inside' },
          data: data.map(row => ({
            value: Number(row[valueField]) || 0,
            name: String(row[nameField] || '')
          })).sort((a, b) => b.value - a.value)
        }]
      }
      break
    }
    case 'heatmap': {
      const groupField = fieldMapping?.group ?? keys[2] ?? keys[1] ?? ''
      option = {
        tooltip: { position: 'top' },
        grid: { top: 30, bottom: 30, left: 80, right: 30 },
        xAxis: { type: 'category', data: [...new Set(data.map(r => String(r[xField] || '')))] },
        yAxis: { type: 'category', data: [...new Set(data.map(r => String(r[groupField] || '')))] },
        visualMap: { min: 0, max: Math.max(...data.map(r => Number(r[valueField]) || 0), 1), calculable: true, orient: 'horizontal', left: 'center', bottom: 0 },
        series: [{
          type: 'heatmap',
          data: data.map(r => [String(r[xField] || ''), String(r[groupField] || ''), Number(r[valueField]) || 0]),
          label: { show: true }
        }]
      }
      break
    }
    case 'kpi':
    case 'kpiCard': {
      // KPI 卡片 - 返回标记，由 KpiCard 组件处理
      const currentValue = data.length > 0 ? Number(data[0][valueField]) || 0 : 0
      const previousField = fieldMapping?.group || keys[2]
      const previousValue = data.length > 0 && previousField ? Number(data[0][previousField]) || null : null
      
      // 计算趋势
      let trend: 'up' | 'down' | 'neutral' = 'neutral'
      let trendPercent: number | null = null
      if (previousValue !== null && previousValue !== 0) {
        const change = currentValue - previousValue
        trendPercent = (change / Math.abs(previousValue)) * 100
        trend = change > 0 ? 'up' : change < 0 ? 'down' : 'neutral'
      }
      
      option = {
        _isKpi: true,
        _kpiData: {
          value: currentValue,
          title: nameField,
          previousValue,
          trend,
          trendPercent
        }
      }
      break
    }
    case 'map':
    case 'chinaMap':
    case 'worldMap': {
      // 地图图表 - 返回标记，由 MapChart 组件处理
      const mapType = chartType === 'worldMap' ? 'world' : 'china'
      const values = data.map(r => Number(r[valueField]) || 0)
      const minVal = values.length > 0 ? Math.min(...values) : 0
      const maxVal = values.length > 0 ? Math.max(...values) : 100
      option = {
        _isMap: true,
        _mapType: mapType,
        tooltip: {
          trigger: 'item',
          formatter: (params: any) => {
            if (params.data) {
              return `${params.name}<br/>数值: ${params.data.value ?? '-'}`
            }
            return `${params.name}<br/>数值: -`
          }
        },
        visualMap: {
          type: 'continuous',
          min: minVal,
          max: maxVal === minVal ? maxVal + 10 : maxVal,
          calculable: true,
          inRange: { color: ['#e0f3f8', '#abd9e9', '#74add1', '#4575b4', '#313695'] },
          text: ['高', '低'],
          orient: 'vertical',
          left: 'left',
          bottom: '15%'
        },
        series: [{
          type: 'map',
          map: mapType,
          roam: true,
          label: { show: true, fontSize: 10 },
          itemStyle: { areaColor: '#e0e0e0', borderColor: '#ffffff' },
          emphasis: {
            label: { show: true, fontWeight: 'bold' },
            itemStyle: { areaColor: '#ffd666' }
          },
          data: data.map(row => ({
            name: String(row[nameField] || ''),
            value: Number(row[valueField]) || 0
          }))
        }]
      }
      break
    }
    case 'waterfall': {
      // 瀑布图 - 返回标记，由 WaterfallChart 组件处理
      const waterfallData = data.map(row => ({
        name: String(row[nameField] || ''),
        value: Number(row[valueField]) || 0,
        isTotal: row.isTotal === true || row.isTotal === 'true'
      }))
      option = {
        _isWaterfall: true,
        _waterfallData: waterfallData
      }
      break
    }
    case 'wordCloud':
    case 'wordcloud': {
      // 词云图 - 返回标记，由 WordCloudChart 组件处理
      const wordCloudData = data.map(row => ({
        name: String(row[nameField] || ''),
        value: Number(row[valueField]) || 0
      }))
      option = {
        _isWordCloud: true,
        _wordCloudData: wordCloudData
      }
      break
    }
    case 'combo':
    case 'comboChart': {
      // 组合图（柱线混合）- 返回标记，由 ComboChart 组件处理
      // 默认第一个 Y 字段为柱状图（左轴），其余为折线图（右轴）
      const comboData = data.map(row => {
        const item: any = { category: String(row[xField] || '') }
        yFields.forEach(f => {
          item[f] = Number(row[f]) || 0
        })
        return item
      })
      
      const seriesConfig = yFields.map((f, idx) => ({
        name: f,
        type: idx === 0 ? 'bar' as const : 'line' as const,
        yAxisIndex: idx === 0 ? 0 as const : 1 as const,
        smooth: true
      }))
      
      option = {
        _isCombo: true,
        _comboData: comboData,
        _seriesConfig: seriesConfig
      }
      break
    }
    default: {
      // bar / line and others → category axis
      const type = chartType === 'line' ? 'line' : 'bar'
      const xData = data.map(row => String(row[xField] || ''))

      // 分组支持
      const groupField = fieldMapping?.group
      if (groupField && yFields.length > 0) {
        const groups = [...new Set(data.map(r => String(r[groupField] || '')))]
        option = {
          tooltip: { trigger: 'axis' },
          legend: { data: groups, top: 5, type: 'scroll' },
          grid: { top: 40, bottom: 30, left: 60, right: 20, containLabel: true },
          xAxis: { type: 'category', data: [...new Set(xData)] },
          yAxis: { type: 'value' },
          series: groups.map(g => ({
            name: g, type,
            data: [...new Set(xData)].map(x => {
              const row = data.find(r => String(r[xField]) === x && String(r[groupField]) === g)
              return row ? Number(row[yFields[0] ?? '']) || 0 : 0
            })
          }))
        }
      } else {
        option = {
          tooltip: { trigger: 'axis' },
          legend: yFields.length > 1 ? { data: yFields, top: 5, type: 'scroll' } : undefined,
          grid: { top: yFields.length > 1 ? 40 : 25, bottom: 30, left: 60, right: 20, containLabel: true },
          xAxis: { type: 'category', data: xData, axisLabel: { rotate: xData.length > 10 ? 30 : 0, fontSize: 11 } },
          yAxis: { type: 'value' },
          series: yFields.map(yF => ({
            name: yF, type,
            data: data.map(row => Number(row[yF]) || 0),
            ...(type === 'bar' ? { barMaxWidth: 40, itemStyle: { borderRadius: [3, 3, 0, 0] } } : {}),
            ...(type === 'line' ? { smooth: true, symbol: 'circle', symbolSize: 5 } : {})
          }))
        }
      }
      break
    }
  }

  if (colors) {
    option.color = colors
  }

  return option
}

/** 空内联图表占位 option */
function _buildEmptyInlineOption(chartType: string): any {
  return {
    graphic: [{
      type: 'group',
      left: 'center',
      top: 'middle',
      children: [
        { type: 'text', style: { text: `📊 ${chartType}`, fontSize: 16, fill: '#bbb', textAlign: 'center' } },
        { type: 'text', top: 24, style: { text: '配置数据后可预览', fontSize: 12, fill: '#ccc', textAlign: 'center' } }
      ]
    }]
  }
}

/**
 * 🆕 自动检测数据字段类型，推断最佳字段映射
 */
export function autoDetectFieldMapping(
  columns: Array<{ columnName: string; dataType: string; remarks?: string }>,
  chartType: string
): { xAxis?: string | undefined; yAxis?: string[]; nameField?: string | undefined; valueField?: string | undefined } {
  const stringCols: string[] = []
  const numericCols: string[] = []
  const dateCols: string[] = []
  const firstCol = columns[0]?.columnName ?? ''
  const secondCol = columns[1]?.columnName ?? firstCol

  columns.forEach(col => {
    const dt = (col.dataType || '').toUpperCase()
    if (/DATE|TIME|TIMESTAMP/.test(dt)) {
      dateCols.push(col.columnName)
    } else if (/INT|DECIMAL|NUMERIC|FLOAT|DOUBLE|BIGINT|SMALLINT|TINYINT|NUMBER|REAL/.test(dt)) {
      numericCols.push(col.columnName)
    } else {
      stringCols.push(col.columnName)
    }
  })

  // 饼图/漏斗/仪表盘：name + value
  if (['pie', 'funnel', 'gauge'].includes(chartType)) {
    return {
      nameField: stringCols[0] ?? dateCols[0] ?? firstCol,
      valueField: numericCols[0] ?? secondCol,
      xAxis: stringCols[0] ?? dateCols[0],
      yAxis: numericCols.slice(0, 1)
    }
  }

  // 散点图：两个数值轴
  if (chartType === 'scatter') {
    return {
      xAxis: numericCols[0] ?? firstCol,
      yAxis: numericCols.length > 1 ? [numericCols[1]!] : [numericCols[0] ?? secondCol]
    }
  }

  // 雷达图：多个数值指标
  if (chartType === 'radar') {
    return {
      xAxis: stringCols[0] ?? dateCols[0] ?? firstCol,
      yAxis: numericCols.length > 0 ? numericCols.slice(0, 5) : [secondCol]
    }
  }

  // 默认: 日期/字符串做X轴，数值做Y轴
  const xAxis = dateCols[0] ?? stringCols[0] ?? firstCol
  const yAxis = numericCols.length > 0 ? numericCols.slice(0, 3) : [secondCol]
  return { xAxis, yAxis: yAxis.filter(Boolean) as string[] }
}

/**
 * 🆕 从 SQL 中检测参数占位符 ${xxx}
 */
export function detectSqlParams(sql: string): string[] {
  if (!sql) return []
  const regex = /\$\{(\w+)\}/g
  const params: string[] = []
  let match
  while ((match = regex.exec(sql)) !== null) {
    const param = match[1]
    if (param && !params.includes(param)) {
      params.push(param)
    }
  }
  return params
}

/**
 * 获取图表类型图标组件
 */
export function getChartTypeIcon(chartType: string) {
  const iconMap: Record<string, any> = {
    bar: 'BarChartOutline',
    pie: 'PieChartOutline',
    line: 'TrendingUpOutline',
    scatter: 'StatsChartOutline',
    radar: 'PulseOutline'
  }
  return iconMap[chartType] || 'BarChartOutline'
}

/**
 * 获取图表类型颜色
 */
export function getChartTypeColor(chartType: string): string {
  const colorMap: Record<string, string> = {
    line: '#18a058',
    bar: '#2080f0',
    pie: '#f0a020',
    scatter: '#d03050',
    radar: '#722ed1',
    table: '#36cfc9',
    summaryTable: '#13c2c2',
    pivotTable: '#9254de',
    map: '#1890ff',
    chinaMap: '#1890ff',
    worldMap: '#52c41a',
    kpi: '#722ed1',
    kpiCard: '#722ed1',
    waterfall: '#13c2c2',
    wordCloud: '#eb2f96',
    wordcloud: '#eb2f96',
    combo: '#fa8c16',
    comboChart: '#fa8c16'
  }
  return colorMap[chartType] || '#666'
}


/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 图表模板库
 * 提供预设的图表配置模板，方便快速创建图表
 */

export interface ChartTemplate {
  id: string
  name: string
  description: string
  chartType: string
  category: string
  thumbnail?: string
  config: any
}

/**
 * 预设图表模板
 */
export const chartTemplates: ChartTemplate[] = [
  // 基础折线图
  {
    id: 'line-basic',
    name: '基础折线图',
    description: '简洁的单系列折线图，适合展示趋势变化',
    chartType: 'line',
    category: '折线图',
    config: {
      title: { text: '', left: 'center' },
      tooltip: { trigger: 'axis' },
      grid: { top: 60, bottom: 30, left: 60, right: 30 },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value' },
      series: [{ type: 'line', smooth: true, data: [] }]
    }
  },
  // 多系列折线图
  {
    id: 'line-multi',
    name: '多系列折线图',
    description: '多条折线对比，适合多维度趋势分析',
    chartType: 'line',
    category: '折线图',
    config: {
      title: { text: '', left: 'center' },
      tooltip: { trigger: 'axis' },
      legend: { top: 30 },
      grid: { top: 80, bottom: 30, left: 60, right: 30 },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value' },
      series: []
    }
  },
  // 面积折线图
  {
    id: 'line-area',
    name: '面积折线图',
    description: '带填充区域的折线图，强调数据量感',
    chartType: 'line',
    category: '折线图',
    config: {
      title: { text: '', left: 'center' },
      tooltip: { trigger: 'axis' },
      grid: { top: 60, bottom: 30, left: 60, right: 30 },
      xAxis: { type: 'category', boundaryGap: false, data: [] },
      yAxis: { type: 'value' },
      series: [{ type: 'line', smooth: true, areaStyle: {}, data: [] }]
    }
  },
  // 基础柱状图
  {
    id: 'bar-basic',
    name: '基础柱状图',
    description: '简洁的单系列柱状图，适合分类数据对比',
    chartType: 'bar',
    category: '柱状图',
    config: {
      title: { text: '', left: 'center' },
      tooltip: { trigger: 'axis' },
      grid: { top: 60, bottom: 30, left: 60, right: 30 },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: [] }]
    }
  },
  // 堆叠柱状图
  {
    id: 'bar-stack',
    name: '堆叠柱状图',
    description: '多系列堆叠展示，适合部分与整体关系',
    chartType: 'bar',
    category: '柱状图',
    config: {
      title: { text: '', left: 'center' },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { top: 30 },
      grid: { top: 80, bottom: 30, left: 60, right: 30 },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value' },
      series: []
    }
  },
  // 横向柱状图
  {
    id: 'bar-horizontal',
    name: '横向柱状图',
    description: '水平方向的柱状图，适合长标签展示',
    chartType: 'bar',
    category: '柱状图',
    config: {
      title: { text: '', left: 'center' },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { top: 60, bottom: 30, left: 100, right: 30 },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: [] },
      series: [{ type: 'bar', data: [] }]
    }
  },
  // 基础饼图
  {
    id: 'pie-basic',
    name: '基础饼图',
    description: '简洁的饼图，适合占比分析',
    chartType: 'pie',
    category: '饼图',
    config: {
      title: { text: '', left: 'center' },
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', left: 'left', top: 'middle' },
      series: [{
        type: 'pie',
        radius: '60%',
        center: ['60%', '50%'],
        data: [],
        emphasis: {
          itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' }
        }
      }]
    }
  },
  // 环形饼图
  {
    id: 'pie-doughnut',
    name: '环形饼图',
    description: '中空的环形图，可在中心显示汇总信息',
    chartType: 'pie',
    category: '饼图',
    config: {
      title: { text: '', left: 'center' },
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', left: 'left', top: 'middle' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['60%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: false, position: 'center' },
        emphasis: {
          label: { show: true, fontSize: 20, fontWeight: 'bold' }
        },
        labelLine: { show: false },
        data: []
      }]
    }
  },
  // 玫瑰图
  {
    id: 'pie-rose',
    name: '玫瑰图',
    description: '南丁格尔玫瑰图，通过半径表示数值大小',
    chartType: 'pie',
    category: '饼图',
    config: {
      title: { text: '', left: 'center' },
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', left: 'left', top: 'middle' },
      series: [{
        type: 'pie',
        radius: ['20%', '70%'],
        center: ['60%', '50%'],
        roseType: 'radius',
        itemStyle: { borderRadius: 5 },
        data: []
      }]
    }
  },
  // 散点图
  {
    id: 'scatter-basic',
    name: '基础散点图',
    description: '展示两个变量之间的关系',
    chartType: 'scatter',
    category: '散点图',
    config: {
      title: { text: '', left: 'center' },
      tooltip: { trigger: 'item' },
      grid: { top: 60, bottom: 30, left: 60, right: 30 },
      xAxis: { type: 'value', scale: true },
      yAxis: { type: 'value', scale: true },
      series: [{ type: 'scatter', symbolSize: 10, data: [] }]
    }
  },
  // 气泡图
  {
    id: 'scatter-bubble',
    name: '气泡图',
    description: '通过气泡大小表示第三个维度',
    chartType: 'scatter',
    category: '散点图',
    config: {
      title: { text: '', left: 'center' },
      tooltip: { trigger: 'item' },
      grid: { top: 60, bottom: 30, left: 60, right: 30 },
      xAxis: { type: 'value', scale: true },
      yAxis: { type: 'value', scale: true },
      series: [{
        type: 'scatter',
        symbolSize: (data: number[]) => Math.sqrt(data[2]) * 5,
        data: []
      }]
    }
  },
  // 雷达图
  {
    id: 'radar-basic',
    name: '基础雷达图',
    description: '多维度数据对比分析',
    chartType: 'radar',
    category: '雷达图',
    config: {
      title: { text: '', left: 'center' },
      tooltip: { trigger: 'item' },
      legend: { top: 30 },
      radar: {
        indicator: [],
        center: ['50%', '55%'],
        radius: '65%'
      },
      series: [{
        type: 'radar',
        data: []
      }]
    }
  },
  // 仪表盘
  {
    id: 'gauge-basic',
    name: '基础仪表盘',
    description: '展示单一指标的完成度或状态',
    chartType: 'gauge',
    category: '仪表盘',
    config: {
      series: [{
        type: 'gauge',
        progress: { show: true, width: 18 },
        axisLine: { lineStyle: { width: 18 } },
        axisTick: { show: false },
        splitLine: { length: 15, lineStyle: { width: 2, color: '#999' } },
        axisLabel: { distance: 25, color: '#999', fontSize: 14 },
        anchor: { show: true, showAbove: true, size: 25, itemStyle: { borderWidth: 10 } },
        title: { show: false },
        detail: {
          valueAnimation: true,
          fontSize: 40,
          offsetCenter: [0, '70%']
        },
        data: [{ value: 0 }]
      }]
    }
  },
  // 漏斗图
  {
    id: 'funnel-basic',
    name: '基础漏斗图',
    description: '展示流程转化率',
    chartType: 'funnel',
    category: '漏斗图',
    config: {
      title: { text: '', left: 'center' },
      tooltip: { trigger: 'item', formatter: '{b}: {c}' },
      legend: { orient: 'vertical', left: 'left', top: 'middle' },
      series: [{
        type: 'funnel',
        left: '20%',
        width: '60%',
        sort: 'descending',
        gap: 2,
        label: { show: true, position: 'inside' },
        labelLine: { length: 10, lineStyle: { width: 1, type: 'solid' } },
        itemStyle: { borderColor: '#fff', borderWidth: 1 },
        emphasis: { label: { fontSize: 16 } },
        data: []
      }]
    }
  },
  // 热力图
  {
    id: 'heatmap-basic',
    name: '基础热力图',
    description: '通过颜色深浅展示数据分布',
    chartType: 'heatmap',
    category: '热力图',
    config: {
      title: { text: '', left: 'center' },
      tooltip: { position: 'top' },
      grid: { top: 60, bottom: 60, left: 80, right: 30 },
      xAxis: { type: 'category', data: [], splitArea: { show: true } },
      yAxis: { type: 'category', data: [], splitArea: { show: true } },
      visualMap: {
        min: 0,
        max: 100,
        calculable: true,
        orient: 'horizontal',
        left: 'center',
        bottom: 10
      },
      series: [{
        type: 'heatmap',
        data: [],
        label: { show: true },
        emphasis: {
          itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0, 0, 0, 0.5)' }
        }
      }]
    }
  }
]

/**
 * 获取指定类型的模板列表
 */
export function getTemplatesByType(chartType: string): ChartTemplate[] {
  return chartTemplates.filter(t => t.chartType === chartType)
}

/**
 * 获取指定分类的模板列表
 */
export function getTemplatesByCategory(category: string): ChartTemplate[] {
  return chartTemplates.filter(t => t.category === category)
}

/**
 * 获取所有分类
 */
export function getTemplateCategories(): string[] {
  return [...new Set(chartTemplates.map(t => t.category))]
}

/**
 * 根据ID获取模板
 */
export function getTemplateById(id: string): ChartTemplate | undefined {
  return chartTemplates.find(t => t.id === id)
}

/**
 * 应用模板配置
 */
export function applyTemplate(template: ChartTemplate, data: any[], fieldMapping: {
  xAxis?: string
  yAxis?: string[]
  nameField?: string
  valueField?: string
}): any {
  const config = JSON.parse(JSON.stringify(template.config))
  
  if (!data || data.length === 0) return config
  
  const { xAxis, yAxis, nameField, valueField } = fieldMapping
  
  switch (template.chartType) {
    case 'line':
    case 'bar':
      if (xAxis && config.xAxis) {
        config.xAxis.data = data.map(item => item[xAxis])
      }
      if (yAxis && yAxis.length > 0) {
        config.series = yAxis.map(field => ({
          ...config.series[0],
          name: field,
          data: data.map(item => Number(item[field]) || 0)
        }))
      }
      break
      
    case 'pie':
      if (nameField && valueField && config.series[0]) {
        config.series[0].data = data.map(item => ({
          name: String(item[nameField] || ''),
          value: Number(item[valueField]) || 0
        }))
      }
      break
      
    case 'scatter':
      if (xAxis && yAxis && yAxis.length > 0 && config.series[0]) {
        config.series[0].data = data.map(item => [
          Number(item[xAxis]) || 0,
          Number(item[yAxis[0]]) || 0
        ])
      }
      break
      
    case 'radar':
      if (yAxis && yAxis.length > 0 && config.radar) {
        config.radar.indicator = yAxis.map(field => ({
          name: field,
          max: Math.max(...data.map(item => Number(item[field]) || 0), 1)
        }))
        config.series[0].data = [{
          value: yAxis.map(field =>
            data.reduce((sum, item) => sum + (Number(item[field]) || 0), 0) / data.length
          ),
          name: '平均值'
        }]
      }
      break
      
    case 'funnel':
      if (nameField && valueField && config.series[0]) {
        config.series[0].data = data.map(item => ({
          name: String(item[nameField] || ''),
          value: Number(item[valueField]) || 0
        }))
      }
      break
  }
  
  return config
}

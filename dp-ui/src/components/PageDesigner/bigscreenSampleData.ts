/**
 * 大屏模板示例数据 - 为各图表类型提供预设的 ECharts option
 * 使模板应用后图表立即可见，无需配置数据源
 */

const DARK_TEXT = '#8ca8c8'
const DARK_TITLE = '#e0ecff'
const DARK_GRID = 'rgba(64,158,255,0.15)'
const COLORS = ['#00d4ff', '#00ffcc', '#ffd700', '#ff6b6b', '#36cfc9', '#5b8ff9', '#ff9a45', '#a78bfa']

export function getSampleChartConfig(chartType: string, label?: string): string {
  const opt = buildOption(chartType, label)
  return JSON.stringify(opt)
}

function buildOption(chartType: string, label?: string): any {
  switch (chartType) {
    case 'bar': return barOption(label)
    case 'line': return lineOption(label)
    case 'pie': return pieOption(label)
    case 'gauge': return gaugeOption(label)
    case 'radar': return radarOption(label)
    case 'scatter': return scatterOption()
    case 'funnel': return funnelOption()
    case 'table': return {} // 表格不用 ECharts option
    default: return barOption(label)
  }
}

function barOption(label?: string): any {
  return {
    color: COLORS,
    title: { text: label || '月度数据', textStyle: { color: DARK_TITLE, fontSize: 14 }, left: 10, top: 5 },
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      data: ['1月', '2月', '3月', '4月', '5月', '6月'],
      axisLabel: { color: DARK_TEXT },
      axisLine: { lineStyle: { color: DARK_GRID } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: DARK_TEXT },
      splitLine: { lineStyle: { color: DARK_GRID } }
    },
    series: [{
      type: 'bar',
      data: [320, 450, 380, 520, 410, 560],
      itemStyle: { borderRadius: [4, 4, 0, 0] }
    }],
    backgroundColor: 'transparent'
  }
}

function lineOption(label?: string): any {
  return {
    color: COLORS,
    title: { text: label || '趋势分析', textStyle: { color: DARK_TITLE, fontSize: 14 }, left: 10, top: 5 },
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月'],
      axisLabel: { color: DARK_TEXT },
      axisLine: { lineStyle: { color: DARK_GRID } },
      boundaryGap: false
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: DARK_TEXT },
      splitLine: { lineStyle: { color: DARK_GRID } }
    },
    series: [
      { type: 'line', name: '本年', data: [820, 932, 901, 1034, 1290, 1330, 1320, 1450], smooth: true, areaStyle: { opacity: 0.15 } },
      { type: 'line', name: '去年', data: [620, 732, 801, 934, 1090, 1130, 1220, 1250], smooth: true, areaStyle: { opacity: 0.08 }, lineStyle: { type: 'dashed' } }
    ],
    legend: { textStyle: { color: DARK_TEXT }, top: 5, right: 20 },
    backgroundColor: 'transparent'
  }
}

function pieOption(label?: string): any {
  return {
    color: COLORS,
    title: { text: label || '占比分析', textStyle: { color: DARK_TITLE, fontSize: 14 }, left: 10, top: 5 },
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      center: ['50%', '55%'],
      label: { color: DARK_TEXT, fontSize: 11 },
      data: [
        { value: 1048, name: '类别A' },
        { value: 735, name: '类别B' },
        { value: 580, name: '类别C' },
        { value: 484, name: '类别D' },
        { value: 300, name: '类别E' }
      ]
    }],
    backgroundColor: 'transparent'
  }
}

function gaugeOption(label?: string): any {
  return {
    series: [{
      type: 'gauge',
      center: ['50%', '55%'],
      radius: '75%',
      startAngle: 210,
      endAngle: -30,
      min: 0,
      max: 100,
      progress: { show: true, width: 12, itemStyle: { color: '#00d4ff' } },
      axisLine: { lineStyle: { width: 12, color: [[1, DARK_GRID]] } },
      axisTick: { show: false },
      splitLine: { length: 8, lineStyle: { width: 2, color: DARK_TEXT } },
      axisLabel: { distance: 18, color: DARK_TEXT, fontSize: 11 },
      pointer: { itemStyle: { color: '#00d4ff' } },
      title: { show: true, offsetCenter: [0, '70%'], color: DARK_TEXT, fontSize: 13 },
      detail: { valueAnimation: true, fontSize: 24, offsetCenter: [0, '40%'], color: '#00d4ff', formatter: '{value}%' },
      data: [{ value: 78, name: label || '完成率' }]
    }],
    backgroundColor: 'transparent'
  }
}

function radarOption(label?: string): any {
  return {
    color: COLORS,
    title: { text: label || '能力分析', textStyle: { color: DARK_TITLE, fontSize: 14 }, left: 10, top: 5 },
    radar: {
      indicator: [
        { name: '销售', max: 100 }, { name: '管理', max: 100 },
        { name: '技术', max: 100 }, { name: '服务', max: 100 },
        { name: '市场', max: 100 }, { name: '研发', max: 100 }
      ],
      axisName: { color: DARK_TEXT },
      splitLine: { lineStyle: { color: DARK_GRID } },
      splitArea: { areaStyle: { color: 'transparent' } },
      axisLine: { lineStyle: { color: DARK_GRID } }
    },
    series: [{
      type: 'radar',
      data: [
        { value: [85, 72, 90, 68, 80, 92], name: '实际', areaStyle: { opacity: 0.2 } },
        { value: [70, 80, 75, 85, 70, 78], name: '目标', lineStyle: { type: 'dashed' }, areaStyle: { opacity: 0.08 } }
      ]
    }],
    legend: { textStyle: { color: DARK_TEXT }, bottom: 5 },
    backgroundColor: 'transparent'
  }
}

function scatterOption(): any {
  const data = Array.from({ length: 40 }, () => [
    Math.round(Math.random() * 100),
    Math.round(Math.random() * 100)
  ])
  return {
    color: COLORS,
    title: { text: '分布分析', textStyle: { color: DARK_TITLE, fontSize: 14 }, left: 10, top: 5 },
    tooltip: { trigger: 'item' },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'value', axisLabel: { color: DARK_TEXT }, splitLine: { lineStyle: { color: DARK_GRID } } },
    yAxis: { type: 'value', axisLabel: { color: DARK_TEXT }, splitLine: { lineStyle: { color: DARK_GRID } } },
    series: [{ type: 'scatter', symbolSize: 10, data }],
    backgroundColor: 'transparent'
  }
}

function funnelOption(): any {
  return {
    color: COLORS,
    title: { text: '转化漏斗', textStyle: { color: DARK_TITLE, fontSize: 14 }, left: 10, top: 5 },
    tooltip: { trigger: 'item' },
    series: [{
      type: 'funnel',
      left: '15%', right: '15%', top: 40, bottom: 20,
      label: { color: DARK_TEXT, fontSize: 12 },
      data: [
        { value: 100, name: '访问' },
        { value: 80, name: '咨询' },
        { value: 60, name: '意向' },
        { value: 40, name: '下单' },
        { value: 20, name: '成交' }
      ]
    }],
    backgroundColor: 'transparent'
  }
}

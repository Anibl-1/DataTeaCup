/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 高级图表配置工具
 * 提供更多图表类型和高级配置选项
 */

import type { EChartsOption } from '@/utils/echarts'

/**
 * 构建漏斗图配置
 */
export function buildFunnelOption(
  data: any[],
  nameField: string,
  valueField: string,
  options: {
    title?: string
    sort?: 'ascending' | 'descending' | 'none'
    gap?: number
    labelPosition?: 'left' | 'right' | 'inside'
  } = {}
): EChartsOption {
  const { title, sort = 'descending', gap = 2, labelPosition = 'inside' } = options

  return {
    title: title ? { text: title, left: 'center' } : undefined,
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle'
    },
    series: [{
      type: 'funnel',
      left: '20%',
      width: '60%',
      sort,
      gap,
      label: {
        show: true,
        position: labelPosition
      },
      labelLine: {
        length: 10,
        lineStyle: { width: 1, type: 'solid' }
      },
      itemStyle: {
        borderColor: '#fff',
        borderWidth: 1
      },
      emphasis: {
        label: { fontSize: 16 }
      },
      data: data.map(item => ({
        name: String(item[nameField] || ''),
        value: Number(item[valueField]) || 0
      }))
    }]
  }
}

/**
 * 构建仪表盘配置
 */
export function buildGaugeOption(
  value: number,
  options: {
    title?: string
    min?: number
    max?: number
    unit?: string
    splitNumber?: number
    colors?: Array<[number, string]>
  } = {}
): EChartsOption {
  const {
    title,
    min = 0,
    max = 100,
    unit = '',
    splitNumber = 10,
    colors = [[0.3, '#67e0e3'], [0.7, '#37a2da'], [1, '#fd666d']]
  } = options

  return {
    title: title ? { text: title, left: 'center' } : undefined,
    series: [{
      type: 'gauge',
      min,
      max,
      splitNumber,
      axisLine: {
        lineStyle: {
          width: 30,
          color: colors
        }
      },
      pointer: {
        itemStyle: { color: 'auto' }
      },
      axisTick: {
        distance: -30,
        length: 8,
        lineStyle: { color: '#fff', width: 2 }
      },
      splitLine: {
        distance: -30,
        length: 30,
        lineStyle: { color: '#fff', width: 4 }
      },
      axisLabel: {
        color: 'inherit',
        distance: 40,
        fontSize: 12
      },
      detail: {
        valueAnimation: true,
        formatter: `{value}${unit}`,
        color: 'inherit',
        fontSize: 24
      },
      data: [{ value }]
    }]
  }
}

/**
 * 构建热力图配置
 */
export function buildHeatmapOption(
  data: any[],
  xField: string,
  yField: string,
  valueField: string,
  options: {
    title?: string
    visualMapMin?: number
    visualMapMax?: number
    colors?: string[]
  } = {}
): EChartsOption {
  const {
    title,
    visualMapMin = 0,
    visualMapMax = 100,
    colors = ['#313695', '#4575b4', '#74add1', '#abd9e9', '#e0f3f8', '#ffffbf', '#fee090', '#fdae61', '#f46d43', '#d73027', '#a50026']
  } = options

  // 获取唯一的X和Y值
  const xValues = [...new Set(data.map(item => String(item[xField] || '')))]
  const yValues = [...new Set(data.map(item => String(item[yField] || '')))]

  // 构建热力图数据
  const heatmapData = data.map(item => [
    xValues.indexOf(String(item[xField] || '')),
    yValues.indexOf(String(item[yField] || '')),
    Number(item[valueField]) || 0
  ])

  return {
    title: title ? { text: title, left: 'center' } : undefined,
    tooltip: {
      position: 'top',
      formatter: (params: any) => {
        return `${xValues[params.data[0]]} - ${yValues[params.data[1]]}: ${params.data[2]}`
      }
    },
    grid: {
      top: title ? 60 : 30,
      bottom: 60,
      left: 80,
      right: 30
    },
    xAxis: {
      type: 'category',
      data: xValues,
      splitArea: { show: true }
    },
    yAxis: {
      type: 'category',
      data: yValues,
      splitArea: { show: true }
    },
    visualMap: {
      min: visualMapMin,
      max: visualMapMax,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: 10,
      inRange: { color: colors }
    },
    series: [{
      type: 'heatmap',
      data: heatmapData,
      label: { show: true },
      emphasis: {
        itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0, 0, 0, 0.5)' }
      }
    }]
  }
}

/**
 * 构建桑基图配置
 */
export function buildSankeyOption(
  nodes: Array<{ name: string }>,
  links: Array<{ source: string; target: string; value: number }>,
  options: {
    title?: string
    orient?: 'horizontal' | 'vertical'
  } = {}
): EChartsOption {
  const { title, orient = 'horizontal' } = options

  return {
    title: title ? { text: title, left: 'center' } : undefined,
    tooltip: {
      trigger: 'item',
      triggerOn: 'mousemove'
    },
    series: [{
      type: 'sankey',
      orient,
      emphasis: { focus: 'adjacency' },
      nodeAlign: 'left',
      data: nodes,
      links,
      lineStyle: {
        color: 'gradient',
        curveness: 0.5
      }
    }]
  }
}

/**
 * 构建树图配置
 */
export function buildTreeOption(
  data: any,
  options: {
    title?: string
    orient?: 'LR' | 'RL' | 'TB' | 'BT'
    layout?: 'orthogonal' | 'radial'
  } = {}
): EChartsOption {
  const { title, orient = 'LR', layout = 'orthogonal' } = options

  return {
    title: title ? { text: title, left: 'center' } : undefined,
    tooltip: {
      trigger: 'item',
      triggerOn: 'mousemove'
    },
    series: [{
      type: 'tree',
      data: [data],
      orient,
      layout,
      symbol: 'emptyCircle',
      symbolSize: 7,
      initialTreeDepth: 3,
      label: {
        position: orient === 'LR' ? 'left' : orient === 'RL' ? 'right' : 'top',
        verticalAlign: 'middle',
        align: orient === 'LR' ? 'right' : orient === 'RL' ? 'left' : 'center'
      },
      leaves: {
        label: {
          position: orient === 'LR' ? 'right' : orient === 'RL' ? 'left' : 'bottom',
          verticalAlign: 'middle',
          align: orient === 'LR' ? 'left' : orient === 'RL' ? 'right' : 'center'
        }
      },
      emphasis: { focus: 'descendant' },
      expandAndCollapse: true,
      animationDuration: 550,
      animationDurationUpdate: 750
    }]
  }
}

/**
 * 构建旭日图配置
 */
export function buildSunburstOption(
  data: any[],
  options: {
    title?: string
    radius?: [string, string]
  } = {}
): EChartsOption {
  const { title, radius = ['15%', '80%'] } = options

  return {
    title: title ? { text: title, left: 'center' } : undefined,
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}'
    },
    series: [{
      type: 'sunburst',
      data,
      radius,
      sort: undefined,
      emphasis: { focus: 'ancestor' },
      levels: [
        {},
        {
          r0: '15%',
          r: '35%',
          itemStyle: { borderWidth: 2 },
          label: { rotate: 'tangential' }
        },
        {
          r0: '35%',
          r: '70%',
          label: { align: 'right' }
        },
        {
          r0: '70%',
          r: '72%',
          label: { position: 'outside', padding: 3, silent: false },
          itemStyle: { borderWidth: 3 }
        }
      ]
    }]
  }
}

/**
 * 构建K线图配置
 */
export function buildCandlestickOption(
  data: any[],
  dateField: string,
  openField: string,
  closeField: string,
  lowField: string,
  highField: string,
  options: {
    title?: string
    showVolume?: boolean
    volumeField?: string
  } = {}
): EChartsOption {
  const { title, showVolume = false, volumeField } = options

  const dates = data.map(item => item[dateField])
  const candlestickData = data.map(item => [
    Number(item[openField]) || 0,
    Number(item[closeField]) || 0,
    Number(item[lowField]) || 0,
    Number(item[highField]) || 0
  ])

  const series: any[] = [{
    type: 'candlestick',
    data: candlestickData,
    itemStyle: {
      color: '#ec0000',
      color0: '#00da3c',
      borderColor: '#8A0000',
      borderColor0: '#008F28'
    }
  }]

  const grid: any[] = [{ left: '10%', right: '10%', top: 60, height: showVolume ? '50%' : '70%' }]
  const xAxis: any[] = [{ type: 'category', data: dates, boundaryGap: false }]
  const yAxis: any[] = [{ scale: true, splitArea: { show: true } }]

  if (showVolume && volumeField) {
    grid.push({ left: '10%', right: '10%', top: '70%', height: '15%' })
    xAxis.push({ type: 'category', gridIndex: 1, data: dates, boundaryGap: false })
    yAxis.push({ scale: true, gridIndex: 1, splitNumber: 2, axisLabel: { show: false } })
    series.push({
      type: 'bar',
      xAxisIndex: 1,
      yAxisIndex: 1,
      data: data.map(item => Number(item[volumeField]) || 0),
      itemStyle: { color: '#7fbe9e' }
    })
  }

  return {
    title: title ? { text: title, left: 'center' } : undefined,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: { data: ['K线'] },
    grid,
    xAxis,
    yAxis,
    dataZoom: [
      { type: 'inside', xAxisIndex: showVolume ? [0, 1] : [0], start: 0, end: 100 },
      { show: true, xAxisIndex: showVolume ? [0, 1] : [0], type: 'slider', bottom: 10, start: 0, end: 100 }
    ],
    series
  }
}

/**
 * 构建组合图配置（柱状图+折线图）
 */
export function buildComboOption(
  data: any[],
  xField: string,
  barFields: string[],
  lineFields: string[],
  options: {
    title?: string
    barStack?: boolean
    yAxisNames?: [string, string]
  } = {}
): EChartsOption {
  const { title, barStack = false, yAxisNames = ['', ''] } = options

  const xData = data.map(item => String(item[xField] || ''))

  const series: any[] = [
    ...barFields.map((field) => ({
      name: field,
      type: 'bar',
      stack: barStack ? 'total' : undefined,
      data: data.map(item => Number(item[field]) || 0)
    })),
    ...lineFields.map((field) => ({
      name: field,
      type: 'line',
      yAxisIndex: 1,
      smooth: true,
      data: data.map(item => Number(item[field]) || 0)
    }))
  ]

  return {
    title: title ? { text: title, left: 'center' } : undefined,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: [...barFields, ...lineFields],
      top: title ? 30 : 10
    },
    grid: {
      top: title ? 80 : 60,
      bottom: 30,
      left: 60,
      right: 60
    },
    xAxis: {
      type: 'category',
      data: xData
    },
    yAxis: [
      { type: 'value', name: yAxisNames[0], position: 'left' },
      { type: 'value', name: yAxisNames[1], position: 'right' }
    ],
    series
  }
}

/**
 * 构建面积图配置
 */
export function buildAreaOption(
  data: any[],
  xField: string,
  yFields: string[],
  options: {
    title?: string
    stack?: boolean
    smooth?: boolean
  } = {}
): EChartsOption {
  const { title, stack = false, smooth = true } = options

  const xData = data.map(item => String(item[xField] || ''))

  return {
    title: title ? { text: title, left: 'center' } : undefined,
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: yFields,
      top: title ? 30 : 10
    },
    grid: {
      top: title ? 80 : 60,
      bottom: 30,
      left: 60,
      right: 30
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xData
    },
    yAxis: { type: 'value' },
    series: yFields.map((field) => ({
      name: field,
      type: 'line',
      stack: stack ? 'total' : undefined,
      smooth,
      areaStyle: {},
      emphasis: { focus: 'series' },
      data: data.map(item => Number(item[field]) || 0)
    }))
  }
}

/**
 * 构建玫瑰图配置
 */
export function buildRoseOption(
  data: any[],
  nameField: string,
  valueField: string,
  options: {
    title?: string
    roseType?: 'radius' | 'area'
  } = {}
): EChartsOption {
  const { title, roseType = 'radius' } = options

  return {
    title: title ? { text: title, left: 'center' } : undefined,
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      top: 'middle'
    },
    series: [{
      type: 'pie',
      radius: ['20%', '70%'],
      center: ['60%', '50%'],
      roseType,
      itemStyle: {
        borderRadius: 5
      },
      label: {
        show: true
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 16,
          fontWeight: 'bold'
        }
      },
      data: data.map(item => ({
        name: String(item[nameField] || ''),
        value: Number(item[valueField]) || 0
      }))
    }]
  }
}

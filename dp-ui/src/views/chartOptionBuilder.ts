/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * Utility for building default ECharts options for AI chart design.
 */
import type { EChartsOption } from '@/utils/echarts'

const DEFAULT_COLORS = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']

export function buildDefaultChartOption(
  chartType: string,
  data: any[],
  xField: string,
  yField: string,
  title: string
): EChartsOption {
  if (chartType === 'pie') {
    return {
      title: { text: title, left: 'center' },
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 10, left: 'center' },
      color: DEFAULT_COLORS,
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}: {d}%' },
        data: data.map(row => ({ name: row[xField], value: row[yField] }))
      }]
    }
  }

  if (chartType === 'gauge') {
    const value = data[0]?.[yField] || 0
    return {
      title: { text: title, left: 'center' },
      tooltip: { formatter: '{b}: {c}' },
      series: [{
        type: 'gauge',
        progress: { show: true, width: 18 },
        axisLine: { lineStyle: { width: 18 } },
        axisTick: { show: false },
        splitLine: { length: 15, lineStyle: { width: 2, color: '#999' } },
        axisLabel: { distance: 25, color: '#999', fontSize: 14 },
        anchor: { show: true, showAbove: true, size: 25, itemStyle: { borderWidth: 10 } },
        detail: { valueAnimation: true, fontSize: 36, offsetCenter: [0, '70%'] },
        data: [{ value, name: title }]
      }]
    }
  }

  // Default: line/bar chart
  return {
    title: { text: title, left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '12%', top: '15%', containLabel: true },
    xAxis: { type: 'category', data: data.map(row => row[xField]), axisLabel: { rotate: data.length > 10 ? 45 : 0 } },
    yAxis: { type: 'value' },
    color: DEFAULT_COLORS,
    series: [{
      name: yField,
      type: chartType as any,
      data: data.map(row => row[yField]),
      smooth: chartType === 'line',
      areaStyle: chartType === 'line' ? { opacity: 0.2 } : undefined,
      itemStyle: chartType === 'bar' ? { borderRadius: [4, 4, 0, 0] } : undefined
    }]
  }
}

/**
 * Resolve data field mappings from config, falling back to actual data keys.
 */
export function resolveFieldMapping(
  mapping: Record<string, any>,
  dataKeys: string[]
): { xField: string; yField: string } {
  let xField = mapping.xField || mapping.nameField
  let yField = mapping.yField || mapping.valueField

  if (!xField || !dataKeys.includes(xField)) {
    xField = dataKeys[0]
  }
  if (!yField || !dataKeys.includes(yField)) {
    yField = dataKeys.length > 1 ? dataKeys[1] : dataKeys[0]
  }

  return { xField, yField }
}

/**
 * Fill data into an existing ECharts config option.
 */
export function fillChartData(
  option: EChartsOption,
  chartType: string,
  data: any[],
  xField: string,
  yField: string
): void {
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

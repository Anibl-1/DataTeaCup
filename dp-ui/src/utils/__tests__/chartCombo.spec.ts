/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 组合图工具函数单元测试
 */

import { describe, it, expect } from 'vitest'
import {
  buildComboOption,
  buildSimpleComboOption,
  validateComboChartDualAxis,
  validateComboDataIntegrity,
  type ComboDataItem,
  type ComboSeriesConfig
} from '../chartCombo'

describe('chartCombo', () => {
  describe('buildComboOption', () => {
    it('should build valid ECharts option with dual Y-axis', () => {
      const data: ComboDataItem[] = [
        { category: 'Jan', sales: 100, growth: 10 },
        { category: 'Feb', sales: 120, growth: 20 },
        { category: 'Mar', sales: 90, growth: -25 }
      ]

      const seriesConfig: ComboSeriesConfig[] = [
        { name: 'sales', type: 'bar', yAxisIndex: 0 },
        { name: 'growth', type: 'line', yAxisIndex: 1 }
      ]

      const option = buildComboOption(data, seriesConfig, {
        title: '销售分析',
        leftYAxis: { name: '销售额' },
        rightYAxis: { name: '增长率' }
      })

      expect(option.title).toBeDefined()
      expect(option.title?.text).toBe('销售分析')
      expect(option.yAxis).toBeDefined()
      expect(Array.isArray(option.yAxis)).toBe(true)
      expect((option.yAxis as any[]).length).toBe(2)
    })

    it('should handle single Y-axis when no right axis series', () => {
      const data: ComboDataItem[] = [
        { category: 'A', value1: 100, value2: 80 }
      ]

      const seriesConfig: ComboSeriesConfig[] = [
        { name: 'value1', type: 'bar', yAxisIndex: 0 },
        { name: 'value2', type: 'bar', yAxisIndex: 0 }
      ]

      const option = buildComboOption(data, seriesConfig)

      expect((option.yAxis as any[]).length).toBe(1)
    })

    it('should handle empty data with graphic placeholder', () => {
      const option = buildComboOption([], [])

      expect(option.graphic).toBeDefined()
    })

    it('should apply custom colors', () => {
      const data: ComboDataItem[] = [
        { category: 'A', value: 100 }
      ]

      const seriesConfig: ComboSeriesConfig[] = [
        { name: 'value', type: 'bar', yAxisIndex: 0, color: '#ff0000' }
      ]

      const option = buildComboOption(data, seriesConfig)

      const series = option.series as any[]
      expect(series[0].itemStyle.color).toBe('#ff0000')
    })

    it('should configure bar series correctly', () => {
      const data: ComboDataItem[] = [
        { category: 'A', value: 100 }
      ]

      const seriesConfig: ComboSeriesConfig[] = [
        { name: 'value', type: 'bar', yAxisIndex: 0, barWidth: '50%' }
      ]

      const option = buildComboOption(data, seriesConfig)

      const series = option.series as any[]
      expect(series[0].type).toBe('bar')
      expect(series[0].barWidth).toBe('50%')
    })

    it('should configure line series correctly', () => {
      const data: ComboDataItem[] = [
        { category: 'A', value: 100 }
      ]

      const seriesConfig: ComboSeriesConfig[] = [
        { name: 'value', type: 'line', yAxisIndex: 0, smooth: true, lineWidth: 3 }
      ]

      const option = buildComboOption(data, seriesConfig)

      const series = option.series as any[]
      expect(series[0].type).toBe('line')
      expect(series[0].smooth).toBe(true)
      expect(series[0].lineStyle.width).toBe(3)
    })

    it('should include legend when showLegend is true', () => {
      const data: ComboDataItem[] = [
        { category: 'A', value1: 100, value2: 80 }
      ]

      const seriesConfig: ComboSeriesConfig[] = [
        { name: 'value1', type: 'bar', yAxisIndex: 0 },
        { name: 'value2', type: 'line', yAxisIndex: 1 }
      ]

      const option = buildComboOption(data, seriesConfig, { showLegend: true })

      expect(option.legend).toBeDefined()
      expect((option.legend as any).data).toContain('value1')
      expect((option.legend as any).data).toContain('value2')
    })

    it('should include toolbox when showToolbox is true', () => {
      const data: ComboDataItem[] = [
        { category: 'A', value: 100 }
      ]

      const seriesConfig: ComboSeriesConfig[] = [
        { name: 'value', type: 'bar', yAxisIndex: 0 }
      ]

      const option = buildComboOption(data, seriesConfig, { showToolbox: true })

      expect(option.toolbox).toBeDefined()
      expect((option.toolbox as any).show).toBe(true)
    })

    it('should include dataZoom when showDataZoom is true', () => {
      const data: ComboDataItem[] = [
        { category: 'A', value: 100 }
      ]

      const seriesConfig: ComboSeriesConfig[] = [
        { name: 'value', type: 'bar', yAxisIndex: 0 }
      ]

      const option = buildComboOption(data, seriesConfig, { showDataZoom: true })

      expect(option.dataZoom).toBeDefined()
      expect(Array.isArray(option.dataZoom)).toBe(true)
    })
  })

  describe('buildSimpleComboOption', () => {
    it('should create combo chart with bar and line series', () => {
      const data: ComboDataItem[] = [
        { category: 'Jan', sales: 100, growth: 10 },
        { category: 'Feb', sales: 120, growth: 20 }
      ]

      const option = buildSimpleComboOption(data, ['sales'], ['growth'])

      const series = option.series as any[]
      expect(series.length).toBe(2)
      expect(series[0].type).toBe('bar')
      expect(series[1].type).toBe('line')
    })

    it('should assign correct yAxisIndex', () => {
      const data: ComboDataItem[] = [
        { category: 'A', bar1: 100, line1: 10 }
      ]

      const option = buildSimpleComboOption(data, ['bar1'], ['line1'])

      const series = option.series as any[]
      expect(series[0].yAxisIndex).toBe(0) // bar on left
      expect(series[1].yAxisIndex).toBe(1) // line on right
    })
  })

  describe('validateComboChartDualAxis', () => {
    it('should return valid for correct dual axis configuration', () => {
      const data: ComboDataItem[] = [
        { category: 'A', bar: 100, line: 10 }
      ]

      const seriesConfig: ComboSeriesConfig[] = [
        { name: 'bar', type: 'bar', yAxisIndex: 0 },
        { name: 'line', type: 'line', yAxisIndex: 1 }
      ]

      const option = buildComboOption(data, seriesConfig)
      const result = validateComboChartDualAxis(option, seriesConfig)

      expect(result.valid).toBe(true)
    })

    it('should return valid for single axis configuration', () => {
      const data: ComboDataItem[] = [
        { category: 'A', value: 100 }
      ]

      const seriesConfig: ComboSeriesConfig[] = [
        { name: 'value', type: 'bar', yAxisIndex: 0 }
      ]

      const option = buildComboOption(data, seriesConfig)
      const result = validateComboChartDualAxis(option, seriesConfig)

      expect(result.valid).toBe(true)
    })

    it('should check yAxis positions', () => {
      const data: ComboDataItem[] = [
        { category: 'A', bar: 100, line: 10 }
      ]

      const seriesConfig: ComboSeriesConfig[] = [
        { name: 'bar', type: 'bar', yAxisIndex: 0 },
        { name: 'line', type: 'line', yAxisIndex: 1 }
      ]

      const option = buildComboOption(data, seriesConfig)
      const yAxis = option.yAxis as any[]

      expect(yAxis[0].position).toBe('left')
      expect(yAxis[1].position).toBe('right')
    })
  })

  describe('validateComboDataIntegrity', () => {
    it('should return true when all data is present', () => {
      const data: ComboDataItem[] = [
        { category: 'A', value1: 100, value2: 80 },
        { category: 'B', value1: 120, value2: 90 }
      ]

      const seriesConfig: ComboSeriesConfig[] = [
        { name: 'value1', type: 'bar', yAxisIndex: 0 },
        { name: 'value2', type: 'line', yAxisIndex: 1 }
      ]

      const option = buildComboOption(data, seriesConfig)

      expect(validateComboDataIntegrity(data, seriesConfig, option)).toBe(true)
    })

    it('should return true for empty data', () => {
      const option = buildComboOption([], [])

      expect(validateComboDataIntegrity([], [], option)).toBe(true)
    })

    it('should verify category data', () => {
      const data: ComboDataItem[] = [
        { category: 'Jan', value: 100 },
        { category: 'Feb', value: 120 }
      ]

      const seriesConfig: ComboSeriesConfig[] = [
        { name: 'value', type: 'bar', yAxisIndex: 0 }
      ]

      const option = buildComboOption(data, seriesConfig)
      const xAxis = option.xAxis as any

      expect(xAxis.data).toEqual(['Jan', 'Feb'])
    })

    it('should verify series data values', () => {
      const data: ComboDataItem[] = [
        { category: 'A', value: 100 },
        { category: 'B', value: 200 }
      ]

      const seriesConfig: ComboSeriesConfig[] = [
        { name: 'value', type: 'bar', yAxisIndex: 0 }
      ]

      const option = buildComboOption(data, seriesConfig)
      const series = option.series as any[]

      expect(series[0].data).toEqual([100, 200])
    })
  })
})

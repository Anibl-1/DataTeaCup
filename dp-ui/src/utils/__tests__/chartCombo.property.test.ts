/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 组合图属性测试
 * Feature: platform-optimization-plan
 * Property 12: 组合图双 Y 轴配置
 * 
 * Validates: Requirements 4.6
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import {
  buildComboOption,
  validateComboChartDualAxis,
  validateComboDataIntegrity,
  type ComboDataItem,
  type ComboSeriesConfig
} from '../chartCombo'

describe('chartCombo Property Tests', () => {
  /**
   * Property 12: 组合图双 Y 轴配置
   * 
   * For any 组合图配置（包含柱状系列和折线系列），生成的 ECharts option
   * SHALL 包含两个 yAxis 配置，且柱状系列绑定第一个 Y 轴，折线系列绑定第二个 Y 轴。
   * 
   * **Validates: Requirements 4.6**
   */
  describe('Property 12: Combo Chart Dual Y-Axis Configuration', () => {
    // Arbitrary for combo data
    const categoryArb = fc.string({ minLength: 1, maxLength: 10 })
    const valueArb = fc.float({ min: -10000, max: 10000, noNaN: true })


    // Generate combo data with bar and line fields
    const comboDataArb = fc.array(
      fc.record({
        category: categoryArb,
        barValue: valueArb,
        lineValue: valueArb
      }),
      { minLength: 1, maxLength: 20 }
    ).map(items => items.map(item => ({
      category: item.category,
      barValue: item.barValue,
      lineValue: item.lineValue
    } as ComboDataItem)))

    it('should create two yAxis when series use different axes', () => {
      fc.assert(
        fc.property(comboDataArb, (data) => {
          const seriesConfig: ComboSeriesConfig[] = [
            { name: 'barValue', type: 'bar', yAxisIndex: 0 },
            { name: 'lineValue', type: 'line', yAxisIndex: 1 }
          ]

          const option = buildComboOption(data, seriesConfig)
          const yAxis = option.yAxis as any[]

          // Should have exactly 2 Y axes
          expect(yAxis.length).toBe(2)
          
          // First axis should be on left
          expect(yAxis[0].position).toBe('left')
          
          // Second axis should be on right
          expect(yAxis[1].position).toBe('right')
        }),
        { numRuns: 100 }
      )
    })

    it('should bind bar series to first Y-axis and line series to second Y-axis', () => {
      fc.assert(
        fc.property(comboDataArb, (data) => {
          const seriesConfig: ComboSeriesConfig[] = [
            { name: 'barValue', type: 'bar', yAxisIndex: 0 },
            { name: 'lineValue', type: 'line', yAxisIndex: 1 }
          ]

          const option = buildComboOption(data, seriesConfig)
          const series = option.series as any[]

          // Bar series should use yAxisIndex 0
          const barSeries = series.find(s => s.type === 'bar')
          expect(barSeries.yAxisIndex).toBe(0)

          // Line series should use yAxisIndex 1
          const lineSeries = series.find(s => s.type === 'line')
          expect(lineSeries.yAxisIndex).toBe(1)
        }),
        { numRuns: 100 }
      )
    })

    it('should pass dual axis validation', () => {
      fc.assert(
        fc.property(comboDataArb, (data) => {
          const seriesConfig: ComboSeriesConfig[] = [
            { name: 'barValue', type: 'bar', yAxisIndex: 0 },
            { name: 'lineValue', type: 'line', yAxisIndex: 1 }
          ]

          const option = buildComboOption(data, seriesConfig)
          const result = validateComboChartDualAxis(option, seriesConfig)

          expect(result.valid).toBe(true)
        }),
        { numRuns: 100 }
      )
    })
  })

  describe('Property: Combo Chart Data Integrity', () => {
    const comboDataArb = fc.array(
      fc.record({
        category: fc.string({ minLength: 1, maxLength: 10 }),
        value1: fc.float({ min: 0, max: 1000, noNaN: true }),
        value2: fc.float({ min: 0, max: 100, noNaN: true })
      }),
      { minLength: 1, maxLength: 15 }
    ).map(items => items.map(item => ({
      category: item.category,
      value1: item.value1,
      value2: item.value2
    } as ComboDataItem)))

    it('should preserve all input data in output', () => {
      fc.assert(
        fc.property(comboDataArb, (data) => {
          const seriesConfig: ComboSeriesConfig[] = [
            { name: 'value1', type: 'bar', yAxisIndex: 0 },
            { name: 'value2', type: 'line', yAxisIndex: 1 }
          ]

          const option = buildComboOption(data, seriesConfig)

          expect(validateComboDataIntegrity(data, seriesConfig, option)).toBe(true)
        }),
        { numRuns: 100 }
      )
    })

    it('should include all categories in xAxis', () => {
      fc.assert(
        fc.property(comboDataArb, (data) => {
          const seriesConfig: ComboSeriesConfig[] = [
            { name: 'value1', type: 'bar', yAxisIndex: 0 }
          ]

          const option = buildComboOption(data, seriesConfig)
          const xAxis = option.xAxis as any

          const expectedCategories = data.map(d => d.category)
          expect(xAxis.data).toEqual(expectedCategories)
        }),
        { numRuns: 100 }
      )
    })
  })
})

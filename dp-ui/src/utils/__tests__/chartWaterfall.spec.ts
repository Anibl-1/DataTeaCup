/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 瀑布图工具函数单元测试
 */

import { describe, it, expect } from 'vitest'
import {
  buildWaterfallOption,
  calculateWaterfallData,
  validateWaterfallInvariant,
  validateWaterfallSteps,
  type WaterfallDataItem
} from '../chartWaterfall'

describe('chartWaterfall', () => {
  describe('calculateWaterfallData', () => {
    it('should calculate waterfall data correctly for positive values', () => {
      const data: WaterfallDataItem[] = [
        { name: '收入', value: 100 },
        { name: '成本', value: -30 },
        { name: '利润', value: 20 }
      ]

      const result = calculateWaterfallData(data, { startValue: 0, showTotal: false })

      // 验证累计值
      expect(result.runningTotals).toContain(100) // 第一项后
      expect(result.runningTotals).toContain(70)  // 第二项后 (100 - 30)
      expect(result.runningTotals).toContain(90)  // 第三项后 (70 + 20)
    })

    it('should handle start value correctly', () => {
      const data: WaterfallDataItem[] = [
        { name: '增加', value: 50 }
      ]

      const result = calculateWaterfallData(data, { startValue: 100, startLabel: '初始' })

      expect(result.categories[0]).toBe('初始')
      expect(result.runningTotals[0]).toBe(100)
      expect(result.runningTotals[1]).toBe(150)
    })

    it('should add total when showTotal is true', () => {
      const data: WaterfallDataItem[] = [
        { name: 'A', value: 100 },
        { name: 'B', value: -20 }
      ]

      const result = calculateWaterfallData(data, { showTotal: true, totalLabel: '合计' })

      expect(result.categories).toContain('合计')
      const totalIndex = result.categories.indexOf('合计')
      expect(result.totalData[totalIndex]).toBe(80)
    })

    it('should handle isTotal flag in data', () => {
      const data: WaterfallDataItem[] = [
        { name: 'A', value: 100 },
        { name: 'B', value: -20 },
        { name: '小计', value: 0, isTotal: true }
      ]

      const result = calculateWaterfallData(data, { showTotal: false })

      expect(result.categories).toContain('小计')
      const totalIndex = result.categories.indexOf('小计')
      expect(result.totalData[totalIndex]).toBe(80)
    })

    it('should handle empty data', () => {
      const result = calculateWaterfallData([], { showTotal: true })

      // Empty data with showTotal adds start (0) and total
      expect(result.categories.length).toBeGreaterThanOrEqual(1)
      expect(result.runningTotals[result.runningTotals.length - 1]).toBe(0)
    })
  })

  describe('buildWaterfallOption', () => {
    it('should build valid ECharts option', () => {
      const data: WaterfallDataItem[] = [
        { name: '收入', value: 100 },
        { name: '支出', value: -40 }
      ]

      const option = buildWaterfallOption(data, { title: '财务分析' })

      expect(option.title).toBeDefined()
      expect(option.title?.text).toBe('财务分析')
      expect(option.series).toBeDefined()
      expect(Array.isArray(option.series)).toBe(true)
      expect((option.series as any[]).length).toBe(4) // 透明、正值、负值、总计
    })

    it('should handle empty data with graphic placeholder', () => {
      const option = buildWaterfallOption([])

      expect(option.graphic).toBeDefined()
    })

    it('should apply custom colors', () => {
      const data: WaterfallDataItem[] = [
        { name: 'A', value: 100 }
      ]

      const option = buildWaterfallOption(data, {
        positiveColor: '#00ff00',
        negativeColor: '#ff0000',
        totalColor: '#0000ff'
      })

      const series = option.series as any[]
      expect(series[1].itemStyle.color).toBe('#00ff00') // 正值
      expect(series[2].itemStyle.color).toBe('#ff0000') // 负值
      expect(series[3].itemStyle.color).toBe('#0000ff') // 总计
    })

    it('should include tooltip with running total', () => {
      const data: WaterfallDataItem[] = [
        { name: 'A', value: 100 }
      ]

      const option = buildWaterfallOption(data)

      expect(option.tooltip).toBeDefined()
      expect(option.tooltip?.trigger).toBe('axis')
    })
  })

  describe('validateWaterfallInvariant', () => {
    it('should return true for valid waterfall data', () => {
      const data: WaterfallDataItem[] = [
        { name: 'A', value: 100 },
        { name: 'B', value: -30 },
        { name: 'C', value: 50 }
      ]

      expect(validateWaterfallInvariant(data, 0)).toBe(true)
    })

    it('should return true for empty data', () => {
      expect(validateWaterfallInvariant([], 0)).toBe(true)
    })

    it('should handle start value in invariant check', () => {
      const data: WaterfallDataItem[] = [
        { name: 'A', value: 50 }
      ]

      expect(validateWaterfallInvariant(data, 100)).toBe(true)
    })

    it('should ignore isTotal items in sum calculation', () => {
      const data: WaterfallDataItem[] = [
        { name: 'A', value: 100 },
        { name: 'B', value: -20 },
        { name: '小计', value: 0, isTotal: true }
      ]

      expect(validateWaterfallInvariant(data, 0)).toBe(true)
    })
  })

  describe('validateWaterfallSteps', () => {
    it('should return true when all steps are correct', () => {
      const data: WaterfallDataItem[] = [
        { name: 'A', value: 100 },
        { name: 'B', value: -30 },
        { name: 'C', value: 20 }
      ]

      expect(validateWaterfallSteps(data, 0)).toBe(true)
    })

    it('should return true for empty data', () => {
      expect(validateWaterfallSteps([], 0)).toBe(true)
    })

    it('should handle negative values correctly', () => {
      const data: WaterfallDataItem[] = [
        { name: 'A', value: -50 },
        { name: 'B', value: -30 }
      ]

      expect(validateWaterfallSteps(data, 100)).toBe(true)
    })
  })
})

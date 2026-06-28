/**
 * 瀑布图属性测试
 * Feature: platform-optimization-plan
 * Property 11: 瀑布图累计总额不变量
 * 
 * Validates: Requirements 4.7
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import {
  buildWaterfallOption,
  calculateWaterfallData,
  validateWaterfallInvariant,
  validateWaterfallSteps,
  type WaterfallDataItem
} from '../chartWaterfall'

describe('chartWaterfall Property Tests', () => {
  /**
   * Property 11: 瀑布图累计总额不变量
   * 
   * For any 瀑布图输入数据序列 [v1, v2, ..., vn]，生成的瀑布图中每一步的累计值
   * SHALL 等于该步之前所有值的总和加上初始值，且最终累计值等于所有值之和。
   * 
   * **Validates: Requirements 4.7**
   */
  describe('Property 11: Waterfall Running Total Invariant', () => {
    // Arbitrary for waterfall data items
    const waterfallDataItemArb = fc.record({
      name: fc.string({ minLength: 1, maxLength: 20 }),
      value: fc.integer({ min: -10000, max: 10000 }),
      isTotal: fc.constant(false)
    })

    const waterfallDataArb = fc.array(waterfallDataItemArb, { minLength: 1, maxLength: 20 })
    const startValueArb = fc.integer({ min: -10000, max: 10000 })


    it('should maintain running total invariant for any data sequence', () => {
      fc.assert(
        fc.property(waterfallDataArb, startValueArb, (data, startValue) => {
          // Calculate expected final total
          const expectedTotal = startValue + data.reduce((sum, item) => sum + item.value, 0)

          // Calculate waterfall data
          const calculation = calculateWaterfallData(data, { startValue, showTotal: true })

          // Get final running total
          const finalTotal = calculation.runningTotals[calculation.runningTotals.length - 1]

          // Verify invariant: final total equals start + sum of all values
          expect(Math.abs(finalTotal - expectedTotal)).toBeLessThan(0.0001)
        }),
        { numRuns: 100 }
      )
    })

    it('should have correct running total at each step', () => {
      fc.assert(
        fc.property(waterfallDataArb, startValueArb, (data, startValue) => {
          const calculation = calculateWaterfallData(data, { startValue, showTotal: false })

          let expectedRunningTotal = startValue
          let dataIndex = 0

          // Skip start item if present
          const hasStartItem = calculation.categories[0] === '起始' || startValue !== 0
          const startIndex = hasStartItem ? 1 : 0

          if (hasStartItem) {
            expect(calculation.runningTotals[0]).toBe(startValue)
          }

          for (let i = startIndex; i < calculation.runningTotals.length && dataIndex < data.length; i++) {
            expectedRunningTotal += data[dataIndex].value
            expect(Math.abs(calculation.runningTotals[i] - expectedRunningTotal)).toBeLessThan(0.0001)
            dataIndex++
          }
        }),
        { numRuns: 100 }
      )
    })

    it('should validate invariant using validateWaterfallInvariant', () => {
      fc.assert(
        fc.property(waterfallDataArb, startValueArb, (data, startValue) => {
          expect(validateWaterfallInvariant(data, startValue)).toBe(true)
        }),
        { numRuns: 100 }
      )
    })

    it('should validate steps using validateWaterfallSteps', () => {
      fc.assert(
        fc.property(waterfallDataArb, startValueArb, (data, startValue) => {
          expect(validateWaterfallSteps(data, startValue)).toBe(true)
        }),
        { numRuns: 100 }
      )
    })
  })

  describe('Property: Waterfall Data Completeness', () => {
    const waterfallDataItemArb = fc.record({
      name: fc.string({ minLength: 1, maxLength: 20 }),
      value: fc.integer({ min: -1000, max: 1000 }),
      isTotal: fc.constant(false)
    })

    const waterfallDataArb = fc.array(waterfallDataItemArb, { minLength: 1, maxLength: 10 })

    it('should include all input data items in output', () => {
      fc.assert(
        fc.property(waterfallDataArb, (data) => {
          const option = buildWaterfallOption(data, { showTotal: false })

          // All input names should be in categories
          const calculation = calculateWaterfallData(data, { showTotal: false })
          
          data.forEach(item => {
            expect(calculation.categories).toContain(item.name)
          })
        }),
        { numRuns: 100 }
      )
    })
  })
})

/**
 * 迷你图表属性测试
 * Mini Chart Property Tests
 * 
 * **属性 69: 迷你图表数据映射**
 * **属性 70: 图标集阈值判断**
 * **Validates: Requirements 14.4**
 * 
 * 测试内容:
 * 1. 迷你图表数据映射 - DataBar、Sparkline、ProgressBar的数据映射正确性
 * 2. 图标集阈值判断 - 根据阈值和比较操作符正确选择图标
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import {
  calculatePercentage,
  getIconForValue,
  calculateSparklinePoints,
  formatProgressLabel,
  PRESET_ICON_SETS,
  DEFAULT_DATA_BAR_CONFIG,
  DEFAULT_SPARKLINE_CONFIG,
  DEFAULT_PROGRESS_BAR_CONFIG,
  DEFAULT_ICON_SET_CONFIG,
} from '../types'
import type { IconSetItem } from '../types'

// ============================================================================
// Arbitraries (测试数据生成器)
// ============================================================================

/** 生成有效的数值（包括负数、零、正数） */
const numericValueArb = fc.double({ min: -10000, max: 10000, noNaN: true, noDefaultInfinity: true })

/** 生成正数 */
const positiveNumberArb = fc.double({ min: 0.01, max: 10000, noNaN: true, noDefaultInfinity: true })

/** 生成百分比值（0-1） */
const percentageValueArb = fc.double({ min: 0, max: 1, noNaN: true, noDefaultInfinity: true })

/** 生成扩展百分比值（0-2，支持超过100%的情况） */
const extendedPercentageArb = fc.double({ min: 0, max: 2, noNaN: true, noDefaultInfinity: true })

/** 生成有效的min/max范围 */
const rangeArb = fc.tuple(numericValueArb, numericValueArb)
  .filter(([a, b]) => a !== b && Math.abs(a - b) > 0.001) // 确保范围足够大以避免浮点精度问题
  .map(([a, b]) => ({ min: Math.min(a, b), max: Math.max(a, b) }))

/** 生成图表尺寸 */
const dimensionArb = fc.integer({ min: 20, max: 500 })

/** 生成数据数组（用于Sparkline） */
const dataArrayArb = fc.array(numericValueArb, { minLength: 2, maxLength: 50 })

/** 生成比较操作符 */
const operatorArb = fc.constantFrom('gte', 'gt', 'lte', 'lt', 'eq') as fc.Arbitrary<IconSetItem['operator']>

/** 生成颜色值 */
const colorArb = fc.constantFrom(
  '#ff0000', '#00ff00', '#0000ff', '#ffff00', '#ff00ff', '#00ffff',
  '#ff4d4f', '#52c41a', '#faad14', '#1890ff', '#722ed1', '#eb2f96'
)

/** 生成图标名称 */
const iconNameArb = fc.constantFrom(
  'arrow-up', 'arrow-down', 'arrow-right',
  'star-filled', 'star-half', 'star-empty',
  'circle-filled', 'flag', 'signal-full'
)

/** 生成单个图标集项 */
const iconSetItemArb: fc.Arbitrary<IconSetItem> = fc.record({
  icon: iconNameArb,
  color: colorArb,
  threshold: fc.double({ min: 0, max: 1, noNaN: true, noDefaultInfinity: true }),
  operator: operatorArb,
})

/** 生成有效的图标集（阈值递减排序） */
const validIconSetArb: fc.Arbitrary<IconSetItem[]> = fc
  .array(fc.double({ min: 0, max: 1, noNaN: true, noDefaultInfinity: true }), { minLength: 2, maxLength: 5 })
  .map(thresholds => {
    const sorted = [...new Set(thresholds)].sort((a, b) => b - a)
    return sorted.map((threshold, index) => ({
      icon: ['arrow-up', 'arrow-right', 'arrow-down', 'circle-filled', 'flag'][index % 5],
      color: ['#52c41a', '#faad14', '#ff4d4f', '#1890ff', '#722ed1'][index % 5],
      threshold,
      operator: 'gte' as const,
    }))
  })
  .filter(icons => icons.length >= 2)

/** 生成进度条格式字符串 */
const labelFormatArb = fc.constantFrom(
  '{percent}%',
  '{value}/{max}',
  '{value}',
  '{percent}% ({value}/{max})'
)

// ============================================================================
// 属性测试
// ============================================================================

describe('MiniChart Property Tests', () => {

  // ==========================================================================
  // 属性 69: 迷你图表数据映射
  // ==========================================================================
  
  describe('Property 69: Mini Chart Data Mapping', () => {
    
    // ------------------------------------------------------------------------
    // 69.1 DataBar 百分比计算正确性
    // ------------------------------------------------------------------------
    
    describe('69.1 DataBar Percentage Calculation', () => {
      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 69.1.1: 百分比计算应在0-100范围内
       * 对于任意数值和范围，计算的百分比应始终在0-100之间
       */
      it('Property 69.1.1: Percentage should always be between 0 and 100', () => {
        fc.assert(
          fc.property(
            numericValueArb,
            rangeArb,
            (value, { min, max }) => {
              const percentage = calculatePercentage(value, min, max)
              return percentage >= 0 && percentage <= 100
            }
          ),
          { numRuns: 200 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 69.1.2: 最小值应映射到0%
       * 当值等于最小值时，百分比应为0
       */
      it('Property 69.1.2: Minimum value should map to 0%', () => {
        fc.assert(
          fc.property(
            rangeArb,
            ({ min, max }) => {
              const percentage = calculatePercentage(min, min, max)
              return percentage === 0
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 69.1.3: 最大值应映射到100%
       * 当值等于最大值时，百分比应为100
       */
      it('Property 69.1.3: Maximum value should map to 100%', () => {
        fc.assert(
          fc.property(
            rangeArb,
            ({ min, max }) => {
              const percentage = calculatePercentage(max, min, max)
              return percentage === 100
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 69.1.4: 百分比计算应保持单调性
       * 对于相同范围内的两个值，较大的值应有较大或相等的百分比
       */
      it('Property 69.1.4: Percentage calculation should be monotonic', () => {
        fc.assert(
          fc.property(
            numericValueArb,
            numericValueArb,
            rangeArb,
            (value1, value2, { min, max }) => {
              const percentage1 = calculatePercentage(value1, min, max)
              const percentage2 = calculatePercentage(value2, min, max)
              
              if (value1 < value2) {
                return percentage1 <= percentage2
              } else if (value1 > value2) {
                return percentage1 >= percentage2
              } else {
                return percentage1 === percentage2
              }
            }
          ),
          { numRuns: 200 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 69.1.5: 中间值应映射到50%
       * 当值等于范围中点时，百分比应为50
       */
      it('Property 69.1.5: Midpoint value should map to 50%', () => {
        fc.assert(
          fc.property(
            rangeArb,
            ({ min, max }) => {
              const midpoint = (min + max) / 2
              const percentage = calculatePercentage(midpoint, min, max)
              return Math.abs(percentage - 50) < 0.0001
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 69.1.6: 相同min和max应返回0
       * 当min等于max时，应返回0避免除零错误
       */
      it('Property 69.1.6: Same min and max should return 0', () => {
        fc.assert(
          fc.property(
            numericValueArb,
            numericValueArb,
            (value, sameValue) => {
              const percentage = calculatePercentage(value, sameValue, sameValue)
              return percentage === 0
            }
          ),
          { numRuns: 50 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 69.1.7: 数据映射一致性 - 相同输入产生相同输出
       * 对于相同的输入参数，calculatePercentage应始终返回相同的结果
       */
      it('Property 69.1.7: Same input should always produce same output', () => {
        fc.assert(
          fc.property(
            numericValueArb,
            rangeArb,
            (value, { min, max }) => {
              const result1 = calculatePercentage(value, min, max)
              const result2 = calculatePercentage(value, min, max)
              return result1 === result2
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ------------------------------------------------------------------------
    // 69.2 Sparkline 点计算正确性
    // ------------------------------------------------------------------------
    
    describe('69.2 Sparkline Point Calculation', () => {
      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 69.2.1: 点数应等于数据长度
       * 对于任意数据数组，生成的点数应等于数据长度
       */
      it('Property 69.2.1: Number of points should equal data length', () => {
        fc.assert(
          fc.property(
            dataArrayArb,
            dimensionArb,
            dimensionArb,
            (data, width, height) => {
              const minY = Math.min(...data)
              const maxY = Math.max(...data)
              const points = calculateSparklinePoints(data, width, height, minY, maxY)
              return points.length === data.length
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 69.2.2: 所有点的X坐标应在有效范围内
       * 对于任意数据，所有点的X坐标应在0到width之间
       */
      it('Property 69.2.2: All X coordinates should be within bounds', () => {
        fc.assert(
          fc.property(
            dataArrayArb,
            dimensionArb,
            dimensionArb,
            (data, width, height) => {
              const minY = Math.min(...data)
              const maxY = Math.max(...data)
              const padding = 2
              const points = calculateSparklinePoints(data, width, height, minY, maxY, padding)
              
              return points.every(p => p.x >= 0 && p.x <= width)
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 69.2.3: 所有点的Y坐标应在有效范围内
       * 对于任意数据，所有点的Y坐标应在0到height之间
       */
      it('Property 69.2.3: All Y coordinates should be within bounds', () => {
        fc.assert(
          fc.property(
            dataArrayArb,
            dimensionArb,
            dimensionArb,
            (data, width, height) => {
              const minY = Math.min(...data)
              const maxY = Math.max(...data)
              const padding = 2
              const points = calculateSparklinePoints(data, width, height, minY, maxY, padding)
              
              return points.every(p => p.y >= 0 && p.y <= height)
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 69.2.4: X坐标应单调递增
       * 对于任意数据，点的X坐标应从左到右递增
       */
      it('Property 69.2.4: X coordinates should be monotonically increasing', () => {
        fc.assert(
          fc.property(
            dataArrayArb,
            dimensionArb,
            dimensionArb,
            (data, width, height) => {
              const minY = Math.min(...data)
              const maxY = Math.max(...data)
              const points = calculateSparklinePoints(data, width, height, minY, maxY)
              
              for (let i = 1; i < points.length; i++) {
                if (points[i].x < points[i - 1].x) {
                  return false
                }
              }
              return true
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 69.2.5: 最大值应映射到最低Y坐标（SVG坐标系Y轴向下）
       * 数据中的最大值应对应最小的Y坐标
       */
      it('Property 69.2.5: Maximum value should map to lowest Y coordinate', () => {
        fc.assert(
          fc.property(
            dataArrayArb.filter(arr => new Set(arr).size > 1), // 确保有不同的值
            dimensionArb,
            dimensionArb,
            (data, width, height) => {
              const minY = Math.min(...data)
              const maxY = Math.max(...data)
              const padding = 2
              const points = calculateSparklinePoints(data, width, height, minY, maxY, padding)
              
              const maxValueIndex = data.indexOf(Math.max(...data))
              const minYCoord = Math.min(...points.map(p => p.y))
              
              // 最大值对应的点应该有最小的Y坐标（或接近）
              return Math.abs(points[maxValueIndex].y - minYCoord) < 1
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 69.2.6: 空数据应返回空数组
       */
      it('Property 69.2.6: Empty data should return empty array', () => {
        const points = calculateSparklinePoints([], 100, 50, 0, 100)
        expect(points).toEqual([])
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 69.2.7: 单个数据点应返回单个点
       */
      it('Property 69.2.7: Single data point should return single point', () => {
        fc.assert(
          fc.property(
            numericValueArb,
            dimensionArb,
            dimensionArb,
            (value, width, height) => {
              const points = calculateSparklinePoints([value], width, height, value, value)
              return points.length === 1
            }
          ),
          { numRuns: 50 }
        )
      })
    })

    // ------------------------------------------------------------------------
    // 69.3 ProgressBar 百分比计算
    // ------------------------------------------------------------------------
    
    describe('69.3 ProgressBar Percentage Calculation', () => {
      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 69.3.1: 进度标签格式化应正确替换占位符
       * 对于任意值和格式字符串，应正确替换{value}、{max}、{percent}
       */
      it('Property 69.3.1: Progress label should correctly replace placeholders', () => {
        fc.assert(
          fc.property(
            positiveNumberArb,
            positiveNumberArb,
            (value, max) => {
              const actualMax = Math.max(value, max, 1) // 确保max >= value且 > 0
              const label = formatProgressLabel(value, actualMax, '{value}/{max}')
              
              return label === `${value}/${actualMax}`
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 69.3.2: 百分比格式应正确计算
       */
      it('Property 69.3.2: Percent format should calculate correctly', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 0, max: 100 }),
            fc.integer({ min: 1, max: 100 }),
            (value, max) => {
              const actualMax = Math.max(value, max)
              const label = formatProgressLabel(value, actualMax, '{percent}%')
              const expectedPercent = Math.round((value / actualMax) * 100)
              
              return label === `${expectedPercent}%`
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 69.3.3: 100%进度应正确显示
       */
      it('Property 69.3.3: 100% progress should display correctly', () => {
        fc.assert(
          fc.property(
            positiveNumberArb,
            (max) => {
              const label = formatProgressLabel(max, max, '{percent}%')
              return label === '100%'
            }
          ),
          { numRuns: 50 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 69.3.4: 0%进度应正确显示
       */
      it('Property 69.3.4: 0% progress should display correctly', () => {
        fc.assert(
          fc.property(
            positiveNumberArb,
            (max) => {
              const label = formatProgressLabel(0, max, '{percent}%')
              return label === '0%'
            }
          ),
          { numRuns: 50 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 69.3.5: 复合格式字符串应正确处理
       */
      it('Property 69.3.5: Compound format string should be handled correctly', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 0, max: 100 }),
            fc.integer({ min: 1, max: 100 }),
            (value, max) => {
              const actualMax = Math.max(value, max)
              const label = formatProgressLabel(value, actualMax, '{percent}% ({value}/{max})')
              const expectedPercent = Math.round((value / actualMax) * 100)
              
              return label === `${expectedPercent}% (${value}/${actualMax})`
            }
          ),
          { numRuns: 100 }
        )
      })
    })
  })


  // ==========================================================================
  // 属性 70: 图标集阈值判断
  // ==========================================================================
  
  describe('Property 70: Icon Set Threshold Evaluation', () => {
    
    // ------------------------------------------------------------------------
    // 70.1 图标选择基于阈值
    // ------------------------------------------------------------------------
    
    describe('70.1 Icon Selection Based on Thresholds', () => {
      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 70.1.1: 应始终返回一个图标（非空图标集）
       * 对于任意数值和非空图标集，应始终返回一个图标
       */
      it('Property 70.1.1: Should always return an icon for non-empty icon set', () => {
        fc.assert(
          fc.property(
            numericValueArb,
            validIconSetArb,
            (value, icons) => {
              const result = getIconForValue(value, icons)
              return result !== null
            }
          ),
          { numRuns: 200 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 70.1.2: 返回的图标应来自图标集
       * 对于任意数值，返回的图标应是图标集中的一个
       */
      it('Property 70.1.2: Returned icon should be from the icon set', () => {
        fc.assert(
          fc.property(
            numericValueArb,
            validIconSetArb,
            (value, icons) => {
              const result = getIconForValue(value, icons)
              if (result === null) return icons.length === 0
              
              return icons.some(icon => 
                icon.icon === result.icon && 
                icon.color === result.color &&
                icon.threshold === result.threshold
              )
            }
          ),
          { numRuns: 200 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 70.1.3: 高于最高阈值的值应返回第一个图标
       * 对于高于所有阈值的值，应返回最高阈值对应的图标
       */
      it('Property 70.1.3: Value above highest threshold should return first icon', () => {
        fc.assert(
          fc.property(
            validIconSetArb,
            (icons) => {
              const highestThreshold = Math.max(...icons.map(i => i.threshold))
              const valueAboveAll = highestThreshold + 1
              
              const result = getIconForValue(valueAboveAll, icons)
              if (result === null) return false
              
              // 应返回最高阈值的图标
              return result.threshold === highestThreshold
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ------------------------------------------------------------------------
    // 70.2 阈值比较操作符
    // ------------------------------------------------------------------------
    
    describe('70.2 Threshold Comparison Operators', () => {
      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 70.2.1: gte操作符应正确处理大于等于
       * 对于gte操作符，值>=阈值时应匹配
       */
      it('Property 70.2.1: gte operator should match value >= threshold', () => {
        fc.assert(
          fc.property(
            numericValueArb,
            numericValueArb,
            colorArb,
            (value, threshold, color) => {
              const icons: IconSetItem[] = [
                { icon: 'test-icon', color, threshold, operator: 'gte' },
              ]
              
              const result = getIconForValue(value, icons)
              
              if (value >= threshold) {
                return result !== null && result.icon === 'test-icon'
              } else {
                // 当值小于阈值时，由于只有一个图标，会作为fallback返回
                return result !== null
              }
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 70.2.2: gt操作符应正确处理大于
       * 对于gt操作符，值>阈值时应匹配
       */
      it('Property 70.2.2: gt operator should match value > threshold', () => {
        fc.assert(
          fc.property(
            numericValueArb,
            numericValueArb,
            colorArb,
            (value, threshold, color) => {
              const icons: IconSetItem[] = [
                { icon: 'high', color: '#52c41a', threshold, operator: 'gt' },
                { icon: 'low', color: '#ff4d4f', threshold: -Infinity, operator: 'gte' },
              ]
              
              const result = getIconForValue(value, icons)
              
              if (value > threshold) {
                return result !== null && result.icon === 'high'
              } else {
                return result !== null && result.icon === 'low'
              }
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 70.2.3: lte操作符应正确处理小于等于
       * 对于lte操作符，值<=阈值时应匹配
       */
      it('Property 70.2.3: lte operator should match value <= threshold', () => {
        fc.assert(
          fc.property(
            numericValueArb,
            numericValueArb,
            colorArb,
            (value, threshold, color) => {
              const icons: IconSetItem[] = [
                { icon: 'test-icon', color, threshold, operator: 'lte' },
              ]
              
              const result = getIconForValue(value, icons)
              
              if (value <= threshold) {
                return result !== null && result.icon === 'test-icon'
              } else {
                // fallback
                return result !== null
              }
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 70.2.4: lt操作符应正确处理小于
       * 对于lt操作符，值<阈值时应匹配
       */
      it('Property 70.2.4: lt operator should match value < threshold', () => {
        fc.assert(
          fc.property(
            numericValueArb,
            fc.double({ min: 1, max: 10000, noNaN: true, noDefaultInfinity: true }),
            colorArb,
            (value, threshold, color) => {
              // 使用lt操作符，当值小于阈值时匹配
              const icons: IconSetItem[] = [
                { icon: 'below', color, threshold, operator: 'lt' },
              ]
              
              const result = getIconForValue(value, icons)
              
              // 由于只有一个图标，它会作为fallback返回
              // 但我们验证的是当值<阈值时，lt条件确实匹配
              if (value < threshold) {
                // 值小于阈值，lt条件匹配，返回该图标
                return result !== null && result.icon === 'below'
              } else {
                // 值>=阈值，lt条件不匹配，但作为fallback仍返回该图标
                return result !== null
              }
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 70.2.5: eq操作符应正确处理等于
       * 对于eq操作符，值===阈值时应匹配
       */
      it('Property 70.2.5: eq operator should match value === threshold', () => {
        fc.assert(
          fc.property(
            numericValueArb,
            colorArb,
            (threshold, color) => {
              const icons: IconSetItem[] = [
                { icon: 'exact', color, threshold, operator: 'eq' },
                { icon: 'other', color: '#ff4d4f', threshold: -Infinity, operator: 'gte' },
              ]
              
              const result = getIconForValue(threshold, icons)
              
              return result !== null && result.icon === 'exact'
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ------------------------------------------------------------------------
    // 70.3 图标集排序（最高阈值优先）
    // ------------------------------------------------------------------------
    
    describe('70.3 Icon Set Ordering (Highest Threshold First)', () => {
      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 70.3.1: 图标应按阈值从高到低评估
       * 对于多个满足条件的图标，应返回最高阈值的图标
       */
      it('Property 70.3.1: Icons should be evaluated from highest to lowest threshold', () => {
        fc.assert(
          fc.property(
            percentageValueArb,
            (value) => {
              // 使用预设的traffic图标集
              const icons = PRESET_ICON_SETS.traffic
              const result = getIconForValue(value, icons)
              
              if (result === null) return false
              
              // 验证返回的是正确阈值区间的图标
              if (value >= 0.7) {
                return result.color === '#52c41a' // 绿色
              } else if (value >= 0.4) {
                return result.color === '#faad14' // 黄色
              } else {
                return result.color === '#ff4d4f' // 红色
              }
            }
          ),
          { numRuns: 200 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 70.3.2: 预设arrows图标集应正确工作
       * 正数返回上箭头，零返回右箭头，负数返回下箭头
       */
      it('Property 70.3.2: Preset arrows icon set should work correctly', () => {
        fc.assert(
          fc.property(
            numericValueArb,
            (value) => {
              const icons = PRESET_ICON_SETS.arrows
              const result = getIconForValue(value, icons)
              
              if (result === null) return false
              
              if (value > 0) {
                return result.icon === 'arrow-up' && result.color === '#52c41a'
              } else if (value === 0) {
                return result.icon === 'arrow-right' && result.color === '#faad14'
              } else {
                return result.icon === 'arrow-down' && result.color === '#ff4d4f'
              }
            }
          ),
          { numRuns: 200 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 70.3.3: 预设stars图标集应正确工作
       */
      it('Property 70.3.3: Preset stars icon set should work correctly', () => {
        fc.assert(
          fc.property(
            percentageValueArb,
            (value) => {
              const icons = PRESET_ICON_SETS.stars
              const result = getIconForValue(value, icons)
              
              if (result === null) return false
              
              if (value >= 0.8) {
                return result.icon === 'star-filled'
              } else if (value >= 0.5) {
                return result.icon === 'star-half'
              } else {
                return result.icon === 'star-empty'
              }
            }
          ),
          { numRuns: 200 }
        )
      })
    })

    // ------------------------------------------------------------------------
    // 70.4 Fallback到最后一个图标
    // ------------------------------------------------------------------------
    
    describe('70.4 Fallback to Last Icon', () => {
      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 70.4.1: 当没有阈值匹配时应返回最后一个图标
       * 对于低于所有阈值的值，应返回最后一个图标作为fallback
       */
      it('Property 70.4.1: Should return last icon when no threshold matches', () => {
        fc.assert(
          fc.property(
            fc.double({ min: -1000, max: -0.01, noNaN: true, noDefaultInfinity: true }),
            (negativeValue) => {
              // 创建一个所有阈值都为正数的图标集
              const icons: IconSetItem[] = [
                { icon: 'high', color: '#52c41a', threshold: 0.7, operator: 'gte' },
                { icon: 'medium', color: '#faad14', threshold: 0.4, operator: 'gte' },
                { icon: 'low', color: '#ff4d4f', threshold: 0.1, operator: 'gte' },
              ]
              
              const result = getIconForValue(negativeValue, icons)
              
              // 应返回最后一个图标作为fallback
              return result !== null && result.icon === 'low'
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 70.4.2: 空图标集应返回null
       */
      it('Property 70.4.2: Empty icon set should return null', () => {
        fc.assert(
          fc.property(
            numericValueArb,
            (value) => {
              const result = getIconForValue(value, [])
              return result === null
            }
          ),
          { numRuns: 50 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 70.4.3: 单个图标的图标集应始终返回该图标
       */
      it('Property 70.4.3: Single icon set should always return that icon', () => {
        fc.assert(
          fc.property(
            numericValueArb,
            iconSetItemArb,
            (value, icon) => {
              const result = getIconForValue(value, [icon])
              return result !== null && result.icon === icon.icon
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ------------------------------------------------------------------------
    // 70.5 预设图标集完整性测试
    // ------------------------------------------------------------------------
    
    describe('70.5 Preset Icon Sets Completeness', () => {
      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 70.5.1: 所有预设图标集应对任意值返回图标
       */
      it('Property 70.5.1: All preset icon sets should return icon for any value', () => {
        const presetNames: (keyof typeof PRESET_ICON_SETS)[] = [
          'arrows', 'arrows3', 'stars', 'traffic', 'flags', 'ratings', 'signals', 'quarters'
        ]
        
        fc.assert(
          fc.property(
            fc.constantFrom(...presetNames),
            numericValueArb,
            (presetName, value) => {
              const icons = PRESET_ICON_SETS[presetName]
              const result = getIconForValue(value, icons)
              return result !== null
            }
          ),
          { numRuns: 200 }
        )
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 70.5.2: ratings图标集应有5个级别
       */
      it('Property 70.5.2: Ratings icon set should have 5 levels', () => {
        expect(PRESET_ICON_SETS.ratings.length).toBe(5)
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 70.5.3: signals图标集应有4个级别
       */
      it('Property 70.5.3: Signals icon set should have 4 levels', () => {
        expect(PRESET_ICON_SETS.signals.length).toBe(4)
      })

      /**
       * **Validates: Requirements 14.4**
       * 
       * 属性 70.5.4: quarters图标集应有4个级别
       */
      it('Property 70.5.4: Quarters icon set should have 4 levels', () => {
        expect(PRESET_ICON_SETS.quarters.length).toBe(4)
      })
    })
  })


  // ==========================================================================
  // 边界条件和一致性测试
  // ==========================================================================
  
  describe('Edge Cases and Consistency', () => {
    /**
     * **Validates: Requirements 14.4**
     * 
     * 数据映射一致性：相同输入应产生相同输出
     */
    it('Data mapping consistency: same input should produce same output', () => {
      fc.assert(
        fc.property(
          numericValueArb,
          rangeArb,
          (value, { min, max }) => {
            const result1 = calculatePercentage(value, min, max)
            const result2 = calculatePercentage(value, min, max)
            const result3 = calculatePercentage(value, min, max)
            
            return result1 === result2 && result2 === result3
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 14.4**
     * 
     * 图标选择一致性：相同输入应产生相同输出
     */
    it('Icon selection consistency: same input should produce same output', () => {
      fc.assert(
        fc.property(
          numericValueArb,
          validIconSetArb,
          (value, icons) => {
            const result1 = getIconForValue(value, icons)
            const result2 = getIconForValue(value, icons)
            const result3 = getIconForValue(value, icons)
            
            if (result1 === null) {
              return result2 === null && result3 === null
            }
            
            return (
              result1.icon === result2?.icon &&
              result2.icon === result3?.icon &&
              result1.color === result2?.color &&
              result2.color === result3?.color
            )
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 14.4**
     * 
     * Sparkline点计算一致性
     */
    it('Sparkline point calculation consistency', () => {
      fc.assert(
        fc.property(
          dataArrayArb,
          dimensionArb,
          dimensionArb,
          (data, width, height) => {
            const minY = Math.min(...data)
            const maxY = Math.max(...data)
            
            const points1 = calculateSparklinePoints(data, width, height, minY, maxY)
            const points2 = calculateSparklinePoints(data, width, height, minY, maxY)
            
            if (points1.length !== points2.length) return false
            
            for (let i = 0; i < points1.length; i++) {
              if (points1[i].x !== points2[i].x || points1[i].y !== points2[i].y) {
                return false
              }
            }
            return true
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 14.4**
     * 
     * 默认配置应有有效值
     */
    it('Default configurations should have valid values', () => {
      // DataBar defaults
      expect(DEFAULT_DATA_BAR_CONFIG.min).toBeDefined()
      expect(DEFAULT_DATA_BAR_CONFIG.max).toBeDefined()
      expect(DEFAULT_DATA_BAR_CONFIG.color).toBeDefined()
      
      // Sparkline defaults
      expect(DEFAULT_SPARKLINE_CONFIG.strokeWidth).toBeGreaterThan(0)
      expect(DEFAULT_SPARKLINE_CONFIG.pointRadius).toBeGreaterThan(0)
      
      // ProgressBar defaults
      expect(DEFAULT_PROGRESS_BAR_CONFIG.max).toBeGreaterThan(0)
      expect(DEFAULT_PROGRESS_BAR_CONFIG.height).toBeGreaterThan(0)
      
      // IconSet defaults
      expect(DEFAULT_ICON_SET_CONFIG.icons.length).toBeGreaterThan(0)
      expect(DEFAULT_ICON_SET_CONFIG.iconSize).toBeGreaterThan(0)
    })
  })
})

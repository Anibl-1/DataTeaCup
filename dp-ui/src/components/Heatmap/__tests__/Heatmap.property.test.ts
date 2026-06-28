/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 热力图属性测试
 * Heatmap Property Tests
 * 
 * **属性 77: 热力图色阶映射**
 * **Validates: Requirements 22.1**
 * 
 * 测试内容:
 * 1. 色阶映射正确性 - 数值应正确映射到对应颜色
 * 2. 边界值处理 - 最小值、最大值、中间值的颜色映射
 * 3. 颜色插值 - 颜色渐变的单调性和正确性
 * 4. 自定义色阶 - 多色渐变的正确处理
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import {
  getColorForValue,
  interpolateColor,
  interpolateColorStops,
  calculateDataRange,
  normalizeData,
  getContrastColor,
  parseColor,
  PRESET_COLOR_SCALES,
} from '../types'
import type { ColorScaleConfig, ColorStop, HeatmapData } from '../types'

// ============================================================================
// Arbitraries (测试数据生成器)
// ============================================================================

/** 生成有效的数值（包括负数、零、正数） */
const numericValueArb = fc.double({ min: -10000, max: 10000, noNaN: true, noDefaultInfinity: true })

/** 生成正数 */
const positiveNumberArb = fc.double({ min: 0.01, max: 10000, noNaN: true, noDefaultInfinity: true })

/** 生成0-1范围的位置值 */
const positionArb = fc.double({ min: 0, max: 1, noNaN: true, noDefaultInfinity: true })

/** 生成有效的min/max范围 */
const rangeArb = fc.tuple(numericValueArb, numericValueArb)
  .filter(([a, b]) => a !== b && Math.abs(a - b) > 0.001)
  .map(([a, b]) => ({ min: Math.min(a, b), max: Math.max(a, b) }))

/** 生成有效的十六进制颜色 */
const hexColorArb = fc.tuple(
  fc.integer({ min: 0, max: 255 }),
  fc.integer({ min: 0, max: 255 }),
  fc.integer({ min: 0, max: 255 })
).map(([r, g, b]) => `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`)


/** 生成RGB颜色字符串 */
const rgbColorArb = fc.tuple(
  fc.integer({ min: 0, max: 255 }),
  fc.integer({ min: 0, max: 255 }),
  fc.integer({ min: 0, max: 255 })
).map(([r, g, b]) => `rgb(${r}, ${g}, ${b})`)

/** 生成任意有效颜色 */
const colorArb = fc.oneof(hexColorArb, rgbColorArb)

/** 生成基础色阶配置（2色） */
const twoColorScaleArb: fc.Arbitrary<ColorScaleConfig> = fc.record({
  minColor: hexColorArb,
  maxColor: hexColorArb,
})

/** 生成三色色阶配置 */
const threeColorScaleArb: fc.Arbitrary<ColorScaleConfig> = fc.record({
  minColor: hexColorArb,
  midColor: hexColorArb,
  maxColor: hexColorArb,
})

/** 生成色阶点 */
const colorStopArb: fc.Arbitrary<ColorStop> = fc.record({
  position: positionArb,
  color: hexColorArb,
})

/** 生成有效的色阶点数组（至少2个，位置排序，确保首尾包含0和1） */
const colorStopsArb: fc.Arbitrary<ColorStop[]> = fc
  .array(fc.integer({ min: 1, max: 99 }), { minLength: 0, maxLength: 4 })
  .map(middlePositions => {
    // 始终包含0和1作为首尾
    const positions = [0, ...middlePositions.map(p => p / 100), 1]
    const uniquePositions = [...new Set(positions)].sort((a, b) => a - b)
    const colors = ['#ff0000', '#00ff00', '#0000ff', '#ffff00', '#ff00ff', '#00ffff']
    return uniquePositions.map((position, i) => ({
      position,
      color: colors[i % colors.length],
    }))
  })
  .filter(stops => stops.length >= 2)

/** 生成带自定义色阶点的配置 */
const customColorScaleArb: fc.Arbitrary<ColorScaleConfig> = fc.record({
  minColor: hexColorArb,
  maxColor: hexColorArb,
  colorStops: colorStopsArb,
})

/** 生成2D数值数组 */
const data2DArb = fc.array(
  fc.array(fc.oneof(numericValueArb, fc.constant(null)), { minLength: 1, maxLength: 10 }),
  { minLength: 1, maxLength: 10 }
)


// ============================================================================
// Helper Functions
// ============================================================================

/**
 * 解析RGB颜色字符串为RGB对象
 */
function parseRgbString(rgb: string): { r: number; g: number; b: number } | null {
  const match = rgb.match(/rgb\((\d+),\s*(\d+),\s*(\d+)\)/)
  if (match) {
    return {
      r: parseInt(match[1], 10),
      g: parseInt(match[2], 10),
      b: parseInt(match[3], 10),
    }
  }
  return null
}

/**
 * 计算两个颜色之间的欧几里得距离
 */
function colorDistance(color1: string, color2: string): number {
  const c1 = parseColor(color1)
  const c2 = parseColor(color2)
  return Math.sqrt(
    Math.pow(c1.r - c2.r, 2) +
    Math.pow(c1.g - c2.g, 2) +
    Math.pow(c1.b - c2.b, 2)
  )
}

/**
 * 检查颜色是否为有效的RGB格式
 */
function isValidRgbColor(color: string): boolean {
  return /^rgb\(\d+,\s*\d+,\s*\d+\)$/.test(color)
}

// ============================================================================
// 属性测试
// ============================================================================

describe('Heatmap Property Tests', () => {

  // ==========================================================================
  // 属性 77: 热力图色阶映射
  // ==========================================================================
  
  describe('Property 77: Heatmap Color Scale Mapping', () => {
    
    // ------------------------------------------------------------------------
    // 77.1 基本颜色映射
    // ------------------------------------------------------------------------
    
    describe('77.1 Basic Color Mapping', () => {
      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.1.1: 范围内的任意值应返回有效颜色
       * 对于任意在[min, max]范围内的值，getColorForValue应返回有效的RGB颜色
       */
      it('Property 77.1.1: Any value in range should return a valid color', () => {
        fc.assert(
          fc.property(
            rangeArb,
            twoColorScaleArb,
            ({ min, max }, config) => {
              // 生成范围内的随机值
              const value = min + Math.random() * (max - min)
              const color = getColorForValue(value, config, min, max)
              
              return isValidRgbColor(color)
            }
          ),
          { numRuns: 200 }
        )
      })


      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.1.2: 最小值应映射到minColor
       * 当值等于最小值时，返回的颜色应等于或非常接近minColor
       */
      it('Property 77.1.2: Minimum value should map to minColor', () => {
        fc.assert(
          fc.property(
            rangeArb,
            twoColorScaleArb,
            ({ min, max }, config) => {
              const color = getColorForValue(min, config, min, max)
              const distance = colorDistance(color, config.minColor)
              
              // 允许小的浮点误差
              return distance < 3
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.1.3: 最大值应映射到maxColor
       * 当值等于最大值时，返回的颜色应等于或非常接近maxColor
       */
      it('Property 77.1.3: Maximum value should map to maxColor', () => {
        fc.assert(
          fc.property(
            rangeArb,
            twoColorScaleArb,
            ({ min, max }, config) => {
              const color = getColorForValue(max, config, min, max)
              const distance = colorDistance(color, config.maxColor)
              
              return distance < 3
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.1.4: 中间值应映射到midColor（三色色阶）
       * 当配置了midColor且值等于中点时，返回的颜色应接近midColor
       */
      it('Property 77.1.4: Middle value should map to midColor (3-color scale)', () => {
        fc.assert(
          fc.property(
            rangeArb,
            threeColorScaleArb,
            ({ min, max }, config) => {
              const midValue = (min + max) / 2
              const color = getColorForValue(midValue, config, min, max)
              const distance = colorDistance(color, config.midColor!)
              
              return distance < 3
            }
          ),
          { numRuns: 100 }
        )
      })
    })


    // ------------------------------------------------------------------------
    // 77.2 边界值处理
    // ------------------------------------------------------------------------
    
    describe('77.2 Boundary Value Handling', () => {
      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.2.1: 超出范围的值应被钳制
       * 小于min的值应映射到minColor，大于max的值应映射到maxColor
       */
      it('Property 77.2.1: Values outside range should be clamped', () => {
        fc.assert(
          fc.property(
            rangeArb,
            twoColorScaleArb,
            positiveNumberArb,
            ({ min, max }, config, offset) => {
              // 测试小于min的值
              const belowMin = min - offset
              const colorBelow = getColorForValue(belowMin, config, min, max)
              const distanceBelow = colorDistance(colorBelow, config.minColor)
              
              // 测试大于max的值
              const aboveMax = max + offset
              const colorAbove = getColorForValue(aboveMax, config, min, max)
              const distanceAbove = colorDistance(colorAbove, config.maxColor)
              
              return distanceBelow < 3 && distanceAbove < 3
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.2.2: null值应返回nullColor
       * 当值为null时，应返回指定的nullColor
       */
      it('Property 77.2.2: Null values should return nullColor', () => {
        fc.assert(
          fc.property(
            twoColorScaleArb,
            hexColorArb,
            (config, nullColor) => {
              const color = getColorForValue(null, config, 0, 100, nullColor)
              const distance = colorDistance(color, nullColor)
              
              return distance < 3
            }
          ),
          { numRuns: 50 }
        )
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.2.3: NaN值应返回nullColor
       * 当值为NaN时，应返回指定的nullColor
       */
      it('Property 77.2.3: NaN values should return nullColor', () => {
        fc.assert(
          fc.property(
            twoColorScaleArb,
            hexColorArb,
            (config, nullColor) => {
              const color = getColorForValue(NaN, config, 0, 100, nullColor)
              const distance = colorDistance(color, nullColor)
              
              return distance < 3
            }
          ),
          { numRuns: 50 }
        )
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.2.4: undefined值应返回nullColor
       */
      it('Property 77.2.4: Undefined values should return nullColor', () => {
        fc.assert(
          fc.property(
            twoColorScaleArb,
            hexColorArb,
            (config, nullColor) => {
              const color = getColorForValue(undefined as any, config, 0, 100, nullColor)
              const distance = colorDistance(color, nullColor)
              
              return distance < 3
            }
          ),
          { numRuns: 50 }
        )
      })
    })


    // ------------------------------------------------------------------------
    // 77.3 颜色插值单调性
    // ------------------------------------------------------------------------
    
    describe('77.3 Color Interpolation Monotonicity', () => {
      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.3.1: 颜色插值应保持单调性
       * 对于两色色阶，随着值增加，颜色应逐渐从minColor过渡到maxColor
       */
      it('Property 77.3.1: Color interpolation should be monotonic', () => {
        fc.assert(
          fc.property(
            rangeArb,
            twoColorScaleArb,
            ({ min, max }, config) => {
              // 生成一系列递增的值
              const steps = 10
              const values: number[] = []
              for (let i = 0; i <= steps; i++) {
                values.push(min + (max - min) * (i / steps))
              }
              
              // 计算每个值对应的颜色到minColor的距离
              const distances = values.map(v => {
                const color = getColorForValue(v, config, min, max)
                return colorDistance(color, config.minColor)
              })
              
              // 距离应该单调递增（或保持不变）
              for (let i = 1; i < distances.length; i++) {
                if (distances[i] < distances[i - 1] - 1) { // 允许小误差
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
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.3.2: 相同输入应产生相同输出
       * 对于相同的输入参数，getColorForValue应始终返回相同的结果
       */
      it('Property 77.3.2: Same input should always produce same output', () => {
        fc.assert(
          fc.property(
            numericValueArb,
            rangeArb,
            twoColorScaleArb,
            (value, { min, max }, config) => {
              const color1 = getColorForValue(value, config, min, max)
              const color2 = getColorForValue(value, config, min, max)
              
              return color1 === color2
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.3.3: 插值因子0应返回第一个颜色
       */
      it('Property 77.3.3: Interpolation factor 0 should return first color', () => {
        fc.assert(
          fc.property(
            hexColorArb,
            hexColorArb,
            (color1, color2) => {
              const result = interpolateColor(color1, color2, 0)
              const distance = colorDistance(result, color1)
              
              return distance < 3
            }
          ),
          { numRuns: 50 }
        )
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.3.4: 插值因子1应返回第二个颜色
       */
      it('Property 77.3.4: Interpolation factor 1 should return second color', () => {
        fc.assert(
          fc.property(
            hexColorArb,
            hexColorArb,
            (color1, color2) => {
              const result = interpolateColor(color1, color2, 1)
              const distance = colorDistance(result, color2)
              
              return distance < 3
            }
          ),
          { numRuns: 50 }
        )
      })
    })


    // ------------------------------------------------------------------------
    // 77.4 自定义色阶点
    // ------------------------------------------------------------------------
    
    describe('77.4 Custom Color Stops', () => {
      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.4.1: 自定义色阶点应被正确使用
       * 当配置了colorStops时，应使用colorStops进行插值
       */
      it('Property 77.4.1: Custom color stops should be used correctly', () => {
        fc.assert(
          fc.property(
            colorStopsArb,
            positionArb,
            (stops, position) => {
              const color = interpolateColorStops(stops, position)
              
              return isValidRgbColor(color)
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.4.2: 位置0应返回第一个色阶点的颜色
       */
      it('Property 77.4.2: Position 0 should return first stop color', () => {
        fc.assert(
          fc.property(
            colorStopsArb,
            (stops) => {
              const sortedStops = [...stops].sort((a, b) => a.position - b.position)
              const color = interpolateColorStops(stops, 0)
              const firstStop = sortedStops[0]
              const distance = colorDistance(color, firstStop.color)
              
              return distance < 3
            }
          ),
          { numRuns: 50 }
        )
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.4.3: 位置1应返回最后一个色阶点的颜色
       */
      it('Property 77.4.3: Position 1 should return last stop color', () => {
        fc.assert(
          fc.property(
            colorStopsArb,
            (stops) => {
              const sortedStops = [...stops].sort((a, b) => a.position - b.position)
              const color = interpolateColorStops(stops, 1)
              const lastStop = sortedStops[sortedStops.length - 1]
              const distance = colorDistance(color, lastStop.color)
              
              return distance < 3
            }
          ),
          { numRuns: 50 }
        )
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.4.4: 色阶点位置处应返回该点的颜色
       */
      it('Property 77.4.4: Stop position should return that stop color', () => {
        fc.assert(
          fc.property(
            colorStopsArb,
            (stops) => {
              // 测试每个色阶点位置
              for (const stop of stops) {
                const color = interpolateColorStops(stops, stop.position)
                const distance = colorDistance(color, stop.color)
                if (distance >= 3) {
                  return false
                }
              }
              return true
            }
          ),
          { numRuns: 50 }
        )
      })
    })


    // ------------------------------------------------------------------------
    // 77.5 数据范围计算
    // ------------------------------------------------------------------------
    
    describe('77.5 Data Range Calculation', () => {
      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.5.1: 计算的min应小于等于所有非null值
       */
      it('Property 77.5.1: Calculated min should be <= all non-null values', () => {
        fc.assert(
          fc.property(
            data2DArb.filter(data => data.some(row => row.some(v => v !== null && !isNaN(v as number)))),
            (data) => {
              const { min } = calculateDataRange(data as (number | null)[][])
              
              for (const row of data) {
                for (const value of row) {
                  if (value !== null && !isNaN(value as number)) {
                    if (min > (value as number)) {
                      return false
                    }
                  }
                }
              }
              return true
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.5.2: 计算的max应大于等于所有非null值
       */
      it('Property 77.5.2: Calculated max should be >= all non-null values', () => {
        fc.assert(
          fc.property(
            data2DArb.filter(data => data.some(row => row.some(v => v !== null && !isNaN(v as number)))),
            (data) => {
              const { max } = calculateDataRange(data as (number | null)[][])
              
              for (const row of data) {
                for (const value of row) {
                  if (value !== null && !isNaN(value as number)) {
                    if (max < (value as number)) {
                      return false
                    }
                  }
                }
              }
              return true
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.5.3: min应始终小于max
       */
      it('Property 77.5.3: min should always be less than max', () => {
        fc.assert(
          fc.property(
            data2DArb,
            (data) => {
              const { min, max } = calculateDataRange(data as (number | null)[][])
              
              return min < max
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.5.4: 空数据应返回默认范围
       */
      it('Property 77.5.4: Empty data should return default range', () => {
        const { min, max } = calculateDataRange([])
        
        expect(min).toBe(0)
        expect(max).toBe(100)
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.5.5: 全null数据应返回默认范围
       */
      it('Property 77.5.5: All-null data should return default range', () => {
        const data: (number | null)[][] = [[null, null], [null, null]]
        const { min, max } = calculateDataRange(data)
        
        expect(min).toBe(0)
        expect(max).toBe(100)
      })
    })


    // ------------------------------------------------------------------------
    // 77.6 对比色计算
    // ------------------------------------------------------------------------
    
    describe('77.6 Contrast Color Calculation', () => {
      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.6.1: 对比色应为黑色或白色
       */
      it('Property 77.6.1: Contrast color should be black or white', () => {
        fc.assert(
          fc.property(
            hexColorArb,
            (backgroundColor) => {
              const contrastColor = getContrastColor(backgroundColor)
              
              return contrastColor === '#000000' || contrastColor === '#ffffff'
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.6.2: 深色背景应返回白色文字
       */
      it('Property 77.6.2: Dark background should return white text', () => {
        // 测试一些已知的深色
        const darkColors = ['#000000', '#1a1a1a', '#333333', '#0000ff', '#000080']
        
        for (const color of darkColors) {
          const contrastColor = getContrastColor(color)
          expect(contrastColor).toBe('#ffffff')
        }
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.6.3: 浅色背景应返回黑色文字
       */
      it('Property 77.6.3: Light background should return black text', () => {
        // 测试一些已知的浅色
        const lightColors = ['#ffffff', '#f0f0f0', '#ffff00', '#00ffff', '#f5f5f5']
        
        for (const color of lightColors) {
          const contrastColor = getContrastColor(color)
          expect(contrastColor).toBe('#000000')
        }
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.6.4: 相同输入应产生相同输出
       */
      it('Property 77.6.4: Same input should always produce same output', () => {
        fc.assert(
          fc.property(
            hexColorArb,
            (backgroundColor) => {
              const result1 = getContrastColor(backgroundColor)
              const result2 = getContrastColor(backgroundColor)
              
              return result1 === result2
            }
          ),
          { numRuns: 50 }
        )
      })
    })


    // ------------------------------------------------------------------------
    // 77.7 预设色阶
    // ------------------------------------------------------------------------
    
    describe('77.7 Preset Color Scales', () => {
      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.7.1: 所有预设色阶应产生有效颜色
       */
      it('Property 77.7.1: All preset color scales should produce valid colors', () => {
        const presetNames = Object.keys(PRESET_COLOR_SCALES) as (keyof typeof PRESET_COLOR_SCALES)[]
        
        fc.assert(
          fc.property(
            fc.constantFrom(...presetNames),
            numericValueArb,
            rangeArb,
            (presetName, value, { min, max }) => {
              const config = PRESET_COLOR_SCALES[presetName]
              const color = getColorForValue(value, config, min, max)
              
              return isValidRgbColor(color)
            }
          ),
          { numRuns: 200 }
        )
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.7.2: 预设色阶的边界值应正确映射
       */
      it('Property 77.7.2: Preset color scale boundary values should map correctly', () => {
        const presetNames = Object.keys(PRESET_COLOR_SCALES) as (keyof typeof PRESET_COLOR_SCALES)[]
        
        for (const presetName of presetNames) {
          const config = PRESET_COLOR_SCALES[presetName]
          const min = 0
          const max = 100
          
          // 测试最小值
          const minColor = getColorForValue(min, config, min, max)
          expect(isValidRgbColor(minColor)).toBe(true)
          
          // 测试最大值
          const maxColor = getColorForValue(max, config, min, max)
          expect(isValidRgbColor(maxColor)).toBe(true)
          
          // 测试中间值
          const midColor = getColorForValue(50, config, min, max)
          expect(isValidRgbColor(midColor)).toBe(true)
        }
      })
    })


    // ------------------------------------------------------------------------
    // 77.8 数据规范化
    // ------------------------------------------------------------------------
    
    describe('77.8 Data Normalization', () => {
      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.8.1: 2D数组应保持不变
       */
      it('Property 77.8.1: 2D array should remain unchanged', () => {
        fc.assert(
          fc.property(
            fc.array(
              fc.array(fc.oneof(numericValueArb, fc.constant(null)), { minLength: 1, maxLength: 5 }),
              { minLength: 1, maxLength: 5 }
            ),
            (data) => {
              const normalized = normalizeData(data as HeatmapData)
              
              // 检查维度
              if (normalized.length !== data.length) return false
              
              for (let i = 0; i < data.length; i++) {
                if (normalized[i].length !== data[i].length) return false
                
                for (let j = 0; j < data[i].length; j++) {
                  if (normalized[i][j] !== data[i][j]) return false
                }
              }
              
              return true
            }
          ),
          { numRuns: 50 }
        )
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.8.2: 空数据应返回空数组
       */
      it('Property 77.8.2: Empty data should return empty array', () => {
        const result = normalizeData([])
        expect(result).toEqual([])
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.8.3: HeatmapCell数组应正确转换为2D数组
       */
      it('Property 77.8.3: HeatmapCell array should convert to 2D array correctly', () => {
        const cells = [
          { row: 0, col: 0, value: 10 },
          { row: 0, col: 1, value: 20 },
          { row: 1, col: 0, value: 30 },
          { row: 1, col: 1, value: 40 },
        ]
        
        const result = normalizeData(cells)
        
        expect(result.length).toBe(2)
        expect(result[0].length).toBe(2)
        expect(result[0][0]).toBe(10)
        expect(result[0][1]).toBe(20)
        expect(result[1][0]).toBe(30)
        expect(result[1][1]).toBe(40)
      })
    })


    // ------------------------------------------------------------------------
    // 77.9 颜色解析
    // ------------------------------------------------------------------------
    
    describe('77.9 Color Parsing', () => {
      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.9.1: 十六进制颜色应正确解析
       */
      it('Property 77.9.1: Hex colors should be parsed correctly', () => {
        fc.assert(
          fc.property(
            fc.tuple(
              fc.integer({ min: 0, max: 255 }),
              fc.integer({ min: 0, max: 255 }),
              fc.integer({ min: 0, max: 255 })
            ),
            ([r, g, b]) => {
              const hex = `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`
              const parsed = parseColor(hex)
              
              return parsed.r === r && parsed.g === g && parsed.b === b
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.9.2: RGB颜色应正确解析
       */
      it('Property 77.9.2: RGB colors should be parsed correctly', () => {
        fc.assert(
          fc.property(
            fc.tuple(
              fc.integer({ min: 0, max: 255 }),
              fc.integer({ min: 0, max: 255 }),
              fc.integer({ min: 0, max: 255 })
            ),
            ([r, g, b]) => {
              const rgb = `rgb(${r}, ${g}, ${b})`
              const parsed = parseColor(rgb)
              
              return parsed.r === r && parsed.g === g && parsed.b === b
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.9.3: 短十六进制颜色应正确解析
       */
      it('Property 77.9.3: Short hex colors should be parsed correctly', () => {
        // #RGB -> #RRGGBB
        const testCases = [
          { short: '#fff', expected: { r: 255, g: 255, b: 255 } },
          { short: '#000', expected: { r: 0, g: 0, b: 0 } },
          { short: '#f00', expected: { r: 255, g: 0, b: 0 } },
          { short: '#0f0', expected: { r: 0, g: 255, b: 0 } },
          { short: '#00f', expected: { r: 0, g: 0, b: 255 } },
        ]
        
        for (const { short, expected } of testCases) {
          const parsed = parseColor(short)
          expect(parsed).toEqual(expected)
        }
      })

      /**
       * **Validates: Requirements 22.1**
       * 
       * 属性 77.9.4: 无效颜色应返回白色
       */
      it('Property 77.9.4: Invalid colors should return white', () => {
        const invalidColors = ['invalid', 'not-a-color', '123456', 'red']
        
        for (const color of invalidColors) {
          const parsed = parseColor(color)
          expect(parsed).toEqual({ r: 255, g: 255, b: 255 })
        }
      })
    })
  })
})

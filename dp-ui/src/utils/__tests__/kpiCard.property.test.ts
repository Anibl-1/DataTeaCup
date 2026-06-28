/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * KPI Card Utility Functions - Property-Based Tests
 * 
 * Feature: platform-optimization-plan
 * Property 9: KPI 卡片计算正确性
 * 
 * **Validates: Requirements 4.3**
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import {
  calculateTrend,
  formatKpiValue,
  buildSparklineOption,
  buildKpiCardConfig,
  getTrendColor,
  formatChangePercent,
  getKpiSizeConfig,
  type TrendDirection
} from '../kpiCard'

describe('Property 9: KPI 卡片计算正确性', () => {
  /**
   * Property 9: KPI_Card 计算的同比变化率 SHALL 等于 (current - previous) / previous × 100%
   * 
   * **Validates: Requirements 4.3**
   */
  it('should calculate YoY change rate correctly: (current - previous) / previous × 100%', () => {
    fc.assert(
      fc.property(
        fc.float({ min: Math.fround(-1e6), max: Math.fround(1e6), noNaN: true }),
        fc.float({ min: Math.fround(0.01), max: Math.fround(1e6), noNaN: true }), // previous > 0
        (current, previous) => {
          const result = buildKpiCardConfig(current, previous, null)
          
          // Expected YoY change rate
          const expectedYoy = ((current - previous) / Math.abs(previous)) * 100
          
          // Verify YoY calculation
          expect(result.yoyChange).not.toBeNull()
          expect(result.yoyChange).toBeCloseTo(expectedYoy, 3)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Property 9: KPI_Card 计算的环比变化率 SHALL 等于 (current - periodPrevious) / periodPrevious × 100%
   * 
   * **Validates: Requirements 4.3**
   */
  it('should calculate MoM change rate correctly: (current - periodPrevious) / periodPrevious × 100%', () => {
    fc.assert(
      fc.property(
        fc.float({ min: Math.fround(-1e6), max: Math.fround(1e6), noNaN: true }),
        fc.float({ min: Math.fround(0.01), max: Math.fround(1e6), noNaN: true }), // periodPrevious > 0
        (current, periodPrevious) => {
          const result = buildKpiCardConfig(current, null, periodPrevious)
          
          // Expected MoM change rate
          const expectedMom = ((current - periodPrevious) / Math.abs(periodPrevious)) * 100
          
          // Verify MoM calculation
          expect(result.momChange).not.toBeNull()
          expect(result.momChange).toBeCloseTo(expectedMom, 3)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Property: Both YoY and MoM should be calculated correctly when both previous values are provided
   * 
   * **Validates: Requirements 4.3**
   */
  it('should calculate both YoY and MoM correctly when both previous values are provided', () => {
    fc.assert(
      fc.property(
        fc.float({ min: Math.fround(-1e6), max: Math.fround(1e6), noNaN: true }),
        fc.float({ min: Math.fround(0.01), max: Math.fround(1e6), noNaN: true }), // previous > 0
        fc.float({ min: Math.fround(0.01), max: Math.fround(1e6), noNaN: true }), // periodPrevious > 0
        (current, previous, periodPrevious) => {
          const result = buildKpiCardConfig(current, previous, periodPrevious)
          
          // Expected change rates
          const expectedYoy = ((current - previous) / Math.abs(previous)) * 100
          const expectedMom = ((current - periodPrevious) / Math.abs(periodPrevious)) * 100
          
          // Verify both calculations
          expect(result.yoyChange).toBeCloseTo(expectedYoy, 3)
          expect(result.momChange).toBeCloseTo(expectedMom, 3)
        }
      ),
      { numRuns: 100 }
    )
  })
})

describe('calculateTrend properties', () => {
  /**
   * Property: Trend direction should be consistent with the sign of change
   */
  it('should have trend direction consistent with change sign', () => {
    fc.assert(
      fc.property(
        fc.float({ min: Math.fround(-1e6), max: Math.fround(1e6), noNaN: true }),
        fc.float({ min: Math.fround(-1e6), max: Math.fround(1e6), noNaN: true }),
        (current, previous) => {
          const result = calculateTrend(current, previous)
          
          if (previous === 0) {
            // Special case: previous is 0
            if (current === 0) {
              expect(result.direction).toBe('neutral')
            } else if (current > 0) {
              expect(result.direction).toBe('up')
            } else {
              expect(result.direction).toBe('down')
            }
          } else {
            const change = current - previous
            if (change > 0) {
              expect(result.direction).toBe('up')
            } else if (change < 0) {
              expect(result.direction).toBe('down')
            } else {
              expect(result.direction).toBe('neutral')
            }
          }
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Property: Change value should equal current - previous
   */
  it('should have change value equal to current - previous', () => {
    fc.assert(
      fc.property(
        fc.float({ min: Math.fround(-1e6), max: Math.fround(1e6), noNaN: true }),
        fc.float({ min: Math.fround(-1e6), max: Math.fround(1e6), noNaN: true }),
        (current, previous) => {
          const result = calculateTrend(current, previous)
          expect(result.change).toBeCloseTo(current - previous, 3)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Property: Percentage calculation should be correct for non-zero previous
   */
  it('should calculate percentage correctly for non-zero previous', () => {
    fc.assert(
      fc.property(
        fc.float({ min: Math.fround(-1e6), max: Math.fround(1e6), noNaN: true }),
        fc.float({ min: Math.fround(0.01), max: Math.fround(1e6), noNaN: true }), // Ensure previous > 0
        (current, previous) => {
          const result = calculateTrend(current, previous)
          const expectedPercentage = ((current - previous) / Math.abs(previous)) * 100
          expect(result.percentage).toBeCloseTo(expectedPercentage, 3)
        }
      ),
      { numRuns: 100 }
    )
  })
})

describe('formatKpiValue properties', () => {
  /**
   * Property: Formatted value should never be empty for valid numbers
   */
  it('should never return empty string for valid numbers', () => {
    fc.assert(
      fc.property(
        fc.float({ min: Math.fround(-1e6), max: Math.fround(1e6), noNaN: true }),
        fc.constantFrom('number', 'currency', 'percentage', 'compact') as fc.Arbitrary<'number' | 'currency' | 'percentage' | 'compact'>,
        (value, format) => {
          const result = formatKpiValue(value, format)
          expect(result.length).toBeGreaterThan(0)
          expect(result).not.toBe('')
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Property: Null/undefined/NaN should always return "--"
   */
  it('should return "--" for null, undefined, or NaN', () => {
    const invalidValues = [null, undefined, NaN]
    const formats: Array<'number' | 'currency' | 'percentage' | 'compact'> = ['number', 'currency', 'percentage', 'compact']
    
    for (const value of invalidValues) {
      for (const format of formats) {
        expect(formatKpiValue(value as any, format)).toBe('--')
      }
    }
  })

  /**
   * Property: Compact format should use correct suffix for magnitude
   */
  it('should use correct suffix for compact format based on magnitude', () => {
    // Test billions (use integer to avoid float precision issues)
    fc.assert(
      fc.property(
        fc.integer({ min: 1000000000, max: 2000000000 }),
        (value) => {
          const result = formatKpiValue(value, 'compact')
          expect(result).toContain('B')
        }
      ),
      { numRuns: 50 }
    )

    // Test millions
    fc.assert(
      fc.property(
        fc.integer({ min: 1000000, max: 999999999 }),
        (value) => {
          const result = formatKpiValue(value, 'compact')
          expect(result).toContain('M')
        }
      ),
      { numRuns: 50 }
    )

    // Test thousands
    fc.assert(
      fc.property(
        fc.integer({ min: 1000, max: 999999 }),
        (value) => {
          const result = formatKpiValue(value, 'compact')
          expect(result).toContain('K')
        }
      ),
      { numRuns: 50 }
    )
  })

  /**
   * Property: Currency format should include currency symbol
   */
  it('should include currency symbol in currency format', () => {
    fc.assert(
      fc.property(
        fc.float({ min: Math.fround(-1e6), max: Math.fround(1e6), noNaN: true }),
        fc.string({ minLength: 1, maxLength: 3 }),
        (value, symbol) => {
          const result = formatKpiValue(value, 'currency', { currencySymbol: symbol })
          expect(result).toContain(symbol)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Property: Percentage format should end with %
   */
  it('should end with % in percentage format', () => {
    fc.assert(
      fc.property(
        fc.float({ min: Math.fround(-1000), max: Math.fround(1000), noNaN: true }),
        (value) => {
          const result = formatKpiValue(value, 'percentage')
          expect(result).toMatch(/%$/)
        }
      ),
      { numRuns: 100 }
    )
  })
})

describe('buildSparklineOption properties', () => {
  /**
   * Property: Sparkline data should be preserved in output
   */
  it('should preserve all input data in sparkline series', () => {
    fc.assert(
      fc.property(
        fc.array(fc.float({ min: Math.fround(-1000), max: Math.fround(1000), noNaN: true }), { minLength: 1, maxLength: 100 }),
        (data) => {
          const option = buildSparklineOption(data)
          const series = option.series as any[]
          
          if (series.length > 0) {
            expect(series[0].data).toEqual(data)
          }
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Property: Empty data should result in empty series
   */
  it('should return empty series for empty data', () => {
    const option = buildSparklineOption([])
    expect(option.series).toEqual([])
  })

  /**
   * Property: Axes should always be hidden
   */
  it('should always hide axes', () => {
    fc.assert(
      fc.property(
        fc.array(fc.float({ min: Math.fround(-1000), max: Math.fround(1000), noNaN: true }), { minLength: 1, maxLength: 50 }),
        (data) => {
          const option = buildSparklineOption(data)
          expect((option.xAxis as any).show).toBe(false)
          expect((option.yAxis as any).show).toBe(false)
        }
      ),
      { numRuns: 50 }
    )
  })

  /**
   * Property: Y-axis range should encompass all data points
   */
  it('should have Y-axis range that encompasses all data points', () => {
    fc.assert(
      fc.property(
        fc.array(fc.float({ min: Math.fround(-1000), max: Math.fround(1000), noNaN: true }), { minLength: 2, maxLength: 50 }),
        (data) => {
          const option = buildSparklineOption(data)
          const yAxis = option.yAxis as any
          const minData = Math.min(...data)
          const maxData = Math.max(...data)
          
          expect(yAxis.min).toBeLessThanOrEqual(minData)
          expect(yAxis.max).toBeGreaterThanOrEqual(maxData)
        }
      ),
      { numRuns: 50 }
    )
  })
})

describe('getTrendColor properties', () => {
  /**
   * Property: Neutral trend should always return gray
   */
  it('should always return gray for neutral trend', () => {
    fc.assert(
      fc.property(
        fc.boolean(),
        (positiveIsGood) => {
          expect(getTrendColor('neutral', positiveIsGood)).toBe('#999999')
        }
      ),
      { numRuns: 10 }
    )
  })

  /**
   * Property: Color should be consistent with positiveIsGood setting
   */
  it('should return correct color based on positiveIsGood setting', () => {
    // When positiveIsGood is true: up = green, down = red
    expect(getTrendColor('up', true)).toBe('#52c41a')
    expect(getTrendColor('down', true)).toBe('#ff4d4f')
    
    // When positiveIsGood is false: up = red, down = green
    expect(getTrendColor('up', false)).toBe('#ff4d4f')
    expect(getTrendColor('down', false)).toBe('#52c41a')
  })
})

describe('formatChangePercent properties', () => {
  /**
   * Property: Positive values should have + prefix
   */
  it('should have + prefix for positive values', () => {
    fc.assert(
      fc.property(
        fc.float({ min: Math.fround(0.01), max: Math.fround(1000), noNaN: true }),
        fc.integer({ min: 0, max: 5 }),
        (value, decimals) => {
          const result = formatChangePercent(value, decimals)
          expect(result.startsWith('+')).toBe(true)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Property: Negative values should have - prefix
   */
  it('should have - prefix for negative values', () => {
    fc.assert(
      fc.property(
        fc.float({ min: Math.fround(-1000), max: Math.fround(-0.01), noNaN: true }),
        fc.integer({ min: 0, max: 5 }),
        (value, decimals) => {
          const result = formatChangePercent(value, decimals)
          expect(result.startsWith('-')).toBe(true)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Property: Result should always end with %
   */
  it('should always end with % for valid numbers', () => {
    fc.assert(
      fc.property(
        fc.float({ min: Math.fround(-1000), max: Math.fround(1000), noNaN: true }),
        fc.integer({ min: 0, max: 5 }),
        (value, decimals) => {
          const result = formatChangePercent(value, decimals)
          expect(result).toMatch(/%$/)
        }
      ),
      { numRuns: 100 }
    )
  })
})

describe('getKpiSizeConfig properties', () => {
  /**
   * Property: All size configs should have positive values
   */
  it('should have all positive values in size config', () => {
    const sizes: Array<'small' | 'medium' | 'large'> = ['small', 'medium', 'large']
    
    for (const size of sizes) {
      const config = getKpiSizeConfig(size)
      expect(config.valueFontSize).toBeGreaterThan(0)
      expect(config.titleFontSize).toBeGreaterThan(0)
      expect(config.trendFontSize).toBeGreaterThan(0)
      expect(config.padding).toBeGreaterThan(0)
      expect(config.iconSize).toBeGreaterThan(0)
    }
  })

  /**
   * Property: Larger sizes should have larger font sizes
   */
  it('should have larger font sizes for larger size configs', () => {
    const small = getKpiSizeConfig('small')
    const medium = getKpiSizeConfig('medium')
    const large = getKpiSizeConfig('large')
    
    expect(small.valueFontSize).toBeLessThan(medium.valueFontSize)
    expect(medium.valueFontSize).toBeLessThan(large.valueFontSize)
    
    expect(small.titleFontSize).toBeLessThan(medium.titleFontSize)
    expect(medium.titleFontSize).toBeLessThan(large.titleFontSize)
  })
})

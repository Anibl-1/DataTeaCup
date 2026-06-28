/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * KPI Card Utility Functions - Unit Tests
 */
import { describe, it, expect } from 'vitest'
import {
  calculateTrend,
  formatKpiValue,
  buildSparklineOption,
  buildKpiCardConfig,
  getTrendColor,
  formatChangePercent,
  getKpiSizeConfig,
  type TrendDirection,
  type ValueFormat
} from '../kpiCard'

describe('calculateTrend', () => {
  it('should return "up" when current > previous', () => {
    const result = calculateTrend(150, 100)
    expect(result.direction).toBe('up')
    expect(result.percentage).toBe(50)
    expect(result.change).toBe(50)
  })

  it('should return "down" when current < previous', () => {
    const result = calculateTrend(80, 100)
    expect(result.direction).toBe('down')
    expect(result.percentage).toBe(-20)
    expect(result.change).toBe(-20)
  })

  it('should return "neutral" when current === previous', () => {
    const result = calculateTrend(100, 100)
    expect(result.direction).toBe('neutral')
    expect(result.percentage).toBe(0)
    expect(result.change).toBe(0)
  })

  it('should handle previous = 0 with positive current', () => {
    const result = calculateTrend(100, 0)
    expect(result.direction).toBe('up')
    expect(result.percentage).toBe(100)
    expect(result.change).toBe(100)
  })

  it('should handle previous = 0 with negative current', () => {
    const result = calculateTrend(-50, 0)
    expect(result.direction).toBe('down')
    expect(result.percentage).toBe(-100)
    expect(result.change).toBe(-50)
  })

  it('should handle both values = 0', () => {
    const result = calculateTrend(0, 0)
    expect(result.direction).toBe('neutral')
    expect(result.percentage).toBe(0)
    expect(result.change).toBe(0)
  })

  it('should handle negative previous value', () => {
    const result = calculateTrend(-50, -100)
    expect(result.direction).toBe('up')
    expect(result.percentage).toBe(50)
    expect(result.change).toBe(50)
  })
})

describe('formatKpiValue', () => {
  describe('number format', () => {
    it('should format integer with thousand separators', () => {
      const result = formatKpiValue(1234567, 'number')
      expect(result).toContain('1')
      expect(result).toContain('234')
      expect(result).toContain('567')
    })

    it('should format decimal with appropriate precision', () => {
      const result = formatKpiValue(1234.567, 'number', { decimals: 2 })
      expect(result).toContain('1')
      expect(result).toContain('234')
    })

    it('should return "--" for null value', () => {
      expect(formatKpiValue(null, 'number')).toBe('--')
    })

    it('should return "--" for undefined value', () => {
      expect(formatKpiValue(undefined, 'number')).toBe('--')
    })

    it('should return "--" for NaN value', () => {
      expect(formatKpiValue(NaN, 'number')).toBe('--')
    })
  })

  describe('currency format', () => {
    it('should format with default currency symbol', () => {
      const result = formatKpiValue(1234.56, 'currency')
      expect(result).toContain('¥')
      expect(result).toContain('1')
    })

    it('should format with custom currency symbol', () => {
      const result = formatKpiValue(1234.56, 'currency', { currencySymbol: '$' })
      expect(result).toContain('$')
    })
  })

  describe('percentage format', () => {
    it('should format as percentage', () => {
      const result = formatKpiValue(75.5, 'percentage', { decimals: 1 })
      expect(result).toBe('75.5%')
    })
  })

  describe('compact format', () => {
    it('should format billions', () => {
      const result = formatKpiValue(1500000000, 'compact', { decimals: 1 })
      expect(result).toBe('1.5B')
    })

    it('should format millions', () => {
      const result = formatKpiValue(2500000, 'compact', { decimals: 1 })
      expect(result).toBe('2.5M')
    })

    it('should format thousands', () => {
      const result = formatKpiValue(3500, 'compact', { decimals: 1 })
      expect(result).toBe('3.5K')
    })

    it('should not compact small numbers', () => {
      const result = formatKpiValue(500, 'compact', { decimals: 0 })
      expect(result).toBe('500')
    })

    it('should handle negative values', () => {
      const result = formatKpiValue(-2500000, 'compact', { decimals: 1 })
      expect(result).toBe('-2.5M')
    })
  })

  describe('with prefix/suffix/unit', () => {
    it('should add prefix', () => {
      const result = formatKpiValue(100, 'number', { prefix: '约 ' })
      expect(result.startsWith('约 ')).toBe(true)
    })

    it('should add unit', () => {
      const result = formatKpiValue(100, 'number', { unit: '元' })
      expect(result.endsWith('元')).toBe(true)
    })

    it('should add suffix', () => {
      const result = formatKpiValue(100, 'number', { suffix: ' (预估)' })
      expect(result.endsWith(' (预估)')).toBe(true)
    })
  })
})

describe('buildSparklineOption', () => {
  it('should return empty series for empty data', () => {
    const option = buildSparklineOption([])
    expect(option.series).toEqual([])
  })

  it('should build line sparkline by default', () => {
    const option = buildSparklineOption([10, 20, 30, 40, 50])
    expect(option.series).toBeDefined()
    expect(Array.isArray(option.series)).toBe(true)
    const series = option.series as any[]
    expect(series.length).toBe(1)
    expect(series[0].type).toBe('line')
    expect(series[0].data).toEqual([10, 20, 30, 40, 50])
  })

  it('should build bar sparkline when specified', () => {
    const option = buildSparklineOption([10, 20, 30], { type: 'bar' })
    const series = option.series as any[]
    expect(series[0].type).toBe('bar')
  })

  it('should apply custom color', () => {
    const option = buildSparklineOption([10, 20, 30], { color: '#ff0000' })
    const series = option.series as any[]
    expect(series[0].lineStyle.color).toBe('#ff0000')
  })

  it('should apply smooth option', () => {
    const option = buildSparklineOption([10, 20, 30], { smooth: false })
    const series = option.series as any[]
    expect(series[0].smooth).toBe(false)
  })

  it('should hide axes', () => {
    const option = buildSparklineOption([10, 20, 30])
    expect((option.xAxis as any).show).toBe(false)
    expect((option.yAxis as any).show).toBe(false)
  })

  it('should add area fill when areaColor is specified', () => {
    const option = buildSparklineOption([10, 20, 30], { areaColor: 'rgba(0,0,255,0.3)' })
    const series = option.series as any[]
    expect(series[0].areaStyle).toBeDefined()
  })
})

describe('buildKpiCardConfig', () => {
  it('should calculate YoY change correctly', () => {
    const result = buildKpiCardConfig(120, 100, null)
    expect(result.yoyChange).toBe(20)
    expect(result.trend).toBe('up')
    expect(result.trendPercent).toBe(20)
  })

  it('should calculate MoM change correctly', () => {
    const result = buildKpiCardConfig(90, null, 100)
    expect(result.momChange).toBe(-10)
    expect(result.trend).toBe('down')
    expect(result.trendPercent).toBe(-10)
  })

  it('should calculate both YoY and MoM', () => {
    const result = buildKpiCardConfig(150, 100, 120)
    expect(result.yoyChange).toBe(50)
    expect(result.momChange).toBe(25)
    // Trend should be based on YoY (previous)
    expect(result.trend).toBe('up')
    expect(result.trendPercent).toBe(50)
  })

  it('should handle null previous values', () => {
    const result = buildKpiCardConfig(100, null, null)
    expect(result.yoyChange).toBeNull()
    expect(result.momChange).toBeNull()
    expect(result.trend).toBe('neutral')
    expect(result.trendPercent).toBeNull()
  })

  it('should handle zero previous value', () => {
    const result = buildKpiCardConfig(100, 0, null)
    expect(result.yoyChange).toBeNull()
  })

  it('should format value according to config', () => {
    const result = buildKpiCardConfig(1234567, null, null, { format: 'compact', decimals: 1 })
    expect(result.formattedValue).toBe('1.2M')
  })
})

describe('getTrendColor', () => {
  it('should return green for up trend when positive is good', () => {
    expect(getTrendColor('up', true)).toBe('#52c41a')
  })

  it('should return red for down trend when positive is good', () => {
    expect(getTrendColor('down', true)).toBe('#ff4d4f')
  })

  it('should return red for up trend when positive is bad', () => {
    expect(getTrendColor('up', false)).toBe('#ff4d4f')
  })

  it('should return green for down trend when positive is bad', () => {
    expect(getTrendColor('down', false)).toBe('#52c41a')
  })

  it('should return gray for neutral trend', () => {
    expect(getTrendColor('neutral', true)).toBe('#999999')
    expect(getTrendColor('neutral', false)).toBe('#999999')
  })
})

describe('formatChangePercent', () => {
  it('should format positive change with + sign', () => {
    expect(formatChangePercent(25.5, 1)).toBe('+25.5%')
  })

  it('should format negative change with - sign', () => {
    expect(formatChangePercent(-15.3, 1)).toBe('-15.3%')
  })

  it('should format zero change with + sign', () => {
    expect(formatChangePercent(0, 1)).toBe('+0.0%')
  })

  it('should return "--" for null', () => {
    expect(formatChangePercent(null)).toBe('--')
  })

  it('should return "--" for NaN', () => {
    expect(formatChangePercent(NaN)).toBe('--')
  })

  it('should respect decimal places', () => {
    expect(formatChangePercent(12.3456, 2)).toBe('+12.35%')
    expect(formatChangePercent(12.3456, 0)).toBe('+12%')
  })
})

describe('getKpiSizeConfig', () => {
  it('should return small config', () => {
    const config = getKpiSizeConfig('small')
    expect(config.valueFontSize).toBe(24)
    expect(config.titleFontSize).toBe(12)
    expect(config.padding).toBe(12)
  })

  it('should return medium config by default', () => {
    const config = getKpiSizeConfig()
    expect(config.valueFontSize).toBe(36)
    expect(config.titleFontSize).toBe(14)
    expect(config.padding).toBe(16)
  })

  it('should return large config', () => {
    const config = getKpiSizeConfig('large')
    expect(config.valueFontSize).toBe(48)
    expect(config.titleFontSize).toBe(16)
    expect(config.padding).toBe(24)
  })
})

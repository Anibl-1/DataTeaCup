/**
 * Data Formatter Unit Tests
 * 数据格式化器单元测试
 * 
 * Validates: 14.3.11, 14.3.12, 14.3.13, 14.3.14, 14.3.15
 */

import { describe, it, expect } from 'vitest'
import {
  formatNumber,
  formatDate,
  formatText,
  formatValue,
  isEmpty,
  getEmptyDisplayText,
  isParenthesesFormat,
  isRedFormat,
  getDatePresets,
  getDatePresetPattern,
  NumberFormatters,
  DateFormatters,
  TextFormatters,
  CURRENCY_CONFIG,
} from '../dataFormatter'
import type { DataFormat } from '@/components/StyleEngine/types/style'

describe('Data Formatter', () => {
  // ==========================================================================
  // Number Formatting Tests (Requirement 14.3.11)
  // ==========================================================================
  describe('formatNumber', () => {
    describe('basic formatting', () => {
      it('should format integer with thousands separator', () => {
        expect(formatNumber(1234567)).toBe('1,234,567')
      })

      it('should format decimal numbers', () => {
        expect(formatNumber(1234.5678, { decimalPlaces: 2 })).toBe('1,234.57')
      })

      it('should handle zero', () => {
        expect(formatNumber(0)).toBe('0')
      })

      it('should handle negative numbers with minus sign', () => {
        expect(formatNumber(-1234)).toBe('-1,234')
      })

      it('should return empty string for null/undefined/NaN', () => {
        expect(formatNumber(null)).toBe('')
        expect(formatNumber(undefined)).toBe('')
        expect(formatNumber(NaN)).toBe('')
      })
    })

    describe('thousands separator', () => {
      it('should format with thousands separator by default', () => {
        expect(formatNumber(1000000)).toBe('1,000,000')
      })

      it('should format without thousands separator when disabled', () => {
        expect(formatNumber(1000000, { useThousandsSeparator: false })).toBe('1000000')
      })
    })

    describe('decimal places', () => {
      it('should format with specified decimal places', () => {
        expect(formatNumber(123.456, { decimalPlaces: 2 })).toBe('123.46')
        expect(formatNumber(123.4, { decimalPlaces: 3 })).toBe('123.400')
        expect(formatNumber(123, { decimalPlaces: 2 })).toBe('123.00')
      })

      it('should round correctly', () => {
        expect(formatNumber(123.455, { decimalPlaces: 2 })).toBe('123.46')
        expect(formatNumber(123.444, { decimalPlaces: 2 })).toBe('123.44')
      })
    })

    describe('negative formats', () => {
      it('should format negative with minus sign (default)', () => {
        expect(formatNumber(-1234, { negativeFormat: 'minus' })).toBe('-1,234')
      })

      it('should format negative with parentheses', () => {
        expect(formatNumber(-1234, { negativeFormat: 'parentheses' })).toBe('(1,234)')
      })

      it('should format negative with red (returns minus)', () => {
        expect(formatNumber(-1234, { negativeFormat: 'red' })).toBe('-1,234')
      })

      it('should format negative with red parentheses', () => {
        expect(formatNumber(-1234, { negativeFormat: 'redParentheses' })).toBe('(1,234)')
      })
    })

    describe('prefix and suffix', () => {
      it('should add prefix', () => {
        expect(formatNumber(100, { prefix: '约 ' })).toBe('约 100')
      })

      it('should add suffix', () => {
        expect(formatNumber(100, { suffix: ' 元' })).toBe('100 元')
      })

      it('should add both prefix and suffix', () => {
        expect(formatNumber(100, { prefix: '约 ', suffix: ' 元' })).toBe('约 100 元')
      })

      it('should handle prefix/suffix with negative parentheses', () => {
        expect(formatNumber(-100, { prefix: '¥', negativeFormat: 'parentheses' })).toBe('(¥100)')
      })
    })

    describe('percentage', () => {
      it('should format as percentage', () => {
        expect(formatNumber(0.5, { asPercentage: true })).toBe('50.0%')
        expect(formatNumber(0.1234, { asPercentage: true, decimalPlaces: 2 })).toBe('12.34%')
      })

      it('should handle percentage with negative values', () => {
        expect(formatNumber(-0.25, { asPercentage: true })).toBe('-25.0%')
      })
    })

    describe('currency', () => {
      it('should format as CNY currency', () => {
        expect(formatNumber(1234.56, { asCurrency: true, currencyCode: 'CNY', decimalPlaces: 2 })).toBe('¥1,234.56')
      })

      it('should format as USD currency', () => {
        expect(formatNumber(1234.56, { asCurrency: true, currencyCode: 'USD', decimalPlaces: 2 })).toBe('$1,234.56')
      })

      it('should format as EUR currency', () => {
        expect(formatNumber(1234.56, { asCurrency: true, currencyCode: 'EUR', decimalPlaces: 2 })).toBe('€1,234.56')
      })

      it('should handle negative currency with parentheses', () => {
        expect(formatNumber(-1234.56, { 
          asCurrency: true, 
          currencyCode: 'USD', 
          decimalPlaces: 2,
          negativeFormat: 'parentheses'
        })).toBe('($1,234.56)')
      })
    })

    describe('abbreviation', () => {
      it('should abbreviate thousands', () => {
        expect(formatNumber(12345, { abbreviate: true })).toBe('12.3K')
      })

      it('should abbreviate millions', () => {
        expect(formatNumber(1234567, { abbreviate: true })).toBe('1.2M')
      })

      it('should abbreviate billions', () => {
        expect(formatNumber(1234567890, { abbreviate: true })).toBe('1.2B')
      })

      it('should abbreviate trillions', () => {
        expect(formatNumber(1234567890123, { abbreviate: true })).toBe('1.2T')
      })

      it('should not abbreviate below threshold', () => {
        expect(formatNumber(9999, { abbreviate: true, abbreviateThreshold: 10000 })).toBe('9,999')
      })

      it('should respect custom threshold', () => {
        expect(formatNumber(500, { abbreviate: true, abbreviateThreshold: 100 })).toBe('500')
        expect(formatNumber(1500, { abbreviate: true, abbreviateThreshold: 1000 })).toBe('1.5K')
      })
    })
  })

  // ==========================================================================
  // Date Formatting Tests (Requirement 14.3.12)
  // ==========================================================================
  describe('formatDate', () => {
    const testDate = '2024-03-15T14:30:00'
    const testTimestamp = 1710509400000 // 2024-03-15T14:30:00 UTC

    describe('basic formatting', () => {
      it('should format date string', () => {
        const result = formatDate(testDate)
        expect(result).toContain('2024')
        expect(result).toContain('3')
        expect(result).toContain('15')
      })

      it('should format Date object', () => {
        const result = formatDate(new Date(testDate))
        expect(result).toContain('2024')
      })

      it('should format timestamp (milliseconds)', () => {
        const result = formatDate(testTimestamp)
        expect(result).toContain('2024')
      })

      it('should format timestamp (seconds)', () => {
        const result = formatDate(testTimestamp / 1000)
        expect(result).toContain('2024')
      })

      it('should return empty string for null/undefined/empty', () => {
        expect(formatDate(null)).toBe('')
        expect(formatDate(undefined)).toBe('')
        expect(formatDate('')).toBe('')
      })

      it('should return empty string for invalid date', () => {
        expect(formatDate('invalid-date')).toBe('')
      })
    })

    describe('presets', () => {
      it('should format with short preset', () => {
        const result = formatDate(testDate, { preset: 'short', locale: 'zh-CN' })
        expect(result).toMatch(/2024\/3\/15/)
      })

      it('should format with medium preset', () => {
        const result = formatDate(testDate, { preset: 'medium', locale: 'zh-CN' })
        expect(result).toContain('年')
        expect(result).toContain('月')
        expect(result).toContain('日')
      })

      it('should format with long preset', () => {
        const result = formatDate(testDate, { preset: 'long', locale: 'zh-CN' })
        expect(result).toContain('年')
        expect(result).toContain(':')
      })

      it('should format with full preset', () => {
        const result = formatDate(testDate, { preset: 'full', locale: 'zh-CN' })
        expect(result).toContain('年')
        expect(result).toContain(':')
      })
    })

    describe('custom pattern', () => {
      it('should format with custom pattern', () => {
        expect(formatDate(testDate, { pattern: 'YYYY-MM-DD' })).toBe('2024-03-15')
      })

      it('should format with time pattern', () => {
        const result = formatDate(testDate, { pattern: 'YYYY-MM-DD HH:mm:ss' })
        expect(result).toMatch(/2024-03-15 \d{2}:\d{2}:\d{2}/)
      })

      it('should format with custom separators', () => {
        expect(formatDate(testDate, { pattern: 'YYYY/MM/DD' })).toBe('2024/03/15')
      })
    })

    describe('locale', () => {
      it('should format with zh-CN locale', () => {
        const result = formatDate(testDate, { preset: 'medium', locale: 'zh-CN' })
        expect(result).toContain('年')
      })

      it('should format with en-US locale', () => {
        const result = formatDate(testDate, { preset: 'medium', locale: 'en-US' })
        expect(result).toContain('Mar')
      })
    })
  })

  // ==========================================================================
  // Text Formatting Tests (Requirements 14.3.13, 14.3.14, 14.3.15)
  // ==========================================================================
  describe('formatText', () => {
    describe('case transformation (14.3.13)', () => {
      it('should convert to uppercase', () => {
        const result = formatText('hello world', { case: 'upper' })
        expect(result.formatted).toBe('HELLO WORLD')
      })

      it('should convert to lowercase', () => {
        const result = formatText('HELLO WORLD', { case: 'lower' })
        expect(result.formatted).toBe('hello world')
      })

      it('should capitalize first letter', () => {
        const result = formatText('hello world', { case: 'capitalize' })
        expect(result.formatted).toBe('Hello world')
      })

      it('should convert to title case', () => {
        const result = formatText('hello world test', { case: 'titleCase' })
        expect(result.formatted).toBe('Hello World Test')
      })

      it('should not transform with none', () => {
        const result = formatText('Hello World', { case: 'none' })
        expect(result.formatted).toBe('Hello World')
      })
    })

    describe('prefix and suffix (14.3.13)', () => {
      it('should add prefix', () => {
        const result = formatText('test', { prefix: '【' })
        expect(result.formatted).toBe('【test')
      })

      it('should add suffix', () => {
        const result = formatText('test', { suffix: '】' })
        expect(result.formatted).toBe('test】')
      })

      it('should add both prefix and suffix', () => {
        const result = formatText('test', { prefix: '【', suffix: '】' })
        expect(result.formatted).toBe('【test】')
      })
    })

    describe('empty value handling (14.3.14)', () => {
      it('should return emptyText for null', () => {
        const result = formatText(null, { emptyText: '-' })
        expect(result.formatted).toBe('-')
      })

      it('should return emptyText for undefined', () => {
        const result = formatText(undefined, { emptyText: 'N/A' })
        expect(result.formatted).toBe('N/A')
      })

      it('should return emptyText for empty string', () => {
        const result = formatText('', { emptyText: '无数据' })
        expect(result.formatted).toBe('无数据')
      })

      it('should use default emptyText', () => {
        const result = formatText(null)
        expect(result.formatted).toBe('-')
      })
    })

    describe('overflow handling (14.3.15)', () => {
      const longText = 'This is a very long text that should be truncated'

      it('should truncate text', () => {
        const result = formatText(longText, { maxLength: 20, overflow: 'truncate' })
        expect(result.formatted).toBe('This is a very long ')
        expect(result.truncated).toBe(true)
      })

      it('should truncate with ellipsis', () => {
        const result = formatText(longText, { maxLength: 20, overflow: 'ellipsis' })
        // maxLength 20 - 3 for ellipsis = 17 chars + '...'
        expect(result.formatted).toBe('This is a very lo...')
        expect(result.formatted.length).toBe(20)
        expect(result.truncated).toBe(true)
      })

      it('should not truncate with wrap', () => {
        const result = formatText(longText, { maxLength: 20, overflow: 'wrap' })
        expect(result.formatted).toBe(longText)
        expect(result.truncated).toBe(false)
      })

      it('should truncate with tooltip (same as ellipsis)', () => {
        const result = formatText(longText, { maxLength: 20, overflow: 'tooltip' })
        // maxLength 20 - 3 for ellipsis = 17 chars + '...'
        expect(result.formatted).toBe('This is a very lo...')
        expect(result.formatted.length).toBe(20)
        expect(result.truncated).toBe(true)
        expect(result.fullText).toBe(longText)
      })

      it('should not truncate short text', () => {
        const result = formatText('short', { maxLength: 20, overflow: 'ellipsis' })
        expect(result.formatted).toBe('short')
        expect(result.truncated).toBe(false)
      })

      it('should provide original length', () => {
        const result = formatText(longText, { maxLength: 20, overflow: 'ellipsis' })
        expect(result.originalLength).toBe(longText.length)
      })
    })
  })

  // ==========================================================================
  // Unified Format Value Tests
  // ==========================================================================
  describe('formatValue', () => {
    it('should format number type', () => {
      const format: DataFormat = {
        type: 'number',
        config: { decimalPlaces: 2, useThousandsSeparator: true }
      }
      expect(formatValue(1234.567, format)).toBe('1,234.57')
    })

    it('should format date type', () => {
      const format: DataFormat = {
        type: 'date',
        config: { pattern: 'YYYY-MM-DD' }
      }
      expect(formatValue('2024-03-15', format)).toBe('2024-03-15')
    })

    it('should format text type', () => {
      const format: DataFormat = {
        type: 'text',
        config: { case: 'upper' }
      }
      expect(formatValue('hello', format)).toBe('HELLO')
    })

    it('should handle null without format', () => {
      expect(formatValue(null)).toBe('-')
    })

    it('should handle number without format', () => {
      expect(formatValue(1234)).toBe('1,234')
    })

    it('should handle Date without format', () => {
      const result = formatValue(new Date('2024-03-15'))
      expect(result).toContain('2024')
    })
  })

  // ==========================================================================
  // Empty Value Utilities Tests
  // ==========================================================================
  describe('isEmpty', () => {
    it('should return true for null', () => {
      expect(isEmpty(null)).toBe(true)
    })

    it('should return true for undefined', () => {
      expect(isEmpty(undefined)).toBe(true)
    })

    it('should return true for empty string', () => {
      expect(isEmpty('')).toBe(true)
    })

    it('should return true for whitespace string', () => {
      expect(isEmpty('   ')).toBe(true)
    })

    it('should return true for NaN', () => {
      expect(isEmpty(NaN)).toBe(true)
    })

    it('should return false for zero', () => {
      expect(isEmpty(0)).toBe(false)
    })

    it('should return false for non-empty string', () => {
      expect(isEmpty('test')).toBe(false)
    })

    it('should return false for false boolean', () => {
      expect(isEmpty(false)).toBe(false)
    })
  })

  describe('getEmptyDisplayText', () => {
    it('should return empty string for empty type', () => {
      expect(getEmptyDisplayText('empty')).toBe('')
    })

    it('should return custom text for text type', () => {
      expect(getEmptyDisplayText('text', 'N/A')).toBe('N/A')
    })

    it('should return dash for dash type', () => {
      expect(getEmptyDisplayText('dash')).toBe('-')
    })

    it('should return custom text for custom type', () => {
      expect(getEmptyDisplayText('custom', '无数据')).toBe('无数据')
    })
  })

  // ==========================================================================
  // Helper Functions Tests
  // ==========================================================================
  describe('helper functions', () => {
    describe('isParenthesesFormat', () => {
      it('should return true for parentheses', () => {
        expect(isParenthesesFormat('parentheses')).toBe(true)
      })

      it('should return true for redParentheses', () => {
        expect(isParenthesesFormat('redParentheses')).toBe(true)
      })

      it('should return false for minus', () => {
        expect(isParenthesesFormat('minus')).toBe(false)
      })

      it('should return false for red', () => {
        expect(isParenthesesFormat('red')).toBe(false)
      })
    })

    describe('isRedFormat', () => {
      it('should return true for red', () => {
        expect(isRedFormat('red')).toBe(true)
      })

      it('should return true for redParentheses', () => {
        expect(isRedFormat('redParentheses')).toBe(true)
      })

      it('should return false for minus', () => {
        expect(isRedFormat('minus')).toBe(false)
      })

      it('should return false for parentheses', () => {
        expect(isRedFormat('parentheses')).toBe(false)
      })
    })

    describe('getDatePresets', () => {
      it('should return array of preset names', () => {
        const presets = getDatePresets()
        expect(presets).toContain('short')
        expect(presets).toContain('medium')
        expect(presets).toContain('long')
        expect(presets).toContain('full')
      })
    })

    describe('getDatePresetPattern', () => {
      it('should return pattern for preset and locale', () => {
        expect(getDatePresetPattern('short', 'zh-CN')).toBe('YYYY/M/D')
        expect(getDatePresetPattern('short', 'en-US')).toBe('M/D/YYYY')
      })

      it('should return default pattern for unknown preset', () => {
        expect(getDatePresetPattern('unknown')).toBe('YYYY-MM-DD')
      })
    })
  })

  // ==========================================================================
  // Preset Formatters Tests
  // ==========================================================================
  describe('preset formatters', () => {
    describe('NumberFormatters', () => {
      it('integer should format without decimals', () => {
        expect(NumberFormatters.integer(1234567)).toBe('1,234,567')
      })

      it('decimal2 should format with 2 decimals', () => {
        expect(NumberFormatters.decimal2(1234.5)).toBe('1,234.50')
      })

      it('percent should format as percentage', () => {
        expect(NumberFormatters.percent(0.1234)).toBe('12.3%')
      })

      it('currencyCNY should format as CNY', () => {
        expect(NumberFormatters.currencyCNY(1234.56)).toBe('¥1,234.56')
      })

      it('currencyUSD should format as USD', () => {
        expect(NumberFormatters.currencyUSD(1234.56)).toBe('$1,234.56')
      })

      it('abbreviated should abbreviate large numbers', () => {
        expect(NumberFormatters.abbreviated(1234567)).toBe('1.2M')
      })

      it('accounting should use parentheses for negative', () => {
        expect(NumberFormatters.accounting(-1234.56)).toBe('(1,234.56)')
      })
    })

    describe('DateFormatters', () => {
      const testDate = '2024-03-15'

      it('short should format with short preset', () => {
        const result = DateFormatters.short(testDate)
        expect(result).toContain('2024')
      })

      it('iso should format as ISO date', () => {
        expect(DateFormatters.iso(testDate)).toBe('2024-03-15')
      })
    })

    describe('TextFormatters', () => {
      it('upper should convert to uppercase', () => {
        expect(TextFormatters.upper('hello').formatted).toBe('HELLO')
      })

      it('lower should convert to lowercase', () => {
        expect(TextFormatters.lower('HELLO').formatted).toBe('hello')
      })

      it('capitalize should capitalize first letter', () => {
        expect(TextFormatters.capitalize('hello').formatted).toBe('Hello')
      })

      it('titleCase should convert to title case', () => {
        expect(TextFormatters.titleCase('hello world').formatted).toBe('Hello World')
      })

      it('truncate50 should truncate at 50 chars', () => {
        const longText = 'a'.repeat(60)
        const result = TextFormatters.truncate50(longText)
        expect(result.truncated).toBe(true)
        expect(result.formatted.length).toBeLessThanOrEqual(50)
      })
    })
  })

  // ==========================================================================
  // Currency Config Tests
  // ==========================================================================
  describe('CURRENCY_CONFIG', () => {
    it('should have CNY configuration', () => {
      expect(CURRENCY_CONFIG.CNY).toBeDefined()
      expect(CURRENCY_CONFIG.CNY.symbol).toBe('¥')
      expect(CURRENCY_CONFIG.CNY.position).toBe('prefix')
    })

    it('should have USD configuration', () => {
      expect(CURRENCY_CONFIG.USD).toBeDefined()
      expect(CURRENCY_CONFIG.USD.symbol).toBe('$')
    })

    it('should have EUR configuration', () => {
      expect(CURRENCY_CONFIG.EUR).toBeDefined()
      expect(CURRENCY_CONFIG.EUR.symbol).toBe('€')
    })

    it('should have RUB with suffix position', () => {
      expect(CURRENCY_CONFIG.RUB).toBeDefined()
      expect(CURRENCY_CONFIG.RUB.position).toBe('suffix')
    })
  })
})

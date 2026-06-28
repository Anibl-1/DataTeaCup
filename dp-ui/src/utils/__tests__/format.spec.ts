/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, it, expect } from 'vitest'
import {
  formatDateTime,
  formatDate,
  formatNumber,
  formatFileSize,
  formatTime,
  formatCellValueSmart,
  formatTableCellValue
} from '../format'

// ==================== formatDateTime ====================

describe('formatDateTime', () => {
  it('should format a date string with default format', () => {
    const result = formatDateTime('2024-01-15T10:30:00')
    expect(result).toContain('2024')
    expect(result).toContain('10:30:00')
  })

  it('should format with custom format', () => {
    const result = formatDateTime('2024-01-15T10:30:00', 'YYYY/MM/DD')
    expect(result).toBe('2024/01/15')
  })

  it('should return "-" for null', () => {
    expect(formatDateTime(null)).toBe('-')
  })

  it('should return "-" for undefined', () => {
    expect(formatDateTime(undefined)).toBe('-')
  })

  it('should return "-" for empty string', () => {
    expect(formatDateTime('')).toBe('-')
  })

  it('should return "-" for 0', () => {
    expect(formatDateTime(0)).toBe('-')
  })

  it('should handle millisecond timestamp', () => {
    // 2024-01-15 in ms
    const ts = 1705276800000
    const result = formatDateTime(ts)
    expect(result).toContain('2024')
  })

  it('should handle second-level timestamp (auto-convert to ms)', () => {
    // 1705276800 is < 10000000000, so it gets multiplied by 1000
    const ts = 1705276800
    const result = formatDateTime(ts)
    expect(result).toContain('2024')
  })

  it('should handle Date object', () => {
    const date = new Date('2024-06-15T12:00:00Z')
    const result = formatDateTime(date)
    expect(result).toContain('2024')
  })
})

// ==================== formatDate ====================

describe('formatDate', () => {
  it('should format date string to YYYY-MM-DD', () => {
    const result = formatDate('2024-01-15T10:30:00')
    expect(result).toBe('2024-01-15')
  })

  it('should return empty string for null', () => {
    expect(formatDate(null)).toBe('')
  })

  it('should return empty string for undefined', () => {
    expect(formatDate(undefined)).toBe('')
  })

  it('should return empty string for empty string', () => {
    expect(formatDate('')).toBe('')
  })

  it('should handle numeric timestamp', () => {
    const result = formatDate(1705276800000)
    expect(result).toContain('2024')
  })
})

// ==================== formatTime ====================

describe('formatTime', () => {
  it('should format time portion', () => {
    const result = formatTime('2024-01-15T10:30:45')
    expect(result).toBe('10:30:45')
  })

  it('should return "-" for null', () => {
    expect(formatTime(null)).toBe('-')
  })

  it('should return "-" for undefined', () => {
    expect(formatTime(undefined)).toBe('-')
  })

  it('should return "-" for empty string', () => {
    expect(formatTime('')).toBe('-')
  })
})

// ==================== formatNumber ====================

describe('formatNumber', () => {
  it('should format number with thousand separators', () => {
    expect(formatNumber(1234567)).toBe('1,234,567')
  })

  it('should handle decimal places', () => {
    expect(formatNumber(1234.56, 2)).toBe('1,234.56')
  })

  it('should handle zero', () => {
    expect(formatNumber(0)).toBe('0')
  })

  it('should return "0" for NaN', () => {
    expect(formatNumber(NaN)).toBe('0')
  })

  it('should return "0" for null (cast as any)', () => {
    expect(formatNumber(null as any)).toBe('0')
  })

  it('should return "0" for undefined (cast as any)', () => {
    expect(formatNumber(undefined as any)).toBe('0')
  })

  it('should handle negative numbers', () => {
    const result = formatNumber(-1234567)
    expect(result).toBe('-1,234,567')
  })

  it('should handle very large numbers', () => {
    const result = formatNumber(999999999999)
    expect(result).toBe('999,999,999,999')
  })

  it('should pad decimals when specified', () => {
    expect(formatNumber(100, 2)).toBe('100.00')
  })
})

// ==================== formatFileSize ====================

describe('formatFileSize', () => {
  it('should return "0 B" for 0 bytes', () => {
    expect(formatFileSize(0)).toBe('0 B')
  })

  it('should format bytes below 1024', () => {
    expect(formatFileSize(500)).toBe('500 B')
  })

  it('should format kilobytes', () => {
    expect(formatFileSize(1024)).toBe('1.00 KB')
  })

  it('should format megabytes', () => {
    expect(formatFileSize(1024 * 1024)).toBe('1.00 MB')
  })

  it('should format gigabytes', () => {
    expect(formatFileSize(1024 * 1024 * 1024)).toBe('1.00 GB')
  })

  it('should format terabytes', () => {
    expect(formatFileSize(1024 * 1024 * 1024 * 1024)).toBe('1.00 TB')
  })

  it('should handle fractional KB', () => {
    const result = formatFileSize(1536) // 1.5 KB
    expect(result).toBe('1.50 KB')
  })
})

// ==================== formatCellValueSmart ====================

describe('formatCellValueSmart', () => {
  it('should return "-" for null', () => {
    expect(formatCellValueSmart(null)).toBe('-')
  })

  it('should return "-" for undefined', () => {
    expect(formatCellValueSmart(undefined)).toBe('-')
  })

  it('should return "-" for empty string', () => {
    expect(formatCellValueSmart('')).toBe('-')
  })

  it('should format boolean true as "是"', () => {
    expect(formatCellValueSmart(true)).toBe('是')
  })

  it('should format boolean false as "否"', () => {
    expect(formatCellValueSmart(false)).toBe('否')
  })

  it('should convert plain string to String', () => {
    expect(formatCellValueSmart('hello')).toBe('hello')
  })

  it('should convert number to String', () => {
    expect(formatCellValueSmart(42)).toBe('42')
  })

  it('should detect millisecond timestamp with date field name', () => {
    const ts = 1705276800000 // 2024-01-15
    const result = formatCellValueSmart(ts, { fieldName: 'create_date' })
    expect(result).toContain('2024')
  })

  it('should detect second-level timestamp with time field name', () => {
    const ts = 1705276800 // seconds
    const result = formatCellValueSmart(ts, { fieldName: 'updated_at' })
    expect(result).toContain('2024')
  })

  it('should detect date string with date field type', () => {
    const result = formatCellValueSmart('2024-01-15T10:30:00', { fieldType: 'datetime' })
    expect(result).toContain('2024')
    expect(result).toContain('10:30')
  })

  it('should detect date string with date field title', () => {
    const result = formatCellValueSmart('2024-01-15', { fieldTitle: '创建日期' })
    expect(result).toContain('2024')
  })

  it('should not format regular number as date without date hints', () => {
    const result = formatCellValueSmart(42)
    expect(result).toBe('42')
  })

  it('should handle special characters in string values', () => {
    expect(formatCellValueSmart('<script>alert("xss")</script>')).toBe('<script>alert("xss")</script>')
  })

  it('should handle unicode characters', () => {
    expect(formatCellValueSmart('你好世界🌍')).toBe('你好世界🌍')
  })
})

// ==================== formatTableCellValue ====================

describe('formatTableCellValue', () => {
  it('should return default "-" for null', () => {
    expect(formatTableCellValue(null)).toBe('-')
  })

  it('should return default "-" for undefined', () => {
    expect(formatTableCellValue(undefined)).toBe('-')
  })

  it('should return custom default for null', () => {
    expect(formatTableCellValue(null, 'N/A')).toBe('N/A')
  })

  it('should format boolean true as "是"', () => {
    expect(formatTableCellValue(true)).toBe('是')
  })

  it('should format boolean false as "否"', () => {
    expect(formatTableCellValue(false)).toBe('否')
  })

  it('should return default for NaN number', () => {
    expect(formatTableCellValue(NaN)).toBe('-')
  })

  it('should return default for empty string', () => {
    expect(formatTableCellValue('')).toBe('-')
  })

  it('should return default for whitespace-only string', () => {
    expect(formatTableCellValue('   ')).toBe('-')
  })

  it('should return default for string "null"', () => {
    expect(formatTableCellValue('null')).toBe('-')
  })

  it('should return default for string "undefined"', () => {
    expect(formatTableCellValue('undefined')).toBe('-')
  })

  it('should return default for string "NaN"', () => {
    expect(formatTableCellValue('NaN')).toBe('-')
  })

  it('should return default for string "none"', () => {
    expect(formatTableCellValue('none')).toBe('-')
  })

  it('should strip control characters', () => {
    expect(formatTableCellValue('hello\x00world')).toBe('helloworld')
  })

  it('should strip zero-width characters', () => {
    expect(formatTableCellValue('hello\u200Bworld')).toBe('helloworld')
  })

  it('should strip Unicode replacement character', () => {
    expect(formatTableCellValue('\uFFFD')).toBe('-')
  })

  it('should trim and return valid string', () => {
    expect(formatTableCellValue('  hello  ')).toBe('hello')
  })

  it('should handle normal string values', () => {
    expect(formatTableCellValue('hello')).toBe('hello')
  })

  it('should handle number values', () => {
    expect(formatTableCellValue(42)).toBe('42')
  })

  it('should handle special characters in valid strings', () => {
    expect(formatTableCellValue('hello@#$%')).toBe('hello@#$%')
  })

  it('should handle string with only invisible chars as empty', () => {
    expect(formatTableCellValue('\x00\x01\x02')).toBe('-')
  })
})

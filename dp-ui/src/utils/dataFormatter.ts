/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 数据格式化器
 * Data Formatter Utility
 * 
 * 支持数值、日期、文本格式化，参考帆软FineReport形态设计
 * Supports number, date, and text formatting (Reference: FineReport)
 * 
 * 需求: 14.3.11, 14.3.12, 14.3.13, 14.3.14, 14.3.15
 */

import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import 'dayjs/locale/en'
import type { NumberFormat, DateFormat, TextFormat, DataFormat } from '@/components/StyleEngine/types/style'

// Initialize dayjs plugins
dayjs.extend(relativeTime)

// ============================================================================
// Currency Configuration
// ============================================================================

/**
 * Currency symbols and configurations
 */
export const CURRENCY_CONFIG: Record<string, { symbol: string; position: 'prefix' | 'suffix'; locale: string }> = {
  CNY: { symbol: '¥', position: 'prefix', locale: 'zh-CN' },
  USD: { symbol: '$', position: 'prefix', locale: 'en-US' },
  EUR: { symbol: '€', position: 'prefix', locale: 'de-DE' },
  GBP: { symbol: '£', position: 'prefix', locale: 'en-GB' },
  JPY: { symbol: '¥', position: 'prefix', locale: 'ja-JP' },
  KRW: { symbol: '₩', position: 'prefix', locale: 'ko-KR' },
  HKD: { symbol: 'HK$', position: 'prefix', locale: 'zh-HK' },
  TWD: { symbol: 'NT$', position: 'prefix', locale: 'zh-TW' },
  SGD: { symbol: 'S$', position: 'prefix', locale: 'en-SG' },
  AUD: { symbol: 'A$', position: 'prefix', locale: 'en-AU' },
  CAD: { symbol: 'C$', position: 'prefix', locale: 'en-CA' },
  INR: { symbol: '₹', position: 'prefix', locale: 'en-IN' },
  RUB: { symbol: '₽', position: 'suffix', locale: 'ru-RU' },
  BRL: { symbol: 'R$', position: 'prefix', locale: 'pt-BR' },
}

// ============================================================================
// Date Format Presets
// ============================================================================

/**
 * Date format presets
 */
export const DATE_PRESETS: Record<string, Record<string, string>> = {
  short: {
    'zh-CN': 'YYYY/M/D',
    'en-US': 'M/D/YYYY',
    'en-GB': 'D/M/YYYY',
    default: 'YYYY-MM-DD',
  },
  medium: {
    'zh-CN': 'YYYY年M月D日',
    'en-US': 'MMM D, YYYY',
    'en-GB': 'D MMM YYYY',
    default: 'YYYY-MM-DD',
  },
  long: {
    'zh-CN': 'YYYY年M月D日 HH:mm',
    'en-US': 'MMMM D, YYYY h:mm A',
    'en-GB': 'D MMMM YYYY HH:mm',
    default: 'YYYY-MM-DD HH:mm',
  },
  full: {
    'zh-CN': 'YYYY年M月D日 dddd HH:mm:ss',
    'en-US': 'dddd, MMMM D, YYYY h:mm:ss A',
    'en-GB': 'dddd, D MMMM YYYY HH:mm:ss',
    default: 'YYYY-MM-DD HH:mm:ss',
  },
}

// ============================================================================
// Number Formatting
// ============================================================================

/**
 * Format a number with the given configuration
 * 格式化数值
 * 
 * Validates: 14.3.11 - 数值格式化（千分位、小数位数、百分比、货币符号）
 * 
 * @param value - The number to format
 * @param config - Number format configuration
 * @returns Formatted string
 */
export function formatNumber(value: number | null | undefined, config: NumberFormat = {}): string {
  // Handle null/undefined/NaN
  if (value === null || value === undefined || Number.isNaN(value)) {
    return ''
  }

  const {
    decimalPlaces,
    useThousandsSeparator = true,
    negativeFormat = 'minus',
    prefix = '',
    suffix = '',
    asPercentage = false,
    asCurrency = false,
    currencyCode = 'CNY',
    abbreviate = false,
    abbreviateThreshold = 10000,
  } = config

  const num = value
  const isNegative = num < 0
  let absNum = Math.abs(num)

  // Handle percentage conversion
  if (asPercentage) {
    absNum = absNum * 100
  }

  // Handle abbreviation (K, M, B, T)
  let abbreviationSuffix = ''
  if (abbreviate && absNum >= abbreviateThreshold) {
    const result = abbreviateNumber(absNum)
    absNum = result.value
    abbreviationSuffix = result.suffix
  }

  // Format the number
  let formatted: string
  const effectiveDecimalPlaces = decimalPlaces ?? (asPercentage ? 1 : (abbreviate && abbreviationSuffix ? 1 : 0))
  
  if (useThousandsSeparator) {
    formatted = absNum.toLocaleString('en-US', {
      minimumFractionDigits: effectiveDecimalPlaces,
      maximumFractionDigits: effectiveDecimalPlaces,
    })
  } else {
    formatted = absNum.toFixed(effectiveDecimalPlaces)
  }

  // Add abbreviation suffix
  formatted += abbreviationSuffix

  // Add percentage suffix
  if (asPercentage) {
    formatted += '%'
  }

  // Handle currency
  let currencyPrefix = ''
  let currencySuffix = ''
  if (asCurrency && currencyCode) {
    const currencyConfig = CURRENCY_CONFIG[currencyCode]
    if (currencyConfig) {
      if (currencyConfig.position === 'prefix') {
        currencyPrefix = currencyConfig.symbol
      } else {
        currencySuffix = currencyConfig.symbol
      }
    }
  }

  // Apply prefix and suffix
  const fullPrefix = prefix + currencyPrefix
  const fullSuffix = currencySuffix + suffix

  // Handle negative format
  if (isNegative) {
    formatted = applyNegativeFormat(formatted, negativeFormat, fullPrefix, fullSuffix)
  } else {
    formatted = fullPrefix + formatted + fullSuffix
  }

  return formatted
}

/**
 * Abbreviate large numbers
 * 大数缩写
 */
function abbreviateNumber(num: number): { value: number; suffix: string } {
  const abbreviations = [
    { threshold: 1e12, suffix: 'T' },
    { threshold: 1e9, suffix: 'B' },
    { threshold: 1e6, suffix: 'M' },
    { threshold: 1e3, suffix: 'K' },
  ]

  for (const { threshold, suffix } of abbreviations) {
    if (num >= threshold) {
      return { value: num / threshold, suffix }
    }
  }

  return { value: num, suffix: '' }
}

/**
 * Apply negative number format
 * 应用负数格式
 */
function applyNegativeFormat(
  formatted: string,
  format: 'minus' | 'parentheses' | 'red' | 'redParentheses',
  prefix: string,
  suffix: string
): string {
  switch (format) {
    case 'parentheses':
      return `(${prefix}${formatted}${suffix})`
    case 'red':
      // Note: Color styling should be handled by CSS, we just return the value with minus
      return `-${prefix}${formatted}${suffix}`
    case 'redParentheses':
      // Note: Color styling should be handled by CSS
      return `(${prefix}${formatted}${suffix})`
    case 'minus':
    default:
      return `-${prefix}${formatted}${suffix}`
  }
}

/**
 * Check if a negative format uses parentheses
 * 检查负数格式是否使用括号
 */
export function isParenthesesFormat(format: 'minus' | 'parentheses' | 'red' | 'redParentheses'): boolean {
  return format === 'parentheses' || format === 'redParentheses'
}

/**
 * Check if a negative format uses red color
 * 检查负数格式是否使用红色
 */
export function isRedFormat(format: 'minus' | 'parentheses' | 'red' | 'redParentheses'): boolean {
  return format === 'red' || format === 'redParentheses'
}

// ============================================================================
// Date Formatting
// ============================================================================

/**
 * Format a date with the given configuration
 * 格式化日期
 * 
 * Validates: 14.3.12 - 日期格式化（多种预设格式和自定义格式）
 * 
 * @param value - The date to format (string, Date, or timestamp)
 * @param config - Date format configuration
 * @returns Formatted string
 */
export function formatDate(
  value: string | Date | number | null | undefined,
  config: DateFormat = {}
): string {
  // Handle null/undefined/empty
  if (value === null || value === undefined || value === '') {
    return ''
  }

  const { pattern, preset = 'medium', locale = 'zh-CN' } = config

  // Parse the date
  let date: dayjs.Dayjs
  
  if (typeof value === 'number') {
    // Handle timestamp (seconds or milliseconds)
    const timestamp = value < 10000000000 ? value * 1000 : value
    date = dayjs(timestamp)
  } else {
    date = dayjs(value)
  }

  // Validate the date
  if (!date.isValid()) {
    return ''
  }

  // Set locale
  const dayjsLocale = locale === 'zh-CN' ? 'zh-cn' : 'en'
  date = date.locale(dayjsLocale)

  // Handle relative time preset
  if (preset === 'relative') {
    return date.fromNow()
  }

  // Use custom pattern if provided
  if (pattern) {
    return date.format(pattern)
  }

  // Use preset format
  const presetFormats = DATE_PRESETS[preset]
  if (presetFormats) {
    const format = presetFormats[locale] || presetFormats.default
    return date.format(format)
  }

  // Default format
  return date.format('YYYY-MM-DD')
}

/**
 * Get available date format presets
 * 获取可用的日期格式预设
 */
export function getDatePresets(): string[] {
  return Object.keys(DATE_PRESETS)
}

/**
 * Get date format pattern for a preset and locale
 * 获取预设和语言环境的日期格式模式
 */
export function getDatePresetPattern(preset: string, locale: string = 'zh-CN'): string {
  const presetFormats = DATE_PRESETS[preset]
  if (presetFormats) {
    return presetFormats[locale] || presetFormats.default
  }
  return 'YYYY-MM-DD'
}

// ============================================================================
// Text Formatting
// ============================================================================

/**
 * Format text with the given configuration
 * 格式化文本
 * 
 * Validates: 14.3.13 - 文本格式化（前缀、后缀、大小写转换）
 * Validates: 14.3.14 - 空值显示配置
 * Validates: 14.3.15 - 超长文本处理
 * 
 * @param value - The text to format
 * @param config - Text format configuration
 * @returns Formatted string and metadata
 */
export function formatText(
  value: string | null | undefined,
  config: TextFormat = {}
): FormatTextResult {
  const {
    case: textCase = 'none',
    prefix = '',
    suffix = '',
    maxLength,
    overflow = 'ellipsis',
    emptyText = '-',
  } = config

  // Handle empty values (Requirement 14.3.14)
  if (value === null || value === undefined || value === '') {
    return {
      formatted: emptyText,
      truncated: false,
      originalLength: 0,
    }
  }

  let text = String(value)
  const originalLength = text.length

  // Apply case transformation (Requirement 14.3.13)
  text = applyTextCase(text, textCase)

  // Handle overflow/truncation (Requirement 14.3.15)
  let truncated = false
  let fullText = text

  if (maxLength && text.length > maxLength) {
    truncated = true
    fullText = text
    
    switch (overflow) {
      case 'truncate':
        text = text.substring(0, maxLength)
        break
      case 'ellipsis':
        text = text.substring(0, maxLength - 3) + '...'
        break
      case 'wrap':
        // Wrap is handled by CSS, we don't modify the text
        truncated = false
        break
      case 'tooltip':
        // Tooltip shows full text on hover, we truncate with ellipsis
        text = text.substring(0, maxLength - 3) + '...'
        break
    }
  }

  // Apply prefix and suffix
  const formatted = prefix + text + suffix

  return {
    formatted,
    truncated,
    originalLength,
    fullText: truncated ? fullText : undefined,
    overflow,
  }
}

/**
 * Result of text formatting
 */
export interface FormatTextResult {
  /** Formatted text */
  formatted: string
  /** Whether the text was truncated */
  truncated: boolean
  /** Original text length before truncation */
  originalLength: number
  /** Full text before truncation (only if truncated) */
  fullText?: string
  /** Overflow mode used */
  overflow?: 'truncate' | 'ellipsis' | 'wrap' | 'tooltip'
}

/**
 * Apply text case transformation
 * 应用文本大小写转换
 */
function applyTextCase(
  text: string,
  textCase: 'none' | 'upper' | 'lower' | 'capitalize' | 'titleCase'
): string {
  switch (textCase) {
    case 'upper':
      return text.toUpperCase()
    case 'lower':
      return text.toLowerCase()
    case 'capitalize':
      return text.charAt(0).toUpperCase() + text.slice(1).toLowerCase()
    case 'titleCase':
      return text
        .split(/\s+/)
        .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
        .join(' ')
    case 'none':
    default:
      return text
  }
}

// ============================================================================
// Unified Data Formatter
// ============================================================================

/**
 * Format any value based on DataFormat configuration
 * 根据 DataFormat 配置格式化任意值
 * 
 * @param value - The value to format
 * @param format - Data format configuration
 * @returns Formatted string
 */
export function formatValue(value: any, format?: DataFormat): string {
  if (!format) {
    // Default formatting for different types
    if (value === null || value === undefined) {
      return '-'
    }
    if (typeof value === 'number') {
      return formatNumber(value)
    }
    if (value instanceof Date) {
      return formatDate(value)
    }
    return String(value)
  }

  switch (format.type) {
    case 'number':
      return formatNumber(Number(value), format.config as NumberFormat)
    case 'date':
      return formatDate(value, format.config as DateFormat)
    case 'text':
      return formatText(String(value ?? ''), format.config as TextFormat).formatted
    case 'custom':
      return formatCustom(value, format.config as CustomFormat)
    default:
      return String(value ?? '-')
  }
}

/**
 * Custom format configuration
 */
export interface CustomFormat {
  expression: string
}

/**
 * Format value using custom expression
 * 使用自定义表达式格式化值
 */
function formatCustom(value: any, config: CustomFormat): string {
  if (!config.expression) {
    return String(value ?? '')
  }

  try {
    // Simple expression evaluation
    // Supports: ${value}, ${value:format}
    let result = config.expression

    // Replace ${value} with the actual value
    result = result.replace(/\$\{value\}/g, String(value ?? ''))

    // Replace ${value:number:2} with formatted number
    result = result.replace(/\$\{value:number:(\d+)\}/g, (_, decimals) => {
      return formatNumber(Number(value), { decimalPlaces: parseInt(decimals) })
    })

    // Replace ${value:date:pattern} with formatted date
    result = result.replace(/\$\{value:date:([^}]+)\}/g, (_, pattern) => {
      return formatDate(value, { pattern })
    })

    return result
  } catch {
    return String(value ?? '')
  }
}

// ============================================================================
// Empty Value Handling
// ============================================================================

/**
 * Empty value display options
 * 空值显示选项
 */
export type EmptyDisplayType = 'empty' | 'text' | 'dash' | 'custom'

/**
 * Get display text for empty values
 * 获取空值的显示文本
 * 
 * Validates: 14.3.14 - 空值显示配置
 * 
 * @param type - Empty display type
 * @param customText - Custom text for 'custom' type
 * @returns Display text for empty values
 */
export function getEmptyDisplayText(type: EmptyDisplayType, customText?: string): string {
  switch (type) {
    case 'empty':
      return ''
    case 'text':
      return customText || ''
    case 'dash':
      return '-'
    case 'custom':
      return customText || '-'
    default:
      return '-'
  }
}

/**
 * Check if a value is considered empty
 * 检查值是否为空
 */
export function isEmpty(value: any): boolean {
  if (value === null || value === undefined) {
    return true
  }
  if (typeof value === 'string' && value.trim() === '') {
    return true
  }
  if (typeof value === 'number' && Number.isNaN(value)) {
    return true
  }
  return false
}

// ============================================================================
// Utility Functions
// ============================================================================

/**
 * Create a number formatter function with preset configuration
 * 创建带有预设配置的数值格式化函数
 */
export function createNumberFormatter(config: NumberFormat): (value: number | null | undefined) => string {
  return (value) => formatNumber(value, config)
}

/**
 * Create a date formatter function with preset configuration
 * 创建带有预设配置的日期格式化函数
 */
export function createDateFormatter(config: DateFormat): (value: string | Date | number | null | undefined) => string {
  return (value) => formatDate(value, config)
}

/**
 * Create a text formatter function with preset configuration
 * 创建带有预设配置的文本格式化函数
 */
export function createTextFormatter(config: TextFormat): (value: string | null | undefined) => FormatTextResult {
  return (value) => formatText(value, config)
}

// ============================================================================
// Preset Formatters
// ============================================================================

/**
 * Preset number formatters
 * 预设数值格式化器
 */
export const NumberFormatters = {
  /** Integer with thousands separator */
  integer: createNumberFormatter({ decimalPlaces: 0, useThousandsSeparator: true }),
  
  /** Decimal with 2 places */
  decimal2: createNumberFormatter({ decimalPlaces: 2, useThousandsSeparator: true }),
  
  /** Percentage with 1 decimal place */
  percent: createNumberFormatter({ asPercentage: true, decimalPlaces: 1 }),
  
  /** Currency CNY */
  currencyCNY: createNumberFormatter({ asCurrency: true, currencyCode: 'CNY', decimalPlaces: 2 }),
  
  /** Currency USD */
  currencyUSD: createNumberFormatter({ asCurrency: true, currencyCode: 'USD', decimalPlaces: 2 }),
  
  /** Abbreviated large numbers */
  abbreviated: createNumberFormatter({ abbreviate: true, abbreviateThreshold: 10000 }),
  
  /** Negative in parentheses (accounting style) */
  accounting: createNumberFormatter({ 
    decimalPlaces: 2, 
    useThousandsSeparator: true, 
    negativeFormat: 'parentheses' 
  }),
}

/**
 * Preset date formatters
 * 预设日期格式化器
 */
export const DateFormatters = {
  /** Short date format */
  short: createDateFormatter({ preset: 'short' }),
  
  /** Medium date format */
  medium: createDateFormatter({ preset: 'medium' }),
  
  /** Long date format with time */
  long: createDateFormatter({ preset: 'long' }),
  
  /** Full date format */
  full: createDateFormatter({ preset: 'full' }),
  
  /** Relative time (e.g., "2 hours ago") */
  relative: createDateFormatter({ preset: 'relative' }),
  
  /** ISO format */
  iso: createDateFormatter({ pattern: 'YYYY-MM-DD' }),
  
  /** DateTime format */
  datetime: createDateFormatter({ pattern: 'YYYY-MM-DD HH:mm:ss' }),
}

/**
 * Preset text formatters
 * 预设文本格式化器
 */
export const TextFormatters = {
  /** Uppercase */
  upper: createTextFormatter({ case: 'upper' }),
  
  /** Lowercase */
  lower: createTextFormatter({ case: 'lower' }),
  
  /** Capitalize first letter */
  capitalize: createTextFormatter({ case: 'capitalize' }),
  
  /** Title case */
  titleCase: createTextFormatter({ case: 'titleCase' }),
  
  /** Truncate with ellipsis at 50 chars */
  truncate50: createTextFormatter({ maxLength: 50, overflow: 'ellipsis' }),
  
  /** Truncate with ellipsis at 100 chars */
  truncate100: createTextFormatter({ maxLength: 100, overflow: 'ellipsis' }),
}

// ============================================================================
// Export Default
// ============================================================================

export default {
  formatNumber,
  formatDate,
  formatText,
  formatValue,
  isEmpty,
  getEmptyDisplayText,
  createNumberFormatter,
  createDateFormatter,
  createTextFormatter,
  NumberFormatters,
  DateFormatters,
  TextFormatters,
  CURRENCY_CONFIG,
  DATE_PRESETS,
}

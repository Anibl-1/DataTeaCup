/**
 * 本地化格式化 Composable
 * 
 * 提供日期和数字的本地化格式化功能，根据当前语言设置自动调整格式。
 * 
 * **Validates: Requirements 24.2, 24.3**
 * - 24.2: THE I18n_Manager SHALL 根据用户语言设置自动调整日期显示格式
 * - 24.3: THE I18n_Manager SHALL 根据用户语言设置自动调整数字格式（千分位分隔符、小数点符号）
 */

import { computed } from 'vue'
import { 
  currentLocale, 
  dateFormats, 
  numberFormats, 
  naiveDateLocale,
  type Locale 
} from '@/i18n'

// 日期格式类型
export type DateFormatType = 'short' | 'long' | 'datetime' | 'time'

// 数字格式选项
export interface NumberFormatOptions {
  /** 小数位数 */
  decimals?: number
  /** 是否使用千分位分隔符 */
  useThousands?: boolean
  /** 是否显示为货币格式 */
  currency?: boolean
  /** 是否显示为百分比 */
  percent?: boolean
  /** 自定义货币符号 */
  currencySymbol?: string
  /** 货币符号位置 */
  currencyPosition?: 'prefix' | 'suffix'
}

// 日期格式配置
export interface DateFormatConfig {
  short: string
  long: string
  datetime: string
  time: string
}

// 数字格式配置
export interface NumberFormatConfig {
  decimal: string
  thousands: string
  currency: string
}

/**
 * 获取当前语言的日期格式配置
 */
export function getDateFormatConfig(locale: Locale): DateFormatConfig {
  return dateFormats[locale]
}

/**
 * 获取当前语言的数字格式配置
 */
export function getNumberFormatConfig(locale: Locale): NumberFormatConfig {
  return numberFormats[locale]
}

/**
 * 格式化日期
 * 根据当前语言设置自动调整日期格式
 * 
 * @param date - 要格式化的日期
 * @param format - 格式类型：short, long, datetime, time
 * @param locale - 可选的语言设置，默认使用当前语言
 * @returns 格式化后的日期字符串
 */
export function formatDateLocale(
  date: Date | number | string | null | undefined,
  format: DateFormatType = 'short',
  locale?: Locale
): string {
  if (date === null || date === undefined) {
    return ''
  }

  const d = new Date(date)
  
  // 检查日期是否有效
  if (isNaN(d.getTime())) {
    return ''
  }

  const currentLang = locale || currentLocale.value
  
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')

  // 获取月份名称（用于 long 格式）
  const monthNames = {
    'en-US': [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ],
    'zh-CN': [
      '一月', '二月', '三月', '四月', '五月', '六月',
      '七月', '八月', '九月', '十月', '十一月', '十二月'
    ]
  }

  switch (format) {
    case 'short':
      // zh-CN: YYYY-MM-DD, en-US: MM/DD/YYYY
      return currentLang === 'zh-CN'
        ? `${year}-${month}-${day}`
        : `${month}/${day}/${year}`
    
    case 'long':
      // zh-CN: YYYY年MM月DD日, en-US: MMMM DD, YYYY
      if (currentLang === 'zh-CN') {
        return `${year}年${parseInt(month)}月${parseInt(day)}日`
      } else {
        const monthName = monthNames['en-US'][d.getMonth()]
        return `${monthName} ${parseInt(day)}, ${year}`
      }
    
    case 'datetime':
      // zh-CN: YYYY-MM-DD HH:mm:ss, en-US: MM/DD/YYYY HH:mm:ss
      return currentLang === 'zh-CN'
        ? `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
        : `${month}/${day}/${year} ${hours}:${minutes}:${seconds}`
    
    case 'time':
      // 时间格式在两种语言中相同
      return `${hours}:${minutes}:${seconds}`
    
    default:
      return `${year}-${month}-${day}`
  }
}

/**
 * 格式化数字
 * 根据当前语言设置自动调整数字格式
 * 
 * @param num - 要格式化的数字
 * @param options - 格式化选项
 * @param locale - 可选的语言设置，默认使用当前语言
 * @returns 格式化后的数字字符串
 */
export function formatNumberLocale(
  num: number | null | undefined,
  options?: NumberFormatOptions,
  locale?: Locale
): string {
  if (num === null || num === undefined || isNaN(num)) {
    return ''
  }

  const currentLang = locale || currentLocale.value
  const config = numberFormats[currentLang]
  
  const {
    decimals = 0,
    useThousands = true,
    currency = false,
    percent = false,
    currencySymbol,
    currencyPosition = 'prefix'
  } = options || {}

  let result: string
  let value = num

  // 处理百分比
  if (percent) {
    value = num * 100
    result = value.toFixed(decimals) + '%'
  } else {
    result = value.toFixed(decimals)
  }

  // 添加千分位分隔符
  if (useThousands && !percent) {
    const parts = result.split('.')
    parts[0] = parts[0]!.replace(/\B(?=(\d{3})+(?!\d))/g, config.thousands)
    result = parts.join(config.decimal)
  } else if (!percent) {
    // 替换小数点符号
    result = result.replace('.', config.decimal)
  }

  // 处理货币格式
  if (currency) {
    const symbol = currencySymbol || config.currency
    if (currencyPosition === 'suffix') {
      result = result + symbol
    } else {
      result = symbol + result
    }
  }

  return result
}

/**
 * 格式化货币
 * 便捷方法，用于格式化货币值
 * 
 * @param num - 要格式化的数字
 * @param decimals - 小数位数，默认为2
 * @param locale - 可选的语言设置
 * @returns 格式化后的货币字符串
 */
export function formatCurrency(
  num: number | null | undefined,
  decimals: number = 2,
  locale?: Locale
): string {
  return formatNumberLocale(num, {
    decimals,
    useThousands: true,
    currency: true
  }, locale)
}

/**
 * 格式化百分比
 * 便捷方法，用于格式化百分比值
 * 
 * @param num - 要格式化的数字（0-1之间的小数）
 * @param decimals - 小数位数，默认为1
 * @param locale - 可选的语言设置
 * @returns 格式化后的百分比字符串
 */
export function formatPercent(
  num: number | null | undefined,
  decimals: number = 1,
  locale?: Locale
): string {
  return formatNumberLocale(num, {
    decimals,
    percent: true
  }, locale)
}

/**
 * 格式化小数
 * 便捷方法，用于格式化小数值
 * 
 * @param num - 要格式化的数字
 * @param decimals - 小数位数，默认为2
 * @param locale - 可选的语言设置
 * @returns 格式化后的小数字符串
 */
export function formatDecimal(
  num: number | null | undefined,
  decimals: number = 2,
  locale?: Locale
): string {
  return formatNumberLocale(num, {
    decimals,
    useThousands: true
  }, locale)
}

/**
 * 本地化格式化 Composable
 * 
 * 提供响应式的日期和数字格式化功能，当语言切换时自动更新格式。
 * 
 * @example
 * ```vue
 * <script setup>
 * import { useLocaleFormat } from '@/composables/useLocaleFormat'
 * 
 * const { formatDate, formatNumber, formatCurrency, formatPercent, locale, dateConfig, numberConfig } = useLocaleFormat()
 * 
 * const date = new Date()
 * const formattedDate = formatDate(date, 'long')
 * const formattedNumber = formatNumber(12345.67, { decimals: 2, useThousands: true })
 * const formattedCurrency = formatCurrency(1234.56)
 * const formattedPercent = formatPercent(0.85)
 * </script>
 * ```
 */
export function useLocaleFormat() {
  // 当前语言
  const locale = computed(() => currentLocale.value)

  // 当前日期格式配置
  const dateConfig = computed<DateFormatConfig>(() => {
    return getDateFormatConfig(currentLocale.value)
  })

  // 当前数字格式配置
  const numberConfig = computed<NumberFormatConfig>(() => {
    return getNumberFormatConfig(currentLocale.value)
  })

  // Naive UI 日期本地化配置
  const naiveDateLocaleConfig = computed(() => naiveDateLocale.value)

  // 格式化日期的响应式方法
  const formatDate = (
    date: Date | number | string | null | undefined,
    format: DateFormatType = 'short'
  ): string => {
    return formatDateLocale(date, format, currentLocale.value)
  }

  // 格式化数字的响应式方法
  const formatNumber = (
    num: number | null | undefined,
    options?: NumberFormatOptions
  ): string => {
    return formatNumberLocale(num, options, currentLocale.value)
  }

  // 格式化货币的响应式方法
  const formatCurrencyValue = (
    num: number | null | undefined,
    decimals: number = 2
  ): string => {
    return formatCurrency(num, decimals, currentLocale.value)
  }

  // 格式化百分比的响应式方法
  const formatPercentValue = (
    num: number | null | undefined,
    decimals: number = 1
  ): string => {
    return formatPercent(num, decimals, currentLocale.value)
  }

  // 格式化小数的响应式方法
  const formatDecimalValue = (
    num: number | null | undefined,
    decimals: number = 2
  ): string => {
    return formatDecimal(num, decimals, currentLocale.value)
  }

  // 获取日期格式模式字符串
  const getDatePattern = (format: DateFormatType = 'short'): string => {
    return dateConfig.value[format]
  }

  // 获取货币符号
  const getCurrencySymbol = (): string => {
    return numberConfig.value.currency
  }

  // 获取千分位分隔符
  const getThousandsSeparator = (): string => {
    return numberConfig.value.thousands
  }

  // 获取小数点符号
  const getDecimalSeparator = (): string => {
    return numberConfig.value.decimal
  }

  return {
    // 当前语言
    locale,
    
    // 配置
    dateConfig,
    numberConfig,
    naiveDateLocaleConfig,
    
    // 格式化方法
    formatDate,
    formatNumber,
    formatCurrency: formatCurrencyValue,
    formatPercent: formatPercentValue,
    formatDecimal: formatDecimalValue,
    
    // 辅助方法
    getDatePattern,
    getCurrencySymbol,
    getThousandsSeparator,
    getDecimalSeparator,
    
    // 静态方法（不依赖响应式）
    formatDateLocale,
    formatNumberLocale
  }
}

export default useLocaleFormat

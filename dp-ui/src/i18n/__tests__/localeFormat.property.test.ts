/**
 * 本地化格式属性测试
 * 
 * 测试日期和数字的本地化格式化功能，确保根据语言设置正确格式化。
 * 
 * **Validates: Requirements 24.2, 24.3**
 * - 24.2: THE I18n_Manager SHALL 根据用户语言设置自动调整日期显示格式
 * - 24.3: THE I18n_Manager SHALL 根据用户语言设置自动调整数字格式（千分位分隔符、小数点符号）
 */

import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import * as fc from 'fast-check'
import { 
  formatDate, 
  formatNumber, 
  formatCurrency, 
  formatPercent, 
  currentLocale, 
  setLocale,
  dateFormats,
  numberFormats,
  type Locale,
  type DateFormatType
} from '../index'
import { 
  useLocaleFormat,
  formatDateLocale,
  formatNumberLocale
} from '@/composables/useLocaleFormat'

// 支持的语言列表
const SUPPORTED_LOCALES: Locale[] = ['zh-CN', 'en-US']

// 日期格式类型
const DATE_FORMAT_TYPES: DateFormatType[] = ['short', 'long', 'datetime', 'time']

describe('Feature: platform-deep-optimization, 本地化格式属性测试', () => {
  // 保存原始语言设置
  let originalLocale: Locale

  beforeEach(() => {
    originalLocale = currentLocale.value
  })

  afterEach(() => {
    setLocale(originalLocale)
  })

  // ==================== Arbitraries ====================

  // Arbitrary for selecting a locale
  const localeArb = fc.constantFrom<Locale>(...SUPPORTED_LOCALES)

  // Arbitrary for selecting a date format type
  const dateFormatArb = fc.constantFrom<DateFormatType>(...DATE_FORMAT_TYPES)

  // Arbitrary for generating valid dates (within reasonable range)
  const dateArb = fc.date({
    min: new Date('1970-01-01'),
    max: new Date('2100-12-31')
  }).filter(d => !isNaN(d.getTime()))

  // Arbitrary for generating numbers
  const numberArb = fc.double({
    min: -1000000000,
    max: 1000000000,
    noNaN: true,
    noDefaultInfinity: true
  })

  // Arbitrary for generating positive numbers (for currency/percent)
  const positiveNumberArb = fc.double({
    min: 0,
    max: 1000000000,
    noNaN: true,
    noDefaultInfinity: true
  })

  // Arbitrary for generating decimal places (0-10)
  const decimalsArb = fc.integer({ min: 0, max: 10 })

  // Arbitrary for generating percent values (0-1)
  const percentArb = fc.double({
    min: 0,
    max: 1,
    noNaN: true,
    noDefaultInfinity: true
  })

  // ==================== Property 62: 本地化格式正确性 ====================

  describe('Property 62: 本地化格式正确性', () => {
    describe('Property 62.1: 日期格式根据语言设置变化', () => {
      it('should format dates differently based on locale', () => {
        fc.assert(
          fc.property(
            dateArb,
            dateFormatArb,
            (date, format) => {
              // 设置中文
              setLocale('zh-CN')
              const zhResult = formatDate(date, format)

              // 设置英文
              setLocale('en-US')
              const enResult = formatDate(date, format)

              // Property: 对于 short 和 datetime 格式，中英文格式应该不同
              if (format === 'short' || format === 'datetime') {
                // zh-CN 使用 YYYY-MM-DD 格式
                // en-US 使用 MM/DD/YYYY 格式
                expect(zhResult).not.toBe(enResult)
              }

              // Property: 对于 long 格式，中英文格式应该不同
              if (format === 'long') {
                // zh-CN 使用 YYYY年MM月DD日 格式
                // en-US 使用 MMMM DD, YYYY 格式
                expect(zhResult).not.toBe(enResult)
              }

              // Property: 对于 time 格式，中英文格式应该相同
              if (format === 'time') {
                expect(zhResult).toBe(enResult)
              }
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    describe('Property 62.2: zh-CN 日期格式正确性', () => {
      it('should format dates correctly for zh-CN locale', () => {
        setLocale('zh-CN')

        fc.assert(
          fc.property(
            dateArb,
            (date) => {
              const year = date.getFullYear()
              const month = String(date.getMonth() + 1).padStart(2, '0')
              const day = String(date.getDate()).padStart(2, '0')

              // Property: short 格式应该是 YYYY-MM-DD
              const shortResult = formatDate(date, 'short')
              expect(shortResult).toBe(`${year}-${month}-${day}`)

              // Property: long 格式应该是 YYYY年M月D日
              const longResult = formatDate(date, 'long')
              expect(longResult).toContain('年')
              expect(longResult).toContain('月')
              expect(longResult).toContain('日')
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    describe('Property 62.3: en-US 日期格式正确性', () => {
      it('should format dates correctly for en-US locale', () => {
        setLocale('en-US')

        fc.assert(
          fc.property(
            dateArb,
            (date) => {
              const year = date.getFullYear()
              const month = String(date.getMonth() + 1).padStart(2, '0')
              const day = String(date.getDate()).padStart(2, '0')

              // Property: short 格式应该是 MM/DD/YYYY
              const shortResult = formatDate(date, 'short')
              expect(shortResult).toBe(`${month}/${day}/${year}`)

              // Property: long 格式应该包含月份名称
              const longResult = formatDate(date, 'long')
              const monthNames = [
                'January', 'February', 'March', 'April', 'May', 'June',
                'July', 'August', 'September', 'October', 'November', 'December'
              ]
              const hasMonthName = monthNames.some(name => longResult.includes(name))
              expect(hasMonthName).toBe(true)
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    describe('Property 62.4: 数字格式根据语言设置变化', () => {
      it('should format numbers with correct currency symbol based on locale', () => {
        fc.assert(
          fc.property(
            positiveNumberArb,
            decimalsArb,
            (num, decimals) => {
              // 设置中文
              setLocale('zh-CN')
              const zhResult = formatCurrency(num, decimals)

              // 设置英文
              setLocale('en-US')
              const enResult = formatCurrency(num, decimals)

              // Property: zh-CN 应该使用 ¥ 符号
              expect(zhResult).toContain('¥')

              // Property: en-US 应该使用 $ 符号
              expect(enResult).toContain('$')
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    describe('Property 62.5: 千分位分隔符正确性', () => {
      it('should add thousands separator correctly', () => {
        fc.assert(
          fc.property(
            localeArb,
            fc.integer({ min: 1000, max: 1000000000 }),
            (locale, num) => {
              setLocale(locale)
              const result = formatNumber(num, { useThousands: true })

              // Property: 大于等于1000的数字应该包含千分位分隔符
              const config = numberFormats[locale]
              expect(result).toContain(config.thousands)
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    describe('Property 62.6: 小数位数正确性', () => {
      it('should format numbers with correct decimal places', () => {
        fc.assert(
          fc.property(
            localeArb,
            numberArb,
            decimalsArb,
            (locale, num, decimals) => {
              setLocale(locale)
              const result = formatNumber(num, { decimals, useThousands: false })

              // Property: 结果应该有正确的小数位数
              const config = numberFormats[locale]
              if (decimals > 0) {
                const parts = result.split(config.decimal)
                if (parts.length === 2) {
                  expect(parts[1].length).toBe(decimals)
                }
              }
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    describe('Property 62.7: 百分比格式正确性', () => {
      it('should format percentages correctly', () => {
        fc.assert(
          fc.property(
            localeArb,
            percentArb,
            decimalsArb,
            (locale, num, decimals) => {
              setLocale(locale)
              const result = formatPercent(num, decimals)

              // Property: 结果应该以 % 结尾
              expect(result).toMatch(/%$/)

              // Property: 值应该乘以100
              const expectedValue = (num * 100).toFixed(decimals)
              expect(result).toBe(`${expectedValue}%`)
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    describe('Property 62.8: 空值处理', () => {
      it('should handle null and undefined values gracefully', () => {
        fc.assert(
          fc.property(
            localeArb,
            (locale) => {
              setLocale(locale)

              // Property: null 日期应该返回空字符串
              expect(formatDate(null, 'short')).toBe('')
              expect(formatDate(undefined, 'short')).toBe('')

              // Property: null 数字应该返回空字符串
              expect(formatNumber(null)).toBe('')
              expect(formatNumber(undefined)).toBe('')
              expect(formatCurrency(null)).toBe('')
              expect(formatPercent(null)).toBe('')
            }
          ),
          { numRuns: 10 }
        )
      })
    })

    describe('Property 62.9: 无效日期处理', () => {
      it('should handle invalid dates gracefully', () => {
        fc.assert(
          fc.property(
            localeArb,
            dateFormatArb,
            (locale, format) => {
              setLocale(locale)

              // Property: 无效日期应该返回空字符串
              expect(formatDate('invalid-date', format)).toBe('')
              expect(formatDate(NaN, format)).toBe('')
            }
          ),
          { numRuns: 10 }
        )
      })
    })

    describe('Property 62.10: NaN 数字处理', () => {
      it('should handle NaN values gracefully', () => {
        fc.assert(
          fc.property(
            localeArb,
            (locale) => {
              setLocale(locale)

              // Property: NaN 应该返回空字符串
              expect(formatNumber(NaN)).toBe('')
              expect(formatCurrency(NaN)).toBe('')
              expect(formatPercent(NaN)).toBe('')
            }
          ),
          { numRuns: 10 }
        )
      })
    })
  })

  // ==================== useLocaleFormat Composable Tests ====================

  describe('useLocaleFormat Composable', () => {
    describe('Property: Composable 返回正确的配置', () => {
      it('should return correct date and number config based on locale', () => {
        fc.assert(
          fc.property(
            localeArb,
            (locale) => {
              setLocale(locale)
              const { dateConfig, numberConfig } = useLocaleFormat()

              // Property: dateConfig 应该匹配当前语言的配置
              expect(dateConfig.value).toEqual(dateFormats[locale])

              // Property: numberConfig 应该匹配当前语言的配置
              expect(numberConfig.value).toEqual(numberFormats[locale])
            }
          ),
          { numRuns: 10 }
        )
      })
    })

    describe('Property: Composable 格式化方法与全局方法一致', () => {
      it('should produce same results as global format functions', () => {
        fc.assert(
          fc.property(
            localeArb,
            dateArb,
            numberArb,
            dateFormatArb,
            (locale, date, num, format) => {
              setLocale(locale)
              const { formatDate: composableFormatDate, formatNumber: composableFormatNumber } = useLocaleFormat()

              // Property: Composable 的 formatDate 应该与全局 formatDate 结果一致
              expect(composableFormatDate(date, format)).toBe(formatDate(date, format))

              // Property: Composable 的 formatNumber 应该与全局 formatNumber 结果一致
              expect(composableFormatNumber(num, { decimals: 2 })).toBe(formatNumber(num, { decimals: 2 }))
            }
          ),
          { numRuns: 50 }
        )
      })
    })

    describe('Property: 辅助方法返回正确值', () => {
      it('should return correct helper values', () => {
        fc.assert(
          fc.property(
            localeArb,
            (locale) => {
              setLocale(locale)
              const { 
                getCurrencySymbol, 
                getThousandsSeparator, 
                getDecimalSeparator,
                getDatePattern
              } = useLocaleFormat()

              const config = numberFormats[locale]
              const dateConfig = dateFormats[locale]

              // Property: getCurrencySymbol 应该返回正确的货币符号
              expect(getCurrencySymbol()).toBe(config.currency)

              // Property: getThousandsSeparator 应该返回正确的千分位分隔符
              expect(getThousandsSeparator()).toBe(config.thousands)

              // Property: getDecimalSeparator 应该返回正确的小数点符号
              expect(getDecimalSeparator()).toBe(config.decimal)

              // Property: getDatePattern 应该返回正确的日期格式模式
              expect(getDatePattern('short')).toBe(dateConfig.short)
              expect(getDatePattern('long')).toBe(dateConfig.long)
            }
          ),
          { numRuns: 10 }
        )
      })
    })
  })

  // ==================== Static Format Functions Tests ====================

  describe('Static Format Functions', () => {
    describe('Property: formatDateLocale 支持显式语言参数', () => {
      it('should format dates with explicit locale parameter', () => {
        fc.assert(
          fc.property(
            dateArb,
            (date) => {
              // Property: 使用显式语言参数应该覆盖当前语言设置
              setLocale('en-US')
              const zhResult = formatDateLocale(date, 'short', 'zh-CN')
              const enResult = formatDateLocale(date, 'short', 'en-US')

              // zh-CN 格式应该是 YYYY-MM-DD
              expect(zhResult).toMatch(/^\d{4}-\d{2}-\d{2}$/)

              // en-US 格式应该是 MM/DD/YYYY
              expect(enResult).toMatch(/^\d{2}\/\d{2}\/\d{4}$/)
            }
          ),
          { numRuns: 50 }
        )
      })
    })

    describe('Property: formatNumberLocale 支持显式语言参数', () => {
      it('should format numbers with explicit locale parameter', () => {
        fc.assert(
          fc.property(
            positiveNumberArb,
            (num) => {
              // Property: 使用显式语言参数应该覆盖当前语言设置
              setLocale('en-US')
              const zhResult = formatNumberLocale(num, { currency: true, decimals: 2 }, 'zh-CN')
              const enResult = formatNumberLocale(num, { currency: true, decimals: 2 }, 'en-US')

              // zh-CN 应该使用 ¥ 符号
              expect(zhResult).toContain('¥')

              // en-US 应该使用 $ 符号
              expect(enResult).toContain('$')
            }
          ),
          { numRuns: 50 }
        )
      })
    })
  })

  // ==================== Edge Cases ====================

  describe('Edge Cases', () => {
    describe('Property: 边界日期处理', () => {
      it('should handle boundary dates correctly', () => {
        fc.assert(
          fc.property(
            localeArb,
            dateFormatArb,
            (locale, format) => {
              setLocale(locale)

              // Property: 最小日期应该正确格式化
              const minDate = new Date('1970-01-01')
              const minResult = formatDate(minDate, format)
              expect(minResult).not.toBe('')

              // Property: 最大日期应该正确格式化
              const maxDate = new Date('2099-12-31')
              const maxResult = formatDate(maxDate, format)
              expect(maxResult).not.toBe('')
            }
          ),
          { numRuns: 10 }
        )
      })
    })

    describe('Property: 边界数字处理', () => {
      it('should handle boundary numbers correctly', () => {
        fc.assert(
          fc.property(
            localeArb,
            (locale) => {
              setLocale(locale)

              // Property: 零应该正确格式化
              expect(formatNumber(0)).toBe('0')

              // Property: 负数应该正确格式化
              const negResult = formatNumber(-1234.56, { decimals: 2 })
              expect(negResult).toContain('-')

              // Property: 非常大的数字应该正确格式化
              const bigResult = formatNumber(1234567890, { useThousands: true })
              expect(bigResult).toContain(numberFormats[locale].thousands)
            }
          ),
          { numRuns: 10 }
        )
      })
    })

    describe('Property: 货币符号位置', () => {
      it('should support currency symbol position configuration', () => {
        fc.assert(
          fc.property(
            localeArb,
            positiveNumberArb,
            (locale, num) => {
              setLocale(locale)

              // Property: prefix 位置应该将符号放在前面
              const prefixResult = formatNumber(num, { 
                currency: true, 
                decimals: 2,
                currencyPosition: 'prefix'
              })
              expect(prefixResult).toMatch(/^[¥$]/)

              // Property: suffix 位置应该将符号放在后面
              const suffixResult = formatNumber(num, { 
                currency: true, 
                decimals: 2,
                currencyPosition: 'suffix'
              })
              expect(suffixResult).toMatch(/[¥$]$/)
            }
          ),
          { numRuns: 50 }
        )
      })
    })

    describe('Property: 自定义货币符号', () => {
      it('should support custom currency symbol', () => {
        fc.assert(
          fc.property(
            localeArb,
            positiveNumberArb,
            fc.constantFrom('€', '£', '₩', '₹'),
            (locale, num, symbol) => {
              setLocale(locale)

              // Property: 自定义货币符号应该被使用
              const result = formatNumber(num, { 
                currency: true, 
                decimals: 2,
                currencySymbol: symbol
              })
              expect(result).toContain(symbol)
            }
          ),
          { numRuns: 50 }
        )
      })
    })
  })

  // ==================== Statistics ====================

  describe('Property: 格式化统计', () => {
    it('should report format statistics', () => {
      console.log('\n=== Locale Format Statistics ===')
      console.log('Supported locales:', SUPPORTED_LOCALES.join(', '))
      console.log('Date format types:', DATE_FORMAT_TYPES.join(', '))
      
      for (const locale of SUPPORTED_LOCALES) {
        console.log(`\n${locale} Date Formats:`)
        console.log(`  short: ${dateFormats[locale].short}`)
        console.log(`  long: ${dateFormats[locale].long}`)
        console.log(`  datetime: ${dateFormats[locale].datetime}`)
        console.log(`  time: ${dateFormats[locale].time}`)
        
        console.log(`${locale} Number Formats:`)
        console.log(`  decimal: "${numberFormats[locale].decimal}"`)
        console.log(`  thousands: "${numberFormats[locale].thousands}"`)
        console.log(`  currency: "${numberFormats[locale].currency}"`)
      }

      // Property: 所有语言都应该有完整的格式配置
      for (const locale of SUPPORTED_LOCALES) {
        expect(dateFormats[locale]).toBeDefined()
        expect(dateFormats[locale].short).toBeDefined()
        expect(dateFormats[locale].long).toBeDefined()
        expect(dateFormats[locale].datetime).toBeDefined()
        expect(dateFormats[locale].time).toBeDefined()
        
        expect(numberFormats[locale]).toBeDefined()
        expect(numberFormats[locale].decimal).toBeDefined()
        expect(numberFormats[locale].thousands).toBeDefined()
        expect(numberFormats[locale].currency).toBeDefined()
      }
    })
  })
})

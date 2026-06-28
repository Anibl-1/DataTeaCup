/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 数据格式化属性测试
 * Data Formatter Property-Based Tests
 * 
 * **属性 67: 数值格式化一致性**
 * **Validates: Requirements 14.3**
 * 
 * 测试内容:
 * 1. 数值格式化一致性 - 相同输入和配置总是产生相同输出
 * 2. 千分位分隔符正确性
 * 3. 小数位数精度
 * 4. 百分比转换准确性 (value * 100)
 * 5. 货币符号位置
 * 6. 负数格式处理 (minus, parentheses, red, redParentheses)
 * 7. 大数缩写阈值 (K, M, B, T)
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import {
  formatNumber,
  CURRENCY_CONFIG,
} from '../dataFormatter'
import type { NumberFormat } from '@/components/StyleEngine/types/style'

// ============================================================================
// Arbitraries (测试数据生成器)
// ============================================================================

/** 生成有效的数值（排除极端值和NaN） */
const validNumberArb = fc.double({
  min: -1e12,
  max: 1e12,
  noNaN: true,
  noDefaultInfinity: true,
})

/** 生成正数 */
const positiveNumberArb = fc.double({
  min: 0.001,
  max: 1e12,
  noNaN: true,
  noDefaultInfinity: true,
})

/** 生成负数 */
const negativeNumberArb = fc.double({
  min: -1e12,
  max: -0.001,
  noNaN: true,
  noDefaultInfinity: true,
})

/** 生成整数 */
const integerArb = fc.integer({ min: -1000000000, max: 1000000000 })

/** 生成小数位数配置 */
const decimalPlacesArb = fc.integer({ min: 0, max: 6 })

/** 生成负数格式 */
const negativeFormatArb = fc.constantFrom<'minus' | 'parentheses' | 'red' | 'redParentheses'>(
  'minus',
  'parentheses',
  'red',
  'redParentheses'
)

/** 生成货币代码 */
const currencyCodeArb = fc.constantFrom(...Object.keys(CURRENCY_CONFIG))

/** 生成缩写阈值 */
const abbreviateThresholdArb = fc.constantFrom(1000, 10000, 100000, 1000000)

/** 生成前缀 */
const prefixArb = fc.constantFrom('', '约 ', '共 ', '总计: ')

/** 生成后缀 */
const suffixArb = fc.constantFrom('', ' 元', ' 个', ' 件')

/** 生成数值格式配置 */
const numberFormatConfigArb = fc.record({
  decimalPlaces: fc.option(decimalPlacesArb, { nil: undefined }),
  useThousandsSeparator: fc.option(fc.boolean(), { nil: undefined }),
  negativeFormat: fc.option(negativeFormatArb, { nil: undefined }),
  prefix: fc.option(prefixArb, { nil: undefined }),
  suffix: fc.option(suffixArb, { nil: undefined }),
  asPercentage: fc.option(fc.boolean(), { nil: undefined }),
  asCurrency: fc.option(fc.boolean(), { nil: undefined }),
  currencyCode: fc.option(currencyCodeArb, { nil: undefined }),
  abbreviate: fc.option(fc.boolean(), { nil: undefined }),
  abbreviateThreshold: fc.option(abbreviateThresholdArb, { nil: undefined }),
})

// ============================================================================
// 辅助函数
// ============================================================================

/**
 * 检查字符串是否包含千分位分隔符
 */
function hasThousandsSeparator(str: string): boolean {
  // 移除前缀、后缀、货币符号等，只检查数字部分
  const numericPart = str.replace(/[^0-9,.-]/g, '')
  return numericPart.includes(',')
}

/**
 * 计算字符串中的小数位数
 */
function getDecimalPlaces(str: string): number {
  // 移除非数字字符（保留小数点）
  const numericPart = str.replace(/[^0-9.]/g, '')
  const dotIndex = numericPart.indexOf('.')
  if (dotIndex === -1) return 0
  return numericPart.length - dotIndex - 1
}

/**
 * 检查是否使用括号格式
 */
function usesParentheses(str: string): boolean {
  return str.startsWith('(') && str.endsWith(')')
}

/**
 * 检查是否包含缩写后缀
 */
function hasAbbreviationSuffix(str: string): boolean {
  return /[KMBT]/.test(str)
}

/**
 * 获取缩写后缀
 */
function getAbbreviationSuffix(str: string): string | null {
  const match = str.match(/([KMBT])/)
  return match ? match[1] : null
}

// ============================================================================
// 属性测试
// ============================================================================

describe('Data Formatter Property Tests', () => {
  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.1: 格式化一致性
   * 相同的输入值和配置应该总是产生相同的输出
   */
  it('Property 67.1: Same input with same config should always produce same output', () => {
    fc.assert(
      fc.property(
        validNumberArb,
        numberFormatConfigArb,
        (value, config) => {
          const result1 = formatNumber(value, config)
          const result2 = formatNumber(value, config)
          const result3 = formatNumber(value, config)
          
          return result1 === result2 && result2 === result3
        }
      ),
      { numRuns: 200 }
    )
  })

  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.2: 千分位分隔符正确性
   * 当启用千分位分隔符时，大于等于1000的数值应该包含逗号分隔符
   */
  it('Property 67.2: Thousands separator should be applied correctly for numbers >= 1000', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 1000, max: 1000000000 }),
        (value) => {
          const withSeparator = formatNumber(value, { useThousandsSeparator: true })
          const withoutSeparator = formatNumber(value, { useThousandsSeparator: false })
          
          // 启用千分位时应该包含逗号
          const hasSeparator = hasThousandsSeparator(withSeparator)
          // 禁用千分位时不应该包含逗号
          const noSeparator = !hasThousandsSeparator(withoutSeparator)
          
          return hasSeparator && noSeparator
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.3: 小于1000的数值不应有千分位分隔符
   */
  it('Property 67.3: Numbers less than 1000 should not have thousands separator', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: 999 }),
        (value) => {
          const result = formatNumber(value, { useThousandsSeparator: true })
          // 小于1000的数值不应该有逗号
          return !hasThousandsSeparator(result)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.4: 小数位数精度
   * 格式化结果应该精确到指定的小数位数
   */
  it('Property 67.4: Decimal places should be exactly as specified', () => {
    fc.assert(
      fc.property(
        validNumberArb,
        decimalPlacesArb,
        (value, decimalPlaces) => {
          const result = formatNumber(value, { 
            decimalPlaces, 
            useThousandsSeparator: false,
            asPercentage: false,
            abbreviate: false
          })
          
          if (result === '') return true // null/undefined/NaN case
          
          const actualDecimalPlaces = getDecimalPlaces(result)
          return actualDecimalPlaces === decimalPlaces
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.5: 百分比转换准确性
   * 百分比格式化应该将值乘以100并添加%符号
   */
  it('Property 67.5: Percentage conversion should multiply by 100 and add % suffix', () => {
    fc.assert(
      fc.property(
        // 使用合理的数值范围，避免极小数的浮点精度问题
        fc.double({ min: 0.01, max: 10, noNaN: true, noDefaultInfinity: true }),
        fc.boolean(), // 是否为负数
        (absValue, isNegative) => {
          const value = isNegative ? -absValue : absValue
          const result = formatNumber(value, { 
            asPercentage: true, 
            decimalPlaces: 2,
            useThousandsSeparator: false
          })
          
          if (result === '') return true
          
          // 应该包含%符号
          const hasPercent = result.includes('%')
          if (!hasPercent) return false
          
          // 提取数值部分（移除负号、%符号等）
          const numericStr = result.replace(/[^0-9.]/g, '')
          const formattedValue = parseFloat(numericStr)
          const expectedValue = Math.abs(value) * 100
          
          // 允许四舍五入误差（0.01的精度）
          const isAccurate = Math.abs(formattedValue - expectedValue) < 0.015
          
          return isAccurate
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.6: 货币符号位置正确性
   * 货币符号应该根据配置放在正确的位置（前缀或后缀）
   */
  it('Property 67.6: Currency symbol should be placed correctly based on currency config', () => {
    fc.assert(
      fc.property(
        positiveNumberArb,
        currencyCodeArb,
        (value, currencyCode) => {
          const result = formatNumber(value, { 
            asCurrency: true, 
            currencyCode,
            decimalPlaces: 2
          })
          
          if (result === '') return true
          
          const currencyConfig = CURRENCY_CONFIG[currencyCode]
          if (!currencyConfig) return true
          
          const { symbol, position } = currencyConfig
          
          if (position === 'prefix') {
            // 前缀货币符号应该在开头
            return result.startsWith(symbol)
          } else {
            // 后缀货币符号应该在结尾
            return result.endsWith(symbol)
          }
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.7: 负数格式 - minus
   * minus格式应该在数值前添加负号
   */
  it('Property 67.7: Negative format "minus" should prepend minus sign', () => {
    fc.assert(
      fc.property(
        negativeNumberArb,
        (value) => {
          const result = formatNumber(value, { negativeFormat: 'minus' })
          
          if (result === '') return true
          
          // 应该以负号开头
          return result.startsWith('-')
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.8: 负数格式 - parentheses
   * parentheses格式应该用括号包围数值
   */
  it('Property 67.8: Negative format "parentheses" should wrap value in parentheses', () => {
    fc.assert(
      fc.property(
        negativeNumberArb,
        (value) => {
          const result = formatNumber(value, { negativeFormat: 'parentheses' })
          
          if (result === '') return true
          
          // 应该用括号包围
          return usesParentheses(result)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.9: 负数格式 - redParentheses
   * redParentheses格式应该用括号包围数值
   */
  it('Property 67.9: Negative format "redParentheses" should wrap value in parentheses', () => {
    fc.assert(
      fc.property(
        negativeNumberArb,
        (value) => {
          const result = formatNumber(value, { negativeFormat: 'redParentheses' })
          
          if (result === '') return true
          
          // 应该用括号包围
          return usesParentheses(result)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.10: 正数不应有负数格式
   * 正数不应该有负号或括号
   */
  it('Property 67.10: Positive numbers should not have negative formatting', () => {
    fc.assert(
      fc.property(
        positiveNumberArb,
        negativeFormatArb,
        (value, negativeFormat) => {
          const result = formatNumber(value, { negativeFormat })
          
          if (result === '') return true
          
          // 正数不应该以负号开头
          const noMinus = !result.startsWith('-')
          // 正数不应该用括号包围
          const noParentheses = !usesParentheses(result)
          
          return noMinus && noParentheses
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.11: 大数缩写 - K (千)
   * 1000-999999之间的数值应该缩写为K
   */
  it('Property 67.11: Numbers between 1K and 999K should abbreviate to K', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 10000, max: 999999 }),
        (value) => {
          const result = formatNumber(value, { 
            abbreviate: true, 
            abbreviateThreshold: 10000 
          })
          
          if (result === '') return true
          
          return getAbbreviationSuffix(result) === 'K'
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.12: 大数缩写 - M (百万)
   * 1000000-999999999之间的数值应该缩写为M
   */
  it('Property 67.12: Numbers between 1M and 999M should abbreviate to M', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 1000000, max: 999999999 }),
        (value) => {
          const result = formatNumber(value, { 
            abbreviate: true, 
            abbreviateThreshold: 10000 
          })
          
          if (result === '') return true
          
          return getAbbreviationSuffix(result) === 'M'
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.13: 大数缩写 - B (十亿)
   * 1000000000-999999999999之间的数值应该缩写为B
   */
  it('Property 67.13: Numbers between 1B and 999B should abbreviate to B', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 1000000000, max: 999999999999 }),
        (value) => {
          const result = formatNumber(value, { 
            abbreviate: true, 
            abbreviateThreshold: 10000 
          })
          
          if (result === '') return true
          
          return getAbbreviationSuffix(result) === 'B'
        }
      ),
      { numRuns: 50 }
    )
  })

  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.14: 缩写阈值正确性
   * 低于阈值的数值不应该被缩写
   */
  it('Property 67.14: Numbers below threshold should not be abbreviated', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 100, max: 9999 }),
        (value) => {
          const result = formatNumber(value, { 
            abbreviate: true, 
            abbreviateThreshold: 10000 
          })
          
          if (result === '') return true
          
          // 低于阈值不应该有缩写后缀
          return !hasAbbreviationSuffix(result)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.15: 前缀后缀正确性
   * 前缀应该在最前面，后缀应该在最后面
   */
  it('Property 67.15: Prefix should be at start, suffix should be at end', () => {
    fc.assert(
      fc.property(
        positiveNumberArb,
        prefixArb.filter(p => p !== ''),
        suffixArb.filter(s => s !== ''),
        (value, prefix, suffix) => {
          const result = formatNumber(value, { prefix, suffix })
          
          if (result === '') return true
          
          const startsWithPrefix = result.startsWith(prefix)
          const endsWithSuffix = result.endsWith(suffix)
          
          return startsWithPrefix && endsWithSuffix
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.16: 空值处理
   * null、undefined、NaN应该返回空字符串
   */
  it('Property 67.16: Null, undefined, and NaN should return empty string', () => {
    fc.assert(
      fc.property(
        fc.constantFrom(null, undefined, NaN),
        numberFormatConfigArb,
        (value, config) => {
          const result = formatNumber(value as any, config)
          return result === ''
        }
      ),
      { numRuns: 50 }
    )
  })

  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.17: 零值处理
   * 零值应该正确格式化，不应该有负号
   */
  it('Property 67.17: Zero should be formatted correctly without negative sign', () => {
    fc.assert(
      fc.property(
        negativeFormatArb,
        decimalPlacesArb,
        (negativeFormat, decimalPlaces) => {
          const result = formatNumber(0, { negativeFormat, decimalPlaces })
          
          // 零不应该有负号
          const noMinus = !result.startsWith('-')
          // 零不应该用括号包围
          const noParentheses = !usesParentheses(result)
          
          return noMinus && noParentheses
        }
      ),
      { numRuns: 50 }
    )
  })

  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.18: 货币与前缀组合
   * 货币符号应该在自定义前缀之后
   */
  it('Property 67.18: Currency symbol should come after custom prefix', () => {
    fc.assert(
      fc.property(
        positiveNumberArb,
        prefixArb.filter(p => p !== ''),
        currencyCodeArb,
        (value, prefix, currencyCode) => {
          const result = formatNumber(value, { 
            prefix, 
            asCurrency: true, 
            currencyCode,
            decimalPlaces: 2
          })
          
          if (result === '') return true
          
          const currencyConfig = CURRENCY_CONFIG[currencyCode]
          if (!currencyConfig || currencyConfig.position !== 'prefix') return true
          
          // 自定义前缀应该在货币符号之前
          const prefixIndex = result.indexOf(prefix)
          const symbolIndex = result.indexOf(currencyConfig.symbol)
          
          return prefixIndex < symbolIndex
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.19: 负数货币格式与括号
   * 负数货币使用括号格式时，货币符号应该在括号内
   */
  it('Property 67.19: Negative currency with parentheses should have symbol inside', () => {
    fc.assert(
      fc.property(
        negativeNumberArb,
        currencyCodeArb,
        (value, currencyCode) => {
          const result = formatNumber(value, { 
            asCurrency: true, 
            currencyCode,
            negativeFormat: 'parentheses',
            decimalPlaces: 2
          })
          
          if (result === '') return true
          
          const currencyConfig = CURRENCY_CONFIG[currencyCode]
          if (!currencyConfig) return true
          
          // 应该用括号包围
          if (!usesParentheses(result)) return false
          
          // 货币符号应该在括号内
          const innerContent = result.slice(1, -1)
          return innerContent.includes(currencyConfig.symbol)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 14.3**
   * 
   * 属性 67.20: 缩写值的数学正确性
   * 缩写后的数值应该在数学上正确
   */
  it('Property 67.20: Abbreviated value should be mathematically correct', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 10000, max: 100000000 }),
        (value) => {
          const result = formatNumber(value, { 
            abbreviate: true, 
            abbreviateThreshold: 10000,
            decimalPlaces: 1,
            useThousandsSeparator: false
          })
          
          if (result === '') return true
          
          // 提取数值和后缀
          const suffix = getAbbreviationSuffix(result)
          if (!suffix) return true
          
          const numericStr = result.replace(/[^0-9.]/g, '')
          const abbreviatedValue = parseFloat(numericStr)
          
          // 计算预期的缩写值
          let divisor = 1
          switch (suffix) {
            case 'K': divisor = 1000; break
            case 'M': divisor = 1000000; break
            case 'B': divisor = 1000000000; break
            case 'T': divisor = 1000000000000; break
          }
          
          const expectedValue = value / divisor
          
          // 允许四舍五入误差
          return Math.abs(abbreviatedValue - expectedValue) < 0.15
        }
      ),
      { numRuns: 100 }
    )
  })
})

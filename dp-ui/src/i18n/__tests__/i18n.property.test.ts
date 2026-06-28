/**
 * 翻译完整性属性测试
 * 
 * 测试国际化系统的翻译完整性，确保所有翻译键在所有支持的语言中都存在。
 * 
 * **Validates: Requirements 24.1**
 * THE I18n_Manager SHALL 支持中文和英文两种语言的完整翻译
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { messages, type Locale } from '../index'

// 支持的语言列表
const SUPPORTED_LOCALES: Locale[] = ['zh-CN', 'en-US']

/**
 * 递归获取对象中所有的键路径
 * @param obj 要遍历的对象
 * @param prefix 当前路径前缀
 * @returns 所有键路径的数组
 */
function getAllKeyPaths(obj: Record<string, unknown>, prefix = ''): string[] {
  const paths: string[] = []
  
  for (const key of Object.keys(obj)) {
    const currentPath = prefix ? `${prefix}.${key}` : key
    const value = obj[key]
    
    if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
      // 递归处理嵌套对象
      paths.push(...getAllKeyPaths(value as Record<string, unknown>, currentPath))
    } else {
      // 叶子节点（字符串值）
      paths.push(currentPath)
    }
  }
  
  return paths
}

/**
 * 根据键路径获取对象中的值
 * @param obj 要查询的对象
 * @param path 键路径（如 'common.confirm'）
 * @returns 对应的值，如果路径不存在则返回 undefined
 */
function getValueByPath(obj: Record<string, unknown>, path: string): unknown {
  const keys = path.split('.')
  let current: unknown = obj
  
  for (const key of keys) {
    if (current === null || typeof current !== 'object') {
      return undefined
    }
    current = (current as Record<string, unknown>)[key]
  }
  
  return current
}

/**
 * 检查值是否为有效的翻译值（非空字符串）
 */
function isValidTranslation(value: unknown): boolean {
  return typeof value === 'string' && value.length > 0
}

describe('Feature: platform-deep-optimization, 翻译完整性属性测试', () => {
  // 获取所有语言的翻译键
  const zhCNKeys = getAllKeyPaths(messages['zh-CN'] as Record<string, unknown>)
  const enUSKeys = getAllKeyPaths(messages['en-US'] as Record<string, unknown>)
  
  // 合并所有唯一的键
  const allKeys = [...new Set([...zhCNKeys, ...enUSKeys])]

  // ==================== Arbitraries ====================

  // Arbitrary for selecting a random translation key from zh-CN
  const zhCNKeyArb = fc.constantFrom(...zhCNKeys)

  // Arbitrary for selecting a random translation key from en-US
  const enUSKeyArb = fc.constantFrom(...enUSKeys)

  // Arbitrary for selecting any translation key
  const anyKeyArb = fc.constantFrom(...allKeys)

  // Arbitrary for selecting a locale
  const localeArb = fc.constantFrom<Locale>(...SUPPORTED_LOCALES)

  // ==================== Property 61: 翻译完整性 ====================

  describe('Property 61: 翻译完整性', () => {
    describe('Property 61.1: zh-CN 中的所有键在 en-US 中都存在', () => {
      it('should have all zh-CN keys present in en-US', () => {
        fc.assert(
          fc.property(
            zhCNKeyArb,
            (keyPath) => {
              const zhValue = getValueByPath(messages['zh-CN'] as Record<string, unknown>, keyPath)
              const enValue = getValueByPath(messages['en-US'] as Record<string, unknown>, keyPath)
              
              // Property: If zh-CN has a valid translation, en-US must also have it
              if (isValidTranslation(zhValue)) {
                expect(enValue).toBeDefined()
                expect(
                  isValidTranslation(enValue),
                  `Key "${keyPath}" exists in zh-CN but is missing or invalid in en-US`
                ).toBe(true)
              }
            }
          ),
          { numRuns: Math.min(zhCNKeys.length, 200) }
        )
      })
    })

    describe('Property 61.2: en-US 中的所有键在 zh-CN 中都存在', () => {
      it('should have all en-US keys present in zh-CN', () => {
        fc.assert(
          fc.property(
            enUSKeyArb,
            (keyPath) => {
              const enValue = getValueByPath(messages['en-US'] as Record<string, unknown>, keyPath)
              const zhValue = getValueByPath(messages['zh-CN'] as Record<string, unknown>, keyPath)
              
              // Property: If en-US has a valid translation, zh-CN must also have it
              if (isValidTranslation(enValue)) {
                expect(zhValue).toBeDefined()
                expect(
                  isValidTranslation(zhValue),
                  `Key "${keyPath}" exists in en-US but is missing or invalid in zh-CN`
                ).toBe(true)
              }
            }
          ),
          { numRuns: Math.min(enUSKeys.length, 200) }
        )
      })
    })

    describe('Property 61.3: 所有翻译键在所有语言中都有非空值', () => {
      it('should have non-empty translations for all keys in all locales', () => {
        fc.assert(
          fc.property(
            anyKeyArb,
            localeArb,
            (keyPath, locale) => {
              const value = getValueByPath(messages[locale] as Record<string, unknown>, keyPath)
              
              // Property: Every key should have a valid translation in every locale
              expect(value).toBeDefined()
              expect(
                isValidTranslation(value),
                `Key "${keyPath}" has invalid or empty translation in ${locale}`
              ).toBe(true)
            }
          ),
          { numRuns: Math.min(allKeys.length * 2, 300) }
        )
      })
    })

    describe('Property 61.4: 翻译键结构一致性', () => {
      it('should have consistent structure between locales', () => {
        // Property: Both locales should have the same set of keys
        const zhCNKeySet = new Set(zhCNKeys)
        const enUSKeySet = new Set(enUSKeys)
        
        // Find keys only in zh-CN
        const onlyInZhCN = zhCNKeys.filter(key => !enUSKeySet.has(key))
        // Find keys only in en-US
        const onlyInEnUS = enUSKeys.filter(key => !zhCNKeySet.has(key))
        
        expect(
          onlyInZhCN,
          `Keys only in zh-CN: ${onlyInZhCN.join(', ')}`
        ).toHaveLength(0)
        
        expect(
          onlyInEnUS,
          `Keys only in en-US: ${onlyInEnUS.join(', ')}`
        ).toHaveLength(0)
      })

      it('should have the same number of keys in both locales', () => {
        expect(zhCNKeys.length).toBe(enUSKeys.length)
      })
    })

    describe('Property 61.5: 翻译值类型一致性', () => {
      it('should have consistent value types between locales', () => {
        fc.assert(
          fc.property(
            anyKeyArb,
            (keyPath) => {
              const zhValue = getValueByPath(messages['zh-CN'] as Record<string, unknown>, keyPath)
              const enValue = getValueByPath(messages['en-US'] as Record<string, unknown>, keyPath)
              
              // Property: Both values should be of the same type
              expect(typeof zhValue).toBe(typeof enValue)
              
              // Property: If one is a string, both should be strings
              if (typeof zhValue === 'string') {
                expect(typeof enValue).toBe('string')
              }
            }
          ),
          { numRuns: Math.min(allKeys.length, 200) }
        )
      })
    })

    describe('Property 61.6: 参数占位符一致性', () => {
      it('should have consistent parameter placeholders between locales', () => {
        // 提取字符串中的参数占位符 {param}
        const extractPlaceholders = (str: string): string[] => {
          const matches = str.match(/\{(\w+)\}/g) || []
          return matches.sort()
        }

        fc.assert(
          fc.property(
            anyKeyArb,
            (keyPath) => {
              const zhValue = getValueByPath(messages['zh-CN'] as Record<string, unknown>, keyPath)
              const enValue = getValueByPath(messages['en-US'] as Record<string, unknown>, keyPath)
              
              if (typeof zhValue === 'string' && typeof enValue === 'string') {
                const zhPlaceholders = extractPlaceholders(zhValue)
                const enPlaceholders = extractPlaceholders(enValue)
                
                // Property: Both translations should have the same parameter placeholders
                expect(
                  zhPlaceholders,
                  `Key "${keyPath}" has different placeholders: zh-CN=${zhPlaceholders.join(',')} vs en-US=${enPlaceholders.join(',')}`
                ).toEqual(enPlaceholders)
              }
            }
          ),
          { numRuns: Math.min(allKeys.length, 200) }
        )
      })
    })
  })

  // ==================== Additional Property Tests ====================

  describe('Property: 顶级分类完整性', () => {
    it('should have the same top-level categories in both locales', () => {
      const zhCNTopKeys = Object.keys(messages['zh-CN'])
      const enUSTopKeys = Object.keys(messages['en-US'])
      
      expect(zhCNTopKeys.sort()).toEqual(enUSTopKeys.sort())
    })
  })

  describe('Property: 翻译键命名规范', () => {
    it('should have valid key names (no special characters except dots)', () => {
      fc.assert(
        fc.property(
          anyKeyArb,
          (keyPath) => {
            // Property: Key path should only contain alphanumeric characters, underscores, and dots
            const validKeyPattern = /^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)*$/
            expect(
              validKeyPattern.test(keyPath),
              `Invalid key path format: "${keyPath}"`
            ).toBe(true)
          }
        ),
        { numRuns: Math.min(allKeys.length, 200) }
      )
    })
  })

  describe('Property: 翻译值非空白', () => {
    it('should not have whitespace-only translations', () => {
      fc.assert(
        fc.property(
          anyKeyArb,
          localeArb,
          (keyPath, locale) => {
            const value = getValueByPath(messages[locale] as Record<string, unknown>, keyPath)
            
            if (typeof value === 'string') {
              // Property: Translation should not be whitespace-only
              expect(
                value.trim().length > 0,
                `Key "${keyPath}" in ${locale} is whitespace-only`
              ).toBe(true)
            }
          }
        ),
        { numRuns: Math.min(allKeys.length * 2, 300) }
      )
    })
  })

  describe('Property: 翻译统计', () => {
    it('should report translation statistics', () => {
      console.log('\n=== Translation Statistics ===')
      console.log(`Total unique keys: ${allKeys.length}`)
      console.log(`zh-CN keys: ${zhCNKeys.length}`)
      console.log(`en-US keys: ${enUSKeys.length}`)
      
      // Count top-level categories
      const zhCNCategories = Object.keys(messages['zh-CN'])
      console.log(`Top-level categories: ${zhCNCategories.length}`)
      console.log(`Categories: ${zhCNCategories.join(', ')}`)
      
      // Property: Statistics should be consistent
      expect(zhCNKeys.length).toBe(enUSKeys.length)
      expect(allKeys.length).toBe(zhCNKeys.length)
    })
  })
})

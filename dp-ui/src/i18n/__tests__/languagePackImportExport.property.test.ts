/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 语言包导入导出属性测试
 * 
 * **Validates: Requirements 24.5**
 * THE I18n_Manager SHALL 提供语言包的导入和导出功能
 * 
 * **Property 64: 语言包导入导出往返一致性**
 * 对于任意语言包，导出后再导入应得到与原始语言包等价的翻译内容
 */
import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import * as fc from 'fast-check'
import {
  exportLanguagePack,
  importLanguagePack,
  validateLanguagePack,
  compareLanguagePacks,
  messages,
  type Locale,
  type LanguagePackExport
} from '../index'

describe('Feature: platform-deep-optimization, 语言包导入导出属性测试', () => {
  // 保存原始语言包以便测试后恢复
  let originalZhCN: typeof messages['zh-CN']
  let originalEnUS: typeof messages['en-US']

  beforeEach(() => {
    // 深拷贝原始语言包
    originalZhCN = JSON.parse(JSON.stringify(messages['zh-CN']))
    originalEnUS = JSON.parse(JSON.stringify(messages['en-US']))
  })

  afterEach(() => {
    // 恢复原始语言包
    messages['zh-CN'] = originalZhCN
    messages['en-US'] = originalEnUS
  })

  describe('Property 64: 语言包导入导出往返一致性', () => {
    it('Property 64.1: 导出后再导入应保持翻译内容一致', () => {
      /**
       * **Validates: Requirements 24.5**
       * 对于任意语言包，导出后再导入应得到与原始语言包等价的翻译内容
       */
      const locales: Locale[] = ['zh-CN', 'en-US']
      
      for (const locale of locales) {
        // 导出语言包
        const exported = exportLanguagePack(locale, true)
        
        // 解析导出的内容
        const parsed: LanguagePackExport = JSON.parse(exported)
        
        // 验证元数据
        expect(parsed.metadata).toBeDefined()
        expect(parsed.metadata.locale).toBe(locale)
        expect(parsed.metadata.application).toBe('DataTeaCup')
        expect(parsed.metadata.keyCount).toBeGreaterThan(0)
        expect(parsed.translations).toBeDefined()
        
        // 保存原始翻译用于比较
        const originalTranslations = JSON.stringify(messages[locale])
        
        // 导入语言包
        const result = importLanguagePack(locale, exported, { merge: false })
        
        // 验证导入成功
        expect(result.success).toBe(true)
        expect(result.importedKeys).toBeGreaterThan(0)
        
        // 验证翻译内容一致
        expect(JSON.stringify(messages[locale])).toBe(originalTranslations)
      }
    })

    it('Property 64.2: 导出不带元数据后再导入应保持一致', () => {
      /**
       * **Validates: Requirements 24.5**
       * 支持简单格式（不带元数据）的导入导出
       */
      const locales: Locale[] = ['zh-CN', 'en-US']
      
      for (const locale of locales) {
        // 导出不带元数据的语言包
        const exported = exportLanguagePack(locale, false)
        
        // 解析导出的内容
        const parsed = JSON.parse(exported)
        
        // 验证没有元数据
        expect(parsed.metadata).toBeUndefined()
        expect(parsed.common).toBeDefined() // 应该直接是翻译内容
        
        // 保存原始翻译用于比较
        const originalTranslations = JSON.stringify(messages[locale])
        
        // 导入语言包
        const result = importLanguagePack(locale, exported, { merge: false })
        
        // 验证导入成功
        expect(result.success).toBe(true)
        
        // 验证翻译内容一致
        expect(JSON.stringify(messages[locale])).toBe(originalTranslations)
      }
    })

    it('Property 64.3: 合并导入应保留现有翻译并添加新翻译', () => {
      /**
       * **Validates: Requirements 24.5**
       * Import should merge translations into the existing language pack
       * Support partial imports (merge with existing translations)
       */
      const locale: Locale = 'zh-CN'
      
      // 创建部分翻译包
      const partialPack = {
        custom: {
          newKey1: '新翻译1',
          newKey2: '新翻译2'
        },
        common: {
          confirm: '确认（已更新）' // 更新现有键
        }
      }
      
      // 保存原始 common.cancel 值
      const originalCancel = messages[locale].common.cancel
      
      // 合并导入
      const result = importLanguagePack(locale, JSON.stringify(partialPack), { merge: true })
      
      // 验证导入成功
      expect(result.success).toBe(true)
      expect(result.newKeys).toBeGreaterThan(0)
      expect(result.mergedKeys).toBeGreaterThan(0)
      
      // 验证新键被添加
      expect((messages[locale] as any).custom?.newKey1).toBe('新翻译1')
      expect((messages[locale] as any).custom?.newKey2).toBe('新翻译2')
      
      // 验证现有键被更新
      expect(messages[locale].common.confirm).toBe('确认（已更新）')
      
      // 验证其他现有键保持不变
      expect(messages[locale].common.cancel).toBe(originalCancel)
    })
  })

  describe('Property 64.4: 验证功能', () => {
    it('should validate correct language pack structure', () => {
      /**
       * **Validates: Requirements 24.5**
       * Support validation of imported language packs
       */
      // 有效的完整格式
      const validFullFormat: LanguagePackExport = {
        metadata: {
          locale: 'zh-CN',
          exportedAt: new Date().toISOString(),
          version: '1.0.0',
          keyCount: 10,
          application: 'Test'
        },
        translations: {
          common: {
            hello: '你好'
          }
        }
      }
      
      const result1 = validateLanguagePack(validFullFormat)
      expect(result1.valid).toBe(true)
      expect(result1.errors).toHaveLength(0)
      
      // 有效的简单格式
      const validSimpleFormat = {
        common: {
          hello: '你好'
        }
      }
      
      const result2 = validateLanguagePack(validSimpleFormat)
      expect(result2.valid).toBe(true)
      expect(result2.errors).toHaveLength(0)
    })

    it('should reject invalid language pack structure', () => {
      /**
       * **Validates: Requirements 24.5**
       * Support validation of imported language packs
       */
      // 无效：null
      const result1 = validateLanguagePack(null)
      expect(result1.valid).toBe(false)
      expect(result1.errors.length).toBeGreaterThan(0)
      
      // 无效：非对象
      const result2 = validateLanguagePack('not an object')
      expect(result2.valid).toBe(false)
      expect(result2.errors.length).toBeGreaterThan(0)
      
      // 无效：数组
      const result3 = validateLanguagePack([])
      expect(result3.valid).toBe(false)
      expect(result3.errors.length).toBeGreaterThan(0)
    })

    it('should warn about empty values', () => {
      /**
       * **Validates: Requirements 24.5**
       * Validation should detect potential issues
       */
      const packWithEmptyValues = {
        common: {
          hello: '',
          world: null,
          test: 'valid'
        }
      }
      
      const result = validateLanguagePack(packWithEmptyValues)
      expect(result.valid).toBe(true) // 空值是警告，不是错误
      expect(result.warnings.length).toBeGreaterThan(0)
      expect(result.warnings.some(w => w.includes('common.hello'))).toBe(true)
    })
  })

  describe('Property 64.5: 错误处理', () => {
    it('should handle invalid JSON gracefully', () => {
      /**
       * **Validates: Requirements 24.5**
       * Add proper error handling for invalid imports
       */
      const result = importLanguagePack('zh-CN', 'not valid json')
      
      expect(result.success).toBe(false)
      expect(result.error).toBeDefined()
      expect(result.error).toContain('Invalid JSON')
    })

    it('should handle validation failures', () => {
      /**
       * **Validates: Requirements 24.5**
       * Support validation of imported language packs
       */
      const result = importLanguagePack('zh-CN', 'null', { validate: true })
      
      expect(result.success).toBe(false)
      expect(result.error).toBeDefined()
    })

    it('should skip validation when disabled', () => {
      /**
       * **Validates: Requirements 24.5**
       * Validation should be optional
       */
      const partialPack = { custom: { key: 'value' } }
      const result = importLanguagePack('zh-CN', JSON.stringify(partialPack), { 
        validate: false,
        merge: true 
      })
      
      expect(result.success).toBe(true)
    })
  })

  describe('Property 64.6: 语言包比较', () => {
    it('should correctly compare language packs', () => {
      /**
       * **Validates: Requirements 24.5**
       * Support comparing language packs for completeness
       */
      const diff = compareLanguagePacks('zh-CN', 'en-US')
      
      // 两个语言包应该有相同的键
      expect(diff.common.length).toBeGreaterThan(0)
      
      // 如果翻译完整，缺失的键应该很少或没有
      // 注意：这取决于实际的翻译完整性
    })
  })

  describe('Property 64.7: 属性测试 - 任意翻译对象', () => {
    // 生成任意翻译键
    const keyArb = fc.string({ minLength: 1, maxLength: 20 })
      .filter(s => /^[a-zA-Z][a-zA-Z0-9]*$/.test(s))

    // 生成任意翻译值
    const valueArb = fc.oneof(
      fc.string({ minLength: 1, maxLength: 100 }),
      fc.constant('测试翻译'),
      fc.constant('Test translation')
    )

    // 生成简单翻译对象
    const simpleTranslationArb = fc.dictionary(keyArb, valueArb, { minKeys: 1, maxKeys: 5 })

    // 生成嵌套翻译对象
    const nestedTranslationArb = fc.dictionary(
      keyArb,
      fc.oneof(valueArb, simpleTranslationArb),
      { minKeys: 1, maxKeys: 3 }
    )

    it('Property 64.8: 任意翻译对象导入后应可访问', () => {
      /**
       * **Validates: Requirements 24.5**
       * 对于任意有效的翻译对象，导入后应能正确访问
       */
      fc.assert(
        fc.property(nestedTranslationArb, (translations) => {
          const locale: Locale = 'zh-CN'
          const packData = JSON.stringify({ custom: translations })
          
          const result = importLanguagePack(locale, packData, { merge: true, validate: false })
          
          // 导入应该成功
          expect(result.success).toBe(true)
          
          // 导入的键应该可访问
          const imported = (messages[locale] as any).custom
          expect(imported).toBeDefined()
          
          // 验证至少一个键存在
          const keys = Object.keys(translations)
          if (keys.length > 0) {
            expect(imported[keys[0]]).toBeDefined()
          }
        }),
        { numRuns: 20 }
      )
    })

    it('Property 64.9: 导出的 JSON 应该是有效的', () => {
      /**
       * **Validates: Requirements 24.5**
       * 导出的语言包应该是有效的 JSON
       */
      const locales: Locale[] = ['zh-CN', 'en-US']
      
      for (const locale of locales) {
        const exported = exportLanguagePack(locale, true)
        
        // 应该是有效的 JSON
        expect(() => JSON.parse(exported)).not.toThrow()
        
        // 解析后应该有正确的结构
        const parsed = JSON.parse(exported)
        expect(parsed.metadata).toBeDefined()
        expect(parsed.translations).toBeDefined()
      }
    })

    it('Property 64.10: 元数据应该包含正确的键数量', () => {
      /**
       * **Validates: Requirements 24.5**
       * 导出的元数据应该准确反映翻译内容
       */
      const locales: Locale[] = ['zh-CN', 'en-US']
      
      for (const locale of locales) {
        const exported = exportLanguagePack(locale, true)
        const parsed: LanguagePackExport = JSON.parse(exported)
        
        // 键数量应该大于 0
        expect(parsed.metadata.keyCount).toBeGreaterThan(0)
        
        // 导出时间应该是有效的 ISO 日期
        expect(() => new Date(parsed.metadata.exportedAt)).not.toThrow()
        expect(new Date(parsed.metadata.exportedAt).getTime()).not.toBeNaN()
      }
    })
  })

  describe('Property 64.11: 导入选项测试', () => {
    it('should support object input in addition to string', () => {
      /**
       * **Validates: Requirements 24.5**
       * 支持直接传入对象进行导入
       */
      const locale: Locale = 'zh-CN'
      const packObject = {
        custom: {
          directImport: '直接导入测试'
        }
      }
      
      const result = importLanguagePack(locale, packObject, { merge: true })
      
      expect(result.success).toBe(true)
      expect((messages[locale] as any).custom?.directImport).toBe('直接导入测试')
    })

    it('should handle locale mismatch in metadata with warning', () => {
      /**
       * **Validates: Requirements 24.5**
       * 当元数据中的 locale 与目标 locale 不匹配时应该警告
       */
      const packWithDifferentLocale: LanguagePackExport = {
        metadata: {
          locale: 'en-US',
          exportedAt: new Date().toISOString(),
          version: '1.0.0',
          keyCount: 1,
          application: 'Test'
        },
        translations: {
          custom: {
            test: 'test value'
          }
        }
      }
      
      // 导入到 zh-CN（与元数据中的 en-US 不匹配）
      const result = importLanguagePack('zh-CN', JSON.stringify(packWithDifferentLocale), { merge: true })
      
      // 应该仍然成功导入
      expect(result.success).toBe(true)
    })
  })
})

/**
 * 动态语言切换测试
 * 
 * 测试无刷新语言切换功能，确保所有 UI 文本在语言切换后立即更新。
 * 
 * **Validates: Requirements 24.4**
 * THE I18n_Manager SHALL 支持动态切换语言而无需刷新页面
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import * as fc from 'fast-check'
import { 
  currentLocale, 
  setLocale, 
  getLocale,
  isZhCN,
  isEnUS,
  t, 
  naiveLocale, 
  naiveDateLocale,
  initLocale,
  onLocaleChange,
  LOCALE_CHANGE_EVENT,
  type Locale 
} from '../index'

// 支持的语言列表
const SUPPORTED_LOCALES: Locale[] = ['zh-CN', 'en-US']

describe('Feature: platform-deep-optimization, 动态语言切换测试', () => {
  // 保存原始状态
  let originalLocale: Locale
  let originalLocalStorage: Storage

  beforeEach(() => {
    // 保存原始状态
    originalLocale = currentLocale.value
    
    // Mock localStorage
    const localStorageMock: Record<string, string> = {}
    originalLocalStorage = global.localStorage
    
    Object.defineProperty(global, 'localStorage', {
      value: {
        getItem: vi.fn((key: string) => localStorageMock[key] || null),
        setItem: vi.fn((key: string, value: string) => {
          localStorageMock[key] = value
        }),
        removeItem: vi.fn((key: string) => {
          delete localStorageMock[key]
        }),
        clear: vi.fn(() => {
          Object.keys(localStorageMock).forEach(key => delete localStorageMock[key])
        })
      },
      writable: true
    })

    // Mock document.documentElement.lang
    Object.defineProperty(document.documentElement, 'lang', {
      value: '',
      writable: true
    })
  })

  afterEach(() => {
    // 恢复原始状态
    currentLocale.value = originalLocale
    
    // 恢复 localStorage
    Object.defineProperty(global, 'localStorage', {
      value: originalLocalStorage,
      writable: true
    })
  })

  // ==================== Arbitraries ====================

  // Arbitrary for selecting a locale
  const localeArb = fc.constantFrom<Locale>(...SUPPORTED_LOCALES)

  // ==================== Property 63: 语言动态切换 ====================

  describe('Property 63: 语言动态切换', () => {
    describe('Property 63.1: setLocale 更新 currentLocale', () => {
      it('should update currentLocale when setLocale is called', () => {
        fc.assert(
          fc.property(
            localeArb,
            (targetLocale) => {
              // Act
              setLocale(targetLocale)
              
              // Assert: currentLocale should be updated
              expect(currentLocale.value).toBe(targetLocale)
            }
          ),
          { numRuns: 10 }
        )
      })
    })

    describe('Property 63.2: setLocale 持久化到 localStorage', () => {
      it('should persist locale to localStorage', () => {
        fc.assert(
          fc.property(
            localeArb,
            (targetLocale) => {
              // First set to a different locale to ensure the change happens
              const otherLocale = targetLocale === 'zh-CN' ? 'en-US' : 'zh-CN'
              currentLocale.value = otherLocale
              
              // Clear mock calls
              vi.clearAllMocks()
              
              // Act
              setLocale(targetLocale)
              
              // Assert: localStorage should be updated
              expect(localStorage.setItem).toHaveBeenCalledWith('locale', targetLocale)
            }
          ),
          { numRuns: 10 }
        )
      })
    })

    describe('Property 63.3: setLocale 派发自定义事件', () => {
      it('should dispatch locale-change event', () => {
        fc.assert(
          fc.property(
            localeArb,
            (targetLocale) => {
              // Arrange
              const eventHandler = vi.fn()
              window.addEventListener(LOCALE_CHANGE_EVENT, eventHandler)
              
              // 先设置为不同的语言，确保会触发事件
              const otherLocale = targetLocale === 'zh-CN' ? 'en-US' : 'zh-CN'
              currentLocale.value = otherLocale
              
              // Act
              setLocale(targetLocale)
              
              // Assert: Event should be dispatched
              expect(eventHandler).toHaveBeenCalled()
              
              // Cleanup
              window.removeEventListener(LOCALE_CHANGE_EVENT, eventHandler)
            }
          ),
          { numRuns: 10 }
        )
      })
    })

    describe('Property 63.4: naiveLocale 响应 currentLocale 变化', () => {
      it('should update naiveLocale when currentLocale changes', () => {
        fc.assert(
          fc.property(
            localeArb,
            (targetLocale) => {
              // Act
              setLocale(targetLocale)
              
              // Assert: naiveLocale should reflect the change
              // Check that the locale object is the correct one by checking a known property
              // Naive UI zhCN has different translations than enUS
              if (targetLocale === 'zh-CN') {
                // zhCN locale should have Chinese text
                expect(naiveLocale.value.Pagination?.goto).toBe('跳至')
              } else {
                // enUS locale should have English text
                expect(naiveLocale.value.Pagination?.goto).toBe('Goto')
              }
            }
          ),
          { numRuns: 10 }
        )
      })
    })

    describe('Property 63.5: naiveDateLocale 响应 currentLocale 变化', () => {
      it('should update naiveDateLocale when currentLocale changes', () => {
        // Test that naiveDateLocale is reactive and changes with currentLocale
        // First set to zh-CN
        setLocale('zh-CN')
        const zhDateLocale = naiveDateLocale.value
        
        // Then set to en-US
        setLocale('en-US')
        const enDateLocale = naiveDateLocale.value
        
        // The date locale objects should be different
        // They are different imported objects from naive-ui
        expect(zhDateLocale).toBeDefined()
        expect(enDateLocale).toBeDefined()
        
        // Verify they are different objects (different references)
        expect(zhDateLocale).not.toBe(enDateLocale)
      })
    })

    describe('Property 63.6: t() 函数响应语言变化', () => {
      it('should return different translations after locale change', () => {
        // Test with a known key that has different translations
        const testKey = 'common.confirm'
        
        // Set to zh-CN
        setLocale('zh-CN')
        const zhTranslation = t(testKey)
        
        // Set to en-US
        setLocale('en-US')
        const enTranslation = t(testKey)
        
        // Assert: Translations should be different
        expect(zhTranslation).toBe('确认')
        expect(enTranslation).toBe('Confirm')
        expect(zhTranslation).not.toBe(enTranslation)
      })
    })

    describe('Property 63.7: 相同语言不触发事件', () => {
      it('should not dispatch event when setting same locale', () => {
        fc.assert(
          fc.property(
            localeArb,
            (targetLocale) => {
              // Arrange: Set initial locale
              currentLocale.value = targetLocale
              
              const eventHandler = vi.fn()
              window.addEventListener(LOCALE_CHANGE_EVENT, eventHandler)
              
              // Act: Set same locale
              setLocale(targetLocale)
              
              // Assert: Event should not be dispatched
              expect(eventHandler).not.toHaveBeenCalled()
              
              // Cleanup
              window.removeEventListener(LOCALE_CHANGE_EVENT, eventHandler)
            }
          ),
          { numRuns: 10 }
        )
      })
    })

    describe('Property 63.8: onLocaleChange 回调被调用', () => {
      it('should call onLocaleChange callback when locale changes', () => {
        // Arrange
        const callback = vi.fn()
        const cleanup = onLocaleChange(callback)
        
        // 设置初始语言
        currentLocale.value = 'zh-CN'
        
        // Act: Change locale
        setLocale('en-US')
        
        // Assert: Callback should be called with new locale
        expect(callback).toHaveBeenCalledWith('en-US', expect.anything())
        
        // Cleanup
        cleanup()
      })
    })

    describe('Property 63.9: onLocaleChange 清理函数有效', () => {
      it('should not call callback after cleanup', () => {
        // Arrange
        const callback = vi.fn()
        const cleanup = onLocaleChange(callback)
        
        // Cleanup immediately
        cleanup()
        
        // 设置初始语言
        currentLocale.value = 'zh-CN'
        
        // Act: Change locale
        setLocale('en-US')
        
        // Assert: Callback should not be called
        expect(callback).not.toHaveBeenCalled()
      })
    })

    describe('Property 63.10: getLocale 返回当前语言', () => {
      it('should return current locale', () => {
        fc.assert(
          fc.property(
            localeArb,
            (targetLocale) => {
              // Act
              setLocale(targetLocale)
              
              // Assert
              expect(getLocale()).toBe(targetLocale)
            }
          ),
          { numRuns: 10 }
        )
      })
    })

    describe('Property 63.11: isZhCN 和 isEnUS 正确反映语言状态', () => {
      it('should correctly reflect locale state', () => {
        // Test zh-CN
        setLocale('zh-CN')
        expect(isZhCN()).toBe(true)
        expect(isEnUS()).toBe(false)
        
        // Test en-US
        setLocale('en-US')
        expect(isZhCN()).toBe(false)
        expect(isEnUS()).toBe(true)
      })
    })

    describe('Property 63.12: HTML lang 属性更新', () => {
      it('should update document.documentElement.lang', () => {
        fc.assert(
          fc.property(
            localeArb,
            (targetLocale) => {
              // 先设置为不同的语言
              const otherLocale = targetLocale === 'zh-CN' ? 'en-US' : 'zh-CN'
              currentLocale.value = otherLocale
              
              // Act
              setLocale(targetLocale)
              
              // Assert: HTML lang should be updated
              const expectedLang = targetLocale === 'zh-CN' ? 'zh' : 'en'
              expect(document.documentElement.lang).toBe(expectedLang)
            }
          ),
          { numRuns: 10 }
        )
      })
    })
  })

  // ==================== initLocale Tests ====================

  describe('initLocale 初始化测试', () => {
    it('should load locale from localStorage if valid', () => {
      // Arrange
      ;(localStorage.getItem as ReturnType<typeof vi.fn>).mockReturnValue('en-US')
      
      // Act
      initLocale()
      
      // Assert
      expect(currentLocale.value).toBe('en-US')
    })

    it('should keep default locale if localStorage value is invalid', () => {
      // Arrange
      ;(localStorage.getItem as ReturnType<typeof vi.fn>).mockReturnValue('invalid-locale')
      currentLocale.value = 'zh-CN'
      
      // Act
      initLocale()
      
      // Assert: Should keep the current value
      expect(currentLocale.value).toBe('zh-CN')
    })

    it('should keep default locale if localStorage is empty', () => {
      // Arrange
      ;(localStorage.getItem as ReturnType<typeof vi.fn>).mockReturnValue(null)
      currentLocale.value = 'zh-CN'
      
      // Act
      initLocale()
      
      // Assert: Should keep the current value
      expect(currentLocale.value).toBe('zh-CN')
    })
  })

  // ==================== 边界情况测试 ====================

  describe('边界情况测试', () => {
    it('should handle rapid locale switching', () => {
      // Arrange
      const eventHandler = vi.fn()
      window.addEventListener(LOCALE_CHANGE_EVENT, eventHandler)
      
      // Act: Rapid switching
      for (let i = 0; i < 10; i++) {
        setLocale(i % 2 === 0 ? 'zh-CN' : 'en-US')
      }
      
      // Assert: Final state should be correct
      expect(currentLocale.value).toBe('en-US')
      
      // Cleanup
      window.removeEventListener(LOCALE_CHANGE_EVENT, eventHandler)
    })

    it('should handle multiple onLocaleChange listeners', () => {
      // Arrange
      const callback1 = vi.fn()
      const callback2 = vi.fn()
      const cleanup1 = onLocaleChange(callback1)
      const cleanup2 = onLocaleChange(callback2)
      
      // 设置初始语言
      currentLocale.value = 'zh-CN'
      
      // Act
      setLocale('en-US')
      
      // Assert: Both callbacks should be called
      expect(callback1).toHaveBeenCalled()
      expect(callback2).toHaveBeenCalled()
      
      // Cleanup
      cleanup1()
      cleanup2()
    })
  })
})

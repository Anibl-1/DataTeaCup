/**
 * 错误消息属性测试
 * 
 * Property 57: 错误解决建议 - 对于任意常见错误类型，应提供至少一条解决建议。
 * 
 * **Validates: Requirements 18.2**
 * THE Error_Handler SHALL 为常见错误提供解决方案建议
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import {
  errorMessages,
  httpStatusMessages,
  ErrorCategory,
  getErrorMessage,
  getHttpStatusMessage,
  getSuggestions,
  getHttpStatusSuggestions,
  getAllErrorCodes,
  getAllCategories,
  getErrorCodesByCategory,
  getFriendlyMessage,
  getFullErrorInfo,
  hasErrorMessage,
  type ErrorMessageConfig
} from '../errorMessages'

describe('Feature: platform-deep-optimization, Property 57: 错误解决建议', () => {
  /**
   * Property 57: 错误解决建议
   * 
   * **Validates: Requirements 18.2**
   * 
   * 对于任意常见错误类型，应提供至少一条解决建议。
   */
  describe('Property 57: 错误解决建议 - 核心属性测试', () => {
    // Get all error codes from the errorMessages mapping
    const allErrorCodes = getAllErrorCodes()
    
    // Arbitrary for selecting any error code from the mapping
    const errorCodeArb = fc.constantFrom(...allErrorCodes)
    
    // Get all HTTP status codes from the httpStatusMessages mapping
    const allHttpStatusCodes = Object.keys(httpStatusMessages).map(Number)
    
    // Arbitrary for selecting any HTTP status code from the mapping
    const httpStatusArb = fc.constantFrom(...allHttpStatusCodes)
    
    // Arbitrary for selecting any error category
    const categoryArb = fc.constantFrom(...getAllCategories())

    it('should provide at least one suggestion for every error code', () => {
      fc.assert(
        fc.property(errorCodeArb, (errorCode) => {
          const config = getErrorMessage(errorCode)
          
          // Config should exist
          expect(config).toBeDefined()
          
          // Suggestions should be an array with at least one item
          expect(config!.suggestions).toBeDefined()
          expect(Array.isArray(config!.suggestions)).toBe(true)
          expect(config!.suggestions.length).toBeGreaterThanOrEqual(1)
          
          // Each suggestion should be a non-empty string
          config!.suggestions.forEach(suggestion => {
            expect(typeof suggestion).toBe('string')
            expect(suggestion.length).toBeGreaterThan(0)
          })
        }),
        { numRuns: 100 }
      )
    })

    it('should provide at least one suggestion for every HTTP status code', () => {
      fc.assert(
        fc.property(httpStatusArb, (statusCode) => {
          const config = getHttpStatusMessage(statusCode)
          
          // Config should exist
          expect(config).toBeDefined()
          
          // Suggestions should be an array with at least one item
          expect(config!.suggestions).toBeDefined()
          expect(Array.isArray(config!.suggestions)).toBe(true)
          expect(config!.suggestions.length).toBeGreaterThanOrEqual(1)
          
          // Each suggestion should be a non-empty string
          config!.suggestions.forEach(suggestion => {
            expect(typeof suggestion).toBe('string')
            expect(suggestion.length).toBeGreaterThan(0)
          })
        }),
        { numRuns: 100 }
      )
    })

    it('should return suggestions via getSuggestions for every error code', () => {
      fc.assert(
        fc.property(errorCodeArb, (errorCode) => {
          const suggestions = getSuggestions(errorCode)
          
          // Should return an array
          expect(Array.isArray(suggestions)).toBe(true)
          
          // Should have at least one suggestion
          expect(suggestions.length).toBeGreaterThanOrEqual(1)
          
          // All suggestions should be non-empty strings
          suggestions.forEach(suggestion => {
            expect(typeof suggestion).toBe('string')
            expect(suggestion.trim().length).toBeGreaterThan(0)
          })
        }),
        { numRuns: 100 }
      )
    })

    it('should return suggestions via getHttpStatusSuggestions for every HTTP status', () => {
      fc.assert(
        fc.property(httpStatusArb, (statusCode) => {
          const suggestions = getHttpStatusSuggestions(statusCode)
          
          // Should return an array
          expect(Array.isArray(suggestions)).toBe(true)
          
          // Should have at least one suggestion
          expect(suggestions.length).toBeGreaterThanOrEqual(1)
          
          // All suggestions should be non-empty strings
          suggestions.forEach(suggestion => {
            expect(typeof suggestion).toBe('string')
            expect(suggestion.trim().length).toBeGreaterThan(0)
          })
        }),
        { numRuns: 100 }
      )
    })

    it('should provide a user-friendly message for every error code', () => {
      fc.assert(
        fc.property(errorCodeArb, (errorCode) => {
          const config = getErrorMessage(errorCode)
          
          // Message should exist and be non-empty
          expect(config!.message).toBeDefined()
          expect(typeof config!.message).toBe('string')
          expect(config!.message.length).toBeGreaterThan(0)
        }),
        { numRuns: 100 }
      )
    })

    it('should provide a user-friendly message for every HTTP status code', () => {
      fc.assert(
        fc.property(httpStatusArb, (statusCode) => {
          const config = getHttpStatusMessage(statusCode)
          
          // Message should exist and be non-empty
          expect(config!.message).toBeDefined()
          expect(typeof config!.message).toBe('string')
          expect(config!.message.length).toBeGreaterThan(0)
        }),
        { numRuns: 100 }
      )
    })

    it('should have at least one error code for each error category', () => {
      fc.assert(
        fc.property(categoryArb, (category) => {
          const errorCodes = getErrorCodesByCategory(category)
          
          // Each category should have at least one error code
          expect(Array.isArray(errorCodes)).toBe(true)
          expect(errorCodes.length).toBeGreaterThanOrEqual(1)
        }),
        { numRuns: 100 }
      )
    })

    it('should return complete error info with message and suggestions via getFullErrorInfo', () => {
      fc.assert(
        fc.property(errorCodeArb, (errorCode) => {
          const info = getFullErrorInfo(errorCode)
          
          // Should have message
          expect(info.message).toBeDefined()
          expect(typeof info.message).toBe('string')
          expect(info.message.length).toBeGreaterThan(0)
          
          // Should have suggestions array with at least one item
          expect(Array.isArray(info.suggestions)).toBe(true)
          expect(info.suggestions.length).toBeGreaterThanOrEqual(1)
        }),
        { numRuns: 100 }
      )
    })

    it('should correctly identify existing error codes via hasErrorMessage', () => {
      fc.assert(
        fc.property(errorCodeArb, (errorCode) => {
          expect(hasErrorMessage(errorCode)).toBe(true)
        }),
        { numRuns: 100 }
      )
    })

    it('should return friendly message via getFriendlyMessage for every error code', () => {
      fc.assert(
        fc.property(errorCodeArb, (errorCode) => {
          const message = getFriendlyMessage(errorCode)
          
          expect(typeof message).toBe('string')
          expect(message.length).toBeGreaterThan(0)
        }),
        { numRuns: 100 }
      )
    })
  })

  describe('Property 57: 错误解决建议 - 边界条件测试', () => {
    it('should return empty array for unknown error codes via getSuggestions', () => {
      const unknownCodes = ['UNKNOWN_CODE_123', 'RANDOM_ERROR', 'NOT_EXIST']
      
      fc.assert(
        fc.property(fc.constantFrom(...unknownCodes), (unknownCode) => {
          const suggestions = getSuggestions(unknownCode)
          
          // Should return empty array for unknown codes
          expect(Array.isArray(suggestions)).toBe(true)
          expect(suggestions.length).toBe(0)
        }),
        { numRuns: 10 }
      )
    })

    it('should return empty array for unknown HTTP status codes via getHttpStatusSuggestions', () => {
      // Status codes not in the mapping
      const unknownStatuses = [199, 299, 399, 418, 451, 599]
      
      fc.assert(
        fc.property(fc.constantFrom(...unknownStatuses), (statusCode) => {
          const suggestions = getHttpStatusSuggestions(statusCode)
          
          // Should return empty array for unknown status codes
          expect(Array.isArray(suggestions)).toBe(true)
          expect(suggestions.length).toBe(0)
        }),
        { numRuns: 10 }
      )
    })

    it('should return fallback message for unknown error codes via getFriendlyMessage', () => {
      const unknownCodes = ['UNKNOWN_CODE_XYZ', 'RANDOM_ERROR_ABC']
      const fallbackMessage = '默认错误消息'
      
      fc.assert(
        fc.property(fc.constantFrom(...unknownCodes), (unknownCode) => {
          const message = getFriendlyMessage(unknownCode, fallbackMessage)
          
          expect(message).toBe(fallbackMessage)
        }),
        { numRuns: 10 }
      )
    })

    it('should return default fallback when no fallback provided for unknown codes', () => {
      const unknownCodes = ['UNKNOWN_CODE_999', 'NONEXISTENT_ERROR']
      
      fc.assert(
        fc.property(fc.constantFrom(...unknownCodes), (unknownCode) => {
          const message = getFriendlyMessage(unknownCode)
          
          // Should return default fallback message
          expect(message).toBe('操作失败，请稍后重试')
        }),
        { numRuns: 10 }
      )
    })

    it('should return default suggestions for unknown codes via getFullErrorInfo', () => {
      const unknownCodes = ['UNKNOWN_ERROR_CODE', 'RANDOM_CODE']
      
      fc.assert(
        fc.property(fc.constantFrom(...unknownCodes), (unknownCode) => {
          const info = getFullErrorInfo(unknownCode)
          
          // Should return default message
          expect(info.message).toBe('操作失败，请稍后重试')
          
          // Should return default suggestions
          expect(info.suggestions.length).toBeGreaterThanOrEqual(1)
          expect(info.suggestions).toContain('请稍后重试')
        }),
        { numRuns: 10 }
      )
    })
  })

  describe('Property 57: 错误解决建议 - 数据完整性测试', () => {
    it('should have valid category for all error codes with category defined', () => {
      const allErrorCodes = getAllErrorCodes()
      const validCategories = Object.values(ErrorCategory)
      
      fc.assert(
        fc.property(fc.constantFrom(...allErrorCodes), (errorCode) => {
          const config = getErrorMessage(errorCode)
          
          if (config?.category !== undefined) {
            expect(validCategories).toContain(config.category)
          }
        }),
        { numRuns: 100 }
      )
    })

    it('should have valid category for all HTTP status codes with category defined', () => {
      const allHttpStatusCodes = Object.keys(httpStatusMessages).map(Number)
      const validCategories = Object.values(ErrorCategory)
      
      fc.assert(
        fc.property(fc.constantFrom(...allHttpStatusCodes), (statusCode) => {
          const config = getHttpStatusMessage(statusCode)
          
          if (config?.category !== undefined) {
            expect(validCategories).toContain(config.category)
          }
        }),
        { numRuns: 100 }
      )
    })

    it('should have non-negative retry delay for retryable errors', () => {
      const allErrorCodes = getAllErrorCodes()
      
      fc.assert(
        fc.property(fc.constantFrom(...allErrorCodes), (errorCode) => {
          const config = getErrorMessage(errorCode)
          
          if (config?.retryable && config?.retryDelay !== undefined) {
            expect(config.retryDelay).toBeGreaterThanOrEqual(0)
          }
        }),
        { numRuns: 100 }
      )
    })

    it('should have consistent structure for all error message configs', () => {
      const allErrorCodes = getAllErrorCodes()
      
      fc.assert(
        fc.property(fc.constantFrom(...allErrorCodes), (errorCode) => {
          const config = errorMessages[errorCode]
          
          // Required fields
          expect(config).toHaveProperty('message')
          expect(config).toHaveProperty('suggestions')
          
          // Type checks
          expect(typeof config.message).toBe('string')
          expect(Array.isArray(config.suggestions)).toBe(true)
          
          // Optional fields type checks
          if (config.category !== undefined) {
            expect(typeof config.category).toBe('string')
          }
          if (config.retryable !== undefined) {
            expect(typeof config.retryable).toBe('boolean')
          }
          if (config.retryDelay !== undefined) {
            expect(typeof config.retryDelay).toBe('number')
          }
        }),
        { numRuns: 100 }
      )
    })

    it('should have consistent structure for all HTTP status message configs', () => {
      const allHttpStatusCodes = Object.keys(httpStatusMessages).map(Number)
      
      fc.assert(
        fc.property(fc.constantFrom(...allHttpStatusCodes), (statusCode) => {
          const config = httpStatusMessages[statusCode]
          
          // Required fields
          expect(config).toHaveProperty('message')
          expect(config).toHaveProperty('suggestions')
          
          // Type checks
          expect(typeof config.message).toBe('string')
          expect(Array.isArray(config.suggestions)).toBe(true)
        }),
        { numRuns: 100 }
      )
    })
  })

  describe('Property 57: 错误解决建议 - 覆盖率验证', () => {
    it('should cover all defined error categories', () => {
      const allCategories = getAllCategories()
      const coveredCategories = new Set<ErrorCategory>()
      
      // Collect all categories from error messages
      Object.values(errorMessages).forEach(config => {
        if (config.category) {
          coveredCategories.add(config.category)
        }
      })
      
      // All categories should be covered
      allCategories.forEach(category => {
        expect(coveredCategories.has(category)).toBe(true)
      })
    })

    it('should have error codes for common HTTP error scenarios', () => {
      // Common HTTP status codes that should be covered
      const commonStatusCodes = [400, 401, 403, 404, 500, 502, 503, 504]
      
      commonStatusCodes.forEach(statusCode => {
        const config = getHttpStatusMessage(statusCode)
        expect(config).toBeDefined()
        expect(config!.suggestions.length).toBeGreaterThanOrEqual(1)
      })
    })

    it('should have error codes for common error scenarios', () => {
      // Common error codes that should be covered
      const commonErrorCodes = [
        'NETWORK_ERROR',
        'AUTH_FAILED',
        'TOKEN_EXPIRED',
        'PERMISSION_DENIED',
        'DATA_SOURCE_CONNECTION_FAILED',
        'SQL_SYNTAX_ERROR',
        'REPORT_NOT_FOUND',
        'EXPORT_FAILED',
        'INTERNAL_SERVER_ERROR',
        'VALIDATION_ERROR'
      ]
      
      commonErrorCodes.forEach(errorCode => {
        expect(hasErrorMessage(errorCode)).toBe(true)
        const suggestions = getSuggestions(errorCode)
        expect(suggestions.length).toBeGreaterThanOrEqual(1)
      })
    })
  })
})

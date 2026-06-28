/**
 * 统一错误处理器属性测试
 * 
 * Property 56: 错误消息友好转换 - 对于任意技术错误，应转换为用户可理解的友好提示消息。
 * 
 * **Validates: Requirements 18.1**
 * THE Error_Handler SHALL 将技术错误信息转换为用户可理解的友好提示
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as fc from 'fast-check'
import {
  errorHandler,
  generateTraceId,
  ErrorType,
  ErrorSeverity,
  type ApiError
} from '../errorHandler'

// Mock logger
vi.mock('../logger', () => ({
  logger: {
    error: vi.fn(),
    warn: vi.fn(),
    info: vi.fn(),
    debug: vi.fn()
  }
}))

describe('Feature: platform-deep-optimization, Property 56: 错误消息友好转换', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  /**
   * Property 56: 错误消息友好转换
   * 
   * **Validates: Requirements 18.1**
   * 
   * 对于任意技术错误，应转换为用户可理解的友好提示消息。
   */
  describe('Property 56: 错误消息友好转换 - 核心属性测试', () => {
    // Arbitrary for generating random error codes
    const errorCodeArb = fc.oneof(
      fc.constant('ERR_NETWORK'),
      fc.constant('ECONNABORTED'),
      fc.constant('ETIMEDOUT'),
      fc.constant('AUTH_FAILED'),
      fc.constant('TOKEN_EXPIRED'),
      fc.constant('PERMISSION_DENIED'),
      fc.constant('DATA_SOURCE_CONNECTION_FAILED'),
      fc.constant('SQL_SYNTAX_ERROR'),
      fc.constant('EXPORT_FAILED'),
      fc.constant('INTERNAL_SERVER_ERROR'),
      fc.string({ minLength: 1, maxLength: 50 }) // Random unknown codes
    )

    // Arbitrary for generating HTTP status codes
    const httpStatusArb = fc.oneof(
      fc.constant(400),
      fc.constant(401),
      fc.constant(403),
      fc.constant(404),
      fc.constant(408),
      fc.constant(429),
      fc.constant(500),
      fc.constant(502),
      fc.constant(503),
      fc.constant(504),
      fc.integer({ min: 100, max: 599 }) // Random HTTP status
    )

    // Arbitrary for generating technical error messages
    const technicalMessageArb = fc.oneof(
      fc.constant('Connection refused'),
      fc.constant('ECONNRESET'),
      fc.constant('java.sql.SQLException: Connection timeout'),
      fc.constant('NullPointerException at com.example.Service.method'),
      fc.constant('ORA-00942: table or view does not exist'),
      fc.constant('FATAL ERROR: CALL_AND_RETRY_LAST Allocation failed'),
      fc.string({ minLength: 1, maxLength: 200 })
    )

    // Arbitrary for generating API errors
    const apiErrorArb = fc.record({
      code: fc.option(errorCodeArb, { nil: undefined }),
      message: fc.option(technicalMessageArb, { nil: undefined }),
      status: fc.option(httpStatusArb, { nil: undefined }),
      response: fc.option(
        fc.record({
          status: fc.option(httpStatusArb, { nil: undefined }),
          data: fc.option(
            fc.record({
              code: fc.option(errorCodeArb, { nil: undefined }),
              msg: fc.option(technicalMessageArb, { nil: undefined }),
              message: fc.option(technicalMessageArb, { nil: undefined })
            }),
            { nil: undefined }
          )
        }),
        { nil: undefined }
      )
    }) as fc.Arbitrary<ApiError>

    // Arbitrary for generating standard Error objects
    const standardErrorArb = technicalMessageArb.map(msg => new Error(msg))

    // Combined arbitrary for any error type
    const anyErrorArb = fc.oneof(
      apiErrorArb,
      standardErrorArb,
      technicalMessageArb,
      fc.constant(null),
      fc.constant(undefined)
    )

    it('should always return a non-empty user-friendly message for any error', () => {
      fc.assert(
        fc.property(anyErrorArb, (error) => {
          const result = errorHandler.handle(error)
          
          // User message should always be defined and non-empty
          expect(result.userMessage).toBeDefined()
          expect(typeof result.userMessage).toBe('string')
          expect(result.userMessage.length).toBeGreaterThan(0)
        }),
        { numRuns: 100 }
      )
    })

    it('should always return a valid ErrorInfo structure for any error', () => {
      fc.assert(
        fc.property(anyErrorArb, (error) => {
          const result = errorHandler.handle(error)
          
          // All required fields should be present
          expect(result).toHaveProperty('code')
          expect(result).toHaveProperty('message')
          expect(result).toHaveProperty('userMessage')
          expect(result).toHaveProperty('suggestions')
          expect(result).toHaveProperty('traceId')
          expect(result).toHaveProperty('timestamp')
          expect(result).toHaveProperty('type')
          expect(result).toHaveProperty('severity')
          
          // Types should be correct
          expect(typeof result.code).toBe('string')
          expect(typeof result.message).toBe('string')
          expect(typeof result.userMessage).toBe('string')
          expect(Array.isArray(result.suggestions)).toBe(true)
          expect(typeof result.traceId).toBe('string')
          expect(typeof result.timestamp).toBe('number')
        }),
        { numRuns: 100 }
      )
    })

    it('should always generate a unique trace ID for each error', () => {
      fc.assert(
        fc.property(anyErrorArb, anyErrorArb, (error1, error2) => {
          const result1 = errorHandler.handle(error1)
          const result2 = errorHandler.handle(error2)
          
          // Each error should have a unique trace ID
          expect(result1.traceId).not.toBe(result2.traceId)
        }),
        { numRuns: 50 }
      )
    })

    it('should always return suggestions as an array', () => {
      fc.assert(
        fc.property(anyErrorArb, (error) => {
          const result = errorHandler.handle(error)
          
          expect(Array.isArray(result.suggestions)).toBe(true)
          result.suggestions.forEach(suggestion => {
            expect(typeof suggestion).toBe('string')
          })
        }),
        { numRuns: 100 }
      )
    })

    it('should always return a valid error type', () => {
      const validTypes = Object.values(ErrorType)
      
      fc.assert(
        fc.property(anyErrorArb, (error) => {
          const result = errorHandler.handle(error)
          
          expect(validTypes).toContain(result.type)
        }),
        { numRuns: 100 }
      )
    })

    it('should always return a valid severity level', () => {
      const validSeverities = Object.values(ErrorSeverity)
      
      fc.assert(
        fc.property(anyErrorArb, (error) => {
          const result = errorHandler.handle(error)
          
          expect(validSeverities).toContain(result.severity)
        }),
        { numRuns: 100 }
      )
    })

    it('should always return a timestamp close to current time', () => {
      fc.assert(
        fc.property(anyErrorArb, (error) => {
          const before = Date.now()
          const result = errorHandler.handle(error)
          const after = Date.now()
          
          expect(result.timestamp).toBeGreaterThanOrEqual(before)
          expect(result.timestamp).toBeLessThanOrEqual(after)
        }),
        { numRuns: 100 }
      )
    })

    it('should convert technical messages to user-friendly Chinese messages', () => {
      // Technical error patterns that should be converted
      const technicalPatterns = [
        'java.sql.SQLException',
        'NullPointerException',
        'Connection refused',
        'ECONNRESET',
        'ORA-',
        'FATAL ERROR'
      ]
      
      fc.assert(
        fc.property(
          fc.constantFrom(...technicalPatterns).chain(pattern => 
            fc.constant(new Error(`${pattern}: some technical details`))
          ),
          (error) => {
            const result = errorHandler.handle(error)
            
            // User message should not contain technical jargon
            technicalPatterns.forEach(pattern => {
              expect(result.userMessage).not.toContain(pattern)
            })
            
            // User message should be in Chinese or a friendly format
            expect(result.userMessage.length).toBeGreaterThan(0)
          }
        ),
        { numRuns: 50 }
      )
    })

    it('should handle HTTP status codes consistently', () => {
      fc.assert(
        fc.property(httpStatusArb, (status) => {
          const error: ApiError = {
            response: {
              status,
              data: { msg: 'Error message' }
            }
          }
          
          const result = errorHandler.handle(error)
          
          // Should always produce a valid result
          expect(result.userMessage).toBeDefined()
          expect(result.userMessage.length).toBeGreaterThan(0)
          
          // Error type should match status code category
          if (status === 401) {
            expect(result.type).toBe(ErrorType.AUTH)
          } else if (status === 403) {
            expect(result.type).toBe(ErrorType.PERMISSION)
          } else if (status === 404) {
            expect(result.type).toBe(ErrorType.NOT_FOUND)
          } else if (status === 429) {
            expect(result.type).toBe(ErrorType.RATE_LIMIT)
          } else if (status === 504 || status === 408) {
            // 504 Gateway Timeout and 408 Request Timeout are timeout errors
            expect(result.type).toBe(ErrorType.TIMEOUT)
          } else if (status >= 500) {
            expect(result.type).toBe(ErrorType.SERVER)
          }
        }),
        { numRuns: 100 }
      )
    })

    it('should preserve context information when provided', () => {
      const contextArb = fc.record({
        userId: fc.option(fc.integer(), { nil: undefined }),
        action: fc.option(fc.string(), { nil: undefined }),
        resource: fc.option(fc.string(), { nil: undefined })
      })
      
      fc.assert(
        fc.property(anyErrorArb, contextArb, (error, context) => {
          const result = errorHandler.handle(error, context)
          
          expect(result.context).toEqual(context)
        }),
        { numRuns: 50 }
      )
    })
  })

  describe('generateTraceId properties', () => {
    it('should always generate unique IDs', () => {
      fc.assert(
        fc.property(fc.integer({ min: 1, max: 100 }), (count) => {
          const ids = new Set<string>()
          
          for (let i = 0; i < count; i++) {
            ids.add(generateTraceId())
          }
          
          // All IDs should be unique
          expect(ids.size).toBe(count)
        }),
        { numRuns: 20 }
      )
    })

    it('should always match the expected format', () => {
      fc.assert(
        fc.property(fc.constant(null), () => {
          const traceId = generateTraceId()
          
          // Format: ERR-{timestamp}-{counter}-{random}
          expect(traceId).toMatch(/^ERR-[A-Z0-9]+-[A-Z0-9]{2,}-[A-Z0-9]{6}$/)
        }),
        { numRuns: 100 }
      )
    })
  })

  describe('error type detection properties', () => {
    it('should correctly identify network errors', () => {
      const networkCodes = ['ERR_NETWORK', 'ECONNREFUSED']
      
      fc.assert(
        fc.property(fc.constantFrom(...networkCodes), (code) => {
          const error: ApiError = { code }
          
          expect(errorHandler.isNetworkError(error)).toBe(true)
        }),
        { numRuns: 10 }
      )
    })

    it('should correctly identify timeout errors', () => {
      const timeoutCodes = ['ECONNABORTED', 'ETIMEDOUT']
      
      fc.assert(
        fc.property(fc.constantFrom(...timeoutCodes), (code) => {
          const error: ApiError = { code }
          
          expect(errorHandler.isTimeoutError(error)).toBe(true)
        }),
        { numRuns: 10 }
      )
    })

    it('should correctly identify retryable errors', () => {
      // Network, timeout, and server errors should be retryable
      const retryableErrors: ApiError[] = [
        { code: 'ERR_NETWORK' },
        { code: 'ECONNABORTED' },
        { response: { status: 500 } },
        { response: { status: 503 } }
      ]
      
      fc.assert(
        fc.property(fc.constantFrom(...retryableErrors), (error) => {
          expect(errorHandler.isRetryable(error)).toBe(true)
        }),
        { numRuns: 10 }
      )
    })

    it('should correctly identify non-retryable errors', () => {
      // Auth, permission, validation errors should not be retryable
      const nonRetryableErrors: ApiError[] = [
        { response: { status: 401 } },
        { response: { status: 403 } },
        { response: { status: 400 } },
        { response: { status: 404 } }
      ]
      
      fc.assert(
        fc.property(fc.constantFrom(...nonRetryableErrors), (error) => {
          expect(errorHandler.isRetryable(error)).toBe(false)
        }),
        { numRuns: 10 }
      )
    })
  })
})
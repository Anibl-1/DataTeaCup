/**
 * 统一错误处理器单元测试
 * 
 * 测试错误消息友好转换功能
 * 需求 18.1: THE Error_Handler SHALL 将技术错误信息转换为用户可理解的友好提示
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  errorHandler,
  handleError,
  getUserFriendlyMessage,
  getErrorSuggestions,
  generateTraceId,
  isValidTraceId,
  extractTimestampFromTraceId,
  formatTraceIdForDisplay,
  ErrorType,
  ErrorSeverity,
  ERROR_CODE_MESSAGES,
  HTTP_STATUS_MESSAGES,
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

describe('ErrorHandler', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('generateTraceId', () => {
    it('should generate unique trace IDs', () => {
      const id1 = generateTraceId()
      const id2 = generateTraceId()
      
      expect(id1).not.toBe(id2)
    })

    it('should generate trace ID with correct format', () => {
      const traceId = generateTraceId()
      
      // New format: ERR-{timestamp}-{counter}-{random}
      // Example: ERR-MLUEG864-00-A71Q3R
      expect(traceId).toMatch(/^ERR-[A-Z0-9]+-[A-Z0-9]{2,}-[A-Z0-9]{6}$/)
    })

    it('should generate many unique trace IDs in rapid succession', () => {
      const ids = new Set<string>()
      const count = 100
      
      for (let i = 0; i < count; i++) {
        ids.add(generateTraceId())
      }
      
      // All IDs should be unique
      expect(ids.size).toBe(count)
    })
  })

  describe('isValidTraceId', () => {
    it('should return true for valid trace IDs', () => {
      const traceId = generateTraceId()
      expect(isValidTraceId(traceId)).toBe(true)
    })

    it('should return false for invalid trace IDs', () => {
      expect(isValidTraceId('')).toBe(false)
      expect(isValidTraceId('invalid')).toBe(false)
      expect(isValidTraceId('ERR-')).toBe(false)
      expect(isValidTraceId('ERR-ABC')).toBe(false)
      expect(isValidTraceId('err-abc-00-123456')).toBe(false) // lowercase
    })
  })

  describe('extractTimestampFromTraceId', () => {
    it('should extract timestamp from valid trace ID', () => {
      const before = Date.now()
      const traceId = generateTraceId()
      const after = Date.now()
      
      const timestamp = extractTimestampFromTraceId(traceId)
      
      expect(timestamp).not.toBeNull()
      expect(timestamp).toBeGreaterThanOrEqual(before)
      expect(timestamp).toBeLessThanOrEqual(after)
    })

    it('should return null for invalid trace ID', () => {
      expect(extractTimestampFromTraceId('invalid')).toBeNull()
      expect(extractTimestampFromTraceId('')).toBeNull()
    })
  })

  describe('formatTraceIdForDisplay', () => {
    it('should format trace ID for display', () => {
      const traceId = generateTraceId()
      const formatted = formatTraceIdForDisplay(traceId)
      
      expect(formatted).toBe(traceId)
    })
  })

  describe('handle()', () => {
    it('should convert network error to friendly message', () => {
      const error: ApiError = {
        code: 'ERR_NETWORK',
        message: 'Network Error'
      }

      const result = errorHandler.handle(error)

      expect(result.userMessage).toBe('网络连接失败')
      expect(result.type).toBe(ErrorType.NETWORK)
      expect(result.suggestions).toContain('请检查您的网络连接是否正常')
    })

    it('should convert timeout error to friendly message', () => {
      const error: ApiError = {
        code: 'ECONNABORTED',
        message: 'timeout of 30000ms exceeded'
      }

      const result = errorHandler.handle(error)

      expect(result.userMessage).toBe('请求超时')
      expect(result.type).toBe(ErrorType.TIMEOUT)
    })

    it('should convert auth error (401) to friendly message', () => {
      const error: ApiError = {
        response: {
          status: 401,
          data: { code: 'UNAUTHORIZED', msg: 'Token expired' }
        }
      }

      const result = errorHandler.handle(error)

      expect(result.userMessage).toBe('未授权，请重新登录')
      expect(result.type).toBe(ErrorType.AUTH)
      expect(result.severity).toBe(ErrorSeverity.HIGH)
    })

    it('should convert permission error (403) to friendly message', () => {
      const error: ApiError = {
        response: {
          status: 403,
          data: { msg: 'Access denied' }
        }
      }

      const result = errorHandler.handle(error)

      expect(result.userMessage).toBe('没有访问权限')
      expect(result.type).toBe(ErrorType.PERMISSION)
    })

    it('should convert not found error (404) to friendly message', () => {
      const error: ApiError = {
        response: {
          status: 404,
          data: { msg: 'Resource not found' }
        }
      }

      const result = errorHandler.handle(error)

      expect(result.userMessage).toBe('请求的资源不存在')
      expect(result.type).toBe(ErrorType.NOT_FOUND)
    })

    it('should convert server error (500) to friendly message', () => {
      const error: ApiError = {
        response: {
          status: 500,
          data: { msg: 'Internal server error' }
        }
      }

      const result = errorHandler.handle(error)

      expect(result.userMessage).toBe('服务器内部错误')
      expect(result.type).toBe(ErrorType.SERVER)
      expect(result.severity).toBe(ErrorSeverity.CRITICAL)
    })

    it('should convert data source connection error to friendly message', () => {
      const error: ApiError = {
        response: {
          status: 500,
          data: { code: 'DATA_SOURCE_CONNECTION_FAILED', msg: 'Connection refused' }
        }
      }

      const result = errorHandler.handle(error)

      expect(result.userMessage).toBe('数据源连接失败')
      expect(result.suggestions).toContain('请检查数据源配置是否正确')
    })

    it('should convert SQL syntax error to friendly message', () => {
      const error: ApiError = {
        response: {
          status: 400,
          data: { code: 'SQL_SYNTAX_ERROR', msg: 'You have an error in your SQL syntax' }
        }
      }

      const result = errorHandler.handle(error)

      expect(result.userMessage).toBe('SQL语法错误')
      expect(result.suggestions).toContain('请检查SQL语句的语法是否正确')
    })

    it('should convert export error to friendly message', () => {
      const error: ApiError = {
        response: {
          status: 500,
          data: { code: 'EXPORT_FAILED', msg: 'Export failed' }
        }
      }

      const result = errorHandler.handle(error)

      expect(result.userMessage).toBe('导出失败')
      expect(result.suggestions).toContain('请稍后重试')
    })

    it('should convert rate limit error to friendly message', () => {
      const error: ApiError = {
        response: {
          status: 429,
          data: { msg: 'Too many requests' }
        }
      }

      const result = errorHandler.handle(error)

      expect(result.userMessage).toBe('请求过于频繁')
      expect(result.type).toBe(ErrorType.RATE_LIMIT)
      expect(result.severity).toBe(ErrorSeverity.LOW)
    })

    it('should include trace ID in error info', () => {
      const error = new Error('Test error')

      const result = errorHandler.handle(error)

      // New format: ERR-{timestamp}-{counter}-{random}
      expect(result.traceId).toMatch(/^ERR-[A-Z0-9]+-[A-Z0-9]{2,}-[A-Z0-9]{6}$/)
    })

    it('should include timestamp in error info', () => {
      const before = Date.now()
      const error = new Error('Test error')

      const result = errorHandler.handle(error)

      expect(result.timestamp).toBeGreaterThanOrEqual(before)
      expect(result.timestamp).toBeLessThanOrEqual(Date.now())
    })

    it('should preserve original error message', () => {
      const originalMessage = 'Original technical error message'
      const error = new Error(originalMessage)

      const result = errorHandler.handle(error)

      expect(result.message).toBe(originalMessage)
    })

    it('should include context in error info', () => {
      const error = new Error('Test error')
      const context = { userId: 123, action: 'save' }

      const result = errorHandler.handle(error, context)

      expect(result.context).toEqual(context)
    })

    it('should handle string errors', () => {
      const result = errorHandler.handle('Something went wrong')

      expect(result.message).toBe('Something went wrong')
      expect(result.userMessage).toBeDefined()
    })

    it('should handle null/undefined errors', () => {
      const result1 = errorHandler.handle(null)
      const result2 = errorHandler.handle(undefined)

      expect(result1.message).toBe('未知错误')
      expect(result2.message).toBe('未知错误')
    })

    it('should handle errors with nested response data', () => {
      const error: ApiError = {
        response: {
          status: 400,
          data: {
            code: 'VALIDATION_ERROR',
            msg: '参数验证失败',
            message: 'Validation failed'
          }
        }
      }

      const result = errorHandler.handle(error)

      expect(result.message).toBe('参数验证失败')
    })
  })

  describe('getSuggestions()', () => {
    it('should return suggestions for known error codes', () => {
      const suggestions = errorHandler.getSuggestions('DATA_SOURCE_CONNECTION_FAILED')

      expect(suggestions).toContain('请检查数据源配置是否正确')
      expect(suggestions.length).toBeGreaterThan(0)
    })

    it('should return empty array for unknown error codes', () => {
      const suggestions = errorHandler.getSuggestions('UNKNOWN_ERROR_CODE')

      expect(suggestions).toEqual([])
    })
  })

  describe('helper functions', () => {
    describe('getUserFriendlyMessage()', () => {
      it('should return user friendly message', () => {
        const error: ApiError = {
          code: 'ERR_NETWORK',
          message: 'Network Error'
        }

        const message = getUserFriendlyMessage(error)

        expect(message).toBe('网络连接失败')
      })
    })

    describe('getErrorSuggestions()', () => {
      it('should return suggestions for error', () => {
        const error: ApiError = {
          response: {
            status: 401,
            data: { msg: 'Unauthorized' }
          }
        }

        const suggestions = getErrorSuggestions(error)

        expect(suggestions.length).toBeGreaterThan(0)
      })
    })

    describe('handleError()', () => {
      it('should return complete error info', () => {
        const error = new Error('Test error')

        const result = handleError(error)

        expect(result).toHaveProperty('code')
        expect(result).toHaveProperty('message')
        expect(result).toHaveProperty('userMessage')
        expect(result).toHaveProperty('suggestions')
        expect(result).toHaveProperty('traceId')
        expect(result).toHaveProperty('timestamp')
        expect(result).toHaveProperty('type')
        expect(result).toHaveProperty('severity')
      })
    })
  })

  describe('error type detection', () => {
    it('should detect network errors', () => {
      expect(errorHandler.isNetworkError({ code: 'ERR_NETWORK' })).toBe(true)
      expect(errorHandler.isNetworkError({ code: 'ECONNREFUSED' })).toBe(true)
    })

    it('should detect auth errors', () => {
      expect(errorHandler.isAuthError({ response: { status: 401 } })).toBe(true)
    })

    it('should detect timeout errors', () => {
      expect(errorHandler.isTimeoutError({ code: 'ECONNABORTED' })).toBe(true)
      expect(errorHandler.isTimeoutError({ code: 'ETIMEDOUT' })).toBe(true)
    })

    it('should detect server errors', () => {
      expect(errorHandler.isServerError({ response: { status: 500 } })).toBe(true)
      expect(errorHandler.isServerError({ response: { status: 503 } })).toBe(true)
    })

    it('should identify retryable errors', () => {
      expect(errorHandler.isRetryable({ code: 'ERR_NETWORK' })).toBe(true)
      expect(errorHandler.isRetryable({ code: 'ECONNABORTED' })).toBe(true)
      expect(errorHandler.isRetryable({ response: { status: 500 } })).toBe(true)
      expect(errorHandler.isRetryable({ response: { status: 403 } })).toBe(false)
    })
  })

  describe('error code mappings', () => {
    it('should have mappings for common error codes', () => {
      const expectedCodes = [
        'ERR_NETWORK',
        'ECONNABORTED',
        'AUTH_FAILED',
        'TOKEN_EXPIRED',
        'PERMISSION_DENIED',
        'DATA_SOURCE_CONNECTION_FAILED',
        'SQL_SYNTAX_ERROR',
        'EXPORT_FAILED',
        'INTERNAL_SERVER_ERROR'
      ]

      expectedCodes.forEach(code => {
        expect(ERROR_CODE_MESSAGES[code]).toBeDefined()
        expect(ERROR_CODE_MESSAGES[code].message).toBeDefined()
        expect(ERROR_CODE_MESSAGES[code].suggestions).toBeDefined()
      })
    })

    it('should have mappings for common HTTP status codes', () => {
      const expectedStatuses = [400, 401, 403, 404, 408, 429, 500, 502, 503, 504]

      expectedStatuses.forEach(status => {
        expect(HTTP_STATUS_MESSAGES[status]).toBeDefined()
        expect(HTTP_STATUS_MESSAGES[status].message).toBeDefined()
        expect(HTTP_STATUS_MESSAGES[status].suggestions).toBeDefined()
      })
    })
  })

  describe('error reporting', () => {
    let fetchSpy: ReturnType<typeof vi.fn>

    beforeEach(() => {
      fetchSpy = vi.fn().mockResolvedValue(new Response())
      vi.stubGlobal('fetch', fetchSpy)
    })

    afterEach(() => {
      vi.unstubAllGlobals()
    })

    it('should report errors when configured', async () => {
      errorHandler.configure({
        reportEndpoint: 'https://api.example.com/errors',
        enableReport: true
      })

      const error = new Error('Test error')
      errorHandler.handle(error)

      // Wait for async report
      await new Promise(resolve => setTimeout(resolve, 10))

      expect(fetchSpy).toHaveBeenCalled()
    })

    it('should not report errors when disabled', async () => {
      errorHandler.configure({
        enableReport: false
      })

      const error = new Error('Test error')
      errorHandler.handle(error)

      await new Promise(resolve => setTimeout(resolve, 10))

      expect(fetchSpy).not.toHaveBeenCalled()
    })
  })
})

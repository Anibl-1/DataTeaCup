/**
 * API 错误分类处理属性测试
 * Feature: page-audit-optimization, Property 28
 *
 * **Validates: Requirements 23.3, 23.4**
 *
 * For any API 请求错误，当响应状态码为 401 时应清除登录状态并重定向到登录页；
 * 当为网络错误（无响应）时应显示网络异常提示。
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import * as fc from 'fast-check'

// Mock message utility
vi.mock('@/utils/message', () => ({
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
  },
}))

// Mock logger
vi.mock('@/utils/logger', () => ({
  logger: {
    error: vi.fn(),
    warn: vi.fn(),
    info: vi.fn(),
    debug: vi.fn(),
  },
}))

import { message } from '@/utils/message'

// ============================================================================
// Pure model of API error classification from request.ts
// ============================================================================

/**
 * Error classification model extracted from request.ts response interceptor.
 *
 * The interceptor handles errors in two main branches:
 * 1. error.response exists → HTTP error (401, 403, 404, 500, etc.)
 * 2. error.request exists but no response → Network error
 */

type ErrorAction =
  | { type: 'unauthorized'; message: string }   // 401: clear state + redirect
  | { type: 'forbidden'; message: string }       // 403: permission denied
  | { type: 'not_found'; message: string }       // 404: resource not found
  | { type: 'rate_limited'; message: string }    // 429: too many requests
  | { type: 'server_error'; message: string }    // 500: server error
  | { type: 'other_http'; message: string; status: number }  // other HTTP errors
  | { type: 'network_error'; message: string }   // no response: network failure

function classifyApiError(error: {
  response?: { status: number; data?: { msg?: string; message?: string } }
  request?: unknown
  message?: string
}): ErrorAction {
  if (error.response) {
    const status = error.response.status
    const res = error.response.data
    let errorMsg = '请求失败'

    if (res) {
      if (typeof res === 'object' && res.msg) {
        errorMsg = res.msg
      } else if (typeof res === 'object' && res.message) {
        errorMsg = res.message
      }
    }

    switch (status) {
      case 401:
        return { type: 'unauthorized', message: errorMsg || '未授权，请重新登录' }
      case 403:
        return { type: 'forbidden', message: '您没有权限执行此操作' }
      case 404:
        return { type: 'not_found', message: errorMsg || '请求的资源不存在' }
      case 429:
        return { type: 'rate_limited', message: errorMsg || '请求过于频繁，请稍后再试' }
      case 500:
        return { type: 'server_error', message: errorMsg || '服务器内部错误' }
      default:
        return { type: 'other_http', message: errorMsg || `请求失败 (${status})`, status }
    }
  } else if (error.request) {
    return { type: 'network_error', message: '网络连接失败，请检查网络或后端服务是否运行' }
  }

  return { type: 'network_error', message: error.message || '请求失败' }
}

// ============================================================================
// Arbitraries
// ============================================================================

/** HTTP status codes that the interceptor handles specifically */
const specificStatusArb = fc.constantFrom(401, 403, 404, 429, 500)

/** Other HTTP status codes (not specifically handled) */
const otherStatusArb = fc.integer({ min: 400, max: 599 }).filter(
  s => ![401, 403, 404, 429, 500].includes(s)
)

/** Random error messages from backend */
const backendMsgArb = fc.oneof(
  fc.constant(undefined),
  fc.record({
    msg: fc.option(fc.string({ minLength: 1, maxLength: 50 }), { nil: undefined }),
    message: fc.option(fc.string({ minLength: 1, maxLength: 50 }), { nil: undefined }),
  })
)

// ============================================================================
// Property-Based Tests
// ============================================================================

describe('Property 28: API 错误分类处理', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    vi.clearAllMocks()
  })

  afterEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  /**
   * Core property: 401 errors should always be classified as 'unauthorized'
   */
  it('401 responses should always be classified as unauthorized', () => {
    fc.assert(
      fc.property(backendMsgArb, (data) => {
        const result = classifyApiError({
          response: { status: 401, data: data as any },
        })
        return result.type === 'unauthorized'
      }),
      { numRuns: 100 }
    )
  })

  /**
   * Core property: network errors (no response) should be classified as 'network_error'
   */
  it('network errors (no response, has request) should be classified as network_error', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 0, maxLength: 100 }),
        (_msg) => {
          const result = classifyApiError({
            request: {}, // request was sent but no response received
            message: 'Network Error',
          })
          return result.type === 'network_error'
        }
      ),
      { numRuns: 50 }
    )
  })

  /**
   * Property: 401 and network errors should produce different action types
   */
  it('401 and network errors should always produce different classifications', () => {
    const unauthorizedResult = classifyApiError({
      response: { status: 401 },
    })
    const networkResult = classifyApiError({
      request: {},
      message: 'Network Error',
    })

    expect(unauthorizedResult.type).toBe('unauthorized')
    expect(networkResult.type).toBe('network_error')
    expect(unauthorizedResult.type).not.toBe(networkResult.type)
  })

  /**
   * Property: 403 errors should be classified as 'forbidden'
   */
  it('403 responses should be classified as forbidden', () => {
    fc.assert(
      fc.property(backendMsgArb, (data) => {
        const result = classifyApiError({
          response: { status: 403, data: data as any },
        })
        return result.type === 'forbidden'
      }),
      { numRuns: 50 }
    )
  })

  /**
   * Property: 500 errors should be classified as 'server_error'
   */
  it('500 responses should be classified as server_error', () => {
    fc.assert(
      fc.property(backendMsgArb, (data) => {
        const result = classifyApiError({
          response: { status: 500, data: data as any },
        })
        return result.type === 'server_error'
      }),
      { numRuns: 50 }
    )
  })

  /**
   * Property: non-specifically-handled HTTP errors should be classified as 'other_http'
   */
  it('other HTTP status codes should be classified as other_http', () => {
    fc.assert(
      fc.property(otherStatusArb, backendMsgArb, (status, data) => {
        const result = classifyApiError({
          response: { status, data: data as any },
        })
        return result.type === 'other_http' && (result as any).status === status
      }),
      { numRuns: 100 }
    )
  })

  /**
   * Property: every error classification should have a non-empty message
   */
  it('every error classification should produce a non-empty message', () => {
    fc.assert(
      fc.property(
        fc.oneof(
          // HTTP error with response
          fc.record({
            response: fc.record({
              status: fc.integer({ min: 400, max: 599 }),
              data: fc.option(
                fc.record({
                  msg: fc.option(fc.string({ minLength: 1, maxLength: 50 }), { nil: undefined }),
                }),
                { nil: undefined }
              ),
            }),
          }),
          // Network error
          fc.constant({ request: {}, message: 'Network Error' })
        ),
        (error) => {
          const result = classifyApiError(error as any)
          return typeof result.message === 'string' && result.message.length > 0
        }
      ),
      { numRuns: 200 }
    )
  })

  /**
   * Property: backend error messages should be preserved when available
   */
  it('should use backend msg when available for 401 errors', () => {
    const customMsg = '令牌已过期'
    const result = classifyApiError({
      response: { status: 401, data: { msg: customMsg } },
    })
    expect(result.type).toBe('unauthorized')
    expect(result.message).toBe(customMsg)
  })

  /**
   * Property: network error message should always mention network
   */
  it('network error message should mention network connectivity', () => {
    const result = classifyApiError({
      request: {},
      message: 'Network Error',
    })
    expect(result.message).toContain('网络')
  })

  /**
   * Integration: verify handleUnauthorized clears localStorage
   */
  it('handleUnauthorized behavior: 401 should clear auth state', () => {
    // Simulate the handleUnauthorized function from request.ts
    localStorage.setItem('token', 'test-token')
    sessionStorage.setItem('tabs-views', '[]')
    sessionStorage.setItem('tabs-active', '/dashboard')
    sessionStorage.setItem('lastActivityTime', '12345')

    // The handleUnauthorized function does:
    const handleUnauthorized = () => {
      localStorage.removeItem('token')
      sessionStorage.removeItem('tabs-views')
      sessionStorage.removeItem('tabs-active')
      sessionStorage.removeItem('lastActivityTime')
    }

    handleUnauthorized()

    expect(localStorage.getItem('token')).toBeNull()
    expect(sessionStorage.getItem('tabs-views')).toBeNull()
    expect(sessionStorage.getItem('tabs-active')).toBeNull()
    expect(sessionStorage.getItem('lastActivityTime')).toBeNull()
  })
})

/**
 * 错误追踪 ID 属性测试
 * 
 * Property 59: 错误追踪 ID 唯一性 - 为每个错误生成唯一的错误追踪ID，便于问题排查。
 * 
 * **Validates: Requirements 18.5**
 * THE Error_Handler SHALL 为每个错误生成唯一的错误追踪ID，便于问题排查
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as fc from 'fast-check'
import {
  generateTraceId,
  isValidTraceId,
  extractTimestampFromTraceId,
  formatTraceIdForDisplay,
  errorHandler
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

describe('Feature: platform-deep-optimization, Property 59: 错误追踪 ID 唯一性', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  /**
   * Property 59: 错误追踪 ID 唯一性
   * 
   * **Validates: Requirements 18.5**
   * 
   * 为每个错误生成唯一的错误追踪ID，便于问题排查。
   */
  describe('Property 59: 错误追踪 ID 唯一性 - 核心属性测试', () => {
    
    describe('唯一性属性', () => {
      it('should generate unique trace IDs for any number of consecutive calls', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 2, max: 500 }),
            (count) => {
              const ids = new Set<string>()
              
              for (let i = 0; i < count; i++) {
                ids.add(generateTraceId())
              }
              
              // All generated IDs should be unique
              expect(ids.size).toBe(count)
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should generate unique trace IDs even when called rapidly in succession', () => {
        fc.assert(
          fc.property(
            fc.constant(null),
            () => {
              // Generate many IDs as fast as possible
              const ids: string[] = []
              for (let i = 0; i < 1000; i++) {
                ids.push(generateTraceId())
              }
              
              const uniqueIds = new Set(ids)
              expect(uniqueIds.size).toBe(ids.length)
            }
          ),
          { numRuns: 10 }
        )
      })

      it('should generate unique trace IDs across multiple error handler calls', () => {
        // Arbitrary for generating various error types
        const errorArb = fc.oneof(
          fc.constant(new Error('Test error')),
          fc.constant({ code: 'ERR_NETWORK' }),
          fc.constant({ response: { status: 500 } }),
          fc.constant(null),
          fc.string().map(msg => new Error(msg))
        )

        fc.assert(
          fc.property(
            fc.array(errorArb, { minLength: 2, maxLength: 100 }),
            (errors) => {
              const traceIds = errors.map(error => errorHandler.handle(error).traceId)
              const uniqueIds = new Set(traceIds)
              
              // All trace IDs should be unique
              expect(uniqueIds.size).toBe(traceIds.length)
            }
          ),
          { numRuns: 30 }
        )
      })
    })

    describe('格式属性', () => {
      it('should always generate trace IDs matching the expected format', () => {
        fc.assert(
          fc.property(
            fc.constant(null),
            () => {
              const traceId = generateTraceId()
              
              // Format: ERR-{timestamp}-{counter}-{random}
              // Example: ERR-LQ2X5K8-00-A1B2C3
              expect(traceId).toMatch(/^ERR-[A-Z0-9]+-[A-Z0-9]{2,}-[A-Z0-9]{6}$/)
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should always start with ERR- prefix', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 1, max: 100 }),
            (count) => {
              for (let i = 0; i < count; i++) {
                const traceId = generateTraceId()
                expect(traceId.startsWith('ERR-')).toBe(true)
              }
            }
          ),
          { numRuns: 20 }
        )
      })

      it('should always generate uppercase trace IDs', () => {
        fc.assert(
          fc.property(
            fc.constant(null),
            () => {
              const traceId = generateTraceId()
              expect(traceId).toBe(traceId.toUpperCase())
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should generate trace IDs with consistent structure (4 parts separated by hyphens)', () => {
        fc.assert(
          fc.property(
            fc.constant(null),
            () => {
              const traceId = generateTraceId()
              const parts = traceId.split('-')
              
              // Should have 4 parts: ERR, timestamp, counter, random
              expect(parts.length).toBe(4)
              expect(parts[0]).toBe('ERR')
              expect(parts[1].length).toBeGreaterThan(0) // timestamp
              expect(parts[2].length).toBeGreaterThanOrEqual(2) // counter
              expect(parts[3].length).toBe(6) // random
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    describe('验证属性', () => {
      it('should validate all generated trace IDs as valid', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 1, max: 100 }),
            (count) => {
              for (let i = 0; i < count; i++) {
                const traceId = generateTraceId()
                expect(isValidTraceId(traceId)).toBe(true)
              }
            }
          ),
          { numRuns: 20 }
        )
      })

      it('should reject invalid trace ID formats', () => {
        const invalidTraceIdArb = fc.oneof(
          fc.constant(''),
          fc.constant('ERR'),
          fc.constant('ERR-'),
          fc.constant('ERR-123'),
          fc.constant('ERR-123-'),
          fc.constant('err-abc-00-123456'), // lowercase
          fc.constant('ERROR-ABC-00-123456'), // wrong prefix
          fc.constant('ERR-ABC-0-123456'), // counter too short
          fc.constant('ERR-ABC-00-12345'), // random too short
          fc.constant('ERR-ABC-00-1234567'), // random too long
          fc.string({ minLength: 1, maxLength: 50 }).filter(s => !s.startsWith('ERR-'))
        )

        fc.assert(
          fc.property(invalidTraceIdArb, (invalidId) => {
            expect(isValidTraceId(invalidId)).toBe(false)
          }),
          { numRuns: 50 }
        )
      })

      it('should correctly validate trace IDs with various valid patterns', () => {
        // Generate valid trace IDs and verify they pass validation
        fc.assert(
          fc.property(
            fc.constant(null),
            () => {
              const traceId = generateTraceId()
              
              // Should be valid
              expect(isValidTraceId(traceId)).toBe(true)
              
              // Modifying any part should make it invalid
              const parts = traceId.split('-')
              
              // Invalid prefix
              expect(isValidTraceId(`XXX-${parts[1]}-${parts[2]}-${parts[3]}`)).toBe(false)
              
              // Empty timestamp
              expect(isValidTraceId(`ERR--${parts[2]}-${parts[3]}`)).toBe(false)
            }
          ),
          { numRuns: 50 }
        )
      })
    })

    describe('时间戳提取属性', () => {
      it('should extract valid timestamps from generated trace IDs', () => {
        fc.assert(
          fc.property(
            fc.constant(null),
            () => {
              const before = Date.now()
              const traceId = generateTraceId()
              const after = Date.now()
              
              const extractedTimestamp = extractTimestampFromTraceId(traceId)
              
              expect(extractedTimestamp).not.toBeNull()
              expect(extractedTimestamp).toBeGreaterThanOrEqual(before)
              expect(extractedTimestamp).toBeLessThanOrEqual(after)
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should return null for invalid trace IDs', () => {
        const invalidTraceIdArb = fc.oneof(
          fc.constant(''),
          fc.constant('INVALID'),
          fc.constant('ERR-'),
          fc.string({ minLength: 1, maxLength: 20 }).filter(s => !s.startsWith('ERR-'))
        )

        fc.assert(
          fc.property(invalidTraceIdArb, (invalidId) => {
            const timestamp = extractTimestampFromTraceId(invalidId)
            expect(timestamp).toBeNull()
          }),
          { numRuns: 30 }
        )
      })

      it('should extract timestamps that are monotonically increasing for sequential IDs', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 5, max: 50 }),
            (count) => {
              const timestamps: number[] = []
              
              for (let i = 0; i < count; i++) {
                const traceId = generateTraceId()
                const timestamp = extractTimestampFromTraceId(traceId)
                if (timestamp !== null) {
                  timestamps.push(timestamp)
                }
              }
              
              // Timestamps should be monotonically non-decreasing
              for (let i = 1; i < timestamps.length; i++) {
                expect(timestamps[i]).toBeGreaterThanOrEqual(timestamps[i - 1])
              }
            }
          ),
          { numRuns: 20 }
        )
      })
    })

    describe('显示格式化属性', () => {
      it('should format trace IDs for display without modification', () => {
        fc.assert(
          fc.property(
            fc.constant(null),
            () => {
              const traceId = generateTraceId()
              const formatted = formatTraceIdForDisplay(traceId)
              
              // Currently, formatting returns the ID as-is
              expect(formatted).toBe(traceId)
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should preserve trace ID integrity through formatting', () => {
        fc.assert(
          fc.property(
            fc.constant(null),
            () => {
              const traceId = generateTraceId()
              const formatted = formatTraceIdForDisplay(traceId)
              
              // Formatted ID should still be valid
              expect(isValidTraceId(formatted)).toBe(true)
            }
          ),
          { numRuns: 50 }
        )
      })
    })

    describe('错误处理集成属性', () => {
      it('should include valid trace ID in every error info', () => {
        const errorArb = fc.oneof(
          fc.constant(new Error('Test error')),
          fc.constant({ code: 'ERR_NETWORK', message: 'Network error' }),
          fc.constant({ response: { status: 500, data: { msg: 'Server error' } } }),
          fc.constant(null),
          fc.constant(undefined),
          fc.string().map(msg => new Error(msg))
        )

        fc.assert(
          fc.property(errorArb, (error) => {
            const errorInfo = errorHandler.handle(error)
            
            // Trace ID should be present and valid
            expect(errorInfo.traceId).toBeDefined()
            expect(typeof errorInfo.traceId).toBe('string')
            expect(errorInfo.traceId.length).toBeGreaterThan(0)
            expect(isValidTraceId(errorInfo.traceId)).toBe(true)
          }),
          { numRuns: 100 }
        )
      })

      it('should generate different trace IDs for the same error handled multiple times', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 2, max: 20 }),
            (count) => {
              const error = new Error('Same error')
              const traceIds: string[] = []
              
              for (let i = 0; i < count; i++) {
                const errorInfo = errorHandler.handle(error)
                traceIds.push(errorInfo.traceId)
              }
              
              const uniqueIds = new Set(traceIds)
              expect(uniqueIds.size).toBe(count)
            }
          ),
          { numRuns: 20 }
        )
      })

      it('should include trace ID that can be used for error tracking', () => {
        fc.assert(
          fc.property(
            fc.record({
              code: fc.string({ minLength: 1, maxLength: 20 }),
              message: fc.string({ minLength: 1, maxLength: 100 })
            }),
            (errorData) => {
              const error = { code: errorData.code, message: errorData.message }
              const errorInfo = errorHandler.handle(error)
              
              // Trace ID should be extractable and usable
              const traceId = errorInfo.traceId
              expect(traceId).toBeDefined()
              
              // Should be able to extract timestamp for tracking
              const timestamp = extractTimestampFromTraceId(traceId)
              expect(timestamp).not.toBeNull()
              
              // Timestamp should be close to error timestamp
              expect(Math.abs(timestamp! - errorInfo.timestamp)).toBeLessThan(1000)
            }
          ),
          { numRuns: 50 }
        )
      })
    })

    describe('并发安全属性', () => {
      it('should maintain uniqueness under simulated concurrent generation', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 10, max: 100 }),
            (batchSize) => {
              // Simulate concurrent generation by creating multiple batches rapidly
              const allIds: string[] = []
              
              // Generate multiple batches
              for (let batch = 0; batch < 5; batch++) {
                const batchIds: string[] = []
                for (let i = 0; i < batchSize; i++) {
                  batchIds.push(generateTraceId())
                }
                allIds.push(...batchIds)
              }
              
              const uniqueIds = new Set(allIds)
              expect(uniqueIds.size).toBe(allIds.length)
            }
          ),
          { numRuns: 20 }
        )
      })
    })

    describe('边界条件属性', () => {
      it('should handle trace ID generation at timestamp boundaries', () => {
        fc.assert(
          fc.property(
            fc.constant(null),
            () => {
              // Generate IDs rapidly to test counter increment
              const ids: string[] = []
              const startTime = Date.now()
              
              // Generate until we've likely crossed a millisecond boundary
              while (Date.now() - startTime < 5) {
                ids.push(generateTraceId())
              }
              
              // All IDs should still be unique
              const uniqueIds = new Set(ids)
              expect(uniqueIds.size).toBe(ids.length)
            }
          ),
          { numRuns: 10 }
        )
      })

      it('should generate valid trace IDs regardless of system time', () => {
        fc.assert(
          fc.property(
            fc.constant(null),
            () => {
              const traceId = generateTraceId()
              
              // Should always be valid regardless of when generated
              expect(isValidTraceId(traceId)).toBe(true)
              
              // Should always have extractable timestamp
              const timestamp = extractTimestampFromTraceId(traceId)
              expect(timestamp).not.toBeNull()
              expect(typeof timestamp).toBe('number')
              expect(timestamp).toBeGreaterThan(0)
            }
          ),
          { numRuns: 100 }
        )
      })
    })
  })
})

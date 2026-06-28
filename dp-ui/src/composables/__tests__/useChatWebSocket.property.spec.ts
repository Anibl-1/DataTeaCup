import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { calculateReconnectDelay, type ReconnectConfig } from '@/composables/useChatWebSocket'

/**
 * Property 11: WebSocket 重连延迟指数退避
 *
 * **Validates: Requirements 11.1**
 * **Feature: core-modules-deep-optimization, Property 11: WebSocket 重连延迟指数退避**
 *
 * For any 重试次数 n（0 ≤ n < maxRetries），计算的重连延迟应等于
 * min(baseDelay × backoffMultiplier^n, maxDelay)。
 */
describe('useChatWebSocket — Property Tests', () => {
  // Smart generator: valid ReconnectConfig with positive, reasonable values
  const reconnectConfigArb: fc.Arbitrary<ReconnectConfig> = fc.record({
    maxRetries: fc.integer({ min: 1, max: 20 }),
    baseDelay: fc.integer({ min: 1, max: 10000 }),
    maxDelay: fc.integer({ min: 1, max: 120000 }),
    backoffMultiplier: fc.double({ min: 1, max: 5, noNaN: true, noDefaultInfinity: true }),
  }).filter((c) => c.maxDelay >= c.baseDelay)

  // Generator: retryCount constrained to [0, maxRetries)
  const retryWithConfigArb = reconnectConfigArb.chain((config) =>
    fc.tuple(
      fc.integer({ min: 0, max: config.maxRetries - 1 }),
      fc.constant(config),
    ),
  )

  it('Property 11: delay equals min(baseDelay × backoffMultiplier^n, maxDelay)', () => {
    fc.assert(
      fc.property(retryWithConfigArb, ([retryCount, config]) => {
        const actual = calculateReconnectDelay(retryCount, config)
        const expected = Math.min(
          config.baseDelay * Math.pow(config.backoffMultiplier, retryCount),
          config.maxDelay,
        )
        expect(actual).toBeCloseTo(expected, 10)
      }),
      { numRuns: 200 },
    )
  })

  it('Property 11 (supplemental): delay is always >= baseDelay for retryCount >= 0', () => {
    fc.assert(
      fc.property(retryWithConfigArb, ([retryCount, config]) => {
        const delay = calculateReconnectDelay(retryCount, config)
        // baseDelay × multiplier^0 = baseDelay, and multiplier >= 1, so delay >= baseDelay
        // But delay is capped at maxDelay, and maxDelay >= baseDelay (by filter)
        expect(delay).toBeGreaterThanOrEqual(config.baseDelay)
      }),
      { numRuns: 200 },
    )
  })

  it('Property 11 (supplemental): delay is always <= maxDelay', () => {
    fc.assert(
      fc.property(retryWithConfigArb, ([retryCount, config]) => {
        const delay = calculateReconnectDelay(retryCount, config)
        expect(delay).toBeLessThanOrEqual(config.maxDelay)
      }),
      { numRuns: 200 },
    )
  })
})

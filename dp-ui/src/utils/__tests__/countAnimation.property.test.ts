/**
 * Feature: mars-integration-optimization, Property 22: 数字递增动画到达目标值
 * **Validates: Requirements 3.2**
 *
 * *For any* 非负整数目标值，数字递增动画函数在动画完成后应精确到达目标值。
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import * as fc from 'fast-check'
import { ref } from 'vue'
import { useCountAnimation } from '../countAnimation'

describe('Property 22: 数字递增动画到达目标值', () => {
  let rafCallbacks: Array<(time: number) => void> = []
  let currentTime = 0

  beforeEach(() => {
    rafCallbacks = []
    currentTime = 0

    // Mock performance.now to return controlled time
    vi.spyOn(performance, 'now').mockImplementation(() => currentTime)

    // Mock requestAnimationFrame to capture callbacks
    vi.spyOn(window, 'requestAnimationFrame').mockImplementation((cb: FrameRequestCallback) => {
      rafCallbacks.push(cb as (time: number) => void)
      return rafCallbacks.length
    })

    vi.spyOn(window, 'cancelAnimationFrame').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  /**
   * Flush all pending requestAnimationFrame callbacks, advancing time
   * to simulate the animation running to completion.
   */
  function runAnimationToCompletion(duration: number) {
    // Run frames until animation completes (progress >= 1)
    // We advance time past the duration to ensure completion
    const steps = [0.25, 0.5, 0.75, 1.0, 1.1]
    for (const fraction of steps) {
      const time = duration * fraction
      currentTime = time
      // Process all pending callbacks
      const pending = [...rafCallbacks]
      rafCallbacks = []
      for (const cb of pending) {
        cb(time)
      }
    }
  }

  /**
   * Property test: For any non-negative integer target, after the animation
   * completes, the current value should precisely equal the target value.
   */
  it('should precisely reach any non-negative integer target after animation completes', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: 1_000_000 }),
        (targetValue) => {
          // Reset mocks state for each iteration
          rafCallbacks = []
          currentTime = 0

          const target = ref(targetValue)
          const current = useCountAnimation(target, 800)

          // Run animation to completion
          runAnimationToCompletion(800)

          // The animated value must precisely equal the target
          expect(current.value).toBe(targetValue)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Property test: For any non-negative integer target with duration 0,
   * the value should immediately equal the target (no animation).
   */
  it('should immediately reach target when duration is 0', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: 1_000_000 }),
        (targetValue) => {
          rafCallbacks = []
          currentTime = 0

          const target = ref(targetValue)
          const current = useCountAnimation(target, 0)

          // With duration 0, value should be set immediately without animation
          expect(current.value).toBe(targetValue)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Property test: The easeOut function embedded in the animation satisfies
   * easeOut(1) === 1, meaning at progress=1 the animation reaches 100%.
   * We verify this indirectly: when time equals duration, the composable
   * sets current.value = targetValue exactly (not a rounded interpolation).
   */
  it('should reach exact target for any non-negative integer (large values)', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 100_000, max: 10_000_000 }),
        (targetValue) => {
          rafCallbacks = []
          currentTime = 0

          const target = ref(targetValue)
          const current = useCountAnimation(target, 800)

          // Run animation to completion
          runAnimationToCompletion(800)

          // Must be exact — no floating point rounding errors
          expect(current.value).toBe(targetValue)
        }
      ),
      { numRuns: 100 }
    )
  })
})

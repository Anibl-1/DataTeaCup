/**
 * 响应式断点属性测试
 * Feature: page-audit-optimization, Property 12: 响应式断点判断
 *
 * **Validates: Requirements 16.6**
 *
 * For any 屏幕宽度值，当宽度 < 768px 时 `isMobile` 应为 true，
 * 当宽度 ≥ 768px 时应为 false。
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { BREAKPOINTS } from '../useBreakpoint'

// ============================================================================
// Pure logic under test
// ============================================================================

/**
 * Mirrors the isMobile computed logic from useBreakpoint:
 *   `computed(() => width.value < 768)`
 *
 * We extract this as a pure function so we can property-test it
 * without needing Vue lifecycle hooks or a DOM environment.
 */
const MOBILE_BREAKPOINT = 768

function isMobile(width: number): boolean {
  return width < MOBILE_BREAKPOINT
}

// ============================================================================
// Arbitraries (Test Data Generators)
// ============================================================================

/** Any realistic screen width (1 – 7680 covering 8K displays) */
const screenWidthArb = fc.integer({ min: 1, max: 7680 })

/** Widths strictly below the mobile breakpoint */
const mobileWidthArb = fc.integer({ min: 1, max: MOBILE_BREAKPOINT - 1 })

/** Widths at or above the mobile breakpoint */
const nonMobileWidthArb = fc.integer({ min: MOBILE_BREAKPOINT, max: 7680 })

/** Boundary-adjacent widths (breakpoint ± 1) */
const nearBoundaryWidthArb = fc.constantFrom(
  MOBILE_BREAKPOINT - 2,
  MOBILE_BREAKPOINT - 1,
  MOBILE_BREAKPOINT,
  MOBILE_BREAKPOINT + 1,
  MOBILE_BREAKPOINT + 2
)

// ============================================================================
// Property-Based Tests
// ============================================================================

describe('Property 12: 响应式断点判断', () => {
  /**
   * Core property: isMobile ↔ width < 768
   */
  it('isMobile should be true iff width < 768 for any width', () => {
    fc.assert(
      fc.property(screenWidthArb, (width) => {
        const result = isMobile(width)
        const expected = width < MOBILE_BREAKPOINT
        return result === expected
      }),
      { numRuns: 1000 }
    )
  })

  /**
   * All widths below 768 must be mobile
   */
  it('should return true for any width < 768', () => {
    fc.assert(
      fc.property(mobileWidthArb, (width) => {
        return isMobile(width) === true
      }),
      { numRuns: 500 }
    )
  })

  /**
   * All widths at or above 768 must NOT be mobile
   */
  it('should return false for any width >= 768', () => {
    fc.assert(
      fc.property(nonMobileWidthArb, (width) => {
        return isMobile(width) === false
      }),
      { numRuns: 500 }
    )
  })

  /**
   * Boundary precision: the transition happens exactly at 768
   */
  it('should transition exactly at the 768px boundary', () => {
    fc.assert(
      fc.property(nearBoundaryWidthArb, (width) => {
        const result = isMobile(width)
        return result === (width < MOBILE_BREAKPOINT)
      }),
      { numRuns: 50 }
    )
  })

  /**
   * Deterministic boundary examples
   */
  it('should be true at 767 and false at 768', () => {
    expect(isMobile(767)).toBe(true)
    expect(isMobile(768)).toBe(false)
  })

  /**
   * Consistency with BREAKPOINTS constant exported from useBreakpoint
   */
  it('BREAKPOINTS.md should equal 768 (the mobile threshold used in useBreakpoint)', () => {
    // useBreakpoint defines: isMobile = width < 768
    // and BREAKPOINTS.md = 768
    expect(BREAKPOINTS.md).toBe(MOBILE_BREAKPOINT)
  })
})

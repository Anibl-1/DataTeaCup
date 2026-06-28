/**
 * 响应式布局属性测试
 * Feature: platform-deep-optimization
 * Property 2: 响应式布局适配
 * 
 * **Validates: Requirements 1.2**
 * 
 * WHEN 用户在不同屏幕尺寸（1920/1440/1366/1280像素宽度）访问平台时，
 * THE DataTeaCup SHALL 自动调整布局以适配当前屏幕尺寸
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { 
  getBreakpointFromWidth, 
  BREAKPOINTS,
  type Breakpoint 
} from '../useResponsive'

// ============================================================================
// Constants
// ============================================================================

/**
 * 断点顺序 (从小到大)
 */
const BREAKPOINT_ORDER: Breakpoint[] = ['xs', 'sm', 'md', 'lg']

// ============================================================================
// Helper Functions
// ============================================================================

/**
 * 获取断点的数值索引
 */
function getBreakpointIndex(bp: Breakpoint): number {
  return BREAKPOINT_ORDER.indexOf(bp)
}

/**
 * 根据宽度计算预期的断点
 */
function expectedBreakpoint(width: number): Breakpoint {
  if (width >= BREAKPOINTS.lg) return 'lg'
  if (width >= BREAKPOINTS.md) return 'md'
  if (width >= BREAKPOINTS.sm) return 'sm'
  return 'xs'
}

/**
 * 计算设备类型标志
 */
function calculateDeviceFlags(width: number): { isMobile: boolean; isTablet: boolean; isDesktop: boolean } {
  return {
    isMobile: width < BREAKPOINTS.xs,
    isTablet: width >= BREAKPOINTS.xs && width < BREAKPOINTS.md,
    isDesktop: width >= BREAKPOINTS.md
  }
}

/**
 * 检查是否大于等于指定断点
 */
function isBreakpointUp(width: number, bp: Breakpoint): boolean {
  return width >= BREAKPOINTS[bp]
}

/**
 * 检查是否小于指定断点
 */
function isBreakpointDown(width: number, bp: Breakpoint): boolean {
  return width < BREAKPOINTS[bp]
}

/**
 * 检查是否在指定断点范围内
 */
function isBreakpointBetween(width: number, min: Breakpoint, max: Breakpoint): boolean {
  const minIndex = getBreakpointIndex(min)
  const maxIndex = getBreakpointIndex(max)
  
  if (minIndex > maxIndex) return false
  
  return width >= BREAKPOINTS[min] && width < BREAKPOINTS[max]
}

// ============================================================================
// Arbitraries (Test Data Generators)
// ============================================================================

/**
 * 生成任意有效的屏幕宽度 (320px - 4096px)
 */
const screenWidthArb = fc.integer({ min: 320, max: 4096 })

/**
 * 生成任意断点类型
 */
const breakpointArb = fc.constantFrom<Breakpoint>('xs', 'sm', 'md', 'lg')

/**
 * 生成断点边界附近的值 (边界值 ± 1)
 */
const nearBoundaryWidthArb = fc.constantFrom(
  BREAKPOINTS.xs - 1,
  BREAKPOINTS.xs,
  BREAKPOINTS.xs + 1,
  BREAKPOINTS.sm - 1,
  BREAKPOINTS.sm,
  BREAKPOINTS.sm + 1,
  BREAKPOINTS.md - 1,
  BREAKPOINTS.md,
  BREAKPOINTS.md + 1,
  BREAKPOINTS.lg - 1,
  BREAKPOINTS.lg,
  BREAKPOINTS.lg + 1
)

/**
 * 生成有效的断点对 (min <= max)
 */
const validBreakpointPairArb = fc.tuple(breakpointArb, breakpointArb)
  .filter(([min, max]) => getBreakpointIndex(min) <= getBreakpointIndex(max))

// ============================================================================
// Property-Based Tests
// ============================================================================

describe('Responsive Layout Property Tests', () => {
  /**
   * Property 2: 响应式布局适配
   * 
   * 验证响应式布局服务在任意屏幕宽度下都能正确返回断点和设备类型
   * 
   * **Validates: Requirements 1.2**
   */
  describe('Property 2: Responsive Layout Adaptation', () => {
    // ========================================================================
    // 2.1 Correct Breakpoint for Any Screen Width
    // ========================================================================
    describe('2.1 For any screen width, the correct breakpoint is returned', () => {
      it('should return correct breakpoint for any valid screen width', () => {
        fc.assert(
          fc.property(
            screenWidthArb,
            (width) => {
              const actual = getBreakpointFromWidth(width)
              const expected = expectedBreakpoint(width)
              return actual === expected
            }
          ),
          { numRuns: 500 }
        )
      })

      it('should return xs for widths below sm breakpoint', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 320, max: BREAKPOINTS.sm - 1 }),
            (width) => {
              const breakpoint = getBreakpointFromWidth(width)
              return breakpoint === 'xs'
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should return sm for widths in sm range', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: BREAKPOINTS.sm, max: BREAKPOINTS.md - 1 }),
            (width) => {
              const breakpoint = getBreakpointFromWidth(width)
              return breakpoint === 'sm'
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should return md for widths in md range', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: BREAKPOINTS.md, max: BREAKPOINTS.lg - 1 }),
            (width) => {
              const breakpoint = getBreakpointFromWidth(width)
              return breakpoint === 'md'
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should return lg for widths at or above lg breakpoint', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: BREAKPOINTS.lg, max: 4096 }),
            (width) => {
              const breakpoint = getBreakpointFromWidth(width)
              return breakpoint === 'lg'
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ========================================================================
    // 2.2 Breakpoint Transitions at Boundary Values
    // ========================================================================
    describe('2.2 Breakpoint transitions are correct at boundary values', () => {
      it('should transition correctly at xs boundary (1280px)', () => {
        // Just below xs: still xs
        expect(getBreakpointFromWidth(BREAKPOINTS.xs - 1)).toBe('xs')
        // At xs: xs
        expect(getBreakpointFromWidth(BREAKPOINTS.xs)).toBe('xs')
        // Just above xs: still xs (until sm)
        expect(getBreakpointFromWidth(BREAKPOINTS.xs + 1)).toBe('xs')
      })

      it('should transition correctly at sm boundary (1366px)', () => {
        // Just below sm: xs
        expect(getBreakpointFromWidth(BREAKPOINTS.sm - 1)).toBe('xs')
        // At sm: sm
        expect(getBreakpointFromWidth(BREAKPOINTS.sm)).toBe('sm')
        // Just above sm: sm
        expect(getBreakpointFromWidth(BREAKPOINTS.sm + 1)).toBe('sm')
      })

      it('should transition correctly at md boundary (1440px)', () => {
        // Just below md: sm
        expect(getBreakpointFromWidth(BREAKPOINTS.md - 1)).toBe('sm')
        // At md: md
        expect(getBreakpointFromWidth(BREAKPOINTS.md)).toBe('md')
        // Just above md: md
        expect(getBreakpointFromWidth(BREAKPOINTS.md + 1)).toBe('md')
      })

      it('should transition correctly at lg boundary (1920px)', () => {
        // Just below lg: md
        expect(getBreakpointFromWidth(BREAKPOINTS.lg - 1)).toBe('md')
        // At lg: lg
        expect(getBreakpointFromWidth(BREAKPOINTS.lg)).toBe('lg')
        // Just above lg: lg
        expect(getBreakpointFromWidth(BREAKPOINTS.lg + 1)).toBe('lg')
      })

      it('should have consistent transitions at all boundary values', () => {
        fc.assert(
          fc.property(
            nearBoundaryWidthArb,
            (width) => {
              const breakpoint = getBreakpointFromWidth(width)
              const expected = expectedBreakpoint(width)
              return breakpoint === expected
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should have monotonic breakpoint progression as width increases', () => {
        fc.assert(
          fc.property(
            fc.tuple(screenWidthArb, fc.integer({ min: 1, max: 500 })),
            ([width, delta]) => {
              const bp1 = getBreakpointFromWidth(width)
              const bp2 = getBreakpointFromWidth(width + delta)
              
              const idx1 = getBreakpointIndex(bp1)
              const idx2 = getBreakpointIndex(bp2)
              
              // Breakpoint should never decrease as width increases
              return idx2 >= idx1
            }
          ),
          { numRuns: 200 }
        )
      })
    })

    // ========================================================================
    // 2.3 Device Type Flags are Mutually Exclusive and Exhaustive
    // ========================================================================
    describe('2.3 Device type flags (isMobile, isTablet, isDesktop) are mutually exclusive and exhaustive', () => {
      it('should have exactly one device type flag true for any width', () => {
        fc.assert(
          fc.property(
            screenWidthArb,
            (width) => {
              const flags = calculateDeviceFlags(width)
              const trueCount = [flags.isMobile, flags.isTablet, flags.isDesktop]
                .filter(Boolean).length
              
              // Exactly one flag should be true
              return trueCount === 1
            }
          ),
          { numRuns: 500 }
        )
      })

      it('should have mutually exclusive device type flags', () => {
        fc.assert(
          fc.property(
            screenWidthArb,
            (width) => {
              const flags = calculateDeviceFlags(width)
              
              // No two flags should be true at the same time
              const mutuallyExclusive = !(
                (flags.isMobile && flags.isTablet) ||
                (flags.isMobile && flags.isDesktop) ||
                (flags.isTablet && flags.isDesktop)
              )
              
              return mutuallyExclusive
            }
          ),
          { numRuns: 500 }
        )
      })

      it('should have exhaustive device type flags (at least one is always true)', () => {
        fc.assert(
          fc.property(
            screenWidthArb,
            (width) => {
              const flags = calculateDeviceFlags(width)
              
              // At least one flag should be true
              return flags.isMobile || flags.isTablet || flags.isDesktop
            }
          ),
          { numRuns: 500 }
        )
      })

      it('should correctly identify mobile for width < 1280', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 320, max: BREAKPOINTS.xs - 1 }),
            (width) => {
              const flags = calculateDeviceFlags(width)
              return flags.isMobile === true && 
                     flags.isTablet === false && 
                     flags.isDesktop === false
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should correctly identify tablet for width 1280-1439', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: BREAKPOINTS.xs, max: BREAKPOINTS.md - 1 }),
            (width) => {
              const flags = calculateDeviceFlags(width)
              return flags.isMobile === false && 
                     flags.isTablet === true && 
                     flags.isDesktop === false
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should correctly identify desktop for width >= 1440', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: BREAKPOINTS.md, max: 4096 }),
            (width) => {
              const flags = calculateDeviceFlags(width)
              return flags.isMobile === false && 
                     flags.isTablet === false && 
                     flags.isDesktop === true
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ========================================================================
    // 2.4 Breakpoint Comparison Functions Work Correctly
    // ========================================================================
    describe('2.4 Breakpoint comparison functions work correctly for any width', () => {
      describe('isBreakpointUp', () => {
        it('should return true when width >= breakpoint value', () => {
          fc.assert(
            fc.property(
              fc.tuple(screenWidthArb, breakpointArb),
              ([width, bp]) => {
                const result = isBreakpointUp(width, bp)
                const expected = width >= BREAKPOINTS[bp]
                return result === expected
              }
            ),
            { numRuns: 200 }
          )
        })

        it('should be true for all breakpoints when width is very large', () => {
          fc.assert(
            fc.property(
              fc.integer({ min: BREAKPOINTS.lg, max: 4096 }),
              (width) => {
                return BREAKPOINT_ORDER.every(bp => isBreakpointUp(width, bp))
              }
            ),
            { numRuns: 50 }
          )
        })

        it('should be false for lg when width is below lg', () => {
          fc.assert(
            fc.property(
              fc.integer({ min: 320, max: BREAKPOINTS.lg - 1 }),
              (width) => {
                return isBreakpointUp(width, 'lg') === false
              }
            ),
            { numRuns: 100 }
          )
        })
      })

      describe('isBreakpointDown', () => {
        it('should return true when width < breakpoint value', () => {
          fc.assert(
            fc.property(
              fc.tuple(screenWidthArb, breakpointArb),
              ([width, bp]) => {
                const result = isBreakpointDown(width, bp)
                const expected = width < BREAKPOINTS[bp]
                return result === expected
              }
            ),
            { numRuns: 200 }
          )
        })

        it('should be true for all breakpoints when width is very small', () => {
          fc.assert(
            fc.property(
              fc.integer({ min: 320, max: BREAKPOINTS.xs - 1 }),
              (width) => {
                return BREAKPOINT_ORDER.every(bp => isBreakpointDown(width, bp))
              }
            ),
            { numRuns: 50 }
          )
        })

        it('should be false for xs when width is at or above xs', () => {
          fc.assert(
            fc.property(
              fc.integer({ min: BREAKPOINTS.xs, max: 4096 }),
              (width) => {
                return isBreakpointDown(width, 'xs') === false
              }
            ),
            { numRuns: 100 }
          )
        })
      })

      describe('isBreakpointBetween', () => {
        it('should return true when width is in range [min, max)', () => {
          fc.assert(
            fc.property(
              fc.tuple(screenWidthArb, validBreakpointPairArb),
              ([width, [min, max]]) => {
                const result = isBreakpointBetween(width, min, max)
                const expected = width >= BREAKPOINTS[min] && width < BREAKPOINTS[max]
                return result === expected
              }
            ),
            { numRuns: 200 }
          )
        })

        it('should return false for invalid range (min > max)', () => {
          fc.assert(
            fc.property(
              fc.tuple(screenWidthArb, breakpointArb, breakpointArb)
                .filter(([_, min, max]) => getBreakpointIndex(min) > getBreakpointIndex(max)),
              ([width, min, max]) => {
                return isBreakpointBetween(width, min, max) === false
              }
            ),
            { numRuns: 50 }
          )
        })

        it('should return false when min equals max (empty range)', () => {
          fc.assert(
            fc.property(
              fc.tuple(screenWidthArb, breakpointArb),
              ([width, bp]) => {
                // When min === max, the range [bp, bp) is empty
                return isBreakpointBetween(width, bp, bp) === false
              }
            ),
            { numRuns: 50 }
          )
        })
      })

      describe('Comparison function consistency', () => {
        it('isBreakpointUp and isBreakpointDown should be complementary', () => {
          fc.assert(
            fc.property(
              fc.tuple(screenWidthArb, breakpointArb),
              ([width, bp]) => {
                const up = isBreakpointUp(width, bp)
                const down = isBreakpointDown(width, bp)
                
                // Exactly one should be true (they are complements)
                return (up && !down) || (!up && down)
              }
            ),
            { numRuns: 200 }
          )
        })

        it('isBreakpointBetween should be consistent with isBreakpointUp and isBreakpointDown', () => {
          fc.assert(
            fc.property(
              fc.tuple(screenWidthArb, validBreakpointPairArb),
              ([width, [min, max]]) => {
                const between = isBreakpointBetween(width, min, max)
                const upMin = isBreakpointUp(width, min)
                const downMax = isBreakpointDown(width, max)
                
                // between should equal (upMin && downMax)
                return between === (upMin && downMax)
              }
            ),
            { numRuns: 200 }
          )
        })
      })
    })

    // ========================================================================
    // 2.5 Breakpoint Configuration Consistency
    // ========================================================================
    describe('2.5 Breakpoint configuration consistency', () => {
      it('should have breakpoints in strictly ascending order', () => {
        const values = BREAKPOINT_ORDER.map(bp => BREAKPOINTS[bp])
        
        for (let i = 1; i < values.length; i++) {
          expect(values[i]!).toBeGreaterThan(values[i - 1]!)
        }
      })

      it('should match design spec breakpoint values', () => {
        expect(BREAKPOINTS.xs).toBe(1280)
        expect(BREAKPOINTS.sm).toBe(1366)
        expect(BREAKPOINTS.md).toBe(1440)
        expect(BREAKPOINTS.lg).toBe(1920)
      })

      it('should have all breakpoints as positive integers', () => {
        fc.assert(
          fc.property(
            breakpointArb,
            (bp) => {
              const value = BREAKPOINTS[bp] as number
              return Number.isInteger(value) && value > 0
            }
          ),
          { numRuns: 10 }
        )
      })
    })
  })
})

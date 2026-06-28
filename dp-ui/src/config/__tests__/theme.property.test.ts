/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 主题配置完整性属性测试
 * Feature: platform-deep-optimization
 * Property 1: 主题配置完整性
 * 
 * **Validates: Requirements 1.1**
 * 
 * THE Theme_Engine SHALL 提供统一的设计语言配置，确保所有页面组件使用一致的颜色、间距、圆角和阴影样式
 * 
 * Feature: mars-integration-optimization, Property 7: 暗色模式主题覆盖正确性
 * **Validates: Requirements 2.1, 2.2**
 */

import { describe, it, expect, beforeEach } from 'vitest'
import * as fc from 'fast-check'
import { createPinia, setActivePinia } from 'pinia'
import { useThemeStore, type ThemeMode } from '@/stores/theme'
import { themeConfig } from '../theme'
import type {
  ThemeConfig,
  BrandConfig,
  ColorConfig,
  SpacingConfig,
  BorderRadiusConfig,
  ShadowConfig,
  TypographyConfig,
  TransitionConfig,
  BreakpointConfig,
  AccessibilityConfig,
  AnimationConfig
} from '@/types/theme'

// ============================================================================
// Validation Helper Functions
// ============================================================================

/**
 * 验证是否为有效的 CSS 颜色值
 * 支持: hex (#RGB, #RRGGBB, #RRGGBBAA), rgb(), rgba(), hsl(), hsla(), 颜色名称
 */
function isValidCssColor(value: string): boolean {
  if (!value || typeof value !== 'string') return false
  
  // Hex color: #RGB, #RRGGBB, #RRGGBBAA
  const hexPattern = /^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$/
  
  // RGB/RGBA: rgb(r, g, b) or rgba(r, g, b, a)
  const rgbPattern = /^rgba?\(\s*\d{1,3}\s*,\s*\d{1,3}\s*,\s*\d{1,3}\s*(,\s*(0|1|0?\.\d+))?\s*\)$/
  
  // HSL/HSLA: hsl(h, s%, l%) or hsla(h, s%, l%, a)
  const hslPattern = /^hsla?\(\s*\d{1,3}\s*,\s*\d{1,3}%\s*,\s*\d{1,3}%\s*(,\s*(0|1|0?\.\d+))?\s*\)$/
  
  // CSS color names (common ones)
  const colorNames = [
    'transparent', 'currentColor', 'inherit',
    'black', 'white', 'red', 'green', 'blue', 'yellow', 'orange', 'purple',
    'gray', 'grey', 'pink', 'brown', 'cyan', 'magenta'
  ]
  
  return (
    hexPattern.test(value) ||
    rgbPattern.test(value) ||
    hslPattern.test(value) ||
    colorNames.includes(value.toLowerCase())
  )
}

/**
 * 验证是否为有效的 CSS 渐变值
 */
function isValidCssGradient(value: string): boolean {
  if (!value || typeof value !== 'string') return false
  
  // Linear gradient: linear-gradient(...)
  const linearPattern = /^linear-gradient\(.+\)$/
  
  // Radial gradient: radial-gradient(...)
  const radialPattern = /^radial-gradient\(.+\)$/
  
  // Conic gradient: conic-gradient(...)
  const conicPattern = /^conic-gradient\(.+\)$/
  
  return linearPattern.test(value) || radialPattern.test(value) || conicPattern.test(value)
}

/**
 * 验证是否为有效的 CSS 单位值 (px, rem, em, %, vh, vw, etc.)
 */
function isValidCssUnit(value: string): boolean {
  if (!value || typeof value !== 'string') return false
  
  // Number with unit: 10px, 1.5rem, 100%, etc.
  const unitPattern = /^-?\d+(\.\d+)?(px|rem|em|%|vh|vw|vmin|vmax|ch|ex|pt|pc|in|cm|mm)$/
  
  // Pure number (for unitless values like line-height)
  const numberPattern = /^-?\d+(\.\d+)?$/
  
  // Special values
  const specialValues = ['auto', 'inherit', 'initial', 'unset', '0']
  
  return unitPattern.test(value) || numberPattern.test(value) || specialValues.includes(value)
}

/**
 * 验证是否为有效的 CSS 时间值 (ms, s)
 */
function isValidCssTime(value: string): boolean {
  if (!value || typeof value !== 'string') return false
  
  // Time with unit: 200ms, 0.3s, etc.
  const timePattern = /^\d+(\.\d+)?(ms|s)$/
  
  return timePattern.test(value)
}

/**
 * 验证是否为有效的 CSS 过渡值
 */
function isValidCssTransition(value: string): boolean {
  if (!value || typeof value !== 'string') return false
  
  // Transition: duration [timing-function]
  // e.g., "200ms ease", "0.3s cubic-bezier(0.4, 0, 0.2, 1)"
  const transitionPattern = /^\d+(\.\d+)?(ms|s)\s+(ease|ease-in|ease-out|ease-in-out|linear|cubic-bezier\([^)]+\)|step-start|step-end|steps\([^)]+\))$/
  
  return transitionPattern.test(value)
}

/**
 * 验证是否为有效的 CSS 阴影值
 */
function isValidCssShadow(value: string): boolean {
  if (!value || typeof value !== 'string') return false
  
  // Special values
  if (value === 'none' || value === 'inherit' || value === 'initial') return true
  
  // Box shadow pattern: offset-x offset-y [blur-radius] [spread-radius] color
  // Can have multiple shadows separated by comma
  // Also supports 'inset' keyword
  const shadowPattern = /^(inset\s+)?-?\d+(\.\d+)?(px|rem|em)?\s+-?\d+(\.\d+)?(px|rem|em)?(\s+-?\d+(\.\d+)?(px|rem|em)?)?(\s+-?\d+(\.\d+)?(px|rem|em)?)?\s+(#[0-9A-Fa-f]{3,8}|rgba?\([^)]+\)|[a-z]+)/i
  
  // Simple check: contains offset values and color
  return shadowPattern.test(value) || value.includes('rgba') || value.includes('rgb')
}

/**
 * 验证是否为正数
 */
function isPositiveNumber(value: number): boolean {
  return typeof value === 'number' && !isNaN(value) && value > 0
}

// ============================================================================
// Property-Based Tests
// ============================================================================

describe('Theme Configuration Property Tests', () => {
  /**
   * Property 1: 主题配置完整性
   * 
   * 验证主题配置包含所有必需的属性（brand, colors, spacing, borderRadius, 
   * shadows, typography, transitions, breakpoints, accessibility, animations），
   * 且所有值都是有效的。
   * 
   * **Validates: Requirements 1.1**
   */
  describe('Property 1: Theme Configuration Completeness', () => {
    // ========================================================================
    // 1. All Required Theme Properties Exist
    // ========================================================================
    describe('1.1 All required theme properties exist', () => {
      const requiredTopLevelProperties: (keyof ThemeConfig)[] = [
        'brand',
        'colors',
        'spacing',
        'borderRadius',
        'shadows',
        'typography',
        'transitions',
        'breakpoints',
        'accessibility',
        'animations'
      ]

      it('should have all required top-level properties', () => {
        requiredTopLevelProperties.forEach(prop => {
          expect(themeConfig).toHaveProperty(prop)
          expect(themeConfig[prop]).toBeDefined()
          expect(themeConfig[prop]).not.toBeNull()
        })
      })

      it('should have all required brand properties', () => {
        const requiredBrandProps: (keyof BrandConfig)[] = ['name', 'slogan', 'fullName', 'copyright']
        requiredBrandProps.forEach(prop => {
          expect(themeConfig.brand).toHaveProperty(prop)
          expect(typeof themeConfig.brand[prop]).toBe('string')
          expect(themeConfig.brand[prop].length).toBeGreaterThan(0)
        })
      })

      it('should have all required color properties', () => {
        const requiredColorProps = ['primary', 'secondary', 'success', 'warning', 'error', 'info']
        requiredColorProps.forEach(prop => {
          expect(themeConfig.colors).toHaveProperty(prop)
        })
        expect(themeConfig.colors).toHaveProperty('gradients')
        expect(themeConfig.colors).toHaveProperty('background')
        expect(themeConfig.colors).toHaveProperty('text')
        expect(themeConfig.colors).toHaveProperty('border')
      })

      it('should have all required spacing properties', () => {
        const requiredSpacingProps: (keyof SpacingConfig)[] = ['xs', 'sm', 'md', 'lg', 'xl', 'xxl']
        requiredSpacingProps.forEach(prop => {
          expect(themeConfig.spacing).toHaveProperty(prop)
        })
      })

      it('should have all required borderRadius properties', () => {
        const requiredRadiusProps: (keyof BorderRadiusConfig)[] = ['sm', 'md', 'lg', 'xl', 'full']
        requiredRadiusProps.forEach(prop => {
          expect(themeConfig.borderRadius).toHaveProperty(prop)
        })
      })

      it('should have all required shadow properties', () => {
        const requiredShadowProps: (keyof ShadowConfig)[] = ['sm', 'md', 'lg', 'xl', 'inner', 'none']
        requiredShadowProps.forEach(prop => {
          expect(themeConfig.shadows).toHaveProperty(prop)
        })
      })

      it('should have all required typography properties', () => {
        expect(themeConfig.typography).toHaveProperty('fontFamily')
        expect(themeConfig.typography).toHaveProperty('fontSize')
        expect(themeConfig.typography).toHaveProperty('fontWeight')
      })

      it('should have all required transition properties', () => {
        const requiredTransitionProps: (keyof TransitionConfig)[] = ['fast', 'base', 'slow']
        requiredTransitionProps.forEach(prop => {
          expect(themeConfig.transitions).toHaveProperty(prop)
        })
      })

      it('should have all required breakpoint properties', () => {
        const requiredBreakpointProps: (keyof BreakpointConfig)[] = ['xs', 'sm', 'md', 'lg']
        requiredBreakpointProps.forEach(prop => {
          expect(themeConfig.breakpoints).toHaveProperty(prop)
        })
      })

      it('should have all required accessibility properties', () => {
        const requiredA11yProps: (keyof AccessibilityConfig)[] = [
          'highContrast',
          'reducedMotion',
          'focusRingWidth',
          'focusRingColor'
        ]
        requiredA11yProps.forEach(prop => {
          expect(themeConfig.accessibility).toHaveProperty(prop)
        })
      })

      it('should have all required animation properties', () => {
        const requiredAnimationProps: (keyof AnimationConfig)[] = ['hover', 'expand', 'loading']
        requiredAnimationProps.forEach(prop => {
          expect(themeConfig.animations).toHaveProperty(prop)
          expect(themeConfig.animations[prop]).toHaveProperty('duration')
          expect(themeConfig.animations[prop]).toHaveProperty('easing')
        })
      })
    })

    // ========================================================================
    // 2. Color Values are Valid CSS Colors
    // ========================================================================
    describe('1.2 Color values are valid CSS colors', () => {
      it('should have valid primary colors', () => {
        const primaryColors = ['primary', 'secondary', 'success', 'warning', 'error', 'info'] as const
        primaryColors.forEach(colorKey => {
          const color = themeConfig.colors[colorKey]
          expect(isValidCssColor(color)).toBe(true)
        })
      })

      it('should have valid gradient values', () => {
        const gradients = themeConfig.colors.gradients
        Object.entries(gradients).forEach(([key, value]) => {
          expect(isValidCssGradient(value)).toBe(true)
        })
      })

      it('should have valid background colors', () => {
        const backgrounds = themeConfig.colors.background
        Object.entries(backgrounds).forEach(([key, value]) => {
          expect(isValidCssColor(value)).toBe(true)
        })
      })

      it('should have valid text colors', () => {
        const textColors = themeConfig.colors.text
        Object.entries(textColors).forEach(([key, value]) => {
          expect(isValidCssColor(value)).toBe(true)
        })
      })

      it('should have valid border colors', () => {
        const borderColors = themeConfig.colors.border
        Object.entries(borderColors).forEach(([key, value]) => {
          expect(isValidCssColor(value)).toBe(true)
        })
      })

      // Property-based test: any valid hex color should be recognized
      it('should recognize any valid hex color format', () => {
        // Generate valid hex characters (0-9, A-F)
        const hexCharArb = fc.constantFrom(
          '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
          'A', 'B', 'C', 'D', 'E', 'F', 'a', 'b', 'c', 'd', 'e', 'f'
        )
        const hexStringArb = fc.array(hexCharArb, { minLength: 6, maxLength: 6 })
          .map(chars => chars.join(''))

        fc.assert(
          fc.property(
            hexStringArb,
            (hex) => {
              const color = `#${hex}`
              return isValidCssColor(color) === true
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ========================================================================
    // 3. Spacing Values are Valid CSS Units
    // ========================================================================
    describe('1.3 Spacing values are valid CSS units', () => {
      it('should have valid spacing values', () => {
        const spacings = themeConfig.spacing
        Object.entries(spacings).forEach(([key, value]) => {
          expect(isValidCssUnit(value)).toBe(true)
        })
      })

      it('should have spacing values in ascending order', () => {
        const spacingOrder: (keyof SpacingConfig)[] = ['xs', 'sm', 'md', 'lg', 'xl', 'xxl']
        const spacingValues = spacingOrder.map(key => parseInt(themeConfig.spacing[key]))
        
        for (let i = 1; i < spacingValues.length; i++) {
          expect(spacingValues[i]).toBeGreaterThan(spacingValues[i - 1])
        }
      })

      // Property-based test: generated spacing values should be valid
      it('should validate any positive pixel value as valid spacing', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 1, max: 1000 }),
            (pixels) => {
              const spacing = `${pixels}px`
              return isValidCssUnit(spacing) === true
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ========================================================================
    // 4. Breakpoint Values are Positive Numbers
    // ========================================================================
    describe('1.4 Breakpoint values are positive numbers', () => {
      it('should have positive breakpoint values', () => {
        const breakpoints = themeConfig.breakpoints
        Object.entries(breakpoints).forEach(([key, value]) => {
          expect(isPositiveNumber(value)).toBe(true)
        })
      })

      it('should have breakpoint values in ascending order', () => {
        const breakpointOrder: (keyof BreakpointConfig)[] = ['xs', 'sm', 'md', 'lg']
        const breakpointValues = breakpointOrder.map(key => themeConfig.breakpoints[key])
        
        for (let i = 1; i < breakpointValues.length; i++) {
          expect(breakpointValues[i]).toBeGreaterThan(breakpointValues[i - 1])
        }
      })

      it('should have breakpoints matching design spec (1280/1366/1440/1920)', () => {
        expect(themeConfig.breakpoints.xs).toBe(1280)
        expect(themeConfig.breakpoints.sm).toBe(1366)
        expect(themeConfig.breakpoints.md).toBe(1440)
        expect(themeConfig.breakpoints.lg).toBe(1920)
      })

      // Property-based test: any positive integer should be a valid breakpoint
      it('should accept any positive integer as a valid breakpoint value', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 1, max: 10000 }),
            (value) => {
              return isPositiveNumber(value) === true
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ========================================================================
    // 5. Animation Durations are Valid CSS Time Values
    // ========================================================================
    describe('1.5 Animation durations are valid CSS time values', () => {
      it('should have valid hover animation duration', () => {
        expect(isValidCssTime(themeConfig.animations.hover.duration)).toBe(true)
      })

      it('should have valid expand animation duration', () => {
        expect(isValidCssTime(themeConfig.animations.expand.duration)).toBe(true)
      })

      it('should have valid loading animation duration', () => {
        expect(isValidCssTime(themeConfig.animations.loading.duration)).toBe(true)
      })

      it('should have hover animation duration not exceeding 200ms (per requirement 1.3)', () => {
        const duration = themeConfig.animations.hover.duration
        const durationMs = duration.endsWith('ms') 
          ? parseInt(duration) 
          : parseFloat(duration) * 1000
        expect(durationMs).toBeLessThanOrEqual(200)
      })

      it('should have valid transition values', () => {
        const transitions = themeConfig.transitions
        Object.entries(transitions).forEach(([key, value]) => {
          expect(isValidCssTransition(value)).toBe(true)
        })
      })

      // Property-based test: any valid time format should be recognized
      it('should recognize any valid millisecond time format', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 1, max: 10000 }),
            (ms) => {
              const time = `${ms}ms`
              return isValidCssTime(time) === true
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should recognize any valid second time format', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 1, max: 100 }),
            (tenths) => {
              // Generate values like 0.1s, 0.2s, ..., 10.0s
              const s = tenths / 10
              const time = `${s.toFixed(1)}s`
              return isValidCssTime(time) === true
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ========================================================================
    // Additional Validation Tests
    // ========================================================================
    describe('1.6 Additional theme validations', () => {
      it('should have valid border radius values', () => {
        const radiuses = themeConfig.borderRadius
        Object.entries(radiuses).forEach(([key, value]) => {
          expect(isValidCssUnit(value)).toBe(true)
        })
      })

      it('should have valid shadow values', () => {
        const shadows = themeConfig.shadows
        Object.entries(shadows).forEach(([key, value]) => {
          expect(isValidCssShadow(value)).toBe(true)
        })
      })

      it('should have valid font size values', () => {
        const fontSizes = themeConfig.typography.fontSize
        Object.entries(fontSizes).forEach(([key, value]) => {
          expect(isValidCssUnit(value)).toBe(true)
        })
      })

      it('should have valid font weight values', () => {
        const fontWeights = themeConfig.typography.fontWeight
        Object.entries(fontWeights).forEach(([key, value]) => {
          expect(typeof value).toBe('number')
          expect(value).toBeGreaterThanOrEqual(100)
          expect(value).toBeLessThanOrEqual(900)
        })
      })

      it('should have valid focus ring configuration', () => {
        expect(isValidCssUnit(themeConfig.accessibility.focusRingWidth)).toBe(true)
        expect(isValidCssColor(themeConfig.accessibility.focusRingColor)).toBe(true)
      })

      it('should have boolean accessibility flags', () => {
        expect(typeof themeConfig.accessibility.highContrast).toBe('boolean')
        expect(typeof themeConfig.accessibility.reducedMotion).toBe('boolean')
      })
    })
  })

  // ==========================================================================
  // Property-Based Tests for Theme Configuration Generation
  // ==========================================================================
  describe('Property-Based Theme Validation', () => {
    /**
     * Property: Theme configuration should be immutable-safe
     * Any deep copy of the theme should have identical structure
     */
    it('should maintain structure integrity through deep copy', () => {
      fc.assert(
        fc.property(
          fc.constant(themeConfig),
          (config) => {
            const copy = JSON.parse(JSON.stringify(config))
            
            // Verify all top-level keys exist
            const originalKeys = Object.keys(config).sort()
            const copyKeys = Object.keys(copy).sort()
            
            return JSON.stringify(originalKeys) === JSON.stringify(copyKeys)
          }
        ),
        { numRuns: 10 }
      )
    })

    /**
     * Property: All color values in theme should be valid
     */
    it('should have all nested color values as valid CSS colors', () => {
      const collectColors = (obj: any, colors: string[] = []): string[] => {
        for (const key in obj) {
          const value = obj[key]
          if (typeof value === 'string' && (
            value.startsWith('#') || 
            value.startsWith('rgb') || 
            value.startsWith('hsl')
          )) {
            colors.push(value)
          } else if (typeof value === 'object' && value !== null) {
            collectColors(value, colors)
          }
        }
        return colors
      }

      const allColors = collectColors(themeConfig.colors)
      
      fc.assert(
        fc.property(
          fc.constantFrom(...allColors),
          (color) => {
            return isValidCssColor(color) || isValidCssGradient(color)
          }
        ),
        { numRuns: allColors.length }
      )
    })

    /**
     * Property: Spacing scale should be monotonically increasing
     */
    it('should have monotonically increasing spacing values', () => {
      const spacingKeys: (keyof SpacingConfig)[] = ['xs', 'sm', 'md', 'lg', 'xl', 'xxl']
      const spacingValues = spacingKeys.map(key => parseInt(themeConfig.spacing[key]))
      
      fc.assert(
        fc.property(
          fc.integer({ min: 0, max: spacingValues.length - 2 }),
          (index) => {
            return spacingValues[index] < spacingValues[index + 1]
          }
        ),
        { numRuns: spacingValues.length - 1 }
      )
    })
  })
})

// ============================================================================
// Property 7: 暗色模式主题覆盖正确性
// Feature: mars-integration-optimization, Property 7: 暗色模式主题覆盖正确性
// **Validates: Requirements 2.1, 2.2**
//
// *For any* 暗色模式状态（isDark=true），theme store 的 `themeOverrides` 计算属性
// 应返回包含正确暗色背景值的配置：cardColor=#1f1f1f、modalColor=#1f1f1f、
// popoverColor=#1f1f1f、tableColor=#1f1f1f。
// ============================================================================

describe('Property 7: 暗色模式主题覆盖正确性', () => {
  // Expected dark mode color values from Requirements 2.1, 2.2
  const DARK_CARD_COLOR = '#1f1f1f'
  const DARK_MODAL_COLOR = '#1f1f1f'
  const DARK_POPOVER_COLOR = '#1f1f1f'
  const DARK_TABLE_COLOR = '#1f1f1f'
  const DARK_BODY_COLOR = '#0f172a'

  beforeEach(() => {
    // Mock window.matchMedia for jsdom environment
    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: (query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: () => {},
        removeListener: () => {},
        addEventListener: () => {},
        removeEventListener: () => {},
        dispatchEvent: () => false,
      }),
    })
    setActivePinia(createPinia())
  })

  /**
   * Property test: For any dark mode activation path (mode='dark' or mode='system'
   * with system preferring dark), the themeOverrides must contain the correct
   * dark background values for card, modal, popover, and table.
   */
  it('should return correct dark mode overrides for any dark mode state', () => {
    fc.assert(
      fc.property(
        // Generate various dark mode activation scenarios
        fc.record({
          mode: fc.constantFrom<ThemeMode>('dark', 'system'),
          // Additional arbitrary data to increase iteration diversity
          iteration: fc.integer({ min: 0, max: 1000 })
        }),
        ({ mode }) => {
          const pinia = createPinia()
          setActivePinia(pinia)
          const themeStore = useThemeStore()

          // Set mode to dark (or system - we'll force system dark below)
          themeStore.setTheme(mode)

          // For 'system' mode, isDark depends on systemDark which defaults
          // to window.matchMedia result. In jsdom it's false, so we only
          // assert for mode='dark' where isDark is guaranteed true.
          // For 'system' mode, we skip since we can't control matchMedia in property test.
          if (mode === 'system' && !themeStore.isDark) {
            return true // skip non-dark system scenarios
          }

          if (!themeStore.isDark) {
            return true // only test dark mode
          }

          const overrides = themeStore.themeOverrides
          const common = overrides.common

          // Verify all required dark mode background colors
          return (
            common?.cardColor === DARK_CARD_COLOR &&
            common?.modalColor === DARK_MODAL_COLOR &&
            common?.popoverColor === DARK_POPOVER_COLOR &&
            common?.tableColor === DARK_TABLE_COLOR &&
            common?.bodyColor === DARK_BODY_COLOR
          )
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Property test: When isDark is true and highContrast is false,
   * the dark mode overrides should always include all four required color keys.
   */
  it('should include all required dark color keys in themeOverrides.common', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: 999 }),
        () => {
          const pinia = createPinia()
          setActivePinia(pinia)
          const themeStore = useThemeStore()

          themeStore.setTheme('dark')

          const overrides = themeStore.themeOverrides
          const common = overrides.common

          // All four keys must be present
          return (
            'cardColor' in (common || {}) &&
            'modalColor' in (common || {}) &&
            'popoverColor' in (common || {}) &&
            'tableColor' in (common || {})
          )
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Property test: Dark mode overrides should also include inputColor and bodyColor
   * as specified in the theme store implementation.
   */
  it('should include bodyColor and inputColor in dark mode overrides', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: 999 }),
        () => {
          const pinia = createPinia()
          setActivePinia(pinia)
          const themeStore = useThemeStore()

          themeStore.setTheme('dark')

          const common = themeStore.themeOverrides.common

          return (
            common?.bodyColor === DARK_BODY_COLOR &&
            common?.inputColor === '#1f1f1f'
          )
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Property test: In light mode (isDark=false), the themeOverrides should NOT
   * contain dark-specific background colors (cardColor, modalColor, etc.).
   * This is the contrapositive: light mode must not have dark overrides.
   */
  it('should NOT include dark background colors when in light mode', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: 999 }),
        () => {
          const pinia = createPinia()
          setActivePinia(pinia)
          const themeStore = useThemeStore()

          themeStore.setTheme('light')

          const common = themeStore.themeOverrides.common

          // In light mode without high contrast, these dark-specific keys should not exist
          return (
            !('cardColor' in (common || {})) &&
            !('modalColor' in (common || {})) &&
            !('popoverColor' in (common || {})) &&
            !('tableColor' in (common || {})) &&
            !('bodyColor' in (common || {}))
          )
        }
      ),
      { numRuns: 100 }
    )
  })
})

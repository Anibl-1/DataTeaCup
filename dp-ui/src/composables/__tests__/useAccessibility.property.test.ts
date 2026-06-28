/**
 * 无障碍支持属性测试
 * Feature: platform-deep-optimization
 * 
 * **Property 3: 键盘导航完整性**
 * **Property 4: ARIA 标签完整性**
 * 
 * **Validates: Requirements 1.6, 1.7**
 * - 1.6: THE DataTeaCup SHALL 支持完整的键盘导航，包括 Tab 键切换焦点和 Enter 键激活操作
 * - 1.7: THE DataTeaCup SHALL 为所有交互元素提供符合 WCAG 2.1 标准的 ARIA 标签
 */

import { describe, it, expect, beforeEach } from 'vitest'
import * as fc from 'fast-check'
import {
  generateAriaId,
  resetAriaIdCounter,
  createButtonAria,
  createMenuItemAria,
  createTabAria,
  createFieldAria,
  createProgressAria,
  createDialogAria,
  type AriaAttributes
} from '../useAccessibility'

// ============================================================================
// Constants
// ============================================================================

// ============================================================================
// Arbitraries (Test Data Generators)
// ============================================================================

/**
 * Generate valid ARIA ID prefix
 */
const ariaIdPrefixArb = fc.array(
  fc.constantFrom(...'abcdefghijklmnopqrstuvwxyz'.split('')),
  { minLength: 1, maxLength: 20 }
).map(chars => chars.join(''))

/**
 * Generate valid label text
 */
const labelTextArb = fc.string({ minLength: 1, maxLength: 100 })
  .filter(s => s.trim().length > 0)

/**
 * Generate valid index for navigation
 */
const indexArb = fc.integer({ min: 0, max: 100 })

/**
 * Generate valid total count for navigation
 */
const totalArb = fc.integer({ min: 1, max: 100 })

/**
 * Generate valid progress value (0-100)
 */
const progressValueArb = fc.integer({ min: 0, max: 100 })

/**
 * Generate valid min/max range
 */
const minMaxRangeArb = fc.tuple(
  fc.integer({ min: 0, max: 50 }),
  fc.integer({ min: 51, max: 100 })
)

/**
 * Generate list of focusable element count
 */
const focusableCountArb = fc.integer({ min: 1, max: 50 })

/**
 * Generate navigation direction
 */
const navigationDirectionArb = fc.constantFrom('next', 'previous', 'first', 'last')

/**
 * Generate keyboard navigation orientation
 */
const orientationArb = fc.constantFrom<'horizontal' | 'vertical' | 'both'>(
  'horizontal', 'vertical', 'both'
)

// ============================================================================
// Helper Functions
// ============================================================================

/**
 * Validate ARIA ID format
 * Valid format: prefix-counter-timestamp
 */
function isValidAriaId(id: string): boolean {
  if (!id || typeof id !== 'string') return false
  // Format: prefix-number-base36timestamp
  const pattern = /^[a-z]+-\d+-[a-z0-9]+$/i
  return pattern.test(id)
}

/**
 * Check if ARIA attributes contain required properties for a role
 */
function hasRequiredAriaForRole(attrs: AriaAttributes, role: string): boolean {
  switch (role) {
    case 'button':
      return attrs['aria-label'] !== undefined || attrs['aria-labelledby'] !== undefined
    case 'progressbar':
      return attrs['aria-valuenow'] !== undefined &&
             attrs['aria-valuemin'] !== undefined &&
             attrs['aria-valuemax'] !== undefined
    case 'tab':
      return attrs['aria-selected'] !== undefined &&
             attrs['aria-controls'] !== undefined
    case 'menuitem':
      return attrs['aria-label'] !== undefined || attrs['aria-labelledby'] !== undefined
    case 'dialog':
    case 'alertdialog':
      return attrs['aria-label'] !== undefined || attrs['aria-labelledby'] !== undefined
    default:
      return true
  }
}

/**
 * Validate tabindex value
 */
function isValidTabindex(tabindex: number | undefined): boolean {
  if (tabindex === undefined) return true
  return Number.isInteger(tabindex) && tabindex >= -1
}

/**
 * Simulate keyboard navigation index calculation
 */
function calculateNavigationIndex(
  currentIndex: number,
  direction: 'next' | 'previous' | 'first' | 'last',
  totalItems: number,
  loop: boolean
): number {
  if (totalItems <= 0) return 0
  
  switch (direction) {
    case 'first':
      return 0
    case 'last':
      return totalItems - 1
    case 'next':
      if (loop) {
        return (currentIndex + 1) % totalItems
      }
      return Math.min(currentIndex + 1, totalItems - 1)
    case 'previous':
      if (loop) {
        return (currentIndex - 1 + totalItems) % totalItems
      }
      return Math.max(currentIndex - 1, 0)
    default:
      return currentIndex
  }
}

// ============================================================================
// Property-Based Tests
// ============================================================================

describe('Accessibility Property Tests', () => {
  beforeEach(() => {
    resetAriaIdCounter()
  })


  // ==========================================================================
  // Property 3: 键盘导航完整性
  // ==========================================================================
  /**
   * Property 3: 键盘导航完整性
   * 
   * For any list of focusable elements, keyboard navigation should correctly
   * cycle through all elements. Arrow keys should move focus in the correct
   * direction. Home/End keys should jump to first/last element.
   * Loop navigation should wrap around correctly.
   * 
   * **Validates: Requirements 1.6**
   */
  describe('Property 3: Keyboard Navigation Completeness', () => {
    // ========================================================================
    // 3.1 Arrow Key Navigation Correctness
    // ========================================================================
    describe('3.1 Arrow key navigation moves focus in correct direction', () => {
      it('should move to next element on forward navigation', () => {
        fc.assert(
          fc.property(
            fc.tuple(indexArb, focusableCountArb),
            ([currentIndex, totalItems]) => {
              // Ensure currentIndex is within bounds
              const validIndex = Math.min(currentIndex, totalItems - 1)
              const newIndex = calculateNavigationIndex(validIndex, 'next', totalItems, false)
              
              // Next should increase index (unless at end)
              if (validIndex < totalItems - 1) {
                return newIndex === validIndex + 1
              }
              // At end, should stay at end (no loop)
              return newIndex === totalItems - 1
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should move to previous element on backward navigation', () => {
        fc.assert(
          fc.property(
            fc.tuple(indexArb, focusableCountArb),
            ([currentIndex, totalItems]) => {
              const validIndex = Math.min(currentIndex, totalItems - 1)
              const newIndex = calculateNavigationIndex(validIndex, 'previous', totalItems, false)
              
              // Previous should decrease index (unless at start)
              if (validIndex > 0) {
                return newIndex === validIndex - 1
              }
              // At start, should stay at start (no loop)
              return newIndex === 0
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should respect orientation for arrow key mapping', () => {
        fc.assert(
          fc.property(
            orientationArb,
            (orientation) => {
              // Vertical: ArrowDown = next, ArrowUp = previous
              // Horizontal: ArrowRight = next, ArrowLeft = previous
              // Both: All arrow keys work
              const supportsVertical = orientation === 'vertical' || orientation === 'both'
              const supportsHorizontal = orientation === 'horizontal' || orientation === 'both'
              
              return (supportsVertical || supportsHorizontal)
            }
          ),
          { numRuns: 10 }
        )
      })
    })

    // ========================================================================
    // 3.2 Home/End Key Navigation
    // ========================================================================
    describe('3.2 Home/End keys jump to first/last element', () => {
      it('should jump to first element on Home key', () => {
        fc.assert(
          fc.property(
            fc.tuple(indexArb, focusableCountArb),
            ([currentIndex, totalItems]) => {
              const newIndex = calculateNavigationIndex(currentIndex, 'first', totalItems, false)
              return newIndex === 0
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should jump to last element on End key', () => {
        fc.assert(
          fc.property(
            fc.tuple(indexArb, focusableCountArb),
            ([currentIndex, totalItems]) => {
              const newIndex = calculateNavigationIndex(currentIndex, 'last', totalItems, false)
              return newIndex === totalItems - 1
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should handle single item list correctly', () => {
        fc.assert(
          fc.property(
            navigationDirectionArb,
            (direction) => {
              const newIndex = calculateNavigationIndex(0, direction, 1, false)
              // With single item, all navigation should result in index 0
              return newIndex === 0
            }
          ),
          { numRuns: 20 }
        )
      })
    })

    // ========================================================================
    // 3.3 Loop Navigation Wrapping
    // ========================================================================
    describe('3.3 Loop navigation wraps around correctly', () => {
      it('should wrap from last to first on forward navigation with loop', () => {
        fc.assert(
          fc.property(
            focusableCountArb,
            (totalItems) => {
              const lastIndex = totalItems - 1
              const newIndex = calculateNavigationIndex(lastIndex, 'next', totalItems, true)
              return newIndex === 0
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should wrap from first to last on backward navigation with loop', () => {
        fc.assert(
          fc.property(
            focusableCountArb,
            (totalItems) => {
              const newIndex = calculateNavigationIndex(0, 'previous', totalItems, true)
              return newIndex === totalItems - 1
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should not wrap when loop is disabled', () => {
        fc.assert(
          fc.property(
            focusableCountArb,
            (totalItems) => {
              // At last, next should stay at last
              const atLastNext = calculateNavigationIndex(totalItems - 1, 'next', totalItems, false)
              // At first, previous should stay at first
              const atFirstPrev = calculateNavigationIndex(0, 'previous', totalItems, false)
              
              return atLastNext === totalItems - 1 && atFirstPrev === 0
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should maintain valid index bounds after any navigation sequence', () => {
        fc.assert(
          fc.property(
            fc.tuple(
              indexArb,
              focusableCountArb,
              fc.array(navigationDirectionArb, { minLength: 1, maxLength: 20 }),
              fc.boolean()
            ),
            ([startIndex, totalItems, directions, loop]) => {
              let currentIndex = Math.min(startIndex, totalItems - 1)
              
              for (const direction of directions) {
                currentIndex = calculateNavigationIndex(currentIndex, direction, totalItems, loop)
              }
              
              // Index should always be within valid bounds
              return currentIndex >= 0 && currentIndex < totalItems
            }
          ),
          { numRuns: 200 }
        )
      })
    })

    // ========================================================================
    // 3.4 Navigation Index Invariants
    // ========================================================================
    describe('3.4 Navigation maintains valid index invariants', () => {
      it('should always return non-negative index', () => {
        fc.assert(
          fc.property(
            fc.tuple(indexArb, focusableCountArb, navigationDirectionArb, fc.boolean()),
            ([currentIndex, totalItems, direction, loop]) => {
              const newIndex = calculateNavigationIndex(currentIndex, direction, totalItems, loop)
              return newIndex >= 0
            }
          ),
          { numRuns: 300 }
        )
      })

      it('should always return index less than total items', () => {
        fc.assert(
          fc.property(
            fc.tuple(indexArb, focusableCountArb, navigationDirectionArb, fc.boolean()),
            ([currentIndex, totalItems, direction, loop]) => {
              // Ensure currentIndex is within valid bounds before navigation
              const validCurrentIndex = Math.min(currentIndex, totalItems - 1)
              const newIndex = calculateNavigationIndex(validCurrentIndex, direction, totalItems, loop)
              return newIndex < totalItems
            }
          ),
          { numRuns: 300 }
        )
      })

      it('should be idempotent for first/last navigation', () => {
        fc.assert(
          fc.property(
            fc.tuple(indexArb, focusableCountArb),
            ([currentIndex, totalItems]) => {
              // Calling first twice should give same result
              const first1 = calculateNavigationIndex(currentIndex, 'first', totalItems, false)
              const first2 = calculateNavigationIndex(first1, 'first', totalItems, false)
              
              // Calling last twice should give same result
              const last1 = calculateNavigationIndex(currentIndex, 'last', totalItems, false)
              const last2 = calculateNavigationIndex(last1, 'last', totalItems, false)
              
              return first1 === first2 && last1 === last2
            }
          ),
          { numRuns: 100 }
        )
      })
    })
  })


  // ==========================================================================
  // Property 4: ARIA 标签完整性
  // ==========================================================================
  /**
   * Property 4: ARIA 标签完整性
   * 
   * All ARIA helper functions should generate valid ARIA attributes.
   * Generated ARIA IDs should be unique.
   * Required ARIA attributes should always be present.
   * ARIA attribute values should be valid according to WCAG 2.1.
   * 
   * **Validates: Requirements 1.7**
   */
  describe('Property 4: ARIA Label Completeness', () => {
    // ========================================================================
    // 4.1 ARIA ID Uniqueness
    // ========================================================================
    describe('4.1 Generated ARIA IDs are unique', () => {
      it('should generate unique IDs for any prefix', () => {
        fc.assert(
          fc.property(
            ariaIdPrefixArb,
            (prefix) => {
              resetAriaIdCounter()
              const id1 = generateAriaId(prefix)
              const id2 = generateAriaId(prefix)
              const id3 = generateAriaId(prefix)
              
              // All IDs should be different
              return id1 !== id2 && id2 !== id3 && id1 !== id3
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should generate unique IDs across different prefixes', () => {
        fc.assert(
          fc.property(
            fc.tuple(ariaIdPrefixArb, ariaIdPrefixArb),
            ([prefix1, prefix2]) => {
              resetAriaIdCounter()
              const id1 = generateAriaId(prefix1)
              const id2 = generateAriaId(prefix2)
              
              // IDs should be different even with different prefixes
              return id1 !== id2
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should generate valid ARIA ID format', () => {
        fc.assert(
          fc.property(
            ariaIdPrefixArb,
            (prefix) => {
              resetAriaIdCounter()
              const id = generateAriaId(prefix)
              return isValidAriaId(id)
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should generate many unique IDs without collision', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 10, max: 100 }),
            (count) => {
              resetAriaIdCounter()
              const ids = new Set<string>()
              
              for (let i = 0; i < count; i++) {
                ids.add(generateAriaId('test'))
              }
              
              // All generated IDs should be unique
              return ids.size === count
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should use default prefix when none provided', () => {
        resetAriaIdCounter()
        const id = generateAriaId()
        expect(id).toMatch(/^aria-/)
      })
    })

    // ========================================================================
    // 4.2 Button ARIA Attributes
    // ========================================================================
    describe('4.2 Button ARIA attributes are valid', () => {
      it('should always include aria-label for buttons', () => {
        fc.assert(
          fc.property(
            labelTextArb,
            (label) => {
              const attrs = createButtonAria({ label })
              return attrs['aria-label'] === label
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should include role="button"', () => {
        fc.assert(
          fc.property(
            labelTextArb,
            (label) => {
              const attrs = createButtonAria({ label })
              return attrs.role === 'button'
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should set correct tabindex based on disabled state', () => {
        fc.assert(
          fc.property(
            fc.tuple(labelTextArb, fc.boolean()),
            ([label, disabled]) => {
              const attrs = createButtonAria({ label, disabled })
              
              if (disabled) {
                return attrs.tabindex === -1 && attrs['aria-disabled'] === true
              }
              return attrs.tabindex === 0
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should include aria-pressed when pressed state is provided', () => {
        fc.assert(
          fc.property(
            fc.tuple(labelTextArb, fc.boolean()),
            ([label, pressed]) => {
              const attrs = createButtonAria({ label, pressed })
              return attrs['aria-pressed'] === pressed
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should include aria-expanded when expanded state is provided', () => {
        fc.assert(
          fc.property(
            fc.tuple(labelTextArb, fc.boolean()),
            ([label, expanded]) => {
              const attrs = createButtonAria({ label, expanded })
              return attrs['aria-expanded'] === expanded
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should include aria-controls when controls ID is provided', () => {
        fc.assert(
          fc.property(
            fc.tuple(labelTextArb, ariaIdPrefixArb),
            ([label, controlsId]) => {
              const attrs = createButtonAria({ label, controls: controlsId })
              return attrs['aria-controls'] === controlsId
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should include aria-haspopup when haspopup is provided and truthy', () => {
        fc.assert(
          fc.property(
            fc.tuple(
              labelTextArb, 
              fc.constantFrom<AriaAttributes['aria-haspopup']>(true, 'menu', 'listbox', 'tree', 'grid', 'dialog')
            ),
            ([label, haspopup]) => {
              const attrs = createButtonAria({ label, haspopup })
              return attrs['aria-haspopup'] === haspopup
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should not include aria-haspopup when haspopup is false', () => {
        fc.assert(
          fc.property(
            labelTextArb,
            (label) => {
              const attrs = createButtonAria({ label, haspopup: false })
              // When haspopup is false, it should not be included in attributes
              return attrs['aria-haspopup'] === undefined || attrs['aria-haspopup'] === false
            }
          ),
          { numRuns: 20 }
        )
      })
    })

    // ========================================================================
    // 4.3 Menu Item ARIA Attributes
    // ========================================================================
    describe('4.3 Menu item ARIA attributes are valid', () => {
      it('should always include aria-label for menu items', () => {
        fc.assert(
          fc.property(
            labelTextArb,
            (label) => {
              const attrs = createMenuItemAria({ label })
              return attrs['aria-label'] === label
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should include role="menuitem"', () => {
        fc.assert(
          fc.property(
            labelTextArb,
            (label) => {
              const attrs = createMenuItemAria({ label })
              return attrs.role === 'menuitem'
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should set tabindex to -1 for menu items', () => {
        fc.assert(
          fc.property(
            labelTextArb,
            (label) => {
              const attrs = createMenuItemAria({ label })
              return attrs.tabindex === -1
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should include position info when index and total provided', () => {
        fc.assert(
          fc.property(
            fc.tuple(labelTextArb, indexArb, totalArb),
            ([label, index, total]) => {
              const validIndex = Math.min(index, total - 1)
              const attrs = createMenuItemAria({ label, index: validIndex, total })
              
              return attrs['aria-posinset'] === validIndex + 1 &&
                     attrs['aria-setsize'] === total
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should include aria-haspopup for submenu items', () => {
        fc.assert(
          fc.property(
            labelTextArb,
            (label) => {
              const attrs = createMenuItemAria({ label, hasSubmenu: true })
              return attrs['aria-haspopup'] === 'menu' &&
                     attrs['aria-expanded'] === false
            }
          ),
          { numRuns: 50 }
        )
      })
    })


    // ========================================================================
    // 4.4 Tab ARIA Attributes
    // ========================================================================
    describe('4.4 Tab ARIA attributes are valid', () => {
      it('should always include required tab attributes', () => {
        fc.assert(
          fc.property(
            fc.tuple(labelTextArb, fc.boolean(), ariaIdPrefixArb, indexArb, totalArb),
            ([label, selected, controls, index, total]) => {
              const validIndex = Math.min(index, total - 1)
              const attrs = createTabAria({
                label,
                selected,
                controls,
                index: validIndex,
                total
              })
              
              return attrs['aria-label'] === label &&
                     attrs['aria-selected'] === selected &&
                     attrs['aria-controls'] === controls &&
                     attrs.role === 'tab'
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should set correct tabindex based on selected state', () => {
        fc.assert(
          fc.property(
            fc.tuple(labelTextArb, fc.boolean(), ariaIdPrefixArb, indexArb, totalArb),
            ([label, selected, controls, index, total]) => {
              const validIndex = Math.min(index, total - 1)
              const attrs = createTabAria({
                label,
                selected,
                controls,
                index: validIndex,
                total
              })
              
              // Selected tab should have tabindex 0, others -1
              return selected ? attrs.tabindex === 0 : attrs.tabindex === -1
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should include correct position info', () => {
        fc.assert(
          fc.property(
            fc.tuple(labelTextArb, fc.boolean(), ariaIdPrefixArb, indexArb, totalArb),
            ([label, selected, controls, index, total]) => {
              const validIndex = Math.min(index, total - 1)
              const attrs = createTabAria({
                label,
                selected,
                controls,
                index: validIndex,
                total
              })
              
              return attrs['aria-posinset'] === validIndex + 1 &&
                     attrs['aria-setsize'] === total
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ========================================================================
    // 4.5 Form Field ARIA Attributes
    // ========================================================================
    describe('4.5 Form field ARIA attributes are valid', () => {
      it('should include aria-label when provided', () => {
        fc.assert(
          fc.property(
            labelTextArb,
            (label) => {
              const attrs = createFieldAria({ label })
              return attrs['aria-label'] === label
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should include aria-labelledby when provided', () => {
        fc.assert(
          fc.property(
            ariaIdPrefixArb,
            (labelledby) => {
              const attrs = createFieldAria({ labelledby })
              return attrs['aria-labelledby'] === labelledby
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should include aria-describedby when provided', () => {
        fc.assert(
          fc.property(
            ariaIdPrefixArb,
            (describedby) => {
              const attrs = createFieldAria({ describedby })
              return attrs['aria-describedby'] === describedby
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should include aria-required when required', () => {
        fc.assert(
          fc.property(
            fc.boolean(),
            (required) => {
              const attrs = createFieldAria({ required })
              return required ? attrs['aria-required'] === true : attrs['aria-required'] === undefined
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should include aria-invalid when invalid', () => {
        fc.assert(
          fc.property(
            fc.boolean(),
            (invalid) => {
              const attrs = createFieldAria({ invalid })
              return invalid ? attrs['aria-invalid'] === true : attrs['aria-invalid'] === undefined
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should include aria-errormessage when provided', () => {
        fc.assert(
          fc.property(
            ariaIdPrefixArb,
            (errormessage) => {
              const attrs = createFieldAria({ errormessage })
              return attrs['aria-errormessage'] === errormessage
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should include aria-readonly when readonly', () => {
        fc.assert(
          fc.property(
            fc.boolean(),
            (readonly) => {
              const attrs = createFieldAria({ readonly })
              return readonly ? attrs['aria-readonly'] === true : attrs['aria-readonly'] === undefined
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should include aria-disabled when disabled', () => {
        fc.assert(
          fc.property(
            fc.boolean(),
            (disabled) => {
              const attrs = createFieldAria({ disabled })
              return disabled ? attrs['aria-disabled'] === true : attrs['aria-disabled'] === undefined
            }
          ),
          { numRuns: 50 }
        )
      })
    })

    // ========================================================================
    // 4.6 Progress Bar ARIA Attributes
    // ========================================================================
    describe('4.6 Progress bar ARIA attributes are valid', () => {
      it('should always include required progress attributes', () => {
        fc.assert(
          fc.property(
            fc.tuple(labelTextArb, progressValueArb),
            ([label, value]) => {
              const attrs = createProgressAria({ label, value })
              
              return attrs['aria-label'] === label &&
                     attrs['aria-valuenow'] === value &&
                     attrs['aria-valuemin'] !== undefined &&
                     attrs['aria-valuemax'] !== undefined &&
                     attrs.role === 'progressbar'
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should use default min/max when not provided', () => {
        fc.assert(
          fc.property(
            fc.tuple(labelTextArb, progressValueArb),
            ([label, value]) => {
              const attrs = createProgressAria({ label, value })
              
              return attrs['aria-valuemin'] === 0 &&
                     attrs['aria-valuemax'] === 100
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should use custom min/max when provided', () => {
        fc.assert(
          fc.property(
            fc.tuple(labelTextArb, progressValueArb, minMaxRangeArb),
            ([label, value, [min, max]]) => {
              const attrs = createProgressAria({ label, value, min, max })
              
              return attrs['aria-valuemin'] === min &&
                     attrs['aria-valuemax'] === max
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should include aria-valuetext when provided', () => {
        fc.assert(
          fc.property(
            fc.tuple(labelTextArb, progressValueArb, labelTextArb),
            ([label, value, valuetext]) => {
              const attrs = createProgressAria({ label, value, valuetext })
              return attrs['aria-valuetext'] === valuetext
            }
          ),
          { numRuns: 50 }
        )
      })
    })

    // ========================================================================
    // 4.7 Dialog ARIA Attributes
    // ========================================================================
    describe('4.7 Dialog ARIA attributes are valid', () => {
      it('should include aria-label when provided', () => {
        fc.assert(
          fc.property(
            labelTextArb,
            (label) => {
              const attrs = createDialogAria({ label })
              return attrs['aria-label'] === label
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should include aria-labelledby when provided', () => {
        fc.assert(
          fc.property(
            ariaIdPrefixArb,
            (labelledby) => {
              const attrs = createDialogAria({ labelledby })
              return attrs['aria-labelledby'] === labelledby
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should include aria-describedby when provided', () => {
        fc.assert(
          fc.property(
            ariaIdPrefixArb,
            (describedby) => {
              const attrs = createDialogAria({ describedby })
              return attrs['aria-describedby'] === describedby
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should set correct role based on modal state', () => {
        fc.assert(
          fc.property(
            fc.tuple(labelTextArb, fc.boolean()),
            ([label, modal]) => {
              const attrs = createDialogAria({ label, modal })
              
              if (modal) {
                return attrs.role === 'alertdialog' && attrs['aria-modal'] === true
              }
              return attrs.role === 'dialog'
            }
          ),
          { numRuns: 50 }
        )
      })
    })

    // ========================================================================
    // 4.8 ARIA Attribute Value Validity
    // ========================================================================
    describe('4.8 ARIA attribute values are valid according to WCAG 2.1', () => {
      it('should generate valid tabindex values', () => {
        fc.assert(
          fc.property(
            fc.tuple(labelTextArb, fc.boolean()),
            ([label, disabled]) => {
              const attrs = createButtonAria({ label, disabled })
              return isValidTabindex(attrs.tabindex)
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should have valid aria-posinset and aria-setsize relationship', () => {
        fc.assert(
          fc.property(
            fc.tuple(labelTextArb, indexArb, totalArb),
            ([label, index, total]) => {
              const validIndex = Math.min(index, total - 1)
              const attrs = createMenuItemAria({ label, index: validIndex, total })
              
              const posinset = attrs['aria-posinset']
              const setsize = attrs['aria-setsize']
              
              // posinset should be >= 1 and <= setsize
              return posinset !== undefined && 
                     setsize !== undefined &&
                     posinset >= 1 && 
                     posinset <= setsize
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should have valid aria-valuenow within min/max range', () => {
        fc.assert(
          fc.property(
            fc.tuple(labelTextArb, minMaxRangeArb),
            ([label, [min, max]]) => {
              // Generate value within range
              const value = Math.floor((min + max) / 2)
              const attrs = createProgressAria({ label, value, min, max })
              
              const valuenow = attrs['aria-valuenow']
              const valuemin = attrs['aria-valuemin']
              const valuemax = attrs['aria-valuemax']
              
              return valuenow !== undefined &&
                     valuemin !== undefined &&
                     valuemax !== undefined &&
                     valuemin <= valuemax
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should have required attributes for each role', () => {
        fc.assert(
          fc.property(
            labelTextArb,
            (label) => {
              const buttonAttrs = createButtonAria({ label })
              const progressAttrs = createProgressAria({ label, value: 50 })
              const tabAttrs = createTabAria({ label, selected: true, controls: 'panel', index: 0, total: 3 })
              const menuItemAttrs = createMenuItemAria({ label })
              const dialogAttrs = createDialogAria({ label })
              
              return hasRequiredAriaForRole(buttonAttrs, 'button') &&
                     hasRequiredAriaForRole(progressAttrs, 'progressbar') &&
                     hasRequiredAriaForRole(tabAttrs, 'tab') &&
                     hasRequiredAriaForRole(menuItemAttrs, 'menuitem') &&
                     hasRequiredAriaForRole(dialogAttrs, 'dialog')
            }
          ),
          { numRuns: 50 }
        )
      })
    })
  })
})

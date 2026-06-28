/**
 * LoadingState 组件正确渲染加载提示 属性测试
 * Feature: ui-deep-polish, Property 2: LoadingState 组件正确渲染加载提示
 *
 * **Validates: Requirements 2.2**
 *
 * For any valid description string, when LoadingState component's loading is true
 * and description prop is provided, the rendered output should contain that hint text.
 * When loading is false, the rendered output should show default slot content instead
 * of loading indicator.
 */

import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import * as fc from 'fast-check'
import LoadingState from '../common/LoadingState.vue'

// ============================================================================
// Arbitraries (测试数据生成器)
// ============================================================================

/** Generate a non-empty printable description string */
const descriptionArb = fc
  .array(
    fc.constantFrom(
      ...'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789数据加载中请稍候'.split(
        '',
      ),
    ),
    { minLength: 1, maxLength: 30 },
  )
  .map((chars) => chars.join(''))

/** Generate slot content text */
const slotTextArb = fc
  .array(
    fc.constantFrom(
      ...'abcdefghijklmnopqrstuvwxyz0123456789表格内容数据列表'.split(''),
    ),
    { minLength: 1, maxLength: 20 },
  )
  .map((chars) => chars.join(''))

/** Generate loading mode */
const modeArb = fc.constantFrom('spinner' as const, 'skeleton' as const)

// ============================================================================
// Property-Based Tests
// ============================================================================

describe('LoadingState — Property Tests', () => {
  /**
   * Property 2: LoadingState 组件正确渲染加载提示
   *
   * **Validates: Requirements 2.2**
   * **Feature: ui-deep-polish, Property 2: LoadingState 组件正确渲染加载提示**
   */
  describe('Property 2: LoadingState renders description and toggles content correctly', () => {
    it('should render description text when loading is true (spinner mode)', () => {
      fc.assert(
        fc.property(descriptionArb, (description) => {
          const wrapper = mount(LoadingState, {
            props: { loading: true, mode: 'spinner', description },
          })

          const desc = wrapper.find('.loading-state-description')
          expect(desc.exists()).toBe(true)
          expect(desc.text()).toBe(description)

          wrapper.unmount()
        }),
        { numRuns: 100 },
      )
    })

    it('should render description text when loading is true (skeleton mode)', () => {
      fc.assert(
        fc.property(descriptionArb, (description) => {
          const wrapper = mount(LoadingState, {
            props: { loading: true, mode: 'skeleton', description },
          })

          const desc = wrapper.find('.loading-state-description')
          expect(desc.exists()).toBe(true)
          expect(desc.text()).toBe(description)

          wrapper.unmount()
        }),
        { numRuns: 100 },
      )
    })

    it('should render description text for any valid mode when loading is true', () => {
      fc.assert(
        fc.property(descriptionArb, modeArb, (description, mode) => {
          const wrapper = mount(LoadingState, {
            props: { loading: true, mode, description },
          })

          const desc = wrapper.find('.loading-state-description')
          expect(desc.exists()).toBe(true)
          expect(desc.text()).toBe(description)

          wrapper.unmount()
        }),
        { numRuns: 100 },
      )
    })

    it('should show slot content and hide loading indicator when loading is false', () => {
      fc.assert(
        fc.property(slotTextArb, (slotText) => {
          const wrapper = mount(LoadingState, {
            props: { loading: false },
            slots: {
              default: `<div class="test-content">${slotText}</div>`,
            },
          })

          // Slot content should be visible
          const content = wrapper.find('.test-content')
          expect(content.exists()).toBe(true)
          expect(content.text()).toContain(slotText)

          // Loading indicators should not be present
          expect(wrapper.find('.loading-state-spinner').exists()).toBe(false)
          expect(wrapper.find('.loading-state-skeleton').exists()).toBe(false)
          expect(wrapper.find('.loading-state-description').exists()).toBe(false)

          wrapper.unmount()
        }),
        { numRuns: 100 },
      )
    })

    it('should not render description element when loading is true but no description provided', () => {
      fc.assert(
        fc.property(modeArb, (mode) => {
          const wrapper = mount(LoadingState, {
            props: { loading: true, mode },
          })

          expect(wrapper.find('.loading-state-description').exists()).toBe(false)

          wrapper.unmount()
        }),
        { numRuns: 100 },
      )
    })
  })
})

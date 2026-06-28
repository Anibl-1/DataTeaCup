/**
 * EmptyState 组件正确渲染属性 属性测试
 * Feature: ui-deep-polish, Property 1: EmptyState 组件正确渲染属性
 *
 * **Validates: Requirements 1.1**
 *
 * For any valid description string, when EmptyState component receives that string
 * as description prop, the rendered output should contain that description text.
 * When default slot content is provided, the rendered output should contain that slot content.
 */

import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import * as fc from 'fast-check'
import EmptyState from '../common/EmptyState.vue'

// ============================================================================
// Arbitraries (测试数据生成器)
// ============================================================================

/** Generate a non-empty printable description string (avoid whitespace-only strings) */
const descriptionArb = fc
  .array(
    fc.constantFrom(
      ...'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789暂无数据测试描述文本内容'.split(
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
      ...'abcdefghijklmnopqrstuvwxyz0123456789添加创建操作按钮'.split(''),
    ),
    { minLength: 1, maxLength: 20 },
  )
  .map((chars) => chars.join(''))

// ============================================================================
// Property-Based Tests
// ============================================================================

describe('EmptyState — Property Tests', () => {
  /**
   * Property 1: EmptyState 组件正确渲染属性
   *
   * **Validates: Requirements 1.1**
   * **Feature: ui-deep-polish, Property 1: EmptyState 组件正确渲染属性**
   */
  describe('Property 1: EmptyState renders description and slot content correctly', () => {
    it('should render any valid description string in the output', () => {
      fc.assert(
        fc.property(descriptionArb, (description) => {
          const wrapper = mount(EmptyState, {
            props: { description },
          })

          expect(wrapper.find('.empty-state-text').text()).toBe(description)

          wrapper.unmount()
        }),
        { numRuns: 100 },
      )
    })

    it('should render default slot content when provided', () => {
      fc.assert(
        fc.property(slotTextArb, (slotText) => {
          const wrapper = mount(EmptyState, {
            slots: {
              default: `<span class="test-slot">${slotText}</span>`,
            },
          })

          const actionDiv = wrapper.find('.empty-state-action')
          expect(actionDiv.exists()).toBe(true)
          expect(actionDiv.text()).toContain(slotText)

          wrapper.unmount()
        }),
        { numRuns: 100 },
      )
    })

    it('should not render action area when no slot content is provided', () => {
      fc.assert(
        fc.property(descriptionArb, (description) => {
          const wrapper = mount(EmptyState, {
            props: { description },
          })

          const actionDiv = wrapper.find('.empty-state-action')
          expect(actionDiv.exists()).toBe(false)

          wrapper.unmount()
        }),
        { numRuns: 100 },
      )
    })

    it('should render both description and slot content together', () => {
      fc.assert(
        fc.property(descriptionArb, slotTextArb, (description, slotText) => {
          const wrapper = mount(EmptyState, {
            props: { description },
            slots: {
              default: `<span>${slotText}</span>`,
            },
          })

          expect(wrapper.find('.empty-state-text').text()).toBe(description)
          expect(wrapper.find('.empty-state-action').text()).toContain(slotText)

          wrapper.unmount()
        }),
        { numRuns: 100 },
      )
    })
  })
})

/**
 * ErrorBoundary 捕获渲染错误 属性测试
 * Feature: ui-deep-polish, Property 3: ErrorBoundary 捕获渲染错误
 *
 * **Validates: Requirements 5.2**
 *
 * For any child component rendering error, ErrorBoundary should capture the error
 * and display an error prompt interface (containing the error message), rather than
 * letting the error propagate upward causing a white screen.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h, nextTick } from 'vue'
import * as fc from 'fast-check'
import ErrorBoundary from '../ErrorBoundary.vue'

// ============================================================================
// Mocks
// ============================================================================

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
  }),
  useRoute: () => ({
    fullPath: '/',
  }),
}))

vi.mock('@/utils/logger', () => ({
  logger: {
    error: vi.fn(),
    warn: vi.fn(),
    info: vi.fn(),
  },
}))

vi.mock('@/utils/errorHandler', () => ({
  generateTraceId: () => 'ERR-MOCK-TRACE-ID',
}))

vi.mock('@/utils/errorReporter', () => ({
  errorReporter: {
    captureError: vi.fn(),
  },
}))

vi.mock('naive-ui', async (importOriginal) => {
  const actual = await importOriginal<typeof import('naive-ui')>()
  return {
    ...actual,
    useMessage: () => ({
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
    }),
  }
})

// ============================================================================
// Helper: ThrowingComponent
// ============================================================================

/**
 * A child component that throws an error during render.
 * This triggers Vue's onErrorCaptured in the parent ErrorBoundary.
 */
const ThrowingComponent = defineComponent({
  name: 'ThrowingComponent',
  props: {
    errorMsg: { type: String, required: true },
  },
  render() {
    throw new Error(this.errorMsg)
  },
})

// ============================================================================
// Arbitraries (测试数据生成器)
// ============================================================================

/** Generate non-empty error message strings with printable characters */
const errorMessageArb = fc
  .array(
    fc.constantFrom(
      ...'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789错误异常网络超时未知'.split(''),
    ),
    { minLength: 1, maxLength: 40 },
  )
  .map((chars) => chars.join(''))

/** Generate slot content text */
const slotTextArb = fc
  .array(
    fc.constantFrom(
      ...'abcdefghijklmnopqrstuvwxyz0123456789正常内容页面'.split(''),
    ),
    { minLength: 1, maxLength: 20 },
  )
  .map((chars) => chars.join(''))

// ============================================================================
// Property-Based Tests
// ============================================================================

describe('ErrorBoundary — Property Tests', () => {
  beforeEach(() => {
    // Suppress Vue's console.warn for unhandled errors during tests
    console.warn = vi.fn()
  })

  /**
   * Property 3: ErrorBoundary 捕获渲染错误
   *
   * **Validates: Requirements 5.2**
   * **Feature: ui-deep-polish, Property 3: ErrorBoundary 捕获渲染错误**
   */
  describe('Property 3: ErrorBoundary captures rendering errors and displays error UI', () => {
    it('should capture any child rendering error and display the error message', async () => {
      await fc.assert(
        fc.asyncProperty(errorMessageArb, async (errorMsg) => {
          const TestApp = defineComponent({
            setup() {
              return () =>
                h(ErrorBoundary, null, {
                  default: () => h(ThrowingComponent, { errorMsg }),
                })
            },
          })

          const wrapper = mount(TestApp)
          await nextTick()

          // ErrorBoundary should show the error UI
          const errorBoundary = wrapper.find('.error-boundary')
          expect(errorBoundary.exists()).toBe(true)

          // The error message should be displayed
          const errorMessageEl = wrapper.find('.error-message')
          expect(errorMessageEl.exists()).toBe(true)
          expect(errorMessageEl.text()).toBe(errorMsg)

          // Retry and Go Home action buttons should be present
          const buttons = wrapper.findAll('button')
          expect(buttons.length).toBeGreaterThanOrEqual(2)

          wrapper.unmount()
        }),
        { numRuns: 100 },
      )
    })

    it('should show slot content when no error occurs', async () => {
      await fc.assert(
        fc.asyncProperty(slotTextArb, async (slotText) => {
          const SafeApp = defineComponent({
            setup() {
              return () =>
                h(ErrorBoundary, null, {
                  default: () => h('div', { class: 'child-content' }, slotText),
                })
            },
          })

          const wrapper = mount(SafeApp)
          await nextTick()

          // Error UI should NOT be shown
          expect(wrapper.find('.error-boundary').exists()).toBe(false)

          // Slot content should be visible
          const content = wrapper.find('.child-content')
          expect(content.exists()).toBe(true)
          expect(content.text()).toContain(slotText)

          wrapper.unmount()
        }),
        { numRuns: 100 },
      )
    })
  })
})

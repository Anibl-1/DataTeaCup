import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import * as fc from 'fast-check'
import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { vLazy, DEFAULT_ERROR } from '../lazyLoad'

/**
 * Property 3: 图片加载失败占位图
 *
 * **Validates: Requirements 7.4**
 * **Feature: frontend-comprehensive-optimization, Property 3: 图片加载失败占位图**
 *
 * 对于任意绑定 lazyLoad 指令的图片元素，error 事件后 src 应为占位图路径。
 */

// Mock IntersectionObserver
const mockObserve = vi.fn()
const mockUnobserve = vi.fn()

class MockIntersectionObserver {
  callback: IntersectionObserverCallback

  constructor(callback: IntersectionObserverCallback) {
    this.callback = callback
  }

  observe = mockObserve
  unobserve = mockUnobserve
  disconnect = vi.fn()
}

describe('vLazy — Property Tests', () => {
  beforeEach(() => {
    vi.stubGlobal('IntersectionObserver', MockIntersectionObserver)
    mockObserve.mockClear()
    mockUnobserve.mockClear()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  // --- Generators ---

  /** Arbitrary image URL: http(s) URLs that are NOT the DEFAULT_ERROR placeholder */
  const imageUrlArb = fc
    .webUrl()
    .filter((url) => url !== DEFAULT_ERROR)

  // --- Property 3: Image error sets src to DEFAULT_ERROR placeholder ---

  it('Property 3: after onerror fires, img src is set to DEFAULT_ERROR for any image URL', () => {
    fc.assert(
      fc.property(imageUrlArb, (imageUrl) => {
        const TestComponent = defineComponent({
          directives: { lazy: vLazy },
          data() {
            return { url: imageUrl }
          },
          template: '<img v-lazy="url" />',
        })

        const wrapper = mount(TestComponent)
        const imgEl = wrapper.find('img').element as HTMLImageElement

        // The directive sets el.onerror on mount — trigger it
        expect(imgEl.onerror).toBeTruthy()
        ;(imgEl.onerror as Function)(new Event('error'))

        // After error, src should be the default error placeholder
        expect(imgEl.src).toBe(DEFAULT_ERROR)

        wrapper.unmount()
      }),
      { numRuns: 100 },
    )
  })

  it('Property 3: custom error placeholder is used when provided via options', () => {
    // Generate well-formed URLs that won't be altered by jsdom normalization:
    // - lowercase only (jsdom lowercases hostnames)
    // - simple path segments (no ./ or ../ that jsdom resolves)
    const alphanumArb = fc.string({ minLength: 1, maxLength: 10, unit: 'grapheme' })
      .filter((s) => /^[a-z0-9]+$/.test(s))
    const customErrorArb = fc.tuple(
      fc.constantFrom('http', 'https'),
      alphanumArb,
      alphanumArb,
    ).map(([scheme, host, path]) => `${scheme}://${host}.com/${path}.png`)

    const imageUrlFixedArb = fc.tuple(
      fc.constantFrom('http', 'https'),
      alphanumArb,
      alphanumArb,
    ).map(([scheme, host, path]) => `${scheme}://${host}.com/${path}.jpg`)

    fc.assert(
      fc.property(imageUrlFixedArb, customErrorArb, (imageUrl, customError) => {
        const TestComponent = defineComponent({
          directives: { lazy: vLazy },
          data() {
            return {
              options: { src: imageUrl, error: customError },
            }
          },
          template: '<img v-lazy="options" />',
        })

        const wrapper = mount(TestComponent)
        const imgEl = wrapper.find('img').element as HTMLImageElement

        expect(imgEl.onerror).toBeTruthy()
        ;(imgEl.onerror as Function)(new Event('error'))

        // When custom error is provided, src should be the custom error URL
        expect(imgEl.src).toBe(customError)

        wrapper.unmount()
      }),
      { numRuns: 100 },
    )
  })
})

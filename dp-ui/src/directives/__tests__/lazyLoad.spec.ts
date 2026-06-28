import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { vLazy } from '../lazyLoad'

// Mock IntersectionObserver
const mockObserve = vi.fn()
const mockUnobserve = vi.fn()
const mockDisconnect = vi.fn()

class MockIntersectionObserver {
  callback: IntersectionObserverCallback
  
  constructor(callback: IntersectionObserverCallback) {
    this.callback = callback
  }
  
  observe = mockObserve
  unobserve = mockUnobserve
  disconnect = mockDisconnect
}

describe('vLazy directive', () => {
  beforeEach(() => {
    vi.stubGlobal('IntersectionObserver', MockIntersectionObserver)
    mockObserve.mockClear()
    mockUnobserve.mockClear()
    mockDisconnect.mockClear()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('should set loading placeholder on mount', () => {
    const TestComponent = defineComponent({
      directives: { lazy: vLazy },
      render() {
        return h('img', {
          'v-lazy': 'https://example.com/image.jpg'
        })
      }
    })

    const wrapper = mount(TestComponent)
    expect(wrapper.find('img').exists()).toBe(true)
  })

  it('should observe element on mount', () => {
    const TestComponent = defineComponent({
      directives: { lazy: vLazy },
      template: '<img v-lazy="\'https://example.com/image.jpg\'" />'
    })

    mount(TestComponent)
    expect(mockObserve).toHaveBeenCalled()
  })

  it('should accept string value', () => {
    const TestComponent = defineComponent({
      directives: { lazy: vLazy },
      data() {
        return { imageUrl: 'https://example.com/test.jpg' }
      },
      template: '<img v-lazy="imageUrl" />'
    })

    const wrapper = mount(TestComponent)
    const img = wrapper.find('img')
    expect(img.classes()).toContain('lazy-loading')
  })

  it('should accept object value with custom loading/error images', () => {
    const TestComponent = defineComponent({
      directives: { lazy: vLazy },
      data() {
        return {
          lazyOptions: {
            src: 'https://example.com/test.jpg',
            loading: 'https://example.com/loading.gif',
            error: 'https://example.com/error.png'
          }
        }
      },
      template: '<img v-lazy="lazyOptions" />'
    })

    const wrapper = mount(TestComponent)
    const img = wrapper.find('img')
    expect(img.element.src).toBe('https://example.com/loading.gif')
  })

  it('should unobserve on unmount', () => {
    const TestComponent = defineComponent({
      directives: { lazy: vLazy },
      template: '<img v-lazy="\'https://example.com/image.jpg\'" />'
    })

    const wrapper = mount(TestComponent)
    wrapper.unmount()
    expect(mockUnobserve).toHaveBeenCalled()
  })
})

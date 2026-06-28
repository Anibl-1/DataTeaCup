import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { vLazyComponent } from '../lazyComponent'

// --- IntersectionObserver mock ---
type IOCallback = IntersectionObserverCallback

let lastCallback: IOCallback | null = null
const mockObserve = vi.fn()
const mockUnobserve = vi.fn()
const mockDisconnect = vi.fn()

class MockIntersectionObserver implements IntersectionObserver {
  readonly root: Element | Document | null = null
  readonly rootMargin: string
  readonly thresholds: ReadonlyArray<number>
  callback: IOCallback

  constructor(callback: IOCallback, options?: IntersectionObserverInit) {
    this.callback = callback
    this.rootMargin = options?.rootMargin ?? '0px'
    this.thresholds = Array.isArray(options?.threshold)
      ? options!.threshold as number[]
      : [options?.threshold ?? 0]
    lastCallback = callback
  }

  observe = mockObserve
  unobserve = mockUnobserve
  disconnect = mockDisconnect
  takeRecords(): IntersectionObserverEntry[] { return [] }
}

/** Simulate an element entering the viewport */
function triggerIntersection(el: Element, isIntersecting: boolean) {
  if (!lastCallback) throw new Error('No IntersectionObserver callback registered')
  lastCallback(
    [{ target: el, isIntersecting } as unknown as IntersectionObserverEntry],
    {} as IntersectionObserver,
  )
}

// --- Tests ---
describe('vLazyComponent directive', () => {
  beforeEach(() => {
    vi.stubGlobal('IntersectionObserver', MockIntersectionObserver)
    mockObserve.mockClear()
    mockUnobserve.mockClear()
    mockDisconnect.mockClear()
    lastCallback = null
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('should observe the element on mount when given a function', () => {
    const onVisible = vi.fn()
    const Comp = defineComponent({
      directives: { 'lazy-component': vLazyComponent },
      setup() { return { onVisible } },
      template: '<div v-lazy-component="onVisible">content</div>',
    })

    mount(Comp)
    expect(mockObserve).toHaveBeenCalledTimes(1)
  })

  it('should call onVisible when element enters viewport', () => {
    const onVisible = vi.fn()
    const Comp = defineComponent({
      directives: { 'lazy-component': vLazyComponent },
      setup() { return { onVisible } },
      template: '<div v-lazy-component="onVisible">content</div>',
    })

    const wrapper = mount(Comp)
    triggerIntersection(wrapper.element.firstElementChild!, true)
    expect(onVisible).toHaveBeenCalledTimes(1)
  })

  it('should NOT call onVisible when element is not intersecting', () => {
    const onVisible = vi.fn()
    const Comp = defineComponent({
      directives: { 'lazy-component': vLazyComponent },
      setup() { return { onVisible } },
      template: '<div v-lazy-component="onVisible">content</div>',
    })

    const wrapper = mount(Comp)
    triggerIntersection(wrapper.element.firstElementChild!, false)
    expect(onVisible).not.toHaveBeenCalled()
  })

  it('should unobserve after first intersection when once=true (default)', () => {
    const onVisible = vi.fn()
    const Comp = defineComponent({
      directives: { 'lazy-component': vLazyComponent },
      setup() { return { onVisible } },
      template: '<div v-lazy-component="onVisible">content</div>',
    })

    const wrapper = mount(Comp)
    triggerIntersection(wrapper.element.firstElementChild!, true)
    expect(mockUnobserve).toHaveBeenCalled()
  })

  it('should accept options object with custom rootMargin', () => {
    const onVisible = vi.fn()
    const Comp = defineComponent({
      directives: { 'lazy-component': vLazyComponent },
      setup() {
        return {
          opts: { onVisible, rootMargin: '100px 0px', threshold: 0.5 },
        }
      },
      template: '<div v-lazy-component="opts">content</div>',
    })

    mount(Comp)
    expect(mockObserve).toHaveBeenCalledTimes(1)
  })

  it('should cleanup observer on unmount', () => {
    const onVisible = vi.fn()
    const Comp = defineComponent({
      directives: { 'lazy-component': vLazyComponent },
      setup() { return { onVisible } },
      template: '<div v-lazy-component="onVisible">content</div>',
    })

    const wrapper = mount(Comp)
    wrapper.unmount()
    expect(mockUnobserve).toHaveBeenCalled()
    expect(mockDisconnect).toHaveBeenCalled()
  })

  it('should not unobserve when once=false and element enters viewport', () => {
    const onVisible = vi.fn()
    const Comp = defineComponent({
      directives: { 'lazy-component': vLazyComponent },
      setup() {
        return { opts: { onVisible, once: false } }
      },
      template: '<div v-lazy-component="opts">content</div>',
    })

    const wrapper = mount(Comp)
    // Reset after mount's observe call
    mockUnobserve.mockClear()

    triggerIntersection(wrapper.element.firstElementChild!, true)
    // once=false means it should NOT unobserve after intersection
    expect(mockUnobserve).not.toHaveBeenCalled()
    expect(onVisible).toHaveBeenCalledTimes(1)
  })
})

/**
 * 可视区域组件加载属性测试
 * Viewport Component Loading Property-Based Tests
 *
 * **属性 45: 可视区域组件加载**
 * **Validates: Requirements 15.2**
 *
 * 测试内容:
 * 对于任意仪表盘组件集合，只有在可视区域内（加上 rootMargin）的组件
 * 应触发其 onVisible 回调。可视区域外的组件不应被加载。
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import * as fc from 'fast-check'
import { mount } from '@vue/test-utils'
import { defineComponent, type PropType } from 'vue'
import { vLazyComponent, type LazyComponentOptions } from '../lazyComponent'

// ============================================================================
// IntersectionObserver Mock Infrastructure
// ============================================================================

interface ObserverRecord {
  element: Element
  callback: IntersectionObserverCallback
  observer: MockIntersectionObserver
  rootMargin: string
  threshold: number
}

/** Registry of all observed elements and their callbacks */
let observerRegistry: ObserverRecord[] = []

class MockIntersectionObserver implements IntersectionObserver {
  readonly root: Element | Document | null = null
  readonly rootMargin: string
  readonly thresholds: ReadonlyArray<number>
  private callback: IntersectionObserverCallback

  constructor(callback: IntersectionObserverCallback, options?: IntersectionObserverInit) {
    this.callback = callback
    this.rootMargin = options?.rootMargin ?? '0px'
    this.thresholds = Array.isArray(options?.threshold)
      ? (options!.threshold as number[])
      : [options?.threshold ?? 0]
  }

  observe(target: Element): void {
    observerRegistry.push({
      element: target,
      callback: this.callback,
      observer: this,
      rootMargin: this.rootMargin,
      threshold: this.thresholds[0] ?? 0,
    })
  }

  unobserve(target: Element): void {
    observerRegistry = observerRegistry.filter(
      (r) => !(r.element === target && r.observer === this),
    )
  }

  disconnect(): void {
    observerRegistry = observerRegistry.filter((r) => r.observer !== this)
  }

  takeRecords(): IntersectionObserverEntry[] {
    return []
  }
}

/**
 * Simulate intersection events for a specific element.
 * Triggers the registered callback with the given isIntersecting value.
 */
function simulateIntersection(element: Element, isIntersecting: boolean): void {
  const records = observerRegistry.filter((r) => r.element === element)
  for (const record of records) {
    record.callback(
      [{ target: element, isIntersecting } as unknown as IntersectionObserverEntry],
      record.observer,
    )
  }
}

// ============================================================================
// Arbitraries (测试数据生成器)
// ============================================================================

/** Dashboard component position and dimensions */
interface ComponentLayout {
  id: number
  top: number
  height: number
}

/** Viewport configuration */
interface ViewportConfig {
  viewportTop: number
  viewportHeight: number
  rootMarginPx: number
}

/** Full test scenario */
interface DashboardScenario {
  components: ComponentLayout[]
  viewport: ViewportConfig
}

/** Generate a single component layout */
const componentLayoutArb = (id: number) =>
  fc.record({
    id: fc.constant(id),
    top: fc.integer({ min: 0, max: 5000 }),
    height: fc.integer({ min: 50, max: 500 }),
  })

/** Generate a set of dashboard components (1 to 20) */
const componentListArb = fc
  .integer({ min: 1, max: 20 })
  .chain((count) => fc.tuple(...Array.from({ length: count }, (_, i) => componentLayoutArb(i))))

/** Generate viewport configuration */
const viewportConfigArb = fc.record({
  viewportTop: fc.integer({ min: 0, max: 4000 }),
  viewportHeight: fc.integer({ min: 200, max: 1200 }),
  rootMarginPx: fc.integer({ min: 0, max: 500 }),
})

/** Generate a complete dashboard scenario */
const dashboardScenarioArb: fc.Arbitrary<DashboardScenario> = fc
  .tuple(componentListArb, viewportConfigArb)
  .map(([components, viewport]) => ({ components, viewport }))

// ============================================================================
// Helper: determine if a component is within the visible area
// ============================================================================

/**
 * Determines whether a component overlaps with the extended viewport
 * (viewport + rootMargin on top and bottom).
 */
function isInViewport(component: ComponentLayout, viewport: ViewportConfig): boolean {
  const extendedTop = viewport.viewportTop - viewport.rootMarginPx
  const extendedBottom = viewport.viewportTop + viewport.viewportHeight + viewport.rootMarginPx
  const componentBottom = component.top + component.height

  // Component overlaps with extended viewport if:
  // component's bottom > extended top AND component's top < extended bottom
  return componentBottom > extendedTop && component.top < extendedBottom
}

// ============================================================================
// 属性 45: 可视区域组件加载
// ============================================================================

describe('属性 45: 可视区域组件加载', () => {
  beforeEach(() => {
    observerRegistry = []
    vi.stubGlobal('IntersectionObserver', MockIntersectionObserver)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('只有可视区域内的组件应触发 onVisible 回调', () => {
    fc.assert(
      fc.property(dashboardScenarioArb, (scenario) => {
        const { components, viewport } = scenario

        // Track which components had their onVisible called
        const visibleCallbacks = new Map<number, vi.Mock>()
        const elements: Element[] = []

        // Create a wrapper component that mounts multiple lazy-loaded children
        const DashboardComp = defineComponent({
          directives: { 'lazy-component': vLazyComponent },
          props: {
            items: {
              type: Array as PropType<
                Array<{ id: number; onVisible: () => void; rootMargin: string }>
              >,
              required: true,
            },
          },
          template: `
            <div class="dashboard">
              <div
                v-for="item in items"
                :key="item.id"
                v-lazy-component="{ onVisible: item.onVisible, rootMargin: item.rootMargin, once: true }"
                :data-id="item.id"
                class="component"
              >
                Component {{ item.id }}
              </div>
            </div>
          `,
        })

        // Prepare items with callbacks
        const items = components.map((comp) => {
          const cb = vi.fn()
          visibleCallbacks.set(comp.id, cb)
          return {
            id: comp.id,
            onVisible: cb,
            rootMargin: `${viewport.rootMarginPx}px 0px`,
          }
        })

        const wrapper = mount(DashboardComp, { props: { items } })

        // Collect mounted elements
        const componentEls = wrapper.findAll('.component')
        componentEls.forEach((w) => elements.push(w.element))

        // Simulate intersection based on component positions relative to viewport
        components.forEach((comp, index) => {
          const el = elements[index]
          if (!el) return
          const inView = isInViewport(comp, viewport)
          simulateIntersection(el, inView)
        })

        // Verify: components in viewport should have onVisible called
        // Components outside viewport should NOT have onVisible called
        for (const comp of components) {
          const cb = visibleCallbacks.get(comp.id)!
          const shouldBeVisible = isInViewport(comp, viewport)

          if (shouldBeVisible) {
            expect(cb).toHaveBeenCalledTimes(1)
          } else {
            expect(cb).not.toHaveBeenCalled()
          }
        }

        wrapper.unmount()
      }),
      { numRuns: 200 },
    )
  })

  it('可视区域外的组件不应被加载（isIntersecting=false 不触发回调）', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 1, max: 15 }),
        fc.integer({ min: 0, max: 500 }),
        (componentCount, rootMarginPx) => {
          const callbacks: vi.Mock[] = []

          const DashboardComp = defineComponent({
            directives: { 'lazy-component': vLazyComponent },
            props: {
              items: {
                type: Array as PropType<
                  Array<{ id: number; onVisible: () => void; rootMargin: string }>
                >,
                required: true,
              },
            },
            template: `
              <div>
                <div
                  v-for="item in items"
                  :key="item.id"
                  v-lazy-component="{ onVisible: item.onVisible, rootMargin: item.rootMargin, once: true }"
                  class="comp"
                >
                  {{ item.id }}
                </div>
              </div>
            `,
          })

          const items = Array.from({ length: componentCount }, (_, i) => {
            const cb = vi.fn()
            callbacks.push(cb)
            return {
              id: i,
              onVisible: cb,
              rootMargin: `${rootMarginPx}px 0px`,
            }
          })

          const wrapper = mount(DashboardComp, { props: { items } })
          const els = wrapper.findAll('.comp')

          // All components are outside viewport (isIntersecting = false)
          els.forEach((w) => simulateIntersection(w.element, false))

          // None should have been called
          for (const cb of callbacks) {
            expect(cb).not.toHaveBeenCalled()
          }

          wrapper.unmount()
        },
      ),
      { numRuns: 200 },
    )
  })

  it('once=true 时组件进入可视区域后只触发一次回调', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 1, max: 10 }),
        fc.integer({ min: 1, max: 5 }),
        (componentCount, intersectionCount) => {
          const callbacks: vi.Mock[] = []

          const DashboardComp = defineComponent({
            directives: { 'lazy-component': vLazyComponent },
            props: {
              items: {
                type: Array as PropType<Array<{ id: number; onVisible: () => void }>>,
                required: true,
              },
            },
            template: `
              <div>
                <div
                  v-for="item in items"
                  :key="item.id"
                  v-lazy-component="{ onVisible: item.onVisible, once: true }"
                  class="comp"
                >
                  {{ item.id }}
                </div>
              </div>
            `,
          })

          const items = Array.from({ length: componentCount }, (_, i) => {
            const cb = vi.fn()
            callbacks.push(cb)
            return { id: i, onVisible: cb }
          })

          const wrapper = mount(DashboardComp, { props: { items } })
          const els = wrapper.findAll('.comp')

          // Simulate entering viewport multiple times
          for (let t = 0; t < intersectionCount; t++) {
            els.forEach((w) => simulateIntersection(w.element, true))
          }

          // With once=true, each callback should be called at most once
          // (after first intersection, the observer unobserves the element)
          for (const cb of callbacks) {
            expect(cb).toHaveBeenCalledTimes(1)
          }

          wrapper.unmount()
        },
      ),
      { numRuns: 200 },
    )
  })

  it('rootMargin 扩展了有效可视区域', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 50, max: 500 }),
        fc.integer({ min: 200, max: 1000 }),
        fc.integer({ min: 0, max: 400 }),
        (componentTop, viewportHeight, rootMarginPx) => {
          // Component is just below the viewport but within rootMargin
          const viewportBottom = viewportHeight
          const componentInMarginZone =
            componentTop >= viewportBottom && componentTop < viewportBottom + rootMarginPx

          const cb = vi.fn()

          const Comp = defineComponent({
            directives: { 'lazy-component': vLazyComponent },
            setup() {
              return {
                opts: {
                  onVisible: cb,
                  rootMargin: `${rootMarginPx}px 0px`,
                  once: true,
                },
              }
            },
            template: '<div v-lazy-component="opts" class="target">content</div>',
          })

          const wrapper = mount(Comp)
          const el = wrapper.find('.target').element

          // Simulate: component is in the margin zone (would be visible with rootMargin)
          simulateIntersection(el, componentInMarginZone)

          if (componentInMarginZone) {
            expect(cb).toHaveBeenCalledTimes(1)
          } else {
            expect(cb).not.toHaveBeenCalled()
          }

          wrapper.unmount()
        },
      ),
      { numRuns: 200 },
    )
  })
})

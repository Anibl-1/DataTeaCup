/**
 * NetworkStatus 组件测试
 * 
 * **Validates: Requirements 23.5**
 *
 * 验证 NetworkStatus 组件在断网时显示提示，恢复时自动隐藏。
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import NetworkStatus from '../NetworkStatus.vue'

// ============================================================================
// Mocks
// ============================================================================

vi.mock('@/stores/app', () => ({
  useAppStore: () => ({
    isOnline: true,
  }),
}))

vi.mock('naive-ui', () => ({
  NIcon: {
    name: 'NIcon',
    template: '<span class="n-icon"><slot /></span>',
    props: ['size'],
  },
}))

vi.mock('@vicons/ionicons5', () => ({
  CloudOfflineOutline: { name: 'CloudOfflineOutline', template: '<i />' },
  CloudDoneOutline: { name: 'CloudDoneOutline', template: '<i />' },
}))

// ============================================================================
// Helpers
// ============================================================================

let onlineListeners: Array<() => void> = []
let offlineListeners: Array<() => void> = []

function setupEventListenerCapture() {
  const originalAddEventListener = window.addEventListener
  const originalRemoveEventListener = window.removeEventListener

  vi.spyOn(window, 'addEventListener').mockImplementation((event: string, handler: any) => {
    if (event === 'online') onlineListeners.push(handler)
    if (event === 'offline') offlineListeners.push(handler)
    originalAddEventListener.call(window, event, handler)
  })

  vi.spyOn(window, 'removeEventListener').mockImplementation((event: string, handler: any) => {
    if (event === 'online') onlineListeners = onlineListeners.filter(h => h !== handler)
    if (event === 'offline') offlineListeners = offlineListeners.filter(h => h !== handler)
    originalRemoveEventListener.call(window, event, handler)
  })
}

function simulateOffline() {
  Object.defineProperty(navigator, 'onLine', { value: false, writable: true, configurable: true })
  offlineListeners.forEach(fn => fn())
}

function simulateOnline() {
  Object.defineProperty(navigator, 'onLine', { value: true, writable: true, configurable: true })
  onlineListeners.forEach(fn => fn())
}

// ============================================================================
// Tests
// ============================================================================

describe('NetworkStatus', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    onlineListeners = []
    offlineListeners = []
    Object.defineProperty(navigator, 'onLine', { value: true, writable: true, configurable: true })
    setupEventListenerCapture()
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('should not show offline banner when network is connected', () => {
    const wrapper = mount(NetworkStatus)
    expect(wrapper.find('.network-offline').exists()).toBe(false)
    wrapper.unmount()
  })

  it('should show offline banner when network disconnects', async () => {
    const wrapper = mount(NetworkStatus)

    simulateOffline()
    await nextTick()

    expect(wrapper.find('.network-offline').exists()).toBe(true)
    expect(wrapper.text()).toContain('网络已断开')
    wrapper.unmount()
  })

  it('should hide offline banner and show reconnected message when network restores', async () => {
    const wrapper = mount(NetworkStatus)

    simulateOffline()
    await nextTick()
    expect(wrapper.find('.network-offline').exists()).toBe(true)

    simulateOnline()
    await nextTick()
    expect(wrapper.find('.network-offline').exists()).toBe(false)
    expect(wrapper.find('.network-reconnected').exists()).toBe(true)
    expect(wrapper.text()).toContain('网络已恢复')
    wrapper.unmount()
  })

  it('should auto-hide reconnected message after 3 seconds', async () => {
    const wrapper = mount(NetworkStatus)

    simulateOffline()
    await nextTick()

    simulateOnline()
    await nextTick()
    expect(wrapper.find('.network-reconnected').exists()).toBe(true)

    vi.advanceTimersByTime(3000)
    await nextTick()
    expect(wrapper.find('.network-reconnected').exists()).toBe(false)
    wrapper.unmount()
  })

  it('should register and clean up event listeners', () => {
    const wrapper = mount(NetworkStatus)
    expect(onlineListeners.length).toBe(1)
    expect(offlineListeners.length).toBe(1)

    wrapper.unmount()
    expect(onlineListeners.length).toBe(0)
    expect(offlineListeners.length).toBe(0)
  })
})

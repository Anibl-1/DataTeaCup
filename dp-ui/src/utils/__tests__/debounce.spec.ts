import { describe, it, expect, beforeEach, vi } from 'vitest'
import { debounce, throttle, cancellableDebounce, createRequestLock } from '../debounce'

describe('debounce', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  describe('debounce', () => {
    it('应该延迟执行函数', () => {
      const fn = vi.fn()
      const debouncedFn = debounce(fn, 100)

      debouncedFn()
      expect(fn).not.toHaveBeenCalled()

      vi.advanceTimersByTime(100)
      expect(fn).toHaveBeenCalledTimes(1)
    })

    it('应该在多次调用时只执行最后一次', () => {
      const fn = vi.fn()
      const debouncedFn = debounce(fn, 100)

      debouncedFn()
      debouncedFn()
      debouncedFn()

      vi.advanceTimersByTime(100)
      expect(fn).toHaveBeenCalledTimes(1)
    })

    it('应该支持立即执行模式', () => {
      const fn = vi.fn()
      const debouncedFn = debounce(fn, 100, true)

      debouncedFn()
      expect(fn).toHaveBeenCalledTimes(1)

      debouncedFn()
      expect(fn).toHaveBeenCalledTimes(1)

      vi.advanceTimersByTime(100)
      debouncedFn()
      expect(fn).toHaveBeenCalledTimes(2)
    })
  })

  describe('throttle', () => {
    it('应该限制函数执行频率', () => {
      const fn = vi.fn()
      const throttledFn = throttle(fn, 100)

      throttledFn()
      expect(fn).toHaveBeenCalledTimes(1)

      throttledFn()
      throttledFn()
      expect(fn).toHaveBeenCalledTimes(1)

      vi.advanceTimersByTime(100)
      expect(fn).toHaveBeenCalledTimes(2)
    })

    it('应该支持禁用首次执行', () => {
      const fn = vi.fn()
      const throttledFn = throttle(fn, 100, { leading: false })

      throttledFn()
      expect(fn).not.toHaveBeenCalled()

      vi.advanceTimersByTime(100)
      expect(fn).toHaveBeenCalledTimes(1)
    })

    it('应该支持禁用尾部执行', () => {
      const fn = vi.fn()
      const throttledFn = throttle(fn, 100, { trailing: false })

      throttledFn()
      expect(fn).toHaveBeenCalledTimes(1)

      throttledFn()
      vi.advanceTimersByTime(100)
      expect(fn).toHaveBeenCalledTimes(1)
    })
  })

  describe('cancellableDebounce', () => {
    it('应该支持取消', () => {
      const fn = vi.fn()
      const { run, cancel } = cancellableDebounce(fn, 100)

      run()
      cancel()
      vi.advanceTimersByTime(100)

      expect(fn).not.toHaveBeenCalled()
    })

    it('应该支持立即执行', () => {
      const fn = vi.fn()
      const { run, flush } = cancellableDebounce(fn, 100)

      run()
      flush()

      expect(fn).toHaveBeenCalledTimes(1)
    })
  })

  describe('createRequestLock', () => {
    it('应该正确管理锁', () => {
      const lock = createRequestLock()

      expect(lock.acquire('key')).toBe(true)
      expect(lock.acquire('key')).toBe(false)
      expect(lock.isLocked('key')).toBe(true)

      lock.release('key')
      expect(lock.isLocked('key')).toBe(false)
      expect(lock.acquire('key')).toBe(true)
    })

    it('应该支持清除所有锁', () => {
      const lock = createRequestLock()

      lock.acquire('key1')
      lock.acquire('key2')
      lock.clear()

      expect(lock.isLocked('key1')).toBe(false)
      expect(lock.isLocked('key2')).toBe(false)
    })
  })
})

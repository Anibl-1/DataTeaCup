import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useLoading, useMultiLoading } from '../useLoading'

describe('useLoading', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  describe('useLoading', () => {
    it('should start with not loading by default', () => {
      const { isLoading } = useLoading()
      expect(isLoading.value).toBe(false)
    })

    it('should start with loading when initialLoading is true', () => {
      const { isLoading } = useLoading({ initialLoading: true })
      expect(isLoading.value).toBe(true)
    })

    it('should toggle loading state', () => {
      const { isLoading, startLoading, stopLoading } = useLoading()
      
      expect(isLoading.value).toBe(false)
      
      startLoading()
      expect(isLoading.value).toBe(true)
      
      stopLoading()
      expect(isLoading.value).toBe(false)
    })

    it('should handle multiple start/stop calls', () => {
      const { isLoading, loadingCount, startLoading, stopLoading } = useLoading()
      
      startLoading()
      startLoading()
      expect(loadingCount.value).toBe(2)
      expect(isLoading.value).toBe(true)
      
      stopLoading()
      expect(loadingCount.value).toBe(1)
      expect(isLoading.value).toBe(true)
      
      stopLoading()
      expect(loadingCount.value).toBe(0)
      expect(isLoading.value).toBe(false)
    })

    it('should reset loading state', () => {
      const { isLoading, loadingCount, startLoading, resetLoading } = useLoading()
      
      startLoading()
      startLoading()
      expect(loadingCount.value).toBe(2)
      
      resetLoading()
      expect(loadingCount.value).toBe(0)
      expect(isLoading.value).toBe(false)
    })

    it('should wrap async function with loading state', async () => {
      const { isLoading, withLoading } = useLoading()
      
      const asyncFn = vi.fn().mockResolvedValue('result')
      
      const promise = withLoading(asyncFn)
      expect(isLoading.value).toBe(true)
      
      const result = await promise
      expect(result).toBe('result')
      expect(isLoading.value).toBe(false)
    })
  })

  describe('useMultiLoading', () => {
    it('should manage multiple loading states', () => {
      const { isLoading, startLoading, stopLoading } = useMultiLoading(['fetch', 'save'])
      
      expect(isLoading('fetch')).toBe(false)
      expect(isLoading('save')).toBe(false)
      
      startLoading('fetch')
      expect(isLoading('fetch')).toBe(true)
      expect(isLoading('save')).toBe(false)
      
      stopLoading('fetch')
      expect(isLoading('fetch')).toBe(false)
    })

    it('should track if any is loading', () => {
      const { isAnyLoading, startLoading, stopLoading } = useMultiLoading(['a', 'b'])
      
      expect(isAnyLoading.value).toBe(false)
      
      startLoading('a')
      expect(isAnyLoading.value).toBe(true)
      
      startLoading('b')
      expect(isAnyLoading.value).toBe(true)
      
      stopLoading('a')
      expect(isAnyLoading.value).toBe(true)
      
      stopLoading('b')
      expect(isAnyLoading.value).toBe(false)
    })
  })
})

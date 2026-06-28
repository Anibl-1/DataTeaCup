import { describe, it, expect, beforeEach, vi } from 'vitest'
import {
  setStorage,
  getStorage,
  removeStorage,
  clearStorage,
  clearExpiredStorage,
  getStorageInfo,
  sessionStorage
} from '../storage'

describe('storage', () => {
  beforeEach(() => {
    localStorage.clear()
    window.sessionStorage.clear()
    vi.useFakeTimers()
  })

  describe('setStorage / getStorage', () => {
    it('应该正确存储和获取数据', () => {
      setStorage('test', { name: 'test', value: 123 })
      const result = getStorage<{ name: string; value: number }>('test')
      expect(result).toEqual({ name: 'test', value: 123 })
    })

    it('应该返回默认值当键不存在时', () => {
      const result = getStorage('nonexistent', 'default')
      expect(result).toBe('default')
    })

    it('应该支持自定义前缀', () => {
      setStorage('key', 'value', { prefix: 'custom_' })
      expect(localStorage.getItem('custom_key')).toBeTruthy()
      expect(getStorage('key', undefined, 'custom_')).toBe('value')
    })
  })

  describe('过期时间', () => {
    it('应该在过期后返回默认值', () => {
      setStorage('expiring', 'value', { expire: 1000 })
      
      // 未过期
      expect(getStorage('expiring')).toBe('value')
      
      // 过期后
      vi.advanceTimersByTime(1500)
      expect(getStorage('expiring', 'default')).toBe('default')
    })

    it('应该在未过期时返回正确值', () => {
      setStorage('notExpired', 'value', { expire: 5000 })
      vi.advanceTimersByTime(3000)
      expect(getStorage('notExpired')).toBe('value')
    })
  })

  describe('removeStorage', () => {
    it('应该正确移除存储项', () => {
      setStorage('toRemove', 'value')
      expect(getStorage('toRemove')).toBe('value')
      
      removeStorage('toRemove')
      expect(getStorage('toRemove')).toBeUndefined()
    })
  })

  describe('clearStorage', () => {
    it('应该清除指定前缀的所有存储项', () => {
      setStorage('key1', 'value1')
      setStorage('key2', 'value2')
      localStorage.setItem('other_key', 'other')
      
      clearStorage()
      
      expect(getStorage('key1')).toBeUndefined()
      expect(getStorage('key2')).toBeUndefined()
      expect(localStorage.getItem('other_key')).toBe('other')
    })
  })

  describe('clearExpiredStorage', () => {
    it('应该清除过期的存储项', () => {
      setStorage('expired', 'value', { expire: 1000 })
      setStorage('notExpired', 'value', { expire: 10000 })
      
      vi.advanceTimersByTime(2000)
      clearExpiredStorage()
      
      expect(getStorage('expired')).toBeUndefined()
      expect(getStorage('notExpired')).toBe('value')
    })
  })

  describe('getStorageInfo', () => {
    it('应该返回存储使用情况', () => {
      setStorage('test', 'some data')
      const info = getStorageInfo()
      
      expect(info.used).toBeGreaterThan(0)
      expect(info.total).toBe(5 * 1024 * 1024)
      expect(info.percent).toBeGreaterThanOrEqual(0)
    })
  })

  describe('sessionStorage', () => {
    it('应该正确存储和获取数据', () => {
      sessionStorage.set('test', { data: 'value' })
      expect(sessionStorage.get('test')).toEqual({ data: 'value' })
    })

    it('应该正确移除数据', () => {
      sessionStorage.set('toRemove', 'value')
      sessionStorage.remove('toRemove')
      expect(sessionStorage.get('toRemove')).toBeUndefined()
    })

    it('应该正确清除数据', () => {
      sessionStorage.set('key1', 'value1')
      sessionStorage.set('key2', 'value2')
      sessionStorage.clear()
      
      expect(sessionStorage.get('key1')).toBeUndefined()
      expect(sessionStorage.get('key2')).toBeUndefined()
    })
  })
})

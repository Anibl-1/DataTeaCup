import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

/**
 * Unit tests for user store — session management and logout cleanup
 *
 * Validates: Requirements 19.3
 */

// Mock API calls
vi.mock('@/api/system/user', () => ({
  login: vi.fn(),
  getUserInfo: vi.fn(),
}))

// Mock logger
vi.mock('@/utils/logger', () => ({
  logger: {
    error: vi.fn(),
    warn: vi.fn(),
    info: vi.fn(),
    debug: vi.fn(),
  },
}))

import { useUserStore } from '../user'

describe('useUserStore — Session Management & Logout', () => {
  let store: ReturnType<typeof useUserStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    sessionStorage.clear()
    store = useUserStore()
  })

  afterEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  // --- lastActivityTime tracking ---

  describe('lastActivityTime tracking', () => {
    it('should have lastActivityTime initialized', () => {
      expect(store.lastActivityTime).toBeGreaterThan(0)
    })

    it('should update lastActivityTime and persist to sessionStorage', () => {
      const before = store.lastActivityTime
      store.setToken('test-token')
      store.updateLastActivityTime()
      expect(store.lastActivityTime).toBeGreaterThanOrEqual(before)
      expect(sessionStorage.getItem('lastActivityTime')).toBe(
        store.lastActivityTime.toString()
      )
    })
  })

  // --- checkSessionTimeout ---

  describe('checkSessionTimeout()', () => {
    it('should return false when no token is set', () => {
      store.removeToken()
      expect(store.checkSessionTimeout()).toBe(false)
    })

    it('should return false when activity is recent', () => {
      store.setToken('test-token')
      store.updateLastActivityTime()
      expect(store.checkSessionTimeout()).toBe(false)
    })

    it('should return true when inactive for 30+ minutes', () => {
      store.setToken('test-token')
      // Set activity time to 31 minutes ago
      const thirtyOneMinutesAgo = Date.now() - 31 * 60 * 1000
      sessionStorage.setItem('lastActivityTime', thirtyOneMinutesAgo.toString())
      expect(store.checkSessionTimeout()).toBe(true)
    })

    it('should return false when inactive for less than 30 minutes', () => {
      store.setToken('test-token')
      const twentyNineMinutesAgo = Date.now() - 29 * 60 * 1000
      sessionStorage.setItem('lastActivityTime', twentyNineMinutesAgo.toString())
      expect(store.checkSessionTimeout()).toBe(false)
    })
  })

  // --- initSessionManagement ---

  describe('initSessionManagement()', () => {
    it('should restore lastActivityTime from sessionStorage when token exists', () => {
      store.setToken('test-token')
      const storedTime = Date.now() - 5 * 60 * 1000
      sessionStorage.setItem('lastActivityTime', storedTime.toString())

      store.initSessionManagement()

      expect(store.lastActivityTime).toBe(storedTime)
    })

    it('should set lastActivityTime when no stored value and token exists', () => {
      store.setToken('test-token')
      sessionStorage.removeItem('lastActivityTime')

      store.initSessionManagement()

      expect(store.lastActivityTime).toBeGreaterThan(0)
      expect(sessionStorage.getItem('lastActivityTime')).toBeTruthy()
    })

    it('should not update when no token', () => {
      store.removeToken()
      const before = store.lastActivityTime

      store.initSessionManagement()

      // Should not change since there's no token
      expect(store.lastActivityTime).toBe(before)
    })
  })

  // --- logout clears all state ---

  describe('logout() clears all state', () => {
    it('should clear token from state and localStorage', () => {
      store.setToken('test-token')
      expect(localStorage.getItem('token')).toBe('test-token')

      store.logout()

      expect(store.token).toBe('')
      expect(localStorage.getItem('token')).toBeNull()
    })

    it('should clear userInfo, permissions, roles', () => {
      store.setUserInfo({ id: 1, username: 'admin', nickname: 'Admin', email: 'a@b.com', roles: ['admin'] })
      store.setPermissions(['system:user:add'])
      store.setRoles(['admin'])

      store.logout()

      expect(store.userInfo).toBeNull()
      expect(store.permissions).toEqual([])
      expect(store.roles).toEqual([])
      expect(store.permissionsLoaded).toBe(false)
      expect(store.mustChangePassword).toBe(false)
    })

    it('should clear session activity time', () => {
      store.setToken('test-token')
      store.updateLastActivityTime()
      expect(sessionStorage.getItem('lastActivityTime')).toBeTruthy()

      store.logout()

      expect(sessionStorage.getItem('lastActivityTime')).toBeNull()
      expect(store.lastActivityTime).toBe(0)
    })

    it('should clear tabs cache from sessionStorage', () => {
      sessionStorage.setItem('tabs-views', JSON.stringify([{ key: '/dashboard', title: '首页' }]))
      sessionStorage.setItem('tabs-active', '/dashboard')

      store.logout()

      expect(sessionStorage.getItem('tabs-views')).toBeNull()
      expect(sessionStorage.getItem('tabs-active')).toBeNull()
    })
  })
})

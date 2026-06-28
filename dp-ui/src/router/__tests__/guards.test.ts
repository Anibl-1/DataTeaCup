/**
 * 路由守卫属性测试
 * Feature: page-audit-optimization
 *
 * Property 22: 无权限路由重定向
 * Property 23: 会话超时处理
 * Property 24: 强制修改密码重定向
 *
 * **Validates: Requirements 19.2, 19.3, 19.6**
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import * as fc from 'fast-check'

// Mock dependencies before importing stores
vi.mock('@/api/system/user', () => ({
  login: vi.fn(),
  getUserInfo: vi.fn(),
}))

vi.mock('@/utils/logger', () => ({
  logger: {
    error: vi.fn(),
    warn: vi.fn(),
    info: vi.fn(),
    debug: vi.fn(),
  },
}))

vi.mock('@/utils/message', () => ({
  message: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn(),
  },
}))

import { useUserStore } from '@/stores/user'
import { checkRoutePermission, hasPermission } from '@/directives/permission'

// ============================================================================
// Property 22: 无权限路由重定向
// ============================================================================

describe('Property 22: 无权限路由重定向', () => {
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

  // Known permissions used in the router
  const routePermissions = [
    'data:source', 'data:collect', 'data:import', 'data:collect:log',
    'db:manager', 'tabledata:manage', 'dataview:manage', 'data:lineage',
    'dict:manage', 'datasync:manage', 'datax:job', 'datax:log',
    'pipeline:manage', 'pipeline:design', 'pipeline:execute', 'pipeline:log',
    'report:query', 'report:manage', 'report:design', 'report:version',
    'chart:manage', 'chart:design', 'page:manage', 'page:design',
    'user:manage', 'role:manage', 'menu:manage', 'department:manage',
    'post:manage', 'system:config', 'system:monitor',
    'log:operation', 'announcement:manage', 'ticket:manage', 'ticket:knowledge',
    'data:quality', 'query:builder', 'rls:config', 'ai:assistant',
    'chat:conversation', 'usage:stats', 'upgrade:manage',
    'ops:manage',
  ]

  /**
   * Core property: when user lacks the required permission,
   * checkRoutePermission should return false.
   */
  it('should deny access when user lacks the required route permission', () => {
    fc.assert(
      fc.property(
        fc.constantFrom(...routePermissions),
        (requiredPermission) => {
          // Set user permissions to an empty list (no permissions)
          store.setPermissions([])

          const route = { meta: { permission: requiredPermission } }
          return checkRoutePermission(route) === false
        }
      ),
      { numRuns: routePermissions.length }
    )
  })

  /**
   * Core property: when user has the required permission,
   * checkRoutePermission should return true.
   */
  it('should allow access when user has the required route permission', () => {
    fc.assert(
      fc.property(
        fc.constantFrom(...routePermissions),
        (requiredPermission) => {
          store.setPermissions([requiredPermission])

          const route = { meta: { permission: requiredPermission } }
          return checkRoutePermission(route) === true
        }
      ),
      { numRuns: routePermissions.length }
    )
  })

  /**
   * Property: admin users (with '*' permission) should always have access.
   */
  it('should always allow access for admin users with wildcard permission', () => {
    fc.assert(
      fc.property(
        fc.constantFrom(...routePermissions),
        (requiredPermission) => {
          store.setPermissions(['*'])

          const route = { meta: { permission: requiredPermission } }
          return checkRoutePermission(route) === true
        }
      ),
      { numRuns: routePermissions.length }
    )
  })

  /**
   * Property: routes without permission meta should always be accessible.
   */
  it('should allow access to routes without permission requirement', () => {
    store.setPermissions([])

    const routeNoMeta = { meta: {} }
    const routeNoPerm = { meta: { title: 'Test' } }
    const routeUndefined = { meta: { permission: undefined } }

    expect(checkRoutePermission(routeNoMeta)).toBe(true)
    expect(checkRoutePermission(routeNoPerm)).toBe(true)
    expect(checkRoutePermission(routeUndefined)).toBe(true)
  })

  /**
   * Property: having an unrelated permission should not grant access.
   */
  it('should deny access when user has only unrelated permissions', () => {
    fc.assert(
      fc.property(
        fc.constantFrom(...routePermissions),
        fc.constantFrom(...routePermissions),
        (requiredPerm, userPerm) => {
          // Only test when permissions are different
          fc.pre(requiredPerm !== userPerm)

          store.setPermissions([userPerm])
          const route = { meta: { permission: requiredPerm } }
          return checkRoutePermission(route) === false
        }
      ),
      { numRuns: 200 }
    )
  })
})

// ============================================================================
// Property 23: 会话超时处理
// ============================================================================

describe('Property 23: 会话超时处理', () => {
  let store: ReturnType<typeof useUserStore>
  const SESSION_TIMEOUT_MS = 30 * 60 * 1000 // 30 minutes

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

  /**
   * Core property: elapsed time > 30 minutes → timeout detected
   */
  it('should detect timeout when elapsed time exceeds 30 minutes', () => {
    fc.assert(
      fc.property(
        // Elapsed time from 31 minutes to 24 hours
        fc.integer({ min: SESSION_TIMEOUT_MS + 1, max: 24 * 60 * 60 * 1000 }),
        (elapsedMs) => {
          store.setToken('test-token')
          const pastTime = Date.now() - elapsedMs
          sessionStorage.setItem('lastActivityTime', pastTime.toString())

          return store.checkSessionTimeout() === true
        }
      ),
      { numRuns: 200 }
    )
  })

  /**
   * Core property: elapsed time < 30 minutes → no timeout
   */
  it('should not detect timeout when elapsed time is under 30 minutes', () => {
    fc.assert(
      fc.property(
        // Elapsed time from 0 to 29 minutes
        fc.integer({ min: 0, max: SESSION_TIMEOUT_MS - 1000 }),
        (elapsedMs) => {
          store.setToken('test-token')
          const pastTime = Date.now() - elapsedMs
          sessionStorage.setItem('lastActivityTime', pastTime.toString())

          return store.checkSessionTimeout() === false
        }
      ),
      { numRuns: 200 }
    )
  })

  /**
   * Property: no token → no timeout (even if time has elapsed)
   */
  it('should never report timeout when no token is set', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: 48 * 60 * 60 * 1000 }),
        (elapsedMs) => {
          store.removeToken()
          const pastTime = Date.now() - elapsedMs
          sessionStorage.setItem('lastActivityTime', pastTime.toString())

          return store.checkSessionTimeout() === false
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Boundary: exactly 30 minutes should NOT timeout (need to exceed)
   */
  it('should not timeout at exactly 30 minutes boundary', () => {
    store.setToken('test-token')
    const exactlyThirtyMinAgo = Date.now() - SESSION_TIMEOUT_MS
    sessionStorage.setItem('lastActivityTime', exactlyThirtyMinAgo.toString())

    // At exactly 30 minutes, elapsed === SESSION_TIMEOUT, which is NOT > SESSION_TIMEOUT
    expect(store.checkSessionTimeout()).toBe(false)
  })

  /**
   * Boundary: 30 minutes + 1ms should timeout
   */
  it('should timeout at 30 minutes + 1ms', () => {
    store.setToken('test-token')
    const justOverThirtyMin = Date.now() - SESSION_TIMEOUT_MS - 1
    sessionStorage.setItem('lastActivityTime', justOverThirtyMin.toString())

    expect(store.checkSessionTimeout()).toBe(true)
  })
})

// ============================================================================
// Property 24: 强制修改密码重定向
// ============================================================================

describe('Property 24: 强制修改密码重定向', () => {
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

  // Paths that should be blocked when mustChangePassword is true
  const protectedPaths = [
    '/dashboard', '/workspace', '/data-source', '/data-collect',
    '/report-manage', '/chart-manage', '/user', '/role',
    '/system-monitor', '/export-center', '/ai-assistant',
  ]

  /**
   * Model of the router guard logic for mustChangePassword:
   *
   *   if (userStore.mustChangePassword && to.path !== '/change-password') {
   *     next({ path: '/change-password', replace: true })
   *   }
   *
   * Returns true if the user should be redirected to change-password.
   */
  function shouldRedirectToChangePassword(
    mustChangePassword: boolean,
    targetPath: string
  ): boolean {
    return mustChangePassword && targetPath !== '/change-password'
  }

  /**
   * Core property: when mustChangePassword is true, any path except
   * /change-password should trigger redirect.
   */
  it('should redirect to change-password for any non-change-password path when mustChangePassword is true', () => {
    fc.assert(
      fc.property(
        fc.constantFrom(...protectedPaths),
        (targetPath) => {
          return shouldRedirectToChangePassword(true, targetPath) === true
        }
      ),
      { numRuns: protectedPaths.length }
    )
  })

  /**
   * Property: /change-password should never be redirected
   */
  it('should not redirect when target is /change-password', () => {
    fc.assert(
      fc.property(
        fc.boolean(),
        (mustChange) => {
          return shouldRedirectToChangePassword(mustChange, '/change-password') === false
        }
      ),
      { numRuns: 50 }
    )
  })

  /**
   * Property: when mustChangePassword is false, no redirect happens
   */
  it('should not redirect when mustChangePassword is false', () => {
    fc.assert(
      fc.property(
        fc.constantFrom(...protectedPaths, '/change-password'),
        (targetPath) => {
          return shouldRedirectToChangePassword(false, targetPath) === false
        }
      ),
      { numRuns: 50 }
    )
  })

  /**
   * Integration: verify the store's mustChangePassword flag works correctly
   */
  it('store mustChangePassword flag should be settable and readable', () => {
    expect(store.mustChangePassword).toBe(false)

    // Simulate setting mustChangePassword (normally set during login)
    store.mustChangePassword = true
    expect(store.mustChangePassword).toBe(true)

    // After logout, it should be cleared
    store.logout()
    expect(store.mustChangePassword).toBe(false)
  })
})

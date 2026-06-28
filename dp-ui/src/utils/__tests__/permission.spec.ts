/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock the user store before importing permission utils
const mockUserStore = {
  username: '',
  permissions: [] as string[],
  roles: [] as string[],
}

vi.mock('@/stores/user', () => ({
  useUserStore: () => mockUserStore,
}))

import {
  hasPermission,
  hasRole,
  isAdmin,
  hasMenuPermission,
  filterMenusByPermission,
} from '../permission'

beforeEach(() => {
  mockUserStore.username = ''
  mockUserStore.permissions = []
  mockUserStore.roles = []
})

// ==================== hasPermission ====================

describe('hasPermission', () => {
  it('should return true for admin user regardless of permissions', () => {
    mockUserStore.username = 'admin'
    mockUserStore.permissions = []
    expect(hasPermission('any:permission')).toBe(true)
  })

  it('should return true when no permission is required (empty string)', () => {
    mockUserStore.username = 'user1'
    expect(hasPermission('')).toBe(true)
  })

  it('should return true when no permission is required (empty array)', () => {
    mockUserStore.username = 'user1'
    expect(hasPermission([])).toBe(true)
  })

  it('should return true when user has the single required permission', () => {
    mockUserStore.username = 'user1'
    mockUserStore.permissions = ['user:read', 'user:write']
    expect(hasPermission('user:read')).toBe(true)
  })

  it('should return false when user lacks the single required permission', () => {
    mockUserStore.username = 'user1'
    mockUserStore.permissions = ['user:read']
    expect(hasPermission('user:write')).toBe(false)
  })

  it('should return true when user has any of the required permissions (default requireAll=false)', () => {
    mockUserStore.username = 'user1'
    mockUserStore.permissions = ['user:read']
    expect(hasPermission(['user:read', 'user:write'])).toBe(true)
  })

  it('should return false when user has none of the required permissions', () => {
    mockUserStore.username = 'user1'
    mockUserStore.permissions = ['dashboard:view']
    expect(hasPermission(['user:read', 'user:write'])).toBe(false)
  })

  it('should return true when user has all required permissions with requireAll=true', () => {
    mockUserStore.username = 'user1'
    mockUserStore.permissions = ['user:read', 'user:write', 'user:delete']
    expect(hasPermission(['user:read', 'user:write'], true)).toBe(true)
  })

  it('should return false when user lacks one of required permissions with requireAll=true', () => {
    mockUserStore.username = 'user1'
    mockUserStore.permissions = ['user:read']
    expect(hasPermission(['user:read', 'user:write'], true)).toBe(false)
  })

  it('should handle null/undefined permissions gracefully', () => {
    mockUserStore.username = 'user1'
    mockUserStore.permissions = null as any
    expect(hasPermission('user:read')).toBe(false)
  })
})

// ==================== hasRole ====================

describe('hasRole', () => {
  it('should return true for admin user regardless of roles', () => {
    mockUserStore.username = 'admin'
    mockUserStore.roles = []
    expect(hasRole('any_role')).toBe(true)
  })

  it('should return true when no role is required (empty string)', () => {
    mockUserStore.username = 'user1'
    expect(hasRole('')).toBe(true)
  })

  it('should return true when no role is required (empty array)', () => {
    mockUserStore.username = 'user1'
    expect(hasRole([])).toBe(true)
  })

  it('should return true when user has the single required role', () => {
    mockUserStore.username = 'user1'
    mockUserStore.roles = ['editor', 'viewer']
    expect(hasRole('editor')).toBe(true)
  })

  it('should return false when user lacks the single required role', () => {
    mockUserStore.username = 'user1'
    mockUserStore.roles = ['viewer']
    expect(hasRole('editor')).toBe(false)
  })

  it('should return true when user has any of the required roles (default requireAll=false)', () => {
    mockUserStore.username = 'user1'
    mockUserStore.roles = ['viewer']
    expect(hasRole(['editor', 'viewer'])).toBe(true)
  })

  it('should return false when user has none of the required roles', () => {
    mockUserStore.username = 'user1'
    mockUserStore.roles = ['guest']
    expect(hasRole(['editor', 'viewer'])).toBe(false)
  })

  it('should return true when user has all required roles with requireAll=true', () => {
    mockUserStore.username = 'user1'
    mockUserStore.roles = ['editor', 'viewer', 'admin']
    expect(hasRole(['editor', 'viewer'], true)).toBe(true)
  })

  it('should return false when user lacks one of required roles with requireAll=true', () => {
    mockUserStore.username = 'user1'
    mockUserStore.roles = ['editor']
    expect(hasRole(['editor', 'viewer'], true)).toBe(false)
  })

  it('should handle null/undefined roles gracefully', () => {
    mockUserStore.username = 'user1'
    mockUserStore.roles = null as any
    expect(hasRole('editor')).toBe(false)
  })
})

// ==================== isAdmin ====================

describe('isAdmin', () => {
  it('should return true for username "admin"', () => {
    mockUserStore.username = 'admin'
    mockUserStore.roles = []
    expect(isAdmin()).toBe(true)
  })

  it('should return true for user with "admin" role', () => {
    mockUserStore.username = 'user1'
    mockUserStore.roles = ['admin']
    expect(isAdmin()).toBe(true)
  })

  it('should return false for non-admin user without admin role', () => {
    mockUserStore.username = 'user1'
    mockUserStore.roles = ['editor']
    expect(isAdmin()).toBe(false)
  })

  it('should return false for user with empty roles', () => {
    mockUserStore.username = 'user1'
    mockUserStore.roles = []
    expect(isAdmin()).toBe(false)
  })
})

// ==================== hasMenuPermission ====================

describe('hasMenuPermission', () => {
  it('should return true for admin user', () => {
    mockUserStore.username = 'admin'
    expect(hasMenuPermission({ permissionCode: 'system:manage' })).toBe(true)
  })

  it('should return true when menu has no permissionCode', () => {
    mockUserStore.username = 'user1'
    expect(hasMenuPermission({})).toBe(true)
  })

  it('should return true when user has the menu permission', () => {
    mockUserStore.username = 'user1'
    mockUserStore.permissions = ['system:manage']
    expect(hasMenuPermission({ permissionCode: 'system:manage' })).toBe(true)
  })

  it('should return false when user lacks the menu permission', () => {
    mockUserStore.username = 'user1'
    mockUserStore.permissions = ['dashboard:view']
    expect(hasMenuPermission({ permissionCode: 'system:manage' })).toBe(false)
  })

  it('should return true when permissionCode is empty/falsy', () => {
    mockUserStore.username = 'user1'
    expect(hasMenuPermission({ permissionCode: '' })).toBe(true)
  })
})

// ==================== filterMenusByPermission ====================

describe('filterMenusByPermission', () => {
  it('should return empty array for null/undefined input', () => {
    expect(filterMenusByPermission(null as any)).toEqual([])
    expect(filterMenusByPermission(undefined as any)).toEqual([])
  })

  it('should return empty array for empty array', () => {
    expect(filterMenusByPermission([])).toEqual([])
  })

  it('should return all menus for admin user', () => {
    mockUserStore.username = 'admin'
    const menus = [
      { name: 'Dashboard', permissionCode: 'dashboard:view' },
      { name: 'System', permissionCode: 'system:manage' },
    ]
    expect(filterMenusByPermission(menus)).toHaveLength(2)
  })

  it('should filter out menus without permission', () => {
    mockUserStore.username = 'user1'
    mockUserStore.permissions = ['dashboard:view']
    const menus = [
      { name: 'Dashboard', permissionCode: 'dashboard:view' },
      { name: 'System', permissionCode: 'system:manage' },
    ]
    const result = filterMenusByPermission(menus)
    expect(result).toHaveLength(1)
    expect(result[0].name).toBe('Dashboard')
  })

  it('should keep menus without permissionCode', () => {
    mockUserStore.username = 'user1'
    mockUserStore.permissions = []
    const menus = [
      { name: 'Home' },
      { name: 'System', permissionCode: 'system:manage' },
    ]
    const result = filterMenusByPermission(menus)
    expect(result).toHaveLength(1)
    expect(result[0].name).toBe('Home')
  })

  it('should recursively filter children menus', () => {
    mockUserStore.username = 'user1'
    mockUserStore.permissions = ['system:manage', 'system:user:view']
    const menus = [
      {
        name: 'System',
        permissionCode: 'system:manage',
        children: [
          { name: 'Users', permissionCode: 'system:user:view' },
          { name: 'Roles', permissionCode: 'system:role:view' },
        ],
      },
    ]
    const result = filterMenusByPermission(menus)
    expect(result).toHaveLength(1)
    expect(result[0].children).toHaveLength(1)
    expect(result[0].children[0].name).toBe('Users')
  })

  it('should handle deeply nested children', () => {
    mockUserStore.username = 'admin'
    const menus = [
      {
        name: 'Level1',
        children: [
          {
            name: 'Level2',
            children: [
              { name: 'Level3', permissionCode: 'deep:perm' },
            ],
          },
        ],
      },
    ]
    const result = filterMenusByPermission(menus)
    expect(result).toHaveLength(1)
    expect(result[0].children[0].children).toHaveLength(1)
  })

  it('should not mutate original menu array', () => {
    mockUserStore.username = 'user1'
    mockUserStore.permissions = ['a']
    const menus = [
      { name: 'A', permissionCode: 'a', children: [{ name: 'B', permissionCode: 'b' }] },
    ]
    const original = JSON.parse(JSON.stringify(menus))
    filterMenusByPermission(menus)
    expect(menus).toEqual(original)
  })
})

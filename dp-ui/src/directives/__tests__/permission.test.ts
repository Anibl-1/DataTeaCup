import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { useUserStore } from '@/stores/user'
import { permission, hasPermission, checkPermission, checkRoutePermission } from '../permission'

// Helper to create a test component with v-permission
function createTestComponent(permissionValue: string | string[], arg?: string) {
  return defineComponent({
    directives: { permission },
    data() {
      return { perms: permissionValue }
    },
    template: arg === 'all'
      ? `<div><button v-permission:all="perms">Action</button></div>`
      : `<div><button v-permission="perms">Action</button></div>`
  })
}

describe('permission directive', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
  })

  describe('hasPermission', () => {
    it('should return true when user has wildcard permission (*)', () => {
      const store = useUserStore()
      store.setPermissions(['*'])
      expect(hasPermission('system:user:add')).toBe(true)
    })

    it('should return true when no permissions are required (empty string)', () => {
      const store = useUserStore()
      store.setPermissions([])
      expect(hasPermission('')).toBe(true)
    })

    it('should return true when no permissions are required (empty array)', () => {
      const store = useUserStore()
      store.setPermissions([])
      expect(hasPermission([])).toBe(true)
    })

    it('should return true for single permission when user has it', () => {
      const store = useUserStore()
      store.setPermissions(['system:user:add'])
      expect(hasPermission('system:user:add')).toBe(true)
    })

    it('should return false for single permission when user lacks it', () => {
      const store = useUserStore()
      store.setPermissions(['system:user:view'])
      expect(hasPermission('system:user:add')).toBe(false)
    })

    it('should return true for multiple permissions (any) when user has one', () => {
      const store = useUserStore()
      store.setPermissions(['system:user:edit'])
      expect(hasPermission(['system:user:add', 'system:user:edit'])).toBe(true)
    })

    it('should return false for multiple permissions (any) when user has none', () => {
      const store = useUserStore()
      store.setPermissions(['system:role:view'])
      expect(hasPermission(['system:user:add', 'system:user:edit'])).toBe(false)
    })

    it('should return true for multiple permissions (all) when user has all', () => {
      const store = useUserStore()
      store.setPermissions(['system:user:add', 'system:user:edit', 'system:user:view'])
      expect(hasPermission(['system:user:add', 'system:user:edit'], true)).toBe(true)
    })

    it('should return false for multiple permissions (all) when user lacks one', () => {
      const store = useUserStore()
      store.setPermissions(['system:user:add'])
      expect(hasPermission(['system:user:add', 'system:user:edit'], true)).toBe(false)
    })
  })

  describe('v-permission directive - DOM removal', () => {
    it('should keep element when user has the required single permission', () => {
      const store = useUserStore()
      store.setPermissions(['system:user:add'])

      const Comp = createTestComponent('system:user:add')
      const wrapper = mount(Comp, { global: { plugins: [pinia] } })
      expect(wrapper.find('button').exists()).toBe(true)
    })

    it('should remove element when user lacks the required single permission', () => {
      const store = useUserStore()
      store.setPermissions(['system:user:view'])

      const Comp = createTestComponent('system:user:add')
      const wrapper = mount(Comp, { global: { plugins: [pinia] } })
      expect(wrapper.find('button').exists()).toBe(false)
    })

    it('should keep element when user has any of the required permissions', () => {
      const store = useUserStore()
      store.setPermissions(['system:user:edit'])

      const Comp = createTestComponent(['system:user:add', 'system:user:edit'])
      const wrapper = mount(Comp, { global: { plugins: [pinia] } })
      expect(wrapper.find('button').exists()).toBe(true)
    })

    it('should remove element when user has none of the required permissions', () => {
      const store = useUserStore()
      store.setPermissions(['system:role:view'])

      const Comp = createTestComponent(['system:user:add', 'system:user:edit'])
      const wrapper = mount(Comp, { global: { plugins: [pinia] } })
      expect(wrapper.find('button').exists()).toBe(false)
    })

    it('should keep element with :all when user has all required permissions', () => {
      const store = useUserStore()
      store.setPermissions(['system:user:add', 'system:user:edit'])

      const Comp = createTestComponent(['system:user:add', 'system:user:edit'], 'all')
      const wrapper = mount(Comp, { global: { plugins: [pinia] } })
      expect(wrapper.find('button').exists()).toBe(true)
    })

    it('should remove element with :all when user lacks one required permission', () => {
      const store = useUserStore()
      store.setPermissions(['system:user:add'])

      const Comp = createTestComponent(['system:user:add', 'system:user:edit'], 'all')
      const wrapper = mount(Comp, { global: { plugins: [pinia] } })
      expect(wrapper.find('button').exists()).toBe(false)
    })

    it('should keep element when user has wildcard permission', () => {
      const store = useUserStore()
      store.setPermissions(['*'])

      const Comp = createTestComponent('system:user:add')
      const wrapper = mount(Comp, { global: { plugins: [pinia] } })
      expect(wrapper.find('button').exists()).toBe(true)
    })
  })

  describe('checkPermission', () => {
    it('should delegate to hasPermission correctly', () => {
      const store = useUserStore()
      store.setPermissions(['perm:a', 'perm:b'])
      expect(checkPermission('perm:a')).toBe(true)
      expect(checkPermission('perm:c')).toBe(false)
      expect(checkPermission(['perm:a', 'perm:c'])).toBe(true)
      expect(checkPermission(['perm:a', 'perm:b'], true)).toBe(true)
      expect(checkPermission(['perm:a', 'perm:c'], true)).toBe(false)
    })
  })

  describe('checkRoutePermission', () => {
    it('should return true when route has no permission meta', () => {
      const store = useUserStore()
      store.setPermissions([])
      expect(checkRoutePermission({ meta: {} })).toBe(true)
      expect(checkRoutePermission({ meta: undefined })).toBe(true)
      expect(checkRoutePermission({})).toBe(true)
    })

    it('should check route permission against user permissions', () => {
      const store = useUserStore()
      store.setPermissions(['system:user:view'])
      expect(checkRoutePermission({ meta: { permission: 'system:user:view' } })).toBe(true)
      expect(checkRoutePermission({ meta: { permission: 'system:admin:view' } })).toBe(false)
    })

    it('should return true for admin with wildcard permission', () => {
      const store = useUserStore()
      store.setPermissions(['*'])
      expect(checkRoutePermission({ meta: { permission: 'anything' } })).toBe(true)
    })
  })
})


// ==================== Property-Based Tests ====================

import * as fc from 'fast-check'

/**
 * Property 21: 权限指令元素控制
 * Feature: page-audit-optimization, Property 21
 *
 * **Validates: Requirements 19.1**
 *
 * For any 带有 v-permission 指令的 DOM 元素，当当前用户的 permissions 列表不包含指定权限时，
 * 该元素应从 DOM 中移除。
 */
describe('Property 21: v-permission removes element when user lacks permission', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
  })

  // Arbitrary: generate permission strings like "module:resource:action"
  const permissionArb = fc.tuple(
    fc.constantFrom('system', 'data', 'report', 'chart', 'monitor'),
    fc.constantFrom('user', 'role', 'menu', 'source', 'task'),
    fc.constantFrom('add', 'edit', 'delete', 'view', 'export')
  ).map(([mod, res, act]) => `${mod}:${res}:${act}`)

  // Arbitrary: generate a set of user permissions and a required permission that is NOT in the set
  const missingPermissionArb = fc.tuple(
    fc.uniqueArray(permissionArb, { minLength: 0, maxLength: 5 }),
    permissionArb
  ).filter(([userPerms, requiredPerm]) => !userPerms.includes(requiredPerm))

  // Arbitrary: generate a set of user permissions and a required permission that IS in the set
  const presentPermissionArb = fc.uniqueArray(permissionArb, { minLength: 1, maxLength: 5 })
    .chain(perms => fc.tuple(
      fc.constant(perms),
      fc.constantFrom(...perms)
    ))

  it('should remove element when user lacks the required permission', () => {
    fc.assert(
      fc.property(missingPermissionArb, ([userPerms, requiredPerm]) => {
        const store = useUserStore()
        store.setPermissions(userPerms)

        const Comp = defineComponent({
          directives: { permission },
          data() { return { perm: requiredPerm } },
          template: `<div><button v-permission="perm">Action</button></div>`
        })

        const wrapper = mount(Comp, { global: { plugins: [pinia] } })

        // Property: element should be removed from DOM
        expect(wrapper.find('button').exists()).toBe(false)

        wrapper.unmount()
      }),
      { numRuns: 100 }
    )
  })

  it('should keep element when user has the required permission', () => {
    fc.assert(
      fc.property(presentPermissionArb, ([userPerms, requiredPerm]) => {
        const store = useUserStore()
        store.setPermissions(userPerms)

        const Comp = defineComponent({
          directives: { permission },
          data() { return { perm: requiredPerm } },
          template: `<div><button v-permission="perm">Action</button></div>`
        })

        const wrapper = mount(Comp, { global: { plugins: [pinia] } })

        // Property: element should remain in DOM
        expect(wrapper.find('button').exists()).toBe(true)

        wrapper.unmount()
      }),
      { numRuns: 100 }
    )
  })
})

/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * ActionButtons 权限过滤正确性 属性测试
 * Feature: core-modules-deep-optimization, Property 10: ActionButtons 权限过滤正确性
 *
 * **Validates: Requirements 5.3, 5.4**
 *
 * For any actions 数组和当前用户权限集合，ActionButtons 渲染的可见按钮数量应等于
 * actions 中 permission 为空或 permission 在用户权限集合中的 action 数量。
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as fc from 'fast-check'

// Mock the permission module before importing the component
let grantedPermissions: string[] = []

vi.mock('@/utils/permission', () => ({
  hasPermission: (perm: string | string[]) => {
    if (!perm || (Array.isArray(perm) && perm.length === 0)) return true
    if (Array.isArray(perm)) {
      return perm.some((p) => grantedPermissions.includes(p))
    }
    return grantedPermissions.includes(perm)
  },
}))

import { mount } from '@vue/test-utils'
import ActionButtons, { type ActionConfig } from '../common/ActionButtons.vue'

// ============================================================================
// Pure filtering logic extracted from ActionButtons for direct testing
// ============================================================================

function computeVisibleActions(
  actions: ActionConfig[],
  row: any,
  permissions: string[],
): ActionConfig[] {
  return actions.filter((action) => {
    if (action.show && !action.show(row)) return false
    if (action.permission && !permissions.includes(action.permission)) return false
    return true
  })
}

// ============================================================================
// Arbitraries (测试数据生成器)
// ============================================================================

/** Generate a permission string */
const permissionArb = fc
  .array(fc.constantFrom(...'abcdefghijklmnopqrstuvwxyz'.split('')), {
    minLength: 3,
    maxLength: 10,
  })
  .map((chars) => chars.join(''))

/** Generate a set of all possible permissions (universe) */
const permissionUniverseArb = fc.uniqueArray(permissionArb, { minLength: 1, maxLength: 10 })

/** Generate an ActionConfig with optional permission */
function actionConfigArb(permissionPool: string[]): fc.Arbitrary<ActionConfig> {
  return fc.record({
    label: fc
      .array(fc.constantFrom(...'abcdefghijklmnopqrstuvwxyz0123456789'.split('')), {
        minLength: 1,
        maxLength: 8,
      })
      .map((chars) => chars.join('')),
    permission: fc.oneof(
      fc.constant(undefined),
      fc.constantFrom(...permissionPool),
    ),
    show: fc.constant(undefined),
    onClick: fc.constant((_row: any) => {}),
  }) as fc.Arbitrary<ActionConfig>
}

// ============================================================================
// Property-Based Tests
// ============================================================================

describe('ActionButtons — Property Tests', () => {
  beforeEach(() => {
    grantedPermissions = []
  })

  /**
   * Property 10: ActionButtons 权限过滤正确性
   *
   * **Validates: Requirements 5.3, 5.4**
   * **Feature: core-modules-deep-optimization, Property 10: ActionButtons 权限过滤正确性**
   */
  describe('Property 10: ActionButtons permission filtering correctness', () => {
    it('pure logic: visible count equals actions with no permission or granted permission', () => {
      fc.assert(
        fc.property(
          permissionUniverseArb.chain((universe) =>
            fc.tuple(
              fc.array(actionConfigArb(universe), { minLength: 0, maxLength: 10 }),
              fc.subarray(universe),
            ),
          ),
          ([actions, granted]) => {
            const row = { id: 1 }

            const expectedCount = actions.filter((a) => {
              if (!a.permission) return true
              return granted.includes(a.permission)
            }).length

            const visible = computeVisibleActions(actions, row, granted)
            expect(visible.length).toBe(expectedCount)
          },
        ),
        { numRuns: 200 },
      )
    })

    it('component: rendered button count matches expected visible count', () => {
      fc.assert(
        fc.property(
          permissionUniverseArb.chain((universe) =>
            fc.tuple(
              fc.array(actionConfigArb(universe), { minLength: 0, maxLength: 6 }),
              fc.subarray(universe),
              fc.constant(universe),
            ),
          ),
          ([actions, granted, _universe]) => {
            // Set the mock permissions
            grantedPermissions = granted
            const row = { id: 1 }

            // Compute expected visible count
            const expectedVisible = actions.filter((a) => {
              if (!a.permission) return true
              return granted.includes(a.permission)
            }).length

            // Ensure unique labels for rendering
            const uniqueActions = actions.map((a, i) => ({
              ...a,
              label: `action${i}`,
            }))

            const wrapper = mount(ActionButtons, {
              props: { actions: uniqueActions, row, maxVisible: 100 },
            })

            const vm = wrapper.vm as any
            expect(vm.visibleActions.length).toBe(expectedVisible)

            wrapper.unmount()
          },
        ),
        { numRuns: 100 },
      )
    })

    it('actions with no permission are always visible', () => {
      fc.assert(
        fc.property(
          fc.array(
            fc.record({
              label: fc.constant('btn'),
              onClick: fc.constant((_row: any) => {}),
              // No permission field
            }) as fc.Arbitrary<ActionConfig>,
            { minLength: 1, maxLength: 8 },
          ),
          (actions) => {
            const row = { id: 1 }
            grantedPermissions = [] // No permissions granted

            const uniqueActions = actions.map((a, i) => ({ ...a, label: `btn${i}` }))

            const wrapper = mount(ActionButtons, {
              props: { actions: uniqueActions, row, maxVisible: 100 },
            })

            const vm = wrapper.vm as any
            // All actions should be visible since none require permission
            expect(vm.visibleActions.length).toBe(uniqueActions.length)

            wrapper.unmount()
          },
        ),
        { numRuns: 100 },
      )
    })

    it('actions with ungranted permissions are hidden', () => {
      fc.assert(
        fc.property(
          permissionUniverseArb.chain((universe) =>
            fc.tuple(
              // All actions require a permission from the universe
              fc.array(
                fc.record({
                  label: fc.constant('btn'),
                  permission: fc.constantFrom(...universe),
                  onClick: fc.constant((_row: any) => {}),
                }) as fc.Arbitrary<ActionConfig>,
                { minLength: 1, maxLength: 8 },
              ),
              fc.constant(universe),
            ),
          ),
          ([actions, _universe]) => {
            // Grant NO permissions
            grantedPermissions = []
            const row = { id: 1 }

            const uniqueActions = actions.map((a, i) => ({ ...a, label: `btn${i}` }))

            const wrapper = mount(ActionButtons, {
              props: { actions: uniqueActions, row, maxVisible: 100 },
            })

            const vm = wrapper.vm as any
            // All actions require permission, none granted → 0 visible
            expect(vm.visibleActions.length).toBe(0)

            wrapper.unmount()
          },
        ),
        { numRuns: 100 },
      )
    })

    it('granting all permissions makes all actions visible', () => {
      fc.assert(
        fc.property(
          permissionUniverseArb.chain((universe) =>
            fc.tuple(
              fc.array(actionConfigArb(universe), { minLength: 0, maxLength: 8 }),
              fc.constant(universe),
            ),
          ),
          ([actions, universe]) => {
            // Grant ALL permissions
            grantedPermissions = [...universe]
            const row = { id: 1 }

            const uniqueActions = actions.map((a, i) => ({ ...a, label: `btn${i}` }))

            const wrapper = mount(ActionButtons, {
              props: { actions: uniqueActions, row, maxVisible: 100 },
            })

            const vm = wrapper.vm as any
            // All permissions granted, no show filter → all visible
            expect(vm.visibleActions.length).toBe(uniqueActions.length)

            wrapper.unmount()
          },
        ),
        { numRuns: 100 },
      )
    })
  })
})

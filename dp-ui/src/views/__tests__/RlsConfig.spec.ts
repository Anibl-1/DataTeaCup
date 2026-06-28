/**
 * RlsConfig 规则列表显示名称而非 ID 属性测试
 * Feature: ui-deep-polish, Property 4: RlsConfig 规则列表显示名称而非 ID
 *
 * **Validates: Requirements 6.3**
 *
 * For any RLS rule record, when the rule contains roleId and dataSourceId,
 * the rule list's rendered output should display the corresponding role name
 * and data source name, rather than raw ID numbers.
 *
 * We test the pure name mapping logic extracted from RlsConfig.vue:
 * - getRoleName(map, roleId): returns name if mapping exists, else String(roleId)
 * - getDataSourceName(map, dataSourceId): returns name if mapping exists, else String(dataSourceId)
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'

// ============================================================================
// Pure functions extracted from RlsConfig.vue for testability
// ============================================================================

/**
 * Given a roleNameMap and a roleId, return the role name if it exists
 * in the map, otherwise fall back to String(roleId).
 */
function getRoleName(roleNameMap: Record<number, string>, roleId: number): string {
  return roleNameMap[roleId] || String(roleId)
}

/**
 * Given a dataSourceNameMap and a dataSourceId, return the data source name
 * if it exists in the map, otherwise fall back to String(dataSourceId).
 */
function getDataSourceName(dataSourceNameMap: Record<number, string>, dataSourceId: number): string {
  return dataSourceNameMap[dataSourceId] || String(dataSourceId)
}

// ============================================================================
// Arbitraries (测试数据生成器)
// ============================================================================

/** Generate a positive integer ID */
const idArb = fc.integer({ min: 1, max: 100000 })

/** Generate a non-empty name string (printable characters) */
const nameArb = fc
  .array(
    fc.constantFrom(
      ...'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789管理员开发者数据源测试角色'.split(''),
    ),
    { minLength: 1, maxLength: 20 },
  )
  .map((chars) => chars.join(''))

/** Generate a Record<number, string> name map with 0..10 entries */
const nameMapArb = fc
  .array(fc.tuple(idArb, nameArb), { minLength: 0, maxLength: 10 })
  .map((pairs) => {
    const map: Record<number, string> = {}
    for (const [id, name] of pairs) {
      map[id] = name
    }
    return map
  })

// ============================================================================
// Property-Based Tests
// ============================================================================

describe('RlsConfig — Property Tests', () => {
  /**
   * Property 4: RlsConfig 规则列表显示名称而非 ID
   *
   * **Validates: Requirements 6.3**
   * **Feature: ui-deep-polish, Property 4: RlsConfig 规则列表显示名称而非 ID**
   */
  describe('Property 4: Name mapping returns name when present, ID string as fallback', () => {
    it('getRoleName returns the mapped name when roleId exists in the map', () => {
      fc.assert(
        fc.property(nameMapArb, idArb, nameArb, (baseMap, roleId, roleName) => {
          // Ensure the roleId is in the map
          const map = { ...baseMap, [roleId]: roleName }
          const result = getRoleName(map, roleId)
          expect(result).toBe(roleName)
        }),
        { numRuns: 100 },
      )
    })

    it('getRoleName falls back to String(roleId) when roleId is not in the map', () => {
      fc.assert(
        fc.property(nameMapArb, idArb, (map, roleId) => {
          // Remove the roleId from the map to guarantee it's missing
          const cleanMap = { ...map }
          delete cleanMap[roleId]
          const result = getRoleName(cleanMap, roleId)
          expect(result).toBe(String(roleId))
        }),
        { numRuns: 100 },
      )
    })

    it('getDataSourceName returns the mapped name when dataSourceId exists in the map', () => {
      fc.assert(
        fc.property(nameMapArb, idArb, nameArb, (baseMap, dsId, dsName) => {
          const map = { ...baseMap, [dsId]: dsName }
          const result = getDataSourceName(map, dsId)
          expect(result).toBe(dsName)
        }),
        { numRuns: 100 },
      )
    })

    it('getDataSourceName falls back to String(dataSourceId) when dataSourceId is not in the map', () => {
      fc.assert(
        fc.property(nameMapArb, idArb, (map, dsId) => {
          const cleanMap = { ...map }
          delete cleanMap[dsId]
          const result = getDataSourceName(cleanMap, dsId)
          expect(result).toBe(String(dsId))
        }),
        { numRuns: 100 },
      )
    })

    it('for any RLS rule with roleId and dataSourceId, names are displayed instead of IDs when mappings exist', () => {
      fc.assert(
        fc.property(
          nameMapArb,
          nameMapArb,
          idArb,
          idArb,
          (roleMap, dsMap, roleId, dsId) => {
            const roleName = getRoleName(roleMap, roleId)
            const dsName = getDataSourceName(dsMap, dsId)

            if (roleMap[roleId]) {
              // When mapping exists, result should be the name (not a numeric string)
              expect(roleName).toBe(roleMap[roleId])
              expect(roleName).not.toBe(String(roleId))
            } else {
              // When no mapping, result should be the ID as string
              expect(roleName).toBe(String(roleId))
            }

            if (dsMap[dsId]) {
              expect(dsName).toBe(dsMap[dsId])
              expect(dsName).not.toBe(String(dsId))
            } else {
              expect(dsName).toBe(String(dsId))
            }
          },
        ),
        { numRuns: 100 },
      )
    })
  })
})

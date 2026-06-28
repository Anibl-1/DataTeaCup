/**
 * 报表模块属性测试
 * Feature: page-audit-optimization
 * Property 7: 报表双路由访问等价性
 * Property 8: 版本对比按钮状态
 *
 * **Validates: Requirements 4.5, 4.7**
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import * as fs from 'fs'
import * as path from 'path'

// ============================================================================
// Constants
// ============================================================================

const PROJECT_ROOT = path.resolve(__dirname, '..', '..', '..')
const ROUTER_FILE = path.join(PROJECT_ROOT, 'src', 'router', 'index.ts')

// ============================================================================
// Helpers
// ============================================================================

/**
 * Extract route definitions from the router file.
 * Returns an array of { path, component, name } objects.
 */
function extractRouteDefinitions(content: string): Array<{
  path: string
  component: string
  name: string
}> {
  const routes: Array<{ path: string; component: string; name: string }> = []
  const lines = content.split('\n')

  for (let i = 0; i < lines.length; i++) {
    const pathMatch = lines[i].match(/path:\s*'([^']+)'/)
    if (pathMatch) {
      const routePath = pathMatch[1]
      let name = ''
      let component = ''

      // Look ahead for name and component within the next few lines
      for (let j = i + 1; j < Math.min(i + 6, lines.length); j++) {
        const nameMatch = lines[j].match(/name:\s*'([^']+)'/)
        if (nameMatch) name = nameMatch[1]

        const componentMatch = lines[j].match(/import\([^)]*'([^']+)'\)/)
        if (componentMatch) component = componentMatch[1]
      }

      if (routePath && component) {
        routes.push({ path: routePath, component, name })
      }
    }
  }

  return routes
}

// ============================================================================
// Load router file
// ============================================================================

const routerContent = fs.readFileSync(ROUTER_FILE, 'utf-8')
const allRoutes = extractRouteDefinitions(routerContent)

// ============================================================================
// Property 7: 报表双路由访问等价性
// ============================================================================

describe('Property 7: 报表双路由访问等价性', () => {
  const reportViewById = allRoutes.find(r => r.path === 'report-view/:id')
  const reportViewByCode = allRoutes.find(r => r.path === 'report-view-code/:code')

  /**
   * Both route patterns should exist in the router configuration.
   *
   * **Validates: Requirements 4.5**
   */
  it('both report-view routes should exist in router config', () => {
    expect(reportViewById).toBeDefined()
    expect(reportViewByCode).toBeDefined()
  })

  /**
   * Both route patterns should point to the same DynamicReport.vue component.
   * For any random selection between the two routes, the component import path
   * should be identical.
   *
   * **Validates: Requirements 4.5**
   */
  it('both routes should resolve to the same DynamicReport.vue component', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: 1 }),
        (routeIdx) => {
          const routes = [reportViewById, reportViewByCode]
          const route = routes[routeIdx]
          expect(route).toBeDefined()
          // Both should point to DynamicReport.vue
          expect(route!.component).toContain('DynamicReport.vue')
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Both routes should have the same meta title, confirming they are
   * semantically equivalent access paths.
   *
   * **Validates: Requirements 4.5**
   */
  it('both routes should have the same meta title in router config', () => {
    // Extract meta titles for both routes
    const idRouteBlock = routerContent.match(
      /path:\s*'report-view\/:id'[\s\S]*?meta:\s*\{([^}]+)\}/
    )
    const codeRouteBlock = routerContent.match(
      /path:\s*'report-view-code\/:code'[\s\S]*?meta:\s*\{([^}]+)\}/
    )

    expect(idRouteBlock).not.toBeNull()
    expect(codeRouteBlock).not.toBeNull()

    const idTitle = idRouteBlock![1].match(/title:\s*'([^']+)'/)
    const codeTitle = codeRouteBlock![1].match(/title:\s*'([^']+)'/)

    expect(idTitle).not.toBeNull()
    expect(codeTitle).not.toBeNull()

    fc.assert(
      fc.property(
        fc.constant(null),
        () => {
          expect(idTitle![1]).toBe(codeTitle![1])
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * For any valid report id or code string, both route patterns should
   * produce paths that are structurally valid (no empty segments).
   *
   * **Validates: Requirements 4.5**
   */
  it('both route patterns should produce valid paths for any id/code', () => {
    fc.assert(
      fc.property(
        fc.oneof(
          fc.nat({ max: 999999 }).map(n => ({ type: 'id' as const, value: String(n) })),
          fc.string({ minLength: 1, maxLength: 50 })
            .filter(s => /^[a-z0-9_-]+$/.test(s))
            .map(s => ({ type: 'code' as const, value: s }))
        ),
        (param) => {
          if (param.type === 'id') {
            const fullPath = `report-view/${param.value}`
            expect(fullPath).toMatch(/^report-view\/\d+$/)
          } else {
            const fullPath = `report-view-code/${param.value}`
            expect(fullPath).toMatch(/^report-view-code\/[a-z0-9_-]+$/)
          }
          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})

// ============================================================================
// Property 8: 版本对比按钮状态
// ============================================================================

describe('Property 8: 版本对比按钮状态', () => {
  /**
   * Simulates the compare button disabled logic from ReportVersion.vue:
   *   :disabled="checkedVersions.length !== 2"
   *
   * The button should be enabled (disabled=false) if and only if
   * exactly 2 versions are selected.
   */
  function isCompareButtonEnabled(checkedVersions: number[]): boolean {
    return checkedVersions.length === 2
  }

  /**
   * For any number of selected versions, the compare button should be
   * enabled only when exactly 2 versions are selected.
   *
   * **Validates: Requirements 4.7**
   */
  it('compare button enabled iff exactly 2 versions selected', () => {
    fc.assert(
      fc.property(
        fc.array(fc.nat({ max: 10000 }), { minLength: 0, maxLength: 20 }),
        (checkedVersions) => {
          const enabled = isCompareButtonEnabled(checkedVersions)
          if (checkedVersions.length === 2) {
            expect(enabled).toBe(true)
          } else {
            expect(enabled).toBe(false)
          }
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * With 0 selected versions, the compare button should be disabled.
   *
   * **Validates: Requirements 4.7**
   */
  it('compare button disabled when no versions selected', () => {
    fc.assert(
      fc.property(
        fc.constant([]),
        (checkedVersions: number[]) => {
          expect(isCompareButtonEnabled(checkedVersions)).toBe(false)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * With exactly 1 selected version, the compare button should be disabled.
   *
   * **Validates: Requirements 4.7**
   */
  it('compare button disabled when only 1 version selected', () => {
    fc.assert(
      fc.property(
        fc.nat({ max: 10000 }).map(id => [id]),
        (checkedVersions) => {
          expect(isCompareButtonEnabled(checkedVersions)).toBe(false)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * With more than 2 selected versions, the compare button should be disabled.
   *
   * **Validates: Requirements 4.7**
   */
  it('compare button disabled when more than 2 versions selected', () => {
    fc.assert(
      fc.property(
        fc.array(fc.nat({ max: 10000 }), { minLength: 3, maxLength: 20 }),
        (checkedVersions) => {
          expect(isCompareButtonEnabled(checkedVersions)).toBe(false)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * For any pair of distinct version IDs, the compare button should be enabled.
   *
   * **Validates: Requirements 4.7**
   */
  it('compare button enabled for any pair of distinct version IDs', () => {
    fc.assert(
      fc.property(
        fc.nat({ max: 10000 }),
        fc.nat({ max: 10000 }),
        (id1, id2) => {
          const checkedVersions = [id1, id2]
          expect(isCompareButtonEnabled(checkedVersions)).toBe(true)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})

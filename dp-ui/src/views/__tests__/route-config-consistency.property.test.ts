/**
 * 路由配置一致性属性测试
 * Feature: ai-feature-consolidation
 * Property 2: 路由配置一致性
 *
 * **Validates: Requirements 1.1, 5.2, 6.1, 6.5**
 *
 * 验证路由文件 router/index.ts 中：
 * - 不包含已移除页面的路由路径（ai-insight、lineage-graph）
 * - 包含保留页面的路由路径（ai-assistant、data-lineage）
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

/** 已移除的路由路径 — 不应出现在路由配置中 */
const REMOVED_ROUTE_PATHS = ['ai-insight', 'lineage-graph']

/** 保留的路由路径 — 应继续存在于路由配置中 */
const RETAINED_ROUTE_PATHS = ['ai-assistant', 'data-lineage']

// ============================================================================
// Helpers
// ============================================================================

/**
 * 从路由文件中提取所有 path: '...' 定义。
 */
function extractRoutePaths(content: string): string[] {
  const pathRegex = /path:\s*'([^']+)'/g
  const paths: string[] = []
  let match: RegExpExecArray | null
  while ((match = pathRegex.exec(content)) !== null) {
    paths.push(match[1])
  }
  return paths
}

// ============================================================================
// Load router file
// ============================================================================

const routerContent = fs.readFileSync(ROUTER_FILE, 'utf-8')
const allRoutePaths = extractRoutePaths(routerContent)

// ============================================================================
// Property-Based Tests
// ============================================================================

describe('Property 2: 路由配置一致性', () => {
  /**
   * 随机选择已移除的路由路径，验证它不出现在路由配置中。
   *
   * **Validates: Requirements 1.1, 5.2, 6.1**
   */
  it('removed route paths should NOT exist in router config', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: REMOVED_ROUTE_PATHS.length - 1 }),
        (idx) => {
          const removedPath = REMOVED_ROUTE_PATHS[idx]
          const found = allRoutePaths.includes(removedPath)
          if (found) {
            throw new Error(
              `Removed route path "${removedPath}" still exists in router config`
            )
          }
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * 随机选择保留的路由路径，验证它存在于路由配置中。
   *
   * **Validates: Requirements 5.2, 6.5**
   */
  it('retained route paths should exist in router config', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: RETAINED_ROUTE_PATHS.length - 1 }),
        (idx) => {
          const retainedPath = RETAINED_ROUTE_PATHS[idx]
          const found = allRoutePaths.includes(retainedPath)
          if (!found) {
            throw new Error(
              `Retained route path "${retainedPath}" is missing from router config`
            )
          }
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * 随机选择路由文件中的行，验证不包含已移除路径的路由定义。
   *
   * **Validates: Requirements 1.1, 6.1**
   */
  it('no line in router file should define a removed route path', () => {
    const lines = routerContent.split('\n')
    expect(lines.length).toBeGreaterThan(0)

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: lines.length - 1 }),
        fc.integer({ min: 0, max: REMOVED_ROUTE_PATHS.length - 1 }),
        (lineIdx, pathIdx) => {
          const line = lines[lineIdx]
          const removedPath = REMOVED_ROUTE_PATHS[pathIdx]
          const pattern = new RegExp(`path:\\s*['"]${removedPath}['"]`)
          return !pattern.test(line)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * 随机组合已移除路径和保留路径，验证路由配置的整体一致性：
   * 已移除路径不存在 AND 保留路径存在。
   *
   * **Validates: Requirements 1.1, 5.2, 6.1, 6.5**
   */
  it('route config should be consistent: removed absent AND retained present', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: REMOVED_ROUTE_PATHS.length - 1 }),
        fc.integer({ min: 0, max: RETAINED_ROUTE_PATHS.length - 1 }),
        (removedIdx, retainedIdx) => {
          const removedPath = REMOVED_ROUTE_PATHS[removedIdx]
          const retainedPath = RETAINED_ROUTE_PATHS[retainedIdx]
          const removedAbsent = !allRoutePaths.includes(removedPath)
          const retainedPresent = allRoutePaths.includes(retainedPath)
          if (!removedAbsent) {
            throw new Error(`Removed path "${removedPath}" still in router`)
          }
          if (!retainedPresent) {
            throw new Error(`Retained path "${retainedPath}" missing from router`)
          }
          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})

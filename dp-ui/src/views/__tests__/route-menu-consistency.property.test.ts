/**
 * 路由与菜单一致性属性测试
 * Feature: frontend-menu-route-completion
 *
 * 验证路由配置与菜单 SQL 之间的一致性，以及清理操作的完整性。
 *
 * P1: 路由路径与菜单路径一致性 (Validates: Requirements 4.1)
 * P2: 权限码一致性 (Validates: Requirements 4.2)
 * P3: 无孤立引用 (Validates: Requirements 1.5)
 * P4: 菜单 SQL 幂等性 (Validates: Requirements 3.8)
 */

import { describe, it, expect } from 'vitest'
import * as fs from 'fs'
import * as path from 'path'

// ============================================================================
// Helpers: Parse router and SQL source files
// ============================================================================

const PROJECT_ROOT = path.resolve(__dirname, '..', '..', '..')
const ROUTER_FILE = path.join(PROJECT_ROOT, 'src', 'router', 'index.ts')
const SQL_FILE = path.resolve(PROJECT_ROOT, '..', 'docs', 'sql', 'migration', 'migration_frontend_menus.sql')

/** Route names that must be consistent between router and SQL. */
const NEW_ROUTE_NAMES = [
  'AiAssistant',
  'QueryBuilder',
  'RlsConfig',
  'ReportVersion',
]

interface RouteEntry {
  name: string
  path: string
  permission: string | undefined
}

interface MenuEntry {
  menuCode: string
  routePath: string
  permissionCode: string | undefined
}

/**
 * Parse route definitions from the router file.
 * Extracts name, path, and meta.permission for each route block.
 */
function parseRoutes(routerContent: string): RouteEntry[] {
  const routes: RouteEntry[] = []

  // Match route blocks inside the children array.
  // Each route block looks like: { path: '...', name: '...', ... meta: { ..., permission: '...' } }
  const routeBlockRegex = /\{\s*\n\s*path:\s*'([^']+)',\s*\n\s*name:\s*'([^']+)',[\s\S]*?meta:\s*\{([^}]*)\}\s*\n\s*\}/g

  let match: RegExpExecArray | null
  while ((match = routeBlockRegex.exec(routerContent)) !== null) {
    const routePath = match[1]
    const routeName = match[2]
    const metaBlock = match[3]

    let permission: string | undefined
    const permMatch = metaBlock.match(/permission:\s*'([^']+)'/)
    if (permMatch) {
      permission = permMatch[1]
    }

    routes.push({ name: routeName, path: routePath, permission })
  }

  return routes
}

/**
 * Parse menu INSERT statements from the SQL file.
 * Extracts menu_code, route_path, and permission_code for page-type menus.
 */
function parseMenuSql(sqlContent: string): MenuEntry[] {
  const menus: MenuEntry[] = []

  // Match INSERT ... SELECT lines for page-type menus
  // Pattern: SELECT 'menu_name', 'menu_code', ..., 'page', '/route-path', 'icon', sort, visible, 'permission'
  const insertRegex = /SELECT\s+'[^']*',\s*'([^']+)',\s*[^,]+,\s*'page',\s*'([^']+)',\s*'[^']+',\s*\d+,\s*\d+,\s*'([^']+)'/g

  let match: RegExpExecArray | null
  while ((match = insertRegex.exec(sqlContent)) !== null) {
    menus.push({
      menuCode: match[1],
      routePath: match[2],
      permissionCode: match[3],
    })
  }

  return menus
}

// ============================================================================
// Load and parse source files once
// ============================================================================

const routerContent = fs.readFileSync(ROUTER_FILE, 'utf-8')
const sqlContent = fs.readFileSync(SQL_FILE, 'utf-8')

const allRoutes = parseRoutes(routerContent)
const allMenus = parseMenuSql(sqlContent)

// Filter to only the routes/menus that should exist in both places.
const newRoutes = allRoutes.filter(r => NEW_ROUTE_NAMES.includes(r.name))
const newMenus = allMenus.filter(m => NEW_ROUTE_NAMES.includes(m.menuCode))

// ============================================================================
// P1: Route path and menu path consistency
// **Validates: Requirements 4.1**
// ============================================================================

describe('P1: Route path and menu path consistency', () => {
  it('should find all expected routes in the router file', () => {
    const foundNames = newRoutes.map(r => r.name).sort()
    expect(foundNames).toEqual([...NEW_ROUTE_NAMES].sort())
  })

  it('should find all expected menus in the SQL file', () => {
    const foundCodes = newMenus.map(m => m.menuCode).sort()
    expect(foundCodes).toEqual([...NEW_ROUTE_NAMES].sort())
  })

  it.each(NEW_ROUTE_NAMES)(
    'route "%s" path should match the corresponding menu SQL route_path',
    (routeName) => {
      const route = newRoutes.find(r => r.name === routeName)
      const menu = newMenus.find(m => m.menuCode === routeName)

      expect(route).toBeDefined()
      expect(menu).toBeDefined()

      // Router path (e.g. "usage-stats") should equal menu route_path without leading slash
      expect('/' + route!.path).toBe(menu!.routePath)
    }
  )
})

// ============================================================================
// P2: Permission code consistency
// **Validates: Requirements 4.2**
// ============================================================================

describe('P2: Permission code consistency', () => {
  it.each(NEW_ROUTE_NAMES)(
    'route "%s" meta.permission should match the corresponding menu SQL permission_code',
    (routeName) => {
      const route = newRoutes.find(r => r.name === routeName)
      const menu = newMenus.find(m => m.menuCode === routeName)

      expect(route).toBeDefined()
      expect(menu).toBeDefined()
      expect(route!.permission).toBeDefined()
      expect(menu!.permissionCode).toBeDefined()

      expect(route!.permission).toBe(menu!.permissionCode)
    }
  )
})

// ============================================================================
// P3: No orphaned references
// **Validates: Requirements 1.5**
// ============================================================================

/** Deleted view files (without extension for import matching) */
const DELETED_VIEW_STEMS = [
  'NotificationManage',
  'NotificationSetting',
  'NotificationSettingView',
  'NotificationTemplateView',
  'SubscriptionManage',
  'NLDashboard',
  'DashboardShare',
]

/** Deleted API module files (without extension) */
const DELETED_API_STEMS = [
  'notification',
  'notificationSetting',
  'notificationTemplate',
  'subscription',
  'nlDashboard',
  'dashboardShare',
]

const ALL_DELETED_STEMS = [...DELETED_VIEW_STEMS, ...DELETED_API_STEMS]

/**
 * Recursively collect all .ts and .vue files under a directory.
 */
function collectSourceFiles(dir: string): string[] {
  const results: string[] = []
  if (!fs.existsSync(dir)) return results

  const entries = fs.readdirSync(dir, { withFileTypes: true })
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      // Skip node_modules and test output directories
      if (entry.name === 'node_modules' || entry.name === 'dist' || entry.name === 'coverage') continue
      results.push(...collectSourceFiles(fullPath))
    } else if (entry.isFile() && /\.(ts|vue|js)$/.test(entry.name)) {
      results.push(fullPath)
    }
  }
  return results
}

describe('P3: No orphaned references to deleted files', () => {
  const srcDir = path.join(PROJECT_ROOT, 'src')
  const sourceFiles = collectSourceFiles(srcDir)

  it.each(ALL_DELETED_STEMS)(
    'no source file should import the deleted module "%s"',
    (deletedStem) => {
      // Build regex patterns that match common import patterns referencing the deleted file
      // e.g. import ... from '.../<stem>' or import ... from '.../<stem>.vue'
      // Also matches dynamic imports: import('.../<stem>')
      const patterns = [
        new RegExp(`from\\s+['"][^'"]*[/]${deletedStem}(\\.vue|\\.ts)?['"]`, 'g'),
        new RegExp(`import\\s*\\(\\s*['"][^'"]*[/]${deletedStem}(\\.vue|\\.ts)?['"]`, 'g'),
      ]

      const violations: string[] = []

      for (const filePath of sourceFiles) {
        const content = fs.readFileSync(filePath, 'utf-8')
        for (const pattern of patterns) {
          pattern.lastIndex = 0
          if (pattern.test(content)) {
            const relativePath = path.relative(PROJECT_ROOT, filePath)
            violations.push(relativePath)
          }
        }
      }

      expect(
        violations,
        `Found orphaned imports referencing deleted file "${deletedStem}" in: ${violations.join(', ')}`
      ).toEqual([])
    }
  )
})

// ============================================================================
// P4: Menu SQL idempotency
// **Validates: Requirements 3.8**
// ============================================================================

describe('P4: Menu SQL idempotency - all new INSERT statements use WHERE NOT EXISTS', () => {
  /**
   * Extract all INSERT INTO sys_menu statements and verify each uses WHERE NOT EXISTS.
   */
  it('every INSERT INTO sys_menu should use WHERE NOT EXISTS pattern', () => {
    // Split SQL into individual statements (separated by semicolons)
    const statements = sqlContent.split(';').map(s => s.trim()).filter(s => s.length > 0)

    // Find all INSERT INTO sys_menu statements
    const insertStatements = statements.filter(s =>
      /INSERT\s+INTO\s+sys_menu\b/i.test(s)
    )

    expect(insertStatements.length).toBeGreaterThan(0)

    for (const stmt of insertStatements) {
      expect(
        stmt,
        `INSERT INTO sys_menu statement missing WHERE NOT EXISTS:\n${stmt.substring(0, 120)}...`
      ).toMatch(/WHERE\s+NOT\s+EXISTS/i)
    }
  })

  it.each(NEW_ROUTE_NAMES)(
    'menu INSERT for "%s" should use WHERE NOT EXISTS pattern',
    (menuCode) => {
      // Find the INSERT statement that references this menu_code
      const pattern = new RegExp(
        `INSERT\\s+INTO\\s+sys_menu[\\s\\S]*?'${menuCode}'[\\s\\S]*?;`,
        'i'
      )
      const match = sqlContent.match(pattern)

      expect(match, `No INSERT statement found for menu_code "${menuCode}"`).toBeTruthy()
      expect(match![0]).toMatch(/WHERE\s+NOT\s+EXISTS/i)
    }
  )
})

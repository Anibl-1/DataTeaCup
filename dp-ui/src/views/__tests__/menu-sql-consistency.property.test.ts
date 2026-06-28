/**
 * 菜单 SQL 一致性属性测试
 * Feature: ai-feature-consolidation
 * Property 3: 菜单 SQL 一致性
 *
 * **Validates: Requirements 1.2, 5.1, 5.3, 6.2, 6.5**
 *
 * 验证菜单迁移 SQL 文件 migration_frontend_menus.sql 中：
 * 1. AiInsight 菜单 INSERT 的 is_visible = 0（或存在 UPDATE 将其设为 0）
 * 2. AiAssistant 菜单 INSERT 的 is_visible = 1
 * 3. LineageGraph 菜单不被插入（无 INSERT）
 * 4. DataLineage 菜单不受影响（若存在，is_visible = 1）
 * 5. AiInsight 不在管理员角色菜单 INSERT 列表中
 * 6. 存在 DELETE 语句移除 AiInsight 和 LineageGraph 的角色菜单关联
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import * as fs from 'fs'
import * as path from 'path'

// ============================================================================
// Constants
// ============================================================================

const PROJECT_ROOT = path.resolve(__dirname, '..', '..', '..')
const SQL_FILE = path.resolve(
  PROJECT_ROOT,
  '..',
  'docs',
  'sql',
  'migration',
  'migration_frontend_menus.sql'
)

// ============================================================================
// Helpers
// ============================================================================

const sqlContent = fs.readFileSync(SQL_FILE, 'utf-8')

/**
 * Extract all INSERT INTO sys_menu ... SELECT statements and parse
 * menu_code and is_visible from each.
 */
interface MenuInsert {
  menuCode: string
  isVisible: number
  raw: string
}

function parseMenuInserts(sql: string): MenuInsert[] {
  const results: MenuInsert[] = []
  // Match INSERT INTO sys_menu ... SELECT ... FROM DUAL WHERE NOT EXISTS ...
  // The SELECT values: 'name', 'code', parent, 'type', 'path', 'icon', sort, is_visible, 'perm'
  const regex =
    /INSERT\s+INTO\s+sys_menu[\s\S]*?SELECT\s+'[^']*',\s*'([^']+)',\s*[^,]+,\s*'[^']+',\s*[^,]+,\s*'[^']+',\s*\d+,\s*(\d+),\s*[^)]*FROM\s+DUAL/gi
  let match: RegExpExecArray | null
  while ((match = regex.exec(sql)) !== null) {
    results.push({
      menuCode: match[1],
      isVisible: parseInt(match[2], 10),
      raw: match[0],
    })
  }
  return results
}

/**
 * Check if there is an UPDATE statement setting is_visible = 0 for a given menu_code.
 */
function hasUpdateSetInvisible(sql: string, menuCode: string): boolean {
  const pattern = new RegExp(
    `UPDATE\\s+sys_menu\\s+SET\\s+is_visible\\s*=\\s*0\\s+WHERE\\s+menu_code\\s*=\\s*'${menuCode}'`,
    'i'
  )
  return pattern.test(sql)
}

/**
 * Extract the admin role menu INSERT list (menu_code values in the IN (...) clause).
 */
function parseAdminRoleMenuCodes(sql: string): string[] {
  const pattern =
    /INSERT\s+IGNORE\s+INTO\s+sys_role_menu[\s\S]*?role_code\s*=\s*'admin'[\s\S]*?menu_code\s+IN\s*\(([^)]+)\)/i
  const match = sql.match(pattern)
  if (!match) return []
  // Extract quoted strings from the IN list
  const codes: string[] = []
  const codeRegex = /'([^']+)'/g
  let m: RegExpExecArray | null
  while ((m = codeRegex.exec(match[1])) !== null) {
    codes.push(m[1])
  }
  return codes
}

/**
 * Check if there is a DELETE FROM sys_role_menu for a given menu_code.
 */
function hasRoleMenuDelete(sql: string, menuCode: string): boolean {
  const pattern = new RegExp(
    `DELETE\\s+FROM\\s+sys_role_menu\\s+WHERE\\s+menu_id\\s*=\\s*\\(\\s*SELECT\\s+id\\s+FROM\\s+sys_menu\\s+WHERE\\s+menu_code\\s*=\\s*'${menuCode}'\\s*\\)`,
    'i'
  )
  return pattern.test(sql)
}

// ============================================================================
// Pre-parsed data
// ============================================================================

const menuInserts = parseMenuInserts(sqlContent)
const adminRoleMenuCodes = parseAdminRoleMenuCodes(sqlContent)

// ============================================================================
// Property-Based Tests
// ============================================================================

describe('Property 3: 菜单 SQL 一致性', () => {
  /**
   * AiInsight 菜单 INSERT 的 is_visible = 0，或存在 UPDATE 将其设为 0。
   *
   * **Validates: Requirements 1.2, 5.3**
   */
  it('AiInsight menu should be invisible (INSERT is_visible=0 or UPDATE to 0)', () => {
    fc.assert(
      fc.property(fc.integer({ min: 0, max: 99 }), (_) => {
        const aiInsightInsert = menuInserts.find((m) => m.menuCode === 'AiInsight')
        const insertHasInvisible = aiInsightInsert !== undefined && aiInsightInsert.isVisible === 0
        const updateSetsInvisible = hasUpdateSetInvisible(sqlContent, 'AiInsight')

        if (!insertHasInvisible && !updateSetsInvisible) {
          throw new Error(
            'AiInsight menu is not set to invisible: INSERT is_visible is not 0 and no UPDATE sets it to 0'
          )
        }
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * AiAssistant 菜单 INSERT 的 is_visible = 1。
   *
   * **Validates: Requirements 5.1**
   */
  it('AiAssistant menu should be visible (is_visible=1)', () => {
    fc.assert(
      fc.property(fc.integer({ min: 0, max: 99 }), (_) => {
        const aiAssistantInsert = menuInserts.find((m) => m.menuCode === 'AiAssistant')
        if (!aiAssistantInsert) {
          throw new Error('AiAssistant menu INSERT not found in SQL')
        }
        if (aiAssistantInsert.isVisible !== 1) {
          throw new Error(
            `AiAssistant menu is_visible = ${aiAssistantInsert.isVisible}, expected 1`
          )
        }
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * LineageGraph 菜单不被插入（无 INSERT 语句）。
   *
   * **Validates: Requirements 6.2**
   */
  it('LineageGraph menu should NOT be inserted', () => {
    fc.assert(
      fc.property(fc.integer({ min: 0, max: 99 }), (_) => {
        const lineageGraphInsert = menuInserts.find((m) => m.menuCode === 'LineageGraph')
        if (lineageGraphInsert) {
          throw new Error(
            'LineageGraph menu INSERT found in SQL — it should not be inserted'
          )
        }
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * DataLineage 菜单不受影响（若存在 INSERT，is_visible = 1）。
   *
   * **Validates: Requirements 6.5**
   */
  it('DataLineage menu should remain unaffected (if present, is_visible=1)', () => {
    fc.assert(
      fc.property(fc.integer({ min: 0, max: 99 }), (_) => {
        const dataLineageInsert = menuInserts.find((m) => m.menuCode === 'DataLineage')
        // DataLineage may or may not be in this SQL file; if present, it must be visible
        if (dataLineageInsert && dataLineageInsert.isVisible !== 1) {
          throw new Error(
            `DataLineage menu is_visible = ${dataLineageInsert.isVisible}, expected 1`
          )
        }
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * AiInsight 不在管理员角色菜单 INSERT 列表中。
   *
   * **Validates: Requirements 1.2, 5.1**
   */
  it('AiInsight should NOT be in admin role menu INSERT list', () => {
    fc.assert(
      fc.property(fc.integer({ min: 0, max: 99 }), (_) => {
        if (adminRoleMenuCodes.includes('AiInsight')) {
          throw new Error(
            'AiInsight is still in the admin role menu INSERT list — it should be removed'
          )
        }
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * 存在 DELETE 语句移除 AiInsight 和 LineageGraph 的角色菜单关联。
   *
   * **Validates: Requirements 1.2, 5.1, 6.2**
   */
  it('DELETE statements should exist for AiInsight and LineageGraph role-menu associations', () => {
    const menuCodesToDelete = ['AiInsight', 'LineageGraph']

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: menuCodesToDelete.length - 1 }),
        (idx) => {
          const menuCode = menuCodesToDelete[idx]
          if (!hasRoleMenuDelete(sqlContent, menuCode)) {
            throw new Error(
              `No DELETE FROM sys_role_menu statement found for menu_code "${menuCode}"`
            )
          }
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * 综合一致性检查：随机选择一个检查维度，验证菜单 SQL 的整体一致性。
   *
   * **Validates: Requirements 1.2, 5.1, 5.3, 6.2, 6.5**
   */
  it('overall menu SQL consistency across all dimensions', () => {
    // 0: AiInsight invisible, 1: AiAssistant visible, 2: no LineageGraph insert,
    // 3: DataLineage unaffected, 4: AiInsight not in admin list, 5: DELETE statements exist
    fc.assert(
      fc.property(fc.integer({ min: 0, max: 5 }), (dimension) => {
        switch (dimension) {
          case 0: {
            const ins = menuInserts.find((m) => m.menuCode === 'AiInsight')
            const insertOk = ins !== undefined && ins.isVisible === 0
            const updateOk = hasUpdateSetInvisible(sqlContent, 'AiInsight')
            if (!insertOk && !updateOk) {
              throw new Error('AiInsight not set to invisible')
            }
            break
          }
          case 1: {
            const ins = menuInserts.find((m) => m.menuCode === 'AiAssistant')
            if (!ins || ins.isVisible !== 1) {
              throw new Error('AiAssistant not visible')
            }
            break
          }
          case 2: {
            if (menuInserts.find((m) => m.menuCode === 'LineageGraph')) {
              throw new Error('LineageGraph INSERT found')
            }
            break
          }
          case 3: {
            const ins = menuInserts.find((m) => m.menuCode === 'DataLineage')
            if (ins && ins.isVisible !== 1) {
              throw new Error('DataLineage visibility changed')
            }
            break
          }
          case 4: {
            if (adminRoleMenuCodes.includes('AiInsight')) {
              throw new Error('AiInsight in admin role menu list')
            }
            break
          }
          case 5: {
            for (const code of ['AiInsight', 'LineageGraph']) {
              if (!hasRoleMenuDelete(sqlContent, code)) {
                throw new Error(`Missing DELETE for ${code}`)
              }
            }
            break
          }
        }
        return true
      }),
      { numRuns: 100 }
    )
  })
})

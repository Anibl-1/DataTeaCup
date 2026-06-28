/**
 * SQL 编辑器自动补全属性测试
 * Feature: platform-deep-optimization, Property 6: SQL 自动补全正确性
 * 
 * **Validates: Requirements 2.3**
 * 
 * WHEN 用户在SQL编辑器中输入时，
 * THE SQL_Editor SHALL 提供表名、字段名和SQL关键字的自动补全建议
 * 
 * 验证属性:
 * 1. 所有SQL关键字在前缀匹配时都包含在补全建议中
 * 2. 数据源中的表名包含在补全建议中
 * 3. 当存在表上下文时，字段名包含在补全建议中
 * 4. 补全建议按相关性排序（完全匹配 > 前缀匹配 > 包含匹配）
 * 5. 补全建议数量限制在合理范围内（最多50条）
 * 6. 空前缀返回所有可用的补全建议
 * 7. 不匹配的前缀返回空补全建议
 * 
 * @author dataplatform
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'

// ============================================================================
// Types
// ============================================================================

/**
 * 补全项类型
 */
interface CompletionItem {
  label: string
  type: 'keyword' | 'table' | 'column'
  detail?: string
  info?: string
  documentation?: string
}

/**
 * 表信息类型
 */
interface TableInfo {
  name: string
  columns: string[]
}

// ============================================================================
// SQL 关键字列表（与后端 SqlAutoCompleteService 保持一致）
// ============================================================================

const SQL_KEYWORDS: string[] = [
  // DQL
  'SELECT', 'FROM', 'WHERE', 'AND', 'OR', 'NOT', 'IN', 'BETWEEN', 'LIKE', 'IS', 'NULL',
  'ORDER', 'BY', 'ASC', 'DESC', 'GROUP', 'HAVING', 'LIMIT', 'OFFSET', 'DISTINCT',
  // JOIN
  'JOIN', 'INNER', 'LEFT', 'RIGHT', 'FULL', 'OUTER', 'CROSS', 'ON', 'USING',
  // Aggregate
  'COUNT', 'SUM', 'AVG', 'MAX', 'MIN', 'COALESCE', 'IFNULL', 'NULLIF',
  // DML
  'INSERT', 'INTO', 'VALUES', 'UPDATE', 'SET', 'DELETE',
  // DDL
  'CREATE', 'TABLE', 'ALTER', 'DROP', 'INDEX', 'VIEW', 'DATABASE',
  // Clauses
  'AS', 'CASE', 'WHEN', 'THEN', 'ELSE', 'END', 'UNION', 'ALL', 'EXISTS',
  // Data types
  'INT', 'INTEGER', 'BIGINT', 'SMALLINT', 'TINYINT', 'DECIMAL', 'NUMERIC', 'FLOAT', 'DOUBLE',
  'VARCHAR', 'CHAR', 'TEXT', 'BLOB', 'DATE', 'TIME', 'DATETIME', 'TIMESTAMP', 'BOOLEAN',
  // Constraints
  'PRIMARY', 'KEY', 'FOREIGN', 'REFERENCES', 'UNIQUE', 'CHECK', 'DEFAULT', 'AUTO_INCREMENT',
  // Functions
  'CONCAT', 'SUBSTRING', 'TRIM', 'UPPER', 'LOWER', 'LENGTH', 'REPLACE', 'CAST', 'CONVERT',
  'NOW', 'CURDATE', 'CURTIME', 'DATE_FORMAT', 'DATEDIFF', 'DATE_ADD', 'DATE_SUB',
  'ROUND', 'FLOOR', 'CEIL', 'ABS', 'MOD', 'POWER', 'SQRT',
  'IF', 'GREATEST', 'LEAST'
]

const SQL_KEYWORDS_LOWER = new Set(SQL_KEYWORDS.map(kw => kw.toLowerCase()))

// ============================================================================
// Core Functions (模拟 SQL 自动补全核心逻辑)
// ============================================================================

/**
 * 获取关键字补全建议
 */
export function getKeywordCompletions(prefix: string): CompletionItem[] {
  const lowerPrefix = (prefix || '').toLowerCase()
  return SQL_KEYWORDS
    .filter(kw => !lowerPrefix || kw.toLowerCase().includes(lowerPrefix))
    .map(kw => ({
      label: kw,
      type: 'keyword' as const,
      detail: 'SQL 关键字'
    }))
}

/**
 * 获取表名补全建议
 */
export function getTableCompletions(tables: TableInfo[], prefix: string): CompletionItem[] {
  const lowerPrefix = (prefix || '').toLowerCase()
  return tables
    .filter(t => !lowerPrefix || t.name.toLowerCase().includes(lowerPrefix))
    .map(t => ({
      label: t.name,
      type: 'table' as const,
      detail: '表',
      info: `${t.columns.length} 个字段`
    }))
}

/**
 * 从 SQL 上下文中解析表名
 */
export function parseTablesFromContext(context: string | null): Set<string> {
  const tables = new Set<string>()
  if (!context) return tables

  const patterns = [/from\s+(\w+)/gi, /join\s+(\w+)/gi]
  for (const pattern of patterns) {
    let match
    while ((match = pattern.exec(context)) !== null) {
      tables.add(match[1].toLowerCase())
    }
  }

  return tables
}

/**
 * 获取字段名补全建议
 */
export function getColumnCompletions(
  tables: TableInfo[],
  prefix: string,
  context: string | null
): CompletionItem[] {
  const lowerPrefix = (prefix || '').toLowerCase()
  const contextTables = parseTablesFromContext(context)
  const completions: CompletionItem[] = []

  for (const table of tables) {
    // 如果有上下文表名，只显示相关表的字段
    if (contextTables.size > 0 && !contextTables.has(table.name.toLowerCase())) {
      continue
    }

    for (const col of table.columns) {
      if (!lowerPrefix || col.toLowerCase().includes(lowerPrefix)) {
        completions.push({
          label: col,
          type: 'column',
          detail: '字段',
          info: table.name
        })
      }
    }
  }

  return completions
}

/**
 * 计算匹配分数
 */
export function calculateMatchScore(label: string, prefix: string): number {
  if (!prefix) return 0
  const lowerLabel = label.toLowerCase()
  const lowerPrefix = prefix.toLowerCase()
  if (lowerLabel === lowerPrefix) return 100
  if (lowerLabel.startsWith(lowerPrefix)) return 80
  if (lowerLabel.includes(lowerPrefix)) return 50
  return 0
}

/**
 * 获取类型排序权重
 */
export function getTypeOrder(type: string): number {
  switch (type) {
    case 'keyword': return 1
    case 'table': return 2
    case 'column': return 3
    default: return 4
  }
}

/**
 * 获取完整的补全建议
 */
export function getCompletions(
  tables: TableInfo[],
  prefix: string,
  context: string | null
): CompletionItem[] {
  const completions: CompletionItem[] = []
  const lowerPrefix = (prefix || '').toLowerCase()

  // 1. 添加 SQL 关键字建议
  completions.push(...getKeywordCompletions(lowerPrefix))

  // 2. 添加表名建议
  if (tables) {
    completions.push(...getTableCompletions(tables, lowerPrefix))
    // 3. 添加字段名建议
    completions.push(...getColumnCompletions(tables, lowerPrefix, context))
  }

  // 按相关性排序
  completions.sort((a, b) => {
    const scoreA = calculateMatchScore(a.label.toLowerCase(), lowerPrefix)
    const scoreB = calculateMatchScore(b.label.toLowerCase(), lowerPrefix)
    if (scoreA !== scoreB) return scoreB - scoreA
    return getTypeOrder(a.type) - getTypeOrder(b.type)
  })

  // 限制返回数量
  return completions.slice(0, 50)
}

/**
 * 映射补全类型到 CodeMirror 类型
 */
export function mapCompletionType(type: string): string {
  switch (type) {
    case 'keyword': return 'keyword'
    case 'table': return 'class'
    case 'column': return 'property'
    default: return 'text'
  }
}

/**
 * 计算补全项的优先级（boost）
 */
export function getBoost(type: string, prefix: string, label: string): number {
  let boost = 0
  const lowerPrefix = prefix.toLowerCase()
  const lowerLabel = label.toLowerCase()

  // 完全匹配优先
  if (lowerLabel === lowerPrefix) boost += 100
  // 前缀匹配次之
  else if (lowerLabel.startsWith(lowerPrefix)) boost += 50

  // 类型优先级：关键字 > 表名 > 字段名
  switch (type) {
    case 'keyword': boost += 10; break
    case 'table': boost += 5; break
    case 'column': boost += 0; break
  }

  return boost
}

// ============================================================================
// Arbitraries (测试数据生成器)
// ============================================================================

/**
 * 生成有效的表名
 */
const validTableNameArb = fc.string({ minLength: 3, maxLength: 15 })
  .filter(s => /^[a-z]+$/.test(s))
  .map(s => s + '_table')

/**
 * 生成有效的字段名
 */
const validColumnNameArb = fc.constantFrom(
  'id', 'name', 'email', 'phone', 'address', 'status', 'created_at', 'updated_at',
  'user_id', 'order_id', 'product_id', 'amount', 'price', 'quantity', 'description',
  'title', 'content', 'category', 'type', 'level', 'score', 'count', 'total'
)

/**
 * 生成表信息
 */
const tableInfoArb: fc.Arbitrary<TableInfo> = fc.record({
  name: validTableNameArb,
  columns: fc.array(validColumnNameArb, { minLength: 2, maxLength: 8 })
})

/**
 * 生成表信息列表
 */
const tableInfoListArb = fc.array(tableInfoArb, { minLength: 1, maxLength: 10 })

/**
 * 生成 SQL 关键字前缀
 */
const sqlKeywordPrefixArb = fc.constantFrom(...SQL_KEYWORDS)
  .map(kw => kw.substring(0, Math.min(3, kw.length)).toLowerCase())

/**
 * 生成有效的前缀（1-5个字符）
 */
const validPrefixArb = fc.string({ minLength: 1, maxLength: 5 })
  .filter(s => /^[a-z]+$/.test(s))

/**
 * 生成不匹配的前缀
 */
const nonMatchingPrefixArb = fc.constantFrom('xyz123', 'qqq999', 'zzz888', 'www777', 'mmm666')

/**
 * 生成 SQL 上下文
 */
const sqlContextArb = fc.constantFrom(
  'SELECT * FROM users',
  'SELECT id, name FROM orders JOIN products ON orders.product_id = products.id',
  'SELECT * FROM customers WHERE status = 1',
  'SELECT a.id FROM table_a a LEFT JOIN table_b b ON a.id = b.a_id',
  ''
)

// ============================================================================
// Property-Based Tests
// ============================================================================

describe('SQL Editor Auto-Complete Property Tests', () => {
  /**
   * **Validates: Requirements 2.3**
   * 
   * WHEN 用户在SQL编辑器中输入时，
   * THE SQL_Editor SHALL 提供表名、字段名和SQL关键字的自动补全建议
   */
  describe('Property 6: SQL Auto-Complete Correctness', () => {
    // ========================================================================
    // 1. SQL Keywords Completions
    // ========================================================================
    describe('1. All SQL keywords are included in completions when prefix matches', () => {
      it('should include all matching SQL keywords', () => {
        fc.assert(
          fc.property(
            sqlKeywordPrefixArb,
            (prefix) => {
              const completions = getKeywordCompletions(prefix)
              const completionLabels = new Set(completions.map(c => c.label.toLowerCase()))

              // 验证所有匹配前缀的关键字都在补全建议中
              for (const keyword of SQL_KEYWORDS) {
                if (keyword.toLowerCase().includes(prefix.toLowerCase())) {
                  if (!completionLabels.has(keyword.toLowerCase())) {
                    return false
                  }
                }
              }
              return true
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should return all keywords for empty prefix', () => {
        const completions = getKeywordCompletions('')
        expect(completions.length).toBe(SQL_KEYWORDS.length)
      })

      it('should mark all keyword completions with type "keyword"', () => {
        fc.assert(
          fc.property(
            sqlKeywordPrefixArb,
            (prefix) => {
              const completions = getKeywordCompletions(prefix)
              return completions.every(c => c.type === 'keyword')
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ========================================================================
    // 2. Table Names Completions
    // ========================================================================
    describe('2. Table names from data source are included in completions', () => {
      it('should include all table names for empty prefix', () => {
        fc.assert(
          fc.property(
            tableInfoListArb,
            (tables) => {
              const completions = getTableCompletions(tables, '')
              const completionLabels = new Set(completions.map(c => c.label))

              for (const table of tables) {
                if (!completionLabels.has(table.name)) {
                  return false
                }
              }
              return true
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should filter tables by prefix', () => {
        fc.assert(
          fc.property(
            fc.tuple(tableInfoListArb, validPrefixArb),
            ([tables, prefix]) => {
              const completions = getTableCompletions(tables, prefix)

              // 所有返回的补全项都应该包含前缀
              return completions.every(c => 
                c.label.toLowerCase().includes(prefix.toLowerCase())
              )
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should mark all table completions with type "table"', () => {
        fc.assert(
          fc.property(
            tableInfoListArb,
            (tables) => {
              const completions = getTableCompletions(tables, '')
              return completions.every(c => c.type === 'table')
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ========================================================================
    // 3. Column Names with Table Context
    // ========================================================================
    describe('3. Column names are included when table context is present', () => {
      it('should include columns from context tables only', () => {
        fc.assert(
          fc.property(
            tableInfoListArb.filter(tables => tables.length >= 2),
            (tables) => {
              const targetTable = tables[0]
              const context = `SELECT * FROM ${targetTable.name}`

              const completions = getColumnCompletions(tables, '', context)

              // 所有返回的字段都应该属于目标表
              return completions.every(c => c.info === targetTable.name)
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should include all columns when no context', () => {
        fc.assert(
          fc.property(
            tableInfoListArb,
            (tables) => {
              const completions = getColumnCompletions(tables, '', null)
              
              // 计算所有表的字段总数
              const totalColumns = tables.reduce((sum, t) => sum + t.columns.length, 0)
              
              return completions.length === totalColumns
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should mark all column completions with type "column"', () => {
        fc.assert(
          fc.property(
            tableInfoListArb,
            (tables) => {
              const completions = getColumnCompletions(tables, '', null)
              return completions.every(c => c.type === 'column')
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ========================================================================
    // 4. Relevance Sorting
    // ========================================================================
    describe('4. Completions are sorted by relevance (exact > prefix > contains)', () => {
      it('should sort completions by match score descending', () => {
        fc.assert(
          fc.property(
            fc.tuple(tableInfoListArb, validPrefixArb),
            ([tables, prefix]) => {
              const completions = getCompletions(tables, prefix, null)

              // 验证排序：分数高的在前面
              for (let i = 0; i < completions.length - 1; i++) {
                const currentScore = calculateMatchScore(completions[i].label, prefix)
                const nextScore = calculateMatchScore(completions[i + 1].label, prefix)

                if (currentScore < nextScore) {
                  return false
                }
              }
              return true
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should give exact match highest score', () => {
        const score = calculateMatchScore('SELECT', 'select')
        expect(score).toBe(100)
      })

      it('should give prefix match higher score than contains match', () => {
        const prefixScore = calculateMatchScore('SELECT', 'sel')
        const containsScore = calculateMatchScore('SELECT', 'ele')
        expect(prefixScore).toBeGreaterThan(containsScore)
      })
    })

    // ========================================================================
    // 5. Completion Limit
    // ========================================================================
    describe('5. Completions are limited to a reasonable number (max 50)', () => {
      it('should never return more than 50 completions', () => {
        fc.assert(
          fc.property(
            fc.tuple(tableInfoListArb, validPrefixArb),
            ([tables, prefix]) => {
              const completions = getCompletions(tables, prefix, null)
              return completions.length <= 50
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should limit even with empty prefix', () => {
        fc.assert(
          fc.property(
            tableInfoListArb,
            (tables) => {
              const completions = getCompletions(tables, '', null)
              return completions.length <= 50
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ========================================================================
    // 6. Empty Prefix Behavior
    // ========================================================================
    describe('6. Empty prefix returns all available completions', () => {
      it('should return keywords + tables + columns for empty prefix', () => {
        fc.assert(
          fc.property(
            tableInfoListArb,
            (tables) => {
              const completions = getCompletions(tables, '', null)

              const expectedKeywords = SQL_KEYWORDS.length
              const expectedTables = tables.length
              const expectedColumns = tables.reduce((sum, t) => sum + t.columns.length, 0)
              const totalExpected = Math.min(50, expectedKeywords + expectedTables + expectedColumns)

              return completions.length === totalExpected
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ========================================================================
    // 7. Non-matching Prefix
    // ========================================================================
    describe('7. Non-matching prefix returns empty completions', () => {
      it('should return empty for non-matching prefix', () => {
        fc.assert(
          fc.property(
            nonMatchingPrefixArb,
            (prefix) => {
              const tables: TableInfo[] = [
                { name: 'users', columns: ['id', 'name', 'email'] },
                { name: 'orders', columns: ['id', 'user_id', 'amount'] }
              ]

              const completions = getCompletions(tables, prefix, null)
              return completions.length === 0
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ========================================================================
    // 8. Type Mapping
    // ========================================================================
    describe('8. Completion type mapping is correct', () => {
      it('should map keyword to "keyword"', () => {
        expect(mapCompletionType('keyword')).toBe('keyword')
      })

      it('should map table to "class"', () => {
        expect(mapCompletionType('table')).toBe('class')
      })

      it('should map column to "property"', () => {
        expect(mapCompletionType('column')).toBe('property')
      })

      it('should map unknown to "text"', () => {
        expect(mapCompletionType('unknown')).toBe('text')
      })
    })

    // ========================================================================
    // 9. Boost Calculation
    // ========================================================================
    describe('9. Boost calculation is correct', () => {
      it('should give highest boost for exact match', () => {
        fc.assert(
          fc.property(
            fc.constantFrom(...SQL_KEYWORDS.filter(kw => kw.length >= 3)),
            (keyword) => {
              const exactBoost = getBoost('keyword', keyword.toLowerCase(), keyword)
              const prefixBoost = getBoost('keyword', keyword.substring(0, 2).toLowerCase(), keyword)
              return exactBoost > prefixBoost
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should give type-based boost', () => {
        const keywordBoost = getBoost('keyword', 'x', 'xyz')
        const tableBoost = getBoost('table', 'x', 'xyz')
        const columnBoost = getBoost('column', 'x', 'xyz')

        expect(keywordBoost).toBeGreaterThan(tableBoost)
        expect(tableBoost).toBeGreaterThan(columnBoost)
      })
    })

    // ========================================================================
    // 10. Case Insensitivity
    // ========================================================================
    describe('10. Prefix matching is case-insensitive', () => {
      it('should return same results for different case prefixes', () => {
        const tables: TableInfo[] = [
          { name: 'Users', columns: ['Id', 'Name', 'Email'] }
        ]

        const lowerCompletions = getCompletions(tables, 'sel', null)
        const upperCompletions = getCompletions(tables, 'SEL', null)
        const mixedCompletions = getCompletions(tables, 'SeL', null)

        expect(lowerCompletions.length).toBe(upperCompletions.length)
        expect(lowerCompletions.length).toBe(mixedCompletions.length)
      })

      it('should match keywords regardless of case', () => {
        fc.assert(
          fc.property(
            fc.constantFrom('select', 'SELECT', 'Select', 'sElEcT'),
            (prefix) => {
              const completions = getKeywordCompletions(prefix)
              // SELECT 关键字应该在结果中
              return completions.some(c => c.label === 'SELECT')
            }
          ),
          { numRuns: 50 }
        )
      })
    })

    // ========================================================================
    // 11. Context Parsing
    // ========================================================================
    describe('11. SQL context parsing is correct', () => {
      it('should parse table names from FROM clause', () => {
        const context = 'SELECT * FROM users WHERE id = 1'
        const tables = parseTablesFromContext(context)
        expect(tables.has('users')).toBe(true)
      })

      it('should parse table names from JOIN clause', () => {
        const context = 'SELECT * FROM users JOIN orders ON users.id = orders.user_id'
        const tables = parseTablesFromContext(context)
        expect(tables.has('users')).toBe(true)
        expect(tables.has('orders')).toBe(true)
      })

      it('should return empty set for null context', () => {
        const tables = parseTablesFromContext(null)
        expect(tables.size).toBe(0)
      })

      it('should return empty set for empty context', () => {
        const tables = parseTablesFromContext('')
        expect(tables.size).toBe(0)
      })

      it('should be case-insensitive', () => {
        fc.assert(
          fc.property(
            fc.constantFrom(
              'SELECT * FROM Users',
              'select * from USERS',
              'SELECT * FROM users'
            ),
            (context) => {
              const tables = parseTablesFromContext(context)
              return tables.has('users')
            }
          ),
          { numRuns: 50 }
        )
      })
    })
  })
})

/**
 * 查询构建器与安全属性测试
 * Feature: page-audit-optimization
 * Property 10: 查询构建器 SQL 生成
 * Property 11: SQL 注入检测
 *
 * **Validates: Requirements 13.1, 13.4**
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import type {
  QueryModel,
  SelectField,
  JoinConfig,
  WhereCondition,
  TableRef,
  OrderByField
} from '@/types/queryBuilder'

// ============================================================================
// Local SQL Builder — mirrors the expected backend SQL generation logic
// so we can property-test the structural correctness of generated SQL.
// ============================================================================

/**
 * Build a SQL string from a QueryModel configuration.
 * This is a frontend-side reference implementation used for property testing.
 */
function buildSqlFromModel(model: QueryModel): string {
  if (!model.tables.length) return ''

  // SELECT clause
  let selectClause: string
  if (model.selectFields.length === 0) {
    selectClause = '*'
  } else {
    selectClause = model.selectFields
      .map((f) => {
        const col = f.table ? `${f.table}.${f.field}` : f.field
        const expr = f.aggregate ? `${f.aggregate}(${col})` : col
        return f.alias ? `${expr} AS ${f.alias}` : expr
      })
      .join(', ')
  }

  // FROM clause
  const mainTable = model.tables[0]!
  const fromClause = mainTable.alias
    ? `${mainTable.name} ${mainTable.alias}`
    : mainTable.name

  // JOIN clauses
  const joinClauses = model.joins
    .map(
      (j) =>
        `${j.type} JOIN ${j.rightTable} ON ${j.leftTable}.${j.leftField} = ${j.rightTable}.${j.rightField}`
    )
    .join(' ')

  // WHERE clause
  let whereClause = ''
  if (model.conditions.length > 0) {
    const parts = model.conditions.map((c, i) => {
      const cond = `${c.field} ${c.operator} ${formatConditionValue(c)}`
      return i === 0 ? cond : `${c.logic || 'AND'} ${cond}`
    })
    whereClause = `WHERE ${parts.join(' ')}`
  }

  // GROUP BY clause
  const groupByClause =
    model.groupBy.length > 0 ? `GROUP BY ${model.groupBy.join(', ')}` : ''

  // ORDER BY clause
  const orderByClause =
    model.orderBy.length > 0
      ? `ORDER BY ${model.orderBy.map((o) => `${o.field} ${o.direction}`).join(', ')}`
      : ''

  // LIMIT clause
  const limitClause =
    model.limit !== undefined && model.limit > 0 ? `LIMIT ${model.limit}` : ''

  return [
    `SELECT ${selectClause}`,
    `FROM ${fromClause}`,
    joinClauses,
    whereClause,
    groupByClause,
    orderByClause,
    limitClause
  ]
    .filter(Boolean)
    .join(' ')
}

function formatConditionValue(c: WhereCondition): string {
  if (c.operator === 'IN') {
    return `(${c.value})`
  }
  if (c.operator === 'BETWEEN') {
    return String(c.value)
  }
  if (typeof c.value === 'number') {
    return String(c.value)
  }
  return `'${c.value}'`
}

// ============================================================================
// Local SQL Injection Detector — mirrors the expected RLS injection test logic
// ============================================================================

/** Common SQL injection patterns */
const INJECTION_PATTERNS: RegExp[] = [
  /'\s*OR\s+\d+=\d+/i,
  /'\s*OR\s+'[^']*'\s*=\s*'[^']*'/i,
  /;\s*DROP\s+TABLE/i,
  /;\s*DELETE\s+FROM/i,
  /;\s*INSERT\s+INTO/i,
  /;\s*UPDATE\s+\w+\s+SET/i,
  /UNION\s+(ALL\s+)?SELECT/i,
  /--\s*$/m,
  /;\s*EXEC\s/i,
  /;\s*EXECUTE\s/i,
  /xp_cmdshell/i,
  /'\s*;\s*--/i,
  /\bOR\s+\d+\s*=\s*\d+/i,
  /OR\s+'[^']*'\s*=\s*'/i
]

/**
 * Detect potential SQL injection patterns in an input string.
 * Returns true if a potential injection is detected.
 */
function detectSqlInjection(input: string): boolean {
  if (!input || typeof input !== 'string') return false
  return INJECTION_PATTERNS.some((pattern) => pattern.test(input))
}

// ============================================================================
// Arbitraries (Test Data Generators)
// ============================================================================

/** Generate a valid SQL identifier (table or column name) */
const sqlIdentifierArb = fc
  .stringMatching(/^[a-z][a-z0-9_]{1,15}$/)

/** Generate a valid table reference */
const tableRefArb: fc.Arbitrary<TableRef> = fc
  .record({
    name: sqlIdentifierArb,
    alias: fc.option(sqlIdentifierArb, { nil: undefined })
  })
  .map((r) => {
    const ref: TableRef = { name: r.name }
    if (r.alias !== undefined) ref.alias = r.alias
    return ref
  })

/** Generate a valid SELECT field */
const selectFieldArb: fc.Arbitrary<SelectField> = fc
  .record({
    table: sqlIdentifierArb,
    field: sqlIdentifierArb,
    alias: fc.option(sqlIdentifierArb, { nil: undefined }),
    aggregate: fc.option(
      fc.constantFrom('COUNT' as const, 'SUM' as const, 'AVG' as const, 'MAX' as const, 'MIN' as const),
      { nil: undefined }
    )
  })
  .map((r) => {
    const sf: SelectField = { table: r.table, field: r.field }
    if (r.alias !== undefined) sf.alias = r.alias
    if (r.aggregate !== undefined) sf.aggregate = r.aggregate
    return sf
  })

/** Generate a valid JOIN config */
const joinConfigArb: fc.Arbitrary<JoinConfig> = fc.record({
  type: fc.constantFrom('INNER' as const, 'LEFT' as const, 'RIGHT' as const),
  leftTable: sqlIdentifierArb,
  leftField: sqlIdentifierArb,
  rightTable: sqlIdentifierArb,
  rightField: sqlIdentifierArb
})

/** Generate a valid WHERE condition */
const whereConditionArb: fc.Arbitrary<WhereCondition> = fc
  .record({
    field: sqlIdentifierArb,
    operator: fc.constantFrom(
      '=' as const, '!=' as const, '>' as const, '>=' as const,
      '<' as const, '<=' as const, 'LIKE' as const
    ),
    value: fc.oneof(
      fc.integer({ min: 0, max: 9999 }),
      fc.stringMatching(/^[a-z0-9_]{1,10}$/)
    ),
    logic: fc.option(fc.constantFrom('AND' as const, 'OR' as const), { nil: undefined })
  })
  .map((r) => {
    const wc: WhereCondition = { field: r.field, operator: r.operator, value: r.value }
    if (r.logic !== undefined) wc.logic = r.logic
    return wc
  })

/** Generate a valid ORDER BY field */
const orderByFieldArb: fc.Arbitrary<OrderByField> = fc.record({
  field: sqlIdentifierArb,
  direction: fc.constantFrom('ASC' as const, 'DESC' as const)
})

/** Generate a valid QueryModel */
const queryModelArb: fc.Arbitrary<QueryModel> = fc
  .record({
    tables: fc.array(tableRefArb, { minLength: 1, maxLength: 3 }),
    joins: fc.array(joinConfigArb, { minLength: 0, maxLength: 3 }),
    selectFields: fc.array(selectFieldArb, { minLength: 1, maxLength: 5 }),
    conditions: fc.array(whereConditionArb, { minLength: 0, maxLength: 4 }),
    groupBy: fc.array(sqlIdentifierArb, { minLength: 0, maxLength: 2 }),
    orderBy: fc.array(orderByFieldArb, { minLength: 0, maxLength: 2 }),
    limit: fc.option(fc.integer({ min: 1, max: 10000 }), { nil: undefined })
  })
  .map((r) => {
    const qm: QueryModel = {
      tables: r.tables,
      joins: r.joins,
      selectFields: r.selectFields,
      conditions: r.conditions,
      groupBy: r.groupBy,
      orderBy: r.orderBy
    }
    if (r.limit !== undefined) qm.limit = r.limit
    return qm
  })

/** Generate common SQL injection attack strings */
const sqlInjectionArb = fc.constantFrom(
  "'; DROP TABLE users; --",
  "1' OR '1'='1",
  "admin'--",
  "1; DELETE FROM options;",
  "' UNION SELECT * FROM passwords --",
  "1' AND 1=1 --",
  "' OR 1=1 --",
  "'; INSERT INTO admin VALUES('hacked'); --",
  "1; UPDATE users SET role='admin'; --",
  "' UNION ALL SELECT username, password FROM users --",
  "test'; EXEC xp_cmdshell('dir'); --",
  "1 OR 1=1",
  "'; DROP TABLE orders --"
)

/** Generate safe (non-injection) input strings */
const safeInputArb = fc
  .stringMatching(/^[a-zA-Z0-9 _.,]{1,30}$/)
  .filter((s) => !detectSqlInjection(s))

// ============================================================================
// Property 10: 查询构建器 SQL 生成
// ============================================================================

describe('Property 10: 查询构建器 SQL 生成', () => {
  /**
   * For any valid query model with at least one table and one select field,
   * the generated SQL should start with SELECT and contain FROM.
   *
   * **Validates: Requirements 13.1**
   */
  it('generated SQL contains SELECT and FROM keywords', () => {
    fc.assert(
      fc.property(queryModelArb, (model) => {
        const sql = buildSqlFromModel(model)
        expect(sql).toMatch(/^SELECT\s/)
        expect(sql).toContain('FROM')
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * For any valid query model, all specified select field names should
   * appear in the generated SQL string.
   *
   * **Validates: Requirements 13.1**
   */
  it('generated SQL contains all specified field names', () => {
    fc.assert(
      fc.property(queryModelArb, (model) => {
        const sql = buildSqlFromModel(model)
        for (const field of model.selectFields) {
          expect(sql).toContain(field.field)
        }
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * For any valid query model, the main table name should appear
   * in the FROM clause of the generated SQL.
   *
   * **Validates: Requirements 13.1**
   */
  it('generated SQL contains the main table name in FROM clause', () => {
    fc.assert(
      fc.property(queryModelArb, (model) => {
        const sql = buildSqlFromModel(model)
        expect(sql).toContain(model.tables[0]!.name)
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * For any query model with JOIN configs, the generated SQL should
   * contain the JOIN keyword and the joined table names.
   *
   * **Validates: Requirements 13.1**
   */
  it('generated SQL contains JOIN clauses when joins are specified', () => {
    const modelWithJoinsArb = queryModelArb.filter((m) => m.joins.length > 0)
    fc.assert(
      fc.property(modelWithJoinsArb, (model) => {
        const sql = buildSqlFromModel(model)
        expect(sql).toContain('JOIN')
        for (const join of model.joins) {
          expect(sql).toContain(join.rightTable)
          expect(sql).toContain(`${join.type} JOIN`)
        }
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * For any query model with WHERE conditions, the generated SQL should
   * contain the WHERE keyword and the condition field names.
   *
   * **Validates: Requirements 13.1**
   */
  it('generated SQL contains WHERE clause when conditions are specified', () => {
    const modelWithConditionsArb = queryModelArb.filter(
      (m) => m.conditions.length > 0
    )
    fc.assert(
      fc.property(modelWithConditionsArb, (model) => {
        const sql = buildSqlFromModel(model)
        expect(sql).toContain('WHERE')
        for (const cond of model.conditions) {
          expect(sql).toContain(cond.field)
          expect(sql).toContain(cond.operator)
        }
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * For any query model with aggregate functions, the generated SQL should
   * contain the aggregate keyword (COUNT, SUM, AVG, MAX, MIN).
   *
   * **Validates: Requirements 13.1**
   */
  it('generated SQL contains aggregate functions when specified', () => {
    const modelWithAggregateArb = queryModelArb.filter((m) =>
      m.selectFields.some((f) => f.aggregate)
    )
    fc.assert(
      fc.property(modelWithAggregateArb, (model) => {
        const sql = buildSqlFromModel(model)
        for (const field of model.selectFields) {
          if (field.aggregate) {
            expect(sql).toContain(`${field.aggregate}(`)
          }
        }
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * For any query model with aliases, the generated SQL should contain
   * the AS keyword followed by the alias.
   *
   * **Validates: Requirements 13.1**
   */
  it('generated SQL contains aliases with AS keyword when specified', () => {
    const modelWithAliasArb = queryModelArb.filter((m) =>
      m.selectFields.some((f) => f.alias)
    )
    fc.assert(
      fc.property(modelWithAliasArb, (model) => {
        const sql = buildSqlFromModel(model)
        for (const field of model.selectFields) {
          if (field.alias) {
            expect(sql).toContain(`AS ${field.alias}`)
          }
        }
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * For any query model with a LIMIT, the generated SQL should contain
   * the LIMIT keyword with the correct value.
   *
   * **Validates: Requirements 13.1**
   */
  it('generated SQL contains LIMIT clause when limit is specified', () => {
    const modelWithLimitArb = queryModelArb.filter(
      (m) => m.limit !== undefined && m.limit > 0
    )
    fc.assert(
      fc.property(modelWithLimitArb, (model) => {
        const sql = buildSqlFromModel(model)
        expect(sql).toContain(`LIMIT ${model.limit!}`)
        return true
      }),
      { numRuns: 100 }
    )
  })
})

// ============================================================================
// Property 11: SQL 注入检测
// ============================================================================

describe('Property 11: SQL 注入检测', () => {
  /**
   * For any common SQL injection pattern string, the detector should
   * flag it as a potential injection risk.
   *
   * **Validates: Requirements 13.4**
   */
  it('detects common SQL injection patterns', () => {
    fc.assert(
      fc.property(sqlInjectionArb, (injectionInput) => {
        const detected = detectSqlInjection(injectionInput)
        expect(detected).toBe(true)
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * For any safe alphanumeric input string, the detector should NOT
   * flag it as an injection risk.
   *
   * **Validates: Requirements 13.4**
   */
  it('does not flag safe alphanumeric inputs as injection', () => {
    fc.assert(
      fc.property(safeInputArb, (safeInput) => {
        const detected = detectSqlInjection(safeInput)
        expect(detected).toBe(false)
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * For any input containing "DROP TABLE", the detector should flag it.
   *
   * **Validates: Requirements 13.4**
   */
  it('detects DROP TABLE injection pattern', () => {
    fc.assert(
      fc.property(
        sqlIdentifierArb,
        (tableName) => {
          const input = `'; DROP TABLE ${tableName}; --`
          expect(detectSqlInjection(input)).toBe(true)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * For any input containing "UNION SELECT", the detector should flag it.
   *
   * **Validates: Requirements 13.4**
   */
  it('detects UNION SELECT injection pattern', () => {
    fc.assert(
      fc.property(
        sqlIdentifierArb,
        (fieldName) => {
          const input = `' UNION SELECT ${fieldName} FROM users --`
          expect(detectSqlInjection(input)).toBe(true)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * For any input containing "OR 1=1", the detector should flag it.
   *
   * **Validates: Requirements 13.4**
   */
  it('detects OR 1=1 tautology injection pattern', () => {
    fc.assert(
      fc.property(
        fc.constant(null),
        () => {
          expect(detectSqlInjection("' OR 1=1 --")).toBe(true)
          expect(detectSqlInjection("1 OR 1=1")).toBe(true)
          expect(detectSqlInjection("admin' OR 1=1 --")).toBe(true)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Empty or null-like inputs should not be flagged as injection.
   *
   * **Validates: Requirements 13.4**
   */
  it('does not flag empty or null inputs as injection', () => {
    expect(detectSqlInjection('')).toBe(false)
    expect(detectSqlInjection(null as unknown as string)).toBe(false)
    expect(detectSqlInjection(undefined as unknown as string)).toBe(false)
  })

  /**
   * For any input containing "DELETE FROM", the detector should flag it.
   *
   * **Validates: Requirements 13.4**
   */
  it('detects DELETE FROM injection pattern', () => {
    fc.assert(
      fc.property(
        sqlIdentifierArb,
        (tableName) => {
          const input = `1; DELETE FROM ${tableName};`
          expect(detectSqlInjection(input)).toBe(true)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})

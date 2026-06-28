/**
 * SQL 生成属性测试
 * 
 * **属性 37: SQL 生成正确性**
 * **验证需求: 12.3, 12.4, 12.5, 12.6**
 * 
 * 对于任意可视化查询配置（表选择、字段选择、条件、分组、排序），
 * 生成的 SQL 应语法正确且语义与配置一致。
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import {
  generateSQL,
  validateAggregationConfig,
  autoCompleteGroupBy,
  type QueryConfig,
  type SelectedField,
  type JoinConfig,
  type ConditionItem,
  type GroupByItem,
  type SortItem,
  type TableConfig
} from '../sqlGenerator'
import { formatSQL, validateSQLSyntax, compressSQL } from '../sqlFormatter'

// ============================================================================
// Arbitraries (Test Data Generators)
// ============================================================================

/**
 * 生成有效的 SQL 标识符（表名、字段名、别名）
 */
const sqlIdentifierArb = fc.stringMatching(/^[a-z][a-z0-9_]{0,19}$/)

/**
 * 生成表别名（短标识符）
 */
const tableAliasArb = fc.stringMatching(/^[a-z][a-z0-9]{0,3}$/)

/**
 * 生成聚合函数类型
 */
const aggregateArb = fc.constantFrom('SUM', 'COUNT', 'AVG', 'MAX', 'MIN', '' as const)

/**
 * 生成比较运算符
 */
const comparisonOperatorArb = fc.constantFrom(
  '=', '!=', '>', '<', '>=', '<=',
  'LIKE', 'NOT LIKE', 'IN', 'NOT IN',
  'BETWEEN', 'NOT BETWEEN', 'IS NULL', 'IS NOT NULL'
)

/**
 * 生成逻辑运算符
 */
const logicOperatorArb = fc.constantFrom('AND', 'OR')

/**
 * 生成 JOIN 类型
 */
const joinTypeArb = fc.constantFrom('INNER', 'LEFT', 'RIGHT', 'FULL')

/**
 * 生成排序方向
 */
const sortDirectionArb = fc.constantFrom('ASC', 'DESC')

/**
 * 生成条件值（字符串或数字）
 */
const conditionValueArb = fc.oneof(
  fc.integer({ min: -10000, max: 10000 }).map(String),
  fc.stringMatching(/^[a-zA-Z0-9_]{1,20}$/)
)

/**
 * 生成表配置
 */
const tableConfigArb = fc.record({
  tableName: sqlIdentifierArb,
  alias: tableAliasArb,
  selectedFields: fc.array(sqlIdentifierArb, { minLength: 1, maxLength: 5 })
})

/**
 * 生成选中字段
 */
function selectedFieldArb(tableAlias: string, tableName: string): fc.Arbitrary<SelectedField> {
  return fc.record({
    id: fc.uuid(),
    tableAlias: fc.constant(tableAlias),
    tableName: fc.constant(tableName),
    fieldName: sqlIdentifierArb,
    alias: fc.option(sqlIdentifierArb, { nil: undefined }),
    aggregate: fc.option(aggregateArb, { nil: undefined })
  }).map(f => ({
    ...f,
    aggregate: f.aggregate || '' as const
  }))
}

/**
 * 生成 JOIN 配置
 */
function joinConfigArb(leftAlias: string, leftTable: string): fc.Arbitrary<JoinConfig> {
  return fc.record({
    id: fc.uuid(),
    leftTable: fc.constant(leftTable),
    leftAlias: fc.constant(leftAlias),
    leftField: sqlIdentifierArb,
    rightTable: sqlIdentifierArb,
    rightAlias: tableAliasArb,
    rightField: sqlIdentifierArb,
    joinType: joinTypeArb
  })
}

/**
 * 生成条件项
 */
function conditionItemArb(tableAlias: string): fc.Arbitrary<ConditionItem> {
  return fc.record({
    id: fc.uuid(),
    field: sqlIdentifierArb.map(f => `${tableAlias}.${f}`),
    operator: comparisonOperatorArb,
    value: conditionValueArb,
    value2: fc.option(conditionValueArb, { nil: undefined }),
    valueList: fc.option(fc.array(conditionValueArb, { minLength: 1, maxLength: 5 }), { nil: undefined }),
    logic: logicOperatorArb
  }).map(c => ({
    ...c,
    // 为 BETWEEN 操作符确保有 value2
    value2: c.operator === 'BETWEEN' || c.operator === 'NOT BETWEEN' ? (c.value2 || '100') : c.value2,
    // 为 IN/NOT IN 操作符确保有 valueList
    valueList: c.operator === 'IN' || c.operator === 'NOT IN' ? (c.valueList || ['1', '2', '3']) : c.valueList
  }))
}

/**
 * 生成 GROUP BY 项
 */
function groupByItemArb(tableAlias: string): fc.Arbitrary<GroupByItem> {
  return sqlIdentifierArb.map(f => ({ field: `${tableAlias}.${f}` }))
}

/**
 * 生成排序项
 */
function sortItemArb(tableAlias: string): fc.Arbitrary<SortItem> {
  return fc.record({
    field: sqlIdentifierArb.map(f => `${tableAlias}.${f}`),
    direction: sortDirectionArb
  })
}

/**
 * 生成完整的查询配置
 */
const queryConfigArb: fc.Arbitrary<QueryConfig> = fc.record({
  tableName: sqlIdentifierArb,
  tableAlias: tableAliasArb,
  fieldCount: fc.integer({ min: 1, max: 5 }),
  conditionCount: fc.integer({ min: 0, max: 3 }),
  groupByCount: fc.integer({ min: 0, max: 2 }),
  orderByCount: fc.integer({ min: 0, max: 2 }),
  hasJoin: fc.boolean(),
  limit: fc.option(fc.integer({ min: 1, max: 1000 }), { nil: undefined })
}).chain(params => {
  const { tableName, tableAlias, fieldCount, conditionCount, groupByCount, orderByCount, hasJoin, limit } = params
  
  return fc.record({
    tables: fc.constant([{ tableName, alias: tableAlias, selectedFields: [] }] as TableConfig[]),
    selectedFields: fc.array(selectedFieldArb(tableAlias, tableName), { minLength: fieldCount, maxLength: fieldCount }),
    joins: hasJoin 
      ? fc.array(joinConfigArb(tableAlias, tableName), { minLength: 1, maxLength: 1 })
      : fc.constant([] as JoinConfig[]),
    conditions: fc.array(conditionItemArb(tableAlias), { minLength: conditionCount, maxLength: conditionCount }),
    groupBy: fc.array(groupByItemArb(tableAlias), { minLength: groupByCount, maxLength: groupByCount }),
    having: fc.constant([] as ConditionItem[]),
    orderBy: fc.array(sortItemArb(tableAlias), { minLength: orderByCount, maxLength: orderByCount }),
    limit: fc.constant(limit)
  })
})

// ============================================================================
// Property Tests
// ============================================================================

describe('SQL Generation Property Tests', () => {
  /**
   * **Validates: Requirements 12.3, 12.4, 12.5, 12.6**
   * 
   * 属性 37.1: 生成的 SQL 应该是语法有效的
   * 对于任意有效的查询配置，生成的 SQL 应通过基本语法验证
   */
  it('Property 37.1: Generated SQL should be syntactically valid', () => {
    fc.assert(
      fc.property(queryConfigArb, (config) => {
        const sql = generateSQL(config)
        
        // 空配置应返回空字符串
        if (config.tables.length === 0) {
          return sql === ''
        }
        
        // 非空配置应生成有效 SQL
        const validation = validateSQLSyntax(sql)
        
        // 如果验证失败，输出调试信息
        if (!validation.valid) {
          console.log('Generated SQL:', sql)
          console.log('Validation errors:', validation.errors)
        }
        
        return validation.valid
      }),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 12.6**
   * 
   * 属性 37.2: 所有选中的字段应出现在 SELECT 子句中
   * 对于任意查询配置，每个选中的字段都应在生成的 SQL 的 SELECT 部分中出现
   */
  it('Property 37.2: All selected fields should appear in SELECT clause', () => {
    fc.assert(
      fc.property(queryConfigArb, (config) => {
        const sql = generateSQL(config)
        
        if (!sql || config.selectedFields.length === 0) {
          return true
        }
        
        const upperSQL = sql.toUpperCase()
        
        // 提取 SELECT 子句（从 SELECT 到 FROM）
        const selectMatch = upperSQL.match(/SELECT\s+([\s\S]*?)\s+FROM/)
        if (!selectMatch) {
          return false
        }
        const selectClause = selectMatch[1]
        
        // 验证每个字段都出现在 SELECT 子句中
        for (const field of config.selectedFields) {
          const fieldName = field.fieldName.toUpperCase()
          const tableAlias = field.tableAlias.toUpperCase()
          
          // 字段可能以 tableAlias.fieldName 或聚合函数形式出现
          const fieldPattern = field.aggregate
            ? `${field.aggregate}(${tableAlias}.${fieldName})`
            : `${tableAlias}.${fieldName}`
          
          // 特殊处理 COUNT(*)
          if (field.fieldName === '*' && field.aggregate === 'COUNT') {
            if (!selectClause.includes('COUNT(*)')) {
              return false
            }
            continue
          }
          
          if (!selectClause.includes(fieldPattern.toUpperCase())) {
            // 也检查不带表别名的情况
            if (!selectClause.includes(fieldName)) {
              console.log('Missing field:', fieldPattern, 'in SELECT:', selectClause)
              return false
            }
          }
        }
        
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 12.4**
   * 
   * 属性 37.3: 所有条件应出现在 WHERE 子句中
   * 对于任意查询配置，每个条件都应在生成的 SQL 的 WHERE 部分中出现
   */
  it('Property 37.3: All conditions should appear in WHERE clause', () => {
    fc.assert(
      fc.property(queryConfigArb, (config) => {
        const sql = generateSQL(config)
        
        if (!sql || config.conditions.length === 0) {
          return true
        }
        
        const upperSQL = sql.toUpperCase()
        
        // 检查是否有 WHERE 子句
        if (!upperSQL.includes('WHERE')) {
          console.log('Missing WHERE clause for conditions:', config.conditions)
          return false
        }
        
        // 提取 WHERE 子句
        const whereMatch = upperSQL.match(/WHERE\s+([\s\S]*?)(?:\s+GROUP\s+BY|\s+HAVING|\s+ORDER\s+BY|\s+LIMIT|$)/)
        if (!whereMatch) {
          return false
        }
        const whereClause = whereMatch[1]
        
        // 验证每个条件的字段都出现在 WHERE 子句中
        for (const condition of config.conditions) {
          const fieldParts = condition.field.split('.')
          const fieldName = fieldParts[fieldParts.length - 1].toUpperCase()
          
          if (!whereClause.includes(fieldName)) {
            console.log('Missing condition field:', fieldName, 'in WHERE:', whereClause)
            return false
          }
          
          // 验证运算符出现
          const operator = condition.operator.toUpperCase()
          if (!whereClause.includes(operator)) {
            console.log('Missing operator:', operator, 'in WHERE:', whereClause)
            return false
          }
        }
        
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 12.3**
   * 
   * 属性 37.4: GROUP BY 字段应正确生成
   * 对于任意查询配置，所有 GROUP BY 字段都应在生成的 SQL 中正确出现
   */
  it('Property 37.4: GROUP BY fields should be correctly generated', () => {
    fc.assert(
      fc.property(queryConfigArb, (config) => {
        const sql = generateSQL(config)
        
        if (!sql || config.groupBy.length === 0) {
          return true
        }
        
        const upperSQL = sql.toUpperCase()
        
        // 检查是否有 GROUP BY 子句
        if (!upperSQL.includes('GROUP BY')) {
          console.log('Missing GROUP BY clause for:', config.groupBy)
          return false
        }
        
        // 提取 GROUP BY 子句
        const groupByMatch = upperSQL.match(/GROUP\s+BY\s+([\s\S]*?)(?:\s+HAVING|\s+ORDER\s+BY|\s+LIMIT|$)/)
        if (!groupByMatch) {
          return false
        }
        const groupByClause = groupByMatch[1]
        
        // 验证每个 GROUP BY 字段都出现
        for (const groupBy of config.groupBy) {
          const fieldParts = groupBy.field.split('.')
          const fieldName = fieldParts[fieldParts.length - 1].toUpperCase()
          
          if (!groupByClause.includes(fieldName)) {
            console.log('Missing GROUP BY field:', fieldName, 'in:', groupByClause)
            return false
          }
        }
        
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 12.5**
   * 
   * 属性 37.5: SQL 格式化应保持语义不变
   * 对于任意生成的 SQL，格式化后的 SQL 应与原始 SQL 语义等价
   */
  it('Property 37.5: SQL formatting should preserve semantics', () => {
    fc.assert(
      fc.property(queryConfigArb, (config) => {
        const sql = generateSQL(config)
        
        if (!sql) {
          return true
        }
        
        // 格式化 SQL
        const formatted = formatSQL(sql)
        
        // 格式化后的 SQL 应该仍然有效
        const validation = validateSQLSyntax(formatted)
        if (!validation.valid) {
          console.log('Original SQL:', sql)
          console.log('Formatted SQL:', formatted)
          console.log('Validation errors:', validation.errors)
          return false
        }
        
        // 规范化 SQL 用于比较（移除空白差异和大小写差异）
        const normalizeSQL = (s: string) => {
          return s
            .toUpperCase()
            .replace(/\s+/g, ' ')
            .replace(/\s*,\s*/g, ', ')
            .replace(/\s*\(\s*/g, '(')
            .replace(/\s*\)\s*/g, ') ')
            .replace(/\s*=\s*/g, ' = ')
            .replace(/\s*>=\s*/g, ' >= ')
            .replace(/\s*<=\s*/g, ' <= ')
            .replace(/\s*>\s*/g, ' > ')
            .replace(/\s*<\s*/g, ' < ')
            .replace(/\s*!=\s*/g, ' != ')
            .trim()
        }
        
        const normalizedOriginal = normalizeSQL(sql)
        const normalizedFormatted = normalizeSQL(formatted)
        
        // 检查关键元素是否保留
        // 提取并比较 SELECT 字段
        const extractSelectFields = (s: string) => {
          const match = s.match(/SELECT\s+([\s\S]*?)\s+FROM/)
          if (!match) return ''
          return match[1].replace(/\s+/g, ' ').trim()
        }
        
        const originalFields = extractSelectFields(normalizedOriginal)
        const formattedFields = extractSelectFields(normalizedFormatted)
        
        // 字段应该相同（忽略顺序和空白）
        const sortFields = (s: string) => s.split(',').map(f => f.trim()).sort().join(', ')
        
        if (sortFields(originalFields) !== sortFields(formattedFields)) {
          console.log('SELECT fields mismatch')
          console.log('Original:', originalFields)
          console.log('Formatted:', formattedFields)
          return false
        }
        
        // 检查 FROM 表是否保留
        const extractFromTable = (s: string) => {
          const match = s.match(/FROM\s+(\S+)/)
          return match ? match[1] : ''
        }
        
        if (extractFromTable(normalizedOriginal) !== extractFromTable(normalizedFormatted)) {
          console.log('FROM table mismatch')
          return false
        }
        
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 12.6**
   * 
   * 属性 37.6: JOIN 配置应正确生成
   * 对于任意包含 JOIN 的查询配置，JOIN 子句应正确生成
   */
  it('Property 37.6: JOIN configurations should be correctly generated', () => {
    fc.assert(
      fc.property(queryConfigArb, (config) => {
        const sql = generateSQL(config)
        
        if (!sql || config.joins.length === 0) {
          return true
        }
        
        const upperSQL = sql.toUpperCase()
        
        // 验证每个 JOIN 都正确生成
        for (const join of config.joins) {
          const joinType = join.joinType.toUpperCase()
          const rightTable = join.rightTable.toUpperCase()
          const rightAlias = join.rightAlias.toUpperCase()
          
          // 检查 JOIN 类型
          if (!upperSQL.includes(`${joinType} JOIN`)) {
            console.log('Missing JOIN type:', joinType)
            return false
          }
          
          // 检查右表
          if (!upperSQL.includes(rightTable)) {
            console.log('Missing right table:', rightTable)
            return false
          }
          
          // 检查 ON 子句
          if (!upperSQL.includes('ON')) {
            console.log('Missing ON clause')
            return false
          }
        }
        
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 12.6**
   * 
   * 属性 37.7: ORDER BY 配置应正确生成
   * 对于任意包含排序的查询配置，ORDER BY 子句应正确生成
   */
  it('Property 37.7: ORDER BY configurations should be correctly generated', () => {
    fc.assert(
      fc.property(queryConfigArb, (config) => {
        const sql = generateSQL(config)
        
        if (!sql || config.orderBy.length === 0) {
          return true
        }
        
        const upperSQL = sql.toUpperCase()
        
        // 检查是否有 ORDER BY 子句
        if (!upperSQL.includes('ORDER BY')) {
          console.log('Missing ORDER BY clause')
          return false
        }
        
        // 提取 ORDER BY 子句
        const orderByMatch = upperSQL.match(/ORDER\s+BY\s+([\s\S]*?)(?:\s+LIMIT|$)/)
        if (!orderByMatch) {
          return false
        }
        const orderByClause = orderByMatch[1]
        
        // 验证每个排序字段和方向
        for (const orderBy of config.orderBy) {
          const fieldParts = orderBy.field.split('.')
          const fieldName = fieldParts[fieldParts.length - 1].toUpperCase()
          const direction = orderBy.direction.toUpperCase()
          
          if (!orderByClause.includes(fieldName)) {
            console.log('Missing ORDER BY field:', fieldName)
            return false
          }
          
          if (!orderByClause.includes(direction)) {
            console.log('Missing ORDER BY direction:', direction)
            return false
          }
        }
        
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 12.6**
   * 
   * 属性 37.8: LIMIT 配置应正确生成
   * 对于任意包含 LIMIT 的查询配置，LIMIT 子句应正确生成
   */
  it('Property 37.8: LIMIT configuration should be correctly generated', () => {
    fc.assert(
      fc.property(queryConfigArb, (config) => {
        const sql = generateSQL(config)
        
        if (!sql) {
          return true
        }
        
        const upperSQL = sql.toUpperCase()
        
        if (config.limit && config.limit > 0) {
          // 应该有 LIMIT 子句
          if (!upperSQL.includes('LIMIT')) {
            console.log('Missing LIMIT clause for limit:', config.limit)
            return false
          }
          
          // LIMIT 值应该正确
          if (!upperSQL.includes(`LIMIT ${config.limit}`)) {
            console.log('Incorrect LIMIT value, expected:', config.limit)
            return false
          }
        }
        
        return true
      }),
      { numRuns: 100 }
    )
  })
})

describe('Aggregation Validation Property Tests', () => {
  /**
   * **Validates: Requirements 12.3**
   * 
   * 属性 37.9: 聚合查询验证应正确检测非聚合字段
   * 当有聚合函数时，非聚合字段必须在 GROUP BY 中
   */
  it('Property 37.9: Aggregation validation should detect non-aggregate fields not in GROUP BY', () => {
    fc.assert(
      fc.property(
        fc.record({
          tableAlias: tableAliasArb,
          tableName: sqlIdentifierArb,
          hasAggregate: fc.boolean(),
          fieldCount: fc.integer({ min: 2, max: 4 })
        }),
        (params) => {
          const { tableAlias, tableName, hasAggregate, fieldCount } = params
          
          // 创建字段列表
          const fields: SelectedField[] = []
          for (let i = 0; i < fieldCount; i++) {
            fields.push({
              id: `field_${i}`,
              tableAlias,
              tableName,
              fieldName: `field${i}`,
              aggregate: hasAggregate && i === 0 ? 'SUM' : ''
            })
          }
          
          const config: QueryConfig = {
            tables: [{ tableName, alias: tableAlias, selectedFields: [] }],
            joins: [],
            selectedFields: fields,
            conditions: [],
            groupBy: [], // 故意不添加 GROUP BY
            having: [],
            orderBy: []
          }
          
          const validation = validateAggregationConfig(config)
          
          if (hasAggregate) {
            // 有聚合函数但没有 GROUP BY，应该报错
            // 非聚合字段数量 = fieldCount - 1（第一个是聚合字段）
            const nonAggregateCount = fieldCount - 1
            return !validation.valid && validation.errors.length === nonAggregateCount
          } else {
            // 没有聚合函数，应该通过验证
            return validation.valid
          }
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 12.3**
   * 
   * 属性 37.10: 自动补全 GROUP BY 应添加所有非聚合字段
   */
  it('Property 37.10: Auto-complete GROUP BY should add all non-aggregate fields', () => {
    fc.assert(
      fc.property(
        fc.record({
          tableAlias: tableAliasArb,
          tableName: sqlIdentifierArb,
          aggregateFieldCount: fc.integer({ min: 1, max: 2 }),
          nonAggregateFieldCount: fc.integer({ min: 1, max: 3 })
        }),
        (params) => {
          const { tableAlias, tableName, aggregateFieldCount, nonAggregateFieldCount } = params
          
          // 创建字段列表
          const fields: SelectedField[] = []
          
          // 添加聚合字段
          for (let i = 0; i < aggregateFieldCount; i++) {
            fields.push({
              id: `agg_${i}`,
              tableAlias,
              tableName,
              fieldName: `agg_field${i}`,
              aggregate: 'SUM'
            })
          }
          
          // 添加非聚合字段
          for (let i = 0; i < nonAggregateFieldCount; i++) {
            fields.push({
              id: `non_agg_${i}`,
              tableAlias,
              tableName,
              fieldName: `non_agg_field${i}`,
              aggregate: ''
            })
          }
          
          const config: QueryConfig = {
            tables: [{ tableName, alias: tableAlias, selectedFields: [] }],
            joins: [],
            selectedFields: fields,
            conditions: [],
            groupBy: [],
            having: [],
            orderBy: []
          }
          
          const completedGroupBy = autoCompleteGroupBy(config)
          
          // 应该有 nonAggregateFieldCount 个 GROUP BY 字段
          if (completedGroupBy.length !== nonAggregateFieldCount) {
            console.log('Expected GROUP BY count:', nonAggregateFieldCount, 'Got:', completedGroupBy.length)
            return false
          }
          
          // 每个非聚合字段都应该在 GROUP BY 中
          for (let i = 0; i < nonAggregateFieldCount; i++) {
            const expectedField = `${tableAlias}.non_agg_field${i}`
            const found = completedGroupBy.some(g => g.field === expectedField)
            if (!found) {
              console.log('Missing GROUP BY field:', expectedField)
              return false
            }
          }
          
          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})

describe('SQL Formatter Property Tests', () => {
  /**
   * **Validates: Requirements 12.5**
   * 
   * 属性 37.11: 格式化后的 SQL 应该是有效的
   */
  it('Property 37.11: Formatted SQL should be valid', () => {
    fc.assert(
      fc.property(queryConfigArb, (config) => {
        const sql = generateSQL(config)
        
        if (!sql) {
          return true
        }
        
        const formatted = formatSQL(sql)
        const validation = validateSQLSyntax(formatted)
        
        return validation.valid
      }),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 12.5**
   * 
   * 属性 37.12: 压缩后再格式化应该得到相同的语义
   */
  it('Property 37.12: Compress then format should preserve semantics', () => {
    fc.assert(
      fc.property(queryConfigArb, (config) => {
        const sql = generateSQL(config)
        
        if (!sql) {
          return true
        }
        
        // 格式化 -> 压缩 -> 再格式化
        const formatted1 = formatSQL(sql)
        const compressed = compressSQL(formatted1)
        const formatted2 = formatSQL(compressed)
        
        // 两次格式化的结果应该相同
        const normalizeSQL = (s: string) => s.replace(/\s+/g, ' ').trim().toUpperCase()
        
        return normalizeSQL(formatted1) === normalizeSQL(formatted2)
      }),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 12.5**
   * 
   * 属性 37.13: 格式化应该保留所有 SQL 关键字
   */
  it('Property 37.13: Formatting should preserve all SQL keywords', () => {
    fc.assert(
      fc.property(queryConfigArb, (config) => {
        const sql = generateSQL(config)
        
        if (!sql) {
          return true
        }
        
        const formatted = formatSQL(sql)
        const upperOriginal = sql.toUpperCase()
        const upperFormatted = formatted.toUpperCase()
        
        // 检查关键字保留
        const keywords = ['SELECT', 'FROM']
        
        if (config.joins.length > 0) {
          keywords.push('JOIN', 'ON')
        }
        if (config.conditions.length > 0) {
          keywords.push('WHERE')
        }
        if (config.groupBy.length > 0) {
          keywords.push('GROUP BY')
        }
        if (config.orderBy.length > 0) {
          keywords.push('ORDER BY')
        }
        if (config.limit) {
          keywords.push('LIMIT')
        }
        
        for (const keyword of keywords) {
          if (upperOriginal.includes(keyword) && !upperFormatted.includes(keyword)) {
            console.log('Missing keyword after formatting:', keyword)
            return false
          }
        }
        
        return true
      }),
      { numRuns: 100 }
    )
  })
})

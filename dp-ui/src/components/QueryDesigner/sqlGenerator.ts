/**
 * 可视化查询配置 → SQL 生成器
 */

export interface SelectedField {
  id?: string
  tableAlias: string
  tableName: string
  fieldName: string
  alias?: string
  aggregate?: 'SUM' | 'COUNT' | 'AVG' | 'MAX' | 'MIN' | ''
}

export interface JoinConfig {
  id: string
  leftTable: string
  leftAlias: string
  leftField: string
  rightTable: string
  rightAlias: string
  rightField: string
  joinType: 'INNER' | 'LEFT' | 'RIGHT' | 'FULL'
}

export interface ConditionItem {
  id: string
  field: string        // 格式: tableAlias.fieldName
  operator: string     // =, !=, >, <, >=, <=, LIKE, NOT LIKE, IN, NOT IN, BETWEEN, NOT BETWEEN, IS NULL, IS NOT NULL
  value: string
  value2?: string      // BETWEEN 第二个值
  valueList?: string[] // IN 操作符的值列表
  logic: 'AND' | 'OR'  // 与下一个条件的逻辑关系
  // 用于嵌套条件组
  type?: 'condition' | 'group'
  groupStart?: boolean
  groupEnd?: boolean
  groupLogic?: 'AND' | 'OR'
}

export interface ConditionGroup {
  id: string
  type: 'group'
  logic: 'AND' | 'OR'
  children: (ConditionItem | ConditionGroup)[]
}

export interface SortItem {
  field: string        // 格式: tableAlias.fieldName
  direction: 'ASC' | 'DESC'
}

export interface GroupByItem {
  field: string        // 格式: tableAlias.fieldName
}

export interface TableConfig {
  tableName: string
  alias: string
  selectedFields: string[]  // 选中的字段名列表
}

export interface QueryConfig {
  tables: TableConfig[]
  joins: JoinConfig[]
  selectedFields: SelectedField[]
  conditions: ConditionItem[]
  groupBy: GroupByItem[]
  having: ConditionItem[]
  orderBy: SortItem[]
  limit?: number
}

/**
 * 根据可视化配置生成 SQL
 */
export function generateSQL(config: QueryConfig): string {
  if (!config.tables.length) return ''

  const parts: string[] = []

  // SELECT
  const selectParts = buildSelectClause(config.selectedFields)
  if (!selectParts) return ''
  parts.push(`SELECT ${selectParts}`)

  // FROM
  const mainTable = config.tables[0]
  parts.push(`FROM ${mainTable.tableName} AS ${mainTable.alias}`)

  // JOINs
  for (const join of config.joins) {
    parts.push(`${join.joinType} JOIN ${join.rightTable} AS ${join.rightAlias} ON ${join.leftAlias}.${join.leftField} = ${join.rightAlias}.${join.rightField}`)
  }

  // WHERE
  const whereClause = buildConditionClause(config.conditions)
  if (whereClause) {
    parts.push(`WHERE ${whereClause}`)
  }

  // GROUP BY
  if (config.groupBy.length > 0) {
    parts.push(`GROUP BY ${config.groupBy.map(g => g.field).join(', ')}`)
  }

  // HAVING
  const havingClause = buildConditionClause(config.having)
  if (havingClause) {
    parts.push(`HAVING ${havingClause}`)
  }

  // ORDER BY
  if (config.orderBy.length > 0) {
    parts.push(`ORDER BY ${config.orderBy.map(o => `${o.field} ${o.direction}`).join(', ')}`)
  }

  // LIMIT
  if (config.limit && config.limit > 0) {
    parts.push(`LIMIT ${config.limit}`)
  }

  return parts.join('\n')
}

function buildSelectClause(fields: SelectedField[]): string {
  if (!fields.length) return '*'

  return fields.map(f => {
    let expr: string
    
    // 处理 COUNT(*) 特殊情况
    if (f.fieldName === '*') {
      expr = f.aggregate ? `${f.aggregate}(*)` : '*'
    } else {
      expr = `${f.tableAlias}.${f.fieldName}`
      if (f.aggregate) {
        expr = `${f.aggregate}(${expr})`
      }
    }
    
    if (f.alias && f.alias !== f.fieldName) {
      expr += ` AS ${f.alias}`
    }
    return expr
  }).join(',\n  ')
}

function buildConditionClause(conditions: ConditionItem[]): string {
  if (!conditions.length) return ''

  // 检查是否有嵌套组标记
  const hasGroups = conditions.some(c => c.groupStart || c.groupEnd)
  
  if (hasGroups) {
    return buildNestedConditionClause(conditions)
  }

  return conditions.map((c, i) => {
    const expr = buildSingleCondition(c)

    if (i > 0) {
      return `${conditions[i - 1].logic} ${expr}`
    }
    return expr
  }).join(' ')
}

/**
 * 构建单个条件的 SQL 表达式
 */
function buildSingleCondition(c: ConditionItem): string {
  switch (c.operator) {
    case 'IS NULL':
      return `${c.field} IS NULL`
    case 'IS NOT NULL':
      return `${c.field} IS NOT NULL`
    case 'IN':
      return `${c.field} IN (${formatValueList(c.valueList || c.value)})`
    case 'NOT IN':
      return `${c.field} NOT IN (${formatValueList(c.valueList || c.value)})`
    case 'BETWEEN':
      return `${c.field} BETWEEN ${formatValue(c.value)} AND ${formatValue(c.value2 || '')}`
    case 'NOT BETWEEN':
      return `${c.field} NOT BETWEEN ${formatValue(c.value)} AND ${formatValue(c.value2 || '')}`
    case 'LIKE':
    case 'NOT LIKE':
      return `${c.field} ${c.operator} ${formatValue(c.value)}`
    default:
      return `${c.field} ${c.operator} ${formatValue(c.value)}`
  }
}

/**
 * 构建嵌套条件的 SQL（支持括号分组）
 */
function buildNestedConditionClause(conditions: ConditionItem[]): string {
  const parts: string[] = []
  const groupStack: { logic: string; startIndex: number }[] = []
  const currentGroupParts: string[][] = [[]]
  
  for (let i = 0; i < conditions.length; i++) {
    const c = conditions[i]
    
    // 处理组开始
    if (c.groupStart) {
      groupStack.push({ logic: c.groupLogic || 'AND', startIndex: currentGroupParts.length })
      currentGroupParts.push([])
    }
    
    // 构建当前条件
    const expr = buildSingleCondition(c)
    const currentParts = currentGroupParts[currentGroupParts.length - 1]
    
    if (currentParts.length > 0) {
      const prevLogic = i > 0 ? conditions[i - 1].logic : 'AND'
      currentParts.push(prevLogic)
    }
    currentParts.push(expr)
    
    // 处理组结束
    if (c.groupEnd && groupStack.length > 0) {
      const group = groupStack.pop()!
      const groupParts = currentGroupParts.pop()!
      const groupSQL = groupParts.join(' ')
      
      // 将组添加到父级
      const parentParts = currentGroupParts[currentGroupParts.length - 1]
      if (parentParts.length > 0) {
        parentParts.push(group.logic)
      }
      parentParts.push(`(${groupSQL})`)
    }
  }
  
  return currentGroupParts[0].join(' ')
}

/**
 * 格式化值列表（用于 IN 操作符）
 */
function formatValueList(values: string[] | string): string {
  if (Array.isArray(values)) {
    if (values.length === 0) return "''"
    return values.map(v => formatValue(v)).join(', ')
  }
  // 如果是字符串，按逗号分割
  if (!values) return "''"
  return values.split(',').map(v => formatValue(v.trim())).join(', ')
}

function formatValue(val: string): string {
  if (!val) return "''"
  // 如果是数字，不加引号
  if (/^-?\d+(\.\d+)?$/.test(val)) return val
  // 如果已经有引号，直接返回
  if ((val.startsWith("'") && val.endsWith("'")) || (val.startsWith('"') && val.endsWith('"'))) return val
  // 字符串加引号
  return `'${val.replace(/'/g, "''")}'`
}

/**
 * 创建空的查询配置
 */
export function createEmptyConfig(): QueryConfig {
  return {
    tables: [],
    joins: [],
    selectedFields: [],
    conditions: [],
    groupBy: [],
    having: [],
    orderBy: []
  }
}

/**
 * 验证聚合查询配置
 * 确保非聚合字段都在 GROUP BY 中
 */
export function validateAggregationConfig(config: QueryConfig): { valid: boolean; errors: string[] } {
  const errors: string[] = []
  
  // 检查是否有聚合字段
  const hasAggregates = config.selectedFields.some(f => f.aggregate)
  
  if (hasAggregates) {
    // 获取所有非聚合字段
    const nonAggregateFields = config.selectedFields
      .filter(f => !f.aggregate && f.fieldName !== '*')
      .map(f => `${f.tableAlias}.${f.fieldName}`)
    
    // 获取 GROUP BY 字段
    const groupByFields = new Set(config.groupBy.map(g => g.field))
    
    // 检查非聚合字段是否都在 GROUP BY 中
    for (const field of nonAggregateFields) {
      if (!groupByFields.has(field)) {
        errors.push(`字段 "${field}" 未在 GROUP BY 中，但也未使用聚合函数`)
      }
    }
  }
  
  // 检查 HAVING 条件是否有效
  if (config.having.length > 0 && config.groupBy.length === 0) {
    errors.push('HAVING 子句需要配合 GROUP BY 使用')
  }
  
  return {
    valid: errors.length === 0,
    errors
  }
}

/**
 * 自动补全 GROUP BY 字段
 * 将所有非聚合的 SELECT 字段添加到 GROUP BY
 */
export function autoCompleteGroupBy(config: QueryConfig): GroupByItem[] {
  const hasAggregates = config.selectedFields.some(f => f.aggregate)
  
  if (!hasAggregates) {
    return []
  }
  
  // 获取所有非聚合字段
  const nonAggregateFields = config.selectedFields
    .filter(f => !f.aggregate && f.fieldName !== '*')
    .map(f => `${f.tableAlias}.${f.fieldName}`)
  
  // 获取现有 GROUP BY 字段
  const existingGroupBy = new Set(config.groupBy.map(g => g.field))
  
  // 添加缺失的字段
  const newGroupBy = [...config.groupBy]
  for (const field of nonAggregateFields) {
    if (!existingGroupBy.has(field)) {
      newGroupBy.push({ field })
    }
  }
  
  return newGroupBy
}

// Re-export SQL formatting utilities for QueryBuilderService interface
export { formatSQL, compressSQL, validateSQLSyntax } from './sqlFormatter'

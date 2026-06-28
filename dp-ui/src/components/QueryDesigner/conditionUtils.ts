/**
 * 条件构建器工具函数
 */

import type { 
  ConditionItemType, 
  ConditionGroupType, 
  ComparisonOperator,
  LogicOperator 
} from './conditionTypes'
import { getOperatorConfig } from './conditionTypes'

/**
 * 生成唯一 ID
 */
export function generateId(prefix: string = 'cond'): string {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

/**
 * 创建空条件
 */
export function createEmptyCondition(): ConditionItemType {
  return {
    id: generateId('cond'),
    type: 'condition',
    field: '',
    operator: '=' as ComparisonOperator,
    value: '',
    logic: 'AND' as LogicOperator
  }
}

/**
 * 创建空条件组
 */
export function createEmptyGroup(): ConditionGroupType {
  return {
    id: generateId('group'),
    type: 'group',
    logic: 'AND' as LogicOperator,
    children: [createEmptyCondition()]
  }
}

/**
 * 格式化值（用于 SQL 生成）
 */
export function formatValue(val: string): string {
  if (!val) return "''"
  // 如果是数字，不加引号
  if (/^-?\d+(\.\d+)?$/.test(val)) return val
  // 如果已经有引号，直接返回
  if ((val.startsWith("'") && val.endsWith("'")) || (val.startsWith('"') && val.endsWith('"'))) return val
  // 字符串加引号，转义单引号
  return `'${val.replace(/'/g, "''")}'`
}

/**
 * 格式化值列表（用于 IN 操作符）
 */
export function formatValueList(values: string[]): string {
  if (!values || values.length === 0) return "''"
  return values.map(v => formatValue(v)).join(', ')
}

/**
 * 生成单个条件的 SQL
 */
export function generateConditionSQL(condition: ConditionItemType): string {
  const { field, operator, value, value2, valueList } = condition
  
  if (!field) return ''
  
  switch (operator) {
    case 'IS NULL':
      return `${field} IS NULL`
    case 'IS NOT NULL':
      return `${field} IS NOT NULL`
    case 'IN':
      return `${field} IN (${formatValueList(valueList || value.split(',').map(v => v.trim()))})`
    case 'NOT IN':
      return `${field} NOT IN (${formatValueList(valueList || value.split(',').map(v => v.trim()))})`
    case 'BETWEEN':
      return `${field} BETWEEN ${formatValue(value)} AND ${formatValue(value2 || '')}`
    case 'NOT BETWEEN':
      return `${field} NOT BETWEEN ${formatValue(value)} AND ${formatValue(value2 || '')}`
    case 'LIKE':
    case 'NOT LIKE':
      return `${field} ${operator} ${formatValue(value)}`
    default:
      return `${field} ${operator} ${formatValue(value)}`
  }
}

/**
 * 生成条件组的 WHERE 子句
 */
export function generateWhereClause(group: ConditionGroupType): string {
  const parts: string[] = []
  
  for (let i = 0; i < group.children.length; i++) {
    const child = group.children[i]
    let sql = ''
    
    if ('type' in child && child.type === 'group') {
      // 递归处理嵌套组
      const groupSQL = generateWhereClause(child as ConditionGroupType)
      if (groupSQL) {
        sql = `(${groupSQL})`
      }
    } else {
      // 处理单个条件
      sql = generateConditionSQL(child as ConditionItemType)
    }
    
    if (sql) {
      if (parts.length > 0) {
        // 使用前一个条件的 logic 或组的默认 logic
        const prevChild = group.children[i - 1]
        const logic = ('logic' in prevChild) ? prevChild.logic : group.logic
        parts.push(logic)
      }
      parts.push(sql)
    }
  }
  
  return parts.join(' ')
}

/**
 * 验证条件是否完整
 */
export function isConditionComplete(condition: ConditionItemType): boolean {
  if (!condition.field) return false
  
  const config = getOperatorConfig(condition.operator)
  if (!config) return false
  
  if (config.needsValue && !condition.value) return false
  if (config.needsSecondValue && !condition.value2) return false
  if (config.needsValueList && (!condition.valueList || condition.valueList.length === 0)) {
    // 检查是否有逗号分隔的值
    if (!condition.value) return false
  }
  
  return true
}

/**
 * 验证条件组是否有效
 */
export function isGroupValid(group: ConditionGroupType): boolean {
  if (group.children.length === 0) return false
  
  return group.children.every(child => {
    if ('type' in child && child.type === 'group') {
      return isGroupValid(child as ConditionGroupType)
    }
    return isConditionComplete(child as ConditionItemType)
  })
}

/**
 * 深拷贝条件或条件组
 */
export function deepClone<T extends ConditionItemType | ConditionGroupType>(item: T): T {
  return JSON.parse(JSON.stringify(item))
}

/**
 * 查找并移除条件或条件组
 */
export function removeFromGroup(
  group: ConditionGroupType, 
  targetId: string
): boolean {
  const index = group.children.findIndex(child => child.id === targetId)
  if (index !== -1) {
    group.children.splice(index, 1)
    return true
  }
  
  // 递归查找
  for (const child of group.children) {
    if ('type' in child && child.type === 'group') {
      if (removeFromGroup(child as ConditionGroupType, targetId)) {
        return true
      }
    }
  }
  
  return false
}

/**
 * 统计条件数量
 */
export function countConditions(group: ConditionGroupType): number {
  let count = 0
  
  for (const child of group.children) {
    if ('type' in child && child.type === 'group') {
      count += countConditions(child as ConditionGroupType)
    } else {
      count++
    }
  }
  
  return count
}

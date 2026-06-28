/**
 * 可视化条件构建器类型定义
 */

// 比较运算符
export type ComparisonOperator = 
  | '=' 
  | '!=' 
  | '>' 
  | '<' 
  | '>=' 
  | '<=' 
  | 'LIKE' 
  | 'NOT LIKE'
  | 'IN' 
  | 'NOT IN'
  | 'BETWEEN' 
  | 'NOT BETWEEN'
  | 'IS NULL' 
  | 'IS NOT NULL'

// 逻辑运算符
export type LogicOperator = 'AND' | 'OR'

// 单个条件项
export interface ConditionItemType {
  id: string
  type?: 'condition'
  field: string           // 格式: tableAlias.fieldName
  operator: ComparisonOperator
  value: string
  value2?: string         // BETWEEN 第二个值
  valueList?: string[]    // IN 操作符的值列表
  logic: LogicOperator    // 与下一个条件的逻辑关系
  // 用于标记条件组边界（兼容扁平结构）
  groupStart?: boolean
  groupEnd?: boolean
  groupLogic?: LogicOperator
}

// 条件组（支持嵌套）
export interface ConditionGroupType {
  id: string
  type: 'group'
  logic: LogicOperator    // 组内条件的逻辑关系
  children: (ConditionItemType | ConditionGroupType)[]
}

// 运算符配置
export interface OperatorConfig {
  value: ComparisonOperator
  label: string
  needsValue: boolean
  needsSecondValue: boolean
  needsValueList: boolean
  placeholder?: string
  placeholder2?: string
}

// 运算符列表
export const OPERATORS: OperatorConfig[] = [
  { value: '=', label: '等于 (=)', needsValue: true, needsSecondValue: false, needsValueList: false, placeholder: '输入值' },
  { value: '!=', label: '不等于 (!=)', needsValue: true, needsSecondValue: false, needsValueList: false, placeholder: '输入值' },
  { value: '>', label: '大于 (>)', needsValue: true, needsSecondValue: false, needsValueList: false, placeholder: '输入值' },
  { value: '<', label: '小于 (<)', needsValue: true, needsSecondValue: false, needsValueList: false, placeholder: '输入值' },
  { value: '>=', label: '大于等于 (>=)', needsValue: true, needsSecondValue: false, needsValueList: false, placeholder: '输入值' },
  { value: '<=', label: '小于等于 (<=)', needsValue: true, needsSecondValue: false, needsValueList: false, placeholder: '输入值' },
  { value: 'LIKE', label: '包含 (LIKE)', needsValue: true, needsSecondValue: false, needsValueList: false, placeholder: '输入模式，如 %关键词%' },
  { value: 'NOT LIKE', label: '不包含 (NOT LIKE)', needsValue: true, needsSecondValue: false, needsValueList: false, placeholder: '输入模式，如 %关键词%' },
  { value: 'IN', label: '在列表中 (IN)', needsValue: false, needsSecondValue: false, needsValueList: true, placeholder: '输入值，回车添加' },
  { value: 'NOT IN', label: '不在列表中 (NOT IN)', needsValue: false, needsSecondValue: false, needsValueList: true, placeholder: '输入值，回车添加' },
  { value: 'BETWEEN', label: '在范围内 (BETWEEN)', needsValue: true, needsSecondValue: true, needsValueList: false, placeholder: '起始值', placeholder2: '结束值' },
  { value: 'NOT BETWEEN', label: '不在范围内 (NOT BETWEEN)', needsValue: true, needsSecondValue: true, needsValueList: false, placeholder: '起始值', placeholder2: '结束值' },
  { value: 'IS NULL', label: '为空 (IS NULL)', needsValue: false, needsSecondValue: false, needsValueList: false },
  { value: 'IS NOT NULL', label: '不为空 (IS NOT NULL)', needsValue: false, needsSecondValue: false, needsValueList: false }
]

// 获取运算符配置
export function getOperatorConfig(operator: ComparisonOperator): OperatorConfig | undefined {
  return OPERATORS.find(op => op.value === operator)
}

// 运算符选项（用于下拉选择）
export const OPERATOR_OPTIONS = OPERATORS.map(op => ({
  label: op.label,
  value: op.value
}))

// 逻辑运算符选项
export const LOGIC_OPTIONS = [
  { label: 'AND', value: 'AND' as LogicOperator },
  { label: 'OR', value: 'OR' as LogicOperator }
]

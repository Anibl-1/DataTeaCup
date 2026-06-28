/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 可视化查询构建器相关类型定义
 */

/**
 * 表引用
 */
export interface TableRef {
  /** 表名 */
  name: string
  /** 别名 */
  alias?: string
}

/**
 * JOIN 配置
 */
export interface JoinConfig {
  /** JOIN 类型 */
  type: 'INNER' | 'LEFT' | 'RIGHT'
  /** 左表名 */
  leftTable: string
  /** 左表字段 */
  leftField: string
  /** 右表名 */
  rightTable: string
  /** 右表字段 */
  rightField: string
}

/**
 * SELECT 字段
 */
export interface SelectField {
  /** 表名 */
  table: string
  /** 字段名 */
  field: string
  /** 别名 */
  alias?: string
  /** 聚合函数 */
  aggregate?: 'COUNT' | 'SUM' | 'AVG' | 'MAX' | 'MIN'
}

/**
 * WHERE 条件
 */
export interface WhereCondition {
  /** 字段名 */
  field: string
  /** 操作符 */
  operator: '=' | '!=' | '>' | '>=' | '<' | '<=' | 'LIKE' | 'IN' | 'BETWEEN'
  /** 值 */
  value: any
  /** 逻辑运算符 */
  logic?: 'AND' | 'OR'
}

/**
 * ORDER BY 字段
 */
export interface OrderByField {
  /** 字段名 */
  field: string
  /** 排序方向 */
  direction: 'ASC' | 'DESC'
}

/**
 * 查询模型
 */
export interface QueryModel {
  /** 表列表 */
  tables: TableRef[]
  /** JOIN 配置列表 */
  joins: JoinConfig[]
  /** SELECT 字段列表 */
  selectFields: SelectField[]
  /** WHERE 条件列表 */
  conditions: WhereCondition[]
  /** GROUP BY 字段列表 */
  groupBy: string[]
  /** ORDER BY 字段列表 */
  orderBy: OrderByField[]
  /** 限制返回行数 */
  limit?: number
}

/**
 * 表元数据
 */
export interface TableMeta {
  /** 表名 */
  tableName: string
  /** 列元数据列表 */
  columns: ColumnMeta[]
}

/**
 * 列元数据
 */
export interface ColumnMeta {
  /** 列名 */
  name: string
  /** 数据类型 */
  type: string
  /** 注释 */
  comment?: string
  /** 是否可空 */
  nullable: boolean
  /** 是否主键 */
  primaryKey: boolean
}

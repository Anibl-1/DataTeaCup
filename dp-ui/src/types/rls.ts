/**
 * 行级权限（RLS）相关类型定义
 */

/**
 * RLS 规则
 */
export interface RlsRule {
  /** 规则ID */
  id?: number
  /** 角色ID */
  roleId: number
  /** 数据源ID */
  dataSourceId: number
  /** 表名 */
  tableName: string
  /** 过滤字段 */
  filterField: string
  /** 过滤操作符 */
  filterOperator: '=' | '!=' | '>' | '<' | 'IN' | 'LIKE'
  /** 过滤值（支持变量如 ${user.deptId}） */
  filterValue: string
  /** 是否启用 */
  enabled?: boolean
  /** 创建时间 */
  createTime?: string
}

/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 数据质量监控相关类型定义
 */

/**
 * 数据质量规则
 */
export interface DataQualityRule {
  /** 规则ID */
  id?: number
  /** 规则名称 */
  ruleName?: string
  /** 规则编码 */
  ruleCode?: string
  /** 规则类型：completeness/accuracy/consistency/timeliness/uniqueness */
  ruleType?: string
  /** 数据源ID */
  dataSourceId: number
  /** 表名 */
  tableName: string
  /** 字段名 */
  columnName?: string
  /** 检查SQL */
  checkSql?: string
  /** 阈值（0-100） */
  threshold?: number
  /** 严重级别：low/medium/high */
  severity?: string
  /** 描述 */
  description?: string
  /** 状态：1-启用，0-禁用 */
  status?: number
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/**
 * 字段质量详情
 */
export interface FieldQualityDetail {
  /** 字段名 */
  fieldName: string
  /** 空值率 */
  nullRate: number
  /** 重复率 */
  duplicateRate: number
  /** 类型一致性 */
  typeConsistency: number
  /** 样本值 */
  sampleValues?: any[]
}

/**
 * 数据质量报告
 */
export interface QualityReport {
  /** 报告ID */
  id?: number
  /** 规则ID */
  ruleId?: number
  /** 数据源ID */
  dataSourceId: number
  /** 表名 */
  tableName: string
  /** 质量评分（0-100） */
  score: number
  /** 详情JSON */
  detailJson?: string
  /** 字段质量详情列表 */
  details?: FieldQualityDetail[]
  /** 创建时间 */
  createTime?: string
}

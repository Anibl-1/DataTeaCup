/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 数据血缘可视化相关类型定义
 */

/**
 * 血缘节点
 */
export interface LineageNode {
  /** 节点ID */
  id: string
  /** 节点名称 */
  name: string
  /** 节点类型 */
  type: 'datasource' | 'etl' | 'table'
  /** 元数据 */
  metadata: Record<string, any>
}

/**
 * 血缘边
 */
export interface LineageEdge {
  /** 源节点ID */
  source: string
  /** 目标节点ID */
  target: string
  /** 转换类型 */
  transformType?: string
}

/**
 * 血缘图
 */
export interface LineageGraph {
  /** 节点列表 */
  nodes: LineageNode[]
  /** 边列表 */
  edges: LineageEdge[]
}

/**
 * 血缘元数据
 */
export interface LineageMetadata {
  /** 元数据ID */
  id?: number
  /** 源数据源ID */
  sourceDsId: number
  /** 源表名 */
  sourceTable: string
  /** 目标数据源ID */
  targetDsId: number
  /** 目标表名 */
  targetTable: string
  /** 转换类型（etl/collect/sql） */
  transformType?: string
  /** 关联的任务ID */
  transformId?: number
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

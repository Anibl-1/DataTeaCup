/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 数据血缘API
 */
import request from './request'

export interface DataLineage {
  id?: number
  sourceType?: string
  sourceId?: number
  sourceName?: string
  sourceDatabase?: string
  sourceTable: string
  sourceColumn?: string
  targetType?: string
  targetId?: number
  targetName?: string
  targetDatabase?: string
  targetTable: string
  targetColumn?: string
  lineageType: string
  transformLogic?: string
  sqlContent?: string
  createBy?: number
  createTime?: string
  updateTime?: string
}

export const dataLineageApi = {
  /** 获取所有血缘关系 */
  getAll() {
    return request.get<DataLineage[]>('/data-lineage/list')
  },

  /** 根据表名查询血缘 */
  getByTable(tableName: string) {
    return request.get<DataLineage[]>(`/data-lineage/table/${tableName}`)
  },

  /** 获取血缘图谱 */
  getGraph(tableName: string, depth: number = 3) {
    return request.get<any>(`/data-lineage/graph/${tableName}`, { params: { depth } })
  },

  /** 获取上游血缘 */
  getUpstream(tableName: string) {
    return request.get<DataLineage[]>(`/data-lineage/upstream/${tableName}`)
  },

  /** 获取下游血缘 */
  getDownstream(tableName: string) {
    return request.get<DataLineage[]>(`/data-lineage/downstream/${tableName}`)
  },

  /** 创建血缘关系 */
  create(data: Partial<DataLineage>) {
    return request.post<DataLineage>('/data-lineage', data)
  },

  /** 更新血缘关系 */
  update(id: number, data: Partial<DataLineage>) {
    return request.put<DataLineage>(`/data-lineage/${id}`, data)
  },

  /** 删除血缘关系 */
  delete(id: number) {
    return request.delete(`/data-lineage/${id}`)
  },

  /** 从SQL解析血缘 */
  parseSql(data: { sql: string; targetTable: string; targetDatabase?: string }) {
    return request.post<DataLineage[]>('/data-lineage/parse-sql', data)
  },

  /** 批量保存血缘 */
  batchSave(lineages: DataLineage[]) {
    return request.post('/data-lineage/batch-save', lineages)
  },

  /** 获取统计信息 */
  getStatistics() {
    return request.get<any>('/data-lineage/statistics')
  },

  /** 自动发现血缘 */
  autoDiscover() {
    return request.post<any>('/data-lineage/discover')
  },

  /** 影响分析 */
  getImpactAnalysis(tableName: string, depth: number = 5) {
    return request.get<any>(`/data-lineage/impact/${tableName}`, { params: { depth } })
  },

  // ==================== 智能分析接口 ====================
  
  /** 全链路追溯 */
  getFullChain(tableName: string, depth: number = 5) {
    return request.get<any>(`/data-lineage/full-chain/${tableName}`, { params: { depth } })
  },

  /** 热点表分析 */
  getHotspotAnalysis(topN: number = 10) {
    return request.get<any>('/data-lineage/hotspot', { params: { topN } })
  },

  /** 孤岛检测 */
  getOrphanAnalysis() {
    return request.get<any>('/data-lineage/orphan')
  },

  /** 血缘健康报告 */
  getHealthReport() {
    return request.get<any>('/data-lineage/health')
  },

  /** 智能SQL解析并创建血缘 */
  smartParseSql(data: { sql: string; targetTable: string; targetDatabase?: string }) {
    return request.post<any>('/data-lineage/smart-parse', data)
  },

  /** 获取表依赖摘要 */
  getTableSummary(tableName: string) {
    return request.get<any>(`/data-lineage/summary/${tableName}`)
  }
}

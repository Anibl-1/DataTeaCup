import { z } from 'zod'
import request from './request'
import type { ApiResponse, PageResult, ApiVoidResponse } from '@/types/api'
import type {
  DataSource,
  DataSourceForm,
  DataSourceQueryParams,
  DataSourceCreateParams,
  DataSourceUpdateParams,
  DataSourceTable,
  TableColumn,
  BatchTestResult,
  DataSourceDetail,
} from '@/types/dataSource'

// ==================== Zod 运行时验证 Schema（仅开发环境） ====================

const isDev = import.meta.env.DEV

/** 数据源基础字段 schema */
const dataSourceSchema = z.object({
  id: z.number(),
  name: z.string(),
  dbType: z.string(),
  host: z.string(),
  port: z.number(),
  database: z.string(),
  username: z.string(),
  password: z.string(),
  groupName: z.string().optional(),
  createTime: z.string().optional(),
  updateTime: z.string().optional(),
})

/** 分页响应 schema */
const pageResultSchema = <T extends z.ZodTypeAny>(itemSchema: T) =>
  z.object({
    list: z.array(itemSchema),
    total: z.number(),
    page: z.number().optional(),
    pageSize: z.number().optional(),
  })

/** 统一 API 响应 schema */
const apiResponseSchema = <T extends z.ZodTypeAny>(dataSchema: T) =>
  z.object({
    code: z.number(),
    msg: z.string(),
    data: dataSchema,
    timestamp: z.number().optional(),
  })

/**
 * 开发环境运行时验证辅助函数
 * 验证失败时仅记录警告日志，不阻断业务流程
 */
function devValidate<T>(schema: z.ZodSchema<T>, data: unknown, label: string): void {
  if (!isDev) return
  const result = schema.safeParse(data)
  if (!result.success) {
    console.warn(
      `[API Validation] ${label} 响应结构不匹配:`,
      result.error.issues.map((i) => `${i.path.join('.')}: ${i.message}`)
    )
  }
}

// ==================== API 函数 ====================

/**
 * 获取数据源列表（分页）
 */
export const getDataSourceList = (
  params: DataSourceQueryParams
): Promise<ApiResponse<PageResult<DataSource>>> => {
  return request
    .get<ApiResponse<PageResult<DataSource>>>('/data-source/list', { params })
    .then((res) => {
      devValidate(
        apiResponseSchema(pageResultSchema(dataSourceSchema)),
        res,
        'getDataSourceList'
      )
      return res
    })
}

/**
 * 创建数据源
 */
export const createDataSource = (
  data: DataSourceCreateParams | DataSourceForm
): Promise<ApiVoidResponse> => {
  return request.post<ApiVoidResponse>('/data-source/create', data)
}

/**
 * 更新数据源
 */
export const updateDataSource = (
  data: DataSourceUpdateParams | DataSourceForm
): Promise<ApiVoidResponse> => {
  return request.post<ApiVoidResponse>('/data-source/update', data)
}

/**
 * 删除数据源
 */
export const deleteDataSource = (id: number): Promise<ApiVoidResponse> => {
  return request.delete<ApiVoidResponse>(`/data-source/${id}`)
}

/**
 * 测试数据源连接
 */
export const testConnection = (
  data: DataSourceCreateParams | DataSourceForm
): Promise<ApiVoidResponse> => {
  return request.post<ApiVoidResponse>('/data-source/test', data)
}

/**
 * 获取数据源的表列表
 */
export const getDataSourceTables = (
  id: number
): Promise<ApiResponse<DataSourceTable[]>> => {
  return request.get<ApiResponse<DataSourceTable[]>>(
    `/data-source/${id}/tables`
  )
}

/**
 * 获取数据源分组列表
 */
export const getDataSourceGroups = (): Promise<ApiResponse<string[]>> => {
  return request.get<ApiResponse<string[]>>('/data-source/groups')
}

/**
 * 获取表的字段列表
 */
export const getTableColumns = (
  dataSourceId: number,
  tableName: string
): Promise<ApiResponse<TableColumn[]>> => {
  return request.get<ApiResponse<TableColumn[]>>(
    `/data-source/${dataSourceId}/tables/${tableName}/columns`
  )
}

/**
 * 批量测试数据源连接
 */
export const batchTestConnection = (
  ids: number[]
): Promise<ApiResponse<BatchTestResult[]>> => {
  return request.post<ApiResponse<BatchTestResult[]>>(
    '/data-source/batch-test',
    { ids }
  )
}

/**
 * 获取数据源详情（含表列表）
 */
export const getDataSourceDetail = (
  id: number
): Promise<ApiResponse<DataSourceDetail>> => {
  return request.get<ApiResponse<DataSourceDetail>>(
    `/data-source/${id}/detail`
  )
}

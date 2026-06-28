import request from './request'

export interface PipelineListParams {
  keyword?: string
  status?: number
  page?: number
  pageSize?: number
}

export interface PipelineFormData {
  pipelineName: string
  pipelineCode?: string
  description?: string
  status?: number
  cronExpression?: string
  [key: string]: unknown
}

export interface PipelineDesignData {
  nodes?: unknown[]
  edges?: unknown[]
  [key: string]: unknown
}

export interface ExecutionListParams {
  pipelineId?: number
  status?: number
  page?: number
  pageSize?: number
}

// 流程管理
export const getPipelines = (params?: PipelineListParams) => {
  return request.get('/pipeline/list', { params })
}

// 别名，用于子流程选择
export const getPipelineList = getPipelines

export const getPipeline = (id: number) => {
  return request.get(`/pipeline/${id}`)
}

export const createPipeline = (data: PipelineFormData) => {
  return request.post('/pipeline', data)
}

export const updatePipeline = (id: number, data: PipelineFormData) => {
  return request.put(`/pipeline/${id}`, data)
}

export const deletePipeline = (id: number) => {
  return request.delete(`/pipeline/${id}`)
}

export const updatePipelineStatus = (id: number, status: number) => {
  return request.put(`/pipeline/${id}/status`, { status })
}

export const copyPipeline = (id: number) => {
  return request.post(`/pipeline/${id}/copy`)
}

// 流程设计
export const getPipelineDesign = (id: number) => {
  return request.get(`/pipeline/${id}/design`)
}

export const savePipelineDesign = (id: number, data: PipelineDesignData) => {
  return request.post(`/pipeline/${id}/design`, data)
}

// 流程执行
export const executePipeline = (id: number) => {
  return request.post(`/pipeline/${id}/execute`)
}

export const stopExecution = (id: number) => {
  return request.post(`/pipeline/execution/${id}/stop`)
}

// 执行日志
export const getExecutions = (params?: ExecutionListParams) => {
  return request.get('/pipeline/executions', { params })
}

export const getExecution = (id: number) => {
  return request.get(`/pipeline/execution/${id}`)
}

export const getRunningExecutions = () => {
  return request.get('/pipeline/executions/running')
}

// 统计
export const getPipelineStatistics = () => {
  return request.get('/pipeline/statistics')
}

export const getExecutionTrend = (days = 7) => {
  return request.get('/pipeline/statistics/trend', { params: { days } })
}

// 节点类型
export const getNodeTypes = () => {
  return request.get('/pipeline/node-types')
}

// 通知测试发送
export const testNotify = (channel: string, config: Record<string, unknown>) => {
  return request.post('/pipeline/notify/test', { channel, config })
}

import request from '@/api/request'

export interface SystemConfig {
  id?: number
  configKey: string
  configValue: string
  configType?: string
  configDesc?: string
  configGroup?: string
  isSystem?: boolean
  createTime?: string
  updateTime?: string
}

/** 分页查询 */
export const getSystemConfigList = (params?: { page?: number; pageSize?: number; keyword?: string }) => {
  return request.get('/system-config/list', { params })
}

/** 获取全部配置 */
export const getAllSystemConfigs = (keyword?: string) => {
  return request.get('/system-config/all', { params: { keyword } })
}

/** 按Key获取值 */
export const getConfigByKey = (configKey: string) => {
  return request.get(`/system-config/key/${configKey}`)
}

/** 获取详情 */
export const getSystemConfigById = (id: number) => {
  return request.get(`/system-config/${id}`)
}

/** 创建 */
export const createSystemConfig = (data: SystemConfig) => {
  return request.post('/system-config', data)
}

/** 更新 */
export const updateSystemConfig = (id: number, data: SystemConfig) => {
  return request.put(`/system-config/${id}`, data)
}

/** 删除 */
export const deleteSystemConfig = (id: number) => {
  return request.delete(`/system-config/${id}`)
}

/** 获取所有配置分组 */
export const getConfigGroups = () => {
  return request.get('/system-config/groups')
}

/** 按分组查询配置列表 */
export const getConfigByGroup = (group?: string) => {
  return request.get('/system-config/by-group', { params: { group } })
}

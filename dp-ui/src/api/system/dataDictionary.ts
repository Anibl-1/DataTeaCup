import request from '@/api/request'

export interface DataDictionary {
  id?: number
  dictType: string
  dictCode: string
  dictLabel: string
  dictValue: string
  sortOrder?: number
  isDefault?: boolean
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 分页查询 */
export const getDataDictionaryList = (params?: {
  page?: number; pageSize?: number; dictType?: string; keyword?: string; status?: number
}) => {
  return request.get('/data-dictionary/list', { params })
}

/** 按字典类型查询（下拉用） */
export const getDictByType = (dictType: string) => {
  return request.get(`/data-dictionary/type/${dictType}`)
}

/** 获取所有字典类型 */
export const getDictTypes = () => {
  return request.get('/data-dictionary/types')
}

/** 获取详情 */
export const getDataDictionaryById = (id: number) => {
  return request.get(`/data-dictionary/${id}`)
}

/** 创建 */
export const createDataDictionary = (data: DataDictionary) => {
  return request.post('/data-dictionary', data)
}

/** 更新 */
export const updateDataDictionary = (id: number, data: DataDictionary) => {
  return request.put(`/data-dictionary/${id}`, data)
}

/** 删除 */
export const deleteDataDictionary = (id: number) => {
  return request.delete(`/data-dictionary/${id}`)
}

/** 批量删除 */
export const batchDeleteDataDictionary = (ids: number[]) => {
  return request.delete('/data-dictionary/batch', { data: ids })
}

/** 批量获取字典映射（报表字段值翻译用） */
export const getDictMappings = (types: string) => {
  return request.get('/data-dictionary/mappings', { params: { types } })
}

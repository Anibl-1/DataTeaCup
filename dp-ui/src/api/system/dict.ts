import request from '@/api/request'

// ==================== 类型定义 ====================

/** 字典类型 */
export interface DictType {
  id?: number
  dictCode: string
  dictName: string
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 字典数据项 */
export interface DictData {
  id?: number
  dictCode: string
  label: string
  value: string
  sortOrder?: number
  cssClass?: string
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

// ==================== 字典类型接口 ====================

/** 查询字典类型列表 */
export const listDictTypes = () => {
  return request.get('/dict/types')
}

/** 创建字典类型 */
export const createDictType = (data: DictType) => {
  return request.post('/dict/types', data)
}

/** 更新字典类型 */
export const updateDictType = (id: number, data: DictType) => {
  return request.put(`/dict/types/${id}`, data)
}

/** 删除字典类型（级联删除数据项） */
export const deleteDictType = (id: number) => {
  return request.delete(`/dict/types/${id}`)
}

// ==================== 字典数据接口 ====================

/** 按字典类型编码查询数据项（启用状态，按排序号升序） */
export const listDictData = (dictCode: string) => {
  return request.get('/dict/data', { params: { dictCode } })
}

/** 批量查询多个字典类型的数据 */
export const listDictDataBatch = (codes: string[]) => {
  return request.get('/dict/data/batch', { params: { codes: codes.join(',') } })
}

/** 创建字典数据项 */
export const createDictData = (data: DictData) => {
  return request.post('/dict/data', data)
}

/** 更新字典数据项 */
export const updateDictData = (id: number, data: DictData) => {
  return request.put(`/dict/data/${id}`, data)
}

/** 删除字典数据项 */
export const deleteDictData = (id: number) => {
  return request.delete(`/dict/data/${id}`)
}

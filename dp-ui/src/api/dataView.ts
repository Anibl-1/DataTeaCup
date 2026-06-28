import request from '@/api/request'

export interface ColumnConfig {
  columnName: string
  displayName: string
  dataType: string
  visible: boolean
  isPrimaryKey: boolean
  sortOrder: number
  /** 关联的数据字典类型（如 gender, status 等），设置后编辑时显示下拉框 */
  dictType?: string
}

export interface DataView {
  id?: number
  name: string
  code: string
  dataSourceId: number
  tableName: string
  primaryKey?: string
  description?: string
  status: number
  columns: ColumnConfig[]
  allowQuery: number
  allowInsert: number
  allowUpdate: number
  allowDelete: number
  allowImport: number
  allowExport: number
  defaultOrderBy?: string
  defaultOrderDir: string
  pageSize: number
  generateMenu: number
  menuName?: string
  menuParentId?: number
  menuIcon?: string
  menuSort?: number
  menuId?: number
}

// 获取数据管理列表
export const getDataViewList = (params?: { keyword?: string }) => {
  return request.get('/dataview/list', { params })
}

// 获取数据管理详情
export const getDataViewById = (id: number) => {
  return request.get(`/dataview/${id}`)
}

// 根据编码获取数据管理
export const getDataViewByCode = (code: string) => {
  return request.get(`/dataview/code/${code}`)
}

// 创建数据管理
export const createDataView = (data: DataView) => {
  return request.post('/dataview', data)
}

// 更新数据管理
export const updateDataView = (id: number, data: DataView) => {
  return request.put(`/dataview/${id}`, data)
}

// 删除数据管理
export const deleteDataView = (id: number) => {
  return request.delete(`/dataview/${id}`)
}

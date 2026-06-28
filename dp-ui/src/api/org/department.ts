import request from '@/api/request'

export interface Department {
  id?: number
  deptName: string
  deptCode?: string
  parentId: number
  ancestors?: string
  leader?: string
  phone?: string
  email?: string
  sortOrder?: number
  status?: number
  createTime?: string
  updateTime?: string
  children?: Department[]
  parentName?: string
}

/** 获取部门列表（平铺） */
export const getDepartmentList = (params?: { keyword?: string; status?: number }) => {
  return request.get('/department/list', { params })
}

/** 获取部门树 */
export const getDepartmentTree = (params?: { keyword?: string; status?: number }) => {
  return request.get('/department/tree', { params })
}

/** 获取部门详情 */
export const getDepartmentById = (id: number) => {
  return request.get(`/department/${id}`)
}

/** 创建部门 */
export const createDepartment = (data: Department) => {
  return request.post('/department', data)
}

/** 更新部门 */
export const updateDepartment = (id: number, data: Department) => {
  return request.put(`/department/${id}`, data)
}

/** 删除部门 */
export const deleteDepartment = (id: number) => {
  return request.delete(`/department/${id}`)
}

/** 更新部门状态 */
export const updateDepartmentStatus = (id: number, status: number) => {
  return request.put(`/department/${id}/status`, { status })
}

/** 批量更新部门排序 */
export const batchUpdateDepartmentSort = (items: Array<{ id: number; sortOrder: number }>) => {
  return request.post('/department/batch-sort', items)
}

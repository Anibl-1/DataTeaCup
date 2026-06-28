import request from '@/api/request'

export interface ChartFolder {
  id?: number
  folderName: string
  parentId?: number
  sortOrder?: number
  createTime?: string
  children?: ChartFolder[]
}

/** 获取文件夹树 */
export const getChartFolderTree = () => {
  return request.get('/chart-folder/tree')
}

/** 获取全部列表 */
export const getChartFolderList = () => {
  return request.get('/chart-folder/list')
}

/** 创建文件夹 */
export const createChartFolder = (data: ChartFolder) => {
  return request.post('/chart-folder', data)
}

/** 更新文件夹 */
export const updateChartFolder = (id: number, data: ChartFolder) => {
  return request.put(`/chart-folder/${id}`, data)
}

/** 删除文件夹 */
export const deleteChartFolder = (id: number) => {
  return request.delete(`/chart-folder/${id}`)
}

import request from '@/api/request'

export interface Announcement {
  id?: number
  title: string
  content: string
  type: 'info' | 'success' | 'warning' | 'error'
  priority: number
  status: number
  isTop: number
  startTime?: string
  endTime?: string
  createBy?: string
  createTime?: string
  updateTime?: string
}

// 分页查询公告
export const getAnnouncementList = (params: {
  page: number
  pageSize: number
  keyword?: string
  status?: number
}) => {
  return request.get('/announcement/list', { params })
}

// 获取当前有效的公告
export const getActiveAnnouncements = () => {
  return request.get('/announcement/active')
}

// 获取公告详情
export const getAnnouncementById = (id: number) => {
  return request.get(`/announcement/${id}`)
}

// 创建公告
export const createAnnouncement = (data: Announcement) => {
  return request.post('/announcement', data)
}

// 更新公告
export const updateAnnouncement = (id: number, data: Announcement) => {
  return request.put(`/announcement/${id}`, data)
}

// 删除公告
export const deleteAnnouncement = (id: number) => {
  return request.delete(`/announcement/${id}`)
}

// 批量删除公告
export const batchDeleteAnnouncements = (ids: number[]) => {
  return request.delete('/announcement/batch', { data: ids })
}

// 获取公告统计数据
export const getAnnouncementStats = () => {
  return request.get('/announcement/stats')
}


// 更新公告状态
export const updateAnnouncementStatus = (id: number, status: number) => {
  return request.put(`/announcement/${id}/status`, null, { params: { status } })
}

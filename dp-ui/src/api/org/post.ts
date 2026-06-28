import request from '@/api/request'

// ==================== 类型定义 ====================

/** 岗位 */
export interface Post {
  id?: number
  postCode: string
  postName: string
  sortOrder?: number
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

// ==================== 岗位接口 ====================

/** 查询岗位列表（支持按名称和状态筛选，按 sortOrder 升序） */
export const listPosts = (params?: { postName?: string; status?: number | null }) => {
  return request.get('/posts', { params })
}

/** 查询岗位详情 */
export const getPostById = (id: number) => {
  return request.get(`/posts/${id}`)
}

/** 创建岗位 */
export const createPost = (data: Post) => {
  return request.post('/posts', data)
}

/** 更新岗位 */
export const updatePost = (id: number, data: Post) => {
  return request.put(`/posts/${id}`, data)
}

/** 删除岗位（检查关联用户） */
export const deletePost = (id: number) => {
  return request.delete(`/posts/${id}`)
}

/** 查询岗位下的用户列表 */
export const getPostUsers = (id: number) => {
  return request.get(`/posts/${id}/users`)
}

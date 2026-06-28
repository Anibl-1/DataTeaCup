import request from '../request'
import type { ApiResponse, PageResult, PageParams } from '@/types/api'
import type { LoginForm, LoginResponse, UserInfo, User, UserForm, CaptchaResult } from '@/types/user'
import type { Role } from '@/types/role'

/**
 * 获取验证码
 */
export const getCaptcha = () => {
  return request.get<ApiResponse<CaptchaResult>>('/auth/captcha')
}

/**
 * 用户登录
 */
export const login = (data: LoginForm) => {
  return request.post<ApiResponse<LoginResponse>>('/auth/login', data)
}

/**
 * 获取当前用户信息
 */
export const getUserInfo = () => {
  return request.get<ApiResponse<{ userInfo: UserInfo; permissions: string[] }>>('/auth/userInfo')
}

/**
 * 获取用户列表（分页）
 */
export const getUserList = (params: PageParams) => {
  return request.get<ApiResponse<PageResult<User>>>('/user/list', { params })
}

/**
 * 创建用户
 */
export const createUser = (data: UserForm) => {
  return request.post<ApiResponse<void>>('/user/create', data)
}

/**
 * 更新用户
 */
export const updateUser = (data: UserForm) => {
  return request.put<ApiResponse<void>>('/user/update', data)
}

/**
 * 删除用户
 */
export const deleteUser = (id: number) => {
  return request.delete<ApiResponse<void>>(`/user/delete/${id}`)
}

/**
 * 为用户分配角色
 */
export const assignUserRoles = (userId: number, roleIds: number[]) => {
  return request.post<ApiResponse<void>>(`/user/${userId}/assignRoles`, roleIds)
}

/**
 * 获取用户的所有角色
 */
export const getUserRoles = (userId: number) => {
  return request.get<ApiResponse<Role[]>>(`/user/${userId}/roles`)
}

/**
 * 修改密码（用户自己修改）
 */
export const changePassword = (data: { oldPassword: string; newPassword: string }) => {
  return request.post<ApiResponse<void>>('/auth/changePassword', data)
}

/**
 * 重置用户密码（管理员操作）
 */
export const resetPassword = (userId: number) => {
  return request.post<ApiResponse<void>>('/user/resetPassword', { userId })
}

/**
 * 批量删除用户
 */
export const batchDeleteUsers = (ids: number[]) => {
  return request.post<ApiResponse<{ success: number; failed: number }>>('/user/batch-delete', ids)
}

/**
 * 批量更新用户状态
 */
export const batchUpdateUserStatus = (ids: number[], status: number) => {
  return request.post<ApiResponse<void>>('/user/batch-status', { ids, status })
}

/**
 * 导出用户列表
 */
export const exportUsers = (filters?: string) => {
  return request.get<ApiResponse<User[]>>('/user/export', { params: { filters } })
}


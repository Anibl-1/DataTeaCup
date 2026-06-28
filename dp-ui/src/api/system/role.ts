import request from '../request'
import type { ApiResponse, PageResult, PageParams } from '@/types/api'
import type { Role, Permission } from '@/types/role'

/**
 * 获取角色列表（分页）
 */
export const getRoleList = (params: PageParams) => {
  return request.get<ApiResponse<PageResult<Role>>>('/role/list', { params })
}

/**
 * 根据ID获取角色详情
 */
export const getRoleById = (id: number) => {
  return request.get<ApiResponse<Role>>(`/role/${id}`)
}

/**
 * 创建角色
 */
export const createRole = (data: RoleForm) => {
  return request.post<ApiResponse<Role>>('/role/create', data)
}

/**
 * 更新角色
 */
export const updateRole = (data: RoleForm) => {
  return request.put<ApiResponse<Role>>('/role/update', data)
}

/**
 * 删除角色
 */
export const deleteRole = (id: number) => {
  return request.delete<ApiResponse<void>>(`/role/delete/${id}`)
}

/**
 * 为角色分配权限
 */
export const assignPermissions = (roleId: number, permissionIds: number[]) => {
  return request.post<ApiResponse<void>>('/role/assignPermissions', {
    roleId,
    permissionIds
  })
}

/**
 * 获取角色的权限列表
 */
export const getRolePermissions = (roleId: number) => {
  return request.get<ApiResponse<Permission[]>>(`/role/${roleId}/permissions`)
}

/**
 * 获取所有权限列表
 */
export const getAllPermissions = () => {
  return request.get<ApiResponse<Permission[]>>('/role/permissions')
}

/**
 * 获取用户的所有角色
 */
export const getUserRoles = (userId: number) => {
  return request.get<ApiResponse<Role[]>>(`/role/user/${userId}`)
}

/**
 * 为角色分配菜单
 */
export const assignMenus = (roleId: number, menuIds: number[]) => {
  return request.post<ApiResponse<void>>(`/role/${roleId}/assignMenus`, menuIds)
}

/**
 * 获取角色的菜单列表
 */
export const getRoleMenus = (roleId: number) => {
  return request.get<ApiResponse<number[]>>(`/role/${roleId}/menus`)
}

/**
 * 角色表单数据
 */
export interface RoleForm {
  id?: number
  roleName: string
  roleCode: string
  description?: string
}



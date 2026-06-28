import request from '../request'
import type { ApiResponse } from '@/types/api'
import type { Menu, MenuForm } from '@/types/menu'

/**
 * 获取所有菜单（树形结构）
 */
export const getAllMenus = (silent = false) => {
  return request.get<ApiResponse<Menu[]>>('/menu/all', silent ? { __silent: true } as any : undefined)
}

/**
 * 获取可见菜单（树形结构）
 */
export const getVisibleMenus = () => {
  return request.get<ApiResponse<Menu[]>>('/menu/visible', { __silent: true } as any)
}

/**
 * 根据ID获取菜单
 */
export const getMenuById = (id: number) => {
  return request.get<ApiResponse<Menu>>(`/menu/${id}`)
}

/**
 * 根据编码获取菜单
 */
export const getMenuByCode = (code: string) => {
  return request.get<ApiResponse<Menu>>(`/menu/code/${code}`)
}

/**
 * 创建菜单
 */
export const createMenu = (data: MenuForm) => {
  return request.post<ApiResponse<number>>('/menu/create', data)
}

/**
 * 更新菜单
 */
export const updateMenu = (data: MenuForm) => {
  return request.post<ApiResponse<void>>('/menu/update', data)
}

/**
 * 删除菜单
 */
export const deleteMenu = (id: number) => {
  return request.delete<ApiResponse<void>>(`/menu/${id}`)
}


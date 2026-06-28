/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 权限工具函数
 */

import { useUserStore } from '@/stores/user'

/**
 * 检查是否有指定权限
 * @param permission 权限码或权限码数组
 * @param requireAll 是否需要全部满足（默认false，满足任一即可）
 * @returns 是否有权限
 */
export function hasPermission(permission: string | string[], requireAll = false): boolean {
  const userStore = useUserStore()
  const userPermissions = userStore.permissions || []
  
  // admin用户拥有所有权限
  if (userStore.username === 'admin') {
    return true
  }
  
  // 如果没有传入权限要求，默认有权限
  if (!permission || (Array.isArray(permission) && permission.length === 0)) {
    return true
  }
  
  // 转换为数组
  const permissionArray = Array.isArray(permission) ? permission : [permission]
  
  // 检查权限
  if (requireAll) {
    // 需要全部满足
    return permissionArray.every(p => userPermissions.includes(p))
  } else {
    // 满足任一即可
    return permissionArray.some(p => userPermissions.includes(p))
  }
}

/**
 * 检查是否有指定角色
 * @param role 角色码或角色码数组
 * @param requireAll 是否需要全部满足（默认false，满足任一即可）
 * @returns 是否有角色
 */
export function hasRole(role: string | string[], requireAll = false): boolean {
  const userStore = useUserStore()
  const userRoles = userStore.roles || []
  
  // admin用户拥有所有角色
  if (userStore.username === 'admin') {
    return true
  }
  
  // 如果没有传入角色要求，默认有权限
  if (!role || (Array.isArray(role) && role.length === 0)) {
    return true
  }
  
  // 转换为数组
  const roleArray = Array.isArray(role) ? role : [role]
  
  // 检查角色
  if (requireAll) {
    // 需要全部满足
    return roleArray.every(r => userRoles.includes(r))
  } else {
    // 满足任一即可
    return roleArray.some(r => userRoles.includes(r))
  }
}

/**
 * 检查是否是管理员
 * @returns 是否是管理员
 */
export function isAdmin(): boolean {
  const userStore = useUserStore()
  return userStore.username === 'admin' || hasRole('admin')
}

/**
 * 检查菜单权限
 * @param menu 菜单对象
 * @returns 是否有权限访问该菜单
 */
export function hasMenuPermission(menu: any): boolean {
  // admin用户拥有所有权限
  if (isAdmin()) {
    return true
  }
  
  // 如果菜单没有定义权限要求，默认允许访问
  if (!menu.permissionCode) {
    return true
  }
  
  return hasPermission(menu.permissionCode)
}

/**
 * 过滤有权限的菜单
 * @param menus 菜单列表
 * @returns 过滤后的菜单列表
 */
export function filterMenusByPermission(menus: any[]): any[] {
  if (!menus || menus.length === 0) {
    return []
  }
  
  return menus
    .filter(menu => hasMenuPermission(menu))
    .map(menu => {
      if (menu.children && menu.children.length > 0) {
        return {
          ...menu,
          children: filterMenusByPermission(menu.children)
        }
      }
      return menu
    })
}

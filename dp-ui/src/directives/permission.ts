/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 权限指令
 * 用于控制按钮、菜单等元素的显示/隐藏
 * 
 * 使用方式：
 * v-permission="'system:user:add'"  // 单个权限
 * v-permission="['system:user:add', 'system:user:edit']"  // 多个权限（满足任一即可）
 * v-permission:all="['system:user:add', 'system:user:edit']"  // 多个权限（需全部满足）
 */

import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 检查用户是否拥有指定权限
 * @param permissions - 单个权限字符串或权限数组
 * @param requireAll - 是否要求全部满足（默认 false，即任一满足）
 * @returns 是否有权限
 */
export function hasPermission(permissions: string | string[], requireAll = false): boolean {
  const userStore = useUserStore()
  const userPermissions = userStore.permissions || []

  // 如果用户拥有通配符权限 '*'，视为管理员，拥有所有权限
  if (userPermissions.includes('*')) {
    return true
  }

  // 如果没有传入权限要求，默认有权限
  if (!permissions || (Array.isArray(permissions) && permissions.length === 0)) {
    return true
  }

  // 转换为数组统一处理
  const permissionArray = Array.isArray(permissions) ? permissions : [permissions]

  if (requireAll) {
    // 需要全部满足
    return permissionArray.every(p => userPermissions.includes(p))
  } else {
    // 满足任一即可
    return permissionArray.some(p => userPermissions.includes(p))
  }
}

/**
 * 权限指令
 * 
 * 在 mounted 和 updated 钩子中检查权限，
 * 当用户缺少权限时从 DOM 移除元素。
 * 
 * 支持三种模式：
 * - 单权限：v-permission="'perm'"
 * - 多权限（任一）：v-permission="['perm1', 'perm2']"
 * - 多权限（全部）：v-permission:all="['perm1', 'perm2']"
 */
export const permission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    checkAndRemove(el, binding)
  },

  updated(el: HTMLElement, binding: DirectiveBinding) {
    checkAndRemove(el, binding)
  }
}

/**
 * 检查权限并在无权限时移除元素
 */
function checkAndRemove(el: HTMLElement, binding: DirectiveBinding) {
  const { value, arg } = binding
  const requireAll = arg === 'all'

  if (!hasPermission(value, requireAll)) {
    // 没有权限，从 DOM 移除元素
    el.parentNode?.removeChild(el)
  }
}

/**
 * 权限检查函数（用于 JS/TS 代码中的编程式权限检查）
 */
export function checkPermission(permissions: string | string[], requireAll = false): boolean {
  return hasPermission(permissions, requireAll)
}

/**
 * 路由权限检查函数（用于路由守卫）
 */
export function checkRoutePermission(route: any): boolean {
  // 如果路由没有定义权限要求，默认允许访问
  if (!route.meta?.permission) {
    return true
  }

  const routePermission = route.meta.permission
  return hasPermission(routePermission, false)
}

export default permission

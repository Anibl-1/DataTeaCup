/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 全局指令统一导出
 */
import { vLazy, setupLazyLoad } from './lazyLoad'
import { vLazyComponent, setupLazyComponent } from './lazyComponent'
import type { Directive } from 'vue'
import { permission } from './permission'
import { useUserStore } from '@/stores/user'

export { vLazy, setupLazyLoad }
export { vLazyComponent, setupLazyComponent }

/**
 * 权限指令
 * 用法：
 *   v-permission="'user:add'"                          // 单权限
 *   v-permission="['user:add', 'user:edit']"           // 多权限（任一）
 *   v-permission:all="['user:add', 'user:edit']"       // 多权限（全部）
 * 
 * 使用 permission.ts 中的统一实现
 */
export const vPermission = permission

/**
 * 角色指令
 * 用法：v-role="'admin'" 或 v-role="['admin', 'editor']"
 */
export const vRole: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    const userStore = useUserStore()
    const { value } = binding
    
    if (!value) return
    
    const roles = userStore.roles || []
    const hasRole = Array.isArray(value)
      ? value.some(r => roles.includes(r))
      : roles.includes(value)
    
    if (!hasRole) {
      el.parentNode?.removeChild(el)
    }
  }
}

/**
 * 注册所有指令
 */
export function setupDirectives(app: any) {
  app.directive('lazy', vLazy)
  app.directive('permission', vPermission)
  app.directive('role', vRole)
  setupLazyComponent(app)
}

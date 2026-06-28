/**
 * 用户状态管理 Store
 * 参考若依框架实现
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, getUserInfo } from '@/api/system/user'
import type { LoginForm, UserInfo } from '@/types/user'
import { logger } from '@/utils/logger'

// Token 存储 key
const TOKEN_KEY = 'token'

export const useUserStore = defineStore('user', () => {
  /** JWT Token */
  const token = ref<string>(getToken())
  
  /** 用户信息 */
  const userInfo = ref<UserInfo | null>(null)
  
  /** 用户权限列表 */
  const permissions = ref<string[]>([])
  
  /** 用户角色列表 */
  const roles = ref<string[]>([])
  
  /** 是否需要强制修改密码（从 sessionStorage 恢复，防止刷新后丢失） */
  const mustChangePassword = ref<boolean>(sessionStorage.getItem('mustChangePassword') === '1')

  /** 权限是否已加载 */
  const permissionsLoaded = ref<boolean>(false)

  /** 会话超时时间（毫秒），30分钟 */
  const SESSION_TIMEOUT = 30 * 60 * 1000

  /** 最后活动时间 */
  const lastActivityTime = ref<number>(Date.now())

  /**
   * 获取 Token
   */
  function getToken(): string {
    return localStorage.getItem(TOKEN_KEY) || ''
  }

  /**
   * 设置 Token
   */
  function setToken(newToken: string) {
    token.value = newToken
    if (newToken) {
      localStorage.setItem(TOKEN_KEY, newToken)
      updateLastActivityTime()
    } else {
      localStorage.removeItem(TOKEN_KEY)
    }
  }

  /**
   * 移除 Token
   */
  function removeToken() {
    token.value = ''
    localStorage.removeItem(TOKEN_KEY)
  }

  /**
   * 更新最后活动时间
   */
  function updateLastActivityTime() {
    lastActivityTime.value = Date.now()
    if (token.value) {
      sessionStorage.setItem('lastActivityTime', lastActivityTime.value.toString())
    }
  }

  /**
   * 检查会话是否超时
   */
  function checkSessionTimeout(): boolean {
    if (!token.value) {
      return false
    }
    
    const storedTime = sessionStorage.getItem('lastActivityTime')
    const stored = storedTime ? parseInt(storedTime, 10) : lastActivityTime.value
    const now = Date.now()
    const elapsed = now - stored
    
    if (elapsed > SESSION_TIMEOUT) {
      return true
    }
    
    return false
  }

  /**
   * 初始化会话管理
   */
  function initSessionManagement() {
    if (token.value) {
      const storedTime = sessionStorage.getItem('lastActivityTime')
      if (storedTime) {
        lastActivityTime.value = parseInt(storedTime, 10)
      } else {
        updateLastActivityTime()
      }
    }
  }

  /**
   * 设置用户信息
   */
  function setUserInfo(info: UserInfo) {
    userInfo.value = info
  }

  /**
   * 设置权限列表
   */
  function setPermissions(perms: string[]) {
    permissions.value = perms
    permissionsLoaded.value = true
  }
  
  /**
   * 设置角色列表
   */
  function setRoles(roleList: string[]) {
    roles.value = roleList
  }

  /**
   * 用户登录
   */
  async function loginAction(form: LoginForm) {
    const res = await login(form)
    const data = (res.data as unknown) as { token: string; userInfo: UserInfo; mustChangePassword?: boolean }
    setToken(data.token)
    setUserInfo(data.userInfo)
    mustChangePassword.value = !!data.mustChangePassword
    if (data.mustChangePassword) {
      sessionStorage.setItem('mustChangePassword', '1')
    } else {
      sessionStorage.removeItem('mustChangePassword')
    }
    setPermissions(['*'])
    return res
  }

  /**
   * 获取用户信息
   */
  async function fetchUserInfo() {
    try {
      const res = await getUserInfo()
      const data = (res.data as unknown) as { 
        userInfo: UserInfo
        permissions: string[] | Record<string, string>
        roles?: string[]
      }
      
      setUserInfo(data.userInfo)
      
      // 处理权限列表
      let permissionsList: string[] = []
      
      if (Array.isArray(data.permissions)) {
        permissionsList = data.permissions
      } else if (data.permissions && typeof data.permissions === 'object') {
        permissionsList = Object.values(data.permissions).filter((v): v is string => typeof v === 'string')
      }
      
      if (permissionsList.length === 0) {
        logger.warn('用户权限列表为空')
      }
      
      setPermissions(permissionsList)
      
      // 处理角色列表
      if (data.roles && Array.isArray(data.roles)) {
        setRoles(data.roles)
      } else {
        setRoles([])
      }
    } catch (error) {
      logger.error('获取用户信息失败', error)
      throw error
    }
  }

  /**
   * 退出登录 - 清理所有状态
   */
  function logout() {
    // 清除 token
    removeToken()
    
    // 清除用户信息
    userInfo.value = null
    permissions.value = []
    roles.value = []
    permissionsLoaded.value = false
    mustChangePassword.value = false
    lastActivityTime.value = 0
    
    // 清除会话相关
    sessionStorage.removeItem('mustChangePassword')
    sessionStorage.removeItem('lastActivityTime')
    
    // 清除标签页缓存（直接操作 sessionStorage 避免循环依赖）
    sessionStorage.removeItem('tabs-views')
    sessionStorage.removeItem('tabs-active')
  }

  /**
   * 重置状态（用于登出后完全重置）
   */
  function resetState() {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    roles.value = []
    permissionsLoaded.value = false
    lastActivityTime.value = Date.now()
  }

  /**
   * 检查是否是管理员
   */
  function isAdmin(): boolean {
    if (!Array.isArray(permissions.value)) {
      return false
    }
    return permissions.value.includes('*')
  }

  /**
   * 检查是否有指定权限
   */
  function hasPermission(permission: string): boolean {
    if (isAdmin()) {
      return true
    }
    
    if (!Array.isArray(permissions.value)) {
      return false
    }
    
    return permissions.value.includes(permission)
  }
  
  /**
   * 检查是否有指定角色
   */
  function hasRole(role: string): boolean {
    if (isAdmin()) {
      return true
    }
    
    if (!Array.isArray(roles.value)) {
      return false
    }
    
    return roles.value.includes(role)
  }
  
  /**
   * 获取用户名（便捷方法）
   */
  const username = computed(() => userInfo.value?.username || '')

  return {
    token,
    userInfo,
    permissions,
    roles,
    permissionsLoaded,
    mustChangePassword,
    lastActivityTime,
    username,
    getToken,
    setToken,
    removeToken,
    setUserInfo,
    setPermissions,
    setRoles,
    loginAction,
    fetchUserInfo,
    logout,
    resetState,
    isAdmin,
    hasPermission,
    hasRole,
    updateLastActivityTime,
    checkSessionTimeout,
    initSessionManagement
  }
})

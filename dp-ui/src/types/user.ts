/**
 * 用户相关类型定义
 */

/**
 * 登录表单
 */
export interface LoginForm {
  /** 用户名 */
  username: string
  /** 密码 */
  password: string
  /** 验证码key（验证码启用时） */
  captchaKey?: string
  /** 验证码（验证码启用时） */
  captchaCode?: string
}

/**
 * 验证码响应
 */
export interface CaptchaResult {
  /** 是否启用验证码 */
  enabled: boolean
  /** 验证码key */
  captchaKey?: string
  /** 验证码base64图片 */
  captchaImage?: string
}

/**
 * 用户信息
 */
export interface UserInfo {
  /** 用户ID */
  id: number
  /** 用户名 */
  username: string
  /** 昵称 */
  nickname: string
  /** 邮箱 */
  email: string
  /** 头像URL */
  avatar?: string
  /** 角色列表 */
  roles: string[]
}

/**
 * 登录响应数据
 */
export interface LoginResponse {
  /** JWT Token */
  token: string
  /** 用户信息 */
  userInfo: UserInfo
}

/**
 * 用户信息（列表）
 */
export interface User {
  /** 用户ID */
  id: number
  /** 用户名 */
  username: string
  /** 昵称 */
  nickname: string
  /** 邮箱 */
  email: string
  /** 部门ID */
  deptId?: number | null
  /** 岗位ID */
  postId?: number | null
  /** 岗位名称（关联查询） */
  postName?: string
  /** 岗位状态（关联查询） */
  postStatus?: number
  /** 手机号 */
  phone?: string
  /** 性别: 0-未知 1-男 2-女 */
  gender?: number
  /** 状态：1-启用，0-禁用 */
  status: number
  /** 头像标识 */
  avatar?: string
  /** 创建时间 */
  createTime: string
  /** 角色列表 */
  roles: string[]
}

/**
 * 用户创建/更新表单
 */
export interface UserForm {
  /** 用户ID（更新时必填） */
  id?: number | null
  /** 用户名 */
  username: string
  /** 密码（创建时必填） */
  password?: string
  /** 昵称 */
  nickname: string
  /** 邮箱 */
  email: string
  /** 部门ID */
  deptId?: number | null
  /** 岗位ID */
  postId?: number | null
  /** 手机号 */
  phone?: string
  /** 性别: 0-未知 1-男 2-女 */
  gender?: number | null
  /** 状态：1-启用，0-禁用 */
  status: number
  /** 头像标识 */
  avatar?: string | null
}

/**
 * 用户状态选项
 */
export interface UserStatusOption {
  label: string
  value: number
}


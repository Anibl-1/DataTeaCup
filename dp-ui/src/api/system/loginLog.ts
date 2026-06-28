import request from '../request'

export interface LoginLog {
  id: number
  username: string
  ipAddress: string
  userAgent: string
  browser: string
  os: string
  status: string
  message: string
  loginTime: string
}

/**
 * 分页查询登录日志
 */
export const getLoginLogList = (params: {
  page?: number
  pageSize?: number
  username?: string
  status?: string
  ipAddress?: string
}) => {
  return request.get('/login-log/list', { params })
}

/**
 * 清理历史登录日志
 */
export const cleanLoginLog = (days: number) => {
  return request.delete('/login-log/clean', { data: { days } })
}

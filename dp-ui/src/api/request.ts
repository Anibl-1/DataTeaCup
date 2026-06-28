/* eslint-disable @typescript-eslint/no-explicit-any */
import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig, AxiosRequestConfig, AxiosError } from 'axios'
import { message } from '@/utils/message'
import { logger } from '@/utils/logger'
import { t } from '@/i18n'
import type { ApiResponse } from '@/types/api'

// ==================== 重试配置 ====================
const RETRY_CONFIG = {
  maxRetries: 3,          // 最大重试次数
  retryDelay: 1000,       // 重试延迟(ms)
  retryableStatuses: [408, 500, 502, 503, 504], // 可重试的HTTP状态码
}

// 扩展 AxiosRequestConfig 支持重试元数据
interface RetryMeta {
  __retryCount?: number
  __noRetry?: boolean
  __silent?: boolean  // 静默模式：不显示错误提示
}

/**
 * 扩展请求方法类型，使返回类型更准确
 * 响应拦截器会解包axios响应，直接返回ApiResponse
 */
interface RequestInstance extends AxiosInstance {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
}

// ==================== 请求去重机制 ====================
const pendingRequests = new Map<string, AbortController>()

/**
 * 生成请求唯一标识（仅用于 GET 请求去重）
 */
export function getRequestKey(config: AxiosRequestConfig): string {
  return `${config.method}:${config.url}:${JSON.stringify(config.params)}`
}

/**
 * 移除已完成的请求记录
 */
function removePendingRequest(config: AxiosRequestConfig): void {
  if (config.method?.toUpperCase() === 'GET') {
    const key = getRequestKey(config)
    pendingRequests.delete(key)
  }
}

/**
 * 对 GET 请求进行去重：相同 URL + 参数的请求在前一个未完成时取消旧请求
 */
function deduplicateRequest(config: InternalAxiosRequestConfig): void {
  if (config.method?.toUpperCase() === 'GET') {
    const key = getRequestKey(config)
    // 如果已有相同请求在进行中，取消旧请求（让新请求执行）
    if (pendingRequests.has(key)) {
      const oldController = pendingRequests.get(key)
      oldController?.abort()
      pendingRequests.delete(key)
    }
    // 注册当前请求
    const controller = new AbortController()
    config.signal = controller.signal
    pendingRequests.set(key, controller)
  }
}

/**
 * 取消所有进行中的请求
 */
export function cancelAllPendingRequests(): void {
  pendingRequests.forEach((controller) => {
    controller.abort()
  })
  pendingRequests.clear()
}

/**
 * 清理登录状态并跳转
 */
const handleUnauthorized = () => {
  localStorage.removeItem('token')
  sessionStorage.removeItem('tabs-views')
  sessionStorage.removeItem('tabs-active')
  sessionStorage.removeItem('lastActivityTime')
  
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

/**
 * 创建Axios实例
 */
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: Number(import.meta.env.VITE_API_TIMEOUT) || 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * 判断是否为可重试的请求
 */
const isRetryable = (error: AxiosError): boolean => {
  const config = error.config as any
  if (!config || config.__noRetry) return false
  
  const isIdempotent = ['GET', 'HEAD', 'OPTIONS', 'PUT', 'DELETE'].includes(config.method?.toUpperCase() || '')
  const isNetworkError = !error.response
  const isTimeoutError = error.code === 'ECONNABORTED' || error.code === 'ERR_NETWORK'
  const isRetryableStatus = error.response && RETRY_CONFIG.retryableStatuses.includes(error.response.status)
  
  // 网络错误和超时对所有幂等方法重试；可重试状态码仅对 GET 重试
  if (isNetworkError || isTimeoutError) return isIdempotent
  return config.method?.toUpperCase() === 'GET' && !!isRetryableStatus
}

const isCanceledRequest = (error: unknown): boolean => {
  const err = error as { code?: string; name?: string; message?: string }
  return axios.isCancel(error) || err?.code === 'ERR_CANCELED' || err?.name === 'CanceledError' || err?.message === 'canceled'
}

/**
 * 执行重试逻辑
 */
const retryRequest = (error: AxiosError): Promise<any> => {
  const config = error.config as any
  if (!config) return Promise.reject(error)
  
  config.__retryCount = config.__retryCount || 0
  
  if (config.__retryCount >= RETRY_CONFIG.maxRetries) {
    return Promise.reject(error)
  }
  
  config.__retryCount += 1
  logger.warn(`[API] 重试第 ${config.__retryCount} 次: ${config.method?.toUpperCase()} ${config.url}`)
  
  return new Promise(resolve => {
    setTimeout(() => resolve(service(config)), RETRY_CONFIG.retryDelay * config.__retryCount)
  })
}

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
      // 更新最后活动时间
      sessionStorage.setItem('lastActivityTime', Date.now().toString())
    }
    
    // 携带当前菜单页面名称（用于操作日志记录）
    try {
      const storedActive = sessionStorage.getItem('tabs-active')
      const storedTabs = sessionStorage.getItem('tabs-views')
      if (storedActive && storedTabs && config.headers) {
        const tabs = JSON.parse(storedTabs)
        const activeTab = tabs.find((t: any) => t.key === storedActive)
        if (activeTab?.title) {
          config.headers['X-Menu-Name'] = encodeURIComponent(activeTab.title)
        }
      }
    } catch {
      // 忽略
    }
    
    // GET 请求去重
    deduplicateRequest(config)
    
    // 开发环境记录请求日志
    logger.debug(`[API] ${config.method?.toUpperCase()} ${config.url}`, {
      params: config.params,
      data: config.data
    })
    
    return config
  },
  (error) => {
    logger.error('请求错误', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    // 请求完成，从去重 Map 中移除
    removePendingRequest(response.config)
    
    // 检查是否有新Token（自动刷新机制）
    const newToken = response.headers['x-new-token']
    if (newToken) {
      localStorage.setItem('token', newToken)
      logger.info('[Request] Token已自动刷新')
    }
    
    // 如果是blob响应（导出文件），检查是否为错误响应
    if (response.config.responseType === 'blob') {
      // 检查Content-Type，如果是JSON说明是错误响应
      const contentType = response.headers['content-type'] || response.headers['Content-Type'] || ''
      if (contentType.includes('application/json')) {
        // 尝试解析错误信息
        return (response.data as Blob).text().then((text: string) => {
          try {
            const json = JSON.parse(text)
            const errorMsg = json.msg || json.message || t('common.requestFailed')
            message.error(errorMsg)
            return Promise.reject(new Error(errorMsg))
          } catch {
            message.error(t('common.requestFailed'))
            return Promise.reject(new Error(t('common.requestFailed')))
          }
        })
      }
      // 正常blob响应，返回 blob 数据
      return response.data
    }
    
    // 普通JSON响应
    const res = response.data as ApiResponse
    
    // 记录响应日志
    logger.debug(`[API] Response ${response.config.url}`, { code: res.code, msg: res.msg })
    
    // 如果响应码不是200，视为错误
    if (res.code !== 200) {
      const errorMsg = res.msg || t('common.requestFailed')
      const config = response.config as any
      if (!config?.__silent) message.error(errorMsg)
      
      // 401未授权，取消所有进行中请求并清除token跳转登录
      if (res.code === 401) {
        cancelAllPendingRequests()
        handleUnauthorized()
      }
      
      return Promise.reject(new Error(errorMsg))
    }
    
    // 返回完整的响应数据，包含code、msg、data等字段
    return res
  },
  (error) => {
    // 请求完成（即使失败），从去重 Map 中移除
    if (error.config) {
      removePendingRequest(error.config)
    }

    if (isCanceledRequest(error)) {
      return Promise.reject(error)
    }
    
    const isSilentRequest = (error.config as any)?.__silent
    if (!isSilentRequest) {
      logger.error('响应错误', {
        message: error.message,
        status: error.response?.status,
        url: error.config?.url
      })
    }
    
    // 处理HTTP错误状态码
    if (error.response) {
      // 可重试的HTTP状态码，先尝试重试
      if (isRetryable(error)) {
        return retryRequest(error)
      }
      
      const config = error.config as any
      const isSilent = config?.__silent
      const status = error.response.status
      const res = error.response.data
      
      // 优先使用后端返回的错误信息
      let errorMsg = t('common.requestFailed')
      if (res) {
        // 如果响应是Result格式，使用msg字段
        if (typeof res === 'object' && res.msg) {
          errorMsg = res.msg
        } else if (typeof res === 'object' && res.message) {
          errorMsg = res.message
        } else if (typeof res === 'string') {
          errorMsg = res
        }
      }
      
      switch (status) {
        case 401:
          errorMsg = errorMsg || t('common.unauthorized')
          cancelAllPendingRequests()
          handleUnauthorized()
          break
        case 403:
          errorMsg = t('common.forbidden')
          console.warn(`[API] 权限不足，被拒绝的 API 路径: ${error.config?.url}`)
          break
        case 404:
          errorMsg = errorMsg || t('common.notFound')
          break
        case 429:
          errorMsg = errorMsg || t('common.tooManyRequests')
          break
        case 500:
          errorMsg = errorMsg || t('common.internalServerError')
          break
        default:
          errorMsg = errorMsg || t('common.requestFailedWithStatus', { status })
      }
      
      if (!isSilent) message.error(errorMsg)
      return Promise.reject(new Error(errorMsg))
    } else if (error.request) {
      // 请求已发出但没有收到响应 - 尝试重试
      if (isRetryable(error)) {
        return retryRequest(error)
      }
      const config = error.config as any
      if (config?.__silent) {
        return Promise.reject(new Error(t('common.networkConnectionFailed')))
      }
      const errorMsg = t('common.networkConnectionFailed')
      message.error(errorMsg)
      logger.error('后端服务连接失败')
      return Promise.reject(new Error(errorMsg))
    } else {
      // 其他错误（包括被取消的请求）
      const config = error.config as any
      if (config?.__silent || isCanceledRequest(error)) {
        return Promise.reject(error)
      }
      message.error(error.message || t('common.requestFailed'))
      return Promise.reject(error)
    }
  }
)

// ==================== 上传/导出超时配置 ====================

/** 创建带加长超时的请求配置（用于上传/导出场景） */
export const LONG_TIMEOUT_CONFIG: AxiosRequestConfig = {
  timeout: 300000 // 5分钟
}

/** 创建带加长超时的上传请求配置 */
export const UPLOAD_CONFIG: AxiosRequestConfig = {
  timeout: 600000, // 10分钟
  headers: { 'Content-Type': 'multipart/form-data' }
}

export default service as RequestInstance

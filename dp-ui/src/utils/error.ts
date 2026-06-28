/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 错误处理工具函数
 */
import { t } from '@/i18n'

/**
 * 从错误对象中提取错误消息
 * @param error 错误对象
 * @param defaultMessage 默认错误消息
 * @returns 错误消息
 */
export function getErrorMessage(error: any, defaultMessage?: string): string {
  const fallback = defaultMessage || t('common.operationFailedRetry')
  if (!error) {
    return fallback
  }
  
  // 处理响应错误
  if (error.response?.data?.msg) {
    return error.response.data.msg
  }
  
  if (error.response?.data?.message) {
    return error.response.data.message
  }
  
  // 处理验证错误
  if (error.errors) {
    const firstError = Object.values(error.errors)[0] as any
    if (Array.isArray(firstError) && firstError.length > 0) {
      return firstError[0]?.message || defaultMessage
    }
    if (firstError?.message) {
      return firstError.message
    }
  }
  
  // 处理普通错误消息
  if (error.message) {
    return error.message
  }
  
  // 处理字符串错误
  if (typeof error === 'string') {
    return error
  }
  
  return fallback
}

/**
 * 处理API错误并记录日志
 * @param error 错误对象
 * @param context 错误上下文（用于日志）
 * @param defaultMessage 默认错误消息
 * @returns 错误消息
 */
export function handleApiError(error: any, context: string, defaultMessage?: string): string {
  const errorMessage = getErrorMessage(error, defaultMessage || t('common.contextFailed', { context }))
  
  // 只在开发环境或非预期错误时记录详细日志
  if (import.meta.env.DEV || !error.response) {
    console.error(`[${context}]`, error)
  }
  
  return errorMessage
}


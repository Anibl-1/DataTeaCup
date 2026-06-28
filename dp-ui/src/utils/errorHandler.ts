/**
 * 统一错误处理器
 * 
 * 实现错误消息友好转换，将技术错误信息转换为用户可理解的友好提示。
 * 
 * 需求 18.1: THE Error_Handler SHALL 将技术错误信息转换为用户可理解的友好提示
 * 
 * @module errorHandler
 */

import { logger } from './logger'

// ==================== 类型定义 ====================

/**
 * 错误类型枚举
 */
export enum ErrorType {
  /** 网络错误 */
  NETWORK = 'NETWORK',
  /** 认证错误 */
  AUTH = 'AUTH',
  /** 权限错误 */
  PERMISSION = 'PERMISSION',
  /** 验证错误 */
  VALIDATION = 'VALIDATION',
  /** 服务器错误 */
  SERVER = 'SERVER',
  /** 业务错误 */
  BUSINESS = 'BUSINESS',
  /** 超时错误 */
  TIMEOUT = 'TIMEOUT',
  /** 资源不存在 */
  NOT_FOUND = 'NOT_FOUND',
  /** 请求频率限制 */
  RATE_LIMIT = 'RATE_LIMIT',
  /** 未知错误 */
  UNKNOWN = 'UNKNOWN'
}

/**
 * 错误严重级别
 */
export enum ErrorSeverity {
  /** 低 - 可忽略的错误 */
  LOW = 'low',
  /** 中 - 需要用户注意 */
  MEDIUM = 'medium',
  /** 高 - 需要用户处理 */
  HIGH = 'high',
  /** 严重 - 系统级错误 */
  CRITICAL = 'critical'
}

/**
 * 错误信息接口
 */
export interface ErrorInfo {
  /** 错误码 */
  code: string
  /** 原始错误消息 */
  message: string
  /** 用户友好消息 */
  userMessage: string
  /** 解决建议列表 */
  suggestions: string[]
  /** 错误追踪ID */
  traceId: string
  /** 错误时间戳 */
  timestamp: number
  /** 错误类型 */
  type: ErrorType
  /** 错误严重级别 */
  severity: ErrorSeverity
  /** 原始错误对象 */
  originalError?: Error | unknown
  /** 额外上下文信息 */
  context?: Record<string, unknown>
}

/**
 * API错误接口
 */
export interface ApiError {
  /** HTTP状态码 */
  status?: number
  /** 响应数据 */
  response?: {
    status?: number
    data?: {
      code?: number | string
      msg?: string
      message?: string
      error?: string
    }
  }
  /** 错误消息 */
  message?: string
  /** 错误码 */
  code?: string
  /** 请求配置 */
  config?: {
    url?: string
    method?: string
  }
}

/**
 * 错误消息映射配置
 */
interface ErrorMessageConfig {
  /** 用户友好消息 */
  message: string
  /** 解决建议 */
  suggestions: string[]
  /** 错误类型 */
  type: ErrorType
  /** 严重级别 */
  severity: ErrorSeverity
}

// ==================== 错误消息映射 ====================

/**
 * 错误码到友好消息的映射
 */
const ERROR_CODE_MESSAGES: Record<string, ErrorMessageConfig> = {
  // 网络相关错误
  'ERR_NETWORK': {
    message: '网络连接失败',
    suggestions: [
      '请检查您的网络连接是否正常',
      '尝试刷新页面重试',
      '如果问题持续，请联系技术支持'
    ],
    type: ErrorType.NETWORK,
    severity: ErrorSeverity.HIGH
  },
  'ECONNABORTED': {
    message: '请求超时',
    suggestions: [
      '网络可能较慢，请稍后重试',
      '检查网络连接是否稳定',
      '如果数据量较大，请耐心等待'
    ],
    type: ErrorType.TIMEOUT,
    severity: ErrorSeverity.MEDIUM
  },
  'ETIMEDOUT': {
    message: '连接超时',
    suggestions: [
      '服务器响应较慢，请稍后重试',
      '检查网络连接是否正常'
    ],
    type: ErrorType.TIMEOUT,
    severity: ErrorSeverity.MEDIUM
  },

  // 认证相关错误
  'AUTH_FAILED': {
    message: '认证失败',
    suggestions: [
      '请重新登录',
      '检查用户名和密码是否正确'
    ],
    type: ErrorType.AUTH,
    severity: ErrorSeverity.HIGH
  },
  'TOKEN_EXPIRED': {
    message: '登录已过期',
    suggestions: [
      '请重新登录以继续操作',
      '为了账户安全，系统会定期要求重新登录'
    ],
    type: ErrorType.AUTH,
    severity: ErrorSeverity.HIGH
  },
  'TOKEN_INVALID': {
    message: '登录状态无效',
    suggestions: [
      '请重新登录',
      '如果问题持续，请清除浏览器缓存后重试'
    ],
    type: ErrorType.AUTH,
    severity: ErrorSeverity.HIGH
  },

  // 权限相关错误
  'PERMISSION_DENIED': {
    message: '没有操作权限',
    suggestions: [
      '您没有执行此操作的权限',
      '请联系管理员获取相应权限'
    ],
    type: ErrorType.PERMISSION,
    severity: ErrorSeverity.MEDIUM
  },
  'ACCESS_DENIED': {
    message: '访问被拒绝',
    suggestions: [
      '您没有访问此资源的权限',
      '请联系管理员确认您的权限设置'
    ],
    type: ErrorType.PERMISSION,
    severity: ErrorSeverity.MEDIUM
  },

  // 数据源相关错误
  'DATA_SOURCE_CONNECTION_FAILED': {
    message: '数据源连接失败',
    suggestions: [
      '请检查数据源配置是否正确',
      '确认数据库服务是否正常运行',
      '检查网络连接是否正常',
      '验证数据库用户名和密码是否正确'
    ],
    type: ErrorType.BUSINESS,
    severity: ErrorSeverity.HIGH
  },
  'DATA_SOURCE_NOT_FOUND': {
    message: '数据源不存在',
    suggestions: [
      '请检查数据源是否已被删除',
      '确认数据源ID是否正确',
      '尝试重新选择数据源'
    ],
    type: ErrorType.NOT_FOUND,
    severity: ErrorSeverity.MEDIUM
  },
  'DATA_SOURCE_TIMEOUT': {
    message: '数据源查询超时',
    suggestions: [
      '查询数据量可能较大，请尝试添加筛选条件',
      '优化SQL查询语句',
      '联系管理员检查数据库性能'
    ],
    type: ErrorType.TIMEOUT,
    severity: ErrorSeverity.MEDIUM
  },

  // SQL相关错误
  'SQL_SYNTAX_ERROR': {
    message: 'SQL语法错误',
    suggestions: [
      '请检查SQL语句的语法是否正确',
      '确认表名和字段名是否存在',
      '检查SQL关键字是否拼写正确'
    ],
    type: ErrorType.VALIDATION,
    severity: ErrorSeverity.MEDIUM
  },
  'SQL_EXECUTION_ERROR': {
    message: 'SQL执行失败',
    suggestions: [
      '请检查SQL语句是否正确',
      '确认引用的表和字段是否存在',
      '检查数据类型是否匹配'
    ],
    type: ErrorType.BUSINESS,
    severity: ErrorSeverity.MEDIUM
  },

  // 导出相关错误
  'EXPORT_FAILED': {
    message: '导出失败',
    suggestions: [
      '请稍后重试',
      '如果数据量较大，请尝试分批导出',
      '检查磁盘空间是否充足'
    ],
    type: ErrorType.BUSINESS,
    severity: ErrorSeverity.MEDIUM
  },
  'EXPORT_SIZE_EXCEEDED': {
    message: '导出数据量超出限制',
    suggestions: [
      '请添加筛选条件减少数据量',
      '尝试分批导出数据',
      '联系管理员调整导出限制'
    ],
    type: ErrorType.VALIDATION,
    severity: ErrorSeverity.MEDIUM
  },

  // 报表相关错误
  'REPORT_NOT_FOUND': {
    message: '报表不存在',
    suggestions: [
      '报表可能已被删除',
      '请检查报表ID是否正确',
      '返回报表列表重新选择'
    ],
    type: ErrorType.NOT_FOUND,
    severity: ErrorSeverity.MEDIUM
  },
  'REPORT_SAVE_FAILED': {
    message: '报表保存失败',
    suggestions: [
      '请检查报表配置是否完整',
      '确认您有保存报表的权限',
      '稍后重试'
    ],
    type: ErrorType.BUSINESS,
    severity: ErrorSeverity.HIGH
  },

  // 图表相关错误
  'CHART_RENDER_ERROR': {
    message: '图表渲染失败',
    suggestions: [
      '请检查图表配置是否正确',
      '确认数据格式是否符合要求',
      '尝试刷新页面'
    ],
    type: ErrorType.BUSINESS,
    severity: ErrorSeverity.MEDIUM
  },
  'CHART_DATA_ERROR': {
    message: '图表数据错误',
    suggestions: [
      '请检查数据源配置',
      '确认SQL查询返回的数据格式正确',
      '检查字段映射是否正确'
    ],
    type: ErrorType.VALIDATION,
    severity: ErrorSeverity.MEDIUM
  },

  // 缓存相关错误
  'CACHE_ERROR': {
    message: '缓存服务异常',
    suggestions: [
      '系统正在使用备用方案，功能不受影响',
      '如果响应较慢，请稍后重试'
    ],
    type: ErrorType.SERVER,
    severity: ErrorSeverity.LOW
  },

  // 脱敏相关错误
  'MASKING_CONFIG_ERROR': {
    message: '数据脱敏配置错误',
    suggestions: [
      '请检查脱敏规则配置是否正确',
      '确认脱敏策略参数是否有效',
      '联系管理员检查脱敏配置'
    ],
    type: ErrorType.VALIDATION,
    severity: ErrorSeverity.HIGH
  },

  // 服务器错误
  'INTERNAL_SERVER_ERROR': {
    message: '服务器内部错误',
    suggestions: [
      '服务器遇到了问题，请稍后重试',
      '如果问题持续，请联系技术支持'
    ],
    type: ErrorType.SERVER,
    severity: ErrorSeverity.CRITICAL
  },
  'SERVICE_UNAVAILABLE': {
    message: '服务暂时不可用',
    suggestions: [
      '服务器正在维护中，请稍后重试',
      '如果问题持续，请联系技术支持'
    ],
    type: ErrorType.SERVER,
    severity: ErrorSeverity.CRITICAL
  },

  // 请求频率限制
  'RATE_LIMIT_EXCEEDED': {
    message: '请求过于频繁',
    suggestions: [
      '请稍等片刻后再试',
      '避免短时间内重复操作'
    ],
    type: ErrorType.RATE_LIMIT,
    severity: ErrorSeverity.LOW
  }
}

/**
 * HTTP状态码到友好消息的映射
 */
const HTTP_STATUS_MESSAGES: Record<number, ErrorMessageConfig> = {
  400: {
    message: '请求参数错误',
    suggestions: [
      '请检查输入的数据是否正确',
      '确认必填项是否已填写'
    ],
    type: ErrorType.VALIDATION,
    severity: ErrorSeverity.MEDIUM
  },
  401: {
    message: '未授权，请重新登录',
    suggestions: [
      '您的登录状态已失效',
      '请重新登录后继续操作'
    ],
    type: ErrorType.AUTH,
    severity: ErrorSeverity.HIGH
  },
  403: {
    message: '没有访问权限',
    suggestions: [
      '您没有执行此操作的权限',
      '请联系管理员获取相应权限'
    ],
    type: ErrorType.PERMISSION,
    severity: ErrorSeverity.MEDIUM
  },
  404: {
    message: '请求的资源不存在',
    suggestions: [
      '请检查访问的地址是否正确',
      '该资源可能已被删除或移动'
    ],
    type: ErrorType.NOT_FOUND,
    severity: ErrorSeverity.MEDIUM
  },
  408: {
    message: '请求超时',
    suggestions: [
      '网络可能较慢，请稍后重试',
      '检查网络连接是否稳定'
    ],
    type: ErrorType.TIMEOUT,
    severity: ErrorSeverity.MEDIUM
  },
  429: {
    message: '请求过于频繁',
    suggestions: [
      '请稍等片刻后再试',
      '避免短时间内重复操作'
    ],
    type: ErrorType.RATE_LIMIT,
    severity: ErrorSeverity.LOW
  },
  500: {
    message: '服务器内部错误',
    suggestions: [
      '服务器遇到了问题，请稍后重试',
      '如果问题持续，请联系技术支持'
    ],
    type: ErrorType.SERVER,
    severity: ErrorSeverity.CRITICAL
  },
  502: {
    message: '网关错误',
    suggestions: [
      '服务器暂时无法处理请求',
      '请稍后重试'
    ],
    type: ErrorType.SERVER,
    severity: ErrorSeverity.HIGH
  },
  503: {
    message: '服务暂时不可用',
    suggestions: [
      '服务器正在维护中',
      '请稍后重试'
    ],
    type: ErrorType.SERVER,
    severity: ErrorSeverity.CRITICAL
  },
  504: {
    message: '网关超时',
    suggestions: [
      '服务器响应超时',
      '请稍后重试',
      '如果问题持续，请联系技术支持'
    ],
    type: ErrorType.TIMEOUT,
    severity: ErrorSeverity.HIGH
  }
}

// ==================== 工具函数 ====================

/**
 * 追踪 ID 计数器，用于确保同一毫秒内生成的 ID 也是唯一的
 */
let traceIdCounter = 0

/**
 * 上一次生成追踪 ID 的时间戳
 */
let lastTraceIdTimestamp = 0

/**
 * 生成唯一的错误追踪ID
 * 
 * 格式: ERR-{timestamp}-{counter}-{random}
 * 
 * 使用时间戳 + 计数器 + 随机数的组合确保全局唯一性：
 * - 时间戳：精确到毫秒，提供时间维度的唯一性
 * - 计数器：同一毫秒内递增，防止时间戳冲突
 * - 随机数：增加额外的随机性，防止跨实例冲突
 * 
 * 需求 18.5: THE Error_Handler SHALL 为每个错误生成唯一的错误追踪ID，便于问题排查
 * 
 * @returns 唯一的错误追踪ID
 */
export function generateTraceId(): string {
  const now = Date.now()
  
  // 如果是同一毫秒内，递增计数器
  if (now === lastTraceIdTimestamp) {
    traceIdCounter++
  } else {
    // 新的毫秒，重置计数器
    traceIdCounter = 0
    lastTraceIdTimestamp = now
  }
  
  // 时间戳转为36进制，更紧凑
  const timestamp = now.toString(36)
  
  // 计数器转为36进制，确保同一毫秒内的唯一性
  const counter = traceIdCounter.toString(36).padStart(2, '0')
  
  // 生成6位随机字符，增加额外的随机性
  const random = Math.random().toString(36).substring(2, 8)
  
  return `ERR-${timestamp}-${counter}-${random}`.toUpperCase()
}

/**
 * 验证追踪 ID 格式是否有效
 * 
 * @param traceId 要验证的追踪 ID
 * @returns 是否为有效的追踪 ID 格式
 */
export function isValidTraceId(traceId: string): boolean {
  // 格式: ERR-{timestamp}-{counter}-{random}
  // 例如: ERR-LQ2X5K8-00-A1B2C3
  const pattern = /^ERR-[A-Z0-9]+-[A-Z0-9]{2,}-[A-Z0-9]{6}$/
  return pattern.test(traceId)
}

/**
 * 从追踪 ID 中提取时间戳
 * 
 * @param traceId 追踪 ID
 * @returns 时间戳（毫秒），如果格式无效则返回 null
 */
export function extractTimestampFromTraceId(traceId: string): number | null {
  if (!isValidTraceId(traceId)) {
    return null
  }
  
  try {
    // 提取时间戳部分 (ERR-{timestamp}-{counter}-{random})
    const parts = traceId.split('-')
    if (parts.length >= 2) {
      const timestampStr = parts[1].toLowerCase()
      return parseInt(timestampStr, 36)
    }
  } catch {
    // 解析失败
  }
  
  return null
}

/**
 * 格式化追踪 ID 用于显示
 * 
 * @param traceId 追踪 ID
 * @returns 格式化后的显示字符串
 */
export function formatTraceIdForDisplay(traceId: string): string {
  return traceId
}

/**
 * 从错误对象中提取错误码
 */
function extractErrorCode(error: Error | ApiError | unknown): string {
  if (!error) return 'UNKNOWN'
  
  const err = error as ApiError
  
  // 从响应数据中提取错误码
  if (err.response?.data?.code) {
    return String(err.response.data.code)
  }
  
  // 从错误对象中提取错误码
  if (err.code) {
    return err.code
  }
  
  // 从HTTP状态码生成错误码
  const status = err.response?.status || err.status
  if (status) {
    return `HTTP_${status}`
  }
  
  return 'UNKNOWN'
}

/**
 * 从错误对象中提取原始消息
 */
function extractOriginalMessage(error: Error | ApiError | unknown): string {
  if (!error) return '未知错误'
  
  const err = error as ApiError
  
  // 从响应数据中提取消息
  if (err.response?.data?.msg) {
    return err.response.data.msg
  }
  if (err.response?.data?.message) {
    return err.response.data.message
  }
  if (err.response?.data?.error) {
    return err.response.data.error
  }
  
  // 从错误对象中提取消息
  if (err.message) {
    return err.message
  }
  
  // 字符串错误
  if (typeof error === 'string') {
    return error
  }
  
  return '未知错误'
}

/**
 * 获取HTTP状态码
 */
function getHttpStatus(error: Error | ApiError | unknown): number | undefined {
  if (!error) return undefined
  const err = error as ApiError
  return err.response?.status || err.status
}

/**
 * 检测错误类型
 */
function detectErrorType(error: Error | ApiError | unknown): ErrorType {
  if (!error) return ErrorType.UNKNOWN
  
  const err = error as ApiError
  const code = err.code
  const status = getHttpStatus(error)
  const message = extractOriginalMessage(error).toLowerCase()
  
  // 网络错误
  if (code === 'ERR_NETWORK' || code === 'ECONNREFUSED' || !err.response && err.config) {
    return ErrorType.NETWORK
  }
  
  // 超时错误
  if (code === 'ECONNABORTED' || code === 'ETIMEDOUT' || message.includes('timeout')) {
    return ErrorType.TIMEOUT
  }
  
  // 认证错误
  if (status === 401 || message.includes('unauthorized') || message.includes('token')) {
    return ErrorType.AUTH
  }
  
  // 权限错误
  if (status === 403 || message.includes('forbidden') || message.includes('permission')) {
    return ErrorType.PERMISSION
  }
  
  // 资源不存在
  if (status === 404 || message.includes('not found')) {
    return ErrorType.NOT_FOUND
  }
  
  // 请求频率限制
  if (status === 429 || message.includes('rate limit') || message.includes('too many')) {
    return ErrorType.RATE_LIMIT
  }
  
  // 验证错误
  if (status === 400 || message.includes('validation') || message.includes('invalid')) {
    return ErrorType.VALIDATION
  }
  
  // 服务器错误
  if (status && status >= 500) {
    return ErrorType.SERVER
  }
  
  return ErrorType.UNKNOWN
}

/**
 * 根据错误类型获取严重级别
 */
function getSeverityByType(type: ErrorType): ErrorSeverity {
  switch (type) {
    case ErrorType.AUTH:
    case ErrorType.SERVER:
      return ErrorSeverity.CRITICAL
    case ErrorType.NETWORK:
    case ErrorType.PERMISSION:
      return ErrorSeverity.HIGH
    case ErrorType.VALIDATION:
    case ErrorType.TIMEOUT:
    case ErrorType.NOT_FOUND:
    case ErrorType.BUSINESS:
      return ErrorSeverity.MEDIUM
    case ErrorType.RATE_LIMIT:
      return ErrorSeverity.LOW
    default:
      return ErrorSeverity.MEDIUM
  }
}

// ==================== 错误处理器类 ====================

/**
 * 统一错误处理器
 * 
 * 提供错误消息友好转换、错误上报、解决建议等功能。
 */
class ErrorHandler {
  private reportEndpoint: string | null = null
  private enableReport: boolean = false
  
  /**
   * 配置错误处理器
   */
  configure(options: {
    reportEndpoint?: string
    enableReport?: boolean
  }) {
    if (options.reportEndpoint) {
      this.reportEndpoint = options.reportEndpoint
    }
    if (options.enableReport !== undefined) {
      this.enableReport = options.enableReport
    }
  }
  
  /**
   * 处理错误并返回友好的错误信息
   * 
   * @param error 原始错误对象
   * @param context 错误上下文信息
   * @returns 处理后的错误信息
   */
  handle(error: Error | ApiError | unknown, context?: Record<string, unknown>): ErrorInfo {
    const code = extractErrorCode(error)
    const originalMessage = extractOriginalMessage(error)
    const status = getHttpStatus(error)
    const traceId = generateTraceId()
    const timestamp = Date.now()
    
    // 尝试从错误码映射中获取配置
    let config = ERROR_CODE_MESSAGES[code]
    
    // 如果没有找到，尝试从HTTP状态码映射中获取
    if (!config && status) {
      config = HTTP_STATUS_MESSAGES[status]
    }
    
    // 如果仍然没有找到，使用默认配置
    const errorType = config?.type || detectErrorType(error)
    const severity = config?.severity || getSeverityByType(errorType)
    
    const errorInfo: ErrorInfo = {
      code,
      message: originalMessage,
      userMessage: config?.message || this.getDefaultUserMessage(errorType, originalMessage),
      suggestions: config?.suggestions || this.getDefaultSuggestions(errorType),
      traceId,
      timestamp,
      type: errorType,
      severity,
      originalError: error,
      context
    }
    
    // 记录错误日志
    logger.error(`[ErrorHandler] ${errorInfo.userMessage}`, {
      traceId,
      code,
      type: errorType,
      severity,
      originalMessage,
      context
    })
    
    // 上报错误
    if (this.enableReport) {
      this.report(errorInfo).catch(() => {
        // 上报失败静默处理
      })
    }
    
    return errorInfo
  }
  
  /**
   * 获取默认的用户友好消息
   */
  private getDefaultUserMessage(type: ErrorType, originalMessage: string): string {
    switch (type) {
      case ErrorType.NETWORK:
        return '网络连接失败，请检查网络后重试'
      case ErrorType.AUTH:
        return '登录状态已失效，请重新登录'
      case ErrorType.PERMISSION:
        return '您没有执行此操作的权限'
      case ErrorType.VALIDATION:
        return '输入的数据有误，请检查后重试'
      case ErrorType.SERVER:
        return '服务器遇到问题，请稍后重试'
      case ErrorType.TIMEOUT:
        return '请求超时，请稍后重试'
      case ErrorType.NOT_FOUND:
        return '请求的资源不存在'
      case ErrorType.RATE_LIMIT:
        return '操作过于频繁，请稍后重试'
      case ErrorType.BUSINESS:
        // 业务错误使用原始消息
        return originalMessage || '操作失败，请稍后重试'
      default:
        return '操作失败，请稍后重试'
    }
  }
  
  /**
   * 获取默认的解决建议
   */
  private getDefaultSuggestions(type: ErrorType): string[] {
    switch (type) {
      case ErrorType.NETWORK:
        return [
          '请检查您的网络连接是否正常',
          '尝试刷新页面重试',
          '如果问题持续，请联系技术支持'
        ]
      case ErrorType.AUTH:
        return [
          '请重新登录后继续操作'
        ]
      case ErrorType.PERMISSION:
        return [
          '请联系管理员获取相应权限'
        ]
      case ErrorType.VALIDATION:
        return [
          '请检查输入的数据是否正确',
          '确认必填项是否已填写'
        ]
      case ErrorType.SERVER:
        return [
          '请稍后重试',
          '如果问题持续，请联系技术支持'
        ]
      case ErrorType.TIMEOUT:
        return [
          '网络可能较慢，请稍后重试',
          '如果数据量较大，请耐心等待'
        ]
      case ErrorType.NOT_FOUND:
        return [
          '请检查访问的地址是否正确',
          '该资源可能已被删除'
        ]
      case ErrorType.RATE_LIMIT:
        return [
          '请稍等片刻后再试'
        ]
      default:
        return [
          '请稍后重试',
          '如果问题持续，请联系技术支持'
        ]
    }
  }
  
  /**
   * 获取指定错误码的解决建议
   */
  getSuggestions(errorCode: string): string[] {
    const config = ERROR_CODE_MESSAGES[errorCode]
    return config?.suggestions || []
  }
  
  /**
   * 上报错误到服务端
   */
  async report(errorInfo: ErrorInfo): Promise<void> {
    if (!this.reportEndpoint) {
      return
    }
    
    try {
      // 构建上报数据，排除原始错误对象（可能包含循环引用）
      const reportData = {
        code: errorInfo.code,
        message: errorInfo.message,
        userMessage: errorInfo.userMessage,
        traceId: errorInfo.traceId,
        timestamp: errorInfo.timestamp,
        type: errorInfo.type,
        severity: errorInfo.severity,
        context: errorInfo.context,
        userAgent: typeof navigator !== 'undefined' ? navigator.userAgent : undefined,
        url: typeof window !== 'undefined' ? window.location.href : undefined
      }
      
      await fetch(this.reportEndpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(reportData)
      })
    } catch {
      // 上报失败静默处理，避免影响用户体验
      logger.warn('[ErrorHandler] 错误上报失败')
    }
  }
  
  /**
   * 判断错误是否为网络错误
   */
  isNetworkError(error: Error | ApiError | unknown): boolean {
    return detectErrorType(error) === ErrorType.NETWORK
  }
  
  /**
   * 判断错误是否为认证错误
   */
  isAuthError(error: Error | ApiError | unknown): boolean {
    return detectErrorType(error) === ErrorType.AUTH
  }
  
  /**
   * 判断错误是否为超时错误
   */
  isTimeoutError(error: Error | ApiError | unknown): boolean {
    return detectErrorType(error) === ErrorType.TIMEOUT
  }
  
  /**
   * 判断错误是否为服务器错误
   */
  isServerError(error: Error | ApiError | unknown): boolean {
    return detectErrorType(error) === ErrorType.SERVER
  }
  
  /**
   * 判断错误是否可重试
   */
  isRetryable(error: Error | ApiError | unknown): boolean {
    const type = detectErrorType(error)
    return type === ErrorType.NETWORK || 
           type === ErrorType.TIMEOUT || 
           type === ErrorType.SERVER
  }
}

// ==================== 导出 ====================

/**
 * 错误处理器单例
 */
export const errorHandler = new ErrorHandler()

/**
 * 便捷函数：处理错误并返回友好消息
 */
export function handleError(error: Error | ApiError | unknown, context?: Record<string, unknown>): ErrorInfo {
  return errorHandler.handle(error, context)
}

/**
 * 便捷函数：获取用户友好的错误消息
 */
export function getUserFriendlyMessage(error: Error | ApiError | unknown): string {
  return errorHandler.handle(error).userMessage
}

/**
 * 便捷函数：获取错误的解决建议
 */
export function getErrorSuggestions(error: Error | ApiError | unknown): string[] {
  return errorHandler.handle(error).suggestions
}

// 导出错误消息映射，供外部扩展使用
export { ERROR_CODE_MESSAGES, HTTP_STATUS_MESSAGES }

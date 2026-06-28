/**
 * 前端错误上报服务
 * 
 * 自动收集前端错误并上报到服务端，包括：
 * - JavaScript 运行时错误
 * - Promise 未捕获的拒绝
 * - 资源加载错误
 * - Vue 组件错误
 * 
 * 需求 18.3: THE Error_Handler SHALL 自动收集前端错误并上报到服务端
 * 
 * @module errorReporter
 */

import { generateTraceId, ErrorSeverity } from './errorHandler'
import { logger } from './logger'
import type { App, ComponentPublicInstance } from 'vue'

// ==================== 类型定义 ====================

/**
 * 错误上报配置
 */
export interface ErrorReporterConfig {
  /** 上报端点 URL */
  reportEndpoint: string
  /** 是否启用上报 */
  enabled: boolean
  /** 采样率 (0-1)，1 表示全部上报 */
  sampleRate: number
  /** 最大上报队列长度 */
  maxQueueSize: number
  /** 批量上报间隔（毫秒） */
  batchInterval: number
  /** 是否捕获全局错误 */
  captureGlobalErrors: boolean
  /** 是否捕获 Promise 拒绝 */
  captureUnhandledRejections: boolean
  /** 是否捕获资源加载错误 */
  captureResourceErrors: boolean
  /** 是否捕获 Vue 组件错误 */
  captureVueErrors: boolean
  /** 忽略的错误消息模式 */
  ignorePatterns: (string | RegExp)[]
  /** 额外的上下文信息 */
  extraContext?: Record<string, unknown>
}

/**
 * 错误报告数据
 */
export interface ErrorReport {
  /** 错误追踪ID */
  traceId: string
  /** 错误类型 */
  type: ErrorReportType
  /** 错误消息 */
  message: string
  /** 错误堆栈 */
  stack?: string
  /** 错误来源 */
  source?: string
  /** 行号 */
  lineno?: number
  /** 列号 */
  colno?: number
  /** 错误时间戳 */
  timestamp: number
  /** 页面 URL */
  url: string
  /** 用户代理 */
  userAgent: string
  /** 错误严重级别 */
  severity: ErrorSeverity
  /** Vue 组件信息 */
  componentInfo?: VueComponentInfo
  /** 额外上下文 */
  context?: Record<string, unknown>
}

/**
 * 错误报告类型
 */
export type ErrorReportType = 
  | 'javascript'      // JavaScript 运行时错误
  | 'promise'         // Promise 未捕获拒绝
  | 'resource'        // 资源加载错误
  | 'vue'             // Vue 组件错误
  | 'api'             // API 请求错误
  | 'custom'          // 自定义错误

/**
 * Vue 组件信息
 */
export interface VueComponentInfo {
  /** 组件名称 */
  name?: string
  /** 组件文件路径 */
  file?: string
  /** 生命周期钩子 */
  hook?: string
  /** 组件 props */
  props?: Record<string, unknown>
}

// ==================== 默认配置 ====================

const DEFAULT_CONFIG: ErrorReporterConfig = {
  reportEndpoint: '/api/error/report',
  enabled: true,
  sampleRate: 1,
  maxQueueSize: 50,
  batchInterval: 5000,
  captureGlobalErrors: true,
  captureUnhandledRejections: true,
  captureResourceErrors: true,
  captureVueErrors: true,
  ignorePatterns: [
    // 忽略常见的无害错误
    'ResizeObserver loop',
    'Script error',
    'Network Error',
    /^Loading chunk \d+ failed/
  ]
}

// ==================== 错误上报器类 ====================

/**
 * 前端错误上报器
 * 
 * 自动收集和上报前端错误，支持批量上报、采样、去重等功能。
 */
class ErrorReporter {
  private config: ErrorReporterConfig = DEFAULT_CONFIG
  private queue: ErrorReport[] = []
  private batchTimer: ReturnType<typeof setTimeout> | null = null
  private reportedErrors: Set<string> = new Set()
  private initialized: boolean = false
  
  /**
   * 初始化错误上报器
   */
  init(config?: Partial<ErrorReporterConfig>): void {
    if (this.initialized) {
      logger.warn('[ErrorReporter] 已经初始化，跳过重复初始化')
      return
    }
    
    this.config = { ...DEFAULT_CONFIG, ...config }
    
    if (!this.config.enabled) {
      logger.info('[ErrorReporter] 错误上报已禁用')
      return
    }
    
    // 设置全局错误捕获
    if (typeof window !== 'undefined') {
      if (this.config.captureGlobalErrors) {
        this.setupGlobalErrorHandler()
      }
      
      if (this.config.captureUnhandledRejections) {
        this.setupUnhandledRejectionHandler()
      }
      
      if (this.config.captureResourceErrors) {
        this.setupResourceErrorHandler()
      }
    }
    
    // 启动批量上报定时器
    this.startBatchTimer()
    
    this.initialized = true
    logger.info('[ErrorReporter] 错误上报器初始化完成')
  }
  
  /**
   * 安装 Vue 错误处理器
   */
  installVueErrorHandler(app: App): void {
    if (!this.config.captureVueErrors) {
      return
    }
    
    const originalHandler = app.config.errorHandler
    
    app.config.errorHandler = (err: unknown, instance: ComponentPublicInstance | null, info: string) => {
      // 上报 Vue 错误
      this.captureVueError(err, instance, info)
      
      // 调用原有的错误处理器
      if (originalHandler) {
        originalHandler(err, instance, info)
      }
    }
    
    logger.info('[ErrorReporter] Vue 错误处理器已安装')
  }
  
  /**
   * 手动上报错误
   */
  captureError(error: Error | string, context?: Record<string, unknown>): void {
    const errorObj = typeof error === 'string' ? new Error(error) : error
    
    const report = this.createReport({
      type: 'custom',
      message: errorObj.message,
      stack: errorObj.stack,
      severity: ErrorSeverity.MEDIUM,
      context
    })
    
    this.addToQueue(report)
  }
  
  /**
   * 上报 API 错误
   */
  captureApiError(error: unknown, requestInfo?: { url?: string; method?: string }): void {
    const message = error instanceof Error ? error.message : String(error)
    const stack = error instanceof Error ? error.stack : undefined
    
    const report = this.createReport({
      type: 'api',
      message,
      stack,
      severity: ErrorSeverity.MEDIUM,
      context: {
        requestUrl: requestInfo?.url,
        requestMethod: requestInfo?.method
      }
    })
    
    this.addToQueue(report)
  }
  
  /**
   * 设置额外上下文
   */
  setExtraContext(context: Record<string, unknown>): void {
    this.config.extraContext = {
      ...this.config.extraContext,
      ...context
    }
  }
  
  /**
   * 立即发送队列中的错误
   */
  flush(): Promise<void> {
    return this.sendBatch()
  }
  
  /**
   * 获取队列长度
   */
  getQueueLength(): number {
    return this.queue.length
  }
  
  /**
   * 清空队列
   */
  clearQueue(): void {
    this.queue = []
  }
  
  // ==================== 私有方法 ====================
  
  /**
   * 设置全局错误处理器
   */
  private setupGlobalErrorHandler(): void {
    window.addEventListener('error', (event: ErrorEvent) => {
      // 忽略资源加载错误（由 setupResourceErrorHandler 处理）
      if (event.target && (event.target as HTMLElement).tagName) {
        return
      }
      
      const report = this.createReport({
        type: 'javascript',
        message: event.message,
        stack: event.error?.stack,
        source: event.filename,
        lineno: event.lineno,
        colno: event.colno,
        severity: ErrorSeverity.HIGH
      })
      
      this.addToQueue(report)
    })
  }
  
  /**
   * 设置未处理 Promise 拒绝处理器
   */
  private setupUnhandledRejectionHandler(): void {
    window.addEventListener('unhandledrejection', (event: PromiseRejectionEvent) => {
      const reason = event.reason
      const message = reason instanceof Error ? reason.message : String(reason)
      const stack = reason instanceof Error ? reason.stack : undefined
      
      const report = this.createReport({
        type: 'promise',
        message: `Unhandled Promise Rejection: ${message}`,
        stack,
        severity: ErrorSeverity.HIGH
      })
      
      this.addToQueue(report)
    })
  }
  
  /**
   * 设置资源加载错误处理器
   */
  private setupResourceErrorHandler(): void {
    window.addEventListener('error', (event: Event) => {
      const target = event.target as HTMLElement
      
      // 只处理资源加载错误
      if (!target || !target.tagName) {
        return
      }
      
      const tagName = target.tagName.toLowerCase()
      const resourceUrl = (target as HTMLImageElement | HTMLScriptElement).src || 
                         (target as HTMLLinkElement).href
      
      if (!resourceUrl) {
        return
      }
      
      const report = this.createReport({
        type: 'resource',
        message: `Failed to load ${tagName}: ${resourceUrl}`,
        source: resourceUrl,
        severity: ErrorSeverity.LOW
      })
      
      this.addToQueue(report)
    }, true) // 使用捕获阶段
  }
  
  /**
   * 捕获 Vue 组件错误
   */
  private captureVueError(
    err: unknown, 
    instance: ComponentPublicInstance | null, 
    info: string
  ): void {
    const error = err instanceof Error ? err : new Error(String(err))
    
    const componentInfo: VueComponentInfo = {
      name: instance?.$options?.name || instance?.$options?.__name || 'Anonymous',
      hook: info
    }
    
    // 安全地获取 props
    if (instance?.$props) {
      try {
        componentInfo.props = JSON.parse(JSON.stringify(instance.$props))
      } catch {
        // 忽略序列化错误
      }
    }
    
    const report = this.createReport({
      type: 'vue',
      message: error.message,
      stack: error.stack,
      severity: ErrorSeverity.HIGH,
      componentInfo
    })
    
    this.addToQueue(report)
  }
  
  /**
   * 创建错误报告
   */
  private createReport(params: {
    type: ErrorReportType
    message: string
    stack?: string
    source?: string
    lineno?: number
    colno?: number
    severity: ErrorSeverity
    componentInfo?: VueComponentInfo
    context?: Record<string, unknown>
  }): ErrorReport {
    return {
      traceId: generateTraceId(),
      type: params.type,
      message: params.message,
      stack: params.stack,
      source: params.source,
      lineno: params.lineno,
      colno: params.colno,
      timestamp: Date.now(),
      url: typeof window !== 'undefined' ? window.location.href : '',
      userAgent: typeof navigator !== 'undefined' ? navigator.userAgent : '',
      severity: params.severity,
      componentInfo: params.componentInfo,
      context: {
        ...this.config.extraContext,
        ...params.context
      }
    }
  }
  
  /**
   * 添加到队列
   */
  private addToQueue(report: ErrorReport): void {
    // 检查是否应该忽略
    if (this.shouldIgnore(report.message)) {
      return
    }
    
    // 采样检查
    if (!this.shouldSample()) {
      return
    }
    
    // 去重检查
    const errorKey = this.getErrorKey(report)
    if (this.reportedErrors.has(errorKey)) {
      return
    }
    this.reportedErrors.add(errorKey)
    
    // 限制去重集合大小
    if (this.reportedErrors.size > 1000) {
      const iterator = this.reportedErrors.values()
      for (let i = 0; i < 500; i++) {
        const value = iterator.next().value
        if (value) {
          this.reportedErrors.delete(value)
        }
      }
    }
    
    // 添加到队列
    this.queue.push(report)
    
    // 队列满时立即发送
    if (this.queue.length >= this.config.maxQueueSize) {
      this.sendBatch()
    }
    
    logger.debug('[ErrorReporter] 错误已加入队列', { traceId: report.traceId, type: report.type })
  }
  
  /**
   * 检查是否应该忽略错误
   */
  private shouldIgnore(message: string): boolean {
    return this.config.ignorePatterns.some(pattern => {
      if (typeof pattern === 'string') {
        return message.includes(pattern)
      }
      return pattern.test(message)
    })
  }
  
  /**
   * 采样检查
   */
  private shouldSample(): boolean {
    return Math.random() < this.config.sampleRate
  }
  
  /**
   * 获取错误唯一键（用于去重）
   */
  private getErrorKey(report: ErrorReport): string {
    return `${report.type}:${report.message}:${report.source || ''}:${report.lineno || ''}`
  }
  
  /**
   * 启动批量上报定时器
   */
  private startBatchTimer(): void {
    if (this.batchTimer) {
      clearInterval(this.batchTimer)
    }
    
    this.batchTimer = setInterval(() => {
      if (this.queue.length > 0) {
        this.sendBatch()
      }
    }, this.config.batchInterval)
  }
  
  /**
   * 发送批量错误报告
   */
  private async sendBatch(): Promise<void> {
    if (this.queue.length === 0) {
      return
    }
    
    const batch = this.queue.splice(0, this.config.maxQueueSize)
    
    try {
      const response = await fetch(this.config.reportEndpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          errors: batch,
          meta: {
            batchSize: batch.length,
            timestamp: Date.now()
          }
        })
      })
      
      if (!response.ok) {
        // 上报失败，将错误放回队列
        this.queue.unshift(...batch)
        logger.warn('[ErrorReporter] 错误上报失败', { status: response.status })
      } else {
        logger.debug('[ErrorReporter] 错误上报成功', { count: batch.length })
      }
    } catch (error) {
      // 网络错误，将错误放回队列
      this.queue.unshift(...batch)
      logger.warn('[ErrorReporter] 错误上报网络错误', error)
    }
  }
  
  /**
   * 销毁错误上报器
   */
  destroy(): void {
    if (this.batchTimer) {
      clearInterval(this.batchTimer)
      this.batchTimer = null
    }
    
    // 发送剩余的错误
    if (this.queue.length > 0) {
      this.sendBatch()
    }
    
    this.initialized = false
    logger.info('[ErrorReporter] 错误上报器已销毁')
  }
  
  /**
   * 重置错误上报器（仅用于测试）
   */
  reset(): void {
    if (this.batchTimer) {
      clearInterval(this.batchTimer)
      this.batchTimer = null
    }
    this.queue = []
    this.reportedErrors.clear()
    this.initialized = false
    this.config = DEFAULT_CONFIG
  }
}

// ==================== 导出 ====================

/**
 * 错误上报器单例
 */
export const errorReporter = new ErrorReporter()

/**
 * 便捷函数：初始化错误上报器
 */
export function initErrorReporter(config?: Partial<ErrorReporterConfig>): void {
  errorReporter.init(config)
}

/**
 * 便捷函数：手动上报错误
 */
export function reportError(error: Error | string, context?: Record<string, unknown>): void {
  errorReporter.captureError(error, context)
}

/**
 * 便捷函数：上报 API 错误
 */
export function reportApiError(error: unknown, requestInfo?: { url?: string; method?: string }): void {
  errorReporter.captureApiError(error, requestInfo)
}

/**
 * Vue 插件：安装错误上报器
 */
export const ErrorReporterPlugin = {
  install(app: App, config?: Partial<ErrorReporterConfig>): void {
    // 初始化错误上报器
    errorReporter.init(config)
    
    // 安装 Vue 错误处理器
    errorReporter.installVueErrorHandler(app)
    
    // 注入到全局属性
    app.config.globalProperties.$errorReporter = errorReporter
  }
}

// 声明 Vue 全局属性类型
declare module '@vue/runtime-core' {
  interface ComponentCustomProperties {
    $errorReporter: ErrorReporter
  }
}

/**
 * 工具函数统一导出
 */

// 格式化
export * from './format'

// 日志
export { logger } from './logger'

// 验证
export * from './validation'

// 存储
export * from './storage'

// 防抖节流
export * from './debounce'

// 性能监控
export { perfMonitor, withPerformance } from './performance'

// 剪贴板
export * from './copy'

// 下载
export * from './download'

// 消息
export { message, initMessage, dialog, initDialog } from './message'

// 错误处理
export { handleApiError } from './error'
export { 
  errorHandler, 
  handleError, 
  getUserFriendlyMessage, 
  getErrorSuggestions,
  generateTraceId,
  isValidTraceId,
  extractTimestampFromTraceId,
  formatTraceIdForDisplay,
  ErrorType,
  ErrorSeverity,
  type ErrorInfo,
  type ApiError
} from './errorHandler'

// 异步组件
export {
  createAsyncComponent,
  createRouteComponent,
  preloadComponent,
  preloadComponents,
  type AsyncComponentOptions
} from './asyncComponent'

/**
 * 增强日志工具
 * 支持日志级别、分组、性能追踪、远程上报
 */

type LogLevel = 'debug' | 'info' | 'warn' | 'error'

interface LogEntry {
  level: LogLevel
  message: string
  timestamp: string
  data?: unknown
  stack?: string
}

interface LoggerConfig {
  level: LogLevel
  enableConsole: boolean
  enableRemote: boolean
  remoteUrl?: string
  maxBufferSize: number
}

const LOG_LEVELS: Record<LogLevel, number> = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3
}

const isDev = import.meta.env.DEV

class Logger {
  private config: LoggerConfig = {
    level: isDev ? 'debug' : 'warn',
    enableConsole: true,
    enableRemote: !isDev,
    maxBufferSize: 100
  }
  
  private buffer: LogEntry[] = []
  private timers: Map<string, number> = new Map()

  /**
   * 配置日志
   */
  configure(config: Partial<LoggerConfig>) {
    this.config = { ...this.config, ...config }
  }

  /**
   * 判断是否应该输出
   */
  private shouldLog(level: LogLevel): boolean {
    return LOG_LEVELS[level] >= LOG_LEVELS[this.config.level]
  }

  /**
   * 格式化消息
   */
  private format(level: LogLevel, message: string): string {
    const time = new Date().toLocaleTimeString('zh-CN', { hour12: false })
    const levelTag = level.toUpperCase().padEnd(5)
    return `[${time}] [${levelTag}] ${message}`
  }

  /**
   * 输出日志
   */
  private log(level: LogLevel, message: string, data?: unknown) {
    if (!this.shouldLog(level)) return

    const entry: LogEntry = {
      level,
      message,
      timestamp: new Date().toISOString(),
      data
    }

    // 控制台输出
    if (this.config.enableConsole) {
      const formatted = this.format(level, message)
      const style = this.getStyle(level)
      
      switch (level) {
        case 'debug':
          console.debug(`%c${formatted}`, style, data ?? '')
          break
        case 'info':
          console.info(`%c${formatted}`, style, data ?? '')
          break
        case 'warn':
          console.warn(`%c${formatted}`, style, data ?? '')
          break
        case 'error':
          console.error(`%c${formatted}`, style, data ?? '')
          break
      }
    }

    // 缓存日志
    this.buffer.push(entry)
    if (this.buffer.length > this.config.maxBufferSize) {
      this.buffer.shift()
    }

    // 远程上报（仅错误）
    if (this.config.enableRemote && level === 'error') {
      this.sendToRemote(entry)
    }
  }

  /**
   * 获取样式
   */
  private getStyle(level: LogLevel): string {
    const styles: Record<LogLevel, string> = {
      debug: 'color: #909399',
      info: 'color: #409EFF',
      warn: 'color: #E6A23C',
      error: 'color: #F56C6C; font-weight: bold'
    }
    return styles[level]
  }

  /**
   * 发送到远程
   */
  private async sendToRemote(entry: LogEntry) {
    if (!this.config.remoteUrl) return
    
    try {
      await fetch(this.config.remoteUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(entry)
      })
    } catch {
      // 静默失败
    }
  }

  // ==================== 公共方法 ====================

  debug(message: string, data?: unknown) {
    this.log('debug', message, data)
  }

  info(message: string, data?: unknown) {
    this.log('info', message, data)
  }

  warn(message: string, data?: unknown) {
    this.log('warn', message, data)
  }

  error(message: string, error?: unknown) {
    const data = error instanceof Error 
      ? { message: error.message, stack: error.stack }
      : error
    this.log('error', message, data)
  }

  /**
   * 分组日志
   */
  group(label: string) {
    if (this.config.enableConsole && isDev) {
      console.group(`📁 ${label}`)
    }
  }

  groupEnd() {
    if (this.config.enableConsole && isDev) {
      console.groupEnd()
    }
  }

  /**
   * 性能计时开始
   */
  time(label: string) {
    this.timers.set(label, performance.now())
  }

  /**
   * 性能计时结束
   */
  timeEnd(label: string) {
    const start = this.timers.get(label)
    if (start) {
      const duration = performance.now() - start
      this.debug(`⏱️ ${label}: ${duration.toFixed(2)}ms`)
      this.timers.delete(label)
    }
  }

  /**
   * 表格输出
   */
  table(data: unknown[]) {
    if (this.config.enableConsole && isDev) {
      console.table(data)
    }
  }

  /**
   * 获取日志缓存
   */
  getBuffer(): LogEntry[] {
    return [...this.buffer]
  }

  /**
   * 清空缓存
   */
  clearBuffer() {
    this.buffer = []
  }
}

export const logger = new Logger()

// 全局错误捕获
if (typeof window !== 'undefined') {
  window.addEventListener('error', event => {
    logger.error('Uncaught Error', {
      message: event.message,
      filename: event.filename,
      lineno: event.lineno,
      colno: event.colno
    })
  })

  window.addEventListener('unhandledrejection', event => {
    logger.error('Unhandled Promise Rejection', event.reason)
  })
}

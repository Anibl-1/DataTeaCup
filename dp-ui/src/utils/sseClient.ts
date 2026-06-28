/**
 * SSE 流式客户端工具
 * 用于消费后端 AI 流式响应
 * 
 * 验收标准:
 * - 1.2: 逐字动画效果实时渲染
 * - 1.4: 停止生成后保留已接收内容
 * - 1.7: 连接中断提示并提供重试选项
 * - 1.8: 心跳保活
 */

import { logger } from '@/utils/logger'

// ==================== 类型定义 ====================

/** SSE 事件类型 */
export type SseEventType = 'start' | 'token' | 'complete' | 'error' | 'heartbeat' | 'abort'

/** SSE 开始事件数据 */
export interface SseStartEvent {
  streamId: string
}

/** SSE 完成事件数据 */
export interface SseCompleteEvent {
  message: string
}

/** SSE 错误事件数据 */
export interface SseErrorEvent {
  error: string
}

/** SSE 心跳事件数据 */
export interface SseHeartbeatEvent {
  timestamp: number
}

/** SSE 中断事件数据 */
export interface SseAbortEvent {
  message: string
}

/** SSE 连接状态 */
export type SseConnectionState = 'connecting' | 'connected' | 'disconnected' | 'error' | 'aborted'

/** SSE 回调选项 */
export interface SseCallbacks {
  /** 流开始时调用，返回 streamId */
  onStart?: (streamId: string) => void
  /** 接收到 token 时调用 */
  onToken?: (token: string) => void
  /** 流完成时调用，返回完整内容 */
  onComplete?: (fullContent: string) => void
  /** 发生错误时调用 */
  onError?: (error: Error) => void
  /** 收到心跳时调用 */
  onHeartbeat?: (timestamp: number) => void
  /** 连接状态变化时调用 */
  onStateChange?: (state: SseConnectionState) => void
  /** 流被中断时调用 */
  onAbort?: () => void
}

/** SSE 连接选项 */
export interface SseConnectionOptions extends SseCallbacks {
  /** 连接超时时间（毫秒），默认 30000 */
  timeout?: number
  /** 是否自动重连，默认 false */
  autoReconnect?: boolean
  /** 最大重连次数，默认 3 */
  maxReconnectAttempts?: number
  /** 重连延迟（毫秒），默认 1000 */
  reconnectDelay?: number
}

/** SSE 连接实例 */
export interface SseConnection {
  /** 流 ID（连接成功后可用） */
  streamId: string | null
  /** 当前连接状态 */
  state: SseConnectionState
  /** 已接收的完整内容 */
  content: string
  /** 中断流 */
  abort: () => void
  /** 关闭连接 */
  close: () => void
}

/** 流式对话参数 */
export interface StreamChatParams {
  message: string
  context?: string
  sessionId?: string
}

/** 流式 SQL 生成参数 */
export interface StreamSqlParams {
  naturalLanguage: string
  dataSourceId: number
  tableName?: string
}

/** 流式图表生成参数 */
export interface StreamChartParams {
  requirement: string
  dataSourceId: number
}

// ==================== 常量配置 ====================

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
const DEFAULT_TIMEOUT = 30000
const DEFAULT_MAX_RECONNECT_ATTEMPTS = 3
const DEFAULT_RECONNECT_DELAY = 1000

// 活跃的 SSE 连接管理
const activeConnections = new Map<string, EventSource>()

// ==================== 核心函数 ====================

/**
 * 创建 SSE 连接
 * @param url 完整的 SSE 端点 URL
 * @param options 连接选项和回调
 * @returns SSE 连接实例
 */
export function createSseConnection(
  url: string,
  options: SseConnectionOptions = {}
): SseConnection {
  const {
    onStart,
    onToken,
    onComplete,
    onError,
    onHeartbeat,
    onStateChange,
    onAbort,
    timeout = DEFAULT_TIMEOUT,
    autoReconnect = false,
    maxReconnectAttempts = DEFAULT_MAX_RECONNECT_ATTEMPTS,
    reconnectDelay = DEFAULT_RECONNECT_DELAY
  } = options

  let eventSource: EventSource | null = null
  let streamId: string | null = null
  let content = ''
  let state: SseConnectionState = 'connecting'
  let reconnectAttempts = 0
  let timeoutId: ReturnType<typeof setTimeout> | null = null
  let isAborted = false

  const updateState = (newState: SseConnectionState) => {
    state = newState
    onStateChange?.(newState)
  }

  const cleanup = () => {
    if (timeoutId) {
      clearTimeout(timeoutId)
      timeoutId = null
    }
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    if (streamId) {
      activeConnections.delete(streamId)
    }
  }

  const connect = () => {
    try {
      // 添加认证 token
      const token = localStorage.getItem('token')
      const urlWithAuth = token 
        ? `${url}${url.includes('?') ? '&' : '?'}token=${encodeURIComponent(token)}`
        : url

      eventSource = new EventSource(urlWithAuth)
      
      // 设置连接超时
      timeoutId = setTimeout(() => {
        if (state === 'connecting') {
          logger.warn('[SSE] 连接超时', { url })
          cleanup()
          updateState('error')
          onError?.(new Error('连接超时'))
        }
      }, timeout)

      // 开始事件
      eventSource.addEventListener('start', (event: MessageEvent) => {
        if (timeoutId) {
          clearTimeout(timeoutId)
          timeoutId = null
        }
        
        try {
          const data: SseStartEvent = JSON.parse(event.data)
          streamId = data.streamId
          activeConnections.set(streamId, eventSource!)
          updateState('connected')
          logger.debug('[SSE] 流开始', { streamId })
          onStart?.(streamId)
        } catch (e) {
          logger.error('[SSE] 解析 start 事件失败', e)
        }
      })

      // Token 事件
      eventSource.addEventListener('token', (event: MessageEvent) => {
        if (isAborted) return
        const token = event.data
        content += token
        onToken?.(token)
      })

      // 完成事件
      eventSource.addEventListener('complete', (_event: MessageEvent) => {
        logger.debug('[SSE] 流完成', { streamId, contentLength: content.length })
        cleanup()
        updateState('disconnected')
        onComplete?.(content)
      })

      // 错误事件（服务端发送）
      eventSource.addEventListener('error', (event: MessageEvent) => {
        // 区分 EventSource 原生错误和服务端发送的错误事件
        if (event.data) {
          try {
            const data: SseErrorEvent = JSON.parse(event.data)
            logger.error('[SSE] 服务端错误', { error: data.error })
            cleanup()
            updateState('error')
            onError?.(new Error(data.error))
          } catch {
            // 非 JSON 格式的错误
            handleConnectionError()
          }
        } else {
          handleConnectionError()
        }
      })

      // 心跳事件
      eventSource.addEventListener('heartbeat', (event: MessageEvent) => {
        try {
          const data: SseHeartbeatEvent = JSON.parse(event.data)
          logger.debug('[SSE] 心跳', { timestamp: data.timestamp })
          onHeartbeat?.(data.timestamp)
        } catch (e) {
          logger.debug('[SSE] 解析心跳事件失败', e)
        }
      })

      // 中断事件
      eventSource.addEventListener('abort', (_event: MessageEvent) => {
        logger.info('[SSE] 流被中断', { streamId })
        isAborted = true
        cleanup()
        updateState('aborted')
        onAbort?.()
      })

      // EventSource 原生错误处理
      eventSource.onerror = () => {
        handleConnectionError()
      }

    } catch (e) {
      logger.error('[SSE] 创建连接失败', e)
      updateState('error')
      onError?.(e instanceof Error ? e : new Error('创建连接失败'))
    }
  }

  const handleConnectionError = () => {
    if (isAborted) return
    
    logger.warn('[SSE] 连接错误', { streamId, reconnectAttempts })
    
    if (autoReconnect && reconnectAttempts < maxReconnectAttempts) {
      reconnectAttempts++
      logger.info(`[SSE] 尝试重连 (${reconnectAttempts}/${maxReconnectAttempts})`)
      cleanup()
      updateState('connecting')
      setTimeout(connect, reconnectDelay * reconnectAttempts)
    } else {
      cleanup()
      updateState('error')
      onError?.(new Error('连接中断'))
    }
  }

  // 启动连接
  connect()

  // 返回连接实例
  return {
    get streamId() { return streamId },
    get state() { return state },
    get content() { return content },
    
    abort: () => {
      if (streamId && !isAborted) {
        isAborted = true
        abortStream(streamId).catch(e => {
          logger.warn('[SSE] 中断请求失败', e)
        })
      }
      cleanup()
      updateState('aborted')
      onAbort?.()
    },
    
    close: () => {
      cleanup()
      if (state !== 'error' && state !== 'aborted') {
        updateState('disconnected')
      }
    }
  }
}

/**
 * 流式对话
 * @param params 对话参数
 * @param callbacks 回调函数
 * @returns SSE 连接实例
 */
export function streamChat(
  params: StreamChatParams,
  callbacks: SseCallbacks = {}
): SseConnection {
  const { message, context, sessionId } = params
  
  const queryParams = new URLSearchParams()
  queryParams.set('message', message)
  if (context) queryParams.set('context', context)
  if (sessionId) queryParams.set('sessionId', sessionId)
  
  const url = `${API_BASE_URL}/ai/stream/chat?${queryParams.toString()}`
  
  logger.info('[SSE] 开始流式对话', { messageLength: message.length })
  
  return createSseConnection(url, callbacks)
}

/**
 * 流式 SQL 生成
 * @param params SQL 生成参数
 * @param callbacks 回调函数
 * @returns SSE 连接实例
 */
export function streamSql(
  params: StreamSqlParams,
  callbacks: SseCallbacks = {}
): SseConnection {
  const { naturalLanguage, dataSourceId, tableName } = params
  
  const queryParams = new URLSearchParams()
  queryParams.set('naturalLanguage', naturalLanguage)
  queryParams.set('dataSourceId', dataSourceId.toString())
  if (tableName) queryParams.set('tableName', tableName)
  
  const url = `${API_BASE_URL}/ai/stream/sql?${queryParams.toString()}`
  
  logger.info('[SSE] 开始流式 SQL 生成', { dataSourceId, tableName })
  
  return createSseConnection(url, callbacks)
}

/**
 * 流式图表生成
 * @param params 图表生成参数
 * @param callbacks 回调函数
 * @returns SSE 连接实例
 */
export function streamChart(
  params: StreamChartParams,
  callbacks: SseCallbacks = {}
): SseConnection {
  const { requirement, dataSourceId } = params
  
  const queryParams = new URLSearchParams()
  queryParams.set('requirement', requirement)
  queryParams.set('dataSourceId', dataSourceId.toString())
  
  const url = `${API_BASE_URL}/ai/stream/chart?${queryParams.toString()}`
  
  logger.info('[SSE] 开始流式图表生成', { dataSourceId })
  
  return createSseConnection(url, callbacks)
}

/**
 * 中断流式生成
 * @param streamId 流 ID
 * @returns Promise
 */
export async function abortStream(streamId: string): Promise<void> {
  logger.info('[SSE] 请求中断流', { streamId })
  
  // 关闭本地 EventSource
  const eventSource = activeConnections.get(streamId)
  if (eventSource) {
    eventSource.close()
    activeConnections.delete(streamId)
  }
  
  // 通知后端中断
  const token = localStorage.getItem('token')
  const headers: HeadersInit = {
    'Content-Type': 'application/json'
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  
  const response = await fetch(`${API_BASE_URL}/ai/stream/abort/${streamId}`, {
    method: 'POST',
    headers
  })
  
  if (!response.ok) {
    throw new Error(`中断失败: ${response.status}`)
  }
}

/**
 * 获取当前活跃的流数量
 * @returns 活跃流数量
 */
export function getActiveStreamCount(): number {
  return activeConnections.size
}

/**
 * 关闭所有活跃的 SSE 连接
 */
export function closeAllConnections(): void {
  logger.info('[SSE] 关闭所有连接', { count: activeConnections.size })
  
  activeConnections.forEach((eventSource, streamId) => {
    eventSource.close()
    // 尝试通知后端（不等待结果）
    abortStream(streamId).catch(() => {})
  })
  
  activeConnections.clear()
}

// 类型已在文件顶部通过 export interface/type 导出

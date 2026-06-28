/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, onUnmounted } from 'vue'
import { serializeMessage, deserializeMessage } from '@/utils/messageSerializer'
import type { WsMessage } from '@/utils/messageSerializer'

/**
 * 重连配置接口
 */
export interface ReconnectConfig {
  maxRetries: number       // 默认 5
  baseDelay: number        // 默认 1000ms
  maxDelay: number         // 默认 30000ms
  backoffMultiplier: number // 默认 2
}

/** 默认重连配置 */
const DEFAULT_RECONNECT_CONFIG: ReconnectConfig = {
  maxRetries: 5,
  baseDelay: 1000,
  maxDelay: 30000,
  backoffMultiplier: 2,
}

/** WebSocket 连接状态类型 */
export type ConnectionState = 'disconnected' | 'connecting' | 'connected' | 'reconnecting'

/**
 * 计算重连延迟（纯函数，便于测试）
 *
 * 公式: min(baseDelay × backoffMultiplier^retryCount, maxDelay)
 */
export function calculateReconnectDelay(retryCount: number, config: ReconnectConfig): number {
  const delay = config.baseDelay * Math.pow(config.backoffMultiplier, retryCount)
  return Math.min(delay, config.maxDelay)
}

/**
 * 聊天 WebSocket composable
 *
 * 不主动探测后端——由调用方确认后端在线后再调用 connect()。
 * 后端不在线时不调用 connect()，零控制台错误。
 * 增强：支持指数退避自动重连和连接状态监控。
 */
export function useChatWebSocket(reconnectConfig?: Partial<ReconnectConfig>) {
  const config: ReconnectConfig = { ...DEFAULT_RECONNECT_CONFIG, ...reconnectConfig }

  const connected = ref(false)
  const error = ref<string | null>(null)
  const reconnecting = ref(false)
  const retryCount = ref(0)
  const connectionState = ref<ConnectionState>('disconnected')

  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let lastToken: string | null = null
  const messageHandlers = new Map<string, Array<(payload: any) => void>>()

  function getWsUrl(token: string): string {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host // includes port (e.g. localhost:3000)
    const contextPath = import.meta.env['VITE_API_CONTEXT'] || '/api'
    return `${protocol}//${host}${contextPath}/ws/chat?token=${encodeURIComponent(token)}`
  }

  function clearReconnectTimer() {
    if (reconnectTimer !== null) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  function resetReconnectState() {
    clearReconnectTimer()
    reconnecting.value = false
    retryCount.value = 0
  }

  function scheduleReconnect() {
    if (!lastToken) return
    if (retryCount.value >= config.maxRetries) {
      // 超过最大重试次数，停止重连
      reconnecting.value = false
      connectionState.value = 'disconnected'
      return
    }

    reconnecting.value = true
    connectionState.value = 'reconnecting'
    const delay = calculateReconnectDelay(retryCount.value, config)
    retryCount.value++

    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      if (lastToken) {
        attemptConnect(lastToken)
      }
    }, delay)
  }

  function attemptConnect(token: string) {
    if (ws && (ws.readyState === WebSocket.CONNECTING || ws.readyState === WebSocket.OPEN)) {
      return
    }
    error.value = null
    connectionState.value = reconnecting.value ? 'reconnecting' : 'connecting'
    const url = getWsUrl(token)
    try {
      ws = new WebSocket(url)
    } catch {
      scheduleReconnect()
      return
    }

    ws.onopen = () => {
      connected.value = true
      error.value = null
      connectionState.value = 'connected'
      resetReconnectState()
    }

    ws.onmessage = (event) => {
      try {
        const msg = deserializeMessage(event.data as string)
        const handlers = messageHandlers.get(msg.type)
        if (handlers) handlers.forEach(h => h(msg.payload))
      } catch { /* 忽略 */ }
    }

    ws.onclose = () => {
      connected.value = false
      if (lastToken) {
        // 非主动断开（lastToken 仍存在），尝试重连
        scheduleReconnect()
      } else {
        connectionState.value = 'disconnected'
      }
    }

    ws.onerror = () => {
      connected.value = false
      error.value = 'WebSocket connection error'
    }
  }

  function connect(token: string) {
    lastToken = token
    resetReconnectState()
    attemptConnect(token)
  }

  function disconnect() {
    lastToken = null
    resetReconnectState()
    if (ws) { ws.close(); ws = null }
    connected.value = false
    connectionState.value = 'disconnected'
  }

  function send(type: string, payload: any) {
    if (!ws || ws.readyState !== WebSocket.OPEN) return
    ws.send(serializeMessage({ type, payload, timestamp: Date.now() } as WsMessage))
  }

  function onMessage(type: string, handler: (payload: any) => void) {
    if (!messageHandlers.has(type)) messageHandlers.set(type, [])
    messageHandlers.get(type)!.push(handler)
  }

  onUnmounted(() => { disconnect() })

  return {
    connected,
    error,
    reconnecting,
    retryCount,
    connectionState,
    connect,
    disconnect,
    send,
    onMessage,
  }
}

/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, onUnmounted } from 'vue'

/**
 * 监控 WebSocket composable
 * 使用原生 WebSocket 连接后端 STOMP 端点
 * 支持自动重连、订阅监控数据频道
 */

export interface MonitorData {
  health: Record<string, any> | null
  jvm: Record<string, any> | null
  alert: Record<string, any> | null
}

export function useMonitorWebSocket() {
  const connected = ref(false)
  const data = ref<MonitorData>({
    health: null,
    jvm: null,
    alert: null
  })
  const error = ref<string | null>(null)

  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let stompConnected = false
  let subscriptionId = 0

  const getWsUrl = (): string => {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host // includes port (e.g. localhost:3000)
    const contextPath = import.meta.env['VITE_API_CONTEXT'] || '/api'
    return `${protocol}//${host}${contextPath}/ws`
  }

  // ==================== STOMP over WebSocket ====================

  const stompSend = (command: string, headers: Record<string, string> = {}, body = '') => {
    if (!ws || ws.readyState !== WebSocket.OPEN) return
    let frame = command + '\n'
    for (const [key, val] of Object.entries(headers)) {
      frame += `${key}:${val}\n`
    }
    frame += '\n' + body + '\0'
    ws.send(frame)
  }

  const parseStompFrame = (raw: string): { command: string; headers: Record<string, string>; body: string } | null => {
    const nullIdx = raw.indexOf('\0')
    const content = nullIdx >= 0 ? raw.substring(0, nullIdx) : raw
    const parts = content.split('\n\n')
    if (parts.length < 1) return null

    const headerSection = parts[0]!
    const body = parts.length > 1 ? parts.slice(1).join('\n\n') : ''
    const lines = headerSection.split('\n')
    const command = lines[0] ?? ''
    const headers: Record<string, string> = {}

    for (let i = 1; i < lines.length; i++) {
      const line = lines[i]!
      const colonIdx = line.indexOf(':')
      if (colonIdx > 0) {
        headers[line.substring(0, colonIdx)] = line.substring(colonIdx + 1)
      }
    }

    return { command, headers, body }
  }

  const subscribe = (destination: string) => {
    const id = `sub-${subscriptionId++}`
    stompSend('SUBSCRIBE', { id, destination })
    return id
  }

  // ==================== Connection Management ====================

  const connect = () => {
    if (ws && (ws.readyState === WebSocket.CONNECTING || ws.readyState === WebSocket.OPEN)) {
      return
    }

    error.value = null
    const url = getWsUrl()

    try {
      ws = new WebSocket(url)
    } catch (e) {
      error.value = '无法创建WebSocket连接'
      return
    }

    ws.onopen = () => {
      // 发送 STOMP CONNECT 帧
      stompSend('CONNECT', {
        'accept-version': '1.1,1.2',
        'heart-beat': '10000,10000'
      })
    }

    ws.onmessage = (event) => {
      const frame = parseStompFrame(event.data as string)
      if (!frame) return

      switch (frame.command) {
        case 'CONNECTED':
          stompConnected = true
          connected.value = true
          error.value = null
          // 订阅监控频道
          subscribe('/topic/monitor/health')
          subscribe('/topic/monitor/jvm')
          subscribe('/topic/monitor/alert')
          break

        case 'MESSAGE': {
          const destination = frame.headers['destination'] || ''
          try {
            const payload = JSON.parse(frame.body)
            if (destination.endsWith('/health')) {
              data.value.health = payload
            } else if (destination.endsWith('/jvm')) {
              data.value.jvm = payload
            } else if (destination.endsWith('/alert')) {
              data.value.alert = payload
            }
          } catch {
            // 非JSON消息忽略
          }
          break
        }

        case 'ERROR':
          error.value = frame.body || 'STOMP错误'
          break

        default:
          break
      }
    }

    ws.onclose = () => {
      connected.value = false
      stompConnected = false
      // 不自动重连，避免后端不在线时控制台刷屏
    }

    ws.onerror = () => {
      error.value = 'WebSocket连接错误'
      connected.value = false
    }
  }

  const disconnect = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (ws) {
      if (stompConnected) {
        stompSend('DISCONNECT')
      }
      ws.close()
      ws = null
    }
    connected.value = false
    stompConnected = false
  }

  // 组件卸载时断开连接
  onUnmounted(() => {
    disconnect()
  })

  return {
    connected,
    data,
    error,
    connect,
    disconnect
  }
}

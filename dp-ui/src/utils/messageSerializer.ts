/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 聊天消息序列化模块
 * 负责 WsMessage 对象与 JSON 字符串之间的转换
 */

/**
 * WebSocket 消息接口
 */
export interface WsMessage {
  /** 消息类型（如 chat、status） */
  type: string
  /** 消息载荷 */
  payload: any
  /** 时间戳（毫秒） */
  timestamp: number
}

/**
 * 将 WsMessage 对象序列化为 JSON 字符串
 */
export function serializeMessage(msg: WsMessage): string {
  return JSON.stringify(msg)
}

/**
 * 将 JSON 字符串反序列化为 WsMessage 对象
 * @throws Error 当 JSON 格式无效或缺少必要字段时
 */
export function deserializeMessage(json: string): WsMessage {
  let parsed: any
  try {
    parsed = JSON.parse(json)
  } catch (e) {
    throw new Error(`Invalid JSON: ${(e as Error).message}`)
  }
  if (!parsed.type || parsed.timestamp === undefined) {
    throw new Error('Invalid message format: missing required fields')
  }
  return parsed as WsMessage
}

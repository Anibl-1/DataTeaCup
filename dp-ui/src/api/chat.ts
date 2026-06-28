/* eslint-disable @typescript-eslint/no-explicit-any */
import request from './request'
import type { ApiResponse } from '@/types/api'
import type {
  Conversation,
  ChatMessage,
  CreateConversationRequest,
  SendMessageRequest,
  OnlineUser
} from '@/types/chat'

/** 静默请求配置：后端不可达时不弹错误提示 */
const SILENT = { __silent: true } as any

/**
 * 获取会话列表
 */
export const getConversationList = () => {
  return request.get<ApiResponse<Conversation[]>>('/chat/conversations', SILENT)
}

/**
 * 获取消息历史（游标分页）
 */
export const getMessages = (conversationId: number, cursor?: number) => {
  return request.get<ApiResponse<ChatMessage[]>>(
    `/chat/conversations/${conversationId}/messages`,
    { params: { cursor }, ...SILENT }
  )
}

/**
 * 创建私聊会话
 */
export const createPrivateConversation = (targetUserId: number) => {
  return request.post<ApiResponse<Conversation>>('/chat/conversations/private', { targetUserId }, SILENT)
}

/**
 * 创建群组会话
 */
export const createGroupConversation = (name: string, memberIds: number[]) => {
  return request.post<ApiResponse<Conversation>>('/chat/conversations/group', { name, memberIds }, SILENT)
}

/**
 * 创建会话（兼容旧调用）
 */
export const createConversation = (data: CreateConversationRequest) => {
  if (data.type === 'private') {
    return createPrivateConversation(data.memberIds[0]!)
  }
  return createGroupConversation(data.name || '群聊', data.memberIds)
}

/**
 * 发送消息（HTTP 备用通道）
 */
export const sendMessage = (data: SendMessageRequest) => {
  return request.post<ApiResponse<ChatMessage>>('/chat/messages', data, SILENT)
}

/**
 * 标记会话消息已读
 */
export const markAsRead = (conversationId: number) => {
  return request.put<ApiResponse<void>>(`/chat/conversations/${conversationId}/read`, null, SILENT)
}

/**
 * 获取在线用户列表
 */
export const getOnlineUsers = () => {
  return request.get<ApiResponse<OnlineUser[]>>('/chat/online-status', SILENT)
}

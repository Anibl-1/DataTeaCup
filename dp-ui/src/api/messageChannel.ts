import request from './request'
import type { ApiResponse } from '@/types/api'

export interface MessageChannel {
  id: number
  channelName: string
  channelType: 'email' | 'wecom' | 'dingtalk' | 'sms'
  config: string
  isDefault: number
  status: number
  description?: string
  createBy?: number
  createTime?: string
  updateTime?: string
}

export interface EmailConfig {
  host: string
  port: number
  username: string
  password: string
  fromName?: string
  ssl?: boolean
}

export interface WecomConfig {
  corpId: string
  agentId: string
  secret: string
  webhookUrl?: string
}

export interface DingtalkConfig {
  webhookUrl: string
  secret?: string
  agentId?: string
  appKey?: string
  appSecret?: string
}

export interface SmsConfig {
  provider: 'aliyun' | 'tencent'
  accessKey: string
  secretKey: string
  signName: string
  templateId?: string
}

/**
 * 获取所有通道配置
 */
export const getMessageChannels = () => {
  return request.get<ApiResponse<MessageChannel[]>>('/message-channel/list')
}

/**
 * 按类型获取通道配置
 */
export const getChannelsByType = (type: string) => {
  return request.get<ApiResponse<MessageChannel[]>>(`/message-channel/by-type/${type}`)
}

/**
 * 获取启用的通道（按类型分组）
 */
export const getEnabledChannels = () => {
  return request.get<ApiResponse<Record<string, MessageChannel[]>>>('/message-channel/enabled')
}

/**
 * 获取单个通道详情
 */
export const getChannelById = (id: number) => {
  return request.get<ApiResponse<MessageChannel>>(`/message-channel/${id}`)
}

/**
 * 创建通道配置
 */
export const createChannel = (data: Partial<MessageChannel>) => {
  return request.post<ApiResponse<MessageChannel>>('/message-channel', data)
}

/**
 * 更新通道配置
 */
export const updateChannel = (id: number, data: Partial<MessageChannel>) => {
  return request.put<ApiResponse<MessageChannel>>(`/message-channel/${id}`, data)
}

/**
 * 删除通道配置
 */
export const deleteChannel = (id: number) => {
  return request.delete<ApiResponse<void>>(`/message-channel/${id}`)
}

/**
 * 设置为默认通道
 */
export const setDefaultChannel = (id: number) => {
  return request.post<ApiResponse<void>>(`/message-channel/${id}/set-default`)
}

/**
 * 测试通道配置
 */
export const testChannel = (id: number, recipient: string, content?: string) => {
  return request.post<ApiResponse<string>>(`/message-channel/${id}/test`, { recipient, content })
}

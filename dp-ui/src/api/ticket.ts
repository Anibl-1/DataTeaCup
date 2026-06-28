/* eslint-disable @typescript-eslint/no-explicit-any */
import request from './request'
import type { ApiResponse } from '@/types/api'

// ==================== TypeScript 接口定义 ====================

/** 工单 */
export interface Ticket {
  id: number
  ticketNo?: string
  title: string
  description: string
  category: string
  priority: 'low' | 'medium' | 'high' | 'critical'
  status: 'open' | 'in_progress' | 'resolved' | 'closed' | 'pending' | 'processing'
  submitterId: string
  submitterName: string
  assigneeId?: string
  assigneeName?: string
  attachments?: string[]
  resolution?: string
  createTime?: string
  updateTime?: string
  createdAt?: string
  updatedAt?: string
}

/** 工单评论 */
export interface TicketComment {
  id: number
  ticketId: number
  content: string
  userId?: string
  userName?: string
  authorId?: string
  authorName?: string
  createTime?: string
  createdAt?: string
}

/** 知识库文章 */
export interface KnowledgeArticle {
  id: number
  title: string
  content: string
  category: string
  tags: string[]
  attachments?: string[]
  viewCount: number
  helpfulCount?: number
  authorId?: string
  authorName?: string
  createTime?: string
  updateTime?: string
  createdAt?: string
}

// ==================== 工单 CRUD 接口 ====================

/** 获取工单列表 */
export const getTicketList = (params: {
  status?: string
  category?: string
  priority?: string
  page?: number
  size?: number
}) => {
  return request.get<ApiResponse<Ticket[]>>('/tickets', { params })
}

/** 创建工单 */
export const createTicket = (data: Partial<Ticket>) => {
  return request.post<ApiResponse<Ticket>>('/tickets', data)
}

/** 获取工单详情 */
export const getTicketDetail = (id: number) => {
  return request.get<ApiResponse<Ticket>>(`/tickets/${id}`)
}

/** 获取我的工单 */
export const getMyTickets = (userId: string) => {
  return request.get<ApiResponse<Ticket[]>>(`/tickets/my/${userId}`)
}

/** 分配工单 */
export const assignTicket = (id: number, data: { assigneeId: string; assigneeName: string }) => {
  return request.put<ApiResponse<Ticket>>(`/tickets/${id}/assign`, data)
}

/** 更新工单状态 */
export const updateTicketStatus = (id: number, data: { status: string; resolution?: string }) => {
  return request.put<ApiResponse<Ticket>>(`/tickets/${id}/status`, data)
}

/** 撤销（删除）工单 */
export const deleteTicket = (id: number) => {
  return request.delete<ApiResponse<void>>(`/tickets/${id}`)
}

// ==================== 评论接口 ====================

/** 添加评论 */
export const addComment = (ticketId: number, data: Partial<TicketComment>) => {
  return request.post<ApiResponse<TicketComment>>(`/tickets/${ticketId}/comments`, data)
}

/** 获取工单评论列表 */
export const getComments = (ticketId: number) => {
  return request.get<ApiResponse<TicketComment[]>>(`/tickets/${ticketId}/comments`)
}

// ==================== 统计接口 ====================

/** 获取工单统计 */
export const getTicketStats = () => {
  return request.get<ApiResponse<Record<string, any>>>('/tickets/stats')
}

// ==================== 知识库接口 ====================

/** 搜索知识库文章 */
export const searchKnowledge = (params: { keyword?: string; category?: string }) => {
  return request.get<ApiResponse<KnowledgeArticle[]>>('/tickets/knowledge', { params })
}

/** 获取知识库文章详情 */
export const getKnowledgeArticle = (id: number) => {
  return request.get<ApiResponse<KnowledgeArticle>>(`/tickets/knowledge/${id}`)
}

/** 创建知识库文章 */
export const createKnowledgeArticle = (data: Partial<KnowledgeArticle>) => {
  return request.post<ApiResponse<KnowledgeArticle>>('/tickets/knowledge', data)
}

/** 获取热门文章 */
export const getPopularArticles = (topN: number = 10) => {
  return request.get<ApiResponse<KnowledgeArticle[]>>('/tickets/knowledge/popular', { params: { topN } })
}

/** 更新知识库文章 */
export const updateKnowledgeArticle = (id: number, data: Partial<KnowledgeArticle>) => {
  return request.put<ApiResponse<KnowledgeArticle>>(`/tickets/knowledge/${id}`, data)
}

/** 删除知识库文章 */
export const deleteKnowledgeArticle = (id: number) => {
  return request.delete<ApiResponse<void>>(`/tickets/knowledge/${id}`)
}

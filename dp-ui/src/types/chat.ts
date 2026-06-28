/**
 * 聊天相关类型定义
 */

/**
 * 会话成员
 */
export interface ConversationMember {
  /** 用户ID */
  userId: number
  /** 用户名（关联查询填充） */
  username?: string
  /** 昵称（关联查询填充） */
  nickname?: string
  /** 头像URL */
  avatar?: string
  /** 未读消息数 */
  unreadCount: number
  /** 加入时间 */
  joinTime: string
}

/**
 * 会话
 */
export interface Conversation {
  /** 会话ID */
  id: number
  /** 会话类型：private-私聊，group-群组 */
  type: 'private' | 'group'
  /** 会话名称 */
  name: string
  /** 头像URL */
  avatar?: string
  /** 创建时间 */
  createTime: string
  /** 最后消息时间 */
  lastMessageTime?: string
  /** 最后消息内容 */
  lastMessage?: string
  /** 未读消息数 */
  unreadCount: number
  /** 会话成员列表 */
  members?: ConversationMember[]
}

/**
 * 聊天消息
 */
export interface ChatMessage {
  /** 消息ID */
  id: number
  /** 会话ID */
  conversationId: number
  /** 发送者ID */
  senderId: number
  /** 发送者昵称（后端可能不返回，需前端补充） */
  senderName?: string
  /** 消息类型：text-文本，image-图片，file-文件 */
  contentType: 'text' | 'image' | 'file'
  /** 消息内容 */
  content: string
  /** 文件URL */
  fileUrl?: string
  /** 文件名 */
  fileName?: string
  /** 文件大小（字节） */
  fileSize?: number
  /** 发送时间 */
  sendTime: string
  /** 消息发送状态 */
  status?: 'sending' | 'sent' | 'failed'
  /** 本地临时ID，用于发送中消息的标识 */
  localId?: string
  /** 已读用户ID列表 */
  readBy?: number[]
}

/**
 * 在线用户状态（匹配后端 ChatUserStatus）
 */
export interface OnlineUser {
  /** 用户ID */
  userId: number
  /** 在线状态 */
  status: 'online' | 'offline'
  /** 最后活跃时间 */
  lastActiveTime?: string
}

/**
 * 创建会话请求
 */
export interface CreateConversationRequest {
  /** 会话类型：private-私聊，group-群组 */
  type: 'private' | 'group'
  /** 会话名称（群组时必填） */
  name?: string
  /** 成员ID列表 */
  memberIds: number[]
}

/**
 * 发送消息请求
 */
export interface SendMessageRequest {
  /** 会话ID */
  conversationId: number
  /** 消息类型：text-文本，image-图片，file-文件 */
  contentType: 'text' | 'image' | 'file'
  /** 消息内容 */
  content?: string
  /** 文件URL */
  fileUrl?: string
  /** 文件名 */
  fileName?: string
  /** 文件大小（字节） */
  fileSize?: number
}

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Conversation, ChatMessage, SendMessageRequest } from '@/types/chat'
import { getConversationList, getMessages, sendMessage as sendMessageApi, markAsRead } from '@/api/chat'
import { useUserStore } from './user'

/** 生成本地临时消息ID */
let localIdCounter = 0
function generateLocalId(): string {
  return `local_${Date.now()}_${++localIdCounter}`
}

/**
 * 纯函数：根据 localId 更新消息列表中对应消息的状态。
 * 返回新数组（不可变更新），若未找到匹配消息则返回原数组。
 */
export function updateMessageStatus(
  messages: ChatMessage[],
  localId: string,
  status: ChatMessage['status'],
  merge?: Partial<ChatMessage>
): ChatMessage[] {
  const idx = messages.findIndex(m => m.localId === localId)
  if (idx === -1) return messages
  const updated = [...messages]
  const existing = updated[idx]!
  const merged: ChatMessage = { ...existing, ...merge }
  if (status !== undefined) {
    merged.status = status
  }
  updated[idx] = merged
  return updated
}

export const useChatStore = defineStore('chat', () => {
  /** 会话列表 */
  const conversations = ref<Conversation[]>([])

  /** 当前选中的会话 ID */
  const currentConversationId = ref<number | null>(null)

  /** 当前会话的消息列表 */
  const messages = ref<ChatMessage[]>([])

  /** 聊天面板是否可见 */
  const panelVisible = ref(false)

  /** 未读消息总数 */
  const totalUnread = computed(() =>
    conversations.value.reduce((sum, c) => sum + (c.unreadCount || 0), 0)
  )

  /** 当前选中的会话对象 */
  const currentConversation = computed(() =>
    conversations.value.find(c => c.id === currentConversationId.value) || null
  )

  /** 加载会话列表（与本地状态合并，保留未同步的unreadCount变化） */
  async function loadConversations() {
    try {
      const res = await getConversationList()
      const serverList: Conversation[] = res.data || []
      // 直接使用服务端数据（服务端现在返回准确的 unreadCount、lastMessage、members）
      conversations.value = serverList
    } catch {
      // 后端不可达时静默处理
    }
  }

  /** 加载消息历史（支持游标分页），返回加载到的消息数 */
  async function loadMessages(conversationId: number, cursor?: number): Promise<number> {
    try {
      const res = await getMessages(conversationId, cursor)
      const newMessages: ChatMessage[] = res.data || []
      if (cursor) {
        // 去重：过滤掉已存在的消息
        const existingIds = new Set(messages.value.map(m => m.id).filter(id => id > 0))
        const uniqueNew = newMessages.filter(m => !existingIds.has(m.id))
        messages.value = [...uniqueNew, ...messages.value]
        return uniqueNew.length
      } else {
        messages.value = newMessages
        return newMessages.length
      }
    } catch {
      // 后端不可达时静默处理
      return 0
    }
  }

  /** 选中会话并加载消息，同时通知服务端已读 */
  async function selectConversation(id: number) {
    if (!id || id <= 0) {
      clearSelection()
      return
    }
    currentConversationId.value = id
    messages.value = []
    const conv = conversations.value.find(c => c.id === id)
    if (conv && conv.unreadCount > 0) {
      conv.unreadCount = 0
      markAsRead(id).catch(() => { /* 静默 */ })
    }
    await loadMessages(id)
  }

  /** 清除当前会话选择（返回会话列表） */
  function clearSelection() {
    currentConversationId.value = null
    messages.value = []
  }

  /** 发送消息（带状态追踪） */
  async function sendMsg(
    conversationId: number,
    contentType: SendMessageRequest['contentType'],
    content: string,
    fileUrl?: string,
    fileName?: string,
    fileSize?: number
  ) {
    if (!conversationId || conversationId <= 0) {
      console.warn('[chat] 无效的会话ID:', conversationId)
      return
    }

    const userStore = useUserStore()
    const localId = generateLocalId()

    // 创建本地消息，status='sending'，立即插入列表
    const localMsg: ChatMessage = {
      id: 0,
      conversationId,
      senderId: userStore.userInfo?.id ?? 0,
      senderName: userStore.userInfo?.nickname ?? '',
      contentType,
      content,
      sendTime: new Date().toISOString(),
      status: 'sending',
      localId,
      ...(fileUrl !== undefined ? { fileUrl } : {}),
      ...(fileName !== undefined ? { fileName } : {}),
      ...(fileSize !== undefined ? { fileSize } : {}),
    }
    messages.value.push(localMsg)

    try {
      const res = await sendMessageApi({
        conversationId, contentType, content,
        ...(fileUrl !== undefined ? { fileUrl } : {}),
        ...(fileName !== undefined ? { fileName } : {}),
        ...(fileSize !== undefined ? { fileSize } : {}),
      })
      // 发送成功：更新状态和服务端返回的真实ID
      const serverMsg = res.data as unknown as ChatMessage | undefined
      messages.value = updateMessageStatus(
        messages.value, localId, 'sent',
        serverMsg ? { id: serverMsg.id, sendTime: serverMsg.sendTime } : undefined
      )
      // 发送成功后更新会话列表的最后一条消息
      const conv = conversations.value.find(c => c.id === conversationId)
      if (conv) {
        conv.lastMessage = content
        conv.lastMessageTime = new Date().toISOString()
      }
    } catch {
      messages.value = updateMessageStatus(messages.value, localId, 'failed')
    }
  }

  /** 重发失败消息 */
  async function resendMsg(localId: string) {
    const idx = messages.value.findIndex(m => m.localId === localId && m.status === 'failed')
    if (idx === -1) return

    const msg = messages.value[idx]!
    // 重置为sending状态
    messages.value = updateMessageStatus(messages.value, localId, 'sending')

    try {
      const res = await sendMessageApi({
        conversationId: msg.conversationId,
        contentType: msg.contentType,
        content: msg.content,
        ...(msg.fileUrl !== undefined ? { fileUrl: msg.fileUrl } : {}),
        ...(msg.fileName !== undefined ? { fileName: msg.fileName } : {}),
        ...(msg.fileSize !== undefined ? { fileSize: msg.fileSize } : {}),
      })
      const serverMsg = res.data as unknown as ChatMessage | undefined
      messages.value = updateMessageStatus(
        messages.value, localId, 'sent',
        serverMsg ? { id: serverMsg.id, sendTime: serverMsg.sendTime } : undefined
      )
    } catch {
      messages.value = updateMessageStatus(messages.value, localId, 'failed')
    }
  }

  /** 处理 WebSocket 实时推送的新消息 */
  function handleIncomingMessage(msg: ChatMessage) {
    const userStore = useUserStore()
    const isCurrentConv = msg.conversationId === currentConversationId.value
    const isSelfMsg = msg.senderId === (userStore.userInfo?.id ?? 0)

    if (isCurrentConv) {
      // 去重：检查是否已有相同 id 的消息（自己发送后WS又推回来）
      if (msg.id && messages.value.some(m => m.id === msg.id)) {
        return // 已存在，跳过
      }
      // 如果是自己发的消息，检查是否有本地 sending/sent 消息对应
      if (isSelfMsg) {
        // 场景1: HTTP响应已更新id → 上面的id匹配已拦截
        // 场景2: HTTP响应未到达，本地消息 id=0 → 按内容+时间匹配
        const recentLocal = messages.value.find(
          m => m.senderId === msg.senderId
            && (m.status === 'sending' || m.status === 'sent')
            && m.content === msg.content
            && m.id === 0
        )
        if (recentLocal) {
          // 用服务端消息更新本地消息的id和状态
          recentLocal.id = msg.id
          recentLocal.sendTime = msg.sendTime
          recentLocal.status = 'sent'
          return
        }
      }
      messages.value.push(msg)
      // 当前会话的消息自动标记已读
      if (!isSelfMsg) {
        markAsRead(msg.conversationId).catch(() => {})
      }
    }

    const conv = conversations.value.find(c => c.id === msg.conversationId)
    if (conv) {
      conv.lastMessage = msg.content
      conv.lastMessageTime = msg.sendTime
      if (!isCurrentConv && !isSelfMsg) {
        conv.unreadCount = (conv.unreadCount || 0) + 1
        // 浏览器通知
        notifyNewMessage(conv.name, msg.content)
      }
    }
  }

  /** 浏览器桌面通知 */
  function notifyNewMessage(convName: string, content: string) {
    // 播放提示音
    try {
      const audio = new Audio('/notification.mp3')
      audio.volume = 0.3
      audio.play().catch(() => {})
    } catch { /* 静默 */ }

    // 桌面通知
    if (typeof Notification !== 'undefined' && Notification.permission === 'granted') {
      try {
        new Notification(convName || '新消息', {
          body: content.length > 50 ? content.slice(0, 50) + '...' : content,
          icon: '/favicon.ico',
          tag: 'chat-msg',
          silent: true,
        })
      } catch { /* 静默 */ }
    } else if (typeof Notification !== 'undefined' && Notification.permission === 'default') {
      Notification.requestPermission().catch(() => {})
    }
  }

  /** 切换聊天面板显示/隐藏 */
  function togglePanel() {
    panelVisible.value = !panelVisible.value
  }

  /** 定时刷新会话列表以更新未读计数（轮询间隔 ID） */
  let pollTimer: ReturnType<typeof setInterval> | null = null

  /** 全局初始化：加载会话列表 + 启动轮询 */
  function initGlobal() {
    loadConversations()
    // 每 30 秒刷新一次会话列表，保持未读数同步（降低频率以减少内存波动）
    if (!pollTimer) {
      pollTimer = setInterval(() => {
        loadConversations()
      }, 30000)
    }
  }

  /** 停止轮询 */
  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  return {
    conversations,
    currentConversationId,
    messages,
    panelVisible,
    totalUnread,
    currentConversation,
    loadConversations,
    loadMessages,
    selectConversation,
    clearSelection,
    sendMsg,
    resendMsg,
    handleIncomingMessage,
    togglePanel,
    initGlobal,
    stopPolling
  }
})

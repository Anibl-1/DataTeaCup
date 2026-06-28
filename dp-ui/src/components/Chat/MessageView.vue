<template>
  <div class="message-view">
    <div v-if="!chatStore.currentConversation" class="empty-state">
      <n-icon size="32" color="#cbd5e1"><ChatbubblesOutline /></n-icon>
      <span>请选择一个会话</span>
    </div>
    <div
      v-else
      ref="messageContainerRef"
      class="message-container"
      @scroll="handleScroll"
    >
      <div v-if="loadingMore" class="loading-more">
        <n-spin size="small" />
        <span>加载历史消息...</span>
      </div>

      <template v-for="(msg, idx) in chatStore.messages" :key="msg.id || msg.localId">
        <div v-if="shouldShowDateDivider(idx)" class="date-divider">
          <div class="date-line" />
          <span class="date-label">{{ formatDateDivider(msg.sendTime) }}</span>
          <div class="date-line" />
        </div>

        <div :class="['msg-row', { 'msg-self': isSelf(msg) }]">
          <n-avatar
            v-if="!isSelf(msg)"
            round
            :size="32"
            :style="{ background: getAvatarGradient(getSenderDisplayName(msg)), flexShrink: 0 }"
          >
            {{ getSenderDisplayName(msg).charAt(0) }}
          </n-avatar>

          <div class="msg-content-wrap">
            <div v-if="!isSelf(msg) && chatStore.currentConversation?.type === 'group'" class="msg-sender">
              {{ getSenderDisplayName(msg) }}
            </div>
            <div :class="['msg-bubble', { self: isSelf(msg) }]">
              <template v-if="msg.contentType === 'text'">
                <span class="msg-text">{{ msg.content }}</span>
              </template>
              <template v-else-if="msg.contentType === 'image'">
                <n-image
                  :src="msg.fileUrl || msg.content"
                  :preview-src="msg.fileUrl || msg.content"
                  :img-props="{ style: 'max-width: 200px; max-height: 200px; border-radius: 8px; cursor: pointer;' }"
                  object-fit="contain"
                />
              </template>
              <template v-else-if="msg.contentType === 'file'">
                <div class="file-card">
                  <div class="file-icon-wrap">
                    <n-icon size="18"><DocumentTextOutline /></n-icon>
                  </div>
                  <div class="file-info">
                    <span class="file-name">{{ msg.fileName || '未知文件' }}</span>
                    <span class="file-size">{{ formatFileSize(msg.fileSize) }}</span>
                  </div>
                  <a :href="msg.fileUrl" target="_blank" rel="noopener noreferrer" class="file-dl">
                    <n-icon size="16"><DownloadOutline /></n-icon>
                  </a>
                </div>
              </template>
            </div>
            <div class="msg-meta">
              <template v-if="isSelf(msg)">
                <span v-if="msg.status === 'sending'" class="msg-status sending">
                  <n-spin :size="10" />
                </span>
                <span v-else-if="msg.status === 'failed'" class="msg-status failed" @click="chatStore.resendMsg(msg.localId ?? '')">
                  发送失败 点击重试
                </span>
                <span v-else-if="isRead(msg)" class="msg-status read">✓ 已读</span>
                <span v-else class="msg-status unread">✓ 已发送</span>
              </template>
              <span class="msg-time">{{ formatTime(msg.sendTime) }}</span>
            </div>
          </div>

          <n-avatar
            v-if="isSelf(msg)"
            round
            :size="32"
            :style="{ background: 'linear-gradient(135deg, #3b82f6, #6366f1)', flexShrink: 0 }"
          >
            {{ userStore.userInfo?.nickname?.charAt(0) || '我' }}
          </n-avatar>
        </div>
      </template>

      <div v-if="chatStore.messages.length === 0" class="no-messages">
        <n-icon size="28" color="#cbd5e1"><ChatbubbleEllipsesOutline /></n-icon>
        <span>发送第一条消息开始聊天吧</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { NAvatar, NImage, NSpin, NIcon } from 'naive-ui'
import { ChatbubblesOutline, ChatbubbleEllipsesOutline, DocumentTextOutline, DownloadOutline } from '@vicons/ionicons5'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import type { ChatMessage } from '@/types/chat'

const chatStore = useChatStore()
const userStore = useUserStore()
const messageContainerRef = ref<HTMLDivElement | null>(null)
const loadingMore = ref(false)
const hasMore = ref(true)
let isAutoScrolling = false

function isSelf(msg: ChatMessage): boolean {
  return msg.senderId === (userStore.userInfo?.id ?? 0)
}

/** 获取发送者显示名称：优先 senderName，回退到会话成员昵称 */
function getSenderDisplayName(msg: ChatMessage): string {
  if (msg.senderName) return msg.senderName
  const conv = chatStore.currentConversation
  if (conv?.members?.length) {
    const member = conv.members.find(m => m.userId === msg.senderId)
    if (member?.nickname) return member.nickname
  }
  return '未知用户'
}

function isRead(msg: ChatMessage): boolean {
  if (!msg.readBy || msg.readBy.length === 0) return false
  return msg.readBy.some(uid => uid !== msg.senderId)
}

function shouldShowDateDivider(idx: number): boolean {
  if (idx === 0) return true
  const prev = chatStore.messages[idx - 1]
  const curr = chatStore.messages[idx]
  if (!prev || !curr) return false
  return new Date(prev.sendTime).toDateString() !== new Date(curr.sendTime).toDateString()
}

function formatDateDivider(timeStr: string): string {
  const date = new Date(timeStr)
  if (isNaN(date.getTime())) return ''
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  if (date.toDateString() === now.toDateString()) return '今天'
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) return '昨天'
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function scrollToBottom() {
  nextTick(() => {
    const el = messageContainerRef.value
    if (el) {
      isAutoScrolling = true
      el.scrollTop = el.scrollHeight
      setTimeout(() => { isAutoScrolling = false }, 100)
    }
  })
}

watch(() => chatStore.currentConversationId, () => {
  hasMore.value = true
  scrollToBottom()
})
watch(() => chatStore.messages.length, (newLen, oldLen) => {
  // 只在新消息追加到末尾时自动滚动（不在加载历史时触发）
  if (!loadingMore.value && newLen > oldLen) scrollToBottom()
})

async function handleScroll() {
  const el = messageContainerRef.value
  if (!el || loadingMore.value || isAutoScrolling || !hasMore.value) return
  if (el.scrollTop > 50) return
  const conversationId = chatStore.currentConversationId
  if (!conversationId) return
  const firstMsg = chatStore.messages[0]
  if (!firstMsg || !firstMsg.id) return
  loadingMore.value = true
  const prevHeight = el.scrollHeight
  try {
    const loaded = await chatStore.loadMessages(conversationId, firstMsg.id)
    if (loaded === 0) {
      hasMore.value = false
    } else {
      nextTick(() => { el.scrollTop = el.scrollHeight - prevHeight })
    }
  } finally {
    loadingMore.value = false
  }
}

function formatFileSize(bytes?: number): string {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatTime(timeStr: string): string {
  const date = new Date(timeStr)
  if (isNaN(date.getTime())) return ''
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const hhmm = `${pad(date.getHours())}:${pad(date.getMinutes())}`
  if (
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()
  ) return hhmm
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${hhmm}`
}

function getAvatarGradient(name?: string): string {
  const gradients = [
    'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
    'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
    'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)',
    'linear-gradient(135deg, #fccb90 0%, #d57eeb 100%)',
    'linear-gradient(135deg, #e0c3fc 0%, #8ec5fc 100%)',
  ]
  const n = name || ''
  let hash = 0
  for (let i = 0; i < n.length; i++) {
    hash = n.charCodeAt(i) + ((hash << 5) - hash)
  }
  return gradients[Math.abs(hash) % gradients.length]!
}
</script>

<style scoped>
.message-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  min-height: 120px;
  gap: 8px;
  color: #94a3b8;
  font-size: 13px;
}

.message-container {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px 14px;
  background: #f8fafc;
}
.message-container::-webkit-scrollbar {
  width: 4px;
}
.message-container::-webkit-scrollbar-thumb {
  background: rgba(0,0,0,0.08);
  border-radius: 2px;
}
.message-container::-webkit-scrollbar-thumb:hover {
  background: rgba(0,0,0,0.15);
}
.message-container::-webkit-scrollbar-track {
  background: transparent;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 0 12px;
  color: #94a3b8;
  font-size: 12px;
}

.no-messages {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 6px;
  color: #94a3b8;
  font-size: 13px;
}

/* ===== Date divider ===== */
.date-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 16px 0;
  padding: 0 4px;
}

.date-line {
  flex: 1;
  height: 1px;
  background: #e2e8f0;
}

.date-label {
  color: #94a3b8;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
}

/* ===== Message row ===== */
.msg-row {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  align-items: flex-start;
}
.msg-row.msg-self {
  flex-direction: row-reverse;
}

.msg-row :deep(.n-avatar) {
  color: #fff;
  font-weight: 600;
  font-size: 13px;
  text-shadow: 0 1px 2px rgba(0,0,0,0.2);
}

.msg-content-wrap {
  max-width: 72%;
  display: flex;
  flex-direction: column;
}
.msg-self .msg-content-wrap {
  align-items: flex-end;
}

.msg-sender {
  font-size: 11px;
  color: #94a3b8;
  margin-bottom: 2px;
  font-weight: 500;
}

/* ===== Bubble ===== */
.msg-bubble {
  padding: 9px 13px;
  border-radius: 4px 14px 14px 14px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  word-break: break-word;
  font-size: 13.5px;
  line-height: 1.55;
  color: #1e293b;
}
.msg-bubble.self {
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: #fff;
  border-radius: 14px 4px 14px 14px;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
}

.msg-text {
  white-space: pre-wrap;
}

/* ===== Meta ===== */
.msg-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 3px;
  padding: 0 2px;
}
.msg-self .msg-meta {
  flex-direction: row-reverse;
}

.msg-time {
  font-size: 10px;
  color: #b0b8c4;
}

.msg-status {
  font-size: 10px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.msg-status.sending {
  color: #94a3b8;
}
.msg-status.unread {
  color: #94a3b8;
}
.msg-status.read {
  color: #10b981;
  font-weight: 500;
}
.msg-status.failed {
  color: #ef4444;
  cursor: pointer;
  font-weight: 500;
}
.msg-status.failed:hover {
  text-decoration: underline;
}

/* ===== File card ===== */
.file-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: rgba(0,0,0,0.03);
  border-radius: 10px;
  min-width: 180px;
}
.msg-bubble.self .file-card {
  background: rgba(255,255,255,0.15);
}

.file-icon-wrap {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: rgba(59,130,246,0.08);
  color: #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.msg-bubble.self .file-icon-wrap {
  background: rgba(255,255,255,0.2);
  color: #fff;
}

.file-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.file-name {
  font-size: 12.5px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.file-size {
  font-size: 11px;
  opacity: 0.65;
}
.file-dl {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  background: rgba(59,130,246,0.08);
  color: #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  text-decoration: none;
  transition: all 0.15s;
}
.file-dl:hover {
  background: rgba(59,130,246,0.15);
  transform: scale(1.05);
}
.msg-bubble.self .file-dl {
  background: rgba(255,255,255,0.2);
  color: #fff;
}
</style>

<style>
/* MessageView dark mode */
html.dark .message-container {
  background: #0c1222 !important;
}
html.dark .msg-bubble {
  background: #1e293b !important;
  color: #e2e8f0 !important;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2) !important;
}
html.dark .msg-bubble.self {
  background: linear-gradient(135deg, #4f46e5, #6366f1) !important;
  color: #fff !important;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.25) !important;
}
html.dark .date-line {
  background: rgba(255,255,255,0.06) !important;
}
html.dark .date-label {
  color: #64748b !important;
}
html.dark .msg-sender {
  color: #64748b !important;
}
html.dark .msg-time {
  color: #475569 !important;
}
html.dark .message-view .empty-state,
html.dark .no-messages {
  color: #64748b !important;
}
html.dark .loading-more {
  color: #64748b !important;
}
html.dark .file-card {
  background: rgba(255,255,255,0.04) !important;
}
html.dark .file-icon-wrap {
  background: rgba(99,102,241,0.15) !important;
  color: #818cf8 !important;
}
html.dark .file-dl {
  background: rgba(99,102,241,0.15) !important;
  color: #818cf8 !important;
}
html.dark .message-container::-webkit-scrollbar-thumb {
  background: rgba(255,255,255,0.06) !important;
}
</style>

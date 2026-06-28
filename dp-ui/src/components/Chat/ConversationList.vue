<template>
  <div class="conversation-list">
    <div
      v-for="conv in displayedConversations"
      :key="conv.id"
      class="conversation-item"
      :class="{ active: chatStore.currentConversationId === conv.id, unread: conv.unreadCount > 0 }"
      @click="chatStore.selectConversation(conv.id)"
    >
      <div class="avatar-wrapper">
        <n-avatar round :size="42" :style="{ background: getAvatarGradient(getDisplayName(conv)) }">
          {{ getDisplayName(conv).charAt(0) }}
        </n-avatar>
        <span
          v-if="conv.type === 'private'"
          class="status-dot"
          :class="{ online: isOnline(conv) }"
        />
      </div>
      <div class="conversation-info">
        <div class="info-top">
          <span class="conv-name">{{ getDisplayName(conv) }}</span>
          <span v-if="conv.lastMessageTime" class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
        </div>
        <div class="info-bottom">
          <span class="last-message">{{ conv.lastMessage || '暂无消息' }}</span>
          <span v-if="conv.unreadCount > 0" class="unread-badge">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
        </div>
      </div>
    </div>
    <div v-if="displayedConversations.length === 0" class="empty-state">
      <div class="empty-icon-wrap">
        <n-icon size="28"><ChatbubblesOutline /></n-icon>
      </div>
      <span class="empty-title">{{ props.searchKeyword ? '未找到匹配会话' : '暂无会话' }}</span>
      <span class="empty-hint">点击联系人开始新的对话</span>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, onMounted, watch } from 'vue'
import { NAvatar, NIcon } from 'naive-ui'
import { ChatbubblesOutline } from '@vicons/ionicons5'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { getOnlineUsers } from '@/api/chat'
import type { OnlineUser, Conversation } from '@/types/chat'

const props = defineProps<{
  searchKeyword?: string
}>()

const chatStore = useChatStore()
const userStore = useUserStore()
const onlineUsers = ref<OnlineUser[]>([])

onMounted(() => {
  chatStore.loadConversations()
  loadOnlineStatus()
})

watch(() => chatStore.conversations.length, () => {
  loadOnlineStatus()
})

async function loadOnlineStatus() {
  try {
    const res = await getOnlineUsers()
    onlineUsers.value = res.data || []
  } catch { /* silent */ }
}

const displayedConversations = computed(() => {
  const convs = chatStore.conversations
  if (!props.searchKeyword) return convs
  const kw = props.searchKeyword.toLowerCase()
  return convs.filter((c: any) => {
    const name = getDisplayName(c).toLowerCase()
    const msg = (c.lastMessage || '').toLowerCase()
    return name.includes(kw) || msg.includes(kw)
  })
})

function getAvatarGradient(name: string): string {
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
  let hash = 0
  for (let i = 0; i < (name?.length || 0); i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return gradients[Math.abs(hash) % gradients.length]!
}

/** 获取会话显示名称：优先用 name，私聊回退到对方昵称 */
function getDisplayName(conv: Conversation): string {
  if (conv.name) return conv.name
  if (conv.type === 'private' && conv.members?.length) {
    const peer = conv.members.find(m => m.userId !== userStore.userInfo?.id)
    if (peer?.nickname) return peer.nickname
    return conv.members[0]?.nickname || '未命名'
  }
  return '未命名会话'
}

function isOnline(conv: Conversation): boolean {
  if (conv.type !== 'private') return false
  const peer = conv.members?.find(m => m.userId !== userStore.userInfo?.id)
  if (!peer) return false
  return onlineUsers.value.some(u => u.userId === peer.userId && u.status === 'online')
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
  ) {
    return hhmm
  }
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${hhmm}`
}
</script>

<style scoped>
.conversation-list {
  padding: 4px 0;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  cursor: pointer;
  transition: all 0.12s ease;
  border-radius: 10px;
  margin: 1px 8px;
}

.conversation-item:hover {
  background: #f1f5f9;
}

.conversation-item.active {
  background: #eff6ff;
}

.conversation-item.unread .conv-name {
  font-weight: 600;
}

.conversation-item.unread .last-message {
  color: #475569;
  font-weight: 500;
}

.avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.avatar-wrapper :deep(.n-avatar) {
  color: #fff;
  font-weight: 600;
  font-size: 15px;
  text-shadow: 0 1px 2px rgba(0,0,0,0.2);
}

.status-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  border: 2px solid #fff;
  background: #d1d5db;
  transition: all 0.2s ease;
}

.status-dot.online {
  background: #10b981;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.15);
}

.conversation-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.info-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.conv-name {
  font-size: 13.5px;
  font-weight: 500;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-time {
  font-size: 11px;
  color: #94a3b8;
  flex-shrink: 0;
  margin-left: 8px;
}

.info-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.last-message {
  font-size: 12.5px;
  color: #94a3b8;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.unread-badge {
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  background: #ef4444;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
  flex-shrink: 0;
  line-height: 1;
}

/* ===== Empty state ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  gap: 6px;
}

.empty-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #cbd5e1;
  margin-bottom: 4px;
}

.empty-title {
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
}

.empty-hint {
  font-size: 12px;
  color: #94a3b8;
}
</style>

<style>
/* ConversationList dark mode */
html.dark .conversation-item:hover {
  background: rgba(255,255,255,0.05) !important;
}
html.dark .conversation-item.active {
  background: rgba(59,130,246,0.1) !important;
}
html.dark .status-dot {
  border-color: #111827 !important;
}
html.dark .conv-name {
  color: #e5e7eb !important;
}
html.dark .conv-time {
  color: #64748b !important;
}
html.dark .last-message {
  color: #64748b !important;
}
html.dark .conversation-item.unread .last-message {
  color: #94a3b8 !important;
}
html.dark .conversation-list .empty-state {
  color: #64748b !important;
}
html.dark .conversation-list .empty-icon-wrap {
  background: #1e293b !important;
  color: #475569 !important;
}
html.dark .conversation-list .empty-title {
  color: #94a3b8 !important;
}
</style>

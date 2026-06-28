<template>
  <!-- 遮罩层 -->
  <Transition name="chat-overlay">
    <div v-if="chatStore.panelVisible" class="chat-overlay" @click="chatStore.togglePanel" />
  </Transition>
  <div class="chat-panel" :class="{ visible: chatStore.panelVisible }">
    <div class="chat-panel-header">
      <div class="header-left">
        <div class="header-icon-wrap">
          <n-icon size="18"><ChatbubblesOutline /></n-icon>
        </div>
        <div class="header-title-group">
          <span class="header-title">即时通讯</span>
          <span class="header-online-count">{{ onlineCount }} 人在线</span>
        </div>
      </div>
      <div class="header-actions">
        <button class="header-btn" @click="loadContacts" title="刷新">
          <n-icon size="16"><RefreshOutline /></n-icon>
        </button>
        <button class="header-btn header-btn-close" @click="chatStore.togglePanel">
          <n-icon size="16"><CloseOutline /></n-icon>
        </button>
      </div>
    </div>
    <div class="chat-panel-body">
      <!-- 会话消息视图 -->
      <template v-if="chatStore.currentConversationId">
        <div class="chat-back-bar">
          <button class="back-btn" @click="chatStore.clearSelection(); currentConversationId_local = null">
            <n-icon size="16"><ArrowBackOutline /></n-icon>
          </button>
          <div class="back-info">
            <span class="chat-back-title">{{ getConvDisplayName(chatStore.currentConversation) }}</span>
            <span v-if="currentPeerOnline" class="back-status online">在线</span>
            <span v-else class="back-status">离线</span>
          </div>
        </div>
        <MessageView />
        <MessageInput />
      </template>
      <!-- 会话列表 + 联系人 -->
      <template v-else>
        <!-- 搜索栏 -->
        <div class="panel-search">
          <n-input
            v-model:value="globalSearch"
            placeholder="搜索会话或联系人..."
            size="small"
            clearable
            round
          >
            <template #prefix><n-icon size="14"><SearchOutline /></n-icon></template>
          </n-input>
        </div>
        <n-tabs v-model:value="activeTab" type="line" justify-content="space-around">
          <n-tab-pane name="conversations" :tab="conversationsTabLabel">
            <ConversationList :search-keyword="globalSearch" />
          </n-tab-pane>
          <n-tab-pane name="contacts" :tab="contactsTabLabel">
            <div class="contacts-list">
              <!-- 在线分组 -->
              <template v-if="onlineFilteredUsers.length > 0">
                <div class="contact-group-title">在线 ({{ onlineFilteredUsers.length }})</div>
                <div
                  v-for="user in onlineFilteredUsers"
                  :key="user.id"
                  class="contact-item"
                  @click="startChatWith(user)"
                >
                  <div class="contact-avatar-wrap">
                    <n-avatar round :size="36" :style="{ background: getAvatarColor(user.nickname) }">
                      {{ user.nickname?.charAt(0) || '?' }}
                    </n-avatar>
                    <span class="online-dot online" />
                  </div>
                  <div class="contact-info">
                    <span class="contact-name">{{ user.nickname }}</span>
                    <span class="contact-role">{{ user.username }}</span>
                  </div>
                  <n-icon size="16" class="contact-chat-icon"><ChatbubbleOutline /></n-icon>
                </div>
              </template>
              <!-- 离线分组 -->
              <template v-if="offlineFilteredUsers.length > 0">
                <div class="contact-group-title">离线 ({{ offlineFilteredUsers.length }})</div>
                <div
                  v-for="user in offlineFilteredUsers"
                  :key="user.id"
                  class="contact-item offline"
                  @click="startChatWith(user)"
                >
                  <div class="contact-avatar-wrap">
                    <n-avatar round :size="36" :style="{ background: getAvatarColor(user.nickname), opacity: 0.6 }">
                      {{ user.nickname?.charAt(0) || '?' }}
                    </n-avatar>
                    <span class="online-dot" />
                  </div>
                  <div class="contact-info">
                    <span class="contact-name">{{ user.nickname }}</span>
                    <span class="contact-role">{{ user.username }}</span>
                  </div>
                  <n-icon size="16" class="contact-chat-icon"><ChatbubbleOutline /></n-icon>
                </div>
              </template>
              <div v-if="filteredUsers.length === 0" class="empty-list">
                <div class="empty-icon-wrap">
                  <n-icon size="32"><PeopleOutline /></n-icon>
                </div>
                <span class="empty-title">{{ globalSearch ? '未找到匹配联系人' : '暂无联系人' }}</span>
                <span class="empty-hint">待管理员添加更多用户</span>
              </div>
            </div>
          </n-tab-pane>
        </n-tabs>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, onMounted, watch, h } from 'vue'
import { NIcon, NTabs, NTabPane, NAvatar, NInput, NBadge, useMessage } from 'naive-ui'
import { CloseOutline, PeopleOutline, ChatbubblesOutline, ChatbubbleOutline, ArrowBackOutline, SearchOutline, RefreshOutline } from '@vicons/ionicons5'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { getOnlineUsers, createPrivateConversation } from '@/api/chat'
import request from '@/api/request'
import type { OnlineUser, Conversation } from '@/types/chat'
import type { User } from '@/types/user'
import ConversationList from './ConversationList.vue'
import MessageView from './MessageView.vue'
import MessageInput from './MessageInput.vue'

const chatStore = useChatStore()
const userStore = useUserStore()
const message = useMessage()
const activeTab = ref<string>('conversations')
const onlineUsers = ref<OnlineUser[]>([])
const allUsers = ref<User[]>([])
const globalSearch = ref('')
const currentConversationId_local = ref<number | string | null>(null)

const onlineCount = computed(() => onlineUsers.value.filter(u => u.status === 'online').length)

const totalUnread = computed(() =>
  chatStore.conversations.reduce((sum: number, c: any) => sum + (c.unreadCount || 0), 0)
)

const conversationsTabLabel = computed(() => {
  const label = '会话'
  if (totalUnread.value > 0) return h('span', {}, [label, ' ', h(NBadge, { value: totalUnread.value, max: 99, offset: [6, -2] })])
  return label
})

const contactsTabLabel = computed(() => {
  return `联系人 (${allUsers.value.length})`
})

const filteredUsers = computed(() => {
  if (!globalSearch.value) return allUsers.value
  const kw = globalSearch.value.toLowerCase()
  return allUsers.value.filter(u =>
    u.nickname?.toLowerCase().includes(kw) ||
    u.username?.toLowerCase().includes(kw)
  )
})

const onlineFilteredUsers = computed(() =>
  filteredUsers.value.filter(u => isUserIdOnline(u.id))
)

const offlineFilteredUsers = computed(() =>
  filteredUsers.value.filter(u => !isUserIdOnline(u.id))
)

const currentPeerOnline = computed(() => {
  const conv = chatStore.currentConversation
  if (!conv || conv.type !== 'private') return false
  const peer = conv.members?.find((m: any) => m.userId !== userStore.userInfo?.id)
  if (!peer) return false
  return onlineUsers.value.some(u => u.userId === peer.userId && u.status === 'online')
})

onMounted(() => {
  loadContacts()
})

watch(() => chatStore.panelVisible, (visible) => {
  if (visible) loadContacts()
})

/** 加载联系人：先加载所有用户，再加载在线状态 */
async function loadContacts() {
  try {
    const res = await request.get('/user/list', {
      params: { page: 1, pageSize: 200 },
      __silent: true
    } as any)
    const data = (res as any)?.data
    const users = data?.records || data?.list || (Array.isArray(data) ? data : [])
    allUsers.value = users.filter(
      (u: any) => u.id !== userStore.userInfo?.id && u.status === 1
    )
  } catch {
    // 后端不可达时静默
  }
  try {
    const res = await getOnlineUsers()
    onlineUsers.value = res.data || []
  } catch {
    // 静默
  }
}

/** 点击联系人开启私聊 */
async function startChatWith(user: User) {
  // 先检查是否已有和此用户的私聊会话（通过成员匹配或名称匹配）
  const existing = chatStore.conversations.find(
    (c: any) => c.type === 'private' && (
      c.targetUserId === user.id ||
      c.members?.some((m: any) => m.userId === user.id) ||
      c.name === user.nickname
    )
  )
  if (existing) {
    chatStore.selectConversation(existing.id)
    currentConversationId_local.value = existing.id
    return
  }
  // 调用后端创建私聊会话
  try {
    const res = await createPrivateConversation(user.id)
    const conv = (res as any)?.data
    if (conv?.id) {
      await chatStore.loadConversations()
      chatStore.selectConversation(conv.id)
      currentConversationId_local.value = conv.id
    } else {
      message.warning('创建会话失败')
    }
  } catch (e: any) {
    const errMsg = e?.response?.data?.message || e?.message || ''
    if (errMsg.includes('已存在') || errMsg.includes('exist')) {
      // 会话可能已存在但本地没有，刷新会话列表后重试查找
      await chatStore.loadConversations()
      const retryConv = chatStore.conversations.find(
        (c: any) => c.type === 'private' && (
          c.members?.some((m: any) => m.userId === user.id) ||
          c.name === user.nickname
        )
      )
      if (retryConv) {
        chatStore.selectConversation(retryConv.id)
        currentConversationId_local.value = retryConv.id
        return
      }
    }
    message.error('无法创建会话：' + (errMsg || '服务器错误'))
  }
}

/** 检查某个用户ID是否在线 */
function isUserIdOnline(userId: number): boolean {
  return onlineUsers.value.some(u => u.userId === userId && u.status === 'online')
}

/** 获取会话显示名称 */
function getConvDisplayName(conv: Conversation | null): string {
  if (!conv) return '会话'
  if (conv.name) return conv.name
  if (conv.type === 'private' && conv.members?.length) {
    const peer = conv.members.find(m => m.userId !== userStore.userInfo?.id)
    if (peer?.nickname) return peer.nickname
    return conv.members[0]?.nickname || '未命名'
  }
  return '未命名会话'
}

/** 根据名称生成渐变头像背景色 */
function getAvatarColor(name: string): string {
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
</script>

<style scoped>
/* 遮罩层 */
.chat-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.25);
  z-index: 999;
  backdrop-filter: blur(2px);
}

.chat-overlay-enter-active,
.chat-overlay-leave-active {
  transition: opacity 0.25s ease;
}

.chat-overlay-enter-from,
.chat-overlay-leave-to {
  opacity: 0;
}

.chat-panel {
  --panel-bg: #fff;
  --panel-bg2: #f8fafc;
  --panel-border: #eef0f3;
  --panel-shadow: rgba(0, 0, 0, 0.10);
  --panel-text: #1e293b;
  --panel-text-secondary: #94a3b8;
  --panel-hover-bg: #f1f5f9;
  --panel-accent: #3b82f6;

  position: fixed;
  top: 0;
  right: 0;
  width: 380px;
  height: 100vh;
  background: var(--panel-bg);
  box-shadow: -6px 0 32px var(--panel-shadow);
  transform: translateX(100%);
  transition: transform 300ms cubic-bezier(0.32, 0.72, 0, 1);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  border-left: 1px solid var(--panel-border);
}

.chat-panel.visible {
  transform: translateX(0);
}

/* ===== Header ===== */
.chat-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid var(--panel-border);
  flex-shrink: 0;
  background: var(--panel-bg);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon-wrap {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header-title-group {
  display: flex;
  flex-direction: column;
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--panel-text);
  line-height: 1.2;
}

.header-online-count {
  font-size: 11px;
  color: #10b981;
  font-weight: 500;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-btn {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--panel-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s;
}

.header-btn:hover {
  background: var(--panel-hover-bg);
  color: var(--panel-text);
}

.header-btn-close:hover {
  background: #fef2f2;
  color: #ef4444;
}

/* ===== Search bar ===== */
.panel-search {
  padding: 8px 12px;
  flex-shrink: 0;
}

.panel-search :deep(.n-input) {
  background: var(--panel-bg2) !important;
  border-color: transparent !important;
}

.panel-search :deep(.n-input:focus-within) {
  background: var(--panel-bg) !important;
  border-color: var(--panel-accent) !important;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

/* ===== Back bar ===== */
.chat-back-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--panel-border);
  flex-shrink: 0;
  background: var(--panel-bg);
}

.back-btn {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: none;
  background: var(--panel-hover-bg);
  color: var(--panel-text);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s;
  flex-shrink: 0;
}

.back-btn:hover {
  background: #dbeafe;
  color: var(--panel-accent);
}

.back-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.chat-back-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--panel-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.back-status {
  font-size: 11px;
  color: var(--panel-text-secondary);
  padding: 1px 6px;
  border-radius: 4px;
  background: #f1f5f9;
  flex-shrink: 0;
}

.back-status.online {
  color: #10b981;
  background: #ecfdf5;
}

/* ===== Body ===== */
.chat-panel-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.chat-panel-body :deep(.n-tabs) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-panel-body :deep(.n-tabs .n-tab-pane) {
  flex: 1;
  overflow-y: auto;
}

.chat-panel-body :deep(.n-tabs .n-tabs-nav) {
  order: 1;
  border-top: 1px solid var(--panel-border);
  border-bottom: none;
  padding: 0 8px;
  background: var(--panel-bg);
}

/* ===== Contacts ===== */
.contacts-list {
  padding: 4px 0;
}

.contact-group-title {
  font-size: 11px;
  font-weight: 600;
  color: var(--panel-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 10px 16px 4px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  cursor: pointer;
  transition: all 0.12s ease;
  border-radius: 10px;
  margin: 1px 8px;
}

.contact-item:hover {
  background: var(--panel-hover-bg);
}

.contact-item:active {
  transform: scale(0.99);
}

.contact-item.offline {
  opacity: 0.7;
}

.contact-avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.contact-avatar-wrap :deep(.n-avatar) {
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  text-shadow: 0 1px 2px rgba(0,0,0,0.2);
}

.online-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #d1d5db;
  border: 2px solid var(--panel-bg);
  transition: all 0.2s ease;
}

.online-dot.online {
  background: #10b981;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.15);
}

.contact-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.contact-name {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--panel-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contact-role {
  font-size: 11.5px;
  color: var(--panel-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contact-chat-icon {
  color: #cbd5e1;
  opacity: 0;
  transition: all 0.15s ease;
}

.contact-item:hover .contact-chat-icon {
  opacity: 1;
  color: var(--panel-accent);
}

/* ===== Empty state ===== */
.empty-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  gap: 6px;
}

.empty-icon-wrap {
  width: 52px;
  height: 52px;
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
/* ChatPanel 深色模式（非 scoped） */
html.dark .chat-overlay {
  background: rgba(0, 0, 0, 0.5) !important;
  backdrop-filter: blur(3px) !important;
}
html.dark .chat-panel {
  --panel-bg: #111827 !important;
  --panel-bg2: #1e293b !important;
  --panel-border: rgba(255, 255, 255, 0.06) !important;
  --panel-shadow: rgba(0, 0, 0, 0.5) !important;
  --panel-text: #e5e7eb !important;
  --panel-text-secondary: #64748b !important;
  --panel-hover-bg: rgba(255, 255, 255, 0.05) !important;
  --panel-accent: #818cf8 !important;
}
html.dark .header-online-count {
  color: #34d399 !important;
}
html.dark .header-btn:hover {
  background: rgba(255,255,255,0.06) !important;
  color: #e2e8f0 !important;
}
html.dark .header-btn-close:hover {
  background: rgba(239,68,68,0.12) !important;
  color: #f87171 !important;
}
html.dark .back-btn {
  background: rgba(255,255,255,0.06) !important;
}
html.dark .back-btn:hover {
  background: rgba(99,102,241,0.15) !important;
  color: #818cf8 !important;
}
html.dark .back-status {
  background: rgba(255,255,255,0.06) !important;
  color: #94a3b8 !important;
}
html.dark .back-status.online {
  background: rgba(16,185,129,0.12) !important;
  color: #34d399 !important;
}
html.dark .chat-panel-body .n-tabs .n-tabs-nav {
  border-top-color: rgba(255,255,255,0.06) !important;
}
html.dark .chat-panel-body .n-tabs-tab {
  color: #64748b !important;
}
html.dark .chat-panel-body .n-tabs-tab--active {
  color: #818cf8 !important;
}
html.dark .contact-group-title {
  color: #64748b !important;
}
html.dark .contact-chat-icon {
  color: #475569 !important;
}
html.dark .contact-item:hover .contact-chat-icon {
  color: #818cf8 !important;
}
html.dark .empty-icon-wrap {
  background: #1e293b !important;
  color: #475569 !important;
}
html.dark .empty-title {
  color: #94a3b8 !important;
}
html.dark .empty-hint {
  color: #64748b !important;
}
html.dark .online-dot {
  border-color: #111827 !important;
}
html.dark .chat-back-bar {
  border-bottom-color: rgba(255,255,255,0.06) !important;
}
</style>

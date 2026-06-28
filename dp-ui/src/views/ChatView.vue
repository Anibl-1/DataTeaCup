<template>
  <div class="chat-page">
    <!-- 左侧面板 -->
    <div class="chat-sidebar">
      <!-- 搜索栏 -->
      <div class="sidebar-search">
        <n-input
          v-model:value="searchKeyword"
          :placeholder="t('chat.searchPlaceholder')"
          size="small"
          clearable
          round
          @clear="searchKeyword = ''"
        >
          <template #prefix>
            <n-icon size="14" color="#999"><SearchOutline /></n-icon>
          </template>
        </n-input>
      </div>

      <!-- 标签切换 -->
      <div class="sidebar-tabs">
        <div
          :class="['tab-btn', { active: activeTab === 'chat' }]"
          @click="activeTab = 'chat'"
        >
          <n-icon size="18"><ChatbubblesOutline /></n-icon>
          <span>{{ t('chat.messages') }}</span>
          <n-badge v-if="chatStore.totalUnread > 0" :value="chatStore.totalUnread" :max="99" class="tab-badge" />
        </div>
        <div
          :class="['tab-btn', { active: activeTab === 'contacts' }]"
          @click="activeTab = 'contacts'"
        >
          <n-icon size="18"><PeopleOutline /></n-icon>
          <span>{{ t('chat.contacts') }}</span>
        </div>
        <div
          :class="['tab-btn', { active: activeTab === 'group' }]"
          @click="activeTab = 'group'"
        >
          <n-icon size="18"><PeopleCircleOutline /></n-icon>
          <span>{{ t('chat.groups') }}</span>
        </div>
      </div>

      <!-- 会话列表 -->
      <div class="sidebar-body">
        <template v-if="activeTab === 'chat'">
          <div class="conv-list">
            <div
              v-for="conv in filteredConversations"
              :key="conv.id"
              :class="['conv-item', { active: chatStore.currentConversationId === conv.id }]"
              @click="chatStore.selectConversation(conv.id)"
            >
              <div class="conv-avatar-wrap">
                <n-avatar
                  round
                  :size="42"
                  :style="{ ...getAvatarStyle(conv.avatar, conv.name), fontSize: '16px' }"
                >
                  {{ getAvatarContent(conv.avatar, conv.name) }}
                </n-avatar>
                <span v-if="conv.type === 'group'" class="group-badge">
                  <n-icon size="10"><PeopleOutline /></n-icon>
                </span>
                <span
                  v-else
                  class="online-dot"
                  :class="{ online: isUserOnline(conv) }"
                />
              </div>
              <div class="conv-info">
                <div class="conv-top">
                  <span class="conv-name">{{ getConvDisplayName(conv) }}</span>
                  <span class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
                </div>
                <div class="conv-bottom">
                  <span class="conv-msg">{{ conv.lastMessage || t('chat.noMessages') }}</span>
                  <n-badge
                    v-if="conv.unreadCount > 0"
                    :value="conv.unreadCount"
                    :max="99"
                    type="error"
                  />
                </div>
              </div>
            </div>
            <div v-if="filteredConversations.length === 0" class="empty-list">
              <n-icon size="36" color="#d4d4d8"><ChatbubblesOutline /></n-icon>
              <span>{{ searchKeyword ? t('chat.noMatchConv') : t('chat.noConversations') }}</span>
            </div>
          </div>
        </template>

        <!-- 联系人列表 -->
        <template v-else-if="activeTab === 'contacts'">
          <div class="contacts-list">
            <div
              v-for="user in filteredContacts"
              :key="user.id"
              class="contact-item"
              @click="startPrivateChat(user)"
            >
              <div class="contact-avatar-wrap">
                <n-avatar
                  round
                  :size="38"
                  :style="getAvatarStyle(user.avatar, user.nickname)"
                >
                  {{ getAvatarContent(user.avatar, user.nickname) }}
                </n-avatar>
                <span class="online-dot" :class="{ online: isUserIdOnline(user.id) }" />
              </div>
              <div class="contact-info">
                <span class="contact-name">{{ user.nickname }}</span>
                <span class="contact-status">{{ isUserIdOnline(user.id) ? t('chat.online') : t('chat.offline') }}</span>
              </div>
            </div>
            <div v-if="filteredContacts.length === 0" class="empty-list">
              <n-icon size="36" color="#d4d4d8"><PeopleOutline /></n-icon>
              <span>{{ searchKeyword ? t('chat.noMatchContacts') : t('chat.noContacts') }}</span>
            </div>
          </div>
        </template>

        <!-- 群组列表 -->
        <template v-else>
          <div class="group-list">
            <div
              v-for="conv in groupConversations"
              :key="conv.id"
              class="conv-item"
              @click="chatStore.selectConversation(conv.id)"
            >
              <n-avatar
                round
                :size="42"
                :style="getAvatarStyle(conv.avatar, conv.name)"
              >
                {{ getAvatarContent(conv.avatar, conv.name) }}
              </n-avatar>
              <div class="conv-info">
                <div class="conv-top">
                  <span class="conv-name">{{ getConvDisplayName(conv) }}</span>
                  <span class="conv-member-count">{{ conv.members?.length || 0 }}{{ t('chat.people') }}</span>
                </div>
                <div class="conv-bottom">
                  <span class="conv-msg">{{ conv.lastMessage || t('chat.noMessages') }}</span>
                </div>
              </div>
            </div>
            <div v-if="groupConversations.length === 0" class="empty-list">
              <n-icon size="36" color="#d4d4d8"><PeopleCircleOutline /></n-icon>
              <span>{{ searchKeyword ? t('chat.noMatchGroups') : t('chat.noGroups') }}</span>
            </div>
          </div>
        </template>
      </div>

      <!-- 底部操作栏 -->
      <div class="sidebar-footer">
        <n-button text size="small" @click="showCreateDialog = true">
          <n-icon size="18"><AddCircleOutline /></n-icon>
        </n-button>
        <n-button text size="small" @click="showCreateGroupDialog = true">
          <n-icon size="18"><PeopleCircleOutline /></n-icon>
        </n-button>
      </div>
    </div>

    <!-- 右侧聊天区 -->
    <div class="chat-main">
      <template v-if="chatStore.currentConversation">
        <!-- 聊天头部 -->
        <div class="chat-header">
          <div class="chat-header-left">
            <n-avatar
              round
              :size="36"
              :style="getAvatarStyle(chatStore.currentConversation.avatar, chatStore.currentConversation.name)"
            >
              {{ getAvatarContent(chatStore.currentConversation.avatar, chatStore.currentConversation.name) }}
            </n-avatar>
            <div class="chat-header-info">
              <div class="chat-header-name">
                {{ chatStore.currentConversation.name }}
                <span v-if="chatStore.currentConversation.type === 'group'" class="chat-type-tag">{{ t('chat.groupChat') }}</span>
              </div>
              <div class="chat-header-meta">
                <template v-if="chatStore.currentConversation.type === 'group'">
                  {{ chatStore.currentConversation.members?.length || 0 }} {{ t('chat.members') }}
                </template>
                <template v-else>
                  <span class="status-text" :class="{ online: isUserOnline(chatStore.currentConversation) }">
                    {{ isUserOnline(chatStore.currentConversation) ? t('chat.online') : t('chat.offline') }}
                  </span>
                </template>
              </div>
            </div>
          </div>
          <div class="chat-header-actions">
            <n-button text size="small" @click="showChatInfo = !showChatInfo">
              <n-icon size="18"><InformationCircleOutline /></n-icon>
            </n-button>
          </div>
        </div>

        <!-- 消息区域 -->
        <div class="chat-messages-wrapper">
          <div ref="messageAreaRef" class="chat-messages" @scroll="handleScroll">
            <div v-if="loadingMore" class="loading-indicator">
              <n-spin size="small" />
              <span>{{ t('chat.loadingHistory') }}</span>
            </div>

            <!-- 骨架屏加载状态 -->
            <template v-if="messagesLoading && chatStore.messages.length === 0">
              <div class="skeleton-messages">
                <div v-for="i in 4" :key="i" :class="['skeleton-row', { 'skeleton-self': i % 2 === 0 }]">
                  <div class="skeleton-avatar" />
                  <div class="skeleton-bubble">
                    <div class="skeleton-line" :style="{ width: (50 + i * 10) + '%' }" />
                    <div v-if="i % 2 === 1" class="skeleton-line" :style="{ width: '40%' }" />
                  </div>
                </div>
              </div>
            </template>

            <!-- 日期分隔线 + 消息 -->
            <template v-for="(msg, idx) in chatStore.messages" :key="msg.id || msg.localId">
            <div
              v-if="shouldShowDateDivider(idx)"
              class="date-divider"
            >
              <span>{{ formatDateDivider(msg.sendTime) }}</span>
            </div>

            <div
              :class="['msg-row', { 'msg-self': isSelf(msg) }]"
            >
              <n-avatar
                v-if="!isSelf(msg)"
                round
                :size="32"
                :style="{ ...getAvatarStyle(getSenderAvatar(msg.senderId), msg.senderName), flexShrink: 0 }"
              >
                {{ getAvatarContent(getSenderAvatar(msg.senderId), msg.senderName) }}
              </n-avatar>

              <div class="msg-content-wrap">
                <div v-if="!isSelf(msg) && chatStore.currentConversation?.type === 'group'" class="msg-sender">
                  {{ msg.senderName }}
                </div>
                <div :class="['msg-bubble', { self: isSelf(msg) }]">
                  <!-- 文本消息 -->
                  <template v-if="msg.contentType === 'text'">
                    <span class="msg-text" v-html="renderMessageText(msg.content)" />
                  </template>
                  <!-- 图片消息 -->
                  <template v-else-if="msg.contentType === 'image'">
                    <n-image
                      :src="msg.fileUrl || msg.content"
                      :preview-src="msg.fileUrl || msg.content"
                      :img-props="{ style: 'max-width: 220px; max-height: 220px; border-radius: 4px; cursor: pointer;' }"
                      object-fit="contain"
                    />
                  </template>
                  <!-- 文件消息 -->
                  <template v-else-if="msg.contentType === 'file'">
                    <div class="file-card">
                      <n-icon size="28" color="#3b82f6"><component :is="resolveFileIconComponent(msg.fileName || '')" /></n-icon>
                      <div class="file-detail">
                        <span class="file-name">{{ msg.fileName || t('chat.file') }}</span>
                        <span class="file-size">{{ formatFileSize(msg.fileSize) }}</span>
                      </div>
                      <a :href="msg.fileUrl" target="_blank" rel="noopener noreferrer" class="file-dl">
                        <n-icon size="16"><DownloadOutline /></n-icon>
                      </a>
                    </div>
                  </template>
                </div>
                <div class="msg-meta-row">
                  <template v-if="isSelf(msg)">
                    <span v-if="msg.status === 'sending'" class="msg-read-status sending"><n-spin :size="10" /></span>
                    <span v-else-if="msg.status === 'failed'" class="msg-read-status failed" @click="chatStore.resendMsg(msg.localId ?? '')">发送失败</span>
                    <span v-else-if="msg.readBy && msg.readBy.length > 0 && msg.readBy.some(uid => uid !== msg.senderId)" class="msg-read-status read">已读</span>
                    <span v-else class="msg-read-status unread">未读</span>
                  </template>
                  <span class="msg-time">{{ formatMsgTime(msg.sendTime) }}</span>
                </div>
              </div>

              <n-avatar
                v-if="isSelf(msg)"
                round
                :size="32"
                :style="{ ...(currentUserAvatarPreset ? { background: currentUserAvatarPreset.gradient } : { backgroundColor: '#3b82f6' }), flexShrink: 0 }"
              >
                {{ currentUserAvatarPreset ? currentUserAvatarPreset.icon : t('chat.me') }}
              </n-avatar>
            </div>
          </template>

          <div v-if="chatStore.messages.length === 0 && !messagesLoading" class="no-messages">
            <span>{{ t('chat.noMessagesYet') }}</span>
          </div>
          </div>

          <!-- 浮动新消息提示按钮 -->
          <div
            v-if="newMessageCount > 0"
            class="new-messages-float"
            @click="scrollToBottom(true)"
          >
            <n-icon size="14"><ArrowDownOutline /></n-icon>
            <span>{{ t('chat.newMessages', { count: newMessageCount }) }}</span>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="chat-input-area">
          <div class="input-toolbar">
            <n-popover trigger="click" placement="top-start" :show-arrow="false" style="padding: 0;">
              <template #trigger>
                <n-button text size="small" class="toolbar-btn">
                  <n-icon size="20"><HappyOutline /></n-icon>
                </n-button>
              </template>
              <div class="emoji-panel">
                <div class="emoji-tabs">
                  <span
                    v-for="cat in emojiCategories"
                    :key="cat.key"
                    :class="['emoji-tab-btn', { active: activeEmojiTab === cat.key }]"
                    @click="activeEmojiTab = cat.key"
                  >{{ cat.label }}</span>
                </div>
                <div class="emoji-grid">
                  <span
                    v-for="emoji in emojiCategories.find(c => c.key === activeEmojiTab)?.emojis || []"
                    :key="emoji"
                    class="emoji-item"
                    @click="insertEmoji(emoji)"
                  >{{ emoji }}</span>
                </div>
              </div>
            </n-popover>
            <n-upload :show-file-list="false" accept="image/*" @change="handleImageUpload">
              <n-button text size="small" class="toolbar-btn">
                <n-icon size="20"><ImageOutline /></n-icon>
              </n-button>
            </n-upload>
            <n-upload :show-file-list="false" @change="handleFileUpload">
              <n-button text size="small" class="toolbar-btn">
                <n-icon size="20"><AttachOutline /></n-icon>
              </n-button>
            </n-upload>
          </div>
          <div class="input-box">
            <n-input
              v-model:value="inputText"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 5 }"
              :placeholder="t('chat.inputPlaceholder')"
              @keydown="handleKeydown"
            />
          </div>
          <div class="input-footer">
            <span class="input-hint">{{ t('chat.shiftEnterHint') }}</span>
            <n-button
              type="primary"
              size="small"
              :disabled="!inputText.trim()"
              @click="sendText"
            >
              {{ t('chat.send') }}
            </n-button>
          </div>
        </div>
      </template>

      <!-- 空状态 -->
      <template v-else>
        <div class="empty-chat">
          <div class="empty-chat-illustration">
            <svg width="160" height="140" viewBox="0 0 160 140" fill="none" xmlns="http://www.w3.org/2000/svg">
              <!-- 大气泡 -->
              <rect x="20" y="20" width="100" height="60" rx="12" fill="#e8f0fe" stroke="#3b82f6" stroke-width="1.5"/>
              <circle cx="46" cy="50" r="4" fill="#93c5fd"/>
              <circle cx="62" cy="50" r="4" fill="#93c5fd"/>
              <circle cx="78" cy="50" r="4" fill="#93c5fd"/>
              <!-- 小气泡 -->
              <rect x="60" y="70" width="80" height="44" rx="10" fill="#dbeafe" stroke="#60a5fa" stroke-width="1.2"/>
              <rect x="74" y="84" width="40" height="6" rx="3" fill="#93c5fd"/>
              <rect x="74" y="96" width="28" height="6" rx="3" fill="#bfdbfe"/>
              <!-- 装饰圆点 -->
              <circle cx="148" cy="30" r="5" fill="#bfdbfe" opacity="0.6"/>
              <circle cx="10" cy="90" r="4" fill="#dbeafe" opacity="0.5"/>
              <circle cx="140" cy="110" r="3" fill="#93c5fd" opacity="0.4"/>
            </svg>
          </div>
          <p class="empty-chat-title">{{ t('chat.instantMessaging') }}</p>
          <p class="empty-chat-desc">{{ t('chat.selectConversation') }}</p>
          <n-button type="primary" size="small" @click="showCreateDialog = true">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            {{ t('chat.startChat') }}
          </n-button>
        </div>
      </template>
    </div>

    <!-- 右侧信息面板（可选展开） -->
    <transition name="slide-right">
      <div v-if="showChatInfo && chatStore.currentConversation" class="chat-info-panel">
        <div class="info-panel-header">
          <span>{{ chatStore.currentConversation.type === 'group' ? t('chat.groupInfo') : t('chat.chatInfo') }}</span>
          <n-button text size="small" @click="showChatInfo = false">
            <n-icon size="16"><CloseOutline /></n-icon>
          </n-button>
        </div>
        <div class="info-panel-body">
          <div class="info-avatar-section">
            <n-avatar
              round
              :size="64"
              :style="getAvatarStyle(chatStore.currentConversation.avatar, chatStore.currentConversation.name)"
            >
              {{ getAvatarContent(chatStore.currentConversation.avatar, chatStore.currentConversation.name) }}
            </n-avatar>
            <div class="info-conv-name">{{ getConvDisplayName(chatStore.currentConversation) }}</div>
            <div class="info-conv-type">
              {{ chatStore.currentConversation.type === 'group' ? t('chat.groupChat') : t('chat.privateChat') }}
            </div>
          </div>
          <template v-if="chatStore.currentConversation.type === 'group' && chatStore.currentConversation.members">
            <div class="info-section-title">{{ t('chat.members') }} ({{ chatStore.currentConversation.members.length }})</div>
            <div class="info-members">
              <div
                v-for="member in chatStore.currentConversation.members"
                :key="member.userId"
                class="info-member-item"
              >
                <n-avatar
                  round
                  :size="28"
                  :style="getAvatarStyle(member.avatar, member.nickname)"
                >
                  {{ getAvatarContent(member.avatar, member.nickname) }}
                </n-avatar>
                <span class="info-member-name">{{ member.nickname }}</span>
              </div>
            </div>
          </template>
        </div>
      </div>
    </transition>

    <!-- 创建私聊对话框 -->
    <n-modal v-model:show="showCreateDialog" preset="card" :title="t('chat.startChat')" :style="{ width: '400px' }">
      <div v-if="allUsers.length > 0" class="create-chat-users">
        <div
          v-for="user in allUsers"
          :key="user.id"
          class="create-chat-user-item"
          @click="startPrivateChat(user)"
        >
          <n-avatar
            round
            :size="32"
            :style="getAvatarStyle(user.avatar, user.nickname)"
          >
            {{ getAvatarContent(user.avatar, user.nickname) }}
          </n-avatar>
          <span>{{ user.nickname }}</span>
          <span class="user-status-tag" :class="{ online: isUserIdOnline(user.id) }">
            {{ isUserIdOnline(user.id) ? t('chat.online') : t('chat.offline') }}
          </span>
        </div>
      </div>
      <div v-else class="create-chat-empty">
        <p>{{ t('chat.noAvailableContacts') }}</p>
        <p class="create-chat-hint">{{ t('chat.ensureBackendRunning') }}</p>
      </div>
    </n-modal>

    <!-- 创建群聊对话框 -->
    <n-modal v-model:show="showCreateGroupDialog" preset="card" :title="t('chat.createGroup')" :style="{ width: '400px' }">
      <n-input v-model:value="newGroupName" :placeholder="t('chat.groupName')" style="margin-bottom: 12px" />
      <n-select
        v-model:value="newGroupMembers"
        multiple
        filterable
        :placeholder="t('chat.selectGroupMembers')"
        :options="allUsers.map(u => ({ label: u.nickname, value: u.id }))"
      />
      <template #footer>
        <n-button type="primary" size="small" :disabled="!newGroupName.trim()" @click="createGroupChat">
          {{ t('chat.create') }}
        </n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  NInput, NButton, NIcon, NAvatar, NBadge, NUpload,
  NImage, NPopover, NModal, NSpin, NSelect, useMessage
} from 'naive-ui'
import {
  SearchOutline, ChatbubblesOutline, PeopleOutline, PeopleCircleOutline,
  AddCircleOutline, AddOutline, InformationCircleOutline, CloseOutline,
  HappyOutline, ImageOutline, AttachOutline, DocumentOutline, DownloadOutline,
  ArrowDownOutline,
  DocumentTextOutline, FolderOpenOutline, CodeSlashOutline, VideocamOutline, MusicalNotesOutline
} from '@vicons/ionicons5'
import { getFileIcon } from '@/utils/fileIcon'
import type { FileIconName } from '@/utils/fileIcon'
import { useChatStore } from '@/stores/chat'
import { useI18n } from '@/i18n'
import { useUserStore } from '@/stores/user'
import { avatarPresets } from '@/constants/avatarPresets'
import { getOnlineUsers, createConversation } from '@/api/chat'
import request from '@/api/request'
import type { OnlineUser, Conversation } from '@/types/chat'
import type { User } from '@/types/user'
import type { UploadFileInfo } from 'naive-ui'

// 文件图标组件映射
const fileIconComponents: Record<FileIconName, any> = {
  ImageOutline,
  DocumentTextOutline,
  FolderOpenOutline,
  CodeSlashOutline,
  VideocamOutline,
  MusicalNotesOutline,
  DocumentOutline,
}

function resolveFileIconComponent(fileName: string) {
  return fileIconComponents[getFileIcon(fileName)]
}

const route = useRoute()
const chatStore = useChatStore()
const userStore = useUserStore()
const message = useMessage()
const { t } = useI18n()

const searchKeyword = ref('')
const activeTab = ref<'chat' | 'contacts' | 'group'>('chat')
const inputText = ref('')
const messageAreaRef = ref<HTMLDivElement | null>(null)
const loadingMore = ref(false)
const hasMore = ref(true)
let isAutoScrolling = false
const messagesLoading = ref(true)
const newMessageCount = ref(0)
const showChatInfo = ref(false)
const showCreateDialog = ref(false)
const showCreateGroupDialog = ref(false)
const newGroupName = ref('')
const newGroupMembers = ref<number[]>([])
const onlineUsers = ref<OnlineUser[]>([])
const allUsers = ref<User[]>([])

const emojiCategories = [
  { label: '😀', key: 'smileys', emojis: [
    '😀','😃','😄','😁','😆','😅','🤣','😂','🙂','😉','😊','😇','🥰','😍','🤩','😘',
    '😗','😚','😙','🥲','😋','😛','😜','🤪','😝','🤑','🤗','🤭','🫢','🤫','🤔','🫡',
    '🤐','🤨','😐','😑','😶','🫥','😏','😒','🙄','😬','🤥','😌','😔','😪','🤤','😴',
    '😷','🤒','🤕','🤢','🤮','🥵','🥶','🥴','😵','🤯','🤠','🥳','🥸','😎','🤓','🧐'
  ]},
  { label: '👋', key: 'gestures', emojis: [
    '👋','🤚','🖐️','✋','🖖','🫱','🫲','🫳','🫴','👌','🤌','🤏','✌️','🤞','🫰','🤟',
    '🤘','🤙','👈','👉','👆','🖕','👇','☝️','🫵','👍','👎','✊','👊','🤛','🤜','👏',
    '🙌','🫶','👐','🤲','🤝','🙏','💪','🦾','🦿','🦵','🦶','👂','🦻','👃','🧠','🫀'
  ]},
  { label: '❤️', key: 'symbols', emojis: [
    '❤️','🧡','💛','💚','💙','💜','🖤','🤍','🤎','💔','❤️‍🔥','❤️‍🩹','💕','💞','💓','💗',
    '💖','💘','💝','💟','☮️','✝️','☪️','🕉️','☸️','✡️','🔯','🕎','☯️','☦️','🛐','⛎',
    '♈','♉','♊','♋','♌','♍','♎','♏','♐','♑','♒','♓','🆔','⚛️','✅','❌'
  ]},
  { label: '🐱', key: 'animals', emojis: [
    '🐶','🐱','🐭','🐹','🐰','🦊','🐻','🐼','🐻‍❄️','🐨','🐯','🦁','🐮','🐷','🐸','🐵',
    '🙈','🙉','🙊','🐒','🐔','🐧','🐦','🐤','🐣','🐥','🦆','🦅','🦉','🦇','🐺','🐗',
    '🐴','🦄','🐝','🪱','🐛','🦋','🐌','🐞','🐜','🪰','🪲','🪳','🦟','🦗','🕷️','🦂'
  ]},
  { label: '🍔', key: 'food', emojis: [
    '🍏','🍎','🍐','🍊','🍋','🍌','🍉','🍇','🍓','🫐','🍈','🍒','🍑','🥭','🍍','🥥',
    '🥝','🍅','🍆','🥑','🥦','🥬','🥒','🌶️','🫑','🌽','🥕','🫒','🧄','🧅','🥔','🍠',
    '🍔','🍟','🍕','🌭','🥪','🌮','🌯','🫔','🥙','🧆','🥚','🍳','🥘','🍲','🫕','🥣'
  ]},
  { label: '🚀', key: 'objects', emojis: [
    '🚀','✈️','🚗','🚕','🚙','🚌','🚎','🏎️','🚓','🚑','🚒','🚐','🛻','🚚','🚛','🚜',
    '⚽','🏀','🏈','⚾','🥎','🎾','🏐','🏉','🥏','🎱','🪀','🏓','🏸','🏒','🥅','⛳',
    '💻','🖥️','🖨️','⌨️','🖱️','🖲️','💾','💿','📀','📱','📲','☎️','📞','📟','📠','🔋'
  ]}
]
const activeEmojiTab = ref('smileys')
const blobUrls: string[] = [] // track blob URLs for cleanup

// 搜索关键词（小写）
const searchLower = computed(() => searchKeyword.value.trim().toLowerCase())

// 过滤后的会话列表
const filteredConversations = computed(() => {
  const kw = searchLower.value
  if (!kw) return chatStore.conversations
  return chatStore.conversations.filter(c =>
    c.name?.toLowerCase().includes(kw) ||
    c.lastMessage?.toLowerCase().includes(kw)
  )
})

// 过滤后的联系人列表
const filteredContacts = computed(() => {
  const kw = searchLower.value
  if (!kw) return allUsers.value
  return allUsers.value.filter(u =>
    u.nickname?.toLowerCase().includes(kw) ||
    u.username?.toLowerCase().includes(kw)
  )
})

// 过滤后的群组会话
const groupConversations = computed(() => {
  const groups = chatStore.conversations.filter(c => c.type === 'group')
  const kw = searchLower.value
  if (!kw) return groups
  return groups.filter(c => c.name?.toLowerCase().includes(kw))
})

let onlineUsersTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  if (route.path.includes('/contacts')) {
    activeTab.value = 'contacts'
  }
  chatStore.loadConversations()
  await loadContacts()
  messagesLoading.value = false
  // 定时刷新在线状态（30秒）
  onlineUsersTimer = setInterval(async () => {
    try {
      const res = await getOnlineUsers()
      onlineUsers.value = res.data || []
    } catch { /* 静默 */ }
  }, 30000)
})

onBeforeUnmount(() => {
  // 清理 blob URLs 防止内存泄漏
  blobUrls.forEach(url => URL.revokeObjectURL(url))
  blobUrls.length = 0
  // 停止在线状态轮询
  if (onlineUsersTimer) {
    clearInterval(onlineUsersTimer)
    onlineUsersTimer = null
  }
})

/** 检查滚动位置是否接近底部（100px以内） */
function isNearBottom(): boolean {
  const el = messageAreaRef.value
  if (!el) return true
  return el.scrollHeight - el.scrollTop - el.clientHeight < 100
}

// 智能滚动：新消息到达时根据滚动位置决定行为
watch(() => chatStore.messages.length, (newLen, oldLen) => {
  if (newLen > oldLen) {
    if (isNearBottom()) {
      scrollToBottom()
    } else {
      newMessageCount.value += newLen - oldLen
    }
  }
})

// 切换会话时重置状态并滚动到底部
watch(() => chatStore.currentConversationId, async (newId) => {
  newMessageCount.value = 0
  hasMore.value = true
  if (newId) {
    messagesLoading.value = true
    // selectConversation 已 await loadMessages，此处等消息加载完成后关闭骨架屏
    await nextTick()
    messagesLoading.value = false
    scrollToBottom(false)
  }
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

function scrollToBottom(smooth = true) {
  newMessageCount.value = 0
  nextTick(() => {
    const el = messageAreaRef.value
    if (el) {
      isAutoScrolling = true
      if (smooth) {
        el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
      } else {
        el.scrollTop = el.scrollHeight
      }
      setTimeout(() => { isAutoScrolling = false }, 150)
    }
  })
}

async function handleScroll() {
  const el = messageAreaRef.value
  if (!el || isAutoScrolling) return

  // 如果滚动到底部附近，清除新消息计数
  if (isNearBottom()) {
    newMessageCount.value = 0
  }

  // 滚动到顶部加载更多历史消息
  if (el.scrollTop < 50 && !loadingMore.value && hasMore.value) {
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
}

function isSelf(msg: { senderId: number }): boolean {
  return msg.senderId === userStore.userInfo?.id
}

function isUserOnline(conv: Conversation): boolean {
  if (conv.type !== 'private') return false
  const member = conv.members?.find(m => m.userId !== userStore.userInfo?.id)
  if (!member) return false
  return onlineUsers.value.some(u => u.userId === member.userId && u.status === 'online')
}

/** 检查某个用户ID是否在线 */
function isUserIdOnline(userId: number): boolean {
  return onlineUsers.value.some(u => u.userId === userId && u.status === 'online')
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendText()
  }
}

function sendText() {
  const text = inputText.value.trim()
  if (!text || !chatStore.currentConversationId) return
  chatStore.sendMsg(chatStore.currentConversationId, 'text', text)
  inputText.value = ''
}

function insertEmoji(emoji: string) {
  inputText.value += emoji
}

function handleImageUpload({ file }: { file: UploadFileInfo }) {
  if (!chatStore.currentConversationId || !file.file) return
  const url = URL.createObjectURL(file.file)
  blobUrls.push(url)
  chatStore.sendMsg(
    chatStore.currentConversationId, 'image',
    file.name || 'image',
    url,
    file.name || 'image',
    file.file.size || 0
  )
}

function handleFileUpload({ file }: { file: UploadFileInfo }) {
  if (!chatStore.currentConversationId || !file.file) return
  const url = URL.createObjectURL(file.file)
  blobUrls.push(url)
  chatStore.sendMsg(
    chatStore.currentConversationId, 'file',
    file.name || 'file',
    url,
    file.name || 'file',
    file.file.size || 0
  )
}

async function startPrivateChat(user: User | OnlineUser) {
  const userId = 'userId' in user ? user.userId : user.id
  const userName = 'nickname' in user ? user.nickname : ('username' in user ? (user as any).username : t('chat.user'))
  try {
    const res = await createConversation({ type: 'private', memberIds: [userId] })
    if (res.data) {
      await chatStore.loadConversations()
      chatStore.selectConversation(res.data.id)
      activeTab.value = 'chat'
      message.success(t('chat.conversationCreated', { name: userName }))
    }
  } catch (e: any) {
    const errMsg = e?.message || ''
    if (errMsg.includes('network') || errMsg.includes('ERR_')) {
      message.warning(t('chat.backendNotRunning'))
    } else {
      message.warning(errMsg || t('chat.createConvFailed'))
    }
  } finally {
    showCreateDialog.value = false
  }
}

async function createGroupChat() {
  try {
    const memberIds = newGroupMembers.value
    if (memberIds.length === 0) {
      message.warning(t('chat.selectAtLeastOne'))
      return
    }
    const res = await createConversation({ type: 'group', name: newGroupName.value, memberIds })
    const conv = res?.data || (res as any)
    if (conv?.id) {
      await chatStore.loadConversations()
      chatStore.selectConversation(conv.id)
      activeTab.value = 'chat'
      message.success(t('chat.groupCreated'))
      showCreateGroupDialog.value = false
      newGroupName.value = ''
      newGroupMembers.value = []
    } else {
      message.warning(t('chat.createGroupFailed'))
    }
  } catch (e: any) {
    const errMsg = e?.message || ''
    if (errMsg.includes('network') || errMsg.includes('ERR_')) {
      message.warning(t('chat.backendNotRunningGroup'))
    } else {
      message.error(errMsg || t('chat.createGroupFailed'))
    }
  }
}

// 渲染消息文本（URL 自动链接化）
function renderMessageText(text: string): string {
  if (!text) return ''
  const escaped = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  return escaped.replace(
    /(https?:\/\/[^\s<]+)/g,
    '<a href="$1" target="_blank" rel="noopener noreferrer" style="color:#3b82f6;text-decoration:underline;">$1</a>'
  )
}

// 是否显示日期分隔线
function shouldShowDateDivider(idx: number): boolean {
  if (idx === 0) return true
  const curr = new Date(chatStore.messages[idx]?.sendTime || 0)
  const prev = new Date(chatStore.messages[idx - 1]?.sendTime || 0)
  // 超过5分钟显示分隔
  return curr.getTime() - prev.getTime() > 5 * 60 * 1000
}

function formatDateDivider(timeStr: string): string {
  const date = new Date(timeStr)
  if (isNaN(date.getTime())) return ''
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const hhmm = `${pad(date.getHours())}:${pad(date.getMinutes())}`
  if (date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth() && date.getDate() === now.getDate()) {
    return `${t('chat.today')} ${hhmm}`
  }
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (date.getFullYear() === yesterday.getFullYear() && date.getMonth() === yesterday.getMonth() && date.getDate() === yesterday.getDate()) {
    return `${t('chat.yesterday')} ${hhmm}`
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${hhmm}`
}

function formatMsgTime(timeStr: string): string {
  const date = new Date(timeStr)
  if (isNaN(date.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function formatTime(timeStr?: string): string {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  if (isNaN(date.getTime())) return ''
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const hhmm = `${pad(date.getHours())}:${pad(date.getMinutes())}`
  if (date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth() && date.getDate() === now.getDate()) {
    return hhmm
  }
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function formatFileSize(bytes?: number): string {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

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
    'linear-gradient(135deg, #f5576c 0%, #ff6f91 100%)',
    'linear-gradient(135deg, #0acffe 0%, #495aff 100%)',
    'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)',
    'linear-gradient(135deg, #fbc2eb 0%, #a6c1ee 100%)',
    'linear-gradient(135deg, #fdcbf1 0%, #e6dee9 100%)',
    'linear-gradient(135deg, #a1c4fd 0%, #c2e9fb 100%)',
    'linear-gradient(135deg, #d4fc79 0%, #96e6a1 100%)',
    'linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%)'
  ]
  let hash = 0
  const n = name || ''
  for (let i = 0; i < n.length; i++) {
    hash = n.charCodeAt(i) + ((hash << 5) - hash)
  }
  return gradients[Math.abs(hash) % gradients.length]!
}

/** 解析头像预设：根据avatar id返回匹配的预设，未匹配返回null */
function resolveAvatarPreset(avatarId?: string | null) {
  if (!avatarId) return null
  return avatarPresets.find(p => p.id === avatarId) || null
}

/** 获取头像样式：优先使用avatar预设，回退到名字渐变 */
function getAvatarStyle(avatarId?: string | null, name?: string) {
  const preset = resolveAvatarPreset(avatarId)
  if (preset) return { background: preset.gradient }
  return { background: getAvatarColor(name || '') }
}

/** 获取头像显示内容：优先使用avatar预设图标，回退到首字母 */
function getAvatarContent(avatarId?: string | null, name?: string) {
  const preset = resolveAvatarPreset(avatarId)
  if (preset) return preset.icon
  return name?.charAt(0) || '用'
}

/** 获取会话显示名称：优先用 name，私聊回退到对方昵称 */
function getConvDisplayName(conv: Conversation): string {
  if (conv.name) return conv.name
  if (conv.type === 'private' && conv.members?.length) {
    const peer = conv.members.find(m => m.userId !== userStore.userInfo?.id)
    if (peer?.nickname) return peer.nickname
    return conv.members[0]?.nickname || '未命名'
  }
  return '未命名会话'
}

/** 当前用户的头像预设 */
const currentUserAvatarPreset = computed(() => resolveAvatarPreset(userStore.userInfo?.avatar))

/** 根据senderId从当前会话成员中查找avatar */
function getSenderAvatar(senderId: number): string | undefined {
  const members = chatStore.currentConversation?.members
  if (!members) return undefined
  const member = members.find(m => m.userId === senderId)
  return member?.avatar
}
</script>

<style scoped>
/* ==================== 整体布局 ==================== */
.chat-page {
  display: flex;
  height: calc(100vh - 178px);
  background: var(--chat-bg, #f5f5f5);
  overflow: hidden;
  border-radius: 8px;
}

/* ==================== 左侧面板 ==================== */
.chat-sidebar {
  width: 280px;
  background: var(--sidebar-bg, #fff);
  border-right: 1px solid var(--border-color, #e8e8e8);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-search {
  padding: var(--dp-spacing-md) var(--dp-spacing-md) var(--dp-spacing-sm);
}

.sidebar-tabs {
  display: flex;
  padding: 0 var(--dp-spacing-sm);
  gap: 2px;
  border-bottom: 1px solid var(--border-color, #f0f0f0);
}

.tab-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--dp-spacing-xs);
  padding: var(--dp-spacing-sm) 0;
  font-size: var(--dp-font-xs);
  color: #888;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 150ms;
  position: relative;
}

.tab-btn:hover { color: #3b82f6; }
.tab-btn.active {
  color: #3b82f6;
  font-weight: 500;
  border-bottom-color: #3b82f6;
}

.tab-badge {
  position: absolute;
  top: 2px;
  right: var(--dp-spacing-sm);
}

.sidebar-body {
  flex: 1;
  overflow-y: auto;
}

.sidebar-footer {
  display: flex;
  justify-content: center;
  gap: var(--dp-spacing-md);
  padding: var(--dp-spacing-sm);
  border-top: 1px solid var(--border-color, #f0f0f0);
}

/* ==================== 会话列表 ==================== */
.conv-list, .contacts-list, .group-list {
  padding: var(--dp-spacing-xs) 0;
}

.conv-item, .contact-item {
  display: flex;
  align-items: center;
  gap: var(--dp-spacing-sm);
  padding: var(--dp-spacing-sm) var(--dp-spacing-md);
  cursor: pointer;
  transition: background 120ms;
  border-left: 3px solid transparent;
}

.conv-item:hover, .contact-item:hover { background: #f5f5f5; }
.conv-item.active {
  background: #e8f0fe;
  border-left-color: #3b82f6;
}

.conv-avatar-wrap, .contact-avatar-wrap {
  position: relative;
  flex-shrink: 0;
}

.conv-avatar-wrap :deep(.n-avatar),
.contact-avatar-wrap :deep(.n-avatar),
.info-avatar-section :deep(.n-avatar),
.chat-header-left :deep(.n-avatar),
.msg-row :deep(.n-avatar),
.create-chat-user-item :deep(.n-avatar),
.info-member-item :deep(.n-avatar) {
  color: #fff;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0,0,0,0.2);
}

.online-dot {
  position: absolute;
  bottom: 1px;
  right: 1px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #d1d5db;
  border: 2px solid #fff;
}

.online-dot.online { background: #10b981; }

.group-badge {
  position: absolute;
  bottom: -1px;
  right: -1px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #3b82f6;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #fff;
}

.conv-info, .contact-info {
  flex: 1;
  min-width: 0;
}

.conv-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.conv-name, .contact-name {
  font-size: var(--dp-font-md);
  font-weight: 500;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-time {
  font-size: var(--dp-font-xs);
  color: #aaa;
  flex-shrink: 0;
  margin-left: var(--dp-spacing-xs);
}

.conv-member-count {
  font-size: var(--dp-font-xs);
  color: #aaa;
}

.conv-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 3px;
}

.conv-msg {
  font-size: var(--dp-font-xs);
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.contact-status {
  font-size: var(--dp-font-xs);
  color: #aaa;
}

.empty-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  gap: var(--dp-spacing-sm);
  color: #bbb;
  font-size: var(--dp-font-sm);
}

/* ==================== 右侧聊天区 ==================== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: var(--chat-main-bg, #f0f2f5);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--dp-spacing-sm) var(--dp-spacing-md);
  background: var(--sidebar-bg, #fff);
  border-bottom: 1px solid var(--border-color, #e8e8e8);
  flex-shrink: 0;
}

.chat-header-left {
  display: flex;
  align-items: center;
  gap: var(--dp-spacing-sm);
}

.chat-header-info {
  display: flex;
  flex-direction: column;
}

.chat-header-name {
  font-size: var(--dp-font-lg);
  font-weight: 600;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: var(--dp-spacing-xs);
}

.chat-type-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: var(--dp-radius-sm);
  background: #e8f0fe;
  color: #3b82f6;
  font-weight: 400;
}

.chat-header-meta {
  font-size: var(--dp-font-xs);
  color: #999;
}

.status-text.online { color: #10b981; }

.chat-header-actions {
  display: flex;
  gap: var(--dp-spacing-xs);
}

/* ==================== 消息区域 ==================== */
.chat-messages-wrapper {
  flex: 1;
  min-height: 0;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.chat-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: var(--dp-spacing-md) var(--dp-spacing-lg);
  scroll-behavior: smooth;
}
.chat-messages::-webkit-scrollbar {
  width: 6px;
}
.chat-messages::-webkit-scrollbar-thumb {
  background: rgba(0,0,0,0.15);
  border-radius: 3px;
}
.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--dp-spacing-xs);
  padding: var(--dp-spacing-sm);
  color: #999;
  font-size: var(--dp-font-xs);
}

.date-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: var(--dp-spacing-md) 0 var(--dp-spacing-md);
}

.date-divider span {
  font-size: var(--dp-font-xs);
  color: #aaa;
  background: var(--chat-main-bg, #f0f2f5);
  padding: 2px var(--dp-spacing-md);
  border-radius: var(--dp-radius-lg);
}

.msg-row {
  display: flex;
  gap: var(--dp-spacing-sm);
  margin-bottom: var(--dp-spacing-md);
  align-items: flex-start;
  animation: msgFadeIn 300ms ease both;
}

@keyframes msgFadeIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.msg-row.msg-self {
  flex-direction: row-reverse;
}

.msg-content-wrap {
  max-width: 60%;
  display: flex;
  flex-direction: column;
}

.msg-row.msg-self .msg-content-wrap {
  align-items: flex-end;
}

.msg-sender {
  font-size: var(--dp-font-xs);
  color: #999;
  margin-bottom: 2px;
}

.msg-bubble {
  padding: var(--dp-spacing-sm) var(--dp-spacing-md);
  border-radius: var(--dp-radius-md);
  background: #fff;
  box-shadow: var(--dp-shadow-sm);
  word-break: break-word;
}

.msg-bubble.self {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
}

.msg-text {
  font-size: var(--dp-font-md);
  line-height: 1.5;
  white-space: pre-wrap;
}

.msg-time {
  font-size: 10px;
  color: #bbb;
  margin-top: var(--dp-spacing-xs);
}

/* 消息状态行（已读/未读 + 时间） */
.msg-meta-row {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
}
.msg-self .msg-meta-row {
  flex-direction: row-reverse;
}
.msg-read-status {
  font-size: 10px;
  display: inline-flex;
  align-items: center;
}
.msg-read-status.sending {
  color: #bbb;
}
.msg-read-status.unread {
  color: #bbb;
}
.msg-read-status.read {
  color: #10b981;
  font-weight: 500;
}
.msg-read-status.failed {
  color: #ef4444;
  cursor: pointer;
}
.msg-read-status.failed:hover {
  text-decoration: underline;
}

.resend-btn:hover {
  text-decoration: underline;
}

.no-messages {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #ccc;
  font-size: var(--dp-font-sm);
}

/* 文件卡片 */
.file-card {
  display: flex;
  align-items: center;
  gap: var(--dp-spacing-sm);
  padding: var(--dp-spacing-xs) var(--dp-spacing-sm);
  min-width: 180px;
}

.file-detail {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.file-name {
  font-size: var(--dp-font-sm);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: var(--dp-font-xs);
  color: #999;
}

.file-dl {
  color: #3b82f6;
  flex-shrink: 0;
}

/* ==================== 骨架屏 ==================== */
.skeleton-messages {
  display: flex;
  flex-direction: column;
  gap: var(--dp-spacing-md);
  padding: var(--dp-spacing-sm) 0;
}

.skeleton-row {
  display: flex;
  align-items: flex-start;
  gap: var(--dp-spacing-sm);
}

.skeleton-row.skeleton-self {
  flex-direction: row-reverse;
}

.skeleton-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(90deg, #e5e7eb 25%, #f3f4f6 50%, #e5e7eb 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
  flex-shrink: 0;
}

.skeleton-bubble {
  display: flex;
  flex-direction: column;
  gap: var(--dp-spacing-xs);
  padding: var(--dp-spacing-sm) var(--dp-spacing-md);
  border-radius: var(--dp-radius-md);
  background: #fff;
  min-width: 120px;
  max-width: 50%;
}

.skeleton-line {
  height: 12px;
  border-radius: var(--dp-spacing-xs);
  background: linear-gradient(90deg, #e5e7eb 25%, #f3f4f6 50%, #e5e7eb 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
}

@keyframes skeleton-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ==================== 新消息浮动提示 ==================== */
.new-messages-float {
  position: absolute;
  bottom: var(--dp-spacing-md);
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: var(--dp-spacing-xs);
  padding: var(--dp-spacing-xs) var(--dp-spacing-md);
  background: #3b82f6;
  color: #fff;
  border-radius: 20px;
  font-size: var(--dp-font-sm);
  cursor: pointer;
  box-shadow: var(--dp-shadow-md);
  transition: background 150ms, transform 150ms;
  z-index: 10;
  user-select: none;
}

.new-messages-float:hover {
  background: #2563eb;
  transform: translateX(-50%) scale(1.03);
}

/* ==================== 输入区域 ==================== */
.chat-input-area {
  background: var(--sidebar-bg, #fff);
  border-top: 1px solid var(--border-color, #e8e8e8);
  padding: var(--dp-spacing-sm) var(--dp-spacing-md) var(--dp-spacing-md);
  flex-shrink: 0;
}

.input-toolbar {
  display: flex;
  gap: var(--dp-spacing-xs);
  margin-bottom: var(--dp-spacing-xs);
}

.toolbar-btn {
  color: #666 !important;
  transition: transform 150ms ease, color 150ms ease;
}

.toolbar-btn:hover {
  color: #3b82f6 !important;
  transform: scale(1.1);
}

.input-box :deep(.n-input .n-input__textarea-el) {
  font-size: var(--dp-font-md);
  line-height: 1.5;
}

.input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--dp-spacing-xs);
}

.input-hint {
  font-size: var(--dp-font-xs);
  color: #ccc;
}

/* Emoji 面板 */
.emoji-panel {
  width: 340px;
  max-height: 320px;
  display: flex;
  flex-direction: column;
}

.emoji-tabs {
  display: flex;
  gap: 2px;
  padding: var(--dp-spacing-xs) var(--dp-spacing-sm);
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.emoji-tab-btn {
  font-size: var(--dp-font-xl);
  cursor: pointer;
  padding: var(--dp-spacing-xs) var(--dp-spacing-xs);
  border-radius: var(--dp-radius-sm);
  transition: background 100ms;
}

.emoji-tab-btn:hover { background: #f0f0f0; }
.emoji-tab-btn.active { background: #e8f0fe; }

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 2px;
  padding: var(--dp-spacing-sm);
  overflow-y: auto;
  max-height: 260px;
}

.emoji-item {
  font-size: 20px;
  cursor: pointer;
  padding: var(--dp-spacing-xs);
  border-radius: var(--dp-radius-sm);
  text-align: center;
  transition: background 100ms;
}

.emoji-item:hover {
  background: #f0f0f0;
}

/* ==================== 右侧信息面板 ==================== */
.chat-info-panel {
  width: 260px;
  background: var(--sidebar-bg, #fff);
  border-left: 1px solid var(--border-color, #e8e8e8);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.info-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--dp-spacing-md) var(--dp-spacing-md);
  border-bottom: 1px solid var(--border-color, #f0f0f0);
  font-size: var(--dp-font-md);
  font-weight: 600;
  color: #1f2937;
}

.info-panel-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--dp-spacing-md) var(--dp-spacing-md);
}

.info-avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--dp-spacing-xs);
  margin-bottom: var(--dp-spacing-lg);
}

.info-conv-name {
  font-size: var(--dp-font-lg);
  font-weight: 600;
  color: #1f2937;
}

.info-conv-type {
  font-size: var(--dp-font-xs);
  color: #999;
}

.info-section-title {
  font-size: var(--dp-font-xs);
  font-weight: 600;
  color: #666;
  margin-bottom: var(--dp-spacing-sm);
}

.info-members {
  display: flex;
  flex-direction: column;
  gap: var(--dp-spacing-xs);
}

.info-member-item {
  display: flex;
  align-items: center;
  gap: var(--dp-spacing-sm);
}

.info-member-name {
  font-size: var(--dp-font-sm);
  color: #333;
}

/* ==================== 空状态 ==================== */
.empty-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--dp-spacing-sm);
}

.empty-chat-illustration {
  opacity: 0.85;
  margin-bottom: var(--dp-spacing-xs);
}

.empty-chat-title {
  font-size: var(--dp-font-xl);
  font-weight: 500;
  color: #999;
  margin: var(--dp-spacing-sm) 0 0;
}

.empty-chat-desc {
  font-size: var(--dp-font-sm);
  color: #bbb;
  margin: 0 0 var(--dp-spacing-md);
}

/* ==================== 创建聊天对话框 ==================== */
.create-chat-users {
  max-height: 300px;
  overflow-y: auto;
}

.create-chat-user-item {
  display: flex;
  align-items: center;
  gap: var(--dp-spacing-sm);
  padding: var(--dp-spacing-sm) var(--dp-spacing-xs);
  cursor: pointer;
  border-radius: var(--dp-radius-sm);
  transition: background 120ms;
}

.create-chat-user-item:hover {
  background: #f5f5f5;
}

.user-status-tag {
  margin-left: auto;
  font-size: var(--dp-font-xs);
  color: #aaa;
}

.user-status-tag.online {
  color: #10b981;
}

.create-chat-empty {
  text-align: center;
  padding: 20px 0;
  color: #999;
}

.create-chat-hint {
  font-size: var(--dp-font-xs);
  color: #ccc;
  margin-top: var(--dp-spacing-xs);
}

/* ==================== 动画 ==================== */
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 200ms ease;
}

.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(100%);
  opacity: 0;
}



/* ========== 移动端适配 ========== */
@media (max-width: 768px) {
  .chat-page {
    flex-direction: column;
    height: calc(100vh - var(--mobile-header-height, 48px) - var(--mobile-tab-bar-height, 56px) - var(--mobile-safe-top, 0px) - var(--mobile-safe-bottom, 0px));
  }

  .chat-sidebar {
    width: 100%;
    height: auto;
    max-height: 40vh;
    border-right: none;
    border-bottom: 1px solid var(--border-light, #e5e7eb);
  }

  .sidebar-search {
    padding: 8px 12px;
  }

  .sidebar-tabs {
    padding: 0 8px;
  }

  .sidebar-body {
    max-height: 25vh;
    overflow-y: auto;
  }

  .conv-item {
    padding: 10px 12px;
  }

  .conv-avatar-wrap .n-avatar {
    width: 36px !important;
    height: 36px !important;
  }

  .chat-main {
    flex: 1;
    min-height: 0;
  }

  .chat-header {
    padding: 8px 12px;
    height: auto;
    min-height: 44px;
  }

  .messages-area {
    padding: 8px;
  }

  .chat-input-area {
    padding: 8px 10px;
  }

  .msg-bubble {
    max-width: 85%;
    font-size: 14px;
    padding: 8px 12px;
  }

  .toolbar-btn {
    width: 32px;
    height: 32px;
  }
}
</style>

<style>
/* ChatView 深色模式（非 scoped） */
html.dark .chat-page {
  --chat-bg: #111 !important;
  --sidebar-bg: #1a1a1a !important;
  --border-color: #333 !important;
  --chat-main-bg: #111 !important;
}
html.dark .conv-item:hover,
html.dark .contact-item:hover { background: #252525 !important; }
html.dark .conv-item.active { background: #1e3a5f !important; }
html.dark .conv-name,
html.dark .contact-name { color: #e5e7eb !important; }
html.dark .conv-msg { color: #6b7280 !important; }
html.dark .contact-status { color: #6b7280 !important; }
html.dark .tab-btn { color: #9ca3af !important; }
html.dark .tab-btn:hover,
html.dark .tab-btn.active { color: #60a5fa !important; }
html.dark .tab-btn.active { border-bottom-color: #60a5fa !important; }
html.dark .chat-header-name { color: #e5e7eb !important; }
html.dark .chat-type-tag { background: #1e3a5f !important; color: #60a5fa !important; }
html.dark .info-panel-header,
html.dark .info-conv-name { color: #e5e7eb !important; }
html.dark .info-member-name { color: #d1d5db !important; }
html.dark .msg-bubble {
  background: #2a2a2a !important;
  color: #e5e7eb !important;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2) !important;
}
html.dark .msg-bubble.self {
  background: linear-gradient(135deg, #1e3a6e, #1e40af) !important;
  color: #e5e7eb !important;
}
html.dark .file-card .file-name { color: #e5e7eb !important; }
html.dark .file-card .file-size { color: #6b7280 !important; }
html.dark .file-dl { color: #60a5fa !important; }
html.dark .online-dot { border-color: #1a1a1a !important; }
html.dark .group-badge { border-color: #1a1a1a !important; }
html.dark .date-divider span { background: #111 !important; color: #6b7280 !important; }
html.dark .emoji-item:hover { background: #333 !important; }
html.dark .emoji-tabs { border-bottom-color: #333 !important; }
html.dark .emoji-tab-btn:hover { background: #333 !important; }
html.dark .emoji-tab-btn.active { background: #1e3a5f !important; }
html.dark .toolbar-btn { color: #9ca3af !important; }
html.dark .toolbar-btn:hover { color: #60a5fa !important; }
html.dark .input-hint { color: #4b5563 !important; }
html.dark .empty-list { color: #4b5563 !important; }
html.dark .empty-chat-title { color: #6b7280 !important; }
html.dark .empty-chat-desc { color: #4b5563 !important; }
html.dark .empty-chat-illustration svg rect { fill: #1e3a5f !important; stroke: #3b82f6 !important; }
html.dark .empty-chat-illustration svg circle { fill: #1e3a5f !important; }
html.dark .create-chat-user-item:hover { background: #252525 !important; }
html.dark .user-status-tag { color: #6b7280 !important; }
html.dark .skeleton-avatar {
  background: linear-gradient(90deg, #333 25%, #444 50%, #333 75%) !important;
  background-size: 200% 100% !important;
  animation: skeleton-shimmer 1.5s infinite !important;
}
html.dark .skeleton-bubble { background: #2a2a2a !important; }
html.dark .skeleton-line {
  background: linear-gradient(90deg, #333 25%, #444 50%, #333 75%) !important;
  background-size: 200% 100% !important;
  animation: skeleton-shimmer 1.5s infinite !important;
}
html.dark .new-messages-float {
  background: #1e40af !important;
  box-shadow: 0 2px 8px rgba(30, 64, 175, 0.4) !important;
}
html.dark .new-messages-float:hover { background: #1e3a8a !important; }
</style>

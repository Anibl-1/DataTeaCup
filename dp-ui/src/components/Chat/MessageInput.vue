<template>
  <div v-if="chatStore.currentConversationId" class="message-input">
    <div class="input-toolbar">
      <n-upload :show-file-list="false" accept="image/*" @change="handleImageUpload">
        <button class="toolbar-btn" title="发送图片">
          <n-icon size="17"><ImageOutline /></n-icon>
        </button>
      </n-upload>
      <n-upload :show-file-list="false" @change="handleFileUpload">
        <button class="toolbar-btn" title="发送文件">
          <n-icon size="17"><AttachOutline /></n-icon>
        </button>
      </n-upload>
      <button class="toolbar-btn" title="表情" @click="showEmojiHint">
        <n-icon size="17"><HappyOutline /></n-icon>
      </button>
      <span class="input-hint">Shift+Enter 换行</span>
    </div>
    <div class="input-row">
      <n-input
        ref="inputRef"
        v-model:value="inputText"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 4 }"
        placeholder="输入消息，Enter 发送..."
        @keydown="handleKeydown"
      />
      <button
        class="send-btn"
        :class="{ active: inputText.trim() }"
        :disabled="!inputText.trim()"
        @click="sendText"
      >
        <n-icon size="18"><SendOutline /></n-icon>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onBeforeUnmount } from 'vue'
import { NInput, NIcon, NUpload, useMessage } from 'naive-ui'
import { ImageOutline, AttachOutline, HappyOutline, SendOutline } from '@vicons/ionicons5'
import { useChatStore } from '@/stores/chat'
import type { UploadFileInfo } from 'naive-ui'

const chatStore = useChatStore()
const message = useMessage()
const inputText = ref('')
const inputRef = ref<any>(null)
const blobUrls: string[] = []

onBeforeUnmount(() => {
  blobUrls.forEach(url => URL.revokeObjectURL(url))
  blobUrls.length = 0
})

/** Enter sends message, Shift+Enter inserts newline */
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendText()
  }
}

/** Send text message */
function sendText() {
  const text = inputText.value.trim()
  if (!text || !chatStore.currentConversationId) return
  chatStore.sendMsg(chatStore.currentConversationId, 'text', text)
  inputText.value = ''
}

/** Handle image upload */
function handleImageUpload({ file }: { file: UploadFileInfo }) {
  if (!chatStore.currentConversationId || !file.file) return
  const url = URL.createObjectURL(file.file)
  blobUrls.push(url)
  const fileName = file.name || 'image'
  const fileSize = file.file.size || 0
  chatStore.sendMsg(
    chatStore.currentConversationId,
    'image',
    fileName,
    url,
    fileName,
    fileSize
  )
}

/** Handle file upload */
function handleFileUpload({ file }: { file: UploadFileInfo }) {
  if (!chatStore.currentConversationId || !file.file) return
  const url = URL.createObjectURL(file.file)
  blobUrls.push(url)
  const fileName = file.name || 'file'
  const fileSize = file.file.size || 0
  chatStore.sendMsg(
    chatStore.currentConversationId,
    'file',
    fileName,
    url,
    fileName,
    fileSize
  )
}

function showEmojiHint() {
  message.info('表情功能开发中，敬请期待')
}
</script>

<style scoped>
.message-input {
  border-top: 1px solid #eef0f3;
  padding: 8px 12px 10px;
  flex-shrink: 0;
  background: #fff;
}

/* ===== Toolbar ===== */
.input-toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-bottom: 6px;
}

.toolbar-btn {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.12s;
}

.toolbar-btn:hover {
  background: #f1f5f9;
  color: #3b82f6;
}

.input-hint {
  margin-left: auto;
  font-size: 10.5px;
  color: #cbd5e1;
  user-select: none;
}

/* ===== Input row ===== */
.input-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.input-row :deep(.n-input) {
  flex: 1;
  border-radius: 12px;
  background: #f8fafc;
  border-color: #eef0f3;
  transition: all 0.15s;
}

.input-row :deep(.n-input:focus-within) {
  background: #fff;
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
}

.input-row :deep(.n-input .n-input__textarea-el) {
  font-size: 13.5px;
  line-height: 1.5;
}

/* ===== Send button ===== */
.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: #e2e8f0;
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: not-allowed;
  transition: all 0.2s;
  flex-shrink: 0;
}

.send-btn.active {
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: #fff;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.send-btn.active:hover {
  transform: scale(1.05);
  box-shadow: 0 3px 12px rgba(59, 130, 246, 0.35);
}

.send-btn.active:active {
  transform: scale(0.97);
}
</style>

<style>
/* MessageInput 深色模式（非 scoped） */
html.dark .message-input {
  border-top-color: rgba(255, 255, 255, 0.06) !important;
  background: #111827 !important;
}
html.dark .toolbar-btn {
  color: #64748b !important;
}
html.dark .toolbar-btn:hover {
  background: rgba(255,255,255,0.05) !important;
  color: #818cf8 !important;
}
html.dark .input-hint {
  color: #475569 !important;
}
html.dark .input-row .n-input {
  background: #1e293b !important;
  border-color: rgba(255,255,255,0.06) !important;
}
html.dark .input-row .n-input:focus-within {
  background: #111827 !important;
  border-color: #6366f1 !important;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.15) !important;
}
html.dark .send-btn {
  background: #1e293b !important;
  color: #475569 !important;
}
html.dark .send-btn.active {
  background: linear-gradient(135deg, #4f46e5, #6366f1) !important;
  color: #fff !important;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3) !important;
}
</style>

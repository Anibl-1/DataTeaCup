<template>
  <div class="team-space-page">
    <!-- ==================== 空间列表 ==================== -->
    <template v-if="!selectedSpace">
      <div class="page-header-stats">
        <div class="stat-item">
          <div class="stat-icon stat-icon-primary"><n-icon size="24"><GridOutline /></n-icon></div>
          <div class="stat-info"><span class="stat-value">{{ spaces.length }}</span><span class="stat-label">{{ t('teamSpace.title') }}</span></div>
        </div>
        <div class="stat-item">
          <div class="stat-icon stat-icon-success"><n-icon size="24"><GlobeOutline /></n-icon></div>
          <div class="stat-info"><span class="stat-value">{{ publicCount }}</span><span class="stat-label">{{ t('teamSpace.publicSpaces') }}</span></div>
        </div>
        <div class="stat-item">
          <div class="stat-icon stat-icon-warning"><n-icon size="24"><LockClosedOutline /></n-icon></div>
          <div class="stat-info"><span class="stat-value">{{ privateCount }}</span><span class="stat-label">{{ t('teamSpace.privateSpaces') }}</span></div>
        </div>
      </div>
      <n-card>
        <template #header><div class="card-title"><n-icon size="22" color="var(--color-primary)"><GridOutline /></n-icon><span>{{ t('teamSpace.title') }}</span></div></template>
        <template #header-extra>
          <n-space :size="8">
            <n-input v-model:value="spaceSearch" :placeholder="t('teamSpace.searchPlaceholder')" clearable style="width:180px;" size="small">
              <template #prefix><n-icon size="14"><SearchOutline /></n-icon></template>
            </n-input>
            <n-button type="primary" @click="openCreateModal"><template #icon><n-icon><AddOutline /></n-icon></template>{{ t('teamSpace.createSpace') }}</n-button>
          </n-space>
        </template>
        <n-empty v-if="filteredSpaces.length === 0 && !loading" :description="t('teamSpace.noSpaces')" style="padding: 60px 0;" />
        <n-spin :show="loading">
          <n-grid v-if="filteredSpaces.length > 0" :cols="3" :x-gap="16" :y-gap="16">
            <n-gi v-for="space in filteredSpaces" :key="space.id">
              <n-card hoverable class="space-card" @click="selectSpace(space)">
                <template #header><n-space align="center" :size="8"><n-icon size="18" color="var(--color-primary)"><GridOutline /></n-icon><span>{{ space.name }}</span></n-space></template>
                <template #header-extra><n-tag :type="space.visibility === 'public' ? 'success' : 'warning'" size="small">{{ space.visibility === 'public' ? t('teamSpace.visPublic') : t('teamSpace.visPrivate') }}</n-tag></template>
                <p style="color:var(--text-secondary);font-size:13px;margin:0 0 8px;">{{ space.description || t('teamSpace.noDescription') }}</p>
                <n-space size="small" align="center">
                  <n-tag size="tiny" round :bordered="false"><template #icon><n-icon size="12"><PeopleOutline /></n-icon></template>{{ spaceMembers[space.id] || 0 }} {{ t('teamSpace.members') }}</n-tag>
                  <n-tag size="tiny" round :bordered="false" type="info"><template #icon><n-icon size="12"><TimeOutline /></n-icon></template>{{ fmtDate(space.createdAt) }}</n-tag>
                </n-space>
                <template #action>
                  <n-space justify="end">
                    <n-button size="small" type="primary" secondary @click.stop="selectSpace(space)">{{ t('teamSpace.enterSpace') }}</n-button>
                    <n-popconfirm @positive-click="handleDeleteSpace(space)">
                      <template #trigger><n-button size="small" type="error" secondary @click.stop>{{ t('teamSpace.deleteSpace') }}</n-button></template>
                      {{ t('teamSpace.deleteConfirm', { name: space.name }) }}
                    </n-popconfirm>
                  </n-space>
                </template>
              </n-card>
            </n-gi>
          </n-grid>
        </n-spin>
      </n-card>
    </template>

    <!-- ==================== 空间详情 ==================== -->
    <template v-else>
      <n-page-header :title="selectedSpace.name" :subtitle="selectedSpace.visibility === 'public' ? t('teamSpace.publicSpace') : t('teamSpace.privateSpace')" @back="handleBackToList">
        <template #extra>
          <n-space>
            <n-button size="small" type="primary" @click="showAddMemberModal = true"><template #icon><n-icon><PersonAddOutline /></n-icon></template>{{ t('teamSpace.addMember') }}</n-button>
          </n-space>
        </template>
      </n-page-header>
      <div class="page-header-stats" style="margin-top:16px;">
        <div class="stat-item">
          <div class="stat-icon stat-icon-primary"><n-icon size="22"><PeopleOutline /></n-icon></div>
          <div class="stat-info"><span class="stat-value">{{ members.length }}</span><span class="stat-label">{{ t('teamSpace.members') }}</span></div>
        </div>
        <div class="stat-item">
          <div class="stat-icon stat-icon-success"><n-icon size="22"><DocumentOutline /></n-icon></div>
          <div class="stat-info"><span class="stat-value">{{ spaceFiles.length }}</span><span class="stat-label">{{ t('teamSpace.files') }}</span></div>
        </div>
        <div class="stat-item">
          <div class="stat-icon stat-icon-warning"><n-icon size="22"><FolderOpenOutline /></n-icon></div>
          <div class="stat-info"><span class="stat-value">{{ sharedResources.length }}</span><span class="stat-label">{{ t('teamSpace.resources') }}</span></div>
        </div>
        <div class="stat-item">
          <div class="stat-icon stat-icon-info"><n-icon size="22"><ChatbubblesOutline /></n-icon></div>
          <div class="stat-info"><span class="stat-value">{{ chatMessages.length }}</span><span class="stat-label">{{ t('teamSpace.messages') }}</span></div>
        </div>
      </div>

      <n-tabs v-model:value="activeTab" type="line" style="margin-top:16px;">
        <!-- ===== 团队聊天 ===== -->
        <n-tab-pane name="chat" :tab="t('teamSpace.tabChat')">
          <n-card class="chat-card">
            <template #header>
              <div class="card-title"><n-icon size="18" color="#6366f1"><ChatbubblesOutline /></n-icon><span>{{ t('teamSpace.teamDiscussion') }}</span></div>
            </template>
            <div ref="chatContainerRef" class="team-chat-messages">
              <n-empty v-if="chatMessages.length === 0" :description="t('teamSpace.noMessages')" style="padding:40px 0;" />
              <div v-for="(msg, idx) in chatMessages" :key="idx" :class="['chat-msg', msg.isMine ? 'mine' : '']">
                <n-avatar v-if="!msg.isMine" round size="small" class="chat-avatar">{{ (msg.senderName || '?').charAt(0) }}</n-avatar>
                <div class="chat-bubble-wrap">
                  <div v-if="!msg.isMine" class="chat-sender">{{ msg.senderName }}</div>
                  <div class="chat-bubble">
                    {{ msg.content }}
                    <div v-if="msg.fileUrl" class="chat-file-attach">
                      <n-icon size="14"><AttachOutline /></n-icon>
                      <a :href="msg.fileUrl" target="_blank">{{ msg.fileName || t('teamSpace.attachment') }}</a>
                    </div>
                  </div>
                  <div class="chat-time">{{ fmtTime(msg.createdAt) }}</div>
                </div>
                <n-avatar v-if="msg.isMine" round size="small" class="chat-avatar">我</n-avatar>
              </div>
            </div>
            <div class="team-chat-input">
              <n-input
                v-model:value="chatInput"
                :placeholder="t('teamSpace.inputPlaceholder')"
                :autosize="{ minRows: 1, maxRows: 3 }"
                type="textarea"
                @keydown.enter.exact.prevent="sendChatMessage"
              />
              <n-button type="primary" :disabled="!chatInput.trim()" @click="sendChatMessage">
                <template #icon><n-icon><SendOutline /></n-icon></template>
              </n-button>
            </div>
          </n-card>
        </n-tab-pane>

        <!-- ===== 文件共享 ===== -->
        <n-tab-pane name="files" :tab="t('teamSpace.tabFiles')">
          <n-card>
            <template #header>
              <div class="card-title"><n-icon size="18" color="#10b981"><DocumentOutline /></n-icon><span>{{ t('teamSpace.sharedFiles') }} ({{ spaceFiles.length }})</span></div>
            </template>
            <template #header-extra>
              <n-upload
                :action="fileUploadUrl"
                :headers="uploadHeaders"
                :show-file-list="false"
                @finish="onFileUploaded"
                @error="onFileUploadError"
              >
                <n-button size="small" type="primary">
                  <template #icon><n-icon size="14"><CloudUploadOutline /></n-icon></template>
                  {{ t('teamSpace.uploadFile') }}
                </n-button>
              </n-upload>
            </template>
            <n-empty v-if="spaceFiles.length === 0" :description="t('teamSpace.noFiles')" style="padding:40px 0;" />
            <n-data-table
              v-else
              :columns="fileColumns"
              :data="spaceFiles"
              :pagination="false"
              :row-key="(row: SpaceFile) => row.id"
              size="small"
              striped
            />
          </n-card>
        </n-tab-pane>

        <!-- ===== 共享资源 ===== -->
        <n-tab-pane name="resources" :tab="t('teamSpace.tabResources')">
          <n-card>
            <template #header><div class="card-title"><n-icon size="18" color="#f59e0b"><FolderOpenOutline /></n-icon><span>{{ t('teamSpace.sharedResources') }}</span></div></template>
            <template #header-extra>
              <n-button size="small" type="primary" @click="handleShareResource">
                <template #icon><n-icon size="14"><ShareOutline /></n-icon></template>
                {{ t('teamSpace.shareResource') }}
              </n-button>
            </template>
            <n-empty v-if="sharedResources.length === 0" :description="t('teamSpace.noResources')" style="padding:40px 0;" />
            <n-list v-else hoverable clickable>
              <n-list-item v-for="res in sharedResources" :key="res.id">
                <template #prefix>
                  <n-icon size="20" :color="res.type === 'chart' ? '#6366f1' : res.type === 'report' ? '#10b981' : '#f59e0b'">
                    <GridOutline />
                  </n-icon>
                </template>
                <n-thing :title="res.name" :description="getResTypeLabel(res.type) + ' · ' + fmtDate(res.sharedAt)">
                  <template #header-extra>
                    <n-popconfirm @positive-click="handleRemoveResource(res.id)">
                      <template #trigger><n-button text size="tiny" type="error">{{ t('teamSpace.removeResource') }}</n-button></template>
                      {{ t('teamSpace.removeResourceConfirm') }}
                    </n-popconfirm>
                  </template>
                </n-thing>
              </n-list-item>
            </n-list>
          </n-card>
        </n-tab-pane>

        <!-- ===== 成员管理 ===== -->
        <n-tab-pane name="members" :tab="t('teamSpace.tabMembers')">
          <n-card>
            <template #header><div class="card-title"><n-icon size="18" color="var(--color-primary)"><PeopleOutline /></n-icon><span>{{ t('teamSpace.members') }} ({{ members.length }})</span></div></template>
            <n-empty v-if="members.length === 0" :description="t('teamSpace.noMembers')" />
            <n-list v-else>
              <n-list-item v-for="m in members" :key="m.userId">
                <template #prefix><n-avatar round size="small">{{ (m.userName || m.userId || '?').charAt(0).toUpperCase() }}</n-avatar></template>
                <n-thing :title="m.userName || m.userId" :description="(roleLabelMap[m.role] || m.role) + ' · ' + t('teamSpace.joinedAt') + ' ' + fmtDate(m.joinedAt)">
                  <template #header-extra>
                    <n-space :size="4" align="center">
                      <n-select
                        v-if="m.role !== 'owner'"
                        :value="m.role"
                        :options="roleOptions"
                        size="tiny"
                        style="width:90px;"
                        @update:value="(val: string) => handleChangeRole(m.userId, val)"
                      />
                      <n-tag v-else type="error" size="small">{{ t('teamSpace.owner') }}</n-tag>
                      <n-popconfirm v-if="m.role !== 'owner'" @positive-click="handleRemoveMember(m.userId)">
                        <template #trigger><n-button text size="tiny" type="error">{{ t('teamSpace.removeMember') }}</n-button></template>
                        {{ t('teamSpace.removeMemberConfirm') }}
                      </n-popconfirm>
                    </n-space>
                  </template>
                </n-thing>
              </n-list-item>
            </n-list>
          </n-card>
        </n-tab-pane>

        <!-- ===== 活动日志 ===== -->
        <n-tab-pane name="activity" :tab="t('teamSpace.tabActivity')">
          <n-card>
            <template #header><div class="card-title"><n-icon size="18" color="var(--color-primary)"><TimeOutline /></n-icon><span>{{ t('teamSpace.activityLog') }}</span></div></template>
            <template #header-extra><n-button text size="small" @click="fetchActivities">{{ t('teamSpace.refresh') }}</n-button></template>
            <n-empty v-if="activities.length === 0" :description="t('teamSpace.noActivities')" />
            <n-timeline v-else>
              <n-timeline-item v-for="(act, idx) in activities" :key="idx" :time="fmtDate(act.createdAt)" :title="actLabel(act.action)">{{ act.detail || '' }}</n-timeline-item>
            </n-timeline>
          </n-card>
        </n-tab-pane>

        <!-- ===== 空间设置 ===== -->
        <n-tab-pane name="settings" :tab="t('teamSpace.tabSettings')">
          <n-card>
            <template #header><div class="card-title"><n-icon size="18" color="var(--color-primary)"><SettingsOutline /></n-icon><span>{{ t('teamSpace.spaceSettings') }}</span></div></template>
            <n-form :model="editSpaceForm" label-placement="left" label-width="100px" style="max-width:500px;">
              <n-form-item :label="t('teamSpace.spaceName')">
                <n-input v-model:value="editSpaceForm.name" />
              </n-form-item>
              <n-form-item :label="t('teamSpace.description')">
                <n-input v-model:value="editSpaceForm.description" type="textarea" :rows="2" />
              </n-form-item>
              <n-form-item :label="t('teamSpace.visibility')">
                <n-radio-group v-model:value="editSpaceForm.visibility">
                  <n-radio value="private">{{ t('teamSpace.visPrivate') }}</n-radio>
                  <n-radio value="public">{{ t('teamSpace.visPublic') }}</n-radio>
                </n-radio-group>
              </n-form-item>
              <n-form-item :label="t('teamSpace.createdAt')">
                <span>{{ fmtDate(selectedSpace.createdAt) }}</span>
              </n-form-item>
              <n-form-item>
                <n-button type="primary" :loading="savingSettings" @click="handleSaveSettings">{{ t('teamSpace.saveSettings') }}</n-button>
              </n-form-item>
            </n-form>
          </n-card>
        </n-tab-pane>
      </n-tabs>
    </template>

    <!-- ==================== 创建空间弹窗 ==================== -->
    <n-modal v-model:show="showCreateModal" preset="card" :title="t('teamSpace.createSpaceTitle')" style="width:480px;border-radius:16px;">
      <n-form :model="newSpace" label-placement="left" label-width="80px">
        <n-form-item :label="t('teamSpace.spaceName')"><n-input v-model:value="newSpace.name" :placeholder="t('teamSpace.spaceNamePlaceholder')" /></n-form-item>
        <n-form-item :label="t('teamSpace.description')"><n-input v-model:value="newSpace.description" type="textarea" :placeholder="t('teamSpace.descPlaceholder')" :rows="3" /></n-form-item>
        <n-form-item :label="t('teamSpace.visibility')">
          <n-radio-group v-model:value="newSpace.visibility"><n-radio value="private">{{ t('teamSpace.visPrivate') }}</n-radio><n-radio value="public">{{ t('teamSpace.visPublic') }}</n-radio></n-radio-group>
        </n-form-item>
        <n-form-item><n-space style="width:100%;justify-content:flex-end;"><n-button @click="showCreateModal = false">{{ t('teamSpace.cancel') }}</n-button><n-button type="primary" :loading="createLoading" @click="handleCreateSpace">{{ t('teamSpace.create') }}</n-button></n-space></n-form-item>
      </n-form>
    </n-modal>

    <!-- ==================== 添加成员弹窗 ==================== -->
    <n-modal v-model:show="showAddMemberModal" preset="card" :title="t('teamSpace.addMemberTitle')" style="width:420px;border-radius:16px;">
      <n-form label-placement="left" label-width="80px">
        <n-form-item :label="t('teamSpace.selectUser')"><n-select v-model:value="addMemberForm.userId" :options="userOptions" :loading="userListLoading" filterable :placeholder="t('teamSpace.searchUser')" /></n-form-item>
        <n-form-item :label="t('teamSpace.role')"><n-select v-model:value="addMemberForm.role" :options="roleOptions" :placeholder="t('teamSpace.selectRole')" /></n-form-item>
        <n-form-item><n-space style="width:100%;justify-content:flex-end;"><n-button @click="showAddMemberModal = false">{{ t('teamSpace.cancel') }}</n-button><n-button type="primary" :loading="addMemberLoading" :disabled="!addMemberForm.userId" @click="handleAddMember">{{ t('teamSpace.confirm') }}</n-button></n-space></n-form-item>
      </n-form>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, h, watch } from 'vue'
import { useMessage, NButton, NIcon } from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import {
  PeopleOutline, GridOutline, AddOutline, PersonAddOutline, TimeOutline,
  GlobeOutline, LockClosedOutline, SearchOutline, FolderOpenOutline,
  ShareOutline, SettingsOutline, ChatbubblesOutline, DocumentOutline,
  CloudUploadOutline, SendOutline, AttachOutline, DownloadOutline, TrashOutline
} from '@vicons/ionicons5'
import request from '@/api/request'
import { getUserList } from '@/api/system/user'
import { initMessage } from '@/utils/message'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const message = useMessage()
initMessage(message)

// ==================== 类型定义 ====================
interface TeamSpace { id: string; name: string; description: string; ownerId?: string; visibility: string; createdAt: string }
interface Member { spaceId: string; userId: string; userName?: string; role: string; joinedAt: string }
interface Activity { spaceId: string; userId: string; action: string; detail: string; createdAt: string }
interface SharedResource { id: string; name: string; type: string; sharedAt: string }
interface SpaceFile { id: string; name: string; size: number; type: string; uploadedBy: string; uploadedAt: string; url?: string }
interface ChatMessage { id?: string; spaceId: string; senderId: string; senderName: string; content: string; fileUrl?: string; fileName?: string; createdAt: string; isMine?: boolean }

// ==================== 常量 ====================
const roleLabelMap = computed<Record<string, string>>(() => ({ owner: t('teamSpace.owner'), admin: t('teamSpace.admin'), editor: t('teamSpace.editor'), viewer: t('teamSpace.viewer') }))
const actLabelMap = computed<Record<string, string>>(() => ({ create_space: t('teamSpace.actCreateSpace'), join_space: t('teamSpace.actJoinSpace'), leave_space: t('teamSpace.actLeaveSpace'), add_resource: t('teamSpace.actAddResource'), share_from_space: t('teamSpace.actShareResource'), upload_file: t('teamSpace.actUploadFile'), send_message: t('teamSpace.actSendMessage') }))
const actLabel = (a: string) => actLabelMap.value[a] || a
const resTypeLabels = computed<Record<string, string>>(() => ({ chart: t('teamSpace.resChart'), report: t('teamSpace.resReport'), dashboard: t('teamSpace.resDashboard'), dataView: t('teamSpace.resDataView') }))
const getResTypeLabel = (tp: string) => resTypeLabels.value[tp] || tp

// ==================== 状态 ====================
const loading = ref(false)
const spaces = ref<TeamSpace[]>([])
const selectedSpace = ref<TeamSpace | null>(null)
const members = ref<Member[]>([])
const activities = ref<Activity[]>([])
const spaceMembers = ref<Record<string, number>>({})
const activeTab = ref('chat')

const spaceSearch = ref('')
const filteredSpaces = computed(() => {
  if (!spaceSearch.value.trim()) return spaces.value
  const kw = spaceSearch.value.toLowerCase()
  return spaces.value.filter(s => s.name.toLowerCase().includes(kw) || (s.description || '').toLowerCase().includes(kw))
})
const publicCount = computed(() => spaces.value.filter(s => s.visibility === 'public').length)
const privateCount = computed(() => spaces.value.filter(s => s.visibility !== 'public').length)

// ==================== 共享资源 ====================
const sharedResources = ref<SharedResource[]>([])

const fetchSharedResources = async () => {
  if (!selectedSpace.value) return
  try {
    const res = await request.get(`/team-spaces/${selectedSpace.value.id}/resources`)
    sharedResources.value = (res as any).data || []
  } catch { sharedResources.value = [] }
}

const handleShareResource = () => {
  message.info(t('teamSpace.shareHint'))
}

const handleRemoveResource = async (resourceId: string) => {
  if (!selectedSpace.value) return
  try {
    await request.delete(`/team-spaces/${selectedSpace.value.id}/resources/${resourceId}`)
    message.success(t('teamSpace.resourceRemoved'))
    await fetchSharedResources()
  } catch { message.error(t('teamSpace.removeResourceFailed')) }
}

// ==================== 文件共享 ====================
const spaceFiles = ref<SpaceFile[]>([])

const fileUploadUrl = computed(() => {
  if (!selectedSpace.value) return ''
  const base = (request.defaults?.baseURL || '/api') as string
  return `${base}/team-spaces/${selectedSpace.value.id}/files`
})

const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token') || ''
  return { Authorization: token ? `Bearer ${token}` : '' }
})

const fetchFiles = async () => {
  if (!selectedSpace.value) return
  try {
    const res = await request.get(`/team-spaces/${selectedSpace.value.id}/files`)
    spaceFiles.value = (res as any).data || []
  } catch { spaceFiles.value = [] }
}

const onFileUploaded = ({ event }: any) => {
  try {
    const resp = JSON.parse((event?.target as XMLHttpRequest)?.response || '{}')
    if (resp.code === 200 || resp.success) {
      message.success(t('teamSpace.uploadSuccess'))
      fetchFiles()
      fetchActivities()
    } else {
      message.error(resp.message || t('teamSpace.uploadFailed'))
    }
  } catch {
    message.success(t('teamSpace.uploadComplete'))
    fetchFiles()
  }
}

const onFileUploadError = () => { message.error(t('teamSpace.uploadFailed')) }

const handleDownloadFile = (file: SpaceFile) => {
  if (file.url) {
    window.open(file.url, '_blank')
  } else if (selectedSpace.value) {
    window.open(`/api/team-spaces/${selectedSpace.value.id}/files/${file.id}/download`, '_blank')
  }
}

const handleDeleteFile = async (file: SpaceFile) => {
  if (!selectedSpace.value) return
  try {
    await request.delete(`/team-spaces/${selectedSpace.value.id}/files/${file.id}`)
    message.success(t('teamSpace.fileDeleted'))
    await fetchFiles()
  } catch { message.error(t('teamSpace.deleteFileFailed')) }
}

const formatFileSize = (bytes: number) => {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return (bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0) + ' ' + units[i]
}

const fileColumns: DataTableColumns<SpaceFile> = [
  { title: () => t('teamSpace.fileName'), key: 'name', ellipsis: { tooltip: true } },
  { title: () => t('teamSpace.fileSize'), key: 'size', width: 100, render: (row) => formatFileSize(row.size) },
  { title: () => t('teamSpace.uploader'), key: 'uploadedBy', width: 120 },
  { title: () => t('teamSpace.uploadTime'), key: 'uploadedAt', width: 150, render: (row) => fmtDate(row.uploadedAt) },
  {
    title: () => t('teamSpace.actions'), key: 'actions', width: 120, fixed: 'right' as const,
    render: (row) => h('div', { style: 'display:flex;gap:4px;' }, [
      h(NButton, { size: 'tiny', quaternary: true, onClick: () => handleDownloadFile(row) }, {
        icon: () => h(NIcon, { size: 14 }, { default: () => h(DownloadOutline) }),
        default: () => t('teamSpace.download')
      }),
      h(NButton, { size: 'tiny', quaternary: true, type: 'error', onClick: () => handleDeleteFile(row) }, {
        icon: () => h(NIcon, { size: 14 }, { default: () => h(TrashOutline) }),
        default: () => t('teamSpace.deleteFile')
      })
    ])
  }
]

// ==================== 团队聊天 ====================
const chatMessages = ref<ChatMessage[]>([])
const chatInput = ref('')
const chatContainerRef = ref<HTMLElement | null>(null)
let chatPollTimer: ReturnType<typeof setInterval> | null = null

const fetchChatMessages = async () => {
  if (!selectedSpace.value) return
  try {
    const res = await request.get(`/team-spaces/${selectedSpace.value.id}/messages`, { params: { limit: 100 } })
    chatMessages.value = ((res as any).data || []).map((m: any) => ({ ...m, isMine: m.isMine || false }))
  } catch { chatMessages.value = [] }
}

const sendChatMessage = async () => {
  const text = chatInput.value.trim()
  if (!text || !selectedSpace.value) return
  chatInput.value = ''
  try {
    await request.post(`/team-spaces/${selectedSpace.value.id}/messages`, { content: text })
    await fetchChatMessages()
    await nextTick()
    scrollChatToBottom()
  } catch { message.error(t('teamSpace.sendFailed')) }
}

const scrollChatToBottom = () => {
  if (chatContainerRef.value) {
    chatContainerRef.value.scrollTop = chatContainerRef.value.scrollHeight
  }
}

const startChatPolling = () => {
  stopChatPolling()
  chatPollTimer = setInterval(() => {
    if (selectedSpace.value && activeTab.value === 'chat') {
      fetchChatMessages()
    }
  }, 10000)
}

const stopChatPolling = () => {
  if (chatPollTimer) { clearInterval(chatPollTimer); chatPollTimer = null }
}

watch(activeTab, (tab) => {
  if (tab === 'chat') { fetchChatMessages(); startChatPolling() }
  else { stopChatPolling() }
})

// ==================== 空间设置 ====================
const editSpaceForm = reactive({ name: '', description: '', visibility: 'private' })
const savingSettings = ref(false)

const handleSaveSettings = async () => {
  if (!selectedSpace.value) return
  savingSettings.value = true
  try {
    await request.put(`/team-spaces/${selectedSpace.value.id}`, {
      name: editSpaceForm.name,
      description: editSpaceForm.description,
      visibility: editSpaceForm.visibility
    })
    message.success(t('teamSpace.settingsSaved'))
    selectedSpace.value.name = editSpaceForm.name
    selectedSpace.value.description = editSpaceForm.description
    selectedSpace.value.visibility = editSpaceForm.visibility
  } catch { message.error(t('teamSpace.saveSettingsFailed')) }
  finally { savingSettings.value = false }
}

// ==================== 创建空间 ====================
const showCreateModal = ref(false)
const createLoading = ref(false)
const newSpace = ref({ name: '', description: '', visibility: 'private' })

// ==================== 添加成员 ====================
const showAddMemberModal = ref(false)
const addMemberLoading = ref(false)
const addMemberForm = reactive({ userId: null as string | null, role: 'viewer' })
const userOptions = ref<{ label: string; value: string }[]>([])
const userListLoading = ref(false)
const roleOptions = computed(() => [
  { label: t('teamSpace.admin'), value: 'admin' },
  { label: t('teamSpace.editor'), value: 'editor' },
  { label: t('teamSpace.viewer'), value: 'viewer' }
])

// ==================== 工具函数 ====================
const fmtDate = (d: string) => { if (!d) return ''; try { return new Date(d).toLocaleDateString() } catch { return d } }
const fmtTime = (d: string) => {
  if (!d) return ''
  try {
    const dt = new Date(d)
    const now = new Date()
    const diff = now.getTime() - dt.getTime()
    if (diff < 60000) return t('teamSpace.justNow')
    if (diff < 3600000) return t('teamSpace.minutesAgo', { n: Math.floor(diff / 60000) })
    if (dt.toDateString() === now.toDateString()) return dt.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    return dt.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }) + ' ' + dt.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } catch { return d }
}

// ==================== 数据加载 ====================
const loadSpaces = async () => {
  loading.value = true
  try {
    const res = await request.get('/team-spaces/user/current')
    spaces.value = (res as any).data || []
    const results = await Promise.allSettled(
      spaces.value.map(space => request.get(`/team-spaces/${space.id}/members`))
    )
    spaces.value.forEach((space, idx) => {
      const result = results[idx]!
      spaceMembers.value[space.id] = result.status === 'fulfilled'
        ? ((result.value as any).data || []).length
        : 0
    })
  } catch { console.error('加载空间失败') }
  finally { loading.value = false }
}

const openCreateModal = () => { newSpace.value = { name: '', description: '', visibility: 'private' }; showCreateModal.value = true }

const handleCreateSpace = async () => {
  if (!newSpace.value.name.trim()) { message.warning(t('teamSpace.spaceNameRequired')); return }
  createLoading.value = true
  try {
    await request.post('/team-spaces', newSpace.value)
    message.success(t('teamSpace.spaceCreated'))
    showCreateModal.value = false
    await loadSpaces()
  } catch { message.error(t('teamSpace.createFailed')) }
  finally { createLoading.value = false }
}

const selectSpace = async (space: TeamSpace) => {
  selectedSpace.value = space
  editSpaceForm.name = space.name
  editSpaceForm.description = space.description
  editSpaceForm.visibility = space.visibility
  activeTab.value = 'chat'
  await Promise.all([fetchMembers(), fetchActivities(), fetchSharedResources(), fetchFiles(), fetchChatMessages()])
  await nextTick()
  scrollChatToBottom()
  startChatPolling()
}

const fetchMembers = async () => {
  if (!selectedSpace.value) return
  try {
    const res = await request.get(`/team-spaces/${selectedSpace.value.id}/members`)
    members.value = (res as any).data || []
  } catch { members.value = [] }
}

const fetchActivities = async () => {
  if (!selectedSpace.value) return
  try {
    const res = await request.get(`/team-spaces/${selectedSpace.value.id}/activities`, { params: { limit: 50 } })
    activities.value = (res as any).data || []
  } catch { activities.value = [] }
}

const fetchUserList = async () => {
  userListLoading.value = true
  try {
    const res = await getUserList({ page: 1, pageSize: 500 })
    const data = (res as any).data
    const records = data?.records || data?.list || (Array.isArray(data) ? data : [])
    userOptions.value = records.map((u: any) => ({ label: `${u.realName || u.username} (${u.username})`, value: String(u.id) }))
  } catch { /* ignore */ }
  finally { userListLoading.value = false }
}

const handleAddMember = async () => {
  if (!selectedSpace.value || !addMemberForm.userId) return
  addMemberLoading.value = true
  try {
    await request.post(`/team-spaces/${selectedSpace.value.id}/members`, { userId: addMemberForm.userId, role: addMemberForm.role })
    message.success(t('teamSpace.memberAdded'))
    showAddMemberModal.value = false
    addMemberForm.userId = null
    addMemberForm.role = 'viewer'
    await Promise.all([fetchMembers(), fetchActivities()])
  } catch { message.error(t('teamSpace.addMemberFailed')) }
  finally { addMemberLoading.value = false }
}

const handleRemoveMember = async (userId: string) => {
  if (!selectedSpace.value) return
  try {
    await request.delete(`/team-spaces/${selectedSpace.value.id}/members/${userId}`)
    message.success(t('teamSpace.memberRemoved'))
    await Promise.all([fetchMembers(), fetchActivities()])
  } catch { message.error(t('teamSpace.removeMemberFailed')) }
}

const handleChangeRole = async (userId: string, newRole: string) => {
  if (!selectedSpace.value) return
  try {
    await request.put(`/team-spaces/${selectedSpace.value.id}/members/${userId}`, { role: newRole })
    message.success(t('teamSpace.roleUpdated'))
    await fetchMembers()
  } catch { message.error(t('teamSpace.updateRoleFailed')) }
}

const handleDeleteSpace = async (space: TeamSpace) => {
  try {
    await request.delete(`/team-spaces/${space.id}`)
    message.success(t('teamSpace.spaceDeleted'))
    await loadSpaces()
  } catch { message.error(t('teamSpace.deleteSpaceFailed')) }
}

const handleBackToList = () => {
  stopChatPolling()
  selectedSpace.value = null
  members.value = []
  activities.value = []
  chatMessages.value = []
  spaceFiles.value = []
}

onMounted(() => { loadSpaces(); fetchUserList() })

onBeforeUnmount(() => {
  stopChatPolling()
})
</script>

<style scoped>
.team-space-page { padding: 0; }

.space-card {
  cursor: pointer;
  border: 1px solid var(--border-light) !important;
  border-radius: var(--radius-lg) !important;
  transition: all 0.3s ease;
}
.space-card:hover {
  transform: translateY(-2px);
  border-color: var(--color-primary) !important;
  box-shadow: var(--shadow-md) !important;
}

/* ===== 团队聊天样式 ===== */
.chat-card :deep(.n-card__content) {
  padding: 0 !important;
  display: flex;
  flex-direction: column;
}

.team-chat-messages {
  height: calc(100vh - 420px);
  min-height: 300px;
  overflow-y: auto;
  padding: 16px;
  background: #f8fafc;
}

.chat-msg {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  align-items: flex-start;
}

.chat-msg.mine {
  flex-direction: row-reverse;
}

.chat-avatar {
  flex-shrink: 0;
  margin-top: 2px;
}

.chat-bubble-wrap {
  max-width: 70%;
}

.chat-sender {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 2px;
  padding-left: 4px;
}

.chat-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.chat-msg.mine .chat-bubble {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
  border: none;
}

.chat-msg.mine .chat-sender {
  text-align: right;
}

.chat-time {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 3px;
  padding: 0 4px;
}

.chat-msg.mine .chat-time {
  text-align: right;
}

.chat-file-attach {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid rgba(255,255,255,0.2);
  font-size: 12px;
}

.chat-file-attach a {
  color: inherit;
  text-decoration: underline;
}

.team-chat-input {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid #e5e7eb;
  background: #fff;
  align-items: flex-end;
}

.team-chat-input :deep(.n-input) {
  flex: 1;
}







</style>

<style>
/* TeamSpaceView 深色模式（非 scoped） */
html.dark .team-chat-messages {
  background: #0f172a !important;
}
html.dark .chat-bubble {
  background: #1e293b !important;
  border-color: #334155 !important;
  color: #e2e8f0 !important;
}
html.dark .chat-sender {
  color: #94a3b8 !important;
}
html.dark .chat-time {
  color: #64748b !important;
}
html.dark .team-chat-input {
  background: #1e293b !important;
  border-top-color: #334155 !important;
}
html.dark .space-card {
  border-color: #334155 !important;
}
html.dark .space-card:hover {
  border-color: var(--color-primary) !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4) !important;
}
</style>

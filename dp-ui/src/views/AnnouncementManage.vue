<template>
  <div class="announcement-manage-page">
    <!-- Page_Header_Stats: 公告统计 (Req 1.1, 11.2) -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="22"><MegaphoneOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ totalCount }}</span>
          <span class="stat-label">{{ t('announcement.totalCount') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="22"><CheckmarkCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ enabledCount }}</span>
          <span class="stat-label">{{ t('announcement.enabled') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="22"><AlertCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ topCount }}</span>
          <span class="stat-label">{{ t('announcement.pinnedCount') }}</span>
        </div>
      </div>
    </div>

    <!-- Main_Card: 公告列表 (Req 1.1, 11.2) -->
    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <div class="header-icon-wrapper">
            <n-icon size="18"><MegaphoneOutline /></n-icon>
          </div>
          <div class="header-text">
            <span class="header-title">{{ t('announcement.title') }}</span>
            <span class="header-subtitle">{{ t('announcement.searchPlaceholder') }}</span>
          </div>
        </div>
      </template>
      <template #header-extra>
        <n-space :size="8">
          <n-button v-if="checkedRowKeys.length > 0" type="error" ghost @click="handleBatchDelete">
            <template #icon><n-icon><TrashOutline /></n-icon></template>
            {{ t('common.delete') }} ({{ checkedRowKeys.length }})
          </n-button>
          <n-button v-permission="'announcement:manage:add'" type="primary" @click="handleCreate">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            {{ t('announcement.create') }}
          </n-button>
        </n-space>
      </template>

      <!-- Query_Form: 搜索筛选 -->
      <n-form class="query-form" inline>
        <n-form-item>
          <n-input
            v-model:value="searchKeyword"
            :placeholder="t('announcement.searchPlaceholder')"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <n-icon><SearchOutline /></n-icon>
            </template>
          </n-input>
        </n-form-item>
        <n-form-item>
          <n-select
            v-model:value="filterStatus"
            :options="statusOptions"
            :placeholder="t('announcement.statusFilter')"
            clearable
            style="width: 160px;"
            @update:value="handleSearch"
          />
        </n-form-item>
        <n-form-item class="query-form-actions">
          <n-button type="primary" @click="handleSearch">{{ t('common.search') }}</n-button>
          <n-button @click="handleReset">{{ t('common.reset') }}</n-button>
        </n-form-item>
      </n-form>

      <!-- 数据表格 -->
      <n-data-table
        v-model:checked-row-keys="checkedRowKeys"
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="false"
        :scroll-x="1000"
        :row-key="(row: any) => row.id"
        striped
        class="custom-table"
      />

      <n-empty v-if="!loading && tableData.length === 0" :description="t('announcement.noData')" style="margin: 32px 0;" />

      <!-- Pagination_Wrapper -->
      <div class="pagination-wrapper">
        <div class="pagination-info">
          <n-tag type="info" size="small" round>
            {{ t('announcement.totalRecords', { count: totalCount }) }}
          </n-tag>
        </div>
        <n-pagination
          v-model:page="currentPage"
          v-model:page-size="pageSize"
          :item-count="totalCount"
          :page-sizes="[10, 20, 50]"
          show-size-picker
          show-quick-jumper
          @update:page="loadData"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-card>

    <!-- 预览弹窗 -->
    <n-modal v-model:show="showPreviewModal" preset="card" :title="previewData.title" style="width: 600px; border-radius: 16px;">
      <div class="preview-content">
        <div class="preview-meta">
          <n-tag :type="typeTagMap[previewData.type] || 'info'" size="small">{{ typeTextMap[previewData.type] || previewData.type }}</n-tag>
          <n-tag v-if="previewData.isTop === 1" type="warning" size="small">{{ t('announcement.pinned') }}</n-tag>
          <span class="preview-time">{{ formatDateTime(previewData.createTime) }}</span>
        </div>
        <div class="preview-body">{{ previewData.content }}</div>
        <div v-if="previewData.startTime || previewData.endTime" class="preview-period">
          {{ t('announcement.validPeriod') }}: {{ previewData.startTime ? previewData.startTime.substring(0, 16) : t('announcement.unlimited') }} ~ {{ previewData.endTime ? previewData.endTime.substring(0, 16) : t('announcement.unlimited') }}
        </div>
      </div>
    </n-modal>

    <!-- 新增/编辑弹窗 -->
    <n-modal v-model:show="showEditModal" preset="card" :title="editMode === 'add' ? t('announcement.create') : t('announcement.edit')" class="modal-lg" style="border-radius: 16px;">
      <n-form ref="formRef" :model="formData" :rules="formRules" label-placement="left" label-width="100px">
        <n-form-item :label="t('announcement.announcementTitle')" path="title">
          <n-input v-model:value="formData.title" :placeholder="t('announcement.titlePlaceholder')" maxlength="100" show-count />
        </n-form-item>
        <n-form-item :label="t('announcement.announcementType')" path="type">
          <n-radio-group v-model:value="formData.type">
            <n-radio value="info">
              <n-tag type="info" size="small">{{ t('announcement.notice') }}</n-tag>
            </n-radio>
            <n-radio value="success">
              <n-tag type="success" size="small">{{ t('announcement.success') }}</n-tag>
            </n-radio>
            <n-radio value="warning">
              <n-tag type="warning" size="small">{{ t('announcement.warning') }}</n-tag>
            </n-radio>
            <n-radio value="error">
              <n-tag type="error" size="small">{{ t('announcement.urgent') }}</n-tag>
            </n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item :label="t('announcement.priority')" path="priority">
          <n-radio-group v-model:value="formData.priority">
            <n-radio :value="1">{{ t('announcement.priorityLow') }}</n-radio>
            <n-radio :value="2">{{ t('announcement.priorityMedium') }}</n-radio>
            <n-radio :value="3">{{ t('announcement.priorityHigh') }}</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item :label="t('announcement.announcementContent')" path="content">
          <n-input
            v-model:value="formData.content"
            type="textarea"
            :placeholder="t('announcement.contentPlaceholder')"
            :autosize="{ minRows: 3, maxRows: 6 }"
            maxlength="500"
            show-count
          />
        </n-form-item>
        <n-form-item :label="t('announcement.validPeriod')">
          <n-space>
            <n-date-picker
              v-model:formatted-value="formData.startTime"
              type="datetime"
              :placeholder="t('announcement.startTime')"
              value-format="yyyy-MM-dd HH:mm:ss"
              clearable
            />
            <span>{{ t('announcement.to') }}</span>
            <n-date-picker
              v-model:formatted-value="formData.endTime"
              type="datetime"
              :placeholder="t('announcement.endTime')"
              value-format="yyyy-MM-dd HH:mm:ss"
              clearable
            />
          </n-space>
        </n-form-item>
        <n-form-item :label="t('announcement.publishScope')">
          <n-radio-group v-model:value="(formData as any).targetType">
            <n-radio value="all">{{ t('announcement.scopeAll') }}</n-radio>
            <n-radio value="dept">{{ t('announcement.scopeDept') }}</n-radio>
            <n-radio value="role">{{ t('announcement.scopeRole') }}</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item v-if="(formData as any).targetType === 'dept' || (formData as any).targetType === 'role'" :label="t('announcement.targetId')">
          <n-input v-model:value="(formData as any).targetIds" :placeholder="t('announcement.targetIdPlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('announcement.isTop')">
          <n-switch v-model:value="formData.isTop" :checked-value="1" :unchecked-value="0" />
        </n-form-item>
        <n-form-item :label="t('announcement.statusLabel')">
          <n-switch v-model:value="formData.status" :checked-value="1" :unchecked-value="0">
            <template #checked>{{ t('announcement.statusEnabled') }}</template>
            <template #unchecked>{{ t('announcement.statusDisabled') }}</template>
          </n-switch>
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showEditModal = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="saving" @click="handleSave">{{ t('common.save') }}</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { logger } from '@/utils/logger'
import { ref, h, onMounted } from 'vue'
import { NButton, NIcon, NTag, NSpace, useMessage, useDialog } from 'naive-ui'
import type { FormInst } from 'naive-ui'
import {
  MegaphoneOutline,
  CheckmarkCircleOutline,
  AlertCircleOutline,
  SearchOutline,
  AddOutline,
  TrashOutline,
  EyeOutline
} from '@vicons/ionicons5'
import * as announcementApi from '@/api/announcement'
import { getAnnouncementStats, updateAnnouncementStatus, batchDeleteAnnouncements } from '@/api/announcement'
import type { Announcement } from '@/api/announcement'
import { formatDateTime } from '@/utils/format'
import { hasPermission } from '@/utils/permission'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const tableData = ref<Announcement[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)
const searchKeyword = ref('')
const filterStatus = ref<number | null>(null)

// 统计数据从后端获取
const enabledCount = ref(0)
const topCount = ref(0)

const statusOptions = [
  { label: t('announcement.statusEnabled'), value: 1 },
  { label: t('announcement.statusDisabled'), value: 0 }
]

const typeTagMap: Record<string, 'info' | 'success' | 'warning' | 'error'> = {
  info: 'info',
  success: 'success',
  warning: 'warning',
  error: 'error'
}

const typeTextMap: Record<string, string> = {
  info: t('announcement.notice'),
  success: t('announcement.success'),
  warning: t('announcement.warning'),
  error: t('announcement.urgent')
}

const priorityTextMap: Record<number, string> = {
  1: t('announcement.priorityLow'),
  2: t('announcement.priorityMedium'),
  3: t('announcement.priorityHigh')
}

const columns = [
  { type: 'selection' as const, width: 40 },
  { title: 'ID', key: 'id', width: 60 },
  {
    title: t('announcement.announcementTitle'),
    key: 'title',
    width: 200,
    ellipsis: { tooltip: true }
  },
  {
    title: t('announcement.type'),
    key: 'type',
    width: 80,
    render: (row: Announcement) => h(NTag, { type: typeTagMap[row.type] || 'info', size: 'small' }, () => typeTextMap[row.type] || row.type)
  },
  {
    title: t('announcement.priority'),
    key: 'priority',
    width: 80,
    render: (row: Announcement) => priorityTextMap[row.priority] || t('announcement.priorityMedium')
  },
  {
    title: t('announcement.publishScope'),
    key: 'targetType',
    width: 90,
    render: (row: Announcement) => {
      const map: Record<string, string> = { all: t('announcement.scopeAll'), dept: t('announcement.dept'), role: t('announcement.role') }
      return map[(row as any).targetType] || t('announcement.scopeAll')
    }
  },
  {
    title: t('announcement.readCount'),
    key: 'readCount',
    width: 70,
    render: (row: Announcement) => (row as any).readCount ?? 0
  },
  {
    title: t('announcement.pinned'),
    key: 'isTop',
    width: 70,
    render: (row: Announcement) => row.isTop === 1 ? h(NTag, { type: 'warning', size: 'small' }, () => t('announcement.pinned')) : '-'
  },
  {
    title: t('announcement.statusLabel'),
    key: 'status',
    width: 80,
    render: (row: Announcement) => h(
      NTag,
      { type: row.status === 1 ? 'success' : 'error', size: 'small' },
      () => row.status === 1 ? t('announcement.statusEnabled') : t('announcement.statusDisabled')
    )
  },
  {
    title: t('announcement.validPeriod'),
    key: 'period',
    width: 180,
    render: (row: Announcement) => {
      if (!row.startTime && !row.endTime) return t('announcement.permanentValid')
      const start = row.startTime ? row.startTime.substring(0, 16) : t('announcement.unlimited')
      const end = row.endTime ? row.endTime.substring(0, 16) : t('announcement.unlimited')
      return `${start} ~ ${end}`
    }
  },
  {
    title: t('common.createTime'),
    key: 'createTime',
    width: 160,
    render: (row: Announcement) => formatDateTime(row.createTime)
  },
  {
    title: t('common.action'),
    key: 'actions',
    width: 220,
    fixed: 'right',
    render: (row: Announcement) => {
      const buttons: ReturnType<typeof h>[] = []
      buttons.push(h(NButton, { size: 'small', quaternary: true, onClick: () => handlePreview(row) }, { icon: () => h(NIcon, null, () => h(EyeOutline)), default: () => t('common.view') }))
      if (hasPermission('announcement:manage:edit')) {
        buttons.push(h(NButton, { size: 'small', onClick: () => handleEdit(row) }, () => t('common.edit')))
        buttons.push(h(NButton, {
          size: 'small',
          type: row.status === 1 ? 'warning' : 'success',
          ghost: true,
          onClick: () => handleToggleStatus(row)
        }, () => row.status === 1 ? t('announcement.statusDisabled') : t('announcement.statusEnabled')))
      }
      if (hasPermission('announcement:manage:delete')) {
        buttons.push(h(NButton, { size: 'small', type: 'error', onClick: () => handleDelete(row) }, () => t('common.delete')))
      }
      return h(NSpace, { size: 'small' }, () => buttons)
    }
  }
]

// 选中行
const checkedRowKeys = ref<number[]>([])

// 预览相关
const showPreviewModal = ref(false)
const previewData = ref<any>({})

// 编辑相关
const showEditModal = ref(false)
const editMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInst | null>(null)
const saving = ref(false)
const defaultFormData = () => ({
  title: '',
  content: '',
  type: 'info' as const,
  priority: 2,
  status: 1,
  isTop: 0,
  targetType: 'all'
})
const formData = ref<Announcement & { targetType?: string; targetIds?: string }>(defaultFormData())

const formRules = {
  title: [{ required: true, message: t('announcement.titleRequired'), trigger: 'blur' }],
  content: [{ required: true, message: t('announcement.contentRequired'), trigger: 'blur' }],
  type: [{ required: true, message: t('announcement.typeRequired'), trigger: 'change' }]
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await announcementApi.getAnnouncementList({
      page: currentPage.value,
      pageSize: pageSize.value,
      ...(searchKeyword.value ? { keyword: searchKeyword.value } : {}),
      ...(filterStatus.value != null ? { status: filterStatus.value } : {})
    })
    tableData.value = res.data?.list || []
    totalCount.value = res.data?.total || 0
    loadStats()
  } catch (error: any) {
    message.error(t('announcement.loadFailed') + ': ' + (error.message || t('common.unknownError')))
  } finally {
    loading.value = false
  }
}

// 加载统计数据
const loadStats = async () => {
  try {
    const res = await getAnnouncementStats()
    if (res.data) {
      totalCount.value = res.data.total || 0
      enabledCount.value = res.data.enabled || 0
      topCount.value = res.data.top || 0
    }
  } catch (error) {
    logger.error('加载统计数据失败', error)
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadData()
}

const handleReset = () => {
  searchKeyword.value = ''
  filterStatus.value = null
  currentPage.value = 1
  loadData()
}

const handlePageSizeChange = () => {
  currentPage.value = 1
  loadData()
}

// 新增
const handleCreate = () => {
  editMode.value = 'add'
  formData.value = defaultFormData()
  showEditModal.value = true
}

// 编辑
const handleEdit = (row: Announcement) => {
  editMode.value = 'edit'
  formData.value = { ...row }
  showEditModal.value = true
}

// 保存
const handleSave = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    if (editMode.value === 'add') {
      await announcementApi.createAnnouncement(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await announcementApi.updateAnnouncement(formData.value.id!, formData.value)
      message.success(t('common.updateSuccess'))
    }
    showEditModal.value = false
    loadData()
  } catch (error: any) {
    message.error(t('announcement.saveFailed') + ': ' + (error.message || t('common.unknownError')))
  } finally {
    saving.value = false
  }
}

// 预览
const handlePreview = (row: Announcement) => {
  previewData.value = { ...row }
  showPreviewModal.value = true
}

// 快速切换状态
const handleToggleStatus = async (row: Announcement) => {
  try {
    const newStatus = row.status === 1 ? 0 : 1
    await updateAnnouncementStatus(row.id!, newStatus)
    message.success(newStatus === 1 ? t('announcement.statusEnabled') : t('announcement.statusDisabled'))
    loadData()
  } catch (error: any) {
    message.error(error.message || t('common.unknownError'))
  }
}

// 批量删除
const handleBatchDelete = () => {
  if (checkedRowKeys.value.length === 0) return
  dialog.warning({
    title: t('announcement.deleteConfirmTitle'),
    content: t('announcement.batchDeleteConfirm', { count: checkedRowKeys.value.length }) || `确定删除选中的 ${checkedRowKeys.value.length} 条公告吗？`,
    positiveText: t('common.confirm'),
    negativeText: t('common.cancel'),
    onPositiveClick: async () => {
      try {
        await batchDeleteAnnouncements(checkedRowKeys.value)
        message.success(t('common.deleteSuccess'))
        checkedRowKeys.value = []
        loadData()
      } catch (error: any) {
        message.error(error.message || t('common.unknownError'))
      }
    }
  })
}

// 删除
const handleDelete = (row: Announcement) => {
  dialog.warning({
    title: t('announcement.deleteConfirmTitle'),
    content: t('announcement.deleteConfirmContent', { title: row.title }),
    positiveText: t('common.confirm'),
    negativeText: t('common.cancel'),
    onPositiveClick: async () => {
      try {
        await announcementApi.deleteAnnouncement(row.id!)
        message.success(t('common.deleteSuccess'))
        loadData()
      } catch (error: any) {
        message.error(t('announcement.deleteFailed') + ': ' + (error.message || t('common.unknownError')))
      }
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.announcement-manage-page {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* ========== 卡片头部增强 ========== */
.header-icon-wrapper {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%);
  color: #fff;
  box-shadow: 0 2px 8px rgba(139, 92, 246, 0.25);
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-subtitle {
  font-size: 12px;
  color: var(--text-tertiary);
}

/* 预览弹窗 */
.preview-content { display: flex; flex-direction: column; gap: 16px; }
.preview-meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.preview-time { font-size: 13px; color: #94a3b8; margin-left: auto; }
.preview-body {
  font-size: 15px;
  line-height: 1.8;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
}
.preview-period {
  font-size: 13px;
  color: #64748b;
  padding: 8px 12px;
  background: #f1f5f9;
  border-radius: 6px;
}


@media (max-width: 768px) {
  .page-header-stats {
    flex-direction: column;
  }
}
</style>

<style>
/* AnnouncementManage 深色模式（非 scoped） */
html.dark .preview-body { background: #1e293b !important; color: #e2e8f0 !important; }
html.dark .preview-period { background: #1a2536 !important; color: #94a3b8 !important; }
html.dark .preview-time { color: #64748b !important; }
html.dark .announcement-manage-page .header-icon-wrapper { box-shadow: 0 2px 8px rgba(139, 92, 246, 0.15) !important; }
html.dark .announcement-manage-page .header-title { color: #f1f5f9 !important; }
html.dark .announcement-manage-page .header-subtitle { color: #64748b !important; }
</style>

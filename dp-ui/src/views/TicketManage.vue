<template>
  <div class="ticket-page">
    <!-- Page_Header_Stats: 各状态工单数量统计 (Req 1.1, 14.3) -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="22"><AlertCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats["open"] || stats["pending"] || 0 }}</span>
          <span class="stat-label">待处理</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="22"><TimeOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats["in_progress"] || stats["processing"] || 0 }}</span>
          <span class="stat-label">处理中</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="22"><CheckmarkCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats["resolved"] || 0 }}</span>
          <span class="stat-label">已解决</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info">
          <n-icon size="22"><CloseCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats["closed"] || 0 }}</span>
          <span class="stat-label">已关闭</span>
        </div>
      </div>
    </div>

    <!-- Main_Card: 工单列表 (Req 1.1, 14.1, 14.2) -->
    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="var(--color-primary)" class="header-icon"><ChatboxEllipsesOutline /></n-icon>
          <span>工单管理</span>
        </div>
      </template>
      <template #header-extra>
        <n-button type="primary" @click="handleCreateTicket">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          创建工单
        </n-button>
      </template>

      <!-- 筛选工具栏 -->
      <div class="ticket-toolbar">
        <n-space :size="8" align="center" :wrap="false">
          <n-input v-model:value="searchKeyword" placeholder="搜索工单标题/编号" clearable style="width: 200px;" @keydown.enter="handleSearch" @clear="handleSearch">
            <template #prefix><n-icon size="16"><SearchOutline /></n-icon></template>
          </n-input>
          <n-select v-model:value="filterStatus" :options="statusFilterOptions" placeholder="状态" clearable style="width: 110px;" @update:value="handleSearch" />
          <n-select v-model:value="filterCategory" :options="categoryOptions" placeholder="分类" clearable style="width: 110px;" @update:value="handleSearch" />
          <n-select v-model:value="filterPriority" :options="priorityFilterOptions" placeholder="优先级" clearable style="width: 110px;" @update:value="handleSearch" />
          <n-button type="primary" size="small" @click="handleSearch">搜索</n-button>
          <n-button size="small" @click="handleReset">重置</n-button>
        </n-space>
        <n-space :size="8" align="center">
          <n-button v-if="checkedRowKeys.length > 0" size="small" type="warning" @click="handleBatchClose">
            批量关闭 ({{ checkedRowKeys.length }})
          </n-button>
          <n-button v-if="checkedRowKeys.length > 0" size="small" quaternary @click="checkedRowKeys = []">取消选择</n-button>
          <n-button size="small" quaternary @click="handleExportTickets">
            <template #icon><n-icon size="14"><DownloadOutline /></n-icon></template>
            导出
          </n-button>
        </n-space>
      </div>

      <!-- 数据表格 -->
      <n-data-table
        v-model:checked-row-keys="checkedRowKeys"
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="false"
        :row-key="(row: Ticket) => row.id"
        :scroll-x="1300"
        striped
        class="custom-table"
      />

      <!-- 空状态 -->
      <n-empty v-if="!loading && tableData.length === 0" description="暂无工单数据" style="margin: 32px 0;" />

      <!-- Pagination_Wrapper -->
      <div class="pagination-wrapper">
        <div class="pagination-info">
          <n-tag type="info" size="small" round>共 {{ totalCount }} 条记录</n-tag>
        </div>
        <n-pagination
          v-model:page="currentPage"
          v-model:page-size="pageSize"
          :item-count="totalCount"
          :page-sizes="PAGE_SIZES"
          show-size-picker
          show-quick-jumper
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-card>

    <!-- 工单详情抽屉 (Req 14.2) -->
    <n-drawer v-model:show="showDetail" :width="600" placement="right">
      <n-drawer-content closable>
        <template #header>
          <n-space align="center" :size="8">
            <n-tag :type="detailTicket ? (priorityColorMap[detailTicket.priority] || 'default') : 'default'" size="small">{{ detailTicket ? (priorityLabelMap[detailTicket.priority] || detailTicket.priority) : '' }}</n-tag>
            <span>{{ detailTicket?.title || '工单详情' }}</span>
            <n-tag :type="detailTicket ? (statusColorMap[detailTicket.status] || 'default') : 'default'" size="small">{{ detailTicket ? (statusLabelMap[detailTicket.status] || detailTicket.status) : '' }}</n-tag>
          </n-space>
        </template>
        <template v-if="detailTicket">
          <n-descriptions bordered :column="2" label-placement="left" size="small">
            <n-descriptions-item label="工单编号">{{ detailTicket.ticketNo || detailTicket.id }}</n-descriptions-item>
            <n-descriptions-item label="分类">
              <n-tag size="small">{{ categoryLabelMap[detailTicket.category] || detailTicket.category }}</n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="提交人">{{ detailTicket.submitterName || '-' }}</n-descriptions-item>
            <n-descriptions-item label="处理人">
              <n-space align="center" :size="4">
                <span>{{ detailTicket.assigneeName || '未分配' }}</span>
                <n-button v-if="isCreator" text size="tiny" type="primary" @click="showAssignModal = true">{{ detailTicket.assigneeName ? '重新分配' : '分配' }}</n-button>
              </n-space>
            </n-descriptions-item>
            <n-descriptions-item label="创建时间">{{ formatDateTime(detailTicket.createTime || detailTicket.createdAt) }}</n-descriptions-item>
            <n-descriptions-item label="更新时间">{{ formatDateTime(detailTicket.updateTime || detailTicket.updatedAt) }}</n-descriptions-item>
          </n-descriptions>

          <!-- SLA 耗时指示 -->
          <div v-if="ticketElapsed" class="ticket-sla-bar">
            <n-icon size="14"><TimeOutline /></n-icon>
            <span>已耗时: {{ ticketElapsed }}</span>
            <n-tag v-if="ticketSlaLevel === 'danger'" type="error" size="tiny">超时</n-tag>
            <n-tag v-else-if="ticketSlaLevel === 'warn'" type="warning" size="tiny">临近超时</n-tag>
          </div>

          <n-divider>描述</n-divider>
          <div class="ticket-description">{{ detailTicket.description || '无描述' }}</div>

          <!-- 附件区域 -->
          <template v-if="detailTicket.attachments && detailTicket.attachments.length > 0">
            <n-divider>附件 ({{ detailTicket.attachments.length }})</n-divider>
            <n-space vertical :size="4">
              <div v-for="(url, idx) in detailTicket.attachments" :key="idx" class="attachment-item">
                <n-icon size="16"><AttachOutline /></n-icon>
                <a :href="'/api' + url" target="_blank" class="attachment-link">附件 {{ idx + 1 }} - {{ getFileName(url) }}</a>
                <n-button text size="tiny" tag="a" :href="'/api' + url" target="_blank">下载</n-button>
              </div>
            </n-space>
          </template>

          <template v-if="detailTicket.resolution">
            <n-divider>解决方案</n-divider>
            <div class="ticket-description">{{ detailTicket.resolution }}</div>
          </template>

          <!-- 操作区 -->
          <n-divider>操作</n-divider>
          <n-space>
            <n-button v-if="isCreator" size="small" type="info" @click="showAssignModal = true">分配处理人</n-button>
            <n-button v-if="isAssignee" size="small" type="warning" @click="showStatusModal = true">更新状态</n-button>
            <n-popconfirm v-if="isCreator" @positive-click="handleDeleteTicket">
              <template #trigger>
                <n-button size="small" type="error">撤销工单</n-button>
              </template>
              确定要撤销此工单吗？撤销后不可恢复。
            </n-popconfirm>
            <n-text v-if="!isCreator && !isAssignee" depth="3" style="font-size:13px;">只有创建人可分配处理人/撤销工单，只有处理人可更新状态</n-text>
          </n-space>

          <!-- 评论列表 -->
          <n-divider>评论 ({{ comments.length }})</n-divider>
          <div v-if="comments.length === 0" style="color: var(--text-secondary); font-size: 13px;">暂无评论</div>
          <n-timeline v-else>
            <n-timeline-item v-for="comment in comments" :key="comment.id" :time="formatDateTime(comment.createTime || comment.createdAt)" :title="comment.userName || comment.authorName">
              {{ comment.content }}
            </n-timeline-item>
          </n-timeline>

          <!-- 添加评论 -->
          <div style="margin-top: 16px;">
            <n-input v-model:value="newComment" type="textarea" placeholder="输入评论内容..." :rows="3" />
            <n-button type="primary" size="small" style="margin-top: 8px;" :loading="commentLoading" :disabled="!newComment.trim()" @click="handleAddComment">添加评论</n-button>
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>

    <!-- 分配处理人弹窗 -->
    <n-modal v-model:show="showAssignModal" preset="card" title="分配处理人" style="width: 480px; border-radius: 16px;">
      <n-form label-placement="left" label-width="80px">
        <n-form-item label="选择用户">
          <n-select v-model:value="assignForm.userId" :options="userOptions" :loading="userListLoading" filterable placeholder="搜索并选择处理人" @update:value="handleUserSelect" />
        </n-form-item>
        <n-form-item>
          <n-space style="width: 100%; justify-content: flex-end;">
            <n-button @click="showAssignModal = false">取消</n-button>
            <n-button type="primary" :loading="assignLoading" :disabled="!assignForm.userId" @click="handleAssign">确定分配</n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-modal>

    <!-- 更新状态弹窗 -->
    <n-modal v-model:show="showStatusModal" preset="card" title="更新工单状态" style="width: 480px; border-radius: 16px;">
      <n-form label-placement="left" label-width="80px">
        <n-form-item label="状态">
          <n-select v-model:value="statusForm.status" :options="statusUpdateOptions" placeholder="请选择状态" />
        </n-form-item>
        <n-form-item v-if="statusForm.status === 'resolved'" label="解决方案">
          <n-input v-model:value="statusForm.resolution" type="textarea" placeholder="请输入解决方案" :rows="3" />
        </n-form-item>
        <n-form-item>
          <n-space style="width: 100%; justify-content: flex-end;">
            <n-button @click="showStatusModal = false">取消</n-button>
            <n-button type="primary" :loading="statusLoading" @click="handleUpdateStatus">确定</n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-modal>

    <!-- 创建工单弹窗 -->
    <n-modal v-model:show="showCreateModal" preset="card" title="创建工单" style="width: 600px; border-radius: 16px;">
      <n-form ref="createFormRef" :model="createForm" :rules="createFormRules" label-placement="left" label-width="80px">
        <n-form-item label="快速模板">
          <n-select :options="ticketTemplateOptions" placeholder="选择模板快速填充（可选）" clearable @update:value="applyTemplate" />
        </n-form-item>
        <n-form-item label="标题" path="title">
          <n-input v-model:value="createForm.title" placeholder="请输入工单标题" />
        </n-form-item>
        <n-form-item label="分类" path="category">
          <n-select v-model:value="createForm.category" :options="categoryOptions" placeholder="请选择分类" />
        </n-form-item>
        <n-form-item label="优先级" path="priority">
          <n-select v-model:value="createForm.priority" :options="priorityFilterOptions" placeholder="请选择优先级" />
        </n-form-item>
        <n-form-item label="指派给">
          <n-select v-model:value="createForm.assigneeId" :options="userOptions" :loading="userListLoading" filterable clearable placeholder="选择处理人（可选）" @update:value="handleCreateUserSelect" />
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input v-model:value="createForm.description" type="textarea" placeholder="请详细描述问题" :rows="4" />
        </n-form-item>
        <n-form-item label="附件">
          <n-upload :action="uploadAction" :headers="uploadHeaders" :max="5" multiple @finish="handleUploadFinish" @remove="handleUploadRemove">
            <n-button>上传附件（最多5个）</n-button>
          </n-upload>
        </n-form-item>
        <n-form-item>
          <n-space style="width: 100%; justify-content: flex-end;">
            <n-button @click="showCreateModal = false">取消</n-button>
            <n-button type="primary" :loading="createLoading" @click="handleSubmitCreate">提交</n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, onMounted, h, computed } from 'vue'
import { NButton, NTag, NIcon, NSpace, NText, useMessage, type DataTableColumns, type FormInst, type UploadFileInfo } from 'naive-ui'
import {
  AlertCircleOutline, TimeOutline, CheckmarkCircleOutline, CloseCircleOutline,
  ChatboxEllipsesOutline, AddOutline, AttachOutline, SearchOutline, DownloadOutline
} from '@vicons/ionicons5'
import {
  getTicketList, getTicketDetail, getTicketStats, assignTicket, updateTicketStatus,
  getComments, addComment, createTicket, deleteTicket, type Ticket, type TicketComment
} from '@/api/ticket'
import { getUserList } from '@/api/system/user'
import { useUserStore } from '@/stores/user'
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE, PAGE_SIZES } from '@/constants'
import { formatDateTime } from '@/utils/format'
import { handleApiError } from '@/utils/error'
import { initMessage } from '@/utils/message'

const message = useMessage()
initMessage(message)

const userStore = useUserStore()
const currentUserId = computed(() => String(userStore.userInfo?.id || ''))

// 权限判断
const isCreator = computed(() => detailTicket.value && currentUserId.value === detailTicket.value.submitterId)
const isAssignee = computed(() => detailTicket.value && currentUserId.value === detailTicket.value.assigneeId)

// 上传配置
const uploadAction = '/api/file/upload'
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

// 颜色/标签映射
const priorityColorMap: Record<string, 'info' | 'warning' | 'error' | 'default'> = { low: 'info', medium: 'warning', high: 'error', critical: 'error' }
const priorityLabelMap: Record<string, string> = { low: '低', medium: '中', high: '高', critical: '紧急' }
const statusColorMap: Record<string, 'default' | 'info' | 'success' | 'warning'> = { open: 'warning', pending: 'warning', in_progress: 'info', processing: 'info', resolved: 'success', closed: 'default' }
const statusLabelMap: Record<string, string> = { open: '待处理', pending: '待处理', in_progress: '处理中', processing: '处理中', resolved: '已解决', closed: '已关闭' }
const categoryLabelMap: Record<string, string> = { technical: '技术问题', feature: '功能请求', bug: '缺陷报告', inquiry: '咨询', consultation: '咨询', feature_request: '功能请求' }

// 统计数据
const stats = ref<Record<string, number>>({})

// 表格数据
const loading = ref(false)
const tableData = ref<Ticket[]>([])
const totalCount = ref(0)
const currentPage = ref(DEFAULT_PAGE)
const pageSize = ref(DEFAULT_PAGE_SIZE)

// 筛选
const searchKeyword = ref('')
const checkedRowKeys = ref<number[]>([])
const filterStatus = ref<string | null>(null)
const filterCategory = ref<string | null>(null)
const filterPriority = ref<string | null>(null)

const statusFilterOptions = [
  { label: '待处理', value: 'pending' },
  { label: '处理中', value: 'processing' },
  { label: '已解决', value: 'resolved' },
  { label: '已关闭', value: 'closed' }
]
const categoryOptions = [
  { label: '技术问题', value: 'technical' },
  { label: '功能请求', value: 'feature_request' },
  { label: '缺陷报告', value: 'bug' },
  { label: '咨询', value: 'consultation' }
]
const priorityFilterOptions = [
  { label: '低', value: 'low' },
  { label: '中', value: 'medium' },
  { label: '高', value: 'high' },
  { label: '紧急', value: 'urgent' }
]
const statusUpdateOptions = [
  { label: '待处理', value: 'pending' },
  { label: '处理中', value: 'processing' },
  { label: '已解决', value: 'resolved' },
  { label: '已关闭', value: 'closed' }
]

// 用户列表（用于分配）
const userOptions = ref<{ label: string; value: string }[]>([])
const userListLoading = ref(false)
const userMap = ref<Record<string, string>>({}) // userId -> userName

// 详情抽屉
const showDetail = ref(false)
const detailTicket = ref<Ticket | null>(null)
const comments = ref<TicketComment[]>([])
const newComment = ref('')
const commentLoading = ref(false)

// SLA 耗时计算
const SLA_HOURS: Record<string, number> = { critical: 4, high: 8, medium: 24, low: 72 }
const ticketElapsed = computed(() => {
  if (!detailTicket.value) return ''
  const createStr = detailTicket.value.createTime || detailTicket.value.createdAt
  if (!createStr) return ''
  const ms = Date.now() - new Date(createStr).getTime()
  if (ms < 0) return ''
  const hours = Math.floor(ms / 3600000)
  const mins = Math.floor((ms % 3600000) / 60000)
  if (hours >= 24) return `${Math.floor(hours / 24)}天${hours % 24}小时`
  return `${hours}小时${mins}分钟`
})
const ticketSlaLevel = computed(() => {
  if (!detailTicket.value) return ''
  const status = detailTicket.value.status
  if (status === 'resolved' || status === 'closed') return ''
  const createStr = detailTicket.value.createTime || detailTicket.value.createdAt
  if (!createStr) return ''
  const hours = (Date.now() - new Date(createStr).getTime()) / 3600000
  const limit = SLA_HOURS[detailTicket.value.priority] || 72
  if (hours > limit) return 'danger'
  if (hours > limit * 0.75) return 'warn'
  return ''
})

// 分配处理人
const showAssignModal = ref(false)
const assignForm = reactive({ userId: null as string | null, userName: '' })
const assignLoading = ref(false)
const deleteLoading = ref(false)

// 更新状态
const showStatusModal = ref(false)
const statusForm = reactive({ status: '' as string, resolution: '' })
const statusLoading = ref(false)

// 工单模板
const ticketTemplateOptions = [
  { label: '系统故障报告', value: 'system_fault' },
  { label: '权限申请', value: 'permission_request' },
  { label: '数据修复', value: 'data_fix' },
  { label: '功能需求', value: 'feature_request' },
  { label: '账号问题', value: 'account_issue' }
]
const templateData: Record<string, { title: string; category: string; priority: string; description: string }> = {
  system_fault: { title: '【系统故障】', category: 'bug', priority: 'high', description: '故障现象：\n影响范围：\n复现步骤：\n1. \n2. \n3. ' },
  permission_request: { title: '【权限申请】', category: 'other', priority: 'medium', description: '申请权限：\n申请原因：\n需要访问的资源：' },
  data_fix: { title: '【数据修复】', category: 'bug', priority: 'high', description: '涉及表/数据源：\n问题数据描述：\n期望结果：' },
  feature_request: { title: '【功能需求】', category: 'feature', priority: 'medium', description: '需求描述：\n使用场景：\n期望效果：' },
  account_issue: { title: '【账号问题】', category: 'other', priority: 'medium', description: '账号/用户名：\n问题描述：\n期望处理方式：' }
}
const applyTemplate = (key: string | null) => {
  if (!key || !templateData[key]) return
  const t = templateData[key]
  createForm.title = t.title
  createForm.category = t.category
  createForm.priority = t.priority
  createForm.description = t.description
}

// 创建工单
const showCreateModal = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInst | null>(null)
const createForm = reactive({
  title: '',
  category: '' as string,
  priority: 'medium' as string,
  description: '',
  assigneeId: null as string | null,
  assigneeName: '',
  attachments: [] as string[]
})
const createFormRules = {
  title: { required: true, message: '请输入工单标题', trigger: 'blur' },
  category: { required: true, message: '请选择分类', trigger: 'change' },
  priority: { required: true, message: '请选择优先级', trigger: 'change' },
  description: { required: true, message: '请描述问题', trigger: 'blur' }
}

// 加载用户列表
const fetchUserList = async () => {
  userListLoading.value = true
  try {
    const res = await getUserList({ page: 1, pageSize: 500 })
    const data = (res as any).data
    const records = data?.records || data?.list || (Array.isArray(data) ? data : [])
    userOptions.value = records.map((u: any) => ({
      label: `${u.realName || u.username} (${u.username})`,
      value: String(u.id)
    }))
    records.forEach((u: any) => {
      userMap.value[String(u.id)] = u.realName || u.username
    })
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    userListLoading.value = false
  }
}

const handleUserSelect = (val: string) => {
  assignForm.userName = userMap.value[val] || ''
}

const handleCreateUserSelect = (val: string | null) => {
  createForm.assigneeName = val ? (userMap.value[val] || '') : ''
}

// 上传回调
const handleUploadFinish = ({ file, event }: { file: UploadFileInfo; event?: ProgressEvent }) => {
  try {
    const res = JSON.parse((event?.target as XMLHttpRequest)?.response || '{}')
    if (res.code === 200 && res.data?.url) {
      createForm.attachments.push(res.data.url)
      file.url = res.data.url
    }
  } catch { /* ignore */ }
  return file
}

const handleUploadRemove = ({ file }: { file: UploadFileInfo }) => {
  if (file.url) {
    createForm.attachments = createForm.attachments.filter(u => u !== file.url)
  }
  return true
}

const getFileName = (url: string) => {
  const parts = url.split('/')
  return parts[parts.length - 1] || '文件'
}

// 表格列
const getElapsedTime = (dateStr: string): string => {
  if (!dateStr) return '-'
  const diff = Date.now() - new Date(dateStr).getTime()
  const hours = Math.floor(diff / 3600000)
  if (hours < 1) return '< 1h'
  if (hours < 24) return `${hours}h`
  const days = Math.floor(hours / 24)
  return `${days}d ${hours % 24}h`
}

const getSlaColor = (dateStr: string, status: string): string => {
  if (status === 'resolved' || status === 'closed') return 'var(--color-success)'
  const hours = (Date.now() - new Date(dateStr).getTime()) / 3600000
  if (hours > 48) return 'var(--color-error)'
  if (hours > 24) return 'var(--color-warning)'
  return 'var(--text-secondary)'
}

const columns: DataTableColumns<Ticket> = [
  {
    type: 'selection',
    width: 48
  },
  {
    title: '工单编号',
    key: 'ticketNo',
    width: 130,
    render: (row) => h('span', { style: 'font-family: monospace; color: var(--color-primary); cursor: pointer;', onClick: () => handleViewDetail(row) }, row.ticketNo || `#${row.id}`)
  },
  { title: '标题', key: 'title', ellipsis: { tooltip: true }, render: (row) => h('span', { style: 'cursor: pointer;', onClick: () => handleViewDetail(row) }, row.title) },
  {
    title: '分类', key: 'category', width: 100,
    render: (row) => h(NTag, { size: 'small', bordered: false }, { default: () => categoryLabelMap[row.category] || row.category })
  },
  {
    title: '优先级', key: 'priority', width: 80,
    render: (row) => h(NTag, { type: priorityColorMap[row.priority] || 'default', size: 'small' }, { default: () => priorityLabelMap[row.priority] || row.priority })
  },
  {
    title: '状态', key: 'status', width: 80,
    render: (row) => h(NTag, { type: statusColorMap[row.status] || 'default', size: 'small' }, { default: () => statusLabelMap[row.status] || row.status })
  },
  { title: '提交人', key: 'submitterName', width: 90 },
  { title: '处理人', key: 'assigneeName', width: 90, render: (row) => row.assigneeName || h('span', { style: 'color: var(--text-tertiary)' }, '未分配') },
  {
    title: '附件', key: 'attachments', width: 60,
    render: (row) => {
      const count = row.attachments?.length || 0
      return count > 0 ? h(NSpace, { align: 'center', size: 2 }, { default: () => [h(NIcon, { size: 14 }, { default: () => h(AttachOutline) }), h('span', count)] }) : '-'
    }
  },
  {
    title: '耗时', key: 'elapsed', width: 80,
    render: (row) => {
      const timeStr = row.createTime || row.createdAt || ''
      return h('span', { style: `font-size: 12px; font-weight: 500; color: ${getSlaColor(timeStr, row.status)}` }, getElapsedTime(timeStr))
    }
  },
  { title: '创建时间', key: 'createTime', width: 160, render: (row) => formatDateTime(row.createTime || row.createdAt) },
  {
    title: '操作', key: 'actions', width: 80, fixed: 'right' as const,
    render: (row) => h(NButton, { size: 'small', type: 'primary', text: true, onClick: () => handleViewDetail(row) }, { default: () => '详情' })
  }
]

// 获取统计数据
const fetchStats = async () => {
  try {
    const res = await getTicketStats()
    const data = (res as any).data || res || {}
    stats.value = data.byStatus || data || {}
  } catch (error) {
    console.error('获取工单统计失败:', error)
  }
}

// 获取列表数据
const fetchData = async () => {
  loading.value = true
  try {
    const params: any = { page: currentPage.value, size: pageSize.value }
    if (searchKeyword.value?.trim()) params.keyword = searchKeyword.value.trim()
    if (filterStatus.value) params.status = filterStatus.value
    if (filterCategory.value) params.category = filterCategory.value
    if (filterPriority.value) params.priority = filterPriority.value
    const res = await getTicketList(params)
    const data = (res as any).data || res
    if (Array.isArray(data)) {
      tableData.value = data
      totalCount.value = data.length
    } else if (data?.records) {
      tableData.value = data.records
      totalCount.value = data.total || 0
    } else {
      tableData.value = []
      totalCount.value = 0
    }
  } catch (error) {
    message.error(handleApiError(error, '获取工单列表'))
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { currentPage.value = 1; fetchData() }
const handleReset = () => {
  searchKeyword.value = ''
  filterStatus.value = null
  filterCategory.value = null
  filterPriority.value = null
  currentPage.value = 1
  fetchData()
}

const handleBatchClose = async () => {
  let successCount = 0
  for (const id of checkedRowKeys.value) {
    try {
      await updateTicketStatus(id, { status: 'closed' })
      successCount++
    } catch { /* skip */ }
  }
  message.success(`批量关闭完成，成功 ${successCount} 条`)
  checkedRowKeys.value = []
  fetchData()
  fetchStats()
}
const handlePageChange = (page: number) => { currentPage.value = page; fetchData() }
const handlePageSizeChange = (size: number) => { pageSize.value = size; currentPage.value = 1; fetchData() }

// 查看详情
const handleViewDetail = async (row: Ticket) => {
  try {
    const res = await getTicketDetail(row.id)
    detailTicket.value = (res as any).data || res
    showDetail.value = true
    fetchComments(row.id)
  } catch (error) {
    message.error(handleApiError(error, '获取工单详情'))
  }
}

const fetchComments = async (ticketId: number) => {
  try {
    const res = await getComments(ticketId)
    comments.value = (res as any).data || res || []
    if (!Array.isArray(comments.value)) comments.value = []
  } catch { comments.value = [] }
}

const handleAddComment = async () => {
  if (!detailTicket.value || !newComment.value.trim()) return
  commentLoading.value = true
  try {
    await addComment(detailTicket.value.id, { content: newComment.value.trim() })
    message.success('评论添加成功')
    newComment.value = ''
    fetchComments(detailTicket.value.id)
  } catch (error) {
    message.error(handleApiError(error, '添加评论'))
  } finally { commentLoading.value = false }
}

// 分配处理人
const handleAssign = async () => {
  if (!detailTicket.value || !assignForm.userId) return
  assignLoading.value = true
  try {
    await assignTicket(detailTicket.value.id, { assigneeId: assignForm.userId, assigneeName: assignForm.userName })
    message.success('分配成功')
    showAssignModal.value = false
    assignForm.userId = null
    assignForm.userName = ''
    handleViewDetail(detailTicket.value)
    fetchData()
    fetchStats()
  } catch (error) {
    message.error(handleApiError(error, '分配处理人'))
  } finally { assignLoading.value = false }
}

// 撤销工单
const handleDeleteTicket = async () => {
  if (!detailTicket.value) return
  deleteLoading.value = true
  try {
    await deleteTicket(detailTicket.value.id)
    message.success('工单已撤销')
    showDetail.value = false
    detailTicket.value = null
    fetchData()
    fetchStats()
  } catch (error) {
    message.error(handleApiError(error, '撤销工单'))
  } finally { deleteLoading.value = false }
}

// 更新状态
const handleUpdateStatus = async () => {
  if (!detailTicket.value || !statusForm.status) return
  statusLoading.value = true
  try {
    const data: { status: string; resolution?: string } = { status: statusForm.status }
    if (statusForm.status === 'resolved' && statusForm.resolution) data.resolution = statusForm.resolution
    await updateTicketStatus(detailTicket.value.id, data)
    message.success('状态更新成功')
    showStatusModal.value = false
    statusForm.status = ''
    statusForm.resolution = ''
    handleViewDetail(detailTicket.value)
    fetchData()
    fetchStats()
  } catch (error) {
    message.error(handleApiError(error, '更新状态'))
  } finally { statusLoading.value = false }
}

// 创建工单
const handleCreateTicket = () => {
  createForm.title = ''
  createForm.category = ''
  createForm.priority = 'medium'
  createForm.description = ''
  createForm.assigneeId = null
  createForm.assigneeName = ''
  createForm.attachments = []
  showCreateModal.value = true
}

const handleSubmitCreate = async () => {
  if (!createFormRef.value) return
  try { await createFormRef.value.validate() } catch { return }
  createLoading.value = true
  try {
    await createTicket({
      title: createForm.title,
      category: createForm.category,
      priority: createForm.priority as Ticket['priority'],
      description: createForm.description,
      assigneeId: createForm.assigneeId || undefined,
      assigneeName: createForm.assigneeName || undefined,
      attachments: createForm.attachments.length > 0 ? createForm.attachments : undefined
    } as any)
    message.success('工单创建成功')
    showCreateModal.value = false
    fetchData()
    fetchStats()
  } catch (error) {
    message.error(handleApiError(error, '创建工单'))
  } finally { createLoading.value = false }
}

const handleExportTickets = () => {
  if (tableData.value.length === 0) { message.warning('暂无数据可导出'); return }
  const headers = ['工单编号', '标题', '分类', '优先级', '状态', '提交人', '处理人', '创建时间']
  let csv = headers.join(',') + '\n'
  tableData.value.forEach(row => {
    csv += [
      row.ticketNo || `#${row.id}`,
      `"${(row.title || '').replace(/"/g, '""')}"`,
      categoryLabelMap[row.category] || row.category,
      priorityLabelMap[row.priority] || row.priority,
      statusLabelMap[row.status] || row.status,
      row.submitterName || '-',
      row.assigneeName || '未分配',
      formatDateTime(row.createTime || row.createdAt)
    ].join(',') + '\n'
  })
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `工单导出_${new Date().toLocaleDateString()}.csv`
  link.click()
  message.success('导出成功')
}

onMounted(() => {
  fetchStats()
  fetchData()
  fetchUserList()
})
</script>

<style scoped>
.ticket-page {
  animation: fadeIn 0.3s ease-out;
}

.ticket-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.ticket-description {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 0;
}

.attachment-link {
  color: var(--color-primary);
  text-decoration: none;
  font-size: 13px;
}

.attachment-link:hover {
  text-decoration: underline;
}

.ticket-sla-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #fefce8 0%, #fef9c3 100%);
  border-radius: 6px;
  font-size: 13px;
  color: #854d0e;
}
</style>

<template>
  <div class="data-transfer-log-page">
    <!-- Page_Header_Stats: 执行日志统计 (Req 1.1, 10.4) -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><ListOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ statistics.total }}</span>
          <span class="stat-label">总执行次数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><CheckmarkCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ statistics.success }}</span>
          <span class="stat-label">成功次数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-error">
          <n-icon size="24"><CloseCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ statistics.fail }}</span>
          <span class="stat-label">失败次数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info">
          <n-icon size="24"><PieChartOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ statistics.successRate }}</span>
          <span class="stat-label">成功率</span>
        </div>
      </div>
    </div>

    <!-- Main_Card: 日志列表 (Req 1.1, 10.4) -->
    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="20" color="var(--color-primary)" class="header-icon"><TimeOutline /></n-icon>
          <span>日志列表</span>
        </div>
      </template>
      <template #header-extra>
        <n-space>
          <n-button @click="goBack">
            <template #icon><n-icon :component="ArrowBackOutline" /></template>
            返回任务列表
          </n-button>
          <n-button @click="loadData">
            <template #icon><n-icon :component="RefreshOutline" /></template>
            刷新
          </n-button>
        </n-space>
      </template>

      <!-- Query_Form: 搜索筛选 (Req 1.4, 18.6) -->
      <n-form class="query-form" inline>
        <n-form-item>
          <n-input
            v-model:value="searchJobName"
            placeholder="请输入任务名称"
            clearable
            @keyup.enter="handleSearch"
            @clear="resetSearch"
          >
            <template #prefix><n-icon :component="SearchOutline" /></template>
          </n-input>
        </n-form-item>
        <n-form-item>
          <n-select
            v-model:value="searchStatus"
            placeholder="全部状态"
            clearable
            style="min-width: 120px"
            :options="statusOptions"
            @update:value="handleSearch"
          />
        </n-form-item>
        <n-form-item class="query-form-actions">
          <n-button type="primary" @click="handleSearch">搜索</n-button>
          <n-button @click="resetSearch">重置</n-button>
        </n-form-item>
      </n-form>

      <!-- Empty State (Req 16.7) -->
      <n-empty v-if="!loading && tableData.length === 0" description="暂无执行日志数据" class="empty-state" />

      <!-- Data Table (Req 1.6) -->
      <n-data-table
        v-else
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="false"
        :row-key="rowKey"
        :scroll-x="1200"
        remote
        striped
        class="custom-table"
      />

      <!-- Pagination_Wrapper (Req 1.5) -->
      <div class="pagination-wrapper">
        <div class="pagination-info">
          <n-tag type="info" size="small" round>共 {{ pagination.itemCount }} 条记录</n-tag>
        </div>
        <n-pagination
          :page="pagination.page"
          :page-size="pagination.pageSize"
          :item-count="pagination.itemCount"
          :page-sizes="[10, 25, 50]"
          show-size-picker
          show-quick-jumper
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-card>

    <!-- 日志详情弹窗 -->
    <n-modal v-model:show="showLogModal" preset="card" title="执行日志详情" style="width: 700px; border-radius: 16px;">
      <!-- 执行概要 -->
      <div class="log-summary">
        <div class="summary-item">
          <p class="summary-label">执行状态</p>
          <p class="summary-value">
            <n-tag :type="getStatusType(logDetail?.executeStatus)" size="small">
              <template #icon><n-icon :component="getStatusIcon(logDetail?.executeStatus)" /></template>
              {{ getStatusText(logDetail?.executeStatus) }}
            </n-tag>
          </p>
        </div>
        <div class="summary-item">
          <p class="summary-label">执行耗时</p>
          <p class="summary-value">{{ logDetail?.executeTime || 0 }} 秒</p>
        </div>
        <div class="summary-item">
          <p class="summary-label">读取记录</p>
          <p class="summary-value read">{{ logDetail?.readCount || 0 }}</p>
        </div>
        <div class="summary-item">
          <p class="summary-label">写入记录</p>
          <p class="summary-value write">{{ logDetail?.writeCount || 0 }}</p>
        </div>
      </div>
      <!-- 日志内容 -->
      <div class="log-content">
        <pre>{{ formatLogContent(logDetail) }}</pre>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, onMounted, h, computed } from 'vue'
import { useMessage, NButton, NIcon, NTag, NProgress } from 'naive-ui'
import { useRoute, useRouter } from 'vue-router'
import {
  ListOutline, CheckmarkCircleOutline, CloseCircleOutline,
  PieChartOutline, TimeOutline, ArrowBackOutline, RefreshOutline, SearchOutline,
  EyeOutline, HandLeftOutline, TimerOutline, CheckmarkOutline, CloseOutline
} from '@vicons/ionicons5'
import { getJobLogList, type DataxJobLog } from '@/api/dataxJob'
import request from '@/api/request'

const message = useMessage()
const route = useRoute()
const router = useRouter()

// 从URL获取jobId
const jobId = computed(() => {
  const val = route.query['jobId']
  return val ? Number(val) : null
})
const jobIdParam = computed(() => {
  const val = jobId.value
  return val !== null ? val : undefined
})

// 统计数据
const statistics = ref({ total: 0, success: 0, fail: 0, successRate: '0%' })

// 列表数据
const tableData = ref<DataxJobLog[]>([])
const loading = ref(false)
const searchJobName = ref('')
const searchStatus = ref<number | null>(null)

const statusOptions = [
  { label: '成功', value: 1 },
  { label: '失败', value: 0 },
  { label: '运行中', value: 2 }
]

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 25, 50]
})

// 日志详情弹窗
const showLogModal = ref(false)
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const logDetail = ref<any>(null)

const rowKey = (row: DataxJobLog) => row.id

const getStatusType = (status?: number) => {
  if (status === 1) return 'success'
  if (status === 0) return 'error'
  return 'warning'
}

const getStatusText = (status?: number) => {
  if (status === 1) return '成功'
  if (status === 0) return '失败'
  return '运行中'
}

const getStatusIcon = (status?: number) => {
  if (status === 1) return CheckmarkOutline
  if (status === 0) return CloseOutline
  return TimerOutline
}

// 表格列定义
const columns = [
  { title: '任务名称', key: 'jobName', width: 180, ellipsis: { tooltip: true } },
  {
    title: '执行状态', key: 'executeStatus', width: 100,
    render: (row: DataxJobLog) => {
      if (row.executeStatus === 2) {
        return h(NProgress, { type: 'line', percentage: 50, indicatorPlacement: 'inside', processing: true, style: 'width: 80px' })
      }
      return h(NTag, { type: getStatusType(row.executeStatus), size: 'small' }, {
        icon: () => h(NIcon, null, () => h(getStatusIcon(row.executeStatus))),
        default: () => getStatusText(row.executeStatus)
      })
    }
  },
  {
    title: '触发方式', key: 'triggerType', width: 100,
    render: (row: DataxJobLog) => h(NTag, {
      type: row.triggerType === 1 ? 'info' : 'success',
      size: 'small'
    }, {
      icon: () => h(NIcon, null, () => h(row.triggerType === 1 ? HandLeftOutline : TimerOutline)),
      default: () => row.triggerType === 1 ? '手动' : '定时'
    })
  },
  { title: '开始时间', key: 'startTime', width: 160 },
  { title: '结束时间', key: 'endTime', width: 160, render: (row: DataxJobLog) => row.endTime || '-' },
  { title: '耗时(秒)', key: 'executeTime', width: 90, render: (row: DataxJobLog) => row.executeTime ?? '-' },
  { title: '读取数', key: 'readCount', width: 90, render: (row: DataxJobLog) => h('span', { style: 'font-weight: 600;' }, row.readCount ?? 0) },
  { title: '写入数', key: 'writeCount', width: 90, render: (row: DataxJobLog) => h('span', { style: 'font-weight: 600;' }, row.writeCount ?? 0) },
  {
    title: '操作', key: 'actions', width: 100,
    render: (row: DataxJobLog) => h(NButton, {
      size: 'small',
      type: 'info',
      secondary: true,
      onClick: () => viewLog(row)
    }, {
      icon: () => h(NIcon, null, () => h(EyeOutline)),
      default: () => '查看'
    })
  }
]

// 搜索处理
const handleSearch = () => {
  pagination.page = 1
  loadData()
}

// 加载统计数据
const loadStatistics = async () => {
  try {
    const params: Record<string, unknown> = {}
    if (jobId.value) params['jobId'] = jobId.value
    const res = await request.get('/datax/job/log-statistics', { params })
    if (res.data) {
      statistics.value = res.data as typeof statistics.value
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params: { page: number; pageSize: number; jobId?: number; status?: number; jobName?: string } = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    if (jobIdParam.value !== undefined) params.jobId = jobIdParam.value
    if (searchStatus.value !== null) params.status = searchStatus.value
    if (searchJobName.value) params.jobName = searchJobName.value
    const res = await getJobLogList(params)
    tableData.value = res.data?.list || []
    pagination.itemCount = res.data?.total || 0
    loadStatistics()
  } catch (error) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchJobName.value = ''
  searchStatus.value = null
  pagination.page = 1
  loadData()
}

const handlePageChange = (page: number) => {
  pagination.page = page
  loadData()
}

const handlePageSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadData()
}

const viewLog = async (row: DataxJobLog) => {
  try {
    const res = await request.get(`/datax/job/log/${row.id}`)
    logDetail.value = res.data
    showLogModal.value = true
  } catch (error) {
    message.error('获取日志详情失败')
  }
}

const formatLogContent = (log: any) => {
  if (!log) return ''
  let content = '========== 执行信息 ==========\n'
  content += `任务名称: ${log.jobName || '-'}\n`
  content += `开始时间: ${log.startTime || '-'}\n`
  content += `结束时间: ${log.endTime || '-'}\n`
  content += `执行耗时: ${log.executeTime || 0} 秒\n`
  content += `读取记录: ${log.readCount || 0}\n`
  content += `写入记录: ${log.writeCount || 0}\n`
  content += `错误记录: ${log.errorCount || 0}\n`
  content += '\n========== 执行日志 ==========\n'
  content += log.executeLog || '无日志'
  if (log.errorMsg) {
    content += '\n\n========== 错误信息 ==========\n'
    content += log.errorMsg
  }
  return content
}

const goBack = () => {
  router.push('/datax/job')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.data-transfer-log-page { padding: 16px; min-height: 100%; }

/* 日志详情 */
.log-summary { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; background: var(--bg-tertiary, #f8f9fa); padding: 16px; border-radius: 8px; margin-bottom: 20px; }
.summary-item { text-align: center; }
.summary-label { color: var(--text-secondary, #6b7280); font-size: 13px; margin: 0 0 5px 0; }
.summary-value { font-weight: 600; margin: 0; color: var(--text-primary, #1e293b); }
.summary-value.read { color: #10b981; }
.summary-value.write { color: #3b82f6; }

.log-content { background: #1e2433; color: #abb2bf; border-radius: 8px; padding: 16px; max-height: 400px; overflow-y: auto; }
.log-content pre { margin: 0; white-space: pre-wrap; word-wrap: break-word; font-family: 'Consolas', 'Monaco', monospace; font-size: 13px; line-height: 1.6; }

</style>

<style>
/* DataTransferLog 深色模式（非 scoped） */
html.dark .log-summary { background: #1a2536 !important; }
</style>

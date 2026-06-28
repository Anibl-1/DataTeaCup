<template>
  <div class="operation-log-page">
    <!-- Page_Header_Stats: 操作日志统计 (Req 1.1, 15.1) -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="22"><ListOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ totalCount }}</span>
          <span class="stat-label">日志总数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="22"><CheckmarkCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ successCount }}</span>
          <span class="stat-label">成功操作</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-error">
          <n-icon size="22"><CloseCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ failedCount }}</span>
          <span class="stat-label">失败操作</span>
        </div>
      </div>
    </div>

    <!-- Main_Card: 操作日志列表 (Req 1.1, 15.1) -->
    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="var(--color-primary)" class="header-icon"><DocumentTextOutline /></n-icon>
          <span>操作日志</span>
        </div>
      </template>
      <template #header-extra>
        <n-space>
          <n-button quaternary circle :loading="loading" @click="handleRefresh">
            <template #icon><n-icon size="18"><RefreshOutline /></n-icon></template>
          </n-button>
          <n-button type="primary" :loading="exporting" @click="handleExport">
            <template #icon><n-icon><DownloadOutline /></n-icon></template>
            导出
          </n-button>
        </n-space>
      </template>

      <!-- Query_Form: 搜索筛选 (Req 15.1) -->
      <n-form class="query-form" inline>
        <n-form-item>
          <n-input v-model:value="queryParams.username" placeholder="用户名" clearable @keyup.enter="handleSearch" />
        </n-form-item>
        <n-form-item>
          <n-select v-model:value="queryParams.moduleName" :options="moduleOptions" placeholder="模块" clearable style="width: 150px;" />
        </n-form-item>
        <n-form-item>
          <n-select v-model:value="queryParams.operationType" :options="operationTypeOptions" placeholder="操作类型" clearable style="width: 150px;" />
        </n-form-item>
        <n-form-item>
          <n-select v-model:value="queryParams.status" :options="statusOptions" placeholder="状态" clearable style="width: 120px;" />
        </n-form-item>
        <n-form-item>
          <n-date-picker v-model:value="dateRange" type="datetimerange" clearable :shortcuts="dateShortcuts" style="width: 340px;" />
        </n-form-item>
        <n-form-item>
          <n-input v-model:value="queryParams.keyword" placeholder="搜索描述或URL" clearable @keyup.enter="handleSearch">
            <template #prefix><n-icon :component="SearchOutline" /></template>
          </n-input>
        </n-form-item>
        <n-form-item class="query-form-actions">
          <n-button type="primary" @click="handleSearch">搜索</n-button>
          <n-button @click="handleReset">重置</n-button>
        </n-form-item>
      </n-form>

      <!-- 数据表格 -->
      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="false"
        :row-key="(row: OperationLog) => row.id"
        :scroll-x="1400"
        remote
        striped
        class="custom-table"
        size="small"
      />

      <!-- 空状态 (Req 16.7) -->
      <n-empty v-if="!loading && tableData.length === 0" description="暂无操作日志数据" style="margin: 32px 0;" />

      <!-- Pagination_Wrapper -->
      <div class="pagination-wrapper">
        <div class="pagination-info">
          <n-tag type="info" size="small" round>共 {{ totalCount }} 条记录</n-tag>
        </div>
        <n-pagination
          v-model:page="currentPage"
          v-model:page-size="currentPageSize"
          :item-count="totalCount"
          :page-sizes="PAGE_SIZES"
          show-size-picker
          show-quick-jumper
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-card>

    <!-- 日志详情抽屉 -->
    <n-drawer v-model:show="showDetail" :width="600" placement="right">
      <n-drawer-content title="日志详情" closable>
        <n-descriptions v-if="currentLog" label-placement="left" :column="1" bordered>
          <n-descriptions-item label="用户">{{ currentLog.username }}</n-descriptions-item>
          <n-descriptions-item label="模块">{{ currentLog.moduleName }}</n-descriptions-item>
          <n-descriptions-item label="操作类型">
            <n-tag :type="getOperationTypeColor(currentLog.operationType)" size="small">
              {{ operationTypeLabelMap[currentLog.operationType] || currentLog.operationType }}
            </n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="操作描述">{{ currentLog.operationDesc }}</n-descriptions-item>
          <n-descriptions-item label="操作时间">{{ formatDateTime(currentLog.createTime) }}</n-descriptions-item>
          <n-descriptions-item label="IP地址">{{ currentLog.ipAddress }}</n-descriptions-item>
          <n-descriptions-item label="执行时间">
            <n-tag :type="getDurationColor(currentLog.durationMs, currentLog.operationType)" size="small">{{ currentLog.durationMs }}ms</n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="状态">
            <n-tag :type="currentLog.status === 'success' ? 'success' : 'error'" size="small">{{ currentLog.status === 'success' ? '成功' : '失败' }}</n-tag>
          </n-descriptions-item>
        </n-descriptions>

        <n-divider />

        <n-h3>请求信息</n-h3>
        <n-descriptions label-placement="left" :column="1" bordered>
          <n-descriptions-item label="请求方法"><n-tag>{{ currentLog?.requestMethod }}</n-tag></n-descriptions-item>
          <n-descriptions-item label="请求URL"><n-text code>{{ currentLog?.requestUrl }}</n-text></n-descriptions-item>
        </n-descriptions>

        <n-h4 style="margin-top: 16px">请求参数</n-h4>
        <div v-if="currentLog?.requestParams" class="json-block">
          <n-code :code="formatJson(currentLog.requestParams)" language="json" :word-wrap="true" />
        </div>
        <n-empty v-else description="无请求参数" size="small" />

        <template v-if="currentLog?.status === 'success' && currentLog?.responseResult">
          <n-h4 style="margin-top: 16px">响应结果</n-h4>
          <div class="json-block">
            <n-code :code="formatJson(currentLog.responseResult)" language="json" :word-wrap="true" />
          </div>
        </template>

        <template v-if="currentLog?.status === 'failed' && currentLog?.errorMessage">
          <n-h4 style="margin-top: 16px">错误信息</n-h4>
          <n-alert type="error" :title="currentLog.errorMessage" />
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, h, onMounted, computed } from 'vue'
import { NButton, NTag, NIcon, useMessage, type DataTableColumns } from 'naive-ui'
import {
  ListOutline, CheckmarkCircleOutline, CloseCircleOutline,
  DocumentTextOutline, RefreshOutline, DownloadOutline,
  SearchOutline, EyeOutline
} from '@vicons/ionicons5'
import { getOperationLogList } from '@/api/system/operationLog'
import type { OperationLog, OperationLogQuery } from '@/types/operationLog'
import { formatDateTime } from '@/utils/format'
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE, PAGE_SIZES } from '@/constants'
import { initMessage } from '@/utils/message'
import { useExport } from '@/composables/useExport'

const message = useMessage()
const { exportCsv: exportCsvFn, exporting } = useExport({ defaultFilename: '操作日志' })
initMessage(message)

const loading = ref(false)
const showDetail = ref(false)
const currentLog = ref<OperationLog | null>(null)
const tableData = ref<OperationLog[]>([])
const dateRange = ref<[number, number] | null>(null)
const totalCount = ref(0)
const currentPage = ref(DEFAULT_PAGE)
const currentPageSize = ref(DEFAULT_PAGE_SIZE)

// Computed stats from table data
const successCount = computed(() => tableData.value.filter(r => r.status === 'success').length)
const failedCount = computed(() => tableData.value.filter(r => r.status === 'failed').length)

// 查询参数
const queryParams = ref<OperationLogQuery>({
  username: '',
  moduleName: '',
  operationType: '',
  status: '',
  keyword: ''
})

// 操作类型中文映射
const operationTypeLabelMap: Record<string, string> = {
  CREATE: '新增', INSERT: '新增', UPDATE: '修改', DELETE: '删除',
  QUERY: '查询', SELECT: '查询', EXPORT: '导出', IMPORT: '导入',
  LOGIN: '登录', LOGOUT: '登出', cleanup: '清理'
}

const moduleOptions = [
  { label: '认证管理', value: '认证管理' },
  { label: '用户管理', value: '用户管理' },
  { label: '角色管理', value: '角色管理' },
  { label: '菜单管理', value: '菜单管理' },
  { label: '数据源管理', value: '数据源管理' },
  { label: '数据采集', value: '数据采集' },
  { label: '报表管理', value: '报表管理' },
  { label: '图表管理', value: '图表管理' },
  { label: '页面管理', value: '页面管理' },
  { label: '公告管理', value: '公告管理' },
  { label: '导出中心', value: '导出中心' },
  { label: '数据传输', value: '数据传输' },
  { label: '流程管理', value: '流程管理' },
  { label: '数据血缘', value: '数据血缘' },
  { label: '数据管理', value: '数据管理' }
]

const operationTypeOptions = [
  { label: '新增', value: 'CREATE' },
  { label: '修改', value: 'UPDATE' },
  { label: '删除', value: 'DELETE' },
  { label: '查询', value: 'QUERY' },
  { label: '导出', value: 'EXPORT' },
  { label: '导入', value: 'IMPORT' },
  { label: '登录', value: 'LOGIN' },
  { label: '登出', value: 'LOGOUT' },
  { label: '清理', value: 'cleanup' }
]

const statusOptions = [
  { label: '成功', value: 'success' },
  { label: '失败', value: 'failed' }
]

const dateShortcuts = {
  '今天': () => {
    const now = new Date()
    const start = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    return [start.getTime(), now.getTime()] as [number, number]
  },
  '最近7天': () => {
    const now = new Date()
    const start = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
    return [start.getTime(), now.getTime()] as [number, number]
  },
  '最近30天': () => {
    const now = new Date()
    const start = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000)
    return [start.getTime(), now.getTime()] as [number, number]
  }
}

const columns: DataTableColumns<OperationLog> = [
  { title: '操作时间', key: 'createTime', width: 180, render: (row) => formatDateTime(row.createTime) },
  { title: '用户', key: 'username', width: 120 },
  { title: '模块', key: 'moduleName', width: 120 },
  {
    title: '操作类型', key: 'operationType', width: 100,
    render: (row) => h(NTag, { type: getOperationTypeColor(row.operationType), size: 'small' },
      { default: () => operationTypeLabelMap[row.operationType] || row.operationType })
  },
  { title: '操作描述', key: 'operationDesc', width: 200, ellipsis: { tooltip: true } },
  { title: 'IP地址', key: 'ipAddress', width: 130 },
  {
    title: '耗时', key: 'durationMs', width: 90,
    render: (row) => h(NTag, { type: getDurationColor(row.durationMs, row.operationType), size: 'small' },
      { default: () => `${row.durationMs}ms` })
  },
  {
    title: '状态', key: 'status', width: 80,
    render: (row) => h(NTag, { type: row.status === 'success' ? 'success' : 'error', size: 'small' },
      { default: () => row.status === 'success' ? '成功' : '失败' })
  },
  {
    title: '操作', key: 'actions', width: 120, fixed: 'right',
    render: (row) => h(NButton, { size: 'small', quaternary: true, onClick: () => handleViewDetail(row) },
      { default: () => '查看详情', icon: () => h(NIcon, null, { default: () => h(EyeOutline) }) })
  }
]

const loadData = async () => {
  loading.value = true
  try {
    const params: OperationLogQuery = {
      ...queryParams.value,
      page: currentPage.value,
      pageSize: currentPageSize.value
    }
    if (dateRange.value) {
      params.startTime = formatDateTime(new Date(dateRange.value[0]))
      params.endTime = formatDateTime(new Date(dateRange.value[1]))
    }
    const res = await getOperationLogList(params)
    tableData.value = res.data?.list || []
    totalCount.value = res.data?.total || 0
  } catch (error) {
    message.error('加载数据失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { currentPage.value = 1; loadData() }
const handleReset = () => {
  queryParams.value = { username: '', moduleName: '', operationType: '', status: '', keyword: '' }
  dateRange.value = null
  currentPage.value = 1
  loadData()
}
const handleRefresh = () => { loadData() }
const handlePageChange = (page: number) => { currentPage.value = page; loadData() }
const handlePageSizeChange = (size: number) => { currentPageSize.value = size; currentPage.value = 1; loadData() }

const handleExport = async () => {
  const exportData = tableData.value.map((log: OperationLog) => ({
    '操作时间': log.createTime || '',
    '操作人': log.username || '',
    '模块': log.moduleName || '',
    '操作类型': log.operationType || '',
    '操作描述': log.operationDesc || '',
    'IP地址': log.ipAddress || '',
    '耗时(ms)': log.durationMs?.toString() || '',
    '状态': log.status === 'success' ? '成功' : '失败'
  }))
  await exportCsvFn(exportData, `操作日志_${new Date().toISOString().slice(0, 10)}`)
}

const handleViewDetail = (row: OperationLog) => {
  currentLog.value = row
  showDetail.value = true
}

const getOperationTypeColor = (type: string) => {
  const colorMap: Record<string, any> = {
    CREATE: 'info', INSERT: 'info', UPDATE: 'warning', DELETE: 'error',
    QUERY: 'default', SELECT: 'default', EXPORT: 'warning', IMPORT: 'info',
    LOGIN: 'success', LOGOUT: 'default'
  }
  return colorMap[type] || 'default'
}

const getDurationColor = (duration: number, operationType?: string) => {
  if (operationType === 'EXPORT' || operationType === 'IMPORT' || operationType === 'QUERY') return 'default'
  if (duration < 200) return 'success'
  if (duration < 1000) return 'warning'
  return 'error'
}

const formatJson = (str: string) => {
  try { return JSON.stringify(JSON.parse(str), null, 2) } catch { return str }
}

onMounted(() => { loadData() })
</script>

<style scoped>
.operation-log-page {
  animation: fadeIn 0.3s ease-out;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
.json-block {
  border-radius: 8px;
  overflow: hidden;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 13px;
  line-height: 1.6;
}
</style>

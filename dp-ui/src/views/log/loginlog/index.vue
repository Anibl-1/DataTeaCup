<template>
  <div class="login-log-page">
    <!-- Page_Header_Stats: 登录日志统计 (Req 1.1, 15.2) -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="22"><ListOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ totalCount }}</span>
          <span class="stat-label">总记录数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="22"><CheckmarkCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ successCount }}</span>
          <span class="stat-label">登录成功</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-error">
          <n-icon size="22"><CloseCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ failedCount }}</span>
          <span class="stat-label">登录失败</span>
        </div>
      </div>
    </div>

    <!-- Main_Card: 登录日志列表 (Req 1.1, 15.2) -->
    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="var(--color-primary)" class="header-icon"><LogInOutline /></n-icon>
          <span>登录日志</span>
        </div>
      </template>
      <template #header-extra>
        <n-space>
          <n-button quaternary circle :loading="loading" @click="loadData">
            <template #icon><n-icon size="18"><RefreshOutline /></n-icon></template>
          </n-button>
          <n-popconfirm @positive-click="handleClean">
            <template #trigger>
              <n-button type="warning">
                <template #icon><n-icon><TrashOutline /></n-icon></template>
                清理日志
              </n-button>
            </template>
            确定清理90天前的登录日志吗？
          </n-popconfirm>
        </n-space>
      </template>

      <!-- Query_Form: 搜索筛选 (Req 15.2) -->
      <n-form class="query-form" inline>
        <n-form-item>
          <n-input v-model:value="queryParams.username" placeholder="用户名" clearable @keyup.enter="handleSearch" />
        </n-form-item>
        <n-form-item>
          <n-select v-model:value="queryParams.status" :options="statusOptions" placeholder="状态" clearable style="width: 120px;" />
        </n-form-item>
        <n-form-item>
          <n-input v-model:value="queryParams.ipAddress" placeholder="IP地址" clearable @keyup.enter="handleSearch" />
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
        :row-key="(row: any) => row.id"
        :scroll-x="1200"
        striped
        class="custom-table"
      />

      <!-- 空状态 (Req 16.7) -->
      <n-empty v-if="!loading && tableData.length === 0" description="暂无登录日志数据" style="margin: 32px 0;" />

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
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, onMounted, h, computed } from 'vue'
import { NTag, useMessage } from 'naive-ui'
import {
  ListOutline, CheckmarkCircleOutline, CloseCircleOutline,
  LogInOutline, RefreshOutline, TrashOutline
} from '@vicons/ionicons5'
import { getLoginLogList, cleanLoginLog } from '@/api/system/loginLog'
import type { LoginLog } from '@/api/system/loginLog'
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE, PAGE_SIZES } from '@/constants'
import { initMessage } from '@/utils/message'

const message = useMessage()
initMessage(message)

const loading = ref(false)
const tableData = ref<LoginLog[]>([])
const totalCount = ref(0)
const currentPage = ref(DEFAULT_PAGE)
const currentPageSize = ref(DEFAULT_PAGE_SIZE)

const successCount = computed(() => tableData.value.filter(r => r.status === 'success').length)
const failedCount = computed(() => tableData.value.filter(r => r.status !== 'success').length)

const queryParams = reactive({
  username: '',
  status: null as string | null,
  ipAddress: ''
})

const statusOptions = [
  { label: '成功', value: 'success' },
  { label: '失败', value: 'failure' }
]

const columns = [
  { title: 'ID', key: 'id', width: 70 },
  { title: '用户名', key: 'username', width: 120 },
  { title: 'IP地址', key: 'ipAddress', width: 140 },
  { title: '浏览器', key: 'browser', width: 100 },
  { title: '操作系统', key: 'os', width: 100 },
  {
    title: '状态', key: 'status', width: 90,
    render: (row: LoginLog) => h(NTag, { type: row.status === 'success' ? 'success' : 'error', size: 'small' },
      { default: () => row.status === 'success' ? '成功' : '失败' })
  },
  { title: '消息', key: 'message', ellipsis: { tooltip: true } },
  { title: '登录时间', key: 'loginTime', width: 180 }
]

const loadData = async () => {
  loading.value = true
  try {
    const res = await getLoginLogList({
      page: currentPage.value,
      pageSize: currentPageSize.value,
      username: queryParams.username || undefined,
      status: queryParams.status || undefined,
      ipAddress: queryParams.ipAddress || undefined
    })
    const data = res.data?.data || res.data
    tableData.value = data.list || data.records || []
    totalCount.value = data.total || 0
  } catch {
    message.error('加载登录日志失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { currentPage.value = 1; loadData() }
const handleReset = () => {
  queryParams.username = ''
  queryParams.status = null
  queryParams.ipAddress = ''
  currentPage.value = 1
  loadData()
}
const handlePageChange = (page: number) => { currentPage.value = page; loadData() }
const handlePageSizeChange = (size: number) => { currentPageSize.value = size; currentPage.value = 1; loadData() }

const handleClean = async () => {
  try {
    const res = await cleanLoginLog(90)
    const count = res.data?.data || 0
    message.success(`已清理 ${count} 条历史日志`)
    loadData()
  } catch {
    message.error('清理失败')
  }
}

onMounted(() => { loadData() })
</script>

<style scoped>
.login-log-page {
  animation: fadeIn 0.3s ease-out;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>

<template>
  <div class="health-check-container">
    <!-- Page_Header_Stats: 健康检查概览 (Req 8.8) -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><PulseOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ healthyCount }}</span>
          <span class="stat-label">{{ t('healthCheck.healthyComponents') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="24"><WarningOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ degradedCount }}</span>
          <span class="stat-label">{{ t('healthCheck.degradedComponents') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-error">
          <n-icon size="24"><CloseCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ unhealthyCount }}</span>
          <span class="stat-label">{{ t('healthCheck.unhealthyComponents') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><ServerOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ statusList.length }}</span>
          <span class="stat-label">{{ t('healthCheck.totalChecks') }}</span>
        </div>
      </div>
    </div>

    <!-- Main_Card: 组件健康状态 (Req 1.1) -->
    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="var(--color-primary)" class="header-icon"><PulseOutline /></n-icon>
          <span>{{ t('healthCheck.systemHealthCheck') }}</span>
        </div>
      </template>
      <template #header-extra>
        <n-space>
          <n-button type="primary" :loading="checkRunning" @click="runCheck">
            <template #icon><n-icon><PulseOutline /></n-icon></template>
            {{ t('healthCheck.manualCheck') }}
          </n-button>
          <n-button :loading="statusLoading" @click="loadStatus">
            <template #icon><n-icon><RefreshOutline /></n-icon></template>
            {{ t('healthCheck.refreshStatus') }}
          </n-button>
        </n-space>
      </template>

      <!-- 组件健康状态卡片 -->
      <div v-if="statusList.length > 0" class="status-cards">
        <n-card
          v-for="item in statusList"
          :key="item.checkType"
          size="small"
          :class="['status-card', `status-${item.status}`]"
        >
          <template #header>
            <div class="status-card-header">
              <n-icon size="22" :color="statusColor(item.status)">
                <component :is="statusIcon(item.checkType)" />
              </n-icon>
              <span>{{ item.checkName || item.checkType }}</span>
            </div>
          </template>
          <div class="card-body">
            <div class="status-badge">
              <n-tag :type="statusTagType(item.status)" size="small" round>
                {{ statusLabel(item.status) }}
              </n-tag>
            </div>
            <div class="card-metrics">
              <div class="metric">
                <span class="metric-label">{{ t('healthCheck.responseTime') }}</span>
                <span class="metric-value">{{ item.responseTime != null ? item.responseTime + 'ms' : '-' }}</span>
              </div>
              <div class="metric">
                <span class="metric-label">{{ t('healthCheck.checkTime') }}</span>
                <span class="metric-value">{{ formatTime(item.checkTime) }}</span>
              </div>
            </div>
            <div v-if="item.errorMessage" class="error-msg">
              <n-tag type="error" size="small">{{ item.errorMessage }}</n-tag>
            </div>
            <div v-if="item.details" class="details-section">
              <n-collapse>
                <n-collapse-item :title="t('healthCheck.detailInfo')" name="details">
                  <n-descriptions :column="1" size="small" bordered>
                    <n-descriptions-item
                      v-for="(val, key) in parseDetails(item.details)"
                      :key="key"
                      :label="String(key)"
                    >
                      {{ val }}
                    </n-descriptions-item>
                  </n-descriptions>
                </n-collapse-item>
              </n-collapse>
            </div>
          </div>
        </n-card>
      </div>

      <!-- 无数据 -->
      <n-empty v-if="!statusLoading && statusList.length === 0" :description="t('healthCheck.noData')" style="margin: 40px 0" />
    </n-card>

    <!-- 检查历史 -->
    <n-card class="main-card" style="margin-top: 16px;">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="var(--color-primary)" class="header-icon"><TimeOutline /></n-icon>
          <span>{{ t('healthCheck.checkHistory') }}</span>
        </div>
      </template>
      <template #header-extra>
        <n-select
          v-model:value="historyHours"
          :options="hoursOptions"
          style="width: 140px"
          size="small"
          @update:value="loadHistory"
        />
      </template>
      <n-data-table
        :columns="historyColumns"
        :data="historyList"
        :loading="historyLoading"
        :bordered="false"
        :scroll-x="900"
        striped
        size="small"
        :max-height="400"
        class="custom-table"
      />
    </n-card>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, h, onMounted } from 'vue'
import { NTag, NIcon, useMessage } from 'naive-ui'
import {
  RefreshOutline,
  PulseOutline,
  ServerOutline,
  DesktopOutline,
  CloudOutline,
  WarningOutline,
  CloseCircleOutline,
  TimeOutline
} from '@vicons/ionicons5'
import { systemMonitorApi } from '@/api/systemMonitor'
import type { DataTableColumns } from 'naive-ui'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const message = useMessage()

// ==================== 健康状态 ====================
const statusList = ref<any[]>([])
const statusLoading = ref(false)
const checkRunning = ref(false)

const healthyCount = computed(() => statusList.value.filter(s => s.status === 'healthy').length)
const degradedCount = computed(() => statusList.value.filter(s => s.status === 'degraded').length)
const unhealthyCount = computed(() => statusList.value.filter(s => s.status === 'unhealthy').length)

const loadStatus = async () => {
  statusLoading.value = true
  try {
    const res = await systemMonitorApi.getHealthStatus() as any
    statusList.value = res.data || []
  } catch { /* handled */ } finally {
    statusLoading.value = false
  }
}

const runCheck = async () => {
  checkRunning.value = true
  try {
    const res = await systemMonitorApi.runHealthCheck() as any
    statusList.value = res.data || []
    message.success(t('healthCheck.checkComplete'))
    loadHistory()
  } catch { /* handled */ } finally {
    checkRunning.value = false
  }
}

// ==================== 检查历史 ====================
const historyList = ref<any[]>([])
const historyLoading = ref(false)
const historyHours = ref(24)

const hoursOptions = computed(() => [
  { label: t('healthCheck.lastHour'), value: 1 },
  { label: t('healthCheck.last6Hours'), value: 6 },
  { label: t('healthCheck.last24Hours'), value: 24 },
  { label: t('healthCheck.last7Days'), value: 168 }
])

const historyColumns = computed<DataTableColumns<any>>(() => [
  {
    title: t('healthCheck.checkTime'), key: 'checkTime', width: 170,
    render: (row) => formatTime(row.checkTime)
  },
  { title: t('healthCheck.type'), key: 'checkType', width: 100 },
  { title: t('healthCheck.name'), key: 'checkName', width: 120 },
  {
    title: t('healthCheck.status'), key: 'status', width: 90,
    render: (row) => h(NTag, {
      type: statusTagType(row.status),
      size: 'small',
      round: true
    }, { default: () => statusLabel(row.status) })
  },
  {
    title: t('healthCheck.responseTime'), key: 'responseTime', width: 100,
    render: (row) => row.responseTime != null ? row.responseTime + 'ms' : '-'
  },
  {
    title: t('healthCheck.errorMessage'), key: 'errorMessage', ellipsis: { tooltip: true },
    render: (row) => row.errorMessage || '-'
  }
])

const loadHistory = async () => {
  historyLoading.value = true
  try {
    const res = await systemMonitorApi.getHealthHistory(historyHours.value, 100) as any
    historyList.value = res.data || []
  } catch { /* handled */ } finally {
    historyLoading.value = false
  }
}

// ==================== 工具函数 ====================
const statusColor = (status: string): string => {
  switch (status) {
    case 'healthy': return '#52c41a'
    case 'degraded': return '#faad14'
    case 'unhealthy': return '#f5222d'
    default: return '#999'
  }
}

const statusTagType = (status: string): 'success' | 'warning' | 'error' | 'info' => {
  switch (status) {
    case 'healthy': return 'success'
    case 'degraded': return 'warning'
    case 'unhealthy': return 'error'
    default: return 'info'
  }
}

const statusLabel = (status: string): string => {
  switch (status) {
    case 'healthy': return t('healthCheck.healthy')
    case 'degraded': return t('healthCheck.degraded')
    case 'unhealthy': return t('healthCheck.unhealthy')
    default: return status || t('healthCheck.unknown')
  }
}

const statusIcon = (type: string) => {
  switch (type) {
    case 'database': return ServerOutline
    case 'jvm': return DesktopOutline
    case 'disk': return CloudOutline
    default: return PulseOutline
  }
}

const parseDetails = (details: string): Record<string, any> => {
  if (!details) return {}
  try {
    return JSON.parse(details)
  } catch {
    return { raw: details }
  }
}

const formatTime = (timeStr: string): string => {
  if (!timeStr) return '-'
  try {
    const locale = t('common.locale') || undefined
    return new Date(timeStr).toLocaleString(locale, { hour12: false })
  } catch { return timeStr }
}

// ==================== 初始化 ====================
onMounted(() => {
  loadStatus()
  loadHistory()
})
</script>

<style scoped>
.health-check-container {
  padding: 16px;
}
.status-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
.status-card {
  border-left: 4px solid #d9d9d9;
  transition: border-color 0.3s;
}
.status-card.status-healthy {
  border-left-color: #52c41a;
}
.status-card.status-degraded {
  border-left-color: #faad14;
}
.status-card.status-unhealthy {
  border-left-color: #f5222d;
}
.status-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
.card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.status-badge {
  margin-bottom: 4px;
}
.card-metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.metric {
  display: flex;
  flex-direction: column;
}
.metric-label {
  font-size: 12px;
  color: #999;
}
.metric-value {
  font-size: 14px;
  font-weight: 500;
}
.error-msg {
  margin-top: 4px;
}
.details-section {
  margin-top: 4px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .health-check-page { padding: 10px; }
  .page-header-stats {
    flex-direction: row !important;
    overflow-x: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;
    gap: 8px;
  }
  .page-header-stats::-webkit-scrollbar { display: none; }
  .stat-item { min-width: 110px; flex-shrink: 0; }
  .status-grid { grid-template-columns: 1fr !important; gap: 10px; }
  .status-card { border-radius: 12px; }
  .card-metrics { grid-template-columns: 1fr; }
  .main-card { border-radius: 12px !important; }
}

</style>

<style>
/* HealthCheckView 深色模式（非 scoped） */
html.dark .health-card { background: #1a2536 !important; border-color: #334155 !important; }
html.dark .health-title { color: #e2e8f0 !important; }
html.dark .health-desc { color: #94a3b8 !important; }
</style>

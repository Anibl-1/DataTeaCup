<template>
  <div class="system-monitor">
    <n-page-header :title="t('monitor.title')" :subtitle="t('monitor.subtitle')">
      <template #extra>
        <n-space>
          <n-tag v-if="wsConnected" type="success" size="small" round>
            <template #icon><n-icon size="12"><PulseOutline /></n-icon></template>
            {{ t('monitor.realTimeConnected') }}
          </n-tag>
          <n-tag v-else type="default" size="small" round>{{ t('monitor.offline') }}</n-tag>
          <n-text v-if="lastRefreshTime" depth="3">
            {{ t('monitor.lastRefresh') }}: {{ formatTime(lastRefreshTime) }}
          </n-text>
          <n-switch v-model:value="autoRefresh" size="small">
            <template #checked>{{ t('monitor.autoRefresh') }}</template>
            <template #unchecked>{{ t('monitor.manualRefresh') }}</template>
          </n-switch>
          <n-button type="primary" :loading="loading" @click="refreshAll">
            <template #icon>
              <n-icon><RefreshOutline /></n-icon>
            </template>
            {{ t('monitor.refresh') }}
          </n-button>
          <n-button @click="showMaintenanceModal = true">
            <template #icon>
              <n-icon><SettingsOutline /></n-icon>
            </template>
            {{ t('monitor.maintenance') }}
          </n-button>
        </n-space>
      </template>
    </n-page-header>

    <!-- 服务器资源监控 -->
    <n-card :title="t('monitor.serverResources')" size="small" style="margin-bottom: 16px">
      <n-grid cols="3" x-gap="16" responsive="screen" item-responsive>
        <n-gi span="3 m:1">
          <div class="resource-item">
            <div class="resource-header">
              <n-icon size="24" color="#18a058"><HardwareChipOutline /></n-icon>
              <span>{{ t('monitor.cpuUsage') }}</span>
            </div>
            <n-progress
              type="circle"
              :percentage="serverStats.cpuUsage"
              :color="getUsageColor(serverStats.cpuUsage, 'cpu')"
              :stroke-width="8"
            />
            <n-text depth="3">{{ serverStats.cpuUsage }}%</n-text>
          </div>
        </n-gi>
        <n-gi span="3 m:1">
          <div class="resource-item">
            <div class="resource-header">
              <n-icon size="24" color="#2080f0"><ServerOutline /></n-icon>
              <span>{{ t('monitor.memoryUsage') }}</span>
            </div>
            <n-progress
              type="circle"
              :percentage="serverStats.memoryUsage"
              :color="getUsageColor(serverStats.memoryUsage, 'memory')"
              :stroke-width="8"
            />
            <n-text depth="3">{{ serverStats.usedMemory }} / {{ serverStats.totalMemory }} GB</n-text>
          </div>
        </n-gi>
        <n-gi span="3 m:1">
          <div class="resource-item">
            <div class="resource-header">
              <n-icon size="24" color="#f0a020"><FolderOutline /></n-icon>
              <span>{{ t('monitor.diskUsage') }}</span>
            </div>
            <n-progress
              type="circle"
              :percentage="serverStats.diskUsage"
              :color="getUsageColor(serverStats.diskUsage, 'disk')"
              :stroke-width="8"
            />
            <n-text depth="3">{{ serverStats.usedDisk }} / {{ serverStats.totalDisk }} GB</n-text>
          </div>
        </n-gi>
      </n-grid>
    </n-card>

    <!-- 系统概览卡片 -->
    <n-grid cols="5" x-gap="12" y-gap="12" style="margin-bottom: 16px" responsive="screen" item-responsive>
      <n-gi span="5 m:1">
        <n-card :segmented="{ content: true }" size="small" hoverable>
          <n-statistic :label="t('monitor.tableCount')" :value="health.totalTables">
            <template #prefix>
              <n-icon size="20" color="#2080f0"><GridOutline /></n-icon>
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
      <n-gi span="5 m:1">
        <n-card :segmented="{ content: true }" size="small" hoverable>
          <n-statistic :label="t('monitor.runningTasks')" :value="health.runningTasks">
            <template #prefix>
              <n-icon size="20" color="#18a058"><PlayCircleOutline /></n-icon>
            </template>
            <template #suffix>
              <n-tag v-if="health.runningTasks > 0" type="success" size="small">{{ t('monitor.active') }}</n-tag>
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
      <n-gi span="5 m:1">
        <n-card :segmented="{ content: true }" size="small" hoverable>
          <n-statistic :label="t('monitor.activeConnections')" :value="health.activeConnections">
            <template #prefix>
              <n-icon size="20" color="#722ed1"><PulseOutline /></n-icon>
            </template>
            <template #suffix>
              <n-tag v-if="health.activeConnections > 10" type="warning" size="small">{{ t('monitor.busy') }}</n-tag>
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
      <n-gi span="5 m:1">
        <n-card :segmented="{ content: true }" size="small" hoverable>
          <n-statistic :label="t('monitor.todayOperations')" :value="health.todayOperations">
            <template #prefix>
              <n-icon size="20" color="#f0a020"><StatsChartOutline /></n-icon>
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
      <n-gi span="5 m:1">
        <n-card :segmented="{ content: true }" size="small" hoverable style="cursor: pointer" @click="showNotifications = true">
          <n-statistic :label="t('monitor.unreadNotifications')" :value="health.unreadNotifications">
            <template #prefix>
              <n-icon size="20" color="#d03050"><NotificationsOutline /></n-icon>
            </template>
            <template #suffix>
              <n-tag v-if="health.unreadNotifications > 0" type="warning" size="small">{{ t('monitor.pending') }}</n-tag>
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 标签页内容 -->
    <n-card :segmented="{ content: true }">
      <n-tabs v-model:value="activeTab" type="line" animated>
        <!-- 活跃任务 -->
        <n-tab-pane name="tasks" :tab="t('monitor.activeTasks')">
          <template #tab>
            <n-space align="center">
              <span>{{ t('monitor.activeTasks') }}</span>
              <n-badge v-if="activeTasks.length > 0" :value="activeTasks.length" :max="99" />
            </n-space>
          </template>
          <n-spin :show="loading">
            <n-data-table
              :columns="activeTaskColumns"
              :data="activeTasks"
              :pagination="{ pageSize: 10 }"
              :max-height="400"
              :scroll-x="1000"
              striped
            />
            <n-empty v-if="!loading && activeTasks.length === 0" :description="t('monitor.noActiveTasks')" size="large" style="padding: 60px 0">
              <template #extra>
                <n-text depth="3">{{ t('monitor.allTasksDone') }}</n-text>
              </template>
            </n-empty>
          </n-spin>
        </n-tab-pane>

        <!-- 数据源统计 -->
        <n-tab-pane name="datasource" :tab="t('monitor.dataSourceStats')">
          <n-spin :show="loading">
            <n-data-table
              :columns="dataSourceColumns"
              :data="dataSourceUsage"
              :pagination="{ pageSize: 10 }"
              :max-height="400"
              :scroll-x="900"
              striped
            />
            <n-empty v-if="!loading && dataSourceUsage.length === 0" :description="t('monitor.noDataSource')" size="large" style="padding: 60px 0" />
          </n-spin>
        </n-tab-pane>

        <!-- 操作日志 -->
        <n-tab-pane name="logs" :tab="t('monitor.operationLogs')">
          <template #tab>
            <n-space align="center">
              <span>{{ t('monitor.operationLogs') }}</span>
              <n-text depth="3" style="font-size: 12px">{{ t('monitor.last7Days') }}</n-text>
            </n-space>
          </template>
          <n-spin :show="loading">
            <n-data-table
              :columns="logColumns"
              :data="operationLogs"
              :pagination="{ pageSize: 10 }"
              :max-height="400"
              :scroll-x="1200"
              striped
            />
            <n-empty v-if="!loading && operationLogs.length === 0" :description="t('monitor.noOperationLogs')" size="large" style="padding: 60px 0" />
          </n-spin>
        </n-tab-pane>

        <!-- 数据库详情 -->
        <n-tab-pane name="database" :tab="t('monitor.dbDetails')">
          <n-grid cols="2" x-gap="16" y-gap="16" responsive="screen" item-responsive>
            <n-gi span="2 m:1">
              <n-card :title="t('monitor.storageDistribution')" size="small">
                <n-space vertical size="large">
                  <div>
                    <n-space justify="space-between">
                      <n-text depth="3">{{ t('monitor.dataSpace') }}</n-text>
                      <n-text depth="3">{{ formatSize(dbStats.dataSize) }}</n-text>
                    </n-space>
                    <n-progress
                      type="line"
                      :percentage="getPercent(dbStats.dataSize, dbStats.totalSize)"
                      color="#18a058"
                      style="margin-top: 4px"
                    />
                  </div>
                  <div>
                    <n-space justify="space-between">
                      <n-text depth="3">{{ t('monitor.indexSpace') }}</n-text>
                      <n-text depth="3">{{ formatSize(dbStats.indexSize) }}</n-text>
                    </n-space>
                    <n-progress
                      type="line"
                      :percentage="getPercent(dbStats.indexSize, dbStats.totalSize)"
                      color="#2080f0"
                      style="margin-top: 4px"
                    />
                  </div>
                  <div>
                    <n-space justify="space-between">
                      <n-text depth="3">{{ t('monitor.freeSpace') }}</n-text>
                      <n-text depth="3">{{ formatSize(dbStats.freeSize) }}</n-text>
                    </n-space>
                    <n-progress
                      type="line"
                      :percentage="getPercent(dbStats.freeSize, dbStats.totalSize)"
                      color="#f0a020"
                      style="margin-top: 4px"
                    />
                  </div>
                </n-space>
              </n-card>
            </n-gi>
            <n-gi span="2 m:1">
              <n-card :title="t('monitor.serverInfo')" size="small">
                <n-descriptions :column="1" label-placement="left" bordered size="small">
                  <n-descriptions-item :label="t('monitor.cpuUsage')">
                    <n-text :type="serverStats.cpuUsage > 80 ? 'error' : serverStats.cpuUsage > 60 ? 'warning' : 'success'">
                      {{ serverStats.cpuUsage }}%
                    </n-text>
                  </n-descriptions-item>
                  <n-descriptions-item :label="t('monitor.memory')">
                    <n-text :type="serverStats.memoryUsage > 80 ? 'error' : serverStats.memoryUsage > 60 ? 'warning' : 'success'">
                      {{ serverStats.usedMemory }} / {{ serverStats.totalMemory }} GB ({{ serverStats.memoryUsage }}%)
                    </n-text>
                  </n-descriptions-item>
                  <n-descriptions-item :label="t('monitor.disk')">
                    <n-text :type="serverStats.diskUsage > 80 ? 'error' : serverStats.diskUsage > 60 ? 'warning' : 'success'">
                      {{ serverStats.usedDisk }} / {{ serverStats.totalDisk }} GB ({{ serverStats.diskUsage }}%)
                    </n-text>
                  </n-descriptions-item>
                  <n-descriptions-item :label="t('monitor.dbSize')">
                    {{ formatSize(dbStats.totalSize) }}
                  </n-descriptions-item>
                  <n-descriptions-item :label="t('monitor.tableCountLabel')">
                    {{ t('monitor.tableCountUnit', { count: health.totalTables }) }}
                  </n-descriptions-item>
                  <n-descriptions-item :label="t('monitor.activeConn')">
                    {{ health.activeConnections }}
                  </n-descriptions-item>
                </n-descriptions>
              </n-card>
            </n-gi>
          </n-grid>
        </n-tab-pane>

        <!-- JVM监控 -->
        <n-tab-pane name="jvm" :tab="t('monitor.jvmMonitor')">
          <n-spin :show="loadingJvm">
            <n-grid cols="4" x-gap="12" y-gap="12" responsive="screen" item-responsive>
              <n-gi span="4 m:1">
                <n-card :segmented="{ content: true }" size="small" hoverable>
                  <n-statistic :label="t('monitor.heapUsed')" :value="jvmMetrics.heapUsed + ' MB'">
                    <template #suffix>
                      <n-text depth="3">/ {{ jvmMetrics.heapMax }} MB</n-text>
                    </template>
                  </n-statistic>
                  <n-progress type="line" :percentage="jvmMetrics.heapUsagePercent" :color="getUsageColor(jvmMetrics.heapUsagePercent)" style="margin-top: 8px" />
                </n-card>
              </n-gi>
              <n-gi span="4 m:1">
                <n-card :segmented="{ content: true }" size="small" hoverable>
                  <n-statistic :label="t('monitor.nonHeapMemory')" :value="jvmMetrics.nonHeapUsed + ' MB'">
                    <template #suffix>
                      <n-text depth="3">committed: {{ jvmMetrics.nonHeapCommitted }} MB</n-text>
                    </template>
                  </n-statistic>
                </n-card>
              </n-gi>
              <n-gi span="4 m:1">
                <n-card :segmented="{ content: true }" size="small" hoverable>
                  <n-statistic :label="t('monitor.threadCount')" :value="jvmMetrics.threadCount">
                    <template #suffix>
                      <n-text depth="3">{{ t('monitor.peakThread') }}: {{ jvmMetrics.peakThreadCount }}</n-text>
                    </template>
                  </n-statistic>
                </n-card>
              </n-gi>
              <n-gi span="4 m:1">
                <n-card :segmented="{ content: true }" size="small" hoverable>
                  <n-statistic :label="t('monitor.gcCount')" :value="jvmMetrics.totalGcCount">
                    <template #suffix>
                      <n-text depth="3">{{ t('monitor.gcTime') }}: {{ jvmMetrics.totalGcTime }}ms</n-text>
                    </template>
                  </n-statistic>
                </n-card>
              </n-gi>
            </n-grid>
            <n-card :title="t('monitor.jvmDetails')" size="small" style="margin-top: 16px">
              <n-descriptions :column="2" label-placement="left" bordered size="small">
                <n-descriptions-item :label="t('monitor.uptime')">{{ jvmMetrics.uptimeFormatted }}</n-descriptions-item>
                <n-descriptions-item :label="t('monitor.loadedClasses')">{{ jvmMetrics.loadedClassCount }}</n-descriptions-item>
                <n-descriptions-item :label="t('monitor.daemonThreads')">{{ jvmMetrics.daemonThreadCount }}</n-descriptions-item>
                <n-descriptions-item :label="t('monitor.totalStartedThreads')">{{ jvmMetrics.totalStartedThreadCount }}</n-descriptions-item>
                <n-descriptions-item :label="t('monitor.heapCommitted')">{{ jvmMetrics.heapCommitted }} MB</n-descriptions-item>
                <n-descriptions-item :label="t('monitor.heapUsagePercent')">{{ jvmMetrics.heapUsagePercent }}%</n-descriptions-item>
              </n-descriptions>
            </n-card>
            <n-card v-if="jvmMetrics.gcDetails && jvmMetrics.gcDetails.length > 0" :title="t('monitor.gcDetails')" size="small" style="margin-top: 16px">
              <n-data-table :columns="gcColumns" :data="jvmMetrics.gcDetails" size="small" :bordered="false" />
            </n-card>
          </n-spin>
        </n-tab-pane>

        <!-- 指标趋势 -->
        <n-tab-pane name="trends" :tab="t('monitor.metricsTrend')">
          <n-space>
            <n-select v-model:value="trendHours" :options="trendHoursOptions" style="width: 150px" @update:value="loadMetricsHistory" />
            <n-button type="primary" :loading="loadingTrends" @click="loadMetricsHistory">{{ t('monitor.refreshTrend') }}</n-button>
          </n-space>
          <n-spin :show="loadingTrends">
            <n-empty v-if="metricsHistory.length === 0" :description="t('monitor.noHistoryData')" style="padding: 60px 0" />
            <div v-else>
              <n-card :title="t('monitor.resourceUsageTrend')" size="small" style="margin-top: 16px">
                <div ref="trendChartRef" style="width: 100%; height: 320px"></div>
              </n-card>
              <n-card :title="t('monitor.jvmThreadTrend')" size="small" style="margin-top: 16px">
                <div ref="jvmTrendChartRef" style="width: 100%; height: 280px"></div>
              </n-card>
              <n-card :title="t('monitor.detailedData')" size="small" style="margin-top: 16px">
                <n-data-table :columns="trendColumns" :data="metricsHistory" :max-height="300" size="small" striped :pagination="{ pageSize: 20 }" />
              </n-card>
            </div>
          </n-spin>
        </n-tab-pane>

        <!-- 调度器管理 -->
        <n-tab-pane name="scheduler" :tab="t('monitor.scheduler')">
          <n-spin :show="loadingScheduler">
            <n-grid cols="3" x-gap="12" y-gap="12" style="margin-bottom: 16px" responsive="screen" item-responsive>
              <n-gi span="3 m:1">
                <n-card :segmented="{ content: true }" size="small">
                  <n-statistic :label="t('monitor.schedulerStatus')">
                    <template #prefix>
                      <n-icon size="20" :color="schedulerStatus.enabled ? '#18a058' : '#999'"><PlayCircleOutline /></n-icon>
                    </template>
                    <n-tag :type="schedulerStatus.enabled ? 'success' : 'default'" size="small">
                      {{ schedulerStatus.enabled ? t('monitor.running') : t('monitor.stopped') }}
                    </n-tag>
                  </n-statistic>
                </n-card>
              </n-gi>
              <n-gi span="3 m:1">
                <n-card :segmented="{ content: true }" size="small">
                  <n-statistic :label="t('monitor.maxConcurrent')" :value="schedulerStatus.maxConcurrentTasks" />
                </n-card>
              </n-gi>
              <n-gi span="3 m:1">
                <n-card :segmented="{ content: true }" size="small">
                  <n-button type="primary" :loading="triggeringScheduler" @click="handleTriggerScheduler">{{ t('monitor.triggerScheduler') }}</n-button>
                </n-card>
              </n-gi>
            </n-grid>
            <n-card :title="t('monitor.subTaskSwitch')" size="small">
              <n-descriptions :column="1" label-placement="left" bordered size="small">
                <n-descriptions-item :label="t('monitor.collectTaskSchedule')">
                  <n-tag :type="schedulerStatus.collectTaskEnabled ? 'success' : 'default'" size="small">{{ schedulerStatus.collectTaskEnabled ? t('monitor.schedulerEnabled') : t('monitor.schedulerDisabled') }}</n-tag>
                </n-descriptions-item>
                <n-descriptions-item :label="t('monitor.dataxJobSchedule')">
                  <n-tag :type="schedulerStatus.dataxJobEnabled ? 'success' : 'default'" size="small">{{ schedulerStatus.dataxJobEnabled ? t('monitor.schedulerEnabled') : t('monitor.schedulerDisabled') }}</n-tag>
                </n-descriptions-item>
                <n-descriptions-item :label="t('monitor.pipelineSchedule')">
                  <n-tag :type="schedulerStatus.pipelineEnabled ? 'success' : 'default'" size="small">{{ schedulerStatus.pipelineEnabled ? t('monitor.schedulerEnabled') : t('monitor.schedulerDisabled') }}</n-tag>
                </n-descriptions-item>
                <n-descriptions-item :label="t('monitor.statusDetails')">
                  <n-text depth="3" style="font-size: 12px">{{ schedulerStatus.statusText }}</n-text>
                </n-descriptions-item>
              </n-descriptions>
            </n-card>
          </n-spin>
        </n-tab-pane>

        <!-- 任务报告 -->
        <n-tab-pane name="report" :tab="t('monitor.taskReport')">
          <n-space vertical>
            <n-space>
              <n-date-picker v-model:value="reportDateRange" type="daterange" clearable />
              <n-button type="primary" :loading="loadingReport" @click="loadTaskReport">
                {{ t('monitor.queryReport') }}
              </n-button>
            </n-space>
            <n-spin :show="loadingReport">
              <n-data-table
                :columns="reportColumns"
                :data="taskReport"
                :pagination="{ pageSize: 10 }"
                :max-height="350"
                striped
              />
              <n-empty v-if="!loadingReport && taskReport.length === 0" :description="t('monitor.noTaskReport')" size="large" style="padding: 40px 0" />
            </n-spin>
          </n-space>
        </n-tab-pane>
      </n-tabs>
    </n-card>

    <!-- 系统维护对话框 -->
    <n-modal v-model:show="showMaintenanceModal" preset="card" :title="t('monitor.maintenance')" style="width: 480px; border-radius: 16px;">
      <n-space vertical :size="16">
        <n-alert type="warning" :title="t('monitor.warning')">
          {{ t('monitor.maintenanceWarning') }}
        </n-alert>
        
        <n-card size="small" :title="t('monitor.cleanLogsTitle')">
          <n-space vertical>
            <n-form-item :label="t('monitor.retainDays')" label-placement="left">
              <n-input-number v-model:value="cleanLogsDays" :min="30" :max="365" :step="30" style="width: 150px" />
            </n-form-item>
            <n-text depth="3" style="font-size: 12px">{{ t('monitor.cleanLogsDesc', { days: cleanLogsDays }) }}</n-text>
            <n-button type="primary" :loading="cleaningLogs" @click="handleCleanLogs">
              {{ t('monitor.executeClean') }}
            </n-button>
          </n-space>
        </n-card>

        <n-card size="small" :title="t('monitor.alertThresholdTitle')">
          <n-space vertical>
            <n-form-item :label="t('monitor.cpuAlert')" label-placement="left">
              <n-input-number v-model:value="alertThresholds.cpu" :min="50" :max="100" :step="5" style="width: 150px" />
            </n-form-item>
            <n-form-item :label="t('monitor.memoryAlert')" label-placement="left">
              <n-input-number v-model:value="alertThresholds.memory" :min="50" :max="100" :step="5" style="width: 150px" />
            </n-form-item>
            <n-form-item :label="t('monitor.diskAlert')" label-placement="left">
              <n-input-number v-model:value="alertThresholds.disk" :min="50" :max="100" :step="5" style="width: 150px" />
            </n-form-item>
            <n-text depth="3" style="font-size: 12px">{{ t('monitor.thresholdDesc') }}</n-text>
          </n-space>
        </n-card>

        <n-card size="small" :title="t('monitor.resetTasksTitle')">
          <n-space vertical>
            <n-form-item :label="t('monitor.timeoutHours')" label-placement="left">
              <n-input-number v-model:value="resetTaskHours" :min="1" :max="168" :step="1" style="width: 150px" />
            </n-form-item>
            <n-text depth="3" style="font-size: 12px">{{ t('monitor.resetTasksDesc', { hours: resetTaskHours }) }}</n-text>
            <n-button type="warning" :loading="resettingTasks" @click="handleResetTasks">
              {{ t('monitor.resetTasks') }}
            </n-button>
          </n-space>
        </n-card>
      </n-space>
    </n-modal>

    <!-- 通知抽屉 -->
    <n-drawer v-model:show="showNotifications" :width="400" placement="right">
      <n-drawer-content :title="t('monitor.systemNotifications')" closable>
        <n-empty v-if="health.unreadNotifications === 0" :description="t('monitor.noUnreadNotifications')" />
        <n-list v-else bordered>
          <n-list-item v-for="i in Math.min(health.unreadNotifications, 5)" :key="i">
            <n-thing :title="t('monitor.systemNotifications')" :description="t('monitor.sampleNotification')">
              <template #header-extra>
                <n-tag size="small" type="info">{{ t('monitor.unread') }}</n-tag>
              </template>
            </n-thing>
          </n-list-item>
        </n-list>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, onMounted, onUnmounted, onBeforeUnmount, h, watch, nextTick } from 'vue'
import echarts from '@/utils/echarts'
import { NTag, NButton, NIcon, useMessage } from 'naive-ui'
import { 
  RefreshOutline, 
  SettingsOutline, 
  ServerOutline, 
  GridOutline, 
  PlayCircleOutline, 
  StatsChartOutline, 
  NotificationsOutline,
  PulseOutline,
  HardwareChipOutline,
  FolderOutline
} from '@vicons/ionicons5'
import { systemMonitorApi, type ActiveTask, type DataSourceUsage, type OperationLogSummary, type TaskReport } from '@/api/systemMonitor'
import { useMonitorWebSocket } from '@/composables/useMonitorWebSocket'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const message = useMessage()

// WebSocket 实时数据
const { connected: wsConnected, data: wsData, connect: wsConnect, disconnect: wsDisconnect } = useMonitorWebSocket()

// 数据
const loading = ref(false)
const autoRefresh = ref(true)
const activeTab = ref('tasks')
const lastRefreshTime = ref<Date | null>(null)
const activeTasks = ref<ActiveTask[]>([])
const dataSourceUsage = ref<DataSourceUsage[]>([])
const operationLogs = ref<OperationLogSummary[]>([])
const showNotifications = ref(false)

// 任务报告
const loadingReport = ref(false)
const taskReport = ref<TaskReport[]>([])
const reportDateRange = ref<[number, number] | null>(null)

// 健康状态
const health = ref({
  totalTables: 0,
  runningTasks: 0,
  todayOperations: 0,
  unreadNotifications: 0,
  activeConnections: 0
})

// 告警阈值设置
const alertThresholds = reactive({ cpu: 80, memory: 85, disk: 90 })

// 服务器资源统计
const serverStats = ref({
  cpuUsage: 0,
  totalMemory: 0,
  usedMemory: 0,
  memoryUsage: 0,
  totalDisk: 0,
  usedDisk: 0,
  diskUsage: 0
})

// 数据库统计
const dbStats = ref({
  totalSize: 0,
  dataSize: 0,
  indexSize: 0,
  freeSize: 0,
  usagePercent: 0
})

// 维护相关
const showMaintenanceModal = ref(false)
const cleanLogsDays = ref(90)
const resetTaskHours = ref(24)
const cleaningLogs = ref(false)
const resettingTasks = ref(false)

// 定时刷新
let refreshTimer: number | null = null

// 格式化文件大小
const formatSize = (bytes: number): string => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

// 格式化时间
const formatTime = (date: Date): string => {
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

// 格式化日期
const formatDate = (timestamp: number): string => {
  return new Date(timestamp).toISOString().split('T')[0]
}

// 获取使用率颜色
const getUsageColor = (percent: number, type?: 'cpu' | 'memory' | 'disk'): string => {
  const threshold = type ? alertThresholds[type] : 80
  if (percent >= threshold) return '#d03050'
  if (percent >= threshold * 0.75) return '#f0a020'
  return '#18a058'
}

// 计算百分比
const getPercent = (value: number, total: number): number => {
  if (!total || total === 0) return 0
  return Math.round((value / total) * 100)
}

// 活跃任务表格列
const activeTaskColumns = [
  { title: t('monitor.taskName'), key: 'task_name', ellipsis: { tooltip: true } },
  { title: t('monitor.dataSource'), key: 'data_source_name', ellipsis: { tooltip: true } },
  { title: t('monitor.tableName'), key: 'table_name', ellipsis: { tooltip: true } },
  {
    title: t('monitor.status'),
    key: 'status',
    width: 100,
    render: () => h(NTag, { type: 'success', size: 'small' }, { default: () => t('monitor.runningStatus') })
  },
  {
    title: t('monitor.runningDuration'),
    key: 'running_minutes',
    width: 120,
    render: (row: ActiveTask) => {
      const minutes = row.running_minutes || 0
      if (minutes < 60) return t('monitor.minutes', { n: minutes })
      const hours = Math.floor(minutes / 60)
      const mins = minutes % 60
      return t('monitor.hoursMinutes', { h: hours, m: mins })
    }
  }
]

// 数据源表格列
const dataSourceColumns = [
  { title: t('monitor.dataSource'), key: 'name', ellipsis: { tooltip: true } },
  {
    title: t('monitor.type'),
    key: 'db_type',
    width: 100,
    render: (row: DataSourceUsage) => h(NTag, { size: 'small' }, { default: () => (row.db_type || '').toUpperCase() })
  },
  { title: t('monitor.host'), key: 'host', width: 150, ellipsis: { tooltip: true } },
  { title: t('monitor.sourceTask'), key: 'source_task_count', width: 80 },
  { title: t('monitor.targetTask'), key: 'target_task_count', width: 80 },
  {
    title: t('monitor.total'),
    key: 'total_task_count',
    width: 80,
    render: (row: DataSourceUsage) => h(NTag, { type: row.total_task_count > 0 ? 'info' : 'default', size: 'small' }, { default: () => row.total_task_count })
  }
]

// 日志表格列
const logColumns = [
  { title: t('monitor.module'), key: 'module_name', width: 120 },
  { title: t('monitor.operationType'), key: 'operation_type', width: 100 },
  { title: t('monitor.totalCount'), key: 'operation_count', width: 80 },
  {
    title: t('monitor.success'),
    key: 'success_count',
    width: 80,
    render: (row: OperationLogSummary) => h(NTag, { type: 'success', size: 'small' }, { default: () => row.success_count })
  },
  {
    title: t('monitor.failure'),
    key: 'failure_count',
    width: 80,
    render: (row: OperationLogSummary) => {
      if (row.failure_count > 0) {
        return h(NTag, { type: 'error', size: 'small' }, { default: () => row.failure_count })
      }
      return h('span', {}, '0')
    }
  },
  {
    title: t('monitor.avgDuration'),
    key: 'avg_duration_ms',
    width: 100,
    render: (row: OperationLogSummary) => `${Math.round(row.avg_duration_ms || 0)}ms`
  }
]

// 任务报告表格列
const reportColumns = [
  { title: t('monitor.date'), key: 'execute_date', width: 120 },
  { title: t('monitor.totalExecutions'), key: 'total_executions', width: 100 },
  {
    title: t('monitor.running'),
    key: 'running_count',
    width: 80,
    render: (row: TaskReport) => h(NTag, { type: 'info', size: 'small' }, { default: () => row.running_count })
  },
  {
    title: t('monitor.stoppedCount'),
    key: 'stopped_count',
    width: 80,
    render: (row: TaskReport) => h(NTag, { type: 'default', size: 'small' }, { default: () => row.stopped_count })
  },
  {
    title: t('monitor.success'),
    key: 'success_count',
    width: 80,
    render: (row: TaskReport) => h(NTag, { type: 'success', size: 'small' }, { default: () => row.success_count })
  },
  {
    title: t('monitor.errorCount'),
    key: 'error_count',
    width: 80,
    render: (row: TaskReport) => row.error_count > 0 
      ? h(NTag, { type: 'error', size: 'small' }, { default: () => row.error_count })
      : h('span', {}, '0')
  }
]

// 解析健康数据
const parseHealthData = (data: any) => {
  // 服务器资源
  serverStats.value = {
    cpuUsage: Math.round(data['CPU Usage'] || 0),
    totalMemory: data['Total Memory (GB)'] || 0,
    usedMemory: data['Used Memory (GB)'] || 0,
    memoryUsage: Math.round(data['Memory Usage (%)'] || 0),
    totalDisk: data['Total Disk Space (GB)'] || 0,
    usedDisk: data['Used Disk Space (GB)'] || 0,
    diskUsage: Math.round(data['Disk Usage (%)'] || 0)
  }

  // 健康状态
  health.value = {
    totalTables: data['Total Tables'] || 0,
    runningTasks: data['Running Tasks'] || 0,
    todayOperations: data['Today Operations'] || 0,
    unreadNotifications: data['Unread Notifications'] || 0,
    activeConnections: data['Active DB Connections'] || 0
  }

  // 数据库大小（使用真实数据，MB转字节）
  const dataSizeMB = data['Data Size (MB)'] || 0
  const indexSizeMB = data['Index Size (MB)'] || 0
  const freeSizeMB = data['Free Size (MB)'] || 0
  const totalSizeMB = data['Database Size (MB)'] || (dataSizeMB + indexSizeMB)
  
  const dataSize = dataSizeMB * 1024 * 1024
  const indexSize = indexSizeMB * 1024 * 1024
  const freeSize = freeSizeMB * 1024 * 1024
  const totalSize = totalSizeMB * 1024 * 1024
  
  dbStats.value = {
    totalSize: totalSize,
    dataSize: dataSize,
    indexSize: indexSize,
    freeSize: freeSize,
    usagePercent: totalSize > 0 ? Math.round((dataSize + indexSize) / (totalSize + freeSize) * 100) : 0
  }
}

// 加载所有数据
const loadAllData = async () => {
  loading.value = true
  try {
    const [healthRes, tasksRes, usageRes, logsRes] = await Promise.all([
      systemMonitorApi.getHealth(),
      systemMonitorApi.getActiveTasks(),
      systemMonitorApi.getDataSourceUsage(),
      systemMonitorApi.getOperationLogs()
    ])

    parseHealthData(healthRes.data)
    activeTasks.value = tasksRes.data || []
    dataSourceUsage.value = usageRes.data || []
    operationLogs.value = logsRes.data || []
    lastRefreshTime.value = new Date()
  } catch (error) {
    console.error(t('monitor.loadFailed'), error)
    message.error(t('monitor.loadFailed'))
  } finally {
    loading.value = false
  }
}

// 加载任务报告
const loadTaskReport = async () => {
  if (!reportDateRange.value) {
    message.warning(t('monitor.selectDateRange'))
    return
  }
  loadingReport.value = true
  try {
    const [start, end] = reportDateRange.value
    const res = await systemMonitorApi.getTaskReport(formatDate(start), formatDate(end))
    taskReport.value = res.data || []
  } catch (error) {
    console.error(t('monitor.loadReportFailed'), error)
    message.error(t('monitor.loadReportFailed'))
  } finally {
    loadingReport.value = false
  }
}

// 刷新所有数据
const refreshAll = () => {
  loadAllData()
  message.success(t('monitor.dataRefreshed'))
}

// 清理日志
const handleCleanLogs = async () => {
  cleaningLogs.value = true
  try {
    const res = await systemMonitorApi.cleanLogs(cleanLogsDays.value)
    message.success(t('monitor.cleanSuccess', { count: res.data.deleted_records }))
    const logsRes = await systemMonitorApi.getOperationLogs()
    operationLogs.value = logsRes.data || []
  } catch (error) {
    console.error(t('monitor.cleanFailed'), error)
    message.error(t('monitor.cleanFailed'))
  } finally {
    cleaningLogs.value = false
  }
}

// 重置任务
const handleResetTasks = async () => {
  resettingTasks.value = true
  try {
    const res = await systemMonitorApi.resetStuckTasks(resetTaskHours.value)
    if (res.data.reset_tasks > 0) {
      message.success(t('monitor.resetSuccess', { count: res.data.reset_tasks }))
      const tasksRes = await systemMonitorApi.getActiveTasks()
      activeTasks.value = tasksRes.data || []
      // 刷新健康数据
      const healthRes = await systemMonitorApi.getHealth()
      parseHealthData(healthRes.data)
    } else {
      message.info(t('monitor.noTasksToReset'))
    }
  } catch (error) {
    console.error(t('monitor.resetFailed'), error)
    message.error(t('monitor.resetFailed'))
  } finally {
    resettingTasks.value = false
  }
}

// ==================== 调度器管理 ====================
const loadingScheduler = ref(false)
const triggeringScheduler = ref(false)
const schedulerStatus = ref<any>({
  enabled: false,
  maxConcurrentTasks: 0,
  collectTaskEnabled: false,
  dataxJobEnabled: false,
  pipelineEnabled: false,
  statusText: ''
})

const loadSchedulerStatus = async () => {
  loadingScheduler.value = true
  try {
    const res = await systemMonitorApi.getSchedulerStatus()
    schedulerStatus.value = res.data || schedulerStatus.value
  } catch (error) {
    console.error('loadSchedulerStatus failed', error)
  } finally {
    loadingScheduler.value = false
  }
}

const handleTriggerScheduler = async () => {
  triggeringScheduler.value = true
  try {
    await systemMonitorApi.triggerScheduler()
    message.success(t('monitor.schedulerTriggered'))
    await loadSchedulerStatus()
  } catch (error) {
    console.error(t('monitor.triggerFailed'), error)
    message.error(t('monitor.triggerFailed'))
  } finally {
    triggeringScheduler.value = false
  }
}

// ==================== JVM监控 ====================
const loadingJvm = ref(false)
const jvmMetrics = ref<any>({
  heapUsed: 0, heapMax: 0, heapCommitted: 0, heapUsagePercent: 0,
  nonHeapUsed: 0, nonHeapCommitted: 0,
  threadCount: 0, peakThreadCount: 0, daemonThreadCount: 0, totalStartedThreadCount: 0,
  totalGcCount: 0, totalGcTime: 0, gcDetails: [],
  uptimeFormatted: '', loadedClassCount: 0
})

const gcColumns = [
  { title: t('monitor.gcName'), key: 'name', width: 200 },
  { title: t('monitor.gcCollectionCount'), key: 'collectionCount', width: 120 },
  { title: t('monitor.gcCollectionTime'), key: 'collectionTime', width: 120 }
]

const loadJvmMetrics = async () => {
  loadingJvm.value = true
  try {
    const res = await systemMonitorApi.getJvmMetrics()
    jvmMetrics.value = res.data || {}
  } catch (error) {
    console.error('loadJvmMetrics failed', error)
  } finally {
    loadingJvm.value = false
  }
}

// ==================== 指标趋势 ====================
const loadingTrends = ref(false)
const metricsHistory = ref<any[]>([])
const trendHours = ref(24)
const trendHoursOptions = [
  { label: t('monitor.last6Hours'), value: 6 },
  { label: t('monitor.last12Hours'), value: 12 },
  { label: t('monitor.last24Hours'), value: 24 },
  { label: t('monitor.last48Hours'), value: 48 },
  { label: t('monitor.last7DaysOption'), value: 168 }
]

const trendColumns = [
  { title: t('monitor.time'), key: 'time', width: 80 },
  { title: t('monitor.cpuPercent'), key: 'cpuUsage', width: 80, render: (row: any) => (row.cpuUsage || 0).toFixed(1) },
  { title: t('monitor.memoryPercent'), key: 'memoryUsage', width: 80, render: (row: any) => (row.memoryUsage || 0).toFixed(1) },
  { title: t('monitor.diskPercent'), key: 'diskUsage', width: 80, render: (row: any) => (row.diskUsage || 0).toFixed(1) },
  { title: t('monitor.heapMemoryMB'), key: 'heapUsed', width: 100 },
  { title: t('monitor.threadCountCol'), key: 'threadCount', width: 80 },
  { title: t('monitor.gcCountCol'), key: 'gcCount', width: 80 },
  { title: t('monitor.activeConnectionsCol'), key: 'activeConnections', width: 80 },
  { title: t('monitor.runningTasksCol'), key: 'runningTasks', width: 80 }
]

// 趋势图表引用
const trendChartRef = ref<HTMLDivElement | null>(null)
const jvmTrendChartRef = ref<HTMLDivElement | null>(null)
let trendChartInstance: echarts.ECharts | null = null
let jvmTrendChartInstance: echarts.ECharts | null = null

const loadMetricsHistory = async () => {
  loadingTrends.value = true
  try {
    const res = await systemMonitorApi.getMetricsHistory(trendHours.value)
    metricsHistory.value = res.data || []
    await nextTick()
    renderTrendCharts()
  } catch (error) {
    console.error('loadMetricsHistory failed', error)
  } finally {
    loadingTrends.value = false
  }
}

const renderTrendCharts = () => {
  if (!metricsHistory.value.length) return
  const times = metricsHistory.value.map(m => m.time)
  
  // 资源使用率趋势图
  if (trendChartRef.value) {
    if (trendChartInstance) trendChartInstance.dispose()
    trendChartInstance = echarts.init(trendChartRef.value)
    trendChartInstance.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
      legend: { data: [t('monitor.cpuPercent'), t('monitor.memoryPercent'), t('monitor.diskPercent')], bottom: 0 },
      grid: { top: 20, right: 30, bottom: 40, left: 50 },
      xAxis: { type: 'category', data: times, boundaryGap: false },
      yAxis: { type: 'value', min: 0, max: 100, axisLabel: { formatter: '{value}%' } },
      series: [
        { name: t('monitor.cpuPercent'), type: 'line', data: metricsHistory.value.map(m => m.cpuUsage?.toFixed(1)), smooth: true, areaStyle: { opacity: 0.1 }, lineStyle: { width: 2 }, itemStyle: { color: '#d03050' } },
        { name: t('monitor.memoryPercent'), type: 'line', data: metricsHistory.value.map(m => m.memoryUsage?.toFixed(1)), smooth: true, areaStyle: { opacity: 0.1 }, lineStyle: { width: 2 }, itemStyle: { color: '#2080f0' } },
        { name: t('monitor.diskPercent'), type: 'line', data: metricsHistory.value.map(m => m.diskUsage?.toFixed(1)), smooth: true, areaStyle: { opacity: 0.1 }, lineStyle: { width: 2 }, itemStyle: { color: '#f0a020' } }
      ]
    })
  }
  
  // JVM/线程趋势图
  if (jvmTrendChartRef.value) {
    if (jvmTrendChartInstance) jvmTrendChartInstance.dispose()
    jvmTrendChartInstance = echarts.init(jvmTrendChartRef.value)
    jvmTrendChartInstance.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: [t('monitor.heapMemoryMB'), t('monitor.threadCountCol'), t('monitor.gcCountCol')], bottom: 0 },
      grid: { top: 20, right: 60, bottom: 40, left: 60 },
      xAxis: { type: 'category', data: times, boundaryGap: false },
      yAxis: [
        { type: 'value', name: 'MB', position: 'left' },
        { type: 'value', name: '#', position: 'right' }
      ],
      series: [
        { name: t('monitor.heapMemoryMB'), type: 'line', data: metricsHistory.value.map(m => m.heapUsed), smooth: true, areaStyle: { opacity: 0.15 }, lineStyle: { width: 2 }, itemStyle: { color: '#18a058' } },
        { name: t('monitor.threadCountCol'), type: 'line', yAxisIndex: 1, data: metricsHistory.value.map(m => m.threadCount), smooth: true, lineStyle: { width: 2 }, itemStyle: { color: '#722ed1' } },
        { name: t('monitor.gcCountCol'), type: 'line', yAxisIndex: 1, data: metricsHistory.value.map(m => m.gcCount), smooth: true, lineStyle: { width: 2, type: 'dashed' }, itemStyle: { color: '#eb2f96' } }
      ]
    })
  }
}

const handleTrendResize = () => {
  trendChartInstance?.resize()
  jvmTrendChartInstance?.resize()
}

// 启动定时刷新
const startAutoRefresh = () => {
  if (refreshTimer) clearInterval(refreshTimer)
  refreshTimer = window.setInterval(() => {
    if (autoRefresh.value) loadAllData()
  }, 30000)
}

// 监听自动刷新开关
watch(autoRefresh, (val) => {
  message.info(val ? t('monitor.autoRefreshOn') : t('monitor.autoRefreshOff'))
})

// 初始化日期范围（最近7天）
const initDateRange = () => {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 7)
  reportDateRange.value = [start.getTime(), end.getTime()]
}

// WebSocket 实时数据更新
watch(() => wsData.value.health, (newHealth) => {
  if (newHealth && wsConnected.value) {
    parseHealthData(newHealth)
    lastRefreshTime.value = new Date()
  }
}, { deep: true })

watch(() => wsData.value.jvm, (newJvm) => {
  if (newJvm && wsConnected.value) {
    jvmMetrics.value = newJvm
  }
}, { deep: true })

onMounted(() => {
  loadAllData()
  loadJvmMetrics()
  loadMetricsHistory()
  loadSchedulerStatus()
  startAutoRefresh()
  initDateRange()
  window.addEventListener('resize', handleTrendResize)
  // 启动 WebSocket 实时连接
  wsConnect()
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  window.removeEventListener('resize', handleTrendResize)
  if (trendChartInstance) { trendChartInstance.dispose(); trendChartInstance = null }
  if (jvmTrendChartInstance) { jvmTrendChartInstance.dispose(); jvmTrendChartInstance = null }
})
</script>

<style scoped>
.system-monitor {
  padding: 20px;
}

/* 页面头部样式 */
:deep(.n-page-header) {
  background: var(--bg-primary);
  padding: 20px 24px;
  border-radius: 16px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-sm);
}

:deep(.n-page-header .n-page-header-header__title) {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
}

/* 资源监控卡片 */
.resource-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.resource-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
  border-color: rgba(0, 102, 255, 0.2);
}

.resource-header {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  font-size: 15px;
  color: #475569;
}

/* 进度环样式 */
:deep(.n-progress--circle .n-progress-graph-circle-fill) {
  transition: stroke-dashoffset 0.6s ease;
}

/* 统计卡片样式 */
:deep(.n-card--hoverable) {
  border-radius: 14px;
  transition: all 0.3s ease;
  border: 1px solid rgba(226, 232, 240, 0.8);
}

:deep(.n-card--hoverable:hover) {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
}

:deep(.n-statistic .n-statistic-value) {
  font-size: 32px;
  font-weight: 700;
  color: #1e293b;
}

:deep(.n-statistic .n-statistic-label) {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 8px;
}

/* 标签页样式 */
:deep(.n-tabs .n-tabs-tab) {
  font-weight: 500;
  padding: 12px 20px;
  border-radius: 10px 10px 0 0;
  transition: all 0.2s ease;
}

:deep(.n-tabs .n-tabs-tab--active) {
  background: linear-gradient(135deg, rgba(0, 102, 255, 0.1), rgba(0, 102, 255, 0.05));
}

/* 表格样式 */
:deep(.n-data-table) {
  border-radius: 12px;
}

:deep(.n-data-table-th) {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%) !important;
  font-weight: 600 !important;
  color: #475569 !important;
}

:deep(.n-data-table-tr--striped .n-data-table-td) {
  background: rgba(248, 250, 252, 0.5);
}

/* 维护对话框样式 */
:deep(.n-modal) {
  border-radius: 16px;
}

:deep(.n-modal .n-card-header) {
  padding: 20px 24px;
  border-bottom: 1px solid #f1f5f9;
}

/* 空状态样式 */
:deep(.n-empty) {
  padding: 48px 0;
}

/* 徽章样式 */
:deep(.n-badge) {
  margin-left: 8px;
}

/* 日期选择器样式 */
:deep(.n-date-picker) {
  border-radius: 10px;
}

/* 滑块样式 */
:deep(.n-slider) {
  margin: 8px 0;
}

:deep(.n-slider-rail__fill) {
  background: linear-gradient(90deg, var(--color-primary), #00D4FF);
}

/* 警告框样式 */
:deep(.n-alert) {
  border-radius: 12px;
}

/* 抽屉样式 */
:deep(.n-drawer) {
  border-radius: 16px 0 0 16px;
}

:deep(.n-drawer-header) {
  padding: 20px 24px;
  border-bottom: 1px solid #f1f5f9;
}

/* 响应式 */
@media (max-width: 768px) {
  .system-monitor {
    padding: 10px;
  }
  
  :deep(.n-page-header) {
    padding: 12px;
  }
  
  :deep(.n-page-header__extra) {
    flex-wrap: wrap;
    gap: 6px;
  }
  
  .resource-item {
    padding: 12px;
  }
  
  .page-header-stats {
    flex-direction: row !important;
    overflow-x: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;
    gap: 8px;
  }
  .page-header-stats::-webkit-scrollbar { display: none; }
  .stat-item { min-width: 110px; flex-shrink: 0; }
  .main-card { border-radius: 12px !important; }
}

</style>

<style>
/* SystemMonitor 深色模式（非 scoped） */
html.dark .metric-title { color: #e2e8f0 !important; }
html.dark .metric-desc { color: #94a3b8 !important; }
html.dark .metric-value { color: #e2e8f0 !important; }
html.dark .metric-label { color: #64748b !important; }
</style>

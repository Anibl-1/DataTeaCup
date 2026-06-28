<template>
  <div class="ops-manage-page">
    <!-- 系统概览统计 -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><ServerOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ overview.version || '-' }}</span>
          <span class="stat-label">{{ t('opsManage.systemVersion') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><TimeOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ overview.uptime || '-' }}</span>
          <span class="stat-label">{{ t('opsManage.uptime') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="24"><HardwareChipOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ overview.jvmMemory || '-' }}</span>
          <span class="stat-label">{{ t('opsManage.jvmMemory') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info">
          <n-icon size="24"><GitBranchOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ overview.threadCount ?? '-' }}</span>
          <span class="stat-label">{{ t('opsManage.threadCount') }}</span>
        </div>
      </div>
    </div>

    <!-- 标签页内容 -->
    <n-card>
      <template #header>
        <div class="card-header-custom">
          <div class="card-title">
            <n-icon size="22" color="var(--color-primary)"><SettingsOutline /></n-icon>
            <span>{{ t('opsManage.opsManagement') }}</span>
          </div>
        </div>
      </template>
      <template #header-extra>
        <n-button quaternary circle :loading="refreshing" @click="refreshAll">
          <template #icon><n-icon><RefreshOutline /></n-icon></template>
        </n-button>
      </template>

      <n-descriptions bordered size="small" :column="4" class="ops-summary">
        <n-descriptions-item label="Java">{{ overview.javaVersion }}</n-descriptions-item>
        <n-descriptions-item label="操作系统">{{ overview.osName }}</n-descriptions-item>
        <n-descriptions-item label="启动时间">{{ overview.startTime }}</n-descriptions-item>
        <n-descriptions-item label="处理器">{{ overview.availableProcessors }}</n-descriptions-item>
        <n-descriptions-item label="非堆内存">{{ overview.nonHeapMemory }}</n-descriptions-item>
        <n-descriptions-item label="待升级告警">{{ overview.pendingEscalations }}</n-descriptions-item>
        <n-descriptions-item label="静默规则">{{ overview.silenceRuleCount }}</n-descriptions-item>
        <n-descriptions-item label="当前值班">
          <n-space size="small">
            <n-tag v-for="person in overview.onCallPersons" :key="person" size="small" type="info">
              {{ person }}
            </n-tag>
            <span v-if="overview.onCallPersons.length === 0">-</span>
          </n-space>
        </n-descriptions-item>
      </n-descriptions>

      <n-tabs type="line" animated>
        <!-- 性能异常 -->
        <n-tab-pane name="anomalies" :tab="t('opsManage.performanceAnomalies')">
          <n-data-table
            :columns="anomalyColumns"
            :data="anomalies"
            :loading="anomaliesLoading"
            :pagination="false"
            :scroll-x="1000"
            striped
            class="custom-table"
          />
        </n-tab-pane>

        <!-- 告警聚合 -->
        <n-tab-pane name="alerts" :tab="t('opsManage.alertAggregation')">
          <n-spin :show="alertLoading">
            <div class="alert-summary-grid">
              <div class="alert-summary-item">
                <span>活跃指纹</span>
                <strong>{{ alertActiveFingerprints }}</strong>
              </div>
              <div class="alert-summary-item">
                <span>聚合条目</span>
                <strong>{{ alertEntries.length }}</strong>
              </div>
            </div>
            <n-data-table
              v-if="alertEntries.length > 0"
              :columns="alertColumns"
              :data="alertEntries"
              :pagination="false"
              :scroll-x="760"
              striped
              class="custom-table"
            />
            <div v-else class="empty-state">{{ t('opsManage.noAlertData') }}</div>
          </n-spin>
        </n-tab-pane>

        <!-- 静默规则 -->
        <n-tab-pane name="silence" :tab="t('opsManage.silenceRules')">
          <n-space style="margin-bottom: 12px;">
            <n-button type="primary" @click="showSilenceModal = true">
              <template #icon><n-icon><AddOutline /></n-icon></template>
              {{ t('opsManage.addRule') }}
            </n-button>
          </n-space>
          <n-data-table
            :columns="silenceColumns"
            :data="silenceRules"
            :loading="silenceLoading"
            :pagination="false"
            :row-key="(row: SilenceRule) => row.id"
            :scroll-x="900"
            striped
            class="custom-table"
          />
        </n-tab-pane>

        <!-- 值班表 -->
        <n-tab-pane name="oncall" :tab="t('opsManage.oncallSchedule')">
          <n-spin :show="oncallLoading">
            <div v-if="oncallSchedules.length === 0" class="empty-state">{{ t('opsManage.noOncallData') }}</div>
            <div v-else class="oncall-list">
              <div v-for="schedule in oncallSchedules" :key="schedule.id" class="oncall-schedule">
                <div class="oncall-schedule-header">
                  <span>{{ schedule.name }}</span>
                  <n-tag :type="schedule.enabled ? 'success' : 'default'" size="small">
                    {{ schedule.enabled ? t('opsManage.enabled') : t('opsManage.disabled') }}
                  </n-tag>
                </div>
                <n-data-table
                  :columns="shiftColumns"
                  :data="schedule.shifts || []"
                  :pagination="false"
                  :scroll-x="700"
                  size="small"
                  striped
                  class="custom-table"
                />
              </div>
            </div>
          </n-spin>
        </n-tab-pane>

        <!-- 审计记录 -->
        <n-tab-pane name="audit" :tab="t('opsManage.auditRecords')">
          <n-data-table
            :columns="auditColumns"
            :data="auditRecords"
            :loading="auditLoading"
            :pagination="false"
            :scroll-x="1200"
            striped
            class="custom-table"
          />
        </n-tab-pane>
      </n-tabs>
    </n-card>

    <!-- 新增静默规则弹窗 -->
    <n-modal v-model:show="showSilenceModal" preset="card" :title="t('opsManage.addSilenceRule')" style="width: 480px; max-width: calc(100vw - 32px); border-radius: 8px;">
      <n-form
        ref="silenceFormRef"
        :model="silenceForm"
        :rules="silenceFormRules"
        label-placement="left"
        label-width="100px"
      >
        <n-form-item :label="t('opsManage.ruleName')" path="name">
          <n-input v-model:value="silenceForm.name" :placeholder="t('opsManage.ruleNamePlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('opsManage.matchPattern')" path="matchPattern">
          <n-input v-model:value="silenceForm.matchPattern" :placeholder="t('opsManage.matchPatternPlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('opsManage.startTime')" path="startTime">
          <n-date-picker v-model:formatted-value="silenceForm.startTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" clearable style="width: 100%;" />
        </n-form-item>
        <n-form-item :label="t('opsManage.endTime')" path="endTime">
          <n-date-picker v-model:formatted-value="silenceForm.endTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" clearable style="width: 100%;" />
        </n-form-item>
        <n-form-item :label="t('opsManage.createdBy')" path="createdBy">
          <n-input v-model:value="silenceForm.createdBy" :placeholder="t('opsManage.createdByPlaceholder')" />
        </n-form-item>
        <n-form-item>
          <n-space style="width: 100%; justify-content: flex-end;">
            <n-button @click="showSilenceModal = false">{{ t('opsManage.cancel') }}</n-button>
            <n-button type="primary" :loading="silenceSubmitLoading" @click="handleCreateSilenceRule">{{ t('opsManage.save') }}</n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, onMounted, h, computed } from 'vue'
import { NButton, NTag, useMessage, useDialog, type FormInst } from 'naive-ui'
import {
  ServerOutline,
  TimeOutline,
  HardwareChipOutline,
  GitBranchOutline,
  SettingsOutline,
  AddOutline,
  RefreshOutline
} from '@vicons/ionicons5'
import {
  getSystemOverview,
  getAnomalies,
  getAlertAggregation,
  getSilenceRules,
  createSilenceRule,
  deleteSilenceRule,
  getOncallSchedules,
  getAuditRecords,
  type SilenceRule,
  type OpsAuditRecord,
  type AnomalyRecord,
  type OnCallSchedule
} from '@/api/ops'
import { formatDateTime } from '@/utils/format'
import { handleApiError } from '@/utils/error'
import { initMessage } from '@/utils/message'
import { useI18n } from '@/i18n'

const { t } = useI18n()

const message = useMessage()
const dialog = useDialog()
initMessage(message)

// ==================== 系统概览 ====================
const overview = reactive({
  version: '' as string,
  uptime: '' as string,
  jvmMemory: '' as string,
  threadCount: null as number | string | null,
  javaVersion: '-' as string,
  osName: '-' as string,
  startTime: '-' as string,
  availableProcessors: '-' as number | string,
  nonHeapMemory: '-' as string,
  pendingEscalations: 0,
  silenceRuleCount: 0,
  onCallPersons: [] as string[]
})

const refreshing = ref(false)

const formatDuration = (milliseconds: unknown) => {
  const value = Number(milliseconds)
  if (!Number.isFinite(value) || value < 0) return '-'
  const totalSeconds = Math.floor(value / 1000)
  const days = Math.floor(totalSeconds / 86400)
  const hours = Math.floor((totalSeconds % 86400) / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  if (days > 0) return `${days}天 ${hours}小时`
  if (hours > 0) return `${hours}小时 ${minutes}分钟`
  return `${minutes}分钟`
}

const formatBytes = (bytes: unknown) => {
  const value = Number(bytes)
  if (!Number.isFinite(value) || value < 0) return '-'
  if (value === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const unitIndex = Math.min(Math.floor(Math.log(value) / Math.log(1024)), units.length - 1)
  return `${(value / Math.pow(1024, unitIndex)).toFixed(unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`
}

const fetchOverview = async () => {
  try {
    const res = await getSystemOverview()
    const data: Record<string, any> = (res as any).data || res
    const jvm = data.jvm || {}
    const alerts = data.alerts || {}
    overview.version = data.version || data.appVersion || '-'
    overview.uptime = data.uptimeFormatted || formatDuration(data.uptime)
    overview.jvmMemory = data.jvmMemory || `${formatBytes(jvm.heapUsed)} / ${formatBytes(jvm.heapMax)}`
    overview.threadCount = jvm.threadCount ?? data.threadCount ?? data.activeThreads ?? '-'
    overview.javaVersion = data.javaVersion || '-'
    overview.osName = data.osName || '-'
    overview.startTime = data.startTime ? formatDateTime(data.startTime) : '-'
    overview.availableProcessors = jvm.availableProcessors ?? '-'
    overview.nonHeapMemory = formatBytes(jvm.nonHeapUsed)
    overview.pendingEscalations = Number(alerts.pendingEscalations ?? 0)
    overview.silenceRuleCount = Number(alerts.silenceRules ?? 0)
    overview.onCallPersons = Array.isArray(alerts.onCallPersons) ? alerts.onCallPersons : []
  } catch (error) {
    message.error(handleApiError(error, t('opsManage.getOverviewError')))
  }
}

// ==================== 性能异常 ====================
const anomalies = ref<AnomalyRecord[]>([])
const anomaliesLoading = ref(false)

const anomalyColumns = [
  { title: t('opsManage.metricName'), key: 'metricName', ellipsis: { tooltip: true } },
  { title: t('opsManage.currentValue'), key: 'value', width: 100, render: (row: AnomalyRecord) => row.value?.toFixed(2) ?? '-' },
  { title: t('opsManage.mean'), key: 'mean', width: 100, render: (row: AnomalyRecord) => row.mean?.toFixed(2) ?? '-' },
  { title: t('opsManage.stdDev'), key: 'stdDev', width: 100, render: (row: AnomalyRecord) => row.stdDev?.toFixed(2) ?? '-' },
  { title: t('opsManage.threshold'), key: 'threshold', width: 100, render: (row: AnomalyRecord) => row.threshold?.toFixed(2) ?? '-' },
  { title: t('opsManage.time'), key: 'timestamp', width: 180, render: (row: AnomalyRecord) => formatDateTime(row.timestamp) }
]

const fetchAnomalies = async () => {
  anomaliesLoading.value = true
  try {
    const res = await getAnomalies()
    anomalies.value = (res as any).data || res || []
  } catch (error) {
    message.error(handleApiError(error, t('opsManage.getAnomaliesError')))
  } finally {
    anomaliesLoading.value = false
  }
}

// ==================== 告警聚合 ====================
const alertAggregation = ref<Record<string, any>>({})
const alertLoading = ref(false)
const alertEntries = computed(() => Array.isArray(alertAggregation.value.entries) ? alertAggregation.value.entries : [])
const alertActiveFingerprints = computed(() => Number(alertAggregation.value.activeFingerprints ?? alertEntries.value.length))

const alertColumns = [
  { title: '告警指纹', key: 'fingerprint', ellipsis: { tooltip: true } },
  { title: '次数', key: 'count', width: 100 },
  { title: '首次出现', key: 'firstOccurrence', width: 180, render: (row: any) => formatDateTime(row.firstOccurrence) },
  { title: '最后发送', key: 'lastSentTime', width: 180, render: (row: any) => formatDateTime(row.lastSentTime) }
]

const fetchAlertAggregation = async () => {
  alertLoading.value = true
  try {
    const res = await getAlertAggregation()
    alertAggregation.value = (res as any).data || res || {}
  } catch (error) {
    message.error(handleApiError(error, t('opsManage.getAlertError')))
  } finally {
    alertLoading.value = false
  }
}

// ==================== 静默规则 ====================
const silenceRules = ref<SilenceRule[]>([])
const silenceLoading = ref(false)
const showSilenceModal = ref(false)
const silenceFormRef = ref<FormInst | null>(null)
const silenceSubmitLoading = ref(false)

const silenceForm = reactive({
  name: '',
  matchPattern: '',
  startTime: '',
  endTime: '',
  createdBy: ''
})

const silenceFormRules = {
  name: { required: true, message: t('opsManage.ruleNameRequired'), trigger: 'blur' },
  matchPattern: { required: true, message: t('opsManage.matchPatternRequired'), trigger: 'blur' },
  startTime: { required: true, message: t('opsManage.startTimeRequired'), trigger: 'blur' },
  endTime: { required: true, message: t('opsManage.endTimeRequired'), trigger: 'blur' },
  createdBy: { required: true, message: t('opsManage.createdByRequired'), trigger: 'blur' }
}

const silenceColumns = [
  { title: t('opsManage.ruleName'), key: 'name', ellipsis: { tooltip: true } },
  { title: t('opsManage.matchPattern'), key: 'matchPattern', width: 180 },
  { title: t('opsManage.startTime'), key: 'startTime', width: 180, render: (row: SilenceRule) => formatDateTime(row.startTime) },
  { title: t('opsManage.endTime'), key: 'endTime', width: 180, render: (row: SilenceRule) => formatDateTime(row.endTime) },
  { title: t('opsManage.createdBy'), key: 'createdBy', width: 120 },
  {
    title: t('opsManage.actions'),
    key: 'actions',
    width: 100,
    fixed: 'right' as const,
    render: (row: SilenceRule) => {
      return h(NButton, { size: 'small', type: 'error', onClick: () => handleDeleteSilenceRule(row) }, { default: () => t('opsManage.delete') })
    }
  }
]

const fetchSilenceRules = async () => {
  silenceLoading.value = true
  try {
    const res = await getSilenceRules()
    silenceRules.value = (res as any).data || res || []
  } catch (error) {
    message.error(handleApiError(error, t('opsManage.getSilenceRulesError')))
  } finally {
    silenceLoading.value = false
  }
}

const handleCreateSilenceRule = async () => {
  if (!silenceFormRef.value) return
  try {
    await silenceFormRef.value.validate()
  } catch {
    return
  }
  silenceSubmitLoading.value = true
  try {
    await createSilenceRule({ ...silenceForm })
    message.success(t('opsManage.createSuccess'))
    showSilenceModal.value = false
    Object.assign(silenceForm, { name: '', matchPattern: '', startTime: '', endTime: '', createdBy: '' })
    fetchSilenceRules()
  } catch (error) {
    message.error(handleApiError(error, t('opsManage.createError')))
  } finally {
    silenceSubmitLoading.value = false
  }
}

const handleDeleteSilenceRule = (row: SilenceRule) => {
  dialog.warning({
    title: t('opsManage.confirmDelete'),
    content: t('opsManage.confirmDeleteContent', { name: row.name }),
    positiveText: t('opsManage.confirm'),
    negativeText: t('opsManage.cancel'),
    onPositiveClick: async () => {
      try {
        await deleteSilenceRule(row.id)
        message.success(t('opsManage.deleteSuccess'))
        fetchSilenceRules()
      } catch (error) {
        message.error(handleApiError(error, t('opsManage.deleteError')))
      }
    }
  })
}

// ==================== 值班表 ====================
const oncallSchedules = ref<OnCallSchedule[]>([])
const oncallLoading = ref(false)

const shiftColumns = [
  { title: t('opsManage.shiftName'), key: 'name' },
  { title: t('opsManage.daysOfWeek'), key: 'daysOfWeek', render: (row: any) => (row.daysOfWeek || []).join(', ') },
  { title: t('opsManage.startTime'), key: 'startTime', width: 100 },
  { title: t('opsManage.endTime'), key: 'endTime', width: 100 },
  { title: t('opsManage.oncallPersons'), key: 'persons', render: (row: any) => (row.persons || []).join(', ') }
]

const fetchOncallSchedules = async () => {
  oncallLoading.value = true
  try {
    const res = await getOncallSchedules()
    oncallSchedules.value = (res as any).data || res || []
  } catch (error) {
    message.error(handleApiError(error, t('opsManage.getOncallError')))
  } finally {
    oncallLoading.value = false
  }
}

// ==================== 审计记录 ====================
const auditRecords = ref<OpsAuditRecord[]>([])
const auditLoading = ref(false)

const auditColumns = [
  { title: t('opsManage.operationType'), key: 'operationType', width: 140 },
  { title: t('opsManage.operator'), key: 'operator', width: 120 },
  { title: t('opsManage.target'), key: 'target', width: 140, ellipsis: { tooltip: true } },
  { title: t('opsManage.detail'), key: 'detail', ellipsis: { tooltip: true } },
  {
    title: t('opsManage.result'),
    key: 'success',
    width: 80,
    render: (row: OpsAuditRecord) => {
      return h(NTag, { type: row.success ? 'success' : 'error', size: 'small' }, {
        default: () => row.success ? t('opsManage.success') : t('opsManage.failure')
      })
    }
  },
  { title: t('opsManage.ipAddress'), key: 'ipAddress', width: 140 },
  { title: t('opsManage.time'), key: 'timestamp', width: 180, render: (row: OpsAuditRecord) => formatDateTime(row.timestamp) }
]

const fetchAuditRecords = async () => {
  auditLoading.value = true
  try {
    const res = await getAuditRecords()
    auditRecords.value = (res as any).data || res || []
  } catch (error) {
    message.error(handleApiError(error, t('opsManage.getAuditError')))
  } finally {
    auditLoading.value = false
  }
}

// ==================== 初始化 ====================
const refreshAll = async () => {
  refreshing.value = true
  try {
    await Promise.all([
      fetchOverview(),
      fetchAnomalies(),
      fetchAlertAggregation(),
      fetchSilenceRules(),
      fetchOncallSchedules(),
      fetchAuditRecords()
    ])
  } finally {
    refreshing.value = false
  }
}

onMounted(() => {
  refreshAll()
})
</script>

<style scoped>
/* 使用全局 page-common.css 统一样式 */

.ops-manage-page {
  padding: 16px;
}

.ops-summary {
  margin-bottom: 16px;
}

.alert-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.alert-summary-item {
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 8px;
  padding: 12px 14px;
  background: var(--bg-secondary, #fff);
}

.alert-summary-item span {
  display: block;
  color: var(--text-secondary);
  font-size: 12px;
  margin-bottom: 4px;
}

.alert-summary-item strong {
  color: var(--text-primary);
  font-size: 22px;
  line-height: 1.2;
}

:deep(.n-data-table-th) {
  background: var(--bg-tertiary) !important;
  font-weight: 600 !important;
  color: var(--text-secondary) !important;
}

:deep(.n-data-table-tr:hover .n-data-table-td) {
  background-color: var(--bg-hover) !important;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 32px;
  color: var(--text-secondary);
}

/* 值班表列表 */
.oncall-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.oncall-schedule {
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 8px;
  padding: 12px;
  background: var(--bg-secondary, #fff);
}

.oncall-schedule-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  font-weight: 600;
}

/* 响应式 */
@media (max-width: 768px) {
  .alert-summary-grid {
    grid-template-columns: 1fr;
  }
}

html.dark .alert-summary-item,
html.dark .oncall-schedule {
  background: #1e293b;
  border-color: #334155;
}
</style>

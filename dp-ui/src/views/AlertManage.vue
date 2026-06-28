<template>
  <div class="alert-manage-page">
    <!-- Page_Header_Stats: 告警统计概览 (Req 8.6) -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="24"><AlertCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.totalUnresolved || 0 }}</span>
          <span class="stat-label">{{ t('alert.unresolvedAlerts') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><NotificationsOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.totalAll || 0 }}</span>
          <span class="stat-label">{{ t('alert.totalAlerts') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><CheckmarkCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.enabledRules || 0 }}</span>
          <span class="stat-label">{{ t('alert.enabledRules') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info">
          <n-icon size="24"><ListOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.totalRules || 0 }}</span>
          <span class="stat-label">{{ t('alert.totalRules') }}</span>
        </div>
      </div>
    </div>

    <!-- Main_Card with Card_Header (Req 8.5) -->
    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <div class="header-icon-wrapper">
            <n-icon size="18"><AlertCircleOutline /></n-icon>
          </div>
          <div class="header-text">
            <span class="header-title">{{ t('alert.title') }}</span>
            <span class="header-subtitle">{{ t('alert.ruleManage') }} · {{ t('alert.recordView') }} · {{ t('alert.alertStats') }}</span>
          </div>
        </div>
      </template>

      <n-tabs v-model:value="activeTab" type="line" animated>
        <!-- 告警规则管理 Tab (Req 8.5) -->
        <n-tab-pane name="rules" :tab="t('alert.ruleManage')">
          <div class="toolbar" style="margin-bottom: 16px;">
            <n-button type="primary" @click="showCreateRule">
              <template #icon><n-icon><AddOutline /></n-icon></template>
              {{ t('alert.createRule') }}
            </n-button>
            <n-button :loading="rulesLoading" @click="loadRules">
              <template #icon><n-icon><RefreshOutline /></n-icon></template>
              {{ t('common.refresh') }}
            </n-button>
          </div>
          <n-data-table
            :columns="ruleColumns"
            :data="rules"
            :loading="rulesLoading"
            :bordered="false"
            :scroll-x="1000"
            striped
            class="custom-table"
          />
        </n-tab-pane>

        <!-- 告警记录查看 Tab (Req 8.5) -->
        <n-tab-pane name="records" :tab="t('alert.recordView')">
          <n-form class="query-form" inline style="margin-bottom: 16px;">
            <n-form-item>
              <n-select
                v-model:value="recordFilter"
                :options="recordFilterOptions"
                style="min-width: 150px"
                @update:value="loadRecords"
              />
            </n-form-item>
            <n-form-item class="query-form-actions">
              <n-button :loading="recordsLoading" @click="loadRecords">
                <template #icon><n-icon><RefreshOutline /></n-icon></template>
                {{ t('common.refresh') }}
              </n-button>
            </n-form-item>
          </n-form>
          <n-data-table
            :columns="recordColumns"
            :data="records"
            :loading="recordsLoading"
            :bordered="false"
            :scroll-x="1100"
            :pagination="false"
            striped
            class="custom-table"
          />
          <div class="pagination-wrapper">
            <div class="pagination-info">
              <n-tag type="info" size="small" round>
                {{ t('alert.totalRecords', { count: recordTotal }) }}
              </n-tag>
            </div>
            <n-pagination
              v-model:page="recordPage"
              :page-size="recordPageSize"
              :item-count="recordTotal"
              :page-sizes="[10, 20, 50]"
              show-size-picker
              show-quick-jumper
              @update:page="loadRecords"
            />
          </div>
        </n-tab-pane>

        <!-- 告警统计 Tab (Req 8.5, 8.6) -->
        <n-tab-pane name="stats" :tab="t('alert.alertStats')">
          <div class="stats-cards">
            <n-card size="small">
              <n-statistic :label="t('alert.unresolvedAlerts')" :value="stats.totalUnresolved || 0">
                <template #prefix><n-icon color="#f5222d"><AlertCircleOutline /></n-icon></template>
              </n-statistic>
            </n-card>
            <n-card size="small">
              <n-statistic :label="t('alert.totalAlerts')" :value="stats.totalAll || 0" />
            </n-card>
            <n-card size="small">
              <n-statistic :label="t('alert.enabledRules')" :value="stats.enabledRules || 0" />
            </n-card>
            <n-card size="small">
              <n-statistic :label="t('alert.totalRules')" :value="stats.totalRules || 0" />
            </n-card>
          </div>
          <n-card :title="t('alert.byLevel')" size="small" style="margin-top: 16px">
            <div v-if="stats.byLevel && stats.byLevel.length > 0">
              <div v-for="item in stats.byLevel" :key="item.alert_level" class="level-bar">
                <n-tag :type="levelTagType(item.alert_level)" size="small" style="width: 80px; text-align: center">
                  {{ levelLabel(item.alert_level) }}
                </n-tag>
                <n-progress
                  type="line"
                  :percentage="Math.min(100, (item.cnt / Math.max(stats.totalUnresolved, 1)) * 100)"
                  :color="levelColor(item.alert_level)"
                  style="flex: 1; margin: 0 12px"
                />
                <span style="min-width: 40px; text-align: right">{{ item.cnt }}</span>
              </div>
            </div>
            <n-empty v-else :description="t('alert.noAlertData')" />
          </n-card>
        </n-tab-pane>
      </n-tabs>
    </n-card>

    <!-- 创建/编辑规则对话框 -->
    <n-modal v-model:show="ruleDialogVisible" :title="ruleDialogTitle" preset="dialog" style="width: 600px; border-radius: 16px;">
      <n-form ref="ruleFormRef" :model="ruleForm" label-placement="left" label-width="100">
        <n-form-item :label="t('alert.ruleName')" path="ruleName">
          <n-input v-model:value="ruleForm.ruleName" :placeholder="t('alert.ruleNamePlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('alert.metricType')" path="metricType">
          <n-select v-model:value="ruleForm.metricType" :options="metricTypeOptions" :placeholder="t('alert.metricTypePlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('alert.metricName')" path="metricName">
          <n-input v-model:value="ruleForm.metricName" :placeholder="t('alert.metricNamePlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('alert.thresholdType')" path="thresholdType">
          <n-select v-model:value="ruleForm.thresholdType" :options="thresholdTypeOptions" :placeholder="t('alert.thresholdTypePlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('alert.threshold')" path="thresholdValue">
          <n-input-number v-model:value="ruleForm.thresholdValue" :min="0" style="width: 100%" :placeholder="t('alert.thresholdPlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('alert.alertLevel')" path="alertLevel">
          <n-select v-model:value="ruleForm.alertLevel" :options="alertLevelOptions" :placeholder="t('alert.alertLevelPlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('alert.alertMessage')" path="alertMessage">
          <n-input
v-model:value="ruleForm.alertMessage" type="textarea" :rows="2"
            :placeholder="t('alert.alertMessagePlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('alert.notificationChannels')" path="notificationChannels">
          <div style="display: flex; flex-direction: column; gap: 8px; width: 100%;">
            <n-checkbox-group v-model:value="selectedChannels">
              <n-space>
                <n-checkbox value="site" :label="t('alert.channelSite')" />
                <n-checkbox v-for="ch in availableChannels" :key="ch.channelType" :value="ch.channelType" :label="ch.channelName" />
              </n-space>
            </n-checkbox-group>
            <n-button text type="primary" size="small" style="align-self: flex-start;" @click="goToChannelManage">
              {{ t('alert.manageChannels') || '管理通知通道 →' }}
            </n-button>
          </div>
        </n-form-item>
        <n-form-item :label="t('alert.notificationUsers')" path="notificationUsers">
          <n-input v-model:value="ruleForm.notificationUsers" :placeholder="t('alert.notificationUsersPlaceholder')" />
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="ruleDialogVisible = false">{{ t('common.cancel') }}</n-button>
        <n-button type="primary" :loading="ruleSubmitting" @click="submitRule">{{ t('alert.confirm') }}</n-button>
      </template>
    </n-modal>

    <!-- 解决告警对话框 -->
    <n-modal v-model:show="resolveDialogVisible" :title="t('alert.resolveAlert')" preset="dialog" style="width: 480px; border-radius: 16px;">
      <n-form>
        <n-form-item :label="t('alert.resolveNote')">
          <n-input v-model:value="resolveNote" type="textarea" :rows="3" :placeholder="t('alert.resolveNotePlaceholder')" />
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="resolveDialogVisible = false">{{ t('common.cancel') }}</n-button>
        <n-button type="primary" :loading="resolveSubmitting" @click="confirmResolve">{{ t('alert.confirmResolve') }}</n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, h, onMounted, onUnmounted } from 'vue'
import { NButton, NIcon, NTag, NSwitch, NSpace, useMessage } from 'naive-ui'
import { useRouter } from 'vue-router'
import { AddOutline, RefreshOutline, AlertCircleOutline, NotificationsOutline, CheckmarkCircleOutline, ListOutline } from '@vicons/ionicons5'
import { alertApi } from '@/api/alert'
import type { AlertRule, AlertRecord } from '@/api/alert'
import { getMessageChannels } from '@/api/messageChannel'
import type { DataTableColumns } from 'naive-ui'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const message = useMessage()
const router = useRouter()

const activeTab = ref('rules')
let autoRefreshTimer: ReturnType<typeof setInterval> | null = null

// ==================== 告警规则 ====================
const rules = ref<AlertRule[]>([])
const rulesLoading = ref(false)
const ruleDialogVisible = ref(false)
const ruleDialogTitle = ref(t('alert.createRule'))
const ruleSubmitting = ref(false)
const editingRuleId = ref<number | null>(null)

const ruleForm = ref<any>({
  ruleName: '',
  metricType: 'cpu',
  metricName: '',
  thresholdType: 'gt',
  thresholdValue: 90,
  alertLevel: 'warning',
  alertMessage: '',
  notificationChannels: '',
  notificationUsers: ''
})

const selectedChannels = computed({
  get: () => ruleForm.value.notificationChannels ? ruleForm.value.notificationChannels.split(',').filter((s: string) => s) : [],
  set: (val: string[]) => { ruleForm.value.notificationChannels = val.join(',') }
})

// 动态加载的通知渠道
const availableChannels = ref<any[]>([])
const channelLabelMap = computed(() => {
  const map: Record<string, string> = { site: t('alert.channelSiteShort') }
  for (const ch of availableChannels.value) {
    map[ch.channelType] = ch.channelName
  }
  return map
})

const loadChannels = async () => {
  try {
    const res = await getMessageChannels() as any
    const list = res.data || []
    // 只显示已启用的渠道
    availableChannels.value = list.filter((ch: any) => ch.status === 1)
  } catch {
    // 加载失败时使用默认渠道列表
    availableChannels.value = [
      { channelType: 'email', channelName: t('alert.channelEmail') },
      { channelType: 'wecom', channelName: t('alert.channelWecom') },
      { channelType: 'dingtalk', channelName: t('alert.channelDingtalk') },
      { channelType: 'sms', channelName: t('alert.channelSms') }
    ]
  }
}

const metricTypeOptions = [
  { label: t('alert.metricCpu'), value: 'cpu' },
  { label: t('alert.metricMemory'), value: 'memory' },
  { label: t('alert.metricDisk'), value: 'disk' },
  { label: t('alert.metricJvm'), value: 'jvm' },
  { label: t('alert.metricDb'), value: 'db' },
  { label: t('alert.metricTask'), value: 'task' }
]

const thresholdTypeOptions = [
  { label: t('alert.thresholdGt'), value: 'gt' },
  { label: t('alert.thresholdGte'), value: 'gte' },
  { label: t('alert.thresholdLt'), value: 'lt' },
  { label: t('alert.thresholdLte'), value: 'lte' },
  { label: t('alert.thresholdEq'), value: 'eq' }
]

const alertLevelOptions = [
  { label: t('alert.levelInfo'), value: 'info' },
  { label: t('alert.levelWarning'), value: 'warning' },
  { label: t('alert.levelError'), value: 'error' },
  { label: t('alert.levelCritical'), value: 'critical' }
]

const ruleColumns: DataTableColumns<AlertRule> = [
  { title: t('alert.ruleName'), key: 'ruleName', width: 160 },
  {
    title: t('alert.metric'), key: 'metricType', width: 100,
    render: (row) => {
      const opt = metricTypeOptions.find(o => o.value === row.metricType)
      return opt ? opt.label : row.metricType
    }
  },
  { title: t('alert.metricName'), key: 'metricName', width: 120 },
  {
    title: t('alert.condition'), key: 'thresholdType', width: 120,
    render: (row) => {
      const op = thresholdTypeOptions.find(o => o.value === row.thresholdType)
      return `${op ? op.label.split(' ')[0] : row.thresholdType} ${row.thresholdValue}`
    }
  },
  {
    title: t('alert.level'), key: 'alertLevel', width: 80,
    render: (row) => h(NTag, { type: levelTagType(row.alertLevel), size: 'small' }, { default: () => levelLabel(row.alertLevel) })
  },
  {
    title: t('alert.notificationChannels'), key: 'notificationChannels', width: 140,
    render: (row) => {
      if (!row.notificationChannels) return '-'
      const labelMap = channelLabelMap.value
      return h(NSpace, { size: 4 }, {
        default: () => row.notificationChannels!.split(',').filter((s: string) => s).map((ch: string) =>
          h(NTag, { size: 'small', bordered: false }, { default: () => labelMap[ch] || ch })
        )
      })
    }
  },
  {
    title: t('common.status'), key: 'isEnabled', width: 80,
    render: (row) => h(NSwitch, {
      value: row.isEnabled === 1,
      onUpdateValue: () => handleToggleRule(row)
    })
  },
  {
    title: t('common.action'), key: 'actions', width: 150,
    render: (row) => h(NSpace, { size: 'small' }, {
      default: () => [
        h(NButton, { size: 'small', onClick: () => showEditRule(row) }, { default: () => t('common.edit') }),
        h(NButton, { size: 'small', type: 'error', onClick: () => handleDeleteRule(row) }, { default: () => t('common.delete') })
      ]
    })
  }
]

const loadRules = async () => {
  rulesLoading.value = true
  try {
    const res = await alertApi.getRules() as any
    rules.value = res.data || []
  } catch { /* handled by interceptor */ } finally {
    rulesLoading.value = false
  }
}

const showCreateRule = () => {
  editingRuleId.value = null
  ruleDialogTitle.value = t('alert.createRule')
  ruleForm.value = { ruleName: '', metricType: 'cpu', metricName: '', thresholdType: 'gt', thresholdValue: 90, alertLevel: 'warning', alertMessage: '', notificationChannels: '', notificationUsers: '' }
  ruleDialogVisible.value = true
}

const showEditRule = (rule: AlertRule) => {
  editingRuleId.value = rule.id!
  ruleDialogTitle.value = t('alert.editRule')
  ruleForm.value = { ...rule }
  ruleDialogVisible.value = true
}

const submitRule = async () => {
  ruleSubmitting.value = true
  try {
    if (editingRuleId.value) {
      await alertApi.updateRule(editingRuleId.value, ruleForm.value)
      message.success(t('alert.ruleUpdateSuccess'))
    } else {
      await alertApi.createRule(ruleForm.value)
      message.success(t('alert.ruleCreateSuccess'))
    }
    ruleDialogVisible.value = false
    loadRules()
    loadStats()
  } catch { /* handled */ } finally {
    ruleSubmitting.value = false
  }
}

const handleToggleRule = async (rule: AlertRule) => {
  try {
    await alertApi.toggleRule(rule.id!)
    message.success(rule.isEnabled === 1 ? t('alert.disabled') : t('alert.enabled'))
    loadRules()
    loadStats()
  } catch { /* handled */ }
}

const handleDeleteRule = async (rule: AlertRule) => {
  try {
    await alertApi.deleteRule(rule.id!)
    message.success(t('common.deleteSuccess'))
    loadRules()
    loadStats()
  } catch { /* handled */ }
}

// ==================== 告警记录 ====================
const records = ref<AlertRecord[]>([])
const recordsLoading = ref(false)
const recordPage = ref(1)
const recordPageSize = 20
const recordTotal = ref(0)
const recordFilter = ref('all')

const recordFilterOptions = [
  { label: t('alert.filterAll'), value: 'all' },
  { label: t('alert.filterUnresolved'), value: 'unresolved' },
  { label: t('alert.filterResolved'), value: 'resolved' }
]

const resolveDialogVisible = ref(false)
const resolveNote = ref('')
const resolveSubmitting = ref(false)
const resolvingRecordId = ref<number | null>(null)

const recordColumns: DataTableColumns<AlertRecord> = [
  {
    title: t('alert.time'), key: 'alertTime', width: 170,
    render: (row) => formatTime(row.alertTime)
  },
  { title: t('alert.rule'), key: 'ruleName', width: 140 },
  {
    title: t('alert.level'), key: 'alertLevel', width: 80,
    render: (row) => h(NTag, { type: levelTagType(row.alertLevel), size: 'small' }, { default: () => levelLabel(row.alertLevel) })
  },
  { title: t('alert.message'), key: 'alertMessage', ellipsis: { tooltip: true } },
  {
    title: t('alert.metricValue'), key: 'metricValue', width: 90,
    render: (row) => String(row.metricValue)
  },
  {
    title: t('common.status'), key: 'isResolved', width: 90,
    render: (row) => h(NTag, {
      type: row.isResolved ? 'success' : 'error',
      size: 'small'
    }, { default: () => row.isResolved ? t('alert.resolved') : t('alert.unresolved') })
  },
  {
    title: t('common.action'), key: 'actions', width: 100,
    render: (row) => {
      if (row.isResolved) return h('span', { style: 'color: #999; font-size: 12px' }, formatTime(row.resolveTime || ''))
      return h(NButton, { size: 'small', type: 'primary', onClick: () => showResolveDialog(row) }, { default: () => t('alert.resolve') })
    }
  }
]

const loadRecords = async () => {
  recordsLoading.value = true
  try {
    const params: any = { page: recordPage.value, size: recordPageSize }
    if (recordFilter.value !== 'all') {
      params.status = recordFilter.value
    }
    const res = await alertApi.getRecords(recordPage.value, recordPageSize) as any
    const data = res.data || res
    let list = data.list || []
    recordTotal.value = data.total || 0
    // Client-side fallback filter (until backend supports status param)
    if (recordFilter.value === 'unresolved') {
      list = list.filter((r: AlertRecord) => !r.isResolved)
    } else if (recordFilter.value === 'resolved') {
      list = list.filter((r: AlertRecord) => r.isResolved)
    }
    records.value = list
  } catch { /* handled */ } finally {
    recordsLoading.value = false
  }
}

const showResolveDialog = (record: AlertRecord) => {
  resolvingRecordId.value = record.id
  resolveNote.value = ''
  resolveDialogVisible.value = true
}

const confirmResolve = async () => {
  if (!resolvingRecordId.value) return
  resolveSubmitting.value = true
  try {
    await alertApi.resolveRecord(resolvingRecordId.value, resolveNote.value)
    message.success(t('alert.alertResolved'))
    resolveDialogVisible.value = false
    loadRecords()
    loadStats()
  } catch { /* handled */ } finally {
    resolveSubmitting.value = false
  }
}

// ==================== 告警统计 ====================
const stats = ref<any>({})

const loadStats = async () => {
  try {
    const res = await alertApi.getStats() as any
    stats.value = res.data || res || {}
  } catch { /* handled */ }
}

// ==================== 工具函数 ====================
const levelTagType = (level: string): 'info' | 'warning' | 'error' | 'success' => {
  switch (level) {
    case 'critical': return 'error'
    case 'error': return 'error'
    case 'warning': return 'warning'
    default: return 'info'
  }
}

const levelLabel = (level: string): string => {
  switch (level) {
    case 'critical': return t('alert.levelCritical')
    case 'error': return t('alert.levelError')
    case 'warning': return t('alert.levelWarning')
    default: return t('alert.levelInfo')
  }
}

const levelColor = (level: string): string => {
  switch (level) {
    case 'critical': return '#f5222d'
    case 'error': return '#ff4d4f'
    case 'warning': return '#faad14'
    default: return '#1890ff'
  }
}

const formatTime = (t: string): string => {
  if (!t) return '-'
  try {
    return new Date(t).toLocaleString('zh-CN', { hour12: false })
  } catch {
    return t
  }
}

// 跳转到通道管理
const goToChannelManage = () => {
  router.push('/message-channel')
}

// ==================== 自动刷新（告警监控） ====================
const startAutoRefresh = () => {
  stopAutoRefresh()
  autoRefreshTimer = setInterval(() => {
    if (activeTab.value === 'records') {
      loadRecords()
      loadStats()
    }
  }, 30000) // 每30秒刷新一次
}

const stopAutoRefresh = () => {
  if (autoRefreshTimer) {
    clearInterval(autoRefreshTimer)
    autoRefreshTimer = null
  }
}

// ==================== 初始化 ====================
onMounted(() => {
  loadRules()
  loadRecords()
  loadStats()
  loadChannels()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.alert-manage-page {
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
  background: linear-gradient(135deg, #f59e0b 0%, #f97316 100%);
  color: #fff;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.25);
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

.main-card {
  border-radius: var(--dp-card-radius);
}

.toolbar {
  display: flex;
  gap: 8px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.level-bar {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

@media (max-width: 768px) {
  .page-header-stats { flex-direction: row; overflow-x: auto; flex-wrap: nowrap; }
  .page-header-stats .stat-item { min-width: 140px; flex: 0 0 auto; }
  .stats-cards { grid-template-columns: 1fr; }
  .level-bar { flex-wrap: wrap; gap: 6px; }
  .main-card { border-radius: 14px !important; }
}
</style>

<style>
/* AlertManage 深色模式（非 scoped） */
html.dark .alert-manage-page .header-icon-wrapper { box-shadow: 0 2px 8px rgba(245, 158, 11, 0.15) !important; }
html.dark .alert-manage-page .header-title { color: #f1f5f9 !important; }
html.dark .alert-manage-page .header-subtitle { color: #64748b !important; }
html.dark .alert-manage-page .stats-cards .n-card { background: #1e293b !important; border-color: #334155 !important; }
html.dark .alert-manage-page .level-bar { color: #e2e8f0 !important; }
</style>

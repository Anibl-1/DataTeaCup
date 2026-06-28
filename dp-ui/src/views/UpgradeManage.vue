<template>
  <div class="upgrade-page">
    <!-- 页面头部统计 -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><CloudUploadOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ currentVersion }}</span>
          <span class="stat-label">{{ t('upgrade.currentVersion') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon" :class="versionInfo?.updateAvailable ? 'stat-icon-success' : 'stat-icon-muted'">
          <n-icon size="24"><ArrowUpCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ versionInfo?.latestVersion || '-' }}</span>
          <span class="stat-label">{{ t('upgrade.latestVersion') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="24"><TimeOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ historyList.length }}</span>
          <span class="stat-label">{{ t('upgrade.upgradeRecords') }}</span>
        </div>
      </div>
    </div>

    <!-- 新版本信息 -->
    <n-alert
      v-if="versionInfo?.updateAvailable"
      type="success"
      :title="t('upgrade.newVersionFound', { version: versionInfo.latestVersion })"
      style="margin-bottom: 16px;"
      closable
    >
      <div style="white-space: pre-line;">{{ versionInfo.releaseNotes || t('upgrade.noReleaseNotes') }}</div>
      <template #action>
        <n-space>
          <n-button size="small" type="primary" :loading="upgradeLoading" @click="handleUpgrade">
            {{ t('upgrade.performUpgrade') }}
          </n-button>
        </n-space>
      </template>
    </n-alert>

    <!-- 升级进度 -->
    <n-card v-if="upgradeLoading" style="margin-bottom: 16px;">
      <div style="text-align: center; padding: 16px 0;">
        <p style="margin-bottom: 12px; font-weight: 500;">{{ t('upgrade.upgrading') }}</p>
        <n-progress type="line" :percentage="upgradeProgress" :indicator-placement="'inside'" processing />
      </div>
    </n-card>

    <!-- 操作区 -->
    <n-card style="margin-bottom: 16px;">
      <template #header>
        <div class="card-header-custom">
          <div class="card-title">
            <n-icon size="22" color="var(--color-primary)"><CloudUploadOutline /></n-icon>
            <span>{{ t('upgrade.upgradeOperations') }}</span>
          </div>
        </div>
      </template>
      <template #header-extra>
        <n-button quaternary type="info" @click="showGuideModal = true">
          <template #icon><n-icon><InformationCircleOutline /></n-icon></template>
          {{ t('upgrade.operationGuide') }}
        </n-button>
      </template>
      <n-space>
        <n-button type="primary" :loading="checkLoading" @click="handleCheckUpdate">
          <template #icon><n-icon><RefreshOutline /></n-icon></template>
          {{ t('upgrade.checkUpdate') }}
        </n-button>
        <n-upload
          :max="1"
          accept=".zip,.tar.gz,.jar,.war"
          :show-file-list="false"
          :custom-request="handleUploadPackage"
        >
          <n-button>
            <template #icon><n-icon><CloudUploadOutline /></n-icon></template>
            {{ t('upgrade.uploadPackage') }}
          </n-button>
        </n-upload>
        <n-button :loading="backupLoading" @click="handleCreateBackup">
          <template #icon><n-icon><SaveOutline /></n-icon></template>
          {{ t('upgrade.createBackup') }}
        </n-button>
        <n-button
          :disabled="historyList.length === 0"
          @click="showRollbackModal = true"
        >
          <template #icon><n-icon><ArrowUndoOutline /></n-icon></template>
          {{ t('upgrade.rollbackOperation') }}
        </n-button>
      </n-space>
    </n-card>

    <!-- 升级历史 -->
    <n-card>
      <template #header>
        <div class="card-header-custom">
          <div class="card-title">
            <n-icon size="22" color="var(--color-primary)"><TimeOutline /></n-icon>
            <span>{{ t('upgrade.upgradeHistory') }}</span>
          </div>
        </div>
      </template>

      <n-data-table
        :columns="columns"
        :data="historyList"
        :loading="historyLoading"
        :pagination="false"
        :scroll-x="1000"
        striped
        class="custom-table"
      />
    </n-card>

    <!-- 回滚弹窗 -->
    <n-modal v-model:show="showRollbackModal" preset="card" :title="t('upgrade.rollbackModal')" style="width: 480px; border-radius: 16px;">
      <n-form label-placement="left" label-width="100px">
        <n-form-item :label="t('upgrade.targetVersion')">
          <n-input v-model:value="rollbackVersion" :placeholder="t('upgrade.enterVersion')" />
        </n-form-item>
        <n-form-item>
          <n-space style="width: 100%; justify-content: flex-end;">
            <n-button @click="showRollbackModal = false">{{ t('common.cancel') }}</n-button>
            <n-button type="warning" :loading="rollbackLoading" @click="handleRollback">{{ t('upgrade.confirmRollback') }}</n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-modal>

    <!-- 操作说明弹窗 -->
    <n-modal v-model:show="showGuideModal" preset="card" :title="t('upgrade.guideTitle')" style="width: 800px; border-radius: 16px;">
      <div class="guide-content">
        <n-alert type="warning" style="margin-bottom: 16px;">
          {{ t('upgrade.guideWarning') }}
        </n-alert>

        <n-timeline>
          <n-timeline-item type="info" :title="t('upgrade.step1Title')">
            <ul class="guide-list">
              <li>{{ t('upgrade.step1_1') }}</li>
              <li>{{ t('upgrade.step1_2') }}</li>
              <li>{{ t('upgrade.step1_3') }}</li>
            </ul>
          </n-timeline-item>
          <n-timeline-item type="info" :title="t('upgrade.step2Title')">
            <ul class="guide-list">
              <li>{{ t('upgrade.step2_1') }}</li>
              <li>{{ t('upgrade.step2_2') }}</li>
              <li>{{ t('upgrade.step2_3') }}</li>
            </ul>
          </n-timeline-item>
          <n-timeline-item type="info" :title="t('upgrade.step3Title')">
            <ul class="guide-list">
              <li>{{ t('upgrade.step3_1') }}</li>
              <li>{{ t('upgrade.step3_2') }}</li>
              <li>{{ t('upgrade.step3_3') }}</li>
            </ul>
          </n-timeline-item>
          <n-timeline-item type="success" :title="t('upgrade.step4Title')">
            <ul class="guide-list">
              <li>{{ t('upgrade.step4_1') }}</li>
              <li>{{ t('upgrade.step4_2') }}</li>
              <li>{{ t('upgrade.step4_3') }}</li>
            </ul>
          </n-timeline-item>
          <n-timeline-item type="success" :title="t('upgrade.step5Title')">
            <ul class="guide-list">
              <li>{{ t('upgrade.step5_1') }}</li>
              <li>{{ t('upgrade.step5_2') }}</li>
              <li>{{ t('upgrade.step5_3') }}</li>
            </ul>
          </n-timeline-item>
          <n-timeline-item type="warning" :title="t('upgrade.step6Title')">
            <ul class="guide-list">
              <li>{{ t('upgrade.step6_1') }}</li>
              <li>{{ t('upgrade.step6_2') }}</li>
              <li>{{ t('upgrade.step6_3') }}</li>
              <li>{{ t('upgrade.step6_4') }}</li>
            </ul>
          </n-timeline-item>
        </n-timeline>

        <n-divider />
        <n-text depth="3" style="font-size: 13px;">
          {{ t('upgrade.contactSupport') }}
        </n-text>
      </div>
      <template #action>
        <n-button @click="showGuideModal = false">{{ t('upgrade.iKnow') }}</n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, onMounted, h } from 'vue'
import { NTag, NIcon, useMessage, useDialog } from 'naive-ui'
import {
  CloudUploadOutline,
  ArrowUpCircleOutline,
  TimeOutline,
  RefreshOutline,
  SaveOutline,
  ArrowUndoOutline,
  InformationCircleOutline
} from '@vicons/ionicons5'
import {
  checkUpdate,
  getCurrentVersion,
  performUpgrade,
  rollback,
  getHistory,
  createBackup,
  type VersionInfo,
  type UpgradeRecord
} from '@/api/upgrade'
import { formatDateTime } from '@/utils/format'
import { handleApiError } from '@/utils/error'
import { initMessage } from '@/utils/message'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const message = useMessage()
const dialog = useDialog()
initMessage(message)

// 状态
const currentVersion = ref('-')
const versionInfo = ref<VersionInfo | null>(null)
const historyList = ref<UpgradeRecord[]>([])
const checkLoading = ref(false)
const upgradeLoading = ref(false)
const upgradeProgress = ref(0)
const backupLoading = ref(false)
const rollbackLoading = ref(false)
const historyLoading = ref(false)
const showRollbackModal = ref(false)
const rollbackVersion = ref('')
const showGuideModal = ref(false)

// 表格列
const columns = [
  { title: t('upgrade.fromVersion'), key: 'fromVersion', width: 120 },
  { title: t('upgrade.toVersion'), key: 'toVersion', width: 120 },
  {
    title: t('upgrade.type'),
    key: 'type',
    width: 100,
    render: (row: UpgradeRecord) => {
      const typeMap: Record<string, { label: string; type: 'info' | 'warning' | 'success' }> = {
        upgrade: { label: t('upgrade.typeUpgrade'), type: 'info' },
        rollback: { label: t('upgrade.typeRollback'), type: 'warning' },
        hotfix: { label: t('upgrade.typeHotfix'), type: 'success' }
      }
      const typeInfo = typeMap[row.type] || { label: row.type, type: 'info' as const }
      return h(NTag, { type: typeInfo.type, size: 'small' }, { default: () => typeInfo.label })
    }
  },
  {
    title: t('upgrade.result'),
    key: 'success',
    width: 80,
    render: (row: UpgradeRecord) => {
      return h(NTag, { type: row.success ? 'success' : 'error', size: 'small' }, {
        default: () => row.success ? t('upgrade.resultSuccess') : t('upgrade.resultFailed')
      })
    }
  },
  { title: t('upgrade.note'), key: 'message', ellipsis: { tooltip: true } },
  {
    title: t('upgrade.time'),
    key: 'timestamp',
    width: 180,
    render: (row: UpgradeRecord) => formatDateTime(row.timestamp)
  }
]

// 获取当前版本
const fetchCurrentVersion = async () => {
  try {
    const res = await getCurrentVersion()
    currentVersion.value = (res as any).data || res || '-'
  } catch (error) {
    console.error('fetchCurrentVersion failed:', error)
  }
}

// 获取升级历史
const fetchHistory = async () => {
  historyLoading.value = true
  try {
    const res = await getHistory()
    historyList.value = (res as any).data || res || []
  } catch (error) {
    message.error(handleApiError(error, t('upgrade.fetchHistoryFailed')))
  } finally {
    historyLoading.value = false
  }
}

// 检查更新
const handleCheckUpdate = async () => {
  checkLoading.value = true
  try {
    const res = await checkUpdate()
    versionInfo.value = (res as any).data || res
    if (versionInfo.value?.updateAvailable) {
      message.success(t('upgrade.newVersionMsg', { version: versionInfo.value.latestVersion }))
    } else {
      message.info(t('upgrade.alreadyLatest'))
    }
  } catch (error) {
    message.error(handleApiError(error, t('upgrade.checkUpdateFailed')))
  } finally {
    checkLoading.value = false
  }
}

// 执行升级
const handleUpgrade = () => {
  if (!versionInfo.value) return
  const targetVersion = versionInfo.value.latestVersion
  dialog.warning({
    title: t('upgrade.confirmUpgrade'),
    content: t('upgrade.confirmUpgradeContent', { version: targetVersion }),
    positiveText: t('upgrade.confirmUpgradeBtn'),
    negativeText: t('common.cancel'),
    onPositiveClick: async () => {
      upgradeLoading.value = true
      upgradeProgress.value = 0
      // 模拟进度
      const timer = setInterval(() => {
        if (upgradeProgress.value < 90) {
          upgradeProgress.value += Math.random() * 15
        }
      }, 500)
      try {
        const res = await performUpgrade(targetVersion)
        const result = (res as any).data || res
        upgradeProgress.value = 100
        if (result?.success) {
          message.success(t('upgrade.upgradeSuccess'))
          versionInfo.value = null
          fetchCurrentVersion()
          fetchHistory()
        } else {
          message.error(result?.message || t('upgrade.upgradeFailed'))
        }
      } catch (error) {
        message.error(handleApiError(error, t('upgrade.performUpgradeFailed')))
      } finally {
        clearInterval(timer)
        upgradeLoading.value = false
        upgradeProgress.value = 0
      }
    }
  })
}

// 创建备份
const handleCreateBackup = async () => {
  backupLoading.value = true
  try {
    await createBackup()
    message.success(t('upgrade.backupSuccess'))
  } catch (error) {
    message.error(handleApiError(error, t('upgrade.createBackupFailed')))
  } finally {
    backupLoading.value = false
  }
}

// 上传升级包
const handleUploadPackage = async ({ file }: any) => {
  if (!file.file) return
  const maxSize = 500 * 1024 * 1024 // 500MB
  if (file.file.size > maxSize) {
    message.error(t('upgrade.packageTooLarge'))
    return
  }
  const formData = new FormData()
  formData.append('file', file.file)
  try {
    message.loading(t('upgrade.uploadingPackage'))
    const { default: request } = await import('@/api/request')
    const res = await request.post('/upgrade/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 600000 // 10分钟超时
    })
    const data = (res as any).data || res
    message.success(data?.version ? t('upgrade.uploadSuccessVersion', { version: data.version }) : t('upgrade.uploadSuccess'))
    handleCheckUpdate()
  } catch (error) {
    message.error(handleApiError(error, t('upgrade.uploadPackageFailed')))
  }
}

// 回滚
const handleRollback = () => {
  if (!rollbackVersion.value.trim()) {
    message.warning(t('upgrade.enterVersionWarning'))
    return
  }
  dialog.warning({
    title: t('upgrade.confirmRollbackTitle'),
    content: t('upgrade.rollbackContent', { version: rollbackVersion.value }),
    positiveText: t('upgrade.confirmRollback'),
    negativeText: t('common.cancel'),
    onPositiveClick: async () => {
      rollbackLoading.value = true
      try {
        const res = await rollback(rollbackVersion.value)
        const result = (res as any).data || res
        if (result?.success) {
          message.success(t('upgrade.rollbackSuccess'))
          showRollbackModal.value = false
          rollbackVersion.value = ''
          fetchCurrentVersion()
          fetchHistory()
        } else {
          message.error(result?.message || t('upgrade.rollbackFailed'))
        }
      } catch (error) {
        message.error(handleApiError(error, t('upgrade.rollbackOperationFailed')))
      } finally {
        rollbackLoading.value = false
      }
    }
  })
}

onMounted(() => {
  fetchCurrentVersion()
  fetchHistory()
})
</script>

<style scoped>
/* 使用全局 page-common.css 统一样式 */

.upgrade-page {
  padding: 16px;
}

:deep(.n-data-table-th) {
  background: var(--bg-tertiary) !important;
  font-weight: 600 !important;
  color: var(--text-secondary) !important;
}

:deep(.n-data-table-tr:hover .n-data-table-td) {
  background-color: var(--bg-hover) !important;
}

/* 操作说明 */
.guide-content {
  max-height: 60vh;
  overflow-y: auto;
}

.guide-list {
  margin: 4px 0 0 0;
  padding-left: 18px;
  line-height: 1.8;
  color: var(--text-secondary);
  font-size: 13px;
}

.guide-list li {
  margin-bottom: 2px;
}

.guide-list b {
  color: var(--text-primary);
}
</style>

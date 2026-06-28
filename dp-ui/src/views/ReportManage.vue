<template>
  <div class="report-manage-page">
    <!-- 页面头部统计 -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><DocumentTextOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ paginationItemCount }}</span>
          <span class="stat-label">{{ t('reportManage.totalReports') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><CheckmarkCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ enabledCount }}</span>
          <span class="stat-label">{{ t('reportManage.enabledReports') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info">
          <n-icon size="24"><AnalyticsOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ tableData.length }}</span>
          <span class="stat-label">{{ t('reportManage.currentPage') }}</span>
        </div>
      </div>
    </div>

    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <div class="header-icon-wrapper">
            <n-icon size="18"><DocumentOutline /></n-icon>
          </div>
          <div class="header-text">
            <span class="header-title">{{ t('reportManage.title') }}</span>
            <span class="header-subtitle">{{ t('reportManage.searchPlaceholder') }}</span>
          </div>
        </div>
      </template>
      <template #header-extra>
        <n-space :size="8">
          <n-button secondary @click="showMobilePublish = true">
            <template #icon><n-icon><PhonePortraitOutline /></n-icon></template>
            {{ t('reportManage.mobilePublish') }}
          </n-button>
          <n-button type="primary" @click="handleCreate">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            {{ t('reportManage.newReport') }}
          </n-button>
        </n-space>
      </template>

      <div class="report-toolbar">
        <n-space :size="8" align="center">
          <FilterPanel
            :fields="filterFields"
            :model-value="activeFilters"
            @apply="handleFilterApply"
          />
          <n-input
            v-model:value="searchKeyword"
            :placeholder="t('reportManage.searchPlaceholder')"
            clearable
            style="width: 220px"
            size="small"
            @keydown.enter="handleSearch"
          >
            <template #prefix><n-icon><SearchOutline /></n-icon></template>
          </n-input>
          <n-button size="small" @click="handleSearch">{{ t('common.search') }}</n-button>
        </n-space>
        <n-space v-if="checkedRowKeys.length > 0" :size="6" align="center">
          <n-tag type="info" size="small">{{ t('reportManage.selected', { count: checkedRowKeys.length }) }}</n-tag>
          <n-button size="small" type="success" @click="handleBatchEnable">{{ t('reportManage.batchEnable') }}</n-button>
          <n-button size="small" type="warning" @click="handleBatchDisable">{{ t('reportManage.batchDisable') }}</n-button>
          <n-popconfirm @positive-click="handleBatchDelete">
            <template #trigger>
              <n-button size="small" type="error">{{ t('reportManage.batchDelete') }}</n-button>
            </template>
            {{ t('reportManage.batchDeleteConfirm', { count: checkedRowKeys.length }) }}
          </n-popconfirm>
          <n-button size="small" quaternary @click="checkedRowKeys = []">{{ t('reportManage.cancelSelect') }}</n-button>
        </n-space>
      </div>
      <n-data-table
        v-model:checked-row-keys="checkedRowKeys"
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="false"
        :scroll-x="1200"
        :row-key="(row: ReportDefinition) => row.id"
        striped
        class="custom-table"
      >
        <template #empty>
          <div class="empty-state-wrapper">
            <div class="empty-state-icon">
              <n-icon size="48"><DocumentTextOutline /></n-icon>
            </div>
            <div class="empty-state-title">{{ t('common.noData') }}</div>
            <div class="empty-state-desc">创建报表以可视化展示您的数据分析结果</div>
            <n-button type="primary" size="large" style="margin-top: 16px;" @click="handleCreate">
              <template #icon><n-icon><AddOutline /></n-icon></template>
              {{ t('reportManage.newReport') }}
            </n-button>
          </div>
        </template>
      </n-data-table>
      <div class="pagination-wrapper">
        <div class="pagination-info">
          <n-tag type="info" size="small" round>
            {{ t('reportManage.totalRecords', { count: paginationItemCount }) }}
          </n-tag>
        </div>
        <n-pagination
          v-model:page="paginationPage"
          v-model:page-size="paginationPageSize"
          :item-count="paginationItemCount"
          :page-sizes="PAGE_SIZES"
          show-size-picker
          show-quick-jumper
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-card>
    
    <!-- 分享弹窗 -->
    <n-modal v-model:show="showShareModal" preset="card" :title="t('reportManage.shareTitle')" style="width: 500px">
      <n-form label-placement="left" label-width="100px">
        <n-form-item :label="t('chartManage.sharePassword')">
          <n-input v-model:value="shareForm.password" :placeholder="t('chartManage.sharePasswordHint')" />
        </n-form-item>
        <n-form-item :label="t('chartManage.shareExpireHours')">
          <n-input-number v-model:value="shareForm.expireHours" :min="0" :max="720" :placeholder="t('chartManage.shareExpireHint')" style="width: 100%" />
        </n-form-item>
        <n-form-item :label="t('chartManage.shareMaxAccess')">
          <n-input-number v-model:value="shareForm.maxAccessCount" :min="0" :max="10000" :placeholder="t('chartManage.shareMaxAccessHint')" style="width: 100%" />
        </n-form-item>
      </n-form>
      <div v-if="shareResult" class="share-result-box">
        <div class="share-result-title">✅ {{ t('chartManage.shareLinkGenerated') }}</div>
        <n-input :value="shareLink" readonly>
          <template #suffix>
            <n-button size="small" text @click="copyShareLink">{{ t('common.copy') }}</n-button>
          </template>
        </n-input>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showShareModal = false">{{ t('common.close') }}</n-button>
          <n-button type="primary" :loading="creatingShare" @click="handleCreateShare">{{ t('chartManage.generateShareLink') }}</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 订阅弹窗 -->
    <n-modal v-model:show="showSubscribeModal" preset="card" :title="t('reportManage.subscribeTitle')" style="width: 550px">
      <n-form label-placement="left" label-width="100px">
        <n-form-item :label="t('reportManage.subscribeType')">
          <n-select v-model:value="subscribeForm.type" :options="[{ label: t('reportManage.emailNotify'), value: 'email' }, { label: t('reportManage.inAppNotify'), value: 'message' }]" />
        </n-form-item>
        <n-form-item :label="t('reportManage.notifyFrequency')">
          <n-select v-model:value="subscribeForm.frequency" :options="[{ label: t('reportManage.daily'), value: 'daily' }, { label: t('reportManage.weekly'), value: 'weekly' }, { label: t('reportManage.monthly'), value: 'monthly' }]" />
        </n-form-item>
        <n-form-item v-if="subscribeForm.type === 'email'" :label="t('reportManage.emailAddress')">
          <n-input v-model:value="subscribeForm.email" :placeholder="t('reportManage.emailPlaceholder')" />
        </n-form-item>
      </n-form>
      <div v-if="reportSubscriptions.length > 0" style="margin-top: 16px">
        <n-text depth="2" style="font-size: 13px; font-weight: 600; margin-bottom: 8px; display: block">{{ t('reportManage.existingSubscriptions') }}</n-text>
        <n-list bordered size="small">
          <n-list-item v-for="sub in reportSubscriptions" :key="sub.id">
            <n-thing :title="sub.type === 'email' ? t('reportManage.emailSubscription') : t('reportManage.inAppNotify')" :description="`${t('reportManage.notifyFrequency')}: ${sub.frequency} | ${sub.email || ''}`">
              <template #header-extra>
                <n-button size="tiny" type="error" text @click="handleUnsubscribe(sub.id)">{{ t('common.cancel') }}</n-button>
              </template>
            </n-thing>
          </n-list-item>
        </n-list>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showSubscribeModal = false">{{ t('common.close') }}</n-button>
          <n-button type="primary" :loading="subscribing" @click="handleSubscribe">{{ t('reportManage.subscribe') }}</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 创建菜单对话框 -->
    <n-modal v-model:show="showMenuModal" preset="card" :title="t('reportManage.createMenuTitle')" style="width: 600px; border-radius: 16px;">
      <n-form
        ref="menuFormRef"
        :model="menuForm"
        :rules="menuRules"
        label-placement="left"
        label-width="100px"
      >
        <n-form-item :label="t('chartManage.menuName')" path="menuName">
          <n-input v-model:value="menuForm.menuName" :placeholder="t('chartManage.menuNamePlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('chartManage.parentMenu')">
          <n-tree-select
            v-model:value="menuForm.parentId"
            :options="menuTreeOptions"
            :placeholder="t('chartManage.parentMenuPlaceholder')"
            clearable
            filterable
          />
        </n-form-item>
        <n-form-item :label="t('chartManage.icon')">
          <div class="icon-picker-container">
            <div v-if="menuForm.icon" class="icon-preview-bar">
              <n-icon :component="getIconComponent(menuForm.icon)" size="24" />
              <span>{{ t('chartManage.iconSelected') }}: {{ getIconLabel(menuForm.icon) }}</span>
              <n-button size="tiny" quaternary style="margin-left: auto;" @click="menuForm.icon = ''">{{ t('common.clear') }}</n-button>
            </div>
            <n-input
              v-model:value="iconSearchKeyword"
              :placeholder="t('chartManage.searchIcon')"
              clearable
              style="margin-bottom: 12px;"
            >
              <template #prefix>
                <n-icon><SearchOutline /></n-icon>
              </template>
            </n-input>
            <n-tabs v-model:value="iconCategory" type="segment" size="small" style="margin-bottom: 12px;">
              <n-tab-pane v-for="category in iconCategories" :key="category.key" :name="category.key" :tab="category.label" />
            </n-tabs>
            <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(90px, 1fr)); gap: 12px;">
              <div
                v-for="iconOption in filteredIcons"
                :key="iconOption.value"
                class="icon-select-item"
                :class="{ 'icon-selected': menuForm.icon === iconOption.value }"
                @click="menuForm.icon = iconOption.value"
              >
                <n-icon :component="getIconComponent(iconOption.value)" size="28" />
                <span style="font-size: 11px; text-align: center; word-break: break-word; line-height: 1.2;">{{ iconOption.label }}</span>
              </div>
            </div>
            <div v-if="filteredIcons.length === 0" class="icon-empty-hint">
              {{ t('chartManage.noIconFound') }}
            </div>
          </div>
        </n-form-item>
        <n-form-item :label="t('chartManage.sortOrder')">
          <n-input-number v-model:value="menuForm.sortOrder" :min="0" style="width: 100%" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showMenuModal = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="creatingMenu" @click="handleCreateMenu">{{ t('common.create') }}</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 定时推送配置对话框 -->
    <n-modal v-model:show="showScheduleModal" preset="card" :title="t('reportManage.scheduleTitle')" style="width: 800px; border-radius: 16px;">
      <n-form label-placement="left" label-width="100px">
        <n-form-item :label="t('reportManage.scheduleName')">
          <n-input v-model:value="scheduleForm.scheduleName" :placeholder="t('reportManage.scheduleNamePlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('reportManage.cronExpression')">
          <CronEditor v-model="scheduleForm.cronExpression" />
        </n-form-item>
        <n-form-item :label="t('reportManage.recipients')">
          <n-input v-model:value="scheduleForm.recipients" type="textarea" :rows="2" :placeholder="t('reportManage.recipientsPlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('reportManage.pushChannel')">
          <n-checkbox-group v-model:value="scheduleChannels">
            <n-space>
              <n-checkbox value="email" :label="t('reportManage.email')" />
              <n-checkbox value="wecom" :label="t('reportManage.wecom')" />
              <n-checkbox value="dingtalk" :label="t('reportManage.dingtalk')" />
            </n-space>
          </n-checkbox-group>
        </n-form-item>
        <n-form-item v-if="scheduleChannels.includes('email')" :label="t('reportManage.emailChannel')">
          <n-select v-model:value="scheduleForm.emailChannelId" :options="emailChannelOptions" :placeholder="t('reportManage.useDefault')" clearable />
        </n-form-item>
        <n-form-item v-if="scheduleChannels.includes('wecom')" :label="t('reportManage.wecomChannel')">
          <n-select v-model:value="scheduleForm.wecomChannelId" :options="wecomChannelOptions" :placeholder="t('reportManage.useDefault')" clearable />
        </n-form-item>
        <n-form-item v-if="scheduleChannels.includes('dingtalk')" :label="t('reportManage.dingtalkChannel')">
          <n-select v-model:value="scheduleForm.dingtalkChannelId" :options="dingtalkChannelOptions" :placeholder="t('reportManage.useDefault')" clearable />
        </n-form-item>
        <n-form-item :label="t('reportManage.attachFile')">
          <n-switch v-model:value="scheduleAttachExcel" />
        </n-form-item>
        <n-form-item v-if="scheduleAttachExcel" :label="t('reportManage.attachFormat')">
          <n-radio-group v-model:value="scheduleForm.attachFormat">
            <n-space>
              <n-radio value="excel">Excel</n-radio>
              <n-radio value="pdf">PDF</n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>

        <n-divider style="margin: 12px 0;">
          <n-text depth="3" style="font-size: 13px;">{{ t('reportManage.reportParamsConfig') }}</n-text>
        </n-divider>

        <n-spin :show="reportParamsLoading" size="small">
          <div v-if="reportParamRows.length > 0">
            <div v-for="(row, idx) in reportParamRows" :key="idx" style="display: flex; gap: 8px; margin-bottom: 10px; align-items: center;">
              <n-tag size="small" :bordered="false" style="min-width: 100px; justify-content: center;">
                {{ row.label || row.name }}
              </n-tag>
              <!-- 日期类型：可选日期函数或固定值 -->
              <template v-if="row.inputType === 'date' || row.inputType === 'daterange'">
                <n-select
                  v-model:value="row.valueType"
                  :options="[{ label: t('reportManage.dateFunction'), value: 'function' }, { label: t('reportManage.fixedValue'), value: 'fixed' }]"
                  style="width: 110px;"
                />
                <n-select
                  v-if="row.valueType === 'function'"
                  v-model:value="row.value"
                  :options="dateFunctionOptions"
                  :placeholder="t('reportManage.selectDateFunction')"
                  style="flex: 1;"
                  filterable
                />
                <n-input
                  v-else
                  v-model:value="row.value"
                  placeholder="yyyy-MM-dd"
                  style="flex: 1;"
                />
              </template>
              <!-- 下拉选择类型 -->
              <n-select
                v-else-if="row.inputType === 'select' && row.options && row.options.length > 0"
                v-model:value="row.value"
                :options="row.options"
                :placeholder="t('reportManage.selectValue')"
                style="flex: 1;"
                filterable
                clearable
              />
              <!-- 其他类型：文本输入 -->
              <n-input
                v-else
                v-model:value="row.value"
                :placeholder="t('reportManage.inputValueFor', { name: row.label || row.name })"
                style="flex: 1;"
              />
              <n-tag v-if="row.required" type="error" size="tiny" :bordered="false">{{ t('reportManage.required') }}</n-tag>
            </div>
          </div>
          <n-empty v-else-if="!reportParamsLoading" :description="t('reportManage.noParams')" size="small" style="padding: 8px 0;" />
        </n-spin>
      </n-form>

      <!-- 已有推送任务列表 -->
      <div v-if="existingSchedules.length > 0" style="margin-top: 12px">
        <n-text strong style="margin-bottom: 8px; display: block">{{ t('reportManage.configuredTasks') }}</n-text>
        <n-list bordered size="small">
          <n-list-item v-for="s in existingSchedules" :key="s.id">
            <n-thing :title="s.scheduleName" :description="`Cron: ${s.cronExpression} | ${t('reportManage.recipients')}: ${s.recipients} | ${t('reportManage.attachFormat')}: ${(s.attachFormat || 'excel').toUpperCase()}`">
              <template #header-extra>
                <n-space size="small">
                  <n-tag :type="s.isEnabled === 1 ? 'success' : 'default'" size="small">
                    {{ s.isEnabled === 1 ? t('chart.enabled') : t('chart.disabled') }}
                  </n-tag>
                  <n-tag v-if="s.attachFormat" size="small" :bordered="false">
                    {{ (s.attachFormat || 'excel').toUpperCase() }}
                  </n-tag>
                  <n-tag v-if="s.lastRunStatus" :type="s.lastRunStatus === 'success' ? 'success' : 'error'" size="small">
                    {{ s.lastRunStatus === 'success' ? t('reportManage.lastSuccess') : t('reportManage.lastFailed') }}
                  </n-tag>
                  <n-button size="tiny" type="info" @click="triggerSchedule(s.id!)">{{ t('reportManage.triggerNow') }}</n-button>
                  <n-button size="tiny" type="error" @click="deleteSchedule(s.id!)">{{ t('common.delete') }}</n-button>
                </n-space>
              </template>
            </n-thing>
          </n-list-item>
        </n-list>
      </div>

      <template #footer>
        <n-space justify="end">
          <n-button @click="showScheduleModal = false">{{ t('common.close') }}</n-button>
          <n-button type="primary" :loading="scheduleSaving" @click="saveSchedule">{{ t('reportManage.saveTask') }}</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 移动端发布对话框 -->
    <MobilePublishDialog
      v-model:show="showMobilePublish"
:title="t('reportManage.mobilePublishTitle')"
      :items="mobilePublishItems"
      @toggle="handleMobileToggle"
      @batch-publish="handleMobileBatchPublish"
      @batch-unpublish="handleMobileBatchUnpublish"
    />
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, onMounted, h, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NTag, NSpace, NIcon, NTabs, NTabPane, NPopconfirm, useMessage } from 'naive-ui'
import type { FormInst } from 'naive-ui'
import { useTabsStore } from '@/stores/tabs'
import { useDataTable } from '@/composables/useDataTable'
import StatusTag from '@/components/common/StatusTag.vue'
import ActionButtons, { type ActionConfig } from '@/components/common/ActionButtons.vue'
import {
  AddOutline,
  SearchOutline,
  DocumentTextOutline,
  AnalyticsOutline,
  CheckmarkCircleOutline,
  // 图标导入（与MenuManage.vue保持一致）
  GridOutline,
  HomeOutline,
  SettingsOutline,
  PersonOutline,
  PeopleOutline,
  FolderOutline,
  DocumentOutline,
  AppsOutline,
  MenuOutline,
  NavigateOutline,
  CompassOutline,
  MapOutline,
  LocationOutline,
  EarthOutline,
  GlobeOutline,
  FlagOutline,
  ServerOutline,
  LibraryOutline,
  StatsChartOutline,
  BarChartOutline,
  PieChartOutline,
  TrendingUpOutline,
  TrendingDownOutline,
  PulseOutline,
  SpeedometerOutline,
  DownloadOutline,
  CloudUploadOutline,
  SyncOutline,
  RefreshOutline,
  ReloadOutline,
  CloudOutline,
  CloudDownloadOutline,
  DocumentsOutline,
  FolderOpenOutline,
  FileTrayOutline,
  ArchiveOutline,
  ClipboardOutline,
  SaveOutline,
  PrintOutline,
  CopyOutline,
  CutOutline,
  TrashBinOutline,
  PlayOutline,
  PauseOutline,
  StopOutline,
  PlaySkipForwardOutline,
  PlaySkipBackOutline,
  VolumeHighOutline,
  VolumeLowOutline,
  VolumeMuteOutline,
  MicOutline,
  MicOffOutline,
  CameraOutline,
  ImageOutline,
  ImagesOutline,
  VideocamOutline,
  FilmOutline,
  MusicalNotesOutline,
  BusinessOutline,
  StorefrontOutline,
  CartOutline,
  WalletOutline,
  CardOutline,
  CashOutline,
  ReceiptOutline,
  CalculatorOutline,
  LockClosedOutline,
  LockOpenOutline,
  KeyOutline,
  ShieldCheckmarkOutline,
  EyeOutline,
  EyeOffOutline,
  PowerOutline,
  BatteryFullOutline,
  WifiOutline,
  NotificationsOutline,
  MailOutline,
  CalendarOutline,
  TimeOutline,
  WarningOutline,
  InformationCircleOutline,
  CloseCircleOutline,
  FilterOutline,
  ColorPaletteOutline,
  ColorFilterOutline,
  BrushOutline,
  ColorFillOutline,
  ShapesOutline,
  SquareOutline,
  AtCircleOutline,
  TriangleOutline,
  EllipseOutline,
  RadioButtonOnOutline,
  RadioButtonOffOutline,
  CheckboxOutline,
  ToggleOutline,
  LinkOutline,
  ShareOutline,
  StarOutline,
  HeartOutline,
  ThumbsUpOutline,
  ThumbsDownOutline,
  PhonePortraitOutline
} from '@vicons/ionicons5'
import MobilePublishDialog from '@/components/mobile/MobilePublishDialog.vue'
import CronEditor from '@/components/CronEditor.vue'
import type { PublishItem } from '@/components/mobile/MobilePublishDialog.vue'
import { getReportDefinitionList, getReportDefinitionById, deleteReportDefinition, updateReportDefinition, createReportShare, subscribeReport, getReportSubscriptions, unsubscribeReport, updateReportMobileEnabled } from '@/api/reportDefinition'
import { reportScheduleApi } from '@/api/reportSchedule'
import type { ReportSchedule } from '@/api/reportSchedule'
import { getAllMenus, getVisibleMenus, createMenu, deleteMenu } from '@/api/system/menu'
import { getEnabledChannels } from '@/api/messageChannel'
import { initMessage } from '@/utils/message'
import { handleApiError } from '@/utils/error'
import { PAGE_SIZES } from '@/constants'
import type { ReportDefinition } from '@/types/reportDefinition'
import type { Menu, MenuForm } from '@/types/menu'
import { formatDateTime } from '@/utils/format'
import FilterPanel, { type FilterField } from '@/components/FilterPanel.vue'
import type { FilterCondition } from '@/types/api'
import { filtersToApiParam } from '@/utils/filterParams'
import { useI18n } from '@/i18n'

const { t } = useI18n()

// ==================== 移动端发布 ====================
const showMobilePublish = ref(false)
const mobilePublishItems = computed<PublishItem[]>(() =>
  tableData.value.map(r => ({
    id: r.id,
    name: r.reportName,
    code: r.reportCode,
    mobileEnabled: r.mobileEnabled || 0
  }))
)

async function handleMobileToggle(id: number, enabled: boolean) {
  const item = tableData.value.find(r => r.id === id)
  if (item) {
    const mobileEnabled = enabled ? 1 : 0
    try {
      await updateReportMobileEnabled(id, mobileEnabled)
      item.mobileEnabled = mobileEnabled
      message.success(enabled ? t('chartManage.mobileEnabled') : t('chartManage.mobileDisabled'))
    } catch (e) {
      handleApiError(e, t('common.operationFailed'))
    }
  }
}

async function handleMobileBatchPublish() {
  try {
    const targets = tableData.value.filter(r => r.id && r.mobileEnabled !== 1)
    await Promise.allSettled(targets.map(r => updateReportMobileEnabled(r.id, 1)))
    targets.forEach(r => { r.mobileEnabled = 1 })
    message.success(t('chartManage.batchMobileEnabled'))
  } catch (e) {
    handleApiError(e, t('common.operationFailed'))
  }
}

async function handleMobileBatchUnpublish() {
  try {
    const targets = tableData.value.filter(r => r.id && r.mobileEnabled !== 0)
    await Promise.allSettled(targets.map(r => updateReportMobileEnabled(r.id, 0)))
    targets.forEach(r => { r.mobileEnabled = 0 })
    message.success(t('chartManage.batchMobileDisabled'))
  } catch (e) {
    handleApiError(e, t('common.operationFailed'))
  }
}

// ==================== 分享功能 ====================
const showShareModal = ref(false)
const creatingShare = ref(false)
const shareResult = ref<any>(null)
const shareReportId = ref<number>(0)
const shareForm = reactive({ password: '', expireHours: 0, maxAccessCount: 0 })

const shareLink = computed(() => {
  if (!shareResult.value) return ''
  return `${window.location.origin}/public/share/${shareResult.value.shareToken}`
})

const handleShareReport = (row: ReportDefinition) => {
  shareReportId.value = row.id
  shareResult.value = null
  shareForm.password = ''
  shareForm.expireHours = 0
  shareForm.maxAccessCount = 0
  showShareModal.value = true
}

const handleCreateShare = async () => {
  creatingShare.value = true
  try {
    const res = await createReportShare(shareReportId.value, shareForm)
    shareResult.value = res.data
    message.success(t('chartManage.shareLinkGenerated'))
  } catch (error: any) {
    message.error(error.message || t('common.operationFailed'))
  } finally {
    creatingShare.value = false
  }
}

const copyShareLink = () => {
  navigator.clipboard.writeText(shareLink.value).then(() => {
    message.success(t('common.copySuccess'))
  }).catch(() => {
    message.error(t('common.copyFailed'))
  })
}

// ==================== 订阅功能 ====================
const showSubscribeModal = ref(false)
const subscribing = ref(false)
const subscribeReportId = ref<number>(0)
const reportSubscriptions = ref<any[]>([])
const subscribeForm = reactive({ type: 'email', frequency: 'daily', email: '' })

const openSubscribeModal = async (row: ReportDefinition) => {
  subscribeReportId.value = row.id
  subscribeForm.type = 'email'
  subscribeForm.frequency = 'daily'
  subscribeForm.email = ''
  showSubscribeModal.value = true
  try {
    const res = await getReportSubscriptions(row.id)
    reportSubscriptions.value = res.data || []
  } catch { reportSubscriptions.value = [] }
}

const handleSubscribe = async () => {
  if (!subscribeForm.email && subscribeForm.type === 'email') {
    message.warning(t('reportManage.enterEmail'))
    return
  }
  subscribing.value = true
  const cronMap: Record<string, string> = {
    daily: '0 0 8 * * ?',
    weekly: '0 0 8 ? * MON',
    monthly: '0 0 8 1 * ?'
  }
  try {
    await subscribeReport(subscribeReportId.value, {
      email: subscribeForm.email,
      cron: cronMap[subscribeForm.frequency] || '0 0 8 * * ?'
    })
    message.success(t('common.operationSuccess'))
    const res = await getReportSubscriptions(subscribeReportId.value)
    reportSubscriptions.value = res.data || []
  } catch (error: any) {
    message.error(error.message || t('common.operationFailed'))
  } finally {
    subscribing.value = false
  }
}

const handleUnsubscribe = async (subId: number) => {
  try {
    await unsubscribeReport(subscribeReportId.value, subId)
    message.success(t('common.operationSuccess'))
    reportSubscriptions.value = reportSubscriptions.value.filter(s => s.id !== subId)
  } catch (error: any) {
    message.error(error.message || t('common.operationFailed'))
  }
}

// 图标分类（与MenuManage.vue保持一致）
const iconCategories = [
  { key: 'all', label: '全部' },
  { key: 'common', label: '常用' },
  { key: 'navigation', label: '导航' },
  { key: 'data', label: '数据' },
  { key: 'file', label: '文件' },
  { key: 'media', label: '媒体' },
  { key: 'business', label: '商业' },
  { key: 'system', label: '系统' },
  { key: 'ui', label: '界面' }
]

// 图标选项（分类，与MenuManage.vue保持一致）
const allIconOptions = [
  // 常用
  { label: '网格', value: 'GridOutline', category: 'common' },
  { label: '首页', value: 'HomeOutline', category: 'common' },
  { label: '设置', value: 'SettingsOutline', category: 'common' },
  { label: '搜索', value: 'SearchOutline', category: 'common' },
  { label: '用户', value: 'PersonOutline', category: 'common' },
  { label: '用户组', value: 'PeopleOutline', category: 'common' },
  { label: '文件夹', value: 'FolderOutline', category: 'common' },
  { label: '文档', value: 'DocumentOutline', category: 'common' },
  { label: '应用', value: 'AppsOutline', category: 'common' },
  { label: '菜单', value: 'MenuOutline', category: 'common' },
  
  // 导航
  { label: '仪表盘', value: 'GridOutline', category: 'navigation' },
  { label: '导航', value: 'NavigateOutline', category: 'navigation' },
  { label: '指南针', value: 'CompassOutline', category: 'navigation' },
  { label: '地图', value: 'MapOutline', category: 'navigation' },
  { label: '位置', value: 'LocationOutline', category: 'navigation' },
  { label: '地球', value: 'EarthOutline', category: 'navigation' },
  { label: '全球', value: 'GlobeOutline', category: 'navigation' },
  { label: '旗帜', value: 'FlagOutline', category: 'navigation' },
  
  // 数据
  { label: '服务器', value: 'ServerOutline', category: 'data' },
  { label: '数据库', value: 'LibraryOutline', category: 'data' },
  { label: '分析', value: 'AnalyticsOutline', category: 'data' },
  { label: '统计', value: 'StatsChartOutline', category: 'data' },
  { label: '柱状图', value: 'BarChartOutline', category: 'data' },
  { label: '饼图', value: 'PieChartOutline', category: 'data' },
  { label: '趋势上升', value: 'TrendingUpOutline', category: 'data' },
  { label: '趋势下降', value: 'TrendingDownOutline', category: 'data' },
  { label: '脉冲', value: 'PulseOutline', category: 'data' },
  { label: '速度表', value: 'SpeedometerOutline', category: 'data' },
  { label: '下载', value: 'DownloadOutline', category: 'data' },
  { label: '上传', value: 'CloudUploadOutline', category: 'data' },
  { label: '同步', value: 'SyncOutline', category: 'data' },
  { label: '刷新', value: 'RefreshOutline', category: 'data' },
  { label: '云', value: 'CloudOutline', category: 'data' },
  { label: '云上传', value: 'CloudUploadOutline', category: 'data' },
  { label: '云下载', value: 'CloudDownloadOutline', category: 'data' },
  
  // 文件
  { label: '文档文本', value: 'DocumentTextOutline', category: 'file' },
  { label: '文档', value: 'DocumentsOutline', category: 'file' },
  { label: '文件夹打开', value: 'FolderOpenOutline', category: 'file' },
  { label: '文件盒', value: 'FileTrayOutline', category: 'file' },
  { label: '归档', value: 'ArchiveOutline', category: 'file' },
  { label: '剪贴板', value: 'ClipboardOutline', category: 'file' },
  { label: '保存', value: 'SaveOutline', category: 'file' },
  { label: '打印', value: 'PrintOutline', category: 'file' },
  { label: '复制', value: 'CopyOutline', category: 'file' },
  { label: '剪切', value: 'CutOutline', category: 'file' },
  { label: '删除', value: 'TrashBinOutline', category: 'file' },
  
  // 媒体
  { label: '播放', value: 'PlayOutline', category: 'media' },
  { label: '暂停', value: 'PauseOutline', category: 'media' },
  { label: '停止', value: 'StopOutline', category: 'media' },
  { label: '快进', value: 'PlaySkipForwardOutline', category: 'media' },
  { label: '快退', value: 'PlaySkipBackOutline', category: 'media' },
  { label: '音量高', value: 'VolumeHighOutline', category: 'media' },
  { label: '音量低', value: 'VolumeLowOutline', category: 'media' },
  { label: '静音', value: 'VolumeMuteOutline', category: 'media' },
  { label: '麦克风', value: 'MicOutline', category: 'media' },
  { label: '麦克风关闭', value: 'MicOffOutline', category: 'media' },
  { label: '相机', value: 'CameraOutline', category: 'media' },
  { label: '图片', value: 'ImageOutline', category: 'media' },
  { label: '图片集', value: 'ImagesOutline', category: 'media' },
  { label: '视频', value: 'VideocamOutline', category: 'media' },
  { label: '电影', value: 'FilmOutline', category: 'media' },
  { label: '音乐', value: 'MusicalNotesOutline', category: 'media' },
  
  // 商业
  { label: '商业', value: 'BusinessOutline', category: 'business' },
  { label: '商店', value: 'StorefrontOutline', category: 'business' },
  { label: '购物车', value: 'CartOutline', category: 'business' },
  { label: '钱包', value: 'WalletOutline', category: 'business' },
  { label: '卡片', value: 'CardOutline', category: 'business' },
  { label: '现金', value: 'CashOutline', category: 'business' },
  { label: '收据', value: 'ReceiptOutline', category: 'business' },
  { label: '计算器', value: 'CalculatorOutline', category: 'business' },
  
  // 系统
  { label: '锁', value: 'LockClosedOutline', category: 'system' },
  { label: '解锁', value: 'LockOpenOutline', category: 'system' },
  { label: '钥匙', value: 'KeyOutline', category: 'system' },
  { label: '盾牌', value: 'ShieldCheckmarkOutline', category: 'system' },
  { label: '眼睛', value: 'EyeOutline', category: 'system' },
  { label: '眼睛关闭', value: 'EyeOffOutline', category: 'system' },
  { label: '电源', value: 'PowerOutline', category: 'system' },
  { label: '电池', value: 'BatteryFullOutline', category: 'system' },
  { label: 'WiFi', value: 'WifiOutline', category: 'system' },
  { label: '通知', value: 'NotificationsOutline', category: 'system' },
  { label: '邮件', value: 'MailOutline', category: 'system' },
  { label: '日历', value: 'CalendarOutline', category: 'system' },
  { label: '时间', value: 'TimeOutline', category: 'system' },
  { label: '警告', value: 'WarningOutline', category: 'system' },
  { label: '信息', value: 'InformationCircleOutline', category: 'system' },
  { label: '成功', value: 'CheckmarkCircleOutline', category: 'system' },
  { label: '错误', value: 'CloseCircleOutline', category: 'system' },
  
  // 界面
  { label: '筛选', value: 'FilterOutline', category: 'ui' },
  { label: '颜色', value: 'ColorPaletteOutline', category: 'ui' },
  { label: '颜色滤镜', value: 'ColorFilterOutline', category: 'ui' },
  { label: '画笔', value: 'BrushOutline', category: 'ui' },
  { label: '填充', value: 'ColorFillOutline', category: 'ui' },
  { label: '形状', value: 'ShapesOutline', category: 'ui' },
  { label: '正方形', value: 'SquareOutline', category: 'ui' },
  { label: '圆形', value: 'AtCircleOutline', category: 'ui' },
  { label: '三角形', value: 'TriangleOutline', category: 'ui' },
  { label: '椭圆', value: 'EllipseOutline', category: 'ui' },
  { label: '单选', value: 'RadioButtonOnOutline', category: 'ui' },
  { label: '复选框', value: 'CheckboxOutline', category: 'ui' },
  { label: '开关', value: 'ToggleOutline', category: 'ui' },
  { label: '链接', value: 'LinkOutline', category: 'ui' },
  { label: '分享', value: 'ShareOutline', category: 'ui' },
  { label: '星星', value: 'StarOutline', category: 'ui' },
  { label: '心形', value: 'HeartOutline', category: 'ui' },
  { label: '点赞', value: 'ThumbsUpOutline', category: 'ui' },
  { label: '点踩', value: 'ThumbsDownOutline', category: 'ui' }
]

// 图标映射
const iconMap: Record<string, any> = {
  GridOutline, HomeOutline, SettingsOutline, SearchOutline, PersonOutline, PeopleOutline,
  FolderOutline, DocumentOutline, AppsOutline, MenuOutline, NavigateOutline, CompassOutline,
  MapOutline, LocationOutline, EarthOutline, GlobeOutline, FlagOutline, ServerOutline,
  LibraryOutline, AnalyticsOutline, StatsChartOutline, BarChartOutline, PieChartOutline,
  TrendingUpOutline, TrendingDownOutline, PulseOutline, SpeedometerOutline, DownloadOutline,
  CloudUploadOutline, SyncOutline, RefreshOutline, ReloadOutline, CloudOutline, CloudDownloadOutline,
  DocumentTextOutline, DocumentsOutline, FolderOpenOutline, FileTrayOutline, ArchiveOutline,
  ClipboardOutline, SaveOutline, PrintOutline, CopyOutline, CutOutline, TrashBinOutline,
  PlayOutline, PauseOutline, StopOutline, PlaySkipForwardOutline, PlaySkipBackOutline,
  VolumeHighOutline, VolumeLowOutline, VolumeMuteOutline, MicOutline, MicOffOutline,
  CameraOutline, ImageOutline, ImagesOutline, VideocamOutline, FilmOutline, MusicalNotesOutline,
  BusinessOutline, StorefrontOutline, CartOutline, WalletOutline, CardOutline, CashOutline,
  ReceiptOutline, CalculatorOutline, LockClosedOutline, LockOpenOutline, KeyOutline, ShieldCheckmarkOutline,
  EyeOutline, EyeOffOutline, PowerOutline, BatteryFullOutline, WifiOutline, NotificationsOutline,
  MailOutline, CalendarOutline, TimeOutline, WarningOutline, InformationCircleOutline,
  CheckmarkCircleOutline, CloseCircleOutline, FilterOutline, ColorPaletteOutline, ColorFilterOutline,
  BrushOutline, ColorFillOutline, ShapesOutline, SquareOutline, AtCircleOutline, TriangleOutline,
  EllipseOutline, RadioButtonOnOutline, RadioButtonOffOutline, CheckboxOutline, ToggleOutline,
  LinkOutline, ShareOutline, StarOutline, HeartOutline, ThumbsUpOutline, ThumbsDownOutline
}

const iconCategory = ref('all')
const iconSearchKeyword = ref('')

// 过滤后的图标
const filteredIcons = computed(() => {
  let icons = allIconOptions
  
  // 按分类过滤
  if (iconCategory.value !== 'all') {
    icons = icons.filter(icon => icon.category === iconCategory.value)
  }
  
  // 按关键词搜索
  if (iconSearchKeyword.value) {
    const keyword = iconSearchKeyword.value.toLowerCase()
    icons = icons.filter(icon => 
      icon.label.toLowerCase().includes(keyword) || 
      icon.value.toLowerCase().includes(keyword)
    )
  }
  
  return icons
})

// 获取图标标签
const getIconLabel = (iconValue: string) => {
  const icon = allIconOptions.find(opt => opt.value === iconValue)
  return icon ? icon.label : iconValue
}

// 获取图标组件
const getIconComponent = (iconName?: string): any => {
  if (!iconName) return undefined
  return iconMap[iconName] || undefined
}

const router = useRouter()
const message = useMessage()
const tabsStore = useTabsStore()
initMessage(message)

const searchKeyword = ref('')
const checkedRowKeys = ref<number[]>([])
const showMenuModal = ref(false)
const creatingMenu = ref(false)
const menuFormRef = ref<FormInst | null>(null)
const menuList = ref<Menu[]>([])
const currentReport = ref<ReportDefinition | null>(null)
const activeFilters = ref<FilterCondition[]>([])

// --- 使用 useDataTable 管理表格数据 (Req 6.1) ---
const paginationPage = ref(1)
const paginationPageSize = ref(10)

const {
  data: tableData,
  loading,
  total: paginationItemCount,
  load: loadDataInternal,
  handlePageChange: dtHandlePageChange,
  handlePageSizeChange: dtHandlePageSizeChange
} = useDataTable<ReportDefinition>({
  apiFn: async (params) => {
    const filtersParam = filtersToApiParam(activeFilters.value)
    return await getReportDefinitionList({
      page: params.page,
      pageSize: params.pageSize,
      keyword: searchKeyword.value || undefined,
      filters: filtersParam
    })
  },
  defaultPageSize: 10,
  immediate: false
})

/** 加载数据（同步本地分页状态） */
const loadData = () => loadDataInternal()

/** 分页变化（同步本地 ref 和 useDataTable 内部状态） */
const handlePageChange = (page: number) => {
  paginationPage.value = page
  dtHandlePageChange(page)
}

const handlePageSizeChange = (pageSize: number) => {
  paginationPageSize.value = pageSize
  paginationPage.value = 1
  dtHandlePageSizeChange(pageSize)
}

const menuForm = reactive<MenuForm>({
  id: null,
  menuName: '',
  menuCode: '',
  parentId: 0,
  menuType: 'menu',
  routePath: '',
  componentPath: '@/views/DynamicReport.vue',
  icon: '',
  sortOrder: 0,
  isVisible: 1,
  permissionCode: '',
  reportId: null
})

const menuRules = {
  menuName: [{ required: true, message: t('chartManage.menuNamePlaceholder'), trigger: 'blur' }]
}

const menuTreeOptions = computed(() => {
  // 先找出所有有子菜单的菜单ID
  const parentIds = new Set(menuList.value.filter(m => m && m.parentId).map(m => m.parentId))
  
  const buildOptions = (menus: Menu[], parentId: number = 0): any[] => {
    return menus
      .filter(menu => {
        if (menu.parentId !== parentId) return false
        // 目录类型可以选择
        if (menu.menuType === 'directory') return true
        // 有子菜单的也可以选择（兼容历史数据）
        if (parentIds.has(menu.id)) return true
        return false
      })
      .map(menu => ({
        label: menu.menuName,
        value: menu.id,
        key: menu.id,
        children: buildOptions(menus, menu.id)
      }))
  }
  return buildOptions(menuList.value)
})

// 获取指定父菜单下的最大排序值
const getMaxSortOrder = (parentId: number): number => {
  const children = menuList.value.filter(m => m.parentId === parentId)
  if (children.length === 0) return 0
  return Math.max(...children.map(m => m.sortOrder || 0))
}

// 监听父菜单变化，自动更新排序值
watch(() => menuForm.parentId, (newParentId) => {
  if (newParentId !== undefined) {
    menuForm.sortOrder = getMaxSortOrder(newParentId || 0) + 1
  }
})

// 计算启用报表数量
const enabledCount = computed(() => {
  return tableData.value.filter(report => report.status === 1).length
})

const filterFields: FilterField[] = [
  { label: 'ID', value: 'id', type: 'number' },
  { label: t('report.name'), value: 'reportName', type: 'string' },
  { label: t('report.code'), value: 'reportCode', type: 'string' },
  { label: t('chartManage.dataSourceId'), value: 'dataSourceId', type: 'number' },
  { label: t('table.tableName'), value: 'tableName', type: 'string' },
  { label: t('common.createTime'), value: 'createTime', type: 'string' }
]

// --- 状态映射 (Req 6.3) ---
const reportStatusMap = computed<Record<string | number, { label: string; type: 'success' | 'warning' | 'error' | 'info' | 'default' }>>(() => ({
  1: { label: t('chart.enabled'), type: 'success' },
  0: { label: t('chart.disabled'), type: 'default' }
}))

const reportTypeMap = computed<Record<string, { label: string; type: 'success' | 'warning' | 'error' | 'info' | 'default' }>>(() => ({
  visual: { label: t('reportManage.visual'), type: 'info' },
  sql: { label: 'SQL', type: 'default' },
  '': { label: 'SQL', type: 'default' }
}))

// --- 操作按钮配置 (Req 6.4) ---
const getRowActions = (row: ReportDefinition): ActionConfig[] => {
  const existingMenu = getReportMenu(row.id)
  const actions: ActionConfig[] = [
    {
      label: t('common.view'),
      type: 'primary',
      onClick: () => handleView(row)
    },
    {
      label: t('common.edit'),
      onClick: () => handleEdit(row)
    }
  ]

  if (existingMenu) {
    actions.push({
      label: t('chart.deleteMenu'),
      type: 'warning',
      onClick: () => handleDeleteMenu(existingMenu.id),
      permission: 'report:manage',
      confirm: t('reportManage.deleteMenuConfirm')
    })
  } else {
    actions.push({
      label: t('chart.createMenu'),
      type: 'info',
      onClick: () => handleCreateMenuForReport(row),
      permission: 'report:manage'
    })
  }

  actions.push(
    {
      label: row.status === 1 ? t('chart.disabled') : t('chart.enabled'),
      type: row.status === 1 ? 'warning' : 'success',
      onClick: () => handleToggleStatus(row),
      confirm: t('reportManage.toggleConfirm', { action: row.status === 1 ? t('chart.disabled') : t('chart.enabled'), name: row.reportName })
    },
    {
      label: t('chartManage.share'),
      type: 'success',
      onClick: () => handleShareReport(row)
    },
    {
      label: t('reportManage.subscribe'),
      type: 'info',
      onClick: () => openSubscribeModal(row)
    },
    {
      label: t('reportManage.schedulePush'),
      type: 'info',
      onClick: () => openScheduleDialog(row)
    },
    {
      label: t('common.delete'),
      type: 'error',
      onClick: () => handleDelete(row),
      permission: 'report:manage',
      confirm: t('reportManage.deleteConfirm', { name: row.reportName })
    }
  )

  return actions
}

const columns = [
  {
    type: 'selection' as const,
    width: 48
  },
  {
    title: t('report.name'),
    key: 'reportName',
    width: 200
  },
  {
    title: t('report.code'),
    key: 'reportCode',
    width: 150
  },
  {
    title: t('chartManage.dataSourceId'),
    key: 'dataSourceId',
    width: 100
  },
  {
    title: t('reportManage.reportType'),
    key: 'reportType',
    width: 90,
    render: (row: ReportDefinition) => {
      return h(StatusTag, {
        status: row.reportType || '',
        statusMap: reportTypeMap.value
      })
    }
  },
  {
    title: t('common.status'),
    key: 'status',
    width: 80,
    render: (row: ReportDefinition) => {
      return h(StatusTag, {
        status: row.status,
        statusMap: reportStatusMap.value
      })
    }
  },
  {
    title: t('common.export'),
    key: 'export',
    width: 100,
    render: (row: ReportDefinition) => {
      const tags = []
      if (row.allowExportExcel !== 0) tags.push(h(NTag, { type: 'success', size: 'tiny', style: 'margin-right:4px' }, { default: () => 'Excel' }))
      if (row.allowExportPdf !== 0) tags.push(h(NTag, { type: 'info', size: 'tiny' }, { default: () => 'PDF' }))
      return tags.length > 0 ? h('span', tags) : h(NTag, { type: 'default', size: 'tiny' }, { default: () => t('chart.disabled') })
    }
  },
  {
    title: t('common.createTime'),
    key: 'createTime',
    width: 180,
    render: (row: ReportDefinition) => formatDateTime(row.createTime)
  },
  {
    title: t('common.actions'),
    key: 'actions',
    width: 360,
    fixed: 'right',
    render: (row: ReportDefinition) => {
      return h(ActionButtons, {
        actions: getRowActions(row),
        row,
        maxVisible: 4
      })
    }
  }
]

// 不再需要前端筛选，使用后端筛选（搜索也通过后端keyword参数）

onMounted(() => {
  loadData()
  loadMenus()
})

const handleSearch = () => {
  handlePageChange(1)
}

const handleFilterApply = (filters: FilterCondition[]) => {
  activeFilters.value = filters
  handlePageChange(1)
}

const handleCreate = () => {
  // 替换当前标签页
  tabsStore.replaceTab('/report-manage', {
    key: '/report-designer/new',
    title: t('reportManage.newReport'),
    closable: true
  })
  router.replace('/report-designer/new')
}

const handleView = (row: ReportDefinition) => {
  // 新标签页打开，避免替换管理页面导致标签混乱
  tabsStore.addTab({
    key: `/report-view/${row.id}`,
    title: row.reportName || t('reportManage.viewReport'),
    closable: true
  })
  router.push(`/report-view/${row.id}`)
}

const handleEdit = (row: ReportDefinition) => {
  // 替换当前标签页
  tabsStore.replaceTab('/report-manage', {
    key: `/report-designer/${row.id}`,
    title: t('reportManage.editReport'),
    closable: true
  })
  router.replace(`/report-designer/${row.id}`)
}

const handleDelete = async (row: ReportDefinition) => {
  try {
    await deleteReportDefinition(row.id)
    message.success(t('common.operationSuccess'))
    await loadData()
  } catch (error: any) {
    const errorMsg = handleApiError(error, t('common.delete'), t('common.operationFailed'))
    message.error(errorMsg)
  }
}

/** 批量启用（并行执行） */
const handleBatchEnable = async () => {
  const targets = checkedRowKeys.value
    .map(id => tableData.value.find(r => r.id === id))
    .filter((row): row is ReportDefinition => !!row && row.status !== 1)
  if (targets.length === 0) { message.info(t('reportManage.noNeedEnable') || '无需启用'); return }
  const results = await Promise.allSettled(
    targets.map(row => updateReportDefinition({ id: row.id, reportName: row.reportName, reportCode: row.reportCode, dataSourceId: row.dataSourceId, sqlContent: row.sqlContent, status: 1 }))
  )
  const successCount = results.filter(r => r.status === 'fulfilled').length
  message.success(t('reportManage.batchEnableDone', { count: successCount }))
  checkedRowKeys.value = []
  await loadData()
}

/** 批量禁用（并行执行） */
const handleBatchDisable = async () => {
  const targets = checkedRowKeys.value
    .map(id => tableData.value.find(r => r.id === id))
    .filter((row): row is ReportDefinition => !!row && row.status !== 0)
  if (targets.length === 0) { message.info(t('reportManage.noNeedDisable') || '无需禁用'); return }
  const results = await Promise.allSettled(
    targets.map(row => updateReportDefinition({ id: row.id, reportName: row.reportName, reportCode: row.reportCode, dataSourceId: row.dataSourceId, sqlContent: row.sqlContent, status: 0 }))
  )
  const successCount = results.filter(r => r.status === 'fulfilled').length
  message.success(t('reportManage.batchDisableDone', { count: successCount }))
  checkedRowKeys.value = []
  await loadData()
}

/** 批量删除（并行执行） */
const handleBatchDelete = async () => {
  const results = await Promise.allSettled(
    checkedRowKeys.value.map(id => deleteReportDefinition(id))
  )
  const successCount = results.filter(r => r.status === 'fulfilled').length
  message.success(t('reportManage.batchDeleteDone', { count: successCount }))
  checkedRowKeys.value = []
  await loadData()
}

/** 启用/禁用报表 (Req 4.1) */
const handleToggleStatus = async (row: ReportDefinition) => {
  const newStatus = row.status === 1 ? 0 : 1
  const actionLabel = newStatus === 1 ? t('chart.enabled') : t('chart.disabled')
  try {
    await updateReportDefinition({ id: row.id, reportName: row.reportName, reportCode: row.reportCode, dataSourceId: row.dataSourceId, sqlContent: row.sqlContent, status: newStatus })
    message.success(t('common.operationSuccess'))
    await loadData()
  } catch (error: any) {
    const errorMsg = handleApiError(error, actionLabel, t('common.operationFailed'))
    message.error(errorMsg)
  }
}

const loadMenus = async () => {
  try {
    // 优先用 getAllMenus（admin），失败则降级用 getVisibleMenus（普通用户）
    let res: any
    try {
      res = await getAllMenus(true)
    } catch {
      res = await getVisibleMenus()
    }
    // res 是 ApiResponse<Menu[]>，res.data 是 Menu[] 数组
    const menus: Menu[] = (res as any).data || []
    menuList.value = Array.isArray(menus) ? menus : []
  } catch (error: any) {
    // 菜单列表加载失败不影响主功能，静默处理
    console.warn('菜单加载失败，不影响报表管理主功能', error)
  }
}

// 获取报表对应的菜单（通过 routePath 匹配）
const getReportMenu = (reportId: number): Menu | undefined => {
  const routePath = `/report-view/${reportId}`
  
  // 扁平化菜单列表
  const flattenMenus = (items: Menu[]): Menu[] => {
    const result: Menu[] = []
    const flatten = (list: Menu[]) => {
      list.forEach(item => {
        result.push(item)
        if (item.children && item.children.length > 0) {
          flatten(item.children)
        }
      })
    }
    flatten(items)
    return result
  }
  
  const flatList = flattenMenus(menuList.value)
  return flatList.find(m => m.routePath === routePath)
}

// 删除菜单
const handleDeleteMenu = async (menuId: number) => {
  try {
    await deleteMenu(menuId)
    message.success(t('common.operationSuccess'))
    await loadMenus()
  } catch (error: any) {
    const errorMsg = handleApiError(error, t('common.delete'), t('common.operationFailed'))
    message.error(errorMsg)
  }
}

const handleCreateMenuForReport = (report: ReportDefinition) => {
  currentReport.value = report
  menuForm.menuName = report.reportName
  menuForm.menuCode = `report_${report.reportCode}`
  menuForm.routePath = `/report-view/${report.id}`
  menuForm.componentPath = '@/views/DynamicReport.vue'
  menuForm.reportId = report.id
  menuForm.parentId = 0 // 默认顶级菜单，用户可以选择
  menuForm.sortOrder = getMaxSortOrder(0) + 1
  showMenuModal.value = true
}

const handleCreateMenu = async () => {
  if (!menuFormRef.value || !currentReport.value) return
  
  try {
    await menuFormRef.value.validate()
    creatingMenu.value = true
    
    await createMenu(menuForm)
    message.success(t('common.operationSuccess'))
    showMenuModal.value = false
    // 重置表单
    menuForm.menuName = ''
    menuForm.menuCode = ''
    menuForm.routePath = ''
    menuForm.parentId = 0
    menuForm.sortOrder = 0
    menuForm.icon = ''
    menuForm.reportId = null
    currentReport.value = null
    loadMenus()
  } catch (error: any) {
    const errorMsg = handleApiError(error, t('common.create'), t('common.operationFailed'))
    message.error(errorMsg)
  } finally {
    creatingMenu.value = false
  }
}

// ==================== 定时推送 ====================
const showScheduleModal = ref(false)
const scheduleSaving = ref(false)
const scheduleReportId = ref<number | null>(null)
const existingSchedules = ref<ReportSchedule[]>([])
const scheduleForm = reactive({
  scheduleName: '',
  cronExpression: '0 0 8 * * ?',
  recipients: '',
  channels: 'email',
  emailChannelId: null as number | null,
  wecomChannelId: null as number | null,
  dingtalkChannelId: null as number | null,
  attachExcel: 1,
  attachFormat: 'excel',
  filterParams: '',
  dateParams: '',
  isEnabled: 1
})

// 通道配置选项
const emailChannelOptions = ref<Array<{ label: string; value: number }>>([])
const wecomChannelOptions = ref<Array<{ label: string; value: number }>>([])
const dingtalkChannelOptions = ref<Array<{ label: string; value: number }>>([])

const loadChannelOptions = async () => {
  try {
    const res = await getEnabledChannels() as any
    const grouped = res.data || {}
    emailChannelOptions.value = (grouped.email || []).map((c: any) => ({ label: c.channelName + (c.isDefault ? ` (${t('reportManage.defaultLabel')})` : ''), value: c.id }))
    wecomChannelOptions.value = (grouped.wecom || []).map((c: any) => ({ label: c.channelName + (c.isDefault ? ` (${t('reportManage.defaultLabel')})` : ''), value: c.id }))
    dingtalkChannelOptions.value = (grouped.dingtalk || []).map((c: any) => ({ label: c.channelName + (c.isDefault ? ` (${t('reportManage.defaultLabel')})` : ''), value: c.id }))
  } catch (error) {
    console.error('Load channel options failed', error)
  }
}

// 日期函数参数
const reportParamsLoading = ref(false)
const reportParamRows = ref<Array<{
  name: string
  label: string
  inputType: string
  required: boolean
  value: string
  valueType: 'function' | 'fixed'
  options?: Array<{ label: string; value: string }>
}>>([])
const dateFunctionOptions = [
  { label: '今天 ($today)', value: '$today' },
  { label: '昨天 ($yesterday)', value: '$yesterday' },
  { label: '明天 ($tomorrow)', value: '$tomorrow' },
  { label: '本月第一天 ($thisMonthStart)', value: '$thisMonthStart' },
  { label: '本月最后一天 ($thisMonthEnd)', value: '$thisMonthEnd' },
  { label: '上月第一天 ($lastMonthStart)', value: '$lastMonthStart' },
  { label: '上月最后一天 ($lastMonthEnd)', value: '$lastMonthEnd' },
  { label: '本年第一天 ($thisYearStart)', value: '$thisYearStart' },
  { label: '去年第一天 ($lastYearStart)', value: '$lastYearStart' },
  { label: '去年最后一天 ($lastYearEnd)', value: '$lastYearEnd' },
  { label: '7天前 ($last7days)', value: '$last7days' },
  { label: '30天前 ($last30days)', value: '$last30days' },
  { label: '上周一 ($lastWeekStart)', value: '$lastWeekStart' },
  { label: '上周日 ($lastWeekEnd)', value: '$lastWeekEnd' },
  { label: '当前时间 ($now)', value: '$now' }
]

/** 加载报表参数列表 */
const loadReportParams = async (reportId: number) => {
  reportParamsLoading.value = true
  reportParamRows.value = []
  try {
    const res = await getReportDefinitionById(reportId) as any
    const report = res.data || res
    let params = report?.params
    // params 在后端是 JSON 字符串，需要解析
    if (typeof params === 'string' && params.trim()) {
      try { params = JSON.parse(params) } catch { params = null }
    }
    if (Array.isArray(params) && params.length > 0) {
      reportParamRows.value = params.map((p: any) => ({
        name: p.name,
        label: p.label || p.name,
        inputType: p.inputType || 'text',
        required: !!p.required,
        value: p.defaultValue || '',
        valueType: (p.inputType === 'date' || p.inputType === 'daterange') ? 'function' as const : 'fixed' as const,
        options: p.options || undefined
      }))
    }
  } catch (e) {
    console.error('Load report params failed', e)
  } finally {
    reportParamsLoading.value = false
  }
}
const scheduleChannels = computed({
  get: () => scheduleForm.channels ? scheduleForm.channels.split(',').filter((s: string) => s) : [],
  set: (val: string[]) => { scheduleForm.channels = val.join(',') }
})
const scheduleAttachExcel = computed({
  get: () => scheduleForm.attachExcel === 1,
  set: (val: boolean) => { scheduleForm.attachExcel = val ? 1 : 0 }
})

const openScheduleDialog = async (row: ReportDefinition) => {
  scheduleReportId.value = row.id
  scheduleForm.scheduleName = row.reportName + t('reportManage.scheduleSuffix')
  scheduleForm.cronExpression = '0 0 8 * * ?'
  scheduleForm.recipients = ''
  scheduleForm.channels = 'email'
  scheduleForm.emailChannelId = null
  scheduleForm.wecomChannelId = null
  scheduleForm.dingtalkChannelId = null
  scheduleForm.attachExcel = 1
  scheduleForm.attachFormat = 'excel'
  scheduleForm.filterParams = ''
  scheduleForm.dateParams = ''
  reportParamRows.value = []
  existingSchedules.value = []
  showScheduleModal.value = true
  loadChannelOptions()
  loadReportParams(row.id)
  try {
    const res = await reportScheduleApi.getByReportId(row.id) as any
    existingSchedules.value = res.data || []
  } catch { /* ignore */ }
}

const saveSchedule = async () => {
  if (!scheduleForm.scheduleName || !scheduleForm.cronExpression || !scheduleForm.recipients) {
    message.warning(t('reportManage.fillRequired'))
    return
  }
  // 校验 Cron 表达式格式（6段：秒 分 时 日 月 周）
  const cronParts = scheduleForm.cronExpression.trim().split(/\s+/)
  if (cronParts.length < 6) {
    message.warning(t('reportManage.invalidCron'))
    return
  }
  if (!scheduleForm.channels || scheduleForm.channels.trim() === '') {
    message.warning(t('reportManage.selectChannel'))
    return
  }
  // 校验必填报表参数
  const missingParams = reportParamRows.value.filter(r => r.required && !r.value)
  if (missingParams.length > 0) {
    message.warning(t('reportManage.missingParams', { names: missingParams.map(p => p.label || p.name).join(', ') }))
    return
  }
  // 从 reportParamRows 构建日期函数参数和固定筛选参数
  const dateParamsObj: Record<string, string> = {}
  const filterParamsObj: Record<string, string> = {}
  reportParamRows.value.forEach(row => {
    if (!row.value) return
    if (row.valueType === 'function') {
      dateParamsObj[row.name] = row.value
    } else {
      filterParamsObj[row.name] = row.value
    }
  })
  const dateParamsJson = Object.keys(dateParamsObj).length > 0 ? JSON.stringify(dateParamsObj) : ''
  const filterParamsJson = Object.keys(filterParamsObj).length > 0 ? JSON.stringify(filterParamsObj) : ''

  scheduleSaving.value = true
  try {
    await reportScheduleApi.create({
      reportId: scheduleReportId.value!,
      scheduleName: scheduleForm.scheduleName,
      cronExpression: scheduleForm.cronExpression,
      recipients: scheduleForm.recipients,
      channels: scheduleForm.channels,
      emailChannelId: scheduleForm.emailChannelId || undefined,
      wecomChannelId: scheduleForm.wecomChannelId || undefined,
      dingtalkChannelId: scheduleForm.dingtalkChannelId || undefined,
      attachExcel: scheduleForm.attachExcel,
      attachFormat: scheduleForm.attachFormat || 'excel',
      filterParams: filterParamsJson || undefined,
      dateParams: dateParamsJson || undefined,
      isEnabled: scheduleForm.isEnabled
    })
    message.success(t('reportManage.taskCreated'))
    const res = await reportScheduleApi.getByReportId(scheduleReportId.value!) as any
    existingSchedules.value = res.data || []
  } catch { /* handled */ } finally {
    scheduleSaving.value = false
  }
}

const triggerSchedule = async (id: number) => {
  try {
    await reportScheduleApi.trigger(id)
    message.success(t('reportManage.triggered'))
  } catch {
    message.error(t('reportManage.triggerFailed'))
  }
}

const deleteSchedule = async (id: number) => {
  try {
    await reportScheduleApi.delete(id)
    message.success(t('common.operationSuccess'))
    if (scheduleReportId.value) {
      const res = await reportScheduleApi.getByReportId(scheduleReportId.value) as any
      existingSchedules.value = res.data || []
    }
  } catch { /* handled */ }
}
</script>

<style scoped>
.report-manage-page {
  animation: fadeIn 0.3s ease-out;
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
  background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%);
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

/* ========== 空状态增强 ========== */
.empty-state-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
}

.empty-state-icon {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(245,158,11,0.1) 0%, rgba(239,68,68,0.06) 100%);
  color: #f59e0b;
  margin-bottom: 16px;
}

.empty-state-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.empty-state-desc {
  font-size: 13px;
  color: var(--text-tertiary);
  max-width: 360px;
  text-align: center;
  line-height: 1.6;
}

.report-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

/* 使用全局 page-common.css 样式（fadeIn 在 page-common.css 中定义） */

/* 图标选择器 */
.icon-select-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px 8px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
  background: var(--bg-primary);
}

.icon-select-item:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.icon-selected {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

/* 分享结果框 */
.share-result-box {
  margin-top: 12px;
  padding: 12px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 6px;
}

.share-result-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-primary);
}

/* 图标选择器容器 */
.icon-picker-container {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 12px;
  max-height: 400px;
  overflow-y: auto;
  background: var(--bg-primary);
}

.icon-preview-bar {
  margin-bottom: 12px;
  padding: 8px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
}

.icon-empty-hint {
  text-align: center;
  padding: 20px;
  color: var(--text-tertiary);
}

/* 响应式 */
@media (max-width: 768px) {
  .page-header-stats { flex-direction: row; overflow-x: auto; flex-wrap: nowrap; }
  .page-header-stats .stat-item { min-width: 140px; flex: 0 0 auto; }
  .main-card { border-radius: 14px !important; }
  .pagination-wrapper { flex-direction: column; gap: 8px; }
}

</style>

<style>
/* ReportManage 深色模式（非 scoped） */
html.dark .share-result-box {
  background: rgba(16, 185, 129, 0.1) !important;
  border-color: rgba(16, 185, 129, 0.3) !important;
}
html.dark .icon-picker-container {
  background: #1a2332 !important;
  border-color: #334155 !important;
}
html.dark .icon-preview-bar {
  background: #243044 !important;
  color: #e2e8f0 !important;
}
html.dark .icon-select-item {
  background: #1e293b !important;
  border-color: #334155 !important;
}
html.dark .icon-select-item:hover {
  background: rgba(99, 102, 241, 0.1) !important;
  border-color: var(--color-primary) !important;
}
html.dark .icon-selected {
  background: rgba(99, 102, 241, 0.15) !important;
  border-color: var(--color-primary) !important;
}
html.dark .report-manage-page .header-icon-wrapper { box-shadow: 0 2px 8px rgba(245, 158, 11, 0.15) !important; }
html.dark .report-manage-page .header-title { color: #f1f5f9 !important; }
html.dark .report-manage-page .header-subtitle { color: #64748b !important; }
html.dark .report-manage-page .empty-state-icon { background: linear-gradient(135deg, rgba(245,158,11,0.15) 0%, rgba(239,68,68,0.1) 100%) !important; }
html.dark .report-manage-page .empty-state-title { color: #e2e8f0 !important; }
html.dark .report-manage-page .empty-state-desc { color: #64748b !important; }
</style>

<template>
  <div class="chart-manage-page">
    <!-- 页面头部统计 -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><BarChartOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ total }}</span>
          <span class="stat-label">{{ t('chartManage.totalCharts') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><CheckmarkCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ enabledCount }}</span>
          <span class="stat-label">{{ t('chartManage.enabledCharts') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="24"><CloseCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ disabledCount }}</span>
          <span class="stat-label">{{ t('chartManage.disabledCharts') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info">
          <n-icon size="24"><StatsChartOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ topChartType }}</span>
          <span class="stat-label">{{ t('chartManage.topType') }}</span>
        </div>
      </div>
    </div>

    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <div class="header-icon-wrapper">
            <n-icon size="18"><BarChartOutline /></n-icon>
          </div>
          <div class="header-text">
            <span class="header-title">{{ t('chartManage.title') }}</span>
            <span class="header-subtitle">{{ t('chartManage.searchPlaceholder') }}</span>
          </div>
        </div>
      </template>

      <!-- 工具栏独立区域 -->
      <div class="table-toolbar">
        <n-space align="center" :wrap="false">
          <n-input
            v-model:value="searchKeyword"
            :placeholder="t('chartManage.searchPlaceholder')"
            clearable
            style="width: 200px"
            @keydown.enter="handleSearch"
            @input="handleSearchInput"
            @clear="handleSearch"
          >
            <template #prefix>
              <n-icon :component="SearchOutline" />
            </template>
          </n-input>
          <n-select
            v-model:value="filterChartType"
            :options="chartTypeFilterOptions"
            :placeholder="t('chart.type')"
            clearable
            style="width: 120px"
            @update:value="handleSearch"
          />
          <n-select
            v-model:value="filterStatus"
            :options="statusFilterOptions"
            :placeholder="t('common.status')"
            clearable
            style="width: 90px"
            @update:value="handleSearch"
          />
          <n-button @click="handleSearch">{{ t('common.search') }}</n-button>
          <n-button quaternary @click="handleResetFilter">{{ t('common.reset') }}</n-button>
        </n-space>
        <n-space align="center">
          <n-button
            tertiary
            :disabled="checkedRowKeys.length === 0"
            @click="handleBatchEnable"
          >
            <template #icon><n-icon :component="CheckmarkCircleOutline" /></template>
            {{ t('chart.enabled') }}
          </n-button>
          <n-button
            tertiary
            :disabled="checkedRowKeys.length === 0"
            @click="handleBatchDisable"
          >
            <template #icon><n-icon :component="CloseCircleOutline" /></template>
            {{ t('chart.disabled') }}
          </n-button>
          <n-button
            tertiary
            type="error"
            :disabled="checkedRowKeys.length === 0"
            @click="handleBatchDelete"
          >
            <template #icon><n-icon :component="TrashOutline" /></template>
            {{ t('common.delete') }}
          </n-button>
          <n-divider vertical />
          <n-button secondary @click="showMobilePublish = true">
            <template #icon><n-icon :component="PhonePortraitOutline" /></template>
            {{ t('chartManage.mobilePublish') }}
          </n-button>
          <n-button type="info" secondary @click="handleAiDesign">
            <template #icon><n-icon :component="SparklesOutline" /></template>
            {{ t('chartManage.aiDesign') }}
          </n-button>
          <n-button type="primary" @click="handleCreate">
            <template #icon><n-icon :component="AddOutline" /></template>
            {{ t('common.add') }}
          </n-button>
          <n-divider vertical />
          <n-button-group size="small">
            <n-button :type="viewMode === 'table' ? 'primary' : 'default'" @click="viewMode = 'table'">
              <template #icon><n-icon :component="ListOutline" /></template>
            </n-button>
            <n-button :type="viewMode === 'card' ? 'primary' : 'default'" @click="viewMode = 'card'">
              <template #icon><n-icon :component="GridOutline" /></template>
            </n-button>
          </n-button-group>
        </n-space>
      </div>

      <!-- 骨架屏加载状态 -->
      <div v-if="loading && data.length === 0">
        <n-space vertical :size="12">
          <n-skeleton v-for="i in 5" :key="i" height="40px" :sharp="false" />
        </n-space>
      </div>

      <!-- 数据表格视图 -->
      <n-data-table
        v-else-if="viewMode === 'table'"
        :columns="columns"
        :data="data"
        :loading="loading"
        :pagination="false"
        :row-key="(row: ChartDefinition) => row.id"
        :checked-row-keys="checkedRowKeys"
        striped
        :scroll-x="1200"
        :default-sort="{ columnKey: 'createTime', order: 'descend' }"
        class="custom-table"
        @update:checked-row-keys="handleCheck"
      >
        <template #empty>
          <div class="empty-state-wrapper">
            <div class="empty-state-icon">
              <n-icon size="48"><BarChartOutline /></n-icon>
            </div>
            <div class="empty-state-title">{{ t('chartManage.noChartData') }}</div>
            <div class="empty-state-desc">创建您的第一个图表，支持折线图、柱状图、饼图等多种可视化类型</div>
            <n-button type="primary" size="large" style="margin-top: 16px;" @click="handleCreate">
              <template #icon>
                <n-icon><AddOutline /></n-icon>
              </template>
              {{ t('chartManage.createFirst') }}
            </n-button>
          </div>
        </template>
      </n-data-table>

      <!-- 卡片网格视图 -->
      <div v-else class="chart-card-grid">
        <div v-if="data.length === 0" style="grid-column: 1 / -1;">
          <div class="empty-state-wrapper">
            <div class="empty-state-icon">
              <n-icon size="48"><BarChartOutline /></n-icon>
            </div>
            <div class="empty-state-title">{{ t('chartManage.noChartData') }}</div>
            <div class="empty-state-desc">创建您的第一个图表，拖拽数据即可快速生成可视化</div>
            <n-button type="primary" size="large" style="margin-top: 16px;" @click="handleCreate">
              <template #icon>
                <n-icon><AddOutline /></n-icon>
              </template>
              {{ t('chartManage.createFirst') }}
            </n-button>
          </div>
        </div>
        <div
          v-for="item in data"
          :key="item.id"
          class="chart-card-item"
          @click="handleEdit(item)"
        >
          <div class="chart-card-thumb">
            <n-icon size="40" :color="item.status === 1 ? '#3b82f6' : '#94a3b8'">
              <BarChartOutline />
            </n-icon>
            <n-tag
              size="tiny"
              :type="item.status === 1 ? 'success' : 'default'"
              style="position: absolute; top: 8px; right: 8px;"
            >
              {{ item.status === 1 ? t('chart.enabled') : t('chart.disabled') }}
            </n-tag>
          </div>
          <div class="chart-card-body">
            <div class="chart-card-name">{{ item.chartName }}</div>
            <div class="chart-card-code">{{ item.chartCode }}</div>
            <div class="chart-card-meta">
              <n-tag size="tiny" :bordered="false">{{ getChartTypeLabel(item.chartType) }}</n-tag>
              <span class="chart-card-time">{{ formatDateTime(item.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination-wrapper">
        <div class="pagination-info">
          <n-tag type="info" size="small" round>
            {{ t('chartManage.totalRecords', { count: total }) }}
          </n-tag>
        </div>
        <n-pagination
          :page="pagination.page"
          :page-size="pagination.pageSize"
          :item-count="total"
          :page-sizes="[10, 20, 50, 100]"
          show-size-picker
          show-quick-jumper
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-card>

    <!-- 分享弹窗 -->
    <n-modal v-model:show="showShareModal" preset="card" :title="t('chartManage.shareTitle')" style="width: 480px; border-radius: 16px;">
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

    <!-- 嵌入弹窗 -->
    <n-modal v-model:show="showEmbedModal" preset="card" :title="t('chartManage.embedTitle')" style="width: 600px; border-radius: 16px;">
      <p class="embed-hint">{{ t('chartManage.embedHint') }}</p>
      <n-input
        :value="embedCode"
        type="textarea"
        readonly
        :autosize="{ minRows: 3, maxRows: 5 }"
      />
      <div style="margin-top: 12px;">
        <n-button type="primary" @click="copyEmbedCode">{{ t('chartManage.copyEmbedCode') }}</n-button>
        <n-button style="margin-left: 8px" tag="a" :href="embedUrl" target="_blank">{{ t('common.preview') }}</n-button>
      </div>
    </n-modal>

    <!-- 创建菜单对话框 - 使用 useFormModal 管理 -->
    <n-modal
      v-model:show="menuModal.visible.value"
      preset="card"
      :title="menuModal.title.value"
      style="width: 700px;"
      :segmented="{ content: 'soft', footer: 'soft' }"
    >
      <div style="max-height: 70vh; overflow-y: auto;">
        <n-form
          ref="menuFormRef"
          :model="menuModal.formData.value"
          :rules="menuRules"
          label-placement="left"
          label-width="100px"
        >
          <n-form-item :label="t('chartManage.menuName')" path="menuName">
            <n-input v-model:value="menuModal.formData.value.menuName" :placeholder="t('chartManage.menuNamePlaceholder')" />
          </n-form-item>
          <n-form-item :label="t('chartManage.parentMenu')">
            <n-tree-select
              v-model:value="menuModal.formData.value.parentId"
              :options="menuTreeOptions"
              :placeholder="t('chartManage.parentMenuPlaceholder')"
              clearable
              filterable
            />
          </n-form-item>
          <n-form-item :label="t('chartManage.icon')">
            <div class="icon-picker-container">
              <!-- 已选择的图标显示 -->
              <div v-if="menuModal.formData.value.icon" class="icon-preview-bar">
                <n-icon :component="getIconComponent(menuModal.formData.value.icon)" size="24" />
                <span>{{ t('chartManage.iconSelected') }}: {{ getIconLabel(menuModal.formData.value.icon) }}</span>
                <n-button size="tiny" quaternary style="margin-left: auto;" @click="menuModal.formData.value.icon = ''">{{ t('common.clear') }}</n-button>
              </div>

              <!-- 搜索框 -->
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

              <!-- 分类标签 -->
              <n-tabs v-model:value="iconCategory" type="segment" size="small" style="margin-bottom: 12px;">
                <n-tab-pane v-for="category in iconCategories" :key="category.key" :name="category.key" :tab="category.label" />
              </n-tabs>

              <!-- 图标网格 -->
              <div class="icon-grid">
                <div
                  v-for="iconOption in filteredIcons"
                  :key="iconOption.value"
                  class="icon-select-item"
                  :class="{ 'icon-selected': menuModal.formData.value.icon === iconOption.value }"
                  :title="iconOption.label"
                  @click="menuModal.formData.value.icon = iconOption.value"
                >
                  <n-icon v-if="getIconComponent(iconOption.value)" :component="getIconComponent(iconOption.value)" size="28" />
                  <div v-else style="width: 28px; height: 28px; background: var(--bg-tertiary); border-radius: 4px;"></div>
                  <span style="font-size: 11px; text-align: center; word-break: break-word; line-height: 1.2;">{{ iconOption.label }}</span>
                </div>
              </div>

              <!-- 无结果提示 -->
              <div v-if="filteredIcons.length === 0" class="icon-empty-hint">
                {{ t('chartManage.noIconFound') }}
              </div>

              <!-- 提示信息 -->
              <div class="icon-count-hint">
                {{ t('chartManage.iconCount', { count: filteredIcons.length }) }}
              </div>
            </div>
          </n-form-item>
          <n-form-item :label="t('chartManage.sortOrder')">
            <n-input-number v-model:value="menuModal.formData.value.sortOrder" :min="0" style="width: 100%" />
          </n-form-item>
        </n-form>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="menuModal.close()">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="menuModal.submitting.value" @click="handleCreateMenu">{{ t('common.create') }}</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 移动端发布对话框 -->
    <MobilePublishDialog
      v-model:show="showMobilePublish"
:title="t('chartManage.mobilePublishTitle')"
      :items="mobilePublishItems"
      @toggle="handleMobileToggle"
      @batch-publish="handleMobileBatchPublish"
      @batch-unpublish="handleMobileBatchUnpublish"
    />
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { logger } from '@/utils/logger'
import { ref, onMounted, h, onBeforeUnmount, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NSpace, NIcon, useMessage, useDialog, NDivider, NTabs, NTabPane } from 'naive-ui'
import type { FormInst } from 'naive-ui'
import {
  AddOutline, SearchOutline, CheckmarkCircleOutline, CloseCircleOutline,
  BarChartOutline, SparklesOutline, TrashOutline, GridOutline, ListOutline,
  StatsChartOutline, PhonePortraitOutline
} from '@vicons/ionicons5'
import MobilePublishDialog from '@/components/mobile/MobilePublishDialog.vue'
import type { PublishItem } from '@/components/mobile/MobilePublishDialog.vue'
import { getChartDefinitionList, deleteChartDefinition, copyChartDefinition, updateChartStatus, batchUpdateChartStatus, batchDeleteChartDefinition, createChartShare, getChartEmbedUrl, updateChartMobileEnabled } from '@/api/chart'
import { getAllMenus, getVisibleMenus, createMenu, deleteMenu } from '@/api/system/menu'
import { initMessage } from '@/utils/message'
import { handleApiError } from '@/utils/error'
import type { ChartDefinition } from '@/types/chart'
import type { Menu, MenuForm } from '@/types/menu'
import { formatDateTime } from '@/utils/format'
import { CHART_TYPES } from '@/types/chart'
import { useTabsStore } from '@/stores/tabs'
import { useDataTable } from '@/composables/useDataTable'
import { useFormModal } from '@/composables/useFormModal'
import StatusTag from '@/components/common/StatusTag.vue'
import ActionButtons, { type ActionConfig } from '@/components/common/ActionButtons.vue'
import { allIconOptions, iconCategories, getIconComponent, getIconLabel } from '@/views/chartManageIcons'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const router = useRouter()
const message = useMessage()
const dialog = useDialog()
const tabsStore = useTabsStore()
initMessage(message)

// ==================== 移动端发布 ====================
const showMobilePublish = ref(false)
const mobilePublishItems = computed<PublishItem[]>(() =>
  data.value.map(c => ({
    id: c.id,
    name: c.chartName,
    code: c.chartCode,
    mobileEnabled: c.mobileEnabled || 0
  }))
)

async function handleMobileToggle(id: number, enabled: boolean) {
  const item = data.value.find(c => c.id === id)
  if (item) {
    const mobileEnabled = enabled ? 1 : 0
    try {
      await updateChartMobileEnabled(id, mobileEnabled)
      item.mobileEnabled = mobileEnabled
      message.success(enabled ? t('chartManage.mobileEnabled') : t('chartManage.mobileDisabled'))
    } catch (e) {
      handleApiError(e, t('common.operationFailed'))
    }
  }
}

async function handleMobileBatchPublish() {
  try {
    for (const c of data.value) {
      if (c.id) {
        await updateChartMobileEnabled(c.id, 1)
        c.mobileEnabled = 1
      }
    }
    message.success(t('chartManage.batchMobileEnabled'))
  } catch (e) {
    handleApiError(e, t('common.operationFailed'))
  }
}

async function handleMobileBatchUnpublish() {
  try {
    for (const c of data.value) {
      if (c.id) {
        await updateChartMobileEnabled(c.id, 0)
        c.mobileEnabled = 0
      }
    }
    message.success(t('chartManage.batchMobileDisabled'))
  } catch (e) {
    handleApiError(e, t('common.operationFailed'))
  }
}

// ==================== 搜索/筛选状态 ====================
const viewMode = ref<'table' | 'card'>('table')
const searchKeyword = ref('')
const filterChartType = ref<string | null>(null)
const filterStatus = ref<number | null>(null)
let searchTimer: ReturnType<typeof setTimeout> | null = null

// ==================== useDataTable 集成 (Req 7.2) ====================
const {
  data,
  loading,
  total,
  pagination,
  checkedRowKeys,
  load,
  refresh,
  handlePageChange,
  handlePageSizeChange,
  handleCheck,
  clearChecked
} = useDataTable<ChartDefinition>({
  apiFn: (params) => {
    const apiParams: any = { page: params.page, pageSize: params.pageSize }
    if (searchKeyword.value?.trim()) {
      apiParams.keyword = searchKeyword.value.trim()
    }
    if (filterChartType.value) {
      apiParams.chartType = filterChartType.value
    }
    if (filterStatus.value !== null && filterStatus.value !== undefined) {
      apiParams.status = filterStatus.value
    }
    return getChartDefinitionList(apiParams)
  },
  defaultPageSize: 10,
  immediate: true,
  rowKey: 'id' as keyof ChartDefinition
})

// ==================== useFormModal 集成 - 菜单创建弹窗 (Req 7.2) ====================
const defaultMenuForm: MenuForm = {
  id: null,
  menuName: '',
  menuCode: '',
  parentId: 0,
  menuType: 'menu',
  routePath: '',
  componentPath: '',
  icon: '',
  sortOrder: 0,
  isVisible: 1,
  permissionCode: '',
  reportId: null
}

const menuModal = useFormModal<MenuForm>({
  defaultFormData: () => ({ ...defaultMenuForm }),
  createFn: async (formData) => {
    await createMenu(formData)
  },
  onSuccess: () => {
    message.success(t('common.operationSuccess'))
    loadMenus()
  },
  onError: (error) => {
    const errorMsg = handleApiError(error, t('common.operationFailed'), t('common.operationFailed'))
    message.error(errorMsg)
  }
})

const menuFormRef = ref<FormInst | null>(null)
const menuList = ref<Menu[]>([])
const currentChart = ref<ChartDefinition | null>(null)

const menuRules = {
  menuName: [{ required: true, message: t('chartManage.menuNamePlaceholder'), trigger: 'blur' }]
}

// ==================== 状态映射 (Req 7.4) ====================
const chartStatusMap = computed(() => ({
  1: { label: t('chart.enabled'), type: 'success' as const },
  0: { label: t('chart.disabled'), type: 'default' as const }
}))

// ==================== 图表类型筛选选项 ====================
const chartTypeFilterOptions = CHART_TYPES.map(type => ({
  label: type.label,
  value: type.value
}))

const statusFilterOptions = computed(() => [
  { label: t('chart.enabled'), value: 1 },
  { label: t('chart.disabled'), value: 0 }
])

// ==================== 统计数据 ====================
const enabledCount = computed(() => data.value.filter(c => c.status === 1).length)
const disabledCount = computed(() => data.value.filter(c => c.status === 0).length)
const topChartType = computed(() => {
  if (data.value.length === 0) return '-'
  const counts: Record<string, number> = {}
  data.value.forEach(c => { counts[c.chartType] = (counts[c.chartType] || 0) + 1 })
  const top = Object.entries(counts).sort((a, b) => b[1] - a[1])[0]
  return top ? getChartTypeLabel(top[0]) : '-'
})

const getChartTypeLabel = (type: string): string => {
  const found = CHART_TYPES.find(t => t.value === type)
  return found ? found.label : type
}

// ==================== 分享功能 ====================
const showShareModal = ref(false)
const creatingShare = ref(false)
const shareResult = ref<any>(null)
const shareChartId = ref<number>(0)
const shareForm = ref({ password: '', expireHours: 0, maxAccessCount: 0 })

const shareLink = computed(() => {
  if (!shareResult.value) return ''
  return `${window.location.origin}/public/share/${shareResult.value.shareToken}`
})

const handleShareChart = (row: ChartDefinition) => {
  shareChartId.value = row.id
  shareResult.value = null
  shareForm.value = { password: '', expireHours: 0, maxAccessCount: 0 }
  showShareModal.value = true
}

const handleCreateShare = async () => {
  creatingShare.value = true
  try {
    const res = await createChartShare(shareChartId.value, shareForm.value)
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

// ==================== 嵌入功能 ====================
const showEmbedModal = ref(false)
const embedUrl = ref('')
const embedCode = computed(() => {
  if (!embedUrl.value) return ''
  return `<iframe src="${embedUrl.value}" width="800" height="500" frameborder="0" style="border: 1px solid #e0e0e0; border-radius: 8px;"></iframe>`
})

const handleEmbedChart = (row: ChartDefinition) => {
  embedUrl.value = getChartEmbedUrl(row.id)
  showEmbedModal.value = true
}

const copyEmbedCode = () => {
  navigator.clipboard.writeText(embedCode.value).then(() => {
    message.success(t('common.copySuccess'))
  }).catch(() => {
    message.error(t('common.copyFailed'))
  })
}

// ==================== 图标选择相关 ====================
const iconCategory = ref('all')
const iconSearchKeyword = ref('')

const filteredIcons = computed(() => {
  let icons = allIconOptions
  if (iconCategory.value !== 'all') {
    icons = icons.filter(icon => icon.category === iconCategory.value)
  }
  if (iconSearchKeyword.value) {
    const keyword = iconSearchKeyword.value.toLowerCase()
    icons = icons.filter(icon =>
      icon.label.toLowerCase().includes(keyword) ||
      icon.value.toLowerCase().includes(keyword)
    )
  }
  return icons
})

// ==================== 菜单树 ====================
const menuTreeOptions = computed(() => {
  const parentIds = new Set(menuList.value.filter(m => m && m.parentId).map(m => m.parentId))
  const buildOptions = (menus: Menu[], parentId: number = 0): any[] => {
    return menus
      .filter(menu => {
        if (menu.parentId !== parentId) return false
        if (menu.menuType === 'directory') return true
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

const getMaxSortOrder = (parentId: number): number => {
  const children = menuList.value.filter(m => m.parentId === parentId)
  if (children.length === 0) return 0
  return Math.max(...children.map(m => m.sortOrder || 0))
}

watch(() => menuModal.formData.value.parentId, (newParentId) => {
  if (newParentId !== undefined) {
    menuModal.formData.value.sortOrder = getMaxSortOrder(newParentId || 0) + 1
  }
})

// ==================== 表格列定义 (使用 StatusTag + ActionButtons) ====================
const columns = computed(() => [
  { type: 'selection' as const, width: 50 },
  { title: t('chart.name'), key: 'chartName', width: 200, sorter: 'default' as const },
  {
    title: t('chart.code'),
    key: 'chartCode',
    width: 150,
    sorter: 'default' as const,
    render: (row: ChartDefinition) => {
      const value = row.chartCode
      return (!value || value.toString().trim() === '') ? '-' : value
    }
  },
  {
    title: t('chart.type'),
    key: 'chartType',
    width: 120,
    filterOptions: CHART_TYPES.map(t => ({ label: t.label, value: t.value })),
    filter: (value: string, row: ChartDefinition) => row.chartType === value,
    render: (row: ChartDefinition) => {
      const chartType = CHART_TYPES.find(t => t.value === row.chartType)
      return chartType ? chartType.label : row.chartType
    }
  },
  {
    title: t('chartManage.dataSourceId'),
    key: 'dataSourceId',
    width: 100,
    sorter: (a: ChartDefinition, b: ChartDefinition) => (a.dataSourceId || 0) - (b.dataSourceId || 0),
    render: (row: ChartDefinition) => {
      const value = row.dataSourceId
      return (!value || value.toString().trim() === '') ? '-' : value
    }
  },
  {
    title: t('common.status'),
    key: 'status',
    width: 80,
    sorter: (a: ChartDefinition, b: ChartDefinition) => (a.status || 0) - (b.status || 0),
    render: (row: ChartDefinition) => h(StatusTag, { status: row.status, statusMap: chartStatusMap.value })
  },
  {
    title: t('chartManage.description'),
    key: 'description',
    width: 180,
    ellipsis: { tooltip: true } as any,
    render: (row: ChartDefinition) => row.description || '-'
  },
  {
    title: t('common.createTime'),
    key: 'createTime',
    width: 180,
    sorter: (a: ChartDefinition, b: ChartDefinition) => new Date(a.createTime || 0).getTime() - new Date(b.createTime || 0).getTime(),
    defaultSortOrder: 'descend' as const,
    render: (row: ChartDefinition) => formatDateTime(row.createTime)
  },
  {
    title: t('common.actions'),
    key: 'actions',
    width: 280,
    fixed: 'right' as const,
    render: (row: ChartDefinition) => {
      const existingMenu = getChartMenu(row.id)
      const actions: ActionConfig[] = [
        { label: t('common.view'), type: 'primary', onClick: () => handleView(row) },
        { label: t('common.edit'), onClick: () => handleEdit(row), permission: 'chart:manage' },
        { label: t('common.copy'), type: 'info', onClick: () => handleCopy(row), permission: 'chart:manage' },
        {
          label: row.status === 1 ? t('chart.disabled') : t('chart.enabled'),
          type: row.status === 1 ? 'warning' : 'success',
          onClick: () => handleToggleStatus(row),
          permission: 'chart:manage'
        },
        ...(existingMenu
          ? [{ label: t('chart.deleteMenu'), type: 'warning' as const, onClick: () => handleDeleteMenu(existingMenu.id), permission: 'chart:manage', confirm: t('chartManage.deleteMenuConfirm') }]
          : [{ label: t('chart.createMenu'), type: 'info' as const, onClick: () => handleCreateMenuForChart(row), permission: 'chart:manage' }]
        ),
        { label: t('chartManage.share'), type: 'success', onClick: () => handleShareChart(row) },
        { label: t('chartManage.embed'), onClick: () => handleEmbedChart(row) },
        { label: t('common.delete'), type: 'error', onClick: () => handleDelete(row), permission: 'chart:manage', confirm: true }
      ]
      return h(ActionButtons, { actions, row, maxVisible: 3 })
    }
  }
])

// ==================== 搜索/筛选处理 ====================
const handleSearch = () => {
  if (searchTimer) {
    clearTimeout(searchTimer)
    searchTimer = null
  }
  load()
}

const handleSearchInput = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => handleSearch(), 500)
}

const handleResetFilter = () => {
  searchKeyword.value = ''
  filterChartType.value = null
  filterStatus.value = null
  load()
}

// ==================== 导航操作 ====================
const handleCreate = () => {
  tabsStore.replaceTab('/chart-manage', { key: '/chart-designer/new', title: t('chartManage.newChart'), closable: true })
  router.replace('/chart-designer/new')
}

const handleAiDesign = () => {
  tabsStore.replaceTab('/chart-manage', { key: '/ai-chart-design', title: t('chartManage.aiDesignTitle'), closable: true })
  router.replace('/ai-chart-design')
}

const handleView = (row: ChartDefinition) => {
  tabsStore.replaceTab('/chart-manage', { key: `/chart-center/${row.id}?from=chart-manage`, title: row.chartName || t('chart.title'), closable: true })
  router.replace(`/chart-center/${row.id}?from=chart-manage`)
}

const handleEdit = (row: ChartDefinition) => {
  tabsStore.replaceTab('/chart-manage', { key: `/chart-designer/${row.id}`, title: t('chartManage.editChart'), closable: true })
  router.replace(`/chart-designer/${row.id}`)
}

// ==================== CRUD 操作 ====================
const handleDelete = async (row: ChartDefinition) => {
  if (!row || !row.id) { message.error(t('common.operationFailed')); return }
  dialog.warning({
    title: t('common.confirmDelete'),
    content: t('chartManage.deleteConfirm', { name: row.chartName }),
    positiveText: t('common.confirm'),
    negativeText: t('common.cancel'),
    onPositiveClick: async () => {
      const loadingMsg = message.loading(t('chartManage.processing', { action: t('common.delete') }), { duration: 0 })
      try {
        await deleteChartDefinition(row.id)
        loadingMsg.destroy()
        message.success(t('common.operationSuccess'))
        if (data.value.length === 1 && pagination.value.page! > 1) {
          handlePageChange(pagination.value.page! - 1)
        } else {
          await refresh()
        }
      } catch (error: any) {
        loadingMsg.destroy()
        logger.error('Delete chart failed:', error)
        message.error(handleApiError(error, t('common.delete'), t('common.operationFailed')))
      }
    }
  })
}

const handleCopy = async (row: ChartDefinition) => {
  dialog.info({
    title: t('chartManage.copyChart'),
    content: t('chartManage.copyConfirm', { name: row.chartName }),
    positiveText: t('common.confirm'),
    negativeText: t('common.cancel'),
    onPositiveClick: async () => {
      const loadingMsg = message.loading(t('chartManage.processing', { action: t('common.copy') }), { duration: 0 })
      try {
        await copyChartDefinition(row.id)
        loadingMsg.destroy()
        message.success(t('common.operationSuccess'))
        await refresh()
      } catch (error: any) {
        loadingMsg.destroy()
        logger.error('Copy chart failed:', error)
        message.error(handleApiError(error, t('common.copy'), t('common.operationFailed')))
      }
    }
  })
}

const handleToggleStatus = async (row: ChartDefinition) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? t('chart.enabled') : t('chart.disabled')
  dialog.info({
    title: action,
    content: t('chartManage.toggleConfirm', { action, name: row.chartName }),
    positiveText: t('common.confirm'),
    negativeText: t('common.cancel'),
    onPositiveClick: async () => {
      const loadingMsg = message.loading(t('chartManage.processing', { action }), { duration: 0 })
      try {
        await updateChartStatus(row.id, newStatus)
        loadingMsg.destroy()
        message.success(t('common.operationSuccess'))
        await refresh()
      } catch (error: any) {
        loadingMsg.destroy()
        logger.error(`Toggle chart status failed:`, error)
        message.error(handleApiError(error, action, t('common.operationFailed')))
      }
    }
  })
}

// ==================== 批量操作 ====================
const handleBatchEnable = () => {
  if (checkedRowKeys.value.length === 0) { message.warning(t('chartManage.selectFirst', { action: t('chart.enabled') })); return }
  dialog.info({
    title: t('chart.enabled'),
    content: t('chartManage.batchEnableConfirm', { count: checkedRowKeys.value.length }),
    positiveText: t('common.confirm'),
    negativeText: t('common.cancel'),
    onPositiveClick: async () => {
      const loadingMsg = message.loading(t('chartManage.processingCount', { action: t('chart.enabled'), count: checkedRowKeys.value.length }), { duration: 0 })
      try {
        await batchUpdateChartStatus(checkedRowKeys.value as number[], 1)
        loadingMsg.destroy()
        message.success(t('common.operationSuccess'))
        clearChecked()
        await refresh()
      } catch (error: any) {
        loadingMsg.destroy()
        logger.error('Batch enable failed:', error)
        message.error(handleApiError(error, t('chart.enabled'), t('common.operationFailed')))
      }
    }
  })
}

const handleBatchDisable = () => {
  if (checkedRowKeys.value.length === 0) { message.warning(t('chartManage.selectFirst', { action: t('chart.disabled') })); return }
  dialog.warning({
    title: t('chart.disabled'),
    content: t('chartManage.batchDisableConfirm', { count: checkedRowKeys.value.length }),
    positiveText: t('common.confirm'),
    negativeText: t('common.cancel'),
    onPositiveClick: async () => {
      const loadingMsg = message.loading(t('chartManage.processingCount', { action: t('chart.disabled'), count: checkedRowKeys.value.length }), { duration: 0 })
      try {
        await batchUpdateChartStatus(checkedRowKeys.value as number[], 0)
        loadingMsg.destroy()
        message.success(t('common.operationSuccess'))
        clearChecked()
        await refresh()
      } catch (error: any) {
        loadingMsg.destroy()
        logger.error('Batch disable failed:', error)
        message.error(handleApiError(error, t('chart.disabled'), t('common.operationFailed')))
      }
    }
  })
}

const handleBatchDelete = () => {
  if (checkedRowKeys.value.length === 0) { message.warning(t('chartManage.selectFirst', { action: t('common.delete') })); return }
  dialog.warning({
    title: t('common.confirmDelete'),
    content: t('chartManage.batchDeleteConfirm', { count: checkedRowKeys.value.length }),
    positiveText: t('common.confirm'),
    negativeText: t('common.cancel'),
    onPositiveClick: async () => {
      const loadingMsg = message.loading(t('chartManage.processingCount', { action: t('common.delete'), count: checkedRowKeys.value.length }), { duration: 0 })
      try {
        await batchDeleteChartDefinition(checkedRowKeys.value as number[])
        loadingMsg.destroy()
        message.success(t('common.operationSuccess'))
      } catch (error: any) {
        loadingMsg.destroy()
        logger.error('Batch delete failed:', error)
        message.error(handleApiError(error, t('common.delete'), t('common.operationFailed')))
      }
      clearChecked()
      await refresh()
    }
  })
}

// ==================== 菜单管理 ====================
const loadMenus = async () => {
  try {
    let res: any
    try {
      res = await getAllMenus(true)
    } catch {
      res = await getVisibleMenus()
    }
    const menus: Menu[] = (res as any).data || []
    menuList.value = Array.isArray(menus) ? menus : []
  } catch (error: any) {
    console.warn('菜单加载失败，不影响主功能', error)
  }
}

const getChartMenu = (chartId: number): Menu | undefined => {
  const routePath = `/chart-center/${chartId}`
  const flattenMenus = (items: Menu[]): Menu[] => {
    const result: Menu[] = []
    const flatten = (list: Menu[]) => {
      list.forEach(item => {
        result.push(item)
        if (item.children && item.children.length > 0) flatten(item.children)
      })
    }
    flatten(items)
    return result
  }
  return flattenMenus(menuList.value).find(m => m.routePath === routePath)
}

const handleDeleteMenu = async (menuId: number) => {
  try {
    await deleteMenu(menuId)
    message.success(t('common.operationSuccess'))
    await loadMenus()
  } catch (error: any) {
    message.error(handleApiError(error, t('common.delete'), t('common.operationFailed')))
  }
}

const handleCreateMenuForChart = (chart: ChartDefinition) => {
  currentChart.value = chart
  menuModal.openCreate()
  // Override form data with chart-specific values
  menuModal.formData.value.menuName = chart.chartName
  menuModal.formData.value.menuCode = `chart_${chart.chartCode || chart.id}`
  menuModal.formData.value.routePath = `/chart-center/${chart.id}`
  menuModal.formData.value.componentPath = ''
  menuModal.formData.value.parentId = 0
  menuModal.formData.value.sortOrder = getMaxSortOrder(0) + 1
}

const handleCreateMenu = async () => {
  if (!menuFormRef.value || !currentChart.value) return
  try {
    await menuFormRef.value.validate()
    await menuModal.submit()
    currentChart.value = null
  } catch {
    // validation failed - do nothing
  }
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadMenus()
})

onBeforeUnmount(() => {
  if (searchTimer) {
    clearTimeout(searchTimer)
    searchTimer = null
  }
})
</script>

<style scoped>
.chart-manage-page {
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
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: #fff;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
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
  background: linear-gradient(135deg, rgba(59,130,246,0.1) 0%, rgba(37,99,235,0.06) 100%);
  color: #3b82f6;
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

/* 图标选择样式 */
.icon-select-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px 8px;
  border: 2px solid var(--border-light, #e8e8e8);
  border-radius: var(--dp-radius-md);
  cursor: pointer;
  transition: all 0.2s;
  background: var(--bg-primary, #fff);
}

.icon-select-item:hover {
  border-color: var(--color-primary, #3b82f6);
  background: var(--color-primary-light, #eff6ff);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}

.icon-select-item.icon-selected {
  border-color: var(--color-primary, #3b82f6);
  background: linear-gradient(135deg, var(--color-primary-light, #eff6ff) 0%, #dbeafe 100%);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
}

.icon-select-item .n-icon { color: var(--text-secondary, #64748b); }
.icon-select-item:hover .n-icon,
.icon-select-item.icon-selected .n-icon { color: var(--color-primary, #3b82f6); }

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

/* 嵌入提示 */
.embed-hint {
  margin-bottom: 12px;
  color: var(--text-secondary);
}

/* 图标选择器容器 */
.icon-picker-container {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 12px;
  width: 100%;
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

.icon-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  max-height: 300px;
  overflow-y: auto;
  padding: 4px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  background: var(--bg-secondary);
}

.icon-empty-hint {
  text-align: center;
  padding: 20px;
  color: var(--text-tertiary);
}

.icon-count-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-tertiary);
}

/* 卡片网格视图 */
.chart-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
  padding: 4px 0;
}

.chart-card-item {
  border: 1px solid var(--border-light);
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s ease;
  background: var(--bg-primary);
}

.chart-card-item:hover {
  border-color: var(--color-primary, #3b82f6);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.12);
  transform: translateY(-2px);
}

.chart-card-thumb {
  position: relative;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f8fafc 0%, #eef2ff 100%);
  border-bottom: 1px solid var(--border-light);
}

.chart-card-body {
  padding: 12px;
}

.chart-card-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chart-card-code {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chart-card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chart-card-time {
  font-size: 11px;
  color: var(--text-tertiary);
}

/* 响应式 */
@media (max-width: 768px) {
  .page-header-stats { flex-direction: row; overflow-x: auto; flex-wrap: nowrap; }
  .page-header-stats .stat-item { min-width: 140px; flex: 0 0 auto; }
  .chart-card-grid { grid-template-columns: repeat(2, 1fr); gap: 10px !important; }
  .main-card { border-radius: 14px !important; }
  .empty-state-wrapper { padding: 32px 16px; }
  .empty-state-icon { width: 68px; height: 68px; }
}
</style>

<style>
/* ChartManage 深色模式（非 scoped） */
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
html.dark .icon-grid {
  background: #141e30 !important;
  border-color: #334155 !important;
}
html.dark .icon-select-item {
  background: #1e293b !important;
  border-color: #334155 !important;
}
html.dark .icon-select-item:hover {
  background: rgba(99, 102, 241, 0.1) !important;
  border-color: var(--color-primary) !important;
}
html.dark .icon-select-item.icon-selected {
  background: linear-gradient(135deg, rgba(129,140,248,0.15) 0%, rgba(99,102,241,0.1) 100%) !important;
  border-color: var(--color-primary) !important;
}
html.dark .chart-card-item { background: #1e293b !important; border-color: #334155 !important; }
html.dark .chart-card-item:hover { border-color: var(--color-primary) !important; }
html.dark .chart-card-thumb { background: linear-gradient(135deg, #1a2536 0%, #243044 100%) !important; border-bottom-color: #334155 !important; }
html.dark .header-icon-wrapper { box-shadow: 0 2px 8px rgba(37, 99, 235, 0.15) !important; }
html.dark .header-title { color: #f1f5f9 !important; }
html.dark .header-subtitle { color: #64748b !important; }
html.dark .empty-state-icon { background: linear-gradient(135deg, rgba(59,130,246,0.15) 0%, rgba(37,99,235,0.1) 100%) !important; }
html.dark .empty-state-title { color: #e2e8f0 !important; }
html.dark .empty-state-desc { color: #64748b !important; }
</style>

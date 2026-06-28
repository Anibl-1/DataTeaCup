<template>
  <div class="data-collect-page">
    <!-- 任务列表子组件 -->
    <CollectTaskList
      :table-data="tableData"
      :loading="loading"
      :total-count="paginationItemCount"
      :page="paginationPage"
      :page-size="paginationPageSize"
      :active-filters="activeFilters"
      @create="handleCreate"
      @edit="handleEdit"
      @view-detail="handleViewDetail"
      @show-import="showImportModal = true"
      @debug-scheduler="handleDebugScheduler"
      @page-change="handlePageChange"
      @page-size-change="handlePageSizeChange"
      @filter-apply="handleFilterApply"
    />

    <!-- 任务配置子组件（新建/编辑弹窗） -->
    <CollectTaskConfig
      ref="taskConfigRef"
      :data-source-options="dataSourceOptions"
      :data-source-loading="dataSourceLoading"
      @saved="fetchData"
    />

    <!-- 任务详情/日志子组件 -->
    <CollectTaskLog
      ref="taskLogRef"
      @edit="handleEdit"
      @refresh="fetchData"
    />

    <!-- 数据导入模态框 -->
    <n-modal v-model:show="showImportModal" preset="card" :title="t('collect.importData')" style="width: 600px">
      <n-form
        ref="importFormRef"
        :model="importForm"
        :rules="importRules"
        label-placement="left"
        label-width="120px"
      >
        <n-form-item :label="t('collect.selectFile')" path="file">
          <n-space vertical :size="16" style="width: 100%;">
            <n-space :size="12" style="width: 100%;">
              <n-button type="primary" size="large" style="flex: 1; min-width: 180px;" @click="handleSelectExcel">
                <template #icon><n-icon size="20">📊</n-icon></template>
                {{ t('collect.selectExcel') }}
              </n-button>
              <n-button type="info" size="large" style="flex: 1; min-width: 180px;" @click="handleSelectText">
                <template #icon><n-icon size="20">📄</n-icon></template>
                {{ t('collect.selectText') }}
              </n-button>
            </n-space>
            <n-alert type="info" :bordered="false" style="margin-top: 8px;">
              <template #header><span style="font-weight: 600;">{{ t('collect.supportedTypes') }}</span></template>
              <n-space vertical :size="6">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <n-tag type="success" size="small" :bordered="false">.xlsx</n-tag>
                  <span style="font-size: 13px;">{{ t('collect.excelFormat2007') }}</span>
                </div>
                <div style="display: flex; align-items: center; gap: 8px;">
                  <n-tag type="success" size="small" :bordered="false">.xls</n-tag>
                  <span style="font-size: 13px;">{{ t('collect.excelFormat97') }}</span>
                </div>
                <div style="display: flex; align-items: center; gap: 8px;">
                  <n-tag type="info" size="small" :bordered="false">.csv</n-tag>
                  <n-tag type="info" size="small" :bordered="false">.txt</n-tag>
                  <span style="font-size: 13px;">{{ t('collect.textFile') }}</span>
                </div>
                <n-divider style="margin: 8px 0;" />
                <div class="form-hint-secondary">
                  <n-icon size="16" style="vertical-align: middle; margin-right: 4px;"><InformationCircleOutline /></n-icon>
                  {{ t('collect.maxFileSize') }}
                </div>
              </n-space>
            </n-alert>
          </n-space>

          <!-- 已选择文件信息卡片 -->
          <n-card
            v-if="importForm.fileList.length > 0"
            size="small" :bordered="false"
            class="file-selected-card"
          >
            <template #header>
              <div style="display: flex; align-items: center; gap: 8px;">
                <n-icon size="22" color="#18a058"><CheckmarkCircleOutline /></n-icon>
                <span style="font-weight: 600; color: var(--color-success);">{{ t('collect.fileSelected') }}</span>
              </div>
            </template>
            <n-space vertical :size="12">
              <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
                <n-icon size="28" color="#18a058"><DocumentOutline /></n-icon>
                <div style="flex: 1; min-width: 200px;">
                  <div style="font-weight: 600; font-size: 15px; margin-bottom: 4px;">
                    {{ importForm.fileList[0].name || importForm.fileList[0].file?.name || t('collect.unknownFile') }}
                  </div>
                  <n-space :size="10">
                    <n-tag size="small" type="info" :bordered="false">
                      {{ formatFileSize(importForm.fileList[0]?.file?.size || importForm.fileList[0]?.size || 0) }}
                    </n-tag>
                    <n-tag size="small" :type="getFileTypeTag(importForm.fileList[0].name || importForm.fileList[0].file?.name || '')" :bordered="false">
                      {{ getFileTypeLabel(importForm.fileList[0].name || importForm.fileList[0].file?.name || '') }}
                    </n-tag>
                  </n-space>
                </div>
              </div>
              <n-space :size="10">
                <n-button size="medium" type="primary" :loading="previewLoading" :disabled="previewLoading" @click="handlePreviewFile">
                  <template #icon><n-icon><EyeOutline /></n-icon></template>
                  {{ t('collect.previewData') }}
                </n-button>
                <n-button size="medium" :disabled="previewLoading || importing" @click="importForm.fileList = []; previewData = null; previewColumns = []">
                  <template #icon><n-icon><CloseOutline /></n-icon></template>
                  {{ t('collect.clearFile') }}
                </n-button>
              </n-space>
            </n-space>
          </n-card>
        </n-form-item>

        <n-form-item v-if="previewData && previewData.headers" :label="t('collect.dataPreview')">
          <n-card size="small" style="max-height: 300px; overflow-y: auto;">
            <n-data-table :columns="previewColumns" :data="previewData.previewData" :pagination="false" size="small" max-height="250" />
            <div class="form-hint" style="margin-top: 8px;">
              {{ t('collect.previewRows', { preview: previewData.previewRows, total: previewData.totalRows }) }}
            </div>
          </n-card>
        </n-form-item>

        <n-form-item :label="t('collect.targetDataSource')" path="dataSourceId">
          <n-select
v-model:value="importForm.dataSourceId" :options="dataSourceOptions" :placeholder="t('collect.selectTargetDs')"
            :loading="dataSourceLoading" filterable clearable
            :disabled="dataSourceLoading || dataSourceOptions.length === 0"
            @update:value="handleDataSourceSelect"
            @blur="() => { if (importFormRef) importFormRef.validate().catch(() => {}) }" />
          <template #feedback>
            <div class="form-hint">
              <div v-if="dataSourceOptions.length === 0" style="color: var(--color-error);">{{ t('collect.dsEmptyHint') }}</div>
              <div v-else>{{ t('collect.dsSelectHint') }}</div>
            </div>
          </template>
        </n-form-item>
        <n-form-item :label="t('collect.targetTable')">
          <n-input v-model:value="importForm.tableName" :placeholder="t('collect.tableAutoHint')" />
        </n-form-item>
        <n-form-item :label="t('collect.autoCreateTable')">
          <n-switch v-model:value="importForm.autoCreateTable" />
          <template #feedback>
            <div class="form-hint">{{ t('collect.autoCreateHint') }}</div>
          </template>
        </n-form-item>
        <n-form-item :label="t('collect.firstRowHeader')">
          <n-switch v-model:value="importForm.firstRowAsHeader" />
        </n-form-item>
        <n-alert v-if="importing" type="info" style="margin-top: 16px;">{{ t('collect.importing') }}</n-alert>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button :disabled="importing" @click="showImportModal = false">{{ t('common.cancel') }}</n-button>
          <n-button
type="primary" :loading="importing"
            :disabled="importForm.fileList.length === 0 || !importForm.dataSourceId || importing"
            @click="handleImport">
            {{ t('collect.startImport') }}
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { logger } from '@/utils/logger'
import { ref, reactive, onMounted } from 'vue'
import { useMessage, useDialog, type FormInst } from 'naive-ui'
import {
  DocumentOutline, EyeOutline, CheckmarkCircleOutline,
  CloseOutline, InformationCircleOutline
} from '@vicons/ionicons5'
import { getCollectTaskList, importData, previewImportFile, debugScheduler } from '@/api/dataCollect'
import { getDataSourceList } from '@/api/dataSource'
import { initMessage } from '@/utils/message'
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE } from '@/constants'
import type { CollectTask } from '@/types/collectTask'
import type { DataSource } from '@/types/dataSource'
import type { PageResult, FilterCondition } from '@/types/api'
import { handleApiError } from '@/utils/error'
import { formatFileSize } from '@/utils/export'
import { filtersToApiParam } from '@/utils/filterParams'
import { validateFileForImport, ALLOWED_EXTENSIONS, isExcelFile, isTextFile } from '@/utils/fileImport'

import CollectTaskList from './CollectTaskList.vue'
import CollectTaskConfig from './CollectTaskConfig.vue'
import CollectTaskLog from './CollectTaskLog.vue'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const message = useMessage()
const dialog = useDialog()
initMessage(message)

// --- Refs for sub-components ---
const taskConfigRef = ref<InstanceType<typeof CollectTaskConfig> | null>(null)
const taskLogRef = ref<InstanceType<typeof CollectTaskLog> | null>(null)

// --- Shared state ---
const loading = ref(false)
const tableData = ref<CollectTask[]>([])
const paginationPage = ref(DEFAULT_PAGE)
const paginationPageSize = ref(DEFAULT_PAGE_SIZE)
const paginationItemCount = ref(0)
const activeFilters = ref<FilterCondition[]>([])

// Data source options (shared between config and import)
const dataSourceOptions = ref<Array<{ label: string; value: number }>>([])
const dataSourceLoading = ref(false)

// Import modal state
const showImportModal = ref(false)
const importFormRef = ref<FormInst | null>(null)
const importing = ref(false)
const previewLoading = ref(false)
const previewData = ref<any>(null)
const previewColumns = ref<any[]>([])

const importForm = reactive({
  fileList: [] as any[],
  dataSourceId: null as number | null,
  tableName: '',
  autoCreateTable: true,
  firstRowAsHeader: true
})

const importRules = {
  fileList: [{
    required: true, message: t('collect.selectFileRequired'), trigger: ['change', 'blur'],
    validator: (_rule: any, value: any) => {
      if (!value || !Array.isArray(value) || value.length === 0) return new Error(t('collect.selectFileRequired'))
      const fileItem = value[0]
      if (!fileItem || (!fileItem.file && !(fileItem instanceof File))) return new Error(t('collect.fileInvalid'))
      return true
    }
  }],
  dataSourceId: [{
    required: true, message: t('collect.selectDsRequired'), trigger: ['change', 'blur'],
    validator: (_rule: any, value: any) => {
      if (!value || value === null) return new Error(t('collect.selectDsRequired'))
      if (typeof value !== 'number' || value <= 0) return new Error(t('collect.dsInvalid'))
      const exists = dataSourceOptions.value.some(opt => opt.value === value)
      if (!exists) return new Error(t('collect.dsNotExist'))
      return true
    }
  }]
}

// --- Data fetching ---
const fetchDataSources = async () => {
  dataSourceLoading.value = true
  try {
    const res = await getDataSourceList({ page: 1, pageSize: 1000 })
    let dataList: DataSource[] = []
    if (res && res.data) {
      const responseData = res.data as any
      if (Array.isArray(responseData)) dataList = responseData
      else if (responseData.list && Array.isArray(responseData.list)) dataList = responseData.list
      else if (responseData.data?.list && Array.isArray(responseData.data.list)) dataList = responseData.data.list
    }
    dataSourceOptions.value = dataList.map((item: DataSource) => ({
      label: `${item.name} (${item.dbType || t('collect.unknown')})`,
      value: item.id
    }))
    if (dataSourceOptions.value.length === 0) {
      logger.warn(t('collect.noDsAvailable'))
    }
  } catch (error) {
    message.error(handleApiError(error, t('collect.fetchTaskFailed'), t('collect.fetchDsFailed')))
    dataSourceOptions.value = []
  } finally {
    dataSourceLoading.value = false
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const filtersParam = filtersToApiParam(activeFilters.value)
    const res = await getCollectTaskList({
      page: paginationPage.value,
      pageSize: paginationPageSize.value,
      filters: filtersParam
    })
    let pageResult: PageResult<CollectTask> | null = null
    if (res && typeof res === 'object') {
      if (res.data && typeof res.data === 'object' && 'list' in res.data && 'total' in res.data) {
        pageResult = res.data as PageResult<CollectTask>
      } else if ('list' in res && 'total' in res) {
        pageResult = res as PageResult<CollectTask>
      }
    }
    tableData.value = pageResult?.list || []
    if (pageResult && pageResult.total !== undefined && pageResult.total !== null) {
      const total = typeof pageResult.total === 'number'
        ? Math.max(0, pageResult.total)
        : (() => { const num = Number(pageResult.total); return isNaN(num) ? 0 : Math.max(0, num) })()
      paginationItemCount.value = total
    } else {
      paginationItemCount.value = 0
    }
  } catch (error) {
    message.error(handleApiError(error, t('collect.fetchTaskFailed')))
    if (tableData.value.length === 0) paginationItemCount.value = 0
  } finally {
    loading.value = false
  }
}

// --- Event handlers from sub-components ---
const handleCreate = () => taskConfigRef.value?.openCreate()
const handleEdit = (row: CollectTask) => taskConfigRef.value?.openEdit(row)
const handleViewDetail = (row: CollectTask) => taskLogRef.value?.openDetail(row)

const handlePageChange = (page: number) => {
  paginationPage.value = page
  fetchData()
}
const handlePageSizeChange = (pageSize: number) => {
  paginationPageSize.value = pageSize
  paginationPage.value = 1
  fetchData()
}
const handleFilterApply = (filters: FilterCondition[]) => {
  activeFilters.value = filters
  paginationPage.value = 1
  fetchData()
}

const handleDebugScheduler = async () => {
  try {
    const res = await debugScheduler() as any
    const data = res.data
    if (data.scheduledRunningTasksCount === 0) {
      message.warning(t('collect.noScheduledTasks'))
    } else {
      message.success(t('collect.foundTasks', { count: data.scheduledRunningTasksCount }))
    }
    dialog.info({
      title: t('collect.schedulerStatus'),
      content: t('collect.schedulerDetail', { time: data.currentTime, count: data.scheduledRunningTasksCount }),
      positiveText: t('common.confirm')
    })
  } catch (error) {
    logger.error('调试失败:', error)
    message.error(t('collect.schedulerFailed'))
  }
}

// --- Import logic ---
const createFileInput = (accept: string, extensions: string[], typeLabel: string) => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = accept
  input.onchange = (e: Event) => {
    const file = (e.target as HTMLInputElement).files?.[0]
    if (!file) return
    const validation = validateFileForImport(file.name, file.size)
    if (!validation.valid) { message.error(validation.error!); return }
    const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase()
    if (!extensions.includes(ext)) { message.error(t('collect.selectFileType', { type: typeLabel, ext: extensions.join(' / ') })); return }
    importForm.fileList = [{ id: Date.now().toString(), name: file.name, status: 'finished', file }]
    previewData.value = null
    previewColumns.value = []
    message.success(t('collect.fileSelectSuccess', { name: file.name, size: formatFileSize(file.size) }))
  }
  input.click()
}

const handleSelectExcel = () => createFileInput(
  '.xlsx,.xls,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel',
  ['.xlsx', '.xls'], 'Excel'
)
const handleSelectText = () => createFileInput('.txt,.csv,text/plain,text/csv', ['.txt', '.csv'], t('collect.text'))

const handleDataSourceSelect = (value: number | null) => {
  if (value) {
    const selected = dataSourceOptions.value.find(opt => opt.value === value)
    if (selected) { importForm.dataSourceId = Number(value) }
    else { message.warning(t('collect.dsInvalid')); importForm.dataSourceId = null }
  } else { importForm.dataSourceId = null }
  importFormRef.value?.validate().catch(() => {})
}

const getFileTypeTag = (fileName: string): 'success' | 'info' | 'warning' => {
  if (!fileName) return 'info'
  const ext = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()
  if (['.xlsx', '.xls'].includes(ext)) return 'success'
  if (['.csv', '.txt'].includes(ext)) return 'info'
  return 'warning'
}

const getFileTypeLabel = (fileName: string): string => {
  if (!fileName) return t('collect.unknown')
  const ext = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()
  const map: Record<string, string> = { '.xlsx': 'Excel 2007+', '.xls': 'Excel 97-2003', '.csv': 'CSV', '.txt': t('collect.text') }
  return map[ext] || ext.toUpperCase()
}

const handlePreviewFile = async () => {
  if (importForm.fileList.length === 0) { message.warning(t('collect.selectFileRequired')); return }
  const fileItem = importForm.fileList[0]
  const file = fileItem?.file || fileItem
  if (!file) { message.warning(t('collect.fileInvalid')); return }
  const fileName = file.name || fileItem.name || ''
  const fileExt = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()
  if (!ALLOWED_EXTENSIONS.includes(fileExt as any)) {
    message.error(t('collect.unsupportedFormat', { ext: fileExt })); return
  }
  previewLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file instanceof File ? file : file.file || file)
    formData.append('firstRowAsHeader', String(importForm.firstRowAsHeader))
    formData.append('previewRows', '10')
    const res = await previewImportFile(formData as any)
    const result = res.data as any
    previewData.value = result
    if (result.headers?.length > 0) {
      previewColumns.value = result.headers.map((header: string) => ({
        title: header, key: header, width: 150, ellipsis: { tooltip: true }
      }))
    }
    message.success(t('collect.previewSuccess', { count: result.totalRows }))
  } catch (error: any) {
    message.error(handleApiError(error, t('collect.previewData'), t('collect.previewFailed')))
  } finally { previewLoading.value = false }
}

const handleImport = async () => {
  if (!importFormRef.value) { message.warning(t('collect.formNotInit')); return }
  if (importForm.fileList.length === 0) { message.warning(t('collect.selectFileRequired')); return }
  if (!importForm.dataSourceId) { message.warning(t('collect.selectDsRequired')); return }

  await importFormRef.value.validate(async (errors: any) => {
    if (errors) {
      const msgs: string[] = []
      if (errors.fileList) msgs.push(t('collect.selectFileRequired'))
      if (errors.dataSourceId) msgs.push(t('collect.selectDsRequired'))
      message.error(msgs.length > 0 ? msgs.join(', ') : t('collect.formValidFailed'))
      return
    }
    try {
      const fileItem = importForm.fileList[0]
      if (!fileItem) { message.error(t('collect.fileItemMissing')); return }
      let file: File | null = null
      if (fileItem.file instanceof File) file = fileItem.file
      else if (fileItem instanceof File) file = fileItem
      else if (fileItem.file && typeof fileItem.file === 'object') {
        const rawFile = (fileItem.file as any).raw || (fileItem.file as any).file || fileItem.file
        if (rawFile instanceof File) file = rawFile
      }
      if (!file) { message.error(t('collect.fileObjFailed')); return }
      const fileName = file.name || fileItem.name || ''
      if (!fileName) { message.error(t('collect.fileNameInvalid')); return }
      const fileExt = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()
      const fileValidation = validateFileForImport(fileName, file.size)
      if (!fileValidation.valid) { message.error(fileValidation.error!); return }

      importing.value = true
      const loadingMessage = message.loading(t('collect.importLoading'), { duration: 0 })
      try {
        const formData = new FormData()
        formData.append('file', file, fileName)
        formData.append('dataSourceId', String(importForm.dataSourceId))
        if (importForm.tableName?.trim()) formData.append('tableName', importForm.tableName.trim())
        formData.append('autoCreateTable', String(importForm.autoCreateTable))
        formData.append('firstRowAsHeader', String(importForm.firstRowAsHeader))
        const res = await importData(formData as any)
        const result = res.data as any
        loadingMessage.destroy()
        const successRate = result.totalCount > 0 ? ((result.successCount / result.totalCount) * 100).toFixed(2) : '0'
        message.success(
          t('collect.importSuccess', { success: result.successCount, fail: result.failCount, total: result.totalCount, rate: successRate, table: result.tableName }) +
          (result.tableCreated ? t('collect.tableAutoCreated') : '')
        )
        showImportModal.value = false
        importForm.fileList = []; importForm.dataSourceId = null; importForm.tableName = ''
        importForm.autoCreateTable = true; importForm.firstRowAsHeader = true
        previewData.value = null; previewColumns.value = []
      } catch (error: any) {
        loadingMessage.destroy()
        message.error(handleApiError(error, t('collect.importData'), t('collect.importFailed')))
      } finally { importing.value = false }
    } catch (error: any) {
      logger.error('导入前验证失败:', error)
      message.error(t('collect.importValidFailed') + ': ' + (error?.message || String(error)))
      importing.value = false
    }
  }).catch((error: any) => {
    logger.error('表单验证异常:', error)
    message.error(t('collect.formValidError'))
    importing.value = false
  })
}

onMounted(() => {
  fetchDataSources()
  fetchData()
})
</script>

<style scoped>
.data-collect-page {
  /* Main container - layout handled by sub-components */
}
</style>

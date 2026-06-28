<template>
  <div class="data-import-page">
    <!-- 页面头部统计 -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><CloudUploadOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ importHistory.length }}</span>
          <span class="stat-label">{{ t('dataImport.importRecords') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><CheckmarkCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ successCount }}</span>
          <span class="stat-label">{{ t('dataImport.success') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-error">
          <n-icon size="24"><CloseCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ failedCount }}</span>
          <span class="stat-label">{{ t('dataImport.failed') }}</span>
        </div>
      </div>
    </div>

    <n-card :bordered="false">
      <n-tabs type="line" animated>
        <!-- Excel/CSV 导入 -->
        <n-tab-pane name="file" :tab="t('dataImport.fileImport')">
          <div class="import-section">
            <n-alert type="info" :bordered="false" style="margin-bottom: 20px;">
              {{ t('dataImport.fileImportHint') }}
            </n-alert>
            
            <n-form ref="fileFormRef" :model="fileForm" label-placement="left" label-width="100px">
              <n-form-item :label="t('dataImport.targetDataSource')" path="dataSourceId">
                <n-select
                  v-model:value="fileForm.dataSourceId"
                  :options="dataSourceOptions"
                  :placeholder="t('dataImport.targetDataSourcePlaceholder')"
                  clearable
                  filterable
                />
              </n-form-item>
              
              <n-form-item :label="t('dataImport.targetTable')" path="tableName">
                <n-input v-model:value="fileForm.tableName" :placeholder="t('dataImport.targetTablePlaceholder')" />
              </n-form-item>
              
              <n-form-item :label="t('dataImport.selectFile')" path="file">
                <n-upload
                  ref="uploadRef"
                  :max="1"
                  :default-upload="false"
                  accept=".xlsx,.xls,.csv"
                  @change="handleFileChange"
                >
                  <n-button>
                    <template #icon><n-icon><DocumentOutline /></n-icon></template>
                    {{ t('dataImport.selectFile') }}
                  </n-button>
                </n-upload>
              </n-form-item>
              
              <n-form-item :label="t('dataImport.importOptions')">
                <n-space>
                  <n-checkbox v-model:checked="fileForm.autoCreateTable">{{ t('dataImport.autoCreateTable') }}</n-checkbox>
                  <n-checkbox v-model:checked="fileForm.skipHeader">{{ t('dataImport.firstRowHeader') }}</n-checkbox>
                  <n-checkbox v-model:checked="fileForm.truncateFirst">{{ t('dataImport.truncateFirst') }}</n-checkbox>
                </n-space>
              </n-form-item>
            </n-form>
            
            <!-- 导入模式选择 -->
            <ImportModeSelector
              v-model="fileImportMode"
              :fields="fileFieldOptions"
              @update:model-value="handleFileModeChange"
            />
            
            <!-- 预览区域 -->
            <div v-if="previewData.length > 0" class="preview-section">
              <div class="preview-header">
                <span>{{ t('dataImport.dataPreview') }}</span>
                <n-space>
                  <n-tag type="info">{{ t('dataImport.totalRows', { n: previewTotal }) }}</n-tag>
                  <n-button
                    size="small"
                    :loading="validating"
                    :disabled="!fileForm.dataSourceId || !fileForm.tableName"
                    @click="handleValidate"
                  >
                    <template #icon><n-icon><ShieldCheckmarkOutline /></n-icon></template>
                    {{ t('dataImport.validateData') }}
                  </n-button>
                </n-space>
              </div>
              <n-data-table
                :columns="previewColumns"
                :data="previewData"
                :max-height="300"
                size="small"
                striped
                class="custom-table"
              />
            </div>
            
            <!-- 校验结果面板 -->
            <div v-if="validationResult" class="validation-panel">
              <div class="validation-header">
                <span style="font-weight: 600;">{{ t('dataImport.validationResult') }}</span>
                <n-space size="small">
                  <n-tag type="success" size="small">{{ t('dataImport.passed', { n: validationResult.passedRows }) }}</n-tag>
                  <n-tag v-if="validationResult.failedRows > 0" type="error" size="small">{{ t('dataImport.failedRows', { n: validationResult.failedRows }) }}</n-tag>
                  <n-tag v-if="validationResult.warningRows > 0" type="warning" size="small">{{ t('dataImport.warningRows', { n: validationResult.warningRows }) }}</n-tag>
                </n-space>
              </div>
              <div v-if="validationResult.errors && validationResult.errors.length > 0" class="validation-errors">
                <n-data-table
                  :columns="validationErrorColumns"
                  :data="validationResult.errors.slice(0, 50)"
                  :max-height="200"
                  size="small"
                />
                <n-text v-if="validationResult.errors.length > 50" depth="3" style="font-size: 12px; margin-top: 4px;">
                  {{ t('dataImport.showFirst50', { n: validationResult.errors.length }) }}
                </n-text>
              </div>
              <div v-else class="validation-success">
                <n-icon color="#18a058" size="20"><CheckmarkCircleOutline /></n-icon>
                <span>{{ t('dataImport.allValidated') }}</span>
              </div>
              <n-form-item :label="t('dataImport.importStrategy')" style="margin-top: 12px;">
                <n-radio-group v-model:value="importStrategy">
                  <n-space>
                    <n-radio value="skipErrors">{{ t('dataImport.skipErrors') }}</n-radio>
                    <n-radio value="stopOnError">{{ t('dataImport.stopOnError') }}</n-radio>
                    <n-radio value="onlyValid">{{ t('dataImport.onlyValid') }}</n-radio>
                  </n-space>
                </n-radio-group>
              </n-form-item>
            </div>
            
            <div class="action-buttons">
              <n-button
                :disabled="!fileForm.dataSourceId || !fileForm.tableName"
                :loading="downloadingFileTemplate"
                @click="handleDownloadFileTemplate"
              >
                <template #icon><n-icon><DownloadOutline /></n-icon></template>
                {{ t('dataImport.downloadTemplate') }}
              </n-button>
              <n-button :loading="previewing" :disabled="!selectedFile" @click="handlePreview">
                <template #icon><n-icon><EyeOutline /></n-icon></template>
                {{ t('dataImport.previewData') }}
              </n-button>
              <n-button type="primary" :loading="importing" :disabled="!selectedFile" @click="handleFileImport">
                <template #icon><n-icon><CloudUploadOutline /></n-icon></template>
                {{ t('dataImport.startImport') }}
              </n-button>
            </div>
          </div>
        </n-tab-pane>
        
        <!-- 字段映射导入 -->
        <n-tab-pane name="mapped" :tab="t('dataImport.mappedImport')">
          <div class="import-section">
            <n-alert type="info" :bordered="false" style="margin-bottom: 20px;">
              {{ t('dataImport.mappedImportHint') }}
            </n-alert>
            
            <n-form label-placement="left" label-width="100px">
              <n-form-item :label="t('dataImport.targetDataSource')">
                <n-select v-model:value="mappedForm.dataSourceId" :options="dataSourceOptions" :placeholder="t('dataImport.selectTargetDataSource')" clearable filterable />
              </n-form-item>
              <n-form-item :label="t('dataImport.targetTable')">
                <n-input v-model:value="mappedForm.tableName" :placeholder="t('dataImport.targetTablePlaceholder')" />
              </n-form-item>
              <n-form-item :label="t('dataImport.selectFile')">
                <n-upload :max="1" :default-upload="false" accept=".xlsx,.xls,.csv" @change="handleMappedFileChange">
                  <n-button><template #icon><n-icon><DocumentOutline /></n-icon></template>{{ t('dataImport.selectFile') }}</n-button>
                </n-upload>
              </n-form-item>
            </n-form>
            
            <!-- 字段映射配置 -->
            <div v-if="mappedFileHeaders.length > 0" class="mapping-section">
              <div class="mapping-header">
                <span style="font-weight: 600;">{{ t('dataImport.fieldMappingConfig') }}</span>
                <n-tag type="info" size="small">{{ t('dataImport.fieldsDetected', { n: mappedFileHeaders.length }) }}</n-tag>
              </div>
              <div class="mapping-list">
                <div v-for="header in mappedFileHeaders" :key="header" class="mapping-row">
                  <div class="mapping-source">
                    <n-tag type="info" size="small">{{ header }}</n-tag>
                  </div>
                  <span class="mapping-arrow">→</span>
                  <div class="mapping-target">
                    <n-input v-model:value="fieldMappings[header]" :placeholder="header" size="small" />
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 导入模式选择（字段映射导入复用文件字段列表） -->
            <ImportModeSelector
              v-model="mappedImportMode"
              :fields="fileFieldOptions"
              @update:model-value="(cfg) => mappedImportMode = cfg"
            />
            
            <!-- 导入进度 -->
            <div v-if="mappedImporting" class="progress-section">
              <n-progress :percentage="mappedProgress" :indicator-placement="'inside'" type="line" :processing="mappedProgress < 100" />
              <span class="progress-text">{{ mappedProgressText }}</span>
            </div>
            
            <div class="action-buttons">
              <n-button
                :disabled="!mappedForm.dataSourceId || !mappedForm.tableName"
                :loading="downloadingMappedTemplate"
                @click="handleDownloadMappedTemplate"
              >
                <template #icon><n-icon><DownloadOutline /></n-icon></template>
                {{ t('dataImport.downloadTemplate') }}
              </n-button>
              <n-button type="primary" :loading="mappedImporting" :disabled="!mappedFile || !mappedForm.tableName" @click="handleMappedImport">
                <template #icon><n-icon><CloudUploadOutline /></n-icon></template>
                {{ t('dataImport.startMappedImport') }}
              </n-button>
            </div>
          </div>
        </n-tab-pane>
        
        <!-- 数据库导入 -->
        <n-tab-pane name="database" :tab="t('dataImport.dbImport')">
          <div class="import-section">
            <n-alert type="info" :bordered="false" style="margin-bottom: 20px;">
              {{ t('dataImport.dbImportHint') }}
            </n-alert>
            
            <n-form ref="dbFormRef" :model="dbForm" label-placement="left" label-width="100px">
              <n-form-item :label="t('dataImport.sourceDataSource')" path="sourceDataSourceId" required>
                <n-select
                  v-model:value="dbForm.sourceDataSourceId"
                  :options="dataSourceOptions"
                  :placeholder="t('dataImport.sourceDataSourcePlaceholder')"
                  filterable
                  @update:value="handleSourceChange"
                />
              </n-form-item>
              
              <n-form-item :label="t('dataImport.sourceTable')" path="sourceTable" required>
                <n-select
                  v-model:value="dbForm.sourceTable"
                  :options="sourceTableOptions"
                  :placeholder="t('dataImport.sourceTablePlaceholder')"
                  filterable
                  :loading="loadingTables"
                />
              </n-form-item>
              
              <n-form-item :label="t('dataImport.targetDataSource')" path="targetDataSourceId">
                <n-select
                  v-model:value="dbForm.targetDataSourceId"
                  :options="dataSourceOptions"
                  :placeholder="t('dataImport.targetDataSourcePlaceholder')"
                  clearable
                  filterable
                  @update:value="handleTargetChange"
                />
              </n-form-item>
              
              <n-form-item :label="t('dataImport.targetTable')" path="targetTable">
                <n-select
                  v-model:value="dbForm.targetTable"
                  :options="targetTableOptions"
                  :placeholder="t('dataImport.targetTableSelectPlaceholder')"
                  filterable
                  tag
                  clearable
                  :loading="loadingTargetTables"
                />
              </n-form-item>
              
              <n-form-item :label="t('dataImport.importOptions')">
                <n-space>
                  <n-checkbox v-model:checked="dbForm.autoCreateTable">{{ t('dataImport.autoCreateTable') }}</n-checkbox>
                  <n-checkbox v-model:checked="dbForm.truncateFirst">{{ t('dataImport.truncateFirst') }}</n-checkbox>
                </n-space>
              </n-form-item>
              
              <n-form-item :label="t('dataImport.filterCondition')">
                <n-input
                  v-model:value="dbForm.whereClause"
                  type="textarea"
                  :placeholder="t('dataImport.filterPlaceholder')"
                  :rows="2"
                />
              </n-form-item>
            </n-form>
            
            <!-- 导入模式选择 -->
            <ImportModeSelector
              ref="dbImportModeRef"
              v-model="dbImportMode"
              :fields="sourceFieldOptions"
              :is-database="true"
              @update:model-value="handleDbModeChange"
              @get-max-value="handleGetDbMaxValue"
            />
            
            <div class="action-buttons">
              <n-button
                :disabled="!dbForm.sourceDataSourceId || !dbForm.sourceTable"
                :loading="downloadingTemplate"
                @click="handleDownloadTemplate"
              >
                <template #icon><n-icon><DownloadOutline /></n-icon></template>
                {{ t('dataImport.downloadTemplate') }}
              </n-button>
              <n-button type="primary" :loading="importing" @click="handleDbImport">
                <template #icon><n-icon><CloudUploadOutline /></n-icon></template>
                {{ t('dataImport.startImport') }}
              </n-button>
            </div>
          </div>
        </n-tab-pane>
      </n-tabs>
    </n-card>
    
    <!-- 导入历史 -->
    <n-card :bordered="false" style="margin-top: 16px;">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="22" color="var(--color-primary)" class="header-icon"><CloudUploadOutline /></n-icon>
          <span>{{ t('dataImport.importHistory') }}</span>
        </div>
      </template>
      <n-data-table
        :columns="historyColumns"
        :data="importHistory"
        :loading="loadingHistory"
        :pagination="{ pageSize: 10 }"
        :scroll-x="1000"
        size="small"
        striped
        class="custom-table"
      />
    </n-card>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { logger } from '@/utils/logger'
import { ref, reactive, computed, onMounted, onBeforeUnmount, h } from 'vue'
import { useMessage, NTag, NButton, NIcon, NProgress, UploadFileInfo } from 'naive-ui'
import { CloudUploadOutline, DocumentOutline, EyeOutline, CheckmarkCircleOutline, CloseCircleOutline, DownloadOutline, ShieldCheckmarkOutline } from '@vicons/ionicons5'
import { getDataSourceList, getDataSourceTables, getTableColumns } from '@/api/dataSource'
import { previewImportFile, importData, importFromDatabase, downloadImportTemplate, getFieldMaxValue, validateImportData } from '@/api/dataCollect'
import { importDataWithMapping, getImportProgress } from '@/api/tableData'
import { initMessage } from '@/utils/message'
import ImportModeSelector from '@/components/ImportModeSelector.vue'
import type { ImportModeConfig } from '@/components/ImportModeSelector.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const message = useMessage()
initMessage(message)

// --- 导入历史状态映射（用于 StatusTag）(Req 9.3) ---
const importStatusMap = computed(() => ({
  success: { label: t('dataImport.statusSuccess'), type: 'success' as const },
  failed: { label: t('dataImport.statusFailed'), type: 'error' as const }
}))

// --- 导入类型映射（用于 StatusTag）(Req 9.3) ---
const importTypeMap = computed(() => ({
  file: { label: t('dataImport.typeFile'), type: 'info' as const },
  database: { label: t('dataImport.typeDatabase'), type: 'success' as const }
}))

// 数据源选项
const dataSourceOptions = ref<Array<{ label: string; value: number }>>([])
const sourceTableOptions = ref<Array<{ label: string; value: string }>>([])
const targetTableOptions = ref<Array<{ label: string; value: string }>>([])
const loadingTables = ref(false)
const loadingTargetTables = ref(false)

// 文件导入表单
const fileForm = ref({
  dataSourceId: null as number | null,
  tableName: '',
  autoCreateTable: true,
  skipHeader: true,
  truncateFirst: false
})
const selectedFile = ref<File | null>(null)
const previewData = ref<any[]>([])
const previewColumns = ref<any[]>([])
const previewTotal = ref(0)
const previewing = ref(false)
const importing = ref(false)

// 导入模式配置
const fileImportMode = ref<ImportModeConfig>({ importMode: 'full' })
const mappedImportMode = ref<ImportModeConfig>({ importMode: 'full' })
const dbImportMode = ref<ImportModeConfig>({ importMode: 'full' })

// 文件导入 Tab 的字段列表（从预览表头生成）
const fileFieldOptions = computed(() => 
  previewColumns.value.map((col: any) => ({ label: col.title, value: col.key }))
)

// 数据库导入 Tab 的源表字段列表
const sourceFieldOptions = ref<Array<{ label: string; value: string }>>([])
const loadSourceFields = async () => {
  if (!dbForm.value.sourceDataSourceId || !dbForm.value.sourceTable) {
    sourceFieldOptions.value = []
    return
  }
  try {
    const res = await getTableColumns(dbForm.value.sourceDataSourceId, dbForm.value.sourceTable)
    const columns = res.data || []
    sourceFieldOptions.value = columns.map((c: any) => ({
      label: c.columnName || c.name || c,
      value: c.columnName || c.name || c
    }))
  } catch { sourceFieldOptions.value = [] }
}

// 导入模式变化时联动 truncateFirst
const handleFileModeChange = (config: ImportModeConfig) => {
  fileImportMode.value = config
  fileForm.value.truncateFirst = config.importMode === 'full'
}
const handleDbModeChange = (config: ImportModeConfig) => {
  dbImportMode.value = config
  dbForm.value.truncateFirst = config.importMode === 'full'
}

// 获取字段最大值（用于增量导入起始值）
const dbImportModeRef = ref<InstanceType<typeof ImportModeSelector> | null>(null)
const handleGetDbMaxValue = async (field: string) => {
  if (!dbForm.value.targetDataSourceId || !dbForm.value.targetTable) {
    message.warning(t('dataImport.selectTargetAndTable'))
    return
  }
  try {
    const res = await getFieldMaxValue(dbForm.value.targetDataSourceId, dbForm.value.targetTable, field)
    const maxVal = res.data?.maxValue ?? ''
    dbImportModeRef.value?.setIncrementStartValue(String(maxVal))
  } catch (error: any) {
    message.error(error.message || t('dataImport.getMaxValueFailed'))
  }
}

// 数据校验
const validating = ref(false)
const validationResult = ref<{
  totalRows: number
  passedRows: number
  failedRows: number
  warningRows: number
  errors: Array<{ row: number; field: string; message: string; type: string }>
} | null>(null)
const importStrategy = ref('skipErrors')

const validationErrorColumns = [
  { title: t('dataImport.rowNumber'), key: 'row', width: 60 },
  { title: t('dataImport.field'), key: 'field', width: 120 },
  { title: t('dataImport.issue'), key: 'message', ellipsis: { tooltip: true } },
  { title: t('dataImport.issueType'), key: 'type', width: 80,
    render: (row: any) => h(StatusTag, {
      status: row.type,
      statusMap: { error: { label: t('dataImport.errorType'), type: 'error' }, warning: { label: t('dataImport.warningType'), type: 'warning' } }
    })
  }
]

const handleValidate = async () => {
  if (!selectedFile.value || !fileForm.value.dataSourceId || !fileForm.value.tableName) {
    message.warning(t('dataImport.ensureFileAndTarget'))
    return
  }
  validating.value = true
  validationResult.value = null
  try {
    const res = await validateImportData({
      file: selectedFile.value,
      dataSourceId: fileForm.value.dataSourceId,
      tableName: fileForm.value.tableName.trim(),
      firstRowAsHeader: fileForm.value.skipHeader
    })
    validationResult.value = res.data || null
    if (res.data && res.data.failedRows === 0) {
      message.success(t('dataImport.validationPassed'))
    } else if (res.data) {
      message.warning(t('dataImport.validationDone', { failed: res.data.failedRows, warning: res.data.warningRows }))
    }
  } catch (error: any) {
    message.error(error.message || t('dataImport.validationFailed'))
  } finally {
    validating.value = false
  }
}

// 数据库导入表单
const dbForm = ref({
  sourceDataSourceId: null as number | null,
  sourceTable: '',
  targetDataSourceId: null as number | null,
  targetTable: '',
  autoCreateTable: true,
  truncateFirst: false,
  whereClause: ''
})

// 导入历史
const importHistory = ref<any[]>([])
const loadingHistory = ref(false)

// 统计
const successCount = computed(() => importHistory.value.filter(h => h.status === 'success').length)
const failedCount = computed(() => importHistory.value.filter(h => h.status === 'failed').length)

// --- 历史表格列定义（使用 StatusTag）(Req 9.3) ---
const historyColumns = computed(() => [
  { title: t('dataImport.importTime'), key: 'importTime', width: 180 },
  { title: t('dataImport.type'), key: 'type', width: 100,
    render: (row: any) => h(StatusTag, { status: row.type, statusMap: importTypeMap.value })
  },
  { title: t('dataImport.tableName'), key: 'tableName', width: 150 },
  { title: t('dataImport.rowCount'), key: 'rowCount', width: 100 },
  { title: t('dataImport.status'), key: 'status', width: 100,
    render: (row: any) => h(StatusTag, { status: row.status, statusMap: importStatusMap.value })
  },
  { title: t('dataImport.remark'), key: 'remark', ellipsis: { tooltip: true } }
])

// 加载数据源列表
const loadDataSources = async () => {
  try {
    const res = await getDataSourceList({ page: 1, pageSize: 100 })
    const list = res.data?.list || []
    dataSourceOptions.value = list.map((ds: any) => ({
      label: `${ds.name} (${ds.dbType})`,
      value: ds.id
    }))
  } catch (error) {
    logger.error('加载数据源失败:', error)
  }
}

// 源数据源变化时加载表列表
const handleSourceChange = async (value: number | null) => {
  if (!value) {
    sourceTableOptions.value = []
    sourceFieldOptions.value = []
    return
  }
  
  loadingTables.value = true
  try {
    const res = await getDataSourceTables(value)
    const tables = res.data || []
    sourceTableOptions.value = tables.map((t: any) => ({ label: t.tableName || t, value: t.tableName || t }))
  } catch (error) {
    message.error(t('dataImport.loadTablesFailed'))
  } finally {
    loadingTables.value = false
  }
  // 源表变化时加载字段列表
  loadSourceFields()
}

// 目标数据源变化时加载表列表
const handleTargetChange = async (value: number | null) => {
  dbForm.value.targetTable = ''
  if (!value) {
    targetTableOptions.value = []
    return
  }
  
  loadingTargetTables.value = true
  try {
    const res = await getDataSourceTables(value)
    const tables = res.data || []
    targetTableOptions.value = tables.map((t: any) => ({ label: t.tableName || t, value: t.tableName || t }))
  } catch (error) {
    message.error(t('dataImport.loadTargetTablesFailed'))
  } finally {
    loadingTargetTables.value = false
  }
}

// 文件选择变化
const handleFileChange = (options: { fileList: UploadFileInfo[] }) => {
  const firstFile = options.fileList[0]
  if (options.fileList.length > 0 && firstFile?.file) {
    selectedFile.value = firstFile.file
    previewData.value = []
    previewColumns.value = []
  } else {
    selectedFile.value = null
  }
}

// 预览文件数据
const handlePreview = async () => {
  if (!selectedFile.value) {
    message.warning(t('dataImport.selectFileFirst'))
    return
  }
  
  previewing.value = true
  try {
    const res = await previewImportFile(selectedFile.value, fileForm.value.skipHeader, 10)
    
    if (res.data) {
      const { headers, previewData: data, totalRows } = res.data
      
      previewColumns.value = (headers || []).map((col: string) => ({
        title: col,
        key: col,
        width: 120,
        ellipsis: { tooltip: true }
      }))
      previewData.value = data || []
      previewTotal.value = totalRows || 0
      
      if (data && data.length > 0) {
        message.success(t('dataImport.previewSuccess', { n: totalRows }))
      } else {
        message.warning(t('dataImport.fileEmpty'))
      }
    } else {
      message.warning(t('dataImport.previewEmpty'))
    }
  } catch (error: any) {
    logger.error('[DataImport] 预览失败:', error)
    message.error(error.message || t('dataImport.previewFailed'))
  } finally {
    previewing.value = false
  }
}

// 文件导入
const handleFileImport = async () => {
  if (!selectedFile.value) {
    message.warning(t('dataImport.selectFileFirst'))
    return
  }
  if (!fileForm.value.dataSourceId) {
    message.warning(t('dataImport.selectTargetDataSourceWarning'))
    return
  }
  if (!fileForm.value.tableName || !fileForm.value.tableName.trim()) {
    message.warning(t('dataImport.enterTargetTable'))
    return
  }
  
  const params = {
    file: selectedFile.value,
    dataSourceId: fileForm.value.dataSourceId,
    tableName: fileForm.value.tableName.trim(),
    autoCreateTable: fileForm.value.autoCreateTable,
    skipHeader: fileForm.value.skipHeader,
    truncateFirst: fileForm.value.truncateFirst
  }
  
  importing.value = true
  try {
    const res = await importData(params)
    const result = res.data
    
    if (result) {
      let successMsg = t('dataImport.importSuccessMsg', { success: result.successCount || 0, fail: result.failCount || 0 })
      if (result.tableTruncated) {
        successMsg += t('dataImport.tableTruncated')
      }
      if (result.tableCreated) {
        successMsg += t('dataImport.tableCreated')
      }
      message.success(successMsg)
      
      importHistory.value.unshift({
        importTime: new Date().toLocaleString(),
        type: 'file',
        tableName: result.tableName || fileForm.value.tableName,
        rowCount: result.successCount || 0,
        status: 'success',
        remark: selectedFile.value.name
      })
    } else {
      message.warning(t('dataImport.importDoneNoResult'))
    }
  } catch (error: any) {
    logger.error('[DataImport] 导入失败:', error)
    message.error(error.message || t('dataImport.importFailed'))
    importHistory.value.unshift({
      importTime: new Date().toLocaleString(),
      type: 'file',
      tableName: fileForm.value.tableName,
      rowCount: 0,
      status: 'failed',
      remark: error.message || '未知错误'
    })
  } finally {
    importing.value = false
  }
}

// 数据库导入
const handleDbImport = async () => {
  if (!dbForm.value.sourceDataSourceId) {
    message.warning(t('dataImport.selectSourceDataSource'))
    return
  }
  if (!dbForm.value.sourceTable) {
    message.warning(t('dataImport.selectSourceTable'))
    return
  }
  if (!dbForm.value.targetDataSourceId) {
    message.warning(t('dataImport.selectTargetDataSourceDb'))
    return
  }
  
  const params = {
    sourceDataSourceId: dbForm.value.sourceDataSourceId,
    sourceTable: dbForm.value.sourceTable,
    targetDataSourceId: dbForm.value.targetDataSourceId,
    targetTable: dbForm.value.targetTable || '',
    autoCreateTable: dbForm.value.autoCreateTable,
    truncateFirst: dbForm.value.truncateFirst,
    whereClause: dbForm.value.whereClause || ''
  }
  
  importing.value = true
  try {
    const res = await importFromDatabase(params)
    
    const result = res.data
    if (result) {
      const targetTableName = result.tableName || dbForm.value.targetTable || dbForm.value.sourceTable
      let successMsg = t('dataImport.importSuccessMsg', { success: result.successCount || 0, fail: result.failCount || 0 })
      if (result.tableTruncated) {
        successMsg += t('dataImport.tableTruncated')
      }
      if (result.tableCreated) {
        successMsg += t('dataImport.tableCreated')
      }
      if (result.message) {
        successMsg += ` - ${result.message}`
      }
      message.success(successMsg)
      
      importHistory.value.unshift({
        importTime: new Date().toLocaleString(),
        type: 'database',
        tableName: targetTableName,
        rowCount: result.successCount || 0,
        status: 'success',
        remark: t('dataImport.importFromSource', { source: dbForm.value.sourceTable })
      })
    } else {
      message.warning(t('dataImport.importDoneNoResult'))
    }
  } catch (error: any) {
    logger.error('[DataImport] 数据库导入失败:', error)
    message.error(error.message || t('dataImport.importFailed'))
    importHistory.value.unshift({
      importTime: new Date().toLocaleString(),
      type: 'database',
      tableName: dbForm.value.targetTable || dbForm.value.sourceTable,
      rowCount: 0,
      status: 'failed',
      remark: error.message || '未知错误'
    })
  } finally {
    importing.value = false
  }
}

// 下载导入模板（通用方法）
const doDownloadTemplate = async (dataSourceId: number, tableName: string, loadingRef: { value: boolean }) => {
  loadingRef.value = true
  try {
    const res = await downloadImportTemplate(dataSourceId, tableName)
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${tableName}_template.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    message.success(t('dataImport.templateDownloaded'))
  } catch (error: any) {
    message.error(error.message || t('dataImport.templateDownloadFailed'))
  } finally {
    loadingRef.value = false
  }
}

// 数据库导入 Tab 下载模板
const downloadingTemplate = ref(false)
const handleDownloadTemplate = async () => {
  if (!dbForm.value.sourceDataSourceId || !dbForm.value.sourceTable) {
    message.warning(t('dataImport.selectDataSourceAndTable'))
    return
  }
  await doDownloadTemplate(dbForm.value.sourceDataSourceId, dbForm.value.sourceTable, downloadingTemplate)
}

// 文件导入 Tab 下载模板
const downloadingFileTemplate = ref(false)
const handleDownloadFileTemplate = async () => {
  if (!fileForm.value.dataSourceId || !fileForm.value.tableName) {
    message.warning(t('dataImport.selectTargetAndTableName'))
    return
  }
  await doDownloadTemplate(fileForm.value.dataSourceId, fileForm.value.tableName.trim(), downloadingFileTemplate)
}

// 字段映射导入 Tab 下载模板
const downloadingMappedTemplate = ref(false)
const handleDownloadMappedTemplate = async () => {
  if (!mappedForm.dataSourceId || !mappedForm.tableName) {
    message.warning(t('dataImport.selectTargetAndTableName'))
    return
  }
  await doDownloadTemplate(mappedForm.dataSourceId, mappedForm.tableName.trim(), downloadingMappedTemplate)
}

// 字段映射导入
const mappedForm = reactive({ dataSourceId: null as number | null, tableName: '' })
const mappedFile = ref<File | null>(null)
const mappedFileHeaders = ref<string[]>([])
const fieldMappings = reactive<Record<string, string>>({})
const mappedImporting = ref(false)
const mappedProgress = ref(0)
const mappedProgressText = ref('')
let progressTimer: ReturnType<typeof setInterval> | null = null

const handleMappedFileChange = async (options: { fileList: UploadFileInfo[] }) => {
  const firstFile = options.fileList[0]
  if (options.fileList.length > 0 && firstFile?.file) {
    mappedFile.value = firstFile.file
    try {
      const res = await previewImportFile(mappedFile.value, true, 1)
      const headers = res.data?.headers || []
      mappedFileHeaders.value = headers
      headers.forEach((h: string) => { fieldMappings[h] = h })
    } catch { mappedFileHeaders.value = [] }
  } else {
    mappedFile.value = null
    mappedFileHeaders.value = []
  }
}

const handleMappedImport = async () => {
  if (!mappedFile.value || !mappedForm.tableName) {
    message.warning(t('dataImport.fillCompleteInfo'))
    return
  }
  
  const formData = new FormData()
  formData.append('file', mappedFile.value)
  formData.append('tableName', mappedForm.tableName)
  if (mappedForm.dataSourceId) formData.append('dataSourceId', String(mappedForm.dataSourceId))
  formData.append('fieldMapping', JSON.stringify(fieldMappings))
  
  mappedImporting.value = true
  mappedProgress.value = 0
  mappedProgressText.value = t('dataImport.importing')
  
  try {
    const res = await importDataWithMapping(formData)
    const taskId = res.data?.taskId
    
    if (taskId) {
      progressTimer = setInterval(async () => {
        try {
          const progressRes = await getImportProgress(taskId)
          const p = progressRes.data
          if (p) {
            mappedProgress.value = p.progress || 0
            mappedProgressText.value = p.status === 'completed' 
              ? t('dataImport.importComplete', { n: p.successCount || 0 }) 
              : t('dataImport.processed', { done: p.processedRows || 0, total: p.totalRows || '?' })
            if (p.status === 'completed' || p.status === 'failed') {
              if (progressTimer) clearInterval(progressTimer)
              if (p.status === 'completed') message.success(t('dataImport.mappedImportSuccess'))
              else message.error(t('dataImport.mappedImportFailed', { error: p.error || '' }))
              mappedImporting.value = false
            }
          }
        } catch {
          if (progressTimer) clearInterval(progressTimer)
          mappedImporting.value = false
        }
      }, 1000)
    } else {
      mappedProgress.value = 100
      mappedProgressText.value = t('dataImport.importComplete', { n: res.data?.successCount || 0 })
      message.success(t('dataImport.mappedImportSuccess'))
      mappedImporting.value = false
    }
  } catch (error: any) {
    message.error(error.message || t('dataImport.importFailed'))
    mappedImporting.value = false
  }
}

onMounted(() => {
  loadDataSources()
})

onBeforeUnmount(() => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
})
</script>

<style scoped>
.data-import-page {
  padding: 0;
}

.stat-icon-primary {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.stat-icon-success {
  background: #D1FAE5;
  color: var(--color-success);
}

.stat-icon-error {
  background: #FEE2E2;
  color: var(--color-error);
}

.import-section {
  padding: 20px 0;
}

.preview-section {
  margin-top: 20px;
  padding: var(--dp-spacing-md);
  background: #f9fafb;
  border-radius: var(--dp-radius-md);
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--dp-spacing-sm);
  font-weight: 500;
}

.validation-panel {
  margin-top: var(--dp-spacing-md);
  padding: var(--dp-spacing-md);
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: var(--dp-radius-md);
}

.validation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--dp-spacing-sm);
}

.validation-errors {
  margin-top: var(--dp-spacing-sm);
}

.validation-success {
  display: flex;
  align-items: center;
  gap: var(--dp-spacing-sm);
  padding: var(--dp-spacing-sm);
  background: #f0fdf4;
  border-radius: var(--dp-radius-sm);
  color: #166534;
  font-weight: 500;
}

.action-buttons {
  display: flex;
  gap: var(--dp-spacing-sm);
  margin-top: var(--dp-spacing-lg);
  padding-top: var(--dp-spacing-md);
  border-top: 1px solid #e8e8e8;
}

.mapping-section {
  margin-top: 20px;
  padding: var(--dp-spacing-md);
  background: #f9fafb;
  border-radius: var(--dp-radius-md);
}

.mapping-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--dp-spacing-sm);
}

.mapping-list {
  display: flex;
  flex-direction: column;
  gap: var(--dp-spacing-sm);
}

.mapping-row {
  display: flex;
  align-items: center;
  gap: var(--dp-spacing-sm);
}

.mapping-source {
  width: 140px;
  text-align: right;
}

.mapping-arrow {
  color: #999;
  font-size: var(--dp-font-lg);
}

.mapping-target {
  flex: 1;
}

.progress-section {
  margin-top: var(--dp-spacing-md);
  padding: var(--dp-spacing-sm);
  background: #f0f9ff;
  border-radius: var(--dp-radius-md);
}

.progress-text {
  display: block;
  margin-top: var(--dp-spacing-sm);
  font-size: var(--dp-font-xs);
  color: #666;
}






</style>

<style>
/* DataImport 深色模式（非 scoped） */
html.dark .stat-icon-success {
  background: rgba(16, 185, 129, 0.15) !important;
}
html.dark .stat-icon-error {
  background: rgba(239, 68, 68, 0.15) !important;
}
html.dark .preview-section {
  background: #1a2536 !important;
}
html.dark .preview-header {
  color: #e2e8f0 !important;
}
html.dark .validation-panel {
  background: rgba(245, 158, 11, 0.08) !important;
  border-color: rgba(245, 158, 11, 0.25) !important;
}
html.dark .validation-header {
  color: #e2e8f0 !important;
}
html.dark .validation-success {
  background: rgba(16, 185, 129, 0.1) !important;
  color: #34d399 !important;
}
html.dark .mapping-section {
  background: #1a2536 !important;
}
html.dark .mapping-header {
  color: #e2e8f0 !important;
}
html.dark .mapping-arrow {
  color: #64748b !important;
}
html.dark .progress-section {
  background: rgba(14, 165, 233, 0.08) !important;
}
html.dark .progress-text {
  color: #94a3b8 !important;
}
html.dark .action-buttons {
  border-top-color: #334155 !important;
}
</style>

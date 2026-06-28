<template>
  <div class="datasource-page">
    <!-- 页面头部统计 -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><ServerOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ total }}</span>
          <span class="stat-label">{{ t('dataSource.totalCount') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><CheckmarkCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ data.length }}</span>
          <span class="stat-label">{{ t('dataSource.currentPage') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info">
          <n-icon size="24"><LayersOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ dbTypeCount }}</span>
          <span class="stat-label">{{ t('dataSource.dbTypeCount') }}</span>
        </div>
      </div>
    </div>

    <n-card class="main-card">
      <template #header>
        <div class="card-header-custom">
          <div class="header-icon-wrapper">
            <n-icon size="18"><ServerOutline /></n-icon>
          </div>
          <div class="header-text">
            <span class="header-title">{{ t('dataSource.title') }}</span>
            <span class="header-subtitle">{{ t('dataSource.searchPlaceholder') }}</span>
          </div>
        </div>
      </template>
      <template #header-extra>
        <n-space>
          <FilterPanel
            :fields="filterFields"
            :model-value="activeFilters"
            @apply="handleFilterApply"
          />
          <n-dropdown trigger="click" :options="exportDropdownOptions" @select="onExportSelect">
            <n-button :loading="exporting">
              <template #icon>
                <n-icon><DownloadOutline /></n-icon>
              </template>
              {{ t('common.export') }}
            </n-button>
          </n-dropdown>
          <n-button :loading="batchTesting" @click="handleBatchTest">
            <template #icon>
              <n-icon><FlashOutline /></n-icon>
            </template>
            {{ t('dataSource.batchTest') }}
          </n-button>
          <n-button v-permission="'data:source:add'" type="primary" @click="modal.openCreate()">
            <template #icon>
              <n-icon><AddOutline /></n-icon>
            </template>
            {{ t('dataSource.addDataSource') }}
          </n-button>
        </n-space>
      </template>

      <!-- Query_Form: 搜索筛选表单 (Req 1.4) -->
      <n-form class="query-form" inline>
        <n-form-item>
          <n-input
            v-model:value="searchKeyword"
            :placeholder="t('dataSource.searchPlaceholder')"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearchReset"
          />
        </n-form-item>
        <n-form-item>
          <n-select
            v-model:value="searchDbType"
            :options="DB_TYPE_OPTIONS"
            :placeholder="t('dataSource.dbTypePlaceholder')"
            clearable
            style="min-width: 160px"
            @update:value="handleSearch"
          />
        </n-form-item>
        <n-form-item>
          <n-select
            v-model:value="searchStatus"
            :options="connectionStatusOptions"
            :placeholder="t('dataSource.statusPlaceholder')"
            clearable
            style="min-width: 140px"
            @update:value="handleSearch"
          />
        </n-form-item>
        <n-form-item class="query-form-actions">
          <n-button type="primary" @click="handleSearch">
            <template #icon>
              <n-icon><SearchOutline /></n-icon>
            </template>
            {{ t('common.search') }}
          </n-button>
          <n-button @click="handleSearchReset">{{ t('common.reset') }}</n-button>
        </n-form-item>
      </n-form>

      <n-data-table
        :columns="columns"
        :data="data"
        :loading="loading"
        :pagination="false"
        :scroll-x="1100"
        striped
        class="custom-table"
      />
      <div class="pagination-wrapper">
        <div class="pagination-info">
          <n-tag type="info" size="small" round>
            {{ t('dataSource.totalRecords', { total }) }}
          </n-tag>
        </div>
        <n-pagination
          :page="pagination.page"
          :page-size="pagination.pageSize"
          :item-count="pagination.itemCount"
          :page-sizes="[10, 20, 50, 100]"
          show-size-picker
          show-quick-jumper
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-card>
    
    <n-modal v-model:show="modal.visible.value" preset="card" :title="modalTitle" style="width: 600px; border-radius: 16px;">
      <n-form
        ref="formRef"
        :model="modal.formData.value"
        :rules="rules"
        label-placement="left"
        label-width="100px"
      >
        <n-form-item :label="t('dataSource.name')" path="name">
          <n-input v-model:value="modal.formData.value.name" :placeholder="t('dataSource.namePlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('dataSource.type')" path="dbType">
          <n-select
            v-model:value="modal.formData.value.dbType"
            :options="DB_TYPE_OPTIONS"
            :placeholder="t('dataSource.dbTypeSel')"
          />
        </n-form-item>
        <n-form-item :label="t('dataSource.host')" path="host">
          <n-input v-model:value="modal.formData.value.host" :placeholder="t('dataSource.hostPlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('dataSource.port')" path="port">
          <n-input-number 
            v-model:value="modal.formData.value.port" 
            :placeholder="t('dataSource.portPlaceholder')"
            :min="1"
            :max="65535"
            :show-button="false"
            style="width: 100%"
          />
        </n-form-item>
        <n-form-item :label="t('dataSource.database')" path="database">
          <n-input v-model:value="modal.formData.value.database" :placeholder="t('dataSource.dbPlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('dataSource.username')" path="username">
          <n-input v-model:value="modal.formData.value.username" :placeholder="t('dataSource.userPlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('dataSource.password')" path="password">
          <n-input
            v-model:value="modal.formData.value.password"
            type="password"
            :placeholder="t('dataSource.pwdPlaceholder')"
            show-password-on="click"
          />
        </n-form-item>
        <n-form-item :label="t('dataSource.group')">
          <n-select
            v-model:value="modal.formData.value.groupName"
            :options="groupOptions"
            :placeholder="t('dataSource.groupPlaceholder')"
            filterable
            tag
            clearable
          />
        </n-form-item>
        <n-collapse style="margin-bottom: 16px">
          <n-collapse-item :title="t('dataSource.advancedConfig')" name="advanced">
            <n-form-item :label="t('dataSource.maxPool')">
              <n-input-number v-model:value="modal.formData.value.maxPoolSize" :min="1" :max="200" placeholder="默认 10" style="width: 100%" />
            </n-form-item>
            <n-form-item :label="t('dataSource.minIdle')">
              <n-input-number v-model:value="modal.formData.value.minIdle" :min="0" :max="50" placeholder="默认 2" style="width: 100%" />
            </n-form-item>
            <n-form-item :label="t('dataSource.connectTimeout')">
              <n-input-number v-model:value="modal.formData.value.connectTimeout" :min="1" :max="300" placeholder="默认 30" style="width: 100%" />
            </n-form-item>
            <n-form-item :label="t('dataSource.queryTimeout')">
              <n-input-number v-model:value="modal.formData.value.queryTimeout" :min="1" :max="3600" placeholder="默认 60" style="width: 100%" />
            </n-form-item>
          </n-collapse-item>
        </n-collapse>
        <n-form-item>
          <n-space style="width: 100%; justify-content: flex-end;">
            <n-button @click="modal.close()">{{ t('common.cancel') }}</n-button>
            <n-button type="info" :loading="testingConnection" @click="handleTestConnection">
              <template #icon>
                <n-icon><FlashOutline /></n-icon>
              </template>
              {{ t('dataSource.testConnection') }}
            </n-button>
            <n-button type="primary" :loading="modal.submitting.value" @click="handleSubmit">{{ t('common.save') }}</n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-modal>

    <!-- 详情抽屉 -->
    <n-drawer v-model:show="showDetailDrawer" :width="480" placement="right">
      <n-drawer-content :title="detailData?.name || t('dataSource.detail')" closable>
        <n-spin :show="detailLoading">
          <template v-if="detailData">
            <n-descriptions :column="1" label-placement="left" bordered size="small" style="margin-bottom: 16px">
              <n-descriptions-item :label="t('dataSource.type')">{{ detailData.dbType?.toUpperCase() }}</n-descriptions-item>
              <n-descriptions-item :label="t('dataSource.host')">{{ detailData.host }}:{{ detailData.port }}</n-descriptions-item>
              <n-descriptions-item :label="t('dataSource.database')">{{ detailData.database }}</n-descriptions-item>
              <n-descriptions-item :label="t('dataSource.username')">{{ detailData.username }}</n-descriptions-item>
              <n-descriptions-item :label="t('dataSource.group')">{{ detailData.groupName || '-' }}</n-descriptions-item>
              <n-descriptions-item :label="t('dataSource.connectionStatus')">
                <StatusTag :status="detailData.lastTestResult ?? -1" :status-map="connectionStatusMap" />
              </n-descriptions-item>
              <n-descriptions-item v-if="detailData.lastTestTime" :label="t('dataSource.lastTestTime')">{{ formatDateTime(detailData.lastTestTime) }}</n-descriptions-item>
            </n-descriptions>

            <n-divider title-placement="left">表列表 ({{ detailData.tables?.length || 0 }})</n-divider>
            <n-list v-if="detailData.tables?.length" bordered size="small" style="max-height: 400px; overflow-y: auto">
              <n-list-item v-for="table in detailData.tables" :key="table.tableName">
                <div style="display: flex; justify-content: space-between; align-items: center">
                  <span>{{ table.tableName }}</span>
                  <n-tag size="tiny">{{ table.tableType }}</n-tag>
                </div>
              </n-list-item>
            </n-list>
            <n-empty v-else description="无表数据" />
          </template>
        </n-spin>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, h, onMounted } from 'vue'
import { NButton, NSpace, NIcon, NTag } from 'naive-ui'
import { useI18n } from '@/i18n'
import type { FormInst } from 'naive-ui'
import { AddOutline, ServerOutline, CheckmarkCircleOutline, LayersOutline, FlashOutline, CreateOutline, TrashOutline, DownloadOutline, SearchOutline } from '@vicons/ionicons5'
import { getDataSourceList, createDataSource, updateDataSource, deleteDataSource, testConnection, getDataSourceGroups, batchTestConnection, getDataSourceDetail } from '@/api/dataSource'
import { message } from '@/utils/message'
import { DB_TYPE_OPTIONS } from '@/constants'
import type { DataSource, DataSourceForm, DataSourceDetail } from '@/types/dataSource'
import { formatDateTime } from '@/utils/format'
import { handleApiError } from '@/utils/error'
import FilterPanel, { type FilterField } from '@/components/FilterPanel.vue'
import type { FilterCondition } from '@/types/api'
import { filtersToApiParam } from '@/utils/filterParams'
import { useCrudPage } from '@/composables/useCrudPage'
import { useExport } from '@/composables/useExport'
import StatusTag from '@/components/common/StatusTag.vue'
import ActionButtons, { type ActionConfig } from '@/components/common/ActionButtons.vue'

const { t } = useI18n()

// --- 连接状态映射（用于 StatusTag） ---
const connectionStatusMap = computed<Record<string | number, { label: string; type: 'success' | 'warning' | 'error' | 'info' | 'default' }>>(() => ({
  1: { label: t('common.normal'), type: 'success' },
  0: { label: t('common.abnormal'), type: 'error' },
  [-1]: { label: t('common.untested'), type: 'default' }
}))

// --- 扩展表单类型（包含高级配置字段） ---
type DataSourceFormExt = DataSourceForm & {
  maxPoolSize: number | null
  minIdle: number | null
  connectTimeout: number | null
  queryTimeout: number | null
} & Record<string, unknown>

// --- 使用 useCrudPage 统一管理表格 + 弹窗 (Req 1.5) ---
const activeFilters = ref<FilterCondition[]>([])
const searchKeyword = ref('')
const searchDbType = ref<string | null>(null)
const searchStatus = ref<number | null>(null)

const connectionStatusOptions = computed(() => [
  { label: t('common.normal'), value: 1 },
  { label: t('common.abnormal'), value: 0 },
  { label: t('common.untested'), value: -1 }
])

const {
  data,
  loading,
  total,
  pagination,
  modal,
  load,
  handlePageChange,
  handlePageSizeChange,
  handleDelete: crudHandleDelete
} = useCrudPage<DataSource & Record<string, unknown>, DataSourceFormExt>({
  listApi: async (params) => {
    const filtersParam = filtersToApiParam(activeFilters.value)
    return getDataSourceList({
      page: params.page,
      pageSize: params.pageSize,
      ...(searchKeyword.value ? { keyword: searchKeyword.value } : {}),
      filters: filtersParam || ''
    })
  },
  createApi: createDataSource as (data: DataSourceFormExt) => Promise<any>,
  updateApi: updateDataSource as (data: DataSourceFormExt) => Promise<any>,
  deleteApi: (id: number | string) => deleteDataSource(Number(id)),
  defaultFormData: () => ({
    id: null,
    name: '',
    dbType: '',
    host: '',
    port: 3306,
    database: '',
    username: '',
    password: '',
    groupName: '',
    maxPoolSize: null,
    minIdle: null,
    connectTimeout: null,
    queryTimeout: null
  }),
  rowKey: 'id',
  defaultPageSize: 10,
  createSuccessMsg: t('common.createSuccess'),
  updateSuccessMsg: t('common.updateSuccess'),
  deleteSuccessMsg: t('common.deleteSuccess'),
  onSubmit: async (mode, formData) => {
    const submitData = { ...formData, database: (formData.database as string)?.trim() || '' }
    if (mode === 'create') {
      await createDataSource(submitData as DataSourceForm)
      message.success(t('common.createSuccess'))
    } else {
      await updateDataSource(submitData as DataSourceForm)
      message.success(t('common.updateSuccess'))
    }
  },
  onError: (error) => {
    const errorMsg = handleApiError(error, modal.mode.value === 'edit' ? t('common.update') + t('dataSource.name') : t('common.create') + t('dataSource.name'))
    message.error(errorMsg)
  }
})

const modalTitle = computed(() => modal.mode.value === 'create' ? t('dataSource.addDataSource') : t('common.edit') + t('dataSource.name'))

// --- 使用 useExport 管理导出 ---
const { exporting, exportOptions: exportDropdownOptions, handleExportSelect: baseExportSelect } = useExport({
  defaultFilename: t('dataSource.title')
})

const onExportSelect = async (key: string) => {
  await baseExportSelect(key, data.value as any[])
}

// --- 表单相关 ---
const formRef = ref<FormInst | null>(null)
const testingConnection = ref(false)
const batchTesting = ref(false)
const showDetailDrawer = ref(false)
const detailData = ref<DataSourceDetail | null>(null)
const detailLoading = ref(false)

const dbTypeCount = computed(() => {
  const types = new Set(data.value.map(item => item.dbType))
  return types.size
})

const groupOptions = ref<Array<{label: string, value: string}>>([])

const loadGroups = async () => {
  try {
    const res = await getDataSourceGroups()
    const groups: string[] = res?.data || []
    groupOptions.value = groups.map(g => ({ label: g, value: g }))
  } catch (e) { /* ignore */ }
}

const rules = computed(() => ({
  name: { required: true, message: t('dataSource.namePlaceholder'), trigger: 'blur' },
  dbType: { required: true, message: t('dataSource.dbTypeSel'), trigger: 'change' },
  host: { required: true, message: t('dataSource.hostPlaceholder'), trigger: 'blur' },
  port: {
    required: true,
    message: t('dataSource.portPlaceholder'),
    trigger: ['blur', 'change'],
    validator: (_rule: any, value: number | null | undefined) => {
      if (value === null || value === undefined) {
        return new Error(t('dataSource.portPlaceholder'))
      }
      if (typeof value === 'number' && (value < 1 || value > 65535)) {
        return new Error(t('form.portRange'))
      }
      return true
    }
  },
  database: { required: true, message: t('dataSource.dbPlaceholder'), trigger: 'blur' },
  username: { required: true, message: t('dataSource.userPlaceholder'), trigger: 'blur' },
  password: { required: true, message: t('dataSource.pwdPlaceholder'), trigger: 'blur' }
}))

const filterFields = computed<FilterField[]>(() => [
  { label: 'ID', value: 'id', type: 'number' },
  { label: t('dataSource.name'), value: 'name', type: 'string' },
  {
    label: t('dataSource.type'),
    value: 'dbType',
    type: 'string',
    options: DB_TYPE_OPTIONS.map(o => ({ label: o.label, value: o.value }))
  },
  { label: t('dataSource.host'), value: 'host', type: 'string' },
  { label: t('dataSource.port'), value: 'port', type: 'number' },
  { label: t('dataSource.database'), value: 'database', type: 'string' },
  {
    label: t('dataSource.connectionStatus'),
    value: 'lastTestResult',
    type: 'number',
    options: [
      { label: t('common.normal'), value: '1' },
      { label: t('common.abnormal'), value: '0' },
      { label: t('common.untested'), value: '-1' }
    ]
  },
  { label: t('common.createTime'), value: 'createTime', type: 'string' }
])

// --- 表格列定义（使用 StatusTag 和 ActionButtons） ---
const getRowActions = (row: DataSource): ActionConfig[] => [
  {
    label: t('common.edit'),
    type: 'info',
    icon: CreateOutline,
    permission: 'data:source',
    onClick: () => modal.openEdit(row as DataSource & Record<string, unknown>)
  },
  {
    label: t('common.delete'),
    type: 'error',
    icon: TrashOutline,
    permission: 'data:source',
    confirm: t('common.deleteConfirm'),
    onClick: () => handleDelete(row.id)
  }
]

const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { 
    title: t('dataSource.name'), 
    key: 'name',
    render: (row: DataSource) => {
      return h('div', { class: 'datasource-name', style: 'cursor: pointer', onClick: () => handleShowDetail(row) }, [
        h('span', { class: 'name-text' }, row.name),
      ])
    }
  },
  {
    title: t('dataSource.connectionStatus'),
    key: 'lastTestResult',
    width: 100,
    render: (row: any) => {
      return h(StatusTag, {
        status: row.lastTestResult ?? -1,
        statusMap: connectionStatusMap.value
      })
    }
  },
  { 
    title: t('dataSource.type'), 
    key: 'dbType',
    width: 120,
    render: (row: DataSource) => {
      const typeColors: Record<string, 'info' | 'success' | 'warning' | 'error' | 'default'> = {
        mysql: 'info',
        postgresql: 'success',
        oracle: 'warning',
        sqlserver: 'error',
        mongodb: 'default'
      }
      return h(NTag, { 
        type: typeColors[row.dbType?.toLowerCase()] || 'default',
        size: 'small',
        round: true
      }, { default: () => row.dbType?.toUpperCase() || '-' })
    }
  },
  { title: t('dataSource.host'), key: 'host' },
  { title: t('dataSource.port'), key: 'port', width: 80 },
  { title: t('dataSource.database'), key: 'database' },
  {
    title: t('dataSource.group'),
    key: 'groupName',
    width: 100,
    render: (row: DataSource) => {
      return row.groupName ? h(NTag, { size: 'small', round: true }, { default: () => row.groupName }) : h('span', { style: 'color: #ccc' }, '-')
    }
  },
  { 
    title: t('common.createTime'), 
    key: 'createTime',
    width: 170,
    render: (row: DataSource) => formatDateTime(row.createTime)
  },
  {
    title: t('common.actions'),
    key: 'actions',
    width: 180,
    fixed: 'right',
    render: (row: DataSource) => {
      return h(ActionButtons, {
        actions: getRowActions(row),
        row
      })
    }
  }
]

// --- 事件处理 ---
const handleTestConnection = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (errors: any) => {
    if (!errors) {
      testingConnection.value = true
      try {
        await testConnection(modal.formData.value as DataSourceForm)
        message.success(t('dataSource.connectionSuccess'))
      } catch (error) {
        message.error(t('dataSource.connectionFailed'))
      } finally {
        testingConnection.value = false
      }
    }
  })
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (errors: any) => {
    if (!errors) {
      await modal.submit()
    }
  })
}

const handleDelete = (id: number) => {
  // 委托给 useCrudPage 的 handleDelete（含确认对话框）
  crudHandleDelete(id)
}

const handleFilterApply = (filters: FilterCondition[]) => {
  activeFilters.value = filters
  load()
}

const handleSearch = () => {
  // Build filter conditions from quick search fields
  const quickFilters: FilterCondition[] = []
  if (searchDbType.value) {
    quickFilters.push({ field: 'dbType', operator: 'eq', value: searchDbType.value })
  }
  if (searchStatus.value !== null && searchStatus.value !== undefined) {
    quickFilters.push({ field: 'lastTestResult', operator: 'eq', value: searchStatus.value })
  }
  // Merge with advanced filters
  const advancedFilters = activeFilters.value.filter(
    f => f.field !== 'dbType' && f.field !== 'lastTestResult'
  )
  activeFilters.value = [...advancedFilters, ...quickFilters]
  load()
}

const handleSearchReset = () => {
  searchKeyword.value = ''
  searchDbType.value = null
  searchStatus.value = null
  activeFilters.value = []
  load()
}

const handleBatchTest = async () => {
  const ids = data.value.map((row: any) => row.id)
  if (ids.length === 0) {
    message.warning(t('common.noData'))
    return
  }
  batchTesting.value = true
  try {
    const res = await batchTestConnection(ids)
    const results = res?.data || []
    const successCount = results.filter((r: any) => r.success).length
    message.success(`${t('dataSource.testConnection')}: ${successCount}/${results.length} ${t('common.success')}`)
    load()
  } catch (error) {
    message.error(t('dataSource.connectionFailed'))
  } finally {
    batchTesting.value = false
  }
}

const handleShowDetail = async (row: DataSource) => {
  showDetailDrawer.value = true
  detailLoading.value = true
  try {
    const res = await getDataSourceDetail(row.id!)
    detailData.value = res?.data || null
  } catch (error) {
    message.error(t('common.operationFailed'))
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  loadGroups()
})
</script>

<style scoped>
.datasource-page {
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
  background: var(--dp-gradient-primary, linear-gradient(135deg, #2563eb, #1e40af));
  color: #fff;
  box-shadow: 0 2px 8px var(--dp-color-primary-glow, rgba(37, 99, 235, 0.25));
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

/* 主卡片 */
.main-card {
  border-radius: var(--dp-card-radius);
}

/* 表格样式 */
.datasource-name {
  display: flex;
  align-items: center;
  gap: var(--dp-spacing-sm);
}

.name-text {
  font-weight: 500;
  color: var(--color-primary);
  transition: color 0.2s ease;
}

.datasource-name:hover .name-text {
  color: var(--color-primary-hover, #1d4ed8);
  text-decoration: underline;
}

/* 响应式 */
@media (max-width: 768px) {
  .datasource-page { padding: 0; }
  .page-header-stats { flex-direction: row; overflow-x: auto; flex-wrap: nowrap; }
  .page-header-stats .stat-item { min-width: 140px; flex: 0 0 auto; }
  .main-card { border-radius: 14px !important; }
  .pagination-wrapper { flex-direction: column; gap: 8px; }
}

</style>

<style>
/* DataSource 深色模式（非 scoped） */
html.dark .datasource-page .header-icon-wrapper { box-shadow: 0 2px 8px rgba(102, 126, 234, 0.15) !important; }
html.dark .datasource-page .header-title { color: #f1f5f9 !important; }
html.dark .datasource-page .header-subtitle { color: #64748b !important; }
html.dark .datasource-page .name-text { color: #818cf8 !important; }
html.dark .datasource-page .datasource-name:hover .name-text { color: #a5b4fc !important; }
</style>

<template>
  <div class="mobile-dataview-page">
    <MobileHeader
      :title="viewConfig?.name || '数据管理'"
      show-back
      :action-icon="RefreshOutline"
      @action="loadData"
    />

    <MobilePageShell no-tab-bar>
      <!-- 骨架屏 -->
      <template v-if="configLoading">
        <div class="dv-skeleton">
          <n-skeleton text style="width: 50%" :sharp="false" />
          <n-skeleton text :repeat="4" :sharp="false" />
          <n-skeleton style="width: 100%; height: 160px; margin-top: 12px" :sharp="false" />
        </div>
      </template>

      <!-- 视图不存在 -->
      <MobileEmpty
        v-else-if="!viewConfig"
        type="error"
        title="视图不存在"
        description="数据管理不存在或已被禁用"
      />

      <template v-else>
        <!-- 信息栏 -->
        <div class="dv-info-bar">
          <div class="dv-info-item">
            <span class="dv-info-label">数据表</span>
            <span class="dv-info-value dv-info-table">{{ viewConfig.tableName }}</span>
          </div>
          <div class="dv-info-sep"></div>
          <div class="dv-info-item">
            <span class="dv-info-label">记录数</span>
            <span class="dv-info-value">{{ totalRecords }}</span>
          </div>
          <div class="dv-info-sep"></div>
          <div class="dv-info-item">
            <span class="dv-info-label">权限</span>
            <div class="dv-info-perms">
              <span v-if="viewConfig.allowInsert" class="perm-tag perm-add">增</span>
              <span v-if="viewConfig.allowUpdate" class="perm-tag perm-edit">改</span>
              <span v-if="viewConfig.allowDelete" class="perm-tag perm-del">删</span>
              <span v-if="!viewConfig.allowInsert && !viewConfig.allowUpdate && !viewConfig.allowDelete" class="perm-tag perm-read">读</span>
            </div>
          </div>
        </div>

        <!-- 搜索栏 -->
        <div class="dv-search-bar">
          <n-input
            v-model:value="searchKeyword"
            placeholder="搜索数据..."
            clearable
            round
            size="small"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <n-icon size="16" color="#94a3b8"><SearchOutline /></n-icon>
            </template>
          </n-input>
        </div>

        <!-- 加载中指示器 -->
        <div v-if="dataLoading" class="dv-loading-bar">
          <div class="dv-loading-progress"></div>
        </div>

        <!-- 数据列表 -->
        <div v-if="dataLoading && tableData.length === 0" class="dv-skeleton">
          <div v-for="i in 3" :key="i" class="dv-card skeleton-card">
            <n-skeleton text style="width: 60%" :sharp="false" />
            <n-skeleton text style="width: 40%" size="small" :sharp="false" />
          </div>
        </div>

        <MobileEmpty
          v-else-if="tableData.length === 0 && !dataLoading"
          type="data"
          title="暂无数据"
          description="当前没有符合条件的数据"
        />

        <div v-else class="dv-list">
          <div
            v-for="(row, idx) in tableData"
            :key="getRowKeyValue(row)"
            class="dv-card"
            :style="{ animationDelay: `${Math.min(idx * 0.03, 0.25)}s` }"
            @click="handleCardClick(row, idx)"
          >
            <!-- 第一个字段作为卡片标题 -->
            <div v-if="displayColumns.length > 0" class="dv-card-header">
              <span class="dv-card-title">{{ formatCellValue(row[displayColumns[0].columnName]) }}</span>
              <span class="dv-card-idx">#{{ (currentPage - 1) * pageSize + idx + 1 }}</span>
            </div>
            <!-- 其余字段 -->
            <div class="dv-card-fields">
              <div v-for="col in displayColumns.slice(1, 5)" :key="col.columnName" class="dv-field">
                <span class="dv-field-label">{{ col.displayName || col.columnName }}</span>
                <span class="dv-field-value">{{ formatCellValue(row[col.columnName]) }}</span>
              </div>
            </div>
            <div v-if="viewConfig.allowUpdate || viewConfig.allowDelete" class="dv-card-actions">
              <n-button v-if="viewConfig.allowUpdate" size="tiny" type="primary" secondary @click.stop="handleEdit(row)">
                <template #icon><n-icon size="14"><CreateOutline /></n-icon></template>
                编辑
              </n-button>
              <n-button v-if="viewConfig.allowDelete" size="tiny" type="error" secondary @click.stop="confirmDelete(row)">
                <template #icon><n-icon size="14"><TrashOutline /></n-icon></template>
                删除
              </n-button>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div v-if="totalRecords > 0" class="dv-pagination">
          <span class="dv-page-info">第 {{ currentPage }}/{{ Math.ceil(totalRecords / pageSize) }} 页 · 共 {{ totalRecords }} 条</span>
          <n-pagination
            v-model:page="currentPage"
            :page-count="Math.ceil(totalRecords / pageSize)"
            :page-slot="3"
            size="small"
            @update:page="handlePageChange"
          />
        </div>

        <!-- 新增按钮 FAB -->
        <div v-if="viewConfig.allowInsert" class="dv-fab" @click="handleAdd">
          <n-icon size="24" color="#fff"><AddOutline /></n-icon>
        </div>
      </template>
    </MobilePageShell>

    <!-- 编辑/新增抽屉 -->
    <n-drawer v-model:show="showFormDrawer" placement="bottom" :height="formDrawerHeight" :trap-focus="false">
      <n-drawer-content :title="isEdit ? '编辑数据' : '新增数据'" closable>
        <n-scrollbar style="max-height: calc(100vh - 180px);">
          <n-form ref="formRef" :model="editForm" label-placement="top" size="small" class="dv-form">
            <n-form-item
              v-for="col in editableColumns"
              :key="col.columnName"
              :label="col.displayName || col.columnName"
            >
              <n-input-number
                v-if="getFieldType(col.dataType) === 'number'"
                v-model:value="editForm[col.columnName]"
                :placeholder="'请输入' + (col.displayName || col.columnName)"
                style="width: 100%"
                clearable
              />
              <n-date-picker
                v-else-if="getFieldType(col.dataType) === 'date'"
                v-model:value="editForm[col.columnName]"
                type="datetime"
                :placeholder="'请选择' + (col.displayName || col.columnName)"
                style="width: 100%"
                clearable
              />
              <n-input
                v-else-if="isLongText(col.dataType)"
                v-model:value="editForm[col.columnName]"
                :placeholder="'请输入' + (col.displayName || col.columnName)"
                type="textarea"
                :rows="3"
              />
              <n-input
                v-else
                v-model:value="editForm[col.columnName]"
                :placeholder="'请输入' + (col.displayName || col.columnName)"
                clearable
              />
            </n-form-item>
          </n-form>
        </n-scrollbar>
        <template #footer>
          <div class="dv-form-footer">
            <n-button @click="showFormDrawer = false">取消</n-button>
            <n-button type="primary" :loading="saving" @click="handleSave">
              {{ isEdit ? '保存' : '新增' }}
            </n-button>
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import MobileHeader from '@/components/mobile/MobileHeader.vue'
import MobilePageShell from '@/components/mobile/MobilePageShell.vue'
import MobileEmpty from '@/components/mobile/MobileEmpty.vue'
import {
  NButton, NIcon, NInput, NInputNumber, NDatePicker, NDrawer, NDrawerContent,
  NSkeleton, NPagination, NScrollbar, NForm, NFormItem,
  useMessage, useDialog
} from 'naive-ui'
import { SearchOutline, RefreshOutline, AddOutline, CreateOutline, TrashOutline } from '@vicons/ionicons5'
import * as dataViewApi from '@/api/dataView'
import * as tableDataApi from '@/api/tableData'
import { saveRecentVisit } from '@/utils/recentVisits'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const message = useMessage()
const dialog = useDialog()

const configLoading = ref(true)
const dataLoading = ref(false)
const saving = ref(false)

const viewConfig = ref<any>(null)
const columns = ref<any[]>([])
const tableData = ref<any[]>([])
const searchKeyword = ref('')
const primaryKeys = ref<string[]>(['id'])

const currentPage = ref(1)
const pageSize = 20
const totalRecords = ref(0)
let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null
let dataLoadVersion = 0

const showFormDrawer = ref(false)
const isEdit = ref(false)
const editForm = reactive<Record<string, any>>({})

const formDrawerHeight = computed(() => {
  const colCount = editableColumns.value.length
  return Math.min(Math.max(colCount * 80 + 180, 360), window.innerHeight * 0.85)
})

const displayColumns = computed(() => {
  return columns.value.filter(c => c.visible)
})

const editableColumns = computed(() => {
  return displayColumns.value.filter(c => !c.isPrimaryKey)
})

function getFieldType(dataType: string): 'string' | 'number' | 'date' {
  const type = dataType?.toLowerCase() || ''
  if (type.includes('int') || type.includes('decimal') || type.includes('float') || type.includes('double') || type.includes('number') || type.includes('numeric')) {
    return 'number'
  }
  if (type.includes('date') || type.includes('time')) {
    return 'date'
  }
  return 'string'
}

function isLongText(dataType: string): boolean {
  const type = dataType?.toLowerCase() || ''
  return type.includes('text') || type.includes('blob') || type.includes('clob')
}

function getRowKeyValue(row: any): string {
  return primaryKeys.value.map(pk => String(row[pk] ?? '')).join('_')
}

function formatCellValue(val: any): string {
  if (val === null || val === undefined) return '-'
  if (typeof val === 'object') return JSON.stringify(val)
  return String(val)
}

async function loadViewConfig() {
  const code = route.params['code'] as string
  if (!code) {
    configLoading.value = false
    return
  }

  try {
    const res = await dataViewApi.getDataViewByCode(code)
    viewConfig.value = res.data

    if (viewConfig.value) {
      saveRecentVisit(`/m/data-view/${code}`, viewConfig.value.name || '数据管理')

      if (viewConfig.value.columnsConfig) {
        columns.value = JSON.parse(viewConfig.value.columnsConfig)
      }

      const pkCols = columns.value.filter(c => c.isPrimaryKey)
      if (pkCols.length > 0) {
        primaryKeys.value = pkCols.map(c => c.columnName)
      }

      await loadData()
    }
  } catch (e) {
    console.error('加载视图配置失败', e)
  } finally {
    configLoading.value = false
  }
}

async function loadData() {
  if (!viewConfig.value?.tableName) return

  const version = ++dataLoadVersion
  dataLoading.value = true
  try {
    // 使用参数化搜索，防止SQL注入
    const queryParams: Parameters<typeof tableDataApi.getTableData>[0] = {
      dataSourceId: viewConfig.value.dataSourceId,
      tableName: viewConfig.value.tableName,
      page: currentPage.value,
      pageSize,
      orderBy: viewConfig.value.defaultOrderBy
        ? `${viewConfig.value.defaultOrderBy} ${viewConfig.value.defaultOrderDir || 'DESC'}`
        : ''
    }
    if (searchKeyword.value) {
      queryParams.searchKeyword = searchKeyword.value
      const searchCols = displayColumns.value
        .filter(col => getFieldType(col.dataType) === 'string')
        .map(col => col.columnName)
      if (searchCols.length > 0) {
        queryParams.searchColumns = searchCols
      }
    }

    const res = await tableDataApi.getTableData(queryParams)
    if (version !== dataLoadVersion) return // 竞态保护

    tableData.value = res.data?.list || []
    totalRecords.value = res.data?.total || 0
  } catch (e: any) {
    if (version !== dataLoadVersion) return
    message.error(e?.message || '加载数据失败')
  } finally {
    if (version === dataLoadVersion) dataLoading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  if (searchDebounceTimer) clearTimeout(searchDebounceTimer)
  searchDebounceTimer = setTimeout(() => loadData(), 300)
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadData()
}

function handleCardClick(_row: any, _idx: number) {
  // 点击卡片暂时不做操作，通过按钮操作
}

function handleAdd() {
  isEdit.value = false
  Object.keys(editForm).forEach(key => delete editForm[key])
  editableColumns.value.forEach(col => {
    editForm[col.columnName] = null
  })
  showFormDrawer.value = true
}

function handleEdit(row: any) {
  isEdit.value = true
  Object.keys(editForm).forEach(key => delete editForm[key])

  // 复制主键值
  for (const pk of primaryKeys.value) {
    editForm[pk] = row[pk]
  }

  // 复制所有可见字段
  displayColumns.value.forEach(col => {
    const value = row[col.columnName]
    const dataType = col.dataType?.toLowerCase() || ''

    if ((dataType.includes('date') || dataType.includes('time')) && value != null) {
      if (typeof value === 'number') {
        editForm[col.columnName] = value
      } else if (typeof value === 'string') {
        const timestamp = new Date(value).getTime()
        editForm[col.columnName] = isNaN(timestamp) ? null : timestamp
      } else {
        editForm[col.columnName] = null
      }
    } else {
      editForm[col.columnName] = value !== undefined ? value : null
    }
  })

  showFormDrawer.value = true
}

function confirmDelete(row: any) {
  dialog.warning({
    title: '确认删除',
    content: '确定要删除这条数据吗？此操作不可恢复。',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: () => handleDelete(row)
  })
}

async function handleDelete(row: any) {
  try {
    const pkValues: Record<string, any> = {}
    for (const pk of primaryKeys.value) {
      pkValues[pk] = row[pk]
    }

    await tableDataApi.deleteRow({
      dataSourceId: viewConfig.value.dataSourceId,
      tableName: viewConfig.value.tableName,
      primaryKeys: primaryKeys.value,
      primaryValues: pkValues
    })
    message.success('删除成功')
    await loadData()
  } catch (e: any) {
    message.error(e?.message || '删除失败')
  }
}

async function handleSave() {
  if (!viewConfig.value?.tableName) return

  saving.value = true
  try {
    const submitData: Record<string, any> = {}

    editableColumns.value.forEach(col => {
      const value = editForm[col.columnName]
      const dataType = col.dataType?.toLowerCase() || ''

      if ((dataType.includes('date') || dataType.includes('time')) && value != null && value !== '') {
        if (typeof value === 'number') {
          submitData[col.columnName] = formatDateTime(value, dataType.includes('datetime') || dataType.includes('timestamp') ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD')
        } else {
          submitData[col.columnName] = value
        }
      } else if (value !== undefined && value !== null && value !== '') {
        submitData[col.columnName] = value
      } else if (value === 0 || value === false) {
        submitData[col.columnName] = value
      }
    })

    if (isEdit.value) {
      const pkValues: Record<string, any> = {}
      for (const pk of primaryKeys.value) {
        const pkValue = editForm[pk]
        if (pkValue == null || pkValue === '') {
          message.error(`主键"${pk}"值不能为空`)
          return
        }
        pkValues[pk] = pkValue
      }

      await tableDataApi.updateRow({
        dataSourceId: viewConfig.value.dataSourceId,
        tableName: viewConfig.value.tableName,
        data: submitData,
        primaryKeys: primaryKeys.value,
        primaryValues: pkValues
      })
      message.success('更新成功')
    } else {
      if (Object.keys(submitData).length === 0) {
        message.error('请至少填写一个字段')
        return
      }

      await tableDataApi.insertRow({
        dataSourceId: viewConfig.value.dataSourceId,
        tableName: viewConfig.value.tableName,
        data: submitData
      })
      message.success('新增成功')
    }

    showFormDrawer.value = false
    await loadData()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

watch(() => route.params['code'], () => {
  configLoading.value = true
  loadViewConfig()
})

onMounted(() => {
  loadViewConfig()
})

onUnmounted(() => {
  if (searchDebounceTimer) clearTimeout(searchDebounceTimer)
})
</script>

<style scoped>
.mobile-dataview-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* 信息栏 */
.dv-info-bar {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 14px;
  padding: 12px 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.dv-info-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.dv-info-label {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 500;
}

.dv-info-value {
  font-size: 14px;
  color: #1e293b;
  font-weight: 700;
}

.dv-info-table {
  font-size: 12px;
  font-family: 'SF Mono', 'Menlo', monospace;
  font-weight: 600;
  color: #3b82f6;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dv-info-sep {
  width: 1px;
  height: 28px;
  background: #e2e8f0;
  flex-shrink: 0;
}

.dv-info-perms {
  display: flex;
  gap: 3px;
}

.perm-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
}

.perm-add { background: rgba(16, 185, 129, 0.12); color: #059669; }
.perm-edit { background: rgba(59, 130, 246, 0.12); color: #2563eb; }
.perm-del { background: rgba(239, 68, 68, 0.1); color: #ef4444; }
.perm-read { background: rgba(148, 163, 184, 0.15); color: #64748b; }

/* 加载进度条 */
.dv-loading-bar {
  height: 2px;
  background: #e2e8f0;
  border-radius: 1px;
  margin-bottom: 8px;
  overflow: hidden;
}

.dv-loading-progress {
  height: 100%;
  width: 40%;
  background: linear-gradient(90deg, #3b82f6, #6366f1);
  border-radius: 1px;
  animation: loadingSlide 1.2s ease-in-out infinite;
}

@keyframes loadingSlide {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(350%); }
}

.dv-skeleton {
  padding: 16px 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dv-search-bar {
  margin-bottom: 12px;
}

.dv-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.dv-card {
  background: #fff;
  border-radius: 14px;
  padding: 14px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: transform 0.15s;
  animation: cardSlideIn 0.3s ease backwards;
}

.dv-card:active {
  transform: scale(0.98);
}

@keyframes cardSlideIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.skeleton-card { pointer-events: none; }

/* 卡片标题 */
.dv-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
}

.dv-card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.dv-card-idx {
  font-size: 11px;
  color: #cbd5e1;
  font-weight: 600;
  font-family: 'SF Mono', 'Menlo', monospace;
  flex-shrink: 0;
  margin-left: 8px;
}

.dv-card-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 16px;
}

.dv-field {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.dv-field-label {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 500;
}

.dv-field-value {
  font-size: 13px;
  color: #1e293b;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dv-card-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
}

.dv-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 4px;
}

.dv-page-info {
  font-size: 12px;
  color: #94a3b8;
}

.dv-fab {
  position: fixed;
  right: 20px;
  bottom: calc(80px + env(safe-area-inset-bottom, 0px));
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #f59e0b, #d97706);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(245, 158, 11, 0.35);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: transform 0.2s;
  z-index: 100;
}

.dv-fab:active {
  transform: scale(0.9);
}

.dv-form {
  padding: 4px 0;
}

.dv-form-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

/* 深色模式 - 搜索框 */

/* 深色模式 - 骨架屏 */

/* 深色模式 - 分页 */

/* 深色模式 - 表单抽屉 */
</style>

<style>
/* MobileDataViewPage 深色模式（非 scoped） */
html.dark .dv-info-bar {
  background: #1e293b !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important;
}
html.dark .dv-info-label { color: #64748b !important; }
html.dark .dv-info-value { color: #e2e8f0 !important; }
html.dark .dv-info-table { color: #60a5fa !important; }
html.dark .dv-info-sep { background: #334155 !important; }
html.dark .perm-add { background: rgba(16, 185, 129, 0.15) !important; color: #34d399 !important; }
html.dark .perm-edit { background: rgba(96, 165, 250, 0.15) !important; color: #60a5fa !important; }
html.dark .perm-del { background: rgba(239, 68, 68, 0.15) !important; color: #f87171 !important; }
html.dark .perm-read { background: rgba(148, 163, 184, 0.1) !important; color: #94a3b8 !important; }
html.dark .dv-loading-bar { background: #334155 !important; }
html.dark .dv-loading-progress { background: linear-gradient(90deg, #6366f1, #818cf8) !important; }
html.dark .dv-card {
  background: #1e293b !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important;
}
html.dark .dv-card:active { background: #263449 !important; }
html.dark .dv-card-header { border-bottom-color: rgba(255, 255, 255, 0.06) !important; }
html.dark .dv-card-title { color: #e2e8f0 !important; }
html.dark .dv-card-idx { color: #475569 !important; }
html.dark .dv-field-label { color: #64748b !important; }
html.dark .dv-field-value { color: #e2e8f0 !important; }
html.dark .dv-card-actions {
  border-top-color: rgba(255, 255, 255, 0.06) !important;
}
html.dark .dv-pagination { color: #94a3b8 !important; }
html.dark .dv-page-info { color: #64748b !important; }
html.dark .dv-fab {
  background: linear-gradient(135deg, #d97706, #b45309) !important;
  box-shadow: 0 4px 16px rgba(217, 119, 6, 0.25) !important;
}
html.dark .dv-search-bar .n-input {
  background: #1e293b !important;
  border-color: #334155 !important;
}
html.dark .dv-search-bar .n-input .n-input__input-el {
  color: #e2e8f0 !important;
  caret-color: #60a5fa !important;
}
html.dark .dv-search-bar .n-input .n-input__placeholder {
  color: #475569 !important;
}
html.dark .dv-search-bar .n-input .n-input__suffix .n-icon,
html.dark .dv-search-bar .n-input .n-input__suffix .n-icon,
html.dark .dv-search-bar .n-input .n-input__prefix .n-icon {
  color: #475569 !important;
}
html.dark .dv-skeleton .n-skeleton {
  background: #334155 !important;
}
html.dark .skeleton-card {
  background: #1e293b !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important;
}
html.dark .dv-pagination .n-pagination .n-pagination-item {
  color: #94a3b8 !important;
  border-color: #334155 !important;
  background: #1e293b !important;
}
html.dark .dv-pagination .n-pagination .n-pagination-item--active {
  color: #60a5fa !important;
  border-color: #60a5fa !important;
  background: rgba(96, 165, 250, 0.1) !important;
}
html.dark .dv-form .n-input {
  background: #0f172a !important;
  border-color: #334155 !important;
}
html.dark .dv-form .n-input .n-input__input-el,
html.dark .dv-form .n-input .n-input__input-el,
html.dark .dv-form .n-input .n-input__textarea-el {
  color: #e2e8f0 !important;
  caret-color: #60a5fa !important;
}
html.dark .dv-form .n-input .n-input__placeholder {
  color: #475569 !important;
}
html.dark .dv-form .n-input-number {
  background: #0f172a !important;
  border-color: #334155 !important;
}
html.dark .dv-form .n-form-item-label__text {
  color: #94a3b8 !important;
}
</style>

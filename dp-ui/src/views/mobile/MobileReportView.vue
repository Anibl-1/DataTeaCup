<template>
  <div class="mobile-report-view">
    <!-- 顶部导航（复用 MobileHeader） -->
    <MobileHeader
      :title="reportDef?.reportName || '报表查看'"
      show-back
      :action-icon="RefreshOutline"
      @action="handleRefresh"
    />

    <MobilePageShell no-tab-bar>
    <!-- 骨架屏 -->
    <template v-if="loading && !reportDef">
      <div class="report-info-bar">
        <n-skeleton text style="width: 80px" :sharp="false" />
        <n-skeleton text style="width: 150px" :sharp="false" />
      </div>
      <div class="table-section" style="padding: 16px;">
        <n-skeleton text :repeat="3" :sharp="false" />
        <n-skeleton text style="width: 70%; margin-top: 8px" :sharp="false" />
        <div style="margin-top: 16px;">
          <n-skeleton style="width: 100%; height: 200px" :sharp="false" />
        </div>
      </div>
    </template>

    <!-- 报表信息 -->
    <div v-else-if="reportDef" class="report-info-bar">
      <n-tag type="info" size="small" round>{{ reportDef.reportCode }}</n-tag>
      <span v-if="reportDef.description" class="report-info-desc">{{ reportDef.description }}</span>
    </div>

    <!-- 参数面板 -->
    <div v-if="reportDef?.params && reportDef.params.length > 0" class="param-section">
      <div class="param-toggle" @click="paramExpanded = !paramExpanded">
        <n-icon size="16"><FunnelOutline /></n-icon>
        <span>查询参数 ({{ reportDef.params.length }})</span>
        <n-icon size="14" :style="{ transform: paramExpanded ? 'rotate(180deg)' : '' }"><ChevronDownOutline /></n-icon>
      </div>
      <Transition name="param-expand">
      <div v-if="paramExpanded" class="param-body">
        <div v-for="param in reportDef.params" :key="param.name" class="param-item">
          <label>{{ param.label }}<span v-if="param.required" class="required">*</span></label>
          <n-input
            v-if="param.inputType === 'text'"
            v-model:value="paramValues[param.name]"
            :placeholder="'请输入' + param.label"
            size="small"
            clearable
          />
          <n-input-number
            v-else-if="param.inputType === 'number'"
            v-model:value="paramValues[param.name]"
            :placeholder="'请输入' + param.label"
            size="small"
            clearable
            style="width: 100%"
          />
          <n-date-picker
            v-else-if="param.inputType === 'date'"
            v-model:formatted-value="paramValues[param.name]"
            type="date"
            value-format="yyyy-MM-dd"
            size="small"
            clearable
            style="width: 100%"
          />
          <n-date-picker
            v-else-if="param.inputType === 'daterange'"
            v-model:formatted-value="paramValues[param.name]"
            type="daterange"
            value-format="yyyy-MM-dd"
            size="small"
            clearable
            style="width: 100%"
          />
          <n-select
            v-else-if="param.inputType === 'select'"
            v-model:value="paramValues[param.name]"
            :options="getParamSelectOptions(param)"
            :placeholder="'请选择' + param.label"
            size="small"
            clearable
            filterable
          />
          <n-tree-select
            v-else-if="param.inputType === 'department'"
            v-model:value="paramValues[param.name]"
            :options="departmentTreeOptions"
            key-field="id"
            label-field="deptName"
            children-field="children"
            :placeholder="'请选择' + param.label"
            size="small"
            clearable
            filterable
          />
          <n-input
            v-else
            v-model:value="paramValues[param.name]"
            :placeholder="'请输入' + param.label"
            size="small"
            clearable
          />
        </div>
        <n-button type="primary" size="small" block @click="executeQuery">查询</n-button>
      </div>
      </Transition>
    </div>

    <!-- 数据表格 -->
    <div class="table-section">
      <n-spin :show="loading">
        <div v-if="error" class="report-error">
          <n-result status="error" title="查询失败" :description="error">
            <template #footer>
              <n-button type="primary" size="small" @click="retryQuery">重新查询</n-button>
            </template>
          </n-result>
        </div>
        <MobileEmpty v-else-if="tableData.length === 0 && !loading" type="data" title="暂无数据" description="请尝试调整查询参数后重新查询" />
        <div v-else class="table-wrapper">
          <n-data-table
            :columns="tableColumns"
            :data="tableData"
            :bordered="false"
            size="small"
            :scroll-x="scrollX"
            max-height="55vh"
            striped
          />
        </div>
      </n-spin>
    </div>

    <!-- 分页 -->
    <div v-if="totalRecords > 0" class="pagination-bar">
      <span class="page-info">共 {{ totalRecords }} 条</span>
      <n-pagination
        v-model:page="currentPage"
        :page-count="Math.ceil(totalRecords / pageSize)"
        :page-slot="3"
        size="small"
        @update:page="handlePageChange"
      />
    </div>
    </MobilePageShell>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import MobileHeader from '@/components/mobile/MobileHeader.vue'
import MobilePageShell from '@/components/mobile/MobilePageShell.vue'
import MobileEmpty from '@/components/mobile/MobileEmpty.vue'
import { NButton, NIcon, NTag, NInput, NInputNumber, NDatePicker, NSelect, NTreeSelect, NSpin, NResult, NDataTable, NPagination, NSkeleton } from 'naive-ui'
import { RefreshOutline, FunnelOutline, ChevronDownOutline } from '@vicons/ionicons5'
import { getReportDefinitionById, executeReportQuery } from '@/api/reportDefinition'
import { getDictByType } from '@/api/system/dataDictionary'
import { getDepartmentTree } from '@/api/org/department'
import { saveRecentVisit } from '@/utils/recentVisits'
import type { ReportDefinition, ReportParam } from '@/types/reportDefinition'

const route = useRoute()

const reportDef = ref<ReportDefinition | null>(null)
const tableData = ref<any[]>([])
const tableColumns = ref<any[]>([])
const scrollX = ref(800)
const loading = ref(false)
const error = ref('')
let queryVersion = 0
const paramExpanded = ref(false)
const paramValues = ref<Record<string, any>>({})
const paramDictOptions = ref<Record<string, Array<{ label: string; value: string }>>>({})
const departmentTreeOptions = ref<any[]>([])
const currentPage = ref(1)
const pageSize = 15
const totalRecords = ref(0)

function getParamSelectOptions(p: ReportParam): Array<{ label: string; value: string }> {
  if (p.dictType && paramDictOptions.value[p.dictType]) {
    return paramDictOptions.value[p.dictType]!
  }
  return (p.options as Array<{ label: string; value: string }>) || []
}

async function loadParamResources() {
  const params = reportDef.value?.params
  if (!params || params.length === 0) return
  let needDept = false
  const dictTypes = new Set<string>()
  for (const p of params) {
    if (p.inputType === 'department') needDept = true
    if (p.inputType === 'select' && p.dictType) dictTypes.add(p.dictType)
  }
  if (needDept) {
    try {
      const res: any = await getDepartmentTree()
      departmentTreeOptions.value = res?.data || []
    } catch { /* ignore */ }
  }
  for (const dt of dictTypes) {
    if (!paramDictOptions.value[dt]) {
      try {
        const res: any = await getDictByType(dt)
        const list = res?.data || []
        paramDictOptions.value[dt] = list.map((d: any) => ({
          label: d.dictLabel || d.dictCode,
          value: d.dictValue || d.dictCode
        }))
      } catch { /* ignore */ }
    }
  }
}

function initParamDefaults() {
  if (!reportDef.value?.params) return
  for (const p of reportDef.value.params) {
    if (paramValues.value[p.name] !== undefined) continue
    if (p.defaultValue) {
      if (p.inputType === 'number') {
        paramValues.value[p.name] = Number(p.defaultValue)
      } else if (p.inputType === 'daterange' && p.defaultValue.includes(',')) {
        paramValues.value[p.name] = p.defaultValue.split(',')
      } else {
        paramValues.value[p.name] = p.defaultValue
      }
    } else {
      paramValues.value[p.name] = null
    }
  }
}

async function loadReport() {
  const id = Number(route.params['id'])
  if (!id) return

  loading.value = true
  error.value = ''
  try {
    const res = await getReportDefinitionById(id) as any
    reportDef.value = res?.data?.data || res?.data || null
    if (reportDef.value) {
      saveRecentVisit(`/m/report/${id}`, reportDef.value.reportName || '报表')
      initParamDefaults()
      await loadParamResources()
      await executeQuery()
    }
  } catch (e: any) {
    error.value = e?.message || '加载报表失败'
    loading.value = false
  }
}

async function executeQuery() {
  if (!reportDef.value) return
  const version = ++queryVersion
  loading.value = true
  error.value = ''
  try {
    // 构建参数（与桌面端 DynamicReport.vue 对齐）
    const paramObj: Record<string, any> = {}
    if (reportDef.value.params) {
      for (const p of reportDef.value.params) {
        const val = paramValues.value[p.name]
        if (val === null || val === undefined || val === '') continue
        if (p.inputType === 'daterange' && Array.isArray(val)) {
          paramObj[p.name] = val.join(',')
        } else {
          paramObj[p.name] = String(val)
        }
      }
    }
    const paramsStr = Object.keys(paramObj).length > 0 ? JSON.stringify(paramObj) : ''
    const res = await executeReportQuery(reportDef.value.id, {
      page: currentPage.value,
      pageSize,
      params: paramsStr
    }) as any
    if (version !== queryVersion) return // 竞态保护

    // 解析响应数据（与桌面端 DynamicReport.vue 对齐）
    let pageResult: any = null
    if (res && typeof res === 'object') {
      if (res.data && typeof res.data === 'object' && 'list' in res.data) {
        pageResult = res.data
      } else if ('list' in res) {
        pageResult = res
      } else if (res.data?.data && typeof res.data.data === 'object' && 'list' in res.data.data) {
        pageResult = res.data.data
      }
    }
    const records = pageResult?.list || pageResult?.records || []
    tableData.value = records
    totalRecords.value = pageResult?.total ?? records.length

    // 构建表头
    if (reportDef.value.fields && reportDef.value.fields.length > 0) {
      tableColumns.value = reportDef.value.fields
        .filter(f => f.isVisible !== 0)
        .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
        .map(f => ({
          title: f.fieldLabel || f.fieldName,
          key: f.fieldName,
          width: f.width || 120,
          ellipsis: { tooltip: true }
        }))
    } else if (records.length > 0) {
      const keys = Object.keys(records[0])
      tableColumns.value = keys.map(k => ({
        title: k,
        key: k,
        minWidth: 100,
        ellipsis: { tooltip: true }
      }))
    }
    scrollX.value = tableColumns.value.length * 130
  } catch (e: any) {
    if (version !== queryVersion) return
    error.value = e?.message || '查询失败'
  } finally {
    if (version === queryVersion) loading.value = false
  }
}

function retryQuery() {
  error.value = ''
  executeQuery()
}

function handleRefresh() {
  currentPage.value = 1
  executeQuery()
}

function handlePageChange(p: number) {
  currentPage.value = p
  // 翻页后滚动到顶部
  const scrollEl = document.querySelector('.n-layout-scroll-container') as HTMLElement
  if (scrollEl) {
    scrollEl.scrollTo({ top: 0, behavior: 'smooth' })
  } else {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
  executeQuery()
}

onMounted(() => {
  loadReport()
})

watch(() => route.params['id'], () => {
  loadReport()
})
</script>

<style scoped>
.mobile-report-view {
  display: flex;
  flex-direction: column;
}

.report-info-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  margin: 0 12px 8px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.report-info-desc {
  font-size: 12px;
  color: #94a3b8;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.param-section {
  background: #fff;
  margin: 0 12px 8px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.param-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.param-toggle .n-icon:last-child {
  margin-left: auto;
  transition: transform 0.2s;
}

.param-body {
  padding: 0 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.param-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.param-item label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.param-item .required {
  color: #ef4444;
  margin-left: 2px;
}

.table-section {
  flex: 1;
  margin: 0 12px;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  min-height: 200px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.table-wrapper {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.report-error {
  padding: 40px 20px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 12px;
  color: #94a3b8;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  margin: 10px 12px 16px;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.page-info {
  font-size: 12px;
  color: #94a3b8;
}

/* 参数面板展开/收起动画 */
.param-expand-enter-active,
.param-expand-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
  max-height: 400px;
}
.param-expand-enter-from,
.param-expand-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  padding-bottom: 0;
}

/* 深色模式 - 表单控件 */

/* 深色模式 - 分页 */

/* 深色模式 - 骨架屏 */

/* 深色模式 - 表格 */

</style>

<style>
/* MobileReportView 深色模式（非 scoped） */
html.dark .report-info-bar { background: #1e293b !important; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important; }
html.dark .report-info-desc { color: #64748b !important; }
html.dark .param-section { background: #1e293b !important; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important; }
html.dark .param-toggle { color: #e2e8f0 !important; }
html.dark .param-toggle:active { background: #334155 !important; }
html.dark .param-item label { color: #94a3b8 !important; }
html.dark .table-section { background: #1e293b !important; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important; }
html.dark .pagination-bar { background: #1e293b !important; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important; }
html.dark .page-info { color: #64748b !important; }
html.dark .empty-state { color: #64748b !important; }
html.dark .param-item .n-input,
html.dark .param-item .n-input,
html.dark .param-item .n-input-number {
  background: #0f172a !important;
  border-color: #334155 !important;
}
html.dark .param-item .n-input .n-input__input-el,
html.dark .param-item .n-input .n-input__input-el,
html.dark .param-item .n-input .n-input__textarea-el {
  color: #e2e8f0 !important;
  caret-color: #60a5fa !important;
}
html.dark .param-item .n-input .n-input__placeholder {
  color: #475569 !important;
}
html.dark .param-item .n-base-selection {
  background: #0f172a !important;
  border-color: #334155 !important;
}
html.dark .param-item .n-base-selection .n-base-selection-label {
  color: #e2e8f0 !important;
}
html.dark .param-item .n-base-selection .n-base-selection-placeholder__inner {
  color: #475569 !important;
}
html.dark .pagination-bar .n-pagination .n-pagination-item {
  color: #94a3b8 !important;
  border-color: #334155 !important;
  background: #1e293b !important;
}
html.dark .pagination-bar .n-pagination .n-pagination-item--active {
  color: #60a5fa !important;
  border-color: #60a5fa !important;
  background: rgba(96, 165, 250, 0.1) !important;
}
html.dark .report-info-bar .n-skeleton,
html.dark .report-info-bar .n-skeleton,
html.dark .table-section .n-skeleton {
  background: #334155 !important;
}
html.dark .table-wrapper .n-data-table {
  background: #1e293b !important;
}
html.dark .table-wrapper .n-data-table-wrapper {
  background: #1e293b !important;
}
html.dark .table-wrapper .n-data-table-th {
  background: #334155 !important;
  color: #f1f5f9 !important;
  border-color: #475569 !important;
}
html.dark .table-wrapper .n-data-table-td {
  background: #1e293b !important;
  color: #e2e8f0 !important;
  border-color: #334155 !important;
}
html.dark .table-wrapper .n-data-table-tr:nth-child(even) .n-data-table-td {
  background: #263449 !important;
}
html.dark .table-wrapper .n-data-table-tr:hover .n-data-table-td {
  background: #374151 !important;
}
html.dark .table-wrapper .n-data-table-base-table {
  border-color: #334155 !important;
}
</style>

<template>
  <div class="dynamic-report-container">
    <!-- 页面头部统计 -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><DocumentTextOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ paginationItemCount }}</span>
          <span class="stat-label">总记录数</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><GridOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ tableColumns.length }}</span>
          <span class="stat-label">显示字段</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="24"><FunnelOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ activeFilters.length }}</span>
          <span class="stat-label">筛选条件</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-info">
          <n-icon size="24"><LayersOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ tableData.length }}</span>
          <span class="stat-label">当前页</span>
        </div>
      </div>
    </div>

    <n-card class="main-card" :bordered="false">
      <template #header>
        <div class="report-header">
          <div class="report-title">
            <n-icon size="24" color="var(--color-primary)"><StatsChartOutline /></n-icon>
            <div class="report-title-info">
              <span class="report-title-text">{{ reportName || '报表查看' }}</span>
              <div class="report-meta">
                <n-tag v-if="reportDefinition" type="info" size="small" :bordered="false">
                  {{ reportDefinition.reportCode }}
                </n-tag>
                <span v-if="reportDefinition?.createBy" class="meta-item">
                  <n-icon size="12"><PersonOutline /></n-icon>
                  {{ reportDefinition.createBy }}
                </span>
                <span v-if="lastRefreshTime" class="meta-item">
                  <n-icon size="12"><TimeOutline /></n-icon>
                  {{ lastRefreshTime }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </template>
      <template #header-extra>
        <n-space :size="8">
          <FilterPanel
            v-if="filterFields.length > 0"
            :fields="filterFields"
            :model-value="activeFilters"
            @apply="handleFilterApply"
          />
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button :loading="loading" secondary @click="handleRefresh">
                <template #icon><n-icon><RefreshOutline /></n-icon></template>
              </n-button>
            </template>
            刷新数据
          </n-tooltip>
          <n-dropdown trigger="click" :options="densityOptions" @select="handleDensityChange">
            <n-tooltip trigger="hover">
              <template #trigger>
                <n-button secondary>
                  <template #icon><n-icon><ListOutline /></n-icon></template>
                </n-button>
              </template>
              表格密度
            </n-tooltip>
          </n-dropdown>
          <n-popover trigger="click" placement="bottom-end" style="max-height: 400px; overflow-y: auto;">
            <template #trigger>
              <n-tooltip trigger="hover">
                <template #trigger>
                  <n-button secondary>
                    <template #icon><n-icon><SettingsOutline /></n-icon></template>
                  </n-button>
                </template>
                列设置
              </n-tooltip>
            </template>
            <div style="width: 220px;">
              <div style="font-weight:600;margin-bottom:8px;font-size:13px;">显示/隐藏列</div>
              <n-checkbox
                v-for="col in allColumnKeys"
                :key="col.key"
                :checked="!hiddenColumns.has(col.key)"
                style="display:block;margin-bottom:4px;"
                @update:checked="(val: boolean) => toggleColumn(col.key, val)"
              >
                {{ col.title }}
              </n-checkbox>
              <n-divider style="margin:8px 0" />
              <n-button size="tiny" quaternary type="primary" @click="hiddenColumns.clear()">显示全部</n-button>
            </div>
          </n-popover>
          <n-tooltip v-if="allowPrint" trigger="hover">
            <template #trigger>
              <n-button secondary @click="handlePrint">
                <template #icon><n-icon><PrintOutline /></n-icon></template>
              </n-button>
            </template>
            打印
          </n-tooltip>
          <n-button v-if="allowExportExcel" type="primary" @click="handleExport">
            <template #icon><n-icon><DownloadOutline /></n-icon></template>
            导出Excel
          </n-button>
          <n-button v-if="allowExportPdf" type="info" :loading="exportingPdf" @click="handleExportPdf">
            <template #icon><n-icon><DocumentTextOutline /></n-icon></template>
            导出PDF
          </n-button>
        </n-space>
      </template>
      
      <!-- 报表描述 -->
      <div v-if="reportDefinition?.description" class="report-description">
        <n-icon size="16"><InformationCircleOutline /></n-icon>
        <span>{{ reportDefinition.description }}</span>
      </div>

      <!-- 查询参数表单 -->
      <div v-if="reportParams.length > 0" class="param-panel">
        <div class="param-panel-header" @click="paramExpanded = !paramExpanded">
          <div class="param-panel-title">
            <n-icon size="16" color="#3b82f6"><OptionsOutline /></n-icon>
            <span>查询参数</span>
            <n-tag size="tiny" :bordered="false" type="info">{{ reportParams.length }} 项</n-tag>
          </div>
          <n-icon
            size="18"
            class="param-panel-arrow"
            :class="{ 'param-panel-arrow-expanded': paramExpanded }"
          >
            <ChevronDownOutline />
          </n-icon>
        </div>
        <div class="param-panel-body" :class="{ 'param-panel-body-collapsed': !paramExpanded }">
          <div class="param-panel-body-inner">
            <n-form inline label-placement="left" label-width="auto" class="param-form">
              <n-form-item v-for="p in reportParams" :key="p.name" :label="p.label">
                <template #label>
                  {{ p.label }}
                  <span v-if="p.required" style="color: var(--color-error)"> *</span>
                </template>
                <!-- 文本 -->
                <n-input
                  v-if="p.inputType === 'text'"
                  v-model:value="paramValues[p.name]"
                  :placeholder="'请输入' + p.label"
                  clearable
                  style="min-width: 220px; width: 220px"
                />
                <!-- 数字 -->
                <n-input-number
                  v-else-if="p.inputType === 'number'"
                  v-model:value="paramValues[p.name]"
                  :placeholder="'请输入' + p.label"
                  clearable
                  style="min-width: 200px; width: 200px"
                />
                <!-- 日期 -->
                <n-date-picker
                  v-else-if="p.inputType === 'date'"
                  v-model:formatted-value="paramValues[p.name]"
                  type="date"
                  value-format="yyyy-MM-dd"
                  clearable
                  style="min-width: 220px; width: 220px"
                />
                <!-- 日期范围 -->
                <n-date-picker
                  v-else-if="p.inputType === 'daterange'"
                  v-model:formatted-value="paramValues[p.name]"
                  type="daterange"
                  value-format="yyyy-MM-dd"
                  clearable
                  style="min-width: 320px; width: 320px"
                />
                <!-- 下拉选择（字典） -->
                <n-select
                  v-else-if="p.inputType === 'select'"
                  v-model:value="paramValues[p.name]"
                  :options="getParamSelectOptions(p)"
                  :placeholder="'请选择' + p.label"
                  clearable
                  filterable
                  style="min-width: 220px; width: 220px"
                />
                <!-- 部门 -->
                <n-tree-select
                  v-else-if="p.inputType === 'department'"
                  v-model:value="paramValues[p.name]"
                  :options="departmentTreeOptions"
                  key-field="id"
                  label-field="deptName"
                  children-field="children"
                  :placeholder="'请选择' + p.label"
                  clearable
                  filterable
                  style="min-width: 240px; width: 240px"
                />
              </n-form-item>
              <n-form-item>
                <n-space>
                  <n-button type="primary" @click="handleParamQuery">
                    <template #icon><n-icon><SearchOutline /></n-icon></template>
                    查询
                  </n-button>
                  <n-button @click="handleParamReset">重置</n-button>
                </n-space>
              </n-form-item>
            </n-form>
          </div>
        </div>
      </div>
      
      <!-- 数据摘要条 -->
      <div v-if="checkedRowKeys.length > 0" class="data-summary-bar">
        <n-space :size="16" align="center">
          <n-tag type="info" size="small" round>已选 {{ checkedRowKeys.length }} 行</n-tag>
          <template v-for="s in numericSummary" :key="s.key">
            <span class="summary-item"><b>{{ s.title }}</b>: 合计 {{ s.sum }} / 均值 {{ s.avg }}</span>
          </template>
          <n-button size="tiny" quaternary @click="checkedRowKeys = []">取消选择</n-button>
        </n-space>
      </div>

      <div class="table-container">
        <n-data-table
          v-model:checked-row-keys="checkedRowKeys"
          :columns="smartTableColumns"
          :data="tableData"
          :loading="loading"
          :pagination="false"
          :scroll-x="tableScrollX"
          :max-height="tableMaxHeight"
          :size="tableDensity"
          :row-key="(_row: any, index: number) => String(index)"
          striped
          virtual-scroll
        />
      </div>
      <div class="pagination-wrapper">
        <div class="pagination-info">
          共 {{ paginationItemCount || 0 }} 条记录
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
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, computed, onMounted, onActivated, watch, h } from 'vue'
import { useRoute } from 'vue-router'
import { useMessage, NSpace, NTag } from 'naive-ui'
import { StatsChartOutline, RefreshOutline, DownloadOutline, InformationCircleOutline, DocumentTextOutline, GridOutline, FunnelOutline, LayersOutline, SearchOutline, OptionsOutline, ChevronDownOutline, PersonOutline, TimeOutline, ListOutline, PrintOutline, SettingsOutline } from '@vicons/ionicons5'
import { 
  executeReportQuery, 
  executeReportQueryByCode,
  getReportDefinitionById,
  getReportDefinitionByCode,
  exportReportDefinition,
  exportReportDefinitionByCode,
  exportReportAsPdf
} from '@/api/reportDefinition'
import { getDictByType } from '@/api/system/dataDictionary'
import { getDepartmentTree } from '@/api/org/department'
import { useExport } from '@/composables/useExport'
import { useDialog } from 'naive-ui'
import { initMessage } from '@/utils/message'
import { handleApiError } from '@/utils/error'
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE, PAGE_SIZES } from '@/constants'
import type { ReportDefinition, ReportParam } from '@/types/reportDefinition'
import type { PageResult, FilterCondition } from '@/types/api'
import FilterPanel, { type FilterField } from '@/components/FilterPanel.vue'
import { filtersToApiParam } from '@/utils/filterParams'
import { formatDateTime } from '@/utils/format'
import { useTabsStore } from '@/stores/tabs'

const tabsStore = useTabsStore()

const route = useRoute()
const message = useMessage()
initMessage(message)

const loading = ref(false)
const exportingPdf = ref(false)
const reportName = ref('')
const lastRefreshTime = ref('')
const tableDensity = ref<'small' | 'medium' | 'large'>('medium')
const densityOptions = [
  { label: '紧凑', key: 'small' },
  { label: '默认', key: 'medium' },
  { label: '宽松', key: 'large' }
]
const handleDensityChange = (key: string) => {
  tableDensity.value = key as 'small' | 'medium' | 'large'
}
// 表格最大高度：确保表头固定、数据区域内部滚动
const tableMaxHeight = computed(() => {
  return Math.max(window.innerHeight - 360, 300)
})
const reportDefinition = ref<ReportDefinition | null>(null)
const tableData = ref<any[]>([])
const activeFilters = ref<FilterCondition[]>([])
const checkedRowKeys = ref<string[]>([])

// 列设置: 隐藏列集合
const hiddenColumns = reactive(new Set<string>())
const allColumnKeys = computed(() => tableColumns.value.map(c => ({ key: c.key, title: c.title })))
const toggleColumn = (key: string, visible: boolean) => {
  if (visible) hiddenColumns.delete(key)
  else hiddenColumns.add(key)
}

// 数据摘要: 选中行的数值列求和/均值
const numericSummary = computed(() => {
  if (checkedRowKeys.value.length === 0 || tableData.value.length === 0) return []
  const keySet = new Set(checkedRowKeys.value)
  const selectedRows = tableData.value.filter((_r, idx) => keySet.has(String(idx)))
  if (selectedRows.length === 0) return []
  // 预判数值列（只采样前10行，避免重复计算）
  const numericCols = tableColumns.value.filter(col => {
    const sample = tableData.value.slice(0, 10).map(r => r[col.key]).filter(v => v !== null && v !== undefined && v !== '')
    return sample.length > 0 && sample.every(v => !isNaN(Number(v)))
  })
  const results: Array<{ key: string; title: string; sum: string; avg: string }> = []
  for (const col of numericCols) {
    const vals = selectedRows.map(r => Number(r[col.key])).filter(v => !isNaN(v) && isFinite(v))
    if (vals.length === 0) continue
    const sum = vals.reduce((a, b) => a + b, 0)
    results.push({
      key: col.key,
      title: col.title,
      sum: sum.toLocaleString(undefined, { maximumFractionDigits: 2 }),
      avg: (sum / vals.length).toLocaleString(undefined, { maximumFractionDigits: 2 })
    })
    if (results.length >= 5) break
  }
  return results
})

// 打印（生成表格内容，限制最多200行）
const PRINT_MAX_ROWS = 200
const handlePrint = () => {
  const cols = smartTableColumns.value
  const rows = tableData.value.slice(0, PRINT_MAX_ROWS)
  if (cols.length === 0) {
    message.warning('暂无数据可打印')
    return
  }

  const title = reportName.value || '报表'
  const totalRows = tableData.value.length
  const truncated = totalRows > PRINT_MAX_ROWS

  // 构建表头
  const thHtml = cols.map((c: any) => `<th>${c.title || c.key}</th>`).join('')

  // 构建表体
  const tbodyHtml = rows.map((row: any) => {
    const tds = cols.map((c: any) => {
      const val = row[c.key]
      return `<td>${formatTableCellValue(val, c.key)}</td>`
    }).join('')
    return `<tr>${tds}</tr>`
  }).join('')

  const now = new Date()
  const timeStr = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')} ${String(now.getHours()).padStart(2,'0')}:${String(now.getMinutes()).padStart(2,'0')}`

  const html = `<!DOCTYPE html><html><head><meta charset="utf-8"><title>${title}</title>
<style>
  body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;margin:24px;color:#1e293b}
  h2{margin:0 0 4px;font-size:18px}
  .meta{color:#64748b;font-size:12px;margin-bottom:12px}
  table{width:100%;border-collapse:collapse;font-size:12px}
  th{background:#f1f5f9;font-weight:600;text-align:left;padding:6px 8px;border:1px solid #e2e8f0}
  td{padding:5px 8px;border:1px solid #e2e8f0;word-break:break-all}
  tr:nth-child(even){background:#f8fafc}
  .footer{margin-top:8px;font-size:11px;color:#94a3b8;text-align:right}
  @media print{body{margin:10px}h2{font-size:16px}}
</style></head><body>
  <h2>${title}</h2>
  <div class="meta">打印时间：${timeStr} | 共 ${totalRows} 条${truncated ? `（仅显示前 ${PRINT_MAX_ROWS} 条）` : ''}</div>
  <table><thead><tr>${thHtml}</tr></thead><tbody>${tbodyHtml}</tbody></table>
  <div class="footer">DataTeaCup 报表系统</div>
  <script>window.onload=function(){window.print();}<\/script>
</body></html>`

  const printWin = window.open('', '_blank')
  if (printWin) {
    printWin.document.write(html)
    printWin.document.close()
  }
}

// 导出/打印权限控制
const allowExportExcel = computed(() => reportDefinition.value?.allowExportExcel !== 0)
const allowExportPdf = computed(() => reportDefinition.value?.allowExportPdf !== 0)
const allowPrint = computed(() => reportDefinition.value?.allowPrint !== 0)

// ==================== 查询参数 ====================
const paramExpanded = ref(true)
const paramValues: Record<string, any> = reactive({})
const departmentTreeOptions = ref<any[]>([])
const paramDictOptions: Record<string, Array<{ label: string; value: string }>> = reactive({})

const reportParams = computed<ReportParam[]>(() => {
  const raw = reportDefinition.value?.params
  if (!raw) return []
  if (Array.isArray(raw)) return raw
  if (typeof raw === 'string') {
    try {
      return JSON.parse(raw)
    } catch {
      return []
    }
  }
  return []
})

const getParamSelectOptions = (p: ReportParam) => {
  if (p.dictType && paramDictOptions[p.dictType]) {
    return paramDictOptions[p.dictType]
  }
  return p.options || []
}

const initParamDefaults = () => {
  Object.keys(paramValues).forEach(k => delete paramValues[k])
  for (const p of reportParams.value) {
    if (p.defaultValue) {
      if (p.inputType === 'number') {
        paramValues[p.name] = Number(p.defaultValue)
      } else if (p.inputType === 'daterange' && p.defaultValue.includes(',')) {
        paramValues[p.name] = p.defaultValue.split(',')
      } else {
        paramValues[p.name] = p.defaultValue
      }
    } else {
      paramValues[p.name] = null
    }
  }
}

const loadParamResources = async () => {
  const params = reportParams.value
  if (params.length === 0) return
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
    if (!paramDictOptions[dt]) {
      try {
        const res: any = await getDictByType(dt)
        const list = res?.data || []
        paramDictOptions[dt] = list.map((d: any) => ({
          label: d.dictLabel || d.dictCode,
          value: d.dictValue || d.dictCode
        }))
      } catch { /* ignore */ }
    }
  }
}

const buildParamsJson = (): string | undefined => {
  const params = reportParams.value
  if (params.length === 0) return undefined
  const result: Record<string, any> = {}
  for (const p of params) {
    const val = paramValues[p.name]
    if (val === null || val === undefined || val === '') continue
    if (p.inputType === 'daterange' && Array.isArray(val)) {
      result[p.name] = val.join(',')
    } else {
      result[p.name] = String(val)
    }
  }
  return Object.keys(result).length > 0 ? JSON.stringify(result) : undefined
}

const handleParamQuery = () => {
  for (const p of reportParams.value) {
    if (p.required) {
      const val = paramValues[p.name]
      if (val === null || val === undefined || val === '' || (Array.isArray(val) && val.length === 0)) {
        message.warning(`请填写必填参数: ${p.label}`)
        return
      }
    }
  }
  paginationPage.value = 1
  loadData()
}

const handleParamReset = () => {
  initParamDefaults()
  paginationPage.value = 1
  loadData()
}

/** 检查是否存在未填写的必填参数 */
const hasUnfilledRequiredParams = (): boolean => {
  for (const p of reportParams.value) {
    if (p.required) {
      const val = paramValues[p.name]
      if (val === null || val === undefined || val === '' || (Array.isArray(val) && val.length === 0)) {
        return true
      }
    }
  }
  return false
}

// 使用 ref 存储分页状态，直接绑定到 n-pagination 组件，确保响应式更新
const paginationPage = ref(DEFAULT_PAGE)
const paginationPageSize = ref(DEFAULT_PAGE_SIZE)
const paginationItemCount = ref(0)

// ==================== 数据字典集成 ====================
const dictOptionsMap: Record<string, Array<{ label: string; value: string }>> = reactive({})

const loadDictOptions = async () => {
  if (!reportDefinition.value?.fields) return
  const dictTypes = new Set<string>()
  reportDefinition.value.fields.forEach(field => {
    if (field.dictType) dictTypes.add(field.dictType)
  })
  for (const dt of dictTypes) {
    try {
      const res: any = await getDictByType(dt)
      const list = res?.data || []
      dictOptionsMap[dt] = list.map((d: any) => ({
        label: d.dictLabel || d.dictCode,
        value: d.dictValue || d.dictCode,
      }))
    } catch { /* ignore */ }
  }
}

// 根据报表字段配置生成筛选字段
const filterFields = computed<FilterField[]>(() => {
  if (!reportDefinition.value?.fields) {
    return []
  }
  
  return reportDefinition.value.fields
    .filter(field => field.isVisible !== 0)
    .map(field => {
      // 根据字段类型判断
      let type: 'string' | 'number' | 'date' = 'string'
      if (field.fieldType === 'number' || field.fieldType === 'integer' || field.fieldType === 'decimal') {
        type = 'number'
      } else if (field.fieldType === 'date' || field.fieldType === 'datetime') {
        type = 'date'
      }
      
      const result: FilterField = {
        label: field.fieldLabel || field.fieldName,
        value: field.fieldName,
        type
      }
      // 字典列附加下拉选项
      if (field.dictType && dictOptionsMap[field.dictType]) {
        result.options = dictOptionsMap[field.dictType] || []
      }
      return result
    })
})

const tableColumns = computed(() => {
  if (!reportDefinition.value?.fields) {
    // 如果没有字段配置，根据数据动态生成列
    if (tableData.value.length > 0) {
      const firstRow = tableData.value[0]
      return Object.keys(firstRow).map(key => ({
        title: key,
        key: key,
        width: 150
      }))
    }
    return []
  }
  
  // 使用配置的字段信息
  const visibleFields = reportDefinition.value.fields
    .filter(field => field.isVisible !== 0)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
  
  return visibleFields.map(field => ({
    title: field.fieldLabel || field.fieldName,
    key: field.fieldName,
    width: field.width || 150,
    align: field.align || 'left'
  }))
})

/**
 * 计算字符串宽度
 */
const calculateTextWidth = (text: string): number => {
  if (!text) return 80
  const chineseRegex = /[\u4e00-\u9fa5]/g
  const chineseCount = (text.match(chineseRegex) || []).length
  const otherCount = text.length - chineseCount
  return chineseCount * 14 + otherCount * 8 + 40
}

/**
 * 计算列宽
 */
const calculateColumnWidth = (column: any, data: any[]): number => {
  const MIN_WIDTH = 100
  const MAX_WIDTH = 350
  
  if (column.width && column.width >= MIN_WIDTH) {
    return Math.min(column.width, MAX_WIDTH)
  }
  
  let maxWidth = calculateTextWidth(column.title || column.key)
  
  const sampleSize = Math.min(data.length, 20)
  for (let i = 0; i < sampleSize; i++) {
    const value = data[i]?.[column.key]
    if (value !== null && value !== undefined) {
      const text = String(value)
      const width = calculateTextWidth(text)
      maxWidth = Math.max(maxWidth, width)
    }
  }
  
  return Math.min(Math.max(maxWidth, MIN_WIDTH), MAX_WIDTH)
}

/**
 * 格式化表格单元格值
 */
const formatTableCellValue = (value: any, columnKey?: string, fieldType?: string, columnTitle?: string): string => {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  if (typeof value === 'boolean') {
    return value ? '是' : '否'
  }

  const keyLower = columnKey?.toLowerCase() || ''
  const titleLower = columnTitle?.toLowerCase() || ''
  const typeLower = fieldType?.toLowerCase() || ''

  // 智能检测时间戳数字（毫秒级：13位，秒级：10位）
  const isTimestampNumber = typeof value === 'number' && (
    (value > 1000000000000 && value < 9999999999999) || // 毫秒级时间戳 (2001-2286年)
    (value > 1000000000 && value < 9999999999)         // 秒级时间戳 (2001-2286年)
  )

  // 字段名、标题或字段类型包含日期关键词
  const hasDateKeyword = /(date|time|日期|时间|月份|年份|日|月|年|create_at|update_at|created_at|updated_at)$/i.test(keyLower) ||
                          /(date|time|日期|时间|月份|年份|日|月|年)$/i.test(titleLower) ||
                          /(date|time|datetime|timestamp)/i.test(typeLower)

  if (isTimestampNumber || (hasDateKeyword && typeof value === 'string' && !isNaN(Date.parse(value)))) {
    const timestamp = isTimestampNumber && typeof value === 'number' && value < 10000000000 ? value * 1000 : value
    const showTime = /(datetime|timestamp|时间|time)/i.test(keyLower) || /(datetime|timestamp|时间|time)/i.test(titleLower) || /(datetime|timestamp)/i.test(typeLower)
    return formatDateTime(timestamp, showTime ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD')
  }

  return String(value)
}

/**
 * 智能表格列（带自动宽度计算和值处理）
 */
const smartTableColumns = computed(() => {
  if (tableColumns.value.length === 0) {
    return []
  }
  
  // 单次遍历构建字段类型和字典类型映射
  const fieldTypeMap: Record<string, string> = {}
  const fieldDictTypeMap: Record<string, string> = {}
  if (reportDefinition.value?.fields) {
    for (const field of reportDefinition.value.fields) {
      fieldTypeMap[field.fieldName] = field.fieldType || ''
      if (field.dictType) fieldDictTypeMap[field.fieldName] = field.dictType
    }
  }

  const visibleCols = tableColumns.value.filter(col => !hiddenColumns.has(col.key))

  const dataCols = visibleCols.map(col => {
    const width = calculateColumnWidth(col, tableData.value)
    const fieldType = fieldTypeMap[col.key] || ''
    const dictType = fieldDictTypeMap[col.key]
    const dictOpts = dictType ? dictOptionsMap[dictType] : null

    return {
      ...col,
      width,
      minWidth: 100,
      ellipsis: {
        tooltip: true
      },
      resizable: true,
      render: (row: any) => {
        const val = row[col.key]
        // 字典翻译
        if (dictOpts && val !== null && val !== undefined) {
          const item = dictOpts.find(d => d.value === String(val))
          if (item) {
            return h(NTag, { size: 'small', bordered: false, type: 'info' }, { default: () => item.label })
          }
        }
        return formatTableCellValue(val, col.key, fieldType, col.title)
      }
    }
  })

  return [
    { type: 'selection' as const, width: 48 },
    ...dataCols
  ]
})

/**
 * 动态水平滚动宽度
 */
const tableScrollX = computed(() => {
  if (smartTableColumns.value.length === 0) {
    return undefined
  }
  
  const totalWidth = smartTableColumns.value.reduce((sum: number, col: any) => {
    return sum + (col.width || 100)
  }, 0)
  
  return totalWidth > 1200 ? totalWidth : undefined
})

// 初始化加载函数
const initLoad = async () => {
  // 重置状态
  reportDefinition.value = null
  reportName.value = ''
  tableData.value = []
  activeFilters.value = []
  paginationPage.value = DEFAULT_PAGE
  paginationPageSize.value = DEFAULT_PAGE_SIZE
  paginationItemCount.value = 0
  // 加载报表定义和数据
  await loadReportDefinition()
  if (reportDefinition.value) {
    initParamDefaults()
    await loadParamResources()
    // 如果有必填参数且无默认值，跳过首次加载，等用户填写后再查询
    if (hasUnfilledRequiredParams()) {
      paramExpanded.value = true
      return
    }
    await loadData()
  }
}

onMounted(async () => {
  await initLoad()
})

// 处理 keep-alive 缓存组件被重新激活的情况
onActivated(async () => {
  // 检查当前路由参数是否与已加载的报表一致
  const routeId = route.params["id"] as string
  const routeCode = route.params["code"] as string
  const currentId = reportDefinition.value?.id?.toString()
  const currentCode = reportDefinition.value?.reportCode
  
  // 如果路由参数与当前加载的报表不一致，重新加载
  if ((routeId && routeId !== currentId) || (routeCode && routeCode !== currentCode)) {
    await initLoad()
  } else if (!routeId && !routeCode) {
    // 路由没有参数，清空状态
    reportDefinition.value = null
    reportName.value = ''
    tableData.value = []
  }
})

// 监听路由参数变化，当切换报表时重新加载
watch(
  () => [route.params["id"], route.params["code"]],
  async ([newId, newCode], [oldId, oldCode]) => {
    // 只有当参数真正变化时才重新加载
    if (newId !== oldId || newCode !== oldCode) {
      await initLoad()
    }
  },
  { immediate: false }
)

const loadReportDefinition = async () => {
  const id = route.params["id"] as string
  const code = route.params["code"] as string
  
  try {
    if (id) {
      const res = await getReportDefinitionById(Number(id))
      reportDefinition.value = (res.data as unknown) as ReportDefinition
    } else if (code) {
      const res = await getReportDefinitionByCode(code)
      reportDefinition.value = (res.data as unknown) as ReportDefinition
    }
    
    if (reportDefinition.value) {
      reportName.value = reportDefinition.value.reportName
      // 更新标签页标题
      tabsStore.updateTabTitle(route.fullPath, reportName.value)
      // 加载字典选项
      await loadDictOptions()
    }
  } catch (error: any) {
    message.error(error.message || '加载报表定义失败')
  }
}

const loadData = async () => {
  if (!reportDefinition.value) {
    return
  }
  
  loading.value = true
  try {
    const id = route.params["id"] as string
    const code = route.params["code"] as string
    
    const filtersParam = filtersToApiParam(activeFilters.value)
    const paramsJson = buildParamsJson()
    let res
    if (id) {
      res = await executeReportQuery(Number(id), {
        page: paginationPage.value,
        pageSize: paginationPageSize.value,
        filters: filtersParam || '',
        params: paramsJson || ''
      })
    } else if (code) {
      res = await executeReportQueryByCode(code, {
        page: paginationPage.value,
        pageSize: paginationPageSize.value,
        filters: filtersParam || '',
        params: paramsJson || ''
      })
    } else {
      message.error('缺少报表ID或编码')
      return
    }
    
    // 解析响应数据
    let pageResult: PageResult<Record<string, any>> | null = null
    
    if (res && typeof res === 'object') {
      if (res.data && typeof res.data === 'object' && 'list' in res.data && 'total' in res.data) {
        pageResult = res.data as PageResult<Record<string, any>>
      } else if ('list' in res && 'total' in res) {
        pageResult = res as PageResult<Record<string, any>>
      }
    }
    
    // 获取列表数据
    tableData.value = pageResult?.list || []
    
    // 设置总记录数
    if (pageResult && pageResult.total !== undefined && pageResult.total !== null) {
      let total: number
      if (typeof pageResult.total === 'number') {
        total = Math.max(0, pageResult.total)
      } else {
        const num = Number(pageResult.total)
        total = isNaN(num) ? 0 : Math.max(0, num)
      }
      paginationItemCount.value = total
    } else {
      paginationItemCount.value = 0
    }
  } catch (error: any) {
    const errorMsg = handleApiError(error, '加载报表数据', '加载数据失败')
    message.error(errorMsg)
    if (tableData.value.length === 0) {
      paginationItemCount.value = 0
    }
  } finally {
    loading.value = false
    lastRefreshTime.value = new Date().toLocaleTimeString()
  }
}

const handlePageChange = (page: number) => {
  paginationPage.value = page
  loadData()
}

const handlePageSizeChange = (pageSize: number) => {
  paginationPageSize.value = pageSize
  paginationPage.value = 1
  loadData()
}

const handleRefresh = () => {
  loadData()
}

const handleFilterApply = (filters: FilterCondition[]) => {
  activeFilters.value = filters
  // 重置到第一页并重新加载数据（后端筛选）
  paginationPage.value = 1
  loadData()
}

// 大数据量阈值
const LARGE_DATA_THRESHOLD = 200000  // 20万：提示使用后台导出
const MAX_DIRECT_EXPORT = 500000     // 50万：强制后台导出

const dialog = useDialog()
const { addToExportCenter } = useExport()

const handleExport = async () => {
  if (!reportDefinition.value) return
  
  const id = route.params["id"] as string
  const code = route.params["code"] as string
  const filtersParam = filtersToApiParam(activeFilters.value)
  const filename = reportName.value || '报表数据'
  
  // 超过50万，只能后台导出
  if (paginationItemCount.value > MAX_DIRECT_EXPORT) {
    dialog.warning({
      title: '大数据量导出',
      content: `当前数据量为 ${paginationItemCount.value.toLocaleString()} 条，超过 ${MAX_DIRECT_EXPORT.toLocaleString()} 条，只能使用后台导出。导出完成后可点击右上角"导出中心"按钮下载文件。`,
      positiveText: '确认后台导出',
      onPositiveClick: async () => {
        await handleAsyncExport(id || '', code || '', filtersParam || '', filename)
      }
    })
    return
  }
  
  // 超过20万，提示使用后台导出（可选）
  if (paginationItemCount.value > LARGE_DATA_THRESHOLD) {
    dialog.warning({
      title: '大数据量导出提示',
      content: `当前数据量为 ${paginationItemCount.value.toLocaleString()} 条，建议使用后台导出功能，避免等待时间过长。`,
      positiveText: '后台导出',
      negativeText: '直接导出',
      onPositiveClick: async () => {
        await handleAsyncExport(id || '', code || '', filtersParam || '', filename)
      },
      onNegativeClick: async () => {
        await handleDirectExport(id || '', code || '', filtersParam || '', filename)
      }
    })
    return
  }
  
  // 数据量较小，直接导出
  await handleDirectExport(id || '', code || '', filtersParam || '', filename)
}

// 异步导出（后台任务）— 使用 useExport 统一导出流程 (Req 22.3)
const handleAsyncExport = async (id: string, code: string, filtersParam: string, filename: string) => {
  const refId = id ? Number(id) : 0
  const paramsJson = buildParamsJson()
  await addToExportCenter({
    taskName: filename,
    taskType: 'report',
    refId,
    refCode: code || '',
    filters: filtersParam || '',
    params: paramsJson || ''
  })
}

// 直接导出（同步）
const handleDirectExport = async (id: string, code: string, filtersParam: string, filename: string) => {
  const loadingMsg = message.loading('正在导出...', { duration: 0 })
  const paramsJson = buildParamsJson()
  
  try {
    let blob: any
    
    if (id) {
      const response = await exportReportDefinition(Number(id), filtersParam, paramsJson)
      blob = response instanceof Blob ? response : (response.data || response)
    } else if (code) {
      const response = await exportReportDefinitionByCode(code, filtersParam, paramsJson)
      blob = response instanceof Blob ? response : (response.data || response)
    } else {
      message.error('缺少报表ID或编码')
      return
    }
    
    if (!(blob instanceof Blob) || blob.size === 0) {
      message.error('导出失败，返回数据为空')
      return
    }
    
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${filename}_${new Date().getTime()}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    message.success('导出成功')
  } catch (error: any) {
    const errorMsg = handleApiError(error, '导出报表', '导出失败')
    message.error(errorMsg)
  } finally {
    loadingMsg.destroy()
  }
}

// PDF导出
const PDF_MAX_ROWS = 5000  // PDF导出最大行数限制

const handleExportPdf = async () => {
  if (!reportDefinition.value) return
  
  const id = route.params["id"] as string
  if (!id) {
    message.error('缺少报表ID，无法导出PDF')
    return
  }
  
  // 数据量超过PDF限制时提示
  if (paginationItemCount.value > PDF_MAX_ROWS) {
    dialog.warning({
      title: 'PDF导出数据量限制',
      content: `当前数据量为 ${paginationItemCount.value.toLocaleString()} 条，PDF导出最多支持 ${PDF_MAX_ROWS.toLocaleString()} 条。导出的PDF将只包含前 ${PDF_MAX_ROWS.toLocaleString()} 条数据。如需完整数据，请使用Excel导出。`,
      positiveText: '继续导出PDF',
      negativeText: '取消',
      onPositiveClick: () => doExportPdf(id),
    })
    return
  }
  
  await doExportPdf(id)
}

const doExportPdf = async (id: string) => {
  const filtersParam = filtersToApiParam(activeFilters.value)
  const paramsJson = buildParamsJson()
  
  exportingPdf.value = true
  try {
    const blob = await exportReportAsPdf(Number(id), filtersParam, paramsJson)
    const url = URL.createObjectURL(new Blob([blob]))
    const a = document.createElement('a')
    a.href = url
    a.download = `${reportName.value || 'report'}.pdf`
    a.click()
    URL.revokeObjectURL(url)
    message.success('PDF导出成功')
  } catch (error: any) {
    message.error(error.message || 'PDF导出失败')
  } finally {
    exportingPdf.value = false
  }
}
</script>

<style scoped>
.dynamic-report-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.pagination-info {
  color: #666;
  font-size: 14px;
}

.report-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.report-title {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.report-title-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.report-title-text {
  font-weight: 600;
  font-size: 17px;
  color: #1e293b;
}

.report-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: #94a3b8;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 3px;
}

.report-description {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 8px;
  margin-bottom: 16px;
  color: #0369a1;
  font-size: 14px;
}

.data-summary-bar {
  padding: 8px 16px;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-radius: 8px;
  border: 1px solid #bfdbfe;
  font-size: 13px;
}

.summary-item {
  color: #1e40af;
  font-size: 12px;
}

.summary-item b {
  font-weight: 600;
}

.table-container {
  width: 100%;
  overflow-x: auto;
  overflow-y: visible;
  flex: 1;
  min-height: 0;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.table-container :deep(.n-data-table) {
  min-width: 100%;
}

.table-container :deep(.n-data-table-wrapper) {
  overflow-x: auto;
}

/* 表格美化 */
:deep(.n-data-table-th) {
  font-weight: 600;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  color: #475569;
  border-bottom: 2px solid #e2e8f0;
  padding: 14px 16px;
  white-space: nowrap;
  font-size: 13px;
  letter-spacing: 0.3px;
}

:deep(.n-data-table-td) {
  padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9;
  transition: all 0.2s ease;
  font-size: 13px;
  color: #334155;
}

:deep(.n-data-table-tr:hover .n-data-table-td) {
  background-color: #f8fafc !important;
}

/* 条纹行 */
:deep(.n-data-table-tr--striped .n-data-table-td) {
  background-color: #fafafa;
}

:deep(.n-data-table-tr--striped:hover .n-data-table-td) {
  background-color: #f5f5f5 !important;
}

/* 滚动条美化 */
:deep(.n-data-table-base-table-header),
:deep(.n-data-table-base-table-body) {
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 #f1f5f9;
}

:deep(.n-data-table-base-table-header::-webkit-scrollbar),
:deep(.n-data-table-base-table-body::-webkit-scrollbar) {
  height: 10px;
  width: 10px;
}

:deep(.n-data-table-base-table-header::-webkit-scrollbar-track),
:deep(.n-data-table-base-table-body::-webkit-scrollbar-track) {
  background: #f1f5f9;
  border-radius: 5px;
}

:deep(.n-data-table-base-table-header::-webkit-scrollbar-thumb),
:deep(.n-data-table-base-table-body::-webkit-scrollbar-thumb) {
  background: linear-gradient(135deg, #94a3b8 0%, #64748b 100%);
  border-radius: 5px;
  transition: background 0.3s ease;
}

:deep(.n-data-table-base-table-header::-webkit-scrollbar-thumb:hover),
:deep(.n-data-table-base-table-body::-webkit-scrollbar-thumb:hover) {
  background: linear-gradient(135deg, #64748b 0%, #475569 100%);
}

/* 空状态美化 */
:deep(.n-data-table-empty) {
  padding: 64px 0;
}

/* 加载状态 */
:deep(.n-data-table--loading .n-data-table-td) {
  opacity: 0.5;
}

/* 查询参数面板 */
.param-panel {
  margin-bottom: 16px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #fff;
  overflow: hidden;
  transition: box-shadow 0.2s ease;
}

.param-panel:hover {
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.08);
}

.param-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f8fbff 0%, #f0f6ff 100%);
  border-bottom: 1px solid #e8eff8;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s ease;
}

.param-panel-header:hover {
  background: linear-gradient(135deg, #f0f6ff 0%, #e8f1fd 100%);
}

.param-panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
  color: #1e40af;
}

.param-panel-arrow {
  color: #94a3b8;
  transition: transform 0.25s ease;
}

.param-panel-arrow-expanded {
  transform: rotate(180deg);
}

.param-panel-body {
  max-height: 300px;
  overflow: hidden;
  transition: max-height 0.3s ease;
}

.param-panel-body-collapsed {
  max-height: 0;
}

.param-panel-body-inner {
  padding: 16px;
}

.param-form :deep(.n-form-item) {
  margin-bottom: 8px;
}

/* ========== 响应式设计 ========== */
@media (max-width: 768px) {
  .dynamic-report-container { padding: 10px; }
  .page-header-stats {
  flex-shrink: 0;
    flex-direction: row !important;
    overflow-x: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;
    gap: 8px;
    padding-bottom: 4px;
  }
  .page-header-stats::-webkit-scrollbar { display: none; }
  .stat-item { min-width: 110px; flex-shrink: 0; }
  .report-header { flex-direction: column; gap: 8px; align-items: flex-start !important; }
  .param-panel-body-inner { padding: 10px; }
  .main-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column; border-radius: 12px !important; }
  :deep(.n-data-table-th),
  :deep(.n-data-table-td) {
    padding: 8px 10px;
    font-size: 12px;
  }
  :deep(.n-data-table) {
    font-size: 12px;
  }
}
</style>

<style>
/* DynamicReport 深色模式（非 scoped） */
html.dark .report-title-text { color: #e2e8f0 !important; }
html.dark .report-description {
  background: linear-gradient(135deg, #1e3a5f 0%, #172554 100%) !important;
  color: #7dd3fc !important;
}
html.dark .data-summary-bar {
  background: linear-gradient(135deg, #172554 0%, #1e3a5f 100%) !important;
  border-color: #1e40af !important;
}
html.dark .summary-item { color: #93c5fd !important; }
html.dark .param-panel {
  background: #1e293b !important;
  border-color: #334155 !important;
}
html.dark .param-panel:hover {
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.15) !important;
}
html.dark .param-panel-header {
  background: linear-gradient(135deg, #1e293b 0%, #1a2742 100%) !important;
  border-bottom-color: #334155 !important;
}
html.dark .param-panel-header:hover {
  background: linear-gradient(135deg, #1a2742 0%, #1e3a5f 100%) !important;
}
html.dark .param-panel-title { color: #93c5fd !important; }
html.dark .pagination-wrapper {
  border-top-color: #334155 !important;
}
html.dark .pagination-info { color: #94a3b8 !important; }
html.dark .n-data-table-th {
  background: linear-gradient(135deg, #1e293b 0%, #1a2332 100%) !important;
  color: #e2e8f0 !important;
  border-bottom-color: #334155 !important;
}
html.dark .n-data-table-td {
  color: #cbd5e1 !important;
  border-bottom-color: #293548 !important;
}
html.dark .n-data-table-tr:hover .n-data-table-td {
  background-color: #1e3a5f !important;
}
html.dark .n-data-table-tr--striped .n-data-table-td {
  background-color: #162032 !important;
}
html.dark .n-data-table-tr--striped:hover .n-data-table-td {
  background-color: #1e3a5f !important;
}
html.dark .table-container {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important;
}
html.dark .n-data-table-base-table-header,
html.dark .n-data-table-base-table-header,
html.dark .n-data-table-base-table-body {
  scrollbar-color: #475569 #1e293b !important;
}
html.dark .n-data-table-base-table-header::-webkit-scrollbar-track,
html.dark .n-data-table-base-table-header::-webkit-scrollbar-track,
html.dark .n-data-table-base-table-body::-webkit-scrollbar-track {
  background: #1e293b !important;
}
html.dark .n-data-table-base-table-header::-webkit-scrollbar-thumb,
html.dark .n-data-table-base-table-header::-webkit-scrollbar-thumb,
html.dark .n-data-table-base-table-body::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #475569 0%, #334155 100%) !important;
}
</style>

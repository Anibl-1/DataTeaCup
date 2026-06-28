<template>
  <div class="data-config-panel">
    <!-- 数据源选择 -->
    <div class="config-section">
      <div class="section-label">数据源</div>
      <n-select
        :value="config.dataSourceId"
        :options="dataSourceOptions"
        placeholder="选择数据源"
        size="small"
        @update:value="updateField('dataSourceId', $event)"
      />
    </div>

    <!-- 表选择（快捷） -->
    <div v-if="config.dataSourceId" class="config-section">
      <div class="section-label">快捷选表</div>
      <n-select
        v-model:value="selectedTable"
        :options="tableOptions"
        :loading="tablesLoading"
        placeholder="选择表快捷生成SQL"
        size="small"
        clearable
        filterable
        @update:value="handleTableSelect"
      />
    </div>

    <!-- SQL 编辑器 -->
    <div class="config-section">
      <div class="section-label-row">
        <span class="section-label">SQL 查询</span>
        <n-space :size="4">
          <n-button size="tiny" secondary type="info" :disabled="!config.dataSourceId || !selectedTable" @click="handleAiSqlGenerate">
            <template #icon><n-icon size="13"><SparklesOutline /></n-icon></template>
            AI生成
          </n-button>
          <n-button size="tiny" secondary type="primary" :loading="testing" :disabled="!config.dataSourceId || !config.sqlContent" @click="handleTestSql">
            <template #icon><n-icon size="13"><PlayOutline /></n-icon></template>
            执行
          </n-button>
        </n-space>
      </div>
      <n-input
        :value="config.sqlContent"
        type="textarea"
        placeholder="SELECT * FROM table_name LIMIT 100"
        :rows="5"
        size="small"
        style="font-family: monospace; font-size: 12px;"
        @update:value="handleSqlChange"
      />
      <!-- SQL 参数语法提示 -->
      <div class="sql-hint">
        <n-icon size="12" color="#aaa"><InformationCircleOutline /></n-icon>
        <span>使用 <code>${{'{参数名}'}}</code> 定义动态参数，如 <code>WHERE date &gt;= ${{'{start_date}'}}</code></span>
      </div>
      <!-- 检测到的参数 -->
      <div v-if="detectedParams.length > 0" class="detected-params">
        <n-tag v-for="p in detectedParams" :key="p" size="tiny" type="warning" :bordered="false" style="margin: 2px">
          ${ {{ p }} }
        </n-tag>
        <span class="param-hint">已检测到参数，可在下方配置或绑定查询组件</span>
      </div>
    </div>

    <!-- 字段映射 -->
    <div v-if="previewColumns.length > 0" class="config-section">
      <div class="section-label-row">
        <span class="section-label">字段映射</span>
        <n-button size="tiny" secondary type="info" @click="handleAutoMapping">
          <template #icon><n-icon size="13"><SparklesOutline /></n-icon></template>
          自动推荐
        </n-button>
      </div>
      <div class="mapping-grid">
        <div class="mapping-item">
          <span class="mapping-label">X轴/维度</span>
          <n-select
            :value="config.fieldMapping?.xAxis"
            :options="columnOptions"
            placeholder="选择字段"
            size="tiny"
            clearable
            @update:value="updateMapping('xAxis', $event)"
          />
        </div>
        <div class="mapping-item">
          <span class="mapping-label">Y轴/指标</span>
          <n-select
            :value="yAxisValue"
            :options="columnOptions"
            placeholder="选择字段"
            size="tiny"
            clearable
            multiple
            @update:value="updateMapping('yAxis', $event)"
          />
        </div>
        <div class="mapping-item">
          <span class="mapping-label">名称字段</span>
          <n-select
            :value="config.fieldMapping?.nameField"
            :options="columnOptions"
            placeholder="可选"
            size="tiny"
            clearable
            @update:value="updateMapping('nameField', $event)"
          />
        </div>
        <div class="mapping-item">
          <span class="mapping-label">值字段</span>
          <n-select
            :value="config.fieldMapping?.valueField"
            :options="columnOptions"
            placeholder="可选"
            size="tiny"
            clearable
            @update:value="updateMapping('valueField', $event)"
          />
        </div>
        <div class="mapping-item">
          <span class="mapping-label">分组字段</span>
          <n-select
            :value="config.fieldMapping?.group"
            :options="columnOptions"
            placeholder="可选"
            size="tiny"
            clearable
            @update:value="updateMapping('group', $event)"
          />
        </div>
      </div>
    </div>

    <!-- 自动生成提示 -->
    <div v-if="previewData.length > 0 && config.fieldMapping?.xAxis" class="config-section">
      <n-button block size="small" type="primary" ghost @click="handleGenerateChartConfig">
        <template #icon><n-icon><SparklesOutline /></n-icon></template>
        根据字段映射生成图表配置
      </n-button>
    </div>

    <!-- 数据预览 -->
    <div v-if="previewData.length > 0" class="config-section">
      <div class="section-label-row">
        <span class="section-label">数据预览</span>
        <n-tag size="tiny" :bordered="false">{{ previewData.length }} 行</n-tag>
      </div>
      <div class="preview-table-wrapper">
        <table class="preview-table">
          <thead>
            <tr>
              <th v-for="col in previewColumns" :key="col">{{ col }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in previewData.slice(0, 10)" :key="idx">
              <td v-for="col in previewColumns" :key="col">{{ row[col] }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 查询参数配置 -->
    <div class="config-section params-section">
      <div class="section-label-row">
        <span class="section-label">
          查询参数
          <n-tag v-if="paramList.length > 0" size="tiny" round :bordered="false" type="info" style="margin-left: 4px; vertical-align: middle;">{{ paramList.length }}</n-tag>
        </span>
        <n-space :size="4">
          <n-button v-if="detectedParams.length > 0" size="tiny" secondary type="info" @click="autoPopulateParams">
            <template #icon><n-icon size="13"><ScanOutline /></n-icon></template>
            从SQL检测
          </n-button>
          <n-button size="tiny" secondary type="primary" @click="addParam">
            <template #icon><n-icon size="13"><AddCircleOutline /></n-icon></template>
            添加
          </n-button>
        </n-space>
      </div>

      <!-- 空状态 -->
      <div v-if="paramList.length === 0" class="param-empty-tip">
        <n-icon size="24" color="#ccc"><FilterOutline /></n-icon>
        <n-text depth="3" style="font-size: 12px">定义参数后，可在页面查询区域关联使用</n-text>
      </div>

      <!-- 参数列表 - 折叠卡片样式 -->
      <div v-else class="param-list">
        <div v-for="(param, idx) in paramList" :key="idx" class="param-card-v2">
          <!-- 参数卡片头部（点击展开/收起） -->
          <div class="param-card-header" @click="expandedParamIndex = expandedParamIndex === idx ? -1 : idx">
            <div class="param-card-summary">
              <n-tag size="tiny" :type="getParamTagType(param.type)" :bordered="false">
                {{ getParamTypeLabel(param.type) }}
              </n-tag>
              <span class="param-card-label">{{ param.label || param.field || '未配置' }}</span>
              <n-tag v-if="param.required" size="tiny" type="error" :bordered="false">必填</n-tag>
            </div>
            <div class="param-card-actions">
              <n-icon
                size="14"
                :style="{ transform: expandedParamIndex === idx ? 'rotate(180deg)' : 'rotate(0)', transition: 'transform 0.2s' }"
              >
                <ChevronDownOutline />
              </n-icon>
              <n-button size="tiny" quaternary type="error" @click.stop="removeParam(idx)">
                <template #icon><n-icon size="12"><CloseCircleOutline /></n-icon></template>
              </n-button>
            </div>
          </div>
          <!-- 参数详细配置（展开时显示） -->
          <n-collapse-transition :show="expandedParamIndex === idx">
            <div class="param-card-detail">
              <div class="param-detail-row">
                <label>字段</label>
                <n-select
                  v-model:value="param.field"
                  :options="paramFieldOptions"
                  placeholder="输入或选择字段名"
                  size="tiny"
                  filterable
                  tag
                  @update:value="(v: string) => { if (!param.label) param.label = v; syncParams() }"
                />
              </div>
              <div class="param-detail-row">
                <label>显示名称</label>
                <n-input v-model:value="param.label" placeholder="输入显示名称" size="tiny" @update:value="syncParams" />
              </div>
              <div class="param-detail-row">
                <label>输入类型</label>
                <n-select
                  v-model:value="param.type"
                  :options="paramTypeOptions"
                  size="tiny"
                  @update:value="() => handleParamTypeChange(param)"
                />
              </div>
              <div class="param-detail-row">
                <label>必填</label>
                <n-switch v-model:value="param.required" size="small" @update:value="syncParams" />
              </div>
              <!-- 日期类型：快捷默认值 -->
              <div v-if="param.type === 'date'" class="param-detail-row">
                <label>默认值</label>
                <n-select
                  v-model:value="param.datePreset"
                  :options="datePresetOptions"
                  placeholder="选择日期快捷值"
                  size="tiny"
                  clearable
                  @update:value="syncParams"
                />
              </div>
              <!-- 日期范围类型：快捷默认值 -->
              <div v-if="param.type === 'dateRange'" class="param-detail-row">
                <label>默认值</label>
                <n-select
                  v-model:value="param.datePreset"
                  :options="dateRangePresetOptions"
                  placeholder="选择日期范围快捷值"
                  size="tiny"
                  clearable
                  @update:value="syncParams"
                />
              </div>
              <!-- 数字类型：默认值 -->
              <div v-if="param.type === 'number'" class="param-detail-row">
                <label>默认值</label>
                <n-input-number
                  :value="param.defaultValue !== '' ? Number(param.defaultValue) : null"
                  placeholder="默认值"
                  size="tiny"
                  style="width: 100%"
                  @update:value="(v: number | null) => { param.defaultValue = v != null ? String(v) : ''; syncParams() }"
                />
              </div>
              <!-- 文本类型：默认值 -->
              <div v-if="param.type === 'text'" class="param-detail-row">
                <label>默认值</label>
                <n-input
                  v-model:value="param.defaultValue"
                  placeholder="默认值"
                  size="tiny"
                  @update:value="syncParams"
                />
              </div>
              <!-- 下拉选择类型：选项配置 -->
              <template v-if="param.type === 'select' || param.type === 'multiSelect'">
                <div class="param-detail-row">
                  <label>选项来源</label>
                  <n-radio-group v-model:value="param.optionSource" size="small" @update:value="syncParams">
                    <n-radio-button value="manual">手动</n-radio-button>
                    <n-radio-button value="sql">SQL</n-radio-button>
                  </n-radio-group>
                </div>
                <div v-if="param.optionSource === 'manual'" class="param-detail-row">
                  <label>选项</label>
                  <n-dynamic-tags
                    v-model:value="param.optionTags"
                    size="small"
                    @update:value="(tags: string[]) => handleParamOptionTagsChange(param, tags)"
                  />
                </div>
                <div v-if="param.optionSource === 'sql'" class="param-detail-row">
                  <label>选项SQL</label>
                  <n-input
                    v-model:value="param.optionSql"
                    type="textarea"
                    :rows="2"
                    placeholder="SELECT value, label FROM table"
                    size="tiny"
                    style="font-family: monospace; font-size: 11px;"
                    @update:value="syncParams"
                  />
                </div>
              </template>
            </div>
          </n-collapse-transition>
        </div>
      </div>
    </div>

    <!-- 数据限制 -->
    <div class="config-section">
      <div class="section-label">数据限制</div>
      <n-input-number
        :value="config.dataLimit || 1000"
        :min="10"
        :max="100000"
        size="small"
        style="width: 100%"
        @update:value="updateField('dataLimit', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, computed, watch } from 'vue'
import {
  NSelect, NInput, NButton, NTag, NInputNumber, NIcon, NSpace, NSwitch, NText,
  NCollapseTransition, NRadioGroup, NRadioButton, NDynamicTags,
  useMessage
} from 'naive-ui'
import { SparklesOutline, CloseCircleOutline, AddCircleOutline, ChevronDownOutline, FilterOutline, PlayOutline, InformationCircleOutline, ScanOutline } from '@vicons/ionicons5'
import type { InlineChartConfig, InlineFieldMapping } from '@/types/page'
import type { ChartParameter, ChartParameterType } from '@/types/chart'
import { getDataSourceTables, getTableColumns } from '@/api/dataSource'
import { testInlineChartSql, aiGenerateInlineChart } from '@/api/pageDesigner'
import { buildInlineChartOption, autoDetectFieldMapping, detectSqlParams } from '@/utils/chartRenderer'

const props = defineProps<{
  config: InlineChartConfig
  dataSources: Array<{ id: number; name: string }>
}>()

const emit = defineEmits<{
  (e: 'update:config', config: InlineChartConfig): void
  (e: 'data-ready', data: any[]): void
  (e: 'params-detected', params: string[]): void
}>()

const message = useMessage()
const selectedTable = ref<string | null>(null)
const tablesLoading = ref(false)
const tables = ref<Array<{ tableName: string; tableType: string }>>([])
const tableColumnsCache = ref<Array<{ columnName: string; dataType: string; remarks?: string }>>([])
const testing = ref(false)
const previewData = ref<any[]>([])
const previewColumns = ref<string[]>([])
const detectedParams = ref<string[]>([])

const dataSourceOptions = computed(() =>
  props.dataSources.map(ds => ({ label: ds.name, value: ds.id }))
)

// ---- 参数配置 ----
const paramTypeOptions = [
  { label: '文本', value: 'text' },
  { label: '数字', value: 'number' },
  { label: '日期', value: 'date' },
  { label: '日期范围', value: 'dateRange' },
  { label: '下拉选择', value: 'select' },
  { label: '多选', value: 'multiSelect' }
]

interface EditableParam {
  field: string
  label: string
  type: ChartParameterType
  defaultValue: string
  required: boolean
  datePreset?: string
  optionSource?: 'manual' | 'sql'
  optionTags?: string[]
  optionSql?: string
}

const paramList = reactive<EditableParam[]>([])
const expandedParamIndex = ref(-1)

// 日期快捷值选项
const datePresetOptions = [
  { label: '今天', value: 'today' },
  { label: '昨天', value: 'yesterday' },
  { label: '本周第一天', value: 'weekStart' },
  { label: '本月第一天', value: 'monthStart' },
  { label: '本年第一天', value: 'yearStart' },
  { label: '最近7天', value: 'last7Days' },
  { label: '最近30天', value: 'last30Days' }
]

const dateRangePresetOptions = [
  { label: '本周', value: 'thisWeek' },
  { label: '上周', value: 'lastWeek' },
  { label: '本月', value: 'thisMonth' },
  { label: '上月', value: 'lastMonth' },
  { label: '本季度', value: 'thisQuarter' },
  { label: '最近7天', value: 'last7Days' },
  { label: '最近30天', value: 'last30Days' },
  { label: '最近90天', value: 'last90Days' }
]

const getParamTagType = (type: ChartParameterType): 'default' | 'info' | 'success' | 'warning' | 'error' => {
  const map: Record<string, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
    text: 'default', number: 'info', date: 'success',
    dateRange: 'success', select: 'warning', multiSelect: 'warning'
  }
  return map[type] || 'default'
}

const getParamTypeLabel = (type: ChartParameterType): string => {
  const map: Record<string, string> = {
    text: '文本', number: '数字', date: '日期',
    dateRange: '日期范围', select: '下拉', multiSelect: '多选'
  }
  return map[type] || type
}

// 从 config.parameters 初始化（避免 syncParams 触发时重复重建）
let isSyncing = false
watch(() => props.config.parameters, (params) => {
  if (isSyncing) return
  if (!params) { paramList.length = 0; return }
  // 只在参数数量或关键字段变化时才重建，避免编辑中丢失焦点
  const normalize = (p: any) => ({
    field: p.field, label: p.label || p.field, type: p.type || 'text', required: !!p.required,
    defaultValue: p.defaultValue != null ? String(p.defaultValue) : '', datePreset: p.datePreset || undefined,
    optionSource: p.optionSource || 'manual', optionTags: p.optionTags || [], optionSql: p.optionSql || ''
  })
  const newJson = JSON.stringify(params.map(normalize))
  const curJson = JSON.stringify(paramList.map(normalize))
  if (newJson === curJson) return
  paramList.length = 0
  params.forEach(p => paramList.push({
    field: p.field,
    label: p.label || p.field,
    type: p.type || 'text',
    defaultValue: p.defaultValue != null ? String(p.defaultValue) : '',
    required: p.required ?? false,
    datePreset: p.datePreset,
    optionSource: p.optionSource || 'manual',
    optionTags: p.optionTags || [],
    optionSql: p.optionSql || ''
  }))
}, { immediate: true })

const syncParams = () => {
  isSyncing = true
  const params: ChartParameter[] = paramList.map(p => ({
    field: p.field,
    operator: '=' as const,
    name: p.field,
    label: p.label || p.field,
    type: p.type,
    required: p.required,
    defaultValue: p.defaultValue || null,
    datePreset: p.datePreset,
    optionSource: p.optionSource,
    optionTags: p.optionTags,
    optionSql: p.optionSql,
    options: p.optionTags?.map(t => ({ label: t, value: t })) || []
  }))
  updateField('parameters', params)
  // 延迟重置标志，确保 watch 跳过本次变化
  setTimeout(() => { isSyncing = false }, 0)
}

const autoPopulateParams = () => {
  const existing = new Set(paramList.map(p => p.field))
  let added = 0
  detectedParams.value.forEach(field => {
    if (!existing.has(field)) {
      paramList.push({ field, label: field, type: 'text', defaultValue: '', required: false, optionSource: 'manual', optionTags: [], optionSql: '' })
      added++
    }
  })
  if (added > 0) {
    syncParams()
    message.success(`已添加 ${added} 个参数`)
  } else {
    message.info('无新参数可添加')
  }
}

const removeParam = (idx: number) => {
  paramList.splice(idx, 1)
  if (expandedParamIndex.value === idx) expandedParamIndex.value = -1
  else if (expandedParamIndex.value > idx) expandedParamIndex.value--
  syncParams()
}

const addParam = () => {
  const newParam: EditableParam = {
    field: 'param' + (paramList.length + 1),
    label: '',
    type: 'text',
    defaultValue: '',
    required: false,
    optionSource: 'manual',
    optionTags: [],
    optionSql: ''
  }
  paramList.push(newParam)
  expandedParamIndex.value = paramList.length - 1
}

const handleParamTypeChange = (param: EditableParam) => {
  param.defaultValue = ''
  param.datePreset = undefined
  if (param.type === 'select' || param.type === 'multiSelect') {
    param.optionSource = param.optionSource || 'manual'
  }
  syncParams()
}

const handleParamOptionTagsChange = (param: EditableParam, tags: string[]) => {
  param.optionTags = tags
  syncParams()
}

const tableOptions = computed(() =>
  tables.value.map(t => ({ label: `${t.tableName} (${t.tableType})`, value: t.tableName }))
)

const columnOptions = computed(() =>
  previewColumns.value.map(c => ({ label: c, value: c }))
)

// 参数字段选项：优先使用SQL检测到的参数，再补充数据列字段
const paramFieldOptions = computed(() => {
  const opts: Array<{ label: string; value: string }> = []
  const seen = new Set<string>()
  // 1) SQL检测到的参数名
  detectedParams.value.forEach(p => {
    if (!seen.has(p)) {
      opts.push({ label: `\${${p}} (SQL参数)`, value: p })
      seen.add(p)
    }
  })
  // 2) 数据列字段
  previewColumns.value.forEach(c => {
    if (!seen.has(c)) {
      opts.push({ label: c, value: c })
      seen.add(c)
    }
  })
  return opts
})

const yAxisValue = computed(() => {
  const val = props.config.fieldMapping?.yAxis
  if (!val) return []
  return Array.isArray(val) ? val : [val]
})

const updateField = (field: string, value: any) => {
  emit('update:config', { ...props.config, [field]: value })
}

const updateMapping = (key: keyof InlineFieldMapping, value: any) => {
  const mapping = { ...(props.config.fieldMapping || {}) }
  if (key === 'yAxis' && Array.isArray(value)) {
    mapping[key] = value.length === 1 ? value[0] : value
  } else {
    (mapping as any)[key] = value
  }
  emit('update:config', { ...props.config, fieldMapping: mapping })
}

// SQL变更时检测参数
const handleSqlChange = (val: string) => {
  updateField('sqlContent', val)
  const params = detectSqlParams(val)
  detectedParams.value = params
  emit('params-detected', params)
}

// 加载表列表
watch(() => props.config.dataSourceId, async (id) => {
  tables.value = []
  selectedTable.value = null
  tableColumnsCache.value = []
  if (!id) return
  tablesLoading.value = true
  try {
    const res = await getDataSourceTables(id)
    if (res?.data) {
      tables.value = Array.isArray(res.data) ? res.data : []
    }
  } catch (e) {
    console.warn('加载表列表失败', e)
  } finally {
    tablesLoading.value = false
  }
}, { immediate: true })

// 选表 → 自动: 加载字段 → 推荐映射 → 生成SQL → 测试SQL → 生成chartConfig
const handleTableSelect = async (tableName: string | null) => {
  if (!tableName || !props.config.dataSourceId) return
  // 清理上一次的预览数据
  previewData.value = []
  previewColumns.value = []
  try {
    const res = await getTableColumns(props.config.dataSourceId, tableName)
    if (res?.data && Array.isArray(res.data)) {
      tableColumnsCache.value = res.data
      const cols = res.data.map((c: any) => c.columnName)

      // 1. 生成 SQL
      const sql = `SELECT ${cols.join(', ')} FROM ${tableName} LIMIT 100`
      
      // 2. 自动推荐字段映射
      const recommended = autoDetectFieldMapping(res.data, props.config.chartType)

      // 3. 一次性更新 config（SQL + mapping）
      const newConfig: InlineChartConfig = {
        ...props.config,
        sqlContent: sql,
        fieldMapping: {
          xAxis: recommended.xAxis,
          yAxis: recommended.yAxis && recommended.yAxis.length === 1 ? recommended.yAxis[0] : recommended.yAxis,
          nameField: recommended.nameField,
          valueField: recommended.valueField
        }
      }
      emit('update:config', newConfig)

      // 4. 检测参数
      const params = detectSqlParams(sql)
      detectedParams.value = params
      emit('params-detected', params)

      // 5. 自动执行测试
      await doTestSql(newConfig)
    }
  } catch (e) {
    console.warn('加载表字段失败', e)
  }
}

// 自动推荐字段映射（手动触发）
const handleAutoMapping = () => {
  if (tableColumnsCache.value.length === 0 && previewColumns.value.length > 0) {
    // 从预览数据推断
    const cols = previewColumns.value.map(c => ({
      columnName: c,
      dataType: previewData.value.length > 0 && !isNaN(Number(previewData.value[0][c])) ? 'NUMERIC' : 'VARCHAR'
    }))
    const recommended = autoDetectFieldMapping(cols, props.config.chartType)
    const mapping: InlineFieldMapping = {
      xAxis: recommended.xAxis,
      yAxis: recommended.yAxis && recommended.yAxis.length === 1 ? recommended.yAxis[0] : recommended.yAxis,
      nameField: recommended.nameField,
      valueField: recommended.valueField
    }
    emit('update:config', { ...props.config, fieldMapping: mapping })
    message.success('已自动推荐字段映射')
    return
  }
  if (tableColumnsCache.value.length > 0) {
    const recommended = autoDetectFieldMapping(tableColumnsCache.value, props.config.chartType)
    const mapping: InlineFieldMapping = {
      xAxis: recommended.xAxis,
      yAxis: recommended.yAxis && recommended.yAxis.length === 1 ? recommended.yAxis[0] : recommended.yAxis,
      nameField: recommended.nameField,
      valueField: recommended.valueField
    }
    emit('update:config', { ...props.config, fieldMapping: mapping })
    message.success('已自动推荐字段映射')
  }
}

// 手动生成图表配置
const handleGenerateChartConfig = () => {
  if (previewData.value.length === 0) {
    message.warning('请先测试SQL获取数据')
    return
  }
  const option = buildInlineChartOption(
    props.config.chartType,
    previewData.value,
    props.config.fieldMapping as any,
    props.config.colorScheme
  )
  const chartConfig = JSON.stringify(option)
  emit('update:config', { ...props.config, chartConfig })
  emit('data-ready', previewData.value)
  message.success('图表配置已生成')
}

// 测试 SQL（公共逻辑）
const doTestSql = async (cfg?: InlineChartConfig) => {
  const c = cfg || props.config
  if (!c.dataSourceId || !c.sqlContent) return
  testing.value = true
  try {
    const res = await testInlineChartSql({
      dataSourceId: c.dataSourceId,
      sqlContent: c.sqlContent,
      limit: c.dataLimit || 100
    })
    if (res?.data && Array.isArray(res.data)) {
      previewData.value = res.data
      previewColumns.value = res.data.length > 0 ? Object.keys(res.data[0]) : []
      message.success(`查询成功，返回 ${res.data.length} 行数据`)

      // 自动生成 chartConfig 并传递数据
      if (c.fieldMapping?.xAxis || c.fieldMapping?.nameField) {
        const option = buildInlineChartOption(
          c.chartType,
          res.data,
          c.fieldMapping as any,
          c.colorScheme
        )
        const chartConfig = JSON.stringify(option)
        emit('update:config', { ...c, chartConfig })
        emit('data-ready', res.data)
      } else {
        emit('data-ready', res.data)
      }
    } else {
      previewData.value = []
      previewColumns.value = []
      message.warning('查询无数据返回')
    }
  } catch (e: any) {
    message.error(e?.message || 'SQL 执行失败')
    previewData.value = []
    previewColumns.value = []
  } finally {
    testing.value = false
  }
}

const handleTestSql = () => doTestSql()

// AI 生成 SQL
const handleAiSqlGenerate = async () => {
  if (!props.config.dataSourceId || !selectedTable.value) return
  message.info('AI 正在智能分析表结构并生成SQL...')
  try {
    // 构建详细的表结构信息
    const columnsInfo = tableColumnsCache.value.map(col => 
      `${col.columnName}(${col.dataType}${col.remarks ? ', ' + col.remarks : ''})`
    ).join(', ')
    
    // 根据图表类型构建具体的需求描述
    const chartTypeGuide: Record<string, string> = {
      bar: '柱状图需要分类维度字段(如名称、类别)作为X轴，数值字段(如金额、数量)作为Y轴，使用GROUP BY聚合',
      line: '折线图需要时间或序列字段作为X轴，数值字段作为Y轴，按时间/序列排序',
      pie: '饼图需要分类字段作为名称，数值字段作为值，使用GROUP BY聚合',
      scatter: '散点图需要两个数值字段分别作为X轴和Y轴',
      gauge: '仪表盘需要单个数值字段，通常是百分比或进度值'
    }
    const typeHint = chartTypeGuide[props.config.chartType] || '请根据表结构选择合适的字段'
    
    const requirement = `基于表 ${selectedTable.value} 生成 ${props.config.chartType} 图表。
表字段: ${columnsInfo}
图表要求: ${typeHint}
请生成合理的聚合查询SQL，选择最适合的字段组合，确保SQL语法正确。`

    const res = await aiGenerateInlineChart({
      requirement,
      dataSourceId: props.config.dataSourceId,
      context: { 
        preferredChartType: props.config.chartType,
        tables: [selectedTable.value]
      }
    })
    if (res?.data?.success && res.data.chartConfig) {
      const cfg = res.data.chartConfig
      const newConfig: InlineChartConfig = {
        ...props.config,
        sqlContent: cfg.sql || props.config.sqlContent,
        chartConfig: cfg.chartConfig ? JSON.stringify(cfg.chartConfig) : props.config.chartConfig,
        chartName: cfg.chartName || props.config.chartName,
        fieldMapping: cfg.dataMapping ? {
          xAxis: cfg.dataMapping.xField,
          yAxis: cfg.dataMapping.yField,
          nameField: cfg.dataMapping.nameField,
          valueField: cfg.dataMapping.valueField
        } : props.config.fieldMapping
      }
      emit('update:config', newConfig)
      message.success('AI 已生成SQL和配置，正在测试...')
      await doTestSql(newConfig)
    } else {
      message.warning(res?.data?.error || 'AI 未返回有效配置')
    }
  } catch (e: any) {
    message.error(e?.message || 'AI 生成失败')
  }
}
</script>

<style scoped>
.data-config-panel {
  padding: 4px 0;
}
.config-section {
  margin-bottom: 14px;
}
.section-label {
  font-size: 12px;
  font-weight: 600;
  color: #666;
  margin-bottom: 6px;
}
.section-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.mapping-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.mapping-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.mapping-label {
  font-size: 11px;
  color: #999;
  width: 60px;
  flex-shrink: 0;
  text-align: right;
}
.sql-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 5px;
  font-size: 11px;
  color: #aaa;
  line-height: 1.4;
}
.sql-hint code {
  background: #f5f5f5;
  padding: 0 3px;
  border-radius: 3px;
  font-family: monospace;
  font-size: 10px;
  color: #d48806;
  border: 1px solid #f0e8d0;
}
.detected-params {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 6px;
  padding: 6px 8px;
  background: #fffbe6;
  border-radius: 4px;
  border: 1px solid #ffe58f;
}
.param-hint {
  font-size: 10px;
  color: #d48806;
  margin-left: 4px;
}
.preview-table-wrapper {
  max-height: 200px;
  overflow: auto;
  border: 1px solid #eee;
  border-radius: 6px;
}
.preview-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 11px;
}
.preview-table th,
.preview-table td {
  padding: 4px 8px;
  border-bottom: 1px solid #f0f0f0;
  white-space: nowrap;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
}
.preview-table th {
  background: #fafafa;
  font-weight: 600;
  color: #666;
  position: sticky;
  top: 0;
}
.preview-table tr:hover td {
  background: #f8f8f8;
}
/* 参数区域 */
.params-section {
  border-top: 1px dashed #e8e8e8;
  padding-top: 12px;
}
.param-empty-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px 0;
}
.param-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.param-card-v2 {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  background: #fafafa;
  overflow: hidden;
  transition: border-color 0.2s;
}
.param-card-v2:hover {
  border-color: #b7d8cb;
}
.param-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px;
  cursor: pointer;
  user-select: none;
  transition: background 0.15s;
}
.param-card-header:hover {
  background: #f0f9f4;
}
.param-card-summary {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  flex: 1;
}
.param-card-label {
  font-size: 12px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.param-card-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
.param-card-detail {
  padding: 8px 10px 10px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.param-detail-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.param-detail-row > label {
  font-size: 11px;
  color: #999;
  width: 55px;
  flex-shrink: 0;
  text-align: right;
}
</style>

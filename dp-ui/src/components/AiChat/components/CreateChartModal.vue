<template>
  <n-modal
    v-model:show="showModal"
    preset="card"
    title="📈 创建图表"
    style="width: 800px; max-width: 95vw;"
    :mask-closable="false"
    :segmented="{ content: 'soft', footer: 'soft' }"
  >
    <template #header-extra>
      <n-tag type="success" size="small">
        步骤 {{ currentStep }}/4
      </n-tag>
    </template>

    <!-- 步骤指示器 -->
    <n-steps :current="currentStep" size="small" style="margin-bottom: 20px;">
      <n-step title="基本信息" />
      <n-step title="字段映射" />
      <n-step title="参数配置" />
      <n-step title="预览确认" />
    </n-steps>

    <!-- 步骤1：基本信息 -->
    <div v-show="currentStep === 1" class="step-content">
      <n-form ref="formRef" :model="form" label-placement="left" label-width="80">
        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item label="图表名称" required>
              <n-input v-model:value="form.chartName" placeholder="输入图表名称" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="图表类型" required>
              <n-select v-model:value="form.chartType" :options="chartTypeOptions" />
            </n-form-item>
          </n-gi>
        </n-grid>
        <n-form-item label="图表描述">
          <n-input v-model:value="form.description" type="textarea" :rows="2" placeholder="描述图表用途" />
        </n-form-item>
        <!-- SQL选择器（当有多个SQL时显示） -->
        <n-form-item v-if="sqlOptions.length > 1" label="选择SQL">
          <n-select
            v-model:value="selectedSqlIndex"
            :options="sqlOptions"
            @update:value="handleSqlSelect"
          />
        </n-form-item>
        
        <n-form-item label="SQL语句" required>
          <n-input 
            v-model:value="form.sql" 
            type="textarea" 
            :rows="6" 
            placeholder="SELECT category, SUM(amount) as total FROM orders WHERE year = ${year} GROUP BY category"
            font-family="monospace"
          />
        </n-form-item>
      </n-form>
      
      <n-alert v-if="sqlOptions.length > 1" type="warning" :bordered="false" style="margin-top: 12px;">
        ⚠️ AI返回了 {{ sqlOptions.length }} 个SQL，已默认选择最优方案，您也可以切换其他SQL
      </n-alert>
      <n-alert type="info" :bordered="false" style="margin-top: 12px;">
        💡 在SQL中使用 <n-text code>${参数名}</n-text> 定义查询参数
      </n-alert>
    </div>

    <!-- 步骤2：字段映射 -->
    <div v-show="currentStep === 2" class="step-content">
      <n-alert type="info" :bordered="false" style="margin-bottom: 16px;">
        🎯 请配置图表的数据字段映射，将SQL查询结果映射到图表的X轴和Y轴
      </n-alert>
      
      <n-spin :show="loadingFields">
        <n-grid :cols="2" :x-gap="16" :y-gap="12">
          <n-gi>
            <n-form-item label="维度（X轴）" required>
              <n-select 
                v-model:value="form.fieldMapping.xAxis"
                :options="fieldOptions"
                placeholder="选择X轴字段"
                filterable
                tag
              />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="指标（Y轴）" required>
              <n-select 
                v-model:value="form.fieldMapping.yAxis"
                :options="fieldOptions"
                placeholder="选择Y轴字段"
                multiple
                filterable
                tag
                @update:value="handleYAxisChange"
              />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="X轴排序">
              <n-space :size="8">
                <n-select 
                  v-model:value="form.fieldMapping.sortField"
                  :options="[{ label: '按X轴字段', value: 'x' }, { label: '按Y轴数值', value: 'y' }]"
                  style="width: 120px"
                />
                <n-select 
                  v-model:value="form.fieldMapping.sortOrder"
                  :options="[{ label: '升序', value: 'ASC' }, { label: '降序', value: 'DESC' }]"
                  style="width: 100px"
                />
              </n-space>
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="分组（可选）">
              <n-select 
                v-model:value="form.fieldMapping.group"
                :options="fieldOptions"
                placeholder="选择分组字段"
                clearable
                filterable
                tag
              />
            </n-form-item>
          </n-gi>
        </n-grid>

        <!-- Y轴聚合配置 -->
        <div v-if="form.fieldMapping.yAxis.length > 0" class="aggregate-config">
          <n-text strong style="margin-bottom: 8px; display: block;">聚合方式 & 别名</n-text>
          <div v-for="field in form.fieldMapping.yAxis" :key="field" class="aggregate-item">
            <n-tag size="small" type="success">{{ field }}</n-tag>
            <n-select 
              v-model:value="form.aggregates[field]"
              :options="aggregateOptions"
              size="small"
              style="width: 120px"
            />
            <n-input 
              v-model:value="form.aliases[field]"
              placeholder="别名"
              size="small"
              style="width: 100px"
            />
          </div>
        </div>
      </n-spin>
      
      <n-button 
        type="primary" 
        size="small" 
        :loading="loadingFields"
        style="margin-top: 12px;"
        @click="parseFieldsFromSql"
      >
        <template #icon><n-icon :component="RefreshOutline" /></template>
        从SQL解析字段
      </n-button>
    </div>

    <!-- 步骤3：参数配置 -->
    <div v-show="currentStep === 3" class="step-content">
      <!-- 字段状态提示 -->
      <n-alert v-if="fieldOptions.length > 0" type="success" :bordered="false" style="margin-bottom: 12px;">
        ✅ 已获取 {{ fieldOptions.length }} 个字段，可从下方选择字段创建筛选参数
      </n-alert>
      <n-alert v-else type="info" :bordered="false" style="margin-bottom: 12px;">
        💡 请先在步骤2中解析SQL获取字段，或直接手动添加参数
      </n-alert>
      
      <ParamConfig 
        v-model="form.queryParams" 
        :sql="form.sql"
        :available-fields="fieldOptions.map(f => f.value)"
        @add-field-param="handleAddFieldParam"
      />
    </div>

    <!-- 步骤4：预览确认 -->
    <div v-show="currentStep === 4" class="step-content">
      <!-- 参数输入区 -->
      <div v-if="form.queryParams.length > 0" class="preview-params">
        <n-text strong style="margin-bottom: 8px; display: block;">
          🔍 输入参数值
        </n-text>
        <n-grid :cols="3" :x-gap="12" :y-gap="8">
          <n-gi v-for="param in form.queryParams" :key="param.name">
            <n-form-item :label="param.label || param.name" size="small" :show-feedback="false">
              <n-input 
                v-model:value="previewParamValues[param.name]"
                size="small"
                :placeholder="'输入' + (param.label || param.name)"
              />
            </n-form-item>
          </n-gi>
        </n-grid>
      </div>

      <!-- 数据和图表预览 -->
      <n-tabs v-model:value="previewTab" type="segment" size="small" display-directive="show" @update:value="handleTabChange">
        <n-tab-pane name="data" tab="📊 数据预览">
          <SqlPreview
            ref="sqlPreviewRef"
            :sql="form.sql"
            :data-source-id="dataSourceId"
            :params="previewParamValues"
            :limit="20"
            @preview-success="handlePreviewSuccess"
          />
        </n-tab-pane>
        <n-tab-pane name="chart" tab="📈 图表预览">
          <div class="chart-preview-container">
            <n-spin :show="chartLoading">
              <div ref="chartRef" class="chart-preview"></div>
              <n-empty v-if="chartPreviewData.length === 0 && !chartLoading" description="请先预览数据" size="small" style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);" />
            </n-spin>
          </div>
        </n-tab-pane>
      </n-tabs>

      <!-- 样式配置 -->
      <n-divider style="margin: 16px 0 12px;">
        <n-text depth="3" style="font-size: 12px;">🎨 图表样式</n-text>
      </n-divider>
      <n-grid :cols="3" :x-gap="12">
        <n-gi>
          <n-form-item label="配色方案" size="small" :show-feedback="false">
            <n-select v-model:value="form.colorScheme" :options="colorSchemeOptions" size="small" />
          </n-form-item>
        </n-gi>
        <n-gi>
          <n-form-item label="背景色" size="small" :show-feedback="false">
            <n-color-picker v-model:value="form.backgroundColor" size="small" :show-alpha="false" />
          </n-form-item>
        </n-gi>
        <n-gi>
          <n-form-item label="显示图例" size="small" :show-feedback="false">
            <n-switch v-model:value="form.showLegend" size="small" />
          </n-form-item>
        </n-gi>
      </n-grid>
      
      <!-- 导出选项 -->
      <n-divider style="margin: 16px 0 12px;">
        <n-text depth="3" style="font-size: 12px;">📤 导出设置</n-text>
      </n-divider>
      <n-grid :cols="2" :x-gap="12">
        <n-gi>
          <n-space :size="16">
            <n-checkbox v-model:checked="form.allowExportExcel">允许导出Excel</n-checkbox>
            <n-checkbox v-model:checked="form.allowExportPdf">允许导出PDF/图片</n-checkbox>
          </n-space>
        </n-gi>
        <n-gi>
          <n-space v-if="form.allowExportPdf" :size="8" align="center" style="flex-wrap: wrap;">
            <n-select
              v-model:value="form.watermarkType"
              :options="watermarkTypeOptions"
              size="small"
              style="width: 180px"
              placeholder="水印类型"
            />
            <n-input
              v-if="form.watermarkType === 'custom'"
              v-model:value="form.pdfWatermark"
              placeholder="输入自定义水印文字"
              size="small"
              clearable
              style="width: 160px"
            />
          </n-space>
        </n-gi>
      </n-grid>

      <!-- 创建菜单选项 -->
      <n-divider style="margin: 16px 0 12px;" />
      <n-space align="center" :size="16">
        <n-checkbox v-model:checked="form.createMenu">同时创建菜单</n-checkbox>
        <n-tree-select
          v-if="form.createMenu"
          v-model:value="form.parentMenuId"
          :options="menuTreeOptions"
          placeholder="选择父级菜单"
          clearable
          style="width: 200px"
          size="small"
          key-field="id"
          label-field="label"
          children-field="children"
        />
      </n-space>
    </div>

    <template #footer>
      <n-space justify="space-between" style="width: 100%">
        <n-button v-if="currentStep > 1" @click="prevStep">
          <template #icon><n-icon :component="ArrowBackOutline" /></template>
          上一步
        </n-button>
        <div v-else></div>
        
        <n-space>
          <n-button @click="close">取消</n-button>
          <n-button 
            v-if="currentStep < 4" 
            type="primary" 
            :disabled="!canProceed"
            @click="nextStep"
          >
            下一步
            <template #icon><n-icon :component="ArrowForwardOutline" /></template>
          </n-button>
          <n-button 
            v-else 
            type="primary" 
            :loading="creating"
            @click="createChart"
          >
            <template #icon><n-icon :component="CheckmarkOutline" /></template>
            {{ form.createMenu ? '创建图表和菜单' : '创建图表' }}
          </n-button>
        </n-space>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, computed, watch, nextTick } from 'vue'
import { useMessage } from 'naive-ui'
import { 
  ArrowBackOutline, 
  ArrowForwardOutline, 
  CheckmarkOutline,
  FilterOutline,
  RefreshOutline
} from '@vicons/ionicons5'
import echarts from '@/utils/echarts'
import request from '@/api/request'
import { getColorScheme } from '@/utils/chartColorSchemes'
import SqlPreview from './SqlPreview.vue'
import ParamConfig, { type QueryParam } from './ParamConfig.vue'

const props = defineProps<{
  show: boolean
  dataSourceId: number | null
  initialSql?: string
  availableSqls?: string[]
  menuTreeOptions?: any[]
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'success', data: { chartId: number; chartCode: string; menuId?: number }): void
}>()

const message = useMessage()

const showModal = computed({
  get: () => props.show,
  set: (val) => emit('update:show', val)
})

const currentStep = ref(1)
const creating = ref(false)
const loadingFields = ref(false)
const sqlPreviewRef = ref()
const chartRef = ref<HTMLElement>()
const selectedSqlIndex = ref(0)
let chartInstance: echarts.ECharts | null = null

// SQL选项列表（当有多个SQL时使用）
const sqlOptions = computed(() => {
  if (!props.availableSqls || props.availableSqls.length <= 1) return []
  return props.availableSqls.map((sql, index) => ({
    label: `SQL ${index + 1}${index === props.availableSqls!.length - 1 ? ' (推荐)' : ''}: ${sql.substring(0, 50)}...`,
    value: index
  }))
})

// 处理SQL选择
const handleSqlSelect = (index: number) => {
  if (props.availableSqls && props.availableSqls[index]) {
    form.sql = props.availableSqls[index]
  }
}

const chartTypeOptions = [
  { label: '📊 柱状图', value: 'bar' },
  { label: '📈 折线图', value: 'line' },
  { label: '🥧 饼图', value: 'pie' },
  { label: '📉 面积图', value: 'area' },
  { label: '🔵 散点图', value: 'scatter' },
  { label: '📊 条形图', value: 'horizontalBar' }
]

const aggregateOptions = [
  { label: '不聚合', value: 'NONE' },
  { label: 'SUM 求和', value: 'SUM' },
  { label: 'AVG 平均', value: 'AVG' },
  { label: 'COUNT 计数', value: 'COUNT' },
  { label: 'MAX 最大', value: 'MAX' },
  { label: 'MIN 最小', value: 'MIN' }
]

const form = reactive({
  chartName: '',
  chartType: 'bar',
  description: '',
  sql: '',
  fieldMapping: {
    xAxis: null as string | null,
    yAxis: [] as string[],
    group: null as string | null,
    sortField: 'x',
    sortOrder: 'ASC'
  },
  aggregates: {} as Record<string, string>,
  aliases: {} as Record<string, string>,
  queryParams: [] as QueryParam[],
  // 样式配置
  colorScheme: 'default',
  backgroundColor: '#ffffff',
  showLegend: true,
  // 导出配置
  allowExportExcel: true,
  allowExportPdf: true,
  watermarkType: 'none' as string,
  pdfWatermark: '',
  // 菜单配置
  createMenu: true,
  parentMenuId: null as number | null
})

// 配色方案选项
const colorSchemeOptions = [
  { label: '🎨 默认配色', value: 'default' },
  { label: '💼 商务蓝', value: 'business' },
  { label: '🌊 清新冷调', value: 'cool' },
  { label: '🌅 日落暖调', value: 'sunset' },
  { label: '🌲 森林绿意', value: 'forest' },
  { label: '🌙 霓虹暗夜', value: 'neon' },
  { label: '🔵 科技深蓝', value: 'tech' }
]

// 水印类型选项
const watermarkTypeOptions = [
  { label: '无水印', value: 'none' },
  { label: '用户名_IP地址', value: 'user_ip' },
  { label: '自定义文字', value: 'custom' }
]

const fieldOptions = ref<{ label: string; value: string }[]>([])
const previewParamValues = reactive<Record<string, any>>({})
const chartPreviewData = ref<any[]>([])
const chartLoading = ref(false)
const previewTab = ref('data')

// 🔧 记录上一次Y轴字段，用于检测删除操作（Vue的deep watch无法正确追踪数组变化）
const previousYAxisFields = ref<string[]>([])

const canProceed = computed(() => {
  if (currentStep.value === 1) {
    return form.chartName.trim() && form.sql.trim() && form.chartType
  }
  if (currentStep.value === 2) {
    return form.fieldMapping.xAxis && form.fieldMapping.yAxis.length > 0
  }
  return true
})

const nextStep = () => {
  if (currentStep.value === 1) {
    parseFieldsFromSql()
  }
  if (currentStep.value < 4) {
    currentStep.value++
    // 进入步骤4时，自动触发数据预览
    if (currentStep.value === 4) {
      nextTick(() => {
        initPreviewParams()
        // 触发SqlPreview自动加载
        if (sqlPreviewRef.value) {
          sqlPreviewRef.value.executePreview()
        }
      })
    }
  }
}

const prevStep = () => {
  if (currentStep.value > 1) {
    currentStep.value--
  }
}

const close = () => {
  showModal.value = false
  resetForm()
}

const resetForm = () => {
  currentStep.value = 1
  form.chartName = ''
  form.chartType = 'bar'
  form.description = ''
  form.sql = ''
  form.fieldMapping = { xAxis: null, yAxis: [], group: null, sortField: 'x', sortOrder: 'ASC' }
  form.aggregates = {}
  form.aliases = {}
  form.queryParams = []
  // 样式配置重置
  form.colorScheme = 'default'
  form.backgroundColor = '#ffffff'
  form.showLegend = true
  // 导出配置重置
  form.allowExportExcel = true
  form.allowExportPdf = true
  form.pdfWatermark = ''
  // 菜单配置重置
  form.createMenu = true
  form.parentMenuId = null
  fieldOptions.value = []
  chartPreviewData.value = []
  selectedSqlIndex.value = 0
  previewTab.value = 'data'
  Object.keys(previewParamValues).forEach(key => delete previewParamValues[key])
}

// 初始化预览参数值（使用默认值）
const initPreviewParams = () => {
  form.queryParams.forEach(param => {
    if (previewParamValues[param.name] === undefined) {
      previewParamValues[param.name] = param.defaultValue || ''
    }
  })
}

// 字段信息类型
interface FieldInfo {
  alias: string
  actualColumn: string
  expression: string
  isAggregate: boolean
  isFunction: boolean
}

// 从SQL SELECT子句中解析字段信息
const parseFieldsInfo = (sql: string): Record<string, FieldInfo> => {
  const fieldsInfo: Record<string, FieldInfo> = {}
  const selectMatch = sql.match(/SELECT\s+(.+?)\s+FROM/is)
  if (!selectMatch) return fieldsInfo
  
  const selectClause = selectMatch[1]
  const fields = selectClause.split(/,(?![^()]*\))/).map(f => f.trim())
  const aggregateRegex = /^(COUNT|SUM|AVG|MAX|MIN|GROUP_CONCAT)\s*\(/i
  const functionRegex = /^\w+\s*\(/
  
  for (const field of fields) {
    const asMatch = field.match(/(.+?)\s+(?:AS\s+)?[`'"]?(\w+)[`'"]?\s*$/i)
    if (asMatch) {
      const expr = asMatch[1].trim()
      const alias = asMatch[2]
      const isAggregate = aggregateRegex.test(expr)
      const isFunction = functionRegex.test(expr)
      const simpleCol = expr.match(/^(?:\w+\.)?[`'"]?(\w+)[`'"]?$/)
      fieldsInfo[alias] = {
        alias,
        actualColumn: simpleCol ? simpleCol[1] : alias,
        expression: expr,
        isAggregate,
        isFunction: isFunction && !isAggregate
      }
    } else {
      const simpleCol = field.match(/^(?:\w+\.)?[`'"]?(\w+)[`'"]?$/)
      if (simpleCol) {
        fieldsInfo[simpleCol[1]] = {
          alias: simpleCol[1],
          actualColumn: simpleCol[1],
          expression: field,
          isAggregate: false,
          isFunction: false
        }
      }
    }
  }
  return fieldsInfo
}

// 判断SQL是否为复杂SQL
const isComplexSql = (sql: string): boolean => {
  const hasGroupBy = /\bGROUP\s+BY\b/i.test(sql)
  const hasAggregate = /\b(COUNT|SUM|AVG|MAX|MIN|GROUP_CONCAT)\s*\(/i.test(sql)
  const hasSubquery = /\(\s*SELECT\b/i.test(sql)
  const hasUnion = /\bUNION\b/i.test(sql)
  return hasGroupBy || hasAggregate || hasSubquery || hasUnion
}

// 从字段添加参数时，智能处理SQL条件
const handleAddFieldParam = (fieldName: string, param: any) => {
  const placeholder = '${' + fieldName + '}'
  
  let newSql = form.sql.trim()
  if (newSql.endsWith(';')) newSql = newSql.slice(0, -1)
  
  // 生成条件表达式（使用反引号包裹字段名，支持中文和特殊字符）
  const quotedField = '`' + fieldName + '`'
  let condition = param.type === 'number' ? `${quotedField} = ${placeholder}` : `${quotedField} = '${placeholder}'`
  const smartCondition = `(${placeholder} IS NULL OR ${placeholder} = '' OR ${condition})`
  
  if (isComplexSql(newSql)) {
    // 复杂SQL：包装为子查询
    let innerSql = newSql
    let orderByClause = ''
    
    // 使用更精确的正则匹配ORDER BY（支持多行）
    const orderByMatch = innerSql.match(/\s+(ORDER\s+BY\s+[\s\S]+?)$/i)
    if (orderByMatch) {
      orderByClause = ' ' + orderByMatch[1].trim()
      innerSql = innerSql.substring(0, innerSql.length - orderByMatch[0].length).trim()
    }
    
    const isAlreadyWrapped = /^\s*SELECT\s+\*\s+FROM\s*\(\s*SELECT\b/i.test(newSql)
    
    if (isAlreadyWrapped) {
      const hasOuterWhere = /\)\s+AS\s+\w+\s+WHERE\b/i.test(newSql)
      if (hasOuterWhere) {
        if (orderByClause) {
          newSql = newSql.replace(
            /(\)\s+AS\s+\w+\s+WHERE\s+.+?)(\s+ORDER\s+BY\s+[\s\S]+)?$/i,
            `$1 AND ${smartCondition}${orderByClause}`
          )
        } else {
          newSql = newSql + ` AND ${smartCondition}`
        }
      } else {
        if (orderByClause) {
          newSql = innerSql + ` WHERE ${smartCondition}${orderByClause}`
        } else {
          newSql = newSql + ` WHERE ${smartCondition}`
        }
      }
    } else {
      newSql = `SELECT * FROM (${innerSql}) AS _sub WHERE ${smartCondition}${orderByClause}`
    }
    
    form.sql = newSql
    message.success(`已将复杂SQL包装为子查询，并添加 ${fieldName} 的筛选条件`)
  } else {
    // 简单SQL：直接添加WHERE条件
    const fieldsInfo = parseFieldsInfo(newSql)
    const fieldInfo = fieldsInfo[fieldName]
    const actualColumnName = fieldInfo?.actualColumn || fieldName
    
    // 使用反引号包裹列名
    const quotedActualCol = '`' + actualColumnName + '`'
    condition = param.type === 'number' ? `${quotedActualCol} = ${placeholder}` : `${quotedActualCol} = '${placeholder}'`
    const whereCondition = `(${placeholder} IS NULL OR ${placeholder} = '' OR ${condition})`
    
    const hasWhere = /\bWHERE\b/i.test(newSql)
    
    if (hasWhere) {
      newSql = newSql.replace(
        /(\bWHERE\b.+?)(\s*(?:ORDER\s+BY|LIMIT|$))/i,
        `$1 AND ${whereCondition}$2`
      )
    } else {
      newSql = newSql.replace(
        /(\bFROM\b\s+[^\s]+(?:\s+\w+)?)(\s*(?:ORDER\s+BY|LIMIT|$))/i,
        `$1 WHERE ${whereCondition}$2`
      )
    }
    
    form.sql = newSql
    message.success(`已添加字段 ${fieldName}（列: ${actualColumnName}）的WHERE条件`)
  }
}

// 🔧 从SELECT子句中解析出各个字段表达式（正确处理括号嵌套和内部逗号）
const splitSelectFields = (selectClause: string): string[] => {
  const fields: string[] = []
  let depth = 0
  let current = ''
  
  for (let i = 0; i < selectClause.length; i++) {
    const ch = selectClause[i]
    if (ch === '(') {
      depth++
      current += ch
    } else if (ch === ')') {
      depth--
      current += ch
    } else if (ch === ',' && depth === 0) {
      fields.push(current.trim())
      current = ''
    } else {
      current += ch
    }
  }
  if (current.trim()) {
    fields.push(current.trim())
  }
  return fields
}

// 🔧 判断一个SELECT字段表达式是否匹配要删除的字段名
// fieldExpr: 如 "c.type AS 优惠券类型", "COUNT(*) AS 发放数量"
// fieldName: 如 "type", "发放数量", "优惠券类型"
const fieldExprMatchesName = (fieldExpr: string, fieldName: string): boolean => {
  const trimmed = fieldExpr.trim()
  const escaped = fieldName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  
  // 1. 匹配别名: ... AS fieldName
  if (new RegExp(`\\bAS\\s+[\`']?${escaped}[\`']?\\s*$`, 'i').test(trimmed)) {
    return true
  }
  
  // 2. 匹配列名（支持 table.col 格式）: 
  //    "c.type AS 优惠券类型" 匹配 "type"
  //    "c.type AS 优惠券类型" 匹配 "c.type"
  //    "type AS 优惠券类型" 匹配 "type"
  const exprBeforeAs = trimmed.replace(/\s+AS\s+.*$/i, '').trim()
  if (exprBeforeAs.toLowerCase() === fieldName.toLowerCase()) {
    return true // 完整匹配: "c.type" === "c.type"
  }
  // table.column 的 column 部分匹配
  const dotParts = exprBeforeAs.split('.')
  if (dotParts.length === 2 && dotParts[1].replace(/[`'"]/g, '').toLowerCase() === fieldName.toLowerCase()) {
    return true // "c.type" 的 "type" 匹配 "type"
  }
  
  return false
}

// 🔧 从字段表达式中提取原始列表达式（用于GROUP BY清理）
// "c.type AS 优惠券类型" → "c.type"
// "COUNT(*) AS 发放数量" → null（聚合函数不在GROUP BY中）
const extractColumnExpr = (fieldExpr: string): string | null => {
  const trimmed = fieldExpr.trim()
  const exprBeforeAs = trimmed.replace(/\s+AS\s+.*$/i, '').trim()
  
  // 如果是聚合函数（包含括号且以函数名开头），返回null
  if (/^(COUNT|SUM|AVG|MAX|MIN|ROUND|COALESCE|IF|CASE)\s*\(/i.test(exprBeforeAs)) {
    return null
  }
  
  return exprBeforeAs
}

// 🔧 Y轴字段变化处理器 - 直接响应删除操作并更新SQL
const handleYAxisChange = (newFields: string[]) => {
  // 检测被删除的字段
  const removedFields = previousYAxisFields.value.filter(f => !newFields.includes(f))
  
  if (removedFields.length > 0 && form.sql) {
    // 清理聚合和别名配置
    removedFields.forEach(f => {
      delete form.aggregates[f]
      delete form.aliases[f]
    })
    
    // 🔧 通过解析SELECT字段列表来精确移除
    let updatedSql = form.sql.replace(/\s+/g, ' ').trim()
    
    // 找最内层的 SELECT（非 SELECT *）
    let selectStart = -1
    let fromEnd = -1
    const selectRegex = /\bSELECT\s+/gi
    let lastSelectMatch = null
    let m
    while ((m = selectRegex.exec(updatedSql)) !== null) {
      const afterSelect = updatedSql.substring(m.index + m[0].length).trimStart()
      if (!afterSelect.startsWith('*')) {
        lastSelectMatch = m
      }
    }
    
    if (lastSelectMatch) {
      selectStart = lastSelectMatch.index + lastSelectMatch[0].length
      
      // 找到对应的 FROM（注意括号嵌套）
      let depth = 0
      for (let i = selectStart; i < updatedSql.length; i++) {
        const ch = updatedSql[i]
        if (ch === '(') depth++
        else if (ch === ')') depth--
        if (depth === 0 && updatedSql.substring(i).match(/^\bFROM\b/i)) {
          fromEnd = i
          break
        }
      }
    }
    
    if (selectStart >= 0 && fromEnd > selectStart) {
      const selectClause = updatedSql.substring(selectStart, fromEnd).trim()
      const fields = splitSelectFields(selectClause)
      // 收集被移除的列表达式（用于清理GROUP BY）
      const removedColumnExprs: string[] = []
      
      // 过滤掉被删除的字段（同时匹配别名和列名）
      const filteredFields = fields.filter(field => {
        for (const name of removedFields) {
          if (fieldExprMatchesName(field, name)) {
            const colExpr = extractColumnExpr(field)
            if (colExpr) removedColumnExprs.push(colExpr)
            return false
          }
        }
        return true
      })
      
      if (filteredFields.length < fields.length) {
        // 重新拼接SELECT部分
        const newSelectClause = filteredFields.join(', ')
        updatedSql = updatedSql.substring(0, selectStart) + newSelectClause + ' ' + updatedSql.substring(fromEnd)
        
        // 🔧 清理GROUP BY中对应的列
        if (removedColumnExprs.length > 0) {
          const groupByMatch = updatedSql.match(/\bGROUP\s+BY\s+/i)
          if (groupByMatch && groupByMatch.index !== undefined) {
            const gbStart = groupByMatch.index + groupByMatch[0].length
            // 找到GROUP BY子句的结束位置（遇到 ORDER BY / LIMIT / HAVING / ) 或字符串末尾）
            const gbEndMatch = updatedSql.substring(gbStart).match(/\b(ORDER\s+BY|LIMIT|HAVING)\b|\)/i)
            const gbEnd = gbEndMatch ? gbStart + gbEndMatch.index! : updatedSql.length
            
            const gbClause = updatedSql.substring(gbStart, gbEnd).trim()
            const gbFields = splitSelectFields(gbClause)
            const filteredGbFields = gbFields.filter(gbField => {
              const gbTrimmed = gbField.trim()
              for (const colExpr of removedColumnExprs) {
                if (gbTrimmed.toLowerCase() === colExpr.toLowerCase()) {
                  return false
                }
              }
              return true
            })
            
            if (filteredGbFields.length < gbFields.length) {
              if (filteredGbFields.length === 0) {
                // 如果GROUP BY所有字段都删完了，移除整个GROUP BY子句
                updatedSql = updatedSql.substring(0, groupByMatch.index) + updatedSql.substring(gbEnd)
              } else {
                updatedSql = updatedSql.substring(0, gbStart) + filteredGbFields.join(', ') + ' ' + updatedSql.substring(gbEnd)
              }
            }
          }
        }
        
        // 清理多余空格
        updatedSql = updatedSql.replace(/\s+/g, ' ').trim()
        
        form.sql = updatedSql
      }
    }
  }
  
  // 初始化新字段的聚合配置
  newFields.forEach(f => {
    if (!form.aggregates[f]) {
      form.aggregates[f] = 'NONE'
    }
  })
  
  // 更新记录
  previousYAxisFields.value = [...newFields]
}

const parseFieldsFromSql = async () => {
  if (!form.sql || !props.dataSourceId) {
    message.warning('请先输入SQL语句')
    return
  }
  
  loadingFields.value = true
  try {
    // 执行SQL获取实际字段（限制1条数据即可）
    let testSql = form.sql.trim()
    if (testSql.endsWith(';')) {
      testSql = testSql.slice(0, -1)
    }
    
    // 替换参数占位符为空字符串，用于测试查询
    testSql = testSql.replace(/\$\{\w+\}/g, "''")
    
    const res: any = await request.post('/ai/execute-sql', {
      dataSourceId: props.dataSourceId,
      sql: testSql,
      limit: 1
    })
    
    if (res?.data?.success && res.data.columns) {
      const fields = res.data.columns
      fieldOptions.value = fields.map((f: string) => ({ label: f, value: f }))
      
      // 自动设置第一个字段为X轴，其余为Y轴
      if (fields.length >= 2 && !form.fieldMapping.xAxis) {
        form.fieldMapping.xAxis = fields[0]
        form.fieldMapping.yAxis = fields.slice(1, 3) // 默认取前2个指标
        previousYAxisFields.value = [...form.fieldMapping.yAxis]
      }
      
      message.success(`已获取 ${fields.length} 个字段`)
    } else {
      // 回退到SQL文本解析
      fallbackParseSql()
      if (res?.data?.error) {
        message.warning('SQL执行失败，已使用文本解析: ' + res.data.error)
      }
    }
  } catch (e: any) {
    console.error('解析SQL字段失败', e)
    // 回退到SQL文本解析
    fallbackParseSql()
  } finally {
    loadingFields.value = false
  }
}

// 回退方案：从SQL文本解析字段
const fallbackParseSql = () => {
  const selectMatch = form.sql.match(/SELECT\s+([\s\S]+?)\s+FROM/i)
  if (selectMatch) {
    const selectPart = selectMatch[1]
    const fields: string[] = []
    
    const fieldParts = selectPart.split(',')
    fieldParts.forEach(part => {
      const trimmed = part.trim()
      const asMatch = trimmed.match(/\s+(?:AS\s+)?[`'"]?(\w+)[`'"]?\s*$/i)
      if (asMatch) {
        fields.push(asMatch[1])
      } else {
        const simpleField = trimmed.replace(/.*\./, '').replace(/[`'"]/g, '').trim()
        if (simpleField && simpleField !== '*') {
          fields.push(simpleField)
        }
      }
    })
    
    if (fields.length > 0) {
      fieldOptions.value = fields.map(f => ({ label: f, value: f }))
      
      if (fields.length >= 2 && !form.fieldMapping.xAxis) {
        form.fieldMapping.xAxis = fields[0]
        form.fieldMapping.yAxis = fields.slice(1)
        previousYAxisFields.value = [...form.fieldMapping.yAxis]
      }
    }
  }
}

// 切换到图表标签时重新渲染
let renderRetryCount = 0
const maxRetries = 10

const handleTabChange = (tab: string) => {
  if (tab === 'chart' && chartPreviewData.value.length > 0) {
    // 重置重试计数
    renderRetryCount = 0
    // 销毁旧实例，确保重新创建
    if (chartInstance) {
      chartInstance.dispose()
      chartInstance = null
    }
    // 延迟渲染，确保DOM已切换
    nextTick(() => {
      setTimeout(() => {
        renderChartPreview()
      }, 100)
    })
  }
}

const handlePreviewSuccess = (data: { columns: string[], data: any[], total: number }) => {
  chartPreviewData.value = data.data
  
  // 更新字段选项
  if (data.columns.length > 0) {
    fieldOptions.value = data.columns.map(c => ({ label: c, value: c }))
    
    // 🔧 智能设置字段映射（如果未设置）
    if (!form.fieldMapping.xAxis && data.columns.length >= 1) {
      const firstRow = data.data[0] || {}
      // X轴优先选择日期或字符串字段
      const xField = data.columns.find(k => {
        const val = firstRow[k]
        return typeof val === 'string' && isNaN(Number(val))
      }) || data.columns[0]
      form.fieldMapping.xAxis = xField
    }
    if (form.fieldMapping.yAxis.length === 0 && data.columns.length >= 2) {
      const firstRow = data.data[0] || {}
      const xAxis = form.fieldMapping.xAxis
      // Y轴优先选择数值字段（排除X轴字段）
      const yField = data.columns.find(k => {
        if (k === xAxis) return false
        const val = firstRow[k]
        return typeof val === 'number' || (typeof val === 'string' && !isNaN(Number(val)) && val !== '')
      }) || data.columns.find(k => k !== xAxis) || data.columns[1]
      form.fieldMapping.yAxis = [yField]
      previousYAxisFields.value = [yField]
    }
  }
  
  // 渲染图表预览 - 使用双重nextTick确保DOM已更新
  nextTick(() => {
    nextTick(() => {
      renderChartPreview()
    })
  })
}

// 判断是否为深色背景
const isDarkColor = (color: string): boolean => {
  if (!color || color === 'transparent') return false
  const hex = color.replace('#', '')
  if (hex.length !== 6) return false
  const r = parseInt(hex.substring(0, 2), 16)
  const g = parseInt(hex.substring(2, 4), 16)
  const b = parseInt(hex.substring(4, 6), 16)
  const brightness = (r * 299 + g * 587 + b * 114) / 1000
  return brightness < 128
}

const renderChartPreview = () => {
  if (!chartRef.value) {
    if (renderRetryCount < maxRetries) {
      renderRetryCount++
      setTimeout(() => renderChartPreview(), 100)
    }
    return
  }
  if (chartPreviewData.value.length === 0) {
    console.warn('[renderChartPreview] no preview data')
    return
  }
  
  // 确保容器有尺寸
  if (chartRef.value.offsetWidth === 0 || chartRef.value.offsetHeight === 0) {
    if (renderRetryCount < maxRetries) {
      renderRetryCount++
      setTimeout(() => renderChartPreview(), 100)
    }
    return
  }
  
  // 重置重试计数
  renderRetryCount = 0
  
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  } else {
    chartInstance.resize()
  }
  
  // 🔧 自动推断字段映射（如果未设置）
  let xAxis = form.fieldMapping.xAxis
  let yAxisFields = form.fieldMapping.yAxis.length > 0 ? [...form.fieldMapping.yAxis] : []
  
  // 如果字段映射未设置，从数据中智能推断
  if (!xAxis || yAxisFields.length === 0) {
    const firstRow = chartPreviewData.value[0] || {}
    const keys = Object.keys(firstRow)
    if (keys.length >= 2) {
      if (!xAxis) {
        xAxis = keys.find(k => {
          const val = firstRow[k]
          return typeof val === 'string' && isNaN(Number(val))
        }) || keys[0]
      }
      if (yAxisFields.length === 0) {
        const yField = keys.find(k => {
          if (k === xAxis) return false
          const val = firstRow[k]
          return typeof val === 'number' || (typeof val === 'string' && !isNaN(Number(val)) && val !== '')
        }) || keys.find(k => k !== xAxis) || keys[1]
        yAxisFields = [yField]
      }
      if (!form.fieldMapping.xAxis) form.fieldMapping.xAxis = xAxis
      if (form.fieldMapping.yAxis.length === 0) {
        form.fieldMapping.yAxis = [...yAxisFields]
        previousYAxisFields.value = [...yAxisFields]
      }
    }
  }
  
  if (!xAxis || yAxisFields.length === 0) {
    console.warn('[renderChartPreview] xAxis or yAxis still not set after auto-infer')
    return
  }
  
  const xData = chartPreviewData.value.map(row => row[xAxis])
  
  // 根据背景色判断文字颜色
  const bgColor = form.backgroundColor || '#ffffff'
  const isDark = isDarkColor(bgColor)
  const textColor = isDark ? '#ffffff' : '#333333'
  const axisLineColor = isDark ? 'rgba(255,255,255,0.3)' : 'rgba(0,0,0,0.1)'
  
  // 获取配色方案颜色
  const schemeColors = getColorScheme(form.colorScheme || 'default').colors
  
  // 🔧 为每个Y轴字段创建一个series
  const seriesList = yAxisFields.map(yField => ({
    name: form.aliases[yField] || yField,
    type: form.chartType === 'horizontalBar' ? 'bar' : (form.chartType as any),
    data: chartPreviewData.value.map(row => Number(row[yField]) || 0)
  }))
  
  const option: echarts.EChartsOption = {
    backgroundColor: bgColor,
    color: schemeColors,
    title: { 
      text: form.chartName || '图表预览', 
      left: 'center', 
      textStyle: { fontSize: 14, color: textColor } 
    },
    tooltip: { trigger: 'axis' },
    legend: yAxisFields.length > 1 ? {
      data: yAxisFields.map(f => form.aliases[f] || f),
      top: 30,
      textStyle: { color: textColor }
    } : undefined,
    grid: yAxisFields.length > 1 ? { top: 60 } : undefined,
    xAxis: { 
      type: 'category', 
      data: xData,
      axisLabel: { color: textColor },
      axisLine: { lineStyle: { color: axisLineColor } }
    },
    yAxis: { 
      type: 'value',
      axisLabel: { color: textColor },
      axisLine: { lineStyle: { color: axisLineColor } },
      splitLine: { lineStyle: { color: axisLineColor } }
    },
    series: seriesList
  }
  
  if (form.chartType === 'pie') {
    option.xAxis = undefined
    option.yAxis = undefined
    option.legend = undefined
    option.grid = undefined
    option.series = [{
      type: 'pie',
      radius: '60%',
      label: { color: textColor },
      data: chartPreviewData.value.map(row => ({
        name: row[xAxis],
        value: Number(row[yAxisFields[0]]) || 0
      }))
    }]
  }
  
  chartInstance.setOption(option, true)
}

const createChart = async () => {
  if (!form.chartName.trim()) {
    message.warning('请输入图表名称')
    return
  }
  if (!props.dataSourceId) {
    message.warning('请选择数据源')
    return
  }

  creating.value = true
  try {
    let cleanSql = form.sql.trim()
    if (cleanSql.endsWith(';')) {
      cleanSql = cleanSql.slice(0, -1)
    }

    const apiUrl = form.createMenu ? '/ai/create-chart-with-menu' : '/ai/create-chart'
    const requestData: any = {
      chartName: form.chartName,
      chartType: form.chartType,
      description: form.description,
      dataSourceId: props.dataSourceId,
      sqlContent: cleanSql,
      fieldMapping: {
        xAxis: form.fieldMapping.xAxis,
        yAxis: form.fieldMapping.yAxis,
        group: form.fieldMapping.group,
        sortField: form.fieldMapping.sortField,
        sortOrder: form.fieldMapping.sortOrder
      },
      aggregates: form.aggregates,
      aliases: form.aliases,
      queryParams: form.queryParams.map(p => ({
        name: p.name,
        label: p.label,
        type: p.type,
        defaultValue: p.defaultValue,
        required: p.required,
        options: p.options,
        optionsSql: p.optionsSql
      })),
      // 样式配置
      styleConfig: {
        colorScheme: form.colorScheme,
        backgroundColor: form.backgroundColor,
        showLegend: form.showLegend
      },
      // 导出配置
      allowExportExcel: form.allowExportExcel,
      allowExportPdf: form.allowExportPdf,
      watermarkType: form.watermarkType || 'none',
      pdfWatermark: form.watermarkType === 'custom' ? (form.pdfWatermark || null) : null,
      icon: 'BarChartOutline'
    }

    if (form.createMenu) {
      requestData.parentMenuId = form.parentMenuId || 0
    }

    const res: any = await request.post(apiUrl, requestData)

    if (res?.data?.success || res?.data?.chartId) {
      message.success(res.data?.message || '图表创建成功')
      emit('success', {
        chartId: res.data.chartId,
        chartCode: res.data.chartCode,
        menuId: res.data.menuId
      })
      close()
    } else {
      message.error('创建失败：' + (res?.data?.error || res?.msg || '未知错误'))
    }
  } catch (e: any) {
    message.error('创建失败：' + (e.message || '网络错误'))
  } finally {
    creating.value = false
  }
}

// 初始化SQL
watch(() => props.initialSql, (sql) => {
  if (sql) {
    form.sql = sql
  }
}, { immediate: true })

// 弹窗显示时重置表单
watch(() => props.show, (show) => {
  if (show) {
    resetForm()
    // 设置SQL，优先使用availableSqls中的最后一个（推荐）
    if (props.availableSqls && props.availableSqls.length > 0) {
      selectedSqlIndex.value = props.availableSqls.length - 1
      form.sql = props.availableSqls[selectedSqlIndex.value]
    } else if (props.initialSql) {
      form.sql = props.initialSql
    }
  }
}, { immediate: false })

// Y轴字段变化时初始化聚合配置（删除逻辑由handleYAxisChange处理）
watch(() => form.fieldMapping.yAxis, (newFields) => {
  // 仅初始化新字段的聚合配置
  newFields.forEach(f => {
    if (!form.aggregates[f]) {
      form.aggregates[f] = 'NONE'
    }
  })
}, { deep: true })

// 背景色或配色方案变化时重新渲染图表
watch([() => form.backgroundColor, () => form.colorScheme], () => {
  if (chartInstance && chartPreviewData.value.length > 0) {
    renderChartPreview()
  }
})

// 清理图表实例
watch(showModal, (show) => {
  if (!show && chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.step-content {
  min-height: 320px;
}

.preview-params {
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.aggregate-config {
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  margin-top: 16px;
}

.aggregate-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.aggregate-item:last-child {
  margin-bottom: 0;
}

.chart-preview-container {
  min-height: 300px;
  padding: 12px;
  position: relative;
}

.chart-preview {
  width: 100%;
  height: 280px;
  background: #fafafa;
  border-radius: 8px;
}
</style>

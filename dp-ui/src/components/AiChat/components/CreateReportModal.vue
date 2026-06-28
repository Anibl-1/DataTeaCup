<template>
  <n-modal
    v-model:show="showModal"
    preset="card"
    title="📊 创建报表"
    style="width: 720px; max-width: 95vw;"
    :mask-closable="false"
    :segmented="{ content: 'soft', footer: 'soft' }"
  >
    <template #header-extra>
      <n-tag type="info" size="small">
        步骤 {{ currentStep }}/3
      </n-tag>
    </template>

    <!-- 步骤指示器 -->
    <n-steps :current="currentStep" size="small" style="margin-bottom: 20px;">
      <n-step title="基本信息" />
      <n-step title="参数配置" />
      <n-step title="预览确认" />
    </n-steps>

    <!-- 步骤1：基本信息 -->
    <div v-show="currentStep === 1" class="step-content">
      <n-form ref="formRef" :model="form" label-placement="left" label-width="80">
        <n-form-item label="报表名称" required>
          <n-input v-model:value="form.reportName" placeholder="输入报表名称" />
        </n-form-item>
        <n-form-item label="报表描述">
          <n-input v-model:value="form.description" type="textarea" :rows="2" placeholder="描述报表用途" />
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
            placeholder="SELECT * FROM table WHERE create_time >= ${startDate}"
            font-family="monospace"
          />
        </n-form-item>
      </n-form>
      
      <n-alert v-if="sqlOptions.length > 1" type="warning" :bordered="false" style="margin-top: 12px;">
        ⚠️ AI返回了 {{ sqlOptions.length }} 个SQL，已默认选择最优方案，您也可以切换其他SQL
      </n-alert>
      <n-alert type="info" :bordered="false" style="margin-top: 12px;">
        💡 在SQL中使用 <n-text code>${参数名}</n-text> 定义查询参数，如 <n-text code>${startDate}</n-text>
      </n-alert>
    </div>

    <!-- 步骤2：参数配置 -->
    <div v-show="currentStep === 2" class="step-content">
      <!-- 字段加载提示 -->
      <n-alert v-if="loadingFields" type="info" :bordered="false" style="margin-bottom: 12px;">
        <template #icon>
          <n-spin size="small" />
        </template>
        正在分析SQL获取可用字段...
      </n-alert>
      
      <!-- 字段选择提示 -->
      <n-alert v-if="availableFields.length > 0" type="success" :bordered="false" style="margin-bottom: 12px;">
        ✅ 已获取 {{ availableFields.length }} 个字段，可从下方选择字段创建筛选参数
      </n-alert>
      <n-alert v-else-if="!loadingFields && fieldFetchError" type="warning" :bordered="false" style="margin-bottom: 12px;">
        ⚠️ {{ fieldFetchError }}
        <n-button size="tiny" type="primary" text style="margin-left: 8px;" @click="fetchSqlFields">
          重新获取
        </n-button>
      </n-alert>
      
      <ParamConfig 
        v-model="form.queryParams" 
        :sql="form.sql"
        :available-fields="availableFields"
        @add-field-param="handleAddFieldParam"
      />
    </div>

    <!-- 步骤3：预览确认 -->
    <div v-show="currentStep === 3" class="step-content">
      <!-- 参数输入区 -->
      <div v-if="form.queryParams.length > 0" class="preview-params">
        <n-text strong style="margin-bottom: 8px; display: block;">
          🔍 输入参数值进行预览
        </n-text>
        <n-grid :cols="3" :x-gap="12" :y-gap="8">
          <n-gi v-for="param in form.queryParams" :key="param.name">
            <n-form-item :label="param.label || param.name" size="small" :show-feedback="false">
              <n-input 
                v-model:value="previewParamValues[param.name]"
                size="small"
                :placeholder="param.defaultValue || '请输入'"
              />
            </n-form-item>
          </n-gi>
        </n-grid>
      </div>

      <!-- SQL预览 -->
      <SqlPreview
        ref="sqlPreviewRef"
        :sql="form.sql"
        :data-source-id="dataSourceId"
        :params="previewParamValues"
        :limit="10"
        @preview-success="handlePreviewSuccess"
      />

      <!-- 导出选项 -->
      <n-divider style="margin: 16px 0 12px;" />
      <n-form-item label="导出权限" :show-feedback="false">
        <n-space :size="20">
          <n-checkbox v-model:checked="form.allowExportExcel">
            <n-space :size="4" align="center">
              <n-icon size="16" color="#217346"><DocumentOutline /></n-icon>
              允许导出Excel
            </n-space>
          </n-checkbox>
          <n-checkbox v-model:checked="form.allowExportPdf">
            <n-space :size="4" align="center">
              <n-icon size="16" color="#E53935"><DocumentOutline /></n-icon>
              允许导出PDF
            </n-space>
          </n-checkbox>
          <n-checkbox v-model:checked="form.allowPrint">
            <n-space :size="4" align="center">
              <n-icon size="16" color="#1565C0"><DocumentOutline /></n-icon>
              允许打印
            </n-space>
          </n-checkbox>
        </n-space>
      </n-form-item>
      
      <!-- 水印配置 -->
      <n-form-item v-if="form.allowExportPdf" label="导出水印" :show-feedback="false">
        <n-space :size="8" align="center" style="flex-wrap: wrap;">
          <n-select
            v-model:value="form.watermarkType"
            :options="watermarkTypeOptions"
            style="width: 180px"
            placeholder="水印类型"
          />
          <n-input
            v-if="form.watermarkType === 'custom'"
            v-model:value="form.pdfWatermark"
            placeholder="输入自定义水印文字"
            clearable
            style="width: 200px"
          />
        </n-space>
      </n-form-item>

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
            v-if="currentStep < 3" 
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
            @click="createReport"
          >
            <template #icon><n-icon :component="CheckmarkOutline" /></template>
            {{ form.createMenu ? '创建报表和菜单' : '创建报表' }}
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
  DocumentOutline
} from '@vicons/ionicons5'
import request from '@/api/request'
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
  (e: 'success', data: { reportId: number; reportCode: string; menuId?: number }): void
}>()

const message = useMessage()

const showModal = computed({
  get: () => props.show,
  set: (val) => emit('update:show', val)
})

const currentStep = ref(1)
const creating = ref(false)
const sqlPreviewRef = ref()
const selectedSqlIndex = ref(0)

const form = reactive({
  reportName: '',
  description: '',
  sql: '',
  queryParams: [] as QueryParam[],
  createMenu: true,
  parentMenuId: null as number | null,
  allowExportExcel: true,
  allowExportPdf: true,
  allowPrint: true,
  watermarkType: 'none' as string,
  pdfWatermark: ''
})

// 水印类型选项
const watermarkTypeOptions = [
  { label: '无水印', value: 'none' },
  { label: '用户名_IP地址', value: 'user_ip' },
  { label: '自定义文字', value: 'custom' }
]

const previewParamValues = reactive<Record<string, any>>({})
const availableFields = ref<string[]>([])
const loadingFields = ref(false)
const fieldFetchError = ref('')

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

const canProceed = computed(() => {
  if (currentStep.value === 1) {
    return form.reportName.trim() && form.sql.trim()
  }
  return true
})

const nextStep = async () => {
  if (currentStep.value < 3) {
    currentStep.value++
    // 进入步骤2时，获取SQL字段列表
    if (currentStep.value === 2) {
      await fetchSqlFields()
    }
    // 进入步骤3时，自动初始化参数并触发预览
    if (currentStep.value === 3) {
      nextTick(() => {
        initPreviewParams()
        if (sqlPreviewRef.value) {
          sqlPreviewRef.value.executePreview()
        }
      })
    }
  }
}

// 获取SQL字段列表
const fetchSqlFields = async () => {
  if (!props.dataSourceId || !form.sql.trim()) {
    fieldFetchError.value = '请先选择数据源并输入SQL'
    return
  }
  
  loadingFields.value = true
  fieldFetchError.value = ''
  
  try {
    // 执行SQL获取字段（限制1条数据即可）
    let testSql = form.sql.trim()
    if (testSql.endsWith(';')) {
      testSql = testSql.slice(0, -1)
    }
    
    // 替换参数占位符为空字符串或默认值，用于测试查询
    testSql = testSql.replace(/\$\{\w+\}/g, "''")
    
    const res: any = await request.post('/ai/execute-sql', {
      dataSourceId: props.dataSourceId,
      sql: testSql,
      limit: 1
    })
    
    if (res?.data?.success && res.data.columns) {
      availableFields.value = res.data.columns
    } else {
      fieldFetchError.value = res?.data?.error || '获取字段失败，请检查SQL语法'
    }
  } catch (e: any) {
    fieldFetchError.value = e.message || '网络错误'
  } finally {
    loadingFields.value = false
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
  form.reportName = ''
  form.description = ''
  form.sql = ''
  form.queryParams = []
  form.createMenu = true
  form.parentMenuId = null
  form.allowExportExcel = true
  form.allowExportPdf = true
  form.allowPrint = true
  form.watermarkType = 'none'
  form.pdfWatermark = ''
  selectedSqlIndex.value = 0
  availableFields.value = []
  loadingFields.value = false
  fieldFetchError.value = ''
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
  alias: string           // 别名（SELECT中显示的名称）
  actualColumn: string    // 实际列名
  expression: string      // 完整表达式
  isAggregate: boolean    // 是否是聚合函数
  isFunction: boolean     // 是否是函数表达式
}

// 从SQL SELECT子句中解析字段信息
const parseFieldsInfo = (sql: string): Record<string, FieldInfo> => {
  const fieldsInfo: Record<string, FieldInfo> = {}
  
  // 提取SELECT和FROM之间的内容
  const selectMatch = sql.match(/SELECT\s+(.+?)\s+FROM/is)
  if (!selectMatch) return fieldsInfo
  
  const selectClause = selectMatch[1]
  // 分割字段（注意处理函数中的逗号）
  const fields = selectClause.split(/,(?![^()]*\))/).map(f => f.trim())
  
  // 聚合函数正则
  const aggregateRegex = /^(COUNT|SUM|AVG|MAX|MIN|GROUP_CONCAT)\s*\(/i
  // 函数表达式正则
  const functionRegex = /^\w+\s*\(/
  
  for (const field of fields) {
    // 匹配 "表达式 AS 别名" 或 "表达式 别名" 格式
    const asMatch = field.match(/(.+?)\s+(?:AS\s+)?[`'"]?(\w+)[`'"]?\s*$/i)
    if (asMatch) {
      const expr = asMatch[1].trim()
      const alias = asMatch[2]
      
      const isAggregate = aggregateRegex.test(expr)
      const isFunction = functionRegex.test(expr)
      
      // 如果表达式是简单列名（可能带表前缀）
      const simpleCol = expr.match(/^(?:\w+\.)?[`'"]?(\w+)[`'"]?$/)
      
      fieldsInfo[alias] = {
        alias,
        actualColumn: simpleCol ? simpleCol[1] : alias,
        expression: expr,
        isAggregate,
        isFunction: isFunction && !isAggregate
      }
    } else {
      // 没有别名，字段本身就是列名
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

// 判断SQL是否为复杂SQL（包含GROUP BY、聚合函数、子查询等）
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
  if (newSql.endsWith(';')) {
    newSql = newSql.slice(0, -1)
  }
  
  // 生成条件表达式（使用反引号包裹字段名，支持中文和特殊字符）
  const quotedField = '`' + fieldName + '`'
  let condition = ''
  if (param.type === 'number') {
    condition = `${quotedField} = ${placeholder}`
  } else {
    condition = `${quotedField} = '${placeholder}'`
  }
  // 简化条件：只在有值时才过滤（避免NULL比较问题）
  const smartCondition = `(${placeholder} IS NULL OR ${placeholder} = '' OR ${condition})`
  
  // 判断是否为复杂SQL
  if (isComplexSql(newSql)) {
    // 复杂SQL：包装为子查询，在外层添加WHERE条件
    // 移除可能存在的ORDER BY（保留到外层）
    let innerSql = newSql
    let orderByClause = ''
    
    // 使用更精确的正则匹配ORDER BY（支持多行）
    const orderByMatch = innerSql.match(/\s+(ORDER\s+BY\s+[\s\S]+?)$/i)
    if (orderByMatch) {
      orderByClause = ' ' + orderByMatch[1].trim()
      innerSql = innerSql.substring(0, innerSql.length - orderByMatch[0].length).trim()
    }
    
    // 检查是否已经是包装过的SQL
    const isAlreadyWrapped = /^\s*SELECT\s+\*\s+FROM\s*\(\s*SELECT\b/i.test(newSql)
    
    if (isAlreadyWrapped) {
      // 已经包装过，在外层WHERE中添加条件
      const hasOuterWhere = /\)\s+AS\s+\w+\s+WHERE\b/i.test(newSql)
      if (hasOuterWhere) {
        // 在现有WHERE条件后追加AND
        if (orderByClause) {
          newSql = newSql.replace(
            /(\)\s+AS\s+\w+\s+WHERE\s+.+?)(\s+ORDER\s+BY\s+[\s\S]+)?$/i,
            `$1 AND ${smartCondition}${orderByClause}`
          )
        } else {
          newSql = newSql + ` AND ${smartCondition}`
        }
      } else {
        // 添加WHERE
        if (orderByClause) {
          newSql = innerSql + ` WHERE ${smartCondition}${orderByClause}`
        } else {
          newSql = newSql + ` WHERE ${smartCondition}`
        }
      }
    } else {
      // 首次包装：SELECT * FROM (原SQL) AS _sub WHERE 条件
      newSql = `SELECT * FROM (${innerSql}) AS _sub WHERE ${smartCondition}${orderByClause}`
    }
    
    form.sql = newSql
    message.success(`已将复杂SQL包装为子查询，并添加 ${fieldName} 的筛选条件`)
    
  } else {
    // 简单SQL：直接添加WHERE条件
    const fieldsInfo = parseFieldsInfo(newSql)
    const fieldInfo = fieldsInfo[fieldName]
    const actualColumnName = fieldInfo?.actualColumn || fieldName
    
    // 重新生成使用实际列名的条件（使用反引号包裹）
    const quotedActualCol = '`' + actualColumnName + '`'
    if (param.type === 'number') {
      condition = `${quotedActualCol} = ${placeholder}`
    } else {
      condition = `${quotedActualCol} = '${placeholder}'`
    }
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

const handlePreviewSuccess = (data: { columns: string[], data: any[], total: number }) => {
  message.success(`预览成功，共 ${data.total} 条数据`)
  // 更新可用字段列表
  if (data.columns && data.columns.length > 0) {
    availableFields.value = data.columns
  }
}

const createReport = async () => {
  if (!form.reportName.trim()) {
    message.warning('请输入报表名称')
    return
  }
  if (!form.sql.trim()) {
    message.warning('请输入SQL语句')
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

    const apiUrl = form.createMenu ? '/ai/create-report-with-menu' : '/ai/create-report'
    const requestData: any = {
      reportName: form.reportName,
      description: form.description,
      dataSourceId: props.dataSourceId,
      sqlContent: cleanSql,
      allowExportExcel: form.allowExportExcel,
      allowExportPdf: form.allowExportPdf,
      allowPrint: form.allowPrint,
      watermarkType: form.watermarkType || 'none',
      pdfWatermark: form.watermarkType === 'custom' ? (form.pdfWatermark || null) : null,
      queryParams: form.queryParams.map(p => ({
        name: p.name,
        label: p.label,
        type: p.type,
        defaultValue: p.defaultValue,
        required: p.required,
        options: p.options,
        optionsSql: p.optionsSql
      }))
    }

    if (form.createMenu) {
      requestData.parentMenuId = form.parentMenuId || 0
      requestData.icon = 'DocumentTextOutline'
    }

    const res: any = await request.post(apiUrl, requestData)

    if (res?.data?.success) {
      message.success(res.data.message || '报表创建成功')
      emit('success', {
        reportId: res.data.reportId,
        reportCode: res.data.reportCode,
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
})

// 初始化参数默认值
watch(() => form.queryParams, (params) => {
  params.forEach(p => {
    if (p.defaultValue && !previewParamValues[p.name]) {
      previewParamValues[p.name] = p.defaultValue
    }
  })
}, { deep: true })
</script>

<style scoped>
.step-content {
  min-height: 300px;
}

.preview-params {
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}
</style>

<template>
  <div class="param-config">
    <div class="param-header">
      <n-space align="center" justify="space-between" style="width: 100%;">
        <n-text strong>
          <n-icon :component="SettingsOutline" />
          查询参数配置
          <n-badge v-if="params.length > 0" :value="params.length" type="info" style="margin-left: 8px;" />
        </n-text>
        <n-button size="tiny" quaternary type="primary" @click="detectParams">
          <template #icon><n-icon :component="ScanOutline" /></template>
          自动检测
        </n-button>
      </n-space>
    </div>
    
    <n-alert v-if="detectedParams.length > 0 && params.length === 0" type="info" style="margin: 8px 0;">
      检测到 {{ detectedParams.length }} 个参数: {{ detectedParams.join(', ') }}
      <n-button size="tiny" type="primary" text style="margin-left: 8px;" @click="addDetectedParams">
        一键添加
      </n-button>
    </n-alert>
    
    <div v-if="params.length > 0" class="param-list">
      <n-collapse accordion>
        <n-collapse-item v-for="(param, index) in params" :key="index" :name="index">
          <template #header>
            <n-space align="center" :size="8">
              <n-tag size="small" type="success">{{ param.name }}</n-tag>
              <n-text depth="3">{{ param.label || '未设置标签' }}</n-text>
              <n-tag size="tiny" :bordered="false">{{ getTypeLabel(param.type) }}</n-tag>
            </n-space>
          </template>
          <template #header-extra>
            <n-button size="tiny" quaternary type="error" @click.stop="removeParam(index)">
              <template #icon><n-icon :component="TrashOutline" /></template>
            </n-button>
          </template>
          
          <div class="param-form">
            <n-grid :cols="2" :x-gap="12" :y-gap="8">
              <n-gi>
                <n-form-item label="参数名" size="small" :show-feedback="false">
                  <n-input v-model:value="param.name" placeholder="参数名" size="small" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="显示名称" size="small" :show-feedback="false">
                  <n-input v-model:value="param.label" placeholder="显示名称" size="small" />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="控件类型" size="small" :show-feedback="false">
                  <n-select 
                    v-model:value="param.type" 
                    :options="typeOptions" 
                    size="small"
                    @update:value="handleTypeChange(param)"
                  />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-form-item label="默认值" size="small" :show-feedback="false">
                  <n-input 
                    v-model:value="param.defaultValue" 
                    :placeholder="param.type === 'date' ? '如: TODAY, YESTERDAY' : '默认值'" 
                    size="small" 
                  />
                </n-form-item>
              </n-gi>
              <n-gi v-if="param.type === 'select'" :span="2">
                <n-form-item label="选项列表 (每行一个: 值|显示名)" size="small" :show-feedback="false">
                  <n-input 
                    v-model:value="param.optionsText" 
                    type="textarea" 
                    :rows="3"
                    placeholder="选项1|显示名1&#10;选项2|显示名2"
                    size="small"
                    @blur="parseOptions(param)"
                  />
                </n-form-item>
              </n-gi>
              <n-gi v-if="param.type === 'dynamicSelect'" :span="2">
                <n-form-item label="选项SQL (返回value,label两列)" size="small" :show-feedback="false">
                  <n-input 
                    v-model:value="param.optionsSql" 
                    type="textarea" 
                    :rows="2"
                    placeholder="SELECT id as value, name as label FROM table"
                    size="small"
                  />
                </n-form-item>
              </n-gi>
              <n-gi>
                <n-checkbox v-model:checked="param.required">必填</n-checkbox>
              </n-gi>
              <n-gi v-if="availableFields && availableFields.length > 0" :span="2">
                <n-form-item label="绑定字段 (用于WHERE过滤)" size="small" :show-feedback="false">
                  <n-select 
                    v-model:value="param.bindField"
                    :options="fieldSelectOptions"
                    placeholder="选择绑定的数据字段"
                    size="small"
                    clearable
                    @update:value="(val: string | null) => handleBindFieldChange(param, val)"
                  />
                </n-form-item>
              </n-gi>
            </n-grid>
          </div>
        </n-collapse-item>
      </n-collapse>
    </div>
    
    <n-empty v-else-if="detectedParams.length === 0" description="暂无查询参数" size="small" style="padding: 20px 0;">
      <template #extra>
        <n-text depth="3" style="font-size: 12px;">
          在SQL中使用 ${参数名} 定义参数
        </n-text>
      </template>
    </n-empty>
    
    <!-- 从字段添加参数 -->
    <div v-if="availableFields.length > 0" class="field-param-section" style="margin-top: 12px;">
      <n-divider style="margin: 8px 0;">
        <n-text depth="3" style="font-size: 12px;">📊 从字段创建筛选参数</n-text>
      </n-divider>
      <n-space vertical :size="8">
        <n-select
          v-model:value="selectedFieldToAdd"
          :options="unusedFieldOptions"
          placeholder="选择字段创建筛选参数..."
          size="small"
          filterable
          clearable
          @update:value="handleFieldSelect"
        />
        <n-text depth="3" style="font-size: 11px;">
          💡 选择字段后将自动创建对应的查询参数，并在SQL中添加 WHERE 条件
        </n-text>
      </n-space>
    </div>
    
    <n-button 
      size="small" 
      dashed 
      block 
      style="margin-top: 12px;" 
      @click="addParam"
    >
      <template #icon><n-icon :component="AddOutline" /></template>
      手动添加参数
    </n-button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { SettingsOutline, ScanOutline, TrashOutline, AddOutline } from '@vicons/ionicons5'

export interface QueryParam {
  name: string
  label: string
  type: 'input' | 'number' | 'date' | 'dateRange' | 'select' | 'dynamicSelect'
  defaultValue: string
  required: boolean
  options?: { label: string; value: string }[]
  optionsText?: string
  optionsSql?: string
  bindField?: string  // 绑定的SQL字段，用于WHERE条件过滤
}

const props = defineProps<{
  sql: string
  modelValue: QueryParam[]
  availableFields?: string[]  // 可选：SQL查询结果的字段列表
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: QueryParam[]): void
  (e: 'addFieldParam', fieldName: string, param: QueryParam): void
}>()

const params = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const detectedParams = ref<string[]>([])

// 可用字段选项
const availableFields = computed(() => props.availableFields || [])
const fieldSelectOptions = computed(() => 
  availableFields.value.map(f => ({ label: f, value: f }))
)

const typeOptions = [
  { label: '文本输入', value: 'input' },
  { label: '数字输入', value: 'number' },
  { label: '日期选择', value: 'date' },
  { label: '日期范围', value: 'dateRange' },
  { label: '下拉选择', value: 'select' },
  { label: '动态下拉', value: 'dynamicSelect' }
]

const dateDefaultOptions = [
  { label: '今天', value: 'TODAY' },
  { label: '昨天', value: 'YESTERDAY' },
  { label: '本周一', value: 'WEEK_START' },
  { label: '本月1日', value: 'MONTH_START' },
  { label: '本季度初', value: 'QUARTER_START' },
  { label: '本年1月1日', value: 'YEAR_START' },
  { label: '过去7天', value: 'LAST_7_DAYS' },
  { label: '过去30天', value: 'LAST_30_DAYS' }
]

const getTypeLabel = (type: string) => {
  return typeOptions.find(t => t.value === type)?.label || type
}

const detectParams = () => {
  const regex = /\$\{(\w+)\}/g
  const matches = props.sql.matchAll(regex)
  const paramNames = new Set<string>()
  for (const match of matches) {
    paramNames.add(match[1])
  }
  detectedParams.value = Array.from(paramNames)
}

// 根据参数名智能推断参数类型
const inferParamType = (paramName: string): QueryParam['type'] => {
  const name = paramName.toLowerCase()
  
  // 日期相关
  if (name.includes('date') || name.includes('time') || name.includes('day') || 
      name === 'start' || name === 'end' || name.includes('创建') || name.includes('更新')) {
    return 'date'
  }
  
  // 日期范围
  if (name.includes('range') || name.includes('period') || name.includes('区间')) {
    return 'dateRange'
  }
  
  // 数字相关
  if (name.includes('id') || name.includes('num') || name.includes('count') || 
      name.includes('amount') || name.includes('price') || name.includes('qty') ||
      name.includes('数量') || name.includes('金额') || name.includes('编号')) {
    return 'number'
  }
  
  // 状态/类型通常是下拉选择
  if (name.includes('status') || name.includes('type') || name.includes('category') ||
      name.includes('状态') || name.includes('类型') || name.includes('分类')) {
    return 'select'
  }
  
  // 年份
  if (name === 'year' || name.includes('年份') || name.includes('年度')) {
    return 'number'
  }
  
  return 'input'
}

// 根据参数名生成显示标签
const inferParamLabel = (paramName: string): string => {
  const labelMap: Record<string, string> = {
    'startDate': '开始日期',
    'endDate': '结束日期',
    'start_date': '开始日期',
    'end_date': '结束日期',
    'createTime': '创建时间',
    'updateTime': '更新时间',
    'year': '年份',
    'month': '月份',
    'day': '日期',
    'status': '状态',
    'type': '类型',
    'category': '分类',
    'name': '名称',
    'keyword': '关键词',
    'id': 'ID',
    'userId': '用户ID',
    'deptId': '部门ID'
  }
  return labelMap[paramName] || paramName
}

// 根据参数名生成默认值
const inferDefaultValue = (paramName: string, paramType: QueryParam['type']): string => {
  const name = paramName.toLowerCase()
  
  if (paramType === 'date') {
    if (name.includes('start') || name.includes('begin')) {
      return 'MONTH_START'
    }
    if (name.includes('end')) {
      return 'TODAY'
    }
    return 'TODAY'
  }
  
  if (name === 'year' || name.includes('年')) {
    return new Date().getFullYear().toString()
  }
  
  return ''
}

const addDetectedParams = () => {
  const newParams: QueryParam[] = detectedParams.value
    .filter(name => !params.value.some(p => p.name === name))
    .map(name => {
      const inferredType = inferParamType(name)
      return {
        name,
        label: inferParamLabel(name),
        type: inferredType,
        defaultValue: inferDefaultValue(name, inferredType),
        required: false
      }
    })
  
  params.value = [...params.value, ...newParams]
}

// 选中的字段
const selectedFieldToAdd = ref<string | null>(null)

// 未使用的字段选项（排除已经创建参数的字段）
const unusedFieldOptions = computed(() => {
  const usedFields = params.value.map(p => p.bindField).filter(Boolean)
  return availableFields.value
    .filter(f => !usedFields.includes(f))
    .map(f => ({ label: f, value: f }))
})

// 从字段创建参数
const handleFieldSelect = (fieldName: string | null) => {
  if (!fieldName) return
  
  // 推断参数类型
  const inferredType = inferParamType(fieldName)
  
  // 创建新参数
  const newParam: QueryParam = {
    name: fieldName,
    label: inferParamLabel(fieldName),
    type: inferredType,
    defaultValue: inferDefaultValue(fieldName, inferredType),
    required: false,
    bindField: fieldName
  }
  
  params.value = [...params.value, newParam]
  
  // 清空选择
  selectedFieldToAdd.value = null
  
  // 发出事件通知父组件需要更新SQL
  emit('addFieldParam', fieldName, newParam)
}

const addParam = () => {
  params.value = [...params.value, {
    name: '',
    label: '',
    type: 'input',
    defaultValue: '',
    required: false
  }]
}

const removeParam = (index: number) => {
  const newParams = [...params.value]
  newParams.splice(index, 1)
  params.value = newParams
}

const handleTypeChange = (param: QueryParam) => {
  if (param.type === 'select') {
    param.optionsText = ''
    param.options = []
  } else if (param.type === 'dynamicSelect') {
    param.optionsSql = ''
  }
}

// 手动添加参数后绑定字段时，通知父组件更新SQL
const handleBindFieldChange = (param: QueryParam, fieldName: string | null) => {
  if (!fieldName) return
  // 更新参数名为字段名（如果参数名为空）
  if (!param.name) {
    param.name = fieldName
    param.label = inferParamLabel(fieldName)
  }
  // 发出事件通知父组件需要更新SQL
  emit('addFieldParam', fieldName, param)
}

const parseOptions = (param: QueryParam) => {
  if (!param.optionsText) {
    param.options = []
    return
  }
  
  param.options = param.optionsText.split('\n')
    .filter(line => line.trim())
    .map(line => {
      const parts = line.split('|')
      return {
        value: parts[0].trim(),
        label: parts[1]?.trim() || parts[0].trim()
      }
    })
}

// 监听SQL变化自动检测并添加参数
watch(() => props.sql, (newSql, oldSql) => {
  detectParams()
  // 如果是新SQL且检测到参数，自动添加
  if (newSql && newSql !== oldSql && detectedParams.value.length > 0 && params.value.length === 0) {
    addDetectedParams()
  }
}, { immediate: true })

defineExpose({
  params,
  detectParams
})
</script>

<style scoped>
.param-config {
  border: 1px solid var(--n-border-color, #e5e7eb);
  border-radius: 8px;
  padding: 12px;
  background: var(--n-color, #fafafa);
}

.param-header {
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--n-border-color, #e5e7eb);
}

.param-list {
  max-height: 400px;
  overflow-y: auto;
}

.param-form {
  padding: 8px 0;
}

:deep(.n-collapse-item__header) {
  padding: 8px 12px !important;
}

:deep(.n-collapse-item__content-inner) {
  padding: 8px 12px !important;
}
</style>

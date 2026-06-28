<template>
  <n-card title="查询条件" size="small" class="condition-builder">
    <template #header-extra>
      <n-tag :bordered="false" type="info" size="small">
        {{ conditions.length }} 个条件
      </n-tag>
    </template>

    <n-space vertical :size="12">
      <!-- 条件列表 -->
      <n-empty 
        v-if="conditions.length === 0" 
        description="暂无查询条件，点击下方按钮添加"
        size="small"
        style="padding: 20px 0"
      />

      <div v-for="(condition, index) in conditions" :key="index" class="condition-item">
        <n-space align="center" :wrap="false">
          <!-- 序号 -->
          <n-tag :bordered="false" size="small" style="min-width: 30px">
            {{ index + 1 }}
          </n-tag>

          <!-- 字段选择 -->
          <n-select
            v-model:value="condition.field"
            :options="fieldOptions"
            placeholder="选择字段"
            style="min-width: 180px"
            filterable
            @update:value="handleFieldChange(index)"
          >
            <template #empty>
              <n-empty description="请先选择数据表" size="small" />
            </template>
          </n-select>

          <!-- 运算符 -->
          <n-select
            v-model:value="condition.operator"
            :options="operatorOptions"
            placeholder="运算符"
            style="min-width: 120px"
            @update:value="handleOperatorChange(index)"
          />

          <!-- 值输入 - 根据运算符类型显示不同的输入框 -->
          <template v-if="needsValueInput(condition.operator)">
            <!-- 日期类型 -->
            <n-date-picker
              v-if="condition.dataType === 'date' || condition.dataType === 'datetime'"
              v-model:value="condition.value"
              type="datetime"
              clearable
              style="min-width: 200px"
            />
            <!-- 数值类型 -->
            <n-input-number
              v-else-if="condition.dataType === 'number'"
              v-model:value="condition.value"
              placeholder="输入数值"
              style="min-width: 150px"
              clearable
            />
            <!-- BETWEEN运算符 -->
            <n-space v-else-if="condition.operator === 'BETWEEN'" align="center">
              <n-input
                v-model:value="condition.value1"
                placeholder="最小值"
                style="width: 100px"
              />
              <n-text>至</n-text>
              <n-input
                v-model:value="condition.value2"
                placeholder="最大值"
                style="width: 100px"
              />
            </n-space>
            <!-- IN运算符 -->
            <n-input
              v-else-if="condition.operator === 'IN'"
              v-model:value="condition.value"
              placeholder="输入多个值，用逗号分隔"
              style="min-width: 200px"
            >
              <template #suffix>
                <n-text depth="3" style="font-size: 12px">如: 1,2,3</n-text>
              </template>
            </n-input>
            <!-- 文本类型 -->
            <n-input
              v-else
              v-model:value="condition.value"
              :placeholder="getValuePlaceholder(condition.operator)"
              style="min-width: 200px"
            />
          </template>

          <!-- 删除按钮 -->
          <n-button
            size="small"
            type="error"
            secondary
            @click="removeCondition(index)"
          >
            <template #icon>
              <n-icon><TrashOutline /></n-icon>
            </template>
          </n-button>
        </n-space>

        <!-- 条件之间的连接符 -->
        <div v-if="index < conditions.length - 1" class="condition-connector">
          <n-tag :bordered="false" :type="combineType === 'AND' ? 'info' : 'warning'" size="small">
            {{ combineType }}
          </n-tag>
        </div>
      </div>

      <!-- 添加条件按钮 -->
      <n-button 
        type="primary" 
        dashed 
        block 
        :disabled="fieldOptions.length === 0"
        @click="addCondition"
      >
        <template #icon>
          <n-icon><AddOutline /></n-icon>
        </template>
        添加查询条件
      </n-button>

      <!-- 条件组合方式 -->
      <n-form-item v-if="conditions.length > 1" label="条件组合方式" label-placement="left">
        <n-radio-group v-model:value="combineType" @update:value="emitUpdate">
          <n-space>
            <n-radio value="AND">
              <n-space align="center" :size="4">
                <n-text>AND</n-text>
                <n-text depth="3" style="font-size: 12px">（所有条件都必须满足）</n-text>
              </n-space>
            </n-radio>
            <n-radio value="OR">
              <n-space align="center" :size="4">
                <n-text>OR</n-text>
                <n-text depth="3" style="font-size: 12px">（任一条件满足即可）</n-text>
              </n-space>
            </n-radio>
          </n-space>
        </n-radio-group>
      </n-form-item>

      <!-- 生成的SQL预览 -->
      <n-alert v-if="conditions.length > 0" type="info" title="生成的SQL WHERE条件" closable>
        <n-code 
          :code="generatedWhereClause" 
          language="sql"
          word-wrap
        />
        <template #footer>
          <n-space justify="end">
            <n-button size="tiny" @click="copyToClipboard">
              <template #icon>
                <n-icon><CopyOutline /></n-icon>
              </template>
              复制SQL
            </n-button>
            <n-button size="tiny" type="error" @click="clearConditions">
              <template #icon>
                <n-icon><TrashOutline /></n-icon>
              </template>
              清空所有条件
            </n-button>
          </n-space>
        </template>
      </n-alert>

      <!-- 快速模板 -->
      <n-collapse v-if="templates.length > 0" arrow-placement="right">
        <n-collapse-item title="快速模板" name="templates">
          <n-space vertical :size="8">
            <n-button
              v-for="(template, index) in templates"
              :key="index"
              size="small"
              block
              text
              @click="applyTemplate(template)"
            >
              <template #icon>
                <n-icon><DocumentTextOutline /></n-icon>
              </template>
              {{ template.name }}
            </n-button>
          </n-space>
        </n-collapse-item>
      </n-collapse>
    </n-space>
  </n-card>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { 
  AddOutline, 
  TrashOutline, 
  CopyOutline,
  DocumentTextOutline 
} from '@vicons/ionicons5'

interface Condition {
  field: string
  operator: string
  value: any
  value1?: any  // For BETWEEN
  value2?: any  // For BETWEEN
  dataType: string
}

interface FieldOption {
  label: string
  value: string
  dataType: string
}

interface Template {
  name: string
  conditions: Partial<Condition>[]
}

interface Props {
  columns?: FieldOption[]
}

const props = withDefaults(defineProps<Props>(), {
  columns: () => []
})

const emit = defineEmits<{
  (e: 'update:whereClause', value: string): void
  (e: 'change', conditions: Condition[]): void
}>()

const message = useMessage()

const conditions = ref<Condition[]>([])
const combineType = ref<'AND' | 'OR'>('AND')

// 字段选项
const fieldOptions = computed(() => {
  return props.columns.map(col => ({
    label: col.label,
    value: col.value,
    dataType: col.dataType
  }))
})

// 运算符选项
const operatorOptions = [
  { label: '等于 (=)', value: '=' },
  { label: '不等于 (!=)', value: '!=' },
  { label: '大于 (>)', value: '>' },
  { label: '小于 (<)', value: '<' },
  { label: '大于等于 (>=)', value: '>=' },
  { label: '小于等于 (<=)', value: '<=' },
  { label: '包含 (LIKE)', value: 'LIKE' },
  { label: '不包含 (NOT LIKE)', value: 'NOT LIKE' },
  { label: '在...之中 (IN)', value: 'IN' },
  { label: '不在...之中 (NOT IN)', value: 'NOT IN' },
  { label: '在...之间 (BETWEEN)', value: 'BETWEEN' },
  { label: '为空 (IS NULL)', value: 'IS NULL' },
  { label: '不为空 (IS NOT NULL)', value: 'IS NOT NULL' }
]

// 快速模板
const templates = ref<Template[]>([
  {
    name: '最近7天',
    conditions: [
      { field: 'create_time', operator: '>=', value: 'DATE_SUB(NOW(), INTERVAL 7 DAY)' }
    ]
  },
  {
    name: '最近30天',
    conditions: [
      { field: 'create_time', operator: '>=', value: 'DATE_SUB(NOW(), INTERVAL 30 DAY)' }
    ]
  },
  {
    name: '今天',
    conditions: [
      { field: 'create_time', operator: '>=', value: 'CURDATE()' }
    ]
  },
  {
    name: '状态启用',
    conditions: [
      { field: 'status', operator: '=', value: '1' }
    ]
  }
])

// 添加条件
const addCondition = () => {
  conditions.value.push({
    field: '',
    operator: '=',
    value: '',
    dataType: 'string'
  })
}

// 删除条件
const removeCondition = (index: number) => {
  conditions.value.splice(index, 1)
  emitUpdate()
}

// 清空所有条件
const clearConditions = () => {
  conditions.value = []
  emitUpdate()
  message.success('已清空所有条件')
}

// 字段变化时更新数据类型
const handleFieldChange = (index: number) => {
  const condition = conditions.value[index]
  const field = fieldOptions.value.find(f => f.value === condition.field)
  if (field) {
    condition.dataType = field.dataType
  }
  emitUpdate()
}

// 运算符变化
const handleOperatorChange = (index: number) => {
  const condition = conditions.value[index]
  // 如果切换到 IS NULL 或 IS NOT NULL，清空值
  if (condition.operator === 'IS NULL' || condition.operator === 'IS NOT NULL') {
    condition.value = ''
  }
  emitUpdate()
}

// 判断是否需要值输入
const needsValueInput = (operator: string) => {
  return operator !== 'IS NULL' && operator !== 'IS NOT NULL'
}

// 获取值输入的占位符
const getValuePlaceholder = (operator: string) => {
  if (operator === 'LIKE' || operator === 'NOT LIKE') {
    return '输入关键词'
  }
  return '输入值'
}

// 格式化值
const formatValue = (value: any, dataType: string): string => {
  if (value === null || value === undefined || value === '') {
    return 'NULL'
  }

  // 如果是SQL函数（包含括号），直接返回
  if (typeof value === 'string' && /\(.*\)/.test(value)) {
    return value
  }

  // 数值类型不加引号
  if (dataType === 'number' || dataType?.toLowerCase().includes('int')) {
    return String(value)
  }

  // 日期类型
  if (dataType === 'date' || dataType === 'datetime') {
    if (typeof value === 'number') {
      const date = new Date(value)
      return `'${date.toISOString().slice(0, 19).replace('T', ' ')}'`
    }
    return `'${value}'`
  }

  // 字符串类型加引号，并转义单引号
  return `'${String(value).replace(/'/g, "''")}'`
}

// 生成WHERE子句
const generatedWhereClause = computed(() => {
  if (conditions.value.length === 0) {
    return '-- 无条件，将查询所有数据'
  }

  const clauses = conditions.value
    .filter(c => c.field) // 过滤未选择字段的条件
    .map(c => {
      const field = `\`${c.field}\``
      
      // IS NULL / IS NOT NULL
      if (c.operator === 'IS NULL' || c.operator === 'IS NOT NULL') {
        return `${field} ${c.operator}`
      }
      
      // LIKE / NOT LIKE
      if (c.operator === 'LIKE' || c.operator === 'NOT LIKE') {
        const value = formatValue(c.value, c.dataType)
        return `${field} ${c.operator} ${value.replace(/^'|'$/g, '')}${c.value ? "'%" : ''}`
      }
      
      // IN / NOT IN
      if (c.operator === 'IN' || c.operator === 'NOT IN') {
        const values = String(c.value || '').split(',').map(v => v.trim()).filter(Boolean)
        if (values.length === 0) return ''
        const formattedValues = values.map(v => formatValue(v, c.dataType)).join(', ')
        return `${field} ${c.operator} (${formattedValues})`
      }
      
      // BETWEEN
      if (c.operator === 'BETWEEN') {
        if (!c.value1 || !c.value2) return ''
        const val1 = formatValue(c.value1, c.dataType)
        const val2 = formatValue(c.value2, c.dataType)
        return `${field} BETWEEN ${val1} AND ${val2}`
      }
      
      // 标准运算符
      const value = formatValue(c.value, c.dataType)
      return `${field} ${c.operator} ${value}`
    })
    .filter(Boolean) // 过滤空条件

  if (clauses.length === 0) {
    return '-- 请完善条件配置'
  }

  return `WHERE ${clauses.join(` ${combineType.value} `)}`
})

// 复制到剪贴板
const copyToClipboard = async () => {
  try {
    await navigator.clipboard.writeText(generatedWhereClause.value)
    message.success('SQL已复制到剪贴板')
  } catch (error) {
    message.error('复制失败，请手动复制')
  }
}

// 应用模板
const applyTemplate = (template: Template) => {
  template.conditions.forEach(templateCondition => {
    const field = fieldOptions.value.find(f => f.value === templateCondition.field)
    conditions.value.push({
      field: templateCondition.field || '',
      operator: templateCondition.operator || '=',
      value: templateCondition.value || '',
      dataType: field?.dataType || 'string'
    })
  })
  emitUpdate()
  message.success(`已应用模板：${template.name}`)
}

// 发送更新事件
const emitUpdate = () => {
  emit('update:whereClause', generatedWhereClause.value)
  emit('change', conditions.value)
}

// 监听条件变化
watch(conditions, () => {
  emitUpdate()
}, { deep: true })

watch(combineType, () => {
  emitUpdate()
})

// 暴露方法
defineExpose({
  getWhereClause: () => generatedWhereClause.value,
  getConditions: () => conditions.value,
  clearConditions
})
</script>

<style scoped>
.condition-builder {
  max-width: 100%;
}

.condition-item {
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.condition-item:hover {
  background: #e9ecef;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.condition-connector {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 8px 0;
}

:deep(.n-code) {
  background: #2d2d2d !important;
  color: #f8f8f2 !important;
  padding: 12px !important;
  border-radius: 6px !important;
  font-size: 13px !important;
  line-height: 1.6 !important;
}

:deep(.n-alert) {
  border-radius: 8px;
}

:deep(.n-collapse-item__header) {
  font-weight: 500;
}
</style>

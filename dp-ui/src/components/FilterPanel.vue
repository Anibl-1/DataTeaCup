<template>
  <div class="filter-panel">
    <n-button quaternary @click="showFilterModal = true">
      <template #icon>
        <n-icon><FilterOutline /></n-icon>
      </template>
      筛选
      <n-badge v-if="filterCount > 0" :value="filterCount" />
    </n-button>
    <!-- 活跃筛选标签 -->
    <div v-if="activeFilterTags.length > 0" class="active-filter-tags">
      <n-tag
        v-for="(tag, idx) in activeFilterTags"
        :key="idx"
        size="small"
        closable
        round
        :bordered="false"
        type="info"
        @close="removeFilterByIndex(tag.index)"
      >
        {{ tag.label }}
      </n-tag>
      <n-button v-if="activeFilterTags.length > 1" text size="tiny" type="error" style="margin-left:4px;" @click="handleReset">清除全部</n-button>
    </div>
    
    <n-modal v-model:show="showFilterModal" preset="card" title="筛选条件" style="width: 600px; border-radius: 16px;">
      <n-form :model="filterForm" label-placement="left" label-width="100px">
        <n-form-item
          v-for="(filter, index) in filters"
          :key="index"
          :label="`条件 ${index + 1}`"
        >
          <n-space style="width: 100%">
            <n-select
              v-model:value="filter.field"
              :options="fieldOptions"
              placeholder="选择字段"
              style="width: 150px"
              clearable
              @update:value="handleFieldChange(index)"
            />
            <n-select
              v-model:value="filter.operator"
              :options="getOperatorOptionsForField(filter.field)"
              placeholder="操作符"
              style="width: 120px"
              :disabled="!filter.field"
            />
            <!-- 'in' operator: multi-select when field has options -->
            <n-select
              v-if="filter.operator === 'in' && getFieldOptions(filter.field)"
              v-model:value="filter.multiValue"
              :options="getFieldOptions(filter.field) || []"
              placeholder="选择值（多选）"
              style="flex: 1; min-width: 150px"
              :disabled="!filter.field"
              clearable
              filterable
              multiple
            />
            <!-- 'in' operator: text input for comma-separated values when no options -->
            <n-input
              v-else-if="filter.operator === 'in'"
              v-model:value="filter.value"
              placeholder="输入值（逗号分隔）"
              style="flex: 1"
              :disabled="!filter.field"
              clearable
            />
            <!-- Date range picker for between operator -->
            <n-date-picker
              v-else-if="filter.operator === 'between' && getFieldType(filter.field) === 'date'"
              v-model:value="filter.dateRange"
              type="daterange"
              style="flex: 1; min-width: 240px"
              :disabled="!filter.field"
              clearable
            />
            <!-- Number range for between operator -->
            <div v-else-if="filter.operator === 'between' && getFieldType(filter.field) === 'number'" style="display:flex;gap:6px;align-items:center;flex:1;">
              <n-input-number v-model:value="filter.rangeMin" placeholder="最小值" style="flex:1;" :disabled="!filter.field" clearable />
              <span style="color:#999;">~</span>
              <n-input-number v-model:value="filter.rangeMax" placeholder="最大值" style="flex:1;" :disabled="!filter.field" clearable />
            </div>
            <!-- Date picker for date fields -->
            <n-date-picker
              v-else-if="getFieldType(filter.field) === 'date' && !isNullOperator(filter.operator)"
              v-model:value="filter.dateValue"
              type="date"
              style="flex: 1; min-width: 150px"
              :disabled="!filter.field"
              clearable
            />
            <!-- Select for fields with predefined options -->
            <n-select
              v-else-if="getFieldOptions(filter.field) && !isNullOperator(filter.operator)"
              v-model:value="filter.value"
              :options="getFieldOptions(filter.field) || []"
              placeholder="选择值"
              style="flex: 1; min-width: 120px"
              :disabled="!filter.field"
              clearable
              filterable
            />
            <!-- Default text/number input -->
            <n-input
              v-else
              v-model:value="filter.value"
              :placeholder="isNullOperator(filter.operator) ? '无需输入' : '输入值'"
              style="flex: 1"
              :disabled="!filter.field || isNullOperator(filter.operator)"
              clearable
            />
            <n-button
              v-if="filters.length > 1"
              quaternary
              type="error"
              @click="removeFilter(index)"
            >
              <template #icon>
                <n-icon><CloseOutline /></n-icon>
              </template>
            </n-button>
          </n-space>
        </n-form-item>
        <n-form-item>
          <n-button dashed type="primary" @click="addFilter">
            <template #icon>
              <n-icon><AddOutline /></n-icon>
            </template>
            添加条件
          </n-button>
        </n-form-item>
      </n-form>
      
      <template #footer>
        <n-space justify="end">
          <n-button @click="handleReset">重置</n-button>
          <n-button @click="showFilterModal = false">取消</n-button>
          <n-button type="primary" @click="handleApply">应用</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, watch } from 'vue'
import { NButton, NModal, NForm, NFormItem, NSelect, NInput, NSpace, NIcon, NBadge, NDatePicker, NInputNumber, NTag } from 'naive-ui'
import { FilterOutline, CloseOutline, AddOutline } from '@vicons/ionicons5'
import type { FilterCondition } from '@/types/api'

export interface FilterField {
  label: string
  value: string
  type?: 'string' | 'number' | 'date' | 'boolean'
  /** 可选下拉选项（如字典列），提供后筛选值输入将渲染为下拉选择 */
  options?: Array<{ label: string; value: string }>
}

// 内部使用的筛选条件格式（value 可为 null）
interface InternalFilterCondition {
  field: string | null
  operator: string | null
  value: string | null
  dateValue: number | null
  multiValue: string[] | null
  dateRange: [number, number] | null
  rangeMin: number | null
  rangeMax: number | null
}

interface Props {
  fields: FilterField[]
  modelValue?: FilterCondition[]
}

interface Emits {
  (e: 'update:modelValue', value: FilterCondition[]): void
  (e: 'apply', filters: FilterCondition[]): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => []
})

const emit = defineEmits<Emits>()

const showFilterModal = ref(false)
const filterForm = ref({})

const createEmptyFilter = (): InternalFilterCondition => ({
  field: null, operator: null, value: null, dateValue: null, multiValue: null, dateRange: null, rangeMin: null, rangeMax: null
})

const filters = ref<InternalFilterCondition[]>([createEmptyFilter()])

const fieldOptions = computed(() => {
  return props.fields.map(field => ({
    label: field.label,
    value: field.value
  }))
})

// 获取字段的下拉选项（字典列）
const getFieldOptions = (fieldValue: string | null): Array<{ label: string; value: string }> | null => {
  if (!fieldValue) return null
  const field = props.fields.find(f => f.value === fieldValue)
  return field?.options && field.options.length > 0 ? field.options : null
}

// 获取字段类型
const getFieldType = (fieldValue: string | null): string => {
  if (!fieldValue) return 'string'
  const field = props.fields.find(f => f.value === fieldValue)
  return field?.type || 'string'
}

// 检查是否为空值操作符
const isNullOperator = (operator: string | null): boolean => {
  return operator === 'isNull' || operator === 'isNotNull'
}

// 根据字段类型返回对应的操作符选项（每行独立）
const getOperatorOptionsForField = (fieldValue: string | null) => {
  const type = getFieldType(fieldValue)
  const hasOptions = !!getFieldOptions(fieldValue)

  const commonOperators = [
    { label: '等于', value: 'eq' },
    { label: '不等于', value: 'ne' },
    { label: '为空', value: 'isNull' },
    { label: '不为空', value: 'isNotNull' }
  ]

  if (type === 'number') {
    return [
      { label: '等于', value: 'eq' },
      { label: '不等于', value: 'ne' },
      { label: '大于', value: 'gt' },
      { label: '大于等于', value: 'gte' },
      { label: '小于', value: 'lt' },
      { label: '小于等于', value: 'lte' },
      { label: '区间', value: 'between' },
      { label: '包含于', value: 'in' },
      { label: '为空', value: 'isNull' },
      { label: '不为空', value: 'isNotNull' }
    ]
  }

  if (type === 'date') {
    return [
      { label: '等于', value: 'eq' },
      { label: '不等于', value: 'ne' },
      { label: '大于', value: 'gt' },
      { label: '大于等于', value: 'gte' },
      { label: '小于', value: 'lt' },
      { label: '小于等于', value: 'lte' },
      { label: '区间', value: 'between' },
      { label: '为空', value: 'isNull' },
      { label: '不为空', value: 'isNotNull' }
    ]
  }

  // string type (default) — includes like and in
  const stringOperators = [
    ...commonOperators.slice(0, 2), // eq, ne
    { label: '包含', value: 'like' },
    { label: '不包含', value: 'notContains' },
    { label: '开始于', value: 'startsWith' },
    { label: '结束于', value: 'endsWith' },
    { label: '包含于', value: 'in' },
    ...commonOperators.slice(2) // isNull, isNotNull
  ]

  // If field has predefined options, also add 'contains' alias
  if (hasOptions) {
    return [
      { label: '等于', value: 'eq' },
      { label: '不等于', value: 'ne' },
      { label: '包含于', value: 'in' },
      { label: '为空', value: 'isNull' },
      { label: '不为空', value: 'isNotNull' }
    ]
  }

  return stringOperators
}

// 获取操作符标签（可供外部使用）
const getOperatorLabel = (operator: string | null): string => {
  const operatorMap: Record<string, string> = {
    'eq': '=',
    'ne': '≠',
    'like': '包含',
    'contains': '包含',
    'notContains': '不包含',
    'startsWith': '开始于',
    'endsWith': '结束于',
    'gt': '>',
    'gte': '≥',
    'lt': '<',
    'lte': '≤',
    'in': '包含于',
    'isNull': '为空',
    'isNotNull': '不为空'
  }
  return operatorMap[operator || ''] || operator || ''
}

const filterCount = computed(() => {
  return filters.value.filter(f => f.field && f.operator).length
})

// 活跃筛选标签（显示在按钮旁边）
const activeFilterTags = computed(() => {
  return filters.value
    .map((f, index) => {
      if (!f.field || !f.operator) return null
      const fieldObj = props.fields.find(fd => fd.value === f.field)
      const fieldLabel = fieldObj?.label || f.field
      const opLabel = getOperatorLabel(f.operator)
      let valStr = ''
      if (isNullOperator(f.operator)) {
        valStr = ''
      } else if (f.operator === 'between') {
        if (f.dateRange) {
          valStr = `${new Date(f.dateRange[0]).toLocaleDateString()} ~ ${new Date(f.dateRange[1]).toLocaleDateString()}`
        } else if (f.rangeMin !== null || f.rangeMax !== null) {
          valStr = `${f.rangeMin ?? ''} ~ ${f.rangeMax ?? ''}`
        }
      } else if (f.operator === 'in' && f.multiValue?.length) {
        valStr = f.multiValue.join(', ')
      } else if (f.dateValue) {
        valStr = new Date(f.dateValue).toLocaleDateString()
      } else if (f.value) {
        // 如果字段有预定义选项，显示选项label
        const opts = getFieldOptions(f.field)
        if (opts) {
          const opt = opts.find(o => o.value === f.value)
          valStr = opt?.label || f.value
        } else {
          valStr = f.value
        }
      }
      return { index, label: `${fieldLabel} ${opLabel}${valStr ? ' ' + valStr : ''}` }
    })
    .filter(Boolean) as { index: number; label: string }[]
})

const removeFilterByIndex = (index: number) => {
  filters.value.splice(index, 1)
  if (filters.value.length === 0) filters.value.push(createEmptyFilter())
  // Auto-apply after removing a tag
  handleApply()
}

// 当字段变更时，重置该行的操作符和值
const handleFieldChange = (index: number) => {
  if (index < 0 || index >= filters.value.length) return
  const filter = filters.value[index]
  if (!filter) return
  filter.operator = null
  filter.value = null
  filter.dateValue = null
  filter.multiValue = null
  filter.dateRange = null
  filter.rangeMin = null
  filter.rangeMax = null
}

const addFilter = () => {
  filters.value.push(createEmptyFilter())
}

const removeFilter = (index: number) => {
  filters.value.splice(index, 1)
  if (filters.value.length === 0) {
    filters.value.push(createEmptyFilter())
  }
}

const handleReset = () => {
  filters.value = [createEmptyFilter()]
  emit('update:modelValue', [])
  emit('apply', [])
  showFilterModal.value = false
}

const handleApply = () => {
  const activeFilters: FilterCondition[] = filters.value
    .filter(f => f.field && f.operator)
    .map(f => {
      let value: any = undefined

      if (isNullOperator(f.operator)) {
        // isNull/isNotNull don't need a value
        value = undefined
      } else if (f.operator === 'in') {
        // 'in' operator: use multiValue array or parse comma-separated string
        if (f.multiValue && f.multiValue.length > 0) {
          value = f.multiValue
        } else if (f.value) {
          value = f.value.split(',').map(v => v.trim()).filter(Boolean)
        }
      } else if (f.operator === 'between') {
        if (getFieldType(f.field) === 'date' && f.dateRange) {
          value = [new Date(f.dateRange[0]).toISOString().split('T')[0], new Date(f.dateRange[1]).toISOString().split('T')[0]]
        } else if (f.rangeMin !== null && f.rangeMax !== null) {
          value = [f.rangeMin, f.rangeMax]
        }
      } else if (f.dateValue) {
        value = new Date(f.dateValue).toISOString().split('T')[0]
      } else if (f.value !== null && f.value !== '') {
        // Try to convert to number for number fields
        const fieldType = getFieldType(f.field)
        if (fieldType === 'number') {
          value = isNaN(Number(f.value)) ? f.value : Number(f.value)
        } else {
          value = f.value
        }
      }

      return {
        field: f.field!,
        operator: f.operator as FilterCondition['operator'],
        value
      }
    })
    .filter(f => isNullOperator(f.operator) || f.value !== undefined)

  emit('update:modelValue', activeFilters)
  emit('apply', activeFilters)
  showFilterModal.value = false
}

// 监听外部值变化，同步到内部状态
watch(() => props.modelValue, (newVal) => {
  if (newVal && newVal.length > 0) {
    filters.value = newVal.map(f => {
      const internal: InternalFilterCondition = {
        field: f.field,
        operator: f.operator,
        value: null,
        dateValue: null,
        multiValue: null,
        dateRange: null,
        rangeMin: null,
        rangeMax: null
      }

      if (f.operator === 'in' && Array.isArray(f.value)) {
        internal.multiValue = f.value.map(String)
      } else if (f.value !== undefined && f.value !== null) {
        internal.value = String(f.value)
      }

      return internal
    })
  } else {
    filters.value = [createEmptyFilter()]
  }
}, { immediate: true })
</script>

<style scoped>
.filter-panel {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.active-filter-tags {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

/* 添加条件按钮 */
:deep(.n-button--dashed) {
  border: 2px dashed var(--color-primary, #2563EB) !important;
  color: var(--color-primary, #2563EB) !important;
  background: transparent !important;
}

:deep(.n-button--dashed:hover) {
  background: var(--color-primary-light, #DBEAFE) !important;
  border-color: var(--color-primary, #2563EB) !important;
}

/* 组件美化 */
:deep(.n-base-selection) {
  border-radius: var(--radius-md, 6px);
}

:deep(.n-base-selection:hover) {
  border-color: var(--color-primary, #2563EB);
}

:deep(.n-input) {
  border-radius: var(--radius-md, 6px);
}

:deep(.n-input:hover) {
  border-color: var(--color-primary, #2563EB);
}

:deep(.n-input:focus-within) {
  box-shadow: 0 0 0 2px var(--color-primary-light, #DBEAFE);
}

/* 响应式设计 */
@media (max-width: 768px) {
  :deep(.n-space) {
    flex-direction: column;
  }

  :deep(.n-space > *) {
    width: 100% !important;
    min-width: auto !important;
  }
}
</style>

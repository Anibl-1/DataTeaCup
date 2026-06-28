<template>
  <WidgetWrapper
    :title="config?.label || '筛选器'"
    :selected="selected"
    :loading="loading"
    :error="error"
    :readonly="readonly"
    :show-header="showHeader"
    :show-refresh="config?.filterType === 'select'"
    @click="$emit('select')"
    @refresh="loadOptions"
    @remove="$emit('remove')"
  >
    <div class="filter-container">
      <div v-if="showLabel" class="filter-label">{{ config?.label }}</div>
      
      <!-- Select Filter -->
      <n-select
        v-if="config?.filterType === 'select'"
        v-model:value="filterValue"
        :options="selectOptions"
        :placeholder="placeholder"
        :loading="loading"
        :multiple="false"
        clearable
        filterable
        @update:value="handleValueChange"
      />
      
      <!-- Date Filter -->
      <n-date-picker
        v-else-if="config?.filterType === 'date'"
        v-model:value="dateValue"
        type="date"
        :placeholder="placeholder"
        clearable
        @update:value="handleDateChange"
      />
      
      <!-- Date Range Filter -->
      <n-date-picker
        v-else-if="config?.filterType === 'dateRange'"
        v-model:value="dateRangeValue"
        type="daterange"
        :start-placeholder="startPlaceholder"
        :end-placeholder="endPlaceholder"
        clearable
        @update:value="handleDateRangeChange"
      />
      
      <!-- Input Filter -->
      <n-input
        v-else
        v-model:value="inputValue"
        :placeholder="placeholder"
        clearable
        @update:value="handleInputChange"
        @keyup.enter="handleInputConfirm"
      >
        <template #suffix>
          <n-button text size="tiny" @click="handleInputConfirm">
            <template #icon>
              <n-icon><SearchOutline /></n-icon>
            </template>
          </n-button>
        </template>
      </n-input>
    </div>
  </WidgetWrapper>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, watch, onMounted } from 'vue'
import { NSelect, NDatePicker, NInput, NButton, NIcon } from 'naive-ui'
import { SearchOutline } from '@vicons/ionicons5'
import WidgetWrapper from './WidgetWrapper.vue'
import type { FilterWidgetConfig } from '@/types/dashboard'
import { executeSql } from '@/api/tableData'

const props = withDefaults(defineProps<{
  config: FilterWidgetConfig
  selected?: boolean
  readonly?: boolean
  showHeader?: boolean
  showLabel?: boolean
  modelValue?: any
}>(), {
  selected: false,
  readonly: false,
  showHeader: true,
  showLabel: false,
  modelValue: undefined
})

const emit = defineEmits<{
  (e: 'select'): void
  (e: 'remove'): void
  (e: 'update:modelValue', value: any): void
  (e: 'change', payload: { field: string; value: any; linkedCharts: string[] }): void
}>()

const loading = ref(false)
const error = ref<string | null>(null)

// Filter values for different types
const filterValue = ref<any>(null)
const dateValue = ref<number | null>(null)
const dateRangeValue = ref<[number, number] | null>(null)
const inputValue = ref<string>('')

// Select options
const selectOptions = ref<Array<{ label: string; value: any }>>([])

// Computed properties
const placeholder = computed(() => {
  return props.config?.label || '请选择'
})

const startPlaceholder = computed(() => '开始日期')
const endPlaceholder = computed(() => '结束日期')

// Initialize value from modelValue or defaultValue
const initializeValue = () => {
  const defaultVal = props.modelValue ?? props.config?.defaultValue
  
  switch (props.config?.filterType) {
    case 'select':
      filterValue.value = defaultVal
      break
    case 'date':
      dateValue.value = defaultVal ? new Date(defaultVal).getTime() : null
      break
    case 'dateRange':
      if (Array.isArray(defaultVal) && defaultVal.length === 2) {
        dateRangeValue.value = [
          new Date(defaultVal[0]).getTime(),
          new Date(defaultVal[1]).getTime()
        ]
      }
      break
    case 'input':
    default:
      inputValue.value = defaultVal || ''
      break
  }
}

// Load select options from data source
const loadOptions = async () => {
  if (props.config?.filterType !== 'select') return
  if (!props.config?.dataSourceId || !props.config?.optionsSql) {
    // Use empty options
    selectOptions.value = []
    return
  }
  
  loading.value = true
  error.value = null
  
  try {
    const res = await executeSql({
      dataSourceId: props.config.dataSourceId,
      sql: props.config.optionsSql
    })
    
    const data = res.data?.data || res.data
    if (Array.isArray(data)) {
      selectOptions.value = data.map((row: any) => {
        // Try to find label and value fields
        const keys = Object.keys(row)
        const labelKey = keys.find(k => k.toLowerCase().includes('label') || k.toLowerCase().includes('name')) || keys[0]
        const valueKey = keys.find(k => k.toLowerCase().includes('value') || k.toLowerCase().includes('id')) || keys[0]
        
        return {
          label: String(row[labelKey] ?? ''),
          value: row[valueKey]
        }
      })
    }
  } catch (err: any) {
    error.value = err.message || '加载选项失败'
    console.error('Failed to load filter options:', err)
  } finally {
    loading.value = false
  }
}

// Emit change event
const emitChange = (value: any) => {
  emit('update:modelValue', value)
  emit('change', {
    field: props.config?.field || '',
    value,
    linkedCharts: props.config?.linkedCharts || []
  })
}

// Handle value changes
const handleValueChange = (value: any) => {
  emitChange(value)
}

const handleDateChange = (value: number | null) => {
  const formattedValue = value ? new Date(value).toISOString().split('T')[0] : null
  emitChange(formattedValue)
}

const handleDateRangeChange = (value: [number, number] | null) => {
  if (value && value.length === 2) {
    const formattedValue = [
      new Date(value[0]).toISOString().split('T')[0],
      new Date(value[1]).toISOString().split('T')[0]
    ]
    emitChange(formattedValue)
  } else {
    emitChange(null)
  }
}

const handleInputChange = (value: string) => {
  // Don't emit on every keystroke, wait for confirm
}

const handleInputConfirm = () => {
  emitChange(inputValue.value || null)
}

// Watch config changes
watch(() => props.config, () => {
  initializeValue()
  if (props.config?.filterType === 'select') {
    loadOptions()
  }
}, { deep: true })

// Watch modelValue changes
watch(() => props.modelValue, () => {
  initializeValue()
})

// Lifecycle
onMounted(() => {
  initializeValue()
  if (props.config?.filterType === 'select') {
    loadOptions()
  }
})

// Expose methods
defineExpose({
  refresh: loadOptions,
  getValue: () => {
    switch (props.config?.filterType) {
      case 'select':
        return filterValue.value
      case 'date':
        return dateValue.value ? new Date(dateValue.value).toISOString().split('T')[0] : null
      case 'dateRange':
        return dateRangeValue.value
      case 'input':
      default:
        return inputValue.value
    }
  },
  setValue: (value: any) => {
    switch (props.config?.filterType) {
      case 'select':
        filterValue.value = value
        break
      case 'date':
        dateValue.value = value ? new Date(value).getTime() : null
        break
      case 'dateRange':
        if (Array.isArray(value) && value.length === 2) {
          dateRangeValue.value = [
            new Date(value[0]).getTime(),
            new Date(value[1]).getTime()
          ]
        } else {
          dateRangeValue.value = null
        }
        break
      case 'input':
      default:
        inputValue.value = value || ''
        break
    }
  }
})
</script>

<style scoped>
.filter-container {
  padding: 8px 12px;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.filter-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

/* Ensure components take full width */
.filter-container :deep(.n-select),
.filter-container :deep(.n-date-picker),
.filter-container :deep(.n-input) {
  width: 100%;
}
</style>

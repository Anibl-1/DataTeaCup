<template>
  <div class="import-mode-selector">
    <n-form-item label="导入模式">
      <n-radio-group v-model:value="mode" @update:value="handleModeChange">
        <n-space>
          <n-radio value="full">全量导入</n-radio>
          <n-radio value="incremental">增量导入</n-radio>
          <n-radio value="conditional">按字段条件导入</n-radio>
        </n-space>
      </n-radio-group>
    </n-form-item>

    <!-- 增量导入配置 -->
    <template v-if="mode === 'incremental'">
      <n-form-item :label="isDatabase ? '增量字段' : '去重字段'">
        <n-select
          v-model:value="incrementField"
          :options="fieldOptions"
          :placeholder="isDatabase ? '选择增量字段（如 update_time、id）' : '选择去重字段（如 id、create_time）'"
          filterable
          @update:value="emitChange"
        />
      </n-form-item>
      <n-form-item v-if="isDatabase" label="增量起始值">
        <n-input-group>
          <n-input
            v-model:value="incrementStartValue"
            placeholder="输入增量起始值，或点击右侧自动获取"
            @update:value="emitChange"
          />
          <n-button :loading="loadingMaxValue" @click="handleGetMaxValue">
            自动获取
          </n-button>
        </n-input-group>
      </n-form-item>
      <n-alert type="info" :bordered="false" style="margin-bottom: 12px;">
        <template v-if="isDatabase">
          仅导入源表中「{{ incrementField || '增量字段' }}」大于起始值的记录
        </template>
        <template v-else>
          追加写入目标表，文件中与目标表「{{ incrementField || '去重字段' }}」重复的行自动跳过
        </template>
      </n-alert>
    </template>

    <!-- 按字段条件导入配置 -->
    <template v-if="mode === 'conditional'">
      <div class="condition-builder">
        <div v-for="(cond, index) in filterConditions" :key="index" class="condition-row">
          <n-select
            v-model:value="cond.field"
            :options="fieldOptions"
            placeholder="选择字段"
            filterable
            style="width: 180px"
            @update:value="emitChange"
          />
          <n-select
            v-model:value="cond.operator"
            :options="operatorOptions"
            placeholder="操作符"
            style="width: 130px"
            @update:value="emitChange"
          />
          <n-input
            v-model:value="cond.value"
            placeholder="值"
            style="flex: 1"
            @update:value="emitChange"
          />
          <n-button
            text
            type="error"
            :disabled="filterConditions.length <= 1"
            @click="removeCondition(index)"
          >
            <n-icon size="18"><CloseCircleOutline /></n-icon>
          </n-button>
        </div>
        <n-button dashed size="small" style="margin-top: 8px;" @click="addCondition">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          添加条件
        </n-button>
      </div>
      <n-alert type="info" :bordered="false" style="margin-top: 12px;">
        仅导入满足以上所有条件的数据行
      </n-alert>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, toRefs } from 'vue'
import { NIcon } from 'naive-ui'
import { CloseCircleOutline, AddOutline } from '@vicons/ionicons5'

export interface FilterCondition {
  field: string | null
  operator: string
  value: string
}

export interface ImportModeConfig {
  importMode: 'full' | 'incremental' | 'conditional'
  deduplicateField?: string
  incrementField?: string
  incrementStartValue?: string
  filterConditions?: FilterCondition[]
}

const props = withDefaults(defineProps<{
  fields: Array<{ label: string; value: string }>
  isDatabase?: boolean
  modelValue?: ImportModeConfig
}>(), {
  isDatabase: false,
  modelValue: () => ({ importMode: 'full' })
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: ImportModeConfig): void
  (e: 'getMaxValue', field: string): void
}>()

const mode = ref<'full' | 'incremental' | 'conditional'>(props.modelValue?.importMode || 'full')
const incrementField = ref<string | null>(props.modelValue?.incrementField || props.modelValue?.deduplicateField || null)
const incrementStartValue = ref(props.modelValue?.incrementStartValue || '')
const loadingMaxValue = ref(false)
const filterConditions = ref<FilterCondition[]>(
  props.modelValue?.filterConditions?.length
    ? props.modelValue.filterConditions
    : [{ field: null, operator: '=', value: '' }]
)

const fieldOptions = ref(props.fields || [])

watch(() => props.fields, (newFields) => {
  fieldOptions.value = newFields || []
})

const operatorOptions = [
  { label: '等于 (=)', value: '=' },
  { label: '不等于 (!=)', value: '!=' },
  { label: '大于 (>)', value: '>' },
  { label: '小于 (<)', value: '<' },
  { label: '大于等于 (>=)', value: '>=' },
  { label: '小于等于 (<=)', value: '<=' },
  { label: '包含 (LIKE)', value: 'LIKE' },
  { label: '在列表中 (IN)', value: 'IN' },
  { label: '范围 (BETWEEN)', value: 'BETWEEN' },
  { label: '为空 (IS NULL)', value: 'IS NULL' },
  { label: '不为空 (IS NOT NULL)', value: 'IS NOT NULL' }
]

const handleModeChange = (val: 'full' | 'incremental' | 'conditional') => {
  mode.value = val
  emitChange()
}

const addCondition = () => {
  filterConditions.value.push({ field: null, operator: '=', value: '' })
}

const removeCondition = (index: number) => {
  if (filterConditions.value.length > 1) {
    filterConditions.value.splice(index, 1)
    emitChange()
  }
}

const handleGetMaxValue = () => {
  if (incrementField.value) {
    loadingMaxValue.value = true
    emit('getMaxValue', incrementField.value)
    // 父组件获取到值后会通过 modelValue 传回
    setTimeout(() => { loadingMaxValue.value = false }, 3000)
  }
}

// 暴露设置增量起始值的方法，供父组件调用
const setIncrementStartValue = (val: string) => {
  incrementStartValue.value = val
  loadingMaxValue.value = false
  emitChange()
}

defineExpose({ setIncrementStartValue })

const emitChange = () => {
  const config: ImportModeConfig = { importMode: mode.value }
  if (mode.value === 'incremental') {
    if (props.isDatabase) {
      config.incrementField = incrementField.value || undefined
      config.incrementStartValue = incrementStartValue.value || undefined
    } else {
      config.deduplicateField = incrementField.value || undefined
    }
  } else if (mode.value === 'conditional') {
    config.filterConditions = filterConditions.value.filter(c => c.field)
  }
  emit('update:modelValue', config)
}
</script>

<style scoped>
.import-mode-selector {
  margin: 12px 0;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.condition-builder {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.condition-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

</style>

<style>
/* ImportModeSelector 深色模式（非 scoped） */
html.dark .import-mode-selector {
  background: #1a2536 !important;
  border-color: #334155 !important;
}
</style>

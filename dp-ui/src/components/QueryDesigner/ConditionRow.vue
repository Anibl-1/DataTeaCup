<template>
  <div class="condition-row" :class="{ 'is-incomplete': !isComplete }">
    <!-- 拖拽手柄 -->
    <n-icon 
      v-if="showDragHandle" 
      class="drag-handle" 
      size="14" 
      color="#94a3b8"
    >
      <ReorderThreeOutline />
    </n-icon>

    <!-- 字段选择 -->
    <n-select
      v-model:value="localCondition.field"
      :options="availableFields"
      size="small"
      filterable
      placeholder="选择字段"
      class="field-select"
      @update:value="handleFieldChange"
    />

    <!-- 运算符选择 -->
    <n-select
      v-model:value="localCondition.operator"
      :options="operatorOptions"
      size="small"
      class="operator-select"
      @update:value="handleOperatorChange"
    />

    <!-- 值输入区域 -->
    <div class="value-inputs">
      <!-- 普通值输入 -->
      <n-input
        v-if="operatorConfig?.needsValue"
        v-model:value="localCondition.value"
        size="small"
        :placeholder="operatorConfig?.placeholder || '输入值'"
        class="value-input"
        @update:value="emitUpdate"
      />

      <!-- BETWEEN 第二个值 -->
      <template v-if="operatorConfig?.needsSecondValue">
        <span class="value-separator">至</span>
        <n-input
          v-model:value="localCondition.value2"
          size="small"
          :placeholder="operatorConfig?.placeholder2 || '结束值'"
          class="value-input"
          @update:value="emitUpdate"
        />
      </template>

      <!-- IN 值列表 -->
      <n-dynamic-tags
        v-if="operatorConfig?.needsValueList"
        v-model:value="valueListTags"
        size="small"
        class="value-tags"
        @update:value="handleTagsChange"
      />

      <!-- 无需值的提示 -->
      <span v-if="!operatorConfig?.needsValue && !operatorConfig?.needsValueList" class="no-value-hint">
        无需输入值
      </span>
    </div>

    <!-- 删除按钮 -->
    <n-button 
      text 
      type="error" 
      size="small" 
      class="remove-btn"
      @click="$emit('remove')"
    >
      <n-icon><TrashOutline /></n-icon>
    </n-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ReorderThreeOutline, TrashOutline } from '@vicons/ionicons5'
import type { ConditionItemType, ComparisonOperator } from './conditionTypes'
import { OPERATOR_OPTIONS, getOperatorConfig } from './conditionTypes'
import { deepClone, isConditionComplete } from './conditionUtils'

const props = defineProps<{
  condition: ConditionItemType
  availableFields: Array<{ label: string; value: string }>
  showDragHandle?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:condition', condition: ConditionItemType): void
  (e: 'remove'): void
}>()

const localCondition = ref<ConditionItemType>(deepClone(props.condition))
const valueListTags = ref<string[]>([])

// 同步 props
watch(() => props.condition, (newVal) => {
  localCondition.value = deepClone(newVal)
  valueListTags.value = newVal.valueList || []
}, { immediate: true, deep: true })

const operatorOptions = OPERATOR_OPTIONS

const operatorConfig = computed(() => {
  return getOperatorConfig(localCondition.value.operator)
})

const isComplete = computed(() => {
  return isConditionComplete(localCondition.value)
})

function handleFieldChange() {
  emitUpdate()
}

function handleOperatorChange(newOperator: ComparisonOperator) {
  // 切换运算符时重置值
  const config = getOperatorConfig(newOperator)
  if (config) {
    if (!config.needsValue) {
      localCondition.value.value = ''
    }
    if (!config.needsSecondValue) {
      localCondition.value.value2 = ''
    }
    if (!config.needsValueList) {
      localCondition.value.valueList = []
      valueListTags.value = []
    }
  }
  emitUpdate()
}

function handleTagsChange(tags: string[]) {
  localCondition.value.valueList = tags
  // 同时更新 value 字段以兼容旧格式
  localCondition.value.value = tags.join(', ')
  emitUpdate()
}

function emitUpdate() {
  emit('update:condition', deepClone(localCondition.value))
}
</script>

<style scoped>
.condition-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.condition-row:hover {
  border-color: #cbd5e1;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.condition-row.is-incomplete {
  border-color: #fbbf24;
  background: #fffbeb;
}

.drag-handle {
  cursor: grab;
  opacity: 0.5;
  transition: opacity 0.15s;
  flex-shrink: 0;
}

.condition-row:hover .drag-handle {
  opacity: 1;
}

.drag-handle:active {
  cursor: grabbing;
}

.field-select {
  width: 160px;
  flex-shrink: 0;
}

.operator-select {
  width: 140px;
  flex-shrink: 0;
}

.value-inputs {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
}

.value-input {
  flex: 1;
  min-width: 80px;
}

.value-separator {
  color: #64748b;
  font-size: 12px;
  flex-shrink: 0;
}

.value-tags {
  flex: 1;
  min-width: 120px;
}

.no-value-hint {
  color: #94a3b8;
  font-size: 11px;
  font-style: italic;
}

.remove-btn {
  opacity: 0;
  transition: opacity 0.15s;
  flex-shrink: 0;
}

.condition-row:hover .remove-btn {
  opacity: 1;
}
</style>

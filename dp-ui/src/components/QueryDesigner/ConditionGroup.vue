<template>
  <div class="condition-group" :class="{ 'is-root': isRoot, 'is-nested': !isRoot }">
    <!-- 组头部 -->
    <div v-if="!isRoot" class="group-header">
      <div class="group-logic-badge" @click="toggleGroupLogic">
        <span class="logic-text">{{ group.logic }}</span>
        <n-icon size="10"><SwapHorizontalOutline /></n-icon>
      </div>
      <span class="group-label">条件组</span>
      <n-space size="small" class="group-actions">
        <n-button size="tiny" quaternary @click="addConditionToGroup">
          <template #icon><n-icon size="12"><AddOutline /></n-icon></template>
        </n-button>
        <n-button size="tiny" quaternary @click="addNestedGroup">
          <template #icon><n-icon size="12"><FolderOutline /></n-icon></template>
        </n-button>
        <n-button size="tiny" quaternary type="error" @click="$emit('remove')">
          <template #icon><n-icon size="12"><TrashOutline /></n-icon></template>
        </n-button>
      </n-space>
    </div>

    <!-- 条件列表 -->
    <draggable
      v-model="localChildren"
      item-key="id"
      handle=".drag-handle"
      :group="{ name: 'conditions', pull: true, put: true }"
      class="conditions-list"
      ghost-class="condition-ghost"
      chosen-class="condition-chosen"
      animation="200"
      @change="handleDragChange"
    >
      <template #item="{ element, index }">
        <div class="condition-wrapper">
          <!-- 逻辑连接符 -->
          <div v-if="index > 0" class="logic-connector">
            <n-button 
              size="tiny" 
              :type="getLogicType(index)"
              secondary
              class="logic-button"
              @click="toggleLogic(index)"
            >
              {{ getPrevLogic(index) }}
            </n-button>
          </div>

          <!-- 条件组 -->
          <ConditionGroup
            v-if="element.type === 'group'"
            :group="element"
            :available-fields="availableFields"
            :is-root="false"
            @update:group="(g) => updateChild(index, g)"
            @remove="removeChild(index)"
          />

          <!-- 单个条件 -->
          <ConditionRow
            v-else
            :condition="element"
            :available-fields="availableFields"
            :show-drag-handle="localChildren.length > 1"
            @update:condition="(c) => updateChild(index, c)"
            @remove="removeChild(index)"
          />
        </div>
      </template>
    </draggable>

    <!-- 空状态 -->
    <div v-if="localChildren.length === 0" class="empty-group">
      <n-button size="small" dashed @click="addConditionToGroup">
        <template #icon><n-icon><AddOutline /></n-icon></template>
        添加条件
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { AddOutline, FolderOutline, TrashOutline, SwapHorizontalOutline } from '@vicons/ionicons5'
import draggable from 'vuedraggable'
import ConditionRow from './ConditionRow.vue'
import type { ConditionGroupType, ConditionItemType, LogicOperator } from './conditionTypes'
import { createEmptyCondition, createEmptyGroup, deepClone } from './conditionUtils'

const props = defineProps<{
  group: ConditionGroupType
  availableFields: Array<{ label: string; value: string }>
  isRoot?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:group', group: ConditionGroupType): void
  (e: 'remove'): void
}>()

const localChildren = ref<(ConditionItemType | ConditionGroupType)[]>([])

// 同步 props 到本地状态
watch(() => props.group.children, (newChildren) => {
  localChildren.value = deepClone(newChildren) as (ConditionItemType | ConditionGroupType)[]
}, { immediate: true, deep: true })

// 监听本地变化并 emit
watch(localChildren, (newChildren) => {
  emitUpdate()
}, { deep: true })

function emitUpdate() {
  emit('update:group', {
    ...props.group,
    children: localChildren.value
  })
}

function toggleGroupLogic() {
  const newLogic: LogicOperator = props.group.logic === 'AND' ? 'OR' : 'AND'
  emit('update:group', {
    ...props.group,
    logic: newLogic
  })
}

function getPrevLogic(index: number): LogicOperator {
  if (index === 0) return props.group.logic
  const prevChild = localChildren.value[index - 1]
  return ('logic' in prevChild) ? prevChild.logic : props.group.logic
}

function getLogicType(index: number): 'primary' | 'warning' {
  return getPrevLogic(index) === 'AND' ? 'primary' : 'warning'
}

function toggleLogic(index: number) {
  if (index === 0) return
  const prevChild = localChildren.value[index - 1]
  if ('logic' in prevChild) {
    prevChild.logic = prevChild.logic === 'AND' ? 'OR' : 'AND'
    emitUpdate()
  }
}

function updateChild(index: number, child: ConditionItemType | ConditionGroupType) {
  localChildren.value[index] = child
  emitUpdate()
}

function removeChild(index: number) {
  localChildren.value.splice(index, 1)
  emitUpdate()
}

function addConditionToGroup() {
  localChildren.value.push(createEmptyCondition())
  emitUpdate()
}

function addNestedGroup() {
  localChildren.value.push(createEmptyGroup())
  emitUpdate()
}

function handleDragChange() {
  emitUpdate()
}
</script>

<style scoped>
.condition-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.condition-group.is-nested {
  padding: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  border-left: 3px solid var(--color-primary);
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 6px;
  margin-bottom: 4px;
  border-bottom: 1px dashed #e2e8f0;
}

.group-logic-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: var(--color-primary);
  color: white;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.group-logic-badge:hover {
  background: #0052cc;
}

.group-label {
  font-size: 11px;
  color: #64748b;
  flex: 1;
}

.group-actions {
  opacity: 0;
  transition: opacity 0.15s;
}

.condition-group.is-nested:hover .group-actions {
  opacity: 1;
}

.conditions-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 20px;
}

.condition-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.logic-connector {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 2px 0;
}

.logic-button {
  font-size: 10px;
  font-weight: 600;
  min-width: 40px;
}

.empty-group {
  display: flex;
  justify-content: center;
  padding: 12px;
}

/* 拖拽样式 */
.condition-ghost {
  opacity: 0.4;
  background: #e0f2fe !important;
}

.condition-chosen {
  box-shadow: 0 2px 8px rgba(0, 102, 255, 0.2);
}
</style>

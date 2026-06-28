<template>
  <div class="condition-builder">
    <div class="builder-header">
      <span class="header-title">WHERE 条件构建器</span>
      <n-space size="small">
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-icon size="14" color="#94a3b8" style="cursor: help;"><HelpCircleOutline /></n-icon>
          </template>
          <div style="max-width: 280px; font-size: 12px;">
            <p style="margin: 0 0 8px 0;"><b>使用说明：</b></p>
            <p style="margin: 0 0 4px 0;">• 点击"添加条件"添加单个筛选条件</p>
            <p style="margin: 0 0 4px 0;">• 点击"添加条件组"创建嵌套条件（括号）</p>
            <p style="margin: 0 0 4px 0;">• 拖拽条件可调整顺序</p>
            <p style="margin: 0;">• 点击 AND/OR 切换逻辑关系</p>
          </div>
        </n-tooltip>
        <n-button size="tiny" quaternary :disabled="!hasConditions" @click="clearAll">
          清空
        </n-button>
      </n-space>
    </div>

    <div class="builder-content">
      <div v-if="!hasConditions" class="empty-state">
        <n-icon size="32" color="#cbd5e1"><FilterOutline /></n-icon>
        <p>暂无筛选条件</p>
        <n-space size="small">
          <n-button size="small" type="primary" @click="addCondition">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            添加条件
          </n-button>
          <n-button size="small" @click="addGroup">
            <template #icon><n-icon><FolderOutline /></n-icon></template>
            添加条件组
          </n-button>
        </n-space>
      </div>

      <div v-else class="conditions-container">
        <ConditionGroup
          :group="rootGroup"
          :available-fields="availableFields"
          :is-root="true"
          @update:group="handleRootUpdate"
          @remove="handleRemoveRoot"
        />
        
        <div class="add-buttons">
          <n-button size="small" type="primary" dashed @click="addCondition">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            添加条件
          </n-button>
          <n-button size="small" dashed @click="addGroup">
            <template #icon><n-icon><FolderOutline /></n-icon></template>
            添加条件组
          </n-button>
        </div>
      </div>
    </div>

    <!-- SQL 预览 -->
    <div v-if="hasConditions" class="sql-preview">
      <div class="preview-header">
        <span>WHERE 子句预览</span>
        <n-button size="tiny" text type="primary" @click="copyWhereClause">复制</n-button>
      </div>
      <pre class="preview-code">{{ whereClausePreview }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useMessage } from 'naive-ui'
import { HelpCircleOutline, FilterOutline, AddOutline, FolderOutline } from '@vicons/ionicons5'
import ConditionGroup from './ConditionGroup.vue'
import type { ConditionGroupType, ConditionItemType } from './conditionTypes'
import { generateWhereClause, createEmptyCondition, createEmptyGroup } from './conditionUtils'

const props = defineProps<{
  modelValue: ConditionItemType[]
  availableFields: Array<{ label: string; value: string }>
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: ConditionItemType[]): void
}>()

const message = useMessage()

// 内部使用的根条件组
const rootGroup = ref<ConditionGroupType>({
  id: 'root',
  type: 'group',
  logic: 'AND',
  children: []
})

// 从 modelValue 初始化
watch(() => props.modelValue, (newVal) => {
  if (newVal && newVal.length > 0) {
    // 将扁平条件转换为嵌套结构
    rootGroup.value.children = convertToNestedStructure(newVal)
  } else {
    rootGroup.value.children = []
  }
}, { immediate: true, deep: true })

// 将扁平条件数组转换为嵌套结构
function convertToNestedStructure(conditions: ConditionItemType[]): (ConditionItemType | ConditionGroupType)[] {
  // 如果已经是嵌套结构，直接返回
  if (conditions.some(c => 'type' in c && c.type === 'group')) {
    return conditions as (ConditionItemType | ConditionGroupType)[]
  }
  // 否则将扁平条件包装
  return conditions.map(c => ({
    ...c,
    type: 'condition' as const
  }))
}

// 将嵌套结构转换回扁平条件数组（用于兼容现有 sqlGenerator）
function convertToFlatStructure(children: (ConditionItemType | ConditionGroupType)[]): ConditionItemType[] {
  const result: ConditionItemType[] = []
  
  for (const child of children) {
    if ('type' in child && child.type === 'group') {
      // 对于条件组，递归处理
      const groupConditions = convertToFlatStructure((child as ConditionGroupType).children)
      if (groupConditions.length > 0) {
        // 标记为组的开始
        result.push({
          ...groupConditions[0],
          groupStart: true,
          groupLogic: (child as ConditionGroupType).logic
        })
        result.push(...groupConditions.slice(1))
        // 标记组的结束
        if (result.length > 0) {
          result[result.length - 1] = {
            ...result[result.length - 1],
            groupEnd: true
          }
        }
      }
    } else {
      result.push(child as ConditionItemType)
    }
  }
  
  return result
}

const hasConditions = computed(() => rootGroup.value.children.length > 0)

const whereClausePreview = computed(() => {
  if (!hasConditions.value) return ''
  return generateWhereClause(rootGroup.value)
})

function handleRootUpdate(newGroup: ConditionGroupType) {
  rootGroup.value = newGroup
  // 同步到 modelValue
  emitUpdate()
}

function handleRemoveRoot() {
  rootGroup.value.children = []
  emitUpdate()
}

function emitUpdate() {
  const flatConditions = convertToFlatStructure(rootGroup.value.children)
  emit('update:modelValue', flatConditions)
}

function addCondition() {
  const newCondition = createEmptyCondition()
  rootGroup.value.children.push(newCondition)
  emitUpdate()
}

function addGroup() {
  const newGroup = createEmptyGroup()
  rootGroup.value.children.push(newGroup)
  emitUpdate()
}

function clearAll() {
  rootGroup.value.children = []
  emitUpdate()
  message.success('已清空所有条件')
}

function copyWhereClause() {
  if (!whereClausePreview.value) return
  navigator.clipboard.writeText(whereClausePreview.value)
  message.success('WHERE 子句已复制')
}
</script>

<style scoped>
.condition-builder {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.builder-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid #e2e8f0;
}

.header-title {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.builder-content {
  min-height: 100px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 24px;
  background: #f8fafc;
  border: 2px dashed #e2e8f0;
  border-radius: 8px;
  color: #94a3b8;
}

.empty-state p {
  margin: 0;
  font-size: 13px;
}

.conditions-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.add-buttons {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
}

.sql-preview {
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  overflow: hidden;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  background: #f1f5f9;
  font-size: 11px;
  font-weight: 500;
  color: #64748b;
}

.preview-code {
  margin: 0;
  padding: 8px 10px;
  background: #f8fafc;
  color: #0f172a;
  font-family: 'Cascadia Code', 'Fira Code', Consolas, monospace;
  font-size: 11px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>

<template>
  <div class="parameter-panel-container" :class="{ 'design-mode': designMode }">
    <!-- 设计模式：可拖拽添加和配置组件 -->
    <div v-if="designMode" class="panel-design-area">
      <div 
        class="panel-drop-zone"
        :class="{ 'drag-over': isDraggingOver }"
        @drop="handleDrop"
        @dragover.prevent="handleDragOver"
        @dragleave="handleDragLeave"
      >
        <div v-if="components.length === 0" class="empty-tip">
          <n-icon size="24" color="#ccc"><FilterOutline /></n-icon>
          <span>从左侧拖拽查询组件到此处</span>
        </div>
        <div v-else class="components-row">
          <div
            v-for="(comp, index) in sortedComponents"
            :key="comp.id"
            class="component-wrapper"
            :class="{ selected: selectedComponentId === comp.id }"
            :style="{ width: comp.width ? `${comp.width}px` : '220px', minWidth: '180px', flex: '1 1 auto', maxWidth: '360px' }"
            draggable="true"
            @click="handleSelectComponent(comp)"
            @dragstart="handleComponentDragStart($event, comp, index)"
            @dragover.prevent="handleComponentDragOver($event, index)"
            @drop="handleComponentDrop($event, index)"
          >
            <div class="component-label">{{ comp.label }}</div>
            <div class="component-preview">
              <component :is="getComponentPreview(comp.type)" :comp="comp" />
            </div>
            <n-button 
              class="component-delete" 
              size="tiny" 
              circle 
              type="error"
              @click.stop="handleDeleteComponent(comp.id)"
            >
              <template #icon><n-icon size="12"><CloseOutline /></n-icon></template>
            </n-button>
          </div>
          <!-- 查询和重置按钮 -->
          <div class="action-buttons">
            <n-button type="primary" size="small" disabled>
              <template #icon><n-icon><SearchOutline /></n-icon></template>
              查询
            </n-button>
            <n-button size="small" disabled>
              <template #icon><n-icon><RefreshOutline /></n-icon></template>
              重置
            </n-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 运行模式：显示实际的输入组件 -->
    <div v-else class="panel-runtime-area">
      <div class="components-row">
        <div
          v-for="comp in sortedComponents"
          :key="comp.id"
          class="runtime-component"
          :style="{ width: comp.width ? `${comp.width}px` : '220px', minWidth: comp.type === 'dateRange' ? '280px' : '180px', flex: '1 1 auto', maxWidth: comp.type === 'dateRange' ? '380px' : '320px' }"
        >
          <label :class="{ required: comp.required }">{{ comp.label }}</label>
          <!-- 文本输入 -->
          <n-input
            v-if="comp.type === 'text'"
            v-model:value="paramValues[comp.name]"
            :placeholder="comp.placeholder || '请输入'"
            size="small"
            clearable
          />
          <!-- 数字输入 -->
          <n-input-number
            v-else-if="comp.type === 'number'"
            v-model:value="paramValues[comp.name]"
            :placeholder="comp.placeholder || '请输入'"
            v-bind="{ ...(comp.min != null ? { min: comp.min } : {}), ...(comp.max != null ? { max: comp.max } : {}) }"
            size="small"
            clearable
            style="width: 100%"
          />
          <!-- 日期选择 -->
          <n-date-picker
            v-else-if="comp.type === 'date'"
            v-model:formatted-value="paramValues[comp.name]"
            type="date"
            :placeholder="comp.placeholder || '选择日期'"
            :value-format="comp.dateFormat || 'yyyy-MM-dd'"
            size="small"
            clearable
            style="width: 100%"
          />
          <!-- 日期范围 -->
          <n-date-picker
            v-else-if="comp.type === 'dateRange'"
            v-model:formatted-value="paramValues[comp.name]"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            :value-format="comp.dateFormat || 'yyyy-MM-dd'"
            size="small"
            clearable
            style="width: 100%"
          />
          <!-- 下拉单选 -->
          <n-select
            v-else-if="comp.type === 'select'"
            v-model:value="paramValues[comp.name]"
            :options="(comp.options as any) || []"
            :placeholder="comp.placeholder || '请选择'"
            size="small"
            clearable
          />
          <!-- 下拉多选 -->
          <n-select
            v-else-if="comp.type === 'multiSelect'"
            v-model:value="paramValues[comp.name]"
            :options="(comp.options as any) || []"
            :placeholder="comp.placeholder || '请选择'"
            size="small"
            multiple
            clearable
            max-tag-count="responsive"
          />
          <!-- 级联选择 -->
          <n-cascader
            v-else-if="comp.type === 'cascader'"
            v-model:value="paramValues[comp.name]"
            :options="(comp.options as any) || []"
            :placeholder="comp.placeholder || '请选择'"
            size="small"
            clearable
            check-strategy="child"
          />
        </div>
        <!-- 查询和重置按钮 -->
        <div v-if="showQueryButton || showResetButton" class="action-buttons">
          <n-button v-if="showQueryButton" type="primary" size="small" @click="handleQuery">
            <template #icon><n-icon><SearchOutline /></n-icon></template>
            查询
          </n-button>
          <n-button v-if="showResetButton" size="small" @click="handleReset">
            <template #icon><n-icon><RefreshOutline /></n-icon></template>
            重置
          </n-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, watch } from 'vue'
import { NIcon, NButton, NInput, NInputNumber, NDatePicker, NSelect, NCascader } from 'naive-ui'
import { FilterOutline, CloseOutline, SearchOutline, RefreshOutline } from '@vicons/ionicons5'
import type { QueryComponent, QueryComponentType } from '@/types/pageParameter'

// 组件预览
import TextPreview from './previews/TextPreview.vue'
import NumberPreview from './previews/NumberPreview.vue'
import DatePreview from './previews/DatePreview.vue'
import SelectPreview from './previews/SelectPreview.vue'

const props = defineProps<{
  /** 是否为设计模式 */
  designMode?: boolean
  /** 组件列表 */
  components: QueryComponent[]
  /** 是否显示查询按钮 */
  showQueryButton?: boolean
  /** 是否显示重置按钮 */
  showResetButton?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:components', components: QueryComponent[]): void
  (e: 'select', component: QueryComponent | null): void
  (e: 'query', params: Record<string, any>): void
  (e: 'reset'): void
}>()

// 参数值
const paramValues = ref<Record<string, any>>({})

// 选中的组件ID
const selectedComponentId = ref<string | null>(null)

// 拖拽状态
const isDraggingOver = ref(false)
const draggedComponentIndex = ref<number | null>(null)

// 排序后的组件
const sortedComponents = computed(() => {
  return [...props.components].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
})

// 初始化参数默认值
watch(() => props.components, (newComponents) => {
  newComponents.forEach(comp => {
    if (paramValues.value[comp.name] === undefined && comp.defaultValue !== undefined) {
      paramValues.value[comp.name] = comp.defaultValue
    }
  })
}, { immediate: true, deep: true })

// 获取组件预览
const getComponentPreview = (type: QueryComponentType) => {
  const previewMap: Record<QueryComponentType, any> = {
    text: TextPreview,
    number: NumberPreview,
    date: DatePreview,
    dateRange: DatePreview,
    select: SelectPreview,
    multiSelect: SelectPreview,
    cascader: SelectPreview
  }
  return previewMap[type] || TextPreview
}

// 处理拖拽进入
const handleDragOver = (e: DragEvent) => {
  e.preventDefault()
  isDraggingOver.value = true
}

// 处理拖拽离开
const handleDragLeave = () => {
  isDraggingOver.value = false
}

// 处理放置新组件
const handleDrop = (e: DragEvent) => {
  e.preventDefault()
  isDraggingOver.value = false
  
  const componentData = e.dataTransfer?.getData('queryComponent')
  if (componentData) {
    try {
      const newComponent = JSON.parse(componentData) as QueryComponent
      newComponent.sortOrder = props.components.length
      const updatedComponents = [...props.components, newComponent]
      emit('update:components', updatedComponents)
      emit('select', newComponent)
      selectedComponentId.value = newComponent.id
    } catch (err) {
      console.error('解析组件数据失败:', err)
    }
  }
}

// 选中组件
const handleSelectComponent = (comp: QueryComponent) => {
  selectedComponentId.value = comp.id
  emit('select', comp)
}

// 删除组件
const handleDeleteComponent = (id: string) => {
  const updatedComponents = props.components.filter(c => c.id !== id)
  emit('update:components', updatedComponents)
  if (selectedComponentId.value === id) {
    selectedComponentId.value = null
    emit('select', null)
  }
}

// 组件内部拖拽排序
const handleComponentDragStart = (e: DragEvent, _comp: QueryComponent, index: number) => {
  draggedComponentIndex.value = index
  e.dataTransfer?.setData('reorderComponent', String(index))
}

const handleComponentDragOver = (e: DragEvent, _index: number) => {
  e.preventDefault()
}

const handleComponentDrop = (e: DragEvent, targetIndex: number) => {
  e.preventDefault()
  e.stopPropagation()
  
  const sourceIndex = draggedComponentIndex.value
  if (sourceIndex === null || sourceIndex === targetIndex) return
  
  const updatedComponents = [...props.components]
  const [moved] = updatedComponents.splice(sourceIndex, 1)
  if (!moved) return
  updatedComponents.splice(targetIndex, 0, moved)
  
  // 更新排序顺序
  updatedComponents.forEach((comp, idx) => {
    comp.sortOrder = idx
  })
  
  emit('update:components', updatedComponents)
  draggedComponentIndex.value = null
}

// 查询
const handleQuery = () => {
  emit('query', { ...paramValues.value })
}

// 重置
const handleReset = () => {
  paramValues.value = {}
  props.components.forEach(comp => {
    if (comp.defaultValue !== undefined) {
      paramValues.value[comp.name] = comp.defaultValue
    }
  })
  emit('reset')
}

// 暴露方法
defineExpose({
  getParamValues: () => ({ ...paramValues.value }),
  setParamValues: (values: Record<string, any>) => {
    paramValues.value = { ...values }
  },
  resetParams: handleReset
})
</script>

<style scoped>
.parameter-panel-container {
  background: #fafbfc;
  border-bottom: 1px solid #e8e8e8;
  min-height: 60px;
}



.parameter-panel-container.design-mode {
  background: #f0f9f4;
  border: 2px dashed #18a058;
  border-radius: 4px;
  margin: 8px;
}

.panel-design-area,
.panel-runtime-area {
  padding: 14px 20px;
}

.panel-drop-zone {
  min-height: 50px;
  border-radius: 4px;
  transition: all 0.2s;
}

.panel-drop-zone.drag-over {
  background: rgba(24, 160, 88, 0.1);
  border-color: #18a058;
}

.empty-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #999;
  font-size: 13px;
  padding: 12px;
}

.components-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px 16px;
}

.component-wrapper {
  position: relative;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  cursor: move;
  transition: all 0.2s;
}

.component-wrapper:hover {
  border-color: #18a058;
  box-shadow: 0 2px 8px rgba(24, 160, 88, 0.15);
}

.component-wrapper.selected {
  border-color: #18a058;
  border-width: 2px;
  box-shadow: 0 2px 12px rgba(24, 160, 88, 0.2);
}

.component-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.component-preview {
  min-width: 160px;
}

.component-delete {
  position: absolute;
  top: -8px;
  right: -8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.component-wrapper:hover .component-delete {
  opacity: 1;
}

.runtime-component {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.runtime-component label {
  font-size: 12px;
  font-weight: 500;
  color: #606266;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.5;
}

.runtime-component label.required::before {
  content: '*';
  color: #d03050;
  margin-right: 2px;
}

.runtime-component :deep(.n-input),
.runtime-component :deep(.n-select),
.runtime-component :deep(.n-date-picker),
.runtime-component :deep(.n-input-number),
.runtime-component :deep(.n-cascader) {
  width: 100% !important;
}

.action-buttons {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding-bottom: 1px;
}
</style>

<style>
/* ParameterPanel 深色模式（非 scoped） */
html.dark .parameter-panel-container {
  background: #1a2332 !important;
  border-bottom-color: #334155 !important;
}
html.dark .runtime-component label {
  color: #cbd5e1 !important;
}
</style>

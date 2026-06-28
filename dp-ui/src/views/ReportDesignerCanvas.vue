<template>
  <n-card class="designer-card fields-card" size="small">
    <template #header>
      <div class="card-header-custom">
        <div class="card-title">
          <n-icon size="20" color="#f0a020"><SettingsOutline /></n-icon>
          <span>字段配置</span>
        </div>
        <n-tag type="info" size="small">{{ fields.length }} 个字段</n-tag>
        <n-tag v-if="fields.length > 0" type="warning" size="small" style="margin-left: 8px;">
          <template #icon><n-icon size="14"><ReorderTwoOutline /></n-icon></template>
          拖拽排序
        </n-tag>
      </div>
    </template>
    <template #header-extra>
      <n-button size="small" type="primary" :loading="fieldLoading" @click="$emit('autoGetFields')">
        <template #icon><n-icon><RefreshOutline /></n-icon></template>
        刷新字段
      </n-button>
    </template>
    
    <n-alert v-if="fields.length === 0 && !fieldLoading" type="info" style="margin-bottom: 16px;">
      <template #icon><n-icon><BulbOutline /></n-icon></template>
      请先输入SQL查询语句，然后点击"自动获取字段"按钮来配置报表字段
    </n-alert>
    
    <!-- 字段表格 -->
    <div v-if="fields.length > 0" class="fields-table">
      <div class="fields-table-header">
        <div class="field-col field-col-drag"></div>
        <div class="field-col field-col-order">序号</div>
        <div class="field-col field-col-name">字段名称</div>
        <div class="field-col field-col-label">显示名称（表头）</div>
        <div class="field-col field-col-type">字段类型</div>
        <div class="field-col field-col-visible">可见</div>
        <div class="field-col field-col-width">宽度</div>
        <div class="field-col field-col-dict">字典类型</div>
        <div class="field-col field-col-align">对齐方式</div>
      </div>
      
      <!-- 可拖拽字段列表 -->
      <draggable
        :model-value="fields"
        item-key="fieldName"
        handle=".drag-handle"
        ghost-class="field-row-ghost"
        chosen-class="field-row-chosen"
        drag-class="field-row-drag"
        animation="200"
        class="fields-table-body"
        @update:model-value="$emit('update:fields', $event)"
        @end="$emit('fieldDragEnd')"
      >
        <template #item="{ element, index }">
          <div class="field-row" :class="{ 'field-row-even': index % 2 === 1 }">
            <div class="field-col field-col-drag">
              <div class="drag-handle" title="拖拽排序">
                <n-icon size="18" color="#999"><ReorderTwoOutline /></n-icon>
              </div>
            </div>
            <div class="field-col field-col-order">{{ index + 1 }}</div>
            <div class="field-col field-col-name">
              <n-ellipsis :tooltip="{ width: 200 }">{{ element.fieldName }}</n-ellipsis>
            </div>
            <div class="field-col field-col-label">
              <n-input
                v-model:value="element.fieldLabel"
                :placeholder="element.fieldName"
                size="small"
              />
            </div>
            <div class="field-col field-col-type">
              <n-ellipsis :tooltip="{ width: 150 }">{{ element.fieldType }}</n-ellipsis>
            </div>
            <div class="field-col field-col-visible">
              <n-switch
                :value="element.isVisible !== 0"
                size="small"
                @update:value="(val: boolean) => { element.isVisible = val ? 1 : 0 }"
              />
            </div>
            <div class="field-col field-col-width">
              <n-input-number
                v-model:value="element.width"
                placeholder="自动"
                :min="0"
                size="small"
                style="width: 100%"
              />
            </div>
            <div class="field-col field-col-dict">
              <n-select
                v-model:value="element.dictType"
                :options="dictTypeOptions"
                placeholder="无"
                clearable
                filterable
                size="small"
              />
            </div>
            <div class="field-col field-col-align">
              <n-select
                v-model:value="element.align"
                :options="alignOptions"
                size="small"
              />
            </div>
          </div>
        </template>
      </draggable>
    </div>
    
    <!-- 空状态 -->
    <div v-if="fields.length === 0 && !fieldLoading" class="empty-fields">
      <n-icon size="48" color="#ddd"><ListOutline /></n-icon>
      <div class="empty-title">暂无字段配置</div>
      <div class="empty-desc">请先输入SQL查询语句，然后点击"自动获取字段"按钮</div>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="fieldLoading" class="fields-loading">
      <n-spin size="medium" />
      <span>正在加载字段...</span>
    </div>
  </n-card>
</template>

<script setup lang="ts">
import draggable from 'vuedraggable'
import {
  SettingsOutline,
  RefreshOutline,
  BulbOutline,
  ReorderTwoOutline,
  ListOutline
} from '@vicons/ionicons5'
import type { ReportField } from '@/types/reportDefinition'

defineProps<{
  fields: ReportField[]
  fieldLoading: boolean
  dictTypeOptions: Array<{ label: string; value: string }>
}>()

defineEmits<{
  'update:fields': [fields: ReportField[]]
  autoGetFields: []
  fieldDragEnd: []
}>()

const alignOptions = [
  { label: '左对齐', value: 'left' },
  { label: '居中', value: 'center' },
  { label: '右对齐', value: 'right' }
]
</script>

<style scoped>
.designer-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.designer-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.card-header-custom {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
}

.empty-fields {
  padding: 40px 20px;
  text-align: center;
}

.empty-title {
  margin-top: 12px;
  font-size: 15px;
  color: #666;
}

.empty-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #999;
}

.fields-table {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  max-height: 400px;
  overflow-y: auto;
}

.fields-table-header {
  display: flex;
  align-items: center;
  background: #f8f9fa;
  border-bottom: 1px solid #e8e8e8;
  font-weight: 600;
  font-size: 13px;
  color: #333;
  position: sticky;
  top: 0;
  z-index: 1;
}

.fields-table-body {
  min-height: 50px;
}

.field-row {
  display: flex;
  align-items: center;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s ease;
}

.field-row:last-child {
  border-bottom: none;
}

.field-row:hover {
  background-color: #f5f7fa;
}

.field-row-even {
  background-color: #fafbfc;
}

.field-row-even:hover {
  background-color: #f0f2f5;
}

.field-row-ghost {
  opacity: 0.5;
  background: #e6f7ff !important;
  border: 2px dashed #1890ff !important;
}

.field-row-chosen {
  background: #e6f7ff !important;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
}

.field-row-drag {
  opacity: 0.9;
  background: #fff !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-radius: 4px;
}

.field-col {
  padding: 10px 8px;
  display: flex;
  align-items: center;
  min-height: 44px;
}

.field-col-drag { width: 40px; justify-content: center; flex-shrink: 0; }
.field-col-order { width: 50px; justify-content: center; flex-shrink: 0; color: #999; font-size: 12px; }
.field-col-name { width: 160px; flex-shrink: 0; font-family: 'Monaco', 'Menlo', monospace; font-size: 12px; color: #666; }
.field-col-label { width: 180px; flex-shrink: 0; }
.field-col-type { width: 100px; flex-shrink: 0; font-size: 12px; color: #999; }
.field-col-visible { width: 70px; justify-content: center; flex-shrink: 0; }
.field-col-width { width: 100px; flex-shrink: 0; }
.field-col-dict { width: 140px; flex-shrink: 0; }
.field-col-align { width: 110px; flex-shrink: 0; }

.drag-handle {
  cursor: grab;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.drag-handle:hover {
  background: #e6f7ff;
  color: #1890ff;
}

.drag-handle:active {
  cursor: grabbing;
}

.fields-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
  color: #999;
}

</style>

<style>
/* ReportDesignerCanvas 深色模式（非 scoped） */
html.dark .canvas-container { background: #0f172a !important; }
html.dark .canvas-wrapper { background: #1e293b !important; }
html.dark .component-item { background: #1a2536 !important; border-color: #334155 !important; color: #e2e8f0 !important; }
html.dark .component-item:hover { border-color: var(--color-primary) !important; }
html.dark .drop-zone { border-color: #334155 !important; background: #1a2536 !important; }
html.dark .placeholder-text { color: #64748b !important; }
</style>

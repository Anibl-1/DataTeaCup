<template>
  <div>
    <!-- 页面头部 -->
    <div class="designer-header">
      <div class="header-left">
        <div class="header-icon">
          <n-icon size="28" color="#fff"><CreateOutline /></n-icon>
        </div>
        <div class="header-info">
          <h2 class="header-title">{{ isEditMode ? '编辑报表' : '新建报表' }}</h2>
          <p class="header-subtitle">{{ isEditMode ? `正在编辑: ${reportName || '未命名报表'}` : '创建新的数据报表' }}</p>
        </div>
      </div>
      <div class="header-right">
        <n-space>
          <!-- 撤销/重做按钮 -->
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button 
                :disabled="!canUndo" 
                quaternary
                @click="$emit('undo')"
              >
                <template #icon><n-icon><ArrowUndoOutline /></n-icon></template>
              </n-button>
            </template>
            撤销 (Ctrl+Z)
          </n-tooltip>
          <n-tooltip trigger="hover">
            <template #trigger>
              <n-button 
                :disabled="!canRedo" 
                quaternary
                @click="$emit('redo')"
              >
                <template #icon><n-icon><ArrowRedoOutline /></n-icon></template>
              </n-button>
            </template>
            重做 (Ctrl+Y)
          </n-tooltip>
          <n-divider vertical />
          <n-button @click="$emit('cancel')">
            <template #icon><n-icon><ArrowBackOutline /></n-icon></template>
            返回
          </n-button>
          <n-button type="primary" :loading="submitting" @click="$emit('submit')">
            <template #icon><n-icon><SaveOutline /></n-icon></template>
            {{ isEditMode ? '保存修改' : '创建报表' }}
          </n-button>
        </n-space>
      </div>
    </div>

    <!-- 设计器状态统计 -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="24"><ServerOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ dataSourceCount }}</span>
          <span class="stat-label">可用数据源</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="24"><ListOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ fieldsCount }}</span>
          <span class="stat-label">已配置字段</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="24"><EyeOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ visibleFieldsCount }}</span>
          <span class="stat-label">可见字段</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon" :class="sqlValidated ? 'stat-icon-success' : 'stat-icon-default'">
          <n-icon size="24"><CheckmarkCircleOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ sqlValidated ? '已验证' : '未验证' }}</span>
          <span class="stat-label">SQL状态</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  CreateOutline,
  ArrowBackOutline,
  SaveOutline,
  ServerOutline,
  ListOutline,
  EyeOutline,
  CheckmarkCircleOutline,
  ArrowUndoOutline,
  ArrowRedoOutline
} from '@vicons/ionicons5'

defineProps<{
  isEditMode: boolean
  reportName: string
  canUndo: boolean
  canRedo: boolean
  submitting: boolean
  dataSourceCount: number
  fieldsCount: number
  visibleFieldsCount: number
  sqlValidated: boolean
}>()

defineEmits<{
  undo: []
  redo: []
  cancel: []
  submit: []
}>()
</script>

<style scoped>
.designer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: var(--dp-gradient-primary, linear-gradient(135deg, #2563eb, #1e40af));
  border-radius: 12px;
  margin-bottom: 20px;
  box-shadow: 0 4px 15px var(--dp-color-primary-glow, rgba(37, 99, 235, 0.3));
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
}

.header-info {
  color: #fff;
}

.header-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
}

.header-subtitle {
  margin: 4px 0 0;
  font-size: 14px;
  opacity: 0.85;
}

.header-right :deep(.n-button) {
  background: rgba(255, 255, 255, 0.15) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  color: #fff !important;
}

.header-right :deep(.n-button:hover) {
  background: rgba(255, 255, 255, 0.25) !important;
}

.header-right :deep(.n-button--primary-type) {
  background: #fff !important;
  color: var(--color-primary, #2563eb) !important;
  border: none !important;
}

.header-right :deep(.n-button--primary-type:hover) {
  background: rgba(255, 255, 255, 0.9) !important;
  color: var(--color-primary, #2563eb) !important;
}

.header-right :deep(.n-button--quaternary-type) {
  background: transparent !important;
  border: none !important;
  color: rgba(255, 255, 255, 0.9) !important;
}

.header-right :deep(.n-button--quaternary-type:hover:not(:disabled)) {
  background: rgba(255, 255, 255, 0.2) !important;
}

.header-right :deep(.n-button--quaternary-type:disabled) {
  color: rgba(255, 255, 255, 0.4) !important;
  cursor: not-allowed;
}

.header-right :deep(.n-divider) {
  background-color: rgba(255, 255, 255, 0.3) !important;
  margin: 0 4px;
}

</style>

<style>
/* ReportDesignerToolbar 深色模式（非 scoped） */
html.dark .toolbar-container { background: #1e293b !important; border-color: #334155 !important; }
html.dark .toolbar-label { color: #94a3b8 !important; }
html.dark .toolbar-divider { background: #334155 !important; }
</style>

<template>
  <n-drawer
    v-model:show="visible"
    placement="bottom"
    :height="height || 400"
    :trap-focus="false"
    class="mobile-filter-sheet"
  >
    <n-drawer-content :body-content-style="{ padding: '16px' }">
      <template #header>
        <div class="filter-sheet-header">
          <span class="filter-sheet-title">{{ title || '筛选' }}</span>
          <n-button text size="small" type="primary" @click="handleReset">重置</n-button>
        </div>
      </template>
      <div class="filter-sheet-body">
        <slot></slot>
      </div>
      <template #footer>
        <div class="filter-sheet-footer">
          <n-button style="flex: 1;" @click="visible = false">取消</n-button>
          <n-button type="primary" style="flex: 2;" @click="handleConfirm">确认筛选</n-button>
        </div>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NDrawer, NDrawerContent, NButton } from 'naive-ui'

const props = defineProps<{
  show: boolean
  title?: string
  height?: number
}>()

const emit = defineEmits<{
  (e: 'update:show', val: boolean): void
  (e: 'confirm'): void
  (e: 'reset'): void
}>()

const visible = computed({
  get: () => props.show,
  set: (val) => emit('update:show', val)
})

const handleConfirm = () => {
  emit('confirm')
  visible.value = false
}

const handleReset = () => {
  emit('reset')
}
</script>

<style scoped>
.filter-sheet-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.filter-sheet-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary, #1e293b);
}

.filter-sheet-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-sheet-footer {
  display: flex;
  gap: 12px;
  padding-bottom: var(--mobile-safe-bottom, 0px);
}
</style>

<style>
.mobile-filter-sheet .n-drawer {
  border-radius: 20px 20px 0 0 !important;
}

.mobile-filter-sheet .n-drawer-header {
  border-bottom: 1px solid var(--border-light, #e2e8f0);
}

/* 深色模式 */
html.dark .mobile-filter-sheet .n-drawer {
  background: #1e293b !important;
}

html.dark .mobile-filter-sheet .n-drawer-header {
  border-bottom-color: #334155 !important;
  background: #1e293b !important;
}

html.dark .mobile-filter-sheet .n-drawer-body-content-wrapper {
  background: #1e293b !important;
}

html.dark .mobile-filter-sheet .n-drawer-footer {
  background: #1e293b !important;
  border-top-color: #334155 !important;
}

html.dark .mobile-filter-sheet .filter-sheet-title {
  color: #f1f5f9 !important;
}

html.dark .mobile-filter-sheet .n-button:not(.n-button--primary-type) {
  background: #334155 !important;
  border-color: #475569 !important;
  color: #e2e8f0 !important;
}

html.dark .mobile-filter-sheet .n-button:not(.n-button--primary-type):hover {
  background: #475569 !important;
}
</style>

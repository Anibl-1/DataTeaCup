<template>
  <n-drawer
    v-model:show="visible"
    placement="right"
    :width="'100%'"
    :trap-focus="false"
    class="mobile-form-drawer"
  >
    <n-drawer-content :body-content-style="{ padding: '16px' }">
      <template #header>
        <div class="form-drawer-header">
          <span class="form-drawer-title">{{ title }}</span>
        </div>
      </template>
      <div class="form-drawer-body">
        <slot></slot>
      </div>
      <template #footer>
        <div class="form-drawer-footer">
          <n-button style="flex: 1;" @click="visible = false">取消</n-button>
          <n-button type="primary" :loading="loading" style="flex: 2;" @click="$emit('submit')">
            {{ submitText || '确认' }}
          </n-button>
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
  title: string
  loading?: boolean
  submitText?: string
}>()

const emit = defineEmits<{
  (e: 'update:show', val: boolean): void
  (e: 'submit'): void
}>()

const visible = computed({
  get: () => props.show,
  set: (val) => emit('update:show', val)
})
</script>

<style scoped>
.form-drawer-header {
  display: flex;
  align-items: center;
  width: 100%;
}

.form-drawer-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary, #1e293b);
}

.form-drawer-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-drawer-footer {
  display: flex;
  gap: 12px;
  padding-bottom: var(--mobile-safe-bottom, 0px);
}

/* 深色模式 */

</style>

<style>
/* 深色模式 - 非scoped样式确保覆盖Naive UI */
html.dark .mobile-form-drawer .n-drawer {
  background: #1e293b !important;
}

html.dark .mobile-form-drawer .n-drawer-header {
  background: #1e293b !important;
  border-bottom-color: #334155 !important;
}

html.dark .mobile-form-drawer .n-drawer-body-content-wrapper {
  background: #1e293b !important;
}

html.dark .mobile-form-drawer .n-drawer-footer {
  background: #1e293b !important;
  border-top-color: #334155 !important;
}

html.dark .mobile-form-drawer .n-button:not(.n-button--primary-type) {
  background: #334155 !important;
  border-color: #475569 !important;
  color: #e2e8f0 !important;
}
</style>

<style>
/* MobileFormDrawer 深色模式（非 scoped） */
html.dark .form-drawer-header {
  background: #1e293b !important;
}
html.dark .form-drawer-title {
  color: #f1f5f9 !important;
}
html.dark .form-drawer-body {
  background: #1e293b !important;
}
html.dark .form-drawer-footer {
  background: #1e293b !important;
  border-top-color: #334155 !important;
}
</style>

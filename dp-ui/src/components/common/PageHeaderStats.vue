<template>
  <div class="page-header-stats">
    <div
      v-for="(item, index) in items"
      :key="index"
      class="stat-item"
    >
      <div class="stat-icon" :class="`stat-icon-${item.type || 'primary'}`">
        <n-icon size="24">
          <component :is="item.icon" />
        </n-icon>
      </div>
      <div class="stat-info">
        <span class="stat-value">{{ item.value }}</span>
        <span class="stat-label">{{ item.label }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import { NIcon } from 'naive-ui'

interface StatItem {
  /** 统计数值 */
  value: number | string
  /** 统计标签 */
  label: string
  /** 图标组件 */
  icon: Component
  /** 颜色类型: primary | success | warning | info | error */
  type?: 'primary' | 'success' | 'warning' | 'info' | 'error'
}

defineProps<{
  items: StatItem[]
}>()
</script>

<style scoped>
.page-header-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-light);
  transition: all 0.2s ease;
}

.stat-item:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-sm);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon-primary {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.stat-icon-success {
  background: #D1FAE5;
  color: var(--color-success);
}

.stat-icon-warning {
  background: #FEF3C7;
  color: var(--color-warning);
}

.stat-icon-info {
  background: #DBEAFE;
  color: #3B82F6;
}

.stat-icon-error {
  background: #FEE2E2;
  color: #EF4444;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 2px;
}

@media (max-width: 768px) {
  .page-header-stats {
    flex-direction: column;
  }
}
</style>

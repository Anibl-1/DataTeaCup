<template>
  <div class="m-stat-bar">
    <div
      v-for="(stat, index) in stats"
      :key="index"
      class="m-stat-chip"
      :class="stat.variant ? `m-stat-chip--${stat.variant}` : ''"
    >
      <div class="m-stat-chip-icon" :style="stat.iconBg ? { background: stat.iconBg } : {}">
        <n-icon :size="18">
          <component :is="stat.icon" />
        </n-icon>
      </div>
      <div class="m-stat-chip-info">
        <span class="m-stat-chip-value">{{ stat.value }}</span>
        <span class="m-stat-chip-label">{{ stat.label }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { type Component } from 'vue'
import { NIcon } from 'naive-ui'

interface StatItem {
  icon: Component
  label: string
  value: string | number
  variant?: 'primary' | 'success' | 'warning' | 'error' | 'info'
  iconBg?: string
}

defineProps<{
  stats: StatItem[]
}>()
</script>

<style scoped>
.m-stat-bar {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 4px;
  margin-bottom: 12px;
  scroll-snap-type: x mandatory;
}

.m-stat-bar::-webkit-scrollbar {
  display: none;
}

.m-stat-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 120px;
  flex: 1 0 auto;
  padding: 12px 14px;
  background: #fff;
  border-radius: 14px;
  border: none;
  scroll-snap-align: start;
  transition: transform 0.15s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05), 0 1px 2px rgba(0, 0, 0, 0.03);
}

.m-stat-chip:active {
  transform: scale(0.97);
}

.m-stat-chip-icon {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  flex-shrink: 0;
}

.m-stat-chip--primary .m-stat-chip-icon { background: linear-gradient(135deg, #3b82f6, #6366f1); }
.m-stat-chip--success .m-stat-chip-icon { background: linear-gradient(135deg, #10b981, #059669); }
.m-stat-chip--warning .m-stat-chip-icon { background: linear-gradient(135deg, #f59e0b, #d97706); }
.m-stat-chip--error .m-stat-chip-icon { background: linear-gradient(135deg, #ef4444, #dc2626); }
.m-stat-chip--info .m-stat-chip-icon { background: linear-gradient(135deg, #0ea5e9, #0284c7); }

.m-stat-chip-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.m-stat-chip-value {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}

.m-stat-chip-label {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 1px;
}

/* 深色模式 */


</style>

<style>
/* MobileStatBar 深色模式（非 scoped） */
html.dark .m-stat-chip {
  background: #1e293b !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important;
}
html.dark .m-stat-chip-value {
  color: #f1f5f9 !important;
}
html.dark .m-stat-chip-label {
  color: #64748b !important;
}
</style>

<template>
  <div class="left-panel" :class="{ collapsed: collapsed }">
    <div class="panel-header">
      <span v-if="!collapsed">{{ t('chart.title') }}</span>
      <n-button text @click="$emit('toggle-collapse')">
        <n-icon size="18">
          <ChevronBackOutline v-if="!collapsed" />
          <ChevronForwardOutline v-else />
        </n-icon>
      </n-button>
    </div>
    <div v-if="!collapsed" class="category-list">
      <div 
        class="category-item" 
        :class="{ active: !activeCategory }"
        @click="$emit('select-category', null)"
      >
        <n-icon size="18"><AppsOutline /></n-icon>
        <span>{{ t('common.all') }}</span>
        <n-badge :value="totalCount" :max="999" />
      </div>
      <div 
        v-for="cat in categoryList" 
        :key="cat.value"
        class="category-item"
        :class="{ active: activeCategory === cat.value }"
        @click="$emit('select-category', cat.value)"
      >
        <n-icon size="18"><component :is="cat.icon" /></n-icon>
        <span>{{ cat.label }}</span>
        <n-badge :value="cat.count" :max="999" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { NButton, NIcon, NBadge } from 'naive-ui'
import {
  ChevronBackOutline, ChevronForwardOutline, AppsOutline
} from '@vicons/ionicons5'
import { useI18n } from '@/i18n'

const { t } = useI18n()

interface CategoryItem {
  value: string
  label: string
  icon: any
  count: number
}

defineProps<{
  collapsed: boolean
  activeCategory: string | null
  totalCount: number
  categoryList: CategoryItem[]
}>()

defineEmits<{
  'toggle-collapse': []
  'select-category': [value: string | null]
}>()
</script>

<style scoped>
.left-panel {
  width: 220px;
  background: #fff;
  border-right: 1px solid #e8eaed;
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  height: 100%;
  flex-shrink: 0;
}

.left-panel.collapsed {
  width: 48px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 14px;
  font-weight: 600;
  font-size: 15px;
  color: #1e293b;
  border-bottom: 1px solid #e8eaed;
  flex-shrink: 0;
}

.left-panel.collapsed .panel-header {
  justify-content: center;
  padding: 16px 8px;
}

.category-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #64748b;
  margin-bottom: 2px;
  font-size: 14px;
}

.category-item::before {
  display: none;
}

.category-item:hover {
  background: #f1f5f9;
  color: #3b82f6;
}

.category-item.active {
  background: #eff6ff;
  color: #2563eb;
  font-weight: 600;
  border-left: 3px solid #2563eb;
  padding-left: 9px;
}

.category-item.active :deep(.n-badge) {
  --n-color: #dbeafe !important;
}

.category-item span {
  flex: 1;
}

@media (max-width: 768px) {
  .left-panel {
    display: none;
  }
}

</style>

<style>
/* ChartCategoryPanel 深色模式（非 scoped） */
html.dark .category-header { color: #e2e8f0 !important; }
html.dark .category-item { color: #94a3b8 !important; }
html.dark .category-item:hover { background: #243044 !important; }
html.dark .category-item.active { color: #818cf8 !important; background: #243044 !important; }
html.dark .category-count { color: #64748b !important; }
</style>

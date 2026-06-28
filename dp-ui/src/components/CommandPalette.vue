<template>
  <n-modal v-model:show="visible" :mask-closable="true" transform-origin="center" @after-leave="handleLeave">
    <div class="command-palette" @mousedown.stop>
      <div class="cp-header">
        <div class="cp-search-icon">
          <n-icon size="18"><SearchOutline /></n-icon>
        </div>
        <input
          ref="searchInputRef"
          v-model="searchQuery"
          class="cp-input"
          :placeholder="t('commandPalette.placeholder')"
          @keydown.esc="visible = false"
          @keydown.enter="handleSelect(filteredResults[activeIndex])"
          @keydown.up.prevent="navigateUp"
          @keydown.down.prevent="navigateDown"
        />
        <span v-if="searchQuery" class="cp-clear" @click="searchQuery = ''">✕</span>
        <kbd class="cp-kbd">ESC</kbd>
      </div>

      <n-scrollbar ref="scrollbarRef" style="max-height: 380px;">
        <div v-if="filteredResults.length === 0" class="cp-empty">
          <div class="cp-empty-icon">
            <n-icon size="36"><SearchOutline /></n-icon>
          </div>
          <span class="cp-empty-text">{{ t('commandPalette.noResults') }}</span>
          <span class="cp-empty-hint">{{ t('commandPalette.tryOther') }}</span>
        </div>
        <div v-else class="cp-list">
          <div class="cp-count">{{ t('commandPalette.found', { n: filteredResults.length }) }}</div>
          <div
            v-for="(item, index) in filteredResults"
            :key="item.key"
            :ref="el => setItemRef(el, index)"
            class="cp-item"
            :class="{ 'cp-item--active': index === activeIndex }"
            @click="handleSelect(item)"
            @mouseenter="activeIndex = index"
          >
            <div class="cp-item-icon" :class="'cp-icon-' + (item.type || 'menu')">
              <n-icon v-if="item.iconComponent" size="18" :component="item.iconComponent" />
              <n-icon v-else size="18"><DocumentTextOutline /></n-icon>
            </div>
            <div class="cp-item-info">
              <span class="cp-item-label" v-html="highlightMatch(item.label)"></span>
              <span v-if="item.path" class="cp-item-path">
                <n-icon size="12"><ArrowForwardOutline /></n-icon>
                {{ item.path }}
              </span>
            </div>
            <span class="cp-type-badge" :class="'cp-badge-' + (item.type || 'menu')">
              {{ typeLabels[item.type || 'menu'] || item.type }}
            </span>
          </div>
        </div>
      </n-scrollbar>

      <div class="cp-footer">
        <div class="cp-footer-left">
          <span><kbd>↑</kbd><kbd>↓</kbd> {{ t('commandPalette.navigate') }}</span>
          <span><kbd>↵</kbd> {{ t('commandPalette.select') }}</span>
          <span><kbd>Esc</kbd> {{ t('commandPalette.close') }}</span>
        </div>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { NIcon, NScrollbar, NModal } from 'naive-ui'
import { SearchOutline, DocumentTextOutline, ArrowForwardOutline } from '@vicons/ionicons5'
import { useI18n } from '@/i18n'

const { t } = useI18n()

interface SearchResult {
  key: string
  label: string
  path: string | undefined
  type: string | undefined
  iconComponent: any
  routePath: string | undefined
  reportId: number | undefined
  chartId: number | undefined
  pageId: number | undefined
  dataViewCode: string | undefined
  onClick: (() => void) | undefined
}

const props = defineProps<{
  menuOptions: any[]
}>()

const visible = defineModel<boolean>({ default: false })

const router = useRouter()
const searchQuery = ref('')
const activeIndex = ref(0)
const searchInputRef = ref<HTMLInputElement | null>(null)
const scrollbarRef = ref<InstanceType<typeof NScrollbar> | null>(null)
const itemRefs = ref<(HTMLElement | null)[]>([])

const setItemRef = (el: any, index: number) => {
  if (el) itemRefs.value[index] = el as HTMLElement
}

const typeLabels: Record<string, string> = {
  menu: '菜单',
  report: '报表',
  chart: '图表',
  page: '页面'
}

// Flatten menu tree into searchable list, SKIP directory items
const flattenMenus = (menus: any[], parentPath: string = ''): SearchResult[] => {
  const results: SearchResult[] = []
  for (const menu of menus) {
    const label = typeof menu.label === 'function' ? menu.key : menu.label
    const currentPath = parentPath ? `${parentPath} / ${label}` : label
    const isDirectory = menu.children?.length > 0

    // Only add leaf (navigable) items — skip directories
    if (!isDirectory) {
      // Determine type
      let itemType = 'menu'
      if (menu.reportId) itemType = 'report'
      else if (menu.chartId) itemType = 'chart'
      else if (menu.pageId) itemType = 'page'

      results.push({
        key: menu.key,
        label: label || menu.key,
        path: parentPath || undefined,
        type: itemType,
        iconComponent: typeof menu.icon === 'function' ? undefined : menu.icon,
        routePath: menu.routePath,
        reportId: menu.reportId,
        chartId: menu.chartId,
        pageId: menu.pageId,
        dataViewCode: menu.dataViewCode,
        onClick: menu.onClick
      })
    }

    // Recurse into children
    if (isDirectory) {
      results.push(...flattenMenus(menu.children, currentPath))
    }
  }
  return results
}

const allResults = computed(() => flattenMenus(props.menuOptions))

const filteredResults = computed(() => {
  // Only show navigable items (have route, entity ID, or onClick)
  const navigable = allResults.value.filter(item =>
    item.routePath || item.reportId || item.chartId || item.pageId || item.dataViewCode || item.onClick
  )
  if (!searchQuery.value) return navigable.slice(0, 20)
  const kw = searchQuery.value.toLowerCase()
  return navigable.filter(item =>
    item.label.toLowerCase().includes(kw) ||
    item.key.toLowerCase().includes(kw) ||
    (item.path && item.path.toLowerCase().includes(kw))
  ).slice(0, 30)
})

// Keyword highlight
const highlightMatch = (text: string): string => {
  if (!searchQuery.value) return text
  const kw = searchQuery.value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${kw})`, 'gi')
  return text.replace(regex, '<mark class="cp-highlight">$1</mark>')
}

// Arrow key navigation with scroll into view
const navigateUp = () => {
  activeIndex.value = Math.max(0, activeIndex.value - 1)
  nextTick(() => scrollActiveIntoView())
}

const navigateDown = () => {
  activeIndex.value = Math.min(filteredResults.value.length - 1, activeIndex.value + 1)
  nextTick(() => scrollActiveIntoView())
}

const scrollActiveIntoView = () => {
  const el = itemRefs.value[activeIndex.value]
  if (el) el.scrollIntoView({ block: 'nearest' })
}

watch(searchQuery, () => {
  activeIndex.value = 0
})

watch(visible, (val) => {
  if (val) {
    nextTick(() => {
      searchInputRef.value?.focus()
    })
  }
})

const handleLeave = () => {
  searchQuery.value = ''
  activeIndex.value = 0
  itemRefs.value = []
}

const handleSelect = (item: SearchResult | undefined) => {
  if (!item) return
  visible.value = false

  if (item.onClick) {
    item.onClick()
  } else if (item.routePath) {
    router.push(item.routePath)
  } else if (item.reportId) {
    router.push(`/report-view/${item.reportId}`)
  } else if (item.chartId) {
    router.push(`/chart-center/${item.chartId}`)
  } else if (item.pageId) {
    router.push(`/page-view/${item.pageId}`)
  } else if (item.dataViewCode) {
    router.push(`/data-view/${item.dataViewCode}`)
  }
}
</script>

<style scoped>
.command-palette {
  width: 580px;
  max-width: 95vw;
  background: #fff;
  border-radius: 16px;
  box-shadow:
    0 0 0 1px rgba(0, 0, 0, 0.03),
    0 8px 20px rgba(0, 0, 0, 0.06),
    0 24px 56px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  animation: cp-slide-in 0.18s ease-out;
}

@keyframes cp-slide-in {
  from { opacity: 0; transform: translateY(-12px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* ===== Header ===== */
.cp-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.cp-search-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cp-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  color: #1e293b;
  background: transparent;
  min-width: 0;
}

.cp-input::placeholder { color: #94a3b8; }

.cp-clear {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #e2e8f0;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s;
}

.cp-clear:hover { background: #cbd5e1; }

.cp-kbd {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 5px;
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #e2e8f0;
  font-family: inherit;
  flex-shrink: 0;
}

/* ===== Empty state ===== */
.cp-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 48px 20px;
}

.cp-empty-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #cbd5e1;
  margin-bottom: 4px;
}

.cp-empty-text { font-size: 14px; font-weight: 500; color: #64748b; }
.cp-empty-hint { font-size: 12px; color: #94a3b8; }

/* ===== List ===== */
.cp-list { padding: 6px 6px 2px; }

.cp-count {
  font-size: 11px;
  color: #94a3b8;
  padding: 2px 8px 6px;
  font-weight: 500;
}

.cp-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.12s ease;
  margin-bottom: 2px;
}

.cp-item:hover { background: #f8fafc; }
.cp-item--active { background: #eff6ff !important; }

/* ===== Item icon - color by type ===== */
.cp-item-icon {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.12s;
}

.cp-icon-menu { background: #f0fdf4; color: #16a34a; }
.cp-icon-report { background: #fffbeb; color: #d97706; }
.cp-icon-chart { background: #eff6ff; color: #2563eb; }
.cp-icon-page { background: #faf5ff; color: #7c3aed; }

.cp-item--active .cp-icon-menu { background: #dcfce7; }
.cp-item--active .cp-icon-report { background: #fef3c7; }
.cp-item--active .cp-icon-chart { background: #dbeafe; }
.cp-item--active .cp-icon-page { background: #ede9fe; }

/* ===== Item info ===== */
.cp-item-info { flex: 1; min-width: 0; }

.cp-item-label {
  font-size: 13.5px;
  font-weight: 500;
  color: #1e293b;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cp-item-path {
  font-size: 11.5px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 3px;
  margin-top: 1px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== Type badge ===== */
.cp-type-badge {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 6px;
  flex-shrink: 0;
  letter-spacing: 0.3px;
}

.cp-badge-menu { background: #f0fdf4; color: #16a34a; }
.cp-badge-report { background: #fffbeb; color: #d97706; }
.cp-badge-chart { background: #eff6ff; color: #2563eb; }
.cp-badge-page { background: #faf5ff; color: #7c3aed; }

/* ===== Footer ===== */
.cp-footer {
  display: flex;
  align-items: center;
  padding: 9px 16px;
  border-top: 1px solid #f1f5f9;
}

.cp-footer-left {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
  color: #94a3b8;
}

.cp-footer kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  font-size: 11px;
  padding: 0 4px;
  border-radius: 4px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #64748b;
  font-family: inherit;
  margin-right: 3px;
}

/* ===== Highlight ===== */
:deep(.cp-highlight) {
  background: rgba(37, 99, 235, 0.12);
  color: #2563eb;
  border-radius: 2px;
  padding: 0 1px;
}

/* ===== Responsive ===== */
@media (max-width: 640px) {
  .command-palette { border-radius: 12px; }
  .cp-header { padding: 12px 14px; }
  .cp-item { padding: 8px; }
}
</style>

<style>
/* CommandPalette 深色模式（非 scoped） */
html.dark .command-palette {
  background: #1e293b !important;
  box-shadow: 0 0 0 1px rgba(255,255,255,0.06), 0 24px 56px rgba(0,0,0,0.5) !important;
}
html.dark .cp-header { border-bottom-color: rgba(255,255,255,0.06) !important; }
html.dark .cp-input { color: #e2e8f0 !important; }
html.dark .cp-clear { background: #334155 !important; color: #94a3b8 !important; }
html.dark .cp-kbd { background: #334155 !important; color: #94a3b8 !important; border-color: #475569 !important; }
html.dark .cp-item:hover { background: rgba(255,255,255,0.04) !important; }
html.dark .cp-item--active { background: rgba(59, 130, 246, 0.12) !important; }
html.dark .cp-icon-menu { background: rgba(22,163,74,0.15) !important; }
html.dark .cp-icon-report { background: rgba(217,119,6,0.15) !important; }
html.dark .cp-icon-chart { background: rgba(37,99,235,0.15) !important; }
html.dark .cp-icon-page { background: rgba(124,58,237,0.15) !important; }
html.dark .cp-item-label { color: #e2e8f0 !important; }
html.dark .cp-badge-menu { background: rgba(22,163,74,0.12) !important; color: #4ade80 !important; }
html.dark .cp-badge-report { background: rgba(217,119,6,0.12) !important; color: #fbbf24 !important; }
html.dark .cp-badge-chart { background: rgba(37,99,235,0.12) !important; color: #60a5fa !important; }
html.dark .cp-badge-page { background: rgba(124,58,237,0.12) !important; color: #a78bfa !important; }
html.dark .cp-empty-icon { background: #334155 !important; }
html.dark .cp-empty-text { color: #94a3b8 !important; }
html.dark .cp-footer { border-top-color: rgba(255,255,255,0.06) !important; }
html.dark .cp-footer kbd { background: #334155 !important; border-color: #475569 !important; }
html.dark .cp-highlight { background: rgba(96,165,250,0.2) !important; color: #60a5fa !important; }
</style>

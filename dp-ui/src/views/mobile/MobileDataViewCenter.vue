<template>
  <MobilePageShell @refresh="loadDataViews">
    <!-- 统计栏 -->
    <MobileStatBar :stats="statItems" />

    <!-- 搜索栏 -->
    <div class="search-bar">
      <n-input
        v-model:value="searchKeyword"
        placeholder="搜索数据管理"
        clearable
        round
        size="small"
      >
        <template #prefix>
          <n-icon size="16" color="#94a3b8"><SearchOutline /></n-icon>
        </template>
      </n-input>
    </div>

    <!-- 筛选标签 -->
    <div class="filter-tabs">
      <div
        class="filter-tab"
        :class="{ active: activeFilter === 'all' }"
        @click="activeFilter = 'all'"
      >
        全部
        <span class="filter-tab-count">{{ allItems.length }}</span>
      </div>
      <div
        class="filter-tab"
        :class="{ active: activeFilter === 'editable' }"
        @click="activeFilter = 'editable'"
      >
        可编辑
        <span class="filter-tab-count">{{ editableCount }}</span>
      </div>
      <div
        class="filter-tab"
        :class="{ active: activeFilter === 'readonly' }"
        @click="activeFilter = 'readonly'"
      >
        只读
        <span class="filter-tab-count">{{ readonlyCount }}</span>
      </div>
      <div class="filter-spacer"></div>
      <div class="sort-toggle" @click="toggleSort">
        <n-icon size="14"><SwapVerticalOutline /></n-icon>
        <span>{{ sortLabel }}</span>
      </div>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="dv-list">
      <div v-for="i in 4" :key="i" class="dv-item skeleton-card">
        <div class="dv-item-icon skeleton-shimmer"></div>
        <div class="dv-item-body">
          <n-skeleton text style="width: 60%" :sharp="false" />
          <n-skeleton text style="width: 40%" size="small" :sharp="false" />
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <MobileEmpty
      v-else-if="displayItems.length === 0"
      :type="searchKeyword ? 'search' : 'data'"
      :title="searchKeyword ? '未找到匹配项' : '暂无数据管理'"
      :description="searchKeyword ? '没有找到与搜索关键词匹配的数据管理' : '请先在管理端创建并启用数据管理'"
    >
      <template v-if="searchKeyword" #action>
        <n-button size="small" @click="searchKeyword = ''">清除搜索</n-button>
      </template>
    </MobileEmpty>

    <!-- 列表 -->
    <div v-else class="dv-list">
      <div
        v-for="(item, idx) in displayItems"
        :key="item.code"
        class="dv-item"
        :style="{ animationDelay: `${Math.min(idx * 0.04, 0.3)}s` }"
        @click="openDataView(item)"
      >
        <div class="dv-item-icon" :style="{ background: getGradient(idx) }">
          <n-icon size="20" color="#fff"><ReaderOutline /></n-icon>
        </div>
        <div class="dv-item-body">
          <div class="dv-item-title">{{ item.name }}</div>
          <div class="dv-item-meta">
            <n-tag
              :type="item.allowUpdate || item.allowInsert ? 'success' : 'default'"
              size="tiny"
              :bordered="false"
              round
            >
              {{ item.allowUpdate || item.allowInsert ? '可编辑' : '只读' }}
            </n-tag>
            <span v-if="item.description" class="meta-desc">{{ item.description }}</span>
            <span v-else class="meta-table">{{ item.tableName }}</span>
          </div>
        </div>
        <div class="dv-item-badges">
          <span v-if="item.allowInsert" class="badge badge-add" title="可新增">+</span>
          <span v-if="item.allowDelete" class="badge badge-del" title="可删除">&minus;</span>
        </div>
        <div class="dv-item-arrow">
          <n-icon size="16"><ChevronForwardOutline /></n-icon>
        </div>
      </div>
    </div>
  </MobilePageShell>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NInput, NIcon, NButton, NSkeleton, NTag } from 'naive-ui'
import { SearchOutline, ReaderOutline, ChevronForwardOutline, SwapVerticalOutline, LayersOutline, CreateOutline, EyeOutline } from '@vicons/ionicons5'
import MobilePageShell from '@/components/mobile/MobilePageShell.vue'
import MobileStatBar from '@/components/mobile/MobileStatBar.vue'
import MobileEmpty from '@/components/mobile/MobileEmpty.vue'
import * as dataViewApi from '@/api/dataView'

interface DataViewItem {
  id: number
  name: string
  code: string
  tableName: string
  description?: string
  status: number
  allowQuery: number
  allowInsert: number
  allowUpdate: number
  allowDelete: number
}

const router = useRouter()
const searchKeyword = ref('')
const loading = ref(false)
const allItems = ref<DataViewItem[]>([])
const activeFilter = ref<'all' | 'editable' | 'readonly'>('all')
const sortMode = ref<'name' | 'table'>('name')

const sortLabel = computed(() => sortMode.value === 'name' ? '名称' : '表名')

function toggleSort() {
  sortMode.value = sortMode.value === 'name' ? 'table' : 'name'
}

const editableCount = computed(() => allItems.value.filter(i => i.allowUpdate || i.allowInsert).length)
const readonlyCount = computed(() => allItems.value.filter(i => !i.allowUpdate && !i.allowInsert).length)

const gradients = [
  'linear-gradient(135deg, #f59e0b, #d97706)',
  'linear-gradient(135deg, #3b82f6, #2563eb)',
  'linear-gradient(135deg, #10b981, #059669)',
  'linear-gradient(135deg, #8b5cf6, #7c3aed)',
  'linear-gradient(135deg, #ec4899, #f43f5e)',
  'linear-gradient(135deg, #0ea5e9, #0284c7)',
  'linear-gradient(135deg, #14b8a6, #0d9488)'
]

function getGradient(idx: number): string {
  return gradients[idx % gradients.length] ?? gradients[0]!
}

const statItems = computed(() => [
  { icon: LayersOutline, label: '全部', value: allItems.value.length, variant: 'primary' as const, iconBg: 'linear-gradient(135deg, #f59e0b, #d97706)' },
  { icon: CreateOutline, label: '可编辑', value: editableCount.value, variant: 'success' as const, iconBg: 'linear-gradient(135deg, #10b981, #059669)' },
  { icon: EyeOutline, label: '只读', value: readonlyCount.value, variant: 'info' as const, iconBg: 'linear-gradient(135deg, #0ea5e9, #0284c7)' }
])

const filteredItems = computed(() => {
  let list = allItems.value
  if (activeFilter.value === 'editable') {
    list = list.filter(i => i.allowUpdate || i.allowInsert)
  } else if (activeFilter.value === 'readonly') {
    list = list.filter(i => !i.allowUpdate && !i.allowInsert)
  }
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(i =>
      i.name.toLowerCase().includes(kw) ||
      i.code.toLowerCase().includes(kw) ||
      i.tableName.toLowerCase().includes(kw) ||
      (i.description && i.description.toLowerCase().includes(kw))
    )
  }
  return list
})

const displayItems = computed(() => {
  const list = [...filteredItems.value]
  if (sortMode.value === 'table') {
    list.sort((a, b) => a.tableName.localeCompare(b.tableName, 'zh-CN'))
  } else {
    list.sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
  }
  return list
})

async function loadDataViews() {
  loading.value = true
  try {
    const res = await dataViewApi.getDataViewList()
    const raw: any[] = res.data || []
    allItems.value = raw
      .filter((item: any) => item.status === 1)
      .map((item: any) => ({
        id: item.id,
        name: item.name || item.menuName || '未命名',
        code: item.code,
        tableName: item.tableName || '',
        description: item.description || '',
        status: item.status,
        allowQuery: item.allowQuery ?? 1,
        allowInsert: item.allowInsert ?? 0,
        allowUpdate: item.allowUpdate ?? 0,
        allowDelete: item.allowDelete ?? 0
      }))
  } catch (e) {
    console.error('[MobileDataViewCenter] 加载失败', e)
  } finally {
    loading.value = false
  }
}

function openDataView(item: DataViewItem) {
  router.push(`/m/data-view/${item.code}`)
}

onMounted(() => { loadDataViews() })
</script>

<style scoped>
.search-bar { margin-bottom: 8px; }

/* 筛选标签 */
.filter-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  padding: 0 2px;
}

.filter-tab {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  background: rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.2s ease;
  -webkit-tap-highlight-color: transparent;
  user-select: none;
}

.filter-tab.active {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #fff;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.3);
}

.filter-tab-count {
  font-size: 11px;
  font-weight: 600;
  min-width: 18px;
  text-align: center;
  padding: 0 4px;
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.06);
  line-height: 18px;
}

.filter-tab.active .filter-tab-count {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

.filter-spacer { flex: 1; }

.sort-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 12px;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.2s;
  -webkit-tap-highlight-color: transparent;
}

.sort-toggle:active {
  background: rgba(0, 0, 0, 0.06);
}

/* 列表 */
.dv-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.dv-item {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  border-radius: 16px;
  padding: 14px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05), 0 1px 2px rgba(0, 0, 0, 0.03);
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
  -webkit-tap-highlight-color: transparent;
  animation: cardSlideIn 0.3s ease backwards;
}

.dv-item:active {
  transform: scale(0.97);
  box-shadow: 0 0 0 rgba(0, 0, 0, 0);
}

@keyframes cardSlideIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.dv-item-icon {
  flex-shrink: 0;
  width: 46px;
  height: 46px;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dv-item-body {
  flex: 1;
  min-width: 0;
}

.dv-item-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 5px;
  line-height: 1.3;
}

.dv-item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #94a3b8;
}

.meta-desc, .meta-table {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.meta-table {
  font-family: 'SF Mono', 'Menlo', monospace;
  font-size: 11px;
  color: #94a3b8;
}

.dv-item-badges {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
}

.badge-add {
  background: rgba(16, 185, 129, 0.12);
  color: #059669;
}

.badge-del {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.dv-item-arrow {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  color: #cbd5e1;
  transition: transform 0.2s ease;
}

.dv-item:active .dv-item-arrow {
  transform: translateX(3px);
}

/* 骨架屏 */
.skeleton-card { pointer-events: none; }
.skeleton-card .dv-item-icon { background: none !important; }
.skeleton-shimmer {
  width: 46px; height: 46px; border-radius: 13px;
  background: linear-gradient(90deg, #e2e8f0 25%, #f1f5f9 50%, #e2e8f0 75%) !important;
  background-size: 200% 100% !important;
  animation: shimmer 1.5s infinite;
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 深色模式 - 筛选标签 */

/* 深色模式 - 搜索框 */

/* 深色模式 - 骨架屏 */
</style>

<style>
/* MobileDataViewCenter 深色模式（非 scoped） */
html.dark .dv-item { background: #1e293b !important; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important; }
html.dark .dv-item:active { background: #263449 !important; }
html.dark .dv-item-title { color: #e2e8f0 !important; }
html.dark .dv-item-meta { color: #64748b !important; }
html.dark .meta-desc, html.dark .meta-table { color: #64748b !important; }
html.dark .dv-item-arrow { color: #475569 !important; }
html.dark .badge-add {
  background: rgba(16, 185, 129, 0.15) !important;
  color: #34d399 !important;
}
html.dark .badge-del {
  background: rgba(239, 68, 68, 0.15) !important;
  color: #f87171 !important;
}
html.dark .filter-tab {
  background: rgba(255, 255, 255, 0.06) !important;
  color: #94a3b8 !important;
}
html.dark .filter-tab.active {
  background: linear-gradient(135deg, #f59e0b, #d97706) !important;
  color: #fff !important;
}
html.dark .filter-tab-count {
  background: rgba(255, 255, 255, 0.08) !important;
}
html.dark .filter-tab.active .filter-tab-count {
  background: rgba(255, 255, 255, 0.25) !important;
}
html.dark .sort-toggle { color: #64748b !important; }
html.dark .sort-toggle:active { background: rgba(255, 255, 255, 0.06) !important; }
html.dark .search-bar .n-input {
  background: #1e293b !important;
  border-color: #334155 !important;
}
html.dark .search-bar .n-input .n-input__input-el {
  color: #e2e8f0 !important;
  caret-color: #60a5fa !important;
}
html.dark .search-bar .n-input .n-input__placeholder {
  color: #475569 !important;
}
html.dark .search-bar .n-input .n-input__suffix .n-icon,
html.dark .search-bar .n-input .n-input__suffix .n-icon,
html.dark .search-bar .n-input .n-input__prefix .n-icon {
  color: #475569 !important;
}
html.dark .skeleton-shimmer {
  background: linear-gradient(90deg, #334155 25%, #475569 50%, #334155 75%) !important;
  background-size: 200% 100% !important;
}
</style>

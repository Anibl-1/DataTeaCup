<template>
  <MobilePageShell @refresh="loadItems">
    <!-- 统计栏 -->
    <MobileStatBar :stats="statItems" />

    <!-- 搜索栏 -->
    <div class="search-bar">
      <n-input
        v-model:value="searchKeyword"
        placeholder="搜索图表、页面或数据"
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
      <div class="filter-tabs-scroll">
        <div
          v-for="tab in filterTabs"
          :key="tab.key"
          class="filter-tab"
          :class="{ active: activeFilter === tab.key }"
          @click="activeFilter = tab.key"
        >
          {{ tab.label }}
          <span class="filter-tab-count">{{ tab.count }}</span>
        </div>
      </div>
      <div class="sort-toggle" @click="toggleSort">
        <n-icon size="14"><SwapVerticalOutline /></n-icon>
        <span>{{ sortLabel }}</span>
      </div>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="item-list">
      <div v-for="i in 4" :key="i" class="item-card skeleton-card">
        <div class="item-card-icon skeleton-shimmer"></div>
        <div class="item-card-body">
          <n-skeleton text style="width: 60%" :sharp="false" />
          <n-skeleton text style="width: 40%" size="small" :sharp="false" />
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <MobileEmpty
      v-else-if="displayItems.length === 0"
      :type="searchKeyword ? 'search' : 'data'"
      :title="searchKeyword ? '未找到匹配项目' : '暂无可用内容'"
      :description="emptyDescription"
    >
      <template v-if="searchKeyword" #action>
        <n-button size="small" @click="searchKeyword = ''">清除搜索</n-button>
      </template>
    </MobileEmpty>

    <!-- 列表 -->
    <div v-else class="item-list">
      <div
        v-for="(item, idx) in displayItems"
        :key="`${item.type}-${item.id}`"
        class="item-card"
        :style="{ animationDelay: `${Math.min(idx * 0.04, 0.3)}s` }"
        @click="viewItem(item)"
      >
        <div class="item-card-icon" :style="{ background: getItemGradient(item) }">
          <n-icon size="20" color="#fff">
            <GridOutline v-if="item.type === 'page'" />
            <ReaderOutline v-else-if="item.type === 'dataview'" />
            <BarChartOutline v-else />
          </n-icon>
        </div>
        <div class="item-card-body">
          <div class="item-card-title">{{ item.name }}</div>
          <div class="item-card-meta">
            <n-tag :type="getTagType(item.type)" size="tiny" :bordered="false" round>
              {{ getTypeLabel(item.type) }}
            </n-tag>
            <span v-if="item.updateTime" class="meta-time">{{ formatTime(item.updateTime) }}</span>
            <span v-else-if="item.description" class="meta-desc">{{ item.description }}</span>
          </div>
        </div>
        <div class="item-card-arrow">
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
import { SearchOutline, BarChartOutline, GridOutline, ReaderOutline, LayersOutline, ChevronForwardOutline, SwapVerticalOutline } from '@vicons/ionicons5'
import MobilePageShell from '@/components/mobile/MobilePageShell.vue'
import MobileStatBar from '@/components/mobile/MobileStatBar.vue'
import MobileEmpty from '@/components/mobile/MobileEmpty.vue'
import { getVisibleMenus } from '@/api/system/menu'
import { getPageDefinitionList } from '@/api/page'
import { getChartDefinitionList } from '@/api/chart'
import * as dataViewApi from '@/api/dataView'
import type { PageDefinition } from '@/types/page'
import type { ChartDefinition } from '@/types/chart'
import type { Menu } from '@/types/menu'

interface MobileListItem {
  id: number | string
  name: string
  description?: string
  type: 'page' | 'chart' | 'dataview'
  updateTime?: string
  code?: string
}

const router = useRouter()
const searchKeyword = ref('')
const loading = ref(false)
const itemList = ref<MobileListItem[]>([])
const activeFilter = ref<'all' | 'chart' | 'page' | 'dataview'>('all')
const sortMode = ref<'name' | 'time'>('name')

const isCanceledRequest = (error: any) => {
  return error?.code === 'ERR_CANCELED' || error?.name === 'CanceledError' || error?.message === 'canceled'
}

const filterTabs = computed(() => [
  { key: 'all' as const, label: '全部', count: itemList.value.length },
  { key: 'chart' as const, label: '图表', count: itemList.value.filter(i => i.type === 'chart').length },
  { key: 'page' as const, label: '页面', count: itemList.value.filter(i => i.type === 'page').length },
  { key: 'dataview' as const, label: '数据', count: itemList.value.filter(i => i.type === 'dataview').length }
])

const sortLabel = computed(() => sortMode.value === 'name' ? '名称' : '时间')

function toggleSort() {
  sortMode.value = sortMode.value === 'name' ? 'time' : 'name'
}

function formatTime(t: string): string {
  if (!t) return ''
  // 只显示月-日
  const d = new Date(t)
  if (isNaN(d.getTime())) return t.slice(5, 10)
  return `${(d.getMonth() + 1).toString().padStart(2, '0')}-${d.getDate().toString().padStart(2, '0')}`
}

const gradients = [
  'linear-gradient(135deg, #3b82f6, #6366f1)',
  'linear-gradient(135deg, #10b981, #059669)',
  'linear-gradient(135deg, #f59e0b, #d97706)',
  'linear-gradient(135deg, #8b5cf6, #7c3aed)',
  'linear-gradient(135deg, #ec4899, #f43f5e)',
  'linear-gradient(135deg, #0ea5e9, #0284c7)',
  'linear-gradient(135deg, #14b8a6, #0d9488)'
]

function getItemGradient(item: MobileListItem): string {
  const numId = typeof item.id === 'number' ? item.id : (parseInt(String(item.id), 10) || 0)
  return gradients[numId % gradients.length] ?? gradients[0]!
}

const statItems = computed(() => [
  { icon: LayersOutline, label: '全部项目', value: itemList.value.length, variant: 'primary' as const, iconBg: 'linear-gradient(135deg, #3b82f6, #6366f1)' },
  { icon: BarChartOutline, label: '图表', value: itemList.value.filter(i => i.type === 'chart').length, variant: 'success' as const, iconBg: 'linear-gradient(135deg, #10b981, #059669)' },
  { icon: GridOutline, label: '页面', value: itemList.value.filter(i => i.type === 'page').length, variant: 'info' as const, iconBg: 'linear-gradient(135deg, #0ea5e9, #0284c7)' },
  { icon: ReaderOutline, label: '数据', value: itemList.value.filter(i => i.type === 'dataview').length, variant: 'warning' as const, iconBg: 'linear-gradient(135deg, #f59e0b, #d97706)' }
])

function getTagType(type: string): 'success' | 'info' | 'warning' | 'default' {
  if (type === 'chart') return 'success'
  if (type === 'page') return 'info'
  if (type === 'dataview') return 'warning'
  return 'default'
}

function getTypeLabel(type: string): string {
  if (type === 'chart') return '图表'
  if (type === 'page') return '页面'
  if (type === 'dataview') return '数据'
  return '未知'
}

const emptyDescription = computed(() => {
  if (searchKeyword.value) return '没有找到与 "' + searchKeyword.value + '" 相关的项目'
  return '请先在管理端发布图表、页面或数据管理到移动端'
})

const filteredItems = computed(() => {
  let list = itemList.value
  if (activeFilter.value !== 'all') {
    list = list.filter(i => i.type === activeFilter.value)
  }
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(item =>
      item.name.toLowerCase().includes(kw) || (item.description && item.description.toLowerCase().includes(kw))
    )
  }
  return list
})

const displayItems = computed(() => {
  const list = [...filteredItems.value]
  if (sortMode.value === 'time') {
    list.sort((a, b) => {
      const ta = a.updateTime || ''
      const tb = b.updateTime || ''
      return tb.localeCompare(ta)
    })
  } else {
    list.sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
  }
  return list
})

// 从菜单树中递归提取所有关联了页面或图表的ID（包括从routePath解析）
function extractPermittedIds(menus: Menu[]): { pageIds: Set<number>, chartIds: Set<number> } {
  const pageIds = new Set<number>()
  const chartIds = new Set<number>()
  function walk(list: Menu[]) {
    for (const m of list) {
      if (m.pageId) pageIds.add(m.pageId)
      if (m.chartId) chartIds.add(m.chartId)
      // 从routePath中解析图表/页面ID，如 /chart-center/5, /page-view/3, /chart-view/10
      if (m.routePath) {
        const chartMatch = m.routePath.match(/\/chart[-_](?:center|view|embed)\/?(\d+)/i)
        if (chartMatch) chartIds.add(Number(chartMatch[1]))
        const pageMatch = m.routePath.match(/\/page[-_]?(?:view|definition)\/?(\d+)/i)
        if (pageMatch) pageIds.add(Number(pageMatch[1]))
      }
      if (m.children?.length) walk(m.children)
    }
  }
  walk(menus)
  return { pageIds, chartIds }
}

async function loadItems() {
  loading.value = true
  try {
    // 1. 获取用户有权限的菜单，提取关联的pageId和chartId
    const menuRes = await getVisibleMenus() as any
    const rawMenus = menuRes?.data?.data || menuRes?.data || []
    const menus: Menu[] = Array.isArray(rawMenus) ? rawMenus : []
    const { pageIds, chartIds } = extractPermittedIds(menus)
    const hasMenuPermission = menus.length > 0
    const items: MobileListItem[] = []

    // 2. 获取所有页面，过滤：status=1 && (mobileEnabled=1 或有菜单权限)
    try {
      const pageRes = await getPageDefinitionList({ page: 1, pageSize: 1000 }) as any
      const pageData = pageRes?.data || pageRes
      const pages: PageDefinition[] = pageData?.list || pageData?.records || (Array.isArray(pageData) ? pageData : [])
      for (const p of pages) {
        if (!p.id || p.status !== 1) continue
        // 显示条件：mobileEnabled=1（显式发布到移动端），或者有菜单权限关联
        const isMobilePublished = p.mobileEnabled === 1
        const hasPermission = pageIds.has(p.id)
        if (isMobilePublished || (hasPermission && hasMenuPermission)) {
          items.push({ id: p.id, name: p.pageName || '未命名页面', description: p.description || '', type: 'page', updateTime: (p as any).updateTime || '' })
        }
      }
    } catch (e) {
      console.warn('[MobileChartCenter] 获取页面列表失败:', e)
    }

    // 3. 获取所有图表，过滤：status=1 && (mobileEnabled=1 或有菜单权限)
    try {
      const chartRes = await getChartDefinitionList({ page: 1, pageSize: 1000 }) as any
      const chartData = chartRes?.data || chartRes
      const charts: ChartDefinition[] = chartData?.list || chartData?.records || (Array.isArray(chartData) ? chartData : [])
      for (const c of charts) {
        if (!c.id || c.status !== 1) continue
        // 显示条件：mobileEnabled=1（显式发布到移动端），或者有菜单权限关联
        const isMobilePublished = c.mobileEnabled === 1
        const hasPermission = chartIds.has(c.id)
        if (isMobilePublished || (hasPermission && hasMenuPermission)) {
          items.push({ id: c.id, name: c.chartName || '未命名图表', description: c.description || '', type: 'chart', updateTime: (c as any).updateTime || '' })
        }
      }
    } catch (e) {
      console.warn('[MobileChartCenter] 获取图表列表失败:', e)
    }

    // 4. 获取数据管理列表，过滤 status=1
    try {
      const dvRes = await dataViewApi.getDataViewList() as any
      const dvList: any[] = dvRes?.data || []
      for (const dv of dvList) {
        if (dv.status !== 1) continue
        items.push({
          id: dv.id,
          name: dv.name || dv.menuName || '未命名数据',
          description: dv.tableName || '',
          type: 'dataview',
          updateTime: dv.updateTime || '',
          code: dv.code
        })
      }
    } catch (e) {
      console.warn('[MobileChartCenter] 获取数据管理列表失败:', e)
    }

    itemList.value = items
  } catch (e) {
    if (!isCanceledRequest(e)) {
      console.error('[MobileChartCenter] 加载失败', e)
    }
  } finally {
    loading.value = false
  }
}

function viewItem(item: MobileListItem) {
  if (item.type === 'page') {
    router.push(`/m/page/${item.id}`)
  } else if (item.type === 'dataview') {
    router.push(`/m/data-view/${item.code || item.id}`)
  } else {
    router.push(`/m/chart/${item.id}`)
  }
}

onMounted(() => { loadItems() })
</script>

<style scoped>
.search-bar { margin-bottom: 8px; }

/* 筛选标签 */
.filter-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 0 2px;
}

.filter-tabs-scroll {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}

.filter-tabs-scroll::-webkit-scrollbar { display: none; }

.filter-tab {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 5px 12px;
  border-radius: 18px;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  background: rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.2s ease;
  -webkit-tap-highlight-color: transparent;
  user-select: none;
  white-space: nowrap;
  flex-shrink: 0;
}

.filter-tab.active {
  background: #3b82f6;
  color: #fff;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.filter-tab-count {
  font-size: 11px;
  font-weight: 600;
  min-width: 16px;
  text-align: center;
  padding: 0 3px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.06);
  line-height: 16px;
}

.filter-tab.active .filter-tab-count {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

.sort-toggle {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 5px 8px;
  border-radius: 8px;
  font-size: 12px;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.2s;
  -webkit-tap-highlight-color: transparent;
  flex-shrink: 0;
  white-space: nowrap;
}

.sort-toggle:active {
  background: rgba(0, 0, 0, 0.06);
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.item-card {
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
.item-card:active {
  transform: scale(0.97);
  box-shadow: 0 0 0 rgba(0, 0, 0, 0);
}

@keyframes cardSlideIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.item-card-icon {
  flex-shrink: 0;
  width: 46px;
  height: 46px;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-card-body {
  flex: 1;
  min-width: 0;
}

.item-card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 5px;
  line-height: 1.3;
}

.item-card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #94a3b8;
}

.meta-desc {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.meta-time {
  font-size: 11px;
  color: #94a3b8;
}

.item-card-arrow {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  color: #cbd5e1;
  transition: transform 0.2s ease;
}

.item-card:active .item-card-arrow {
  transform: translateX(3px);
}

/* 骨架屏 */
.skeleton-card { pointer-events: none; }
.skeleton-card .item-card-icon { background: none !important; }
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
</style>

<style>
/* MobileChartCenter 深色模式（非 scoped） */
html.dark .item-card { background: #1e293b !important; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important; }
html.dark .item-card:active { background: #263449 !important; }
html.dark .item-card-title { color: #e2e8f0 !important; }
html.dark .item-card-meta { color: #64748b !important; }
html.dark .meta-desc { color: #64748b !important; }
html.dark .item-card-arrow { color: #475569 !important; }
html.dark .filter-tab {
  background: rgba(255, 255, 255, 0.06) !important;
  color: #94a3b8 !important;
}
html.dark .filter-tab.active {
  background: #3b82f6 !important;
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
html.dark .meta-time { color: #475569 !important; }
html.dark .skeleton-shimmer {
  background: linear-gradient(90deg, #334155 25%, #475569 50%, #334155 75%) !important;
  background-size: 200% 100% !important;
}
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
</style>

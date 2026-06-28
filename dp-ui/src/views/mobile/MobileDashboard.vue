<template>
  <MobilePageShell @refresh="refreshAll">
    <!-- 欢迎卡片 -->
    <div class="welcome-card">
      <div class="welcome-info">
        <div class="welcome-greeting">{{ greetingText }}</div>
        <div class="welcome-sub">今天是 {{ todayStr }}，祝您工作顺利</div>
      </div>
      <div class="welcome-avatar">
        <n-avatar round :size="42" :style="{ background: '#3b82f6' }">
          {{ userStore.userInfo?.nickname?.[0] || '用' }}
        </n-avatar>
      </div>
    </div>

    <!-- 快捷统计 -->
    <div class="quick-stats">
      <div class="stat-card">
        <div class="stat-icon stat-icon-page">
          <n-icon :size="18"><GridOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ pageCount }}</span>
          <span class="stat-label">页面</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon-chart">
          <n-icon :size="18"><BarChartOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ chartCount }}</span>
          <span class="stat-label">图表</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon stat-icon-report">
          <n-icon :size="18"><DocumentTextOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ reportCount }}</span>
          <span class="stat-label">报表</span>
        </div>
      </div>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="page-list" style="margin-top: 16px;">
      <div v-for="i in 4" :key="i" class="page-card skeleton-card">
        <div class="page-card-icon"><n-skeleton circle size="medium" /></div>
        <div class="page-card-body">
          <n-skeleton text style="width: 60%" :sharp="false" />
          <n-skeleton text style="width: 35%" size="small" :sharp="false" />
        </div>
      </div>
    </div>

    <template v-else>
      <!-- 授权资源列表（从后端菜单权限生成） -->
      <MobileEmpty
        v-if="allResources.length === 0"
        type="data"
        title="暂无授权资源"
        description="管理员尚未为您分配移动端可访问的资源"
      />

      <template v-else>
        <div class="section-title">授权资源 <span class="section-badge">{{ allResources.length }}</span></div>
        <div class="page-list">
          <div
            v-for="item in allResources"
            :key="item.key"
            class="page-card"
            @click="navigateResource(item)"
          >
            <div class="page-card-icon" :style="{ background: item.gradient }">
              <n-icon size="24" color="#fff"><component :is="item.icon" /></n-icon>
            </div>
            <div class="page-card-body">
              <div class="page-card-title">{{ item.label }}</div>
              <div class="page-card-meta">
                <n-tag :type="item.tagType" size="tiny" round>{{ item.typeLabel }}</n-tag>
              </div>
            </div>
            <n-icon size="16" color="#cbd5e1"><ChevronForwardOutline /></n-icon>
          </div>
        </div>
      </template>

      <!-- 最近访问 -->
      <div v-if="recentVisits.length > 0" class="section-title">最近访问</div>
      <div v-if="recentVisits.length > 0" class="recent-list">
        <div v-for="item in recentVisits" :key="item.path" class="recent-item" @click="router.push(item.path)">
          <div class="recent-icon">
            <n-icon size="16"><TimeOutline /></n-icon>
          </div>
          <div class="recent-info">
            <span class="recent-title">{{ item.title }}</span>
            <span class="recent-time">{{ item.timeAgo }}</span>
          </div>
          <n-icon size="14" color="#cbd5e1"><ChevronForwardOutline /></n-icon>
        </div>
      </div>
    </template>
  </MobilePageShell>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, onMounted, type Component } from 'vue'
import { useRouter } from 'vue-router'
import { NIcon, NTag, NSkeleton, NAvatar } from 'naive-ui'
import {
  GridOutline, ChevronForwardOutline,
  BarChartOutline, DocumentTextOutline,
  ReaderOutline,
  TimeOutline
} from '@vicons/ionicons5'
import MobilePageShell from '@/components/mobile/MobilePageShell.vue'
import MobileEmpty from '@/components/mobile/MobileEmpty.vue'
import { useUserStore } from '@/stores/user'
import { getVisibleMenus } from '@/api/system/menu'
import { getMobileEnabledPages } from '@/api/page'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)

// 问候语
const greetingText = computed(() => {
  const hour = new Date().getHours()
  const name = userStore.userInfo?.nickname || '用户'
  if (hour < 6) return `夜深了，${name}`
  if (hour < 12) return `早上好，${name}`
  if (hour < 18) return `下午好，${name}`
  return `晚上好，${name}`
})

const todayStr = computed(() => {
  const d = new Date()
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return `${d.getMonth() + 1}月${d.getDate()}日 ${weekdays[d.getDay()]}`
})

// 授权资源列表（从后端菜单权限生成）
interface MenuResource {
  key: string
  label: string
  icon: Component
  gradient: string
  tagType: 'info' | 'success' | 'warning' | 'default'
  typeLabel: string
  path: string
}
const menuResources = ref<MenuResource[]>([])
let loadVersion = 0
const publishedPages = ref<MenuResource[]>([])
const allResources = computed(() => [...menuResources.value, ...publishedPages.value])

// 资源统计（单次遍历，包含直接发布页面）
const resourceCounts = computed(() => {
  let page = 0, chart = 0, report = 0
  for (const r of allResources.value) {
    const prefix = r.key.substring(0, r.key.indexOf('_'))
    if (prefix === 'page') page++
    else if (prefix === 'chart') chart++
    else if (prefix === 'report') report++
  }
  return { page, chart, report }
})
const pageCount = computed(() => resourceCounts.value.page)
const chartCount = computed(() => resourceCounts.value.chart)
const reportCount = computed(() => resourceCounts.value.report)

const defaultGradient = 'linear-gradient(135deg, #3b82f6, #2563eb)'
const gradientMap: Record<string, string> = {
  report: 'linear-gradient(135deg, #8b5cf6, #7c3aed)',
  chart: 'linear-gradient(135deg, #3b82f6, #2563eb)',
  page: 'linear-gradient(135deg, #10b981, #059669)',
  dataView: 'linear-gradient(135deg, #f59e0b, #d97706)'
}

function flattenMenus(menus: any[]): MenuResource[] {
  const result: MenuResource[] = []
  const walk = (list: any[]) => {
    for (const menu of list) {
      if (menu.mobileVisible === 0) continue
      if (menu.reportId) {
        result.push({
          key: `report_${menu.reportId}`,
          label: menu.menuName,
          icon: DocumentTextOutline,
          gradient: gradientMap['report'] || defaultGradient,
          tagType: 'info',
          typeLabel: '报表',
          path: `/m/report/${menu.reportId}`
        })
      } else if (menu.chartId) {
        result.push({
          key: `chart_${menu.chartId}`,
          label: menu.menuName,
          icon: BarChartOutline,
          gradient: gradientMap['chart'] || defaultGradient,
          tagType: 'success',
          typeLabel: '图表',
          path: `/m/chart/${menu.chartId}`
        })
      } else if (menu.pageId) {
        result.push({
          key: `page_${menu.pageId}`,
          label: menu.menuName,
          icon: GridOutline,
          gradient: gradientMap['page'] || defaultGradient,
          tagType: 'warning',
          typeLabel: '页面',
          path: `/m/page/${menu.pageId}`
        })
      } else if (menu.dataViewCode) {
        result.push({
          key: `dv_${menu.dataViewCode}`,
          label: menu.menuName,
          icon: ReaderOutline,
          gradient: gradientMap['dataView'] || defaultGradient,
          tagType: 'default',
          typeLabel: '数据管理',
          path: `/m/data-view/${menu.dataViewCode}`
        })
      } else if (menu.routePath) {
        // 从routePath解析图表/页面/报表ID
        const chartMatch = menu.routePath.match(/\/chart[-_](?:center|view|embed)\/?([0-9]+)/i)
        const pageMatch = menu.routePath.match(/\/page[-_]?(?:view|definition)\/?([0-9]+)/i)
        const reportMatch = menu.routePath.match(/\/report[-_]?view\/?([0-9]+)/i)
        const dvMatch = menu.routePath.match(/\/data[-_]view\/(.+)/i)
        if (chartMatch) {
          result.push({ key: `chart_${chartMatch[1]}`, label: menu.menuName, icon: BarChartOutline, gradient: gradientMap['chart'] || defaultGradient, tagType: 'success', typeLabel: '图表', path: `/m/chart/${chartMatch[1]}` })
        } else if (pageMatch) {
          result.push({ key: `page_${pageMatch[1]}`, label: menu.menuName, icon: GridOutline, gradient: gradientMap['page'] || defaultGradient, tagType: 'warning', typeLabel: '页面', path: `/m/page/${pageMatch[1]}` })
        } else if (reportMatch) {
          result.push({ key: `report_${reportMatch[1]}`, label: menu.menuName, icon: DocumentTextOutline, gradient: gradientMap['report'] || defaultGradient, tagType: 'info', typeLabel: '报表', path: `/m/report/${reportMatch[1]}` })
        } else if (dvMatch) {
          result.push({ key: `dv_${dvMatch[1]}`, label: menu.menuName, icon: ReaderOutline, gradient: gradientMap['dataView'] || defaultGradient, tagType: 'default', typeLabel: '数据管理', path: `/m/data-view/${dvMatch[1]}` })
        }
      }
      if (menu.children?.length) {
        walk(menu.children)
      }
    }
  }
  walk(menus)
  return result
}

async function loadMenuResources() {
  const version = ++loadVersion
  loading.value = true
  try {
    const [menuRes, pageRes] = await Promise.allSettled([
      getVisibleMenus(),
      getMobileEnabledPages()
    ])
    if (version !== loadVersion) return

    const menus = menuRes.status === 'fulfilled' ? (menuRes.value.data || []) : []
    menuResources.value = flattenMenus(Array.isArray(menus) ? menus : [])

    if (pageRes.status === 'fulfilled') {
      const pages = pageRes.value.data || []
      const menuPageKeys = new Set(menuResources.value.map(r => r.key))
      publishedPages.value = pages
        .filter(p => !menuPageKeys.has(`page_${p.id}`))
        .map(p => ({
          key: `page_${p.id}`,
          label: p.page_name,
          icon: GridOutline,
          gradient: gradientMap['page'] || defaultGradient,
          tagType: 'warning' as const,
          typeLabel: '页面',
          path: `/m/page/${p.id}`
        }))
    }
  } catch (e) {
    if (version !== loadVersion) return
    console.error('加载资源失败', e)
  } finally {
    if (version === loadVersion) loading.value = false
  }
}

function navigateResource(item: MenuResource) {
  router.push(item.path)
}

// 最近访问
interface RecentVisit { path: string; title: string; time: number; timeAgo: string }
const recentVisits = ref<RecentVisit[]>([])

function loadRecentVisits() {
  try {
    const raw = localStorage.getItem('dp_recent_visits')
    if (!raw) return
    const list = JSON.parse(raw) as { path: string; title: string; time: number }[]
    const now = Date.now()
    recentVisits.value = list.slice(0, 5).map(item => ({
      ...item,
      timeAgo: formatTimeAgo(now - item.time)
    }))
  } catch { /* ignore */ }
}

function formatTimeAgo(ms: number): string {
  const minutes = Math.floor(ms / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  return `${days}天前`
}

function refreshAll() {
  loadRecentVisits()
  loadMenuResources()
}

onMounted(() => {
  loadRecentVisits()
  loadMenuResources()
})
</script>

<style scoped>
/* 欢迎卡片 */
.welcome-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 20px;
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 50%, #8b5cf6 100%);
  border-radius: 20px;
  margin-bottom: 14px;
  color: #fff;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.2);
}

.welcome-greeting {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 6px;
  letter-spacing: -0.2px;
}

.welcome-sub {
  font-size: 13px;
  opacity: 0.85;
  line-height: 1.4;
}

/* 区域标题 */
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin: 18px 0 10px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-badge {
  font-size: 11px;
  padding: 2px 9px;
  border-radius: 10px;
  background: #eff6ff;
  color: #3b82f6;
  font-weight: 600;
}

/* 最近访问 */
.recent-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 4px;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: #fff;
  border-radius: 14px;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: transform 0.15s ease, background 0.15s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.recent-item:active { background: #f1f5f9; transform: scale(0.97); }

.recent-icon {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  flex-shrink: 0;
}

.recent-info { flex: 1; min-width: 0; }

.recent-title {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-time {
  font-size: 11px;
  color: #94a3b8;
}

/* 资源列表 */
.skeleton-card { pointer-events: none; }

.page-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.page-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: #fff;
  border-radius: 16px;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05), 0 1px 2px rgba(0, 0, 0, 0.03);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
  -webkit-tap-highlight-color: transparent;
  animation: cardFadeIn 0.3s ease backwards;
}

@keyframes cardFadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-card:active { transform: scale(0.97); box-shadow: 0 0 0 rgba(0,0,0,0); }

.page-card-icon {
  width: 46px;
  height: 46px;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.page-card-body { flex: 1; min-width: 0; }

.page-card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 5px;
  line-height: 1.3;
}

.page-card-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 快捷统计 */
.quick-stats {
  display: flex;
  gap: 10px;
  margin-bottom: 4px;
}

.stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 12px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05), 0 1px 2px rgba(0, 0, 0, 0.03);
}

.stat-icon {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-icon-page { background: linear-gradient(135deg, #10b981, #059669); }
.stat-icon-chart { background: linear-gradient(135deg, #3b82f6, #2563eb); }
.stat-icon-report { background: linear-gradient(135deg, #8b5cf6, #7c3aed); }

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}

.stat-label {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 1px;
}

/* 深色模式 - 骨架屏 */
</style>

<style>
/* MobileDashboard 深色模式（非 scoped） */
html.dark .welcome-card {
  background: linear-gradient(135deg, #1e40af 0%, #4338ca 50%, #6d28d9 100%) !important;
  box-shadow: 0 4px 16px rgba(30, 64, 175, 0.3) !important;
}
html.dark .quick-stats .stat-card { background: #1e293b !important; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important; }
html.dark .stat-value { color: #e2e8f0 !important; }
html.dark .stat-label { color: #64748b !important; }
html.dark .section-title { color: #e2e8f0 !important; }
html.dark .section-badge { background: rgba(59, 130, 246, 0.15) !important; color: #60a5fa !important; }
html.dark .page-card { background: #1e293b !important; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important; }
html.dark .page-card:active { background: #263449 !important; }
html.dark .page-card-title { color: #e2e8f0 !important; }
html.dark .page-card-meta { color: #64748b !important; }
html.dark .page-card .n-icon:last-child { color: #475569 !important; }
html.dark .recent-item { background: #1e293b !important; box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2) !important; }
html.dark .recent-item:active { background: #334155 !important; }
html.dark .recent-icon { background: #334155 !important; color: #64748b !important; }
html.dark .recent-title { color: #e2e8f0 !important; }
html.dark .recent-time { color: #475569 !important; }
html.dark .recent-item .n-icon:last-child { color: #475569 !important; }
html.dark .skeleton-card .page-card-icon .n-skeleton {
  background: #334155 !important;
}
html.dark .skeleton-card .n-skeleton {
  background: #334155 !important;
}
</style>

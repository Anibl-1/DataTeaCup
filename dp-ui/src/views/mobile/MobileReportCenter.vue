<template>
  <MobilePageShell @refresh="loadReports">
    <!-- 统计栏 -->
    <MobileStatBar :stats="statItems" />

    <!-- 搜索栏 -->
    <div class="search-bar">
      <n-input
        v-model:value="searchKeyword"
        placeholder="搜索报表名称"
        clearable
        round
        size="small"
      >
        <template #prefix>
          <n-icon size="16" color="#94a3b8"><SearchOutline /></n-icon>
        </template>
      </n-input>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="report-list">
      <div v-for="i in 4" :key="i" class="report-card skeleton-card">
        <div class="report-card-icon skeleton-shimmer"></div>
        <div class="report-card-body">
          <n-skeleton text style="width: 55%" :sharp="false" />
          <n-skeleton text style="width: 35%" size="small" :sharp="false" />
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <MobileEmpty
      v-else-if="filteredReports.length === 0"
      :type="searchKeyword ? 'search' : 'data'"
      :title="searchKeyword ? '未找到匹配报表' : '暂无可用报表'"
      :description="emptyDescription"
    >
      <template v-if="searchKeyword" #action>
        <n-button size="small" @click="searchKeyword = ''">清除搜索</n-button>
      </template>
    </MobileEmpty>

    <!-- 列表 -->
    <div v-else class="report-list">
      <div
        v-for="report in filteredReports"
        :key="report.id"
        class="report-card"
        @click="viewReport(report)"
      >
        <div class="report-card-icon" :style="{ background: getGradient(report) }">
          <n-icon size="20" color="#fff"><DocumentTextOutline /></n-icon>
        </div>
        <div class="report-card-body">
          <div class="report-card-title">{{ report.reportName }}</div>
          <div class="report-card-meta">
            <n-tag type="warning" size="tiny" :bordered="false" round>报表</n-tag>
            <span v-if="report.description" class="meta-desc">{{ report.description }}</span>
          </div>
        </div>
        <div class="report-card-arrow">
          <n-icon size="16" color="#cbd5e1"><ChevronForwardOutline /></n-icon>
        </div>
      </div>
    </div>

  </MobilePageShell>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NInput, NIcon, NTag, NButton, NSkeleton } from 'naive-ui'
import { SearchOutline, DocumentTextOutline, ChevronForwardOutline, ListOutline } from '@vicons/ionicons5'
import MobilePageShell from '@/components/mobile/MobilePageShell.vue'
import MobileStatBar from '@/components/mobile/MobileStatBar.vue'
import MobileEmpty from '@/components/mobile/MobileEmpty.vue'
import { getVisibleMenus } from '@/api/system/menu'
import { getReportDefinitionById } from '@/api/reportDefinition'
import type { ReportDefinition } from '@/types/reportDefinition'
import type { Menu } from '@/types/menu'

const router = useRouter()
const searchKeyword = ref('')
const loading = ref(false)
const reportList = ref<ReportDefinition[]>([])

const isCanceledRequest = (error: any) => {
  return error?.code === 'ERR_CANCELED' || error?.name === 'CanceledError' || error?.message === 'canceled'
}

const gradients = [
  'linear-gradient(135deg, #f59e0b, #d97706)',
  'linear-gradient(135deg, #3b82f6, #6366f1)',
  'linear-gradient(135deg, #10b981, #059669)',
  'linear-gradient(135deg, #8b5cf6, #7c3aed)',
  'linear-gradient(135deg, #ec4899, #f43f5e)',
  'linear-gradient(135deg, #0ea5e9, #0284c7)',
  'linear-gradient(135deg, #14b8a6, #0d9488)'
]

function getGradient(report: ReportDefinition): string {
  return gradients[(report.id ?? 0) % gradients.length] ?? gradients[0]!
}

const statItems = computed(() => [
  { icon: DocumentTextOutline, label: '全部报表', value: reportList.value.length, variant: 'primary' as const, iconBg: 'linear-gradient(135deg, #f59e0b, #d97706)' },
  { icon: ListOutline, label: '当前筛选', value: filteredReports.value.length, variant: 'info' as const, iconBg: 'linear-gradient(135deg, #0ea5e9, #0284c7)' }
])

const emptyDescription = computed(() => {
  if (searchKeyword.value) return '没有找到与 "' + searchKeyword.value + '" 相关的报表'
  return '请先在管理端发布报表到移动端'
})

const filteredReports = computed(() => {
  if (!searchKeyword.value) return reportList.value
  const kw = searchKeyword.value.toLowerCase()
  return reportList.value.filter(r =>
    r.reportName.toLowerCase().includes(kw) || r.reportCode.toLowerCase().includes(kw) ||
    (r.description && r.description.toLowerCase().includes(kw))
  )
})

// 从菜单树中递归提取所有关联了报表的ID（包括从routePath解析）
function extractPermittedReportIds(menus: Menu[]): Set<number> {
  const ids = new Set<number>()
  function walk(list: Menu[]) {
    for (const m of list) {
      if (m.reportId) ids.add(m.reportId)
      if (m.routePath) {
        const match = m.routePath.match(/\/report[-_]?view\/?([0-9]+)/i)
        if (match) ids.add(Number(match[1]))
      }
      if (m.children?.length) walk(m.children)
    }
  }
  walk(menus)
  return ids
}

async function loadReports() {
  loading.value = true
  try {
    // 1. 获取用户有权限的菜单，提取关联的reportId
    const menuRes = await getVisibleMenus() as any
    const rawMenus = menuRes?.data?.data || menuRes?.data || []
    const menus: Menu[] = Array.isArray(rawMenus) ? rawMenus : []
    const reportIds = extractPermittedReportIds(menus)
    const hasMenuPermission = menus.length > 0

    // 2. 获取有权限的报表（并行请求）
    const fetchPromises = [...reportIds].map(id =>
      getReportDefinitionById(id).then(res => {
        const report = (res as any)?.data?.data || (res as any)?.data
        return report as ReportDefinition | null
      }).catch(() => null)
    )
    const reports = await Promise.all(fetchPromises)

    // 3. 过滤：显示 mobileEnabled=1 的，或有菜单权限关联的
    const validReports = reports.filter((r): r is ReportDefinition =>
      r != null && r.id != null && r.status === 1
    )
    // 也尝试通过全量接口获取 mobileEnabled=1 但未在菜单中关联的报表
    // （这样管理员只设置 mobileEnabled=1 就能显示）
    reportList.value = validReports.filter(r =>
      r.mobileEnabled === 1 || (hasMenuPermission && reportIds.has(r.id!))
    )
  } catch (e) {
    if (!isCanceledRequest(e)) {
      console.error('加载报表失败', e)
    }
  } finally {
    loading.value = false
  }
}

function viewReport(report: ReportDefinition) {
  router.push(`/m/report/${report.id}`)
}

onMounted(() => { loadReports() })
</script>

<style scoped>
.search-bar { margin-bottom: 12px; }

.report-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.report-card {
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
  animation: reportCardIn 0.3s ease backwards;
}

@keyframes reportCardIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}
.report-card:active {
  transform: scale(0.97);
  box-shadow: 0 0 0 rgba(0, 0, 0, 0);
}

.report-card-icon {
  width: 46px;
  height: 46px;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.report-card-body { flex: 1; min-width: 0; }

.report-card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 5px;
  line-height: 1.3;
}

.report-card-meta {
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

.report-card-arrow {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

/* 骨架屏 */
.skeleton-card { pointer-events: none; }
.skeleton-card .report-card-icon { background: none !important; }
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


/* 深色模式 - 搜索框 */
</style>

<style>
/* MobileReportCenter 深色模式（非 scoped） */
html.dark .report-card { background: #1e293b !important; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important; }
html.dark .report-card:active { background: #263449 !important; }
html.dark .report-card-title { color: #e2e8f0 !important; }
html.dark .report-card-meta { color: #64748b !important; }
html.dark .meta-desc { color: #64748b !important; }
html.dark .report-card-arrow .n-icon { color: #475569 !important; }
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

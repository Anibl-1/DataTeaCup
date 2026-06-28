<template>
  <div class="workspace-page">
    <!-- 顶部问候区 -->
    <div class="greeting-banner">
      <div class="greeting-left">
        <div class="greeting-avatar">
          <n-avatar :size="56" round :style="userAvatarPreset ? { background: userAvatarPreset.gradient } : { background: themeStore.primaryColor }">
            {{ userAvatarPreset ? userAvatarPreset.icon : avatarText }}
          </n-avatar>
        </div>
        <div class="greeting-info">
          <h2 class="greeting-title">{{ greetingText }}，{{ displayName }}</h2>
          <p class="greeting-subtitle">
            <span v-if="workspaceData.deptName" class="dept-tag">
              <n-icon size="14"><BusinessOutline /></n-icon>
              {{ workspaceData.deptName }}
            </span>
            <span class="greeting-date">{{ todayStr }}</span>
          </p>
        </div>
      </div>
      <div class="greeting-right">
        <div v-if="canViewCollect" class="greeting-stat">
          <span class="greeting-stat-value">{{ workspaceData.todayCollectCount || 0 }}</span>
          <span class="greeting-stat-label">今日采集</span>
        </div>
        <div v-if="canViewPipeline" class="greeting-stat">
          <span class="greeting-stat-value">{{ pipelineStats.todayCount || 0 }}</span>
          <span class="greeting-stat-label">今日流程</span>
        </div>
      </div>
    </div>

    <!-- 统计卡片行 -->
    <n-grid v-if="visibleStatCards.length > 0" :cols="visibleStatCards.length" :x-gap="12" :y-gap="12" style="margin-bottom: 16px;" responsive="screen" item-responsive>
      <n-gi v-for="stat in visibleStatCards" :key="stat.label" :span="`${visibleStatCards.length} m:1`">
        <div class="stat-card" :class="'stat-card--' + stat.variant">
          <div class="stat-card-icon" :style="{ background: stat.bgColor }">
            <n-icon size="22" :color="stat.color"><component :is="stat.icon" /></n-icon>
          </div>
          <div class="stat-card-body">
            <div class="stat-card-value">{{ stat.value }}</div>
            <div class="stat-card-label">{{ stat.label }}</div>
          </div>
        </div>
      </n-gi>
    </n-grid>

    <!-- 主内容区 -->
    <n-grid :cols="mainGridCols" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <!-- 左列：待办事项 + 最新公告 -->
      <n-gi span="3 m:1">
        <!-- 待办事项 -->
        <n-card size="small" class="workspace-card" style="margin-bottom: 16px;">
          <template #header>
            <div class="card-header">
              <div class="card-title">
                <n-icon size="18" :color="themeStore.primaryColor"><CheckboxOutline /></n-icon>
                <span>我的待办</span>
              </div>
              <n-tag :type="pendingTodoCount > 0 ? 'warning' : 'success'" size="small" round>
                {{ pendingTodoCount }} 项
              </n-tag>
            </div>
          </template>
          <div class="todo-input-row">
            <n-input
              v-model:value="newTodoText"
              placeholder="添加待办事项..."
              size="small"
              @keydown.enter="addTodo"
            />
            <n-button size="small" type="primary" :disabled="!newTodoText.trim()" @click="addTodo">
              添加
            </n-button>
          </div>
          <div v-if="todoList.length > 0" class="todo-list">
            <TransitionGroup name="todo">
              <div
                v-for="(item, idx) in todoList"
                :key="item.id"
                class="todo-item"
                :class="{ 'todo-done': item.done }"
              >
                <n-checkbox :checked="item.done" @update:checked="toggleTodo(idx)" />
                <span class="todo-text">{{ item.text }}</span>
                <n-button quaternary size="tiny" type="error" @click="removeTodo(idx)">
                  <n-icon size="14"><CloseOutline /></n-icon>
                </n-button>
              </div>
            </TransitionGroup>
          </div>
          <EmptyState v-else description="暂无待办" :icon-size="48" />
        </n-card>

        <!-- 最新公告 -->
        <n-card size="small" class="workspace-card">
          <template #header>
            <div class="card-header">
              <div class="card-title">
                <n-icon size="18" color="#f0a020"><MegaphoneOutline /></n-icon>
                <span>最新公告</span>
              </div>
            </div>
          </template>
          <div v-if="announcements.length > 0" class="announcement-list">
            <div v-for="item in announcements" :key="item.id" class="announcement-item">
              <n-tag :type="item.type || 'info'" size="small">
                {{ item.type === 'warning' ? '警告' : item.type === 'error' ? '紧急' : '通知' }}
              </n-tag>
              <span class="announcement-title">{{ item.title }}</span>
              <span class="announcement-time">{{ formatShortDate(item.createTime) }}</span>
            </div>
          </div>
          <EmptyState v-else description="暂无公告" :icon-size="48" />
        </n-card>
      </n-gi>

      <!-- 中列：快捷操作 + 流程执行状态 -->
      <n-gi :span="`${mainGridCols} m:1`">
        <!-- 快捷操作 -->
        <n-card size="small" class="workspace-card" style="margin-bottom: 16px;">
          <template #header>
            <div class="card-header">
              <div class="card-title">
                <n-icon size="18" :color="themeStore.primaryColor"><AppsOutline /></n-icon>
                <span>快捷操作</span>
              </div>
            </div>
          </template>
          <div v-if="filteredQuickActions.length > 0" class="quick-actions-grid">
            <div
              v-for="action in filteredQuickActions"
              :key="action.name"
              class="quick-action-item"
              @click="router.push(action.path)"
            >
              <div class="quick-action-icon">
                <n-icon size="20" :color="themeStore.primaryColor"><component :is="getIcon(action.icon)" /></n-icon>
              </div>
              <span class="quick-action-label">{{ action.name }}</span>
            </div>
          </div>
          <EmptyState v-else description="暂无可用操作" :icon-size="48" />
        </n-card>

        <!-- 流程执行概览（管理员/部门领导可见） -->
        <n-card v-if="canViewPipeline" size="small" class="workspace-card">
          <template #header>
            <div class="card-header">
              <div class="card-title">
                <n-icon size="18" color="#8a2be2"><GitBranchOutline /></n-icon>
                <span>流程执行概览</span>
              </div>
              <n-button text size="small" @click="router.push('/pipeline/monitor')">查看详情</n-button>
            </div>
          </template>
          <div class="pipeline-overview">
            <div class="pipeline-stat-item">
              <div class="pipeline-stat-num text-primary">{{ pipelineStats.totalExecutions || 0 }}</div>
              <div class="pipeline-stat-label">总执行数</div>
            </div>
            <div class="pipeline-stat-item">
              <div class="pipeline-stat-num text-warning">{{ pipelineStats.runningCount || 0 }}</div>
              <div class="pipeline-stat-label">运行中</div>
            </div>
            <div class="pipeline-stat-item">
              <div class="pipeline-stat-num text-success">{{ pipelineStats.successToday || 0 }}</div>
              <div class="pipeline-stat-label">成功</div>
            </div>
            <div class="pipeline-stat-item">
              <div class="pipeline-stat-num text-error">{{ pipelineStats.failedToday || 0 }}</div>
              <div class="pipeline-stat-label">失败</div>
            </div>
          </div>
          <!-- 采集趋势迷你图 -->
          <div class="trend-section">
            <div class="trend-title">近7日采集趋势</div>
            <div class="mini-bar-chart">
              <div
                v-for="(count, idx) in trendCounts"
                :key="idx"
                class="mini-bar-wrapper"
              >
                <div
                  class="mini-bar"
                  :style="{ height: getBarHeight(count) + 'px' }"
                  :title="trendDates[idx] + ': ' + count"
                ></div>
                <span class="mini-bar-label">{{ trendDates[idx] }}</span>
              </div>
            </div>
          </div>
        </n-card>
      </n-gi>

      <!-- 右列：最近操作（管理员可见） -->
      <n-gi v-if="canViewOperationLog" :span="`${mainGridCols} m:1`">
        <n-card size="small" class="workspace-card" style="height: 100%;">
          <template #header>
            <div class="card-header">
              <div class="card-title">
                <n-icon size="18" color="#d03050"><TimeOutline /></n-icon>
                <span>最近操作</span>
              </div>
              <n-button text size="small" @click="router.push('/operation-log')">全部日志</n-button>
            </div>
          </template>
          <div v-if="recentOps.length > 0" class="timeline-list">
            <div v-for="(op, idx) in recentOps" :key="idx" class="timeline-item">
              <div class="timeline-dot" :class="'dot-' + getOpType(op.operation_type)"></div>
              <div class="timeline-content">
                <div class="timeline-header">
                  <n-tag :type="getOpColor(op.operation_type)" size="small">{{ op.operation_type }}</n-tag>
                  <span class="timeline-module">{{ op.module_name }}</span>
                </div>
                <div class="timeline-desc">{{ op.operation_desc }}</div>
                <div class="timeline-time">{{ formatTime(op.create_time) }}</div>
              </div>
            </div>
          </div>
          <EmptyState v-else description="暂无操作记录" :icon-size="48" />
        </n-card>
      </n-gi>
    </n-grid>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { avatarPresets } from '@/constants/avatarPresets'
import {
  ServerOutline, DownloadOutline, DocumentOutline, BarChartOutline,
  CloudUploadOutline, SparklesOutline,
  BusinessOutline, CheckboxOutline, CloseOutline, MegaphoneOutline,
  AppsOutline, GitBranchOutline, TimeOutline, PeopleOutline,
  BookOutline, SettingsOutline
} from '@vicons/ionicons5'
import EmptyState from '@/components/common/EmptyState.vue'
import request from '@/api/request'
import { getGreetingByHour } from '@/utils/greeting'

const userStore = useUserStore()
const themeStore = useThemeStore()
const router = useRouter()

/** 解析头像预设 */
function resolveAvatarPreset(avatarId?: string | null) {
  if (!avatarId) return null
  return avatarPresets.find(p => p.id === avatarId) || null
}

const userAvatarPreset = computed(() => resolveAvatarPreset(userStore.userInfo?.avatar))

// ==================== 权限控制 ====================
const isAdminUser = computed(() => userStore.isAdmin())
const isManager = computed(() => {
  return isAdminUser.value
    || userStore.hasRole('dept_leader')
    || userStore.hasRole('manager')
    || userStore.hasRole('ROLE_ADMIN')
    || userStore.hasRole('admin')
    || userStore.hasPermission('system:manage')
})

/** 可查看采集相关内容（有采集权限或管理员） */
const canViewCollect = computed(() => isManager.value || userStore.hasPermission('data:collect'))
/** 可查看流程概览（管理员或部门负责人） */
const canViewPipeline = computed(() => isManager.value)
/** 可查看操作日志（仅管理员） */
const canViewOperationLog = computed(() => isAdminUser.value)
/** 主区域列数：有操作日志时 3 列，否则 2 列 */
const mainGridCols = computed(() => canViewOperationLog.value ? 3 : 2)

const workspaceData = ref<any>({})
const recentOps = ref<any[]>([])
const quickActions = ref<any[]>([])
const announcements = ref<any[]>([])
let todoIdCounter = 0
const todoList = ref<{ id: number; text: string; done: boolean }[]>([])
const newTodoText = ref('')

// 从 userStore 获取用户名
const displayName = computed(() => {
  return workspaceData.value.nickname || userStore.userInfo?.nickname || userStore.userInfo?.username || '用户'
})

const avatarText = computed(() => {
  const name = displayName.value
  return name ? name.substring(0, 1).toUpperCase() : 'U'
})

// 时间问候 (Property 25: 0-5=夜深了, 6-11=早上好, 12-17=下午好, 18-23=晚上好)
const greetingText = computed(() => getGreetingByHour(new Date().getHours()))

const todayStr = computed(() => {
  const d = new Date()
  const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${weekDays[d.getDay()]}`
})

// 流程统计
const pipelineStats = computed(() => workspaceData.value.pipelineStats || {})

// 采集趋势
const trendDates = computed(() => workspaceData.value.collectTrend?.dates || [])
const trendCounts = computed(() => workspaceData.value.collectTrend?.counts || [])

const getBarHeight = (count: number) => {
  const max = Math.max(...(trendCounts.value as number[]), 1)
  return Math.max(4, (count / max) * 60)
}

// 统计卡片
const statCards = computed(() => [
  { label: '数据源', value: workspaceData.value.dataSourceCount || 0, variant: 'primary', color: themeStore.primaryColor, bgColor: themeStore.primaryColor + '14', icon: ServerOutline, permission: 'data:source' },
  { label: '采集任务', value: workspaceData.value.collectTaskCount || 0, variant: 'success', color: '#18a058', bgColor: '#E8F5E9', icon: DownloadOutline, permission: 'data:collect' },
  { label: '报表', value: workspaceData.value.reportCount || 0, variant: 'warning', color: '#f0a020', bgColor: '#FFF8E1', icon: DocumentOutline },
  { label: '图表', value: workspaceData.value.chartCount || 0, variant: 'purple', color: '#8a2be2', bgColor: '#F3E5F5', icon: BarChartOutline },
  { label: '用户', value: workspaceData.value.userCount || 0, variant: 'error', color: '#d03050', bgColor: '#FFEBEE', icon: PeopleOutline, adminOnly: true },
  { label: '部门', value: workspaceData.value.totalDepartments || 0, variant: 'teal', color: '#00796B', bgColor: '#E0F2F1', icon: BusinessOutline, adminOnly: true }
])

/** 根据权限过滤统计卡片 */
const visibleStatCards = computed(() => {
  return statCards.value.filter((s: any) => {
    if (s.adminOnly && !isAdminUser.value) return false
    if (s.permission && !isAdminUser.value && !userStore.hasPermission(s.permission)) return false
    return true
  })
})

/** 快捷操作路径 → 所需权限映射（'ADMIN_ONLY' 表示仅管理员可见） */
const actionPermissionMap: Record<string, string> = {
  // ===== 仅管理员可见 =====
  '/user': 'ADMIN_ONLY',
  '/role': 'ADMIN_ONLY',
  '/menu': 'ADMIN_ONLY',
  '/department': 'ADMIN_ONLY',
  '/system-config': 'ADMIN_ONLY',
  '/system-setting': 'ADMIN_ONLY',
  '/system-monitor': 'ADMIN_ONLY',
  '/operation-log': 'ADMIN_ONLY',
  '/login-log': 'ADMIN_ONLY',
  '/db-manager': 'ADMIN_ONLY',
  '/upgrade': 'ADMIN_ONLY',
  '/data-source': 'ADMIN_ONLY',
  '/datasource': 'ADMIN_ONLY',
  '/data-dictionary': 'ADMIN_ONLY',
  '/data-import': 'ADMIN_ONLY',
  // ===== 需要特定权限 =====
  '/data-collect': 'data:collect',
  '/collect': 'data:collect',
  '/pipeline': 'pipeline:read',
  '/datax': 'datax:job',
  '/report-manage': 'report:manage',
  '/chart-manage': 'chart:manage',
}

/** 根据权限过滤快捷操作 */
const filteredQuickActions = computed(() => {
  if (isAdminUser.value) return quickActions.value
  return quickActions.value.filter((a: any) => {
    if (!a.path) return true
    // 查找匹配的权限要求
    for (const [prefix, perm] of Object.entries(actionPermissionMap)) {
      if (a.path.startsWith(prefix)) {
        // ADMIN_ONLY 标记：仅管理员可见
        if (perm === 'ADMIN_ONLY') return false
        return userStore.hasPermission(perm)
      }
    }
    return true // 无特殊权限要求的操作对所有人可见
  })
})

// 待办数
const pendingTodoCount = computed(() => todoList.value.filter(t => !t.done).length)

// 图标映射
const iconMap: Record<string, any> = {
  ServerOutline, DownloadOutline, DocumentOutline, BarChartOutline,
  SparklesOutline, CloudUploadOutline, GitBranchOutline, BookOutline,
  SettingsOutline
}
const getIcon = (name: string) => iconMap[name] || ServerOutline

const getOpColor = (type: string): 'success' | 'info' | 'warning' | 'error' | 'default' => {
  const map: Record<string, any> = { CREATE: 'success', UPDATE: 'info', DELETE: 'error', EXPORT: 'warning', IMPORT: 'warning', LOGIN: 'success' }
  return map[type] || 'default'
}
const getOpType = (type: string) => {
  const map: Record<string, string> = { CREATE: 'success', UPDATE: 'info', DELETE: 'error', EXPORT: 'warning' }
  return map[type] || 'default'
}

const formatTime = (time: any) => {
  if (!time) return ''
  try {
    const d = new Date(time)
    if (isNaN(d.getTime())) return String(time)
    const now = new Date()
    const diff = now.getTime() - d.getTime()
    if (diff < 60000) return '刚刚'
    if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
    if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
    if (diff < 172800000) return '昨天'
    return `${d.getMonth() + 1}-${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  } catch { return String(time) }
}

const formatShortDate = (time: any) => {
  if (!time) return ''
  try {
    const d = new Date(time)
    return `${d.getMonth() + 1}-${d.getDate()}`
  } catch { return '' }
}

// 待办事项 (localStorage 持久化)
const TODO_KEY = 'workspace_todos'

const loadTodos = () => {
  try {
    const raw = localStorage.getItem(TODO_KEY)
    const items = raw ? JSON.parse(raw) : []
    todoList.value = items.map((item: any) => ({
      id: ++todoIdCounter,
      text: item.text,
      done: item.done
    }))
  } catch { todoList.value = [] }
}

const saveTodos = () => {
  localStorage.setItem(TODO_KEY, JSON.stringify(todoList.value.map(t => ({ text: t.text, done: t.done }))))
}

const addTodo = () => {
  const text = newTodoText.value.trim()
  if (!text) return
  todoList.value.unshift({ id: ++todoIdCounter, text, done: false })
  newTodoText.value = ''
  saveTodos()
}

const toggleTodo = (idx: number) => {
  const item = todoList.value[idx]
  if (item) { item.done = !item.done }
  saveTodos()
}

const removeTodo = (idx: number) => {
  todoList.value.splice(idx, 1)
  saveTodos()
}

// 加载工作台数据
const loadWorkspace = async () => {
  try {
    const res = await request.get('/dashboard/workspace')
    const data = res?.data || res || {}
    workspaceData.value = data
    recentOps.value = data.recentOperations || []
    quickActions.value = data.quickActions || []
    announcements.value = data.announcements || []
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  loadTodos()
  loadWorkspace()
})
</script>

<style scoped>
.workspace-page {
}

/* 问候区 */
.greeting-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-hover) 100%);
  border-radius: var(--radius-lg, 12px);
  color: #fff;
}

.greeting-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.greeting-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
}

.greeting-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  opacity: 0.85;
  display: flex;
  align-items: center;
  gap: 12px;
}

.dept-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: rgba(255,255,255,0.2);
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
}

.greeting-right {
  display: flex;
  gap: 28px;
  align-items: center;
}

.greeting-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: transform 0.2s;
}

.greeting-stat:hover {
  transform: scale(1.05);
}

.greeting-stat-value {
  font-size: 24px;
  font-weight: 700;
}

.greeting-stat-label {
  font-size: 12px;
  opacity: 0.8;
}

/* 统计卡片 */
.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: var(--bg-primary, #fff);
  border-radius: var(--radius-md, 8px);
  border: 1px solid var(--border-light, #f0f0f0);
  border-left: 3px solid;
  transition: all 0.2s;
}

.stat-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  transform: translateY(-1px);
}

.stat-card-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary, #1a1a2e);
  line-height: 1.2;
}

.stat-card-label {
  font-size: 12px;
  color: var(--text-secondary, #8c8c8c);
}

/* 卡片通用 */
.workspace-card {
  border-radius: var(--radius-lg, 12px) !important;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 15px;
  color: var(--text-primary, #1a1a2e);
}

/* 待办事项 */
.todo-input-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.todo-list {
  max-height: 220px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: var(--bg-secondary, #fafafa);
  border-radius: 6px;
  transition: all 0.2s;
}

.todo-item:hover {
  background: var(--bg-hover, #f0f0f0);
}

.todo-done .todo-text {
  text-decoration: line-through;
  color: #999;
}

.todo-text {
  flex: 1;
  font-size: 13px;
  color: var(--text-primary, #333);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 待办事项添加/删除微动画 */
.todo-enter-active {
  transition: all 0.3s ease-out;
}

.todo-leave-active {
  transition: all 0.25s ease-in;
}

.todo-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.todo-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

.todo-move {
  transition: transform 0.3s ease;
}

/* 公告 */
.announcement-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.announcement-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: var(--bg-secondary, #fafafa);
  border-radius: 6px;
  font-size: 13px;
}

.announcement-title {
  flex: 1;
  color: var(--text-primary, #333);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.announcement-time {
  color: #999;
  font-size: 12px;
  flex-shrink: 0;
}

/* 快捷操作 */
.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.quick-action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 8px;
  border-radius: 8px;
  background: var(--bg-secondary, #f5f7fa);
  cursor: pointer;
  transition: all 0.2s;
}

.quick-action-item:hover {
  background: #e8f4ff;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0,102,255,0.1);
}

.quick-action-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-primary, #fff);
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.quick-action-label {
  font-size: 12px;
  color: var(--text-primary, #333);
  text-align: center;
}

/* 流程概览 */
.pipeline-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 16px;
}

.pipeline-stat-item {
  text-align: center;
  padding: 12px 0;
  background: var(--bg-secondary, #fafafa);
  border-radius: 8px;
}

.pipeline-stat-num {
  font-size: 22px;
  font-weight: 700;
}

.pipeline-stat-label {
  font-size: 12px;
  color: var(--text-secondary, #999);
  margin-top: 2px;
}

/* 迷你柱状图 */
.trend-section {
  border-top: 1px solid var(--border-light, #f0f0f0);
  padding-top: 12px;
}

.trend-title {
  font-size: 12px;
  color: var(--text-secondary, #999);
  margin-bottom: 10px;
}

.mini-bar-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 80px;
  gap: 4px;
}

.mini-bar-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  height: 100%;
}

.mini-bar {
  width: 100%;
  max-width: 28px;
  background: linear-gradient(180deg, var(--color-primary) 0%, var(--color-primary-hover) 100%);
  border-radius: 3px 3px 0 0;
  min-height: 4px;
  transition: height 0.5s ease;
}

.mini-bar-label {
  font-size: 10px;
  color: #999;
  margin-top: 4px;
}

/* 时间线列表 */
.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  max-height: 520px;
  overflow-y: auto;
}

.timeline-item {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-light, #f5f5f5);
}

.timeline-item:last-child {
  border-bottom: none;
}

.timeline-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
  background: #ccc;
}

.dot-success { background: #18a058; }
.dot-info { background: var(--color-primary); }
.dot-error { background: #d03050; }
.dot-warning { background: #f0a020; }

.timeline-content {
  flex: 1;
  min-width: 0;
}

.timeline-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.timeline-module {
  font-size: 13px;
  color: var(--text-secondary, #666);
}

.timeline-desc {
  font-size: 13px;
  color: var(--text-primary, #333);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.timeline-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}




























/* 响应式 */
@media (max-width: 768px) {
  .workspace-page {
    padding: 0;
  }

  .greeting-banner {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    padding: 16px !important;
    border-radius: 14px !important;
    margin-bottom: 12px;
  }

  .greeting-avatar .n-avatar {
    width: 44px !important;
    height: 44px !important;
    font-size: 18px !important;
  }

  .greeting-title {
    font-size: 18px !important;
  }

  .greeting-subtitle {
    font-size: 12px !important;
  }

  .greeting-right {
    width: 100%;
    justify-content: space-around;
  }

  .greeting-stat-value {
    font-size: 20px !important;
  }

  .greeting-stat-label {
    font-size: 11px !important;
  }

  /* 统计卡片横向滚动 */
  .stat-card {
    min-width: 130px;
    padding: 12px !important;
    border-radius: 12px !important;
  }

  .stat-card-value {
    font-size: 20px !important;
  }

  .stat-card-label {
    font-size: 11px !important;
  }

  .workspace-card {
    border-radius: 14px !important;
  }

  .quick-actions-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 8px !important;
  }

  .quick-action-item {
    padding: 12px 8px !important;
    border-radius: 10px !important;
  }

  .quick-action-label {
    font-size: 11px !important;
  }

  .pipeline-overview {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px !important;
  }

  .pipeline-stat {
    padding: 10px !important;
    border-radius: 10px !important;
  }

  .timeline-desc {
    font-size: 12px;
  }
}
</style>

<style>
/* Workspace 深色模式（非 scoped） */
html.dark .greeting-banner {
  background: linear-gradient(135deg, #1e40af 0%, #4338ca 100%) !important;
}
html.dark .stat-card {
  background: #1e293b !important;
  border-color: #334155 !important;
}
html.dark .stat-card-value {
  color: #f1f5f9 !important;
}
html.dark .stat-card-label {
  color: #94a3b8 !important;
}
html.dark .workspace-card {
  background: #1e293b !important;
  border-color: #334155 !important;
}
html.dark .card-title {
  color: #f1f5f9 !important;
}
html.dark .todo-item {
  background: #0f172a !important;
}
html.dark .todo-item:hover {
  background: #1e293b !important;
}
html.dark .todo-text {
  color: #e2e8f0 !important;
}
html.dark .todo-done .todo-text {
  color: #64748b !important;
}
html.dark .announcement-item {
  background: #0f172a !important;
}
html.dark .announcement-title {
  color: #e2e8f0 !important;
}
html.dark .announcement-time {
  color: #64748b !important;
}
html.dark .quick-action-item {
  background: #0f172a !important;
}
html.dark .quick-action-item:hover {
  background: #1e3a5f !important;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2) !important;
}
html.dark .quick-action-label {
  color: #e2e8f0 !important;
}
html.dark .pipeline-stat-item {
  background: #0f172a !important;
}
html.dark .pipeline-stat-num {
  color: #f1f5f9 !important;
}
html.dark .pipeline-stat-label {
  color: #94a3b8 !important;
}
html.dark .trend-section {
  border-top-color: #334155 !important;
}
html.dark .trend-title {
  color: #94a3b8 !important;
}
html.dark .mini-bar {
  background: linear-gradient(180deg, var(--color-primary) 0%, var(--color-primary-hover) 100%) !important;
}
html.dark .mini-bar-label {
  color: #64748b !important;
}
html.dark .timeline-item {
  border-bottom-color: #334155 !important;
}
html.dark .timeline-module {
  color: #94a3b8 !important;
}
html.dark .timeline-desc {
  color: #e2e8f0 !important;
}
html.dark .timeline-time {
  color: #64748b !important;
}
</style>

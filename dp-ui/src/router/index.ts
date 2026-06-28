/**
 * 路由配置
 * 参考若依框架实现
 * 
 * 需求: 1.9, 1.10 - 路由懒加载和组件按需加载
 * - 使用动态导入实现路由懒加载
 * - Vite 自动进行代码分割
 * - 按功能模块分组，减少初始包体积
 */
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useTabsStore } from '@/stores/tabs'
import { checkRoutePermission } from '@/directives/permission'
import { message } from '@/utils/message'

// 白名单路由（不需要登录）
const whiteList = ['/login', '/embed/chart', '/s']

/**
 * 路由组件分组说明：
 * - core: 核心组件（登录、布局、仪表盘）
 * - data: 数据管理相关（数据源、采集、导入）
 * - datax: DataX 数据传输模块
 * - pipeline: 数据流程模块
 * - report: 报表相关（报表管理、设计器、查看）
 * - chart: 图表相关（图表管理、设计器、查看）
 * - page: 页面设计相关
 * - system: 系统管理（用户、角色、菜单、监控）
 * - log: 日志相关
 */

const routes: RouteRecordRaw[] = [
  // ==================== 核心路由 (core chunk) ====================
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/redirect/:path(.*)',
    name: 'Redirect',
    component: () => import('@/views/RedirectView.vue'),
    meta: { requiresAuth: true }
  },
  
  // ==================== 嵌入路由 (embed chunk) ====================
  {
    path: '/embed/chart/:id',
    name: 'ChartEmbed',
    component: () => import('@/views/ChartEmbed.vue'),
    meta: { requiresAuth: false }
  },
  
  // ==================== 全屏页面路由（不使用主布局） ====================
  {
    path: '/bigscreen-view/:id',
    name: 'BigscreenView',
    component: () => import('@/views/BigscreenView.vue'),
    meta: { title: '大屏查看', requiresAuth: true, fullscreen: true }
  },
  
  // ==================== 主布局路由 ====================
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    redirect: '/dashboard',
    children: [
      // ==================== 仪表盘 (dashboard chunk) ====================
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘', requiresAuth: true }
      },
      {
        path: 'workspace',
        name: 'Workspace',
        component: () => import('@/views/Workspace.vue'),
        meta: { title: '个人工作台', requiresAuth: true }
      },
      // ==================== 数据管理 (data chunk) ====================
      {
        path: 'data-source',
        name: 'DataSource',
        component: () => import('@/views/DataSource.vue'),
        meta: { title: '数据源管理', requiresAuth: true, permission: 'data:source' }
      },
      {
        path: 'data-collect',
        name: 'DataCollect',
        component: () => import('@/views/DataCollect.vue'),
        meta: { title: '采集任务', requiresAuth: true, permission: 'data:collect' }
      },
      {
        path: 'data-import',
        name: 'DataImport',
        component: () => import('@/views/DataImport.vue'),
        meta: { title: '数据导入', requiresAuth: true, permission: 'data:import' }
      },
      {
        path: 'collect-log',
        name: 'CollectLog',
        component: () => import('@/views/CollectLog.vue'),
        meta: { title: '采集日志', requiresAuth: true, permission: 'data:collect:log' }
      },
      {
        path: 'db-manager',
        name: 'DatabaseManager',
        component: () => import('@/views/DatabaseManager.vue'),
        meta: { title: '数据库管理', requiresAuth: true, permission: 'db:manager' }
      },
      {
        path: 'table-data-manage',
        name: 'TableDataManage',
        component: () => import('@/views/TableDataManage.vue'),
        meta: { title: '数据管理', requiresAuth: true, permission: 'tabledata:manage' }
      },
      {
        path: 'data-view-manage',
        name: 'DataViewManage',
        component: () => import('@/views/DataViewManage.vue'),
        meta: { title: '数据管理', requiresAuth: true, permission: 'dataview:manage' }
      },
      {
        path: 'data-lineage',
        name: 'DataLineage',
        component: () => import('@/views/DataLineage.vue'),
        meta: { title: '数据血缘', requiresAuth: true, permission: 'data:lineage' }
      },
      {
        path: 'data-view/:code',
        name: 'DataViewPage',
        component: () => import('@/views/DataViewPage.vue'),
        meta: { title: '数据管理', requiresAuth: true }
      },
      {
        path: 'data-dictionary',
        name: 'DataDictionary',
        component: () => import('@/views/system/dict/index.vue'),
        meta: { title: '数据字典', requiresAuth: true, permission: 'dict:manage' }
      },
      {
        path: 'data-sync',
        name: 'DataSyncManage',
        component: () => import('@/views/DataSyncManage.vue'),
        meta: { title: '数据同步', requiresAuth: true, permission: 'datasync:manage' }
      },
      
      // ==================== DataX 数据传输 (datax chunk) ====================
      {
        path: 'datax',
        name: 'DataxOverview',
        component: () => import('@/views/DataxOverview.vue'),
        meta: { title: '概览', requiresAuth: true, permission: 'datax:job' }
      },
      {
        path: 'datax/datasource',
        name: 'DataxDataSource',
        component: () => import('@/views/DataSource.vue'),
        meta: { title: '数据源管理', requiresAuth: true, permission: 'data:source' }
      },
      {
        path: 'datax/job',
        name: 'DataxJob',
        component: () => import('@/views/DataTransfer.vue'),
        meta: { title: '传输任务', requiresAuth: true, permission: 'datax:job' }
      },
      {
        path: 'datax/log',
        name: 'DataxLog',
        component: () => import('@/views/DataTransferLog.vue'),
        meta: { title: '执行日志', requiresAuth: true, permission: 'datax:log' }
      },
      
      // ==================== 数据流程 (pipeline chunk) ====================
      {
        path: 'pipeline/manage',
        name: 'PipelineManage',
        component: () => import('@/views/PipelineManage.vue'),
        meta: { title: '流程管理', requiresAuth: true, permission: 'pipeline:manage' }
      },
      {
        path: 'pipeline/designer/:id?',
        name: 'PipelineDesigner',
        component: () => import('@/views/PipelineDesigner.vue'),
        meta: { title: '流程设计器', requiresAuth: true, permission: ['pipeline:manage', 'pipeline:design'] }
      },
      {
        path: 'pipeline/monitor',
        name: 'PipelineMonitor',
        component: () => import('@/views/PipelineMonitor.vue'),
        meta: { title: '执行监控', requiresAuth: true, permission: 'pipeline:execute' }
      },
      {
        path: 'pipeline/log',
        name: 'PipelineLog',
        component: () => import('@/views/PipelineLog.vue'),
        meta: { title: '执行日志', requiresAuth: true, permission: 'pipeline:log' }
      },
      
      // ==================== 报表模块 (report chunk) ====================
      {
        path: 'report',
        name: 'Report',
        component: () => import('@/views/Report.vue'),
        meta: { title: '报表查询', requiresAuth: true, permission: 'report:query' }
      },
      {
        path: 'report-manage',
        name: 'ReportManage',
        component: () => import('@/views/ReportManage.vue'),
        meta: { title: '报表管理', requiresAuth: true, permission: 'report:manage' }
      },
      {
        path: 'report-designer/:id?',
        name: 'ReportDesigner',
        component: () => import('@/views/ReportDesigner.vue'),
        meta: { title: '报表设计器', requiresAuth: true, permission: ['report:manage', 'report:design'] }
      },
      {
        path: 'report-view/:id',
        name: 'ReportView',
        component: () => import('@/views/DynamicReport.vue'),
        meta: { title: '报表查看', requiresAuth: true }
      },
      {
        path: 'report-view-code/:code',
        name: 'ReportViewCode',
        component: () => import('@/views/DynamicReport.vue'),
        meta: { title: '报表查看', requiresAuth: true }
      },
      {
        path: 'report-version',
        name: 'ReportVersion',
        component: () => import('@/views/ReportVersion.vue'),
        meta: { title: '报表版本', requiresAuth: true, permission: 'report:version' }
      },
      
      // ==================== 图表模块 (chart chunk) ====================
      {
        path: 'chart-manage',
        name: 'ChartManage',
        component: () => import('@/views/ChartManage.vue'),
        meta: { title: '图表资源库', requiresAuth: true, permission: 'chart:manage' }
      },
      {
        path: 'chart-center',
        name: 'ChartCenter',
        component: () => import('@/views/ChartCenter.vue'),
        meta: { title: '图表中心', requiresAuth: true }
      },
      {
        path: 'chart-center/:id',
        name: 'ChartCenterView',
        component: () => import('@/views/ChartCenter.vue'),
        meta: { title: '图表查看', requiresAuth: true }
      },
      {
        path: 'chart-view/:id',
        name: 'DynamicChart',
        component: () => import('@/views/DynamicChart.vue'),
        meta: { title: '图表查看', requiresAuth: true }
      },
      {
        path: 'chart-designer/:id?',
        name: 'ChartDesigner',
        component: () => import('@/views/ChartDesigner.vue'),
        meta: { title: '图表设计器', requiresAuth: true, permission: ['chart:manage', 'chart:design'] }
      },
      {
        path: 'ai-chart-design',
        name: 'AiChartDesign',
        component: () => import('@/views/AiChartDesign.vue'),
        meta: { title: 'AI智能图表设计', requiresAuth: true, permission: 'chart:design' }
      },
      
      // ==================== 页面设计 (page chunk) ====================
      {
        path: 'page-manage',
        name: 'PageManage',
        component: () => import('@/views/PageManage.vue'),
        meta: { title: '页面管理', requiresAuth: true, permission: 'page:manage' }
      },
      {
        path: 'page-designer/:id?',
        name: 'PageDesigner',
        component: () => import('@/views/PageDesigner.vue'),
        meta: { title: '页面设计器', requiresAuth: true, permission: ['page:manage', 'page:design'] }
      },
      {
        path: 'page-view/:id',
        name: 'PageView',
        component: () => import('@/views/PageView.vue'),
        meta: { title: '页面查看', requiresAuth: true }
      },
      
      // ==================== 系统管理 (system chunk) ====================
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', requiresAuth: true, permission: 'user:manage' }
      },
      {
        path: 'role',
        name: 'Role',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', requiresAuth: true, permission: 'role:manage' }
      },
      {
        path: 'menu-manage',
        name: 'MenuManage',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', requiresAuth: true, permission: 'menu:manage' }
      },
      {
        path: 'department-manage',
        name: 'DepartmentManage',
        component: () => import('@/views/org/dept/index.vue'),
        meta: { title: '部门管理', requiresAuth: true, permission: 'department:manage' }
      },
      {
        path: 'post',
        name: 'PostManage',
        component: () => import('@/views/org/post/index.vue'),
        meta: { title: '岗位管理', requiresAuth: true, permission: 'post:manage' }
      },
      {
        path: 'system-config',
        name: 'SystemConfig',
        component: () => import('@/views/system/config/index.vue'),
        meta: { title: '系统配置', requiresAuth: true, permission: 'system:config' }
      },
      
      // ==================== 系统监控 (monitor chunk) ====================
      {
        path: 'system-monitor',
        name: 'SystemMonitor',
        component: () => import('@/views/SystemMonitor.vue'),
        meta: { title: '系统监控', requiresAuth: true, permission: 'system:monitor' }
      },
      {
        path: 'alert-manage',
        name: 'AlertManage',
        component: () => import('@/views/AlertManage.vue'),
        meta: { title: '告警管理', requiresAuth: true, permission: 'system:monitor' }
      },
      {
        path: 'slow-query',
        name: 'SlowQueryAnalysis',
        component: () => import('@/views/SlowQueryAnalysis.vue'),
        meta: { title: '慢查询分析', requiresAuth: true, permission: 'system:monitor' }
      },
      {
        path: 'health-check',
        name: 'HealthCheckView',
        component: () => import('@/views/HealthCheckView.vue'),
        meta: { title: '健康检查', requiresAuth: true, permission: 'system:monitor' }
      },
      
      // ==================== 日志管理 (log chunk) ====================
      {
        path: 'operation-log',
        name: 'OperationLog',
        component: () => import('@/views/log/operlog/index.vue'),
        meta: { title: '操作日志', requiresAuth: true, permission: 'log:operation' }
      },
      {
        path: 'login-log',
        name: 'LoginLog',
        component: () => import('@/views/log/loginlog/index.vue'),
        meta: { title: '登录日志', requiresAuth: true, permission: 'log:login' }
      },
      
      // ==================== 通知与消息 (notification chunk) ====================
      {
        path: 'message-channel',
        name: 'MessageChannel',
        component: () => import('@/views/MessageChannel.vue'),
        meta: { title: '消息通道', requiresAuth: true, permission: 'system:config' }
      },
      {
        path: 'announcement-manage',
        name: 'AnnouncementManage',
        component: () => import('@/views/AnnouncementManage.vue'),
        meta: { title: '公告管理', requiresAuth: true, permission: 'announcement:manage' }
      },
      
      // ==================== 用户设置 (user-settings chunk) ====================
      {
        path: 'change-password',
        name: 'ChangePassword',
        component: () => import('@/views/ChangePassword.vue'),
        meta: { title: '修改密码', requiresAuth: true }
      },
      {
        path: 'export-center',
        name: 'ExportCenter',
        component: () => import('@/views/ExportCenterPage.vue'),
        meta: { title: '导出中心', requiresAuth: true }
      },
      
      // ==================== 团队空间 (team chunk) ====================
      {
        path: 'team-space',
        name: 'TeamSpace',
        component: () => import('@/views/team/TeamSpaceView.vue'),
        meta: { title: '团队空间', requiresAuth: true }
      },
      
      // ==================== 商业化 (commercial chunk) ====================
      
      // ==================== 技术支持 (support chunk) ====================
      {
        path: 'ticket-manage',
        name: 'TicketManage',
        component: () => import('@/views/TicketManage.vue'),
        meta: { title: '工单管理', requiresAuth: true, permission: 'ticket:manage' }
      },
      {
        path: 'knowledge-base',
        name: 'KnowledgeBase',
        component: () => import('@/views/KnowledgeBase.vue'),
        meta: { title: '知识库', requiresAuth: true, permission: 'ticket:knowledge' }
      },
      
      // ==================== 数据质量 (data-quality chunk) ====================
      {
        path: 'data-quality',
        name: 'DataQuality',
        component: () => import('@/views/DataQuality.vue'),
        meta: { title: '数据质量', requiresAuth: true, permission: 'data:quality' }
      },
      {
        path: 'query-builder',
        name: 'QueryBuilder',
        component: () => import('@/views/QueryBuilder.vue'),
        meta: { title: '查询构建器', requiresAuth: true, permission: 'query:builder' }
      },
      {
        path: 'rls-config',
        name: 'RlsConfig',
        component: () => import('@/views/RlsConfig.vue'),
        meta: { title: '行级安全配置', requiresAuth: true, permission: 'rls:config' }
      },
      
      // ==================== AI 智能 (ai chunk) ====================
      {
        path: 'ai-assistant',
        name: 'AiAssistant',
        component: () => import('@/views/AiAssistant.vue'),
        meta: { title: 'AI 助手', requiresAuth: true, permission: 'ai:assistant' }
      },
      
      // ==================== 即时通讯 (chat chunk) ====================
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('@/views/ChatView.vue'),
        meta: { title: '即时通讯', requiresAuth: true, permission: 'chat:conversation' }
      },
      
      // ==================== 运维工具 (ops-tools chunk) ====================
      {
        path: 'usage-stats',
        name: 'UsageStats',
        component: () => import('@/views/UsageStats.vue'),
        meta: { title: '使用统计', requiresAuth: true, permission: 'usage:stats' }
      },
      {
        path: 'upgrade-manage',
        name: 'UpgradeManage',
        component: () => import('@/views/UpgradeManage.vue'),
        meta: { title: '在线升级', requiresAuth: true, permission: 'upgrade:manage' }
      },
      {
        path: 'ops-manage',
        name: 'OpsManage',
        component: () => import('@/views/OpsManage.vue'),
        meta: { title: '运维管理', requiresAuth: true, permission: 'ops:manage' }
      }
    ]
  },
  
  // ==================== 移动端专用路由 (mobile chunk) ====================
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'm/charts',
        name: 'MobileChartCenter',
        component: () => import('@/views/mobile/MobileChartCenter.vue'),
        meta: { title: '图表中心', requiresAuth: true }
      },
      {
        path: 'm/chart/:id',
        name: 'MobileChartView',
        component: () => import('@/views/mobile/MobileChartView.vue'),
        meta: { title: '图表查看', requiresAuth: true }
      },
      {
        path: 'm/reports',
        name: 'MobileReportCenter',
        component: () => import('@/views/mobile/MobileReportCenter.vue'),
        meta: { title: '报表中心', requiresAuth: true }
      },
      {
        path: 'm/report/:id',
        name: 'MobileReportView',
        component: () => import('@/views/mobile/MobileReportView.vue'),
        meta: { title: '报表查看', requiresAuth: true }
      },
      {
        path: 'm/profile',
        name: 'MobileProfile',
        component: () => import('@/views/mobile/MobileProfilePage.vue'),
        meta: { title: '我的', requiresAuth: true }
      },
      {
        path: 'm/pages',
        name: 'MobileDashboard',
        component: () => import('@/views/mobile/MobileDashboard.vue'),
        meta: { title: '页面中心', requiresAuth: true }
      },
      {
        path: 'm/page/:id',
        name: 'MobilePageView',
        component: () => import('@/views/mobile/MobilePageView.vue'),
        meta: { title: '页面查看', requiresAuth: true }
      },
      {
        path: 'm/data-views',
        name: 'MobileDataViewCenter',
        component: () => import('@/views/mobile/MobileDataViewCenter.vue'),
        meta: { title: '数据管理', requiresAuth: true }
      },
      {
        path: 'm/data-view/:code',
        name: 'MobileDataViewPage',
        component: () => import('@/views/mobile/MobileDataViewPage.vue'),
        meta: { title: '数据管理', requiresAuth: true }
      }
    ]
  },

  // ==================== 公开分享 (share chunk, 无需认证) ====================
  {
    path: '/s/:token',
    name: 'PublicShare',
    component: () => import('@/views/share/PublicShareView.vue'),
    meta: { title: '公开分享', requiresAuth: false }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 是否已获取用户信息
let isUserInfoFetched = false

/**
 * 重置路由状态（登出时调用）
 */
export function resetRouter() {
  isUserInfoFetched = false
}

/**
 * 统一设备类型判断（与 appStore.isMobileView 保持一致）
 * 优先读取 localStorage 中用户手动设定的模式，否则按屏幕宽度自动判断
 */
function getIsMobileDevice(): boolean {
  const mobileMode = typeof localStorage !== 'undefined' ? localStorage.getItem('dp-mobile-mode') : null
  if (mobileMode === 'mobile') return true
  if (mobileMode === 'desktop') return false
  return window.innerWidth < 768
}

/**
 * 设备自适应路由重定向
 * 移动端访问桌面路由 → 重定向到移动端对应路由
 * 桌面端访问移动端路由 → 重定向到桌面端对应路由
 */
function getMobileRedirect(path: string, isMobile: boolean): string | null {
  if (isMobile) {
    // 移动端访问桌面路由，重定向到移动端
    if (path === '/' || path === '/dashboard') return '/m/pages'
    const reportMatch = path.match(/^\/report-view\/(\d+)$/)
    if (reportMatch) return `/m/report/${reportMatch[1]}`
    const chartMatch = path.match(/^\/chart-view\/(\d+)$/)
    if (chartMatch) return `/m/chart/${chartMatch[1]}`
    const pageMatch = path.match(/^\/page-view\/(\d+)$/)
    if (pageMatch) return `/m/page/${pageMatch[1]}`
    // 图表中心也重定向
    if (path === '/chart-center') return '/m/charts'
    // 数据管理重定向
    if (path === '/data-view-manage') return '/m/data-views'
    const dvMatch = path.match(/^\/data-view\/(.+)$/)
    if (dvMatch) return `/m/data-view/${dvMatch[1]}`
  } else {
    // 桌面端访问移动端路由，重定向到桌面端
    if (path === '/m/pages') return '/dashboard'
    if (path === '/m/charts') return '/chart-center'
    if (path === '/m/reports') return '/dashboard'
    if (path === '/m/data-views') return '/data-view-manage'
    if (path === '/m/profile') return '/dashboard'
    const mReportMatch = path.match(/^\/m\/report\/(\d+)$/)
    if (mReportMatch) return `/report-view/${mReportMatch[1]}`
    const mChartMatch = path.match(/^\/m\/chart\/(\d+)$/)
    if (mChartMatch) return `/chart-view/${mChartMatch[1]}`
    const mPageMatch = path.match(/^\/m\/page\/(\d+)$/)
    if (mPageMatch) return `/page-view/${mPageMatch[1]}`
    const mDvMatch = path.match(/^\/m\/data-view\/(.+)$/)
    if (mDvMatch) return `/data-view/${mDvMatch[1]}`
  }
  return null
}

// 路由守卫
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  const tabsStore = useTabsStore()
  
  // 获取 token
  const hasToken = userStore.getToken()
  
  if (hasToken) {
    if (to.path === '/login') {
      // 已登录，根据设备类型跳转到对应首页
      const homePath = getIsMobileDevice() ? '/m/pages' : '/'
      next({ path: homePath })
    } else {
      // 检查会话超时（需求 19.3）
      if (userStore.checkSessionTimeout()) {
        // 会话超时，清理状态并重定向登录页（携带超时标识）
        message.warning?.('会话已超时，请重新登录')
        await handleLogout(userStore, tabsStore)
        next({ path: '/login', query: { redirect: to.fullPath, timeout: 'true' } })
        return
      }
      
      // 检查是否已获取用户信息
      if (!isUserInfoFetched) {
        try {
          // 获取用户信息
          await userStore.fetchUserInfo()
          isUserInfoFetched = true
          
          // 初始化标签页
          tabsStore.initTabs()
          
          // 初始化会话管理
          userStore.initSessionManagement()
          
          // 重新导航到目标路由
          next({ ...to, replace: true })
        } catch (error) {
          // 获取用户信息失败，清理状态并跳转登录
          console.error('获取用户信息失败:', error)
          await handleLogout(userStore, tabsStore)
          next({ path: '/login', query: { redirect: to.fullPath } })
        }
      } else {
        // 强制改密检查（需求 19.6）：mustChangePassword 为 true 时强制重定向修改密码页
        if (userStore.mustChangePassword && to.path !== '/change-password') {
          message.warning?.('请先修改密码后再继续操作')
          next({ path: '/change-password', replace: true })
          return
        }
        
        // 设备自适应路由重定向：移动端访问桌面路由自动跳转移动端，反之亦然
        const isMobileDevice = getIsMobileDevice()
        const mobileRedirect = getMobileRedirect(to.path, isMobileDevice)
        if (mobileRedirect) {
          next({ path: mobileRedirect, replace: true })
          return
        }
        
        // 路由权限检查（需求 19.2）：使用 checkRoutePermission 统一检查
        if (!checkRoutePermission(to)) {
          message.warning?.('权限不足，无法访问该页面')
          next({ path: isMobileDevice ? '/m/pages' : '/dashboard' })
        } else {
          next()
        }
      }
    }
  } else {
    // 没有 token
    if (whiteList.some(w => to.path === w || to.path.startsWith(w + '/'))) {
      // 在白名单中，直接进入
      next()
    } else {
      // 不在白名单中，跳转登录
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
  }
})

/**
 * 处理登出逻辑
 */
async function handleLogout(userStore: ReturnType<typeof useUserStore>, tabsStore: ReturnType<typeof useTabsStore>) {
  // 清理标签页
  tabsStore.clearTabs()
  // 清理用户状态
  userStore.logout()
  // 重置路由状态
  resetRouter()
}

// 监听 storage 事件，当其他标签页清除 token 时同步
if (typeof window !== 'undefined') {
  window.addEventListener('storage', (event) => {
    if (event.key === 'token' && !event.newValue) {
      resetRouter()
      const userStore = useUserStore()
      const tabsStore = useTabsStore()
      tabsStore.clearTabs()
      userStore.resetState()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
  })
}

export default router

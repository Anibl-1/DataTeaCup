<template>
  <n-layout has-sider class="main-layout">
    <!-- 桌面端侧边栏（强制改密模式下隐藏） -->
    <n-layout-sider
      v-if="!isMobile && !userStore.mustChangePassword"
      bordered
      collapse-mode="width"
      :collapsed-width="64"
      :width="240"
      :collapsed="collapsed"
      show-trigger
      @collapse="collapsed = true"
      @expand="collapsed = false"
    >
      <div class="sidebar-wrapper">
        <div class="sidebar-top">
          <div class="logo">
            <div class="logo-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path d="M2 12h3l3-8 4 16 3-12 2 4h5" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <Transition name="sidebar-text">
              <span v-if="!collapsed" class="logo-text">DataTeaCup</span>
            </Transition>
          </div>
          <n-menu
            v-model:value="activeKey"
            :collapsed="collapsed"
            :options="menuOptions"
            :default-expanded-keys="[]"
            @update:value="handleMenuSelect"
          />
        </div>
        <Transition name="sidebar-text">
        <div v-if="!collapsed" class="sidebar-footer">
          <div class="sidebar-version">{{ sidebarVersion }}</div>
          <div class="sidebar-copyright">{{ sidebarCopyright }}</div>
        </div>
        </Transition>
      </div>
    </n-layout-sider>

    <n-layout :native-scrollbar="true">
      <!-- 顶部导航栏（移动端完全隐藏，由各移动端页面自行提供头部） -->
      <n-layout-header v-if="!isMobile && !userStore.mustChangePassword" class="header">
        <div class="header-left">
          <div class="hamburger" @click="handleHamburgerClick">
            <n-icon size="20">
              <MenuOutline />
            </n-icon>
          </div>
          <n-breadcrumb v-if="breadcrumbItems.length > 0" class="breadcrumb">
            <n-breadcrumb-item v-for="item in breadcrumbItems" :key="item.key" @click="item.onClick">
              {{ item.label }}
            </n-breadcrumb-item>
          </n-breadcrumb>
        </div>
        <div class="header-right">
          <!-- 工具栏组 -->
          <n-tooltip trigger="hover">
            <template #trigger>
              <div class="header-action" @click="showCommandPalette = true">
                <n-icon size="18"><SearchOutline /></n-icon>
              </div>
            </template>
            <span>{{ t('layout.searchTip') }} <kbd class="header-kbd">Ctrl+K</kbd></span>
          </n-tooltip>
          <div class="header-action" @click="refreshCurrent">
            <n-icon size="18"><RefreshOutline /></n-icon>
          </div>
          <n-tooltip trigger="hover">
            <template #trigger>
              <div class="header-action" @click="goToExportCenter">
                <n-icon size="18"><DownloadOutline /></n-icon>
              </div>
            </template>
            {{ t('common.export') }}
          </n-tooltip>
          <div class="header-divider" />
          <!-- 聊天按钮 (移动端隐藏，用底部Tab代替) -->
          <n-badge :value="chatStore.totalUnread" :max="99" :offset="[-6, 4]" :show="chatStore.totalUnread > 0" type="error">
            <div class="header-action" @click="chatStore.togglePanel()">
              <n-icon size="18"><ChatbubbleEllipsesOutline /></n-icon>
            </div>
          </n-badge>
          <!-- 通知铃铛 -->
          <n-popover trigger="click" placement="bottom-end" style="width: 360px; padding: 0;" @update:show="(v: boolean) => { if (v) loadNotifications() }">
            <template #trigger>
              <n-badge :value="unreadNotificationCount" :max="99" :offset="[-4, 4]">
                <div class="header-action">
                  <n-icon size="18"><NotificationsOutline /></n-icon>
                </div>
              </n-badge>
            </template>
            <div class="notification-header">
              <span class="notification-title">{{ t('layout.notificationCenter') }}</span>
              <n-button text size="small" :disabled="unreadNotificationCount === 0" @click="handleMarkAllRead">{{ t('layout.markAllRead') }}</n-button>
            </div>
            <n-scrollbar style="max-height: 320px;">
              <div v-if="notifications.length === 0" class="notification-empty">
            <div class="notification-empty-icon">🔔</div>
            <div class="notification-empty-text">{{ t('layout.noNotification') }}</div>
          </div>
              <div
                v-for="item in notifications"
                :key="item.id"
                class="notification-item"
                :class="{ 'notification-item--unread': !item.isRead }"
                @click="handleNotificationClick(item)"
              >
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span class="notification-item-title">{{ item.title }}</span>
                  <n-tag v-if="!item.isRead" type="warning" size="small" round>{{ t('layout.unread') }}</n-tag>
                </div>
                <div class="notification-item-content">{{ item.content }}</div>
                <div class="notification-item-time">{{ item.createTime }}</div>
              </div>
            </n-scrollbar>
          </n-popover>
          <!-- 主题色切换 -->
          <n-popover trigger="click" placement="bottom-end" style="width: 240px;">
            <template #trigger>
              <n-tooltip trigger="hover">
                <template #trigger>
                  <div class="header-action">
                    <n-icon size="18"><ColorPaletteOutline /></n-icon>
                  </div>
                </template>
                {{ t('layout.themeSettings') }}
              </n-tooltip>
            </template>
            <div style="padding: 4px 0;">
              <div style="font-size: 13px; font-weight: 600; margin-bottom: 10px; color: var(--text-primary);">{{ t('layout.themeColor') }}</div>
              <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 14px;">
                <div
                  v-for="preset in COLOR_PRESETS"
                  :key="preset.name"
                  style="display: flex; flex-direction: column; align-items: center; gap: 4px; cursor: pointer; padding: 6px; border-radius: 8px; transition: background 0.2s;"
                  :style="{ background: themeStore.primaryColor === preset.primary ? 'var(--bg-hover, #f0f0f0)' : 'transparent' }"
                  @click="themeStore.setPrimaryColor(preset.primary)"
                >
                  <div
                    style="width: 28px; height: 28px; border-radius: 50%; position: relative;"
                    :style="{ background: preset.primary }"
                  >
                    <svg v-if="themeStore.primaryColor === preset.primary" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3" style="width: 16px; height: 16px; position: absolute; top: 6px; left: 6px;">
                      <polyline points="20 6 9 17 4 12" />
                    </svg>
                  </div>
                  <span style="font-size: 11px; color: var(--text-secondary, #666);">{{ preset.label }}</span>
                </div>
              </div>
              <n-divider style="margin: 8px 0;" />
              <div style="display: flex; align-items: center; justify-content: space-between;">
                <div style="font-size: 13px; font-weight: 600; color: var(--text-primary);">{{ t('layout.darkMode') }}</div>
                <n-switch :value="themeStore.isDark" @update:value="(val: boolean) => themeStore.setTheme(val ? 'dark' : 'light')">
                  <template #checked>
                    <n-icon size="14"><MoonOutline /></n-icon>
                  </template>
                  <template #unchecked>
                    <n-icon size="14"><SunnyOutline /></n-icon>
                  </template>
                </n-switch>
              </div>
            </div>
          </n-popover>
          <!-- 语言切换 -->
          <n-dropdown :options="languageOptions" placement="bottom-end" @select="handleLanguageChange">
            <n-tooltip trigger="hover">
              <template #trigger>
                <div class="header-action language-btn">
                  <n-icon size="18"><LanguageOutline /></n-icon>
                  <span class="language-text">{{ currentLocale === 'zh-CN' ? '中' : 'EN' }}</span>
                </div>
              </template>
              {{ t('language.switch') }}
            </n-tooltip>
          </n-dropdown>
          <div class="header-divider" />
          <n-dropdown :options="userOptions" @select="handleUserSelect">
            <div class="user-info">
              <div class="user-avatar-wrapper">
                <n-avatar round size="small" :style="userAvatarPreset ? { background: userAvatarPreset.gradient } : { background: 'var(--dp-gradient-primary, linear-gradient(135deg,#2563eb,#1e40af))' }">
                  {{ userAvatarPreset ? userAvatarPreset.icon : (userStore.userInfo?.nickname || '用')[0] }}
                </n-avatar>
                <span class="user-online-dot" />
              </div>
              <span class="user-name">{{ userStore.userInfo?.nickname || t('layout.userDefault') }}</span>
            </div>
          </n-dropdown>
        </div>
      </n-layout-header>
      <!-- 标签页导航 (移动端隐藏) -->
      <TabsNav v-if="!isMobile && !userStore.mustChangePassword" />
      <n-layout-content class="content" :class="{ 'mobile-content': isMobile }">
        <ErrorBoundary>
          <router-view v-slot="{ Component, route: currentRoute }">
            <Transition name="page-fade" mode="out-in">
              <keep-alive :include="tabsStore.cachedViews" :max="6">
                <component :is="Component" :key="currentRoute.fullPath" />
              </keep-alive>
            </Transition>
          </router-view>
        </ErrorBoundary>
      </n-layout-content>
      <!-- 底部版权信息 (移动端隐藏) -->
      <n-layout-footer v-if="!isMobile" class="footer">
        <span>© {{ new Date().getFullYear() }} DataTeaCup</span>
        <span class="footer-sep">|</span>
        <span>Powered by DataTeaCup</span>
      </n-layout-footer>
    </n-layout>
    
    <!-- AI助手悬浮按钮 (移动端隐藏) -->
    <AiChat v-if="!isMobile && !userStore.mustChangePassword" />

    <!-- 聊天面板 -->
    <ChatPanel v-if="!userStore.mustChangePassword" />

    <!-- 移动端底部导航（详情页隐藏） -->
    <MobileTabBar v-if="isMobile && !isMobileSubpage" />
    
    <!-- 移动端/桌面端模式切换器已移除，切换功能保留在"我的"页面设置中 -->

    <!-- 全局搜索面板 (Ctrl+K) -->
    <CommandPalette v-model="showCommandPalette" :menu-options="menuOptions" />
  </n-layout>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, h, watch, onMounted, onUnmounted, defineAsyncComponent } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NIcon, NAvatar, NSwitch, useMessage } from 'naive-ui'
import { useI18n, initLocale, setLocale, currentLocale, type Locale } from '@/i18n'
import { LanguageOutline, ColorPaletteOutline, MoonOutline, SunnyOutline } from '@vicons/ionicons5'
import TabsNav from '@/components/TabsNav/TabsNav.vue'
const AiChat = defineAsyncComponent(() => import('@/components/AiChat/AiChat.vue'))
const ChatPanel = defineAsyncComponent(() => import('@/components/Chat/ChatPanel.vue'))
import ErrorBoundary from '@/components/ErrorBoundary.vue'
const CommandPalette = defineAsyncComponent(() => import('@/components/CommandPalette.vue'))
const MobileTabBar = defineAsyncComponent(() => import('@/components/mobile/MobileTabBar.vue'))

import { useTabsStore } from '@/stores/tabs'
import { useAppStore } from '@/stores/app'
import {
  iconMap,
  HomeOutline,
  RefreshOutline,
  DownloadOutline,
  NotificationsOutline,
  MenuOutline,
  LogOutOutline,
  CreateOutline,
  SearchOutline,
  ChatbubbleEllipsesOutline
} from '@/utils/iconMap'
import { useThemeStore, COLOR_PRESETS } from '@/stores/theme'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { avatarPresets } from '@/constants/avatarPresets'
import { useNotificationPush } from '@/composables/useNotificationPush'
import { useChatWebSocket } from '@/composables/useChatWebSocket'
import { logger } from '@/utils/logger'
import { resetRouter } from '@/router'
import { systemMonitorApi } from '@/api/systemMonitor'
import { getConfigByKey } from '@/api/system/systemConfig'
import request from '@/api/request'

const router = useRouter()
const route = useRoute()
const message = useMessage()

// 刷新当前页面
const refreshCurrent = () => {
  router.replace('/redirect' + route.fullPath)
}
const userStore = useUserStore()
const tabsStore = useTabsStore()
const chatStore = useChatStore()
const chatWs = useChatWebSocket()
const themeStore = useThemeStore()
const { unreadNotificationCount, notifications, init: initNotificationPush } = useNotificationPush()

/** 解析头像预设：根据avatar id返回匹配的预设，未匹配返回null */
function resolveAvatarPreset(avatarId?: string | null) {
  if (!avatarId) return null
  return avatarPresets.find(p => p.id === avatarId) || null
}

/** 当前用户的头像预设 */
const userAvatarPreset = computed(() => resolveAvatarPreset(userStore.userInfo?.avatar))

const { t } = useI18n()

const collapsed = ref(false)
const collapsedBeforeFullscreen = ref(false)  // 记录全屏前的状态
const activeKey = ref<string>(route.name as string || '')
const isNavigating = ref(false) // 防止导航循环

// 面包屑导航项
const breadcrumbItems = computed(() => {
  const noop = () => { /* breadcrumb leaf */ }
  const items: { key: string; label: string; onClick: () => void }[] = []
  
  // 根据当前activeKey在menuOptions中查找路径
  const findPath = (menus: any[], targetKey: string, path: any[] = []): any[] | null => {
    for (const menu of menus) {
      const currentPath = [...path, menu]
      if (menu.key === targetKey) return currentPath
      if (menu.children?.length > 0) {
        const found = findPath(menu.children, targetKey, currentPath)
        if (found) return found
      }
    }
    return null
  }
  
  const menuPath = findPath(menuOptions.value, activeKey.value)
  if (menuPath && menuPath.length > 0) {
    menuPath.forEach((menu: any, index: number) => {
      const isLast = index === menuPath.length - 1
      const label = typeof menu.label === 'function' ? menu.key : menu.label
      items.push({
        key: menu.key,
        label: label || menu.key,
        onClick: isLast ? noop : () => {
          if (menu.routePath) router.push(menu.routePath)
        }
      })
    })
  } else {
    // 回退：使用路由 meta title
    const meta = route.meta as Record<string, unknown>
    const title = (meta?.['title'] as string) || (route.name as string)
    if (title) {
      items.push({ key: 'current', label: title, onClick: noop })
    }
  }
  
  return items
})

// 全局搜索面板
const showCommandPalette = ref(false)

// 移动端模式管理
const appStore = useAppStore()
const isMobile = computed(() => appStore.isMobileView)
// 移动端详情页（自带 MobileHeader + 无 TabBar）
const isMobileSubpage = computed(() => {
  if (!isMobile.value) return false
  return /^\/m\/(chart|report|page|data-view)\//.test(route.path) || route.path === '/change-password'
})

const handleResize = () => {
  appStore.detectDevice()
}

const handleHamburgerClick = () => {
  collapsed.value = !collapsed.value
}

// 侧边栏底部版本号和版权信息（从 Config_Service 读取）
const sidebarVersion = ref('v1.0.0')
const sidebarCopyright = ref('© DataTeaCup')

const loadSidebarInfo = async () => {
  try {
    const versionRes = await getConfigByKey('system.version').catch(() => null)
    const copyrightRes = await getConfigByKey('system.copyright').catch(() => null)
    if (versionRes?.data) {
      sidebarVersion.value = versionRes.data
    }
    if (copyrightRes?.data) {
      sidebarCopyright.value = copyrightRes.data
    }
  } catch { /* 使用默认值 */ }
}

// 创建图标渲染函数
const createIcon = (IconComponent: any) => {
  return () => h(NIcon, { component: IconComponent })
}

// 默认菜单 - 仅包含仪表盘（用于用户没有任何权限时的后备）
const defaultDashboardMenu = computed(() => [
  {
    label: t('menu.dashboard'),
    key: 'Dashboard',
    icon: createIcon(HomeOutline),
    routePath: '/dashboard'
  }
])

// 实际显示的菜单（由后端权限控制）
const menuOptions = ref(defaultDashboardMenu.value)

// 监听路由变化，更新activeKey和标签页
watch(() => route.fullPath, (newPath) => {
  // 跳过 redirect 路由
  if (newPath.startsWith('/redirect') || newPath === '/login') {
    return
  }
  
  // 检查是否是动态页面（报表、图表、页面、数据视图）
  const isDynamicPage = route.path.startsWith('/report-view/') || 
                        (route.path.startsWith('/chart-center/') && route.params["id"]) ||
                        route.path.startsWith('/page-view/') ||
                        route.path.startsWith('/data-view/')
  
  // 检查标签是否已存在
  const existingTab = tabsStore.tabs.find(tab => tab.key === newPath)
  if (existingTab) {
    // 对于动态页面，只激活标签，不更新标题（由各页面组件自己更新）
    tabsStore.setActiveTab(newPath)
    return
  }
  
  // 动态页面使用特定标记（用于后续可能的刷新逻辑）
  void isDynamicPage
  
  const routeName = route.name as string
  const routeMeta = route.meta
  const title = (routeMeta?.["title"] as string) || routeName || t('common.view')
  
  // 添加标签
  tabsStore.addTab({
    key: newPath,
    title: title,
    name: routeName,
    closable: newPath !== '/dashboard'
  })
  
  // 更新菜单选中状态
  if (routeName && !isNavigating.value) {
    const findMenuKey = (menus: any[]): string | null => {
      for (const menu of menus) {
        if (menu.key === routeName) return menu.key
        if (menu.routePath === newPath) return menu.key
        if (menu.children?.length > 0) {
          const found = findMenuKey(menu.children)
          if (found) return found
        }
      }
      return null
    }
    
    const menuKey = findMenuKey(menuOptions.value)
    if (menuKey) {
      activeKey.value = menuKey
    } else {
      activeKey.value = routeName
    }
  }
}, { immediate: true })

// 初始化时获取用户信息和动态菜单
onMounted(async () => {
  // 监听 Ctrl+K 打开搜索面板
  const handleKeydown = (e: KeyboardEvent) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
      e.preventDefault()
      showCommandPalette.value = !showCommandPalette.value
    }
  }
  window.addEventListener('keydown', handleKeydown)
  
  // 监听窗口大小变化（响应式）
  window.addEventListener('resize', handleResize)
  
  // 监听菜单更新事件
  window.addEventListener('menu-updated', loadDynamicMenus)
  
  // 初始化语言设置
  initLocale()
  
  // 如果已登录但没有用户信息，或者权限未加载，则获取用户信息
  // 注意：路由守卫可能已经加载了权限，这里检查以避免重复加载
  if (userStore.token && (!userStore.userInfo || !userStore.permissionsLoaded)) {
    try {
      await userStore.fetchUserInfo()
    } catch (error: any) {
      // 如果是401或404错误，清除无效的token并跳转到登录页
      if (error?.message?.includes('未授权') || error?.message?.includes('不存在') || error?.message?.includes('过期')) {
        logger.warn('Token无效，清除登录状态')
        userStore.logout()
        if (route.path !== '/login') {
          router.push('/login')
        }
      } else {
        logger.error('获取用户信息失败', error)
      }
    }
  }
  
  // 强制修改密码模式：跳过所有非必要的 API 调用（菜单、系统配置、聊天等），
  // 避免因用户无权限导致大量 403 错误
  if (userStore.mustChangePassword) {
    logger.info('强制修改密码模式，跳过非必要资源加载')
    return
  }

  // 初始化通知推送（仅请求桌面通知权限，不连接 WebSocket）
  // WebSocket 通知推送需要后端在线，但无法保证此时后端可达
  // 因此不在页面加载时连接，避免控制台报错
  if (userStore.token) {
    initNotificationPush()
    // 加载侧边栏版本号和版权信息
    loadSidebarInfo()
    // 初始化聊天：加载会话列表 + 启动轮询以同步未读数
    chatStore.initGlobal()
    // 连接聊天 WebSocket 以接收实时消息
    chatWs.onMessage('chat', (payload: any) => {
      chatStore.handleIncomingMessage(payload)
    })
    chatWs.connect(userStore.token!)
  }

  // 加载动态菜单
  if (userStore.token) {
    await loadDynamicMenus()
    
    // 菜单加载完成后，更新activeKey以匹配当前路由
    if (route.name) {
      const findMenuKey = (menus: any[]): string | null => {
        for (const menu of menus) {
          if (menu.key === route.name) {
            return menu.key
          }
          if (menu.routePath && route.path === menu.routePath) {
            return menu.key
          }
          if (menu.reportId && route.path === `/report-view/${menu.reportId}`) {
            return menu.key
          }
          if (menu.children && menu.children.length > 0) {
            const found = findMenuKey(menu.children)
            if (found) return found
          }
        }
        return null
      }
      
      const menuKey = findMenuKey(menuOptions.value)
      if (menuKey) {
        activeKey.value = menuKey
      } else if (route.name) {
        activeKey.value = route.name as string
      }
    }
  }
})

// 获取图标组件（iconMap已从 @/utils/iconMap 导入）
const getIconComponent = (iconName?: string) => {
  if (!iconName) return undefined
  const IconComponent = iconMap[iconName]
  if (IconComponent) {
    return createIcon(IconComponent)
  }
  return undefined
}

// 加载动态菜单
const loadDynamicMenus = async () => {
  try {
    // 使用 __silent 避免权限不足时弹出错误提示（普通用户可能无菜单管理权限）
    const res = await request.get('/menu/visible', { __silent: true } as any)
    const dynamicMenus = res.data || []
    
    // 如果返回的数据为空或不是数组，使用静态菜单
    if (!Array.isArray(dynamicMenus) || dynamicMenus.length === 0) {
      return
    }
    
    // 将动态菜单转换为菜单选项
    const convertMenuToOption = (menu: any): any => {
      const menuLabel = menu.badge
        ? () => h('div', { style: 'display:flex;align-items:center;gap:6px;width:100%' }, [
            h('span', {}, menu.menuName),
            h('span', { style: 'font-size:10px;padding:1px 6px;border-radius:8px;background:linear-gradient(135deg,#f59e0b,#ef4444);color:#fff;font-weight:600;line-height:1.4;flex-shrink:0' }, menu.badge)
          ])
        : menu.menuName
      const option: any = {
        label: menuLabel,
        key: menu.menuCode || `menu_${menu.id}`,
        icon: getIconComponent(menu.icon),
        routePath: menu.routePath,
        reportId: menu.reportId,
        chartId: menu.chartId,
        pageId: menu.pageId,
        dataViewCode: menu.dataViewCode,
        openMode: menu.openMode || 'tab'
      }
      
      // 如果有路由路径或报表ID，设置点击事件
      if (menu.routePath) {
        option.onClick = async () => {
          isNavigating.value = true
          try {
            // 先添加标签页，使用菜单名称作为标题
            tabsStore.addTab({
              key: menu.routePath,
              title: menu.menuName,
              closable: true
            })
            await router.push(menu.routePath).catch((err) => {
              if (err.name !== 'NavigationDuplicated') {
                logger.error('路由跳转失败', err)
              }
            })
          } finally {
            setTimeout(() => {
              isNavigating.value = false
            }, 200)
          }
        }
      } else if (menu.reportId) {
        // 如果有报表ID，跳转到报表查看页面
        option.onClick = async () => {
          isNavigating.value = true
          try {
            const reportPath = `/report-view/${menu.reportId}`
            // 先添加标签页，使用菜单名称作为标题
            tabsStore.addTab({
              key: reportPath,
              title: menu.menuName,
              closable: true
            })
            await router.push(reportPath).catch((err) => {
              if (err.name !== 'NavigationDuplicated') {
                logger.error('路由跳转失败', err)
              }
            })
          } finally {
            setTimeout(() => {
              isNavigating.value = false
            }, 200)
          }
        }
      } else if (menu.chartId) {
        // 如果有图表ID，跳转到图表查看页面
        option.onClick = async () => {
          isNavigating.value = true
          try {
            const chartPath = `/chart-center/${menu.chartId}`
            tabsStore.addTab({
              key: chartPath,
              title: menu.menuName,
              closable: true
            })
            await router.push(chartPath).catch((err) => {
              if (err.name !== 'NavigationDuplicated') {
                logger.error('路由跳转失败', err)
              }
            })
          } finally {
            setTimeout(() => {
              isNavigating.value = false
            }, 200)
          }
        }
      } else if (menu.pageId) {
        // 如果有页面ID，跳转到页面查看
        option.onClick = async () => {
          isNavigating.value = true
          try {
            const pagePath = `/page-view/${menu.pageId}`
            tabsStore.addTab({
              key: pagePath,
              title: menu.menuName,
              closable: true
            })
            await router.push(pagePath).catch((err) => {
              if (err.name !== 'NavigationDuplicated') {
                logger.error('路由跳转失败', err)
              }
            })
          } finally {
            setTimeout(() => {
              isNavigating.value = false
            }, 200)
          }
        }
      } else if (menu.dataViewCode) {
        // 如果有数据视图编码，跳转到数据视图页面
        option.onClick = async () => {
          isNavigating.value = true
          try {
            const dataViewPath = `/data-view/${menu.dataViewCode}`
            tabsStore.addTab({
              key: dataViewPath,
              title: menu.menuName,
              closable: true
            })
            await router.push(dataViewPath).catch((err) => {
              if (err.name !== 'NavigationDuplicated') {
                logger.error('路由跳转失败', err)
              }
            })
          } finally {
            setTimeout(() => {
              isNavigating.value = false
            }, 200)
          }
        }
      }
      
      // 递归处理子菜单
      if (menu.children && menu.children.length > 0) {
        option.children = menu.children.map(convertMenuToOption)
      }
      
      return option
    }
    
    // 移动端过滤：仅显示 mobileVisible !== 0 的菜单
    const filterMobileMenus = (menus: any[]): any[] => {
      if (!isMobile.value) return menus
      return menus
        .filter((m: any) => m.mobileVisible !== 0)
        .map((m: any) => ({
          ...m,
          children: m.children ? filterMobileMenus(m.children) : undefined
        }))
    }
    const filteredMenus = filterMobileMenus(dynamicMenus)
    
    // 使用数据库菜单替换静态菜单（数据库菜单包含层级结构）
    const dbMenus = filteredMenus.map(convertMenuToOption)
    
    // 使用后端返回的菜单，如果为空则只显示仪表盘
    if (dbMenus.length > 0) {
      menuOptions.value = dbMenus
    } else {
      // 后端返回空菜单，只显示仪表盘
      menuOptions.value = defaultDashboardMenu.value
    }
  } catch (error) {
    logger.error('加载动态菜单失败', error)
    // 加载失败时只显示仪表盘
    menuOptions.value = defaultDashboardMenu.value
  }
}

// 语言选项
const languageOptions = computed(() => [
  {
    label: '🇨🇳 ' + t('language.zhCN'),
    key: 'zh-CN',
    disabled: currentLocale.value === 'zh-CN'
  },
  {
    label: '🇺🇸 ' + t('language.enUS'),
    key: 'en-US',
    disabled: currentLocale.value === 'en-US'
  }
])

// 切换语言
const handleLanguageChange = (key: Locale) => {
  setLocale(key)
  // 无刷新切换语言 - 所有 UI 文本会立即更新
  // 显示成功提示
  message.success(key === 'zh-CN' ? '语言已切换为中文' : 'Language switched to English')
  // 重新加载动态菜单以更新菜单文本
  loadDynamicMenus()
}

const userOptions = computed(() => [
  {
    label: t('common.export'),
    key: 'exportCenter',
    icon: () => h(NIcon, null, { default: () => h(DownloadOutline) })
  },
  {
    label: t('user.changePassword'),
    key: 'changePassword',
    icon: () => h(NIcon, null, { default: () => h(CreateOutline) })
  },
  {
    label: t('user.logout'),
    key: 'logout',
    icon: () => h(NIcon, null, { default: () => h(LogOutOutline) })
  }
])

const handleMenuSelect = async (key: string) => {
  // 防止重复导航
  if (isNavigating.value) {
    return
  }
  
  // 递归查找菜单（包括子菜单）
  const findMenu = (menus: any[]): any => {
    for (const menu of menus) {
      if (menu.key === key) {
        return menu
      }
      if (menu.children && menu.children.length > 0) {
        const found = findMenu(menu.children)
        if (found) return found
      }
    }
    return null
  }
  
  const menu = findMenu(menuOptions.value)
  
  // 如果菜单有子菜单但没有路由路径，说明是父菜单，只展开/收起，不跳转
  if (menu && menu.children && menu.children.length > 0 && !menu.routePath && !menu.reportId && !menu.onClick) {
    // 父菜单，只更新activeKey，不执行跳转
    activeKey.value = key
    return
  }
  
  // 设置导航标志
  isNavigating.value = true
  
  try {
    if (menu) {
      // 如果有onClick，使用onClick
      if (menu.onClick) {
        await menu.onClick()
        return
      }
      
      // 如果有routePath，直接跳转
      if (menu.routePath) {
        await router.push(menu.routePath).catch((err) => {
          if (err.name !== 'NavigationDuplicated') {
            logger.error('路由跳转失败', err)
          }
        })
        return
      }
      
      // 如果有reportId，跳转到报表查看页面
      if (menu.reportId) {
        await router.push(`/report-view/${menu.reportId}`).catch((err) => {
          if (err.name !== 'NavigationDuplicated') {
            logger.error('路由跳转失败', err)
          }
        })
        return
      }
    }
    
    // 尝试通过路由名称跳转（只对叶子节点菜单）
    if (!menu || (!menu.children || menu.children.length === 0)) {
      try {
        await router.push({ name: key }).catch((err) => {
          if (err.name !== 'NavigationDuplicated') {
            logger.warn(`路由跳转失败: 路由名称 ${key} 不存在`, err)
            // 如果路由名称不存在，不进行跳转，避免误跳转到 dashboard
          }
        })
      } catch (error) {
        logger.error('路由跳转失败', error)
      }
    }
  } finally {
    // 确保导航标志被重置
    setTimeout(() => {
      isNavigating.value = false
    }, 200)
  }
}

// 清理资源
onUnmounted(() => {
  // 移除全屏监听
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  // 移除菜单更新监听
  window.removeEventListener('menu-updated', loadDynamicMenus)
  // 移除窗口大小监听
  window.removeEventListener('resize', handleResize)
  // 停止聊天轮询和 WebSocket
  chatStore.stopPolling()
  chatWs.disconnect()
})

// 全屏变化处理
const handleFullscreenChange = () => {
  if (document.fullscreenElement) {
    // 进入全屏：记录当前状态并收起侧边栏
    collapsedBeforeFullscreen.value = collapsed.value
    collapsed.value = true
  } else {
    // 退出全屏：恢复之前的状态
    collapsed.value = collapsedBeforeFullscreen.value
  }
}

// 添加全屏监听
document.addEventListener('fullscreenchange', handleFullscreenChange)

// ==================== 通知功能 ====================
// unreadNotificationCount 和 notifications 由 useNotificationPush composable 提供

const loadNotifications = async () => {
  try {
    const res = await systemMonitorApi.getNotifications(20) as any
    const data = res.data || []
    notifications.value = data
    unreadNotificationCount.value = data.filter((n: any) => !n.isRead).length
  } catch { /* 静默失败 */ }
}

const handleNotificationClick = async (item: any) => {
  if (!item.isRead) {
    try {
      await systemMonitorApi.markNotificationRead(item.id)
      item.isRead = 1
      unreadNotificationCount.value = Math.max(0, unreadNotificationCount.value - 1)
    } catch { /* 静默失败 */ }
  }
}

const handleMarkAllRead = async () => {
  try {
    await systemMonitorApi.markAllNotificationsRead()
    notifications.value.forEach((n: any) => n.isRead = 1)
    unreadNotificationCount.value = 0
  } catch { /* 静默失败 */ }
}

// 跳转到导出中心
const goToExportCenter = () => {
  router.push('/export-center')
}

const handleUserSelect = (key: string) => {
  if (key === 'exportCenter') {
    router.push('/export-center')
  } else if (key === 'changePassword') {
    router.push('/change-password')
  } else if (key === 'logout') {
    // 清空标签页
    tabsStore.clearTabs()
    // 清除用户状态
    userStore.logout()
    // 重置路由状态
    resetRouter()
    // 跳转登录页
    router.push('/login')
    message.success(t('user.logoutSuccess'))
  }
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
}

/* Logo 区域 */
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 12px;
  font-weight: 700;
  font-size: 20px;
  background: var(--dp-bg-primary, #ffffff);
  color: var(--dp-text-primary, #0f172a);
  overflow: hidden;
  position: relative;
  border-bottom: 1px solid var(--dp-border-light, rgba(226,232,240,0.8));
  flex-shrink: 0;
  cursor: pointer;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
}

.logo::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(37, 99, 235, 0.35) 35%,
    rgba(6, 182, 212, 0.3) 65%,
    transparent 100%
  );
}

.logo-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  background: var(--dp-gradient-primary, linear-gradient(135deg, #2563eb 0%, #1e40af 100%));
  border-radius: 9px;
  color: white;
  flex-shrink: 0;
  transition: all 0.28s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 3px 10px rgba(37, 99, 235, 0.32), inset 0 1px 0 rgba(255,255,255,0.2);
}

.logo:hover .logo-icon {
  transform: scale(1.08) rotate(-4deg);
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.42), inset 0 1px 0 rgba(255,255,255,0.25);
}

.logo-icon svg {
  width: 22px;
  height: 22px;
}

.logo-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 16.5px;
  font-weight: 800;
  letter-spacing: -0.4px;
  background: linear-gradient(135deg, var(--dp-text-primary, #0f172a) 0%, var(--dp-color-primary, #1e40af) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* 顶部导航栏 */
.header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: var(--dp-z-sticky, 200);
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hamburger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  cursor: pointer;
  border-radius: 10px;
  transition: all 0.25s ease;
  color: #64748b;
}

.hamburger:hover {
  background: rgba(37, 99, 235, 0.08);
  color: var(--dp-color-primary, #2563eb);
}

.breadcrumb {
  font-size: 14px;
  margin-left: 4px;
}

.breadcrumb :deep(.n-breadcrumb-item:last-child .n-breadcrumb-item__link) {
  color: #1e293b;
  font-weight: 600;
}

.breadcrumb :deep(.n-breadcrumb-item__link) {
  color: #94a3b8;
}

.breadcrumb :deep(.n-breadcrumb-item__link:hover) {
  color: var(--color-primary, #3b82f6);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  cursor: pointer;
  border-radius: 9px;
  color: #64748b;
  transition: all 0.2s var(--dp-ease-in-out, cubic-bezier(0.4, 0, 0.2, 1));
  position: relative;
}

.header-action:hover {
  background: rgba(37, 99, 235, 0.07);
  color: var(--dp-color-primary, #2563eb);
}

.header-action:active {
  transform: scale(0.94);
  background: rgba(37, 99, 235, 0.12);
}

/* 语言切换按钮 */
.language-btn {
  width: auto;
  padding: 0 12px;
  gap: 4px;
}

.language-text {
  font-size: 12px;
  font-weight: 600;
  color: inherit;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 12px 5px 6px;
  height: 36px;
  cursor: pointer;
  border-radius: 18px;
  transition: all 0.2s var(--dp-ease-in-out, cubic-bezier(0.4, 0, 0.2, 1));
  background: rgba(241, 245, 249, 0.8);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.user-info:hover {
  background: rgba(37, 99, 235, 0.07);
  border-color: rgba(37, 99, 235, 0.2);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.1);
}

.user-name {
  font-size: 13px;
  color: #475569;
  font-weight: 500;
  transition: color 0.2s ease;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-info:hover .user-name {
  color: var(--dp-color-primary, #2563eb);
}

/* 侧边栏 */
:deep(.n-layout-sider) {
  background: var(--dp-bg-primary, #ffffff) !important;
  box-shadow: 1px 0 0 rgba(226, 232, 240, 0.7), 2px 0 16px rgba(0, 0, 0, 0.04);
  border-right: none !important;
  transition: width 0.28s cubic-bezier(0.4, 0, 0.2, 1), min-width 0.28s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

:deep(.n-menu) {
  padding: 16px 12px;
  background: transparent;
}

:deep(.n-menu-item) {
  margin: 3px 0;
  border-radius: 10px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: relative;
}

:deep(.n-menu-item:hover) {
  background: rgba(37, 99, 235, 0.05);
}

:deep(.n-menu-item-content) {
  color: var(--dp-text-primary, #1e293b) !important;
  font-weight: 500;
  font-size: 13.5px;
  padding: 10px 16px !important;
  letter-spacing: 0.01em;
}

:deep(.n-menu-item-content .n-icon) {
  color: #475569 !important;
  font-size: 20px !important;
  transition: all 0.25s ease;
}

:deep(.n-menu-item:hover .n-menu-item-content) {
  color: #1f2937 !important;
}

:deep(.n-menu-item:hover .n-menu-item-content .n-icon) {
  color: var(--color-primary, #3b82f6) !important;
}

:deep(.n-menu-item-content--selected),
:deep(.n-menu-item-content--selected .n-menu-item-content__header),
:deep(.n-menu-item-content--selected .n-menu-item-content-header) {
  background: linear-gradient(135deg, color-mix(in srgb, var(--color-primary, #2563eb) 8%, #fff) 0%, color-mix(in srgb, var(--color-primary, #2563eb) 15%, #fff) 100%) !important;
  color: var(--color-primary, #2563eb) !important;
  border-radius: 10px;
  font-weight: 600;
  box-shadow: 0 2px 8px color-mix(in srgb, var(--color-primary, #2563eb) 12%, transparent);
  position: relative;
}

:deep(.n-menu-item-content--selected::after) {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  bottom: 6px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--color-primary, #2563eb);
}

:deep(.n-menu-item-content--selected .n-icon) {
  color: var(--color-primary, #2563eb) !important;
}

:deep(.n-menu-item-content--selected::before) {
  display: none;
}

/* 子菜单样式 */
:deep(.n-submenu-children) {
  padding-left: 12px !important;
  position: relative;
}


:deep(.n-submenu-children .n-menu-item) {
  margin: 2px 0;
}

:deep(.n-submenu-children .n-menu-item-content) {
  color: #374151 !important;
  font-size: 13px;
  padding: 9px 16px !important;
}

:deep(.n-submenu-children .n-menu-item:hover .n-menu-item-content) {
  color: #1f2937 !important;
}

:deep(.n-submenu-children .n-menu-item-content--selected) {
  color: var(--color-primary, #2563eb) !important;
  background: linear-gradient(135deg, color-mix(in srgb, var(--color-primary, #2563eb) 8%, #fff) 0%, color-mix(in srgb, var(--color-primary, #2563eb) 15%, #fff) 100%) !important;
  box-shadow: 0 1px 4px color-mix(in srgb, var(--color-primary, #2563eb) 10%, transparent);
}

:deep(.n-menu-item-content__arrow) {
  color: #475569 !important;
  transition: transform 0.25s ease, color 0.25s ease;
}

:deep(.n-menu-item-content--selected .n-menu-item-content__arrow) {
  color: var(--color-primary, #3b82f6) !important;
}

:deep(.n-submenu--show > .n-menu-item-content .n-menu-item-content__arrow) {
  transform: rotate(90deg);
}

:deep(.n-submenu > .n-menu-item-content) {
  color: #1e293b !important;
  font-weight: 500;
}

:deep(.n-submenu > .n-menu-item-content .n-icon) {
  color: #475569 !important;
}

:deep(.n-submenu:hover > .n-menu-item-content) {
  color: #1f2937 !important;
}

:deep(.n-submenu:hover > .n-menu-item-content .n-icon) {
  color: var(--color-primary, #3b82f6) !important;
}

/* 内容区域 */
.content {
  padding: 20px;
  background: var(--dp-gradient-page, linear-gradient(160deg, #f0f4f8 0%, #e8edf5 100%));
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  min-height: 0; /* Fix flex overflow */
  scrollbar-width: thin;
  scrollbar-color: rgba(148, 163, 184, 0.4) transparent;
}

.content::-webkit-scrollbar {
  width: 6px;
}

.content::-webkit-scrollbar-track {
  background: transparent;
}

.content::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.4);
  border-radius: 3px;
}

.content::-webkit-scrollbar-thumb:hover {
  background: rgba(100, 116, 139, 0.6);
}

/* 移动端内容区域（无 MainLayout header，全高布局） */
.mobile-content {
  padding: 0 !important;
  padding-bottom: calc(54px + env(safe-area-inset-bottom, 0px) + 8px) !important;
  min-height: 100vh !important;
  overflow-x: hidden !important;
}

/* 底部版权信息 */
.footer {
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 11px;
  color: var(--dp-text-tertiary, #94a3b8);
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(12px);
  border-top: 1px solid rgba(226, 232, 240, 0.5);
  flex-shrink: 0;
  letter-spacing: 0.02em;
  transition: color 200ms ease;
}

.footer-sep {
  margin: 0 6px;
  color: var(--dp-border-light, #e2e8f0);
  font-size: 10px;
}

/* 折叠按钮 */
:deep(.n-layout-toggle-button) {
  background: var(--dp-bg-primary, #fff) !important;
  border: 1px solid var(--dp-border-light, #e2e8f0) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  color: var(--dp-text-tertiary, #94a3b8) !important;
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1) !important;
  width: 20px !important;
  height: 48px !important;
  border-radius: 0 8px 8px 0 !important;
}

:deep(.n-layout-toggle-button:hover) {
  background: var(--dp-color-primary-light, rgba(37,99,235,0.06)) !important;
  border-color: var(--dp-color-primary, #2563eb) !important;
  color: var(--dp-color-primary, #2563eb) !important;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.12);
}

:deep(.n-layout-sider-scroll-container) {
  scrollbar-width: thin;
  scrollbar-color: #d1d5db transparent;
}

:deep(.n-layout-sider-scroll-container::-webkit-scrollbar) {
  width: 4px;
}

:deep(.n-layout-sider-scroll-container::-webkit-scrollbar-track) {
  background: transparent;
}

:deep(.n-layout-sider-scroll-container::-webkit-scrollbar-thumb) {
  background: #d1d5db;
  border-radius: 4px;
}

:deep(.n-layout-sider-scroll-container::-webkit-scrollbar-thumb:hover) {
  background: #9ca3af;
}

/* 侧边栏包装器 - 使用 flex 布局实现底部固定 */
.sidebar-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.sidebar-top {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-version {
  font-size: 11px;
  color: var(--dp-text-tertiary, #94a3b8);
  margin-bottom: 2px;
  font-variant-numeric: tabular-nums;
}

.sidebar-copyright {
  font-size: 10px;
  color: var(--dp-text-tertiary, #cbd5e1);
  opacity: 0.7;
}

/* 侧边栏折叠/展开 文字过渡动画 */
.sidebar-text-enter-active,
.sidebar-text-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
  overflow: hidden;
}
.sidebar-text-enter-from,
.sidebar-text-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}

/* 侧边栏宽度过渡 */
:deep(.n-layout-sider) {
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1),
              min-width 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

/* 菜单项折叠/展开过渡 */
:deep(.n-menu-item-content-header) {
  transition: opacity 0.2s ease, max-width 0.2s ease;
  white-space: nowrap;
  overflow: hidden;
}

/* ========== header 动作组分隔线 ========== */
.header-divider {
  width: 1px;
  height: 20px;
  background: rgba(226, 232, 240, 0.8);
  margin: 0 4px;
  flex-shrink: 0;
}

/* 键盘快捷键辽明标记 */
.header-kbd {
  display: inline-flex;
  align-items: center;
  padding: 1px 6px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 5px;
  font-size: 10px;
  font-family: 'SFMono-Regular', 'Cascadia Code', Consolas, monospace;
  letter-spacing: 0.04em;
  line-height: 1.6;
  color: inherit;
  white-space: nowrap;
}

/* 用户头像包装器（定位在线点 */
.user-avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

/* 在线状态绳【绿色小点】 */
.user-online-dot {
  position: absolute;
  bottom: 0;
  right: -1px;
  width: 8px;
  height: 8px;
  background: #22c55e;
  border: 2px solid var(--dp-bg-primary, #fff);
  border-radius: 50%;
  box-shadow: 0 0 0 2px rgba(34, 197, 94, 0.2);
  animation: onlinePulse 3s ease-in-out infinite;
}

@keyframes onlinePulse {
  0%, 100% { box-shadow: 0 0 0 2px rgba(34, 197, 94, 0.2); }
  50% { box-shadow: 0 0 0 4px rgba(34, 197, 94, 0); }
}

/* ========== 通知中心样式 ========== */
.notification-header {
  padding: 14px 16px 10px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notification-title {
  font-weight: 700;
  font-size: 14px;
  color: var(--dp-text-primary, #0f172a);
  letter-spacing: -0.01em;
}

.notification-empty {
  padding: 40px 20px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.notification-empty-icon {
  font-size: 36px;
  opacity: 0.25;
}

.notification-empty-text {
  font-size: 13px;
  color: var(--dp-text-tertiary, #94a3b8);
}

.notification-item {
  padding: 12px 16px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  cursor: pointer;
  background: transparent;
  transition: background 0.15s ease;
  position: relative;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item--unread {
  background: rgba(37, 99, 235, 0.03);
}

.notification-item--unread::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--dp-color-primary, #2563eb);
  border-radius: 0 2px 2px 0;
}

.notification-item:hover {
  background: rgba(37, 99, 235, 0.04);
}

.notification-item-title {
  font-weight: 500;
  font-size: 13px;
  color: var(--dp-text-primary, #0f172a);
  line-height: 1.4;
}

.notification-item-content {
  font-size: 12px;
  color: var(--dp-text-secondary, #64748b);
  margin-top: 3px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.notification-item-time {
  font-size: 11px;
  color: var(--dp-text-tertiary, #94a3b8);
  margin-top: 5px;
  font-variant-numeric: tabular-nums;
}

/* ========== 侧边栏 footer 升级 ========== */
.sidebar-footer {
  padding: 10px 16px 14px;
  border-top: 1px solid rgba(226, 232, 240, 0.6);
  text-align: center;
  flex-shrink: 0;
  background: linear-gradient(0deg, rgba(37, 99, 235, 0.02) 0%, transparent 100%);
}

/* ========== 暗色模式 - 新增 header 元素 ========== */


</style>

<!-- 非 scoped 样式：确保暗色模式下 Naive UI 组件样式可靠覆盖 -->
<style>
/* ========== 暗色模式侧边栏（非 scoped，确保覆盖 Naive UI 默认样式） ========== */
html.dark .n-layout-sider {
  background: var(--dp-bg-sidebar, #0d1829) !important;
  border-right-color: rgba(255, 255, 255, 0.04) !important;
  box-shadow: 1px 0 0 rgba(255, 255, 255, 0.04) !important;
}

html.dark .n-layout-sider .n-menu-item-content {
  color: #cbd5e1 !important;
}

html.dark .n-layout-sider .n-menu-item-content .n-icon {
  color: #cbd5e1 !important;
}

html.dark .n-layout-sider .n-menu-item:hover {
  background: rgba(59, 130, 246, 0.08) !important;
}

html.dark .n-layout-sider .n-menu-item:hover .n-menu-item-content {
  color: #e2e8f0 !important;
}

html.dark .n-layout-sider .n-menu-item:hover .n-menu-item-content .n-icon {
  color: #60a5fa !important;
}

html.dark .n-layout-sider .n-menu-item-content--selected,
html.dark .n-layout-sider .n-menu-item-content--selected .n-menu-item-content__header,
html.dark .n-layout-sider .n-menu-item-content--selected .n-menu-item-content-header {
  background: rgba(37, 99, 235, 0.18) !important;
  color: #60a5fa !important;
  box-shadow: inset 0 0 0 1px rgba(59, 130, 246, 0.2) !important;
}

html.dark .n-layout-sider .n-menu-item-content--selected .n-icon {
  color: #60a5fa !important;
}

html.dark .n-layout-sider .n-submenu > .n-menu-item-content {
  color: #cbd5e1 !important;
}

html.dark .n-layout-sider .n-submenu > .n-menu-item-content .n-icon {
  color: #cbd5e1 !important;
}

html.dark .n-layout-sider .n-submenu:hover > .n-menu-item-content {
  color: #f1f5f9 !important;
}

html.dark .n-layout-sider .n-submenu:hover > .n-menu-item-content .n-icon {
  color: #f1f5f9 !important;
}

html.dark .n-layout-sider .n-submenu-children .n-menu-item-content {
  color: #a1b0c4 !important;
}

html.dark .n-layout-sider .n-submenu-children .n-menu-item:hover .n-menu-item-content {
  color: #f1f5f9 !important;
}

html.dark .n-layout-sider .n-submenu-children .n-menu-item-content--selected {
  color: #60a5fa !important;
  background: rgba(59, 130, 246, 0.15) !important;
}

html.dark .n-layout-sider .n-menu-item-content__arrow {
  color: #94a3b8 !important;
}

html.dark .n-layout-sider .n-menu-item-content--selected .n-menu-item-content__arrow {
  color: #60a5fa !important;
}

html.dark .n-layout-toggle-button {
  background: var(--dp-bg-elevated, #1e293b) !important;
  border-color: rgba(255, 255, 255, 0.07) !important;
  color: #64748b !important;
}

html.dark .n-layout-toggle-button:hover {
  background: var(--dp-bg-tertiary, #1e2d45) !important;
  border-color: var(--dp-color-primary, #3b82f6) !important;
  color: var(--dp-color-primary, #3b82f6) !important;
}

html.dark .n-breadcrumb-item {
  color: #94a3b8 !important;
}

/* Logo 图标深色模式 */
html.dark .logo-icon {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3) !important;
  border: none !important;
}

html.dark .logo:hover .logo-icon {
  box-shadow: 0 2px 12px rgba(59, 130, 246, 0.4) !important;
}

html.dark .logo-text {
  color: #f1f5f9 !important;
  -webkit-text-fill-color: #f1f5f9 !important;
}

/* 确保 Naive UI layout 组件在暗色模式下背景正确 */
html.dark .n-layout-header {
  background: rgba(6, 13, 26, 0.88) !important;
}

html.dark .n-layout > .n-layout-scroll-container {
  background: var(--dp-bg-canvas, #060d1a) !important;
}

html.dark .n-layout-footer {
  background: rgba(6, 13, 26, 0.7) !important;
  border-top-color: rgba(255, 255, 255, 0.04) !important;
  color: #475569 !important;
}

/* ========== 全局深色模式 - Naive UI CSS 变量覆盖 ========== */
/* 这些覆盖 Naive UI 的 CSS-in-JS 内部变量，确保所有组件在暗色模式下显示正确 */

/* 卡片 */
html.dark .n-card {
  --n-color: var(--dp-bg-elevated, #1e293b) !important;
  --n-color-embedded: var(--dp-bg-secondary, #1a2438) !important;
  --n-border-color: var(--dp-border-light, rgba(255,255,255,0.06)) !important;
  --n-text-color: var(--dp-text-primary, #f1f5f9) !important;
  --n-title-text-color: var(--dp-text-primary, #f1f5f9) !important;
  --n-close-icon-color: #64748b !important;
  --n-close-icon-color-hover: #94a3b8 !important;
  --n-close-color-hover: rgba(255,255,255,0.06) !important;
  --n-action-color: var(--dp-bg-secondary, #1a2438) !important;
  background-color: var(--dp-bg-elevated, #1e293b) !important;
  color: var(--dp-text-primary, #f1f5f9) !important;
}

html.dark .n-card-header {
  border-bottom-color: var(--dp-border-light, rgba(255,255,255,0.06)) !important;
}

/* 输入框 */
html.dark .n-input {
  --n-color: var(--dp-bg-secondary, #1a2438) !important;
  --n-color-focus: var(--dp-bg-secondary, #1a2438) !important;
  --n-color-disabled: var(--dp-bg-elevated, #1e293b) !important;
  --n-border: 1px solid var(--dp-border-default, rgba(255,255,255,0.1)) !important;
  --n-border-hover: 1px solid var(--dp-border-strong, rgba(255,255,255,0.18)) !important;
  --n-border-focus: 1px solid var(--dp-color-primary, #3b82f6) !important;
  --n-text-color: var(--dp-text-primary, #f1f5f9) !important;
  --n-placeholder-color: var(--dp-text-tertiary, #64748b) !important;
  --n-caret-color: var(--dp-color-primary, #3b82f6) !important;
}

/* 选择器 */
html.dark .n-base-selection {
  --n-color: var(--dp-bg-secondary, #1a2438) !important;
  --n-color-active: var(--dp-bg-secondary, #1a2438) !important;
  --n-color-disabled: var(--dp-bg-elevated, #1e293b) !important;
  --n-border: 1px solid var(--dp-border-default, rgba(255,255,255,0.1)) !important;
  --n-text-color: var(--dp-text-primary, #f1f5f9) !important;
  --n-placeholder-color: var(--dp-text-tertiary, #64748b) !important;
}

/* 标签页 */
html.dark .n-tabs {
  --n-tab-text-color: #94a3b8 !important;
  --n-tab-text-color-active: #f1f5f9 !important;
  --n-tab-text-color-hover: #cbd5e1 !important;
  --n-tab-text-color-disabled: #475569 !important;
  --n-bar-color: var(--color-primary, #409EFF) !important;
  --n-tab-border-color: #334155 !important;
  --n-pane-text-color: #e2e8f0 !important;
  --n-tab-color-segment: #1e293b !important;
}

html.dark .n-tabs-nav {
  border-bottom-color: #334155 !important;
}

/* 表格 */
html.dark .n-data-table {
  --n-th-color: var(--dp-bg-elevated, #1e293b) !important;
  --n-td-color: var(--dp-bg-primary, #0f172a) !important;
  --n-td-color-striped: var(--dp-bg-secondary, #1a2438) !important;
  --n-td-color-hover: rgba(59, 130, 246, 0.07) !important;
  --n-border-color: var(--dp-border-light, rgba(255,255,255,0.06)) !important;
  --n-th-text-color: var(--dp-text-secondary, #94a3b8) !important;
  --n-td-text-color: var(--dp-text-primary, #e2e8f0) !important;
}

/* 表单 */
html.dark .n-form-item-label__text {
  color: #94a3b8 !important;
}

html.dark .n-form-item .n-form-item-feedback__line {
  color: #64748b !important;
}

/* 单选按钮 */
html.dark .n-radio__label {
  color: #cbd5e1 !important;
}

html.dark .n-radio-group {
  color: #cbd5e1 !important;
}

/* 提醒/警告框 */
html.dark .n-alert {
  --n-color: rgba(59, 130, 246, 0.08) !important;
  --n-border: 1px solid rgba(59, 130, 246, 0.2) !important;
  --n-title-text-color: #e2e8f0 !important;
  --n-content-text-color: #cbd5e1 !important;
  --n-icon-color: #60a5fa !important;
}

/* 按钮 - 默认类型 */
html.dark .n-button--default-type {
  --n-color: #1e293b !important;
  --n-color-hover: #334155 !important;
  --n-color-pressed: #0f172a !important;
  --n-border: 1px solid #334155 !important;
  --n-border-hover: 1px solid #475569 !important;
  --n-text-color: #e2e8f0 !important;
}

/* 分页 */
html.dark .n-pagination {
  --n-item-color: #1e293b !important;
  --n-item-color-hover: rgba(255,255,255,0.06) !important;
  --n-item-color-active: rgba(59, 130, 246, 0.12) !important;
  --n-item-text-color: #94a3b8 !important;
  --n-item-border-color: #334155 !important;
}

/* 弹出菜单/下拉 */
html.dark .n-popover,
html.dark .n-dropdown-menu {
  --n-color: #1e293b !important;
  --n-text-color: #e2e8f0 !important;
}

/* 对话框 */
html.dark .n-dialog {
  --n-color: #1e293b !important;
  --n-text-color: #e2e8f0 !important;
  --n-title-text-color: #f1f5f9 !important;
}

/* 抽屉 */
html.dark .n-drawer {
  --n-color: #1e293b !important;
  --n-text-color: #e2e8f0 !important;
  --n-title-text-color: #f1f5f9 !important;
  --n-header-border-bottom: 1px solid #334155 !important;
  --n-body-padding: 16px 24px !important;
}

/* 上传 */
html.dark .n-upload-dragger {
  background: #111827 !important;
  border-color: #334155 !important;
}

html.dark .n-upload-trigger {
  border-color: #334155 !important;
}

/* 通用文字颜色 */
html.dark .n-text {
  color: #e2e8f0 !important;
}

html.dark .n-h1, html.dark .n-h2, html.dark .n-h3,
html.dark .n-h4, html.dark .n-h5, html.dark .n-h6 {
  color: #f1f5f9 !important;
}

html.dark .n-statistic-value {
  color: #f1f5f9 !important;
}

html.dark .n-statistic__label {
  color: #94a3b8 !important;
}

html.dark .n-empty__description {
  color: #64748b !important;
}

html.dark .n-divider {
  --n-color: #334155 !important;
}

html.dark .n-descriptions {
  color: #cbd5e1 !important;
}

/* ========== 页面级背景 - 深海军蓝 ========== */
html.dark .content {
  background: var(--dp-gradient-page, linear-gradient(160deg, #060d1a 0%, #0d1829 100%)) !important;
}

html.dark .n-layout-content {
  background: var(--dp-bg-canvas, #060d1a) !important;
}

/* 通用页面容器 - 捕获所有 *-page 类的白色背景 */
html.dark [class$="-page"],
html.dark [class*="-page "] {
  background-color: transparent !important;
  color: #e2e8f0;
}

/* 通用面板/区块 - 浅灰/白色背景修复 */
html.dark [class$="-section"],
html.dark [class$="-panel"],
html.dark [class$="-container"],
html.dark [class$="-wrapper"],
html.dark [class$="-block"],
html.dark [class$="-area"] {
  color: #e2e8f0;
}

/* 页面内 stat 类组件 */
html.dark [class*="stat-item"],
html.dark [class*="stat-card"] {
  background-color: #1e293b !important;
  color: #e2e8f0 !important;
  border-color: #334155 !important;
}

/* 通用表头/卡片头 */
html.dark [class*="card-header"],
html.dark [class*="-header-custom"] {
  color: #f1f5f9 !important;
}

/* 通用值文字 */
html.dark [class*="stat-value"] {
  color: #f1f5f9 !important;
}

html.dark [class*="stat-label"] {
  color: #94a3b8 !important;
}

/* ========== 新增 header 元素暗色模式 ========== */
html.dark .header-divider {
  background: rgba(255, 255, 255, 0.07) !important;
}

html.dark .user-online-dot {
  border-color: var(--dp-bg-primary, #0f172a) !important;
}

html.dark .notification-header {
  border-color: rgba(255, 255, 255, 0.06) !important;
  background: var(--dp-bg-primary, #0f172a) !important;
}

html.dark .notification-title {
  color: var(--dp-text-primary, #f1f5f9) !important;
}

html.dark .notification-item {
  border-color: rgba(255, 255, 255, 0.04) !important;
  background: transparent !important;
}

html.dark .notification-item--unread {
  background: rgba(37, 99, 235, 0.06) !important;
}

html.dark .notification-item--unread::before {
  background: var(--dp-color-primary, #60a5fa) !important;
}

html.dark .notification-item:hover {
  background: rgba(59, 130, 246, 0.06) !important;
}

html.dark .notification-item-title {
  color: var(--dp-text-primary, #f1f5f9) !important;
}

html.dark .notification-item-content {
  color: var(--dp-text-secondary, #94a3b8) !important;
}

html.dark .notification-item-time {
  color: var(--dp-text-tertiary, #64748b) !important;
}

html.dark .notification-empty-text {
  color: var(--dp-text-tertiary, #64748b) !important;
}

html.dark .sidebar-footer {
  border-top-color: rgba(255, 255, 255, 0.05) !important;
  background: linear-gradient(0deg, rgba(37, 99, 235, 0.04) 0%, transparent 100%) !important;
}

/* ========== header 主体暗色修复 ========== */
html.dark .header {
  background: rgba(6, 13, 26, 0.9) !important;
  border-bottom-color: rgba(255, 255, 255, 0.05) !important;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.3) !important;
}

html.dark .hamburger {
  color: #94a3b8 !important;
}
html.dark .hamburger:hover {
  color: #60a5fa !important;
  background: rgba(37, 99, 235, 0.12) !important;
}

html.dark .breadcrumb .n-breadcrumb-item__link {
  color: #64748b !important;
}
html.dark .breadcrumb .n-breadcrumb-item:last-child .n-breadcrumb-item__link {
  color: #e2e8f0 !important;
}
html.dark .breadcrumb .n-breadcrumb-item__link:hover {
  color: #60a5fa !important;
}

html.dark .header-action {
  color: #94a3b8 !important;
}
html.dark .header-action:hover {
  color: #60a5fa !important;
  background: rgba(37, 99, 235, 0.12) !important;
}

/* 用户信息胶囊 */
html.dark .user-info {
  background: rgba(255, 255, 255, 0.05) !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
}
html.dark .user-info:hover {
  background: rgba(37, 99, 235, 0.12) !important;
  border-color: rgba(96, 165, 250, 0.25) !important;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.15) !important;
}
html.dark .user-name {
  color: #cbd5e1 !important;
}
html.dark .user-info:hover .user-name {
  color: #60a5fa !important;
}

/* header-kbd 快捷键标记 */
html.dark .header-kbd {
  background: rgba(255, 255, 255, 0.08) !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
  color: #94a3b8 !important;
}
</style>

<template>
  <div class="mobile-tab-bar" :class="{ 'mobile-tab-bar--hidden': hidden }">
    <div class="tab-bar-inner">
      <div
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-bar-item"
        :class="{ 'tab-bar-item--active': activeTab === tab.key }"
        @click="handleTabClick(tab)"
      >
        <div class="tab-bar-icon">
          <n-badge :value="tab.badge || 0" :max="99" :show="!!tab.badge" :offset="[-2, 2]">
            <n-icon :size="22">
              <component :is="activeTab === tab.key ? tab.activeIcon : tab.icon" />
            </n-icon>
          </n-badge>
        </div>
        <span class="tab-bar-label">{{ tab.label }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, type Component } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NIcon, NBadge } from 'naive-ui'
import {
  HomeOutline,
  Home,
  BarChartOutline,
  BarChart,
  DocumentTextOutline,
  DocumentText,
  PersonOutline,
  Person
} from '@vicons/ionicons5'

interface TabItem {
  key: string
  label: string
  icon: Component
  activeIcon: Component
  route: string
  badge?: number
}

defineProps<{
  hidden?: boolean
}>()

const router = useRouter()
const route = useRoute()

const tabs = computed<TabItem[]>(() => [
  {
    key: 'home',
    label: '首页',
    icon: HomeOutline,
    activeIcon: Home,
    route: '/m/pages'
  },
  {
    key: 'charts',
    label: '图表',
    icon: BarChartOutline,
    activeIcon: BarChart,
    route: '/m/charts'
  },
  {
    key: 'reports',
    label: '报表',
    icon: DocumentTextOutline,
    activeIcon: DocumentText,
    route: '/m/reports'
  },
  {
    key: 'profile',
    label: '我的',
    icon: PersonOutline,
    activeIcon: Person,
    route: '/m/profile'
  }
])

const activeTab = computed(() => {
  const path = route.path
  if (path === '/m/pages' || path.startsWith('/m/page/') || path === '/dashboard' || path === '/') return 'home'
  if (path.startsWith('/m/charts') || path.startsWith('/m/chart/') || path.startsWith('/chart')) return 'charts'
  if (path.startsWith('/m/reports') || path.startsWith('/m/report/') || path.startsWith('/report')) return 'reports'
  if (path.startsWith('/m/profile') || path === '/change-password' || path.startsWith('/user') || path.startsWith('/system') || path.startsWith('/export')) return 'profile'
  return 'home'
})

const handleTabClick = (tab: TabItem) => {
  if (route.path !== tab.route) {
    router.push(tab.route)
  }
}
</script>

<style scoped>
.mobile-tab-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  padding-bottom: env(safe-area-inset-bottom, 0px);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.mobile-tab-bar--hidden {
  transform: translateY(100%);
}

.tab-bar-inner {
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: 54px;
  padding: 0 8px;
}

.tab-bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  gap: 2px;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: all 0.25s ease;
  padding: 4px 0;
  position: relative;
}

.tab-bar-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 28px;
  border-radius: 14px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  color: #94a3b8;
}

.tab-bar-item--active .tab-bar-icon {
  color: var(--color-primary, #2563eb);
  background: color-mix(in srgb, var(--color-primary, #2563eb) 10%, transparent);
  width: 48px;
  transform: translateY(-1px);
}

.tab-bar-label {
  font-size: 10px;
  font-weight: 500;
  color: #94a3b8;
  transition: color 0.25s ease;
  line-height: 1.2;
}

.tab-bar-item--active .tab-bar-label {
  color: var(--color-primary, #2563eb);
  font-weight: 600;
}

.tab-bar-item:active .tab-bar-icon {
  transform: scale(0.88);
}

/* 深色模式 */

</style>

<style>
/* MobileTabBar 深色模式（非 scoped） */
html.dark .mobile-tab-bar {
  background: rgba(17, 24, 39, 0.95) !important;
  border-top-color: rgba(255, 255, 255, 0.06) !important;
}
html.dark .tab-bar-icon {
  color: #64748b !important;
}
html.dark .tab-bar-item--active .tab-bar-icon {
  color: #60a5fa !important;
  background: rgba(96, 165, 250, 0.12) !important;
}
html.dark .tab-bar-label {
  color: #64748b !important;
}
html.dark .tab-bar-item--active .tab-bar-label {
  color: #60a5fa !important;
}
</style>

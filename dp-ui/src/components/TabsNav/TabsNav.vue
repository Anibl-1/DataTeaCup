<template>
  <div class="tabs-nav">
    <!-- 左侧滚动按钮 -->
    <div 
      v-show="showScrollBtn" 
      class="scroll-btn scroll-btn--left"
      @click="scrollLeft"
    >
      <n-icon size="14"><ChevronBackOutline /></n-icon>
    </div>
    
    <div ref="tabsWrapperRef" class="tabs-wrapper">
      <div ref="tabsScrollRef" class="tabs-scroll">
        <div
          v-for="tab in tabsStore.tabs"
          :key="tab.key"
          class="tab-item"
          :class="{ 
            'tab-item--active': tab.key === tabsStore.activeTab,
            'tab-item--home': tab.key === '/dashboard'
          }"
          @click="handleTabClick(tab)"
          @contextmenu.prevent="handleContextMenu($event, tab)"
        >
          <span class="tab-dot"></span>
          <n-icon v-if="tab.key === '/dashboard'" class="tab-icon" size="15">
            <HomeOutline />
          </n-icon>
          <span class="tab-title">{{ tab.title }}</span>
          <span
            v-if="tab.closable"
            class="tab-close"
            @click.stop="handleClose(tab.key)"
          >
            <n-icon size="11"><CloseOutline /></n-icon>
          </span>
        </div>
      </div>
    </div>
    
    <!-- 右侧滚动按钮 -->
    <div 
      v-show="showScrollBtn" 
      class="scroll-btn scroll-btn--right"
      @click="scrollRight"
    >
      <n-icon size="14"><ChevronForwardOutline /></n-icon>
    </div>
    
    <!-- 操作按钮 -->
    <div class="tabs-actions">
      <!-- 标签列表下拉菜单 -->
      <n-dropdown 
        :options="tabListOptions" 
        placement="bottom-end" 
        :style="{ maxHeight: '400px', overflowY: 'auto' }"
        @select="handleTabListSelect"
      >
        <div class="action-btn tabs-list-btn">
          <n-icon size="15"><ListOutline /></n-icon>
        </div>
      </n-dropdown>
      <n-tooltip trigger="hover" placement="bottom">
        <template #trigger>
          <div class="action-btn" @click="refreshCurrent">
            <n-icon size="15"><RefreshOutline /></n-icon>
          </div>
        </template>
        刷新当前页
      </n-tooltip>
      <n-dropdown :options="moreOptions" placement="bottom-end" @select="handleMoreSelect">
        <div class="action-btn">
          <n-icon size="15"><EllipsisHorizontalOutline /></n-icon>
        </div>
      </n-dropdown>
    </div>
    
    <!-- 右键菜单 -->
    <n-dropdown
      placement="bottom-start"
      trigger="manual"
      :x="contextMenuX"
      :y="contextMenuY"
      :options="contextMenuOptions"
      :show="showContextMenu"
      @select="handleContextMenuSelect"
      @clickoutside="showContextMenu = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { NIcon, NDropdown, NTooltip } from 'naive-ui'
import { 
  CloseOutline, 
  HomeOutline, 
  EllipsisHorizontalOutline,
  ChevronBackOutline,
  ChevronForwardOutline,
  RefreshOutline,
  ListOutline
} from '@vicons/ionicons5'
import { useTabsStore, type TabItem } from '@/stores/tabs'

const router = useRouter()
const tabsStore = useTabsStore()

// 滚动相关
const tabsWrapperRef = ref<HTMLElement | null>(null)
const tabsScrollRef = ref<HTMLElement | null>(null)
const showScrollBtn = ref(false)

// 检查是否需要显示滚动按钮
const checkScroll = () => {
  if (tabsWrapperRef.value && tabsScrollRef.value) {
    showScrollBtn.value = tabsScrollRef.value.scrollWidth > tabsWrapperRef.value.clientWidth
  }
}

// 滚动
const scrollLeft = () => {
  if (tabsScrollRef.value) {
    tabsScrollRef.value.scrollBy({ left: -200, behavior: 'smooth' })
  }
}

const scrollRight = () => {
  if (tabsScrollRef.value) {
    tabsScrollRef.value.scrollBy({ left: 200, behavior: 'smooth' })
  }
}

// 防抖定时器
let scrollDebounceTimer: number | null = null

// 滚动到指定标签（带防抖，避免频繁滚动导致跳动）
const scrollToTab = (key: string, immediate = false) => {
  // 清除之前的定时器
  if (scrollDebounceTimer) {
    clearTimeout(scrollDebounceTimer)
  }
  
  const doScroll = () => {
    nextTick(() => {
      if (!tabsScrollRef.value) return
      const tabIndex = tabsStore.tabs.findIndex(t => t.key === key)
      if (tabIndex === -1) return
      
      const tabElements = tabsScrollRef.value.querySelectorAll('.tab-item')
      const targetTab = tabElements[tabIndex] as HTMLElement
      if (!targetTab) return
      
      const scrollContainer = tabsScrollRef.value
      const containerWidth = scrollContainer.clientWidth
      const tabLeft = targetTab.offsetLeft
      const tabWidth = targetTab.offsetWidth
      const currentScroll = scrollContainer.scrollLeft
      
      // 检查标签是否已经在可视区域内
      const tabRight = tabLeft + tabWidth
      const viewLeft = currentScroll
      const viewRight = currentScroll + containerWidth
      
      // 如果标签已经完全可见，不需要滚动
      if (tabLeft >= viewLeft && tabRight <= viewRight) {
        return
      }
      
      // 计算需要滚动到的位置，让标签居中或可见
      const scrollTo = tabLeft - (containerWidth / 2) + (tabWidth / 2)
      scrollContainer.scrollTo({ left: Math.max(0, scrollTo), behavior: 'smooth' })
    })
  }
  
  if (immediate) {
    doScroll()
  } else {
    // 防抖延迟执行
    scrollDebounceTimer = window.setTimeout(doScroll, 100)
  }
}

// 右键菜单状态
const showContextMenu = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const contextMenuTab = ref<TabItem | null>(null)

// 右键菜单选项
const contextMenuOptions = computed(() => {
  const tab = contextMenuTab.value
  if (!tab) return []
  
  return [
    { label: '🔄 刷新页面', key: 'refresh' },
    { type: 'divider', key: 'd1' },
    { label: '✕ 关闭标签', key: 'close', disabled: !tab.closable },
    { label: '📌 关闭其他', key: 'closeOther' },
    { label: '⬅ 关闭左侧', key: 'closeLeft' },
    { label: '➡ 关闭右侧', key: 'closeRight' },
    { type: 'divider', key: 'd2' },
    { label: '🗑 关闭全部', key: 'closeAll' }
  ]
})

// 更多操作选项
const moreOptions = [
  { label: '📌 关闭其他标签', key: 'closeOtherCurrent' },
  { label: '🗑 关闭全部标签', key: 'closeAllTabs' }
]

// 标签列表选项
const tabListOptions = computed(() => {
  return tabsStore.tabs.map(tab => ({
    label: tab.key === tabsStore.activeTab ? `● ${tab.title}` : tab.title,
    key: tab.key,
    props: {
      style: tab.key === tabsStore.activeTab ? 'font-weight: bold; color: #409EFF;' : ''
    }
  }))
})

// 标签列表选择
const handleTabListSelect = (key: string) => {
  const tab = tabsStore.tabs.find(t => t.key === key)
  if (tab) {
    handleTabClick(tab)
    scrollToTab(key)
  }
}

// 刷新当前页
const refreshCurrent = () => {
  router.replace('/redirect' + tabsStore.activeTab)
}

// 点击标签
const handleTabClick = (tab: TabItem) => {
  if (tab.key === tabsStore.activeTab) return
  
  tabsStore.setActiveTab(tab.key)
  router.push(tab.key).catch(err => {
    if (err.name !== 'NavigationDuplicated') {
      console.error('路由跳转失败:', err)
    }
  })
}

// 关闭标签
const handleClose = (key: string) => {
  const redirectPath = tabsStore.closeTab(key)
  if (redirectPath) {
    router.push(redirectPath).catch(err => {
      if (err.name !== 'NavigationDuplicated') {
        console.error('路由跳转失败:', err)
      }
    })
  }
}

// 右键菜单
const handleContextMenu = (e: MouseEvent, tab: TabItem) => {
  contextMenuX.value = e.clientX
  contextMenuY.value = e.clientY
  contextMenuTab.value = tab
  showContextMenu.value = true
}

// 右键菜单选择
const handleContextMenuSelect = (key: string) => {
  showContextMenu.value = false
  const tab = contextMenuTab.value
  if (!tab) return
  
  switch (key) {
    case 'refresh':
      router.replace('/redirect' + tab.key)
      break
    case 'close':
      handleClose(tab.key)
      break
    case 'closeOther':
      tabsStore.closeOtherTabs(tab.key)
      if (tabsStore.activeTab !== tab.key) {
        router.push(tab.key)
      }
      break
    case 'closeLeft':
      tabsStore.closeLeftTabs(tab.key)
      if (!tabsStore.hasTab(tabsStore.activeTab)) {
        router.push(tab.key)
      }
      break
    case 'closeRight':
      tabsStore.closeRightTabs(tab.key)
      if (!tabsStore.hasTab(tabsStore.activeTab)) {
        router.push(tab.key)
      }
      break
    case 'closeAll':
      const homePath = tabsStore.closeAllTabs()
      router.push(homePath)
      break
  }
}

// 更多操作
const handleMoreSelect = (key: string) => {
  switch (key) {
    case 'closeOtherCurrent':
      tabsStore.closeOtherTabs(tabsStore.activeTab)
      break
    case 'closeAllTabs':
      const homePath = tabsStore.closeAllTabs()
      router.push(homePath)
      break
  }
}

// 鼠标滚轮横向滚动
const handleWheel = (e: WheelEvent) => {
  if (tabsScrollRef.value && e.deltaY !== 0) {
    e.preventDefault()
    tabsScrollRef.value.scrollBy({ left: e.deltaY, behavior: 'smooth' })
  }
}

onMounted(() => {
  checkScroll()
  window.addEventListener('resize', checkScroll)
  // 初始滚动到激活标签
  scrollToTab(tabsStore.activeTab)
  // 监听滚轮事件实现横向滚动
  tabsWrapperRef.value?.addEventListener('wheel', handleWheel, { passive: false })
})

onUnmounted(() => {
  window.removeEventListener('resize', checkScroll)
  tabsWrapperRef.value?.removeEventListener('wheel', handleWheel)
})

// 监听激活标签变化，自动滚动
watch(() => tabsStore.activeTab, (newTab, oldTab) => {
  // 只有当标签确实改变时才滚动
  if (newTab !== oldTab) {
    checkScroll()
    scrollToTab(newTab)
  }
})

// 监听标签数量变化
watch(() => tabsStore.tabs.length, (newLen, oldLen) => {
  checkScroll()
  // 只有新增标签时才滚动到新标签
  if (newLen > oldLen && tabsStore.tabs.length > 0) {
    nextTick(() => {
      const lastTab = tabsStore.tabs[tabsStore.tabs.length - 1]
      if (lastTab && lastTab.key === tabsStore.activeTab) {
        scrollToTab(lastTab.key, true) // 立即滚动
      }
    })
  }
})
</script>

<style scoped>
.tabs-nav {
  display: flex;
  align-items: center;
  background: var(--dp-bg-primary, #ffffff);
  border-bottom: 1px solid var(--dp-border-light, rgba(226,232,240,0.8));
  padding: 0;
  height: 40px;
  position: sticky;
  top: 56px;
  z-index: var(--dp-z-sticky, 190);
  box-shadow: 0 1px 0 rgba(0,0,0,0.04);
  flex-shrink: 0;
}

.scroll-btn {
  width: 28px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--dp-text-tertiary, #94a3b8);
  background: var(--dp-bg-primary, #fff);
  transition: all 0.18s ease;
  flex-shrink: 0;
  z-index: 2;
  border: none;
}

.scroll-btn:hover {
  color: var(--dp-color-primary, #2563eb);
  background: var(--dp-color-primary-light, rgba(37,99,235,0.06));
}

.scroll-btn--left {
  border-right: 1px solid var(--dp-border-light, rgba(226,232,240,0.8));
}

.scroll-btn--right {
  border-left: 1px solid var(--dp-border-light, rgba(226,232,240,0.8));
}

.tabs-wrapper {
  flex: 1;
  overflow: hidden;
  padding: 0 6px;
}

.tabs-scroll {
  display: flex;
  align-items: center;
  gap: 4px;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  padding: 5px 0;
}

.tabs-scroll::-webkit-scrollbar {
  display: none;
}

/* 标签项 */
.tab-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 0 12px;
  height: 30px;
  background: var(--dp-bg-secondary, #f8fafc);
  border: 1px solid var(--dp-border-light, rgba(226,232,240,0.7));
  border-radius: var(--dp-radius-md, 8px);
  cursor: pointer;
  transition: all 0.18s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 12.5px;
  color: var(--dp-text-secondary, #64748b);
  white-space: nowrap;
  flex-shrink: 0;
  user-select: none;
  -webkit-tap-highlight-color: transparent;
}

.tab-item:hover {
  color: var(--dp-color-primary, #2563eb);
  background: var(--dp-color-primary-light, rgba(37,99,235,0.06));
  border-color: rgba(37, 99, 235, 0.18);
}

/* 激活状态 - 底部品牌蓝色线 */
.tab-item--active {
  background: #fff;
  border-color: rgba(37, 99, 235, 0.18);
  border-bottom: 2px solid var(--dp-color-primary, #2563eb);
  color: var(--dp-color-primary, #2563eb);
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.08);
  font-weight: 500;
}

.tab-item--active:hover {
  color: var(--dp-color-primary, #2563eb);
}

/* 首页特殊样式 */
.tab-item--home.tab-item--active {
  background: #fff;
  border-color: rgba(16, 185, 129, 0.2);
  border-bottom: 2px solid #10b981;
  color: #059669;
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.08);
}

/* 切换动画 */
.tab-item--active {
  animation: tabActivate 150ms ease-out;
}

@keyframes tabActivate {
  from { transform: scale(0.97); opacity: 0.7; }
  to { transform: scale(1); opacity: 1; }
}

@media (prefers-reduced-motion: reduce) {
  .tab-item--active { animation: none; }
}

/* 状态点 */
.tab-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--dp-border-strong, #cbd5e1);
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.tab-item:hover .tab-dot {
  background: var(--dp-color-primary, #2563eb);
}

.tab-item--active .tab-dot {
  background: var(--dp-color-primary, #2563eb);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.2);
}

.tab-item--home .tab-dot {
  background: #10b981;
}

.tab-item--home:hover .tab-dot {
  background: #059669;
}

.tab-item--home.tab-item--active .tab-dot {
  background: #10b981;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
}

.tab-icon {
  flex-shrink: 0;
}

.tab-title {
  max-width: 96px;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 400;
  letter-spacing: 0.01em;
}

/* 关闭按钮 */
.tab-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  margin-left: 2px;
  transition: all 0.15s ease;
  flex-shrink: 0;
  color: var(--dp-text-tertiary, #94a3b8);
  opacity: 0.7;
}

.tab-item:hover .tab-close {
  opacity: 1;
}

.tab-close:hover {
  background: rgba(239, 68, 68, 0.12);
  color: #ef4444;
  opacity: 1;
}

.tab-item--active .tab-close {
  color: rgba(37, 99, 235, 0.5);
  opacity: 0.8;
}

.tab-item--active .tab-close:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  opacity: 1;
}

/* 操作按钮区域 */
.tabs-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 0;
  padding: 0;
  height: 100%;
  border-left: 1px solid var(--dp-border-light, rgba(226,232,240,0.8));
  background: var(--dp-bg-primary, #fff);
}

.action-btn {
  width: 36px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--dp-text-tertiary, #94a3b8);
  transition: all 0.18s ease;
  border-left: 1px solid var(--dp-border-light, rgba(226,232,240,0.5));
  position: relative;
}

.action-btn:first-child { border-left: none; }

.action-btn:hover {
  color: var(--dp-color-primary, #2563eb);
  background: var(--dp-color-primary-light, rgba(37,99,235,0.05));
}

.action-btn:active {
  transform: scale(0.92);
}

/* 标签列表按钮 */
.tabs-list-btn { gap: 3px; }

.tabs-count {
  font-size: 10px;
  background: rgba(37,99,235,0.08);
  color: var(--dp-color-primary, #2563eb);
  padding: 1px 5px;
  border-radius: 10px;
  min-width: 16px;
  text-align: center;
  font-weight: 600;
}


</style>

<!-- 深色模式：必须用非 scoped 块，:global(html.dark) 在 scoped 中会被 Vue SFC 编译器丢弃 -->
<style>
html.dark .tabs-nav {
  background: var(--dp-bg-primary, #0f172a) !important;
  border-color: var(--dp-border-light, rgba(255,255,255,0.05)) !important;
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.03) !important;
}

html.dark .scroll-btn {
  background: var(--dp-bg-primary, #0f172a) !important;
  color: #64748b !important;
  border-color: var(--dp-border-light, rgba(255,255,255,0.05)) !important;
}

html.dark .scroll-btn:hover {
  background: var(--dp-bg-elevated, #1e293b) !important;
  color: var(--dp-color-primary, #60a5fa) !important;
}

html.dark .scroll-btn--left,
html.dark .scroll-btn--right {
  border-color: var(--dp-border-light, rgba(255,255,255,0.05)) !important;
}

html.dark .tab-item {
  background: var(--dp-bg-elevated, rgba(255,255,255,0.04)) !important;
  border-color: var(--dp-border-light, rgba(255,255,255,0.06)) !important;
  color: var(--dp-text-secondary, #94a3b8) !important;
}

html.dark .tab-item:hover {
  color: #e2e8f0 !important;
  background: rgba(59, 130, 246, 0.08) !important;
  border-color: rgba(59, 130, 246, 0.15) !important;
}

html.dark .tab-item--active {
  background: rgba(37, 99, 235, 0.14) !important;
  border-color: rgba(59, 130, 246, 0.22) !important;
  border-bottom: 2px solid var(--dp-color-primary, #60a5fa) !important;
  color: var(--dp-color-primary, #60a5fa) !important;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.12) !important;
}

html.dark .tab-item--active:hover {
  background: rgba(37, 99, 235, 0.18) !important;
}

html.dark .tab-item--home.tab-item--active {
  background: rgba(16, 185, 129, 0.12) !important;
  border-color: rgba(16, 185, 129, 0.2) !important;
  border-bottom: 2px solid #34d399 !important;
  color: #34d399 !important;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.1) !important;
}

html.dark .tab-close {
  color: #64748b !important;
}

html.dark .tab-close:hover {
  background: rgba(239, 68, 68, 0.15) !important;
  color: #f87171 !important;
}

html.dark .tab-item--active .tab-close {
  color: #60a5fa !important;
}

html.dark .tab-item--active .tab-close:hover {
  background: rgba(239, 68, 68, 0.15) !important;
  color: #f87171 !important;
}

html.dark .tab-dot {
  background: #475569 !important;
}

html.dark .tab-item:hover .tab-dot {
  background: #60a5fa !important;
}

html.dark .tab-item--active .tab-dot {
  background: #3b82f6 !important;
  box-shadow: none !important;
}

html.dark .tab-item--home .tab-dot {
  background: #059669 !important;
}

html.dark .tab-item--home:hover .tab-dot {
  background: #34d399 !important;
}

html.dark .tab-item--home.tab-item--active .tab-dot {
  background: #10b981 !important;
  box-shadow: none !important;
}

html.dark .tab-title {
  color: inherit !important;
}

html.dark .tabs-actions {
  background: var(--dp-bg-primary, #0f172a) !important;
  border-color: var(--dp-border-light, rgba(255,255,255,0.05)) !important;
}

html.dark .action-btn {
  color: #64748b !important;
  border-color: var(--dp-border-light, rgba(255,255,255,0.04)) !important;
}

html.dark .action-btn:hover {
  color: var(--dp-color-primary, #60a5fa) !important;
  background: rgba(59, 130, 246, 0.08) !important;
}

html.dark .tabs-count {
  background: #3b82f6 !important;
  box-shadow: none !important;
}
</style>

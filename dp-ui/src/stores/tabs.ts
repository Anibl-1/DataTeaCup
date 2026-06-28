/**
 * 标签页管理 Store
 * 参考若依框架实现，使用 sessionStorage 存储
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface TabItem {
  key: string           // 唯一标识（路由路径）
  title: string         // 标签标题
  name?: string         // 路由名称
  closable: boolean     // 是否可关闭
  icon?: string         // 图标
}

// 默认首页标签
const DEFAULT_TAB: TabItem = { 
  key: '/dashboard', 
  title: '首页', 
  name: 'Dashboard', 
  closable: false 
}

// 存储 key
const TABS_KEY = 'tabs-views'
const ACTIVE_KEY = 'tabs-active'

export const useTabsStore = defineStore('tabs', () => {
  // 标签列表
  const tabs = ref<TabItem[]>([DEFAULT_TAB])
  
  // 当前激活的标签
  const activeTab = ref('/dashboard')
  
  // 是否已初始化
  const initialized = ref(false)
  
  // 是否跳过下一次自动添加标签（用于 replaceTab）
  const skipNextAdd = ref(false)
  
  // 缓存的组件名称列表（用于 keep-alive）
  // 限制最大缓存数量，防止打开过多页面导致内存溢出和白屏
  const MAX_CACHED_VIEWS = 6
  // 排除重量级全页面组件（包含 CodeMirror / ECharts / Canvas 等复杂子组件，
  // 在 keep-alive 缓存/恢复时容易导致 DOM 状态异常或占用大量内存）
  const excludeFromCache = [
    'DatabaseManager', 'PipelineDesigner', 'PageDesigner',
    'ChartDesigner', 'ReportDesigner', 'SystemMonitor',
    'AiAssistant', 'AiChartDesign', 'ChatView', 'DataLineage',
    'PageView', 'BigscreenView', 'TableDataManage'
  ]
  const cachedViews = computed(() => {
    const eligible = tabs.value
      .filter(tab => tab.name && !excludeFromCache.includes(tab.name))
      .map(tab => tab.name!)
    // 只缓存最近的 N 个视图，淘汰最早的
    if (eligible.length > MAX_CACHED_VIEWS) {
      return eligible.slice(eligible.length - MAX_CACHED_VIEWS)
    }
    return eligible
  })
  
  /**
   * 初始化标签页（从 sessionStorage 恢复）
   * 只在登录成功后调用
   */
  const initTabs = () => {
    if (initialized.value) return
    
    try {
      const storedTabs = sessionStorage.getItem(TABS_KEY)
      const storedActive = sessionStorage.getItem(ACTIVE_KEY)
      
      if (storedTabs) {
        const parsedTabs = JSON.parse(storedTabs) as TabItem[]
        // 确保首页标签存在
        if (!parsedTabs.find(t => t.key === '/dashboard')) {
          parsedTabs.unshift(DEFAULT_TAB)
        }
        tabs.value = parsedTabs
      }
      
      if (storedActive) {
        activeTab.value = storedActive
      }
    } catch (e) {
      console.warn('恢复标签页失败:', e)
      resetTabs()
    }
    
    initialized.value = true
  }
  
  /**
   * 保存到 sessionStorage
   */
  const saveTabs = () => {
    try {
      sessionStorage.setItem(TABS_KEY, JSON.stringify(tabs.value))
      sessionStorage.setItem(ACTIVE_KEY, activeTab.value)
    } catch (e) {
      console.warn('保存标签页失败:', e)
    }
  }
  
  /**
   * 重置标签页（恢复默认状态）
   */
  const resetTabs = () => {
    tabs.value = [DEFAULT_TAB]
    activeTab.value = '/dashboard'
    initialized.value = false
  }
  
  /**
   * 清空所有标签页（登出时调用）
   * 同时清除 sessionStorage
   */
  const clearTabs = () => {
    resetTabs()
    sessionStorage.removeItem(TABS_KEY)
    sessionStorage.removeItem(ACTIVE_KEY)
  }
  
  /**
   * 添加标签（如果已存在则只激活）
   */
  // 管理页 ↔ 设计器/详情页 的关联映射
  // 当导航到管理页时，如果存在对应的设计器标签，应替换而非新增
  const RELATED_PAGE_GROUPS: string[][] = [
    ['/page-manage', '/page-designer'],
    ['/report-manage', '/report-designer'],
    ['/chart-manage', '/chart-designer', '/ai-chart-design'],
  ]

  /**
   * 查找与目标路径关联的已存在标签索引
   * 例如：添加 '/page-manage' 时查找 '/page-designer/5' 等
   */
  const findRelatedTabIndex = (targetPath: string): number => {
    const basePath = (targetPath.split('?')[0] ?? '').replace(/\/+$/, '')
    for (const group of RELATED_PAGE_GROUPS) {
      // 检查目标路径是否属于某个关联组
      const belongsToGroup = group.some(prefix => basePath === prefix || basePath.startsWith(prefix + '/'))
      if (!belongsToGroup) continue
      
      // 查找同组内其他路径的已有标签
      return tabs.value.findIndex(t => {
        const existingPath = (t.key.split('?')[0] ?? '').replace(/\/+$/, '')
        if (existingPath === basePath) return false // 跳过自身
        return group.some(prefix => existingPath === prefix || existingPath.startsWith(prefix + '/'))
      })
    }
    return -1
  }

  const addTab = (tab: TabItem) => {
    // 确保已初始化
    if (!initialized.value) {
      initTabs()
    }
    
    // 如果设置了跳过标记，则跳过本次添加
    if (skipNextAdd.value) {
      skipNextAdd.value = false
      return
    }
    
    const exists = tabs.value.find(t => t.key === tab.key)
    if (!exists) {
      // 检查是否存在子路径的标签（防止重复）
      // 例如：添加 '/chart-center' 时已存在 '/chart-center/5'，应替换而非新增
      const tabBasePath = (tab.key.split('?')[0] ?? '').replace(/\/+$/, '')
      const childIdx = tabs.value.findIndex(t => {
        const existingPath = (t.key.split('?')[0] ?? '').replace(/\/+$/, '')
        return existingPath !== tabBasePath && existingPath.startsWith(tabBasePath + '/')
      })
      
      if (childIdx !== -1) {
        // 在原位置替换子路径标签（用户从详情页回到列表页）
        const replacement = {
          ...tab,
          closable: tab.key !== '/dashboard'
        }
        tabs.value.splice(childIdx, 1, replacement)
      } else {
        // 检查关联页面（管理页 ↔ 设计器）
        const relatedIdx = findRelatedTabIndex(tab.key)
        if (relatedIdx !== -1) {
          // 替换关联标签（例如从设计器标签替换回管理页标签）
          tabs.value.splice(relatedIdx, 1, {
            ...tab,
            closable: tab.key !== '/dashboard'
          })
        } else {
          tabs.value.push({
            ...tab,
            closable: tab.key !== '/dashboard'
          })
        }
      }
    } else {
      // 更新标题（可能动态变化）
      exists.title = tab.title
      if (tab.name !== undefined) {
        exists.name = tab.name
      }
    }
    activeTab.value = tab.key
    saveTabs()
  }
  
  /**
   * 关闭标签，返回需要跳转的路径
   */
  const closeTab = (key: string): string | null => {
    const index = tabs.value.findIndex(t => t.key === key)
    if (index === -1) return null
    
    const tab = tabs.value[index]
    if (!tab || !tab.closable) return null
    
    // 移除标签
    tabs.value.splice(index, 1)
    
    // 如果关闭的是当前激活的标签，需要切换到其他标签
    if (activeTab.value === key) {
      const newIndex = index >= tabs.value.length ? tabs.value.length - 1 : index
      const newTab = tabs.value[newIndex] || tabs.value[0]
      activeTab.value = newTab?.key || '/dashboard'
      saveTabs()
      return activeTab.value
    }
    
    saveTabs()
    return null
  }
  
  /**
   * 关闭其他标签
   */
  const closeOtherTabs = (key: string) => {
    tabs.value = tabs.value.filter(t => !t.closable || t.key === key)
    if (!tabs.value.find(t => t.key === '/dashboard')) {
      tabs.value.unshift(DEFAULT_TAB)
    }
    activeTab.value = key
    saveTabs()
  }
  
  /**
   * 关闭所有可关闭的标签
   */
  const closeAllTabs = (): string => {
    tabs.value = tabs.value.filter(t => !t.closable)
    if (!tabs.value.find(t => t.key === '/dashboard')) {
      tabs.value.unshift(DEFAULT_TAB)
    }
    activeTab.value = '/dashboard'
    saveTabs()
    return '/dashboard'
  }
  
  /**
   * 关闭左侧标签
   */
  const closeLeftTabs = (key: string) => {
    const index = tabs.value.findIndex(t => t.key === key)
    if (index <= 0) return
    
    const newTabs: TabItem[] = []
    tabs.value.forEach((t, i) => {
      if (!t.closable || i >= index) {
        newTabs.push(t)
      }
    })
    tabs.value = newTabs
    
    if (!tabs.value.find(t => t.key === activeTab.value)) {
      activeTab.value = key
    }
    saveTabs()
  }
  
  /**
   * 关闭右侧标签
   */
  const closeRightTabs = (key: string) => {
    const index = tabs.value.findIndex(t => t.key === key)
    if (index === -1) return
    
    const newTabs: TabItem[] = []
    tabs.value.forEach((t, i) => {
      if (!t.closable || i <= index) {
        newTabs.push(t)
      }
    })
    tabs.value = newTabs
    
    if (!tabs.value.find(t => t.key === activeTab.value)) {
      activeTab.value = key
    }
    saveTabs()
  }
  
  /**
   * 设置当前标签
   */
  const setActiveTab = (key: string) => {
    if (tabs.value.find(t => t.key === key)) {
      activeTab.value = key
      saveTabs()
    }
  }
  
  /**
   * 检查标签是否存在
   */
  const hasTab = (key: string): boolean => {
    return tabs.value.some(t => t.key === key)
  }
  
  /**
   * 更新标签标题
   */
  const updateTabTitle = (key: string, title: string) => {
    const tab = tabs.value.find(t => t.key === key)
    if (tab) {
      tab.title = title
      saveTabs()
    }
  }

  /**
   * 替换当前标签页（在当前位置打开新页面，不新增标签）
   * @param oldKey 要替换的标签key
   * @param newTab 新标签信息
   */
  const replaceTab = (oldKey: string, newTab: TabItem) => {
    // 尝试精确匹配
    let index = tabs.value.findIndex(t => t.key === oldKey)
    
    // 如果精确匹配失败，尝试不带查询参数的匹配
    if (index === -1) {
      const oldPath = oldKey.split('?')[0]
      index = tabs.value.findIndex(t => t.key.split('?')[0] === oldPath)
    }
    
    // 如果还是找不到，尝试匹配当前激活的标签
    if (index === -1) {
      index = tabs.value.findIndex(t => t.key === activeTab.value)
    }
    
    if (index !== -1) {
      // 在原位置替换
      tabs.value[index] = {
        ...newTab,
        closable: newTab.key !== '/dashboard'
      }
      activeTab.value = newTab.key
      // 设置跳过标记，防止 MainLayout 重复添加
      skipNextAdd.value = true
      saveTabs()
    } else {
      // 如果找不到旧标签，直接添加新标签（不跳过）
      addTab(newTab)
    }
  }

  /**
   * 关闭当前标签并跳转到指定页面（不新增标签，直接替换）
   * @param currentKey 当前标签key
   * @param targetKey 目标页面key（可能包含查询参数）
   * @param targetTitle 目标页面标题
   */
  const closeAndNavigate = (currentKey: string, targetKey: string, targetTitle: string) => {
    const index = tabs.value.findIndex(t => t.key === currentKey || t.key.split('?')[0] === currentKey.split('?')[0])
    
    // 提取不带查询参数的路径用于标签key
    const targetPath = targetKey.split('?')[0]
    
    // 检查目标标签是否已存在（使用不带查询参数的路径匹配）
    const targetIndex = tabs.value.findIndex(t => t.key === targetPath || t.key.split('?')[0] === targetPath)
    const targetExists = targetIndex !== -1 ? tabs.value[targetIndex] : null
    
    // 设置跳过标记，防止 MainLayout 的路由监听重复添加
    skipNextAdd.value = true
    
    if (targetExists && targetIndex !== index) {
      // 目标已存在且不是当前标签，关闭当前标签并激活目标
      if (index !== -1 && tabs.value[index]?.closable) {
        tabs.value.splice(index, 1)
      }
      // 更新目标标签的key和标题
      targetExists.key = targetKey
      targetExists.title = targetTitle
      activeTab.value = targetKey
    } else if (index !== -1) {
      // 目标不存在或就是当前标签，在当前位置替换
      tabs.value[index] = {
        key: targetKey,
        title: targetTitle,
        closable: targetPath !== '/dashboard'
      }
      activeTab.value = targetKey
    } else {
      // 当前标签不存在，直接添加目标（不跳过）
      skipNextAdd.value = false
      addTab({
        key: targetKey,
        title: targetTitle,
        closable: targetPath !== '/dashboard'
      })
      return
    }
    
    saveTabs()
  }
  
  return {
    tabs,
    activeTab,
    cachedViews,
    initialized,
    skipNextAdd,
    initTabs,
    addTab,
    closeTab,
    closeOtherTabs,
    closeAllTabs,
    closeLeftTabs,
    closeRightTabs,
    setActiveTab,
    hasTab,
    updateTabTitle,
    clearTabs,
    resetTabs,
    replaceTab,
    closeAndNavigate
  }
})

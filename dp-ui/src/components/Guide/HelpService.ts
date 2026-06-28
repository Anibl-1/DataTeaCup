/**
 * 帮助文档服务
 * Help Documentation Service
 * 
 * 管理帮助文档的存储、检索和搜索功能。
 * 
 * 需求 19.3: THE DataTeaCup SHALL 提供内置的帮助文档系统
 * 
 * @module HelpService
 */

import { ref, readonly, computed } from 'vue'
import type {
  HelpArticle,
  HelpCategory,
  HelpCategoryInfo,
  HelpSearchResult,
  HelpCenterState,
  IHelpService
} from './helpTypes'

// ==================== 常量定义 ====================

/** 分类信息配置 */
export const HELP_CATEGORIES: HelpCategoryInfo[] = [
  {
    id: 'getting-started',
    name: '入门指南',
    description: '快速了解平台基本功能和操作流程',
    icon: 'rocket',
    order: 1
  },
  {
    id: 'report-design',
    name: '报表设计',
    description: '学习如何创建和配置数据报表',
    icon: 'document',
    order: 2
  },
  {
    id: 'chart-design',
    name: '图表设计',
    description: '掌握数据可视化图表的设计技巧',
    icon: 'bar-chart',
    order: 3
  },
  {
    id: 'data-source',
    name: '数据源管理',
    description: '配置和管理数据库连接',
    icon: 'database',
    order: 4
  },
  {
    id: 'data-masking',
    name: '数据脱敏',
    description: '保护敏感数据的脱敏配置',
    icon: 'shield',
    order: 5
  },
  {
    id: 'query-builder',
    name: '查询构建',
    description: '使用可视化工具构建SQL查询',
    icon: 'code',
    order: 6
  },
  {
    id: 'export',
    name: '导出功能',
    description: '导出报表和图表数据',
    icon: 'download',
    order: 7
  },
  {
    id: 'performance',
    name: '性能优化',
    description: '提升系统性能的最佳实践',
    icon: 'speedometer',
    order: 8
  },
  {
    id: 'troubleshooting',
    name: '故障排除',
    description: '常见问题的解决方案',
    icon: 'bug',
    order: 9
  },
  {
    id: 'faq',
    name: '常见问题',
    description: '用户常见问题解答',
    icon: 'help-circle',
    order: 10
  }
]

/** 默认状态 */
const DEFAULT_STATE: HelpCenterState = {
  isOpen: false,
  selectedCategory: null,
  currentArticle: null,
  searchQuery: '',
  searchResults: [],
  isSearching: false,
  history: []
}

/** 最大历史记录数 */
const MAX_HISTORY_LENGTH = 20

/** 搜索结果最大数量 */
const MAX_SEARCH_RESULTS = 20

// ==================== 搜索工具函数 ====================

/**
 * 计算字符串相似度（简化的编辑距离）
 */
function calculateSimilarity(str1: string, str2: string): number {
  const s1 = str1.toLowerCase()
  const s2 = str2.toLowerCase()
  
  if (s1 === s2) return 1
  if (s1.includes(s2) || s2.includes(s1)) return 0.8
  
  // 计算共同字符比例
  const chars1 = new Set(s1.split(''))
  const chars2 = new Set(s2.split(''))
  const intersection = [...chars1].filter(c => chars2.has(c))
  
  return intersection.length / Math.max(chars1.size, chars2.size) * 0.5
}

/**
 * 高亮匹配文本
 */
function highlightText(text: string, query: string): string {
  if (!query.trim()) return text
  
  const words = query.toLowerCase().split(/\s+/).filter(w => w.length > 0)
  let result = text
  
  words.forEach(word => {
    const regex = new RegExp(`(${escapeRegExp(word)})`, 'gi')
    result = result.replace(regex, '<mark>$1</mark>')
  })
  
  return result
}

/**
 * 转义正则表达式特殊字符
 */
function escapeRegExp(str: string): string {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

// ==================== 帮助服务类 ====================

/**
 * 帮助文档服务类
 * 
 * 提供帮助文档的完整管理功能，包括：
 * - 文章注册和存储
 * - 分类管理
 * - 全文搜索
 * - 上下文相关帮助
 */
export class HelpService implements IHelpService {
  /** 文章存储 */
  private articles: Map<string, HelpArticle> = new Map()
  
  /** 分类索引 */
  private categoryIndex: Map<HelpCategory, Set<string>> = new Map()
  
  /** 功能索引（用于上下文帮助） */
  private featureIndex: Map<string, Set<string>> = new Map()
  
  /** 关键词索引 */
  private keywordIndex: Map<string, Set<string>> = new Map()
  
  /** 状态变更回调 */
  private onStateChange: ((state: HelpCenterState) => void) | null = null
  
  /** 当前状态 */
  private state: HelpCenterState = { ...DEFAULT_STATE }
  
  constructor() {
    // 初始化分类索引
    HELP_CATEGORIES.forEach(cat => {
      this.categoryIndex.set(cat.id, new Set())
    })
  }

  /**
   * 设置状态变更回调
   */
  setOnStateChange(callback: (state: HelpCenterState) => void): void {
    this.onStateChange = callback
  }
  
  /**
   * 触发状态变更
   */
  private emitStateChange(): void {
    if (this.onStateChange) {
      this.onStateChange({ ...this.state })
    }
  }
  
  /**
   * 获取当前状态
   */
  getState(): HelpCenterState {
    return { ...this.state }
  }
  
  /**
   * 获取所有分类
   */
  getCategories(): HelpCategoryInfo[] {
    return [...HELP_CATEGORIES].sort((a, b) => a.order - b.order)
  }
  
  /**
   * 获取分类信息
   */
  getCategoryInfo(category: HelpCategory): HelpCategoryInfo | undefined {
    return HELP_CATEGORIES.find(c => c.id === category)
  }
  
  /**
   * 注册文章
   */
  registerArticle(article: HelpArticle): void {
    if (!article.id || !article.title || !article.content) {
      console.warn('[HelpService] 无效的文章配置:', article)
      return
    }
    
    // 存储文章
    this.articles.set(article.id, article)
    
    // 更新分类索引
    const categorySet = this.categoryIndex.get(article.category)
    if (categorySet) {
      categorySet.add(article.id)
    }
    
    // 更新功能索引
    if (article.relatedFeatures) {
      article.relatedFeatures.forEach(feature => {
        if (!this.featureIndex.has(feature)) {
          this.featureIndex.set(feature, new Set())
        }
        this.featureIndex.get(feature)!.add(article.id)
      })
    }
    
    // 更新关键词索引
    article.keywords.forEach(keyword => {
      const lowerKeyword = keyword.toLowerCase()
      if (!this.keywordIndex.has(lowerKeyword)) {
        this.keywordIndex.set(lowerKeyword, new Set())
      }
      this.keywordIndex.get(lowerKeyword)!.add(article.id)
    })
  }
  
  /**
   * 批量注册文章
   */
  registerArticles(articles: HelpArticle[]): void {
    articles.forEach(article => this.registerArticle(article))
  }
  
  /**
   * 获取文章详情
   */
  getArticle(articleId: string): HelpArticle | undefined {
    return this.articles.get(articleId)
  }
  
  /**
   * 获取所有文章
   */
  getAllArticles(): HelpArticle[] {
    return Array.from(this.articles.values())
  }
  
  /**
   * 获取分类下的文章
   */
  getArticlesByCategory(category: HelpCategory): HelpArticle[] {
    const articleIds = this.categoryIndex.get(category)
    if (!articleIds) return []
    
    const articles: HelpArticle[] = []
    articleIds.forEach(id => {
      const article = this.articles.get(id)
      if (article) {
        articles.push(article)
      }
    })
    
    // 按 order 排序
    return articles.sort((a, b) => (a.order ?? 999) - (b.order ?? 999))
  }
  
  /**
   * 获取热门文章
   */
  getPopularArticles(): HelpArticle[] {
    return Array.from(this.articles.values())
      .filter(article => article.isPopular)
      .sort((a, b) => (a.order ?? 999) - (b.order ?? 999))
  }
  
  /**
   * 获取上下文相关的帮助文章
   */
  getContextualHelp(feature: string): HelpArticle[] {
    const articleIds = this.featureIndex.get(feature)
    if (!articleIds) return []
    
    const articles: HelpArticle[] = []
    articleIds.forEach(id => {
      const article = this.articles.get(id)
      if (article) {
        articles.push(article)
      }
    })
    
    return articles.sort((a, b) => (a.order ?? 999) - (b.order ?? 999))
  }
  
  /**
   * 搜索文章
   */
  search(query: string): HelpSearchResult[] {
    if (!query.trim()) return []
    
    const queryLower = query.toLowerCase()
    const queryWords = queryLower.split(/\s+/).filter(w => w.length > 0)
    const results: Map<string, HelpSearchResult> = new Map()
    
    // 1. 关键词精确匹配
    queryWords.forEach(word => {
      const matchedIds = this.keywordIndex.get(word)
      if (matchedIds) {
        matchedIds.forEach(id => {
          const article = this.articles.get(id)
          if (article) {
            const existing = results.get(id)
            if (existing) {
              existing.score += 10
              existing.matchedKeywords.push(word)
            } else {
              results.set(id, {
                article,
                score: 10,
                matchedKeywords: [word],
                highlightedTitle: highlightText(article.title, query),
                highlightedSummary: highlightText(article.summary, query)
              })
            }
          }
        })
      }
    })
    
    // 2. 标题匹配
    this.articles.forEach((article, id) => {
      const titleLower = article.title.toLowerCase()
      let titleScore = 0
      
      queryWords.forEach(word => {
        if (titleLower.includes(word)) {
          titleScore += 8
        }
      })
      
      if (titleScore > 0) {
        const existing = results.get(id)
        if (existing) {
          existing.score += titleScore
        } else {
          results.set(id, {
            article,
            score: titleScore,
            matchedKeywords: [],
            highlightedTitle: highlightText(article.title, query),
            highlightedSummary: highlightText(article.summary, query)
          })
        }
      }
    })
    
    // 3. 摘要匹配
    this.articles.forEach((article, id) => {
      const summaryLower = article.summary.toLowerCase()
      let summaryScore = 0
      
      queryWords.forEach(word => {
        if (summaryLower.includes(word)) {
          summaryScore += 4
        }
      })
      
      if (summaryScore > 0) {
        const existing = results.get(id)
        if (existing) {
          existing.score += summaryScore
        } else {
          results.set(id, {
            article,
            score: summaryScore,
            matchedKeywords: [],
            highlightedTitle: highlightText(article.title, query),
            highlightedSummary: highlightText(article.summary, query)
          })
        }
      }
    })
    
    // 4. 内容匹配
    this.articles.forEach((article, id) => {
      const contentLower = article.content.toLowerCase()
      let contentScore = 0
      
      queryWords.forEach(word => {
        if (contentLower.includes(word)) {
          contentScore += 2
        }
      })
      
      if (contentScore > 0) {
        const existing = results.get(id)
        if (existing) {
          existing.score += contentScore
        } else {
          results.set(id, {
            article,
            score: contentScore,
            matchedKeywords: [],
            highlightedTitle: highlightText(article.title, query),
            highlightedSummary: highlightText(article.summary, query)
          })
        }
      }
    })
    
    // 排序并限制结果数量
    return Array.from(results.values())
      .sort((a, b) => b.score - a.score)
      .slice(0, MAX_SEARCH_RESULTS)
  }
  
  // ==================== 状态管理方法 ====================
  
  /**
   * 打开帮助中心
   */
  open(category?: HelpCategory): void {
    this.state.isOpen = true
    if (category) {
      this.state.selectedCategory = category
    }
    this.emitStateChange()
  }
  
  /**
   * 关闭帮助中心
   */
  close(): void {
    this.state.isOpen = false
    this.state.searchQuery = ''
    this.state.searchResults = []
    this.emitStateChange()
  }
  
  /**
   * 切换帮助中心显示状态
   */
  toggle(): void {
    if (this.state.isOpen) {
      this.close()
    } else {
      this.open()
    }
  }
  
  /**
   * 选择分类
   */
  selectCategory(category: HelpCategory | null): void {
    this.state.selectedCategory = category
    this.state.currentArticle = null
    this.emitStateChange()
  }
  
  /**
   * 查看文章
   */
  viewArticle(articleId: string): void {
    const article = this.articles.get(articleId)
    if (article) {
      this.state.currentArticle = article
      this.state.selectedCategory = article.category
      
      // 添加到历史记录
      this.addToHistory(articleId)
      
      this.emitStateChange()
    }
  }
  
  /**
   * 返回文章列表
   */
  backToList(): void {
    this.state.currentArticle = null
    this.emitStateChange()
  }
  
  /**
   * 设置搜索关键词
   */
  setSearchQuery(query: string): void {
    this.state.searchQuery = query
    this.state.isSearching = true
    
    if (query.trim()) {
      this.state.searchResults = this.search(query)
    } else {
      this.state.searchResults = []
    }
    
    this.state.isSearching = false
    this.emitStateChange()
  }
  
  /**
   * 清除搜索
   */
  clearSearch(): void {
    this.state.searchQuery = ''
    this.state.searchResults = []
    this.emitStateChange()
  }
  
  /**
   * 添加到历史记录
   */
  private addToHistory(articleId: string): void {
    // 移除已存在的记录
    const index = this.state.history.indexOf(articleId)
    if (index > -1) {
      this.state.history.splice(index, 1)
    }
    
    // 添加到开头
    this.state.history.unshift(articleId)
    
    // 限制历史记录长度
    if (this.state.history.length > MAX_HISTORY_LENGTH) {
      this.state.history = this.state.history.slice(0, MAX_HISTORY_LENGTH)
    }
  }
  
  /**
   * 获取浏览历史
   */
  getHistory(): HelpArticle[] {
    return this.state.history
      .map(id => this.articles.get(id))
      .filter((article): article is HelpArticle => article !== undefined)
  }
  
  /**
   * 清除历史记录
   */
  clearHistory(): void {
    this.state.history = []
    this.emitStateChange()
  }
}

// ==================== 单例实例 ====================

/** 全局帮助服务实例 */
let helpServiceInstance: HelpService | null = null

/**
 * 获取帮助服务实例（单例）
 */
export function getHelpService(): HelpService {
  if (!helpServiceInstance) {
    helpServiceInstance = new HelpService()
  }
  return helpServiceInstance
}

/**
 * 创建新的帮助服务实例
 */
export function createHelpService(): HelpService {
  return new HelpService()
}

// ==================== Vue Composable ====================

/**
 * 帮助服务 Composable
 * 
 * 提供响应式的帮助服务访问
 * 
 * @example
 * ```typescript
 * const { 
 *   isOpen, 
 *   categories,
 *   currentArticle,
 *   open, 
 *   close,
 *   search 
 * } = useHelp()
 * 
 * // 打开帮助中心
 * open('getting-started')
 * ```
 */
export function useHelp() {
  const service = getHelpService()
  
  // 响应式状态
  const state = ref<HelpCenterState>(service.getState())
  
  // 监听状态变更
  service.setOnStateChange((newState) => {
    state.value = newState
  })
  
  // 计算属性
  const isOpen = computed(() => state.value.isOpen)
  const selectedCategory = computed(() => state.value.selectedCategory)
  const currentArticle = computed(() => state.value.currentArticle)
  const searchQuery = computed(() => state.value.searchQuery)
  const searchResults = computed(() => state.value.searchResults)
  const isSearching = computed(() => state.value.isSearching)
  const categories = computed(() => service.getCategories())
  
  return {
    // 状态
    state: readonly(state),
    isOpen,
    selectedCategory,
    currentArticle,
    searchQuery,
    searchResults,
    isSearching,
    categories,
    
    // 方法
    open: (category?: HelpCategory) => service.open(category),
    close: () => service.close(),
    toggle: () => service.toggle(),
    selectCategory: (category: HelpCategory | null) => service.selectCategory(category),
    viewArticle: (articleId: string) => service.viewArticle(articleId),
    backToList: () => service.backToList(),
    setSearchQuery: (query: string) => service.setSearchQuery(query),
    clearSearch: () => service.clearSearch(),
    
    // 数据获取方法
    getArticle: (id: string) => service.getArticle(id),
    getArticlesByCategory: (category: HelpCategory) => service.getArticlesByCategory(category),
    getPopularArticles: () => service.getPopularArticles(),
    getContextualHelp: (feature: string) => service.getContextualHelp(feature),
    getHistory: () => service.getHistory(),
    clearHistory: () => service.clearHistory(),
    
    // 注册方法
    registerArticle: (article: HelpArticle) => service.registerArticle(article),
    registerArticles: (articles: HelpArticle[]) => service.registerArticles(articles)
  }
}

// ==================== 导出类型 ====================

export type { HelpArticle, HelpCategory, HelpCategoryInfo, HelpSearchResult, HelpCenterState }


/**
 * 帮助文档系统类型定义
 * Help Documentation System Type Definitions
 * 
 * 需求 19.3: THE DataTeaCup SHALL 提供内置的帮助文档系统
 */

/**
 * 帮助文章分类
 */
export type HelpCategory = 
  | 'getting-started'    // 入门指南
  | 'report-design'      // 报表设计
  | 'chart-design'       // 图表设计
  | 'data-source'        // 数据源管理
  | 'data-masking'       // 数据脱敏
  | 'query-builder'      // 查询构建
  | 'export'             // 导出功能
  | 'performance'        // 性能优化
  | 'troubleshooting'    // 故障排除
  | 'faq'                // 常见问题

/**
 * 帮助文章接口
 */
export interface HelpArticle {
  /** 文章唯一标识 */
  id: string
  /** 文章标题 */
  title: string
  /** 文章摘要 */
  summary: string
  /** 文章内容（Markdown 格式） */
  content: string
  /** 文章分类 */
  category: HelpCategory
  /** 关联的功能/页面路径 */
  relatedFeatures?: string[]
  /** 关联的关键词（用于搜索） */
  keywords: string[]
  /** 排序权重（越小越靠前） */
  order?: number
  /** 是否为热门文章 */
  isPopular?: boolean
  /** 最后更新时间 */
  lastUpdated?: string
}

/**
 * 帮助分类信息
 */
export interface HelpCategoryInfo {
  /** 分类标识 */
  id: HelpCategory
  /** 分类名称 */
  name: string
  /** 分类描述 */
  description: string
  /** 分类图标 */
  icon: string
  /** 排序权重 */
  order: number
}

/**
 * 搜索结果项
 */
export interface HelpSearchResult {
  /** 文章信息 */
  article: HelpArticle
  /** 匹配分数（越高越相关） */
  score: number
  /** 匹配的关键词 */
  matchedKeywords: string[]
  /** 高亮的标题 */
  highlightedTitle?: string
  /** 高亮的摘要 */
  highlightedSummary?: string
}

/**
 * 帮助中心状态
 */
export interface HelpCenterState {
  /** 是否显示帮助中心 */
  isOpen: boolean
  /** 当前选中的分类 */
  selectedCategory: HelpCategory | null
  /** 当前查看的文章 */
  currentArticle: HelpArticle | null
  /** 搜索关键词 */
  searchQuery: string
  /** 搜索结果 */
  searchResults: HelpSearchResult[]
  /** 是否正在搜索 */
  isSearching: boolean
  /** 浏览历史 */
  history: string[]
}

/**
 * 帮助服务接口
 */
export interface IHelpService {
  /** 获取所有分类 */
  getCategories(): HelpCategoryInfo[]
  /** 获取分类下的文章 */
  getArticlesByCategory(category: HelpCategory): HelpArticle[]
  /** 获取文章详情 */
  getArticle(articleId: string): HelpArticle | undefined
  /** 搜索文章 */
  search(query: string): HelpSearchResult[]
  /** 获取热门文章 */
  getPopularArticles(): HelpArticle[]
  /** 获取上下文相关的帮助文章 */
  getContextualHelp(feature: string): HelpArticle[]
  /** 注册文章 */
  registerArticle(article: HelpArticle): void
  /** 批量注册文章 */
  registerArticles(articles: HelpArticle[]): void
}

/**
 * 帮助中心配置
 */
export interface HelpCenterConfig {
  /** 默认显示的分类 */
  defaultCategory?: HelpCategory
  /** 搜索防抖延迟（毫秒） */
  searchDebounce?: number
  /** 最大搜索结果数 */
  maxSearchResults?: number
  /** 是否显示热门文章 */
  showPopular?: boolean
  /** 是否启用上下文帮助 */
  enableContextualHelp?: boolean
}

/**
 * 帮助中心事件
 */
export interface HelpCenterEvents {
  /** 打开帮助中心 */
  onOpen?: () => void
  /** 关闭帮助中心 */
  onClose?: () => void
  /** 查看文章 */
  onArticleView?: (article: HelpArticle) => void
  /** 搜索 */
  onSearch?: (query: string, results: HelpSearchResult[]) => void
}


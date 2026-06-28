/**
 * 功能引导系统模块导出
 * Feature Guide System Module Exports
 * 
 * 需求 19.1: WHEN 用户首次使用某功能时，THE DataTeaCup SHALL 显示功能引导教程
 * 需求 19.2: THE DataTeaCup SHALL 为复杂功能提供上下文相关的使用提示
 * 需求 19.3: THE DataTeaCup SHALL 提供内置的帮助文档系统
 * 需求 19.4: THE DataTeaCup SHALL 显示当前页面可用的快捷键列表
 */

// 类型导出
export * from './types'
export * from './featureTipTypes'
export * from './helpTypes'
export * from './shortcutHintsTypes'

// 组件导出
export { default as GuideOverlay } from './GuideOverlay.vue'
export { default as FeatureTip } from './FeatureTip.vue'
export { default as HelpCenter } from './HelpCenter.vue'
export { default as ShortcutHints } from './ShortcutHints.vue'

// 引导服务导出
export {
  // 服务类
  GuideService,
  // 工厂函数
  getGuideService,
  createGuideService,
  // Composable
  useGuide,
  // 类型
  type GuideConfig,
  type GuideState,
  type GuideStep,
  type IGuideService
} from './GuideService'

// 功能提示服务导出
export {
  // 服务类
  FeatureTipService,
  // 工厂函数
  getFeatureTipService,
  createFeatureTipService,
  // Composable
  useFeatureTip,
  // 类型
  type FeatureTipConfig,
  type FeatureTipState,
  type IFeatureTipService,
  type UseFeatureTipReturn
} from './FeatureTipService'

// 预设引导配置导出
export {
  PRESET_GUIDES,
  REPORT_DESIGNER_GUIDE,
  CHART_DESIGNER_GUIDE,
  DATASOURCE_GUIDE,
  QUERY_BUILDER_GUIDE,
  getGuideByFeature,
  getGuideById
} from './presetGuides'

// 预设功能提示配置导出
export {
  PRESET_FEATURE_TIPS,
  SQL_EDITOR_TIPS,
  REPORT_DESIGNER_TIPS,
  CHART_DESIGNER_TIPS,
  DATA_MASKING_TIPS,
  QUERY_BUILDER_TIPS,
  PARAMETER_TIPS,
  EXPORT_TIPS,
  CACHE_TIPS,
  STYLE_ENGINE_TIPS,
  getTipsByFeature,
  getTipById,
  getAllFeatures
} from './presetFeatureTips'

// 帮助服务导出
export {
  // 服务类
  HelpService,
  // 工厂函数
  getHelpService,
  createHelpService,
  // Composable
  useHelp,
  // 常量
  HELP_CATEGORIES,
  // 类型
  type HelpArticle,
  type HelpCategory,
  type HelpCategoryInfo,
  type HelpSearchResult,
  type HelpCenterState
} from './HelpService'

// 预设帮助文章导出
export {
  PRESET_HELP_ARTICLES,
  GETTING_STARTED_ARTICLES,
  REPORT_DESIGN_ARTICLES,
  CHART_DESIGN_ARTICLES,
  DATA_SOURCE_ARTICLES,
  DATA_MASKING_ARTICLES,
  QUERY_BUILDER_ARTICLES,
  EXPORT_ARTICLES,
  PERFORMANCE_ARTICLES,
  TROUBLESHOOTING_ARTICLES,
  FAQ_ARTICLES,
  getArticlesByCategory,
  getArticleById,
  getPopularArticles,
  getArticlesByFeature
} from './presetHelpArticles'


// 快捷键提示服务导出
export {
  // 服务类
  ShortcutHintsService,
  // 工厂函数
  getShortcutHintsService,
  createShortcutHintsService,
  // Composable
  useShortcutHints,
  // 常量
  SHORTCUT_CATEGORIES,
  // 类型
  type UseShortcutHintsReturn
} from './useShortcutHints'

// 预设快捷键导出
export {
  PRESET_SHORTCUTS
} from './ShortcutHintsService'


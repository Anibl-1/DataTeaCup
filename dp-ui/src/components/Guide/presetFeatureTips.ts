/**
 * 预设功能提示配置
 * Preset Feature Tip Configurations
 * 
 * 为平台复杂功能提供预设的上下文提示配置。
 * 
 * 需求 19.2: THE DataTeaCup SHALL 为复杂功能提供上下文相关的使用提示
 */

import type { FeatureTipConfig } from './featureTipTypes'

// ==================== SQL 编辑器相关提示 ====================

export const SQL_EDITOR_TIPS: FeatureTipConfig[] = [
  {
    id: 'sql-editor-autocomplete',
    title: 'SQL 自动补全',
    content: '输入表名或字段名时，按 Ctrl+Space 触发自动补全建议。支持表名、字段名和 SQL 关键字补全。',
    type: 'tip',
    dismissible: true,
    feature: 'sql-editor',
    example: 'SELECT * FROM us... → users'
  },
  {
    id: 'sql-editor-format',
    title: 'SQL 格式化',
    content: '按 Ctrl+Shift+F 可以自动格式化 SQL 语句，使代码更易读。',
    type: 'tip',
    dismissible: true,
    feature: 'sql-editor',
    example: 'SELECT * FROM users WHERE id=1 → 格式化后的多行 SQL'
  },
  {
    id: 'sql-editor-preview',
    title: '实时预览',
    content: '编辑 SQL 后，系统会在 500ms 内自动预览查询结果（限制前 10 条数据）。',
    type: 'info',
    dismissible: true,
    feature: 'sql-editor'
  }
]

// ==================== 报表设计器相关提示 ====================

export const REPORT_DESIGNER_TIPS: FeatureTipConfig[] = [
  {
    id: 'report-field-drag',
    title: '字段拖拽排序',
    content: '可以通过拖拽字段来调整显示顺序，拖拽时会显示放置位置指示器。',
    type: 'tip',
    dismissible: true,
    feature: 'report-designer'
  },
  {
    id: 'report-undo-redo',
    title: '撤销/重做',
    content: '支持 Ctrl+Z 撤销和 Ctrl+Y 重做操作，最多保留 50 步历史记录。',
    type: 'info',
    dismissible: true,
    feature: 'report-designer',
    example: 'Ctrl+Z 撤销 | Ctrl+Y 重做'
  },
  {
    id: 'report-template',
    title: '使用模板',
    content: '可以选择预设模板快速创建报表，模板会自动填充 SQL 和字段配置。',
    type: 'tip',
    dismissible: true,
    feature: 'report-designer'
  },
  {
    id: 'report-conditional-format',
    title: '条件格式化',
    content: '支持基于数值范围设置单元格样式，如负数显示红色、正数显示绿色。',
    type: 'info',
    dismissible: true,
    feature: 'report-designer'
  }
]

// ==================== 图表设计器相关提示 ====================

export const CHART_DESIGNER_TIPS: FeatureTipConfig[] = [
  {
    id: 'chart-theme-switch',
    title: '主题切换',
    content: '提供 6 套配色主题（默认、商务、科技、清新、暗黑、自定义），切换后图表会在 200ms 内更新。',
    type: 'tip',
    dismissible: true,
    feature: 'chart-designer'
  },
  {
    id: 'chart-shortcuts',
    title: '快捷键',
    content: '支持常用快捷键：Ctrl+S 保存、Ctrl+P 预览、Ctrl+E 导出。',
    type: 'info',
    dismissible: true,
    feature: 'chart-designer',
    example: 'Ctrl+S 保存 | Ctrl+P 预览 | Ctrl+E 导出'
  },
  {
    id: 'chart-realtime-preview',
    title: '实时预览',
    content: '修改配置后，图表会在 300ms 内自动更新预览，所见即所得。',
    type: 'info',
    dismissible: true,
    feature: 'chart-designer'
  }
]

// ==================== 数据脱敏相关提示 ====================

export const DATA_MASKING_TIPS: FeatureTipConfig[] = [
  {
    id: 'masking-auto-detect',
    title: '自动识别',
    content: '系统会自动识别敏感字段类型（手机号、身份证、银行卡等），并推荐合适的脱敏策略。',
    type: 'info',
    dismissible: true,
    feature: 'data-masking'
  },
  {
    id: 'masking-preview',
    title: '脱敏预览',
    content: '配置脱敏规则时可以实时预览脱敏效果，确保配置正确。',
    type: 'tip',
    dismissible: true,
    feature: 'data-masking',
    example: '138****8888 | 张* | a1b2c3...'
  },
  {
    id: 'masking-role-based',
    title: '角色级脱敏',
    content: '可以为不同角色配置不同的脱敏级别，实现精细化的数据权限控制。',
    type: 'warning',
    dismissible: true,
    feature: 'data-masking'
  }
]

// ==================== 查询构建器相关提示 ====================

export const QUERY_BUILDER_TIPS: FeatureTipConfig[] = [
  {
    id: 'query-table-drag',
    title: '拖拽选表',
    content: '从左侧表列表拖拽表到画布上，系统会自动推荐表之间的关联关系。',
    type: 'tip',
    dismissible: true,
    feature: 'query-builder'
  },
  {
    id: 'query-join-recommend',
    title: '关联推荐',
    content: '系统会基于外键和字段名匹配自动推荐表关联，可以手动调整关联条件。',
    type: 'info',
    dismissible: true,
    feature: 'query-builder'
  },
  {
    id: 'query-condition-builder',
    title: '条件构建器',
    content: '使用可视化条件构建器添加 WHERE 条件，支持多条件组合和嵌套。',
    type: 'tip',
    dismissible: true,
    feature: 'query-builder'
  }
]

// ==================== 参数配置相关提示 ====================

export const PARAMETER_TIPS: FeatureTipConfig[] = [
  {
    id: 'param-cascade',
    title: '参数级联',
    content: '支持参数之间的级联选择，如选择省份后自动筛选城市列表。',
    type: 'info',
    dismissible: true,
    feature: 'parameter-config'
  },
  {
    id: 'param-dynamic',
    title: '动态数据源',
    content: '参数选项可以从数据库动态获取，保持数据实时性。',
    type: 'tip',
    dismissible: true,
    feature: 'parameter-config'
  },
  {
    id: 'param-default-recommend',
    title: '默认值推荐',
    content: '系统会基于您的历史使用记录智能推荐参数默认值。',
    type: 'info',
    dismissible: true,
    feature: 'parameter-config'
  }
]

// ==================== 导出相关提示 ====================

export const EXPORT_TIPS: FeatureTipConfig[] = [
  {
    id: 'export-large-data',
    title: '大数据量导出',
    content: '导出超过 50000 行数据时，系统会创建后台异步任务处理，完成后会通知您。',
    type: 'warning',
    dismissible: true,
    feature: 'export'
  },
  {
    id: 'export-resume',
    title: '断点续传',
    content: '导出任务支持断点续传，如果导出中断可以从上次位置继续。',
    type: 'info',
    dismissible: true,
    feature: 'export'
  },
  {
    id: 'export-compress',
    title: '自动压缩',
    content: '导出文件超过 10MB 时会自动压缩为 ZIP 格式，减少下载时间。',
    type: 'info',
    dismissible: true,
    feature: 'export'
  }
]

// ==================== 缓存相关提示 ====================

export const CACHE_TIPS: FeatureTipConfig[] = [
  {
    id: 'cache-hit-rate',
    title: '缓存命中率',
    content: '当缓存命中率低于 70% 时，系统会触发告警。可以在监控面板查看详细统计。',
    type: 'warning',
    dismissible: true,
    feature: 'cache-monitor'
  },
  {
    id: 'cache-invalidate',
    title: '缓存失效',
    content: '数据源数据变更时，相关缓存会自动精准失效，确保数据一致性。',
    type: 'info',
    dismissible: true,
    feature: 'cache-monitor'
  }
]

// ==================== 样式引擎相关提示 ====================

export const STYLE_ENGINE_TIPS: FeatureTipConfig[] = [
  {
    id: 'style-inheritance',
    title: '样式继承',
    content: '样式按优先级继承：全局样式 → 列样式 → 行样式 → 单元格样式。高优先级样式会覆盖低优先级。',
    type: 'info',
    dismissible: true,
    feature: 'style-engine'
  },
  {
    id: 'style-template',
    title: '样式模板',
    content: '提供财务报表、销售报表、库存报表等预设样式模板，可以一键应用。',
    type: 'tip',
    dismissible: true,
    feature: 'style-engine'
  },
  {
    id: 'style-conditional',
    title: '条件样式',
    content: '支持基于数值比较、文本匹配、跨字段比较等条件动态应用样式。',
    type: 'info',
    dismissible: true,
    feature: 'style-engine'
  }
]

// ==================== 所有预设提示 ====================

export const PRESET_FEATURE_TIPS: FeatureTipConfig[] = [
  ...SQL_EDITOR_TIPS,
  ...REPORT_DESIGNER_TIPS,
  ...CHART_DESIGNER_TIPS,
  ...DATA_MASKING_TIPS,
  ...QUERY_BUILDER_TIPS,
  ...PARAMETER_TIPS,
  ...EXPORT_TIPS,
  ...CACHE_TIPS,
  ...STYLE_ENGINE_TIPS
]

// ==================== 工具函数 ====================

/**
 * 根据功能名称获取提示配置列表
 */
export function getTipsByFeature(feature: string): FeatureTipConfig[] {
  return PRESET_FEATURE_TIPS.filter(tip => tip.feature === feature)
}

/**
 * 根据提示 ID 获取提示配置
 */
export function getTipById(id: string): FeatureTipConfig | undefined {
  return PRESET_FEATURE_TIPS.find(tip => tip.id === id)
}

/**
 * 获取所有功能名称
 */
export function getAllFeatures(): string[] {
  const features = new Set<string>()
  PRESET_FEATURE_TIPS.forEach(tip => {
    if (tip.feature) {
      features.add(tip.feature)
    }
  })
  return Array.from(features)
}


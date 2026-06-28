/**
 * 预设帮助文章
 * Preset Help Articles
 * 
 * 需求 19.3: THE DataTeaCup SHALL 提供内置的帮助文档系统
 * 
 * @module presetHelpArticles
 */

import type { HelpArticle } from './helpTypes'

// ==================== 入门指南 ====================

export const GETTING_STARTED_ARTICLES: HelpArticle[] = [
  {
    id: 'gs-overview',
    title: '平台概述',
    summary: '了解 DataTeaCup 的核心功能和使用场景',
    category: 'getting-started',
    keywords: ['概述', '介绍', '功能', 'overview', 'introduction'],
    relatedFeatures: ['/dashboard', '/'],
    order: 1,
    isPopular: true,
    content: `
# 平台概述

DataTeaCup 是一个功能强大的数据分析平台，提供报表设计、图表可视化、数据查询等核心功能。

## 核心功能

### 报表设计
- 可视化报表设计器
- 丰富的字段配置选项
- 支持多种数据格式化

### 图表设计
- 多种图表类型支持
- 实时预览功能
- 主题配色切换

### 数据查询
- 可视化查询构建器
- SQL 编辑器
- 查询结果预览

## 快速开始

1. 连接数据源
2. 创建报表或图表
3. 配置数据展示
4. 发布和分享
    `
  },
  {
    id: 'gs-quick-start',
    title: '快速入门',
    summary: '5分钟快速上手平台基本操作',
    category: 'getting-started',
    keywords: ['快速', '入门', '开始', 'quick start', 'tutorial'],
    relatedFeatures: ['/dashboard'],
    order: 2,
    isPopular: true,
    content: `
# 快速入门

本指南将帮助您在5分钟内完成第一个报表的创建。

## 步骤一：登录系统

使用您的账号密码登录系统，首次登录后建议修改默认密码。

## 步骤二：连接数据源

1. 进入「数据源管理」页面
2. 点击「新建数据源」
3. 选择数据库类型并填写连接信息
4. 测试连接并保存

## 步骤三：创建报表

1. 进入「报表设计器」
2. 选择数据源
3. 编写或构建 SQL 查询
4. 配置字段显示
5. 保存报表

## 步骤四：预览和发布

1. 点击「预览」查看报表效果
2. 调整样式和格式
3. 点击「发布」使报表生效
    `
  }
]


// ==================== 报表设计 ====================

export const REPORT_DESIGN_ARTICLES: HelpArticle[] = [
  {
    id: 'rd-basics',
    title: '报表设计基础',
    summary: '学习报表设计器的基本操作和功能',
    category: 'report-design',
    keywords: ['报表', '设计', '基础', 'report', 'design', 'basics'],
    relatedFeatures: ['/report-designer', '/reports'],
    order: 1,
    isPopular: true,
    content: `
# 报表设计基础

报表设计器是创建数据报表的核心工具，提供直观的可视化配置界面。

## 界面布局

- **左侧面板**：数据源和字段选择
- **中间区域**：SQL 编辑器和预览
- **右侧面板**：字段配置和样式设置

## 基本操作

### 选择数据源
从下拉列表中选择已配置的数据源。

### 编写 SQL
在 SQL 编辑器中编写查询语句，支持自动补全。

### 配置字段
拖拽调整字段顺序，设置显示名称和格式。

## 快捷键

- \`Ctrl+S\`：保存报表
- \`Ctrl+Z\`：撤销操作
- \`Ctrl+Y\`：重做操作
    `
  },
  {
    id: 'rd-sql-editor',
    title: 'SQL 编辑器使用',
    summary: '掌握 SQL 编辑器的高级功能',
    category: 'report-design',
    keywords: ['SQL', '编辑器', '查询', 'editor', 'query'],
    relatedFeatures: ['/report-designer'],
    order: 2,
    content: `
# SQL 编辑器使用

SQL 编辑器提供智能的代码编辑体验。

## 自动补全

输入时会自动提示：
- 表名
- 字段名
- SQL 关键字

## 语法高亮

不同类型的 SQL 元素会以不同颜色显示：
- 关键字：蓝色
- 字符串：绿色
- 数字：橙色

## 实时预览

编辑 SQL 后，系统会在 500ms 内显示查询结果预览（限制前10条）。

## 格式化

点击格式化按钮可自动整理 SQL 语句格式。
    `
  },
  {
    id: 'rd-field-config',
    title: '字段配置详解',
    summary: '深入了解字段的各种配置选项',
    category: 'report-design',
    keywords: ['字段', '配置', '格式', 'field', 'config', 'format'],
    relatedFeatures: ['/report-designer'],
    order: 3,
    content: `
# 字段配置详解

字段配置决定了数据在报表中的展示方式。

## 基本配置

- **显示名称**：字段在表头显示的名称
- **字段类型**：文本、数值、日期等
- **对齐方式**：左对齐、居中、右对齐

## 格式化配置

### 数值格式
- 小数位数
- 千分位分隔符
- 百分比显示
- 货币符号

### 日期格式
- 预设格式（YYYY-MM-DD 等）
- 自定义格式

## 条件格式化

根据数值范围设置不同的显示样式：
- 负数红色显示
- 正数绿色显示
- 区间色阶
    `
  }
]

// ==================== 图表设计 ====================

export const CHART_DESIGN_ARTICLES: HelpArticle[] = [
  {
    id: 'cd-basics',
    title: '图表设计基础',
    summary: '学习如何创建和配置数据可视化图表',
    category: 'chart-design',
    keywords: ['图表', '设计', '可视化', 'chart', 'visualization'],
    relatedFeatures: ['/chart-designer', '/charts'],
    order: 1,
    isPopular: true,
    content: `
# 图表设计基础

图表设计器帮助您创建直观的数据可视化图表。

## 支持的图表类型

- 柱状图
- 折线图
- 饼图
- 散点图
- 雷达图
- 热力图

## 创建图表

1. 选择数据源
2. 配置数据维度和指标
3. 选择图表类型
4. 调整样式配置

## 实时预览

修改配置后，图表会在 300ms 内更新预览。
    `
  },
  {
    id: 'cd-themes',
    title: '图表主题配置',
    summary: '使用预设主题或自定义图表配色',
    category: 'chart-design',
    keywords: ['主题', '配色', '样式', 'theme', 'color'],
    relatedFeatures: ['/chart-designer'],
    order: 2,
    content: `
# 图表主题配置

系统提供多套预设主题，也支持自定义配色。

## 预设主题

- **默认**：平衡的蓝色系配色
- **商务**：专业的深色系配色
- **科技**：现代的渐变配色
- **清新**：明亮的浅色系配色
- **暗黑**：深色背景配色
- **自定义**：完全自定义配色

## 切换主题

在图表设计器右侧面板选择主题，图表会在 200ms 内更新配色。

## 自定义配色

选择「自定义」主题后，可以设置：
- 主色调
- 辅助色
- 背景色
- 文字颜色
    `
  }
]


// ==================== 数据源管理 ====================

export const DATA_SOURCE_ARTICLES: HelpArticle[] = [
  {
    id: 'ds-connect',
    title: '连接数据源',
    summary: '学习如何配置和连接各种数据库',
    category: 'data-source',
    keywords: ['数据源', '连接', '数据库', 'datasource', 'database', 'connect'],
    relatedFeatures: ['/datasource', '/datasources'],
    order: 1,
    isPopular: true,
    content: `
# 连接数据源

数据源是报表和图表的数据来源，支持多种数据库类型。

## 支持的数据库

- MySQL
- PostgreSQL
- Oracle
- SQL Server
- 更多...

## 配置步骤

1. 进入数据源管理页面
2. 点击「新建数据源」
3. 选择数据库类型
4. 填写连接信息：
   - 主机地址
   - 端口号
   - 数据库名
   - 用户名
   - 密码
5. 点击「测试连接」
6. 保存配置

## 连接池配置

高级用户可以配置连接池参数：
- 最大连接数
- 最小空闲连接
- 连接超时时间
    `
  }
]

// ==================== 数据脱敏 ====================

export const DATA_MASKING_ARTICLES: HelpArticle[] = [
  {
    id: 'dm-overview',
    title: '数据脱敏概述',
    summary: '了解数据脱敏的作用和配置方法',
    category: 'data-masking',
    keywords: ['脱敏', '安全', '隐私', 'masking', 'security', 'privacy'],
    relatedFeatures: ['/masking', '/settings/masking'],
    order: 1,
    content: `
# 数据脱敏概述

数据脱敏用于保护敏感数据，防止数据泄露。

## 支持的敏感数据类型

- 手机号
- 身份证号
- 银行卡号
- 邮箱地址
- 姓名
- 地址
- IP 地址

## 脱敏策略

- **掩码**：138****8888
- **截断**：张*
- **哈希**：不可逆加密
- **替换**：固定值替换
- **区间化**：数值转区间

## 配置方式

1. 系统自动识别敏感字段
2. 手动配置脱敏规则
3. 按角色设置脱敏级别
    `
  }
]

// ==================== 查询构建 ====================

export const QUERY_BUILDER_ARTICLES: HelpArticle[] = [
  {
    id: 'qb-visual',
    title: '可视化查询构建',
    summary: '使用拖拽方式构建 SQL 查询',
    category: 'query-builder',
    keywords: ['查询', '构建', '可视化', 'query', 'builder', 'visual'],
    relatedFeatures: ['/query-builder', '/report-designer'],
    order: 1,
    content: `
# 可视化查询构建

无需编写 SQL，通过拖拽操作构建数据查询。

## 功能特点

- 拖拽选择表和字段
- 自动推荐表关联
- 可视化条件配置
- 聚合函数向导

## 使用步骤

1. 选择数据表
2. 拖拽需要的字段
3. 配置筛选条件
4. 设置排序和分组
5. 预览生成的 SQL

## 表关联

系统会自动推荐表之间的关联关系，也可以手动配置。
    `
  }
]

// ==================== 导出功能 ====================

export const EXPORT_ARTICLES: HelpArticle[] = [
  {
    id: 'ex-formats',
    title: '导出格式说明',
    summary: '了解支持的导出格式和使用场景',
    category: 'export',
    keywords: ['导出', '格式', 'Excel', 'PDF', 'export', 'format'],
    relatedFeatures: ['/reports', '/charts'],
    order: 1,
    content: `
# 导出格式说明

支持多种格式导出报表和图表数据。

## 支持的格式

### Excel (.xlsx)
- 保留数据格式
- 支持条件格式化
- 适合数据分析

### CSV
- 纯文本格式
- 通用兼容性
- 适合数据交换

### PDF
- 固定布局
- 适合打印和分享

## 大数据导出

- 超过 10000 行：流式处理
- 超过 50000 行：异步任务
- 超过 10MB：自动压缩

## 导出进度

大数据导出时会显示进度百分比。
    `
  }
]


// ==================== 性能优化 ====================

export const PERFORMANCE_ARTICLES: HelpArticle[] = [
  {
    id: 'pf-cache',
    title: '缓存机制说明',
    summary: '了解系统的多级缓存机制',
    category: 'performance',
    keywords: ['缓存', '性能', '优化', 'cache', 'performance'],
    relatedFeatures: ['/settings/cache'],
    order: 1,
    content: `
# 缓存机制说明

系统采用多级缓存架构提升查询性能。

## 缓存层级

### L1 本地缓存
- 基于 Caffeine
- 最大 10000 条目
- 5 分钟 TTL

### L2 分布式缓存
- 基于 Redis
- 30 分钟 TTL
- 支持集群模式

## 缓存策略

- 热点数据自动识别
- 智能缓存预热
- 精准缓存失效
- 防穿透保护

## 监控指标

- L1/L2 命中率
- 内存占用
- 慢查询关联
    `
  }
]

// ==================== 故障排除 ====================

export const TROUBLESHOOTING_ARTICLES: HelpArticle[] = [
  {
    id: 'ts-common',
    title: '常见问题排查',
    summary: '解决使用过程中的常见问题',
    category: 'troubleshooting',
    keywords: ['问题', '排查', '错误', 'troubleshoot', 'error', 'issue'],
    order: 1,
    isPopular: true,
    content: `
# 常见问题排查

本文档帮助您解决使用过程中的常见问题。

## 连接问题

### 数据源连接失败
- 检查网络连接
- 验证连接信息
- 确认防火墙设置

### 查询超时
- 优化 SQL 语句
- 添加适当索引
- 减少数据量

## 显示问题

### 数据不显示
- 检查 SQL 语法
- 确认字段配置
- 查看错误日志

### 样式异常
- 清除浏览器缓存
- 检查条件格式配置

## 性能问题

### 加载缓慢
- 使用分页查询
- 启用缓存
- 优化查询语句
    `
  }
]

// ==================== 常见问题 ====================

export const FAQ_ARTICLES: HelpArticle[] = [
  {
    id: 'faq-general',
    title: '常见问题解答',
    summary: '用户最常问的问题和解答',
    category: 'faq',
    keywords: ['FAQ', '问题', '解答', 'question', 'answer'],
    order: 1,
    isPopular: true,
    content: `
# 常见问题解答

## 账号相关

**Q: 如何修改密码？**
A: 进入个人设置页面，点击「修改密码」。

**Q: 忘记密码怎么办？**
A: 联系管理员重置密码。

## 报表相关

**Q: 报表最多支持多少数据？**
A: 单次查询建议不超过 10 万行，大数据量请使用分页。

**Q: 如何分享报表？**
A: 发布报表后，可以通过链接分享或导出。

## 权限相关

**Q: 为什么看不到某些数据？**
A: 可能是数据脱敏或权限限制，请联系管理员。

**Q: 如何申请更多权限？**
A: 联系系统管理员申请相应角色权限。
    `
  }
]

// ==================== 汇总导出 ====================

/**
 * 所有预设帮助文章
 */
export const PRESET_HELP_ARTICLES: HelpArticle[] = [
  ...GETTING_STARTED_ARTICLES,
  ...REPORT_DESIGN_ARTICLES,
  ...CHART_DESIGN_ARTICLES,
  ...DATA_SOURCE_ARTICLES,
  ...DATA_MASKING_ARTICLES,
  ...QUERY_BUILDER_ARTICLES,
  ...EXPORT_ARTICLES,
  ...PERFORMANCE_ARTICLES,
  ...TROUBLESHOOTING_ARTICLES,
  ...FAQ_ARTICLES
]

/**
 * 根据分类获取文章
 */
export function getArticlesByCategory(category: string): HelpArticle[] {
  return PRESET_HELP_ARTICLES.filter(article => article.category === category)
}

/**
 * 根据 ID 获取文章
 */
export function getArticleById(id: string): HelpArticle | undefined {
  return PRESET_HELP_ARTICLES.find(article => article.id === id)
}

/**
 * 获取热门文章
 */
export function getPopularArticles(): HelpArticle[] {
  return PRESET_HELP_ARTICLES.filter(article => article.isPopular)
}

/**
 * 根据功能路径获取相关文章
 */
export function getArticlesByFeature(feature: string): HelpArticle[] {
  return PRESET_HELP_ARTICLES.filter(
    article => article.relatedFeatures?.includes(feature)
  )
}


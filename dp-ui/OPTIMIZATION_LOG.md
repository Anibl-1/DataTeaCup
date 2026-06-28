# 前端优化日志 (Frontend Optimization Log)

## 1. Console.log 清理

- **方案**: 在 `vite.config.ts` 中配置 esbuild `drop: ['console', 'debugger']`，生产构建自动剥离所有 `console.*` 和 `debugger` 语句
- **影响**: 零运行时开销，无需手动删除

## 2. 废弃代码与冗余依赖清理

- 移除未使用的组件、工具函数和冗余依赖
- 清理无效导入和死代码路径

## 3. 布局与样式深度优化

### 3.1 深色模式修复
- **关键Bug**: `<style scoped>` 中的 `:global(html.dark)` 选择器被 Vite/Vue 编译器静默剥离，影响 82 个文件
- **修复**: 将深色模式 CSS 移至独立的 `src/styles/dark-mode.css`（最后导入确保最高优先级）或组件的非 scoped `<style>` 块
- **已修复文件**: `TabsNav.vue`, `MainLayout.vue`, 以及全局覆盖

### 3.2 CSS 变量化
将以下页面的硬编码颜色替换为 CSS 变量：

| 文件 | 替换内容 |
|------|---------|
| `DbConnectionPanel.vue` | `background: white` → `var(--bg-primary)`, `border: #e6e6e6` → `var(--border-light)` |
| `DbTableBrowser.vue` | `background: #f8f9fa` → `var(--bg-secondary)` |
| `DbQueryResult.vue` | `background: #f8f9fa` → `var(--bg-secondary)` |
| `PipelineMonitor.vue` | 4 处 `#fff` → CSS 变量 |
| `PipelineManage.vue` | stat-card/pipeline-card 背景+边框+阴影变量化 |
| `SystemMonitor.vue` | page-header `white` → `var(--bg-primary)` |

### 3.3 全局深色模式覆盖 (`style.css`)
- 数据库管理器: sidebar, connection-panel, tree-item 等 20+ 选择器
- Pipeline 组件: header, monitor-header
- SystemMonitor: metric-card

### 3.4 商业风格增强 (`commercial.css`)
- `:focus-visible` 键盘焦点可见性 (WCAG 无障碍)
- `.page-container-base` 统一页面容器 + 入场动画
- 全局链接悬停效果、表格空状态最小高度

### 3.5 深色模式容器覆盖 (`dark-mode.css`)
- 新增 `[class$="-container"]` 通配符选择器

## 4. 图表中心页面管理 (layoutMode 功能)

### 4.1 类型定义 (`types/page.ts`)
- 新增 `PageLayoutMode = 'desktop' | 'mobile' | 'bigscreen'`
- 新增 `BigscreenConfig` 接口及 `createDefaultBigscreenConfig()` 工厂函数
- `PageDefinition` 扩展 `layoutMode`, `bigscreenConfig`, `mobileLayoutConfig` 字段

### 4.2 API (`api/page.ts`)
- `getPageDefinitionList` 支持 `layoutMode` 过滤参数

### 4.3 页面管理 (`PageManage.vue`)
- 三端 Tab 切换 (桌面端/移动端/大屏)
- 布局模式列、筛选、路由分发
- 大屏页面路由到 `BigscreenView`

### 4.4 大屏预览 (`BigscreenView.vue`)
- 全屏自适应缩放渲染
- 支持主题配置、参数面板、ECharts 图表
- 路由配置 (`/bigscreen-view/:id`)

### 4.5 页面设计器 (`PageDesigner.vue`)
- 新建页面支持选择 layoutMode
- 保存时携带 layoutMode 和 bigscreenConfig
- 预览按钮根据 layoutMode 路由到对应页面

## 5. 代码质量优化

### 5.1 TypeScript 类型修复
| 文件 | 修复内容 |
|------|---------|
| `types/menu.ts` | `MenuForm` 接口: Naive UI 绑定字段改为 required + `\| null`，兼容 `exactOptionalPropertyTypes` |
| `PageManage.vue` | `mobilePublishItems` 类型守卫过滤 undefined id; `row.id!` 非空断言 |
| `PageDesigner.vue` | `leftPanelTab` 类型补充 `'templates'`; `setChartPreviewRef` el 类型转换; `pageTemplates` 类型扩展; regex match 非空断言; `pageData` 类型宽松化 |

### 5.2 未使用导入清理 (`PageDesigner.vue`)
移除: `NScrollbar`, `SearchOutline`, `getChartTypeColor`, `createPageFromTemplate`, `SparklesOutline`

### 5.3 未使用变量标记 (`PageDesigner.vue`)
15 个 legacy 变量/函数添加 `_` 前缀标记，保留代码供未来参考

### 5.4 构建验证
- `npx vite build` ✅ 成功，无编译错误
- 产物包含 365 个 precache 条目

## 样式文件导入顺序 (`main.ts`)

```
style.css → design-tokens.css + page-common.css + mobile.css
high-contrast.css
commercial.css
dark-mode.css  ← 最后导入，确保最高优先级
```

## 已知限制

1. **`exactOptionalPropertyTypes`**: 项目启用了此严格 TS 选项，导致 `?: T` 类型的字段不能赋值 `undefined`。部分 Naive UI 组件 props 需要 `?? fallback` 处理
2. **`:global(html.dark)` 在 scoped 中无效**: 82 个文件仍使用此模式，需逐步迁移到 `dark-mode.css` 或非 scoped 块
3. **大包体积警告**: `naive-ui` (1.1MB), `echarts` (850KB), `SqlEditor` (433KB) 建议使用 `manualChunks` 优化

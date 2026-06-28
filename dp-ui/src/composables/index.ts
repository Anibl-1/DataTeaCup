/**
 * Composables 统一导出
 */

// 响应式布局
export { useResponsive, BREAKPOINTS, getBreakpointFromWidth } from './useResponsive'
export type { Breakpoint, ResponsiveState, UseResponsiveReturn } from './useResponsive'

// 无障碍支持
export {
  useAccessibility,
  useFocusTrap,
  useKeyboardNavigation,
  useAnnounce,
  useReducedMotion,
  generateAriaId,
  resetAriaIdCounter,
  getFocusableElements,
  isFocusable,
  createButtonAria,
  createMenuItemAria,
  createTabAria,
  createFieldAria,
  createProgressAria,
  createDialogAria
} from './useAccessibility'
export type {
  FocusTrapOptions,
  UseFocusTrapReturn,
  KeyboardNavigationOptions,
  UseKeyboardNavigationReturn,
  AnnounceOptions,
  UseAnnounceReturn,
  UseReducedMotionReturn,
  AriaAttributes,
  UseAccessibilityReturn
} from './useAccessibility'

// 请求
export { useRequest, usePagination, useDebouncedRequest } from './useRequest'

// 表格
export { useTable, createColumns } from './useTable'

// 增强版数据表格
export { useDataTable } from './useDataTable'
export type {
  UseDataTableOptions,
  UseDataTableReturn,
  DataTableQueryParams
} from './useDataTable'

// 表单弹窗
export { useFormModal } from './useFormModal'
export type {
  UseFormModalOptions,
  UseFormModalReturn
} from './useFormModal'

// CRUD 页面组合式函数
export { useCrudPage } from './useCrudPage'
export type {
  CrudPageOptions,
  CrudPageReturn
} from './useCrudPage'

// 导出
export { useExport } from './useExport'
export type {
  UseExportOptions,
  UseExportReturn,
  ExportCenterParams
} from './useExport'

// 数据库连接
export { useDbConnection, DB_TYPE_OPTIONS } from './useDbConnection'
export type { ConnectFormData } from './useDbConnection'

// 确认对话框
export { useConfirm } from './useConfirm'

// 加载状态
export { useLoading, useMultiLoading } from './useLoading'

// 仪表盘筛选器
export { useDashboardFilters } from './useDashboardFilters'
export type {
  FilterValue,
  FilterChangeEvent,
  CascadeFilterConfig,
  FilterState,
  UseDashboardFiltersOptions
} from './useDashboardFilters'

// 操作历史（撤销/重做）
export { useHistory } from './useHistory'
export type {
  HistoryEntry,
  UseHistoryOptions,
  UseHistoryReturn
} from './useHistory'

// 图表实时预览
export {
  useChartPreview,
  CONFIG_TOOLTIPS,
  getConfigTooltip,
  THEME_TRANSITION_CONFIG,
  PREVIEW_UPDATE_CONFIG
} from './useChartPreview'
export type {
  ChartPreviewConfig,
  ChartPreviewState,
  UseChartPreviewReturn
} from './useChartPreview'

// 快捷键管理
export {
  useShortcuts,
  parseShortcutKey,
  normalizeShortcutKey,
  matchShortcut,
  formatShortcutForDisplay,
  resetShortcutIdCounter,
  COMMON_SHORTCUTS
} from './useShortcuts'
export type {
  ShortcutModifiers,
  ShortcutDefinition,
  ParsedShortcut,
  RegisteredShortcut,
  ShortcutConflict,
  UseShortcutsOptions,
  UseShortcutsReturn
} from './useShortcuts'

// 参数级联选择
export {
  useParameterLinkage,
  createCascadeConfig,
  createParameterLinkage,
  resetLinkageIdCounter
} from './useParameterLinkage'
export type {
  ParameterOption,
  LinkageType,
  CascadeConfig,
  FilterConfig,
  ComputeConfig,
  ParameterLinkage,
  ParameterState,
  OptionsLoader,
  UseParameterLinkageOptions,
  UseParameterLinkageReturn
} from './useParameterLinkage'

// 动态参数选项加载器
export {
  createDynamicParameterLoader,
  createCachedDynamicParameterLoader,
  createMockDynamicParameterLoader,
  createCascadeConfigForDynamic,
  defaultDynamicParameterLoader
} from '@/services/dynamicParameterLoader'
export type {
  DynamicParameterLoaderConfig
} from '@/services/dynamicParameterLoader'

// 表格布局增强
export { useTableLayout } from './useTableLayout'
export type {
  UseTableLayoutOptions,
  UseTableLayoutReturn
} from './useTableLayout'

// 表格交互增强
export { useTableInteraction } from './useTableInteraction'
export type {
  UseTableInteractionOptions,
  UseTableInteractionReturn
} from './useTableInteraction'

// 虚拟滚动
export { useVirtualScroll } from './useVirtualScroll'
export type {
  VirtualScrollOptions,
  UseVirtualScrollReturn
} from './useVirtualScroll'


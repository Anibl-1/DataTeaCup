/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 表格布局增强类型定义
 * Enhanced Table Layout Type Definitions
 * 
 * 参考帆软FineReport报表布局、Excel冻结窗格、Ant Design Table
 * Reference: FineReport table layout, Excel freeze panes, Ant Design Table
 * 
 * 需求: 14.5.21, 14.5.22, 14.5.23, 14.5.24, 14.5.25, 14.5.26
 */

// ============================================================================
// Column Configuration Types (列配置)
// ============================================================================

/**
 * Column configuration
 * 列配置
 */
export interface ColumnConfig {
  /** Unique column key */
  key: string
  /** Column title/header */
  title: string
  /** Data field name */
  dataIndex: string
  /** Column width (px or 'auto') */
  width?: number | 'auto'
  /** Minimum column width */
  minWidth?: number
  /** Maximum column width */
  maxWidth?: number
  /** Whether column is fixed/frozen */
  fixed?: 'left' | 'right' | false
  /** Text alignment */
  align?: 'left' | 'center' | 'right'
  /** Whether column is sortable */
  sortable?: boolean
  /** Whether column is filterable */
  filterable?: boolean
  /** Whether column is resizable */
  resizable?: boolean
  /** Custom render function */
  render?: (value: any, row: Record<string, any>, rowIndex: number) => any
  /** Column class name */
  className?: string
  /** Header class name */
  headerClassName?: string
  /** Whether to enable auto width based on content */
  autoWidth?: boolean
  /** Ellipsis for overflow text */
  ellipsis?: boolean
}

// ============================================================================
// Merge Configuration Types (合并配置)
// ============================================================================

/**
 * Cell merge span info
 * 单元格合并跨度信息
 * 
 * Validates: 14.5.21 - 行合并 - 支持相同值自动合并
 * Validates: 14.5.22 - 列合并 - 支持跨列合并
 */
export interface CellMergeSpan {
  /** Number of rows to span (rowspan) */
  rowspan: number
  /** Number of columns to span (colspan) */
  colspan: number
}

/**
 * Merge rule for automatic merging
 * 自动合并规则
 */
export interface MergeRule {
  /** Column key to apply merge */
  columnKey: string
  /** Merge type */
  type: 'row' | 'column' | 'both'
  /** Whether to merge cells with same value */
  mergeOnSameValue?: boolean
  /** Custom merge condition function */
  condition?: (
    currentValue: any,
    previousValue: any,
    currentRow: Record<string, any>,
    previousRow: Record<string, any>
  ) => boolean
}

/**
 * Manual merge cell definition
 * 手动合并单元格定义
 */
export interface ManualMergeCell {
  /** Starting row index */
  rowIndex: number
  /** Starting column key */
  columnKey: string
  /** Number of rows to span */
  rowspan: number
  /** Number of columns to span */
  colspan: number
}

/**
 * Merge configuration
 * 合并配置
 */
export interface MergeConfig {
  /** Enable automatic row merge for same values */
  autoRowMerge?: boolean
  /** Columns to apply auto row merge */
  autoRowMergeColumns?: string[]
  /** Merge rules for automatic merging */
  rules?: MergeRule[]
  /** Manual merge cells */
  manualMerges?: ManualMergeCell[]
  /** Callback to get merge span for a cell */
  spanMethod?: (params: {
    row: Record<string, any>
    column: ColumnConfig
    rowIndex: number
    columnIndex: number
  }) => CellMergeSpan | undefined
}

// ============================================================================
// Freeze Configuration Types (冻结配置)
// ============================================================================

/**
 * Freeze pane configuration
 * 冻结窗格配置
 * 
 * Validates: 14.5.23 - 冻结表头 - 固定表头滚动
 * Validates: 14.5.24 - 冻结列 - 固定左侧列滚动
 */
export interface FreezeConfig {
  /** Whether to freeze header row(s) */
  freezeHeader?: boolean
  /** Number of header rows to freeze (default: 1) */
  freezeHeaderRows?: number
  /** Number of left columns to freeze */
  freezeLeftColumns?: number
  /** Number of right columns to freeze */
  freezeRightColumns?: number
  /** Column keys to freeze on left */
  freezeLeftColumnKeys?: string[]
  /** Column keys to freeze on right */
  freezeRightColumnKeys?: string[]
}

// ============================================================================
// Auto Size Configuration Types (自适应配置)
// ============================================================================

/**
 * Auto size configuration
 * 自适应配置
 * 
 * Validates: 14.5.25 - 列宽自适应 - 根据内容自动调整列宽
 */
export interface AutoSizeConfig {
  /** Enable auto column width */
  autoColumnWidth?: boolean
  /** Columns to apply auto width */
  autoWidthColumns?: string[]
  /** Include header text in width calculation */
  includeHeader?: boolean
  /** Padding to add to calculated width */
  padding?: number
  /** Maximum width limit for auto-sized columns */
  maxAutoWidth?: number
  /** Minimum width limit for auto-sized columns */
  minAutoWidth?: number
  /** Enable auto row height */
  autoRowHeight?: boolean
  /** Minimum row height */
  minRowHeight?: number
  /** Maximum row height */
  maxRowHeight?: number
}

// ============================================================================
// Zebra Stripe Configuration Types (斑马纹配置)
// ============================================================================

/**
 * Zebra stripe configuration
 * 斑马纹配置
 * 
 * Validates: 14.5.26 - 斑马纹 - 交替行背景色
 */
export interface ZebraStripeConfig {
  /** Enable zebra stripes */
  enabled: boolean
  /** Even row background color */
  evenRowColor?: string
  /** Odd row background color */
  oddRowColor?: string
  /** Whether to apply to header */
  applyToHeader?: boolean
  /** Custom stripe pattern (e.g., every 2 rows) */
  stripeInterval?: number
}

// ============================================================================
// Table Style Configuration Types (表格样式配置)
// ============================================================================

/**
 * Table border style
 * 表格边框样式
 */
export interface TableBorderStyle {
  /** Border style */
  style?: 'none' | 'solid' | 'dashed' | 'dotted'
  /** Border width */
  width?: number
  /** Border color */
  color?: string
}

/**
 * Table style configuration
 * 表格样式配置
 */
export interface TableStyleConfig {
  /** Border configuration */
  border?: {
    outer?: TableBorderStyle
    inner?: TableBorderStyle
    header?: TableBorderStyle
  }
  /** Header style */
  headerStyle?: {
    backgroundColor?: string
    color?: string
    fontWeight?: string | number
    height?: number
  }
  /** Body style */
  bodyStyle?: {
    backgroundColor?: string
    color?: string
    height?: number
  }
  /** Hover row style */
  hoverStyle?: {
    backgroundColor?: string
    color?: string
  }
  /** Selected row style */
  selectedStyle?: {
    backgroundColor?: string
    color?: string
  }
}

// ============================================================================
// Enhanced Table Props Types (增强表格属性)
// ============================================================================

/**
 * Enhanced table props
 * 增强表格属性
 */
export interface EnhancedTableProps {
  /** Table data */
  data: Record<string, any>[]
  /** Column configurations */
  columns: ColumnConfig[]
  /** Merge configuration */
  mergeConfig?: MergeConfig
  /** Freeze configuration */
  freezeConfig?: FreezeConfig
  /** Auto size configuration */
  autoSizeConfig?: AutoSizeConfig
  /** Zebra stripe configuration */
  zebraStripe?: boolean | ZebraStripeConfig
  /** Table style configuration */
  tableStyle?: TableStyleConfig
  /** Row key field */
  rowKey?: string | ((row: Record<string, any>) => string)
  /** Table height (enables virtual scroll if set) */
  height?: number | string
  /** Maximum table height */
  maxHeight?: number | string
  /** Whether to show loading state */
  loading?: boolean
  /** Empty state text */
  emptyText?: string
  /** Whether table is bordered */
  bordered?: boolean
  /** Table size */
  size?: 'small' | 'medium' | 'large'
}

// ============================================================================
// Computed Cell Info Types (计算单元格信息)
// ============================================================================

/**
 * Computed cell information
 * 计算后的单元格信息
 */
export interface ComputedCellInfo {
  /** Row index */
  rowIndex: number
  /** Column index */
  columnIndex: number
  /** Column key */
  columnKey: string
  /** Cell value */
  value: any
  /** Row data */
  rowData: Record<string, any>
  /** Rowspan value */
  rowspan: number
  /** Colspan value */
  colspan: number
  /** Whether cell is hidden (merged into another cell) */
  hidden: boolean
  /** Whether cell is frozen */
  frozen: boolean
  /** Frozen position */
  frozenPosition?: 'left' | 'right'
  /** Computed width */
  width?: number
  /** Computed height */
  height?: number
  /** Zebra stripe class */
  zebraClass?: string
}

/**
 * Computed column information
 * 计算后的列信息
 */
export interface ComputedColumnInfo extends ColumnConfig {
  /** Computed width */
  computedWidth: number
  /** Whether column is frozen */
  frozen: boolean
  /** Frozen position */
  frozenPosition?: 'left' | 'right'
  /** Left offset for frozen columns */
  frozenOffset?: number
}

// ============================================================================
// Table State Types (表格状态)
// ============================================================================

/**
 * Table scroll state
 * 表格滚动状态
 */
export interface TableScrollState {
  /** Horizontal scroll position */
  scrollLeft: number
  /** Vertical scroll position */
  scrollTop: number
  /** Whether scrolled to left edge */
  isScrolledLeft: boolean
  /** Whether scrolled to right edge */
  isScrolledRight: boolean
  /** Whether scrolled to top */
  isScrolledTop: boolean
  /** Whether scrolled to bottom */
  isScrolledBottom: boolean
}

/**
 * Table layout state
 * 表格布局状态
 */
export interface TableLayoutState {
  /** Computed columns with widths */
  columns: ComputedColumnInfo[]
  /** Computed cells with merge info */
  cells: Map<string, ComputedCellInfo>
  /** Total table width */
  totalWidth: number
  /** Total table height */
  totalHeight: number
  /** Left frozen columns width */
  leftFrozenWidth: number
  /** Right frozen columns width */
  rightFrozenWidth: number
  /** Scroll state */
  scrollState: TableScrollState
}

// ============================================================================
// Event Types (事件类型)
// ============================================================================

/**
 * Cell click event
 */
export interface CellClickEvent {
  row: Record<string, any>
  column: ColumnConfig
  rowIndex: number
  columnIndex: number
  value: any
  event: MouseEvent
}

/**
 * Column resize event
 */
export interface ColumnResizeEvent {
  column: ColumnConfig
  width: number
  oldWidth: number
}

/**
 * Scroll event
 */
export interface TableScrollEvent {
  scrollLeft: number
  scrollTop: number
  event: Event
}

// ============================================================================
// Table Interaction Types (表格交互类型)
// Validates: 14.6.28, 14.6.29, 14.6.30, 14.6.31, 14.6.32, 14.6.33
// ============================================================================

/**
 * Cell click action type
 * 单元格点击动作类型
 * 
 * Validates: 14.6.28 - 单元格点击事件（钻取、跳转、弹窗）
 */
export type CellClickActionType = 'drill-down' | 'navigate' | 'popup' | 'custom'

/**
 * Drill-down configuration
 * 钻取配置
 */
export interface DrillDownConfig {
  /** Target report/chart ID */
  targetId?: string
  /** Target URL for navigation */
  targetUrl?: string
  /** Parameters to pass */
  params?: Record<string, string>
  /** Parameter mapping from current row */
  paramMapping?: Record<string, string>
  /** Drill-down level */
  level?: number
  /** Custom drill-down handler */
  handler?: (row: Record<string, any>, column: ColumnConfig, value: any) => void
}

/**
 * Cell click action configuration
 * 单元格点击动作配置
 */
export interface CellClickAction {
  /** Action type */
  type: CellClickActionType
  /** Column keys this action applies to */
  columns?: string[]
  /** Drill-down configuration */
  drillDown?: DrillDownConfig
  /** Navigation URL template (supports {field} placeholders) */
  navigateUrl?: string
  /** Popup content configuration */
  popupConfig?: {
    title?: string
    content?: string | ((row: Record<string, any>, value: any) => string)
    width?: number
    height?: number
  }
  /** Custom action handler */
  handler?: (event: CellClickEvent) => void
}

/**
 * Tooltip configuration
 * 悬停提示配置
 * 
 * Validates: 14.6.29 - 单元格悬停提示（Tooltip）
 */
export interface TooltipConfig {
  /** Enable tooltip */
  enabled: boolean
  /** Show tooltip only for truncated content */
  showOnTruncate?: boolean
  /** Custom tooltip content generator */
  content?: (row: Record<string, any>, column: ColumnConfig, value: any) => string
  /** Tooltip placement */
  placement?: 'top' | 'bottom' | 'left' | 'right'
  /** Delay before showing tooltip (ms) */
  showDelay?: number
  /** Delay before hiding tooltip (ms) */
  hideDelay?: number
  /** Maximum tooltip width */
  maxWidth?: number
  /** Columns to enable tooltip */
  columns?: string[]
}

/**
 * Sort direction
 * 排序方向
 */
export type SortDirection = 'asc' | 'desc' | null

/**
 * Sort configuration
 * 排序配置
 * 
 * Validates: 14.6.30 - 列排序（点击表头排序）
 */
export interface SortConfig {
  /** Column key being sorted */
  column: string | null
  /** Sort direction */
  direction: SortDirection
  /** Enable multi-column sort */
  multiple?: boolean
  /** Sort order for multi-column sort */
  sortOrder?: Array<{ column: string; direction: SortDirection }>
  /** Custom sort comparator */
  comparator?: (a: any, b: any, column: string, direction: SortDirection) => number
  /** Remote sort (emit event instead of local sort) */
  remote?: boolean
}

/**
 * Filter operator type
 * 筛选操作符类型
 */
export type FilterOperator = 
  | 'equals' 
  | 'notEquals' 
  | 'contains' 
  | 'notContains' 
  | 'startsWith' 
  | 'endsWith'
  | 'greaterThan'
  | 'lessThan'
  | 'greaterThanOrEqual'
  | 'lessThanOrEqual'
  | 'between'
  | 'in'
  | 'notIn'
  | 'isEmpty'
  | 'isNotEmpty'

/**
 * Filter value type
 * 筛选值类型
 */
export interface FilterValue {
  /** Filter operator */
  operator: FilterOperator
  /** Filter value(s) */
  value: any
  /** Second value for 'between' operator */
  value2?: any
}

/**
 * Column filter configuration
 * 列筛选配置
 * 
 * Validates: 14.6.31 - 列筛选（下拉筛选器）
 */
export interface ColumnFilterConfig {
  /** Column key */
  column: string
  /** Filter type */
  type: 'text' | 'select' | 'number' | 'date' | 'custom'
  /** Available options for select filter */
  options?: Array<{ label: string; value: any }>
  /** Load options dynamically */
  loadOptions?: () => Promise<Array<{ label: string; value: any }>>
  /** Multiple selection */
  multiple?: boolean
  /** Searchable options */
  searchable?: boolean
  /** Custom filter function */
  filterFn?: (value: any, filterValue: FilterValue, row: Record<string, any>) => boolean
}

/**
 * Filter state
 * 筛选状态
 */
export interface FilterState {
  /** Active filters by column */
  filters: Map<string, FilterValue>
  /** Filter configurations by column */
  configs: Map<string, ColumnFilterConfig>
}

/**
 * Column drag configuration
 * 列拖拽配置
 * 
 * Validates: 14.6.32 - 列拖拽调整宽度
 * Validates: 14.6.33 - 列拖拽调整顺序
 */
export interface ColumnDragConfig {
  /** Enable column resize by dragging */
  resizable?: boolean
  /** Enable column reorder by dragging */
  reorderable?: boolean
  /** Minimum column width when resizing */
  minWidth?: number
  /** Maximum column width when resizing */
  maxWidth?: number
  /** Columns that cannot be resized */
  nonResizableColumns?: string[]
  /** Columns that cannot be reordered */
  nonReorderableColumns?: string[]
}

/**
 * Column reorder event
 * 列重排序事件
 */
export interface ColumnReorderEvent {
  /** Column being moved */
  column: ColumnConfig
  /** Original index */
  fromIndex: number
  /** New index */
  toIndex: number
  /** New column order */
  newOrder: string[]
}

/**
 * Sort change event
 * 排序变更事件
 */
export interface SortChangeEvent {
  /** Column being sorted */
  column: string
  /** Sort direction */
  direction: SortDirection
  /** Full sort configuration */
  sortConfig: SortConfig
}

/**
 * Filter change event
 * 筛选变更事件
 */
export interface FilterChangeEvent {
  /** Column being filtered */
  column: string
  /** Filter value */
  value: FilterValue | null
  /** All active filters */
  filters: Map<string, FilterValue>
}

/**
 * Table interaction configuration
 * 表格交互配置
 */
export interface TableInteractionConfig {
  /** Cell click actions */
  cellClickActions?: CellClickAction[]
  /** Tooltip configuration */
  tooltip?: TooltipConfig
  /** Sort configuration */
  sort?: Partial<SortConfig>
  /** Filter configurations */
  filters?: ColumnFilterConfig[]
  /** Column drag configuration */
  columnDrag?: ColumnDragConfig
}

/**
 * Tooltip state
 * 提示框状态
 */
export interface TooltipState {
  /** Whether tooltip is visible */
  visible: boolean
  /** Tooltip content */
  content: string
  /** Tooltip position */
  position: { x: number; y: number }
  /** Target cell info */
  cell?: {
    rowIndex: number
    columnKey: string
    value: any
  }
}

// ============================================================================
// Utility Types (工具类型)
// ============================================================================

/**
 * Cell key generator
 */
export type CellKeyGenerator = (rowIndex: number, columnKey: string) => string

/**
 * Default cell key generator
 */
export const defaultCellKeyGenerator: CellKeyGenerator = (rowIndex, columnKey) => 
  `${rowIndex}-${columnKey}`

/**
 * Row key generator
 */
export type RowKeyGenerator = (row: Record<string, any>, index: number) => string

/**
 * Default row key generator
 */
export const defaultRowKeyGenerator: RowKeyGenerator = (row, index) => 
  row.id?.toString() ?? index.toString()

// ============================================================================
// Default Configurations (默认配置)
// ============================================================================

/**
 * Default merge configuration
 */
export const DEFAULT_MERGE_CONFIG: MergeConfig = {
  autoRowMerge: false,
  autoRowMergeColumns: [],
  rules: [],
  manualMerges: [],
}

/**
 * Default freeze configuration
 */
export const DEFAULT_FREEZE_CONFIG: FreezeConfig = {
  freezeHeader: true,
  freezeHeaderRows: 1,
  freezeLeftColumns: 0,
  freezeRightColumns: 0,
}

/**
 * Default auto size configuration
 */
export const DEFAULT_AUTO_SIZE_CONFIG: AutoSizeConfig = {
  autoColumnWidth: false,
  autoWidthColumns: [],
  includeHeader: true,
  padding: 16,
  maxAutoWidth: 300,
  minAutoWidth: 50,
  autoRowHeight: false,
  minRowHeight: 40,
  maxRowHeight: 200,
}

/**
 * Default zebra stripe configuration
 */
export const DEFAULT_ZEBRA_STRIPE_CONFIG: ZebraStripeConfig = {
  enabled: true,
  evenRowColor: '#fafafa',
  oddRowColor: '#ffffff',
  applyToHeader: false,
  stripeInterval: 1,
}

/**
 * Default table style configuration
 */
export const DEFAULT_TABLE_STYLE_CONFIG: TableStyleConfig = {
  border: {
    outer: { style: 'solid', width: 1, color: '#e8e8e8' },
    inner: { style: 'solid', width: 1, color: '#e8e8e8' },
    header: { style: 'solid', width: 1, color: '#e8e8e8' },
  },
  headerStyle: {
    backgroundColor: '#fafafa',
    color: '#262626',
    fontWeight: 600,
    height: 48,
  },
  bodyStyle: {
    backgroundColor: '#ffffff',
    color: '#262626',
    height: 48,
  },
  hoverStyle: {
    backgroundColor: '#f5f5f5',
  },
  selectedStyle: {
    backgroundColor: '#e6f7ff',
  },
}

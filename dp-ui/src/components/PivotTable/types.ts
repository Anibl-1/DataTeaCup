/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 数据透视表类型定义
 * Pivot Table Type Definitions
 * 
 * 支持行列维度拖拽配置、多级分组汇总、小计和总计位置配置
 * Supports row/column dimension drag-drop configuration, multi-level grouping, subtotals and totals positioning
 * 
 * 需求: 22.3.7, 22.3.8, 22.3.9
 */

// ============================================================================
// Dimension Types (维度类型)
// ============================================================================

/**
 * Dimension field configuration
 * 维度字段配置
 * 
 * Validates: 22.3.7 - 支持行列维度拖拽配置
 */
export interface DimensionField {
  /** Unique field identifier */
  id: string
  /** Field name in data */
  field: string
  /** Display label */
  label: string
  /** Field data type */
  dataType: 'string' | 'number' | 'date' | 'boolean'
  /** Sort order for this dimension */
  sortOrder?: 'asc' | 'desc' | 'none'
  /** Custom sort comparator */
  sortComparator?: (a: any, b: any) => number
  /** Custom formatter for display */
  formatter?: (value: any) => string
  /** Whether field is currently active */
  active?: boolean
}

/**
 * Value field configuration with aggregation
 * 值字段配置（含聚合方式）
 * 
 * Validates: 22.3.8 - 支持多级分组汇总
 */
export interface ValueField {
  /** Unique field identifier */
  id: string
  /** Field name in data */
  field: string
  /** Display label */
  label: string
  /** Aggregation function */
  aggregation: AggregationType
  /** Number format configuration */
  format?: NumberFormat
  /** Custom aggregation function */
  customAggregation?: (values: any[]) => any
}

/**
 * Aggregation type
 * 聚合类型
 */
export type AggregationType = 
  | 'sum' 
  | 'count' 
  | 'avg' 
  | 'min' 
  | 'max' 
  | 'countDistinct'
  | 'first'
  | 'last'
  | 'custom'

/**
 * Number format configuration
 * 数值格式配置
 */
export interface NumberFormat {
  /** Decimal places */
  decimalPlaces?: number
  /** Use thousands separator */
  useThousandsSeparator?: boolean
  /** Prefix (e.g., '$', '¥') */
  prefix?: string
  /** Suffix (e.g., '%', '元') */
  suffix?: string
  /** Display as percentage */
  asPercentage?: boolean
}

// ============================================================================
// Filter Types (筛选类型)
// ============================================================================

/**
 * Filter field configuration
 * 筛选字段配置
 */
export interface FilterField {
  /** Unique field identifier */
  id: string
  /** Field name in data */
  field: string
  /** Display label */
  label: string
  /** Filter type */
  filterType: 'select' | 'multiSelect' | 'range' | 'search'
  /** Selected filter values */
  values?: any[]
  /** Available options (for select types) */
  options?: FilterOption[]
}

/**
 * Filter option
 * 筛选选项
 */
export interface FilterOption {
  /** Option label */
  label: string
  /** Option value */
  value: any
  /** Whether option is selected */
  selected?: boolean
}

// ============================================================================
// Pivot Configuration Types (透视配置类型)
// ============================================================================

/**
 * Pivot table configuration
 * 透视表配置
 * 
 * Validates: 22.3.7 - 支持行列维度拖拽配置
 */
export interface PivotConfig {
  /** Row dimension fields */
  rows: DimensionField[]
  /** Column dimension fields */
  columns: DimensionField[]
  /** Value fields with aggregation */
  values: ValueField[]
  /** Filter fields */
  filters?: FilterField[]
}

/**
 * Subtotal and total position configuration
 * 小计和总计位置配置
 * 
 * Validates: 22.3.9 - 支持小计和总计显示位置配置
 */
export interface TotalConfig {
  /** Show row subtotals */
  showRowSubtotals?: boolean
  /** Row subtotal position */
  rowSubtotalPosition?: 'top' | 'bottom'
  /** Show column subtotals */
  showColumnSubtotals?: boolean
  /** Column subtotal position */
  columnSubtotalPosition?: 'left' | 'right'
  /** Show grand total for rows */
  showRowGrandTotal?: boolean
  /** Row grand total position */
  rowGrandTotalPosition?: 'top' | 'bottom'
  /** Show grand total for columns */
  showColumnGrandTotal?: boolean
  /** Column grand total position */
  columnGrandTotalPosition?: 'left' | 'right'
  /** Subtotal label template */
  subtotalLabel?: string
  /** Grand total label */
  grandTotalLabel?: string
}

// ============================================================================
// Pivot Data Types (透视数据类型)
// ============================================================================

/**
 * Pivot cell data
 * 透视单元格数据
 */
export interface PivotCell {
  /** Cell value */
  value: any
  /** Formatted display value */
  formattedValue: string
  /** Row path (dimension values) */
  rowPath: any[]
  /** Column path (dimension values) */
  columnPath: any[]
  /** Value field ID */
  valueFieldId: string
  /** Whether this is a subtotal cell */
  isSubtotal: boolean
  /** Whether this is a grand total cell */
  isGrandTotal: boolean
  /** Aggregated raw values (for drill-down) */
  rawValues?: any[]
  /** Number of records aggregated */
  recordCount: number
}

/**
 * Pivot row header
 * 透视行表头
 */
export interface PivotRowHeader {
  /** Header value */
  value: any
  /** Display label */
  label: string
  /** Depth level (0 = root) */
  depth: number
  /** Parent header */
  parent?: PivotRowHeader
  /** Child headers */
  children: PivotRowHeader[]
  /** Whether this is a subtotal row */
  isSubtotal: boolean
  /** Whether this is a grand total row */
  isGrandTotal: boolean
  /** Whether row is expanded */
  expanded: boolean
  /** Row span for merged cells */
  rowSpan: number
  /** Path from root to this header */
  path: any[]
}

/**
 * Pivot column header
 * 透视列表头
 */
export interface PivotColumnHeader {
  /** Header value */
  value: any
  /** Display label */
  label: string
  /** Depth level (0 = root) */
  depth: number
  /** Parent header */
  parent?: PivotColumnHeader
  /** Child headers */
  children: PivotColumnHeader[]
  /** Whether this is a subtotal column */
  isSubtotal: boolean
  /** Whether this is a grand total column */
  isGrandTotal: boolean
  /** Column span for merged cells */
  colSpan: number
  /** Path from root to this header */
  path: any[]
}

/**
 * Computed pivot data
 * 计算后的透视数据
 */
export interface PivotData {
  /** Row headers hierarchy */
  rowHeaders: PivotRowHeader[]
  /** Column headers hierarchy */
  columnHeaders: PivotColumnHeader[]
  /** Cell data matrix */
  cells: Map<string, PivotCell>
  /** Flattened row headers for rendering */
  flattenedRowHeaders: PivotRowHeader[]
  /** Flattened column headers for rendering */
  flattenedColumnHeaders: PivotColumnHeader[]
  /** Total row count */
  totalRows: number
  /** Total column count */
  totalColumns: number
}

// ============================================================================
// Drag and Drop Types (拖拽类型)
// ============================================================================

/**
 * Drag zone type
 * 拖拽区域类型
 * 
 * Validates: 22.3.7 - 支持行列维度拖拽配置
 */
export type DragZoneType = 'available' | 'rows' | 'columns' | 'values' | 'filters'

/**
 * Drag item
 * 拖拽项
 */
export interface DragItem {
  /** Item type */
  type: 'dimension' | 'value' | 'filter'
  /** Field data */
  field: DimensionField | ValueField | FilterField
  /** Source zone */
  sourceZone: DragZoneType
}

/**
 * Drop event
 * 放置事件
 */
export interface DropEvent {
  /** Dragged item */
  item: DragItem
  /** Target zone */
  targetZone: DragZoneType
  /** Target index in zone */
  targetIndex: number
}

// ============================================================================
// Pivot Table Props Types (透视表属性类型)
// ============================================================================

/**
 * Pivot table props
 * 透视表属性
 */
export interface PivotTableProps {
  /** Raw data array */
  data: Record<string, any>[]
  /** Available fields for pivot */
  availableFields: DimensionField[]
  /** Initial pivot configuration */
  config?: PivotConfig
  /** Total configuration */
  totalConfig?: TotalConfig
  /** Table height */
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
  /** Enable row expand/collapse */
  expandable?: boolean
  /** Default expanded row paths */
  defaultExpandedPaths?: any[][]
  /** Enable cell click for drill-down */
  drillDownEnabled?: boolean
  /** Show configuration panel */
  showConfigPanel?: boolean
  /** Configuration panel position */
  configPanelPosition?: 'left' | 'right' | 'top'
}

// ============================================================================
// Event Types (事件类型)
// ============================================================================

/**
 * Config change event
 * 配置变更事件
 */
export interface ConfigChangeEvent {
  /** New configuration */
  config: PivotConfig
  /** Changed zone */
  changedZone: DragZoneType
}

/**
 * Cell click event
 * 单元格点击事件
 */
export interface PivotCellClickEvent {
  /** Cell data */
  cell: PivotCell
  /** Row header */
  rowHeader: PivotRowHeader
  /** Column header */
  columnHeader: PivotColumnHeader
  /** Mouse event */
  event: MouseEvent
}

/**
 * Row expand event
 * 行展开事件
 */
export interface RowExpandEvent {
  /** Row header being expanded */
  rowHeader: PivotRowHeader
  /** Whether row is now expanded */
  expanded: boolean
  /** All expanded paths */
  expandedPaths: any[][]
}

/**
 * Drill down event
 * 钻取事件
 */
export interface DrillDownEvent {
  /** Cell data */
  cell: PivotCell
  /** Raw records for this cell */
  records: Record<string, any>[]
  /** Row path */
  rowPath: any[]
  /** Column path */
  columnPath: any[]
}

// ============================================================================
// Default Configurations (默认配置)
// ============================================================================

/**
 * Default total configuration
 */
export const DEFAULT_TOTAL_CONFIG: Required<TotalConfig> = {
  showRowSubtotals: true,
  rowSubtotalPosition: 'bottom',
  showColumnSubtotals: true,
  columnSubtotalPosition: 'right',
  showRowGrandTotal: true,
  rowGrandTotalPosition: 'bottom',
  showColumnGrandTotal: true,
  columnGrandTotalPosition: 'right',
  subtotalLabel: '小计',
  grandTotalLabel: '总计',
}

/**
 * Default pivot configuration
 */
export const DEFAULT_PIVOT_CONFIG: PivotConfig = {
  rows: [],
  columns: [],
  values: [],
  filters: [],
}

/**
 * Default number format
 */
export const DEFAULT_NUMBER_FORMAT: NumberFormat = {
  decimalPlaces: 2,
  useThousandsSeparator: true,
  prefix: '',
  suffix: '',
  asPercentage: false,
}

// ============================================================================
// Utility Types (工具类型)
// ============================================================================

/**
 * Cell key generator
 */
export type CellKeyGenerator = (rowPath: any[], columnPath: any[], valueFieldId: string) => string

/**
 * Default cell key generator
 */
export const defaultCellKeyGenerator: CellKeyGenerator = (rowPath, columnPath, valueFieldId) => 
  `${rowPath.join('|')}::${columnPath.join('|')}::${valueFieldId}`

/**
 * Path comparator for sorting
 */
export type PathComparator = (a: any[], b: any[]) => number

/**
 * Default path comparator
 */
export const defaultPathComparator: PathComparator = (a, b) => {
  for (let i = 0; i < Math.min(a.length, b.length); i++) {
    if (a[i] < b[i]) return -1
    if (a[i] > b[i]) return 1
  }
  return a.length - b.length
}

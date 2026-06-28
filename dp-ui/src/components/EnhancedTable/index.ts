/**
 * EnhancedTable Module Exports
 * 增强表格模块导出
 * 
 * 需求: 14.5.21, 14.5.22, 14.5.23, 14.5.24, 14.5.25, 14.5.26
 */

// Component
export { default as EnhancedTable } from './EnhancedTable.vue'

// Types
export type {
  // Column types
  ColumnConfig,
  
  // Merge types
  CellMergeSpan,
  MergeRule,
  ManualMergeCell,
  MergeConfig,
  
  // Freeze types
  FreezeConfig,
  
  // Auto size types
  AutoSizeConfig,
  
  // Zebra stripe types
  ZebraStripeConfig,
  
  // Table style types
  TableBorderStyle,
  TableStyleConfig,
  
  // Props types
  EnhancedTableProps,
  
  // Computed types
  ComputedCellInfo,
  ComputedColumnInfo,
  
  // State types
  TableScrollState,
  TableLayoutState,
  
  // Event types
  CellClickEvent,
  ColumnResizeEvent,
  TableScrollEvent,
  
  // Utility types
  CellKeyGenerator,
  RowKeyGenerator,
} from './types'

// Default configurations
export {
  DEFAULT_MERGE_CONFIG,
  DEFAULT_FREEZE_CONFIG,
  DEFAULT_AUTO_SIZE_CONFIG,
  DEFAULT_ZEBRA_STRIPE_CONFIG,
  DEFAULT_TABLE_STYLE_CONFIG,
  defaultCellKeyGenerator,
  defaultRowKeyGenerator,
} from './types'

// Composable
export { useTableLayout } from '@/composables/useTableLayout'
export type { UseTableLayoutOptions, UseTableLayoutReturn } from '@/composables/useTableLayout'

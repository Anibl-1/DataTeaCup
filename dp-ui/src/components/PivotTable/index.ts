/**
 * 数据透视表模块导出
 * Pivot Table Module Exports
 * 
 * 需求: 22.3.7, 22.3.8, 22.3.9
 */

// Components
export { default as PivotTable } from './PivotTable.vue'
export { default as PivotConfig } from './PivotConfig.vue'

// Types
export type {
  // Dimension types
  DimensionField,
  ValueField,
  FilterField,
  FilterOption,
  AggregationType,
  NumberFormat,
  
  // Configuration types
  PivotConfig,
  TotalConfig,
  
  // Data types
  PivotCell,
  PivotRowHeader,
  PivotColumnHeader,
  PivotData,
  
  // Drag and drop types
  DragZoneType,
  DragItem,
  DropEvent,
  
  // Props types
  PivotTableProps,
  
  // Event types
  ConfigChangeEvent,
  PivotCellClickEvent,
  RowExpandEvent,
  DrillDownEvent,
  
  // Utility types
  CellKeyGenerator,
  PathComparator,
} from './types'

// Constants
export {
  DEFAULT_TOTAL_CONFIG,
  DEFAULT_PIVOT_CONFIG,
  DEFAULT_NUMBER_FORMAT,
  defaultCellKeyGenerator,
  defaultPathComparator,
} from './types'

// Utilities
export {
  // Aggregation
  aggregationFunctions,
  calculateAggregation,
  
  // Formatting
  formatNumber,
  
  // Data processing
  applyFilters,
  groupByDimensions,
  buildHierarchicalHeaders,
  flattenRowHeaders,
  flattenColumnHeaders,
  calculatePivotCells,
  calculatePivotData,
  
  // Field utilities
  getUniqueValues,
  inferFieldDataType,
  createDimensionField,
  createValueField,
} from './pivotUtils'

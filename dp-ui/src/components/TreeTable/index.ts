/**
 * 树形表格组件模块导出
 * Tree Table Component Module Exports
 * 
 * 需求: 22.2.4, 22.2.5, 22.2.6
 */

// Main component
export { default as TreeTable } from './TreeTable.vue'

// Row component (for advanced usage)
export { default as TreeTableRow } from './TreeTableRow.vue'

// Types
export type {
  // Node types
  TreeNode,
  FlattenedTreeNode,
  NodeKey,
  NodeKeyGetter,
  
  // Column types
  TreeTableColumn,
  
  // Configuration types
  ExpandConfig,
  ExpandState,
  IndentConfig,
  TreeTableProps,
  
  // Event types
  ExpandEvent,
  RowClickEvent,
  SelectionChangeEvent,
  CheckChangeEvent,
} from './types'

// Default configurations
export {
  DEFAULT_EXPAND_CONFIG,
  DEFAULT_INDENT_CONFIG,
  defaultNodeKeyGetter,
} from './types'

/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 树形表格类型定义
 * Tree Table Type Definitions
 * 
 * 支持层级数据展示、展开/折叠操作、缩进显示
 * Supports hierarchical data display, expand/collapse operations, indentation
 * 
 * 需求: 22.2.4, 22.2.5, 22.2.6
 */

// ============================================================================
// Tree Node Types (树节点类型)
// ============================================================================

/**
 * Tree node interface
 * 树节点接口
 * 
 * Validates: 22.2.4 - 树形表格支持层级数据展示
 */
export interface TreeNode {
  /** Unique identifier */
  id: string | number
  /** Child nodes */
  children?: TreeNode[]
  /** Whether node is a leaf (no children) */
  isLeaf?: boolean
  /** Whether children are loading (for lazy load) */
  loading?: boolean
  /** Whether node is disabled */
  disabled?: boolean
  /** Additional data fields */
  [key: string]: any
}

/**
 * Flattened tree node with depth info
 * 扁平化的树节点（包含深度信息）
 */
export interface FlattenedTreeNode extends TreeNode {
  /** Node depth level (0 = root) */
  depth: number
  /** Parent node ID */
  parentId: string | number | null
  /** Whether node is expanded */
  expanded: boolean
  /** Whether node is visible */
  visible: boolean
  /** Path from root to this node */
  path: (string | number)[]
  /** Whether node has children */
  hasChildren: boolean
}

// ============================================================================
// Column Configuration Types (列配置类型)
// ============================================================================

/**
 * Tree table column configuration
 * 树形表格列配置
 */
export interface TreeTableColumn {
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
  /** Text alignment */
  align?: 'left' | 'center' | 'right'
  /** Whether this is the tree column (shows expand icon) */
  treeColumn?: boolean
  /** Custom render function */
  render?: (value: any, node: FlattenedTreeNode, rowIndex: number) => any
  /** Column class name */
  className?: string
  /** Header class name */
  headerClassName?: string
  /** Ellipsis for overflow text */
  ellipsis?: boolean
}

// ============================================================================
// Expand/Collapse Configuration Types (展开/折叠配置类型)
// ============================================================================

/**
 * Expand configuration
 * 展开配置
 * 
 * Validates: 22.2.5 - 支持展开/折叠操作（单行、全部展开/折叠）
 */
export interface ExpandConfig {
  /** Default expand all nodes */
  defaultExpandAll?: boolean
  /** Default expanded node keys */
  defaultExpandedKeys?: (string | number)[]
  /** Expand to specific depth level */
  defaultExpandDepth?: number
  /** Accordion mode (only one node expanded at same level) */
  accordion?: boolean
  /** Enable lazy loading of children */
  lazyLoad?: boolean
  /** Load children function for lazy loading */
  loadChildren?: (node: TreeNode) => Promise<TreeNode[]>
  /** Expand icon position */
  expandIconPosition?: 'start' | 'end'
}

/**
 * Expand state
 * 展开状态
 */
export interface ExpandState {
  /** Set of expanded node keys */
  expandedKeys: Set<string | number>
  /** Set of loading node keys */
  loadingKeys: Set<string | number>
}

// ============================================================================
// Indentation Configuration Types (缩进配置类型)
// ============================================================================

/**
 * Indentation configuration
 * 缩进配置
 * 
 * Validates: 22.2.6 - 支持缩进显示（根据层级自动缩进）
 */
export interface IndentConfig {
  /** Indentation width per level (px) */
  indentSize?: number
  /** Show tree lines connecting nodes */
  showLine?: boolean
  /** Line style */
  lineStyle?: 'solid' | 'dashed' | 'dotted'
  /** Line color */
  lineColor?: string
}

// ============================================================================
// Tree Table Props Types (树形表格属性类型)
// ============================================================================

/**
 * Tree table props
 * 树形表格属性
 */
export interface TreeTableProps {
  /** Tree data */
  data: TreeNode[]
  /** Column configurations */
  columns: TreeTableColumn[]
  /** Controlled expanded keys */
  expandedKeys?: (string | number)[]
  /** Expand configuration */
  expandConfig?: ExpandConfig
  /** Indentation configuration */
  indentConfig?: IndentConfig
  /** Row key field (default: 'id') */
  rowKey?: string | ((node: TreeNode) => string | number)
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
  /** Enable row selection */
  selectable?: boolean
  /** Selected row keys */
  selectedKeys?: (string | number)[]
  /** Enable checkbox selection */
  checkable?: boolean
  /** Checked row keys */
  checkedKeys?: (string | number)[]
  /** Enable keyboard navigation */
  keyboardNavigation?: boolean
}

// ============================================================================
// Event Types (事件类型)
// ============================================================================

/**
 * Expand event
 * 展开事件
 */
export interface ExpandEvent {
  /** Node being expanded */
  node: FlattenedTreeNode
  /** Whether node is now expanded */
  expanded: boolean
  /** All expanded keys */
  expandedKeys: (string | number)[]
}

/**
 * Row click event
 * 行点击事件
 */
export interface RowClickEvent {
  /** Clicked node */
  node: FlattenedTreeNode
  /** Row index */
  rowIndex: number
  /** Mouse event */
  event: MouseEvent
}

/**
 * Selection change event
 * 选择变更事件
 */
export interface SelectionChangeEvent {
  /** Selected node keys */
  selectedKeys: (string | number)[]
  /** Selected nodes */
  selectedNodes: FlattenedTreeNode[]
}

/**
 * Check change event
 * 勾选变更事件
 */
export interface CheckChangeEvent {
  /** Checked node keys */
  checkedKeys: (string | number)[]
  /** Checked nodes */
  checkedNodes: FlattenedTreeNode[]
  /** Half-checked keys (some children checked) */
  halfCheckedKeys: (string | number)[]
}

// ============================================================================
// Default Configurations (默认配置)
// ============================================================================

/**
 * Default expand configuration
 */
export const DEFAULT_EXPAND_CONFIG: Required<ExpandConfig> = {
  defaultExpandAll: false,
  defaultExpandedKeys: [],
  defaultExpandDepth: 0,
  accordion: false,
  lazyLoad: false,
  loadChildren: async () => [],
  expandIconPosition: 'start',
}

/**
 * Default indentation configuration
 */
export const DEFAULT_INDENT_CONFIG: Required<IndentConfig> = {
  indentSize: 24,
  showLine: false,
  lineStyle: 'solid',
  lineColor: '#d9d9d9',
}

// ============================================================================
// Utility Types (工具类型)
// ============================================================================

/**
 * Node key type
 */
export type NodeKey = string | number

/**
 * Node key getter function
 */
export type NodeKeyGetter = (node: TreeNode) => NodeKey

/**
 * Default node key getter
 */
export const defaultNodeKeyGetter: NodeKeyGetter = (node) => node.id

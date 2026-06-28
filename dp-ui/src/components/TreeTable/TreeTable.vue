<script setup lang="ts">
/**
 * 树形表格组件
 * Tree Table Component
 * 
 * 支持层级数据展示、展开/折叠操作、缩进显示
 * Supports hierarchical data display, expand/collapse operations, indentation
 * 
 * 需求: 22.2.4, 22.2.5, 22.2.6
 */

import { ref, computed, watch, onMounted, nextTick } from 'vue'
import TreeTableRow from './TreeTableRow.vue'
import type {
  TreeNode,
  FlattenedTreeNode,
  TreeTableColumn,
  ExpandConfig,
  IndentConfig,
  ExpandEvent,
  RowClickEvent,
  NodeKey,
  NodeKeyGetter,
} from './types'
import {
  DEFAULT_EXPAND_CONFIG,
  DEFAULT_INDENT_CONFIG,
  defaultNodeKeyGetter,
} from './types'

// ============================================================================
// Props
// ============================================================================

interface Props {
  /** Tree data */
  data: TreeNode[]
  /** Column configurations */
  columns: TreeTableColumn[]
  /** Controlled expanded keys */
  expandedKeys?: NodeKey[]
  /** Expand configuration */
  expandConfig?: ExpandConfig
  /** Indentation configuration */
  indentConfig?: IndentConfig
  /** Row key field (default: 'id') */
  rowKey?: string | NodeKeyGetter
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
  /** Selected row keys */
  selectedKeys?: NodeKey[]
  /** Enable keyboard navigation */
  keyboardNavigation?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  rowKey: 'id',
  loading: false,
  emptyText: '暂无数据',
  bordered: true,
  size: 'medium',
  keyboardNavigation: true,
})

// ============================================================================
// Emits
// ============================================================================

const emit = defineEmits<{
  (e: 'expand', event: ExpandEvent): void
  (e: 'collapse', event: ExpandEvent): void
  (e: 'row-click', event: RowClickEvent): void
  (e: 'update:expandedKeys', keys: NodeKey[]): void
  (e: 'update:selectedKeys', keys: NodeKey[]): void
}>()

// ============================================================================
// Refs
// ============================================================================

const tableContainerRef = ref<HTMLElement | null>(null)

// ============================================================================
// State
// ============================================================================

// Internal expanded keys state
const internalExpandedKeys = ref<Set<NodeKey>>(new Set())
// Loading keys for lazy load
const loadingKeys = ref<Set<NodeKey>>(new Set())
// Internal selected keys
const internalSelectedKeys = ref<Set<NodeKey>>(new Set())

// ============================================================================
// Computed
// ============================================================================

const normalizedExpandConfig = computed<Required<ExpandConfig>>(() => ({
  ...DEFAULT_EXPAND_CONFIG,
  ...props.expandConfig,
}))

const normalizedIndentConfig = computed<Required<IndentConfig>>(() => ({
  ...DEFAULT_INDENT_CONFIG,
  ...props.indentConfig,
}))

/**
 * Get node key from node
 */
const getNodeKey = computed<NodeKeyGetter>(() => {
  if (typeof props.rowKey === 'function') {
    return props.rowKey
  }
  return (node: TreeNode) => node[props.rowKey as string] as NodeKey
})

/**
 * Effective expanded keys (controlled or internal)
 */
const effectiveExpandedKeys = computed<Set<NodeKey>>(() => {
  if (props.expandedKeys !== undefined) {
    return new Set(props.expandedKeys)
  }
  return internalExpandedKeys.value
})

/**
 * Effective selected keys (controlled or internal)
 */
const effectiveSelectedKeys = computed<Set<NodeKey>>(() => {
  if (props.selectedKeys !== undefined) {
    return new Set(props.selectedKeys)
  }
  return internalSelectedKeys.value
})

/**
 * Flatten tree data for rendering
 * 扁平化树数据用于渲染
 * 
 * Validates: 22.2.4 - 树形表格支持层级数据展示
 */
const flattenedData = computed<FlattenedTreeNode[]>(() => {
  const result: FlattenedTreeNode[] = []
  const expandedSet = effectiveExpandedKeys.value
  
  const flatten = (
    nodes: TreeNode[],
    depth: number,
    parentId: NodeKey | null,
    parentPath: NodeKey[],
    parentVisible: boolean
  ) => {
    for (const node of nodes) {
      const nodeKey = getNodeKey.value(node)
      const hasChildren = !!(node.children && node.children.length > 0) || node.isLeaf === false
      const isExpanded = expandedSet.has(nodeKey)
      const path = [...parentPath, nodeKey]
      
      const flatNode: FlattenedTreeNode = {
        ...node,
        id: nodeKey,
        depth,
        parentId,
        expanded: isExpanded,
        visible: parentVisible,
        path,
        hasChildren,
      }
      
      result.push(flatNode)
      
      // Recursively flatten children if expanded
      if (node.children && node.children.length > 0 && isExpanded) {
        flatten(node.children, depth + 1, nodeKey, path, true)
      }
    }
  }
  
  flatten(props.data, 0, null, [], true)
  return result
})

/**
 * Visible flattened data (only visible nodes)
 */
const visibleData = computed<FlattenedTreeNode[]>(() => {
  return flattenedData.value.filter(node => node.visible)
})

const tableContainerStyle = computed(() => ({
  height: typeof props.height === 'number' ? `${props.height}px` : props.height,
  maxHeight: typeof props.maxHeight === 'number' ? `${props.maxHeight}px` : props.maxHeight,
}))

const tableClasses = computed(() => [
  'tree-table',
  `tree-table--${props.size}`,
  {
    'tree-table--bordered': props.bordered,
    'tree-table--loading': props.loading,
  },
])

const rowHeight = computed(() => {
  switch (props.size) {
    case 'small': return 36
    case 'large': return 56
    default: return 48
  }
})

// ============================================================================
// Methods
// ============================================================================

/**
 * Handle node expand
 * 处理节点展开
 * 
 * Validates: 22.2.5 - 支持展开/折叠操作
 */
const handleExpand = async (nodeKey: NodeKey) => {
  const node = flattenedData.value.find(n => n.id === nodeKey)
  if (!node) return
  
  // Handle lazy loading
  if (normalizedExpandConfig.value.lazyLoad && !node.children?.length && !node.isLeaf) {
    loadingKeys.value.add(nodeKey)
    
    try {
      const children = await normalizedExpandConfig.value.loadChildren(node)
      // Update the original data with loaded children
      updateNodeChildren(nodeKey, children)
    } catch (error) {
      console.error('Failed to load children:', error)
    } finally {
      loadingKeys.value.delete(nodeKey)
    }
  }
  
  // Handle accordion mode
  if (normalizedExpandConfig.value.accordion) {
    // Collapse siblings at the same level
    const siblings = flattenedData.value.filter(
      n => n.parentId === node.parentId && n.id !== nodeKey
    )
    for (const sibling of siblings) {
      if (effectiveExpandedKeys.value.has(sibling.id)) {
        collapseNode(sibling.id)
      }
    }
  }
  
  // Expand the node
  expandNode(nodeKey)
  
  // Emit event
  const expandedKeysArray = Array.from(effectiveExpandedKeys.value)
  emit('expand', {
    node,
    expanded: true,
    expandedKeys: expandedKeysArray,
  })
  emit('update:expandedKeys', expandedKeysArray)
}

/**
 * Handle node collapse
 * 处理节点折叠
 */
const handleCollapse = (nodeKey: NodeKey) => {
  const node = flattenedData.value.find(n => n.id === nodeKey)
  if (!node) return
  
  collapseNode(nodeKey)
  
  // Emit event
  const expandedKeysArray = Array.from(effectiveExpandedKeys.value)
  emit('collapse', {
    node,
    expanded: false,
    expandedKeys: expandedKeysArray,
  })
  emit('update:expandedKeys', expandedKeysArray)
}

/**
 * Expand a node
 */
const expandNode = (nodeKey: NodeKey) => {
  if (props.expandedKeys === undefined) {
    internalExpandedKeys.value.add(nodeKey)
  }
}

/**
 * Collapse a node
 */
const collapseNode = (nodeKey: NodeKey) => {
  if (props.expandedKeys === undefined) {
    internalExpandedKeys.value.delete(nodeKey)
  }
}

/**
 * Update node children (for lazy loading)
 */
const updateNodeChildren = (nodeKey: NodeKey, children: TreeNode[]) => {
  const updateInTree = (nodes: TreeNode[]): boolean => {
    for (const node of nodes) {
      if (getNodeKey.value(node) === nodeKey) {
        node.children = children
        return true
      }
      if (node.children && updateInTree(node.children)) {
        return true
      }
    }
    return false
  }
  updateInTree(props.data)
}

/**
 * Handle row click
 */
const handleRowClick = (node: FlattenedTreeNode, rowIndex: number, event: MouseEvent) => {
  // Update selection
  if (props.selectedKeys === undefined) {
    internalSelectedKeys.value.clear()
    internalSelectedKeys.value.add(node.id)
  }
  emit('update:selectedKeys', [node.id])
  
  emit('row-click', {
    node,
    rowIndex,
    event,
  })
}

/**
 * Handle keyboard navigation between rows
 */
const handleRowKeyDown = (node: FlattenedTreeNode, event: KeyboardEvent) => {
  if (!props.keyboardNavigation) return
  
  const currentIndex = visibleData.value.findIndex(n => n.id === node.id)
  
  switch (event.key) {
    case 'ArrowUp':
      if (currentIndex > 0) {
        event.preventDefault()
        focusRow(currentIndex - 1)
      }
      break
    case 'ArrowDown':
      if (currentIndex < visibleData.value.length - 1) {
        event.preventDefault()
        focusRow(currentIndex + 1)
      }
      break
    case 'Home':
      event.preventDefault()
      focusRow(0)
      break
    case 'End':
      event.preventDefault()
      focusRow(visibleData.value.length - 1)
      break
  }
}

/**
 * Focus a specific row
 */
const focusRow = (index: number) => {
  nextTick(() => {
    const rows = tableContainerRef.value?.querySelectorAll('.tree-table-row')
    if (rows && rows[index]) {
      (rows[index] as HTMLElement).focus()
    }
  })
}

/**
 * Expand all nodes
 * 展开所有节点
 * 
 * Validates: 22.2.5 - 支持全部展开
 */
const expandAll = () => {
  const allKeys = new Set<NodeKey>()
  
  const collectKeys = (nodes: TreeNode[]) => {
    for (const node of nodes) {
      const nodeKey = getNodeKey.value(node)
      if (node.children && node.children.length > 0) {
        allKeys.add(nodeKey)
        collectKeys(node.children)
      }
    }
  }
  
  collectKeys(props.data)
  
  if (props.expandedKeys === undefined) {
    internalExpandedKeys.value = allKeys
  }
  
  emit('update:expandedKeys', Array.from(allKeys))
}

/**
 * Collapse all nodes
 * 折叠所有节点
 * 
 * Validates: 22.2.5 - 支持全部折叠
 */
const collapseAll = () => {
  if (props.expandedKeys === undefined) {
    internalExpandedKeys.value.clear()
  }
  
  emit('update:expandedKeys', [])
}

/**
 * Expand to specific depth
 */
const expandToDepth = (depth: number) => {
  const keysToExpand = new Set<NodeKey>()
  
  const collectKeys = (nodes: TreeNode[], currentDepth: number) => {
    if (currentDepth >= depth) return
    
    for (const node of nodes) {
      const nodeKey = getNodeKey.value(node)
      if (node.children && node.children.length > 0) {
        keysToExpand.add(nodeKey)
        collectKeys(node.children, currentDepth + 1)
      }
    }
  }
  
  collectKeys(props.data, 0)
  
  if (props.expandedKeys === undefined) {
    internalExpandedKeys.value = keysToExpand
  }
  
  emit('update:expandedKeys', Array.from(keysToExpand))
}

// ============================================================================
// Initialization
// ============================================================================

/**
 * Initialize expanded state based on config
 */
const initializeExpandedState = () => {
  if (props.expandedKeys !== undefined) return
  
  const config = normalizedExpandConfig.value
  
  if (config.defaultExpandAll) {
    expandAll()
  } else if (config.defaultExpandDepth > 0) {
    expandToDepth(config.defaultExpandDepth)
  } else if (config.defaultExpandedKeys.length > 0) {
    internalExpandedKeys.value = new Set(config.defaultExpandedKeys)
  }
}

// ============================================================================
// Watchers
// ============================================================================

// Watch for data changes to reinitialize
watch(() => props.data, () => {
  // Keep existing expanded state, but remove keys for nodes that no longer exist
  const existingKeys = new Set<NodeKey>()
  
  const collectKeys = (nodes: TreeNode[]) => {
    for (const node of nodes) {
      existingKeys.add(getNodeKey.value(node))
      if (node.children) {
        collectKeys(node.children)
      }
    }
  }
  
  collectKeys(props.data)
  
  // Filter out non-existing keys
  for (const key of internalExpandedKeys.value) {
    if (!existingKeys.has(key)) {
      internalExpandedKeys.value.delete(key)
    }
  }
}, { deep: true })

// ============================================================================
// Lifecycle
// ============================================================================

onMounted(() => {
  initializeExpandedState()
})

// ============================================================================
// Expose
// ============================================================================

defineExpose({
  expandAll,
  collapseAll,
  expandToDepth,
  expandedKeys: effectiveExpandedKeys,
  flattenedData,
  visibleData,
})
</script>

<template>
  <div
    ref="tableContainerRef"
    :class="tableClasses"
    :style="tableContainerStyle"
    role="treegrid"
    :aria-busy="loading"
  >
    <!-- Loading Overlay -->
    <div v-if="loading" class="tree-table__loading">
      <div class="tree-table__loading-spinner" />
    </div>

    <!-- Table -->
    <table class="tree-table__table">
      <!-- Header -->
      <thead class="tree-table__header">
        <tr class="tree-table__header-row" :style="{ height: `${rowHeight}px` }">
          <th
            v-for="column in columns"
            :key="column.key"
            :class="[
              'tree-table__header-cell',
              column.headerClassName,
            ]"
            :style="{
              width: column.width && column.width !== 'auto' ? `${column.width}px` : undefined,
              minWidth: column.minWidth ? `${column.minWidth}px` : undefined,
              maxWidth: column.maxWidth ? `${column.maxWidth}px` : undefined,
              textAlign: column.align ?? 'left',
            }"
            role="columnheader"
          >
            {{ column.title }}
          </th>
        </tr>
      </thead>

      <!-- Body -->
      <tbody class="tree-table__body">
        <!-- Empty State -->
        <tr v-if="visibleData.length === 0 && !loading" class="tree-table__empty-row">
          <td :colspan="columns.length" class="tree-table__empty-cell">
            <slot name="empty">
              <div class="tree-table__empty-content">
                {{ emptyText }}
              </div>
            </slot>
          </td>
        </tr>

        <!-- Data Rows -->
        <TreeTableRow
          v-for="(node, index) in visibleData"
          :key="node.id"
          :node="node"
          :row-index="index"
          :columns="columns"
          :indent-config="normalizedIndentConfig"
          :expanded-keys="effectiveExpandedKeys"
          :loading-keys="loadingKeys"
          :size="size"
          :bordered="bordered"
          :selected-keys="effectiveSelectedKeys"
          :keyboard-navigation="keyboardNavigation"
          @expand="handleExpand"
          @collapse="handleCollapse"
          @row-click="handleRowClick"
          @row-keydown="handleRowKeyDown"
        />
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.tree-table {
  position: relative;
  overflow: auto;
  background-color: var(--tree-table-bg, #fff);
  border-radius: 4px;
}

.tree-table--bordered {
  border: 1px solid var(--tree-table-border-color, #e8e8e8);
}

.tree-table__table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

/* Header */
.tree-table__header {
  position: sticky;
  top: 0;
  z-index: 2;
  background-color: var(--tree-table-header-bg, #fafafa);
}

.tree-table__header-row {
  height: 48px;
}

.tree-table__header-cell {
  padding: 12px 16px;
  font-weight: 600;
  color: var(--tree-table-header-color, #262626);
  text-align: left;
  background-color: var(--tree-table-header-bg, #fafafa);
  border-bottom: 1px solid var(--tree-table-border-color, #e8e8e8);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tree-table--bordered .tree-table__header-cell {
  border-right: 1px solid var(--tree-table-border-color, #e8e8e8);
}

.tree-table--bordered .tree-table__header-cell:last-child {
  border-right: none;
}

/* Body */
.tree-table__body {
  background-color: var(--tree-table-body-bg, #fff);
}

/* Empty State */
.tree-table__empty-row {
  height: 100px;
}

.tree-table__empty-cell {
  text-align: center;
  color: var(--tree-table-empty-color, #999);
  padding: 32px;
}

.tree-table__empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

/* Loading */
.tree-table__loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.tree-table__loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #1890ff;
  border-radius: 50%;
  animation: tree-table-spin 1s linear infinite;
}

@keyframes tree-table-spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Size Variants */
.tree-table--small .tree-table__header-row {
  height: 36px;
}

.tree-table--small .tree-table__header-cell {
  padding: 8px 12px;
  font-size: 13px;
}

.tree-table--large .tree-table__header-row {
  height: 56px;
}

.tree-table--large .tree-table__header-cell {
  padding: 16px 20px;
  font-size: 15px;
}

/* Bordered cells */
.tree-table--bordered :deep(.tree-table-cell) {
  border-right: 1px solid var(--tree-table-border-color, #e8e8e8);
}

.tree-table--bordered :deep(.tree-table-cell:last-child) {
  border-right: none;
}
</style>

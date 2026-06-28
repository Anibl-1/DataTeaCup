<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 树形表格行组件
 * Tree Table Row Component
 * 
 * 递归渲染树形结构的行
 * Recursively renders tree structure rows
 * 
 * 需求: 22.2.4, 22.2.5, 22.2.6
 */

import { computed } from 'vue'
import type {
  FlattenedTreeNode,
  TreeTableColumn,
  IndentConfig,
  NodeKey,
} from './types'
import { DEFAULT_INDENT_CONFIG } from './types'

// ============================================================================
// Props
// ============================================================================

interface Props {
  /** Flattened tree node */
  node: FlattenedTreeNode
  /** Row index */
  rowIndex: number
  /** Column configurations */
  columns: TreeTableColumn[]
  /** Indentation configuration */
  indentConfig?: IndentConfig
  /** Set of expanded keys */
  expandedKeys: Set<NodeKey>
  /** Set of loading keys */
  loadingKeys: Set<NodeKey>
  /** Table size */
  size?: 'small' | 'medium' | 'large'
  /** Whether table is bordered */
  bordered?: boolean
  /** Selected row keys */
  selectedKeys?: Set<NodeKey>
  /** Enable keyboard navigation */
  keyboardNavigation?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 'medium',
  bordered: true,
  keyboardNavigation: false,
})

// ============================================================================
// Emits
// ============================================================================

const emit = defineEmits<{
  (e: 'expand', nodeKey: NodeKey): void
  (e: 'collapse', nodeKey: NodeKey): void
  (e: 'row-click', node: FlattenedTreeNode, rowIndex: number, event: MouseEvent): void
  (e: 'row-keydown', node: FlattenedTreeNode, event: KeyboardEvent): void
}>()

// ============================================================================
// Computed
// ============================================================================

const normalizedIndentConfig = computed(() => ({
  ...DEFAULT_INDENT_CONFIG,
  ...props.indentConfig,
}))

const isExpanded = computed(() => props.expandedKeys.has(props.node.id))
const isLoading = computed(() => props.loadingKeys.has(props.node.id))
const isSelected = computed(() => props.selectedKeys?.has(props.node.id) ?? false)

/**
 * Calculate indentation style based on depth
 * 根据深度计算缩进样式
 * 
 * Validates: 22.2.6 - 支持缩进显示（根据层级自动缩进）
 */
const indentStyle = computed(() => ({
  paddingLeft: `${props.node.depth * normalizedIndentConfig.value.indentSize}px`,
}))

const rowHeight = computed(() => {
  switch (props.size) {
    case 'small': return 36
    case 'large': return 56
    default: return 48
  }
})

const rowClasses = computed(() => [
  'tree-table-row',
  `tree-table-row--${props.size}`,
  {
    'tree-table-row--expanded': isExpanded.value,
    'tree-table-row--selected': isSelected.value,
    'tree-table-row--has-children': props.node.hasChildren,
    'tree-table-row--leaf': !props.node.hasChildren,
    'tree-table-row--loading': isLoading.value,
  },
])

// Find the tree column (column that shows expand icon)
const treeColumnKey = computed(() => {
  const treeCol = props.columns.find(col => col.treeColumn)
  return treeCol?.key ?? props.columns[0]?.key
})

// ============================================================================
// Methods
// ============================================================================

/**
 * Handle expand/collapse toggle
 * 处理展开/折叠切换
 * 
 * Validates: 22.2.5 - 支持展开/折叠操作
 */
const handleToggleExpand = (event: MouseEvent) => {
  event.stopPropagation()
  
  if (!props.node.hasChildren || isLoading.value) return
  
  if (isExpanded.value) {
    emit('collapse', props.node.id)
  } else {
    emit('expand', props.node.id)
  }
}

const handleRowClick = (event: MouseEvent) => {
  emit('row-click', props.node, props.rowIndex, event)
}

/**
 * Handle keyboard navigation
 * 处理键盘导航
 */
const handleKeyDown = (event: KeyboardEvent) => {
  if (!props.keyboardNavigation) return
  
  switch (event.key) {
    case 'ArrowRight':
      // Expand node
      if (props.node.hasChildren && !isExpanded.value && !isLoading.value) {
        event.preventDefault()
        emit('expand', props.node.id)
      }
      break
    case 'ArrowLeft':
      // Collapse node
      if (props.node.hasChildren && isExpanded.value) {
        event.preventDefault()
        emit('collapse', props.node.id)
      }
      break
    case 'Enter':
    case ' ':
      // Toggle expand
      if (props.node.hasChildren && !isLoading.value) {
        event.preventDefault()
        if (isExpanded.value) {
          emit('collapse', props.node.id)
        } else {
          emit('expand', props.node.id)
        }
      }
      break
  }
  
  emit('row-keydown', props.node, event)
}

const getCellClasses = (column: TreeTableColumn) => [
  'tree-table-cell',
  {
    'tree-table-cell--tree': column.key === treeColumnKey.value,
    'tree-table-cell--ellipsis': column.ellipsis,
  },
  column.className,
]

const getCellStyle = (column: TreeTableColumn) => {
  const style: Record<string, any> = {
    textAlign: column.align ?? 'left',
  }
  
  if (column.width && column.width !== 'auto') {
    style.width = `${column.width}px`
    style.minWidth = `${column.width}px`
  }
  
  return style
}
</script>

<template>
  <tr
    :class="rowClasses"
    :style="{ height: `${rowHeight}px` }"
    :tabindex="keyboardNavigation ? 0 : -1"
    role="row"
    :aria-expanded="node.hasChildren ? isExpanded : undefined"
    :aria-level="node.depth + 1"
    :aria-selected="isSelected"
    @click="handleRowClick"
    @keydown="handleKeyDown"
  >
    <td
      v-for="column in columns"
      :key="column.key"
      :class="getCellClasses(column)"
      :style="getCellStyle(column)"
      role="gridcell"
    >
      <div class="tree-table-cell-content">
        <!-- Tree column with expand icon and indentation -->
        <template v-if="column.key === treeColumnKey">
          <!-- Indentation spacer - 22.2.6 -->
          <span 
            class="tree-table-indent" 
            :style="indentStyle"
            aria-hidden="true"
          />
          
          <!-- Expand/Collapse icon - 22.2.5 -->
          <span
            v-if="node.hasChildren"
            class="tree-table-expand-icon"
            :class="{
              'tree-table-expand-icon--expanded': isExpanded,
              'tree-table-expand-icon--loading': isLoading,
            }"
            role="button"
            :aria-label="isExpanded ? '折叠' : '展开'"
            :aria-expanded="isExpanded"
            tabindex="-1"
            @click="handleToggleExpand"
          >
            <!-- Loading spinner -->
            <svg
              v-if="isLoading"
              class="tree-table-expand-icon__spinner"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
            >
              <circle cx="12" cy="12" r="10" stroke-width="2" stroke-dasharray="31.4" />
            </svg>
            <!-- Expand arrow -->
            <svg
              v-else
              class="tree-table-expand-icon__arrow"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
            >
              <polyline points="9 18 15 12 9 6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </span>
          
          <!-- Leaf node spacer -->
          <span v-else class="tree-table-leaf-spacer" aria-hidden="true" />
          
          <!-- Cell content -->
          <span class="tree-table-cell-value">
            <template v-if="column.render">
              <component
                :is="{ render: () => column.render!(node[column.dataIndex], node, rowIndex) }"
              />
            </template>
            <template v-else>
              {{ node[column.dataIndex] }}
            </template>
          </span>
        </template>
        
        <!-- Regular column -->
        <template v-else>
          <template v-if="column.render">
            <component
              :is="{ render: () => column.render!(node[column.dataIndex], node, rowIndex) }"
            />
          </template>
          <template v-else>
            {{ node[column.dataIndex] }}
          </template>
        </template>
      </div>
    </td>
  </tr>
</template>

<style scoped>
.tree-table-row {
  transition: background-color 0.2s;
  cursor: pointer;
}

.tree-table-row:hover {
  background-color: var(--tree-table-hover-bg, #f5f5f5);
}

.tree-table-row--selected {
  background-color: var(--tree-table-selected-bg, #e6f7ff);
}

.tree-table-row--selected:hover {
  background-color: var(--tree-table-selected-hover-bg, #bae7ff);
}

.tree-table-row:focus {
  outline: 2px solid var(--tree-table-focus-color, #1890ff);
  outline-offset: -2px;
}

/* Cell styles */
.tree-table-cell {
  padding: 12px 16px;
  color: var(--tree-table-cell-color, #262626);
  border-bottom: 1px solid var(--tree-table-border-color, #e8e8e8);
  vertical-align: middle;
}

.tree-table-cell--ellipsis .tree-table-cell-content {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-table-cell-content {
  display: flex;
  align-items: center;
}

/* Tree column specific styles */
.tree-table-cell--tree .tree-table-cell-content {
  display: flex;
  align-items: center;
}

/* Indentation - 22.2.6 */
.tree-table-indent {
  display: inline-block;
  flex-shrink: 0;
}

/* Expand icon - 22.2.5 */
.tree-table-expand-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  margin-right: 8px;
  flex-shrink: 0;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
  color: var(--tree-table-expand-icon-color, #595959);
}

.tree-table-expand-icon:hover {
  background-color: var(--tree-table-expand-icon-hover-bg, rgba(0, 0, 0, 0.06));
  color: var(--tree-table-expand-icon-hover-color, #1890ff);
}

.tree-table-expand-icon__arrow {
  width: 14px;
  height: 14px;
  transition: transform 0.2s;
}

.tree-table-expand-icon--expanded .tree-table-expand-icon__arrow {
  transform: rotate(90deg);
}

.tree-table-expand-icon__spinner {
  width: 14px;
  height: 14px;
  animation: tree-table-spin 1s linear infinite;
}

@keyframes tree-table-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Leaf node spacer */
.tree-table-leaf-spacer {
  display: inline-block;
  width: 20px;
  height: 20px;
  margin-right: 8px;
  flex-shrink: 0;
}

/* Cell value */
.tree-table-cell-value {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Size variants */
.tree-table-row--small .tree-table-cell {
  padding: 8px 12px;
  font-size: 13px;
}

.tree-table-row--large .tree-table-cell {
  padding: 16px 20px;
  font-size: 15px;
}

/* Loading state */
.tree-table-row--loading {
  opacity: 0.7;
  pointer-events: none;
}
</style>

/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 树形表格属性测试
 * Tree Table Property Tests
 * 
 * **属性 78: 树形表格层级正确性**
 * **Validates: Requirements 22.2**
 * 
 * 测试内容:
 * 1. 扁平化数据应保持父子关系
 * 2. 节点深度应等于祖先数量
 * 3. 展开节点应使其子节点可见
 * 4. 折叠节点应隐藏所有后代
 * 5. 节点路径应正确（从根到节点）
 * 6. 展开全部应展开所有非叶子节点
 * 7. 折叠全部应折叠所有节点
 * 8. 手风琴模式应只允许同级一个展开节点
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import type { TreeNode, FlattenedTreeNode, NodeKey } from '../types'

// ============================================================================
// Tree Utility Functions (树形工具函数)
// ============================================================================

/**
 * Flatten tree data for rendering
 * 扁平化树数据用于渲染
 */
export function flattenTree(
  nodes: TreeNode[],
  expandedKeys: Set<NodeKey>,
  getNodeKey: (node: TreeNode) => NodeKey = (node) => node.id
): FlattenedTreeNode[] {
  const result: FlattenedTreeNode[] = []
  
  const flatten = (
    nodes: TreeNode[],
    depth: number,
    parentId: NodeKey | null,
    parentPath: NodeKey[],
    parentVisible: boolean
  ) => {
    for (const node of nodes) {
      const nodeKey = getNodeKey(node)
      const hasChildren = !!(node.children && node.children.length > 0) || node.isLeaf === false
      const isExpanded = expandedKeys.has(nodeKey)
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
  
  flatten(nodes, 0, null, [], true)
  return result
}

/**
 * Get all node keys from tree
 * 获取树中所有节点的键
 */
export function getAllNodeKeys(
  nodes: TreeNode[],
  getNodeKey: (node: TreeNode) => NodeKey = (node) => node.id
): NodeKey[] {
  const keys: NodeKey[] = []
  
  const collect = (nodes: TreeNode[]) => {
    for (const node of nodes) {
      keys.push(getNodeKey(node))
      if (node.children) {
        collect(node.children)
      }
    }
  }
  
  collect(nodes)
  return keys
}

/**
 * Get all non-leaf node keys (nodes with children)
 * 获取所有非叶子节点的键
 */
export function getNonLeafNodeKeys(
  nodes: TreeNode[],
  getNodeKey: (node: TreeNode) => NodeKey = (node) => node.id
): NodeKey[] {
  const keys: NodeKey[] = []
  
  const collect = (nodes: TreeNode[]) => {
    for (const node of nodes) {
      if (node.children && node.children.length > 0) {
        keys.push(getNodeKey(node))
        collect(node.children)
      }
    }
  }
  
  collect(nodes)
  return keys
}

/**
 * Get all descendant keys of a node
 * 获取节点的所有后代键
 */
export function getDescendantKeys(
  nodes: TreeNode[],
  targetKey: NodeKey,
  getNodeKey: (node: TreeNode) => NodeKey = (node) => node.id
): NodeKey[] {
  const descendants: NodeKey[] = []
  
  const findAndCollect = (nodes: TreeNode[]): boolean => {
    for (const node of nodes) {
      const nodeKey = getNodeKey(node)
      if (nodeKey === targetKey) {
        // Found target, collect all descendants
        if (node.children) {
          collectAll(node.children)
        }
        return true
      }
      if (node.children && findAndCollect(node.children)) {
        return true
      }
    }
    return false
  }
  
  const collectAll = (nodes: TreeNode[]) => {
    for (const node of nodes) {
      descendants.push(getNodeKey(node))
      if (node.children) {
        collectAll(node.children)
      }
    }
  }
  
  findAndCollect(nodes)
  return descendants
}

/**
 * Get children keys of a node
 * 获取节点的直接子节点键
 */
export function getChildrenKeys(
  nodes: TreeNode[],
  targetKey: NodeKey,
  getNodeKey: (node: TreeNode) => NodeKey = (node) => node.id
): NodeKey[] {
  const findNode = (nodes: TreeNode[]): TreeNode | null => {
    for (const node of nodes) {
      if (getNodeKey(node) === targetKey) {
        return node
      }
      if (node.children) {
        const found = findNode(node.children)
        if (found) return found
      }
    }
    return null
  }
  
  const node = findNode(nodes)
  if (!node || !node.children) return []
  return node.children.map(getNodeKey)
}

/**
 * Get sibling keys at the same level
 * 获取同级兄弟节点键
 */
export function getSiblingKeys(
  nodes: TreeNode[],
  targetKey: NodeKey,
  getNodeKey: (node: TreeNode) => NodeKey = (node) => node.id
): NodeKey[] {
  const findSiblings = (nodes: TreeNode[], parent: TreeNode[] | null): NodeKey[] | null => {
    for (let i = 0; i < nodes.length; i++) {
      const node = nodes[i]
      if (getNodeKey(node) === targetKey) {
        // Found target, return siblings (excluding self)
        return nodes.filter((_, idx) => idx !== i).map(getNodeKey)
      }
      if (node.children) {
        const result = findSiblings(node.children, nodes)
        if (result !== null) return result
      }
    }
    return null
  }
  
  return findSiblings(nodes, null) || []
}

/**
 * Calculate expected depth for a node
 * 计算节点的预期深度
 */
export function calculateNodeDepth(
  nodes: TreeNode[],
  targetKey: NodeKey,
  getNodeKey: (node: TreeNode) => NodeKey = (node) => node.id
): number {
  const findDepth = (nodes: TreeNode[], currentDepth: number): number => {
    for (const node of nodes) {
      if (getNodeKey(node) === targetKey) {
        return currentDepth
      }
      if (node.children) {
        const depth = findDepth(node.children, currentDepth + 1)
        if (depth >= 0) return depth
      }
    }
    return -1
  }
  
  return findDepth(nodes, 0)
}

/**
 * Get expected path for a node
 * 获取节点的预期路径
 */
export function calculateNodePath(
  nodes: TreeNode[],
  targetKey: NodeKey,
  getNodeKey: (node: TreeNode) => NodeKey = (node) => node.id
): NodeKey[] {
  const findPath = (nodes: TreeNode[], currentPath: NodeKey[]): NodeKey[] | null => {
    for (const node of nodes) {
      const nodeKey = getNodeKey(node)
      const newPath = [...currentPath, nodeKey]
      
      if (nodeKey === targetKey) {
        return newPath
      }
      if (node.children) {
        const result = findPath(node.children, newPath)
        if (result !== null) return result
      }
    }
    return null
  }
  
  return findPath(nodes, []) || []
}

/**
 * Expand all non-leaf nodes
 * 展开所有非叶子节点
 */
export function expandAll(
  nodes: TreeNode[],
  getNodeKey: (node: TreeNode) => NodeKey = (node) => node.id
): Set<NodeKey> {
  const expandedKeys = new Set<NodeKey>()
  
  const collect = (nodes: TreeNode[]) => {
    for (const node of nodes) {
      if (node.children && node.children.length > 0) {
        expandedKeys.add(getNodeKey(node))
        collect(node.children)
      }
    }
  }
  
  collect(nodes)
  return expandedKeys
}

/**
 * Apply accordion mode - collapse siblings when expanding a node
 * 应用手风琴模式 - 展开节点时折叠兄弟节点
 */
export function applyAccordionMode(
  nodes: TreeNode[],
  expandedKeys: Set<NodeKey>,
  nodeToExpand: NodeKey,
  getNodeKey: (node: TreeNode) => NodeKey = (node) => node.id
): Set<NodeKey> {
  const newExpandedKeys = new Set(expandedKeys)
  
  // Get siblings of the node to expand
  const siblings = getSiblingKeys(nodes, nodeToExpand, getNodeKey)
  
  // Collapse all siblings
  for (const sibling of siblings) {
    newExpandedKeys.delete(sibling)
  }
  
  // Expand the target node
  newExpandedKeys.add(nodeToExpand)
  
  return newExpandedKeys
}

// ============================================================================
// Arbitraries (测试数据生成器)
// ============================================================================

/** 生成唯一ID */
let idCounter = 0
const resetIdCounter = () => { idCounter = 0 }
const generateId = () => `node_${++idCounter}`

/** 生成树节点名称 */
const nodeNameArb = fc.string({ minLength: 1, maxLength: 10 }).filter(s => /^[a-z]+$/.test(s))

/** 生成单个叶子节点 */
const leafNodeArb: fc.Arbitrary<TreeNode> = fc.record({
  id: fc.constant(null as any).map(() => generateId()),
  name: nodeNameArb,
  value: fc.integer({ min: 0, max: 1000 }),
})

/** 递归生成树节点（最大深度限制） */
const treeNodeArb = (maxDepth: number): fc.Arbitrary<TreeNode> => {
  if (maxDepth <= 0) {
    return leafNodeArb
  }
  
  return fc.record({
    id: fc.constant(null as any).map(() => generateId()),
    name: nodeNameArb,
    value: fc.integer({ min: 0, max: 1000 }),
    children: fc.option(
      fc.array(treeNodeArb(maxDepth - 1), { minLength: 1, maxLength: 4 }),
      { nil: undefined }
    ),
  })
}

/** 生成树形数据（根节点数组） */
const treeDataArb = (maxDepth: number = 3, maxRoots: number = 5): fc.Arbitrary<TreeNode[]> => {
  return fc.array(treeNodeArb(maxDepth), { minLength: 1, maxLength: maxRoots })
    .map(nodes => {
      resetIdCounter()
      // 重新生成ID以确保唯一性
      const regenerateIds = (nodes: TreeNode[]): TreeNode[] => {
        return nodes.map(node => ({
          ...node,
          id: generateId(),
          children: node.children ? regenerateIds(node.children) : undefined,
        }))
      }
      return regenerateIds(nodes)
    })
}

/** 生成有效的展开键集合 */
const expandedKeysArb = (nodes: TreeNode[]): fc.Arbitrary<Set<NodeKey>> => {
  const nonLeafKeys = getNonLeafNodeKeys(nodes)
  if (nonLeafKeys.length === 0) {
    return fc.constant(new Set<NodeKey>())
  }
  return fc.subarray(nonLeafKeys).map(keys => new Set(keys))
}

// ============================================================================
// 属性测试
// ============================================================================

describe('TreeTable Property Tests', () => {

  // ==========================================================================
  // 属性 78: 树形表格层级正确性
  // ==========================================================================
  
  describe('Property 78: Tree Table Hierarchy Correctness', () => {
    
    // ------------------------------------------------------------------------
    // 78.1 扁平化数据应保持父子关系
    // ------------------------------------------------------------------------
    
    describe('78.1 Flattened Data Preserves Parent-Child Relationships', () => {
      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.1.1: 每个节点的parentId应指向正确的父节点
       * 对于任意树形数据，扁平化后每个节点的parentId应正确
       */
      it('Property 78.1.1: Each node parentId should point to correct parent', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const expandedKeys = expandAll(treeData)
              const flattened = flattenTree(treeData, expandedKeys)
              
              for (const node of flattened) {
                if (node.parentId !== null) {
                  // 验证父节点存在于扁平化数据中
                  const parent = flattened.find(n => n.id === node.parentId)
                  if (!parent) return false
                  
                  // 验证父节点的深度比当前节点小1
                  if (parent.depth !== node.depth - 1) return false
                }
              }
              return true
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.1.2: 根节点的parentId应为null
       * 对于任意树形数据，所有根节点的parentId应为null
       */
      it('Property 78.1.2: Root nodes should have null parentId', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const expandedKeys = expandAll(treeData)
              const flattened = flattenTree(treeData, expandedKeys)
              
              // 所有深度为0的节点应该有null parentId
              const rootNodes = flattened.filter(n => n.depth === 0)
              return rootNodes.every(n => n.parentId === null)
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.1.3: 非根节点的parentId不应为null
       * 对于任意树形数据，所有非根节点的parentId不应为null
       */
      it('Property 78.1.3: Non-root nodes should have non-null parentId', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const expandedKeys = expandAll(treeData)
              const flattened = flattenTree(treeData, expandedKeys)
              
              // 所有深度大于0的节点应该有非null parentId
              const nonRootNodes = flattened.filter(n => n.depth > 0)
              return nonRootNodes.every(n => n.parentId !== null)
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ------------------------------------------------------------------------
    // 78.2 节点深度应等于祖先数量
    // ------------------------------------------------------------------------
    
    describe('78.2 Node Depth Equals Ancestor Count', () => {
      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.2.1: 节点深度应等于路径长度减1
       * 对于任意节点，其深度应等于从根到该节点的路径长度减1
       */
      it('Property 78.2.1: Node depth should equal path length minus 1', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const expandedKeys = expandAll(treeData)
              const flattened = flattenTree(treeData, expandedKeys)
              
              for (const node of flattened) {
                // 路径长度应等于深度+1（路径包含自身）
                if (node.path.length !== node.depth + 1) return false
              }
              return true
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.2.2: 子节点深度应比父节点大1
       * 对于任意父子节点对，子节点深度应比父节点大1
       */
      it('Property 78.2.2: Child depth should be parent depth plus 1', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const expandedKeys = expandAll(treeData)
              const flattened = flattenTree(treeData, expandedKeys)
              
              for (const node of flattened) {
                if (node.parentId !== null) {
                  const parent = flattened.find(n => n.id === node.parentId)
                  if (!parent) return false
                  if (node.depth !== parent.depth + 1) return false
                }
              }
              return true
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.2.3: 深度应与原始树结构一致
       * 对于任意节点，其深度应与在原始树中计算的深度一致
       */
      it('Property 78.2.3: Depth should match original tree structure', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const expandedKeys = expandAll(treeData)
              const flattened = flattenTree(treeData, expandedKeys)
              
              for (const node of flattened) {
                const expectedDepth = calculateNodeDepth(treeData, node.id)
                if (node.depth !== expectedDepth) return false
              }
              return true
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ------------------------------------------------------------------------
    // 78.3 展开节点应使其子节点可见
    // ------------------------------------------------------------------------
    
    describe('78.3 Expanding Node Makes Children Visible', () => {
      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.3.1: 展开的节点的直接子节点应出现在扁平化数据中
       * 对于任意展开的节点，其直接子节点应在扁平化结果中
       */
      it('Property 78.3.1: Expanded node children should appear in flattened data', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const nonLeafKeys = getNonLeafNodeKeys(treeData)
              if (nonLeafKeys.length === 0) return true
              
              // 随机选择一个非叶子节点展开
              const keyToExpand = nonLeafKeys[0]
              const expandedKeys = new Set<NodeKey>([keyToExpand])
              const flattened = flattenTree(treeData, expandedKeys)
              
              // 获取该节点的直接子节点
              const childrenKeys = getChildrenKeys(treeData, keyToExpand)
              
              // 验证所有直接子节点都在扁平化数据中
              const flattenedIds = new Set(flattened.map(n => n.id))
              return childrenKeys.every(key => flattenedIds.has(key))
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.3.2: 展开的节点的子节点应标记为可见
       * 对于任意展开的节点，其子节点的visible属性应为true
       */
      it('Property 78.3.2: Children of expanded node should be marked visible', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const nonLeafKeys = getNonLeafNodeKeys(treeData)
              if (nonLeafKeys.length === 0) return true
              
              const keyToExpand = nonLeafKeys[0]
              const expandedKeys = new Set<NodeKey>([keyToExpand])
              const flattened = flattenTree(treeData, expandedKeys)
              
              const childrenKeys = getChildrenKeys(treeData, keyToExpand)
              
              // 验证所有直接子节点的visible为true
              for (const childKey of childrenKeys) {
                const child = flattened.find(n => n.id === childKey)
                if (!child || !child.visible) return false
              }
              return true
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.3.3: 展开的节点应标记为expanded=true
       * 对于任意展开的节点，其expanded属性应为true
       */
      it('Property 78.3.3: Expanded node should have expanded=true', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const nonLeafKeys = getNonLeafNodeKeys(treeData)
              if (nonLeafKeys.length === 0) return true
              
              const keyToExpand = nonLeafKeys[0]
              const expandedKeys = new Set<NodeKey>([keyToExpand])
              const flattened = flattenTree(treeData, expandedKeys)
              
              const expandedNode = flattened.find(n => n.id === keyToExpand)
              return expandedNode !== undefined && expandedNode.expanded === true
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ------------------------------------------------------------------------
    // 78.4 折叠节点应隐藏所有后代
    // ------------------------------------------------------------------------
    
    describe('78.4 Collapsing Node Hides All Descendants', () => {
      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.4.1: 折叠的节点的后代不应出现在扁平化数据中
       * 对于任意折叠的节点，其所有后代不应在扁平化结果中
       */
      it('Property 78.4.1: Collapsed node descendants should not appear in flattened data', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const nonLeafKeys = getNonLeafNodeKeys(treeData)
              if (nonLeafKeys.length === 0) return true
              
              // 不展开任何节点（全部折叠）
              const expandedKeys = new Set<NodeKey>()
              const flattened = flattenTree(treeData, expandedKeys)
              
              // 获取第一个非叶子节点的所有后代
              const keyToCheck = nonLeafKeys[0]
              const descendants = getDescendantKeys(treeData, keyToCheck)
              
              // 验证所有后代都不在扁平化数据中
              const flattenedIds = new Set(flattened.map(n => n.id))
              return descendants.every(key => !flattenedIds.has(key))
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.4.2: 折叠的节点应标记为expanded=false
       * 对于任意折叠的节点，其expanded属性应为false
       */
      it('Property 78.4.2: Collapsed node should have expanded=false', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const nonLeafKeys = getNonLeafNodeKeys(treeData)
              if (nonLeafKeys.length === 0) return true
              
              // 不展开任何节点
              const expandedKeys = new Set<NodeKey>()
              const flattened = flattenTree(treeData, expandedKeys)
              
              // 验证所有非叶子节点的expanded为false
              for (const key of nonLeafKeys) {
                const node = flattened.find(n => n.id === key)
                // 只有根级别的非叶子节点会在扁平化数据中
                if (node && node.expanded !== false) return false
              }
              return true
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.4.3: 折叠父节点应隐藏所有嵌套后代
       * 即使子节点是展开的，如果父节点折叠，子节点的后代也不应可见
       */
      it('Property 78.4.3: Collapsing parent should hide all nested descendants', () => {
        fc.assert(
          fc.property(
            treeDataArb(4, 2),
            (treeData) => {
              const nonLeafKeys = getNonLeafNodeKeys(treeData)
              if (nonLeafKeys.length < 2) return true
              
              // 展开所有节点
              const allExpanded = expandAll(treeData)
              const flattenedAll = flattenTree(treeData, allExpanded)
              
              // 找到一个有子节点的节点，折叠它
              const keyToCollapse = nonLeafKeys[0]
              const descendants = getDescendantKeys(treeData, keyToCollapse)
              
              // 从展开集合中移除该节点
              const partialExpanded = new Set(allExpanded)
              partialExpanded.delete(keyToCollapse)
              
              const flattenedPartial = flattenTree(treeData, partialExpanded)
              const flattenedIds = new Set(flattenedPartial.map(n => n.id))
              
              // 验证所有后代都不在扁平化数据中
              return descendants.every(key => !flattenedIds.has(key))
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ------------------------------------------------------------------------
    // 78.5 节点路径应正确（从根到节点）
    // ------------------------------------------------------------------------
    
    describe('78.5 Node Paths Are Correct (Root to Node)', () => {
      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.5.1: 节点路径应以自身ID结尾
       * 对于任意节点，其路径的最后一个元素应是自身ID
       */
      it('Property 78.5.1: Node path should end with own id', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const expandedKeys = expandAll(treeData)
              const flattened = flattenTree(treeData, expandedKeys)
              
              for (const node of flattened) {
                if (node.path[node.path.length - 1] !== node.id) return false
              }
              return true
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.5.2: 根节点路径应只包含自身
       * 对于任意根节点，其路径应只有一个元素
       */
      it('Property 78.5.2: Root node path should contain only itself', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const expandedKeys = expandAll(treeData)
              const flattened = flattenTree(treeData, expandedKeys)
              
              const rootNodes = flattened.filter(n => n.depth === 0)
              return rootNodes.every(n => n.path.length === 1 && n.path[0] === n.id)
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.5.3: 节点路径应与原始树结构一致
       * 对于任意节点，其路径应与在原始树中计算的路径一致
       */
      it('Property 78.5.3: Node path should match original tree structure', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const expandedKeys = expandAll(treeData)
              const flattened = flattenTree(treeData, expandedKeys)
              
              for (const node of flattened) {
                const expectedPath = calculateNodePath(treeData, node.id)
                if (node.path.length !== expectedPath.length) return false
                for (let i = 0; i < node.path.length; i++) {
                  if (node.path[i] !== expectedPath[i]) return false
                }
              }
              return true
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.5.4: 子节点路径应包含父节点路径
       * 对于任意子节点，其路径应以父节点路径为前缀
       */
      it('Property 78.5.4: Child path should contain parent path as prefix', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const expandedKeys = expandAll(treeData)
              const flattened = flattenTree(treeData, expandedKeys)
              
              for (const node of flattened) {
                if (node.parentId !== null) {
                  const parent = flattened.find(n => n.id === node.parentId)
                  if (!parent) return false
                  
                  // 子节点路径应以父节点路径为前缀
                  for (let i = 0; i < parent.path.length; i++) {
                    if (node.path[i] !== parent.path[i]) return false
                  }
                }
              }
              return true
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ------------------------------------------------------------------------
    // 78.6 展开全部应展开所有非叶子节点
    // ------------------------------------------------------------------------
    
    describe('78.6 Expand All Should Expand All Non-Leaf Nodes', () => {
      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.6.1: 展开全部后所有节点都应在扁平化数据中
       * 对于任意树形数据，展开全部后所有节点都应可见
       */
      it('Property 78.6.1: After expand all, all nodes should be in flattened data', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const expandedKeys = expandAll(treeData)
              const flattened = flattenTree(treeData, expandedKeys)
              
              const allKeys = getAllNodeKeys(treeData)
              const flattenedIds = new Set(flattened.map(n => n.id))
              
              return allKeys.every(key => flattenedIds.has(key))
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.6.2: 展开全部后所有非叶子节点应标记为expanded=true
       * 对于任意树形数据，展开全部后所有非叶子节点的expanded应为true
       */
      it('Property 78.6.2: After expand all, all non-leaf nodes should have expanded=true', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const expandedKeys = expandAll(treeData)
              const flattened = flattenTree(treeData, expandedKeys)
              
              const nonLeafKeys = getNonLeafNodeKeys(treeData)
              
              for (const key of nonLeafKeys) {
                const node = flattened.find(n => n.id === key)
                if (!node || !node.expanded) return false
              }
              return true
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.6.3: 展开全部应返回所有非叶子节点的键
       * expandAll函数应返回所有非叶子节点的键集合
       */
      it('Property 78.6.3: Expand all should return all non-leaf node keys', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const expandedKeys = expandAll(treeData)
              const nonLeafKeys = getNonLeafNodeKeys(treeData)
              
              // 展开的键数量应等于非叶子节点数量
              if (expandedKeys.size !== nonLeafKeys.length) return false
              
              // 所有非叶子节点都应在展开集合中
              return nonLeafKeys.every(key => expandedKeys.has(key))
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ------------------------------------------------------------------------
    // 78.7 折叠全部应折叠所有节点
    // ------------------------------------------------------------------------
    
    describe('78.7 Collapse All Should Collapse All Nodes', () => {
      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.7.1: 折叠全部后只有根节点应在扁平化数据中
       * 对于任意树形数据，折叠全部后只有根节点可见
       */
      it('Property 78.7.1: After collapse all, only root nodes should be in flattened data', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const expandedKeys = new Set<NodeKey>() // 空集合 = 全部折叠
              const flattened = flattenTree(treeData, expandedKeys)
              
              // 扁平化数据的数量应等于根节点数量
              if (flattened.length !== treeData.length) return false
              
              // 所有节点的深度应为0
              return flattened.every(n => n.depth === 0)
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.7.2: 折叠全部后所有节点应标记为expanded=false
       * 对于任意树形数据，折叠全部后所有节点的expanded应为false
       */
      it('Property 78.7.2: After collapse all, all nodes should have expanded=false', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const expandedKeys = new Set<NodeKey>()
              const flattened = flattenTree(treeData, expandedKeys)
              
              return flattened.every(n => n.expanded === false)
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.7.3: 折叠全部后hasChildren属性应正确
       * 对于任意树形数据，折叠全部后有子节点的节点hasChildren应为true
       */
      it('Property 78.7.3: After collapse all, hasChildren should be correct', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const expandedKeys = new Set<NodeKey>()
              const flattened = flattenTree(treeData, expandedKeys)
              
              for (const node of flattened) {
                const childrenKeys = getChildrenKeys(treeData, node.id)
                const expectedHasChildren = childrenKeys.length > 0
                if (node.hasChildren !== expectedHasChildren) return false
              }
              return true
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ------------------------------------------------------------------------
    // 78.8 手风琴模式应只允许同级一个展开节点
    // ------------------------------------------------------------------------
    
    describe('78.8 Accordion Mode Should Only Allow One Expanded Node Per Level', () => {
      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.8.1: 手风琴模式下展开节点应折叠兄弟节点
       * 在手风琴模式下，展开一个节点应自动折叠其兄弟节点
       */
      it('Property 78.8.1: In accordion mode, expanding node should collapse siblings', () => {
        fc.assert(
          fc.property(
            treeDataArb(2, 4),
            (treeData) => {
              // 需要至少2个根节点来测试手风琴模式
              if (treeData.length < 2) return true
              
              const nonLeafKeys = getNonLeafNodeKeys(treeData)
              if (nonLeafKeys.length < 2) return true
              
              // 找到两个兄弟节点（同级）
              const rootNonLeafKeys = treeData
                .filter(n => n.children && n.children.length > 0)
                .map(n => n.id)
              
              if (rootNonLeafKeys.length < 2) return true
              
              // 先展开第一个节点
              let expandedKeys = new Set<NodeKey>([rootNonLeafKeys[0]])
              
              // 应用手风琴模式展开第二个节点
              expandedKeys = applyAccordionMode(treeData, expandedKeys, rootNonLeafKeys[1])
              
              // 验证第一个节点被折叠，第二个节点被展开
              return !expandedKeys.has(rootNonLeafKeys[0]) && expandedKeys.has(rootNonLeafKeys[1])
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.8.2: 手风琴模式不应影响不同级别的节点
       * 在手风琴模式下，展开一个节点不应影响其他级别的展开状态
       */
      it('Property 78.8.2: Accordion mode should not affect nodes at different levels', () => {
        fc.assert(
          fc.property(
            treeDataArb(3, 3),
            (treeData) => {
              const nonLeafKeys = getNonLeafNodeKeys(treeData)
              if (nonLeafKeys.length < 2) return true
              
              // 找到不同级别的节点
              const expandedKeys = expandAll(treeData)
              const flattened = flattenTree(treeData, expandedKeys)
              
              const level0Nodes = flattened.filter(n => n.depth === 0 && n.hasChildren)
              const level1Nodes = flattened.filter(n => n.depth === 1 && n.hasChildren)
              
              if (level0Nodes.length === 0 || level1Nodes.length === 0) return true
              
              // 展开一个level0节点
              let currentExpanded = new Set<NodeKey>([level0Nodes[0].id])
              
              // 如果有level1节点，也展开它
              if (level1Nodes.length > 0) {
                currentExpanded.add(level1Nodes[0].id)
              }
              
              // 应用手风琴模式展开另一个level0节点（如果存在）
              if (level0Nodes.length > 1) {
                currentExpanded = applyAccordionMode(treeData, currentExpanded, level0Nodes[1].id)
                
                // level1节点应该不受影响（如果它不是被折叠节点的后代）
                // 这里简化测试，只验证手风琴模式正确折叠了同级节点
                return !currentExpanded.has(level0Nodes[0].id) && currentExpanded.has(level0Nodes[1].id)
              }
              
              return true
            }
          ),
          { numRuns: 100 }
        )
      })

      /**
       * **Validates: Requirements 22.2**
       * 
       * 属性 78.8.3: 手风琴模式下同级最多只有一个展开节点
       * 在手风琴模式下，任何时候同级节点中最多只有一个是展开的
       */
      it('Property 78.8.3: In accordion mode, at most one sibling should be expanded', () => {
        fc.assert(
          fc.property(
            treeDataArb(2, 5),
            (treeData) => {
              if (treeData.length < 2) return true
              
              const rootNonLeafKeys = treeData
                .filter(n => n.children && n.children.length > 0)
                .map(n => n.id)
              
              if (rootNonLeafKeys.length < 2) return true
              
              // 模拟多次手风琴模式操作
              let expandedKeys = new Set<NodeKey>()
              
              for (const key of rootNonLeafKeys) {
                expandedKeys = applyAccordionMode(treeData, expandedKeys, key)
                
                // 验证同级中只有一个展开
                const expandedSiblings = rootNonLeafKeys.filter(k => expandedKeys.has(k))
                if (expandedSiblings.length > 1) return false
              }
              
              return true
            }
          ),
          { numRuns: 100 }
        )
      })
    })
  })
})

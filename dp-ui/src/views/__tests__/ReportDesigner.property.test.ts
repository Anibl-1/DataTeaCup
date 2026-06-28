/**
 * 报表设计器字段拖拽排序属性测试
 * Feature: platform-deep-optimization
 * Property 5: 字段拖拽排序一致性
 * 
 * **Validates: Requirements 2.1**
 * 
 * WHEN 用户在字段配置区域拖拽字段时，
 * THE Report_Designer SHALL 允许通过拖拽操作调整字段显示顺序
 * 
 * 验证属性:
 * 1. 拖拽操作后，所有原始字段都被保留（无字段丢失或重复）
 * 2. sortOrder 值正确更新以反映新顺序
 * 3. 拖拽操作可逆（拖回原位置恢复原始顺序）
 * 4. 拖拽后字段的其他属性（除 sortOrder 外）保持不变
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import type { ReportField } from '@/types/reportDefinition'

// ============================================================================
// Types
// ============================================================================

/**
 * 拖拽操作定义
 */
interface DragOperation {
  /** 源位置索引 */
  fromIndex: number
  /** 目标位置索引 */
  toIndex: number
}

// ============================================================================
// Core Functions (模拟 ReportDesigner.vue 中的拖拽逻辑)
// ============================================================================

/**
 * 执行字段拖拽操作
 * 模拟 vuedraggable 的行为：将元素从 fromIndex 移动到 toIndex
 * 
 * @param fields - 原始字段数组
 * @param fromIndex - 源位置索引
 * @param toIndex - 目标位置索引
 * @returns 新的字段数组（不修改原数组）
 */
export function performDragOperation(
  fields: ReportField[],
  fromIndex: number,
  toIndex: number
): ReportField[] {
  if (fields.length === 0) return []
  if (fromIndex < 0 || fromIndex >= fields.length) return [...fields]
  if (toIndex < 0 || toIndex >= fields.length) return [...fields]
  if (fromIndex === toIndex) return [...fields]

  // 创建新数组，模拟 vuedraggable 的移动行为
  const result = [...fields]
  const [movedItem] = result.splice(fromIndex, 1)
  result.splice(toIndex, 0, movedItem)
  
  return result
}

/**
 * 更新字段的 sortOrder 值
 * 模拟 handleFieldDragEnd 函数的行为
 * 
 * @param fields - 字段数组
 * @returns 更新了 sortOrder 的新字段数组
 */
export function updateSortOrders(fields: ReportField[]): ReportField[] {
  return fields.map((field, index) => ({
    ...field,
    sortOrder: index
  }))
}

/**
 * 完整的拖拽操作（移动 + 更新 sortOrder）
 * 
 * @param fields - 原始字段数组
 * @param fromIndex - 源位置索引
 * @param toIndex - 目标位置索引
 * @returns 完成拖拽后的字段数组
 */
export function completeDragOperation(
  fields: ReportField[],
  fromIndex: number,
  toIndex: number
): ReportField[] {
  const movedFields = performDragOperation(fields, fromIndex, toIndex)
  return updateSortOrders(movedFields)
}

/**
 * 获取字段的非排序属性（用于比较）
 * 
 * @param field - 字段对象
 * @returns 不包含 sortOrder 的字段属性
 */
export function getNonSortProperties(field: ReportField): Omit<ReportField, 'sortOrder'> {
  const { sortOrder, ...rest } = field
  return rest
}

/**
 * 比较两个字段的非排序属性是否相等
 * 
 * @param field1 - 第一个字段
 * @param field2 - 第二个字段
 * @returns 是否相等
 */
export function areNonSortPropertiesEqual(field1: ReportField, field2: ReportField): boolean {
  const props1 = getNonSortProperties(field1)
  const props2 = getNonSortProperties(field2)
  return JSON.stringify(props1) === JSON.stringify(props2)
}

/**
 * 获取字段名称集合
 * 
 * @param fields - 字段数组
 * @returns 字段名称集合
 */
export function getFieldNameSet(fields: ReportField[]): Set<string> {
  return new Set(fields.map(f => f.fieldName))
}

/**
 * 检查字段数组是否有重复的字段名
 * 
 * @param fields - 字段数组
 * @returns 是否有重复
 */
export function hasDuplicateFieldNames(fields: ReportField[]): boolean {
  const names = fields.map(f => f.fieldName)
  return new Set(names).size !== names.length
}

/**
 * 验证 sortOrder 是否连续且从 0 开始
 * 
 * @param fields - 字段数组
 * @returns 是否有效
 */
export function hasValidSortOrders(fields: ReportField[]): boolean {
  if (fields.length === 0) return true
  
  for (let i = 0; i < fields.length; i++) {
    if (fields[i].sortOrder !== i) {
      return false
    }
  }
  return true
}

// ============================================================================
// Arbitraries (Test Data Generators)
// ============================================================================

/**
 * 生成有效的字段名称
 */
const fieldNameArb = fc.array(
  fc.constantFrom(...'abcdefghijklmnopqrstuvwxyz_'.split('')),
  { minLength: 1, maxLength: 20 }
).map(chars => chars.join('').toLowerCase())

/**
 * 生成唯一的字段名称数组
 */
const uniqueFieldNamesArb = (count: number) => 
  fc.uniqueArray(fieldNameArb, { minLength: count, maxLength: count })

/**
 * 生成字段类型
 */
const fieldTypeArb = fc.constantFrom(
  'VARCHAR', 'INT', 'BIGINT', 'DECIMAL', 'DATE', 'DATETIME', 'TEXT', 'BOOLEAN'
)

/**
 * 生成对齐方式
 */
const alignArb = fc.constantFrom('left', 'center', 'right')

/**
 * 生成单个报表字段
 */
const reportFieldArb = (index: number, fieldName: string): fc.Arbitrary<ReportField> =>
  fc.record({
    id: fc.option(fc.integer({ min: 1, max: 10000 }), { nil: undefined }),
    reportId: fc.option(fc.integer({ min: 1, max: 1000 }), { nil: undefined }),
    fieldName: fc.constant(fieldName),
    fieldLabel: fc.option(
      fc.array(fc.constantFrom(...'abcdefghijklmnopqrstuvwxyz0123456789_'.split('')), { minLength: 1, maxLength: 50 })
        .map(chars => chars.join('')),
      { nil: undefined }
    ),
    fieldType: fc.option(fieldTypeArb, { nil: undefined }),
    sortOrder: fc.constant(index),
    isVisible: fc.option(fc.constantFrom(0, 1), { nil: undefined }),
    width: fc.option(fc.integer({ min: 50, max: 500 }), { nil: undefined }),
    align: fc.option(alignArb, { nil: undefined }),
    dictType: fc.option(
      fc.array(fc.constantFrom(...'abcdefghijklmnopqrstuvwxyz_'.split('')), { minLength: 1, maxLength: 30 })
        .map(chars => chars.join('')),
      { nil: undefined }
    )
  })

/**
 * 生成报表字段数组（带唯一字段名）
 */
const reportFieldsArb = (minCount: number, maxCount: number): fc.Arbitrary<ReportField[]> =>
  fc.integer({ min: minCount, max: maxCount }).chain(count => {
    if (count === 0) return fc.constant([])
    
    return uniqueFieldNamesArb(count).chain(names =>
      fc.tuple(...names.map((name, index) => reportFieldArb(index, name)))
    )
  })

/**
 * 生成有效的拖拽操作（基于字段数组长度）
 */
const dragOperationArb = (fieldCount: number): fc.Arbitrary<DragOperation> => {
  if (fieldCount <= 1) {
    return fc.constant({ fromIndex: 0, toIndex: 0 })
  }
  
  return fc.record({
    fromIndex: fc.integer({ min: 0, max: fieldCount - 1 }),
    toIndex: fc.integer({ min: 0, max: fieldCount - 1 })
  })
}

/**
 * 生成字段数组和对应的有效拖拽操作
 */
const fieldsWithDragArb = fc.integer({ min: 2, max: 20 }).chain(count =>
  fc.tuple(
    reportFieldsArb(count, count),
    dragOperationArb(count)
  )
)

/**
 * 生成多个连续的拖拽操作
 */
const multipleDragOperationsArb = (fieldCount: number, opCount: number): fc.Arbitrary<DragOperation[]> => {
  if (fieldCount <= 1 || opCount === 0) {
    return fc.constant([])
  }
  
  return fc.array(dragOperationArb(fieldCount), { minLength: opCount, maxLength: opCount })
}

// ============================================================================
// Property-Based Tests
// ============================================================================

describe('Report Designer Field Drag Sort Property Tests', () => {
  /**
   * Property 5: 字段拖拽排序一致性
   * 
   * 验证字段拖拽排序在任意操作下都能保持数据一致性
   * 
   * **Validates: Requirements 2.1**
   */
  describe('Property 5: Field Drag Sort Consistency', () => {
    // ========================================================================
    // 5.1 Field Preservation - No Fields Lost or Duplicated
    // ========================================================================
    describe('5.1 After any drag operation, all original fields are preserved', () => {
      it('should preserve all field names after drag operation', () => {
        fc.assert(
          fc.property(
            fieldsWithDragArb,
            ([fields, { fromIndex, toIndex }]) => {
              const originalNames = getFieldNameSet(fields)
              const result = completeDragOperation(fields, fromIndex, toIndex)
              const resultNames = getFieldNameSet(result)
              
              // 所有原始字段名都应该存在于结果中
              for (const name of originalNames) {
                if (!resultNames.has(name)) return false
              }
              
              // 结果中不应该有新的字段名
              for (const name of resultNames) {
                if (!originalNames.has(name)) return false
              }
              
              return true
            }
          ),
          { numRuns: 500 }
        )
      })

      it('should maintain the same field count after drag operation', () => {
        fc.assert(
          fc.property(
            fieldsWithDragArb,
            ([fields, { fromIndex, toIndex }]) => {
              const result = completeDragOperation(fields, fromIndex, toIndex)
              return result.length === fields.length
            }
          ),
          { numRuns: 500 }
        )
      })

      it('should not create duplicate fields after drag operation', () => {
        fc.assert(
          fc.property(
            fieldsWithDragArb,
            ([fields, { fromIndex, toIndex }]) => {
              const result = completeDragOperation(fields, fromIndex, toIndex)
              return !hasDuplicateFieldNames(result)
            }
          ),
          { numRuns: 500 }
        )
      })

      it('should preserve fields after multiple consecutive drag operations', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 3, max: 15 }).chain(count =>
              fc.tuple(
                reportFieldsArb(count, count),
                multipleDragOperationsArb(count, 5)
              )
            ),
            ([fields, operations]) => {
              const originalNames = getFieldNameSet(fields)
              
              let current = fields
              for (const op of operations) {
                current = completeDragOperation(current, op.fromIndex, op.toIndex)
              }
              
              const resultNames = getFieldNameSet(current)
              
              // 字段数量应该保持不变
              if (current.length !== fields.length) return false
              
              // 所有原始字段名都应该存在
              for (const name of originalNames) {
                if (!resultNames.has(name)) return false
              }
              
              return true
            }
          ),
          { numRuns: 200 }
        )
      })
    })

    // ========================================================================
    // 5.2 SortOrder Values Correctly Updated
    // ========================================================================
    describe('5.2 The sortOrder values are correctly updated to reflect the new order', () => {
      it('should have consecutive sortOrder values starting from 0', () => {
        fc.assert(
          fc.property(
            fieldsWithDragArb,
            ([fields, { fromIndex, toIndex }]) => {
              const result = completeDragOperation(fields, fromIndex, toIndex)
              return hasValidSortOrders(result)
            }
          ),
          { numRuns: 500 }
        )
      })

      it('should have sortOrder matching array index after drag', () => {
        fc.assert(
          fc.property(
            fieldsWithDragArb,
            ([fields, { fromIndex, toIndex }]) => {
              const result = completeDragOperation(fields, fromIndex, toIndex)
              
              for (let i = 0; i < result.length; i++) {
                if (result[i].sortOrder !== i) return false
              }
              
              return true
            }
          ),
          { numRuns: 500 }
        )
      })

      it('should correctly position the dragged field at target index', () => {
        fc.assert(
          fc.property(
            fieldsWithDragArb.filter(([fields, { fromIndex, toIndex }]) => 
              fromIndex !== toIndex && fields.length > 1
            ),
            ([fields, { fromIndex, toIndex }]) => {
              const draggedFieldName = fields[fromIndex].fieldName
              const result = completeDragOperation(fields, fromIndex, toIndex)
              
              // 被拖拽的字段应该在目标位置
              return result[toIndex].fieldName === draggedFieldName
            }
          ),
          { numRuns: 300 }
        )
      })

      it('should maintain valid sortOrder after multiple operations', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 3, max: 15 }).chain(count =>
              fc.tuple(
                reportFieldsArb(count, count),
                multipleDragOperationsArb(count, 10)
              )
            ),
            ([fields, operations]) => {
              let current = fields
              for (const op of operations) {
                current = completeDragOperation(current, op.fromIndex, op.toIndex)
                
                // 每次操作后都应该有有效的 sortOrder
                if (!hasValidSortOrders(current)) return false
              }
              
              return true
            }
          ),
          { numRuns: 200 }
        )
      })
    })

    // ========================================================================
    // 5.3 Drag Operations are Reversible
    // ========================================================================
    describe('5.3 Drag operations are reversible (dragging back restores original order)', () => {
      it('should restore original order when dragging back to original position', () => {
        fc.assert(
          fc.property(
            fieldsWithDragArb.filter(([fields, { fromIndex, toIndex }]) => 
              fromIndex !== toIndex && fields.length > 1
            ),
            ([fields, { fromIndex, toIndex }]) => {
              // 执行拖拽
              const afterDrag = completeDragOperation(fields, fromIndex, toIndex)
              
              // 执行反向拖拽
              const restored = completeDragOperation(afterDrag, toIndex, fromIndex)
              
              // 验证字段顺序恢复
              for (let i = 0; i < fields.length; i++) {
                if (fields[i].fieldName !== restored[i].fieldName) return false
              }
              
              return true
            }
          ),
          { numRuns: 300 }
        )
      })

      it('should have same field order after drag and reverse drag', () => {
        fc.assert(
          fc.property(
            fieldsWithDragArb.filter(([fields, { fromIndex, toIndex }]) => 
              fromIndex !== toIndex && fields.length > 1
            ),
            ([fields, { fromIndex, toIndex }]) => {
              const afterDrag = completeDragOperation(fields, fromIndex, toIndex)
              const restored = completeDragOperation(afterDrag, toIndex, fromIndex)
              
              // 字段名顺序应该完全相同
              const originalOrder = fields.map(f => f.fieldName).join(',')
              const restoredOrder = restored.map(f => f.fieldName).join(',')
              
              return originalOrder === restoredOrder
            }
          ),
          { numRuns: 300 }
        )
      })

      it('should be idempotent when dragging to same position', () => {
        fc.assert(
          fc.property(
            reportFieldsArb(2, 15),
            (fields) => {
              // 随机选择一个位置
              const index = Math.floor(Math.random() * fields.length)
              
              // 拖拽到相同位置
              const result = completeDragOperation(fields, index, index)
              
              // 字段顺序应该不变
              for (let i = 0; i < fields.length; i++) {
                if (fields[i].fieldName !== result[i].fieldName) return false
              }
              
              return true
            }
          ),
          { numRuns: 200 }
        )
      })
    })

    // ========================================================================
    // 5.4 Non-SortOrder Properties Remain Unchanged
    // ========================================================================
    describe('5.4 Field properties other than sortOrder remain unchanged after drag', () => {
      it('should preserve all non-sortOrder properties after drag', () => {
        fc.assert(
          fc.property(
            fieldsWithDragArb,
            ([fields, { fromIndex, toIndex }]) => {
              const result = completeDragOperation(fields, fromIndex, toIndex)
              
              // 为每个原始字段找到对应的结果字段并比较非排序属性
              for (const originalField of fields) {
                const resultField = result.find(f => f.fieldName === originalField.fieldName)
                
                if (!resultField) return false
                if (!areNonSortPropertiesEqual(originalField, resultField)) return false
              }
              
              return true
            }
          ),
          { numRuns: 500 }
        )
      })

      it('should preserve fieldLabel after drag', () => {
        fc.assert(
          fc.property(
            fieldsWithDragArb,
            ([fields, { fromIndex, toIndex }]) => {
              const result = completeDragOperation(fields, fromIndex, toIndex)
              
              for (const originalField of fields) {
                const resultField = result.find(f => f.fieldName === originalField.fieldName)
                if (!resultField) return false
                if (resultField.fieldLabel !== originalField.fieldLabel) return false
              }
              
              return true
            }
          ),
          { numRuns: 300 }
        )
      })

      it('should preserve fieldType after drag', () => {
        fc.assert(
          fc.property(
            fieldsWithDragArb,
            ([fields, { fromIndex, toIndex }]) => {
              const result = completeDragOperation(fields, fromIndex, toIndex)
              
              for (const originalField of fields) {
                const resultField = result.find(f => f.fieldName === originalField.fieldName)
                if (!resultField) return false
                if (resultField.fieldType !== originalField.fieldType) return false
              }
              
              return true
            }
          ),
          { numRuns: 300 }
        )
      })

      it('should preserve isVisible after drag', () => {
        fc.assert(
          fc.property(
            fieldsWithDragArb,
            ([fields, { fromIndex, toIndex }]) => {
              const result = completeDragOperation(fields, fromIndex, toIndex)
              
              for (const originalField of fields) {
                const resultField = result.find(f => f.fieldName === originalField.fieldName)
                if (!resultField) return false
                if (resultField.isVisible !== originalField.isVisible) return false
              }
              
              return true
            }
          ),
          { numRuns: 300 }
        )
      })

      it('should preserve width after drag', () => {
        fc.assert(
          fc.property(
            fieldsWithDragArb,
            ([fields, { fromIndex, toIndex }]) => {
              const result = completeDragOperation(fields, fromIndex, toIndex)
              
              for (const originalField of fields) {
                const resultField = result.find(f => f.fieldName === originalField.fieldName)
                if (!resultField) return false
                if (resultField.width !== originalField.width) return false
              }
              
              return true
            }
          ),
          { numRuns: 300 }
        )
      })

      it('should preserve align after drag', () => {
        fc.assert(
          fc.property(
            fieldsWithDragArb,
            ([fields, { fromIndex, toIndex }]) => {
              const result = completeDragOperation(fields, fromIndex, toIndex)
              
              for (const originalField of fields) {
                const resultField = result.find(f => f.fieldName === originalField.fieldName)
                if (!resultField) return false
                if (resultField.align !== originalField.align) return false
              }
              
              return true
            }
          ),
          { numRuns: 300 }
        )
      })

      it('should preserve dictType after drag', () => {
        fc.assert(
          fc.property(
            fieldsWithDragArb,
            ([fields, { fromIndex, toIndex }]) => {
              const result = completeDragOperation(fields, fromIndex, toIndex)
              
              for (const originalField of fields) {
                const resultField = result.find(f => f.fieldName === originalField.fieldName)
                if (!resultField) return false
                if (resultField.dictType !== originalField.dictType) return false
              }
              
              return true
            }
          ),
          { numRuns: 300 }
        )
      })

      it('should preserve all properties after multiple drag operations', () => {
        fc.assert(
          fc.property(
            fc.integer({ min: 3, max: 10 }).chain(count =>
              fc.tuple(
                reportFieldsArb(count, count),
                multipleDragOperationsArb(count, 5)
              )
            ),
            ([fields, operations]) => {
              let current = fields
              for (const op of operations) {
                current = completeDragOperation(current, op.fromIndex, op.toIndex)
              }
              
              // 验证所有非排序属性都保持不变
              for (const originalField of fields) {
                const resultField = current.find(f => f.fieldName === originalField.fieldName)
                if (!resultField) return false
                if (!areNonSortPropertiesEqual(originalField, resultField)) return false
              }
              
              return true
            }
          ),
          { numRuns: 200 }
        )
      })
    })

    // ========================================================================
    // 5.5 Edge Cases
    // ========================================================================
    describe('5.5 Edge cases are handled correctly', () => {
      it('should handle empty field array', () => {
        const result = completeDragOperation([], 0, 0)
        expect(result).toEqual([])
      })

      it('should handle single field array', () => {
        fc.assert(
          fc.property(
            reportFieldsArb(1, 1),
            (fields) => {
              const result = completeDragOperation(fields, 0, 0)
              return result.length === 1 && 
                     result[0].fieldName === fields[0].fieldName &&
                     result[0].sortOrder === 0
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should handle invalid fromIndex gracefully', () => {
        fc.assert(
          fc.property(
            reportFieldsArb(3, 10),
            (fields) => {
              // 使用超出范围的 fromIndex
              const result = completeDragOperation(fields, -1, 0)
              
              // 应该返回原数组的副本（带更新的 sortOrder）
              return result.length === fields.length
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should handle invalid toIndex gracefully', () => {
        fc.assert(
          fc.property(
            reportFieldsArb(3, 10),
            (fields) => {
              // 使用超出范围的 toIndex
              const result = completeDragOperation(fields, 0, fields.length + 10)
              
              // 应该返回原数组的副本（带更新的 sortOrder）
              return result.length === fields.length
            }
          ),
          { numRuns: 50 }
        )
      })

      it('should handle drag from first to last position', () => {
        fc.assert(
          fc.property(
            reportFieldsArb(3, 15),
            (fields) => {
              const firstFieldName = fields[0].fieldName
              const result = completeDragOperation(fields, 0, fields.length - 1)
              
              // 第一个字段应该移动到最后
              return result[result.length - 1].fieldName === firstFieldName &&
                     hasValidSortOrders(result)
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should handle drag from last to first position', () => {
        fc.assert(
          fc.property(
            reportFieldsArb(3, 15),
            (fields) => {
              const lastFieldName = fields[fields.length - 1].fieldName
              const result = completeDragOperation(fields, fields.length - 1, 0)
              
              // 最后一个字段应该移动到第一个
              return result[0].fieldName === lastFieldName &&
                     hasValidSortOrders(result)
            }
          ),
          { numRuns: 100 }
        )
      })

      it('should handle adjacent position drag', () => {
        fc.assert(
          fc.property(
            reportFieldsArb(3, 15).chain(fields =>
              fc.tuple(
                fc.constant(fields),
                fc.integer({ min: 0, max: fields.length - 2 })
              )
            ),
            ([fields, index]) => {
              const fieldName = fields[index].fieldName
              const result = completeDragOperation(fields, index, index + 1)
              
              // 字段应该移动到相邻位置
              return result[index + 1].fieldName === fieldName &&
                     hasValidSortOrders(result)
            }
          ),
          { numRuns: 100 }
        )
      })
    })

    // ========================================================================
    // 5.6 Consistency with Original Implementation
    // ========================================================================
    describe('5.6 Consistency with original implementation behavior', () => {
      it('should not modify the original array', () => {
        fc.assert(
          fc.property(
            fieldsWithDragArb,
            ([fields, { fromIndex, toIndex }]) => {
              const originalFieldsCopy = JSON.stringify(fields)
              completeDragOperation(fields, fromIndex, toIndex)
              
              // 原数组应该保持不变
              return JSON.stringify(fields) === originalFieldsCopy
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should return a new array instance', () => {
        fc.assert(
          fc.property(
            fieldsWithDragArb,
            ([fields, { fromIndex, toIndex }]) => {
              const result = completeDragOperation(fields, fromIndex, toIndex)
              
              // 结果应该是新数组
              return result !== fields
            }
          ),
          { numRuns: 200 }
        )
      })

      it('should return new field objects (not references)', () => {
        fc.assert(
          fc.property(
            fieldsWithDragArb,
            ([fields, { fromIndex, toIndex }]) => {
              const result = completeDragOperation(fields, fromIndex, toIndex)
              
              // 结果中的字段对象应该是新对象
              for (let i = 0; i < result.length; i++) {
                const originalField = fields.find(f => f.fieldName === result[i].fieldName)
                if (originalField && result[i] === originalField) {
                  return false
                }
              }
              
              return true
            }
          ),
          { numRuns: 200 }
        )
      })
    })
  })
})

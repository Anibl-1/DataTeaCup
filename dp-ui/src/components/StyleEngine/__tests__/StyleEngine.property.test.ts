/**
 * 样式继承属性测试
 * Style Inheritance Property Tests
 * 
 * **属性 68: 样式继承正确性**
 * **验证需求: 21.1**
 * 
 * 测试内容:
 * 1. 样式继承顺序: 全局 → 列 → 行 → 单元格
 * 2. 后定义的样式覆盖先定义的样式
 * 3. 未指定的属性从父级继承
 * 4. 样式合并是关联的和确定性的
 * 5. 空/undefined 样式不影响继承
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import {
  mergeStyles,
  applyStyleInheritance,
  StyleEngine,
  createStyleEngine,
  cellStyleToCss,
} from '../StyleEngine'
import type { CellStyle, FontStyle, AlignmentStyle, BackgroundStyle, BorderStyle, PaddingStyle } from '../types/style'

// ============================================================================
// Arbitraries (测试数据生成器)
// ============================================================================

/** 生成颜色值 */
const colorArb = fc.constantFrom(
  '#ff0000', '#00ff00', '#0000ff', '#ffff00', '#ff00ff', '#00ffff',
  '#ff4d4f', '#52c41a', '#faad14', '#1890ff', '#722ed1', '#eb2f96',
  '#ffffff', '#000000', '#f0f0f0', '#333333'
)

/** 生成字体族 */
const fontFamilyArb = fc.constantFrom(
  'Arial', 'Microsoft YaHei', 'Helvetica', 'Times New Roman', 'Courier New'
)

/** 生成字体大小 */
const fontSizeArb = fc.integer({ min: 10, max: 32 })

/** 生成字体粗细 */
const fontWeightArb = fc.constantFrom('normal', 'bold', 400, 500, 600, 700)


/** 生成字体样式 */
const fontStyleArb: fc.Arbitrary<FontStyle> = fc.record({
  family: fc.option(fontFamilyArb, { nil: undefined }),
  size: fc.option(fontSizeArb, { nil: undefined }),
  weight: fc.option(fontWeightArb as fc.Arbitrary<FontStyle['weight']>, { nil: undefined }),
  style: fc.option(fc.constantFrom('normal', 'italic', 'oblique') as fc.Arbitrary<FontStyle['style']>, { nil: undefined }),
  decoration: fc.option(fc.constantFrom('none', 'underline', 'line-through') as fc.Arbitrary<FontStyle['decoration']>, { nil: undefined }),
  color: fc.option(colorArb, { nil: undefined }),
}).filter(f => Object.values(f).some(v => v !== undefined))

/** 生成对齐样式 */
const alignmentStyleArb: fc.Arbitrary<AlignmentStyle> = fc.record({
  horizontal: fc.option(fc.constantFrom('left', 'center', 'right', 'justify') as fc.Arbitrary<AlignmentStyle['horizontal']>, { nil: undefined }),
  vertical: fc.option(fc.constantFrom('top', 'middle', 'bottom') as fc.Arbitrary<AlignmentStyle['vertical']>, { nil: undefined }),
  textDirection: fc.option(fc.constantFrom('ltr', 'rtl') as fc.Arbitrary<AlignmentStyle['textDirection']>, { nil: undefined }),
  indent: fc.option(fc.integer({ min: 0, max: 5 }), { nil: undefined }),
  wrapText: fc.option(fc.boolean(), { nil: undefined }),
}).filter(a => Object.values(a).some(v => v !== undefined))

/** 生成背景样式 */
const backgroundStyleArb: fc.Arbitrary<BackgroundStyle> = fc.oneof(
  fc.record({
    type: fc.constant('solid' as const),
    color: colorArb,
  }),
  fc.record({
    type: fc.constant('gradient' as const),
    gradient: fc.record({
      type: fc.constantFrom('linear', 'radial') as fc.Arbitrary<'linear' | 'radial'>,
      angle: fc.option(fc.integer({ min: 0, max: 360 }), { nil: undefined }),
      colors: fc.array(
        fc.record({ offset: fc.double({ min: 0, max: 1, noNaN: true }), color: colorArb }),
        { minLength: 2, maxLength: 4 }
      ),
    }),
  })
)

/** 生成内边距样式 */
const paddingStyleArb: fc.Arbitrary<PaddingStyle> = fc.record({
  top: fc.option(fc.integer({ min: 0, max: 20 }), { nil: undefined }),
  right: fc.option(fc.integer({ min: 0, max: 20 }), { nil: undefined }),
  bottom: fc.option(fc.integer({ min: 0, max: 20 }), { nil: undefined }),
  left: fc.option(fc.integer({ min: 0, max: 20 }), { nil: undefined }),
}).filter(p => Object.values(p).some(v => v !== undefined))


/** 生成单元格样式 */
const cellStyleArb: fc.Arbitrary<CellStyle> = fc.record({
  font: fc.option(fontStyleArb, { nil: undefined }),
  alignment: fc.option(alignmentStyleArb, { nil: undefined }),
  background: fc.option(backgroundStyleArb, { nil: undefined }),
  padding: fc.option(paddingStyleArb, { nil: undefined }),
}).filter(s => Object.values(s).some(v => v !== undefined))

/** 生成非空单元格样式 */
const nonEmptyCellStyleArb: fc.Arbitrary<CellStyle> = cellStyleArb.filter(
  s => Object.keys(s).length > 0 && Object.values(s).some(v => v !== undefined)
)

/** 生成行索引 */
const rowIndexArb = fc.integer({ min: 0, max: 1000 })

/** 生成列键 */
const columnKeyArb = fc.constantFrom('amount', 'name', 'date', 'status', 'price', 'quantity', 'total')

/** 生成数值 */
const numericValueArb = fc.double({ min: -10000, max: 10000, noNaN: true })

/** 生成行数据 */
const rowDataArb = fc.record({
  amount: fc.double({ min: -10000, max: 10000, noNaN: true }),
  name: fc.string({ minLength: 1, maxLength: 20 }),
  date: fc.date().map(d => d.toISOString()),
  status: fc.constantFrom('active', 'inactive', 'pending'),
  price: fc.double({ min: 0, max: 10000, noNaN: true }),
  quantity: fc.integer({ min: 0, max: 1000 }),
  total: fc.double({ min: 0, max: 100000, noNaN: true }),
})

// ============================================================================
// 辅助函数
// ============================================================================

/** 检查样式是否为空 */
function isEmptyStyle(style: CellStyle | undefined): boolean {
  if (!style) return true
  return Object.keys(style).length === 0 || Object.values(style).every(v => v === undefined)
}

/** 获取样式中的有效属性数量 */
function countDefinedProps(style: CellStyle | undefined): number {
  if (!style) return 0
  let count = 0
  if (style.font) count += Object.values(style.font).filter(v => v !== undefined).length
  if (style.alignment) count += Object.values(style.alignment).filter(v => v !== undefined).length
  if (style.background) count++
  if (style.padding) count += Object.values(style.padding).filter(v => v !== undefined).length
  return count
}


// ============================================================================
// 属性测试
// ============================================================================

describe('Style Inheritance Property Tests', () => {
  
  // ==========================================================================
  // 属性 68: 样式继承正确性
  // ==========================================================================
  
  describe('Property 68: Style Inheritance Correctness', () => {
    
    /**
     * **Validates: Requirements 21.1**
     * 
     * 属性 68.1: 样式继承顺序 - 单元格样式覆盖行样式
     * 当单元格样式和行样式都定义了相同属性时，单元格样式应覆盖行样式
     */
    it('Property 68.1: Cell style should override row style', () => {
      fc.assert(
        fc.property(
          colorArb,
          colorArb,
          (rowColor, cellColor) => {
            // 确保颜色不同以验证覆盖
            if (rowColor === cellColor) return true
            
            const rowStyle: CellStyle = { font: { color: rowColor } }
            const cellStyle: CellStyle = { font: { color: cellColor } }
            
            const result = applyStyleInheritance(undefined, undefined, rowStyle, cellStyle)
            
            // 单元格样式应覆盖行样式
            return result.font?.color === cellColor
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 21.1**
     * 
     * 属性 68.2: 样式继承顺序 - 行样式覆盖列样式
     * 当行样式和列样式都定义了相同属性时，行样式应覆盖列样式
     */
    it('Property 68.2: Row style should override column style', () => {
      fc.assert(
        fc.property(
          colorArb,
          colorArb,
          (columnColor, rowColor) => {
            if (columnColor === rowColor) return true
            
            const columnStyle: CellStyle = { font: { color: columnColor } }
            const rowStyle: CellStyle = { font: { color: rowColor } }
            
            const result = applyStyleInheritance(undefined, columnStyle, rowStyle, undefined)
            
            return result.font?.color === rowColor
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 21.1**
     * 
     * 属性 68.3: 样式继承顺序 - 列样式覆盖全局样式
     * 当列样式和全局样式都定义了相同属性时，列样式应覆盖全局样式
     */
    it('Property 68.3: Column style should override global style', () => {
      fc.assert(
        fc.property(
          colorArb,
          colorArb,
          (globalColor, columnColor) => {
            if (globalColor === columnColor) return true
            
            const globalStyle: CellStyle = { font: { color: globalColor } }
            const columnStyle: CellStyle = { font: { color: columnColor } }
            
            const result = applyStyleInheritance(globalStyle, columnStyle, undefined, undefined)
            
            return result.font?.color === columnColor
          }
        ),
        { numRuns: 100 }
      )
    })


    /**
     * **Validates: Requirements 21.1**
     * 
     * 属性 68.4: 完整继承链 - 全局 → 列 → 行 → 单元格
     * 验证完整的四级继承链，每级都能正确覆盖上一级
     */
    it('Property 68.4: Full inheritance chain global → column → row → cell', () => {
      fc.assert(
        fc.property(
          colorArb,
          colorArb,
          colorArb,
          colorArb,
          (globalColor, columnColor, rowColor, cellColor) => {
            const globalStyle: CellStyle = { font: { color: globalColor } }
            const columnStyle: CellStyle = { font: { color: columnColor } }
            const rowStyle: CellStyle = { font: { color: rowColor } }
            const cellStyle: CellStyle = { font: { color: cellColor } }
            
            const result = applyStyleInheritance(globalStyle, columnStyle, rowStyle, cellStyle)
            
            // 最终结果应该是单元格样式（最高优先级）
            return result.font?.color === cellColor
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 21.1**
     * 
     * 属性 68.5: 未指定属性从父级继承
     * 当子级未定义某属性时，应从父级继承该属性
     */
    it('Property 68.5: Unspecified properties should be inherited from parent', () => {
      fc.assert(
        fc.property(
          colorArb,
          fontSizeArb,
          (globalColor, cellSize) => {
            const globalStyle: CellStyle = { font: { color: globalColor, size: 12 } }
            const cellStyle: CellStyle = { font: { size: cellSize } } // 只定义 size，不定义 color
            
            const result = applyStyleInheritance(globalStyle, undefined, undefined, cellStyle)
            
            // color 应从全局继承，size 应使用单元格的值
            return result.font?.color === globalColor && result.font?.size === cellSize
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 21.1**
     * 
     * 属性 68.6: 多属性继承 - 不同级别定义不同属性
     * 当不同级别定义不同属性时，所有属性都应被保留
     */
    it('Property 68.6: Multiple properties from different levels should all be preserved', () => {
      fc.assert(
        fc.property(
          colorArb,
          fontSizeArb,
          fc.constantFrom('left', 'center', 'right') as fc.Arbitrary<'left' | 'center' | 'right'>,
          colorArb,
          (globalColor, columnSize, rowAlign, cellBgColor) => {
            const globalStyle: CellStyle = { font: { color: globalColor } }
            const columnStyle: CellStyle = { font: { size: columnSize } }
            const rowStyle: CellStyle = { alignment: { horizontal: rowAlign } }
            const cellStyle: CellStyle = { background: { type: 'solid', color: cellBgColor } }
            
            const result = applyStyleInheritance(globalStyle, columnStyle, rowStyle, cellStyle)
            
            // 所有属性都应被保留
            return (
              result.font?.color === globalColor &&
              result.font?.size === columnSize &&
              result.alignment?.horizontal === rowAlign &&
              result.background?.color === cellBgColor
            )
          }
        ),
        { numRuns: 100 }
      )
    })


    /**
     * **Validates: Requirements 21.1**
     * 
     * 属性 68.7: 空/undefined 样式不影响继承
     * 当某级样式为空或 undefined 时，不应影响其他级别的样式
     */
    it('Property 68.7: Empty/undefined styles should not affect inheritance', () => {
      fc.assert(
        fc.property(
          colorArb,
          fontSizeArb,
          (globalColor, cellSize) => {
            const globalStyle: CellStyle = { font: { color: globalColor } }
            const cellStyle: CellStyle = { font: { size: cellSize } }
            
            // 中间级别为 undefined
            const result1 = applyStyleInheritance(globalStyle, undefined, undefined, cellStyle)
            // 中间级别为空对象
            const result2 = applyStyleInheritance(globalStyle, {}, {}, cellStyle)
            
            // 两种情况结果应相同
            return (
              result1.font?.color === globalColor &&
              result1.font?.size === cellSize &&
              result2.font?.color === globalColor &&
              result2.font?.size === cellSize
            )
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 21.1**
     * 
     * 属性 68.8: 样式合并是确定性的
     * 对于相同的输入，样式合并应始终产生相同的输出
     */
    it('Property 68.8: Style merge should be deterministic', () => {
      fc.assert(
        fc.property(
          cellStyleArb,
          cellStyleArb,
          cellStyleArb,
          cellStyleArb,
          (global, column, row, cell) => {
            const result1 = applyStyleInheritance(global, column, row, cell)
            const result2 = applyStyleInheritance(global, column, row, cell)
            
            // 两次调用结果应完全相同
            return JSON.stringify(result1) === JSON.stringify(result2)
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 21.1**
     * 
     * 属性 68.9: mergeStyles 函数的关联性
     * merge(merge(a, b), c) 应等于 merge(a, merge(b, c))
     */
    it('Property 68.9: mergeStyles should be associative', () => {
      fc.assert(
        fc.property(
          cellStyleArb,
          cellStyleArb,
          cellStyleArb,
          (a, b, c) => {
            const leftAssoc = mergeStyles(mergeStyles(a, b), c)
            const rightAssoc = mergeStyles(a, mergeStyles(b, c))
            
            // 关联性：(a ∘ b) ∘ c = a ∘ (b ∘ c)
            return JSON.stringify(leftAssoc) === JSON.stringify(rightAssoc)
          }
        ),
        { numRuns: 100 }
      )
    })


    /**
     * **Validates: Requirements 21.1**
     * 
     * 属性 68.10: mergeStyles 的单位元
     * merge(style, {}) 应等于 style，merge({}, style) 也应等于 style
     */
    it('Property 68.10: Empty style should be identity element for merge', () => {
      fc.assert(
        fc.property(
          nonEmptyCellStyleArb,
          (style) => {
            const mergedWithEmptyRight = mergeStyles(style, {})
            const mergedWithEmptyLeft = mergeStyles({}, style)
            const mergedWithUndefined = mergeStyles(style, undefined)
            
            // 空样式应该是单位元
            const styleStr = JSON.stringify(style)
            return (
              JSON.stringify(mergedWithEmptyRight) === styleStr &&
              JSON.stringify(mergedWithEmptyLeft) === styleStr &&
              JSON.stringify(mergedWithUndefined) === styleStr
            )
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 21.1**
     * 
     * 属性 68.11: 后定义的样式完全覆盖先定义的相同属性
     * 当两个样式定义了相同的属性时，后者应完全覆盖前者
     */
    it('Property 68.11: Later style should completely override earlier style for same property', () => {
      fc.assert(
        fc.property(
          fontStyleArb,
          fontStyleArb,
          (font1, font2) => {
            const style1: CellStyle = { font: font1 }
            const style2: CellStyle = { font: font2 }
            
            const merged = mergeStyles(style1, style2)
            
            // 验证 font2 中定义的所有属性都覆盖了 font1
            if (font2.color !== undefined) {
              if (merged.font?.color !== font2.color) return false
            }
            if (font2.size !== undefined) {
              if (merged.font?.size !== font2.size) return false
            }
            if (font2.weight !== undefined) {
              if (merged.font?.weight !== font2.weight) return false
            }
            
            return true
          }
        ),
        { numRuns: 100 }
      )
    })
  })


  // ==========================================================================
  // StyleEngine 类测试
  // ==========================================================================
  
  describe('StyleEngine Class Tests', () => {
    
    /**
     * **Validates: Requirements 21.1**
     * 
     * 属性 68.12: StyleEngine.computeCellStyle 应正确应用继承
     * 使用 StyleEngine 计算单元格样式时，应正确应用四级继承
     */
    it('Property 68.12: StyleEngine.computeCellStyle should apply inheritance correctly', () => {
      fc.assert(
        fc.property(
          colorArb,
          colorArb,
          columnKeyArb,
          rowIndexArb,
          numericValueArb,
          (globalColor, columnColor, columnKey, rowIndex, value) => {
            const engine = createStyleEngine({
              globalStyle: { font: { color: globalColor } },
              columnStyles: { [columnKey]: { font: { color: columnColor } } },
            })
            
            const result = engine.computeCellStyle({
              rowIndex,
              columnKey,
              value,
              rowData: { [columnKey]: value },
            })
            
            // 列样式应覆盖全局样式
            return result.font?.color === columnColor
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 21.1**
     * 
     * 属性 68.13: StyleEngine 应正确处理斑马纹样式
     * 偶数行和奇数行应应用不同的样式
     */
    it('Property 68.13: StyleEngine should correctly apply zebra stripe styles', () => {
      fc.assert(
        fc.property(
          colorArb,
          colorArb,
          columnKeyArb,
          fc.integer({ min: 0, max: 500 }),
          (evenColor, oddColor, columnKey, baseIndex) => {
            // 确保颜色不同
            if (evenColor === oddColor) return true
            
            const engine = createStyleEngine({
              rowStyles: {
                evenRowStyle: { background: { type: 'solid', color: evenColor } },
                oddRowStyle: { background: { type: 'solid', color: oddColor } },
              },
            }, { enableZebraStripe: true, enableConditionalFormat: false })
            
            const evenRowIndex = baseIndex * 2
            const oddRowIndex = baseIndex * 2 + 1
            
            const evenResult = engine.computeCellStyle({
              rowIndex: evenRowIndex,
              columnKey,
              value: 0,
              rowData: {},
            })
            
            const oddResult = engine.computeCellStyle({
              rowIndex: oddRowIndex,
              columnKey,
              value: 0,
              rowData: {},
            })
            
            return (
              evenResult.background?.color === evenColor &&
              oddResult.background?.color === oddColor
            )
          }
        ),
        { numRuns: 100 }
      )
    })


    /**
     * **Validates: Requirements 21.1**
     * 
     * 属性 68.14: StyleEngine 单元格样式应覆盖所有其他级别
     * 直接设置的单元格样式应具有最高优先级
     */
    it('Property 68.14: Cell-specific style should override all other levels', () => {
      fc.assert(
        fc.property(
          colorArb,
          colorArb,
          colorArb,
          colorArb,
          columnKeyArb,
          rowIndexArb,
          (globalColor, columnColor, rowColor, cellColor, columnKey, rowIndex) => {
            const engine = createStyleEngine({
              globalStyle: { font: { color: globalColor } },
              columnStyles: { [columnKey]: { font: { color: columnColor } } },
              rowStyles: {
                evenRowStyle: { font: { color: rowColor } },
                oddRowStyle: { font: { color: rowColor } },
              },
            }, { enableConditionalFormat: false })
            
            // 设置特定单元格样式
            engine.setCellStyle(rowIndex, columnKey, { font: { color: cellColor } })
            
            const result = engine.computeCellStyle({
              rowIndex,
              columnKey,
              value: 0,
              rowData: {},
            })
            
            // 单元格样式应覆盖所有其他级别
            return result.font?.color === cellColor
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 21.1**
     * 
     * 属性 68.15: StyleEngine 配置更新应清除缓存
     * 更新配置后，计算结果应反映新配置
     */
    it('Property 68.15: Config update should reflect in computed styles', () => {
      fc.assert(
        fc.property(
          colorArb,
          colorArb,
          columnKeyArb,
          rowIndexArb,
          (color1, color2, columnKey, rowIndex) => {
            if (color1 === color2) return true
            
            const engine = createStyleEngine({
              globalStyle: { font: { color: color1 } },
            }, { enableConditionalFormat: false })
            
            const result1 = engine.computeCellStyle({
              rowIndex,
              columnKey,
              value: 0,
              rowData: {},
            })
            
            // 更新全局样式
            engine.setGlobalStyle({ font: { color: color2 } })
            
            const result2 = engine.computeCellStyle({
              rowIndex,
              columnKey,
              value: 0,
              rowData: {},
            })
            
            return result1.font?.color === color1 && result2.font?.color === color2
          }
        ),
        { numRuns: 100 }
      )
    })


    /**
     * **Validates: Requirements 21.1**
     * 
     * 属性 68.16: StyleEngine 批量计算应与单独计算结果一致
     * computeCellStyles 的结果应与多次调用 computeCellStyle 的结果一致
     */
    it('Property 68.16: Batch compute should match individual compute results', () => {
      fc.assert(
        fc.property(
          colorArb,
          fc.array(
            fc.record({
              rowIndex: rowIndexArb,
              columnKey: columnKeyArb,
              value: numericValueArb,
            }),
            { minLength: 1, maxLength: 10 }
          ),
          (globalColor, cells) => {
            const engine = createStyleEngine({
              globalStyle: { font: { color: globalColor } },
            }, { enableConditionalFormat: false })
            
            // 批量计算
            const batchResults = engine.computeCellStyles(
              cells.map(c => ({ ...c, rowData: { [c.columnKey]: c.value } }))
            )
            
            // 单独计算并比较
            for (const cell of cells) {
              const singleResult = engine.computeCellStyle({
                ...cell,
                rowData: { [cell.columnKey]: cell.value },
              })
              const batchResult = batchResults.get(`${cell.rowIndex}:${cell.columnKey}`)
              
              if (JSON.stringify(singleResult) !== JSON.stringify(batchResult)) {
                return false
              }
            }
            
            return true
          }
        ),
        { numRuns: 50 }
      )
    })
  })

  // ==========================================================================
  // 边界条件和特殊情况测试
  // ==========================================================================
  
  describe('Edge Cases and Special Conditions', () => {
    
    /**
     * **Validates: Requirements 21.1**
     * 
     * 所有级别都为空时应返回空样式
     */
    it('Should return empty style when all levels are empty', () => {
      const result = applyStyleInheritance(undefined, undefined, undefined, undefined)
      expect(Object.keys(result).length).toBe(0)
    })

    /**
     * **Validates: Requirements 21.1**
     * 
     * 所有级别都为空对象时应返回空样式
     */
    it('Should return empty style when all levels are empty objects', () => {
      const result = applyStyleInheritance({}, {}, {}, {})
      expect(Object.keys(result).length).toBe(0)
    })


    /**
     * **Validates: Requirements 21.1**
     * 
     * 只有全局样式时应返回全局样式
     */
    it('Should return global style when only global is defined', () => {
      fc.assert(
        fc.property(
          nonEmptyCellStyleArb,
          (globalStyle) => {
            const result = applyStyleInheritance(globalStyle, undefined, undefined, undefined)
            return JSON.stringify(result) === JSON.stringify(globalStyle)
          }
        ),
        { numRuns: 50 }
      )
    })

    /**
     * **Validates: Requirements 21.1**
     * 
     * 边框样式的 all 快捷属性应正确展开
     */
    it('Border all shortcut should expand correctly', () => {
      const style1: CellStyle = {
        border: { all: { style: 'solid', width: 1, color: '#000' } }
      }
      const style2: CellStyle = {
        border: { top: { style: 'dashed', width: 2, color: '#fff' } }
      }
      
      const merged = mergeStyles(style1, style2)
      
      // top 应被覆盖，其他边应保持 all 的值
      expect(merged.border?.top?.style).toBe('dashed')
      expect(merged.border?.top?.width).toBe(2)
      expect(merged.border?.right?.style).toBe('solid')
      expect(merged.border?.bottom?.style).toBe('solid')
      expect(merged.border?.left?.style).toBe('solid')
    })

    /**
     * **Validates: Requirements 21.1**
     * 
     * 背景样式应完全覆盖（不做深度合并）
     */
    it('Background style should be completely overridden', () => {
      fc.assert(
        fc.property(
          backgroundStyleArb,
          backgroundStyleArb,
          (bg1, bg2) => {
            const style1: CellStyle = { background: bg1 }
            const style2: CellStyle = { background: bg2 }
            
            const merged = mergeStyles(style1, style2)
            
            // 背景应完全使用 style2 的值
            return merged.background?.type === bg2.type
          }
        ),
        { numRuns: 50 }
      )
    })

    /**
     * **Validates: Requirements 21.1**
     * 
     * cellStyleToCss 应正确转换样式
     */
    it('cellStyleToCss should correctly convert styles', () => {
      fc.assert(
        fc.property(
          colorArb,
          fontSizeArb,
          fc.constantFrom('left', 'center', 'right') as fc.Arbitrary<'left' | 'center' | 'right'>,
          (color, size, align) => {
            const style: CellStyle = {
              font: { color, size },
              alignment: { horizontal: align },
            }
            
            const css = cellStyleToCss(style)
            
            return (
              css.color === color &&
              css.fontSize === `${size}px` &&
              css.textAlign === align
            )
          }
        ),
        { numRuns: 50 }
      )
    })


    /**
     * **Validates: Requirements 21.1**
     * 
     * StyleEngine 缓存统计应正确反映缓存状态
     */
    it('StyleEngine cache stats should reflect cache state', () => {
      const engine = createStyleEngine({
        globalStyle: { font: { color: '#000' } },
      }, { enableConditionalFormat: false, cacheSize: 100 })
      
      const initialStats = engine.getCacheStats()
      expect(initialStats.size).toBe(0)
      expect(initialStats.maxSize).toBe(100)
      
      // 计算一些样式以填充缓存
      for (let i = 0; i < 10; i++) {
        engine.computeCellStyle({
          rowIndex: i,
          columnKey: 'amount',
          value: i * 100,
          rowData: { amount: i * 100 },
        })
      }
      
      const afterStats = engine.getCacheStats()
      expect(afterStats.size).toBe(10)
      
      // 清除缓存
      engine.clearCache()
      const clearedStats = engine.getCacheStats()
      expect(clearedStats.size).toBe(0)
    })

    /**
     * **Validates: Requirements 21.1**
     * 
     * 特定行样式应覆盖斑马纹样式
     */
    it('Specific row style should override zebra stripe', () => {
      fc.assert(
        fc.property(
          colorArb,
          colorArb,
          colorArb,
          fc.integer({ min: 0, max: 100 }),
          (evenColor, oddColor, specificColor, rowIndex) => {
            // 确保颜色不同
            if (evenColor === specificColor || oddColor === specificColor) return true
            
            const engine = createStyleEngine({
              rowStyles: {
                evenRowStyle: { background: { type: 'solid', color: evenColor } },
                oddRowStyle: { background: { type: 'solid', color: oddColor } },
                specificRows: { [rowIndex]: { background: { type: 'solid', color: specificColor } } },
              },
            }, { enableZebraStripe: true, enableConditionalFormat: false })
            
            const result = engine.computeCellStyle({
              rowIndex,
              columnKey: 'amount',
              value: 0,
              rowData: {},
            })
            
            // 特定行样式应覆盖斑马纹
            return result.background?.color === specificColor
          }
        ),
        { numRuns: 50 }
      )
    })

    /**
     * **Validates: Requirements 21.1**
     * 
     * 表头行样式应正确应用
     */
    it('Header row style should be applied correctly', () => {
      fc.assert(
        fc.property(
          colorArb,
          colorArb,
          (headerColor, summaryColor) => {
            if (headerColor === summaryColor) return true
            
            const engine = createStyleEngine({
              rowStyles: {
                headerStyle: { font: { color: headerColor, weight: 'bold' } },
                summaryStyle: { font: { color: summaryColor, weight: 'bold' } },
              },
            }, { enableConditionalFormat: false, enableZebraStripe: false })
            
            const headerResult = engine.computeCellStyle({
              rowIndex: 0,
              columnKey: 'amount',
              value: 0,
              rowData: {},
              rowType: 'header',
            })
            
            const summaryResult = engine.computeCellStyle({
              rowIndex: 10,
              columnKey: 'amount',
              value: 0,
              rowData: {},
              rowType: 'summary',
            })
            
            return (
              headerResult.font?.color === headerColor &&
              headerResult.font?.weight === 'bold' &&
              summaryResult.font?.color === summaryColor &&
              summaryResult.font?.weight === 'bold'
            )
          }
        ),
        { numRuns: 50 }
      )
    })
  })
})

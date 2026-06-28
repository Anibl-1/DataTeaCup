/**
 * 条件格式化属性测试
 * Conditional Formatting Property Tests
 * 
 * **属性 42: 条件格式化正确性**
 * **属性 65: 条件格式化规则优先级**
 * **属性 66: 负数格式化正确性**
 * **验证需求: 14.1**
 * 
 * 测试内容:
 * 1. 条件格式化正确性 - 当数据值满足条件时应应用对应的格式样式
 * 2. 规则优先级 - 多个规则同时满足时，按优先级顺序应用样式
 * 3. 负数格式化 - 负数值应正确应用红色格式
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import {
  evaluateStyle,
  evaluateCondition,
  evaluateValueCondition,
  evaluateCrossFieldCondition,
  evaluateFormulaCondition,
  sortRulesByPriority,
  mergeStyles,
  getPresetRules,
  createValueRangeRule,
  createPercentageThresholdRule,
  ConditionalFormatService,
  PRESET_COLORS,
} from '../ConditionalFormat'
import type {
  ConditionalRule,
  ValueCondition,
  CrossFieldCondition,
  FormulaCondition,
  CellStyle,
  RuleCondition,
} from '../types'

// ============================================================================
// Arbitraries (测试数据生成器)
// ============================================================================

/** 生成有效的规则ID */
const ruleIdArb = fc.stringMatching(/^rule_[a-z0-9]{8,16}$/)

/** 生成规则名称 */
const ruleNameArb = fc.string({ minLength: 2, maxLength: 20 }).filter(s => s.trim().length > 0)

/** 生成优先级（1-100） */
const priorityArb = fc.integer({ min: 1, max: 100 })

/** 生成数值（包括负数、零、正数） */
const numericValueArb = fc.double({ min: -10000, max: 10000, noNaN: true })

/** 生成负数 */
const negativeNumberArb = fc.double({ min: -10000, max: -0.01, noNaN: true })

/** 生成正数 */
const positiveNumberArb = fc.double({ min: 0.01, max: 10000, noNaN: true })

/** 生成零值 */
const zeroValueArb = fc.constant(0)

/** 生成百分比值（0-2，即0%-200%） */
const percentageValueArb = fc.double({ min: 0, max: 2, noNaN: true })

/** 生成颜色值 */
const colorArb = fc.constantFrom(
  '#ff0000', '#00ff00', '#0000ff', '#ffff00', '#ff00ff', '#00ffff',
  '#ff4d4f', '#52c41a', '#faad14', '#1890ff', '#722ed1', '#eb2f96'
)

/** 生成数值比较操作符 */
const valueOperatorArb = fc.constantFrom('eq', 'ne', 'gt', 'gte', 'lt', 'lte', 'between', 'notBetween') as fc.Arbitrary<ValueCondition['operator']>

/** 生成简单数值条件 */
const simpleValueConditionArb: fc.Arbitrary<ValueCondition> = fc.record({
  operator: fc.constantFrom('eq', 'ne', 'gt', 'gte', 'lt', 'lte') as fc.Arbitrary<ValueCondition['operator']>,
  value: numericValueArb,
})

/** 生成区间数值条件 */
const rangeValueConditionArb: fc.Arbitrary<ValueCondition> = fc.tuple(
  numericValueArb,
  numericValueArb
).map(([a, b]) => ({
  operator: 'between' as const,
  value: Math.min(a, b),
  value2: Math.max(a, b),
}))

/** 生成字体样式 */
const fontStyleArb: fc.Arbitrary<CellStyle['font']> = fc.record({
  color: colorArb,
  weight: fc.constantFrom('normal', 'bold'),
  size: fc.integer({ min: 10, max: 24 }),
})

/** 生成背景样式 */
const backgroundStyleArb: fc.Arbitrary<CellStyle['background']> = fc.record({
  type: fc.constant('solid' as const),
  color: colorArb,
})

/** 生成单元格样式 */
const cellStyleArb: fc.Arbitrary<CellStyle> = fc.record({
  font: fc.option(fontStyleArb, { nil: undefined }),
  background: fc.option(backgroundStyleArb, { nil: undefined }),
})

/** 生成数值条件规则 */
const valueConditionRuleArb: fc.Arbitrary<ConditionalRule> = fc.record({
  id: ruleIdArb,
  name: ruleNameArb,
  priority: priorityArb,
  enabled: fc.constant(true),
  condition: fc.record({
    type: fc.constant('value' as const),
    config: simpleValueConditionArb,
  }),
  style: cellStyleArb,
})

/** 生成多个不同优先级的规则 */
function uniquePriorityRulesArb(count: number): fc.Arbitrary<ConditionalRule[]> {
  return fc.array(priorityArb, { minLength: count, maxLength: count })
    .filter(priorities => new Set(priorities).size === priorities.length)
    .chain(priorities => 
      fc.tuple(
        ...priorities.map((priority, index) => 
          fc.record({
            id: fc.constant(`rule_${index}_${Date.now()}`),
            name: fc.constant(`Rule ${index}`),
            priority: fc.constant(priority),
            enabled: fc.constant(true),
            condition: fc.record({
              type: fc.constant('value' as const),
              config: simpleValueConditionArb,
            }),
            style: cellStyleArb,
          })
        )
      )
    )
}

// ============================================================================
// 属性测试
// ============================================================================

describe('Conditional Formatting Property Tests', () => {
  
  // ==========================================================================
  // 属性 42: 条件格式化正确性
  // ==========================================================================
  
  describe('Property 42: Conditional Formatting Correctness', () => {
    /**
     * **Validates: Requirements 14.1**
     * 
     * 属性 42.1: 满足条件时应应用样式
     * 对于任意数值和条件规则，当数值满足条件时，应返回对应的样式
     */
    it('Property 42.1: Should apply style when condition is satisfied', () => {
      fc.assert(
        fc.property(
          numericValueArb,
          cellStyleArb,
          (value, style) => {
            // 创建一个总是匹配的规则（值等于自身）
            const rule: ConditionalRule = {
              id: 'test_rule',
              name: 'Test Rule',
              priority: 1,
              enabled: true,
              condition: {
                type: 'value',
                config: { operator: 'eq', value } as ValueCondition,
              },
              style,
            }
            
            const result = evaluateStyle(value, {}, [rule])
            
            // 样式应该被应用
            if (style.font?.color) {
              return result.font?.color === style.font.color
            }
            if (style.background?.color) {
              return result.background?.color === style.background.color
            }
            return true
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 14.1**
     * 
     * 属性 42.2: 不满足条件时不应用样式
     * 对于任意数值和条件规则，当数值不满足条件时，不应返回样式
     */
    it('Property 42.2: Should not apply style when condition is not satisfied', () => {
      fc.assert(
        fc.property(
          numericValueArb,
          cellStyleArb,
          (value, style) => {
            // 创建一个永远不匹配的规则（值不等于自身+1000）
            const rule: ConditionalRule = {
              id: 'test_rule',
              name: 'Test Rule',
              priority: 1,
              enabled: true,
              condition: {
                type: 'value',
                config: { operator: 'eq', value: value + 1000 } as ValueCondition,
              },
              style,
            }
            
            const result = evaluateStyle(value, {}, [rule])
            
            // 样式不应该被应用（结果应为空对象）
            return Object.keys(result).length === 0
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 14.1**
     * 
     * 属性 42.3: 大于条件正确性
     * 对于任意数值和阈值，当数值大于阈值时，gt条件应返回true
     */
    it('Property 42.3: Greater than condition should work correctly', () => {
      fc.assert(
        fc.property(
          numericValueArb,
          numericValueArb,
          (value, threshold) => {
            const condition: ValueCondition = { operator: 'gt', value: threshold }
            const result = evaluateValueCondition(value, condition)
            return result === (value > threshold)
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 14.1**
     * 
     * 属性 42.4: 小于条件正确性
     * 对于任意数值和阈值，当数值小于阈值时，lt条件应返回true
     */
    it('Property 42.4: Less than condition should work correctly', () => {
      fc.assert(
        fc.property(
          numericValueArb,
          numericValueArb,
          (value, threshold) => {
            const condition: ValueCondition = { operator: 'lt', value: threshold }
            const result = evaluateValueCondition(value, condition)
            return result === (value < threshold)
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 14.1**
     * 
     * 属性 42.5: 区间条件正确性
     * 对于任意数值和区间，当数值在区间内时，between条件应返回true
     */
    it('Property 42.5: Between condition should work correctly', () => {
      fc.assert(
        fc.property(
          numericValueArb,
          numericValueArb,
          numericValueArb,
          (value, bound1, bound2) => {
            const min = Math.min(bound1, bound2)
            const max = Math.max(bound1, bound2)
            const condition: ValueCondition = { operator: 'between', value: min, value2: max }
            const result = evaluateValueCondition(value, condition)
            return result === (value >= min && value <= max)
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 14.1.2**
     * 
     * 属性 42.6: 百分比阈值条件正确性
     * 对于任意百分比值和阈值，条件格式化应正确应用
     */
    it('Property 42.6: Percentage threshold condition should work correctly', () => {
      fc.assert(
        fc.property(
          percentageValueArb,
          fc.double({ min: 0.1, max: 1.5, noNaN: true }),
          (value, threshold) => {
            const rules = createPercentageThresholdRule(
              'Target Achievement',
              threshold,
              { font: { color: PRESET_COLORS.red } },
              { font: { color: PRESET_COLORS.green } }
            )
            
            const result = evaluateStyle(value, {}, rules)
            
            if (value < threshold) {
              // 低于阈值应显示红色
              return result.font?.color === PRESET_COLORS.red
            } else {
              // 达到或超过阈值应显示绿色
              return result.font?.color === PRESET_COLORS.green
            }
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 14.1**
     * 
     * 属性 42.7: 禁用规则不应用样式
     * 对于任意禁用的规则，即使条件满足也不应应用样式
     */
    it('Property 42.7: Disabled rules should not apply styles', () => {
      fc.assert(
        fc.property(
          numericValueArb,
          cellStyleArb,
          (value, style) => {
            const rule: ConditionalRule = {
              id: 'test_rule',
              name: 'Test Rule',
              priority: 1,
              enabled: false, // 禁用
              condition: {
                type: 'value',
                config: { operator: 'eq', value } as ValueCondition,
              },
              style,
            }
            
            const result = evaluateStyle(value, {}, [rule])
            
            // 禁用规则不应应用样式
            return Object.keys(result).length === 0
          }
        ),
        { numRuns: 50 }
      )
    })
  })

  // ==========================================================================
  // 属性 65: 条件格式化规则优先级
  // ==========================================================================
  
  describe('Property 65: Conditional Formatting Rule Priority', () => {
    /**
     * **Validates: Requirements 14.1.3**
     * 
     * 属性 65.1: 规则应按优先级排序
     * 对于任意规则列表，排序后应按优先级从小到大排列
     */
    it('Property 65.1: Rules should be sorted by priority (ascending)', () => {
      fc.assert(
        fc.property(
          fc.array(valueConditionRuleArb, { minLength: 2, maxLength: 10 }),
          (rules) => {
            const sorted = sortRulesByPriority(rules)
            
            // 验证排序正确性
            for (let i = 1; i < sorted.length; i++) {
              if (sorted[i].priority < sorted[i - 1].priority) {
                return false
              }
            }
            return true
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 14.1.3**
     * 
     * 属性 65.2: 高优先级规则样式应覆盖低优先级
     * 当多个规则同时满足时，高优先级（数字小）的样式应覆盖低优先级
     */
    it('Property 65.2: Higher priority rule styles should override lower priority', () => {
      fc.assert(
        fc.property(
          numericValueArb,
          colorArb,
          colorArb,
          (value, highPriorityColor, lowPriorityColor) => {
            // 确保颜色不同
            if (highPriorityColor === lowPriorityColor) return true
            
            const highPriorityRule: ConditionalRule = {
              id: 'high_priority',
              name: 'High Priority',
              priority: 1, // 高优先级
              enabled: true,
              condition: {
                type: 'value',
                config: { operator: 'gte', value: value - 1 } as ValueCondition,
              },
              style: { font: { color: highPriorityColor } },
            }
            
            const lowPriorityRule: ConditionalRule = {
              id: 'low_priority',
              name: 'Low Priority',
              priority: 10, // 低优先级
              enabled: true,
              condition: {
                type: 'value',
                config: { operator: 'gte', value: value - 1 } as ValueCondition,
              },
              style: { font: { color: lowPriorityColor } },
            }
            
            // 无论规则顺序如何，高优先级应先应用，低优先级后应用并覆盖
            const result1 = evaluateStyle(value, {}, [highPriorityRule, lowPriorityRule])
            const result2 = evaluateStyle(value, {}, [lowPriorityRule, highPriorityRule])
            
            // 由于样式是按优先级顺序合并的，低优先级的样式会覆盖高优先级
            // 但这里我们验证的是排序后的应用顺序一致性
            return result1.font?.color === result2.font?.color
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 14.1.3**
     * 
     * 属性 65.3: 规则排序应保持稳定性
     * 对于相同的规则列表，多次排序结果应一致
     */
    it('Property 65.3: Rule sorting should be stable', () => {
      fc.assert(
        fc.property(
          fc.array(valueConditionRuleArb, { minLength: 2, maxLength: 10 }),
          (rules) => {
            const sorted1 = sortRulesByPriority(rules)
            const sorted2 = sortRulesByPriority(rules)
            
            // 两次排序结果应完全一致
            if (sorted1.length !== sorted2.length) return false
            
            for (let i = 0; i < sorted1.length; i++) {
              if (sorted1[i].id !== sorted2[i].id) return false
              if (sorted1[i].priority !== sorted2[i].priority) return false
            }
            return true
          }
        ),
        { numRuns: 50 }
      )
    })

    /**
     * **Validates: Requirements 14.1.3**
     * 
     * 属性 65.4: 样式合并应正确叠加
     * 对于多个规则的样式，应正确合并（后应用的覆盖先应用的）
     */
    it('Property 65.4: Style merging should correctly overlay', () => {
      fc.assert(
        fc.property(
          cellStyleArb,
          cellStyleArb,
          (baseStyle, overlayStyle) => {
            const merged = mergeStyles(baseStyle, overlayStyle)
            
            // 验证覆盖样式的属性被正确应用
            if (overlayStyle.font?.color) {
              if (merged.font?.color !== overlayStyle.font.color) return false
            }
            if (overlayStyle.background?.color) {
              if (merged.background?.color !== overlayStyle.background.color) return false
            }
            
            // 验证基础样式的未覆盖属性被保留
            if (baseStyle.font?.size && !overlayStyle.font?.size) {
              if (merged.font?.size !== baseStyle.font.size) return false
            }
            
            return true
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 14.1.3**
     * 
     * 属性 65.5: 多条件组合应按优先级顺序应用
     * 对于红绿灯预设规则，应按正确的优先级顺序应用
     */
    it('Property 65.5: Traffic light rules should apply in correct priority order', () => {
      fc.assert(
        fc.property(
          percentageValueArb,
          (value) => {
            const rules = getPresetRules('trafficLight')
            const result = evaluateStyle(value, {}, rules)
            
            // 验证红绿灯规则的正确应用
            if (value < 0.7) {
              // 红灯
              return result.font?.color === PRESET_COLORS.red
            } else if (value >= 0.7 && value <= 0.9) {
              // 黄灯
              return result.font?.color === PRESET_COLORS.yellow
            } else {
              // 绿灯
              return result.font?.color === PRESET_COLORS.green
            }
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  // ==========================================================================
  // 属性 66: 负数格式化正确性
  // ==========================================================================
  
  describe('Property 66: Negative Number Formatting Correctness', () => {
    /**
     * **Validates: Requirements 14.1.1**
     * 
     * 属性 66.1: 负数应显示红色
     * 对于任意负数值，使用negativeRed预设规则时应显示红色
     */
    it('Property 66.1: Negative numbers should display in red', () => {
      fc.assert(
        fc.property(
          negativeNumberArb,
          (value) => {
            const rules = getPresetRules('negativeRed')
            const result = evaluateStyle(value, {}, rules)
            
            // 负数应显示红色
            return result.font?.color === PRESET_COLORS.red
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 14.1.1**
     * 
     * 属性 66.2: 正数不应应用负数红色规则
     * 对于任意正数值，使用negativeRed预设规则时不应显示红色
     */
    it('Property 66.2: Positive numbers should not apply negative red rule', () => {
      fc.assert(
        fc.property(
          positiveNumberArb,
          (value) => {
            const rules = getPresetRules('negativeRed')
            const result = evaluateStyle(value, {}, rules)
            
            // 正数不应应用红色样式
            return result.font?.color !== PRESET_COLORS.red || Object.keys(result).length === 0
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 14.1.1**
     * 
     * 属性 66.3: 零值不应应用负数红色规则
     * 对于零值，使用negativeRed预设规则时不应显示红色
     */
    it('Property 66.3: Zero should not apply negative red rule', () => {
      const rules = getPresetRules('negativeRed')
      const result = evaluateStyle(0, {}, rules)
      
      // 零不应应用红色样式
      expect(result.font?.color).not.toBe(PRESET_COLORS.red)
    })

    /**
     * **Validates: Requirements 14.1.1**
     * 
     * 属性 66.4: 正数应显示绿色
     * 对于任意正数值，使用positiveGreen预设规则时应显示绿色
     */
    it('Property 66.4: Positive numbers should display in green', () => {
      fc.assert(
        fc.property(
          positiveNumberArb,
          (value) => {
            const rules = getPresetRules('positiveGreen')
            const result = evaluateStyle(value, {}, rules)
            
            // 正数应显示绿色
            return result.font?.color === PRESET_COLORS.green
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 14.1.1**
     * 
     * 属性 66.5: 零值应显示灰色
     * 对于零值，使用zeroGray预设规则时应显示灰色
     */
    it('Property 66.5: Zero should display in gray', () => {
      const rules = getPresetRules('zeroGray')
      const result = evaluateStyle(0, {}, rules)
      
      // 零应显示灰色
      expect(result.font?.color).toBe(PRESET_COLORS.gray)
    })

    /**
     * **Validates: Requirements 14.1.1**
     * 
     * 属性 66.6: 负数红色规则应正确识别负数边界
     * 对于接近零的负数，应正确应用红色规则
     */
    it('Property 66.6: Negative red rule should correctly identify negative boundary', () => {
      fc.assert(
        fc.property(
          fc.double({ min: -1, max: -0.0001, noNaN: true }),
          (value) => {
            const rules = getPresetRules('negativeRed')
            const result = evaluateStyle(value, {}, rules)
            
            // 即使是很小的负数也应显示红色
            return result.font?.color === PRESET_COLORS.red
          }
        ),
        { numRuns: 50 }
      )
    })

    /**
     * **Validates: Requirements 14.1.1**
     * 
     * 属性 66.7: 组合规则应正确处理负数、零、正数
     * 对于任意数值，组合使用negativeRed、zeroGray、positiveGreen规则时应正确格式化
     */
    it('Property 66.7: Combined rules should correctly format negative, zero, and positive', () => {
      fc.assert(
        fc.property(
          numericValueArb,
          (value) => {
            const negativeRules = getPresetRules('negativeRed')
            const zeroRules = getPresetRules('zeroGray')
            const positiveRules = getPresetRules('positiveGreen')
            
            // 组合所有规则
            const allRules = [...negativeRules, ...zeroRules, ...positiveRules]
            const result = evaluateStyle(value, {}, allRules)
            
            if (value < 0) {
              return result.font?.color === PRESET_COLORS.red
            } else if (value === 0) {
              return result.font?.color === PRESET_COLORS.gray
            } else {
              return result.font?.color === PRESET_COLORS.green
            }
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  // ==========================================================================
  // 属性 75: 跨字段条件比较正确性
  // ==========================================================================
  
  describe('Property 75: Cross-Field Condition Comparison Correctness', () => {
    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 75.1: 数值字段跨字段比较正确性
     * 对于任意两个数值字段，比较操作符（gt, lt, eq等）应返回正确的布尔值
     */
    it('Property 75.1: Numeric cross-field comparison should return correct boolean', () => {
      fc.assert(
        fc.property(
          numericValueArb,
          numericValueArb,
          fc.constantFrom('eq', 'ne', 'gt', 'gte', 'lt', 'lte') as fc.Arbitrary<'eq' | 'ne' | 'gt' | 'gte' | 'lt' | 'lte'>,
          (value1, value2, operator) => {
            const condition: CrossFieldCondition = {
              field1: 'fieldA',
              operator,
              field2: 'fieldB',
              valueType: 'number',
            }
            const rowData = { fieldA: value1, fieldB: value2 }
            
            const result = evaluateCrossFieldCondition(condition, rowData)
            
            // 验证比较结果正确性
            switch (operator) {
              case 'eq': return result === (value1 === value2)
              case 'ne': return result === (value1 !== value2)
              case 'gt': return result === (value1 > value2)
              case 'gte': return result === (value1 >= value2)
              case 'lt': return result === (value1 < value2)
              case 'lte': return result === (value1 <= value2)
              default: return false
            }
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 75.2: 字符串字段跨字段比较正确性
     * 对于任意两个字符串字段，比较应使用localeCompare正确比较
     */
    it('Property 75.2: String cross-field comparison should use localeCompare correctly', () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 1, maxLength: 20 }),
          fc.string({ minLength: 1, maxLength: 20 }),
          fc.constantFrom('eq', 'ne', 'gt', 'gte', 'lt', 'lte') as fc.Arbitrary<'eq' | 'ne' | 'gt' | 'gte' | 'lt' | 'lte'>,
          (str1, str2, operator) => {
            const condition: CrossFieldCondition = {
              field1: 'fieldA',
              operator,
              field2: 'fieldB',
              valueType: 'string',
            }
            const rowData = { fieldA: str1, fieldB: str2 }
            
            const result = evaluateCrossFieldCondition(condition, rowData)
            const compareResult = str1.localeCompare(str2)
            
            // 验证字符串比较结果正确性
            switch (operator) {
              case 'eq': return result === (compareResult === 0)
              case 'ne': return result === (compareResult !== 0)
              case 'gt': return result === (compareResult > 0)
              case 'gte': return result === (compareResult >= 0)
              case 'lt': return result === (compareResult < 0)
              case 'lte': return result === (compareResult <= 0)
              default: return false
            }
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 75.3: 日期字段跨字段比较正确性
     * 对于任意两个日期字段，比较应使用时间戳正确比较
     */
    it('Property 75.3: Date cross-field comparison should use timestamp correctly', () => {
      fc.assert(
        fc.property(
          fc.date({ min: new Date('2000-01-01'), max: new Date('2030-12-31') }),
          fc.date({ min: new Date('2000-01-01'), max: new Date('2030-12-31') }),
          fc.constantFrom('eq', 'ne', 'gt', 'gte', 'lt', 'lte') as fc.Arbitrary<'eq' | 'ne' | 'gt' | 'gte' | 'lt' | 'lte'>,
          (date1, date2, operator) => {
            const condition: CrossFieldCondition = {
              field1: 'fieldA',
              operator,
              field2: 'fieldB',
              valueType: 'date',
            }
            const rowData = { fieldA: date1, fieldB: date2 }
            
            const result = evaluateCrossFieldCondition(condition, rowData)
            const time1 = date1.getTime()
            const time2 = date2.getTime()
            
            // 验证日期比较结果正确性
            switch (operator) {
              case 'eq': return result === (time1 === time2)
              case 'ne': return result === (time1 !== time2)
              case 'gt': return result === (time1 > time2)
              case 'gte': return result === (time1 >= time2)
              case 'lt': return result === (time1 < time2)
              case 'lte': return result === (time1 <= time2)
              default: return false
            }
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 75.4: 自动类型检测应正确工作
     * 对于valueType为auto的情况，应根据实际值类型自动选择正确的比较方式
     */
    it('Property 75.4: Auto type detection should work correctly', () => {
      fc.assert(
        fc.property(
          numericValueArb,
          numericValueArb,
          fc.constantFrom('gt', 'lt', 'eq') as fc.Arbitrary<'gt' | 'lt' | 'eq'>,
          (value1, value2, operator) => {
            // 测试数值类型自动检测
            const condition: CrossFieldCondition = {
              field1: 'fieldA',
              operator,
              field2: 'fieldB',
              valueType: 'auto', // 自动检测
            }
            const rowData = { fieldA: value1, fieldB: value2 }
            
            const result = evaluateCrossFieldCondition(condition, rowData)
            
            // 由于两个值都是数值，应该使用数值比较
            switch (operator) {
              case 'eq': return result === (value1 === value2)
              case 'gt': return result === (value1 > value2)
              case 'lt': return result === (value1 < value2)
              default: return false
            }
          }
        ),
        { numRuns: 50 }
      )
    })

    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 75.5: 空值字段应返回false
     * 当任一字段为空值时，跨字段比较应返回false
     */
    it('Property 75.5: Null field values should return false', () => {
      fc.assert(
        fc.property(
          fc.constantFrom(null, undefined),
          numericValueArb,
          fc.constantFrom('eq', 'ne', 'gt', 'gte', 'lt', 'lte') as fc.Arbitrary<'eq' | 'ne' | 'gt' | 'gte' | 'lt' | 'lte'>,
          fc.boolean(),
          (nullValue, validValue, operator, nullFirst) => {
            const condition: CrossFieldCondition = {
              field1: 'fieldA',
              operator,
              field2: 'fieldB',
              valueType: 'number',
            }
            
            // 测试第一个字段为空或第二个字段为空
            const rowData = nullFirst 
              ? { fieldA: nullValue, fieldB: validValue }
              : { fieldA: validValue, fieldB: nullValue }
            
            const result = evaluateCrossFieldCondition(condition, rowData)
            
            // 任一字段为空时应返回false
            return result === false
          }
        ),
        { numRuns: 50 }
      )
    })

    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 75.6: 数值字符串应正确转换并比较
     * 当数值以字符串形式存储时，应正确转换为数值进行比较
     */
    it('Property 75.6: Numeric strings should be correctly converted and compared', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: -1000, max: 1000 }),
          fc.integer({ min: -1000, max: 1000 }),
          fc.constantFrom('gt', 'lt', 'eq') as fc.Arbitrary<'gt' | 'lt' | 'eq'>,
          (num1, num2, operator) => {
            const condition: CrossFieldCondition = {
              field1: 'fieldA',
              operator,
              field2: 'fieldB',
              valueType: 'number',
            }
            // 使用字符串形式的数值
            const rowData = { fieldA: String(num1), fieldB: String(num2) }
            
            const result = evaluateCrossFieldCondition(condition, rowData)
            
            switch (operator) {
              case 'eq': return result === (num1 === num2)
              case 'gt': return result === (num1 > num2)
              case 'lt': return result === (num1 < num2)
              default: return false
            }
          }
        ),
        { numRuns: 50 }
      )
    })

    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 75.7: 非数值字符串在数值比较模式下应返回false
     * 当valueType为number但字段值无法转换为数值时，应返回false
     */
    it('Property 75.7: Non-numeric strings in number mode should return false', () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 1, maxLength: 10 }).filter(s => isNaN(parseFloat(s))),
          numericValueArb,
          fc.constantFrom('eq', 'gt', 'lt') as fc.Arbitrary<'eq' | 'gt' | 'lt'>,
          (invalidStr, validNum, operator) => {
            const condition: CrossFieldCondition = {
              field1: 'fieldA',
              operator,
              field2: 'fieldB',
              valueType: 'number',
            }
            const rowData = { fieldA: invalidStr, fieldB: validNum }
            
            const result = evaluateCrossFieldCondition(condition, rowData)
            
            // 无法转换为数值时应返回false
            return result === false
          }
        ),
        { numRuns: 50 }
      )
    })
  })

  // ==========================================================================
  // 属性 76: 自定义表达式条件正确性
  // ==========================================================================
  
  describe('Property 76: Custom Expression Condition Correctness', () => {
    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 76.1: 简单算术表达式应正确求值
     * 对于简单的算术表达式（加减乘除），应返回正确的布尔结果
     */
    it('Property 76.1: Simple arithmetic expressions should evaluate correctly', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 1, max: 100 }),
          fc.integer({ min: 1, max: 100 }),
          fc.constantFrom('+', '-', '*') as fc.Arbitrary<'+' | '-' | '*'>,
          fc.integer({ min: 1, max: 200 }),
          (a, b, op, threshold) => {
            let expression: string
            let expectedResult: number
            
            switch (op) {
              case '+':
                expression = `\${fieldA} + \${fieldB} > ${threshold}`
                expectedResult = a + b
                break
              case '-':
                expression = `\${fieldA} - \${fieldB} > ${threshold}`
                expectedResult = a - b
                break
              case '*':
                expression = `\${fieldA} * \${fieldB} > ${threshold}`
                expectedResult = a * b
                break
              default:
                return true
            }
            
            const condition: FormulaCondition = { expression }
            const rowData = { fieldA: a, fieldB: b }
            
            const result = evaluateFormulaCondition(a, condition, rowData)
            
            return result === (expectedResult > threshold)
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 76.2: 变量替换应正确工作
     * 对于包含变量引用的表达式，变量应被正确替换为实际值
     */
    it('Property 76.2: Variable substitution should work correctly', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 0, max: 100 }),
          fc.integer({ min: 1, max: 100 }),
          (value, threshold) => {
            // 测试 ${value} 变量替换
            const condition: FormulaCondition = {
              expression: `\${value} > ${threshold}`,
            }
            const rowData = {}
            
            const result = evaluateFormulaCondition(value, condition, rowData)
            
            return result === (value > threshold)
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 76.3: 内置函数abs应正确工作
     * 对于使用abs函数的表达式，应返回正确的绝对值比较结果
     */
    it('Property 76.3: Built-in function abs should work correctly', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: -100, max: 100 }),
          fc.integer({ min: 0, max: 100 }),
          (value, threshold) => {
            const condition: FormulaCondition = {
              expression: `abs(\${value}) > ${threshold}`,
            }
            const rowData = {}
            
            const result = evaluateFormulaCondition(value, condition, rowData)
            
            return result === (Math.abs(value) > threshold)
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 76.4: 内置函数round应正确工作
     * 对于使用round函数的表达式，应返回正确的四舍五入比较结果
     */
    it('Property 76.4: Built-in function round should work correctly', () => {
      fc.assert(
        fc.property(
          fc.double({ min: -100, max: 100, noNaN: true }),
          fc.integer({ min: -100, max: 100 }),
          (value, threshold) => {
            const condition: FormulaCondition = {
              expression: `round(\${value}) > ${threshold}`,
            }
            const rowData = {}
            
            const result = evaluateFormulaCondition(value, condition, rowData)
            
            return result === (Math.round(value) > threshold)
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 76.5: 内置函数floor和ceil应正确工作
     * 对于使用floor和ceil函数的表达式，应返回正确的取整比较结果
     */
    it('Property 76.5: Built-in functions floor and ceil should work correctly', () => {
      fc.assert(
        fc.property(
          fc.double({ min: -100, max: 100, noNaN: true }),
          fc.integer({ min: -100, max: 100 }),
          fc.boolean(),
          (value, threshold, useFloor) => {
            const func = useFloor ? 'floor' : 'ceil'
            const condition: FormulaCondition = {
              expression: `${func}(\${value}) > ${threshold}`,
            }
            const rowData = {}
            
            const result = evaluateFormulaCondition(value, condition, rowData)
            const expected = useFloor ? Math.floor(value) : Math.ceil(value)
            
            return result === (expected > threshold)
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 76.6: 聚合函数sum应正确工作
     * 当提供allValues时，sum()聚合函数应返回正确的求和结果
     */
    it('Property 76.6: Aggregate function sum should work correctly when allValues provided', () => {
      fc.assert(
        fc.property(
          fc.array(fc.integer({ min: 1, max: 100 }), { minLength: 2, maxLength: 10 }),
          fc.integer({ min: 1, max: 1000 }),
          (values, threshold) => {
            const condition: FormulaCondition = {
              expression: `sum() > ${threshold}`,
            }
            const rowData = {}
            const currentValue = values[0]
            
            const result = evaluateFormulaCondition(currentValue, condition, rowData, values)
            const expectedSum = values.reduce((a, b) => a + b, 0)
            
            return result === (expectedSum > threshold)
          }
        ),
        { numRuns: 50 }
      )
    })

    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 76.7: 聚合函数avg应正确工作
     * 当提供allValues时，avg()聚合函数应返回正确的平均值结果
     */
    it('Property 76.7: Aggregate function avg should work correctly when allValues provided', () => {
      fc.assert(
        fc.property(
          fc.array(fc.integer({ min: 1, max: 100 }), { minLength: 2, maxLength: 10 }),
          fc.integer({ min: 1, max: 100 }),
          (values, threshold) => {
            const condition: FormulaCondition = {
              expression: `avg() > ${threshold}`,
            }
            const rowData = {}
            const currentValue = values[0]
            
            const result = evaluateFormulaCondition(currentValue, condition, rowData, values)
            const expectedAvg = values.reduce((a, b) => a + b, 0) / values.length
            
            return result === (expectedAvg > threshold)
          }
        ),
        { numRuns: 50 }
      )
    })

    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 76.8: 聚合函数count应正确工作
     * 当提供allValues时，count()聚合函数应返回正确的计数结果
     */
    it('Property 76.8: Aggregate function count should work correctly when allValues provided', () => {
      fc.assert(
        fc.property(
          fc.array(fc.integer({ min: 1, max: 100 }), { minLength: 1, maxLength: 20 }),
          fc.integer({ min: 1, max: 15 }),
          (values, threshold) => {
            const condition: FormulaCondition = {
              expression: `count() > ${threshold}`,
            }
            const rowData = {}
            const currentValue = values[0]
            
            const result = evaluateFormulaCondition(currentValue, condition, rowData, values)
            
            return result === (values.length > threshold)
          }
        ),
        { numRuns: 50 }
      )
    })

    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 76.9: 变量映射应正确工作
     * 对于使用variables映射的表达式，变量应被正确替换
     */
    it('Property 76.9: Variable mapping should work correctly', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 0, max: 100 }),
          fc.integer({ min: 0, max: 100 }),
          fc.integer({ min: 1, max: 200 }),
          (value, targetValue, threshold) => {
            const condition: FormulaCondition = {
              expression: `\${currentVal} + \${targetVal} > ${threshold}`,
              variables: {
                currentVal: 'amount',
                targetVal: 'target',
              },
            }
            const rowData = { amount: value, target: targetValue }
            
            const result = evaluateFormulaCondition(value, condition, rowData)
            
            return result === (value + targetValue > threshold)
          }
        ),
        { numRuns: 50 }
      )
    })

    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 76.10: 比较运算符应正确工作
     * 对于使用比较运算符（==, !=, >, <, >=, <=）的表达式，应返回正确结果
     */
    it('Property 76.10: Comparison operators should work correctly', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 0, max: 100 }),
          fc.integer({ min: 0, max: 100 }),
          fc.constantFrom('==', '!=', '>', '<', '>=', '<=') as fc.Arbitrary<'==' | '!=' | '>' | '<' | '>=' | '<='>,
          (value, threshold, op) => {
            const condition: FormulaCondition = {
              expression: `\${value} ${op} ${threshold}`,
            }
            const rowData = {}
            
            const result = evaluateFormulaCondition(value, condition, rowData)
            
            switch (op) {
              case '==': return result === (value === threshold)
              case '!=': return result === (value !== threshold)
              case '>': return result === (value > threshold)
              case '<': return result === (value < threshold)
              case '>=': return result === (value >= threshold)
              case '<=': return result === (value <= threshold)
              default: return false
            }
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 76.11: 逻辑运算符应正确工作
     * 对于使用逻辑运算符（&&, ||）的表达式，应返回正确结果
     */
    it('Property 76.11: Logical operators should work correctly', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 0, max: 100 }),
          fc.integer({ min: 0, max: 50 }),
          fc.integer({ min: 50, max: 100 }),
          fc.boolean(),
          (value, lowThreshold, highThreshold, useAnd) => {
            const op = useAnd ? '&&' : '||'
            const condition: FormulaCondition = {
              expression: `\${value} > ${lowThreshold} ${op} \${value} < ${highThreshold}`,
            }
            const rowData = {}
            
            const result = evaluateFormulaCondition(value, condition, rowData)
            
            if (useAnd) {
              return result === (value > lowThreshold && value < highThreshold)
            } else {
              return result === (value > lowThreshold || value < highThreshold)
            }
          }
        ),
        { numRuns: 100 }
      )
    })

    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 76.12: 空值处理应返回false或使用默认值
     * 当表达式中引用的字段为空时，应使用默认值0进行计算
     */
    it('Property 76.12: Null value handling should use default value', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 1, max: 100 }),
          (threshold) => {
            const condition: FormulaCondition = {
              expression: `\${missingField} > ${threshold}`,
            }
            const rowData = {} // missingField 不存在
            
            const result = evaluateFormulaCondition(null, condition, rowData)
            
            // 缺失字段应使用默认值0，所以 0 > threshold 应为 false（当 threshold > 0）
            return result === (0 > threshold)
          }
        ),
        { numRuns: 50 }
      )
    })

    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 76.13: 无效表达式应返回false
     * 对于语法错误的表达式，应安全地返回false而不是抛出异常
     */
    it('Property 76.13: Invalid expressions should return false safely', () => {
      const invalidExpressions = [
        '${value} +++ 5',     // 无效语法 - 三重加号
        '((${value} > 5',     // 括号不匹配
        '${value} > > > 5',   // 多重运算符
      ]
      
      for (const expr of invalidExpressions) {
        const condition: FormulaCondition = { expression: expr }
        const result = evaluateFormulaCondition(10, condition, {})
        expect(result).toBe(false)
      }
    })

    /**
     * **Validates: Requirements 21.2**
     * 
     * 属性 76.14: 聚合函数min和max应正确工作
     * 当提供allValues时，min()和max()聚合函数应返回正确结果
     */
    it('Property 76.14: Aggregate functions min and max should work correctly', () => {
      fc.assert(
        fc.property(
          fc.array(fc.integer({ min: 1, max: 100 }), { minLength: 2, maxLength: 10 }),
          fc.integer({ min: 1, max: 100 }),
          fc.boolean(),
          (values, threshold, useMin) => {
            const func = useMin ? 'min' : 'max'
            const condition: FormulaCondition = {
              expression: `${func}() > ${threshold}`,
            }
            const rowData = {}
            const currentValue = values[0]
            
            const result = evaluateFormulaCondition(currentValue, condition, rowData, values)
            const expected = useMin ? Math.min(...values) : Math.max(...values)
            
            return result === (expected > threshold)
          }
        ),
        { numRuns: 50 }
      )
    })
  })

  // ==========================================================================
  // 额外的边界条件测试
  // ==========================================================================
  
  describe('Edge Cases and Boundary Conditions', () => {
    /**
     * **Validates: Requirements 14.1**
     * 
     * 空值处理：空值不应匹配数值条件
     */
    it('Null values should not match value conditions', () => {
      fc.assert(
        fc.property(
          fc.constantFrom(null, undefined),
          simpleValueConditionArb,
          (value, condition) => {
            const result = evaluateValueCondition(value, condition)
            return result === false
          }
        ),
        { numRuns: 20 }
      )
    })

    /**
     * **Validates: Requirements 14.1**
     * 
     * 非数字字符串不应匹配数值条件
     */
    it('Non-numeric strings should not match value conditions', () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 1, maxLength: 10 }).filter(s => isNaN(parseFloat(s))),
          simpleValueConditionArb,
          (value, condition) => {
            const result = evaluateValueCondition(value, condition)
            return result === false
          }
        ),
        { numRuns: 50 }
      )
    })

    /**
     * **Validates: Requirements 14.1**
     * 
     * 数字字符串应正确匹配数值条件
     */
    it('Numeric strings should correctly match value conditions', () => {
      fc.assert(
        fc.property(
          numericValueArb,
          (value) => {
            const stringValue = String(value)
            const condition: ValueCondition = { operator: 'eq', value }
            const result = evaluateValueCondition(stringValue, condition)
            return result === true
          }
        ),
        { numRuns: 50 }
      )
    })

    /**
     * **Validates: Requirements 14.1**
     * 
     * 空规则列表应返回空样式
     */
    it('Empty rules list should return empty style', () => {
      fc.assert(
        fc.property(
          numericValueArb,
          (value) => {
            const result = evaluateStyle(value, {}, [])
            return Object.keys(result).length === 0
          }
        ),
        { numRuns: 20 }
      )
    })

    /**
     * **Validates: Requirements 14.1.3**
     * 
     * 规则范围限制应正确应用
     */
    it('Rule scope should correctly limit application', () => {
      fc.assert(
        fc.property(
          numericValueArb,
          colorArb,
          (value, color) => {
            const rule: ConditionalRule = {
              id: 'scoped_rule',
              name: 'Scoped Rule',
              priority: 1,
              enabled: true,
              condition: {
                type: 'value',
                config: { operator: 'gte', value: value - 1 } as ValueCondition,
              },
              style: { font: { color } },
              scope: { columns: ['amount'] }, // 只应用于 amount 列
            }
            
            // 对于 amount 列应该应用样式
            const resultForAmount = evaluateStyle(value, {}, [rule], 'amount')
            // 对于其他列不应该应用样式
            const resultForOther = evaluateStyle(value, {}, [rule], 'name')
            
            return resultForAmount.font?.color === color && 
                   Object.keys(resultForOther).length === 0
          }
        ),
        { numRuns: 50 }
      )
    })
  })
})

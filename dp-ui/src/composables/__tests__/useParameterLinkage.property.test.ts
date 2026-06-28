/**
 * 参数级联属性测试
 * 
 * **属性 38: 参数级联正确性**
 * **验证需求: 13.1**
 * 
 * 对于任意参数联动配置，当源参数值变化时，目标参数的选项应正确更新。
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as fc from 'fast-check'
import {
  useParameterLinkage,
  createCascadeConfig,
  resetLinkageIdCounter,
  type ParameterOption,
  type CascadeConfig
} from '../useParameterLinkage'

// ============================================================================
// Arbitraries (Test Data Generators)
// ============================================================================

/**
 * 生成有效的参数名称
 */
const paramNameArb = fc.stringMatching(/^[a-z][a-z0-9_]{2,15}$/)

/**
 * 生成参数值（字符串或数字）
 */
const paramValueArb = fc.oneof(
  fc.string({ minLength: 1, maxLength: 20 }).filter(s => s.trim().length > 0),
  fc.integer({ min: 1, max: 10000 })
)

/**
 * 生成参数选项
 */
const paramOptionArb: fc.Arbitrary<ParameterOption> = fc.record({
  label: fc.string({ minLength: 1, maxLength: 30 }).filter(s => s.trim().length > 0),
  value: fc.oneof(
    fc.string({ minLength: 1, maxLength: 20 }).filter(s => s.trim().length > 0),
    fc.integer({ min: 1, max: 10000 })
  ),
  disabled: fc.boolean()
})

/**
 * 生成参数选项列表
 */
const paramOptionsArb = fc.array(paramOptionArb, { minLength: 1, maxLength: 10 })


/**
 * 生成级联配置
 */
const cascadeConfigArb: fc.Arbitrary<CascadeConfig> = fc.record({
  sourceField: fc.stringMatching(/^[a-z][a-z0-9_]{2,15}$/),
  targetField: fc.stringMatching(/^[a-z][a-z0-9_]{2,15}$/),
  dataSourceId: fc.integer({ min: 1, max: 100 }),
  sql: fc.constant('SELECT * FROM options WHERE parent_id = ${parentValue}'),
  labelField: fc.constant('label'),
  valueField: fc.constant('value')
})

/**
 * 生成级联层级数量（2-5级）
 */
const cascadeLevelCountArb = fc.integer({ min: 2, max: 5 })

/**
 * 生成唯一的参数名称列表
 */
function uniqueParamNamesArb(count: number): fc.Arbitrary<string[]> {
  return fc.array(paramNameArb, { minLength: count, maxLength: count })
    .filter(names => new Set(names).size === names.length)
}

// ============================================================================
// Property Tests
// ============================================================================

describe('Parameter Cascade Property Tests', () => {
  beforeEach(() => {
    resetLinkageIdCounter()
  })

  /**
   * **Validates: Requirements 13.1**
   * 
   * 属性 38.1: 子参数应在父参数变化时被清除
   * 对于任意父子参数关系，当父参数值变化时，子参数值应被清除为 null
   */
  it('Property 38.1: Child parameters should be cleared when parent changes', async () => {
    await fc.assert(
      fc.asyncProperty(
        fc.record({
          parentName: paramNameArb,
          childName: paramNameArb,
          parentValue1: paramValueArb,
          parentValue2: paramValueArb,
          childValue: paramValueArb,
          cascadeConfig: cascadeConfigArb
        }).filter(p => p.parentName !== p.childName && p.parentValue1 !== p.parentValue2),
        async (params) => {
          const { parentName, childName, parentValue1, parentValue2, childValue, cascadeConfig } = params
          
          const { registerParameter, addLinkage, setParameterValue, getParameterValue } = useParameterLinkage({
            autoClearChildren: true,
            debounceDelay: 0
          })

          // 注册参数
          registerParameter(parentName)
          registerParameter(childName)

          // 添加级联关系
          addLinkage({
            sourceParam: parentName,
            targetParam: childName,
            linkageType: 'cascade',
            linkageConfig: cascadeConfig,
            enabled: true
          })

          // 设置初始值
          await setParameterValue(parentName, parentValue1)
          await setParameterValue(childName, childValue)

          // 验证子参数有值
          expect(getParameterValue(childName)).toBe(childValue)

          // 改变父参数值（不同的值）
          await setParameterValue(parentName, parentValue2)

          // 子参数应被清除
          const childValueAfter = getParameterValue(childName)
          return childValueAfter === null
        }
      ),
      { numRuns: 50 }
    )
  })


  /**
   * **Validates: Requirements 13.1**
   * 
   * 属性 38.2: 级联层级应正确计算
   * 对于任意多级级联配置，每个参数的级联层级应等于其到根参数的最大路径长度
   */
  it('Property 38.2: Cascade levels should be calculated correctly', () => {
    fc.assert(
      fc.property(
        cascadeLevelCountArb.chain(count => uniqueParamNamesArb(count)),
        (names) => {
          const { registerParameter, addLinkage, getCascadeLevel } = useParameterLinkage()

          // 注册所有参数
          names.forEach(name => registerParameter(name))

          // 创建链式级联关系: param0 -> param1 -> param2 -> ...
          for (let i = 0; i < names.length - 1; i++) {
            addLinkage({
              sourceParam: names[i]!,
              targetParam: names[i + 1]!,
              linkageType: 'cascade',
              linkageConfig: createCascadeConfig({
                sourceField: `${names[i]!}_code`,
                targetField: `${names[i + 1]!}_code`,
                dataSourceId: 1,
                sql: 'SELECT * FROM options'
              }),
              enabled: true
            })
          }

          // 验证每个参数的级联层级
          for (let i = 0; i < names.length; i++) {
            const level = getCascadeLevel(names[i]!)
            if (level !== i) {
              return false
            }
          }

          return true
        }
      ),
      { numRuns: 50 }
    )
  })

  /**
   * **Validates: Requirements 13.1**
   * 
   * 属性 38.3: 依赖验证应正确工作
   * 对于任意级联配置，只有当所有父参数都有值时，依赖验证才应返回 true
   */
  it('Property 38.3: Dependency validation should work correctly', async () => {
    await fc.assert(
      fc.asyncProperty(
        fc.record({
          parentName: paramNameArb,
          childName: paramNameArb,
          hasParentValue: fc.boolean(),
          parentValue: paramValueArb,
          cascadeConfig: cascadeConfigArb
        }).filter(p => p.parentName !== p.childName),
        async (params) => {
          const { parentName, childName, hasParentValue, parentValue, cascadeConfig } = params
          
          const { registerParameter, addLinkage, setParameterValue, validateDependencies } = useParameterLinkage()

          // 注册参数
          registerParameter(parentName)
          registerParameter(childName)

          // 添加级联关系
          addLinkage({
            sourceParam: parentName,
            targetParam: childName,
            linkageType: 'cascade',
            linkageConfig: cascadeConfig,
            enabled: true
          })

          // 根据 hasParentValue 决定是否设置父参数值
          if (hasParentValue) {
            await setParameterValue(parentName, parentValue)
          }

          // 验证依赖
          const isValid = validateDependencies(childName)
          
          // 如果父参数有值，依赖应该有效；否则无效
          return isValid === hasParentValue
        }
      ),
      { numRuns: 50 }
    )
  })


  /**
   * **Validates: Requirements 13.1**
   * 
   * 属性 38.4: 多级级联应正确传播清除
   * 对于任意多级级联配置，当顶层参数变化时，所有下游参数都应被清除
   */
  it('Property 38.4: Multi-level cascade should propagate clearing correctly', async () => {
    await fc.assert(
      fc.asyncProperty(
        cascadeLevelCountArb.chain(count => 
          fc.record({
            names: uniqueParamNamesArb(count),
            values: fc.array(paramValueArb, { minLength: count, maxLength: count }),
            newRootValue: paramValueArb
          })
        ).filter(p => p.values[0] !== p.newRootValue), // 确保新值不同
        async (params) => {
          const { names, values, newRootValue } = params
          
          const { registerParameter, addLinkage, setParameterValue, getParameterValue } = useParameterLinkage({
            autoClearChildren: true,
            debounceDelay: 0
          })

          // 注册所有参数
          names.forEach(name => registerParameter(name))

          // 创建链式级联关系
          for (let i = 0; i < names.length - 1; i++) {
            addLinkage({
              sourceParam: names[i]!,
              targetParam: names[i + 1]!,
              linkageType: 'cascade',
              linkageConfig: createCascadeConfig({
                sourceField: `${names[i]!}_code`,
                targetField: `${names[i + 1]!}_code`,
                dataSourceId: 1,
                sql: 'SELECT * FROM options'
              }),
              enabled: true
            })
          }

          // 设置所有参数的初始值
          for (let i = 0; i < names.length; i++) {
            await setParameterValue(names[i]!, values[i]!)
          }

          // 改变根参数值
          await setParameterValue(names[0]!, newRootValue)

          // 根参数应该有新值
          if (getParameterValue(names[0]!) !== newRootValue) {
            return false
          }

          // 所有下游参数应该被清除
          for (let i = 1; i < names.length; i++) {
            const value = getParameterValue(names[i]!)
            if (value !== null) {
              return false
            }
          }

          return true
        }
      ),
      { numRuns: 30 }
    )
  })

  /**
   * **Validates: Requirements 13.1**
   * 
   * 属性 38.5: 根参数和叶子参数应正确识别
   * 对于任意级联配置，根参数应没有父参数，叶子参数应没有子参数
   */
  it('Property 38.5: Root and leaf parameters should be correctly identified', () => {
    fc.assert(
      fc.property(
        cascadeLevelCountArb.chain(count => uniqueParamNamesArb(count)),
        (names) => {
          const { registerParameter, addLinkage, getRootParameters, getLeafParameters, parameters } = useParameterLinkage()

          // 注册所有参数
          names.forEach(name => registerParameter(name))

          // 创建链式级联关系
          for (let i = 0; i < names.length - 1; i++) {
            addLinkage({
              sourceParam: names[i]!,
              targetParam: names[i + 1]!,
              linkageType: 'cascade',
              linkageConfig: createCascadeConfig({
                sourceField: `${names[i]!}_code`,
                targetField: `${names[i + 1]!}_code`,
                dataSourceId: 1,
                sql: 'SELECT * FROM options'
              }),
              enabled: true
            })
          }

          const roots = getRootParameters()
          const leaves = getLeafParameters()

          // 第一个参数应该是根参数
          if (!roots.includes(names[0]!)) {
            return false
          }

          // 最后一个参数应该是叶子参数
          if (!leaves.includes(names[names.length - 1]!)) {
            return false
          }

          // 中间参数不应该是根或叶子
          for (let i = 1; i < names.length - 1; i++) {
            if (roots.includes(names[i]!) || leaves.includes(names[i]!)) {
              return false
            }
          }

          // 验证父子关系
          for (let i = 0; i < names.length - 1; i++) {
            const parent = parameters.value[names[i]!]
            const child = parameters.value[names[i + 1]!]
            
            if (!parent!.children.includes(names[i + 1]!) || !child!.parents.includes(names[i]!)) {
              return false
            }
          }

          return true
        }
      ),
      { numRuns: 50 }
    )
  })


  /**
   * **Validates: Requirements 13.1**
   * 
   * 属性 38.6: 依赖值应正确收集
   * 对于任意多级级联配置，getDependencyValues 应返回所有祖先参数的值
   */
  it('Property 38.6: Dependency values should be correctly collected', async () => {
    await fc.assert(
      fc.asyncProperty(
        cascadeLevelCountArb.chain(count => 
          fc.record({
            names: uniqueParamNamesArb(count),
            values: fc.array(paramValueArb, { minLength: count, maxLength: count })
          })
        ),
        async (params) => {
          const { names, values } = params
          
          const { registerParameter, addLinkage, setParameterValue, getDependencyValues } = useParameterLinkage({
            autoClearChildren: false, // 禁用自动清除以便设置所有值
            debounceDelay: 0
          })

          // 注册所有参数
          names.forEach(name => registerParameter(name))

          // 创建链式级联关系
          for (let i = 0; i < names.length - 1; i++) {
            addLinkage({
              sourceParam: names[i]!,
              targetParam: names[i + 1]!,
              linkageType: 'cascade',
              linkageConfig: createCascadeConfig({
                sourceField: `${names[i]!}_code`,
                targetField: `${names[i + 1]!}_code`,
                dataSourceId: 1,
                sql: 'SELECT * FROM options'
              }),
              enabled: true
            })
          }

          // 设置所有参数的值
          for (let i = 0; i < names.length; i++) {
            await setParameterValue(names[i]!, values[i]!)
          }

          // 验证最后一个参数的依赖值
          const lastParamDeps = getDependencyValues(names[names.length - 1]!)
          
          // 应该包含所有祖先参数的值
          for (let i = 0; i < names.length - 1; i++) {
            if (lastParamDeps[names[i]!] !== values[i]) {
              return false
            }
          }

          // 不应该包含自己
          if (names[names.length - 1]! in lastParamDeps) {
            return false
          }

          return true
        }
      ),
      { numRuns: 30 }
    )
  })

  /**
   * **Validates: Requirements 13.1**
   * 
   * 属性 38.7: 禁用的联动不应影响参数关系
   * 对于任意禁用的联动配置，参数之间不应建立父子关系
   */
  it('Property 38.7: Disabled linkages should not affect parameter relationships', () => {
    fc.assert(
      fc.property(
        fc.record({
          parentName: paramNameArb,
          childName: paramNameArb,
          cascadeConfig: cascadeConfigArb
        }).filter(p => p.parentName !== p.childName),
        (params) => {
          const { parentName, childName, cascadeConfig } = params
          
          const { registerParameter, addLinkage, parameters, getCascadeLevel } = useParameterLinkage()

          // 注册参数
          registerParameter(parentName)
          registerParameter(childName)

          // 添加禁用的级联关系
          addLinkage({
            sourceParam: parentName,
            targetParam: childName,
            linkageType: 'cascade',
            linkageConfig: cascadeConfig,
            enabled: false // 禁用
          })

          // 父参数不应该有子参数
          if (parameters.value[parentName]!.children.length !== 0) {
            return false
          }

          // 子参数不应该有父参数
          if (parameters.value[childName]!.parents.length !== 0) {
            return false
          }

          // 两个参数的级联层级都应该是 0
          if (getCascadeLevel(parentName) !== 0 || getCascadeLevel(childName) !== 0) {
            return false
          }

          return true
        }
      ),
      { numRuns: 50 }
    )
  })


  /**
   * **Validates: Requirements 13.1**
   * 
   * 属性 38.8: 批量设置参数值应按级联层级顺序执行
   * 对于任意多级级联配置，批量设置值时应先设置父参数再设置子参数
   */
  it('Property 38.8: Batch set values should respect cascade level order', async () => {
    await fc.assert(
      fc.asyncProperty(
        cascadeLevelCountArb.chain(count => 
          fc.record({
            names: uniqueParamNamesArb(count),
            values: fc.array(paramValueArb, { minLength: count, maxLength: count })
          })
        ),
        async (params) => {
          const { names, values } = params
          
          const setOrder: string[] = []
          const onValueChange = vi.fn((name: string) => {
            setOrder.push(name)
          })
          
          const { registerParameter, addLinkage, setParameterValues, getAllValues } = useParameterLinkage({
            onValueChange,
            autoClearChildren: false,
            debounceDelay: 0
          })

          // 注册所有参数
          names.forEach(name => registerParameter(name))

          // 创建链式级联关系
          for (let i = 0; i < names.length - 1; i++) {
            addLinkage({
              sourceParam: names[i]!,
              targetParam: names[i + 1]!,
              linkageType: 'cascade',
              linkageConfig: createCascadeConfig({
                sourceField: `${names[i]!}_code`,
                targetField: `${names[i + 1]!}_code`,
                dataSourceId: 1,
                sql: 'SELECT * FROM options'
              }),
              enabled: true
            })
          }

          // 创建值映射
          const valueMap: Record<string, unknown> = {}
          for (let i = 0; i < names.length; i++) {
            valueMap[names[i]!] = values[i]
          }

          // 批量设置值
          await setParameterValues(valueMap)

          // 验证所有值都被正确设置
          const allValues = getAllValues()
          for (let i = 0; i < names.length; i++) {
            if (allValues[names[i]!] !== values[i]) {
              return false
            }
          }

          // 验证设置顺序：父参数应该在子参数之前
          for (let i = 0; i < names.length - 1; i++) {
            const parentIndex = setOrder.indexOf(names[i]!)
            const childIndex = setOrder.indexOf(names[i + 1]!)
            
            // 如果两个参数都被设置了，父参数应该先被设置
            if (parentIndex !== -1 && childIndex !== -1 && parentIndex > childIndex) {
              return false
            }
          }

          return true
        }
      ),
      { numRuns: 30 }
    )
  })

  /**
   * **Validates: Requirements 13.1**
   * 
   * 属性 38.9: 重置应恢复所有参数到初始状态
   * 对于任意参数配置，重置后所有参数应恢复到注册时的初始值
   */
  it('Property 38.9: Reset should restore all parameters to initial state', async () => {
    await fc.assert(
      fc.asyncProperty(
        fc.record({
          names: uniqueParamNamesArb(3),
          initialValues: fc.array(paramValueArb, { minLength: 3, maxLength: 3 }),
          newValues: fc.array(paramValueArb, { minLength: 3, maxLength: 3 }),
          initialOptions: fc.array(paramOptionsArb, { minLength: 3, maxLength: 3 })
        }),
        async (params) => {
          const { names, initialValues, newValues, initialOptions } = params
          
          const { registerParameter, addLinkage, setParameterValue, reset, getParameterValue, getParameterOptions } = useParameterLinkage({
            autoClearChildren: false,
            debounceDelay: 0
          })

          // 注册参数并设置初始值和选项
          for (let i = 0; i < names.length; i++) {
            registerParameter(names[i]!, initialValues[i]!, initialOptions[i]!)
          }

          // 创建级联关系
          for (let i = 0; i < names.length - 1; i++) {
            addLinkage({
              sourceParam: names[i]!,
              targetParam: names[i + 1]!,
              linkageType: 'cascade',
              linkageConfig: createCascadeConfig({
                sourceField: `${names[i]!}_code`,
                targetField: `${names[i + 1]!}_code`,
                dataSourceId: 1,
                sql: 'SELECT * FROM options'
              }),
              enabled: true
            })
          }

          // 修改所有参数的值
          for (let i = 0; i < names.length; i++) {
            await setParameterValue(names[i]!, newValues[i]!)
          }

          // 重置
          reset()

          // 验证所有参数恢复到初始状态
          for (let i = 0; i < names.length; i++) {
            const value = getParameterValue(names[i]!)
            const options = getParameterOptions(names[i]!)
            
            if (value !== initialValues[i]) {
              return false
            }
            
            if (JSON.stringify(options) !== JSON.stringify(initialOptions[i])) {
              return false
            }
          }

          return true
        }
      ),
      { numRuns: 30 }
    )
  })


  /**
   * **Validates: Requirements 13.1**
   * 
   * 属性 38.10: 移除联动后参数关系应正确更新
   * 对于任意联动配置，移除联动后相关参数的父子关系应被清除
   */
  it('Property 38.10: Removing linkage should update parameter relationships', () => {
    fc.assert(
      fc.property(
        fc.record({
          parentName: paramNameArb,
          childName: paramNameArb,
          cascadeConfig: cascadeConfigArb
        }).filter(p => p.parentName !== p.childName),
        (params) => {
          const { parentName, childName, cascadeConfig } = params
          
          const { registerParameter, addLinkage, removeLinkage, parameters, getCascadeLevel } = useParameterLinkage()

          // 注册参数
          registerParameter(parentName)
          registerParameter(childName)

          // 添加级联关系
          const linkageId = addLinkage({
            sourceParam: parentName,
            targetParam: childName,
            linkageType: 'cascade',
            linkageConfig: cascadeConfig,
            enabled: true
          })

          // 验证关系已建立
          if (!parameters.value[parentName]!.children.includes(childName)) {
            return false
          }
          if (!parameters.value[childName]!.parents.includes(parentName)) {
            return false
          }
          if (getCascadeLevel(childName) !== 1) {
            return false
          }

          // 移除联动
          removeLinkage(linkageId)

          // 验证关系已清除
          if (parameters.value[parentName]!.children.includes(childName)) {
            return false
          }
          if (parameters.value[childName]!.parents.includes(parentName)) {
            return false
          }
          if (getCascadeLevel(childName) !== 0) {
            return false
          }

          return true
        }
      ),
      { numRuns: 50 }
    )
  })
})

describe('Parameter Options Loading Property Tests', () => {
  beforeEach(() => {
    resetLinkageIdCounter()
  })

  /**
   * **Validates: Requirements 13.1**
   * 
   * 属性 38.11: 选项加载应在依赖满足时触发
   * 对于任意级联配置，只有当所有父参数都有值时才应加载子参数选项
   */
  it('Property 38.11: Options loading should trigger when dependencies are satisfied', async () => {
    await fc.assert(
      fc.asyncProperty(
        fc.record({
          parentName: paramNameArb,
          childName: paramNameArb,
          parentValue: paramValueArb,
          options: paramOptionsArb,
          cascadeConfig: cascadeConfigArb
        }).filter(p => p.parentName !== p.childName),
        async (params) => {
          const { parentName, childName, parentValue, options, cascadeConfig } = params
          
          const mockLoader = vi.fn().mockResolvedValue(options)
          
          const { registerParameter, addLinkage, setParameterValue, refreshOptions, getParameterOptions, validateDependencies } = useParameterLinkage({
            optionsLoader: mockLoader,
            debounceDelay: 0
          })

          // 注册参数
          registerParameter(parentName)
          registerParameter(childName)

          // 添加级联关系
          addLinkage({
            sourceParam: parentName,
            targetParam: childName,
            linkageType: 'cascade',
            linkageConfig: cascadeConfig,
            enabled: true
          })

          // 在父参数没有值时尝试刷新子参数选项
          await refreshOptions(childName)
          
          // 依赖不满足，不应该调用加载器
          const callsBeforeParentSet = mockLoader.mock.calls.length
          if (callsBeforeParentSet !== 0) {
            return false
          }

          // 设置父参数值
          await setParameterValue(parentName, parentValue)

          // 现在依赖应该满足
          if (!validateDependencies(childName)) {
            return false
          }

          // 刷新子参数选项
          await refreshOptions(childName)

          // 应该调用加载器
          if (mockLoader.mock.calls.length !== 1) {
            return false
          }

          // 选项应该被正确设置
          const loadedOptions = getParameterOptions(childName)
          if (JSON.stringify(loadedOptions) !== JSON.stringify(options)) {
            return false
          }

          return true
        }
      ),
      { numRuns: 30 }
    )
  })

  /**
   * **Validates: Requirements 13.1**
   * 
   * 属性 38.12: 选项加载错误应正确处理
   * 对于任意加载错误，错误信息应被正确记录，选项应被清空
   */
  it('Property 38.12: Options loading errors should be handled correctly', async () => {
    await fc.assert(
      fc.asyncProperty(
        fc.record({
          parentName: paramNameArb,
          childName: paramNameArb,
          parentValue: paramValueArb,
          errorMessage: fc.string({ minLength: 1, maxLength: 50 }).filter(s => s.trim().length > 0),
          cascadeConfig: cascadeConfigArb
        }).filter(p => p.parentName !== p.childName),
        async (params) => {
          const { parentName, childName, parentValue, errorMessage, cascadeConfig } = params
          
          const mockError = new Error(errorMessage)
          const mockLoader = vi.fn().mockRejectedValue(mockError)
          const onError = vi.fn()
          
          const { registerParameter, addLinkage, setParameterValue, refreshOptions, getError, getParameterOptions, isLoading } = useParameterLinkage({
            optionsLoader: mockLoader,
            onError,
            debounceDelay: 0
          })

          // 注册参数
          registerParameter(parentName)
          registerParameter(childName)

          // 添加级联关系
          addLinkage({
            sourceParam: parentName,
            targetParam: childName,
            linkageType: 'cascade',
            linkageConfig: cascadeConfig,
            enabled: true
          })

          // 设置父参数值
          await setParameterValue(parentName, parentValue)

          // 刷新子参数选项（会失败）
          await refreshOptions(childName)

          // 错误应该被记录
          const error = getError(childName)
          if (error !== errorMessage) {
            return false
          }

          // 选项应该被清空
          const loadedOptions = getParameterOptions(childName)
          if (loadedOptions.length !== 0) {
            return false
          }

          // 加载状态应该为 false
          if (isLoading(childName)) {
            return false
          }

          // onError 回调应该被调用
          if (onError.mock.calls.length !== 1) {
            return false
          }
          if (onError.mock.calls[0][0] !== childName) {
            return false
          }

          return true
        }
      ),
      { numRuns: 30 }
    )
  })
})

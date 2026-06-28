/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 参数校验属性测试
 * 
 * **属性 41: 参数双重校验一致性**
 * **验证需求: 13.4**
 * 
 * 对于任意无效的参数值，前端和后端校验应返回一致的错误结果。
 * 
 * 测试内容:
 * 1. 必填校验一致性
 * 2. 类型校验一致性
 * 3. 范围校验一致性
 * 4. 长度校验一致性
 * 5. 正则校验一致性
 * 6. 枚举校验一致性
 * 7. 内置校验器一致性（phone, email, idCard, url等）
 */

import { describe, it, expect, beforeEach } from 'vitest'
import * as fc from 'fast-check'
import {
  validateParameter,
  validateParameters,
  required,
  dataType,
  range,
  length,
  pattern,
  enumValues,
  custom,
  type ValidationRule,
  type ParameterValidationConfig,
  type ValidationResult
} from '../parameterValidationService'

// ============================================================================
// 模拟后端校验逻辑（与 ParameterValidationService.java 保持一致）
// ============================================================================

/**
 * 后端校验规则接口（模拟Java端）
 */
interface BackendValidationRule {
  type: string
  message?: string
  enabled?: boolean
  config: Record<string, any>
}

/**
 * 后端校验配置接口
 */
interface BackendValidationConfig {
  paramName: string
  displayName?: string
  rules: BackendValidationRule[]
}

/**
 * 后端校验错误接口
 */
interface BackendValidationError {
  paramName: string
  ruleType: string
  message: string
  actualValue?: any
}

/**
 * 后端校验结果接口
 */
interface BackendValidationResult {
  valid: boolean
  errors: BackendValidationError[]
}

// ============================================================================
// 后端校验逻辑模拟（与Java实现保持一致）
// ============================================================================

/**
 * 检查值是否为空（与后端一致）
 */
function backendIsEmpty(value: any): boolean {
  if (value === null || value === undefined) return true
  if (typeof value === 'string' && value.trim() === '') return true
  if (Array.isArray(value) && value.length === 0) return true
  return false
}

/**
 * 检查是否为数字字符串
 */
function isNumericString(value: any): boolean {
  try {
    const num = parseFloat(String(value))
    return !isNaN(num)
  } catch {
    return false
  }
}

/**
 * 检查是否为整数
 */
function isInteger(value: any): boolean {
  if (typeof value === 'number' && Number.isInteger(value)) return true
  try {
    const str = String(value)
    const num = parseInt(str, 10)
    return !isNaN(num) && !str.includes('.')
  } catch {
    return false
  }
}

/**
 * 检查是否为布尔字符串
 */
function isBooleanString(value: any): boolean {
  const str = String(value).toLowerCase()
  return str === 'true' || str === 'false'
}

/**
 * 检查是否为有效日期
 */
function isValidDate(value: any): boolean {
  if (value instanceof Date) return !isNaN(value.getTime())
  try {
    const date = new Date(value)
    return !isNaN(date.getTime())
  } catch {
    return false
  }
}

/**
 * 后端必填校验（模拟Java实现）
 */
function backendValidateRequired(
  value: any,
  rule: BackendValidationRule,
  displayName: string,
  paramName: string
): BackendValidationError | null {
  const required = rule.config.required
  if (!required) return null
  
  if (backendIsEmpty(value)) {
    const message = rule.message || `${displayName}不能为空`
    return { paramName, ruleType: 'required', message, actualValue: value }
  }
  return null
}

/**
 * 后端类型校验（模拟Java实现）
 */
function backendValidateType(
  value: any,
  rule: BackendValidationRule,
  displayName: string,
  paramName: string
): BackendValidationError | null {
  if (backendIsEmpty(value)) return null
  
  const dataType = rule.config.dataType
  let valid = false
  let expectedTypeLabel = dataType
  
  switch (dataType) {
    case 'string':
      valid = typeof value === 'string'
      expectedTypeLabel = '字符串'
      break
    case 'number':
      valid = typeof value === 'number' || isNumericString(value)
      expectedTypeLabel = '数字'
      break
    case 'integer':
      valid = isInteger(value)
      expectedTypeLabel = '整数'
      break
    case 'boolean':
      valid = typeof value === 'boolean' || isBooleanString(value)
      expectedTypeLabel = '布尔值'
      break
    case 'date':
    case 'datetime':
      valid = isValidDate(value)
      expectedTypeLabel = dataType === 'date' ? '日期' : '日期时间'
      break
    case 'array':
      valid = Array.isArray(value)
      expectedTypeLabel = '数组'
      break
    default:
      valid = true
  }
  
  if (!valid) {
    const message = rule.message || `${displayName}必须是${expectedTypeLabel}类型`
    return { paramName, ruleType: 'type', message, actualValue: value }
  }
  return null
}

/**
 * 后端范围校验（模拟Java实现）
 */
function backendValidateRange(
  value: any,
  rule: BackendValidationRule,
  displayName: string,
  paramName: string
): BackendValidationError | null {
  if (backendIsEmpty(value)) return null
  
  const num = parseFloat(String(value))
  if (isNaN(num)) {
    const message = rule.message || `${displayName}必须是有效的数字`
    return { paramName, ruleType: 'range', message, actualValue: value }
  }
  
  const min = rule.config.min
  const max = rule.config.max
  const minInclusive = rule.config.minInclusive !== false
  const maxInclusive = rule.config.maxInclusive !== false
  
  if (min !== undefined) {
    const minValid = minInclusive ? num >= min : num > min
    if (!minValid) {
      const boundary = minInclusive ? '大于等于' : '大于'
      const message = rule.message || `${displayName}必须${boundary}${min}`
      return { paramName, ruleType: 'range', message, actualValue: value }
    }
  }
  
  if (max !== undefined) {
    const maxValid = maxInclusive ? num <= max : num < max
    if (!maxValid) {
      const boundary = maxInclusive ? '小于等于' : '小于'
      const message = rule.message || `${displayName}必须${boundary}${max}`
      return { paramName, ruleType: 'range', message, actualValue: value }
    }
  }
  
  return null
}

/**
 * 后端长度校验（模拟Java实现）
 */
function backendValidateLength(
  value: any,
  rule: BackendValidationRule,
  displayName: string,
  paramName: string
): BackendValidationError | null {
  if (backendIsEmpty(value)) return null
  
  let len: number
  if (typeof value === 'string') {
    len = value.length
  } else if (Array.isArray(value)) {
    len = value.length
  } else {
    return null
  }
  
  const minLength = rule.config.minLength
  const maxLength = rule.config.maxLength
  
  if (minLength !== undefined && len < minLength) {
    const message = rule.message || `${displayName}长度不能少于${minLength}`
    return { paramName, ruleType: 'length', message, actualValue: value }
  }
  
  if (maxLength !== undefined && len > maxLength) {
    const message = rule.message || `${displayName}长度不能超过${maxLength}`
    return { paramName, ruleType: 'length', message, actualValue: value }
  }
  
  return null
}

/**
 * 后端正则校验（模拟Java实现）
 */
function backendValidatePattern(
  value: any,
  rule: BackendValidationRule,
  displayName: string,
  paramName: string
): BackendValidationError | null {
  if (backendIsEmpty(value)) return null
  
  const patternStr = rule.config.pattern
  const flags = rule.config.flags
  
  try {
    let patternFlags = ''
    if (flags) {
      if (flags.includes('i')) patternFlags += 'i'
      if (flags.includes('m')) patternFlags += 'm'
    }
    
    const regex = new RegExp(patternStr, patternFlags)
    if (!regex.test(String(value))) {
      const message = rule.message || `${displayName}格式不正确`
      return { paramName, ruleType: 'pattern', message, actualValue: value }
    }
  } catch {
    return { paramName, ruleType: 'pattern', message: '校验规则配置错误', actualValue: value }
  }
  
  return null
}

/**
 * 后端枚举校验（模拟Java实现）
 */
function backendValidateEnum(
  value: any,
  rule: BackendValidationRule,
  displayName: string,
  paramName: string
): BackendValidationError | null {
  if (backendIsEmpty(value)) return null
  
  const allowedValues = rule.config.values || []
  if (allowedValues.length === 0) return null
  
  const valueStr = String(value)
  const found = allowedValues.some((v: any) => String(v) === valueStr)
  
  if (!found) {
    const message = rule.message || `${displayName}的值不在允许的范围内`
    return { paramName, ruleType: 'enum', message, actualValue: value }
  }
  
  return null
}

// 内置校验器（与后端保持一致）
const PHONE_PATTERN = /^1[3-9]\d{9}$/
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const ID_CARD_PATTERN = /^(\d{15}|\d{17}[\dXx])$/

/**
 * 后端自定义校验（模拟Java实现）
 */
function backendValidateCustom(
  value: any,
  rule: BackendValidationRule,
  displayName: string,
  paramName: string
): BackendValidationError | null {
  if (backendIsEmpty(value)) return null
  
  const validatorName = rule.config.validatorName
  const params = rule.config.params || {}
  
  switch (validatorName) {
    case 'phone':
      if (!PHONE_PATTERN.test(String(value))) {
        return { paramName, ruleType: 'custom', message: rule.message || '请输入有效的手机号', actualValue: value }
      }
      break
    case 'email':
      if (!EMAIL_PATTERN.test(String(value))) {
        return { paramName, ruleType: 'custom', message: rule.message || '请输入有效的邮箱地址', actualValue: value }
      }
      break
    case 'idCard':
      if (!ID_CARD_PATTERN.test(String(value))) {
        return { paramName, ruleType: 'custom', message: rule.message || '请输入有效的身份证号', actualValue: value }
      }
      break
    case 'url':
      try {
        new URL(String(value))
      } catch {
        return { paramName, ruleType: 'custom', message: rule.message || '请输入有效的URL', actualValue: value }
      }
      break
    case 'positiveInteger': {
      const num = parseInt(String(value), 10)
      if (isNaN(num) || num <= 0) {
        return { paramName, ruleType: 'custom', message: rule.message || '请输入正整数', actualValue: value }
      }
      break
    }
    case 'nonNegativeInteger': {
      const num = parseInt(String(value), 10)
      if (isNaN(num) || num < 0) {
        return { paramName, ruleType: 'custom', message: rule.message || '请输入非负整数', actualValue: value }
      }
      break
    }
    case 'decimalPlaces': {
      const maxPlaces = params.maxPlaces ?? 2
      const str = String(value)
      const dotIndex = str.indexOf('.')
      if (dotIndex >= 0) {
        const decimalLength = str.length - dotIndex - 1
        if (decimalLength > maxPlaces) {
          return { paramName, ruleType: 'custom', message: rule.message || `小数位数不能超过 ${maxPlaces} 位`, actualValue: value }
        }
      }
      break
    }
  }
  
  return null
}

/**
 * 后端校验单个规则
 */
function backendValidateRule(
  value: any,
  rule: BackendValidationRule,
  displayName: string,
  paramName: string
): BackendValidationError | null {
  switch (rule.type) {
    case 'required':
      return backendValidateRequired(value, rule, displayName, paramName)
    case 'type':
      return backendValidateType(value, rule, displayName, paramName)
    case 'range':
      return backendValidateRange(value, rule, displayName, paramName)
    case 'length':
      return backendValidateLength(value, rule, displayName, paramName)
    case 'pattern':
      return backendValidatePattern(value, rule, displayName, paramName)
    case 'enum':
      return backendValidateEnum(value, rule, displayName, paramName)
    case 'custom':
      return backendValidateCustom(value, rule, displayName, paramName)
    default:
      return null
  }
}

/**
 * 后端校验单个参数（模拟Java实现）
 */
function backendValidateParameter(value: any, config: BackendValidationConfig): BackendValidationResult {
  const errors: BackendValidationError[] = []
  const displayName = config.displayName || config.paramName
  
  for (const rule of config.rules) {
    if (rule.enabled === false) continue
    
    const error = backendValidateRule(value, rule, displayName, config.paramName)
    
    if (error) {
      errors.push(error)
      if (rule.type === 'required') break
    }
  }
  
  return { valid: errors.length === 0, errors }
}

// ============================================================================
// 转换函数：前端配置 -> 后端配置
// ============================================================================

/**
 * 将前端校验规则转换为后端格式
 */
function convertToBackendRule(rule: ValidationRule): BackendValidationRule {
  const backendRule: BackendValidationRule = {
    type: rule.type,
    message: rule.message,
    enabled: rule.enabled,
    config: {}
  }
  
  switch (rule.type) {
    case 'required':
      backendRule.config.required = (rule as any).required
      break
    case 'type':
      backendRule.config.dataType = (rule as any).dataType
      break
    case 'range':
      backendRule.config.min = (rule as any).min
      backendRule.config.max = (rule as any).max
      backendRule.config.minInclusive = (rule as any).minInclusive
      backendRule.config.maxInclusive = (rule as any).maxInclusive
      break
    case 'length':
      backendRule.config.minLength = (rule as any).minLength
      backendRule.config.maxLength = (rule as any).maxLength
      break
    case 'pattern':
      backendRule.config.pattern = (rule as any).pattern
      backendRule.config.flags = (rule as any).flags
      break
    case 'enum':
      backendRule.config.values = (rule as any).values
      break
    case 'custom':
      backendRule.config.validatorName = (rule as any).validatorName
      backendRule.config.params = (rule as any).params
      break
  }
  
  return backendRule
}

/**
 * 将前端配置转换为后端格式
 */
function convertToBackendConfig(config: ParameterValidationConfig): BackendValidationConfig {
  return {
    paramName: config.paramName,
    displayName: config.displayName,
    rules: config.rules.map(convertToBackendRule)
  }
}

// ============================================================================
// Arbitraries (测试数据生成器)
// ============================================================================

/** 生成有效的参数名 */
const paramNameArb = fc.stringMatching(/^[a-z][a-z0-9_]{2,15}$/)

/** 生成显示名称 */
const displayNameArb = fc.string({ minLength: 2, maxLength: 10 }).filter(s => s.trim().length > 0)

/** 生成字符串值 */
const stringValueArb = fc.string({ minLength: 0, maxLength: 50 })

/** 生成非空字符串值 */
const nonEmptyStringArb = fc.string({ minLength: 1, maxLength: 30 }).filter(s => s.trim().length > 0)

/** 生成数字值 */
const numberValueArb = fc.double({ min: -10000, max: 10000, noNaN: true })

/** 生成整数值 */
const integerValueArb = fc.integer({ min: -10000, max: 10000 })

/** 生成布尔值 */
const booleanValueArb = fc.boolean()

/** 生成数组值 */
const arrayValueArb = fc.array(fc.string({ minLength: 1, maxLength: 10 }), { minLength: 0, maxLength: 5 })

/** 生成空值 */
const emptyValueArb = fc.constantFrom(null, undefined, '', '   ', [])

/** 生成混合值 */
const mixedValueArb = fc.oneof(
  stringValueArb,
  numberValueArb,
  booleanValueArb,
  arrayValueArb,
  fc.constant(null),
  fc.constant(undefined)
)

/** 生成有效手机号 */
const validPhoneArb = fc.stringMatching(/^1[3-9]\d{9}$/)

/** 生成无效手机号 */
const invalidPhoneArb = fc.oneof(
  fc.constant('12345678901'),
  fc.constant('1234567890'),
  fc.constant('23456789012'),
  fc.constant('abc'),
  fc.constant('')
)

/** 生成有效邮箱 */
const validEmailArb = fc.emailAddress()

/** 生成无效邮箱 */
const invalidEmailArb = fc.oneof(
  fc.constant('invalid'),
  fc.constant('no@'),
  fc.constant('@domain.com'),
  fc.constant('test@'),
  fc.constant('')
)

/** 生成有效身份证号 */
const validIdCardArb = fc.oneof(
  fc.stringMatching(/^\d{15}$/),
  fc.stringMatching(/^\d{17}[\dXx]$/)
)

/** 生成无效身份证号 */
const invalidIdCardArb = fc.oneof(
  fc.constant('12345'),
  fc.constant('1234567890123456'),
  fc.constant('abc'),
  fc.constant('')
)

/** 生成有效URL */
const validUrlArb = fc.webUrl()

/** 生成无效URL */
const invalidUrlArb = fc.oneof(
  fc.constant('not-a-url'),
  fc.constant('ftp://'),
  fc.constant('://missing'),
  fc.constant('')
)

/** 生成正整数 */
const positiveIntegerArb = fc.integer({ min: 1, max: 10000 })

/** 生成非正整数 */
const nonPositiveIntegerArb = fc.oneof(
  fc.constant(0),
  fc.integer({ min: -10000, max: -1 }),
  fc.constant(-0.5),
  fc.constant('abc')
)

/** 生成非负整数 */
const nonNegativeIntegerArb = fc.integer({ min: 0, max: 10000 })

/** 生成负整数 */
const negativeIntegerArb = fc.integer({ min: -10000, max: -1 })

// ============================================================================
// 属性测试
// ============================================================================

describe('Parameter Validation Consistency Property Tests', () => {
  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.1: 必填校验一致性
   * 对于任意空值，前端和后端必填校验应返回一致的结果
   */
  it('Property 41.1: Required validation should be consistent between frontend and backend', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        displayNameArb,
        emptyValueArb,
        (paramName, displayName, value) => {
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            displayName,
            rules: [required()]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          const frontendResult = validateParameter(value, frontendConfig)
          const backendResult = backendValidateParameter(value, backendConfig)
          
          // 校验结果应一致
          return frontendResult.valid === backendResult.valid
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.2: 类型校验一致性 - 字符串类型
   * 对于任意值，字符串类型校验应返回一致的结果
   */
  it('Property 41.2: String type validation should be consistent', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        mixedValueArb,
        (paramName, value) => {
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            rules: [dataType('string')]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          const frontendResult = validateParameter(value, frontendConfig)
          const backendResult = backendValidateParameter(value, backendConfig)
          
          return frontendResult.valid === backendResult.valid
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.3: 类型校验一致性 - 数字类型
   * 注意：前端和后端对数字类型的判断逻辑：
   * - 前端：typeof value === 'number' (严格类型检查)
   * - 后端：value instanceof Number || isNumericString(value) (宽松检查)
   * 此测试验证对于实际数字类型值，两端行为一致
   */
  it('Property 41.3: Number type validation should be consistent', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        fc.oneof(
          numberValueArb,
          // 非数字值
          fc.constantFrom('abc', 'not-a-number', true, false),
          fc.constant(null)
        ),
        (paramName, value) => {
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            rules: [dataType('number')]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          const frontendResult = validateParameter(value, frontendConfig)
          const backendResult = backendValidateParameter(value, backendConfig)
          
          return frontendResult.valid === backendResult.valid
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.4: 类型校验一致性 - 整数类型
   * 注意：前端和后端对整数类型的判断逻辑：
   * - 前端：typeof value === 'number' && Number.isInteger(value) (严格类型检查)
   * - 后端：isInteger(value) 包括整数字符串 (宽松检查)
   * 此测试验证对于实际整数类型值，两端行为一致
   */
  it('Property 41.4: Integer type validation should be consistent', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        fc.oneof(
          integerValueArb,
          // 浮点数（应该失败）
          fc.double({ min: 0.1, max: 100, noNaN: true }).filter(n => !Number.isInteger(n)),
          // 非数字值
          fc.constantFrom('abc', 'not-a-number', true, false),
          fc.constant(null)
        ),
        (paramName, value) => {
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            rules: [dataType('integer')]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          const frontendResult = validateParameter(value, frontendConfig)
          const backendResult = backendValidateParameter(value, backendConfig)
          
          return frontendResult.valid === backendResult.valid
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.5: 范围校验一致性
   * 对于任意数值和范围配置，前端和后端校验应返回一致的结果
   */
  it('Property 41.5: Range validation should be consistent', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        numberValueArb,
        fc.integer({ min: -100, max: 0 }),
        fc.integer({ min: 1, max: 100 }),
        (paramName, value, min, max) => {
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            rules: [range(min, max)]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          const frontendResult = validateParameter(value, frontendConfig)
          const backendResult = backendValidateParameter(value, backendConfig)
          
          return frontendResult.valid === backendResult.valid
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.6: 范围校验边界一致性
   * 对于边界值，前端和后端校验应返回一致的结果
   */
  it('Property 41.6: Range boundary validation should be consistent', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        fc.integer({ min: 0, max: 100 }),
        fc.boolean(),
        fc.boolean(),
        (paramName, boundary, minInclusive, maxInclusive) => {
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            rules: [{
              type: 'range',
              min: boundary,
              max: boundary + 10,
              minInclusive,
              maxInclusive
            } as any]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          // 测试边界值
          const frontendResult = validateParameter(boundary, frontendConfig)
          const backendResult = backendValidateParameter(boundary, backendConfig)
          
          return frontendResult.valid === backendResult.valid
        }
      ),
      { numRuns: 50 }
    )
  })

  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.7: 长度校验一致性
   * 对于任意字符串和长度配置，前端和后端校验应返回一致的结果
   */
  it('Property 41.7: Length validation should be consistent', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        stringValueArb,
        fc.integer({ min: 0, max: 10 }),
        fc.integer({ min: 11, max: 50 }),
        (paramName, value, minLength, maxLength) => {
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            rules: [length(minLength, maxLength)]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          const frontendResult = validateParameter(value, frontendConfig)
          const backendResult = backendValidateParameter(value, backendConfig)
          
          return frontendResult.valid === backendResult.valid
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.8: 正则校验一致性
   * 对于任意值和正则模式，前端和后端校验应返回一致的结果
   */
  it('Property 41.8: Pattern validation should be consistent', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        nonEmptyStringArb,
        fc.constantFrom('^[a-z]+$', '^\\d+$', '^[A-Z][a-z]+$', '^\\w+@\\w+\\.\\w+$'),
        (paramName, value, patternStr) => {
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            rules: [pattern(patternStr)]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          const frontendResult = validateParameter(value, frontendConfig)
          const backendResult = backendValidateParameter(value, backendConfig)
          
          return frontendResult.valid === backendResult.valid
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.9: 枚举校验一致性
   * 对于任意值和枚举列表，前端和后端校验应返回一致的结果
   */
  it('Property 41.9: Enum validation should be consistent', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        fc.oneof(
          fc.constantFrom('a', 'b', 'c', 'd', 'e'),
          fc.constantFrom(1, 2, 3, 4, 5),
          nonEmptyStringArb
        ),
        (paramName, value) => {
          const allowedValues = ['a', 'b', 'c', 1, 2, 3]
          
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            rules: [enumValues(allowedValues)]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          const frontendResult = validateParameter(value, frontendConfig)
          const backendResult = backendValidateParameter(value, backendConfig)
          
          return frontendResult.valid === backendResult.valid
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.10: 手机号校验一致性
   * 对于任意手机号格式，前端和后端校验应返回一致的结果
   */
  it('Property 41.10: Phone validation should be consistent', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        fc.oneof(validPhoneArb, invalidPhoneArb),
        (paramName, value) => {
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            rules: [custom('phone')]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          const frontendResult = validateParameter(value, frontendConfig)
          const backendResult = backendValidateParameter(value, backendConfig)
          
          return frontendResult.valid === backendResult.valid
        }
      ),
      { numRuns: 50 }
    )
  })

  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.11: 邮箱校验一致性
   * 对于任意邮箱格式，前端和后端校验应返回一致的结果
   */
  it('Property 41.11: Email validation should be consistent', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        fc.oneof(validEmailArb, invalidEmailArb),
        (paramName, value) => {
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            rules: [custom('email')]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          const frontendResult = validateParameter(value, frontendConfig)
          const backendResult = backendValidateParameter(value, backendConfig)
          
          return frontendResult.valid === backendResult.valid
        }
      ),
      { numRuns: 50 }
    )
  })

  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.12: 身份证号校验一致性
   * 对于任意身份证号格式，前端和后端校验应返回一致的结果
   */
  it('Property 41.12: ID card validation should be consistent', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        fc.oneof(validIdCardArb, invalidIdCardArb),
        (paramName, value) => {
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            rules: [custom('idCard')]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          const frontendResult = validateParameter(value, frontendConfig)
          const backendResult = backendValidateParameter(value, backendConfig)
          
          return frontendResult.valid === backendResult.valid
        }
      ),
      { numRuns: 50 }
    )
  })

  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.13: URL校验一致性
   * 对于任意URL格式，前端和后端校验应返回一致的结果
   */
  it('Property 41.13: URL validation should be consistent', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        fc.oneof(validUrlArb, invalidUrlArb),
        (paramName, value) => {
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            rules: [custom('url')]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          const frontendResult = validateParameter(value, frontendConfig)
          const backendResult = backendValidateParameter(value, backendConfig)
          
          return frontendResult.valid === backendResult.valid
        }
      ),
      { numRuns: 50 }
    )
  })

  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.14: 正整数校验一致性
   * 对于任意整数值，正整数校验应返回一致的结果
   */
  it('Property 41.14: Positive integer validation should be consistent', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        fc.oneof(positiveIntegerArb, nonPositiveIntegerArb),
        (paramName, value) => {
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            rules: [custom('positiveInteger')]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          const frontendResult = validateParameter(value, frontendConfig)
          const backendResult = backendValidateParameter(value, backendConfig)
          
          return frontendResult.valid === backendResult.valid
        }
      ),
      { numRuns: 50 }
    )
  })

  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.15: 非负整数校验一致性
   * 对于任意整数值，非负整数校验应返回一致的结果
   */
  it('Property 41.15: Non-negative integer validation should be consistent', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        fc.oneof(nonNegativeIntegerArb, negativeIntegerArb),
        (paramName, value) => {
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            rules: [custom('nonNegativeInteger')]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          const frontendResult = validateParameter(value, frontendConfig)
          const backendResult = backendValidateParameter(value, backendConfig)
          
          return frontendResult.valid === backendResult.valid
        }
      ),
      { numRuns: 50 }
    )
  })

  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.16: 小数位数校验一致性
   * 对于任意小数值，小数位数校验应返回一致的结果
   */
  it('Property 41.16: Decimal places validation should be consistent', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        fc.oneof(
          fc.constant('1.23'),
          fc.constant('1.234'),
          fc.constant('1.2345'),
          fc.constant('100'),
          fc.constant('0.1')
        ),
        fc.integer({ min: 1, max: 4 }),
        (paramName, value, maxPlaces) => {
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            rules: [custom('decimalPlaces', { maxPlaces })]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          const frontendResult = validateParameter(value, frontendConfig)
          const backendResult = backendValidateParameter(value, backendConfig)
          
          return frontendResult.valid === backendResult.valid
        }
      ),
      { numRuns: 50 }
    )
  })

  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.17: 多规则组合校验一致性
   * 对于任意多规则组合，前端和后端校验应返回一致的结果
   */
  it('Property 41.17: Multiple rules validation should be consistent', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        nonEmptyStringArb,
        (paramName, value) => {
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            rules: [
              required(),
              dataType('string'),
              length(1, 50)
            ]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          const frontendResult = validateParameter(value, frontendConfig)
          const backendResult = backendValidateParameter(value, backendConfig)
          
          return frontendResult.valid === backendResult.valid
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.18: 空值跳过非必填校验一致性
   * 对于空值，非必填字段的其他校验应被跳过
   */
  it('Property 41.18: Empty values should skip non-required validations consistently', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        emptyValueArb,
        (paramName, value) => {
          // 不包含required规则
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            rules: [
              dataType('string'),
              length(5, 50),
              pattern('^[a-z]+$')
            ]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          const frontendResult = validateParameter(value, frontendConfig)
          const backendResult = backendValidateParameter(value, backendConfig)
          
          // 空值应该通过校验（因为没有required规则）
          return frontendResult.valid === backendResult.valid && frontendResult.valid === true
        }
      ),
      { numRuns: 50 }
    )
  })

  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.19: 禁用规则应被跳过
   * 对于禁用的规则，前端和后端都应跳过
   */
  it('Property 41.19: Disabled rules should be skipped consistently', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        emptyValueArb,
        (paramName, value) => {
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            rules: [
              { type: 'required', required: true, enabled: false } as any
            ]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          const frontendResult = validateParameter(value, frontendConfig)
          const backendResult = backendValidateParameter(value, backendConfig)
          
          // 禁用的规则应该被跳过，空值应该通过
          return frontendResult.valid === backendResult.valid && frontendResult.valid === true
        }
      ),
      { numRuns: 50 }
    )
  })

  /**
   * **Validates: Requirements 13.4**
   * 
   * 属性 41.20: 错误消息格式一致性
   * 对于相同的校验失败，错误消息应一致
   */
  it('Property 41.20: Error messages should be consistent', () => {
    fc.assert(
      fc.property(
        paramNameArb,
        displayNameArb,
        emptyValueArb,
        (paramName, displayName, value) => {
          const frontendConfig: ParameterValidationConfig = {
            paramName,
            displayName,
            rules: [required()]
          }
          
          const backendConfig = convertToBackendConfig(frontendConfig)
          
          const frontendResult = validateParameter(value, frontendConfig)
          const backendResult = backendValidateParameter(value, backendConfig)
          
          // 如果都失败，错误消息应该一致
          if (!frontendResult.valid && !backendResult.valid) {
            return frontendResult.errors[0].message === backendResult.errors[0].message
          }
          return true
        }
      ),
      { numRuns: 50 }
    )
  })
})

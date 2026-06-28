/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 参数校验服务
 * 实现前端参数校验，与后端保持一致的校验规则
 * 
 * 功能：
 * - 支持多种校验规则：required, type, range, pattern, custom
 * - 返回清晰的错误信息
 * - 与后端校验规则保持一致
 * 
 * @validates 需求 13.4 - 实现前后端双重参数校验
 */

import { logger } from '@/utils/logger'

// ============================================================================
// 类型定义
// ============================================================================

/**
 * 校验规则类型
 */
export type ValidationRuleType = 'required' | 'type' | 'range' | 'pattern' | 'custom' | 'length' | 'enum'

/**
 * 参数数据类型
 */
export type ParameterDataType = 'string' | 'number' | 'integer' | 'boolean' | 'date' | 'datetime' | 'array'

/**
 * 基础校验规则
 */
export interface BaseValidationRule {
  /** 规则类型 */
  type: ValidationRuleType
  /** 错误消息（可选，使用默认消息） */
  message?: string
  /** 是否启用 */
  enabled?: boolean
}

/**
 * 必填校验规则
 */
export interface RequiredRule extends BaseValidationRule {
  type: 'required'
  /** 是否必填 */
  required: boolean
}

/**
 * 类型校验规则
 */
export interface TypeRule extends BaseValidationRule {
  type: 'type'
  /** 期望的数据类型 */
  dataType: ParameterDataType
}

/**
 * 范围校验规则（数值）
 */
export interface RangeRule extends BaseValidationRule {
  type: 'range'
  /** 最小值（包含） */
  min?: number
  /** 最大值（包含） */
  max?: number
  /** 是否包含最小值边界 */
  minInclusive?: boolean
  /** 是否包含最大值边界 */
  maxInclusive?: boolean
}

/**
 * 长度校验规则（字符串/数组）
 */
export interface LengthRule extends BaseValidationRule {
  type: 'length'
  /** 最小长度 */
  minLength?: number
  /** 最大长度 */
  maxLength?: number
}

/**
 * 正则表达式校验规则
 */
export interface PatternRule extends BaseValidationRule {
  type: 'pattern'
  /** 正则表达式模式 */
  pattern: string
  /** 正则表达式标志 */
  flags?: string
}

/**
 * 枚举校验规则
 */
export interface EnumRule extends BaseValidationRule {
  type: 'enum'
  /** 允许的值列表 */
  values: (string | number | boolean)[]
}

/**
 * 自定义校验规则
 */
export interface CustomRule extends BaseValidationRule {
  type: 'custom'
  /** 自定义校验函数名称（用于后端匹配） */
  validatorName: string
  /** 校验函数（前端使用） */
  validator?: (value: any, params?: Record<string, any>) => boolean | string
  /** 校验参数 */
  params?: Record<string, any>
}

/**
 * 校验规则联合类型
 */
export type ValidationRule = RequiredRule | TypeRule | RangeRule | LengthRule | PatternRule | EnumRule | CustomRule

/**
 * 参数校验配置
 */
export interface ParameterValidationConfig {
  /** 参数名称 */
  paramName: string
  /** 参数显示名称 */
  displayName?: string
  /** 校验规则列表 */
  rules: ValidationRule[]
}

/**
 * 校验错误
 */
export interface ValidationError {
  /** 参数名称 */
  paramName: string
  /** 规则类型 */
  ruleType: ValidationRuleType
  /** 错误消息 */
  message: string
  /** 实际值 */
  actualValue?: any
}

/**
 * 校验结果
 */
export interface ValidationResult {
  /** 是否校验通过 */
  valid: boolean
  /** 错误列表 */
  errors: ValidationError[]
}

/**
 * 批量校验结果
 */
export interface BatchValidationResult {
  /** 是否全部校验通过 */
  valid: boolean
  /** 按参数名分组的错误 */
  errorsByParam: Record<string, ValidationError[]>
  /** 所有错误列表 */
  allErrors: ValidationError[]
  /** 第一个错误消息（用于快速显示） */
  firstError?: string
}

// ============================================================================
// 内置校验器
// ============================================================================

/**
 * 内置自定义校验器
 */
const builtInValidators: Record<string, (value: any, params?: Record<string, any>) => boolean | string> = {
  /**
   * 手机号校验
   */
  phone: (value: any) => {
    if (value === null || value === undefined || value === '') return true
    const pattern = /^1[3-9]\d{9}$/
    return pattern.test(String(value)) || '请输入有效的手机号'
  },

  /**
   * 邮箱校验
   */
  email: (value: any) => {
    if (value === null || value === undefined || value === '') return true
    const pattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return pattern.test(String(value)) || '请输入有效的邮箱地址'
  },

  /**
   * 身份证号校验
   */
  idCard: (value: any) => {
    if (value === null || value === undefined || value === '') return true
    const pattern = /^(\d{15}|\d{17}[\dXx])$/
    return pattern.test(String(value)) || '请输入有效的身份证号'
  },

  /**
   * URL校验
   */
  url: (value: any) => {
    if (value === null || value === undefined || value === '') return true
    try {
      new URL(String(value))
      return true
    } catch {
      return '请输入有效的URL'
    }
  },

  /**
   * 日期范围校验
   */
  dateRange: (value: any, params?: Record<string, any>) => {
    if (value === null || value === undefined || value === '') return true
    const date = new Date(value)
    if (isNaN(date.getTime())) return '请输入有效的日期'
    
    if (params?.minDate) {
      const minDate = new Date(params.minDate)
      if (date < minDate) return `日期不能早于 ${params.minDate}`
    }
    if (params?.maxDate) {
      const maxDate = new Date(params.maxDate)
      if (date > maxDate) return `日期不能晚于 ${params.maxDate}`
    }
    return true
  },

  /**
   * 正整数校验
   */
  positiveInteger: (value: any) => {
    if (value === null || value === undefined || value === '') return true
    const num = Number(value)
    return (Number.isInteger(num) && num > 0) || '请输入正整数'
  },

  /**
   * 非负整数校验
   */
  nonNegativeInteger: (value: any) => {
    if (value === null || value === undefined || value === '') return true
    const num = Number(value)
    return (Number.isInteger(num) && num >= 0) || '请输入非负整数'
  },

  /**
   * 小数位数校验
   */
  decimalPlaces: (value: any, params?: Record<string, any>) => {
    if (value === null || value === undefined || value === '') return true
    const maxPlaces = params?.maxPlaces ?? 2
    const str = String(value)
    const decimalPart = str.split('.')[1]
    if (decimalPart && decimalPart.length > maxPlaces) {
      return `小数位数不能超过 ${maxPlaces} 位`
    }
    return true
  }
}

// ============================================================================
// 校验函数
// ============================================================================

/**
 * 检查值是否为空
 */
function isEmpty(value: any): boolean {
  if (value === null || value === undefined) return true
  if (typeof value === 'string' && value.trim() === '') return true
  if (Array.isArray(value) && value.length === 0) return true
  return false
}

/**
 * 获取值的类型
 */
function getValueType(value: any): string {
  if (value === null) return 'null'
  if (value === undefined) return 'undefined'
  if (Array.isArray(value)) return 'array'
  return typeof value
}

/**
 * 校验必填规则
 */
function validateRequired(value: any, rule: RequiredRule, displayName: string): ValidationError | null {
  if (!rule.required) return null
  
  if (isEmpty(value)) {
    return {
      paramName: displayName,
      ruleType: 'required',
      message: rule.message || `${displayName}不能为空`,
      actualValue: value
    }
  }
  return null
}

/**
 * 校验类型规则
 */
function validateType(value: any, rule: TypeRule, displayName: string): ValidationError | null {
  // 空值不校验类型（由required规则处理）
  if (isEmpty(value)) return null

  const actualType = getValueType(value)
  let valid = false
  let expectedTypeLabel = rule.dataType

  switch (rule.dataType) {
    case 'string':
      valid = typeof value === 'string'
      expectedTypeLabel = '字符串'
      break
    case 'number':
      valid = typeof value === 'number' && !isNaN(value)
      expectedTypeLabel = '数字'
      break
    case 'integer':
      valid = typeof value === 'number' && Number.isInteger(value)
      expectedTypeLabel = '整数'
      break
    case 'boolean':
      valid = typeof value === 'boolean'
      expectedTypeLabel = '布尔值'
      break
    case 'date':
    case 'datetime':
      if (value instanceof Date) {
        valid = !isNaN(value.getTime())
      } else if (typeof value === 'string') {
        const date = new Date(value)
        valid = !isNaN(date.getTime())
      }
      expectedTypeLabel = rule.dataType === 'date' ? '日期' : '日期时间'
      break
    case 'array':
      valid = Array.isArray(value)
      expectedTypeLabel = '数组'
      break
    default:
      valid = true
  }

  if (!valid) {
    return {
      paramName: displayName,
      ruleType: 'type',
      message: rule.message || `${displayName}必须是${expectedTypeLabel}类型`,
      actualValue: value
    }
  }
  return null
}

/**
 * 校验范围规则
 */
function validateRange(value: any, rule: RangeRule, displayName: string): ValidationError | null {
  if (isEmpty(value)) return null

  const num = Number(value)
  if (isNaN(num)) {
    return {
      paramName: displayName,
      ruleType: 'range',
      message: rule.message || `${displayName}必须是有效的数字`,
      actualValue: value
    }
  }

  const minInclusive = rule.minInclusive !== false
  const maxInclusive = rule.maxInclusive !== false

  if (rule.min !== undefined) {
    const minValid = minInclusive ? num >= rule.min : num > rule.min
    if (!minValid) {
      const boundary = minInclusive ? '大于等于' : '大于'
      return {
        paramName: displayName,
        ruleType: 'range',
        message: rule.message || `${displayName}必须${boundary}${rule.min}`,
        actualValue: value
      }
    }
  }

  if (rule.max !== undefined) {
    const maxValid = maxInclusive ? num <= rule.max : num < rule.max
    if (!maxValid) {
      const boundary = maxInclusive ? '小于等于' : '小于'
      return {
        paramName: displayName,
        ruleType: 'range',
        message: rule.message || `${displayName}必须${boundary}${rule.max}`,
        actualValue: value
      }
    }
  }

  return null
}

/**
 * 校验长度规则
 */
function validateLength(value: any, rule: LengthRule, displayName: string): ValidationError | null {
  if (isEmpty(value)) return null

  let length: number
  if (typeof value === 'string') {
    length = value.length
  } else if (Array.isArray(value)) {
    length = value.length
  } else {
    return null // 非字符串/数组不校验长度
  }

  if (rule.minLength !== undefined && length < rule.minLength) {
    return {
      paramName: displayName,
      ruleType: 'length',
      message: rule.message || `${displayName}长度不能少于${rule.minLength}`,
      actualValue: value
    }
  }

  if (rule.maxLength !== undefined && length > rule.maxLength) {
    return {
      paramName: displayName,
      ruleType: 'length',
      message: rule.message || `${displayName}长度不能超过${rule.maxLength}`,
      actualValue: value
    }
  }

  return null
}

/**
 * 校验正则表达式规则
 */
function validatePattern(value: any, rule: PatternRule, displayName: string): ValidationError | null {
  if (isEmpty(value)) return null

  try {
    const regex = new RegExp(rule.pattern, rule.flags)
    if (!regex.test(String(value))) {
      return {
        paramName: displayName,
        ruleType: 'pattern',
        message: rule.message || `${displayName}格式不正确`,
        actualValue: value
      }
    }
  } catch (e) {
    logger.error(`Invalid regex pattern: ${rule.pattern}`, e)
    return {
      paramName: displayName,
      ruleType: 'pattern',
      message: '校验规则配置错误',
      actualValue: value
    }
  }

  return null
}

/**
 * 校验枚举规则
 */
function validateEnum(value: any, rule: EnumRule, displayName: string): ValidationError | null {
  if (isEmpty(value)) return null

  if (!rule.values.includes(value)) {
    return {
      paramName: displayName,
      ruleType: 'enum',
      message: rule.message || `${displayName}的值不在允许的范围内`,
      actualValue: value
    }
  }

  return null
}

/**
 * 校验自定义规则
 */
function validateCustom(value: any, rule: CustomRule, displayName: string): ValidationError | null {
  if (isEmpty(value)) return null

  // 优先使用规则中的validator函数
  let validator = rule.validator

  // 如果没有提供validator，尝试使用内置校验器
  if (!validator && rule.validatorName) {
    validator = builtInValidators[rule.validatorName]
  }

  if (!validator) {
    logger.warn(`Custom validator not found: ${rule.validatorName}`)
    return null
  }

  const result = validator(value, rule.params)
  
  if (result === true) {
    return null
  }

  return {
    paramName: displayName,
    ruleType: 'custom',
    message: typeof result === 'string' ? result : (rule.message || `${displayName}校验失败`),
    actualValue: value
  }
}

// ============================================================================
// 主要导出函数
// ============================================================================

/**
 * 校验单个参数值
 * 
 * @param value 参数值
 * @param config 校验配置
 * @returns 校验结果
 */
export function validateParameter(value: any, config: ParameterValidationConfig): ValidationResult {
  const errors: ValidationError[] = []
  const displayName = config.displayName || config.paramName

  for (const rule of config.rules) {
    // 跳过禁用的规则
    if (rule.enabled === false) continue

    let error: ValidationError | null = null

    switch (rule.type) {
      case 'required':
        error = validateRequired(value, rule, displayName)
        break
      case 'type':
        error = validateType(value, rule, displayName)
        break
      case 'range':
        error = validateRange(value, rule, displayName)
        break
      case 'length':
        error = validateLength(value, rule, displayName)
        break
      case 'pattern':
        error = validatePattern(value, rule, displayName)
        break
      case 'enum':
        error = validateEnum(value, rule, displayName)
        break
      case 'custom':
        error = validateCustom(value, rule, displayName)
        break
    }

    if (error) {
      errors.push(error)
      // 如果是必填校验失败，后续规则不再校验
      if (rule.type === 'required') break
    }
  }

  return {
    valid: errors.length === 0,
    errors
  }
}

/**
 * 批量校验多个参数
 * 
 * @param values 参数值映射
 * @param configs 校验配置列表
 * @returns 批量校验结果
 */
export function validateParameters(
  values: Record<string, any>,
  configs: ParameterValidationConfig[]
): BatchValidationResult {
  const errorsByParam: Record<string, ValidationError[]> = {}
  const allErrors: ValidationError[] = []

  for (const config of configs) {
    const value = values[config.paramName]
    const result = validateParameter(value, config)

    if (!result.valid) {
      errorsByParam[config.paramName] = result.errors
      allErrors.push(...result.errors)
    }
  }

  return {
    valid: allErrors.length === 0,
    errorsByParam,
    allErrors,
    firstError: allErrors.length > 0 ? allErrors[0].message : undefined
  }
}

/**
 * 注册自定义校验器
 * 
 * @param name 校验器名称
 * @param validator 校验函数
 */
export function registerValidator(
  name: string,
  validator: (value: any, params?: Record<string, any>) => boolean | string
): void {
  builtInValidators[name] = validator
  logger.debug(`Registered custom validator: ${name}`)
}

/**
 * 获取所有内置校验器名称
 */
export function getBuiltInValidatorNames(): string[] {
  return Object.keys(builtInValidators)
}

// ============================================================================
// 辅助函数 - 创建校验规则
// ============================================================================

/**
 * 创建必填规则
 */
export function required(message?: string): RequiredRule {
  return { type: 'required', required: true, message }
}

/**
 * 创建类型规则
 */
export function dataType(type: ParameterDataType, message?: string): TypeRule {
  return { type: 'type', dataType: type, message }
}

/**
 * 创建范围规则
 */
export function range(min?: number, max?: number, message?: string): RangeRule {
  return { type: 'range', min, max, message }
}

/**
 * 创建长度规则
 */
export function length(minLength?: number, maxLength?: number, message?: string): LengthRule {
  return { type: 'length', minLength, maxLength, message }
}

/**
 * 创建正则规则
 */
export function pattern(regex: string, message?: string, flags?: string): PatternRule {
  return { type: 'pattern', pattern: regex, message, flags }
}

/**
 * 创建枚举规则
 */
export function enumValues(values: (string | number | boolean)[], message?: string): EnumRule {
  return { type: 'enum', values, message }
}

/**
 * 创建自定义规则
 */
export function custom(
  validatorName: string,
  params?: Record<string, any>,
  message?: string
): CustomRule {
  return { type: 'custom', validatorName, params, message }
}

// ============================================================================
// 导出类型和常量
// ============================================================================

export { builtInValidators }

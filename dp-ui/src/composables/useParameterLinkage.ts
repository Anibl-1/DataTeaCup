/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 参数级联选择 Composable
 * 用于管理报表参数之间的级联联动关系
 * 
 * 功能：
 * - 支持父子参数关系（如：省份 -> 城市）
 * - 支持多级级联（如：国家 -> 省份 -> 城市）
 * - 父参数变化时自动刷新子参数选项
 * - 支持从数据库动态获取参数选项
 * - 加载状态和错误处理
 * - 前端参数校验
 * 
 * @validates 需求 13.1 - 参数级联选择
 * @validates 需求 13.4 - 实现前后端双重参数校验
 */
import { ref, computed, type Ref, type ComputedRef } from 'vue'
import { logger } from '@/utils/logger'
import { 
  validateParameter, 
  validateParameters,
  type ParameterValidationConfig,
  type ValidationResult,
  type BatchValidationResult,
  type ValidationRule
} from '@/services/parameterValidationService'

/**
 * 参数选项
 */
export interface ParameterOption {
  /** 显示标签 */
  label: string
  /** 选项值 */
  value: string | number
  /** 子选项（用于级联选择器） */
  children?: ParameterOption[]
  /** 是否禁用 */
  disabled?: boolean
}

/**
 * 联动类型
 */
export type LinkageType = 'cascade' | 'filter' | 'compute'

/**
 * 级联配置
 */
export interface CascadeConfig {
  /** 源字段名（父参数关联的字段） */
  sourceField: string
  /** 目标字段名（子参数关联的字段） */
  targetField: string
  /** 数据源ID */
  dataSourceId: number
  /** SQL查询语句，支持 ${parentValue} 占位符 */
  sql: string
  /** 标签字段名 */
  labelField?: string
  /** 值字段名 */
  valueField?: string
}

/**
 * 过滤配置
 */
export interface FilterConfig {
  /** 过滤表达式 */
  expression: string
  /** 过滤字段 */
  filterField: string
}

/**
 * 计算配置
 */
export interface ComputeConfig {
  /** 计算表达式 */
  expression: string
  /** 依赖的参数列表 */
  dependencies: string[]
}

/**
 * 参数联动配置
 */
export interface ParameterLinkage {
  /** 联动ID */
  id: string
  /** 源参数名（父参数） */
  sourceParam: string
  /** 目标参数名（子参数） */
  targetParam: string
  /** 联动类型 */
  linkageType: LinkageType
  /** 联动配置 */
  linkageConfig: CascadeConfig | FilterConfig | ComputeConfig
  /** 是否启用 */
  enabled: boolean
}

/**
 * 参数状态
 */
export interface ParameterState {
  /** 参数名 */
  name: string
  /** 当前值 */
  value: any
  /** 选项列表 */
  options: ParameterOption[]
  /** 是否正在加载选项 */
  loading: boolean
  /** 错误信息 */
  error: string | null
  /** 父参数列表 */
  parents: string[]
  /** 子参数列表 */
  children: string[]
  /** 是否已初始化 */
  initialized: boolean
  /** 校验规则 */
  validationRules?: ValidationRule[]
  /** 校验错误列表 */
  validationErrors?: string[]
  /** 是否校验通过 */
  isValid?: boolean
}

/**
 * 选项加载函数类型
 */
export type OptionsLoader = (
  paramName: string,
  dependencies: Record<string, any>,
  config: CascadeConfig
) => Promise<ParameterOption[]>

/**
 * 参数联动配置选项
 */
export interface UseParameterLinkageOptions {
  /** 选项加载函数 */
  optionsLoader?: OptionsLoader
  /** 参数值变化回调 */
  onValueChange?: (paramName: string, value: any, oldValue: any) => void
  /** 选项加载完成回调 */
  onOptionsLoaded?: (paramName: string, options: ParameterOption[]) => void
  /** 错误回调 */
  onError?: (paramName: string, error: Error) => void
  /** 是否自动清除子参数值 */
  autoClearChildren?: boolean
  /** 加载选项的防抖延迟（毫秒） */
  debounceDelay?: number
  /** 是否在值变化时自动校验 */
  autoValidate?: boolean
  /** 校验失败回调 */
  onValidationError?: (paramName: string, errors: string[]) => void
}

/**
 * 参数联动返回类型
 */
export interface UseParameterLinkageReturn {
  /** 参数状态映射 */
  parameters: Ref<Record<string, ParameterState>>
  /** 联动配置列表 */
  linkages: Ref<ParameterLinkage[]>
  /** 是否有任何参数正在加载 */
  isAnyLoading: ComputedRef<boolean>
  /** 是否所有参数都校验通过 */
  isAllValid: ComputedRef<boolean>
  /** 注册参数 */
  registerParameter: (name: string, initialValue?: any, initialOptions?: ParameterOption[], validationRules?: ValidationRule[]) => void
  /** 注销参数 */
  unregisterParameter: (name: string) => void
  /** 添加联动配置 */
  addLinkage: (linkage: Omit<ParameterLinkage, 'id'>) => string
  /** 移除联动配置 */
  removeLinkage: (linkageId: string) => void
  /** 设置参数值 */
  setParameterValue: (name: string, value: any) => Promise<void>
  /** 获取参数值 */
  getParameterValue: (name: string) => any
  /** 获取参数选项 */
  getParameterOptions: (name: string) => ParameterOption[]
  /** 刷新参数选项 */
  refreshOptions: (name: string) => Promise<void>
  /** 获取参数的所有依赖值 */
  getDependencyValues: (name: string) => Record<string, any>
  /** 清除所有参数值 */
  clearAll: () => void
  /** 重置参数到初始状态 */
  reset: () => void
  /** 获取参数加载状态 */
  isLoading: (name: string) => boolean
  /** 获取参数错误信息 */
  getError: (name: string) => string | null
  /** 批量设置参数值 */
  setParameterValues: (values: Record<string, any>) => Promise<void>
  /** 获取所有参数值 */
  getAllValues: () => Record<string, any>
  /** 验证参数依赖是否满足 */
  validateDependencies: (name: string) => boolean
  /** 获取参数的级联层级 */
  getCascadeLevel: (name: string) => number
  /** 获取根参数列表（没有父参数的参数） */
  getRootParameters: () => string[]
  /** 获取叶子参数列表（没有子参数的参数） */
  getLeafParameters: () => string[]
  /** 校验单个参数 */
  validateParam: (name: string) => ValidationResult
  /** 校验所有参数 */
  validateAllParams: () => BatchValidationResult
  /** 设置参数校验规则 */
  setValidationRules: (name: string, rules: ValidationRule[]) => void
  /** 获取参数校验错误 */
  getValidationErrors: (name: string) => string[]
  /** 清除参数校验错误 */
  clearValidationErrors: (name: string) => void
}

/**
 * 生成唯一ID
 */
let linkageIdCounter = 0
function generateLinkageId(): string {
  return `linkage_${Date.now()}_${++linkageIdCounter}`
}

/**
 * 重置ID计数器（用于测试）
 */
export function resetLinkageIdCounter(): void {
  linkageIdCounter = 0
}

/**
 * 默认选项加载函数
 * 实际项目中应该通过 API 调用后端获取数据
 */
const defaultOptionsLoader: OptionsLoader = async (
  paramName: string,
  _dependencies: Record<string, any>,
  _config: CascadeConfig
): Promise<ParameterOption[]> => {
  // 这是一个占位实现，实际应该调用后端 API
  logger.warn(`使用默认选项加载器，参数: ${paramName}，请提供自定义 optionsLoader`)
  return []
}

/**
 * 参数级联选择 Composable
 */
export function useParameterLinkage(
  options: UseParameterLinkageOptions = {}
): UseParameterLinkageReturn {
  const {
    optionsLoader = defaultOptionsLoader,
    onValueChange,
    onOptionsLoaded,
    onError,
    autoClearChildren = true,
    debounceDelay = 300,
    autoValidate = false,
    onValidationError
  } = options

  // 参数状态存储
  const parameters = ref<Record<string, ParameterState>>({})
  
  // 联动配置存储
  const linkages = ref<ParameterLinkage[]>([])
  
  // 初始值存储（用于重置）
  const initialValues = ref<Record<string, any>>({})
  const initialOptions = ref<Record<string, ParameterOption[]>>({})
  const initialValidationRules = ref<Record<string, ValidationRule[]>>({})
  
  // 防抖定时器存储
  const debounceTimers = new Map<string, ReturnType<typeof setTimeout>>()

  /**
   * 是否有任何参数正在加载
   */
  const isAnyLoading = computed(() => {
    return Object.values(parameters.value).some(p => p.loading)
  })

  /**
   * 是否所有参数都校验通过
   */
  const isAllValid = computed(() => {
    return Object.values(parameters.value).every(p => p.isValid !== false)
  })

  /**
   * 注册参数
   */
  const registerParameter = (
    name: string,
    initialValue: any = null,
    options: ParameterOption[] = [],
    validationRules: ValidationRule[] = []
  ): void => {
    if (parameters.value[name]) {
      logger.warn(`参数 ${name} 已存在，将被覆盖`)
    }

    parameters.value[name] = {
      name,
      value: initialValue,
      options,
      loading: false,
      error: null,
      parents: [],
      children: [],
      initialized: true,
      validationRules,
      validationErrors: [],
      isValid: true
    }

    // 保存初始值用于重置
    initialValues.value[name] = initialValue
    initialOptions.value[name] = [...options]
    initialValidationRules.value[name] = [...validationRules]

    // 更新联动关系
    updateLinkageRelations()
  }

  /**
   * 注销参数
   */
  const unregisterParameter = (name: string): void => {
    if (!parameters.value[name]) {
      return
    }

    // 清除相关的防抖定时器
    const timer = debounceTimers.get(name)
    if (timer) {
      clearTimeout(timer)
      debounceTimers.delete(name)
    }

    // 移除相关的联动配置
    linkages.value = linkages.value.filter(
      l => l.sourceParam !== name && l.targetParam !== name
    )

    // 删除参数
    delete parameters.value[name]
    delete initialValues.value[name]
    delete initialOptions.value[name]

    // 更新联动关系
    updateLinkageRelations()
  }

  /**
   * 更新联动关系
   */
  const updateLinkageRelations = (): void => {
    // 重置所有参数的父子关系
    Object.values(parameters.value).forEach(param => {
      param.parents = []
      param.children = []
    })

    // 根据联动配置建立关系
    linkages.value.forEach(linkage => {
      if (!linkage.enabled) return

      const sourceParam = parameters.value[linkage.sourceParam]
      const targetParam = parameters.value[linkage.targetParam]

      if (sourceParam && targetParam) {
        if (!sourceParam.children.includes(linkage.targetParam)) {
          sourceParam.children.push(linkage.targetParam)
        }
        if (!targetParam.parents.includes(linkage.sourceParam)) {
          targetParam.parents.push(linkage.sourceParam)
        }
      }
    })
  }

  /**
   * 添加联动配置
   */
  const addLinkage = (linkage: Omit<ParameterLinkage, 'id'>): string => {
    const id = generateLinkageId()
    const newLinkage: ParameterLinkage = {
      ...linkage,
      id,
      enabled: linkage.enabled ?? true
    }

    linkages.value.push(newLinkage)
    updateLinkageRelations()

    return id
  }

  /**
   * 移除联动配置
   */
  const removeLinkage = (linkageId: string): void => {
    linkages.value = linkages.value.filter(l => l.id !== linkageId)
    updateLinkageRelations()
  }

  /**
   * 获取参数的所有依赖值
   */
  const getDependencyValues = (name: string): Record<string, any> => {
    const param = parameters.value[name]
    if (!param) return {}

    const result: Record<string, any> = {}
    
    // 递归获取所有父参数的值
    const collectParentValues = (paramName: string): void => {
      const p = parameters.value[paramName]
      if (!p) return

      p.parents.forEach(parentName => {
        const parent = parameters.value[parentName]
        if (parent) {
          result[parentName] = parent.value
          collectParentValues(parentName)
        }
      })
    }

    collectParentValues(name)
    return result
  }

  /**
   * 验证参数依赖是否满足
   */
  const validateDependencies = (name: string): boolean => {
    const param = parameters.value[name]
    if (!param) return false

    // 检查所有父参数是否都有值
    return param.parents.every(parentName => {
      const parent = parameters.value[parentName]
      return parent && parent.value !== null && parent.value !== undefined && parent.value !== ''
    })
  }

  /**
   * 加载参数选项
   */
  const loadOptions = async (name: string): Promise<void> => {
    const param = parameters.value[name]
    if (!param) return

    // 查找该参数的级联配置
    const linkage = linkages.value.find(
      l => l.targetParam === name && l.linkageType === 'cascade' && l.enabled
    )

    if (!linkage) return

    const config = linkage.linkageConfig as CascadeConfig

    // 检查依赖是否满足
    if (!validateDependencies(name)) {
      param.options = []
      param.error = null
      return
    }

    // 获取依赖值
    const dependencies = getDependencyValues(name)

    // 设置加载状态
    param.loading = true
    param.error = null

    try {
      const options = await optionsLoader(name, dependencies, config)
      param.options = options
      param.loading = false
      
      onOptionsLoaded?.(name, options)
      logger.debug(`参数 ${name} 选项加载完成，共 ${options.length} 个选项`)
    } catch (error) {
      const err = error instanceof Error ? error : new Error(String(error))
      param.error = err.message
      param.loading = false
      param.options = []
      
      onError?.(name, err)
      logger.error(`参数 ${name} 选项加载失败`, err)
    }
  }

  /**
   * 防抖加载选项
   */
  const debouncedLoadOptions = (name: string): void => {
    // 清除之前的定时器
    const existingTimer = debounceTimers.get(name)
    if (existingTimer) {
      clearTimeout(existingTimer)
    }

    // 设置新的定时器
    const timer = setTimeout(() => {
      loadOptions(name)
      debounceTimers.delete(name)
    }, debounceDelay)

    debounceTimers.set(name, timer)
  }

  /**
   * 刷新参数选项
   */
  const refreshOptions = async (name: string): Promise<void> => {
    await loadOptions(name)
  }

  /**
   * 清除子参数值
   */
  const clearChildrenValues = (name: string): void => {
    const param = parameters.value[name]
    if (!param) return

    param.children.forEach(childName => {
      const child = parameters.value[childName]
      if (child) {
        const oldValue = child.value
        child.value = null
        child.options = []
        child.error = null
        
        onValueChange?.(childName, null, oldValue)
        
        // 递归清除子参数的子参数
        clearChildrenValues(childName)
      }
    })
  }

  /**
   * 触发子参数选项刷新
   */
  const triggerChildrenRefresh = (name: string): void => {
    const param = parameters.value[name]
    if (!param) return

    param.children.forEach(childName => {
      debouncedLoadOptions(childName)
    })
  }

  /**
   * 设置参数值
   */
  const setParameterValue = async (name: string, value: any): Promise<void> => {
    const param = parameters.value[name]
    if (!param) {
      logger.warn(`参数 ${name} 不存在`)
      return
    }

    const oldValue = param.value
    
    // 如果值没有变化，不处理
    if (JSON.stringify(oldValue) === JSON.stringify(value)) {
      return
    }

    param.value = value
    
    // 自动校验
    if (autoValidate && param.validationRules && param.validationRules.length > 0) {
      const result = validateParam(name)
      if (!result.valid && onValidationError) {
        onValidationError(name, result.errors.map(e => e.message))
      }
    }
    
    // 触发值变化回调
    onValueChange?.(name, value, oldValue)

    // 如果启用了自动清除子参数
    if (autoClearChildren) {
      clearChildrenValues(name)
    }

    // 触发子参数选项刷新
    triggerChildrenRefresh(name)
  }

  /**
   * 获取参数值
   */
  const getParameterValue = (name: string): any => {
    return parameters.value[name]?.value ?? null
  }

  /**
   * 获取参数选项
   */
  const getParameterOptions = (name: string): ParameterOption[] => {
    return parameters.value[name]?.options ?? []
  }

  /**
   * 获取参数加载状态
   */
  const isLoading = (name: string): boolean => {
    return parameters.value[name]?.loading ?? false
  }

  /**
   * 获取参数错误信息
   */
  const getError = (name: string): string | null => {
    return parameters.value[name]?.error ?? null
  }

  /**
   * 清除所有参数值
   */
  const clearAll = (): void => {
    Object.keys(parameters.value).forEach(name => {
      const param = parameters.value[name]
      if (param) {
        param.value = null
        param.error = null
      }
    })
  }

  /**
   * 重置参数到初始状态
   */
  const reset = (): void => {
    Object.keys(parameters.value).forEach(name => {
      const param = parameters.value[name]
      if (param) {
        param.value = initialValues.value[name] ?? null
        param.options = [...(initialOptions.value[name] ?? [])]
        param.error = null
        param.loading = false
      }
    })
  }

  /**
   * 批量设置参数值
   */
  const setParameterValues = async (values: Record<string, any>): Promise<void> => {
    // 按照级联层级排序，先设置父参数
    const sortedNames = Object.keys(values).sort((a, b) => {
      return getCascadeLevel(a) - getCascadeLevel(b)
    })

    for (const name of sortedNames) {
      await setParameterValue(name, values[name])
    }
  }

  /**
   * 获取所有参数值
   */
  const getAllValues = (): Record<string, any> => {
    const result: Record<string, any> = {}
    Object.entries(parameters.value).forEach(([name, param]) => {
      result[name] = param.value
    })
    return result
  }

  /**
   * 获取参数的级联层级（0 表示根参数）
   */
  const getCascadeLevel = (name: string): number => {
    const param = parameters.value[name]
    if (!param || param.parents.length === 0) {
      return 0
    }

    // 计算最大父级深度
    let maxLevel = 0
    param.parents.forEach(parentName => {
      const parentLevel = getCascadeLevel(parentName)
      maxLevel = Math.max(maxLevel, parentLevel + 1)
    })

    return maxLevel
  }

  /**
   * 获取根参数列表（没有父参数的参数）
   */
  const getRootParameters = (): string[] => {
    return Object.entries(parameters.value)
      .filter(([_, param]) => param.parents.length === 0)
      .map(([name]) => name)
  }

  /**
   * 获取叶子参数列表（没有子参数的参数）
   */
  const getLeafParameters = (): string[] => {
    return Object.entries(parameters.value)
      .filter(([_, param]) => param.children.length === 0)
      .map(([name]) => name)
  }

  // ========================================================================
  // 参数校验方法
  // @validates 需求 13.4 - 实现前后端双重参数校验
  // ========================================================================

  /**
   * 校验单个参数
   */
  const validateParam = (name: string): ValidationResult => {
    const param = parameters.value[name]
    if (!param) {
      return { valid: true, errors: [] }
    }

    if (!param.validationRules || param.validationRules.length === 0) {
      param.isValid = true
      param.validationErrors = []
      return { valid: true, errors: [] }
    }

    const config: ParameterValidationConfig = {
      paramName: name,
      displayName: name,
      rules: param.validationRules
    }

    const result = validateParameter(param.value, config)
    
    // 更新参数状态
    param.isValid = result.valid
    param.validationErrors = result.errors.map(e => e.message)

    return result
  }

  /**
   * 校验所有参数
   */
  const validateAllParams = (): BatchValidationResult => {
    const values: Record<string, any> = {}
    const configs: ParameterValidationConfig[] = []

    Object.entries(parameters.value).forEach(([name, param]) => {
      values[name] = param.value
      if (param.validationRules && param.validationRules.length > 0) {
        configs.push({
          paramName: name,
          displayName: name,
          rules: param.validationRules
        })
      }
    })

    const result = validateParameters(values, configs)

    // 更新各参数的校验状态
    Object.entries(parameters.value).forEach(([name, param]) => {
      const paramErrors = result.errorsByParam[name]
      if (paramErrors && paramErrors.length > 0) {
        param.isValid = false
        param.validationErrors = paramErrors.map(e => e.message)
      } else {
        param.isValid = true
        param.validationErrors = []
      }
    })

    return result
  }

  /**
   * 设置参数校验规则
   */
  const setValidationRules = (name: string, rules: ValidationRule[]): void => {
    const param = parameters.value[name]
    if (!param) {
      logger.warn(`参数 ${name} 不存在`)
      return
    }
    param.validationRules = rules
    // 重新校验
    if (autoValidate) {
      validateParam(name)
    }
  }

  /**
   * 获取参数校验错误
   */
  const getValidationErrors = (name: string): string[] => {
    return parameters.value[name]?.validationErrors ?? []
  }

  /**
   * 清除参数校验错误
   */
  const clearValidationErrors = (name: string): void => {
    const param = parameters.value[name]
    if (param) {
      param.validationErrors = []
      param.isValid = true
    }
  }

  return {
    // 状态
    parameters,
    linkages,
    isAnyLoading,
    isAllValid,
    
    // 参数管理
    registerParameter,
    unregisterParameter,
    
    // 联动管理
    addLinkage,
    removeLinkage,
    
    // 值操作
    setParameterValue,
    getParameterValue,
    setParameterValues,
    getAllValues,
    clearAll,
    reset,
    
    // 选项操作
    getParameterOptions,
    refreshOptions,
    getDependencyValues,
    
    // 状态查询
    isLoading,
    getError,
    validateDependencies,
    
    // 层级查询
    getCascadeLevel,
    getRootParameters,
    getLeafParameters,
    
    // 校验方法
    validateParam,
    validateAllParams,
    setValidationRules,
    getValidationErrors,
    clearValidationErrors
  }
}

/**
 * 创建级联配置的辅助函数
 */
export function createCascadeConfig(config: {
  sourceField: string
  targetField: string
  dataSourceId: number
  sql: string
  labelField?: string
  valueField?: string
}): CascadeConfig {
  return {
    sourceField: config.sourceField,
    targetField: config.targetField,
    dataSourceId: config.dataSourceId,
    sql: config.sql,
    labelField: config.labelField ?? 'label',
    valueField: config.valueField ?? 'value'
  }
}

/**
 * 创建参数联动配置的辅助函数
 */
export function createParameterLinkage(config: {
  sourceParam: string
  targetParam: string
  linkageType?: LinkageType
  linkageConfig: CascadeConfig | FilterConfig | ComputeConfig
  enabled?: boolean
}): Omit<ParameterLinkage, 'id'> {
  return {
    sourceParam: config.sourceParam,
    targetParam: config.targetParam,
    linkageType: config.linkageType ?? 'cascade',
    linkageConfig: config.linkageConfig,
    enabled: config.enabled ?? true
  }
}

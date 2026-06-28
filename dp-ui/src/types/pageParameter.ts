/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 页面参数组件类型定义
 * 类似帆软报表的参数面板设计
 */

/**
 * 查询组件类型
 */
export type QueryComponentType = 
  | 'text'        // 文本输入框
  | 'number'      // 数字输入框
  | 'date'        // 日期选择器
  | 'dateRange'   // 日期范围选择器
  | 'select'      // 下拉单选
  | 'multiSelect' // 下拉多选
  | 'cascader'    // 级联选择

/**
 * 查询组件选项（用于下拉选择）
 */
export interface QueryComponentOption {
  label: string
  value: string | number
  children?: QueryComponentOption[]
}

/**
 * 图表参数关联配置
 */
export interface ChartParameterBinding {
  /** 关联的图表ID */
  chartId: number
  /** 关联的字段名 */
  field: string
  /** 运算符 */
  operator: '=' | '!=' | '>' | '>=' | '<' | '<=' | 'LIKE' | 'IN' | 'BETWEEN' | 'NOT IN'
}

/**
 * 查询组件定义
 */
export interface QueryComponent {
  /** 组件唯一ID */
  id: string
  /** 组件类型 */
  type: QueryComponentType
  /** 参数名称（用于SQL ${name} 占位符匹配） */
  name: string
  /** 显示标签 */
  label: string
  /** 占位提示 */
  placeholder?: string
  /** 默认值 */
  defaultValue?: any
  /** 是否必填 */
  required?: boolean
  /** 组件宽度（像素） */
  width?: number
  /** 排序顺序 */
  sortOrder?: number
  /** 下拉选项（type为select/multiSelect/cascader时使用） */
  options?: QueryComponentOption[]
  /** 选项数据来源：static-静态配置, sql-SQL查询 */
  optionSource?: 'static' | 'sql'
  /** SQL查询语句（optionSource为sql时使用） */
  optionSql?: string
  /** SQL查询的数据源ID */
  optionDataSourceId?: number
  /** 日期格式（type为date/dateRange时使用） */
  dateFormat?: string
  /** 最小值（type为number时使用） */
  min?: number
  /** 最大值（type为number时使用） */
  max?: number
  /** 关联的图表参数配置（旧版，保留兼容） */
  bindings: ChartParameterBinding[]
}

/**
 * 页面参数面板配置
 */
export interface PageParameterPanel {
  /** 是否显示参数面板 */
  visible: boolean
  /** 面板高度（像素） */
  height?: number
  /** 查询组件列表 */
  components: QueryComponent[]
  /** 是否显示查询按钮 */
  showQueryButton?: boolean
  /** 是否显示重置按钮 */
  showResetButton?: boolean
  /** 是否自动查询（参数变化时自动刷新） */
  autoQuery?: boolean
}

/**
 * 查询组件类型配置
 */
export const QUERY_COMPONENT_TYPES: Array<{
  type: QueryComponentType
  label: string
  icon: string
  description: string
}> = [
  { type: 'text', label: '文本框', icon: 'TextOutline', description: '单行文本输入' },
  { type: 'number', label: '数字框', icon: 'CalculatorOutline', description: '数字输入' },
  { type: 'date', label: '日期', icon: 'CalendarOutline', description: '日期选择' },
  { type: 'dateRange', label: '日期范围', icon: 'CalendarOutline', description: '日期范围选择' },
  { type: 'select', label: '下拉单选', icon: 'ChevronDownOutline', description: '下拉单选' },
  { type: 'multiSelect', label: '下拉多选', icon: 'CheckboxOutline', description: '下拉多选' },
  { type: 'cascader', label: '级联选择', icon: 'GitBranchOutline', description: '级联选择' }
]

/**
 * 运算符选项
 */
export const OPERATOR_OPTIONS = [
  { label: '等于 (=)', value: '=' },
  { label: '不等于 (!=)', value: '!=' },
  { label: '大于 (>)', value: '>' },
  { label: '大于等于 (>=)', value: '>=' },
  { label: '小于 (<)', value: '<' },
  { label: '小于等于 (<=)', value: '<=' },
  { label: '包含 (LIKE)', value: 'LIKE' },
  { label: '在列表中 (IN)', value: 'IN' },
  { label: '范围 (BETWEEN)', value: 'BETWEEN' },
  { label: '不在列表中 (NOT IN)', value: 'NOT IN' }
]

/**
 * 创建默认查询组件
 */
export function createDefaultQueryComponent(type: QueryComponentType): QueryComponent {
  const id = `param_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  const typeConfig = QUERY_COMPONENT_TYPES.find(t => t.type === type)
  
  return {
    id,
    type,
    name: id,
    label: typeConfig?.label || '参数',
    placeholder: `请输入${typeConfig?.label || '参数'}`,
    required: false,
    width: type === 'dateRange' ? 320 : type === 'cascader' ? 240 : 220,
    sortOrder: 0,
    bindings: [],
    ...(type === 'date' || type === 'dateRange' ? { dateFormat: 'yyyy-MM-dd' } : {}),
    ...(type === 'select' || type === 'multiSelect' ? { options: [], optionSource: 'static' as const } : {})
  }
}

/**
 * 创建默认参数面板配置
 */
export function createDefaultParameterPanel(): PageParameterPanel {
  return {
    visible: false,
    height: 60,
    components: [],
    showQueryButton: true,
    showResetButton: true,
    autoQuery: false
  }
}

/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 报表定义相关类型定义
 */

/**
 * 报表查询参数（SQL 中的 ${param_name} 占位符）
 */
export interface ReportParam {
  /** 参数名（从 SQL 解析，如 start_date） */
  name: string
  /** 显示标签（如“开始日期”） */
  label: string
  /** 输入类型 */
  inputType: 'text' | 'number' | 'date' | 'daterange' | 'select' | 'department'
  /** 是否必填 */
  required: boolean
  /** 默认值 */
  defaultValue?: string
  /** 关联字典类型（inputType=select 时使用） */
  dictType?: string
  /** 自定义下拉选项（inputType=select 时使用） */
  options?: Array<{ label: string; value: string }>
}

/**
 * 报表字段信息
 */
export interface ReportField {
  /** 字段ID */
  id?: number
  /** 报表ID */
  reportId?: number
  /** 字段名称（数据库字段名） */
  fieldName: string
  /** 字段显示名称（表头） */
  fieldLabel?: string
  /** 字段类型 */
  fieldType?: string
  /** 排序顺序 */
  sortOrder?: number
  /** 是否可见：1-可见，0-隐藏 */
  isVisible?: number
  /** 列宽度 */
  width?: number
  /** 对齐方式：left,center,right */
  align?: string
  /** 关联的数据字典类型 */
  dictType?: string
}

/**
 * 报表定义信息
 */
export interface ReportDefinition {
  /** 报表ID */
  id: number
  /** 报表名称 */
  reportName: string
  /** 报表编码（唯一标识） */
  reportCode: string
  /** 数据源ID */
  dataSourceId: number
  /** SQL查询语句 */
  sqlContent: string
  /** 报表描述 */
  description?: string
  /** 状态：1-启用，0-禁用 */
  status: number
  /** 报表类型：sql-SQL模式，visual-可视化模式 */
  reportType?: string
  /** 允许导出Excel：1-允许，0-禁止 */
  allowExportExcel?: number
  /** 允许导出PDF：1-允许，0-禁止 */
  allowExportPdf?: number
  /** 允许打印：1-允许，0-禁止 */
  allowPrint?: number
  /** 创建人ID */
  createBy?: number
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
  /** 字段列表 */
  fields?: ReportField[]
  /** 查询参数配置 */
  params?: ReportParam[]
  /** 数据源信息 */
  dataSource?: any
  /** 是否发布到移动端：1-是，0-否 */
  mobileEnabled?: number
  /** 移动端排序 */
  mobileSortOrder?: number
}

/**
 * 报表定义表单
 */
export interface ReportDefinitionForm {
  /** 报表ID（更新时必填） */
  id?: number | null
  /** 报表名称 */
  reportName: string
  /** 报表编码（唯一标识） */
  reportCode: string
  /** 数据源ID */
  dataSourceId: number
  /** SQL查询语句 */
  sqlContent: string
  /** 报表描述 */
  description?: string
  /** 状态：1-启用，0-禁用 */
  status?: number
  /** 报表类型：sql-SQL模式，visual-可视化模式 */
  reportType?: string
  /** 允许导出Excel：1-允许，0-禁止 */
  allowExportExcel?: number
  /** 允许导出PDF：1-允许，0-禁止 */
  allowExportPdf?: number
  /** 允许打印：1-允许，0-禁止 */
  allowPrint?: number
  /** 水印类型：none-无水印，user_ip-用户名_IP，custom-自定义文本 */
  watermarkType?: string
  /** PDF导出水印文字 */
  pdfWatermark?: string
  /** 字段列表 */
  fields?: ReportField[]
  /** 查询参数配置 */
  params?: ReportParam[]
}


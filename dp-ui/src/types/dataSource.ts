/**
 * 数据源相关类型定义
 */

/**
 * 数据源信息
 */
export interface DataSource {
  /** 数据源ID */
  id: number
  /** 数据源名称 */
  name: string
  /** 数据库类型：mysql、postgresql、oracle、sqlserver */
  dbType: string
  /** 主机地址 */
  host: string
  /** 端口号 */
  port: number
  /** 数据库名 */
  database: string
  /** 用户名 */
  username: string
  /** 密码 */
  password: string
  /** 分组名称 */
  groupName?: string
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
  /** 最后测试结果: 1=正常, 0=异常, -1=未测试 */
  lastTestResult?: number
  /** 最后测试时间 */
  lastTestTime?: string
}

/**
 * 数据源创建/更新表单
 */
export interface DataSourceForm {
  /** 数据源ID（更新时必填） */
  id?: number | null
  /** 数据源名称 */
  name: string
  /** 数据库类型 */
  dbType: string
  /** 主机地址 */
  host: string
  /** 端口号 */
  port: number
  /** 数据库名 */
  database: string
  /** 用户名 */
  username: string
  /** 密码 */
  password: string
  /** 分组名称 */
  groupName?: string
}

/**
 * 数据库类型选项
 */
export interface DbTypeOption {
  label: string
  value: string
}


// ==================== API 请求参数类型 ====================

/**
 * 数据源列表查询参数
 */
export interface DataSourceQueryParams {
  /** 页码（从1开始） */
  page?: number
  /** 每页大小 */
  pageSize?: number
  /** 搜索关键词 */
  keyword?: string
  /** 筛选条件 */
  filters?: string
}

/**
 * 数据源创建参数
 */
export interface DataSourceCreateParams {
  /** 数据源名称 */
  name: string
  /** 数据库类型 */
  dbType: string
  /** 主机地址 */
  host: string
  /** 端口号 */
  port: number
  /** 数据库名 */
  database: string
  /** 用户名 */
  username: string
  /** 密码 */
  password: string
  /** 分组名称 */
  groupName?: string
}

/**
 * 数据源更新参数（需要 id）
 */
export interface DataSourceUpdateParams extends DataSourceCreateParams {
  /** 数据源ID */
  id: number
}

// ==================== API 响应数据类型 ====================

/**
 * 数据源表信息
 */
export interface DataSourceTable {
  /** 表名 */
  tableName: string
  /** 表类型 */
  tableType: string
}

/**
 * 表字段信息
 */
export interface TableColumn {
  /** 字段名 */
  columnName: string
  /** 数据类型 */
  dataType: string
  /** 字段大小 */
  columnSize: number
  /** 是否可空 */
  nullable: boolean
  /** 备注 */
  remarks?: string
}

/**
 * 批量测试连接结果
 */
export interface BatchTestResult {
  /** 数据源ID */
  id: number
  /** 数据源名称 */
  name: string
  /** 是否成功 */
  success: boolean
  /** 消息 */
  message: string
  /** 响应时间(ms) */
  responseTime?: number
}

/**
 * 数据源详情（含表列表）
 */
export interface DataSourceDetail extends DataSource {
  /** 表列表 */
  tables?: DataSourceTable[]
}

/**
 * 采集任务相关类型定义
 */

/**
 * 采集任务信息
 */
export interface CollectTask {
  /** 任务ID */
  id: number
  /** 任务名称 */
  taskName: string
  /** 源数据源ID */
  dataSourceId: number
  /** 源数据源名称（冗余字段） */
  dataSourceName: string
  /** 目标数据源ID（可选） */
  targetDataSourceId?: number | null
  /** 目标数据源名称（冗余字段） */
  targetDataSourceName?: string
  /** 源表名 */
  tableName: string
  /** 目标表名（可选） */
  targetTableName?: string
  /** 采集模式 */
  collectMode?: 'full' | 'incremental' | 'custom'
  /** 自定义SQL */
  customSql?: string
  /** 增量字段 */
  incrementalField?: string
  /** 增量字段类型 */
  incrementalType?: 'timestamp' | 'id'
  /** 上次采集值 */
  lastCollectValue?: string
  /** 字段映射配置 */
  fieldMapping?: string
  /** 数据转换规则 */
  transformRules?: string
  /** 批量大小 */
  batchSize?: number
  /** 是否自动创建表 */
  autoCreateTable?: boolean
  /** 状态：running-运行中，stopped-已停止，error-错误 */
  status: 'running' | 'stopped' | 'error'
  /** 是否启用定时任务 */
  scheduleEnabled?: boolean
  /** Cron表达式 */
  cronExpression?: string
  /** 定时任务描述 */
  scheduleDescription?: string
  /** 下次执行时间 */
  nextExecuteTime?: string
  /** 上次执行时间 */
  lastExecuteTime?: string
  /** 上次执行结果 */
  lastExecuteResult?: string
  /** 执行次数统计 */
  executeCount?: number
  /** 成功次数统计 */
  successCount?: number
  /** 失败次数统计 */
  failCount?: number
  /** 最大重试次数 */
  maxRetryCount?: number
  /** 重试间隔(秒) */
  retryInterval?: number
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/**
 * 采集任务创建/更新表单
 */
export interface CollectTaskForm {
  /** 任务ID（更新时必填） */
  id?: number | null
  /** 任务名称 */
  taskName: string
  /** 源数据源ID */
  dataSourceId: number | null
  /** 目标数据源ID（可选） */
  targetDataSourceId?: number | null
  /** 源表名 */
  tableName: string
  /** 目标表名（可选） */
  targetTableName?: string
  /** 采集模式 */
  collectMode?: 'full' | 'incremental' | 'custom'
  /** 自定义SQL */
  customSql?: string
  /** 增量字段 */
  incrementalField?: string
  /** 增量字段类型 */
  incrementalType?: 'timestamp' | 'id'
  /** 字段映射配置 */
  fieldMapping?: string
  /** 数据转换规则 */
  transformRules?: string
  /** 批量大小 */
  batchSize?: number
  /** 是否自动创建表 */
  autoCreateTable?: boolean
  /** 是否启用定时任务 */
  scheduleEnabled?: boolean
  /** Cron表达式 */
  cronExpression?: string
  /** 定时任务描述 */
  scheduleDescription?: string
  /** 最大重试次数 */
  maxRetryCount?: number
  /** 重试间隔(秒) */
  retryInterval?: number
}

/**
 * 任务状态配置
 */
export interface TaskStatusConfig {
  type: 'success' | 'default' | 'error' | 'warning'
  text: string
}


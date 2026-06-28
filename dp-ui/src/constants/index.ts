/**
 * 常量定义
 */

/**
 * 默认分页配置
 */
export const DEFAULT_PAGE_SIZE = 10
export const DEFAULT_PAGE = 1
export const PAGE_SIZES = [10, 20, 50, 100]

/**
 * 数据库类型选项
 * driverReady: true=驱动已内置, false=需要手动配置驱动
 */
export const DB_TYPE_OPTIONS = [
  { label: 'MySQL', value: 'mysql', defaultPort: 3306, driverReady: true },
  { label: 'MariaDB', value: 'mariadb', defaultPort: 3306, driverReady: true },
  { label: 'PostgreSQL', value: 'postgresql', defaultPort: 5432, driverReady: true },
  { label: 'Oracle', value: 'oracle', defaultPort: 1521, driverReady: true },
  { label: 'SQL Server', value: 'sqlserver', defaultPort: 1433, driverReady: true },
  { label: 'SQLite', value: 'sqlite', defaultPort: 0, driverReady: true },
  { label: '达梦 DM8', value: 'dm', defaultPort: 5236, driverReady: false },
  { label: '人大金仓 KingbaseES', value: 'kingbase', defaultPort: 5432, driverReady: false },
  { label: '南大通用 GBase', value: 'gbase', defaultPort: 5258, driverReady: false },
  { label: 'TiDB', value: 'tidb', defaultPort: 4000, driverReady: true },
  { label: 'OceanBase', value: 'oceanbase', defaultPort: 2881, driverReady: true },
  { label: 'ClickHouse', value: 'clickhouse', defaultPort: 8123, driverReady: true },
  { label: 'Presto/Trino (支持Hive)', value: 'presto', defaultPort: 8080, driverReady: true }
] as const

/**
 * 获取数据库默认端口
 */
export const getDbDefaultPort = (dbType: string): number => {
  const option = DB_TYPE_OPTIONS.find(opt => opt.value === dbType.toLowerCase())
  return option?.defaultPort || 3306
}

/**
 * 检查数据库驱动是否就绪
 */
export const isDbDriverReady = (dbType: string): boolean => {
  const option = DB_TYPE_OPTIONS.find(opt => opt.value === dbType.toLowerCase())
  return option?.driverReady ?? false
}

/**
 * 用户状态选项
 */
export const USER_STATUS_OPTIONS = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
] as const

/**
 * 任务状态配置
 */
export const TASK_STATUS_MAP = {
  running: { type: 'success' as const, text: '运行中' },
  stopped: { type: 'default' as const, text: '已停止' },
  error: { type: 'error' as const, text: '错误' }
} as const


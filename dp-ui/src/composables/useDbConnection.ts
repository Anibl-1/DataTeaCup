/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 数据库连接状态管理组合式函数
 * 封装连接、断开、登录验证、数据库对象加载等逻辑
 *
 * Requirements: 8.2
 */
import { ref, computed, watch } from 'vue'
import { useMessage } from 'naive-ui'
import * as dbManagerApi from '@/api/dbManager'

/** 连接表单数据 */
export interface ConnectFormData {
  dbType: string
  host: string
  port: string
  dbName: string
  username: string
  password: string
}

/** 数据库类型选项 */
export const DB_TYPE_OPTIONS = [
  { label: 'MySQL', value: 'MYSQL' },
  { label: 'MariaDB', value: 'MARIADB' },
  { label: 'PostgreSQL', value: 'POSTGRESQL' },
  { label: 'Oracle', value: 'ORACLE' },
  { label: 'SQL Server', value: 'SQLSERVER' },
  { label: 'SQLite', value: 'SQLITE' },
  { label: '达梦 DM8', value: 'DM' },
  { label: '人大金仓 KingbaseES', value: 'KINGBASE' },
  { label: '南大通用 GBase', value: 'GBASE' },
  { label: 'TiDB', value: 'TIDB' },
  { label: 'OceanBase', value: 'OCEANBASE' },
  { label: 'ClickHouse', value: 'CLICKHOUSE' },
  { label: 'Presto/Trino (支持Hive)', value: 'PRESTO' },
]

/** 数据库类型 → 默认端口映射 */
const PORT_MAP: Record<string, string> = {
  MYSQL: '3306',
  MARIADB: '3306',
  ORACLE: '1521',
  SQLSERVER: '1433',
  POSTGRESQL: '5432',
  SQLITE: '',
  DM: '5236',
  KINGBASE: '5432',
  GBASE: '5258',
  TIDB: '4000',
  OCEANBASE: '2881',
  CLICKHOUSE: '8123',
  PRESTO: '8080',
}

export function useDbConnection() {
  const message = useMessage()

  // ---- 登录状态 ----
  const isAuthenticated = ref(false)
  const accessPassword = ref('')
  const loginLoading = ref(false)

  // ---- 连接状态 ----
  const sessionId = ref<string | null>(null)
  const connecting = ref(false)
  const isSystemDb = ref(false)
  const showManualConnect = ref(false)

  const connectForm = ref<ConnectFormData>({
    dbType: 'MYSQL',
    host: 'localhost',
    port: '3306',
    dbName: '',
    username: '',
    password: '',
  })

  // ---- 数据库对象 ----
  const tables = ref<any[]>([])
  const views = ref<any[]>([])
  const procedures = ref<any[]>([])
  const loadingObjects = ref(false)
  const tableColumnsCache = ref<Record<string, Array<{ name: string; type: string; comment?: string }>>>({})

  const isConnected = computed(() => !!sessionId.value)

  // ---- 登录 ----
  const handleLogin = async () => {
    if (!accessPassword.value) {
      message.warning('请输入访问密码')
      return
    }
    loginLoading.value = true
    try {
      const res = await dbManagerApi.verifyPassword(accessPassword.value)
      if (res.data) {
        isAuthenticated.value = true
        message.success('验证成功，欢迎使用！')
      } else {
        message.error('密码错误')
        accessPassword.value = ''
      }
    } catch (error: any) {
      message.error(error.message || '验证失败')
    } finally {
      loginLoading.value = false
    }
  }

  const handleLogout = () => {
    if (sessionId.value) handleDisconnect()
    isAuthenticated.value = false
    accessPassword.value = ''
  }

  // ---- 连接 ----
  const connectSystemDb = async () => {
    connecting.value = true
    try {
      const res = await dbManagerApi.connectSystemDb()
      sessionId.value = res.data
      isSystemDb.value = true
      message.success('🎉 系统库连接成功！')
      await loadDatabaseObjects()
    } catch (error: any) {
      message.error(error.message || '连接失败')
    } finally {
      connecting.value = false
    }
  }

  const handleConnect = async () => {
    const conn = connectForm.value
    if (!conn.dbType) { message.warning('请选择数据库类型'); return }
    if (!conn.host) { message.warning('请输入主机地址'); return }
    if (!conn.dbName) { message.warning('请输入数据库名'); return }
    if (!conn.username) { message.warning('请输入用户名'); return }

    connecting.value = true
    try {
      const res = await dbManagerApi.createConnection(conn)
      sessionId.value = res.data
      isSystemDb.value = false
      message.success('🎉 连接成功')
      await loadDatabaseObjects()
    } catch (error: any) {
      message.error(error.message || '连接失败')
    } finally {
      connecting.value = false
    }
  }

  const handleDisconnect = async () => {
    if (sessionId.value) {
      try { await dbManagerApi.closeConnection(sessionId.value) } catch { /* ignore */ }
    }
    sessionId.value = null
    isSystemDb.value = false
    tables.value = []
    views.value = []
    procedures.value = []
    tableColumnsCache.value = {}
    message.success('已断开连接')
  }

  // ---- 数据库对象加载 ----
  const getTableColumns = async (tableName: string): Promise<Array<{ name: string; type: string; comment?: string }>> => {
    if (tableColumnsCache.value[tableName]) {
      return tableColumnsCache.value[tableName]
    }
    if (!sessionId.value) return []
    try {
      const res = await dbManagerApi.getTableStructure(sessionId.value, tableName, true)
      const columns = (res.data || []).map((col: any) => ({
        name: col.columnName,
        type: col.dataType,
        comment: col.remarks,
      }))
      tableColumnsCache.value[tableName] = columns
      return columns
    } catch {
      return []
    }
  }

  const preloadTableColumns = async () => {
    if (!sessionId.value) return
    const sid = sessionId.value
    const batch = 5
    for (let i = 0; i < tables.value.length; i += batch) {
      if (!sessionId.value || sessionId.value !== sid) break
      const chunk = tables.value.slice(i, i + batch)
      const results = await Promise.all(
        chunk.map((t) => {
          if (tableColumnsCache.value[t.tableName]) return Promise.resolve(true)
          return getTableColumns(t.tableName).then(cols => cols.length > 0).catch(() => false)
        }),
      )
      // If all requests in the batch failed, connection is likely lost — stop
      if (results.every(r => !r)) break
    }
  }

  const loadDatabaseObjects = async () => {
    if (!sessionId.value) return
    loadingObjects.value = true
    try {
      const [tablesRes, viewsRes, proceduresRes] = await Promise.all([
        dbManagerApi.getTables(sessionId.value),
        dbManagerApi.getViews(sessionId.value),
        dbManagerApi.getProcedures(sessionId.value),
      ])
      tables.value = tablesRes.data || []
      views.value = viewsRes.data || []
      procedures.value = proceduresRes.data || []
      preloadTableColumns()
    } catch {
      message.error('加载数据库对象失败')
    } finally {
      loadingObjects.value = false
    }
  }

  const refreshObjects = () => loadDatabaseObjects()

  // ---- 监听数据库类型变化自动设置端口 ----
  watch(() => connectForm.value.dbType, (val) => {
    connectForm.value.port = PORT_MAP[val] || '3306'
  })

  return {
    // 登录
    isAuthenticated,
    accessPassword,
    loginLoading,
    handleLogin,
    handleLogout,
    // 连接
    sessionId,
    connecting,
    isSystemDb,
    isConnected,
    showManualConnect,
    connectForm,
    connectSystemDb,
    handleConnect,
    handleDisconnect,
    // 数据库对象
    tables,
    views,
    procedures,
    loadingObjects,
    tableColumnsCache,
    loadDatabaseObjects,
    refreshObjects,
    getTableColumns,
  }
}

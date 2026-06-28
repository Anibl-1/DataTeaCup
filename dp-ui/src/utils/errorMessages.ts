/**
 * 错误消息映射
 * 
 * 为常见错误提供用户友好的消息和解决建议。
 * 
 * 需求 18.2: THE Error_Handler SHALL 为常见错误提供解决方案建议
 * 
 * @module errorMessages
 */

// ==================== 类型定义 ====================

/**
 * 错误消息配置接口
 */
export interface ErrorMessageConfig {
  /** 用户友好的错误消息 */
  message: string
  /** 解决建议列表 */
  suggestions: string[]
  /** 错误分类 */
  category?: ErrorCategory
  /** 是否可重试 */
  retryable?: boolean
  /** 重试延迟（毫秒） */
  retryDelay?: number
}

/**
 * 错误分类枚举
 */
export enum ErrorCategory {
  /** 网络相关 */
  NETWORK = 'network',
  /** 认证相关 */
  AUTH = 'auth',
  /** 权限相关 */
  PERMISSION = 'permission',
  /** 数据源相关 */
  DATA_SOURCE = 'data_source',
  /** SQL相关 */
  SQL = 'sql',
  /** 报表相关 */
  REPORT = 'report',
  /** 图表相关 */
  CHART = 'chart',
  /** 导出相关 */
  EXPORT = 'export',
  /** 缓存相关 */
  CACHE = 'cache',
  /** 脱敏相关 */
  MASKING = 'masking',
  /** 服务器相关 */
  SERVER = 'server',
  /** 验证相关 */
  VALIDATION = 'validation',
  /** 资源相关 */
  RESOURCE = 'resource',
  /** 配置相关 */
  CONFIG = 'config'
}


// ==================== 错误消息映射 ====================

/**
 * 错误消息映射表
 * 
 * 包含所有常见错误的用户友好消息和解决建议。
 * 每个错误码至少提供一条解决建议。
 * 
 * **Validates: Requirements 18.2**
 */
export const errorMessages: Record<string, ErrorMessageConfig> = {
  // ==================== 网络相关错误 ====================
  
  'NETWORK_ERROR': {
    message: '网络连接失败',
    suggestions: [
      '请检查您的网络连接是否正常',
      '尝试刷新页面重试',
      '检查是否使用了代理或VPN',
      '如果问题持续，请联系技术支持'
    ],
    category: ErrorCategory.NETWORK,
    retryable: true,
    retryDelay: 3000
  },
  
  'ERR_NETWORK': {
    message: '网络请求失败',
    suggestions: [
      '请检查网络连接是否正常',
      '确认服务器地址是否正确',
      '尝试刷新页面后重试'
    ],
    category: ErrorCategory.NETWORK,
    retryable: true,
    retryDelay: 3000
  },
  
  'ECONNABORTED': {
    message: '请求超时',
    suggestions: [
      '网络可能较慢，请稍后重试',
      '检查网络连接是否稳定',
      '如果数据量较大，请耐心等待',
      '尝试减少查询数据量'
    ],
    category: ErrorCategory.NETWORK,
    retryable: true,
    retryDelay: 5000
  },
  
  'ETIMEDOUT': {
    message: '连接超时',
    suggestions: [
      '服务器响应较慢，请稍后重试',
      '检查网络连接是否正常',
      '如果问题持续，请联系技术支持'
    ],
    category: ErrorCategory.NETWORK,
    retryable: true,
    retryDelay: 5000
  },
  
  'ECONNREFUSED': {
    message: '连接被拒绝',
    suggestions: [
      '服务器可能暂时不可用',
      '请稍后重试',
      '如果问题持续，请联系技术支持'
    ],
    category: ErrorCategory.NETWORK,
    retryable: true,
    retryDelay: 10000
  },

  // ==================== 认证相关错误 ====================
  
  'AUTH_FAILED': {
    message: '认证失败',
    suggestions: [
      '请检查用户名和密码是否正确',
      '确认账号是否已激活',
      '如果忘记密码，请使用找回密码功能'
    ],
    category: ErrorCategory.AUTH,
    retryable: false
  },
  
  'TOKEN_EXPIRED': {
    message: '登录已过期',
    suggestions: [
      '请重新登录以继续操作',
      '为了账户安全，系统会定期要求重新登录',
      '建议保存当前工作后重新登录'
    ],
    category: ErrorCategory.AUTH,
    retryable: false
  },
  
  'TOKEN_INVALID': {
    message: '登录状态无效',
    suggestions: [
      '请重新登录',
      '如果问题持续，请清除浏览器缓存后重试',
      '检查是否在其他设备上登录导致当前会话失效'
    ],
    category: ErrorCategory.AUTH,
    retryable: false
  },
  
  'SESSION_EXPIRED': {
    message: '会话已过期',
    suggestions: [
      '您的会话已超时，请重新登录',
      '建议保存当前工作后重新登录'
    ],
    category: ErrorCategory.AUTH,
    retryable: false
  },
  
  'INVALID_CREDENTIALS': {
    message: '用户名或密码错误',
    suggestions: [
      '请检查用户名和密码是否正确',
      '注意区分大小写',
      '如果忘记密码，请使用找回密码功能'
    ],
    category: ErrorCategory.AUTH,
    retryable: false
  },
  
  'ACCOUNT_LOCKED': {
    message: '账号已被锁定',
    suggestions: [
      '由于多次登录失败，账号已被临时锁定',
      '请等待30分钟后重试',
      '或联系管理员解锁账号'
    ],
    category: ErrorCategory.AUTH,
    retryable: false
  },
  
  'ACCOUNT_DISABLED': {
    message: '账号已被禁用',
    suggestions: [
      '您的账号已被管理员禁用',
      '请联系管理员了解详情'
    ],
    category: ErrorCategory.AUTH,
    retryable: false
  },

  // ==================== 权限相关错误 ====================
  
  'PERMISSION_DENIED': {
    message: '没有操作权限',
    suggestions: [
      '您没有执行此操作的权限',
      '请联系管理员获取相应权限',
      '确认您的角色是否具有此功能的访问权限'
    ],
    category: ErrorCategory.PERMISSION,
    retryable: false
  },
  
  'ACCESS_DENIED': {
    message: '访问被拒绝',
    suggestions: [
      '您没有访问此资源的权限',
      '请联系管理员确认您的权限设置',
      '检查是否需要申请额外的访问权限'
    ],
    category: ErrorCategory.PERMISSION,
    retryable: false
  },
  
  'INSUFFICIENT_PRIVILEGES': {
    message: '权限不足',
    suggestions: [
      '您的权限级别不足以执行此操作',
      '请联系管理员提升权限',
      '或使用具有更高权限的账号'
    ],
    category: ErrorCategory.PERMISSION,
    retryable: false
  },
  
  'ROLE_NOT_ALLOWED': {
    message: '角色不允许此操作',
    suggestions: [
      '您当前的角色不允许执行此操作',
      '请联系管理员调整角色权限'
    ],
    category: ErrorCategory.PERMISSION,
    retryable: false
  },
  
  // ==================== 数据源相关错误 ====================
  
  'DATA_SOURCE_CONNECTION_FAILED': {
    message: '数据源连接失败',
    suggestions: [
      '请检查数据源配置是否正确',
      '确认数据库服务是否正常运行',
      '检查网络连接是否正常',
      '验证数据库用户名和密码是否正确',
      '检查数据库端口是否开放'
    ],
    category: ErrorCategory.DATA_SOURCE,
    retryable: true,
    retryDelay: 5000
  },
  
  'DATA_SOURCE_NOT_FOUND': {
    message: '数据源不存在',
    suggestions: [
      '请检查数据源是否已被删除',
      '确认数据源ID是否正确',
      '尝试重新选择数据源',
      '联系管理员确认数据源状态'
    ],
    category: ErrorCategory.DATA_SOURCE,
    retryable: false
  },
  
  'DATA_SOURCE_TIMEOUT': {
    message: '数据源查询超时',
    suggestions: [
      '查询数据量可能较大，请尝试添加筛选条件',
      '优化SQL查询语句，减少返回数据量',
      '联系管理员检查数据库性能',
      '考虑使用分页查询'
    ],
    category: ErrorCategory.DATA_SOURCE,
    retryable: true,
    retryDelay: 10000
  },
  
  'DATA_SOURCE_AUTH_FAILED': {
    message: '数据源认证失败',
    suggestions: [
      '请检查数据源的用户名和密码是否正确',
      '确认数据库用户是否有访问权限',
      '联系管理员重置数据源凭据'
    ],
    category: ErrorCategory.DATA_SOURCE,
    retryable: false
  },
  
  'DATA_SOURCE_POOL_EXHAUSTED': {
    message: '数据源连接池已满',
    suggestions: [
      '系统当前负载较高，请稍后重试',
      '减少并发查询数量',
      '联系管理员扩展连接池配置'
    ],
    category: ErrorCategory.DATA_SOURCE,
    retryable: true,
    retryDelay: 5000
  },

  // ==================== SQL相关错误 ====================
  
  'SQL_SYNTAX_ERROR': {
    message: 'SQL语法错误',
    suggestions: [
      '请检查SQL语句的语法是否正确',
      '确认表名和字段名是否存在',
      '检查SQL关键字是否拼写正确',
      '使用SQL编辑器的语法检查功能'
    ],
    category: ErrorCategory.SQL,
    retryable: false
  },
  
  'SQL_EXECUTION_ERROR': {
    message: 'SQL执行失败',
    suggestions: [
      '请检查SQL语句是否正确',
      '确认引用的表和字段是否存在',
      '检查数据类型是否匹配',
      '查看详细错误信息了解具体原因'
    ],
    category: ErrorCategory.SQL,
    retryable: false
  },
  
  'SQL_INJECTION_DETECTED': {
    message: '检测到潜在的SQL注入',
    suggestions: [
      '请使用参数化查询',
      '避免在SQL中直接拼接用户输入',
      '检查输入数据是否包含特殊字符'
    ],
    category: ErrorCategory.SQL,
    retryable: false
  },
  
  'TABLE_NOT_FOUND': {
    message: '表不存在',
    suggestions: [
      '请检查表名是否正确',
      '确认表是否已创建',
      '检查是否有访问该表的权限',
      '确认数据库schema是否正确'
    ],
    category: ErrorCategory.SQL,
    retryable: false
  },
  
  'COLUMN_NOT_FOUND': {
    message: '字段不存在',
    suggestions: [
      '请检查字段名是否正确',
      '确认字段是否存在于指定的表中',
      '检查字段名的大小写是否正确'
    ],
    category: ErrorCategory.SQL,
    retryable: false
  },
  
  'QUERY_TOO_COMPLEX': {
    message: '查询过于复杂',
    suggestions: [
      '请简化查询语句',
      '减少JOIN的表数量',
      '考虑将复杂查询拆分为多个简单查询',
      '使用临时表或视图优化查询'
    ],
    category: ErrorCategory.SQL,
    retryable: false
  },
  
  'QUERY_RESULT_TOO_LARGE': {
    message: '查询结果数据量过大',
    suggestions: [
      '请添加WHERE条件限制数据范围',
      '使用LIMIT限制返回行数',
      '考虑使用分页查询',
      '导出大数据量请使用异步导出功能'
    ],
    category: ErrorCategory.SQL,
    retryable: false
  },

  // ==================== 报表相关错误 ====================
  
  'REPORT_NOT_FOUND': {
    message: '报表不存在',
    suggestions: [
      '报表可能已被删除',
      '请检查报表ID是否正确',
      '返回报表列表重新选择',
      '联系报表创建者确认报表状态'
    ],
    category: ErrorCategory.REPORT,
    retryable: false
  },
  
  'REPORT_SAVE_FAILED': {
    message: '报表保存失败',
    suggestions: [
      '请检查报表配置是否完整',
      '确认您有保存报表的权限',
      '检查报表名称是否重复',
      '稍后重试'
    ],
    category: ErrorCategory.REPORT,
    retryable: true,
    retryDelay: 3000
  },
  
  'REPORT_LOAD_FAILED': {
    message: '报表加载失败',
    suggestions: [
      '请刷新页面重试',
      '检查网络连接是否正常',
      '报表配置可能已损坏，请联系管理员'
    ],
    category: ErrorCategory.REPORT,
    retryable: true,
    retryDelay: 3000
  },
  
  'REPORT_CONFIG_INVALID': {
    message: '报表配置无效',
    suggestions: [
      '请检查报表配置是否完整',
      '确认数据源和SQL配置是否正确',
      '检查字段映射是否正确'
    ],
    category: ErrorCategory.REPORT,
    retryable: false
  },
  
  'REPORT_TEMPLATE_NOT_FOUND': {
    message: '报表模板不存在',
    suggestions: [
      '请检查模板是否已被删除',
      '尝试选择其他模板',
      '联系管理员恢复模板'
    ],
    category: ErrorCategory.REPORT,
    retryable: false
  },
  
  'REPORT_PARAMETER_MISSING': {
    message: '报表参数缺失',
    suggestions: [
      '请填写所有必填参数',
      '检查参数值是否有效',
      '确认参数格式是否正确'
    ],
    category: ErrorCategory.REPORT,
    retryable: false
  },
  
  'REPORT_PARAMETER_INVALID': {
    message: '报表参数无效',
    suggestions: [
      '请检查参数值是否符合要求',
      '确认日期格式是否正确',
      '检查数值是否在有效范围内'
    ],
    category: ErrorCategory.REPORT,
    retryable: false
  },

  // ==================== 图表相关错误 ====================
  
  'CHART_RENDER_ERROR': {
    message: '图表渲染失败',
    suggestions: [
      '请检查图表配置是否正确',
      '确认数据格式是否符合要求',
      '尝试刷新页面',
      '检查浏览器是否支持图表功能'
    ],
    category: ErrorCategory.CHART,
    retryable: true,
    retryDelay: 2000
  },
  
  'CHART_DATA_ERROR': {
    message: '图表数据错误',
    suggestions: [
      '请检查数据源配置',
      '确认SQL查询返回的数据格式正确',
      '检查字段映射是否正确',
      '确认数据类型是否匹配图表要求'
    ],
    category: ErrorCategory.CHART,
    retryable: false
  },
  
  'CHART_CONFIG_INVALID': {
    message: '图表配置无效',
    suggestions: [
      '请检查图表类型是否正确',
      '确认必要的配置项已填写',
      '检查坐标轴配置是否正确'
    ],
    category: ErrorCategory.CHART,
    retryable: false
  },
  
  'CHART_TYPE_NOT_SUPPORTED': {
    message: '不支持的图表类型',
    suggestions: [
      '请选择系统支持的图表类型',
      '查看帮助文档了解支持的图表类型'
    ],
    category: ErrorCategory.CHART,
    retryable: false
  },
  
  'CHART_DATA_EMPTY': {
    message: '图表数据为空',
    suggestions: [
      '请检查数据源是否有数据',
      '确认查询条件是否正确',
      '检查筛选条件是否过于严格'
    ],
    category: ErrorCategory.CHART,
    retryable: false
  },

  // ==================== 导出相关错误 ====================
  
  'EXPORT_FAILED': {
    message: '导出失败',
    suggestions: [
      '请稍后重试',
      '如果数据量较大，请尝试分批导出',
      '检查磁盘空间是否充足',
      '联系管理员检查导出服务状态'
    ],
    category: ErrorCategory.EXPORT,
    retryable: true,
    retryDelay: 5000
  },
  
  'EXPORT_SIZE_EXCEEDED': {
    message: '导出数据量超出限制',
    suggestions: [
      '请添加筛选条件减少数据量',
      '尝试分批导出数据',
      '联系管理员调整导出限制',
      '使用异步导出功能处理大数据量'
    ],
    category: ErrorCategory.EXPORT,
    retryable: false
  },
  
  'EXPORT_FORMAT_NOT_SUPPORTED': {
    message: '不支持的导出格式',
    suggestions: [
      '请选择支持的导出格式（Excel、CSV、PDF）',
      '查看帮助文档了解支持的导出格式'
    ],
    category: ErrorCategory.EXPORT,
    retryable: false
  },
  
  'EXPORT_TASK_NOT_FOUND': {
    message: '导出任务不存在',
    suggestions: [
      '导出任务可能已过期或被删除',
      '请重新创建导出任务'
    ],
    category: ErrorCategory.EXPORT,
    retryable: false
  },
  
  'EXPORT_TASK_CANCELLED': {
    message: '导出任务已取消',
    suggestions: [
      '导出任务已被取消',
      '如需导出，请重新创建任务'
    ],
    category: ErrorCategory.EXPORT,
    retryable: false
  },
  
  'EXPORT_DISK_FULL': {
    message: '磁盘空间不足',
    suggestions: [
      '服务器磁盘空间不足',
      '请联系管理员清理磁盘空间',
      '稍后重试'
    ],
    category: ErrorCategory.EXPORT,
    retryable: true,
    retryDelay: 60000
  },

  // ==================== 缓存相关错误 ====================
  
  'CACHE_ERROR': {
    message: '缓存服务异常',
    suggestions: [
      '系统正在使用备用方案，功能不受影响',
      '如果响应较慢，请稍后重试',
      '联系管理员检查缓存服务状态'
    ],
    category: ErrorCategory.CACHE,
    retryable: true,
    retryDelay: 5000
  },
  
  'CACHE_CONNECTION_FAILED': {
    message: '缓存服务连接失败',
    suggestions: [
      '缓存服务暂时不可用',
      '系统将直接查询数据库',
      '如果响应较慢，请稍后重试'
    ],
    category: ErrorCategory.CACHE,
    retryable: true,
    retryDelay: 10000
  },
  
  'CACHE_INVALIDATION_FAILED': {
    message: '缓存清除失败',
    suggestions: [
      '缓存清除操作失败',
      '数据可能存在延迟，请稍后刷新',
      '联系管理员手动清除缓存'
    ],
    category: ErrorCategory.CACHE,
    retryable: true,
    retryDelay: 5000
  },
  
  // ==================== 脱敏相关错误 ====================
  
  'MASKING_CONFIG_ERROR': {
    message: '数据脱敏配置错误',
    suggestions: [
      '请检查脱敏规则配置是否正确',
      '确认脱敏策略参数是否有效',
      '联系管理员检查脱敏配置',
      '检查正则表达式是否正确'
    ],
    category: ErrorCategory.MASKING,
    retryable: false
  },
  
  'MASKING_RULE_NOT_FOUND': {
    message: '脱敏规则不存在',
    suggestions: [
      '请检查脱敏规则是否已被删除',
      '重新配置脱敏规则',
      '联系管理员恢复规则'
    ],
    category: ErrorCategory.MASKING,
    retryable: false
  },
  
  'MASKING_EXECUTION_ERROR': {
    message: '脱敏执行失败',
    suggestions: [
      '脱敏处理过程中发生错误',
      '请检查脱敏规则配置',
      '联系管理员查看详细日志'
    ],
    category: ErrorCategory.MASKING,
    retryable: true,
    retryDelay: 3000
  },

  // ==================== 服务器相关错误 ====================
  
  'INTERNAL_SERVER_ERROR': {
    message: '服务器内部错误',
    suggestions: [
      '服务器遇到了问题，请稍后重试',
      '如果问题持续，请联系技术支持',
      '记录错误追踪ID以便排查问题'
    ],
    category: ErrorCategory.SERVER,
    retryable: true,
    retryDelay: 10000
  },
  
  'SERVICE_UNAVAILABLE': {
    message: '服务暂时不可用',
    suggestions: [
      '服务器正在维护中，请稍后重试',
      '如果问题持续，请联系技术支持',
      '查看系统公告了解维护计划'
    ],
    category: ErrorCategory.SERVER,
    retryable: true,
    retryDelay: 30000
  },
  
  'BAD_GATEWAY': {
    message: '网关错误',
    suggestions: [
      '服务器暂时无法处理请求',
      '请稍后重试',
      '如果问题持续，请联系技术支持'
    ],
    category: ErrorCategory.SERVER,
    retryable: true,
    retryDelay: 10000
  },
  
  'GATEWAY_TIMEOUT': {
    message: '网关超时',
    suggestions: [
      '服务器响应超时',
      '请稍后重试',
      '如果查询数据量较大，请添加筛选条件',
      '联系技术支持检查服务器状态'
    ],
    category: ErrorCategory.SERVER,
    retryable: true,
    retryDelay: 15000
  },
  
  'SERVER_OVERLOADED': {
    message: '服务器负载过高',
    suggestions: [
      '服务器当前负载较高',
      '请稍后重试',
      '避免同时执行多个大查询'
    ],
    category: ErrorCategory.SERVER,
    retryable: true,
    retryDelay: 30000
  },
  
  // ==================== 验证相关错误 ====================
  
  'VALIDATION_ERROR': {
    message: '数据验证失败',
    suggestions: [
      '请检查输入的数据是否正确',
      '确认必填项是否已填写',
      '检查数据格式是否符合要求'
    ],
    category: ErrorCategory.VALIDATION,
    retryable: false
  },
  
  'INVALID_PARAMETER': {
    message: '参数无效',
    suggestions: [
      '请检查参数值是否正确',
      '确认参数格式是否符合要求',
      '查看帮助文档了解参数要求'
    ],
    category: ErrorCategory.VALIDATION,
    retryable: false
  },
  
  'MISSING_REQUIRED_FIELD': {
    message: '缺少必填字段',
    suggestions: [
      '请填写所有必填字段',
      '检查表单是否完整'
    ],
    category: ErrorCategory.VALIDATION,
    retryable: false
  },
  
  'INVALID_DATE_FORMAT': {
    message: '日期格式无效',
    suggestions: [
      '请使用正确的日期格式',
      '推荐格式：YYYY-MM-DD',
      '检查日期是否在有效范围内'
    ],
    category: ErrorCategory.VALIDATION,
    retryable: false
  },
  
  'INVALID_NUMBER_FORMAT': {
    message: '数字格式无效',
    suggestions: [
      '请输入有效的数字',
      '检查是否包含非数字字符',
      '确认数值是否在有效范围内'
    ],
    category: ErrorCategory.VALIDATION,
    retryable: false
  },

  // ==================== 资源相关错误 ====================
  
  'RESOURCE_NOT_FOUND': {
    message: '资源不存在',
    suggestions: [
      '请检查访问的地址是否正确',
      '该资源可能已被删除或移动',
      '返回上一页重新操作'
    ],
    category: ErrorCategory.RESOURCE,
    retryable: false
  },
  
  'RESOURCE_LOCKED': {
    message: '资源已被锁定',
    suggestions: [
      '该资源正在被其他用户编辑',
      '请稍后重试',
      '联系资源所有者解锁'
    ],
    category: ErrorCategory.RESOURCE,
    retryable: true,
    retryDelay: 10000
  },
  
  'RESOURCE_CONFLICT': {
    message: '资源冲突',
    suggestions: [
      '该资源已被其他用户修改',
      '请刷新页面获取最新数据',
      '重新进行操作'
    ],
    category: ErrorCategory.RESOURCE,
    retryable: false
  },
  
  'DUPLICATE_RESOURCE': {
    message: '资源已存在',
    suggestions: [
      '同名资源已存在',
      '请使用不同的名称',
      '或删除现有资源后重试'
    ],
    category: ErrorCategory.RESOURCE,
    retryable: false
  },
  
  // ==================== 配置相关错误 ====================
  
  'CONFIG_ERROR': {
    message: '配置错误',
    suggestions: [
      '系统配置存在问题',
      '请联系管理员检查配置',
      '查看系统日志了解详情'
    ],
    category: ErrorCategory.CONFIG,
    retryable: false
  },
  
  'CONFIG_NOT_FOUND': {
    message: '配置不存在',
    suggestions: [
      '所需的配置项不存在',
      '请联系管理员添加配置',
      '检查配置文件是否完整'
    ],
    category: ErrorCategory.CONFIG,
    retryable: false
  },
  
  'CONFIG_INVALID': {
    message: '配置无效',
    suggestions: [
      '配置值无效或格式错误',
      '请联系管理员修正配置',
      '检查配置是否符合要求'
    ],
    category: ErrorCategory.CONFIG,
    retryable: false
  },
  
  // ==================== 请求频率限制 ====================
  
  'RATE_LIMIT_EXCEEDED': {
    message: '请求过于频繁',
    suggestions: [
      '请稍等片刻后再试',
      '避免短时间内重复操作',
      '如需大量操作，请使用批量功能'
    ],
    category: ErrorCategory.NETWORK,
    retryable: true,
    retryDelay: 60000
  },
  
  'TOO_MANY_REQUESTS': {
    message: '请求次数过多',
    suggestions: [
      '您的请求次数已达到限制',
      '请等待一段时间后重试',
      '联系管理员提升请求配额'
    ],
    category: ErrorCategory.NETWORK,
    retryable: true,
    retryDelay: 60000
  },

  // ==================== 文件相关错误 ====================
  
  'FILE_NOT_FOUND': {
    message: '文件不存在',
    suggestions: [
      '请检查文件路径是否正确',
      '文件可能已被删除或移动',
      '重新上传文件'
    ],
    category: ErrorCategory.RESOURCE,
    retryable: false
  },
  
  'FILE_TOO_LARGE': {
    message: '文件过大',
    suggestions: [
      '文件大小超出限制',
      '请压缩文件后重试',
      '或分割为多个小文件上传'
    ],
    category: ErrorCategory.VALIDATION,
    retryable: false
  },
  
  'FILE_TYPE_NOT_ALLOWED': {
    message: '文件类型不允许',
    suggestions: [
      '请上传允许的文件类型',
      '查看帮助文档了解支持的文件格式',
      '转换文件格式后重试'
    ],
    category: ErrorCategory.VALIDATION,
    retryable: false
  },
  
  'FILE_UPLOAD_FAILED': {
    message: '文件上传失败',
    suggestions: [
      '请检查网络连接',
      '确认文件大小是否在限制范围内',
      '稍后重试'
    ],
    category: ErrorCategory.RESOURCE,
    retryable: true,
    retryDelay: 5000
  },
  
  // ==================== 通用错误 ====================
  
  'UNKNOWN_ERROR': {
    message: '未知错误',
    suggestions: [
      '发生了未知错误',
      '请稍后重试',
      '如果问题持续，请联系技术支持',
      '记录错误追踪ID以便排查问题'
    ],
    category: ErrorCategory.SERVER,
    retryable: true,
    retryDelay: 5000
  },
  
  'OPERATION_FAILED': {
    message: '操作失败',
    suggestions: [
      '操作未能成功完成',
      '请稍后重试',
      '检查输入数据是否正确'
    ],
    category: ErrorCategory.SERVER,
    retryable: true,
    retryDelay: 3000
  },
  
  'OPERATION_TIMEOUT': {
    message: '操作超时',
    suggestions: [
      '操作执行时间过长',
      '请稍后重试',
      '如果数据量较大，请尝试分批处理'
    ],
    category: ErrorCategory.SERVER,
    retryable: true,
    retryDelay: 10000
  },
  
  'OPERATION_CANCELLED': {
    message: '操作已取消',
    suggestions: [
      '操作已被取消',
      '如需继续，请重新执行操作'
    ],
    category: ErrorCategory.SERVER,
    retryable: false
  }
}


// ==================== HTTP状态码映射 ====================

/**
 * HTTP状态码到错误消息的映射
 */
export const httpStatusMessages: Record<number, ErrorMessageConfig> = {
  400: {
    message: '请求参数错误',
    suggestions: [
      '请检查输入的数据是否正确',
      '确认必填项是否已填写',
      '检查数据格式是否符合要求'
    ],
    category: ErrorCategory.VALIDATION,
    retryable: false
  },
  401: {
    message: '未授权，请重新登录',
    suggestions: [
      '您的登录状态已失效',
      '请重新登录后继续操作'
    ],
    category: ErrorCategory.AUTH,
    retryable: false
  },
  403: {
    message: '没有访问权限',
    suggestions: [
      '您没有执行此操作的权限',
      '请联系管理员获取相应权限'
    ],
    category: ErrorCategory.PERMISSION,
    retryable: false
  },
  404: {
    message: '请求的资源不存在',
    suggestions: [
      '请检查访问的地址是否正确',
      '该资源可能已被删除或移动'
    ],
    category: ErrorCategory.RESOURCE,
    retryable: false
  },
  405: {
    message: '请求方法不允许',
    suggestions: [
      '请求方法不被支持',
      '请检查API调用是否正确'
    ],
    category: ErrorCategory.VALIDATION,
    retryable: false
  },
  408: {
    message: '请求超时',
    suggestions: [
      '网络可能较慢，请稍后重试',
      '检查网络连接是否稳定'
    ],
    category: ErrorCategory.NETWORK,
    retryable: true,
    retryDelay: 5000
  },
  409: {
    message: '资源冲突',
    suggestions: [
      '该资源已被其他用户修改',
      '请刷新页面获取最新数据'
    ],
    category: ErrorCategory.RESOURCE,
    retryable: false
  },
  413: {
    message: '请求数据过大',
    suggestions: [
      '请求的数据量超出限制',
      '请减少数据量后重试'
    ],
    category: ErrorCategory.VALIDATION,
    retryable: false
  },
  422: {
    message: '数据验证失败',
    suggestions: [
      '请检查提交的数据是否正确',
      '确认数据格式是否符合要求'
    ],
    category: ErrorCategory.VALIDATION,
    retryable: false
  },
  429: {
    message: '请求过于频繁',
    suggestions: [
      '请稍等片刻后再试',
      '避免短时间内重复操作'
    ],
    category: ErrorCategory.NETWORK,
    retryable: true,
    retryDelay: 60000
  },
  500: {
    message: '服务器内部错误',
    suggestions: [
      '服务器遇到了问题，请稍后重试',
      '如果问题持续，请联系技术支持'
    ],
    category: ErrorCategory.SERVER,
    retryable: true,
    retryDelay: 10000
  },
  502: {
    message: '网关错误',
    suggestions: [
      '服务器暂时无法处理请求',
      '请稍后重试'
    ],
    category: ErrorCategory.SERVER,
    retryable: true,
    retryDelay: 10000
  },
  503: {
    message: '服务暂时不可用',
    suggestions: [
      '服务器正在维护中',
      '请稍后重试'
    ],
    category: ErrorCategory.SERVER,
    retryable: true,
    retryDelay: 30000
  },
  504: {
    message: '网关超时',
    suggestions: [
      '服务器响应超时',
      '请稍后重试',
      '如果问题持续，请联系技术支持'
    ],
    category: ErrorCategory.SERVER,
    retryable: true,
    retryDelay: 15000
  }
}


// ==================== 工具函数 ====================

/**
 * 获取错误消息配置
 * 
 * @param errorCode 错误码
 * @returns 错误消息配置，如果未找到则返回undefined
 */
export function getErrorMessage(errorCode: string): ErrorMessageConfig | undefined {
  return errorMessages[errorCode]
}

/**
 * 获取HTTP状态码对应的错误消息配置
 * 
 * @param statusCode HTTP状态码
 * @returns 错误消息配置，如果未找到则返回undefined
 */
export function getHttpStatusMessage(statusCode: number): ErrorMessageConfig | undefined {
  return httpStatusMessages[statusCode]
}

/**
 * 获取错误的解决建议
 * 
 * @param errorCode 错误码
 * @returns 解决建议列表，如果未找到则返回空数组
 */
export function getSuggestions(errorCode: string): string[] {
  const config = errorMessages[errorCode]
  return config?.suggestions || []
}

/**
 * 获取HTTP状态码对应的解决建议
 * 
 * @param statusCode HTTP状态码
 * @returns 解决建议列表，如果未找到则返回空数组
 */
export function getHttpStatusSuggestions(statusCode: number): string[] {
  const config = httpStatusMessages[statusCode]
  return config?.suggestions || []
}

/**
 * 检查错误是否可重试
 * 
 * @param errorCode 错误码
 * @returns 是否可重试
 */
export function isRetryable(errorCode: string): boolean {
  const config = errorMessages[errorCode]
  return config?.retryable ?? false
}

/**
 * 获取重试延迟时间
 * 
 * @param errorCode 错误码
 * @returns 重试延迟时间（毫秒），如果不可重试则返回0
 */
export function getRetryDelay(errorCode: string): number {
  const config = errorMessages[errorCode]
  return config?.retryable ? (config.retryDelay ?? 3000) : 0
}

/**
 * 获取错误分类
 * 
 * @param errorCode 错误码
 * @returns 错误分类，如果未找到则返回undefined
 */
export function getErrorCategory(errorCode: string): ErrorCategory | undefined {
  const config = errorMessages[errorCode]
  return config?.category
}

/**
 * 获取指定分类的所有错误码
 * 
 * @param category 错误分类
 * @returns 该分类下的所有错误码
 */
export function getErrorCodesByCategory(category: ErrorCategory): string[] {
  return Object.entries(errorMessages)
    .filter(([, config]) => config.category === category)
    .map(([code]) => code)
}

/**
 * 获取用户友好的错误消息
 * 
 * @param errorCode 错误码
 * @param fallbackMessage 默认消息（当错误码未找到时使用）
 * @returns 用户友好的错误消息
 */
export function getFriendlyMessage(errorCode: string, fallbackMessage?: string): string {
  const config = errorMessages[errorCode]
  return config?.message || fallbackMessage || '操作失败，请稍后重试'
}

/**
 * 获取完整的错误信息（包含消息和建议）
 * 
 * @param errorCode 错误码
 * @returns 包含消息和建议的对象
 */
export function getFullErrorInfo(errorCode: string): { message: string; suggestions: string[] } {
  const config = errorMessages[errorCode]
  return {
    message: config?.message || '操作失败，请稍后重试',
    suggestions: config?.suggestions || ['请稍后重试', '如果问题持续，请联系技术支持']
  }
}

/**
 * 检查错误码是否存在于映射中
 * 
 * @param errorCode 错误码
 * @returns 是否存在
 */
export function hasErrorMessage(errorCode: string): boolean {
  return errorCode in errorMessages
}

/**
 * 获取所有错误码列表
 * 
 * @returns 所有错误码
 */
export function getAllErrorCodes(): string[] {
  return Object.keys(errorMessages)
}

/**
 * 获取所有错误分类
 * 
 * @returns 所有错误分类
 */
export function getAllCategories(): ErrorCategory[] {
  return Object.values(ErrorCategory)
}

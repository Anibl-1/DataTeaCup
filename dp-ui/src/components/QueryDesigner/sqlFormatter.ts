/**
 * SQL 自动格式化工具
 * 支持 SELECT, FROM, JOIN, WHERE, GROUP BY, HAVING, ORDER BY 等子句的格式化
 */

export interface FormatOptions {
  /** 缩进字符，默认为两个空格 */
  indent?: string
  /** 关键字大写，默认为 true */
  uppercase?: boolean
  /** 每个子句换行，默认为 true */
  newlinePerClause?: boolean
  /** 逗号后换行（用于字段列表），默认为 true */
  newlineAfterComma?: boolean
  /** 最大行宽，超过则换行，默认为 80 */
  maxLineWidth?: number
}

const DEFAULT_OPTIONS: Required<FormatOptions> = {
  indent: '  ',
  uppercase: true,
  newlinePerClause: true,
  newlineAfterComma: true,
  maxLineWidth: 80
}

/** SQL 关键字列表 */
const SQL_KEYWORDS = [
  'SELECT', 'DISTINCT', 'FROM', 'WHERE', 'AND', 'OR', 'NOT',
  'JOIN', 'INNER', 'LEFT', 'RIGHT', 'FULL', 'OUTER', 'CROSS', 'ON',
  'GROUP', 'BY', 'HAVING', 'ORDER', 'ASC', 'DESC',
  'LIMIT', 'OFFSET', 'AS', 'IN', 'BETWEEN', 'LIKE', 'IS', 'NULL',
  'CASE', 'WHEN', 'THEN', 'ELSE', 'END',
  'COUNT', 'SUM', 'AVG', 'MAX', 'MIN',
  'UNION', 'ALL', 'EXCEPT', 'INTERSECT',
  'EXISTS', 'ANY', 'SOME'
]

/** 主要子句关键字（需要换行的） */
const CLAUSE_KEYWORDS = [
  'SELECT', 'FROM', 'WHERE', 'GROUP BY', 'HAVING', 'ORDER BY', 'LIMIT'
] as const

/** JOIN 类型关键字 */
const JOIN_KEYWORDS = ['INNER JOIN', 'LEFT JOIN', 'RIGHT JOIN', 'FULL JOIN', 'CROSS JOIN', 'JOIN'] as const

// 导出供外部使用
export { CLAUSE_KEYWORDS, JOIN_KEYWORDS }

/**
 * 格式化 SQL 语句
 * @param sql 原始 SQL 语句
 * @param options 格式化选项
 * @returns 格式化后的 SQL 语句
 */
export function formatSQL(sql: string, options: FormatOptions = {}): string {
  if (!sql || !sql.trim()) return ''

  const opts = { ...DEFAULT_OPTIONS, ...options }
  
  // 预处理：规范化空白字符
  let formatted = normalizeWhitespace(sql)
  
  // 处理关键字大小写
  if (opts.uppercase) {
    formatted = uppercaseKeywords(formatted)
  }
  
  // 解析并格式化各个子句
  formatted = formatClauses(formatted, opts)
  
  return formatted.trim()
}

/**
 * 规范化空白字符
 */
function normalizeWhitespace(sql: string): string {
  return sql
    .replace(/\s+/g, ' ')  // 多个空白字符合并为一个空格
    .replace(/\s*,\s*/g, ', ')  // 逗号后加空格
    .replace(/\s*\(\s*/g, '(')  // 左括号前后去空格
    .replace(/\s*\)\s*/g, ') ')  // 右括号后加空格
    .replace(/\s*=\s*/g, ' = ')  // 等号前后加空格
    .replace(/\s*<>\s*/g, ' <> ')
    .replace(/\s*!=\s*/g, ' != ')
    .replace(/\s*>=\s*/g, ' >= ')
    .replace(/\s*<=\s*/g, ' <= ')
    .replace(/\s*>\s*/g, ' > ')
    .replace(/\s*<\s*/g, ' < ')
    .trim()
}

/**
 * 将 SQL 关键字转为大写
 */
function uppercaseKeywords(sql: string): string {
  let result = sql
  
  // 先处理复合关键字（如 GROUP BY, ORDER BY）
  const compoundKeywords = ['GROUP BY', 'ORDER BY', 'INNER JOIN', 'LEFT JOIN', 'RIGHT JOIN', 'FULL JOIN', 'CROSS JOIN', 'IS NULL', 'IS NOT NULL', 'NOT IN', 'NOT LIKE', 'NOT BETWEEN']
  for (const keyword of compoundKeywords) {
    const regex = new RegExp(`\\b${keyword.replace(' ', '\\s+')}\\b`, 'gi')
    result = result.replace(regex, keyword)
  }
  
  // 处理单个关键字
  for (const keyword of SQL_KEYWORDS) {
    const regex = new RegExp(`\\b${keyword}\\b`, 'gi')
    result = result.replace(regex, keyword)
  }
  
  return result
}

/**
 * 格式化各个 SQL 子句
 */
function formatClauses(sql: string, opts: Required<FormatOptions>): string {
  const { indent, newlineAfterComma } = opts
  // newlinePerClause is used implicitly by formatting each clause on its own line
  
  // 解析 SQL 为各个部分
  const parts = parseSQL(sql)
  
  const lines: string[] = []
  
  // SELECT 子句
  if (parts.select) {
    const selectFields = formatSelectFields(parts.select, indent, newlineAfterComma)
    lines.push(`SELECT${selectFields}`)
  }
  
  // FROM 子句
  if (parts.from) {
    lines.push(`FROM ${parts.from.trim()}`)
  }
  
  // JOIN 子句
  for (const join of parts.joins) {
    lines.push(formatJoinClause(join, indent))
  }
  
  // WHERE 子句
  if (parts.where) {
    const whereConditions = formatWhereClause(parts.where, indent)
    lines.push(`WHERE ${whereConditions}`)
  }
  
  // GROUP BY 子句
  if (parts.groupBy) {
    lines.push(`GROUP BY ${parts.groupBy.trim()}`)
  }
  
  // HAVING 子句
  if (parts.having) {
    const havingConditions = formatWhereClause(parts.having, indent)
    lines.push(`HAVING ${havingConditions}`)
  }
  
  // ORDER BY 子句
  if (parts.orderBy) {
    lines.push(`ORDER BY ${parts.orderBy.trim()}`)
  }
  
  // LIMIT 子句
  if (parts.limit) {
    lines.push(`LIMIT ${parts.limit.trim()}`)
  }
  
  return lines.join('\n')
}

interface SQLParts {
  select: string
  from: string
  joins: string[]
  where: string
  groupBy: string
  having: string
  orderBy: string
  limit: string
}

/**
 * 解析 SQL 语句为各个部分
 */
function parseSQL(sql: string): SQLParts {
  const parts: SQLParts = {
    select: '',
    from: '',
    joins: [],
    where: '',
    groupBy: '',
    having: '',
    orderBy: '',
    limit: ''
  }
  
  // 使用正则表达式匹配各个子句
  // 注意：需要处理嵌套括号的情况
  
  let remaining = sql.trim()
  
  // 提取 SELECT
  const selectMatch = remaining.match(/^SELECT\s+(DISTINCT\s+)?(.+?)(?=\s+FROM\b)/i)
  if (selectMatch) {
    parts.select = (selectMatch[1] || '') + selectMatch[2]
    remaining = remaining.substring(selectMatch[0].length).trim()
  }
  
  // 提取 FROM（到下一个主要子句为止）
  const fromMatch = remaining.match(/^FROM\s+(.+?)(?=\s+(?:INNER\s+JOIN|LEFT\s+JOIN|RIGHT\s+JOIN|FULL\s+JOIN|CROSS\s+JOIN|JOIN|WHERE|GROUP\s+BY|HAVING|ORDER\s+BY|LIMIT)\b|$)/i)
  if (fromMatch) {
    parts.from = fromMatch[1].trim()
    remaining = remaining.substring(fromMatch[0].length).trim()
  }
  
  // 提取 JOINs
  const joinRegex = /^((?:INNER|LEFT|RIGHT|FULL|CROSS)?\s*JOIN\s+.+?\s+ON\s+.+?)(?=\s+(?:INNER\s+JOIN|LEFT\s+JOIN|RIGHT\s+JOIN|FULL\s+JOIN|CROSS\s+JOIN|JOIN|WHERE|GROUP\s+BY|HAVING|ORDER\s+BY|LIMIT)\b|$)/gi
  let joinMatch
  while ((joinMatch = joinRegex.exec(remaining)) !== null) {
    parts.joins.push(joinMatch[1].trim())
    remaining = remaining.substring(joinMatch[0].length).trim()
    joinRegex.lastIndex = 0  // 重置正则表达式
  }
  
  // 提取 WHERE
  const whereMatch = remaining.match(/^WHERE\s+(.+?)(?=\s+(?:GROUP\s+BY|HAVING|ORDER\s+BY|LIMIT)\b|$)/i)
  if (whereMatch) {
    parts.where = whereMatch[1].trim()
    remaining = remaining.substring(whereMatch[0].length).trim()
  }
  
  // 提取 GROUP BY
  const groupByMatch = remaining.match(/^GROUP\s+BY\s+(.+?)(?=\s+(?:HAVING|ORDER\s+BY|LIMIT)\b|$)/i)
  if (groupByMatch) {
    parts.groupBy = groupByMatch[1].trim()
    remaining = remaining.substring(groupByMatch[0].length).trim()
  }
  
  // 提取 HAVING
  const havingMatch = remaining.match(/^HAVING\s+(.+?)(?=\s+(?:ORDER\s+BY|LIMIT)\b|$)/i)
  if (havingMatch) {
    parts.having = havingMatch[1].trim()
    remaining = remaining.substring(havingMatch[0].length).trim()
  }
  
  // 提取 ORDER BY
  const orderByMatch = remaining.match(/^ORDER\s+BY\s+(.+?)(?=\s+LIMIT\b|$)/i)
  if (orderByMatch) {
    parts.orderBy = orderByMatch[1].trim()
    remaining = remaining.substring(orderByMatch[0].length).trim()
  }
  
  // 提取 LIMIT
  const limitMatch = remaining.match(/^LIMIT\s+(.+)$/i)
  if (limitMatch) {
    parts.limit = limitMatch[1].trim()
  }
  
  return parts
}

/**
 * 格式化 SELECT 字段列表
 */
function formatSelectFields(fields: string, indent: string, newlineAfterComma: boolean): string {
  // 检查是否有 DISTINCT
  const distinctMatch = fields.match(/^(DISTINCT\s+)/i)
  const distinctPrefix = distinctMatch ? 'DISTINCT ' : ''
  const fieldsOnly = distinctMatch ? fields.substring(distinctMatch[0].length) : fields
  
  // 分割字段（需要处理函数中的逗号）
  const fieldList = splitByComma(fieldsOnly)
  
  if (fieldList.length === 1) {
    return ` ${distinctPrefix}${fieldList[0].trim()}`
  }
  
  if (newlineAfterComma) {
    const formattedFields = fieldList.map((f, i) => {
      const trimmed = f.trim()
      return i === 0 ? `${distinctPrefix}${trimmed}` : `${indent}${trimmed}`
    }).join(',\n')
    return `\n${indent}${formattedFields}`
  }
  
  return ` ${distinctPrefix}${fieldList.map(f => f.trim()).join(', ')}`
}

/**
 * 格式化 JOIN 子句
 */
function formatJoinClause(join: string, indent: string): string {
  // 规范化 JOIN 类型
  let formatted = join
    .replace(/\bINNER\s+JOIN\b/gi, 'INNER JOIN')
    .replace(/\bLEFT\s+JOIN\b/gi, 'LEFT JOIN')
    .replace(/\bRIGHT\s+JOIN\b/gi, 'RIGHT JOIN')
    .replace(/\bFULL\s+JOIN\b/gi, 'FULL JOIN')
    .replace(/\bCROSS\s+JOIN\b/gi, 'CROSS JOIN')
    .replace(/\bJOIN\b/gi, 'JOIN')
    .replace(/\bON\b/gi, 'ON')
    .replace(/\bAS\b/gi, 'AS')
  
  // 在 ON 前换行并缩进
  formatted = formatted.replace(/\s+ON\s+/i, `\n${indent}ON `)
  
  return formatted
}

/**
 * 格式化 WHERE/HAVING 条件
 */
function formatWhereClause(conditions: string, indent: string): string {
  // 在 AND/OR 前换行
  const formatted = conditions
    .replace(/\s+AND\s+/gi, `\n${indent}AND `)
    .replace(/\s+OR\s+/gi, `\n${indent}OR `)
  
  return formatted
}

/**
 * 按逗号分割字符串，但忽略括号内的逗号
 */
function splitByComma(str: string): string[] {
  const result: string[] = []
  let current = ''
  let depth = 0
  
  for (let i = 0; i < str.length; i++) {
    const char = str[i]
    
    if (char === '(') {
      depth++
      current += char
    } else if (char === ')') {
      depth--
      current += char
    } else if (char === ',' && depth === 0) {
      result.push(current)
      current = ''
    } else {
      current += char
    }
  }
  
  if (current.trim()) {
    result.push(current)
  }
  
  return result
}

/**
 * 压缩 SQL（移除多余空白和换行）
 */
export function compressSQL(sql: string): string {
  if (!sql) return ''
  return sql
    .replace(/\s+/g, ' ')
    .replace(/\s*,\s*/g, ', ')
    .trim()
}

/**
 * 验证 SQL 基本语法（简单检查）
 */
export function validateSQLSyntax(sql: string): { valid: boolean; errors: string[] } {
  const errors: string[] = []
  
  if (!sql || !sql.trim()) {
    errors.push('SQL 语句不能为空')
    return { valid: false, errors }
  }
  
  const normalized = sql.toUpperCase()
  
  // 检查是否以 SELECT 开头
  if (!normalized.trim().startsWith('SELECT')) {
    errors.push('SQL 语句必须以 SELECT 开头')
  }
  
  // 检查是否有 FROM
  if (!normalized.includes('FROM')) {
    errors.push('SQL 语句缺少 FROM 子句')
  }
  
  // 检查括号匹配
  let parenCount = 0
  for (const char of sql) {
    if (char === '(') parenCount++
    if (char === ')') parenCount--
    if (parenCount < 0) {
      errors.push('括号不匹配：多余的右括号')
      break
    }
  }
  if (parenCount > 0) {
    errors.push('括号不匹配：缺少右括号')
  }
  
  return { valid: errors.length === 0, errors }
}

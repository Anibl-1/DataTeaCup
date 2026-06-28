/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 格式化工具函数
 */

import dayjs from 'dayjs'

/**
 * 格式化日期时间
 */
export const formatDateTime = (date: string | Date | number | null | undefined, format: string = 'YYYY-MM-DD HH:mm:ss'): string => {
  if (!date) return '-'
  // 处理时间戳数字（毫秒或秒）
  if (typeof date === 'number') {
    // 如果是秒级时间戳（10位），转换为毫秒
    const timestamp = date < 10000000000 ? date * 1000 : date
    return dayjs(timestamp).format(format)
  }
  return dayjs(date).format(format)
}

/**
 * 智能格式化单元格值（自动检测日期类型）
 * @param value 原始值
 * @param options 选项 { fieldName?: string, fieldType?: string, fieldTitle?: string }
 * @returns 格式化后的字符串
 */
export const formatCellValueSmart = (
  value: any, 
  options?: { fieldName?: string; fieldType?: string; fieldTitle?: string }
): string => {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  if (typeof value === 'boolean') {
    return value ? '是' : '否'
  }

  const keyLower = options?.fieldName?.toLowerCase() || ''
  const titleLower = options?.fieldTitle?.toLowerCase() || ''
  const typeLower = options?.fieldType?.toLowerCase() || ''

  // 智能检测时间戳数字（毫秒级：13位，秒级：10位）
  const isTimestampNumber = typeof value === 'number' && (
    (value > 1000000000000 && value < 9999999999999) || // 毫秒级时间戳 (2001-2286年)
    (value > 1000000000 && value < 9999999999)         // 秒级时间戳 (2001-2286年)
  )

  // 字段名、标题或字段类型包含日期关键词（使用结尾匹配避免误判）
  const hasDateKeyword = /(date|time|日期|时间|月份|年份|create_at|update_at|created_at|updated_at)$/i.test(keyLower) ||
                          /(date|time|日期|时间|月份|年份)$/i.test(titleLower) ||
                          /(date|time|datetime|timestamp)/i.test(typeLower)

  // 如果是时间戳数字，或者字段名暗示是日期且值可解析为日期
  if (isTimestampNumber || (hasDateKeyword && typeof value === 'string' && !isNaN(Date.parse(value)))) {
    // 秒级转毫秒
    const timestamp = isTimestampNumber && typeof value === 'number' && value < 10000000000 ? value * 1000 : value
    // 判断是否需要显示时间部分
    const showTime = /(datetime|timestamp|时间)/i.test(keyLower) || 
                     /(datetime|timestamp|时间)/i.test(titleLower) || 
                     /(datetime|timestamp)/i.test(typeLower)
    return formatDateTime(timestamp, showTime ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD')
  }

  return String(value)
}

/**
 * 格式化日期
 */
export const formatDate = (date: string | Date | number | null | undefined): string => {
  if (!date) return ''
  return formatDateTime(date, 'YYYY-MM-DD')
}

/**
 * 格式化数字（千分位）
 */
export const formatNumber = (num: number, decimals: number = 0): string => {
  if (num === null || num === undefined || isNaN(num)) return '0'
  return num.toLocaleString('en-US', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  })
}

/**
 * 格式化时间
 */
export const formatTime = (date: string | Date | null | undefined): string => {
  return formatDateTime(date, 'HH:mm:ss')
}

/**
 * 格式化文件大小
 */
export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

/**
 * 处理表格单元格值显示，统一处理空值情况
 * @param value 原始值
 * @param defaultValue 默认显示值，默认为 '-'
 * @returns 处理后的显示值
 */
export const formatTableCellValue = (value: any, defaultValue: string = '-'): string => {
  // 处理 null、undefined
  if (value === null || value === undefined) {
    return defaultValue
  }
  
  // 处理布尔值（在转换为字符串之前）
  if (typeof value === 'boolean') {
    return value ? '是' : '否'
  }
  
  // 处理数字类型的 NaN
  if (typeof value === 'number' && isNaN(value)) {
    return defaultValue
  }
  
  // 转换为字符串进行进一步检查
  let strValue = String(value)
  
  // 移除不可见字符和特殊 Unicode 字符（包括 \x00, \uFFFD 等）
  // \x00-\x1F: 控制字符
  // \x7F-\x9F: 其他控制字符
  // \uFFFD: Unicode 替换字符（显示为方框）
  // \u200B-\u200D: 零宽字符
  // \uFEFF: 零宽非断空格
  strValue = strValue.replace(/[\x00-\x1F\x7F-\x9F\uFFFD\u200B-\u200D\uFEFF]/g, '')
  
  // 去除首尾空白
  strValue = strValue.trim()
  
  // 处理空字符串、纯空白字符
  if (strValue === '' || strValue.length === 0) {
    return defaultValue
  }
  
  // 处理常见的空值表示
  const lowerValue = strValue.toLowerCase()
  if (lowerValue === 'null' || lowerValue === 'undefined' || lowerValue === 'nan' || lowerValue === 'none') {
    return defaultValue
  }
  
  // 返回处理后的字符串值
  return strValue
}


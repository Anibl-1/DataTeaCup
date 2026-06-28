/* eslint-disable @typescript-eslint/no-explicit-any */
import * as XLSX from 'xlsx'
import dayjs from 'dayjs'

/**
 * 智能格式化日期值
 * @param value 值
 * @param key 字段名
 * @returns 格式化后的值
 */
const formatDateValue = (value: any, key: string): any => {
  if (value === null || value === undefined || value === '') {
    return value
  }
  
  const keyLower = key.toLowerCase()
  
  // 检查是否是时间戳数字（13位或10位）
  const isTimestampNumber = typeof value === 'number' && (
    (value > 1000000000000 && value < 9999999999999) || // 毫秒级时间戳 (2001-2286年)
    (value > 1000000000 && value < 9999999999)         // 秒级时间戳 (2001-2286年)
  )
  
  // 字段名包含日期关键词
  const hasDateKeyword = /(date|time|日期|时间|月份|年份|create_at|update_at|created_at|updated_at)$/i.test(keyLower)
  
  if (isTimestampNumber || (hasDateKeyword && typeof value === 'string' && !isNaN(Date.parse(value)))) {
    const timestamp = isTimestampNumber && value < 10000000000 ? value * 1000 : value
    const showTime = /(datetime|timestamp|时间|time)/i.test(keyLower)
    return dayjs(timestamp).format(showTime ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD')
  }
  
  return value
}

/**
 * 导出数据到Excel（前端导出，适用于小数据量）
 * 注意：对于大数据量（超过10万行），建议使用后端导出接口
 * 
 * @param data 要导出的数据数组
 * @param filename 文件名（不含扩展名）
 * @param maxRows 最大行数限制，超过此数量会提示使用后端导出
 * @param onWarning 警告回调函数（可选）
 */
export const exportToExcel = (
  data: any[], 
  filename: string = '导出数据', 
  maxRows: number = 100000,
  onWarning?: (message: string) => void
) => {
  if (!data || data.length === 0) {
    console.warn('导出数据为空')
    if (onWarning) {
      onWarning('导出数据为空')
    }
    return
  }
  
  // 检查数据量，如果超过限制，提示使用后端导出
  if (data.length > maxRows) {
    const warningMsg = `数据量过大（${data.length}行），建议使用后端导出接口以避免浏览器内存溢出`
    console.warn(warningMsg)
    if (onWarning) {
      onWarning(warningMsg)
    }
    // 可以选择只导出前maxRows行
    // data = data.slice(0, maxRows)
  }
  
  try {
    // 格式化日期值
    const formattedData = data.map(row => {
      const newRow: Record<string, any> = {}
      Object.entries(row).forEach(([key, value]) => {
        newRow[key] = formatDateValue(value, key)
      })
      return newRow
    })
    
    const ws = XLSX.utils.json_to_sheet(formattedData)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, 'Sheet1')
    XLSX.writeFile(wb, `${filename}_${new Date().getTime()}.xlsx`)
  } catch (error) {
    console.error('导出Excel失败:', error)
    const errorMsg = '导出Excel失败，请检查数据格式或使用后端导出功能'
    if (onWarning) {
      onWarning(errorMsg)
    }
    throw new Error(errorMsg)
  }
}

/**
 * 格式化文件大小
 */
export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}


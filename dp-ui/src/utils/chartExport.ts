/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 图表导出工具
 * 支持多种格式导出：PNG、SVG、PDF、Excel、CSV
 */

import echarts from '@/utils/echarts'
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

/** 水印类型: none-无水印, user_ip-用户名_IP, custom-自定义文本 */
export type WatermarkType = 'none' | 'user_ip' | 'custom'

export interface ExportOptions {
  filename?: string
  pixelRatio?: number
  backgroundColor?: string
  excludeComponents?: string[]
  /** 水印文字（直接传入已解析的文本） */
  watermark?: string
}

/**
 * 根据水印配置解析最终水印文字
 * @param watermarkType 水印类型
 * @param customText 自定义水印文字（仅 type=custom 时使用）
 * @param username 当前用户名
 * @param ip 当前用户IP
 */
export function resolveWatermarkText(
  watermarkType?: WatermarkType | string,
  customText?: string,
  username?: string,
  ip?: string
): string {
  if (!watermarkType || watermarkType === 'none') return ''
  
  if (watermarkType === 'user_ip') {
    const user = username || '未知用户'
    const addr = ip || '未知IP'
    return `${user}_${addr}`
  }
  
  if (watermarkType === 'custom') {
    return customText || ''
  }
  
  // 兼容旧数据：如果 watermarkType 不是已知类型，当作自定义文本
  return customText || ''
}

/**
 * 从图表实例中获取有效背景色
 * 处理 transparent / undefined 等无效值，确保导出有确定背景
 */
function resolveBackgroundColor(chart: echarts.ECharts, override?: string): string {
  if (override && override !== 'transparent') return override
  const chartOption = chart.getOption() as any
  const bg = chartOption?.backgroundColor
  if (bg && bg !== 'transparent') return bg
  return '#fff'
}

/**
 * 在Canvas上绘制水印
 */
function drawWatermark(ctx: CanvasRenderingContext2D, watermark: string, width: number, height: number): void {
  ctx.save()
  ctx.globalAlpha = 0.15
  ctx.font = '20px sans-serif'
  ctx.fillStyle = '#000'
  ctx.rotate(-20 * Math.PI / 180)
  
  // 计算水印文字宽度以确定间距
  const textWidth = ctx.measureText(watermark).width
  const xGap = Math.max(textWidth + 100, 200)
  const yGap = 80
  
  for (let y = -height; y < height * 2; y += yGap) {
    for (let x = -width; x < width * 2; x += xGap) {
      ctx.fillText(watermark, x, y)
    }
  }
  ctx.restore()
}

/**
 * 导出图表为PNG图片
 * 自动从图表实例获取当前背景色，确保导出颜色与显示一致
 * 支持水印功能
 */
export function exportToPng(
  chart: echarts.ECharts,
  options: ExportOptions = {}
): void {
  const {
    filename = 'chart',
    pixelRatio = 2,
    watermark
  } = options

  const backgroundColor = resolveBackgroundColor(chart, options.backgroundColor)

  const url = chart.getDataURL({
    type: 'png',
    pixelRatio,
    backgroundColor,
    excludeComponents: options.excludeComponents
  })

  if (watermark) {
    // 有水印：用canvas叠加水印后导出
    const img = new Image()
    img.onload = () => {
      const canvas = document.createElement('canvas')
      canvas.width = img.width
      canvas.height = img.height
      const ctx = canvas.getContext('2d')!
      ctx.drawImage(img, 0, 0)
      
      // 绘制水印
      drawWatermark(ctx, watermark, canvas.width, canvas.height)
      
      downloadFile(canvas.toDataURL('image/png'), `${filename}.png`)
    }
    img.src = url
  } else {
    downloadFile(url, `${filename}.png`)
  }
}

/**
 * 导出图表为SVG
 */
export function exportToSvg(
  chart: echarts.ECharts,
  options: ExportOptions = {}
): void {
  const { filename = 'chart' } = options

  // 获取SVG渲染器的内容
  const svgElement = chart.getDom().querySelector('svg')
  if (!svgElement) {
    console.error('图表未使用SVG渲染器')
    return
  }

  const svgData = new XMLSerializer().serializeToString(svgElement)
  const blob = new Blob([svgData], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)

  downloadFile(url, `${filename}.svg`)
  URL.revokeObjectURL(url)
}

/**
 * 导出图表数据为Excel
 */
export function exportToExcel(
  data: any[],
  options: { filename?: string; sheetName?: string } = {}
): void {
  const { filename = 'chart_data', sheetName = 'Sheet1' } = options

  if (!data || data.length === 0) {
    console.error('没有数据可导出')
    return
  }

  // 格式化日期值
  const formattedData = data.map(row => {
    const newRow: Record<string, any> = {}
    Object.entries(row).forEach(([key, value]) => {
      newRow[key] = formatDateValue(value, key)
    })
    return newRow
  })

  const worksheet = XLSX.utils.json_to_sheet(formattedData)
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, sheetName)

  // 自动调整列宽
  const colWidths = Object.keys(formattedData[0]).map(key => {
    const maxLength = Math.max(
      key.length,
      ...formattedData.map(row => String(row[key] || '').length)
    )
    return { wch: Math.min(maxLength + 2, 50) }
  })
  worksheet['!cols'] = colWidths

  XLSX.writeFile(workbook, `${filename}.xlsx`)
}

/**
 * 导出图表数据为CSV
 */
export function exportToCsv(
  data: any[],
  options: { filename?: string; separator?: string } = {}
): void {
  const { filename = 'chart_data', separator = ',' } = options

  if (!data || data.length === 0) {
    console.error('没有数据可导出')
    return
  }

  // 格式化日期值
  const formattedData = data.map(row => {
    const newRow: Record<string, any> = {}
    Object.entries(row).forEach(([key, value]) => {
      newRow[key] = formatDateValue(value, key)
    })
    return newRow
  })

  const headers = Object.keys(formattedData[0])
  const csvContent = [
    headers.join(separator),
    ...formattedData.map(row =>
      headers.map(header => {
        const value = row[header]
        // 处理包含分隔符或换行的值
        if (typeof value === 'string' && (value.includes(separator) || value.includes('\n'))) {
          return `"${value.replace(/"/g, '""')}"`
        }
        return value ?? ''
      }).join(separator)
    )
  ].join('\n')

  // 添加BOM以支持中文
  const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)

  downloadFile(url, `${filename}.csv`)
  URL.revokeObjectURL(url)
}

/**
 * 导出图表为JSON配置
 */
export function exportToJson(
  option: any,
  options: { filename?: string } = {}
): void {
  const { filename = 'chart_config' } = options

  const jsonContent = JSON.stringify(option, null, 2)
  const blob = new Blob([jsonContent], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)

  downloadFile(url, `${filename}.json`)
  URL.revokeObjectURL(url)
}

/**
 * 复制图表为Base64图片到剪贴板
 */
export async function copyChartToClipboard(
  chart: echarts.ECharts,
  options: ExportOptions = {}
): Promise<boolean> {
  const { pixelRatio = 2 } = options
  const backgroundColor = resolveBackgroundColor(chart, options.backgroundColor)

  try {
    const url = chart.getDataURL({
      type: 'png',
      pixelRatio,
      backgroundColor
    })

    // 将base64转换为blob
    const response = await fetch(url)
    const blob = await response.blob()

    // 复制到剪贴板
    await navigator.clipboard.write([
      new ClipboardItem({ 'image/png': blob })
    ])

    return true
  } catch (error) {
    console.error('复制到剪贴板失败:', error)
    return false
  }
}

/**
 * 批量导出多个图表
 */
export async function exportMultipleCharts(
  charts: Array<{ chart: echarts.ECharts; name: string }>,
  options: ExportOptions = {}
): Promise<void> {
  const { pixelRatio = 2 } = options

  for (const { chart, name } of charts) {
    const backgroundColor = resolveBackgroundColor(chart, options.backgroundColor)
    const url = chart.getDataURL({
      type: 'png',
      pixelRatio,
      backgroundColor
    })
    downloadFile(url, `${name}.png`)
    // 添加延迟避免浏览器阻止多次下载
    await new Promise(resolve => setTimeout(resolve, 500))
  }
}

/**
 * 下载文件
 */
function downloadFile(url: string, filename: string): void {
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

/**
 * 格式化数据用于导出
 */
export function formatDataForExport(
  data: any[],
  fieldMapping?: { [key: string]: string }
): any[] {
  if (!fieldMapping) return data

  return data.map(row => {
    const newRow: any = {}
    Object.entries(row).forEach(([key, value]) => {
      const newKey = fieldMapping[key] || key
      newRow[newKey] = value
    })
    return newRow
  })
}

/**
 * 获取图表数据摘要
 */
export function getChartDataSummary(data: any[]): {
  rowCount: number
  columnCount: number
  columns: string[]
  numericColumns: string[]
  textColumns: string[]
} {
  if (!data || data.length === 0) {
    return {
      rowCount: 0,
      columnCount: 0,
      columns: [],
      numericColumns: [],
      textColumns: []
    }
  }

  const columns = Object.keys(data[0])
  const numericColumns: string[] = []
  const textColumns: string[] = []

  columns.forEach(col => {
    const sampleValue = data.find(row => row[col] != null)?.[col]
    if (typeof sampleValue === 'number') {
      numericColumns.push(col)
    } else {
      textColumns.push(col)
    }
  })

  return {
    rowCount: data.length,
    columnCount: columns.length,
    columns,
    numericColumns,
    textColumns
  }
}

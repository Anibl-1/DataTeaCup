/**
 * 文件下载工具
 */
import { message } from './message'

/**
 * 下载 Blob 文件
 */
export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/**
 * 下载文本文件
 */
export function downloadText(content: string, filename: string, mimeType = 'text/plain'): void {
  const blob = new Blob([content], { type: mimeType })
  downloadBlob(blob, filename)
}

/**
 * 下载 JSON 文件
 */
export function downloadJSON(data: unknown, filename: string): void {
  const content = JSON.stringify(data, null, 2)
  downloadText(content, filename.endsWith('.json') ? filename : `${filename}.json`, 'application/json')
}

/**
 * 下载 CSV 文件
 */
export function downloadCSV(data: Record<string, unknown>[], filename: string, headers?: string[]): void {
  if (data.length === 0) {
    message.warning('没有数据可导出')
    return
  }
  
  const keys = headers || Object.keys(data[0])
  const csvRows: string[] = []
  
  // 添加表头
  csvRows.push(keys.join(','))
  
  // 添加数据行
  for (const row of data) {
    const values = keys.map(key => {
      const value = row[key]
      // 处理包含逗号、引号、换行的值
      if (value === null || value === undefined) {
        return ''
      }
      const strValue = String(value)
      if (strValue.includes(',') || strValue.includes('"') || strValue.includes('\n')) {
        return `"${strValue.replace(/"/g, '""')}"`
      }
      return strValue
    })
    csvRows.push(values.join(','))
  }
  
  // 添加 BOM 以支持中文
  const BOM = '\uFEFF'
  const content = BOM + csvRows.join('\n')
  downloadText(content, filename.endsWith('.csv') ? filename : `${filename}.csv`, 'text/csv;charset=utf-8')
}

/**
 * 从 URL 下载文件
 */
export async function downloadFromUrl(url: string, filename?: string): Promise<void> {
  try {
    const response = await fetch(url)
    if (!response.ok) {
      throw new Error(`下载失败: ${response.status}`)
    }
    
    const blob = await response.blob()
    
    // 尝试从响应头获取文件名
    let finalFilename = filename
    if (!finalFilename) {
      const contentDisposition = response.headers.get('content-disposition')
      if (contentDisposition) {
        const match = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
        if (match) {
          finalFilename = match[1].replace(/['"]/g, '')
        }
      }
    }
    
    if (!finalFilename) {
      finalFilename = url.split('/').pop() || 'download'
    }
    
    downloadBlob(blob, finalFilename)
  } catch (error) {
    console.error('下载失败:', error)
    message.error('下载失败')
    throw error
  }
}

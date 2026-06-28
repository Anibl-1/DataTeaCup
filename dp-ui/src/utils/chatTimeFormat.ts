/**
 * 聊天时间格式化工具
 * 当天显示 HH:mm，非当天显示 MM-DD HH:mm
 */
export function formatChatTime(timeStr: string, now?: Date): string {
  const date = new Date(timeStr)
  if (isNaN(date.getTime())) return ''

  const reference = now || new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const hhmm = `${pad(date.getHours())}:${pad(date.getMinutes())}`

  const isToday =
    date.getFullYear() === reference.getFullYear() &&
    date.getMonth() === reference.getMonth() &&
    date.getDate() === reference.getDate()

  if (isToday) {
    return hhmm
  }

  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${hhmm}`
}

/**
 * 格式化文件大小
 */
export function formatFileSize(bytes?: number): string {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

/**
 * 在线状态颜色映射
 */
export function getStatusColor(status: 'online' | 'offline'): string {
  return status === 'online' ? '#10B981' : '#9CA3AF'
}

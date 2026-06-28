/**
 * 文件扩展名到图标名称的映射工具
 * 根据文件名返回对应的 Ionicons5 图标组件名称
 */

export type FileIconName =
  | 'ImageOutline'
  | 'DocumentTextOutline'
  | 'FolderOpenOutline'
  | 'CodeSlashOutline'
  | 'VideocamOutline'
  | 'MusicalNotesOutline'
  | 'DocumentOutline'

const extensionMap: Record<string, FileIconName> = {
  // 图片
  '.jpg': 'ImageOutline',
  '.jpeg': 'ImageOutline',
  '.png': 'ImageOutline',
  '.gif': 'ImageOutline',
  '.svg': 'ImageOutline',
  '.webp': 'ImageOutline',
  // 文档
  '.pdf': 'DocumentTextOutline',
  '.doc': 'DocumentTextOutline',
  '.docx': 'DocumentTextOutline',
  '.xls': 'DocumentTextOutline',
  '.xlsx': 'DocumentTextOutline',
  '.ppt': 'DocumentTextOutline',
  '.pptx': 'DocumentTextOutline',
  '.txt': 'DocumentTextOutline',
  // 压缩包
  '.zip': 'FolderOpenOutline',
  '.rar': 'FolderOpenOutline',
  '.7z': 'FolderOpenOutline',
  '.tar': 'FolderOpenOutline',
  '.gz': 'FolderOpenOutline',
  // 代码
  '.js': 'CodeSlashOutline',
  '.ts': 'CodeSlashOutline',
  '.vue': 'CodeSlashOutline',
  '.html': 'CodeSlashOutline',
  '.css': 'CodeSlashOutline',
  '.json': 'CodeSlashOutline',
  '.py': 'CodeSlashOutline',
  '.java': 'CodeSlashOutline',
  // 视频
  '.mp4': 'VideocamOutline',
  '.avi': 'VideocamOutline',
  '.mov': 'VideocamOutline',
  '.mkv': 'VideocamOutline',
  // 音频
  '.mp3': 'MusicalNotesOutline',
  '.wav': 'MusicalNotesOutline',
  '.flac': 'MusicalNotesOutline',
  '.aac': 'MusicalNotesOutline',
}

/**
 * 根据文件名返回对应的图标名称
 * @param fileName 文件名（如 "report.pdf"）
 * @returns Ionicons5 图标组件名称
 */
export function getFileIcon(fileName: string): FileIconName {
  if (!fileName) return 'DocumentOutline'
  const dotIndex = fileName.lastIndexOf('.')
  if (dotIndex === -1) return 'DocumentOutline'
  const ext = fileName.slice(dotIndex).toLowerCase()
  return extensionMap[ext] || 'DocumentOutline'
}

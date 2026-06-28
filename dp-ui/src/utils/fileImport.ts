/**
 * 文件导入校验工具
 *
 * 支持 Excel (.xlsx/.xls) 和文本文件 (.csv/.txt) 导入校验。
 * 文件大小上限 100MB。
 */

/** 允许的文件扩展名 */
export const ALLOWED_EXTENSIONS = ['.xlsx', '.xls', '.csv', '.txt'] as const

/** 最大文件大小 (100MB) */
export const MAX_FILE_SIZE = 100 * 1024 * 1024

export type AllowedExtension = (typeof ALLOWED_EXTENSIONS)[number]

export interface FileValidationResult {
  valid: boolean
  error?: string
}

/**
 * 从文件名中提取小写扩展名
 */
export function getFileExtension(fileName: string): string {
  const dotIndex = fileName.lastIndexOf('.')
  if (dotIndex === -1 || dotIndex === fileName.length - 1) return ''
  return fileName.substring(dotIndex).toLowerCase()
}

/**
 * 校验文件是否可导入
 * @param fileName 文件名
 * @param fileSize 文件大小（字节）
 * @returns 校验结果
 */
export function validateFileForImport(fileName: string, fileSize: number): FileValidationResult {
  if (!fileName) {
    return { valid: false, error: '文件名不能为空' }
  }

  const ext = getFileExtension(fileName)
  if (!ext) {
    return { valid: false, error: '无法识别文件类型' }
  }

  if (!(ALLOWED_EXTENSIONS as readonly string[]).includes(ext)) {
    return {
      valid: false,
      error: `不支持的文件格式: ${ext}，仅支持 ${ALLOWED_EXTENSIONS.join(', ')}`
    }
  }

  if (fileSize <= 0) {
    return { valid: false, error: '文件为空' }
  }

  if (fileSize > MAX_FILE_SIZE) {
    return {
      valid: false,
      error: `文件大小超过限制，最大允许 100MB`
    }
  }

  return { valid: true }
}

/**
 * 判断文件是否为 Excel 类型
 */
export function isExcelFile(fileName: string): boolean {
  const ext = getFileExtension(fileName)
  return ext === '.xlsx' || ext === '.xls'
}

/**
 * 判断文件是否为文本类型
 */
export function isTextFile(fileName: string): boolean {
  const ext = getFileExtension(fileName)
  return ext === '.csv' || ext === '.txt'
}

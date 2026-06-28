/**
 * 剪贴板工具
 */
import { message } from './message'

/**
 * 复制文本到剪贴板
 */
export async function copyToClipboard(text: string, showMessage = true): Promise<boolean> {
  try {
    // 优先使用现代 API
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
      if (showMessage) {
        message.success('已复制到剪贴板')
      }
      return true
    }
    
    // 降级方案：使用 execCommand
    const textArea = document.createElement('textarea')
    textArea.value = text
    textArea.style.position = 'fixed'
    textArea.style.left = '-9999px'
    textArea.style.top = '-9999px'
    document.body.appendChild(textArea)
    textArea.focus()
    textArea.select()
    
    const success = document.execCommand('copy')
    document.body.removeChild(textArea)
    
    if (success && showMessage) {
      message.success('已复制到剪贴板')
    }
    
    return success
  } catch (error) {
    console.error('复制失败:', error)
    if (showMessage) {
      message.error('复制失败')
    }
    return false
  }
}

/**
 * 从剪贴板读取文本
 */
export async function readFromClipboard(): Promise<string | null> {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      return await navigator.clipboard.readText()
    }
    return null
  } catch (error) {
    console.error('读取剪贴板失败:', error)
    return null
  }
}

import { useMessage, useDialog } from 'naive-ui'
import { h } from 'vue'
import { generateTraceId, type ErrorInfo } from './errorHandler'

let messageInstance: ReturnType<typeof useMessage> | null = null
let dialogInstance: ReturnType<typeof useDialog> | null = null

export const initMessage = (instance: ReturnType<typeof useMessage>) => {
  messageInstance = instance
}

export const initDialog = (instance: ReturnType<typeof useDialog>) => {
  dialogInstance = instance
}

export const message = {
  success: (content: string) => messageInstance?.success(content),
  error: (content: string) => messageInstance?.error(content),
  warning: (content: string) => messageInstance?.warning(content),
  info: (content: string) => messageInstance?.info(content),
  
  /**
   * 显示带追踪ID的错误消息
   * 
   * 需求 18.5: THE Error_Handler SHALL 为每个错误生成唯一的错误追踪ID，便于问题排查
   * 
   * @param content 错误消息内容
   * @param traceId 可选的追踪ID，如果不提供则自动生成
   * @returns 追踪ID
   */
  errorWithTraceId: (content: string, traceId?: string): string => {
    const id = traceId || generateTraceId()
    const messageContent = `${content} (追踪ID: ${id})`
    messageInstance?.error(messageContent, { duration: 5000 })
    return id
  },
  
  /**
   * 显示 ErrorInfo 对象的错误消息
   * 
   * @param errorInfo 错误信息对象
   */
  errorFromInfo: (errorInfo: ErrorInfo) => {
    const messageContent = `${errorInfo.userMessage} (追踪ID: ${errorInfo.traceId})`
    messageInstance?.error(messageContent, { duration: 5000 })
  }
}

export const dialog = {
  warning: (options: {
    title: string
    content: string
    positiveText?: string
    negativeText?: string
    onPositiveClick?: () => void | Promise<void>
    onNegativeClick?: () => void | Promise<void>
  }) => {
    if (!dialogInstance) {
      console.error('Dialog instance not initialized. Please call initDialog first.')
      return
    }
    dialogInstance.warning({
      title: options.title,
      content: options.content,
      positiveText: options.positiveText || '确定',
      negativeText: options.negativeText || '取消',
      onPositiveClick: options.onPositiveClick,
      onNegativeClick: options.onNegativeClick
    })
  },
  success: (options: {
    title: string
    content: string
    positiveText?: string
    negativeText?: string
    onPositiveClick?: () => void | Promise<void>
  }) => {
    if (!dialogInstance) {
      console.error('Dialog instance not initialized. Please call initDialog first.')
      return
    }
    dialogInstance.success({
      title: options.title,
      content: options.content,
      positiveText: options.positiveText || '确定',
      negativeText: options.negativeText || '取消',
      onPositiveClick: options.onPositiveClick
    })
  },
  error: (options: {
    title: string
    content: string
    positiveText?: string
    negativeText?: string
    onPositiveClick?: () => void | Promise<void>
  }) => {
    if (!dialogInstance) {
      console.error('Dialog instance not initialized. Please call initDialog first.')
      return
    }
    dialogInstance.error({
      title: options.title,
      content: options.content,
      positiveText: options.positiveText || '确定',
      negativeText: options.negativeText || '取消',
      onPositiveClick: options.onPositiveClick
    })
  },
  
  /**
   * 显示带追踪ID的错误对话框
   * 
   * 需求 18.5: THE Error_Handler SHALL 为每个错误生成唯一的错误追踪ID，便于问题排查
   * 
   * @param options 对话框选项
   * @returns 追踪ID
   */
  errorWithTraceId: (options: {
    title: string
    content: string
    traceId?: string
    suggestions?: string[]
    positiveText?: string
    onPositiveClick?: () => void | Promise<void>
  }): string => {
    if (!dialogInstance) {
      console.error('Dialog instance not initialized. Please call initDialog first.')
      return options.traceId || generateTraceId()
    }
    
    const traceId = options.traceId || generateTraceId()
    
    // 构建包含追踪ID和建议的内容
    let fullContent = options.content
    
    if (options.suggestions && options.suggestions.length > 0) {
      fullContent += '\n\n建议：\n' + options.suggestions.map(s => `• ${s}`).join('\n')
    }
    
    fullContent += `\n\n错误追踪ID: ${traceId}\n（请在反馈问题时提供此ID）`
    
    dialogInstance.error({
      title: options.title,
      content: fullContent,
      positiveText: options.positiveText || '确定',
      onPositiveClick: options.onPositiveClick
    })
    
    return traceId
  },
  
  /**
   * 显示 ErrorInfo 对象的错误对话框
   * 
   * @param errorInfo 错误信息对象
   * @param options 额外选项
   */
  errorFromInfo: (errorInfo: ErrorInfo, options?: {
    title?: string
    positiveText?: string
    onPositiveClick?: () => void | Promise<void>
  }) => {
    if (!dialogInstance) {
      console.error('Dialog instance not initialized. Please call initDialog first.')
      return
    }
    
    let fullContent = errorInfo.userMessage
    
    if (errorInfo.suggestions && errorInfo.suggestions.length > 0) {
      fullContent += '\n\n建议：\n' + errorInfo.suggestions.map(s => `• ${s}`).join('\n')
    }
    
    fullContent += `\n\n错误追踪ID: ${errorInfo.traceId}\n（请在反馈问题时提供此ID）`
    
    dialogInstance.error({
      title: options?.title || '操作失败',
      content: fullContent,
      positiveText: options?.positiveText || '确定',
      onPositiveClick: options?.onPositiveClick
    })
  }
}


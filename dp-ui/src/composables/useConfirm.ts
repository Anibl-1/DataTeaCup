/**
 * 确认对话框 Hook
 */
import { useDialog } from 'naive-ui'
import type { DialogOptions } from 'naive-ui'

export interface ConfirmOptions {
  title?: string
  content: string
  type?: 'info' | 'success' | 'warning' | 'error'
  positiveText?: string
  negativeText?: string
  showIcon?: boolean
}

export function useConfirm() {
  const dialog = useDialog()

  /**
   * 显示确认对话框
   */
  const confirm = (options: ConfirmOptions): Promise<boolean> => {
    return new Promise((resolve) => {
      const dialogOptions: DialogOptions = {
        title: options.title || '确认',
        content: options.content,
        positiveText: options.positiveText || '确定',
        negativeText: options.negativeText || '取消',
        showIcon: options.showIcon !== false,
        onPositiveClick: () => {
          resolve(true)
        },
        onNegativeClick: () => {
          resolve(false)
        },
        onClose: () => {
          resolve(false)
        }
      }

      switch (options.type) {
        case 'success':
          dialog.success(dialogOptions)
          break
        case 'warning':
          dialog.warning(dialogOptions)
          break
        case 'error':
          dialog.error(dialogOptions)
          break
        default:
          dialog.info(dialogOptions)
      }
    })
  }

  /**
   * 删除确认
   */
  const confirmDelete = (itemName?: string): Promise<boolean> => {
    return confirm({
      title: '确认删除',
      content: itemName ? `确定要删除"${itemName}"吗？此操作不可恢复。` : '确定要删除吗？此操作不可恢复。',
      type: 'warning',
      positiveText: '删除',
      negativeText: '取消'
    })
  }

  /**
   * 批量删除确认
   */
  const confirmBatchDelete = (count: number): Promise<boolean> => {
    return confirm({
      title: '确认批量删除',
      content: `确定要删除选中的 ${count} 条记录吗？此操作不可恢复。`,
      type: 'warning',
      positiveText: '删除',
      negativeText: '取消'
    })
  }

  /**
   * 操作确认
   */
  const confirmAction = (action: string, itemName?: string): Promise<boolean> => {
    return confirm({
      title: `确认${action}`,
      content: itemName ? `确定要${action}"${itemName}"吗？` : `确定要${action}吗？`,
      type: 'info'
    })
  }

  return {
    confirm,
    confirmDelete,
    confirmBatchDelete,
    confirmAction
  }
}

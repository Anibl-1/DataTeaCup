/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 统一的导出组合式函数
 * 封装前端 Excel/CSV 导出和后端导出的统一调用逻辑
 * 支持将导出任务添加到导出中心（Req 22.3）
 *
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 22.3
 */
import { ref, type Ref } from 'vue'
import { useMessage } from 'naive-ui'
import { exportToExcel } from '@/utils/export'
import { logger } from '@/utils/logger'
import { createExportTask } from '@/api/exportTask'

/** 导出中心任务参数 */
export interface ExportCenterParams {
  /** 任务名称 */
  taskName: string
  /** 任务类型（如 'report', 'user', 'datasource' 等） */
  taskType?: string
  /** 关联资源 ID */
  refId?: number
  /** 关联资源编码 */
  refCode?: string
  /** 筛选条件 JSON */
  filters?: string
  /** 额外参数 JSON */
  params?: string
}

/** useExport 配置选项 */
export interface UseExportOptions {
  /** 后端导出 API（可选） */
  backendExportFn?: (params: any) => Promise<Blob>
  /** 前端导出最大行数阈值，超过则使用后端导出 */
  maxFrontendRows?: number
  /** 默认文件名 */
  defaultFilename?: string
}

/** useExport 返回值 */
export interface UseExportReturn {
  /** 导出中状态 */
  exporting: Ref<boolean>
  /** 导出为 Excel */
  exportExcel: (data: any[], filename?: string) => Promise<void>
  /** 导出为 CSV */
  exportCsv: (data: any[], filename?: string) => Promise<void>
  /** 导出下拉选项（可直接绑定 NDropdown） */
  exportOptions: { label: string; key: string }[]
  /** 处理导出选择 */
  handleExportSelect: (key: string, data: any[]) => Promise<void>
  /** 将导出任务添加到导出中心（Req 22.3） */
  addToExportCenter: (params: ExportCenterParams) => Promise<boolean>
}

/** 默认前端导出最大行数 */
const DEFAULT_MAX_FRONTEND_ROWS = 10000

/**
 * 将数据数组转换为 CSV 字符串
 */
function convertToCsv(data: any[]): string {
  if (data.length === 0) return ''
  const headers = Object.keys(data[0])
  const escapeField = (field: any): string => {
    const str = field === null || field === undefined ? '' : String(field)
    if (str.includes(',') || str.includes('"') || str.includes('\n')) {
      return `"${str.replace(/"/g, '""')}"`
    }
    return str
  }
  const lines = [
    headers.map(escapeField).join(','),
    ...data.map(row => headers.map(h => escapeField(row[h])).join(','))
  ]
  return lines.join('\n')
}

/**
 * 触发浏览器下载 Blob
 */
function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

/**
 * 统一的导出组合式函数
 *
 * 封装 Excel/CSV 前端导出，支持阈值路由（数据量超过 maxFrontendRows 时自动调用后端导出），
 * 导出锁防止重复触发，错误时通过 message.error 通知用户。
 */
export function useExport(options: UseExportOptions = {}): UseExportReturn {
  const {
    backendExportFn,
    maxFrontendRows = DEFAULT_MAX_FRONTEND_ROWS,
    defaultFilename = '导出数据'
  } = options

  const message = useMessage()
  const exporting = ref(false) as Ref<boolean>

  /** 静态导出选项列表 */
  const exportOptions: { label: string; key: string }[] = [
    { label: 'Excel', key: 'excel' },
    { label: 'CSV', key: 'csv' }
  ]

  /**
   * 检查是否应使用后端导出（Req 3.3）
   * 当数据量超过阈值且配置了 backendExportFn 时返回 true
   */
  function shouldUseBackend(dataLength: number): boolean {
    return dataLength > maxFrontendRows && !!backendExportFn
  }

  /**
   * 调用后端导出并触发下载
   */
  async function doBackendExport(filename: string, format: string): Promise<void> {
    const blob = await backendExportFn!({ format })
    downloadBlob(blob, `${filename}_${Date.now()}.${format === 'csv' ? 'csv' : 'xlsx'}`)
  }

  /**
   * 导出为 Excel（Req 3.1）
   * - 导出锁：exporting 为 true 时拒绝重复调用（Req 3.4）
   * - 阈值路由：数据量超过 maxFrontendRows 时调用 backendExportFn（Req 3.3）
   * - 错误处理：失败时 message.error 通知用户（Req 3.5）
   */
  const exportExcel = async (data: any[], filename?: string): Promise<void> => {
    if (exporting.value) return

    exporting.value = true
    const name = filename || defaultFilename
    try {
      if (shouldUseBackend(data.length)) {
        await doBackendExport(name, 'excel')
      } else {
        exportToExcel(data, name)
      }
    } catch (err) {
      const error = err instanceof Error ? err : new Error(String(err))
      logger.error('导出 Excel 失败', error)
      message.error('导出 Excel 失败，请稍后重试')
    } finally {
      exporting.value = false
    }
  }

  /**
   * 导出为 CSV（Req 3.2）
   * - 导出锁：exporting 为 true 时拒绝重复调用（Req 3.4）
   * - 阈值路由：数据量超过 maxFrontendRows 时调用 backendExportFn（Req 3.3）
   * - 错误处理：失败时 message.error 通知用户（Req 3.5）
   */
  const exportCsv = async (data: any[], filename?: string): Promise<void> => {
    if (exporting.value) return

    exporting.value = true
    const name = filename || defaultFilename
    try {
      if (shouldUseBackend(data.length)) {
        await doBackendExport(name, 'csv')
      } else {
        const csvContent = convertToCsv(data)
        const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
        downloadBlob(blob, `${name}_${Date.now()}.csv`)
      }
    } catch (err) {
      const error = err instanceof Error ? err : new Error(String(err))
      logger.error('导出 CSV 失败', error)
      message.error('导出 CSV 失败，请稍后重试')
    } finally {
      exporting.value = false
    }
  }

  /**
   * 处理导出选择（便捷绑定，可直接用于 NDropdown 的 onSelect）
   */
  const handleExportSelect = async (key: string, data: any[]): Promise<void> => {
    if (key === 'excel') {
      await exportExcel(data)
    } else if (key === 'csv') {
      await exportCsv(data)
    }
  }

  /**
   * 将导出任务添加到导出中心（Req 22.3）
   * - 导出锁：exporting 为 true 时拒绝重复调用
   * - 调用 createExportTask API 创建后台导出任务
   * - 成功后提示用户到导出中心查看进度
   * @returns true 表示任务创建成功，false 表示失败
   */
  const addToExportCenter = async (params: ExportCenterParams): Promise<boolean> => {
    if (exporting.value) return false

    exporting.value = true
    try {
      const res = await createExportTask({
        taskName: params.taskName,
        taskType: params.taskType,
        refId: params.refId,
        refCode: params.refCode,
        filters: params.filters,
        params: params.params
      }) as any

      if (res.code === 200) {
        message.success('导出任务已创建，请在导出中心查看进度和下载')
        return true
      } else {
        message.error(res.msg || '创建导出任务失败')
        return false
      }
    } catch (err) {
      const error = err instanceof Error ? err : new Error(String(err))
      logger.error('创建导出任务失败', error)
      message.error('创建导出任务失败，请稍后重试')
      return false
    } finally {
      exporting.value = false
    }
  }

  return {
    exporting,
    exportExcel,
    exportCsv,
    exportOptions,
    handleExportSelect,
    addToExportCenter
  }
}

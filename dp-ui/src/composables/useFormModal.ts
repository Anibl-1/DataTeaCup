/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 统一的表单弹窗组合式函数
 * 封装新增/编辑弹窗的打开、关闭、提交逻辑
 *
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6
 */
import { ref, computed, type Ref, type ComputedRef } from 'vue'
import { logger } from '@/utils/logger'

/** useFormModal 配置选项 */
export interface UseFormModalOptions<T> {
  /** 默认表单数据（用于重置） */
  defaultFormData: T | (() => T)
  /** 新增 API */
  createFn?: (data: T) => Promise<any>
  /** 编辑 API */
  updateFn?: (data: T) => Promise<any>
  /** 提交成功回调 */
  onSuccess?: (mode: 'create' | 'edit') => void
  /** 提交失败回调 */
  onError?: (error: Error) => void
}

/** useFormModal 返回值 */
export interface UseFormModalReturn<T> {
  /** 弹窗可见状态 */
  visible: Ref<boolean>
  /** 当前模式 */
  mode: Ref<'create' | 'edit'>
  /** 表单数据 */
  formData: Ref<T>
  /** 提交中状态 */
  submitting: Ref<boolean>
  /** 弹窗标题（根据 mode 自动生成） */
  title: ComputedRef<string>
  /** 打开新增弹窗 */
  openCreate: () => void
  /** 打开编辑弹窗 */
  openEdit: (record: Partial<T>) => void
  /** 关闭弹窗 */
  close: () => void
  /** 提交表单 */
  submit: () => Promise<void>
  /** 重置表单数据 */
  resetForm: () => void
}

/** 获取默认表单数据的深拷贝 */
function getDefaultData<T>(defaultFormData: T | (() => T)): T {
  const raw = typeof defaultFormData === 'function'
    ? (defaultFormData as () => T)()
    : defaultFormData
  return JSON.parse(JSON.stringify(raw))
}

/**
 * 统一的表单弹窗组合式函数
 *
 * 封装弹窗的可见状态、模式切换、表单数据管理和提交逻辑。
 * submit 根据 mode 自动路由到 createFn 或 updateFn，
 * 成功后自动关闭弹窗，失败时保持弹窗打开并通过 onError 回调通知。
 */
export function useFormModal<T extends object>(
  options: UseFormModalOptions<T>
): UseFormModalReturn<T> {
  const { defaultFormData, createFn, updateFn, onSuccess, onError } = options

  // --- 状态 (Req 2.1) ---
  const visible = ref(false) as Ref<boolean>
  const mode = ref<'create' | 'edit'>('create') as Ref<'create' | 'edit'>
  const formData = ref<T>(getDefaultData(defaultFormData)) as Ref<T>
  const submitting = ref(false) as Ref<boolean>

  // --- 计算属性 ---
  const title: ComputedRef<string> = computed(() =>
    mode.value === 'create' ? '新增' : '编辑'
  )

  // --- 方法 ---

  /** 重置表单数据为默认值 */
  const resetForm = (): void => {
    formData.value = getDefaultData(defaultFormData)
  }

  /** 打开新增弹窗 (Req 2.2) */
  const openCreate = (): void => {
    mode.value = 'create'
    resetForm()
    visible.value = true
  }

  /** 打开编辑弹窗 (Req 2.3) */
  const openEdit = (record: Partial<T>): void => {
    mode.value = 'edit'
    formData.value = { ...getDefaultData(defaultFormData), ...record }
    visible.value = true
  }

  /** 关闭弹窗 (Req 2.4) */
  const close = (): void => {
    visible.value = false
    resetForm()
  }

  /** 提交表单 (Req 2.5, 2.6) */
  const submit = async (): Promise<void> => {
    const fn = mode.value === 'create' ? createFn : updateFn
    if (!fn) {
      logger.warn(`useFormModal: 未配置 ${mode.value === 'create' ? 'createFn' : 'updateFn'}`)
      return
    }

    submitting.value = true
    try {
      await fn(formData.value)
      onSuccess?.(mode.value)
      close()
    } catch (err) {
      // Req 2.6: 失败时保持弹窗打开，通过 onError 回调通知
      const error = err instanceof Error ? err : new Error(String(err))
      logger.error('useFormModal 提交失败', error)
      onError?.(error)
    } finally {
      submitting.value = false
    }
  }

  return {
    visible,
    mode,
    formData,
    submitting,
    title,
    openCreate,
    openEdit,
    close,
    submit,
    resetForm
  }
}

import { ref, computed, type Ref } from 'vue'

/**
 * 历史记录项接口
 */
export interface HistoryEntry<T> {
  /** 状态快照 */
  state: T
  /** 操作描述 */
  description?: string
  /** 时间戳 */
  timestamp: number
}

/**
 * useHistory 配置选项
 */
export interface UseHistoryOptions {
  /** 最大历史记录数量，默认 50 */
  maxSize?: number
  /** 是否深拷贝状态，默认 true */
  deepClone?: boolean
}

/**
 * useHistory 返回值接口
 */
export interface UseHistoryReturn<T> {
  /** 是否可以撤销 */
  canUndo: Ref<boolean>
  /** 是否可以重做 */
  canRedo: Ref<boolean>
  /** 撤销栈长度 */
  undoStackSize: Ref<number>
  /** 重做栈长度 */
  redoStackSize: Ref<number>
  /** 当前状态 */
  currentState: Ref<T | null>
  /** 推入新状态 */
  push: (state: T, description?: string) => void
  /** 撤销操作 */
  undo: () => T | null
  /** 重做操作 */
  redo: () => T | null
  /** 清空历史记录 */
  clear: () => void
  /** 获取撤销栈 */
  getUndoStack: () => HistoryEntry<T>[]
  /** 获取重做栈 */
  getRedoStack: () => HistoryEntry<T>[]
}


/**
 * 深拷贝函数
 * 用于创建状态的独立副本，避免引用问题
 */
function deepClone<T>(obj: T): T {
  if (obj === null || typeof obj !== 'object') {
    return obj
  }
  
  if (obj instanceof Date) {
    return new Date(obj.getTime()) as unknown as T
  }
  
  if (Array.isArray(obj)) {
    return obj.map(item => deepClone(item)) as unknown as T
  }
  
  if (obj instanceof Object) {
    const copy = {} as T
    for (const key in obj) {
      if (Object.prototype.hasOwnProperty.call(obj, key)) {
        (copy as Record<string, unknown>)[key] = deepClone((obj as Record<string, unknown>)[key])
      }
    }
    return copy
  }
  
  return obj
}

/**
 * 操作历史管理 Composable
 * 
 * 提供撤销/重做功能，支持：
 * - 可配置的最大历史记录数量（默认 50）
 * - 状态快照的深拷贝
 * - 操作描述记录
 * - 时间戳追踪
 * 
 * @param options 配置选项
 * @returns 历史管理方法和状态
 * 
 * @example
 * ```typescript
 * const { canUndo, canRedo, push, undo, redo, clear } = useHistory<FormState>({ maxSize: 50 })
 * 
 * // 记录状态变更
 * push({ ...currentForm }, '修改字段配置')
 * 
 * // 撤销
 * if (canUndo.value) {
 *   const prevState = undo()
 *   if (prevState) applyState(prevState)
 * }
 * 
 * // 重做
 * if (canRedo.value) {
 *   const nextState = redo()
 *   if (nextState) applyState(nextState)
 * }
 * ```
 */
export function useHistory<T>(options: UseHistoryOptions = {}): UseHistoryReturn<T> {
  const { maxSize = 50, deepClone: shouldDeepClone = true } = options
  
  // 撤销栈：存储可以撤销的历史状态
  const undoStack = ref<HistoryEntry<T>[]>([]) as Ref<HistoryEntry<T>[]>
  
  // 重做栈：存储可以重做的状态
  const redoStack = ref<HistoryEntry<T>[]>([]) as Ref<HistoryEntry<T>[]>
  
  // 当前状态
  const currentState = ref<T | null>(null) as Ref<T | null>

  // 计算属性：是否可以撤销
  const canUndo = computed(() => undoStack.value.length > 0)
  
  // 计算属性：是否可以重做
  const canRedo = computed(() => redoStack.value.length > 0)
  
  // 计算属性：撤销栈长度
  const undoStackSize = computed(() => undoStack.value.length)
  
  // 计算属性：重做栈长度
  const redoStackSize = computed(() => redoStack.value.length)
  
  /**
   * 创建状态快照
   */
  const createSnapshot = (state: T): T => {
    return shouldDeepClone ? deepClone(state) : state
  }
  
  /**
   * 推入新状态到历史记录
   * 
   * @param state 新状态
   * @param description 操作描述（可选）
   */
  const push = (state: T, description?: string): void => {
    // 如果有当前状态，将其推入撤销栈
    if (currentState.value !== null) {
      const entry: HistoryEntry<T> = {
        state: createSnapshot(currentState.value),
        timestamp: Date.now()
      }
      if (description !== undefined) entry.description = description
      undoStack.value.push(entry)
      
      // 如果超过最大限制，移除最早的记录
      if (undoStack.value.length > maxSize) {
        undoStack.value.shift()
      }
    }
    
    // 更新当前状态
    currentState.value = createSnapshot(state)
    
    // 清空重做栈（新操作会使重做历史失效）
    redoStack.value = []
  }

  /**
   * 撤销操作
   * 
   * @returns 撤销后的状态，如果无法撤销则返回 null
   */
  const undo = (): T | null => {
    if (!canUndo.value) {
      return null
    }
    
    // 从撤销栈弹出最后一个状态
    const entry = undoStack.value.pop()
    if (!entry) {
      return null
    }
    
    // 将当前状态推入重做栈
    if (currentState.value !== null) {
      const redoEntry: HistoryEntry<T> = {
        state: createSnapshot(currentState.value),
        timestamp: Date.now()
      }
      if (entry.description !== undefined) redoEntry.description = entry.description
      redoStack.value.push(redoEntry)
    }
    
    // 更新当前状态为撤销后的状态
    currentState.value = createSnapshot(entry.state)
    
    // 返回状态的副本，避免外部修改影响内部状态
    return createSnapshot(currentState.value)
  }
  
  /**
   * 重做操作
   * 
   * @returns 重做后的状态，如果无法重做则返回 null
   */
  const redo = (): T | null => {
    if (!canRedo.value) {
      return null
    }
    
    // 从重做栈弹出最后一个状态
    const entry = redoStack.value.pop()
    if (!entry) {
      return null
    }
    
    // 将当前状态推入撤销栈
    if (currentState.value !== null) {
      const undoEntry: HistoryEntry<T> = {
        state: createSnapshot(currentState.value),
        timestamp: Date.now()
      }
      if (entry.description !== undefined) undoEntry.description = entry.description
      undoStack.value.push(undoEntry)
    }
    
    // 更新当前状态为重做后的状态
    currentState.value = createSnapshot(entry.state)
    
    // 返回状态的副本，避免外部修改影响内部状态
    return createSnapshot(currentState.value)
  }

  /**
   * 清空所有历史记录
   */
  const clear = (): void => {
    undoStack.value = []
    redoStack.value = []
    currentState.value = null
  }
  
  /**
   * 获取撤销栈的副本
   */
  const getUndoStack = (): HistoryEntry<T>[] => {
    return [...undoStack.value]
  }
  
  /**
   * 获取重做栈的副本
   */
  const getRedoStack = (): HistoryEntry<T>[] => {
    return [...redoStack.value]
  }
  
  return {
    canUndo,
    canRedo,
    undoStackSize,
    redoStackSize,
    currentState,
    push,
    undo,
    redo,
    clear,
    getUndoStack,
    getRedoStack
  }
}

export default useHistory

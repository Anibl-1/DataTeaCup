/**
 * 图表渲染优先级队列
 * Chart Render Priority Queue
 *
 * 按优先级渲染图表，优先渲染可视区域内的图表。
 * 使用 requestAnimationFrame / requestIdleCallback 实现非阻塞渲染。
 *
 * 需求 15.4: THE DataTeaCup SHALL 按优先级队列渲染图表，优先渲染可视区域内的图表
 */

/** 渲染优先级 */
export type RenderPriority = 'high' | 'medium' | 'low'

/** 优先级数值映射（越小越优先） */
const PRIORITY_ORDER: Record<RenderPriority, number> = {
  high: 0,
  medium: 1,
  low: 2,
}

/** 渲染任务 */
export interface RenderTask {
  /** 唯一标识 */
  id: string
  /** 优先级 */
  priority: RenderPriority
  /** 渲染函数 */
  render: () => Promise<void>
}

/** 内部任务（带入队顺序） */
interface InternalTask extends RenderTask {
  /** 入队序号，用于同优先级 FIFO 排序 */
  seq: number
}

/** 渲染队列配置 */
export interface RenderQueueOptions {
  /** 最大并发渲染数，默认 3 */
  maxConcurrent?: number
}

/** 默认最大并发数 */
const DEFAULT_MAX_CONCURRENT = 3

/**
 * 创建渲染优先级队列
 *
 * 特性：
 * - 按优先级排序（high > medium > low），同优先级 FIFO
 * - 支持动态调整任务优先级（可视区域进入时提升）
 * - 使用 requestAnimationFrame / requestIdleCallback 非阻塞调度
 * - 支持取消待处理任务
 */
export function createRenderQueue(options: RenderQueueOptions = {}) {
  const maxConcurrent = Math.max(1, options.maxConcurrent ?? DEFAULT_MAX_CONCURRENT)

  /** 待处理任务队列 */
  let pendingTasks: InternalTask[] = []
  /** 正在执行的任务 ID 集合 */
  const activeTasks = new Set<string>()
  /** 自增序号 */
  let seqCounter = 0
  /** 是否已调度下一轮处理 */
  let scheduled = false

  /**
   * 按优先级和入队顺序排序
   */
  function sortQueue(): void {
    pendingTasks.sort((a, b) => {
      const priorityDiff = PRIORITY_ORDER[a.priority] - PRIORITY_ORDER[b.priority]
      if (priorityDiff !== 0) return priorityDiff
      return a.seq - b.seq
    })
  }

  /**
   * 调度下一轮任务处理
   */
  function scheduleProcessing(): void {
    if (scheduled) return
    if (pendingTasks.length === 0) return
    if (activeTasks.size >= maxConcurrent) return

    scheduled = true

    // 优先使用 requestIdleCallback，降级到 requestAnimationFrame
    if (typeof requestIdleCallback === 'function') {
      requestIdleCallback(() => {
        scheduled = false
        processNext()
      })
    } else if (typeof requestAnimationFrame === 'function') {
      requestAnimationFrame(() => {
        scheduled = false
        processNext()
      })
    } else {
      // Fallback for environments without rAF (e.g., tests)
      Promise.resolve().then(() => {
        scheduled = false
        processNext()
      })
    }
  }

  /**
   * 处理下一批任务
   */
  function processNext(): void {
    while (activeTasks.size < maxConcurrent && pendingTasks.length > 0) {
      const task = pendingTasks.shift()!
      activeTasks.add(task.id)

      task
        .render()
        .catch(() => {
          // Silently handle render errors - the consumer should handle errors in their render fn
        })
        .finally(() => {
          activeTasks.delete(task.id)
          scheduleProcessing()
        })
    }
  }

  /**
   * 将任务加入队列
   */
  function enqueue(task: RenderTask): void {
    // 如果任务已在队列中，先移除旧的
    pendingTasks = pendingTasks.filter((t) => t.id !== task.id)

    // 如果任务正在执行中，不重复入队
    if (activeTasks.has(task.id)) return

    const internalTask: InternalTask = {
      ...task,
      seq: seqCounter++,
    }

    pendingTasks.push(internalTask)
    sortQueue()
    scheduleProcessing()
  }

  /**
   * 从队列中移除任务（取消待处理任务）
   */
  function dequeue(id: string): void {
    pendingTasks = pendingTasks.filter((t) => t.id !== id)
  }

  /**
   * 动态调整任务优先级
   * 典型场景：组件进入可视区域时提升为 high
   */
  function setPriority(id: string, priority: RenderPriority): void {
    const task = pendingTasks.find((t) => t.id === id)
    if (task) {
      task.priority = priority
      sortQueue()
      scheduleProcessing()
    }
  }

  /**
   * 获取当前是否正在处理任务
   */
  function isProcessing(): boolean {
    return activeTasks.size > 0
  }

  /**
   * 获取待处理队列大小
   */
  function getQueueSize(): number {
    return pendingTasks.length
  }

  /**
   * 获取正在执行的任务数
   */
  function getActiveCount(): number {
    return activeTasks.size
  }

  /**
   * 清空所有待处理任务
   */
  function clear(): void {
    pendingTasks = []
  }

  /**
   * 获取待处理任务 ID 列表（按当前排序顺序）
   */
  function getPendingIds(): string[] {
    return pendingTasks.map((t) => t.id)
  }

  return {
    enqueue,
    dequeue,
    setPriority,
    isProcessing,
    getQueueSize,
    getActiveCount,
    clear,
    getPendingIds,
  }
}

export type RenderQueue = ReturnType<typeof createRenderQueue>

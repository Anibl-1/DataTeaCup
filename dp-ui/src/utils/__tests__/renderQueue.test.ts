/**
 * 图表渲染优先级队列 - 单元测试
 * Render Queue Unit Tests
 *
 * 需求 15.4: THE DataTeaCup SHALL 按优先级队列渲染图表，优先渲染可视区域内的图表
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { createRenderQueue, type RenderPriority } from '../renderQueue'

// Helper: create a render task that resolves immediately
function makeTask(id: string, priority: RenderPriority, log?: string[]) {
  return {
    id,
    priority,
    render: vi.fn(async () => {
      log?.push(id)
    }),
  }
}

// Helper: flush microtasks + scheduled callbacks
async function flush(times = 5) {
  for (let i = 0; i < times; i++) {
    await new Promise((r) => setTimeout(r, 0))
  }
}

describe('createRenderQueue', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should create a queue with default options', () => {
    const queue = createRenderQueue()
    expect(queue.getQueueSize()).toBe(0)
    expect(queue.isProcessing()).toBe(false)
    expect(queue.getActiveCount()).toBe(0)
  })

  it('should enqueue and process a single task', async () => {
    vi.useRealTimers()
    const log: string[] = []
    const queue = createRenderQueue()

    queue.enqueue(makeTask('t1', 'high', log))
    await flush()

    expect(log).toContain('t1')
  })

  it('should process high priority tasks before low priority', async () => {
    vi.useRealTimers()
    const log: string[] = []
    const queue = createRenderQueue({ maxConcurrent: 1 })

    // Enqueue a blocking task first to hold the slot
    let resolveBlocker!: () => void
    const blocker = {
      id: 'blocker',
      priority: 'high' as RenderPriority,
      render: () => new Promise<void>((r) => { resolveBlocker = r }),
    }
    queue.enqueue(blocker)
    await flush()

    // Now enqueue low then high - high should run first after blocker
    queue.enqueue(makeTask('low1', 'low', log))
    queue.enqueue(makeTask('high1', 'high', log))

    // Release blocker
    resolveBlocker()
    await flush()

    expect(log.indexOf('high1')).toBeLessThan(log.indexOf('low1'))
  })

  it('should respect maxConcurrent limit', async () => {
    vi.useRealTimers()
    const queue = createRenderQueue({ maxConcurrent: 2 })
    let activeCount = 0
    let maxActive = 0

    const makeSlowTask = (id: string) => ({
      id,
      priority: 'high' as RenderPriority,
      render: async () => {
        activeCount++
        maxActive = Math.max(maxActive, activeCount)
        await new Promise((r) => setTimeout(r, 10))
        activeCount--
      },
    })

    queue.enqueue(makeSlowTask('t1'))
    queue.enqueue(makeSlowTask('t2'))
    queue.enqueue(makeSlowTask('t3'))
    queue.enqueue(makeSlowTask('t4'))

    await flush(20)
    await new Promise((r) => setTimeout(r, 100))

    expect(maxActive).toBeLessThanOrEqual(2)
  })

  it('should dequeue (cancel) a pending task', async () => {
    vi.useRealTimers()
    const log: string[] = []
    const queue = createRenderQueue({ maxConcurrent: 1 })

    // Block the slot
    let resolveBlocker!: () => void
    queue.enqueue({
      id: 'blocker',
      priority: 'high',
      render: () => new Promise<void>((r) => { resolveBlocker = r }),
    })
    await flush()

    // Enqueue then cancel
    queue.enqueue(makeTask('t1', 'medium', log))
    expect(queue.getQueueSize()).toBe(1)

    queue.dequeue('t1')
    expect(queue.getQueueSize()).toBe(0)

    resolveBlocker()
    await flush()

    expect(log).not.toContain('t1')
  })

  it('should allow setPriority to promote a task', async () => {
    vi.useRealTimers()
    const log: string[] = []
    const queue = createRenderQueue({ maxConcurrent: 1 })

    // Block the slot
    let resolveBlocker!: () => void
    queue.enqueue({
      id: 'blocker',
      priority: 'high',
      render: () => new Promise<void>((r) => { resolveBlocker = r }),
    })
    await flush()

    // Enqueue two tasks
    queue.enqueue(makeTask('low1', 'low', log))
    queue.enqueue(makeTask('low2', 'low', log))

    // Promote low2 to high
    queue.setPriority('low2', 'high')

    resolveBlocker()
    await flush()

    expect(log.indexOf('low2')).toBeLessThan(log.indexOf('low1'))
  })

  it('should clear all pending tasks', async () => {
    vi.useRealTimers()
    const queue = createRenderQueue({ maxConcurrent: 1 })

    // Block the slot
    queue.enqueue({
      id: 'blocker',
      priority: 'high',
      render: () => new Promise<void>(() => {}),
    })
    await flush()

    queue.enqueue(makeTask('t1', 'medium'))
    queue.enqueue(makeTask('t2', 'low'))
    expect(queue.getQueueSize()).toBe(2)

    queue.clear()
    expect(queue.getQueueSize()).toBe(0)
  })

  it('should return pending IDs in priority order', async () => {
    vi.useRealTimers()
    const queue = createRenderQueue({ maxConcurrent: 1 })

    // Block the slot
    queue.enqueue({
      id: 'blocker',
      priority: 'high',
      render: () => new Promise<void>(() => {}),
    })
    await flush()

    queue.enqueue(makeTask('low1', 'low'))
    queue.enqueue(makeTask('high1', 'high'))
    queue.enqueue(makeTask('med1', 'medium'))

    const ids = queue.getPendingIds()
    expect(ids).toEqual(['high1', 'med1', 'low1'])
  })

  it('should deduplicate tasks by id on re-enqueue', async () => {
    vi.useRealTimers()
    const queue = createRenderQueue({ maxConcurrent: 1 })

    // Block the slot
    queue.enqueue({
      id: 'blocker',
      priority: 'high',
      render: () => new Promise<void>(() => {}),
    })
    await flush()

    queue.enqueue(makeTask('t1', 'low'))
    queue.enqueue(makeTask('t1', 'high'))

    expect(queue.getQueueSize()).toBe(1)
    expect(queue.getPendingIds()).toEqual(['t1'])
  })

  it('should not re-enqueue a task that is currently active', async () => {
    vi.useRealTimers()
    const queue = createRenderQueue({ maxConcurrent: 1 })

    let resolveTask!: () => void
    queue.enqueue({
      id: 't1',
      priority: 'high',
      render: () => new Promise<void>((r) => { resolveTask = r }),
    })
    await flush()

    // t1 is now active, try to re-enqueue
    queue.enqueue(makeTask('t1', 'low'))
    expect(queue.getQueueSize()).toBe(0)

    resolveTask()
    await flush()
  })

  it('should handle render errors gracefully', async () => {
    vi.useRealTimers()
    const log: string[] = []
    const queue = createRenderQueue({ maxConcurrent: 1 })

    queue.enqueue({
      id: 'fail',
      priority: 'high',
      render: async () => { throw new Error('render failed') },
    })
    queue.enqueue(makeTask('t2', 'high', log))

    await flush(10)

    // t2 should still run after fail
    expect(log).toContain('t2')
  })

  it('should enforce maxConcurrent >= 1', () => {
    const queue = createRenderQueue({ maxConcurrent: 0 })
    // Should still work (clamped to 1)
    expect(queue.getQueueSize()).toBe(0)
  })

  it('should maintain FIFO order for same priority', async () => {
    vi.useRealTimers()
    const log: string[] = []
    const queue = createRenderQueue({ maxConcurrent: 1 })

    // Block the slot
    let resolveBlocker!: () => void
    queue.enqueue({
      id: 'blocker',
      priority: 'high',
      render: () => new Promise<void>((r) => { resolveBlocker = r }),
    })
    await flush()

    queue.enqueue(makeTask('m1', 'medium', log))
    queue.enqueue(makeTask('m2', 'medium', log))
    queue.enqueue(makeTask('m3', 'medium', log))

    resolveBlocker()
    await flush(15)

    expect(log).toEqual(['m1', 'm2', 'm3'])
  })
})

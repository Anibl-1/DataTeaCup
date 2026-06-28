/**
 * Render Queue Property-Based Tests
 * **属性 46: 图表渲染优先级**
 * **Validates: Requirements 15.4**
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { createRenderQueue, type RenderPriority } from '../renderQueue'

const PRIORITIES: RenderPriority[] = ['high', 'medium', 'low']
const PRIORITY_ORDER: Record<RenderPriority, number> = { high: 0, medium: 1, low: 2 }

async function flush(ms = 100): Promise<void> {
  for (let i = 0; i < 10; i++) { await new Promise<void>((r) => setTimeout(r, ms / 10)) }
}

const priorityArb = fc.constantFrom<RenderPriority>(...PRIORITIES)
const taskListArb = fc.integer({ min: 2, max: 15 }).chain((n) =>
  fc.array(priorityArb, { minLength: n, maxLength: n })
    .map((ps) => ps.map((p, i) => ({ id: `t-${i}`, priority: p })))
)
const mixedTaskListArb = fc.integer({ min: 1, max: 8 }).chain((extra) =>
  fc.array(priorityArb, { minLength: extra, maxLength: extra })
    .map((ps) => [
      { id: 't-0', priority: 'high' as RenderPriority },
      { id: 't-1', priority: 'low' as RenderPriority },
      ...ps.map((p, i) => ({ id: `t-${i + 2}`, priority: p })),
    ])
)

describe('属性 46: 图表渲染优先级', () => {
  it('待处理队列始终按优先级排序', async () => {
    await fc.assert(
      fc.asyncProperty(taskListArb, async (tasks) => {
        const queue = createRenderQueue({ maxConcurrent: 1 })
        queue.enqueue({ id: '__blocker__', priority: 'high', render: () => new Promise<void>(() => {}) })
        await flush(50)
        for (const t of tasks) {
          queue.enqueue({ id: t.id, priority: t.priority, render: async () => {} })
        }
        const ids = queue.getPendingIds()
        const taskMap = new Map(tasks.map((t) => [t.id, t.priority]))
        for (let i = 0; i < ids.length - 1; i++) {
          const a = taskMap.get(ids[i])
          const b = taskMap.get(ids[i + 1])
          if (a === undefined || b === undefined) continue
          expect(PRIORITY_ORDER[a]).toBeLessThanOrEqual(PRIORITY_ORDER[b])
        }
        queue.clear()
      }), { numRuns: 100 })
  }, 60000)
})

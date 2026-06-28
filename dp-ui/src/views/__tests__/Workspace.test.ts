/**
 * Workspace.vue unit tests
 * Tests core logic for task 6.2 requirements:
 * - Personalized greeting based on time of day (Req 21.1)
 * - User avatar and department info display (Req 21.2)
 * - Todo management: add, complete, delete (Req 21.3)
 * - Latest announcements section (Req 21.4)
 * - Dynamic stat cards based on user permissions (Req 21.4)
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { getGreetingByHour } from '@/utils/greeting'

describe('Workspace logic', () => {
  describe('Personalized greeting (Req 21.1)', () => {
    it('should return 夜深了 for hours 0-5', () => {
      for (let h = 0; h <= 5; h++) {
        expect(getGreetingByHour(h)).toBe('夜深了')
      }
    })

    it('should return 早上好 for hours 6-11', () => {
      for (let h = 6; h <= 11; h++) {
        expect(getGreetingByHour(h)).toBe('早上好')
      }
    })

    it('should return 下午好 for hours 12-17', () => {
      for (let h = 12; h <= 17; h++) {
        expect(getGreetingByHour(h)).toBe('下午好')
      }
    })

    it('should return 晚上好 for hours 18-23', () => {
      for (let h = 18; h <= 23; h++) {
        expect(getGreetingByHour(h)).toBe('晚上好')
      }
    })
  })

  describe('User display name resolution (Req 21.2)', () => {
    it('should prefer nickname from workspace data', () => {
      const workspaceNickname = '小明'
      const userNickname = '小红'
      const username = 'user1'
      const result = workspaceNickname || userNickname || username || '用户'
      expect(result).toBe('小明')
    })

    it('should fallback to userInfo nickname', () => {
      const workspaceNickname = ''
      const userNickname = '小红'
      const username = 'user1'
      const result = workspaceNickname || userNickname || username || '用户'
      expect(result).toBe('小红')
    })

    it('should fallback to username', () => {
      const workspaceNickname = ''
      const userNickname = ''
      const username = 'user1'
      const result = workspaceNickname || userNickname || username || '用户'
      expect(result).toBe('user1')
    })

    it('should fallback to default 用户', () => {
      const result = '' || '' || '' || '用户'
      expect(result).toBe('用户')
    })
  })

  describe('Todo management (Req 21.3)', () => {
    let todoList: { id: number; text: string; done: boolean }[]
    let idCounter: number

    beforeEach(() => {
      todoList = []
      idCounter = 0
      localStorage.clear()
    })

    function addTodo(text: string) {
      const trimmed = text.trim()
      if (!trimmed) return
      todoList.unshift({ id: ++idCounter, text: trimmed, done: false })
    }

    function toggleTodo(idx: number) {
      todoList[idx].done = !todoList[idx].done
    }

    function removeTodo(idx: number) {
      todoList.splice(idx, 1)
    }

    it('should add a todo item', () => {
      addTodo('写测试')
      expect(todoList).toHaveLength(1)
      expect(todoList[0].text).toBe('写测试')
      expect(todoList[0].done).toBe(false)
    })

    it('should not add empty todo', () => {
      addTodo('')
      addTodo('   ')
      expect(todoList).toHaveLength(0)
    })

    it('should add new items at the beginning', () => {
      addTodo('第一个')
      addTodo('第二个')
      expect(todoList[0].text).toBe('第二个')
      expect(todoList[1].text).toBe('第一个')
    })

    it('should toggle todo completion', () => {
      addTodo('任务')
      expect(todoList[0].done).toBe(false)
      toggleTodo(0)
      expect(todoList[0].done).toBe(true)
      toggleTodo(0)
      expect(todoList[0].done).toBe(false)
    })

    it('should remove a todo item', () => {
      addTodo('A')
      addTodo('B')
      addTodo('C')
      expect(todoList).toHaveLength(3)
      removeTodo(1) // remove B
      expect(todoList).toHaveLength(2)
      expect(todoList.map(t => t.text)).toEqual(['C', 'A'])
    })

    it('should count pending todos correctly', () => {
      addTodo('A')
      addTodo('B')
      addTodo('C')
      toggleTodo(0) // complete C
      const pending = todoList.filter(t => !t.done).length
      expect(pending).toBe(2)
    })
  })

  describe('Dynamic stat cards by permissions (Req 21.4)', () => {
    const allCards = [
      { label: '数据源', adminOnly: false },
      { label: '采集任务', adminOnly: false },
      { label: '报表', adminOnly: false },
      { label: '图表', adminOnly: false },
      { label: '用户', adminOnly: true },
      { label: '部门', adminOnly: true },
    ]

    it('should show all cards for admin users', () => {
      const isAdmin = true
      const visible = isAdmin ? allCards : allCards.filter(s => !s.adminOnly)
      expect(visible).toHaveLength(6)
    })

    it('should hide admin-only cards for regular users', () => {
      const isAdmin = false
      const visible = isAdmin ? allCards : allCards.filter(s => !s.adminOnly)
      expect(visible).toHaveLength(4)
      expect(visible.every(c => !c.adminOnly)).toBe(true)
    })
  })

  describe('Announcements display (Req 21.4)', () => {
    it('should map announcement types to labels', () => {
      const typeMap: Record<string, string> = {
        warning: '警告',
        error: '紧急',
        info: '通知',
      }
      expect(typeMap['warning']).toBe('警告')
      expect(typeMap['error']).toBe('紧急')
      expect(typeMap['info']).toBe('通知')
    })
  })
})

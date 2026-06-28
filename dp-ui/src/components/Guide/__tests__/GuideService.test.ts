/**
 * GuideService 单元测试
 * GuideService Unit Tests
 * 
 * 测试功能引导服务的核心功能。
 * 
 * 需求 19.1: WHEN 用户首次使用某功能时，THE DataTeaCup SHALL 显示功能引导教程
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { GuideService, createGuideService } from '../GuideService'
import type { GuideConfig, GuideStep } from '../types'

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: vi.fn((key: string) => store[key] || null),
    setItem: vi.fn((key: string, value: string) => {
      store[key] = value
    }),
    removeItem: vi.fn((key: string) => {
      delete store[key]
    }),
    clear: vi.fn(() => {
      store = {}
    })
  }
})()

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock
})

// 测试用引导配置
const createTestGuide = (id: string, triggerOnce = true): GuideConfig => ({
  id,
  name: `Test Guide ${id}`,
  triggerOnce,
  steps: [
    {
      target: '.step-1',
      title: 'Step 1',
      content: 'Content 1',
      placement: 'bottom'
    },
    {
      target: '.step-2',
      title: 'Step 2',
      content: 'Content 2',
      placement: 'right'
    },
    {
      target: '.step-3',
      title: 'Step 3',
      content: 'Content 3',
      placement: 'top'
    }
  ]
})

describe('GuideService', () => {
  let service: GuideService

  beforeEach(() => {
    localStorageMock.clear()
    service = createGuideService()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('registerGuide', () => {
    it('should register a valid guide configuration', () => {
      const guide = createTestGuide('test-guide')
      service.registerGuide(guide)
      
      expect(service.getGuide('test-guide')).toEqual(guide)
    })

    it('should not register guide with empty id', () => {
      const guide = { ...createTestGuide(''), id: '' }
      service.registerGuide(guide)
      
      expect(service.getGuide('')).toBeUndefined()
    })

    it('should not register guide with empty steps', () => {
      const guide = { ...createTestGuide('test'), steps: [] }
      service.registerGuide(guide)
      
      expect(service.getGuide('test')).toBeUndefined()
    })

    it('should register multiple guides', () => {
      const guide1 = createTestGuide('guide-1')
      const guide2 = createTestGuide('guide-2')
      
      service.registerGuides([guide1, guide2])
      
      expect(service.getGuide('guide-1')).toEqual(guide1)
      expect(service.getGuide('guide-2')).toEqual(guide2)
    })
  })

  describe('start', () => {
    it('should start a registered guide', () => {
      const guide = createTestGuide('test-guide')
      service.registerGuide(guide)
      
      service.start('test-guide')
      
      const state = service.getState()
      expect(state.isActive).toBe(true)
      expect(state.currentGuideId).toBe('test-guide')
      expect(state.currentStepIndex).toBe(0)
    })

    it('should not start an unregistered guide', () => {
      service.start('non-existent')
      
      const state = service.getState()
      expect(state.isActive).toBe(false)
      expect(state.currentGuideId).toBeNull()
    })

    it('should not start a completed guide with triggerOnce=true', () => {
      const guide = createTestGuide('test-guide', true)
      service.registerGuide(guide)
      service.markCompleted('test-guide')
      
      service.start('test-guide')
      
      const state = service.getState()
      expect(state.isActive).toBe(false)
    })

    it('should start a completed guide with triggerOnce=false', () => {
      const guide = createTestGuide('test-guide', false)
      service.registerGuide(guide)
      service.markCompleted('test-guide')
      
      service.start('test-guide')
      
      const state = service.getState()
      expect(state.isActive).toBe(true)
    })
  })

  describe('navigation', () => {
    beforeEach(() => {
      const guide = createTestGuide('test-guide')
      service.registerGuide(guide)
      service.start('test-guide')
    })

    it('should navigate to next step', () => {
      service.next()
      
      expect(service.getState().currentStepIndex).toBe(1)
    })

    it('should navigate to previous step', () => {
      service.next()
      service.prev()
      
      expect(service.getState().currentStepIndex).toBe(0)
    })

    it('should not go before first step', () => {
      service.prev()
      
      expect(service.getState().currentStepIndex).toBe(0)
    })

    it('should complete guide on last step next', () => {
      service.next() // step 1
      service.next() // step 2
      service.next() // complete
      
      const state = service.getState()
      expect(state.isActive).toBe(false)
      expect(service.isCompleted('test-guide')).toBe(true)
    })

    it('should go to specific step', () => {
      service.goToStep(2)
      
      expect(service.getState().currentStepIndex).toBe(2)
    })

    it('should not go to invalid step index', () => {
      service.goToStep(10)
      
      expect(service.getState().currentStepIndex).toBe(0)
    })
  })

  describe('skip', () => {
    it('should skip current guide without marking as completed', () => {
      const guide = createTestGuide('test-guide')
      service.registerGuide(guide)
      service.start('test-guide')
      
      service.skip()
      
      const state = service.getState()
      expect(state.isActive).toBe(false)
      expect(service.isCompleted('test-guide')).toBe(false)
    })
  })

  describe('complete', () => {
    it('should complete guide and mark as completed', () => {
      const guide = createTestGuide('test-guide')
      service.registerGuide(guide)
      service.start('test-guide')
      
      service.complete()
      
      const state = service.getState()
      expect(state.isActive).toBe(false)
      expect(service.isCompleted('test-guide')).toBe(true)
    })

    it('should persist completed guides to localStorage', () => {
      const guide = createTestGuide('test-guide')
      service.registerGuide(guide)
      service.start('test-guide')
      service.complete()
      
      expect(localStorageMock.setItem).toHaveBeenCalledWith(
        'datateacup_completed_guides',
        JSON.stringify(['test-guide'])
      )
    })
  })

  describe('isCompleted', () => {
    it('should return false for uncompleted guide', () => {
      expect(service.isCompleted('test-guide')).toBe(false)
    })

    it('should return true for completed guide', () => {
      service.markCompleted('test-guide')
      
      expect(service.isCompleted('test-guide')).toBe(true)
    })
  })

  describe('shouldShowGuide', () => {
    it('should return true for unregistered guide', () => {
      expect(service.shouldShowGuide('non-existent')).toBe(false)
    })

    it('should return true for uncompleted guide with triggerOnce=true', () => {
      const guide = createTestGuide('test-guide', true)
      service.registerGuide(guide)
      
      expect(service.shouldShowGuide('test-guide')).toBe(true)
    })

    it('should return false for completed guide with triggerOnce=true', () => {
      const guide = createTestGuide('test-guide', true)
      service.registerGuide(guide)
      service.markCompleted('test-guide')
      
      expect(service.shouldShowGuide('test-guide')).toBe(false)
    })

    it('should return true for completed guide with triggerOnce=false', () => {
      const guide = createTestGuide('test-guide', false)
      service.registerGuide(guide)
      service.markCompleted('test-guide')
      
      expect(service.shouldShowGuide('test-guide')).toBe(true)
    })
  })

  describe('resetGuide', () => {
    it('should reset a completed guide', () => {
      service.markCompleted('test-guide')
      expect(service.isCompleted('test-guide')).toBe(true)
      
      service.resetGuide('test-guide')
      
      expect(service.isCompleted('test-guide')).toBe(false)
    })
  })

  describe('resetAllGuides', () => {
    it('should reset all completed guides', () => {
      service.markCompleted('guide-1')
      service.markCompleted('guide-2')
      
      service.resetAllGuides()
      
      expect(service.isCompleted('guide-1')).toBe(false)
      expect(service.isCompleted('guide-2')).toBe(false)
    })

    it('should stop active guide when resetting all', () => {
      const guide = createTestGuide('test-guide')
      service.registerGuide(guide)
      service.start('test-guide')
      
      service.resetAllGuides()
      
      expect(service.getState().isActive).toBe(false)
    })
  })

  describe('getProgress', () => {
    it('should return correct progress', () => {
      const guide = createTestGuide('test-guide')
      service.registerGuide(guide)
      service.start('test-guide')
      
      let progress = service.getProgress()
      expect(progress.current).toBe(1)
      expect(progress.total).toBe(3)
      expect(progress.percentage).toBe(33)
      
      service.next()
      progress = service.getProgress()
      expect(progress.current).toBe(2)
      expect(progress.percentage).toBe(67)
    })

    it('should return zero progress when no guide is active', () => {
      const progress = service.getProgress()
      
      expect(progress.current).toBe(0)
      expect(progress.total).toBe(0)
      expect(progress.percentage).toBe(0)
    })
  })

  describe('getCurrentStep', () => {
    it('should return current step', () => {
      const guide = createTestGuide('test-guide')
      service.registerGuide(guide)
      service.start('test-guide')
      
      const step = service.getCurrentStep()
      
      expect(step).toEqual(guide.steps[0])
    })

    it('should return undefined when no guide is active', () => {
      expect(service.getCurrentStep()).toBeUndefined()
    })
  })

  describe('step action callback', () => {
    it('should execute step action on next', () => {
      const actionMock = vi.fn()
      const guide: GuideConfig = {
        id: 'test-guide',
        name: 'Test',
        triggerOnce: true,
        steps: [
          {
            target: '.step-1',
            title: 'Step 1',
            content: 'Content 1',
            placement: 'bottom',
            action: actionMock
          },
          {
            target: '.step-2',
            title: 'Step 2',
            content: 'Content 2',
            placement: 'bottom'
          }
        ]
      }
      
      service.registerGuide(guide)
      service.start('test-guide')
      service.next()
      
      expect(actionMock).toHaveBeenCalledTimes(1)
    })
  })

  describe('tryStart', () => {
    it('should start guide and return true if should show', () => {
      const guide = createTestGuide('test-guide')
      service.registerGuide(guide)
      
      const result = service.tryStart('test-guide')
      
      expect(result).toBe(true)
      expect(service.getState().isActive).toBe(true)
    })

    it('should not start guide and return false if already completed', () => {
      const guide = createTestGuide('test-guide', true)
      service.registerGuide(guide)
      service.markCompleted('test-guide')
      
      const result = service.tryStart('test-guide')
      
      expect(result).toBe(false)
      expect(service.getState().isActive).toBe(false)
    })
  })

  describe('state change callback', () => {
    it('should call onStateChange callback when state changes', () => {
      const callback = vi.fn()
      service.setOnStateChange(callback)
      
      const guide = createTestGuide('test-guide')
      service.registerGuide(guide)
      service.start('test-guide')
      
      expect(callback).toHaveBeenCalled()
    })
  })
})


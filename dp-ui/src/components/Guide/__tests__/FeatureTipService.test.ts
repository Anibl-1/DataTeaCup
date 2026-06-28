/**
 * FeatureTipService 单元测试
 * 
 * 需求 19.2: THE DataTeaCup SHALL 为复杂功能提供上下文相关的使用提示
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { 
  FeatureTipService, 
  createFeatureTipService,
  useFeatureTip
} from '../FeatureTipService'
import type { FeatureTipConfig } from '../featureTipTypes'

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

describe('FeatureTipService', () => {
  let service: FeatureTipService

  beforeEach(() => {
    localStorageMock.clear()
    vi.clearAllMocks()
    service = createFeatureTipService()
  })

  describe('registerTip', () => {
    it('should register a valid tip configuration', () => {
      const tipConfig: FeatureTipConfig = {
        id: 'test-tip',
        title: 'Test Tip',
        content: 'This is a test tip',
        type: 'info',
        dismissible: true
      }

      service.registerTip(tipConfig)
      
      const tip = service.getTip('test-tip')
      expect(tip).toEqual(tipConfig)
    })

    it('should not register tip without id', () => {
      const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})
      
      service.registerTip({
        id: '',
        title: 'Test',
        content: 'Content',
        type: 'info',
        dismissible: false
      })

      expect(service.getTip('')).toBeUndefined()
      expect(consoleSpy).toHaveBeenCalled()
      
      consoleSpy.mockRestore()
    })

    it('should not register tip without content', () => {
      const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})
      
      service.registerTip({
        id: 'test',
        title: 'Test',
        content: '',
        type: 'info',
        dismissible: false
      })

      expect(service.getTip('test')).toBeUndefined()
      expect(consoleSpy).toHaveBeenCalled()
      
      consoleSpy.mockRestore()
    })
  })

  describe('registerTips', () => {
    it('should register multiple tips', () => {
      const tips: FeatureTipConfig[] = [
        { id: 'tip1', title: 'Tip 1', content: 'Content 1', type: 'info', dismissible: true },
        { id: 'tip2', title: 'Tip 2', content: 'Content 2', type: 'warning', dismissible: false },
        { id: 'tip3', title: 'Tip 3', content: 'Content 3', type: 'tip', dismissible: true }
      ]

      service.registerTips(tips)

      expect(service.getTip('tip1')).toBeDefined()
      expect(service.getTip('tip2')).toBeDefined()
      expect(service.getTip('tip3')).toBeDefined()
      expect(service.getAllTips()).toHaveLength(3)
    })
  })

  describe('getTipsByFeature', () => {
    it('should return tips for a specific feature', () => {
      const tips: FeatureTipConfig[] = [
        { id: 'tip1', title: 'Tip 1', content: 'Content 1', type: 'info', dismissible: true, feature: 'sql-editor' },
        { id: 'tip2', title: 'Tip 2', content: 'Content 2', type: 'info', dismissible: true, feature: 'sql-editor' },
        { id: 'tip3', title: 'Tip 3', content: 'Content 3', type: 'info', dismissible: true, feature: 'chart-designer' }
      ]

      service.registerTips(tips)

      const sqlEditorTips = service.getTipsByFeature('sql-editor')
      expect(sqlEditorTips).toHaveLength(2)
      expect(sqlEditorTips.every(t => t.feature === 'sql-editor')).toBe(true)
    })

    it('should return empty array for unknown feature', () => {
      const tips = service.getTipsByFeature('unknown-feature')
      expect(tips).toHaveLength(0)
    })
  })

  describe('dismiss', () => {
    it('should dismiss a tip and persist to localStorage', () => {
      service.dismiss('test-tip')

      expect(service.isDismissed('test-tip')).toBe(true)
      expect(localStorageMock.setItem).toHaveBeenCalledWith(
        'datateacup_dismissed_tips',
        JSON.stringify(['test-tip'])
      )
    })

    it('should not duplicate dismissed tips', () => {
      service.dismiss('test-tip')
      service.dismiss('test-tip')

      expect(service.getDismissedTips()).toEqual(['test-tip'])
    })

    it('should dismiss multiple tips', () => {
      service.dismiss('tip1')
      service.dismiss('tip2')
      service.dismiss('tip3')

      expect(service.isDismissed('tip1')).toBe(true)
      expect(service.isDismissed('tip2')).toBe(true)
      expect(service.isDismissed('tip3')).toBe(true)
      expect(service.getDismissedTips()).toHaveLength(3)
    })
  })

  describe('isDismissed', () => {
    it('should return false for non-dismissed tip', () => {
      expect(service.isDismissed('test-tip')).toBe(false)
    })

    it('should return true for dismissed tip', () => {
      service.dismiss('test-tip')
      expect(service.isDismissed('test-tip')).toBe(true)
    })
  })

  describe('reset', () => {
    it('should reset a dismissed tip', () => {
      service.dismiss('test-tip')
      expect(service.isDismissed('test-tip')).toBe(true)

      service.reset('test-tip')
      expect(service.isDismissed('test-tip')).toBe(false)
    })

    it('should persist reset to localStorage', () => {
      service.dismiss('tip1')
      service.dismiss('tip2')
      service.reset('tip1')

      expect(localStorageMock.setItem).toHaveBeenLastCalledWith(
        'datateacup_dismissed_tips',
        JSON.stringify(['tip2'])
      )
    })

    it('should do nothing for non-dismissed tip', () => {
      const initialCallCount = localStorageMock.setItem.mock.calls.length
      service.reset('non-existent-tip')
      expect(localStorageMock.setItem.mock.calls.length).toBe(initialCallCount)
    })
  })

  describe('resetAll', () => {
    it('should reset all dismissed tips', () => {
      service.dismiss('tip1')
      service.dismiss('tip2')
      service.dismiss('tip3')

      service.resetAll()

      expect(service.isDismissed('tip1')).toBe(false)
      expect(service.isDismissed('tip2')).toBe(false)
      expect(service.isDismissed('tip3')).toBe(false)
      expect(service.getDismissedTips()).toHaveLength(0)
    })

    it('should persist reset to localStorage', () => {
      service.dismiss('tip1')
      service.resetAll()

      expect(localStorageMock.setItem).toHaveBeenLastCalledWith(
        'datateacup_dismissed_tips',
        JSON.stringify([])
      )
    })
  })

  describe('state change callback', () => {
    it('should call callback on dismiss', () => {
      const callback = vi.fn()
      service.setOnStateChange(callback)

      service.dismiss('test-tip')

      expect(callback).toHaveBeenCalledWith({
        dismissedTips: ['test-tip']
      })
    })

    it('should call callback on reset', () => {
      const callback = vi.fn()
      service.dismiss('test-tip')
      service.setOnStateChange(callback)

      service.reset('test-tip')

      expect(callback).toHaveBeenCalledWith({
        dismissedTips: []
      })
    })

    it('should call callback on resetAll', () => {
      const callback = vi.fn()
      service.dismiss('tip1')
      service.dismiss('tip2')
      service.setOnStateChange(callback)

      service.resetAll()

      expect(callback).toHaveBeenCalledWith({
        dismissedTips: []
      })
    })
  })

  describe('localStorage persistence', () => {
    it('should load dismissed tips from localStorage on init', () => {
      localStorageMock.getItem.mockReturnValueOnce(JSON.stringify(['tip1', 'tip2']))
      
      const newService = createFeatureTipService()
      
      expect(newService.isDismissed('tip1')).toBe(true)
      expect(newService.isDismissed('tip2')).toBe(true)
    })

    it('should handle invalid localStorage data gracefully', () => {
      const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})
      localStorageMock.getItem.mockReturnValueOnce('invalid json')
      
      const newService = createFeatureTipService()
      
      expect(newService.getDismissedTips()).toEqual([])
      
      consoleSpy.mockRestore()
    })

    it('should handle non-array localStorage data gracefully', () => {
      localStorageMock.getItem.mockReturnValueOnce(JSON.stringify({ not: 'array' }))
      
      const newService = createFeatureTipService()
      
      expect(newService.getDismissedTips()).toEqual([])
    })
  })
})

describe('useFeatureTip composable', () => {
  beforeEach(() => {
    localStorageMock.clear()
    vi.clearAllMocks()
  })

  it('should provide dismiss and isDismissed functions', () => {
    const { dismiss, isDismissed } = useFeatureTip()

    expect(isDismissed('test-tip')).toBe(false)
    
    dismiss('test-tip')
    
    expect(isDismissed('test-tip')).toBe(true)
  })

  it('should provide reset function', () => {
    const { dismiss, reset, isDismissed } = useFeatureTip()

    dismiss('test-tip')
    expect(isDismissed('test-tip')).toBe(true)

    reset('test-tip')
    expect(isDismissed('test-tip')).toBe(false)
  })

  it('should provide resetAll function', () => {
    const { dismiss, resetAll, isDismissed } = useFeatureTip()

    dismiss('tip1')
    dismiss('tip2')

    resetAll()

    expect(isDismissed('tip1')).toBe(false)
    expect(isDismissed('tip2')).toBe(false)
  })

  it('should provide tip registration functions', () => {
    const { registerTip, getTip } = useFeatureTip()

    const tipConfig: FeatureTipConfig = {
      id: 'test-tip',
      title: 'Test',
      content: 'Content',
      type: 'info',
      dismissible: true
    }

    registerTip(tipConfig)

    expect(getTip('test-tip')).toEqual(tipConfig)
  })
})


/**
 * 首次使用引导属性测试
 * First-Use Guide Property Tests
 * 
 * Property 60: 首次使用引导触发
 * 
 * **Validates: Requirements 19.1**
 * WHEN 用户首次使用某功能时，THE DataTeaCup SHALL 显示功能引导教程
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import * as fc from 'fast-check'
import { GuideService, createGuideService } from '../GuideService'
import type { GuideConfig, GuideStep, GuidePlacement } from '../types'

// ==================== Mock localStorage ====================

const createLocalStorageMock = () => {
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
    }),
    getStore: () => ({ ...store })
  }
}

let localStorageMock = createLocalStorageMock()

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock,
  writable: true
})

// ==================== Arbitraries ====================

/** Arbitrary for valid guide IDs */
const guideIdArb = fc.stringMatching(/^[a-z][a-z0-9-]{2,29}$/)

/** Arbitrary for guide placement */
const placementArb: fc.Arbitrary<GuidePlacement> = fc.constantFrom(
  'top', 'bottom', 'left', 'right', 'center'
)

/** Arbitrary for a single guide step */
const guideStepArb: fc.Arbitrary<GuideStep> = fc.record({
  target: fc.stringMatching(/^\.[a-z][a-z0-9-]{0,19}$/),
  title: fc.string({ minLength: 1, maxLength: 50 }),
  content: fc.string({ minLength: 1, maxLength: 200 }),
  placement: placementArb,
  allowInteraction: fc.boolean(),
  highlightPadding: fc.integer({ min: 0, max: 20 })
})

/** Arbitrary for guide steps array (1-10 steps) */
const guideStepsArb = fc.array(guideStepArb, { minLength: 1, maxLength: 10 })

/** Arbitrary for a complete guide configuration */
const guideConfigArb: fc.Arbitrary<GuideConfig> = fc.record({
  id: guideIdArb,
  name: fc.string({ minLength: 1, maxLength: 50 }),
  steps: guideStepsArb,
  triggerOnce: fc.boolean(),
  description: fc.option(fc.string({ minLength: 1, maxLength: 100 }), { nil: undefined }),
  feature: fc.option(fc.string({ minLength: 1, maxLength: 30 }), { nil: undefined })
})

/** Arbitrary for guide config with triggerOnce=true */
const triggerOnceGuideArb = guideConfigArb.map(config => ({
  ...config,
  triggerOnce: true
}))

/** Arbitrary for guide config with triggerOnce=false */
const repeatableGuideArb = guideConfigArb.map(config => ({
  ...config,
  triggerOnce: false
}))

// ==================== Property Tests ====================

describe('Feature: platform-deep-optimization, Property 60: 首次使用引导触发', () => {
  let service: GuideService

  beforeEach(() => {
    localStorageMock = createLocalStorageMock()
    Object.defineProperty(window, 'localStorage', {
      value: localStorageMock,
      writable: true
    })
    service = createGuideService()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  /**
   * Property 60: 首次使用引导触发
   * 
   * **Validates: Requirements 19.1**
   * 
   * WHEN 用户首次使用某功能时，THE DataTeaCup SHALL 显示功能引导教程
   */
  describe('Property 60.1: 首次使用时显示引导', () => {
    it('should always show guide on first use for any valid guide configuration', () => {
      fc.assert(
        fc.property(guideConfigArb, (guideConfig) => {
          // Reset service for each test
          service = createGuideService()
          
          // Register the guide
          service.registerGuide(guideConfig)
          
          // Property: shouldShowGuide should return true for first use
          expect(service.shouldShowGuide(guideConfig.id)).toBe(true)
          
          // Property: tryStart should succeed and return true
          const started = service.tryStart(guideConfig.id)
          expect(started).toBe(true)
          
          // Property: Guide should be active after first start
          const state = service.getState()
          expect(state.isActive).toBe(true)
          expect(state.currentGuideId).toBe(guideConfig.id)
          expect(state.currentStepIndex).toBe(0)
        }),
        { numRuns: 100 }
      )
    })

    it('should show guide on first use regardless of triggerOnce setting', () => {
      fc.assert(
        fc.property(
          guideConfigArb,
          fc.boolean(),
          (baseConfig, triggerOnce) => {
            service = createGuideService()
            const guideConfig = { ...baseConfig, triggerOnce }
            
            service.registerGuide(guideConfig)
            
            // Property: First use should always show guide
            expect(service.shouldShowGuide(guideConfig.id)).toBe(true)
            
            const started = service.tryStart(guideConfig.id)
            expect(started).toBe(true)
            expect(service.getState().isActive).toBe(true)
          }
        ),
        { numRuns: 50 }
      )
    })
  })

  describe('Property 60.2: 完成后不再显示（triggerOnce=true）', () => {
    it('should not show guide after completion when triggerOnce is true', () => {
      fc.assert(
        fc.property(triggerOnceGuideArb, (guideConfig) => {
          service = createGuideService()
          service.registerGuide(guideConfig)
          
          // Start and complete the guide
          service.start(guideConfig.id)
          expect(service.getState().isActive).toBe(true)
          
          service.complete()
          
          // Property: Guide should be marked as completed
          expect(service.isCompleted(guideConfig.id)).toBe(true)
          
          // Property: shouldShowGuide should return false after completion
          expect(service.shouldShowGuide(guideConfig.id)).toBe(false)
          
          // Property: tryStart should fail and return false
          const startedAgain = service.tryStart(guideConfig.id)
          expect(startedAgain).toBe(false)
          expect(service.getState().isActive).toBe(false)
        }),
        { numRuns: 50 }
      )
    })

    it('should persist completion state across service instances', () => {
      fc.assert(
        fc.property(triggerOnceGuideArb, (guideConfig) => {
          // First service instance
          const service1 = createGuideService()
          service1.registerGuide(guideConfig)
          service1.start(guideConfig.id)
          service1.complete()
          
          // Property: Completion should be persisted to localStorage
          expect(localStorageMock.setItem).toHaveBeenCalled()
          
          // Simulate new service instance (e.g., page reload)
          const service2 = createGuideService()
          service2.registerGuide(guideConfig)
          
          // Property: New instance should recognize guide as completed
          expect(service2.isCompleted(guideConfig.id)).toBe(true)
          expect(service2.shouldShowGuide(guideConfig.id)).toBe(false)
        }),
        { numRuns: 30 }
      )
    })
  })

  describe('Property 60.3: 可重复显示（triggerOnce=false）', () => {
    it('should show guide after completion when triggerOnce is false', () => {
      fc.assert(
        fc.property(repeatableGuideArb, (guideConfig) => {
          service = createGuideService()
          service.registerGuide(guideConfig)
          
          // Complete the guide
          service.start(guideConfig.id)
          service.complete()
          
          // Property: Guide should be marked as completed
          expect(service.isCompleted(guideConfig.id)).toBe(true)
          
          // Property: shouldShowGuide should still return true
          expect(service.shouldShowGuide(guideConfig.id)).toBe(true)
          
          // Property: Can start the guide again
          const startedAgain = service.tryStart(guideConfig.id)
          expect(startedAgain).toBe(true)
          expect(service.getState().isActive).toBe(true)
        }),
        { numRuns: 50 }
      )
    })

    it('should allow multiple completions for repeatable guides', () => {
      fc.assert(
        fc.property(
          repeatableGuideArb,
          fc.integer({ min: 2, max: 5 }),
          (guideConfig, repeatCount) => {
            service = createGuideService()
            service.registerGuide(guideConfig)
            
            for (let i = 0; i < repeatCount; i++) {
              // Property: Should be able to start each time
              expect(service.shouldShowGuide(guideConfig.id)).toBe(true)
              
              service.start(guideConfig.id)
              expect(service.getState().isActive).toBe(true)
              
              service.complete()
              expect(service.getState().isActive).toBe(false)
            }
          }
        ),
        { numRuns: 20 }
      )
    })
  })

  describe('Property 60.4: 完成状态持久化', () => {
    it('should correctly persist and restore completion state', () => {
      fc.assert(
        fc.property(
          fc.array(triggerOnceGuideArb, { minLength: 1, maxLength: 5 })
            .filter(guides => new Set(guides.map(g => g.id)).size === guides.length),
          (guides) => {
            // Complete all guides
            const service1 = createGuideService()
            guides.forEach(guide => {
              service1.registerGuide(guide)
              service1.start(guide.id)
              service1.complete()
            })
            
            // Property: All guides should be completed
            guides.forEach(guide => {
              expect(service1.isCompleted(guide.id)).toBe(true)
            })
            
            // Create new service instance
            const service2 = createGuideService()
            guides.forEach(guide => {
              service2.registerGuide(guide)
            })
            
            // Property: All completion states should be restored
            guides.forEach(guide => {
              expect(service2.isCompleted(guide.id)).toBe(true)
              expect(service2.shouldShowGuide(guide.id)).toBe(false)
            })
          }
        ),
        { numRuns: 20 }
      )
    })

    it('should handle localStorage errors gracefully', () => {
      fc.assert(
        fc.property(guideConfigArb, (guideConfig) => {
          // Simulate localStorage error
          localStorageMock.getItem.mockImplementation(() => {
            throw new Error('localStorage error')
          })
          
          // Property: Service should still work with empty completed list
          const service = createGuideService()
          service.registerGuide(guideConfig)
          
          expect(service.isCompleted(guideConfig.id)).toBe(false)
          expect(service.shouldShowGuide(guideConfig.id)).toBe(true)
        }),
        { numRuns: 20 }
      )
    })

    it('should handle invalid localStorage data gracefully', () => {
      fc.assert(
        fc.property(
          guideConfigArb,
          fc.oneof(
            fc.constant('invalid json'),
            fc.constant('null'),
            fc.constant('{}'),
            fc.constant('123'),
            fc.constant('"string"')
          ),
          (guideConfig, invalidData) => {
            localStorageMock.getItem.mockReturnValue(invalidData)
            
            // Property: Service should handle invalid data gracefully
            const service = createGuideService()
            service.registerGuide(guideConfig)
            
            // Should treat as no completed guides
            expect(service.shouldShowGuide(guideConfig.id)).toBe(true)
          }
        ),
        { numRuns: 20 }
      )
    })
  })

  describe('Property 60.5: 步骤导航正确性', () => {
    it('should navigate through all steps correctly', () => {
      fc.assert(
        fc.property(guideConfigArb, (guideConfig) => {
          service = createGuideService()
          service.registerGuide(guideConfig)
          service.start(guideConfig.id)
          
          const totalSteps = guideConfig.steps.length
          
          // Property: Should start at step 0
          expect(service.getState().currentStepIndex).toBe(0)
          
          // Navigate through all steps
          for (let i = 0; i < totalSteps - 1; i++) {
            const currentStep = service.getCurrentStep()
            
            // Property: Current step should match expected step
            expect(currentStep).toEqual(guideConfig.steps[i])
            
            // Property: Progress should be accurate
            const progress = service.getProgress()
            expect(progress.current).toBe(i + 1)
            expect(progress.total).toBe(totalSteps)
            
            service.next()
            
            // Property: Step index should increment
            expect(service.getState().currentStepIndex).toBe(i + 1)
          }
          
          // Property: Last step should be correct
          expect(service.getCurrentStep()).toEqual(guideConfig.steps[totalSteps - 1])
          
          // Property: Next on last step should complete
          service.next()
          expect(service.getState().isActive).toBe(false)
          expect(service.isCompleted(guideConfig.id)).toBe(true)
        }),
        { numRuns: 30 }
      )
    })

    it('should navigate backwards correctly', () => {
      fc.assert(
        fc.property(
          guideConfigArb.filter(g => g.steps.length >= 2),
          fc.integer({ min: 1, max: 9 }),
          (guideConfig, forwardSteps) => {
            service = createGuideService()
            service.registerGuide(guideConfig)
            service.start(guideConfig.id)
            
            const maxForward = Math.min(forwardSteps, guideConfig.steps.length - 1)
            
            // Go forward
            for (let i = 0; i < maxForward; i++) {
              service.next()
            }
            
            const currentIndex = service.getState().currentStepIndex
            expect(currentIndex).toBe(maxForward)
            
            // Go backward
            for (let i = 0; i < maxForward; i++) {
              service.prev()
            }
            
            // Property: Should be back at step 0
            expect(service.getState().currentStepIndex).toBe(0)
            
            // Property: prev at step 0 should stay at step 0
            service.prev()
            expect(service.getState().currentStepIndex).toBe(0)
          }
        ),
        { numRuns: 30 }
      )
    })

    it('should jump to specific step correctly', () => {
      fc.assert(
        fc.property(
          guideConfigArb.filter(g => g.steps.length >= 2),
          (guideConfig) => {
            service = createGuideService()
            service.registerGuide(guideConfig)
            service.start(guideConfig.id)
            
            const totalSteps = guideConfig.steps.length
            
            // Test jumping to each valid step
            for (let targetStep = 0; targetStep < totalSteps; targetStep++) {
              service.goToStep(targetStep)
              
              // Property: Should be at target step
              expect(service.getState().currentStepIndex).toBe(targetStep)
              expect(service.getCurrentStep()).toEqual(guideConfig.steps[targetStep])
            }
            
            // Property: Invalid step indices should be ignored
            const currentStep = service.getState().currentStepIndex
            service.goToStep(-1)
            expect(service.getState().currentStepIndex).toBe(currentStep)
            
            service.goToStep(totalSteps + 10)
            expect(service.getState().currentStepIndex).toBe(currentStep)
          }
        ),
        { numRuns: 20 }
      )
    })
  })

  describe('Property 60.6: 跳过引导不标记完成', () => {
    it('should not mark guide as completed when skipped', () => {
      fc.assert(
        fc.property(
          guideConfigArb,
          fc.integer({ min: 0, max: 9 }),
          (guideConfig, skipAtStep) => {
            service = createGuideService()
            service.registerGuide(guideConfig)
            service.start(guideConfig.id)
            
            // Navigate to skip point
            const actualSkipStep = Math.min(skipAtStep, guideConfig.steps.length - 1)
            for (let i = 0; i < actualSkipStep; i++) {
              service.next()
            }
            
            // Skip the guide
            service.skip()
            
            // Property: Guide should not be active
            expect(service.getState().isActive).toBe(false)
            
            // Property: Guide should NOT be marked as completed
            expect(service.isCompleted(guideConfig.id)).toBe(false)
            
            // Property: Should be able to show guide again
            expect(service.shouldShowGuide(guideConfig.id)).toBe(true)
          }
        ),
        { numRuns: 30 }
      )
    })
  })

  describe('Property 60.7: 重置引导状态', () => {
    it('should reset individual guide completion state', () => {
      fc.assert(
        fc.property(triggerOnceGuideArb, (guideConfig) => {
          service = createGuideService()
          service.registerGuide(guideConfig)
          
          // Complete the guide
          service.start(guideConfig.id)
          service.complete()
          expect(service.isCompleted(guideConfig.id)).toBe(true)
          expect(service.shouldShowGuide(guideConfig.id)).toBe(false)
          
          // Reset the guide
          service.resetGuide(guideConfig.id)
          
          // Property: Guide should no longer be completed
          expect(service.isCompleted(guideConfig.id)).toBe(false)
          
          // Property: Should be able to show guide again
          expect(service.shouldShowGuide(guideConfig.id)).toBe(true)
          
          // Property: Can start the guide again
          const started = service.tryStart(guideConfig.id)
          expect(started).toBe(true)
        }),
        { numRuns: 30 }
      )
    })

    it('should reset all guides completion state', () => {
      fc.assert(
        fc.property(
          fc.array(triggerOnceGuideArb, { minLength: 2, maxLength: 5 })
            .filter(guides => new Set(guides.map(g => g.id)).size === guides.length),
          (guides) => {
            service = createGuideService()
            
            // Register and complete all guides
            guides.forEach(guide => {
              service.registerGuide(guide)
              service.start(guide.id)
              service.complete()
            })
            
            // Verify all completed
            guides.forEach(guide => {
              expect(service.isCompleted(guide.id)).toBe(true)
            })
            
            // Reset all
            service.resetAllGuides()
            
            // Property: All guides should be reset
            guides.forEach(guide => {
              expect(service.isCompleted(guide.id)).toBe(false)
              expect(service.shouldShowGuide(guide.id)).toBe(true)
            })
          }
        ),
        { numRuns: 15 }
      )
    })
  })

  describe('Property 60.8: 状态变更通知', () => {
    it('should notify on all state changes', () => {
      fc.assert(
        fc.property(guideConfigArb, (guideConfig) => {
          service = createGuideService()
          const stateChanges: Array<{ isActive: boolean; currentStepIndex: number }> = []
          
          service.setOnStateChange((state) => {
            stateChanges.push({
              isActive: state.isActive,
              currentStepIndex: state.currentStepIndex
            })
          })
          
          service.registerGuide(guideConfig)
          
          // Start should trigger change
          service.start(guideConfig.id)
          expect(stateChanges.length).toBeGreaterThan(0)
          expect(stateChanges[stateChanges.length - 1].isActive).toBe(true)
          
          // Next should trigger change
          const beforeNext = stateChanges.length
          if (guideConfig.steps.length > 1) {
            service.next()
            expect(stateChanges.length).toBeGreaterThan(beforeNext)
          }
          
          // Skip should trigger change
          const beforeSkip = stateChanges.length
          service.skip()
          expect(stateChanges.length).toBeGreaterThan(beforeSkip)
          expect(stateChanges[stateChanges.length - 1].isActive).toBe(false)
        }),
        { numRuns: 30 }
      )
    })
  })

  describe('Property 60.9: 多引导独立性', () => {
    it('should manage multiple guides independently', () => {
      fc.assert(
        fc.property(
          fc.array(guideConfigArb, { minLength: 2, maxLength: 5 })
            .filter(guides => new Set(guides.map(g => g.id)).size === guides.length),
          (guides) => {
            service = createGuideService()
            
            // Register all guides
            guides.forEach(guide => service.registerGuide(guide))
            
            // Complete first guide
            const firstGuide = guides[0]
            service.start(firstGuide.id)
            service.complete()
            
            // Property: First guide should be completed
            expect(service.isCompleted(firstGuide.id)).toBe(true)
            
            // Property: Other guides should not be affected
            for (let i = 1; i < guides.length; i++) {
              expect(service.isCompleted(guides[i].id)).toBe(false)
              expect(service.shouldShowGuide(guides[i].id)).toBe(true)
            }
            
            // Property: Can start other guides
            if (guides.length > 1) {
              const secondGuide = guides[1]
              const started = service.tryStart(secondGuide.id)
              expect(started).toBe(true)
              expect(service.getState().currentGuideId).toBe(secondGuide.id)
            }
          }
        ),
        { numRuns: 20 }
      )
    })

    it('should switch between guides correctly', () => {
      fc.assert(
        fc.property(
          fc.tuple(guideConfigArb, guideConfigArb)
            .filter(([g1, g2]) => g1.id !== g2.id),
          ([guide1, guide2]) => {
            service = createGuideService()
            service.registerGuide(guide1)
            service.registerGuide(guide2)
            
            // Start first guide
            service.start(guide1.id)
            expect(service.getState().currentGuideId).toBe(guide1.id)
            
            // Start second guide (should skip first)
            service.start(guide2.id)
            
            // Property: Should switch to second guide
            expect(service.getState().currentGuideId).toBe(guide2.id)
            expect(service.getState().isActive).toBe(true)
            
            // Property: First guide should not be completed (was skipped)
            expect(service.isCompleted(guide1.id)).toBe(false)
          }
        ),
        { numRuns: 20 }
      )
    })
  })

  describe('Property 60.10: 边界条件处理', () => {
    it('should handle unregistered guide gracefully', () => {
      fc.assert(
        fc.property(guideIdArb, (guideId) => {
          service = createGuideService()
          
          // Property: shouldShowGuide returns false for unregistered guide
          expect(service.shouldShowGuide(guideId)).toBe(false)
          
          // Property: start does nothing for unregistered guide
          service.start(guideId)
          expect(service.getState().isActive).toBe(false)
          
          // Property: tryStart returns false for unregistered guide
          expect(service.tryStart(guideId)).toBe(false)
        }),
        { numRuns: 30 }
      )
    })

    it('should handle operations when no guide is active', () => {
      fc.assert(
        fc.property(fc.constant(null), () => {
          service = createGuideService()
          
          // Property: next does nothing when no guide active
          service.next()
          expect(service.getState().isActive).toBe(false)
          
          // Property: prev does nothing when no guide active
          service.prev()
          expect(service.getState().isActive).toBe(false)
          
          // Property: skip does nothing when no guide active
          service.skip()
          expect(service.getState().isActive).toBe(false)
          
          // Property: complete does nothing when no guide active
          service.complete()
          expect(service.getState().isActive).toBe(false)
          
          // Property: getCurrentStep returns undefined
          expect(service.getCurrentStep()).toBeUndefined()
          
          // Property: getProgress returns zeros
          const progress = service.getProgress()
          expect(progress.current).toBe(0)
          expect(progress.total).toBe(0)
          expect(progress.percentage).toBe(0)
        }),
        { numRuns: 10 }
      )
    })

    it('should handle duplicate guide registration', () => {
      fc.assert(
        fc.property(guideConfigArb, (guideConfig) => {
          service = createGuideService()
          
          // Register same guide twice
          service.registerGuide(guideConfig)
          const modifiedConfig = { ...guideConfig, name: 'Modified Name' }
          service.registerGuide(modifiedConfig)
          
          // Property: Second registration should overwrite
          const retrieved = service.getGuide(guideConfig.id)
          expect(retrieved?.name).toBe('Modified Name')
        }),
        { numRuns: 20 }
      )
    })
  })
})


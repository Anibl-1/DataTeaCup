/**
 * Property-based tests for drill-down engine
 * Feature: platform-optimization-plan
 * 
 * **Validates: Requirements 3.4, 3.5**
 */

import { describe, it, expect, vi } from 'vitest'
import * as fc from 'fast-check'
import {
  createDrillDownManager,
  validateDrillConfig,
  type DrillDownConfig,
  type DrillLevel
} from '../drillDown'

// Mock the request module
vi.mock('@/api/request', () => ({
  default: {
    post: vi.fn().mockImplementation((_url: string, params: { level?: number }) => {
      // Return mock data based on level
      const level = params.level || 0
      return Promise.resolve({
        code: 200,
        msg: 'success',
        data: {
          data: [
            { id: 1, value: 100 + level },
            { id: 2, value: 200 + level }
          ],
          level,
          field: `field_${level}`
        }
      })
    })
  }
}))

/**
 * Arbitrary for valid granularity types
 */
const granularityArb: fc.Arbitrary<DrillLevel['granularity']> = fc.constantFrom(
  'year' as const,
  'quarter' as const,
  'month' as const,
  'day' as const,
  'custom' as const
)

/**
 * Arbitrary for valid drill level
 */
const drillLevelArb: fc.Arbitrary<DrillLevel> = fc.record({
  field: fc.string({ minLength: 1, maxLength: 20 }).filter((s: string) => /^[a-zA-Z_][a-zA-Z0-9_]*$/.test(s)),
  granularity: granularityArb,
  label: fc.option(fc.string({ minLength: 1, maxLength: 50 }), { nil: undefined })
})

/**
 * Arbitrary for valid drill levels array (2-5 levels with unique fields)
 */
const drillLevelsArb: fc.Arbitrary<DrillLevel[]> = fc.array(drillLevelArb, { minLength: 2, maxLength: 5 })
  .filter((levels: DrillLevel[]) => {
    const fields = levels.map((l: DrillLevel) => l.field)
    return new Set(fields).size === fields.length // All fields must be unique
  })

/**
 * Arbitrary for valid drill config
 */
const drillConfigArb: fc.Arbitrary<DrillDownConfig> = fc.record({
  chartId: fc.integer({ min: 1, max: 10000 }),
  levels: drillLevelsArb,
  currentLevel: fc.constant(0) // Always start at level 0
}).map((config: { chartId: number; levels: DrillLevel[]; currentLevel: number }) => ({
  ...config,
  currentLevel: 0 // Ensure currentLevel is within bounds
}))

/**
 * Arbitrary for dimension values
 */
const dimensionValueArb: fc.Arbitrary<string | number> = fc.oneof(
  fc.string({ minLength: 1, maxLength: 50 }),
  fc.integer({ min: 1, max: 9999 }),
  fc.date().map((d: Date) => d.getFullYear().toString())
)

describe('drillDown property tests', () => {
  /**
   * Property 7: 下钻/上钻往返一致性
   * 
   * For any drill state at level i, after drilling down to level i+1 
   * and then drilling up, the current level SHALL be restored to i.
   * 
   * **Validates: Requirements 3.4, 3.5**
   */
  describe('Property 7: Drill down/up round-trip consistency', () => {
    it('should restore to original level after drill down then drill up', async () => {
      await fc.assert(
        fc.asyncProperty(
          drillConfigArb,
          dimensionValueArb,
          async (config: DrillDownConfig, dimensionValue: string | number) => {
            const manager = createDrillDownManager(config.chartId)
            
            try {
              // Set up the drill config
              manager.setDrillConfig({
                levels: config.levels,
                currentLevel: 0
              })
              
              // Record initial state
              const initialLevel = manager.getCurrentLevel()
              const initialBreadcrumbLength = manager.getBreadcrumb().length
              
              // Verify we can drill down
              if (!manager.canDrillDown()) {
                return true // Skip if can't drill down
              }
              
              // Drill down
              await manager.drillDown(dimensionValue)
              
              // Verify level increased
              const afterDrillDownLevel = manager.getCurrentLevel()
              expect(afterDrillDownLevel).toBe(initialLevel + 1)
              
              // Verify breadcrumb grew
              const afterDrillDownBreadcrumb = manager.getBreadcrumb()
              expect(afterDrillDownBreadcrumb.length).toBe(initialBreadcrumbLength + 1)
              
              // Drill up
              await manager.drillUp()
              
              // Verify level restored
              const afterDrillUpLevel = manager.getCurrentLevel()
              expect(afterDrillUpLevel).toBe(initialLevel)
              
              // Verify breadcrumb restored
              const afterDrillUpBreadcrumb = manager.getBreadcrumb()
              expect(afterDrillUpBreadcrumb.length).toBe(initialBreadcrumbLength)
              
              return true
            } finally {
              manager.destroy()
            }
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should maintain state consistency through multiple drill operations', async () => {
      await fc.assert(
        fc.asyncProperty(
          drillConfigArb,
          fc.array(dimensionValueArb, { minLength: 1, maxLength: 3 }),
          async (config: DrillDownConfig, dimensionValues: (string | number)[]) => {
            const manager = createDrillDownManager(config.chartId)
            
            try {
              manager.setDrillConfig({
                levels: config.levels,
                currentLevel: 0
              })
              
              // Drill down multiple times
              let drillCount = 0
              for (const value of dimensionValues) {
                if (!manager.canDrillDown()) break
                await manager.drillDown(value)
                drillCount++
              }
              
              // Verify level matches drill count
              expect(manager.getCurrentLevel()).toBe(drillCount)
              
              // Drill up the same number of times
              for (let i = 0; i < drillCount; i++) {
                await manager.drillUp()
              }
              
              // Should be back at level 0
              expect(manager.getCurrentLevel()).toBe(0)
              
              return true
            } finally {
              manager.destroy()
            }
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  /**
   * Property: Reset always returns to level 0
   */
  describe('Reset invariant', () => {
    it('should always reset to level 0 regardless of current state', async () => {
      await fc.assert(
        fc.asyncProperty(
          drillConfigArb,
          fc.array(dimensionValueArb, { minLength: 0, maxLength: 4 }),
          async (config: DrillDownConfig, dimensionValues: (string | number)[]) => {
            const manager = createDrillDownManager(config.chartId)
            
            try {
              manager.setDrillConfig({
                levels: config.levels,
                currentLevel: 0
              })
              
              // Drill down random number of times
              for (const value of dimensionValues) {
                if (!manager.canDrillDown()) break
                await manager.drillDown(value)
              }
              
              // Reset
              await manager.resetDrill()
              
              // Should always be at level 0
              expect(manager.getCurrentLevel()).toBe(0)
              expect(manager.getBreadcrumb().length).toBe(1)
              expect(manager.canDrillUp()).toBe(false)
              
              return true
            } finally {
              manager.destroy()
            }
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  /**
   * Property: Breadcrumb length equals current level + 1
   */
  describe('Breadcrumb length invariant', () => {
    it('should maintain breadcrumb length = currentLevel + 1', async () => {
      await fc.assert(
        fc.asyncProperty(
          drillConfigArb,
          fc.array(dimensionValueArb, { minLength: 0, maxLength: 4 }),
          async (config: DrillDownConfig, dimensionValues: (string | number)[]) => {
            const manager = createDrillDownManager(config.chartId)
            
            try {
              manager.setDrillConfig({
                levels: config.levels,
                currentLevel: 0
              })
              
              // Initial check
              expect(manager.getBreadcrumb().length).toBe(manager.getCurrentLevel() + 1)
              
              // Drill down and check
              for (const value of dimensionValues) {
                if (!manager.canDrillDown()) break
                await manager.drillDown(value)
                expect(manager.getBreadcrumb().length).toBe(manager.getCurrentLevel() + 1)
              }
              
              // Drill up and check
              while (manager.canDrillUp()) {
                await manager.drillUp()
                expect(manager.getBreadcrumb().length).toBe(manager.getCurrentLevel() + 1)
              }
              
              return true
            } finally {
              manager.destroy()
            }
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  /**
   * Property: canDrillDown and canDrillUp are consistent with level bounds
   */
  describe('Drill capability bounds', () => {
    it('should correctly report drill capabilities based on level', async () => {
      await fc.assert(
        fc.asyncProperty(
          drillConfigArb,
          fc.array(dimensionValueArb, { minLength: 0, maxLength: 5 }),
          async (config: DrillDownConfig, dimensionValues: (string | number)[]) => {
            const manager = createDrillDownManager(config.chartId)
            
            try {
              manager.setDrillConfig({
                levels: config.levels,
                currentLevel: 0
              })
              
              const maxLevel = config.levels.length - 1
              
              // At level 0, can't drill up
              expect(manager.canDrillUp()).toBe(false)
              
              // Drill down and check capabilities
              for (const value of dimensionValues) {
                const currentLevel = manager.getCurrentLevel()
                
                // canDrillDown should be true if not at max level
                expect(manager.canDrillDown()).toBe(currentLevel < maxLevel)
                
                // canDrillUp should be true if not at level 0
                expect(manager.canDrillUp()).toBe(currentLevel > 0)
                
                if (!manager.canDrillDown()) break
                await manager.drillDown(value)
              }
              
              return true
            } finally {
              manager.destroy()
            }
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  /**
   * Property: Config validation catches all invalid configs
   */
  describe('Config validation properties', () => {
    it('should accept all valid configs', () => {
      fc.assert(
        fc.property(
          drillConfigArb,
          (config: DrillDownConfig) => {
            const errors = validateDrillConfig(config)
            return errors.length === 0
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should reject configs with duplicate field names', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 1, max: 1000 }),
          fc.string({ minLength: 1, maxLength: 20 }).filter((s: string) => /^[a-zA-Z_][a-zA-Z0-9_]*$/.test(s)),
          (chartId: number, field: string) => {
            const config: DrillDownConfig = {
              chartId,
              levels: [
                { field, granularity: 'year' },
                { field, granularity: 'month' } // Duplicate field
              ],
              currentLevel: 0
            }
            
            const errors = validateDrillConfig(config)
            return errors.some((e: string) => e.includes('重复'))
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should reject configs with invalid chart ID', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: -1000, max: 0 }),
          drillLevelsArb,
          (chartId: number, levels: DrillLevel[]) => {
            const config: DrillDownConfig = {
              chartId,
              levels,
              currentLevel: 0
            }
            
            const errors = validateDrillConfig(config)
            return errors.some((e: string) => e.includes('图表ID无效'))
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should reject configs with currentLevel out of bounds', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 1, max: 1000 }),
          drillLevelsArb,
          (chartId: number, levels: DrillLevel[]) => {
            const config: DrillDownConfig = {
              chartId,
              levels,
              currentLevel: levels.length + 5 // Out of bounds
            }
            
            const errors = validateDrillConfig(config)
            return errors.some((e: string) => e.includes('超出层级配置范围'))
          }
        ),
        { numRuns: 100 }
      )
    })
  })
})

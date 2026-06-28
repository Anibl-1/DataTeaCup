/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  createDrillDownManager,
  validateDrillConfig,
  formatBreadcrumb,
  getTimeFormatter,
  createDrillConfig,
  TIME_DRILL_CONFIG,
  REGION_DRILL_CONFIG,
  type DrillDownConfig,
  type BreadcrumbItem
} from '../drillDown'

// Mock the request module
vi.mock('@/api/request', () => ({
  default: {
    post: vi.fn().mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        data: [
          { year: '2023', value: 100 },
          { year: '2024', value: 200 }
        ],
        level: 0,
        field: 'year'
      }
    })
  }
}))

describe('drillDown', () => {
  describe('createDrillDownManager', () => {
    let manager: ReturnType<typeof createDrillDownManager>

    beforeEach(() => {
      manager = createDrillDownManager(1)
    })

    afterEach(() => {
      manager.destroy()
    })

    it('should create a drill-down manager instance', () => {
      expect(manager).toBeDefined()
      expect(manager.setDrillConfig).toBeDefined()
      expect(manager.getDrillConfig).toBeDefined()
      expect(manager.drillDown).toBeDefined()
      expect(manager.drillUp).toBeDefined()
      expect(manager.resetDrill).toBeDefined()
      expect(manager.getCurrentLevel).toBeDefined()
      expect(manager.getBreadcrumb).toBeDefined()
    })

    it('should return null config before setting', () => {
      expect(manager.getDrillConfig()).toBeNull()
    })

    it('should set and get drill configuration', () => {
      const config = {
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      }
      
      manager.setDrillConfig(config)
      const result = manager.getDrillConfig()
      
      expect(result).not.toBeNull()
      expect(result!.chartId).toBe(1)
      expect(result!.levels).toHaveLength(4)
      expect(result!.currentLevel).toBe(0)
    })

    it('should initialize breadcrumb with root level', () => {
      manager.setDrillConfig({
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      })
      
      const breadcrumb = manager.getBreadcrumb()
      
      expect(breadcrumb).toHaveLength(1)
      expect(breadcrumb[0].level).toBe(0)
      expect(breadcrumb[0].label).toBe('年')
      expect(breadcrumb[0].value).toBeNull()
    })

    it('should return current level', () => {
      manager.setDrillConfig({
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      })
      
      expect(manager.getCurrentLevel()).toBe(0)
    })

    it('should check if can drill down', () => {
      manager.setDrillConfig({
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      })
      
      expect(manager.canDrillDown()).toBe(true)
    })

    it('should check if can drill up', () => {
      manager.setDrillConfig({
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      })
      
      expect(manager.canDrillUp()).toBe(false)
    })

    it('should not be able to drill down at last level', () => {
      manager.setDrillConfig({
        levels: TIME_DRILL_CONFIG,
        currentLevel: 3 // Last level (day)
      })
      
      // Manually set state to last level
      manager.setDrillConfig({
        levels: [{ field: 'day', granularity: 'day', label: '日' }],
        currentLevel: 0
      })
      
      expect(manager.canDrillDown()).toBe(false)
    })

    it('should add and remove event listeners', async () => {
      const listener = vi.fn()
      
      manager.setDrillConfig({
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      })
      
      manager.addEventListener(listener)
      await manager.resetDrill()
      
      expect(listener).toHaveBeenCalled()
      
      listener.mockClear()
      manager.removeEventListener(listener)
      await manager.resetDrill()
      
      expect(listener).not.toHaveBeenCalled()
    })

    it('should emit drill-reset event on reset', async () => {
      const listener = vi.fn()
      
      manager.setDrillConfig({
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      })
      
      manager.addEventListener(listener)
      await manager.resetDrill()
      
      expect(listener).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'drill-reset',
          chartId: 1,
          level: 0
        })
      )
    })

    it('should emit drill-down event on drill down', async () => {
      const listener = vi.fn()
      
      manager.setDrillConfig({
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      })
      
      manager.addEventListener(listener)
      await manager.drillDown('2023')
      
      expect(listener).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'drill-down',
          chartId: 1,
          level: 1,
          dimensionValue: '2023'
        })
      )
    })

    it('should update breadcrumb on drill down', async () => {
      manager.setDrillConfig({
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      })
      
      await manager.drillDown('2023')
      
      const breadcrumb = manager.getBreadcrumb()
      expect(breadcrumb).toHaveLength(2)
      expect(breadcrumb[1].value).toBe('2023')
      expect(breadcrumb[1].label).toBe('季度')
    })

    it('should update current level on drill down', async () => {
      manager.setDrillConfig({
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      })
      
      await manager.drillDown('2023')
      
      expect(manager.getCurrentLevel()).toBe(1)
    })

    it('should emit drill-up event on drill up', async () => {
      const listener = vi.fn()
      
      manager.setDrillConfig({
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      })
      
      // First drill down
      await manager.drillDown('2023')
      
      manager.addEventListener(listener)
      await manager.drillUp()
      
      expect(listener).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'drill-up',
          chartId: 1,
          level: 0
        })
      )
    })

    it('should update breadcrumb on drill up', async () => {
      manager.setDrillConfig({
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      })
      
      await manager.drillDown('2023')
      expect(manager.getBreadcrumb()).toHaveLength(2)
      
      await manager.drillUp()
      expect(manager.getBreadcrumb()).toHaveLength(1)
    })

    it('should not drill up at root level', async () => {
      const listener = vi.fn()
      
      manager.setDrillConfig({
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      })
      
      manager.addEventListener(listener)
      await manager.drillUp()
      
      expect(listener).not.toHaveBeenCalled()
      expect(manager.getCurrentLevel()).toBe(0)
    })

    it('should not drill down without config', async () => {
      const listener = vi.fn()
      manager.addEventListener(listener)
      
      await manager.drillDown('2023')
      
      expect(listener).not.toHaveBeenCalled()
    })

    it('should get current state', () => {
      manager.setDrillConfig({
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      })
      
      const state = manager.getState()
      
      expect(state.currentLevel).toBe(0)
      expect(state.breadcrumb).toHaveLength(1)
      expect(state.filters).toEqual({})
    })

    it('should destroy the manager', () => {
      manager.setDrillConfig({
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      })
      
      manager.destroy()
      
      expect(manager.getDrillConfig()).toBeNull()
      expect(manager.getBreadcrumb()).toHaveLength(0)
    })

    it('should drill to specific level', async () => {
      const listener = vi.fn()
      
      manager.setDrillConfig({
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      })
      
      // Drill down twice first
      await manager.drillDown('2023')
      await manager.drillDown('Q1')
      
      expect(manager.getCurrentLevel()).toBe(2)
      
      manager.addEventListener(listener)
      
      // Drill back to level 0
      await manager.drillToLevel(0)
      
      expect(manager.getCurrentLevel()).toBe(0)
    })

    it('should not drill to invalid level', async () => {
      manager.setDrillConfig({
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      })
      
      const initialLevel = manager.getCurrentLevel()
      
      await manager.drillToLevel(-1)
      expect(manager.getCurrentLevel()).toBe(initialLevel)
      
      await manager.drillToLevel(10)
      expect(manager.getCurrentLevel()).toBe(initialLevel)
    })

    it('should not drill to same level', async () => {
      const listener = vi.fn()
      
      manager.setDrillConfig({
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      })
      
      manager.addEventListener(listener)
      await manager.drillToLevel(0)
      
      expect(listener).not.toHaveBeenCalled()
    })
  })

  describe('validateDrillConfig', () => {
    it('should return no errors for valid config', () => {
      const config: DrillDownConfig = {
        chartId: 1,
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      }
      
      const errors = validateDrillConfig(config)
      expect(errors).toHaveLength(0)
    })

    it('should return error for invalid chart ID', () => {
      const config: DrillDownConfig = {
        chartId: 0,
        levels: TIME_DRILL_CONFIG,
        currentLevel: 0
      }
      
      const errors = validateDrillConfig(config)
      expect(errors).toContain('图表ID无效')
    })

    it('should return error for empty levels', () => {
      const config: DrillDownConfig = {
        chartId: 1,
        levels: [],
        currentLevel: 0
      }
      
      const errors = validateDrillConfig(config)
      expect(errors).toContain('下钻层级配置不能为空')
    })

    it('should return error for missing field name', () => {
      const config: DrillDownConfig = {
        chartId: 1,
        levels: [{ field: '', granularity: 'year' }],
        currentLevel: 0
      }
      
      const errors = validateDrillConfig(config)
      expect(errors).toContain('层级1的字段名不能为空')
    })

    it('should return error for duplicate field names', () => {
      const config: DrillDownConfig = {
        chartId: 1,
        levels: [
          { field: 'year', granularity: 'year' },
          { field: 'year', granularity: 'month' }
        ],
        currentLevel: 0
      }
      
      const errors = validateDrillConfig(config)
      expect(errors).toContain('层级2的字段名"year"重复')
    })

    it('should return error for invalid granularity', () => {
      const config: DrillDownConfig = {
        chartId: 1,
        levels: [{ field: 'year', granularity: 'invalid' as any }],
        currentLevel: 0
      }
      
      const errors = validateDrillConfig(config)
      expect(errors.some(e => e.includes('粒度类型'))).toBe(true)
    })

    it('should return error for negative current level', () => {
      const config: DrillDownConfig = {
        chartId: 1,
        levels: TIME_DRILL_CONFIG,
        currentLevel: -1
      }
      
      const errors = validateDrillConfig(config)
      expect(errors).toContain('当前层级不能为负数')
    })

    it('should return error for current level out of range', () => {
      const config: DrillDownConfig = {
        chartId: 1,
        levels: TIME_DRILL_CONFIG,
        currentLevel: 10
      }
      
      const errors = validateDrillConfig(config)
      expect(errors).toContain('当前层级超出层级配置范围')
    })
  })

  describe('formatBreadcrumb', () => {
    it('should format breadcrumb with default separator', () => {
      const breadcrumb: BreadcrumbItem[] = [
        { level: 0, label: '年', value: null, field: 'year' },
        { level: 1, label: '季度', value: '2023', field: 'quarter' }
      ]
      
      const result = formatBreadcrumb(breadcrumb)
      expect(result).toBe('年 > 季度: 2023')
    })

    it('should format breadcrumb with custom separator', () => {
      const breadcrumb: BreadcrumbItem[] = [
        { level: 0, label: '年', value: null, field: 'year' },
        { level: 1, label: '季度', value: '2023', field: 'quarter' }
      ]
      
      const result = formatBreadcrumb(breadcrumb, ' / ')
      expect(result).toBe('年 / 季度: 2023')
    })

    it('should handle empty breadcrumb', () => {
      const result = formatBreadcrumb([])
      expect(result).toBe('')
    })

    it('should handle breadcrumb with only root', () => {
      const breadcrumb: BreadcrumbItem[] = [
        { level: 0, label: '年', value: null, field: 'year' }
      ]
      
      const result = formatBreadcrumb(breadcrumb)
      expect(result).toBe('年')
    })
  })

  describe('getTimeFormatter', () => {
    it('should format year', () => {
      const formatter = getTimeFormatter('year')
      expect(formatter(2023)).toBe('2023年')
    })

    it('should format quarter', () => {
      const formatter = getTimeFormatter('quarter')
      expect(formatter(1)).toBe('Q1')
    })

    it('should format month', () => {
      const formatter = getTimeFormatter('month')
      expect(formatter(12)).toBe('12月')
    })

    it('should format day', () => {
      const formatter = getTimeFormatter('day')
      expect(formatter(15)).toBe('15日')
    })

    it('should format custom as string', () => {
      const formatter = getTimeFormatter('custom')
      expect(formatter('test')).toBe('test')
    })
  })

  describe('createDrillConfig', () => {
    it('should create drill config from simple array', () => {
      const levels = [
        { field: 'region' },
        { field: 'city' },
        { field: 'store' }
      ]
      
      const result = createDrillConfig(levels)
      
      expect(result).toHaveLength(3)
      expect(result[0].field).toBe('region')
      expect(result[0].granularity).toBe('custom')
      expect(result[0].label).toBe('层级1')
    })

    it('should preserve custom labels', () => {
      const levels = [
        { field: 'region', label: '区域' },
        { field: 'city', label: '城市' }
      ]
      
      const result = createDrillConfig(levels)
      
      expect(result[0].label).toBe('区域')
      expect(result[1].label).toBe('城市')
    })

    it('should preserve custom granularity', () => {
      const levels = [
        { field: 'year', granularity: 'year' as const },
        { field: 'month', granularity: 'month' as const }
      ]
      
      const result = createDrillConfig(levels)
      
      expect(result[0].granularity).toBe('year')
      expect(result[1].granularity).toBe('month')
    })
  })

  describe('preset configurations', () => {
    it('should have valid TIME_DRILL_CONFIG', () => {
      expect(TIME_DRILL_CONFIG).toHaveLength(4)
      expect(TIME_DRILL_CONFIG[0].field).toBe('year')
      expect(TIME_DRILL_CONFIG[1].field).toBe('quarter')
      expect(TIME_DRILL_CONFIG[2].field).toBe('month')
      expect(TIME_DRILL_CONFIG[3].field).toBe('day')
    })

    it('should have valid REGION_DRILL_CONFIG', () => {
      expect(REGION_DRILL_CONFIG).toHaveLength(3)
      expect(REGION_DRILL_CONFIG[0].field).toBe('region')
      expect(REGION_DRILL_CONFIG[1].field).toBe('city')
      expect(REGION_DRILL_CONFIG[2].field).toBe('store')
    })
  })
})

/**
 * Chart Types Unit Tests
 * Tests for the extended chart type definitions
 */

import { describe, it, expect } from 'vitest'
import { 
  CHART_TYPES, 
  CHART_TYPE_CONFIGS, 
  CHART_CATEGORY_LABELS,
  getChartTypesByCategory,
  type ChartCategory
} from '../chart'

describe('Chart Types', () => {
  describe('CHART_TYPES', () => {
    it('should include all new chart types', () => {
      const chartTypeValues = CHART_TYPES.map(t => t.value)
      
      // New chart types from task 6.4
      expect(chartTypeValues).toContain('chinaMap')
      expect(chartTypeValues).toContain('worldMap')
      expect(chartTypeValues).toContain('kpi')
      expect(chartTypeValues).toContain('waterfall')
      expect(chartTypeValues).toContain('wordCloud')
      expect(chartTypeValues).toContain('combo')
    })

    it('should have proper category for each chart type', () => {
      const validCategories: ChartCategory[] = ['basic', 'advanced', 'special', 'geographic', 'statistical', 'financial', 'relationship']
      
      for (const chartType of CHART_TYPES) {
        expect(validCategories).toContain(chartType.category)
      }
    })

    it('should have icon for each chart type', () => {
      for (const chartType of CHART_TYPES) {
        expect(chartType.icon).toBeDefined()
        expect(typeof chartType.icon).toBe('string')
        expect(chartType.icon.length).toBeGreaterThan(0)
      }
    })

    it('should have unique values', () => {
      const values = CHART_TYPES.map(t => t.value)
      const uniqueValues = new Set(values)
      expect(uniqueValues.size).toBe(values.length)
    })
  })

  describe('CHART_TYPE_CONFIGS', () => {
    it('should have config for new chart types', () => {
      expect(CHART_TYPE_CONFIGS.chinaMap).toBeDefined()
      expect(CHART_TYPE_CONFIGS.worldMap).toBeDefined()
      expect(CHART_TYPE_CONFIGS.kpi).toBeDefined()
      expect(CHART_TYPE_CONFIGS.waterfall).toBeDefined()
      expect(CHART_TYPE_CONFIGS.wordCloud).toBeDefined()
      expect(CHART_TYPE_CONFIGS.combo).toBeDefined()
    })

    it('should have proper field descriptions for new chart types', () => {
      expect(CHART_TYPE_CONFIGS.chinaMap.fieldDescription).toBeDefined()
      expect(CHART_TYPE_CONFIGS.worldMap.fieldDescription).toBeDefined()
      expect(CHART_TYPE_CONFIGS.kpi.fieldDescription).toBeDefined()
      expect(CHART_TYPE_CONFIGS.waterfall.fieldDescription).toBeDefined()
      expect(CHART_TYPE_CONFIGS.wordCloud.fieldDescription).toBeDefined()
      expect(CHART_TYPE_CONFIGS.combo.fieldDescription).toBeDefined()
    })

    it('should have special fields for new chart types', () => {
      expect(CHART_TYPE_CONFIGS.chinaMap.specialFields).toContain('region')
      expect(CHART_TYPE_CONFIGS.kpi.specialFields).toContain('value')
      expect(CHART_TYPE_CONFIGS.waterfall.specialFields).toContain('name')
      expect(CHART_TYPE_CONFIGS.wordCloud.specialFields).toContain('word')
      expect(CHART_TYPE_CONFIGS.combo.specialFields).toContain('category')
    })
  })

  describe('CHART_CATEGORY_LABELS', () => {
    it('should have labels for all categories', () => {
      const categories: ChartCategory[] = ['basic', 'advanced', 'special', 'geographic', 'statistical', 'financial', 'relationship']
      
      for (const category of categories) {
        expect(CHART_CATEGORY_LABELS[category]).toBeDefined()
        expect(typeof CHART_CATEGORY_LABELS[category]).toBe('string')
      }
    })
  })

  describe('getChartTypesByCategory', () => {
    it('should group chart types by category', () => {
      const grouped = getChartTypesByCategory()
      
      // Check that all categories exist
      expect(grouped.basic).toBeDefined()
      expect(grouped.advanced).toBeDefined()
      expect(grouped.special).toBeDefined()
      expect(grouped.geographic).toBeDefined()
      
      // Check that new chart types are in correct categories
      const advancedValues = grouped.advanced.map(t => t.value)
      expect(advancedValues).toContain('kpi')
      expect(advancedValues).toContain('combo')
      expect(advancedValues).toContain('waterfall')
      expect(advancedValues).toContain('wordCloud')
      
      const geographicValues = grouped.geographic.map(t => t.value)
      expect(geographicValues).toContain('chinaMap')
      expect(geographicValues).toContain('worldMap')
    })

    it('should include all chart types in grouped result', () => {
      const grouped = getChartTypesByCategory()
      const allGroupedTypes = Object.values(grouped).flat()
      
      expect(allGroupedTypes.length).toBe(CHART_TYPES.length)
    })
  })
})

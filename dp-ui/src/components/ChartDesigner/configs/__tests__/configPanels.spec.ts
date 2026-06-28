/**
 * Chart Config Panels Unit Tests
 * Tests for the chart-specific configuration panel utilities
 */

import { describe, it, expect } from 'vitest'
import { getChartConfigComponent, hasChartConfigPanel } from '../index'

describe('Chart Config Panels', () => {
  describe('hasChartConfigPanel', () => {
    it('should return true for new chart types with config panels', () => {
      expect(hasChartConfigPanel('map')).toBe(true)
      expect(hasChartConfigPanel('chinaMap')).toBe(true)
      expect(hasChartConfigPanel('worldMap')).toBe(true)
      expect(hasChartConfigPanel('kpi')).toBe(true)
      expect(hasChartConfigPanel('waterfall')).toBe(true)
      expect(hasChartConfigPanel('wordCloud')).toBe(true)
      expect(hasChartConfigPanel('combo')).toBe(true)
    })

    it('should return false for chart types without specific config panels', () => {
      expect(hasChartConfigPanel('line')).toBe(false)
      expect(hasChartConfigPanel('bar')).toBe(false)
      expect(hasChartConfigPanel('pie')).toBe(false)
      expect(hasChartConfigPanel('scatter')).toBe(false)
      expect(hasChartConfigPanel('radar')).toBe(false)
      expect(hasChartConfigPanel('table')).toBe(false)
    })

    it('should return false for invalid chart types', () => {
      expect(hasChartConfigPanel('')).toBe(false)
      expect(hasChartConfigPanel('invalid')).toBe(false)
      expect(hasChartConfigPanel('unknown')).toBe(false)
    })
  })

  describe('getChartConfigComponent', () => {
    it('should return correct component name for map chart types', () => {
      expect(getChartConfigComponent('map')).toBe('MapChartConfig')
      expect(getChartConfigComponent('chinaMap')).toBe('MapChartConfig')
      expect(getChartConfigComponent('worldMap')).toBe('MapChartConfig')
    })

    it('should return correct component name for KPI chart', () => {
      expect(getChartConfigComponent('kpi')).toBe('KpiChartConfig')
    })

    it('should return correct component name for waterfall chart', () => {
      expect(getChartConfigComponent('waterfall')).toBe('WaterfallChartConfig')
    })

    it('should return correct component name for word cloud chart', () => {
      expect(getChartConfigComponent('wordCloud')).toBe('WordCloudChartConfig')
    })

    it('should return correct component name for combo chart', () => {
      expect(getChartConfigComponent('combo')).toBe('ComboChartConfig')
    })

    it('should return null for chart types without specific config', () => {
      expect(getChartConfigComponent('line')).toBeNull()
      expect(getChartConfigComponent('bar')).toBeNull()
      expect(getChartConfigComponent('pie')).toBeNull()
      expect(getChartConfigComponent('invalid')).toBeNull()
    })
  })
})

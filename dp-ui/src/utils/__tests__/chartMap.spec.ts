/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * Unit Tests for Map Chart Utilities
 * 
 * Tests for chartMap.ts utility functions
 */

import { describe, it, expect } from 'vitest'
import {
  buildChinaMapOption,
  buildWorldMapOption,
  buildProvinceMapOption,
  getProvinceMapName,
  validateMapDataIntegrity,
  CHINA_PROVINCES,
  type MapDataItem
} from '../chartMap'

describe('chartMap utilities', () => {
  describe('buildChinaMapOption', () => {
    it('should build valid China map option with data', () => {
      const data: MapDataItem[] = [
        { name: '北京', value: 100 },
        { name: '上海', value: 200 },
        { name: '广东', value: 300 }
      ]

      const option = buildChinaMapOption(data)

      expect(option.series).toBeDefined()
      expect(option.visualMap).toBeDefined()
      expect(option.tooltip).toBeDefined()

      const series = option.series as any[]
      expect(series[0].type).toBe('map')
      expect(series[0].map).toBe('china')
      expect(series[0].data).toHaveLength(3)
    })

    it('should apply title when provided', () => {
      const data: MapDataItem[] = [{ name: '北京', value: 100 }]
      const option = buildChinaMapOption(data, { title: '中国地图' })

      expect(option.title).toBeDefined()
      expect((option.title as any).text).toBe('中国地图')
    })

    it('should handle empty data', () => {
      const option = buildChinaMapOption([])

      expect(option.series).toBeDefined()
      const series = option.series as any[]
      expect(series[0].data).toHaveLength(0)
    })

    it('should calculate visualMap range from data', () => {
      const data: MapDataItem[] = [
        { name: '北京', value: 50 },
        { name: '上海', value: 150 }
      ]

      const option = buildChinaMapOption(data)
      const vm = option.visualMap as any

      expect(vm.min).toBe(50)
      expect(vm.max).toBe(150)
    })

    it('should handle single value data range', () => {
      const data: MapDataItem[] = [{ name: '北京', value: 100 }]

      const option = buildChinaMapOption(data)
      const vm = option.visualMap as any

      // When min equals max, range should be expanded
      expect(vm.min).toBeLessThan(vm.max)
    })

    it('should apply custom visualMap config', () => {
      const data: MapDataItem[] = [{ name: '北京', value: 100 }]
      const option = buildChinaMapOption(data, {
        visualMapConfig: {
          min: 0,
          max: 1000,
          calculable: false,
          inRange: { color: ['#fff', '#000'] }
        }
      })

      const vm = option.visualMap as any
      expect(vm.min).toBe(0)
      expect(vm.max).toBe(1000)
      expect(vm.calculable).toBe(false)
      expect(vm.inRange.color).toEqual(['#fff', '#000'])
    })

    it('should configure roam option', () => {
      const data: MapDataItem[] = [{ name: '北京', value: 100 }]

      const optionWithRoam = buildChinaMapOption(data, { roam: true })
      const optionWithoutRoam = buildChinaMapOption(data, { roam: false })

      expect((optionWithRoam.series as any[])[0].roam).toBe(true)
      expect((optionWithoutRoam.series as any[])[0].roam).toBe(false)
    })

    it('should configure label visibility', () => {
      const data: MapDataItem[] = [{ name: '北京', value: 100 }]

      const optionWithLabel = buildChinaMapOption(data, { showLabel: true })
      const optionWithoutLabel = buildChinaMapOption(data, { showLabel: false })

      expect((optionWithLabel.series as any[])[0].label.show).toBe(true)
      expect((optionWithoutLabel.series as any[])[0].label.show).toBe(false)
    })
  })

  describe('buildWorldMapOption', () => {
    it('should build valid World map option with data', () => {
      const data: MapDataItem[] = [
        { name: 'China', value: 1000 },
        { name: 'United States', value: 800 },
        { name: 'Russia', value: 600 }
      ]

      const option = buildWorldMapOption(data)

      expect(option.series).toBeDefined()
      const series = option.series as any[]
      expect(series[0].type).toBe('map')
      expect(series[0].map).toBe('world')
      expect(series[0].data).toHaveLength(3)
    })

    it('should default to horizontal visualMap orientation', () => {
      const data: MapDataItem[] = [{ name: 'China', value: 100 }]
      const option = buildWorldMapOption(data)

      const vm = option.visualMap as any
      expect(vm.orient).toBe('horizontal')
    })

    it('should default to no labels for world map', () => {
      const data: MapDataItem[] = [{ name: 'China', value: 100 }]
      const option = buildWorldMapOption(data)

      const series = option.series as any[]
      expect(series[0].label.show).toBe(false)
    })
  })

  describe('buildProvinceMapOption', () => {
    it('should build valid province map option', () => {
      const data: MapDataItem[] = [
        { name: '广州', value: 500 },
        { name: '深圳', value: 400 }
      ]

      const option = buildProvinceMapOption('广东', data)

      expect(option.series).toBeDefined()
      const series = option.series as any[]
      expect(series[0].type).toBe('map')
      expect(series[0].map).toBe('广东')
      expect(series[0].data).toHaveLength(2)
    })

    it('should use province name as series name when no title', () => {
      const data: MapDataItem[] = [{ name: '广州', value: 100 }]
      const option = buildProvinceMapOption('广东', data)

      const series = option.series as any[]
      expect(series[0].name).toBe('广东')
    })

    it('should use title as series name when provided', () => {
      const data: MapDataItem[] = [{ name: '广州', value: 100 }]
      const option = buildProvinceMapOption('广东', data, { title: '广东省数据' })

      const series = option.series as any[]
      expect(series[0].name).toBe('广东省数据')
    })
  })

  describe('getProvinceMapName', () => {
    it('should return standard province name', () => {
      expect(getProvinceMapName('北京')).toBe('北京')
      expect(getProvinceMapName('北京市')).toBe('北京')
      expect(getProvinceMapName('广东')).toBe('广东')
      expect(getProvinceMapName('广东省')).toBe('广东')
    })

    it('should handle autonomous regions', () => {
      expect(getProvinceMapName('内蒙古')).toBe('内蒙古')
      expect(getProvinceMapName('内蒙古自治区')).toBe('内蒙古')
      expect(getProvinceMapName('新疆')).toBe('新疆')
      expect(getProvinceMapName('新疆维吾尔自治区')).toBe('新疆')
    })

    it('should handle special administrative regions', () => {
      expect(getProvinceMapName('香港')).toBe('香港')
      expect(getProvinceMapName('香港特别行政区')).toBe('香港')
      expect(getProvinceMapName('澳门')).toBe('澳门')
      expect(getProvinceMapName('澳门特别行政区')).toBe('澳门')
    })

    it('should return original name for unknown provinces', () => {
      expect(getProvinceMapName('未知省份')).toBe('未知省份')
    })
  })

  describe('validateMapDataIntegrity', () => {
    it('should return true for valid data mapping', () => {
      const data: MapDataItem[] = [
        { name: '北京', value: 100 },
        { name: '上海', value: 200 }
      ]
      const option = buildChinaMapOption(data)

      expect(validateMapDataIntegrity(data, option)).toBe(true)
    })

    it('should return true for empty data', () => {
      const option = buildChinaMapOption([])
      expect(validateMapDataIntegrity([], option)).toBe(true)
    })

    it('should return false when series is missing', () => {
      const data: MapDataItem[] = [{ name: '北京', value: 100 }]
      const option = { visualMap: {} }

      expect(validateMapDataIntegrity(data, option)).toBe(false)
    })

    it('should return false when data item is missing', () => {
      const data: MapDataItem[] = [
        { name: '北京', value: 100 },
        { name: '上海', value: 200 }
      ]
      const option = {
        series: [{
          data: [{ name: '北京', value: 100 }] // Missing 上海
        }]
      }

      expect(validateMapDataIntegrity(data, option)).toBe(false)
    })

    it('should return false when value does not match', () => {
      const data: MapDataItem[] = [{ name: '北京', value: 100 }]
      const option = {
        series: [{
          data: [{ name: '北京', value: 999 }] // Wrong value
        }]
      }

      expect(validateMapDataIntegrity(data, option)).toBe(false)
    })
  })

  describe('CHINA_PROVINCES', () => {
    it('should contain all 34 provinces/regions', () => {
      expect(CHINA_PROVINCES).toHaveLength(34)
    })

    it('should include major provinces', () => {
      expect(CHINA_PROVINCES).toContain('北京')
      expect(CHINA_PROVINCES).toContain('上海')
      expect(CHINA_PROVINCES).toContain('广东')
      expect(CHINA_PROVINCES).toContain('浙江')
    })

    it('should include autonomous regions', () => {
      expect(CHINA_PROVINCES).toContain('内蒙古')
      expect(CHINA_PROVINCES).toContain('西藏')
      expect(CHINA_PROVINCES).toContain('新疆')
      expect(CHINA_PROVINCES).toContain('广西')
      expect(CHINA_PROVINCES).toContain('宁夏')
    })

    it('should include special administrative regions', () => {
      expect(CHINA_PROVINCES).toContain('香港')
      expect(CHINA_PROVINCES).toContain('澳门')
      expect(CHINA_PROVINCES).toContain('台湾')
    })
  })
})

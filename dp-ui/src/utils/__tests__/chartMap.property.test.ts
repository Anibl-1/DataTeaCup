/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * Property-Based Tests for Map Chart
 * 
 * Feature: platform-optimization-plan
 * Property 8: 地图图表数据映射完整性
 * 
 * **Validates: Requirements 4.1, 4.2**
 * 
 * For any valid region-value dataset (China provinces or world countries),
 * the generated map ECharts option SHALL contain data mappings for all input regions,
 * and each region's value SHALL match the input.
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import {
  buildChinaMapOption,
  buildWorldMapOption,
  buildProvinceMapOption,
  validateMapDataIntegrity,
  CHINA_PROVINCES,
  type MapDataItem,
  type VisualMapConfig
} from '../chartMap'

describe('Map Chart Property Tests', () => {
  /**
   * Property 8: 地图图表数据映射完整性
   * 
   * For any valid region-value dataset (China provinces or world countries),
   * the generated map ECharts option SHALL contain data mappings for all input regions,
   * and each region's value SHALL match the input.
   * 
   * **Validates: Requirements 4.1, 4.2**
   */
  describe('Property 8: Map Data Mapping Integrity', () => {
    // Arbitrary for generating valid China province data
    const chinaProvinceDataArb = fc.array(
      fc.record({
        name: fc.constantFrom(...CHINA_PROVINCES),
        value: fc.float({ min: 0, max: 10000, noNaN: true })
      }),
      { minLength: 1, maxLength: 34 }
    ).map(arr => {
      // Ensure unique province names
      const seen = new Set<string>()
      return arr.filter(item => {
        if (seen.has(item.name)) return false
        seen.add(item.name)
        return true
      })
    })

    // Arbitrary for generating valid world country data
    const worldCountryNames = [
      'China', 'United States', 'Russia', 'Canada', 'Brazil',
      'Australia', 'India', 'Argentina', 'Kazakhstan', 'Algeria',
      'Germany', 'France', 'United Kingdom', 'Japan', 'South Korea',
      'Mexico', 'Indonesia', 'Saudi Arabia', 'Turkey', 'Iran',
      'Thailand', 'Spain', 'Italy', 'Poland', 'Sweden'
    ]

    const worldCountryDataArb = fc.array(
      fc.record({
        name: fc.constantFrom(...worldCountryNames),
        value: fc.float({ min: 0, max: 10000, noNaN: true })
      }),
      { minLength: 1, maxLength: 25 }
    ).map(arr => {
      // Ensure unique country names
      const seen = new Set<string>()
      return arr.filter(item => {
        if (seen.has(item.name)) return false
        seen.add(item.name)
        return true
      })
    })

    // Arbitrary for visual map config
    const visualMapConfigArb = fc.option(
      fc.record({
        min: fc.float({ min: 0, max: 100, noNaN: true }),
        max: fc.float({ min: 100, max: 10000, noNaN: true }),
        calculable: fc.boolean()
      }),
      { nil: undefined }
    )

    it('should preserve all input data in China map option', () => {
      fc.assert(
        fc.property(chinaProvinceDataArb, (data) => {
          // Skip empty data
          if (data.length === 0) return true

          const option = buildChinaMapOption(data)

          // Verify data integrity
          expect(validateMapDataIntegrity(data, option)).toBe(true)

          // Verify series data contains all input items
          const series = option.series as any[]
          expect(series).toBeDefined()
          expect(series.length).toBeGreaterThan(0)

          const seriesData = series[0].data as MapDataItem[]
          expect(seriesData).toBeDefined()
          expect(seriesData.length).toBe(data.length)

          // Verify each input item exists in output with correct value
          for (const inputItem of data) {
            const outputItem = seriesData.find(d => d.name === inputItem.name)
            expect(outputItem).toBeDefined()
            expect(outputItem?.value).toBe(inputItem.value)
          }

          return true
        }),
        { numRuns: 100 }
      )
    })

    it('should preserve all input data in World map option', () => {
      fc.assert(
        fc.property(worldCountryDataArb, (data) => {
          // Skip empty data
          if (data.length === 0) return true

          const option = buildWorldMapOption(data)

          // Verify data integrity
          expect(validateMapDataIntegrity(data, option)).toBe(true)

          // Verify series data contains all input items
          const series = option.series as any[]
          expect(series).toBeDefined()
          expect(series.length).toBeGreaterThan(0)

          const seriesData = series[0].data as MapDataItem[]
          expect(seriesData).toBeDefined()
          expect(seriesData.length).toBe(data.length)

          // Verify each input item exists in output with correct value
          for (const inputItem of data) {
            const outputItem = seriesData.find(d => d.name === inputItem.name)
            expect(outputItem).toBeDefined()
            expect(outputItem?.value).toBe(inputItem.value)
          }

          return true
        }),
        { numRuns: 100 }
      )
    })

    it('should apply visual map config correctly', () => {
      fc.assert(
        fc.property(
          chinaProvinceDataArb,
          visualMapConfigArb,
          (data, visualMapConfig) => {
            if (data.length === 0) return true

            const option = buildChinaMapOption(data, { visualMapConfig })

            // Verify visualMap exists
            expect(option.visualMap).toBeDefined()

            // If custom config provided, verify it's applied
            if (visualMapConfig) {
              const vm = option.visualMap as any
              if (visualMapConfig.calculable !== undefined) {
                expect(vm.calculable).toBe(visualMapConfig.calculable)
              }
            }

            return true
          }
        ),
        { numRuns: 100 }
      )
    })

    it('should handle empty data gracefully', () => {
      const emptyData: MapDataItem[] = []

      const chinaOption = buildChinaMapOption(emptyData)
      const worldOption = buildWorldMapOption(emptyData)

      // Should still return valid options
      expect(chinaOption.series).toBeDefined()
      expect(worldOption.series).toBeDefined()

      // Series data should be empty
      const chinaSeries = chinaOption.series as any[]
      const worldSeries = worldOption.series as any[]
      expect(chinaSeries[0].data).toHaveLength(0)
      expect(worldSeries[0].data).toHaveLength(0)
    })

    it('should calculate data range correctly for visualMap', () => {
      fc.assert(
        fc.property(chinaProvinceDataArb, (data) => {
          if (data.length === 0) return true

          const option = buildChinaMapOption(data)
          const vm = option.visualMap as any

          const values = data.map(d => d.value)
          const minVal = Math.min(...values)
          const maxVal = Math.max(...values)

          // VisualMap min should be <= actual min
          expect(vm.min).toBeLessThanOrEqual(minVal)
          // VisualMap max should be >= actual max
          expect(vm.max).toBeGreaterThanOrEqual(maxVal)

          return true
        }),
        { numRuns: 100 }
      )
    })

    it('should preserve additional data properties', () => {
      fc.assert(
        fc.property(
          fc.array(
            fc.record({
              name: fc.constantFrom(...CHINA_PROVINCES),
              value: fc.float({ min: 0, max: 10000, noNaN: true }),
              extraField: fc.string({ minLength: 1, maxLength: 20 })
            }),
            { minLength: 1, maxLength: 10 }
          ).map(arr => {
            const seen = new Set<string>()
            return arr.filter(item => {
              if (seen.has(item.name)) return false
              seen.add(item.name)
              return true
            })
          }),
          (data) => {
            if (data.length === 0) return true

            const option = buildChinaMapOption(data)
            const series = option.series as any[]
            const seriesData = series[0].data

            // Verify extra properties are preserved
            for (const inputItem of data) {
              const outputItem = seriesData.find((d: any) => d.name === inputItem.name)
              expect(outputItem).toBeDefined()
              expect(outputItem.extraField).toBe(inputItem.extraField)
            }

            return true
          }
        ),
        { numRuns: 100 }
      )
    })
  })

  describe('Province Map Tests', () => {
    const cityNames = ['广州', '深圳', '东莞', '佛山', '珠海', '中山', '惠州', '江门']

    const cityDataArb = fc.array(
      fc.record({
        name: fc.constantFrom(...cityNames),
        value: fc.float({ min: 0, max: 10000, noNaN: true })
      }),
      { minLength: 1, maxLength: 8 }
    ).map(arr => {
      const seen = new Set<string>()
      return arr.filter(item => {
        if (seen.has(item.name)) return false
        seen.add(item.name)
        return true
      })
    })

    it('should preserve all input data in province map option', () => {
      fc.assert(
        fc.property(cityDataArb, (data) => {
          if (data.length === 0) return true

          const option = buildProvinceMapOption('广东', data)

          // Verify data integrity
          expect(validateMapDataIntegrity(data, option)).toBe(true)

          const series = option.series as any[]
          const seriesData = series[0].data as MapDataItem[]

          // Verify each input item exists in output
          for (const inputItem of data) {
            const outputItem = seriesData.find(d => d.name === inputItem.name)
            expect(outputItem).toBeDefined()
            expect(outputItem?.value).toBe(inputItem.value)
          }

          return true
        }),
        { numRuns: 100 }
      )
    })
  })
})

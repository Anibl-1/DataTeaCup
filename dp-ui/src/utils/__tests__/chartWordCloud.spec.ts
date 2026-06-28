/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 词云图工具函数单元测试
 */

import { describe, it, expect } from 'vitest'
import {
  buildWordCloudOption,
  validateWordCloudDataIntegrity,
  extractWordFrequency,
  generateRandomWordCloudData,
  type WordCloudDataItem
} from '../chartWordCloud'

describe('chartWordCloud', () => {
  describe('buildWordCloudOption', () => {
    it('should build valid ECharts option', () => {
      const data: WordCloudDataItem[] = [
        { name: '数据分析', value: 100 },
        { name: '可视化', value: 80 },
        { name: '机器学习', value: 60 }
      ]

      const option = buildWordCloudOption(data, { title: '热门词汇' })

      expect(option.title).toBeDefined()
      expect(option.title?.text).toBe('热门词汇')
      expect(option.series).toBeDefined()
      expect(Array.isArray(option.series)).toBe(true)
    })

    it('should handle empty data with graphic placeholder', () => {
      const option = buildWordCloudOption([])

      expect(option.graphic).toBeDefined()
    })

    it('should apply custom shape', () => {
      const data: WordCloudDataItem[] = [
        { name: 'A', value: 100 }
      ]

      const option = buildWordCloudOption(data, { shape: 'diamond' })

      const series = option.series as any[]
      expect(series[0].shape).toBe('diamond')
    })

    it('should apply custom color range', () => {
      const data: WordCloudDataItem[] = [
        { name: 'A', value: 100 },
        { name: 'B', value: 80 }
      ]

      const customColors = ['#ff0000', '#00ff00', '#0000ff']
      const option = buildWordCloudOption(data, { colorRange: customColors })

      const series = option.series as any[]
      // 验证数据项有颜色
      expect(series[0].data[0].textStyle.color).toBeDefined()
    })

    it('should apply font size range', () => {
      const data: WordCloudDataItem[] = [
        { name: 'A', value: 100 }
      ]

      const option = buildWordCloudOption(data, {
        minFontSize: 20,
        maxFontSize: 80
      })

      const series = option.series as any[]
      expect(series[0].sizeRange).toEqual([20, 80])
    })

    it('should apply rotation settings', () => {
      const data: WordCloudDataItem[] = [
        { name: 'A', value: 100 }
      ]

      const option = buildWordCloudOption(data, {
        rotationRange: [-90, 90],
        rotationStep: 30
      })

      const series = option.series as any[]
      expect(series[0].rotationRange).toEqual([-90, 90])
      expect(series[0].rotationStep).toBe(30)
    })

    it('should include tooltip', () => {
      const data: WordCloudDataItem[] = [
        { name: 'A', value: 100 }
      ]

      const option = buildWordCloudOption(data)

      expect(option.tooltip).toBeDefined()
      expect(option.tooltip?.show).toBe(true)
    })
  })

  describe('validateWordCloudDataIntegrity', () => {
    it('should return true when all data is present', () => {
      const data: WordCloudDataItem[] = [
        { name: 'A', value: 100 },
        { name: 'B', value: 80 }
      ]

      const option = buildWordCloudOption(data)

      expect(validateWordCloudDataIntegrity(data, option)).toBe(true)
    })

    it('should return true for empty data', () => {
      const option = buildWordCloudOption([])

      expect(validateWordCloudDataIntegrity([], option)).toBe(true)
    })

    it('should return false when data is missing', () => {
      const data: WordCloudDataItem[] = [
        { name: 'A', value: 100 }
      ]

      const option = buildWordCloudOption(data)
      
      // 修改 option 移除数据
      const series = option.series as any[]
      series[0].data = []

      expect(validateWordCloudDataIntegrity(data, option)).toBe(false)
    })
  })

  describe('extractWordFrequency', () => {
    it('should extract word frequency from text', () => {
      const text = 'hello world hello test hello'
      const result = extractWordFrequency(text, 2, 10)

      expect(result.length).toBeGreaterThan(0)
      
      const helloItem = result.find(item => item.name === 'hello')
      expect(helloItem).toBeDefined()
      expect(helloItem?.value).toBe(3)
    })

    it('should filter words by minimum length', () => {
      const text = 'a ab abc abcd'
      const result = extractWordFrequency(text, 3, 10)

      expect(result.find(item => item.name === 'a')).toBeUndefined()
      expect(result.find(item => item.name === 'ab')).toBeUndefined()
      expect(result.find(item => item.name === 'abc')).toBeDefined()
    })

    it('should limit results to topN', () => {
      const text = 'a b c d e f g h i j k l m n o p q r s t'
      const result = extractWordFrequency(text, 1, 5)

      expect(result.length).toBeLessThanOrEqual(5)
    })

    it('should handle empty text', () => {
      const result = extractWordFrequency('', 2, 10)

      expect(result).toEqual([])
    })

    it('should handle Chinese text', () => {
      const text = '数据分析 数据分析 可视化 机器学习'
      const result = extractWordFrequency(text, 2, 10)

      expect(result.length).toBeGreaterThan(0)
    })
  })

  describe('generateRandomWordCloudData', () => {
    it('should generate specified number of items', () => {
      const result = generateRandomWordCloudData(10, 10, 100)

      expect(result.length).toBe(10)
    })

    it('should generate values within range', () => {
      const result = generateRandomWordCloudData(20, 50, 150)

      result.forEach(item => {
        expect(item.value).toBeGreaterThanOrEqual(50)
        expect(item.value).toBeLessThanOrEqual(150)
      })
    })

    it('should generate unique names', () => {
      const result = generateRandomWordCloudData(20, 10, 100)
      const names = result.map(item => item.name)
      const uniqueNames = new Set(names)

      expect(uniqueNames.size).toBe(names.length)
    })

    it('should handle count larger than sample words', () => {
      const result = generateRandomWordCloudData(100, 10, 100)

      // Should not exceed sample words length
      expect(result.length).toBeLessThanOrEqual(30) // Sample words length
    })
  })
})

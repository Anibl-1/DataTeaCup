/**
 * 图表数据处理工具测试
 */

import { describe, it, expect } from 'vitest'
import {
  aggregateData,
  sortData,
  filterData,
  calculateStatistics,
  bucketData,
  deduplicateData,
  fillMissingValues
} from '../chartDataProcessor'

describe('chartDataProcessor', () => {
  const sampleData = [
    { category: 'A', value: 10, date: '2024-01-01' },
    { category: 'A', value: 20, date: '2024-01-02' },
    { category: 'B', value: 30, date: '2024-01-01' },
    { category: 'B', value: 40, date: '2024-01-02' },
    { category: 'A', value: 15, date: '2024-01-03' }
  ]

  describe('aggregateData', () => {
    it('should aggregate data by group field with SUM', () => {
      const result = aggregateData(sampleData, 'category', { value: 'SUM' })
      expect(result).toHaveLength(2)
      
      const groupA = result.find(r => r.category === 'A')
      const groupB = result.find(r => r.category === 'B')
      
      expect(groupA?.value).toBe(45) // 10 + 20 + 15
      expect(groupB?.value).toBe(70) // 30 + 40
    })

    it('should aggregate data with AVG', () => {
      const result = aggregateData(sampleData, 'category', { value: 'AVG' })
      
      const groupA = result.find(r => r.category === 'A')
      expect(groupA?.value).toBe(15) // (10 + 20 + 15) / 3
    })

    it('should aggregate data with COUNT', () => {
      const result = aggregateData(sampleData, 'category', { value: 'COUNT' })
      
      const groupA = result.find(r => r.category === 'A')
      expect(groupA?.value).toBe(3)
    })
  })

  describe('sortData', () => {
    it('should sort data ascending', () => {
      const result = sortData(sampleData, 'value', 'asc')
      expect(result[0].value).toBe(10)
      expect(result[result.length - 1].value).toBe(40)
    })

    it('should sort data descending', () => {
      const result = sortData(sampleData, 'value', 'desc')
      expect(result[0].value).toBe(40)
      expect(result[result.length - 1].value).toBe(10)
    })
  })

  describe('filterData', () => {
    it('should filter data with equals operator', () => {
      const result = filterData(sampleData, [
        { field: 'category', operator: '=', value: 'A' }
      ])
      expect(result).toHaveLength(3)
      expect(result.every(r => r.category === 'A')).toBe(true)
    })

    it('should filter data with greater than operator', () => {
      const result = filterData(sampleData, [
        { field: 'value', operator: '>', value: 20 }
      ])
      expect(result).toHaveLength(2)
      expect(result.every(r => r.value > 20)).toBe(true)
    })

    it('should filter data with LIKE operator', () => {
      const result = filterData(sampleData, [
        { field: 'date', operator: 'LIKE', value: '2024-01-01' }
      ])
      expect(result).toHaveLength(2)
    })
  })

  describe('calculateStatistics', () => {
    it('should calculate correct statistics', () => {
      const stats = calculateStatistics(sampleData, 'value')
      
      expect(stats.count).toBe(5)
      expect(stats.sum).toBe(115)
      expect(stats.avg).toBe(23)
      expect(stats.min).toBe(10)
      expect(stats.max).toBe(40)
    })

    it('should handle empty data', () => {
      const stats = calculateStatistics([], 'value')
      expect(stats.count).toBe(0)
    })
  })

  describe('bucketData', () => {
    it('should create correct number of buckets', () => {
      const result = bucketData(sampleData, 'value', 3)
      expect(result).toHaveLength(3)
    })

    it('should count items in buckets correctly', () => {
      const result = bucketData(sampleData, 'value', 3)
      const totalCount = result.reduce((sum, b) => sum + b.count, 0)
      expect(totalCount).toBe(5)
    })
  })

  describe('deduplicateData', () => {
    it('should remove duplicates based on key fields', () => {
      const dataWithDupes = [
        { id: 1, name: 'A' },
        { id: 1, name: 'A' },
        { id: 2, name: 'B' }
      ]
      const result = deduplicateData(dataWithDupes, ['id'])
      expect(result).toHaveLength(2)
    })
  })

  describe('fillMissingValues', () => {
    it('should fill missing values with zero', () => {
      const dataWithNull = [
        { value: 10 },
        { value: null },
        { value: 30 }
      ]
      const result = fillMissingValues(dataWithNull, 'value', 'zero')
      expect(result[1].value).toBe(0)
    })

    it('should fill missing values with mean', () => {
      const dataWithNull = [
        { value: 10 },
        { value: null },
        { value: 30 }
      ]
      const result = fillMissingValues(dataWithNull, 'value', 'mean')
      expect(result[1].value).toBe(20) // (10 + 30) / 2
    })
  })
})

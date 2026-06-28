import { describe, it, expect } from 'vitest'
import { filterData } from '../filter'
import type { FilterCondition } from '@/types/api'

// ==================== filterData 基础行为 ====================

describe('filterData', () => {
  const sampleData = [
    { id: 1, name: 'Alice', age: 30, city: 'Beijing' },
    { id: 2, name: 'Bob', age: 25, city: 'Shanghai' },
    { id: 3, name: 'Charlie', age: 35, city: 'Beijing' },
    { id: 4, name: 'David', age: 28, city: 'Guangzhou' },
  ]

  describe('empty / no filters', () => {
    it('should return all data when filters is empty array', () => {
      expect(filterData(sampleData, [])).toEqual(sampleData)
    })

    it('should return all data when filters is null-ish', () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      expect(filterData(sampleData, null as any)).toEqual(sampleData)
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      expect(filterData(sampleData, undefined as any)).toEqual(sampleData)
    })

    it('should return empty array when data is empty', () => {
      const filters: FilterCondition[] = [{ field: 'name', operator: 'eq', value: 'Alice' }]
      expect(filterData([], filters)).toEqual([])
    })
  })

  describe('invalid filter conditions', () => {
    it('should skip filter with empty field', () => {
      const filters: FilterCondition[] = [{ field: '', operator: 'eq', value: 'Alice' }]
      expect(filterData(sampleData, filters)).toEqual(sampleData)
    })

    it('should skip filter with empty operator', () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const filters: FilterCondition[] = [{ field: 'name', operator: '' as any, value: 'Alice' }]
      expect(filterData(sampleData, filters)).toEqual(sampleData)
    })

    it('should pass through on unknown operator', () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const filters: FilterCondition[] = [{ field: 'name', operator: 'unknown' as any, value: 'x' }]
      expect(filterData(sampleData, filters)).toEqual(sampleData)
    })
  })

  // ==================== eq / ne ====================

  describe('eq operator', () => {
    it('should match exact string value', () => {
      const filters: FilterCondition[] = [{ field: 'name', operator: 'eq', value: 'Alice' }]
      const result = filterData(sampleData, filters)
      expect(result).toHaveLength(1)
      expect(result[0].name).toBe('Alice')
    })

    it('should match numeric value via string comparison', () => {
      const filters: FilterCondition[] = [{ field: 'age', operator: 'eq', value: 30 }]
      const result = filterData(sampleData, filters)
      expect(result).toHaveLength(1)
      expect(result[0].name).toBe('Alice')
    })

    it('should return empty when no match', () => {
      const filters: FilterCondition[] = [{ field: 'name', operator: 'eq', value: 'Nobody' }]
      expect(filterData(sampleData, filters)).toHaveLength(0)
    })
  })

  describe('ne operator', () => {
    it('should exclude matching rows', () => {
      const filters: FilterCondition[] = [{ field: 'city', operator: 'ne', value: 'Beijing' }]
      const result = filterData(sampleData, filters)
      expect(result).toHaveLength(2)
      expect(result.every(r => r.city !== 'Beijing')).toBe(true)
    })
  })

  // ==================== contains / notContains ====================

  describe('contains operator', () => {
    it('should match substring case-insensitively', () => {
      const filters: FilterCondition[] = [{ field: 'name', operator: 'contains', value: 'ali' }]
      const result = filterData(sampleData, filters)
      expect(result).toHaveLength(1)
      expect(result[0].name).toBe('Alice')
    })

    it('should match when value appears anywhere in string', () => {
      const filters: FilterCondition[] = [{ field: 'name', operator: 'contains', value: 'ob' }]
      expect(filterData(sampleData, filters)).toHaveLength(1)
    })
  })

  describe('notContains operator', () => {
    it('should exclude rows containing substring', () => {
      const filters: FilterCondition[] = [{ field: 'name', operator: 'notContains', value: 'a' }]
      const result = filterData(sampleData, filters)
      // 'a' appears in Alice, Charlie, David (case-insensitive) → only Bob remains
      expect(result).toHaveLength(1)
      expect(result[0].name).toBe('Bob')
    })
  })

  // ==================== startsWith / endsWith ====================

  describe('startsWith operator', () => {
    it('should match prefix case-insensitively', () => {
      const filters: FilterCondition[] = [{ field: 'name', operator: 'startsWith', value: 'ch' }]
      const result = filterData(sampleData, filters)
      expect(result).toHaveLength(1)
      expect(result[0].name).toBe('Charlie')
    })
  })

  describe('endsWith operator', () => {
    it('should match suffix case-insensitively', () => {
      const filters: FilterCondition[] = [{ field: 'name', operator: 'endsWith', value: 'id' }]
      const result = filterData(sampleData, filters)
      expect(result).toHaveLength(1)
      expect(result[0].name).toBe('David')
    })
  })

  // ==================== numeric comparisons ====================

  describe('gt operator', () => {
    it('should filter values greater than threshold', () => {
      const filters: FilterCondition[] = [{ field: 'age', operator: 'gt', value: 30 }]
      const result = filterData(sampleData, filters)
      expect(result).toHaveLength(1)
      expect(result[0].name).toBe('Charlie')
    })
  })

  describe('gte operator', () => {
    it('should include equal values', () => {
      const filters: FilterCondition[] = [{ field: 'age', operator: 'gte', value: 30 }]
      const result = filterData(sampleData, filters)
      expect(result).toHaveLength(2)
    })
  })

  describe('lt operator', () => {
    it('should filter values less than threshold', () => {
      const filters: FilterCondition[] = [{ field: 'age', operator: 'lt', value: 28 }]
      const result = filterData(sampleData, filters)
      expect(result).toHaveLength(1)
      expect(result[0].name).toBe('Bob')
    })
  })

  describe('lte operator', () => {
    it('should include equal values', () => {
      const filters: FilterCondition[] = [{ field: 'age', operator: 'lte', value: 28 }]
      const result = filterData(sampleData, filters)
      expect(result).toHaveLength(2)
    })
  })

  // ==================== isNull / isNotNull ====================

  describe('isNull operator', () => {
    const dataWithNulls = [
      { id: 1, name: 'Alice', note: null },
      { id: 2, name: 'Bob', note: undefined },
      { id: 3, name: 'Charlie', note: '' },
      { id: 4, name: 'David', note: 'some note' },
    ]

    it('should match null, undefined, and empty string', () => {
      const filters: FilterCondition[] = [{ field: 'note', operator: 'isNull' }]
      const result = filterData(dataWithNulls, filters)
      expect(result).toHaveLength(3)
    })
  })

  describe('isNotNull operator', () => {
    const dataWithNulls = [
      { id: 1, name: 'Alice', note: null },
      { id: 2, name: 'Bob', note: 'hello' },
    ]

    it('should match only non-null, non-undefined, non-empty values', () => {
      const filters: FilterCondition[] = [{ field: 'note', operator: 'isNotNull' }]
      const result = filterData(dataWithNulls, filters)
      expect(result).toHaveLength(1)
      expect(result[0].name).toBe('Bob')
    })
  })

  // ==================== multiple filters (AND logic) ====================

  describe('multiple filters', () => {
    it('should apply all filters with AND logic', () => {
      const filters: FilterCondition[] = [
        { field: 'city', operator: 'eq', value: 'Beijing' },
        { field: 'age', operator: 'gt', value: 30 },
      ]
      const result = filterData(sampleData, filters)
      expect(result).toHaveLength(1)
      expect(result[0].name).toBe('Charlie')
    })
  })

  // ==================== nested field access ====================

  describe('nested field access', () => {
    const nestedData = [
      { id: 1, profile: { address: { city: 'Beijing' } } },
      { id: 2, profile: { address: { city: 'Shanghai' } } },
    ]

    it('should access nested fields via dot notation', () => {
      const filters: FilterCondition[] = [
        { field: 'profile.address.city', operator: 'eq', value: 'Beijing' },
      ]
      const result = filterData(nestedData, filters)
      expect(result).toHaveLength(1)
      expect(result[0].id).toBe(1)
    })

    it('should return undefined for non-existent nested path', () => {
      const filters: FilterCondition[] = [
        { field: 'profile.nonexistent.field', operator: 'isNull' },
      ]
      const result = filterData(nestedData, filters)
      expect(result).toHaveLength(2)
    })

    it('should handle null in nested path gracefully', () => {
      const dataWithNull = [{ id: 1, profile: null }]
      const filters: FilterCondition[] = [
        { field: 'profile.address.city', operator: 'isNull' },
      ]
      const result = filterData(dataWithNull, filters)
      expect(result).toHaveLength(1)
    })
  })
})

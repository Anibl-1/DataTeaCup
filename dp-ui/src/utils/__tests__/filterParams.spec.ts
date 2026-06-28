import { describe, it, expect, vi } from 'vitest'
import { filtersToApiParam, apiParamToFilters } from '../filterParams'
import type { FilterCondition } from '@/types/api'

// ==================== filtersToApiParam ====================

describe('filtersToApiParam', () => {
  describe('empty / invalid input', () => {
    it('should return undefined for empty array', () => {
      expect(filtersToApiParam([])).toBeUndefined()
    })

    it('should return undefined for null-ish input', () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      expect(filtersToApiParam(null as any)).toBeUndefined()
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      expect(filtersToApiParam(undefined as any)).toBeUndefined()
    })

    it('should return undefined when all filters are invalid (no field)', () => {
      const filters: FilterCondition[] = [
        { field: '', operator: 'eq', value: 'test' },
      ]
      expect(filtersToApiParam(filters)).toBeUndefined()
    })

    it('should return undefined when all filters are invalid (no operator)', () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const filters: FilterCondition[] = [
        { field: 'name', operator: '' as any, value: 'test' },
      ]
      expect(filtersToApiParam(filters)).toBeUndefined()
    })

    it('should return undefined when value is undefined for non-null operators', () => {
      const filters: FilterCondition[] = [
        { field: 'name', operator: 'eq' },
      ]
      expect(filtersToApiParam(filters)).toBeUndefined()
    })
  })

  describe('valid filters', () => {
    it('should serialize a single filter to JSON', () => {
      const filters: FilterCondition[] = [
        { field: 'name', operator: 'eq', value: 'Alice' },
      ]
      const result = filtersToApiParam(filters)
      expect(result).toBeDefined()
      const parsed = JSON.parse(result!)
      expect(parsed).toHaveLength(1)
      expect(parsed[0]).toEqual({ field: 'name', operator: 'eq', value: 'Alice' })
    })

    it('should serialize multiple filters', () => {
      const filters: FilterCondition[] = [
        { field: 'name', operator: 'contains', value: 'test' },
        { field: 'age', operator: 'gt', value: 18 },
      ]
      const result = filtersToApiParam(filters)
      const parsed = JSON.parse(result!)
      expect(parsed).toHaveLength(2)
    })

    it('should allow isNull operator without value', () => {
      const filters: FilterCondition[] = [
        { field: 'note', operator: 'isNull' },
      ]
      const result = filtersToApiParam(filters)
      expect(result).toBeDefined()
      const parsed = JSON.parse(result!)
      expect(parsed).toHaveLength(1)
      expect(parsed[0].operator).toBe('isNull')
    })

    it('should allow isNotNull operator without value', () => {
      const filters: FilterCondition[] = [
        { field: 'note', operator: 'isNotNull' },
      ]
      const result = filtersToApiParam(filters)
      expect(result).toBeDefined()
      const parsed = JSON.parse(result!)
      expect(parsed[0].operator).toBe('isNotNull')
    })

    it('should filter out invalid entries and keep valid ones', () => {
      const filters: FilterCondition[] = [
        { field: '', operator: 'eq', value: 'skip' },
        { field: 'name', operator: 'eq', value: 'keep' },
      ]
      const result = filtersToApiParam(filters)
      const parsed = JSON.parse(result!)
      expect(parsed).toHaveLength(1)
      expect(parsed[0].value).toBe('keep')
    })

    it('should handle special characters in values', () => {
      const filters: FilterCondition[] = [
        { field: 'name', operator: 'contains', value: 'hello "world" & <test>' },
      ]
      const result = filtersToApiParam(filters)
      const parsed = JSON.parse(result!)
      expect(parsed[0].value).toBe('hello "world" & <test>')
    })

    it('should handle numeric zero as a valid value', () => {
      const filters: FilterCondition[] = [
        { field: 'count', operator: 'eq', value: 0 },
      ]
      const result = filtersToApiParam(filters)
      expect(result).toBeDefined()
      const parsed = JSON.parse(result!)
      expect(parsed[0].value).toBe(0)
    })
  })
})

// ==================== apiParamToFilters ====================

describe('apiParamToFilters', () => {
  describe('empty / invalid input', () => {
    it('should return empty array for undefined', () => {
      expect(apiParamToFilters(undefined)).toEqual([])
    })

    it('should return empty array for empty string', () => {
      expect(apiParamToFilters('')).toEqual([])
    })

    it('should return empty array for invalid JSON', () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
      expect(apiParamToFilters('not-json')).toEqual([])
      consoleSpy.mockRestore()
    })

    it('should return empty array when parsed result is not an array', () => {
      expect(apiParamToFilters('{"field":"name"}')).toEqual([])
    })

    it('should return empty array for JSON number', () => {
      expect(apiParamToFilters('42')).toEqual([])
    })

    it('should return empty array for JSON string', () => {
      expect(apiParamToFilters('"hello"')).toEqual([])
    })
  })

  describe('valid JSON arrays', () => {
    it('should parse a single filter', () => {
      const json = JSON.stringify([{ field: 'name', operator: 'eq', value: 'Alice' }])
      const result = apiParamToFilters(json)
      expect(result).toHaveLength(1)
      expect(result[0]).toEqual({ field: 'name', operator: 'eq', value: 'Alice' })
    })

    it('should parse multiple filters', () => {
      const filters = [
        { field: 'name', operator: 'contains', value: 'test' },
        { field: 'age', operator: 'gt', value: 18 },
      ]
      const result = apiParamToFilters(JSON.stringify(filters))
      expect(result).toHaveLength(2)
      expect(result).toEqual(filters)
    })

    it('should parse empty array', () => {
      expect(apiParamToFilters('[]')).toEqual([])
    })

    it('should preserve special characters in values', () => {
      const filters = [{ field: 'name', operator: 'eq', value: 'hello "world"' }]
      const result = apiParamToFilters(JSON.stringify(filters))
      expect(result[0].value).toBe('hello "world"')
    })
  })

  // ==================== round-trip ====================

  describe('round-trip: filtersToApiParam → apiParamToFilters', () => {
    it('should round-trip a standard filter set', () => {
      const original: FilterCondition[] = [
        { field: 'name', operator: 'eq', value: 'Alice' },
        { field: 'age', operator: 'gte', value: 18 },
        { field: 'city', operator: 'contains', value: 'Bei' },
      ]
      const serialized = filtersToApiParam(original)
      const deserialized = apiParamToFilters(serialized)
      expect(deserialized).toEqual(original)
    })

    it('should round-trip isNull filter (no value)', () => {
      const original: FilterCondition[] = [
        { field: 'note', operator: 'isNull' },
      ]
      const serialized = filtersToApiParam(original)
      const deserialized = apiParamToFilters(serialized)
      expect(deserialized).toHaveLength(1)
      expect(deserialized[0].field).toBe('note')
      expect(deserialized[0].operator).toBe('isNull')
    })
  })
})

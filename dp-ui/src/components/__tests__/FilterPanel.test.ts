import { describe, it, expect } from 'vitest'
import { filterData } from '@/utils/filter'
import type { FilterCondition } from '@/types/api'

/**
 * Tests for FilterPanel enhanced functionality.
 * Since FilterPanel is a Vue component with Naive UI dependencies,
 * we test the core filter logic through the filterData utility
 * which processes FilterCondition objects emitted by FilterPanel.
 *
 * Validates: Requirements 3.2, 7.4
 */

const sampleData = [
  { id: 1, name: 'MySQL Production', dbType: 'mysql', status: 'active', port: 3306 },
  { id: 2, name: 'PostgreSQL Dev', dbType: 'postgresql', status: 'inactive', port: 5432 },
  { id: 3, name: 'MySQL Staging', dbType: 'mysql', status: 'active', port: 3307 },
  { id: 4, name: 'Oracle Legacy', dbType: 'oracle', status: 'inactive', port: 1521 },
  { id: 5, name: 'Redis Cache', dbType: 'redis', status: 'active', port: 6379 }
]

describe('FilterPanel - dynamic filter conditions (field/operator/value)', () => {
  describe('like operator', () => {
    it('should match substring case-insensitively', () => {
      const filters: FilterCondition[] = [
        { field: 'name', operator: 'like', value: 'mysql' }
      ]
      const result = filterData(sampleData, filters)
      expect(result).toHaveLength(2)
      expect(result.map(r => r.id)).toEqual([1, 3])
    })

    it('should return empty when no match', () => {
      const filters: FilterCondition[] = [
        { field: 'name', operator: 'like', value: 'nonexistent' }
      ]
      expect(filterData(sampleData, filters)).toHaveLength(0)
    })
  })

  describe('in operator', () => {
    it('should match any value in the array', () => {
      const filters: FilterCondition[] = [
        { field: 'dbType', operator: 'in', value: ['mysql', 'oracle'] }
      ]
      const result = filterData(sampleData, filters)
      expect(result).toHaveLength(3)
      expect(result.map(r => r.id)).toEqual([1, 3, 4])
    })

    it('should return empty when no values match', () => {
      const filters: FilterCondition[] = [
        { field: 'dbType', operator: 'in', value: ['mongodb', 'cassandra'] }
      ]
      expect(filterData(sampleData, filters)).toHaveLength(0)
    })

    it('should handle single-value array', () => {
      const filters: FilterCondition[] = [
        { field: 'dbType', operator: 'in', value: ['redis'] }
      ]
      const result = filterData(sampleData, filters)
      expect(result).toHaveLength(1)
      expect(result[0].id).toBe(5)
    })

    it('should handle non-array value as single match', () => {
      const filters: FilterCondition[] = [
        { field: 'dbType', operator: 'in', value: 'mysql' }
      ]
      const result = filterData(sampleData, filters)
      expect(result).toHaveLength(2)
    })
  })

  describe('combined filters (DataSource integration scenario)', () => {
    it('should filter by dbType and status together', () => {
      const filters: FilterCondition[] = [
        { field: 'dbType', operator: 'eq', value: 'mysql' },
        { field: 'status', operator: 'eq', value: 'active' }
      ]
      const result = filterData(sampleData, filters)
      expect(result).toHaveLength(2)
      expect(result.every(r => r.dbType === 'mysql' && r.status === 'active')).toBe(true)
    })

    it('should filter by name like and status', () => {
      const filters: FilterCondition[] = [
        { field: 'name', operator: 'like', value: 'prod' },
        { field: 'status', operator: 'eq', value: 'active' }
      ]
      const result = filterData(sampleData, filters)
      expect(result).toHaveLength(1)
      expect(result[0].name).toBe('MySQL Production')
    })

    it('should filter by port range', () => {
      const filters: FilterCondition[] = [
        { field: 'port', operator: 'gte', value: 3000 },
        { field: 'port', operator: 'lte', value: 5500 }
      ]
      const result = filterData(sampleData, filters)
      expect(result).toHaveLength(3)
    })
  })

  describe('combined filters (Role integration scenario)', () => {
    const roleData = [
      { id: 1, roleName: '管理员', roleCode: 'admin', status: 1 },
      { id: 2, roleName: '编辑者', roleCode: 'editor', status: 1 },
      { id: 3, roleName: '查看者', roleCode: 'viewer', status: 0 },
      { id: 4, roleName: '审计员', roleCode: 'auditor', status: 1 }
    ]

    it('should filter roles by name like', () => {
      const filters: FilterCondition[] = [
        { field: 'roleName', operator: 'like', value: '员' }
      ]
      const result = filterData(roleData, filters)
      expect(result).toHaveLength(2)
    })

    it('should filter roles by status and code', () => {
      const filters: FilterCondition[] = [
        { field: 'status', operator: 'eq', value: 1 },
        { field: 'roleCode', operator: 'like', value: 'edit' }
      ]
      const result = filterData(roleData, filters)
      expect(result).toHaveLength(1)
      expect(result[0].roleCode).toBe('editor')
    })

    it('should filter roles using in operator', () => {
      const filters: FilterCondition[] = [
        { field: 'roleCode', operator: 'in', value: ['admin', 'editor'] }
      ]
      const result = filterData(roleData, filters)
      expect(result).toHaveLength(2)
    })
  })
})

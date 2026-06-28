/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, it, expect } from 'vitest'
import {
  parseFilterPlaceholders,
  applyFiltersToSql,
  buildFilterParams,
  filterParamsToQueryString,
  filterParamsToJson,
  hasFilterPlaceholders,
  getRequiredFilterFields,
  validateFiltersForSql,
  buildSqlWithDefaults,
  createFilterLinker
} from '../filterLinker'

describe('filterLinker', () => {
  describe('parseFilterPlaceholders', () => {
    it('should parse ${field} format placeholders', () => {
      const sql = "SELECT * FROM sales WHERE region = '${region}' AND date >= '${startDate}'"
      const placeholders = parseFilterPlaceholders(sql)
      
      expect(placeholders).toHaveLength(2)
      expect(placeholders[0]).toEqual({
        placeholder: '${region}',
        field: 'region',
        format: 'dollar'
      })
      expect(placeholders[1]).toEqual({
        placeholder: '${startDate}',
        field: 'startDate',
        format: 'dollar'
      })
    })

    it('should parse {{field}} format placeholders', () => {
      const sql = "SELECT * FROM sales WHERE region = '{{region}}' AND date >= '{{startDate}}'"
      const placeholders = parseFilterPlaceholders(sql)
      
      expect(placeholders).toHaveLength(2)
      expect(placeholders[0]).toEqual({
        placeholder: '{{region}}',
        field: 'region',
        format: 'mustache'
      })
      expect(placeholders[1]).toEqual({
        placeholder: '{{startDate}}',
        field: 'startDate',
        format: 'mustache'
      })
    })

    it('should parse mixed format placeholders', () => {
      const sql = "SELECT * FROM sales WHERE region = '${region}' AND date >= '{{startDate}}'"
      const placeholders = parseFilterPlaceholders(sql)
      
      expect(placeholders).toHaveLength(2)
      expect(placeholders[0].format).toBe('dollar')
      expect(placeholders[1].format).toBe('mustache')
    })

    it('should return empty array for SQL without placeholders', () => {
      const sql = "SELECT * FROM sales WHERE region = 'north'"
      const placeholders = parseFilterPlaceholders(sql)
      
      expect(placeholders).toHaveLength(0)
    })

    it('should return empty array for empty SQL', () => {
      expect(parseFilterPlaceholders('')).toHaveLength(0)
      expect(parseFilterPlaceholders(null as any)).toHaveLength(0)
    })
  })

  describe('applyFiltersToSql', () => {
    it('should replace string filter values', () => {
      const sql = "SELECT * FROM sales WHERE region = '${region}'"
      const result = applyFiltersToSql(sql, { region: 'north' })
      
      expect(result).toBe("SELECT * FROM sales WHERE region = ''north''")
    })

    it('should replace number filter values', () => {
      const sql = 'SELECT * FROM sales WHERE amount > ${minAmount}'
      const result = applyFiltersToSql(sql, { minAmount: 1000 })
      
      expect(result).toBe('SELECT * FROM sales WHERE amount > 1000')
    })

    it('should replace boolean filter values', () => {
      const sql = 'SELECT * FROM sales WHERE active = ${isActive}'
      const result = applyFiltersToSql(sql, { isActive: true })
      
      expect(result).toBe('SELECT * FROM sales WHERE active = 1')
    })

    it('should handle array values for IN clause', () => {
      const sql = 'SELECT * FROM sales WHERE region IN ${regions}'
      const result = applyFiltersToSql(sql, { regions: ['north', 'south', 'east'] })
      
      expect(result).toBe("SELECT * FROM sales WHERE region IN ('north', 'south', 'east')")
    })

    it('should handle date range values', () => {
      const sql = 'SELECT * FROM sales WHERE date BETWEEN ${dateRange}'
      const result = applyFiltersToSql(sql, { dateRange: ['2024-01-01', '2024-12-31'] })
      
      expect(result).toBe("SELECT * FROM sales WHERE date BETWEEN '2024-01-01' AND '2024-12-31'")
    })

    it('should escape single quotes in string values', () => {
      const sql = "SELECT * FROM sales WHERE name = '${name}'"
      const result = applyFiltersToSql(sql, { name: "O'Brien" })
      
      expect(result).toBe("SELECT * FROM sales WHERE name = ''O''Brien''")
    })

    it('should not replace placeholders with null/empty values', () => {
      const sql = "SELECT * FROM sales WHERE region = '${region}'"
      
      expect(applyFiltersToSql(sql, { region: null })).toBe(sql)
      expect(applyFiltersToSql(sql, { region: '' })).toBe(sql)
      expect(applyFiltersToSql(sql, {})).toBe(sql)
    })

    it('should handle mustache format placeholders', () => {
      const sql = "SELECT * FROM sales WHERE region = '{{region}}'"
      const result = applyFiltersToSql(sql, { region: 'north' })
      
      expect(result).toBe("SELECT * FROM sales WHERE region = ''north''")
    })
  })

  describe('buildFilterParams', () => {
    it('should build filter params from filters and configs', () => {
      const filters = {
        filter1: 'north',
        filter2: '2024-01-01'
      }
      const filterConfigs = {
        filter1: { field: 'region', linkedCharts: ['chart1'] },
        filter2: { field: 'date', linkedCharts: ['chart1', 'chart2'] }
      }
      
      const params = buildFilterParams(filters, filterConfigs)
      
      expect(params).toHaveLength(2)
      expect(params).toContainEqual({ field: 'region', value: 'north' })
      expect(params).toContainEqual({ field: 'date', value: '2024-01-01' })
    })

    it('should filter by linked charts when specified', () => {
      const filters = {
        filter1: 'north',
        filter2: '2024-01-01'
      }
      const filterConfigs = {
        filter1: { field: 'region', linkedCharts: ['chart1'] },
        filter2: { field: 'date', linkedCharts: ['chart2'] }
      }
      
      const params = buildFilterParams(filters, filterConfigs, ['chart1'])
      
      expect(params).toHaveLength(1)
      expect(params[0]).toEqual({ field: 'region', value: 'north' })
    })

    it('should skip null/empty values', () => {
      const filters = {
        filter1: 'north',
        filter2: null,
        filter3: ''
      }
      const filterConfigs = {
        filter1: { field: 'region', linkedCharts: [] },
        filter2: { field: 'date', linkedCharts: [] },
        filter3: { field: 'category', linkedCharts: [] }
      }
      
      const params = buildFilterParams(filters, filterConfigs)
      
      expect(params).toHaveLength(1)
      expect(params[0].field).toBe('region')
    })

    it('should skip filters without config', () => {
      const filters = {
        filter1: 'north',
        filter2: 'value'
      }
      const filterConfigs = {
        filter1: { field: 'region', linkedCharts: [] }
      }
      
      const params = buildFilterParams(filters, filterConfigs)
      
      expect(params).toHaveLength(1)
    })
  })

  describe('filterParamsToQueryString', () => {
    it('should convert params to query string', () => {
      const params = [
        { field: 'region', value: 'north' },
        { field: 'date', value: '2024-01-01' }
      ]
      
      const queryString = filterParamsToQueryString(params)
      
      expect(queryString).toBe('region=north&date=2024-01-01')
    })

    it('should handle array values', () => {
      const params = [
        { field: 'regions', value: ['north', 'south'] }
      ]
      
      const queryString = filterParamsToQueryString(params)
      
      expect(queryString).toBe('regions=north%2Csouth')
    })

    it('should return empty string for empty params', () => {
      expect(filterParamsToQueryString([])).toBe('')
      expect(filterParamsToQueryString(null as any)).toBe('')
    })
  })

  describe('filterParamsToJson', () => {
    it('should convert params to JSON object', () => {
      const params = [
        { field: 'region', value: 'north' },
        { field: 'date', value: '2024-01-01' }
      ]
      
      const json = filterParamsToJson(params)
      
      expect(json).toEqual({
        region: 'north',
        date: '2024-01-01'
      })
    })
  })

  describe('hasFilterPlaceholders', () => {
    it('should return true for SQL with placeholders', () => {
      expect(hasFilterPlaceholders("SELECT * FROM t WHERE x = '${x}'")).toBe(true)
      expect(hasFilterPlaceholders("SELECT * FROM t WHERE x = '{{x}}'")).toBe(true)
    })

    it('should return false for SQL without placeholders', () => {
      expect(hasFilterPlaceholders("SELECT * FROM t WHERE x = 'value'")).toBe(false)
      expect(hasFilterPlaceholders('')).toBe(false)
    })
  })

  describe('getRequiredFilterFields', () => {
    it('should return unique field names', () => {
      const sql = "SELECT * FROM t WHERE x = '${region}' AND y = '${region}' AND z = '${date}'"
      const fields = getRequiredFilterFields(sql)
      
      expect(fields).toHaveLength(2)
      expect(fields).toContain('region')
      expect(fields).toContain('date')
    })
  })

  describe('validateFiltersForSql', () => {
    it('should return missing fields', () => {
      const sql = "SELECT * FROM t WHERE x = '${region}' AND y = '${date}'"
      const filters = { region: 'north' }
      
      const missing = validateFiltersForSql(sql, filters)
      
      expect(missing).toEqual(['date'])
    })

    it('should return empty array when all fields are provided', () => {
      const sql = "SELECT * FROM t WHERE x = '${region}'"
      const filters = { region: 'north' }
      
      const missing = validateFiltersForSql(sql, filters)
      
      expect(missing).toHaveLength(0)
    })
  })

  describe('buildSqlWithDefaults', () => {
    it('should use default values for missing filters', () => {
      const sql = "SELECT * FROM t WHERE x = '${region}' AND y = '${date}'"
      const filters = { region: 'north' }
      const defaults = { date: '2024-01-01' }
      
      const result = buildSqlWithDefaults(sql, filters, defaults)
      
      expect(result).toContain("'north'")
      expect(result).toContain("'2024-01-01'")
    })

    it('should override defaults with provided filters', () => {
      const sql = "SELECT * FROM t WHERE x = '${region}'"
      const filters = { region: 'south' }
      const defaults = { region: 'north' }
      
      const result = buildSqlWithDefaults(sql, filters, defaults)
      
      expect(result).toContain("'south'")
      expect(result).not.toContain("'north'")
    })
  })

  describe('createFilterLinker', () => {
    it('should register and retrieve chart links', () => {
      const linker = createFilterLinker()
      
      linker.registerChartLink('chart1', ['region', 'date'])
      linker.registerChartLink('chart2', ['region'])
      
      expect(linker.getChartFilters('chart1')).toEqual(['region', 'date'])
      expect(linker.getChartFilters('chart2')).toEqual(['region'])
    })

    it('should get affected charts by filter field', () => {
      const linker = createFilterLinker()
      
      linker.registerChartLink('chart1', ['region', 'date'])
      linker.registerChartLink('chart2', ['region'])
      linker.registerChartLink('chart3', ['category'])
      
      const affected = linker.getAffectedCharts('region')
      
      expect(affected).toHaveLength(2)
      expect(affected).toContain('chart1')
      expect(affected).toContain('chart2')
    })

    it('should unregister chart links', () => {
      const linker = createFilterLinker()
      
      linker.registerChartLink('chart1', ['region'])
      linker.registerChartLink('chart2', ['region'])
      
      linker.unregisterChartLink('chart1')
      
      expect(linker.getChartFilters('chart1')).toHaveLength(0)
      expect(linker.getAffectedCharts('region')).toEqual(['chart2'])
    })

    it('should clear all links', () => {
      const linker = createFilterLinker()
      
      linker.registerChartLink('chart1', ['region'])
      linker.registerChartLink('chart2', ['date'])
      
      linker.clearAllLinks()
      
      expect(linker.getChartFilters('chart1')).toHaveLength(0)
      expect(linker.getChartFilters('chart2')).toHaveLength(0)
      expect(linker.getAffectedCharts('region')).toHaveLength(0)
    })
  })
})

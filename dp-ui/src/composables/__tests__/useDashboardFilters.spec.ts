import { describe, it, expect, vi } from 'vitest'
import { useDashboardFilters } from '../useDashboardFilters'
import type { DashboardWidget, FilterWidgetConfig } from '@/types/dashboard'

describe('useDashboardFilters', () => {
  describe('basic filter operations', () => {
    it('should initialize with empty filter values', () => {
      const { filterValues, hasActiveFilters } = useDashboardFilters()
      
      expect(Object.keys(filterValues).length).toBe(0)
      expect(hasActiveFilters.value).toBe(false)
    })

    it('should set and get filter value', () => {
      const { setFilterValue, getFilterValue, filterValues } = useDashboardFilters()
      
      setFilterValue('filter1', 'value1', false)
      
      expect(getFilterValue('filter1')).toBe('value1')
      expect(filterValues['filter1']).toBe('value1')
    })

    it('should return null for non-existent filter', () => {
      const { getFilterValue } = useDashboardFilters()
      
      expect(getFilterValue('nonexistent')).toBeNull()
    })

    it('should clear all filters', () => {
      const { setFilterValue, clearFilters, filterValues, hasActiveFilters } = useDashboardFilters()
      
      setFilterValue('filter1', 'value1', false)
      setFilterValue('filter2', 'value2', false)
      
      expect(hasActiveFilters.value).toBe(true)
      
      clearFilters()
      
      expect(filterValues['filter1']).toBeNull()
      expect(filterValues['filter2']).toBeNull()
      expect(hasActiveFilters.value).toBe(false)
    })

    it('should clear single filter', () => {
      const { setFilterValue, clearFilter, getFilterValue } = useDashboardFilters()
      
      setFilterValue('filter1', 'value1', false)
      setFilterValue('filter2', 'value2', false)
      
      clearFilter('filter1')
      
      expect(getFilterValue('filter1')).toBeNull()
      expect(getFilterValue('filter2')).toBe('value2')
    })
  })

  describe('filter registration', () => {
    it('should register filter with config', () => {
      const { registerFilter, filterConfigs, filterValues } = useDashboardFilters()
      
      registerFilter('filter1', {
        field: 'region',
        linkedCharts: ['chart1', 'chart2'],
        value: 'default'
      })
      
      expect(filterConfigs.value['filter1']).toBeDefined()
      expect(filterConfigs.value['filter1']!.field).toBe('region')
      expect(filterConfigs.value['filter1']!.linkedCharts).toEqual(['chart1', 'chart2'])
      expect(filterValues['filter1']).toBe('default')
    })

    it('should register filter from widget', () => {
      const { registerFilterFromWidget, filterConfigs, filterValues } = useDashboardFilters()
      
      const widget: DashboardWidget = {
        i: 'widget1',
        x: 0,
        y: 0,
        w: 3,
        h: 3,
        type: 'filter',
        config: {
          filterType: 'select',
          label: 'Region',
          field: 'region',
          linkedCharts: ['chart1'],
          defaultValue: 'all'
        } as FilterWidgetConfig
      }
      
      registerFilterFromWidget(widget)
      
      expect(filterConfigs.value['widget1']).toBeDefined()
      expect(filterConfigs.value['widget1']!.field).toBe('region')
      expect(filterValues['widget1']).toBe('all')
    })

    it('should not register non-filter widget', () => {
      const { registerFilterFromWidget, filterConfigs } = useDashboardFilters()
      
      const widget: DashboardWidget = {
        i: 'widget1',
        x: 0,
        y: 0,
        w: 4,
        h: 6,
        type: 'chart',
        config: { title: 'Chart' }
      }
      
      registerFilterFromWidget(widget)
      
      expect(filterConfigs.value['widget1']).toBeUndefined()
    })

    it('should unregister filter', () => {
      const { registerFilter, unregisterFilter, filterConfigs, filterValues } = useDashboardFilters()
      
      registerFilter('filter1', { field: 'region', linkedCharts: [] })
      
      expect(filterConfigs.value['filter1']).toBeDefined()
      
      unregisterFilter('filter1')
      
      expect(filterConfigs.value['filter1']).toBeUndefined()
      expect(filterValues['filter1']).toBeUndefined()
    })
  })

  describe('getFiltersForChart', () => {
    it('should return filters linked to specific chart', () => {
      const { registerFilter, setFilterValue, getFiltersForChart } = useDashboardFilters()
      
      registerFilter('filter1', { field: 'region', linkedCharts: ['chart1', 'chart2'] })
      registerFilter('filter2', { field: 'date', linkedCharts: ['chart1'] })
      registerFilter('filter3', { field: 'category', linkedCharts: ['chart3'] })
      
      setFilterValue('filter1', 'north', false)
      setFilterValue('filter2', '2024-01-01', false)
      setFilterValue('filter3', 'electronics', false)
      
      const chart1Filters = getFiltersForChart('chart1')
      
      expect(chart1Filters).toEqual({
        region: 'north',
        date: '2024-01-01'
      })
    })

    it('should exclude null/empty filter values', () => {
      const { registerFilter, setFilterValue, getFiltersForChart } = useDashboardFilters()
      
      registerFilter('filter1', { field: 'region', linkedCharts: ['chart1'] })
      registerFilter('filter2', { field: 'date', linkedCharts: ['chart1'] })
      
      setFilterValue('filter1', 'north', false)
      setFilterValue('filter2', null, false)
      
      const filters = getFiltersForChart('chart1')
      
      expect(filters).toEqual({ region: 'north' })
      expect(filters['date']).toBeUndefined()
    })

    it('should return empty object for chart with no linked filters', () => {
      const { registerFilter, setFilterValue, getFiltersForChart } = useDashboardFilters()
      
      registerFilter('filter1', { field: 'region', linkedCharts: ['chart1'] })
      setFilterValue('filter1', 'north', false)
      
      const filters = getFiltersForChart('chart2')
      
      expect(filters).toEqual({})
    })
  })

  describe('filter change callback', () => {
    it('should call onFilterChange when filter value changes', () => {
      const onFilterChange = vi.fn()
      const { registerFilter, setFilterValue } = useDashboardFilters({ onFilterChange })
      
      registerFilter('filter1', { field: 'region', linkedCharts: ['chart1'] })
      setFilterValue('filter1', 'north')
      
      expect(onFilterChange).toHaveBeenCalledWith({
        filterId: 'filter1',
        field: 'region',
        value: 'north',
        linkedCharts: ['chart1']
      })
    })

    it('should not call onFilterChange when triggerChange is false', () => {
      const onFilterChange = vi.fn()
      const { registerFilter, setFilterValue } = useDashboardFilters({ onFilterChange })
      
      registerFilter('filter1', { field: 'region', linkedCharts: ['chart1'] })
      setFilterValue('filter1', 'north', false)
      
      expect(onFilterChange).not.toHaveBeenCalled()
    })

    it('should not call onFilterChange when value does not change', () => {
      const onFilterChange = vi.fn()
      const { registerFilter, setFilterValue } = useDashboardFilters({ onFilterChange })
      
      registerFilter('filter1', { field: 'region', linkedCharts: ['chart1'] })
      setFilterValue('filter1', 'north')
      
      onFilterChange.mockClear()
      
      setFilterValue('filter1', 'north')
      
      expect(onFilterChange).not.toHaveBeenCalled()
    })
  })

  describe('cascade filters', () => {
    it('should clear child filter when parent changes', () => {
      const onFilterChange = vi.fn()
      const { registerFilter, setFilterValue, getFilterValue, addCascadeConfig } = useDashboardFilters({
        onFilterChange
      })
      
      registerFilter('province', { field: 'province', linkedCharts: ['chart1'] })
      registerFilter('city', { field: 'city', linkedCharts: ['chart1'] })
      
      addCascadeConfig({
        parentFilterId: 'province',
        childFilterId: 'city',
        parentField: 'province',
        childField: 'city'
      })
      
      setFilterValue('city', 'Beijing', false)
      expect(getFilterValue('city')).toBe('Beijing')
      
      // When parent changes, child should be cleared
      setFilterValue('province', 'Guangdong')
      
      expect(getFilterValue('city')).toBeNull()
    })
  })

  describe('batch operations', () => {
    it('should set multiple filter values at once', () => {
      const { registerFilter, setFilterValues, getFilterValue } = useDashboardFilters()
      
      registerFilter('filter1', { field: 'region', linkedCharts: [] })
      registerFilter('filter2', { field: 'date', linkedCharts: [] })
      
      setFilterValues({
        filter1: 'north',
        filter2: '2024-01-01'
      }, false)
      
      expect(getFilterValue('filter1')).toBe('north')
      expect(getFilterValue('filter2')).toBe('2024-01-01')
    })

    it('should get all filter values', () => {
      const { registerFilter, setFilterValue, getAllFilterValues } = useDashboardFilters()
      
      registerFilter('filter1', { field: 'region', linkedCharts: [] })
      registerFilter('filter2', { field: 'date', linkedCharts: [] })
      
      setFilterValue('filter1', 'north', false)
      setFilterValue('filter2', '2024-01-01', false)
      
      const allValues = getAllFilterValues()
      
      expect(allValues).toEqual({
        filter1: 'north',
        filter2: '2024-01-01'
      })
    })
  })

  describe('activeFilterCount', () => {
    it('should count active filters correctly', () => {
      const { registerFilter, setFilterValue, activeFilterCount } = useDashboardFilters()
      
      registerFilter('filter1', { field: 'region', linkedCharts: [] })
      registerFilter('filter2', { field: 'date', linkedCharts: [] })
      registerFilter('filter3', { field: 'category', linkedCharts: [] })
      
      expect(activeFilterCount.value).toBe(0)
      
      setFilterValue('filter1', 'north', false)
      expect(activeFilterCount.value).toBe(1)
      
      setFilterValue('filter2', '2024-01-01', false)
      expect(activeFilterCount.value).toBe(2)
      
      setFilterValue('filter1', null, false)
      expect(activeFilterCount.value).toBe(1)
    })
  })

  describe('updateLinkedCharts', () => {
    it('should update linked charts for a filter', () => {
      const { registerFilter, updateLinkedCharts, getFiltersForChart, setFilterValue } = useDashboardFilters()
      
      registerFilter('filter1', { field: 'region', linkedCharts: ['chart1'] })
      setFilterValue('filter1', 'north', false)
      
      expect(getFiltersForChart('chart1')).toEqual({ region: 'north' })
      expect(getFiltersForChart('chart2')).toEqual({})
      
      updateLinkedCharts('filter1', ['chart1', 'chart2'])
      
      expect(getFiltersForChart('chart2')).toEqual({ region: 'north' })
    })
  })
})

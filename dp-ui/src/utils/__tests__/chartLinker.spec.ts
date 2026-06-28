/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  createChartLinker,
  findLinkedCharts,
  hasLinkRelation,
  getUpstreamSources,
  validateLinkConfig,
  detectCyclicDependency,
  generateLinkConfigId,
  type ChartLinkConfigWithId
} from '../chartLinker'

// Mock ECharts instance
const createMockChartInstance = () => ({
  dispatchAction: vi.fn(),
  setOption: vi.fn(),
  getOption: vi.fn(() => ({})),
  resize: vi.fn(),
  dispose: vi.fn()
})

describe('chartLinker', () => {
  describe('createChartLinker', () => {
    let linker: ReturnType<typeof createChartLinker>

    beforeEach(() => {
      linker = createChartLinker()
    })

    afterEach(() => {
      linker.destroy()
    })

    it('should create a chart linker instance', () => {
      expect(linker).toBeDefined()
      expect(linker.registerChart).toBeDefined()
      expect(linker.unregisterChart).toBeDefined()
      expect(linker.setLinkConfig).toBeDefined()
      expect(linker.getLinkConfig).toBeDefined()
      expect(linker.handleChartClick).toBeDefined()
      expect(linker.resetAllLinks).toBeDefined()
    })

    it('should register and unregister charts', () => {
      const mockChart = createMockChartInstance()
      
      linker.registerChart('chart1', mockChart as any)
      expect(linker.getRegisteredCharts()).toContain('chart1')
      
      linker.unregisterChart('chart1')
      expect(linker.getRegisteredCharts()).not.toContain('chart1')
    })

    it('should set and get link configurations', () => {
      const configs: ChartLinkConfigWithId[] = [
        {
          id: 'link1',
          sourceChartId: 'chart1',
          targetChartId: 'chart2',
          sourceField: 'category',
          targetField: 'category',
          linkType: 'filter'
        }
      ]
      
      linker.setLinkConfig(configs)
      const result = linker.getLinkConfig()
      
      expect(result).toHaveLength(1)
      expect(result[0].id).toBe('link1')
      expect(result[0].sourceChartId).toBe('chart1')
      expect(result[0].targetChartId).toBe('chart2')
    })

    it('should get linked targets for a source chart', () => {
      const configs: ChartLinkConfigWithId[] = [
        {
          id: 'link1',
          sourceChartId: 'chart1',
          targetChartId: 'chart2',
          sourceField: 'category',
          targetField: 'category',
          linkType: 'filter'
        },
        {
          id: 'link2',
          sourceChartId: 'chart1',
          targetChartId: 'chart3',
          sourceField: 'category',
          targetField: 'category',
          linkType: 'highlight'
        }
      ]
      
      linker.setLinkConfig(configs)
      const targets = linker.getLinkedTargets('chart1')
      
      expect(targets).toContain('chart2')
      expect(targets).toContain('chart3')
      expect(targets).toHaveLength(2)
    })

    it('should add and remove event listeners', () => {
      const listener = vi.fn()
      
      linker.addEventListener(listener)
      linker.resetAllLinks()
      
      expect(listener).toHaveBeenCalled()
      
      listener.mockClear()
      linker.removeEventListener(listener)
      linker.resetAllLinks()
      
      expect(listener).not.toHaveBeenCalled()
    })

    it('should apply highlight to target charts', async () => {
      const mockChart1 = createMockChartInstance()
      const mockChart2 = createMockChartInstance()
      
      linker.registerChart('chart1', mockChart1 as any)
      linker.registerChart('chart2', mockChart2 as any)
      
      const configs: ChartLinkConfigWithId[] = [
        {
          id: 'link1',
          sourceChartId: 'chart1',
          targetChartId: 'chart2',
          sourceField: 'category',
          targetField: 'category',
          linkType: 'highlight'
        }
      ]
      
      linker.setLinkConfig(configs)
      
      await linker.handleChartClick('chart1', {
        dimensionValue: 'Electronics',
        seriesName: 'Sales'
      })
      
      // Check that highlight actions were dispatched
      expect(mockChart2.dispatchAction).toHaveBeenCalledWith(
        expect.objectContaining({ type: 'downplay' })
      )
      expect(mockChart2.dispatchAction).toHaveBeenCalledWith(
        expect.objectContaining({ type: 'highlight', name: 'Electronics' })
      )
    })

    it('should emit events on chart click', async () => {
      const listener = vi.fn()
      const mockChart1 = createMockChartInstance()
      const mockChart2 = createMockChartInstance()
      
      linker.registerChart('chart1', mockChart1 as any)
      linker.registerChart('chart2', mockChart2 as any)
      linker.addEventListener(listener)
      
      const configs: ChartLinkConfigWithId[] = [
        {
          id: 'link1',
          sourceChartId: 'chart1',
          targetChartId: 'chart2',
          sourceField: 'category',
          targetField: 'category',
          linkType: 'highlight'
        }
      ]
      
      linker.setLinkConfig(configs)
      
      await linker.handleChartClick('chart1', {
        dimensionValue: 'Electronics'
      })
      
      expect(listener).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'link-highlight',
          sourceChartId: 'chart1',
          targetChartIds: ['chart2'],
          dimensionValue: 'Electronics'
        })
      )
    })

    it('should not process click with null dimension value', async () => {
      const listener = vi.fn()
      linker.addEventListener(listener)
      
      await linker.handleChartClick('chart1', {
        dimensionValue: null
      })
      
      expect(listener).not.toHaveBeenCalled()
    })

    it('should not process click when no configs exist', async () => {
      const listener = vi.fn()
      linker.addEventListener(listener)
      
      await linker.handleChartClick('chart1', {
        dimensionValue: 'Electronics'
      })
      
      expect(listener).not.toHaveBeenCalled()
    })

    it('should clear cache', () => {
      // This is mainly to ensure the method exists and doesn't throw
      expect(() => linker.clearCache()).not.toThrow()
    })

    it('should destroy the linker', () => {
      const mockChart = createMockChartInstance()
      linker.registerChart('chart1', mockChart as any)
      
      linker.destroy()
      
      expect(linker.getRegisteredCharts()).toHaveLength(0)
      expect(linker.getLinkConfig()).toHaveLength(0)
    })
  })

  describe('findLinkedCharts', () => {
    it('should find all linked charts for a source', () => {
      const configs: ChartLinkConfigWithId[] = [
        {
          id: 'link1',
          sourceChartId: 'chart1',
          targetChartId: 'chart2',
          sourceField: 'category',
          targetField: 'category',
          linkType: 'filter'
        },
        {
          id: 'link2',
          sourceChartId: 'chart1',
          targetChartId: 'chart3',
          sourceField: 'category',
          targetField: 'category',
          linkType: 'filter'
        },
        {
          id: 'link3',
          sourceChartId: 'chart2',
          targetChartId: 'chart3',
          sourceField: 'region',
          targetField: 'region',
          linkType: 'filter'
        }
      ]
      
      const result = findLinkedCharts('chart1', configs)
      
      expect(result).toHaveLength(2)
      expect(result.map(c => c.targetChartId)).toContain('chart2')
      expect(result.map(c => c.targetChartId)).toContain('chart3')
    })

    it('should return empty array when no links exist', () => {
      const configs: ChartLinkConfigWithId[] = []
      const result = findLinkedCharts('chart1', configs)
      expect(result).toHaveLength(0)
    })
  })

  describe('hasLinkRelation', () => {
    const configs: ChartLinkConfigWithId[] = [
      {
        id: 'link1',
        sourceChartId: 'chart1',
        targetChartId: 'chart2',
        sourceField: 'category',
        targetField: 'category',
        linkType: 'filter'
      }
    ]

    it('should return true when link exists', () => {
      expect(hasLinkRelation('chart1', 'chart2', configs)).toBe(true)
    })

    it('should return false when link does not exist', () => {
      expect(hasLinkRelation('chart1', 'chart3', configs)).toBe(false)
    })

    it('should return false for reverse direction', () => {
      expect(hasLinkRelation('chart2', 'chart1', configs)).toBe(false)
    })
  })

  describe('getUpstreamSources', () => {
    it('should find all upstream sources for a target', () => {
      const configs: ChartLinkConfigWithId[] = [
        {
          id: 'link1',
          sourceChartId: 'chart1',
          targetChartId: 'chart3',
          sourceField: 'category',
          targetField: 'category',
          linkType: 'filter'
        },
        {
          id: 'link2',
          sourceChartId: 'chart2',
          targetChartId: 'chart3',
          sourceField: 'region',
          targetField: 'region',
          linkType: 'filter'
        }
      ]
      
      const result = getUpstreamSources('chart3', configs)
      
      expect(result).toContain('chart1')
      expect(result).toContain('chart2')
      expect(result).toHaveLength(2)
    })

    it('should return unique sources', () => {
      const configs: ChartLinkConfigWithId[] = [
        {
          id: 'link1',
          sourceChartId: 'chart1',
          targetChartId: 'chart2',
          sourceField: 'category',
          targetField: 'category',
          linkType: 'filter'
        },
        {
          id: 'link2',
          sourceChartId: 'chart1',
          targetChartId: 'chart2',
          sourceField: 'region',
          targetField: 'region',
          linkType: 'highlight'
        }
      ]
      
      const result = getUpstreamSources('chart2', configs)
      
      expect(result).toHaveLength(1)
      expect(result[0]).toBe('chart1')
    })
  })

  describe('validateLinkConfig', () => {
    it('should return no errors for valid config', () => {
      const config: ChartLinkConfigWithId = {
        id: 'link1',
        sourceChartId: 'chart1',
        targetChartId: 'chart2',
        sourceField: 'category',
        targetField: 'category',
        linkType: 'filter'
      }
      
      const errors = validateLinkConfig(config)
      expect(errors).toHaveLength(0)
    })

    it('should return error for missing id', () => {
      const config: ChartLinkConfigWithId = {
        id: '',
        sourceChartId: 'chart1',
        targetChartId: 'chart2',
        sourceField: 'category',
        targetField: 'category',
        linkType: 'filter'
      }
      
      const errors = validateLinkConfig(config)
      expect(errors).toContain('配置ID不能为空')
    })

    it('should return error for same source and target', () => {
      const config: ChartLinkConfigWithId = {
        id: 'link1',
        sourceChartId: 'chart1',
        targetChartId: 'chart1',
        sourceField: 'category',
        targetField: 'category',
        linkType: 'filter'
      }
      
      const errors = validateLinkConfig(config)
      expect(errors).toContain('源图表和目标图表不能相同')
    })

    it('should return error for invalid link type', () => {
      const config = {
        id: 'link1',
        sourceChartId: 'chart1',
        targetChartId: 'chart2',
        sourceField: 'category',
        targetField: 'category',
        linkType: 'invalid' as any
      }
      
      const errors = validateLinkConfig(config)
      expect(errors).toContain('联动类型无效，必须是 filter、drillDown 或 highlight')
    })

    it('should return multiple errors', () => {
      const config: ChartLinkConfigWithId = {
        id: '',
        sourceChartId: '',
        targetChartId: '',
        sourceField: '',
        targetField: '',
        linkType: 'invalid' as any
      }
      
      const errors = validateLinkConfig(config)
      expect(errors.length).toBeGreaterThan(1)
    })
  })

  describe('detectCyclicDependency', () => {
    it('should return false for acyclic graph', () => {
      const configs: ChartLinkConfigWithId[] = [
        {
          id: 'link1',
          sourceChartId: 'chart1',
          targetChartId: 'chart2',
          sourceField: 'category',
          targetField: 'category',
          linkType: 'filter'
        },
        {
          id: 'link2',
          sourceChartId: 'chart2',
          targetChartId: 'chart3',
          sourceField: 'category',
          targetField: 'category',
          linkType: 'filter'
        }
      ]
      
      expect(detectCyclicDependency(configs)).toBe(false)
    })

    it('should return true for cyclic graph', () => {
      const configs: ChartLinkConfigWithId[] = [
        {
          id: 'link1',
          sourceChartId: 'chart1',
          targetChartId: 'chart2',
          sourceField: 'category',
          targetField: 'category',
          linkType: 'filter'
        },
        {
          id: 'link2',
          sourceChartId: 'chart2',
          targetChartId: 'chart3',
          sourceField: 'category',
          targetField: 'category',
          linkType: 'filter'
        },
        {
          id: 'link3',
          sourceChartId: 'chart3',
          targetChartId: 'chart1',
          sourceField: 'category',
          targetField: 'category',
          linkType: 'filter'
        }
      ]
      
      expect(detectCyclicDependency(configs)).toBe(true)
    })

    it('should return true for self-loop', () => {
      const configs: ChartLinkConfigWithId[] = [
        {
          id: 'link1',
          sourceChartId: 'chart1',
          targetChartId: 'chart1',
          sourceField: 'category',
          targetField: 'category',
          linkType: 'filter'
        }
      ]
      
      expect(detectCyclicDependency(configs)).toBe(true)
    })

    it('should return false for empty configs', () => {
      expect(detectCyclicDependency([])).toBe(false)
    })
  })

  describe('generateLinkConfigId', () => {
    it('should generate unique ids', () => {
      const id1 = generateLinkConfigId()
      const id2 = generateLinkConfigId()
      
      expect(id1).not.toBe(id2)
    })

    it('should start with "link-"', () => {
      const id = generateLinkConfigId()
      expect(id.startsWith('link-')).toBe(true)
    })
  })
})

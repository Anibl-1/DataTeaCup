/**
 * useParameterLinkage 单元测试
 * 测试参数级联选择功能
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  useParameterLinkage,
  createCascadeConfig,
  createParameterLinkage,
  resetLinkageIdCounter
} from '../useParameterLinkage'
import type { ParameterOption } from '../useParameterLinkage'

describe('useParameterLinkage', () => {
  beforeEach(() => {
    resetLinkageIdCounter()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('parameter registration', () => {
    it('should register a parameter with initial value', () => {
      const { registerParameter, getParameterValue, parameters } = useParameterLinkage()

      registerParameter('province', '北京')

      expect(getParameterValue('province')).toBe('北京')
      expect(parameters.value['province']).toBeDefined()
      expect(parameters.value['province']!.initialized).toBe(true)
    })

    it('should register a parameter with initial options', () => {
      const { registerParameter, getParameterOptions } = useParameterLinkage()
      const options: ParameterOption[] = [
        { label: '北京', value: 'beijing' },
        { label: '上海', value: 'shanghai' }
      ]

      registerParameter('province', null, options)

      expect(getParameterOptions('province')).toEqual(options)
    })

    it('should unregister a parameter', () => {
      const { registerParameter, unregisterParameter, parameters, getParameterValue } = useParameterLinkage()

      registerParameter('province', '北京')
      expect(parameters.value['province']).toBeDefined()

      unregisterParameter('province')
      expect(parameters.value['province']).toBeUndefined()
      expect(getParameterValue('province')).toBeNull()
    })

    it('should warn when registering duplicate parameter', () => {
      const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})
      const { registerParameter } = useParameterLinkage()

      registerParameter('province', '北京')
      registerParameter('province', '上海')

      expect(warnSpy).toHaveBeenCalled()
      warnSpy.mockRestore()
    })
  })

  describe('linkage configuration', () => {
    it('should add linkage configuration', () => {
      const { registerParameter, addLinkage, linkages } = useParameterLinkage()

      registerParameter('province')
      registerParameter('city')

      const linkageId = addLinkage({
        sourceParam: 'province',
        targetParam: 'city',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'province_code',
          targetField: 'city_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM cities WHERE province_code = ${parentValue}'
        }),
        enabled: true
      })

      expect(linkageId).toBeDefined()
      expect(linkages.value.length).toBe(1)
      expect(linkages.value[0]!.sourceParam).toBe('province')
      expect(linkages.value[0]!.targetParam).toBe('city')
    })

    it('should remove linkage configuration', () => {
      const { registerParameter, addLinkage, removeLinkage, linkages } = useParameterLinkage()

      registerParameter('province')
      registerParameter('city')

      const linkageId = addLinkage({
        sourceParam: 'province',
        targetParam: 'city',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'province_code',
          targetField: 'city_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM cities'
        }),
        enabled: true
      })

      expect(linkages.value.length).toBe(1)

      removeLinkage(linkageId)

      expect(linkages.value.length).toBe(0)
    })

    it('should establish parent-child relationships', () => {
      const { registerParameter, addLinkage, parameters } = useParameterLinkage()

      registerParameter('province')
      registerParameter('city')

      addLinkage({
        sourceParam: 'province',
        targetParam: 'city',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'province_code',
          targetField: 'city_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM cities'
        }),
        enabled: true
      })

      expect(parameters.value['province']!.children).toContain('city')
      expect(parameters.value['city']!.parents).toContain('province')
    })
  })

  describe('value operations', () => {
    it('should set and get parameter value', async () => {
      const { registerParameter, setParameterValue, getParameterValue } = useParameterLinkage()

      registerParameter('province')
      await setParameterValue('province', '北京')

      expect(getParameterValue('province')).toBe('北京')
    })

    it('should trigger onValueChange callback', async () => {
      const onValueChange = vi.fn()
      const { registerParameter, setParameterValue } = useParameterLinkage({ onValueChange })

      registerParameter('province', '北京')
      await setParameterValue('province', '上海')

      expect(onValueChange).toHaveBeenCalledWith('province', '上海', '北京')
    })

    it('should not trigger callback when value unchanged', async () => {
      const onValueChange = vi.fn()
      const { registerParameter, setParameterValue } = useParameterLinkage({ onValueChange })

      registerParameter('province', '北京')
      await setParameterValue('province', '北京')

      expect(onValueChange).not.toHaveBeenCalled()
    })

    it('should clear all parameter values', async () => {
      const { registerParameter, setParameterValue, clearAll, getParameterValue } = useParameterLinkage()

      registerParameter('province')
      registerParameter('city')
      await setParameterValue('province', '北京')
      await setParameterValue('city', '朝阳区')

      clearAll()

      expect(getParameterValue('province')).toBeNull()
      expect(getParameterValue('city')).toBeNull()
    })

    it('should reset parameters to initial state', async () => {
      const { registerParameter, setParameterValue, reset, getParameterValue, getParameterOptions } = useParameterLinkage()
      const initialOptions: ParameterOption[] = [{ label: '北京', value: 'beijing' }]

      registerParameter('province', '北京', initialOptions)
      await setParameterValue('province', '上海')

      reset()

      expect(getParameterValue('province')).toBe('北京')
      expect(getParameterOptions('province')).toEqual(initialOptions)
    })

    it('should batch set parameter values', async () => {
      const { registerParameter, setParameterValues, getAllValues } = useParameterLinkage()

      registerParameter('province')
      registerParameter('city')

      await setParameterValues({
        province: '北京',
        city: '朝阳区'
      })

      const values = getAllValues()
      expect(values['province']).toBe('北京')
      expect(values['city']).toBe('朝阳区')
    })
  })

  describe('cascade behavior', () => {
    it('should clear child values when parent changes', async () => {
      const { registerParameter, addLinkage, setParameterValue, getParameterValue } = useParameterLinkage({
        autoClearChildren: true
      })

      registerParameter('province')
      registerParameter('city')

      addLinkage({
        sourceParam: 'province',
        targetParam: 'city',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'province_code',
          targetField: 'city_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM cities'
        }),
        enabled: true
      })

      await setParameterValue('city', '朝阳区')
      expect(getParameterValue('city')).toBe('朝阳区')

      await setParameterValue('province', '上海')
      expect(getParameterValue('city')).toBeNull()
    })

    it('should not clear child values when autoClearChildren is false', async () => {
      const { registerParameter, addLinkage, setParameterValue, getParameterValue } = useParameterLinkage({
        autoClearChildren: false
      })

      registerParameter('province')
      registerParameter('city')

      addLinkage({
        sourceParam: 'province',
        targetParam: 'city',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'province_code',
          targetField: 'city_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM cities'
        }),
        enabled: true
      })

      await setParameterValue('city', '朝阳区')
      await setParameterValue('province', '上海')

      expect(getParameterValue('city')).toBe('朝阳区')
    })

    it('should handle multi-level cascade', async () => {
      const { registerParameter, addLinkage, setParameterValue, getParameterValue } = useParameterLinkage()

      registerParameter('country')
      registerParameter('province')
      registerParameter('city')

      // country -> province
      addLinkage({
        sourceParam: 'country',
        targetParam: 'province',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'country_code',
          targetField: 'province_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM provinces'
        }),
        enabled: true
      })

      // province -> city
      addLinkage({
        sourceParam: 'province',
        targetParam: 'city',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'province_code',
          targetField: 'city_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM cities'
        }),
        enabled: true
      })

      await setParameterValue('province', '北京')
      await setParameterValue('city', '朝阳区')

      // Changing country should clear both province and city
      await setParameterValue('country', '美国')

      expect(getParameterValue('province')).toBeNull()
      expect(getParameterValue('city')).toBeNull()
    })
  })

  describe('options loading', () => {
    it('should call optionsLoader when refreshing options', async () => {
      const mockLoader = vi.fn().mockResolvedValue([
        { label: '朝阳区', value: 'chaoyang' },
        { label: '海淀区', value: 'haidian' }
      ])

      const { registerParameter, addLinkage, setParameterValue, refreshOptions } = useParameterLinkage({
        optionsLoader: mockLoader,
        debounceDelay: 0
      })

      registerParameter('province')
      registerParameter('city')

      addLinkage({
        sourceParam: 'province',
        targetParam: 'city',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'province_code',
          targetField: 'city_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM cities WHERE province_code = ${parentValue}'
        }),
        enabled: true
      })

      await setParameterValue('province', '北京')
      await refreshOptions('city')

      expect(mockLoader).toHaveBeenCalled()
    })

    it('should set loading state during options fetch', async () => {
      let resolveLoader: (value: ParameterOption[]) => void
      const mockLoader = vi.fn().mockImplementation(() => {
        return new Promise<ParameterOption[]>((resolve) => {
          resolveLoader = resolve
        })
      })

      const { registerParameter, addLinkage, setParameterValue, refreshOptions, isLoading } = useParameterLinkage({
        optionsLoader: mockLoader,
        debounceDelay: 0
      })

      registerParameter('province')
      registerParameter('city')

      addLinkage({
        sourceParam: 'province',
        targetParam: 'city',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'province_code',
          targetField: 'city_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM cities'
        }),
        enabled: true
      })

      await setParameterValue('province', '北京')
      
      const loadPromise = refreshOptions('city')
      
      expect(isLoading('city')).toBe(true)

      resolveLoader!([])
      await loadPromise

      expect(isLoading('city')).toBe(false)
    })

    it('should handle options loading error', async () => {
      const mockError = new Error('Network error')
      const mockLoader = vi.fn().mockRejectedValue(mockError)
      const onError = vi.fn()

      const { registerParameter, addLinkage, setParameterValue, refreshOptions, getError } = useParameterLinkage({
        optionsLoader: mockLoader,
        onError,
        debounceDelay: 0
      })

      registerParameter('province')
      registerParameter('city')

      addLinkage({
        sourceParam: 'province',
        targetParam: 'city',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'province_code',
          targetField: 'city_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM cities'
        }),
        enabled: true
      })

      await setParameterValue('province', '北京')
      await refreshOptions('city')

      expect(getError('city')).toBe('Network error')
      expect(onError).toHaveBeenCalledWith('city', mockError)
    })

    it('should call onOptionsLoaded callback', async () => {
      const options: ParameterOption[] = [
        { label: '朝阳区', value: 'chaoyang' }
      ]
      const mockLoader = vi.fn().mockResolvedValue(options)
      const onOptionsLoaded = vi.fn()

      const { registerParameter, addLinkage, setParameterValue, refreshOptions } = useParameterLinkage({
        optionsLoader: mockLoader,
        onOptionsLoaded,
        debounceDelay: 0
      })

      registerParameter('province')
      registerParameter('city')

      addLinkage({
        sourceParam: 'province',
        targetParam: 'city',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'province_code',
          targetField: 'city_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM cities'
        }),
        enabled: true
      })

      await setParameterValue('province', '北京')
      await refreshOptions('city')

      expect(onOptionsLoaded).toHaveBeenCalledWith('city', options)
    })
  })

  describe('dependency validation', () => {
    it('should validate dependencies correctly', async () => {
      const { registerParameter, addLinkage, setParameterValue, validateDependencies } = useParameterLinkage()

      registerParameter('province')
      registerParameter('city')

      addLinkage({
        sourceParam: 'province',
        targetParam: 'city',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'province_code',
          targetField: 'city_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM cities'
        }),
        enabled: true
      })

      // City depends on province, but province has no value
      expect(validateDependencies('city')).toBe(false)

      await setParameterValue('province', '北京')
      expect(validateDependencies('city')).toBe(true)
    })

    it('should get dependency values', async () => {
      const { registerParameter, addLinkage, setParameterValue, getDependencyValues } = useParameterLinkage()

      registerParameter('country')
      registerParameter('province')
      registerParameter('city')

      addLinkage({
        sourceParam: 'country',
        targetParam: 'province',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'country_code',
          targetField: 'province_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM provinces'
        }),
        enabled: true
      })

      addLinkage({
        sourceParam: 'province',
        targetParam: 'city',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'province_code',
          targetField: 'city_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM cities'
        }),
        enabled: true
      })

      await setParameterValue('country', '中国')
      await setParameterValue('province', '北京')

      const deps = getDependencyValues('city')
      expect(deps).toEqual({
        country: '中国',
        province: '北京'
      })
    })
  })

  describe('cascade level', () => {
    it('should calculate cascade level correctly', () => {
      const { registerParameter, addLinkage, getCascadeLevel } = useParameterLinkage()

      registerParameter('country')
      registerParameter('province')
      registerParameter('city')

      addLinkage({
        sourceParam: 'country',
        targetParam: 'province',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'country_code',
          targetField: 'province_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM provinces'
        }),
        enabled: true
      })

      addLinkage({
        sourceParam: 'province',
        targetParam: 'city',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'province_code',
          targetField: 'city_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM cities'
        }),
        enabled: true
      })

      expect(getCascadeLevel('country')).toBe(0)
      expect(getCascadeLevel('province')).toBe(1)
      expect(getCascadeLevel('city')).toBe(2)
    })

    it('should get root parameters', () => {
      const { registerParameter, addLinkage, getRootParameters } = useParameterLinkage()

      registerParameter('country')
      registerParameter('province')
      registerParameter('city')
      registerParameter('standalone')

      addLinkage({
        sourceParam: 'country',
        targetParam: 'province',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'country_code',
          targetField: 'province_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM provinces'
        }),
        enabled: true
      })

      addLinkage({
        sourceParam: 'province',
        targetParam: 'city',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'province_code',
          targetField: 'city_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM cities'
        }),
        enabled: true
      })

      const roots = getRootParameters()
      expect(roots).toContain('country')
      expect(roots).toContain('standalone')
      expect(roots).not.toContain('province')
      expect(roots).not.toContain('city')
    })

    it('should get leaf parameters', () => {
      const { registerParameter, addLinkage, getLeafParameters } = useParameterLinkage()

      registerParameter('country')
      registerParameter('province')
      registerParameter('city')
      registerParameter('standalone')

      addLinkage({
        sourceParam: 'country',
        targetParam: 'province',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'country_code',
          targetField: 'province_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM provinces'
        }),
        enabled: true
      })

      addLinkage({
        sourceParam: 'province',
        targetParam: 'city',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'province_code',
          targetField: 'city_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM cities'
        }),
        enabled: true
      })

      const leaves = getLeafParameters()
      expect(leaves).toContain('city')
      expect(leaves).toContain('standalone')
      expect(leaves).not.toContain('country')
      expect(leaves).not.toContain('province')
    })
  })

  describe('loading state', () => {
    it('should track isAnyLoading correctly', async () => {
      let resolveLoader: (value: ParameterOption[]) => void
      const mockLoader = vi.fn().mockImplementation(() => {
        return new Promise<ParameterOption[]>((resolve) => {
          resolveLoader = resolve
        })
      })

      const { registerParameter, addLinkage, setParameterValue, refreshOptions, isAnyLoading } = useParameterLinkage({
        optionsLoader: mockLoader,
        debounceDelay: 0
      })

      registerParameter('province')
      registerParameter('city')

      addLinkage({
        sourceParam: 'province',
        targetParam: 'city',
        linkageType: 'cascade',
        linkageConfig: createCascadeConfig({
          sourceField: 'province_code',
          targetField: 'city_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM cities'
        }),
        enabled: true
      })

      expect(isAnyLoading.value).toBe(false)

      await setParameterValue('province', '北京')
      const loadPromise = refreshOptions('city')

      expect(isAnyLoading.value).toBe(true)

      resolveLoader!([])
      await loadPromise

      expect(isAnyLoading.value).toBe(false)
    })
  })

  describe('helper functions', () => {
    it('should create cascade config correctly', () => {
      const config = createCascadeConfig({
        sourceField: 'province_code',
        targetField: 'city_code',
        dataSourceId: 1,
        sql: 'SELECT * FROM cities'
      })

      expect(config.sourceField).toBe('province_code')
      expect(config.targetField).toBe('city_code')
      expect(config.dataSourceId).toBe(1)
      expect(config.sql).toBe('SELECT * FROM cities')
      expect(config.labelField).toBe('label')
      expect(config.valueField).toBe('value')
    })

    it('should create parameter linkage correctly', () => {
      const linkage = createParameterLinkage({
        sourceParam: 'province',
        targetParam: 'city',
        linkageConfig: createCascadeConfig({
          sourceField: 'province_code',
          targetField: 'city_code',
          dataSourceId: 1,
          sql: 'SELECT * FROM cities'
        })
      })

      expect(linkage.sourceParam).toBe('province')
      expect(linkage.targetParam).toBe('city')
      expect(linkage.linkageType).toBe('cascade')
      expect(linkage.enabled).toBe(true)
    })
  })
})

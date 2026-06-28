/**
 * Feature: mars-integration-optimization, Property 10: 配置搜索过滤正确性
 * Validates: Requirements 7.4
 *
 * For any config list and keyword, filterConfigs returns only items where
 * configKey or configDesc contains the keyword (case-insensitive),
 * and doesn't miss any matches.
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { filterConfigs } from '@/utils/configSerializer'
import type { SystemConfig } from '@/api/system/systemConfig'

/** Generate a minimal SystemConfig with the fields filterConfigs inspects */
const arbConfig: fc.Arbitrary<SystemConfig> = fc.record({
  configKey: fc.string({ minLength: 1, maxLength: 50 }),
  configValue: fc.string({ maxLength: 100 }),
  configDesc: fc.option(fc.string({ maxLength: 100 }), { nil: undefined }),
})

const arbConfigList = fc.array(arbConfig, { minLength: 0, maxLength: 20 })
const arbKeyword = fc.string({ minLength: 0, maxLength: 20 })

/** Reference implementation: does the config match the keyword? */
function shouldMatch(config: SystemConfig, keyword: string): boolean {
  if (!keyword || !keyword.trim()) return true
  const lk = keyword.toLowerCase()
  return (
    config.configKey.toLowerCase().includes(lk) ||
    (config.configDesc != null && config.configDesc.toLowerCase().includes(lk))
  )
}

describe('Property 10: 配置搜索过滤正确性', () => {
  it('every returned item matches the keyword in configKey or configDesc', () => {
    fc.assert(
      fc.property(arbConfigList, arbKeyword, (configs, keyword) => {
        const result = filterConfigs(configs, keyword)
        for (const item of result) {
          expect(shouldMatch(item, keyword)).toBe(true)
        }
      }),
      { numRuns: 200 },
    )
  })

  it('no matching item is omitted from the result', () => {
    fc.assert(
      fc.property(arbConfigList, arbKeyword, (configs, keyword) => {
        const result = filterConfigs(configs, keyword)
        const expectedCount = configs.filter((c) => shouldMatch(c, keyword)).length
        expect(result.length).toBe(expectedCount)
      }),
      { numRuns: 200 },
    )
  })

  it('empty or whitespace-only keyword returns the full list', () => {
    fc.assert(
      fc.property(
        arbConfigList,
        fc.constantFrom('', '  ', '\t', '\n'),
        (configs, keyword) => {
          const result = filterConfigs(configs, keyword)
          expect(result.length).toBe(configs.length)
        },
      ),
      { numRuns: 200 },
    )
  })

  it('result is a subset of the original list (preserves order)', () => {
    fc.assert(
      fc.property(arbConfigList, arbKeyword, (configs, keyword) => {
        const result = filterConfigs(configs, keyword)
        let idx = 0
        for (const item of result) {
          while (idx < configs.length && configs[idx] !== item) idx++
          expect(idx).toBeLessThan(configs.length)
          idx++
        }
      }),
      { numRuns: 200 },
    )
  })
})

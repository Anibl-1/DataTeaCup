/**
 * Feature: frontend-comprehensive-optimization, Property 5: configSerializer 序列化 Round-Trip
 *
 * **Validates: Requirements 10.3**
 *
 * 对于任意合法 SystemConfig 对象，deserializeConfig(serializeConfig(config))
 * 应产生与原始 config 深度相等的对象（round-trip 属性）。
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { serializeConfig, deserializeConfig } from '../configSerializer'
import type { SystemConfig } from '@/api/system/systemConfig'

// --- Generators ---

const arbConfigType = fc.constantFrom('string', 'number', 'boolean', 'json', 'password')

/** ISO date-time string within a reasonable range */
const arbDateTimeString = fc
  .integer({
    min: new Date('2020-01-01').getTime(),
    max: new Date('2030-12-31').getTime(),
  })
  .map((ts) => new Date(ts).toISOString())

/**
 * Generate a valid SystemConfig object.
 * Only configKey and configValue are required; all other fields are optional.
 * JSON.stringify drops `undefined` values, so we use requiredKeys to let
 * fast-check omit optional fields entirely rather than setting them to undefined.
 */
const arbSystemConfig: fc.Arbitrary<SystemConfig> = fc.record(
  {
    id: fc.nat({ max: 1_000_000 }),
    configKey: fc.string({ minLength: 1, maxLength: 50 }).filter((s) => /^\S+$/.test(s)),
    configValue: fc.string({ maxLength: 200 }),
    configType: arbConfigType,
    configDesc: fc.string({ maxLength: 100 }),
    configGroup: fc.string({ minLength: 1, maxLength: 30 }),
    isSystem: fc.boolean(),
    createTime: arbDateTimeString,
    updateTime: arbDateTimeString,
  },
  {
    requiredKeys: ['configKey', 'configValue'],
  },
)

/**
 * Strip undefined-valued keys to match JSON round-trip semantics
 * (JSON.stringify omits keys whose value is undefined).
 */
function stripUndefined(obj: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(Object.entries(obj).filter(([, v]) => v !== undefined))
}

describe('Property 5: configSerializer 序列化 Round-Trip', () => {
  it('deserializeConfig(serializeConfig(config)) deeply equals original for any valid SystemConfig', () => {
    fc.assert(
      fc.property(arbSystemConfig, (config) => {
        const serialized = serializeConfig(config)
        const deserialized = deserializeConfig(serialized)

        // JSON round-trip drops undefined keys, so compare stripped versions
        expect(deserialized).toEqual(stripUndefined(config as unknown as Record<string, unknown>))
      }),
      { numRuns: 100 },
    )
  })
})

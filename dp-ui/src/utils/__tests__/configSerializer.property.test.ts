/**
 * Feature: mars-integration-optimization, Property 1: 配置序列化往返一致性
 * Validates: Requirements 8.1, 8.2, 8.3
 *
 * For any valid SystemConfig object, deserializeConfig(serializeConfig(config))
 * produces an equivalent object. The serialized form is a valid JSON string,
 * and the deserialized object has all the same fields as the original.
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { serializeConfig, deserializeConfig } from '../configSerializer'
import type { SystemConfig } from '@/api/system/systemConfig'

// --- Generators ---

const arbConfigType = fc.constantFrom('string', 'number', 'boolean', 'json', 'password')

/** Generate an ISO date-time string from a safe integer timestamp range */
const arbDateTimeString = fc.integer({
  min: new Date('2020-01-01').getTime(),
  max: new Date('2030-12-31').getTime(),
}).map(ts => new Date(ts).toISOString())

/**
 * Generate a valid SystemConfig object that survives JSON round-trip.
 * JSON.stringify drops keys with `undefined` values, so optional fields
 * use `fc.option` with `{ nil: null }` to keep them as null, or we
 * simply omit undefined keys before comparison. Here we build configs
 * that only include optional fields when they have a real value.
 */
const arbSystemConfig: fc.Arbitrary<SystemConfig> = fc.record(
  {
    id: fc.nat({ max: 1_000_000 }),
    configKey: fc.string({ minLength: 1, maxLength: 50 }).filter(s => /^\S+$/.test(s)),
    configValue: fc.string({ maxLength: 200 }),
    configType: arbConfigType,
    configDesc: fc.string({ maxLength: 100 }),
    configGroup: fc.string({ minLength: 1, maxLength: 30 }),
    isSystem: fc.boolean(),
    createTime: arbDateTimeString,
    updateTime: arbDateTimeString,
  },
  {
    // Allow optional fields to be omitted entirely from the generated object
    requiredKeys: ['configKey', 'configValue'],
  }
)

/**
 * Helper: strip undefined-valued keys from an object, matching JSON round-trip
 * behavior where JSON.stringify omits undefined values.
 */
function stripUndefined(obj: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(Object.entries(obj).filter(([, v]) => v !== undefined))
}

describe('Property 1: 配置序列化往返一致性', () => {
  it('round-trip: deserializeConfig(serializeConfig(config)) equals original', () => {
    fc.assert(
      fc.property(arbSystemConfig, (config) => {
        const serialized = serializeConfig(config)
        const deserialized = deserializeConfig(serialized)
        // JSON round-trip drops undefined keys, so compare stripped versions
        expect(deserialized).toEqual(stripUndefined(config as unknown as Record<string, unknown>))
      }),
      { numRuns: 200 }
    )
  })

  it('serialized form is a valid JSON string', () => {
    fc.assert(
      fc.property(arbSystemConfig, (config) => {
        const serialized = serializeConfig(config)
        expect(typeof serialized).toBe('string')
        expect(() => JSON.parse(serialized)).not.toThrow()
      }),
      { numRuns: 200 }
    )
  })

  it('deserialized object preserves all defined (non-undefined) fields from the original', () => {
    fc.assert(
      fc.property(arbSystemConfig, (config) => {
        const deserialized = deserializeConfig(serializeConfig(config))

        // Every key with a defined value in the original should be present and equal
        for (const [key, value] of Object.entries(config)) {
          if (value !== undefined) {
            expect(deserialized).toHaveProperty(key)
            expect((deserialized as unknown as Record<string, unknown>)[key]).toEqual(value)
          }
        }
      }),
      { numRuns: 200 }
    )
  })
})

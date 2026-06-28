/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { serializeMessage, deserializeMessage, type WsMessage } from '../messageSerializer'

/**
 * Property 3: WsMessage序列化往返一致性
 *
 * **Validates: Requirements 19.1**
 * **Feature: platform-ui-polish, Property 3: WsMessage序列化往返一致性**
 *
 * For any valid WsMessage object (containing type string, any payload,
 * and number timestamp), serializing to JSON string then deserializing
 * should yield an equivalent result.
 */
describe('messageSerializer — Property Tests', () => {
  // Generator for valid WsMessage objects
  const wsMessageArb: fc.Arbitrary<WsMessage> = fc.record({
    type: fc.string({ minLength: 1, maxLength: 50 }),
    payload: fc.jsonValue(),
    timestamp: fc.double({
      min: 0,
      max: Number.MAX_SAFE_INTEGER,
      noNaN: true,
      noDefaultInfinity: true,
    }),
  })

  it('Property 3: serialize then deserialize yields deep-equal result', () => {
    fc.assert(
      fc.property(wsMessageArb, (msg) => {
        const serialized = serializeMessage(msg)
        const deserialized = deserializeMessage(serialized)
        expect(deserialized).toEqual(msg)
      }),
      { numRuns: 100 },
    )
  })

  /**
   * Property 4: 无效JSON输入产生错误
   *
   * **Validates: Requirements 19.2**
   * **Feature: platform-ui-polish, Property 4: 无效JSON输入产生错误**
   *
   * For any non-valid JSON format string, calling deserializeMessage
   * should throw an Error containing "Invalid JSON" description.
   */
  it('Property 4: invalid JSON input throws Error containing "Invalid JSON"', () => {
    // Generator for strings that are NOT valid JSON
    const invalidJsonArb = fc.string().filter((s) => {
      try {
        JSON.parse(s)
        return false // valid JSON — exclude
      } catch {
        return true // invalid JSON — keep
      }
    })

    fc.assert(
      fc.property(invalidJsonArb, (input) => {
        expect(() => deserializeMessage(input)).toThrowError(/Invalid JSON/)
      }),
      { numRuns: 100 },
    )
  })

  /**
   * Property 5: 缺少必要字段的JSON产生错误
   *
   * **Validates: Requirements 19.3**
   * **Feature: platform-ui-polish, Property 5: 缺少必要字段的JSON产生错误**
   *
   * For any valid JSON object but missing type or timestamp field,
   * calling deserializeMessage should throw an Error containing
   * "Invalid message format" description.
   */
  it('Property 5: JSON missing required fields throws Error containing "Invalid message format"', () => {
    // Strategy: generate an arbitrary JSON object, then delete type and/or timestamp
    // to ensure the required field(s) are missing.
    // We test three scenarios via oneOf:
    //   1. missing type only
    //   2. missing timestamp only
    //   3. missing both

    const baseObjArb = fc.dictionary(
      fc.string({ minLength: 1, maxLength: 20 }),
      fc.jsonValue(),
    )

    // Scenario 1: has timestamp but no type (or type is falsy)
    const missingTypeArb = baseObjArb.map((obj) => {
      const copy = { ...obj }
      delete (copy as any).type
      // Ensure timestamp is present and defined
      ;(copy as any).timestamp = Date.now()
      return JSON.stringify(copy)
    })

    // Scenario 2: has type but no timestamp
    const missingTimestampArb = baseObjArb.map((obj) => {
      const copy = { ...obj }
      delete (copy as any).timestamp
      // Ensure type is a non-empty string (truthy)
      ;(copy as any).type = 'test-message'
      return JSON.stringify(copy)
    })

    // Scenario 3: missing both type and timestamp
    const missingBothArb = baseObjArb.map((obj) => {
      const copy = { ...obj }
      delete (copy as any).type
      delete (copy as any).timestamp
      return JSON.stringify(copy)
    })

    const missingFieldsArb = fc.oneof(missingTypeArb, missingTimestampArb, missingBothArb)

    fc.assert(
      fc.property(missingFieldsArb, (jsonStr) => {
        expect(() => deserializeMessage(jsonStr)).toThrowError(/Invalid message format/)
      }),
      { numRuns: 100 },
    )
  })
})

/**
 * Feature: mars-integration-optimization
 * Property 2: 聊天消息序列化往返一致性
 * Property 3: 无效 JSON 反序列化抛出异常
 * Validates: Requirements 11.1, 11.2, 11.3, 11.4
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { serializeMessage, deserializeMessage } from '../messageSerializer'
import type { WsMessage } from '../messageSerializer'

// --- Generators ---

/**
 * Generate a JSON-safe payload value.
 * We use fc.jsonValue() which produces values that survive JSON round-trip.
 */
const arbJsonSafePayload = fc.jsonValue()

/**
 * Generate a valid WsMessage object that survives JSON round-trip.
 */
const arbWsMessage: fc.Arbitrary<WsMessage> = fc.record({
  type: fc.string({ minLength: 1, maxLength: 50 }).filter(s => s.trim().length > 0),
  payload: arbJsonSafePayload,
  timestamp: fc.nat({ max: 2_000_000_000_000 }),
})

// --- Property 2: 聊天消息序列化往返一致性 ---

describe('Property 2: 聊天消息序列化往返一致性', () => {
  it('round-trip: deserializeMessage(serializeMessage(msg)) equals original', () => {
    fc.assert(
      fc.property(arbWsMessage, (msg) => {
        const serialized = serializeMessage(msg)
        const deserialized = deserializeMessage(serialized)
        expect(deserialized).toEqual(msg)
      }),
      { numRuns: 200 }
    )
  })

  it('serialized form is a valid JSON string', () => {
    fc.assert(
      fc.property(arbWsMessage, (msg) => {
        const serialized = serializeMessage(msg)
        expect(typeof serialized).toBe('string')
        expect(() => JSON.parse(serialized)).not.toThrow()
      }),
      { numRuns: 200 }
    )
  })

  it('deserialized object preserves type, payload, and timestamp fields', () => {
    fc.assert(
      fc.property(arbWsMessage, (msg) => {
        const deserialized = deserializeMessage(serializeMessage(msg))
        expect(deserialized.type).toBe(msg.type)
        expect(deserialized.payload).toEqual(msg.payload)
        expect(deserialized.timestamp).toBe(msg.timestamp)
      }),
      { numRuns: 200 }
    )
  })
})

// --- Property 3: 无效 JSON 反序列化抛出异常 ---

describe('Property 3: 无效 JSON 反序列化抛出异常', () => {
  it('throws on invalid JSON strings', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 1 }).filter(s => {
          try { JSON.parse(s); return false } catch { return true }
        }),
        (invalidJson) => {
          expect(() => deserializeMessage(invalidJson)).toThrow()
        }
      ),
      { numRuns: 200 }
    )
  })

  it('throws on empty string', () => {
    expect(() => deserializeMessage('')).toThrow()
  })

  it('throws on JSON missing required "type" field', () => {
    fc.assert(
      fc.property(
        fc.record({
          payload: arbJsonSafePayload,
          timestamp: fc.nat(),
        }),
        (obj) => {
          const json = JSON.stringify(obj)
          expect(() => deserializeMessage(json)).toThrow('missing required fields')
        }
      ),
      { numRuns: 200 }
    )
  })

  it('throws on JSON missing required "timestamp" field', () => {
    fc.assert(
      fc.property(
        fc.record({
          type: fc.string({ minLength: 1 }),
          payload: arbJsonSafePayload,
        }),
        (obj) => {
          const json = JSON.stringify(obj)
          expect(() => deserializeMessage(json)).toThrow('missing required fields')
        }
      ),
      { numRuns: 200 }
    )
  })

  it('throws on JSON with empty string type', () => {
    const json = JSON.stringify({ type: '', payload: null, timestamp: 123 })
    expect(() => deserializeMessage(json)).toThrow('missing required fields')
  })
})

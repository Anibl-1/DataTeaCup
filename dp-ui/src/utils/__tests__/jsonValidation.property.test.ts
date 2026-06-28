/**
 * Feature: mars-integration-optimization, Property 9: JSON 格式实时校验
 * Validates: Requirements 7.2
 *
 * For any string input, validateJsonFormat correctly identifies valid/invalid JSON.
 * Valid JSON returns { valid: true }, invalid JSON returns { valid: false, error: string }.
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { validateJsonFormat } from '../configSerializer'

describe('Property 9: JSON 格式实时校验', () => {
  it('valid JSON objects are accepted', () => {
    fc.assert(
      fc.property(fc.jsonValue(), (value) => {
        const jsonStr = JSON.stringify(value)
        const result = validateJsonFormat(jsonStr)
        expect(result.valid).toBe(true)
        expect(result.error).toBeUndefined()
      }),
      { numRuns: 200 },
    )
  })

  it('arbitrary non-JSON strings are rejected with an error message', () => {
    // Generate strings that are very unlikely to be valid JSON
    const arbInvalidJson = fc
      .string({ minLength: 1, maxLength: 100 })
      .filter((s) => {
        try {
          JSON.parse(s)
          return false
        } catch {
          return true
        }
      })

    fc.assert(
      fc.property(arbInvalidJson, (input) => {
        const result = validateJsonFormat(input)
        expect(result.valid).toBe(false)
        expect(typeof result.error).toBe('string')
        expect(result.error!.length).toBeGreaterThan(0)
      }),
      { numRuns: 200 },
    )
  })

  it('empty string is invalid JSON', () => {
    const result = validateJsonFormat('')
    expect(result.valid).toBe(false)
    expect(result.error).toBeDefined()
  })

  it('validateJsonFormat agrees with JSON.parse for any string', () => {
    fc.assert(
      fc.property(fc.string({ maxLength: 200 }), (input) => {
        let nativeParseable: boolean
        try {
          JSON.parse(input)
          nativeParseable = true
        } catch {
          nativeParseable = false
        }
        const result = validateJsonFormat(input)
        expect(result.valid).toBe(nativeParseable)
      }),
      { numRuns: 200 },
    )
  })
})

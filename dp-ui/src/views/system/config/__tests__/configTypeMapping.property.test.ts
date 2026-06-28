/**
 * Feature: mars-integration-optimization, Property 8: 配置类型到 UI 控件和颜色标签的映射
 * Validates: Requirements 7.1, 7.3
 *
 * For any configType value (string/number/boolean/json/password), the mapping
 * functions should return the correct edit control type and corresponding tag color.
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { getConfigTypeColor, getConfigControlType } from '@/utils/configSerializer'

const KNOWN_TYPES = ['string', 'number', 'boolean', 'json', 'password'] as const

const expectedControlMap: Record<string, string> = {
  string: 'input',
  number: 'input-number',
  boolean: 'switch',
  json: 'textarea',
  password: 'password',
}

const expectedColorMap: Record<string, string> = {
  string: 'default',
  number: '#2080f0',
  boolean: '#18a058',
  json: '#8b5cf6',
  password: '#f0a020',
}

describe('Property 8: 配置类型到 UI 控件和颜色标签的映射', () => {
  it('known configType maps to correct control type', () => {
    fc.assert(
      fc.property(fc.constantFrom(...KNOWN_TYPES), (configType) => {
        expect(getConfigControlType(configType)).toBe(expectedControlMap[configType])
      }),
      { numRuns: 200 },
    )
  })

  it('known configType maps to correct tag color', () => {
    fc.assert(
      fc.property(fc.constantFrom(...KNOWN_TYPES), (configType) => {
        expect(getConfigTypeColor(configType)).toBe(expectedColorMap[configType])
      }),
      { numRuns: 200 },
    )
  })

  it('unknown configType falls back to string defaults (input / default)', () => {
    const arbUnknownType = fc
      .string({ minLength: 1, maxLength: 20 })
      .filter((s) => !(KNOWN_TYPES as readonly string[]).includes(s))

    fc.assert(
      fc.property(arbUnknownType, (configType) => {
        expect(getConfigControlType(configType)).toBe('input')
        expect(getConfigTypeColor(configType)).toBe('default')
      }),
      { numRuns: 200 },
    )
  })
})

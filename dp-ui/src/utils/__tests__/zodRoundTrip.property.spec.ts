/**
 * Feature: frontend-comprehensive-optimization, Property 8: Zod Schema 解析 Round-Trip
 *
 * **Validates: Requirements 11.1, 11.2, 11.3**
 *
 * 对于任意符合 loginSchema / dataSourceSchema / roleSchema 约束的合法数据，
 * 经 schema.parse(data) 后所有字段值应与原始输入保持一致（round-trip 属性）。
 *
 * 注意：Zod 会应用 .trim() 和 .default() 等转换，因此生成器产生的数据
 * 已经是"规范化"后的形式（无前后空白、默认值已填充），以确保 parse 后值不变。
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { loginSchema, dataSourceSchema, roleSchema } from '../validation'

// --- Generators ---

/**
 * Non-empty trimmed string generator.
 * Produces strings without leading/trailing whitespace so that Zod's .trim()
 * transformation is a no-op, enabling true round-trip equality.
 */
const arbTrimmedString = (minLength = 1, maxLength = 50) =>
  fc
    .string({ minLength: Math.max(minLength, 1), maxLength })
    .map((s) => s.replace(/^\s+|\s+$/g, ''))
    .filter((s) => s.length >= minLength)

/**
 * Generator for valid loginSchema data.
 * Both username and password are requiredString (min 1, trimmed).
 */
const arbLoginData = fc.record({
  username: arbTrimmedString(1, 20),
  password: arbTrimmedString(1, 30),
})

/**
 * Generator for valid dataSourceSchema data.
 * - name, type, host, database, username, password: requiredString (trimmed)
 * - port: integer 1–65535
 * - description: optionalString (string | undefined | null)
 */
const arbDataSourceData = fc.record({
  name: arbTrimmedString(1, 30),
  type: arbTrimmedString(1, 20),
  host: arbTrimmedString(1, 50),
  port: fc.integer({ min: 1, max: 65535 }),
  database: arbTrimmedString(1, 30),
  username: arbTrimmedString(1, 30),
  password: arbTrimmedString(1, 30),
  description: fc.oneof(fc.constant(undefined), fc.constant(null), fc.string({ maxLength: 100 })),
})

/**
 * Generator for valid roleSchema data.
 * - roleName: requiredString (trimmed)
 * - roleCode: requiredString matching /^[A-Z_]+$/ (trimmed, uppercase + underscore only)
 * - description: optionalString
 * - status: number with .default(1) — we always provide it explicitly to ensure round-trip
 */
const arbRoleCode = fc
  .array(fc.constantFrom(...'ABCDEFGHIJKLMNOPQRSTUVWXYZ_'.split('')), {
    minLength: 1,
    maxLength: 20,
  })
  .map((chars) => chars.join(''))

const arbRoleData = fc.record({
  roleName: arbTrimmedString(1, 20),
  roleCode: arbRoleCode,
  description: fc.oneof(fc.constant(undefined), fc.constant(null), fc.string({ maxLength: 100 })),
  status: fc.integer({ min: 0, max: 10 }),
})

// --- Property Tests ---

describe('Property 8: Zod Schema 解析 Round-Trip', () => {
  it('loginSchema.parse(data) preserves all field values for any valid login data', () => {
    fc.assert(
      fc.property(arbLoginData, (data) => {
        const parsed = loginSchema.parse(data)
        expect(parsed.username).toBe(data.username)
        expect(parsed.password).toBe(data.password)
      }),
      { numRuns: 100 },
    )
  })

  it('dataSourceSchema.parse(data) preserves all field values for any valid data source data', () => {
    fc.assert(
      fc.property(arbDataSourceData, (data) => {
        const parsed = dataSourceSchema.parse(data)
        expect(parsed.name).toBe(data.name)
        expect(parsed.type).toBe(data.type)
        expect(parsed.host).toBe(data.host)
        expect(parsed.port).toBe(data.port)
        expect(parsed.database).toBe(data.database)
        expect(parsed.username).toBe(data.username)
        expect(parsed.password).toBe(data.password)
        expect(parsed.description).toEqual(data.description)
      }),
      { numRuns: 100 },
    )
  })

  it('roleSchema.parse(data) preserves all field values for any valid role data', () => {
    fc.assert(
      fc.property(arbRoleData, (data) => {
        const parsed = roleSchema.parse(data)
        expect(parsed.roleName).toBe(data.roleName)
        expect(parsed.roleCode).toBe(data.roleCode)
        expect(parsed.description).toEqual(data.description)
        expect(parsed.status).toBe(data.status)
      }),
      { numRuns: 100 },
    )
  })
})

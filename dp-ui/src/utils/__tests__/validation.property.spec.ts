/**
 * Feature: frontend-comprehensive-optimization, Property 6: validate 函数对合法输入的正确性
 *
 * **Validates: Requirements 10.4**
 *
 * 对于任意符合给定 Zod schema 约束的输入数据，validate(schema, data) 应返回
 * { success: true }，且返回的 data 字段与输入深度相等。
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import {
  validate,
  loginSchema,
  dataSourceSchema,
  roleSchema,
  menuSchema,
  reportSchema,
  chartSchema,
} from '../validation'
import { z } from 'zod'

// --- Generators ---

/**
 * Non-empty trimmed string (no leading/trailing whitespace) to avoid
 * Zod's .trim() transformation changing the value.
 */
const arbTrimmedString = (minLength = 1, maxLength = 50) =>
  fc
    .string({ minLength: Math.max(minLength, 1), maxLength })
    .map((s) => s.replace(/^\s+|\s+$/g, ''))
    .filter((s) => s.length >= minLength)

/** Valid loginSchema data */
const arbLoginData = fc.record({
  username: arbTrimmedString(1, 20),
  password: arbTrimmedString(1, 30),
})

/** Valid dataSourceSchema data */
const arbDataSourceData = fc.record(
  {
    name: arbTrimmedString(1, 30),
    type: arbTrimmedString(1, 20),
    host: arbTrimmedString(1, 50),
    port: fc.integer({ min: 1, max: 65535 }),
    database: arbTrimmedString(1, 30),
    username: arbTrimmedString(1, 30),
    password: arbTrimmedString(1, 30),
    description: fc.oneof(fc.constant(undefined), fc.constant(null), fc.string({ maxLength: 100 })),
  },
  { requiredKeys: ['name', 'type', 'host', 'port', 'database', 'username', 'password'] },
)

/** Valid roleSchema data — roleCode must be uppercase letters and underscores */
const arbRoleCode = fc
  .array(fc.constantFrom(...'ABCDEFGHIJKLMNOPQRSTUVWXYZ_'.split('')), {
    minLength: 1,
    maxLength: 20,
  })
  .map((chars) => chars.join(''))

const arbRoleData = fc.record(
  {
    roleName: arbTrimmedString(1, 20),
    roleCode: arbRoleCode,
    description: fc.oneof(fc.constant(undefined), fc.constant(null), fc.string({ maxLength: 100 })),
    status: fc.oneof(fc.constant(undefined), fc.integer({ min: 0, max: 10 })),
  },
  { requiredKeys: ['roleName', 'roleCode'] },
)

/** Valid menuSchema data */
const arbMenuData = fc.record(
  {
    menuName: arbTrimmedString(1, 20),
    menuCode: arbTrimmedString(1, 20),
    parentId: fc.oneof(fc.constant(undefined), fc.constant(null), fc.integer({ min: 1, max: 10000 })),
    routePath: fc.oneof(fc.constant(undefined), fc.constant(null), fc.string({ maxLength: 50 })),
    icon: fc.oneof(fc.constant(undefined), fc.constant(null), fc.string({ maxLength: 30 })),
    sortOrder: fc.oneof(fc.constant(undefined), fc.integer({ min: 0, max: 999 })),
    status: fc.oneof(fc.constant(undefined), fc.integer({ min: 0, max: 10 })),
  },
  { requiredKeys: ['menuName', 'menuCode'] },
)

/** Valid reportSchema data — code starts with letter, alphanumeric + underscore */
const arbReportCode = fc
  .tuple(
    fc.constantFrom(...'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')),
    fc
      .array(
        fc.constantFrom(...'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_'.split('')),
        { minLength: 0, maxLength: 19 },
      )
      .map((chars) => chars.join('')),
  )
  .map(([first, rest]) => first + rest)

const arbReportData = fc.record(
  {
    name: arbTrimmedString(1, 30),
    code: arbReportCode,
    dataSourceId: fc.integer({ min: 1, max: 100000 }),
    sqlContent: arbTrimmedString(1, 200),
    description: fc.oneof(fc.constant(undefined), fc.constant(null), fc.string({ maxLength: 100 })),
  },
  { requiredKeys: ['name', 'code', 'dataSourceId', 'sqlContent'] },
)

/** Valid chartSchema data */
const arbChartData = fc.record(
  {
    name: arbTrimmedString(1, 30),
    chartType: arbTrimmedString(1, 20),
    dataSourceId: fc.integer({ min: 1, max: 100000 }),
    sqlContent: arbTrimmedString(1, 200),
    chartConfig: fc.oneof(fc.constant(undefined), fc.string({ maxLength: 100 })),
  },
  { requiredKeys: ['name', 'chartType', 'dataSourceId', 'sqlContent'] },
)

// --- Helpers ---

/**
 * Assert that validate returns success and the parsed data is equivalent to
 * what Zod's own parse would produce (accounting for .trim(), .default(), etc.).
 *
 * We compare against schema.parse(data) rather than the raw input, because
 * Zod applies transformations (trim, defaults) that make the output differ
 * from the raw input in expected ways.
 */
function expectValidateSuccess(
  schema: z.ZodType,
  data: Record<string, unknown>,
) {
  const result = validate(schema, data)
  expect(result.success).toBe(true)
  expect(result.data).toBeDefined()

  // The returned data should match what Zod's parse produces
  const expected = schema.parse(data)
  expect(result.data).toEqual(expected)
}

// --- Property Tests ---

describe('Property 6: validate 函数对合法输入的正确性', () => {
  it('validate(loginSchema, data) returns success for any valid login data', () => {
    fc.assert(
      fc.property(arbLoginData, (data) => {
        expectValidateSuccess(loginSchema, data)
      }),
      { numRuns: 100 },
    )
  })

  it('validate(dataSourceSchema, data) returns success for any valid data source data', () => {
    fc.assert(
      fc.property(arbDataSourceData, (data) => {
        expectValidateSuccess(dataSourceSchema, data as Record<string, unknown>)
      }),
      { numRuns: 100 },
    )
  })

  it('validate(roleSchema, data) returns success for any valid role data', () => {
    fc.assert(
      fc.property(arbRoleData, (data) => {
        expectValidateSuccess(roleSchema, data as Record<string, unknown>)
      }),
      { numRuns: 100 },
    )
  })

  it('validate(menuSchema, data) returns success for any valid menu data', () => {
    fc.assert(
      fc.property(arbMenuData, (data) => {
        expectValidateSuccess(menuSchema, data as Record<string, unknown>)
      }),
      { numRuns: 100 },
    )
  })

  it('validate(reportSchema, data) returns success for any valid report data', () => {
    fc.assert(
      fc.property(arbReportData, (data) => {
        expectValidateSuccess(reportSchema, data as Record<string, unknown>)
      }),
      { numRuns: 100 },
    )
  })

  it('validate(chartSchema, data) returns success for any valid chart data', () => {
    fc.assert(
      fc.property(arbChartData, (data) => {
        expectValidateSuccess(chartSchema, data as Record<string, unknown>)
      }),
      { numRuns: 100 },
    )
  })
})

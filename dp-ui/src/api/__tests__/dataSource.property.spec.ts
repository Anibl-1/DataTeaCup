/**
 * Property 2: API 响应 Zod 验证捕获不匹配
 *
 * **Validates: Requirements 2.3**
 * **Feature: frontend-comprehensive-optimization, Property 2: API 响应 Zod 验证捕获不匹配**
 *
 * 对于任意不符合 schema 的响应对象，验证函数应返回失败结果，
 * 且错误信息中包含具体的字段路径。
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { z } from 'zod'

// ==================== Recreate the Zod schemas from dataSource.ts ====================
// These are module-scoped in the source, so we replicate them here for direct testing.

const dataSourceSchema = z.object({
  id: z.number(),
  name: z.string(),
  dbType: z.string(),
  host: z.string(),
  port: z.number(),
  database: z.string(),
  username: z.string(),
  password: z.string(),
  groupName: z.string().optional(),
  createTime: z.string().optional(),
  updateTime: z.string().optional(),
})

const pageResultSchema = <T extends z.ZodTypeAny>(itemSchema: T) =>
  z.object({
    list: z.array(itemSchema),
    total: z.number(),
    page: z.number().optional(),
    pageSize: z.number().optional(),
  })

const apiResponseSchema = <T extends z.ZodTypeAny>(dataSchema: T) =>
  z.object({
    code: z.number(),
    msg: z.string(),
    data: dataSchema,
    timestamp: z.number().optional(),
  })

// Composed schema: the full API response wrapping a paginated data source list
const fullResponseSchema = apiResponseSchema(pageResultSchema(dataSourceSchema))

// ==================== Generators ====================

/**
 * Generator for values that are NOT valid numbers (used to corrupt numeric fields).
 * Produces strings, booleans, null, undefined, arrays, and objects.
 */
const nonNumberArb = fc.oneof(
  fc.string(),
  fc.boolean().map((v) => v as unknown),
  fc.constant(null),
  fc.constant(undefined),
  fc.constant([]),
  fc.constant({}),
)

/**
 * Generator for values that are NOT valid strings.
 */
const nonStringArb = fc.oneof(
  fc.integer(),
  fc.boolean().map((v) => v as unknown),
  fc.constant(null),
  fc.constant(undefined),
  fc.constant([]),
  fc.constant({}),
)

/**
 * A valid data source object generator (used as a baseline for corruption).
 */
const validDataSourceArb = fc.record({
  id: fc.integer({ min: 1 }),
  name: fc.string({ minLength: 1, maxLength: 50 }),
  dbType: fc.constantFrom('mysql', 'postgresql', 'oracle', 'sqlserver'),
  host: fc.string({ minLength: 1, maxLength: 50 }),
  port: fc.integer({ min: 1, max: 65535 }),
  database: fc.string({ minLength: 1, maxLength: 50 }),
  username: fc.string({ minLength: 1, maxLength: 30 }),
  password: fc.string({ minLength: 1, maxLength: 30 }),
})

/**
 * A valid full API response generator (baseline for corruption).
 */
const validFullResponseArb = fc
  .record({
    code: fc.integer(),
    msg: fc.string(),
    list: fc.array(validDataSourceArb, { minLength: 0, maxLength: 3 }),
    total: fc.nat(),
  })
  .map(({ code, msg, list, total }) => ({
    code,
    msg,
    data: { list, total },
  }))

// ==================== Property Tests ====================

describe('Property 2: API 响应 Zod 验证捕获不匹配', () => {
  it('dataSourceSchema rejects objects with a non-number id', () => {
    fc.assert(
      fc.property(validDataSourceArb, nonNumberArb, (base, badId) => {
        const corrupted = { ...base, id: badId }
        const result = dataSourceSchema.safeParse(corrupted)
        expect(result.success).toBe(false)
        if (!result.success) {
          const paths = result.error.issues.map((i) => i.path.join('.'))
          expect(paths).toContain('id')
        }
      }),
      { numRuns: 100 },
    )
  })

  it('dataSourceSchema rejects objects with a non-string name', () => {
    fc.assert(
      fc.property(validDataSourceArb, nonStringArb, (base, badName) => {
        const corrupted = { ...base, name: badName }
        const result = dataSourceSchema.safeParse(corrupted)
        expect(result.success).toBe(false)
        if (!result.success) {
          const paths = result.error.issues.map((i) => i.path.join('.'))
          expect(paths).toContain('name')
        }
      }),
      { numRuns: 100 },
    )
  })

  it('dataSourceSchema rejects objects with a non-number port', () => {
    fc.assert(
      fc.property(validDataSourceArb, nonNumberArb, (base, badPort) => {
        const corrupted = { ...base, port: badPort }
        const result = dataSourceSchema.safeParse(corrupted)
        expect(result.success).toBe(false)
        if (!result.success) {
          const paths = result.error.issues.map((i) => i.path.join('.'))
          expect(paths).toContain('port')
        }
      }),
      { numRuns: 100 },
    )
  })

  it('dataSourceSchema rejects objects with missing required fields', () => {
    const requiredFields = ['id', 'name', 'dbType', 'host', 'port', 'database', 'username', 'password'] as const
    fc.assert(
      fc.property(
        validDataSourceArb,
        fc.constantFrom(...requiredFields),
        (base, fieldToRemove) => {
          const corrupted = { ...base }
          delete (corrupted as Record<string, unknown>)[fieldToRemove]
          const result = dataSourceSchema.safeParse(corrupted)
          expect(result.success).toBe(false)
        },
      ),
      { numRuns: 100 },
    )
  })

  it('apiResponseSchema rejects responses with non-number code', () => {
    fc.assert(
      fc.property(validFullResponseArb, nonNumberArb, (base, badCode) => {
        const corrupted = { ...base, code: badCode }
        const result = fullResponseSchema.safeParse(corrupted)
        expect(result.success).toBe(false)
        if (!result.success) {
          const paths = result.error.issues.map((i) => i.path.join('.'))
          expect(paths).toContain('code')
        }
      }),
      { numRuns: 100 },
    )
  })

  it('apiResponseSchema rejects responses with non-string msg', () => {
    fc.assert(
      fc.property(validFullResponseArb, nonStringArb, (base, badMsg) => {
        const corrupted = { ...base, msg: badMsg }
        const result = fullResponseSchema.safeParse(corrupted)
        expect(result.success).toBe(false)
        if (!result.success) {
          const paths = result.error.issues.map((i) => i.path.join('.'))
          expect(paths).toContain('msg')
        }
      }),
      { numRuns: 100 },
    )
  })

  it('pageResultSchema rejects responses where data.list contains invalid items', () => {
    fc.assert(
      fc.property(
        validFullResponseArb,
        fc.anything().filter((v) => typeof v !== 'object' || v === null || Array.isArray(v)),
        (base, badItem) => {
          const corrupted = {
            ...base,
            data: { ...base.data, list: [badItem] },
          }
          const result = fullResponseSchema.safeParse(corrupted)
          expect(result.success).toBe(false)
        },
      ),
      { numRuns: 100 },
    )
  })

  it('pageResultSchema rejects responses where data.total is not a number', () => {
    fc.assert(
      fc.property(validFullResponseArb, nonNumberArb, (base, badTotal) => {
        const corrupted = {
          ...base,
          data: { ...base.data, total: badTotal },
        }
        const result = fullResponseSchema.safeParse(corrupted)
        expect(result.success).toBe(false)
        if (!result.success) {
          const paths = result.error.issues.map((i) => i.path.join('.'))
          expect(paths).toContain('data.total')
        }
      }),
      { numRuns: 100 },
    )
  })

  it('fullResponseSchema rejects completely arbitrary non-conforming objects', () => {
    fc.assert(
      fc.property(
        fc.anything().filter((v) => {
          // Filter out objects that might accidentally pass validation
          if (typeof v !== 'object' || v === null || Array.isArray(v)) return true
          const obj = v as Record<string, unknown>
          // Must not have all required top-level fields with correct types
          return !(
            typeof obj.code === 'number' &&
            typeof obj.msg === 'string' &&
            typeof obj.data === 'object' &&
            obj.data !== null
          )
        }),
        (arbitrary) => {
          const result = fullResponseSchema.safeParse(arbitrary)
          expect(result.success).toBe(false)
        },
      ),
      { numRuns: 100 },
    )
  })
})

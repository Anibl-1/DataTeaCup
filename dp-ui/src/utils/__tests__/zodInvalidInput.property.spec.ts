/**
 * Feature: frontend-comprehensive-optimization, Property 9: 非法输入验证返回错误
 *
 * **Validates: Requirements 11.4**
 *
 * 对于任意违反 schema 约束的输入（如空字符串用户名、超出范围的端口号、非法格式的邮箱），
 * validate(schema, data) 应返回 { success: false }，且 errors 对象非空。
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import {
  validate,
  loginSchema,
  dataSourceSchema,
  roleSchema,
  userCreateSchema,
} from '../validation'

// --- Invalid Input Generators ---

/**
 * Truly empty string — violates requiredString (min 1).
 * Note: Zod's `.min(1).trim()` checks min(1) BEFORE trimming, so a single
 * space " " actually passes min(1). Only truly empty strings "" fail.
 */
const arbEmpty = fc.constant('')

/**
 * Invalid loginSchema data: at least one of username/password is empty.
 */
const arbInvalidLoginData = fc.oneof(
  fc.record({
    username: arbEmpty,
    password: fc.string({ minLength: 1, maxLength: 20 }),
  }),
  fc.record({
    username: fc.string({ minLength: 1, maxLength: 20 }),
    password: arbEmpty,
  }),
  fc.record({
    username: arbEmpty,
    password: arbEmpty,
  }),
)

/**
 * Invalid dataSourceSchema data: port out of range (0, negative, or > 65535).
 */
const arbInvalidDataSourcePort = fc.record({
  name: fc.string({ minLength: 1, maxLength: 20 }),
  type: fc.string({ minLength: 1, maxLength: 20 }),
  host: fc.string({ minLength: 1, maxLength: 20 }),
  port: fc.oneof(
    fc.integer({ min: -10000, max: 0 }),
    fc.integer({ min: 65536, max: 100000 }),
  ),
  database: fc.string({ minLength: 1, maxLength: 20 }),
  username: fc.string({ minLength: 1, maxLength: 20 }),
  password: fc.string({ minLength: 1, maxLength: 20 }),
})

/**
 * Invalid dataSourceSchema data: empty required name field.
 */
const arbInvalidDataSourceEmpty = fc.record({
  name: arbEmpty,
  type: fc.string({ minLength: 1, maxLength: 20 }),
  host: fc.string({ minLength: 1, maxLength: 20 }),
  port: fc.integer({ min: 1, max: 65535 }),
  database: fc.string({ minLength: 1, maxLength: 20 }),
  username: fc.string({ minLength: 1, maxLength: 20 }),
  password: fc.string({ minLength: 1, maxLength: 20 }),
})

/**
 * Invalid roleSchema data: roleCode contains lowercase or digits (violates /^[A-Z_]+$/).
 */
const arbInvalidRoleCode = fc.oneof(
  fc
    .array(fc.constantFrom(...'abcdefghijklmnopqrstuvwxyz'.split('')), { minLength: 1, maxLength: 10 })
    .map((chars) => chars.join('')),
  fc
    .array(fc.constantFrom(...'0123456789'.split('')), { minLength: 1, maxLength: 10 })
    .map((chars) => chars.join('')),
  fc
    .array(fc.constantFrom(...'abc!@#$%'.split('')), { minLength: 1, maxLength: 10 })
    .map((chars) => chars.join('')),
)

const arbInvalidRoleData = fc.record({
  roleName: fc.string({ minLength: 1, maxLength: 20 }),
  roleCode: arbInvalidRoleCode,
  status: fc.integer({ min: 0, max: 10 }),
})

/**
 * Invalid userCreateSchema data: username too short (< 3 chars after trim).
 * Generate 1-2 visible characters so trim doesn't change length.
 */
const arbInvalidUserShortUsername = fc.record({
  username: fc
    .array(fc.constantFrom(...'abcdefghijklmnopqrstuvwxyz'.split('')), { minLength: 1, maxLength: 2 })
    .map((chars) => chars.join('')),
  password: fc.string({ minLength: 6, maxLength: 20 }),
  nickname: fc.string({ minLength: 1, maxLength: 20 }),
})

/**
 * Invalid userCreateSchema data: password too short (< 6 chars).
 */
const arbInvalidUserShortPassword = fc.record({
  username: fc.string({ minLength: 3, maxLength: 20 }),
  password: fc.string({ minLength: 1, maxLength: 5 }),
  nickname: fc.string({ minLength: 1, maxLength: 20 }),
})

// --- Property Tests ---

describe('Property 9: 非法输入验证返回错误', () => {
  it('validate(loginSchema, data) returns failure for empty username or password', () => {
    fc.assert(
      fc.property(arbInvalidLoginData, (data) => {
        const result = validate(loginSchema, data)
        expect(result.success).toBe(false)
        expect(result.errors).toBeDefined()
        expect(Object.keys(result.errors!).length).toBeGreaterThan(0)
      }),
      { numRuns: 100 },
    )
  })

  it('validate(dataSourceSchema, data) returns failure for out-of-range port', () => {
    fc.assert(
      fc.property(arbInvalidDataSourcePort, (data) => {
        const result = validate(dataSourceSchema, data)
        expect(result.success).toBe(false)
        expect(result.errors).toBeDefined()
        expect(Object.keys(result.errors!).length).toBeGreaterThan(0)
      }),
      { numRuns: 100 },
    )
  })

  it('validate(dataSourceSchema, data) returns failure for empty required name', () => {
    fc.assert(
      fc.property(arbInvalidDataSourceEmpty, (data) => {
        const result = validate(dataSourceSchema, data)
        expect(result.success).toBe(false)
        expect(result.errors).toBeDefined()
        expect(Object.keys(result.errors!).length).toBeGreaterThan(0)
      }),
      { numRuns: 100 },
    )
  })

  it('validate(roleSchema, data) returns failure for invalid roleCode format', () => {
    fc.assert(
      fc.property(arbInvalidRoleData, (data) => {
        const result = validate(roleSchema, data)
        expect(result.success).toBe(false)
        expect(result.errors).toBeDefined()
        expect(Object.keys(result.errors!).length).toBeGreaterThan(0)
      }),
      { numRuns: 100 },
    )
  })

  it('validate(userCreateSchema, data) returns failure for too-short username', () => {
    fc.assert(
      fc.property(arbInvalidUserShortUsername, (data) => {
        const result = validate(userCreateSchema, data)
        expect(result.success).toBe(false)
        expect(result.errors).toBeDefined()
        expect(Object.keys(result.errors!).length).toBeGreaterThan(0)
      }),
      { numRuns: 100 },
    )
  })

  it('validate(userCreateSchema, data) returns failure for too-short password', () => {
    fc.assert(
      fc.property(arbInvalidUserShortPassword, (data) => {
        const result = validate(userCreateSchema, data)
        expect(result.success).toBe(false)
        expect(result.errors).toBeDefined()
        expect(Object.keys(result.errors!).length).toBeGreaterThan(0)
      }),
      { numRuns: 100 },
    )
  })
})

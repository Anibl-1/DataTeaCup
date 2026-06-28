/**
 * Feature: mars-integration-optimization, Property 11: GET 请求去重
 * Validates: Requirements 5.1
 *
 * Tests the core of the GET request deduplication mechanism:
 * the `getRequestKey` function that generates unique keys from method+url+params.
 */
import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { getRequestKey } from '../request'

// --- Generators ---

/** Generate a valid HTTP method string */
const arbMethod = fc.constantFrom('get', 'GET', 'post', 'PUT', 'delete', 'patch')

/** Generate a URL-like path */
const arbUrl = fc.array(
  fc.constantFrom(
    'a', 'b', 'c', 'd', 'e', 'f', '/', '-', '_', '0', '1', '2', '3'
  ),
  { minLength: 1, maxLength: 30 }
).map(chars => `/api/${chars.join('')}`)

/** Generate a params object with string/number values */
const arbParams = fc.option(
  fc.dictionary(
    fc.string({ minLength: 1, maxLength: 10 }).filter(s => /^[a-zA-Z_]\w*$/.test(s)),
    fc.oneof(fc.string({ maxLength: 20 }), fc.integer(), fc.boolean()),
    { minKeys: 0, maxKeys: 5 }
  ),
  { nil: undefined }
)

/** Generate a full request config */
const arbRequestConfig = fc.record({
  method: arbMethod,
  url: arbUrl,
  params: arbParams,
})

describe('Property 11: GET 请求去重 - getRequestKey', () => {
  it('should be deterministic: same input always produces the same key', () => {
    fc.assert(
      fc.property(arbRequestConfig, (config) => {
        const key1 = getRequestKey(config)
        const key2 = getRequestKey(config)
        expect(key1).toBe(key2)
      }),
      { numRuns: 200 }
    )
  })

  it('should produce identical keys for identical method+url+params', () => {
    fc.assert(
      fc.property(arbRequestConfig, (config) => {
        // Create a deep copy to ensure we're comparing by value, not reference
        const copy = {
          method: config.method,
          url: config.url,
          params: config.params ? { ...config.params } : config.params,
        }
        expect(getRequestKey(config)).toBe(getRequestKey(copy))
      }),
      { numRuns: 200 }
    )
  })

  it('should produce different keys when method differs', () => {
    fc.assert(
      fc.property(
        arbUrl,
        arbParams,
        fc.constantFrom('get', 'post').chain(m1 =>
          fc.constantFrom(...(['get', 'post', 'put', 'delete'] as const).filter(m => m !== m1)).map(m2 => [m1, m2] as const)
        ),
        (url, params, [method1, method2]) => {
          const key1 = getRequestKey({ method: method1, url, params })
          const key2 = getRequestKey({ method: method2, url, params })
          expect(key1).not.toBe(key2)
        }
      ),
      { numRuns: 200 }
    )
  })

  it('should produce different keys when url differs', () => {
    fc.assert(
      fc.property(
        arbMethod,
        arbParams,
        arbUrl,
        arbUrl,
        (method, params, url1, url2) => {
          fc.pre(url1 !== url2)
          const key1 = getRequestKey({ method, url: url1, params })
          const key2 = getRequestKey({ method, url: url2, params })
          expect(key1).not.toBe(key2)
        }
      ),
      { numRuns: 200 }
    )
  })

  it('should produce different keys when params differ', () => {
    fc.assert(
      fc.property(
        arbMethod,
        arbUrl,
        (method, url) => {
          const params1 = { page: 1 }
          const params2 = { page: 2 }
          const key1 = getRequestKey({ method, url, params: params1 })
          const key2 = getRequestKey({ method, url, params: params2 })
          expect(key1).not.toBe(key2)
        }
      ),
      { numRuns: 100 }
    )
  })

  it('should encode the key as method:url:JSON(params)', () => {
    fc.assert(
      fc.property(arbRequestConfig, (config) => {
        const key = getRequestKey(config)
        const expected = `${config.method}:${config.url}:${JSON.stringify(config.params)}`
        expect(key).toBe(expected)
      }),
      { numRuns: 200 }
    )
  })
})

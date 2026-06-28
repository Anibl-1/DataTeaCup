/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * StatusTag 状态映射正确性 属性测试
 * Feature: core-modules-deep-optimization, Property 9: StatusTag 状态映射正确性
 *
 * **Validates: Requirements 5.1**
 *
 * For any status 值和 statusMap 配置，当 status 存在于 statusMap 中时，
 * StatusTag 应渲染 statusMap[status] 中配置的 label 和 type。
 */

import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import * as fc from 'fast-check'
import StatusTag from '../common/StatusTag.vue'

// ============================================================================
// Types
// ============================================================================

type TagType = 'success' | 'warning' | 'error' | 'info' | 'default'

interface StatusConfig {
  label: string
  type: TagType
}

// ============================================================================
// Pure logic extracted from StatusTag for direct testing
// ============================================================================

function getTagLabel(
  status: string | number,
  statusMap: Record<string | number, StatusConfig>,
): string {
  const config = statusMap[status]
  return config?.label ?? String(status)
}

function getTagType(
  status: string | number,
  statusMap: Record<string | number, StatusConfig>,
): TagType {
  const config = statusMap[status]
  return config?.type ?? 'default'
}

// ============================================================================
// Arbitraries (测试数据生成器)
// ============================================================================

/** Generate a valid tag type */
const tagTypeArb: fc.Arbitrary<TagType> = fc.constantFrom(
  'success',
  'warning',
  'error',
  'info',
  'default',
)

/** Generate a non-empty trimmed label string (alphanumeric to avoid whitespace issues) */
const labelArb = fc
  .array(fc.constantFrom(...'abcdefghijklmnopqrstuvwxyz0123456789'.split('')), {
    minLength: 1,
    maxLength: 15,
  })
  .map((chars) => chars.join(''))

/** Generate a StatusConfig */
const statusConfigArb: fc.Arbitrary<StatusConfig> = fc.record({
  label: labelArb,
  type: tagTypeArb,
})

/** Generate a status key (string or number) */
const statusKeyStringArb = fc
  .array(fc.constantFrom(...'abcdefghijklmnopqrstuvwxyz0123456789'.split('')), {
    minLength: 1,
    maxLength: 10,
  })
  .map((chars) => chars.join(''))

const statusKeyArb = fc.oneof(statusKeyStringArb, fc.integer({ min: 0, max: 100 }))

/** Generate a statusMap with at least one entry */
const statusMapArb = fc
  .array(fc.tuple(statusKeyStringArb, statusConfigArb), { minLength: 1, maxLength: 8 })
  .map((entries) => {
    const map: Record<string, StatusConfig> = {}
    for (const [key, config] of entries) {
      map[key] = config
    }
    return map
  })
  .filter((map) => Object.keys(map).length > 0)

// ============================================================================
// Property-Based Tests
// ============================================================================

describe('StatusTag — Property Tests', () => {
  /**
   * Property 9: StatusTag 状态映射正确性
   *
   * **Validates: Requirements 5.1**
   * **Feature: core-modules-deep-optimization, Property 9: StatusTag 状态映射正确性**
   */
  describe('Property 9: StatusTag renders correct label and type from statusMap', () => {
    it('should compute the correct label when status exists in statusMap', () => {
      fc.assert(
        fc.property(statusMapArb, (statusMap) => {
          const keys = Object.keys(statusMap)
          const status = keys[0]

          const result = getTagLabel(status, statusMap)
          expect(result).toBe(statusMap[status].label)
        }),
        { numRuns: 100 },
      )
    })

    it('should compute the correct type when status exists in statusMap', () => {
      fc.assert(
        fc.property(statusMapArb, (statusMap) => {
          const keys = Object.keys(statusMap)
          const status = keys[0]

          const result = getTagType(status, statusMap)
          expect(result).toBe(statusMap[status].type)
        }),
        { numRuns: 100 },
      )
    })

    it('should render the mapped label text for every key in statusMap', () => {
      fc.assert(
        fc.property(statusMapArb, (statusMap) => {
          const keys = Object.keys(statusMap)

          for (const status of keys) {
            const wrapper = mount(StatusTag, {
              props: { status, statusMap },
            })

            const expectedLabel = statusMap[status].label
            // wrapper.text() returns the visible text content
            expect(wrapper.text()).toContain(expectedLabel)

            wrapper.unmount()
          }
        }),
        { numRuns: 100 },
      )
    })

    it('should render the mapped type via NTag props for every key in statusMap', () => {
      fc.assert(
        fc.property(statusMapArb, (statusMap) => {
          const keys = Object.keys(statusMap)

          for (const status of keys) {
            const wrapper = mount(StatusTag, {
              props: { status, statusMap },
            })

            const expectedType = statusMap[status].type
            // Find the NTag component - it may be registered under different names
            const nTag =
              wrapper.findComponent({ name: 'NTag' }) ||
              wrapper.findComponent({ name: 'Tag' })

            if (nTag.exists()) {
              expect(nTag.props('type')).toBe(expectedType)
            } else {
              // Fallback: verify via the wrapper's vm internal state
              // The component exposes tagType as a computed
              const vm = wrapper.vm as any
              expect(vm.tagType).toBe(expectedType)
            }

            wrapper.unmount()
          }
        }),
        { numRuns: 100 },
      )
    })

    it('should fallback to default type and raw status text when status is NOT in statusMap', () => {
      fc.assert(
        fc.property(
          statusMapArb,
          statusKeyStringArb,
          (statusMap, candidateStatus) => {
            // Ensure the status is NOT in the map
            if (candidateStatus in statusMap) return // skip

            const wrapper = mount(StatusTag, {
              props: { status: candidateStatus, statusMap },
            })

            // Should display the raw status value
            expect(wrapper.text()).toContain(candidateStatus)

            // Verify default type via vm
            const vm = wrapper.vm as any
            expect(vm.tagType).toBe('default')

            wrapper.unmount()
          },
        ),
        { numRuns: 100 },
      )
    })

    it('should render with numeric status keys correctly', () => {
      fc.assert(
        fc.property(
          fc.integer({ min: 0, max: 50 }),
          statusConfigArb,
          (numStatus, config) => {
            const statusMap: Record<number, StatusConfig> = { [numStatus]: config }

            const wrapper = mount(StatusTag, {
              props: { status: numStatus, statusMap },
            })

            expect(wrapper.text()).toContain(config.label)

            const vm = wrapper.vm as any
            expect(vm.tagType).toBe(config.type)

            wrapper.unmount()
          },
        ),
        { numRuns: 100 },
      )
    })
  })
})

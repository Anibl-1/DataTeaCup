/**
 * 属性测试：响应式布局适配正确性
 * Property Test: Responsive Layout Adaptation Correctness
 *
 * **Validates: Requirements 26.1**
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import {
  getDeviceType,
  getColumns,
  getContainerPadding,
  getFontSize,
  MOBILE_BREAKPOINT,
  TABLET_BREAKPOINT,
  type DeviceType
} from '../useResponsiveLayout'

describe('Feature: enterprise-production-ready, Property 23: 响应式布局适配正确性', () => {
  /**
   * @Label Feature: enterprise-production-ready, Property 23.1: 设备类型与宽度一致性
   * 对于任意屏幕宽度，getDeviceType 应返回正确的设备类型
   */
  it('Property 23.1: 设备类型与宽度一致性', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 1, max: 4000 }),
        (width: number) => {
          const deviceType = getDeviceType(width)

          if (width < MOBILE_BREAKPOINT) {
            expect(deviceType).toBe('mobile')
          } else if (width < TABLET_BREAKPOINT) {
            expect(deviceType).toBe('tablet')
          } else {
            expect(deviceType).toBe('desktop')
          }
        }
      ),
      { numRuns: 200 }
    )
  })

  /**
   * @Label Feature: enterprise-production-ready, Property 23.2: 设备类型互斥且完备
   * 对于任意屏幕宽度，设备类型必须是 mobile/tablet/desktop 之一
   */
  it('Property 23.2: 设备类型互斥且完备', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 1, max: 5000 }),
        (width: number) => {
          const deviceType = getDeviceType(width)
          expect(['mobile', 'tablet', 'desktop']).toContain(deviceType)
        }
      ),
      { numRuns: 200 }
    )
  })

  /**
   * @Label Feature: enterprise-production-ready, Property 23.3: 列数随设备类型单调递增
   * desktop 列数 >= tablet 列数 >= mobile 列数
   */
  it('Property 23.3: 列数随设备类型单调递增', () => {
    fc.assert(
      fc.property(
        fc.boolean(),
        (isLandscape: boolean) => {
          const mobileCols = getColumns('mobile', isLandscape)
          const tabletCols = getColumns('tablet', isLandscape)
          const desktopCols = getColumns('desktop', isLandscape)

          expect(desktopCols).toBeGreaterThanOrEqual(tabletCols)
          expect(tabletCols).toBeGreaterThanOrEqual(mobileCols)
          expect(mobileCols).toBeGreaterThanOrEqual(1)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * @Label Feature: enterprise-production-ready, Property 23.4: 横屏列数 >= 竖屏列数
   * 对于任意设备类型，横屏模式的列数应大于等于竖屏模式
   */
  it('Property 23.4: 横屏列数 >= 竖屏列数', () => {
    const deviceTypes: DeviceType[] = ['mobile', 'tablet', 'desktop']

    for (const dt of deviceTypes) {
      const portraitCols = getColumns(dt, false)
      const landscapeCols = getColumns(dt, true)
      expect(landscapeCols).toBeGreaterThanOrEqual(portraitCols)
    }
  })

  /**
   * @Label Feature: enterprise-production-ready, Property 23.5: 断点边界正确性
   * 断点边界值应正确归类
   */
  it('Property 23.5: 断点边界正确性', () => {
    // 刚好在 mobile 断点
    expect(getDeviceType(MOBILE_BREAKPOINT - 1)).toBe('mobile')
    expect(getDeviceType(MOBILE_BREAKPOINT)).toBe('tablet')

    // 刚好在 tablet 断点
    expect(getDeviceType(TABLET_BREAKPOINT - 1)).toBe('tablet')
    expect(getDeviceType(TABLET_BREAKPOINT)).toBe('desktop')
  })

  /**
   * @Label Feature: enterprise-production-ready, Property 23.6: 容器内边距和字号与设备类型一致
   */
  it('Property 23.6: 容器内边距和字号与设备类型一致', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 1, max: 4000 }),
        (width: number) => {
          const dt = getDeviceType(width)
          const padding = getContainerPadding(dt)
          const fontSize = getFontSize(dt)

          // padding 和 fontSize 应为有效 CSS 值
          expect(padding).toMatch(/^\d+px$/)
          expect(fontSize).toMatch(/^\d+px$/)

          // mobile 的 padding 应最小
          const mobilePadding = parseInt(getContainerPadding('mobile'))
          const desktopPadding = parseInt(getContainerPadding('desktop'))
          expect(desktopPadding).toBeGreaterThanOrEqual(mobilePadding)
        }
      ),
      { numRuns: 100 }
    )
  })
})

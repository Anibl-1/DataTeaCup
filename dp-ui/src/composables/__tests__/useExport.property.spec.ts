/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as fc from 'fast-check'

// Mock naive-ui useMessage
vi.mock('naive-ui', () => ({
  useMessage: () => ({
    error: vi.fn(),
    success: vi.fn(),
    info: vi.fn(),
    warning: vi.fn(),
  }),
}))

// Mock @/utils/export
const mockExportToExcel = vi.fn()
vi.mock('@/utils/export', () => ({
  exportToExcel: (...args: any[]) => mockExportToExcel(...args),
}))

// Mock @/utils/logger
vi.mock('@/utils/logger', () => ({
  logger: {
    error: vi.fn(),
    warn: vi.fn(),
    info: vi.fn(),
    debug: vi.fn(),
  },
}))

import { useExport } from '../useExport'

/**
 * Property 5: useExport 阈值路由正确性
 *
 * **Validates: Requirements 3.3**
 * **Feature: core-modules-deep-optimization, Property 5: useExport 阈值路由正确性**
 *
 * For any 数据数组，当数组长度超过配置的 maxFrontendRows 阈值时，
 * 导出操作应调用 backendExportFn 而非前端导出逻辑。
 */

/**
 * Property 6: useExport 导出锁防重复
 *
 * **Validates: Requirements 3.4**
 * **Feature: core-modules-deep-optimization, Property 6: useExport 导出锁防重复**
 *
 * For any 导出操作，当 exporting 状态为 true 时，再次调用导出方法应被忽略（不执行），
 * 确保不会重复触发导出。
 */

describe('useExport — Property Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockExportToExcel.mockReset()
  })

  // --- Generators ---

  /** Arbitrary threshold for maxFrontendRows */
  const thresholdArb = fc.integer({ min: 1, max: 1000 })

  // --- Property 5 Tests ---

  describe('Property 5: useExport 阈值路由正确性', () => {
    it('when data.length > maxFrontendRows and backendExportFn is provided, exportExcel calls backendExportFn', async () => {
      // Setup DOM mocks for downloadBlob (backend export returns a Blob that gets downloaded)
      const origCreateObjectURL = globalThis.URL.createObjectURL
      const origRevokeObjectURL = globalThis.URL.revokeObjectURL
      globalThis.URL.createObjectURL = vi.fn().mockReturnValue('blob:test')
      globalThis.URL.revokeObjectURL = vi.fn()
      const mockCreateElement = vi.spyOn(document, 'createElement').mockReturnValue({
        href: '',
        download: '',
        click: vi.fn(),
      } as any)
      const mockAppendChild = vi.spyOn(document.body, 'appendChild').mockImplementation(vi.fn() as any)
      const mockRemoveChild = vi.spyOn(document.body, 'removeChild').mockImplementation(vi.fn() as any)

      try {
        await fc.assert(
          fc.asyncProperty(
            thresholdArb,
            fc.integer({ min: 1, max: 200 }),
            async (threshold, extra) => {
              const dataLength = threshold + extra // always > threshold
              const data = Array.from({ length: dataLength }, (_, i) => ({ id: i, name: `r${i}` }))

              const backendExportFn = vi.fn().mockResolvedValue(new Blob(['test']))
              mockExportToExcel.mockReset()

              const { exportExcel } = useExport({
                backendExportFn,
                maxFrontendRows: threshold,
              })

              await exportExcel(data, 'test')

              expect(backendExportFn).toHaveBeenCalledTimes(1)
              expect(mockExportToExcel).not.toHaveBeenCalled()
            },
          ),
          { numRuns: 100 },
        )
      } finally {
        globalThis.URL.createObjectURL = origCreateObjectURL
        globalThis.URL.revokeObjectURL = origRevokeObjectURL
        mockCreateElement.mockRestore()
        mockAppendChild.mockRestore()
        mockRemoveChild.mockRestore()
      }
    })

    it('when data.length <= maxFrontendRows, exportExcel calls frontend exportToExcel', async () => {
      await fc.assert(
        fc.asyncProperty(
          thresholdArb,
          fc.integer({ min: 0, max: 200 }),
          async (threshold, offset) => {
            // dataLength <= threshold
            const dataLength = Math.max(0, threshold - offset)
            const data = Array.from({ length: dataLength }, (_, i) => ({ id: i, name: `r${i}` }))

            const backendExportFn = vi.fn().mockResolvedValue(new Blob(['test']))
            mockExportToExcel.mockReset()

            const { exportExcel } = useExport({
              backendExportFn,
              maxFrontendRows: threshold,
            })

            await exportExcel(data, 'test')

            expect(mockExportToExcel).toHaveBeenCalledTimes(1)
            expect(mockExportToExcel).toHaveBeenCalledWith(data, 'test')
            expect(backendExportFn).not.toHaveBeenCalled()
          },
        ),
        { numRuns: 100 },
      )
    })

    it('when data.length > maxFrontendRows but no backendExportFn, uses frontend export', async () => {
      await fc.assert(
        fc.asyncProperty(
          thresholdArb,
          fc.integer({ min: 1, max: 200 }),
          async (threshold, extra) => {
            const dataLength = threshold + extra
            const data = Array.from({ length: dataLength }, (_, i) => ({ id: i, name: `r${i}` }))

            mockExportToExcel.mockReset()

            const { exportExcel } = useExport({
              // no backendExportFn
              maxFrontendRows: threshold,
            })

            await exportExcel(data, 'test')

            // Without backendExportFn, should fall through to frontend export
            expect(mockExportToExcel).toHaveBeenCalledTimes(1)
          },
        ),
        { numRuns: 100 },
      )
    })

    it('when data.length > maxFrontendRows and backendExportFn is provided, exportCsv calls backendExportFn', async () => {
      // Setup DOM mocks for downloadBlob
      const origCreateObjectURL = globalThis.URL.createObjectURL
      const origRevokeObjectURL = globalThis.URL.revokeObjectURL
      globalThis.URL.createObjectURL = vi.fn().mockReturnValue('blob:test')
      globalThis.URL.revokeObjectURL = vi.fn()
      const mockCreateElement = vi.spyOn(document, 'createElement').mockReturnValue({
        href: '',
        download: '',
        click: vi.fn(),
      } as any)
      const mockAppendChild = vi.spyOn(document.body, 'appendChild').mockImplementation(vi.fn() as any)
      const mockRemoveChild = vi.spyOn(document.body, 'removeChild').mockImplementation(vi.fn() as any)

      try {
        await fc.assert(
          fc.asyncProperty(
            thresholdArb,
            fc.integer({ min: 1, max: 200 }),
            async (threshold, extra) => {
              const dataLength = threshold + extra
              const data = Array.from({ length: dataLength }, (_, i) => ({ id: i, name: `r${i}` }))

              const backendExportFn = vi.fn().mockResolvedValue(new Blob(['test']))
              mockExportToExcel.mockReset()

              const { exportCsv } = useExport({
                backendExportFn,
                maxFrontendRows: threshold,
              })

              await exportCsv(data, 'test')

              expect(backendExportFn).toHaveBeenCalledTimes(1)
              expect(mockExportToExcel).not.toHaveBeenCalled()
            },
          ),
          { numRuns: 100 },
        )
      } finally {
        globalThis.URL.createObjectURL = origCreateObjectURL
        globalThis.URL.revokeObjectURL = origRevokeObjectURL
        mockCreateElement.mockRestore()
        mockAppendChild.mockRestore()
        mockRemoveChild.mockRestore()
      }
    })
  })

  // --- Property 6 Tests ---

  describe('Property 6: useExport 导出锁防重复', () => {
    it('exportExcel is ignored when exporting is already true', async () => {
      await fc.assert(
        fc.asyncProperty(
          thresholdArb,
          fc.integer({ min: 0, max: 100 }),
          async (threshold, dataLen) => {
            const data = Array.from({ length: dataLen }, (_, i) => ({ id: i, name: `r${i}` }))

            const backendExportFn = vi.fn().mockResolvedValue(new Blob(['test']))
            mockExportToExcel.mockReset()

            const { exportExcel, exporting } = useExport({
              backendExportFn,
              maxFrontendRows: threshold,
            })

            // Manually set exporting to true to simulate an ongoing export
            exporting.value = true

            await exportExcel(data, 'test')

            // Neither backend nor frontend export should be called
            expect(backendExportFn).not.toHaveBeenCalled()
            expect(mockExportToExcel).not.toHaveBeenCalled()
          },
        ),
        { numRuns: 100 },
      )
    })

    it('exportCsv is ignored when exporting is already true', async () => {
      await fc.assert(
        fc.asyncProperty(
          thresholdArb,
          fc.integer({ min: 0, max: 100 }),
          async (threshold, dataLen) => {
            const data = Array.from({ length: dataLen }, (_, i) => ({ id: i, name: `r${i}` }))

            const backendExportFn = vi.fn().mockResolvedValue(new Blob(['test']))
            mockExportToExcel.mockReset()

            const { exportCsv, exporting } = useExport({
              backendExportFn,
              maxFrontendRows: threshold,
            })

            // Manually set exporting to true
            exporting.value = true

            await exportCsv(data, 'test')

            // Neither backend nor frontend export should be called
            expect(backendExportFn).not.toHaveBeenCalled()
            expect(mockExportToExcel).not.toHaveBeenCalled()
          },
        ),
        { numRuns: 100 },
      )
    })

    it('export lock is released after successful export, allowing subsequent calls', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.integer({ min: 0, max: 50 }),
          async (dataLen) => {
            const data = Array.from({ length: dataLen }, (_, i) => ({ id: i, name: `r${i}` }))

            mockExportToExcel.mockReset()

            const { exportExcel, exporting } = useExport({
              maxFrontendRows: 10000, // high threshold so frontend export is used
            })

            // First call should succeed
            await exportExcel(data, 'test')
            expect(exporting.value).toBe(false)
            expect(mockExportToExcel).toHaveBeenCalledTimes(1)

            // Second call should also succeed (lock released)
            await exportExcel(data, 'test2')
            expect(exporting.value).toBe(false)
            expect(mockExportToExcel).toHaveBeenCalledTimes(2)
          },
        ),
        { numRuns: 100 },
      )
    })
  })
})

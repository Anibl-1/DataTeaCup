import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock message
vi.mock('../message', () => ({
  message: {
    success: vi.fn(),
    error: vi.fn()
  }
}))

import { copyToClipboard } from '../copy'
import { message } from '../message'

describe('copy', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('copyToClipboard', () => {
    it('should copy text using clipboard API when available', async () => {
      const writeTextMock = vi.fn().mockResolvedValue(undefined)
      Object.assign(navigator, {
        clipboard: {
          writeText: writeTextMock
        }
      })
      Object.defineProperty(window, 'isSecureContext', { value: true, writable: true })

      const result = await copyToClipboard('test text')
      
      expect(result).toBe(true)
      expect(writeTextMock).toHaveBeenCalledWith('test text')
      expect(message.success).toHaveBeenCalledWith('已复制到剪贴板')
    })

    it('should not show message when showMessage is false', async () => {
      const writeTextMock = vi.fn().mockResolvedValue(undefined)
      Object.assign(navigator, {
        clipboard: {
          writeText: writeTextMock
        }
      })
      Object.defineProperty(window, 'isSecureContext', { value: true, writable: true })

      await copyToClipboard('test text', false)
      
      expect(message.success).not.toHaveBeenCalled()
    })

    it('should handle clipboard API error', async () => {
      const writeTextMock = vi.fn().mockRejectedValue(new Error('Failed'))
      Object.assign(navigator, {
        clipboard: {
          writeText: writeTextMock
        }
      })
      Object.defineProperty(window, 'isSecureContext', { value: true, writable: true })

      const result = await copyToClipboard('test text')
      
      expect(result).toBe(false)
      expect(message.error).toHaveBeenCalledWith('复制失败')
    })
  })
})

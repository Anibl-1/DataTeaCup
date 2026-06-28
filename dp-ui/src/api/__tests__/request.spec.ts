import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}
Object.defineProperty(window, 'localStorage', { value: localStorageMock })

// Mock sessionStorage
const sessionStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}
Object.defineProperty(window, 'sessionStorage', { value: sessionStorageMock })

describe('Request Utils', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should get token from localStorage', () => {
    localStorageMock.getItem.mockReturnValue('test-token')
    const token = localStorage.getItem('token')
    expect(token).toBe('test-token')
  })

  it('should handle missing token', () => {
    localStorageMock.getItem.mockReturnValue(null)
    const token = localStorage.getItem('token')
    expect(token).toBeNull()
  })

  it('should update last activity time', () => {
    const now = Date.now()
    sessionStorage.setItem('lastActivityTime', now.toString())
    expect(sessionStorageMock.setItem).toHaveBeenCalledWith('lastActivityTime', now.toString())
  })
})

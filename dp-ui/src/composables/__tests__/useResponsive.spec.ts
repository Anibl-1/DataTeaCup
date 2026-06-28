/**
 * useResponsive Composable Tests
 * 
 * Validates: Requirements 1.2
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { 
  useResponsive, 
  BREAKPOINTS, 
  getBreakpointFromWidth,
  type Breakpoint 
} from '../useResponsive'

// Mock window object
const mockWindow = {
  innerWidth: 1920,
  addEventListener: vi.fn(),
  removeEventListener: vi.fn()
}

describe('useResponsive', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    // Reset window mock
    Object.defineProperty(global, 'window', {
      value: mockWindow,
      writable: true
    })
    mockWindow.innerWidth = 1920
    mockWindow.addEventListener.mockClear()
    mockWindow.removeEventListener.mockClear()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('BREAKPOINTS', () => {
    it('should have correct breakpoint values', () => {
      expect(BREAKPOINTS.xs).toBe(1280)
      expect(BREAKPOINTS.sm).toBe(1366)
      expect(BREAKPOINTS.md).toBe(1440)
      expect(BREAKPOINTS.lg).toBe(1920)
    })
  })

  describe('getBreakpointFromWidth', () => {
    it('should return xs for width < 1366', () => {
      expect(getBreakpointFromWidth(1280)).toBe('xs')
      expect(getBreakpointFromWidth(1300)).toBe('xs')
      expect(getBreakpointFromWidth(1365)).toBe('xs')
    })

    it('should return sm for width >= 1366 and < 1440', () => {
      expect(getBreakpointFromWidth(1366)).toBe('sm')
      expect(getBreakpointFromWidth(1400)).toBe('sm')
      expect(getBreakpointFromWidth(1439)).toBe('sm')
    })

    it('should return md for width >= 1440 and < 1920', () => {
      expect(getBreakpointFromWidth(1440)).toBe('md')
      expect(getBreakpointFromWidth(1600)).toBe('md')
      expect(getBreakpointFromWidth(1919)).toBe('md')
    })

    it('should return lg for width >= 1920', () => {
      expect(getBreakpointFromWidth(1920)).toBe('lg')
      expect(getBreakpointFromWidth(2560)).toBe('lg')
      expect(getBreakpointFromWidth(3840)).toBe('lg')
    })

    it('should return xs for very small widths', () => {
      expect(getBreakpointFromWidth(800)).toBe('xs')
      expect(getBreakpointFromWidth(1024)).toBe('xs')
      expect(getBreakpointFromWidth(1279)).toBe('xs')
    })
  })

  describe('useResponsive composable', () => {
    describe('initial state', () => {
      it('should return correct breakpoint for large screen (1920px)', () => {
        mockWindow.innerWidth = 1920
        const { breakpoint, width } = useResponsive()
        
        expect(width.value).toBe(1920)
        expect(breakpoint.value).toBe('lg')
      })

      it('should return correct breakpoint for medium screen (1440px)', () => {
        mockWindow.innerWidth = 1440
        const { breakpoint, width } = useResponsive()
        
        expect(width.value).toBe(1440)
        expect(breakpoint.value).toBe('md')
      })

      it('should return correct breakpoint for small screen (1366px)', () => {
        mockWindow.innerWidth = 1366
        const { breakpoint, width } = useResponsive()
        
        expect(width.value).toBe(1366)
        expect(breakpoint.value).toBe('sm')
      })

      it('should return correct breakpoint for extra small screen (1280px)', () => {
        mockWindow.innerWidth = 1280
        const { breakpoint, width } = useResponsive()
        
        expect(width.value).toBe(1280)
        expect(breakpoint.value).toBe('xs')
      })
    })

    describe('device type detection', () => {
      it('should detect mobile for width < 1280', () => {
        mockWindow.innerWidth = 1024
        const { isMobile, isTablet, isDesktop } = useResponsive()
        
        expect(isMobile.value).toBe(true)
        expect(isTablet.value).toBe(false)
        expect(isDesktop.value).toBe(false)
      })

      it('should detect tablet for width 1280-1439', () => {
        mockWindow.innerWidth = 1366
        const { isMobile, isTablet, isDesktop } = useResponsive()
        
        expect(isMobile.value).toBe(false)
        expect(isTablet.value).toBe(true)
        expect(isDesktop.value).toBe(false)
      })

      it('should detect desktop for width >= 1440', () => {
        mockWindow.innerWidth = 1440
        const { isMobile, isTablet, isDesktop } = useResponsive()
        
        expect(isMobile.value).toBe(false)
        expect(isTablet.value).toBe(false)
        expect(isDesktop.value).toBe(true)
      })

      it('should detect large screen for width >= 1920', () => {
        mockWindow.innerWidth = 1920
        const { isLargeScreen, isDesktop } = useResponsive()
        
        expect(isLargeScreen.value).toBe(true)
        expect(isDesktop.value).toBe(true)
      })

      it('should detect small screen for width < 1366', () => {
        mockWindow.innerWidth = 1280
        const { isSmallScreen } = useResponsive()
        
        expect(isSmallScreen.value).toBe(true)
      })
    })

    describe('breakpoint utility functions', () => {
      it('isBreakpointUp should return true when width >= breakpoint', () => {
        mockWindow.innerWidth = 1440
        const { isBreakpointUp } = useResponsive()
        
        expect(isBreakpointUp('xs')).toBe(true)
        expect(isBreakpointUp('sm')).toBe(true)
        expect(isBreakpointUp('md')).toBe(true)
        expect(isBreakpointUp('lg')).toBe(false)
      })

      it('isBreakpointDown should return true when width < breakpoint', () => {
        mockWindow.innerWidth = 1440
        const { isBreakpointDown } = useResponsive()
        
        expect(isBreakpointDown('xs')).toBe(false)
        expect(isBreakpointDown('sm')).toBe(false)
        expect(isBreakpointDown('md')).toBe(false)
        expect(isBreakpointDown('lg')).toBe(true)
      })

      it('isBreakpointBetween should return true when width is in range', () => {
        mockWindow.innerWidth = 1400
        const { isBreakpointBetween } = useResponsive()
        
        expect(isBreakpointBetween('xs', 'md')).toBe(true)
        expect(isBreakpointBetween('sm', 'md')).toBe(true)
        expect(isBreakpointBetween('md', 'lg')).toBe(false)
        expect(isBreakpointBetween('xs', 'sm')).toBe(false)
      })

      it('isBreakpointBetween should warn for invalid range', () => {
        mockWindow.innerWidth = 1440
        const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})
        const { isBreakpointBetween } = useResponsive()
        
        const result = isBreakpointBetween('lg', 'xs')
        
        expect(result).toBe(false)
        expect(consoleSpy).toHaveBeenCalledWith(
          '[useResponsive] Invalid breakpoint range: lg > xs'
        )
        
        consoleSpy.mockRestore()
      })
    })

    describe('breakpoints constant', () => {
      it('should expose breakpoints configuration', () => {
        const { breakpoints } = useResponsive()
        
        expect(breakpoints).toEqual({
          xs: 1280,
          sm: 1366,
          md: 1440,
          lg: 1920
        })
      })
    })
  })

  describe('all four screen sizes', () => {
    const testCases: Array<{ width: number; expected: Breakpoint; description: string }> = [
      { width: 1280, expected: 'xs', description: '1280px (小屏幕)' },
      { width: 1366, expected: 'sm', description: '1366px (中小屏幕)' },
      { width: 1440, expected: 'md', description: '1440px (中等屏幕)' },
      { width: 1920, expected: 'lg', description: '1920px (大屏幕)' }
    ]

    testCases.forEach(({ width, expected, description }) => {
      it(`should correctly identify ${description}`, () => {
        mockWindow.innerWidth = width
        const { breakpoint } = useResponsive()
        
        expect(breakpoint.value).toBe(expected)
      })
    })
  })
})

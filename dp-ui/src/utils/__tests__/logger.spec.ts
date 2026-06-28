import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { logger } from '../logger'

describe('Logger', () => {
  // 使用 mockImplementation 来捕获所有调用
  let debugCalls: unknown[][] = []
  let infoCalls: unknown[][] = []
  let warnCalls: unknown[][] = []
  let errorCalls: unknown[][] = []
  let groupCalls: unknown[][] = []
  let groupEndCalls: number = 0
  let tableCalls: unknown[][] = []

  beforeEach(() => {
    debugCalls = []
    infoCalls = []
    warnCalls = []
    errorCalls = []
    groupCalls = []
    groupEndCalls = 0
    tableCalls = []

    vi.spyOn(console, 'debug').mockImplementation((...args) => { debugCalls.push(args) })
    vi.spyOn(console, 'info').mockImplementation((...args) => { infoCalls.push(args) })
    vi.spyOn(console, 'warn').mockImplementation((...args) => { warnCalls.push(args) })
    vi.spyOn(console, 'error').mockImplementation((...args) => { errorCalls.push(args) })
    vi.spyOn(console, 'group').mockImplementation((...args) => { groupCalls.push(args) })
    vi.spyOn(console, 'groupEnd').mockImplementation(() => { groupEndCalls++ })
    vi.spyOn(console, 'table').mockImplementation((...args) => { tableCalls.push(args) })
    
    logger.clearBuffer()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('should log debug messages', () => {
    logger.debug('test debug message')
    expect(debugCalls.length).toBeGreaterThan(0)
  })

  it('should log info messages', () => {
    logger.info('test info message')
    expect(infoCalls.length).toBeGreaterThan(0)
  })

  it('should log warn messages', () => {
    logger.warn('test warn message')
    expect(warnCalls.length).toBeGreaterThan(0)
  })

  it('should log error messages', () => {
    logger.error('test error message')
    expect(errorCalls.length).toBeGreaterThan(0)
  })

  it('should log error with Error object', () => {
    const error = new Error('test error')
    logger.error('error occurred', error)
    expect(errorCalls.length).toBeGreaterThan(0)
  })

  it('should store logs in buffer', () => {
    logger.info('buffered message')
    const buffer = logger.getBuffer()
    expect(buffer.length).toBeGreaterThan(0)
    expect(buffer[buffer.length - 1].message).toBe('buffered message')
  })

  it('should clear buffer', () => {
    logger.info('message 1')
    logger.info('message 2')
    logger.clearBuffer()
    expect(logger.getBuffer()).toHaveLength(0)
  })

  it('should support group logging', () => {
    logger.group('test group')
    logger.info('grouped message')
    logger.groupEnd()
    expect(groupCalls.length).toBeGreaterThan(0)
    expect(groupEndCalls).toBeGreaterThan(0)
  })

  it('should support table logging', () => {
    const data = [{ name: 'test', value: 1 }]
    logger.table(data)
    expect(tableCalls.length).toBeGreaterThan(0)
  })
})

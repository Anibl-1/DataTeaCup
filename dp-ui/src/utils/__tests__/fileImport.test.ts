import { describe, it, expect } from 'vitest'
import {
  validateFileForImport,
  getFileExtension,
  isExcelFile,
  isTextFile,
  ALLOWED_EXTENSIONS,
  MAX_FILE_SIZE
} from '../fileImport'

describe('fileImport', () => {
  describe('getFileExtension', () => {
    it('extracts .xlsx extension', () => {
      expect(getFileExtension('data.xlsx')).toBe('.xlsx')
    })

    it('extracts .xls extension', () => {
      expect(getFileExtension('report.xls')).toBe('.xls')
    })

    it('extracts .csv extension', () => {
      expect(getFileExtension('export.csv')).toBe('.csv')
    })

    it('extracts .txt extension', () => {
      expect(getFileExtension('log.txt')).toBe('.txt')
    })

    it('returns lowercase extension', () => {
      expect(getFileExtension('DATA.XLSX')).toBe('.xlsx')
    })

    it('returns empty string for no extension', () => {
      expect(getFileExtension('noext')).toBe('')
    })

    it('returns empty string for trailing dot', () => {
      expect(getFileExtension('file.')).toBe('')
    })
  })

  describe('validateFileForImport', () => {
    it('accepts valid .xlsx file', () => {
      const result = validateFileForImport('data.xlsx', 1024)
      expect(result.valid).toBe(true)
      expect(result.error).toBeUndefined()
    })

    it('accepts valid .xls file', () => {
      expect(validateFileForImport('data.xls', 5000).valid).toBe(true)
    })

    it('accepts valid .csv file', () => {
      expect(validateFileForImport('data.csv', 2048).valid).toBe(true)
    })

    it('accepts valid .txt file', () => {
      expect(validateFileForImport('data.txt', 100).valid).toBe(true)
    })

    it('accepts file at exactly 100MB', () => {
      expect(validateFileForImport('big.xlsx', MAX_FILE_SIZE).valid).toBe(true)
    })

    it('rejects file over 100MB', () => {
      const result = validateFileForImport('huge.xlsx', MAX_FILE_SIZE + 1)
      expect(result.valid).toBe(false)
      expect(result.error).toContain('100MB')
    })

    it('rejects empty file name', () => {
      const result = validateFileForImport('', 1024)
      expect(result.valid).toBe(false)
    })

    it('rejects unsupported extension', () => {
      const result = validateFileForImport('image.png', 1024)
      expect(result.valid).toBe(false)
      expect(result.error).toContain('不支持')
    })

    it('rejects zero-size file', () => {
      const result = validateFileForImport('data.xlsx', 0)
      expect(result.valid).toBe(false)
      expect(result.error).toContain('空')
    })

    it('rejects .pdf file', () => {
      expect(validateFileForImport('doc.pdf', 1024).valid).toBe(false)
    })

    it('handles case-insensitive extensions', () => {
      expect(validateFileForImport('DATA.XLSX', 1024).valid).toBe(true)
      expect(validateFileForImport('file.CSV', 1024).valid).toBe(true)
    })
  })

  describe('isExcelFile', () => {
    it('returns true for .xlsx', () => {
      expect(isExcelFile('data.xlsx')).toBe(true)
    })

    it('returns true for .xls', () => {
      expect(isExcelFile('data.xls')).toBe(true)
    })

    it('returns false for .csv', () => {
      expect(isExcelFile('data.csv')).toBe(false)
    })
  })

  describe('isTextFile', () => {
    it('returns true for .csv', () => {
      expect(isTextFile('data.csv')).toBe(true)
    })

    it('returns true for .txt', () => {
      expect(isTextFile('data.txt')).toBe(true)
    })

    it('returns false for .xlsx', () => {
      expect(isTextFile('data.xlsx')).toBe(false)
    })
  })

  describe('constants', () => {
    it('ALLOWED_EXTENSIONS contains all required types', () => {
      expect(ALLOWED_EXTENSIONS).toContain('.xlsx')
      expect(ALLOWED_EXTENSIONS).toContain('.xls')
      expect(ALLOWED_EXTENSIONS).toContain('.csv')
      expect(ALLOWED_EXTENSIONS).toContain('.txt')
    })

    it('MAX_FILE_SIZE is 100MB', () => {
      expect(MAX_FILE_SIZE).toBe(100 * 1024 * 1024)
    })
  })
})

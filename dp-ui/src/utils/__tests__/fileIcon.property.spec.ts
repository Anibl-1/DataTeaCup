import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { getFileIcon, type FileIconName } from '../fileIcon'

const VALID_ICONS: FileIconName[] = [
  'ImageOutline',
  'DocumentTextOutline',
  'FolderOpenOutline',
  'CodeSlashOutline',
  'VideocamOutline',
  'MusicalNotesOutline',
  'DocumentOutline',
]

/**
 * Property 2: 文件扩展名到图标的映射一致性
 *
 * **Validates: Requirements 3.3**
 *
 * For any file name string, getFileIcon always returns a valid icon name,
 * and the same extension always returns the same icon.
 */
describe('getFileIcon — Property Tests', () => {
  it('Property 2a: always returns a valid FileIconName for any string', () => {
    fc.assert(
      fc.property(fc.string({ minLength: 0, maxLength: 300 }), (fileName) => {
        const icon = getFileIcon(fileName)
        expect(VALID_ICONS).toContain(icon)
      }),
      { numRuns: 200 },
    )
  })

  it('Property 2b: same extension always maps to the same icon', () => {
    // Arbitrary for lowercase alphanumeric strings (used as extension and base names)
    const alphaNum = fc.string({ minLength: 1, maxLength: 10 })
      .map(s => s.replace(/[^a-z0-9]/g, 'a'))
      .filter(s => s.length > 0)

    fc.assert(
      fc.property(
        alphaNum, // extension
        alphaNum, // baseName1
        alphaNum, // baseName2
        (ext, baseName1, baseName2) => {
          const file1 = `${baseName1}.${ext}`
          const file2 = `${baseName2}.${ext}`
          expect(getFileIcon(file1)).toBe(getFileIcon(file2))
        },
      ),
      { numRuns: 200 },
    )
  })

  it('Property 2c: extension matching is case-insensitive', () => {
    const baseName = fc.string({ minLength: 1, maxLength: 10 })
      .map(s => s.replace(/[^a-z]/g, 'a'))
      .filter(s => s.length > 0)

    fc.assert(
      fc.property(
        fc.constantFrom('jpg', 'png', 'pdf', 'js', 'mp4', 'mp3', 'zip'),
        baseName,
        (ext, base) => {
          const lower = `${base}.${ext.toLowerCase()}`
          const upper = `${base}.${ext.toUpperCase()}`
          const mixed = `${base}.${ext.split('').map((c, i) => (i % 2 === 0 ? c.toUpperCase() : c.toLowerCase())).join('')}`
          const icon = getFileIcon(lower)
          expect(getFileIcon(upper)).toBe(icon)
          expect(getFileIcon(mixed)).toBe(icon)
        },
      ),
      { numRuns: 100 },
    )
  })
})

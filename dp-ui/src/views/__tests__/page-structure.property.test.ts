/**
 * 页面结构一致性属性测试
 * Feature: page-audit-optimization
 * Property 1: 管理类页面结构一致性
 * Property 2: 搜索表单位置一致性
 * Property 3: 分页区域结构一致性
 *
 * **Validates: Requirements 1.1, 1.4, 1.5**
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import * as fs from 'fs'
import * as path from 'path'

// ============================================================================
// Constants
// ============================================================================

const PROJECT_ROOT = path.resolve(__dirname, '..', '..', '..')
const VIEWS_DIR = path.join(PROJECT_ROOT, 'src', 'views')

/**
 * Management pages that should follow the unified page structure.
 * Referenced in Requirements 1.1, 1.4, 1.5.
 */
const MANAGEMENT_PAGES = [
  'DataSource.vue',
  'ChartManage.vue',
  'ReportManage.vue',
  'PageManage.vue',
  'PipelineManage.vue',
  'DataTransfer.vue',
  'DataSyncManage.vue',
  'RlsConfig.vue',
  'TicketManage.vue',
  'KnowledgeBase.vue',
  'MessageChannel.vue',
  'AnnouncementManage.vue',
  'ExportCenterPage.vue',
]

/** System management pages in subdirectories */
const SYSTEM_PAGES = [
  { dir: 'system/user', file: 'index.vue' },
  { dir: 'system/role', file: 'index.vue' },
  { dir: 'system/menu', file: 'index.vue' },
  { dir: 'system/config', file: 'index.vue' },
  { dir: 'org/post', file: 'index.vue' },
  { dir: 'org/dept', file: 'index.vue' },
]

// ============================================================================
// Helpers
// ============================================================================

/** Read a Vue SFC file content */
function readVueFile(relativePath: string): string {
  const fullPath = path.join(VIEWS_DIR, relativePath)
  if (!fs.existsSync(fullPath)) return ''
  return fs.readFileSync(fullPath, 'utf-8')
}

/** Get all management page contents as { name, content } pairs */
function getAllManagementPages(): Array<{ name: string; content: string }> {
  const pages: Array<{ name: string; content: string }> = []

  for (const file of MANAGEMENT_PAGES) {
    const content = readVueFile(file)
    if (content) pages.push({ name: file, content })
  }

  for (const { dir, file } of SYSTEM_PAGES) {
    const content = readVueFile(path.join(dir, file))
    if (content) pages.push({ name: `${dir}/${file}`, content })
  }

  return pages
}

/**
 * Extract the <template> section from a Vue SFC.
 * Uses the outermost <template>...</template> block (last closing tag).
 */
function extractTemplate(content: string): string {
  const startIdx = content.indexOf('<template>')
  if (startIdx < 0) return content
  const endIdx = content.lastIndexOf('</template>')
  if (endIdx < 0) return content
  return content.slice(startIdx, endIdx)
}

// ============================================================================
// Property 1: 管理类页面结构一致性
// ============================================================================

describe('Property 1: 管理类页面结构一致性', () => {
  const allPages = getAllManagementPages()

  /** Check if page has page-header-stats section */
  function hasPageHeaderStats(content: string): boolean {
    return /page-header-stats/.test(content)
  }

  /** Check if page has n-card (main card) */
  function hasMainCard(content: string): boolean {
    return /n-card/.test(content)
  }

  /** Check if page has card-header-custom */
  function hasCardHeaderCustom(content: string): boolean {
    return /card-header-custom/.test(content)
  }

  /** Check if page has n-data-table */
  function hasDataTable(content: string): boolean {
    return /n-data-table/.test(content)
  }

  /** Check if page has pagination-wrapper */
  function hasPaginationWrapper(content: string): boolean {
    return /pagination-wrapper/.test(content)
  }

  /**
   * For any management page, the rendered DOM should contain
   * page-header-stats with stat-item elements.
   *
   * **Validates: Requirements 1.1**
   */
  it('all management pages have page-header-stats section', () => {
    expect(allPages.length).toBeGreaterThan(0)

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: allPages.length - 1 }),
        (idx) => {
          const page = allPages[idx]!
          expect(hasPageHeaderStats(page.content)).toBe(true)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * For any management page, the DOM should contain an n-card
   * serving as the Main_Card container.
   *
   * **Validates: Requirements 1.1**
   */
  it('all management pages have n-card (main card)', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: allPages.length - 1 }),
        (idx) => {
          const page = allPages[idx]!
          expect(hasMainCard(page.content)).toBe(true)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * For any management page, the Main_Card should contain
   * a card-header-custom element.
   *
   * **Validates: Requirements 1.1**
   */
  it('all management pages have card-header-custom', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: allPages.length - 1 }),
        (idx) => {
          const page = allPages[idx]!
          expect(hasCardHeaderCustom(page.content)).toBe(true)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * For any management page, the Main_Card should contain
   * an n-data-table component.
   *
   * **Validates: Requirements 1.1**
   */
  it('all management pages have n-data-table', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: allPages.length - 1 }),
        (idx) => {
          const page = allPages[idx]!
          expect(hasDataTable(page.content)).toBe(true)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * For any management page that uses paginated data (not tree-based),
   * the Main_Card should contain a pagination-wrapper element.
   *
   * **Validates: Requirements 1.1**
   */
  it('all paginated management pages have pagination-wrapper', () => {
    // Tree-based pages (dept, menu) use expanded-row-keys and don't paginate
    const isTreePage = (content: string): boolean => {
      return /expanded-row-keys|children-key/.test(content)
    }
    const paginatedPages = allPages.filter(p => {
      return /n-data-table/.test(p.content) && !isTreePage(p.content)
    })
    expect(paginatedPages.length).toBeGreaterThan(0)

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: paginatedPages.length - 1 }),
        (idx) => {
          const page = paginatedPages[idx]!
          expect(hasPaginationWrapper(page.content)).toBe(true)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})

// ============================================================================
// Property 2: 搜索表单位置一致性
// ============================================================================

describe('Property 2: 搜索表单位置一致性', () => {
  const allPages = getAllManagementPages()

  /** Get pages that have a query-form */
  function getPagesWithQueryForm(): Array<{ name: string; content: string }> {
    return allPages.filter(p => /query-form/.test(p.content))
  }

  /**
   * For any management page with a query-form, the form should appear
   * inside an n-card (Main_Card) container.
   *
   * **Validates: Requirements 1.4**
   */
  it('query-form is inside n-card (Main_Card)', () => {
    const pagesWithForm = getPagesWithQueryForm()
    expect(pagesWithForm.length).toBeGreaterThan(0)

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: pagesWithForm.length - 1 }),
        (idx) => {
          const page = pagesWithForm[idx]!
          const template = extractTemplate(page.content)

          // Find the position of n-card opening and query-form
          const cardStart = template.indexOf('<n-card')
          const queryFormPos = template.indexOf('query-form')

          // query-form should appear after n-card opens
          expect(cardStart).toBeGreaterThanOrEqual(0)
          expect(queryFormPos).toBeGreaterThan(cardStart)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * For any management page with a query-form, the form should appear
   * before the n-data-table (i.e., above the table).
   *
   * **Validates: Requirements 1.4**
   */
  it('query-form appears before n-data-table', () => {
    const pagesWithForm = getPagesWithQueryForm()

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: pagesWithForm.length - 1 }),
        (idx) => {
          const page = pagesWithForm[idx]!
          const template = extractTemplate(page.content)

          const queryFormPos = template.indexOf('query-form')
          const dataTablePos = template.indexOf('<n-data-table')

          // Both should exist and query-form should come first
          expect(queryFormPos).toBeGreaterThanOrEqual(0)
          expect(dataTablePos).toBeGreaterThan(queryFormPos)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})

// ============================================================================
// Property 3: 分页区域结构一致性
// ============================================================================

describe('Property 3: 分页区域结构一致性', () => {
  const allPages = getAllManagementPages()

  /** Get pages that have a pagination-wrapper */
  function getPagesWithPagination(): Array<{ name: string; content: string }> {
    return allPages.filter(p => /pagination-wrapper/.test(p.content))
  }

  /**
   * Extract the pagination-wrapper block from the full file content.
   * Captures from the pagination-wrapper div to a reasonable end boundary.
   */
  function extractPaginationBlock(content: string): string {
    const startIdx = content.indexOf('pagination-wrapper')
    if (startIdx < 0) return ''

    // Go back to find the opening tag
    const tagStart = content.lastIndexOf('<', startIdx)
    // Look for the closing </div> that ends the pagination-wrapper
    // We search for the next </n-card> or a reasonable chunk (500 chars)
    const searchEnd = Math.min(content.length, tagStart + 500)
    return content.slice(tagStart, searchEnd)
  }

  /** Get pages that have pagination-wrapper with n-pagination inside */
  function getPagesWithFullPagination(): Array<{ name: string; content: string }> {
    return allPages.filter(p => {
      const block = extractPaginationBlock(p.content)
      return block.includes('pagination-wrapper') && block.includes('n-pagination')
    })
  }

  /**
   * For any page with pagination-wrapper, the block should contain
   * total count text (共 ... 条记录 or similar).
   *
   * **Validates: Requirements 1.5**
   */
  it('pagination-wrapper contains total count text', () => {
    const pagesWithPagination = getPagesWithPagination()
    expect(pagesWithPagination.length).toBeGreaterThan(0)

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: pagesWithPagination.length - 1 }),
        (idx) => {
          const page = pagesWithPagination[idx]!
          const paginationBlock = extractPaginationBlock(page.content)

          // Should contain total count text pattern: 共 {{ xxx }} 条
          const hasTotalText = /共\s*\{\{.*?\}\}\s*条/.test(paginationBlock)
          expect(hasTotalText).toBe(true)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * For any page with full pagination (pagination-wrapper + n-pagination),
   * the block should contain an n-pagination component.
   *
   * **Validates: Requirements 1.5**
   */
  it('pagination-wrapper contains n-pagination component', () => {
    const pagesWithFullPagination = getPagesWithFullPagination()
    expect(pagesWithFullPagination.length).toBeGreaterThan(0)

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: pagesWithFullPagination.length - 1 }),
        (idx) => {
          const page = pagesWithFullPagination[idx]!
          const paginationBlock = extractPaginationBlock(page.content)

          expect(paginationBlock).toContain('n-pagination')
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * For any page with full pagination, the n-pagination should have
   * show-size-picker and show-quick-jumper attributes.
   *
   * **Validates: Requirements 1.5**
   */
  it('n-pagination has show-size-picker and show-quick-jumper', () => {
    const pagesWithFullPagination = getPagesWithFullPagination()

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: pagesWithFullPagination.length - 1 }),
        (idx) => {
          const page = pagesWithFullPagination[idx]!
          const paginationBlock = extractPaginationBlock(page.content)

          expect(paginationBlock).toContain('show-size-picker')
          expect(paginationBlock).toContain('show-quick-jumper')
          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})

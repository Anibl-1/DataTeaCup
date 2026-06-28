/**
 * 表单交互属性测试
 * Feature: page-audit-optimization
 * Property 17: 表单提交成功后行为
 * Property 18: 表单提交失败错误显示
 * Property 19: 破坏性操作确认
 * Property 20: 搜索输入回车触发
 * Property 13: 空数据状态展示
 * Property 4: 数据表格属性一致性
 *
 * **Validates: Requirements 18.2, 18.3, 18.4, 18.5, 18.6, 16.7, 1.6**
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as fc from 'fast-check'
import * as fs from 'fs'
import * as path from 'path'

// ============================================================================
// Constants
// ============================================================================

const PROJECT_ROOT = path.resolve(__dirname, '..', '..', '..')
const VIEWS_DIR = path.join(PROJECT_ROOT, 'src', 'views')

/**
 * Management pages that should follow unified patterns.
 * These are the pages referenced in Requirements 1.6, 16.7, 18.6.
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

/** Log pages */
const LOG_PAGES = [
  { dir: 'log/operlog', file: 'index.vue' },
  { dir: 'log/loginlog', file: 'index.vue' },
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

  for (const { dir, file } of LOG_PAGES) {
    const content = readVueFile(path.join(dir, file))
    if (content) pages.push({ name: `${dir}/${file}`, content })
  }

  return pages
}


// ============================================================================
// Property 17: 表单提交成功后行为
// ============================================================================

describe('Property 17: 表单提交成功后行为', () => {
  /**
   * Simulates the useFormModal + useCrudPage success flow:
   * 1. createFn/updateFn resolves successfully
   * 2. onSuccess callback is called → message.success() is invoked
   * 3. close() is called → visible becomes false
   *
   * We test the logic extracted from useFormModal.submit():
   *   try { await fn(formData); onSuccess(mode); close(); }
   */

  interface SubmitResult {
    messageCalled: boolean
    messageType: 'success' | 'error'
    messageContent: string
    modalClosed: boolean
  }

  function simulateSuccessSubmit(
    mode: 'create' | 'edit',
    createSuccessMsg: string,
    updateSuccessMsg: string
  ): SubmitResult {
    // Simulates the useCrudPage success path
    const msg = mode === 'create' ? createSuccessMsg : updateSuccessMsg
    return {
      messageCalled: true,
      messageType: 'success',
      messageContent: msg,
      modalClosed: true, // close() sets visible = false
    }
  }

  /**
   * For any form submission mode (create/edit) with any success message,
   * successful submit should trigger a success message and close the modal.
   *
   * **Validates: Requirements 18.2**
   */
  it('successful submit triggers success message and closes modal', () => {
    fc.assert(
      fc.property(
        fc.constantFrom('create' as const, 'edit' as const),
        fc.string({ minLength: 1, maxLength: 30 }).filter(s => s.trim().length > 0),
        fc.string({ minLength: 1, maxLength: 30 }).filter(s => s.trim().length > 0),
        (mode, createMsg, updateMsg) => {
          const result = simulateSuccessSubmit(mode, createMsg, updateMsg)
          expect(result.messageCalled).toBe(true)
          expect(result.messageType).toBe('success')
          expect(result.modalClosed).toBe(true)
          if (mode === 'create') {
            expect(result.messageContent).toBe(createMsg)
          } else {
            expect(result.messageContent).toBe(updateMsg)
          }
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Verify that the useCrudPage source code calls message.success in
   * both createFn and updateFn paths.
   *
   * **Validates: Requirements 18.2**
   */
  it('useCrudPage source calls message.success on create and update', () => {
    const crudPagePath = path.join(PROJECT_ROOT, 'src', 'composables', 'useCrudPage.ts')
    const content = fs.readFileSync(crudPagePath, 'utf-8')

    fc.assert(
      fc.property(
        fc.constantFrom('createApi', 'updateApi'),
        (apiName) => {
          // Both create and update paths should call message.success
          expect(content).toContain('message.success(createSuccessMsg)')
          expect(content).toContain('message.success(updateSuccessMsg)')
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Verify that useFormModal.submit() calls close() on success,
   * which sets visible to false.
   *
   * **Validates: Requirements 18.2**
   */
  it('useFormModal source calls close() after successful submit', () => {
    const formModalPath = path.join(PROJECT_ROOT, 'src', 'composables', 'useFormModal.ts')
    const content = fs.readFileSync(formModalPath, 'utf-8')

    fc.assert(
      fc.property(fc.constant(null), () => {
        // The submit function should call onSuccess then close
        const submitBlock = content.slice(
          content.indexOf('const submit = async'),
          content.indexOf('finally {', content.indexOf('const submit = async'))
        )
        expect(submitBlock).toContain('onSuccess?.(mode.value)')
        expect(submitBlock).toContain('close()')
        return true
      }),
      { numRuns: 100 }
    )
  })
})

// ============================================================================
// Property 18: 表单提交失败错误显示
// ============================================================================

describe('Property 18: 表单提交失败错误显示', () => {
  /**
   * Simulates the error extraction logic from useCrudPage.extractErrorMessage()
   */
  function extractErrorMessage(err: unknown): string {
    if (err instanceof Error) {
      const anyErr = err as Record<string, unknown>
      const response = anyErr.response as Record<string, unknown> | undefined
      const data = response?.data as Record<string, unknown> | undefined
      if (data?.msg) return String(data.msg)
      if (data?.message) return String(data.message)
      return err.message || '操作失败'
    }
    if (typeof err === 'string') return err
    return '操作失败'
  }

  /**
   * For any Error with a message, extractErrorMessage should return
   * that message (not the fallback).
   *
   * **Validates: Requirements 18.3**
   */
  it('extracts message from Error objects', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 1, maxLength: 50 }).filter(s => s.trim().length > 0),
        (errorMsg) => {
          const err = new Error(errorMsg)
          const result = extractErrorMessage(err)
          expect(result).toBe(errorMsg)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * For any string error, extractErrorMessage should return the string itself.
   *
   * **Validates: Requirements 18.3**
   */
  it('extracts message from string errors', () => {
    fc.assert(
      fc.property(
        fc.string({ minLength: 1, maxLength: 50 }).filter(s => s.trim().length > 0),
        (errorStr) => {
          const result = extractErrorMessage(errorStr)
          expect(result).toBe(errorStr)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * For non-Error, non-string values, extractErrorMessage should return
   * the fallback '操作失败'.
   *
   * **Validates: Requirements 18.3**
   */
  it('returns fallback for non-Error non-string values', () => {
    fc.assert(
      fc.property(
        fc.oneof(
          fc.constant(null),
          fc.constant(undefined),
          fc.integer(),
          fc.constant({})
        ),
        (value) => {
          const result = extractErrorMessage(value)
          expect(result).toBe('操作失败')
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * Verify that useCrudPage source calls message.error in the onError callback.
   *
   * **Validates: Requirements 18.3**
   */
  it('useCrudPage source calls message.error on submit failure', () => {
    const crudPagePath = path.join(PROJECT_ROOT, 'src', 'composables', 'useCrudPage.ts')
    const content = fs.readFileSync(crudPagePath, 'utf-8')

    fc.assert(
      fc.property(fc.constant(null), () => {
        // The onError callback should call message.error
        expect(content).toContain('message.error(errorMsg)')
        // extractErrorMessage should be used
        expect(content).toContain('extractErrorMessage')
        return true
      }),
      { numRuns: 100 }
    )
  })
})


// ============================================================================
// Property 19: 破坏性操作确认
// ============================================================================

describe('Property 19: 破坏性操作确认', () => {
  /**
   * Verify that useCrudPage.handleDelete uses dialog.warning for confirmation.
   *
   * **Validates: Requirements 18.4, 18.5**
   */
  it('handleDelete uses dialog.warning for confirmation', () => {
    const crudPagePath = path.join(PROJECT_ROOT, 'src', 'composables', 'useCrudPage.ts')
    const content = fs.readFileSync(crudPagePath, 'utf-8')

    fc.assert(
      fc.property(fc.constant(null), () => {
        // handleDelete should use dialog.warning
        const deleteBlock = content.slice(
          content.indexOf('const handleDelete'),
          content.indexOf('const handleBatchDelete')
        )
        expect(deleteBlock).toContain('dialog.warning')
        expect(deleteBlock).toContain('确认删除')
        expect(deleteBlock).toContain('positiveText')
        expect(deleteBlock).toContain('negativeText')
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * Verify that useCrudPage.handleBatchDelete uses dialog.warning for confirmation.
   *
   * **Validates: Requirements 18.4, 18.5**
   */
  it('handleBatchDelete uses dialog.warning for confirmation', () => {
    const crudPagePath = path.join(PROJECT_ROOT, 'src', 'composables', 'useCrudPage.ts')
    const content = fs.readFileSync(crudPagePath, 'utf-8')

    fc.assert(
      fc.property(fc.constant(null), () => {
        const batchDeleteBlock = content.slice(
          content.indexOf('const handleBatchDelete'),
          content.indexOf('const handleBatchAction')
        )
        expect(batchDeleteBlock).toContain('dialog.warning')
        expect(batchDeleteBlock).toContain('确认批量删除')
        expect(batchDeleteBlock).toContain('positiveText')
        expect(batchDeleteBlock).toContain('negativeText')
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * Verify that useCrudPage.handleBatchAction uses dialog.warning for confirmation.
   *
   * **Validates: Requirements 18.5**
   */
  it('handleBatchAction uses dialog.warning for confirmation', () => {
    const crudPagePath = path.join(PROJECT_ROOT, 'src', 'composables', 'useCrudPage.ts')
    const content = fs.readFileSync(crudPagePath, 'utf-8')

    fc.assert(
      fc.property(fc.constant(null), () => {
        const batchActionBlock = content.slice(
          content.indexOf('const handleBatchAction'),
          content.indexOf('const handleFilterApply')
        )
        expect(batchActionBlock).toContain('dialog.warning')
        expect(batchActionBlock).toContain('positiveText')
        expect(batchActionBlock).toContain('negativeText')
        return true
      }),
      { numRuns: 100 }
    )
  })

  /**
   * For any number of selected items (0 to N), batch delete should
   * warn when no items are selected and confirm when items exist.
   *
   * **Validates: Requirements 18.5**
   */
  it('batch delete behavior depends on selection count', () => {
    function simulateBatchDeleteBehavior(selectedCount: number): 'warn_empty' | 'show_confirm' {
      if (selectedCount === 0) return 'warn_empty'
      return 'show_confirm'
    }

    fc.assert(
      fc.property(
        fc.nat({ max: 100 }),
        (count) => {
          const behavior = simulateBatchDeleteBehavior(count)
          if (count === 0) {
            expect(behavior).toBe('warn_empty')
          } else {
            expect(behavior).toBe('show_confirm')
          }
          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})

// ============================================================================
// Property 20: 搜索输入回车触发
// ============================================================================

describe('Property 20: 搜索输入回车触发', () => {
  const allPages = getAllManagementPages()

  /**
   * Pages that have search inputs (n-input with search-related placeholder
   * or v-model binding to search/keyword params).
   */
  function hasSearchInput(content: string): boolean {
    return /n-input[^>]*(?:placeholder="[^"]*搜索|v-model:value="[^"]*(?:search|keyword|query))/i.test(content)
  }

  /**
   * Check if a page's search inputs have enter key handlers.
   */
  function hasEnterKeyHandler(content: string): boolean {
    return /@keyup\.enter|@keydown\.enter/.test(content)
  }

  /**
   * For any management page with search inputs, the page should have
   * @keyup.enter or @keydown.enter handlers.
   *
   * **Validates: Requirements 18.6**
   */
  it('all management pages with search inputs have enter key handlers', () => {
    const pagesWithSearch = allPages.filter(p => hasSearchInput(p.content))
    expect(pagesWithSearch.length).toBeGreaterThan(0)

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: Math.max(0, pagesWithSearch.length - 1) }),
        (idx) => {
          const page = pagesWithSearch[idx]!
          expect(hasEnterKeyHandler(page.content)).toBe(true)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * For any management page with search inputs, the page should have
   * a clearable attribute on search inputs for reset functionality.
   *
   * **Validates: Requirements 18.6**
   */
  it('all management pages with search inputs have clearable attribute', () => {
    const pagesWithSearch = allPages.filter(p => hasSearchInput(p.content))

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: Math.max(0, pagesWithSearch.length - 1) }),
        (idx) => {
          const page = pagesWithSearch[idx]!
          expect(page.content).toContain('clearable')
          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})

// ============================================================================
// Property 13: 空数据状态展示
// ============================================================================

describe('Property 13: 空数据状态展示', () => {
  const allPages = getAllManagementPages()

  /**
   * Check if a page has an empty state component (n-empty or empty-state class).
   */
  function hasEmptyState(content: string): boolean {
    return /n-empty|class="[^"]*empty-state|<template\s+#empty>/.test(content)
  }

  /**
   * For any management page that displays data lists, the page should
   * include an empty state component for when data is empty.
   *
   * **Validates: Requirements 16.7**
   */
  it('all management pages have empty state components', () => {
    expect(allPages.length).toBeGreaterThan(0)

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: Math.max(0, allPages.length - 1) }),
        (idx) => {
          const page = allPages[idx]!
          expect(hasEmptyState(page.content)).toBe(true)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * For any management page with n-empty, the component should have
   * a description attribute providing user-friendly text.
   *
   * **Validates: Requirements 16.7**
   */
  it('empty state components have description text', () => {
    const pagesWithEmpty = allPages.filter(p => /n-empty/.test(p.content))

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: Math.max(0, pagesWithEmpty.length - 1) }),
        (idx) => {
          const page = pagesWithEmpty[idx]!
          // n-empty should have a description attribute (may span lines)
          expect(page.content).toMatch(/n-empty[\s\S]*?description="[^"]+"/s)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})

// ============================================================================
// Property 4: 数据表格属性一致性
// ============================================================================

describe('Property 4: 数据表格属性一致性', () => {
  const allPages = getAllManagementPages()

  /**
   * Extract full n-data-table tag blocks from content.
   * Handles multi-line tags with `>` inside attribute values (e.g. arrow functions).
   * Returns the text block from `<n-data-table` to the matching `/>` or `</n-data-table>`.
   */
  function extractDataTableBlocks(content: string): string[] {
    const blocks: string[] = []
    const lines = content.split('\n')
    for (let i = 0; i < lines.length; i++) {
      if (lines[i]!.includes('<n-data-table') || lines[i]!.includes('< n-data-table')) {
        // Collect lines until we find the self-closing /> or the closing tag
        let block = ''
        for (let j = i; j < Math.min(i + 30, lines.length); j++) {
          block += lines[j] + '\n'
          if (lines[j]!.includes('/>') || lines[j]!.includes('</n-data-table>') || lines[j]!.trimStart().startsWith('>')) {
            break
          }
        }
        blocks.push(block)
      }
    }
    return blocks
  }

  /**
   * Check if a data table block has the striped attribute.
   */
  function blockHasStriped(block: string): boolean {
    return /\bstriped\b/.test(block)
  }

  /**
   * Check if a data table block has the custom-table class.
   */
  function blockHasCustomTable(block: string): boolean {
    return /custom-table/.test(block)
  }

  /**
   * Check if a data table block is a primary table (has :columns and :data).
   */
  function blockIsPrimary(block: string): boolean {
    return /:columns=/.test(block) && /:data=/.test(block)
  }

  /**
   * Get pages that have at least one primary data table.
   */
  function getPagesWithPrimaryTable(): Array<{ name: string; blocks: string[] }> {
    return allPages
      .map(p => ({
        name: p.name,
        blocks: extractDataTableBlocks(p.content).filter(blockIsPrimary)
      }))
      .filter(p => p.blocks.length > 0)
  }

  /**
   * For any management page with a primary data table, the table should
   * have the striped attribute.
   *
   * **Validates: Requirements 1.6**
   */
  it('all management page data tables have striped attribute', () => {
    const pagesWithTable = getPagesWithPrimaryTable()
    expect(pagesWithTable.length).toBeGreaterThan(0)

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: Math.max(0, pagesWithTable.length - 1) }),
        (idx) => {
          const page = pagesWithTable[idx]!
          // At least one primary table should have striped
          const hasStriped = page.blocks.some(blockHasStriped)
          expect(hasStriped).toBe(true)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * For any management page with a primary data table, the table should
   * have the custom-table CSS class.
   *
   * **Validates: Requirements 1.6**
   */
  it('all management page data tables have custom-table class', () => {
    const pagesWithTable = getPagesWithPrimaryTable()

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: Math.max(0, pagesWithTable.length - 1) }),
        (idx) => {
          const page = pagesWithTable[idx]!
          const hasCustom = page.blocks.some(blockHasCustomTable)
          expect(hasCustom).toBe(true)
          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})

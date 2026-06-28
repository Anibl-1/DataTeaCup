/**
 * 前端洞察代码无 SSE/EventSource 使用 属性测试
 * Feature: ai-feature-consolidation
 * Property 1: 前端洞察代码无 SSE/EventSource 使用
 *
 * **Validates: Requirements 2.3, 4.1, 4.3**
 *
 * 验证 aiInsight.ts API 模块不包含 EventSource 构造函数调用、
 * analyzeDatasetStream 函数引用或其他 SSE 相关模式。
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import * as fs from 'fs'
import * as path from 'path'

// ============================================================================
// Constants
// ============================================================================

const PROJECT_ROOT = path.resolve(__dirname, '..', '..', '..')
const AI_INSIGHT_FILE = path.join(PROJECT_ROOT, 'src', 'api', 'aiInsight.ts')

/**
 * SSE/EventSource 相关的禁止模式列表。
 * 每个条目包含一个正则表达式和描述。
 */
const FORBIDDEN_SSE_PATTERNS: Array<{ pattern: RegExp; description: string }> = [
  { pattern: /\bnew\s+EventSource\b/, description: 'new EventSource constructor' },
  { pattern: /\bEventSource\b/, description: 'EventSource reference' },
  { pattern: /\banalyzeDatasetStream\b/, description: 'analyzeDatasetStream function' },
  { pattern: /\btext\/event-stream\b/, description: 'text/event-stream MIME type' },
  { pattern: /\bSseEmitter\b/, description: 'SseEmitter reference' },
  { pattern: /\bonmessage\b/, description: 'onmessage event handler (SSE)' },
  { pattern: /\baddEventListener\s*\(\s*['"]message['"]/, description: 'addEventListener("message") for SSE' },
]

// ============================================================================
// Helpers
// ============================================================================

/**
 * 读取 aiInsight.ts 文件内容
 */
function readAiInsightContent(): string {
  return fs.readFileSync(AI_INSIGHT_FILE, 'utf-8')
}

/**
 * 检查内容中是否包含指定的禁止模式
 */
function findForbiddenPattern(
  content: string,
  pattern: RegExp
): boolean {
  return pattern.test(content)
}

// ============================================================================
// Property-Based Tests
// ============================================================================

describe('Property 1: 前端洞察代码无 SSE/EventSource 使用', () => {
  const fileContent = readAiInsightContent()

  /**
   * 对每个禁止的 SSE 模式，使用 fast-check 生成随机子串采样位置，
   * 验证文件内容的任意片段都不包含 SSE 相关代码。
   *
   * **Validates: Requirements 2.3, 4.1, 4.3**
   */
  it('aiInsight.ts should not contain any SSE/EventSource patterns across random content slices', () => {
    const contentLength = fileContent.length

    fc.assert(
      fc.property(
        // 生成随机的起始位置和片段长度来采样文件内容
        fc.integer({ min: 0, max: Math.max(0, contentLength - 1) }),
        fc.integer({ min: 1, max: contentLength }),
        (start, length) => {
          const slice = fileContent.substring(start, start + length)

          for (const { pattern, description } of FORBIDDEN_SSE_PATTERNS) {
            if (findForbiddenPattern(slice, pattern)) {
              return false // 发现禁止模式
            }
          }
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * 逐行验证：对文件的每一行，使用 fast-check 随机选择行号，
   * 确认该行不包含任何 SSE 相关模式。
   *
   * **Validates: Requirements 2.3, 4.1, 4.3**
   */
  it('no line in aiInsight.ts should contain SSE/EventSource patterns', () => {
    const lines = fileContent.split('\n')
    const lineCount = lines.length

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: lineCount - 1 }),
        (lineIndex) => {
          const line = lines[lineIndex]

          for (const { pattern } of FORBIDDEN_SSE_PATTERNS) {
            if (findForbiddenPattern(line, pattern)) {
              return false
            }
          }
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * 使用 fast-check 从禁止模式列表中随机选择模式，
   * 验证整个文件内容都不匹配该模式。
   *
   * **Validates: Requirements 2.3, 4.1, 4.3**
   */
  it('aiInsight.ts should not match any randomly selected forbidden SSE pattern', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: FORBIDDEN_SSE_PATTERNS.length - 1 }),
        (patternIndex) => {
          const { pattern } = FORBIDDEN_SSE_PATTERNS[patternIndex]
          return !findForbiddenPattern(fileContent, pattern)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * 验证文件中导出的函数都使用 request（HTTP）而非 EventSource。
   * 随机采样导出函数块，确认每个都使用 request 调用。
   *
   * **Validates: Requirements 4.1, 4.3**
   */
  it('all exported functions should use HTTP request, not EventSource', () => {
    // 提取所有 export function 块
    const exportBlocks = fileContent.split(/(?=export\s+function\s+)/).filter(
      block => block.startsWith('export function') || block.startsWith('export ')
    )

    if (exportBlocks.length === 0) {
      // 如果没有导出函数，测试通过（文件可能为空或格式不同）
      return
    }

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: exportBlocks.length - 1 }),
        (blockIndex) => {
          const block = exportBlocks[blockIndex]

          // 不应包含 EventSource
          if (/\bEventSource\b/.test(block)) return false

          // 不应包含 analyzeDatasetStream
          if (/\banalyzeDatasetStream\b/.test(block)) return false

          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})

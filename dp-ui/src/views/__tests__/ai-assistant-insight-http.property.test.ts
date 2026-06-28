/**
 * 数据洞察 Tab 使用 HTTP 请求而非 SSE 属性测试
 * Feature: ai-feature-consolidation
 * Property 1: 前端洞察代码无 SSE/EventSource 使用
 *
 * **Validates: Requirements 4.1**
 *
 * 验证 AiAssistant.vue 中数据洞察 Tab 相关代码不包含 EventSource、
 * analyzeDatasetStream、text/event-stream、SseEmitter、onmessage 等 SSE 模式，
 * 且使用 generateInsightReport、detectAnomalies、analyzeTrend HTTP 函数。
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import * as fs from 'fs'
import * as path from 'path'

// ============================================================================
// Constants
// ============================================================================

const PROJECT_ROOT = path.resolve(__dirname, '..', '..', '..')
const AI_ASSISTANT_FILE = path.join(PROJECT_ROOT, 'src', 'views', 'AiAssistant.vue')

/** SSE/EventSource 相关的禁止模式 */
const FORBIDDEN_SSE_PATTERNS: Array<{ pattern: RegExp; description: string }> = [
  { pattern: /\bnew\s+EventSource\b/, description: 'new EventSource constructor' },
  { pattern: /\bEventSource\b/, description: 'EventSource reference' },
  { pattern: /\banalyzeDatasetStream\b/, description: 'analyzeDatasetStream function' },
  { pattern: /\btext\/event-stream\b/, description: 'text/event-stream MIME type' },
  { pattern: /\bSseEmitter\b/, description: 'SseEmitter reference' },
  { pattern: /\bonmessage\b/, description: 'onmessage event handler (SSE)' },
]

/** 期望存在的 HTTP 函数 */
const EXPECTED_HTTP_FUNCTIONS = [
  'generateInsightReport',
  'detectAnomalies',
  'analyzeTrend',
]

// ============================================================================
// Helpers
// ============================================================================

/**
 * 提取 AiAssistant.vue 中数据洞察相关的代码段。
 * 包括 insight Tab 模板区域和脚本中的洞察逻辑。
 */
function extractInsightSections(content: string): string[] {
  const sections: string[] = []

  // 提取 insight tab-pane 模板区域
  const insightTabMatch = content.match(/<n-tab-pane\s+name="insight"[\s\S]*?<\/n-tab-pane>/)
  if (insightTabMatch) {
    sections.push(insightTabMatch[0])
  }

  // 提取脚本中 "数据洞察 Tab" 区域
  const insightScriptMatch = content.match(/\/\/\s*=+\s*数据洞察[\s\S]*?(?=\/\/\s*=+|<\/script>)/)
  if (insightScriptMatch) {
    sections.push(insightScriptMatch[0])
  }

  // 提取 import 行中包含 aiInsight 的部分
  const importLines = content.split('\n').filter(line => /aiInsight/.test(line))
  if (importLines.length > 0) {
    sections.push(importLines.join('\n'))
  }

  return sections
}

// ============================================================================
// Property-Based Tests
// ============================================================================

describe('Property 1: AiAssistant.vue 数据洞察 Tab 无 SSE/EventSource 使用', () => {
  const fileContent = fs.readFileSync(AI_ASSISTANT_FILE, 'utf-8')
  const insightSections = extractInsightSections(fileContent)
  const combinedInsightCode = insightSections.join('\n')

  /**
   * 对每个禁止的 SSE 模式，使用 fast-check 随机选择模式和洞察代码段，
   * 验证洞察相关代码不包含任何 SSE 模式。
   *
   * **Validates: Requirements 4.1**
   */
  it('insight sections should not contain any SSE/EventSource patterns', () => {
    expect(insightSections.length).toBeGreaterThan(0)

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: FORBIDDEN_SSE_PATTERNS.length - 1 }),
        fc.integer({ min: 0, max: insightSections.length - 1 }),
        (patternIdx, sectionIdx) => {
          const { pattern, description } = FORBIDDEN_SSE_PATTERNS[patternIdx]
          const section = insightSections[sectionIdx]
          const found = pattern.test(section)
          if (found) {
            throw new Error(`Found forbidden SSE pattern "${description}" in insight section ${sectionIdx}`)
          }
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * 逐行验证：随机选择洞察代码中的行，确认不包含 SSE 模式。
   *
   * **Validates: Requirements 4.1**
   */
  it('no line in insight code should contain SSE patterns', () => {
    const lines = combinedInsightCode.split('\n')
    expect(lines.length).toBeGreaterThan(0)

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: lines.length - 1 }),
        fc.integer({ min: 0, max: FORBIDDEN_SSE_PATTERNS.length - 1 }),
        (lineIdx, patternIdx) => {
          const line = lines[lineIdx]
          const { pattern } = FORBIDDEN_SSE_PATTERNS[patternIdx]
          return !pattern.test(line)
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * 验证洞察代码使用 HTTP 函数（generateInsightReport、detectAnomalies、analyzeTrend）。
   * 随机选择一个期望的 HTTP 函数名，确认它出现在洞察代码中。
   *
   * **Validates: Requirements 4.1**
   */
  it('insight code should reference expected HTTP functions', () => {
    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: EXPECTED_HTTP_FUNCTIONS.length - 1 }),
        (funcIdx) => {
          const funcName = EXPECTED_HTTP_FUNCTIONS[funcIdx]
          const found = combinedInsightCode.includes(funcName)
          if (!found) {
            throw new Error(`Expected HTTP function "${funcName}" not found in insight code`)
          }
          return true
        }
      ),
      { numRuns: 100 }
    )
  })

  /**
   * 随机采样洞察代码片段，验证片段中不包含 SSE 模式。
   *
   * **Validates: Requirements 4.1**
   */
  it('random slices of insight code should be free of SSE patterns', () => {
    const codeLength = combinedInsightCode.length
    expect(codeLength).toBeGreaterThan(0)

    fc.assert(
      fc.property(
        fc.integer({ min: 0, max: Math.max(0, codeLength - 1) }),
        fc.integer({ min: 1, max: Math.min(500, codeLength) }),
        (start, length) => {
          const slice = combinedInsightCode.substring(start, start + length)
          for (const { pattern } of FORBIDDEN_SSE_PATTERNS) {
            if (pattern.test(slice)) return false
          }
          return true
        }
      ),
      { numRuns: 100 }
    )
  })
})

<template>
  <div class="sql-editor-wrap" :class="{ readonly: readOnly }">
    <Codemirror
      v-model="code"
      :style="{ height: height }"
      :autofocus="!readOnly"
      :indent-with-tab="true"
      :tab-size="2"
      :extensions="extensions"
      :disabled="readOnly"
      @ready="handleReady"
      @keydown="handleKeydown"
    />
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, watch, shallowRef, onMounted, onUnmounted } from 'vue'
import { Codemirror } from 'vue-codemirror'
import { sql, MySQL } from '@codemirror/lang-sql'
import { syntaxHighlighting, HighlightStyle } from '@codemirror/language'
import { tags } from '@lezer/highlight'
import { EditorView, keymap, highlightActiveLine, lineNumbers } from '@codemirror/view'
import { autocompletion, CompletionContext, type Completion } from '@codemirror/autocomplete'
import type { ViewUpdate } from '@codemirror/view'
import { getCompletions, type CompletionItem } from '@/api/sqlAutoComplete'

const props = withDefaults(defineProps<{
  modelValue?: string
  height?: string
  readOnly?: boolean
  placeholder?: string
  tables?: Record<string, string[]>
  dataSourceId?: number  // 数据源ID，用于获取表名和字段名补全
  enableAutoComplete?: boolean  // 是否启用自动补全
}>(), {
  modelValue: '',
  height: '300px',
  readOnly: false,
  placeholder: '-- 请输入 SQL 语句',
  tables: () => ({}),
  dataSourceId: undefined,
  enableAutoComplete: true
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'execute'): void
  (e: 'executeSelected', selectedText: string): void
}>()

const code = computed({
  get: () => props.modelValue,
  set: (val: string) => emit('update:modelValue', val)
})

const editorView = shallowRef<EditorView | null>(null)

const lightTheme = EditorView.theme({
  '&': {
    backgroundColor: '#ffffff',
    color: '#1e293b',
    fontSize: '14px',
    fontFamily: "'Consolas', 'Monaco', 'Courier New', monospace",
  },
  '.cm-content': {
    caretColor: '#2563eb',
    padding: '8px 0',
  },
  '.cm-cursor': {
    borderLeftColor: '#2563eb',
    borderLeftWidth: '2px',
  },
  '.cm-activeLine': {
    backgroundColor: '#f8fafc',
  },
  '.cm-gutters': {
    backgroundColor: '#f9fafb',
    color: '#94a3b8',
    borderRight: '1px solid #e5e7eb',
    minWidth: '40px',
  },
  '.cm-activeLineGutter': {
    backgroundColor: '#f1f5f9',
    color: '#475569',
  },
  '.cm-lineNumbers .cm-gutterElement': {
    padding: '0 8px 0 4px',
  },
  '.cm-matchingBracket': {
    backgroundColor: '#e0f2fe',
    color: '#0369a1 !important',
    outline: '1px solid #7dd3fc',
  },
  '.cm-tooltip': {
    backgroundColor: '#ffffff',
    border: '1px solid #e5e7eb',
    boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
  },
  '.cm-tooltip-autocomplete': {
    '& > ul > li[aria-selected]': {
      backgroundColor: '#eff6ff',
      color: '#1e40af',
    }
  },
  '.cm-foldPlaceholder': {
    backgroundColor: '#f1f5f9',
    color: '#64748b',
    border: '1px solid #e2e8f0',
  },
})

// SQL 浅色语法高亮
const lightHighlight = syntaxHighlighting(HighlightStyle.define([
  { tag: tags.keyword, color: '#7c3aed', fontWeight: '600' },          // SELECT, FROM 等
  { tag: tags.operatorKeyword, color: '#7c3aed', fontWeight: '600' },  // AND, OR, NOT
  { tag: tags.definitionKeyword, color: '#7c3aed', fontWeight: '600' },
  { tag: tags.typeName, color: '#0891b2' },                            // INT, VARCHAR 等
  { tag: tags.string, color: '#16a34a' },                              // 字符串
  { tag: tags.number, color: '#ea580c' },                              // 数字
  { tag: tags.comment, color: '#94a3b8', fontStyle: 'italic' },        // 注释
  { tag: tags.lineComment, color: '#94a3b8', fontStyle: 'italic' },
  { tag: tags.blockComment, color: '#94a3b8', fontStyle: 'italic' },
  { tag: tags.operator, color: '#dc2626' },                            // =, <, > 等
  { tag: tags.punctuation, color: '#64748b' },                         // 逗号、括号
  { tag: tags.bracket, color: '#64748b' },
  { tag: tags.function(tags.variableName), color: '#2563eb' },         // 函数名
  { tag: tags.variableName, color: '#1e293b' },                        // 列名/表名
  { tag: tags.bool, color: '#ea580c' },                                // TRUE, FALSE
  { tag: tags.null, color: '#ea580c', fontStyle: 'italic' },           // NULL
  { tag: tags.special(tags.string), color: '#0d9488' },                // 特殊字符串
]))

// 快捷键绑定
const customKeymap = keymap.of([
  {
    key: 'Ctrl-Enter',
    run: () => {
      emit('execute')
      return true
    }
  },
  {
    key: 'F9',
    run: (view) => {
      const sel = view.state.sliceDoc(
        view.state.selection.main.from,
        view.state.selection.main.to
      )
      if (sel.trim()) {
        emit('executeSelected', sel)
      } else {
        // 执行光标所在语句
        const pos = view.state.selection.main.head
        const text = view.state.doc.toString()
        let start = pos
        let end = pos
        while (start > 0 && text[start - 1] !== ';') start--
        while (end < text.length && text[end] !== ';') end++
        const stmt = text.substring(start, end).trim()
        if (stmt) emit('executeSelected', stmt)
      }
      return true
    }
  }
])

// 缓存补全结果，避免频繁请求
const completionCache = ref<Map<string, CompletionItem[]>>(new Map())
const cacheTimeout = 30000 // 30秒缓存

// SQL 关键字列表，用于后端不可用时的本地回退补全
const SQL_KEYWORDS = [
  'SELECT', 'FROM', 'WHERE', 'AND', 'OR', 'NOT', 'IN', 'LIKE', 'BETWEEN',
  'INSERT', 'INTO', 'VALUES', 'UPDATE', 'SET', 'DELETE',
  'CREATE', 'ALTER', 'DROP', 'TABLE', 'INDEX', 'VIEW',
  'JOIN', 'LEFT', 'RIGHT', 'INNER', 'OUTER', 'CROSS', 'ON',
  'GROUP', 'BY', 'ORDER', 'ASC', 'DESC', 'HAVING', 'LIMIT', 'OFFSET',
  'AS', 'DISTINCT', 'ALL', 'EXISTS', 'CASE', 'WHEN', 'THEN', 'ELSE', 'END',
  'NULL', 'IS', 'TRUE', 'FALSE', 'COUNT', 'SUM', 'AVG', 'MIN', 'MAX',
  'UNION', 'EXCEPT', 'INTERSECT', 'PRIMARY', 'KEY', 'FOREIGN', 'REFERENCES',
  'IF', 'SHOW', 'DESCRIBE', 'USE', 'DATABASE', 'DATABASES', 'TABLES', 'COLUMNS',
  'VARCHAR', 'INT', 'BIGINT', 'TEXT', 'DATE', 'DATETIME', 'TIMESTAMP', 'DECIMAL', 'FLOAT', 'DOUBLE', 'BOOLEAN',
  'DEFAULT', 'AUTO_INCREMENT', 'COMMENT', 'ENGINE', 'CHARSET', 'COLLATE',
  'TRUNCATE', 'REPLACE', 'EXPLAIN', 'CALL', 'PROCEDURE', 'FUNCTION'
]

/** 本地回退补全：关键字 + 传入的 tables schema */
function localFallbackComplete(prefix: string, from: number): { from: number; options: Completion[] } | null {
  const lowerPrefix = prefix.toLowerCase()
  const options: Completion[] = []

  // 关键字补全
  for (const kw of SQL_KEYWORDS) {
    if (kw.toLowerCase().startsWith(lowerPrefix)) {
      options.push({ label: kw, type: 'keyword', boost: 10 })
    }
  }

  // 表名和字段补全（来自 props.tables）
  for (const [tableName, columns] of Object.entries(props.tables)) {
    if (tableName.toLowerCase().startsWith(lowerPrefix)) {
      options.push({ label: tableName, type: 'class', detail: 'table', boost: 5 })
    }
    // 如果前缀包含 "表名."，补全字段
    if (lowerPrefix.includes('.')) {
      const [tbl, colPrefix] = lowerPrefix.split('.')
      if (tbl === tableName.toLowerCase()) {
        for (const col of columns) {
          if (!colPrefix || col.toLowerCase().startsWith(colPrefix)) {
            options.push({ label: col, type: 'property', detail: tableName, boost: 8 })
          }
        }
      }
    } else {
      // 也补全字段名（不带表名前缀时）
      for (const col of columns) {
        if (col.toLowerCase().startsWith(lowerPrefix)) {
          options.push({ label: col, type: 'property', detail: tableName, boost: 3 })
        }
      }
    }
  }

  return options.length > 0 ? { from, options } : null
}

// 自定义自动补全函数
async function sqlAutoComplete(context: CompletionContext): Promise<{ from: number; options: Completion[] } | null> {
  // 获取当前输入的单词
  const word = context.matchBefore(/[\w.]+/)
  if (!word && !context.explicit) return null
  
  const prefix = word?.text || ''
  const from = word?.from ?? context.pos
  
  // 获取上下文（当前行及前几行）
  const doc = context.state.doc
  const pos = context.pos
  const lineStart = doc.lineAt(pos).from
  const contextStart = Math.max(0, lineStart - 500)
  const sqlContext = doc.sliceString(contextStart, pos)
  
  // 生成缓存键
  const cacheKey = `${props.dataSourceId || 'none'}-${prefix.toLowerCase()}`
  
  // 检查缓存
  let completions = completionCache.value.get(cacheKey)
  
  if (!completions) {
    try {
      const response = await getCompletions(props.dataSourceId, prefix, sqlContext)
      if (response.data?.code === 200 && response.data?.data) {
        completions = response.data.data
        completionCache.value.set(cacheKey, completions)
        // 设置缓存过期
        setTimeout(() => {
          completionCache.value.delete(cacheKey)
        }, cacheTimeout)
      }
    } catch (error) {
      console.warn('获取自动补全建议失败，使用本地补全:', error)
      return localFallbackComplete(prefix, from)
    }
  }
  
  if (!completions || completions.length === 0) {
    return localFallbackComplete(prefix, from)
  }
  
  // 转换为 CodeMirror 补全格式
  const options: Completion[] = completions.map(item => ({
    label: item.label,
    type: mapCompletionType(item.type),
    detail: item.detail,
    info: item.info ? `${item.info}${item.documentation ? ' - ' + item.documentation : ''}` : item.documentation,
    boost: getBoost(item.type, prefix, item.label)
  }))
  
  return { from, options }
}

// 映射补全类型到 CodeMirror 类型
function mapCompletionType(type: string): string {
  switch (type) {
    case 'keyword': return 'keyword'
    case 'table': return 'class'
    case 'column': return 'property'
    default: return 'text'
  }
}

// 计算补全项的优先级
function getBoost(type: string, prefix: string, label: string): number {
  let boost = 0
  const lowerPrefix = prefix.toLowerCase()
  const lowerLabel = label.toLowerCase()
  
  // 完全匹配优先
  if (lowerLabel === lowerPrefix) boost += 100
  // 前缀匹配次之
  else if (lowerLabel.startsWith(lowerPrefix)) boost += 50
  
  // 类型优先级：关键字 > 表名 > 字段名
  switch (type) {
    case 'keyword': boost += 10; break
    case 'table': boost += 5; break
    case 'column': boost += 0; break
  }
  
  return boost
}

// 自动补全扩展
const autoCompleteExtension = computed(() => {
  if (!props.enableAutoComplete) return []
  
  return [
    autocompletion({
      override: [sqlAutoComplete],
      activateOnTyping: true,
      maxRenderedOptions: 50,
      defaultKeymap: true,
      icons: true
    })
  ]
})

const extensions = computed(() => {
  const exts: any[] = [
    sql({ dialect: MySQL, upperCaseKeywords: true, schema: props.tables }),
    lightTheme,
    lightHighlight,
    customKeymap,
    highlightActiveLine(),
    lineNumbers(),
    EditorView.lineWrapping,
    ...autoCompleteExtension.value
  ]
  if (props.readOnly) {
    exts.push(EditorView.editable.of(false))
  }
  return exts
})

const handleReady = ({ view }: { view: EditorView }) => {
  editorView.value = view
}

const handleKeydown = (e: KeyboardEvent) => {
  // 保留额外的键盘事件给父组件处理
}

/** 获取选中文本 */
const getSelectedText = (): string => {
  if (!editorView.value) return ''
  const state = editorView.value.state
  return state.sliceDoc(state.selection.main.from, state.selection.main.to)
}

/** 插入文本到光标处 */
const insertText = (text: string) => {
  if (!editorView.value) return
  const pos = editorView.value.state.selection.main.head
  editorView.value.dispatch({
    changes: { from: pos, insert: text }
  })
}

/** 清除补全缓存 */
const clearCompletionCache = () => {
  completionCache.value.clear()
}

// 当数据源ID变化时清除缓存
watch(() => props.dataSourceId, () => {
  clearCompletionCache()
})

// 组件卸载时清理
onUnmounted(() => {
  clearCompletionCache()
})

defineExpose({ getSelectedText, insertText, editorView, clearCompletionCache })
</script>

<style scoped>
.sql-editor-wrap {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}
.sql-editor-wrap.readonly {
  border-color: #e5e7eb;
  background: #fafbfc;
}
.sql-editor-wrap :deep(.cm-editor) {
  border-radius: 6px;
}
.sql-editor-wrap :deep(.cm-scroller) {
  overflow: auto;
}
.sql-editor-wrap :deep(.cm-focused) {
  outline: none;
}
.sql-editor-wrap :deep(.cm-content ::selection) {
  background-color: #3390ff !important;
  color: #fff !important;
}
.sql-editor-wrap :deep(.cm-content:focus ::selection) {
  background-color: #3390ff !important;
  color: #fff !important;
}
.sql-editor-wrap :deep(.cm-gutters ::selection) {
  background: transparent !important;
}
</style>

<template>
  <WidgetWrapper
    title="文本"
    :selected="selected"
    :loading="false"
    :error="null"
    :readonly="readonly"
    :show-header="showHeader"
    :show-refresh="false"
    @click="$emit('select')"
    @remove="$emit('remove')"
  >
    <div class="text-container" :style="containerStyle">
      <!-- Markdown rendered content -->
      <div
        v-if="isMarkdown"
        class="text-content markdown-content"
        :style="textStyle"
        v-html="renderedContent"
      ></div>
      
      <!-- Plain text / Rich text content -->
      <div
        v-else
        class="text-content"
        :style="textStyle"
        v-html="sanitizedContent"
      ></div>
    </div>
  </WidgetWrapper>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import WidgetWrapper from './WidgetWrapper.vue'
import type { TextWidgetConfig } from '@/types/dashboard'

const props = withDefaults(defineProps<{
  config: TextWidgetConfig
  selected?: boolean
  readonly?: boolean
  showHeader?: boolean
}>(), {
  selected: false,
  readonly: false,
  showHeader: false
})

const emit = defineEmits<{
  (e: 'select'): void
  (e: 'remove'): void
}>()

// Check if content appears to be markdown
const isMarkdown = computed(() => {
  const content = props.config?.content || ''
  // Simple heuristic: check for common markdown patterns
  return /^#{1,6}\s|^\*\*|^\*\s|^-\s|^\d+\.\s|```|`[^`]+`|\[.+\]\(.+\)/.test(content)
})

// Render markdown content
const renderedContent = computed(() => {
  if (!props.config?.content) return ''
  
  try {
    // Configure marked options
    marked.setOptions({
      breaks: true,
      gfm: true
    })
    
    return marked(props.config.content) as string
  } catch (e) {
    console.error('Markdown parsing error:', e)
    return props.config.content
  }
})

// Sanitize HTML content (basic XSS prevention)
const sanitizedContent = computed(() => {
  const content = props.config?.content || ''
  
  // Allow basic formatting tags, remove potentially dangerous ones
  const allowedTags = ['b', 'i', 'u', 'strong', 'em', 'br', 'p', 'span', 'div', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'ul', 'ol', 'li', 'a']
  
  // Simple sanitization - in production, use a proper sanitizer like DOMPurify
  let sanitized = content
  
  // Remove script tags
  sanitized = sanitized.replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
  
  // Remove event handlers
  sanitized = sanitized.replace(/\s*on\w+\s*=\s*["'][^"']*["']/gi, '')
  
  // Remove javascript: URLs
  sanitized = sanitized.replace(/javascript:/gi, '')
  
  return sanitized
})

// Container styles
const containerStyle = computed(() => ({
  backgroundColor: props.config?.backgroundColor || 'transparent'
}))

// Text styles
const textStyle = computed(() => ({
  fontSize: `${props.config?.fontSize || 14}px`,
  color: props.config?.color || '#333',
  textAlign: props.config?.align || 'left'
}))
</script>

<style scoped>
.text-container {
  height: 100%;
  overflow: auto;
  padding: 12px;
}

.text-content {
  line-height: 1.6;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

/* Markdown specific styles */
.markdown-content :deep(h1) {
  font-size: 1.8em;
  margin: 0.5em 0;
  font-weight: 600;
}

.markdown-content :deep(h2) {
  font-size: 1.5em;
  margin: 0.5em 0;
  font-weight: 600;
}

.markdown-content :deep(h3) {
  font-size: 1.25em;
  margin: 0.5em 0;
  font-weight: 600;
}

.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  font-size: 1em;
  margin: 0.5em 0;
  font-weight: 600;
}

.markdown-content :deep(p) {
  margin: 0.5em 0;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 0.5em 0;
  padding-left: 1.5em;
}

.markdown-content :deep(li) {
  margin: 0.25em 0;
}

.markdown-content :deep(code) {
  background: #f5f5f5;
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.9em;
}

.markdown-content :deep(pre) {
  background: #f5f5f5;
  padding: 1em;
  border-radius: 4px;
  overflow-x: auto;
}

.markdown-content :deep(pre code) {
  background: transparent;
  padding: 0;
}

.markdown-content :deep(blockquote) {
  margin: 0.5em 0;
  padding: 0.5em 1em;
  border-left: 4px solid #1890ff;
  background: #f9f9f9;
  color: #666;
}

.markdown-content :deep(a) {
  color: #1890ff;
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}

.markdown-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 0.5em 0;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  border: 1px solid #e8e8e8;
  padding: 8px 12px;
  text-align: left;
}

.markdown-content :deep(th) {
  background: #fafafa;
  font-weight: 600;
}

.markdown-content :deep(hr) {
  border: none;
  border-top: 1px solid #e8e8e8;
  margin: 1em 0;
}

.markdown-content :deep(img) {
  max-width: 100%;
  height: auto;
}
</style>

<template>
  <Teleport to="body">
    <Transition name="shortcut-hints-fade">
      <div
        v-if="isOpen"
        class="shortcut-hints-overlay"
        tabindex="-1"
        role="dialog"
        aria-modal="true"
        aria-labelledby="shortcut-hints-title"
        @click.self="close"
        @keydown.escape="close"
      >
        <div ref="modalRef" class="shortcut-hints-modal">
          <!-- 头部 -->
          <div class="shortcut-hints-header">
            <h2 id="shortcut-hints-title" class="shortcut-hints-title">
              <span class="shortcut-hints-icon">⌨️</span>
              快捷键
            </h2>
            <button
              class="shortcut-hints-close"
              aria-label="关闭快捷键提示"
              @click="close"
            >
              <span aria-hidden="true">×</span>
            </button>
          </div>

          <!-- 搜索框 -->
          <div class="shortcut-hints-search">
            <input
              ref="searchInputRef"
              v-model="searchQuery"
              type="text"
              class="shortcut-hints-search-input"
              placeholder="搜索快捷键..."
              @input="handleSearch"
            />
            <button
              v-if="searchQuery"
              class="shortcut-hints-search-clear"
              aria-label="清除搜索"
              @click="clearSearch"
            >
              ×
            </button>
          </div>

          <!-- 分类标签 -->
          <div class="shortcut-hints-categories" role="tablist">
            <button
              class="shortcut-hints-category-tab"
              :class="{ active: selectedCategory === null }"
              role="tab"
              :aria-selected="selectedCategory === null"
              @click="selectCategory(null)"
            >
              全部
            </button>
            <button
              v-for="category in categories"
              :key="category.id"
              class="shortcut-hints-category-tab"
              :class="{ active: selectedCategory === category.id }"
              role="tab"
              :aria-selected="selectedCategory === category.id"
              @click="selectCategory(category.id)"
            >
              {{ category.name }}
            </button>
          </div>

          <!-- 快捷键列表 -->
          <div class="shortcut-hints-content" role="tabpanel">
            <template v-if="filteredShortcuts.length > 0">
              <div
                v-for="(shortcuts, category) in groupedShortcuts"
                :key="category"
                class="shortcut-hints-group"
              >
                <h3 class="shortcut-hints-group-title">
                  {{ getCategoryName(category) }}
                </h3>
                <ul class="shortcut-hints-list">
                  <li
                    v-for="shortcut in shortcuts"
                    :key="shortcut.id"
                    class="shortcut-hints-item"
                  >
                    <span class="shortcut-hints-description">
                      {{ shortcut.description }}
                    </span>
                    <kbd class="shortcut-hints-key">
                      {{ formatShortcut(shortcut.key) }}
                    </kbd>
                  </li>
                </ul>
              </div>
            </template>
            <div v-else class="shortcut-hints-empty">
              <p>没有找到匹配的快捷键</p>
            </div>
          </div>

          <!-- 底部提示 -->
          <div class="shortcut-hints-footer">
            <span class="shortcut-hints-tip">
              按 <kbd>Esc</kbd> 或 <kbd>{{ formatShortcut('Ctrl+/') }}</kbd> 关闭
            </span>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
/**
 * 快捷键提示组件
 * Shortcut Hints Component
 * 
 * 显示当前页面可用的快捷键列表，支持搜索和分类筛选。
 * 
 * 需求 19.4: THE DataTeaCup SHALL 显示当前页面可用的快捷键列表
 */

import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useShortcutHints } from './useShortcutHints'
import type { ShortcutCategory, ShortcutItem } from './shortcutHintsTypes'

// ==================== Composable ====================

const {
  isOpen,
  searchQuery,
  selectedCategory,
  categories,
  open,
  close,
  toggle,
  selectCategory,
  setSearchQuery,
  clearSearch,
  getAllShortcuts,
  getShortcutsByCategory,
  search,
  formatShortcut,
  getCategoryInfo
} = useShortcutHints()

// ==================== Refs ====================

const modalRef = ref<HTMLElement | null>(null)
const searchInputRef = ref<HTMLInputElement | null>(null)

// ==================== Computed ====================

/**
 * 过滤后的快捷键列表
 */
const filteredShortcuts = computed<ShortcutItem[]>(() => {
  let shortcuts: ShortcutItem[]
  
  if (searchQuery.value) {
    shortcuts = search(searchQuery.value)
  } else if (selectedCategory.value) {
    shortcuts = getShortcutsByCategory(selectedCategory.value)
  } else {
    shortcuts = getAllShortcuts()
  }
  
  return shortcuts
})

/**
 * 按分类分组的快捷键
 */
const groupedShortcuts = computed<Map<ShortcutCategory, ShortcutItem[]>>(() => {
  const grouped = new Map<ShortcutCategory, ShortcutItem[]>()
  
  filteredShortcuts.value.forEach(shortcut => {
    if (!grouped.has(shortcut.category)) {
      grouped.set(shortcut.category, [])
    }
    grouped.get(shortcut.category)!.push(shortcut)
  })
  
  return grouped
})

// ==================== Methods ====================

/**
 * 获取分类名称
 */
function getCategoryName(category: ShortcutCategory): string {
  const info = getCategoryInfo(category)
  return info?.name || category
}

/**
 * 处理搜索输入
 */
function handleSearch(event: Event): void {
  const target = event.target as HTMLInputElement
  setSearchQuery(target.value)
}

/**
 * 处理键盘事件
 */
function handleKeyDown(event: KeyboardEvent): void {
  // Ctrl+/ 或 ? 切换显示
  if ((event.ctrlKey && event.key === '/') || 
      (event.key === '?' && !event.ctrlKey && !event.altKey && !event.metaKey)) {
    // 检查是否在输入框中
    const target = event.target as HTMLElement
    const isInputElement = target.tagName === 'INPUT' || 
                          target.tagName === 'TEXTAREA' || 
                          target.isContentEditable
    
    // 如果是 ? 键且在输入框中，不处理
    if (event.key === '?' && isInputElement) {
      return
    }
    
    event.preventDefault()
    toggle()
  }
}

// ==================== Lifecycle ====================

onMounted(() => {
  // 注册全局快捷键
  window.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  // 移除全局快捷键
  window.removeEventListener('keydown', handleKeyDown)
})

// 打开时聚焦搜索框
watch(isOpen, async (newValue) => {
  if (newValue) {
    await nextTick()
    searchInputRef.value?.focus()
  }
})

// ==================== Expose ====================

defineExpose({
  open,
  close,
  toggle
})
</script>


<style scoped>
/* ==================== 遮罩层 ==================== */
.shortcut-hints-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px;
}

/* ==================== 模态框 ==================== */
.shortcut-hints-modal {
  background-color: var(--n-color, #fff);
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  width: 100%;
  max-width: 600px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ==================== 头部 ==================== */
.shortcut-hints-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--n-border-color, #e8e8e8);
}

.shortcut-hints-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--n-text-color, #333);
  display: flex;
  align-items: center;
  gap: 8px;
}

.shortcut-hints-icon {
  font-size: 20px;
}

.shortcut-hints-close {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  font-size: 20px;
  color: var(--n-text-color-3, #999);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.shortcut-hints-close:hover {
  background-color: var(--n-color-hover, #f5f5f5);
  color: var(--n-text-color, #333);
}

/* ==================== 搜索框 ==================== */
.shortcut-hints-search {
  padding: 12px 20px;
  position: relative;
}

.shortcut-hints-search-input {
  width: 100%;
  padding: 10px 36px 10px 12px;
  border: 1px solid var(--n-border-color, #e8e8e8);
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s ease;
  background-color: var(--n-color, #fff);
  color: var(--n-text-color, #333);
}

.shortcut-hints-search-input:focus {
  border-color: var(--n-primary-color, #18a058);
}

.shortcut-hints-search-input::placeholder {
  color: var(--n-text-color-3, #999);
}

.shortcut-hints-search-clear {
  position: absolute;
  right: 28px;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  border: none;
  background: var(--n-color-hover, #f5f5f5);
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
  color: var(--n-text-color-3, #999);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.shortcut-hints-search-clear:hover {
  background-color: var(--n-color-pressed, #e8e8e8);
  color: var(--n-text-color, #333);
}

/* ==================== 分类标签 ==================== */
.shortcut-hints-categories {
  display: flex;
  gap: 8px;
  padding: 0 20px 12px;
  overflow-x: auto;
  flex-wrap: wrap;
}

.shortcut-hints-category-tab {
  padding: 6px 12px;
  border: 1px solid var(--n-border-color, #e8e8e8);
  background: transparent;
  border-radius: 16px;
  font-size: 13px;
  color: var(--n-text-color-2, #666);
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.shortcut-hints-category-tab:hover {
  border-color: var(--n-primary-color, #18a058);
  color: var(--n-primary-color, #18a058);
}

.shortcut-hints-category-tab.active {
  background-color: var(--n-primary-color, #18a058);
  border-color: var(--n-primary-color, #18a058);
  color: #fff;
}

/* ==================== 内容区域 ==================== */
.shortcut-hints-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px 16px;
}

/* ==================== 分组 ==================== */
.shortcut-hints-group {
  margin-bottom: 16px;
}

.shortcut-hints-group:last-child {
  margin-bottom: 0;
}

.shortcut-hints-group-title {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--n-text-color-3, #999);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* ==================== 快捷键列表 ==================== */
.shortcut-hints-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.shortcut-hints-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: 6px;
  transition: background-color 0.2s ease;
}

.shortcut-hints-item:hover {
  background-color: var(--n-color-hover, #f5f5f5);
}

.shortcut-hints-description {
  font-size: 14px;
  color: var(--n-text-color, #333);
}

.shortcut-hints-key {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background-color: var(--n-color-hover, #f5f5f5);
  border: 1px solid var(--n-border-color, #e8e8e8);
  border-radius: 4px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  color: var(--n-text-color-2, #666);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

/* ==================== 空状态 ==================== */
.shortcut-hints-empty {
  text-align: center;
  padding: 40px 20px;
  color: var(--n-text-color-3, #999);
}

.shortcut-hints-empty p {
  margin: 0;
  font-size: 14px;
}

/* ==================== 底部 ==================== */
.shortcut-hints-footer {
  padding: 12px 20px;
  border-top: 1px solid var(--n-border-color, #e8e8e8);
  text-align: center;
}

.shortcut-hints-tip {
  font-size: 12px;
  color: var(--n-text-color-3, #999);
}

.shortcut-hints-tip kbd {
  display: inline-block;
  padding: 2px 6px;
  background-color: var(--n-color-hover, #f5f5f5);
  border: 1px solid var(--n-border-color, #e8e8e8);
  border-radius: 3px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
  margin: 0 2px;
}

/* ==================== 动画 ==================== */
.shortcut-hints-fade-enter-active,
.shortcut-hints-fade-leave-active {
  transition: opacity 0.2s ease;
}

.shortcut-hints-fade-enter-active .shortcut-hints-modal,
.shortcut-hints-fade-leave-active .shortcut-hints-modal {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.shortcut-hints-fade-enter-from,
.shortcut-hints-fade-leave-to {
  opacity: 0;
}

.shortcut-hints-fade-enter-from .shortcut-hints-modal,
.shortcut-hints-fade-leave-to .shortcut-hints-modal {
  transform: scale(0.95);
  opacity: 0;
}

/* ==================== 暗色模式支持 ==================== */
:root.dark .shortcut-hints-modal {
  background-color: #1e1e1e;
}

:root.dark .shortcut-hints-header,
:root.dark .shortcut-hints-footer {
  border-color: #333;
}

:root.dark .shortcut-hints-search-input {
  background-color: #2d2d2d;
  border-color: #333;
  color: #e0e0e0;
}

:root.dark .shortcut-hints-category-tab {
  border-color: #333;
  color: #999;
}

:root.dark .shortcut-hints-item:hover {
  background-color: #2d2d2d;
}

:root.dark .shortcut-hints-key {
  background-color: #2d2d2d;
  border-color: #333;
}
</style>


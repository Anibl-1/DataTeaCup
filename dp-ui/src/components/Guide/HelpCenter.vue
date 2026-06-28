<template>
  <Teleport to="body">
    <Transition name="help-center">
      <div
        v-if="isOpen"
        class="help-center"
        role="dialog"
        aria-modal="true"
        aria-label="帮助中心"
      >
        <!-- 遮罩层 -->
        <div class="help-center__backdrop" @click="handleClose" />
        
        <!-- 主面板 -->
        <div class="help-center__panel">
          <!-- 头部 -->
          <div class="help-center__header">
            <div class="help-center__title-row">
              <h2 class="help-center__title">
                <NIcon :size="20"><HelpCircleOutline /></NIcon>
                帮助中心
              </h2>
              <button
                class="help-center__close"
                aria-label="关闭帮助中心"
                @click="handleClose"
              >
                <NIcon :size="18"><CloseOutline /></NIcon>
              </button>
            </div>
            
            <!-- 搜索框 -->
            <div class="help-center__search">
              <NInput
                v-model:value="localSearchQuery"
                placeholder="搜索帮助文档..."
                clearable
                @update:value="handleSearchInput"
              >
                <template #prefix>
                  <NIcon :size="16"><SearchOutline /></NIcon>
                </template>
              </NInput>
            </div>
          </div>

          <!-- 内容区域 -->
          <div class="help-center__content">
            <!-- 搜索结果 -->
            <div v-if="searchQuery && searchResults.length > 0" class="help-center__search-results">
              <div class="help-center__section-title">
                搜索结果 ({{ searchResults.length }})
              </div>
              <div class="help-center__article-list">
                <div
                  v-for="result in searchResults"
                  :key="result.article.id"
                  class="help-center__article-item"
                  @click="handleViewArticle(result.article.id)"
                >
                  <div class="help-center__article-title" v-html="result.highlightedTitle" />
                  <div class="help-center__article-summary" v-html="result.highlightedSummary" />
                  <div class="help-center__article-meta">
                    <NTag size="small" :bordered="false">
                      {{ getCategoryName(result.article.category) }}
                    </NTag>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 无搜索结果 -->
            <div v-else-if="searchQuery && searchResults.length === 0" class="help-center__empty">
              <NIcon :size="48" color="#d9d9d9"><SearchOutline /></NIcon>
              <p>未找到相关文档</p>
              <p class="help-center__empty-hint">尝试使用其他关键词搜索</p>
            </div>
            
            <!-- 文章详情 -->
            <div v-else-if="currentArticle" class="help-center__article-detail">
              <button class="help-center__back" @click="handleBackToList">
                <NIcon :size="16"><ArrowBackOutline /></NIcon>
                返回列表
              </button>
              <div class="help-center__article-content">
                <h3 class="help-center__article-detail-title">{{ currentArticle.title }}</h3>
                <div class="help-center__article-detail-meta">
                  <NTag size="small" :bordered="false">
                    {{ getCategoryName(currentArticle.category) }}
                  </NTag>
                  <span v-if="currentArticle.lastUpdated" class="help-center__article-date">
                    更新于 {{ currentArticle.lastUpdated }}
                  </span>
                </div>
                <div class="help-center__markdown" v-html="renderedContent" />
              </div>
            </div>
            
            <!-- 分类和文章列表 -->
            <div v-else class="help-center__main">
              <!-- 热门文章 -->
              <div v-if="popularArticles.length > 0" class="help-center__section">
                <div class="help-center__section-title">
                  <NIcon :size="16"><FlameOutline /></NIcon>
                  热门文章
                </div>
                <div class="help-center__popular-list">
                  <div
                    v-for="article in popularArticles"
                    :key="article.id"
                    class="help-center__popular-item"
                    @click="handleViewArticle(article.id)"
                  >
                    <span class="help-center__popular-title">{{ article.title }}</span>
                    <NIcon :size="14"><ChevronForwardOutline /></NIcon>
                  </div>
                </div>
              </div>
              
              <!-- 分类列表 -->
              <div class="help-center__section">
                <div class="help-center__section-title">
                  <NIcon :size="16"><FolderOutline /></NIcon>
                  文档分类
                </div>
                <div class="help-center__category-grid">
                  <div
                    v-for="category in categories"
                    :key="category.id"
                    class="help-center__category-card"
                    :class="{ 'help-center__category-card--active': selectedCategory === category.id }"
                    @click="handleSelectCategory(category.id)"
                  >
                    <div class="help-center__category-icon">
                      <NIcon :size="24"><component :is="getCategoryIcon(category.icon)" /></NIcon>
                    </div>
                    <div class="help-center__category-info">
                      <div class="help-center__category-name">{{ category.name }}</div>
                      <div class="help-center__category-desc">{{ category.description }}</div>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- 选中分类的文章列表 -->
              <div v-if="selectedCategory && categoryArticles.length > 0" class="help-center__section">
                <div class="help-center__section-title">
                  {{ getCategoryName(selectedCategory) }} 文档
                </div>
                <div class="help-center__article-list">
                  <div
                    v-for="article in categoryArticles"
                    :key="article.id"
                    class="help-center__article-item"
                    @click="handleViewArticle(article.id)"
                  >
                    <div class="help-center__article-title">{{ article.title }}</div>
                    <div class="help-center__article-summary">{{ article.summary }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 底部 -->
          <div class="help-center__footer">
            <span class="help-center__footer-text">
              按 <kbd>?</kbd> 或 <kbd>F1</kbd> 打开帮助中心
            </span>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>


<script setup lang="ts">
/**
 * 帮助中心组件
 * Help Center Component
 * 
 * 提供内置的帮助文档系统，支持分类浏览、搜索和上下文帮助。
 * 
 * 需求 19.3: THE DataTeaCup SHALL 提供内置的帮助文档系统
 */
import { ref, computed, watch, onMounted, onUnmounted, type Component } from 'vue'
import { NInput, NIcon, NTag } from 'naive-ui'
import {
  HelpCircleOutline,
  CloseOutline,
  SearchOutline,
  ArrowBackOutline,
  FlameOutline,
  FolderOutline,
  ChevronForwardOutline,
  RocketOutline,
  DocumentOutline,
  BarChartOutline,
  ServerOutline,
  ShieldOutline,
  CodeOutline,
  DownloadOutline,
  SpeedometerOutline,
  BugOutline,
  HelpCircle
} from '@vicons/ionicons5'
import { useHelp } from './HelpService'
import { PRESET_HELP_ARTICLES } from './presetHelpArticles'
import type { HelpCategory, HelpArticle } from './helpTypes'

// ==================== Props & Emits ====================

interface Props {
  /** 初始显示的分类 */
  initialCategory?: HelpCategory
  /** 当前功能路径（用于上下文帮助） */
  currentFeature?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'article-view', article: HelpArticle): void
}>()

// ==================== Composables ====================

const {
  isOpen,
  selectedCategory,
  currentArticle,
  searchQuery,
  searchResults,
  categories,
  open,
  close,
  selectCategory,
  viewArticle,
  backToList,
  setSearchQuery,
  getArticlesByCategory,
  getPopularArticles,
  registerArticles
} = useHelp()

// ==================== Local State ====================

const localSearchQuery = ref('')
let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null

// ==================== Computed ====================

const popularArticles = computed(() => getPopularArticles())

const categoryArticles = computed(() => {
  if (!selectedCategory.value) return []
  return getArticlesByCategory(selectedCategory.value)
})

const renderedContent = computed(() => {
  if (!currentArticle.value) return ''
  // 简单的 Markdown 渲染（实际项目中可使用 marked 等库）
  return renderMarkdown(currentArticle.value.content)
})

// ==================== Icon Mapping ====================

const iconMap: Record<string, Component> = {
  'rocket': RocketOutline,
  'document': DocumentOutline,
  'bar-chart': BarChartOutline,
  'database': ServerOutline,
  'shield': ShieldOutline,
  'code': CodeOutline,
  'download': DownloadOutline,
  'speedometer': SpeedometerOutline,
  'bug': BugOutline,
  'help-circle': HelpCircle
}

function getCategoryIcon(iconName: string): Component {
  return iconMap[iconName] || HelpCircle
}

// ==================== Methods ====================

function getCategoryName(categoryId: HelpCategory): string {
  const category = categories.value.find(c => c.id === categoryId)
  return category?.name || categoryId
}

function handleClose(): void {
  close()
  emit('close')
}

function handleSelectCategory(categoryId: HelpCategory): void {
  if (selectedCategory.value === categoryId) {
    selectCategory(null)
  } else {
    selectCategory(categoryId)
  }
}

function handleViewArticle(articleId: string): void {
  viewArticle(articleId)
  if (currentArticle.value) {
    emit('article-view', currentArticle.value)
  }
}

function handleBackToList(): void {
  backToList()
}

function handleSearchInput(value: string): void {
  // 防抖搜索
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer)
  }
  searchDebounceTimer = setTimeout(() => {
    setSearchQuery(value)
  }, 300)
}

function renderMarkdown(content: string): string {
  // 简单的 Markdown 渲染
  return content
    // 标题
    .replace(/^### (.*$)/gm, '<h4>$1</h4>')
    .replace(/^## (.*$)/gm, '<h3>$1</h3>')
    .replace(/^# (.*$)/gm, '<h2>$1</h2>')
    // 粗体
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    // 斜体
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    // 代码
    .replace(/`(.*?)`/g, '<code>$1</code>')
    // 列表
    .replace(/^- (.*$)/gm, '<li>$1</li>')
    // 段落
    .replace(/\n\n/g, '</p><p>')
    // 换行
    .replace(/\n/g, '<br>')
}

function handleKeydown(e: KeyboardEvent): void {
  // ESC 关闭
  if (e.key === 'Escape' && isOpen.value) {
    handleClose()
  }
  // ? 或 F1 打开
  if ((e.key === '?' || e.key === 'F1') && !isOpen.value) {
    e.preventDefault()
    open(props.initialCategory)
  }
}

// ==================== Lifecycle ====================

onMounted(() => {
  // 注册预设文章
  registerArticles(PRESET_HELP_ARTICLES)
  
  // 监听键盘事件
  window.addEventListener('keydown', handleKeydown)
  
  // 如果有初始分类，打开并选中
  if (props.initialCategory) {
    selectCategory(props.initialCategory)
  }
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer)
  }
})

// ==================== Watchers ====================

watch(searchQuery, (newQuery) => {
  localSearchQuery.value = newQuery
})

// ==================== Expose ====================

defineExpose({
  open,
  close
})
</script>


<style scoped>
.help-center {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9998;
  display: flex;
  justify-content: flex-end;
}

.help-center__backdrop {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.3);
}

.help-center__panel {
  position: relative;
  width: 480px;
  max-width: 100%;
  height: 100%;
  background: #fff;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  animation: slide-in 0.3s ease;
}

@keyframes slide-in {
  from {
    transform: translateX(100%);
  }
  to {
    transform: translateX(0);
  }
}

.help-center__header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.help-center__title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.help-center__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #262626;
}

.help-center__close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  background: transparent;
  color: #8c8c8c;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.help-center__close:hover {
  background: #f5f5f5;
  color: #262626;
}

.help-center__search {
  margin-top: 8px;
}

.help-center__content {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

.help-center__section {
  margin-bottom: 24px;
}

.help-center__section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #595959;
  margin-bottom: 12px;
}

.help-center__popular-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.help-center__popular-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.help-center__popular-item:hover {
  background: #f0f0f0;
}

.help-center__popular-title {
  font-size: 14px;
  color: #262626;
}

.help-center__category-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.help-center__category-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: #fafafa;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.help-center__category-card:hover {
  background: #f0f0f0;
  border-color: #d9d9d9;
}

.help-center__category-card--active {
  background: #e6f7ff;
  border-color: #1890ff;
}

.help-center__category-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: #fff;
  border-radius: 8px;
  color: #1890ff;
  flex-shrink: 0;
}

.help-center__category-info {
  flex: 1;
  min-width: 0;
}

.help-center__category-name {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  margin-bottom: 2px;
}

.help-center__category-desc {
  font-size: 12px;
  color: #8c8c8c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.help-center__article-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.help-center__article-item {
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.help-center__article-item:hover {
  background: #f0f0f0;
}

.help-center__article-title {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
  margin-bottom: 4px;
}

.help-center__article-title :deep(mark) {
  background: #ffe58f;
  padding: 0 2px;
  border-radius: 2px;
}

.help-center__article-summary {
  font-size: 13px;
  color: #8c8c8c;
  line-height: 1.5;
}

.help-center__article-summary :deep(mark) {
  background: #ffe58f;
  padding: 0 2px;
  border-radius: 2px;
}

.help-center__article-meta {
  margin-top: 8px;
}

.help-center__search-results {
  margin-bottom: 16px;
}

.help-center__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  text-align: center;
}

.help-center__empty p {
  margin: 12px 0 0;
  font-size: 14px;
  color: #8c8c8c;
}

.help-center__empty-hint {
  font-size: 12px !important;
  color: #bfbfbf !important;
}

.help-center__article-detail {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.help-center__back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  margin-bottom: 16px;
  border: none;
  background: #f5f5f5;
  color: #595959;
  font-size: 13px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.help-center__back:hover {
  background: #e8e8e8;
  color: #262626;
}

.help-center__article-content {
  flex: 1;
  overflow-y: auto;
}

.help-center__article-detail-title {
  margin: 0 0 12px;
  font-size: 20px;
  font-weight: 600;
  color: #262626;
}

.help-center__article-detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.help-center__article-date {
  font-size: 12px;
  color: #8c8c8c;
}

.help-center__markdown {
  font-size: 14px;
  line-height: 1.8;
  color: #595959;
}

.help-center__markdown :deep(h2) {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
  margin: 24px 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.help-center__markdown :deep(h3) {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin: 20px 0 10px;
}

.help-center__markdown :deep(h4) {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin: 16px 0 8px;
}

.help-center__markdown :deep(p) {
  margin: 12px 0;
}

.help-center__markdown :deep(code) {
  padding: 2px 6px;
  background: #f5f5f5;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  color: #d4380d;
}

.help-center__markdown :deep(li) {
  margin: 6px 0;
  padding-left: 8px;
  list-style-position: inside;
}

.help-center__markdown :deep(strong) {
  font-weight: 600;
  color: #262626;
}

.help-center__footer {
  padding: 12px 20px;
  border-top: 1px solid #f0f0f0;
  text-align: center;
  flex-shrink: 0;
}

.help-center__footer-text {
  font-size: 12px;
  color: #8c8c8c;
}

.help-center__footer-text kbd {
  display: inline-block;
  padding: 2px 6px;
  background: #f5f5f5;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-family: inherit;
  font-size: 11px;
}

/* 过渡动画 */
.help-center-enter-active,
.help-center-leave-active {
  transition: opacity 0.3s ease;
}

.help-center-enter-active .help-center__panel,
.help-center-leave-active .help-center__panel {
  transition: transform 0.3s ease;
}

.help-center-enter-from,
.help-center-leave-to {
  opacity: 0;
}

.help-center-enter-from .help-center__panel,
.help-center-leave-to .help-center__panel {
  transform: translateX(100%);
}
</style>


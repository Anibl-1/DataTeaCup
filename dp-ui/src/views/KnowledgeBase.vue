<template>
  <div class="knowledge-page">
    <!-- Page_Header_Stats: 文章总数、热门文章数、分类数 (Req 1.1, 14.6) -->
    <div class="page-header-stats">
      <div class="stat-item">
        <div class="stat-icon stat-icon-primary">
          <n-icon size="22"><BookOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ totalCount }}</span>
          <span class="stat-label">{{ t('kb.totalArticles') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-success">
          <n-icon size="22"><FlameOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ popularArticles.length }}</span>
          <span class="stat-label">{{ t('kb.popularArticles') }}</span>
        </div>
      </div>
      <div class="stat-item">
        <div class="stat-icon stat-icon-warning">
          <n-icon size="22"><FolderOpenOutline /></n-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ categoryCount }}</span>
          <span class="stat-label">{{ t('kb.categoryCount') }}</span>
        </div>
      </div>
    </div>

    <!-- 热门文章展示区域 (Req 14.5) -->
    <n-card v-if="popularArticles.length > 0" style="margin-bottom: 16px;">
      <template #header>
        <div class="card-header-custom">
          <n-icon size="20" color="#F59E0B" class="header-icon"><FlameOutline /></n-icon>
          <span>热门文章</span>
        </div>
      </template>
      <n-grid :cols="3" :x-gap="12" :y-gap="12" responsive="screen" :collapsed-rows="1">
        <n-grid-item v-for="article in popularArticles" :key="article.id">
          <n-card hoverable size="small" style="cursor: pointer;" @click="handleViewArticle(article)">
            <div class="popular-article-item">
              <div class="popular-title">{{ article.title }}</div>
              <div class="popular-meta">
                <n-space size="small" align="center">
                  <n-tag v-if="article.category" size="tiny" :type="getCategoryType(article.category)">{{ getCategoryLabel(article.category) }}</n-tag>
                  <span class="view-count"><n-icon size="12"><EyeOutline /></n-icon> {{ article.viewCount }}</span>
                  <span v-if="article.helpfulCount" class="view-count"><n-icon size="12"><ThumbsUpOutline /></n-icon> {{ article.helpfulCount }}</span>
                </n-space>
                <span class="popular-author">{{ article.authorName || t('kb.anonymous') }}</span>
              </div>
            </div>
          </n-card>
        </n-grid-item>
      </n-grid>
    </n-card>

    <!-- 主内容区：分类树 + 文章列表 -->
    <div class="kb-layout">
      <!-- 左侧分类树 -->
      <n-card size="small" class="kb-category-tree">
        <template #header>
          <div class="card-header-custom">
            <n-icon size="18" color="#10b981"><FolderOpenOutline /></n-icon>
            <span>{{ t('kb.categoryNav') }}</span>
          </div>
        </template>
        <n-tree
          :data="categoryTreeData"
          block-line
          :selected-keys="selectedCategoryKeys"
          @update:selected-keys="handleCategoryTreeSelect"
        />
        <!-- 热门标签云 -->
        <n-divider style="margin:12px 0 8px;font-size:12px">{{ t('kb.hotTags') }}</n-divider>
        <n-space :size="6" style="padding:0 4px;">
          <n-tag v-for="tag in hotTags" :key="tag.name" size="small" round :type="selectedTag === tag.name ? 'primary' : 'default'" style="cursor:pointer;" @click="handleTagClick(tag.name)">
            {{ tag.name }} ({{ tag.count }})
          </n-tag>
          <n-text v-if="hotTags.length === 0" depth="3" style="font-size:12px;">{{ t('kb.noTags') }}</n-text>
        </n-space>
      </n-card>

      <!-- 右侧文章列表 -->
      <n-card class="main-card kb-main-content">
        <template #header>
          <div class="card-header-custom">
            <n-icon size="20" color="var(--color-primary)" class="header-icon"><BookOutline /></n-icon>
            <span>{{ t('kb.knowledgeBase') }}</span>
          </div>
        </template>
        <template #header-extra>
          <n-button type="primary" @click="handleCreate">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            创建文章
          </n-button>
        </template>

        <!-- Query_Form: 搜索和分类筛选 (Req 1.4, 14.4) -->
        <n-form class="query-form" inline>
          <n-form-item>
            <n-input v-model:value="searchKeyword" :placeholder="t('kb.searchArticles')" clearable style="width: 200px;" @keyup.enter="handleSearch" @clear="handleSearch">
              <template #prefix><n-icon><SearchOutline /></n-icon></template>
            </n-input>
          </n-form-item>
          <n-form-item>
            <n-select v-model:value="searchCategory" :options="categoryOptions" :placeholder="t('kb.categoryFilter')" clearable style="width: 150px;" @update:value="handleSearch" />
          </n-form-item>
          <n-form-item class="query-form-actions">
            <n-button type="primary" @click="handleSearch">{{ t('common.search') }}</n-button>
            <n-button @click="handleReset">{{ t('common.reset') }}</n-button>
          </n-form-item>
        </n-form>

        <!-- 数据表格 (Req 1.6) -->
        <n-data-table
          :columns="columns"
          :data="articleList"
          :loading="loading"
          :pagination="false"
          :row-key="(row: KnowledgeArticle) => row.id"
          :scroll-x="1000"
          striped
          class="custom-table"
        />

        <!-- 空状态 -->
        <n-empty v-if="!loading && articleList.length === 0" :description="t('kb.noArticles')" style="margin: 32px 0;" />

        <!-- Pagination_Wrapper (Req 1.5) -->
        <div class="pagination-wrapper">
          <div class="pagination-info">
            <n-tag type="info" size="small" round>{{ t('kb.totalRecords', { count: totalCount }) }}</n-tag>
          </div>
          <n-pagination
            v-model:page="currentPage"
            v-model:page-size="pageSize"
            :item-count="totalCount"
            :page-sizes="PAGE_SIZES"
            show-size-picker
            show-quick-jumper
            @update:page="handlePageChange"
            @update:page-size="handlePageSizeChange"
          />
        </div>
      </n-card>
    </div>

    <!-- 创建/编辑文章弹窗 (Req 14.4) -->
    <n-modal v-model:show="showFormModal" preset="card" :title="editingArticle ? t('kb.editArticle') : t('kb.createArticle')" style="width: 800px; border-radius: 16px;">
      <n-form ref="formRef" :model="articleForm" :rules="formRules" label-placement="left" label-width="80px">
        <n-form-item :label="t('kb.title')" path="title">
          <n-input v-model:value="articleForm.title" :placeholder="t('kb.enterTitle')" />
        </n-form-item>
        <n-form-item :label="t('kb.category')" path="category">
          <n-select v-model:value="articleForm.category" :options="categoryOptions" :placeholder="t('kb.selectCategory')" />
        </n-form-item>
        <n-form-item :label="t('kb.tags')">
          <n-dynamic-tags v-model:value="articleForm.tags" />
        </n-form-item>
        <n-form-item :label="t('kb.content')" path="content">
          <n-input v-model:value="articleForm.content" type="textarea" :placeholder="t('kb.enterContent')" :rows="12" />
        </n-form-item>
        <n-form-item :label="t('kb.attachments')">
          <n-upload :action="uploadAction" :headers="uploadHeaders" :max="5" multiple :default-file-list="existingFiles" @finish="handleUploadFinish" @remove="handleUploadRemove">
            <n-button size="small">{{ t('kb.uploadAttachments') }}</n-button>
          </n-upload>
        </n-form-item>
        <n-form-item>
          <n-space style="width: 100%; justify-content: flex-end;">
            <n-button @click="showFormModal = false">{{ t('common.cancel') }}</n-button>
            <n-button type="primary" :loading="submitLoading" @click="handleSubmit">{{ t('common.save') }}</n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-modal>

    <!-- 文章详情抽屉 (Req 14.5) -->
    <n-drawer v-model:show="showDetailDrawer" :width="640" placement="right">
      <n-drawer-content closable>
        <template #header>
          <div style="max-width: 540px;">{{ detailArticle?.title || t('kb.articleDetail') }}</div>
        </template>
        <template v-if="detailArticle">
          <!-- 元信息 -->
          <div class="detail-meta-bar">
            <n-space size="small" align="center" wrap>
              <n-tag v-if="detailArticle.category" :type="getCategoryType(detailArticle.category)" size="small">{{ getCategoryLabel(detailArticle.category) }}</n-tag>
              <n-tag v-for="tag in (detailArticle.tags || [])" :key="tag" size="small" round>{{ tag }}</n-tag>
            </n-space>
            <n-space size="small" align="center" class="detail-stats">
              <span><n-icon size="14"><PersonOutline /></n-icon> {{ detailArticle.authorName || t('kb.anonymous') }}</span>
              <span><n-icon size="14"><TimeOutline /></n-icon> {{ fmtTime(detailArticle.createTime) }}</span>
              <span><n-icon size="14"><EyeOutline /></n-icon> {{ detailArticle.viewCount }} 次浏览</span>
              <span v-if="detailArticle.helpfulCount"><n-icon size="14"><ThumbsUpOutline /></n-icon> {{ detailArticle.helpfulCount }} 人觉得有帮助</span>
            </n-space>
          </div>

          <n-divider />

          <!-- 文章内容 -->
          <div class="article-content" v-html="renderContent(detailArticle.content)"></div>

          <!-- 附件区域 -->
          <template v-if="detailArticle.attachments && detailArticle.attachments.length > 0">
            <n-divider>{{ t('kb.attachments') }} ({{ detailArticle.attachments.length }})</n-divider>
            <n-space vertical :size="4">
              <div v-for="(url, idx) in detailArticle.attachments" :key="idx" class="attachment-item">
                <n-icon size="16"><AttachOutline /></n-icon>
                <a :href="'/api' + url" target="_blank" class="attachment-link">{{ getFileName(url) }}</a>
                <n-button text size="tiny" tag="a" :href="'/api' + url" target="_blank">{{ t('common.download') }}</n-button>
              </div>
            </n-space>
          </template>

          <!-- 操作区 -->
          <n-divider />
          <n-space>
            <n-button type="primary" size="small" @click="handleHelpful">
              <template #icon><n-icon><ThumbsUpOutline /></n-icon></template>
              有帮助
            </n-button>
            <n-button size="small" @click="handleEdit(detailArticle)">{{ t('common.edit') }}</n-button>
            <n-popconfirm @positive-click="handleDeleteFromDetail">
              <template #trigger>
                <n-button size="small" type="error">{{ t('common.delete') }}</n-button>
              </template>
              确定删除此文章吗？
            </n-popconfirm>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive, onMounted, h, computed } from 'vue'
import { NButton, NSpace, NIcon, NTag, NPopconfirm, useMessage, type FormInst, type UploadFileInfo } from 'naive-ui'
import {
  BookOutline, FlameOutline, FolderOpenOutline, SearchOutline, AddOutline,
  AttachOutline, EyeOutline, ThumbsUpOutline, PersonOutline, TimeOutline
} from '@vicons/ionicons5'
import {
  searchKnowledge, createKnowledgeArticle, getPopularArticles, getKnowledgeArticle,
  updateKnowledgeArticle, deleteKnowledgeArticle, type KnowledgeArticle
} from '@/api/ticket'
import { formatDateTime } from '@/utils/format'
import { useI18n } from '@/i18n'
import { handleApiError } from '@/utils/error'
import { initMessage } from '@/utils/message'
import { DEFAULT_PAGE, DEFAULT_PAGE_SIZE, PAGE_SIZES } from '@/constants'

const { t } = useI18n()
const message = useMessage()
initMessage(message)

const uploadAction = '/api/file/upload'
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

// Table state
const loading = ref(false)
const articleList = ref<KnowledgeArticle[]>([])
const popularArticles = ref<KnowledgeArticle[]>([])
const totalCount = ref(0)
const currentPage = ref(DEFAULT_PAGE)
const pageSize = ref(DEFAULT_PAGE_SIZE)

// Search filters
const searchKeyword = ref('')
const searchCategory = ref<string | null>(null)

const categoryCount = computed(() => {
  const cats = new Set(articleList.value.map(a => a.category).filter(Boolean))
  return cats.size
})

const categoryOptions = computed(() => [
  { label: t('kb.catFAQ'), value: 'FAQ' },
  { label: t('kb.catGuide'), value: 'Guide' },
  { label: t('kb.catTroubleshooting'), value: 'Troubleshooting' },
  { label: t('kb.catBestPractice'), value: 'BestPractice' },
  { label: t('kb.catRelease'), value: 'Release' },
  { label: t('kb.catDevDoc'), value: 'DevDoc' }
])

const categoryLabelMap: Record<string, string> = {
  FAQ: '常见问题', Guide: '使用指南', Troubleshooting: '故障排查',
  BestPractice: '最佳实践', Release: '版本更新', DevDoc: '开发文档'
}
const categoryTypeMap: Record<string, 'info' | 'success' | 'warning' | 'error' | 'default'> = {
  FAQ: 'info', Guide: 'success', Troubleshooting: 'warning',
  BestPractice: 'default', Release: 'info', DevDoc: 'default'
}
const getCategoryLabel = (cat: string) => categoryLabelMap[cat] || cat
const getCategoryType = (cat: string) => categoryTypeMap[cat] || 'default'

// 分类树数据（计数仅统计当前已加载的文章）
const selectedCategoryKeys = ref<string[]>([])
const categoryCounts = computed(() => {
  const map: Record<string, number> = {}
  articleList.value.forEach(a => { if (a.category) map[a.category] = (map[a.category] || 0) + 1 })
  return map
})
const categoryTreeData = computed(() => {
  const allNode = { key: '__all__', label: `${t('kb.allArticles')} (${totalCount.value})` }
  const catNodes = categoryOptions.value.map((opt: { label: string; value: string }) => {
    const count = categoryCounts.value[opt.value] || 0
    return { key: opt.value, label: `${opt.label}${count > 0 ? ` (${count})` : ''}` }
  })
  return [allNode, ...catNodes]
})
const handleCategoryTreeSelect = (keys: string[]) => {
  selectedCategoryKeys.value = keys
  if (keys.length === 0 || keys[0] === '__all__') {
    searchCategory.value = null
  } else {
    searchCategory.value = keys[0] || null
  }
  currentPage.value = 1
  handleSearch()
}

// 热门标签云
const selectedTag = ref<string | null>(null)
const hotTags = computed(() => {
  const tagMap: Record<string, number> = {}
  articleList.value.forEach(a => {
    (a.tags || []).forEach(tag => { tagMap[tag] = (tagMap[tag] || 0) + 1 })
  })
  return Object.entries(tagMap)
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 15)
})
const handleTagClick = (tag: string) => {
  if (selectedTag.value === tag) {
    selectedTag.value = null
    searchKeyword.value = ''
  } else {
    selectedTag.value = tag
    searchKeyword.value = tag
  }
  currentPage.value = 1
  handleSearch()
}

// Form modal state
const showFormModal = ref(false)
const formRef = ref<FormInst | null>(null)
const submitLoading = ref(false)
const editingArticle = ref<KnowledgeArticle | null>(null)
const existingFiles = ref<UploadFileInfo[]>([])

const articleForm = reactive({
  title: '', content: '', category: '' as string,
  tags: [] as string[], attachments: [] as string[]
})
const formRules = computed(() => ({
  title: { required: true, message: t('kb.titleRequired'), trigger: 'blur' },
  content: { required: true, message: t('kb.contentRequired'), trigger: 'blur' },
  category: { required: true, message: t('kb.categoryRequired'), trigger: 'change' }
}))

// Detail drawer state
const showDetailDrawer = ref(false)
const detailArticle = ref<KnowledgeArticle | null>(null)

const fmtTime = (d?: string) => d ? formatDateTime(d) : '-'
const getFileName = (url: string) => { const p = url.split('/'); return p[p.length - 1] || t('kb.file') }

const renderContent = (content: string): string => {
  if (!content) return ''
  let html = content
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  // headings
  html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>')
  html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>')
  // bold / italic
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')
  // code blocks
  html = html.replace(/```([\s\S]*?)```/g, '<pre style="background:#f4f4f5;padding:12px;border-radius:6px;overflow-x:auto;font-size:13px;">$1</pre>')
  // inline code
  html = html.replace(/`([^`]+)`/g, '<code style="background:#f4f4f5;padding:2px 6px;border-radius:4px;font-size:13px;">$1</code>')
  // line breaks
  html = html.replace(/\n/g, '<br>')
  return html
}

// Upload handlers
const handleUploadFinish = ({ file, event }: { file: UploadFileInfo; event?: ProgressEvent }) => {
  try {
    const res = JSON.parse((event?.target as XMLHttpRequest)?.response || '{}')
    if (res.code === 200 && res.data?.url) { articleForm.attachments.push(res.data.url); file.url = res.data.url }
  } catch { /* ignore */ }
  return file
}
const handleUploadRemove = ({ file }: { file: UploadFileInfo }) => {
  if (file.url) articleForm.attachments = articleForm.attachments.filter(u => u !== file.url)
  return true
}

// Table columns
const columns = computed(() => [
  {
    title: t('kb.title'), key: 'title', ellipsis: { tooltip: true },
    render: (row: KnowledgeArticle) => h('span', { style: 'cursor:pointer;color:var(--color-primary);', onClick: () => handleViewArticle(row) }, row.title)
  },
  {
    title: t('kb.category'), key: 'category', width: 110,
    render: (row: KnowledgeArticle) => row.category
      ? h(NTag, { size: 'small', type: getCategoryType(row.category), bordered: false }, { default: () => getCategoryLabel(row.category) })
      : '-'
  },
  {
    title: t('kb.tags'), key: 'tags', width: 200,
    render: (row: KnowledgeArticle) => {
      if (!row.tags || row.tags.length === 0) return '-'
      return h(NSpace, { size: 'small' }, { default: () => row.tags.slice(0, 3).map(tag => h(NTag, { size: 'small', round: true }, { default: () => tag })) })
    }
  },
  { title: t('kb.author'), key: 'authorName', width: 90, render: (row: KnowledgeArticle) => row.authorName || '-' },
  { title: t('kb.viewCount'), key: 'viewCount', width: 70 },
  {
    title: t('kb.attachments'), key: 'attachments', width: 50,
    render: (row: KnowledgeArticle) => (row.attachments?.length || 0) > 0 ? String(row.attachments!.length) : '-'
  },
  {
    title: t('common.createTime'), key: 'createTime', width: 160,
    render: (row: KnowledgeArticle) => fmtTime(row.createTime || row.createdAt)
  },
  {
    title: t('common.actions'), key: 'actions', width: 160,
    render: (row: KnowledgeArticle) => h(NSpace, { size: 4 }, { default: () => [
      h(NButton, { size: 'small', text: true, type: 'primary', onClick: () => handleViewArticle(row) }, { default: () => t('common.view') }),
      h(NButton, { size: 'small', text: true, type: 'info', onClick: () => handleEdit(row) }, { default: () => t('common.edit') }),
      h(NPopconfirm, { onPositiveClick: () => handleDelete(row) }, {
        trigger: () => h(NButton, { size: 'small', text: true, type: 'error' }, { default: () => t('common.delete') }),
        default: () => t('kb.confirmDeleteArticle')
      })
    ]})
  }
])

// Data fetching
const fetchArticles = async () => {
  loading.value = true
  try {
    const params: any = {}
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (searchCategory.value) params.category = searchCategory.value
    const res = await searchKnowledge(params)
    const data = (res as any).data || res
    if (Array.isArray(data)) {
      articleList.value = data
      totalCount.value = data.length
    } else if (data?.records) {
      articleList.value = data.records
      totalCount.value = data.total || 0
    } else {
      articleList.value = []
      totalCount.value = 0
    }
  } catch (error) {
    message.error(handleApiError(error, t('kb.fetchArticles')))
  } finally {
    loading.value = false
  }
}

const fetchPopularArticles = async () => {
  try {
    const res = await getPopularArticles(6)
    popularArticles.value = (res as any).data || []
  } catch { /* ignore */ }
}

const handleSearch = () => { currentPage.value = 1; fetchArticles() }
const handleReset = () => {
  searchKeyword.value = ''
  searchCategory.value = null
  currentPage.value = 1
  fetchArticles()
}
const handlePageChange = (page: number) => { currentPage.value = page; fetchArticles() }
const handlePageSizeChange = (size: number) => { pageSize.value = size; currentPage.value = 1; fetchArticles() }

// CRUD operations
const handleCreate = () => {
  editingArticle.value = null
  existingFiles.value = []
  articleForm.title = ''; articleForm.content = ''; articleForm.category = ''
  articleForm.tags = []; articleForm.attachments = []
  showFormModal.value = true
}

const handleEdit = (article: KnowledgeArticle) => {
  editingArticle.value = article
  articleForm.title = article.title
  articleForm.content = article.content
  articleForm.category = article.category || ''
  articleForm.tags = article.tags ? [...article.tags] : []
  articleForm.attachments = article.attachments ? [...article.attachments] : []
  existingFiles.value = (article.attachments || []).map((url, i) => ({
    id: String(i), name: getFileName(url), status: 'finished' as const, url
  }))
  showFormModal.value = true
  showDetailDrawer.value = false
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try { await formRef.value.validate() } catch { return }
  submitLoading.value = true
  try {
    const payload: any = { title: articleForm.title, content: articleForm.content, category: articleForm.category, tags: articleForm.tags }
    if (articleForm.attachments.length > 0) payload.attachments = articleForm.attachments
    if (editingArticle.value) {
      await updateKnowledgeArticle(editingArticle.value.id, payload)
      message.success(t('kb.articleUpdated'))
    } else {
      await createKnowledgeArticle(payload)
      message.success(t('kb.articleCreated'))
    }
    showFormModal.value = false
    fetchArticles()
    fetchPopularArticles()
  } catch (error) {
    message.error(handleApiError(error, editingArticle.value ? t('kb.updateArticle') : t('kb.createArticle')))
  } finally {
    submitLoading.value = false
  }
}

const handleViewArticle = async (article: KnowledgeArticle) => {
  try {
    const res = await getKnowledgeArticle(article.id)
    detailArticle.value = (res as any).data || article
    showDetailDrawer.value = true
  } catch {
    detailArticle.value = article
    showDetailDrawer.value = true
  }
}

const handleHelpful = async () => {
  if (!detailArticle.value) return
  try {
    const { default: request } = await import('@/api/request')
    await request.put(`/tickets/knowledge/${detailArticle.value.id}/helpful`)
    message.success(t('kb.thanksFeedback'))
    if (detailArticle.value.helpfulCount != null) detailArticle.value.helpfulCount++
  } catch {
    message.info(t('kb.thanksFeedback'))
  }
}

const handleDelete = async (article: KnowledgeArticle) => {
  try {
    await deleteKnowledgeArticle(article.id)
    message.success(t('kb.articleDeleted'))
    fetchArticles()
    fetchPopularArticles()
  } catch (error) {
    message.error(handleApiError(error, t('kb.deleteArticle')))
  }
}

const handleDeleteFromDetail = async () => {
  if (!detailArticle.value) return
  await handleDelete(detailArticle.value)
  showDetailDrawer.value = false
  detailArticle.value = null
}

onMounted(() => {
  fetchArticles()
  fetchPopularArticles()
})
</script>

<style scoped>
.knowledge-page {
  animation: fadeIn 0.3s ease-out;
}

.kb-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 16px;
}

.kb-category-tree {
  height: fit-content;
  position: sticky;
  top: 16px;
}

.kb-main-content {
  min-width: 0;
}

@media (max-width: 768px) {
  .kb-layout {
    grid-template-columns: 1fr;
  }
  .kb-category-tree {
    position: static;
  }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.popular-article-item {
  cursor: pointer;
}

.popular-title {
  font-weight: 500;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.popular-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.popular-author {
  font-size: 12px;
  color: var(--text-secondary);
}

.view-count {
  font-size: 12px;
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.detail-meta-bar {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-stats {
  font-size: 13px;
  color: var(--text-secondary);
}

.detail-stats span {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.article-content {
  white-space: pre-wrap;
  line-height: 1.8;
  color: var(--text-primary);
  font-size: 14px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 0;
}

.attachment-link {
  color: var(--color-primary);
  text-decoration: none;
  font-size: 13px;
}

.attachment-link:hover {
  text-decoration: underline;
}
</style>

<template>
  <div class="knowledge-base">
    <n-card title="知识库">
      <template #header-extra>
        <n-input
          v-model:value="keyword"
          placeholder="搜索知识库..."
          clearable
          style="width: 240px"
          @update:value="handleSearch"
        >
          <template #prefix>
            <n-icon><SearchOutline /></n-icon>
          </template>
        </n-input>
      </template>

      <n-tabs v-model:value="activeTab" type="line">
        <n-tab-pane name="popular" tab="热门文章">
          <n-list>
            <n-list-item v-for="article in popularArticles" :key="article.id">
              <n-thing :title="article.title" :description="article.category">
                <template #header-extra>
                  <n-space>
                    <n-tag size="small">{{ article.viewCount }} 次浏览</n-tag>
                    <n-tag size="small" type="success">{{ article.helpfulCount }} 人觉得有帮助</n-tag>
                  </n-space>
                </template>
                <div class="article-preview">{{ truncate(article.content, 200) }}</div>
              </n-thing>
            </n-list-item>
          </n-list>
        </n-tab-pane>
        <n-tab-pane name="search" tab="搜索结果">
          <n-empty v-if="!searchResults.length" description="请输入关键词搜索" />
          <n-list v-else>
            <n-list-item v-for="article in searchResults" :key="article.id">
              <n-thing :title="article.title" :description="article.category">
                <div class="article-preview">{{ truncate(article.content, 200) }}</div>
              </n-thing>
            </n-list-item>
          </n-list>
        </n-tab-pane>
      </n-tabs>
    </n-card>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, onMounted } from 'vue'
import { NCard, NInput, NIcon, NTabs, NTabPane, NList, NListItem, NThing, NTag, NSpace, NEmpty } from 'naive-ui'
import { SearchOutline } from '@vicons/ionicons5'
import request from '@/api/request'

const keyword = ref('')
const activeTab = ref('popular')
const popularArticles = ref<any[]>([])
const searchResults = ref<any[]>([])

function truncate(text: string, maxLen: number): string {
  if (!text) return ''
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

async function loadPopular() {
  try {
    const { data } = await request.get('/api/tickets/knowledge/popular', { params: { topN: 10 } })
    popularArticles.value = data || []
  } catch {
    popularArticles.value = []
  }
}

async function handleSearch(value: string) {
  if (!value) {
    searchResults.value = []
    return
  }
  activeTab.value = 'search'
  try {
    const { data } = await request.get('/api/tickets/knowledge', { params: { keyword: value } })
    searchResults.value = data || []
  } catch {
    searchResults.value = []
  }
}

onMounted(loadPopular)
</script>

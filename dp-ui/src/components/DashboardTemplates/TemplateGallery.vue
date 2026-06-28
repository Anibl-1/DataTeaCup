<template>
  <div class="template-gallery">
    <!-- 搜索和筛选 -->
    <div class="gallery-header">
      <n-input
        v-model:value="searchKeyword"
        placeholder="搜索模板..."
        clearable
        size="small"
      >
        <template #prefix>
          <n-icon><SearchOutline /></n-icon>
        </template>
      </n-input>
      <n-select
        v-model:value="selectedCategory"
        :options="categoryOptions"
        placeholder="全部分类"
        clearable
        size="small"
        style="width: 120px"
      />
    </div>

    <!-- 模板网格 -->
    <n-spin :show="loading">
      <div v-if="filteredTemplates.length > 0" class="template-grid">
        <div
          v-for="template in filteredTemplates"
          :key="template.id"
          class="template-card"
          @click="handleTemplateClick(template)"
        >
          <div class="template-thumbnail">
            <img
              v-if="template.thumbnail"
              :src="template.thumbnail"
              :alt="template.name"
            />
            <div v-else class="thumbnail-placeholder">
              <n-icon size="40"><AppsOutline /></n-icon>
            </div>
          </div>
          <div class="template-info">
            <div class="template-name">{{ template.name }}</div>
            <n-tag size="small" :type="getCategoryType(template.category)">
              {{ template.category }}
            </n-tag>
          </div>
          <div class="template-actions">
            <n-button size="tiny" quaternary @click.stop="handlePreview(template)">
              <template #icon><n-icon><EyeOutline /></n-icon></template>
              预览
            </n-button>
            <n-button size="tiny" type="primary" quaternary @click.stop="handleUse(template)">
              <template #icon><n-icon><AddOutline /></n-icon></template>
              使用
            </n-button>
          </div>
        </div>
      </div>
      <n-empty v-else description="暂无模板" />
    </n-spin>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, onMounted } from 'vue'
import { NInput, NSelect, NIcon, NSpin, NEmpty, NButton, NTag, useMessage } from 'naive-ui'
import { SearchOutline, AppsOutline, EyeOutline, AddOutline } from '@vicons/ionicons5'
import type { DashboardTemplate } from '@/types/dashboard'
import { getDashboardTemplates } from '@/api/dashboardDesigner'

const emit = defineEmits<{
  (e: 'preview', template: DashboardTemplate): void
  (e: 'use', template: DashboardTemplate): void
}>()

const message = useMessage()

// 状态
const loading = ref(false)
const templates = ref<DashboardTemplate[]>([])
const searchKeyword = ref('')
const selectedCategory = ref<string | null>(null)

// 分类选项
const categoryOptions = computed(() => {
  const categories = [...new Set(templates.value.map(t => t.category))]
  return categories.map(c => ({ label: c, value: c }))
})

// 过滤后的模板
const filteredTemplates = computed(() => {
  return templates.value.filter(template => {
    const matchKeyword = !searchKeyword.value || 
      template.name.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
      template.description?.toLowerCase().includes(searchKeyword.value.toLowerCase())
    const matchCategory = !selectedCategory.value || template.category === selectedCategory.value
    return matchKeyword && matchCategory
  })
})

// 获取分类标签类型
const getCategoryType = (category: string): 'default' | 'info' | 'success' | 'warning' => {
  const typeMap: Record<string, 'default' | 'info' | 'success' | 'warning'> = {
    '销售分析': 'info',
    '运营监控': 'success',
    '财务概览': 'warning'
  }
  return typeMap[category] || 'default'
}

// 加载模板
const loadTemplates = async () => {
  loading.value = true
  try {
    const res = await getDashboardTemplates()
    templates.value = res.data || []
  } catch (error: any) {
    message.error(error.message || '加载模板失败')
  } finally {
    loading.value = false
  }
}

// 点击模板
const handleTemplateClick = (template: DashboardTemplate) => {
  emit('preview', template)
}

// 预览模板
const handlePreview = (template: DashboardTemplate) => {
  emit('preview', template)
}

// 使用模板
const handleUse = (template: DashboardTemplate) => {
  emit('use', template)
}

// 刷新模板列表
const refresh = () => {
  loadTemplates()
}

// 暴露方法
defineExpose({ refresh })

onMounted(() => {
  loadTemplates()
})
</script>

<style scoped>
.template-gallery {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.gallery-header {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
  overflow-y: auto;
  flex: 1;
}

.template-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.template-card:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
}

.template-thumbnail {
  height: 80px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.template-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumbnail-placeholder {
  color: #c0c4cc;
}

.template-info {
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.template-name {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.template-actions {
  display: flex;
  justify-content: space-between;
  padding: 4px 8px 8px;
  border-top: 1px solid #f0f0f0;
}
</style>

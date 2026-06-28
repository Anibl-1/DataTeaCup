<template>
  <n-modal
    :show="show"
    preset="card"
    :title="title || '移动端发布管理'"
    :style="{ width: isMobileView ? '95vw' : '520px', maxHeight: '80vh' }"
    :bordered="false"
    :segmented="{ content: true }"
    @update:show="$emit('update:show', $event)"
  >
    <div class="publish-dialog">
      <!-- 发布状态概览 -->
      <div class="publish-summary">
        <div class="summary-item">
          <span class="summary-count published">{{ publishedCount }}</span>
          <span class="summary-label">已发布</span>
        </div>
        <div class="summary-item">
          <span class="summary-count unpublished">{{ unpublishedCount }}</span>
          <span class="summary-label">未发布</span>
        </div>
        <div class="summary-item">
          <span class="summary-count total">{{ items.length }}</span>
          <span class="summary-label">总数</span>
        </div>
      </div>

      <!-- 搜索 -->
      <n-input
        v-model:value="searchKeyword"
        placeholder="搜索名称或编码"
        clearable
        size="small"
        style="margin-bottom: 12px"
      >
        <template #prefix>
          <n-icon size="14"><SearchOutline /></n-icon>
        </template>
      </n-input>

      <!-- 资源列表 -->
      <div class="publish-list">
        <div
          v-for="item in filteredItems"
          :key="item.id"
          class="publish-item"
        >
          <div class="item-info">
            <div class="item-name">{{ item.name }}</div>
            <div class="item-code">{{ item.code }}</div>
          </div>
          <n-switch
            :value="item.mobileEnabled === 1"
            size="small"
            @update:value="(v: boolean) => handleToggle(item, v)"
          >
            <template #checked>已发布</template>
            <template #unchecked>未发布</template>
          </n-switch>
        </div>
        <div v-if="filteredItems.length === 0" class="empty-tip">无匹配项</div>
      </div>
    </div>

    <template #footer>
      <div class="publish-footer">
        <n-button size="small" @click="handleBatchPublish">全部发布</n-button>
        <n-button size="small" @click="handleBatchUnpublish">全部取消</n-button>
        <n-button type="primary" size="small" @click="$emit('update:show', false)">完成</n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { NModal, NInput, NIcon, NSwitch, NButton } from 'naive-ui'
import { SearchOutline } from '@vicons/ionicons5'
import { useAppStore } from '@/stores/app'

export interface PublishItem {
  id: number
  name: string
  code: string
  mobileEnabled: number
}

const props = defineProps<{
  show: boolean
  title?: string
  items: PublishItem[]
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
  'toggle': [id: number, enabled: boolean]
  'batch-publish': []
  'batch-unpublish': []
}>()

const appStore = useAppStore()
const isMobileView = computed(() => appStore.isMobileView)

const searchKeyword = ref('')

const filteredItems = computed(() => {
  if (!searchKeyword.value) return props.items
  const kw = searchKeyword.value.toLowerCase()
  return props.items.filter(i =>
    i.name.toLowerCase().includes(kw) || i.code.toLowerCase().includes(kw)
  )
})

const publishedCount = computed(() => props.items.filter(i => i.mobileEnabled === 1).length)
const unpublishedCount = computed(() => props.items.filter(i => i.mobileEnabled !== 1).length)

function handleToggle(item: PublishItem, enabled: boolean) {
  emit('toggle', item.id, enabled)
}

function handleBatchPublish() {
  emit('batch-publish')
}

function handleBatchUnpublish() {
  emit('batch-unpublish')
}
</script>

<style scoped>
.publish-dialog {
  max-height: 55vh;
  overflow-y: auto;
}

.publish-summary {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 10px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.summary-count {
  font-size: 22px;
  font-weight: 700;
}

.summary-count.published { color: #10b981; }
.summary-count.unpublished { color: #94a3b8; }
.summary-count.total { color: #2563eb; }

.summary-label {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

.publish-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.publish-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 8px;
  transition: background 0.15s;
}

.publish-item:hover {
  background: #f8fafc;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-code {
  font-size: 11px;
  color: #94a3b8;
}

.empty-tip {
  text-align: center;
  padding: 24px;
  color: #94a3b8;
  font-size: 13px;
}

.publish-footer {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

</style>

<style>
/* MobilePublishDialog 深色模式（非 scoped） */
html.dark .publish-summary { background: #1e293b !important; }
html.dark .publish-item:hover { background: #334155 !important; }
html.dark .item-name { color: #e2e8f0 !important; }
</style>

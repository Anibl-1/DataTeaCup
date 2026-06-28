<template>
  <div class="m-card-list">
    <div v-if="loading" class="m-card-list-loading">
      <div v-for="i in 3" :key="i" class="m-card-item m-card-skeleton">
        <div class="m-card-item-header">
          <n-skeleton text style="width: 50%" :sharp="false" />
          <n-skeleton text style="width: 60px" size="small" :sharp="false" />
        </div>
        <div class="m-card-item-body">
          <n-skeleton text style="width: 80%" size="small" :sharp="false" />
          <n-skeleton text style="width: 60%" size="small" :sharp="false" />
        </div>
      </div>
    </div>

    <template v-else-if="items.length > 0">
      <div
        v-for="(item, index) in items"
        :key="itemKey ? item[itemKey] : index"
        class="m-card-item"
        @click="$emit('itemClick', item)"
      >
        <div class="m-card-item-header">
          <span class="m-card-item-title">
            <slot name="title" :item="item">{{ item.name || item.title || '—' }}</slot>
          </span>
          <span class="m-card-item-status">
            <slot name="status" :item="item"></slot>
          </span>
        </div>
        <div class="m-card-item-body">
          <slot name="body" :item="item">
            <div v-for="(field, fi) in fields" :key="fi" class="m-card-item-row">
              <span class="label">{{ field.label }}:</span>
              <span>{{ getFieldValue(item, field.key) }}</span>
            </div>
          </slot>
        </div>
        <div class="m-card-item-footer">
          <span class="m-card-item-time">
            <slot name="footer-left" :item="item">{{ item.createTime || item.updateTime || '' }}</slot>
          </span>
          <div class="m-card-item-actions" @click.stop>
            <slot name="actions" :item="item"></slot>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="m-empty">
      <div class="m-empty-icon">
        <n-icon :size="48"><InboxOutline /></n-icon>
      </div>
      <div class="m-empty-text">{{ emptyText || '暂无数据' }}</div>
    </div>

    <!-- 加载更多 -->
    <div v-if="!loading && items.length > 0 && hasMore" class="m-load-more" @click="$emit('loadMore')">
      <span v-if="loadingMore">加载中...</span>
      <span v-else>点击加载更多</span>
    </div>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { NIcon, NSkeleton } from 'naive-ui'
import { InboxOutline } from '@vicons/ionicons5'

interface FieldDef {
  label: string
  key: string
}

defineProps<{
  items: any[]
  fields?: FieldDef[]
  itemKey?: string
  loading?: boolean
  loadingMore?: boolean
  hasMore?: boolean
  emptyText?: string
}>()

defineEmits<{
  (e: 'itemClick', item: any): void
  (e: 'loadMore'): void
}>()

const getFieldValue = (item: any, key: string) => {
  const keys = key.split('.')
  let val = item
  for (const k of keys) {
    val = val?.[k]
  }
  return val ?? '—'
}
</script>

<style scoped>
.m-card-list-loading {
  display: flex;
  flex-direction: column;
  gap: var(--mobile-card-gap, 10px);
}

.m-card-skeleton {
  pointer-events: none;
}

/* 深色模式 */

/* 深色模式骨架屏 */
</style>

<style>
/* MobileListCard 深色模式（非 scoped） */
html.dark .m-card-item {
  background: #1e293b !important;
  border-color: #334155 !important;
}
html.dark .m-card-item:active {
  background: #253449 !important;
}
html.dark .m-card-item-title {
  color: #f1f5f9 !important;
}
html.dark .m-card-item-row {
  color: #94a3b8 !important;
}
html.dark .m-card-item-row .label {
  color: #64748b !important;
}
html.dark .m-card-item-footer {
  border-top-color: #334155 !important;
}
html.dark .m-card-item-time {
  color: #64748b !important;
}
html.dark .m-empty {
  color: #64748b !important;
}
html.dark .m-empty-icon {
  color: #475569 !important;
}
html.dark .m-load-more {
  color: #64748b !important;
}
html.dark .m-card-skeleton {
  background: #1e293b !important;
  border-color: #334155 !important;
}
</style>

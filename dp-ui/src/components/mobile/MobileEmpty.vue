<template>
  <div class="m-empty" :class="`m-empty--${type}`">
    <div class="m-empty-illustration">
      <!-- 空数据插画 -->
      <svg v-if="type === 'data'" width="120" height="100" viewBox="0 0 120 100" fill="none">
        <rect x="20" y="30" width="80" height="50" rx="8" fill="currentColor" opacity="0.06"/>
        <rect x="28" y="42" width="36" height="4" rx="2" fill="currentColor" opacity="0.15"/>
        <rect x="28" y="52" width="28" height="4" rx="2" fill="currentColor" opacity="0.1"/>
        <rect x="28" y="62" width="44" height="4" rx="2" fill="currentColor" opacity="0.08"/>
        <circle cx="86" cy="52" r="12" fill="currentColor" opacity="0.08"/>
        <path d="M82 52L85 55L91 49" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" opacity="0.2"/>
      </svg>
      <!-- 搜索无结果 -->
      <svg v-else-if="type === 'search'" width="120" height="100" viewBox="0 0 120 100" fill="none">
        <circle cx="54" cy="46" r="22" stroke="currentColor" stroke-width="3" opacity="0.12"/>
        <line x1="70" y1="62" x2="88" y2="80" stroke="currentColor" stroke-width="3" stroke-linecap="round" opacity="0.12"/>
        <line x1="46" y1="40" x2="62" y2="52" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.15"/>
        <line x1="62" y1="40" x2="46" y2="52" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.15"/>
      </svg>
      <!-- 错误 -->
      <svg v-else-if="type === 'error'" width="120" height="100" viewBox="0 0 120 100" fill="none">
        <circle cx="60" cy="50" r="28" stroke="currentColor" stroke-width="2.5" opacity="0.1"/>
        <path d="M60 36V56" stroke="currentColor" stroke-width="3" stroke-linecap="round" opacity="0.2"/>
        <circle cx="60" cy="64" r="2.5" fill="currentColor" opacity="0.2"/>
      </svg>
      <!-- 默认/通用 -->
      <svg v-else width="120" height="100" viewBox="0 0 120 100" fill="none">
        <rect x="30" y="25" width="60" height="50" rx="10" stroke="currentColor" stroke-width="2" opacity="0.08"/>
        <circle cx="60" cy="45" r="8" stroke="currentColor" stroke-width="2" opacity="0.12"/>
        <path d="M56 45L59 48L65 42" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" opacity="0.15"/>
        <rect x="45" y="60" width="30" height="4" rx="2" fill="currentColor" opacity="0.08"/>
      </svg>
    </div>
    <div class="m-empty-title">{{ title || defaultTitle }}</div>
    <div v-if="description" class="m-empty-desc">{{ description }}</div>
    <div v-if="$slots['action']" class="m-empty-action">
      <slot name="action"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  type?: 'data' | 'search' | 'error' | 'default'
  title?: string
  description?: string
}>()

const defaultTitle = computed(() => {
  const map: Record<string, string> = {
    data: '暂无数据',
    search: '未找到结果',
    error: '加载失败',
    default: '暂无内容'
  }
  return map[props.type || 'default'] || '暂无内容'
})
</script>

<style scoped>
.m-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  animation: emptyFadeIn 0.4s ease;
}

@keyframes emptyFadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.m-empty-illustration {
  color: #94a3b8;
  margin-bottom: 16px;
}

.m-empty-title {
  font-size: 16px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 4px;
}

.m-empty-desc {
  font-size: 13px;
  color: #94a3b8;
  text-align: center;
  max-width: 260px;
  line-height: 1.5;
}

.m-empty-action {
  margin-top: 20px;
}

</style>

<style>
/* MobileEmpty 深色模式（非 scoped） */
html.dark .m-empty-illustration { color: #475569 !important; }
html.dark .m-empty-title { color: #94a3b8 !important; }
html.dark .m-empty-desc { color: #64748b !important; }
</style>

<template>
  <div
    class="widget-wrapper"
    :class="{
      'widget-selected': selected,
      'widget-loading': loading,
      'widget-error': hasError
    }"
    @click="handleClick"
  >
    <!-- Widget Header -->
    <div v-if="showHeader" class="widget-header">
      <div class="widget-title">
        <slot name="title">{{ title }}</slot>
      </div>
      <div class="widget-actions">
        <slot name="actions">
          <n-button
            v-if="showRefresh"
            text
            size="tiny"
            @click.stop="handleRefresh"
          >
            <template #icon>
              <n-icon :class="{ 'spin-animation': refreshing }">
                <RefreshOutline />
              </n-icon>
            </template>
          </n-button>
          <n-button
            v-if="showRemove && !readonly"
            text
            size="tiny"
            @click.stop="handleRemove"
          >
            <template #icon>
              <n-icon><CloseOutline /></n-icon>
            </template>
          </n-button>
        </slot>
      </div>
    </div>

    <!-- Widget Content -->
    <div ref="contentRef" class="widget-content">
      <slot></slot>
    </div>

    <!-- Loading Overlay -->
    <div v-if="loading" class="widget-overlay loading-overlay">
      <n-spin size="medium" />
    </div>

    <!-- Error Overlay -->
    <div v-if="hasError && !loading" class="widget-overlay error-overlay">
      <n-result status="error" size="small" :description="errorMessage">
        <template #footer>
          <n-button size="small" @click.stop="handleRefresh">重试</n-button>
        </template>
      </n-result>
    </div>

    <!-- Resize Handle (for design mode) -->
    <div v-if="showResizeHandle && !readonly" class="resize-handle">
      <n-icon size="12"><ResizeOutline /></n-icon>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { NButton, NIcon, NSpin, NResult } from 'naive-ui'
import { RefreshOutline, CloseOutline, ResizeOutline } from '@vicons/ionicons5'

const props = withDefaults(defineProps<{
  title?: string
  selected?: boolean
  loading?: boolean
  error?: string | null
  readonly?: boolean
  showHeader?: boolean
  showRefresh?: boolean
  showRemove?: boolean
  showResizeHandle?: boolean
}>(), {
  title: '',
  selected: false,
  loading: false,
  error: null,
  readonly: false,
  showHeader: true,
  showRefresh: true,
  showRemove: true,
  showResizeHandle: false
})

const emit = defineEmits<{
  (e: 'click'): void
  (e: 'refresh'): void
  (e: 'remove'): void
  (e: 'resize', size: { width: number; height: number }): void
}>()

const contentRef = ref<HTMLElement | null>(null)
const refreshing = ref(false)

const hasError = computed(() => !!props.error)
const errorMessage = computed(() => props.error || '加载失败')

// ResizeObserver for content size changes
let resizeObserver: ResizeObserver | null = null

const handleClick = () => {
  emit('click')
}

const handleRefresh = async () => {
  refreshing.value = true
  emit('refresh')
  // Animation duration
  setTimeout(() => {
    refreshing.value = false
  }, 1000)
}

const handleRemove = () => {
  emit('remove')
}

const setupResizeObserver = () => {
  if (!contentRef.value) return
  
  resizeObserver = new ResizeObserver((entries) => {
    for (const entry of entries) {
      const { width, height } = entry.contentRect
      emit('resize', { width, height })
    }
  })
  
  resizeObserver.observe(contentRef.value)
}

onMounted(() => {
  setupResizeObserver()
})

onBeforeUnmount(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
})

defineExpose({
  contentRef
})
</script>

<style scoped>
.widget-wrapper {
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 6px;
  overflow: hidden;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.widget-wrapper.widget-selected {
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.3);
}

.widget-wrapper.widget-loading .widget-content {
  opacity: 0.5;
  pointer-events: none;
}

.widget-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  min-height: 36px;
}

.widget-title {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.widget-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.widget-content {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.widget-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.loading-overlay {
  background: rgba(255, 255, 255, 0.8);
}

.error-overlay {
  background: rgba(255, 255, 255, 0.95);
}

.resize-handle {
  position: absolute;
  right: 2px;
  bottom: 2px;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
  cursor: se-resize;
}

.spin-animation {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>

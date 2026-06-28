<template>
  <Transition name="fade">
    <div v-if="appStore.isLoading" class="global-loading">
      <div class="loading-content">
        <n-spin size="large" />
        <p class="loading-text">{{ appStore.loadingText || '加载中...' }}</p>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { NSpin } from 'naive-ui'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
</script>

<style scoped>
.global-loading {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(2px);
}


.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.loading-text {
  color: #666;
  font-size: 14px;
  margin: 0;
}


.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

<style>
/* GlobalLoading 深色模式（非 scoped） */
html.dark .global-loading {
  background: rgba(20, 20, 20, 0.8) !important;
}
html.dark .loading-text {
  color: #aaa !important;
}
</style>

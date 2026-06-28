<template>
  <div class="mobile-header">
    <div class="mobile-header-inner">
      <div class="mobile-header-left">
        <div v-if="showBack" class="mobile-header-back" @click="handleBack">
          <n-icon :size="22"><ChevronBackOutline /></n-icon>
        </div>
        <div v-else class="mobile-header-menu" @click="$emit('toggleMenu')">
          <n-icon :size="20"><MenuOutline /></n-icon>
        </div>
      </div>
      <div class="mobile-header-title">
        <span>{{ title }}</span>
      </div>
      <div class="mobile-header-right">
        <slot name="right">
          <div class="mobile-header-action" @click="$emit('action')">
            <n-icon v-if="actionIcon" :size="20"><component :is="actionIcon" /></n-icon>
          </div>
        </slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { type Component } from 'vue'
import { useRouter } from 'vue-router'
import { NIcon } from 'naive-ui'
import { ChevronBackOutline, MenuOutline } from '@vicons/ionicons5'

const props = defineProps<{
  title?: string
  showBack?: boolean
  actionIcon?: Component
}>()

defineEmits<{
  (e: 'toggleMenu'): void
  (e: 'action'): void
}>()

const router = useRouter()

const handleBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/m/pages')
  }
}
</script>

<style scoped>
.mobile-header {
  position: sticky;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding-top: env(safe-area-inset-top, 0px);
}

.mobile-header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--mobile-header-height, 48px);
  padding: 0 4px;
}

.mobile-header-left,
.mobile-header-right {
  display: flex;
  align-items: center;
  width: 48px;
  flex-shrink: 0;
}

.mobile-header-right {
  justify-content: flex-end;
}

.mobile-header-back,
.mobile-header-menu,
.mobile-header-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  cursor: pointer;
  color: var(--text-primary, #1e293b);
  -webkit-tap-highlight-color: transparent;
  transition: all 0.2s ease;
}

.mobile-header-back:active,
.mobile-header-menu:active,
.mobile-header-action:active {
  background: rgba(0, 0, 0, 0.06);
  transform: scale(0.92);
}

.mobile-header-title {
  flex: 1;
  min-width: 0;
  text-align: center;
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary, #1e293b);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 0 8px;
}

.mobile-header-title span {
  display: block;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 深色模式 */

</style>

<style>
/* MobileHeader 深色模式（非 scoped） */
html.dark .mobile-header {
  background: rgba(17, 24, 39, 0.95) !important;
  border-bottom-color: rgba(255, 255, 255, 0.06) !important;
}
html.dark .mobile-header-back,
html.dark .mobile-header-back,
html.dark .mobile-header-menu,
html.dark .mobile-header-back,
html.dark .mobile-header-menu,
html.dark .mobile-header-action {
  color: #e2e8f0 !important;
}
html.dark .mobile-header-back:active,
html.dark .mobile-header-back:active,
html.dark .mobile-header-menu:active,
html.dark .mobile-header-back:active,
html.dark .mobile-header-menu:active,
html.dark .mobile-header-action:active {
  background: rgba(255, 255, 255, 0.08) !important;
}
html.dark .mobile-header-title {
  color: #f1f5f9 !important;
}
</style>

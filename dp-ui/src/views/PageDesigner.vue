<template>
  <DesktopOnlyTip v-if="isMobileView" title="页面设计器" desc="页面设计器需要更大的屏幕空间进行拖拽布局，请在电脑端打开。" />
  <ErrorBoundary v-else>
    <PageDesigner />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import PageDesigner from '@/components/PageDesigner/PageDesigner.vue'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import DesktopOnlyTip from '@/components/mobile/DesktopOnlyTip.vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const isMobileView = computed(() => appStore.isMobileView)
</script>

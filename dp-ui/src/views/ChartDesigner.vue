<template>
  <DesktopOnlyTip v-if="isMobileView" title="图表设计器" desc="图表设计器需要更大的屏幕空间进行可视化操作，请在电脑端打开。" />
  <ChartDesigner v-else />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ChartDesigner from '@/components/ChartDesigner/ChartDesigner.vue'
import DesktopOnlyTip from '@/components/mobile/DesktopOnlyTip.vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const isMobileView = computed(() => appStore.isMobileView)
</script>

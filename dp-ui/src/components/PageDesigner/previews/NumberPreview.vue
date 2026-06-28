<template>
  <div class="preview-input">
    <n-input-number size="small" disabled :placeholder="comp.placeholder || '数字输入'" style="width: 100%" />
  </div>
</template>

<script setup lang="ts">
import { NInputNumber } from 'naive-ui'
import type { QueryComponent } from '@/types/pageParameter'

defineProps<{ comp: QueryComponent }>()
</script>

<style scoped>
.preview-input {
  pointer-events: none;
}
</style>

<template>
  <div class="preview-input">
    <n-input size="small" disabled :placeholder="comp.placeholder || '文本输入'" />
  </div>
</template>

<script setup lang="ts">
import { NInput } from 'naive-ui'
import type { QueryComponent } from '@/types/pageParameter'

defineProps<{ comp: QueryComponent }>()
</script>

<style scoped>
.preview-input {
  pointer-events: none;
}
</style>

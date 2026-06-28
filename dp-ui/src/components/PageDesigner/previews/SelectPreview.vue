<template>
  <div class="preview-input">
    <n-select 
      size="small" 
      disabled 
      :placeholder="comp.placeholder || '请选择'"
      :multiple="comp.type === 'multiSelect'"
    />
  </div>
</template>

<script setup lang="ts">
import { NSelect } from 'naive-ui'
import type { QueryComponent } from '@/types/pageParameter'

defineProps<{ comp: QueryComponent }>()
</script>

<style scoped>
.preview-input {
  pointer-events: none;
}
</style>

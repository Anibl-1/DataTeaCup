<template>
  <div class="preview-input">
    <n-date-picker 
      v-if="comp.type === 'dateRange'"
      size="small" 
      disabled 
      type="daterange"
      start-placeholder="开始日期"
      end-placeholder="结束日期"
      style="width: 100%"
    />
    <n-date-picker 
      v-else
      size="small" 
      disabled 
      type="date"
      :placeholder="comp.placeholder || '选择日期'"
      style="width: 100%"
    />
  </div>
</template>

<script setup lang="ts">
import { NDatePicker } from 'naive-ui'
import type { QueryComponent } from '@/types/pageParameter'

defineProps<{ comp: QueryComponent }>()
</script>

<style scoped>
.preview-input {
  pointer-events: none;
}
</style>

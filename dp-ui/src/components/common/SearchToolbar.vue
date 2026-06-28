<template>
  <n-space align="center">
    <n-input
      v-model:value="keyword"
      :placeholder="placeholder"
      clearable
      :style="{ width: inputWidth }"
      @keydown.enter="handleSearch"
      @clear="handleSearch"
    >
      <template #prefix><n-icon><SearchOutline /></n-icon></template>
    </n-input>
    <n-select
      v-if="statusOptions.length > 0"
      v-model:value="statusValue"
      :options="statusOptions"
      :placeholder="statusPlaceholder"
      clearable
      style="width: 120px"
      @update:value="handleSearch"
    />
    <n-button @click="handleSearch">搜索</n-button>
    <slot name="actions" />
  </n-space>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { NInput, NButton, NSpace, NIcon, NSelect } from 'naive-ui'
import type { SelectOption } from 'naive-ui'
import { SearchOutline } from '@vicons/ionicons5'

const props = withDefaults(defineProps<{
  /** 搜索框占位文本 */
  placeholder?: string
  /** 搜索框宽度 */
  inputWidth?: string
  /** 状态筛选选项 */
  statusOptions?: SelectOption[]
  /** 状态筛选占位文本 */
  statusPlaceholder?: string
}>(), {
  placeholder: '搜索...',
  inputWidth: '200px',
  statusOptions: () => [],
  statusPlaceholder: '状态'
})

const emit = defineEmits<{
  search: [keyword: string, status: number | null]
}>()

const keyword = ref('')
const statusValue = ref<number | null>(null)

function handleSearch() {
  emit('search', keyword.value, statusValue.value)
}
</script>

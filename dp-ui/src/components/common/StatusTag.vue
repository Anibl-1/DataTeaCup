<template>
  <n-tag :type="tagType" :size="size">
    <template v-if="tagIcon" #icon>
      <n-icon :component="tagIcon" />
    </template>
    {{ tagLabel }}
  </n-tag>
</template>

<script setup lang="ts">
import { computed, type Component } from 'vue'
import { NTag, NIcon } from 'naive-ui'

export interface StatusConfig {
  label: string
  type: 'success' | 'warning' | 'error' | 'info' | 'default'
  icon?: Component
}

export interface StatusTagProps {
  status: string | number
  statusMap?: Record<string | number, StatusConfig>
  size?: 'small' | 'medium' | 'large'
}

const props = withDefaults(defineProps<StatusTagProps>(), {
  statusMap: () => ({}),
  size: 'small'
})

const config = computed<StatusConfig | undefined>(
  () => props.statusMap[props.status]
)

const tagLabel = computed(() => config.value?.label ?? String(props.status))

const tagType = computed(() => config.value?.type ?? 'default')

const tagIcon = computed(() => config.value?.icon)
</script>

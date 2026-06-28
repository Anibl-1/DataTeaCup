<template>
  <div class="empty-state">
    <div class="empty-state-icon" :style="iconStyles">
      <n-icon :size="iconSize">
        <component :is="icon" v-if="icon" />
        <FileTrayOutline v-else />
      </n-icon>
    </div>
    <div class="empty-state-text">{{ description }}</div>
    <div v-if="$slots.default" class="empty-state-action">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, type Component } from 'vue'
import { NIcon } from 'naive-ui'
import { FileTrayOutline } from '@vicons/ionicons5'

export interface EmptyStateProps {
  /** Custom icon component, default uses FileTrayOutline */
  icon?: Component
  /** Description text, default "暂无数据" */
  description?: string
  /** Icon size in px, default 64 */
  iconSize?: number
  /** Icon color, default var(--text-tertiary) */
  iconColor?: string
}

const props = withDefaults(defineProps<EmptyStateProps>(), {
  description: '暂无数据',
  iconSize: 64,
  iconColor: ''
})

const iconStyles = computed(() => {
  if (props.iconColor) {
    return { color: props.iconColor }
  }
  return {}
})
</script>

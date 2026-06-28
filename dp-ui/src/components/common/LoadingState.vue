<template>
  <div class="loading-state-wrapper" :style="{ minHeight: loading ? minHeight : undefined }">
    <template v-if="loading">
      <!-- Spinner mode -->
      <div v-if="mode === 'spinner'" class="loading-state-spinner">
        <n-spin size="medium" />
        <div v-if="description" class="loading-state-description">{{ description }}</div>
      </div>

      <!-- Skeleton mode -->
      <div v-else class="loading-state-skeleton">
        <n-skeleton
          v-for="i in skeletonRows"
          :key="i"
          :height="skeletonRowHeight"
          :width="i === skeletonRows ? '60%' : '100%'"
          :sharp="false"
          class="loading-state-skeleton-row"
        />
        <div v-if="description" class="loading-state-description">{{ description }}</div>
      </div>
    </template>

    <slot v-else />
  </div>
</template>

<script setup lang="ts">
import { NSpin, NSkeleton } from 'naive-ui'

export interface LoadingStateProps {
  /** Whether to show loading state */
  loading: boolean
  /** Loading mode, default 'spinner' */
  mode?: 'spinner' | 'skeleton'
  /** Loading hint text */
  description?: string
  /** Number of rows in skeleton mode, default 3 */
  skeletonRows?: number
  /** Minimum height, default '200px' */
  minHeight?: string
}

withDefaults(defineProps<LoadingStateProps>(), {
  mode: 'spinner',
  skeletonRows: 3,
  minHeight: '200px'
})

const skeletonRowHeight = '20px'
</script>

<style scoped>
.loading-state-wrapper {
  width: 100%;
}

.loading-state-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: inherit;
  padding: var(--dp-spacing-xl, 32px) var(--dp-spacing-md, 16px);
}

.loading-state-skeleton {
  padding: var(--dp-spacing-md, 16px);
}

.loading-state-skeleton-row {
  margin-bottom: var(--dp-spacing-sm, 8px);
  border-radius: var(--dp-radius-sm, 4px);
}

.loading-state-description {
  margin-top: var(--dp-spacing-md, 16px);
  font-size: var(--dp-font-md, 14px);
  color: var(--text-tertiary);
  text-align: center;
}
</style>

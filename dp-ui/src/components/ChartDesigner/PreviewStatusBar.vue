<template>
  <div class="preview-status-bar" :class="{ 'preview-status-bar--updating': isUpdating }">
    <div class="status-left">
      <div class="status-indicator" :class="statusClass">
        <n-icon v-if="isUpdating" size="12" class="spin-icon">
          <SyncOutline />
        </n-icon>
        <n-icon v-else-if="isReady" size="12">
          <CheckmarkCircleOutline />
        </n-icon>
        <n-icon v-else size="12">
          <EllipseOutline />
        </n-icon>
      </div>
      <span class="status-text">{{ statusText }}</span>
    </div>
    
    <div class="status-right">
      <n-tooltip v-if="showLatency && updateLatency > 0" trigger="hover">
        <template #trigger>
          <span class="latency-badge" :class="latencyClass">
            {{ updateLatency }}ms
          </span>
        </template>
        上次更新耗时
      </n-tooltip>
      
      <n-tooltip v-if="showUpdateCount && updateCount > 0" trigger="hover">
        <template #trigger>
          <span class="update-count">
            {{ updateCount }} 次更新
          </span>
        </template>
        预览更新次数
      </n-tooltip>
      
      <n-tooltip v-if="lastUpdateTime > 0" trigger="hover">
        <template #trigger>
          <span class="last-update">
            {{ formatTime(lastUpdateTime) }}
          </span>
        </template>
        上次更新时间
      </n-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 预览状态栏组件
 * 显示实时预览的状态信息
 */
import { computed } from 'vue'
import { NIcon, NTooltip } from 'naive-ui'
import { SyncOutline, CheckmarkCircleOutline, EllipseOutline } from '@vicons/ionicons5'

interface Props {
  /** 是否正在更新 */
  isUpdating?: boolean
  /** 是否就绪 */
  isReady?: boolean
  /** 更新延迟（毫秒） */
  updateLatency?: number
  /** 更新次数 */
  updateCount?: number
  /** 上次更新时间 */
  lastUpdateTime?: number
  /** 是否显示延迟 */
  showLatency?: boolean
  /** 是否显示更新次数 */
  showUpdateCount?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isUpdating: false,
  isReady: false,
  updateLatency: 0,
  updateCount: 0,
  lastUpdateTime: 0,
  showLatency: true,
  showUpdateCount: false
})

const statusClass = computed(() => ({
  'status-indicator--updating': props.isUpdating,
  'status-indicator--ready': props.isReady && !props.isUpdating,
  'status-indicator--idle': !props.isReady && !props.isUpdating
}))

const statusText = computed(() => {
  if (props.isUpdating) return '正在更新预览...'
  if (props.isReady) return '预览已就绪'
  return '等待配置'
})

const latencyClass = computed(() => {
  if (props.updateLatency < 100) return 'latency-badge--fast'
  if (props.updateLatency < 300) return 'latency-badge--normal'
  return 'latency-badge--slow'
})

const formatTime = (timestamp: number): string => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}
</script>

<style scoped>
.preview-status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  font-size: 11px;
  color: #64748b;
  transition: background-color 0.2s;
}

.preview-status-bar--updating {
  background: #eff6ff;
}

.status-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  transition: all 0.2s;
}

.status-indicator--updating {
  color: #3b82f6;
}

.status-indicator--ready {
  color: #22c55e;
}

.status-indicator--idle {
  color: #94a3b8;
}

.spin-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.status-text {
  font-weight: 500;
}

.status-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.latency-badge {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  font-family: 'Fira Code', Consolas, monospace;
}

.latency-badge--fast {
  background: #dcfce7;
  color: #16a34a;
}

.latency-badge--normal {
  background: #fef3c7;
  color: #d97706;
}

.latency-badge--slow {
  background: #fee2e2;
  color: #dc2626;
}

.update-count {
  color: #94a3b8;
}

.last-update {
  color: #94a3b8;
  font-family: 'Fira Code', Consolas, monospace;
}
</style>

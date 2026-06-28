<template>
  <n-tooltip
    :trigger="trigger"
    :placement="placement"
    :show-arrow="showArrow"
    :delay="delay"
  >
    <template #trigger>
      <slot>
        <n-icon
          v-if="showIcon"
          :size="iconSize"
          class="config-tooltip-icon"
          :class="{ 'config-tooltip-icon--inline': inline }"
        >
          <HelpCircleOutline />
        </n-icon>
      </slot>
    </template>
    <div class="config-tooltip-content" :style="{ maxWidth: maxWidth + 'px' }">
      <div v-if="title" class="config-tooltip-title">{{ title }}</div>
      <div class="config-tooltip-text">{{ content }}</div>
      <div v-if="example" class="config-tooltip-example">
        <span class="example-label">示例：</span>
        <span class="example-text">{{ example }}</span>
      </div>
    </div>
  </n-tooltip>
</template>

<script setup lang="ts">
/**
 * 配置项提示组件
 * 验证需求: 3.6 - 悬停显示配置项使用提示
 */
import { NTooltip, NIcon } from 'naive-ui'
import { HelpCircleOutline } from '@vicons/ionicons5'

interface Props {
  /** 提示内容 */
  content: string
  /** 提示标题 */
  title?: string
  /** 示例文本 */
  example?: string
  /** 触发方式 */
  trigger?: 'hover' | 'click' | 'focus' | 'manual'
  /** 显示位置 */
  placement?: 'top' | 'bottom' | 'left' | 'right' | 'top-start' | 'top-end' | 'bottom-start' | 'bottom-end'
  /** 是否显示箭头 */
  showArrow?: boolean
  /** 显示延迟（毫秒） */
  delay?: number
  /** 是否显示图标 */
  showIcon?: boolean
  /** 图标大小 */
  iconSize?: number
  /** 是否内联显示 */
  inline?: boolean
  /** 最大宽度 */
  maxWidth?: number
}

withDefaults(defineProps<Props>(), {
  title: '',
  example: '',
  trigger: 'hover',
  placement: 'top',
  showArrow: true,
  delay: 100,
  showIcon: true,
  iconSize: 14,
  inline: true,
  maxWidth: 280
})
</script>

<style scoped>
.config-tooltip-icon {
  color: #94a3b8;
  cursor: help;
  transition: color 0.2s;
}

.config-tooltip-icon:hover {
  color: #3b82f6;
}

.config-tooltip-icon--inline {
  margin-left: 4px;
  vertical-align: middle;
}

.config-tooltip-content {
  font-size: 12px;
  line-height: 1.5;
}

.config-tooltip-title {
  font-weight: 600;
  color: #f8fafc;
  margin-bottom: 4px;
}

.config-tooltip-text {
  color: #e2e8f0;
}

.config-tooltip-example {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.example-label {
  color: #94a3b8;
}

.example-text {
  color: #60a5fa;
  font-family: 'Fira Code', Consolas, monospace;
}
</style>

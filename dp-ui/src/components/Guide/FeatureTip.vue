<template>
  <n-tooltip
    v-if="shouldShow"
    :trigger="trigger"
    :placement="placement"
    :show-arrow="true"
    :delay="100"
  >
    <template #trigger>
      <slot>
        <n-icon
          v-if="showIcon"
          :size="iconSize"
          class="feature-tip-icon"
          :class="[
            `feature-tip-icon--${type}`,
            { 'feature-tip-icon--inline': inline }
          ]"
          :aria-label="ariaLabel"
          role="button"
          tabindex="0"
        >
          <component :is="iconComponent" />
        </n-icon>
      </slot>
    </template>
    <div 
      class="feature-tip-content" 
      :class="[`feature-tip-content--${type}`]"
      :style="{ maxWidth: maxWidth + 'px' }"
    >
      <!-- 标题栏 -->
      <div v-if="title || dismissible" class="feature-tip-header">
        <div v-if="title" class="feature-tip-title">
          <n-icon :size="14" class="feature-tip-title-icon">
            <component :is="iconComponent" />
          </n-icon>
          <span>{{ title }}</span>
        </div>
        <button
          v-if="dismissible && tipId"
          class="feature-tip-dismiss"
          :aria-label="dismissAriaLabel"
          @click.stop="handleDismiss"
        >
          <n-icon :size="12"><CloseOutline /></n-icon>
        </button>
      </div>
      
      <!-- 内容 -->
      <div class="feature-tip-body">
        {{ content }}
      </div>
      
      <!-- 示例 -->
      <div v-if="example" class="feature-tip-example">
        <span class="feature-tip-example-label">示例：</span>
        <span class="feature-tip-example-text">{{ example }}</span>
      </div>
      
      <!-- 链接 -->
      <div v-if="link" class="feature-tip-link">
        <a 
          :href="link.url" 
          target="_blank" 
          rel="noopener noreferrer"
          class="feature-tip-link-anchor"
        >
          {{ link.text }}
          <n-icon :size="12"><OpenOutline /></n-icon>
        </a>
      </div>
    </div>
  </n-tooltip>
</template>

<script setup lang="ts">
/**
 * 功能提示组件
 * Feature Tip Component
 * 
 * 为复杂功能提供上下文相关的使用提示，支持不同类型（info、warning、tip）
 * 和可关闭功能（带 localStorage 持久化）。
 * 
 * 需求 19.2: THE DataTeaCup SHALL 为复杂功能提供上下文相关的使用提示
 * 
 * @example
 * ```vue
 * <FeatureTip
 *   tip-id="sql-editor-tip"
 *   title="SQL 编辑器"
 *   content="支持语法高亮和自动补全功能"
 *   type="info"
 *   dismissible
 * />
 * ```
 */
import { computed, type Component } from 'vue'
import { NTooltip, NIcon } from 'naive-ui'
import { 
  InformationCircleOutline, 
  WarningOutline, 
  BulbOutline,
  CloseOutline,
  OpenOutline
} from '@vicons/ionicons5'
import { useFeatureTip } from './FeatureTipService'
import type { TipType, TipPlacement } from './featureTipTypes'

// ==================== Props ====================

interface Props {
  /** 提示 ID（用于持久化关闭状态） */
  tipId?: string
  /** 提示标题 */
  title?: string
  /** 提示内容 */
  content: string
  /** 提示类型 */
  type?: TipType
  /** 是否可关闭 */
  dismissible?: boolean
  /** 显示位置 */
  placement?: TipPlacement
  /** 示例文本 */
  example?: string
  /** 相关链接 */
  link?: {
    text: string
    url: string
  }
  /** 是否显示图标 */
  showIcon?: boolean
  /** 图标大小 */
  iconSize?: number
  /** 触发方式 */
  trigger?: 'hover' | 'click' | 'focus' | 'manual'
  /** 最大宽度 */
  maxWidth?: number
  /** 是否内联显示 */
  inline?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  tipId: undefined,
  title: '',
  type: 'info',
  dismissible: false,
  placement: 'top',
  example: '',
  link: undefined,
  showIcon: true,
  iconSize: 14,
  trigger: 'hover',
  maxWidth: 320,
  inline: true
})

// ==================== Emits ====================

const emit = defineEmits<{
  (e: 'dismiss', tipId: string): void
}>()

// ==================== Composables ====================

const { isDismissed, dismiss } = useFeatureTip()

// ==================== Computed ====================

/**
 * 是否应该显示提示
 */
const shouldShow = computed(() => {
  // 如果有 tipId 且已关闭，则不显示
  if (props.tipId && isDismissed(props.tipId)) {
    return false
  }
  return true
})

/**
 * 根据类型获取图标组件
 */
const iconComponent = computed<Component>(() => {
  switch (props.type) {
    case 'warning':
      return WarningOutline
    case 'tip':
      return BulbOutline
    case 'info':
    default:
      return InformationCircleOutline
  }
})

/**
 * 无障碍标签
 */
const ariaLabel = computed(() => {
  const typeLabels: Record<TipType, string> = {
    info: '信息提示',
    warning: '警告提示',
    tip: '使用技巧'
  }
  return `${typeLabels[props.type]}：${props.title || props.content}`
})

/**
 * 关闭按钮无障碍标签
 */
const dismissAriaLabel = computed(() => {
  return `关闭提示：${props.title || props.content}`
})

// ==================== Methods ====================

/**
 * 处理关闭提示
 */
function handleDismiss(): void {
  if (props.tipId) {
    dismiss(props.tipId)
    emit('dismiss', props.tipId)
  }
}
</script>

<style scoped>
/* 图标样式 */
.feature-tip-icon {
  cursor: help;
  transition: color 0.2s, transform 0.2s;
}

.feature-tip-icon:hover {
  transform: scale(1.1);
}

.feature-tip-icon:focus {
  outline: 2px solid var(--primary-color, #1890ff);
  outline-offset: 2px;
  border-radius: 50%;
}

.feature-tip-icon--inline {
  margin-left: 4px;
  vertical-align: middle;
}

/* 类型颜色 */
.feature-tip-icon--info {
  color: #3b82f6;
}

.feature-tip-icon--info:hover {
  color: #2563eb;
}

.feature-tip-icon--warning {
  color: #f59e0b;
}

.feature-tip-icon--warning:hover {
  color: #d97706;
}

.feature-tip-icon--tip {
  color: #10b981;
}

.feature-tip-icon--tip:hover {
  color: #059669;
}

/* 内容容器 */
.feature-tip-content {
  font-size: 12px;
  line-height: 1.6;
}

/* 头部 */
.feature-tip-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 6px;
}

.feature-tip-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
  color: #f8fafc;
}

.feature-tip-title-icon {
  flex-shrink: 0;
}

/* 类型标题颜色 */
.feature-tip-content--info .feature-tip-title {
  color: #93c5fd;
}

.feature-tip-content--warning .feature-tip-title {
  color: #fcd34d;
}

.feature-tip-content--tip .feature-tip-title {
  color: #6ee7b7;
}

/* 关闭按钮 */
.feature-tip-dismiss {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  padding: 0;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: #94a3b8;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
  flex-shrink: 0;
  margin-left: 8px;
}

.feature-tip-dismiss:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #f8fafc;
}

.feature-tip-dismiss:focus {
  outline: 2px solid var(--primary-color, #1890ff);
  outline-offset: 1px;
}

/* 内容主体 */
.feature-tip-body {
  color: #e2e8f0;
}

/* 示例 */
.feature-tip-example {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.feature-tip-example-label {
  color: #94a3b8;
}

.feature-tip-example-text {
  color: #60a5fa;
  font-family: 'Fira Code', Consolas, monospace;
}

/* 链接 */
.feature-tip-link {
  margin-top: 8px;
}

.feature-tip-link-anchor {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #60a5fa;
  text-decoration: none;
  transition: color 0.2s;
}

.feature-tip-link-anchor:hover {
  color: #93c5fd;
  text-decoration: underline;
}

.feature-tip-link-anchor:focus {
  outline: 2px solid var(--primary-color, #1890ff);
  outline-offset: 2px;
  border-radius: 2px;
}
</style>


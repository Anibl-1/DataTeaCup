<template>
  <div v-if="hasError" class="error-boundary">
    <div class="error-content">
      <n-icon size="64" color="#ff6b6b">
        <AlertCircleOutline />
      </n-icon>
      <h2>页面出现错误</h2>
      <p class="error-message">{{ errorMessage }}</p>
      <div v-if="traceId" class="error-trace-id">
        <span class="trace-label">错误追踪ID：</span>
        <n-tag type="info" size="small" :bordered="false">
          <span class="trace-value">{{ traceId }}</span>
          <template #icon>
            <n-icon :component="CopyOutline" style="cursor: pointer;" @click.stop="copyTraceId" />
          </template>
        </n-tag>
        <n-tooltip trigger="hover">
          <template #trigger>
            <n-icon size="14" color="#909399" style="margin-left: 4px; cursor: help;">
              <InformationCircleOutline />
            </n-icon>
          </template>
          请在反馈问题时提供此ID，便于技术人员排查
        </n-tooltip>
      </div>
      <div class="error-actions">
        <n-button type="primary" @click="handleRetry">
          <template #icon><n-icon><RefreshOutline /></n-icon></template>
          重试
        </n-button>
        <n-button @click="handleGoHome">
          <template #icon><n-icon><HomeOutline /></n-icon></template>
          返回首页
        </n-button>
      </div>
      <n-collapse v-if="errorStack" class="error-details">
        <n-collapse-item title="错误详情" name="details">
          <pre>{{ errorStack }}</pre>
        </n-collapse-item>
      </n-collapse>
    </div>
  </div>
  <div v-else class="error-boundary-content">
    <slot />
  </div>
</template>

<script setup lang="ts">
import { ref, onErrorCaptured, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NIcon, NButton, NCollapse, NCollapseItem, NTag, NTooltip, useMessage } from 'naive-ui'
import { AlertCircleOutline, RefreshOutline, HomeOutline, CopyOutline, InformationCircleOutline } from '@vicons/ionicons5'
import { logger } from '@/utils/logger'
import { generateTraceId } from '@/utils/errorHandler'
import { errorReporter } from '@/utils/errorReporter'

const router = useRouter()
const message = useMessage()
const hasError = ref(false)
const errorMessage = ref('')
const errorStack = ref('')
const traceId = ref('')

onErrorCaptured((err: Error, instance, info) => {
  hasError.value = true
  errorMessage.value = err.message || '未知错误'
  errorStack.value = err.stack || ''
  traceId.value = generateTraceId()
  
  logger.error('组件错误', { 
    error: err, 
    info, 
    component: instance?.$options?.name,
    traceId: traceId.value
  })
  
  // 上报错误到服务端
  errorReporter.captureError(err, {
    traceId: traceId.value,
    componentName: instance?.$options?.name,
    lifecycleHook: info
  })
  
  // 返回 false 阻止错误继续传播
  return false
})

// 路由变化时自动重置错误状态，防止一个页面的错误影响其他页面
const route = useRoute()
watch(() => route.fullPath, () => {
  if (hasError.value) {
    hasError.value = false
    errorMessage.value = ''
    errorStack.value = ''
    traceId.value = ''
  }
})

const handleRetry = () => {
  hasError.value = false
  errorMessage.value = ''
  errorStack.value = ''
  traceId.value = ''
}

const handleGoHome = () => {
  hasError.value = false
  router.push('/dashboard')
}

const copyTraceId = async () => {
  if (traceId.value) {
    try {
      await navigator.clipboard.writeText(traceId.value)
      message.success('追踪ID已复制到剪贴板')
    } catch {
      message.error('复制失败，请手动复制')
    }
  }
}
</script>

<style scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 40px;
}

.error-content {
  text-align: center;
  max-width: 500px;
}

.error-content h2 {
  margin: 16px 0 8px;
  color: #1e293b;
  font-size: 20px;
}

.error-message {
  color: #64748b;
  margin-bottom: 24px;
}

.error-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 24px;
}

.error-trace-id {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 20px;
  padding: 8px 16px;
  background: #f8fafc;
  border-radius: 8px;
}

.trace-label {
  color: #64748b;
  font-size: 13px;
}

.trace-value {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  letter-spacing: 0.5px;
}

.error-details {
  text-align: left;
  margin-top: 16px;
}

.error-details pre {
  background: #f8fafc;
  padding: 12px;
  border-radius: 8px;
  font-size: 12px;
  overflow-x: auto;
  color: #64748b;
}






.error-boundary-content {
  width: 100%;
  height: 100%;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .error-boundary {
    padding: 24px 16px;
    min-height: 300px;
  }

  .error-content {
    max-width: 100%;
  }

  .error-content h2 {
    font-size: 18px;
  }

  .error-actions {
    flex-direction: column;
    width: 100%;
  }

  .error-actions .n-button {
    width: 100%;
  }

  .error-trace-id {
    flex-wrap: wrap;
    justify-content: center;
    padding: 8px 12px;
  }
}
</style>

<style>
/* ErrorBoundary 深色模式（非 scoped） */
html.dark .error-content h2 {
  color: rgba(255, 255, 255, 0.85) !important;
}
html.dark .error-message {
  color: rgba(255, 255, 255, 0.65) !important;
}
html.dark .error-details pre {
  background: #2a2a2a !important;
  color: rgba(255, 255, 255, 0.65) !important;
}
html.dark .error-trace-id {
  background: #2a2a2a !important;
}
html.dark .trace-label {
  color: rgba(255, 255, 255, 0.65) !important;
}
</style>

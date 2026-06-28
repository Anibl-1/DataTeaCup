<template>
  <n-config-provider 
    :theme="themeStore.naiveTheme" 
    :theme-overrides="themeStore.themeOverrides" 
    :locale="naiveLocale" 
    :date-locale="naiveDateLocale"
    :hljs="hljs"
  >
    <n-message-provider>
      <n-notification-provider>
        <n-dialog-provider>
          <!-- 页面加载进度条 -->
          <PageProgress />
          <!-- 网络状态提示 -->
          <NetworkStatus />
          <!-- 全局加载遮罩 -->
          <GlobalLoading />
          <!-- 会话管理 -->
          <SessionManager />
          <!-- 路由视图 -->
          <router-view />
        </n-dialog-provider>
      </n-notification-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { 
  NConfigProvider, 
  NMessageProvider, 
  NNotificationProvider,
  NDialogProvider
} from 'naive-ui'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { useAppStore } from '@/stores/app'
import { throttle } from '@/utils/debounce'
import { naiveLocale, naiveDateLocale, initLocale } from '@/i18n'
import hljs from 'highlight.js/lib/core'
import json from 'highlight.js/lib/languages/json'
import sql from 'highlight.js/lib/languages/sql'
import plaintext from 'highlight.js/lib/languages/plaintext'
import SessionManager from '@/components/SessionManager.vue'
import GlobalLoading from '@/components/GlobalLoading.vue'
import NetworkStatus from '@/components/NetworkStatus.vue'
import PageProgress from '@/components/PageProgress.vue'

hljs.registerLanguage('json', json)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('text', plaintext)
hljs.registerLanguage('plaintext', plaintext)

const userStore = useUserStore()
const themeStore = useThemeStore()
const appStore = useAppStore()

// 用户活动事件监听器（移除mousemove以降低性能开销）
const activityEvents = ['mousedown', 'keypress', 'scroll', 'touchstart', 'click']

const handleUserActivity = throttle(() => {
  if (userStore.token) {
    userStore.updateLastActivityTime()
  }
}, 5000, { leading: true, trailing: false })

onMounted(() => {
  // 初始化应用状态
  appStore.init()
  
  // 初始化主题
  themeStore.init()
  
  // 初始化国际化语言
  initLocale()
  
  // 初始化会话管理
  if (userStore.token) {
    userStore.initSessionManagement()
  }
  
  // 监听用户活动
  activityEvents.forEach(event => {
    document.addEventListener(event, handleUserActivity, true)
  })
})

onUnmounted(() => {
  // 清理事件监听器
  activityEvents.forEach(event => {
    document.removeEventListener(event, handleUserActivity, true)
  })
})
</script>

<style>
/* 全局过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 选中文本样式 */
::selection {
  background: rgba(64, 158, 255, 0.3);
}
</style>

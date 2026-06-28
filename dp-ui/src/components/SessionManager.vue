<template>
  <!-- 此组件用于在 message provider 内部管理会话 -->
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage, useDialog } from 'naive-ui'
import { useUserStore } from '@/stores/user'
import { useTabsStore } from '@/stores/tabs'
import { initMessage, initDialog } from '@/utils/message'
import { resetRouter } from '@/router'

const router = useRouter()
const message = useMessage()
const dialogInst = useDialog()
const userStore = useUserStore()
const tabsStore = useTabsStore()

// 初始化 message & dialog
initMessage(message)
initDialog(dialogInst)

// 会话超时检查间隔（每30秒检查一次）
let sessionCheckInterval: number | null = null

onMounted(() => {
  // 定期检查会话超时（每30秒）
  sessionCheckInterval = window.setInterval(() => {
    if (userStore.token && userStore.checkSessionTimeout()) {
      // 清理所有状态
      tabsStore.clearTabs()
      userStore.logout()
      resetRouter()
      
      message.warning('会话已超时，请重新登录')
      router.push('/login').catch(() => {
        window.location.href = '/login'
      })
    }
  }, 30000)
})

onUnmounted(() => {
  // 清理定时器
  if (sessionCheckInterval !== null) {
    clearInterval(sessionCheckInterval)
  }
})
</script>


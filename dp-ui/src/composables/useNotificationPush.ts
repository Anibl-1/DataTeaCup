/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, onUnmounted } from 'vue'
import { useChatStore } from '@/stores/chat'

/**
 * 通知推送 composable
 *
 * 当前策略：不建立任何 WebSocket 连接。
 * 通知功能通过 REST API 轮询或页面刷新获取。
 * 这样在后端未启动时，前端控制台零错误。
 *
 * 当后端启动后，可以在此处恢复 WebSocket 连接逻辑。
 */

export interface NotificationItem {
  id: number
  title: string
  content: string
  type: string
  createTime: string
  isRead: number
  conversationId?: number
}

export function useNotificationPush() {
  const chatStore = useChatStore()

  const connected = ref(false)
  const unreadNotificationCount = ref(0)
  const notifications = ref<NotificationItem[]>([])

  let notificationPermission: NotificationPermission = 'default'

  // ==================== Browser Notification ====================
  const requestNotificationPermission = async () => {
    if (!('Notification' in window)) return
    notificationPermission = Notification.permission
    if (notificationPermission === 'default') {
      try {
        notificationPermission = await Notification.requestPermission()
      } catch { /* 静默 */ }
    }
  }

  const showDesktopNotification = (item: NotificationItem) => {
    if (!('Notification' in window) || notificationPermission !== 'granted') return
    try {
      const notification = new Notification(item.title, {
        body: item.content, icon: '/logo.svg', tag: `notification-${item.id}`
      })
      notification.onclick = () => {
        window.focus()
        if (item.conversationId) {
          if (!chatStore.panelVisible) chatStore.togglePanel()
          chatStore.selectConversation(item.conversationId)
        }
        notification.close()
      }
    } catch { /* 静默 */ }
  }

  // ==================== Notification Handling ====================
  const handleNotification = (payload: any) => {
    try {
      const item: NotificationItem = {
        id: payload.id || Date.now(),
        title: payload.title || '新通知',
        content: payload.content || '',
        type: payload.type || 'info',
        createTime: payload.createTime || new Date().toISOString(),
        isRead: 0,
        conversationId: payload.conversationId
      }
      unreadNotificationCount.value += 1
      notifications.value.unshift(item)
      showDesktopNotification(item)
    } catch { /* 静默 */ }
  }

  /**
   * 初始化：仅请求桌面通知权限。
   * 不建立任何网络连接，零控制台错误。
   */
  const init = async () => {
    await requestNotificationPermission()
  }

  const disconnect = () => {
    connected.value = false
  }

  // 保留 onUnmounted 以防未来恢复 WebSocket
  onUnmounted(() => {
    disconnect()
  })

  return {
    connected,
    unreadNotificationCount,
    notifications,
    init,
    disconnect,
    handleNotification
  }
}

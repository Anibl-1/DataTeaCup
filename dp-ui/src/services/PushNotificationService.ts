/**
 * 推送通知服务
 * Push Notification Service
 *
 * 集成 Web Push API，接收报表更新和告警通知
 * Validates: Requirements 27.3
 */

export interface PushSubscriptionInfo {
  endpoint: string
  keys: {
    p256dh: string
    auth: string
  }
}

export type NotificationPermission = 'granted' | 'denied' | 'default'

class PushNotificationService {
  private registration: ServiceWorkerRegistration | null = null

  /**
   * 检查浏览器是否支持推送通知
   */
  isSupported(): boolean {
    return 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window
  }

  /**
   * 获取当前通知权限状态
   */
  getPermission(): NotificationPermission {
    if (!this.isSupported()) return 'denied'
    return Notification.permission as NotificationPermission
  }

  /**
   * 请求通知权限
   */
  async requestPermission(): Promise<NotificationPermission> {
    if (!this.isSupported()) return 'denied'
    const result = await Notification.requestPermission()
    return result as NotificationPermission
  }

  /**
   * 注册 Service Worker 并订阅推送
   */
  async subscribe(vapidPublicKey: string): Promise<PushSubscriptionInfo | null> {
    if (!this.isSupported()) return null

    const permission = await this.requestPermission()
    if (permission !== 'granted') return null

    try {
      this.registration = await navigator.serviceWorker.ready

      const subscription = await this.registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: this.urlBase64ToUint8Array(vapidPublicKey)
      })

      const json = subscription.toJSON()
      return {
        endpoint: json.endpoint!,
        keys: {
          p256dh: json.keys!.p256dh!,
          auth: json.keys!.auth!
        }
      }
    } catch {
      return null
    }
  }

  /**
   * 取消订阅
   */
  async unsubscribe(): Promise<boolean> {
    if (!this.registration) return false
    try {
      const subscription = await this.registration.pushManager.getSubscription()
      if (subscription) {
        return await subscription.unsubscribe()
      }
      return true
    } catch {
      return false
    }
  }

  /**
   * 发送本地通知（用于测试或本地事件）
   */
  async showLocalNotification(title: string, options?: NotificationOptions): Promise<void> {
    if (this.getPermission() !== 'granted') return

    if (this.registration) {
      await this.registration.showNotification(title, {
        icon: '/logo.svg',
        badge: '/logo.svg',
        ...options
      })
    } else {
      new Notification(title, {
        icon: '/logo.svg',
        ...options
      })
    }
  }

  private urlBase64ToUint8Array(base64String: string): Uint8Array {
    const padding = '='.repeat((4 - base64String.length % 4) % 4)
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
    const rawData = window.atob(base64)
    const outputArray = new Uint8Array(rawData.length)
    for (let i = 0; i < rawData.length; ++i) {
      outputArray[i] = rawData.charCodeAt(i)
    }
    return outputArray
  }
}

export const pushNotificationService = new PushNotificationService()
export default pushNotificationService

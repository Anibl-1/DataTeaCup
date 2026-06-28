/**
 * Service Worker 管理器
 * Validates: Requirements 28.1, 28.4
 */

import { ref } from 'vue'

export const swUpdateAvailable = ref(false)
export const swRegistration = ref<ServiceWorkerRegistration | null>(null)

let updateSW: (() => Promise<void>) | null = null

/**
 * 初始化 PWA Service Worker
 * 由 vite-plugin-pwa 的 registerSW 调用
 */
export async function initServiceWorker(): Promise<void> {
  if (!('serviceWorker' in navigator)) return

  try {
    const { registerSW } = await import('virtual:pwa-register')
    updateSW = registerSW({
      immediate: true,
      onRegisteredSW(swUrl, registration) {
        if (registration) {
          swRegistration.value = registration
          // 每小时检查更新
          setInterval(() => {
            registration.update()
          }, 60 * 60 * 1000)
        }
      },
      onNeedRefresh() {
        swUpdateAvailable.value = true
      },
      onOfflineReady() {
        // offline ready
      }
    })
  } catch {
    // PWA 未配置或不支持
  }
}

/**
 * 应用 Service Worker 更新
 */
export async function applyUpdate(): Promise<void> {
  if (updateSW) {
    await updateSW()
    swUpdateAvailable.value = false
  }
}

/**
 * 检查网络状态
 */
export function isOnline(): boolean {
  return typeof navigator !== 'undefined' ? navigator.onLine : true
}

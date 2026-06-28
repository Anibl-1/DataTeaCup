/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 主屏幕快捷方式服务
 * Validates: Requirements 27.4
 */

export interface ShortcutConfig {
  name: string
  url: string
  icon?: string
  description?: string
}

class HomeScreenShortcutService {
  private deferredPrompt: any = null

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('beforeinstallprompt', (e: Event) => {
        e.preventDefault()
        this.deferredPrompt = e
      })
    }
  }

  canAddToHomeScreen(): boolean {
    return this.deferredPrompt !== null
  }

  async promptAddToHomeScreen(): Promise<boolean> {
    if (!this.deferredPrompt) return false
    try {
      this.deferredPrompt.prompt()
      const result = await this.deferredPrompt.userChoice
      this.deferredPrompt = null
      return result.outcome === 'accepted'
    } catch {
      return false
    }
  }

  isRunningAsPwa(): boolean {
    if (typeof window === 'undefined') return false
    const mq = window.matchMedia('(display-mode: standalone)')
    return mq.matches || (window.navigator as any).standalone === true
  }
}

export const homeScreenShortcutService = new HomeScreenShortcutService()
export default homeScreenShortcutService

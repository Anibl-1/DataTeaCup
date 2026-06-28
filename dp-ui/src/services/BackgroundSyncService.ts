/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 后台数据同步服务
 * 网络恢复时自动同步离线操作
 * Validates: Requirements 28.5
 */

import { ref } from 'vue'

export interface PendingAction {
  id: string
  type: string
  url: string
  method: string
  body?: any
  createdAt: number
}

const STORAGE_KEY = 'datateacup-pending-sync'

class BackgroundSyncService {
  pendingCount = ref(0)
  isSyncing = ref(false)

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('online', () => this.syncAll())
      this.pendingCount.value = this.getPendingActions().length
    }
  }

  /**
   * 添加待同步操作
   */
  addPendingAction(action: Omit<PendingAction, 'id' | 'createdAt'>): void {
    const actions = this.getPendingActions()
    actions.push({
      ...action,
      id: crypto.randomUUID?.() || `${Date.now()}-${Math.random()}`,
      createdAt: Date.now()
    })
    this.savePendingActions(actions)
    this.pendingCount.value = actions.length
  }

  /**
   * 同步所有待处理操作
   */
  async syncAll(): Promise<{ success: number; failed: number }> {
    if (this.isSyncing.value || !navigator.onLine) return { success: 0, failed: 0 }

    this.isSyncing.value = true
    const actions = this.getPendingActions()
    let success = 0
    let failed = 0
    const remaining: PendingAction[] = []

    for (const action of actions) {
      try {
        await fetch(action.url, {
          method: action.method,
          headers: { 'Content-Type': 'application/json' },
          body: action.body ? JSON.stringify(action.body) : undefined
        })
        success++
      } catch {
        remaining.push(action)
        failed++
      }
    }

    this.savePendingActions(remaining)
    this.pendingCount.value = remaining.length
    this.isSyncing.value = false
    return { success, failed }
  }

  getPendingActions(): PendingAction[] {
    try {
      const data = localStorage.getItem(STORAGE_KEY)
      return data ? JSON.parse(data) : []
    } catch {
      return []
    }
  }

  private savePendingActions(actions: PendingAction[]): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(actions))
    } catch { /* ignore */ }
  }

  clearAll(): void {
    try { localStorage.removeItem(STORAGE_KEY) } catch { /* ignore */ }
    this.pendingCount.value = 0
  }
}

export const backgroundSyncService = new BackgroundSyncService()
export default backgroundSyncService

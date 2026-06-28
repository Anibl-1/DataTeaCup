/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 离线缓存服务
 * Offline Cache Service
 *
 * 使用 IndexedDB 缓存报表数据供离线查看
 * Validates: Requirements 27.2
 */

const DB_NAME = 'datateacup-offline'
const DB_VERSION = 1
const STORE_NAME = 'report-cache'

export interface CachedReport {
  id: string
  title: string
  data: any
  cachedAt: number
  expiresAt: number
}

class OfflineCacheService {
  private db: IDBDatabase | null = null

  async init(): Promise<void> {
    if (this.db) return
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, DB_VERSION)

      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result
        if (!db.objectStoreNames.contains(STORE_NAME)) {
          const store = db.createObjectStore(STORE_NAME, { keyPath: 'id' })
          store.createIndex('cachedAt', 'cachedAt', { unique: false })
          store.createIndex('expiresAt', 'expiresAt', { unique: false })
        }
      }

      request.onsuccess = (event) => {
        this.db = (event.target as IDBOpenDBRequest).result
        resolve()
      }

      request.onerror = () => reject(request.error)
    })
  }

  /**
   * 缓存报表数据
   * @param report 报表数据
   * @param ttlMs 缓存有效期（毫秒），默认24小时
   */
  async cacheReport(report: Omit<CachedReport, 'cachedAt' | 'expiresAt'>, ttlMs = 24 * 60 * 60 * 1000): Promise<void> {
    await this.init()
    const now = Date.now()
    const entry: CachedReport = {
      ...report,
      cachedAt: now,
      expiresAt: now + ttlMs
    }

    return new Promise((resolve, reject) => {
      const tx = this.db!.transaction(STORE_NAME, 'readwrite')
      tx.objectStore(STORE_NAME).put(entry)
      tx.oncomplete = () => resolve()
      tx.onerror = () => reject(tx.error)
    })
  }

  /**
   * 获取缓存的报表
   */
  async getReport(id: string): Promise<CachedReport | null> {
    await this.init()
    return new Promise((resolve, reject) => {
      const tx = this.db!.transaction(STORE_NAME, 'readonly')
      const request = tx.objectStore(STORE_NAME).get(id)
      request.onsuccess = () => {
        const result = request.result as CachedReport | undefined
        if (result && result.expiresAt > Date.now()) {
          resolve(result)
        } else {
          // 过期则删除
          if (result) this.removeReport(id).catch(() => {})
          resolve(null)
        }
      }
      request.onerror = () => reject(request.error)
    })
  }

  /**
   * 获取所有缓存的报表列表
   */
  async listCachedReports(): Promise<CachedReport[]> {
    await this.init()
    return new Promise((resolve, reject) => {
      const tx = this.db!.transaction(STORE_NAME, 'readonly')
      const request = tx.objectStore(STORE_NAME).getAll()
      request.onsuccess = () => {
        const now = Date.now()
        resolve((request.result as CachedReport[]).filter(r => r.expiresAt > now))
      }
      request.onerror = () => reject(request.error)
    })
  }

  /**
   * 删除缓存的报表
   */
  async removeReport(id: string): Promise<void> {
    await this.init()
    return new Promise((resolve, reject) => {
      const tx = this.db!.transaction(STORE_NAME, 'readwrite')
      tx.objectStore(STORE_NAME).delete(id)
      tx.oncomplete = () => resolve()
      tx.onerror = () => reject(tx.error)
    })
  }

  /**
   * 清理过期缓存
   */
  async cleanExpired(): Promise<number> {
    await this.init()
    const all = await this.listAllRaw()
    const now = Date.now()
    let cleaned = 0

    for (const item of all) {
      if (item.expiresAt <= now) {
        await this.removeReport(item.id)
        cleaned++
      }
    }
    return cleaned
  }

  private async listAllRaw(): Promise<CachedReport[]> {
    return new Promise((resolve, reject) => {
      const tx = this.db!.transaction(STORE_NAME, 'readonly')
      const request = tx.objectStore(STORE_NAME).getAll()
      request.onsuccess = () => resolve(request.result as CachedReport[])
      request.onerror = () => reject(request.error)
    })
  }

  /**
   * 获取缓存大小估算
   */
  async getCacheSize(): Promise<{ count: number; estimatedBytes: number }> {
    const reports = await this.listCachedReports()
    const estimatedBytes = reports.reduce((sum, r) => sum + JSON.stringify(r.data).length * 2, 0)
    return { count: reports.length, estimatedBytes }
  }

  /**
   * 清空所有缓存
   */
  async clearAll(): Promise<void> {
    await this.init()
    return new Promise((resolve, reject) => {
      const tx = this.db!.transaction(STORE_NAME, 'readwrite')
      tx.objectStore(STORE_NAME).clear()
      tx.oncomplete = () => resolve()
      tx.onerror = () => reject(tx.error)
    })
  }
}

export const offlineCacheService = new OfflineCacheService()
export default offlineCacheService

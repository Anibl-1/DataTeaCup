/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 图表数据缓存服务
 * 提供内存缓存和本地存储缓存，减少重复请求
 */

interface CacheEntry<T> {
  data: T
  timestamp: number
  expireAt: number
}

interface CacheOptions {
  ttl?: number // 缓存时间（毫秒），默认5分钟
  useLocalStorage?: boolean // 是否使用本地存储
  maxSize?: number // 最大缓存条目数
}

const DEFAULT_TTL = 5 * 60 * 1000 // 5分钟
const DEFAULT_MAX_SIZE = 100
const STORAGE_PREFIX = 'chart_cache_'

class ChartCache {
  private memoryCache: Map<string, CacheEntry<any>> = new Map()
  private maxSize: number = DEFAULT_MAX_SIZE

  constructor(maxSize?: number) {
    if (maxSize) this.maxSize = maxSize
    this.cleanExpired()
  }

  /**
   * 生成缓存键
   */
  generateKey(chartId: number | string, params?: Record<string, any>): string {
    const paramStr = params ? JSON.stringify(params, Object.keys(params).sort()) : ''
    return `chart_${chartId}_${paramStr}`
  }

  /**
   * 获取缓存
   */
  get<T>(key: string, options: CacheOptions = {}): T | null {
    // 先检查内存缓存
    const memEntry = this.memoryCache.get(key)
    if (memEntry && memEntry.expireAt > Date.now()) {
      return memEntry.data as T
    }

    // 检查本地存储
    if (options.useLocalStorage) {
      try {
        const stored = localStorage.getItem(STORAGE_PREFIX + key)
        if (stored) {
          const entry: CacheEntry<T> = JSON.parse(stored)
          if (entry.expireAt > Date.now()) {
            // 同步到内存缓存
            this.memoryCache.set(key, entry)
            return entry.data
          } else {
            // 清理过期数据
            localStorage.removeItem(STORAGE_PREFIX + key)
          }
        }
      } catch (e) {
        console.warn('读取本地缓存失败:', e)
      }
    }

    // 清理过期的内存缓存
    if (memEntry) {
      this.memoryCache.delete(key)
    }

    return null
  }

  /**
   * 设置缓存
   */
  set<T>(key: string, data: T, options: CacheOptions = {}): void {
    const ttl = options.ttl || DEFAULT_TTL
    const entry: CacheEntry<T> = {
      data,
      timestamp: Date.now(),
      expireAt: Date.now() + ttl
    }

    // 检查缓存大小
    if (this.memoryCache.size >= this.maxSize) {
      this.evictOldest()
    }

    // 存入内存缓存
    this.memoryCache.set(key, entry)

    // 存入本地存储
    if (options.useLocalStorage) {
      try {
        localStorage.setItem(STORAGE_PREFIX + key, JSON.stringify(entry))
      } catch (e) {
        console.warn('写入本地缓存失败:', e)
        // 可能是存储空间不足，清理旧数据
        this.cleanLocalStorage()
      }
    }
  }

  /**
   * 删除缓存
   */
  delete(key: string): void {
    this.memoryCache.delete(key)
    try {
      localStorage.removeItem(STORAGE_PREFIX + key)
    } catch (e) {
      // 忽略错误
    }
  }

  /**
   * 清除指定图表的所有缓存
   */
  clearChart(chartId: number | string): void {
    const prefix = `chart_${chartId}_`
    
    // 清除内存缓存
    for (const key of this.memoryCache.keys()) {
      if (key.startsWith(prefix)) {
        this.memoryCache.delete(key)
      }
    }

    // 清除本地存储
    try {
      for (let i = localStorage.length - 1; i >= 0; i--) {
        const key = localStorage.key(i)
        if (key && key.startsWith(STORAGE_PREFIX + prefix)) {
          localStorage.removeItem(key)
        }
      }
    } catch (e) {
      // 忽略错误
    }
  }

  /**
   * 清除所有缓存
   */
  clear(): void {
    this.memoryCache.clear()
    
    try {
      for (let i = localStorage.length - 1; i >= 0; i--) {
        const key = localStorage.key(i)
        if (key && key.startsWith(STORAGE_PREFIX)) {
          localStorage.removeItem(key)
        }
      }
    } catch (e) {
      // 忽略错误
    }
  }

  /**
   * 获取缓存统计信息
   */
  getStats(): {
    memorySize: number
    localStorageSize: number
    oldestEntry: number | null
    newestEntry: number | null
  } {
    let oldestEntry: number | null = null
    let newestEntry: number | null = null
    let localStorageSize = 0

    for (const entry of this.memoryCache.values()) {
      if (oldestEntry === null || entry.timestamp < oldestEntry) {
        oldestEntry = entry.timestamp
      }
      if (newestEntry === null || entry.timestamp > newestEntry) {
        newestEntry = entry.timestamp
      }
    }

    try {
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i)
        if (key && key.startsWith(STORAGE_PREFIX)) {
          localStorageSize++
        }
      }
    } catch (e) {
      // 忽略错误
    }

    return {
      memorySize: this.memoryCache.size,
      localStorageSize,
      oldestEntry,
      newestEntry
    }
  }

  /**
   * 清理过期缓存
   */
  private cleanExpired(): void {
    const now = Date.now()
    
    // 清理内存缓存
    for (const [key, entry] of this.memoryCache.entries()) {
      if (entry.expireAt <= now) {
        this.memoryCache.delete(key)
      }
    }

    // 清理本地存储
    try {
      for (let i = localStorage.length - 1; i >= 0; i--) {
        const key = localStorage.key(i)
        if (key && key.startsWith(STORAGE_PREFIX)) {
          const stored = localStorage.getItem(key)
          if (stored) {
            try {
              const entry = JSON.parse(stored)
              if (entry.expireAt <= now) {
                localStorage.removeItem(key)
              }
            } catch {
              localStorage.removeItem(key)
            }
          }
        }
      }
    } catch (e) {
      // 忽略错误
    }
  }

  /**
   * 淘汰最旧的缓存
   */
  private evictOldest(): void {
    let oldestKey: string | null = null
    let oldestTime = Infinity

    for (const [key, entry] of this.memoryCache.entries()) {
      if (entry.timestamp < oldestTime) {
        oldestTime = entry.timestamp
        oldestKey = key
      }
    }

    if (oldestKey) {
      this.memoryCache.delete(oldestKey)
    }
  }

  /**
   * 清理本地存储
   */
  private cleanLocalStorage(): void {
    try {
      const entries: Array<{ key: string; timestamp: number }> = []
      
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i)
        if (key && key.startsWith(STORAGE_PREFIX)) {
          const stored = localStorage.getItem(key)
          if (stored) {
            try {
              const entry = JSON.parse(stored)
              entries.push({ key, timestamp: entry.timestamp })
            } catch {
              localStorage.removeItem(key)
            }
          }
        }
      }

      // 按时间排序，删除最旧的一半
      entries.sort((a, b) => a.timestamp - b.timestamp)
      const toDelete = Math.ceil(entries.length / 2)
      for (let i = 0; i < toDelete; i++) {
        localStorage.removeItem(entries[i].key)
      }
    } catch (e) {
      // 忽略错误
    }
  }
}

// 导出单例
export const chartCache = new ChartCache()

/**
 * 带缓存的数据获取函数
 */
export async function fetchWithCache<T>(
  key: string,
  fetcher: () => Promise<T>,
  options: CacheOptions = {}
): Promise<T> {
  // 尝试从缓存获取
  const cached = chartCache.get<T>(key, options)
  if (cached !== null) {
    return cached
  }

  // 获取新数据
  const data = await fetcher()
  
  // 存入缓存
  chartCache.set(key, data, options)
  
  return data
}

/**
 * 预加载图表数据
 */
export async function preloadChartData(
  chartIds: number[],
  fetcher: (id: number) => Promise<any>,
  options: CacheOptions = {}
): Promise<void> {
  const promises = chartIds.map(async id => {
    const key = chartCache.generateKey(id)
    const cached = chartCache.get(key, options)
    if (cached === null) {
      try {
        const data = await fetcher(id)
        chartCache.set(key, data, options)
      } catch (e) {
        console.warn(`预加载图表 ${id} 失败:`, e)
      }
    }
  })

  await Promise.all(promises)
}

export default chartCache

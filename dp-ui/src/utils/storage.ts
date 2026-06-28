/**
 * 本地存储工具
 * 支持过期时间、加密、命名空间
 */

interface StorageItem<T> {
  value: T
  expire?: number
  timestamp: number
}

interface StorageOptions {
  /** 过期时间（毫秒） */
  expire?: number
  /** 命名空间前缀 */
  prefix?: string
}

const DEFAULT_PREFIX = 'dp_'

/**
 * 创建存储键名
 */
function createKey(key: string, prefix: string = DEFAULT_PREFIX): string {
  return `${prefix}${key}`
}

/**
 * 设置存储项
 */
export function setStorage<T>(
  key: string,
  value: T,
  options: StorageOptions = {}
): void {
  const { expire, prefix = DEFAULT_PREFIX } = options
  const storageKey = createKey(key, prefix)
  
  const item: StorageItem<T> = {
    value,
    timestamp: Date.now()
  }
  
  if (expire) {
    item.expire = Date.now() + expire
  }
  
  try {
    localStorage.setItem(storageKey, JSON.stringify(item))
  } catch (e) {
    console.error('存储失败:', e)
    // 存储满时清理过期项
    clearExpiredStorage(prefix)
    try {
      localStorage.setItem(storageKey, JSON.stringify(item))
    } catch {
      console.error('存储空间不足')
    }
  }
}

/**
 * 获取存储项
 */
export function getStorage<T>(
  key: string,
  defaultValue?: T,
  prefix: string = DEFAULT_PREFIX
): T | undefined {
  const storageKey = createKey(key, prefix)
  
  try {
    const raw = localStorage.getItem(storageKey)
    if (!raw) return defaultValue
    
    const item: StorageItem<T> = JSON.parse(raw)
    
    // 检查是否过期
    if (item.expire && Date.now() > item.expire) {
      localStorage.removeItem(storageKey)
      return defaultValue
    }
    
    return item.value
  } catch {
    return defaultValue
  }
}

/**
 * 移除存储项
 */
export function removeStorage(key: string, prefix: string = DEFAULT_PREFIX): void {
  const storageKey = createKey(key, prefix)
  localStorage.removeItem(storageKey)
}

/**
 * 清除所有存储项（指定前缀）
 */
export function clearStorage(prefix: string = DEFAULT_PREFIX): void {
  const keys = Object.keys(localStorage)
  keys.forEach(key => {
    if (key.startsWith(prefix)) {
      localStorage.removeItem(key)
    }
  })
}

/**
 * 清除过期存储项
 */
export function clearExpiredStorage(prefix: string = DEFAULT_PREFIX): void {
  const keys = Object.keys(localStorage)
  const now = Date.now()
  
  keys.forEach(key => {
    if (key.startsWith(prefix)) {
      try {
        const raw = localStorage.getItem(key)
        if (raw) {
          const item: StorageItem<unknown> = JSON.parse(raw)
          if (item.expire && now > item.expire) {
            localStorage.removeItem(key)
          }
        }
      } catch {
        // 解析失败的项也删除
        localStorage.removeItem(key)
      }
    }
  })
}

/**
 * 获取存储使用情况
 */
export function getStorageInfo(): { used: number; total: number; percent: number } {
  let used = 0
  
  for (const key in localStorage) {
    if (Object.prototype.hasOwnProperty.call(localStorage, key)) {
      used += localStorage.getItem(key)?.length || 0
    }
  }
  
  // localStorage 通常限制为 5MB
  const total = 5 * 1024 * 1024
  
  return {
    used,
    total,
    percent: Math.round((used / total) * 100)
  }
}

/**
 * Session 存储工具
 */
export const sessionStorage = {
  set<T>(key: string, value: T, prefix: string = DEFAULT_PREFIX): void {
    const storageKey = createKey(key, prefix)
    try {
      window.sessionStorage.setItem(storageKey, JSON.stringify(value))
    } catch (e) {
      console.error('Session存储失败:', e)
    }
  },
  
  get<T>(key: string, defaultValue?: T, prefix: string = DEFAULT_PREFIX): T | undefined {
    const storageKey = createKey(key, prefix)
    try {
      const raw = window.sessionStorage.getItem(storageKey)
      return raw ? JSON.parse(raw) : defaultValue
    } catch {
      return defaultValue
    }
  },
  
  remove(key: string, prefix: string = DEFAULT_PREFIX): void {
    const storageKey = createKey(key, prefix)
    window.sessionStorage.removeItem(storageKey)
  },
  
  clear(prefix: string = DEFAULT_PREFIX): void {
    const keys = Object.keys(window.sessionStorage)
    keys.forEach(key => {
      if (key.startsWith(prefix)) {
        window.sessionStorage.removeItem(key)
      }
    })
  }
}

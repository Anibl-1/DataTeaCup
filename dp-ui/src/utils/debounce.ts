/**
 * 防抖节流工具
 */

/**
 * 防抖函数
 * @param fn 要执行的函数
 * @param delay 延迟时间（毫秒）
 * @param immediate 是否立即执行
 */
export function debounce<T extends (...args: unknown[]) => unknown>(
  fn: T,
  delay: number = 300,
  immediate: boolean = false
): (...args: Parameters<T>) => void {
  let timer: ReturnType<typeof setTimeout> | null = null
  let isInvoked = false

  return function (this: unknown, ...args: Parameters<T>) {
    if (timer) {
      clearTimeout(timer)
    }

    if (immediate && !isInvoked) {
      fn.apply(this, args)
      isInvoked = true
    }

    timer = setTimeout(() => {
      if (!immediate) {
        fn.apply(this, args)
      }
      isInvoked = false
      timer = null
    }, delay)
  }
}

/**
 * 节流函数
 * @param fn 要执行的函数
 * @param interval 间隔时间（毫秒）
 * @param options 配置选项
 */
export function throttle<T extends (...args: unknown[]) => unknown>(
  fn: T,
  interval: number = 300,
  options: { leading?: boolean; trailing?: boolean } = {}
): (...args: Parameters<T>) => void {
  const { leading = true, trailing = true } = options
  let lastTime = 0
  let timer: ReturnType<typeof setTimeout> | null = null

  return function (this: unknown, ...args: Parameters<T>) {
    const now = Date.now()

    if (!lastTime && !leading) {
      lastTime = now
    }

    const remaining = interval - (now - lastTime)

    if (remaining <= 0 || remaining > interval) {
      if (timer) {
        clearTimeout(timer)
        timer = null
      }
      lastTime = now
      fn.apply(this, args)
    } else if (!timer && trailing) {
      timer = setTimeout(() => {
        lastTime = leading ? Date.now() : 0
        timer = null
        fn.apply(this, args)
      }, remaining)
    }
  }
}

/**
 * 可取消的防抖函数
 */
export function cancellableDebounce<T extends (...args: unknown[]) => unknown>(
  fn: T,
  delay: number = 300
): {
  run: (...args: Parameters<T>) => void
  cancel: () => void
  flush: () => void
} {
  let timer: ReturnType<typeof setTimeout> | null = null
  let lastArgs: Parameters<T> | null = null
  let context: unknown = null

  const cancel = () => {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
    lastArgs = null
    context = null
  }

  const flush = () => {
    if (timer && lastArgs) {
      fn.apply(context, lastArgs)
      cancel()
    }
  }

  const run = function (this: unknown, ...args: Parameters<T>) {
    lastArgs = args
    context = this

    if (timer) {
      clearTimeout(timer)
    }

    timer = setTimeout(() => {
      fn.apply(context, lastArgs!)
      cancel()
    }, delay)
  }

  return { run, cancel, flush }
}

/**
 * 异步防抖（返回 Promise）
 */
export function asyncDebounce<T extends (...args: unknown[]) => Promise<unknown>>(
  fn: T,
  delay: number = 300
): (...args: Parameters<T>) => Promise<Awaited<ReturnType<T>>> {
  let timer: ReturnType<typeof setTimeout> | null = null
  let pendingPromise: Promise<Awaited<ReturnType<T>>> | null = null
  let resolve: ((value: Awaited<ReturnType<T>>) => void) | null = null
  let reject: ((reason?: unknown) => void) | null = null

  return function (this: unknown, ...args: Parameters<T>): Promise<Awaited<ReturnType<T>>> {
    if (timer) {
      clearTimeout(timer)
    }

    if (!pendingPromise) {
      pendingPromise = new Promise<Awaited<ReturnType<T>>>((res, rej) => {
        resolve = res
        reject = rej
      })
    }

    timer = setTimeout(async () => {
      try {
        const result = await fn.apply(this, args)
        resolve?.(result as Awaited<ReturnType<T>>)
      } catch (error) {
        reject?.(error)
      } finally {
        pendingPromise = null
        resolve = null
        reject = null
        timer = null
      }
    }, delay)

    return pendingPromise
  }
}

/**
 * 请求锁（防止重复请求）
 */
export function createRequestLock() {
  const locks = new Map<string, boolean>()

  return {
    /**
     * 尝试获取锁
     */
    acquire(key: string): boolean {
      if (locks.get(key)) {
        return false
      }
      locks.set(key, true)
      return true
    },

    /**
     * 释放锁
     */
    release(key: string): void {
      locks.delete(key)
    },

    /**
     * 检查是否已锁定
     */
    isLocked(key: string): boolean {
      return locks.get(key) === true
    },

    /**
     * 清除所有锁
     */
    clear(): void {
      locks.clear()
    }
  }
}

/**
 * 带锁的异步函数执行
 */
export async function withLock<T>(
  key: string,
  fn: () => Promise<T>,
  lock = createRequestLock()
): Promise<T | null> {
  if (!lock.acquire(key)) {
    return null
  }

  try {
    return await fn()
  } finally {
    lock.release(key)
  }
}

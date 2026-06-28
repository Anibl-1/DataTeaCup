/**
 * 性能监控工具
 */
import { logger } from './logger'

interface PerformanceMetric {
  name: string
  value: number
  timestamp: number
}

interface PerformanceReport {
  metrics: PerformanceMetric[]
  navigation?: PerformanceNavigationTiming
  resources?: PerformanceResourceTiming[]
}

class PerformanceMonitor {
  private metrics: PerformanceMetric[] = []
  private marks: Map<string, number> = new Map()

  /**
   * 标记开始时间
   */
  mark(name: string): void {
    this.marks.set(name, performance.now())
    if (typeof performance.mark === 'function') {
      try {
        performance.mark(`${name}-start`)
      } catch {
        // 忽略错误
      }
    }
  }

  /**
   * 测量从标记到现在的时间
   */
  measure(name: string, startMark?: string): number {
    const markName = startMark || name
    const startTime = this.marks.get(markName)
    
    if (startTime === undefined) {
      logger.warn(`性能标记 "${markName}" 不存在`)
      return 0
    }

    const duration = performance.now() - startTime
    this.addMetric(name, duration)
    this.marks.delete(markName)

    if (typeof performance.measure === 'function') {
      try {
        performance.measure(name, `${markName}-start`)
      } catch {
        // 忽略错误
      }
    }

    return duration
  }

  /**
   * 添加自定义指标
   */
  addMetric(name: string, value: number): void {
    this.metrics.push({
      name,
      value,
      timestamp: Date.now()
    })

    // 开发环境输出
    if (import.meta.env.DEV) {
      logger.debug(`[性能] ${name}: ${value.toFixed(2)}ms`)
    }
  }

  /**
   * 获取页面加载性能
   */
  getNavigationTiming(): PerformanceNavigationTiming | null {
    const entries = performance.getEntriesByType('navigation')
    return entries[0] as PerformanceNavigationTiming || null
  }

  /**
   * 获取资源加载性能
   */
  getResourceTiming(): PerformanceResourceTiming[] {
    return performance.getEntriesByType('resource') as PerformanceResourceTiming[]
  }

  /**
   * 获取 Web Vitals 指标
   */
  getWebVitals(): Record<string, number | null> {
    const nav = this.getNavigationTiming()
    
    return {
      // 首次内容绘制 (FCP)
      fcp: this.getFCP(),
      // 最大内容绘制 (LCP)
      lcp: this.getLCP(),
      // DOM 加载完成时间
      domContentLoaded: nav ? nav.domContentLoadedEventEnd - nav.startTime : null,
      // 页面完全加载时间
      loadComplete: nav ? nav.loadEventEnd - nav.startTime : null,
      // DNS 查询时间
      dns: nav ? nav.domainLookupEnd - nav.domainLookupStart : null,
      // TCP 连接时间
      tcp: nav ? nav.connectEnd - nav.connectStart : null,
      // 首字节时间 (TTFB)
      ttfb: nav ? nav.responseStart - nav.requestStart : null
    }
  }

  /**
   * 获取 FCP
   */
  private getFCP(): number | null {
    const entries = performance.getEntriesByName('first-contentful-paint')
    return entries.length > 0 ? entries[0].startTime : null
  }

  /**
   * 获取 LCP
   */
  private getLCP(): number | null {
    const entries = performance.getEntriesByType('largest-contentful-paint')
    return entries.length > 0 ? entries[entries.length - 1].startTime : null
  }

  /**
   * 生成性能报告
   */
  generateReport(): PerformanceReport {
    return {
      metrics: [...this.metrics],
      navigation: this.getNavigationTiming() || undefined,
      resources: this.getResourceTiming()
    }
  }

  /**
   * 清除指标
   */
  clear(): void {
    this.metrics = []
    this.marks.clear()
    if (typeof performance.clearMarks === 'function') {
      performance.clearMarks()
    }
    if (typeof performance.clearMeasures === 'function') {
      performance.clearMeasures()
    }
  }

  /**
   * 输出性能报告到控制台
   */
  logReport(): void {
    const vitals = this.getWebVitals()
    
    logger.group('📊 性能报告')
    logger.info('Web Vitals:', vitals)
    
    if (this.metrics.length > 0) {
      logger.info('自定义指标:', this.metrics)
    }
    
    logger.groupEnd()
  }
}

// 导出单例
export const perfMonitor = new PerformanceMonitor()

/**
 * 性能监控装饰器（用于方法）
 */
export function measurePerformance(name?: string) {
  return function (
    _target: unknown,
    propertyKey: string,
    descriptor: PropertyDescriptor
  ) {
    const originalMethod = descriptor.value
    const metricName = name || propertyKey

    descriptor.value = async function (...args: unknown[]) {
      perfMonitor.mark(metricName)
      try {
        const result = await originalMethod.apply(this, args)
        perfMonitor.measure(metricName)
        return result
      } catch (error) {
        perfMonitor.measure(metricName)
        throw error
      }
    }

    return descriptor
  }
}

/**
 * 监控函数执行时间
 */
export async function withPerformance<T>(
  name: string,
  fn: () => T | Promise<T>
): Promise<T> {
  perfMonitor.mark(name)
  try {
    const result = await fn()
    perfMonitor.measure(name)
    return result
  } catch (error) {
    perfMonitor.measure(name)
    throw error
  }
}

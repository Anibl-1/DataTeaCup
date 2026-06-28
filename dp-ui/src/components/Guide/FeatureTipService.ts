/**
 * 功能提示服务
 * Feature Tip Service
 * 
 * 管理功能提示的状态，支持可关闭提示和 localStorage 持久化。
 * 
 * 需求 19.2: THE DataTeaCup SHALL 为复杂功能提供上下文相关的使用提示
 * 
 * @module FeatureTipService
 */

import { ref } from 'vue'
import type { 
  FeatureTipConfig, 
  FeatureTipState, 
  IFeatureTipService,
  UseFeatureTipReturn
} from './featureTipTypes'

// ==================== 常量定义 ====================

/** localStorage 存储键 */
const STORAGE_KEY = 'datateacup_dismissed_tips'

/** 默认状态 */
const DEFAULT_STATE: FeatureTipState = {
  dismissedTips: []
}

// ==================== 存储工具函数 ====================

/**
 * 从 localStorage 加载已关闭的提示列表
 */
function loadDismissedTips(): string[] {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored) {
      const parsed = JSON.parse(stored)
      if (Array.isArray(parsed)) {
        return parsed
      }
    }
  } catch (e) {
    console.warn('[FeatureTipService] 加载已关闭提示列表失败:', e)
  }
  return []
}

/**
 * 保存已关闭的提示列表到 localStorage
 */
function saveDismissedTips(tips: string[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(tips))
  } catch (e) {
    console.warn('[FeatureTipService] 保存已关闭提示列表失败:', e)
  }
}

// ==================== 功能提示服务类 ====================

/**
 * 功能提示服务类
 * 
 * 提供功能提示的完整生命周期管理，包括：
 * - 提示配置注册
 * - 关闭状态管理
 * - 状态持久化
 */
export class FeatureTipService implements IFeatureTipService {
  /** 提示配置映射 */
  private tips: Map<string, FeatureTipConfig> = new Map()
  
  /** 提示状态 */
  private state: FeatureTipState
  
  /** 状态变更回调 */
  private onStateChange: ((state: FeatureTipState) => void) | null = null
  
  constructor() {
    // 初始化状态，加载已关闭的提示
    this.state = {
      ...DEFAULT_STATE,
      dismissedTips: loadDismissedTips()
    }
  }
  
  /**
   * 设置状态变更回调
   */
  setOnStateChange(callback: (state: FeatureTipState) => void): void {
    this.onStateChange = callback
  }
  
  /**
   * 触发状态变更
   */
  private emitStateChange(): void {
    if (this.onStateChange) {
      this.onStateChange({ ...this.state })
    }
  }
  
  /**
   * 注册提示配置
   */
  registerTip(config: FeatureTipConfig): void {
    if (!config.id || !config.content) {
      console.warn('[FeatureTipService] 无效的提示配置:', config)
      return
    }
    this.tips.set(config.id, config)
  }
  
  /**
   * 批量注册提示配置
   */
  registerTips(configs: FeatureTipConfig[]): void {
    configs.forEach(config => this.registerTip(config))
  }
  
  /**
   * 获取提示配置
   */
  getTip(tipId: string): FeatureTipConfig | undefined {
    return this.tips.get(tipId)
  }
  
  /**
   * 获取所有提示配置
   */
  getAllTips(): FeatureTipConfig[] {
    return Array.from(this.tips.values())
  }
  
  /**
   * 获取功能相关的提示
   */
  getTipsByFeature(feature: string): FeatureTipConfig[] {
    return this.getAllTips().filter(tip => tip.feature === feature)
  }
  
  /**
   * 获取当前状态
   */
  getState(): FeatureTipState {
    return { ...this.state }
  }
  
  /**
   * 检查提示是否已关闭
   */
  isDismissed(tipId: string): boolean {
    return this.state.dismissedTips.includes(tipId)
  }
  
  /**
   * 关闭提示
   */
  dismiss(tipId: string): void {
    if (!this.state.dismissedTips.includes(tipId)) {
      this.state.dismissedTips.push(tipId)
      saveDismissedTips(this.state.dismissedTips)
      this.emitStateChange()
    }
  }
  
  /**
   * 重置提示（重新显示）
   */
  reset(tipId: string): void {
    const index = this.state.dismissedTips.indexOf(tipId)
    if (index > -1) {
      this.state.dismissedTips.splice(index, 1)
      saveDismissedTips(this.state.dismissedTips)
      this.emitStateChange()
    }
  }
  
  /**
   * 重置所有提示
   */
  resetAll(): void {
    this.state.dismissedTips = []
    saveDismissedTips([])
    this.emitStateChange()
  }
  
  /**
   * 获取已关闭的提示列表
   */
  getDismissedTips(): string[] {
    return [...this.state.dismissedTips]
  }
}

// ==================== 单例实例 ====================

/** 全局功能提示服务实例 */
let featureTipServiceInstance: FeatureTipService | null = null

/**
 * 获取功能提示服务实例（单例）
 */
export function getFeatureTipService(): FeatureTipService {
  if (!featureTipServiceInstance) {
    featureTipServiceInstance = new FeatureTipService()
  }
  return featureTipServiceInstance
}

/**
 * 创建新的功能提示服务实例
 */
export function createFeatureTipService(): FeatureTipService {
  return new FeatureTipService()
}

// ==================== Vue Composable ====================

/**
 * 功能提示 Composable
 * 
 * 提供响应式的功能提示服务访问
 * 
 * @example
 * ```typescript
 * const { 
 *   isDismissed, 
 *   dismiss, 
 *   reset,
 *   getTip
 * } = useFeatureTip()
 * 
 * // 检查提示是否已关闭
 * if (!isDismissed('sql-editor-tip')) {
 *   // 显示提示
 * }
 * 
 * // 关闭提示
 * dismiss('sql-editor-tip')
 * ```
 */
export function useFeatureTip(): UseFeatureTipReturn {
  const service = getFeatureTipService()
  
  // 响应式状态
  const state = ref<FeatureTipState>(service.getState())
  
  // 监听状态变更
  service.setOnStateChange((newState) => {
    state.value = newState
  })
  
  return {
    // 状态检查
    isDismissed: (tipId: string) => service.isDismissed(tipId),
    
    // 操作方法
    dismiss: (tipId: string) => service.dismiss(tipId),
    reset: (tipId: string) => service.reset(tipId),
    resetAll: () => service.resetAll(),
    
    // 配置管理
    registerTip: (config: FeatureTipConfig) => service.registerTip(config),
    registerTips: (configs: FeatureTipConfig[]) => service.registerTips(configs),
    getTip: (tipId: string) => service.getTip(tipId),
    getTipsByFeature: (feature: string) => service.getTipsByFeature(feature),
    
    // 状态
    get dismissedTips() {
      return service.getDismissedTips()
    }
  }
}

// ==================== 导出 ====================

export type { 
  FeatureTipConfig, 
  FeatureTipState, 
  IFeatureTipService,
  UseFeatureTipReturn
}


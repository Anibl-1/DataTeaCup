/**
 * 功能引导服务
 * Feature Guide Service
 * 
 * 管理功能引导的状态和步骤，支持首次使用引导、步骤导航和完成状态追踪。
 * 
 * 需求 19.1: WHEN 用户首次使用某功能时，THE DataTeaCup SHALL 显示功能引导教程
 * 
 * @module GuideService
 */

import { ref, readonly } from 'vue'
import type { 
  GuideConfig, 
  GuideState, 
  GuideStep, 
  IGuideService 
} from './types'

// ==================== 常量定义 ====================

/** localStorage 存储键 */
const STORAGE_KEY = 'datateacup_completed_guides'

/** 默认引导状态 */
const DEFAULT_STATE: GuideState = {
  currentGuideId: null,
  currentStepIndex: 0,
  isActive: false,
  completedGuides: []
}

// ==================== 存储工具函数 ====================

/**
 * 从 localStorage 加载已完成的引导列表
 */
function loadCompletedGuides(): string[] {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored) {
      const parsed = JSON.parse(stored)
      if (Array.isArray(parsed)) {
        return parsed
      }
    }
  } catch (e) {
    console.warn('[GuideService] 加载已完成引导列表失败:', e)
  }
  return []
}

/**
 * 保存已完成的引导列表到 localStorage
 */
function saveCompletedGuides(guides: string[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(guides))
  } catch (e) {
    console.warn('[GuideService] 保存已完成引导列表失败:', e)
  }
}

// ==================== 引导服务类 ====================

/**
 * 引导服务类
 * 
 * 提供功能引导的完整生命周期管理，包括：
 * - 引导配置注册
 * - 引导状态管理
 * - 步骤导航
 * - 完成状态持久化
 */
export class GuideService implements IGuideService {
  /** 引导配置映射 */
  private guides: Map<string, GuideConfig> = new Map()
  
  /** 引导状态 */
  private state: GuideState
  
  /** 状态变更回调 */
  private onStateChange: ((state: GuideState) => void) | null = null
  
  constructor() {
    // 初始化状态，加载已完成的引导
    this.state = {
      ...DEFAULT_STATE,
      completedGuides: loadCompletedGuides()
    }
  }
  
  /**
   * 设置状态变更回调
   */
  setOnStateChange(callback: (state: GuideState) => void): void {
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
   * 注册引导配置
   */
  registerGuide(config: GuideConfig): void {
    if (!config.id || !config.steps || config.steps.length === 0) {
      console.warn('[GuideService] 无效的引导配置:', config)
      return
    }
    this.guides.set(config.id, config)
  }
  
  /**
   * 批量注册引导配置
   */
  registerGuides(configs: GuideConfig[]): void {
    configs.forEach(config => this.registerGuide(config))
  }
  
  /**
   * 获取引导配置
   */
  getGuide(guideId: string): GuideConfig | undefined {
    return this.guides.get(guideId)
  }
  
  /**
   * 获取所有引导配置
   */
  getAllGuides(): GuideConfig[] {
    return Array.from(this.guides.values())
  }
  
  /**
   * 获取当前状态
   */
  getState(): GuideState {
    return { ...this.state }
  }
  
  /**
   * 获取当前引导配置
   */
  getCurrentGuide(): GuideConfig | undefined {
    if (!this.state.currentGuideId) return undefined
    return this.guides.get(this.state.currentGuideId)
  }
  
  /**
   * 获取当前步骤
   */
  getCurrentStep(): GuideStep | undefined {
    const guide = this.getCurrentGuide()
    if (!guide) return undefined
    return guide.steps[this.state.currentStepIndex]
  }
  
  /**
   * 检查引导是否已完成
   */
  isCompleted(guideId: string): boolean {
    return this.state.completedGuides.includes(guideId)
  }
  
  /**
   * 检查是否应该显示引导（首次使用检测）
   */
  shouldShowGuide(guideId: string): boolean {
    const guide = this.guides.get(guideId)
    if (!guide) return false
    
    // 如果设置了只触发一次且已完成，则不显示
    if (guide.triggerOnce && this.isCompleted(guideId)) {
      return false
    }
    
    return true
  }
  
  /**
   * 开始引导
   */
  start(guideId: string): void {
    const guide = this.guides.get(guideId)
    if (!guide) {
      console.warn(`[GuideService] 引导不存在: ${guideId}`)
      return
    }
    
    // 检查是否应该显示
    if (!this.shouldShowGuide(guideId)) {
      return
    }
    
    // 如果当前有正在进行的引导，先结束它
    if (this.state.isActive) {
      this.skip()
    }
    
    // 开始新引导
    this.state.currentGuideId = guideId
    this.state.currentStepIndex = 0
    this.state.isActive = true
    
    this.emitStateChange()
  }
  
  /**
   * 尝试开始引导（仅在首次使用时）
   */
  tryStart(guideId: string): boolean {
    if (this.shouldShowGuide(guideId)) {
      this.start(guideId)
      return true
    }
    return false
  }
  
  /**
   * 下一步
   */
  next(): void {
    if (!this.state.isActive || !this.state.currentGuideId) return
    
    const guide = this.guides.get(this.state.currentGuideId)
    if (!guide) return
    
    // 执行当前步骤的 action
    const currentStep = guide.steps[this.state.currentStepIndex]
    if (currentStep?.action) {
      try {
        currentStep.action()
      } catch (e) {
        console.warn('[GuideService] 步骤 action 执行失败:', e)
      }
    }
    
    // 检查是否是最后一步
    if (this.state.currentStepIndex >= guide.steps.length - 1) {
      this.complete()
      return
    }
    
    // 前进到下一步
    this.state.currentStepIndex++
    this.emitStateChange()
  }
  
  /**
   * 上一步
   */
  prev(): void {
    if (!this.state.isActive || this.state.currentStepIndex <= 0) return
    
    this.state.currentStepIndex--
    this.emitStateChange()
  }
  
  /**
   * 跳转到指定步骤
   */
  goToStep(stepIndex: number): void {
    if (!this.state.isActive || !this.state.currentGuideId) return
    
    const guide = this.guides.get(this.state.currentGuideId)
    if (!guide) return
    
    if (stepIndex < 0 || stepIndex >= guide.steps.length) {
      console.warn(`[GuideService] 无效的步骤索引: ${stepIndex}`)
      return
    }
    
    this.state.currentStepIndex = stepIndex
    this.emitStateChange()
  }
  
  /**
   * 跳过当前引导
   */
  skip(): void {
    if (!this.state.isActive) return
    
    // 重置状态但不标记为完成
    this.state.currentGuideId = null
    this.state.currentStepIndex = 0
    this.state.isActive = false
    
    this.emitStateChange()
  }
  
  /**
   * 完成当前引导
   */
  complete(): void {
    if (!this.state.isActive || !this.state.currentGuideId) return
    
    const guideId = this.state.currentGuideId
    // 标记为已完成
    this.markCompleted(guideId)
    
    // 重置状态
    this.state.currentGuideId = null
    this.state.currentStepIndex = 0
    this.state.isActive = false
    
    this.emitStateChange()
  }
  
  /**
   * 标记引导为已完成
   */
  markCompleted(guideId: string): void {
    if (!this.state.completedGuides.includes(guideId)) {
      this.state.completedGuides.push(guideId)
      saveCompletedGuides(this.state.completedGuides)
    }
  }
  
  /**
   * 重置引导完成状态
   */
  resetGuide(guideId: string): void {
    const index = this.state.completedGuides.indexOf(guideId)
    if (index > -1) {
      this.state.completedGuides.splice(index, 1)
      saveCompletedGuides(this.state.completedGuides)
    }
  }
  
  /**
   * 重置所有引导
   */
  resetAllGuides(): void {
    this.state.completedGuides = []
    saveCompletedGuides([])
    
    // 如果当前有正在进行的引导，也重置
    if (this.state.isActive) {
      this.state.currentGuideId = null
      this.state.currentStepIndex = 0
      this.state.isActive = false
    }
    
    this.emitStateChange()
  }
  
  /**
   * 获取引导进度
   */
  getProgress(): { current: number; total: number; percentage: number } {
    const guide = this.getCurrentGuide()
    if (!guide) {
      return { current: 0, total: 0, percentage: 0 }
    }
    
    const current = this.state.currentStepIndex + 1
    const total = guide.steps.length
    const percentage = Math.round((current / total) * 100)
    
    return { current, total, percentage }
  }
}

// ==================== 单例实例 ====================

/** 全局引导服务实例 */
let guideServiceInstance: GuideService | null = null

/**
 * 获取引导服务实例（单例）
 */
export function getGuideService(): GuideService {
  if (!guideServiceInstance) {
    guideServiceInstance = new GuideService()
  }
  return guideServiceInstance
}

/**
 * 创建新的引导服务实例
 */
export function createGuideService(): GuideService {
  return new GuideService()
}

// ==================== Vue Composable ====================

/**
 * 引导服务 Composable
 * 
 * 提供响应式的引导服务访问
 * 
 * @example
 * ```typescript
 * const { 
 *   isActive, 
 *   currentStep, 
 *   progress,
 *   start, 
 *   next, 
 *   skip 
 * } = useGuide()
 * 
 * // 首次使用时触发引导
 * onMounted(() => {
 *   tryStart('report-designer-guide')
 * })
 * ```
 */
export function useGuide() {
  const service = getGuideService()
  
  // 响应式状态
  const state = ref<GuideState>(service.getState())
  const currentGuide = ref<GuideConfig | undefined>(service.getCurrentGuide())
  const currentStep = ref<GuideStep | undefined>(service.getCurrentStep())
  
  // 监听状态变更
  service.setOnStateChange((newState) => {
    state.value = newState
    currentGuide.value = service.getCurrentGuide()
    currentStep.value = service.getCurrentStep()
  })
  
  // 计算属性
  const isActive = ref(state.value.isActive)
  const currentStepIndex = ref(state.value.currentStepIndex)
  const progress = ref(service.getProgress())
  
  // 更新计算属性
  const updateComputed = () => {
    isActive.value = state.value.isActive
    currentStepIndex.value = state.value.currentStepIndex
    progress.value = service.getProgress()
  }
  
  // 包装方法以更新响应式状态
  const start = (guideId: string) => {
    service.start(guideId)
    updateComputed()
  }
  
  const tryStart = (guideId: string): boolean => {
    const result = service.tryStart(guideId)
    updateComputed()
    return result
  }
  
  const next = () => {
    service.next()
    updateComputed()
  }
  
  const prev = () => {
    service.prev()
    updateComputed()
  }
  
  const skip = () => {
    service.skip()
    updateComputed()
  }
  
  const complete = () => {
    service.complete()
    updateComputed()
  }
  
  const goToStep = (stepIndex: number) => {
    service.goToStep(stepIndex)
    updateComputed()
  }
  
  return {
    // 状态
    state: readonly(state),
    isActive,
    currentGuide: readonly(currentGuide),
    currentStep: readonly(currentStep),
    currentStepIndex,
    progress,
    
    // 方法
    start,
    tryStart,
    next,
    prev,
    skip,
    complete,
    goToStep,
    
    // 服务方法
    registerGuide: (config: GuideConfig) => service.registerGuide(config),
    registerGuides: (configs: GuideConfig[]) => service.registerGuides(configs),
    getGuide: (guideId: string) => service.getGuide(guideId),
    isCompleted: (guideId: string) => service.isCompleted(guideId),
    shouldShowGuide: (guideId: string) => service.shouldShowGuide(guideId),
    markCompleted: (guideId: string) => service.markCompleted(guideId),
    resetGuide: (guideId: string) => service.resetGuide(guideId),
    resetAllGuides: () => service.resetAllGuides()
  }
}

// ==================== 导出 ====================

export type { GuideConfig, GuideState, GuideStep, IGuideService }


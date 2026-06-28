/**
 * 功能引导系统类型定义
 * Feature Guide System Type Definitions
 * 
 * 需求 19.1: WHEN 用户首次使用某功能时，THE DataTeaCup SHALL 显示功能引导教程
 */

/**
 * 引导步骤位置
 */
export type GuidePlacement = 'top' | 'bottom' | 'left' | 'right' | 'center'

/**
 * 引导步骤接口
 */
export interface GuideStep {
  /** 目标元素 CSS 选择器 */
  target: string
  /** 步骤标题 */
  title: string
  /** 步骤内容 */
  content: string
  /** 提示框位置 */
  placement: GuidePlacement
  /** 步骤完成后的回调 */
  action?: () => void
  /** 是否允许点击目标元素 */
  allowInteraction?: boolean
  /** 自定义偏移量 */
  offset?: { x: number; y: number }
  /** 高亮区域内边距 */
  highlightPadding?: number
}

/**
 * 引导配置接口
 */
export interface GuideConfig {
  /** 引导唯一标识 */
  id: string
  /** 引导名称 */
  name: string
  /** 引导步骤列表 */
  steps: GuideStep[]
  /** 是否只触发一次 */
  triggerOnce: boolean
  /** 引导描述 */
  description?: string
  /** 关联的功能/页面 */
  feature?: string
}

/**
 * 引导状态接口
 */
export interface GuideState {
  /** 当前引导 ID */
  currentGuideId: string | null
  /** 当前步骤索引 */
  currentStepIndex: number
  /** 是否正在显示引导 */
  isActive: boolean
  /** 已完成的引导 ID 列表 */
  completedGuides: string[]
}

/**
 * 引导服务接口
 */
export interface IGuideService {
  /** 开始引导 */
  start(guideId: string): void
  /** 跳过当前引导 */
  skip(): void
  /** 下一步 */
  next(): void
  /** 上一步 */
  prev(): void
  /** 完成引导 */
  complete(): void
  /** 检查引导是否已完成 */
  isCompleted(guideId: string): boolean
  /** 标记引导为已完成 */
  markCompleted(guideId: string): void
  /** 重置引导完成状态 */
  resetGuide(guideId: string): void
  /** 重置所有引导 */
  resetAllGuides(): void
  /** 注册引导配置 */
  registerGuide(config: GuideConfig): void
  /** 获取引导配置 */
  getGuide(guideId: string): GuideConfig | undefined
  /** 获取当前状态 */
  getState(): GuideState
}

/**
 * 高亮区域信息
 */
export interface HighlightRect {
  top: number
  left: number
  width: number
  height: number
  padding: number
}

/**
 * 提示框位置信息
 */
export interface TooltipPosition {
  top: number
  left: number
  placement: GuidePlacement
  arrowPosition: 'start' | 'center' | 'end'
}


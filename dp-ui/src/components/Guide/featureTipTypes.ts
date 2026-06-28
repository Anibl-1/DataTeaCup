/**
 * 功能提示类型定义
 * Feature Tip Type Definitions
 * 
 * 需求 19.2: THE DataTeaCup SHALL 为复杂功能提供上下文相关的使用提示
 */

/**
 * 提示类型
 */
export type TipType = 'info' | 'warning' | 'tip'

/**
 * 提示位置
 */
export type TipPlacement = 
  | 'top' 
  | 'bottom' 
  | 'left' 
  | 'right' 
  | 'top-start' 
  | 'top-end' 
  | 'bottom-start' 
  | 'bottom-end'

/**
 * 功能提示配置
 */
export interface FeatureTipConfig {
  /** 提示唯一标识 */
  id: string
  /** 提示标题 */
  title: string
  /** 提示内容 */
  content: string
  /** 提示类型 */
  type: TipType
  /** 是否可关闭 */
  dismissible: boolean
  /** 关联的功能/页面 */
  feature?: string
  /** 示例文本 */
  example?: string
  /** 相关链接 */
  link?: {
    text: string
    url: string
  }
}

/**
 * 功能提示状态
 */
export interface FeatureTipState {
  /** 已关闭的提示 ID 列表 */
  dismissedTips: string[]
}

/**
 * 功能提示服务接口
 */
export interface IFeatureTipService {
  /** 检查提示是否已关闭 */
  isDismissed(tipId: string): boolean
  /** 关闭提示 */
  dismiss(tipId: string): void
  /** 重置提示（重新显示） */
  reset(tipId: string): void
  /** 重置所有提示 */
  resetAll(): void
  /** 获取提示配置 */
  getTip(tipId: string): FeatureTipConfig | undefined
  /** 注册提示配置 */
  registerTip(config: FeatureTipConfig): void
  /** 获取功能相关的提示 */
  getTipsByFeature(feature: string): FeatureTipConfig[]
}

/**
 * FeatureTip 组件 Props
 */
export interface FeatureTipProps {
  /** 提示 ID（用于持久化关闭状态） */
  tipId?: string
  /** 提示标题 */
  title?: string
  /** 提示内容 */
  content: string
  /** 提示类型 */
  type?: TipType
  /** 是否可关闭 */
  dismissible?: boolean
  /** 显示位置 */
  placement?: TipPlacement
  /** 示例文本 */
  example?: string
  /** 相关链接 */
  link?: {
    text: string
    url: string
  }
  /** 是否显示图标 */
  showIcon?: boolean
  /** 触发方式 */
  trigger?: 'hover' | 'click' | 'manual'
  /** 最大宽度 */
  maxWidth?: number
  /** 是否内联显示 */
  inline?: boolean
}

/**
 * useFeatureTip 返回类型
 */
export interface UseFeatureTipReturn {
  /** 检查提示是否已关闭 */
  isDismissed: (tipId: string) => boolean
  /** 关闭提示 */
  dismiss: (tipId: string) => void
  /** 重置提示 */
  reset: (tipId: string) => void
  /** 重置所有提示 */
  resetAll: () => void
  /** 注册提示配置 */
  registerTip: (config: FeatureTipConfig) => void
  /** 批量注册提示配置 */
  registerTips: (configs: FeatureTipConfig[]) => void
  /** 获取提示配置 */
  getTip: (tipId: string) => FeatureTipConfig | undefined
  /** 获取功能相关的提示 */
  getTipsByFeature: (feature: string) => FeatureTipConfig[]
  /** 已关闭的提示列表 */
  dismissedTips: string[]
}


/**
 * DataTeaCup Theme Type Definitions
 * 主题类型定义 - 完整的主题配置接口
 * 
 * Validates: Requirements 1.1
 */

// ============================================================================
// Brand Configuration
// ============================================================================

export interface BrandConfig {
  name: string
  slogan: string
  fullName: string
  copyright: string
}

// ============================================================================
// Color Configuration
// ============================================================================

export interface GradientConfig {
  primary: string
  secondary: string
  success: string
  dark: string
  light: string
}

export interface BackgroundConfig {
  primary: string
  secondary: string
  tertiary: string
  dark: string
  darkSecondary: string
}

export interface TextColorConfig {
  primary: string
  secondary: string
  tertiary: string
  inverse: string
  link: string
}

export interface BorderColorConfig {
  light: string
  default: string
  dark: string
}

export interface ColorConfig {
  primary: string
  secondary: string
  success: string
  warning: string
  error: string
  info: string
  gradients: GradientConfig
  background: BackgroundConfig
  text: TextColorConfig
  border: BorderColorConfig
}

// ============================================================================
// Spacing Configuration
// ============================================================================

export interface SpacingConfig {
  xs: string
  sm: string
  md: string
  lg: string
  xl: string
  xxl: string
}

// ============================================================================
// Border Radius Configuration
// ============================================================================

export interface BorderRadiusConfig {
  sm: string
  md: string
  lg: string
  xl: string
  full: string
}

// ============================================================================
// Shadow Configuration
// ============================================================================

export interface ShadowConfig {
  sm: string
  md: string
  lg: string
  xl: string
  inner: string
  none: string
}

// ============================================================================
// Typography Configuration
// ============================================================================

export interface FontFamilyConfig {
  sans: string
  mono: string
}

export interface FontSizeConfig {
  xs: string
  sm: string
  base: string
  lg: string
  xl: string
  '2xl': string
  '3xl': string
  '4xl': string
}

export interface FontWeightConfig {
  normal: number
  medium: number
  semibold: number
  bold: number
}

export interface TypographyConfig {
  fontFamily: FontFamilyConfig
  fontSize: FontSizeConfig
  fontWeight: FontWeightConfig
}

// ============================================================================
// Transition Configuration
// ============================================================================

export interface TransitionConfig {
  fast: string
  base: string
  slow: string
}

// ============================================================================
// Breakpoint Configuration (New)
// 响应式断点配置 - 支持 1280/1366/1440/1920 四种屏幕尺寸
// ============================================================================

export interface BreakpointConfig {
  /** Extra small screens - 1280px */
  xs: number
  /** Small screens - 1366px */
  sm: number
  /** Medium screens - 1440px */
  md: number
  /** Large screens - 1920px */
  lg: number
}

// ============================================================================
// Accessibility Configuration (New)
// 无障碍配置 - 支持高对比度模式和减少动画
// ============================================================================

export interface AccessibilityConfig {
  /** 高对比度模式 */
  highContrast: boolean
  /** 减少动画模式 */
  reducedMotion: boolean
  /** 焦点环宽度 */
  focusRingWidth: string
  /** 焦点环颜色 */
  focusRingColor: string
  /** 焦点环偏移 */
  focusRingOffset?: string
  /** 最小点击目标尺寸 (WCAG 2.1 要求至少 44px) */
  minTouchTarget?: string
}

// ============================================================================
// Animation Configuration (New)
// 动画配置 - 悬停、展开、加载动画
// ============================================================================

export interface AnimationDef {
  /** 动画持续时间 */
  duration: string
  /** 动画缓动函数 */
  easing: string
  /** 动画延迟 */
  delay?: string
}

export interface HoverAnimationConfig extends AnimationDef {
  /** 缩放比例 */
  scale?: number
  /** 透明度变化 */
  opacity?: number
  /** 位移 */
  translateY?: string
}

export interface ExpandAnimationConfig extends AnimationDef {
  /** 展开方向 */
  direction?: 'vertical' | 'horizontal'
}

export interface LoadingAnimationConfig extends AnimationDef {
  /** 加载动画类型 */
  type?: 'spin' | 'pulse' | 'skeleton'
}

export interface AnimationConfig {
  /** 悬停动画配置 */
  hover: HoverAnimationConfig
  /** 展开/收起动画配置 */
  expand: ExpandAnimationConfig
  /** 加载动画配置 */
  loading: LoadingAnimationConfig
  /** 淡入淡出动画 */
  fade?: AnimationDef
  /** 滑动动画 */
  slide?: AnimationDef
}

// ============================================================================
// Complete Theme Configuration Interface
// ============================================================================

export interface ThemeConfig {
  /** 品牌配置 */
  brand: BrandConfig
  /** 颜色配置 */
  colors: ColorConfig
  /** 间距配置 */
  spacing: SpacingConfig
  /** 圆角配置 */
  borderRadius: BorderRadiusConfig
  /** 阴影配置 */
  shadows: ShadowConfig
  /** 字体配置 */
  typography: TypographyConfig
  /** 过渡动画配置 */
  transitions: TransitionConfig
  /** 响应式断点配置 */
  breakpoints: BreakpointConfig
  /** 无障碍配置 */
  accessibility: AccessibilityConfig
  /** 动画配置 */
  animations: AnimationConfig
}

// ============================================================================
// Theme Variant Types
// ============================================================================

export type ThemeMode = 'light' | 'dark' | 'system'

export type BreakpointKey = keyof BreakpointConfig

export type SpacingKey = keyof SpacingConfig

export type ShadowKey = keyof ShadowConfig

export type BorderRadiusKey = keyof BorderRadiusConfig

// ============================================================================
// High Contrast Color Configuration (New)
// 高对比度配色方案 - 供视力障碍用户使用
// Validates: Requirements 1.8
// ============================================================================

export interface HighContrastColorScheme {
  /** 主要文本颜色 */
  textPrimary: string
  /** 次要文本颜色 */
  textSecondary: string
  /** 第三级文本颜色 */
  textTertiary: string
  /** 主要背景颜色 */
  bgPrimary: string
  /** 次要背景颜色 */
  bgSecondary: string
  /** 第三级背景颜色 */
  bgTertiary: string
  /** 主色调 */
  primary: string
  /** 主色调悬停 */
  primaryHover: string
  /** 主色调按下 */
  primaryPressed: string
  /** 成功颜色 */
  success: string
  /** 警告颜色 */
  warning: string
  /** 错误颜色 */
  error: string
  /** 信息颜色 */
  info: string
  /** 浅色边框 */
  borderLight: string
  /** 默认边框 */
  borderDefault: string
  /** 深色边框 */
  borderDark: string
  /** 焦点环颜色 */
  focusRing: string
  /** 焦点环宽度 */
  focusRingWidth: string
  /** 焦点环偏移 */
  focusRingOffset: string
}

export interface HighContrastColors {
  /** 亮色模式高对比度配色 */
  light: HighContrastColorScheme
  /** 暗色模式高对比度配色 */
  dark: HighContrastColorScheme
}

// ============================================================================
// Utility Types
// ============================================================================

/** 深度只读类型 */
export type DeepReadonly<T> = {
  readonly [P in keyof T]: T[P] extends object ? DeepReadonly<T[P]> : T[P]
}

/** 只读主题配置 */
export type ReadonlyThemeConfig = DeepReadonly<ThemeConfig>

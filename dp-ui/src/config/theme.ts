/**
 * DataTeaCup Theme Configuration
 * 主题配置 - 采用现代化商业级配色方案
 * 
 * Validates: Requirements 1.1
 */

import type { ThemeConfig } from '@/types/theme'

export const themeConfig: ThemeConfig = {
  // 品牌信息
  brand: {
    name: 'DataTeaCup',
    slogan: 'Empower Your Data Intelligence',
    fullName: 'DataTeaCup',
    copyright: '© 2026 DataTeaCup. All rights reserved.'
  },

  // 主题色彩
  colors: {
    primary: '#0066FF',        // 主色 - 专业蓝
    secondary: '#00D4FF',      // 辅助色 - 青色
    success: '#10B981',        // 成功 - 绿色
    warning: '#F59E0B',        // 警告 - 橙色
    error: '#EF4444',          // 错误 - 红色
    info: '#3B82F6',           // 信息 - 蓝色
    
    // 渐变色
    gradients: {
      primary: 'linear-gradient(135deg, #0066FF 0%, #00D4FF 100%)',
      secondary: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      success: 'linear-gradient(135deg, #10B981 0%, #059669 100%)',
      dark: 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)',
      light: 'linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%)'
    },

    // 背景色
    background: {
      primary: '#FFFFFF',
      secondary: '#F8FAFC',
      tertiary: '#F1F5F9',
      dark: '#0F172A',
      darkSecondary: '#1E293B'
    },

    // 文本色
    text: {
      primary: '#1E293B',
      secondary: '#64748B',
      tertiary: '#94A3B8',
      inverse: '#FFFFFF',
      link: '#0066FF'
    },

    // 边框色
    border: {
      light: '#E2E8F0',
      default: '#CBD5E1',
      dark: '#94A3B8'
    }
  },

  // 间距
  spacing: {
    xs: '4px',
    sm: '8px',
    md: '16px',
    lg: '24px',
    xl: '32px',
    xxl: '48px'
  },

  // 圆角
  borderRadius: {
    sm: '4px',
    md: '8px',
    lg: '12px',
    xl: '16px',
    full: '9999px'
  },

  // 阴影
  shadows: {
    sm: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
    md: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
    lg: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)',
    xl: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
    inner: 'inset 0 2px 4px 0 rgba(0, 0, 0, 0.06)',
    none: 'none'
  },

  // 字体
  typography: {
    fontFamily: {
      sans: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
      mono: 'ui-monospace, SFMono-Regular, "SF Mono", Consolas, "Liberation Mono", Menlo, monospace'
    },
    fontSize: {
      xs: '12px',
      sm: '14px',
      base: '16px',
      lg: '18px',
      xl: '20px',
      '2xl': '24px',
      '3xl': '30px',
      '4xl': '36px'
    },
    fontWeight: {
      normal: 400,
      medium: 500,
      semibold: 600,
      bold: 700
    }
  },

  // 动画
  transitions: {
    fast: '150ms cubic-bezier(0.4, 0, 0.2, 1)',
    base: '200ms cubic-bezier(0.4, 0, 0.2, 1)',
    slow: '300ms cubic-bezier(0.4, 0, 0.2, 1)'
  },

  // 响应式断点 - 支持 1280/1366/1440/1920 四种屏幕尺寸
  breakpoints: {
    xs: 1280,  // 小屏幕
    sm: 1366,  // 中小屏幕
    md: 1440,  // 中等屏幕
    lg: 1920   // 大屏幕
  },

  // 无障碍配置 - 支持高对比度模式和减少动画
  accessibility: {
    highContrast: false,
    reducedMotion: false,
    focusRingWidth: '2px',
    focusRingColor: '#0066FF',
    focusRingOffset: '2px',
    minTouchTarget: '44px'
  },

  // 动画配置 - 悬停、展开、加载动画
  animations: {
    // 悬停动画 - 过渡时间不超过200毫秒
    hover: {
      duration: '200ms',
      easing: 'cubic-bezier(0.4, 0, 0.2, 1)',
      scale: 1.02,
      opacity: 0.9,
      translateY: '-2px'
    },
    // 展开/收起动画
    expand: {
      duration: '300ms',
      easing: 'cubic-bezier(0.4, 0, 0.2, 1)',
      direction: 'vertical'
    },
    // 加载动画
    loading: {
      duration: '1500ms',
      easing: 'linear',
      type: 'skeleton'
    },
    // 淡入淡出动画
    fade: {
      duration: '200ms',
      easing: 'ease-in-out'
    },
    // 滑动动画
    slide: {
      duration: '300ms',
      easing: 'cubic-bezier(0.4, 0, 0.2, 1)'
    }
  }
}

export default themeConfig

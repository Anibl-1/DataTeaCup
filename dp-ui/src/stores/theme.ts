/**
 * 主题管理 Store
 * 支持亮色/暗色主题切换和高对比度模式
 * 
 * Validates: Requirements 1.8
 */
import { defineStore } from 'pinia'
import { ref, watch, computed } from 'vue'
import { darkTheme, type GlobalTheme } from 'naive-ui'

export type ThemeMode = 'light' | 'dark' | 'system'

/** 预设主题色方案 */
export interface ColorPreset {
  name: string
  label: string
  primary: string
  primaryHover: string
  primaryPressed: string
}

export const COLOR_PRESETS: ColorPreset[] = [
  { name: 'blue',   label: '默认蓝', primary: '#409EFF', primaryHover: '#66B1FF', primaryPressed: '#3A8EE6' },
  { name: 'green',  label: '翠绿',   primary: '#18a058', primaryHover: '#36ad6a', primaryPressed: '#0c7a43' },
  { name: 'purple', label: '典雅紫', primary: '#722ed1', primaryHover: '#9254de', primaryPressed: '#531dab' },
  { name: 'orange', label: '活力橙', primary: '#f0a020', primaryHover: '#fcb040', primaryPressed: '#d48806' },
  { name: 'red',    label: '中国红', primary: '#d03050', primaryHover: '#de576d', primaryPressed: '#ab1f3f' },
  { name: 'cyan',   label: '科技青', primary: '#2080f0', primaryHover: '#4098fc', primaryPressed: '#1060c9' },
]

/**
 * 高对比度配色方案
 * 符合 WCAG 2.1 AAA 标准，对比度至少 7:1
 */
export const highContrastColors = {
  light: {
    // 文本颜色 - 纯黑色确保最高对比度
    textPrimary: '#000000',
    textSecondary: '#1a1a1a',
    textTertiary: '#333333',
    // 背景颜色 - 纯白色
    bgPrimary: '#FFFFFF',
    bgSecondary: '#F5F5F5',
    bgTertiary: '#EBEBEB',
    // 主色调 - 深蓝色确保高对比度
    primary: '#0000CC',
    primaryHover: '#0000AA',
    primaryPressed: '#000088',
    // 状态颜色 - 更深的颜色确保可见性
    success: '#006600',
    warning: '#CC6600',
    error: '#CC0000',
    info: '#0000CC',
    // 边框 - 更粗更深的边框
    borderLight: '#666666',
    borderDefault: '#333333',
    borderDark: '#000000',
    // 焦点指示器 - 更明显的焦点环
    focusRing: '#0000CC',
    focusRingWidth: '3px',
    focusRingOffset: '2px'
  },
  dark: {
    // 文本颜色 - 纯白色确保最高对比度
    textPrimary: '#FFFFFF',
    textSecondary: '#F0F0F0',
    textTertiary: '#E0E0E0',
    // 背景颜色 - 纯黑色
    bgPrimary: '#000000',
    bgSecondary: '#0D0D0D',
    bgTertiary: '#1A1A1A',
    // 主色调 - 亮蓝色确保高对比度
    primary: '#66B3FF',
    primaryHover: '#99CCFF',
    primaryPressed: '#3399FF',
    // 状态颜色 - 更亮的颜色确保可见性
    success: '#66FF66',
    warning: '#FFCC00',
    error: '#FF6666',
    info: '#66B3FF',
    // 边框 - 更粗更亮的边框
    borderLight: '#999999',
    borderDefault: '#CCCCCC',
    borderDark: '#FFFFFF',
    // 焦点指示器 - 更明显的焦点环
    focusRing: '#FFFF00',
    focusRingWidth: '3px',
    focusRingOffset: '2px'
  }
} as const

export const useThemeStore = defineStore('theme', () => {
  // 主题模式
  const mode = ref<ThemeMode>(getStoredTheme())
  
  // 系统主题
  const systemDark = ref(window.matchMedia('(prefers-color-scheme: dark)').matches)
  
  // 系统高对比度偏好
  const systemHighContrast = ref(window.matchMedia('(prefers-contrast: more)').matches)
  
  // 高对比度模式状态
  const highContrastMode = ref<boolean>(getStoredHighContrast())
  
  // 主题色
  const primaryColor = ref<string>(getStoredPrimaryColor())
  const currentPreset = computed(() => COLOR_PRESETS.find(p => p.primary === primaryColor.value) || COLOR_PRESETS[0]!)
  
  // 实际是否为暗色
  const isDark = computed(() => {
    if (mode.value === 'system') {
      return systemDark.value
    }
    return mode.value === 'dark'
  })
  
  // 实际是否启用高对比度
  const isHighContrast = computed(() => {
    return highContrastMode.value || systemHighContrast.value
  })
  
  // 当前高对比度配色方案
  const currentHighContrastColors = computed(() => {
    return isDark.value ? highContrastColors.dark : highContrastColors.light
  })
  
  // Naive UI 主题
  const naiveTheme = computed<GlobalTheme | null>(() => {
    return isDark.value ? darkTheme : null
  })
  
  // 主题覆盖配置
  const themeOverrides = computed(() => {
    const preset = COLOR_PRESETS.find(p => p.primary === primaryColor.value)
    const pColor = preset?.primary || primaryColor.value
    const pHover = preset?.primaryHover || adjustColor(pColor, 20)
    const pPressed = preset?.primaryPressed || adjustColor(pColor, -15)
    const baseOverrides: Record<string, any> = {
      common: {
        primaryColor: pColor,
        primaryColorHover: pHover,
        primaryColorPressed: pPressed,
        primaryColorSuppl: pColor,
        borderRadius: '8px',
        borderRadiusSmall: '6px',
        fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
        fontSize: '14px',
        fontSizeMedium: '14px',
        fontSizeSmall: '13px',
        fontSizeLarge: '15px',
        heightMedium: '36px',
        heightSmall: '30px',
        heightLarge: '42px',
        // 暗色模式下的背景色 - 统一暗黑模式配色
        ...(isDark.value ? {
          bodyColor: '#111827',
          cardColor: '#1e293b',
          modalColor: '#1e293b',
          popoverColor: '#1e293b',
          tableColor: '#1e293b',
          inputColor: '#111827',
          tableHeaderColor: '#1e293b',
          borderColor: '#334155',
          dividerColor: '#334155',
          hoverColor: 'rgba(255, 255, 255, 0.06)',
          textColorBase: '#f8fafc',
          textColor1: '#f8fafc',
          textColor2: '#cbd5e1',
          textColor3: '#94a3b8',
          closeIconColor: '#94a3b8',
          closeIconColorHover: '#cbd5e1',
          closeColorHover: 'rgba(255, 255, 255, 0.08)',
          clearColor: '#94a3b8',
          clearColorHover: '#cbd5e1',
          clearColorPressed: '#f8fafc'
        } : {})
      },
      Card: {
        borderRadius: '12px',
        paddingMedium: '20px',
        paddingSmall: '14px',
        titleFontSizeMedium: '16px',
        titleFontWeight: '600',
        ...(isDark.value ? {
          color: '#1e293b',
          borderColor: '#334155',
        } : {
          borderColor: '#e8ecf1',
          boxShadow: '0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.02)',
        })
      },
      Button: {
        borderRadiusMedium: '8px',
        borderRadiusSmall: '6px',
        borderRadiusLarge: '10px',
        fontWeight: '500',
        fontSizeMedium: '14px',
        heightMedium: '36px',
        paddingMedium: '0 18px',
      },
      Input: {
        borderRadius: '8px',
        heightMedium: '36px',
        fontSizeMedium: '14px',
        ...(isDark.value ? {
          color: '#111827',
          colorFocus: '#111827',
          border: '1px solid #334155',
          borderHover: `1px solid ${pColor}`,
          borderFocus: `1px solid ${pColor}`,
          caretColor: pColor,
          placeholderColor: '#475569',
          textColor: '#e2e8f0',
        } : {
          border: '1px solid #d1d5db',
          borderHover: `1px solid ${pColor}`,
          borderFocus: `1px solid ${pColor}`,
        })
      },
      DataTable: {
        borderRadius: '10px',
        fontSizeMedium: '13px',
        thFontWeight: '600',
        ...(isDark.value ? {
          thColor: '#1e293b',
          tdColor: '#111827',
          tdColorStriped: '#162032',
          tdColorHover: 'rgba(255, 255, 255, 0.04)',
          borderColor: '#334155',
          thTextColor: '#e2e8f0',
          tdTextColor: '#cbd5e1',
        } : {
          thColor: '#f8fafc',
          tdColorStriped: '#fafbfd',
          tdColorHover: '#f0f7ff',
          borderColor: '#eef1f6',
          thTextColor: '#475569',
        })
      },
      Tag: {
        borderRadius: '6px',
        fontSizeSmall: '12px',
        fontSizeMedium: '13px',
        heightSmall: '22px',
        heightMedium: '26px',
      },
      Dialog: {
        borderRadius: '14px',
        padding: '24px',
        titleFontSize: '17px',
        titleFontWeight: '600',
        ...(isDark.value ? {
          color: '#1e293b',
          textColor: '#e2e8f0',
        } : {})
      },
      Drawer: {
        borderRadius: '0',
        headerPadding: '16px 24px',
        bodyPadding: '16px 24px',
        ...(isDark.value ? {
          color: '#1e293b',
        } : {})
      },
      Tabs: {
        tabFontSizeMedium: '14px',
        tabFontWeight: '500',
        tabFontWeightActive: '600',
      },
      Message: {
        borderRadius: '10px',
        padding: '10px 16px',
        fontSize: '14px',
      },
      Notification: {
        borderRadius: '12px',
        padding: '16px 20px',
      },
      Pagination: {
        itemBorderRadius: '8px',
        itemFontSize: '13px',
        ...(isDark.value ? {
          itemColor: '#1e293b',
          itemColorHover: 'rgba(255, 255, 255, 0.06)',
          itemColorActive: 'rgba(59, 130, 246, 0.12)',
          itemTextColor: '#94a3b8',
          itemTextColorActive: pColor,
          itemBorderColor: '#334155',
        } : {})
      },
      Form: {
        labelFontWeight: '500',
        labelFontSizeTopMedium: '13px',
        ...(isDark.value ? {
          labelTextColor: '#94a3b8',
        } : {
          labelTextColor: '#475569',
        })
      },
      Select: {
        ...(isDark.value ? {
          peers: {
            InternalSelection: {
              color: '#111827',
              border: '1px solid #334155',
              borderHover: `1px solid ${pColor}`,
              borderFocus: `1px solid ${pColor}`,
              borderActive: `1px solid ${pColor}`,
              textColor: '#e2e8f0',
              placeholderColor: '#475569',
            }
          }
        } : {})
      },
      Popover: {
        borderRadius: '10px',
        padding: '12px 16px',
        ...(isDark.value ? {
          color: '#1e293b',
        } : {
          boxShadow: '0 4px 16px rgba(0,0,0,0.08), 0 2px 6px rgba(0,0,0,0.04)',
        })
      },
      Tooltip: {
        borderRadius: '8px',
        padding: '8px 12px',
      },
      Dropdown: {
        borderRadius: '10px',
        optionBorderRadius: '6px',
        padding: '6px',
        ...(isDark.value ? {
          color: '#1e293b',
          optionColorHover: 'rgba(255, 255, 255, 0.06)',
          optionTextColor: '#e2e8f0',
        } : {
          boxShadow: '0 4px 16px rgba(0,0,0,0.08), 0 2px 6px rgba(0,0,0,0.04)',
        })
      },
      Breadcrumb: {
        fontSize: '14px',
        fontWeight: '400',
        fontWeightActive: '600',
      },
      Switch: {
        railHeight: '22px',
        railWidth: '44px',
        buttonHeight: '18px',
        buttonWidth: '18px',
      },
      Badge: {
        fontSize: '11px',
      },
      Skeleton: {
        borderRadius: '6px',
        ...(isDark.value ? {
          color: '#263348',
          colorEnd: '#334155',
        } : {})
      },
      Result: {
        titleFontSizeMedium: '18px',
        fontSizeMedium: '14px',
      },
    }
    
    // 高对比度模式覆盖
    if (isHighContrast.value) {
      const hcColors = currentHighContrastColors.value
      return {
        common: {
          ...baseOverrides['common'],
          // 主色调
          primaryColor: hcColors.primary,
          primaryColorHover: hcColors.primaryHover,
          primaryColorPressed: hcColors.primaryPressed,
          primaryColorSuppl: hcColors.primary,
          // 状态颜色
          successColor: hcColors.success,
          warningColor: hcColors.warning,
          errorColor: hcColors.error,
          infoColor: hcColors.info,
          // 文本颜色
          textColorBase: hcColors.textPrimary,
          textColor1: hcColors.textPrimary,
          textColor2: hcColors.textSecondary,
          textColor3: hcColors.textTertiary,
          // 背景颜色
          bodyColor: hcColors.bgPrimary,
          cardColor: hcColors.bgSecondary,
          modalColor: hcColors.bgSecondary,
          popoverColor: hcColors.bgSecondary,
          tableColor: hcColors.bgSecondary,
          inputColor: hcColors.bgTertiary,
          // 边框颜色
          borderColor: hcColors.borderDefault,
          dividerColor: hcColors.borderLight
        },
        Button: {
          // 更粗的边框
          borderRadiusMedium: '4px',
          textColor: hcColors.textPrimary,
          border: `2px solid ${hcColors.borderDefault}`
        },
        Input: {
          // 更明显的输入框边框
          border: `2px solid ${hcColors.borderDefault}`,
          borderHover: `2px solid ${hcColors.primary}`,
          borderFocus: `2px solid ${hcColors.primary}`
        },
        Card: {
          // 更明显的卡片边框
          borderColor: hcColors.borderDefault,
          borderRadius: '4px'
        },
        Table: {
          // 更明显的表格边框
          borderColor: hcColors.borderDefault,
          thColor: hcColors.bgTertiary,
          tdColor: hcColors.bgPrimary
        }
      }
    }
    
    return baseOverrides
  })
  
  // 切换主题
  function setTheme(newMode: ThemeMode) {
    mode.value = newMode
    localStorage.setItem('theme-mode', newMode)
    applyTheme()
  }
  
  // 切换暗色模式
  function toggleDark() {
    if (mode.value === 'system') {
      setTheme(systemDark.value ? 'light' : 'dark')
    } else {
      setTheme(mode.value === 'dark' ? 'light' : 'dark')
    }
  }
  
  /**
   * 切换高对比度模式
   * Validates: Requirements 1.8
   */
  function toggleHighContrast() {
    setHighContrast(!highContrastMode.value)
  }
  
  /**
   * 设置高对比度模式
   * @param enabled 是否启用高对比度模式
   * Validates: Requirements 1.8
   */
  function setHighContrast(enabled: boolean) {
    highContrastMode.value = enabled
    localStorage.setItem('high-contrast-mode', String(enabled))
    applyHighContrastMode()
  }
  
  // 应用主题到 DOM
  function applyTheme() {
    const html = document.documentElement
    if (isDark.value) {
      html.classList.add('dark')
      html.style.colorScheme = 'dark'
    } else {
      html.classList.remove('dark')
      html.style.colorScheme = 'light'
    }
    // 同时应用高对比度模式
    applyHighContrastMode()
  }
  
  /**
   * 应用高对比度模式到 DOM
   * Validates: Requirements 1.8
   */
  function applyHighContrastMode() {
    const html = document.documentElement
    const hcColors = currentHighContrastColors.value
    
    if (isHighContrast.value) {
      html.classList.add('high-contrast')
      // 设置 CSS 变量用于高对比度模式
      html.style.setProperty('--hc-text-primary', hcColors.textPrimary)
      html.style.setProperty('--hc-text-secondary', hcColors.textSecondary)
      html.style.setProperty('--hc-text-tertiary', hcColors.textTertiary)
      html.style.setProperty('--hc-bg-primary', hcColors.bgPrimary)
      html.style.setProperty('--hc-bg-secondary', hcColors.bgSecondary)
      html.style.setProperty('--hc-bg-tertiary', hcColors.bgTertiary)
      html.style.setProperty('--hc-primary', hcColors.primary)
      html.style.setProperty('--hc-primary-hover', hcColors.primaryHover)
      html.style.setProperty('--hc-primary-pressed', hcColors.primaryPressed)
      html.style.setProperty('--hc-success', hcColors.success)
      html.style.setProperty('--hc-warning', hcColors.warning)
      html.style.setProperty('--hc-error', hcColors.error)
      html.style.setProperty('--hc-info', hcColors.info)
      html.style.setProperty('--hc-border-light', hcColors.borderLight)
      html.style.setProperty('--hc-border-default', hcColors.borderDefault)
      html.style.setProperty('--hc-border-dark', hcColors.borderDark)
      html.style.setProperty('--hc-focus-ring', hcColors.focusRing)
      html.style.setProperty('--hc-focus-ring-width', hcColors.focusRingWidth)
      html.style.setProperty('--hc-focus-ring-offset', hcColors.focusRingOffset)
    } else {
      html.classList.remove('high-contrast')
      // 移除高对比度 CSS 变量
      const hcVars = [
        '--hc-text-primary', '--hc-text-secondary', '--hc-text-tertiary',
        '--hc-bg-primary', '--hc-bg-secondary', '--hc-bg-tertiary',
        '--hc-primary', '--hc-primary-hover', '--hc-primary-pressed',
        '--hc-success', '--hc-warning', '--hc-error', '--hc-info',
        '--hc-border-light', '--hc-border-default', '--hc-border-dark',
        '--hc-focus-ring', '--hc-focus-ring-width', '--hc-focus-ring-offset'
      ]
      hcVars.forEach(varName => html.style.removeProperty(varName))
    }
  }
  
  // 监听系统主题变化
  function initSystemThemeListener() {
    const darkMediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    darkMediaQuery.addEventListener('change', (e) => {
      systemDark.value = e.matches
      if (mode.value === 'system') {
        applyTheme()
      }
    })
    
    // 监听系统高对比度偏好变化
    const contrastMediaQuery = window.matchMedia('(prefers-contrast: more)')
    contrastMediaQuery.addEventListener('change', (e) => {
      systemHighContrast.value = e.matches
      applyHighContrastMode()
    })
  }
  
  // 初始化
  function init() {
    initSystemThemeListener()
    applyTheme()
  }
  
  // 监听变化
  watch(isDark, () => {
    applyTheme()
  })
  
  watch(isHighContrast, () => {
    applyHighContrastMode()
  })
  
  /**
   * 设置主题色
   */
  function setPrimaryColor(color: string) {
    primaryColor.value = color
    localStorage.setItem('theme-primary-color', color)
    applyPrimaryColor()
  }

  /**
   * 应用主题色 CSS 变量
   */
  function applyPrimaryColor() {
    const html = document.documentElement
    const preset = COLOR_PRESETS.find(p => p.primary === primaryColor.value)
    const pColor = preset?.primary || primaryColor.value
    const pHover = preset?.primaryHover || adjustColor(pColor, 20)
    const pPressed = preset?.primaryPressed || adjustColor(pColor, -15)
    html.style.setProperty('--color-primary', pColor)
    html.style.setProperty('--color-primary-hover', pHover)
    html.style.setProperty('--color-primary-pressed', pPressed)
    html.style.setProperty('--color-primary-light', pColor + '18')
    html.style.setProperty('--color-primary-bg', pColor + '0D')
    html.style.setProperty('--color-primary-border', pColor + '40')
    html.style.setProperty('--color-primary-gradient', `linear-gradient(135deg, ${pColor} 0%, ${pHover} 100%)`)
    // 同步更新 dp-* 设计令牌，让 Dashboard 等使用 dp- 变量的组件也跟随主题色
    html.style.setProperty('--dp-color-primary', pColor)
    html.style.setProperty('--dp-color-primary-hover', pHover)
    html.style.setProperty('--dp-color-primary-active', pPressed)
    html.style.setProperty('--dp-color-primary-light', pColor + '14')
    html.style.setProperty('--dp-color-primary-glow', pColor + '40')
    html.style.setProperty('--dp-gradient-primary', `linear-gradient(135deg, ${pColor} 0%, ${pPressed} 100%)`)
  }

  // 监听主题色变化
  watch(primaryColor, () => {
    applyPrimaryColor()
  })

  // 初始化时应用主题色
  const originalInit = init
  function initAll() {
    originalInit()
    applyPrimaryColor()
  }

  return {
    mode,
    isDark,
    naiveTheme,
    themeOverrides,
    setTheme,
    toggleDark,
    init: initAll,
    // 主题色相关
    primaryColor,
    currentPreset,
    setPrimaryColor,
    // 高对比度模式相关
    highContrastMode,
    isHighContrast,
    systemHighContrast,
    currentHighContrastColors,
    toggleHighContrast,
    setHighContrast
  }
})

// 获取存储的主题
function getStoredTheme(): ThemeMode {
  const stored = localStorage.getItem('theme-mode')
  if (stored === 'light' || stored === 'dark' || stored === 'system') {
    return stored
  }
  return 'light'
}

/**
 * 获取存储的高对比度模式设置
 * Validates: Requirements 1.8
 */
function getStoredHighContrast(): boolean {
  const stored = localStorage.getItem('high-contrast-mode')
  return stored === 'true'
}

function getStoredPrimaryColor(): string {
  return localStorage.getItem('theme-primary-color') || '#409EFF'
}

/** 简单调亮/调暗颜色 */
function adjustColor(hex: string, amount: number): string {
  const num = parseInt(hex.replace('#', ''), 16)
  const r = Math.min(255, Math.max(0, ((num >> 16) & 0xff) + amount))
  const g = Math.min(255, Math.max(0, ((num >> 8) & 0xff) + amount))
  const b = Math.min(255, Math.max(0, (num & 0xff) + amount))
  return '#' + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)
}

/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * i18n 国际化配置
 * 基于 vue-i18n 实现，支持中英文切换，集成 Naive UI 组件库国际化
 *
 * 需求 24.1: THE I18n_Manager SHALL 支持中文和英文两种语言的完整翻译
 */
import { ref, computed } from 'vue'
import { createI18n } from 'vue-i18n'
import {
  zhCN,
  enUS,
  dateZhCN,
  dateEnUS,
  type NLocale,
  type NDateLocale
} from 'naive-ui'
import zhCNMessages from './locales/zh-CN'
// en-US 语言包延迟加载，减少启动内存（~89KB）
let enUSMessages: Record<string, any> | null = null
const loadEnUSMessages = async (): Promise<Record<string, any>> => {
  if (!enUSMessages) {
    const mod = await import('./locales/en-US')
    enUSMessages = mod.default
  }
  return enUSMessages
}

// ====================== 类型定义 ======================
export type Locale = 'zh-CN' | 'en-US'

// ====================== vue-i18n 实例 ======================
/**
 * 创建 vue-i18n 实例
 * - legacy: false 使用 Composition API 模式
 * - globalInjection: true 使所有组件可以直接在模板中使用 $t()
 * - fallbackLocale: 'zh-CN' 中文作为兜底语言
 * - silentTranslationWarn: true 避免控制台翻译缺失警告刷屏
 */
export const i18n = createI18n({
  legacy: false,
  locale: 'zh-CN',
  fallbackLocale: 'zh-CN',
  globalInjection: true,
  silentTranslationWarn: true,
  silentFallbackWarn: true,
  messages: {
    'zh-CN': zhCNMessages,
  },
})

// ====================== 响应式状态 ======================
// 当前语言（与 vue-i18n 同步）
export const currentLocale = ref<Locale>('zh-CN')

// ====================== Naive UI 组件库语言配置 ======================
export const naiveLocale = computed<NLocale>(() => {
  return currentLocale.value === 'zh-CN' ? zhCN : enUS
})

export const naiveDateLocale = computed<NDateLocale>(() => {
  return currentLocale.value === 'zh-CN' ? dateZhCN : dateEnUS
})

// ====================== 日期/数字格式配置 ======================
export const dateFormats = {
  'zh-CN': {
    short: 'YYYY-MM-DD',
    long: 'YYYY年MM月DD日',
    datetime: 'YYYY-MM-DD HH:mm:ss',
    time: 'HH:mm:ss'
  },
  'en-US': {
    short: 'MM/DD/YYYY',
    long: 'MMMM DD, YYYY',
    datetime: 'MM/DD/YYYY HH:mm:ss',
    time: 'HH:mm:ss'
  }
}

export const numberFormats = {
  'zh-CN': {
    decimal: '.',
    thousands: ',',
    currency: '¥'
  },
  'en-US': {
    decimal: '.',
    thousands: ',',
    currency: '$'
  }
}

// ====================== 翻译函数 ======================
/**
 * 获取翻译文本（独立函数版本）
 * 委托给 vue-i18n 实例，可在非组件上下文中使用
 */
export const t = (key: string, params?: Record<string, any>): string => {
  const global = i18n.global as any
  if (params) {
    return global.t(key, params)
  }
  return global.t(key)
}

// ====================== 语言切换 ======================
export const LOCALE_CHANGE_EVENT = 'locale-change'

/**
 * 切换语言
 * 同步更新 vue-i18n、currentLocale ref、Naive UI locale、HTML lang、localStorage
 *
 * 需求 24.4: 支持动态切换语言而无需刷新页面
 */
export const setLocale = async (locale: Locale): Promise<void> => {
  if (currentLocale.value === locale) {
    return
  }

  const previousLocale = currentLocale.value
  const global = i18n.global as any

  // 按需加载 en-US 语言包
  if (locale === 'en-US' && !global.getLocaleMessage('en-US')?.common) {
    const msgs = await loadEnUSMessages()
    global.setLocaleMessage('en-US', msgs)
  }

  // 同步 vue-i18n 实例
  global.locale.value = locale

  // 同步响应式 ref
  currentLocale.value = locale

  // 持久化到 localStorage
  localStorage.setItem('locale', locale)

  // 更新 HTML lang 属性
  document.documentElement.lang = locale === 'zh-CN' ? 'zh' : 'en'

  // 派发自定义事件
  window.dispatchEvent(new CustomEvent(LOCALE_CHANGE_EVENT, {
    detail: { locale, previousLocale }
  }))
}

/**
 * 初始化语言
 * 从 localStorage 读取用户偏好，同步到 vue-i18n 和 Naive UI
 */
export const initLocale = async (): Promise<void> => {
  const savedLocale = localStorage.getItem('locale') as Locale
  if (savedLocale && (savedLocale === 'zh-CN' || savedLocale === 'en-US')) {
    // 如果保存的是 en-US，需要先加载语言包
    if (savedLocale === 'en-US') {
      const global = i18n.global as any
      const msgs = await loadEnUSMessages()
      global.setLocaleMessage('en-US', msgs)
      global.locale.value = savedLocale
    } else {
      const global = i18n.global as any
      global.locale.value = savedLocale
    }
    currentLocale.value = savedLocale
  }
  document.documentElement.lang = currentLocale.value === 'zh-CN' ? 'zh' : 'en'
}

export const getLocale = (): Locale => currentLocale.value
export const isZhCN = (): boolean => currentLocale.value === 'zh-CN'
export const isEnUS = (): boolean => currentLocale.value === 'en-US'

/**
 * 监听语言变化
 * @returns 清理函数，用于移除监听器
 */
export const onLocaleChange = (callback: (locale: Locale, previousLocale: Locale) => void): (() => void) => {
  const handler = (event: Event) => {
    const customEvent = event as CustomEvent<{ locale: Locale; previousLocale: Locale }>
    callback(customEvent.detail.locale, customEvent.detail.previousLocale)
  }
  window.addEventListener(LOCALE_CHANGE_EVENT, handler)
  return () => {
    window.removeEventListener(LOCALE_CHANGE_EVENT, handler)
  }
}

// ====================== 格式化工具 ======================
export type DateFormatType = 'short' | 'long' | 'datetime' | 'time'

const monthNames = {
  'en-US': [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ],
  'zh-CN': [
    '一月', '二月', '三月', '四月', '五月', '六月',
    '七月', '八月', '九月', '十月', '十一月', '十二月'
  ]
}

/**
 * 格式化日期
 * 需求 24.2: 根据语言设置自动调整日期显示格式
 */
export const formatDate = (
  date: Date | number | string | null | undefined,
  format: DateFormatType = 'short'
): string => {
  if (date === null || date === undefined) return ''
  const d = new Date(date)
  if (isNaN(d.getTime())) return ''

  const locale = currentLocale.value
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')

  switch (format) {
    case 'short':
      return locale === 'zh-CN'
        ? `${year}-${month}-${day}`
        : `${month}/${day}/${year}`
    case 'long':
      if (locale === 'zh-CN') {
        return `${year}年${parseInt(month)}月${parseInt(day)}日`
      } else {
        const monthName = monthNames['en-US'][d.getMonth()]
        return `${monthName} ${parseInt(day)}, ${year}`
      }
    case 'datetime':
      return locale === 'zh-CN'
        ? `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
        : `${month}/${day}/${year} ${hours}:${minutes}:${seconds}`
    case 'time':
      return `${hours}:${minutes}:${seconds}`
    default:
      return `${year}-${month}-${day}`
  }
}

export interface NumberFormatOptions {
  decimals?: number
  useThousands?: boolean
  currency?: boolean
  percent?: boolean
  currencySymbol?: string
  currencyPosition?: 'prefix' | 'suffix'
}

/**
 * 格式化数字
 * 需求 24.3: 根据语言设置自动调整数字格式
 */
export const formatNumber = (
  num: number | null | undefined,
  options?: NumberFormatOptions
): string => {
  if (num === null || num === undefined || isNaN(num)) return ''

  const locale = currentLocale.value
  const config = numberFormats[locale]

  const {
    decimals = 0,
    useThousands = true,
    currency = false,
    percent = false,
    currencySymbol,
    currencyPosition = 'prefix'
  } = options || {}

  let result: string
  let value = num

  if (percent) {
    value = num * 100
    result = value.toFixed(decimals) + '%'
  } else {
    result = value.toFixed(decimals)
  }

  if (useThousands && !percent) {
    const parts = result.split('.')
    parts[0] = (parts[0] || '').replace(/\B(?=(\d{3})+(?!\d))/g, config.thousands)
    result = parts.join(config.decimal)
  } else if (!percent) {
    result = result.replace('.', config.decimal)
  }

  if (currency) {
    const symbol = currencySymbol || config.currency
    if (currencyPosition === 'suffix') {
      result = result + symbol
    } else {
      result = symbol + result
    }
  }

  return result
}

export const formatCurrency = (
  num: number | null | undefined,
  decimals: number = 2
): string => {
  return formatNumber(num, { decimals, useThousands: true, currency: true })
}

export const formatPercent = (
  num: number | null | undefined,
  decimals: number = 1
): string => {
  return formatNumber(num, { decimals, percent: true })
}

export const formatDecimal = (
  num: number | null | undefined,
  decimals: number = 2
): string => {
  return formatNumber(num, { decimals, useThousands: true })
}

// ====================== useI18n composable ======================
/**
 * 组合式 API hook
 * 提供完整的 i18n 功能，兼容已有组件的调用方式
 */
export const useI18n = () => {
  return {
    locale: currentLocale,
    t,
    setLocale,
    getLocale,
    isZhCN,
    isEnUS,
    onLocaleChange,
    formatDate,
    formatNumber,
    formatCurrency,
    formatPercent,
    formatDecimal,
    dateFormats,
    numberFormats,
    naiveLocale,
    naiveDateLocale,
    LOCALE_CHANGE_EVENT
  }
}

// ====================== 语言包管理 ======================
export interface LanguagePackExport {
  metadata: {
    locale: Locale
    exportedAt: string
    version: string
    keyCount: number
    application: string
  }
  translations: Record<string, any>
}

export interface LanguagePackImportResult {
  success: boolean
  error?: string
  importedKeys?: number
  mergedKeys?: number
  newKeys?: number
  warnings?: string[]
}

const getAllKeyPaths = (obj: Record<string, any>, prefix = ''): string[] => {
  const keys: string[] = []
  for (const key of Object.keys(obj)) {
    const fullKey = prefix ? `${prefix}.${key}` : key
    if (typeof obj[key] === 'object' && obj[key] !== null) {
      keys.push(...getAllKeyPaths(obj[key], fullKey))
    } else {
      keys.push(fullKey)
    }
  }
  return keys
}

/**
 * 导出语言包
 */
export const exportLanguagePack = (locale: Locale): LanguagePackExport => {
  const global = i18n.global as any
  const translations = global.getLocaleMessage(locale) || {}
  return {
    metadata: {
      locale,
      exportedAt: new Date().toISOString(),
      version: '2.0.0',
      keyCount: getAllKeyPaths(translations).length,
      application: 'DataTeaCup'
    },
    translations: JSON.parse(JSON.stringify(translations))
  }
}

/**
 * 导入语言包
 */
export const importLanguagePack = (
  locale: Locale,
  jsonContent: string,
  options: { merge?: boolean; validate?: boolean } = {}
): LanguagePackImportResult => {
  try {
    const data = JSON.parse(jsonContent)
    const translations = data.translations || data
    const global = i18n.global as any

    if (options.merge !== false) {
      const existing = global.getLocaleMessage(locale) || {}
      global.setLocaleMessage(locale, { ...existing, ...translations })
    } else {
      global.setLocaleMessage(locale, translations)
    }

    const importedKeys = getAllKeyPaths(translations)
    return {
      success: true,
      importedKeys: importedKeys.length,
      newKeys: importedKeys.length,
      mergedKeys: 0
    }
  } catch (e: any) {
    return {
      success: false,
      error: e.message || 'Parse error'
    }
  }
}

/**
 * 从文件导入语言包
 */
export const importLanguagePackFromFile = async (
  locale: Locale,
  file: File,
  options: { merge?: boolean; validate?: boolean } = {}
): Promise<LanguagePackImportResult> => {
  return new Promise((resolve) => {
    const reader = new FileReader()
    reader.onload = (event) => {
      const content = event.target?.result as string
      if (content) {
        resolve(importLanguagePack(locale, content, options))
      } else {
        resolve({ success: false, error: 'Failed to read file content' })
      }
    }
    reader.onerror = () => {
      resolve({ success: false, error: 'Failed to read file: ' + reader.error?.message })
    }
    reader.readAsText(file, 'utf-8')
  })
}

/**
 * 比较两个语言包的差异
 */
export const compareLanguagePacks = (locale1: Locale, locale2: Locale): {
  missingInFirst: string[]
  missingInSecond: string[]
  common: string[]
} => {
  const global = i18n.global as any
  const keys1 = getAllKeyPaths(global.getLocaleMessage(locale1) || {})
  const keys2 = getAllKeyPaths(global.getLocaleMessage(locale2) || {})

  const set1 = new Set(keys1)
  const set2 = new Set(keys2)

  return {
    missingInFirst: keys2.filter(key => !set1.has(key)),
    missingInSecond: keys1.filter(key => !set2.has(key)),
    common: keys1.filter(key => set2.has(key))
  }
}

// 保留旧的 messages 引用以支持可能的直接访问
export const messages = {
  'zh-CN': zhCNMessages,
}

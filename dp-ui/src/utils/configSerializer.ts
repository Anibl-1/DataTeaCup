/* eslint-disable @typescript-eslint/no-explicit-any */
import type { SystemConfig } from '@/api/system/systemConfig'

/**
 * 将 SystemConfig 对象序列化为 JSON 字符串
 */
export function serializeConfig(config: SystemConfig): string {
  return JSON.stringify(config)
}

/**
 * 将 JSON 字符串反序列化为 SystemConfig 对象
 */
export function deserializeConfig(json: string): SystemConfig {
  return JSON.parse(json) as SystemConfig
}

/**
 * 根据 configType 返回对应的 n-tag 颜色
 * string→默认色, number→蓝色, boolean→绿色, json→紫色, password→橙色
 */
export function getConfigTypeColor(configType: string): string {
  switch (configType) {
    case 'number':
      return '#2080f0'
    case 'boolean':
      return '#18a058'
    case 'json':
      return '#8b5cf6'
    case 'password':
      return '#f0a020'
    case 'string':
    default:
      return 'default'
  }
}

/**
 * 根据 configType 返回对应的 UI 编辑控件类型
 */
export function getConfigControlType(configType: string): string {
  switch (configType) {
    case 'number':
      return 'input-number'
    case 'boolean':
      return 'switch'
    case 'json':
      return 'textarea'
    case 'password':
      return 'password'
    case 'string':
    default:
      return 'input'
  }
}

/**
 * 校验字符串是否为有效 JSON 格式
 */
export function validateJsonFormat(value: string): { valid: boolean; error?: string } {
  try {
    JSON.parse(value)
    return { valid: true }
  } catch (e: any) {
    return { valid: false, error: e.message ?? 'Invalid JSON' }
  }
}

/**
 * 根据关键词过滤配置列表，匹配 configKey 或 configDesc
 */
export function filterConfigs(configs: SystemConfig[], keyword: string): SystemConfig[] {
  if (!keyword || !keyword.trim()) {
    return configs
  }
  const lowerKeyword = keyword.toLowerCase()
  return configs.filter(
    (c) =>
      c.configKey.toLowerCase().includes(lowerKeyword) ||
      (c.configDesc && c.configDesc.toLowerCase().includes(lowerKeyword))
  )
}

/**
 * 仪表盘分享与嵌入相关类型定义
 */

/**
 * 分享链接
 */
export interface ShareLink {
  /** 分享ID */
  id?: number
  /** 仪表盘ID */
  dashboardId: number
  /** 分享 token */
  shareToken: string
  /** 过期时间 */
  expireTime?: string
  /** 创建人ID */
  createBy?: number
  /** 创建时间 */
  createTime?: string
  /** 状态：1-有效，0-已撤销 */
  status?: number
}

/**
 * 嵌入配置
 */
export interface EmbedConfig {
  /** 仪表盘ID */
  dashboardId: number
  /** 宽度 */
  width?: string
  /** 高度 */
  height?: string
  /** 是否显示头部 */
  showHeader?: boolean
  /** 是否显示筛选器 */
  showFilters?: boolean
}

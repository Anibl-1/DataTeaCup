/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 菜单相关类型定义
 */

/**
 * 菜单类型
 * - directory: 目录（用于分组，不对应具体页面）
 * - menu: 菜单（对应具体页面）
 * - button: 按钮（权限控制用）
 */
export type MenuType = 'directory' | 'menu' | 'button'

/**
 * 菜单信息
 */
export interface Menu {
  /** 菜单ID */
  id: number
  /** 菜单名称 */
  menuName: string
  /** 菜单编码（唯一标识，用于路由） */
  menuCode: string
  /** 父菜单ID，0表示顶级菜单 */
  parentId: number
  /** 菜单类型：directory-目录，menu-菜单，button-按钮 */
  menuType: MenuType
  /** 路由路径 */
  routePath?: string
  /** 组件路径 */
  componentPath?: string
  /** 图标 */
  icon?: string
  /** 排序顺序 */
  sortOrder: number
  /** 是否可见：1-可见，0-隐藏 */
  isVisible: number
  /** 移动端可见：1-可见，0-隐藏 */
  mobileVisible?: number
  /** 权限编码 */
  permissionCode?: string
  /** 关联的报表ID（如果是报表菜单） */
  reportId?: number
  /** 关联的图表ID */
  chartId?: number
  /** 关联的页面ID */
  pageId?: number
  /** 关联的数据视图编码 */
  dataViewCode?: string
  /** 打开方式: tab-标签页, window-新窗口, drawer-抽屉 */
  openMode?: string
  /** 角标文案，如 New、Beta */
  badge?: string
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
  /** 子菜单列表 */
  children?: Menu[]
  /** 关联的报表信息 */
  report?: any
}

/**
 * 菜单表单
 */
export interface MenuForm {
  /** 菜单ID（更新时必填） */
  id?: number | null
  /** 菜单名称 */
  menuName: string
  /** 菜单编码（唯一标识，用于路由） */
  menuCode: string
  /** 父菜单ID，0表示顶级菜单 */
  parentId: number | null
  /** 菜单类型：directory-目录，menu-菜单，button-按钮 */
  menuType?: MenuType
  /** 路由路径 */
  routePath: string | null
  /** 组件路径 */
  componentPath: string | null
  /** 图标 */
  icon: string
  /** 排序顺序 */
  sortOrder: number | null
  /** 是否可见：1-可见，0-隐藏 */
  isVisible: number
  /** 移动端可见：1-可见，0-隐藏 */
  mobileVisible?: number
  /** 权限编码 */
  permissionCode: string | null
  /** 关联的报表ID（如果是报表菜单） */
  reportId?: number | null
  /** 关联的图表ID */
  chartId?: number | null
  /** 关联的页面ID */
  pageId?: number | null
  /** 关联的数据视图编码 */
  dataViewCode?: string
  /** 打开方式: tab-标签页, window-新窗口, drawer-抽屉 */
  openMode?: string
  /** 角标文案 */
  badge?: string
}


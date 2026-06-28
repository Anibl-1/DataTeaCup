/**
 * 角色信息
 */
export interface Role {
  /** 角色ID */
  id: number
  /** 角色名称 */
  roleName: string
  /** 角色编码 */
  roleCode: string
  /** 描述 */
  description?: string
  /** 状态（1=启用, 0=禁用） */
  status?: number
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/**
 * 权限信息
 */
export interface Permission {
  /** 权限ID */
  id: number
  /** 权限名称 */
  permissionName: string
  /** 权限编码 */
  permissionCode: string
  /** 描述 */
  description?: string
  /** 创建时间 */
  createTime?: string
}

/**
 * 角色表单数据
 */
export interface RoleForm {
  /** 角色ID（更新时必填） */
  id?: number
  /** 角色名称 */
  roleName: string
  /** 角色编码 */
  roleCode: string
  /** 描述 */
  description?: string
}



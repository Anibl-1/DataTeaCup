/**
 * 数据校验工具 - 基于 Zod
 */
import { z } from 'zod'

// ==================== 通用校验规则 ====================

/** 非空字符串 */
export const requiredString = (fieldName: string) =>
  z.string({ required_error: `${fieldName}不能为空` })
    .min(1, `${fieldName}不能为空`)
    .trim()

/** 可选字符串 */
export const optionalString = z.string().optional().nullable()

/** 邮箱 */
export const email = z.string().email('邮箱格式不正确')

/** 手机号 */
export const phone = z.string().regex(/^1[3-9]\d{9}$/, '手机号格式不正确')

/** 密码（至少6位） */
export const password = z.string().min(6, '密码至少6位')

/** 正整数 */
export const positiveInt = z.number().int().positive('必须为正整数')

/** ID */
export const id = z.number().int().positive()

// ==================== 业务校验 Schema ====================

/** 登录表单 */
export const loginSchema = z.object({
  username: requiredString('用户名'),
  password: requiredString('密码')
})

/** 用户创建 */
export const userCreateSchema = z.object({
  username: requiredString('用户名').min(3, '用户名至少3个字符').max(20, '用户名最多20个字符'),
  password: password,
  nickname: requiredString('昵称'),
  email: email.optional().or(z.literal('')),
  phone: phone.optional().or(z.literal('')),
  status: z.number().default(1),
  roleIds: z.array(z.number()).optional()
})

/** 用户更新 */
export const userUpdateSchema = z.object({
  id: id,
  nickname: requiredString('昵称'),
  email: email.optional().or(z.literal('')),
  phone: phone.optional().or(z.literal('')),
  status: z.number().optional(),
  roleIds: z.array(z.number()).optional()
})

/** 修改密码 */
export const changePasswordSchema = z.object({
  oldPassword: requiredString('原密码'),
  newPassword: password,
  confirmPassword: z.string()
}).refine(data => data.newPassword === data.confirmPassword, {
  message: '两次输入的密码不一致',
  path: ['confirmPassword']
})

/** 数据源创建 */
export const dataSourceSchema = z.object({
  name: requiredString('数据源名称'),
  type: requiredString('数据库类型'),
  host: requiredString('主机地址'),
  port: z.number({ required_error: '端口不能为空', invalid_type_error: '端口必须为数字' }).int('端口必须为整数').min(1, '端口最小为1').max(65535, '端口范围1-65535'),
  database: requiredString('数据库名'),
  username: requiredString('用户名'),
  password: requiredString('密码'),
  description: optionalString
})

/** 角色创建 */
export const roleSchema = z.object({
  roleName: requiredString('角色名称'),
  roleCode: requiredString('角色编码').regex(/^[A-Z_]+$/, '角色编码只能包含大写字母和下划线'),
  description: optionalString,
  status: z.number({ invalid_type_error: '状态必须为数字' }).default(1)
})

/** 菜单创建 */
export const menuSchema = z.object({
  menuName: requiredString('菜单名称'),
  menuCode: requiredString('菜单编码'),
  parentId: z.number().optional().nullable(),
  routePath: optionalString,
  icon: optionalString,
  sortOrder: z.number().int().default(0),
  status: z.number().default(1)
})

/** 报表定义 */
export const reportSchema = z.object({
  name: requiredString('报表名称'),
  code: requiredString('报表编码').regex(/^[a-zA-Z][a-zA-Z0-9_]*$/, '编码以字母开头，只能包含字母数字下划线'),
  dataSourceId: z.number({ required_error: '数据源不能为空', invalid_type_error: '数据源ID必须为数字' }).int().positive('数据源ID必须为正整数'),
  sqlContent: requiredString('SQL语句'),
  description: optionalString
})

/** 图表定义 */
export const chartSchema = z.object({
  name: requiredString('图表名称'),
  chartType: requiredString('图表类型'),
  dataSourceId: z.number({ required_error: '数据源不能为空', invalid_type_error: '数据源ID必须为数字' }).int().positive('数据源ID必须为正整数'),
  sqlContent: requiredString('SQL语句'),
  chartConfig: z.string().optional()
})

/** 工单创建 */
export const ticketSchema = z.object({
  title: requiredString('工单标题'),
  category: requiredString('工单分类'),
  priority: requiredString('优先级'),
  description: requiredString('工单描述'),
  assigneeId: z.number({ required_error: '处理人不能为空', invalid_type_error: '处理人ID必须为数字' }).int().positive('处理人ID必须为正整数').optional().nullable(),
  status: z.string().default('pending'),
  attachments: z.array(z.string()).optional()
})

/** 知识库文章 */
export const knowledgeArticleSchema = z.object({
  title: requiredString('文章标题'),
  category: requiredString('文章分类'),
  content: requiredString('文章内容'),
  tags: z.array(z.string()).optional(),
  attachments: z.array(z.string()).optional(),
  status: z.string().default('draft')
})

/** 行级安全规则 */
export const rlsRuleSchema = z.object({
  ruleName: requiredString('规则名称'),
  tableName: requiredString('表名'),
  condition: requiredString('过滤条件'),
  description: optionalString,
  status: z.number().default(1)
})

// ==================== 校验工具函数 ====================

/**
 * 校验数据
 * @returns { success: boolean, data?: T, errors?: Record<string, string> }
 */
export function validate<T>(schema: z.ZodSchema<T>, data: unknown): {
  success: boolean
  data?: T
  errors?: Record<string, string>
} {
  const result = schema.safeParse(data)
  
  if (result.success) {
    return { success: true, data: result.data }
  }
  
  const errors: Record<string, string> = {}
  result.error.errors.forEach(err => {
    const path = err.path.join('.')
    if (!(path in errors)) {
      errors[path] = err.message
    }
  })
  
  return { success: false, errors }
}

/**
 * 获取第一个错误信息
 */
export function getFirstError(errors: Record<string, string> | undefined): string {
  if (!errors) return ''
  for (const key in errors) {
    if (Object.prototype.hasOwnProperty.call(errors, key)) {
      return errors[key] ?? ''
    }
  }
  return ''
}

// 导出类型
export type LoginForm = z.infer<typeof loginSchema>
export type UserCreateForm = z.infer<typeof userCreateSchema>
export type UserUpdateForm = z.infer<typeof userUpdateSchema>
export type ChangePasswordForm = z.infer<typeof changePasswordSchema>
export type DataSourceForm = z.infer<typeof dataSourceSchema>
export type RoleForm = z.infer<typeof roleSchema>
export type MenuForm = z.infer<typeof menuSchema>
export type ReportForm = z.infer<typeof reportSchema>
export type ChartForm = z.infer<typeof chartSchema>
export type TicketForm = z.infer<typeof ticketSchema>
export type KnowledgeArticleForm = z.infer<typeof knowledgeArticleSchema>
export type RlsRuleForm = z.infer<typeof rlsRuleSchema>

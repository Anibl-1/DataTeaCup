/**
 * 数据校验工具测试
 */
import { describe, it, expect } from 'vitest'
import {
  validate,
  getFirstError,
  loginSchema,
  dataSourceSchema,
  roleSchema,
  menuSchema,
  reportSchema,
  chartSchema,
  ticketSchema,
  knowledgeArticleSchema,
  rlsRuleSchema,
  changePasswordSchema
} from '../validation'

// ==================== validate() 工具函数 ====================

describe('validate()', () => {
  it('returns success with parsed data for valid input', () => {
    const result = validate(loginSchema, { username: 'admin', password: 'secret' })
    expect(result.success).toBe(true)
    expect(result.data).toEqual({ username: 'admin', password: 'secret' })
    expect(result.errors).toBeUndefined()
  })

  it('returns field-level errors for invalid input', () => {
    const result = validate(loginSchema, { username: '', password: '' })
    expect(result.success).toBe(false)
    expect(result.errors).toBeDefined()
    expect(result.errors!['username']).toBe('用户名不能为空')
    expect(result.errors!['password']).toBe('密码不能为空')
  })

  it('returns errors when required fields are undefined', () => {
    const result = validate(loginSchema, {})
    expect(result.success).toBe(false)
    expect(result.errors).toBeDefined()
    expect(result.errors!['username']).toBeDefined()
    expect(result.errors!['password']).toBeDefined()
  })
})

// ==================== getFirstError() ====================

describe('getFirstError()', () => {
  it('returns empty string for undefined errors', () => {
    expect(getFirstError(undefined)).toBe('')
  })

  it('returns empty string for empty errors object', () => {
    expect(getFirstError({})).toBe('')
  })

  it('returns the first error message', () => {
    const errors = { name: '名称不能为空', code: '编码不能为空' }
    expect(getFirstError(errors)).toBe('名称不能为空')
  })
})


// ==================== 必填字段空值校验 ====================

describe('required field validation - empty values', () => {
  it('loginSchema rejects empty strings', () => {
    const result = validate(loginSchema, { username: '', password: '' })
    expect(result.success).toBe(false)
    expect(result.errors!['username']).toBe('用户名不能为空')
    expect(result.errors!['password']).toBe('密码不能为空')
  })

  it('dataSourceSchema rejects empty required fields', () => {
    const result = validate(dataSourceSchema, {
      name: '', type: '', host: '', port: null, database: '', username: '', password: ''
    })
    expect(result.success).toBe(false)
    expect(result.errors!['name']).toBe('数据源名称不能为空')
    expect(result.errors!['type']).toBe('数据库类型不能为空')
    expect(result.errors!['host']).toBe('主机地址不能为空')
    expect(result.errors!['port']).toBe('端口必须为数字')
  })

  it('dataSourceSchema provides port error for missing port', () => {
    const result = validate(dataSourceSchema, {
      name: 'test', type: 'mysql', host: 'localhost', database: 'db', username: 'root', password: 'pass'
    })
    expect(result.success).toBe(false)
    expect(result.errors!['port']).toBe('端口不能为空')
  })

  it('roleSchema rejects empty required fields', () => {
    const result = validate(roleSchema, { roleName: '', roleCode: '' })
    expect(result.success).toBe(false)
    expect(result.errors!['roleName']).toBe('角色名称不能为空')
    expect(result.errors!['roleCode']).toBe('角色编码不能为空')
  })

  it('reportSchema rejects empty required fields and missing dataSourceId', () => {
    const result = validate(reportSchema, { name: '', code: '', sqlContent: '' })
    expect(result.success).toBe(false)
    expect(result.errors!['name']).toBe('报表名称不能为空')
    expect(result.errors!['dataSourceId']).toBe('数据源不能为空')
  })

  it('chartSchema rejects empty required fields and missing dataSourceId', () => {
    const result = validate(chartSchema, { name: '', chartType: '', sqlContent: '' })
    expect(result.success).toBe(false)
    expect(result.errors!['name']).toBe('图表名称不能为空')
    expect(result.errors!['dataSourceId']).toBe('数据源不能为空')
  })
})

// ==================== 新增业务 Schema 测试 ====================

describe('ticketSchema', () => {
  it('validates a valid ticket', () => {
    const result = validate(ticketSchema, {
      title: '数据库连接异常',
      category: 'bug',
      priority: 'high',
      description: '生产环境数据库连接超时'
    })
    expect(result.success).toBe(true)
  })

  it('rejects empty required fields', () => {
    const result = validate(ticketSchema, {
      title: '', category: '', priority: '', description: ''
    })
    expect(result.success).toBe(false)
    expect(result.errors!['title']).toBe('工单标题不能为空')
    expect(result.errors!['category']).toBe('工单分类不能为空')
    expect(result.errors!['priority']).toBe('优先级不能为空')
    expect(result.errors!['description']).toBe('工单描述不能为空')
  })
})

describe('knowledgeArticleSchema', () => {
  it('validates a valid article', () => {
    const result = validate(knowledgeArticleSchema, {
      title: '如何配置数据源',
      category: '使用指南',
      content: '本文介绍数据源配置步骤...'
    })
    expect(result.success).toBe(true)
  })

  it('rejects empty required fields', () => {
    const result = validate(knowledgeArticleSchema, {
      title: '', category: '', content: ''
    })
    expect(result.success).toBe(false)
    expect(result.errors!['title']).toBe('文章标题不能为空')
    expect(result.errors!['category']).toBe('文章分类不能为空')
    expect(result.errors!['content']).toBe('文章内容不能为空')
  })
})

describe('rlsRuleSchema', () => {
  it('validates a valid RLS rule', () => {
    const result = validate(rlsRuleSchema, {
      ruleName: '部门数据隔离',
      tableName: 'orders',
      condition: 'dept_id = :current_dept_id'
    })
    expect(result.success).toBe(true)
  })

  it('rejects empty required fields', () => {
    const result = validate(rlsRuleSchema, {
      ruleName: '', tableName: '', condition: ''
    })
    expect(result.success).toBe(false)
    expect(result.errors!['ruleName']).toBe('规则名称不能为空')
    expect(result.errors!['tableName']).toBe('表名不能为空')
    expect(result.errors!['condition']).toBe('过滤条件不能为空')
  })
})

// ==================== 已有 Schema 有效输入测试 ====================

describe('existing schemas - valid input', () => {
  it('dataSourceSchema accepts valid data', () => {
    const result = validate(dataSourceSchema, {
      name: 'prod-mysql', type: 'mysql', host: '192.168.1.1',
      port: 3306, database: 'analytics', username: 'root', password: 'pass123'
    })
    expect(result.success).toBe(true)
  })

  it('roleSchema accepts valid data', () => {
    const result = validate(roleSchema, {
      roleName: '管理员', roleCode: 'ADMIN'
    })
    expect(result.success).toBe(true)
  })

  it('menuSchema accepts valid data', () => {
    const result = validate(menuSchema, {
      menuName: '用户管理', menuCode: 'system:user'
    })
    expect(result.success).toBe(true)
  })

  it('changePasswordSchema validates matching passwords', () => {
    const result = validate(changePasswordSchema, {
      oldPassword: 'old123', newPassword: 'new123', confirmPassword: 'new123'
    })
    expect(result.success).toBe(true)
  })

  it('changePasswordSchema rejects mismatched passwords', () => {
    const result = validate(changePasswordSchema, {
      oldPassword: 'old123', newPassword: 'new123', confirmPassword: 'different'
    })
    expect(result.success).toBe(false)
    expect(result.errors!['confirmPassword']).toBe('两次输入的密码不一致')
  })
})


// ==================== Property-Based Tests ====================

import * as fc from 'fast-check'

/**
 * Property 16: 必填字段校验
 * Feature: page-audit-optimization, Property 16
 *
 * **Validates: Requirements 18.1**
 *
 * For any Zod schema 中标记为 required 的字段，当该字段值为空字符串或 undefined 时，
 * validate() 函数应返回 success: false 且 errors 中包含该字段的错误信息。
 */
describe('Property 16: Required field validation rejects empty/undefined values', () => {
  // Schema definitions with their required string fields
  const schemasWithRequiredFields: Array<{
    name: string
    schema: Parameters<typeof validate>[0]
    requiredFields: string[]
  }> = [
    { name: 'loginSchema', schema: loginSchema, requiredFields: ['username', 'password'] },
    { name: 'roleSchema', schema: roleSchema, requiredFields: ['roleName', 'roleCode'] },
    { name: 'ticketSchema', schema: ticketSchema, requiredFields: ['title', 'category', 'priority', 'description'] },
    { name: 'knowledgeArticleSchema', schema: knowledgeArticleSchema, requiredFields: ['title', 'category', 'content'] },
    { name: 'rlsRuleSchema', schema: rlsRuleSchema, requiredFields: ['ruleName', 'tableName', 'condition'] },
  ]

  // Arbitrary: choose between empty string and undefined for the "empty" value
  const emptyValueArb = fc.constantFrom('', undefined)

  for (const { name, schema, requiredFields } of schemasWithRequiredFields) {
    it(`${name}: any required field set to empty string or undefined should fail validation`, () => {
      fc.assert(
        fc.property(
          fc.constantFrom(...requiredFields),
          emptyValueArb,
          (field, emptyValue) => {
            // Build data with the target field set to empty/undefined
            const data: Record<string, unknown> = {}
            for (const f of requiredFields) {
              data[f] = f === field ? emptyValue : `valid-${f}`
            }

            const result = validate(schema, data)

            // Property: validation should fail
            expect(result.success).toBe(false)

            // Property: errors should contain the specific field
            expect(result.errors).toBeDefined()
            expect(result.errors![field]).toBeDefined()
            expect(typeof result.errors![field]).toBe('string')
            expect(result.errors![field].length).toBeGreaterThan(0)
          }
        ),
        { numRuns: 100 }
      )
    })
  }
})

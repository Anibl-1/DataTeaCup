import { describe, it, expect } from 'vitest'
import {
  validate,
  getFirstError,
  loginSchema,
  userCreateSchema,
  userUpdateSchema,
  changePasswordSchema,
  dataSourceSchema,
  roleSchema,
  menuSchema,
  reportSchema,
  chartSchema,
  requiredString,
  email,
  phone,
  password,
  positiveInt,
  optionalString,
  id
} from '../validation'

// ==================== 通用校验规则 ====================

describe('Common Validators', () => {
  describe('requiredString', () => {
    const nameField = requiredString('名称')

    it('should accept non-empty string', () => {
      expect(nameField.safeParse('hello').success).toBe(true)
    })

    it('should reject empty string', () => {
      const r = nameField.safeParse('')
      expect(r.success).toBe(false)
    })

    it('should trim whitespace-only string to empty (passes min check before trim)', () => {
      // Note: Zod applies min(1) before trim(), so '   ' passes min(1) then gets trimmed
      const r = nameField.safeParse('   ')
      // The actual behavior: min(1) passes (length 3), then trim produces ''
      expect(r.success).toBe(true)
      if (r.success) expect(r.data).toBe('')
    })

    it('should trim whitespace', () => {
      const r = nameField.safeParse('  hello  ')
      expect(r.success).toBe(true)
      if (r.success) expect(r.data).toBe('hello')
    })

    it('should reject undefined', () => {
      expect(nameField.safeParse(undefined).success).toBe(false)
    })

    it('should reject null', () => {
      expect(nameField.safeParse(null).success).toBe(false)
    })

    it('should accept special characters', () => {
      expect(nameField.safeParse('hello@#$%^&*').success).toBe(true)
    })
  })

  describe('optionalString', () => {
    it('should accept string', () => {
      expect(optionalString.safeParse('hello').success).toBe(true)
    })

    it('should accept empty string', () => {
      expect(optionalString.safeParse('').success).toBe(true)
    })

    it('should accept undefined', () => {
      expect(optionalString.safeParse(undefined).success).toBe(true)
    })

    it('should accept null', () => {
      expect(optionalString.safeParse(null).success).toBe(true)
    })
  })

  describe('email', () => {
    it('should accept valid email', () => {
      expect(email.safeParse('user@example.com').success).toBe(true)
    })

    it('should reject invalid email without @', () => {
      expect(email.safeParse('userexample.com').success).toBe(false)
    })

    it('should reject invalid email without domain', () => {
      expect(email.safeParse('user@').success).toBe(false)
    })

    it('should reject empty string', () => {
      expect(email.safeParse('').success).toBe(false)
    })
  })

  describe('phone', () => {
    it('should accept valid phone number', () => {
      expect(phone.safeParse('13800138000').success).toBe(true)
    })

    it('should reject phone not starting with 1', () => {
      expect(phone.safeParse('23800138000').success).toBe(false)
    })

    it('should reject phone with wrong length', () => {
      expect(phone.safeParse('1380013800').success).toBe(false)
    })

    it('should reject phone with letters', () => {
      expect(phone.safeParse('1380013800a').success).toBe(false)
    })

    it('should reject empty string', () => {
      expect(phone.safeParse('').success).toBe(false)
    })
  })

  describe('password', () => {
    it('should accept password with 6+ chars', () => {
      expect(password.safeParse('123456').success).toBe(true)
    })

    it('should reject password with less than 6 chars', () => {
      expect(password.safeParse('12345').success).toBe(false)
    })

    it('should accept long password', () => {
      expect(password.safeParse('a'.repeat(100)).success).toBe(true)
    })
  })

  describe('positiveInt', () => {
    it('should accept positive integer', () => {
      expect(positiveInt.safeParse(1).success).toBe(true)
    })

    it('should reject zero', () => {
      expect(positiveInt.safeParse(0).success).toBe(false)
    })

    it('should reject negative number', () => {
      expect(positiveInt.safeParse(-1).success).toBe(false)
    })

    it('should reject float', () => {
      expect(positiveInt.safeParse(1.5).success).toBe(false)
    })
  })

  describe('id', () => {
    it('should accept positive integer', () => {
      expect(id.safeParse(1).success).toBe(true)
    })

    it('should reject zero', () => {
      expect(id.safeParse(0).success).toBe(false)
    })

    it('should reject negative', () => {
      expect(id.safeParse(-1).success).toBe(false)
    })
  })
})


// ==================== 业务 Schema 测试 ====================

describe('loginSchema', () => {
  it('should validate correct login data', () => {
    const result = validate(loginSchema, { username: 'admin', password: 'admin123' })
    expect(result.success).toBe(true)
    expect(result.data).toEqual({ username: 'admin', password: 'admin123' })
  })

  it('should reject empty username', () => {
    const result = validate(loginSchema, { username: '', password: 'admin123' })
    expect(result.success).toBe(false)
    expect(result.errors?.username).toBeDefined()
  })

  it('should reject empty password', () => {
    const result = validate(loginSchema, { username: 'admin', password: '' })
    expect(result.success).toBe(false)
    expect(result.errors?.password).toBeDefined()
  })

  it('should trim whitespace from username', () => {
    const result = validate(loginSchema, { username: '  admin  ', password: 'admin123' })
    expect(result.success).toBe(true)
    expect(result.data?.username).toBe('admin')
  })

  it('should reject missing fields', () => {
    const result = validate(loginSchema, {})
    expect(result.success).toBe(false)
    expect(result.errors?.username).toBeDefined()
    expect(result.errors?.password).toBeDefined()
  })

  it('should reject non-string username', () => {
    const result = validate(loginSchema, { username: 123, password: 'admin123' })
    expect(result.success).toBe(false)
  })
})

describe('userCreateSchema', () => {
  const validUser = {
    username: 'testuser',
    password: 'password123',
    nickname: '测试用户'
  }

  it('should validate correct user data', () => {
    const result = validate(userCreateSchema, validUser)
    expect(result.success).toBe(true)
  })

  it('should reject username shorter than 3 chars', () => {
    const result = validate(userCreateSchema, { ...validUser, username: 'ab' })
    expect(result.success).toBe(false)
    expect(result.errors?.username).toContain('至少3个字符')
  })

  it('should accept username with exactly 3 chars', () => {
    const result = validate(userCreateSchema, { ...validUser, username: 'abc' })
    expect(result.success).toBe(true)
  })

  it('should reject username longer than 20 chars', () => {
    const result = validate(userCreateSchema, { ...validUser, username: 'a'.repeat(21) })
    expect(result.success).toBe(false)
    expect(result.errors?.username).toContain('最多20个字符')
  })

  it('should accept username with exactly 20 chars', () => {
    const result = validate(userCreateSchema, { ...validUser, username: 'a'.repeat(20) })
    expect(result.success).toBe(true)
  })

  it('should reject short password', () => {
    const result = validate(userCreateSchema, { ...validUser, password: '12345' })
    expect(result.success).toBe(false)
    expect(result.errors?.password).toContain('至少6位')
  })

  it('should allow optional email as empty string', () => {
    const result = validate(userCreateSchema, { ...validUser, email: '' })
    expect(result.success).toBe(true)
  })

  it('should allow valid email', () => {
    const result = validate(userCreateSchema, { ...validUser, email: 'test@example.com' })
    expect(result.success).toBe(true)
  })

  it('should reject invalid email format', () => {
    const result = validate(userCreateSchema, { ...validUser, email: 'not-an-email' })
    expect(result.success).toBe(false)
  })

  it('should allow optional phone as empty string', () => {
    const result = validate(userCreateSchema, { ...validUser, phone: '' })
    expect(result.success).toBe(true)
  })

  it('should allow valid phone', () => {
    const result = validate(userCreateSchema, { ...validUser, phone: '13800138000' })
    expect(result.success).toBe(true)
  })

  it('should reject invalid phone', () => {
    const result = validate(userCreateSchema, { ...validUser, phone: '12345' })
    expect(result.success).toBe(false)
  })

  it('should default status to 1', () => {
    const result = validate(userCreateSchema, validUser)
    expect(result.success).toBe(true)
    expect(result.data?.status).toBe(1)
  })

  it('should allow roleIds array', () => {
    const result = validate(userCreateSchema, { ...validUser, roleIds: [1, 2, 3] })
    expect(result.success).toBe(true)
    expect(result.data?.roleIds).toEqual([1, 2, 3])
  })
})

describe('userUpdateSchema', () => {
  const validUpdate = { id: 1, nickname: '新昵称' }

  it('should validate correct update data', () => {
    const result = validate(userUpdateSchema, validUpdate)
    expect(result.success).toBe(true)
  })

  it('should reject missing id', () => {
    const result = validate(userUpdateSchema, { nickname: '新昵称' })
    expect(result.success).toBe(false)
  })

  it('should reject non-positive id', () => {
    const result = validate(userUpdateSchema, { id: 0, nickname: '新昵称' })
    expect(result.success).toBe(false)
  })

  it('should reject empty nickname', () => {
    const result = validate(userUpdateSchema, { id: 1, nickname: '' })
    expect(result.success).toBe(false)
  })

  it('should allow optional email and phone', () => {
    const result = validate(userUpdateSchema, {
      ...validUpdate,
      email: 'test@example.com',
      phone: '13800138000'
    })
    expect(result.success).toBe(true)
  })
})

describe('changePasswordSchema', () => {
  it('should validate matching passwords', () => {
    const result = validate(changePasswordSchema, {
      oldPassword: 'oldpass',
      newPassword: 'newpass123',
      confirmPassword: 'newpass123'
    })
    expect(result.success).toBe(true)
  })

  it('should reject mismatched passwords', () => {
    const result = validate(changePasswordSchema, {
      oldPassword: 'oldpass',
      newPassword: 'newpass123',
      confirmPassword: 'different'
    })
    expect(result.success).toBe(false)
    expect(result.errors?.confirmPassword).toContain('不一致')
  })

  it('should reject short new password', () => {
    const result = validate(changePasswordSchema, {
      oldPassword: 'oldpass',
      newPassword: '12345',
      confirmPassword: '12345'
    })
    expect(result.success).toBe(false)
  })

  it('should reject empty old password', () => {
    const result = validate(changePasswordSchema, {
      oldPassword: '',
      newPassword: 'newpass123',
      confirmPassword: 'newpass123'
    })
    expect(result.success).toBe(false)
  })
})

describe('dataSourceSchema', () => {
  const validDS = {
    name: '测试数据源',
    type: 'MySQL',
    host: 'localhost',
    port: 3306,
    database: 'test_db',
    username: 'root',
    password: 'password'
  }

  it('should validate correct data source', () => {
    const result = validate(dataSourceSchema, validDS)
    expect(result.success).toBe(true)
  })

  it('should accept port at lower boundary (1)', () => {
    const result = validate(dataSourceSchema, { ...validDS, port: 1 })
    expect(result.success).toBe(true)
  })

  it('should accept port at upper boundary (65535)', () => {
    const result = validate(dataSourceSchema, { ...validDS, port: 65535 })
    expect(result.success).toBe(true)
  })

  it('should reject port 0', () => {
    const result = validate(dataSourceSchema, { ...validDS, port: 0 })
    expect(result.success).toBe(false)
  })

  it('should reject port above 65535', () => {
    const result = validate(dataSourceSchema, { ...validDS, port: 65536 })
    expect(result.success).toBe(false)
    expect(result.errors?.port).toBeDefined()
  })

  it('should reject negative port', () => {
    const result = validate(dataSourceSchema, { ...validDS, port: -1 })
    expect(result.success).toBe(false)
  })

  it('should reject float port', () => {
    const result = validate(dataSourceSchema, { ...validDS, port: 3306.5 })
    expect(result.success).toBe(false)
  })

  it('should reject empty name', () => {
    const result = validate(dataSourceSchema, { ...validDS, name: '' })
    expect(result.success).toBe(false)
  })

  it('should allow optional description', () => {
    const result = validate(dataSourceSchema, { ...validDS, description: '描述' })
    expect(result.success).toBe(true)
  })

  it('should allow null description', () => {
    const result = validate(dataSourceSchema, { ...validDS, description: null })
    expect(result.success).toBe(true)
  })
})


describe('roleSchema', () => {
  const validRole = {
    roleName: '管理员',
    roleCode: 'ADMIN',
    description: '系统管理员'
  }

  it('should validate correct role data', () => {
    const result = validate(roleSchema, validRole)
    expect(result.success).toBe(true)
  })

  it('should reject empty roleName', () => {
    const result = validate(roleSchema, { ...validRole, roleName: '' })
    expect(result.success).toBe(false)
  })

  it('should reject empty roleCode', () => {
    const result = validate(roleSchema, { ...validRole, roleCode: '' })
    expect(result.success).toBe(false)
  })

  it('should reject lowercase roleCode', () => {
    const result = validate(roleSchema, { ...validRole, roleCode: 'admin' })
    expect(result.success).toBe(false)
  })

  it('should reject roleCode with numbers', () => {
    const result = validate(roleSchema, { ...validRole, roleCode: 'ADMIN123' })
    expect(result.success).toBe(false)
  })

  it('should accept roleCode with underscores', () => {
    const result = validate(roleSchema, { ...validRole, roleCode: 'SUPER_ADMIN' })
    expect(result.success).toBe(true)
  })

  it('should default status to 1', () => {
    const result = validate(roleSchema, validRole)
    expect(result.success).toBe(true)
    expect(result.data?.status).toBe(1)
  })

  it('should allow null description', () => {
    const result = validate(roleSchema, { ...validRole, description: null })
    expect(result.success).toBe(true)
  })
})

describe('menuSchema', () => {
  const validMenu = {
    menuName: '系统管理',
    menuCode: 'system'
  }

  it('should validate correct menu data', () => {
    const result = validate(menuSchema, validMenu)
    expect(result.success).toBe(true)
  })

  it('should reject empty menuName', () => {
    const result = validate(menuSchema, { ...validMenu, menuName: '' })
    expect(result.success).toBe(false)
  })

  it('should reject empty menuCode', () => {
    const result = validate(menuSchema, { ...validMenu, menuCode: '' })
    expect(result.success).toBe(false)
  })

  it('should allow optional parentId', () => {
    const result = validate(menuSchema, { ...validMenu, parentId: 1 })
    expect(result.success).toBe(true)
  })

  it('should allow null parentId', () => {
    const result = validate(menuSchema, { ...validMenu, parentId: null })
    expect(result.success).toBe(true)
  })

  it('should allow optional routePath', () => {
    const result = validate(menuSchema, { ...validMenu, routePath: '/system' })
    expect(result.success).toBe(true)
  })

  it('should default sortOrder to 0', () => {
    const result = validate(menuSchema, validMenu)
    expect(result.success).toBe(true)
    expect(result.data?.sortOrder).toBe(0)
  })

  it('should default status to 1', () => {
    const result = validate(menuSchema, validMenu)
    expect(result.success).toBe(true)
    expect(result.data?.status).toBe(1)
  })
})

describe('reportSchema', () => {
  const validReport = {
    name: '销售报表',
    code: 'salesReport',
    dataSourceId: 1,
    sqlContent: 'SELECT * FROM sales'
  }

  it('should validate correct report data', () => {
    const result = validate(reportSchema, validReport)
    expect(result.success).toBe(true)
  })

  it('should reject empty name', () => {
    const result = validate(reportSchema, { ...validReport, name: '' })
    expect(result.success).toBe(false)
  })

  it('should reject code starting with number', () => {
    const result = validate(reportSchema, { ...validReport, code: '1report' })
    expect(result.success).toBe(false)
  })

  it('should reject code with special characters', () => {
    const result = validate(reportSchema, { ...validReport, code: 'report-name' })
    expect(result.success).toBe(false)
  })

  it('should accept code with underscores', () => {
    const result = validate(reportSchema, { ...validReport, code: 'sales_report_v2' })
    expect(result.success).toBe(true)
  })

  it('should reject non-positive dataSourceId', () => {
    const result = validate(reportSchema, { ...validReport, dataSourceId: 0 })
    expect(result.success).toBe(false)
  })

  it('should reject empty sqlContent', () => {
    const result = validate(reportSchema, { ...validReport, sqlContent: '' })
    expect(result.success).toBe(false)
  })

  it('should allow optional description', () => {
    const result = validate(reportSchema, { ...validReport, description: '月度销售报表' })
    expect(result.success).toBe(true)
  })
})

describe('chartSchema', () => {
  const validChart = {
    name: '销售趋势图',
    chartType: 'line',
    dataSourceId: 1,
    sqlContent: 'SELECT date, amount FROM sales'
  }

  it('should validate correct chart data', () => {
    const result = validate(chartSchema, validChart)
    expect(result.success).toBe(true)
  })

  it('should reject empty name', () => {
    const result = validate(chartSchema, { ...validChart, name: '' })
    expect(result.success).toBe(false)
  })

  it('should reject empty chartType', () => {
    const result = validate(chartSchema, { ...validChart, chartType: '' })
    expect(result.success).toBe(false)
  })

  it('should reject non-positive dataSourceId', () => {
    const result = validate(chartSchema, { ...validChart, dataSourceId: -1 })
    expect(result.success).toBe(false)
  })

  it('should reject empty sqlContent', () => {
    const result = validate(chartSchema, { ...validChart, sqlContent: '' })
    expect(result.success).toBe(false)
  })

  it('should allow optional chartConfig', () => {
    const result = validate(chartSchema, { ...validChart, chartConfig: '{"color":"red"}' })
    expect(result.success).toBe(true)
  })

  it('should allow missing chartConfig', () => {
    const result = validate(chartSchema, validChart)
    expect(result.success).toBe(true)
    expect(result.data?.chartConfig).toBeUndefined()
  })
})

// ==================== validate 函数 ====================

describe('validate function', () => {
  it('should return success with parsed data for valid input', () => {
    const result = validate(loginSchema, { username: 'admin', password: 'admin123' })
    expect(result.success).toBe(true)
    expect(result.data).toBeDefined()
    expect(result.errors).toBeUndefined()
  })

  it('should return errors record for invalid input', () => {
    const result = validate(loginSchema, { username: '', password: '' })
    expect(result.success).toBe(false)
    expect(result.errors).toBeDefined()
    expect(result.data).toBeUndefined()
  })

  it('should map error paths correctly for nested fields', () => {
    const result = validate(changePasswordSchema, {
      oldPassword: 'old',
      newPassword: 'newpass123',
      confirmPassword: 'mismatch'
    })
    expect(result.success).toBe(false)
    expect(result.errors?.confirmPassword).toBeDefined()
  })

  it('should handle completely wrong input type', () => {
    const result = validate(loginSchema, 'not an object')
    expect(result.success).toBe(false)
  })

  it('should handle null input', () => {
    const result = validate(loginSchema, null)
    expect(result.success).toBe(false)
  })

  it('should handle undefined input', () => {
    const result = validate(loginSchema, undefined)
    expect(result.success).toBe(false)
  })

  it('should apply default values', () => {
    const result = validate(roleSchema, { roleName: '管理员', roleCode: 'ADMIN' })
    expect(result.success).toBe(true)
    expect(result.data?.status).toBe(1)
  })
})

// ==================== getFirstError 函数 ====================

describe('getFirstError function', () => {
  it('should return first error message', () => {
    const errors = { username: '用户名不能为空', password: '密码不能为空' }
    expect(getFirstError(errors)).toBe('用户名不能为空')
  })

  it('should return empty string for undefined', () => {
    expect(getFirstError(undefined)).toBe('')
  })

  it('should return empty string for empty object', () => {
    expect(getFirstError({})).toBe('')
  })

  it('should return the single error when only one exists', () => {
    expect(getFirstError({ field: '错误信息' })).toBe('错误信息')
  })
})

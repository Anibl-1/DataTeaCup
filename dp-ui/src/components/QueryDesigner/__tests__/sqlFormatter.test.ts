import { describe, it, expect } from 'vitest'
import { formatSQL, compressSQL, validateSQLSyntax } from '../sqlFormatter'

describe('sqlFormatter', () => {
  describe('formatSQL', () => {
    it('should format a simple SELECT query', () => {
      const sql = 'SELECT id, name FROM users'
      const formatted = formatSQL(sql)
      
      expect(formatted).toContain('SELECT')
      expect(formatted).toContain('FROM users')
    })

    it('should format SELECT with multiple fields on separate lines', () => {
      const sql = 'SELECT id, name, email, created_at FROM users'
      const formatted = formatSQL(sql)
      
      // Should have newlines for multiple fields
      expect(formatted.split('\n').length).toBeGreaterThan(1)
    })

    it('should format JOIN clauses', () => {
      const sql = 'SELECT u.id, o.total FROM users AS u INNER JOIN orders AS o ON u.id = o.user_id'
      const formatted = formatSQL(sql)
      
      expect(formatted).toContain('INNER JOIN')
      expect(formatted).toContain('ON')
    })

    it('should format WHERE clause with AND/OR', () => {
      const sql = 'SELECT * FROM users WHERE status = 1 AND age > 18 OR role = "admin"'
      const formatted = formatSQL(sql)
      
      expect(formatted).toContain('WHERE')
      expect(formatted).toContain('AND')
      expect(formatted).toContain('OR')
    })

    it('should format GROUP BY and HAVING clauses', () => {
      const sql = 'SELECT department, COUNT(*) FROM employees GROUP BY department HAVING COUNT(*) > 5'
      const formatted = formatSQL(sql)
      
      expect(formatted).toContain('GROUP BY')
      expect(formatted).toContain('HAVING')
    })

    it('should format ORDER BY and LIMIT clauses', () => {
      const sql = 'SELECT * FROM products ORDER BY price DESC LIMIT 10'
      const formatted = formatSQL(sql)
      
      expect(formatted).toContain('ORDER BY')
      expect(formatted).toContain('DESC')
      expect(formatted).toContain('LIMIT')
    })

    it('should uppercase SQL keywords', () => {
      const sql = 'select id from users where status = 1'
      const formatted = formatSQL(sql)
      
      expect(formatted).toContain('SELECT')
      expect(formatted).toContain('FROM')
      expect(formatted).toContain('WHERE')
    })

    it('should handle aggregate functions', () => {
      const sql = 'SELECT COUNT(*), SUM(amount), AVG(price) FROM orders'
      const formatted = formatSQL(sql)
      
      expect(formatted).toContain('COUNT(*)')
      expect(formatted).toContain('SUM(amount)')
      expect(formatted).toContain('AVG(price)')
    })

    it('should handle empty input', () => {
      expect(formatSQL('')).toBe('')
      expect(formatSQL('   ')).toBe('')
    })

    it('should handle complex nested query', () => {
      const sql = 'SELECT t1.id, t1.name, t2.total FROM table1 AS t1 LEFT JOIN table2 AS t2 ON t1.id = t2.t1_id WHERE t1.status = 1 AND t2.amount > 100 GROUP BY t1.id ORDER BY t2.total DESC LIMIT 50'
      const formatted = formatSQL(sql)
      
      expect(formatted).toContain('SELECT')
      expect(formatted).toContain('LEFT JOIN')
      expect(formatted).toContain('WHERE')
      expect(formatted).toContain('GROUP BY')
      expect(formatted).toContain('ORDER BY')
      expect(formatted).toContain('LIMIT')
    })
  })

  describe('compressSQL', () => {
    it('should compress formatted SQL to single line', () => {
      const sql = `SELECT
  id,
  name
FROM users
WHERE status = 1`
      const compressed = compressSQL(sql)
      
      expect(compressed).not.toContain('\n')
      expect(compressed).toContain('SELECT')
      expect(compressed).toContain('FROM')
    })

    it('should handle empty input', () => {
      expect(compressSQL('')).toBe('')
    })
  })

  describe('validateSQLSyntax', () => {
    it('should validate correct SQL', () => {
      const result = validateSQLSyntax('SELECT id FROM users')
      expect(result.valid).toBe(true)
      expect(result.errors).toHaveLength(0)
    })

    it('should reject empty SQL', () => {
      const result = validateSQLSyntax('')
      expect(result.valid).toBe(false)
      expect(result.errors).toContain('SQL 语句不能为空')
    })

    it('should reject SQL without SELECT', () => {
      const result = validateSQLSyntax('UPDATE users SET name = "test"')
      expect(result.valid).toBe(false)
      expect(result.errors.some(e => e.includes('SELECT'))).toBe(true)
    })

    it('should reject SQL without FROM', () => {
      const result = validateSQLSyntax('SELECT 1 + 1')
      expect(result.valid).toBe(false)
      expect(result.errors.some(e => e.includes('FROM'))).toBe(true)
    })

    it('should detect unmatched parentheses', () => {
      const result = validateSQLSyntax('SELECT COUNT(* FROM users')
      expect(result.valid).toBe(false)
      expect(result.errors.some(e => e.includes('括号'))).toBe(true)
    })
  })
})

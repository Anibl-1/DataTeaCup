import request from './request'
import type { RlsRule } from '@/types/rls'

/**
 * 保存 RLS 规则
 */
export function saveRule(rule: RlsRule) {
  return request<RlsRule>({
    url: '/rls/rule',
    method: 'post',
    data: rule
  })
}

/**
 * 根据角色获取规则
 */
export function getRulesByRole(roleId: number) {
  return request<RlsRule[]>({
    url: `/rls/rules/role/${roleId}`,
    method: 'get'
  })
}

/**
 * 获取所有规则
 */
export function getAllRules() {
  return request<RlsRule[]>({
    url: '/rls/rules',
    method: 'get'
  })
}

/**
 * 根据数据源获取规则
 */
export function getRulesByDataSource(dataSourceId: number) {
  return request<RlsRule[]>({
    url: `/rls/rules/datasource/${dataSourceId}`,
    method: 'get'
  })
}

/**
 * 更新 RLS 规则
 */
export function updateRule(rule: RlsRule) {
  return request<RlsRule>({
    url: `/rls/rule/${rule.id}`,
    method: 'put',
    data: rule
  })
}

/**
 * 删除规则
 */
export function deleteRule(id: number) {
  return request<void>({
    url: `/rls/rule/${id}`,
    method: 'delete'
  })
}

/**
 * 测试 SQL 注入
 */
export function testInject(sql: string) {
  return request<string>({
    url: '/rls/test-inject',
    method: 'post',
    data: sql,
    headers: { 'Content-Type': 'text/plain' }
  })
}

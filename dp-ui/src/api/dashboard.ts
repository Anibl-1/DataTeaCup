/* eslint-disable @typescript-eslint/no-explicit-any */
import request from './request'
import type { ApiResponse } from '@/types/api'
import type { DashboardStats } from '@/types/dashboard'

/**
 * 获取仪表盘统计数据
 */
export const getDashboardStats = () => {
  return request.get<ApiResponse<DashboardStats>>('/dashboard/stats')
}

/**
 * 获取数据源类型分布
 */
export const getDataSourceDistribution = () => {
  return request.get('/dashboard/datasource-distribution')
}

/**
 * 获取采集任务趋势数据
 */
export const getCollectTrend = () => {
  return request.get('/dashboard/collect-trend')
}

/**
 * 获取仪表盘公告
 */
export const getDashboardAnnouncements = () => {
  return request.get<ApiResponse<any[]>>('/dashboard/announcements')
}


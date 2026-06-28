/* eslint-disable @typescript-eslint/no-explicit-any */
import request from './request'
import type { ApiResponse } from '@/types/api'
import type { DashboardLayout, DashboardTemplate, DashboardWidget } from '@/types/dashboard'

/**
 * 仪表盘设计器 API
 */

/**
 * 创建仪表盘
 */
export const saveDashboardDesigner = (data: DashboardLayout) => {
  return request.post<ApiResponse<DashboardLayout>>('/dashboard/designer', data)
}

/**
 * 更新仪表盘
 */
export const updateDashboardDesigner = (id: number, data: DashboardLayout) => {
  return request.put<ApiResponse<DashboardLayout>>(`/dashboard/designer/${id}`, data)
}

/**
 * 获取仪表盘详情
 */
export const getDashboardDesigner = (id: number) => {
  return request.get<ApiResponse<DashboardLayout>>(`/dashboard/designer/${id}`)
}

/**
 * 删除仪表盘
 */
export const deleteDashboardDesigner = (id: number) => {
  return request.delete<ApiResponse<void>>(`/dashboard/designer/${id}`)
}

/**
 * 获取仪表盘列表
 */
export const getDashboardDesignerList = (params: { page: number; size: number; keyword?: string }) => {
  return request.get<ApiResponse<{ records: DashboardLayout[]; total: number }>>('/dashboard/designer/list', { params })
}

/**
 * 保存仪表盘布局
 */
export const saveDashboardLayout = (dashboardId: number, layout: DashboardWidget[]) => {
  return request.post<ApiResponse<void>>(`/dashboard/designer/${dashboardId}/layout`, layout)
}

/**
 * 获取仪表盘布局
 */
export const getDashboardLayout = (dashboardId: number) => {
  return request.get<ApiResponse<DashboardWidget[]>>(`/dashboard/designer/${dashboardId}/layout`)
}

/**
 * 添加组件到仪表盘
 */
export const addDashboardWidget = (dashboardId: number, widget: DashboardWidget) => {
  return request.post<ApiResponse<DashboardWidget>>(`/dashboard/designer/${dashboardId}/widget`, widget)
}

/**
 * 更新仪表盘组件
 */
export const updateDashboardWidget = (dashboardId: number, widgetId: string, widget: DashboardWidget) => {
  return request.put<ApiResponse<DashboardWidget>>(`/dashboard/designer/${dashboardId}/widget/${widgetId}`, widget)
}

/**
 * 删除仪表盘组件
 */
export const deleteDashboardWidget = (dashboardId: number, widgetId: string) => {
  return request.delete<ApiResponse<void>>(`/dashboard/designer/${dashboardId}/widget/${widgetId}`)
}

/**
 * 获取仪表盘的所有组件
 */
export const getDashboardWidgets = (dashboardId: number) => {
  return request.get<ApiResponse<DashboardWidget[]>>(`/dashboard/designer/${dashboardId}/widgets`)
}

/**
 * 获取仪表盘模板列表
 */
export const getDashboardTemplates = () => {
  return request.get<ApiResponse<DashboardTemplate[]>>('/dashboard/designer/templates')
}

/**
 * 分页查询模板列表
 */
export const listDashboardTemplates = (params: { page: number; size: number }) => {
  return request.get<ApiResponse<{ records: DashboardTemplate[]; total: number }>>('/dashboard/designer/templates/list', { params })
}

/**
 * 从模板创建仪表盘
 */
export const createDashboardFromTemplate = (templateId: number, name: string) => {
  return request.post<ApiResponse<DashboardLayout>>('/dashboard/designer/from-template', { templateId, name })
}

/**
 * 将仪表盘保存为模板
 */
export const saveDashboardAsTemplate = (
  dashboardId: number, 
  templateName: string,
  category?: string,
  description?: string
) => {
  return request.post<ApiResponse<DashboardTemplate>>(
    `/dashboard/designer/${dashboardId}/save-as-template`, 
    { name: templateName, category, description }
  )
}

/**
 * 复制仪表盘
 */
export const duplicateDashboard = (id: number) => {
  return request.post<ApiResponse<DashboardLayout>>(`/dashboard/designer/${id}/duplicate`)
}

/**
 * 设置全局筛选器
 */
export const setGlobalFilters = (dashboardId: number, filters: any[]) => {
  return request.post<ApiResponse<void>>(`/dashboard/designer/${dashboardId}/filters`, filters)
}

/**
 * 获取全局筛选器
 */
export const getGlobalFilters = (dashboardId: number) => {
  return request.get<ApiResponse<any[]>>(`/dashboard/designer/${dashboardId}/filters`)
}


// ==================== 图表联动 API ====================

import type { ChartLinkConfigWithId } from '@/utils/chartLinker'

/**
 * 保存图表联动配置
 */
export const saveChartLinkConfig = (dashboardId: number, configs: ChartLinkConfigWithId[]) => {
  return request.post<ApiResponse<void>>(`/chart-link/config/${dashboardId}`, configs)
}

/**
 * 获取图表联动配置
 */
export const getChartLinkConfig = (dashboardId: number) => {
  return request.get<ApiResponse<ChartLinkConfigWithId[]>>(`/chart-link/config/${dashboardId}`)
}

/**
 * 获取联动图表数据
 */
export const getLinkedChartData = (params: {
  sourceChartId: number
  dimensionValue: string
  targetChartIds: number[]
}) => {
  return request.post<ApiResponse<Record<string, any[]>>>('/chart-link/linked-data', params)
}

/**
 * 删除联动配置
 */
export const deleteChartLinkConfig = (dashboardId: number, linkId: string) => {
  return request.delete<ApiResponse<void>>(`/chart-link/config/${dashboardId}/${linkId}`)
}

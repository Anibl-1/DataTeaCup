import request from './request'
import type { ApiResponse, PageResult } from '@/types/api'
import type { PageDefinition, BigscreenProject, PageLayoutMode } from '@/types/page'

/**
 * 获取页面定义列表
 */
export const getPageDefinitionList = (params: {
  page?: number
  pageSize?: number
  keyword?: string
  layoutMode?: PageLayoutMode
  projectId?: number
}) => {
  return request.get<ApiResponse<PageResult<PageDefinition>>>('/page-definition/list', { params })
}

/**
 * 根据ID获取页面定义
 */
export const getPageDefinitionById = (id: number) => {
  return request.get<ApiResponse<PageDefinition>>(`/page-definition/${id}`)
}

/**
 * 根据编码获取页面定义
 */
export const getPageDefinitionByCode = (code: string) => {
  return request.get<ApiResponse<PageDefinition>>(`/page-definition/code/${code}`)
}

/**
 * 创建页面定义
 */
export const createPageDefinition = (page: PageDefinition) => {
  return request.post<ApiResponse<PageDefinition>>('/page-definition', {
    pageName: page.pageName,
    pageCode: page.pageCode,
    layoutConfig: page.layoutConfig,
    description: page.description,
    theme: page.theme,
    themeConfig: page.themeConfig || null,
    parameterPanel: page.parameterPanel || null,
    status: page.status,
    charts: page.charts || [],
    layoutMode: page.layoutMode || 'desktop',
    bigscreenConfig: page.bigscreenConfig || null,
    mobileLayoutConfig: page.mobileLayoutConfig || null,
    projectId: page.projectId || null
  })
}

/**
 * 更新页面定义
 */
export const updatePageDefinition = (id: number, page: PageDefinition) => {
  return request.put<ApiResponse<PageDefinition>>(`/page-definition/${id}`, {
    pageName: page.pageName,
    pageCode: page.pageCode,
    layoutConfig: page.layoutConfig,
    description: page.description,
    theme: page.theme,
    themeConfig: page.themeConfig || null,
    parameterPanel: page.parameterPanel || null,
    status: page.status,
    charts: page.charts || [],
    layoutMode: page.layoutMode || 'desktop',
    bigscreenConfig: page.bigscreenConfig || null,
    mobileLayoutConfig: page.mobileLayoutConfig || null,
    projectId: page.projectId || null
  })
}

/**
 * 删除页面定义
 */
export const deletePageDefinition = (id: number) => {
  return request.delete<ApiResponse<void>>(`/page-definition/${id}`)
}

/**
 * 更新页面移动端配置
 */
export const updatePageMobileEnabled = (id: number, mobileEnabled: number) => {
  return request.put<ApiResponse<void>>(`/page-definition/${id}/mobile`, { mobileEnabled })
}

/**
 * 获取移动端已发布页面列表（mobileEnabled=1）
 */
export interface MobileEnabledPage {
  id: number
  page_name: string
  page_code: string
  description?: string
  layout_mode?: string
}
export const getMobileEnabledPages = () => {
  return request.get<ApiResponse<MobileEnabledPage[]>>('/page-definition/mobile-enabled')
}

/**
 * 获取各布局模式的页面数量（用于Tab计数）
 */
export const getLayoutModeCounts = () => {
  return request.get<ApiResponse<Record<string, number>>>('/page-definition/counts')
}

// ==================== 大屏项目管理 ====================

/**
 * 获取大屏项目列表
 */
export const getBigscreenProjectList = (params?: {
  page?: number
  pageSize?: number
  keyword?: string
}) => {
  return request.get<ApiResponse<PageResult<BigscreenProject>>>('/bigscreen-project/list', { params })
}

/**
 * 根据ID获取大屏项目
 */
export const getBigscreenProjectById = (id: number) => {
  return request.get<ApiResponse<BigscreenProject>>(`/bigscreen-project/${id}`)
}

/**
 * 创建大屏项目
 */
export const createBigscreenProject = (project: BigscreenProject) => {
  return request.post<ApiResponse<BigscreenProject>>('/bigscreen-project', project)
}

/**
 * 更新大屏项目
 */
export const updateBigscreenProject = (id: number, project: BigscreenProject) => {
  return request.put<ApiResponse<BigscreenProject>>(`/bigscreen-project/${id}`, project)
}

/**
 * 删除大屏项目
 */
export const deleteBigscreenProject = (id: number) => {
  return request.delete<ApiResponse<void>>(`/bigscreen-project/${id}`)
}

/**
 * 更新大屏项目页面列表
 */
export const updateBigscreenProjectPages = (id: number, pageIds: number[]) => {
  return request.put<ApiResponse<void>>(`/bigscreen-project/${id}/pages`, { pageIds })
}


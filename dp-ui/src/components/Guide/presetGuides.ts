/**
 * 预设功能引导配置
 * Preset Feature Guide Configurations
 * 
 * 为平台主要功能提供预设的引导配置。
 * 
 * 需求 19.1: WHEN 用户首次使用某功能时，THE DataTeaCup SHALL 显示功能引导教程
 */

import type { GuideConfig } from './types'

/**
 * 报表设计器引导
 */
export const REPORT_DESIGNER_GUIDE: GuideConfig = {
  id: 'report-designer',
  name: '报表设计器引导',
  description: '了解如何使用报表设计器创建数据报表',
  feature: 'report-designer',
  triggerOnce: true,
  steps: [
    {
      target: '.report-designer__sql-editor',
      title: 'SQL 编辑器',
      content: '在这里编写 SQL 查询语句，支持语法高亮和自动补全功能。',
      placement: 'right'
    },
    {
      target: '.report-designer__field-config',
      title: '字段配置',
      content: '配置报表显示的字段，可以拖拽调整字段顺序。',
      placement: 'left'
    },
    {
      target: '.report-designer__preview',
      title: '数据预览',
      content: '实时预览查询结果，帮助您快速验证 SQL 语句。',
      placement: 'top'
    },
    {
      target: '.report-designer__save-btn',
      title: '保存报表',
      content: '完成配置后，点击保存按钮保存您的报表。',
      placement: 'bottom'
    }
  ]
}

/**
 * 图表设计器引导
 */
export const CHART_DESIGNER_GUIDE: GuideConfig = {
  id: 'chart-designer',
  name: '图表设计器引导',
  description: '了解如何使用图表设计器创建数据可视化图表',
  feature: 'chart-designer',
  triggerOnce: true,
  steps: [
    {
      target: '.chart-designer__type-selector',
      title: '选择图表类型',
      content: '首先选择您想要创建的图表类型，如柱状图、折线图、饼图等。',
      placement: 'right'
    },
    {
      target: '.chart-designer__data-config',
      title: '数据配置',
      content: '配置图表的数据源和字段映射。',
      placement: 'left'
    },
    {
      target: '.chart-designer__style-panel',
      title: '样式配置',
      content: '自定义图表的颜色、字体、标题等样式。',
      placement: 'left'
    },
    {
      target: '.chart-designer__preview',
      title: '图表预览',
      content: '实时预览图表效果，所见即所得。',
      placement: 'top'
    }
  ]
}

/**
 * 仪表盘设计器引导
 */
export const DASHBOARD_DESIGNER_GUIDE: GuideConfig = {
  id: 'dashboard-designer',
  name: '仪表盘设计器引导',
  description: '了解如何创建和编辑数据仪表盘',
  feature: 'dashboard-designer',
  triggerOnce: true,
  steps: [
    {
      target: '.dashboard-designer__component-library',
      title: '组件库',
      content: '从组件库中拖拽组件到画布上，包括图表、文本、筛选器等。',
      placement: 'right'
    },
    {
      target: '.dashboard-designer__canvas',
      title: '设计画布',
      content: '在画布上自由布局组件，支持拖拽调整位置和大小。',
      placement: 'center'
    },
    {
      target: '.dashboard-designer__config-panel',
      title: '配置面板',
      content: '选中组件后，在这里配置组件的数据和样式。',
      placement: 'left'
    }
  ]
}

/**
 * 数据源管理引导
 */
export const DATASOURCE_GUIDE: GuideConfig = {
  id: 'datasource-management',
  name: '数据源管理引导',
  description: '了解如何添加和管理数据源',
  feature: 'datasource',
  triggerOnce: true,
  steps: [
    {
      target: '.datasource__add-btn',
      title: '添加数据源',
      content: '点击此按钮添加新的数据源连接。',
      placement: 'bottom'
    },
    {
      target: '.datasource__list',
      title: '数据源列表',
      content: '这里显示所有已配置的数据源，可以进行编辑、测试连接等操作。',
      placement: 'right'
    },
    {
      target: '.datasource__test-btn',
      title: '测试连接',
      content: '配置完成后，点击测试连接确保数据源可用。',
      placement: 'left'
    }
  ]
}

/**
 * 查询构建器引导
 */
export const QUERY_BUILDER_GUIDE: GuideConfig = {
  id: 'query-builder',
  name: '查询构建器引导',
  description: '了解如何使用可视化查询构建器',
  feature: 'query-builder',
  triggerOnce: true,
  steps: [
    {
      target: '.query-builder__table-list',
      title: '选择数据表',
      content: '从左侧列表中选择要查询的数据表。',
      placement: 'right'
    },
    {
      target: '.query-builder__field-selector',
      title: '选择字段',
      content: '勾选需要查询的字段，支持拖拽排序。',
      placement: 'right'
    },
    {
      target: '.query-builder__condition-panel',
      title: '添加条件',
      content: '在这里添加查询条件，支持多条件组合。',
      placement: 'top'
    },
    {
      target: '.query-builder__sql-preview',
      title: 'SQL 预览',
      content: '实时查看生成的 SQL 语句。',
      placement: 'top'
    }
  ]
}

/**
 * 所有预设引导配置
 */
export const PRESET_GUIDES: GuideConfig[] = [
  REPORT_DESIGNER_GUIDE,
  CHART_DESIGNER_GUIDE,
  DATASOURCE_GUIDE,
  QUERY_BUILDER_GUIDE
]

/**
 * 根据功能名称获取引导配置
 */
export function getGuideByFeature(feature: string): GuideConfig | undefined {
  return PRESET_GUIDES.find(guide => guide.feature === feature)
}

/**
 * 根据引导 ID 获取引导配置
 */
export function getGuideById(id: string): GuideConfig | undefined {
  return PRESET_GUIDES.find(guide => guide.id === id)
}


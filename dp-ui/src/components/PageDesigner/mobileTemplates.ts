/**
 * 移动端布局模板定义
 * 参考成熟移动端App设计，提供常用的移动端页面布局预设
 */

export interface MobileTemplate {
  id: string
  name: string
  description: string
  thumbnail: string
  width: number
  height: number
  items: Array<{
    type: 'inline' | 'static'
    chartType?: string
    staticType?: string
    label: string
    left: number
    top: number
    width: number
    height: number
  }>
}

export const MOBILE_TEMPLATES: MobileTemplate[] = [
  {
    id: 'mobile-dashboard',
    name: '数据概览',
    description: '顶部KPI + 图表列表，适合数据看板',
    thumbnail: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    width: 390,
    height: 844,
    items: [
      { type: 'static', staticType: 'title', label: '数据概览', left: 16, top: 16, width: 358, height: 40 },
      { type: 'inline', chartType: 'kpi', label: 'KPI指标', left: 16, top: 70, width: 170, height: 80 },
      { type: 'inline', chartType: 'kpi', label: 'KPI指标', left: 204, top: 70, width: 170, height: 80 },
      { type: 'inline', chartType: 'line', label: '趋势图', left: 16, top: 166, width: 358, height: 220 },
      { type: 'inline', chartType: 'bar', label: '柱状图', left: 16, top: 402, width: 358, height: 200 },
      { type: 'inline', chartType: 'pie', label: '饼图', left: 16, top: 618, width: 358, height: 200 }
    ]
  },
  {
    id: 'mobile-report',
    name: '报表页面',
    description: '多图表组合报表，适合数据分析',
    thumbnail: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    width: 390,
    height: 844,
    items: [
      { type: 'static', staticType: 'title', label: '月度报表', left: 16, top: 16, width: 358, height: 36 },
      { type: 'inline', chartType: 'bar', label: '销售额', left: 16, top: 68, width: 358, height: 200 },
      { type: 'static', staticType: 'divider', label: '分割线', left: 16, top: 280, width: 358, height: 16 },
      { type: 'inline', chartType: 'line', label: '增长趋势', left: 16, top: 308, width: 358, height: 200 },
      { type: 'inline', chartType: 'pie', label: '分类占比', left: 16, top: 524, width: 170, height: 170 },
      { type: 'inline', chartType: 'radar', label: '综合评分', left: 204, top: 524, width: 170, height: 170 },
      { type: 'inline', chartType: 'table', label: '明细数据', left: 16, top: 710, width: 358, height: 120 }
    ]
  },
  {
    id: 'mobile-monitor',
    name: '监控面板',
    description: '实时监控数据展示，适合运维场景',
    thumbnail: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    width: 390,
    height: 844,
    items: [
      { type: 'static', staticType: 'title', label: '系统监控', left: 16, top: 16, width: 358, height: 36 },
      { type: 'inline', chartType: 'gauge', label: 'CPU使用率', left: 16, top: 68, width: 170, height: 160 },
      { type: 'inline', chartType: 'gauge', label: '内存使用', left: 204, top: 68, width: 170, height: 160 },
      { type: 'inline', chartType: 'line', label: '请求趋势', left: 16, top: 244, width: 358, height: 200 },
      { type: 'inline', chartType: 'bar', label: '响应时间', left: 16, top: 460, width: 358, height: 180 },
      { type: 'inline', chartType: 'table', label: '告警列表', left: 16, top: 656, width: 358, height: 170 }
    ]
  },
  {
    id: 'mobile-card-list',
    name: '卡片列表',
    description: '卡片式布局，适合信息展示',
    thumbnail: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
    width: 390,
    height: 844,
    items: [
      { type: 'static', staticType: 'title', label: '业务数据', left: 16, top: 16, width: 358, height: 36 },
      { type: 'inline', chartType: 'kpi', label: '总收入', left: 16, top: 68, width: 170, height: 80 },
      { type: 'inline', chartType: 'kpi', label: '总订单', left: 204, top: 68, width: 170, height: 80 },
      { type: 'inline', chartType: 'kpi', label: '转化率', left: 16, top: 164, width: 170, height: 80 },
      { type: 'inline', chartType: 'kpi', label: '用户数', left: 204, top: 164, width: 170, height: 80 },
      { type: 'inline', chartType: 'line', label: '每日趋势', left: 16, top: 260, width: 358, height: 200 },
      { type: 'inline', chartType: 'bar', label: '分类统计', left: 16, top: 476, width: 358, height: 180 },
      { type: 'inline', chartType: 'pie', label: '来源分析', left: 16, top: 672, width: 358, height: 160 }
    ]
  },
  {
    id: 'mobile-sales',
    name: '销售简报',
    description: '核心销售指标+趋势+排行，适合销售团队',
    thumbnail: 'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)',
    width: 390,
    height: 844,
    items: [
      { type: 'static', staticType: 'title', label: '今日销售简报', left: 16, top: 16, width: 358, height: 36 },
      { type: 'inline', chartType: 'kpi', label: '今日GMV', left: 16, top: 68, width: 170, height: 80 },
      { type: 'inline', chartType: 'kpi', label: '订单量', left: 204, top: 68, width: 170, height: 80 },
      { type: 'inline', chartType: 'kpi', label: '客单价', left: 16, top: 164, width: 170, height: 80 },
      { type: 'inline', chartType: 'kpi', label: '转化率', left: 204, top: 164, width: 170, height: 80 },
      { type: 'inline', chartType: 'line', label: '销售趋势', left: 16, top: 260, width: 358, height: 200 },
      { type: 'inline', chartType: 'bar', label: '销售排行', left: 16, top: 476, width: 358, height: 200 },
      { type: 'inline', chartType: 'pie', label: '品类占比', left: 16, top: 692, width: 358, height: 140 }
    ]
  },
  {
    id: 'mobile-health',
    name: '健康数据',
    description: '仪表盘+趋势图，适合健康/运动数据展示',
    thumbnail: 'linear-gradient(135deg, #89f7fe 0%, #66a6ff 100%)',
    width: 390,
    height: 844,
    items: [
      { type: 'static', staticType: 'title', label: '健康数据', left: 16, top: 16, width: 358, height: 36 },
      { type: 'inline', chartType: 'gauge', label: '综合评分', left: 70, top: 68, width: 250, height: 200 },
      { type: 'inline', chartType: 'kpi', label: '步数', left: 16, top: 284, width: 170, height: 80 },
      { type: 'inline', chartType: 'kpi', label: '心率', left: 204, top: 284, width: 170, height: 80 },
      { type: 'inline', chartType: 'line', label: '运动趋势', left: 16, top: 380, width: 358, height: 200 },
      { type: 'inline', chartType: 'bar', label: '每日步数', left: 16, top: 596, width: 358, height: 200 }
    ]
  },
  {
    id: 'mobile-workspace',
    name: '工作台',
    description: '待办+统计+快速操作，适合移动办公',
    thumbnail: 'linear-gradient(135deg, #c3cfe2 0%, #f5f7fa 100%)',
    width: 390,
    height: 844,
    items: [
      { type: 'static', staticType: 'title', label: '我的工作台', left: 16, top: 16, width: 358, height: 36 },
      { type: 'inline', chartType: 'kpi', label: '待处理', left: 16, top: 68, width: 112, height: 80 },
      { type: 'inline', chartType: 'kpi', label: '进行中', left: 140, top: 68, width: 112, height: 80 },
      { type: 'inline', chartType: 'kpi', label: '已完成', left: 264, top: 68, width: 110, height: 80 },
      { type: 'inline', chartType: 'pie', label: '任务分布', left: 16, top: 164, width: 170, height: 170 },
      { type: 'inline', chartType: 'gauge', label: '完成率', left: 204, top: 164, width: 170, height: 170 },
      { type: 'inline', chartType: 'bar', label: '本周工时', left: 16, top: 350, width: 358, height: 200 },
      { type: 'inline', chartType: 'table', label: '待办事项', left: 16, top: 566, width: 358, height: 260 }
    ]
  },
  {
    id: 'mobile-comparison',
    name: '数据对比',
    description: '同比环比对比分析，适合业绩对比',
    thumbnail: 'linear-gradient(135deg, #fbc2eb 0%, #a6c1ee 100%)',
    width: 390,
    height: 844,
    items: [
      { type: 'static', staticType: 'title', label: '数据对比分析', left: 16, top: 16, width: 358, height: 36 },
      { type: 'inline', chartType: 'kpi', label: '本期', left: 16, top: 68, width: 170, height: 80 },
      { type: 'inline', chartType: 'kpi', label: '同期', left: 204, top: 68, width: 170, height: 80 },
      { type: 'inline', chartType: 'bar', label: '同比对比', left: 16, top: 164, width: 358, height: 200 },
      { type: 'inline', chartType: 'line', label: '趋势对比', left: 16, top: 380, width: 358, height: 200 },
      { type: 'inline', chartType: 'radar', label: '多维对比', left: 16, top: 596, width: 358, height: 230 }
    ]
  }
]

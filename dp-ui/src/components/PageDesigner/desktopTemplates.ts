/**
 * 桌面端布局模板定义
 * 适用于常规桌面端页面设计（1280~1440宽度）
 */

export type TemplateCategory = 'dashboard' | 'report' | 'analysis' | 'monitor' | 'business' | 'layout'

export interface DesktopTemplate {
  id: string
  name: string
  description: string
  thumbnail: string
  category: TemplateCategory
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

export const TEMPLATE_CATEGORIES: Array<{ value: TemplateCategory | 'all'; label: string }> = [
  { value: 'all', label: '全部模板' },
  { value: 'dashboard', label: '数据看板' },
  { value: 'report', label: '业务报表' },
  { value: 'analysis', label: '数据分析' },
  { value: 'monitor', label: '监控大屏' },
  { value: 'business', label: '行业场景' },
  { value: 'layout', label: '基础布局' }
]

// ====== 网格系统: padding=20, gap=20, usable=1240 ======
// 4列: w=295, x=[20, 335, 650, 965]  → right=1260
// 3列: w=400, x=[20, 440, 860]       → right=1260
// 2列: w=610, x=[20, 650]            → right=1260
// 全宽: w=1240, x=20                 → right=1260

export const DESKTOP_TEMPLATES: DesktopTemplate[] = [
  // ========== 数据看板类 ==========
  {
    id: 'desktop-dashboard',
    name: '数据看板',
    category: 'dashboard',
    description: '顶部KPI + 双列图表，通用数据看板',
    thumbnail: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '数据看板', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'static', staticType: 'kpiCard', label: '总访问量', left: 20, top: 76, width: 295, height: 120 },
      { type: 'static', staticType: 'kpiCard', label: '活跃用户', left: 335, top: 76, width: 295, height: 120 },
      { type: 'static', staticType: 'kpiCard', label: '转化率', left: 650, top: 76, width: 295, height: 120 },
      { type: 'static', staticType: 'kpiCard', label: '总收入', left: 965, top: 76, width: 295, height: 120 },
      { type: 'inline', chartType: 'line', label: '访问趋势', left: 20, top: 220, width: 610, height: 300 },
      { type: 'inline', chartType: 'bar', label: '分类统计', left: 650, top: 220, width: 610, height: 300 },
      { type: 'inline', chartType: 'pie', label: '来源分布', left: 20, top: 544, width: 400, height: 320 },
      { type: 'inline', chartType: 'table', label: '明细数据', left: 440, top: 544, width: 820, height: 320 }
    ]
  },
  {
    id: 'desktop-analysis',
    name: '数据分析',
    category: 'analysis',
    description: '左右布局，左侧筛选+图表，右侧详情表格',
    thumbnail: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '数据分析报告', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'line', label: '趋势分析', left: 20, top: 70, width: 813, height: 280 },
      { type: 'inline', chartType: 'pie', label: '占比分析', left: 853, top: 70, width: 407, height: 280 },
      { type: 'inline', chartType: 'bar', label: '对比分析', left: 20, top: 370, width: 400, height: 260 },
      { type: 'inline', chartType: 'radar', label: '多维评估', left: 440, top: 370, width: 400, height: 260 },
      { type: 'inline', chartType: 'scatter', label: '相关性', left: 860, top: 370, width: 400, height: 260 },
      { type: 'inline', chartType: 'table', label: '数据明细', left: 20, top: 650, width: 1240, height: 230 }
    ]
  },
  {
    id: 'desktop-report',
    name: '业务报表',
    category: 'report',
    description: '经典报表布局，KPI汇总 + 多维度图表 + 表格',
    thumbnail: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    width: 1280,
    height: 960,
    items: [
      { type: 'static', staticType: 'title', label: '月度业务报表', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '本月营收', left: 20, top: 72, width: 400, height: 90 },
      { type: 'inline', chartType: 'bar', label: '环比增长', left: 440, top: 72, width: 400, height: 90 },
      { type: 'inline', chartType: 'bar', label: '客单价', left: 860, top: 72, width: 400, height: 90 },
      { type: 'inline', chartType: 'line', label: '营收趋势', left: 20, top: 182, width: 813, height: 300 },
      { type: 'inline', chartType: 'pie', label: '产品占比', left: 853, top: 182, width: 407, height: 300 },
      { type: 'inline', chartType: 'bar', label: '区域对比', left: 20, top: 502, width: 610, height: 220 },
      { type: 'inline', chartType: 'funnel', label: '转化漏斗', left: 650, top: 502, width: 610, height: 220 },
      { type: 'inline', chartType: 'table', label: '订单明细', left: 20, top: 742, width: 1240, height: 200 }
    ]
  },
  {
    id: 'desktop-operations',
    name: '运营监控',
    category: 'monitor',
    description: '多仪表盘 + 实时曲线 + 告警列表',
    thumbnail: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '运营监控中心', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'gauge', label: '系统负载', left: 20, top: 70, width: 295, height: 240 },
      { type: 'inline', chartType: 'gauge', label: '响应时间', left: 335, top: 70, width: 295, height: 240 },
      { type: 'inline', chartType: 'gauge', label: '成功率', left: 650, top: 70, width: 295, height: 240 },
      { type: 'inline', chartType: 'gauge', label: '并发数', left: 965, top: 70, width: 295, height: 240 },
      { type: 'inline', chartType: 'line', label: '实时流量', left: 20, top: 330, width: 813, height: 270 },
      { type: 'inline', chartType: 'bar', label: '错误分布', left: 853, top: 330, width: 407, height: 270 },
      { type: 'inline', chartType: 'table', label: '告警列表', left: 20, top: 620, width: 1240, height: 260 }
    ]
  },
  {
    id: 'desktop-simple-list',
    name: '列表页面',
    category: 'layout',
    description: '简洁列表布局，适合数据管理页面',
    thumbnail: 'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)',
    width: 1280,
    height: 800,
    items: [
      { type: 'static', staticType: 'title', label: '数据管理', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '总记录', left: 20, top: 72, width: 295, height: 80 },
      { type: 'inline', chartType: 'bar', label: '今日新增', left: 335, top: 72, width: 295, height: 80 },
      { type: 'inline', chartType: 'bar', label: '待处理', left: 650, top: 72, width: 295, height: 80 },
      { type: 'inline', chartType: 'bar', label: '完成率', left: 965, top: 72, width: 295, height: 80 },
      { type: 'inline', chartType: 'table', label: '数据列表', left: 20, top: 172, width: 1240, height: 600 }
    ]
  },
  {
    id: 'desktop-finance',
    name: '财务报表',
    category: 'business',
    description: '财务数据汇总+趋势+明细，适合财务分析',
    thumbnail: 'linear-gradient(135deg, #2b5876 0%, #4e4376 100%)',
    width: 1280,
    height: 960,
    items: [
      { type: 'static', staticType: 'title', label: '财务分析报表', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '总营收', left: 20, top: 72, width: 400, height: 90 },
      { type: 'inline', chartType: 'bar', label: '净利润', left: 440, top: 72, width: 400, height: 90 },
      { type: 'inline', chartType: 'bar', label: '毛利率', left: 860, top: 72, width: 400, height: 90 },
      { type: 'inline', chartType: 'line', label: '收入趋势', left: 20, top: 182, width: 610, height: 280 },
      { type: 'inline', chartType: 'bar', label: '费用构成', left: 650, top: 182, width: 610, height: 280 },
      { type: 'inline', chartType: 'pie', label: '收入来源', left: 20, top: 482, width: 400, height: 240 },
      { type: 'inline', chartType: 'funnel', label: '回款漏斗', left: 440, top: 482, width: 400, height: 240 },
      { type: 'inline', chartType: 'gauge', label: '预算执行率', left: 860, top: 482, width: 400, height: 240 },
      { type: 'inline', chartType: 'table', label: '财务明细', left: 20, top: 742, width: 1240, height: 200 }
    ]
  },
  {
    id: 'desktop-project',
    name: '项目管理',
    category: 'business',
    description: '项目进度+资源分配+风险评估',
    thumbnail: 'linear-gradient(135deg, #0052D4 0%, #65C7F7 50%, #9CECFB 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '项目管理看板', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'gauge', label: '总进度', left: 20, top: 70, width: 295, height: 200 },
      { type: 'inline', chartType: 'gauge', label: '按时率', left: 335, top: 70, width: 295, height: 200 },
      { type: 'inline', chartType: 'gauge', label: '预算使用', left: 650, top: 70, width: 295, height: 200 },
      { type: 'inline', chartType: 'gauge', label: '资源利用', left: 965, top: 70, width: 295, height: 200 },
      { type: 'inline', chartType: 'bar', label: '各项目进度', left: 20, top: 290, width: 813, height: 280 },
      { type: 'inline', chartType: 'radar', label: '风险评估', left: 853, top: 290, width: 407, height: 280 },
      { type: 'inline', chartType: 'table', label: '任务清单', left: 20, top: 590, width: 1240, height: 290 }
    ]
  },
  {
    id: 'desktop-ecommerce',
    name: '电商数据',
    category: 'business',
    description: '销售额+订单+转化漏斗，适合电商运营',
    thumbnail: 'linear-gradient(135deg, #FF512F 0%, #F09819 100%)',
    width: 1280,
    height: 960,
    items: [
      { type: 'static', staticType: 'title', label: '电商运营数据', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: 'GMV', left: 20, top: 72, width: 295, height: 90 },
      { type: 'inline', chartType: 'bar', label: '订单数', left: 335, top: 72, width: 295, height: 90 },
      { type: 'inline', chartType: 'bar', label: '客单价', left: 650, top: 72, width: 295, height: 90 },
      { type: 'inline', chartType: 'bar', label: '转化率', left: 965, top: 72, width: 295, height: 90 },
      { type: 'inline', chartType: 'line', label: '销售趋势', left: 20, top: 182, width: 610, height: 260 },
      { type: 'inline', chartType: 'funnel', label: '转化漏斗', left: 650, top: 182, width: 610, height: 260 },
      { type: 'inline', chartType: 'pie', label: '品类占比', left: 20, top: 462, width: 400, height: 260 },
      { type: 'inline', chartType: 'bar', label: '热销TOP10', left: 440, top: 462, width: 820, height: 260 },
      { type: 'inline', chartType: 'table', label: '订单明细', left: 20, top: 742, width: 1240, height: 200 }
    ]
  },
  {
    id: 'desktop-three-equal',
    name: '三栏等分',
    category: 'layout',
    description: '均匀三栏布局，适合多维度数据对比',
    thumbnail: 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '多维数据分析', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '维度A概览', left: 20, top: 70, width: 400, height: 380 },
      { type: 'inline', chartType: 'line', label: '维度B趋势', left: 440, top: 70, width: 400, height: 380 },
      { type: 'inline', chartType: 'pie', label: '维度C分布', left: 860, top: 70, width: 400, height: 380 },
      { type: 'inline', chartType: 'scatter', label: '相关性分析', left: 20, top: 470, width: 610, height: 280 },
      { type: 'inline', chartType: 'radar', label: '综合评估', left: 650, top: 470, width: 610, height: 280 },
      { type: 'inline', chartType: 'table', label: '汇总数据', left: 20, top: 770, width: 1240, height: 110 }
    ]
  },
  // ========== 更多看板类 ==========
  {
    id: 'desktop-executive',
    name: '高管驾驶舱',
    category: 'dashboard',
    description: '核心KPI一览+关键趋势，适合管理层',
    thumbnail: 'linear-gradient(135deg, #1e3c72 0%, #2a5298 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '高管驾驶舱', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'gauge', label: '营收达成', left: 20, top: 70, width: 400, height: 200 },
      { type: 'inline', chartType: 'gauge', label: '利润达成', left: 440, top: 70, width: 400, height: 200 },
      { type: 'inline', chartType: 'gauge', label: '增长率', left: 860, top: 70, width: 400, height: 200 },
      { type: 'inline', chartType: 'line', label: '年度趋势', left: 20, top: 290, width: 813, height: 280 },
      { type: 'inline', chartType: 'pie', label: '业务构成', left: 853, top: 290, width: 407, height: 280 },
      { type: 'inline', chartType: 'bar', label: '部门排名', left: 20, top: 590, width: 610, height: 290 },
      { type: 'inline', chartType: 'table', label: '重点事项', left: 650, top: 590, width: 610, height: 290 }
    ]
  },
  {
    id: 'desktop-marketing',
    name: '营销分析',
    category: 'dashboard',
    description: '渠道效果+用户转化+ROI分析',
    thumbnail: 'linear-gradient(135deg, #E100FF 0%, #7F00FF 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '营销效果分析', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '总投入', left: 20, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '总曝光', left: 335, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '获客数', left: 650, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: 'ROI', left: 965, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'funnel', label: '转化漏斗', left: 20, top: 190, width: 610, height: 320 },
      { type: 'inline', chartType: 'bar', label: '渠道效果', left: 650, top: 190, width: 610, height: 320 },
      { type: 'inline', chartType: 'line', label: '投放趋势', left: 20, top: 530, width: 813, height: 350 },
      { type: 'inline', chartType: 'pie', label: '预算分配', left: 853, top: 530, width: 407, height: 350 }
    ]
  },
  // ========== 更多分析类 ==========
  {
    id: 'desktop-user-analysis',
    name: '用户分析',
    category: 'analysis',
    description: '用户画像+行为分析+留存分析',
    thumbnail: 'linear-gradient(135deg, #00c6ff 0%, #0072ff 100%)',
    width: 1280,
    height: 960,
    items: [
      { type: 'static', staticType: 'title', label: '用户分析报告', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '总用户', left: 20, top: 70, width: 295, height: 90 },
      { type: 'inline', chartType: 'bar', label: '活跃用户', left: 335, top: 70, width: 295, height: 90 },
      { type: 'inline', chartType: 'bar', label: '新增用户', left: 650, top: 70, width: 295, height: 90 },
      { type: 'inline', chartType: 'bar', label: '留存率', left: 965, top: 70, width: 295, height: 90 },
      { type: 'inline', chartType: 'pie', label: '用户来源', left: 20, top: 180, width: 400, height: 280 },
      { type: 'inline', chartType: 'bar', label: '年龄分布', left: 440, top: 180, width: 400, height: 280 },
      { type: 'inline', chartType: 'radar', label: '用户画像', left: 860, top: 180, width: 400, height: 280 },
      { type: 'inline', chartType: 'line', label: '用户增长', left: 20, top: 480, width: 610, height: 240 },
      { type: 'inline', chartType: 'heatmap', label: '活跃时段', left: 650, top: 480, width: 610, height: 240 },
      { type: 'inline', chartType: 'table', label: '用户明细', left: 20, top: 740, width: 1240, height: 200 }
    ]
  },
  {
    id: 'desktop-sales-analysis',
    name: '销售分析',
    category: 'analysis',
    description: '销售业绩+区域分布+产品分析',
    thumbnail: 'linear-gradient(135deg, #f5af19 0%, #f12711 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '销售数据分析', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '总销售额', left: 20, top: 70, width: 400, height: 100 },
      { type: 'inline', chartType: 'bar', label: '同比增长', left: 440, top: 70, width: 400, height: 100 },
      { type: 'inline', chartType: 'bar', label: '完成率', left: 860, top: 70, width: 400, height: 100 },
      { type: 'inline', chartType: 'line', label: '销售趋势', left: 20, top: 190, width: 610, height: 320 },
      { type: 'inline', chartType: 'pie', label: '区域分布', left: 650, top: 190, width: 610, height: 320 },
      { type: 'inline', chartType: 'bar', label: '产品排名', left: 20, top: 530, width: 610, height: 350 },
      { type: 'inline', chartType: 'scatter', label: '价量关系', left: 650, top: 530, width: 610, height: 350 }
    ]
  },
  // ========== 更多报表类 ==========
  {
    id: 'desktop-weekly-report',
    name: '周报模板',
    category: 'report',
    description: '周度数据汇总+对比分析',
    thumbnail: 'linear-gradient(135deg, #8E2DE2 0%, #4A00E0 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '周度运营报告', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '本周总量', left: 20, top: 70, width: 400, height: 100 },
      { type: 'inline', chartType: 'bar', label: '环比变化', left: 440, top: 70, width: 400, height: 100 },
      { type: 'inline', chartType: 'bar', label: '周目标达成', left: 860, top: 70, width: 400, height: 100 },
      { type: 'inline', chartType: 'line', label: '日趋势', left: 20, top: 190, width: 1240, height: 300 },
      { type: 'inline', chartType: 'bar', label: '分项对比', left: 20, top: 510, width: 610, height: 370 },
      { type: 'inline', chartType: 'table', label: '本周明细', left: 650, top: 510, width: 610, height: 370 }
    ]
  },
  {
    id: 'desktop-monthly-summary',
    name: '月度汇总',
    category: 'report',
    description: '月度核心指标+环比分析+明细',
    thumbnail: 'linear-gradient(135deg, #3a7bd5 0%, #00d2ff 100%)',
    width: 1280,
    height: 960,
    items: [
      { type: 'static', staticType: 'title', label: '月度业务汇总', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '月度营收', left: 20, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '环比增长', left: 335, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '同比增长', left: 650, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '目标达成', left: 965, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'line', label: '日趋势曲线', left: 20, top: 190, width: 813, height: 280 },
      { type: 'inline', chartType: 'pie', label: '构成分析', left: 853, top: 190, width: 407, height: 280 },
      { type: 'inline', chartType: 'bar', label: '分项排名', left: 20, top: 490, width: 610, height: 220 },
      { type: 'inline', chartType: 'radar', label: '多维评估', left: 650, top: 490, width: 610, height: 220 },
      { type: 'inline', chartType: 'table', label: '月度明细', left: 20, top: 730, width: 1240, height: 210 }
    ]
  },
  // ========== 更多监控类 ==========
  {
    id: 'desktop-server-monitor',
    name: '服务监控',
    category: 'monitor',
    description: 'CPU/内存/磁盘/网络监控',
    thumbnail: 'linear-gradient(135deg, #1a2a6c 0%, #b21f1f 50%, #fdbb2d 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '服务器监控', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'gauge', label: 'CPU使用率', left: 20, top: 70, width: 295, height: 200 },
      { type: 'inline', chartType: 'gauge', label: '内存使用率', left: 335, top: 70, width: 295, height: 200 },
      { type: 'inline', chartType: 'gauge', label: '磁盘使用率', left: 650, top: 70, width: 295, height: 200 },
      { type: 'inline', chartType: 'gauge', label: '网络带宽', left: 965, top: 70, width: 295, height: 200 },
      { type: 'inline', chartType: 'line', label: 'CPU趋势', left: 20, top: 290, width: 610, height: 280 },
      { type: 'inline', chartType: 'line', label: '内存趋势', left: 650, top: 290, width: 610, height: 280 },
      { type: 'inline', chartType: 'table', label: '进程列表', left: 20, top: 590, width: 1240, height: 290 }
    ]
  },
  {
    id: 'desktop-realtime-monitor',
    name: '实时监控',
    category: 'monitor',
    description: '实时数据流+告警状态+趋势',
    thumbnail: 'linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '实时监控中心', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '当前在线', left: 20, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: 'QPS', left: 335, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '响应时间', left: 650, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '错误率', left: 965, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'line', label: '实时流量', left: 20, top: 190, width: 1240, height: 300 },
      { type: 'inline', chartType: 'bar', label: '接口排名', left: 20, top: 510, width: 610, height: 370 },
      { type: 'inline', chartType: 'table', label: '告警日志', left: 650, top: 510, width: 610, height: 370 }
    ]
  },
  // ========== 更多行业场景 ==========
  {
    id: 'desktop-hr-dashboard',
    name: '人力资源',
    category: 'business',
    description: '人员结构+招聘+绩效分析',
    thumbnail: 'linear-gradient(135deg, #56ab2f 0%, #a8e063 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '人力资源看板', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '总人数', left: 20, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '本月入职', left: 335, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '本月离职', left: 650, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '离职率', left: 965, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'pie', label: '部门分布', left: 20, top: 190, width: 400, height: 320 },
      { type: 'inline', chartType: 'bar', label: '学历构成', left: 440, top: 190, width: 400, height: 320 },
      { type: 'inline', chartType: 'bar', label: '司龄分布', left: 860, top: 190, width: 400, height: 320 },
      { type: 'inline', chartType: 'line', label: '人员变化', left: 20, top: 530, width: 610, height: 350 },
      { type: 'inline', chartType: 'table', label: '招聘进度', left: 650, top: 530, width: 610, height: 350 }
    ]
  },
  {
    id: 'desktop-logistics',
    name: '物流监控',
    category: 'business',
    description: '订单量+配送效率+异常监控',
    thumbnail: 'linear-gradient(135deg, #c31432 0%, #240b36 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '物流监控中心', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '待发货', left: 20, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '配送中', left: 335, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '已签收', left: 650, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '异常件', left: 965, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'line', label: '订单趋势', left: 20, top: 190, width: 610, height: 320 },
      { type: 'inline', chartType: 'pie', label: '区域分布', left: 650, top: 190, width: 610, height: 320 },
      { type: 'inline', chartType: 'bar', label: '时效分析', left: 20, top: 530, width: 610, height: 350 },
      { type: 'inline', chartType: 'table', label: '异常订单', left: 650, top: 530, width: 610, height: 350 }
    ]
  },
  // ========== 更多基础布局 ==========
  {
    id: 'desktop-two-column',
    name: '双栏布局',
    category: 'layout',
    description: '左右对称双栏，适合对比展示',
    thumbnail: 'linear-gradient(135deg, #654ea3 0%, #eaafc8 100%)',
    width: 1280,
    height: 800,
    items: [
      { type: 'static', staticType: 'title', label: '双栏数据展示', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'line', label: '左侧图表A', left: 20, top: 70, width: 610, height: 340 },
      { type: 'inline', chartType: 'bar', label: '右侧图表A', left: 650, top: 70, width: 610, height: 340 },
      { type: 'inline', chartType: 'pie', label: '左侧图表B', left: 20, top: 430, width: 610, height: 350 },
      { type: 'inline', chartType: 'table', label: '右侧表格', left: 650, top: 430, width: 610, height: 350 }
    ]
  },
  {
    id: 'desktop-four-grid',
    name: '四宫格',
    category: 'layout',
    description: '2×2四宫格布局，适合多维对比',
    thumbnail: 'linear-gradient(135deg, #ff9966 0%, #ff5e62 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '四维数据对比', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'line', label: '维度一', left: 20, top: 70, width: 610, height: 400 },
      { type: 'inline', chartType: 'bar', label: '维度二', left: 650, top: 70, width: 610, height: 400 },
      { type: 'inline', chartType: 'pie', label: '维度三', left: 20, top: 490, width: 610, height: 390 },
      { type: 'inline', chartType: 'radar', label: '维度四', left: 650, top: 490, width: 610, height: 390 }
    ]
  },
  {
    id: 'desktop-full-table',
    name: '全表格页',
    category: 'layout',
    description: '大表格+筛选区，适合数据管理',
    thumbnail: 'linear-gradient(135deg, #373B44 0%, #4286f4 100%)',
    width: 1280,
    height: 800,
    items: [
      { type: 'static', staticType: 'title', label: '数据查询', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'table', label: '数据表格', left: 20, top: 70, width: 1240, height: 710 }
    ]
  },
  // ========== 更多数据看板 ==========
  {
    id: 'desktop-realtime-dashboard',
    name: '实时数据大盘',
    category: 'dashboard',
    description: '实时数据监控，多指标并行展示',
    thumbnail: 'linear-gradient(135deg, #00c6fb 0%, #005bea 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '实时数据大盘', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '当前在线', left: 20, top: 70, width: 195, height: 100 },
      { type: 'inline', chartType: 'bar', label: '今日PV', left: 230, top: 70, width: 195, height: 100 },
      { type: 'inline', chartType: 'bar', label: '今日UV', left: 440, top: 70, width: 195, height: 100 },
      { type: 'inline', chartType: 'bar', label: '转化订单', left: 650, top: 70, width: 195, height: 100 },
      { type: 'inline', chartType: 'bar', label: '成交金额', left: 860, top: 70, width: 195, height: 100 },
      { type: 'inline', chartType: 'bar', label: '客单价', left: 1070, top: 70, width: 190, height: 100 },
      { type: 'inline', chartType: 'line', label: '流量趋势', left: 20, top: 190, width: 820, height: 340 },
      { type: 'inline', chartType: 'pie', label: '渠道来源', left: 860, top: 190, width: 400, height: 340 },
      { type: 'inline', chartType: 'bar', label: '热门页面', left: 20, top: 550, width: 610, height: 330 },
      { type: 'inline', chartType: 'table', label: '实时访客', left: 650, top: 550, width: 610, height: 330 }
    ]
  },
  {
    id: 'desktop-kpi-center',
    name: 'KPI指标中心',
    category: 'dashboard',
    description: '核心KPI一览，支持多维度下钻',
    thumbnail: 'linear-gradient(135deg, #f857a6 0%, #ff5858 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: 'KPI指标中心', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '营收目标', left: 20, top: 70, width: 400, height: 120 },
      { type: 'inline', chartType: 'bar', label: '利润目标', left: 440, top: 70, width: 400, height: 120 },
      { type: 'inline', chartType: 'bar', label: '增长目标', left: 860, top: 70, width: 400, height: 120 },
      { type: 'inline', chartType: 'line', label: '目标达成趋势', left: 20, top: 210, width: 610, height: 320 },
      { type: 'inline', chartType: 'radar', label: '多维度评估', left: 650, top: 210, width: 610, height: 320 },
      { type: 'inline', chartType: 'bar', label: '部门KPI对比', left: 20, top: 550, width: 820, height: 330 },
      { type: 'inline', chartType: 'pie', label: '权重分布', left: 860, top: 550, width: 400, height: 330 }
    ]
  },
  // ========== 更多数据分析 ==========
  {
    id: 'desktop-customer-analysis',
    name: '客户分析',
    category: 'analysis',
    description: '客户画像、行为分析、价值分层',
    thumbnail: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '客户深度分析', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '客户总数', left: 20, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '新增客户', left: 335, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '活跃客户', left: 650, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '流失率', left: 965, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'pie', label: '客户分层', left: 20, top: 190, width: 400, height: 340 },
      { type: 'inline', chartType: 'bar', label: '消费分布', left: 440, top: 190, width: 400, height: 340 },
      { type: 'inline', chartType: 'scatter', label: 'RFM分析', left: 860, top: 190, width: 400, height: 340 },
      { type: 'inline', chartType: 'line', label: '客户增长趋势', left: 20, top: 550, width: 610, height: 330 },
      { type: 'inline', chartType: 'funnel', label: '转化漏斗', left: 650, top: 550, width: 610, height: 330 }
    ]
  },
  {
    id: 'desktop-product-analysis',
    name: '产品分析',
    category: 'analysis',
    description: '产品销量、库存、毛利分析',
    thumbnail: 'linear-gradient(135deg, #96fbc4 0%, #f9f586 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '产品分析报告', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: 'SKU数', left: 20, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '销售额', left: 335, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '库存量', left: 650, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '毛利率', left: 965, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '品类销量排行', left: 20, top: 190, width: 610, height: 340 },
      { type: 'inline', chartType: 'pie', label: '品类占比', left: 650, top: 190, width: 610, height: 340 },
      { type: 'inline', chartType: 'line', label: '销量趋势', left: 20, top: 550, width: 610, height: 330 },
      { type: 'inline', chartType: 'table', label: '产品明细', left: 650, top: 550, width: 610, height: 330 }
    ]
  },
  {
    id: 'desktop-traffic-analysis',
    name: '流量分析',
    category: 'analysis',
    description: '访问来源、页面热度、用户路径',
    thumbnail: 'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '流量分析', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: 'PV', left: 20, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: 'UV', left: 335, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '跳出率', left: 650, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '平均停留', left: 965, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'line', label: '流量趋势', left: 20, top: 190, width: 820, height: 320 },
      { type: 'inline', chartType: 'pie', label: '来源渠道', left: 860, top: 190, width: 400, height: 320 },
      { type: 'inline', chartType: 'bar', label: '页面热度TOP10', left: 20, top: 530, width: 610, height: 350 },
      { type: 'inline', chartType: 'funnel', label: '转化路径', left: 650, top: 530, width: 610, height: 350 }
    ]
  },
  // ========== 更多业务报表 ==========
  {
    id: 'desktop-daily-report',
    name: '日报模板',
    category: 'report',
    description: '每日数据汇总，适合日常运营',
    thumbnail: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '运营日报', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '今日订单', left: 20, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '今日营收', left: 335, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '环比昨日', left: 650, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '同比上周', left: 965, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'line', label: '24小时趋势', left: 20, top: 190, width: 1240, height: 300 },
      { type: 'inline', chartType: 'bar', label: '品类销售', left: 20, top: 510, width: 610, height: 370 },
      { type: 'inline', chartType: 'table', label: '明细数据', left: 650, top: 510, width: 610, height: 370 }
    ]
  },
  {
    id: 'desktop-quarterly-report',
    name: '季度报告',
    category: 'report',
    description: '季度业绩汇总，目标达成分析',
    thumbnail: 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)',
    width: 1280,
    height: 960,
    items: [
      { type: 'static', staticType: 'title', label: '季度业绩报告', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '季度营收', left: 20, top: 70, width: 400, height: 120 },
      { type: 'inline', chartType: 'bar', label: '目标达成', left: 440, top: 70, width: 400, height: 120 },
      { type: 'inline', chartType: 'bar', label: '同比增长', left: 860, top: 70, width: 400, height: 120 },
      { type: 'inline', chartType: 'line', label: '月度趋势对比', left: 20, top: 210, width: 820, height: 340 },
      { type: 'inline', chartType: 'radar', label: '多维评估', left: 860, top: 210, width: 400, height: 340 },
      { type: 'inline', chartType: 'bar', label: '部门贡献', left: 20, top: 570, width: 610, height: 370 },
      { type: 'inline', chartType: 'pie', label: '业务构成', left: 650, top: 570, width: 610, height: 370 }
    ]
  },
  {
    id: 'desktop-annual-report',
    name: '年度总结',
    category: 'report',
    description: '年度业绩回顾，战略规划展示',
    thumbnail: 'linear-gradient(135deg, #fc4a1a 0%, #f7b733 100%)',
    width: 1280,
    height: 1000,
    items: [
      { type: 'static', staticType: 'title', label: '年度业绩总结', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '年度营收', left: 20, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '年度利润', left: 335, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '客户增长', left: 650, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '市场份额', left: 965, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'line', label: '12月趋势', left: 20, top: 190, width: 1240, height: 280 },
      { type: 'inline', chartType: 'bar', label: '区域业绩', left: 20, top: 490, width: 610, height: 240 },
      { type: 'inline', chartType: 'pie', label: '产品线构成', left: 650, top: 490, width: 610, height: 240 },
      { type: 'inline', chartType: 'table', label: '年度明细', left: 20, top: 750, width: 1240, height: 230 }
    ]
  },
  // ========== 更多监控大屏 ==========
  {
    id: 'desktop-server-monitor',
    name: '服务器监控',
    category: 'monitor',
    description: 'CPU、内存、磁盘、网络实时监控',
    thumbnail: 'linear-gradient(135deg, #232526 0%, #414345 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '服务器监控', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: 'CPU使用率', left: 20, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '内存使用', left: 335, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '磁盘IO', left: 650, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '网络带宽', left: 965, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'line', label: 'CPU趋势', left: 20, top: 190, width: 610, height: 250 },
      { type: 'inline', chartType: 'line', label: '内存趋势', left: 650, top: 190, width: 610, height: 250 },
      { type: 'inline', chartType: 'line', label: '磁盘IO趋势', left: 20, top: 460, width: 610, height: 200 },
      { type: 'inline', chartType: 'line', label: '网络流量', left: 650, top: 460, width: 610, height: 200 },
      { type: 'inline', chartType: 'table', label: '告警列表', left: 20, top: 680, width: 1240, height: 200 }
    ]
  },
  {
    id: 'desktop-api-monitor',
    name: 'API监控',
    category: 'monitor',
    description: '接口调用量、响应时间、错误率',
    thumbnail: 'linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: 'API监控中心', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '调用总量', left: 20, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '成功率', left: 335, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '平均耗时', left: 650, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: 'P99耗时', left: 965, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'line', label: 'QPS趋势', left: 20, top: 190, width: 610, height: 320 },
      { type: 'inline', chartType: 'line', label: '响应时间', left: 650, top: 190, width: 610, height: 320 },
      { type: 'inline', chartType: 'bar', label: '接口TOP10', left: 20, top: 530, width: 610, height: 350 },
      { type: 'inline', chartType: 'table', label: '错误日志', left: 650, top: 530, width: 610, height: 350 }
    ]
  },
  {
    id: 'desktop-business-monitor',
    name: '业务监控',
    category: 'monitor',
    description: '订单、支付、库存实时监控',
    thumbnail: 'linear-gradient(135deg, #834d9b 0%, #d04ed6 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '业务监控大屏', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '待支付', left: 20, top: 70, width: 195, height: 100 },
      { type: 'inline', chartType: 'bar', label: '已支付', left: 230, top: 70, width: 195, height: 100 },
      { type: 'inline', chartType: 'bar', label: '已发货', left: 440, top: 70, width: 195, height: 100 },
      { type: 'inline', chartType: 'bar', label: '已完成', left: 650, top: 70, width: 195, height: 100 },
      { type: 'inline', chartType: 'bar', label: '退款中', left: 860, top: 70, width: 195, height: 100 },
      { type: 'inline', chartType: 'bar', label: '异常单', left: 1070, top: 70, width: 190, height: 100 },
      { type: 'inline', chartType: 'line', label: '订单趋势', left: 20, top: 190, width: 610, height: 340 },
      { type: 'inline', chartType: 'pie', label: '订单状态', left: 650, top: 190, width: 610, height: 340 },
      { type: 'inline', chartType: 'table', label: '异常订单', left: 20, top: 550, width: 1240, height: 330 }
    ]
  },
  // ========== 更多行业场景 ==========
  {
    id: 'desktop-education',
    name: '教育培训',
    category: 'business',
    description: '学员管理、课程分析、收入统计',
    thumbnail: 'linear-gradient(135deg, #1e3c72 0%, #2a5298 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '教育培训数据', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '学员总数', left: 20, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '新增学员', left: 335, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '续费率', left: 650, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '总课时', left: 965, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'line', label: '招生趋势', left: 20, top: 190, width: 610, height: 320 },
      { type: 'inline', chartType: 'bar', label: '课程热度', left: 650, top: 190, width: 610, height: 320 },
      { type: 'inline', chartType: 'pie', label: '学员来源', left: 20, top: 530, width: 400, height: 350 },
      { type: 'inline', chartType: 'table', label: '学员列表', left: 440, top: 530, width: 820, height: 350 }
    ]
  },
  {
    id: 'desktop-healthcare',
    name: '医疗健康',
    category: 'business',
    description: '就诊量、科室分布、收入分析',
    thumbnail: 'linear-gradient(135deg, #00b09b 0%, #96c93d 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '医疗数据看板', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '今日就诊', left: 20, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '预约挂号', left: 335, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '住院人数', left: 650, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '床位使用', left: 965, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'line', label: '就诊趋势', left: 20, top: 190, width: 610, height: 320 },
      { type: 'inline', chartType: 'pie', label: '科室分布', left: 650, top: 190, width: 610, height: 320 },
      { type: 'inline', chartType: 'bar', label: '收入分析', left: 20, top: 530, width: 610, height: 350 },
      { type: 'inline', chartType: 'table', label: '科室统计', left: 650, top: 530, width: 610, height: 350 }
    ]
  },
  {
    id: 'desktop-restaurant',
    name: '餐饮管理',
    category: 'business',
    description: '营业额、客流量、菜品销量',
    thumbnail: 'linear-gradient(135deg, #f12711 0%, #f5af19 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '餐饮数据中心', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '今日营业', left: 20, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '客流量', left: 335, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '桌均消费', left: 650, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '翻台率', left: 965, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'line', label: '营业趋势', left: 20, top: 190, width: 610, height: 320 },
      { type: 'inline', chartType: 'bar', label: '热销菜品', left: 650, top: 190, width: 610, height: 320 },
      { type: 'inline', chartType: 'pie', label: '菜品分类', left: 20, top: 530, width: 400, height: 350 },
      { type: 'inline', chartType: 'table', label: '订单明细', left: 440, top: 530, width: 820, height: 350 }
    ]
  },
  {
    id: 'desktop-realestate',
    name: '房产销售',
    category: 'business',
    description: '楼盘销售、客户来访、成交分析',
    thumbnail: 'linear-gradient(135deg, #3a1c71 0%, #d76d77 50%, #ffaf7b 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '房产销售看板', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '总房源', left: 20, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '已售套数', left: 335, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '成交金额', left: 650, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '来访客户', left: 965, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'line', label: '销售趋势', left: 20, top: 190, width: 610, height: 320 },
      { type: 'inline', chartType: 'funnel', label: '转化漏斗', left: 650, top: 190, width: 610, height: 320 },
      { type: 'inline', chartType: 'pie', label: '户型分布', left: 20, top: 530, width: 400, height: 350 },
      { type: 'inline', chartType: 'table', label: '成交记录', left: 440, top: 530, width: 820, height: 350 }
    ]
  },
  // ========== 更多基础布局 ==========
  {
    id: 'desktop-six-grid',
    name: '六宫格',
    category: 'layout',
    description: '2×3六宫格布局，均匀展示',
    thumbnail: 'linear-gradient(135deg, #2193b0 0%, #6dd5ed 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '六维数据展示', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'line', label: '图表一', left: 20, top: 70, width: 400, height: 390 },
      { type: 'inline', chartType: 'bar', label: '图表二', left: 440, top: 70, width: 400, height: 390 },
      { type: 'inline', chartType: 'pie', label: '图表三', left: 860, top: 70, width: 400, height: 390 },
      { type: 'inline', chartType: 'radar', label: '图表四', left: 20, top: 480, width: 400, height: 400 },
      { type: 'inline', chartType: 'scatter', label: '图表五', left: 440, top: 480, width: 400, height: 400 },
      { type: 'inline', chartType: 'funnel', label: '图表六', left: 860, top: 480, width: 400, height: 400 }
    ]
  },
  {
    id: 'desktop-top-detail',
    name: '顶部汇总+详情',
    category: 'layout',
    description: 'KPI汇总+大表格，适合数据查询',
    thumbnail: 'linear-gradient(135deg, #514a9d 0%, #24c6dc 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '数据查询中心', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '指标一', left: 20, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '指标二', left: 335, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '指标三', left: 650, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'bar', label: '指标四', left: 965, top: 70, width: 295, height: 100 },
      { type: 'inline', chartType: 'table', label: '数据明细', left: 20, top: 190, width: 1240, height: 690 }
    ]
  },
  {
    id: 'desktop-left-right',
    name: '左图右表',
    category: 'layout',
    description: '左侧图表+右侧表格，经典布局',
    thumbnail: 'linear-gradient(135deg, #ee9ca7 0%, #ffdde1 100%)',
    width: 1280,
    height: 900,
    items: [
      { type: 'static', staticType: 'title', label: '图表详情', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'line', label: '趋势图', left: 20, top: 70, width: 610, height: 400 },
      { type: 'inline', chartType: 'table', label: '数据表', left: 650, top: 70, width: 610, height: 400 },
      { type: 'inline', chartType: 'bar', label: '对比图', left: 20, top: 490, width: 610, height: 390 },
      { type: 'inline', chartType: 'pie', label: '占比图', left: 650, top: 490, width: 610, height: 390 }
    ]
  },
  {
    id: 'desktop-dashboard-simple',
    name: '简约看板',
    category: 'layout',
    description: '简洁风格，适合轻量级展示',
    thumbnail: 'linear-gradient(135deg, #d299c2 0%, #fef9d7 100%)',
    width: 1280,
    height: 800,
    items: [
      { type: 'static', staticType: 'title', label: '数据概览', left: 20, top: 16, width: 1240, height: 40 },
      { type: 'inline', chartType: 'bar', label: '核心指标', left: 20, top: 70, width: 610, height: 120 },
      { type: 'inline', chartType: 'bar', label: '辅助指标', left: 650, top: 70, width: 610, height: 120 },
      { type: 'inline', chartType: 'line', label: '主趋势', left: 20, top: 210, width: 1240, height: 280 },
      { type: 'inline', chartType: 'table', label: '数据列表', left: 20, top: 510, width: 1240, height: 270 }
    ]
  }
]

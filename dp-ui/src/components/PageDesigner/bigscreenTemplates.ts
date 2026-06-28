/**
 * 大屏布局模板定义
 * 参考成熟大屏项目设计，提供常用的大屏布局预设
 */

export interface BigscreenTemplate {
  id: string
  name: string
  description: string
  thumbnail: string  // CSS gradient or emoji placeholder
  width: number
  height: number
  /** 预设图表布局 */
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

/**
 * 大屏布局模板集合
 */
export const BIGSCREEN_TEMPLATES: BigscreenTemplate[] = [
  {
    id: 'classic-three-column',
    name: '经典三栏布局',
    description: '左中右三栏结构，中间区域突出展示核心数据',
    thumbnail: 'linear-gradient(135deg, #0c1527 0%, #1a2a4a 100%)',
    width: 1920,
    height: 1080,
    items: [
      // 顶部标题栏
      { type: 'static', staticType: 'headerBar', label: '大屏标题栏', left: 0, top: 0, width: 1920, height: 64 },
      // 左侧面板 - 3个图表
      { type: 'inline', chartType: 'bar', label: '柱状图', left: 20, top: 80, width: 440, height: 300 },
      { type: 'inline', chartType: 'pie', label: '饼图', left: 20, top: 400, width: 440, height: 300 },
      { type: 'inline', chartType: 'line', label: '折线图', left: 20, top: 720, width: 440, height: 300 },
      // 中间核心区域
      { type: 'inline', chartType: 'gauge', label: 'KPI卡片', left: 480, top: 80, width: 960, height: 160 },
      { type: 'inline', chartType: 'line', label: '趋势图', left: 480, top: 260, width: 960, height: 400 },
      { type: 'inline', chartType: 'table', label: '数据表格', left: 480, top: 680, width: 960, height: 340 },
      // 右侧面板 - 3个图表
      { type: 'inline', chartType: 'gauge', label: '仪表盘', left: 1460, top: 80, width: 440, height: 300 },
      { type: 'inline', chartType: 'radar', label: '雷达图', left: 1460, top: 400, width: 440, height: 300 },
      { type: 'inline', chartType: 'bar', label: '横向柱状图', left: 1460, top: 720, width: 440, height: 300 }
    ]
  },
  {
    id: 'center-focus',
    name: '中心聚焦布局',
    description: '中心区域最大化展示，四周环绕辅助数据',
    thumbnail: 'linear-gradient(135deg, #0a1628 0%, #162d50 100%)',
    width: 1920,
    height: 1080,
    items: [
      { type: 'static', staticType: 'headerBar', label: '大屏标题栏', left: 0, top: 0, width: 1920, height: 64 },
      // 顶部KPI行
      { type: 'inline', chartType: 'gauge', label: 'KPI-1', left: 20, top: 80, width: 300, height: 120 },
      { type: 'inline', chartType: 'gauge', label: 'KPI-2', left: 340, top: 80, width: 300, height: 120 },
      { type: 'inline', chartType: 'gauge', label: 'KPI-3', left: 1280, top: 80, width: 300, height: 120 },
      { type: 'inline', chartType: 'gauge', label: 'KPI-4', left: 1600, top: 80, width: 300, height: 120 },
      // 中心大图
      { type: 'inline', chartType: 'line', label: '核心趋势图', left: 340, top: 220, width: 1240, height: 500 },
      // 左右辅助
      { type: 'inline', chartType: 'bar', label: '左侧图表', left: 20, top: 220, width: 300, height: 400 },
      { type: 'inline', chartType: 'pie', label: '右侧图表', left: 1600, top: 220, width: 300, height: 400 },
      // 底部行
      { type: 'inline', chartType: 'bar', label: '底部图表1', left: 20, top: 740, width: 620, height: 300 },
      { type: 'inline', chartType: 'table', label: '底部表格', left: 660, top: 740, width: 600, height: 300 },
      { type: 'inline', chartType: 'gauge', label: '底部仪表', left: 1280, top: 740, width: 620, height: 300 }
    ]
  },
  {
    id: 'monitor-dashboard',
    name: '监控看板',
    description: '多仪表盘+KPI指标，适合实时监控场景',
    thumbnail: 'linear-gradient(135deg, #0d1b2a 0%, #1b2838 100%)',
    width: 1920,
    height: 1080,
    items: [
      { type: 'static', staticType: 'headerBar', label: '监控中心', left: 0, top: 0, width: 1920, height: 64 },
      // KPI 行
      { type: 'inline', chartType: 'gauge', label: '指标1', left: 20, top: 80, width: 360, height: 120 },
      { type: 'inline', chartType: 'gauge', label: '指标2', left: 400, top: 80, width: 360, height: 120 },
      { type: 'inline', chartType: 'gauge', label: '指标3', left: 780, top: 80, width: 360, height: 120 },
      { type: 'inline', chartType: 'gauge', label: '指标4', left: 1160, top: 80, width: 360, height: 120 },
      { type: 'inline', chartType: 'gauge', label: '指标5', left: 1540, top: 80, width: 360, height: 120 },
      // 仪表盘行 (5列: w=360, gap=20, x=[20,400,780,1160,1540])
      { type: 'inline', chartType: 'gauge', label: '仪表盘1', left: 20, top: 220, width: 360, height: 300 },
      { type: 'inline', chartType: 'gauge', label: '仪表盘2', left: 400, top: 220, width: 360, height: 300 },
      { type: 'inline', chartType: 'gauge', label: '仪表盘3', left: 780, top: 220, width: 360, height: 300 },
      { type: 'inline', chartType: 'gauge', label: '仪表盘4', left: 1160, top: 220, width: 360, height: 300 },
      { type: 'inline', chartType: 'gauge', label: '仪表盘5', left: 1540, top: 220, width: 360, height: 300 },
      // 底部图表
      { type: 'inline', chartType: 'line', label: '趋势曲线', left: 20, top: 540, width: 940, height: 480 },
      { type: 'inline', chartType: 'table', label: '设备列表', left: 980, top: 540, width: 920, height: 480 }
    ]
  },
  {
    id: 'production-board',
    name: '生产看板',
    description: '适合制造业/生产线的数据展示',
    thumbnail: 'linear-gradient(135deg, #0f1923 0%, #1a2940 100%)',
    width: 1920,
    height: 1080,
    items: [
      { type: 'static', staticType: 'headerBar', label: '业务生产看板', left: 0, top: 0, width: 1920, height: 64 },
      // 左侧KPI列
      { type: 'inline', chartType: 'gauge', label: '今日产量', left: 20, top: 80, width: 300, height: 100 },
      { type: 'inline', chartType: 'gauge', label: '设备运行数', left: 20, top: 200, width: 300, height: 100 },
      { type: 'inline', chartType: 'gauge', label: '在岗人员数', left: 20, top: 320, width: 300, height: 100 },
      { type: 'inline', chartType: 'gauge', label: '完成率', left: 20, top: 440, width: 300, height: 100 },
      // 中心区域
      { type: 'inline', chartType: 'gauge', label: '当月完成总值', left: 340, top: 80, width: 600, height: 200 },
      { type: 'inline', chartType: 'pie', label: '质量分布', left: 960, top: 80, width: 460, height: 300 },
      { type: 'inline', chartType: 'bar', label: '每日产量', left: 340, top: 300, width: 600, height: 340 },
      // 右侧
      { type: 'inline', chartType: 'funnel', label: '不良原因', left: 1440, top: 80, width: 460, height: 300 },
      // 底部行
      { type: 'inline', chartType: 'table', label: '工单进度', left: 20, top: 660, width: 620, height: 380 },
      { type: 'inline', chartType: 'bar', label: '每日产量柱图', left: 660, top: 660, width: 620, height: 380 },
      { type: 'inline', chartType: 'bar', label: '库存情况', left: 1300, top: 400, width: 600, height: 240 },
      { type: 'inline', chartType: 'line', label: '趋势分析', left: 1300, top: 660, width: 600, height: 380 }
    ]
  },
  {
    id: 'data-overview',
    name: '数据总览',
    description: '均匀网格布局，适合多维度数据概览',
    thumbnail: 'linear-gradient(135deg, #101829 0%, #1c2d4a 100%)',
    width: 1920,
    height: 1080,
    items: [
      { type: 'static', staticType: 'headerBar', label: '数据总览', left: 0, top: 0, width: 1920, height: 64 },
      // 2x3 网格
      { type: 'inline', chartType: 'bar', label: '图表1', left: 20, top: 80, width: 620, height: 460 },
      { type: 'inline', chartType: 'line', label: '图表2', left: 660, top: 80, width: 600, height: 460 },
      { type: 'inline', chartType: 'pie', label: '图表3', left: 1280, top: 80, width: 620, height: 460 },
      { type: 'inline', chartType: 'table', label: '图表4', left: 20, top: 560, width: 620, height: 460 },
      { type: 'inline', chartType: 'radar', label: '图表5', left: 660, top: 560, width: 600, height: 460 },
      { type: 'inline', chartType: 'scatter', label: '图表6', left: 1280, top: 560, width: 620, height: 460 }
    ]
  },
  {
    id: 'dual-comparison',
    name: '双屏对比',
    description: '左右对称布局，适合AB对比或同比环比分析',
    thumbnail: 'linear-gradient(135deg, #0b1d35 0%, #1e3a5f 100%)',
    width: 1920,
    height: 1080,
    items: [
      { type: 'static', staticType: 'headerBar', label: '数据对比分析', left: 0, top: 0, width: 1920, height: 64 },
      // 左半屏
      { type: 'inline', chartType: 'gauge', label: 'A组指标', left: 20, top: 80, width: 440, height: 120 },
      { type: 'inline', chartType: 'line', label: 'A组趋势', left: 20, top: 220, width: 920, height: 380 },
      { type: 'inline', chartType: 'bar', label: 'A组分类', left: 20, top: 620, width: 440, height: 400 },
      { type: 'inline', chartType: 'pie', label: 'A组占比', left: 480, top: 620, width: 460, height: 400 },
      // 右半屏
      { type: 'inline', chartType: 'gauge', label: 'B组指标', left: 1460, top: 80, width: 440, height: 120 },
      { type: 'inline', chartType: 'line', label: 'B组趋势', left: 980, top: 220, width: 920, height: 380 },
      { type: 'inline', chartType: 'bar', label: 'B组分类', left: 980, top: 620, width: 460, height: 400 },
      { type: 'inline', chartType: 'pie', label: 'B组占比', left: 1460, top: 620, width: 440, height: 400 },
      // 中间分隔KPI
      { type: 'inline', chartType: 'gauge', label: '差异率', left: 480, top: 80, width: 960, height: 120 }
    ]
  },
  {
    id: 'cockpit',
    name: '管理驾驶舱',
    description: '中心仪表+环绕指标，适合高层管理决策',
    thumbnail: 'linear-gradient(135deg, #060e1a 0%, #0f2644 100%)',
    width: 1920,
    height: 1080,
    items: [
      { type: 'static', staticType: 'headerBar', label: '管理驾驶舱', left: 0, top: 0, width: 1920, height: 64 },
      // 顶部KPI行 (6个)
      { type: 'inline', chartType: 'gauge', label: '营业收入', left: 20, top: 80, width: 300, height: 100 },
      { type: 'inline', chartType: 'gauge', label: '利润率', left: 340, top: 80, width: 300, height: 100 },
      { type: 'inline', chartType: 'gauge', label: '客户数', left: 660, top: 80, width: 300, height: 100 },
      { type: 'inline', chartType: 'gauge', label: '订单量', left: 980, top: 80, width: 300, height: 100 },
      { type: 'inline', chartType: 'gauge', label: '库存周转', left: 1300, top: 80, width: 300, height: 100 },
      { type: 'inline', chartType: 'gauge', label: '满意度', left: 1620, top: 80, width: 280, height: 100 },
      // 中心大仪表盘
      { type: 'inline', chartType: 'gauge', label: '综合健康度', left: 560, top: 200, width: 800, height: 440 },
      // 左侧
      { type: 'inline', chartType: 'bar', label: '部门业绩', left: 20, top: 200, width: 520, height: 280 },
      { type: 'inline', chartType: 'funnel', label: '销售漏斗', left: 20, top: 500, width: 520, height: 280 },
      // 右侧
      { type: 'inline', chartType: 'pie', label: '产品结构', left: 1380, top: 200, width: 520, height: 280 },
      { type: 'inline', chartType: 'radar', label: '多维评估', left: 1380, top: 500, width: 520, height: 280 },
      // 底部
      { type: 'inline', chartType: 'line', label: '月度趋势', left: 20, top: 800, width: 940, height: 240 },
      { type: 'inline', chartType: 'table', label: '关键事项', left: 980, top: 800, width: 920, height: 240 }
    ]
  },
  {
    id: 'geo-layout',
    name: '地图可视化',
    description: '中心地图+四周数据，适合区域数据展示',
    thumbnail: 'linear-gradient(135deg, #071422 0%, #132b46 100%)',
    width: 1920,
    height: 1080,
    items: [
      { type: 'static', staticType: 'headerBar', label: '区域数据总览', left: 0, top: 0, width: 1920, height: 64 },
      // 左侧面板
      { type: 'inline', chartType: 'bar', label: '区域排名', left: 20, top: 80, width: 380, height: 480 },
      { type: 'inline', chartType: 'table', label: '实时数据', left: 20, top: 580, width: 380, height: 460 },
      // 中心地图区域（用scatter代替map）
      { type: 'inline', chartType: 'scatter', label: '区域分布图', left: 420, top: 80, width: 1080, height: 640 },
      // 中下进度指标
      { type: 'inline', chartType: 'gauge', label: '完成进度', left: 420, top: 740, width: 520, height: 300 },
      { type: 'inline', chartType: 'line', label: '增长趋势', left: 960, top: 740, width: 540, height: 300 },
      // 右侧面板
      { type: 'inline', chartType: 'pie', label: '类型分布', left: 1520, top: 80, width: 380, height: 320 },
      { type: 'inline', chartType: 'gauge', label: '核心指标', left: 1520, top: 420, width: 380, height: 300 },
      { type: 'inline', chartType: 'bar', label: '月度统计', left: 1520, top: 740, width: 380, height: 300 }
    ]
  },
  {
    id: 'simple-quad',
    name: '简约四宫格',
    description: '四等分布局+顶部KPI，简洁高效',
    thumbnail: 'linear-gradient(135deg, #0d1520 0%, #1a2d42 100%)',
    width: 1920,
    height: 1080,
    items: [
      { type: 'static', staticType: 'headerBar', label: '数据监控', left: 0, top: 0, width: 1920, height: 64 },
      // KPI行
      { type: 'inline', chartType: 'gauge', label: '指标A', left: 20, top: 80, width: 460, height: 100 },
      { type: 'inline', chartType: 'gauge', label: '指标B', left: 500, top: 80, width: 460, height: 100 },
      { type: 'inline', chartType: 'gauge', label: '指标C', left: 980, top: 80, width: 460, height: 100 },
      { type: 'inline', chartType: 'gauge', label: '指标D', left: 1460, top: 80, width: 440, height: 100 },
      // 四宫格
      { type: 'inline', chartType: 'line', label: '趋势分析', left: 20, top: 200, width: 930, height: 400 },
      { type: 'inline', chartType: 'bar', label: '分类对比', left: 970, top: 200, width: 930, height: 400 },
      { type: 'inline', chartType: 'pie', label: '比例分析', left: 20, top: 620, width: 930, height: 420 },
      { type: 'inline', chartType: 'table', label: '数据明细', left: 970, top: 620, width: 930, height: 420 }
    ]
  }
]

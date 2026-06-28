/**
 * ECharts 按需注册模块
 *
 * 仅注册项目实际使用的图表类型和组件，减少构建产物体积。
 * 所有使用 echarts 的文件应从此模块导入，而非直接 import * as echarts from 'echarts'。
 *
 * 架构：核心图表启动时注册，扩展图表按需加载
 * - 核心：bar, line, pie, gauge, scatter（Dashboard / 常用页面）
 * - 扩展：heatmap, treemap, graph, radar, funnel, sankey,
 *         candlestick, boxplot, map, custom, effectScatter
 *
 * 渲染器：CanvasRenderer
 *
 * 注意：echarts-wordcloud 为独立扩展包，自行注册 wordCloud 系列类型，
 *       使用时仍需 import 'echarts-wordcloud'。
 */

import * as echarts from 'echarts/core'

// --- 核心图表（启动时注册） ---
import {
  BarChart,
  LineChart,
  PieChart,
  GaugeChart,
  ScatterChart,
} from 'echarts/charts'

// --- 核心组件 ---
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  GraphicComponent,
  DatasetComponent,
  TransformComponent,
  ToolboxComponent,
} from 'echarts/components'

// --- Renderer ---
import { CanvasRenderer } from 'echarts/renderers'

// 启动时仅注册核心图表和组件
echarts.use([
  BarChart, LineChart, PieChart, GaugeChart, ScatterChart,
  TitleComponent, TooltipComponent, LegendComponent,
  GridComponent, GraphicComponent, DatasetComponent, TransformComponent,
  ToolboxComponent,
  CanvasRenderer,
])

// --- 扩展图表和组件（按需加载） ---
export const EXTENDED_CHART_TYPES = new Set([
  'heatmap', 'treemap', 'graph', 'radar', 'funnel', 'sankey',
  'candlestick', 'boxplot', 'map', 'custom', 'effectScatter',
  'waterfall', 'wordCloud', 'combo'
])

let extendedRegistered = false
let extendedRegisterPromise: Promise<void> | null = null

export async function registerExtendedCharts(): Promise<void> {
  if (extendedRegistered) return
  if (extendedRegisterPromise) return extendedRegisterPromise

  extendedRegisterPromise = (async () => {
    const [
      { HeatmapChart, TreemapChart, GraphChart, RadarChart, FunnelChart,
        SankeyChart, CandlestickChart, BoxplotChart, MapChart, CustomChart,
        EffectScatterChart },
      { DataZoomComponent, VisualMapComponent, ToolboxComponent, MarkPointComponent }
    ] = await Promise.all([
      import('echarts/charts'),
      import('echarts/components')
    ])
    echarts.use([
      HeatmapChart, TreemapChart, GraphChart, RadarChart, FunnelChart,
      SankeyChart, CandlestickChart, BoxplotChart, MapChart, CustomChart,
      EffectScatterChart,
      DataZoomComponent, VisualMapComponent, ToolboxComponent, MarkPointComponent,
    ])
    extendedRegistered = true
  })()

  try {
    await extendedRegisterPromise
  } catch (error) {
    extendedRegisterPromise = null
    throw error
  }
}

export function optionNeedsExtendedCharts(option: any, chartType?: string): boolean {
  if (chartType && EXTENDED_CHART_TYPES.has(chartType)) return true
  if (!option || typeof option !== 'object') return false
  if (option.radar) return true
  const series = Array.isArray(option.series) ? option.series : option.series ? [option.series] : []
  return series.some((item: any) => item?.type && EXTENDED_CHART_TYPES.has(item.type))
}

export async function ensureExtendedChartsForOption(option: any, chartType?: string): Promise<void> {
  if (optionNeedsExtendedCharts(option, chartType)) {
    await registerExtendedCharts()
  }
}

// --- Type Re-exports ---
export type { ComposeOption as EChartsOption } from 'echarts/core'
export type { ECharts } from 'echarts/core'

export default echarts

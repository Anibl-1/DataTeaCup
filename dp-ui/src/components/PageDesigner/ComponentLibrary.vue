<template>
  <div class="component-library" :class="{ 'mobile-lib': currentMode === 'mobile', 'bigscreen-lib': currentMode === 'bigscreen' }">
    <n-input
      v-model:value="searchKeyword"
      :placeholder="searchPlaceholder"
      clearable
      size="small"
      style="margin-bottom: 12px"
    >
      <template #prefix>
        <n-icon><SearchOutline /></n-icon>
      </template>
    </n-input>

    <n-scrollbar style="max-height: calc(100vh - 300px)">
      <div v-for="group in visibleGroups" :key="group.key" class="type-group">
        <div
          class="group-title"
          :class="groupTitleClass(group)"
          @click="toggleGroup(group.key)"
        >
          <n-icon size="14" :component="groupExpanded[group.key] ? ChevronDownOutline : ChevronForwardOutline" />
          <span>{{ group.label }}</span>
          <n-tag size="tiny" :bordered="false" :type="group.tagType || 'info'">{{ group.items.length }}</n-tag>
        </div>
        <div v-show="groupExpanded[group.key]" class="group-items">
          <div
            v-for="item in group.items"
            :key="item.id"
            class="type-card"
            :class="cardClass(item)"
            draggable="true"
            @dragstart="handleItemDragStart($event, item)"
          >
            <div class="type-icon" :style="{ background: item.iconBg }">
              <n-icon size="18" :color="item.iconColor">
                <component :is="item.icon" />
              </n-icon>
            </div>
            <div class="type-info">
              <span class="type-label">{{ item.label }}</span>
              <span v-if="item.desc" class="type-desc">{{ item.desc }}</span>
            </div>
            <n-button size="tiny" quaternary circle class="add-btn" :class="addBtnClass(item)" @click.stop="handleItemClick(item)">
              <template #icon><n-icon size="14"><AddOutline /></n-icon></template>
            </n-button>
          </div>
        </div>
      </div>
    </n-scrollbar>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, computed, reactive, watch } from 'vue'
import { NInput, NIcon, NScrollbar, NTag, NButton } from 'naive-ui'
import {
  SearchOutline,
  ChevronDownOutline,
  ChevronForwardOutline,
  AddOutline,
  BarChartOutline,
  PieChartOutline,
  TrendingUpOutline,
  StatsChartOutline,
  PulseOutline,
  GridOutline,
  SpeedometerOutline,
  FunnelOutline,
  MapOutline,
  GitBranchOutline,
  GitMergeOutline,
  CubeOutline,
  ShareSocialOutline,
  ResizeOutline,
  AppsOutline,
  CalculatorOutline,
  TextOutline,
  CalendarOutline,
  CheckboxOutline,
  DocumentTextOutline,
  ImageOutline,
  RemoveOutline,
  PhonePortraitOutline,
  TabletPortraitOutline,
  ListOutline,
  CardOutline,
  LayersOutline,
  CloudOutline,
  GlobeOutline,
  EllipseOutline,
  TimerOutline,
  SwapHorizontalOutline,
  ReorderFourOutline
} from '@vicons/ionicons5'
import { CHART_TYPES, CHART_CATEGORY_LABELS, type ChartCategory } from '@/types/chart'
import { QUERY_COMPONENT_TYPES, createDefaultQueryComponent } from '@/types/pageParameter'
import type { QueryComponentType } from '@/types/pageParameter'

// ─── 统一组件项定义 ───
interface ComponentItem {
  id: string
  label: string
  desc?: string | undefined
  kind: 'chart' | 'static' | 'query'
  typeValue: string // chart type value / static type / query type
  icon: any
  iconColor: string
  iconBg: string
}
interface ComponentGroup {
  key: string
  label: string
  tagType?: 'info' | 'success' | 'warning' | 'error'
  items: ComponentItem[]
}

const props = defineProps<{
  layoutMode?: string
}>()

const emit = defineEmits<{
  (e: 'drag-chart-type', chartType: string): void
  (e: 'drag-query-component', data: string): void
  (e: 'drag-static-component', data: string): void
  (e: 'add-chart-type', chartType: string): void
  (e: 'add-query-component', data: string): void
  (e: 'add-static-component', data: string): void
}>()

const searchKeyword = ref('')
const currentMode = computed(() => props.layoutMode || 'desktop')

const searchPlaceholder = computed(() => {
  if (currentMode.value === 'mobile') return '搜索移动端组件...'
  if (currentMode.value === 'bigscreen') return '搜索大屏组件...'
  return '搜索组件...'
})

// ─── 图标和颜色映射 ───
const chartIconMap: Record<string, any> = {
  line: TrendingUpOutline, bar: BarChartOutline, pie: PieChartOutline,
  table: GridOutline, summaryTable: CalculatorOutline, pivotTable: AppsOutline,
  scatter: StatsChartOutline, radar: PulseOutline, gauge: SpeedometerOutline,
  funnel: FunnelOutline, heatmap: GridOutline, map: MapOutline,
  chinaMap: MapOutline, worldMap: GlobeOutline,
  tree: GitBranchOutline, sankey: GitMergeOutline, parallel: ResizeOutline,
  boxplot: CubeOutline, candlestick: TrendingUpOutline, graph: ShareSocialOutline,
  kpi: SpeedometerOutline, combo: LayersOutline, waterfall: BarChartOutline, wordCloud: CloudOutline
}
const chartColorMap: Record<string, string> = {
  line: '#5470c6', bar: '#91cc75', pie: '#fac858', table: '#36cfc9',
  summaryTable: '#36cfc9', pivotTable: '#36cfc9', scatter: '#73c0de',
  radar: '#9a60b4', gauge: '#ee6666', funnel: '#fc8452', heatmap: '#ea7ccc',
  map: '#3ba272', chinaMap: '#3ba272', worldMap: '#3ba272',
  tree: '#5470c6', sankey: '#91cc75', parallel: '#fac858',
  boxplot: '#73c0de', candlestick: '#ee6666', graph: '#9a60b4',
  kpi: '#ee6666', combo: '#5470c6', waterfall: '#91cc75', wordCloud: '#9a60b4'
}

const staticIconMap: Record<string, any> = {
  title: DocumentTextOutline, text: TextOutline, divider: RemoveOutline, image: ImageOutline,
  statusBar: PhonePortraitOutline, navbar: TabletPortraitOutline,
  tabBar: AppsOutline, mobileCard: CardOutline, listItem: ListOutline,
  searchBar: SearchOutline, progressBar: EllipseOutline,
  kpiCard: SpeedometerOutline, numberFlipper: CalculatorOutline,
  decorBorder: ResizeOutline, headerBar: LayersOutline,
  countdown: TimerOutline, scrollList: ReorderFourOutline, marquee: SwapHorizontalOutline
}

const queryIconMap: Record<string, any> = {
  text: TextOutline, number: CalculatorOutline, date: CalendarOutline,
  dateRange: CalendarOutline, select: ChevronDownOutline,
  multiSelect: CheckboxOutline, cascader: GitBranchOutline
}

// ─── 构建组件项的工具函数 ───
const makeChartItem = (chartType: typeof CHART_TYPES[number], desc?: string): ComponentItem => {
  const c = chartColorMap[chartType.value] || '#5470c6'
  return {
    id: `chart-${chartType.value}`,
    label: chartType.label,
    desc,
    kind: 'chart',
    typeValue: chartType.value,
    icon: chartIconMap[chartType.value] || BarChartOutline,
    iconColor: c,
    iconBg: `${c}18`
  }
}

const makeStaticItem = (type: string, label: string, color: string, desc?: string): ComponentItem => ({
  id: `static-${type}`,
  label,
  desc,
  kind: 'static',
  typeValue: type,
  icon: staticIconMap[type] || TextOutline,
  iconColor: color,
  iconBg: `${color}18`
})

const makeQueryItem = (comp: typeof QUERY_COMPONENT_TYPES[number]): ComponentItem => ({
  id: `query-${comp.type}`,
  label: comp.label,
  kind: 'query',
  typeValue: comp.type,
  icon: queryIconMap[comp.type] || TextOutline,
  iconColor: '#18a058',
  iconBg: 'rgba(24,160,88,0.1)'
})

// ─── 通过 CHART_TYPES 查找 ───
const findChart = (value: string) => CHART_TYPES.find(ct => ct.value === value)

// ─── 移动端组件分组 ───
const mobileGroups = computed<ComponentGroup[]>(() => [
  {
    key: 'mb-layout', label: '📱 布局组件', tagType: 'success',
    items: [
      makeStaticItem('statusBar', '状态栏', '#18a058', '顶部信号/时间'),
      makeStaticItem('navbar', '导航栏', '#18a058', '标题+返回按钮'),
      makeStaticItem('tabBar', '底部标签栏', '#18a058', '底部导航Tab'),
      makeStaticItem('searchBar', '搜索栏', '#18a058', '搜索输入框'),
    ]
  },
  {
    key: 'mb-content', label: '📋 内容组件', tagType: 'warning',
    items: [
      makeStaticItem('mobileCard', '卡片容器', '#f0a020', '圆角卡片'),
      makeStaticItem('listItem', '列表项', '#f0a020', '图标+标题+描述'),
      makeStaticItem('title', '标题', '#f0a020'),
      makeStaticItem('text', '文本段落', '#f0a020'),
      makeStaticItem('divider', '分割线', '#f0a020'),
      makeStaticItem('image', '图片', '#f0a020'),
      makeStaticItem('progressBar', '进度条', '#f0a020'),
    ]
  },
  {
    key: 'mb-chart', label: '📊 数据图表', tagType: 'info',
    items: [
      findChart('line'), findChart('bar'), findChart('pie'),
      findChart('kpi'), findChart('gauge'), findChart('radar'), findChart('table')
    ].filter(Boolean).map(ct => makeChartItem(ct!, '触屏适配'))
  },
  {
    key: 'mb-query', label: '🔍 查询组件', tagType: 'success',
    items: QUERY_COMPONENT_TYPES.map(makeQueryItem)
  }
])

// ─── 大屏组件分组 ───
const bigscreenGroups = computed<ComponentGroup[]>(() => [
  {
    key: 'bs-layout', label: '🖥️ 大屏布局', tagType: 'warning',
    items: [
      makeStaticItem('headerBar', '大屏标题栏', '#00d4ff', '顶部标题+时间'),
      makeStaticItem('decorBorder', '装饰边框', '#00d4ff', '模块容器'),
      makeStaticItem('title', '标题', '#00d4ff'),
      makeStaticItem('text', '文本段落', '#00d4ff'),
      makeStaticItem('divider', '分割线', '#00d4ff'),
      makeStaticItem('image', '图片', '#00d4ff'),
    ]
  },
  {
    key: 'bs-kpi', label: '📈 数据指标', tagType: 'error',
    items: [
      makeStaticItem('kpiCard', 'KPI卡片', '#ee6666', '指标数值展示'),
      makeStaticItem('numberFlipper', '数字翻牌器', '#ee6666', '翻牌动画'),
      makeStaticItem('progressBar', '进度条', '#ee6666', '进度百分比'),
      makeStaticItem('countdown', '倒计时', '#ee6666', '时间倒计'),
    ]
  },
  {
    key: 'bs-chart', label: '📊 可视化图表', tagType: 'info',
    items: [
      findChart('bar'), findChart('line'), findChart('pie'), findChart('kpi'),
      findChart('scatter'), findChart('radar'), findChart('gauge'),
      findChart('funnel'), findChart('heatmap'), findChart('table'),
      findChart('combo'), findChart('waterfall'), findChart('wordCloud')
    ].filter(Boolean).map(ct => makeChartItem(ct!))
  },
  {
    key: 'bs-geo', label: '🗺️ 地理图表', tagType: 'info',
    items: [
      findChart('chinaMap'), findChart('worldMap'), findChart('map')
    ].filter(Boolean).map(ct => makeChartItem(ct!))
  },
  {
    key: 'bs-advanced', label: '🔬 高级图表', tagType: 'info',
    items: [
      findChart('sankey'), findChart('graph'), findChart('candlestick'),
      findChart('tree'), findChart('parallel'), findChart('boxplot')
    ].filter(Boolean).map(ct => makeChartItem(ct!))
  },
  {
    key: 'bs-dynamic', label: '🎬 动态组件', tagType: 'warning',
    items: [
      makeStaticItem('scrollList', '滚动列表', '#f0a020', '自动滚动'),
      makeStaticItem('marquee', '跑马灯', '#f0a020', '文字滚动'),
    ]
  },
  {
    key: 'bs-query', label: '🔍 查询组件', tagType: 'success',
    items: QUERY_COMPONENT_TYPES.map(makeQueryItem)
  }
])

// ─── 桌面端组件分组 ───
const desktopGroups = computed<ComponentGroup[]>(() => {
  // 按 category 分组图表
  const catMap = new Map<string, ComponentItem[]>()
  CHART_TYPES.forEach(ct => {
    const cat = ct.category
    if (!catMap.has(cat)) catMap.set(cat, [])
    catMap.get(cat)!.push(makeChartItem(ct))
  })

  const chartGroups: ComponentGroup[] = []
  // 查询组件置顶
  chartGroups.push({
    key: 'dt-query', label: '🔍 查询组件', tagType: 'success',
    items: QUERY_COMPONENT_TYPES.map(makeQueryItem)
  })
  // 图表按类别
  const catOrder: ChartCategory[] = ['basic', 'advanced', 'special', 'geographic', 'statistical', 'financial', 'relationship']
  catOrder.forEach(cat => {
    const items = catMap.get(cat)
    if (items && items.length > 0) {
      chartGroups.push({
        key: `dt-${cat}`, label: CHART_CATEGORY_LABELS[cat], tagType: 'info',
        items
      })
    }
  })
  // 静态组件
  chartGroups.push({
    key: 'dt-static', label: '🧩 静态组件', tagType: 'warning',
    items: [
      makeStaticItem('title', '标题', '#f0a020'),
      makeStaticItem('text', '文本段落', '#f0a020'),
      makeStaticItem('divider', '分割线', '#f0a020'),
      makeStaticItem('image', '图片', '#f0a020'),
      makeStaticItem('progressBar', '进度条', '#f0a020'),
    ]
  })
  return chartGroups
})

// ─── 当前模式的分组（带搜索过滤） ───
const allGroups = computed<ComponentGroup[]>(() => {
  if (currentMode.value === 'mobile') return mobileGroups.value
  if (currentMode.value === 'bigscreen') return bigscreenGroups.value
  return desktopGroups.value
})

const visibleGroups = computed(() => {
  if (!searchKeyword.value) return allGroups.value
  const kw = searchKeyword.value.toLowerCase()
  return allGroups.value
    .map(g => ({
      ...g,
      items: g.items.filter(i =>
        i.label.toLowerCase().includes(kw) ||
        i.typeValue.toLowerCase().includes(kw) ||
        (i.desc || '').toLowerCase().includes(kw)
      )
    }))
    .filter(g => g.items.length > 0)
})

// ─── 折叠状态（所有分组默认展开前3个） ───
const groupExpanded = reactive<Record<string, boolean>>({})
const toggleGroup = (key: string) => {
  if (groupExpanded[key] === undefined) groupExpanded[key] = true
  groupExpanded[key] = !groupExpanded[key]
}
// 直接在 template 用 groupExpanded[key]，初始化时设置默认值
const initGroupDefaults = () => {
  allGroups.value.forEach((g, i) => {
    if (groupExpanded[g.key] === undefined) {
      groupExpanded[g.key] = i < 3
    }
  })
}
initGroupDefaults()

// ─── CSS class helpers ───
const groupTitleClass = (group: ComponentGroup) => {
  if (group.tagType === 'success') return 'query-group-title'
  if (group.tagType === 'warning') return 'static-group-title'
  return ''
}
const cardClass = (item: ComponentItem) => {
  if (item.kind === 'query') return 'query-type'
  if (item.kind === 'static') return 'static-type'
  return ''
}
const addBtnClass = (item: ComponentItem) => {
  if (item.kind === 'query') return 'query-add'
  if (item.kind === 'static') return 'static-add'
  return ''
}

// ─── 拖拽和点击添加 ───
const handleItemDragStart = (e: DragEvent, item: ComponentItem) => {
  if (item.kind === 'chart') {
    e.dataTransfer?.setData('inlineChartType', item.typeValue)
    e.dataTransfer!.effectAllowed = 'copy'
    emit('drag-chart-type', item.typeValue)
  } else if (item.kind === 'query') {
    const comp = createDefaultQueryComponent(item.typeValue as QueryComponentType)
    const data = JSON.stringify(comp)
    e.dataTransfer?.setData('queryComponent', data)
    e.dataTransfer!.effectAllowed = 'copy'
    emit('drag-query-component', data)
  } else {
    const data = JSON.stringify({ type: item.typeValue, label: item.label })
    e.dataTransfer?.setData('staticComponent', data)
    e.dataTransfer!.effectAllowed = 'copy'
    emit('drag-static-component', data)
  }
}

const handleItemClick = (item: ComponentItem) => {
  if (item.kind === 'chart') {
    emit('add-chart-type', item.typeValue)
  } else if (item.kind === 'query') {
    const comp = createDefaultQueryComponent(item.typeValue as QueryComponentType)
    emit('add-query-component', JSON.stringify(comp))
  } else {
    emit('add-static-component', JSON.stringify({ type: item.typeValue, label: item.label }))
  }
}

// watch mode changes to re-init group defaults
watch(currentMode, () => {
  initGroupDefaults()
})
</script>

<style scoped>
.component-library {
  padding: 4px 0;
}
.type-group {
  margin-bottom: 4px;
}
.group-title {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  font-size: 12px;
  font-weight: 600;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
  user-select: none;
  transition: background 0.15s;
}
.group-title:hover {
  background: #f5f5f5;
}
.group-items {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  padding: 4px 4px 8px;
}
.type-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  background: #fff;
  border: 1px solid #eaedf3;
  border-radius: 8px;
  cursor: grab;
  transition: all 0.2s ease;
  user-select: none;
}
.type-card:hover {
  border-color: #5470c6;
  background: linear-gradient(135deg, #f4f7ff 0%, #edf1fc 100%);
  box-shadow: 0 3px 10px rgba(84, 112, 198, 0.18);
  transform: translateY(-2px);
}
.type-card:active {
  cursor: grabbing;
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(84, 112, 198, 0.12);
}
.type-card .add-btn {
  opacity: 0;
  transition: opacity 0.15s;
  flex-shrink: 0;
  color: #5470c6;
}
.type-card:hover .add-btn {
  opacity: 1;
}
.type-card .add-btn.query-add {
  color: #18a058;
}
.type-card .add-btn.static-add {
  color: #f0a020;
}
.type-card.query-type:hover {
  border-color: #18a058;
  background: linear-gradient(135deg, #f0faf4 0%, #e8f5ee 100%);
  box-shadow: 0 3px 10px rgba(24, 160, 88, 0.18);
}
.type-card.static-type:hover {
  border-color: #f0a020;
  background: linear-gradient(135deg, #fffbf0 0%, #fef5e0 100%);
  box-shadow: 0 3px 10px rgba(240, 160, 32, 0.18);
}
.type-icon {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.2s;
}
.type-card:hover .type-icon {
  transform: scale(1.08);
}
.type-icon.query-icon {
  background: rgba(24, 160, 88, 0.1);
}
.type-icon.static-icon {
  background: rgba(240, 160, 32, 0.1);
}
.type-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.type-label {
  font-size: 12px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.type-desc {
  font-size: 10px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 1px;
}

/* ══════════════════════════════════════════════════════════════
   移动端组件库样式 — 柔和绿色
   ══════════════════════════════════════════════════════════════ */
.mobile-lib .group-title {
  font-size: 13px;
  color: #2d6a4f;
}
.mobile-lib .group-title:hover {
  background: #e8f5ee;
}
.mobile-lib .type-card {
  background: #f8fdf9;
  border-color: #d4ede0;
}
.mobile-lib .type-card:hover {
  background: #e8f5ee;
  border-color: #18a058;
  box-shadow: 0 2px 8px rgba(24, 160, 88, 0.15);
}
.mobile-lib .type-label {
  color: #1b4332;
}

</style>

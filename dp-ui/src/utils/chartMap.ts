/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 地图图表工具函数
 * 提供中国地图和世界地图的 ECharts 配置构建
 */

import type { EChartsOption } from '@/utils/echarts'

/**
 * 地图数据项接口
 */
export interface MapDataItem {
  /** 区域名称（省份名/国家名） */
  name: string
  /** 数值 */
  value: number
  /** 额外数据 */
  [key: string]: any
}

/**
 * 视觉映射配置接口
 */
export interface VisualMapConfig {
  /** 最小值 */
  min?: number
  /** 最大值 */
  max?: number
  /** 是否可计算 */
  calculable?: boolean
  /** 颜色范围 */
  inRange?: {
    color?: string[]
  }
  /** 文本标签 */
  text?: [string, string]
  /** 方向 */
  orient?: 'horizontal' | 'vertical'
  /** 左侧位置 */
  left?: string | number
  /** 底部位置 */
  bottom?: string | number
  /** 右侧位置 */
  right?: string | number
  /** 顶部位置 */
  top?: string | number
}

/**
 * 地图图表配置选项
 */
export interface MapChartOptions {
  /** 图表标题 */
  title?: string
  /** 视觉映射配置 */
  visualMapConfig?: VisualMapConfig
  /** 是否显示标签 */
  showLabel?: boolean
  /** 标签字体大小 */
  labelFontSize?: number
  /** 区域默认颜色 */
  areaColor?: string
  /** 区域边框颜色 */
  borderColor?: string
  /** 高亮区域颜色 */
  emphasisAreaColor?: string
  /** 选中区域颜色 */
  selectedAreaColor?: string
  /** 是否启用缩放 */
  roam?: boolean | 'scale' | 'move'
  /** 地图缩放比例 */
  zoom?: number
  /** 地图中心点 */
  center?: [number, number]
}

/**
 * 默认视觉映射配置
 */
const DEFAULT_VISUAL_MAP_CONFIG: VisualMapConfig = {
  min: 0,
  max: 100,
  calculable: true,
  inRange: {
    color: ['#e0f3f8', '#abd9e9', '#74add1', '#4575b4', '#313695']
  },
  text: ['高', '低'],
  orient: 'vertical',
  left: 'left',
  bottom: '15%'
}

/**
 * 计算数据的最小值和最大值
 */
function calculateDataRange(data: MapDataItem[]): { min: number; max: number } {
  if (!data || data.length === 0) {
    return { min: 0, max: 100 }
  }
  
  const values = data.map(item => item.value).filter(v => typeof v === 'number' && !isNaN(v))
  
  if (values.length === 0) {
    return { min: 0, max: 100 }
  }
  
  const min = Math.min(...values)
  const max = Math.max(...values)
  
  // 如果最小值等于最大值，扩展范围
  if (min === max) {
    return { min: Math.max(0, min - 10), max: max + 10 }
  }
  
  return { min, max }
}

/**
 * 构建中国地图 ECharts 配置
 * @param data 地图数据（省份名称和数值）
 * @param options 配置选项
 * @returns ECharts 配置对象
 */
export function buildChinaMapOption(
  data: MapDataItem[],
  options: MapChartOptions = {}
): EChartsOption {
  const {
    title,
    visualMapConfig = {},
    showLabel = true,
    labelFontSize = 10,
    areaColor = '#e0e0e0',
    borderColor = '#ffffff',
    emphasisAreaColor = '#ffd666',
    selectedAreaColor = '#1890ff',
    roam = true,
    zoom = 1.2,
    center
  } = options

  // 计算数据范围
  const dataRange = calculateDataRange(data)
  
  // 合并视觉映射配置
  const mergedVisualMap: VisualMapConfig = {
    ...DEFAULT_VISUAL_MAP_CONFIG,
    min: dataRange.min,
    max: dataRange.max,
    ...visualMapConfig
  }

  const option: EChartsOption = {
    title: title ? {
      text: title,
      left: 'center',
      top: 10,
      textStyle: {
        fontSize: 16,
        fontWeight: 'bold'
      }
    } : undefined,
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        if (params.data) {
          return `${params.name}<br/>数值: ${params.data.value ?? '-'}`
        }
        return `${params.name}<br/>数值: -`
      }
    },
    visualMap: {
      type: 'continuous',
      min: mergedVisualMap.min,
      max: mergedVisualMap.max,
      calculable: mergedVisualMap.calculable,
      inRange: mergedVisualMap.inRange,
      text: mergedVisualMap.text,
      orient: mergedVisualMap.orient,
      left: mergedVisualMap.left,
      bottom: mergedVisualMap.bottom,
      right: mergedVisualMap.right,
      top: mergedVisualMap.top
    },
    series: [{
      name: title || '数据',
      type: 'map',
      map: 'china',
      roam,
      zoom,
      center,
      label: {
        show: showLabel,
        fontSize: labelFontSize,
        color: '#333'
      },
      itemStyle: {
        areaColor,
        borderColor,
        borderWidth: 1
      },
      emphasis: {
        label: {
          show: true,
          fontSize: labelFontSize + 2,
          fontWeight: 'bold'
        },
        itemStyle: {
          areaColor: emphasisAreaColor,
          shadowBlur: 10,
          shadowColor: 'rgba(0, 0, 0, 0.3)'
        }
      },
      select: {
        label: {
          show: true,
          color: '#fff'
        },
        itemStyle: {
          areaColor: selectedAreaColor
        }
      },
      data: data.map(item => ({
        name: item.name,
        value: item.value,
        ...item
      }))
    }]
  }

  return option
}

/**
 * 构建世界地图 ECharts 配置
 * @param data 地图数据（国家名称和数值）
 * @param options 配置选项
 * @returns ECharts 配置对象
 */
export function buildWorldMapOption(
  data: MapDataItem[],
  options: MapChartOptions = {}
): EChartsOption {
  const {
    title,
    visualMapConfig = {},
    showLabel = false,
    labelFontSize = 8,
    areaColor = '#e0e0e0',
    borderColor = '#ffffff',
    emphasisAreaColor = '#ffd666',
    selectedAreaColor = '#1890ff',
    roam = true,
    zoom = 1.0,
    center
  } = options

  // 计算数据范围
  const dataRange = calculateDataRange(data)
  
  // 合并视觉映射配置
  const mergedVisualMap: VisualMapConfig = {
    ...DEFAULT_VISUAL_MAP_CONFIG,
    min: dataRange.min,
    max: dataRange.max,
    orient: 'horizontal',
    left: 'center',
    bottom: '5%',
    ...visualMapConfig
  }

  const option: EChartsOption = {
    title: title ? {
      text: title,
      left: 'center',
      top: 10,
      textStyle: {
        fontSize: 16,
        fontWeight: 'bold'
      }
    } : undefined,
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        if (params.data) {
          return `${params.name}<br/>数值: ${params.data.value ?? '-'}`
        }
        return `${params.name}<br/>数值: -`
      }
    },
    visualMap: {
      type: 'continuous',
      min: mergedVisualMap.min,
      max: mergedVisualMap.max,
      calculable: mergedVisualMap.calculable,
      inRange: mergedVisualMap.inRange,
      text: mergedVisualMap.text,
      orient: mergedVisualMap.orient,
      left: mergedVisualMap.left,
      bottom: mergedVisualMap.bottom,
      right: mergedVisualMap.right,
      top: mergedVisualMap.top
    },
    series: [{
      name: title || '数据',
      type: 'map',
      map: 'world',
      roam,
      zoom,
      center,
      label: {
        show: showLabel,
        fontSize: labelFontSize,
        color: '#333'
      },
      itemStyle: {
        areaColor,
        borderColor,
        borderWidth: 0.5
      },
      emphasis: {
        label: {
          show: true,
          fontSize: labelFontSize + 2,
          fontWeight: 'bold'
        },
        itemStyle: {
          areaColor: emphasisAreaColor,
          shadowBlur: 10,
          shadowColor: 'rgba(0, 0, 0, 0.3)'
        }
      },
      select: {
        label: {
          show: true,
          color: '#fff'
        },
        itemStyle: {
          areaColor: selectedAreaColor
        }
      },
      data: data.map(item => ({
        name: item.name,
        value: item.value,
        ...item
      }))
    }]
  }

  return option
}

/**
 * 构建省级地图 ECharts 配置（用于下钻）
 * @param province 省份名称
 * @param data 地图数据（市级名称和数值）
 * @param options 配置选项
 * @returns ECharts 配置对象
 */
export function buildProvinceMapOption(
  province: string,
  data: MapDataItem[],
  options: MapChartOptions = {}
): EChartsOption {
  const {
    title,
    visualMapConfig = {},
    showLabel = true,
    labelFontSize = 10,
    areaColor = '#e0e0e0',
    borderColor = '#ffffff',
    emphasisAreaColor = '#ffd666',
    selectedAreaColor = '#1890ff',
    roam = true,
    zoom = 1.0,
    center
  } = options

  // 计算数据范围
  const dataRange = calculateDataRange(data)
  
  // 合并视觉映射配置
  const mergedVisualMap: VisualMapConfig = {
    ...DEFAULT_VISUAL_MAP_CONFIG,
    min: dataRange.min,
    max: dataRange.max,
    ...visualMapConfig
  }

  // 省份名称到地图名称的映射
  const provinceMapName = getProvinceMapName(province)

  const option: EChartsOption = {
    title: title ? {
      text: title,
      left: 'center',
      top: 10,
      textStyle: {
        fontSize: 16,
        fontWeight: 'bold'
      }
    } : undefined,
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        if (params.data) {
          return `${params.name}<br/>数值: ${params.data.value ?? '-'}`
        }
        return `${params.name}<br/>数值: -`
      }
    },
    visualMap: {
      type: 'continuous',
      min: mergedVisualMap.min,
      max: mergedVisualMap.max,
      calculable: mergedVisualMap.calculable,
      inRange: mergedVisualMap.inRange,
      text: mergedVisualMap.text,
      orient: mergedVisualMap.orient,
      left: mergedVisualMap.left,
      bottom: mergedVisualMap.bottom,
      right: mergedVisualMap.right,
      top: mergedVisualMap.top
    },
    series: [{
      name: title || province,
      type: 'map',
      map: provinceMapName,
      roam,
      zoom,
      center,
      label: {
        show: showLabel,
        fontSize: labelFontSize,
        color: '#333'
      },
      itemStyle: {
        areaColor,
        borderColor,
        borderWidth: 1
      },
      emphasis: {
        label: {
          show: true,
          fontSize: labelFontSize + 2,
          fontWeight: 'bold'
        },
        itemStyle: {
          areaColor: emphasisAreaColor,
          shadowBlur: 10,
          shadowColor: 'rgba(0, 0, 0, 0.3)'
        }
      },
      select: {
        label: {
          show: true,
          color: '#fff'
        },
        itemStyle: {
          areaColor: selectedAreaColor
        }
      },
      data: data.map(item => ({
        name: item.name,
        value: item.value,
        ...item
      }))
    }]
  }

  return option
}

/**
 * 获取省份对应的地图名称
 * @param province 省份名称
 * @returns 地图注册名称
 */
export function getProvinceMapName(province: string): string {
  // 省份名称标准化映射
  const provinceMap: Record<string, string> = {
    '北京': '北京',
    '北京市': '北京',
    '天津': '天津',
    '天津市': '天津',
    '河北': '河北',
    '河北省': '河北',
    '山西': '山西',
    '山西省': '山西',
    '内蒙古': '内蒙古',
    '内蒙古自治区': '内蒙古',
    '辽宁': '辽宁',
    '辽宁省': '辽宁',
    '吉林': '吉林',
    '吉林省': '吉林',
    '黑龙江': '黑龙江',
    '黑龙江省': '黑龙江',
    '上海': '上海',
    '上海市': '上海',
    '江苏': '江苏',
    '江苏省': '江苏',
    '浙江': '浙江',
    '浙江省': '浙江',
    '安徽': '安徽',
    '安徽省': '安徽',
    '福建': '福建',
    '福建省': '福建',
    '江西': '江西',
    '江西省': '江西',
    '山东': '山东',
    '山东省': '山东',
    '河南': '河南',
    '河南省': '河南',
    '湖北': '湖北',
    '湖北省': '湖北',
    '湖南': '湖南',
    '湖南省': '湖南',
    '广东': '广东',
    '广东省': '广东',
    '广西': '广西',
    '广西壮族自治区': '广西',
    '海南': '海南',
    '海南省': '海南',
    '重庆': '重庆',
    '重庆市': '重庆',
    '四川': '四川',
    '四川省': '四川',
    '贵州': '贵州',
    '贵州省': '贵州',
    '云南': '云南',
    '云南省': '云南',
    '西藏': '西藏',
    '西藏自治区': '西藏',
    '陕西': '陕西',
    '陕西省': '陕西',
    '甘肃': '甘肃',
    '甘肃省': '甘肃',
    '青海': '青海',
    '青海省': '青海',
    '宁夏': '宁夏',
    '宁夏回族自治区': '宁夏',
    '新疆': '新疆',
    '新疆维吾尔自治区': '新疆',
    '台湾': '台湾',
    '台湾省': '台湾',
    '香港': '香港',
    '香港特别行政区': '香港',
    '澳门': '澳门',
    '澳门特别行政区': '澳门'
  }

  return provinceMap[province] || province
}

/**
 * 中国省份列表
 */
export const CHINA_PROVINCES = [
  '北京', '天津', '河北', '山西', '内蒙古',
  '辽宁', '吉林', '黑龙江', '上海', '江苏',
  '浙江', '安徽', '福建', '江西', '山东',
  '河南', '湖北', '湖南', '广东', '广西',
  '海南', '重庆', '四川', '贵州', '云南',
  '西藏', '陕西', '甘肃', '青海', '宁夏',
  '新疆', '台湾', '香港', '澳门'
]

/**
 * 验证地图数据完整性
 * 检查所有输入区域是否都在输出配置中
 * @param data 输入数据
 * @param option 生成的 ECharts 配置
 * @returns 是否完整
 */
export function validateMapDataIntegrity(
  data: MapDataItem[],
  option: EChartsOption
): boolean {
  if (!data || data.length === 0) {
    return true
  }

  const series = option.series as any[]
  if (!series || series.length === 0) {
    return false
  }

  const seriesData = series[0].data as MapDataItem[]
  if (!seriesData) {
    return false
  }

  // 检查每个输入数据项是否都在输出中
  for (const item of data) {
    const found = seriesData.find(
      d => d.name === item.name && d.value === item.value
    )
    if (!found) {
      return false
    }
  }

  return true
}

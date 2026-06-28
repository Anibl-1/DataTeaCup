/**
 * 预设样式模板
 * Preset Style Templates
 * 
 * 提供常用报表类型的预设样式模板，包括：
 * - 财务报表样式模板（需求: 21.3.10）
 * - 销售报表样式模板（需求: 21.3.11）
 * - 库存报表样式模板（需求: 21.3.12）
 * - KPI仪表盘样式模板（需求: 21.3.13）
 * 
 * 每个模板包含：
 * - 模板元数据（id, name, description, category）
 * - 默认单元格样式
 * - 条件格式化规则
 * - 数据格式化规则
 */

import type { CellStyle, DataFormat, NumberFormat } from './types/style';
import type { ConditionalRule, ValueCondition, FormulaCondition, CrossFieldCondition } from './types/conditional';
import type { StyleConfig, RowStyleConfig } from './StyleEngine';

/**
 * 模板分类
 */
export type TemplateCategory = 'finance' | 'sales' | 'inventory' | 'kpi' | 'custom';

/**
 * 列样式配置
 */
export interface ColumnStyleConfig {
  /** 字段名匹配模式（正则表达式） */
  fieldPattern?: string;
  /** 字段类型 */
  fieldType?: 'number' | 'date' | 'text' | 'percentage' | 'currency' | 'any';
  /** 默认样式 */
  defaultStyle: CellStyle;
  /** 表头样式 */
  headerStyle?: CellStyle;
  /** 汇总行样式 */
  summaryStyle?: CellStyle;
}

/**
 * 表格样式配置
 */
export interface TableStyleConfig {
  /** 表头样式 */
  headerStyle: CellStyle;
  /** 数据行样式 */
  bodyStyle: CellStyle;
  /** 交替行样式（斑马纹） */
  alternateRowStyle?: CellStyle;
  /** 汇总行样式 */
  summaryRowStyle?: CellStyle;
  /** 边框样式 */
  borderStyle: 'none' | 'horizontal' | 'vertical' | 'all' | 'outer';
  /** 边框颜色 */
  borderColor?: string;
}

/**
 * 样式模板接口
 */
export interface StyleTemplate {
  /** 模板ID */
  id: string;
  /** 模板名称 */
  name: string;
  /** 模板分类 */
  category: TemplateCategory;
  /** 模板描述 */
  description: string;
  /** 预览图URL */
  preview?: string;
  /** 是否系统预设 */
  isSystem: boolean;
  /** 创建者ID */
  creatorId?: number;
  /** 列样式配置 */
  columnStyles: Record<string, ColumnStyleConfig>;
  /** 条件格式化规则 */
  conditionalRules: ConditionalRule[];
  /** 表格样式配置 */
  tableStyle: TableStyleConfig;
}

// ============================================================================
// 预设颜色常量
// ============================================================================

const COLORS = {
  // 基础颜色
  red: '#ff4d4f',
  lightRed: '#fff2f0',
  green: '#52c41a',
  lightGreen: '#f6ffed',
  yellow: '#faad14',
  lightYellow: '#fffbe6',
  orange: '#fa8c16',
  lightOrange: '#fff7e6',
  blue: '#1890ff',
  lightBlue: '#e6f7ff',
  purple: '#722ed1',
  lightPurple: '#f9f0ff',
  gray: '#8c8c8c',
  lightGray: '#fafafa',
  white: '#ffffff',
  black: '#000000',
  
  // 边框颜色
  border: '#e8e8e8',
  borderLight: '#f0f0f0',
  
  // 财务相关
  profit: '#52c41a',
  loss: '#ff4d4f',
  neutral: '#8c8c8c',
  
  // 库存相关
  danger: '#ff4d4f',
  warning: '#faad14',
  safe: '#52c41a',
  
  // KPI相关
  excellent: '#52c41a',
  good: '#73d13d',
  average: '#faad14',
  poor: '#ff7a45',
  critical: '#ff4d4f',
};

// ============================================================================
// 辅助函数
// ============================================================================

/**
 * 生成唯一ID
 */
function generateId(): string {
  return `rule_${Date.now()}_${Math.random().toString(36).substring(2, 11)}`;
}

/**
 * 创建数值格式配置
 */
function createNumberFormat(options: Partial<NumberFormat>): DataFormat {
  return {
    type: 'number',
    config: {
      decimalPlaces: 2,
      useThousandsSeparator: true,
      ...options,
    },
  };
}

/**
 * 创建货币格式配置
 */
function createCurrencyFormat(currencyCode: string = 'CNY', decimalPlaces: number = 2): DataFormat {
  const prefixMap: Record<string, string> = {
    CNY: '¥',
    USD: '$',
    EUR: '€',
    GBP: '£',
    JPY: '¥',
  };
  
  return {
    type: 'number',
    config: {
      decimalPlaces,
      useThousandsSeparator: true,
      asCurrency: true,
      currencyCode,
      prefix: prefixMap[currencyCode] || '',
      negativeFormat: 'redParentheses',
    },
  };
}

/**
 * 创建百分比格式配置
 */
function createPercentageFormat(decimalPlaces: number = 1): DataFormat {
  return {
    type: 'number',
    config: {
      decimalPlaces,
      asPercentage: true,
      suffix: '%',
    },
  };
}

// ============================================================================
// 财务报表样式模板
// 需求: 21.3.10 - 数字右对齐、负数红色、千分位、货币符号
// ============================================================================

/**
 * 财务报表条件格式化规则
 */
const financeConditionalRules: ConditionalRule[] = [
  // 负数红色显示
  {
    id: generateId(),
    name: '负数红色',
    priority: 1,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'lt',
        value: 0,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.loss },
    },
  },
  // 正数绿色显示（利润）
  {
    id: generateId(),
    name: '正数绿色',
    priority: 2,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'gt',
        value: 0,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.profit },
    },
    scope: {
      columns: ['profit', 'net_profit', 'gross_profit', 'operating_profit'],
    },
  },
  // 零值灰色显示
  {
    id: generateId(),
    name: '零值灰色',
    priority: 3,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'eq',
        value: 0,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.neutral },
    },
  },
];

/**
 * 财务报表样式模板
 */
export const FINANCE_TEMPLATE: StyleTemplate = {
  id: 'finance-standard',
  name: '财务报表标准样式',
  category: 'finance',
  description: '适用于财务报表，数字右对齐、负数红色括号格式、千分位分隔、货币符号显示',
  isSystem: true,
  columnStyles: {
    // 金额类字段
    'amount|money|price|cost|revenue|profit|income|expense|balance': {
      fieldPattern: 'amount|money|price|cost|revenue|profit|income|expense|balance',
      fieldType: 'currency',
      defaultStyle: {
        font: { family: 'Arial', size: 12 },
        alignment: { horizontal: 'right' },
        format: createCurrencyFormat('CNY', 2),
      },
      headerStyle: {
        alignment: { horizontal: 'center' },
      },
      summaryStyle: {
        font: { weight: 'bold' },
        background: { type: 'solid', color: COLORS.lightGray },
      },
    },
    // 百分比类字段
    'rate|ratio|percent|margin': {
      fieldPattern: 'rate|ratio|percent|margin',
      fieldType: 'percentage',
      defaultStyle: {
        alignment: { horizontal: 'right' },
        format: createPercentageFormat(2),
      },
    },
    // 数量类字段
    'quantity|count|number|qty': {
      fieldPattern: 'quantity|count|number|qty',
      fieldType: 'number',
      defaultStyle: {
        alignment: { horizontal: 'right' },
        format: createNumberFormat({ decimalPlaces: 0 }),
      },
    },
  },
  conditionalRules: financeConditionalRules,
  tableStyle: {
    headerStyle: {
      font: { weight: 'bold', color: COLORS.white },
      background: { type: 'solid', color: COLORS.blue },
      alignment: { horizontal: 'center', vertical: 'middle' },
      padding: { top: 10, right: 12, bottom: 10, left: 12 },
    },
    bodyStyle: {
      font: { family: 'Arial', size: 12 },
      padding: { top: 8, right: 12, bottom: 8, left: 12 },
      border: { all: { style: 'solid', width: 1, color: COLORS.border } },
    },
    alternateRowStyle: {
      background: { type: 'solid', color: COLORS.lightGray },
    },
    summaryRowStyle: {
      font: { weight: 'bold' },
      background: { type: 'solid', color: '#f0f0f0' },
      border: { top: { style: 'double', width: 3, color: COLORS.border } },
    },
    borderStyle: 'all',
    borderColor: COLORS.border,
  },
};

// ============================================================================
// 销售报表样式模板
// 需求: 21.3.11 - 业绩达标绿色、未达标红色、排名高亮
// ============================================================================

/**
 * 销售报表条件格式化规则
 */
const salesConditionalRules: ConditionalRule[] = [
  // 业绩达标（>=100%）绿色
  {
    id: generateId(),
    name: '业绩达标',
    priority: 1,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'gte',
        value: 1.0,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.green, weight: 'bold' },
      background: { type: 'solid', color: COLORS.lightGreen },
    },
    scope: {
      columns: ['achievement_rate', 'completion_rate', 'target_rate'],
    },
  },
  // 业绩接近达标（80%-100%）黄色
  {
    id: generateId(),
    name: '业绩接近达标',
    priority: 2,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'between',
        value: 0.8,
        value2: 0.9999,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.orange },
      background: { type: 'solid', color: COLORS.lightYellow },
    },
    scope: {
      columns: ['achievement_rate', 'completion_rate', 'target_rate'],
    },
  },
  // 业绩未达标（<80%）红色
  {
    id: generateId(),
    name: '业绩未达标',
    priority: 3,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'lt',
        value: 0.8,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.red },
      background: { type: 'solid', color: COLORS.lightRed },
    },
    scope: {
      columns: ['achievement_rate', 'completion_rate', 'target_rate'],
    },
  },
  // 排名前三高亮
  {
    id: generateId(),
    name: '排名前三',
    priority: 4,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'lte',
        value: 3,
      } as ValueCondition,
    },
    style: {
      font: { weight: 'bold', color: COLORS.purple },
      background: { type: 'solid', color: COLORS.lightPurple },
    },
    scope: {
      columns: ['rank', 'ranking'],
    },
  },
  // 同比增长正数绿色
  {
    id: generateId(),
    name: '同比增长',
    priority: 5,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'gt',
        value: 0,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.green },
    },
    scope: {
      columns: ['yoy', 'yoy_growth', 'year_on_year'],
    },
  },
  // 同比下降负数红色
  {
    id: generateId(),
    name: '同比下降',
    priority: 6,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'lt',
        value: 0,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.red },
    },
    scope: {
      columns: ['yoy', 'yoy_growth', 'year_on_year'],
    },
  },
  // 环比增长正数绿色
  {
    id: generateId(),
    name: '环比增长',
    priority: 7,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'gt',
        value: 0,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.green },
    },
    scope: {
      columns: ['mom', 'mom_growth', 'month_on_month'],
    },
  },
  // 环比下降负数红色
  {
    id: generateId(),
    name: '环比下降',
    priority: 8,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'lt',
        value: 0,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.red },
    },
    scope: {
      columns: ['mom', 'mom_growth', 'month_on_month'],
    },
  },
];

/**
 * 销售报表样式模板
 */
export const SALES_TEMPLATE: StyleTemplate = {
  id: 'sales-dashboard',
  name: '销售报表样式',
  category: 'sales',
  description: '适用于销售报表，业绩达标绿色、未达标红色、排名高亮、同比环比箭头指示',
  isSystem: true,
  columnStyles: {
    // 达成率类字段
    'rate|ratio|achievement|completion|target': {
      fieldPattern: 'rate|ratio|achievement|completion|target',
      fieldType: 'percentage',
      defaultStyle: {
        alignment: { horizontal: 'center' },
        format: createPercentageFormat(1),
      },
    },
    // 金额类字段
    'amount|sales|revenue|target_amount': {
      fieldPattern: 'amount|sales|revenue|target_amount',
      fieldType: 'currency',
      defaultStyle: {
        alignment: { horizontal: 'right' },
        format: createCurrencyFormat('CNY', 0),
      },
    },
    // 排名字段
    'rank|ranking': {
      fieldPattern: 'rank|ranking',
      fieldType: 'number',
      defaultStyle: {
        alignment: { horizontal: 'center' },
        font: { weight: 'bold' },
      },
    },
    // 同比环比字段
    'yoy|mom|growth': {
      fieldPattern: 'yoy|mom|growth',
      fieldType: 'percentage',
      defaultStyle: {
        alignment: { horizontal: 'right' },
        format: createPercentageFormat(1),
      },
    },
  },
  conditionalRules: salesConditionalRules,
  tableStyle: {
    headerStyle: {
      font: { weight: 'bold', size: 13 },
      background: { type: 'solid', color: '#f0f5ff' },
      alignment: { horizontal: 'center', vertical: 'middle' },
      padding: { top: 12, right: 16, bottom: 12, left: 16 },
    },
    bodyStyle: {
      font: { family: 'Microsoft YaHei', size: 13 },
      padding: { top: 10, right: 16, bottom: 10, left: 16 },
    },
    alternateRowStyle: {
      background: { type: 'solid', color: COLORS.lightGray },
    },
    summaryRowStyle: {
      font: { weight: 'bold' },
      background: { type: 'solid', color: '#e6f7ff' },
    },
    borderStyle: 'horizontal',
    borderColor: COLORS.borderLight,
  },
};

// ============================================================================
// 库存报表样式模板
// 需求: 21.3.12 - 库存预警红色、安全库存绿色、周转率色阶
// ============================================================================

/**
 * 库存报表条件格式化规则
 */
const inventoryConditionalRules: ConditionalRule[] = [
  // 库存危险（低于安全库存50%）- 红色
  {
    id: generateId(),
    name: '库存危险',
    priority: 1,
    enabled: true,
    condition: {
      type: 'formula',
      config: {
        expression: '${stock} < ${safety_stock} * 0.5',
      } as FormulaCondition,
    },
    style: {
      font: { color: COLORS.white, weight: 'bold' },
      background: { type: 'solid', color: COLORS.danger },
    },
    scope: {
      columns: ['stock', 'current_stock', 'inventory'],
    },
  },
  // 库存预警（低于安全库存）- 黄色
  {
    id: generateId(),
    name: '库存预警',
    priority: 2,
    enabled: true,
    condition: {
      type: 'crossField',
      config: {
        field1: 'stock',
        operator: 'lt',
        field2: 'safety_stock',
        valueType: 'number',
      } as CrossFieldCondition,
    },
    style: {
      font: { color: COLORS.black },
      background: { type: 'solid', color: COLORS.warning },
    },
    scope: {
      columns: ['stock', 'current_stock', 'inventory'],
    },
  },
  // 库存正常（>=安全库存）- 绿色
  {
    id: generateId(),
    name: '库存正常',
    priority: 3,
    enabled: true,
    condition: {
      type: 'crossField',
      config: {
        field1: 'stock',
        operator: 'gte',
        field2: 'safety_stock',
        valueType: 'number',
      } as CrossFieldCondition,
    },
    style: {
      font: { color: COLORS.white },
      background: { type: 'solid', color: COLORS.safe },
    },
    scope: {
      columns: ['stock', 'current_stock', 'inventory'],
    },
  },
  // 周转率优秀（>12次/年）
  {
    id: generateId(),
    name: '周转率优秀',
    priority: 4,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'gt',
        value: 12,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.excellent },
      background: { type: 'solid', color: COLORS.lightGreen },
    },
    scope: {
      columns: ['turnover_rate', 'inventory_turnover'],
    },
  },
  // 周转率良好（6-12次/年）
  {
    id: generateId(),
    name: '周转率良好',
    priority: 5,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'between',
        value: 6,
        value2: 12,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.good },
    },
    scope: {
      columns: ['turnover_rate', 'inventory_turnover'],
    },
  },
  // 周转率一般（3-6次/年）
  {
    id: generateId(),
    name: '周转率一般',
    priority: 6,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'between',
        value: 3,
        value2: 5.99,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.average },
    },
    scope: {
      columns: ['turnover_rate', 'inventory_turnover'],
    },
  },
  // 周转率较差（<3次/年）
  {
    id: generateId(),
    name: '周转率较差',
    priority: 7,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'lt',
        value: 3,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.critical },
      background: { type: 'solid', color: COLORS.lightRed },
    },
    scope: {
      columns: ['turnover_rate', 'inventory_turnover'],
    },
  },
  // 库存天数过长（>90天）
  {
    id: generateId(),
    name: '库存天数过长',
    priority: 8,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'gt',
        value: 90,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.red },
      background: { type: 'solid', color: COLORS.lightRed },
    },
    scope: {
      columns: ['days_on_hand', 'inventory_days'],
    },
  },
];

/**
 * 库存报表样式模板
 */
export const INVENTORY_TEMPLATE: StyleTemplate = {
  id: 'inventory-alert',
  name: '库存报表样式',
  category: 'inventory',
  description: '适用于库存报表，库存预警红黄绿灯指示、周转率色阶显示',
  isSystem: true,
  columnStyles: {
    // 库存数量字段
    'stock|inventory|quantity|qty': {
      fieldPattern: 'stock|inventory|quantity|qty',
      fieldType: 'number',
      defaultStyle: {
        alignment: { horizontal: 'right' },
        format: createNumberFormat({ decimalPlaces: 0 }),
      },
    },
    // 安全库存字段
    'safety_stock|min_stock|reorder_point': {
      fieldPattern: 'safety_stock|min_stock|reorder_point',
      fieldType: 'number',
      defaultStyle: {
        alignment: { horizontal: 'right' },
        font: { color: COLORS.gray },
        format: createNumberFormat({ decimalPlaces: 0 }),
      },
    },
    // 周转率字段
    'turnover|turnover_rate': {
      fieldPattern: 'turnover|turnover_rate',
      fieldType: 'number',
      defaultStyle: {
        alignment: { horizontal: 'center' },
        format: createNumberFormat({ decimalPlaces: 1, suffix: '次' }),
      },
    },
    // 库存天数字段
    'days|days_on_hand|inventory_days': {
      fieldPattern: 'days|days_on_hand|inventory_days',
      fieldType: 'number',
      defaultStyle: {
        alignment: { horizontal: 'center' },
        format: createNumberFormat({ decimalPlaces: 0, suffix: '天' }),
      },
    },
    // 金额字段
    'value|amount|cost': {
      fieldPattern: 'value|amount|cost',
      fieldType: 'currency',
      defaultStyle: {
        alignment: { horizontal: 'right' },
        format: createCurrencyFormat('CNY', 2),
      },
    },
  },
  conditionalRules: inventoryConditionalRules,
  tableStyle: {
    headerStyle: {
      font: { weight: 'bold' },
      background: { type: 'solid', color: '#e6f7ff' },
      alignment: { horizontal: 'center', vertical: 'middle' },
      padding: { top: 10, right: 12, bottom: 10, left: 12 },
    },
    bodyStyle: {
      font: { family: 'Arial', size: 12 },
      alignment: { vertical: 'middle' },
      padding: { top: 8, right: 12, bottom: 8, left: 12 },
    },
    alternateRowStyle: {
      background: { type: 'solid', color: '#fafafa' },
    },
    summaryRowStyle: {
      font: { weight: 'bold' },
      background: { type: 'solid', color: '#f0f0f0' },
    },
    borderStyle: 'all',
    borderColor: COLORS.border,
  },
};

// ============================================================================
// KPI仪表盘样式模板
// 需求: 21.3.13 - 目标达成率色阶、趋势箭头、同比环比高亮
// ============================================================================

/**
 * KPI仪表盘条件格式化规则
 */
const kpiConditionalRules: ConditionalRule[] = [
  // 目标达成率 - 优秀（>=120%）
  {
    id: generateId(),
    name: '达成率优秀',
    priority: 1,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'gte',
        value: 1.2,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.white, weight: 'bold' },
      background: { type: 'solid', color: COLORS.excellent },
    },
    scope: {
      columns: ['achievement', 'completion', 'target_rate', 'kpi_rate'],
    },
  },
  // 目标达成率 - 良好（100%-120%）
  {
    id: generateId(),
    name: '达成率良好',
    priority: 2,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'between',
        value: 1.0,
        value2: 1.199,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.white },
      background: { type: 'solid', color: COLORS.good },
    },
    scope: {
      columns: ['achievement', 'completion', 'target_rate', 'kpi_rate'],
    },
  },
  // 目标达成率 - 一般（80%-100%）
  {
    id: generateId(),
    name: '达成率一般',
    priority: 3,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'between',
        value: 0.8,
        value2: 0.999,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.black },
      background: { type: 'solid', color: COLORS.average },
    },
    scope: {
      columns: ['achievement', 'completion', 'target_rate', 'kpi_rate'],
    },
  },
  // 目标达成率 - 较差（60%-80%）
  {
    id: generateId(),
    name: '达成率较差',
    priority: 4,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'between',
        value: 0.6,
        value2: 0.799,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.white },
      background: { type: 'solid', color: COLORS.poor },
    },
    scope: {
      columns: ['achievement', 'completion', 'target_rate', 'kpi_rate'],
    },
  },
  // 目标达成率 - 危险（<60%）
  {
    id: generateId(),
    name: '达成率危险',
    priority: 5,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'lt',
        value: 0.6,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.white, weight: 'bold' },
      background: { type: 'solid', color: COLORS.critical },
    },
    scope: {
      columns: ['achievement', 'completion', 'target_rate', 'kpi_rate'],
    },
  },
  // 同比增长 - 正增长绿色
  {
    id: generateId(),
    name: '同比正增长',
    priority: 6,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'gt',
        value: 0,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.green, weight: 'bold' },
      background: { type: 'solid', color: COLORS.lightGreen },
    },
    scope: {
      columns: ['yoy', 'yoy_growth', 'year_on_year', 'yoy_change'],
    },
  },
  // 同比下降 - 负增长红色
  {
    id: generateId(),
    name: '同比负增长',
    priority: 7,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'lt',
        value: 0,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.red, weight: 'bold' },
      background: { type: 'solid', color: COLORS.lightRed },
    },
    scope: {
      columns: ['yoy', 'yoy_growth', 'year_on_year', 'yoy_change'],
    },
  },
  // 环比增长 - 正增长绿色
  {
    id: generateId(),
    name: '环比正增长',
    priority: 8,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'gt',
        value: 0,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.green },
      background: { type: 'solid', color: COLORS.lightGreen },
    },
    scope: {
      columns: ['mom', 'mom_growth', 'month_on_month', 'mom_change'],
    },
  },
  // 环比下降 - 负增长红色
  {
    id: generateId(),
    name: '环比负增长',
    priority: 9,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator: 'lt',
        value: 0,
      } as ValueCondition,
    },
    style: {
      font: { color: COLORS.red },
      background: { type: 'solid', color: COLORS.lightRed },
    },
    scope: {
      columns: ['mom', 'mom_growth', 'month_on_month', 'mom_change'],
    },
  },
  // 趋势上升
  {
    id: generateId(),
    name: '趋势上升',
    priority: 10,
    enabled: true,
    condition: {
      type: 'text',
      config: {
        operator: 'equals',
        value: 'up',
        caseSensitive: false,
      },
    },
    style: {
      font: { color: COLORS.green },
    },
    scope: {
      columns: ['trend', 'direction'],
    },
  },
  // 趋势下降
  {
    id: generateId(),
    name: '趋势下降',
    priority: 11,
    enabled: true,
    condition: {
      type: 'text',
      config: {
        operator: 'equals',
        value: 'down',
        caseSensitive: false,
      },
    },
    style: {
      font: { color: COLORS.red },
    },
    scope: {
      columns: ['trend', 'direction'],
    },
  },
];

/**
 * KPI仪表盘样式模板
 */
export const KPI_TEMPLATE: StyleTemplate = {
  id: 'kpi-dashboard',
  name: 'KPI仪表盘样式',
  category: 'kpi',
  description: '适用于KPI仪表盘，目标达成率色阶、趋势箭头指示、同比环比高亮显示',
  isSystem: true,
  columnStyles: {
    // KPI名称字段
    'name|kpi_name|indicator': {
      fieldPattern: 'name|kpi_name|indicator',
      fieldType: 'text',
      defaultStyle: {
        font: { weight: 'bold', size: 14 },
        alignment: { horizontal: 'left' },
      },
    },
    // 达成率字段
    'achievement|completion|target_rate|kpi_rate': {
      fieldPattern: 'achievement|completion|target_rate|kpi_rate',
      fieldType: 'percentage',
      defaultStyle: {
        alignment: { horizontal: 'center' },
        font: { size: 16, weight: 'bold' },
        format: createPercentageFormat(1),
      },
    },
    // 实际值字段
    'actual|value|current': {
      fieldPattern: 'actual|value|current',
      fieldType: 'number',
      defaultStyle: {
        alignment: { horizontal: 'right' },
        font: { size: 14 },
        format: createNumberFormat({ decimalPlaces: 0, useThousandsSeparator: true }),
      },
    },
    // 目标值字段
    'target|goal|plan': {
      fieldPattern: 'target|goal|plan',
      fieldType: 'number',
      defaultStyle: {
        alignment: { horizontal: 'right' },
        font: { color: COLORS.gray },
        format: createNumberFormat({ decimalPlaces: 0, useThousandsSeparator: true }),
      },
    },
    // 同比字段
    'yoy|year_on_year': {
      fieldPattern: 'yoy|year_on_year',
      fieldType: 'percentage',
      defaultStyle: {
        alignment: { horizontal: 'center' },
        format: createPercentageFormat(1),
      },
    },
    // 环比字段
    'mom|month_on_month': {
      fieldPattern: 'mom|month_on_month',
      fieldType: 'percentage',
      defaultStyle: {
        alignment: { horizontal: 'center' },
        format: createPercentageFormat(1),
      },
    },
    // 趋势字段
    'trend|direction': {
      fieldPattern: 'trend|direction',
      fieldType: 'text',
      defaultStyle: {
        alignment: { horizontal: 'center' },
        font: { size: 16 },
      },
    },
  },
  conditionalRules: kpiConditionalRules,
  tableStyle: {
    headerStyle: {
      font: { weight: 'bold', size: 14, color: COLORS.white },
      background: { type: 'solid', color: COLORS.purple },
      alignment: { horizontal: 'center', vertical: 'middle' },
      padding: { top: 14, right: 16, bottom: 14, left: 16 },
    },
    bodyStyle: {
      font: { family: 'Arial', size: 13 },
      alignment: { horizontal: 'center', vertical: 'middle' },
      padding: { top: 12, right: 16, bottom: 12, left: 16 },
    },
    alternateRowStyle: {
      background: { type: 'solid', color: COLORS.lightPurple },
    },
    summaryRowStyle: {
      font: { weight: 'bold', size: 14 },
      background: { type: 'solid', color: '#f0e6ff' },
    },
    borderStyle: 'horizontal',
    borderColor: '#d9d9d9',
  },
};

// ============================================================================
// 模板注册表和工厂函数
// ============================================================================

/**
 * 预设模板注册表
 */
export const PRESET_TEMPLATES: Record<string, StyleTemplate> = {
  'finance-standard': FINANCE_TEMPLATE,
  'sales-dashboard': SALES_TEMPLATE,
  'inventory-alert': INVENTORY_TEMPLATE,
  'kpi-dashboard': KPI_TEMPLATE,
};

/**
 * 获取所有预设模板
 */
export function getAllPresetTemplates(): StyleTemplate[] {
  return Object.values(PRESET_TEMPLATES);
}

/**
 * 按分类获取预设模板
 */
export function getPresetTemplatesByCategory(category: TemplateCategory): StyleTemplate[] {
  return Object.values(PRESET_TEMPLATES).filter(t => t.category === category);
}

/**
 * 根据ID获取预设模板
 */
export function getPresetTemplateById(id: string): StyleTemplate | undefined {
  return PRESET_TEMPLATES[id];
}

/**
 * 将样式模板转换为StyleConfig
 */
export function templateToStyleConfig(template: StyleTemplate): StyleConfig {
  // 构建行样式配置
  const rowStyles: RowStyleConfig = {
    headerStyle: template.tableStyle.headerStyle,
    evenRowStyle: template.tableStyle.alternateRowStyle,
    summaryStyle: template.tableStyle.summaryRowStyle,
  };

  // 构建列样式映射
  const columnStyles: Record<string, CellStyle> = {};
  for (const [pattern, config] of Object.entries(template.columnStyles)) {
    // 使用模式作为键，实际应用时需要匹配字段名
    columnStyles[pattern] = config.defaultStyle;
  }

  return {
    globalStyle: template.tableStyle.bodyStyle,
    columnStyles,
    rowStyles,
    conditionalRules: template.conditionalRules,
  };
}

/**
 * 应用模板到字段
 * 根据字段名匹配模板中的列样式配置
 */
export function applyTemplateToField(
  template: StyleTemplate,
  fieldName: string
): CellStyle | undefined {
  for (const [, config] of Object.entries(template.columnStyles)) {
    if (config.fieldPattern) {
      const regex = new RegExp(config.fieldPattern, 'i');
      if (regex.test(fieldName)) {
        return config.defaultStyle;
      }
    }
  }
  return undefined;
}

/**
 * 获取字段的条件格式化规则
 * 根据字段名过滤适用的条件规则
 */
export function getFieldConditionalRules(
  template: StyleTemplate,
  fieldName: string
): ConditionalRule[] {
  return template.conditionalRules.filter(rule => {
    if (!rule.scope?.columns) {
      return true; // 无范围限制，适用于所有字段
    }
    // 检查字段名是否在规则范围内
    return rule.scope.columns.some(col => {
      const regex = new RegExp(col, 'i');
      return regex.test(fieldName);
    });
  });
}

// ============================================================================
// 模板工厂函数
// ============================================================================

/**
 * 创建自定义模板
 */
export function createCustomTemplate(
  name: string,
  description: string,
  baseTemplate?: StyleTemplate
): StyleTemplate {
  const base = baseTemplate || FINANCE_TEMPLATE;
  
  return {
    id: `custom_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`,
    name,
    category: 'custom',
    description,
    isSystem: false,
    columnStyles: { ...base.columnStyles },
    conditionalRules: [...base.conditionalRules],
    tableStyle: { ...base.tableStyle },
  };
}

/**
 * 克隆模板
 */
export function cloneTemplate(template: StyleTemplate, newName?: string): StyleTemplate {
  return {
    ...template,
    id: `clone_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`,
    name: newName || `${template.name} (副本)`,
    isSystem: false,
    columnStyles: JSON.parse(JSON.stringify(template.columnStyles)),
    conditionalRules: JSON.parse(JSON.stringify(template.conditionalRules)),
    tableStyle: JSON.parse(JSON.stringify(template.tableStyle)),
  };
}

/**
 * 合并两个模板
 * 后者的配置覆盖前者
 */
export function mergeTemplates(
  base: StyleTemplate,
  override: Partial<StyleTemplate>
): StyleTemplate {
  return {
    ...base,
    ...override,
    id: override.id || base.id,
    columnStyles: {
      ...base.columnStyles,
      ...(override.columnStyles || {}),
    },
    conditionalRules: [
      ...base.conditionalRules,
      ...(override.conditionalRules || []),
    ],
    tableStyle: {
      ...base.tableStyle,
      ...(override.tableStyle || {}),
    },
  };
}

/**
 * 验证模板配置
 */
export function validateTemplate(template: StyleTemplate): { valid: boolean; errors: string[] } {
  const errors: string[] = [];

  if (!template.id || template.id.trim() === '') {
    errors.push('模板ID不能为空');
  }

  if (!template.name || template.name.trim() === '') {
    errors.push('模板名称不能为空');
  }

  if (!template.category) {
    errors.push('模板分类不能为空');
  }

  if (!template.tableStyle) {
    errors.push('表格样式配置不能为空');
  }

  if (!template.tableStyle?.headerStyle) {
    errors.push('表头样式不能为空');
  }

  if (!template.tableStyle?.bodyStyle) {
    errors.push('数据行样式不能为空');
  }

  return {
    valid: errors.length === 0,
    errors,
  };
}

// 导出类型
export type { CellStyle, DataFormat, NumberFormat };

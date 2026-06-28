/**
 * 条件格式化类型定义（参考帆软条件属性）
 * Conditional formatting type definitions (Reference: FineReport conditional properties)
 * 
 * 需求: 21.2.5, 21.2.6, 21.2.7, 21.2.8, 21.2.9
 */

import type { CellStyle } from './style';

// 数值条件
// 需求: 21.2.5 - 数值比较条件（等于、不等于、大于、小于、区间）
export interface ValueCondition {
  operator: 'eq' | 'ne' | 'gt' | 'gte' | 'lt' | 'lte' | 'between' | 'notBetween';
  value?: number;
  value2?: number;             // 用于 between
  compareField?: string;       // 跨字段比较 (需求: 21.2.8)
}

// 文本条件
// 需求: 21.2.6 - 文本匹配条件（包含、开头、结尾、正则匹配）
export interface TextCondition {
  operator: 'contains' | 'notContains' | 'startsWith' | 'endsWith' | 'equals' | 'notEquals' | 'regex';
  value: string;
  caseSensitive?: boolean;
  compareField?: string;       // 跨字段比较 (需求: 21.2.8)
}

// 空值条件
// 需求: 21.2.7 - 空值判断条件（为空、不为空）
export interface NullCondition {
  operator: 'isEmpty' | 'isNotEmpty';
}

// 日期条件
export interface DateCondition {
  operator: 'before' | 'after' | 'between' | 'today' | 'thisWeek' | 'thisMonth' | 'thisYear';
  value?: string;
  value2?: string;
  compareField?: string;       // 跨字段比较 (需求: 21.2.8)
}

// 公式条件
// 需求: 21.2.9 - 自定义表达式条件（支持数学运算和函数）
export interface FormulaCondition {
  expression: string;          // 如：${value} > ${avgValue} * 1.2
  variables?: Record<string, string>;  // 变量映射
}

// 跨字段比较条件
// 需求: 21.2.8 - 跨字段比较条件（字段A > 字段B）
export interface CrossFieldCondition {
  field1: string;              // 第一个字段名
  operator: 'eq' | 'ne' | 'gt' | 'gte' | 'lt' | 'lte';
  field2: string;              // 第二个字段名
  valueType?: 'number' | 'string' | 'date' | 'auto';  // 比较值类型
}

// Top/Bottom 条件
export interface TopCondition {
  type: 'top' | 'bottom';
  count: number;
  isPercent?: boolean;         // 是否为百分比
}

// 规则条件类型
export type ConditionType = 'value' | 'text' | 'date' | 'formula' | 'top' | 'unique' | 'duplicate' | 'null' | 'crossField';

// 规则条件配置
export type ConditionConfig = ValueCondition | TextCondition | DateCondition | FormulaCondition | TopCondition | NullCondition | CrossFieldCondition;

// 规则条件
export interface RuleCondition {
  type: ConditionType;
  config: ConditionConfig;
}

// 规则应用范围
export interface RuleScope {
  columns?: string[];          // 应用到指定列
  rows?: 'all' | 'data' | 'summary';  // 应用到行类型
}

// 条件格式化规则
export interface ConditionalRule {
  id: string;
  name: string;
  priority: number;            // 优先级，数字越小优先级越高
  enabled: boolean;
  condition: RuleCondition;
  style: CellStyle;
  scope?: RuleScope;           // 应用范围
}

// 预设规则类型
export type PresetRuleType = 
  | 'negativeRed'           // 负数红色
  | 'positiveGreen'         // 正数绿色
  | 'zeroGray'              // 零值灰色
  | 'trafficLight'          // 红绿灯
  | 'dataBar'               // 数据条
  | 'colorScale'            // 色阶
  | 'iconSet'               // 图标集
  | 'duplicateHighlight'    // 重复值高亮
  | 'uniqueHighlight'       // 唯一值高亮
  | 'percentageThreshold';  // 百分比阈值

// 色阶配置
export interface ColorScaleConfig {
  minColor: string;
  midColor?: string;
  maxColor: string;
  minValue?: number;
  midValue?: number;
  maxValue?: number;
  minType?: 'min' | 'number' | 'percent' | 'percentile';
  midType?: 'number' | 'percent' | 'percentile';
  maxType?: 'max' | 'number' | 'percent' | 'percentile';
}

// 数据条配置
export interface DataBarConfig {
  positiveColor: string;
  negativeColor: string;
  showValue: boolean;
  minValue?: number;
  maxValue?: number;
  direction: 'leftToRight' | 'rightToLeft';
  borderColor?: string;
  fillType: 'solid' | 'gradient';
}

// 图标集配置
export interface IconSetConfig {
  iconType: 'arrows' | 'traffic' | 'stars' | 'flags' | 'ratings' | 'custom';
  reverseOrder: boolean;
  showIconOnly: boolean;
  thresholds: IconThreshold[];
}

// 图标阈值
export interface IconThreshold {
  icon: string;
  color: string;
  value: number;
  operator: 'gte' | 'gt' | 'lte' | 'lt';
  type: 'number' | 'percent' | 'percentile';
}

// 验证结果
export interface ValidationResult {
  valid: boolean;
  errors: ValidationError[];
}

// 验证错误
export interface ValidationError {
  field: string;
  message: string;
  code: string;
}

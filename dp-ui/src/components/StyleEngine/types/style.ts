/**
 * 样式类型定义（参考帆软单元格属性）
 * Style type definitions (Reference: FineReport cell properties)
 */

// 字体样式
export interface FontStyle {
  family?: string;           // 字体族：'Microsoft YaHei', 'Arial', etc.
  size?: number;             // 字号：12, 14, 16, etc.
  weight?: 'normal' | 'bold' | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900;
  style?: 'normal' | 'italic' | 'oblique';
  decoration?: 'none' | 'underline' | 'line-through' | 'underline line-through';
  color?: string;            // 颜色：'#FF0000', 'rgb(255,0,0)', etc.
}

// 对齐样式
export interface AlignmentStyle {
  horizontal?: 'left' | 'center' | 'right' | 'justify';
  vertical?: 'top' | 'middle' | 'bottom';
  textDirection?: 'ltr' | 'rtl';
  indent?: number;           // 缩进级别
  wrapText?: boolean;        // 自动换行
  shrinkToFit?: boolean;     // 缩小字体填充
  rotation?: number;         // 文字旋转角度 -90 to 90
}

// 边框侧
export interface BorderSide {
  style?: 'none' | 'solid' | 'dashed' | 'dotted' | 'double';
  width?: number;
  color?: string;
}

// 边框样式
export interface BorderStyle {
  top?: BorderSide;
  right?: BorderSide;
  bottom?: BorderSide;
  left?: BorderSide;
  all?: BorderSide;          // 快捷设置四边
}

// 渐变配置
export interface GradientConfig {
  type: 'linear' | 'radial';
  angle?: number;            // 线性渐变角度
  colors: { offset: number; color: string }[];
}

// 图案配置
export interface PatternConfig {
  type: 'stripe' | 'dots' | 'grid' | 'diagonal';
  foreground: string;
  background: string;
}

// 背景样式
export interface BackgroundStyle {
  type?: 'solid' | 'gradient' | 'pattern';
  color?: string;            // 纯色背景
  gradient?: GradientConfig; // 渐变背景
  pattern?: PatternConfig;   // 图案背景
}

// 内边距样式
export interface PaddingStyle {
  top?: number;
  right?: number;
  bottom?: number;
  left?: number;
}

// 数值格式
export interface NumberFormat {
  decimalPlaces?: number;      // 小数位数
  useThousandsSeparator?: boolean;  // 千分位分隔符
  negativeFormat?: 'minus' | 'parentheses' | 'red' | 'redParentheses';  // 负数格式
  prefix?: string;             // 前缀（如：¥、$）
  suffix?: string;             // 后缀（如：%、元）
  asPercentage?: boolean;      // 显示为百分比
  asCurrency?: boolean;        // 显示为货币
  currencyCode?: string;       // 货币代码：CNY, USD, EUR
  abbreviate?: boolean;        // 大数缩写：1000 -> 1K
  abbreviateThreshold?: number; // 缩写阈值
}

// 日期格式
export interface DateFormat {
  pattern?: string;            // 自定义格式：'YYYY-MM-DD HH:mm:ss'
  preset?: 'short' | 'medium' | 'long' | 'full' | 'relative';
  locale?: string;             // 本地化
}

// 文本格式
export interface TextFormat {
  case?: 'none' | 'upper' | 'lower' | 'capitalize' | 'titleCase';
  prefix?: string;
  suffix?: string;
  maxLength?: number;          // 最大显示长度
  overflow?: 'truncate' | 'ellipsis' | 'wrap' | 'tooltip';
  emptyText?: string;          // 空值显示文本
}

// 自定义格式
export interface CustomFormat {
  expression: string;          // 自定义格式表达式
}

// 数据格式
export interface DataFormat {
  type: 'number' | 'date' | 'text' | 'custom';
  config: NumberFormat | DateFormat | TextFormat | CustomFormat;
}

// 单元格样式
export interface CellStyle {
  font?: FontStyle;
  alignment?: AlignmentStyle;
  border?: BorderStyle;
  background?: BackgroundStyle;
  padding?: PaddingStyle;
  format?: DataFormat;
}

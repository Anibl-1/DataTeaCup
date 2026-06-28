/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 样式规则引擎
 * Style Rule Engine
 * 
 * 实现功能：
 * - 样式继承机制（全局样式 → 列样式 → 行样式 → 单元格样式）
 * - 样式合并算法（后定义的样式覆盖先定义的样式）
 * - 与条件格式化集成
 * - 大数据量性能优化
 * 
 * 需求: 21.1.1, 21.1.2
 */

import type {
  CellStyle,
  FontStyle,
  AlignmentStyle,
  BorderStyle,
  BorderSide,
  BackgroundStyle,
  PaddingStyle,
  DataFormat,
} from './types/style';
import type { ConditionalRule } from './types/conditional';
import { evaluateStyle as evaluateConditionalStyle } from './ConditionalFormat';

/**
 * 样式配置接口
 * Style configuration interface
 */
export interface StyleConfig {
  /** 全局样式 */
  globalStyle?: CellStyle;
  /** 列样式映射 (columnKey -> style) */
  columnStyles?: Record<string, CellStyle>;
  /** 行样式配置 */
  rowStyles?: RowStyleConfig;
  /** 单元格样式映射 (rowIndex:columnKey -> style) */
  cellStyles?: Record<string, CellStyle>;
  /** 条件格式化规则 */
  conditionalRules?: ConditionalRule[];
}

/**
 * 行样式配置
 */
export interface RowStyleConfig {
  /** 偶数行样式（斑马纹） */
  evenRowStyle?: CellStyle;
  /** 奇数行样式 */
  oddRowStyle?: CellStyle;
  /** 表头行样式 */
  headerStyle?: CellStyle;
  /** 汇总行样式 */
  summaryStyle?: CellStyle;
  /** 特定行样式 (rowIndex -> style) */
  specificRows?: Record<number, CellStyle>;
}

/**
 * 计算单元格样式的参数
 */
export interface ComputeCellStyleParams {
  rowIndex: number;
  columnKey: string;
  value: any;
  rowData: Record<string, any>;
  rowType?: 'header' | 'data' | 'summary';
  allColumnValues?: any[];
}

/**
 * 样式引擎配置选项
 */
export interface StyleEngineOptions {
  /** 是否启用斑马纹 */
  enableZebraStripe?: boolean;
  /** 是否启用条件格式化 */
  enableConditionalFormat?: boolean;
  /** 样式缓存大小 */
  cacheSize?: number;
}

/**
 * 深度合并两个对象
 * Deep merge two objects with later values overriding earlier ones
 */
function deepMerge<T extends Record<string, any>>(target: T, source: Partial<T> | undefined): T {
  if (!source) return target;
  
  const result = { ...target };
  
  for (const key in source) {
    if (source[key] === undefined) continue;
    
    const sourceValue = source[key];
    const targetValue = result[key];
    
    if (
      typeof sourceValue === 'object' &&
      sourceValue !== null &&
      !Array.isArray(sourceValue) &&
      typeof targetValue === 'object' &&
      targetValue !== null &&
      !Array.isArray(targetValue)
    ) {
      result[key] = deepMerge(targetValue, sourceValue as any);
    } else {
      result[key] = sourceValue as any;
    }
  }
  
  return result;
}

/**
 * 合并字体样式
 */
function mergeFontStyle(base: FontStyle | undefined, override: FontStyle | undefined): FontStyle | undefined {
  if (!base && !override) return undefined;
  if (!base) return override;
  if (!override) return base;
  return deepMerge(base, override);
}

/**
 * 合并对齐样式
 */
function mergeAlignmentStyle(base: AlignmentStyle | undefined, override: AlignmentStyle | undefined): AlignmentStyle | undefined {
  if (!base && !override) return undefined;
  if (!base) return override;
  if (!override) return base;
  return deepMerge(base, override);
}

/**
 * 合并边框侧样式
 */
function mergeBorderSide(base: BorderSide | undefined, override: BorderSide | undefined): BorderSide | undefined {
  if (!base && !override) return undefined;
  if (!base) return override;
  if (!override) return base;
  return deepMerge(base, override);
}

/**
 * 合并边框样式
 */
function mergeBorderStyle(base: BorderStyle | undefined, override: BorderStyle | undefined): BorderStyle | undefined {
  if (!base && !override) return undefined;
  if (!base) return override;
  if (!override) return base;
  
  // 处理 all 快捷属性
  const baseExpanded: BorderStyle = base.all 
    ? { top: base.all, right: base.all, bottom: base.all, left: base.all, ...base }
    : base;
  const overrideExpanded: BorderStyle = override.all
    ? { top: override.all, right: override.all, bottom: override.all, left: override.all, ...override }
    : override;
  
  return {
    top: mergeBorderSide(baseExpanded.top, overrideExpanded.top),
    right: mergeBorderSide(baseExpanded.right, overrideExpanded.right),
    bottom: mergeBorderSide(baseExpanded.bottom, overrideExpanded.bottom),
    left: mergeBorderSide(baseExpanded.left, overrideExpanded.left),
  };
}

/**
 * 合并背景样式
 */
function mergeBackgroundStyle(base: BackgroundStyle | undefined, override: BackgroundStyle | undefined): BackgroundStyle | undefined {
  if (!base && !override) return undefined;
  if (!base) return override;
  if (!override) return base;
  
  // 背景样式完全覆盖（不做深度合并）
  return { ...base, ...override };
}

/**
 * 合并内边距样式
 */
function mergePaddingStyle(base: PaddingStyle | undefined, override: PaddingStyle | undefined): PaddingStyle | undefined {
  if (!base && !override) return undefined;
  if (!base) return override;
  if (!override) return base;
  return deepMerge(base, override);
}

/**
 * 合并数据格式
 */
function mergeDataFormat(base: DataFormat | undefined, override: DataFormat | undefined): DataFormat | undefined {
  if (!base && !override) return undefined;
  if (!base) return override;
  if (!override) return base;
  
  // 如果类型不同，完全使用覆盖值
  if (base.type !== override.type) {
    return override;
  }
  
  // 同类型时合并配置
  return {
    type: override.type,
    config: deepMerge(base.config as any, override.config as any),
  };
}

/**
 * 合并多个样式对象
 * Merges multiple style objects with later styles overriding earlier ones
 * 
 * @param styles - 样式对象数组，按优先级从低到高排列
 * @returns 合并后的样式对象
 * 
 * 需求: 21.1.2 - 样式合并算法，后定义的样式覆盖先定义的样式
 */
export function mergeStyles(...styles: (CellStyle | undefined)[]): CellStyle {
  const validStyles = styles.filter((s): s is CellStyle => s !== undefined && s !== null);
  
  if (validStyles.length === 0) return {};
  if (validStyles.length === 1) return { ...validStyles[0] };
  
  let result: CellStyle = {};
  
  for (const style of validStyles) {
    result = {
      font: mergeFontStyle(result.font, style.font),
      alignment: mergeAlignmentStyle(result.alignment, style.alignment),
      border: mergeBorderStyle(result.border, style.border),
      background: mergeBackgroundStyle(result.background, style.background),
      padding: mergePaddingStyle(result.padding, style.padding),
      format: mergeDataFormat(result.format, style.format),
    };
  }
  
  // 清理 undefined 属性
  return cleanUndefinedProps(result);
}

/**
 * 清理对象中的 undefined 属性
 */
function cleanUndefinedProps<T extends Record<string, any>>(obj: T): T {
  const result: any = {};
  for (const key in obj) {
    if (obj[key] !== undefined) {
      result[key] = obj[key];
    }
  }
  return result;
}

/**
 * 应用样式继承机制
 * Applies style inheritance: global → column → row → cell
 * 
 * @param globalStyle - 全局样式
 * @param columnStyle - 列样式
 * @param rowStyle - 行样式
 * @param cellStyle - 单元格样式
 * @returns 继承合并后的最终样式
 * 
 * 需求: 21.1.1 - 样式继承机制（全局样式 → 列样式 → 行样式 → 单元格样式）
 */
export function applyStyleInheritance(
  globalStyle?: CellStyle,
  columnStyle?: CellStyle,
  rowStyle?: CellStyle,
  cellStyle?: CellStyle
): CellStyle {
  return mergeStyles(globalStyle, columnStyle, rowStyle, cellStyle);
}

/**
 * 生成单元格样式缓存键
 */
function generateCacheKey(rowIndex: number, columnKey: string): string {
  return `${rowIndex}:${columnKey}`;
}

/**
 * 样式规则引擎类
 * Style Rule Engine Class
 */
export class StyleEngine {
  private config: StyleConfig;
  private options: StyleEngineOptions;
  private styleCache: Map<string, CellStyle>;
  private maxCacheSize: number;

  constructor(config: StyleConfig = {}, options: StyleEngineOptions = {}) {
    this.config = config;
    this.options = {
      enableZebraStripe: true,
      enableConditionalFormat: true,
      cacheSize: 10000,
      ...options,
    };
    this.styleCache = new Map();
    this.maxCacheSize = this.options.cacheSize || 10000;
  }

  /**
   * 更新样式配置
   */
  setConfig(config: StyleConfig): void {
    this.config = config;
    this.clearCache();
  }

  /**
   * 更新部分配置
   */
  updateConfig(partialConfig: Partial<StyleConfig>): void {
    this.config = { ...this.config, ...partialConfig };
    this.clearCache();
  }

  /**
   * 设置全局样式
   */
  setGlobalStyle(style: CellStyle): void {
    this.config.globalStyle = style;
    this.clearCache();
  }

  /**
   * 设置列样式
   */
  setColumnStyle(columnKey: string, style: CellStyle): void {
    if (!this.config.columnStyles) {
      this.config.columnStyles = {};
    }
    this.config.columnStyles[columnKey] = style;
    this.clearCache();
  }

  /**
   * 批量设置列样式
   */
  setColumnStyles(styles: Record<string, CellStyle>): void {
    this.config.columnStyles = { ...this.config.columnStyles, ...styles };
    this.clearCache();
  }

  /**
   * 设置行样式配置
   */
  setRowStyles(rowStyles: RowStyleConfig): void {
    this.config.rowStyles = rowStyles;
    this.clearCache();
  }

  /**
   * 设置单元格样式
   */
  setCellStyle(rowIndex: number, columnKey: string, style: CellStyle): void {
    if (!this.config.cellStyles) {
      this.config.cellStyles = {};
    }
    const key = generateCacheKey(rowIndex, columnKey);
    this.config.cellStyles[key] = style;
    this.styleCache.delete(key);
  }

  /**
   * 设置条件格式化规则
   */
  setConditionalRules(rules: ConditionalRule[]): void {
    this.config.conditionalRules = rules;
    this.clearCache();
  }

  /**
   * 添加条件格式化规则
   */
  addConditionalRule(rule: ConditionalRule): void {
    if (!this.config.conditionalRules) {
      this.config.conditionalRules = [];
    }
    this.config.conditionalRules.push(rule);
    this.clearCache();
  }

  /**
   * 获取行样式
   */
  private getRowStyle(rowIndex: number, rowType: 'header' | 'data' | 'summary' = 'data'): CellStyle | undefined {
    const rowStyles = this.config.rowStyles;
    if (!rowStyles) return undefined;

    // 1. 检查特定行样式
    if (rowStyles.specificRows && rowStyles.specificRows[rowIndex]) {
      return rowStyles.specificRows[rowIndex];
    }

    // 2. 检查行类型样式
    switch (rowType) {
      case 'header':
        return rowStyles.headerStyle;
      case 'summary':
        return rowStyles.summaryStyle;
      case 'data':
      default:
        // 3. 斑马纹样式
        if (this.options.enableZebraStripe) {
          return rowIndex % 2 === 0 ? rowStyles.evenRowStyle : rowStyles.oddRowStyle;
        }
        return undefined;
    }
  }

  /**
   * 计算单元格最终样式
   * Computes the final style for a cell by applying inheritance and conditional formatting
   * 
   * @param params - 计算参数
   * @returns 计算后的最终样式
   */
  computeCellStyle(params: ComputeCellStyleParams): CellStyle {
    const { rowIndex, columnKey, value, rowData, rowType = 'data', allColumnValues = [] } = params;
    
    // 检查缓存（仅对非条件格式化场景有效）
    const cacheKey = generateCacheKey(rowIndex, columnKey);
    if (!this.options.enableConditionalFormat && this.styleCache.has(cacheKey)) {
      return this.styleCache.get(cacheKey)!;
    }

    // 1. 获取各层级样式
    const globalStyle = this.config.globalStyle;
    const columnStyle = this.config.columnStyles?.[columnKey];
    const rowStyle = this.getRowStyle(rowIndex, rowType);
    const cellStyleKey = generateCacheKey(rowIndex, columnKey);
    const cellStyle = this.config.cellStyles?.[cellStyleKey];

    // 2. 应用样式继承
    let computedStyle = applyStyleInheritance(globalStyle, columnStyle, rowStyle, cellStyle);

    // 3. 应用条件格式化
    if (this.options.enableConditionalFormat && this.config.conditionalRules?.length) {
      const conditionalStyle = evaluateConditionalStyle(
        value,
        rowData,
        this.config.conditionalRules,
        columnKey,
        rowType === 'summary' ? 'summary' : 'data',
        allColumnValues
      );
      computedStyle = mergeStyles(computedStyle, conditionalStyle);
    }

    // 4. 缓存结果（仅对非条件格式化场景）
    if (!this.options.enableConditionalFormat) {
      this.cacheStyle(cacheKey, computedStyle);
    }

    return computedStyle;
  }

  /**
   * 批量计算多个单元格样式
   * Batch compute styles for multiple cells (optimized for large datasets)
   */
  computeCellStyles(
    cells: Array<{ rowIndex: number; columnKey: string; value: any; rowData: Record<string, any> }>,
    rowType: 'header' | 'data' | 'summary' = 'data'
  ): Map<string, CellStyle> {
    const results = new Map<string, CellStyle>();
    
    // 预计算列值（用于条件格式化的 top/bottom/unique/duplicate）
    const columnValues: Record<string, any[]> = {};
    if (this.options.enableConditionalFormat) {
      for (const cell of cells) {
        if (!columnValues[cell.columnKey]) {
          columnValues[cell.columnKey] = [];
        }
        columnValues[cell.columnKey].push(cell.value);
      }
    }

    for (const cell of cells) {
      const style = this.computeCellStyle({
        ...cell,
        rowType,
        allColumnValues: columnValues[cell.columnKey] || [],
      });
      results.set(generateCacheKey(cell.rowIndex, cell.columnKey), style);
    }

    return results;
  }

  /**
   * 获取有效样式配置
   * Returns the effective style configuration
   */
  getEffectiveConfig(): StyleConfig {
    return { ...this.config };
  }

  /**
   * 清除样式缓存
   */
  clearCache(): void {
    this.styleCache.clear();
  }

  /**
   * 缓存样式（带 LRU 淘汰）
   */
  private cacheStyle(key: string, style: CellStyle): void {
    if (this.styleCache.size >= this.maxCacheSize) {
      // 简单的 FIFO 淘汰策略
      const firstKey = this.styleCache.keys().next().value;
      if (firstKey) {
        this.styleCache.delete(firstKey);
      }
    }
    this.styleCache.set(key, style);
  }

  /**
   * 获取缓存统计
   */
  getCacheStats(): { size: number; maxSize: number } {
    return {
      size: this.styleCache.size,
      maxSize: this.maxCacheSize,
    };
  }
}

/**
 * 创建样式引擎实例
 */
export function createStyleEngine(config?: StyleConfig, options?: StyleEngineOptions): StyleEngine {
  return new StyleEngine(config, options);
}

/**
 * 将 CellStyle 转换为 CSS 样式对象
 * Converts CellStyle to CSS style object for rendering
 */
export function cellStyleToCss(style: CellStyle): Record<string, string> {
  const css: Record<string, string> = {};

  // 字体样式
  if (style.font) {
    if (style.font.family) css.fontFamily = style.font.family;
    if (style.font.size) css.fontSize = `${style.font.size}px`;
    if (style.font.weight) css.fontWeight = String(style.font.weight);
    if (style.font.style) css.fontStyle = style.font.style;
    if (style.font.decoration) css.textDecoration = style.font.decoration;
    if (style.font.color) css.color = style.font.color;
  }

  // 对齐样式
  if (style.alignment) {
    if (style.alignment.horizontal) css.textAlign = style.alignment.horizontal;
    if (style.alignment.vertical) css.verticalAlign = style.alignment.vertical;
    if (style.alignment.textDirection) css.direction = style.alignment.textDirection;
    if (style.alignment.indent) css.paddingLeft = `${style.alignment.indent * 16}px`;
    if (style.alignment.wrapText) css.whiteSpace = 'pre-wrap';
    if (style.alignment.rotation) css.transform = `rotate(${style.alignment.rotation}deg)`;
  }

  // 边框样式
  if (style.border) {
    const formatBorder = (side: BorderSide | undefined): string => {
      if (!side || side.style === 'none') return 'none';
      return `${side.width || 1}px ${side.style || 'solid'} ${side.color || '#000'}`;
    };
    if (style.border.top) css.borderTop = formatBorder(style.border.top);
    if (style.border.right) css.borderRight = formatBorder(style.border.right);
    if (style.border.bottom) css.borderBottom = formatBorder(style.border.bottom);
    if (style.border.left) css.borderLeft = formatBorder(style.border.left);
  }

  // 背景样式
  if (style.background) {
    if (style.background.type === 'solid' && style.background.color) {
      css.backgroundColor = style.background.color;
    } else if (style.background.type === 'gradient' && style.background.gradient) {
      const { gradient } = style.background;
      const colorStops = gradient.colors
        .map(c => `${c.color} ${c.offset * 100}%`)
        .join(', ');
      if (gradient.type === 'linear') {
        css.background = `linear-gradient(${gradient.angle || 0}deg, ${colorStops})`;
      } else {
        css.background = `radial-gradient(circle, ${colorStops})`;
      }
    }
  }

  // 内边距
  if (style.padding) {
    const { top = 0, right = 0, bottom = 0, left = 0 } = style.padding;
    css.padding = `${top}px ${right}px ${bottom}px ${left}px`;
  }

  return css;
}

/**
 * 预设样式模板（简化版，用于快速应用）
 * 完整版模板请使用 presetTemplates.ts 中的模板
 */
export const PRESET_STYLE_TEMPLATES = {
  /** 财务报表样式 */
  finance: {
    globalStyle: {
      font: { family: 'Arial', size: 12 },
      alignment: { horizontal: 'right' as const },
      border: {
        all: { style: 'solid' as const, width: 1, color: '#e8e8e8' },
      },
    },
    rowStyles: {
      headerStyle: {
        font: { weight: 'bold' as const, color: '#ffffff' },
        background: { type: 'solid' as const, color: '#1890ff' },
        alignment: { horizontal: 'center' as const },
      },
      evenRowStyle: {
        background: { type: 'solid' as const, color: '#fafafa' },
      },
      summaryStyle: {
        font: { weight: 'bold' as const },
        background: { type: 'solid' as const, color: '#f0f0f0' },
      },
    },
  },

  /** 销售报表样式 */
  sales: {
    globalStyle: {
      font: { family: 'Microsoft YaHei', size: 13 },
      padding: { top: 8, right: 12, bottom: 8, left: 12 },
    },
    rowStyles: {
      headerStyle: {
        font: { weight: 'bold' as const },
        background: { type: 'solid' as const, color: '#f0f5ff' },
      },
      evenRowStyle: {
        background: { type: 'solid' as const, color: '#fafafa' },
      },
    },
  },

  /** 库存报表样式 */
  inventory: {
    globalStyle: {
      font: { family: 'Arial', size: 12 },
      alignment: { vertical: 'middle' as const },
    },
    rowStyles: {
      headerStyle: {
        font: { weight: 'bold' as const },
        background: { type: 'solid' as const, color: '#e6f7ff' },
      },
    },
  },

  /** KPI 仪表盘样式 */
  kpi: {
    globalStyle: {
      font: { family: 'Arial', size: 14 },
      alignment: { horizontal: 'center' as const, vertical: 'middle' as const },
    },
    rowStyles: {
      headerStyle: {
        font: { weight: 'bold' as const, size: 16, color: '#ffffff' },
        background: { type: 'solid' as const, color: '#722ed1' },
      },
    },
  },
} as const;

/**
 * 获取预设样式模板（简化版）
 */
export function getPresetStyleTemplate(
  templateName: keyof typeof PRESET_STYLE_TEMPLATES
): StyleConfig {
  return PRESET_STYLE_TEMPLATES[templateName] as StyleConfig;
}

// 重新导出完整版预设模板
export {
  FINANCE_TEMPLATE,
  SALES_TEMPLATE,
  INVENTORY_TEMPLATE,
  KPI_TEMPLATE,
  PRESET_TEMPLATES,
  getAllPresetTemplates,
  getPresetTemplatesByCategory,
  getPresetTemplateById,
  templateToStyleConfig,
  applyTemplateToField,
  getFieldConditionalRules,
  createCustomTemplate,
  cloneTemplate,
  mergeTemplates,
  validateTemplate,
} from './presetTemplates';

export type {
  StyleTemplate,
  TemplateCategory,
  ColumnStyleConfig,
  TableStyleConfig,
} from './presetTemplates';

// 导出类型
export type { CellStyle, FontStyle, AlignmentStyle, BorderStyle, BackgroundStyle, PaddingStyle, DataFormat };

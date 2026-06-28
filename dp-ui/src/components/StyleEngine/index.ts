/**
 * StyleEngine 模块导出
 * StyleEngine Module Exports
 */

// 类型导出
export * from './types';

// 组件导出
export { default as CellStyleEditor } from './CellStyleEditor.vue';

// 样式规则引擎导出
export {
  // 核心函数
  mergeStyles,
  applyStyleInheritance,
  cellStyleToCss,
  // 引擎类
  StyleEngine,
  createStyleEngine,
  // 预设模板（简化版）
  PRESET_STYLE_TEMPLATES,
  getPresetStyleTemplate,
  // 预设模板（完整版）- 需求: 21.3.10, 21.3.11, 21.3.12, 21.3.13
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
  // 类型
  type StyleConfig,
  type RowStyleConfig,
  type ComputeCellStyleParams,
  type StyleEngineOptions,
  type StyleTemplate,
  type TemplateCategory,
  type ColumnStyleConfig,
  type TableStyleConfig,
} from './StyleEngine';

// 条件格式化引擎导出
export {
  // 评估函数
  evaluateValueCondition,
  evaluateTextCondition,
  evaluateDateCondition,
  evaluateFormulaCondition,
  evaluateTopCondition,
  evaluateUniqueCondition,
  evaluateDuplicateCondition,
  evaluateCondition,
  evaluateStyle,
  // 样式操作
  mergeStyles as mergeConditionalStyles,
  sortRulesByPriority,
  // 验证
  validateRule,
  // 预设规则
  getPresetRules,
  PRESET_COLORS,
  // 规则创建工具
  createValueRangeRule,
  createPercentageThresholdRule,
  createColorScaleRules,
  // 服务类
  ConditionalFormatService,
  createConditionalFormatService,
} from './ConditionalFormat';

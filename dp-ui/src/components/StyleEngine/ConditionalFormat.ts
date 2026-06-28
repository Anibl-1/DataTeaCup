/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 条件格式化引擎
 * Conditional Formatting Engine
 * 
 * 支持功能：
 * - 数值范围条件（负数红色、正数绿色、区间色阶）
 * - 百分比阈值条件
 * - 多条件组合和优先级排序
 * - 预设规则模板
 * - 空值判断条件（为空、非空）
 * - 跨字段比较条件（字段A > 字段B）
 * - 自定义表达式条件（支持数学运算和函数）
 * 
 * 需求: 14.1.1, 14.1.2, 14.1.3, 21.2.5, 21.2.6, 21.2.7, 21.2.8, 21.2.9
 */

import type {
  CellStyle,
  ConditionalRule,
  RuleCondition,
  ValueCondition,
  TextCondition,
  DateCondition,
  FormulaCondition,
  TopCondition,
  NullCondition,
  CrossFieldCondition,
  PresetRuleType,
  ValidationResult,
  ValidationError,
  ColorScaleConfig,
} from './types';

/**
 * 生成唯一ID
 */
function generateId(): string {
  return `rule_${Date.now()}_${Math.random().toString(36).substring(2, 11)}`;
}

/**
 * 深度合并对象
 */
function deepMerge<T extends Record<string, any>>(target: T, source: Partial<T>): T {
  const result = { ...target };
  for (const key in source) {
    if (source[key] !== undefined) {
      if (
        typeof source[key] === 'object' &&
        source[key] !== null &&
        !Array.isArray(source[key]) &&
        typeof result[key] === 'object' &&
        result[key] !== null
      ) {
        result[key] = deepMerge(result[key], source[key] as any);
      } else {
        result[key] = source[key] as any;
      }
    }
  }
  return result;
}


/**
 * 评估数值条件
 * Evaluates a value condition against a given value
 */
export function evaluateValueCondition(
  value: any,
  condition: ValueCondition,
  rowData?: Record<string, any>
): boolean {
  // 处理空值
  if (value === null || value === undefined) {
    return false;
  }

  const numValue = typeof value === 'number' ? value : parseFloat(value);
  if (isNaN(numValue)) {
    return false;
  }

  // 跨字段比较
  let compareValue = condition.value;
  if (condition.compareField && rowData) {
    const fieldValue = rowData[condition.compareField];
    compareValue = typeof fieldValue === 'number' ? fieldValue : parseFloat(fieldValue);
    if (isNaN(compareValue as number)) {
      return false;
    }
  }

  switch (condition.operator) {
    case 'eq':
      return numValue === compareValue;
    case 'ne':
      return numValue !== compareValue;
    case 'gt':
      return compareValue !== undefined && numValue > compareValue;
    case 'gte':
      return compareValue !== undefined && numValue >= compareValue;
    case 'lt':
      return compareValue !== undefined && numValue < compareValue;
    case 'lte':
      return compareValue !== undefined && numValue <= compareValue;
    case 'between':
      return (
        condition.value !== undefined &&
        condition.value2 !== undefined &&
        numValue >= condition.value &&
        numValue <= condition.value2
      );
    case 'notBetween':
      return (
        condition.value !== undefined &&
        condition.value2 !== undefined &&
        (numValue < condition.value || numValue > condition.value2)
      );
    default:
      return false;
  }
}


/**
 * 评估文本条件
 * Evaluates a text condition against a given value
 * 
 * 需求: 21.2.6 - 文本匹配条件（包含、开头、结尾、正则匹配）
 * 需求: 21.2.8 - 跨字段比较条件
 */
export function evaluateTextCondition(
  value: any,
  condition: TextCondition,
  rowData?: Record<string, any>
): boolean {
  if (value === null || value === undefined) {
    return false;
  }

  const strValue = String(value);
  
  // 跨字段比较 (需求: 21.2.8)
  let compareValue = condition.value;
  if (condition.compareField && rowData) {
    const fieldValue = rowData[condition.compareField];
    if (fieldValue === null || fieldValue === undefined) {
      return false;
    }
    compareValue = String(fieldValue);
  }
  
  const normalizedCompareValue = condition.caseSensitive
    ? compareValue
    : compareValue.toLowerCase();
  const testValue = condition.caseSensitive ? strValue : strValue.toLowerCase();

  switch (condition.operator) {
    case 'contains':
      return testValue.includes(normalizedCompareValue);
    case 'notContains':
      return !testValue.includes(normalizedCompareValue);
    case 'startsWith':
      return testValue.startsWith(normalizedCompareValue);
    case 'endsWith':
      return testValue.endsWith(normalizedCompareValue);
    case 'equals':
      return testValue === normalizedCompareValue;
    case 'notEquals':
      return testValue !== normalizedCompareValue;
    case 'regex':
      try {
        const regex = new RegExp(condition.value, condition.caseSensitive ? '' : 'i');
        return regex.test(strValue);
      } catch {
        return false;
      }
    default:
      return false;
  }
}

/**
 * 评估日期条件
 * Evaluates a date condition against a given value
 * 
 * 需求: 21.2.8 - 跨字段比较条件
 */
export function evaluateDateCondition(
  value: any,
  condition: DateCondition,
  rowData?: Record<string, any>
): boolean {
  if (value === null || value === undefined) {
    return false;
  }

  const dateValue = value instanceof Date ? value : new Date(value);
  if (isNaN(dateValue.getTime())) {
    return false;
  }

  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());

  // 跨字段比较 (需求: 21.2.8)
  if (condition.compareField && rowData) {
    const fieldValue = rowData[condition.compareField];
    if (fieldValue === null || fieldValue === undefined) {
      return false;
    }
    const compareDate = fieldValue instanceof Date ? fieldValue : new Date(fieldValue);
    if (isNaN(compareDate.getTime())) {
      return false;
    }
    
    switch (condition.operator) {
      case 'before':
        return dateValue < compareDate;
      case 'after':
        return dateValue > compareDate;
      default:
        return false;
    }
  }

  switch (condition.operator) {
    case 'before':
      if (!condition.value) return false;
      return dateValue < new Date(condition.value);
    case 'after':
      if (!condition.value) return false;
      return dateValue > new Date(condition.value);
    case 'between':
      if (!condition.value || !condition.value2) return false;
      return dateValue >= new Date(condition.value) && dateValue <= new Date(condition.value2);
    case 'today':
      return (
        dateValue.getFullYear() === today.getFullYear() &&
        dateValue.getMonth() === today.getMonth() &&
        dateValue.getDate() === today.getDate()
      );
    case 'thisWeek': {
      const weekStart = new Date(today);
      weekStart.setDate(today.getDate() - today.getDay());
      const weekEnd = new Date(weekStart);
      weekEnd.setDate(weekStart.getDate() + 6);
      return dateValue >= weekStart && dateValue <= weekEnd;
    }
    case 'thisMonth':
      return (
        dateValue.getFullYear() === today.getFullYear() &&
        dateValue.getMonth() === today.getMonth()
      );
    case 'thisYear':
      return dateValue.getFullYear() === today.getFullYear();
    default:
      return false;
  }
}

/**
 * 评估空值条件
 * Evaluates a null/empty condition against a given value
 * 
 * 需求: 21.2.7 - 空值判断条件（为空、不为空）
 */
export function evaluateNullCondition(value: any, condition: NullCondition): boolean {
  const isEmpty = value === null || 
                  value === undefined || 
                  value === '' || 
                  (typeof value === 'string' && value.trim() === '') ||
                  (Array.isArray(value) && value.length === 0);
  
  switch (condition.operator) {
    case 'isEmpty':
      return isEmpty;
    case 'isNotEmpty':
      return !isEmpty;
    default:
      return false;
  }
}

/**
 * 评估跨字段比较条件
 * Evaluates a cross-field comparison condition
 * 
 * 需求: 21.2.8 - 跨字段比较条件（字段A > 字段B）
 */
export function evaluateCrossFieldCondition(
  condition: CrossFieldCondition,
  rowData: Record<string, any>
): boolean {
  const value1 = rowData[condition.field1];
  const value2 = rowData[condition.field2];
  
  // 处理空值
  if (value1 === null || value1 === undefined || value2 === null || value2 === undefined) {
    return false;
  }
  
  // 确定比较类型
  const valueType = condition.valueType || 'auto';
  let compareResult: number;
  
  if (valueType === 'number' || (valueType === 'auto' && typeof value1 === 'number' && typeof value2 === 'number')) {
    // 数值比较
    const num1 = typeof value1 === 'number' ? value1 : parseFloat(value1);
    const num2 = typeof value2 === 'number' ? value2 : parseFloat(value2);
    if (isNaN(num1) || isNaN(num2)) {
      return false;
    }
    compareResult = num1 - num2;
  } else if (valueType === 'date' || (valueType === 'auto' && (value1 instanceof Date || value2 instanceof Date))) {
    // 日期比较
    const date1 = value1 instanceof Date ? value1 : new Date(value1);
    const date2 = value2 instanceof Date ? value2 : new Date(value2);
    if (isNaN(date1.getTime()) || isNaN(date2.getTime())) {
      return false;
    }
    compareResult = date1.getTime() - date2.getTime();
  } else {
    // 字符串比较
    const str1 = String(value1);
    const str2 = String(value2);
    compareResult = str1.localeCompare(str2);
  }
  
  switch (condition.operator) {
    case 'eq':
      return compareResult === 0;
    case 'ne':
      return compareResult !== 0;
    case 'gt':
      return compareResult > 0;
    case 'gte':
      return compareResult >= 0;
    case 'lt':
      return compareResult < 0;
    case 'lte':
      return compareResult <= 0;
    default:
      return false;
  }
}


/**
 * 评估公式条件
 * Evaluates a formula condition against row data
 * 
 * 需求: 21.2.9 - 自定义表达式条件（支持数学运算和函数）
 * 
 * 支持的表达式语法：
 * - 变量引用: ${fieldName} 或 ${value}
 * - 数学运算: +, -, *, /, %, **
 * - 比较运算: ==, !=, >, <, >=, <=
 * - 逻辑运算: &&, ||, !
 * - 内置函数: abs(), round(), floor(), ceil(), min(), max(), sqrt(), pow()
 * - 字符串函数: len(), upper(), lower(), trim()
 * - 聚合函数: sum(), avg(), count() (需要 allValues)
 */
export function evaluateFormulaCondition(
  value: any,
  condition: FormulaCondition,
  rowData: Record<string, any>,
  allValues?: any[]
): boolean {
  try {
    let expression = condition.expression;
    
    // 替换 ${value} 为当前值
    const safeValue = value ?? 0;
    expression = expression.replace(/\$\{value\}/g, JSON.stringify(safeValue));
    
    // 替换变量映射
    if (condition.variables) {
      for (const [varName, fieldName] of Object.entries(condition.variables)) {
        const fieldValue = rowData[fieldName] ?? 0;
        expression = expression.replace(
          new RegExp(`\\$\\{${escapeRegExp(varName)}\\}`, 'g'),
          JSON.stringify(fieldValue)
        );
      }
    }
    
    // 替换行数据中的字段引用
    for (const [key, val] of Object.entries(rowData)) {
      const safeVal = val ?? 0;
      expression = expression.replace(
        new RegExp(`\\$\\{${escapeRegExp(key)}\\}`, 'g'),
        JSON.stringify(safeVal)
      );
    }
    
    // 替换聚合函数（如果提供了 allValues）
    if (allValues && allValues.length > 0) {
      const numericValues = allValues
        .filter(v => v !== null && v !== undefined)
        .map(v => typeof v === 'number' ? v : parseFloat(v))
        .filter(v => !isNaN(v));
      
      if (numericValues.length > 0) {
        const sum = numericValues.reduce((a, b) => a + b, 0);
        const avg = sum / numericValues.length;
        const count = numericValues.length;
        const minVal = Math.min(...numericValues);
        const maxVal = Math.max(...numericValues);
        
        expression = expression.replace(/\bsum\(\)/gi, String(sum));
        expression = expression.replace(/\bavg\(\)/gi, String(avg));
        expression = expression.replace(/\bcount\(\)/gi, String(count));
        expression = expression.replace(/\bmin\(\)/gi, String(minVal));
        expression = expression.replace(/\bmax\(\)/gi, String(maxVal));
      }
    }
    
    // 创建安全的函数上下文
    const safeContext = createSafeExpressionContext();
    
    // 安全评估表达式
    // eslint-disable-next-line no-new-func
    const evalFunc = new Function(...Object.keys(safeContext), `return ${expression}`);
    const result = evalFunc(...Object.values(safeContext));
    return Boolean(result);
  } catch {
    return false;
  }
}

/**
 * 转义正则表达式特殊字符
 */
function escapeRegExp(string: string): string {
  return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * 创建安全的表达式上下文
 * 提供内置数学函数和字符串函数
 */
function createSafeExpressionContext(): Record<string, any> {
  return {
    // 数学函数
    abs: Math.abs,
    round: Math.round,
    floor: Math.floor,
    ceil: Math.ceil,
    sqrt: Math.sqrt,
    pow: Math.pow,
    min: Math.min,
    max: Math.max,
    // 字符串函数
    len: (s: any) => String(s ?? '').length,
    upper: (s: any) => String(s ?? '').toUpperCase(),
    lower: (s: any) => String(s ?? '').toLowerCase(),
    trim: (s: any) => String(s ?? '').trim(),
    // 类型检查函数
    isNumber: (v: any) => typeof v === 'number' && !isNaN(v),
    isString: (v: any) => typeof v === 'string',
    isEmpty: (v: any) => v === null || v === undefined || v === '' || (typeof v === 'string' && v.trim() === ''),
  };
}

/**
 * 评估 Top/Bottom 条件
 * Evaluates a top/bottom condition (requires all values for comparison)
 */
export function evaluateTopCondition(
  value: any,
  condition: TopCondition,
  allValues: number[]
): boolean {
  if (value === null || value === undefined) {
    return false;
  }

  const numValue = typeof value === 'number' ? value : parseFloat(value);
  if (isNaN(numValue)) {
    return false;
  }

  const sortedValues = [...allValues].sort((a, b) => 
    condition.type === 'top' ? b - a : a - b
  );

  let count = condition.count;
  if (condition.isPercent) {
    count = Math.ceil((condition.count / 100) * allValues.length);
  }

  const threshold = sortedValues[Math.min(count - 1, sortedValues.length - 1)];
  
  return condition.type === 'top' ? numValue >= threshold : numValue <= threshold;
}

/**
 * 评估唯一值条件
 */
export function evaluateUniqueCondition(value: any, allValues: any[]): boolean {
  if (value === null || value === undefined) {
    return false;
  }
  return allValues.filter(v => v === value).length === 1;
}

/**
 * 评估重复值条件
 */
export function evaluateDuplicateCondition(value: any, allValues: any[]): boolean {
  if (value === null || value === undefined) {
    return false;
  }
  return allValues.filter(v => v === value).length > 1;
}


/**
 * 评估规则条件
 * Evaluates a rule condition against a value
 * 
 * 需求: 21.2.5, 21.2.6, 21.2.7, 21.2.8, 21.2.9
 */
export function evaluateCondition(
  value: any,
  condition: RuleCondition,
  rowData: Record<string, any> = {},
  allValues: any[] = []
): boolean {
  switch (condition.type) {
    case 'value':
      return evaluateValueCondition(value, condition.config as ValueCondition, rowData);
    case 'text':
      return evaluateTextCondition(value, condition.config as TextCondition, rowData);
    case 'date':
      return evaluateDateCondition(value, condition.config as DateCondition, rowData);
    case 'formula':
      return evaluateFormulaCondition(value, condition.config as FormulaCondition, rowData, allValues);
    case 'top':
      return evaluateTopCondition(value, condition.config as TopCondition, allValues as number[]);
    case 'unique':
      return evaluateUniqueCondition(value, allValues);
    case 'duplicate':
      return evaluateDuplicateCondition(value, allValues);
    case 'null':
      return evaluateNullCondition(value, condition.config as NullCondition);
    case 'crossField':
      return evaluateCrossFieldCondition(condition.config as CrossFieldCondition, rowData);
    default:
      return false;
  }
}

/**
 * 合并样式（按优先级）
 * Merges styles with priority handling
 */
export function mergeStyles(baseStyle: CellStyle, conditionalStyle: CellStyle): CellStyle {
  return deepMerge(baseStyle, conditionalStyle);
}

/**
 * 按优先级排序规则
 * Sorts rules by priority (lower number = higher priority)
 */
export function sortRulesByPriority(rules: ConditionalRule[]): ConditionalRule[] {
  return [...rules].sort((a, b) => a.priority - b.priority);
}

/**
 * 评估单元格样式
 * Evaluates and returns the computed style for a cell value
 */
export function evaluateStyle(
  value: any,
  rowData: Record<string, any>,
  rules: ConditionalRule[],
  columnName?: string,
  rowType: 'data' | 'summary' = 'data',
  allValues: any[] = []
): CellStyle {
  // 过滤启用的规则并按优先级排序
  const enabledRules = sortRulesByPriority(rules.filter(rule => rule.enabled));
  
  let resultStyle: CellStyle = {};
  
  for (const rule of enabledRules) {
    // 检查规则范围
    if (rule.scope) {
      // 检查列范围
      if (rule.scope.columns && columnName && !rule.scope.columns.includes(columnName)) {
        continue;
      }
      // 检查行类型范围
      if (rule.scope.rows && rule.scope.rows !== 'all' && rule.scope.rows !== rowType) {
        continue;
      }
    }
    
    // 评估条件
    if (evaluateCondition(value, rule.condition, rowData, allValues)) {
      resultStyle = mergeStyles(resultStyle, rule.style);
    }
  }
  
  return resultStyle;
}


/**
 * 验证规则
 * Validates a conditional rule
 */
export function validateRule(rule: ConditionalRule): ValidationResult {
  const errors: ValidationError[] = [];
  
  // 验证 ID
  if (!rule.id || rule.id.trim() === '') {
    errors.push({
      field: 'id',
      message: '规则ID不能为空',
      code: 'REQUIRED_ID',
    });
  }
  
  // 验证名称
  if (!rule.name || rule.name.trim() === '') {
    errors.push({
      field: 'name',
      message: '规则名称不能为空',
      code: 'REQUIRED_NAME',
    });
  }
  
  // 验证优先级
  if (typeof rule.priority !== 'number' || rule.priority < 0) {
    errors.push({
      field: 'priority',
      message: '优先级必须是非负整数',
      code: 'INVALID_PRIORITY',
    });
  }
  
  // 验证条件
  if (!rule.condition) {
    errors.push({
      field: 'condition',
      message: '条件不能为空',
      code: 'REQUIRED_CONDITION',
    });
  } else {
    const conditionErrors = validateCondition(rule.condition);
    errors.push(...conditionErrors);
  }
  
  // 验证样式
  if (!rule.style || Object.keys(rule.style).length === 0) {
    errors.push({
      field: 'style',
      message: '样式不能为空',
      code: 'REQUIRED_STYLE',
    });
  }
  
  return {
    valid: errors.length === 0,
    errors,
  };
}

/**
 * 验证条件配置
 */
function validateCondition(condition: RuleCondition): ValidationError[] {
  const errors: ValidationError[] = [];
  
  if (!condition.type) {
    errors.push({
      field: 'condition.type',
      message: '条件类型不能为空',
      code: 'REQUIRED_CONDITION_TYPE',
    });
    return errors;
  }
  
  if (!condition.config) {
    errors.push({
      field: 'condition.config',
      message: '条件配置不能为空',
      code: 'REQUIRED_CONDITION_CONFIG',
    });
    return errors;
  }
  
  switch (condition.type) {
    case 'value':
      errors.push(...validateValueCondition(condition.config as ValueCondition));
      break;
    case 'text':
      errors.push(...validateTextCondition(condition.config as TextCondition));
      break;
    case 'top':
      errors.push(...validateTopCondition(condition.config as TopCondition));
      break;
    case 'null':
      errors.push(...validateNullCondition(condition.config as NullCondition));
      break;
    case 'crossField':
      errors.push(...validateCrossFieldCondition(condition.config as CrossFieldCondition));
      break;
    case 'formula':
      errors.push(...validateFormulaCondition(condition.config as FormulaCondition));
      break;
  }
  
  return errors;
}

function validateValueCondition(config: ValueCondition): ValidationError[] {
  const errors: ValidationError[] = [];
  
  if (!config.operator) {
    errors.push({
      field: 'condition.config.operator',
      message: '操作符不能为空',
      code: 'REQUIRED_OPERATOR',
    });
  }
  
  if (['between', 'notBetween'].includes(config.operator)) {
    if (config.value === undefined || config.value2 === undefined) {
      errors.push({
        field: 'condition.config.value',
        message: '区间条件需要两个值',
        code: 'REQUIRED_RANGE_VALUES',
      });
    }
  } else if (!config.compareField && config.value === undefined) {
    errors.push({
      field: 'condition.config.value',
      message: '比较值不能为空',
      code: 'REQUIRED_VALUE',
    });
  }
  
  return errors;
}

function validateTextCondition(config: TextCondition): ValidationError[] {
  const errors: ValidationError[] = [];
  
  if (!config.operator) {
    errors.push({
      field: 'condition.config.operator',
      message: '操作符不能为空',
      code: 'REQUIRED_OPERATOR',
    });
  }
  
  if (config.value === undefined || config.value === '') {
    errors.push({
      field: 'condition.config.value',
      message: '比较值不能为空',
      code: 'REQUIRED_VALUE',
    });
  }
  
  return errors;
}

function validateTopCondition(config: TopCondition): ValidationError[] {
  const errors: ValidationError[] = [];
  
  if (!config.type || !['top', 'bottom'].includes(config.type)) {
    errors.push({
      field: 'condition.config.type',
      message: '类型必须是 top 或 bottom',
      code: 'INVALID_TOP_TYPE',
    });
  }
  
  if (typeof config.count !== 'number' || config.count <= 0) {
    errors.push({
      field: 'condition.config.count',
      message: '数量必须是正整数',
      code: 'INVALID_COUNT',
    });
  }
  
  return errors;
}

/**
 * 验证空值条件配置
 * 需求: 21.2.7
 */
function validateNullCondition(config: NullCondition): ValidationError[] {
  const errors: ValidationError[] = [];
  
  if (!config.operator || !['isEmpty', 'isNotEmpty'].includes(config.operator)) {
    errors.push({
      field: 'condition.config.operator',
      message: '操作符必须是 isEmpty 或 isNotEmpty',
      code: 'INVALID_NULL_OPERATOR',
    });
  }
  
  return errors;
}

/**
 * 验证跨字段比较条件配置
 * 需求: 21.2.8
 */
function validateCrossFieldCondition(config: CrossFieldCondition): ValidationError[] {
  const errors: ValidationError[] = [];
  
  if (!config.field1 || config.field1.trim() === '') {
    errors.push({
      field: 'condition.config.field1',
      message: '第一个字段名不能为空',
      code: 'REQUIRED_FIELD1',
    });
  }
  
  if (!config.field2 || config.field2.trim() === '') {
    errors.push({
      field: 'condition.config.field2',
      message: '第二个字段名不能为空',
      code: 'REQUIRED_FIELD2',
    });
  }
  
  if (!config.operator || !['eq', 'ne', 'gt', 'gte', 'lt', 'lte'].includes(config.operator)) {
    errors.push({
      field: 'condition.config.operator',
      message: '操作符无效',
      code: 'INVALID_CROSS_FIELD_OPERATOR',
    });
  }
  
  if (config.valueType && !['number', 'string', 'date', 'auto'].includes(config.valueType)) {
    errors.push({
      field: 'condition.config.valueType',
      message: '值类型必须是 number、string、date 或 auto',
      code: 'INVALID_VALUE_TYPE',
    });
  }
  
  return errors;
}

/**
 * 验证公式条件配置
 * 需求: 21.2.9
 */
function validateFormulaCondition(config: FormulaCondition): ValidationError[] {
  const errors: ValidationError[] = [];
  
  if (!config.expression || config.expression.trim() === '') {
    errors.push({
      field: 'condition.config.expression',
      message: '表达式不能为空',
      code: 'REQUIRED_EXPRESSION',
    });
  }
  
  // 基本语法检查
  if (config.expression) {
    try {
      // 尝试解析表达式（替换变量为占位符）
      let testExpr = config.expression.replace(/\$\{[^}]+\}/g, '0');
      testExpr = testExpr.replace(/\b(sum|avg|count|min|max)\(\)/gi, '0');
      // eslint-disable-next-line no-new-func
      new Function(`return ${testExpr}`);
    } catch (e) {
      errors.push({
        field: 'condition.config.expression',
        message: '表达式语法错误',
        code: 'INVALID_EXPRESSION_SYNTAX',
      });
    }
  }
  
  return errors;
}


/**
 * 预设规则模板
 * Preset rule templates
 */

// 预设颜色常量
const PRESET_COLORS = {
  red: '#ff4d4f',
  lightRed: '#fff2f0',
  green: '#52c41a',
  lightGreen: '#f6ffed',
  yellow: '#faad14',
  lightYellow: '#fffbe6',
  gray: '#8c8c8c',
  lightGray: '#fafafa',
  blue: '#1890ff',
  lightBlue: '#e6f7ff',
};

/**
 * 获取预设规则
 * Returns preset rules for a given type
 */
export function getPresetRules(type: PresetRuleType): ConditionalRule[] {
  switch (type) {
    case 'negativeRed':
      return [
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
            font: { color: PRESET_COLORS.red },
          },
        },
      ];

    case 'positiveGreen':
      return [
        {
          id: generateId(),
          name: '正数绿色',
          priority: 1,
          enabled: true,
          condition: {
            type: 'value',
            config: {
              operator: 'gt',
              value: 0,
            } as ValueCondition,
          },
          style: {
            font: { color: PRESET_COLORS.green },
          },
        },
      ];

    case 'zeroGray':
      return [
        {
          id: generateId(),
          name: '零值灰色',
          priority: 1,
          enabled: true,
          condition: {
            type: 'value',
            config: {
              operator: 'eq',
              value: 0,
            } as ValueCondition,
          },
          style: {
            font: { color: PRESET_COLORS.gray },
          },
        },
      ];

    case 'trafficLight':
      return [
        {
          id: generateId(),
          name: '红灯 - 低于70%',
          priority: 1,
          enabled: true,
          condition: {
            type: 'value',
            config: {
              operator: 'lt',
              value: 0.7,
            } as ValueCondition,
          },
          style: {
            font: { color: PRESET_COLORS.red },
            background: { type: 'solid', color: PRESET_COLORS.lightRed },
          },
        },
        {
          id: generateId(),
          name: '黄灯 - 70%-90%',
          priority: 2,
          enabled: true,
          condition: {
            type: 'value',
            config: {
              operator: 'between',
              value: 0.7,
              value2: 0.9,
            } as ValueCondition,
          },
          style: {
            font: { color: PRESET_COLORS.yellow },
            background: { type: 'solid', color: PRESET_COLORS.lightYellow },
          },
        },
        {
          id: generateId(),
          name: '绿灯 - 高于90%',
          priority: 3,
          enabled: true,
          condition: {
            type: 'value',
            config: {
              operator: 'gte',
              value: 0.9,
            } as ValueCondition,
          },
          style: {
            font: { color: PRESET_COLORS.green },
            background: { type: 'solid', color: PRESET_COLORS.lightGreen },
          },
        },
      ];

    case 'percentageThreshold':
      return [
        {
          id: generateId(),
          name: '低于目标值警告',
          priority: 1,
          enabled: true,
          condition: {
            type: 'value',
            config: {
              operator: 'lt',
              value: 1.0, // 100% 目标值
            } as ValueCondition,
          },
          style: {
            font: { color: PRESET_COLORS.red },
            background: { type: 'solid', color: PRESET_COLORS.lightRed },
          },
        },
        {
          id: generateId(),
          name: '达到目标值',
          priority: 2,
          enabled: true,
          condition: {
            type: 'value',
            config: {
              operator: 'gte',
              value: 1.0,
            } as ValueCondition,
          },
          style: {
            font: { color: PRESET_COLORS.green },
            background: { type: 'solid', color: PRESET_COLORS.lightGreen },
          },
        },
      ];

    case 'duplicateHighlight':
      return [
        {
          id: generateId(),
          name: '重复值高亮',
          priority: 1,
          enabled: true,
          condition: {
            type: 'duplicate',
            config: {} as any,
          },
          style: {
            background: { type: 'solid', color: PRESET_COLORS.lightYellow },
          },
        },
      ];

    case 'uniqueHighlight':
      return [
        {
          id: generateId(),
          name: '唯一值高亮',
          priority: 1,
          enabled: true,
          condition: {
            type: 'unique',
            config: {} as any,
          },
          style: {
            background: { type: 'solid', color: PRESET_COLORS.lightBlue },
          },
        },
      ];

    default:
      return [];
  }
}


/**
 * 创建数值范围条件规则
 * Creates a value range condition rule
 */
export function createValueRangeRule(
  name: string,
  operator: ValueCondition['operator'],
  value: number,
  value2: number | undefined,
  style: CellStyle,
  priority: number = 1
): ConditionalRule {
  return {
    id: generateId(),
    name,
    priority,
    enabled: true,
    condition: {
      type: 'value',
      config: {
        operator,
        value,
        value2,
      } as ValueCondition,
    },
    style,
  };
}

/**
 * 创建百分比阈值规则
 * Creates a percentage threshold rule
 */
export function createPercentageThresholdRule(
  name: string,
  threshold: number,
  belowStyle: CellStyle,
  aboveStyle: CellStyle
): ConditionalRule[] {
  return [
    {
      id: generateId(),
      name: `${name} - 低于${threshold * 100}%`,
      priority: 1,
      enabled: true,
      condition: {
        type: 'value',
        config: {
          operator: 'lt',
          value: threshold,
        } as ValueCondition,
      },
      style: belowStyle,
    },
    {
      id: generateId(),
      name: `${name} - 达到${threshold * 100}%`,
      priority: 2,
      enabled: true,
      condition: {
        type: 'value',
        config: {
          operator: 'gte',
          value: threshold,
        } as ValueCondition,
      },
      style: aboveStyle,
    },
  ];
}

/**
 * 创建色阶规则
 * Creates color scale rules for a range of values
 */
export function createColorScaleRules(
  config: ColorScaleConfig,
  steps: number = 5
): ConditionalRule[] {
  const rules: ConditionalRule[] = [];
  const minVal = config.minValue ?? 0;
  const maxVal = config.maxValue ?? 100;
  const range = maxVal - minVal;
  const stepSize = range / steps;

  for (let i = 0; i < steps; i++) {
    const lowerBound = minVal + i * stepSize;
    const upperBound = minVal + (i + 1) * stepSize;
    const ratio = i / (steps - 1);
    
    // 计算颜色插值
    const color = interpolateColor(
      config.minColor,
      config.midColor || config.maxColor,
      config.maxColor,
      ratio
    );

    rules.push({
      id: generateId(),
      name: `色阶 ${i + 1}`,
      priority: steps - i, // 较高值优先级更高
      enabled: true,
      condition: {
        type: 'value',
        config: {
          operator: i === steps - 1 ? 'gte' : 'between',
          value: lowerBound,
          value2: i === steps - 1 ? undefined : upperBound,
        } as ValueCondition,
      },
      style: {
        background: { type: 'solid', color },
      },
    });
  }

  return rules;
}

/**
 * 创建空值条件规则
 * Creates a null/empty condition rule
 * 
 * 需求: 21.2.7 - 空值判断条件（为空、不为空）
 */
export function createNullConditionRule(
  name: string,
  operator: 'isEmpty' | 'isNotEmpty',
  style: CellStyle,
  priority: number = 1
): ConditionalRule {
  return {
    id: generateId(),
    name,
    priority,
    enabled: true,
    condition: {
      type: 'null',
      config: {
        operator,
      } as NullCondition,
    },
    style,
  };
}

/**
 * 创建跨字段比较规则
 * Creates a cross-field comparison rule
 * 
 * 需求: 21.2.8 - 跨字段比较条件（字段A > 字段B）
 */
export function createCrossFieldRule(
  name: string,
  field1: string,
  operator: 'eq' | 'ne' | 'gt' | 'gte' | 'lt' | 'lte',
  field2: string,
  style: CellStyle,
  valueType: 'number' | 'string' | 'date' | 'auto' = 'auto',
  priority: number = 1
): ConditionalRule {
  return {
    id: generateId(),
    name,
    priority,
    enabled: true,
    condition: {
      type: 'crossField',
      config: {
        field1,
        operator,
        field2,
        valueType,
      } as CrossFieldCondition,
    },
    style,
  };
}

/**
 * 创建自定义表达式规则
 * Creates a custom expression condition rule
 * 
 * 需求: 21.2.9 - 自定义表达式条件（支持数学运算和函数）
 */
export function createFormulaRule(
  name: string,
  expression: string,
  style: CellStyle,
  variables?: Record<string, string>,
  priority: number = 1
): ConditionalRule {
  return {
    id: generateId(),
    name,
    priority,
    enabled: true,
    condition: {
      type: 'formula',
      config: {
        expression,
        variables,
      } as FormulaCondition,
    },
    style,
  };
}

/**
 * 创建文本匹配规则
 * Creates a text matching condition rule
 * 
 * 需求: 21.2.6 - 文本匹配条件（包含、开头、结尾、正则匹配）
 */
export function createTextMatchRule(
  name: string,
  operator: TextCondition['operator'],
  value: string,
  style: CellStyle,
  caseSensitive: boolean = false,
  priority: number = 1
): ConditionalRule {
  return {
    id: generateId(),
    name,
    priority,
    enabled: true,
    condition: {
      type: 'text',
      config: {
        operator,
        value,
        caseSensitive,
      } as TextCondition,
    },
    style,
  };
}

/**
 * 颜色插值
 * Interpolates between colors
 */
function interpolateColor(
  minColor: string,
  midColor: string,
  maxColor: string,
  ratio: number
): string {
  const parseHex = (hex: string): [number, number, number] => {
    const h = hex.replace('#', '');
    return [
      parseInt(h.substring(0, 2), 16),
      parseInt(h.substring(2, 4), 16),
      parseInt(h.substring(4, 6), 16),
    ];
  };

  const toHex = (r: number, g: number, b: number): string => {
    return `#${[r, g, b].map(v => Math.round(v).toString(16).padStart(2, '0')).join('')}`;
  };

  const [r1, g1, b1] = parseHex(minColor);
  const [r2, g2, b2] = parseHex(midColor);
  const [r3, g3, b3] = parseHex(maxColor);

  let r: number, g: number, b: number;

  if (ratio <= 0.5) {
    const t = ratio * 2;
    r = r1 + (r2 - r1) * t;
    g = g1 + (g2 - g1) * t;
    b = b1 + (b2 - b1) * t;
  } else {
    const t = (ratio - 0.5) * 2;
    r = r2 + (r3 - r2) * t;
    g = g2 + (g3 - g2) * t;
    b = b2 + (b3 - b2) * t;
  }

  return toHex(r, g, b);
}


/**
 * 条件格式化服务类
 * Conditional Format Service Class
 */
export class ConditionalFormatService {
  private rules: ConditionalRule[] = [];

  constructor(initialRules: ConditionalRule[] = []) {
    this.rules = initialRules;
  }

  /**
   * 添加规则
   */
  addRule(rule: ConditionalRule): void {
    const validation = validateRule(rule);
    if (!validation.valid) {
      throw new Error(`Invalid rule: ${validation.errors.map(e => e.message).join(', ')}`);
    }
    this.rules.push(rule);
  }

  /**
   * 移除规则
   */
  removeRule(ruleId: string): boolean {
    const index = this.rules.findIndex(r => r.id === ruleId);
    if (index !== -1) {
      this.rules.splice(index, 1);
      return true;
    }
    return false;
  }

  /**
   * 更新规则
   */
  updateRule(ruleId: string, updates: Partial<ConditionalRule>): boolean {
    const index = this.rules.findIndex(r => r.id === ruleId);
    if (index !== -1) {
      this.rules[index] = { ...this.rules[index], ...updates };
      return true;
    }
    return false;
  }

  /**
   * 获取所有规则
   */
  getRules(): ConditionalRule[] {
    return [...this.rules];
  }

  /**
   * 获取排序后的规则
   */
  getSortedRules(): ConditionalRule[] {
    return sortRulesByPriority(this.rules);
  }

  /**
   * 启用/禁用规则
   */
  toggleRule(ruleId: string, enabled: boolean): boolean {
    return this.updateRule(ruleId, { enabled });
  }

  /**
   * 评估单元格样式
   */
  evaluateStyle(
    value: any,
    rowData: Record<string, any>,
    columnName?: string,
    rowType: 'data' | 'summary' = 'data',
    allValues: any[] = []
  ): CellStyle {
    return evaluateStyle(value, rowData, this.rules, columnName, rowType, allValues);
  }

  /**
   * 合并样式
   */
  mergeStyles(baseStyle: CellStyle, conditionalStyle: CellStyle): CellStyle {
    return mergeStyles(baseStyle, conditionalStyle);
  }

  /**
   * 验证规则
   */
  validateRule(rule: ConditionalRule): ValidationResult {
    return validateRule(rule);
  }

  /**
   * 获取预设规则
   */
  getPresetRules(type: PresetRuleType): ConditionalRule[] {
    return getPresetRules(type);
  }

  /**
   * 应用预设规则
   */
  applyPresetRules(type: PresetRuleType): void {
    const presetRules = getPresetRules(type);
    this.rules.push(...presetRules);
  }

  /**
   * 清空所有规则
   */
  clearRules(): void {
    this.rules = [];
  }

  /**
   * 批量设置规则
   */
  setRules(rules: ConditionalRule[]): void {
    this.rules = [...rules];
  }

  /**
   * 重新排序规则优先级
   */
  reorderRules(ruleIds: string[]): void {
    const reorderedRules: ConditionalRule[] = [];
    ruleIds.forEach((id, index) => {
      const rule = this.rules.find(r => r.id === id);
      if (rule) {
        reorderedRules.push({ ...rule, priority: index + 1 });
      }
    });
    // 添加未在列表中的规则
    this.rules.forEach(rule => {
      if (!ruleIds.includes(rule.id)) {
        reorderedRules.push(rule);
      }
    });
    this.rules = reorderedRules;
  }
}

/**
 * 创建条件格式化服务实例
 */
export function createConditionalFormatService(
  initialRules: ConditionalRule[] = []
): ConditionalFormatService {
  return new ConditionalFormatService(initialRules);
}

// 导出预设颜色供外部使用
export { PRESET_COLORS };

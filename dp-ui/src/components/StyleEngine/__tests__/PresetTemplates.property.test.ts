/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * 样式模板属性测试
 * Style Template Property Tests
 * 
 * **属性 74: 样式模板应用完整性**
 * **验证需求: 21.3**
 * 
 * 测试内容:
 * 1. templateToStyleConfig 应产生有效的 StyleConfig
 * 2. applyTemplateToField 应正确匹配字段并返回样式
 * 3. getFieldConditionalRules 应返回所有适用的条件规则
 * 4. 所有预设模板应通过验证
 * 5. 模板克隆应产生相同的模板
 * 6. 模板合并应正确组合样式
 */

import { describe, it, expect } from 'vitest';
import * as fc from 'fast-check';
import {
  FINANCE_TEMPLATE,
  SALES_TEMPLATE,
  INVENTORY_TEMPLATE,
  KPI_TEMPLATE,
  PRESET_TEMPLATES,
  getAllPresetTemplates,
  getPresetTemplateById,
  getPresetTemplatesByCategory,
  templateToStyleConfig,
  applyTemplateToField,
  getFieldConditionalRules,
  validateTemplate,
  cloneTemplate,
  mergeTemplates,
  createCustomTemplate,
  type StyleTemplate,
  type TemplateCategory,
} from '../presetTemplates';
import type { CellStyle } from '../types/style';
import type { ConditionalRule } from '../types/conditional';

// ============================================================================
// Arbitraries (测试数据生成器)
// ============================================================================

/** 生成模板分类 */
const templateCategoryArb: fc.Arbitrary<TemplateCategory> = fc.constantFrom(
  'finance', 'sales', 'inventory', 'kpi', 'custom'
);

/** 生成预设模板 */
const presetTemplateArb: fc.Arbitrary<StyleTemplate> = fc.constantFrom(
  FINANCE_TEMPLATE,
  SALES_TEMPLATE,
  INVENTORY_TEMPLATE,
  KPI_TEMPLATE
);

/** 生成颜色值 */
const colorArb = fc.constantFrom(
  '#ff0000', '#00ff00', '#0000ff', '#ffff00', '#ff00ff', '#00ffff',
  '#ff4d4f', '#52c41a', '#faad14', '#1890ff', '#722ed1', '#eb2f96',
  '#ffffff', '#000000', '#f0f0f0', '#333333'
);

/** 生成字体族 */
const fontFamilyArb = fc.constantFrom(
  'Arial', 'Microsoft YaHei', 'Helvetica', 'Times New Roman', 'Courier New'
);

/** 生成字体大小 */
const fontSizeArb = fc.integer({ min: 10, max: 32 });

/** 生成字体粗细 */
const fontWeightArb = fc.constantFrom('normal', 'bold', 400, 500, 600, 700);

/** 生成对齐方式 */
const horizontalAlignArb = fc.constantFrom('left', 'center', 'right', 'justify');
const verticalAlignArb = fc.constantFrom('top', 'middle', 'bottom');

/** 生成边框样式 */
const borderStyleTypeArb = fc.constantFrom('none', 'solid', 'dashed', 'dotted', 'double');

/** 生成简单单元格样式 */
const simpleCellStyleArb: fc.Arbitrary<CellStyle> = fc.record({
  font: fc.option(
    fc.record({
      family: fc.option(fontFamilyArb, { nil: undefined }),
      size: fc.option(fontSizeArb, { nil: undefined }),
      weight: fc.option(fontWeightArb as fc.Arbitrary<'normal' | 'bold' | number>, { nil: undefined }),
      color: fc.option(colorArb, { nil: undefined }),
    }),
    { nil: undefined }
  ),
  alignment: fc.option(
    fc.record({
      horizontal: fc.option(horizontalAlignArb as fc.Arbitrary<'left' | 'center' | 'right' | 'justify'>, { nil: undefined }),
      vertical: fc.option(verticalAlignArb as fc.Arbitrary<'top' | 'middle' | 'bottom'>, { nil: undefined }),
    }),
    { nil: undefined }
  ),
  background: fc.option(
    fc.record({
      type: fc.constant('solid' as const),
      color: colorArb,
    }),
    { nil: undefined }
  ),
});

/** 财务相关字段名 */
const financeFieldNameArb = fc.constantFrom(
  'amount', 'money', 'price', 'cost', 'revenue', 'profit',
  'income', 'expense', 'balance', 'total_amount', 'net_profit'
);

/** 销售相关字段名 */
const salesFieldNameArb = fc.constantFrom(
  'rate', 'ratio', 'achievement', 'completion', 'target',
  'sales', 'revenue', 'rank', 'ranking', 'yoy', 'mom'
);

/** 库存相关字段名 */
const inventoryFieldNameArb = fc.constantFrom(
  'stock', 'inventory', 'quantity', 'qty', 'safety_stock',
  'turnover', 'turnover_rate', 'days_on_hand', 'inventory_days'
);

/** KPI相关字段名 */
const kpiFieldNameArb = fc.constantFrom(
  'achievement', 'completion', 'target_rate', 'kpi_rate',
  'actual', 'value', 'current', 'target', 'goal', 'yoy', 'mom', 'trend'
);

/** 通用字段名 */
const genericFieldNameArb = fc.constantFrom(
  'id', 'name', 'description', 'status', 'created_at', 'updated_at',
  'user_id', 'category', 'type', 'code', 'remark'
);

/** 模板名称 */
const templateNameArb = fc.string({ minLength: 1, maxLength: 50 }).filter(s => s.trim().length > 0);

/** 模板描述 */
const templateDescriptionArb = fc.string({ minLength: 1, maxLength: 200 }).filter(s => s.trim().length > 0);

// ============================================================================
// 辅助函数
// ============================================================================

/** 检查样式是否有效（非空且有定义的属性） */
function isValidStyle(style: CellStyle | undefined): boolean {
  if (!style) return false;
  return Object.keys(style).length > 0 && Object.values(style).some(v => v !== undefined);
}

/** 检查两个样式是否深度相等 */
function stylesDeepEqual(style1: CellStyle | undefined, style2: CellStyle | undefined): boolean {
  return JSON.stringify(style1) === JSON.stringify(style2);
}

/** 检查条件规则是否有效 */
function isValidConditionalRule(rule: ConditionalRule): boolean {
  return (
    typeof rule.id === 'string' &&
    rule.id.length > 0 &&
    typeof rule.name === 'string' &&
    typeof rule.priority === 'number' &&
    typeof rule.enabled === 'boolean' &&
    rule.condition !== undefined &&
    rule.style !== undefined
  );
}

// ============================================================================
// 属性测试
// ============================================================================

describe('Style Template Property Tests', () => {
  
  // ==========================================================================
  // 属性 74: 样式模板应用完整性
  // ==========================================================================
  
  describe('Property 74: Style Template Application Completeness', () => {
    
    /**
     * **Validates: Requirements 21.3**
     * 
     * 属性 74.1: templateToStyleConfig 应产生有效的 StyleConfig
     * 对于任意预设模板，转换后的 StyleConfig 应包含所有必要的样式配置
     */
    it('Property 74.1: templateToStyleConfig should produce valid StyleConfig', () => {
      fc.assert(
        fc.property(
          presetTemplateArb,
          (template) => {
            const styleConfig = templateToStyleConfig(template);
            
            // StyleConfig 应包含全局样式
            if (!styleConfig.globalStyle) return false;
            
            // StyleConfig 应包含行样式配置
            if (!styleConfig.rowStyles) return false;
            
            // 如果模板有条件规则，StyleConfig 也应包含
            if (template.conditionalRules.length > 0) {
              if (!styleConfig.conditionalRules || styleConfig.conditionalRules.length === 0) {
                return false;
              }
            }
            
            // 全局样式应来自模板的 bodyStyle
            const bodyStyleStr = JSON.stringify(template.tableStyle.bodyStyle);
            const globalStyleStr = JSON.stringify(styleConfig.globalStyle);
            if (bodyStyleStr !== globalStyleStr) return false;
            
            // 表头样式应正确传递
            if (template.tableStyle.headerStyle) {
              const headerStyleStr = JSON.stringify(template.tableStyle.headerStyle);
              const rowHeaderStyleStr = JSON.stringify(styleConfig.rowStyles.headerStyle);
              if (headerStyleStr !== rowHeaderStyleStr) return false;
            }
            
            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * 属性 74.2: templateToStyleConfig 应保留所有条件规则
     * 转换后的 StyleConfig 应包含模板中定义的所有条件格式化规则
     */
    it('Property 74.2: templateToStyleConfig should preserve all conditional rules', () => {
      fc.assert(
        fc.property(
          presetTemplateArb,
          (template) => {
            const styleConfig = templateToStyleConfig(template);
            
            // 条件规则数量应相同
            if (styleConfig.conditionalRules?.length !== template.conditionalRules.length) {
              return false;
            }
            
            // 每个规则都应被保留
            for (const rule of template.conditionalRules) {
              const found = styleConfig.conditionalRules?.find(r => r.id === rule.id);
              if (!found) return false;
              
              // 规则内容应相同
              if (JSON.stringify(found) !== JSON.stringify(rule)) return false;
            }
            
            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * 属性 74.3: applyTemplateToField 应正确匹配财务字段
     * 对于财务模板，金额相关字段应返回正确的样式
     */
    it('Property 74.3: applyTemplateToField should match finance fields correctly', () => {
      fc.assert(
        fc.property(
          financeFieldNameArb,
          (fieldName) => {
            const style = applyTemplateToField(FINANCE_TEMPLATE, fieldName);
            
            // 财务字段应匹配到样式
            if (!style) return false;
            
            // 样式应包含右对齐（财务数字通常右对齐）
            if (style.alignment?.horizontal !== 'right') return false;
            
            // 样式应包含数字格式配置
            if (!style.format || style.format.type !== 'number') return false;
            
            return true;
          }
        ),
        { numRuns: 50 }
      );
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * 属性 74.4: applyTemplateToField 对不匹配字段返回 undefined
     * 对于不匹配模板列样式模式的字段，应返回 undefined
     */
    it('Property 74.4: applyTemplateToField should return undefined for non-matching fields', () => {
      fc.assert(
        fc.property(
          presetTemplateArb,
          genericFieldNameArb,
          (template, fieldName) => {
            const style = applyTemplateToField(template, fieldName);
            
            // 通用字段名（如 id, name, description）通常不匹配特定模板的列样式
            // 如果返回了样式，需要验证确实有匹配的模式
            if (style) {
              // 检查是否有匹配的模式
              let hasMatch = false;
              for (const config of Object.values(template.columnStyles)) {
                if (config.fieldPattern) {
                  const regex = new RegExp(config.fieldPattern, 'i');
                  if (regex.test(fieldName)) {
                    hasMatch = true;
                    break;
                  }
                }
              }
              return hasMatch;
            }
            
            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * 属性 74.5: getFieldConditionalRules 应返回所有适用规则
     * 对于任意字段，应返回所有适用的条件规则（无范围限制或字段在范围内）
     */
    it('Property 74.5: getFieldConditionalRules should return all applicable rules', () => {
      fc.assert(
        fc.property(
          presetTemplateArb,
          financeFieldNameArb,
          (template, fieldName) => {
            const rules = getFieldConditionalRules(template, fieldName);
            
            // 验证返回的规则都是适用的
            for (const rule of rules) {
              if (rule.scope?.columns) {
                // 如果有范围限制，字段应在范围内
                const isInScope = rule.scope.columns.some(col => {
                  const regex = new RegExp(col, 'i');
                  return regex.test(fieldName);
                });
                if (!isInScope) return false;
              }
              // 无范围限制的规则应该被包含
            }
            
            // 验证没有遗漏适用的规则
            for (const rule of template.conditionalRules) {
              if (!rule.scope?.columns) {
                // 无范围限制的规则应该被包含
                if (!rules.find(r => r.id === rule.id)) return false;
              } else {
                // 有范围限制的规则，检查字段是否匹配
                const isInScope = rule.scope.columns.some(col => {
                  const regex = new RegExp(col, 'i');
                  return regex.test(fieldName);
                });
                if (isInScope && !rules.find(r => r.id === rule.id)) return false;
              }
            }
            
            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * 属性 74.6: 所有预设模板应通过验证
     * FINANCE, SALES, INVENTORY, KPI 模板都应是有效的
     */
    it('Property 74.6: All preset templates should pass validation', () => {
      fc.assert(
        fc.property(
          presetTemplateArb,
          (template) => {
            const result = validateTemplate(template);
            
            // 预设模板应该是有效的
            if (!result.valid) {
              console.log('Validation errors:', result.errors);
              return false;
            }
            
            // 不应有任何错误
            return result.errors.length === 0;
          }
        ),
        { numRuns: 100 }
      );
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * 属性 74.7: validateTemplate 应检测无效模板
     * 缺少必要字段的模板应验证失败
     */
    it('Property 74.7: validateTemplate should detect invalid templates', () => {
      // 测试缺少 ID 的模板
      const noIdTemplate = { ...FINANCE_TEMPLATE, id: '' };
      const noIdResult = validateTemplate(noIdTemplate);
      expect(noIdResult.valid).toBe(false);
      expect(noIdResult.errors).toContain('模板ID不能为空');
      
      // 测试缺少名称的模板
      const noNameTemplate = { ...FINANCE_TEMPLATE, name: '' };
      const noNameResult = validateTemplate(noNameTemplate);
      expect(noNameResult.valid).toBe(false);
      expect(noNameResult.errors).toContain('模板名称不能为空');
      
      // 测试缺少表格样式的模板
      const noTableStyleTemplate = { ...FINANCE_TEMPLATE, tableStyle: undefined as any };
      const noTableStyleResult = validateTemplate(noTableStyleTemplate);
      expect(noTableStyleResult.valid).toBe(false);
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * 属性 74.8: cloneTemplate 应产生相同内容的模板
     * 克隆的模板应与原模板具有相同的样式配置
     */
    it('Property 74.8: cloneTemplate should produce identical template content', () => {
      fc.assert(
        fc.property(
          presetTemplateArb,
          (template) => {
            const cloned = cloneTemplate(template);
            
            // ID 应该不同
            if (cloned.id === template.id) return false;
            
            // 名称应包含 "(副本)" 后缀
            if (!cloned.name.includes('副本')) return false;
            
            // 克隆的模板不应是系统模板
            if (cloned.isSystem) return false;
            
            // 列样式应相同
            if (JSON.stringify(cloned.columnStyles) !== JSON.stringify(template.columnStyles)) {
              return false;
            }
            
            // 条件规则应相同
            if (JSON.stringify(cloned.conditionalRules) !== JSON.stringify(template.conditionalRules)) {
              return false;
            }
            
            // 表格样式应相同
            if (JSON.stringify(cloned.tableStyle) !== JSON.stringify(template.tableStyle)) {
              return false;
            }
            
            return true;
          }
        ),
        { numRuns: 100 }
      );
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * 属性 74.9: cloneTemplate 应产生独立的副本
     * 修改克隆的模板不应影响原模板
     */
    it('Property 74.9: cloneTemplate should produce independent copy', () => {
      fc.assert(
        fc.property(
          presetTemplateArb,
          colorArb,
          (template, newColor) => {
            const cloned = cloneTemplate(template);
            
            // 保存原始值
            const originalHeaderColor = template.tableStyle.headerStyle.background?.color;
            
            // 修改克隆的模板
            if (cloned.tableStyle.headerStyle.background) {
              cloned.tableStyle.headerStyle.background.color = newColor;
            }
            
            // 原模板不应被修改
            return template.tableStyle.headerStyle.background?.color === originalHeaderColor;
          }
        ),
        { numRuns: 50 }
      );
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * 属性 74.10: mergeTemplates 应正确合并列样式
     * 合并后的模板应包含两个模板的所有列样式
     */
    it('Property 74.10: mergeTemplates should correctly merge column styles', () => {
      fc.assert(
        fc.property(
          presetTemplateArb,
          presetTemplateArb,
          (base, override) => {
            const merged = mergeTemplates(base, { columnStyles: override.columnStyles });
            
            // 基础模板的列样式应被保留（除非被覆盖）
            for (const [key, style] of Object.entries(base.columnStyles)) {
              if (!override.columnStyles[key]) {
                // 未被覆盖的样式应保留
                if (JSON.stringify(merged.columnStyles[key]) !== JSON.stringify(style)) {
                  return false;
                }
              }
            }
            
            // 覆盖模板的列样式应被应用
            for (const [key, style] of Object.entries(override.columnStyles)) {
              if (JSON.stringify(merged.columnStyles[key]) !== JSON.stringify(style)) {
                return false;
              }
            }
            
            return true;
          }
        ),
        { numRuns: 50 }
      );
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * 属性 74.11: mergeTemplates 应合并条件规则
     * 合并后的模板应包含两个模板的所有条件规则
     */
    it('Property 74.11: mergeTemplates should merge conditional rules', () => {
      fc.assert(
        fc.property(
          presetTemplateArb,
          presetTemplateArb,
          (base, override) => {
            const merged = mergeTemplates(base, { conditionalRules: override.conditionalRules });
            
            // 合并后的规则数量应等于两者之和
            const expectedCount = base.conditionalRules.length + override.conditionalRules.length;
            if (merged.conditionalRules.length !== expectedCount) return false;
            
            // 基础模板的所有规则应被保留
            for (const rule of base.conditionalRules) {
              if (!merged.conditionalRules.find(r => r.id === rule.id)) return false;
            }
            
            // 覆盖模板的所有规则应被添加
            for (const rule of override.conditionalRules) {
              if (!merged.conditionalRules.find(r => r.id === rule.id)) return false;
            }
            
            return true;
          }
        ),
        { numRuns: 50 }
      );
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * 属性 74.12: mergeTemplates 应覆盖表格样式
     * 覆盖模板的表格样式应覆盖基础模板的表格样式
     */
    it('Property 74.12: mergeTemplates should override table styles', () => {
      fc.assert(
        fc.property(
          presetTemplateArb,
          presetTemplateArb,
          (base, override) => {
            const merged = mergeTemplates(base, { tableStyle: override.tableStyle });
            
            // 表格样式应被覆盖
            // 注意：mergeTemplates 使用浅合并，所以整个 tableStyle 对象会被合并
            if (merged.tableStyle.headerStyle !== override.tableStyle.headerStyle) {
              return false;
            }
            if (merged.tableStyle.bodyStyle !== override.tableStyle.bodyStyle) {
              return false;
            }
            
            return true;
          }
        ),
        { numRuns: 50 }
      );
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * 属性 74.13: createCustomTemplate 应创建有效的自定义模板
     * 创建的自定义模板应通过验证
     */
    it('Property 74.13: createCustomTemplate should create valid custom template', () => {
      fc.assert(
        fc.property(
          templateNameArb,
          templateDescriptionArb,
          presetTemplateArb,
          (name, description, baseTemplate) => {
            const customTemplate = createCustomTemplate(name, description, baseTemplate);
            
            // 自定义模板应通过验证
            const result = validateTemplate(customTemplate);
            if (!result.valid) return false;
            
            // 分类应为 custom
            if (customTemplate.category !== 'custom') return false;
            
            // 不应是系统模板
            if (customTemplate.isSystem) return false;
            
            // 名称和描述应正确设置
            if (customTemplate.name !== name) return false;
            if (customTemplate.description !== description) return false;
            
            return true;
          }
        ),
        { numRuns: 50 }
      );
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * 属性 74.14: 模板转换应是确定性的
     * 对于相同的模板，templateToStyleConfig 应始终产生相同的结果
     */
    it('Property 74.14: Template conversion should be deterministic', () => {
      fc.assert(
        fc.property(
          presetTemplateArb,
          (template) => {
            const result1 = templateToStyleConfig(template);
            const result2 = templateToStyleConfig(template);
            
            // 两次转换结果应完全相同
            return JSON.stringify(result1) === JSON.stringify(result2);
          }
        ),
        { numRuns: 100 }
      );
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * 属性 74.15: 字段匹配应是确定性的
     * 对于相同的模板和字段名，applyTemplateToField 应始终返回相同的结果
     */
    it('Property 74.15: Field matching should be deterministic', () => {
      fc.assert(
        fc.property(
          presetTemplateArb,
          fc.oneof(financeFieldNameArb, salesFieldNameArb, inventoryFieldNameArb, kpiFieldNameArb),
          (template, fieldName) => {
            const result1 = applyTemplateToField(template, fieldName);
            const result2 = applyTemplateToField(template, fieldName);
            
            // 两次调用结果应完全相同
            return JSON.stringify(result1) === JSON.stringify(result2);
          }
        ),
        { numRuns: 100 }
      );
    });
  });

  // ==========================================================================
  // 预设模板特定测试
  // ==========================================================================
  
  describe('Preset Template Specific Tests', () => {
    
    /**
     * **Validates: Requirements 21.3**
     * 
     * 财务模板应正确配置负数红色规则
     */
    it('FINANCE_TEMPLATE should have negative number red rule', () => {
      const negativeRule = FINANCE_TEMPLATE.conditionalRules.find(
        r => r.name === '负数红色'
      );
      
      expect(negativeRule).toBeDefined();
      expect(negativeRule?.enabled).toBe(true);
      expect(negativeRule?.condition.type).toBe('value');
      expect(negativeRule?.style.font?.color).toBe('#ff4d4f');
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * 销售模板应正确配置业绩达标规则
     */
    it('SALES_TEMPLATE should have achievement rate rules', () => {
      const achievementRules = SALES_TEMPLATE.conditionalRules.filter(
        r => r.name.includes('业绩')
      );
      
      expect(achievementRules.length).toBeGreaterThan(0);
      
      // 应有达标、接近达标、未达标三种规则
      const achieved = achievementRules.find(r => r.name === '业绩达标');
      const nearAchieved = achievementRules.find(r => r.name === '业绩接近达标');
      const notAchieved = achievementRules.find(r => r.name === '业绩未达标');
      
      expect(achieved).toBeDefined();
      expect(nearAchieved).toBeDefined();
      expect(notAchieved).toBeDefined();
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * 库存模板应正确配置库存预警规则
     */
    it('INVENTORY_TEMPLATE should have stock alert rules', () => {
      const stockRules = INVENTORY_TEMPLATE.conditionalRules.filter(
        r => r.name.includes('库存')
      );
      
      expect(stockRules.length).toBeGreaterThan(0);
      
      // 应有危险、预警、正常三种规则
      const danger = stockRules.find(r => r.name === '库存危险');
      const warning = stockRules.find(r => r.name === '库存预警');
      const normal = stockRules.find(r => r.name === '库存正常');
      
      expect(danger).toBeDefined();
      expect(warning).toBeDefined();
      expect(normal).toBeDefined();
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * KPI模板应正确配置达成率色阶规则
     */
    it('KPI_TEMPLATE should have achievement rate color scale rules', () => {
      const achievementRules = KPI_TEMPLATE.conditionalRules.filter(
        r => r.name.includes('达成率')
      );
      
      expect(achievementRules.length).toBeGreaterThanOrEqual(5);
      
      // 应有优秀、良好、一般、较差、危险五种规则
      const excellent = achievementRules.find(r => r.name === '达成率优秀');
      const good = achievementRules.find(r => r.name === '达成率良好');
      const average = achievementRules.find(r => r.name === '达成率一般');
      const poor = achievementRules.find(r => r.name === '达成率较差');
      const critical = achievementRules.find(r => r.name === '达成率危险');
      
      expect(excellent).toBeDefined();
      expect(good).toBeDefined();
      expect(average).toBeDefined();
      expect(poor).toBeDefined();
      expect(critical).toBeDefined();
    });
  });

  // ==========================================================================
  // 模板注册表测试
  // ==========================================================================
  
  describe('Template Registry Tests', () => {
    
    /**
     * **Validates: Requirements 21.3**
     * 
     * getAllPresetTemplates 应返回所有预设模板
     */
    it('getAllPresetTemplates should return all preset templates', () => {
      const templates = getAllPresetTemplates();
      
      expect(templates.length).toBe(4);
      expect(templates.find(t => t.id === 'finance-standard')).toBeDefined();
      expect(templates.find(t => t.id === 'sales-dashboard')).toBeDefined();
      expect(templates.find(t => t.id === 'inventory-alert')).toBeDefined();
      expect(templates.find(t => t.id === 'kpi-dashboard')).toBeDefined();
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * getPresetTemplateById 应正确返回指定模板
     */
    it('getPresetTemplateById should return correct template', () => {
      fc.assert(
        fc.property(
          presetTemplateArb,
          (template) => {
            const found = getPresetTemplateById(template.id);
            
            if (!found) return false;
            
            return found.id === template.id && found.name === template.name;
          }
        ),
        { numRuns: 100 }
      );
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * getPresetTemplateById 对不存在的ID应返回 undefined
     */
    it('getPresetTemplateById should return undefined for non-existent id', () => {
      const result = getPresetTemplateById('non-existent-template');
      expect(result).toBeUndefined();
    });

    /**
     * **Validates: Requirements 21.3**
     * 
     * getPresetTemplatesByCategory 应正确过滤模板
     */
    it('getPresetTemplatesByCategory should filter templates correctly', () => {
      const financeTemplates = getPresetTemplatesByCategory('finance');
      expect(financeTemplates.length).toBe(1);
      expect(financeTemplates[0].id).toBe('finance-standard');
      
      const salesTemplates = getPresetTemplatesByCategory('sales');
      expect(salesTemplates.length).toBe(1);
      expect(salesTemplates[0].id).toBe('sales-dashboard');
      
      const inventoryTemplates = getPresetTemplatesByCategory('inventory');
      expect(inventoryTemplates.length).toBe(1);
      expect(inventoryTemplates[0].id).toBe('inventory-alert');
      
      const kpiTemplates = getPresetTemplatesByCategory('kpi');
      expect(kpiTemplates.length).toBe(1);
      expect(kpiTemplates[0].id).toBe('kpi-dashboard');
      
      // 自定义分类应返回空数组（预设模板中没有自定义模板）
      const customTemplates = getPresetTemplatesByCategory('custom');
      expect(customTemplates.length).toBe(0);
    });
  });
});

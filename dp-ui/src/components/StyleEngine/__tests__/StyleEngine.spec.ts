/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * StyleEngine 单元测试
 * StyleEngine Unit Tests
 * 
 * 测试样式继承机制和合并算法
 * Tests for style inheritance mechanism and merge algorithm
 * 
 * 需求: 21.1.1, 21.1.2
 */

import { describe, it, expect, beforeEach } from 'vitest';
import {
  StyleEngine,
  createStyleEngine,
  mergeStyles,
  applyStyleInheritance,
  cellStyleToCss,
  getPresetStyleTemplate,
  PRESET_STYLE_TEMPLATES,
} from '../StyleEngine';
import type { CellStyle, StyleConfig } from '../StyleEngine';

describe('StyleEngine', () => {
  describe('mergeStyles', () => {
    it('should return empty object when no styles provided', () => {
      const result = mergeStyles();
      expect(result).toEqual({});
    });

    it('should return single style unchanged', () => {
      const style: CellStyle = {
        font: { color: '#ff0000', size: 14 },
      };
      const result = mergeStyles(style);
      expect(result).toEqual(style);
    });

    it('should merge font styles with later values overriding', () => {
      const style1: CellStyle = { font: { color: '#ff0000', size: 12 } };
      const style2: CellStyle = { font: { color: '#00ff00' } };
      
      const result = mergeStyles(style1, style2);
      
      expect(result.font?.color).toBe('#00ff00');
      expect(result.font?.size).toBe(12);
    });

    it('should merge alignment styles', () => {
      const style1: CellStyle = { alignment: { horizontal: 'left', vertical: 'top' } };
      const style2: CellStyle = { alignment: { horizontal: 'center' } };
      
      const result = mergeStyles(style1, style2);
      
      expect(result.alignment?.horizontal).toBe('center');
      expect(result.alignment?.vertical).toBe('top');
    });

    it('should merge border styles including all shorthand', () => {
      const style1: CellStyle = {
        border: { all: { style: 'solid', width: 1, color: '#000' } },
      };
      const style2: CellStyle = {
        border: { top: { style: 'dashed', width: 2, color: '#f00' } },
      };
      
      const result = mergeStyles(style1, style2);
      
      expect(result.border?.top?.style).toBe('dashed');
      expect(result.border?.top?.width).toBe(2);
      expect(result.border?.right?.style).toBe('solid');
    });

    it('should override background style completely', () => {
      const style1: CellStyle = {
        background: { type: 'solid', color: '#fff' },
      };
      const style2: CellStyle = {
        background: { type: 'gradient', gradient: { type: 'linear', colors: [] } },
      };
      
      const result = mergeStyles(style1, style2);
      
      expect(result.background?.type).toBe('gradient');
    });

    it('should merge padding styles', () => {
      const style1: CellStyle = { padding: { top: 10, left: 5 } };
      const style2: CellStyle = { padding: { top: 20, right: 15 } };
      
      const result = mergeStyles(style1, style2);
      
      expect(result.padding?.top).toBe(20);
      expect(result.padding?.left).toBe(5);
      expect(result.padding?.right).toBe(15);
    });

    it('should merge data format with same type', () => {
      const style1: CellStyle = {
        format: { type: 'number', config: { decimalPlaces: 2, useThousandsSeparator: true } },
      };
      const style2: CellStyle = {
        format: { type: 'number', config: { decimalPlaces: 4 } },
      };
      
      const result = mergeStyles(style1, style2);
      
      expect(result.format?.type).toBe('number');
      expect((result.format?.config as any).decimalPlaces).toBe(4);
      expect((result.format?.config as any).useThousandsSeparator).toBe(true);
    });

    it('should override data format with different type', () => {
      const style1: CellStyle = {
        format: { type: 'number', config: { decimalPlaces: 2 } },
      };
      const style2: CellStyle = {
        format: { type: 'date', config: { pattern: 'YYYY-MM-DD' } },
      };
      
      const result = mergeStyles(style1, style2);
      
      expect(result.format?.type).toBe('date');
      expect((result.format?.config as any).pattern).toBe('YYYY-MM-DD');
    });

    it('should handle undefined styles in merge', () => {
      const style1: CellStyle = { font: { color: '#ff0000' } };
      
      const result = mergeStyles(style1, undefined, { alignment: { horizontal: 'center' } });
      
      expect(result.font?.color).toBe('#ff0000');
      expect(result.alignment?.horizontal).toBe('center');
    });
  });

  describe('applyStyleInheritance', () => {
    it('should apply inheritance: global → column → row → cell', () => {
      const globalStyle: CellStyle = {
        font: { family: 'Arial', size: 12, color: '#000' },
        alignment: { horizontal: 'left' },
      };
      const columnStyle: CellStyle = {
        font: { size: 14 },
        alignment: { horizontal: 'right' },
      };
      const rowStyle: CellStyle = {
        background: { type: 'solid', color: '#f0f0f0' },
      };
      const cellStyle: CellStyle = {
        font: { weight: 'bold' },
      };

      const result = applyStyleInheritance(globalStyle, columnStyle, rowStyle, cellStyle);

      // Global font family preserved
      expect(result.font?.family).toBe('Arial');
      // Column font size overrides global
      expect(result.font?.size).toBe(14);
      // Global font color preserved
      expect(result.font?.color).toBe('#000');
      // Cell font weight added
      expect(result.font?.weight).toBe('bold');
      // Column alignment overrides global
      expect(result.alignment?.horizontal).toBe('right');
      // Row background added
      expect(result.background?.color).toBe('#f0f0f0');
    });

    it('should handle missing intermediate styles', () => {
      const globalStyle: CellStyle = { font: { size: 12 } };
      const cellStyle: CellStyle = { font: { color: '#f00' } };

      const result = applyStyleInheritance(globalStyle, undefined, undefined, cellStyle);

      expect(result.font?.size).toBe(12);
      expect(result.font?.color).toBe('#f00');
    });
  });

  describe('StyleEngine class', () => {
    let engine: StyleEngine;

    beforeEach(() => {
      engine = createStyleEngine();
    });

    it('should create engine with default options', () => {
      expect(engine).toBeInstanceOf(StyleEngine);
      expect(engine.getCacheStats().maxSize).toBe(10000);
    });

    it('should set and apply global style', () => {
      engine.setGlobalStyle({ font: { size: 14 } });

      const result = engine.computeCellStyle({
        rowIndex: 0,
        columnKey: 'name',
        value: 'test',
        rowData: { name: 'test' },
      });

      expect(result.font?.size).toBe(14);
    });

    it('should set and apply column style', () => {
      engine.setGlobalStyle({ font: { size: 12 } });
      engine.setColumnStyle('amount', { font: { size: 16 }, alignment: { horizontal: 'right' } });

      const result = engine.computeCellStyle({
        rowIndex: 0,
        columnKey: 'amount',
        value: 100,
        rowData: { amount: 100 },
      });

      expect(result.font?.size).toBe(16);
      expect(result.alignment?.horizontal).toBe('right');
    });

    it('should apply zebra stripe styles', () => {
      engine.setRowStyles({
        evenRowStyle: { background: { type: 'solid', color: '#fff' } },
        oddRowStyle: { background: { type: 'solid', color: '#f5f5f5' } },
      });

      const evenResult = engine.computeCellStyle({
        rowIndex: 0,
        columnKey: 'name',
        value: 'test',
        rowData: {},
      });
      const oddResult = engine.computeCellStyle({
        rowIndex: 1,
        columnKey: 'name',
        value: 'test',
        rowData: {},
      });

      expect(evenResult.background?.color).toBe('#fff');
      expect(oddResult.background?.color).toBe('#f5f5f5');
    });

    it('should apply header row style', () => {
      engine.setRowStyles({
        headerStyle: { font: { weight: 'bold' }, background: { type: 'solid', color: '#1890ff' } },
      });

      const result = engine.computeCellStyle({
        rowIndex: 0,
        columnKey: 'name',
        value: 'Name',
        rowData: {},
        rowType: 'header',
      });

      expect(result.font?.weight).toBe('bold');
      expect(result.background?.color).toBe('#1890ff');
    });

    it('should apply summary row style', () => {
      engine.setRowStyles({
        summaryStyle: { font: { weight: 'bold' }, background: { type: 'solid', color: '#f0f0f0' } },
      });

      const result = engine.computeCellStyle({
        rowIndex: 10,
        columnKey: 'total',
        value: 1000,
        rowData: {},
        rowType: 'summary',
      });

      expect(result.font?.weight).toBe('bold');
      expect(result.background?.color).toBe('#f0f0f0');
    });

    it('should apply specific row style', () => {
      engine.setRowStyles({
        specificRows: {
          5: { background: { type: 'solid', color: '#ffe58f' } },
        },
      });

      const result = engine.computeCellStyle({
        rowIndex: 5,
        columnKey: 'name',
        value: 'test',
        rowData: {},
      });

      expect(result.background?.color).toBe('#ffe58f');
    });

    it('should apply cell-specific style', () => {
      engine.setGlobalStyle({ font: { size: 12 } });
      engine.setCellStyle(2, 'status', { font: { color: '#52c41a' } });

      const result = engine.computeCellStyle({
        rowIndex: 2,
        columnKey: 'status',
        value: 'active',
        rowData: {},
      });

      expect(result.font?.size).toBe(12);
      expect(result.font?.color).toBe('#52c41a');
    });

    it('should integrate with conditional formatting', () => {
      engine.setConditionalRules([
        {
          id: 'negative-red',
          name: 'Negative Red',
          priority: 1,
          enabled: true,
          condition: { type: 'value', config: { operator: 'lt', value: 0 } },
          style: { font: { color: '#ff4d4f' } },
        },
      ]);

      const negativeResult = engine.computeCellStyle({
        rowIndex: 0,
        columnKey: 'amount',
        value: -100,
        rowData: { amount: -100 },
      });
      const positiveResult = engine.computeCellStyle({
        rowIndex: 1,
        columnKey: 'amount',
        value: 100,
        rowData: { amount: 100 },
      });

      expect(negativeResult.font?.color).toBe('#ff4d4f');
      expect(positiveResult.font?.color).toBeUndefined();
    });

    it('should batch compute cell styles', () => {
      engine.setGlobalStyle({ font: { size: 12 } });
      engine.setColumnStyle('amount', { alignment: { horizontal: 'right' } });

      const cells = [
        { rowIndex: 0, columnKey: 'name', value: 'Alice', rowData: { name: 'Alice', amount: 100 } },
        { rowIndex: 0, columnKey: 'amount', value: 100, rowData: { name: 'Alice', amount: 100 } },
        { rowIndex: 1, columnKey: 'name', value: 'Bob', rowData: { name: 'Bob', amount: 200 } },
        { rowIndex: 1, columnKey: 'amount', value: 200, rowData: { name: 'Bob', amount: 200 } },
      ];

      const results = engine.computeCellStyles(cells);

      expect(results.size).toBe(4);
      expect(results.get('0:amount')?.alignment?.horizontal).toBe('right');
      expect(results.get('1:name')?.font?.size).toBe(12);
    });

    it('should clear cache when config changes', () => {
      // Disable conditional formatting to enable caching
      const engineWithCache = createStyleEngine({}, { enableConditionalFormat: false });
      engineWithCache.setGlobalStyle({ font: { size: 12 } });
      
      // Compute to populate cache
      engineWithCache.computeCellStyle({
        rowIndex: 0,
        columnKey: 'name',
        value: 'test',
        rowData: {},
      });
      
      expect(engineWithCache.getCacheStats().size).toBeGreaterThan(0);
      
      // Update config should clear cache
      engineWithCache.setGlobalStyle({ font: { size: 14 } });
      
      expect(engineWithCache.getCacheStats().size).toBe(0);
    });

    it('should return effective config', () => {
      const config: StyleConfig = {
        globalStyle: { font: { size: 12 } },
        columnStyles: { amount: { alignment: { horizontal: 'right' } } },
      };
      
      engine.setConfig(config);
      const effectiveConfig = engine.getEffectiveConfig();
      
      expect(effectiveConfig.globalStyle?.font?.size).toBe(12);
      expect(effectiveConfig.columnStyles?.amount?.alignment?.horizontal).toBe('right');
    });
  });

  describe('cellStyleToCss', () => {
    it('should convert font style to CSS', () => {
      const style: CellStyle = {
        font: {
          family: 'Arial',
          size: 14,
          weight: 'bold',
          style: 'italic',
          decoration: 'underline',
          color: '#ff0000',
        },
      };

      const css = cellStyleToCss(style);

      expect(css.fontFamily).toBe('Arial');
      expect(css.fontSize).toBe('14px');
      expect(css.fontWeight).toBe('bold');
      expect(css.fontStyle).toBe('italic');
      expect(css.textDecoration).toBe('underline');
      expect(css.color).toBe('#ff0000');
    });

    it('should convert alignment style to CSS', () => {
      const style: CellStyle = {
        alignment: {
          horizontal: 'center',
          vertical: 'middle',
          textDirection: 'rtl',
          indent: 2,
          wrapText: true,
          rotation: 45,
        },
      };

      const css = cellStyleToCss(style);

      expect(css.textAlign).toBe('center');
      expect(css.verticalAlign).toBe('middle');
      expect(css.direction).toBe('rtl');
      expect(css.paddingLeft).toBe('32px');
      expect(css.whiteSpace).toBe('pre-wrap');
      expect(css.transform).toBe('rotate(45deg)');
    });

    it('should convert border style to CSS', () => {
      const style: CellStyle = {
        border: {
          top: { style: 'solid', width: 2, color: '#000' },
          right: { style: 'dashed', width: 1, color: '#ccc' },
        },
      };

      const css = cellStyleToCss(style);

      expect(css.borderTop).toBe('2px solid #000');
      expect(css.borderRight).toBe('1px dashed #ccc');
    });

    it('should convert solid background to CSS', () => {
      const style: CellStyle = {
        background: { type: 'solid', color: '#f0f0f0' },
      };

      const css = cellStyleToCss(style);

      expect(css.backgroundColor).toBe('#f0f0f0');
    });

    it('should convert gradient background to CSS', () => {
      const style: CellStyle = {
        background: {
          type: 'gradient',
          gradient: {
            type: 'linear',
            angle: 90,
            colors: [
              { offset: 0, color: '#fff' },
              { offset: 1, color: '#000' },
            ],
          },
        },
      };

      const css = cellStyleToCss(style);

      expect(css.background).toContain('linear-gradient');
      expect(css.background).toContain('90deg');
    });

    it('should convert padding to CSS', () => {
      const style: CellStyle = {
        padding: { top: 10, right: 20, bottom: 10, left: 20 },
      };

      const css = cellStyleToCss(style);

      expect(css.padding).toBe('10px 20px 10px 20px');
    });
  });

  describe('Preset Style Templates', () => {
    it('should have finance template', () => {
      const template = getPresetStyleTemplate('finance');
      
      expect(template.globalStyle?.font?.family).toBe('Arial');
      expect(template.rowStyles?.headerStyle?.background?.color).toBe('#1890ff');
    });

    it('should have sales template', () => {
      const template = getPresetStyleTemplate('sales');
      
      expect(template.globalStyle?.font?.family).toBe('Microsoft YaHei');
    });

    it('should have inventory template', () => {
      const template = getPresetStyleTemplate('inventory');
      
      expect(template.rowStyles?.headerStyle?.background?.color).toBe('#e6f7ff');
    });

    it('should have kpi template', () => {
      const template = getPresetStyleTemplate('kpi');
      
      expect(template.globalStyle?.alignment?.horizontal).toBe('center');
      expect(template.rowStyles?.headerStyle?.background?.color).toBe('#722ed1');
    });

    it('should apply preset template to engine', () => {
      const engine = createStyleEngine(getPresetStyleTemplate('finance'));
      
      const headerResult = engine.computeCellStyle({
        rowIndex: 0,
        columnKey: 'amount',
        value: 1000,
        rowData: {},
        rowType: 'header',
      });
      
      expect(headerResult.font?.weight).toBe('bold');
      expect(headerResult.background?.color).toBe('#1890ff');
    });
  });
});

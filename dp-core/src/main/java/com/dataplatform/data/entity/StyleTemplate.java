package com.dataplatform.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 样式模板实体类
 * 用于存储预设和用户自定义的样式模板（需求 21.1.3）
 * 支持样式模板的保存和复用（需求 21.3）
 * 
 * @author dataplatform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("style_template")
public class StyleTemplate {
    
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 模板名称 */
    private String name;
    
    /** 分类：finance-财务报表, sales-销售报表, inventory-库存报表, kpi-KPI仪表盘, custom-自定义 */
    private String category;
    
    /** 模板描述 */
    private String description;
    
    /** 是否系统预设模板：1-是，0-否 */
    private Boolean isSystem;
    
    /**
     * 列样式配置JSON
     * 包含各列的默认样式、表头样式、汇总行样式等
     * 格式示例：
     * {
     *   "amount|money|price": {
     *     "fieldPattern": "amount|money|price",
     *     "fieldType": "number",
     *     "defaultStyle": {...},
     *     "headerStyle": {...},
     *     "summaryStyle": {...}
     *   }
     * }
     */
    private String columnStyles;
    
    /**
     * 条件规则JSON
     * 包含条件格式化规则列表
     * 格式示例：
     * [
     *   {
     *     "id": "negative-red",
     *     "name": "负数红色",
     *     "priority": 1,
     *     "enabled": true,
     *     "condition": {...},
     *     "style": {...}
     *   }
     * ]
     */
    private String conditionalRules;
    
    /**
     * 表格样式配置JSON
     * 包含表头样式、表体样式、斑马纹、边框等
     * 格式示例：
     * {
     *   "headerStyle": {...},
     *   "bodyStyle": {...},
     *   "alternateRowStyle": {...},
     *   "summaryRowStyle": {...},
     *   "borderStyle": "all",
     *   "borderColor": "#e8e8e8"
     * }
     */
    private String tableStyle;
    
    /** 预览图URL */
    private String previewImage;
    
    /** 创建者ID，系统模板为null */
    private Long createdBy;
    
    /** 使用次数统计 */
    private Integer useCount;
    
    /** 状态：1-启用，0-禁用 */
    private Integer status;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}

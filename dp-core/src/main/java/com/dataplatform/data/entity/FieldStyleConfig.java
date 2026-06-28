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
 * 字段样式配置实体类
 * 用于存储报表字段的样式配置，支持样式持久化（需求 21.1.3）
 * 支持从数据库加载样式配置（需求 21.1.4）
 * 
 * @author dataplatform
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("field_style_config")
public class FieldStyleConfig {
    
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 关联报表ID */
    private Long reportId;
    
    /** 字段名 */
    private String fieldName;
    
    /**
     * 样式配置JSON
     * 包含字体、对齐、边框、背景、内边距、数据格式等配置
     * 格式示例：
     * {
     *   "font": {"family": "Arial", "size": 12, "weight": "bold", "color": "#333"},
     *   "alignment": {"horizontal": "center", "vertical": "middle"},
     *   "border": {"all": {"style": "solid", "width": 1, "color": "#e8e8e8"}},
     *   "background": {"type": "solid", "color": "#ffffff"},
     *   "padding": {"top": 4, "right": 8, "bottom": 4, "left": 8},
     *   "format": {"type": "number", "config": {"decimalPlaces": 2, "useThousandsSeparator": true}}
     * }
     */
    private String styleConfig;
    
    /**
     * 条件规则JSON
     * 包含条件格式化规则列表
     * 格式示例：
     * [
     *   {
     *     "id": "rule1",
     *     "name": "负数红色",
     *     "priority": 1,
     *     "enabled": true,
     *     "condition": {"type": "value", "config": {"operator": "lt", "value": 0}},
     *     "style": {"font": {"color": "#ff4d4f"}}
     *   }
     * ]
     */
    private String conditionalRules;
    
    /** 排序顺序 */
    private Integer sortOrder;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
    
    /** 创建人ID */
    private Long createdBy;
}

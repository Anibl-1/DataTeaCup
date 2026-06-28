package com.dataplatform.data.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报表模板实体类
 * 用于存储预设和用户自定义的报表模板
 * 
 * @author dataplatform
 */
@Data
@TableName("report_template")
public class ReportTemplate {
    
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 模板名称 */
    private String name;
    
    /** 分类：sales-销售报表, finance-财务报表, operation-运营报表, inventory-库存报表, hr-人员报表, custom-自定义 */
    private String category;
    
    /** 模板描述 */
    private String description;
    
    /** SQL模板，支持参数占位符 ${paramName} */
    private String sqlTemplate;
    
    /** 字段配置JSON，包含字段名、显示名、类型、格式化等配置 */
    private String fieldsConfig;
    
    /** 参数配置JSON，包含参数名、类型、默认值、选项等配置 */
    private String paramsConfig;
    
    /** 预览图URL */
    private String previewImage;
    
    /** 是否系统预设模板：1-是，0-否 */
    private Boolean isSystem;
    
    /** 创建者ID，系统模板为null */
    private Long creatorId;
    
    /** 使用次数统计 */
    private Integer useCount;
    
    /** 状态：1-启用，0-禁用 */
    private Integer status;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
}

package com.dataplatform.data.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订阅计划实体
 * 
 * 需求 8.1, 8.3: 订阅计划管理
 *
 * @author dataplatform
 */
@Data
@TableName("subscription_plan")
public class SubscriptionPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 计划名称 */
    private String planName;

    /** 计划编码 */
    private String planCode;

    /** 计划描述 */
    private String description;

    /** 计划类型：FREE-免费，BASIC-基础，PRO-专业，ENTERPRISE-企业 */
    private String planType;

    /** 月价格 */
    private BigDecimal monthlyPrice;

    /** 年价格 */
    private BigDecimal yearlyPrice;

    /** 最大用户数 */
    private Integer maxUsers;

    /** 最大数据源数 */
    private Integer maxDataSources;

    /** 最大存储空间(MB) */
    private Long maxStorageMb;

    /** 功能列表(JSON) */
    private String features;

    /** 配额配置(JSON) */
    private String quotaConfig;

    /** 排序 */
    private Integer sortOrder;

    /** 是否启用 */
    private Boolean enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

package com.dataplatform.data.entity;

import lombok.Data;
import java.util.Date;

/**
 * 消息通道配置实体
 * 统一管理企业微信、钉钉、邮件等消息通道配置
 * 每种类型可以配置多个，在使用时选择具体配置
 */
@Data
public class MessageChannel {
    private Long id;
    
    /** 通道名称（用于显示和选择） */
    private String channelName;
    
    /** 通道类型: email/wecom/dingtalk/sms */
    private String channelType;
    
    /** 配置内容（JSON格式，不同类型有不同配置项） */
    private String config;
    
    /** 是否默认配置：1-是，0-否（每种类型只能有一个默认） */
    private Integer isDefault;
    
    /** 是否启用：1-启用，0-禁用 */
    private Integer status;
    
    /** 描述说明 */
    private String description;
    
    /** 创建人 */
    private Long createBy;
    
    private Date createTime;
    private Date updateTime;
    
    // ==================== 各类型配置字段说明 ====================
    // email: {"host":"smtp.example.com","port":465,"username":"xxx","password":"xxx","fromName":"系统通知","ssl":true}
    // wecom: {"corpId":"xxx","agentId":"xxx","secret":"xxx","webhookUrl":"xxx(可选)"}
    // dingtalk: {"webhookUrl":"xxx","secret":"xxx(可选)","agentId":"xxx(可选)","appKey":"xxx(可选)","appSecret":"xxx(可选)"}
    // sms: {"provider":"aliyun/tencent","accessKey":"xxx","secretKey":"xxx","signName":"xxx","templateId":"xxx"}
}

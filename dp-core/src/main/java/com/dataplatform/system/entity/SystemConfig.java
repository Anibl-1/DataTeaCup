package com.dataplatform.system.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统配置实体
 */
@Data
public class SystemConfig {

    private Long id;

    /** 配置键 */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 配置类型: string/number/boolean/json */
    private String configType;

    /** 描述 */
    private String configDesc;

    /** 配置分组 */
    private String configGroup;

    /** 是否系统配置 */
    private Boolean isSystem;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}

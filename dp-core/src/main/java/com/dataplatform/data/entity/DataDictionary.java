package com.dataplatform.data.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 数据字典实体
 */
@Data
public class DataDictionary {

    private Long id;

    /** 字典类型 */
    private String dictType;

    /** 字典编码 */
    private String dictCode;

    /** 字典标签 */
    private String dictLabel;

    /** 字典值 */
    private String dictValue;

    /** 排序 */
    private Integer sortOrder;

    /** 是否默认 */
    private Boolean isDefault;

    /** 状态: 0-禁用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}

package com.dataplatform.system.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 字典类型实体
 */
@Data
public class DictType {

    private Long id;

    /** 字典类型编码（唯一） */
    private String dictCode;

    /** 字典类型名称 */
    private String dictName;

    /** 状态: 0-禁用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}

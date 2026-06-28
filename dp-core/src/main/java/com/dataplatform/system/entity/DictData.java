package com.dataplatform.system.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 字典数据实体
 */
@Data
public class DictData {

    private Long id;

    /** 关联字典类型编码 */
    private String dictCode;

    /** 字典标签 */
    private String label;

    /** 字典值 */
    private String value;

    /** 排序号 */
    private Integer sortOrder;

    /** 样式类名 */
    private String cssClass;

    /** 状态: 0-禁用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}

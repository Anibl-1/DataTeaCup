package com.dataplatform.org.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 岗位实体
 */
@Data
public class Post {

    private Long id;

    /** 岗位编码（唯一） */
    private String postCode;

    /** 岗位名称 */
    private String postName;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态: 0-禁用 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}

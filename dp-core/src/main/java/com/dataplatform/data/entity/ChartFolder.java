package com.dataplatform.data.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 图表文件夹实体
 */
@Data
public class ChartFolder {
    private Long id;

    /** 文件夹名称 */
    private String folderName;

    /** 父文件夹ID */
    private Long parentId;

    /** 排序 */
    private Integer sortOrder;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 子文件夹（树结构用） */
    private transient List<ChartFolder> children;
}

package com.dataplatform.data.service.metadata;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 元数据条目
 */
@Data
public class MetadataEntry {
    private String id;
    private String dataSourceId;
    private String tableName;
    private String columnName;
    private String columnType;
    private boolean nullable;
    private String businessName; // 业务名称
    private String description; // 业务描述
    private String category; // 数据分类
    private Set<String> tags = new HashSet<>();
    private int version;
    private LocalDateTime collectedAt;
    private LocalDateTime updatedAt;
}

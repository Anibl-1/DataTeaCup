package com.dataplatform.data.service.quality;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 数据质量检查结果
 */
@Data
public class QualityCheckResult {
    private String id;
    private String ruleId;
    private String ruleName;
    private String ruleType;
    private String targetTable;
    private String targetColumn;
    private long totalRecords;
    private long violationCount;
    private double score; // 0-1, 合格率
    private boolean passed;
    private String severity;
    private List<Map<String, Object>> sampleViolations = new ArrayList<>();
    private LocalDateTime checkTime;
}

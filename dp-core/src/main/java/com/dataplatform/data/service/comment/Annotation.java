package com.dataplatform.data.service.comment;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 图表标注
 * 需求: 23.4, 23.5
 */
@Data
public class Annotation {
    private String id;
    private String chartId;
    private String userId;
    private String text;
    private double x; // 标注位置X坐标（百分比）
    private double y; // 标注位置Y坐标（百分比）
    private String color;
    private String type; // point, range, line
    private LocalDateTime createdAt;
}

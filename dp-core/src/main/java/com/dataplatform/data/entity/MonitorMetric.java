package com.dataplatform.data.entity;

import lombok.Data;
import java.util.Date;

/**
 * 监控指标历史记录实体
 * 存储系统运行时指标的时序数据
 */
@Data
public class MonitorMetric {
    private Long id;
    /** CPU使用率 */
    private Double cpuUsage;
    /** 内存使用率 */
    private Double memoryUsage;
    /** 磁盘使用率 */
    private Double diskUsage;
    /** JVM堆内存使用(MB) */
    private Long heapUsed;
    /** JVM堆内存最大(MB) */
    private Long heapMax;
    /** JVM线程数 */
    private Integer threadCount;
    /** GC次数 */
    private Long gcCount;
    /** 活跃数据库连接数 */
    private Integer activeConnections;
    /** 运行中任务数 */
    private Integer runningTasks;
    /** 采集时间 */
    private Date collectTime;
}

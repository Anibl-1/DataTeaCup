package com.dataplatform.data.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;
import java.util.List;

/**
 * 流程节点实体
 */
@Data
public class PipelineNode {
    private Long id;
    private Long pipelineId;
    private String nodeCode;
    private String nodeName;
    private String nodeType; // source/script/sink
    private String nodeConfig; // JSON配置
    private Integer positionX;
    private Integer positionY;
    private Integer sortOrder;
    
    // 前置任务依赖（逗号分隔的nodeCode）
    private String preTaskCodesStr;
    // 前端使用的数组形式
    private transient List<String> preTaskCodes;
    
    // 失败策略: 0-停止流程 1-继续执行
    private Integer failStrategy;
    
    // 超时设置
    private Integer timeoutFlag; // 0-否 1-是
    private Integer timeoutSeconds;
    private Integer timeoutStrategy; // 0-告警 1-失败
    
    // 重试机制
    private Integer retryTimes;
    private Integer retryInterval;
    
    // 优先级: 0-最高 1-高 2-中 3-低 4-最低
    private Integer priority;
    
    // 条件分支
    private Integer conditionType; // 0-无 1-成功分支 2-失败分支
    private String conditionResult;
    
    // 运行标记
    private Integer runFlag; // 0-禁止 1-正常
    private Integer isEnabled;
    
    // 描述
    private String description;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}

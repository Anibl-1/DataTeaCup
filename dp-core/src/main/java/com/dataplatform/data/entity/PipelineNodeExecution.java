package com.dataplatform.data.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

/**
 * 流程节点执行记录实体
 */
@Data
public class PipelineNodeExecution {
    private Long id;

    /** 流程执行ID */
    private Long executionId;

    /** 节点编码 */
    private String nodeCode;

    /** 节点名称 */
    private String nodeName;

    /** 状态: 0-失败, 1-成功, 2-运行中, 3-跳过 */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    /** 执行时长(毫秒) */
    private Long duration;

    /** 输入数据量 */
    private Long inputCount;

    /** 输出数据量 */
    private Long outputCount;

    /** 错误信息 */
    private String errorMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}

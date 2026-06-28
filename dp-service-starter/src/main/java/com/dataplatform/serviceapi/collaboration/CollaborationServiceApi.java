package com.dataplatform.serviceapi.collaboration;

import com.dataplatform.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * collaboration-service OpenFeign 接口
 *
 * <p>供其他微服务调用 collaboration-service 发送通知等。</p>
 * <p>设计文档 6.2 节异步事件：</p>
 * <ul>
 *   <li>monitor.alert.triggered → collaboration-service: 发送告警通知</li>
 *   <li>analytics.export.finished → collaboration-service: 发送导出完成通知</li>
 * </ul>
 * <p>注：一期使用同步调用，后续迁移到 RocketMQ 事件驱动。</p>
 */
@FeignClient(name = "dp-collaboration-service", contextId = "collaborationServiceApi", path = "/")
public interface CollaborationServiceApi {

    @PostMapping("/notification/send")
    Result<String> sendNotification(@RequestBody Map<String, Object> notification);
}

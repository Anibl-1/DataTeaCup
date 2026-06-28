package com.dataplatform.analytics.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.service.AiStreamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * SSE流式AI接口控制器
 * 提供基于Server-Sent Events协议的流式AI响应
 * 
 * 验收标准:
 * - 1.1: SSE协议逐token返回AI响应
 * - 1.3: 支持中断生成
 * - 1.5: 流式SQL生成
 * - 1.6: 流式图表生成
 * - 1.8: 心跳保活（由AiStreamService处理）
 */
@Slf4j
@RestController
@RequestMapping("/ai/stream")
@RequirePermission("ai:use")
public class SseController {

    @Autowired
    private AiStreamService aiStreamService;

    /**
     * 流式对话
     * 通过SSE协议逐token返回AI对话响应
     * 
     * @param message 用户消息
     * @param context 上下文信息（可选）
     * @param sessionId 会话ID（可选，用于保持对话上下文）
     * @return SseEmitter 流式响应
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestParam String message,
            @RequestParam(required = false) String context,
            @RequestParam(required = false) String sessionId) {
        
        log.info("SSE流式对话请求: sessionId={}, message={}", 
            sessionId, message.substring(0, Math.min(50, message.length())));
        
        if (message == null || message.trim().isEmpty()) {
            return createErrorEmitter("消息内容不能为空");
        }
        
        return aiStreamService.streamChat(message, context, sessionId);
    }

    /**
     * 流式SQL生成
     * 通过SSE协议逐token返回生成的SQL语句
     * 
     * @param naturalLanguage 自然语言描述
     * @param dataSourceId 数据源ID
     * @param tableName 表名（可选，用于提供表结构上下文）
     * @return SseEmitter 流式响应
     */
    @GetMapping(value = "/sql", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSqlGenerate(
            @RequestParam String naturalLanguage,
            @RequestParam Long dataSourceId,
            @RequestParam(required = false) String tableName) {
        
        log.info("SSE流式SQL生成请求: dataSourceId={}, tableName={}, query={}", 
            dataSourceId, tableName, naturalLanguage.substring(0, Math.min(50, naturalLanguage.length())));
        
        if (naturalLanguage == null || naturalLanguage.trim().isEmpty()) {
            return createErrorEmitter("自然语言描述不能为空");
        }
        
        if (dataSourceId == null) {
            return createErrorEmitter("数据源ID不能为空");
        }
        
        return aiStreamService.streamSqlGenerate(naturalLanguage, dataSourceId, tableName);
    }

    /**
     * 流式图表生成
     * 通过SSE协议逐token返回ECharts图表配置JSON
     * 
     * @param requirement 图表需求描述
     * @param dataSourceId 数据源ID
     * @return SseEmitter 流式响应
     */
    @GetMapping(value = "/chart", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChartGenerate(
            @RequestParam String requirement,
            @RequestParam Long dataSourceId) {
        
        log.info("SSE流式图表生成请求: dataSourceId={}, requirement={}", 
            dataSourceId, requirement.substring(0, Math.min(50, requirement.length())));
        
        if (requirement == null || requirement.trim().isEmpty()) {
            return createErrorEmitter("图表需求描述不能为空");
        }
        
        if (dataSourceId == null) {
            return createErrorEmitter("数据源ID不能为空");
        }
        
        // 构建上下文信息
        Map<String, Object> context = Map.of(
            "dataSourceId", dataSourceId
        );
        
        return aiStreamService.streamChartGenerate(requirement, dataSourceId, context);
    }

    /**
     * 中断流式生成
     * 立即终止指定的流式生成任务并释放资源
     * 
     * @param streamId 流ID（从SSE start事件中获取）
     * @return 操作结果
     */
    @PostMapping("/abort/{streamId}")
    public ResponseEntity<Result<Void>> abortStream(@PathVariable String streamId) {
        log.info("中断流式生成请求: streamId={}", streamId);
        
        if (streamId == null || streamId.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Result.error("streamId不能为空"));
        }
        
        try {
            aiStreamService.abortStream(streamId);
            return ResponseEntity.ok(Result.success(null));
        } catch (Exception e) {
            log.error("中断流式生成失败: streamId={}, error={}", streamId, e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Result.error("中断失败: " + e.getMessage()));
        }
    }

    /**
     * 获取活跃流数量（用于监控）
     * 
     * @return 当前活跃的SSE连接数
     */
    @GetMapping("/active-count")
    public ResponseEntity<Result<Map<String, Object>>> getActiveStreamCount() {
        int count = aiStreamService.getActiveStreamCount();
        return ResponseEntity.ok(Result.success(Map.of("activeStreams", count)));
    }

    /**
     * 创建错误响应的SSE发射器
     */
    private SseEmitter createErrorEmitter(String errorMessage) {
        SseEmitter emitter = new SseEmitter(0L);
        try {
            emitter.send(SseEmitter.event()
                .name("error")
                .data("{\"error\":\"" + escapeJson(errorMessage) + "\"}"));
            emitter.complete();
        } catch (Exception e) {
            log.error("发送错误事件失败: {}", e.getMessage());
        }
        return emitter;
    }

    /**
     * 转义JSON字符串中的特殊字符
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}

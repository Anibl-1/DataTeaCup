package com.dataplatform.data.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * AI流式服务 - 已废弃
 * 流式功能已移除，所有AI调用统一使用非流式接口
 * 保留此类仅为兼容已有引用，所有方法返回错误提示
 */
@Slf4j
@Service
@Deprecated
public class AiStreamService {

    private static final String DEPRECATED_MSG = "流式功能已移除，请使用非流式AI接口";

    public SseEmitter streamChat(String message, String context, String sessionId) {
        return createErrorEmitter(DEPRECATED_MSG);
    }

    public SseEmitter streamSqlGenerate(String naturalLanguage, Long dataSourceId, String tableName) {
        return createErrorEmitter(DEPRECATED_MSG);
    }

    public SseEmitter streamChartGenerate(String requirement, Long dataSourceId, Map<String, Object> context) {
        return createErrorEmitter(DEPRECATED_MSG);
    }

    public void abortStream(String streamId) {
        log.debug("abortStream called on deprecated AiStreamService: {}", streamId);
    }

    public int getActiveStreamCount() {
        return 0;
    }

    private SseEmitter createErrorEmitter(String errorMessage) {
        SseEmitter emitter = new SseEmitter(0L);
        try {
            emitter.send(SseEmitter.event()
                .name("error")
                .data("{\"error\":\"" + errorMessage + "\"}"));
            emitter.complete();
        } catch (IOException e) {
            log.debug("发送错误事件失败: {}", e.getMessage());
        }
        return emitter;
    }
}

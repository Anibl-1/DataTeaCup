package com.dataplatform.data.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 传输引擎选择器
 * 根据任务配置的 engine_type 自动选择对应引擎
 */
@Slf4j
@Component
public class TransferEngineSelector {

    private final Map<String, TransferEngine> engineMap = new ConcurrentHashMap<>();

    public TransferEngineSelector(List<TransferEngine> engines) {
        for (TransferEngine engine : engines) {
            engineMap.put(engine.getType(), engine);
            log.info("[引擎注册] {} -> {}", engine.getType(), engine.getClass().getSimpleName());
        }
    }

    /**
     * 根据引擎类型获取引擎实例
     *
     * @param engineType 引擎类型标识，null 或空则默认 jdbc
     * @return 引擎实例
     */
    public TransferEngine select(String engineType) {
        String type = (engineType == null || engineType.isBlank()) ? "jdbc" : engineType.toLowerCase();
        TransferEngine engine = engineMap.get(type);
        if (engine == null) {
            log.warn("[引擎选择] 不支持的引擎类型: {}，回退到 jdbc", type);
            engine = engineMap.get("jdbc");
        }
        if (engine == null) {
            throw new IllegalStateException("没有可用的传输引擎");
        }
        return engine;
    }

    /**
     * 获取所有已注册的引擎类型
     */
    public List<String> getAvailableEngines() {
        return List.copyOf(engineMap.keySet());
    }
}

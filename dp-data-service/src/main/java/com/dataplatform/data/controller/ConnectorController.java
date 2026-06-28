package com.dataplatform.data.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.service.connector.ConnectorConfig;
import com.dataplatform.data.service.connector.ConnectorManager;
import com.dataplatform.data.service.connector.ConnectorResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 鏁版嵁婧愯繛鎺ュ櫒API
 * 闇€姹? 17.5, 17.6
 */
@RestController
@RequestMapping("/connectors")
@RequiredArgsConstructor
@RequirePermission("data:source:read")
public class ConnectorController {

    private final ConnectorManager connectorManager;

    /**
     * 鑾峰彇鏀寔鐨勮繛鎺ュ櫒绫诲瀷
     */
    @GetMapping("/types")
    public Result<Set<String>> getSupportedTypes() {
        return Result.success(connectorManager.getSupportedTypes());
    }

    /**
     * 娴嬭瘯杩炴帴
     */
    @PostMapping("/test")
    public Result<Map<String, Object>> testConnection(@RequestBody ConnectorConfig config) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            boolean success = connectorManager.testConnection(config);
            result.put("success", success);
            result.put("message", success ? "杩炴帴鎴愬姛" : "杩炴帴澶辫触");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "杩炴帴寮傚父: " + e.getMessage());
        }
        return Result.success(result);
    }

    /**
     * 鑾峰彇琛?闆嗗悎鍒楄〃
     */
    @PostMapping("/tables")
    public Result<List<String>> listTables(@RequestBody ConnectorConfig config) {
        return Result.success(connectorManager.listTables(config));
    }

    /**
     * 鑾峰彇鍒椾俊鎭?
     */
    @PostMapping("/columns")
    public Result<List<ConnectorResult.ColumnMeta>> getColumns(
            @RequestBody ConnectorConfig config,
            @RequestParam String tableName) {
        return Result.success(connectorManager.getColumns(config, tableName));
    }
}

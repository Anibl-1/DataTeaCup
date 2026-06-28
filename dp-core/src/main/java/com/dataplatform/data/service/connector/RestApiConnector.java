package com.dataplatform.data.service.connector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * REST API数据源连接器
 * 支持GET/POST请求、认证配置、分页处理
 * 需求: 17.1
 */
@Slf4j
@Component
public class RestApiConnector implements DataConnector {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getType() {
        return "rest_api";
    }

    @Override
    public boolean testConnection(ConnectorConfig config) {
        try {
            String url = config.getProperty("url");
            HttpHeaders headers = buildHeaders(config);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("[REST连接器] 连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> listTables(ConnectorConfig config) {
        // REST API没有表概念，返回配置的端点列表
        String endpoints = config.getProperty("endpoints", "");
        if (endpoints.isEmpty()) return Collections.emptyList();
        return Arrays.asList(endpoints.split(","));
    }

    @Override
    public List<ConnectorResult.ColumnMeta> getColumns(ConnectorConfig config, String tableName) {
        // 通过请求一条数据推断列信息
        try {
            ConnectorResult result = readData(config, tableName, Map.of("limit", 1));
            return result.getColumns();
        } catch (Exception e) {
            log.warn("[REST连接器] 获取列信息失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConnectorResult readData(ConnectorConfig config, String endpoint, Map<String, Object> params) {
        String baseUrl = config.getProperty("url");
        String method = config.getProperty("method", "GET").toUpperCase();
        String url = baseUrl + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);

        HttpHeaders headers = buildHeaders(config);
        ConnectorResult result = new ConnectorResult();
        List<Map<String, Object>> allRows = new ArrayList<>();

        try {
            ResponseEntity<Object> response;
            if ("POST".equals(method)) {
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);
                response = restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);
            } else {
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                response = restTemplate.exchange(url, HttpMethod.GET, entity, Object.class);
            }

            Object body = response.getBody();
            if (body instanceof List) {
                for (Object item : (List<?>) body) {
                    if (item instanceof Map) {
                        allRows.add((Map<String, Object>) item);
                    }
                }
            } else if (body instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) body;
                // 尝试从常见的data字段提取列表
                Object data = map.getOrDefault("data", map.getOrDefault("items", map.getOrDefault("records", body)));
                if (data instanceof List) {
                    for (Object item : (List<?>) data) {
                        if (item instanceof Map) {
                            allRows.add((Map<String, Object>) item);
                        }
                    }
                } else {
                    allRows.add(map);
                }
            }

            result.setRows(allRows);
            result.setTotalRows(allRows.size());
            result.setHasMore(false);

            // 推断列信息
            if (!allRows.isEmpty()) {
                List<ConnectorResult.ColumnMeta> columns = new ArrayList<>();
                for (Map.Entry<String, Object> entry : allRows.get(0).entrySet()) {
                    String type = entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "String";
                    columns.add(new ConnectorResult.ColumnMeta(entry.getKey(), type, true));
                }
                result.setColumns(columns);
            }
        } catch (Exception e) {
            log.error("[REST连接器] 读取数据失败: {}", e.getMessage());
            result.setRows(Collections.emptyList());
            result.setTotalRows(0);
        }

        return result;
    }

    private HttpHeaders buildHeaders(ConnectorConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String authType = config.getProperty("authType", "none");
        switch (authType) {
            case "bearer":
                headers.setBearerAuth(config.getProperty("token", ""));
                break;
            case "basic":
                headers.setBasicAuth(
                        config.getProperty("username", ""),
                        config.getProperty("password", ""));
                break;
            case "apikey":
                headers.set(
                        config.getProperty("apiKeyHeader", "X-API-Key"),
                        config.getProperty("apiKey", ""));
                break;
        }

        // 自定义请求头
        String customHeaders = config.getProperty("customHeaders", "");
        if (!customHeaders.isEmpty()) {
            for (String header : customHeaders.split(";")) {
                String[] parts = header.split(":", 2);
                if (parts.length == 2) {
                    headers.set(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return headers;
    }
}

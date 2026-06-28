package com.dataplatform.data.service.connector;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源连接器配置
 * 需求: 17.1, 17.2, 17.3, 17.4
 */
@Data
public class ConnectorConfig {
    private String id;
    private String name;
    private String type; // rest_api, file, kafka, minio
    private Map<String, String> properties = new HashMap<>();

    public String getProperty(String key) {
        return properties.get(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }
}

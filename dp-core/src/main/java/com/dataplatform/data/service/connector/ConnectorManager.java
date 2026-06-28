package com.dataplatform.data.service.connector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 连接器管理服务
 * 统一管理所有数据源连接器，提供连接测试和元数据获取
 * 需求: 17.5, 17.6, 17.7
 */
@Slf4j
@Service
public class ConnectorManager {

    private final Map<String, DataConnector> connectorMap = new ConcurrentHashMap<>();

    public ConnectorManager(List<DataConnector> connectors) {
        for (DataConnector connector : connectors) {
            connectorMap.put(connector.getType(), connector);
            log.info("[连接器管理] 注册连接器: {}", connector.getType());
        }
    }

    /**
     * 获取所有支持的连接器类型
     */
    public Set<String> getSupportedTypes() {
        return Collections.unmodifiableSet(connectorMap.keySet());
    }

    /**
     * 获取连接器
     */
    public DataConnector getConnector(String type) {
        DataConnector connector = connectorMap.get(type);
        if (connector == null) {
            throw new IllegalArgumentException("不支持的连接器类型: " + type);
        }
        return connector;
    }

    /**
     * 测试连接（需求: 17.5）
     */
    public boolean testConnection(ConnectorConfig config) {
        DataConnector connector = getConnector(config.getType());
        return connector.testConnection(config);
    }

    /**
     * 获取表/集合列表（需求: 17.6）
     */
    public List<String> listTables(ConnectorConfig config) {
        DataConnector connector = getConnector(config.getType());
        return connector.listTables(config);
    }

    /**
     * 获取列信息（需求: 17.6）
     */
    public List<ConnectorResult.ColumnMeta> getColumns(ConnectorConfig config, String tableName) {
        DataConnector connector = getConnector(config.getType());
        return connector.getColumns(config, tableName);
    }

    /**
     * 读取数据
     */
    public ConnectorResult readData(ConnectorConfig config, String source, Map<String, Object> params) {
        DataConnector connector = getConnector(config.getType());
        return connector.readData(config, source, params);
    }
}

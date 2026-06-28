package com.dataplatform.data.service.connector;

import java.util.List;
import java.util.Map;

/**
 * 数据源连接器接口
 * 需求: 17.1, 17.2, 17.3, 17.4
 */
public interface DataConnector {

    /**
     * 连接器类型标识
     */
    String getType();

    /**
     * 测试连接（需求: 17.5）
     */
    boolean testConnection(ConnectorConfig config);

    /**
     * 获取元数据 - 表/集合列表（需求: 17.6）
     */
    List<String> listTables(ConnectorConfig config);

    /**
     * 获取表/集合的列信息（需求: 17.6）
     */
    List<ConnectorResult.ColumnMeta> getColumns(ConnectorConfig config, String tableName);

    /**
     * 读取数据
     */
    ConnectorResult readData(ConnectorConfig config, String source, Map<String, Object> params);
}

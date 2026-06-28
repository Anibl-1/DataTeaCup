package com.dataplatform.data.service.connector;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 连接器查询结果
 */
@Data
public class ConnectorResult {
    private List<Map<String, Object>> rows;
    private List<ColumnMeta> columns;
    private long totalRows;
    private boolean hasMore;

    @Data
    public static class ColumnMeta {
        private String name;
        private String type;
        private boolean nullable;

        public ColumnMeta() {}

        public ColumnMeta(String name, String type, boolean nullable) {
            this.name = name;
            this.type = type;
            this.nullable = nullable;
        }
    }
}

package com.dataplatform.serviceapi.data;

import com.dataplatform.common.Result;
import com.dataplatform.serviceapi.data.dto.DataSourceInfoDTO;
import com.dataplatform.serviceapi.data.dto.SqlExecuteRequest;
import com.dataplatform.serviceapi.data.dto.SqlExecuteResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * data-service OpenFeign 接口
 *
 * <p>供其他微服务调用 data-service 获取数据源元数据、执行 SQL 等。</p>
 * <p>设计文档 6.1 节同步调用链路：</p>
 * <ul>
 *   <li>analytics-service → data-service: 元数据查询、数据预览、SQL 执行</li>
 *   <li>analytics-service(AI 子域) → data-service: 元数据、查询执行、样本数据获取</li>
 * </ul>
 */
@FeignClient(name = "dp-data-service", contextId = "dataServiceApi", path = "/")
public interface DataServiceApi {

    // ==================== 数据源元数据 ====================

    @GetMapping("/data-source/{id}")
    Result<DataSourceInfoDTO> getDataSourceById(@PathVariable("id") Long id);

    @GetMapping("/data-source/list")
    Result<List<DataSourceInfoDTO>> listDataSources();

    // ==================== SQL 执行 ====================

    @PostMapping("/query-builder/execute")
    Result<SqlExecuteResult> executeSql(@RequestBody SqlExecuteRequest request);

    // ==================== 表元数据 ====================

    @GetMapping("/db-manager/{dataSourceId}/tables")
    Result<List<Map<String, Object>>> getTableList(@PathVariable("dataSourceId") Long dataSourceId);

    @GetMapping("/db-manager/{dataSourceId}/tables/{tableName}/columns")
    Result<List<Map<String, Object>>> getTableColumns(
            @PathVariable("dataSourceId") Long dataSourceId,
            @PathVariable("tableName") String tableName);
}

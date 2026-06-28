package com.dataplatform.data.service;

import com.dataplatform.common.constants.Constants;
import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.data.service.DbConnectionUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceService {
    private final DataSourceMapper dataSourceMapper;
    private final DbConnectionUtil dbConnectionUtil;
    private final JdbcTemplate jdbcTemplate;
    private final com.dataplatform.data.service.DataSourceConnectionPoolManager connectionPoolManager;
    
    /** 表列表缓存：dataSourceId -> 表列表，TTL 5分钟 */
    private Cache<Long, List<Map<String, String>>> tablesCache;
    
    /** 字段列表缓存："dataSourceId:tableName" -> 字段列表，TTL 5分钟 */
    private Cache<String, List<Map<String, Object>>> columnsCache;
    
    @PostConstruct
    public void initCache() {
        tablesCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(200)
                .build();
        columnsCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(2000)
                .build();
        log.info("元数据缓存初始化完成, TTL=5分钟");
    }

    /**
     * 获取数据源列表（分页）
     * 
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @param filters 筛选条件（JSON字符串）
     * @return 数据源列表
     */
    public List<DataSource> getDataSourceList(Integer page, Integer pageSize, String filters) {
        if (page == null || page < 1) {
            page = Constants.DEFAULT_PAGE;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = Constants.DEFAULT_PAGE_SIZE;
        }
        int offset = (page - 1) * pageSize;
        List<com.dataplatform.common.dto.FilterCondition> filterList = com.dataplatform.common.util.FilterUtil.parseFilters(filters);
        return dataSourceMapper.selectList(offset, pageSize, filterList);
    }

    /**
     * 获取数据源总数
     * 
     * @param filters 筛选条件（JSON字符串）
     * @return 数据源总数
     */
    public long getDataSourceCount(String filters) {
        List<com.dataplatform.common.dto.FilterCondition> filterList = com.dataplatform.common.util.FilterUtil.parseFilters(filters);
        return dataSourceMapper.count(filterList);
    }

    /**
     * 根据ID获取数据源
     * 
     * @param id 数据源ID
     * @return 数据源信息
     */
    public DataSource getDataSourceById(Long id) {
        return dataSourceMapper.selectById(id);
    }

    /**
     * 根据ID获取数据源连接
     * 
     * @param dataSourceId 数据源ID
     * @return javax.sql.DataSource 连接
     */
    public javax.sql.DataSource getDataSource(Long dataSourceId) {
        DataSource ds = getDataSourceById(dataSourceId);
        if (ds == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        // 返回一个简单的 DataSource 包装器
        return new SimpleDataSourceWrapper(ds, connectionPoolManager);
    }
    
    /**
     * 简单的 DataSource 包装器
     */
    private static class SimpleDataSourceWrapper implements javax.sql.DataSource {
        private final DataSource dataSource;
        private final com.dataplatform.data.service.DataSourceConnectionPoolManager poolManager;
        
        public SimpleDataSourceWrapper(DataSource dataSource, 
                com.dataplatform.data.service.DataSourceConnectionPoolManager poolManager) {
            this.dataSource = dataSource;
            this.poolManager = poolManager;
        }
        
        @Override
        public java.sql.Connection getConnection() throws java.sql.SQLException {
            return poolManager.getConnection(dataSource);
        }
        
        @Override
        public java.sql.Connection getConnection(String username, String password) throws java.sql.SQLException {
            return poolManager.getConnection(dataSource);
        }
        
        @Override
        public java.io.PrintWriter getLogWriter() { return null; }
        
        @Override
        public void setLogWriter(java.io.PrintWriter out) {}
        
        @Override
        public void setLoginTimeout(int seconds) {}
        
        @Override
        public int getLoginTimeout() { return 0; }
        
        @Override
        public java.util.logging.Logger getParentLogger() { return null; }
        
        @Override
        public <T> T unwrap(Class<T> iface) { return null; }
        
        @Override
        public boolean isWrapperFor(Class<?> iface) { return false; }
    }

    /**
     * 创建数据源
     * 
     * @param dataSource 数据源信息
     * @throws BusinessException 参数错误时抛出
     */
    @Transactional
    public void createDataSource(DataSource dataSource) {
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源信息不能为空");
        }
        if (!StringUtils.hasText(dataSource.getName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源名称不能为空");
        }
        if (!StringUtils.hasText(dataSource.getDbType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据库类型不能为空");
        }
        if (!StringUtils.hasText(dataSource.getHost())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "主机地址不能为空");
        }
        if (dataSource.getPort() == null || dataSource.getPort() < 1 || dataSource.getPort() > 65535) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "端口号必须在1-65535之间");
        }
        if (!StringUtils.hasText(dataSource.getDatabase())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据库名不能为空");
        }
        if (!StringUtils.hasText(dataSource.getUsername())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名不能为空");
        }
        if (!StringUtils.hasText(dataSource.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不能为空");
        }
        
        // 清理并验证database字段
        if (dataSource.getDatabase() != null) {
            dataSource.setDatabase(dataSource.getDatabase().trim());
        }
        
        dataSourceMapper.insert(dataSource);
    }

    /**
     * 更新数据源
     * 
     * @param dataSource 数据源信息
     * @throws BusinessException 数据源不存在时抛出
     */
    @Transactional
    public void updateDataSource(DataSource dataSource) {
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源信息不能为空");
        }
        if (dataSource.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源ID不能为空");
        }
        DataSource existing = dataSourceMapper.selectById(dataSource.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        // 确保所有必要字段都有值（如果前端没有传递，使用原有值）
        if (!StringUtils.hasText(dataSource.getName())) {
            dataSource.setName(existing.getName());
        }
        if (!StringUtils.hasText(dataSource.getDbType())) {
            dataSource.setDbType(existing.getDbType());
        }
        if (!StringUtils.hasText(dataSource.getHost())) {
            dataSource.setHost(existing.getHost());
        }
        if (dataSource.getPort() == null) {
            dataSource.setPort(existing.getPort());
        }
        if (!StringUtils.hasText(dataSource.getDatabase())) {
            dataSource.setDatabase(existing.getDatabase());
        }
        if (!StringUtils.hasText(dataSource.getUsername())) {
            dataSource.setUsername(existing.getUsername());
        }
        if (!StringUtils.hasText(dataSource.getPassword())) {
            dataSource.setPassword(existing.getPassword());
        }
        
        dataSourceMapper.update(dataSource);
    }

    /**
     * 删除数据源
     * 删除前检查关联（替代原触发器trg_data_source_before_delete）
     * 
     * @param id 数据源ID
     * @throws BusinessException 数据源不存在或正在使用时抛出
     */
    @Transactional
    public void deleteDataSource(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源ID不能为空");
        }
        DataSource dataSource = dataSourceMapper.selectById(id);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        // 检查是否有采集任务使用此数据源
        String taskCheckSql = "SELECT COUNT(*) FROM collect_task " +
                "WHERE data_source_id = ? OR target_data_source_id = ?";
        Long taskCount = jdbcTemplate.queryForObject(taskCheckSql, Long.class, id, id);
        
        // 检查是否有报表使用此数据源
        String reportCheckSql = "SELECT COUNT(*) FROM report_definition WHERE data_source_id = ?";
        Long reportCount = jdbcTemplate.queryForObject(reportCheckSql, Long.class, id);
        
        // 检查是否有图表使用此数据源
        String chartCheckSql = "SELECT COUNT(*) FROM chart_definition WHERE data_source_id = ?";
        Long chartCount = jdbcTemplate.queryForObject(chartCheckSql, Long.class, id);
        
        // 如果有关联则阻止删除
        if ((taskCount != null && taskCount > 0) || 
            (reportCount != null && reportCount > 0) || 
            (chartCount != null && chartCount > 0)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, 
                "数据源正在被使用，无法删除。请先删除相关的采集任务、报表或图表。");
        }
        
        dataSourceMapper.delete(id);
    }

    /**
     * 获取所有分组名称
     */
    public List<String> getGroups() {
        return dataSourceMapper.selectGroups();
    }

    /**
     * 测试数据源连接
     * 
     * @param dataSource 数据源信息
     * @throws BusinessException 连接失败时抛出
     */
    public void testConnection(DataSource dataSource) {
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源信息不能为空");
        }
        dbConnectionUtil.testConnection(dataSource);
    }

    /**
     * 构建JDBC URL
     * 
     * @param dataSource 数据源信息
     * @return JDBC URL
     */
    public String buildJdbcUrl(DataSource dataSource) {
        return dbConnectionUtil.buildJdbcUrl(dataSource);
    }
    
    /**
     * 获取数据源的表列表
     * 过滤掉系统表（平台自身使用的表）
     * 
     * @param dataSourceId 数据源ID
     * @return 表列表
     */
    public List<Map<String, String>> getTables(Long dataSourceId) {
        // 先查缓存
        List<Map<String, String>> cached = tablesCache.getIfPresent(dataSourceId);
        if (cached != null) {
            return cached;
        }
        
        DataSource dataSource = getDataSourceById(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        
        // 系统表列表
        java.util.Set<String> systemTables = java.util.Set.of(
            "chart_definition", "collect_task", "data_dictionary", "data_source",
            "menu", "page_chart", "page_definition", "report_definition",
            "role", "role_menu", "sys_user", "user_role", "datax_job", "datax_job_log"
        );
        
        try {
            String dbType = dataSource.getDbType().toLowerCase();
            String database = dataSource.getDatabase();
            
            List<Map<String, String>> tables = new java.util.ArrayList<>();
            
            try (java.sql.Connection conn = connectionPoolManager.getConnection(dataSource)) {
                java.sql.DatabaseMetaData metaData = conn.getMetaData();
                String[] types = {"TABLE", "VIEW"};
                
                java.sql.ResultSet rs;
                if ("mysql".equals(dbType)) {
                    rs = metaData.getTables(database, null, "%", types);
                } else if ("postgresql".equals(dbType)) {
                    rs = metaData.getTables(null, "public", "%", types);
                } else if ("oracle".equals(dbType)) {
                    String schema = dataSource.getUsername() != null ? dataSource.getUsername().toUpperCase() : null;
                    rs = metaData.getTables(null, schema, "%", types);
                } else {
                    rs = metaData.getTables(database, null, "%", types);
                }
                
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    if (systemTables.contains(tableName.toLowerCase())) {
                        continue;
                    }
                    Map<String, String> table = new java.util.HashMap<>();
                    table.put("tableName", tableName);
                    table.put("tableType", rs.getString("TABLE_TYPE"));
                    tables.add(table);
                }
                rs.close();
            }
            
            tablesCache.put(dataSourceId, tables);
            return tables;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ERROR, "获取表列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取表字段列表
     * 
     * @param dataSourceId 数据源ID
     * @param tableName 表名
     * @return 字段列表
     */
    public List<Map<String, Object>> getTableColumns(Long dataSourceId, String tableName) {
        // 先查缓存
        String cacheKey = dataSourceId + ":" + tableName;
        List<Map<String, Object>> cached = columnsCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        DataSource dataSource = getDataSourceById(dataSourceId);
        if (dataSource == null) {
            return java.util.Collections.emptyList();
        }
        
        try (java.sql.Connection conn = connectionPoolManager.getConnection(dataSource)) {
            java.sql.DatabaseMetaData metaData = conn.getMetaData();
            java.sql.ResultSet rs = metaData.getColumns(null, null, tableName, null);
            
            List<Map<String, Object>> columns = new java.util.ArrayList<>();
            while (rs.next()) {
                Map<String, Object> column = new java.util.HashMap<>();
                column.put("columnName", rs.getString("COLUMN_NAME"));
                column.put("dataType", rs.getString("TYPE_NAME"));
                column.put("nullable", rs.getString("IS_NULLABLE"));
                column.put("remarks", rs.getString("REMARKS"));
                column.put("columnSize", rs.getInt("COLUMN_SIZE"));
                columns.add(column);
            }
            rs.close();
            columnsCache.put(cacheKey, columns);
            return columns;
        } catch (Exception e) {
            log.warn("获取表字段失败: dataSourceId={}, tableName={}, error={}", dataSourceId, tableName, e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 批量测试数据源连接
     */
    public List<Map<String, Object>> batchTestConnection(List<Long> ids) {
        List<Map<String, Object>> results = new java.util.ArrayList<>();
        for (Long id : ids) {
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("id", id);
            DataSource ds = dataSourceMapper.selectById(id);
            if (ds == null) {
                result.put("success", false);
                result.put("message", "数据源不存在");
                results.add(result);
                continue;
            }
            result.put("name", ds.getName());
            try {
                long start = System.currentTimeMillis();
                dbConnectionUtil.testConnection(ds);
                long elapsed = System.currentTimeMillis() - start;
                result.put("success", true);
                result.put("message", "连接成功");
                result.put("responseTime", elapsed);
                // 更新测试结果
                ds.setLastTestResult(1);
                ds.setLastTestTime(new java.util.Date());
                ds.setLastTestError(null);
                dataSourceMapper.update(ds);
            } catch (Exception e) {
                result.put("success", false);
                result.put("message", e.getMessage());
                ds.setLastTestResult(0);
                ds.setLastTestTime(new java.util.Date());
                ds.setLastTestError(e.getMessage());
                dataSourceMapper.update(ds);
            }
            results.add(result);
        }
        return results;
    }

    /**
     * 获取数据源详情（含表列表和连接状态）
     */
    public Map<String, Object> getDataSourceDetail(Long id) {
        DataSource ds = dataSourceMapper.selectById(id);
        if (ds == null) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_NOT_FOUND, "数据源不存在");
        }
        Map<String, Object> detail = new java.util.HashMap<>();
        detail.put("dataSource", ds);
        // 获取表列表
        try {
            List<Map<String, String>> tables = getTables(id);
            detail.put("tables", tables);
            detail.put("tableCount", tables.size());
        } catch (Exception e) {
            detail.put("tables", java.util.Collections.emptyList());
            detail.put("tableCount", 0);
            detail.put("tableError", e.getMessage());
        }
        return detail;
    }
}


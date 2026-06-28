package com.dataplatform.data.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.data.entity.DataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 外部数据源连接池管理器
 * 为每个外部数据源维护独立的HikariCP连接池，避免每次请求创建新连接
 * 
 * @author dataplatform
 */
@Slf4j
@Component
public class DataSourceConnectionPoolManager {
    
    @Autowired
    private DbConnectionUtil dbConnectionUtil;
    
    /** 连接池缓存：dataSourceId -> HikariDataSource */
    private final Map<Long, HikariDataSource> poolCache = new ConcurrentHashMap<>();
    
    /** 数据源配置快照：dataSourceId -> configHash，用于检测配置变更 */
    private final Map<Long, String> configHashCache = new ConcurrentHashMap<>();
    
    @Value("${datasource.pool.max-pool-size:5}")
    private int maxPoolSize;
    
    @Value("${datasource.pool.min-idle:1}")
    private int minIdle;
    
    @Value("${datasource.pool.connection-timeout:10000}")
    private long connectionTimeout;
    
    @Value("${datasource.pool.idle-timeout:300000}")
    private long idleTimeout;
    
    @Value("${datasource.pool.max-lifetime:600000}")
    private long maxLifetime;
    
    /**
     * 获取外部数据源的数据库连接（从连接池获取）
     * 
     * @param dataSource 数据源配置
     * @return 数据库连接
     * @throws SQLException 连接失败时抛出
     */
    public Connection getConnection(DataSource dataSource) throws SQLException {
        if (dataSource == null || dataSource.getId() == null) {
            throw new SQLException("数据源信息不完整");
        }
        
        Long dsId = dataSource.getId();
        String currentHash = buildConfigHash(dataSource);
        
        // 检查是否需要重建连接池（配置变更时）
        String cachedHash = configHashCache.get(dsId);
        if (cachedHash != null && !cachedHash.equals(currentHash)) {
            log.info("数据源[{}]配置已变更，重建连接池", dsId);
            closePool(dsId);
        }
        
        HikariDataSource pool = poolCache.computeIfAbsent(dsId, id -> {
            try {
                return createPool(dataSource);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECTION_FAILED, "创建连接池失败: " + e.getMessage());
            }
        });
        
        configHashCache.put(dsId, currentHash);
        return pool.getConnection();
    }
    
    /**
     * 关闭指定数据源的连接池
     * 
     * @param dataSourceId 数据源ID
     */
    public void closePool(Long dataSourceId) {
        HikariDataSource pool = poolCache.remove(dataSourceId);
        configHashCache.remove(dataSourceId);
        if (pool != null && !pool.isClosed()) {
            pool.close();
            log.info("已关闭数据源[{}]的连接池", dataSourceId);
        }
    }
    
    /**
     * 获取连接池统计信息
     * 
     * @return 各数据源的连接池状态
     */
    public Map<Long, String> getPoolStats() {
        Map<Long, String> stats = new ConcurrentHashMap<>();
        poolCache.forEach((id, pool) -> {
            if (!pool.isClosed()) {
                stats.put(id, String.format("active=%d, idle=%d, total=%d, waiting=%d",
                        pool.getHikariPoolMXBean().getActiveConnections(),
                        pool.getHikariPoolMXBean().getIdleConnections(),
                        pool.getHikariPoolMXBean().getTotalConnections(),
                        pool.getHikariPoolMXBean().getThreadsAwaitingConnection()));
            }
        });
        return stats;
    }
    
    /**
     * 创建HikariCP连接池
     */
    private HikariDataSource createPool(DataSource dataSource) {
        String url = dbConnectionUtil.buildJdbcUrl(dataSource);
        String driver = dbConnectionUtil.getDriverClassName(dataSource.getDbType());
        
        HikariConfig config = new HikariConfig();
        config.setPoolName("ExtDS-" + dataSource.getId());
        config.setJdbcUrl(url);
        config.setUsername(dataSource.getUsername());
        config.setPassword(dataSource.getPassword());
        config.setDriverClassName(driver);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setConnectionTestQuery("SELECT 1");
        
        log.info("为数据源[{}:{}]创建连接池, maxPool={}", dataSource.getId(), dataSource.getName(), maxPoolSize);
        return new HikariDataSource(config);
    }
    
    /**
     * 构建配置哈希（用于检测配置变更）
     */
    private String buildConfigHash(DataSource ds) {
        return String.format("%s|%s|%d|%s|%s|%s",
                ds.getDbType(), ds.getHost(), ds.getPort(),
                ds.getDatabase(), ds.getUsername(), ds.getPassword());
    }
    
    /**
     * 应用关闭时清理所有连接池
     */
    @PreDestroy
    public void destroy() {
        log.info("正在关闭所有外部数据源连接池...");
        poolCache.forEach((id, pool) -> {
            if (!pool.isClosed()) {
                pool.close();
            }
        });
        poolCache.clear();
        configHashCache.clear();
        log.info("所有外部数据源连接池已关闭");
    }
}

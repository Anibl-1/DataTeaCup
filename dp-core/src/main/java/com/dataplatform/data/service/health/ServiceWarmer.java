package com.dataplatform.data.service.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 服务预热器
 * 在启动时预加载热点数据和配置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceWarmer {

    private final DataSource dataSource;
    private final ExecutorService warmupExecutor = Executors.newFixedThreadPool(3);

    private volatile boolean warmupComplete = false;
    private final List<String> warmupErrors = new ArrayList<>();

    /**
     * 应用启动后执行预热
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("开始服务预热...");
        long startTime = System.currentTimeMillis();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 预热数据库连接池
        futures.add(CompletableFuture.runAsync(this::warmupDatabasePool, warmupExecutor));

        // 预热系统配置
        futures.add(CompletableFuture.runAsync(this::warmupSystemConfig, warmupExecutor));

        // 预热热点数据
        futures.add(CompletableFuture.runAsync(this::warmupHotData, warmupExecutor));

        // 等待所有预热任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .whenComplete((result, ex) -> {
                warmupComplete = true;
                long duration = System.currentTimeMillis() - startTime;
                
                if (warmupErrors.isEmpty()) {
                    log.info("服务预热完成，耗时: {}ms", duration);
                } else {
                    log.warn("服务预热完成（有错误），耗时: {}ms, 错误: {}", duration, warmupErrors);
                }
                
                warmupExecutor.shutdown();
            });
    }

    /**
     * 预热数据库连接池
     */
    private void warmupDatabasePool() {
        log.info("预热数据库连接池...");
        List<Connection> connections = new ArrayList<>();
        try {
            // 创建多个连接以预热连接池
            for (int i = 0; i < 5; i++) {
                connections.add(dataSource.getConnection());
            }
            
            // 执行简单查询
            for (Connection conn : connections) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT 1")) {
                    ps.executeQuery();
                }
            }
            
            log.info("数据库连接池预热完成");
        } catch (Exception e) {
            log.error("数据库连接池预热失败", e);
            warmupErrors.add("数据库连接池: " + e.getMessage());
        } finally {
            // 确保所有连接都被关闭（归还到连接池）
            for (Connection conn : connections) {
                try { conn.close(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * 预热系统配置
     */
    private void warmupSystemConfig() {
        log.info("预热系统配置...");
        try {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT config_key, config_value FROM sys_config LIMIT 100")) {
                ResultSet rs = ps.executeQuery();
                int count = 0;
                while (rs.next()) {
                    count++;
                }
                log.info("系统配置预热完成，加载 {} 条配置", count);
            }
        } catch (Exception e) {
            log.warn("系统配置预热失败（表可能不存在）: {}", e.getMessage());
            // 不记录为错误，因为表可能不存在
        }
    }

    /**
     * 预热热点数据
     */
    private void warmupHotData() {
        log.info("预热热点数据...");
        try {
            // 预热数据源列表
            warmupTable("data_source", "SELECT id, name, db_type FROM data_source LIMIT 50");
            
            // 预热用户列表
            warmupTable("sys_user", "SELECT id, username, status FROM sys_user WHERE status = 1 LIMIT 100");
            
            // 预热角色列表
            warmupTable("sys_role", "SELECT id, role_name, role_code FROM sys_role LIMIT 50");
            
            log.info("热点数据预热完成");
        } catch (Exception e) {
            log.warn("热点数据预热失败: {}", e.getMessage());
        }
    }

    /**
     * 预热单个表
     */
    private void warmupTable(String tableName, String sql) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
                count++;
            }
            log.debug("表 {} 预热完成，加载 {} 条记录", tableName, count);
        } catch (Exception e) {
            log.debug("表 {} 预热跳过（可能不存在）: {}", tableName, e.getMessage());
        }
    }

    /**
     * 检查预热是否完成
     */
    public boolean isWarmupComplete() {
        return warmupComplete;
    }

    /**
     * 获取预热错误列表
     */
    public List<String> getWarmupErrors() {
        return new ArrayList<>(warmupErrors);
    }
}

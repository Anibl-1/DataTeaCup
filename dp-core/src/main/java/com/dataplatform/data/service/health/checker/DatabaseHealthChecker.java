package com.dataplatform.data.service.health.checker;

import com.dataplatform.data.service.health.ComponentHealth;
import com.dataplatform.data.service.health.HealthChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库健康检查器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseHealthChecker implements HealthChecker {

    private final DataSource dataSource;

    @Override
    public ComponentHealth check() {
        long start = System.currentTimeMillis();
        ComponentHealth health = new ComponentHealth();
        health.setName(getName());
        health.setCritical(true);

        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(5);
            health.setResponseTime(System.currentTimeMillis() - start);
            
            if (valid) {
                health.setStatus(ComponentHealth.STATUS_UP);
                health.setMessage("数据库连接正常");
                
                // 添加详细信息
                health.withDetail("databaseProductName", conn.getMetaData().getDatabaseProductName())
                      .withDetail("databaseProductVersion", conn.getMetaData().getDatabaseProductVersion())
                      .withDetail("driverName", conn.getMetaData().getDriverName())
                      .withDetail("url", conn.getMetaData().getURL());
            } else {
                health.setStatus(ComponentHealth.STATUS_DOWN);
                health.setMessage("数据库连接无效");
            }
        } catch (SQLException e) {
            health.setResponseTime(System.currentTimeMillis() - start);
            health.setStatus(ComponentHealth.STATUS_DOWN);
            health.setMessage("数据库连接失败: " + e.getMessage());
            log.error("数据库健康检查失败", e);
        }

        return health;
    }

    @Override
    public String getName() {
        return "database";
    }

    @Override
    public int getCheckInterval() {
        return 30;
    }
}

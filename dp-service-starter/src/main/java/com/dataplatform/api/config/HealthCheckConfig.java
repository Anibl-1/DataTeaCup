package com.dataplatform.api.config;

import com.dataplatform.data.service.health.HealthCheckService;
import com.dataplatform.data.service.health.HealthChecker;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 健康检查配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class HealthCheckConfig {

    private final HealthCheckService healthCheckService;
    private final List<HealthChecker> checkers;

    @PostConstruct
    public void init() {
        for (HealthChecker checker : checkers) {
            healthCheckService.registerChecker(checker.getName(), checker);
        }
        log.info("已注册 {} 个健康检查器", checkers.size());
    }
}

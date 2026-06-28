package com.dataplatform.api.config;

import io.micrometer.core.instrument.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义业务指标采集
 */
@Component
@RequiredArgsConstructor
@ConditionalOnClass(MeterRegistry.class)
public class CustomMetrics {

    private final MeterRegistry meterRegistry;

    private Counter queryCounter;
    private Counter exportCounter;
    private Counter loginSuccessCounter;
    private Counter loginFailCounter;
    private Counter alertCounter;

    private final AtomicInteger activeQueries = new AtomicInteger(0);
    private final AtomicInteger activeSessions = new AtomicInteger(0);
    private final AtomicInteger onlineUsers = new AtomicInteger(0);

    private Timer queryTimer;
    private Timer exportTimer;
    private Timer dashboardLoadTimer;

    @PostConstruct
    public void init() {
        queryCounter = Counter.builder("datateacup.query.total").description("数据查询总次数").register(meterRegistry);
        queryTimer = Timer.builder("datateacup.query.duration").description("数据查询耗时").register(meterRegistry);
        meterRegistry.gauge("datateacup.query.active", activeQueries);
        exportCounter = Counter.builder("datateacup.export.total").description("数据导出总次数").register(meterRegistry);
        exportTimer = Timer.builder("datateacup.export.duration").description("数据导出耗时").register(meterRegistry);
        loginSuccessCounter = Counter.builder("datateacup.login.success").description("登录成功次数").register(meterRegistry);
        loginFailCounter = Counter.builder("datateacup.login.fail").description("登录失败次数").register(meterRegistry);
        meterRegistry.gauge("datateacup.sessions.active", activeSessions);
        meterRegistry.gauge("datateacup.users.online", onlineUsers);
        alertCounter = Counter.builder("datateacup.alert.total").description("告警触发总次数").register(meterRegistry);
        dashboardLoadTimer = Timer.builder("datateacup.dashboard.load").description("仪表盘加载耗时").register(meterRegistry);
    }

    public void recordQuery() { queryCounter.increment(); }
    public Timer.Sample startQueryTimer() { activeQueries.incrementAndGet(); return Timer.start(meterRegistry); }
    public void stopQueryTimer(Timer.Sample sample) { activeQueries.decrementAndGet(); sample.stop(queryTimer); }
    public void recordExport() { exportCounter.increment(); }
    public Timer.Sample startExportTimer() { return Timer.start(meterRegistry); }
    public void stopExportTimer(Timer.Sample sample) { sample.stop(exportTimer); }
    public void recordLoginSuccess() { loginSuccessCounter.increment(); }
    public void recordLoginFail() { loginFailCounter.increment(); }
    public void sessionCreated() { activeSessions.incrementAndGet(); onlineUsers.incrementAndGet(); }
    public void sessionDestroyed() { activeSessions.decrementAndGet(); onlineUsers.decrementAndGet(); }
    public void recordAlert() { alertCounter.increment(); }
    public Timer.Sample startDashboardLoadTimer() { return Timer.start(meterRegistry); }
    public void stopDashboardLoadTimer(Timer.Sample sample) { sample.stop(dashboardLoadTimer); }
    public void incrementCounter(String name, String... tags) { Counter.builder(name).tags(tags).register(meterRegistry).increment(); }
}

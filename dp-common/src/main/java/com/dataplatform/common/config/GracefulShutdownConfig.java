package com.dataplatform.common.config;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 优雅停机配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class GracefulShutdownConfig {

    @Value("${server.shutdown.timeout:30}")
    private int shutdownTimeout;

    private volatile boolean shuttingDown = false;

    public boolean isShuttingDown() { return shuttingDown; }

    @PreDestroy
    public void onShutdown() {
        log.info("开始优雅停机，等待时间: {}秒", shutdownTimeout);
        shuttingDown = true;
        try {
            Thread.sleep(5000);
            log.info("优雅停机完成");
        } catch (InterruptedException e) {
            log.warn("优雅停机被中断");
            Thread.currentThread().interrupt();
        }
    }

    public void shutdownExecutor(ExecutorService executor, String name) {
        log.info("关闭线程池: {}", name);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(shutdownTimeout, TimeUnit.SECONDS)) {
                log.warn("线程池 {} 未能在 {} 秒内关闭，强制关闭", name, shutdownTimeout);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("等待线程池 {} 关闭时被中断", name);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void shutdownTaskExecutor(ThreadPoolTaskExecutor executor, String name) {
        log.info("关闭Spring线程池: {}", name);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(shutdownTimeout);
        executor.shutdown();
    }
}

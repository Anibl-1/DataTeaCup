package com.dataplatform.analytics;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 分析服务启动类
 * 负责：报表定义/模板、图表、仪表盘、页面设计器、样式配置、AI 智能分析、版本、动态参数
 *
 * Phase 3: Controller 已物理迁移到 com.dataplatform.analytics.controller 包下
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.dataplatform")
@MapperScan({
    "com.dataplatform.system.mapper",
    "com.dataplatform.org.mapper",
    "com.dataplatform.data.mapper",
    "com.dataplatform.message.mapper"
})
@EnableScheduling
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.dataplatform.serviceapi")
public class AnalyticsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}

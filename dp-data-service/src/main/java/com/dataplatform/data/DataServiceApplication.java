package com.dataplatform.data;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 数据服务启动类
 * 负责：数据源接入、SQL执行、数据管道、数据采集、数据同步、数据治理
 *
 * Phase 3: Controller 已物理迁移到 com.dataplatform.data.controller 包下
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
public class DataServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataServiceApplication.class, args);
    }
}

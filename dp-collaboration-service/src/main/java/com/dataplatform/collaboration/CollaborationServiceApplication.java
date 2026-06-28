package com.dataplatform.collaboration;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 协作服务启动类
 * 负责：即时聊天、通知管理、公告、评论、团队空间、工单
 *
 * Phase 3: Controller 已物理迁移到 com.dataplatform.collaboration.controller 包下
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
public class CollaborationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CollaborationServiceApplication.class, args);
    }
}

package com.dataplatform.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 系统服务启动类
 * 负责：用户/角色/菜单/权限管理、部门/岗位管理、系统配置、操作日志、系统监控、运维管理
 *
 * Phase 3: Controller 已物理迁移到 com.dataplatform.system.controller 包下，
 *          Service/Mapper/Config 等 Bean 仍然全量加载（共享数据库阶段）
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
public class SystemServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SystemServiceApplication.class, args);
    }
}

package com.dataplatform.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DataTeaCup API 网关启动类
 * 路由请求到 4 个微服务：system(9001) / data(9002) / analytics(9003) / collaboration(9004)
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}

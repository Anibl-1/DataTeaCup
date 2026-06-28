package com.dataplatform.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 全局过滤器：透传 Authorization 头到后端服务
 * 网关不做鉴权逻辑，由各后端服务自行校验 JWT + Sa-Token
 */
@Slf4j
@Component
public class AuthHeaderRelayFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 透传 Authorization 头（不做任何鉴权处理）
        String authorization = request.getHeaders().getFirst("Authorization");
        if (authorization != null) {
            log.debug("Gateway relay auth header for path: {}", path);
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}

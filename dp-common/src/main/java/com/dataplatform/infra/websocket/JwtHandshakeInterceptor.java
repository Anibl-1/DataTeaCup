package com.dataplatform.infra.websocket;

import com.dataplatform.common.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * JWT 握手拦截器
 * 从 URL 参数 token 提取 JWT，通过 Sa-Token 验证有效性，
 * 无效则以关闭码 4001 拒绝连接。
 */
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    private final JwtUtil jwtUtil;

    public JwtHandshakeInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build().getQueryParams().getFirst("token");

        if (token == null || token.isBlank()) {
            log.warn("[ChatWS] 握手被拒绝: 缺少 token 参数, uri={}", request.getURI());
            return false;
        }

        try {
            if (!jwtUtil.validateToken(token)) {
                log.warn("[ChatWS] 握手被拒绝: token 无效或已过期");
                return false;
            }

            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);

            if (userId == null) {
                log.warn("[ChatWS] 握手被拒绝: token 中无 userId");
                return false;
            }

            // 将用户信息存入 WebSocket session attributes
            attributes.put("userId", userId);
            attributes.put("username", username);

            log.debug("[ChatWS] 握手成功: userId={}, username={}", userId, username);
            return true;
        } catch (Exception e) {
            log.error("[ChatWS] 握手异常: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}

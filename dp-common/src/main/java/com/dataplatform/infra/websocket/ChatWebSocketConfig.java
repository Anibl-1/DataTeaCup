package com.dataplatform.infra.websocket;

import com.dataplatform.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 原生 WebSocket 配置（聊天端点）
 * <p>
 * 注册 /ws/chat 端点，与现有 STOMP /ws 端点并存互不影响。
 * 使用 JwtHandshakeInterceptor 进行 JWT 认证。
 */
@Configuration
@EnableWebSocket
public class ChatWebSocketConfig implements WebSocketConfigurer {

    @Value("${cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
    private String allowedOriginsConfig;

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = allowedOriginsConfig.split(",");
        for (int i = 0; i < origins.length; i++) {
            origins[i] = origins[i].trim();
        }

        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(new JwtHandshakeInterceptor(jwtUtil))
                .setAllowedOrigins(origins);
    }
}

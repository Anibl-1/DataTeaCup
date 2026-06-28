package com.dataplatform;

import com.dataplatform.infra.websocket.ChatWebSocketHandler;
import com.fasterxml.jackson.databind.JsonNode;
import net.jqwik.api.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 属性测试：WebSocket 消息类型路由
 *
 * Feature: mars-integration-optimization, Property 23: WebSocket 消息类型路由
 * Validates: Requirements 9.4
 */
class WebSocketMessageRoutingPropertyTest {

    /**
     * Testable subclass that exposes the protected handleTextMessage method.
     */
    static class TestableChatWebSocketHandler extends ChatWebSocketHandler {
        public void invokeHandleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            handleTextMessage(session, message);
        }
    }

    private WebSocketSession createMockSession(Long userId) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(Map.of("userId", userId));
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    // ========== Property 23a: "chat" type messages are routed to the chat router ==========

    /**
     * Property 23a: Messages with type "chat" are routed to the chat router.
     *
     * Feature: mars-integration-optimization, Property 23: WebSocket 消息类型路由
     * Validates: Requirements 9.4
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_23_WebSocket消息类型路由")
    void chatType_routedToChatRouter(@ForAll("validUserIds") Long userId,
                                     @ForAll("arbitraryPayloads") String payloadContent) throws Exception {
        TestableChatWebSocketHandler handler = new TestableChatWebSocketHandler();
        WebSocketSession session = createMockSession(userId);

        AtomicInteger chatCallCount = new AtomicInteger(0);
        AtomicReference<Long> capturedUserId = new AtomicReference<>();
        AtomicReference<JsonNode> capturedPayload = new AtomicReference<>();

        handler.registerRouter("chat", (uid, payload) -> {
            chatCallCount.incrementAndGet();
            capturedUserId.set(uid);
            capturedPayload.set(payload);
        });

        // Also register a status router to verify it's NOT called
        AtomicInteger statusCallCount = new AtomicInteger(0);
        handler.registerRouter("status", (uid, payload) -> statusCallCount.incrementAndGet());

        String json = "{\"type\":\"chat\",\"payload\":" + payloadContent + "}";
        handler.invokeHandleTextMessage(session, new TextMessage(json));

        assertThat(chatCallCount.get()).isEqualTo(1);
        assertThat(capturedUserId.get()).isEqualTo(userId);
        assertThat(statusCallCount.get()).isEqualTo(0);
    }

    // ========== Property 23b: "status" type messages are routed to the status router ==========

    /**
     * Property 23b: Messages with type "status" are routed to the status router.
     *
     * Feature: mars-integration-optimization, Property 23: WebSocket 消息类型路由
     * Validates: Requirements 9.4
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_23_WebSocket消息类型路由")
    void statusType_routedToStatusRouter(@ForAll("validUserIds") Long userId,
                                         @ForAll("arbitraryPayloads") String payloadContent) throws Exception {
        TestableChatWebSocketHandler handler = new TestableChatWebSocketHandler();
        WebSocketSession session = createMockSession(userId);

        AtomicInteger chatCallCount = new AtomicInteger(0);
        handler.registerRouter("chat", (uid, payload) -> chatCallCount.incrementAndGet());

        AtomicInteger statusCallCount = new AtomicInteger(0);
        AtomicReference<Long> capturedUserId = new AtomicReference<>();

        handler.registerRouter("status", (uid, payload) -> {
            statusCallCount.incrementAndGet();
            capturedUserId.set(uid);
        });

        String json = "{\"type\":\"status\",\"payload\":" + payloadContent + "}";
        handler.invokeHandleTextMessage(session, new TextMessage(json));

        assertThat(statusCallCount.get()).isEqualTo(1);
        assertThat(capturedUserId.get()).isEqualTo(userId);
        assertThat(chatCallCount.get()).isEqualTo(0);
    }

    // ========== Property 23c: Unknown types are not routed to any registered router ==========

    /**
     * Property 23c: Messages with unknown types are not routed to any registered router.
     *
     * Feature: mars-integration-optimization, Property 23: WebSocket 消息类型路由
     * Validates: Requirements 9.4
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_23_WebSocket消息类型路由")
    void unknownType_notRouted(@ForAll("validUserIds") Long userId,
                               @ForAll("unknownTypes") String unknownType) throws Exception {
        TestableChatWebSocketHandler handler = new TestableChatWebSocketHandler();
        WebSocketSession session = createMockSession(userId);

        AtomicInteger chatCallCount = new AtomicInteger(0);
        AtomicInteger statusCallCount = new AtomicInteger(0);

        handler.registerRouter("chat", (uid, payload) -> chatCallCount.incrementAndGet());
        handler.registerRouter("status", (uid, payload) -> statusCallCount.incrementAndGet());

        String json = "{\"type\":\"" + escapeJson(unknownType) + "\",\"payload\":{}}";
        handler.invokeHandleTextMessage(session, new TextMessage(json));

        assertThat(chatCallCount.get()).isEqualTo(0);
        assertThat(statusCallCount.get()).isEqualTo(0);
    }

    // ========== Property 23d: Messages without a type field are not routed ==========

    /**
     * Property 23d: Messages without a type field are not routed.
     *
     * Feature: mars-integration-optimization, Property 23: WebSocket 消息类型路由
     * Validates: Requirements 9.4
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_23_WebSocket消息类型路由")
    void missingType_notRouted(@ForAll("validUserIds") Long userId,
                               @ForAll("messagesWithoutType") String jsonMessage) throws Exception {
        TestableChatWebSocketHandler handler = new TestableChatWebSocketHandler();
        WebSocketSession session = createMockSession(userId);

        AtomicInteger chatCallCount = new AtomicInteger(0);
        AtomicInteger statusCallCount = new AtomicInteger(0);

        handler.registerRouter("chat", (uid, payload) -> chatCallCount.incrementAndGet());
        handler.registerRouter("status", (uid, payload) -> statusCallCount.incrementAndGet());

        handler.invokeHandleTextMessage(session, new TextMessage(jsonMessage));

        assertThat(chatCallCount.get()).isEqualTo(0);
        assertThat(statusCallCount.get()).isEqualTo(0);
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<Long> validUserIds() {
        return Arbitraries.longs().between(1L, 100000L);
    }

    @Provide
    Arbitrary<String> arbitraryPayloads() {
        return Arbitraries.oneOf(
                Arbitraries.of("{}",
                        "{\"msg\":\"hello\"}",
                        "{\"id\":1}",
                        "{\"data\":[1,2,3]}",
                        "\"text\"",
                        "null",
                        "123",
                        "true")
        );
    }

    @Provide
    Arbitrary<String> unknownTypes() {
        return Arbitraries.oneOf(
                // Explicitly not "chat" or "status"
                Arbitraries.of("ping", "pong", "heartbeat", "unknown", "message",
                        "notification", "typing", "read", "ack", "error",
                        "subscribe", "unsubscribe", "join", "leave"),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                        .filter(s -> !s.equals("chat") && !s.equals("status"))
        );
    }

    @Provide
    Arbitrary<String> messagesWithoutType() {
        return Arbitraries.of(
                // No type field at all
                "{\"payload\":{\"msg\":\"hello\"}}",
                "{\"data\":\"test\"}",
                "{\"action\":\"send\"}",
                "{}",
                // type field is null
                "{\"type\":null,\"payload\":{}}",
                // type field is empty string
                "{\"type\":\"\",\"payload\":{}}",
                // type field is blank
                "{\"type\":\"   \",\"payload\":{}}"
        );
    }

    // ========== Utility ==========

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

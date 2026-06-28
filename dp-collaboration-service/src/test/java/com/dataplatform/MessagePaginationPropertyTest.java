package com.dataplatform;

import com.dataplatform.message.entity.ChatMessage;
import com.dataplatform.message.mapper.ChatConversationMapper;
import com.dataplatform.message.mapper.ChatConversationMemberMapper;
import com.dataplatform.message.mapper.ChatMessageMapper;
import com.dataplatform.message.service.ChatMessageService;
import net.jqwik.api.*;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 属性测试：游标分页消息查询有序性
 *
 * Feature: mars-integration-optimization, Property 15: 游标分页消息查询有序性
 * **Validates: Requirements 10.9**
 */
class MessagePaginationPropertyTest {

    private static final int PAGE_SIZE = 20;

    // ========== Helper methods ==========

    private ChatMessageService createServiceWithMocks(ChatMessageMapper messageMapper) throws Exception {
        ChatMessageService service = new ChatMessageService();

        Field msgField = ChatMessageService.class.getDeclaredField("chatMessageMapper");
        msgField.setAccessible(true);
        msgField.set(service, messageMapper);

        Field convField = ChatMessageService.class.getDeclaredField("conversationMapper");
        convField.setAccessible(true);
        convField.set(service, Mockito.mock(ChatConversationMapper.class));

        Field memField = ChatMessageService.class.getDeclaredField("memberMapper");
        memField.setAccessible(true);
        memField.set(service, Mockito.mock(ChatConversationMemberMapper.class));

        return service;
    }

    /**
     * Simulates the SQL behavior: filter by cursor (id < cursor), sort by sendTime ASC then id ASC, limit PAGE_SIZE.
     */
    private List<ChatMessage> simulateMapperQuery(List<ChatMessage> allMessages, Long cursor) {
        return allMessages.stream()
                .filter(m -> cursor == null || m.getId() < cursor)
                .sorted(Comparator.comparing(ChatMessage::getSendTime).thenComparing(ChatMessage::getId))
                .limit(PAGE_SIZE)
                .collect(Collectors.toList());
    }

    // ========== Generators ==========

    @Provide
    Arbitrary<List<ChatMessage>> randomMessages() {
        Arbitrary<ChatMessage> messageArb = Arbitraries.longs().between(1, 10000)
                .flatMap(id -> Arbitraries.longs().between(1, 100)
                        .flatMap(senderId -> Arbitraries.integers().between(2020, 2025)
                                .flatMap(year -> Arbitraries.integers().between(1, 12)
                                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                                .flatMap(day -> Arbitraries.integers().between(0, 23)
                                                        .flatMap(hour -> Arbitraries.integers().between(0, 59)
                                                                .flatMap(minute -> Arbitraries.integers().between(0, 59)
                                                                        .map(second -> {
                                                                            ChatMessage msg = new ChatMessage();
                                                                            msg.setId(id);
                                                                            msg.setConversationId(1L);
                                                                            msg.setSenderId(senderId);
                                                                            msg.setContentType("text");
                                                                            msg.setContent("msg-" + id);
                                                                            msg.setSendTime(LocalDateTime.of(year, month, day, hour, minute, second));
                                                                            return msg;
                                                                        }))))))));

        return messageArb.list().ofMinSize(0).ofMaxSize(50)
                .map(list -> {
                    // Ensure unique IDs by deduplicating
                    Map<Long, ChatMessage> byId = new LinkedHashMap<>();
                    for (ChatMessage m : list) {
                        byId.putIfAbsent(m.getId(), m);
                    }
                    return new ArrayList<>(byId.values());
                });
    }

    @Provide
    Arbitrary<Long> optionalCursor() {
        return Arbitraries.longs().between(1, 10000);
    }

    // ========== Property 15.1: Results are ordered by sendTime ascending ==========

    /**
     * For any conversation's message history query, returned messages should be ordered by sendTime ascending.
     *
     * Feature: mars-integration-optimization, Property 15: 游标分页消息查询有序性
     * **Validates: Requirements 10.9**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_15_游标分页消息查询有序性")
    void messagesReturnedInSendTimeAscendingOrder(
            @ForAll("randomMessages") List<ChatMessage> allMessages
    ) throws Exception {
        List<ChatMessage> expectedResult = simulateMapperQuery(allMessages, null);

        ChatMessageMapper mockMapper = Mockito.mock(ChatMessageMapper.class);
        when(mockMapper.selectByConversationId(eq(1L), isNull(), eq(PAGE_SIZE)))
                .thenReturn(expectedResult);

        ChatMessageService service = createServiceWithMocks(mockMapper);
        List<ChatMessage> result = service.getMessages(1L, null);

        // Verify ascending sendTime order
        for (int i = 1; i < result.size(); i++) {
            assertThat(result.get(i).getSendTime())
                    .isAfterOrEqualTo(result.get(i - 1).getSendTime());
        }
    }

    // ========== Property 15.2: Page size is at most 20 ==========

    /**
     * For any query, the result size should be at most PAGE_SIZE (20).
     *
     * Feature: mars-integration-optimization, Property 15: 游标分页消息查询有序性
     * **Validates: Requirements 10.9**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_15_游标分页消息查询有序性")
    void pageSizeIsAtMost20(
            @ForAll("randomMessages") List<ChatMessage> allMessages
    ) throws Exception {
        List<ChatMessage> expectedResult = simulateMapperQuery(allMessages, null);

        ChatMessageMapper mockMapper = Mockito.mock(ChatMessageMapper.class);
        when(mockMapper.selectByConversationId(eq(1L), isNull(), eq(PAGE_SIZE)))
                .thenReturn(expectedResult);

        ChatMessageService service = createServiceWithMocks(mockMapper);
        List<ChatMessage> result = service.getMessages(1L, null);

        assertThat(result).hasSizeLessThanOrEqualTo(PAGE_SIZE);
    }

    // ========== Property 15.3: With cursor, all returned message IDs < cursor ==========

    /**
     * When a cursor value is given, all returned messages' IDs should be less than the cursor,
     * and their sendTime should be consistent with the ordering.
     *
     * Feature: mars-integration-optimization, Property 15: 游标分页消息查询有序性
     * **Validates: Requirements 10.9**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_15_游标分页消息查询有序性")
    void withCursor_allReturnedMessageIdsLessThanCursor(
            @ForAll("randomMessages") List<ChatMessage> allMessages,
            @ForAll("optionalCursor") Long cursor
    ) throws Exception {
        List<ChatMessage> expectedResult = simulateMapperQuery(allMessages, cursor);

        ChatMessageMapper mockMapper = Mockito.mock(ChatMessageMapper.class);
        when(mockMapper.selectByConversationId(eq(1L), eq(cursor), eq(PAGE_SIZE)))
                .thenReturn(expectedResult);

        ChatMessageService service = createServiceWithMocks(mockMapper);
        List<ChatMessage> result = service.getMessages(1L, cursor);

        // All returned message IDs must be less than cursor
        for (ChatMessage msg : result) {
            assertThat(msg.getId()).isLessThan(cursor);
        }

        // Results should still be in sendTime ascending order
        for (int i = 1; i < result.size(); i++) {
            assertThat(result.get(i).getSendTime())
                    .isAfterOrEqualTo(result.get(i - 1).getSendTime());
        }

        // Page size constraint still holds
        assertThat(result).hasSizeLessThanOrEqualTo(PAGE_SIZE);
    }
}

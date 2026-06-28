package com.dataplatform;

import com.dataplatform.message.entity.ChatConversation;
import com.dataplatform.message.mapper.ChatConversationMapper;
import com.dataplatform.message.mapper.ChatConversationMemberMapper;
import com.dataplatform.message.service.ConversationService;
import net.jqwik.api.*;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 属性测试：会话列表排序
 *
 * Feature: mars-integration-optimization, Property 16: 会话列表排序
 * **Validates: Requirements 10.10**
 */
class ConversationListOrderPropertyTest {

    // ========== Helper methods ==========

    private ConversationService createServiceWithMocks(
            ChatConversationMapper conversationMapper,
            ChatConversationMemberMapper memberMapper
    ) throws Exception {
        ConversationService service = new ConversationService();

        Field convMapperField = ConversationService.class.getDeclaredField("conversationMapper");
        convMapperField.setAccessible(true);
        convMapperField.set(service, conversationMapper);

        Field memberMapperField = ConversationService.class.getDeclaredField("memberMapper");
        memberMapperField.setAccessible(true);
        memberMapperField.set(service, memberMapper);

        return service;
    }

    /**
     * Simulates the SQL behavior: sort conversations by lastMessageTime DESC.
     */
    private List<ChatConversation> simulateMapperQuery(List<ChatConversation> conversations) {
        return conversations.stream()
                .sorted(Comparator.comparing(
                        ChatConversation::getLastMessageTime,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .collect(Collectors.toList());
    }

    // ========== Generators ==========

    @Provide
    Arbitrary<List<ChatConversation>> randomConversations() {
        Arbitrary<ChatConversation> conversationArb = Arbitraries.longs().between(1, 10000)
                .flatMap(id -> Arbitraries.of("private", "group")
                        .flatMap(type -> Arbitraries.integers().between(2020, 2025)
                                .flatMap(year -> Arbitraries.integers().between(1, 12)
                                        .flatMap(month -> Arbitraries.integers().between(1, 28)
                                                .flatMap(day -> Arbitraries.integers().between(0, 23)
                                                        .flatMap(hour -> Arbitraries.integers().between(0, 59)
                                                                .flatMap(minute -> Arbitraries.integers().between(0, 59)
                                                                        .map(second -> {
                                                                            ChatConversation conv = new ChatConversation();
                                                                            conv.setId(id);
                                                                            conv.setType(type);
                                                                            conv.setName("conv-" + id);
                                                                            conv.setCreateTime(LocalDateTime.of(2020, 1, 1, 0, 0));
                                                                            conv.setLastMessageTime(LocalDateTime.of(year, month, day, hour, minute, second));
                                                                            return conv;
                                                                        }))))))));

        return conversationArb.list().ofMinSize(0).ofMaxSize(30)
                .map(list -> {
                    // Ensure unique IDs
                    Map<Long, ChatConversation> byId = new LinkedHashMap<>();
                    for (ChatConversation c : list) {
                        byId.putIfAbsent(c.getId(), c);
                    }
                    return new ArrayList<>(byId.values());
                });
    }

    // ========== Property 16: 会话列表排序 ==========

    /**
     * For any user's conversation list query, returned conversations should be
     * ordered by lastMessageTime descending.
     *
     * Feature: mars-integration-optimization, Property 16: 会话列表排序
     * **Validates: Requirements 10.10**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_16_会话列表排序")
    void conversationsReturnedInLastMessageTimeDescendingOrder(
            @ForAll("randomConversations") List<ChatConversation> allConversations
    ) throws Exception {
        Long userId = 1L;

        // Simulate the mapper returning conversations sorted by lastMessageTime DESC
        List<ChatConversation> sortedConversations = simulateMapperQuery(allConversations);

        ChatConversationMapper conversationMapper = Mockito.mock(ChatConversationMapper.class);
        ChatConversationMemberMapper memberMapper = Mockito.mock(ChatConversationMemberMapper.class);

        when(conversationMapper.selectByUserId(eq(userId))).thenReturn(sortedConversations);

        ConversationService service = createServiceWithMocks(conversationMapper, memberMapper);
        List<ChatConversation> result = service.getUserConversations(userId);

        // Verify descending lastMessageTime order
        for (int i = 1; i < result.size(); i++) {
            LocalDateTime prev = result.get(i - 1).getLastMessageTime();
            LocalDateTime curr = result.get(i).getLastMessageTime();
            if (prev != null && curr != null) {
                assertThat(curr).isBeforeOrEqualTo(prev);
            }
        }
    }

    /**
     * The result list size should match the number of conversations returned by the mapper.
     *
     * Feature: mars-integration-optimization, Property 16: 会话列表排序
     * **Validates: Requirements 10.10**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_16_会话列表排序")
    void conversationListSizeMatchesMapperResult(
            @ForAll("randomConversations") List<ChatConversation> allConversations
    ) throws Exception {
        Long userId = 42L;

        List<ChatConversation> sortedConversations = simulateMapperQuery(allConversations);

        ChatConversationMapper conversationMapper = Mockito.mock(ChatConversationMapper.class);
        ChatConversationMemberMapper memberMapper = Mockito.mock(ChatConversationMemberMapper.class);

        when(conversationMapper.selectByUserId(eq(userId))).thenReturn(sortedConversations);

        ConversationService service = createServiceWithMocks(conversationMapper, memberMapper);
        List<ChatConversation> result = service.getUserConversations(userId);

        assertThat(result).hasSize(sortedConversations.size());
    }
}

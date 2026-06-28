package com.dataplatform;

import com.dataplatform.message.entity.ChatConversation;
import com.dataplatform.message.mapper.ChatConversationMapper;
import com.dataplatform.message.mapper.ChatConversationMemberMapper;
import com.dataplatform.message.service.ConversationService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 属性测试：私聊会话幂等性
 *
 * Feature: mars-integration-optimization, Property 12: 私聊会话幂等性
 * **Validates: Requirements 10.2**
 */
class PrivateChatIdempotencePropertyTest {

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

    // ========== Property 12: 私聊会话幂等性 ==========

    /**
     * For any two distinct users A and B, calling createPrivateConversation twice
     * should return the same conversation ID. The first call creates a new conversation
     * (selectPrivateConversationId returns null), and the second call returns the
     * existing one (selectPrivateConversationId returns the created ID).
     *
     * Feature: mars-integration-optimization, Property 12: 私聊会话幂等性
     * **Validates: Requirements 10.2**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_12_私聊会话幂等性")
    void createPrivateConversation_calledTwice_returnsSameConversationId(
            @ForAll @LongRange(min = 1, max = 10000) Long userIdA,
            @ForAll @LongRange(min = 1, max = 10000) Long userIdB
    ) throws Exception {
        Assume.that(!userIdA.equals(userIdB));

        ChatConversationMapper conversationMapper = Mockito.mock(ChatConversationMapper.class);
        ChatConversationMemberMapper memberMapper = Mockito.mock(ChatConversationMemberMapper.class);

        // Use a fixed conversation ID for this test run
        final long conversationId = userIdA * 10000 + userIdB;

        // First call: no existing conversation, so a new one is created
        // Second call: existing conversation found
        when(memberMapper.selectPrivateConversationId(eq(userIdA), eq(userIdB)))
                .thenReturn(null)       // first call
                .thenReturn(conversationId);  // second call

        // When insert is called, simulate setting the ID on the conversation entity
        doAnswer(invocation -> {
            ChatConversation conv = invocation.getArgument(0);
            conv.setId(conversationId);
            return 1;
        }).when(conversationMapper).insert(any(ChatConversation.class));

        when(memberMapper.batchInsert(any())).thenReturn(2);

        // For the second call, selectById returns the existing conversation
        ChatConversation existingConversation = new ChatConversation();
        existingConversation.setId(conversationId);
        existingConversation.setType("private");
        existingConversation.setCreateTime(LocalDateTime.now());
        when(conversationMapper.selectById(conversationId)).thenReturn(existingConversation);

        ConversationService service = createServiceWithMocks(conversationMapper, memberMapper);

        // First call: creates new conversation
        ChatConversation result1 = service.createPrivateConversation(userIdA, userIdB);
        // Second call: returns existing conversation
        ChatConversation result2 = service.createPrivateConversation(userIdA, userIdB);

        // Both calls should return the same conversation ID
        assertThat(result1.getId()).isEqualTo(conversationId);
        assertThat(result2.getId()).isEqualTo(conversationId);
        assertThat(result1.getId()).isEqualTo(result2.getId());

        // insert should only be called once (first call)
        verify(conversationMapper, times(1)).insert(any(ChatConversation.class));
        // batchInsert should only be called once (first call)
        verify(memberMapper, times(1)).batchInsert(any());
        // selectPrivateConversationId should be called twice
        verify(memberMapper, times(2)).selectPrivateConversationId(eq(userIdA), eq(userIdB));
    }
}

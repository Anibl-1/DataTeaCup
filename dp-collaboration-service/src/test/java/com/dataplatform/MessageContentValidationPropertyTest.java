package com.dataplatform;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.message.entity.ChatMessage;
import com.dataplatform.message.mapper.ChatConversationMapper;
import com.dataplatform.message.mapper.ChatConversationMemberMapper;
import com.dataplatform.message.mapper.ChatMessageMapper;
import com.dataplatform.message.service.ChatMessageService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 属性测试：消息内容校验
 *
 * Feature: mars-integration-optimization, Property 14: 消息内容校验
 * **Validates: Requirements 10.4, 10.5, 10.6**
 */
class MessageContentValidationPropertyTest {

    // ========== Helper methods ==========

    private ChatMessageService createServiceWithMocks(
            ChatMessageMapper messageMapper,
            ChatConversationMapper conversationMapper,
            ChatConversationMemberMapper memberMapper
    ) throws Exception {
        ChatMessageService service = new ChatMessageService();

        Field msgField = ChatMessageService.class.getDeclaredField("chatMessageMapper");
        msgField.setAccessible(true);
        msgField.set(service, messageMapper);

        Field convField = ChatMessageService.class.getDeclaredField("conversationMapper");
        convField.setAccessible(true);
        convField.set(service, conversationMapper);

        Field memField = ChatMessageService.class.getDeclaredField("memberMapper");
        memField.setAccessible(true);
        memField.set(service, memberMapper);

        return service;
    }

    private ChatMessageService createDefaultService() throws Exception {
        return createServiceWithMocks(
                Mockito.mock(ChatMessageMapper.class),
                Mockito.mock(ChatConversationMapper.class),
                Mockito.mock(ChatConversationMemberMapper.class)
        );
    }

    private ChatMessageService createSuccessService() throws Exception {
        ChatMessageMapper messageMapper = Mockito.mock(ChatMessageMapper.class);
        ChatConversationMapper conversationMapper = Mockito.mock(ChatConversationMapper.class);
        ChatConversationMemberMapper memberMapper = Mockito.mock(ChatConversationMemberMapper.class);

        doAnswer(invocation -> {
            ChatMessage msg = invocation.getArgument(0);
            msg.setId(1L);
            return 1;
        }).when(messageMapper).insert(any(ChatMessage.class));

        when(conversationMapper.updateLastMessageTime(anyLong())).thenReturn(1);
        when(memberMapper.incrementUnreadCount(anyLong(), anyLong())).thenReturn(1);

        return createServiceWithMocks(messageMapper, conversationMapper, memberMapper);
    }

    // ========== Property 14.1: Text message with null/blank content → BusinessException 400 ==========

    /**
     * Text messages with null content should be rejected with BusinessException code 400.
     *
     * Feature: mars-integration-optimization, Property 14: 消息内容校验
     * **Validates: Requirements 10.4, 10.5, 10.6**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_14_消息内容校验")
    void textMessage_nullContent_shouldBeRejected() throws Exception {
        ChatMessageService service = createDefaultService();

        BusinessException ex = catchThrowableOfType(
                () -> service.sendMessage(1L, 1L, "text", null, null, null, null),
                BusinessException.class
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getCode()).isEqualTo(400);
    }

    /**
     * Text messages with blank/whitespace-only content should be rejected with BusinessException code 400.
     *
     * Feature: mars-integration-optimization, Property 14: 消息内容校验
     * **Validates: Requirements 10.4, 10.5, 10.6**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_14_消息内容校验")
    void textMessage_blankContent_shouldBeRejected(
            @ForAll("blankStrings") String content
    ) throws Exception {
        ChatMessageService service = createDefaultService();

        BusinessException ex = catchThrowableOfType(
                () -> service.sendMessage(1L, 1L, "text", content, null, null, null),
                BusinessException.class
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getCode()).isEqualTo(400);
    }

    @Provide
    Arbitrary<String> blankStrings() {
        return Arbitraries.of("", " ", "  ", "\t", "\n", "   \t\n  ");
    }

    // ========== Property 14.2: Text message with content > 5000 chars → BusinessException 400 ==========

    /**
     * Text messages with content exceeding 5000 characters (after trim) should be rejected
     * with BusinessException code 400.
     *
     * Feature: mars-integration-optimization, Property 14: 消息内容校验
     * **Validates: Requirements 10.4, 10.5, 10.6**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_14_消息内容校验")
    void textMessage_contentExceeding5000Chars_shouldBeRejected(
            @ForAll @IntRange(min = 5001, max = 6000) int length
    ) throws Exception {
        ChatMessageService service = createDefaultService();
        String content = "a".repeat(length);

        BusinessException ex = catchThrowableOfType(
                () -> service.sendMessage(1L, 1L, "text", content, null, null, null),
                BusinessException.class
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getCode()).isEqualTo(400);
    }

    // ========== Property 14.3: Text message with valid content (1-5000 chars) → success ==========

    /**
     * Text messages with valid non-blank content of 1-5000 characters should succeed.
     *
     * Feature: mars-integration-optimization, Property 14: 消息内容校验
     * **Validates: Requirements 10.4, 10.5, 10.6**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_14_消息内容校验")
    void textMessage_validContent_shouldSucceed(
            @ForAll("validTextContent") String content
    ) throws Exception {
        ChatMessageService service = createSuccessService();

        ChatMessage result = service.sendMessage(1L, 1L, "text", content, null, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getContentType()).isEqualTo("text");
        assertThat(result.getContent()).isEqualTo(content);
    }

    @Provide
    Arbitrary<String> validTextContent() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(5000)
                .alpha()
                .filter(s -> !s.trim().isEmpty());
    }

    // ========== Property 14.4: Image/file message with null/blank fileUrl → BusinessException 400 ==========

    /**
     * Image or file messages with null fileUrl should be rejected with BusinessException code 400.
     *
     * Feature: mars-integration-optimization, Property 14: 消息内容校验
     * **Validates: Requirements 10.4, 10.5, 10.6**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_14_消息内容校验")
    void imageOrFileMessage_nullFileUrl_shouldBeRejected(
            @ForAll("imageOrFileType") String contentType
    ) throws Exception {
        ChatMessageService service = createDefaultService();

        BusinessException ex = catchThrowableOfType(
                () -> service.sendMessage(1L, 1L, contentType, null, null, null, null),
                BusinessException.class
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getCode()).isEqualTo(400);
    }

    /**
     * Image or file messages with blank fileUrl should be rejected with BusinessException code 400.
     *
     * Feature: mars-integration-optimization, Property 14: 消息内容校验
     * **Validates: Requirements 10.4, 10.5, 10.6**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_14_消息内容校验")
    void imageOrFileMessage_blankFileUrl_shouldBeRejected(
            @ForAll("imageOrFileType") String contentType,
            @ForAll("blankStrings") String fileUrl
    ) throws Exception {
        ChatMessageService service = createDefaultService();

        BusinessException ex = catchThrowableOfType(
                () -> service.sendMessage(1L, 1L, contentType, null, fileUrl, null, null),
                BusinessException.class
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getCode()).isEqualTo(400);
    }

    @Provide
    Arbitrary<String> imageOrFileType() {
        return Arbitraries.of("image", "file");
    }

    // ========== Property 14.5: Image/file message with valid fileUrl → success ==========

    /**
     * Image or file messages with a valid (non-blank) fileUrl should succeed.
     *
     * Feature: mars-integration-optimization, Property 14: 消息内容校验
     * **Validates: Requirements 10.4, 10.5, 10.6**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_14_消息内容校验")
    void imageOrFileMessage_validFileUrl_shouldSucceed(
            @ForAll("imageOrFileType") String contentType,
            @ForAll("validFileUrl") String fileUrl
    ) throws Exception {
        ChatMessageService service = createSuccessService();

        ChatMessage result = service.sendMessage(1L, 1L, contentType, null, fileUrl, "test.png", 1024L);

        assertThat(result).isNotNull();
        assertThat(result.getContentType()).isEqualTo(contentType);
        assertThat(result.getFileUrl()).isEqualTo(fileUrl);
    }

    @Provide
    Arbitrary<String> validFileUrl() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(200)
                .alpha()
                .map(s -> "/uploads/" + s + ".png")
                .filter(s -> !s.isBlank());
    }
}

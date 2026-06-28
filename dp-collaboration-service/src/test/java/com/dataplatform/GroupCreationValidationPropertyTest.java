package com.dataplatform;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.message.entity.ChatConversation;
import com.dataplatform.message.mapper.ChatConversationMapper;
import com.dataplatform.message.mapper.ChatConversationMemberMapper;
import com.dataplatform.message.service.ConversationService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 属性测试：群组创建校验
 *
 * Feature: mars-integration-optimization, Property 13: 群组创建校验
 * **Validates: Requirements 10.3**
 */
class GroupCreationValidationPropertyTest {

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

    private ConversationService createDefaultService() throws Exception {
        ChatConversationMapper conversationMapper = Mockito.mock(ChatConversationMapper.class);
        ChatConversationMemberMapper memberMapper = Mockito.mock(ChatConversationMemberMapper.class);
        return createServiceWithMocks(conversationMapper, memberMapper);
    }

    private List<Long> generateMemberIds(int count) {
        return LongStream.rangeClosed(1, count).boxed().collect(Collectors.toList());
    }

    // ========== Property 13.1: Null group name → BusinessException 400 ==========

    /**
     * A null group name should always be rejected with BusinessException code 400.
     *
     * Feature: mars-integration-optimization, Property 13: 群组创建校验
     * **Validates: Requirements 10.3**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_13_群组创建校验")
    void nullGroupName_shouldBeRejected(
            @ForAll @IntRange(min = 2, max = 10) int memberCount
    ) throws Exception {
        ConversationService service = createDefaultService();
        List<Long> memberIds = generateMemberIds(memberCount);

        BusinessException ex = catchThrowableOfType(
                () -> service.createGroupConversation(null, memberIds),
                BusinessException.class
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getCode()).isEqualTo(400);
    }

    // ========== Property 13.2: Empty/blank group name → BusinessException 400 ==========

    /**
     * An empty or whitespace-only group name should be rejected with BusinessException code 400.
     *
     * Feature: mars-integration-optimization, Property 13: 群组创建校验
     * **Validates: Requirements 10.3**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_13_群组创建校验")
    void emptyOrBlankGroupName_shouldBeRejected(
            @ForAll("blankStrings") String name,
            @ForAll @IntRange(min = 2, max = 10) int memberCount
    ) throws Exception {
        ConversationService service = createDefaultService();
        List<Long> memberIds = generateMemberIds(memberCount);

        BusinessException ex = catchThrowableOfType(
                () -> service.createGroupConversation(name, memberIds),
                BusinessException.class
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getCode()).isEqualTo(400);
    }

    @Provide
    Arbitrary<String> blankStrings() {
        return Arbitraries.of("", " ", "  ", "\t", "\n", "   \t\n  ");
    }

    // ========== Property 13.3: Group name > 50 chars → BusinessException 400 ==========

    /**
     * A group name exceeding 50 characters should be rejected with BusinessException code 400.
     *
     * Feature: mars-integration-optimization, Property 13: 群组创建校验
     * **Validates: Requirements 10.3**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_13_群组创建校验")
    void groupNameExceeding50Chars_shouldBeRejected(
            @ForAll @StringLength(min = 51, max = 200) String name,
            @ForAll @IntRange(min = 2, max = 10) int memberCount
    ) throws Exception {
        ConversationService service = createDefaultService();
        List<Long> memberIds = generateMemberIds(memberCount);

        BusinessException ex = catchThrowableOfType(
                () -> service.createGroupConversation(name, memberIds),
                BusinessException.class
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getCode()).isEqualTo(400);
    }

    // ========== Property 13.4: Members null or < 2 → BusinessException 400 ==========

    /**
     * A null member list or a list with fewer than 2 members should be rejected
     * with BusinessException code 400.
     *
     * Feature: mars-integration-optimization, Property 13: 群组创建校验
     * **Validates: Requirements 10.3**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_13_群组创建校验")
    void membersTooFew_shouldBeRejected(
            @ForAll("validGroupName") String name,
            @ForAll("tooFewMembers") List<Long> memberIds
    ) throws Exception {
        ConversationService service = createDefaultService();

        BusinessException ex = catchThrowableOfType(
                () -> service.createGroupConversation(name, memberIds),
                BusinessException.class
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getCode()).isEqualTo(400);
    }

    @Property(tries = 10)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_13_群组创建校验")
    void nullMembers_shouldBeRejected(
            @ForAll("validGroupName") String name
    ) throws Exception {
        ConversationService service = createDefaultService();

        BusinessException ex = catchThrowableOfType(
                () -> service.createGroupConversation(name, null),
                BusinessException.class
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getCode()).isEqualTo(400);
    }

    @Provide
    Arbitrary<List<Long>> tooFewMembers() {
        return Arbitraries.of(
                Collections.emptyList(),
                Collections.singletonList(1L)
        );
    }

    // ========== Property 13.5: Members > 200 → BusinessException 400 ==========

    /**
     * A member list exceeding 200 members should be rejected with BusinessException code 400.
     *
     * Feature: mars-integration-optimization, Property 13: 群组创建校验
     * **Validates: Requirements 10.3**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_13_群组创建校验")
    void membersExceeding200_shouldBeRejected(
            @ForAll("validGroupName") String name,
            @ForAll @IntRange(min = 201, max = 300) int memberCount
    ) throws Exception {
        ConversationService service = createDefaultService();
        List<Long> memberIds = generateMemberIds(memberCount);

        BusinessException ex = catchThrowableOfType(
                () -> service.createGroupConversation(name, memberIds),
                BusinessException.class
        );

        assertThat(ex).isNotNull();
        assertThat(ex.getCode()).isEqualTo(400);
    }

    // ========== Property 13.6: Valid name + valid members → success ==========

    /**
     * A valid group name (1-50 chars, non-blank) with valid member count (2-200)
     * should succeed without throwing any exception.
     *
     * Feature: mars-integration-optimization, Property 13: 群组创建校验
     * **Validates: Requirements 10.3**
     */
    @Property(tries = 100)
    @Tag("Feature_mars-integration-optimization")
    @Tag("Property_13_群组创建校验")
    void validNameAndMembers_shouldSucceed(
            @ForAll("validGroupName") String name,
            @ForAll @IntRange(min = 2, max = 200) int memberCount
    ) throws Exception {
        ChatConversationMapper conversationMapper = Mockito.mock(ChatConversationMapper.class);
        ChatConversationMemberMapper memberMapper = Mockito.mock(ChatConversationMemberMapper.class);

        doAnswer(invocation -> {
            ChatConversation conv = invocation.getArgument(0);
            conv.setId(1L);
            return 1;
        }).when(conversationMapper).insert(any(ChatConversation.class));

        when(memberMapper.batchInsert(any())).thenReturn(memberCount);

        ConversationService service = createServiceWithMocks(conversationMapper, memberMapper);
        List<Long> memberIds = generateMemberIds(memberCount);

        ChatConversation result = service.createGroupConversation(name, memberIds);

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo("group");
        assertThat(result.getName()).isEqualTo(name);
        verify(conversationMapper, times(1)).insert(any(ChatConversation.class));
        verify(memberMapper, times(1)).batchInsert(any());
    }

    // ========== Providers ==========

    @Provide
    Arbitrary<String> validGroupName() {
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(50)
                .alpha()
                .filter(s -> !s.trim().isEmpty());
    }
}

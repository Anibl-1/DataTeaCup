package com.dataplatform.message.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.message.entity.ChatConversation;
import com.dataplatform.message.entity.ChatConversationMember;
import com.dataplatform.message.mapper.ChatConversationMapper;
import com.dataplatform.message.mapper.ChatConversationMemberMapper;
import com.dataplatform.message.mapper.ChatMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话服务
 * 负责私聊会话创建（去重）、群组会话创建（校验）、会话列表查询
 */
@Service
public class ConversationService {

    @Autowired
    private ChatConversationMapper conversationMapper;

    @Autowired
    private ChatConversationMemberMapper memberMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    /**
     * 创建私聊会话（幂等：已存在则返回已有会话）
     *
     * @param userIdA 用户 A 的 ID
     * @param userIdB 用户 B 的 ID
     * @return 会话对象
     */
    @Transactional
    public ChatConversation createPrivateConversation(Long userIdA, Long userIdB) {
        if (userIdA == null || userIdB == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        if (userIdA.equals(userIdB)) {
            throw new BusinessException(400, "不能与自己创建私聊");
        }

        // 查询是否已存在私聊会话
        Long existingId = memberMapper.selectPrivateConversationId(userIdA, userIdB);
        if (existingId != null) {
            return conversationMapper.selectById(existingId);
        }

        // 创建新的私聊会话
        ChatConversation conversation = new ChatConversation();
        conversation.setType("private");
        conversation.setCreateTime(LocalDateTime.now());
        conversationMapper.insert(conversation);

        // 添加两个成员
        LocalDateTime now = LocalDateTime.now();
        List<ChatConversationMember> members = new ArrayList<>();

        ChatConversationMember memberA = new ChatConversationMember();
        memberA.setConversationId(conversation.getId());
        memberA.setUserId(userIdA);
        memberA.setUnreadCount(0);
        memberA.setJoinTime(now);
        members.add(memberA);

        ChatConversationMember memberB = new ChatConversationMember();
        memberB.setConversationId(conversation.getId());
        memberB.setUserId(userIdB);
        memberB.setUnreadCount(0);
        memberB.setJoinTime(now);
        members.add(memberB);

        memberMapper.batchInsert(members);

        return conversation;
    }

    /**
     * 创建群组会话
     *
     * @param name      群组名称（非空且≤50字符）
     * @param memberIds 成员用户 ID 列表（≥2人且≤200人）
     * @return 会话对象
     */
    @Transactional
    public ChatConversation createGroupConversation(String name, List<Long> memberIds) {
        // 校验群名
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(400, "群组名称不能为空");
        }
        if (name.length() > 50) {
            throw new BusinessException(400, "群组名称不能超过50个字符");
        }

        // 校验成员数量
        if (memberIds == null || memberIds.size() < 2) {
            throw new BusinessException(400, "群组成员至少需要2人");
        }
        if (memberIds.size() > 200) {
            throw new BusinessException(400, "群组成员不能超过200人");
        }

        // 创建群组会话
        ChatConversation conversation = new ChatConversation();
        conversation.setType("group");
        conversation.setName(name);
        conversation.setCreateTime(LocalDateTime.now());
        conversationMapper.insert(conversation);

        // 批量添加成员
        LocalDateTime now = LocalDateTime.now();
        List<ChatConversationMember> members = new ArrayList<>();
        for (Long userId : memberIds) {
            ChatConversationMember member = new ChatConversationMember();
            member.setConversationId(conversation.getId());
            member.setUserId(userId);
            member.setUnreadCount(0);
            member.setJoinTime(now);
            members.add(member);
        }
        memberMapper.batchInsert(members);

        return conversation;
    }

    /**
     * 查询用户的会话列表（按 lastMessageTime 降序）
     *
     * @param userId 用户 ID
     * @return 会话列表
     */
    public List<ChatConversation> getUserConversations(Long userId) {
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        List<ChatConversation> conversations = conversationMapper.selectByUserId(userId);
        // 填充每个会话的 lastMessage、unreadCount、members
        for (ChatConversation conv : conversations) {
            try {
                // 最后一条消息内容
                conv.setLastMessage(chatMessageMapper.selectLastContent(conv.getId()));
                // 成员列表（带昵称）
                List<ChatConversationMember> members = memberMapper.selectMembersWithNickname(conv.getId());
                conv.setMembers(members);
                // 当前用户的未读数
                conv.setUnreadCount(members.stream()
                        .filter(m -> m.getUserId().equals(userId))
                        .map(ChatConversationMember::getUnreadCount)
                        .findFirst().orElse(0));
            } catch (Exception e) {
                // 填充失败不影响会话列表返回
                conv.setUnreadCount(0);
            }
        }
        return conversations;
    }

    /**
     * 标记会话已读（重置未读计数）
     */
    public void markAsRead(Long conversationId, Long userId) {
        memberMapper.resetUnreadCount(conversationId, userId);
    }
}

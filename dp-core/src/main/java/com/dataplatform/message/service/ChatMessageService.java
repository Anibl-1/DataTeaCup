package com.dataplatform.message.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.message.entity.ChatMessage;
import com.dataplatform.message.mapper.ChatConversationMapper;
import com.dataplatform.message.mapper.ChatConversationMemberMapper;
import com.dataplatform.message.mapper.ChatMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息服务
 * 负责消息发送（含校验）、游标分页查询消息历史。
 * DB 操作完成后返回消息实体，WebSocket 推送由调用方（ChatController）处理。
 */
@Service
public class ChatMessageService {

    private static final int PAGE_SIZE = 20;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private ChatConversationMapper conversationMapper;

    @Autowired
    private ChatConversationMemberMapper memberMapper;

    /**
     * 发送消息
     * <p>
     * 1. 根据 contentType 校验消息内容
     * 2. 创建 ChatMessage 实体并插入数据库
     * 3. 更新会话的 lastMessageTime
     * 4. 为非发送者的会话成员增加 unreadCount
     * 5. 返回创建的消息（调用方负责 WebSocket 推送）
     *
     * @param senderId       发送者 ID
     * @param conversationId 会话 ID
     * @param contentType    消息类型：text / image / file
     * @param content        消息内容（text 类型必填）
     * @param fileUrl        文件 URL（image/file 类型必填）
     * @param fileName       文件名
     * @param fileSize       文件大小（字节）
     * @return 创建的消息实体
     */
    @Transactional
    public ChatMessage sendMessage(Long senderId, Long conversationId, String contentType,
                                   String content, String fileUrl, String fileName, Long fileSize) {
        // 校验消息内容
        validateMessage(contentType, content, fileUrl);

        // 创建消息实体
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setContentType(contentType);
        message.setContent(content);
        message.setFileUrl(fileUrl);
        message.setFileName(fileName);
        message.setFileSize(fileSize);
        message.setSendTime(LocalDateTime.now());

        chatMessageMapper.insert(message);

        // 更新会话最后消息时间
        conversationMapper.updateLastMessageTime(conversationId);

        // 为非发送者的会话成员增加未读计数
        memberMapper.incrementUnreadCount(conversationId, senderId);

        return message;
    }

    /**
     * 游标分页查询消息历史（每页 20 条，按 sendTime 升序）
     *
     * @param conversationId 会话 ID
     * @param cursor         游标（上一页最后一条消息的 id），首次传 null
     * @return 消息列表
     */
    public List<ChatMessage> getMessages(Long conversationId, Long cursor) {
        return chatMessageMapper.selectByConversationId(conversationId, cursor, PAGE_SIZE);
    }

    /**
     * 根据消息类型校验内容
     */
    private void validateMessage(String contentType, String content, String fileUrl) {
        if (contentType == null || contentType.isBlank()) {
            throw new BusinessException(400, "消息类型不能为空");
        }

        switch (contentType) {
            case "text":
                if (content == null || content.trim().isEmpty()) {
                    throw new BusinessException(400, "消息内容不能为空");
                }
                if (content.trim().length() > 5000) {
                    throw new BusinessException(400, "消息内容不能超过5000个字符");
                }
                break;
            case "image":
            case "file":
                if (fileUrl == null || fileUrl.isBlank()) {
                    throw new BusinessException(400, "文件URL不能为空");
                }
                break;
            default:
                throw new BusinessException(400, "不支持的消息类型: " + contentType);
        }
    }
}

package com.dataplatform.data.service.comment;

import com.dataplatform.data.mapper.CommentMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 评论服务 - 数据库持久化
 * 需求: 23.1, 23.2, 23.3, 23.6, 23.7
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");
    private static final ObjectMapper JSON = new ObjectMapper();

    public Comment addComment(Comment comment) {
        if (comment.getId() == null) {
            comment.setId(UUID.randomUUID().toString().substring(0, 12));
        }
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());

        List<String> mentions = parseMentions(comment.getContent());
        comment.setMentions(mentions);
        comment.setMentionsJson(toJson(mentions));

        commentMapper.insertComment(comment);
        log.info("[评论] 新增: id={}, resource={}/{}", comment.getId(),
                comment.getResourceType(), comment.getResourceId());
        return comment;
    }

    public Comment updateComment(String commentId, String content) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment != null) {
            comment.setContent(content);
            List<String> mentions = parseMentions(content);
            comment.setMentions(mentions);
            comment.setMentionsJson(toJson(mentions));
            comment.setUpdatedAt(LocalDateTime.now());
            commentMapper.updateComment(comment);
        }
        return comment;
    }

    public void deleteComment(String commentId) {
        commentMapper.deleteComment(commentId);
    }

    public void resolveComment(String commentId) {
        commentMapper.resolveComment(commentId);
    }

    public List<Comment> getComments(String resourceType, String resourceId) {
        List<Comment> comments = commentMapper.selectByResource(resourceType, resourceId);
        comments.forEach(this::parseMentionsFromJson);
        return comments;
    }

    public List<Comment> getReplies(String parentId) {
        List<Comment> replies = commentMapper.selectReplies(parentId);
        replies.forEach(this::parseMentionsFromJson);
        return replies;
    }

    // ========== 标注 ==========

    public Annotation addAnnotation(Annotation annotation) {
        if (annotation.getId() == null) {
            annotation.setId(UUID.randomUUID().toString().substring(0, 12));
        }
        annotation.setCreatedAt(LocalDateTime.now());
        commentMapper.insertAnnotation(annotation);
        return annotation;
    }

    public void deleteAnnotation(String annotationId) {
        commentMapper.deleteAnnotation(annotationId);
    }

    public List<Annotation> getAnnotations(String chartId) {
        return commentMapper.selectAnnotationsByChart(chartId);
    }

    private List<String> parseMentions(String content) {
        if (content == null) return Collections.emptyList();
        List<String> mentions = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            mentions.add(matcher.group(1));
        }
        return mentions;
    }

    private String toJson(List<String> list) {
        try { return JSON.writeValueAsString(list); } catch (Exception e) { return "[]"; }
    }

    private void parseMentionsFromJson(Comment comment) {
        if (comment.getMentionsJson() != null && !comment.getMentionsJson().isEmpty()) {
            try {
                comment.setMentions(JSON.readValue(comment.getMentionsJson(), new TypeReference<List<String>>() {}));
            } catch (Exception e) {
                comment.setMentions(Collections.emptyList());
            }
        }
    }
}

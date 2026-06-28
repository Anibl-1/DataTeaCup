package com.dataplatform.collaboration.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.service.comment.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 璇勮涓庢爣娉ˋPI
 * 闇€姹? 23.1, 23.2, 23.4
 */
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@RequirePermission("comment:read")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public Result<Comment> addComment(@RequestBody Comment comment) {
        return Result.success(commentService.addComment(comment));
    }

    @PutMapping("/{commentId}")
    public Result<Comment> updateComment(@PathVariable String commentId, @RequestBody Comment comment) {
        return Result.success(commentService.updateComment(commentId, comment.getContent()));
    }

    @DeleteMapping("/{commentId}")
    public Result<Void> deleteComment(@PathVariable String commentId) {
        commentService.deleteComment(commentId);
        return Result.success();
    }

    @PostMapping("/{commentId}/resolve")
    public Result<Void> resolveComment(@PathVariable String commentId) {
        commentService.resolveComment(commentId);
        return Result.success();
    }

    @GetMapping
    public Result<List<Comment>> getComments(@RequestParam String resourceType, @RequestParam String resourceId) {
        return Result.success(commentService.getComments(resourceType, resourceId));
    }

    @GetMapping("/{commentId}/replies")
    public Result<List<Comment>> getReplies(@PathVariable String commentId) {
        return Result.success(commentService.getReplies(commentId));
    }

    // ========== 鏍囨敞 ==========

    @PostMapping("/annotations")
    public Result<Annotation> addAnnotation(@RequestBody Annotation annotation) {
        return Result.success(commentService.addAnnotation(annotation));
    }

    @DeleteMapping("/annotations/{annotationId}")
    public Result<Void> deleteAnnotation(@PathVariable String annotationId) {
        commentService.deleteAnnotation(annotationId);
        return Result.success();
    }

    @GetMapping("/annotations/{chartId}")
    public Result<List<Annotation>> getAnnotations(@PathVariable String chartId) {
        return Result.success(commentService.getAnnotations(chartId));
    }
}

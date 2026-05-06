package org.tongji.sse.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.tongji.sse.dto.ApiResponse;
import org.tongji.sse.dto.AudioCommentReplyResponse;
import org.tongji.sse.dto.AudioCommentResponse;
import org.tongji.sse.dto.AudioDetailsResponse;
import org.tongji.sse.enums.TargetType;
import org.tongji.sse.eventUtil.ProducesEvent;
import org.tongji.sse.service.CommentReplyService;
import org.tongji.sse.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final CommentReplyService commentReplyService;

    @ProducesEvent
    @PostMapping("/audio/comment/{audioId}")
    public ApiResponse<AudioCommentResponse> addComment(
            @PathVariable Long audioId,
            @RequestParam String content,
            HttpServletRequest request
    ) {
        return ApiResponse.success(commentService.addComment(audioId, content, request));
    }

    @GetMapping("/audio/comment/{audioId}")
    public ApiResponse<List<AudioCommentResponse>> getComments(
            @PathVariable Long audioId
    ) {
        return ApiResponse.success(commentService.getComments(audioId));
    }

    @ProducesEvent
    @PostMapping("/audio/reply/{commentId}")
    public ApiResponse<AudioCommentReplyResponse> addReply(
            @PathVariable Long commentId,
            @RequestParam String content,
            HttpServletRequest request
    ) {
        return ApiResponse.success(commentReplyService.addReply(commentId, content, request));
    }

    @GetMapping("/audio/reply/{commentId}")
    public ApiResponse<List<AudioCommentReplyResponse>> getReplies(
            @PathVariable Long commentId
    ) {
        return ApiResponse.success(commentReplyService.getReplies(commentId));
    }

    @DeleteMapping("/{type}/{id}")
    public ApiResponse<Void> delete(
            @PathVariable("id") Long targetId,
            @PathVariable("type") TargetType type,
            HttpServletRequest request
    ) {
        commentService.delete(targetId, type, request);
        return ApiResponse.success(null);
    }

    @GetMapping("/audio/{audioId}/detail")
    public ApiResponse<AudioDetailsResponse> getAudioDetails(
            @PathVariable Long audioId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(commentService.getAudioDetails(audioId, request));
    }

    @GetMapping("/comment/{commentId}")
    public ApiResponse<AudioCommentResponse> getComment(
            @PathVariable Long commentId,
            HttpServletRequest request
    ){
        return ApiResponse.success(commentService.getComment(commentId, request));
    }

    @GetMapping("/reply/{replyId}")
    public ApiResponse<AudioCommentReplyResponse> getReply(
            @PathVariable Long replyId,
            HttpServletRequest request
    ){
        return ApiResponse.success(commentReplyService.getReply(replyId, request));
    }
}

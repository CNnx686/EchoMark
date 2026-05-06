package org.tongji.sse.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.tongji.sse.dto.ApiResponse;
import org.tongji.sse.dto.LikeAudioResponse;
import org.tongji.sse.dto.LikeResponse;
import org.tongji.sse.enums.TargetType;
import org.tongji.sse.eventUtil.ProducesEvent;
import org.tongji.sse.service.LikeService;

@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService service;

    @ProducesEvent
    @PostMapping("/audio/{audioId}/like")
    public ApiResponse<LikeAudioResponse> toggleAudioLike(
            @PathVariable Long audioId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(service.toggleAudioLike(audioId, request));
    }

    @ProducesEvent
    @PostMapping("/{type}/{id}/like")
    public ApiResponse<LikeResponse> toggleCommentOrReplyLike(
            @PathVariable("id") Long targetId,
            @PathVariable("type") TargetType type,
            HttpServletRequest request
    ) {
        return ApiResponse.success(service.toggleCommentOrReplyLike(targetId, type, request));
    }

    @GetMapping("/audio/{audioId}/like")
    public ApiResponse<Long> getAudioLikeCount(
            @PathVariable Long audioId
    ) {
        return ApiResponse.success(service.getAudioLikeCount(audioId));
    }

    @GetMapping("/{type}/{id}/like")
    public ApiResponse<Long> getCommentOrReplyLikeCount(
            @PathVariable("id") Long targetId,
            @PathVariable("type") TargetType type
    ) {
        return ApiResponse.success(service.getCommentOrReplyLikeCount(type, targetId));
    }

    @GetMapping("/audio/{audioId}/like-status")
    public ApiResponse<Boolean> getAudioLikeStatus(
            @PathVariable Long audioId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(service.getAudioLikeStatus(audioId, request));
    }

    @GetMapping("/{type}/{id}/like-status")
    public ApiResponse<Boolean> getCommentOrReplyLikeStatus(
            @PathVariable("id") Long targetId,
            @PathVariable("type") TargetType type,
            HttpServletRequest request
    ) {
        return ApiResponse.success(service.getCommentOrReplyLikeStatus(type, targetId, request));
    }
}

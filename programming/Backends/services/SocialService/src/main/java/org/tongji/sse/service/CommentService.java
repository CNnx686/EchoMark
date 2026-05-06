package org.tongji.sse.service;

import jakarta.servlet.http.HttpServletRequest;
import org.tongji.sse.dto.AudioCommentResponse;
import org.tongji.sse.dto.AudioDetailsResponse;
import org.tongji.sse.enums.TargetType;

import java.util.List;

public interface CommentService {
    AudioCommentResponse addComment(Long audioId, String content, HttpServletRequest request);

    List<AudioCommentResponse> getComments(Long audioId);

    void delete(Long targetId, TargetType type, HttpServletRequest request);

    AudioDetailsResponse getAudioDetails(Long audioId, HttpServletRequest request);

    AudioCommentResponse getComment(Long commentId, HttpServletRequest request);
}

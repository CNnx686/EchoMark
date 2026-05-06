package org.tongji.sse.service;

import jakarta.servlet.http.HttpServletRequest;
import org.tongji.sse.dto.LikeAudioResponse;
import org.tongji.sse.dto.LikeResponse;
import org.tongji.sse.enums.TargetType;

public interface LikeService {
    LikeAudioResponse toggleAudioLike(Long audioId, HttpServletRequest request);
    LikeResponse toggleCommentOrReplyLike(Long targetId, TargetType type, HttpServletRequest request);
    Boolean getCommentOrReplyLikeStatus(TargetType type, Long targetId, HttpServletRequest request);
    Boolean getAudioLikeStatus(Long audioId, HttpServletRequest request);
    Long getAudioLikeCount(Long audioId);
    Long getCommentOrReplyLikeCount(TargetType type, Long targetId);
}

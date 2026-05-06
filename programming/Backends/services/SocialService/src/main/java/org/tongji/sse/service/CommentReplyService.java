package org.tongji.sse.service;

import jakarta.servlet.http.HttpServletRequest;
import org.tongji.sse.dto.AudioCommentReplyResponse;
import org.tongji.sse.enums.TargetType;

import java.util.List;

public interface CommentReplyService {
    AudioCommentReplyResponse addReply(Long commentId, String content, HttpServletRequest request);

    List<AudioCommentReplyResponse> getReplies(Long commentId);

    AudioCommentReplyResponse getReply(Long replyId, HttpServletRequest request);
}

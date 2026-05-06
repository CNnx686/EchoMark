package org.tongji.sse.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tongji.sse.dto.AudioCommentReplyResponse;
import org.tongji.sse.entity.Audio;
import org.tongji.sse.entity.AudioComment;
import org.tongji.sse.entity.AudioCommentReply;
import org.tongji.sse.entity.User;
import org.tongji.sse.eventUtil.EventBuilder;
import org.tongji.sse.eventUtil.EventPublisher;
import org.tongji.sse.eventUtil.enums.EventChannelEnum;
import org.tongji.sse.eventUtil.event.NotificationEvent;
import org.tongji.sse.exception.NotFoundException;
import org.tongji.sse.repository.AudioCommentReplyRepository;
import org.tongji.sse.repository.AudioCommentRepository;
import org.tongji.sse.repository.AudioRepository;
import org.tongji.sse.repository.UserRepository;
import org.tongji.sse.security.SecurityUtil;
import org.tongji.sse.service.CommentReplyService;
import org.tongji.sse.type.NotificationType;
import org.tongji.sse.type.TargetType;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentReplyServiceImpl implements CommentReplyService {

    private final AudioCommentReplyRepository replyRepository;
    private final AudioCommentRepository commentRepository;
    private final AudioRepository audioRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public AudioCommentReplyResponse addReply(Long commentId, String content, HttpServletRequest request) {
        Long userId = SecurityUtil.getUserIdOrThrow(request);

        // 获取评论并检查是否存在/未删除
        AudioComment comment = commentRepository.findById(commentId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new NotFoundException("评论未找到或已被删除"));

        // 检查评论对应的音频是否可用
        audioRepository.findById(comment.getAudioId())
                .filter(a -> !a.isDeleted() && "PUBLISHED".equals(a.getStatus()))
                .orElseThrow(() -> new NotFoundException("音频未找到或无法回复"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户未找到"));

        AudioCommentReply reply = AudioCommentReply.builder()
                .commentId(commentId)
                .userId(userId)
                .content(content)
                .createTime(LocalDateTime.now())
                .comment(comment)
                .user(user)
                .build();

        replyRepository.save(reply);

        NotificationEvent event = EventBuilder.builder()
                .actorUserId(userId)
                .receiverUserId(comment.getUserId())
                .targetId(commentId)
                .targetType(TargetType.COMMENT)
                .type(NotificationType.REPLY)
                .content("有人给你的评论回复了：" + content)
                .build();
        eventPublisher.register(event, EventChannelEnum.NOTIFICATION_REPLY);

        return AudioCommentReplyResponse.builder()
                .id(reply.getId())
                .audioId(reply.getComment().getAudioId())
                .commentId(reply.getCommentId())
                .userId(reply.getUserId())
                .content(reply.getContent())
                .username(reply.getUser().getUsername())
                .createTime(reply.getCreateTime())
                .build();
    }

    @Override
    public List<AudioCommentReplyResponse> getReplies(Long commentId) {
        // 获取评论并检查是否存在/未删除
        AudioComment comment = commentRepository.findById(commentId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new NotFoundException("评论未找到或已被删除"));

        // 检查评��对应的音频是否可用
        audioRepository.findById(comment.getAudioId())
                .filter(a -> !a.isDeleted() && "PUBLISHED".equals(a.getStatus()))
                .orElseThrow(() -> new NotFoundException("音频未找到或无法查看回复"));

        return replyRepository.findByCommentIdAndIsDeletedFalseOrderByCreateTimeAsc(commentId)
                .stream()
                .map(reply -> new AudioCommentReplyResponse(
                        reply.getId(),
                        reply.getComment().getAudioId(),
                        reply.getCommentId(),
                        reply.getUserId(),
                        reply.getContent(),
                        reply.getUser().getUsername(),
                        reply.getCreateTime()
                ))
                .toList();
    }

    @Override
    public AudioCommentReplyResponse getReply(Long replyId, HttpServletRequest request) {
        Long uid = SecurityUtil.getUserIdOrThrow(request);
        AudioCommentReply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new NotFoundException("回复内容未找到"));
        if(reply.getIsDeleted()){
            throw new NotFoundException("回复已被删除");
        }
        AudioComment comment = reply.getComment();
        if(comment.getIsDeleted()){
            throw new NotFoundException("所属评论已被删除");
        }
        Audio audio = comment.getAudio();
        if (audio.isDeleted()){
            throw new NotFoundException("所属音频已被删除");
        }
        if(!audio.getUserId().equals(uid) && !"PUBLISHED".equals(audio.getStatus())){
            throw new NotFoundException("相关音频不可见");
        }

        return AudioCommentReplyResponse.builder()
                .id(replyId)
                .audioId(audio.getId())
                .commentId(comment.getId())
                .userId(reply.getUserId())
                .content(reply.getContent())
                .username(reply.getUser().getUsername())
                .createTime(reply.getCreateTime())
                .build();
    }
}

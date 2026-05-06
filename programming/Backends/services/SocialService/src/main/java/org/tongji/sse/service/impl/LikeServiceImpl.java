package org.tongji.sse.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tongji.sse.dto.LikeAudioResponse;
import org.tongji.sse.dto.LikeResponse;
import org.tongji.sse.entity.*;
import org.tongji.sse.enums.TargetType;
import org.tongji.sse.eventUtil.EventBuilder;
import org.tongji.sse.eventUtil.EventPublisher;
import org.tongji.sse.eventUtil.enums.EventChannelEnum;
import org.tongji.sse.eventUtil.event.NotificationEvent;
import org.tongji.sse.eventUtil.event.UserBehaviorSignalEvent;
import org.tongji.sse.exception.NotFoundException;
import org.tongji.sse.repository.*;
import org.tongji.sse.security.SecurityUtil;
import org.tongji.sse.service.LikeService;
import org.tongji.sse.type.BehaviorTargetType;
import org.tongji.sse.type.BehaviorType;
import org.tongji.sse.type.NotificationType;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final AudioLikeRepository audioLikeRepository;
    private final AudioRepository audioRepository;
    private final UserRepository userRepository;
    private final CommentReplyLikeRepository commentReplyLikeRepository;
    private final AudioCommentRepository commentRepository;
    private final AudioCommentReplyRepository replyRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public LikeAudioResponse toggleAudioLike(Long audioId, HttpServletRequest request) {
        Long userId = SecurityUtil.getUserIdOrNull(request);

        var existing = audioLikeRepository.findByAudio_IdAndUser_Id(audioId, userId);

        boolean liked;

        if (existing.isPresent()) {
            // 取消点赞
            audioLikeRepository.delete(existing.get());
            liked = false;

        } else {
            // 创建点赞
            Audio audio = audioRepository.findById(audioId)
                    .orElseThrow(() -> new NotFoundException("音频未找到"));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("用户未找到"));

            AudioLike like = AudioLike.builder()
                    .audioId(audio.getId())
                    .userId(user.getId())
                    .user(user)
                    .audio(audio)
                    .build();

            audioLikeRepository.save(like);
            liked = true;

            // 注册通知事件
            NotificationEvent event = EventBuilder.builder()
                    .actorUserId(userId)
                    .receiverUserId(audio.getUserId())
                    .targetId(audioId)
                    .targetType(org.tongji.sse.type.TargetType.AUDIO)
                    .type(NotificationType.LIKE)
                    .content("有人给你的音频点赞了")
                    .build();
            eventPublisher.register(event, EventChannelEnum.NOTIFICATION_LIKE);

            UserBehaviorSignalEvent behaviorSignalEvent = UserBehaviorSignalEvent.builder()
                    .behaviorType(BehaviorType.LIKE)
                    .userId(userId)
                    .behaviorTargetType(BehaviorTargetType.AUDIO)
                    .targetId(audioId)
                    .tags(audio.getTags() == null ? new ArrayList<>() : new ArrayList<>(audio.getTags()))
                    .authorId(audio.getUserId())
                    .build();
            eventPublisher.register(behaviorSignalEvent, EventChannelEnum.BEHAVIOR_PUSH);
        }

        Long count = audioLikeRepository.countByAudio_Id(audioId);

        return new LikeAudioResponse(liked, count);
    }

    @Override
    @Transactional
    public LikeResponse toggleCommentOrReplyLike(Long targetId, TargetType type, HttpServletRequest request) {
        Long userId = SecurityUtil.getUserIdOrNull(request);

        validateTargetCanBeOperated(targetId, type);

        var existing = commentReplyLikeRepository.findByTargetIdAndUserIdAndTargetType(targetId, userId, type);

        boolean liked;
        if (existing.isPresent()) {
            commentReplyLikeRepository.delete(existing.get());
            liked = false;
        } else {
            CommentReplyLike like = CommentReplyLike.builder()
                    .targetId(targetId)
                    .userId(userId)
                    .targetType(type)
                    .build();
            commentReplyLikeRepository.save(like);
            liked = true;

            // 注册通知事件
            NotificationEvent event = EventBuilder.builder()
                    .actorUserId(userId)
                    .receiverUserId(
                            switch (type) {
                                case COMMENT -> commentRepository.findById(targetId)
                                        .orElseThrow(() -> new NotFoundException("评论未找到"))
                                        .getUserId();
                                case REPLY -> replyRepository.findById(targetId)
                                        .orElseThrow(() -> new NotFoundException("回复未找到"))
                                        .getComment()
                                        .getUserId();
                                default -> throw new IllegalArgumentException("不支持的目标类型: " + type);
                            }
                    )
                    .targetId(targetId)
                    .targetType(type == TargetType.COMMENT ? org.tongji.sse.type.TargetType.COMMENT : org.tongji.sse.type.TargetType.REPLY)
                    .type(NotificationType.LIKE)
                    .content("有人给你的" + (type == TargetType.COMMENT ? "评论" : "回复") + "点赞了")
                    .build();
            eventPublisher.register(event, EventChannelEnum.NOTIFICATION_LIKE);
        }

        Long count = commentReplyLikeRepository.countByTargetIdAndTargetType(targetId, type);
        return new LikeResponse(liked, count);
    }


    @Override
    public Boolean getCommentOrReplyLikeStatus(TargetType type, Long targetId, HttpServletRequest request) {
        Long userId = SecurityUtil.getUserIdOrNull(request);

        validateTargetCanBeOperated(targetId, type);

        return commentReplyLikeRepository.existsByTargetIdAndUserIdAndTargetType(targetId, userId, type);
        
    }

    @Override
    public Boolean getAudioLikeStatus(Long audioId, HttpServletRequest request) {
        Long userId = SecurityUtil.getUserIdOrNull(request);

        if(!audioRepository.existsByIdAndDeletedFalseAndStatus(audioId, "PUBLISHED")) {
            throw new NotFoundException("音频未找到或不可见");
        }

        return audioLikeRepository.existsByAudio_IdAndUser_Id(audioId, userId);
    }

    @Override
    public Long getAudioLikeCount(Long audioId) {
        if(!audioRepository.existsByIdAndDeletedFalseAndStatus(audioId, "PUBLISHED")) {
            throw new NotFoundException("音频未找到或不可见");
        }
        return audioLikeRepository.countByAudio_Id(audioId);
    }

    @Override
    public Long getCommentOrReplyLikeCount(TargetType type, Long targetId) {
        validateTargetCanBeOperated(targetId, type);
        return commentReplyLikeRepository.countByTargetIdAndTargetType(targetId, type);
    }

    private void validateTargetCanBeOperated(Long targetId, TargetType type) {
        // 检查被点赞对象是否可以被操作
        Long audioId;
        switch (type) {
            case COMMENT -> {
                AudioComment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new NotFoundException("评论未找到"));
                if (Boolean.TRUE.equals(comment.getIsDeleted())) {
                    throw new NotFoundException("评论已被删除");
                }
                audioId = comment.getAudio().getId();
            }
            case REPLY -> {
                AudioCommentReply reply = replyRepository.findById(targetId)
                        .orElseThrow(() -> new NotFoundException("回复未找到"));
                if (Boolean.TRUE.equals(reply.getIsDeleted())) {
                    throw new NotFoundException("回复已被删除");
                }
                audioId = reply.getComment().getAudio().getId();
            }
            default -> throw new IllegalArgumentException("不支持的目标类型: " + type);
        }
        // 校验音频状态
        if(!audioRepository.existsByIdAndDeletedFalseAndStatus(audioId, "PUBLISHED")) {
            throw new NotFoundException("音频未找到或不可见");
        }
    }
}

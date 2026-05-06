package org.tongji.sse.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tongji.sse.dto.AudioCommentResponse;
import org.tongji.sse.dto.AudioDetailsResponse;
import org.tongji.sse.dto.CommentWithRepliesResponse;
import org.tongji.sse.dto.ReplyResponse;
import org.tongji.sse.entity.Audio;
import org.tongji.sse.entity.AudioComment;
import org.tongji.sse.entity.AudioCommentReply;
import org.tongji.sse.entity.User;
import org.tongji.sse.enums.TargetType;
import org.tongji.sse.eventUtil.EventBuilder;
import org.tongji.sse.eventUtil.EventPublisher;
import org.tongji.sse.eventUtil.enums.EventChannelEnum;
import org.tongji.sse.eventUtil.event.NotificationEvent;
import org.tongji.sse.eventUtil.event.UserBehaviorSignalEvent;
import org.tongji.sse.exception.AccessDeniedException;
import org.tongji.sse.exception.BadRequestException;
import org.tongji.sse.exception.NotFoundException;
import org.tongji.sse.repository.*;
import org.tongji.sse.security.SecurityUtil;
import org.tongji.sse.service.CommentService;
import org.tongji.sse.type.BehaviorTargetType;
import org.tongji.sse.type.BehaviorType;
import org.tongji.sse.type.NotificationType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final AudioCommentRepository commentRepository;
    private final AudioCommentReplyRepository replyRepository;
    private final AudioRepository audioRepository;
    private final UserRepository userRepository;
    private final AudioLikeRepository audioLikeRepository;
    private final CommentReplyLikeRepository commentReplyLikeRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public AudioCommentResponse addComment(Long audioId, String content, HttpServletRequest request) {
        Long userId = SecurityUtil.getUserIdOrThrow(request);

        Audio audio = audioRepository.findById(audioId)
                .filter(a -> !a.isDeleted() && "PUBLISHED".equals(a.getStatus()))
                .orElseThrow(() -> new NotFoundException("音频未找到或无法评论"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户未找到"));

        AudioComment comment = AudioComment.builder()
                .audioId(audio.getId())
                .userId(user.getId())
                .content(content)
                .createTime(LocalDateTime.now())
                .user(user)
                .audio(audio)
                .build();

        commentRepository.save(comment);

        NotificationEvent event = EventBuilder.builder()
                .actorUserId(userId)
                .receiverUserId(audio.getUserId())
                .targetId(audioId)
                .targetType(org.tongji.sse.type.TargetType.AUDIO)
                .type(NotificationType.COMMENT)
                .content("有人给你的音频评论了：" + content)
                .build();
        eventPublisher.register(event, EventChannelEnum.NOTIFICATION_COMMENT);

        UserBehaviorSignalEvent behaviorSignalEvent = UserBehaviorSignalEvent.builder()
                .behaviorType(BehaviorType.COMMENT)
                .userId(userId)
                .behaviorTargetType(BehaviorTargetType.AUDIO)
                .targetId(audioId)
                .tags(audio.getTags() == null ? new ArrayList<>() : new ArrayList<>(audio.getTags()))
                .authorId(audio.getUserId())
                .textContent(content)
                .build();
        eventPublisher.register(behaviorSignalEvent, EventChannelEnum.BEHAVIOR_PUSH);

        return AudioCommentResponse.builder()
                .id(comment.getId())
                .audioId(comment.getAudioId())
                .userId(comment.getUserId())
                .username(comment.getUser().getUsername())
                .content(content)
                .createTime(comment.getCreateTime())
                .build();
    }

    @Override
    public List<AudioCommentResponse> getComments(Long audioId) {
        // 先检查音频是否可用
        audioRepository.findById(audioId)
                .filter(a -> !a.isDeleted() && "PUBLISHED".equals(a.getStatus()))
                .orElseThrow(() -> new NotFoundException("音频未找到或无法查看评论"));

        List<AudioComment> comments = commentRepository.findByAudioIdAndIsDeletedFalseOrderByCreateTimeDesc(audioId);

        return comments.stream()
                .map(c -> new AudioCommentResponse(
                        c.getId(),
                        c.getAudioId(),
                        c.getUserId(),
                        c.getUser().getUsername(),
                        c.getContent(),
                        c.getCreateTime()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long targetId, TargetType type, HttpServletRequest request) {
        Long userId = SecurityUtil.getUserIdOrNull(request);

        switch (type) {
            case COMMENT -> {
                AudioComment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new NotFoundException("评论未找到"));

                if (!comment.getUserId().equals(userId)) {
                    throw new AccessDeniedException("您没有权限删除此评论");
                }

                comment.setIsDeleted(true);
                commentRepository.save(comment);
            }
            case REPLY -> {
                AudioCommentReply reply = replyRepository.findById(targetId)
                        .orElseThrow(() -> new NotFoundException("回复未找到"));

                if (!reply.getUserId().equals(userId)) {
                    throw new AccessDeniedException("您没有权限删除此回复");
                }

                reply.setIsDeleted(true);
                replyRepository.save(reply);
            }
            default -> throw new BadRequestException("不支持的类型: " + type);
        }
    }

    @Override
    public AudioDetailsResponse getAudioDetails(Long audioId, HttpServletRequest request) {
        // ======== 音频检查 ========
        Audio audio = audioRepository.findById(audioId)
                .orElseThrow(() -> new NotFoundException("未找到该音频"));

        Long userId = SecurityUtil.getUserIdOrNull(request);

        boolean isOwner = (userId != null && userId.equals(audio.getUserId()));

        if (audio.isDeleted()) {
            throw new NotFoundException("音频已被删除");
        }

        if (isOwner && "UPLOADED".equals(audio.getStatus())) {
            throw new NotFoundException("音频未发布");
        }

        if (!isOwner && !"PUBLISHED".equals(audio.getStatus())) {
            throw new NotFoundException("音频不可用");
        }

        // ======== 统计音频点赞 ========
        Long audioLikes = audioLikeRepository.countByAudio_Id(audioId);
        boolean userLikedAudio = audioLikeRepository.existsByAudio_IdAndUser_Id(audioId, userId);

        // ======== 获取所有评论（可选：fetch join 避免 user Lazy 触发） ========
        List<AudioComment> comments =
                commentRepository.findByAudioIdAndIsDeletedFalseOrderByCreateTimeAsc(audioId);

        List<Long> commentIds = comments.stream().map(AudioComment::getId).toList();
        if (commentIds.isEmpty()) commentIds = List.of(-1L);


        // ======== 获取所有回复 ========
        List<AudioCommentReply> replies =
                replyRepository.findByCommentIdInAndIsDeletedFalse(commentIds);

        List<Long> replyIds = replies.stream().map(AudioCommentReply::getId).toList();
        if (replyIds.isEmpty()) replyIds = List.of(-1L);


        // ====================================================================================
        // 1) 批量查询评论 & 回复的点赞数（解决 N+1）
        // ====================================================================================
        Map<Long, Long> commentLikeCountMap =
                commentReplyLikeRepository.countLikesGroupByTargetIdsAndType(commentIds, TargetType.COMMENT);

        Map<Long, Long> replyLikeCountMap =
                commentReplyLikeRepository.countLikesGroupByTargetIdsAndType(replyIds, TargetType.REPLY);

        // ====================================================================================
        // 2) 批量查询用户是否点赞过（解决 N+1）
        // ====================================================================================
        Set<Long> userLikedCommentIds = Set.of();
        Set<Long> userLikedReplyIds = Set.of();

        if (userId != null) {
            userLikedCommentIds = commentReplyLikeRepository
                    .findLikedTargetIdsByUserAndType(userId, TargetType.COMMENT, commentIds);

            userLikedReplyIds = commentReplyLikeRepository
                    .findLikedTargetIdsByUserAndType(userId, TargetType.REPLY, replyIds);
        }


        // ======== 回复按 commentId 分组 ========
        Map<Long, List<AudioCommentReply>> replyMap =
                replies.stream().collect(Collectors.groupingBy(AudioCommentReply::getCommentId));


        // ======== 构建评论 + 回复 DTO ========
        List<CommentWithRepliesResponse> commentResponses = new ArrayList<>();

        for (AudioComment c : comments) {
            CommentWithRepliesResponse dto = new CommentWithRepliesResponse();
            dto.setCommentId(c.getId());
            dto.setUserId(c.getUserId());
            dto.setUsername(c.getUser().getUsername());
            dto.setContent(c.getContent());
            dto.setCreateTime(c.getCreateTime());

            // 批量点赞数
            dto.setLikes(commentLikeCountMap.getOrDefault(c.getId(), 0L));

            // 批量“是否点赞”
            dto.setUserLiked(userLikedCommentIds.contains(c.getId()));

            // 回复
            List<AudioCommentReply> replyList = replyMap.getOrDefault(c.getId(), List.of());
            List<ReplyResponse> replyDtos = new ArrayList<>();

            for (AudioCommentReply r : replyList) {
                ReplyResponse rr = new ReplyResponse();
                rr.setReplyId(r.getId());
                rr.setUserId(r.getUserId());
                rr.setUsername(r.getUser().getUsername());
                rr.setContent(r.getContent());
                rr.setCreateTime(r.getCreateTime());

                rr.setLikes(replyLikeCountMap.getOrDefault(r.getId(), 0L));
                rr.setUserLiked(userLikedReplyIds.contains(r.getId()));

                replyDtos.add(rr);
            }

            dto.setReplies(replyDtos);
            commentResponses.add(dto);
        }


        // ======== 最终结果 ========
        AudioDetailsResponse result = new AudioDetailsResponse();
        result.setAudioId(audioId);
        result.setLikes(audioLikes);
        result.setUserLiked(userLikedAudio);
        result.setComments(commentResponses);

        // 让音频访问量加一
        audio.setVisitCount(audio.getVisitCount() + 1);
        audioRepository.save(audio);

        return result;
    }

    @Override
    public AudioCommentResponse getComment(Long commentId, HttpServletRequest request) {
        Long uid = SecurityUtil.getUserIdOrThrow(request);
        AudioComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("评论内容未找到"));
        if(comment.getIsDeleted()){
            throw new NotFoundException("评论已被删除");
        }
        Audio audio = comment.getAudio();
        if(audio.isDeleted()){
            throw new NotFoundException("相关音频已被删除");
        }
        if(!audio.getUserId().equals(uid) && !"PUBLISHED".equals(audio.getStatus())){
            throw new NotFoundException("相关音频不可见");
        }
        return AudioCommentResponse.builder()
                .id(commentId)
                .audioId(comment.getAudioId())
                .userId(comment.getUserId())
                .username(comment.getUser().getUsername())
                .content(comment.getContent())
                .createTime(comment.getCreateTime())
                .build();
    }
}

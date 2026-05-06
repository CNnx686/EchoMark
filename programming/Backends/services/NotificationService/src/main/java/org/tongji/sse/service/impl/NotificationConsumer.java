package org.tongji.sse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tongji.sse.entity.Notification;
import org.tongji.sse.eventUtil.EventPublisher;
import org.tongji.sse.eventUtil.enums.EventChannelEnum;
import org.tongji.sse.eventUtil.event.NotificationCreatedEvent;
import org.tongji.sse.eventUtil.event.NotificationEvent;
import org.tongji.sse.repository.NotificationRepository;
import org.tongji.sse.type.NotificationType;

@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationRepository repository;
    private final EventPublisher eventPublisher;

    @RabbitListener(
            queues = {
                    "notification.like.queue",
                    "notification.comment.queue",
                    "notification.reply.queue"
            }
    )
    @Transactional
    public void handleNotificationEvent(NotificationEvent event) {
        // 基于业务唯一性判断幂等
        if(event.getType().equals(NotificationType.LIKE)) {
            var existing = repository.findByReceiverUserIdAndActorUserIdAndTypeAndTargetTypeAndTargetId(
                    event.getReceiverUserId(),
                    event.getActorUserId(),
                    event.getType(),
                    event.getTargetType(),
                    event.getTargetId()
            );

            if (existing.isPresent()) {
                return; // 幂等处理
            }
        }

        Notification notification = Notification.builder()
                .eventId(event.getEventId())
                .receiverUserId(event.getReceiverUserId())
                .actorUserId(event.getActorUserId())
                .type(event.getType())
                .content(event.getContent())
                .targetType(event.getTargetType())
                .targetId(event.getTargetId())
                .createdAt(event.getCreatedAt())
                .isRead(false)
                .build();
        repository.save(notification);

        // 注册产生新通知的事件
        NotificationCreatedEvent createdEvent = NotificationCreatedEvent.builder()
                .notificationId(notification.getId())
                .receiverUserId(notification.getReceiverUserId())
                .createdAt(notification.getCreatedAt())
                .build();
        eventPublisher.register(createdEvent, EventChannelEnum.SSE_PUSH);
    }
}

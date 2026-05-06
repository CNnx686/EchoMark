package org.tongji.sse.eventUtil;

import org.tongji.sse.eventUtil.event.NotificationEvent;
import org.tongji.sse.type.NotificationType;
import org.tongji.sse.type.TargetType;

import java.time.Instant;
import java.util.UUID;

public class EventBuilder {

    private Long receiverUserId;
    private Long actorUserId;
    private NotificationType type;
    private String content;
    private TargetType targetType;
    private Long targetId;

    public static EventBuilder builder() {
        return new EventBuilder();
    }

    public EventBuilder receiverUserId(Long receiverUserId) {
        this.receiverUserId = receiverUserId;
        return this;
    }

    public EventBuilder actorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
        return this;
    }

    public EventBuilder type(NotificationType type) {
        this.type = type;
        return this;
    }

    public EventBuilder content(String content) {
        this.content = content;
        return this;
    }

    public EventBuilder targetType(TargetType targetType) {
        this.targetType = targetType;
        return this;
    }

    public EventBuilder targetId(Long targetId) {
        this.targetId = targetId;
        return this;
    }

    public NotificationEvent build() {
        return NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .receiverUserId(receiverUserId)
                .actorUserId(actorUserId)
                .type(type)
                .content(content)
                .targetType(targetType)
                .targetId(targetId)
                .createdAt(Instant.now())
                .build();
    }
}


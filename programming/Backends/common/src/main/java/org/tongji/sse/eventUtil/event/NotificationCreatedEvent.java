package org.tongji.sse.eventUtil.event;

import lombok.*;
import org.tongji.sse.type.NotificationType;
import org.tongji.sse.type.TargetType;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCreatedEvent {
    private Long notificationId;
    private Long receiverUserId;
    private Instant createdAt;
}

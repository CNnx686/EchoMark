package org.tongji.sse.dto;

import lombok.*;
import org.tongji.sse.type.NotificationType;
import org.tongji.sse.type.TargetType;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private Long receiverUserId;
    private Long actorUserId;
    private NotificationType type;
    private String content;
    private TargetType targetType;
    private Long targetId;
    private Instant createdAt;
    private Boolean isRead;
}
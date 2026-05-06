package org.tongji.sse.eventUtil.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tongji.sse.type.NotificationType;
import org.tongji.sse.type.TargetType;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private String eventId;          // UUID，用于幂等
    private Long receiverUserId;     // 谁收到通知
    private Long actorUserId;        // 谁触发的
    private NotificationType type;             // LIKE / COMMENT / REPLY
    private String content;          // 文案
    private TargetType targetType;       // AUDIO / COMMENT
    private Long targetId;           // 音频ID / 评论ID
    private Instant createdAt;

    public String routingKey() {
        return "notification." + type.name().toLowerCase();
    }
}

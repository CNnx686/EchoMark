package org.tongji.sse.entity;

import jakarta.persistence.*;
import lombok.*;
import org.tongji.sse.type.NotificationType;
import org.tongji.sse.type.TargetType;

import java.time.Instant;

@Entity
@Table(name = "notification", indexes = {
        @Index(columnList = "eventId", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String eventId;      // 对应 NotificationEvent.eventId，用于幂等

    @Column(nullable = false)
    private Long receiverUserId;

    @Column(nullable = false)
    private Long actorUserId;

    @Column(nullable = false)
    private NotificationType type;         // LIKE / COMMENT / REPLY

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private TargetType targetType;   // AUDIO / COMMENT

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Boolean isRead;
}

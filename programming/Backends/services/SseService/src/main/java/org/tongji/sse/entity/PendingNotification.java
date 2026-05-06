package org.tongji.sse.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(
        name = "sse_pending_notification",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_notification",
                columnNames = {"user_id", "notification_id"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 接收通知的用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 对应的通知ID
     */
    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    /**
     * 是否已经推送过
     */
    @Column(name = "pushed", nullable = false)
    private Boolean pushed = false;

    /**
     * 创建时间（事件生成时间）
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * 实际推送时间
     */
    @Column(name = "pushed_at")
    private Instant pushedAt;
}

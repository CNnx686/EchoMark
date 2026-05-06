package org.tongji.sse.entity;

import jakarta.persistence.*;
import lombok.*;
import org.tongji.sse.enums.TargetType;


@Entity
@Table(name = "comment_reply_like",
        uniqueConstraints = @UniqueConstraint(columnNames = {"target_id", "user_id", "target_type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentReplyLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_id", nullable = false)
    private Long targetId; // 对应评论或回复的 ID

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private TargetType targetType; // COMMENT 或 REPLY

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    // 不用直接关联评论或回复，因为 targetId + targetType 决定
    // 可以通过查询 Service 层再加载对象
}


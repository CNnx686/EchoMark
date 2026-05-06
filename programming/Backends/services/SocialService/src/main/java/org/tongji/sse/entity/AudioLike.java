package org.tongji.sse.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "audio_like",
        uniqueConstraints = @UniqueConstraint(columnNames = {"audio_id", "user_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 外键指向 AudioService 中的 Audio 表
    @Column(name = "audio_id", nullable = false)
    private Long audioId;

    // 外键指向 AuthService 中的 User 表
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 导航属性（注意：不会跨微服务加载，不强制 fetch）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_audio_like_audio"))
    private Audio audio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_audio_like_user"))
    private User user;
}

package org.tongji.sse.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audio_comment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audio_id", nullable = false)
    private Long audioId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // 导航属性
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_audio_comment_audio"))
    private Audio audio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_audio_comment_user"))
    private User user;
}


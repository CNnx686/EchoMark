package org.tongji.sse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audio_comment_reply")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioCommentReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;  // 所回复的评论

    @Column(name = "user_id", nullable = false)
    private Long userId;     // 回复人

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // 导航属性
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_reply_comment"))
    private AudioComment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_reply_user"))
    private User user;
}

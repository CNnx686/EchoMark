package org.tongji.sse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Entity
@Table(name = "audios")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Audio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String audioUrl;

    private String photoUrl;

    private Double latitude;

    private Double longitude;

    private String title;

    @Column(length = 200)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "audio_tags", joinColumns = @JoinColumn(name = "audio_id"))
    @Column(name = "tag", length = 50)
    private Set<String> tags;

    private Instant uploadTime;

    private Instant publishTime;

    private String status;

    private boolean deleted = false;

    private Long visitCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_audio_user"))
    private User user;
}

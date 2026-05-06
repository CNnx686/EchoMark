package org.tongji.sse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "audios")
public class LlmAudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String status;

    private String title;

    @Column(length = 200)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "audio_tags", joinColumns = @JoinColumn(name = "audio_id"))
    @Column(name = "tag", length = 50)
    private Set<String> tags;
}

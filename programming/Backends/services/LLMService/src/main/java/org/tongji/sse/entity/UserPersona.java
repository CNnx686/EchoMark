package org.tongji.sse.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_persona")
public class UserPersona {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "persona_json", columnDefinition = "json")
    private String personaJson;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "version")
    private Integer version;
}

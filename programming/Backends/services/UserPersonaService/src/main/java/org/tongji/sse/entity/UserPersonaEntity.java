package org.tongji.sse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "user_persona")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPersonaEntity {

    @Id
    private Long userId;

    @Column(columnDefinition = "json", nullable = false)
    private String personaJson;

    private Integer version;

    private Instant updatedAt;

}

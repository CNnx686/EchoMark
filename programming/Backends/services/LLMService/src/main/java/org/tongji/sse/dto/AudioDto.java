package org.tongji.sse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioDto {
    private Long id;
    private Long userId;
    private String userName;
    private String audioUrl;
    private String photoUrl;
    private Double latitude;
    private Double longitude;
    private String title;
    private String description;
    private Set<String> tags;
    private Instant uploadTime;
    private Instant publishTime;
    private String status;
}

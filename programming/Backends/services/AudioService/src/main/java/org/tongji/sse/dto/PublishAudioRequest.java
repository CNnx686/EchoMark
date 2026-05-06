package org.tongji.sse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishAudioRequest {
    private String title;
    private String description;
    private Set<String> tags;
    private String photoUrl;
    private Boolean isPublic;
}

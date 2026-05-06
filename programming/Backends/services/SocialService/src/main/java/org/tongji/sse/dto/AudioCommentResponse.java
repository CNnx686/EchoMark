package org.tongji.sse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AudioCommentResponse {
    private Long id;
    private Long audioId;
    private Long userId;
    private String username;
    private String content;
    private LocalDateTime createTime;
}

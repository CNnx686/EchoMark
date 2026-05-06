package org.tongji.sse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioCommentReplyResponse {

    private Long id;               // 回复ID
    private Long audioId;
    private Long commentId;
    private Long userId;
    private String content;        // 回复内容
    private String username;       // 回复用户名称
    private LocalDateTime createTime; // 创建时间
}

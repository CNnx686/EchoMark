package org.tongji.sse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReplyResponse {
    private Long replyId;
    private Long userId;
    private String username;
    private String content;
    private LocalDateTime createTime;

    private Long likes;
    private boolean userLiked;
}

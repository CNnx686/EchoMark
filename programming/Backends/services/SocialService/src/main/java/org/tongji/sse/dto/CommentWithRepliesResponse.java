package org.tongji.sse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentWithRepliesResponse {
    private Long commentId;
    private Long userId;
    private String username;
    private String content;
    private LocalDateTime createTime;

    private Long likes;
    private boolean userLiked;

    private List<ReplyResponse> replies;
}

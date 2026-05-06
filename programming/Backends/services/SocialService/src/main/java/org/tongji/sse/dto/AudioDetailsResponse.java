package org.tongji.sse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AudioDetailsResponse {
    private Long audioId;
    private Long likes;
    private boolean userLiked;
    private List<CommentWithRepliesResponse> comments;
}

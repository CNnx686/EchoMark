package org.tongji.sse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeAudioResponse {
    private boolean liked;   // true 表示点赞成功，false 表示取消点赞
    private Long likeCount;  // 当前该音频的总点赞数
}
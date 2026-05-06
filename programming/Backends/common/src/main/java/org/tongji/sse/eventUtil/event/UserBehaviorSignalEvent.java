package org.tongji.sse.eventUtil.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tongji.sse.type.BehaviorTargetType;
import org.tongji.sse.type.BehaviorType;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBehaviorSignalEvent implements Serializable {
    /* ========= 事件基础信息 ========= */

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();             // UUID，幂等用
    private Instant occurredAt;           // 行为发生时间

    /* ========= 用户信息 ========= */

    private Long userId;                  // 行为发起用户

    /* ========= 行为描述 ========= */

    private BehaviorType behaviorType;    // 行为类型
    private BehaviorTargetType behaviorTargetType;         // 作用对象类型
    private Long targetId;                 // 目标ID（audioId / authorId）

    /* ========= 推理线索（最重要） ========= */

    private List<String> tags;             // 音频标签
    private String category;               // 音频分类
    private Long authorId;                 // 内容作者

    /* ========= 行为补充信息（可选） ========= */

    private String keyword;                // 搜索关键词
    private String textContent;            // 评论文本 / 发布描述

    /* ========= 扩展字段 ========= */

    private Map<String, Object> ext;       // 未来扩展（播放时长等）
}

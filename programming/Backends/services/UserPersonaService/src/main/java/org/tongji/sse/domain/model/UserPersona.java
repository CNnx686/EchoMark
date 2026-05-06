package org.tongji.sse.domain.model;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class UserPersona {

    private final Map<String, Double> tagWeights = new HashMap<>();
    private final Map<String, Double> categoryWeights = new HashMap<>();
    private final Map<Long, Double> authorWeights = new HashMap<>();
    // 新增：活跃时间段（0~23）
    private final Map<Integer, Double> preferredTimeSlots = new HashMap<>();

    // 新增：行为占比
    private final Map<String, Double> interactionRatio = new HashMap<>();

    // 新增：关键词偏好
    private final Map<String, Double> keywordWeights = new HashMap<>();
}

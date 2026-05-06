package org.tongji.sse.service.impl;

import org.springframework.stereotype.Service;
import org.tongji.sse.domain.model.UserPersona;
import org.tongji.sse.domain.strategy.*;
import org.tongji.sse.eventUtil.event.UserBehaviorSignalEvent;
import org.tongji.sse.service.UserPersonaUpdateService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class UserPersonaUpdateServiceImpl implements UserPersonaUpdateService {

    private final List<BehaviorUpdateStrategy> strategies = List.of(
            new SearchBehaviorStrategy(),
            new ViewBehaviorStrategy(),
            new LikeBehaviorStrategy(),
            new CommentBehaviorStrategy(),
            new PublishBehaviorStrategy()
    );

    @Override
    public void update(UserBehaviorSignalEvent event, UserPersona persona) {

        // ① 更新活跃时间（边际递减）
        int hour = LocalDateTime.now().getHour();
        increaseAdaptive(
                persona.getPreferredTimeSlots(),
                hour,
                1.0,
                10.0    // cap
        );

        // ② 更新行为占比（EMA 风格）
        updateInteractionRatio(persona, event.getBehaviorType().name());

        // ③ 行为特定策略
        strategies.stream()
                .filter(s -> s.supports(event.getBehaviorType().name()))
                .findFirst()
                .ifPresent(s -> s.apply(event, persona));
    }

    public static <K> void increaseAdaptive(
            Map<K, Double> map,
            K key,
            double base,
            double cap
    ) {
        double old = map.getOrDefault(key, 0.0);
        double factor = Math.max(0.2, 1.0 - old / cap);
        map.put(key, old + base * factor);
    }

    private void updateInteractionRatio(UserPersona persona, String behavior) {
        Map<String, Double> ratio = persona.getInteractionRatio();

        double alpha = 0.1; // 学习率（可调）

        // 先对所有行为做衰减
        ratio.replaceAll((k, v) -> v * (1 - alpha));

        // 当前行为加权
        ratio.put(behavior,
                ratio.getOrDefault(behavior, 0.0) + alpha
        );

        // 归一化（防漂移）
        double sum = ratio.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum > 0) {
            ratio.replaceAll((k, v) -> v / sum);
        }
    }


}

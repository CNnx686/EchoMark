package org.tongji.sse.service.impl;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.tongji.sse.eventUtil.event.UserBehaviorSignalEvent;
import org.tongji.sse.service.UserPersonaService;
import org.tongji.sse.type.BehaviorType;

import java.time.Duration;

@Component
public class UserBehaviorEventConsumer {

    private final UserPersonaService personaService;
    private final StringRedisTemplate redis;

    public UserBehaviorEventConsumer(UserPersonaService personaService, StringRedisTemplate redis) {
        this.personaService = personaService;
        this.redis = redis;
    }

    @RabbitListener(queues = "behavior.queue")
    public void onMessage(UserBehaviorSignalEvent event) {
        // 1. eventId 幂等
        String eventKey = "behavior:event:" + event.getEventId();
        if (redis.hasKey(eventKey)) {
            return;
        }

        // 2. LIKE 特判（兴趣信号防刷）
        if (event.getBehaviorType() == BehaviorType.LIKE) {
            String likeKey = "like:signal:" + event.getUserId() + ":" + event.getTargetId();
            if (redis.hasKey(likeKey)) {
                // 标记该 event 已处理，防止重复消费
                redis.opsForValue().set(
                        eventKey,
                        "1",
                        Duration.ofDays(1)
                );
                return;
            }
        }

        // 3. 更新用户画像（核心逻辑）
        personaService.handleEvent(event);

        // 4. 写 Redis（在更新成功之后）
        redis.opsForValue().set(
                eventKey,
                "1",
                Duration.ofDays(1)
        );

        if (event.getBehaviorType() == BehaviorType.LIKE) {
            String likeKey = "like:signal:" + event.getUserId() + ":" + event.getTargetId();
            redis.opsForValue().set(
                    likeKey,
                    "1",
                    Duration.ofDays(1)
            );
        }
    }
}

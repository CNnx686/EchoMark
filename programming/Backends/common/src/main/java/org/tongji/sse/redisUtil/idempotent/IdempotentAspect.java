package org.tongji.sse.redisUtil.idempotent;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.tongji.sse.exception.IdempotentException;

import java.time.Duration;

@Aspect
@Component
@ConditionalOnProperty(
        prefix = "redis",
        name = "enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
public class IdempotentAspect {

    private final StringRedisTemplate redisTemplate;
    private final IdempotentProperties properties;

    @Around("@annotation(idempotent)")
    public Object around(
            ProceedingJoinPoint joinPoint,
            Idempotent idempotent
    ) throws Throwable {

        String name = idempotent.name();

        IdempotentProperties.Instance config =
                properties.getInstances().get(name);

        if (config == null) {
            throw new IllegalStateException(
                    "Idempotent config not found: " + name
            );
        }

        String key = buildKey(name, config);

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(
                        key,
                        "1",
                        Duration.ofSeconds(config.getExpireSeconds())
                );

        if (Boolean.FALSE.equals(success)) {
            throw new IdempotentException();
        }

        return joinPoint.proceed();
    }

    private String buildKey(
            String name,
            IdempotentProperties.Instance config
    ) {

        StringBuilder key = new StringBuilder("idempotent:");
        key.append(name);

        if (config.isPerUser()) {
            key.append(":user:").append(getCurrentUserId());
        }

        return key.toString();
    }

    private String getCurrentUserId() {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return "anonymous";
        }
        return auth.getName();
    }
}


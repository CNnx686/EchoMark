package org.tongji.sse.redisUtil.rateLimit;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.tongji.sse.exception.RateLimitExceededException;

import java.lang.reflect.Method;
import java.time.Duration;

@Aspect
@Component
@ConditionalOnProperty(
        prefix = "redis",
        name = "enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
public class RedisRateLimitAspect {

    private final StringRedisTemplate redisTemplate;
    private final RedisRateLimiterProperties properties;

    @Around("@within(redisRateLimit) || @annotation(redisRateLimit)")
    public Object around(
            ProceedingJoinPoint joinPoint,
            RedisRateLimit redisRateLimit
    ) throws Throwable {

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

        // ① 方法级注解优先
        RedisRateLimit methodAnno = method.getAnnotation(RedisRateLimit.class);
        RedisRateLimit effectiveAnno =
                methodAnno != null ? methodAnno : redisRateLimit;

        if (effectiveAnno == null) {
            return joinPoint.proceed();
        }

        // ② 根据 name 读取配置
        String name = effectiveAnno.name();

        RedisRateLimiterProperties.Instance config =
                properties.getInstances().get(name);

        if (config == null) {
            throw new IllegalStateException(
                    "RedisRateLimiter config not found: " + name
            );
        }

        // ③ 构造 key
        String key = buildKey(joinPoint, name, config);

        // ④ 限流判断
        if (!tryAcquire(key, config)) {
            throw new RateLimitExceededException();
        }

        return joinPoint.proceed();
    }

    private String buildKey(
            ProceedingJoinPoint joinPoint,
            String name,
            RedisRateLimiterProperties.Instance config
    ) {

        StringBuilder keyBuilder = new StringBuilder("rate:");
        keyBuilder.append(name);

        if (config.isPerUser()) {
            String userId = getCurrentUserId();
            keyBuilder.append(":user:").append(userId);
        }

        return keyBuilder.toString();
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "anonymous";
        }
        return auth.getName(); // 或 userId
    }

    private boolean tryAcquire(
            String key,
            RedisRateLimiterProperties.Instance config
    ) {

        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) {
            return false;
        }

        if (count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(1));
        }

        return count <= config.getPermitsPerSecond();
    }
}


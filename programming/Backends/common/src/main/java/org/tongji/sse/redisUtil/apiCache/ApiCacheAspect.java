package org.tongji.sse.redisUtil.apiCache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;

@Aspect
@Component
@ConditionalOnProperty(
        prefix = "redis",
        name = "enabled",
        havingValue = "true"
)
@RequiredArgsConstructor
public class ApiCacheAspect {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ApiCacheProperties properties;

    @Around("@annotation(apiCache)")
    public Object around(
            ProceedingJoinPoint joinPoint,
            ApiCache apiCache
    ) throws Throwable {

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

        String name = apiCache.name();

        ApiCacheProperties.CacheInstance config =
                properties.getInstances().get(name);

        if (config == null) {
            throw new IllegalStateException("ApiCache config not found: " + name);
        }

        String cacheKey = buildCacheKey(joinPoint, name, apiCache, config);

        // ① 查缓存
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return deserialize(cached, method);
        }

        // ② 执行业务
        Object result = joinPoint.proceed();

        // ③ 写缓存（只缓存成功结果）
        int ttl = apiCache.ttl() > 0 ? apiCache.ttl() : config.getTtl();
        redisTemplate.opsForValue().set(
                cacheKey,
                objectMapper.writeValueAsString(result),
                Duration.ofSeconds(ttl)
        );

        return result;
    }

    private String buildCacheKey(
            ProceedingJoinPoint joinPoint,
            String name,
            ApiCache apiCache,
            ApiCacheProperties.CacheInstance config
    ) {

        StringBuilder key = new StringBuilder("cache:");
        key.append(name);

        // 方法参数
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            key.append(":args:").append(Arrays.deepHashCode(args));
        }

        // 用户维度
        boolean perUser = apiCache.perUser() || config.isPerUser();
        if (perUser) {
            key.append(":user:").append(getCurrentUserId());
        }

        return key.toString();
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "anonymous";
        }
        return auth.getName(); // 或 userId
    }

    private Object deserialize(String cached, Method method) throws IOException {

        JavaType returnType = objectMapper.getTypeFactory()
                .constructType(method.getGenericReturnType());

        return objectMapper.readValue(cached, returnType);
    }

}


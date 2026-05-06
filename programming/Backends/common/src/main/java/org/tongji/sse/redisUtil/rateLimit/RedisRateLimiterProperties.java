package org.tongji.sse.redisUtil.rateLimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "redis.ratelimiter")
public class RedisRateLimiterProperties {

    private Map<String, Instance> instances = new HashMap<>();

    @Data
    public static class Instance {
        private int permitsPerSecond;
        private int burstCapacity;
        private boolean perUser;
    }
}

package org.tongji.sse.redisUtil.apiCache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "redis.cache")
public class ApiCacheProperties {

    private Map<String, CacheInstance> instances = new HashMap<>();

    @Data
    public static class CacheInstance {
        private int ttl;
        private boolean perUser;
    }
}

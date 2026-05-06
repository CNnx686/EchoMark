package org.tongji.sse.redisUtil.rateLimit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(RedisRateLimiterProperties.class)
@Configuration
public class RedisRateLimiterConfig {
}

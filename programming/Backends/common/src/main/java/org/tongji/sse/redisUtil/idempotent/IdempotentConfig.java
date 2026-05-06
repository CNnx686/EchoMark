package org.tongji.sse.redisUtil.idempotent;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(IdempotentProperties.class)
public class IdempotentConfig {
}


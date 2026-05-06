package org.tongji.sse.redisUtil.idempotent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "redis.idempotent")
public class IdempotentProperties {

    private Map<String, Instance> instances = new HashMap<>();

    @Data
    public static class Instance {

        /**
         * 幂等过期时间（秒）
         */
        private int expireSeconds = 10;

        /**
         * 是否按用户区分
         */
        private boolean perUser = true;
    }
}


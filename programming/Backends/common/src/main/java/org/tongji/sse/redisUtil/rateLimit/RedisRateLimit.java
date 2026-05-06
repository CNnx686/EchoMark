package org.tongji.sse.redisUtil.rateLimit;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisRateLimit {

    /**
     * 对应配置文件中的限流策略名
     */
    String name();
}


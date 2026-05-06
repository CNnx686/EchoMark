package org.tongji.sse.redisUtil.apiCache;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiCache {

    /**
     * 缓存策略名（配置驱动）
     */
    String name();

    /**
     * TTL（秒），默认从配置读取
     */
    int ttl() default -1;

    /**
     * 是否按用户缓存
     */
    boolean perUser() default false;
}

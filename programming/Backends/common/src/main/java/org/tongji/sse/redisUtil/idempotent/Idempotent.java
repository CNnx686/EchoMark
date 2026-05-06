package org.tongji.sse.redisUtil.idempotent;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等策略名（对应配置文件）
     */
    String name();
}

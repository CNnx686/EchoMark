package org.tongji.sse.eventUtil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProducesEvent {
    // 可选：指定事件类型
    Class<?>[] value() default {};
}

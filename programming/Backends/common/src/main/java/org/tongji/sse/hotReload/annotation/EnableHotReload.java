package org.tongji.sse.hotReload.annotation;

import org.springframework.context.annotation.Import;
import org.tongji.sse.hotReload.bootstrap.HotReloadBootstrap;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(HotReloadBootstrap.class)
public @interface EnableHotReload {
}

package org.tongji.sse.hotReload.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.tongji.sse.hotReload.core.HotReloadManager;

@Configuration
@Slf4j
public class HotReloadBootstrap implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        log.info("HotReloadBootstrap init");
        HotReloadManager manager = applicationContext.getBean(HotReloadManager.class);
        manager.init();
    }
}

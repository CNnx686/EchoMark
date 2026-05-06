package org.tongji.sse.hotReload.live;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.*;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class LiveReloader {

    private final ConfigurableApplicationContext context;

    public LiveReloader(ConfigurableApplicationContext context) {
        this.context = context;
    }

    public void apply(Path newConfig) {
        ConfigurableEnvironment env = context.getEnvironment();
        PropertySource<?> ps;
        try {
            ps = new ResourcePropertySource("hotReload", "file:" + newConfig.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        MutablePropertySources sources = env.getPropertySources();
        sources.addFirst(ps);
    }
}

package org.tongji.sse.hotReload.monitor;

import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.tongji.sse.hotReload.core.HotReloadManager;

@Component
public class RuntimeFailureListener {

    private final HotReloadManager manager;

    public RuntimeFailureListener(HotReloadManager manager) {
        this.manager = manager;
    }

    @EventListener
    public void onFailure(ApplicationFailedEvent event) {
        manager.onRuntimeFailure(event.getException());
    }
}

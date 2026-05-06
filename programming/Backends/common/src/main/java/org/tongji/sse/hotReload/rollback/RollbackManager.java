package org.tongji.sse.hotReload.rollback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tongji.sse.hotReload.core.StableConfigSnapshot;
import org.tongji.sse.hotReload.live.LiveReloader;

@Component
public class RollbackManager {

    private static final Logger log = LoggerFactory.getLogger(RollbackManager.class);

    private final LiveReloader liveReloader;

    public RollbackManager(LiveReloader liveReloader) {
        this.liveReloader = liveReloader;
    }

    public boolean rollback(StableConfigSnapshot snapshot) {
        if (snapshot == null) {
            log.error("No stable config to rollback to");
            return false;
        }
        try {
            liveReloader.apply(snapshot.getConfigPath());
            log.info("Rollback successful");
            return true;
        } catch (Exception e) {
            log.error("Rollback failed", e);
            return false;
        }
    }
}

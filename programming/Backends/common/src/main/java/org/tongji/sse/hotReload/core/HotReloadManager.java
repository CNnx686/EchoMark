package org.tongji.sse.hotReload.core;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tongji.sse.hotReload.live.LiveReloader;
import org.tongji.sse.hotReload.rollback.RollbackManager;
import org.tongji.sse.hotReload.shadow.ShadowLauncher;
import org.tongji.sse.hotReload.watcher.ConfigFileWatcher;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

@Component
public class HotReloadManager {

    private static final Logger log = LoggerFactory.getLogger(HotReloadManager.class);

    private volatile HotReloadState state = HotReloadState.IDLE;

    private StableConfigSnapshot lastStableConfig;

    private Instant cooldownUntil;
    private int consecutiveFailures = 0;

    private final ShadowLauncher shadowLauncher;
    private final LiveReloader liveReloader;
    private final RollbackManager rollbackManager;
    private final ConfigFileWatcher watcher;

    public HotReloadManager(
            ShadowLauncher shadowLauncher,
            LiveReloader liveReloader,
            RollbackManager rollbackManager,
            ConfigFileWatcher watcher
    ) {
        this.shadowLauncher = shadowLauncher;
        this.liveReloader = liveReloader;
        this.rollbackManager = rollbackManager;
        this.watcher = watcher;
    }

    @PostConstruct
    public void init() {
        watcher.startWatching(this::onConfigChanged);
    }

    public synchronized void onConfigChanged(Path newConfigPath, String hash) {
        if (state == HotReloadState.FROZEN) {
            log.warn("Hot reload frozen. Ignoring config change.");
            return;
        }

        if (cooldownUntil != null && Instant.now().isBefore(cooldownUntil)) {
            log.warn("Hot reload in cooldown until {}", cooldownUntil);
            return;
        }

        log.info("Config change detected: {}", newConfigPath);
        state = HotReloadState.SHADOW_STARTING;

        boolean shadowOk = shadowLauncher.tryStart(newConfigPath);
        if (!shadowOk) {
            onFailure("Shadow instance failed");
            return;
        }

        state = HotReloadState.APPLYING_LIVE_CONFIG;
        try {
            liveReloader.apply(newConfigPath);
        } catch (Exception e) {
            onFailure("Live reload failed: " + e.getMessage());
            return;
        }

        state = HotReloadState.LIVE_RUNNING;

        // 延迟确认稳定（简单实现：立即确认）
        lastStableConfig = new StableConfigSnapshot(newConfigPath, hash);
        consecutiveFailures = 0;
        state = HotReloadState.IDLE;

        log.info("Config applied successfully and marked stable");
    }

    public synchronized void onRuntimeFailure(Throwable t) {
        log.error("Runtime failure detected after reload", t);
        state = HotReloadState.ROLLING_BACK;

        boolean rollbackOk = rollbackManager.rollback(lastStableConfig);
        if (!rollbackOk) {
            freezeForever();
        } else {
            state = HotReloadState.IDLE;
        }
    }

    private void onFailure(String reason) {
        log.error(reason);
        consecutiveFailures++;

        if (consecutiveFailures >= 3) {
            cooldownUntil = Instant.now().plus(Duration.ofMinutes(30));
            log.warn("Hot reload entering cooldown");
        }

        state = HotReloadState.IDLE;
    }

    private void freezeForever() {
        state = HotReloadState.FROZEN;
        log.error("Hot reload permanently frozen. Manual intervention required.");
    }
}
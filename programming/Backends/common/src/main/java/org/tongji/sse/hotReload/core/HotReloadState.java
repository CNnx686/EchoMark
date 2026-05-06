package org.tongji.sse.hotReload.core;

public enum HotReloadState {
    IDLE,
    SHADOW_STARTING,
    APPLYING_LIVE_CONFIG,
    LIVE_RUNNING,
    ROLLING_BACK,
    FROZEN
}

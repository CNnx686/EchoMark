package org.tongji.sse.hotReload.core;

import lombok.Getter;

import java.nio.file.Path;
import java.time.Instant;

@Getter
public class StableConfigSnapshot {

    private final Path configPath;
    private final String hash;
    private final Instant verifiedAt;

    public StableConfigSnapshot(Path configPath, String hash) {
        this.configPath = configPath;
        this.hash = hash;
        this.verifiedAt = Instant.now();
    }

}

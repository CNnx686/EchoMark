package org.tongji.sse.hotReload.watcher;

import org.springframework.stereotype.Component;

import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

@Component
public class ConfigFileWatcher {

    private static final Path CONFIG_PATH = Paths.get("application.properties");
    private String lastHash;

    public void startWatching(BiConsumer<Path, String> callback) {
        Executors.newSingleThreadExecutor().submit(() -> {
            while (true) {
                try {
                    String hash = hash(CONFIG_PATH);
                    if (lastHash != null && !lastHash.equals(hash)) {
                        callback.accept(CONFIG_PATH, hash);
                    }
                    lastHash = hash;
                    Thread.sleep(30_000L);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private String hash(Path path) throws Exception {
        byte[] data = Files.readAllBytes(path);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(md.digest(data));
    }
}

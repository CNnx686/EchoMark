package org.tongji.sse.hotReload.shadow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Component
public class ShadowLauncher {

    private static final Logger log = LoggerFactory.getLogger(ShadowLauncher.class);

    public boolean tryStart(Path configPath) {
        try {
            Process process = new ProcessBuilder(
                    "java",
                    "-jar",
                    "app.jar",
                    "--spring.config.location=" + configPath.toAbsolutePath(),
                    "--server.port=0",
                    "--spring.main.web-application-type=none"
            ).start();

            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }

            if (process.exitValue() != 0) {
                return false;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                return reader.lines()
                        .anyMatch(line -> line.contains("Started"));
            }
        } catch (Exception e) {
            log.error("Shadow instance error", e);
            return false;
        }
    }
}

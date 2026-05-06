package org.tongji.sse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.tongji.sse.hotReload.annotation.EnableHotReload;

@SpringBootApplication
@EnableHotReload
public class AudioServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AudioServiceApplication.class, args);
    }
}
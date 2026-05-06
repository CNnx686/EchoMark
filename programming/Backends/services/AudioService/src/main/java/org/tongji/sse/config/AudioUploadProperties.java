package org.tongji.sse.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Getter
public class AudioUploadProperties {

    @Value("${audio.max-duration-seconds:60}")
    private int maxDurationSeconds;

    @Value("${audio.allowed-mime-types:audio/*}")
    private List<String> allowedMimeTypes;
}

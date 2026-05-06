package org.tongji.sse.eventUtil;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.tongji.sse.eventUtil.properties.EventChannelsProperties;

@Configuration
@EnableConfigurationProperties(EventChannelsProperties.class)
public class EventConfig {
}

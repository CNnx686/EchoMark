package org.tongji.sse.eventUtil.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "event")
public class EventChannelsProperties {

    private boolean enabled = true;

    private List<EventChannel> channels = new ArrayList<>();

    @Data
    public static class EventChannel {
        private String name;       // 枚举对应 EventChannelEnum
        private String exchange;
        private String queue;
        private String routingKey;
    }
}

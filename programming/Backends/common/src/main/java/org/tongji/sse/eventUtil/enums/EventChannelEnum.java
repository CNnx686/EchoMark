package org.tongji.sse.eventUtil.enums;

import lombok.Getter;

@Getter
public enum EventChannelEnum {
    NOTIFICATION_LIKE("notification.like"),
    NOTIFICATION_COMMENT("notification.comment"),
    NOTIFICATION_REPLY("notification.reply"),
    SSE_PUSH("sse.push"),
    BEHAVIOR_PUSH("behavior.push");

    private final String keyPrefix;

    EventChannelEnum(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

}

package org.tongji.sse.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.tongji.sse.eventUtil.event.NotificationCreatedEvent;

public interface SseService {

    SseEmitter subscribe(HttpServletRequest request);
    void pushToUser(Long userId, NotificationCreatedEvent event);
}

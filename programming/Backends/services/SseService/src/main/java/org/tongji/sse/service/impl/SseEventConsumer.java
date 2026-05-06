package org.tongji.sse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.tongji.sse.eventUtil.event.NotificationCreatedEvent;
import org.tongji.sse.service.SseService;

@Service
@RequiredArgsConstructor
public class SseEventConsumer {
    private final SseService sseService;

    @RabbitListener(queues = "sse.queue")
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        sseService.pushToUser(event.getReceiverUserId(), event);
    }
}

package org.tongji.sse.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.tongji.sse.entity.PendingNotification;
import org.tongji.sse.eventUtil.event.NotificationCreatedEvent;
import org.tongji.sse.repository.PendingNotificationRepository;
import org.tongji.sse.security.SecurityUtil;
import org.tongji.sse.service.SseService;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SseServiceImpl implements SseService {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final PendingNotificationRepository pendingRepository;

    @Override
    public SseEmitter subscribe(HttpServletRequest request) {
        Long userId = SecurityUtil.getUserIdOrThrow(request);

        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(userId, emitter);

        try {
            // ======= 首次连接 hello =======
            emitter.send(SseEmitter.event()
                    .name("init")
                    .data("hello"));
        } catch (IOException e) {
            // 如果首次推送就失败，直接结束连接
            emitter.completeWithError(e);
            emitters.remove(userId);
            return emitter;
        }

        // 查询该用户所有未推送通知
        List<PendingNotification> pendingList =
                pendingRepository.findByUserIdAndPushedOrderByCreatedAtAsc(userId, false);

        // 如果有待推送通知
        if (!pendingList.isEmpty()) {
            List<Long> notificationIds = new ArrayList<>();
            for (PendingNotification p : pendingList) {
                try {
                    NotificationCreatedEvent event = NotificationCreatedEvent.builder()
                            .notificationId(p.getNotificationId())
                            .receiverUserId(userId)
                            .createdAt(p.getCreatedAt() != null ? p.getCreatedAt() : Instant.now())
                            .build();
                    emitter.send(SseEmitter.event()
                            .name("notification-created")
                            .data(event));
                    notificationIds.add(p.getNotificationId());
                } catch (IOException e) {
                    // 推送失败，中断循环，剩余的下次继续
                    break;
                }
            }

            // 批量更新数据库，标记已推送
            if (!notificationIds.isEmpty()) {
                pendingRepository.markPushedBatch(userId, notificationIds);
            }
        }

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        return emitter;
    }

    @Override
    public void pushToUser(Long userId, NotificationCreatedEvent event) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            // 用户不在线 → 保存到待推送表
            if (!pendingRepository.existsByUserIdAndNotificationId(userId, event.getNotificationId())) {
                PendingNotification pending = PendingNotification.builder()
                        .userId(userId)
                        .notificationId(event.getNotificationId())
                        .pushed(false)
                        .createdAt(event.getCreatedAt() != null ? event.getCreatedAt() : Instant.now())
                        .build();
                pendingRepository.save(pending);
            }
            return;
        }

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("notification-created")
                            .data(event)
            );
        } catch (IOException e) {
            // 推送失败 → 移除 emitter，并存入待推送表
            emitters.remove(userId);
            if (!pendingRepository.existsByUserIdAndNotificationId(userId, event.getNotificationId())) {
                PendingNotification pending = PendingNotification.builder()
                        .userId(userId)
                        .notificationId(event.getNotificationId())
                        .pushed(false)
                        .createdAt(event.getCreatedAt() != null ? event.getCreatedAt() : Instant.now())
                        .build();
                pendingRepository.save(pending);
            }
        }
    }
}

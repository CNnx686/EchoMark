package org.tongji.sse.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tongji.sse.dto.NotificationDTO;
import org.tongji.sse.eventUtil.event.NotificationCreatedEvent;
import org.tongji.sse.mapper.NotificationMapper;
import org.tongji.sse.repository.NotificationRepository;
import org.tongji.sse.security.SecurityUtil;
import org.tongji.sse.service.NotificationService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;

    @Override
    public List<NotificationDTO> getNotificationsByIds(List<Long> ids, HttpServletRequest request) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        Long userId = SecurityUtil.getUserIdOrThrow(request);

        return notificationRepository
                .findByIdInAndReceiverUserId(ids, userId)
                .stream()
                .map(NotificationMapper::toDTO)
                .toList();
    }

    @Override
    public List<NotificationDTO> getUserNotifications(HttpServletRequest request) {
        Long userId = SecurityUtil.getUserIdOrThrow(request);
        return notificationRepository
                .findByReceiverUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public Boolean readNotificationsByIds(List<Long> notificationIds, HttpServletRequest request) {
        Long userId = SecurityUtil.getUserIdOrThrow(request);
        // 批量更新：只更新属于当前用户的通知
        int updatedCount = notificationRepository.markAsReadByIdsAndReceiverUserId(notificationIds, userId);

        // 如果更新条数 > 0 就返回 true
        return updatedCount > 0;
    }
}

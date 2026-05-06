package org.tongji.sse.service;

import jakarta.servlet.http.HttpServletRequest;
import org.tongji.sse.dto.NotificationDTO;
import org.tongji.sse.eventUtil.event.NotificationCreatedEvent;

import java.util.List;

public interface NotificationService {
    /**
     * 批量根据通知 ID 获取通知内容
     */
    List<NotificationDTO> getNotificationsByIds(List<Long> notificationIds, HttpServletRequest request);

    /**
     * 获取某个用户的所有通知
     */
    List<NotificationDTO> getUserNotifications(HttpServletRequest request);

    Boolean readNotificationsByIds(List<Long> notificationIds, HttpServletRequest request);
}

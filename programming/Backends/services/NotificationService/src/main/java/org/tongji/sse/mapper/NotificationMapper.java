package org.tongji.sse.mapper;

import org.tongji.sse.dto.NotificationDTO;
import org.tongji.sse.entity.Notification;

public class NotificationMapper {

    private NotificationMapper() {
        // 工具类，禁止实例化
    }

    public static NotificationDTO toDTO(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationDTO.builder()
                .id(notification.getId())
                .receiverUserId(notification.getReceiverUserId())
                .actorUserId(notification.getActorUserId())
                .type(notification.getType())
                .content(notification.getContent())
                .targetType(notification.getTargetType())
                .targetId(notification.getTargetId())
                .createdAt(notification.getCreatedAt())
                .isRead(notification.getIsRead())
                .build();
    }
}

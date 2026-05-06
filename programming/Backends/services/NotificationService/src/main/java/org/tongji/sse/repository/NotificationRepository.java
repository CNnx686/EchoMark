package org.tongji.sse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tongji.sse.entity.Notification;
import org.tongji.sse.type.NotificationType;
import org.tongji.sse.type.TargetType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByEventId(String eventId);
    Optional<Notification> findByReceiverUserIdAndActorUserIdAndTypeAndTargetTypeAndTargetId(
            Long receiverUserId, Long actorUserId, NotificationType type, TargetType targetType, Long targetId
    );

    List<Notification> findByReceiverUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByIdInAndReceiverUserId(List<Long> ids, Long userId);

    @Modifying
    @Query("UPDATE Notification n " +
            "SET n.isRead = TRUE " +
            "WHERE n.id IN :ids AND n.receiverUserId = :userId")
    int markAsReadByIdsAndReceiverUserId(@Param("ids") List<Long> ids,
                                         @Param("userId") Long userId);
}

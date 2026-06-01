package org.tongji.sse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.tongji.sse.entity.PendingNotification;

import java.util.List;

@Repository
public interface PendingNotificationRepository extends JpaRepository<PendingNotification, Long> {

    /**
     * 查询某用户所有未推送的通知，按创建时间升序
     */
    List<PendingNotification> findByUserIdAndPushedOrderByCreatedAtAsc(Long userId, Boolean pushed);

    // 批量标记已推送
    @Modifying
    @Transactional
    @Query(value = "UPDATE sse_pending_notification SET pushed = true, pushed_at = NOW() " +
            "WHERE user_id = :userId AND notification_id IN :notificationIds", nativeQuery = true)
    int markPushedBatch(Long userId, List<Long> notificationIds);

    /**
     * 判断某条通知是否已经存在
     */
    boolean existsByUserIdAndNotificationId(Long userId, Long notificationId);

    /**
     * 删除已推送的通知（可选，用于清理）
     */
    void deleteByUserIdAndPushedTrue(Long userId);
}

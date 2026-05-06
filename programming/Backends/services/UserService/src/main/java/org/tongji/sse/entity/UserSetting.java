package org.tongji.sse.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 用户设置实体类
 * 对应数据库中的 user_settings 表
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_settings")
public class UserSetting {

    /**
     * 用户ID，作为主键
     */
    @Id
    @Column(name = "user_id")
    private Long userId;

    /**
     * 通知静默设置
     * true: 静默通知
     * false: 接收通知 (默认)
     */
    @Column(name = "notification_mute", nullable = false)
    @Builder.Default
    private Boolean notificationMute = false;
}

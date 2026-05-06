package org.tongji.sse.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 用户个人资料实体类
 * 对应数据库中的 user_profiles 表
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_profiles")
public class UserProfile {

    /**
     * 用户ID，作为主键
     * 与 AuthenticationAccount ID 相同，用于关联认证账户
     */
    @Id
    private Long userId; // Same as AuthenticationAccount ID

    /**
     * 用户昵称
     * 不能为空
     */
    @Column(nullable = false)
    private String nickname;

    /**
     * 用户头像URL地址
     */
    private String avatarUrl;

    /**
     * 用户简介（短）
     */
    private String bio;

    /**
     * 用户自我描述（长）
     * 最大长度为2000字符
     */
    @Column(length = 2000)
    private String selfDescription;
}

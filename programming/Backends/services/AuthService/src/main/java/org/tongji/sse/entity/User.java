package org.tongji.sse.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * User 类表示用户实体，包含用户的基本信息。
 */
@Data
@Entity
@Builder
@NoArgsConstructor        // 无参构造函数（JPA 必须）
@AllArgsConstructor       // 全参构造函数（@Builder 需要）
@Table(name = "users")   // 数据库表名为 "users"
public class User {

    /**
     * 用户 ID，主键，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名，唯一且不能为空
     */
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * 用户密码，不能为空
     */
    @Column(nullable = false)
    private String password;

    /**
     * 用户邮箱，唯一且不能为空
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * 用户手机号，唯一，可为空
     */
    @Column(unique = true)
    private String phoneNumber;

    /**
     * 注册时间（DATETIME），可为空
     */
    @Column
    private java.time.LocalDateTime registrationTime;
}


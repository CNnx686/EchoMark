package org.tongji.sse.dto;

/**
 * UserResponseDTO 类用于封装用户信息的响应数据。
 */
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserResponseDTO 类用于封装用户信息的响应数据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {
    /**
     * 用户名
     */
    private String username;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 用户手机号
     */
    private String phoneNumber;

    /**
     * 用户注册时间
     */
    private java.time.LocalDateTime registrationTime;
}

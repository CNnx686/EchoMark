package org.tongji.sse.dto;

import lombok.Data;
import lombok.ToString;

/**
 * PasswordResetRequestDTO 类用于封装用户重置密码请求的数据。
 */
@Data
public class PasswordResetRequestDTO {
    /**
     * 用户标识符，可以是邮箱或手机号
     */
    private String identifier;

    /**
     * 验证码
     */
    private String code;

    /**
     * 新密码
     * 使用 @ToString.Exclude 防止密码被打印到日志中
     */
    @ToString.Exclude
    private String newPassword;
}

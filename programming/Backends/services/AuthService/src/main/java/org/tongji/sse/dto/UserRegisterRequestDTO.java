package org.tongji.sse.dto;

import lombok.Data;
import lombok.ToString;

/**
 * UserRegisterRequestDTO 类用于封装用户注册请求的数据。
 */
@Data
public class UserRegisterRequestDTO {
    /**
     * 用户名
     */
    private String username;

    /**
     * 用户密码
     * 使用 @ToString.Exclude 防止密码被打印到日志中
     */
    @ToString.Exclude
    private String password;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 用户手机号
     */
    private String phoneNumber;

    /**
     * 验证码
     */
    private String code;
}

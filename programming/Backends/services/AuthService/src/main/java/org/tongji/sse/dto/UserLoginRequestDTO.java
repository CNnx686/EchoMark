package org.tongji.sse.dto;

import lombok.Data;
import lombok.ToString;

/**
 * UserLoginRequestDTO 类用于封装用户登录请求的数据。
 */
@Data
public class UserLoginRequestDTO {
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
}

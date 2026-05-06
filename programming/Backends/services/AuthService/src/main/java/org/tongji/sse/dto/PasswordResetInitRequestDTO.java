package org.tongji.sse.dto;

import lombok.Data;

/**
 * PasswordResetInitRequestDTO 类用于封装请求重置密码（发送验证码）的数据。
 */
@Data
public class PasswordResetInitRequestDTO {
    /**
     * 用户标识符，可以是邮箱或手机号
     */
    private String identifier;
}

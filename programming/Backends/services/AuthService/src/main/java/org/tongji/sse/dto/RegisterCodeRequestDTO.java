package org.tongji.sse.dto;

import lombok.Data;

/**
 * RegisterCodeRequestDTO 类用于封装请求注册验证码的数据。
 */
@Data
public class RegisterCodeRequestDTO {
    /**
     * 用户邮箱
     */
    private String email;
}

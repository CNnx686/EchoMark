package org.tongji.sse.dto;

import lombok.Data;
import lombok.ToString;

/**
 * PasswordChangeRequestDTO 类用于封装用户修改密码请求的数据。
 */
@Data
public class PasswordChangeRequestDTO {
    /**
     * 用户名
     */
    private String username;

    /**
     * 旧密码
     * 使用 @ToString.Exclude 防止密码被打印到日志中
     */
    @ToString.Exclude
    private String oldPassword;

    /**
     * 新密码
     * 使用 @ToString.Exclude 防止密码被打印到日志中
     */
    @ToString.Exclude
    private String newPassword;
}

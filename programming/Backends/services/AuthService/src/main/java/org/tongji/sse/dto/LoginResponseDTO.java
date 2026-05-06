package org.tongji.sse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

/**
 * LoginResponseDTO 类用于封装用户登录响应的数据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {
    /**
     * 用户认证的 Token（JWT 或 Session Token）
     */
    private String token;

    /**
     * 用户名
     */
    private String username;
}

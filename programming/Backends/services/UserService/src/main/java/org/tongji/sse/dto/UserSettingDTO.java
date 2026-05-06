package org.tongji.sse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户设置 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingDTO {
    private Long userId;
    private Boolean notificationMute;
}

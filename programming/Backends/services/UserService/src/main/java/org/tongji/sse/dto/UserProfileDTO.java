package org.tongji.sse.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 用户个人资料数据传输对象 (DTO)
 * 用于在服务层和控制层之间传输用户资料信息
 */
@Data
@Builder
public class UserProfileDTO {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像URL地址
     */
    private String avatarUrl;

    /**
     * 用户简介（短）
     */
    private String bio;

    /**
     * 用户自我描述（长）
     */
    private String selfDescription;
}


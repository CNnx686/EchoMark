package org.tongji.sse.dto;

import lombok.Data;

/**
 * 用户个人资料更新请求数据传输对象 (DTO)
 * 用于接收客户端发送的更新用户资料的请求数据
 */
@Data
public class UserProfileUpdateRequestDTO {
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


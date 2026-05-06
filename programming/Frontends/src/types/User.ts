export interface UserResponseDTO {
  /**
   * 用户名
   */
  username: string;

  /**
   * 用户ID
   */
  userId: number; // 注意：Java 的 Long 在 TS 中通常用 number 表示（JS 最大安全整数为 2^53-1，若 ID 超出需用 string）

  /**
   * 用户邮箱
   */
  email: string;

  /**
   * 用户手机号
   */
  phoneNumber: string;

  /**
   * 用户注册时间（ISO 8601 格式字符串，如 "2025-12-29T17:16:00"）
   */
  registrationTime: string; // 通常后端返回的是 ISO 字符串，前端可使用 Date 或 dayjs/moment 解析
}


export interface UserProfileDTO {
  /**
   * 用户ID
   */
  userId: number;

  /**
   * 用户昵称
   */
  nickname: string;

  /**
   * 用户头像URL地址
   */
  avatarUrl: string;

  /**
   * 用户简介（短）
   */
  bio: string;

  /**
   * 用户自我描述（长）
   */
  selfDescription: string;
}

/**
 * 用户个人资料更新请求数据传输对象 (DTO)
 * 用于向前端 API 发送更新用户资料的请求数据
 */
export interface UserProfileUpdateRequestDTO {
  /**
   * 用户昵称
   */
  nickname?: string;

  /**
   * 用户头像URL地址
   */
  avatarUrl?: string;

  /**
   * 用户简介（短）
   */
  bio?: string;

  /**
   * 用户自我描述（长）
   */
  selfDescription?: string;
}

/**
 * 用户设置数据传输对象 (DTO)
 * 用于存储和管理用户的个性化设置
 */
export interface UserSettingDTO {
  /**
   * 静默通知开关
   */
  notificationMute: boolean;
}

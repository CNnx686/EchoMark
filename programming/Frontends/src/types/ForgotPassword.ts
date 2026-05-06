export interface PasswordResetRequestDTO {
  /**
   * 用户标识符，可以是邮箱或手机号
   */
  identifier: string;

  /**
   * 验证码
   */
  code: string;

  /**
   * 新密码
   */
  newPassword: string;
}

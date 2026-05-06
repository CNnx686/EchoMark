export interface RegisterCodeRequestDTO {
  /**
   * 用户邮箱
   */
  email:string;
}
// types/auth.ts 或类似路径
export interface UserRegisterRequestDTO {
  /**
   * 用户名
   */
  username: string;

  /**
   * 用户密码
   */
  password: string;

  /**
   * 用户邮箱
   */
  email: string;

  /**
   * 用户手机号
   */
  phoneNumber: string;

  /**
   * 验证码
   */
  code: string;
}


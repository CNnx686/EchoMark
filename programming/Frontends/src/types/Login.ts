export interface LoginResponseDTO {
  token: string;
}


export interface UserLoginRequestDTO {
  /**
   * 用户名
   */
  username:string;

  /**
   * 用户密码
   * 使用 @ToString.Exclude 防止密码被打印到日志中
   */
  password:string;
}


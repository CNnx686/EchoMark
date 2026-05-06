package org.tongji.sse.service;

import jakarta.servlet.http.HttpServletRequest;
import org.tongji.sse.dto.*;
import org.tongji.sse.entity.User;

/**
 * AuthService 接口定义了用户认证相关的业务逻辑。
 * 包括用户注册、登录、获取当前用户信息、登出等功能。
 */
public interface AuthService {

    /**
     * 发送注册验证码
     * @param email 用户邮箱
     * @return ApiResponse<Void>
     */
    ApiResponse<Void> sendRegisterCode(String email);

    /**
     * 用户注册方法
     * @param dto 包含用户名、密码、邮箱等注册信息
     * @return ApiResponse<Void>，data=null 表示无具体返回数据
     */
    ApiResponse<Void> register(UserRegisterRequestDTO dto);

    /**
     * 用户登录方法
     * @param dto 包含用户名和密码的登录信息
     * @return ApiResponse<LoginResponseDTO>，data 包含 token 和用户名
     */
    ApiResponse<LoginResponseDTO> login(UserLoginRequestDTO dto);

    /**
     * 获取当前登录用户信息
     * @param request HttpServletRequest 对象
     * @return ApiResponse<UserResponseDTO>，data 包含当前用户的用户名等信息
     */
    ApiResponse<UserResponseDTO> getCurrentUser(HttpServletRequest request);

    /**
     * 用户登出方法
     * @param request HttpServletRequest 对象
     * @return ApiResponse<Void>，data=null 表示无具体返回数据
     */
    ApiResponse<Void> logout(HttpServletRequest request);

    /**
     * 请求重置密码
     * @param identifier 用户邮箱
     * @return ApiResponse<Void>，data=null 表示无具体返回数据
     */
    ApiResponse<Void> requestPasswordReset(String identifier);

    /**
     * 重置密码
     * @param dto 包含验证码和新密码的请求数据
     * @return ApiResponse<Void>，data=null 表示无具体返回数据
     */
    ApiResponse<Void> resetPassword(PasswordResetRequestDTO dto);
}

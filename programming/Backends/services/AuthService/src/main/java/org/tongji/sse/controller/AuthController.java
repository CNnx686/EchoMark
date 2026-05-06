package org.tongji.sse.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.tongji.sse.dto.*;
import org.tongji.sse.service.AuthService;

/**
 * AuthController 类负责处理与用户认证相关的 HTTP 请求。
 * 提供用户注册、登录、获取当前用户信息以及登出功能。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    // 注入 AuthService 服务，用于处理具体的业务逻辑
    private final AuthService authService;

    /**
     * 发送注册验证码接口
     * @param dto 请求注册验证码信息，包含邮箱
     * @return ApiResponse<Void>，data=null
     */
    @PostMapping("/register/code")
    public ApiResponse<Void> sendRegisterCode(@RequestBody RegisterCodeRequestDTO dto) {
        return authService.sendRegisterCode(dto.getEmail());
    }

    /**
     * 用户注册接口
     * @param dto 注册请求信息，包括用户名、密码、邮箱等
     * @return ApiResponse<Void>，data=null 表示无具体返回数据
     */
    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody UserRegisterRequestDTO dto) {
        return authService.register(dto);
    }

    /**
     * 用户登录接口
     * @param dto 登录请求信息，包括用户名和密码
     * @return ApiResponse<LoginResponseDTO>，data 包含 token 和用户名
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponseDTO> login(@RequestBody UserLoginRequestDTO dto) {
        return authService.login(dto);
    }

    /**
     * 获取当前登录用户信息接口
     * @return ApiResponse<UserResponseDTO>，data 包含当前用户的用户名等信息
     */
    @GetMapping("/me")
    public ApiResponse<UserResponseDTO> getCurrentUser(HttpServletRequest request) {
        return authService.getCurrentUser(request);
    }

    /**
     * 用户登出接口
     * @return ApiResponse<Void>，data=null 表示无具体返回数据
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        return authService.logout(request);
    }

    /**
     * 请求密码重置接口
     * @param dto 请求密码重置信息，包含用户标识（邮箱或手机号）
     * @return ApiResponse<Void>，data=null
     */
    @PostMapping("/password/reset-request")
    public ApiResponse<Void> requestPasswordReset(@RequestBody PasswordResetInitRequestDTO dto) {
        return authService.requestPasswordReset(dto.getIdentifier());
    }

    /**
     * 重置密码接口
     * @param dto 密码重置请求信息
     * @return ApiResponse<Void>，data=null
     */
    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@RequestBody PasswordResetRequestDTO dto) {
        return authService.resetPassword(dto);
    }
}

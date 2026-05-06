package org.tongji.sse.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.tongji.sse.dto.ApiResponse;
import org.tongji.sse.dto.UserProfileDTO;
import org.tongji.sse.dto.UserProfileUpdateRequestDTO;
import org.tongji.sse.dto.UserSettingDTO;
import org.tongji.sse.service.UserService;

/**
 * 用户控制器
 * 处理与用户相关的HTTP请求，如获取和更新个人资料
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户的个人资料
     * 
     * @param request HTTP请求对象，用于获取认证信息
     * @return 包含用户个人资料DTO的API响应
     */
    @GetMapping("/profile")
    public ApiResponse<UserProfileDTO> getMyProfile(HttpServletRequest request) {
        return userService.getMyProfile(request);
    }

    /**
     * 更新当前登录用户的个人资料
     * 
     * @param request HTTP请求对象，用于获取认证信息
     * @param dto 包含更新信息的DTO
     * @param avatar 用户头像文件
     * @return 包含更新后的用户个人资料DTO的API响应
     */
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserProfileDTO> updateMyProfile(
            HttpServletRequest request,
            @ModelAttribute UserProfileUpdateRequestDTO dto,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar) {
        return userService.updateMyProfile(request, dto, avatar);
    }

    /**
     * 获取指定用户的个人资料
     * 
     * @param userId 用户ID
     * @return 包含用户个人资料DTO的API响应
     */
    @GetMapping("/{userId}/profile")
    public ApiResponse<UserProfileDTO> getUserProfile(@PathVariable Long userId) {
        return userService.getProfile(userId);
    }

    /**
     * 获取当前用户的收藏列表
     * @param request HTTP请求
     * @return 收藏的音频ID列表
     */
    @GetMapping("/favorites")
    public ApiResponse<java.util.List<Long>> getMyFavorites(HttpServletRequest request) {
        return userService.getMyFavorites(request);
    }

    /**
     * 添加收藏
     * @param request HTTP请求
     * @param recordId 音频记录ID
     * @return 操作结果
     */
    @PostMapping("/favorites/{recordId}")
    public ApiResponse<Void> addFavorite(HttpServletRequest request, @PathVariable Long recordId) {
        return userService.addFavorite(request, recordId);
    }

    /**
     * 删除收藏
     * @param request HTTP请求
     * @param recordId 音频记录ID
     * @return 操作结果
     */
    @DeleteMapping("/favorites/{recordId}")
    public ApiResponse<Void> removeFavorite(HttpServletRequest request, @PathVariable Long recordId) {
        return userService.removeFavorite(request, recordId);
    }

    /**
     * 获取当前用户的设置
     * @param request HTTP请求
     * @return 包含用户设置DTO的API响应
     */
    @GetMapping("/settings")
    public ApiResponse<UserSettingDTO> getMySettings(HttpServletRequest request) {
        return userService.getMySettings(request);
    }

    /**
     * 更新当前用户的设置
     * @param request HTTP请求
     * @param dto 包含更新信息的DTO
     * @return 包含更新后的用户设置DTO的API响应
     */
    @PutMapping("/settings")
    public ApiResponse<UserSettingDTO> updateMySettings(
            HttpServletRequest request,
            @RequestBody UserSettingDTO dto) {
        return userService.updateMySettings(request, dto);
    }
}

